package ml.alternet.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import ml.alternet.misc.WtfException;

/**
 * String utilities.
 *
 * @author Philippe Poulard
 */
@Util
public final class StringUtil {

    private StringUtil() { }

    private static final char[] HEXES = "0123456789ABCDEF".toCharArray();
    private static final char[] hexes = "0123456789abcdef".toCharArray();

    /**
     * Convert an array of bytes to an hexa string.
     *
     * @param raw
     *            An array of bytes.
     * @param uppercase <code>true</code> to write hexa chars in uppercase
     *          <code>false</false> to write hexa chars in lowercase
     * @return The hexa string conversion of that bytes; each byte is
     *         represented with 2 hexa chars.
     */
    public static String getHex(byte[] raw, boolean uppercase) {
        return getHex(raw, 0, raw.length, uppercase);
    }

    /**
     * Convert an array of bytes to an hexa string.
     *
     * @param raw
     *            An array of bytes.
     * @param start
     *            The start offset.
     * @param length
     *            The length of bytes to convert.
     * @param uppercase <code>true</code> to write hexa chars in uppercase
     *          <code>false</false> to write hexa chars in lowercase
     * @return The hexa string conversion of that bytes; each byte is
     *         represented with 2 hexa chars.
     */
    public static String getHex(byte[] raw, int start, int length, boolean uppercase) {
        if (raw == null) {
            return null;
        }
        char[] hexChars = new char[length * 2];
        char[] h = uppercase ? HEXES : hexes;
        int v = -1;
        for (int i = 0; i < length; i++) {
            v = raw[i + start] & 0xFF;
            hexChars[i * 2] = h[v >>> 4];
            hexChars[i * 2 + 1] = h[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Compute a MD5 hash of a string.
     *
     * @param string
     *            The actual input string.
     * @return The MD5 hash in hexa.
     */
    public static String getHash(String string) {
        return getHash(string.toCharArray());
    }

    /**
     * Convert an hexa string to bytes.
     *
     * @param string The hexa string to convert
     * @return The bytes
     */
    public static byte[] hexToBin(char[] string) {
        final int len = string.length;
        // "111" is not a valid hex encoding.
        if (len % 2 != 0) {
            throw new IllegalArgumentException("hexBinary needs to be even-length: " + new String(string));
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int h = hexToBin(string[i]);
            int l = hexToBin(string[i + 1]);
            if (h == -1 || l == -1) {
                throw new IllegalArgumentException("contains illegal character for hexBinary: " + new String(string));
            }
            out[i / 2] = (byte) (h * 16 + l);
        }
        return out;
    }

    private static int hexToBin(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if ('a' <= ch && ch <= 'f') {
            return ch - 'a' + 10;
        }
        return -1;
    }

    /**
     * Compute a MD5 hash of a string.
     *
     * @param string
     *            The actual input string.
     * @return The MD5 hash in hexa.
     */
    public static String getHash(char[] string) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(convert(string));
            return getHex(hash, false);
        } catch (NoSuchAlgorithmException nsae) {
            throw WtfException.throwException(nsae);
        }
    }

    /**
     * Convert a char array to an array of bytes with an UTF-8 encoding.
     *
     * @param string
     *            The char array.
     * @return The byte array.
     */
    public static byte[] convert(char[] string) {
        ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(string));
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return b;
    }

    /**
     * Convert a byte array to a char array with an UTF-8 decoding.
     *
     * @param string
     *            The byte array.
     * @return The char array.
     */
    public static char[] convert(byte[] string) {
        CharBuffer cb = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(string));
        char[] c = new char[cb.remaining()];
        cb.get(c);
        return c;
    }

    /**
     * Returns the index within a string of the first occurrence of the
     * specified substring. If no such value of <i>k</i> exists, then {@code -1}
     * is returned.
     *
     * @param string
     *            The substring to search for.
     * @param chars
     *            The characters of the substring to search.
     * @return The index of the first occurrence of the specified substring, or
     *         {@code -1} if there is no such occurrence.
     */
    public static int indexOf(String string, char[] chars) {
        if (chars == null || chars.length == 0) {
            return -1;
        } else {
            int idx = string.indexOf(chars[0]);
            if (idx == -1 || chars.length == 1) {
                return idx;
            } else {
                for (int i = 1; i < chars.length; i++) {
                    if (string.charAt(idx + i) != chars[i]) {
                        return -1;
                    }
                }
                return idx;
            }
        }
    }

    /**
     * Indicates whether a string is void or undefined.
     *
     * @param string
     *            The string to test.
     *
     * @return <tt>true</tt> if the string is <tt>null</tt> or has no character,
     *         <tt>false</tt> otherwise.
     */
    public static boolean isVoid(String string) {
        return string == null || string.length() == 0;
    }

    /**
     * Remove diacritics from a string, e.g. "côté" gives "cote".
     *
     * @param input
     *            The string to process.
     *
     * @return A string without diacritics.
     */
    public static String removeDiacritics(String input) {
        // Normalizer.normalise() converts each accented
        // character into 1 non-accented character followed
        // by 1 or more characters representing the accent(s)
        // alone. These characters representing only
        // an accent belong to the Unicode category
        // CombiningDiacriticalMarks. The call to replaceAll
        // strips out all characters in that category.
        String normalized = Normalizer.normalize(input, Form.NFKD);
        String cleared = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return cleared;
    }

}
