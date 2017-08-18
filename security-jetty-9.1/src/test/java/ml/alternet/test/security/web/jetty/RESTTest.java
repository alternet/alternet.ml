package ml.alternet.test.security.web.jetty;

import jodd.methref.Methref;
import ml.alternet.security.web.Config;
import ml.alternet.security.web.jetty.EnhancedHttpConnectionFactory;
import ml.alternet.test.security.web.server.RESTServerTestHarness;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.testng.annotations.Test;

/**
 * The tests that show that the passwords are captured by
 * Jetty (and encrypted), and that the raw value of the password in the
 * incoming buffer is replaced with '*'.
 *
 * @author Philippe Poulard
 */
public class RESTTest extends RESTServerTestHarness<Server> {

    static {
        name = Methref.on(RESTTest.class);
    }

    @Override
    @Test(enabled = false)
    public void doStopServer() throws Exception {
        server.stop();
        server.destroy();
        server = null;
    }

    @Override
    @Test(enabled = false)
    public void doStartServer() throws Exception {
        if (server == null) {
            WebAppContext wac = new WebAppContext();
            // a reference to this test
            wac.setAttribute(RESTServerTestHarness.class.getName(), this);

            EnhancedHttpConnectionFactory cf = new EnhancedHttpConnectionFactory(wac);

            server = new Server();
            ServerConnector connector=new ServerConnector(server, cf);
            connector.setPort(port);
            server.setConnectors(new Connector[]{connector});

            ServletHolder sh = new ServletHolder("JERSEY_SERVLET", ServletContainer.class);
            String classes = ExampleRequest.class.getName() + " ";
            sh.setInitParameter(ServerProperties.PROVIDER_CLASSNAMES, classes);
            sh.setInitParameter("javax.ws.rs.Application", RestApp.class.getName());

            ServletHandler servletHandler = new ServletHandler();
            servletHandler.addServletWithMapping(sh, "/*");

            wac.setServletHandler(servletHandler);
            wac.setResourceBase(resourceBase);
            wac.setContextPath(contextPath);
            // configure for handling the "pwd" field when POSTing on the "/Test" path
            wac.setInitParameter(Config.FORM_FIELDS_INIT_PARAM, "/Test?pwd");

            server.setHandler(wac);

            server.start();
//            server.dumpStdErr();
        }
    }

}
