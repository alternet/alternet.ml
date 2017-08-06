package ml.alternet.security.web.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Destroyable;

import ml.alternet.security.EmptyPassword;
import ml.alternet.security.Password;
import ml.alternet.security.web.PasswordParam;
import ml.alternet.security.web.Passwords;

/**
 * If the incoming request URI matches the path
 * configured, this capture context will be set
 * while handling the request for capturing the
 * passwords.
 *
 * @author Philippe Poulard
 */
public class CaptureContext<T> implements Destroyable {

    /**
     * Create a capture context.
     *
     * @param fields The list of fields to capture, e.g. "pwd", "oldPwd", "newPwd", etc
     */
    public CaptureContext(List<String> fields) {
        this.fields = fields;
    }

    List<String> fields;

    /**
     * A map of {name, List&ltPassword&gt;} that contains the passwords captured.
     */
    private Map<String, List<Password>> passwords = new HashMap<>();

    /**
     * Contains the input source ; it has to be writable in order
     * to replace the read bytes with '*' when necessary.
     */
    public T writableInputBuffer;

    @Override
    public void destroy() {
        if (passwords != null) {
            passwords.values().forEach(list -> list.forEach(pwd -> pwd.destroy()));
            passwords = null;
        }
    }

    @Override
    public boolean isDestroyed() {
        return passwords == null;
    }

    /**
     * Append a password that have been captured to this context.
     *
     * @param name The field name.
     * @param newPassword The password.
     */
    public void add(String name, Password newPassword) {
        List<Password> pwds = passwords.get(name);
        if (pwds == null) {
            pwds = new ArrayList<>(2); // oldPwd + newPwd : that'a all we should get
            passwords.put(name, pwds);
        }
        pwds.add(newPassword);
    }

    /**
     * Return all the passwords that have been captured ;
     * the passwords can be retrieved by their name and
     * may be multi-valued. This method can be invoked
     * at the end of the capture phase.
     *
     * @return The passwords that have been captured.
     */
    public Passwords asPasswords() {
        Passwords pwd = new Passwords() {
            @Override
            protected PasswordParam getPasswords(String name) {
                List<Password> list = passwords.get(name);
                if (list == null || list.isEmpty()) {
                    return new PasswordParam(EmptyPassword.SINGLETON);
                } else {
                    return new PasswordParam(list.iterator());
                }
            }
        };
        return pwd;
    }

}
