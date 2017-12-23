package ml.alternet.security.binary;

import java.util.Iterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ml.alternet.misc.CharRange;

/**
 * Out-of-the-box bytes encoders.
 *
 * @author Philippe Poulard
 */
public enum BytesEncoder implements BytesEncoding {

    /** Represent bytes in base 64 string. */
    base64(new Base64(ValueSpace.base64, Base64.PaddingMode.PADDING)),

    /** Represent bytes in base 64 string, without padding. */
    base64_no_padding(new Base64(ValueSpace.base64, Base64.PaddingMode.NO_PADDING)),

    /** Adapted base 64 represent bytes in base 64 string, except that it uses . instead of +, and omits trailing padding = and whitespace. */
    abase64(new Base64(ValueSpace.abase64, Base64.PaddingMode.PADDING)),

    /** Represent bytes in BCrypt's base 64 string. */
    bcrypt64(new Base64(ValueSpace.bcrypt64, Base64.PaddingMode.NO_PADDING)),

    /** Represent bytes in base 64 string, but mapped to the alphabet : [./0-9A-Za-z] */
    h64(new Base64(ValueSpace.h64, Base64.PaddingMode.NO_PADDING)),

    /**
     * Represent bytes in base 64 string, but mapped to the alphabet : [./0-9A-Za-z],
     * high bits of last sextets/bytes are skipped.
     */
    h64be(new Base64(ValueSpace.h64, Base64.PaddingMode.NO_PADDING_SKIP_HIGH_BITS)),

    /** Represent bytes in uppercase hexa string. */
    HEXA(new Hexa(true)),

    /** Represent bytes in lowercase hexa string. */
    hexa(new Hexa(false)),

