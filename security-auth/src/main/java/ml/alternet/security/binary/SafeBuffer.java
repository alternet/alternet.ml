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
     * Append a byte to a buffer ; if the buffer is too small,
     * a new array is allocated and the previous one is cleared.
     *
     * @param bb The buffer.
     * @param by The byte to append.
     *
     * @return The buffer.
     */
    public static ByteBuffer append(ByteBuffer bb, byte by) {
        int requiredSize = bb.limit() + 1;
        if (requiredSize <= bb.capacity()) {
            bb.mark();
            bb.position(bb.limit());
            bb.limit(requiredSize);
            bb.put(by);
            bb.reset();
        } else { // need reallocation
            byte[] b = bb.array();
            int pos = bb.position();
            int len = bb.limit();
            bb = ByteBuffer.allocate(requiredSize);
            bb.put(b, 0, len);
            Arrays.fill(b, (byte) 0);
            bb.put(by);
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

    /**
     * Append some chars to a buffer ; if the buffer is too small,
     * a new array is allocated and the previous one is cleared.
     *
     * @param cb The buffer.
     * @param chars The chars to append.
     *
     * @return The buffer.
     */
    public static CharBuffer append(CharBuffer cb, char[] chars) {
        int requiredSize = cb.limit() + chars.length;
        if (requiredSize <= cb.capacity()) {
            cb.mark();
            cb.position(cb.limit());
            cb.limit(requiredSize);
            cb.put(chars);
            cb.reset();
        } else { // need reallocation
            char[] c = cb.array();
            int pos = cb.position();
            int len = cb.limit();
            cb = CharBuffer.allocate(requiredSize);
            cb.put(c, 0, len);
            Arrays.fill(c, (char) 0);
            cb.put(chars);
            cb.position(pos);
        }
        return cb;
    }

    /**
     * Append a single char to a buffer ; if the buffer is too small,
     * a new array is allocated and the previous one is cleared.
     *
     * @param cb The buffer.
     * @param ch The char to append.
     *
     * @return The buffer.
     */
    public static CharBuffer append(CharBuffer cb, char ch) {
        int requiredSize = cb.limit() + 1;
        if (requiredSize <= cb.capacity()) {
            cb.mark();
            cb.position(cb.limit());
            cb.limit(requiredSize);
            cb.put(ch);
            cb.reset();
        } else { // need reallocation
            char[] c = cb.array();
            int pos = cb.position();
            int len = cb.limit();
            cb = CharBuffer.allocate(requiredSize);
            cb.put(c, 0, len);
            Arrays.fill(c, (char) 0);
            cb.put(ch);
            cb.position(pos);
        }
        return cb;
    }

    /**
     * Get the actual chars from a buffer ; if the capacity
     * of this buffer is larger than the actual chars, a new
     * array is allocated and the previous one is cleared.
     *
     * @param cc The buffer.
     * @return The actual chars.
     */
    public static char[] getData(CharBuffer cc) {
        char[] chars;
        if (cc.remaining() == cc.capacity()) {
            chars = cc.array();
        } else { // larger than necessary
            chars = new char[cc.remaining()];
            cc.get(chars);
            Arrays.fill(cc.array(), (char) 0);
        }
        return chars;
    }

}
