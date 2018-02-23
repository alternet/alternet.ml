package ml.alternet.security.auth.formatters;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat.SchemePart;

/**
 * Crypt formatter for <tt>{scheme}hash</tt> and <tt>{scheme.encoding}hash</tt>.
 *
 * The encoding can be "HEX" (hexa) or "b64" (base64), set as the variant "withEncoding".
 *
 * @author Philippe Poulard
 */
public class CurlyBracesCryptFormatter implements ml.alternet.security.auth.CryptFormatter<CryptParts> {

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