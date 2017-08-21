package ml.alternet.test.security.web.jetty;

import java.io.IOException;
import java.util.List;

import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.web.jetty.auth.CredentialChecker;
import ml.alternet.security.web.jetty.auth.CryptFormatAware;

class MappedLoginServiceImpl extends MappedLoginService implements CryptFormatAware, CredentialChecker {

    List<CryptFormat> formats;

    @Override
    public void setCryptFormats(List<CryptFormat> formats) {
        this.formats = formats;
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
                        return MappedLoginServiceImpl.this.check(crypt, (Credentials) credentials);
                    } else {
                        return false;
                    }
                }
            },
            roles
        );
    }

    private boolean check(String crypt, Credentials credentials) {
        return check(crypt, this.formats, credentials);
    }

}
