package ml.alternet.security.auth.crypt;

import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;

/**
 * Base class for crypt parts.
 *
 * Subclasses contains additional fields when necessary
 * (typically, a salt).
 *
 * @see CryptFormatter
 *
 * @author Philippe Poulard
 */
public class CryptParts {

    /**
     * Create some crypt for a hasher
     *
     * @param hr The actual hasher.
     */
    public CryptParts(Hasher hr) {
        this.hr = hr;
    }

    /**
     * The hash field maybe initialized after parsing
     * when the purpose of the crypt is for checking some
     * credentials, or left blank on creation when the
     * purpose of the crypt is to compute a hash (then set
     * with the hash result before formatting a string
     * representation of this parts).
     */
    public byte[] hash;

    /**
     * The hasher that can process this crypt
     */
    public Hasher hr;;

}
