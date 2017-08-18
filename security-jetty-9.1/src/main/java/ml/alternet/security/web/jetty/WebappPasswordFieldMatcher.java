package ml.alternet.security.web.jetty;

import java.util.List;
import java.util.Optional;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.Authenticator.AuthConfiguration;
import org.eclipse.jetty.security.Authenticator.Factory;
import org.eclipse.jetty.security.DefaultAuthenticatorFactory;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.ClientCertAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.security.authentication.SpnegoAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.web.server.AuthenticationMethod;
import ml.alternet.security.web.server.FormFieldConfiguration;
import ml.alternet.security.web.server.FormLimit;
import ml.alternet.security.web.server.PasswordFieldMatcher;

/**
 * A matcher based on a Jetty web application.
 *
 * <p>The Web application has to be configured as follow :</p>
 * <pre>
 *&lt;context-param&gt;
 *    &lt;param-name&gt;ml.alternet.security.web.config.formFields&lt;/param-name&gt;
 *    &lt;param-value&gt;
 *          /doRegister.html?pwd&amp;confirmPwd,
 *          /doUpdatePassword.html?oldPwd&amp;newPwd&amp;confirmPwd
 *    &lt;/param-value&gt;
 *&lt;/context-param&gt;
 *&lt;context-param&gt;
 *    &lt;param-name&gt;ml.alternet.security.web.config.authenticationMethod&lt;/param-name&gt;
 *    &lt;param-value&gt;Basic&lt;/param-value&gt;
 *&lt;/context-param&gt;
 * </pre>
 * <p>The former context parameter consist of a comma-separated sequence of
 * paths relative to the context path ; those paths are the target
 * destination to which form data are posted, and that indicates in
 * their query string the fields of the form parameters that contains
 * the passwords to capture.</p>
 * <p>The latter context parameter indicates the authentication method ;
 * setting "Basic" indicates to capture passwords
 * during HTTP Basic Authentication ; setting "Form" indicates to capture
 * passwords during HTTP Form Authentication.</p>
 *
 * @author Philippe Poulard
 */
public class WebappPasswordFieldMatcher implements PasswordFieldMatcher, FormLimit {

    WebAppContext wac;
    AuthenticationMethod am;

    /**
     * Create a password field matcher for a Jetty Webapp.
     *
     * @param wac The Jetty Webapp context.
     */
    public WebappPasswordFieldMatcher(WebAppContext wac) {
        this.wac = wac;
        this.wac.addLifeCycleListener(new LifeCycle.Listener() {
            @Override
            public void lifeCycleStopping(LifeCycle event) {
                ServletContext sc = wac.getServletContext();
                FormFieldConfiguration.reset(sc);
                AuthenticationMethod.reset(sc);
            }
            @Override
            public void lifeCycleStopped(LifeCycle event) { }
            @Override
            public void lifeCycleStarting(LifeCycle event) {
                am = getAuthenticationMethod();
                if (am == AuthenticationMethod.Basic || am == AuthenticationMethod.Form) {
                    wac.getSecurityHandler().setAuthenticatorFactory(
                        newAuthenticatorFactory()
                    );
                }
            }
            @Override
            public void lifeCycleStarted(LifeCycle event) { }
            @Override
            public void lifeCycleFailure(LifeCycle event, Throwable cause) { }
        });
    }

    /**
     * Login with the captured password.
     *
     * @param loginAuthenticator For retrieving the login service
     * @param username The username.
     * @param request The request that contains the captured password
     * @return The user identity, or <code>null</code>.
     */
    public UserIdentity login(LoginAuthenticator loginAuthenticator, String username, ServletRequest request) {
        // "password" contains "*****"
        // the pwd has been captured before, let's retrieve it
        Credentials credentials = am.getCredentials(request);
        // use "pwd" instead of "password"
        UserIdentity user = loginAuthenticator.getLoginService().login(username, credentials);
        return user;
    }

