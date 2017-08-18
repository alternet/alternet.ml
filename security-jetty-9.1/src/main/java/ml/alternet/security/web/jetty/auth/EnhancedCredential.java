package ml.alternet.security.web.jetty.auth;

import java.security.InvalidAlgorithmParameterException;
import java.util.List;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.Credential;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;

public class EnhancedCredential extends Credential {
	
    private static final Logger LOG = Log.getLogger(EnhancedCredential.class);

    private final Credentials cred;
	List<CryptFormat> formats;
	
	private static final long serialVersionUID = -1339997190338716825L;

	public EnhancedCredential(Credentials cred, List<CryptFormat> formats) {
		this.cred = cred;
		this.formats = formats;
	}

	@Override
	public boolean check(Object credentials) {
	    String crypt = credentials.toString();
	    return Hasher.resolve(crypt, this.formats)
	    		.map(hr -> {
					try {
						return hr.check(this.cred, crypt);
					} catch (InvalidAlgorithmParameterException e) {
						LOG.warn("Unable to load hasher for crypt " + crypt, e);
						return false;
					}
				})
	    		.orElseGet(() -> {
				LOG.warn("Unable to find a hasher for crypt " + crypt);
	    			return false;
	    		});
	}
}