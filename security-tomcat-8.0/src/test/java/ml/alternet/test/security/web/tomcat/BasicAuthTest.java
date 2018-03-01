package ml.alternet.test.security.web.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ApplicationParameter;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.testng.annotations.Test;

import jodd.methref.Methref;
import ml.alternet.security.web.Config;
import ml.alternet.security.web.tomcat.AltBasicAuthenticator;
import ml.alternet.security.web.tomcat.AltCredentialHandler;
import ml.alternet.test.security.web.server.BasicAuthServerTestHarness;

/**
 * The tests that show on BASIC authentication that the password is captured by
 * Tomcat (and encrypted), and that the raw value of the password in the
 * incoming buffer is replaced with '*'.
 *
 * @author Philippe Poulard
 */
public class BasicAuthTest extends BasicAuthServerTestHarness<Tomcat> implements TomcatSupplier {

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
        server = get();

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
        wac.addServletMapping("/*", "JERSEY_SERVLET");

        // a reference to this test
        wac.getServletContext().setAttribute(BasicAuthServerTestHarness.class.getName(), this);

        // configure the security (standard fashion)
        wac.setPreemptiveAuthentication(true);
        wac.setPrivileged(true);
        wac.addSecurityRole("customer");
        wac.addSecurityRole("admin");
        wac.setLoginConfig(new LoginConfig("BASIC", "realm", "/login.html", "/error.html"));
        SecurityConstraint security = new SecurityConstraint();
        security.addAuthRole("admin");
        security.setAuthConstraint(true);
        SecurityCollection coll = new SecurityCollection();
        coll.addMethod("GET");
        coll.addPattern("/*");
        security.addCollection(coll);
        wac.addConstraint(security);

        // specific security config
        ApplicationParameter initParam = new ApplicationParameter();
        initParam.setName(Config.AUTH_METHOD_INIT_PARAM);
        initParam.setValue("Basic");
        wac.addApplicationParameter(initParam);

        AuthenticatorBase auth = new AltBasicAuthenticator();
        auth.setSecurePagesWithPragma(false);
        auth.setChangeSessionIdOnAuthentication(false);
        auth.setAlwaysUseSession(true);
        ((StandardContext) wac).addValve(auth);

        MappedRealm realm = new MappedRealm();
        realm.putUser(userName, unsafePwd.toCharArray(), new String[] {"admin"});
        // specific conf
        realm.setCredentialHandler(new AltCredentialHandler());
        wac.setRealm(realm);

        server.start();
    }

}
