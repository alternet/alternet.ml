package ml.alternet.security.auth.hasher;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.security.algorithms.MD5Crypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.SaltedParts;
import ml.alternet.security.binary.Transposer;

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
                        11}; // no padding
        return Transposer.transpose(b, order, 16);
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
