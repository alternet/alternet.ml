package ml.alternet.security.auth.hashers.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;

import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.formats.WorkFactorSaltedParts;
import ml.alternet.security.binary.SafeBuffer;
import ml.alternet.util.BytesUtil;

public class PBKDF2Hasher extends HasherBase<WorkFactorSaltedParts> {

    static final Logger LOGGER = Logger.getLogger(PBKDF2Hasher.class.getName());

    public PBKDF2Hasher(Configuration config) {
        super(config);
    }

    /**
     * By default, return "PBKDF2".
     *
     * @return This hasher's scheme.
     */
    @Override
    public String getScheme() {
        return "PBKDF2";
    }

    /**
     * By default, return "PBKDF2WithHmacSHA1".
     *
     * @return The algorithm used by this implementation.
     */
    protected String getAlgorithm() {
        return "PBKDF2WithHmacSHA1";
    }

    @Override
    public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
        int hashByteSize = parts.hr.getConfiguration().getHashByteSize();
        try {
            return hash(credentials.getPassword(), parts.salt, parts.workFactor, hashByteSize);
        } catch (InvalidAlgorithmParameterException e) {
            return Thrower.doThrow(e);
        }
    }

    @Override
    public WorkFactorSaltedParts initializeParts() {
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(this);
        parts.generateSalt(); // TODO: check parameter and min-max
        parts.workFactor = getConfiguration().getIterations();
        return parts;
    }

    /**
     * Hash a password and ensure that intermediate data are erased.
     *
     * @param password The password.
     * @param salt The salt.
     * @param iterCount The number of iterations of the PBKDF2 algorithm.
     * @param keyLength The number of bytes of the hash to produce.
     *
     * @return The hash.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    protected byte[] hash(Password password, byte[] salt, int iterCount, int keyLength) throws InvalidAlgorithmParameterException {
        // JCE algorithm that computes a hash

        // Same as :
        //        SecretKeyFactory skf = SecretKeyFactory.getInstance(getAlgorithm()); // "PBKDF2WithHmacSHA1"
        //        char[] passwordChars = pwd.get();
        //        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, i, bytes * 8);
        //        SecretKey key = skf.generateSecret(spec);
        //        byte[] b = key.getEncoded();
        // but intermediate data are erased

        String macAlgorith = getAlgorithm().substring(getAlgorithm().indexOf("With") + 4); // "HmacSHA1"
        byte[] pBytes = null;
        try (Password.Clear pwd = password.getClearCopy()) {
            // the encoder take care of erasing intermediate data if necessary
            ByteBuffer bb = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), StandardCharsets.UTF_8);
            if (bb.limit() == bb.capacity()) {
                pBytes = bb.array();
            } else {
                pBytes = new byte[bb.limit()];
                bb.get(pBytes);
                BytesUtil.unset(bb.array());
            }
            final byte[] passwordBytes = pBytes;
            // HmacSHA1, HmacSHA224, HmacSHA256, HmacSHA384, HmacSHA512
            final Mac mac = Mac.getInstance(macAlgorith);

            // code adapted from
            // com.sun.crypto.provider.PBKDF2KeyImpl
            byte[] key = new byte[keyLength];
            int hlen = mac.getMacLength();
            int intL = (keyLength + hlen - 1)/hlen; // ceiling
            int intR = keyLength - (intL - 1)*hlen; // residue
            byte[] ui = new byte[hlen];
            byte[] ti = new byte[hlen];
            // SecretKeySpec cannot be used, since password can be empty here.
            SecretKey macKey = new SecretKey() {
                private static final long serialVersionUID = 7293295131471223961L;
                @Override
                public String getAlgorithm() {
                    return mac.getAlgorithm();
                }
                @Override
                public String getFormat() {
                    return "RAW";
                }
                @Override
                public byte[] getEncoded() {
                    // the good new is after the MAC init phase,
                    // all the bytes will be erased
                    return passwordBytes;
                }
                @Override
                public int hashCode() {
                    return Arrays.hashCode(passwordBytes) * 41 +
                            mac.getAlgorithm().toLowerCase().hashCode();
                }
                @Override
                public boolean equals(Object obj) {
                    if (this == obj) return true;
                    if (this.getClass() != obj.getClass()) return false;
                    SecretKey sk = (SecretKey)obj;
                    return mac.getAlgorithm().equalsIgnoreCase(
                        sk.getAlgorithm()) &&
                        Arrays.equals(passwordBytes, sk.getEncoded());
                }
            };
            mac.init(macKey);
            // passwordBytes are cleared

            byte[] ibytes = new byte[4];
            for (int i = 1; i <= intL; i++) {
                mac.update(salt);
                ibytes[3] = (byte) i;
                ibytes[2] = (byte) ((i >> 8) & 0xff);
                ibytes[1] = (byte) ((i >> 16) & 0xff);
                ibytes[0] = (byte) ((i >> 24) & 0xff);
                mac.update(ibytes);
                mac.doFinal(ui, 0);
                System.arraycopy(ui, 0, ti, 0, ui.length);

                for (int j = 2; j <= iterCount; j++) {
                    mac.update(ui);
                    mac.doFinal(ui, 0);
                    // XOR the intermediate Ui's together.
                    for (int k = 0; k < ui.length; k++) {
                        ti[k] ^= ui[k];
                    }
                }
                if (i == intL) {
                    System.arraycopy(ti, 0, key, (i-1)*hlen, intR);
                } else {
                    System.arraycopy(ti, 0, key, (i-1)*hlen, hlen);
                }
            }
            return key;
        } catch (NoSuchAlgorithmException | ShortBufferException | IllegalStateException | InvalidKeyException e) {
            throw new InvalidAlgorithmParameterException(e);
        } finally {
            BytesUtil.unset(pBytes);
        }
    }

}
