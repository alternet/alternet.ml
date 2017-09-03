package ml.alternet.security.auth.hashers.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;

import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.algorithms.PBKDF2;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.formats.WorkFactorSaltedParts;
import ml.alternet.security.binary.SafeBuffer;

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
    public PBKDF2Hasher(Configuration config) {
        super(config);
    }

    /**
     * By default, return "PBKDF2".
     *
     * @return This hasher's scheme.
     */
    @Override
    public String getScheme() {
        return "PBKDF2";
    }

    /**
     * By default, return "PBKDF2WithHmacSHA1".
     *
     * @return The algorithm used by this implementation.
     */
    protected String getAlgorithm() {
        return "PBKDF2WithHmacSHA1";
    }

    @Override
    public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
        int hashByteSize = parts.hr.getConfiguration().getHashByteSize();
        try {
            return PBKDF2.hash(credentials.getPassword(), parts.salt, getAlgorithm(), parts.workFactor, hashByteSize);
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
    }

}
