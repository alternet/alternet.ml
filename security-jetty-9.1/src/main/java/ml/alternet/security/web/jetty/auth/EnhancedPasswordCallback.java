package ml.alternet.security.web.jetty.auth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.Destroyable;
import javax.security.auth.callback.PasswordCallback;

import ml.alternet.security.Password;

public class EnhancedPasswordCallback extends PasswordCallback implements Destroyable {

    public EnhancedPasswordCallback(String prompt, boolean echoOn) {
        super(prompt, echoOn);
    }

    private static final long serialVersionUID = 228293073539305899L;

    private Password password;

    public void setPassword(Password password) {
        this.password = password;
    }

    List<Password.Clear> clearList = new ArrayList<>();

    @Override
    public char[] getPassword() {
        Password.Clear clear = this.password.getClearCopy();
        clearList.add(clear);
        setPassword(clear.get());
        return super.getPassword();
    }

    @Override
    public void destroy() {
        for (Iterator<Password.Clear> it = clearList.iterator(); it.hasNext() ; ) {
            Password.Clear clear = it.next();
            clear.close();
            it.remove();
        }
    }

    @Override
    public boolean isDestroyed() {
        return clearList.isEmpty();
    };

    @Override
    public void clearPassword() {
        destroy();
    }

}

