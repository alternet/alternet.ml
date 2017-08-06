package ml.alternet.test.security.web;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import javax.xml.bind.DatatypeConverter;

import ml.alternet.security.Password;
import ml.alternet.security.PasswordManagerFactory;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.web.server.BasicAuthorizationBuffer;
import ml.alternet.security.web.server.BasicAuthorizationBuffer.Scope;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class BasicAuthBufferTest {

    String unsafePwd = "da_AcTu@| P@zzm0R|)";
    String cred = "john:" + unsafePwd;

    String b64Cred = DatatypeConverter.printBase64Binary(cred.getBytes());
    String basic = "Basic " + b64Cred;
    String auth = "Authorization: " + basic;

    String firsHttpHeaders =
              "Host: localhost:8675\r\n"
            + "Connection: keep-alive\r\n"
            + "Cache-Control: max-age=0\r\n";

    String lastHttpHeaders =
            "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n"
          + "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.111 Safari/537.36\r\n"
          + "Accept-Encoding: gzip, deflate, sdch\r\n"
          + "Accept-Language: fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4";

    String httpHeaders =
              firsHttpHeaders
            + auth + "\r\n"
            + lastHttpHeaders;

    @Test
    public void password_ShouldBe_extractedFromHttpHeaders() {
        BasicAuthorizationBuffer authBuf = createBuffer(httpHeaders, Scope.Headers);
        check(authBuf);
    }

    @Test
    public void password_ShouldBe_extractedFromHttpHeadersEndingWithCRLF() {
        BasicAuthorizationBuffer authBuf = createBuffer(httpHeaders + "\r\n", Scope.Headers);
        check(authBuf);
    }

    @Test
    public void password_ShouldBe_extractedFromSingleHttpHeader() {
        BasicAuthorizationBuffer authBuf = createBuffer(auth, Scope.AuthorizationHeader);
        check(authBuf);
    }

    @Test
    public void password_ShouldBe_extractedFromBasicCredential() {
        BasicAuthorizationBuffer authBuf = createBuffer(basic, Scope.AuthorizationHeaderValue);
        check(authBuf);
    }

    @Test
    public void credentials_ShouldNotBe_foundInOtherHttpHeader() {
        BasicAuthorizationBuffer authBuf = createBuffer("Cache-Control: max-age=0", Scope.AuthorizationHeader);
        Assertions.assertThat(authBuf.findCredentialsBoundaries()).isFalse();
    }

    @Test
    public void credentials_ShouldNotBe_foundInHttpHeadersWithoutAuthorizationHeader() {
        String httpHeadersWithoutAuthorizationHeader = firsHttpHeaders + lastHttpHeaders;
        BasicAuthorizationBuffer authBuf = createBuffer(httpHeadersWithoutAuthorizationHeader, Scope.Headers);
        Assertions.assertThat(authBuf.findCredentialsBoundaries()).isFalse();
    }

    @Test
    public void credentials_ShouldBe_unreachable() {
        // because double CRLF ends the HTTP headers
        String httpHeadersWithoutAuthorizationHeader = firsHttpHeaders + "\r\n"
                    + auth + "\r\n" + lastHttpHeaders; // unreachable part
        BasicAuthorizationBuffer authBuf = createBuffer(httpHeadersWithoutAuthorizationHeader, Scope.Headers);
        Assertions.assertThat(authBuf.findCredentialsBoundaries()).isFalse();
    }

    public BasicAuthorizationBuffer createBuffer(String string, Scope scope) {
        byte[] bytes = string.getBytes(Charset.forName("ISO-8859-1"));
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        BasicAuthorizationBuffer authBuf = new BasicAuthorizationBuffer(scope, buf.position(), buf.limit()) {
            @Override
            public void set(int i, byte b) {
                buf.put(i, b);
            }
            @Override
            public byte get(int i) {
                return buf.get(i);
            }
            @Override
            public void debug(String msg) { }
        };
        return authBuf;
    }

    public void check(BasicAuthorizationBuffer authBuf) {
        Assertions.assertThat(authBuf.findCredentialsBoundaries()).isTrue();
        Credentials credentials = authBuf.replace(PasswordManagerFactory.getStrongPasswordManager());
        char[] clearPwd;
        try (Password.Clear clear = credentials.getPassword().getClearCopy()) {
            clearPwd = clear.get();
            Assertions.assertThat(clearPwd).isEqualTo(unsafePwd.toCharArray());
        }

        // check if the pwd has been replaced with '*' in the buffer
        credentials = authBuf.replace(PasswordManagerFactory.getStrongPasswordManager());
        try (Password.Clear clear = credentials.getPassword().getClearCopy()) {
            clearPwd = clear.get();
            Assertions.assertThat(clearPwd.length).isEqualTo(unsafePwd.length());
            Assertions.assertThat(clearPwd).containsOnly('*');
        }
    }

}
