package ml.alternet.security.auth.crypt;

import java.security.SecureRandom;

import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.Hasher.Configuration;

/**
 * When a crypt use a salt.
 *
 * @author Philippe Poulard
 */
public class SaltedParts extends CryptParts {

    public SaltedParts(Hasher hr) {
        super(hr);
    }

    /**
     * The salt.
     */
    public byte[] salt;

    /**
     * Generate a salt without checking the byte size.
     *
     * @see Configuration#getSaltByteSize()
     */
    public void generateSalt() {
        int saltSize = this.hr.getConfiguration().getSaltByteSize();
        salt = new byte[saltSize];
        new SecureRandom().nextBytes(salt);
    }

}
