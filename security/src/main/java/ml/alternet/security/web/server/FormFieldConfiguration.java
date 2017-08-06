package ml.alternet.security.web.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import ml.alternet.security.web.Config;

/**
 * Base class for handling form fields.
 *
 * @author Philippe Poulard
 */
public class FormFieldConfiguration {

    /**
     * Target path for Form Authentication.
     */
    public static final String J_SECURITY_CHECK = "/j_security_check";

    /**
     * Form field name for the username.
     */
    public static final String J_USERNAME = "j_username";

    /**
     * Form field name for the password.
     */
    public static final String J_PASSWORD = "j_password";

    /** The path to the Webapp. */
    protected String ctxtPath;

    /** A map of {path, fields}. */
    protected Map<String, List<String>> fields = new HashMap<>();

    /**
     * Extract the parameters from the Webapp configuration.
     *
     * <pre>
     *&lt;context-param&gt;
     *    &lt;param-name&gt;ml.alternet.security.web.config.formFields&lt;/param-name&gt;
     *    &lt;param-value&gt;
     *          /doRegister.html?pwd&amp;confirmPwd,
     *          /doUpdatePassword.html?oldPwd&amp;newPwd&amp;confirmPwd
     *    &lt;/param-value&gt;
     *&lt;/context-param&gt;
     * </pre>
     *
     * @param webapp The Web application.
     */
    protected void extract(ServletContext webapp) {
        // the value of the context param has the form :
        // "/register.html?pwd&confirmPwd, /updatePassword.html?oldPwd&newPwd&confirmPwd"
        String initParam = webapp.getInitParameter(Config.FORM_FIELDS_INIT_PARAM);
        if (initParam != null) {
            String[] conf = initParam.split("\\s*,\\s*");
            for (String c: conf) {
                String[] parts = c.split("\\?");
                String[] formFields = parts[1].split("&");
                addValues(parts[0], formFields);
            }
        }
    }

    /**
     * Add the login form value if the authentication method
     * is "Form" (that is to say, bound the form field
     * "j_password" to the path "/j_security_check" in that
     * context).
     *
     * @param webapp The Web application.
     */
    protected void addLoginFormValue(ServletContext webapp) {
        AuthenticationMethod method = AuthenticationMethod.extract(webapp);
        if (method == AuthenticationMethod.Form) {
            // same as "/j_security_check?j_password"
            addValues(J_SECURITY_CHECK, J_PASSWORD);
        }
    }

    /**
     * Called by the extractor for each single parameter found in the configuration.
     *
     * @param path The path
     * @param formFields The fields
     */
    protected void addValues(String path, String... formFields) {
        path = ctxtPath + path;
        this.fields.put(path, Arrays.asList(formFields));
    };

    /**
     * Get the fields bound to a path.
     *
     * @param path The path.
     * @return The fields.
     */
    protected List<String> get(String path) {
        return this.fields.get(path);
    }

    /**
     * Check whether the path of an HTTP request matches
     * one of those found in the configuration,
     * and return the list of passwords fields that have
     * to be captured in a request.
     *
     * @param webapp The Web application.
     * @param request The HTTP request.
     * @return If the request matches the configuration,
     *      return the list of field names.
     */
    public static Optional<List<String>> matches(ServletContext webapp, HttpServletRequest request) {
        return matches(webapp, request, FormFieldConfiguration::new);
    }

    /**
     * Check whether the path of an HTTP request matches
     * one of those found in the configuration,
     * and return the list of passwords fields that have
     * to be captured in a request.
     *
     * @param webapp The Web application.
     * @param request The HTTP request.
     * @param factory For creating a specific instance of this class.
     * @return If the request matches the configuration,
     *      return the list of field names.
     */
    public static Optional<List<String>> matches(ServletContext webapp, HttpServletRequest request,
            Supplier<FormFieldConfiguration> factory)
    {
        // IMPL NOTE : the ServletContext can't be extract from the request
        // because some container might not have been set it yet

        String ctxtPath = webapp.getContextPath(); // "/webapp"
        String target = request.getRequestURI();   // "/webapp/doRegister.html"
        if (target.startsWith(ctxtPath)) {
            FormFieldConfiguration forms = (FormFieldConfiguration)
                    webapp.getAttribute(FormFieldConfiguration.class.getName());
            if (forms == null) {
                // we don't care if there is a race : no lock
                forms = factory.get();
                forms.ctxtPath = ctxtPath; //sc.getContextPath();
                forms.extract(webapp);
                forms.addLoginFormValue(webapp);
                webapp.setAttribute(FormFieldConfiguration.class.getName(), forms);
            }
            List<String> fields = forms.get(target);
            return Optional.ofNullable(fields);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Remove this attribute from the Web application.
     *
     * @param webapp The Web application.
     */
    public static void reset(ServletContext webapp) {
        webapp.removeAttribute(FormFieldConfiguration.class.getName());
    }

}
