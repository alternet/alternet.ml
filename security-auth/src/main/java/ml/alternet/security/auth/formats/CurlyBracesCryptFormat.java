package ml.alternet.security.auth.formats;

import java.util.Optional;

import javax.inject.Singleton;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.encode.BytesEncoder;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formatters.CurlyBracesCryptFormatterWrapper;
import ml.alternet.security.auth.hashers.CurlyBracesCryptFormatHashers;

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
                b.setFormatter(new CurlyBracesCryptFormatterWrapper<>(b.getFormatter(), "CRYPT"));
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
                    b.setEncoder("HEX".equalsIgnoreCase(schemePart.encoding)
                        ? BytesEncoder.hexa.toString()
                        : BytesEncoder.base64.toString());
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

        /**
         * The scheme
         */
        public String scheme = null;

        /**
         * The encoding
         */
        public String encoding = null;

        /**
         * Right curly brace index, for further parsing
         */
        public int rcb = -1;

        /**
         * Breakdown a crypt into its parts.
         *
         * @param crypt The actual crypt.
         */
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

}
