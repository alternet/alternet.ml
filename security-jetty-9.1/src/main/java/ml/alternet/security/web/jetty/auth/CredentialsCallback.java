package ml.alternet.security.web.jetty.auth;

import java.util.List;

import org.eclipse.jetty.jaas.callback.ObjectCallback;
import org.eclipse.jetty.util.security.Credential;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;

/**
 *
 *
 * @author Philippe Poulard
 */
public class CredentialsCallback extends ObjectCallback implements CredentialChecker {

    List<CryptFormat> formats;

    public CredentialsCallback(List<CryptFormat> formats) {
        this.formats = formats;
    }

    @Override
    public void setObject(Object o) {
        if (o instanceof Credentials) {
            super.setObject(o);
        }
    };

    @Override
    public Object getObject() {
        Credentials cred = (Credentials) super.getObject();
        return new Credential() {
            private static final long serialVersionUID = -1339997190338716825L;
            @Override
            public boolean check(Object credentials) {
                String crypt = credentials.toString();
                return CredentialsCallback.this.check(crypt, cred);
            }
        };
    }

    private boolean check(String crypt, Credentials credentials) {
        return check(crypt, this.formats, credentials);
    }

}
