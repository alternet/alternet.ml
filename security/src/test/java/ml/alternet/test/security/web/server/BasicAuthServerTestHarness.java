package ml.alternet.test.security.web.server;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import jodd.methref.Methref;
import ml.alternet.security.Password;
import ml.alternet.security.PasswordState;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.web.Passwords;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public abstract class BasicAuthServerTestHarness<T> extends ServerTestHarness<T> {

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
    protected static Methref<? extends BasicAuthServerTestHarness<?>> name;

    // a RESTful app

    // https://github.com/jersey/jersey/tree/master/tests/integration/jersey-1107
    @Path("/")
    public static class ExampleRequest {

        @Path("{example}")
        @GET
        @Produces(MediaType.APPLICATION_XML)
        public Data example(@PathParam("example") String example,
                @Context HttpHeaders httpHeaders,
                @Context HttpServletRequest req) throws UnsupportedEncodingException {

            // this attribute MUST have been set in the server settings
            BasicAuthServerTestHarness<?> that = (BasicAuthServerTestHarness<?>) req.getServletContext().getAttribute(BasicAuthServerTestHarness.class.getName());

            List<String> authHeaders = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION);

            // collect some tests before returning

            name.to().HTTPAuthenticationMissing();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(authHeaders.size())
                .isNotNull()
                .as("REST server : HTTP Authentication missing")
            );

            String authHeader = authHeaders.get(0);

            name.to().HTTPBasicAuthenticationMissing();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(authHeader.startsWith("Basic "))
                .isNotNull()
                .as("REST server : HTTP Basic Authentication missing")
            );

            String[] cred = new String(
                DatatypeConverter.parseBase64Binary(
                    authHeader.substring("Basic ".length())
                ), "ISO-8859-1").split(":");

            // the result of the request to return
            Data data = new Data(cred[0], cred[1]);

            name.to().credential_Should_containTheRightUserName();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(cred[0])
                .isEqualTo("who")
                .as("REST server : credential should contain the right user name")
            );

            name.to().passwordHeader_ShouldBe_FilledWithStars();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(cred[1])
                .as("REST server : password header should be filled with '*'")
                .matches("^\\*+$") // "*****"
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

            Credentials credentials = (Credentials) req.getAttribute(Passwords.BASIC_AUTH_ATTRIBUTE_KEY);

            name.to().request_Should_SupplyPassword();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(credentials)
                .as("REST server : request should supply password")
                .isNotNull()
            );

            Password pwd = credentials.getPassword();
            PasswordState state  = pwd.state();
            name.to().request_Should_SupplyValidPassword();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(state)
                .as("REST server : request should supply valid password")
                .isSameAs(PasswordState.Valid)
            );

            char[] clearPwd = pwd.getClearCopy().get();
            name.to().password_Should_HaveTheRigthValue();
            that.serverTests.put(name.ref(),
                () -> Assertions.assertThat(clearPwd)
                .as("REST server : password should have the rigth value")
                .isEqualTo(that.unsafePwd.toCharArray())
            );

            return data;
        }
    }

    public String contextPath = "/test";

    // client stuff

    @Test(priority=1)
    public void authPassword_ShouldBe_capturedByServer() throws Exception {
        String uri = "http://localhost:" + port + contextPath + "/Test";
        Client client = ClientBuilder.newClient();
        String unsafeCred = userName + ":" + unsafePwd;
        Response response = client.target(uri)
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Basic " +
                DatatypeConverter.printBase64Binary(unsafeCred.getBytes("UTF-8")))
            .get();
        Data data = response.readEntity(Data.class);

        Assertions.assertThat(data.pwd)
            .as("REST client : password header should be filled with '*'")
            .matches("^\\*+$"); // "*****"
        Assertions.assertThat(data.user)
            .as("REST client : user should be the same")
            .isEqualTo("who");

        Map<String,NewCookie> newCookies = response.getCookies();
        NewCookie session = newCookies.get("JSESSIONID");
        Assertions.assertThat(session).isNotNull();
    }

    @Test(priority=10)
    public void HTTPAuthenticationMissing() {
        name.to().HTTPAuthenticationMissing();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void HTTPBasicAuthenticationMissing() {
        name.to().HTTPBasicAuthenticationMissing();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void credential_Should_containTheRightUserName() {
        name.to().credential_Should_containTheRightUserName();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void passwordHeader_ShouldBe_FilledWithStars() {
        name.to().passwordHeader_ShouldBe_FilledWithStars();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void request_Should_SupplyPassword() {
        name.to().request_Should_SupplyPassword();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void request_Should_SupplyValidPassword() {
        name.to().request_Should_SupplyValidPassword();
        this.serverTests.get(name.ref()).run();
    }

    @Test(priority=10)
    public void password_Should_HaveTheRigthValue() {
        name.to().password_Should_HaveTheRigthValue();
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

}