    /**
     * Return an authenticator that login with the captured
     * Password class instead of the supplied password field
     * that hes been filled with '*'.
     *
     * @return By default, a subclass of BasicAuthenticator ;
     *      see implementation for details.
     */
    protected Factory newAuthenticatorFactory() {
        return new DefaultAuthenticatorFactory() {
            @Override
            public Authenticator getAuthenticator(Server server, ServletContext context,
                    AuthConfiguration configuration, IdentityService identityService, LoginService loginService)
            {
                // do the normal stuff, see the parent class

                String auth = configuration.getAuthMethod();
                Authenticator authenticator = null;

                if (auth == null || Constraint.__BASIC_AUTH.equalsIgnoreCase(auth)) {
                    authenticator = new BasicAuthenticator() {
                        @Override
                        public UserIdentity login(String username, Object password, ServletRequest request) {
                            UserIdentity user = WebappPasswordFieldMatcher.this.login(this, username, request);
                            if (user != null) {
                                renewSession((HttpServletRequest) request, (request instanceof Request
                                        ? ((Request) request).getResponse() : null));
                                // the parent class doesn't call that, but if it is not present
                                // the session is not created (and I don't know why)
                                ((HttpServletRequest) request).getSession(true); // ensure to have a session
                                return user;
                            }
                            return null;
                        }
                    };
                } else if (Constraint.__DIGEST_AUTH.equalsIgnoreCase(auth)) {
                    authenticator = new DigestAuthenticator();
                } else if (Constraint.__FORM_AUTH.equalsIgnoreCase(auth)) {
                    authenticator = new FormAuthenticator() {
                        @Override
                        public UserIdentity login(String username, Object password, ServletRequest request) {
                            UserIdentity user = WebappPasswordFieldMatcher.this.login(this, username, request);
                            if (user != null) {
                                renewSession((HttpServletRequest) request, (request instanceof Request
                                        ? ((Request) request).getResponse() : null));
                            }
                            if (user != null) {
                                HttpSession session = ((HttpServletRequest) request).getSession(true);
                                // NOTE : "pwd" is invalidate() when leaving the stack
                                // "password" just contains "*****" and won't be usable for
                                //        session serialization/deserialization (used for login)
                                // IT IS A BAD PRACTICE TO SERIALIZE PASSWORDS (like Jetty can do)
                                Authentication cached = new SessionAuthentication(getAuthMethod(), user, password);
                                session.setAttribute(SessionAuthentication.__J_AUTHENTICATED, cached);
                            }
                            return user;
                        }
                    };
                } else if ( Constraint.__SPNEGO_AUTH.equalsIgnoreCase(auth) ) {
                    authenticator = new SpnegoAuthenticator();
                } else if ( Constraint.__NEGOTIATE_AUTH.equalsIgnoreCase(auth) ) { // see Bug #377076
                    authenticator = new SpnegoAuthenticator(Constraint.__NEGOTIATE_AUTH);
                } else if (Constraint.__CERT_AUTH.equalsIgnoreCase(auth)
                        || Constraint.__CERT_AUTH2.equalsIgnoreCase(auth))
                {
                    authenticator = new ClientCertAuthenticator();
                }
                return authenticator;
            }
        };
    }

    @Override
    public Optional<List<String>> matches(HttpServletRequest request) {
        // IMPL NOTE : the ServletContext can't be extract from the request
        // because Jetty didn't set it yet
        ServletContext sc = wac.getServletContext();
        return FormFieldConfiguration.matches(sc, request);
    }

    @Override
    public AuthenticationMethod getAuthenticationMethod() {
        ServletContext sc = wac.getServletContext();
        return AuthenticationMethod.extract(sc);
    }

    @Override
    public int getMaxFormContentSize() {
        return wac.getMaxFormContentSize();
    }

    @Override
    public int getMaxFormKeys() {
        return wac.getMaxFormKeys();
    }

}
