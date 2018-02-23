package ml.alternet.security.auth.formatters;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.hasher.BCryptHasher;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.util.StringUtil;

/**
 * The most popular formatter for BCrypt is the Modular Crypt Format.
 *
 * <p>"<tt>password</tt>" -&gt; "<tt>$2a$08$YkG5/ze2FPw8C6vuAs7WHuvS0IeyyQfLgE7Ti8tT5F2sMEkVJlNo.</tt>"</p>
 * <p>"<tt>password</tt>" -&gt; "<tt>$bcrypt-sha256$2a,12$LrmaIX5x4TRtAwEfwJZa1.$2ehnw6LvuIUTM0iz4iz9hTxv21B6KFO</tt>"</p>
 *
 * @see ModularCryptFormatHashers
 *
 * @author Philippe Poulard
 */
public class BCryptFormatter implements CryptFormatter<WorkFactorSaltedParts> {

    @Override
    public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
        boolean hasAlgo = false;
        String[] stringParts = crypt.split("\\$");
        if (stringParts[1].startsWith("bcrypt-")) {
            hasAlgo = true;
            String algo = stringParts[1].substring("bcrypt-".length()); // sha256
            if (algo.startsWith("sha")) {
                algo = "SHA-" + algo.substring("sha".length());
            } // SHA-256
            hr = hr.getBuilder()
                .setClass(BCryptHasher.Digest.class)
                .setAlgorithm(algo)
                .build();
            String[] varRound = stringParts[2].split(","); // 2a,12
            stringParts[1] = varRound[0]; // 2a
            stringParts[2] = varRound[1]; // 12
        } // go on normally
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
        if (! stringParts[1].equals(parts.hr.getConfiguration().getVariant())) {
            hr = parts.hr.getBuilder().setVariant(stringParts[1]).build();
            parts.hr = hr;
        }
        if (stringParts.length > 2 && ! StringUtil.isVoid(stringParts[2])) {
            parts.workFactor = Integer.parseInt(stringParts[2]);
        }
        if (stringParts.length > 3 && ! StringUtil.isVoid(stringParts[3])) {
            String salt;
            if (hasAlgo) {
                salt = stringParts[3];
            } else {
                salt = stringParts[3].substring(0, 22);
            }
            BytesEncoding encoding = hr.getConfiguration().getEncoding();
            parts.salt = encoding.decode(salt);
            String hash = "";
            if (hasAlgo) {
                if (stringParts.length > 4 && ! StringUtil.isVoid(stringParts[4])) {
                    hash = stringParts[4];
                }
            } else {
                hash = stringParts[3].substring(22);
            }
            if (hash.length() > 0) {
                parts.hash = encoding.decode(hash);
            }
        }
        return parts;
    }

    @Override
    public String format(WorkFactorSaltedParts parts) {
        String algo = parts.hr.getConfiguration().getAlgorithm(); // SHA-256
        boolean hasAlgo = ! StringUtil.isVoid(algo);
        StringBuffer crypt = new StringBuffer(60);
        if (hasAlgo) {
            if (algo.startsWith("SHA-")) {
                algo = "sha" + algo.substring("SHA-".length());
            }
            crypt.append("$bcrypt-").append(algo); // sha256
        }
        crypt.append("$2");
        char version = BCryptHasher.getVersion(parts.hr);
        if (version >= 'a') {
            crypt.append(version);
        }
        crypt.append(hasAlgo ? ',' : '$');
        if (parts.workFactor < 10) {
            crypt.append("0");
        }
        if (parts.workFactor > 30) {
            throw new IllegalArgumentException(
                    "log_rounds exceeds maximum (30)");
        }
        crypt.append(Integer.toString(parts.workFactor));
        crypt.append("$");
        BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
        crypt.append(encoding.encode(parts.salt));
        if (parts.hash != null && parts.hash.length > 0) {
            if (hasAlgo) {
                crypt.append('$');
            }
            crypt.append(encoding.encode(parts.hash));
        }
        return crypt.toString();
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new ModularCryptFormat();
    }
}