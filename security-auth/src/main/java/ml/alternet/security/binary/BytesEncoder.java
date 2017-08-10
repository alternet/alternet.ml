package ml.alternet.security.binary;

import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import ml.alternet.misc.CharRange;
import ml.alternet.util.BytesUtil;
import ml.alternet.util.StringUtil;

/**
 * Out-of-the-box bytes encoders.
 *
 * @author Philippe Poulard
 */
public enum BytesEncoder implements BytesEncoding {

    /** Represent bytes in base 64 string. */
    base64 {

        BytesEncoder.Base64 B64 = new Base64(ValueSpace.base64.get(), PaddingMode.PADDING);

        @Override
        public String encode(byte[] data, int offset, int len) {
            return B64.encode(data, offset, len);
        }

        @Override
        public byte[] decode(String data) {
            return B64.decode(data);
        }

        @Override
        public CharRange valueSpace() {
            return B64.valueSpace();
        }
    },

    /** Adapted base 64 represent bytes in base 64 string, except that it uses . instead of +, and omits trailing padding = and whitespace. */
    abase64 {

        BytesEncoder.Base64 B64 = new Base64(ValueSpace.abase64.get(), PaddingMode.PADDING);

        @Override
        public String encode(byte[] data, int offset, int len) {
            return B64.encode(data, offset, len);
        }

        @Override
        public byte[] decode(String data) {
            return B64.decode(data);
        }

        @Override
        public CharRange valueSpace() {
            return B64.valueSpace();
        }
    },

    /** Represent bytes in BCrypt's base 64 string. */
    bcrypt64 {

        BytesEncoder.Base64 B64 = new Base64(ValueSpace.bcrypt64.get(), PaddingMode.NO_PADDING);

        @Override
        public String encode(byte[] data, int offset, int len) {
            return B64.encode(data, offset, len);
        }

        @Override
        public byte[] decode(String data) {
            return B64.decode(data);
        }

        @Override
        public CharRange valueSpace() {
            return B64.valueSpace();
        }
    },

    /** Represent bytes in base 64 string, but mapped to the alphabet : [./0-9A-Za-z] */
    h64 {

        BytesEncoder.Base64 B64 = new Base64(Base64.H64, PaddingMode.NO_PADDING);

        @Override
        public String encode(byte[] data, int offset, int len) {
            return B64.encode(data, offset, len);
        }

        @Override
        public byte[] decode(String data) {
            return B64.decode(data);
        }

        @Override
        public CharRange valueSpace() {
            return B64.valueSpace();
        }
    },

    /**
     * Represent bytes in base 64 string, but mapped to the alphabet : [./0-9A-Za-z],
     * high bits of last sextets/bytes are skipped.
     */
    h64be {

        BytesEncoder.Base64 B64 = new Base64(Base64.H64, PaddingMode.NO_PADDING_SKIP_HIGH_BITS);

        @Override
        public String encode(byte[] data, int offset, int len) {
            return B64.encode(data, offset, len);
        }

        @Override
        public byte[] decode(String data) {
            return B64.decode(data);
        }

        @Override
        public CharRange valueSpace() {
            return B64.valueSpace();
        }
    },

    /** Represent bytes in hexa string. */
    hexa {
        @Override
        public String encode(byte[] data, int offset, int len) {
            return StringUtil.getHex(data, offset, len);
        }

        @Override
        public byte[] decode(String data) {
            return DatatypeConverter.parseHexBinary(data);
        }

        @Override
        public CharRange valueSpace() {
            // "0123456789ABCDEF"
            return CharRange.range('0', '9').union(CharRange.range('A', 'F'));
        }
    },

    /** Represent bytes in hexa string. */
    hexaLower {
        @Override
        public String encode(byte[] data, int offset, int len) {
            return StringUtil.getHex(data, offset, len).toLowerCase();
        }

        @Override
        public byte[] decode(String data) {
            return DatatypeConverter.parseHexBinary(data);
        }

        @Override
        public CharRange valueSpace() {
            // 0123456789abcdef
            return CharRange.range('0', '9').union(CharRange.range('a', 'f'));
        }
    },

