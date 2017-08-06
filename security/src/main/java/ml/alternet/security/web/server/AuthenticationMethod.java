package ml.alternet.security.web.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import ml.alternet.security.Password;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.web.Config;
import ml.alternet.security.web.Passwords;

/**
 * Authentication methods.
 *
 * @author Philippe Poulard
 */
public enum AuthenticationMethod {

    /**
     * HTTP Basic Authentication.
     */
    Basic {
        @Override
        public Credentials getCredentials(ServletRequest request) {
            Credentials credentials = (Credentials) request.getAttribute(Passwords.BASIC_AUTH_ATTRIBUTE_KEY);
            return credentials;
        }
    },

    /**
     * Form Authentication.
     */
    Form {
        @Override
        public Credentials getCredentials(ServletRequest request) {
            String username = request.getParameter(FormFieldConfiguration.J_USERNAME);
            Password pwd = Passwords.getPasswords(request, FormFieldConfiguration.J_PASSWORD);
            return Credentials.fromUserPassword(username, pwd);
        }
    },

    /**
     * Other authentication.
     */
    Other,

    /**
     * No authentication.
     */
    None;

    /**
     * Get the user credentials after authentication.
     *
     * @param request The request.
     *
     * @return The user credentials.
     */
    public Credentials getCredentials(ServletRequest request) {
        return new Credentials();
    }

    /**
     * Extract the authentication method from the init parameter of
     * the Web application.
     *
     * <pre>
     *&lt;context-param&gt;
     *    &lt;param-name&gt;ml.alternet.security.web.config.authenticationMethod&lt;/param-name&gt;
     *    &lt;param-value&gt;Basic&lt;/param-value&gt;
     *&lt;/context-param&gt;
     * </pre>
     *
     * Once extracted, the parameter is set as an attribute of the application.
     *
     * @see #reset(ServletContext)
     *
     * @param webapp The Web application
     *
     * @return The non-null authentication method.
     */
    public static AuthenticationMethod extract(ServletContext webapp) {
        AuthenticationMethod authMethod = (AuthenticationMethod) webapp
                .getAttribute(AuthenticationMethod.class.getName());
        if ( authMethod == null) {
            String initParam = webapp.getInitParameter(Config.AUTH_METHOD_INIT_PARAM);
            if (initParam != null && initParam.length() > 0) {
                char[] chars = initParam.toLowerCase().toCharArray();
                chars[0] = Character.toUpperCase(chars[0]);
                initParam = new String(chars);
                try {
                    authMethod = AuthenticationMethod.valueOf(initParam);
                } catch (IllegalArgumentException e) {
                    authMethod = Other;
                }
                webapp.setAttribute(AuthenticationMethod.class.getName(), authMethod);
            } else {
                authMethod = None;
            }
        }
        return authMethod;
    }

    /**
     * Remove this attribute from the Web application.
     *
     * @see #extract(ServletContext)
     *
     * @param webapp The Web application.
     */
    public static void reset(ServletContext webapp) {
        webapp.removeAttribute(AuthenticationMethod.class.getName());
    }

}
