package ml.alternet.security.web.jetty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpParser;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.HttpInputOverHTTP;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

import ml.alternet.security.PasswordManager;
import ml.alternet.security.PasswordManagerFactory;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.web.Passwords;
import ml.alternet.security.web.server.AuthenticationMethod;
import ml.alternet.security.web.server.BasicAuthorizationBuffer;
import ml.alternet.security.web.server.CaptureContext;
import ml.alternet.security.web.server.DebugLevel;
import ml.alternet.security.web.server.FormLimit;
import ml.alternet.security.web.server.FormReader;
import ml.alternet.security.web.server.PasswordFieldMatcher;

/**
 * Enhance a Jetty server for safe-processing of passwords.
 *
 * <p>A Jetty enhanced HTTP connection factory allow to capture the
 * passwords that would be sent by the client BEFORE they are
 * processed as Strings ; after capturing, the raw data are replaced
 * with '*'. The captured passwords are wrapped in an encrypted form
 * and are available to the user by the classes of the package
 * <tt>ml.alternet.security.web</tt>.</p>
 *
 * @author Philippe Poulard
 */
public class AltHttpConnectionFactory extends HttpConnectionFactory implements DebugLevel.Debuggable {

    // configuration :
    // -a set of paths that received forms with passwords are configured by the user
    //
    // strategy :
    // -the connection is called on "onFillable()"
    // -we first extract and parse the URI from which we get the target path
    // -if the request URI matches the paths of the configuration
    //     we are setting a "capture context" and the process can go on
    // -after handling, the context is removed

    /**
     * Handle buffers in order to capture passwords in forms
     *
     * @author Philippe Poulard
     */
    public abstract class AltdHttpInput extends HttpInputOverHTTP {

        // a writable clone of the input buffer
        // if a password is reading, its content is filled with '*'
        ByteBuffer wBuf;
        // used by the read() method
        byte[] buffer;
        int pos = 0;

        FormReader fr = new FormReader(formLimit, pm) {
            @Override
            public int readItem(byte[] buf, int i) {
                byte b = wBuf.get();
                if (replace && ! wBuf.isReadOnly()) {
                    // the intercepted password will be stored encrypted elsewhere

                    // we can supply '*' to the next stage, from which
                    // String objects will be built
                    buf[i] = '*';
                    // don't let the input buffer with the clear password
                    wBuf.put(wBuf.position() - 1, (byte) '*');
                } else {
                    buf[i] = b;
                }
                return b & 0xFF;
            }

            @Override
            public CaptureContext<ByteBuffer> getCurrentCaptureContext() {
                return AltdHttpInput.this.getCaptureContext();
            }

            @Override
            public void log(Exception exception) {
                LOG.warn(exception.toString());
                LOG.debug(exception);
            }
        };

        /**
         * Create an instance from a connection.
         *
         * @param httpConnection The connection.
         */
        public AltdHttpInput(HttpConnection httpConnection) {
            super(httpConnection);
        }

        /**
         * Return the capture context.
         *
         * @return The capture context.
         */
        public abstract CaptureContext<ByteBuffer> getCaptureContext();

        /**
         * Set the capture context.
         *
         * @param cc The capture context.
         */
        public abstract void setCaptureContext(CaptureContext<ByteBuffer> cc);

        @Override
        public void recycle() {
            super.recycle();
            fr.reset();
            wBuf = null;
            setCaptureContext(null);
        }

        @Override
        public void content(ByteBuffer item) {
            CaptureContext<ByteBuffer> cc = getCaptureContext();
            // "item" is a READONLY clone of "writableBuffer"
            // we are just creating a WRITABLE clone of it
            if (cc != null) {
                ByteBuffer writableBuffer = cc.writableInputBuffer;
//                wBuf = ByteBuffer.wrap(writableBuffer.array(), writableBuffer.position(), writableBuffer.remaining());
                wBuf = ByteBuffer.wrap(writableBuffer.array(), item.position(), item.remaining());
                // wBuf is now a WRITABLE clone of "writableBuffer"
            } else {
                wBuf = null;
            }
            super.content(wBuf);
        }

