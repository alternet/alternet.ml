package ml.alternet.security.auth.formatters;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.SaltedParts;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat.SchemePart;

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
public class SaltedCurlyBracesCryptFormatter implements ml.alternet.security.auth.CryptFormatter<SaltedParts> {

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