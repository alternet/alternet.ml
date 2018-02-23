package ml.alternet.security.auth.formatters;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formats.ColonCryptFormat;
import ml.alternet.util.StringUtil;

/**
 * The colon crypt formatter :
 *
 * <p>"<tt>PBKDF2:999:0314E17362D0D966C8F999A66045210DBE7EA897F024E07F:3AE344F7AB5AA17308A49FDAD997105340DD6E348FDF5623</tt>"</p>
 *
 * @author Philippe Poulard
 */
public class ColonCryptFormatter implements CryptFormatter<WorkFactorSaltedParts> {

    @Override
    public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
        String[] stringParts = crypt.split(":");
        if (stringParts.length > 1 && ! StringUtil.isVoid(stringParts[1])) {
            parts.workFactor = Integer.parseInt(stringParts[1]);
        }
        BytesEncoding encoding = hr.getConfiguration().getEncoding();
        if (stringParts.length > 2 && ! StringUtil.isVoid(stringParts[2])) {
            String salt = stringParts[2];
            parts.salt = encoding.decode(salt);
        }
        if (stringParts.length > 3 && ! StringUtil.isVoid(stringParts[3])) {
            String hash = stringParts[3];
            parts.hash = encoding.decode(hash);
        }
        return parts;
    }

    @Override
    public String format(WorkFactorSaltedParts parts) {
        StringBuffer crypt = new StringBuffer(60);
        BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
        crypt.append(parts.hr.getConfiguration().getScheme())
            .append(':')
            .append(Integer.toString(parts.workFactor))
            .append(':')
            .append(encoding.encode(parts.salt));
        if (parts.hash != null && parts.hash.length > 0) {
            crypt.append(':')
                .append(encoding.encode(parts.hash));
        }
        return crypt.toString();
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new ColonCryptFormat();
    }
}