package ml.alternet.security.auth.formatters;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formats.ModularCryptFormat;

/**
 * <tt>password</tt> -&gt; <tt>$5$rounds=80000$wnsT7Yr92oJoP28r$cKhJImk5mfuSKV9b3mumNzlbstFUplKtQXXMo4G6Ep5</tt>
 *
 * @author Philippe Poulard
 */
public class IterativeSaltedModularCryptFormatter implements CryptFormatter<WorkFactorSaltedParts> {

    @Override
    public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
        String[] fields = crypt.split("\\$");
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
        int index = 1;
        if (fields[1].startsWith("rounds=")) {
            parts.workFactor = Integer.parseInt(fields[1].substring("rounds=".length()));
        } else {
            parts.workFactor = 5000;
        }
        BytesEncoding encoding = hr.getConfiguration().getEncoding();
        parts.salt = encoding.decode(fields[++index]);
        parts.hash = encoding.decode(fields[++index]);
        return parts;
    }

    @Override
    public String format(WorkFactorSaltedParts parts) {
        StringBuffer buf = new StringBuffer();
        buf.append('$');
        buf.append(parts.hr.getConfiguration().getAlgorithm());
        buf.append('$');
        if (parts.workFactor != 5000) {
            buf.append("rounds=")
               .append(parts.workFactor);
        }
        buf.append('$');
        buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.salt));
        buf.append('$');
        buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.hash));
        return buf.toString();
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new ModularCryptFormat();
    }

}
