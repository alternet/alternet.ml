package ml.alternet.security.auth.hasher;

import java.nio.CharBuffer;
import java.security.SecureRandom;

import ml.alternet.security.Password;
import ml.alternet.security.algorithms.UnixCrypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.crypt.SaltedParts;
import ml.alternet.security.binary.SafeBuffer;

/**
 * The Unix Crypt hasher.
 *
 * @author Philippe Poulard
 */
public class UnixCryptHasher extends HasherBase<SaltedParts> {

    /**
     * UnixCryptHasher constructor.
     *
     * @param config The configuration of this hasher.
     */
    public UnixCryptHasher(Builder config) {
        super(config);
    }

    @Override
    public byte[] encrypt(Credentials credentials, SaltedParts parts) {
        long keyword = 0L;
        int salt = (parts.salt[1] << 6) | parts.salt[0];
        try (Password.Clear clear = credentials.getPassword().getClearCopy()) {
            byte[] pwd = SafeBuffer.getData(
                SafeBuffer.encode(
                    CharBuffer.wrap(clear.get()), getConfiguration().getCharset()
                )
            );
            for (int i = 0; i < 8; i++) {
                                                                // cast byte to int
                keyword = (keyword << 8) | (i < pwd.length ? 2 * (pwd[i]  &  0xff) : 0);
            }
        }
        try {
            return UnixCrypt.encrypt(salt, keyword);
        } finally {
            keyword = 0L;
        }
    }

    @Override
    public SaltedParts initializeParts() {
        SaltedParts parts = new SaltedParts(this);
        SecureRandom rand = new SecureRandom();
        parts.salt = new byte[2];
        rand.nextBytes(parts.salt);
        // bytes are not in the right range
        parts.salt[0] %= 64;
        parts.salt[1] %= 64;
        return parts;
    }

}
