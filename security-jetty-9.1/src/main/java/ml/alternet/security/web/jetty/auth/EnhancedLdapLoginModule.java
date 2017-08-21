package ml.alternet.security.web.jetty.auth;

import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;

import org.eclipse.jetty.jaas.spi.LdapLoginModule;

import ml.alternet.security.auth.CryptFormat;

public class EnhancedLdapLoginModule extends LdapLoginModule implements CryptFormatAware {

    List<CryptFormat> formats;

    @Override
    public Callback[] configureCallbacks () {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Enter user name");
        callbacks[1] = new CredentialsCallback(this.formats);
        return callbacks;
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
        Map<String, ?> options) 
    {
        super.initialize(subject, callbackHandler, sharedState, options);
        // see LDAPAuthTest#setJaasConfiguration()
        String[] cryptFormatClasses = ((String) options.get(CryptFormat.class.getName())).split("\\s*,\\s*");
        setCryptFormats(cryptFormatClasses);
    }

    @Override
    public void setCryptFormats(List<CryptFormat> formats) {
        this.formats = formats;
    }

}
