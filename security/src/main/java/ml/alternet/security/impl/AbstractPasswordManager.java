package ml.alternet.security.impl;

import java.util.Arrays;

import ml.alternet.security.EmptyPassword;
import ml.alternet.security.Password;
import ml.alternet.security.PasswordManager;

/**
 * Base implementation of a password manager.
 *
 * @author Philippe Poulard
 */
public abstract class AbstractPasswordManager implements PasswordManager {

    @Override
    public final Password newPassword(char[] password) {
        if (password == null || password.length == 0) {
            return EmptyPassword.SINGLETON;
        }
        Password pwd = newValidPassword(password);
        Arrays.fill(password, (char) 0);
        return pwd;
    }

    /**
     * Obfuscate the given password in a new Password instance.
     *
     * @param password
     *            A non-empty password.
     *            It is useless to unset the chars, because this
     *            method is called from {@link #newPassword(char[])}
     *            that do it.
     *
     * @return A new password
     */
    protected abstract Password newValidPassword(char[] password);

}
