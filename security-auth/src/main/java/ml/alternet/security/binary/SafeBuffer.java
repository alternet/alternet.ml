package ml.alternet.security.binary;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import ml.alternet.util.Util;

/**
 * Perform safe operations on buffers, that is to say
 * that when the capacity is inappropriate for an operation,
 * a new array is allocated and the previous one is cleared.
 *
 * @author Philippe Poulard
 */
@Util
public final class SafeBuffer {

    private SafeBuffer() {}

    /**
     * Encode characters to bytes and ensures that intermediate data are cleared.
     * Designed for getting passwords bytes.
     *
     * @param cb The input characters ; not erased at the end of the conversion.
     * @param charset The charset use for encoding.
     * @return The encoded bytes.
     */
    public static ByteBuffer encode(CharBuffer cb, Charset charset) {
        // following code same as :
        //      return charset.encode(cb);
        CharsetEncoder enc = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return encode(cb, enc);
    }

    /**
     * Encode characters to bytes and ensures that intermediate data are cleared.
     * Designed for getting passwords bytes.
     *
     * @param cb The input characters ; not erased at the end of the conversion.
     * @param encoder The charset encoder use for encoding.
     * @return The encoded bytes.
     */
    public static ByteBuffer encode(CharBuffer cb, CharsetEncoder encoder) {
        // code taken from CharsetEncoder, but that take care of cleaning bytes
        // when the byte buffer is too small and need to reallocate
        int n = (int) (cb.remaining() * encoder.averageBytesPerChar());
        ByteBuffer bb = ByteBuffer.allocate(n);
        if ((n != 0) || (cb.remaining() != 0)) {
            encoder.reset();
            for (;;) {
                CoderResult cr = cb.hasRemaining() ?
                    encoder.encode(cb, bb, true) : CoderResult.UNDERFLOW;
                if (cr.isUnderflow()) {
                    cr = encoder.flush(bb);
                }
                if (cr.isUnderflow())
                    break;
                if (cr.isOverflow()) {
                    n = 2*n + 1; // Ensure progress; n might be 0!
                    ByteBuffer o = ByteBuffer.allocate(n);
                    bb.flip();
                    o.put(bb);
                    Arrays.fill(bb.array(), (byte) 0);
                    bb = o;
                    continue;
                }
                return null;
            }
            bb.flip();
        }
        return bb;
    }

    /**
     * Append some bytes to a buffer ; if the buffer is too small,
     * a new array is allocated and the previous one is cleared.
     *
     * @param bb The buffer.
     * @param bytes The bytes to append.
     *
     * @return The buffer.
     */
    public static ByteBuffer append(ByteBuffer bb, byte[] bytes) {
        int requiredSize = bb.limit() + bytes.length;
        if (requiredSize <= bb.capacity()) {
            bb.mark();
            bb.position(bb.limit());
            bb.limit(requiredSize);
            bb.put(bytes);
            bb.reset();
        } else { // need reallocation
            byte[] b = bb.array();
            int pos = bb.position();
            int len = bb.limit();
            bb = ByteBuffer.allocate(requiredSize);
            bb.put(b, 0, len);
            Arrays.fill(b, (byte) 0);
            bb.put(bytes);
            bb.position(pos);
        }
        return bb;
    }

    /**
     * Get the actual bytes from a buffer ; if the capacity
     * of this buffer is larger than the actual bytes, a new
     * array is allocated and the previous one is cleared.
     *
     * @param bb The buffer.
     * @return The actual bytes.
     */
    public static byte[] getData(ByteBuffer bb) {
        byte[] bytes;
        if (bb.remaining() == bb.capacity()) {
            bytes = bb.array();
        } else { // larger than necessary
            bytes = new byte[bb.remaining()];
            bb.get(bytes);
            Arrays.fill(bb.array(), (byte) 0);
        }
        return bytes;
    }

}
