package ml.alternet.security.impl;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import ml.alternet.misc.WtfException;
import ml.alternet.security.Password;
import ml.alternet.security.PasswordManager;
import ml.alternet.security.PasswordState;
import ml.alternet.util.BytesUtil;

/**
 * A password manager that encrypt passwords.
 *
 * <h3>Note</h3> Passwords are encrypted, but the key to decrypt such password
 * is still stored somewhere in the memory.
 *
 * @author Philippe Poulard
 */
public class StrongPasswordManager extends AbstractPasswordManager implements PasswordManager {

    // for IV generation
    private static SecureRandom random = new SecureRandom();

    // used to crypt/decrypt
    private final SecretKey secret;

    /**
     * Create a strong password manager that encrypt passwords.
     */
    public StrongPasswordManager() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128); // 128 // 192 and 256 bits may not be available
            secret = kg.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw WtfException.throwException(e);
        }
    }

    // the paranoid mode expect that every intermediate data
    // has to be unset before they lost their strong reference
    // since we don't know when the GC will reclaim the allocated
    // memory
    @Override
    public Password newValidPassword(char[] password) {
        final byte[] iv = new byte[16];
        final byte[] obfuscate;
        random.nextBytes(iv);
        try {
            Cipher jcaCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            jcaCipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));
            byte[] clearBytes = BytesUtil.cast(password);
            obfuscate = jcaCipher.doFinal(clearBytes);
            BytesUtil.unset(clearBytes); // clear intermediate data
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e)
        {
            throw WtfException.throwException(e);
        }
        return new AbstractPassword() {
            @Override
            public Clear getClearValidPassword() {
                return new AbstractClearPassword() {
                    @Override
                    protected char[] getClearCopy() {
                        try {
                            Cipher jcaCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                            jcaCipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
                            byte[] clearBytes = jcaCipher.doFinal(obfuscate);
                            char[] clearChars = BytesUtil.cast(clearBytes);
                            BytesUtil.unset(clearBytes); // clear intermediate data
                            return clearChars;
                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                                InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e)
                        {
                            throw WtfException.throwException(e);
                        }
                    }

                    @Override
                    protected boolean isInvalid() {
                        return state() == PasswordState.Invalid;
                    }
                };
            }

            @Override
            protected byte[] getPrivatePassword() {
                return obfuscate;
            }
        };
    }

}
