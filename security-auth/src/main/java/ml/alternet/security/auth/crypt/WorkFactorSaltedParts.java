package ml.alternet.security.auth.crypt;

import ml.alternet.security.auth.Hasher;

/**
 * When a crypt use a work factor.
 *
 * @author Philippe Poulard
 */
public class WorkFactorSaltedParts extends SaltedParts {

    public WorkFactorSaltedParts(Hasher hr) {
        super(hr);
    }

    /**
     * The work factor (number of iterations
     * or log rounds).
     */
    public int workFactor;

}
