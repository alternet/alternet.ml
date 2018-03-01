package ml.alternet.security.auth.formatters;

import static ml.alternet.security.auth.formatters.Util.decode;
import static ml.alternet.security.auth.formatters.Util.ensure;

import ml.alternet.misc.Thrower;
import ml.alternet.scan.NumberConstraint;
import ml.alternet.scan.Scanner;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat.Hashers;
import ml.alternet.util.StringUtil;

/**
 * An iterative salted formatter.
 *
 * @author Philippe Poulard
 *
 * @see Hashers#PBKDF2
 */
public class IterativeSaltedCurlyBracesCryptFormatter implements CryptFormatter<WorkFactorSaltedParts> {

    @Override
    public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
        Scanner scanner = Scanner.of(crypt);
        Hasher hasher = CurlyBracesCryptFormatter.parseScheme(scanner, hr);
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hasher);
        return Thrower.safeCall(() -> {
            Number workFactor = scanner.nextNumber(NumberConstraint.INT_CONSTRAINT);
            if (workFactor != null) {
                parts.workFactor = workFactor.intValue();
                if (scanner.hasNext()) {
                    ensure(scanner.hasNextChar('$', true));
                }
            }
            if (scanner.hasNext()) {
                parts.salt = decode(hr, scanner, c -> c != '$');
                if (scanner.hasNext()) {
                    ensure(scanner.hasNextChar('$', true));
                    parts.hash = decode(hr, scanner, c -> true); // decode till the end
                }
            }
            return parts;
        });
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
        if (parts.salt != null && parts.salt.length > 0) {
            buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.salt));
            buf.append('$');
            if (parts.hash != null && parts.hash.length > 0) {
                buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.hash));
            }
        }
        return buf.toString();
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new CurlyBracesCryptFormat();
    }

}