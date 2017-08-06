package ml.alternet.test.security.web.server;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Base class for tests to run in a Web container.
 *
 * @author Philippe Poulard
 *
 * @param <T> The Web server.
 */
public abstract class ServerTestHarness<T> {

    static AtomicInteger PORT = new AtomicInteger(18641);

    public int port = PORT.getAndIncrement();

    public T server;

    public String userName = "who";
    public String unsafePwd = "da_AcTu@| P@zzm0R|)";
    public String resourceBase = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();

    /** Tests registered inside the server but to run outside the server. */
    protected ConcurrentHashMap<String, Runnable> serverTests = new ConcurrentHashMap<String, Runnable>();

    @BeforeClass
    public final void startServer() throws Exception {
        if (server == null) {
            doStartServer();
        }
    }

    @AfterClass
    public final void stopServer() throws Exception {
        if (server != null) {
            doStopServer();
            server = null;
        }
    }

    public abstract void doStartServer() throws Exception;

    public abstract void doStopServer() throws Exception;

}
