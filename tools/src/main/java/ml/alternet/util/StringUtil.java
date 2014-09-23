package ml.alternet.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Iterator;

import ml.alternet.misc.WtfException;

/**
 * String utilities.
 *
 * @author Philippe Poulard
 */
@Util
public final class StringUtil {

    private StringUtil() {
    }

    private static final String HEXES = "0123456789ABCDEF";

    /**
     * Convert an array of bytes to an hexa string.
     *
     * @param raw
     *            An array of bytes.
     * @return The hexa string conversion of that bytes; each byte is
     *         represented with 2 hexa chars.
     */
    public static String getHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt(b & 0xF0) >> 4).append(HEXES.charAt(b & 0x0F));
        }
        return hex.toString();
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
     * @return The hexa string conversion of that bytes; each byte is
     *         represented with 2 hexa chars.
     */
    public static String getHex(byte[] raw, int start, int length) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * length);
        byte b = 0;
        for (int i = start; i < start + length; i++) {
            b = raw[i];
            hex.append(HEXES.charAt(b & 0xF0) >> 4).append(HEXES.charAt(b & 0x0F));
        }
        return hex.toString();
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
     * Compute a MD5 hash of a string.
     *
     * @param string
     *            The actual input string.
     * @return The MD5 hash in hexa.
     */
    public static String getHash(char[] string) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(convert(string));
            return getHex(hash);
        } catch (NoSuchAlgorithmException nsae) {
            throw WtfException.throwException(nsae);
        }
    }

    /**
     * Convert a char array to an array of bytes
     * 
     * @param string
     *            The char array.
     * @return The byte array.
     */
    public static byte[] convert(char[] string) {
        ByteBuffer bb = Charset.forName("UTF-8").encode(CharBuffer.wrap(string));
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return b;
    }

    /**
     * Convert a byte array to a char array
     * 
     * @param string
     *            The byte array.
     * @return The char array.
     */
    public static char[] convert(byte[] string) {
        CharBuffer cb = Charset.forName("UTF-8").decode(ByteBuffer.wrap(string));
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

    /**
     * Iterate on the unicode code points of a string (a code point is made of 1
     * or 2 chars).
     *
     * @param string
     *            The actual non-null string.
     * @return An iterator on its unicode code points.
     */
    public static Iterable<Integer> unicodeCodePoints(final String string) {
        return new Iterable<Integer>() {
            String text = string;

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int nextIndex = 0;

                    @Override
                    public boolean hasNext() {
                        return nextIndex < text.length();
                    }

                    @Override
                    public Integer next() {
                        int result = text.codePointAt(nextIndex);
                        nextIndex += Character.charCount(result);
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

}
