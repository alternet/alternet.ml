package ml.alternet.security.web.server;

import java.util.List;
import java.util.Optional;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * According to the Web application configuration,
 * determine whether an incoming HTTP request contains
 * passwords to capture.
 *
 * <p>A typical matcher will check whether the URI request
 * will match some given paths.</p>
 *
 * @author Philippe Poulard
 */
public interface PasswordFieldMatcher {

    /**
     * Check whether an HTTP request matches this matcher,
     * and return the list of passwords fields that have
     * to be captured in a request.
     *
     * @param request The HTTP request.
     * @return If the request matches the configuration,
     *      return the list of field names.
     */
    Optional<List<String>> matches(HttpServletRequest request);

    /**
     * Indicates whether HTTP Basic | Form authentication has to
     * be processed for a given request.
     *
     * @param request The HTTP request.
     * @return The authentication method.
     */
    AuthenticationMethod getAuthenticationMethod(ServletRequest request);

}
