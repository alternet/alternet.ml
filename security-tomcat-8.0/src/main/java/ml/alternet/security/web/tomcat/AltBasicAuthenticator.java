package ml.alternet.security.web.tomcat;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.connector.Request;

/**
 * Prepare BASIC authentication check.
 *
 * @author Philippe Poulard
 */
public class AlternetBasicAuthenticator extends BasicAuthenticator {

    @Override
    public boolean authenticate(Request request, HttpServletResponse response) throws IOException {
        EnhancedProtocolHandler.request.set(request);
        try {
            return super.authenticate(request, response);
        } finally {
            EnhancedProtocolHandler.request.remove();
        }
    }

}
