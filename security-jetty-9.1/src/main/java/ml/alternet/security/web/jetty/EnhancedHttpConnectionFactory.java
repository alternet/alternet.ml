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
public class EnhancedHttpConnectionFactory extends HttpConnectionFactory implements DebugLevel.Debuggable {

    // configuration :
    // -a set of paths that received forms with passwords are configured by the user
    //
    // strategy :
    // -the connection is called on "onFillable()"
    // -we first extract and parse the URI from which we get the target path
    // -if the request URI matches the paths of the configuration
    //     we are setting a "capture context" and the process can go on
    // -after handling, the context is removed

    static final Logger LOG = Log.getLogger(EnhancedHttpConnectionFactory.class);

    /**
     * The size of the buffer.
     */
    public static final int BUFFER_SIZE = 8196;

    PasswordManager pm;
    PasswordFieldMatcher pfm;
    FormLimit formLimit;
    DebugLevel debugLevel = new DebugLevel();

    ThreadLocal<CaptureContext<ByteBuffer>> captureContext = new ThreadLocal<CaptureContext<ByteBuffer>>();

    /**
     * Allow to enhance a Jetty server.
     *
     * @param wac The web application to enhance.
     *
     * @see ml.alternet.security.impl.StrongPasswordManager
     */
    public EnhancedHttpConnectionFactory(@Name(value="webAppContext") WebAppContext wac) {
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
    public EnhancedHttpConnectionFactory(PasswordFieldMatcher pfm, FormLimit formLimit) {
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
    public EnhancedHttpConnectionFactory(PasswordManager pm, WebAppContext wac) {
        this.pm = pm;
        WebappPasswordFieldMatcher wpfm = new WebappPasswordFieldMatcher(wac);
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
    public EnhancedHttpConnectionFactory(PasswordManager pm, PasswordFieldMatcher pfm, FormLimit formLimit) {
        this.pm = pm;
        this.pfm = pfm;
        this.formLimit = formLimit;
    }

    @Override
    public DebugLevel getDebugLevel() {
        return this.debugLevel;
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {
        return configure(new HttpConnection(getHttpConfiguration(), connector, endPoint) {
            @Override
            protected HttpInput<ByteBuffer> newHttpInput() {
                return new HttpInputOverHTTP(this) {
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
                            return captureContext.get();
                        }

                        @Override
                        public void log(Exception exception) {
                            LOG.warn(exception.toString());
                            LOG.debug(exception);
                        }
                    };
                    @Override
                    public void recycle() {
                        super.recycle();
                        fr.reset();
                        wBuf = null;
                    }
                    @Override
                    public void content(ByteBuffer item) {
                        CaptureContext<ByteBuffer> cc = captureContext.get();
                        // "item" is a READONLY clone of "writableBuffer"
                        // we are just creating a WRITABLE clone of it
                        if (cc != null) {
                            ByteBuffer writableBuffer = cc.writableInputBuffer;
//                            wBuf = ByteBuffer.wrap(writableBuffer.array(), writableBuffer.position(), writableBuffer.remaining());
                            wBuf = ByteBuffer.wrap(writableBuffer.array(), item.position(), item.remaining());
                            // wBuf is now a WRITABLE clone of "writableBuffer"
//System.err.println(item.position() + " " + item.remaining() + " ::: " + wBuf.position() + " " + wBuf.remaining());
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
                                final byte[] data = new byte[EnhancedHttpConnectionFactory.BUFFER_SIZE];
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
                };
            }

            @Override
            protected HttpParser newHttpParser() {
                return new HttpParser(newRequestHandler(), getHttpConfiguration().getRequestHeaderSize()) {
                    @Override
                    protected boolean parseContent(ByteBuffer buffer) {
                        CaptureContext<ByteBuffer> cc = captureContext.get();
                        if (cc != null) {
                            // we need to store this buffer here because
                            // in one of the next stage, it is passed as a readonly buffer
                            // (I don't know why)
                            cc.writableInputBuffer = buffer;
                            if (EnhancedHttpConnectionFactory.this.debugLevel.isAllowingUnsercureTrace()) {
                                LOG.debug("Parsing HTTP request :\n{}", new String(buffer.array()));
                            }
                        }
                        return super.parseContent(buffer);
                    }

                    @Override
                    protected boolean parseHeaders(ByteBuffer buffer) {
                        if (pfm.getAuthenticationMethod() == AuthenticationMethod.Basic) {
                            if (EnhancedHttpConnectionFactory.this.debugLevel.isAllowingUnsercureTrace()) {
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
                                    if (EnhancedHttpConnectionFactory.this.debugLevel.isAllowingUnsercureTrace()) {
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
                            CaptureContext<ByteBuffer> cc = captureContext.get();
                            if (cc != null) {
                                // at the end, clean the capture context
                                cc.destroy();
                                captureContext.remove();
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
                            CaptureContext<ByteBuffer> cc = new CaptureContext<ByteBuffer>(fields);
                            captureContext.set(cc);
                            Passwords pwd = cc.asPasswords();
                            getRequest().setAttribute(Passwords.ATTRIBUTE_KEY, pwd);
                        });

                        return result;
                    }
                };
            }
        }, connector, endPoint);
    }

}
