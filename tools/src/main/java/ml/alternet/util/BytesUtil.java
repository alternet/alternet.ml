package ml.alternet.util;

import java.io.ByteArrayOutputStream;
import java.util.stream.Collector;

import ml.alternet.misc.Thrower;

/**
 * Bytes-related utilities.
 *
 * @author Philippe Poulard
 */
@Util
public final class BytesUtil {

    private BytesUtil() { }

    /**
     * Byte-to-byte copy of an array of chars to an array of bytes without
     * conversion (a char contains 2 bytes).
     *
     * This method converts non-ASCII chars.
     *
     * @param chars
     *            The chars to cast.
     *
     * @return The same input, but as bytes.
     */
    public static byte[] cast(char[] chars) {
        byte[] bytes = new byte[chars.length << 1];
        for (int i = 0; i < chars.length; i++) {
            int pos = i << 1;
            bytes[pos] = (byte) ((chars[i] & 0xFF00) >> 8);
            bytes[pos + 1] = (byte) (chars[i] & 0x00FF);
        }
        return bytes;
    }

    /**
     * Byte-to-byte copy of an array of bytes to an array of chars without
     * conversion. (a char contains 2 bytes).
     *
     * This method converts from non-ASCII chars.
     *
     * @param bytes
     *            The bytes to cast.
     *
     * @return The same input, but as chars.
     */
    public static char[] cast(byte[] bytes) {
        char[] chars = new char[bytes.length >> 1];
        for (int i = 0; i < chars.length; i++) {
            int pos = i << 1;
            char c = (char) (((bytes[pos] & 0x00FF) << 8) + (bytes[pos + 1] & 0x00FF));
            chars[i] = c;
        }
        return chars;
    }

    /**
     * Byte-to-byte copy of an array of bytes to an array of chars without
     * conversion. (a char contains 2 bytes).
     *
     * This method converts from non-ASCII chars.
     *
     * @param bytes
     *            The bytes to cast.
     * @param offset The offset from which to start the cast.
     * @param len The number of bytes to cast.
     *
     * @return The same input, but as chars.
     */
    public static char[] cast(byte[] bytes, int offset, int len) {
        char[] chars = new char[len >> 1];
        for (int i = 0; i < chars.length; i++) {
            int pos = i << 1;
            char c = (char) (((bytes[offset + pos] & 0x00FF) << 8) + (bytes[offset + pos + 1] & 0x00FF));
            chars[i] = c;
        }
        return chars;
    }

    /**
     * Collect a stream of bytes to an array.
     *
     * @return A byte array collector.
     */
    public static Collector<Integer, ?, byte[]> toByteArray() {

        return Collector.of(ByteArrayOutputStream::new,
                ByteArrayOutputStream::write,
                (baos1, baos2) -> {
                    Thrower.safeCall(() -> baos2.writeTo(baos1));
                    return baos1;
                },
                ByteArrayOutputStream::toByteArray);
    }

}
