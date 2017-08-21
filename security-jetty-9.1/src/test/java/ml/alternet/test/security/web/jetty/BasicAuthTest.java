package ml.alternet.test.security.web.jetty;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.testng.annotations.Test;

import jodd.methref.Methref;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.formats.PlainTextCryptFormat;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.security.web.Config;
import ml.alternet.security.web.jetty.EnhancedHttpConnectionFactory;
import ml.alternet.test.security.web.server.BasicAuthServerTestHarness;

/**
 * The tests that show on BASIC authentication that the password is captured by
 * Jetty (and encrypted), and that the raw value of the password in the
 * incoming buffer is replaced with '*'.
 *
 * @author Philippe Poulard
 */
@Test
public class BasicAuthTest extends BasicAuthServerTestHarness<Server> {

    static {
        name = Methref.on(BasicAuthTest.class);
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
        WebAppContext wac = new WebAppContext();
        // a reference to this test
        wac.setAttribute(BasicAuthServerTestHarness.class.getName(), this);

        EnhancedHttpConnectionFactory cf = new EnhancedHttpConnectionFactory(wac);

        server = new Server();
        ServerConnector connector=new ServerConnector(server, cf);
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});

        ServletHolder sh = new ServletHolder("JERSEY_SERVLET", ServletContainer.class);
        String classes = ExampleRequest.class.getName() + " ";
        sh.setInitParameter(ServerProperties.PROVIDER_CLASSNAMES, classes);

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(sh, "/*");

        wac.setServletHandler(servletHandler);
        wac.setResourceBase(resourceBase);
        wac.setContextPath(contextPath);

        // configure the security (standard fashion)
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"customer","admin"});
        constraint.setAuthenticate(true);

        MappedLoginServiceImpl loginService = new MappedLoginServiceImpl();
        loginService.setName("realm");
        loginService.setCryptFormats(
                ModularCryptFormat.class.getName(),
                CurlyBracesCryptFormat.class.getName(),
                PlainTextCryptFormat.class.getName()
        );
        String crypt = ModularCryptFormatHashers.$2$.get().build()
            .encrypt(unsafePwd.toCharArray());
        loginService.putUser(userName, crypt, new String[] {"admin"});

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        security.setAuthMethod(Constraint.__BASIC_AUTH);
        security.setLoginService(loginService);
        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");
        security.setConstraintMappings(new ConstraintMapping[]{cm});

        wac.setSecurityHandler(security);
        wac.setInitParameter(Config.AUTH_METHOD_INIT_PARAM, "Basic");

        server.setHandler(wac);

        server.start();
//        server.dumpStdErr();
    }

}
