package ml.alternet.security.web.tomcat;

import javax.servlet.ServletRequest;

import org.apache.catalina.CredentialHandler;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import ml.alternet.misc.Thrower;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.security.web.server.AuthenticationMethod;

/**
 * Check a password hash.
 *
 * @author Philippe Poulard
 */
public class AltCredentialHandler implements CredentialHandler {

    private static final Log LOG = LogFactory.getLog(AltCredentialHandler.class);

    private Hasher hasher;

    @Override
    public boolean matches(String inputCredentials, String storedCredentials) {
        if (storedCredentials == null) {
            return false;
        }
        ServletRequest request = AltProtocolHandler.request.get();
        AuthenticationMethod am = AuthenticationMethod.extract(request.getServletContext());
        Credentials credentials = am.getCredentials(request);
//        try {
            // TODO : use CryptFormat instead ? (and change the doc accordingly)
            return getHasher().check(credentials, storedCredentials);
//        } catch (InvalidAlgorithmParameterException e) {
//            LOG.error("Unable to check password hash.", e);
//            return false;
//        }
    }

    @Override
    public String mutate(String inputCredentials) {
        return inputCredentials;
    }

    /**
     * Set a hasher.
     *
     * @param hasher The hasher.
     */
    public void setHasher(Hasher hasher) {
        // TODO : set hasher as a class name
        this.hasher = hasher;
    }

    /**
     * Get the hasher.
     *
     * @return The hasher, maybe initialized with the default.
     */
    public Hasher getHasher() {
        if (this.hasher == null) {
//            this.hasher = Hasher.getDefault();
            this.hasher = ModularCryptFormatHashers.$2$.get().build(); // TODO Hasher conf
        }
        return this.hasher;
    }

    /**
     * Set a hasher from its class name.
     *
     * @param hasher The name of the hasher.
     *
     * @see Hasher
     */
    public void setHasher(String hasher) {
        try {
            this.hasher = (Hasher) Class.forName(hasher).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            Thrower.doThrow(e);
        }
    }

}
