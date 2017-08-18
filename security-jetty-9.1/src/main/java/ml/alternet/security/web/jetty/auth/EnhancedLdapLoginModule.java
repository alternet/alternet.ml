package ml.alternet.security.web.jetty.auth;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;

import org.eclipse.jetty.jaas.spi.LdapLoginModule;

import ml.alternet.misc.Thrower;
import ml.alternet.security.auth.CryptFormat;

public class EnhancedLdapLoginModule extends LdapLoginModule {
	
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
    		this.formats = Arrays.asList(cryptFormatClasses).stream()
    			.map(s -> {
					try {
						return (CryptFormat) Class.forName(s).newInstance();
					} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
						return Thrower.doThrow(e);
					}
				})
    			.collect(Collectors.toList());
    }

}
