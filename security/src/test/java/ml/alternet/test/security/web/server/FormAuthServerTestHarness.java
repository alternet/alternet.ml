package ml.alternet.test.security.web.server;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import jodd.methref.Methref;

import org.assertj.core.api.Assertions;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.testng.annotations.Test;

public abstract class FormAuthServerTestHarness<T> extends ServerTestHarness<T> {

    /**
     * MUST BE INITIALIZED ON SUBCLASS (not in the constructor) WITH :
     * <pre>
     *    static {
     *        name = Methref.on(TheSubClass.class);
     *    }
     * </pre>
     *
     * Allow to get method name (used as key in the "serverTests" map)
     */
    protected static Methref<? extends FormAuthServerTestHarness<?>> name;

    // a RESTful app

    @Path("/")
    public static class ExampleRequest {

        @Path("/test.html")
        @GET
        @Produces(MediaType.APPLICATION_XML)
        public Data example(
                @Context HttpServletRequest req) throws UnsupportedEncodingException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

            // this attribute MUST have been set in the server settings
            FormAuthServerTestHarness<?> that = (FormAuthServerTestHarness<?>) req.getServletContext().getAttribute(FormAuthServerTestHarness.class.getName());

            // Password not available here since this is a new request (a redirect to the login form
            // was sent to the client, and after the client was redirected to this page);
            // there is only a session internally, the session store the pwd, but when leaving the stack,
            // the pwd has been invalidate()

            // collect some tests before returning

            Principal user = req.getUserPrincipal();
            name.to().user_ShouldBe_Authentified();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(user)
                .as("REST server : user should be authentified")
                .isNotNull()
            );

            String userName = user.getName();
            name.to().userName_ShouldBe_Defined();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(userName)
                .as("REST server : user should be authentified")
                .isEqualTo("who")
            );

            boolean uira = req.isUserInRole("admin");
            name.to().user_ShouldHave_AdminRole();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(uira)
                .as("REST server : user should have \"admin\" role")
                .isTrue()
            );

            boolean uirc = req.isUserInRole("customer");
            name.to().user_ShouldNotHave_CustomerRole();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(uirc)
                .as("REST server : user should not have \"customer\" role")
                .isFalse()
            );

            @SuppressWarnings("rawtypes")
            String credentials = ((FormAuthServerTestHarness) that).checkSession(req);
                name.to().passwordSession_ShouldBe_FilledWithStarsOrUnavailable();
                if (credentials != null) {
                that.serverTests.put(name.ref(),
                    () -> Assertions.assertThat(credentials)
                    .as("REST server : password header should be filled with '*'")
                    .matches("^\\*+$") // "*****"
                );
            }

            Data data = new Data(userName, credentials);

            return data;
        }
    }

    /**
     * Check the internal session / &lt;T&gt; dependant.
     *
     * @param req The request
     * @return The credential that was put in the session.
     */
    public abstract String checkSession(HttpServletRequest req);

    public String contextPath = "/test";

    // client stuff

    @Test(priority=1)
    public void authPassword_ShouldBe_capturedByWebServer() throws Exception {
        ClientConfig conf = new ClientConfig();
        // don't let the client go to the login page itself
        conf.property(ClientProperties.FOLLOW_REDIRECTS, "false");
        Client client = ClientBuilder.newClient(conf);

        String uri = "http://localhost:" + port + contextPath + "/protected/test.html";
        Response response = client.target(uri)
            .request()
            .get();

        Assertions.assertThat(response.getStatus())
            .as("REST client : protected resource should be redirected to login form")
            .isIn(  // redirect to login page (HTTP 300 or 302)
                    HttpServletResponse.SC_SEE_OTHER, HttpServletResponse.SC_MOVED_TEMPORARILY,
                    // ok to serve directly the login page
                    HttpServletResponse.SC_OK);

        MultivaluedMap<String, Object> headers = response.getHeaders();
        String loginUri = "http://localhost:" + port + contextPath + "/login.html";
        if (response.getStatus() == HttpServletResponse.SC_SEE_OTHER
                || response.getStatus() ==HttpServletResponse.SC_MOVED_TEMPORARILY) {
            // check redirect to login page
            Assertions.assertThat(headers.getFirst("Location").toString()).startsWith(loginUri);
        }

        // NOTE about cookie session :
        //   Tomcat has a parameter setChangeSessionIdOnAuthentication(true) which indicates to renew the sessionID
        //   Jetty renew it too
        // but they don't achieve the renewal at the same time :
        //   Tomcat change the session BEFORE going to the login page
        //   Jetty change the session AFTER the user validate the login form
        // therefore, we need to track the 2 cases below, that is to say
        // handle the session token with care

        Map<String,NewCookie> newCookies = response.getCookies();
        NewCookie session = newCookies.get("JSESSIONID");
        Assertions.assertThat(session).isNotNull();

        // fill form in "login.html"
        Form form = new Form();
        form.param("j_username", userName);
        form.param("j_password", unsafePwd);
        String doLoginUri = "http://localhost:" + port + contextPath + "/j_security_check";

        Invocation.Builder builder = client.target(doLoginUri).request();
        builder.cookie(session);
        response = builder.post(Entity.form(form));

        Assertions.assertThat(response.getStatus())
            .as("REST client : login form should redirect to original URL")
            .isEqualTo(303);

        headers = response.getHeaders();
        // change in Tomcat 8.0.30 : As per RFC7231 (HTTP/1.1), allow HTTP/1.1 and later redirects to use relative URIs
        Assertions.assertThat(uri).endsWith(headers.getFirst("Location").toString());

        newCookies = response.getCookies();
        if (newCookies.get("JSESSIONID") != null) {
            // only set a new cookie if it has been changed
            session = newCookies.get("JSESSIONID");
        }
        response = client.target(uri)
                .request()
                .cookie(session)
                .get();

        Assertions.assertThat(response.getStatus())
            .as("REST client : protected resource should be read")
            .isEqualTo(200);

        Data data = response.readEntity(Data.class);

        if (data.pwd != null) {
            Assertions.assertThat(data.pwd)
                .as("REST client : password header should be filled with '*'")
                .matches("^\\*+$"); // "*****"
        }
        Assertions.assertThat(data.user)
            .as("REST client : user should be the same")
            .isEqualTo(userName);
    }

    @Test(priority=10)
    public void user_ShouldBe_Authentified() {
        name.to().user_ShouldBe_Authentified();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void userName_ShouldBe_Defined() {
        name.to().userName_ShouldBe_Defined();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void user_ShouldHave_AdminRole() {
        name.to().user_ShouldHave_AdminRole();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void user_ShouldNotHave_CustomerRole() {
        name.to().user_ShouldNotHave_CustomerRole();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void sessionAuthentication_ShouldBe_Define() {
        name.to().sessionAuthentication_ShouldBe_Define();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void passwordSession_ShouldBe_FilledWithStarsOrUnavailable() {
        name.to().passwordSession_ShouldBe_FilledWithStarsOrUnavailable();
        this.serverTests.getOrDefault(name.ref(), () -> {}).run();
    }

}