        @Override
        public int read() throws IOException {
            // we must buffer the input, in order to cause field capture
            // otherwise get() will process char by char and nothing would be captured
            // NOTE : the PasswordConverterProvider cause Jersey to use its own buffer
            //        and this method is unused
            if (buffer == null) {
                try {
                    int read;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    final byte[] data = new byte[AltHttpConnectionFactory.BUFFER_SIZE];
                    while ((read = read(data)) != -1) {
                        out.write(data, 0, read);
                    }
                    buffer = out.toByteArray();
                } finally {
                    close();
                }
            }
            if (pos == buffer.length) {
                pos = 0;
                buffer = null;
                return -1;
            } else {
                return buffer[pos++] & 0xFF;
            }
        }

        @Override
        protected int get(ByteBuffer item, byte[] buf, int offset, int length) {
            if (wBuf == null) {
                return super.get(item, buf, offset, length);
            } else {
                int size = Math.min(item.remaining(), length);
                return fr.get(size, buf, offset, length);
            }
        }
    }

    static final Logger LOG = Log.getLogger(AltHttpConnectionFactory.class);

    /**
     * The size of the buffer.
     */
    public static final int BUFFER_SIZE = 8196;

    PasswordManager pm;
    PasswordFieldMatcher pfm;
    FormLimit formLimit;
    DebugLevel debugLevel = new DebugLevel();

    /**
     * Allow to enhance a Jetty server.
     *
     * @param wac The web application to enhance.
     *
     * @see ml.alternet.security.impl.StrongPasswordManager
     */
    public AltHttpConnectionFactory(@Name(value="webAppContext") WebAppContext wac) {
        this(PasswordManagerFactory.getStrongPasswordManager(), wac);
    }

    /**
     * Allow to enhance a Jetty server.
     *
     * @param pfm A password field matcher.
     * @param formLimit Form size limits.
     *
     * @see ml.alternet.security.impl.StrongPasswordManager
     */
    public AltHttpConnectionFactory(PasswordFieldMatcher pfm, FormLimit formLimit) {
        this(PasswordManagerFactory.getStrongPasswordManager(), pfm, formLimit);
    }

    /**
     * Allow to enhance a Jetty server.
     *
     * @param pm A specific password manager.
     * @param wac The web application to enhance.
     *
     * @see WebappPasswordFieldMatcher
     */
    public AltHttpConnectionFactory(PasswordManager pm, WebAppContext wac) {
        this.pm = pm;
        WebappPasswordFieldMatcher wpfm = new WebappPasswordFieldMatcher(wac);
        this.pfm = wpfm;
        this.formLimit = wpfm;
    }

    /**
     * Allow to enhance a Jetty server.
     *
     * @param pm A specific password manager.
     * @param server The server to enhance.
     *
     * @see WebappPasswordFieldMatcher
     */
    public AltHttpConnectionFactory(PasswordManager pm, Server server) {
        this.pm = pm;
        WebappPasswordFieldMatcher wpfm = new WebappPasswordFieldMatcher(server);
        this.pfm = wpfm;
        this.formLimit = wpfm;
    }

    /**
     * Allow to enhance a Jetty server.
     *
     * @param server The Jetty server to enhance.
     *
     * @see ml.alternet.security.impl.StrongPasswordManager
     */
    public void setServer(Server server) {
        WebappPasswordFieldMatcher wpfm = new WebappPasswordFieldMatcher(server);
        this.pfm = wpfm;
        this.formLimit = wpfm;
    }

    /**
     * Allow to enhance a Jetty server.
     *
     * @param pm A specific password manager.
     * @param pfm A password field matcher.
     * @param formLimit Form size limits.
     */
    public AltHttpConnectionFactory(PasswordManager pm, PasswordFieldMatcher pfm, FormLimit formLimit) {
        this.pm = pm;
        this.pfm = pfm;
        this.formLimit = formLimit;
    }

    /**
     * Allow to enhance a Jetty server.
     *
     * @param server The Jetty server to enhance.
     *
     * @see ml.alternet.security.impl.StrongPasswordManager
     */
    public AltHttpConnectionFactory(@Name(value="server") Server server) {
        this(PasswordManagerFactory.getStrongPasswordManager(), server);
    }

    /**
     * Allow to enhance a Jetty server.
     *
     * @see ml.alternet.security.impl.StrongPasswordManager
     */
    public AltHttpConnectionFactory() {
        this.pm = PasswordManagerFactory.getStrongPasswordManager();
    }

