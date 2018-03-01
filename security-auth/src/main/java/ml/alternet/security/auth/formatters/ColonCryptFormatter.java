package ml.alternet.security.auth.formatters;

import static ml.alternet.security.auth.formatters.Util.decode;
import static ml.alternet.security.auth.formatters.Util.ensure;

import java.util.Optional;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.misc.Thrower;
import ml.alternet.scan.NumberConstraint;
import ml.alternet.scan.Scanner;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formats.ColonCryptFormat;
import ml.alternet.security.auth.formats.SchemeWithEncoding;

/**
 * The colon crypt formatter :
 *
 * <p>"<tt>PBKDF2:999:0314E17362D0D966C8F999A66045210DBE7EA897F024E07F:3AE344F7AB5AA17308A49FDAD997105340DD6E348FDF5623</tt>"</p>
 *
 * Although it is based on {@link WorkFactorSaltedParts}, the work factor may be omitted.
 *
 * @author Philippe Poulard
 */
public class ColonCryptFormatter implements CryptFormatter<WorkFactorSaltedParts> {

    @Override
    public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
        return Thrower.safeCall(() -> {
            Hasher hasher = hr;
            Scanner scanner = Scanner.of(crypt);
            SchemeWithEncoding swe = new SchemeWithEncoding(scanner, c -> true, ':');
            Optional<BytesEncoding> encoding = swe.encoding.get();
            if (encoding.isPresent()) {
                hasher = hr.getBuilder().setEncoding(encoding.get())
                    .setVariant("withEncoding")
                    .build();
            };
            ensure(scanner.hasNextChar(':', true));
            WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hasher);
            Number workFactor = scanner.nextNumber(NumberConstraint.INT_CONSTRAINT);
            if (workFactor != null) {
                parts.workFactor = workFactor.intValue();
                if (scanner.hasNext()) {
                    ensure(scanner.hasNextChar(':', true));
                }
            }
            if (scanner.hasNext()) {
                parts.salt = decode(hr, scanner, c -> c != ':');
                if (scanner.hasNext()) {
                    ensure(scanner.hasNextChar(':', true));
                    parts.hash = decode(hr, scanner, c -> true); // decode till the end
                }
            }
            return parts;
        });
    }

    @Override
    public String format(WorkFactorSaltedParts parts) {
        StringBuffer crypt = new StringBuffer(60);
        BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
        crypt.append(parts.hr.getConfiguration().getScheme());
        if ("withEncoding".equals(parts.hr.getConfiguration().getVariant())) {
            SchemeWithEncoding.code(encoding).ifPresent(s -> crypt.append('.').append(s));
        }
        if (parts.workFactor != -1) {
            crypt.append(':').append(Integer.toString(parts.workFactor));
        }
        if (parts.salt != null && parts.salt.length > 0) {
            crypt.append(':').append(encoding.encode(parts.salt));
            if (parts.hash != null && parts.hash.length > 0) {
                crypt.append(':').append(encoding.encode(parts.hash));
            }
        }
        return crypt.toString();
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new ColonCryptFormat();
    }

}
