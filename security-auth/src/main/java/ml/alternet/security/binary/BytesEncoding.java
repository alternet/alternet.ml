package ml.alternet.security.binary;

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
     * @return The encoded string.
     */
    String encode(byte[] data, int offset, int len);

    /**
     * Decode a string representation of bytes to bytes
     *
     * @param data The encoded string.
     * @return The bytes.
     */
    byte[] decode(String data);

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
