package ml.alternet.security.auth.hasher;

import java.security.InvalidAlgorithmParameterException;
import java.util.logging.Logger;

import ml.alternet.misc.Thrower;
import ml.alternet.security.algorithms.PBKDF2;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;

/**
 * A slightly modified PBKDF2 hasher, that erase intermediate data.
 *
 * The underlying MAC algorithm is unchanged.
 *
 * @author Philippe Poulard
 */
public class PBKDF2Hasher extends HasherBase<WorkFactorSaltedParts> {

    static final Logger LOGGER = Logger.getLogger(PBKDF2Hasher.class.getName());

    /**
     * Create a PBKDF2 hasher.
     *
     * @param config The configuration of this hasher.
     */
    public PBKDF2Hasher(Builder config) {
        super(config);
    }

    @Override
    public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
        int hashByteSize = parts.hr.getConfiguration().getHashByteSize();
        try {
            return PBKDF2.hash(
                credentials.getPassword(),
                parts.salt,
                parts.hr.getConfiguration().getAlgorithm(),
                parts.workFactor,
                hashByteSize
            );
        } catch (InvalidAlgorithmParameterException e) {
            return Thrower.doThrow(e);
        }
    }

    @Override
    public WorkFactorSaltedParts initializeParts() {
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(this);
        parts.generateSalt(); // TODO: check parameter and min-max
        parts.workFactor = getConfiguration().getIterations();
        return parts;
    };

}