    /**
     * Auto detect the encoding (hexa, base64 or none) ;
     * encode in base64.
     */
    auto {
        @Override
        public String encode(byte[] data, int offset, int len) {
            return base64.encode(data, offset, len);
        }

        @Override
        public byte[] decode(String data) {
            try {
                return hexa.decode(data);
            } catch (IllegalArgumentException e1) {
                try {
                    return base64.decode(data);
                } catch (IllegalArgumentException e2) {
                    return none.decode(data);
                }
            }
        }

        @Override
        public CharRange valueSpace() {
            return base64.valueSpace();
        }
    },

    /**
     * Bytes are left as-is.
     *
     * @see BytesUtil#cast(byte[])
     * @see BytesUtil#cast(char[])
     */
    none {
        @Override
        public String encode(byte[] data, int offset, int len) {
            return new String(BytesUtil.cast(data, offset, len));
        }

        @Override
        public byte[] decode(String data) {
            return BytesUtil.cast(data.toCharArray());
        }

        @Override
        public CharRange valueSpace() {
            return CharRange.ANY;
        }
    };

    private static enum PaddingMode {
        PADDING, // ends incomplete sequences with '='
        NO_PADDING, // shift the bits like with padding
        NO_PADDING_SKIP_HIGH_BITS; // left as-is
    }

    private static class Base64 implements BytesEncoding {

        final static char[] H64 = ValueSpace.h64.get();

        CharRange encodeChars;
        char[] encodeMap;
        byte[] decodeMap;
        PaddingMode padding;

        private static final byte PADDING = 127;

        Base64(char[] encodeMap, PaddingMode withPadding) {
            this.encodeMap = encodeMap;
            this.padding = withPadding;

            // prepare reverse map
            this.decodeMap = new byte[128];
            for (int i = 0; i < 128; i++) {
                this.decodeMap[i] = -1;
            }
            for (byte i = 0; i < encodeMap.length; i++) {
                this.decodeMap[encodeMap[i]] = i;
            }
            if (this.padding == PaddingMode.PADDING) {
                this.decodeMap['='] = PADDING;
            }
        }

        char encode(int i) {
            return encodeMap[i & 0x3F];
        }

        @Override
        public String encode(byte[] data, int offset, int len) {
            char[] buf = new char[((len + 2) / 3) * 4];
            int ptr = 0;
            // encode elements until only 1 or 2 elements are left to encode
            int remaining = len;
            int i;
            for (i = offset;remaining >= 3; remaining -= 3, i += 3) {
                buf[ptr++] = encode(data[i] >> 2);
                buf[ptr++] = encode(
                        ((data[i] & 0x3) << 4)
                        | ((data[i + 1] >> 4) & 0xF));
                buf[ptr++] = encode(
                        ((data[i + 1] & 0xF) << 2)
                        | ((data[i + 2] >> 6) & 0x3));
                buf[ptr++] = encode(data[i + 2] & 0x3F);
            }
            // encode when exactly 1 element (left) to encode
            if (remaining == 1) {
                buf[ptr++] = encode(data[i] >> 2);
                if (this.padding == PaddingMode.NO_PADDING_SKIP_HIGH_BITS) {
                    // mode for $6$
                    buf[ptr++] = encode(((data[i]) & 0x3));
                } else {
                    buf[ptr++] = encode(((data[i]) & 0x3) << 4);
                }
                if (this.padding == PaddingMode.PADDING) {
                    buf[ptr++] = '=';
                    buf[ptr++] = '=';
                }
            }
            // encode when exactly 2 elements (left) to encode
            if (remaining == 2) {
                buf[ptr++] = encode(data[i] >> 2);
                buf[ptr++] = encode(((data[i] & 0x3) << 4)
                        | ((data[i + 1] >> 4) & 0xF));
                if (this.padding == PaddingMode.NO_PADDING_SKIP_HIGH_BITS) {
                    // mode for $5$
                    buf[ptr++] = encode((data[i + 1] & 0xF) );
                } else {
                    buf[ptr++] = encode((data[i + 1] & 0xF) << 2);
                }
                if (this.padding == PaddingMode.PADDING) {
                    buf[ptr++] = '=';
                }
            }
            return new String(buf, 0, ptr);
        }

