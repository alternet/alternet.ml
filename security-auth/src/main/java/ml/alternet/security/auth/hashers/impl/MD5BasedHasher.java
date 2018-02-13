package ml.alternet.security.auth.hashers.impl;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import ml.alternet.security.algorithms.MD5Crypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.CryptFormatter;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.formats.SaltedParts;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.security.binary.BytesEncoder;
import ml.alternet.util.StringUtil;

/**
 * The MD5 based BSD password algorithm 1 and apr1 (Apache variant).
 *
 * Usually in the Modular Crypt Format :
 * <ol>
 * <li>Apache MD5 crypt format :<br>
 *      <tt>$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1</tt></li>
 * <li>Crypt MD5 :<br>
 *      <tt>$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.</tt></li>
 * </ol>
 *
 * @author Philippe Poulard
 */
public class MD5BasedHasher extends HasherBase<SaltedParts> {

    private static final String APR1 = "apr1";

    static boolean isApacheVariant(Hasher hr) {
        return APR1.equals(hr.getConfiguration().getVariant());
    }

    public MD5BasedHasher(Configuration config) {
        super(config);
    }

    @Override
    public byte[] encrypt(Credentials credentials, SaltedParts parts) {
        byte b[] = MD5Crypt.encrypt(credentials.getPassword(), parts.salt, isApacheVariant(parts.hr));

        // bytes are not encoded in the original order but in that order :
        byte[] order = {0, 6, 12,
                        1, 7, 13,
                        2, 8, 14,
                        3, 9, 15,
                        4, 10, 5,
                        11};
        // and 3 consecutive 8-bits gives 4 *reverted* 6-bits
        // that is to say with the following mapping :
        // 0       6       12       // source index
        // ........________........ // 8-bits position
        // abcdefghijklmnopqrstuvwx // 24 bits input (3 bytes)
        // ++++++------++++++------ // 6 bits position marks (4 * 6-bits)
        // stuvwxmnopqrghijklabcdef // 24 bits output (3 bytes)
        // ........________........ // 8-bits position
        // 0       1       2        // target index
        byte[] result = new byte[16];
        // process the 15 first bytes
        for (int i= 0 ; i< 5; i++) {
            // process 3 by 3
            byte pos0 = order[i*3  ]; // starts with 0
            byte pos1 = order[i*3+1]; // starts with 6
            byte pos2 = order[i*3+2]; // starts with 12
            // some masks are useless but ensure we don't miss a bit
            // stuvwxmn
            result[i*3  ] = (byte) ( ((b[pos2] << 2) & 0b11111100) | ((b[pos1] >> 2) & 0b00000011) );
            // opqrghij
            result[i*3+1] = (byte) ( ((b[pos1] << 6) & 0b11000000) | ((b[pos2] >> 2) & 0b00110000)
                                   | ((b[pos0] << 2) & 0b00001100) | ((b[pos1] >> 6) & 0b00000011) );
            // klabcdef
            result[i*3+2] = (byte) ( ((b[pos1] << 2) & 0b11000000) | ((b[pos0] >> 2) & 0b00111111 ) );
            // and so on with 1, 7, 13, you got it ?
        }
        // revert the 16th byte
        result[15] = (byte) ( ((b[11] << 2)  & 0b11111100) | ((b[11] >> 6) & 0b00000011) );
        return result;
    }

    @Override
    public SaltedParts initializeParts() {
        SaltedParts parts = new SaltedParts(this);
        parts.salt = genSalt();
        return parts;
    }

    static private final char[] SALTCHARS = BytesEncoder.ValueSpace.base64.get();
        // "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890+/"

    private byte[] genSalt() {
        StringBuffer salt = new StringBuffer();
        SecureRandom randgen = new SecureRandom();
        while (salt.length() < 8) {
            int index = (int) (randgen.nextFloat() * SALTCHARS.length);
            salt.append(SALTCHARS[index]);
        }
        return salt.toString().getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * The most popular formatter for this hasher is the Modular Crypt Format.
     *
     * <ul>
     * <li>Apache MD5 crypt format :
     * <pre>$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1</pre></li>
     * <li>Crypt MD5 :
     * <pre>$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.</pre></li>
     * </ul>
     *
     * @see ModularCryptFormatHashers
     */
    public static final CryptFormatter<SaltedParts> MD5CRYPT_FORMATTER = new CryptFormatter<SaltedParts>() {
        @Override
        public SaltedParts parse(String crypt, Hasher hr) {
            String[] stringParts = crypt.split("\\$");
            SaltedParts parts = new SaltedParts(hr);
            if (! stringParts[1].equals(parts.hr.getConfiguration().getVariant())) {
                parts.hr = parts.hr.getBuilder().setVariant(stringParts[1]).build();
            }
            if (stringParts.length > 2 && ! StringUtil.isVoid(stringParts[2])) {
                parts.salt = stringParts[2].getBytes(StandardCharsets.US_ASCII);
            }
            if (stringParts.length > 3 && ! StringUtil.isVoid(stringParts[3])) {
                parts.hash = hr.getConfiguration().getEncoding().decode(stringParts[3]);
            }
            return parts;
        }

        @Override
        public String format(SaltedParts parts) {
            boolean isApacheVariant = isApacheVariant(parts.hr);
            StringBuffer buf = new StringBuffer(isApacheVariant ? 37 : 34);
            buf.append('$');
            buf.append(parts.hr.getConfiguration().getVariant());
            buf.append("$");
            buf.append(new String(parts.salt, StandardCharsets.US_ASCII));
            buf.append('$');
            buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.hash));
            return buf.toString();
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new ModularCryptFormat();
        }

    };

}
