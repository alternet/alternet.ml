package ml.alternet.security.auth.crypt;

import ml.alternet.security.auth.Hasher;

/**
 * The crypt parts for Argon2.
 *
 * @author Philippe Poulard
 */
public class Argon2Parts extends SaltedParts {

    /**
     * Create some crypt for a hasher
     *
     * @param hr The actual hasher.
     */
    public Argon2Parts(Hasher hr) {
        super(hr);
    }

    public int version = -1;
    public int memoryCost = -1;
    public int timeCost = -1;
    public int parallelism = -1;
    public byte[] keyid;
    public byte[] data;

}