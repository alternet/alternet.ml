package ml.alternet.security.auth.formats;

import java.util.Optional;

import javax.inject.Singleton;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.hashers.CurlyBracesCryptFormatHashers;
import ml.alternet.security.binary.BytesEncoder;
import ml.alternet.security.binary.BytesEncoding;

/**
 * The scheme of this format appears in curly braces.
 *
 * <h1>Examples :</h1>
 * <ul>
 * <li>Contains the password encoded to base64 (just like {SSHA}) :
 * <pre>{SSHA.b64}986H5cS9JcDYQeJd6wKaITMho4M9CrXM</pre></li>
 * <li>Contains the password encoded to hexa :
 * <pre>{SSHA.HEX}3f5ca6203f8cdaa44d9160575c1ee1d77abcf59ca5f852d1</pre></li>
 * </ul>
 *
 * SSHA : Salted SHA
 *
 * @author Philippe Poulard
 */
@Singleton
public class CurlyBracesCryptFormat implements CryptFormat {

    @Override
    public Optional<Hasher> resolve(String crypt) {
        Hasher.Builder b = null;
        SchemePart schemePart = null;
        try {
            schemePart = new SchemePart(crypt);
            if ("CRYPT".equals(schemePart.scheme)) {
                String mcfPart = crypt.substring(schemePart.rcb + 1);
                b = new ModularCryptFormat().resolve(mcfPart).get().getBuilder();
                b.setFormatter(new CryptFormatterWrapper<>(b.getFormatter(), "CRYPT"));
            } else if (schemePart.scheme != null) {
                String lookupKey = Hasher.Builder.class.getCanonicalName() + "/" + family() + "/" + schemePart.scheme;
                try {
                    Class<Hasher.Builder> clazz = DiscoveryService.lookup(lookupKey);
                    if (clazz != null) {
                        b = clazz.newInstance();
                    }
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException cnfe) {
                    LOGGER.warning(cnfe.toString());
                }
                if (b == null) {
                    try {
                        b = CurlyBracesCryptFormatHashers.valueOf(schemePart.scheme)
                            .get().getBuilder();
                    } catch (Exception e) {
                        LOGGER.fine("No crypt format found for " + schemePart.scheme + " for " + family());
                    }
                }
                if (b != null && schemePart.encoding != null) {
                    b.setEncoder("HEX".equalsIgnoreCase(schemePart.encoding) ? BytesEncoder.hexa.toString() : BytesEncoder.base64.toString());
                    b.setVariant("withEncoding");
                }
            }
        } catch (Exception e) {
            if (schemePart.scheme == null) {
                LOGGER.fine("Unable to parse " + family());
            } else {
                LOGGER.fine("No crypt format found for " + schemePart.scheme + " for " + family());
            }
        }
        return Optional.ofNullable(b).map(Hasher.Builder::build);
    }

    /**
     * @return "CurlyBracesCryptFormat"
     */
    @Override
    public String family() {
        return "CurlyBracesCryptFormat";
    }

    /**
     * @return "{[scheme]}:[shemeSpecificPart]"
     */
    @Override
    public String infoTemplate() {
        return "{[scheme]}[shemeSpecificPart]";
    }

    /**
     * Focus on the parts in curly braces.
     *
     * Exist in a separate class just for handling the optional part "encodeInHexa".
     *
     * @author Philippe Poulard
     */
    public static class SchemePart {

        public String scheme = null;
        public String encoding = null;
        public int rcb = -1; // right curly brace index

        public SchemePart(String crypt) {
            if (crypt.startsWith("{")) {
                rcb = crypt.indexOf('}');
                if (rcb > 1) {
                    scheme = crypt.substring(1, rcb);
                    int dot = scheme.indexOf('.');
                    if (dot != -1) {
                        encoding = scheme.substring(dot + 1);
                        scheme = scheme.substring(0, dot);
                        if (! "HEX".equalsIgnoreCase(encoding) && ! "b64".equalsIgnoreCase(encoding)) {
                            LOGGER.warning("Unknown scheme variant \"" + encoding + "\" in " + "\"" + crypt + "\"");
                        }
                    }
                }
            }
        }
    }

    /**
     * Crypt formatter for <tt>{scheme}hash</tt> and <tt>{scheme.encoding}hash</tt>.
     *
     * The encoding can be "HEX" (hexa) or "b64" (base64), set as the variant "withEncoding".
     *
     * @author Philippe Poulard
     */
    public static class CryptFormatter implements ml.alternet.security.auth.formats.CryptFormatter<CryptParts> {

        public static final CryptFormatter INSTANCE = new CryptFormatter();

        @Override
        public CryptParts parse(String crypt, Hasher hr) {
            SchemePart schemePart = new SchemePart(crypt);
            if (schemePart.scheme == null) {
                throw new IllegalArgumentException(crypt);
            }
            CryptParts parts = new CryptParts(hr);
            if (schemePart.encoding != hr.getConfiguration().getVariant()) {
                hr = hr.getBuilder().setVariant("withEncoding").build();
                parts.hr = hr;
            }
            BytesEncoding encoding = hr.getConfiguration().getEncoding();
            if (crypt.length() > schemePart.rcb + 1) {
                String ssp = crypt.substring(schemePart.rcb + 1);
                parts.hash = encoding.decode(ssp);
            }
            return parts;
        }

