package ml.alternet.security.auth.hasher;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.security.algorithms.MD5Crypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.SaltedParts;

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

    /**
     * Indicates whether a hasher is configured with the apache variant.
     *
     * @param hr The hasher
     *
     * @return {@code true} for {@code $apr1$}, {@code false} otherwise.
     */
    public static boolean isApacheVariant(Hasher hr) {
        return APR1.equals(hr.getConfiguration().getVariant());
    }

    /**
     * Create an MD5 based hasher.
     *
     * @param config The configuration of this hasher.
     */
    public MD5BasedHasher(Builder config) {
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
        for (int i = 0 ; i < 5; i++) {
            // process 3 by 3
            byte pos0 = order[i * 3    ]; // starts with 0
            byte pos1 = order[i * 3 + 1]; // starts with 6
            byte pos2 = order[i * 3 + 2]; // starts with 12
            // some masks are useless but ensure we don't miss a bit
            // stuvwxmn
            result[i * 3    ] = (byte) ( ((b[pos2] << 2) & 0b11111100) | ((b[pos1] >> 2) & 0b00000011) );
            // opqrghij
            result[i * 3 + 1] = (byte) ( ((b[pos1] << 6) & 0b11000000) | ((b[pos2] >> 2) & 0b00110000)
                                       | ((b[pos0] << 2) & 0b00001100) | ((b[pos1] >> 6) & 0b00000011) );
            // klabcdef
            result[i * 3 + 2] = (byte) ( ((b[pos1] << 2) & 0b11000000) | ((b[pos0] >> 2) & 0b00111111 ) );
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

    private static final char[] SALTCHARS = BytesEncoder.ValueSpace.base64.get();
        // "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890+/"

    private byte[] genSalt() {
        StringBuffer salt = new StringBuffer();
        SecureRandom randgen = new SecureRandom();
        while (salt.length() < 8) {
            int index = (int) (randgen.nextFloat() * SALTCHARS.length);
            salt.append(SALTCHARS[index]);
        }
        return salt.toString().getBytes(StandardCharsets.US_ASCII);
    };

}
