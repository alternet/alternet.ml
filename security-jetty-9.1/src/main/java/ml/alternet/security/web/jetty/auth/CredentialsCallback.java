package ml.alternet.security.web.jetty.auth;

import java.util.List;

import org.eclipse.jetty.jaas.callback.ObjectCallback;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;

/**
 *
 *
 * @author Philippe Poulard
 */
public class CredentialsCallback extends ObjectCallback {

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
        return new EnhancedCredential(cred, this.formats);
    }

}
