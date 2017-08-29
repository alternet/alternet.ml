package ml.alternet.security.web.tomcat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import ml.alternet.misc.Invoker;
import ml.alternet.misc.Thrower;
import ml.alternet.security.PasswordManager;
import ml.alternet.security.PasswordManagerFactory;
import ml.alternet.security.web.server.DebugLevel;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.CoyoteAdapter;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.coyote.Adapter;
import org.apache.coyote.ProtocolHandler;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.IntrospectionUtils;

/**
 * A protocol handler wrapper that enhance a Tomcat server for
 * safe-processing of passwords.
 *
 * <p>This protocol handler wrapper allow to capture the
 * passwords that would be sent by the client BEFORE they are
 * processed as Strings ; after capturing, the raw data are replaced
 * with '*'. The captured passwords are wrapped in an encrypted form
 * and are available to the user by the classes of the package
 * <tt>ml.alternet.security.web</tt>.</p>
 *
 * <p>The configuration of the wrapped protocol handler remains the
 * same, except for the "protocol" attribute that have to be set
 * to this class name. Additional attributes are "tomcatProtocol"
 * (with the replaced value of the "protocol" attribute),
 * "passwordManager" (by default, it is the strong password
 * manager), and "allowUnsecureTrace" (false by default).</p>
 *
 * <pre>&lt;Connector port="8080"
 *             tomcatProtocol="HTTP/1.1"
 *             protocol="ml.alternet.security.web.tomcat.EnhancedProtocolHandler"
 *             passwordManager="ml.alternet.security.impl.StrongPasswordManager"
 *             allowUnsecureTrace="false"
 *             connectionTimeout="20000"
 *             redirectPort="8443" /&gt;
 * </pre>
 *
 * @author Philippe Poulard
 */
public class EnhancedProtocolHandler implements ProtocolHandler, DebugLevel.Debuggable {

    static final Log LOG = LogFactory.getLog(EnhancedProtocolHandler.class);

    public static ThreadLocal<org.apache.catalina.connector.Request> request = new ThreadLocal<>();

    private ProtocolHandler ph; // ph 7 is neutral - just joking
    private DebugLevel debugLevel = new DebugLevel();
    private PasswordManager pm;
    private Map<String,String> props = new HashMap<>(); // pending props until ph is set

    @Override
    public DebugLevel getDebugLevel() {
        return this.debugLevel;
    }

    /**
     * Set a password manager from its class name.
     *
     * @param passwordManager The name of the password manager.
     *
     * @see PasswordManager
     */
    public void setPasswordManager(String passwordManager) {
        try {
            this.pm = (PasswordManager) Class.forName(passwordManager).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            Thrower.doThrow(e);
        }
    }

    /**
     * Get the password manager.
     *
     * @return By default, the password manager use encryption.
     */
    public PasswordManager getPasswordManager() {
        if (this.pm == null) {
            this.pm = PasswordManagerFactory.getStrongPasswordManager();
        }
        return this.pm;
    }

    /**
     * Allow unsecure trace (<code>false</code> by default).
     * DO NOT ALLOW UNSECURE TRACE IN PRODUCTION ENVIRONMENT.
     *
     * @param value <code>true</code> to allow unsecure trace,
     *      <code>false</code> to disallow unsecure trace.
     */
    public void setAllowUnsecureTrace(String value) {
        if ("true".equalsIgnoreCase(value)) {
            this.debugLevel.allowUnsecureTrace();
        } else {
            this.debugLevel.disallowUnsecureTrace();
        }
    }

    private void setProtocolHandler(String phClass) {
        try {
            ph = (ProtocolHandler) Class.forName(phClass).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            Thrower.doThrow(e);
        }
    }

    /**
     * Set a configured property.
     *
     * @param name The name of the property.
     * @param value The value of the property.
     */
    public boolean setProperty(String name, String value) {
        if ("tomcatProtocol".equals(name)) {
            // see Connector.setProtocol(String)
            if (AprLifecycleListener.isAprAvailable()) {
                if ("HTTP/1.1".equals(value)) {
                    setProtocolHandler("org.apache.coyote.http11.Http11AprProtocol");
                } else if ("AJP/1.3".equals(value)) {
                    setProtocolHandler
                        ("org.apache.coyote.ajp.AjpAprProtocol");
                } else if (value != null) {
                    setProtocolHandler(value);
                } else {
                    setProtocolHandler
                        ("org.apache.coyote.http11.Http11AprProtocol");
                }
            } else {
                if ("HTTP/1.1".equals(value)) {
                    setProtocolHandler
                        ("org.apache.coyote.http11.Http11NioProtocol");
                } else if ("AJP/1.3".equals(value)) {
                    setProtocolHandler
                        ("org.apache.coyote.ajp.AjpNioProtocol");
                } else if (value != null) {
                    setProtocolHandler(value);
                }
            }
        } else if (ph == null) {
            props.put(name, value);
            return true;
        } else {
            return IntrospectionUtils.setProperty(ph, name, value);
        }
        if (ph != null && props != null) {
            props.forEach((n,v) -> IntrospectionUtils.setProperty(ph, n, v));
            props = null;
        }
        return true;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        try {
            CoyoteAdapter ca = (CoyoteAdapter) adapter;
            Connector connector = Invoker.get(ca, "connector");
            adapter = new AlternetCoyoteAdapter(connector, getDebugLevel(), this.pm);
            ph.setAdapter(adapter);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Thrower.doThrow(e);
        }
    }

    // delegate methods

    @Override
    public Adapter getAdapter() {
        return ph.getAdapter();
    }

    @Override
    public Executor getExecutor() {
        return ph.getExecutor();
    }

    @Override
    public void init() throws Exception {
        ph.init();
    }

    @Override
    public void start() throws Exception {
        ph.start();
    }

    @Override
    public void pause() throws Exception {
        ph.pause();
    }

    @Override
    public void resume() throws Exception {
        ph.resume();
    }

    @Override
    public void stop() throws Exception {
        ph.stop();
    }

    @Override
    public void destroy() throws Exception {
        ph.destroy();
    }

    @Override
    public boolean isAprRequired() {
        return ph.isAprRequired();
    }

    @Override
    public boolean isCometSupported() {
        return ph.isCometSupported();
    }

    @Override
    public boolean isCometTimeoutSupported() {
        return ph.isCometTimeoutSupported();
    }

    @Override
    public boolean isSendfileSupported() {
        return ph.isSendfileSupported();
    }

}
