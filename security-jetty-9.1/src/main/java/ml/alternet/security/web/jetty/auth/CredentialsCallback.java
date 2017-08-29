package ml.alternet.security.web.jetty.auth;

import java.util.List;

import org.eclipse.jetty.jaas.callback.ObjectCallback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.Credential;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CredentialsChecker;
import ml.alternet.security.auth.CryptFormat;

/**
 * Jetty Jaas credentials callback for Alternet Security Authentication
 *
 * @author Philippe Poulard
 */
public class CredentialsCallback extends ObjectCallback implements CredentialsChecker {

    static Logger LOG = Log.getLogger(CredentialsChecker.class);

    List<CryptFormat> formats;

    public CredentialsCallback() { }

    public CredentialsCallback(List<CryptFormat> formats) {
        this.formats = formats;
    }

    @Override
    public List<CryptFormat> getCryptFormats() {
        return this.formats;
    }

    @Override
    public void setCryptFormats(List<CryptFormat> formats) {
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
        return new AltCredential(cred);
    }

    @Override
    public void reportError(String message, String crypt, Exception e) {
        if (e == null) {
            LOG.warn(message);
        } else {
            LOG.warn(message, e);
        }
    }

   /**
    *
    *
    * @author Philippe Poulard
    */
    public class AltCredential extends Credential {

        private static final long serialVersionUID = -1339997190338716825L;

        Credentials cred;

        public AltCredential(Credentials cred) {
            this.cred = cred;
        }

        @Override
        public boolean check(Object credentials) {
            String crypt = credentials.toString();
            return CredentialsCallback.this.check(this.cred, crypt);
        }

        public Credentials getCredentials() {
            return this.cred;
        }
    }

}