        @Override
        public byte[] decode(String text) {
            final int buflen = guessLength(text);
            final byte[] out = new byte[buflen];
            int o = 0;

            final int len = text.length();
            int i;

            final byte[] quadruplet = new byte[4];
            int q = 0;

            // convert each quadruplet to three bytes.
            for (i = 0; i < len; i++) {
                char ch = text.charAt(i);
                byte v = decodeMap[ch];

                if (v != -1) {
                    quadruplet[q++] = v;
                }

                if (q == 4) {
                    // quadruplet is now filled.
                    out[o++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1] >> 4));
                    if (quadruplet[2] != PADDING) {
                        out[o++] = (byte) ((quadruplet[1] << 4) | (quadruplet[2] >> 2));
                    }
                    if (quadruplet[3] != PADDING) {
                        out[o++] = (byte) ((quadruplet[2] << 6) | (quadruplet[3]));
                    }
                    q = 0;
                }
            }

            // when no padding :

            // if (q==1) {
                // incomplete, should not occur
                // out[o++] = (byte) (quadruplet[0] << 2);
            if (q==2) {
                if (this.padding == PaddingMode.NO_PADDING_SKIP_HIGH_BITS) {
                    // mode for $6$
                    out[o++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1]));
                } else {
                    out[o++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1] >> 4));
                }
                // next byte incomplete, this is why it is skipped
                // out[o++] = (byte) (quadruplet[1] << 4);
            } else if (q==3) {
                out[o++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1] >> 4));
                if (this.padding == PaddingMode.NO_PADDING_SKIP_HIGH_BITS) {
                    // mode for $5$
                    out[o++] = (byte) ((quadruplet[1] << 4) | (quadruplet[2] ));
                } else {
                    out[o++] = (byte) ((quadruplet[1] << 4) | (quadruplet[2] >> 2));
                }
                // next byte incomplete, this is why it is skipped
                // out[o++] = (byte) (quadruplet[2] << 6);
            }

            if (buflen == o) {// speculation worked out to be OK
                return out;
            }

            // we overestimated, so need to create a new buffer
            byte[] nb = new byte[o];
            System.arraycopy(out, 0, nb, 0, o);
            return nb;
        }

        int guessLength(String text) {
            final int len = text.length();

            // compute the tail '=' chars
            int j = len - 1;
            for (; j >= 0; j--) {
                byte code = decodeMap[text.charAt(j)];
                if (code == PADDING) {
                    continue;
                }
                if (code == -1) // most likely this base64 text is indented. go with the upper bound
                {
                    return 3 * len / 4;
                }
                break;
            }

            j++;    // text.charAt(j) is now at some base64 char, so +1 to make it the size
            int padSize = len - j;
            if (padSize > 2) // something is wrong with base64. be safe and go with the upper bound
            {
                return 3 * len / 4;
            }

            // so far this base64 looks like it's unindented tightly packed base64.
            // take a chance and create an array with the expected size
            return 3 * len / 4 - padSize;
            // NOTE : padding may be omitted, don't use : "len / 4 * 3 - padSize"
            //        otherwise the size won't be large enough and some characters
            //        won't be processed
        }

        @Override
        public String name() {
            return "base64";
        }

        @Override
        public CharRange valueSpace() {
            if (this.encodeChars == null) {
                this.encodeChars = CharRange.isOneOf(new String(this.encodeMap));
            }
            return this.encodeChars;
        }

    }

    /**
     * Common encoding value spaces.
     *
     * @author Philippe Poulard
     */
    public enum ValueSpace {
        /** Represent bytes in base 64 string. */
        base64(  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"),
        /** Adapted base 64 represent bytes in base 64 string, except that it uses '.' instead of '+'. */
        abase64( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789./"),
        /** Represent bytes in BCrypt's base 64 string. */
        bcrypt64("./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"),
        /** Represent bytes in base 64 string, but mapped to the alphabet : [./0-9A-Za-z] */
        h64(     "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"),
        /** Represent bytes in base 64 string, but mapped to the alphabet : [./0-9A-Za-z] */
        h64be(h64.chars);

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

    /*


BASE64 Encoding :

BASE64_CHARS
'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
HASH64_CHARS
'./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
BCRYPT_CHARS
'./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'

Options :
DES crypt :
    big-endian encoding
custom pbkdf2 hashes :
    BASE64_CHARS , except that it uses '.' instead of '+', and omits trailing padding '=' and whitespace.

     */

}