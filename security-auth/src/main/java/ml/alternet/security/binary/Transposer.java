package ml.alternet.security.binary;

/**
 * Bits transposition.
 *
 * Typically in use for transposing bits before mapping to base 64.
 *
 * Used by SHA-256, SHA-512, MD5.
 *
 * @author Philippe Poulard
 */
public final class Transposer {

    private Transposer() {};

    /**
     * Transpose the bytes to the (base 64) expected order :
     *
     * <p>e) the base-64 encoded final C digest. The encoding used is as follows:<br/>
     * [...]</p>
     *
     * <p>Each group of three bytes from the digest produces four
     * characters as output:</p>
     *
     * <ol>
     * <li>character: the six low bits of the first byte</li>
     * <li>character: the two high bits of the first byte and the
     * four low bytes from the second byte</li>
     * <li>character: the four high bytes from the second byte and
     * the two low bits from the third byte</li>
     * <li>character: the six high bits from the third byte</li>
     * </ol>
     *
     * <p>The groups of three bytes are as follows (in this sequence).
     * These are the indices into the byte array containing the
     * digest, starting with index 0. For the last group there are
     * not enough bytes left in the digest and the value zero is used
     * in its place (zero padding). This group also produces only three or two
     * characters as output for SHA-512 and SHA-512 respectively.
     * MD5 doesn't use a zero-padding, but revert alone the last byte.</p>
     *
     * <p>3 consecutive 8-bits gives 4 *reverted* 6-bits
     * that is to say with the following mapping :</p>
     *
     * <code>
     * 0       10      20       // source index (see SHA-256 order)
     * ........________........ // 8-bits position
     * abcdefghijklmnopqrstuvwx // 24 bits input (3 bytes)
     * ++++++------++++++------ // 6 bits position marks (4 * 6-bits)
     * stuvwxmnopqrghijklabcdef // 24 bits output (3 bytes)
     * ........________........ // 8-bits position
     * 0       1       2        // target index
     * </code>
     *
     * @param b The bytes to map.
     * @param order The order of the indexes of
     *      the input byte array.
     * @param blockSize The block size used by this algorithm..
     *
     * @return The transposed bytes.
     */
    public static byte[] transpose(byte[] b, byte[] order, int blockSize) {
        byte[] result = new byte[blockSize];
        int i = 0;
        for ( ; i < order.length / 3; i++) {
            // process 3 by 3
            byte pos0 = order[i * 3    ]; // starts with 0  see below
            byte pos1 = order[i * 3 + 1]; // starts with 10 see below
            byte pos2 = order[i * 3 + 2]; // starts with 20 see below
            byte v0 = pos0 == -1 ? 0 : b[pos0]; // 0 padding if necessary
            byte v1 = pos1 == -1 ? 0 : b[pos1]; // 0 padding if necessary
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
            // and so on with 21, 1, 11, you got it ? (see SHA-256 ORDER)
        }
        if (i * 3 < order.length) { // no padding (MD5 case)
            // just a single additional byte (not covered : 2 additional bytes)
            // revert the last byte
            byte pos = order[i * 3]; // i * 3 = 15 for MD5
            byte val = b[pos]; // b[11] for MD5
            result[i * 3] = (byte) ( ((val << 2)  & 0b11111100) | ((val >> 6) & 0b00000011) );
        }
        return result;
        // SHA-256 ORDER:
        //  0 - 10 - 20 <= 1st iter
        // 21 -  1 - 11 <= 2nd iter
        // 12 - 22 -  2 <= ...
        //  3 - 13 - 23
        // 24 -  4 - 14
        // 15 - 25 -  5
        //  6 - 16 - 26
        // 27 -  7 - 17
        // 18 - 28 -  8
        //  9 - 19 - 29
        //  * - 31 - 30

        // * appears as -1 in the array, and stands for 0 padding
    }

}
