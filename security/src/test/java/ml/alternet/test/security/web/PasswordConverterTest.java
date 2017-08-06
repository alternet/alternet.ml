package ml.alternet.test.security.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import ml.alternet.security.EmptyPassword;
import ml.alternet.security.Password;
import ml.alternet.security.PasswordManagerFactory;
import ml.alternet.security.PasswordState;
import ml.alternet.security.web.PasswordConverterProvider;
import ml.alternet.security.web.PasswordParam;
import ml.alternet.security.web.Passwords;

import org.assertj.core.api.Assertions;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTestNg;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.testng.annotations.Test;

/**
 * Test the password converter provider inside a RESTful application.
 *
 * The converter provider works with injection of resources and with
 * a filter that mimic the work of Tomcat or Jetty : it get the
 * parameter and simply create the expected password object.
 *
 * @see PasswordConverterProvider
 *
 * @author Philippe Poulard
 */
public class PasswordConverterTest extends JerseyTestNg.ContainerPerClassTest {

    // the password field name in the form
    static final String PASSWORD_FIELD = "password";

    // the actual password
    static final String PASSWORD = "Da_actu@L pazzw0r|) !";
    // in this scenario, the user wrongly typed again its password
    static final String PASSWORD_CHECK = "Da actuaL password !";

    static PasswordConverterTest that;

    public PasswordConverterTest() {
        that = this;
    }

    // the thing that we want to test : getting the form param
    // as a PasswordParam instance
    @Path("sendPassword")
    public static class PasswordResource {

        @POST
        @Path("post")
        public String processPassword(@FormParam(PASSWORD_FIELD) PasswordParam pwd) {
            // pwd is the converted value

            that.checkLater(() -> Assertions.assertThat(pwd)
                .as("No passwords found").isNotNull());

            final String clearPwd = new String(pwd.getClearCopy().get());
            that.checkLater(() -> Assertions.assertThat(clearPwd)
                .as("1st password \"%s\" has not the value expected", clearPwd).isEqualTo(PASSWORD));

            final boolean hasNext = pwd.hasNext(); // need to capture the value
            that.checkLater(() -> Assertions.assertThat(hasNext)
                .as("2nd password \"%s\" should NOT be the last", clearPwd).isTrue());

            final PasswordParam pwdCheck = pwd.next();
            String clearPwdCheck = new String(pwdCheck.getClearCopy().get());
            that.checkLater(() -> Assertions.assertThat(clearPwdCheck)
                .as("2nd password \"%s\" has not the value expected", clearPwdCheck).isEqualTo(PASSWORD_CHECK));

            that.checkLater(() -> Assertions.assertThat(pwdCheck.hasNext())
                .as("2nd password \"%s\" should be the last", clearPwdCheck).isFalse());

            return clearPwd;
        }

        @POST
        @Path("postNoForm")
        public String processNoPassword(@FormParam(PASSWORD_FIELD) PasswordParam pwd) {
            // pwd is not sent in the form

            that.checkLater(() -> Assertions.assertThat(pwd)
                .as("Unexpected password found").isNull());

            return "";
        }

        @GET
        @Path("get")
        public String wrongProcessPassword(@QueryParam(PASSWORD_FIELD) PasswordParam pwd) {
            // pwd MUST NOT be sent by GET, and therefore is not handled by the converter

            that.checkLater(() -> Assertions.assertThat(pwd.state())
                .as("@QueryParam should not capture the password").isEqualTo(PasswordState.Empty));
            return new String(pwd.getClearCopy().get());
        }
    }

    // this filter mimic the work of the web container
    @Provider
    private static class WebContainerFilter implements ContainerRequestFilter {

