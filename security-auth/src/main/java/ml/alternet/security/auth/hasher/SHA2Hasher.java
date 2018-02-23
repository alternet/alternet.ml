package ml.alternet.security.auth.hasher;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.algorithms.SHA2Crypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formats.CryptFormatter;
import ml.alternet.security.auth.formats.ModularCryptFormat;
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
        return map(bytes, algo);
    }

    /**
     * Map the bytes to the base 64 expected order.
     *
     * @param b The bytes to map.
     * @param algo The algorithm.
     *
     * @return The remapped bytes.
     */
    protected byte[] map(byte[] b, Algorithm algo) {
        // e) the base-64 encoded final C digest. The encoding used is as
        // follows:
        // [...]
        //
        // Each group of three bytes from the digest produces four
        // characters as output:
        //
        // 1. character: the six low bits of the first byte
        // 2. character: the two high bits of the first byte and the
        // four low bytes from the second byte
        // 3. character: the four high bytes from the second byte and
        // the two low bits from the third byte
        // 4. character: the six high bits from the third byte
        //
        // The groups of three bytes are as follows (in this sequence).
        // These are the indices into the byte array containing the
        // digest, starting with index 0. For the last group there are
        // not enough bytes left in the digest and the value zero is used
        // in its place. This group also produces only three or two
        // characters as output for SHA-512 and SHA-512 respectively.


        // 3 consecutive 8-bits gives 4 *reverted* 6-bits
        // that is to say with the following mapping :
        // 0       10      20       // source index (see SHA_256_ORDER above)
        // ........________........ // 8-bits position
        // abcdefghijklmnopqrstuvwx // 24 bits input (3 bytes)
        // ++++++------++++++------ // 6 bits position marks (4 * 6-bits)
        // stuvwxmnopqrghijklabcdef // 24 bits output (3 bytes)
        // ........________........ // 8-bits position
        // 0       1       2        // target index

        byte[] order = algo.order();
        byte[] result = new byte[algo.blockSize()];

        for (int i = 0 ; i < order.length / 3; i++) {
            // process 3 by 3
            byte pos0 = order[i * 3    ]; // starts with 0
            byte pos1 = order[i * 3 + 1]; // starts with 10
            byte pos2 = order[i * 3 + 2]; // starts with 20
            byte v0 = pos0 == -1 ? 0 : b[pos0];
            byte v1 = pos1 == -1 ? 0 : b[pos1];
            byte v2 = b[pos2];
            // some masks are useless but ensure we don't miss a bit
            // stuvwxmn
            result[i * 3] = (byte) ( ((v2 << 2) & 0b11111100) | ((v1 >> 2) & 0b00000011) );
            if (i * 3 + 1 < result.length) {
                if (pos0 == -1) {
                    // what is annoying is the processing of the end,
                    // since 43 sextets (258 bits) doesn't fit in 32 bytes (256 bits)

                    // just keep the significative bits

                    // opqrijkl (abcdefgh are missing)
                    result[i * 3 + 1] = (byte) (  ((v1 << 6) & 0b11000000) | ((v2 >> 2) & 0b00110000)
                                                | ((v1 >> 4) & 0b00001111) );
                } else {
                    // opqrghij
                    result[i * 3 + 1] = (byte) (  ((v1 << 6) & 0b11000000) | ((v2 >> 2) & 0b00110000)
                                                | ((v0 << 2) & 0b00001100) | ((v1 >> 6) & 0b00000011) );
                }
            }
            if (i * 3 + 2 < result.length) {
                // klabcdef
                result[i * 3 + 2  ] = (byte) ( ((v1 << 2) & 0b11000000) | ((v0 >> 2) & 0b00111111 ) );
            }
            if (pos1 == -1) {
                // stuvwxqr (abcdefghijklmnop are missing)
                result[i * 3] = (byte) ( ((v2 << 2) & 0b11111100) | ((v2 >> 6) & 0b00000011) );
            }
            // and so on with 21, 1, 11, you got it ? (see SHA_256_ORDER above)
        }
        return result;
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
    }

    /**
     * SHA2 crypt formatter :
     * <ul>
     * <li><tt>$5$rounds=12345$q3hvJE5mn5jKRsW.$BbbYTFiaImz9rTy03GGi.Jf9YY5bmxN0LU3p3uI1iUB</tt></li>
     * <li><tt>$6$49gH89TK$kt//rwoKf1ad/.hnthg363594OMwnM8Z4XScLZug4HdA36pw62AST6/kbirnypS5uzha83Ew2AmITy2HrCW3O0</tt></li>
     * </ul>
     */
    public static final CryptFormatter<WorkFactorSaltedParts> SHA2CRYPT_FORMATTER = new CryptFormatter<WorkFactorSaltedParts>() {
        @Override
        public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
            String[] stringParts = crypt.split("\\$");
            if ("5".equals(stringParts[1]) || "6".equals(stringParts[1])) {
                WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
                int pos = 2;
                if (stringParts[2].startsWith(SHA2Crypt.ROUNDS_PREFIX)) {
                    pos++;
                    parts.workFactor = Integer.parseInt(stringParts[2].substring(SHA2Crypt.ROUNDS_PREFIX.length()));
                    parts.workFactor = Math.max(SHA2Crypt.ROUNDS_MIN, Math.min(SHA2Crypt.ROUNDS_MAX, parts.workFactor));
                } else {
                    parts.workFactor = -1;
                }
                BytesEncoding encoding = hr.getConfiguration().getEncoding();
                parts.salt = stringParts[pos++].getBytes(StandardCharsets.US_ASCII);
                if (stringParts.length > pos) {
                    parts.hash = encoding.decode(stringParts[pos++]);
                }
                return parts;
            } else {
                throw new IllegalArgumentException("Unable to parse " + crypt);
            }
        }

        @Override
        public String format(WorkFactorSaltedParts parts) {
            StringBuffer buf = new StringBuffer();
            buf.append('$');
            Algorithm algo = Algorithm .valueOf(parts.hr.getConfiguration().getAlgorithm());
            buf.append(algo.ordinal());
            buf.append("$");
            if (parts.workFactor != -1) {
                buf.append(SHA2Crypt.ROUNDS_PREFIX);
                buf.append(parts.workFactor);
                buf.append("$");
            }
            buf.append(new String(parts.salt, StandardCharsets.US_ASCII));
            buf.append('$');
            BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
            String string = encoding.encode(parts.hash);
            buf.append(string);
            return buf.toString();
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new ModularCryptFormat();
        }
    };

}
