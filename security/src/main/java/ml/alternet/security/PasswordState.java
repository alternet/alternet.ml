package ml.alternet.security;

/**
 * Indicates whether a password is empty, valid, or invalid.
 *
 * @author Philippe Poulard
 */
public enum PasswordState {

    /**
     * The empty state
     */
    Empty,

    /**
     * The valid state
     */
    Valid,

    /**
     * The invalid state
     */
    Invalid;

}
