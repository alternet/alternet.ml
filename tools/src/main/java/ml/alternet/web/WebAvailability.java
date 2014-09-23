package ml.alternet.web;

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
public final class WebAvailability {

    private WebAvailability() {
    }

    private static Boolean isAvailable;

    /**
     * Indicates whether the servlet package is available; this doesn't imply
     * that a web application is running, it only states that the web classes
     * are available.
     * 
     * @return <tt>true</tt> if the servlet package is available, <tt>false</tt>
     *         otherwise.
     */
    public static boolean servletAvailable() {
        if (isAvailable == null) {
            try {
                Class.forName("javax.servlet.ServletContext");
                isAvailable = Boolean.TRUE;
            } catch (ClassNotFoundException cnfe) {
                isAvailable = Boolean.FALSE;
            }
        }
        return isAvailable.booleanValue();
    }

}
