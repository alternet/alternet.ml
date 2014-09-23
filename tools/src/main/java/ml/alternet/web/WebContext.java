package ml.alternet.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Hold the Web context for each thread.
 *
 * @author Philippe Poulard
 */
public final class WebContext {

    private final ServletContext ctxt;
    private final ServletRequest request;
    private final ServletResponse response;

    // Hold the context for each thread and its descendant.
    private static final ThreadLocal<WebContext> WEB_CONTEXT = new InheritableThreadLocal<WebContext>();

    /**
     * Create a Web context.
     *
     * @param context
     *            The servlet context.
     * @param request
     *            The current request.
     * @param response
     *            The response.
     */
    public WebContext(ServletContext context, ServletRequest request, ServletResponse response) {
        this.ctxt = context;
        this.request = request;
        this.response = response;
    }

    /**
     * Get the Web context in use.
     *
     * @return The current web context, or {@code null}.
     */
    public static WebContext get() {
        return WEB_CONTEXT.get();
    }

    /**
     * Set this Web context as the current Web context.
     */
    public void set() {
        WEB_CONTEXT.set(this);
    }

    /**
     * Remove the current web context.
     */
    public void remove() {
        WEB_CONTEXT.remove();
    }

    /**
     * Return the web application in use.
     *
     * @return The web application in use, or <tt>null</tt>.
     */
    public ServletContext getServletContext() {
        return this.ctxt;
    }

    /**
     * Return the request in use.
     *
     * @return The current servlet request, or <tt>null</tt>.
     */
    public ServletRequest getServletRequest() {
        return this.request;
    }

    /**
     * Return the response in use.
     *
     * @return The current servlet response, or <tt>null</tt>.
     */
    public ServletResponse getServletResponse() {
        return this.response;
    }

    /**
     * Return the init parameter of the web application in use.
     *
     * <pre>
     * &lt;web-app&gt;&lt;context-param&gt;...
     * </pre>
     *
     * @param key
     *            The key of the init parameter.
     * @return Its value, or <tt>null</tt>.
     */
    public static String getInitParameter(String key) {
        WebContext wc = get();
        if (wc != null) {
            ServletContext webApp = wc.getServletContext();
            if (webApp != null) {
                return webApp.getInitParameter(key);
            }
        }
        return null;
    }

    /**
     * Get the input stream of the request in use.
     *
     * @return The current input stream.
     *
     * @throws IOException
     *             When an I/O exception occurs
     */
    public static InputStream getInputStream() throws IOException {
        return get().getServletRequest().getInputStream();
    }

    /**
     * Get the character reader of the request in use.
     *
     * @return The current character reader.
     *
     * @throws IOException
     *             When an I/O exception occurs
     */
    public static Reader getReader() throws IOException {
        return get().getServletRequest().getReader();
    }

    /**
     * Get the content length of the request in use.
     *
     * @return The content length.
     */
    public static long getContentLength() {
        return get().getServletRequest().getContentLength();
    }

    /**
     * Get the output stream of the response in use.
     *
     * @return The current input stream.
     *
     * @throws IOException
     *             When an I/O exception occurs
     */
    public static OutputStream getOutputStream() throws IOException {
        return get().getServletResponse().getOutputStream();
    }

    /**
     * Get the output stream of the response in use.
     *
     * @return The current input stream.
     *
     * @throws IOException
     *             When an I/O exception occurs
     */
    public static Writer getWriter() throws IOException {
        return get().getServletResponse().getWriter();
    }

    /**
     * Get the real path of a relative path.
     *
     * @param filePath
     *            A path relative to the current web context.
     *
     * @return An absolute path.
     */
    public static String getRealPath(String filePath) {
        String realPath = get().getServletContext().getRealPath(filePath);
        return realPath;
    }

    /**
     * Get the registered MIME type bound to an extension.
     *
     * @param extension
     *            The extension of a file, e.g. ".html".
     *
     * @return The MIME type, e.g. "text/html".
     */
    public static String getMimeType(String extension) {
        String mimeType = get().getServletContext().getMimeType(extension);
        return mimeType;
    }

}