    @Override
    public DebugLevel getDebugLevel() {
        return this.debugLevel;
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {
        return configure(new HttpConnection(getHttpConfiguration(), connector, endPoint) {

            CaptureContext<ByteBuffer> captureContext;

            @Override
            protected HttpInput<ByteBuffer> newHttpInput() {
                return new AltdHttpInput(this) {
                    @Override
                    public CaptureContext<ByteBuffer> getCaptureContext() {
                        return captureContext;
                    }
                    @Override
                    public void setCaptureContext(CaptureContext<ByteBuffer> cc) {
                        captureContext = cc;
                    }
                };
            }

            @Override
            protected HttpParser newHttpParser() {
                return new HttpParser(newRequestHandler(), getHttpConfiguration().getRequestHeaderSize()) {
                    @Override
                    protected boolean parseContent(ByteBuffer buffer) {
                        CaptureContext<ByteBuffer> cc = captureContext;
                        if (cc != null) {
                            // we need to store this buffer here because
                            // in one of the next stage, it is passed as a readonly buffer
                            // (I don't know why)
                            cc.writableInputBuffer = buffer;
                            if (AltHttpConnectionFactory.this.debugLevel.isAllowingUnsercureTrace()) {
                                LOG.debug("Parsing HTTP request :\n{}", new String(buffer.array()));
                            }
                        }
                        return super.parseContent(buffer);
                    }

                    @Override
                    protected boolean parseHeaders(ByteBuffer buffer) {
                        if (pfm.getAuthenticationMethod(getHttpChannel().getRequest()) == AuthenticationMethod.Basic) {
                            if (AltHttpConnectionFactory.this.debugLevel.isAllowingUnsercureTrace()) {
                                LOG.debug("Parsing HTTP Headers :\n{}",
                                        new String(buffer.array(), Charset.forName("ISO-8859-1")));
                            }
                            BasicAuthorizationBuffer authBuf = new BasicAuthorizationBuffer(
                                    BasicAuthorizationBuffer.Scope.Headers,
                                    buffer.position(),
                                    buffer.limit())
                            {
                                @Override
                                public void set(int i, byte b) {
                                    buffer.put(i, b);
                                }
                                @Override
                                public byte get(int i) {
                                    return buffer.get(i);
                                }
                                @Override
                                public void debug(String msg) {
                                    if (AltHttpConnectionFactory.this.debugLevel.isAllowingUnsercureTrace()) {
                                        LOG.debug(msg + " :\n{}",
                                                new String(buffer.array(), Charset.forName("ISO-8859-1")));
                                    }
                                }
                            };
                            if (authBuf.findCredentialsBoundaries()) {
                                // capture pwd
                                Credentials credentials = authBuf.replace(pm);
                                getHttpChannel().getRequest().setAttribute(Passwords.BASIC_AUTH_ATTRIBUTE_KEY, credentials);
                            }
                        }
                        // do things normally
                        return super.parseHeaders(buffer);
                    }
                };
            }

            @Override
            protected HttpChannelOverHttp newHttpChannel(HttpInput<ByteBuffer> httpInput) {
                return new HttpChannelOverHttp(getConnector(), getHttpConfiguration(), getEndPoint(), this, httpInput) {

                    @Override
                    public boolean handle() {
                        try {
                            return super.handle();
                        } finally {
                            CaptureContext<ByteBuffer> cc = captureContext;
                            if (cc != null) {
                                // at the end, clean the capture context
                                cc.destroy();
                                cc = null;
                            }
                        }
                    }

                    @Override
                    public boolean startRequest(HttpMethod httpMethod, String method,
                            ByteBuffer uri, HttpVersion version)
                    {
                        // let parse the URI
                        boolean result = super.startRequest(httpMethod, method, uri, version);

                        pfm.matches(getRequest()).ifPresent(fields -> {
                            // the incoming request URI matches a known path

                            // we are creating a capture context with the field names
                            captureContext = new CaptureContext<ByteBuffer>(fields);
                            Passwords pwd = captureContext.asPasswords();
                            getRequest().setAttribute(Passwords.ATTRIBUTE_KEY, pwd);
                        });

                        return result;
                    }
                };
            }
        }, connector, endPoint);
    }

}