        @Override
        public String format(CryptParts parts) {
            StringBuffer buf = new StringBuffer();
            buf.append('{');
            buf.append(parts.hr.getConfiguration().getScheme());
            String variant = parts.hr.getConfiguration().getVariant();
            if ("withEncoding".equals(variant)) {
                BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
                String encName = encoding.name();
                if (encName.startsWith("hexa")) {
                    buf.append(".HEX");
                } else if (encName.startsWith("base64")) {
                    buf.append(".b64");
                }
            }
            buf.append('}');
            if (parts.hash != null && parts.hash.length > 0) {
                buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.hash));
            }
            return buf.toString();
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new CurlyBracesCryptFormat();
        }
    }

    /**
     * Salted crypt formatter for <tt>{scheme}hash</tt> and <tt>{scheme.encoding}hash</tt>.
     *
     * <ul>
     * <li>The encoding can be "HEX" (hexa) or "b64" (base64),
     * set as the variant "withEncoding".</li>
     * <li>{scheme} is {SMD5} or {SSHA}.</li>
     * <li>{hash} is the base64 encoding of {checksum}{salt};
     * and in turn {salt} is a multi-byte binary salt, and
     * {checksum} is the raw digest of the the string {password}{salt},
     * using the appropriate digest algorithm.</li>
     * </ul>
     *
     * @author Philippe Poulard
     */
    public static class SaltedCryptFormatter implements ml.alternet.security.auth.formats.CryptFormatter<SaltedParts> {

        public static final SaltedCryptFormatter INSTANCE = new SaltedCryptFormatter();

        @Override
        public SaltedParts parse(String crypt, Hasher hr) {
            SchemePart schemePart = new SchemePart(crypt);
            if (schemePart.scheme == null) {
                throw new IllegalArgumentException(crypt);
            }
            SaltedParts parts = new SaltedParts(hr);
            if (schemePart.encoding != hr.getConfiguration().getVariant()) {
                hr = hr.getBuilder().setVariant("withEncoding").build();
                parts.hr = hr;
            }
            BytesEncoding encoding = hr.getConfiguration().getEncoding();
            if (crypt.length() > schemePart.rcb + 1) {
                String ssp = crypt.substring(schemePart.rcb + 1);
                byte[] bytes = encoding.decode(ssp);
                int saltSize = hr.getConfiguration().getSaltByteSize();
                parts.salt = new byte[saltSize];
                System.arraycopy(bytes, bytes.length - saltSize, parts.salt, 0, saltSize);
                parts.hash = new byte[bytes.length - saltSize];
                System.arraycopy(bytes, 0, parts.hash, 0, bytes.length - saltSize);
            }
            return parts;
        }

        @Override
        public String format(SaltedParts parts) {
            StringBuffer buf = new StringBuffer();
            buf.append('{');
            buf.append(parts.hr.getConfiguration().getScheme());
            String variant = parts.hr.getConfiguration().getVariant();
            if ("withEncoding".equals(variant)) {
                BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
                String encName = encoding.name();
                if (encName.startsWith("hexa")) {
                    buf.append(".HEX");
                } else if (encName.startsWith("base64")) {
                    buf.append(".b64");
                }
            }
            buf.append('}');
            if (parts.hash != null && parts.hash.length > 0) {
                byte[] bytes = new byte[parts.hash.length + parts.salt.length];
                System.arraycopy(parts.hash, 0, bytes, 0, parts.hash.length);
                System.arraycopy(parts.salt, 0, bytes, parts.hash.length, parts.salt.length);
                buf.append(parts.hr.getConfiguration().getEncoding().encode(bytes));
            }
            return buf.toString();
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new CurlyBracesCryptFormat();
        }
    }

    public static class IterativeSaltedFormatter implements ml.alternet.security.auth.formats.CryptFormatter<WorkFactorSaltedParts> {

        public static final IterativeSaltedFormatter INSTANCE = new IterativeSaltedFormatter();

        @Override
        public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
            SchemePart schemePart = new SchemePart(crypt);
            if (schemePart.scheme == null) {
                throw new IllegalArgumentException(crypt);
            }

            String[] fields = crypt.substring(schemePart.rcb + 1).split("\\$");
            WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
            int index = 0;
            parts.workFactor = Integer.parseInt(fields[index++]);
            BytesEncoding encoding = hr.getConfiguration().getEncoding();
            parts.salt = encoding.decode(fields[index++]);
            parts.hash = encoding.decode(fields[index]);
            return parts;
        }

        @Override
        public String format(WorkFactorSaltedParts parts) {
            StringBuffer buf = new StringBuffer();
            buf.append('{');
            buf.append(parts.hr.getConfiguration().getScheme());
            buf.append('}');
            buf.append(parts.workFactor);
            buf.append('$');
            buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.salt));
            buf.append('$');
            buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.hash));
            return buf.toString();
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new CurlyBracesCryptFormat();
        }

    }

    /**
     * A wrapper around another formatter, for handling composed
     * crypts such as <tt>{CRYPT}$1$gwvn5BO0$3dyk8j.UTcsNUPrLMsU6/0</tt>
     *
     * @author Philippe Poulard
     *
     * @param <T> The crypt parts type
     */
    public static class CryptFormatterWrapper<T extends CryptParts> implements ml.alternet.security.auth.formats.CryptFormatter<T> {

        ml.alternet.security.auth.formats.CryptFormatter<T> cf;
        String scheme;

        public CryptFormatterWrapper(ml.alternet.security.auth.formats.CryptFormatter<T> cf, String scheme) {
            this.cf = cf;
            this.scheme = scheme;
        }

        @Override
        public T parse(String crypt, Hasher hr) {
            SchemePart sp = new SchemePart(crypt);
            String mcfPart = crypt.substring( sp.rcb + 1);
            return cf.parse(mcfPart, hr);
        }

        @Override
        public String format(T parts) {
            return "{" + this.scheme + "}" + cf.format(parts);
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new CurlyBracesCryptFormat();
        }

    }

}
