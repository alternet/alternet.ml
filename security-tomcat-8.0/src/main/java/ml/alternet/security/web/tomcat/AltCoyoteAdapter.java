package ml.alternet.security.web.tomcat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.CoyoteAdapter;
import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;

import ml.alternet.security.PasswordManager;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.web.Passwords;
import ml.alternet.security.web.server.AuthenticationMethod;
import ml.alternet.security.web.server.BasicAuthorizationBuffer;
import ml.alternet.security.web.server.CaptureContext;
import ml.alternet.security.web.server.DebugLevel;
import ml.alternet.security.web.server.FormFieldConfiguration;
import ml.alternet.security.web.server.FormLimit;
import ml.alternet.security.web.server.FormReader;
import ml.alternet.security.web.server.PasswordFieldMatcher;

/**
 * A connector adapter that process BASIC authentication
 * and FORM capture of passwords.
 *
 * @author Philippe Poulard
 */
public class AltCoyoteAdapter extends CoyoteAdapter {

    FormLimit formLimit;
    DebugLevel debugLevel;
    PasswordManager pm;

    /**
     * Create a new connector adapter.
     *
     * @param connector The connector.
     * @param debugLevel The debug level.
     * @param pm The password manager.
     */
    public AltCoyoteAdapter(Connector connector, DebugLevel debugLevel, PasswordManager pm) {
        super(connector);
        this.debugLevel = debugLevel;
        this.pm = pm;
        this.formLimit = new FormLimit() {
            @Override
            public int getMaxFormKeys() {
                return connector.getMaxParameterCount();
            }
            @Override
            public int getMaxFormContentSize() {
                return connector.getMaxPostSize();
            }
        };
    }

    @Override
    protected boolean postParseRequest(Request req, org.apache.catalina.connector.Request request,
            Response res, org.apache.catalina.connector.Response response) throws IOException,
            ServletException {
        try {
            // first, let set the context to the request
            // (because right now, it is unknown)
            return super.postParseRequest(req, request, res, response);
        } finally {
            // now we do have a context
            PasswordFieldMatcher pfm = new PasswordFieldMatcher() {

                ServletContext scontext = request.getServletContext();

                @Override
                public Optional<List<String>> matches(HttpServletRequest request) {
                    return FormFieldConfiguration.matches(scontext, request);
                }

                @Override
                public AuthenticationMethod getAuthenticationMethod(ServletRequest request) {
                    return AuthenticationMethod.extract(scontext);
                }

            };

            // process BASIC auth
            if (pfm.getAuthenticationMethod(null) == AuthenticationMethod.Basic) {
                MessageBytes auth = req.getMimeHeaders().getValue("authorization");
                if (auth != null) {
                    // "Basic d2hvOmRhX0FjVHVAfCBQQHp6bTBSfCk="
                    ByteChunk byteChunk = auth.getByteChunk();
                    byte[] buffer = byteChunk.getBuffer();
                    // capture pwd
                    BasicAuthorizationBuffer authBuf = new BasicAuthorizationBuffer(
                            BasicAuthorizationBuffer.Scope.AuthorizationHeaderValue,
                            byteChunk.getStart(),
                            byteChunk.getEnd())
                    {
                        @Override
                        public void set(int i, byte b) {
                            buffer[i] = b;
                        }
                        @Override
                        public byte get(int i) {
                            return buffer[i];
                        }
                        @Override
                        public void debug(String msg) {
                            if (debugLevel.isAllowingUnsercureTrace()) {
                                AltProtocolHandler.LOG.debug(msg + " :\n" + new String(buffer, Charset.forName("ISO-8859-1")));
                            }
                        }
                    };
                    if (authBuf.findCredentialsBoundaries()) {
                        Credentials credentials = authBuf.replace(pm);
                        req.setAttribute(Passwords.BASIC_AUTH_ATTRIBUTE_KEY, credentials);
                    }
                }
            }

            // process FORM capture
            pfm.matches(request).ifPresent(fields -> {
                CaptureContext<ByteBuffer> cc = new CaptureContext<ByteBuffer>(fields);
                InputBuffer in = req.getInputBuffer();
                req.setInputBuffer(new InputBuffer() {
                    FormReader fr = new FormReader(formLimit, pm) {
                        @Override
                        public int readItem(byte[] buf, int i) {
                            byte b = cc.writableInputBuffer.get();
                            if (replace && ! cc.writableInputBuffer.isReadOnly()) {
                                // the intercepted password will be stored
                                // encrypted in the capture context

                                // we can supply '*' to the next stage, from which
                                // String objects will be built
                                buf[i] = '*';
                                // don't let the input buffer with the clear password
                                cc.writableInputBuffer.put(cc.writableInputBuffer.position() - 1, (byte) '*');
                            } else {
                                buf[i] = b;
                            }
                            return b & 0xFF;
                        }
                        @Override
                        public void log(Exception exception) {
                            AltProtocolHandler.LOG.debug(exception.toString(), exception);
                        }
                        @Override
                        public CaptureContext<ByteBuffer> getCurrentCaptureContext() {
                            return cc;
                        }
                    };
                    @Override
                    public int doRead(ByteChunk chunk, Request request) throws IOException {
                        try {
                            return in.doRead(chunk, request);
                        } finally {
                            int size = chunk.getLength();
                            cc.writableInputBuffer = ByteBuffer.wrap(chunk.getBytes(), chunk.getStart(), chunk.getLength());
                            fr.get(size, chunk.getBytes(), chunk.getStart(), chunk.getLength());

                            Passwords pwd = cc.asPasswords();
                            request.setAttribute(Passwords.ATTRIBUTE_KEY, pwd);
                        }
                    }
                });
            });
        }
    }

    @Override
    public void service(Request coyoteRequest, Response res) throws Exception {
        try {
            super.service(coyoteRequest, res);
        } finally { // #From : ml.alternet.security.web.Passwords looks like a CaptureContext
            Credentials credentials = (Credentials) coyoteRequest.getAttributes().get(Passwords.BASIC_AUTH_ATTRIBUTE_KEY);
            if (credentials != null) {
                credentials.destroy();
                coyoteRequest.getAttributes().remove(Passwords.BASIC_AUTH_ATTRIBUTE_KEY);
            }
            // do not try to destroy here the CaptureContext since :
            //     -service() will be called again in the pipeline
            //     -the capture context can be GC reclaimed once the process do end
        }
    }

}
