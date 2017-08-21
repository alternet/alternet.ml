package ml.alternet.test.security.web.jetty;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.servlet.http.HttpServletRequest;

import org.assertj.core.api.Assertions;
import org.eclipse.jetty.jaas.JAASLoginService;
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
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.formats.PlainTextCryptFormat;
import ml.alternet.security.web.Config;
import ml.alternet.security.web.jetty.EnhancedHttpConnectionFactory;
import ml.alternet.security.web.jetty.auth.EnhancedLdapLoginModule;
import ml.alternet.test.security.web.jetty.ldap.LdapServer;
import ml.alternet.test.security.web.server.FormAuthServerTestHarness;

/**
 * LDAP tests that show on Form authentication that the password is captured by
 * Jetty (and encrypted), and that the raw value of the password in the
 * incoming buffer is replaced with '*'.
 *
 * @author Philippe Poulard
 */
@Test
public class LDAPAuthTest extends FormAuthServerTestHarness<Server> {

    static {
        name = Methref.on(LDAPAuthTest.class);
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

    LdapServer ldapServer;
    Configuration conf;

    @Override
    @Test(enabled = false)
    public void doStopServer() throws Exception {
        server.stop();
        //        server.join();
        server.destroy();
        server = null;

        ldapServer.stop();
        ldapServer = null;

        Configuration.setConfiguration(conf);
    }

    @Override
    @Test(enabled = false)
    public void doStartServer() throws Exception {
        this.userName = "lsimpson";

        conf = Configuration.getConfiguration();
        setJaasConfiguration();

        WebAppContext wac = new WebAppContext();
        // a reference to this test
        wac.setAttribute(FormAuthServerTestHarness.class.getName(), this);

        EnhancedHttpConnectionFactory cf = new EnhancedHttpConnectionFactory(wac);

        server = new Server();
        ServerConnector connector=new ServerConnector(server, cf);
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});

        ldapServer = new LdapServer();
        ldapServer.setPort(10389);
        ldapServer.setBaseDn("dc=alternet,dc=ml");
        String ldifFile = LdapServer.class.getResource("users.ldif").getFile();
        ldapServer.setLdifFiles(ldifFile);
        server.addBean(ldapServer);

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

        JAASLoginService loginService = new JAASLoginService();
        loginService.setName("Alternet Realm");
        loginService.setLoginModuleName("ldap");

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        security.setRealmName("Alternet Realm");
        security.setLoginService(loginService);
        security.setAuthMethod(Constraint.__FORM_AUTH);
        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/protected/*");
        security.setConstraintMappings(new ConstraintMapping[]{cm});
        security.setInitParameter(FormAuthenticator.__FORM_LOGIN_PAGE, "/login.html");
        security.setInitParameter(FormAuthenticator.__FORM_ERROR_PAGE, "/authFail.html");

        wac.setSecurityHandler(security);
        wac.setInitParameter(Config.AUTH_METHOD_INIT_PARAM, "Form");

        server.setHandler(wac);

        server.start();
        //        server.dumpStdErr();
    }

    protected void setJaasConfiguration() {
        Configuration.setConfiguration(new Configuration(){
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name){
                if ("ldap".equals(name)) {
                    final Map<String,Object> options=new HashMap<String,Object>();
/*
                    options.put("useSSL", "false");
                    options.put("debug","true");
                    options.put("contextFactory","com.sun.jndi.ldap.LdapCtxFactory");
                    options.put("userProvider","ldap://localhost:10389/ou=people,dc=alternet,dc=ml");
                    options.put("userFilter","(&(uid={USERNAME})(objectClass=inetOrgPerson))");

                    options.put("authenticationMethod", "simple");
                    options.put("forceBindingLogin", "false");
                    options.put("userBaseDn", "ou=people,dc=alternet,dc=ml");
                    options.put("userRdnAttribute", "uid");
                    options.put("userIdAttribute", "uid");
                    options.put("userPasswordAttribute", "userPassword");
                    options.put("userObjectClass", "inetOrgPerson");
                    options.put("roleBaseDn", "ou=groups,dc=alternet,dc=ml");
                    options.put("roleNameAttribute", "cn");
                    options.put("roleMemberAttribute", "uniqueMember");
                    options.put("roleObjectClass", "groupOfUniqueNames");
*/
                    options.put("useSSL", "false");
                    options.put("debug","true");
                    options.put("contextFactory","com.sun.jndi.ldap.LdapCtxFactory");
                    options.put("hostname","localhost");
                    options.put("port","10389");

                    options.put("bindDn","ou=people,dc=alternet,dc=ml");
                    options.put("bindPassword","secret");
                    options.put("authenticationMethod", "simple");
                    options.put("forceBindingLogin", "false");

                    options.put("userBaseDn", "ou=people,dc=alternet,dc=ml");
                    options.put("userRdnAttribute", "uid");
                    options.put("userIdAttribute", "uid");
                    options.put("userPasswordAttribute", "userPassword");
                    options.put("userObjectClass", "inetOrgPerson");

                    options.put("roleBaseDn", "ou=groups,dc=alternet,dc=ml");
                    options.put("roleNameAttribute", "cn");
                    options.put("roleMemberAttribute", "member");
                    options.put("roleObjectClass", "groupOfNames");

                    options.put(CryptFormat.class.getName(),
                            ModularCryptFormat.class.getName()
                            + " , " + CurlyBracesCryptFormat.class.getName()
                            + "," + PlainTextCryptFormat.class.getName());

                    AppConfigurationEntry appConf = new AppConfigurationEntry(EnhancedLdapLoginModule.class.getName(), LoginModuleControlFlag.REQUIRED, options);
                    return new AppConfigurationEntry[]{ appConf };
                } else {
                    return null;
                }
            }
        });
    }

}
