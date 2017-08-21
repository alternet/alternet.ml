package ml.alternet.test.security.web.tomcat;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ApplicationParameter;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.testng.annotations.Test;

import jodd.methref.Methref;
import ml.alternet.security.web.Config;
import ml.alternet.security.web.tomcat.AlternetCredentialHandler;
import ml.alternet.security.web.tomcat.AlternetFormAuthenticator;
import ml.alternet.test.security.web.server.FormAuthServerTestHarness;

/**
 * The tests that show on Form authentication that the password is captured by
 * Tomcat (and encrypted), and that the raw value of the password in the
 * incoming buffer is replaced with '*'.
 *
 * @author Philippe Poulard
 */
public class FormAuthTest extends FormAuthServerTestHarness<Tomcat> implements TomcatSupplier {

    static {
        name = Methref.on(FormAuthTest.class);
    }

    /**
     * Tomcat specific test
     */
    @Override
    public String checkSession(HttpServletRequest req) {
        HttpSession session = req.getSession();
        name.to().sessionAuthentication_ShouldBe_Define();
        serverTests.put(name.ref(),
            () -> Assertions.assertThat(session)
            .as("REST server : session authentication should be defined")
            .isNotNull()
        );
        return null; // Tomcat remove the password from the session notes
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

        // use the specific connector
        Connector connector = new Connector("ml.alternet.security.web.tomcat.EnhancedProtocolHandler");
//        Connector connector = new Connector("HTTP/1.1");
        connector.setPort(port);
        // set in "tomcatProtocol" what you would have set in the  Connector constructor
        connector.setProperty("tomcatProtocol", "HTTP/1.1");
        connector.setProperty("passwordManager", "ml.alternet.security.impl.StrongPasswordManager");
        server.getService().addConnector(connector);
        server.setConnector(connector);

        // create a REST webapp
        String resourceBase = new File("src/main/resources/").getAbsolutePath();
        Context wac = server.addContext(contextPath, resourceBase);
        wac.setUseHttpOnly(false);
        Wrapper servlet = Tomcat.addServlet(wac, "JERSEY_SERVLET", new ServletContainer());
        String classes = ExampleRequest.class.getName() + " ";
        servlet.addInitParameter(ServerProperties.PROVIDER_CLASSNAMES, classes);
        wac.addServletMapping("/protected/*", "JERSEY_SERVLET");
        wac.addServletMapping("/go/*", "JERSEY_SERVLET");
        // a reference to this test
        wac.getServletContext().setAttribute(FormAuthServerTestHarness.class.getName(), this);

        @SuppressWarnings("unused")
        Wrapper defaultServlet = Tomcat.addServlet(wac, "default", new DefaultServlet());
        wac.addServletMapping("/login.html", "default");

        // configure the security (standard fashion)
        wac.setPreemptiveAuthentication(true);
        wac.setPrivileged(true);
        wac.addSecurityRole("customer");
        wac.addSecurityRole("admin");
        wac.setLoginConfig(new LoginConfig("FORM", "realm", "/login.html", "/authFail.html"));
        SecurityConstraint security = new SecurityConstraint();
        security.addAuthRole("admin");
        security.setAuthConstraint(true);
        SecurityCollection coll = new SecurityCollection();
        coll.addMethod("GET");
        coll.addPattern("/protected/*");
        security.addCollection(coll);
        wac.addConstraint(security);

        // specific security config
        ApplicationParameter initParam = new ApplicationParameter();
        initParam.setName(Config.AUTH_METHOD_INIT_PARAM);
        initParam.setValue("Form");
        wac.addApplicationParameter(initParam);

        AuthenticatorBase auth = new AlternetFormAuthenticator();
        auth.setSecurePagesWithPragma(false);
        auth.setChangeSessionIdOnAuthentication(true);
        auth.setAlwaysUseSession(true);
        ((StandardContext) wac).addValve(auth);

        MappedRealm realm = new MappedRealm();
        realm.putUser(userName, unsafePwd.toCharArray(), new String[] {"admin"});
        // specific conf
        realm.setCredentialHandler(new AlternetCredentialHandler());
        wac.setRealm(realm);

        server.start();

    }

}