    /**
     * Bytes are left as-is (without conversion), a char contains 2 bytes.
     */
    none(new BytesEncoding() {

        @Override
        public CharRange valueSpace() {
            return CharRange.ANY;
        }

        @Override
        public String name() {
            return "none";
        }

        @Override
        public Stream<Character> encode(IntStream bytes) {
            OfInt it = bytes.iterator();
            return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<Character>(Long.MAX_VALUE,
                    Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {
                        @Override
                        public boolean tryAdvance(Consumer<? super Character> action) {
                            if (it.hasNext()) {
                                int i1 = it.nextInt();
                                int i2 = it.nextInt();
                                char c = (char) (((i1 & 0x00FF) << 8) + (i2 & 0x00FF));
                                action.accept(c);
                                return true;
                            } else {
                                return false;
                            }
                        }
                }, false
            );
        }

        @Override
        public IntStream decode(Stream<Character> data) {
            Iterator<Character> it = data.iterator();
            return StreamSupport.intStream(
                new Spliterators.AbstractIntSpliterator(Long.MAX_VALUE,
                        Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {

                    byte[] b = new byte[2];
                    int n = 0;
                    int i = 0;

                    @Override
                    public boolean tryAdvance(IntConsumer action) {
                        if (n == 0) {
                            if (it.hasNext()) {
                                char c = it.next();
                                b[n++] = (byte) ((c & 0xFF00) >> 8);
                                b[n++] = (byte) (c & 0x00FF);
                            } else {
                                return false;
                            }
                        }
                        action.accept(b[i++]);
                        if (i == n) {
                            n = 0;
                            i = 0;
                        }
                        return true;
                    }
                }, false
            );
        }
    });

    private static class Hexa implements BytesEncoding {

        boolean uppercase;

        Hexa(boolean uppercase) {
            this.uppercase = uppercase;
        }

        @Override
        public String name() {
            return this.uppercase ? "HEXA" : "hexa";
        }

        @Override
        public CharRange valueSpace() {
            // "0123456789ABCDEF"
            // "0123456789abcdef"
            return CharRange.range('0', '9').union(
                    CharRange.range(this.uppercase ? 'A' : 'a', this.uppercase ? 'F' : 'f')
            );
        }

        @Override
        public Stream<Character> encode(IntStream bytes) {
            OfInt it = bytes.iterator();
            char[] h = this.uppercase ? ValueSpace.HEXA.get() : ValueSpace.hexa.get();
            return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<Character>(Long.MAX_VALUE,
                    Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {

                        char[] c = new char[2];
                        int n = 0;
                        int i = 0;

                        @Override
                        public boolean tryAdvance(Consumer<? super Character> action) {
                            if (n == 0) {
                                if (it.hasNext()) {
                                    int v = it.nextInt() & 0xFF;
                                    c[n++] = h[v >>> 4];
                                    c[n++] = h[v & 0x0F];
                                } else {
                                    return false;
                                }
                            }
                            action.accept(c[i++]);
                            if (i == n) {
                                n = 0;
                                i = 0;
                            }
                            return true;
                        }
                }, false
            );
        }

        @Override
        public IntStream decode(Stream<Character> data) {
            Iterator<Character> it = data.iterator();
            return StreamSupport.intStream(
                new Spliterators.AbstractIntSpliterator(Long.MAX_VALUE,
                        Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {

                    private int hexToBin(char ch) {
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

                    @Override
                    public boolean tryAdvance(IntConsumer action) {
                        if (it.hasNext()) {
                            int h = hexToBin(it.next());
                            int l = hexToBin(it.next());
                            if (h == -1 || l == -1) {
                                throw new IllegalArgumentException("contains illegal character for hexBinary: " + (h == -1 ? h:l) );
                            }
                            action.accept(h * 16 + l);
                            return true;
                        } else {
                            return false;
                        }
                    }
                }, false
            );
        }

    }

    static class Base64 implements BytesEncoding {

        static enum PaddingMode {
            PADDING, // ends incomplete sequences with '='
            NO_PADDING, // shift the bits like with padding
            NO_PADDING_SKIP_HIGH_BITS; // left as-is
        }

        CharRange encodeChars;
        char[] encodeMap;
        byte[] decodeMap = new byte[128];
        PaddingMode padding;
        char padChar;
        String name = "base64";
        ValueSpace vs;

        private static final byte PADDING = 127;

        Base64(ValueSpace valueSpace, PaddingMode withPadding) {
            this(valueSpace.get(), withPadding);
            this.vs = valueSpace;
        }

        Base64(char[] encodeMap, PaddingMode withPadding) {
            this(encodeMap, withPadding, '=');
        }

        Base64(char[] encodeMap, PaddingMode withPadding, char padChar) {
            this.encodeMap = encodeMap;
            this.padding = withPadding;
            this.padChar = padChar;

            // prepare reverse map
            for (int i = 0; i < 128; i++) {
                this.decodeMap[i] = -1;
            }
            for (byte i = 0; i < encodeMap.length; i++) {
                this.decodeMap[encodeMap[i]] = i;
            }
            if (this.padding == PaddingMode.PADDING) {
                this.decodeMap[this.padChar] = PADDING;
            }
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public CharRange valueSpace() {
            if (this.encodeChars == null) {
                this.encodeChars = CharRange.isOneOf(new String(this.encodeMap));
            }
            return this.encodeChars;
        }

        @Override
        public IntStream decode(Stream<Character> data) {
            Iterator<Character> it = data.iterator();
            return StreamSupport.intStream(
                    new Spliterators.AbstractIntSpliterator(Long.MAX_VALUE,
                            Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {

                        byte[] b = new byte[3];
                        int n = 0;
                        int i = 0;
                        int q = 0;
                        byte[] quadruplet = new byte[4];

                        void decode(int d) {
                            char c = it.next();
                            byte v = decodeMap[c];
                            if (v != -1) {
                                quadruplet[q++] = v;
                            }
                            if (--d > 0 && it.hasNext()) {
                                decode(d);
                            }
                        }

                        @Override
                        public boolean tryAdvance(IntConsumer action) {
                            if (n == 0) {
                                if (it.hasNext()) {
                                    decode(4); // decode up to 4
                                    if (q == 4) {
                                        // quadruplet is now filled.
                                        b[n++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1] >> 4));
                                        if (quadruplet[2] != PADDING) {
                                            b[n++] = (byte) ((quadruplet[1] << 4) | (quadruplet[2] >> 2));
                                        }
                                        if (quadruplet[3] != PADDING) {
                                            b[n++] = (byte) ((quadruplet[2] << 6) | (quadruplet[3]));
                                        }
                                        q = 0;
                                    } else
                                        // when no padding :
                                        // if (q==1) {
                                                // incomplete, should not occur
                                                // b[n++] = (byte) (quadruplet[0] << 2);
                                    if (q == 2) {
                                        if (padding == PaddingMode.NO_PADDING_SKIP_HIGH_BITS) {
                                            // mode for $6$
                                            b[n++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1]));
                                        } else {
                                            b[n++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1] >> 4));
                                        }
                                        // next byte incomplete, this is why it is skipped
                                        // b[n++] = (byte) (quadruplet[1] << 4);
                                    } else if ( q==3 ) {
                                        b[n++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1] >> 4));
                                        if (padding == PaddingMode.NO_PADDING_SKIP_HIGH_BITS) {
                                            // mode for $5$
                                            b[n++] = (byte) ((quadruplet[1] << 4) | (quadruplet[2] ));
                                        } else {
                                            b[n++] = (byte) ((quadruplet[1] << 4) | (quadruplet[2] >> 2));
                                        }
                                        // next byte incomplete, this is why it is skipped
                                        // b[n++] = (byte) (quadruplet[2] << 6);
                                    }
                                } else {
                                    return false;
                                }
                            }
                            action.accept(b[i++]);
                            if (i == n) {
                                n = 0;
                                i = 0;
                            }
                            return true;
                        }
            }, false);
        }

        @Override
        public Stream<Character> encode(IntStream bytes) {
            OfInt it = bytes.iterator();
            return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<Character>(Long.MAX_VALUE,
                    Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {

                        char[] c = new char[4];
                        int n = 0;
                        int i = 0;

                        char encode(int i) {
                            return encodeMap[i & 0x3F];
                        }

                        @Override
                        public boolean tryAdvance(Consumer<? super Character> action) {
                            if (n == 0) {
                                if (it.hasNext()) {
                                    int i1 = it.nextInt();
                                    if (it.hasNext()) {
                                        int i2 = it.nextInt();
                                        if (it.hasNext()) {
                                            int i3 = it.nextInt();
                                            c[n++] = encode(i1 >> 2);
                                            c[n++] = encode(
                                                    ((i1 & 0x3) << 4)
                                                    | ((i2 >> 4) & 0xF));
                                            c[n++] = encode(
                                                    ((i2 & 0xF) << 2)
                                                    | (i3 >> 6) & 0x3);
                                            c[n++] = encode(i3 & 0x3F);
                                        } else {
                                            // encode when exactly 2 bytes (left) to encode
                                            c[n++] = encode(i1 >> 2);
                                            c[n++] = encode(((i1 & 0x3) << 4)
                                                    | (i2 >> 4) & 0xF);
                                            if (padding == PaddingMode.NO_PADDING_SKIP_HIGH_BITS) {
                                                // mode for $5$
                                                c[n++] = encode(i2 & 0xF);
                                            } else {
                                                c[n++] = encode((i2 & 0xF) << 2);
                                            }
                                            if (padding == PaddingMode.PADDING) {
                                                c[n++] = padChar;
                                            }
                                        }
                                    } else {
                                        // encode when exactly 1 byte (left) to encode
                                        c[n++] = encode(i1 >> 2);
                                        if (padding == PaddingMode.NO_PADDING_SKIP_HIGH_BITS) {
                                            // mode for $6$
                                            c[n++] = encode(i1 & 0x3);
                                        } else {
                                            c[n++] = encode((i1 & 0x3) << 4);
                                        }
                                        if (padding == PaddingMode.PADDING) {
                                            c[n++] = padChar;
                                            c[n++] = padChar;
                                        }
                                    }
                                } else {
                                    return false;
                                }
                            }
                            action.accept(c[i++]);
                            if (i == n) {
                                n = 0;
                                i = 0;
                            }
                            return true;
                        }
                }, false);
        }

    }

    /**
     * Common encoding value spaces.
     *
     * @author Philippe Poulard
     */
    public enum ValueSpace {
        /** Represent bytes in base 64 string : [A-Za-z0-9+/] */
        base64(   "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"),
        /** Represent bytes in base 64 string : [A-Za-z0-9-_], see RFC 4648 Table 2. */
        base64url("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"),
        /** Adapted base 64 represent bytes in base 64 string, except that it uses '.' instead of '+' : [A-Za-z0-9./] */
        abase64(  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789./"),
        /** Represent bytes in BCrypt's base 64 string : [./A-Za-z0-9]  */
        bcrypt64( "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"),
        /** Represent bytes in base 64 string, but mapped to the alphabet : [./0-9A-Za-z] */
        h64(      "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"),
        /** Represent bytes in hexa lowercase : [0-9a-f] */
        hexa("0123456789abcdef"),
        /** Represent bytes in hexa uppercase : [0-9A-F]  */
        HEXA("0123456789ABCDEF");

        String chars;

        private ValueSpace(String chars) {
            this.chars = chars;
        }

        /**
         * Get the characters in the value space.
         *
         * @return The characters.
         */
        public char[] get() {
            return this.chars.toCharArray();
        }

        /**
         * Return the ordered set of legal characters in the encoding form.
         *
         * Warning : use only for small ranges of value space !!!
         *
         * @param byteEncoding The byte encoding for extracting characters.
         *
         * @return The set of the legal characters in character order.
         *
         * @see BytesEncoding#valueSpace()
         */
        public static char[] valueSpace(BytesEncoding byteEncoding) {
            if (byteEncoding instanceof Base64) {
                Base64 b64 = (Base64) byteEncoding;
                if (b64.vs != null) {
                    return b64.vs.get();
                }
            }
            try {
                return ValueSpace.valueOf(byteEncoding.name()).get();
            } catch (IllegalArgumentException e) {
                return byteEncoding.valueSpace()
                        .asIntervals()
                        .flatMapToInt(br -> br.characters())
                        .mapToObj(c -> new String(Character.toChars(c)))
                        .collect(Collectors.joining()).toCharArray();
            }
        }

    };

    private BytesEncoding bytesEncoding;

    BytesEncoder(BytesEncoding encoder) {
        this.bytesEncoding = encoder;
        if (encoder instanceof Base64) {
            ((Base64) encoder).name = name();
        }
    }

    @Override
    public String encode(byte[] data, int offset, int len) {
        return this.bytesEncoding.encode(data, offset, len);
    }

    @Override
    public byte[] decode(String data) {
        return this.bytesEncoding.decode(data);
    }

    @Override
    public CharRange valueSpace() {
        return this.bytesEncoding.valueSpace();
    }

    @Override
    public Stream<Character> encode(IntStream bytes) {
        return this.bytesEncoding.encode(bytes);
    }

    @Override
    public IntStream decode(Stream<Character> data) {
        return this.bytesEncoding.decode(data);
    }

}