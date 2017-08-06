package ml.alternet.security.web;

/**
 * Define configuration properties for Web applications.
 *
 * @author Philippe Poulard
 */
public interface Config {

    /**
     * "<code>ml.alternet.security.web.config.formFields</code>"
     * indicates which form fields to process as a password.
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
     */
    String FORM_FIELDS_INIT_PARAM = Config.class.getName().toLowerCase() + ".formFields";

    /**
     * "<code>ml.alternet.security.web.config.authenticationMethod</code>"
     * Indicates whether HTTP Basic or Form Authentication has to be processed.
     *
     * <pre>
     *&lt;context-param&gt;
     *    &lt;param-name&gt;ml.alternet.security.web.config.authenticationMethod&lt;/param-name&gt;
     *    &lt;param-value&gt;Basic&lt;/param-value&gt;
     *&lt;/context-param&gt;
     * </pre>
     */
    String AUTH_METHOD_INIT_PARAM = Config.class.getName().toLowerCase()
            + ".authenticationMethod";

}
