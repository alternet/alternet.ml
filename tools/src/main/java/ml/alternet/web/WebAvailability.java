package ml.alternet.web;

import ml.alternet.util.Util;

/**
 * Indicates whether servlet classes are available.
 *
 * <p>
 * This class doesn't have any dependency with the servlet packages, and can be
 * used safely as a guard before using some classes that are referring to this
 * package (and might avoid some "ClassNotFoundException").
 * </p>
 *
 * @author Philippe Poulard
 */
@Util
public final class WebAvailability {

    private WebAvailability() { }

    private static Boolean IS_AVAILABLE;

    /**
     * Indicates whether the servlet package is available; this doesn't imply
     * that a web application is running, it only states that the web classes
     * are available.
     *
     * @return <tt>true</tt> if the servlet package is available, <tt>false</tt>
     *         otherwise.
     */
    public static boolean servletAvailable() {
        if (IS_AVAILABLE == null) {
            try {
                Class.forName("javax.servlet.ServletContext");
                IS_AVAILABLE = Boolean.TRUE;
            } catch (ClassNotFoundException cnfe) {
                IS_AVAILABLE = Boolean.FALSE;
            }
        }
        return IS_AVAILABLE.booleanValue();
    }

}
