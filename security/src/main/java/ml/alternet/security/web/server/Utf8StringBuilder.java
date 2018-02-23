package ml.alternet.security.web.server;

import java.util.Arrays;

/**
 * UTF-8 StringBuilder that allow char extraction and cleaning.
 *
 * This class provides methods to append UTF-8 encoded bytes, that are converted into characters.
 *
 * This class is stateful and up to 4 calls to {@link #append(byte)} may be needed before
 * state a character is appended to the buffer.
 *
 * The UTF-8 decoding is done by this class and no additional buffers or Readers are used.
 */
public class Utf8StringBuilder extends Utf8Appendable {

    /**
     * A safe appendable clean unused data when growing.
     *
     * @author Philippe Poulard
     */
    static class SafeAppendable implements Appendable {

        char[] c;
        int len = 0;

        SafeAppendable(int capacity) {
            c = new char[Math.max(capacity, 32)];
        }

        SafeAppendable() {
            this(32);
        }

        @Override
        public Appendable append(CharSequence csq) {
            return append(csq, 0, csq.length());
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) {
            ensureCapacity(end - start);
            csq.chars().forEach(ch -> c[len++] = (char) ch);
            return this;
        }

        @Override
        public Appendable append(char ch) {
            ensureCapacity(1);
            c[len++] = ch;
            return this;
        }

        public Appendable append(char[] chars) {
            ensureCapacity(chars.length);
            for (int i = 0 ; i < chars.length ; i++) {
                c[len++] = chars[i];
            }
            return this;
        }

        public int length() {
            return len;
        }

        public void reset() {
            Arrays.fill(c, (char) 0);
            len = 0;
        }

        public void clear() {
            Arrays.fill(c, (char) 0);
        }

        public char[] toChars() {
            if (len != c.length) {
                char[] chars = new char[len];
                System.arraycopy(c, 0, chars, 0, len);
                return chars;
            }
            return c;
        }

        void ensureCapacity(int size) {
            if (len + size > c.length) {
                char[] chars = new char[Math.min(Math.max(c.length * 2, len + size), Integer.MAX_VALUE - 5)];
                System.arraycopy(c, 0, chars, 0, len);
                clear();
                c = chars;
            }
        }

        @Override
        public String toString() {
            return new String(toChars());
        }

    }

    /**
     * Create an Utf8StringBuilder
     */
    public Utf8StringBuilder() {
        super(new SafeAppendable());
    }

    /**
     * Create an Utf8StringBuilder
     *
     * @param capacity The initial capacity
     */
    public Utf8StringBuilder(int capacity) {
        super(new SafeAppendable(capacity));
    }

    /**
     * Return the content as chars
     *
     * @return A char array of the UTF-8 encoded content.
     */
    public char[] toChars() {
        checkState();
        return ((SafeAppendable) _appendable).toChars();
    }

    /**
     * Clear all the char in this content.
     */
    public void clear() {
        ((SafeAppendable) _appendable).clear();
    }

    @Override
    public int length() {
        return ((SafeAppendable) _appendable).length();
    }

    @Override
    public void reset() {
        super.reset();
        ((SafeAppendable) _appendable).reset();
    }

    @Override
    public String toString() {
        checkState();
        return ((SafeAppendable) _appendable).toString();
    }

    /**
     * Append some chars
     *
     * @param chars The chars to append
     */
    public void append(char[] chars) {
        ((SafeAppendable) _appendable).append(chars);
    }

    /**
     * Append a single char
     *
     * @param c The char to append
     */
    public void append(char c) {
        ((SafeAppendable) _appendable).append(c);
    }

}
