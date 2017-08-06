package ml.alternet.misc;

import java.nio.CharBuffer;

/**
 * A char sequence representation of a char array.
 *
 * Unlike {@link CharBuffer}, the underlying buffer
 * grows as necessary when characters are appended.
 *
 * @see CharBuffer#wrap(char[])
 *
 * @author Philippe Poulard
 */
public final class CharArray implements CharSequence, Comparable<CharSequence>, Appendable {

    private char[] chars;
    private int offset = 0;
    private int length;

    /**
     * Creates an empty character array.
     */
    public CharArray() {
        this(0);
    }

    /**
     * Creates a character array of specified default size.
     *
     * @param size The size.
     */
    public CharArray(int size) {
        this(new char[size]);
    }

    /**
     * Creates a character array from the specified String.
     *
     * @param string The string source.
     */
    public CharArray(String string) {
        this(string.toCharArray());
    }

    /**
     * Creates a character array from the specified char sequence.
     *
     * @param charSequence The char sequence source.
     */
    public CharArray(CharSequence charSequence) {
        this.length = 0;
        this.chars = new char[charSequence.length()];
        charSequence.chars().forEach(c -> this.chars[this.length++] = (char) c);
    }

    /**
     * Creates a character array from the specified array
     * without performing a copy.
     *
     * @param chars The char source.
     */
    public CharArray(char[] chars) {
        this.chars = chars;
        this.length = chars.length;
    }

    /**
     * Creates a character array from the specified array
     * without performing a copy.
     *
     * @param chars The char source.
     * @param offset The offset.
     * @param length The length.
     */
    public CharArray(char[] chars, int offset, int length) {
        this.chars = chars;
        this.offset = offset;
        this.length = length;
    }

    /**
     * Set this char array to the empty sequence.
     */
    public void clear() {
        this.offset = 0;
        this.length = 0;
    }

    /**
     * Returns the underlying array as is,
     * that is to say that the relevant characters
     * are starting at the offset and have a
     * specific length.
     *
     * @return The wrapped array.
     *
     * @see CharArray#offset()
     * @see CharArray#length()
     */
    public char[] array() {
        return this.chars;
    }

    /**
     * Returns the length of this character sequence.
     *
     * @return The number of characters.
     */
    @Override
    public int length() {
        return this.length;
    }

    /**
     * Returns the offset of the first character
     * in the underlying array.
     *
     * @return The offset.
     */
    public int offset() {
        return this.offset;
    }

    @Override
    public String toString() {
        return new String(this.chars, this.offset, this.length);
    }

    /**
     * Returns the hash code for this CharArray.
     *
     * <p> Note: Returns the same hashCode as <code>java.lang.String</code>
     *           (consistent with {@link #equals})</p>
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0, j = this.offset; i < this.length; i++) {
            h = 31 * h + this.chars[j++];
        }
        return h;
    }

    /**
     * Compares this character sequence against the specified object
     * (<code>String</code> or <code>CharSequence</code>).
     *
     * @param  that the object to compare with.
     * @return <code>true</code> if both objects represent the same sequence;
     *         <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object that) {
        if (that == null) {
            return false;
        } else if (that instanceof CharSequence) {
            CharSequence chars = (CharSequence) that;
            if (this.length != chars.length()) {
                return false;
            }
            for (int i = this.length, j = this.offset + this.length ; --i >= 0 ; ) {
                if (this.chars[--j] != chars.charAt(i)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public char charAt(int index) {
        return this.chars[this.offset + index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start < 0 || end < 0 || start > end || end > this.length()) {
            throw new IndexOutOfBoundsException();
        }
        return new CharArray(this.chars, this.offset + start, end - start);
    }

    @Override
    public int compareTo(CharSequence other) {
        int len = other.length();
        int lim = Math.min(this.length, len);
        for (int k = 0; k < lim; k++) {
            char c1 = this.chars[this.offset + k];
            char c2 = other.charAt(k);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return this.length - len;
    }

    @Override
    public Appendable append(CharSequence csq) {
        return append(csq, 0, csq.length());
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) {
        if (this.offset + this.length + end - start >= this.chars.length) {
            char[] newchars = new char[ this.length + end - start ];
            System.arraycopy( this.chars, this.offset, newchars, 0, this.length );
            this.chars = newchars;
            this.offset = 0;
        }
        for (int i = 0, j = this.offset + this.length; i < end - start ; i++) {
            this.chars[ j++ ] = csq.charAt(start + i);
        }
        this.length = this.length + end - start;
        return this;
    }

    @Override
    public Appendable append(char c) {
        if (this.offset + this.length + 1 >= this.chars.length) {
            char[] newchars = new char[ this.length + 1 ];
            System.arraycopy( this.chars, this.offset, newchars, 0, this.length );
            this.chars = newchars;
            this.offset = 0;
        }
        this.chars[this.offset + this.length++] = c;
        return this;
    }

}
