package ml.alternet.test.security.web.jetty;

import java.io.IOException;
import java.util.List;

import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.Credential;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CredentialsChecker;
import ml.alternet.security.auth.CryptFormat;

class AltMappedLoginService extends MappedLoginService implements CredentialsChecker {

    static Logger LOG = Log.getLogger(AltMappedLoginService.class);

    List<CryptFormat> formats;

    @Override
    public void setCryptFormats(List<CryptFormat> formats) {
        this.formats = formats;
    }

    @Override
    public List<CryptFormat> getCryptFormats() {
        return this.formats;
    }

    @Override
    protected void loadUsers() throws IOException { }

    @Override
    protected UserIdentity loadUser(String username) {
        return null;
    }

    public synchronized UserIdentity putUser(String userName, String crypt, String[] roles) {
        return super.putUser(
            userName,
            new Credential() {
                private static final long serialVersionUID = -4044371224161812264L;
                @Override
                public boolean check(Object credentials) {
                    if (credentials instanceof Credentials) {
                        return AltMappedLoginService.this.check((Credentials) credentials, crypt);
                    } else {
                        return false;
                    }
                }
            },
            roles
        );
    }

    @Override
    public void reportError(String message, String crypt, Exception e) {
        if (e == null) {
            LOG.warn(message);
        } else {
            LOG.warn(message, e);
        }
    }

}
