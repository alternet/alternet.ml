package ml.alternet.test.security.web.jetty;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.security.web.jetty.auth.HasherCredential;

import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;

class MappedLoginServiceImpl extends MappedLoginService {

    Hasher hr;

    public void setHasher(Hasher hr) {
		this.hr = hr;
	}

    @Override
    protected void loadUsers() throws IOException { }
    
    @Override
    protected UserIdentity loadUser(String username) {
        return null;
    }
    
    public synchronized UserIdentity putUser(String userName, Credentials password, String[] roles)
            throws InvalidAlgorithmParameterException {
        return super.putUser(
                userName,
                // the hasher credential is the only "non standard" facet
                // of the configuration, required for checking Password
//                new HasherCredential(Hasher.getDefault().encrypt(password)),
                new HasherCredential(
                		hr.encrypt(password),
                		hr.getConfiguration().getFormatter().getCryptFormat()
                		),
                roles);
    }

}