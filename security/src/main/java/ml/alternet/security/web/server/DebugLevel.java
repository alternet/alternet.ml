package ml.alternet.security.web.server;

/**
 * Indicates the debug level to the server.
 *
 * @author Philippe Poulard
 */
public class DebugLevel {

    boolean allowUnsecureTrace = false;

    /**
     * Allow writing in the log the raw data in clear ;
     * it is unsecure because passwords may appear in clear ;
     * USE FOR DEBUG ONLY NOT ON PRODUCTION ENVIRONMENT.
     * By default, it is false.
     */
    public void allowUnsecureTrace() {
        this.allowUnsecureTrace = true;
    }

    /**
     * Disallow writing in the log the raw data in clear.
     */
    public void disallowUnsecureTrace() {
        this.allowUnsecureTrace = false;
    }

    /**
     * Indicates the current debug level.
     *
     * @return <code>true</code> if the server can write in
     * the log raw data in clear, <code>false</code> otherwise.
     */
    public boolean isAllowingUnsercureTrace() {
        return this.allowUnsecureTrace;
    }

    /**
     * A component can be debuggable.
     *
     * @author Philippe Poulard
     */
    public interface Debuggable {

        /**
         * Return the debug level of this component.
         *
         * @return The debug level.
         */
        DebugLevel getDebugLevel();

    }

}
