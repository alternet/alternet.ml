package ml.alternet.security.web;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import ml.alternet.security.Password;

/**
 * JAX-RS provider that converts a form parameter or a header parameter to
 * a secure password (query parameter are not handled by this
 * converter since it would be a security flaw to send a password
 * in the URI).
 * <br>
 * <pre>
 *{@literal @}POST
 * public String login
 *       ({@literal @}FormParam("username") String userName,
 *        {@literal @}FormParam("password") Password pwd) {
 *     // ...
 * }
 * </pre>
 * <br>
 * If the pwd field is multivalued, don't use <code>List&lt;Password&gt;</code>, use instead :
 * <br>
 * <pre>
 *{@literal @}POST
 * public String registerNewUser
 *       ({@literal @}FormParam("username") String userName,
 *        {@literal @}FormParam("password") PasswordParam pwd) {
 *     // ...
 * }
 * </pre>
 *
 * As a {@link javax.ws.rs.ext.Provider}, this class has to be registered to the application manually, or
 * by setting on the lookup mechanism of the underlying JAX-RS implementation.
 *
 * Note that at the invocation stage, the password string passed to the converter is a dummy
 * string not involved in the conversion process.
 *
 * @see javax.ws.rs.FormParam
 * @see javax.ws.rs.HeaderParam
 * @see javax.ws.rs.QueryParam
 *
 * @author Philippe Poulard
 */
@Provider
public class PasswordConverterProvider implements ParamConverterProvider {

    static final Logger LOGGER = Logger.getLogger(PasswordConverterProvider.class.getName());

    @Context
    HttpServletRequest httpServletRequest;

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Password.class.equals(rawType) || PasswordParam.class.equals(rawType)) {
            if (httpServletRequest == null) {
                LOGGER.warning("Unable to inject HttpServletRequest ; please check "
                        + "your JAX-RS engine capabilities and Web server configuration.");
            }
            String fieldName = null;
            for (Annotation ann : annotations) {
                if (ann.annotationType().equals(FormParam.class)) {
                    FormParam fp = (FormParam) ann;
                    fieldName = fp.value();
                    break;
                }
                if (ann.annotationType().equals(HeaderParam.class)) {
                    HeaderParam hp = (HeaderParam) ann;
                    fieldName = hp.value();
                    break;
                }
                if (ann.annotationType().equals(QueryParam.class)) {
                    // fail now or later ?
                    processQueryParam((QueryParam) ann);
                }
            }
            return (ParamConverter<T>) new PasswordConverter(httpServletRequest, fieldName);
        } else {
            return null;
        }
    }

    /**
     * This method is called during annotation processing when a password
     * field is annotated with <tt>{@literal @}QueryParam</tt>, and throws
     * a security exception.
     *
     * Sending a password field in the request URI is a security flaw
     * and MUST NOT be handled by an application.
     *
     * By default, a security exception will be thrown and the underlying
     * Web application won't be available, which force the developer to
     * fix the application.
     *
     * This method can be override if this default behavior is too severe.
     * Instead of throwing an exception this method can do nothing : in
     * this case the Web application will be available and a warning will
     * be logged only when processing the query param.
     *
     * @param passwordFieldAnnotation The annotation.
     * @throws SecurityException By default, this implementation throw a security exception
     */
    protected void processQueryParam(QueryParam passwordFieldAnnotation) throws SecurityException {
        throw new SecurityException(SECURITY_ERROR_MSG);
    }

    private static final String SECURITY_ERROR_MSG = "Webapp design error (security flaw) :"
            + "a password field must not be annotated with @QueryParam.";

    private static final class PasswordConverter implements ParamConverter<PasswordParam> {

        static final Logger LOGGER = Logger.getLogger(PasswordConverter.class.getName());

        private final String name;
        private final HttpServletRequest httpServletRequest;

        private PasswordConverter(HttpServletRequest httpServletRequest, String name) {
            this.name = name;
            this.httpServletRequest = httpServletRequest;
        }

        @Override
        public PasswordParam fromString(String value) {
            // value contains the string "*****"
            // otherwise the Web container has not been configured as expected
            if (name == null) {
                // exception will cause 404 NOT FOUND, therefore we log
                LOGGER.warning(SECURITY_ERROR_MSG);
            }
            PasswordParam pp = Passwords.getPasswords(httpServletRequest, name);
            LOGGER.info("Password parameter \"" + name + "\" filtered safely");
            return pp;
        }

        @Override
        public String toString(PasswordParam value) {
            return value.toString();
        }
    }

}

