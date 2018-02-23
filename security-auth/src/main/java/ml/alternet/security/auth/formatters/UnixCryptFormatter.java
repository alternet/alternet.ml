package ml.alternet.security.auth.formatters;

import java.nio.charset.StandardCharsets;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.SaltedParts;
import ml.alternet.security.auth.formats.UnixCryptFormat;

/**
 * The Unix Crypt formatter : 2 characters salt followed by the hash,
 * each character encoded in the h64 encoder value space.
 *
 * @author Philippe Poulard
 */
public class UnixCryptFormatter implements CryptFormatter<SaltedParts> {

    @Override
    public SaltedParts parse(String crypt, Hasher hr) {
        SaltedParts parts = new SaltedParts(hr);
        BytesEncoding encoding = hr.getConfiguration().getEncoding();
        String b64 = new String(BytesEncoder.ValueSpace.valueSpace(encoding));
        // characters are individually mapped to the value space
        parts.salt = new byte[2];
        parts.salt[0] = (byte) b64.indexOf(crypt.charAt(0));
        parts.salt[1] = (byte) b64.indexOf(crypt.charAt(1));
        parts.hash = new byte[11];
        for (int i = 2 ; i < 13 ; i++) {
            parts.hash[i - 2] = (byte) b64.indexOf(crypt.charAt(i));
        }
        return parts;
    }

    @Override
    public String format(SaltedParts parts) {
        byte[] b = new byte[13];
        b[0] = parts.salt[0];
        b[1] = parts.salt[1];
        System.arraycopy(parts.hash, 0, b, 2, 11);
        return new String(b, 0, 13, StandardCharsets.US_ASCII);
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new UnixCryptFormat();
    }

}