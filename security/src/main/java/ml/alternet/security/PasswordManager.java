package ml.alternet.security;

/**
 * A concrete implementation should supply obfuscate Passwords according to the
 * level of security expected.
 * 
 * For using the configured implementation, use
 * 
 * <pre>
 * PasswordManager impl = DiscoveryService.lookupSingleton(PasswordManager.class);
 * </pre>
 * 
 * or
 * 
 * <pre>
 * PasswordManager impl = PasswordManagerFactory.getDefaultPasswordManager()
 * </pre>
 * 
 * which lead to the same default implementation.
 * 
 * Several flavors of PasswordManager are already available in the factory.
 * 
 * @see PasswordManagerFactory
 * 
 * @author Philippe Poulard
 */
public interface PasswordManager {

    /**
     * Obfuscate the given password in a new Password instance.
     * 
     * @param password
     *            The password to obfuscate ; may be null or empty ; that char
     *            array is cleared when returning
     * @return The password
     * 
     * @see EmptyPassword#SINGLETON
     */
    Password newPassword(char[] password);

}
