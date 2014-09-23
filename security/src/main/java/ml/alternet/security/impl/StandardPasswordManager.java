package ml.alternet.security.impl;

import java.util.Base64;

import ml.alternet.discover.LookupKey;
import ml.alternet.security.Password;
import ml.alternet.security.PasswordManager;
import ml.alternet.security.PasswordState;
import ml.alternet.util.BytesUtil;

/**
 * A simple password manager that obfuscate passwords with a Base64 encoding.
 * 
 * <h3>Note</h3> Base64 is just an encoding scheme ; such encoded password
 * doesn't appear in clear but can be easily decoded. If you prefer a stronger
 * password manager, use {@link StrongPasswordManager}
 * 
 * @author Philippe Poulard
 */
@LookupKey(forClass = PasswordManager.class)
public class StandardPasswordManager extends AbstractPasswordManager implements PasswordManager {

    @Override
    public Password newValidPassword(char[] password) {
        // we don't need a conversion, just a raw copy
        byte[] clearBytes = BytesUtil.cast(password);
        final byte[] obfuscate = Base64.getEncoder().encode(clearBytes);
        BytesUtil.unset(clearBytes); // clear intermediate data
        return new AbstractPassword() {
            @Override
            public Clear getClearValidPassword() {
                return new AbstractClearPassword() {
                    @Override
                    protected char[] getClearCopy() {
                        byte[] clearBytes = Base64.getDecoder().decode(obfuscate);
                        char[] clearChars = BytesUtil.cast(clearBytes);
                        BytesUtil.unset(clearBytes); // clear intermediate data
                        return clearChars;
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
