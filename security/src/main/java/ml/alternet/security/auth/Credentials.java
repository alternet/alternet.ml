package ml.alternet.security.auth;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Destroyable;

import ml.alternet.security.Password;
import ml.alternet.security.PasswordManagerFactory;

/**
 * User credentials are the more often just a password,
 * but this class can also build credentials with
 * common fields such as a user name, a realm (or domain),
 * or any custom field.
 *
 * Each field is unique in this class.
 *
 * @author Philippe Poulard
 */
public class Credentials implements Destroyable {

    String username;
    Password password;
    String realm;
    Map<String, Object> fields;

    /**
     * Create the credentials from a password.
     *
     * @param password The password.
     *
     * @return The credentials.
     */
    public static Credentials fromPassword(Password password) {
        return new Credentials().withPassword(password);
    }

    /**
     * Create the credentials from a password.
     *
     * @param password The password.
     *
     * @return The credentials.
     */
    public static Credentials fromPassword(char[] password) {
        return new Credentials().withPassword(password);
    }

    /**
     * Create the credentials from a user and a password.
     *
     * @param username The user name.
     * @param password The password.
     *
     * @return The credentials.
     */
    public static Credentials fromUserPassword(String username, Password password) {
        return new Credentials().withUser(username).withPassword(password);
    }

    /**
     * Create the credentials from a user and a password.
     *
     * @param username The user name.
     * @param password The password.
     *
     * @return The credentials.
     */
    public static Credentials fromUserPassword(String username, char[] password) {
        return new Credentials().withUser(username).withPassword(password);
    }

    /**
     * Append a user name field to this credentials.
     *
     * @param username The user name.
     *
     * @return The modified credentials.
     */
    public Credentials withUser(String username) {
        this.username = username;
        return this;
    }

    /**
     * Append a password field to this credentials.
     *
     * @param password The password.
     *
     * @return The modified credentials.
     */
    public Credentials withPassword(Password password) {
        this.password = password;
        return this;
    }

    /**
     * Append a password field to this credentials.
     *
     * @param password The password.
     *
     * @return The modified credentials.
     *
     * @see PasswordManagerFactory#getStrongPasswordManager()
     */
    public Credentials withPassword(char[] password) {
        this.password = PasswordManagerFactory.getStrongPasswordManager().newPassword(password);
        return this;
    }

    /**
     * Append a realm field to this credentials.
     *
     * @param realm The realm.
     *
     * @return The modified credentials.
     */
    public Credentials withRealm(String realm) {
        this.realm = realm;
        return this;
    }

    /**
     * Append a custom field to this credentials.
     *
     * @param field The field name.
     * @param data The field value.
     *
     * @return The modified credentials.
     */
    public Credentials withField(String field, Object data) {
        if (this.fields == null) {
            this.fields = new HashMap<>();
        }
        this.fields.put(field, data);
        return this;
    }

    /**
     * Return the user name.
     *
     * @return The user name, or <code>null</code>
     */
    public String getUserName() {
        return this.username;
    }

    /**
     * Return the password.
     *
     * @return The password, or <code>null</code>
     */
    public Password getPassword() {
        return this.password;
    }

    /**
     * Return the realm.
     *
     * @return The realm, or <code>null</code>
     */
    public String getRealm() {
        return this.realm;
    }

    /**
     * Return a field value.
     *
     * @param field The field name.
     * @param <T> The type of the field.
     *
     * @return The field value, or <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String field) {
        return (T) this.fields.get(field);
    }

    /**
     * Invalidate these credentials ;
     * act only on the password field.
     */
    @Override
    public void destroy() {
        if (this.password != null) {
            this.password.destroy();
        }
    }

    @Override
    public boolean isDestroyed() {
        if (this.password != null) {
            return this.password.isDestroyed();
        } else {
            return false;
        }
    }

}
