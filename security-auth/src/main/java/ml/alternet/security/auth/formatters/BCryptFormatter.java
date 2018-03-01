package ml.alternet.security.auth.formatters;

import static ml.alternet.security.auth.formatters.Util.decode;
import static ml.alternet.security.auth.formatters.Util.ensure;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.inject.Singleton;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.scan.NumberConstraint;
import ml.alternet.scan.Scanner;
import ml.alternet.scan.StringConstraint.ReadUntilSingleChar;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.formats.ModularCryptFormat.Hashers;
import ml.alternet.security.auth.hasher.BCryptHasher;
import ml.alternet.util.StringUtil;

/**
 * The most popular formatter for BCrypt is the Modular Crypt Format.
 *
 * <p>"<tt>password</tt>" -&gt; "<tt>$2a$08$YkG5/ze2FPw8C6vuAs7WHuvS0IeyyQfLgE7Ti8tT5F2sMEkVJlNo.</tt>"</p>
 * <p>"<tt>password</tt>" -&gt; "<tt>$bcrypt-sha256$2a,12$LrmaIX5x4TRtAwEfwJZa1.$2ehnw6LvuIUTM0iz4iz9hTxv21B6KFO</tt>"</p>
 *
 * @see ModularCryptFormat
 *
 * @author Philippe Poulard
 *
 * @see Hashers#$2$
 */
@Singleton
public class BCryptFormatter implements CryptFormatter<WorkFactorSaltedParts> {

    @Override
    public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
        Scanner scanner = Scanner.of(crypt);
        try {
            ensure(scanner.hasNextChar('$', true));
            boolean hasAlgo = scanner.hasNextString("bcrypt-", true); // "bcrypt-sha256"
            if (hasAlgo) {
                String algo = scanner.nextString(new ReadUntilSingleChar('$')).get();
                ensure(scanner.hasNextChar('$', true));
                if (algo.startsWith("sha")) {
                    algo = "SHA-" + algo.substring("sha".length());
                } // SHA-256
                hr = hr.getBuilder()
                    .setClass(BCryptHasher.Digest.class)
                    .setAlgorithm(algo)
                    .build();
            }
            char variantSep = hasAlgo ? ',' : '$'; // hasAlgo => "2a,12"
            String variant = scanner.nextString(new ReadUntilSingleChar(variantSep)).get();
            if (! variant.equals(parts.hr.getConfiguration().getVariant())) {
                hr = parts.hr.getBuilder().setVariant(variant).build();
                parts.hr = hr;
            }
            ensure(scanner.hasNextChar(variantSep, true));
            parts.workFactor = scanner.nextNumber(NumberConstraint.INT_CONSTRAINT).intValue();
            if (scanner.hasNext()) {
                ensure(scanner.hasNextChar('$', true));
                int[] saltLen = { 0 };
                parts.salt = decode(hr, scanner, c -> hasAlgo ? c != '$' : saltLen[0]++ < 22);
                if (scanner.hasNext()) {
                    if (hasAlgo) {
                        ensure(scanner.hasNextChar('$', true));
                    }
                    parts.hash = decode(hr, scanner, c -> true); // decode till the end
                }
            }
        } catch (NullPointerException | NoSuchElementException | IOException e) {
            throw new IllegalArgumentException("Unable to parse " + crypt, e);
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
