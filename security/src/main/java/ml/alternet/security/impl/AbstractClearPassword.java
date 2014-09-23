package ml.alternet.security.impl;

import ml.alternet.security.Password;
import ml.alternet.util.BytesUtil;

/**
 * Base implementation of a clear password.
 * 
 * @author Philippe Poulard
 */
public abstract class AbstractClearPassword implements Password.Clear {

    private char[] clearPassword;

    @Override
    public final char[] get() {
        if (isInvalid()) {
            throw new IllegalStateException("This password has been invalidated");
        }
        if (this.clearPassword == null) {
            this.clearPassword = getClearCopy();
        }
        return this.clearPassword;
    }

    /**
     * Indicates whether the underlying password has been invalidated.
     * 
     * @return The state of the password.
     */
    protected abstract boolean isInvalid();

    /**
     * Deobfuscate the underlying password.
     * 
     * @return A clear copy of the password.
     */
    protected abstract char[] getClearCopy();

    @Override
    public void close() {
        if (this.clearPassword != null) {
            BytesUtil.unset(this.clearPassword);
            this.clearPassword = null;
        }
    }

}
