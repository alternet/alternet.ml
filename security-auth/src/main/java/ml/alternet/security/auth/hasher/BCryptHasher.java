package ml.alternet.security.auth.hasher;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.stream.IntStream;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.algorithms.BCrypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.binary.SafeBuffer;

/**
 * The BCrypt hasher.
 *
 * @author Philippe Poulard
 */
public class BCryptHasher extends HasherBase<WorkFactorSaltedParts> {

    /**
     * Create a BCrypt hasher.
     *
     * @param config The configuration of this hasher.
     */
    public BCryptHasher(Builder config) {
        super(config);
    }

    /**
     * Get the version configured in a hasher.
     *
     * @param hr The hasher
     *
     * @return The version such as 'a' ; may also be {@code (char) 0}
     */
    public static char getVersion(Hasher hr) {
        String variant = hr.getConfiguration().getVariant();
        if (variant != null && variant.length() > 1) {
            return variant.charAt(1);
        } else {
            return (char) 0;
        }
    }

    @Override
    public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
        ByteBuffer bb;
        try (Password.Clear pwd = credentials.getPassword().getClearCopy()) {
            bb = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), getConfiguration().getCharset());
        }
        if (getVersion(this) >= 'a') {
            // append some more bytes to the buffer
            byte[] minorBytes = "\000".getBytes(getConfiguration().getCharset());
            bb = SafeBuffer.append(bb, minorBytes);
        }
        byte[] passwordb = SafeBuffer.getData(bb);
        byte[] hash = new BCrypt().crypt_raw(passwordb, parts.salt, parts.workFactor);
        Arrays.fill(passwordb, (byte) 0);

        // adjust size
        if (hash.length > BCrypt.resLen) {
            byte[] b = new byte[BCrypt.resLen];
            System.arraycopy(hash, 0, b, 0, BCrypt.resLen);
            hash = b;
        }
        return hash;
    }

    @Override
    public WorkFactorSaltedParts initializeParts() {
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(this);
        parts.workFactor = getConfiguration().getLogRounds();
        parts.generateSalt();
        return parts;
    }

    /**
     * BCrypt truncate passwords to 72 bytes, and some other minor quirks. This class works around that
     * issue by first running the password through a digest such as SHA2-256.
     *
     * @author Philippe Poulard
     */
    public static class Digest extends BCryptHasher {

        /**
         * Create a BCrypt hasher.
         *
         * @param config The configuration of this hasher.
         */
        public Digest(Builder config) {
            super(config);
        }

        @Override
        public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
            ByteBuffer bb;
            try (Password.Clear pwd = credentials.getPassword().getClearCopy()) {
                bb = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), getConfiguration().getCharset());
            }
            MessageDigest digest = Thrower.safeCall(() ->
                MessageDigest.getInstance(getConfiguration().getAlgorithm())
            );
            digest.update(bb);
            byte[] bytes = digest.digest();

            // same as String key = BytesEncoder.base64.encode(bytes);
            // but with safe buffers
            CharBuffer[] cbuf = { CharBuffer.allocate(bytes.length * 2) };
            cbuf[0].flip();
            BytesEncoder.base64.encode(IntStream.range(0, bytes.length).map(i -> bytes[i]))
                .forEach(c -> cbuf[0] = SafeBuffer.append(cbuf[0], c));
            credentials = Credentials.fromPassword(SafeBuffer.getData(cbuf[0]));

            return super.encrypt(credentials, parts);
        }

    }

}
