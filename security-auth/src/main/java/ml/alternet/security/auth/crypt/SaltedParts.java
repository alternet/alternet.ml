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

    /**
     * Generate a salt with checking the byte size.
     *
     * @param min The min byte size of the salt size
     * @param max The max byte size of the salt size
     *
     * @see Configuration#getSaltByteSize()
     */
    public void generateSalt(int min, int max) {
        int saltSize = this.hr.getConfiguration().getSaltByteSize();
        if (saltSize < min || saltSize > max) {
            throw new IllegalArgumentException("Invalid salt size " + saltSize + " ; must be between " + min + " and " + max + " bytes.");
        }
        salt = new byte[saltSize];
        new SecureRandom().nextBytes(salt);
    }

}
