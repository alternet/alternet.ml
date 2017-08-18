package ml.alternet.test.security.web.jetty;

import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;

import org.assertj.core.api.Assertions;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
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
import ml.alternet.misc.Thrower;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.security.web.Config;
import ml.alternet.security.web.jetty.EnhancedHttpConnectionFactory;
import ml.alternet.test.security.web.server.FormAuthServerTestHarness;

/**
 * The tests that show on Form authentication that the password is captured by
 * Jetty (and encrypted), and that the raw value of the password in the
 * incoming buffer is replaced with '*'.
 *
 * @author Philippe Poulard
 */
@Test
public class FormAuthTest extends FormAuthServerTestHarness<Server> {

    static {
        name = Methref.on(FormAuthTest.class);
    }

    /**
     * Jetty specific test
     */
    @Override
    public String checkSession(HttpServletRequest req) {
        SessionAuthentication sauth = (SessionAuthentication) req.getSession().getAttribute(SessionAuthentication.__J_AUTHENTICATED);
        name.to().sessionAuthentication_ShouldBe_Define();
        serverTests.put(name.ref(),
            () -> Assertions.assertThat(sauth)
            .as("REST server : session authentication should be defined")
            .isNotNull()
        );

        try {
            // unable to check that sauth._credentials is "******" since it is a private field
            Field field = SessionAuthentication.class.getDeclaredField("_credentials");
            field.setAccessible(true);
            String credentials = "" + field.get(sauth);
            return credentials;
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            return Thrower.doThrow(e);
        }
    }

    @Override
    @Test(enabled = false)
    public void doStopServer() throws Exception {
        server.stop();
//        server.join();
        server.destroy();
        server = null;
    }

    @Override
    @Test(enabled = false)
    public void doStartServer() throws Exception {
        WebAppContext wac = new WebAppContext();
        // a reference to this test
        wac.setAttribute(FormAuthServerTestHarness.class.getName(), this);

        EnhancedHttpConnectionFactory cf = new EnhancedHttpConnectionFactory(wac);

        server = new Server();
        ServerConnector connector=new ServerConnector(server, cf);
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});

        ServletHolder sh = new ServletHolder("JERSEY_SERVLET", ServletContainer.class);
        String classes = ExampleRequest.class.getName() + " ";
        sh.setInitParameter(ServerProperties.PROVIDER_CLASSNAMES, classes);

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(sh, "/protected/*");

        wac.setServletHandler(servletHandler);
        wac.setResourceBase(resourceBase);
        wac.setContextPath(contextPath);

        // configure the security (standard fashion)
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__FORM_AUTH);
        constraint.setRoles(new String[]{"customer","admin"});
        constraint.setAuthenticate(true);
        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/protected/*");
        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        MappedLoginServiceImpl loginService = new MappedLoginServiceImpl();
        loginService.setHasher(ModularCryptFormatHashers.$2$.get().build());
        loginService.setName("realm");
        loginService.putUser(userName, Credentials.fromPassword(unsafePwd.toCharArray()), new String[] {"admin"});
        security.setLoginService(loginService);
        security.setAuthMethod(Constraint.__FORM_AUTH);
        security.setConstraintMappings(new ConstraintMapping[]{cm});
        security.setInitParameter(FormAuthenticator.__FORM_LOGIN_PAGE, "/login.html");
        security.setInitParameter(FormAuthenticator.__FORM_ERROR_PAGE, "/authFail.html");

        wac.setSecurityHandler(security);
        wac.setInitParameter(Config.AUTH_METHOD_INIT_PARAM, "Form");

        server.setHandler(wac);

        server.start();
//        server.dumpStdErr();
    }

}
