package ml.alternet.security.impl;

import ml.alternet.security.Password;
import ml.alternet.security.PasswordManager;
import ml.alternet.security.PasswordState;
import ml.alternet.util.BytesUtil;

/**
 * A weak password manager that doesn't obfuscate passwords.
 * 
 * @author Philippe Poulard
 */
public class WeakPasswordManager extends AbstractPasswordManager implements PasswordManager {

    @Override
    public Password newValidPassword(char[] password) {
        // we don't need a conversion, just a raw copy
        final byte[] clearBytes = BytesUtil.cast(password);
        return new AbstractPassword() {
            @Override
            public Clear getClearValidPassword() {
                return new AbstractClearPassword() {
                    @Override
                    protected char[] getClearCopy() {
                        char[] clearChars = BytesUtil.cast(clearBytes);
                        return clearChars;
                    }

                    @Override
                    protected boolean isDestroyed() {
                        return state() == PasswordState.Invalid;
                    }
                };
            }

            @Override
            protected byte[] getPrivatePassword() {
                return clearBytes;
            }
        };
    }

}
