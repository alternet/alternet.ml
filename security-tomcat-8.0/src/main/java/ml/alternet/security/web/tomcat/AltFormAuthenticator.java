package ml.alternet.security.web.tomcat;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;

/**
 * Prepare Form authentication check.
 *
 * @author Philippe Poulard
 */
public class AltFormAuthenticator extends FormAuthenticator {

    @Override
    public boolean authenticate(Request request, HttpServletResponse response) throws IOException {
        AltProtocolHandler.request.set(request);
        try {
            return super.authenticate(request, response);
        } finally {
            AltProtocolHandler.request.remove();
        }
    }

}
