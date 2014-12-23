package ml.alternet.security;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.security.impl.StandardPasswordManager;
import ml.alternet.security.impl.StrongPasswordManager;
import ml.alternet.security.impl.WeakPasswordManager;

/**
 * A factory that can supply different flavors of PasswordManager.
 *
 * {@link #getDefaultPasswordManager()} is mapped to the implementation defined
 * by the discovery service.
 *
 * @see DiscoveryService
 *
 * @author Philippe Poulard
 */
public final class PasswordManagerFactory {

    /*
     * Used when creating the concrete instance of a password manager is costly
     * (when a lookup is performed or when the constructor is greedy
     */
    private abstract static class DelegatePasswordManager implements PasswordManager {
        PasswordManager pm;

        @Override
        public Password newPassword(char[] password) {
            if (pm == null) {
                pm = getPasswordManager();
            }
            return pm.newPassword(password);
        }

        // we won't sacrifice the performance with a synchronized method
        // we expect that this method is likely to be called only once
        abstract PasswordManager getPasswordManager();
    }

    private static PasswordManager DEFAULT_PASSWORD_MANAGER = new DelegatePasswordManager() {
        @Override
        PasswordManager getPasswordManager() {
            try {
                DEFAULT_PASSWORD_MANAGER = DiscoveryService.lookupSingleton(PasswordManager.class);
                return DEFAULT_PASSWORD_MANAGER;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    /**
     * Get the default password manager, according to the configuration, which
     * can be any of the supplied password manager (see other methods) or also a
     * custom configuration.
     *
     * If not overridden, the default is {@link StandardPasswordManager}.
     *
     * To override, use the {@link DiscoveryService}
     *
     * @return The password manager.
     */
    public static PasswordManager getDefaultPasswordManager() {
        return DEFAULT_PASSWORD_MANAGER;
    }

    private static PasswordManager WEAK_PASSWORD_MANAGER = new WeakPasswordManager();

    /**
     * Return the weak password manager where passwords are kept clear ; a weak
     * password manager is suitable for example when a password is already clear
     * in the system, such as a database password which has been read from a
     * configuration file aside the system, which therefore already appears
     * clear.
     *
     * @see WeakPasswordManager
     *
     * @return The weak password manager.
     */
    public static PasswordManager getWeakPasswordManager() {
        return WEAK_PASSWORD_MANAGER;
    }

    private static PasswordManager STANDARD_PASSWORD_MANAGER = new StandardPasswordManager();

    /**
     * Return the standard password manager where passwords are Base64 encoded.
     *
     * @see StandardPasswordManager
     *
     * @return The standard password manager.
     */
    public static PasswordManager getStandardPasswordManager() {
        return STANDARD_PASSWORD_MANAGER;
    }

    /*
     * The delegate is used because the strong password manager performs a lot
     * of stuff when loaded
     */
    private static PasswordManager STRONG_PASSWORD_MANAGER = new DelegatePasswordManager() {
        @Override
        PasswordManager getPasswordManager() {
            STRONG_PASSWORD_MANAGER = new StrongPasswordManager();
            return STRONG_PASSWORD_MANAGER;
        }
    };

    /**
     * Return the strong password manager where passwords are encrypted,
     * therefore not easy to find in the memory.
     *
     * @see StrongPasswordManager
     *
     * @return The strong password manager.
     */
    public static PasswordManager getStrongPasswordManager() {
        return STRONG_PASSWORD_MANAGER;
    }

}
