package ml.alternet.security.binary;

import java.io.InputStream;
import java.io.Reader;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ml.alternet.io.IOUtil;
import ml.alternet.misc.CharRange;

/**
 * Encode/decode bytes to base 64 or hexa strings.
 *
 * @author Philippe Poulard
 */
public interface BytesEncoding {

    /**
     * Encode bytes to string.
     *
     * @param data The bytes.
     * @return The encoded string.
     */
    default String encode(byte[] data) {
        return encode(data, 0, data.length);
    };

    /**
     * Encode bytes to string.
     *
     * @param data The bytes.
     * @param offset The offset of the first byte to encode.
     * @param len The length of data to encode.
     *
     * @return The encoded string.
     */
    default String encode(byte[] data, int offset, int len) {
        return encode(IntStream.range(offset, offset + len).map(i -> data[i]))
                .collect(StringBuilder::new,
                        (sb, c) -> sb.append((char) c),
                        StringBuilder::append)
                .toString();
    }

    /**
     * Encode bytes to characters.
     *
     * @param bytes The bytes.
     *
     * @return The encoded characters.
     */
    default Stream<Character> encode(InputStream bytes) {
        return encode(IOUtil.asStream(bytes));
    }

    /**
     * Encode bytes to characters.
     *
     * @param bytes The bytes.
     *
     * @return The encoded characters.
     */
    Stream<Character> encode(IntStream bytes);

//    public Stream<Character> encode(IntStream bytes) {
//        int[] ints = bytes.toArray();
//        byte[] b = new byte[ints.length];
//        for (int i = 0; i < ints.length; i++) {
//            b[i] = (byte) ints[i];
//        }
//        return encode(b).chars().mapToObj(i -> (Character) (char) i);
//    }

    /**
     * Decode a string representation of bytes to bytes
     *
     * @param data The encoded string.
     *
     * @return The bytes.
     */
    default byte[] decode(String data) {
        int[] ints = decode(data.chars().mapToObj(c -> (Character) (char) c))
            .toArray();
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i];
        }
        return bytes;
    }

    /**
     * Decode a character representation of bytes to bytes
     *
     * @param data The encoded characters.
     *
     * @return The bytes.
     */
     default IntStream decode(Reader data) {
        return decode(IOUtil.asStream(data));
    }

     /**
      * Decode a character representation of bytes to bytes
      *
      * @param data The encoded characters.
      *
      * @return The bytes.
      */
    IntStream decode(Stream<Character> data);

//    public IntStream decode(Stream<Character> data) {
//        byte[] bytes = decode(
//                data.collect(
//                        StringBuilder::new,
//                        (sb, c) -> sb.append((char) c),
//                        StringBuilder::append).toString()
//                );
//        return IntStream.range(0, bytes.length).map(i -> bytes[i]);
//    }

    /**
     * Return the name of the encoding.
     *
     * @return The name.
     */
    String name();

    /**
     * Return the set of legal characters in the encoding form.
     *
     * @return The set of the legal characters.
     */
    CharRange valueSpace();

}
