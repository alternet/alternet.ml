package ml.alternet.security.impl;

import ml.alternet.security.Password;
import ml.alternet.security.PasswordState;
import ml.alternet.util.BytesUtil;

/**
 * Base implementation of a password.
 * 
 * @author Philippe Poulard
 */
public abstract class AbstractPassword implements Password {

    PasswordState state = PasswordState.Valid;

    @Override
    public String toString() {
        return "*****";
    }

    @Override
    public PasswordState state() {
        return this.state;
    }

    @Override
    public final void destroy() {
        this.state = PasswordState.Invalid;
        byte[] pwd = getPrivatePassword();
        BytesUtil.unset(pwd);
    }

    @Override
    public final Clear getClearCopy() {
        if (state() == PasswordState.Invalid) {
            throw new IllegalStateException("This password has been invalidated");
        }
        return getClearValidPassword();
    }

    /**
     * Return a new clear copy of this valid password.
     * 
     * @return A new clear password
     */
    protected abstract Clear getClearValidPassword();

    /**
     * Get the private bytes of this password, used internally to unset the
     * bytes when the password is invalidated.
     * 
     * @return The private bytes of this password.
     */
    protected abstract byte[] getPrivatePassword();

}