        @Context
        HttpServletRequest req;

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            final List<Password> pwdList = new ArrayList<Password>();
            // IMPORTANT : do NOT read HttpServletRequest.getParameter(name) since it will consume the form
            // this why it is hardcoded here
            pwdList.add(PasswordManagerFactory.getWeakPasswordManager()
                    .newPassword(PASSWORD.toCharArray()));
            pwdList.add(PasswordManagerFactory.getWeakPasswordManager()
                    .newPassword(PASSWORD_CHECK.toCharArray()));
            Passwords pwd = new Passwords() {
                @Override
                protected PasswordParam getPasswords(String name) {
                    if (PASSWORD_FIELD.equals(name)) {
                        return new PasswordParam(pwdList.iterator());
                    } else {
                        return new PasswordParam(EmptyPassword.SINGLETON);
                    }
                }
            };
            // NOTE : this is the job of the container to set that attribute
            // in the request, but this test doesn't intend to check that ;
            // this test just check the conversion of a password to the
            // PasswordParam class
            req.setAttribute(Passwords.ATTRIBUTE_KEY, pwd);
            // NOTE : setting this attribute by Tomcat / Jetty / and others
            //      are performed elsewhere on a specific subproject
        }

    }

    /**
     * Send a "registration form" where the user is invited to fill a form
     * that contains a password and a password check.
     *
     * @throws Exception
     */
    @Test
    public void passwordsSentByPOST_ShouldBe_convertedToPasswordParamClass() throws Exception {
        Form form = new Form();
        form.param(PASSWORD_FIELD, PASSWORD);
        form.param(PASSWORD_FIELD, PASSWORD_CHECK);
        final Response response = target("sendPassword/post").request().post(Entity.form(form));

        checkNow(); // check server assertions first

        Assertions.assertThat(response.getStatus()).isEqualTo(200);
        Assertions.assertThat(response.readEntity(String.class)).isEqualTo(PASSWORD);
    }

    /**
     * Send an empty POST request.
     *
     * @throws Exception
     */
    @Test
    public void noPasswordSentByPOST_ShouldBe_empty() throws Exception {
        final Response response = target("sendPassword/postNoForm").request()
            .post(Entity.entity(null, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        checkNow(); // check server assertions first

        Assertions.assertThat(response.getStatus()).isEqualTo(200);
        Assertions.assertThat(response.readEntity(String.class)).isEmpty();
    }

    /**
     * Send a "registration form" where the user is invited to fill a form
     * that contains a password and a password check.
     *
     * @throws Exception
     */
    @Test
    public void passwordsSentByGET_ShouldNotBe_converted() throws Exception {
        final Response response = target("sendPassword/get")
                .queryParam(PASSWORD_FIELD, PASSWORD, PASSWORD_CHECK).request().get();

        checkNow(); // run server tests first

        Assertions.assertThat(response.getStatus()).isEqualTo(200);
        Assertions.assertThat(response.readEntity(String.class)).isEmpty();
    }

    // tests registered inside the server but to run outside the server
    ConcurrentLinkedQueue<Runnable> tests = new ConcurrentLinkedQueue<Runnable>();
    // ideally, the communication could be made with Arquillian

    /**
     * Server side calls
     *
     * @param test A deferred (closure) test, have to contain all the
     *          required data to check
     */
    public void checkLater(Runnable test) {
        this.tests.add(test);
    }

    /**
     * Server tests to run client side
     */
    public void checkNow() {
        for (Runnable r = this.tests.poll() ; r != null ; r = this.tests.poll() ) {
            r.run();
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig app = new ResourceConfig();
        return app;
    }

    // won't disable the web app
    // (because we are testing the failure of capturing the pwd with @QueryParam)
    static class TolerantPasswordConverterProvider extends PasswordConverterProvider {
        @Override
        protected void processQueryParam(QueryParam passwordFieldAnnotation) throws SecurityException {}
    }

    static String classes =
        PasswordResource.class.getName() + " " +
        WebContainerFilter.class.getName() + " " +
        TolerantPasswordConverterProvider.class.getName();

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.builder(configure())
              .initParam(ServerProperties.PROVIDER_CLASSNAMES, classes)
              .build();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        // we need a real Web server in order to allow injection of
        // server-side objects (HttpServletRequest)
        return new GrizzlyWebTestContainerFactory();
    }

}
