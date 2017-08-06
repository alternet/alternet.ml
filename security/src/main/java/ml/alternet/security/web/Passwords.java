package ml.alternet.security.web;

import javax.servlet.ServletRequest;

/**
 * Handle safe passwords on a Web environment, meaning that in the
 * Web processing chain, a password NEVER appear as a String inside
 * the system.
 *
 * <p>A password maybe received by a Web server when the client send an
 * HTTP authentication, or when the user fill a login form (POST only
 * since GET is not secure for this purpose). Note that passwords sent
 * by forms doesn't necessary aim to authenticate a user, they may
 * for example have been design to register a user. This class allows
 * to handle all the use case where passwords are sent.</p>
 *
 * <p>A safe data type is also available for RESTful applications.</p>
 *
 * <p>To retrieve the passwords, use :</p>
 * <pre>
 * public void doPost(HttpServletRequest req, HttpServletResponse resp) {
 *     // retrieve the password(s) of a form field
 *     PasswordParam pwd = Passwords.getPasswords(req, "pwdField");
 *     // ...
 * }</pre>
 *
 * @author Philippe Poulard
 */
public abstract class Passwords {

    /**
     * The attribute key for <tt>HttpServletRequest</tt>
     */
    public static final String ATTRIBUTE_KEY = Passwords.class.getName();

    /**
     * The BASIC authentication attribute key for <tt>HttpServletRequest</tt>,
     * used to store the credentials (login, password).
     */
    public static final String BASIC_AUTH_ATTRIBUTE_KEY = Passwords.class.getPackage().getName() + ".basicAuthPassword";

    /**
     * Return a non empty sequence of passwords.
     * If no passwords are defined for the given
     * field name, the first item represents the
     * empty password.
     *
     * @param name The name of the password field.
     * @return A non empty sequence of passwords.
     */
    protected abstract PasswordParam getPasswords(String name);

    /**
     * Extract the passwords sent by the HTTP request.
     *
     * @param req The actual HTTP request.
     * @param name The name of the password field (either a form field
     *                  or a header field according to the configuration)
     * @return A non empty sequence of passwords.
     * If no passwords are defined for the given field name, the first item
     * represents the empty password.
     */
    public static PasswordParam getPasswords(ServletRequest req, String name) {
        Passwords passwords = (Passwords) req.getAttribute(ATTRIBUTE_KEY);
        return passwords.getPasswords(name);
    }

}
