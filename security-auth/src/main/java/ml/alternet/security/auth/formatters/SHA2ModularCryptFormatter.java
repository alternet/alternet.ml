package ml.alternet.security.auth.formatters;

import java.nio.charset.StandardCharsets;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.algorithms.SHA2Crypt;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.hasher.SHA2Hasher;
import ml.alternet.security.auth.hasher.SHA2Hasher.Algorithm;

/**
 * SHA2 crypt formatter :
 * <ul>
 * <li><tt>$5$rounds=12345$q3hvJE5mn5jKRsW.$BbbYTFiaImz9rTy03GGi.Jf9YY5bmxN0LU3p3uI1iUB</tt></li>
 * <li><tt>$6$49gH89TK$kt//rwoKf1ad/.hnthg363594OMwnM8Z4XScLZug4HdA36pw62AST6/kbirnypS5uzha83Ew2AmITy2HrCW3O0</tt></li>
 * </ul>
 *
 * @author Philippe Poulard
 */
public class SHA2ModularCryptFormatter implements CryptFormatter<WorkFactorSaltedParts> {
    @Override
    public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
        String[] stringParts = crypt.split("\\$");
        if ("5".equals(stringParts[1]) || "6".equals(stringParts[1])) {
            WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
            int pos = 2;
            if (stringParts[2].startsWith(SHA2Crypt.ROUNDS_PREFIX)) {
                pos++;
                parts.workFactor = Integer.parseInt(stringParts[2].substring(SHA2Crypt.ROUNDS_PREFIX.length()));
                parts.workFactor = Math.max(SHA2Crypt.ROUNDS_MIN, Math.min(SHA2Crypt.ROUNDS_MAX, parts.workFactor));
            } else {
                parts.workFactor = -1;
            }
            BytesEncoding encoding = hr.getConfiguration().getEncoding();
            parts.salt = stringParts[pos++].getBytes(StandardCharsets.US_ASCII);
            if (stringParts.length > pos) {
                parts.hash = encoding.decode(stringParts[pos++]);
            }
            return parts;
        } else {
            throw new IllegalArgumentException("Unable to parse " + crypt);
        }
    }

    @Override
    public String format(WorkFactorSaltedParts parts) {
        StringBuffer buf = new StringBuffer();
        buf.append('$');
        Algorithm algo = Algorithm .valueOf(parts.hr.getConfiguration().getAlgorithm());
        buf.append(algo.ordinal());
        buf.append("$");
        if (parts.workFactor != -1) {
            buf.append(SHA2Crypt.ROUNDS_PREFIX);
            buf.append(parts.workFactor);
            buf.append("$");
        }
        buf.append(new String(parts.salt, StandardCharsets.US_ASCII));
        buf.append('$');
        BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
        String string = encoding.encode(parts.hash);
        buf.append(string);
        return buf.toString();
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new ModularCryptFormat();
    }
}