package ml.alternet.test.security.web.tomcat;

import jodd.methref.Methref;
import ml.alternet.security.web.Config;
import ml.alternet.test.security.web.server.RESTServerTestHarness;
import ml.alternet.test.security.web.server.RESTServerTestHarness.RestApp;

import java.util.Objects;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ApplicationParameter;
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
public class RESTTest extends RESTServerTestHarness<Tomcat> implements TomcatSupplier {

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
        server = get();
        server.getEngine().setName(server.getEngine().getName() + "-" + Objects.hashCode(server.getEngine()));

        // use the specific connector
        Connector connector = new Connector("ml.alternet.security.web.tomcat.AltProtocolHandler");
        connector.setPort(port);
        // set in "tomcatProtocol" what you would have set in the  Connector constructor
        connector.setProperty("tomcatProtocol", "HTTP/1.1");
        connector.setProperty("passwordManager", "ml.alternet.security.impl.StrongPasswordManager");
        server.getService().addConnector(connector);
        server.setConnector(connector);

        // create a REST webapp
        Context wac = server.addContext(contextPath, resourceBase);
        Wrapper servlet = Tomcat.addServlet(wac, "JERSEY_SERVLET", new ServletContainer());
        String classes = ExampleRequest.class.getName() + " ";
        servlet.addInitParameter(ServerProperties.PROVIDER_CLASSNAMES, classes);
        servlet.addInitParameter("javax.ws.rs.Application", RestApp.class.getName());
        wac.addServletMapping("/*", "JERSEY_SERVLET");

        // a reference to this test
        wac.getServletContext().setAttribute(RESTServerTestHarness.class.getName(), this);

        // configure for handling the "pwd" field when POSTing on the "/Test" path
        ApplicationParameter initParam = new ApplicationParameter();
        initParam.setName(Config.FORM_FIELDS_INIT_PARAM);
        initParam.setValue("/Test?pwd");
        wac.addApplicationParameter(initParam);

        server.start();
    }

}
