package ml.alternet.security.auth;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ml.alternet.misc.Thrower;

/**
 * Allow to check credentials regarding a list of candidates crypt formats.
 *
 * @author Philippe Poulard
 */
public interface CredentialsChecker extends Credentials.Checker {

    /**
     * Default implementation.
     */
    class $ implements CredentialsChecker {

        static Logger LOG = Logger.getLogger(CredentialsChecker.class.getName());

        List<CryptFormat> formats;

        public $(List<CryptFormat> formats) {
            this.formats = formats;
        }

        public $(CryptFormat... formats) {
            this(Arrays.asList(formats));
        }

        @Override
        public void setCryptFormats(List<CryptFormat> formats) {
            this.formats = formats;
        }

        @Override
        public void reportError(String message, String crypt, Exception e) {
            LOG.warning(message + e);
        }

        @Override
        public List<CryptFormat> getCryptFormats() {
            return this.formats;
        }
    };

    /**
     * Check some credentials with a given crypt, regarding the
     * list of candidates crypt formats that was set.
     *
     * @param credentials The credentials to check.
     * @param crypt The crypt to compare with.
     *
     * @return <code>true</code> when the credentials matches the
     *      crypt, <code>false</code> otherwise. When <code>false</code>
     *      is returned, an error may have been reported.
     *
     * @see #reportError(String, String, Exception)
     */
    @Override
    default boolean check(Credentials credentials, String crypt) {
        return Hasher.resolve(crypt, getCryptFormats())
            .map(hr -> {
                return hr.check(credentials, crypt);
            })
            .orElseGet(() -> {
                reportError("Unable to find a hasher for crypt " + crypt, crypt, null);
                return false;
            });
    }

    /**
     * Report an error, typically when a bad parameter was set to the hasher,
     *      or when no suitable hasher were found for a given crypt.
     *
     * @param message The error message.
     * @param crypt The crypt to check.
     * @param e The exception, may be <code>null</code>
     */
    @Override
    void reportError(String message, String crypt, Exception e);

    /**
     * Return the set of candidate crypt formats to use.
     *
     * @return The set of candidate crypt formats to use.
     */
    List<CryptFormat> getCryptFormats();

    /**
     * Set the list of candidate crypt formats to use for getting
     * the suitable hasher.
     *
     * @param formats The list of candidate crypt formats.
     */
    void setCryptFormats(List<CryptFormat> formats);

    /**
     * Set the list of candidate crypt formats to use for getting
     * the suitable hasher.
     *
     * @param formats The list of candidate crypt formats.
     */
    default void setCryptFormats(CryptFormat... formats) {
        setCryptFormats(Arrays.asList(formats));
    }

    /**
     * Set the list of candidate crypt formats to use for getting
     * the suitable hasher.
     *
     * @param cryptFormatClasses The list of candidate crypt formats class names.
     */
    default void setCryptFormats(String... cryptFormatClasses) {
        setCryptFormats(
            Arrays.asList(cryptFormatClasses).stream()
            .map(s -> {
                try {
                    return (CryptFormat) Class.forName(s).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    return Thrower.doThrow(e);
                }
            })
            .collect(Collectors.toList())
        );
    }

}
