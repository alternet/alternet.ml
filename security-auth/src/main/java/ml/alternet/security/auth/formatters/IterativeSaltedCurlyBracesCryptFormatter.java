package ml.alternet.security.auth.formatters;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat.SchemePart;
import ml.alternet.util.StringUtil;

/**
 * An iterative salted formatter.
 *
 * @author Philippe Poulard
 */
public class IterativeSaltedCurlyBracesCryptFormatter
    implements ml.alternet.security.auth.CryptFormatter<WorkFactorSaltedParts>
{

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
        String algo = parts.hr.getConfiguration().getAlgorithm();
        if (! StringUtil.isVoid(algo) && algo.startsWith("PBKDF2WithHmac")) {
            algo = algo.substring("PBKDF2WithHMac".length());
            if (! "SHA1".equals(algo)) {
                buf.append('-').append(algo);
            }
        }
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