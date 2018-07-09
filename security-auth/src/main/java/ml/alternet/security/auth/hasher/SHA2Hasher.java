package ml.alternet.security.auth.hasher;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.security.algorithms.SHA2Crypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.binary.Transposer;
import ml.alternet.util.EnumUtil;

/**
 * The SHA-256/512 hash algorithms in a libc6 crypt() compatible way.
 *
 * Usually in the Modular Crypt Format :
 * <ol>
 * <li><tt>$5$rounds=12345$q3hvJE5mn5jKRsW.$BbbYTFiaImz9rTy03GGi.Jf9YY5bmxN0LU3p3uI1iUB</tt></li>
 * <li><tt>$6$49gH89TK$kt//rwoKf1ad/.hnthg363594OMwnM8Z4XScLZug4HdA36pw62AST6/kbirnypS5uzha83Ew2AmITy2HrCW3O0</tt></li>
 * </ol>
 *
 * @author Philippe Poulard
 */
public class SHA2Hasher extends HasherBase<WorkFactorSaltedParts> {

    /**
     * List of SHA algorithms with their variations.
     *
     * To get a value from its name, use the 'dashed' name of the algorithm,
     * not the 'underscored' name, e.g. "SHA-256" instead of "SHA_256".
     *
     * @author Philippe Poulard
     */
    public enum Algorithm {

        /**
         * The SHA-256 hash algorithm in a crypt(3) compatible way.
         */
        SHA_256( 5 ) {
            @Override
            public int blockSize() {
                return SHA2Crypt.SHA256_BLOCKSIZE;
            }
            @Override
            public byte[] order() {
                return SHA_256_ORDER;
            }
        },

        /**
         * The SHA-512 hash algorithm  in a crypt(3) compatible way.
         */
        SHA_512( 6 ) {
            @Override
            public int blockSize() {
                return SHA2Crypt.SHA512_BLOCKSIZE;
            }
            @Override
            public byte[] order() {
                return SHA_512_ORDER;
            }
        };

        // ordinal used only in the MCF formatting
        Algorithm(int ordinal) {
            // replace the "_" in the name by a "-"
            EnumUtil.replace(this, s -> s.replace('_', '-'));
            // replace the ordinal value
            EnumUtil.reorder(this, o -> ordinal);
        }

        /**
         * The block size used by this algorithm.
         *
         * @return The block size.
         */
        public abstract int blockSize();

        /**
         * Order of the result bytes to map
         * to base 64.
         *
         * @return The order of the indexes of
         *      the input byte array.
         */
        public abstract byte[] order();

    }

    /**
     * Create the SHA2 hasher.
     *
     * @param config The settings to use.
     */
    public SHA2Hasher(Builder config) {
        super(config);
    }

    static final byte[] SHA_256_ORDER = {
        0, 10, 20,
        21,  1, 11,
        12, 22,  2,
         3, 13, 23,
        24,  4, 14,
        15, 25,  5,
         6, 16, 26,
        27,  7, 17,
        18, 28,  8,
         9, 19, 29,
        -1, 31, 30
    };

    static final byte[] SHA_512_ORDER = {
        0, 21, 42,
       22, 43,  1,
       44,  2, 23,
        3, 24, 45,
       25, 46,  4,
       47,  5, 26,
        6, 27, 48,
       28, 49,  7,
       50,  8, 29,
        9, 30, 51,
       31, 52, 10,
       53, 11, 32,
       12, 33, 54,
       34, 55, 13,
       56, 14, 35,
       15, 36, 57,
       37, 58, 16,
       59, 17, 38,
       18, 39, 60,
       40, 61, 19,
       62, 20, 41,
       -1, -1, 63
    };

    @Override
    public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
        // SHA-256 or SHA-512
        Algorithm algo = Algorithm.valueOf(parts.hr.getConfiguration().getAlgorithm());
        byte bytes[] = SHA2Crypt.encrypt(
            credentials.getPassword(),
            parts.workFactor == -1 ? SHA2Crypt.ROUNDS_DEFAULT : parts.workFactor,
            parts.salt,
            algo.name(),
            algo.blockSize()
        );
        // bytes are not encoded in the original order but in that order
        return Transposer.transpose(bytes, algo.order(), algo.blockSize());
    }

    @Override
    public WorkFactorSaltedParts initializeParts() {
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(this);
        parts.salt = genSalt();
        parts.workFactor = -1;
        return parts;
    }

    private static final char[] SALTCHARS = BytesEncoder.ValueSpace.h64.get();
        // "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    private byte[] genSalt() {
        StringBuffer salt = new StringBuffer();
        SecureRandom randgen = new SecureRandom();
        while (salt.length() < 8) {
            int index = (int) (randgen.nextFloat() * SALTCHARS.length);
            salt.append(SALTCHARS[index]);
        }
        return salt.toString().getBytes(StandardCharsets.US_ASCII);
    };

}
