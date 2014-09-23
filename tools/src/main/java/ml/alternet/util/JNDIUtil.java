package ml.alternet.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;

import ml.alternet.misc.JNDIInitialContextFactory;

/**
 * Utilities for JNDI.
 *
 * @author Philippe Poulard
 */
@Util
public class JNDIUtil {

    /**
     * "java:comp/env"
     */
    public static final String JAVA_COMP_ENV = "java:comp/env";

    private JNDIUtil() {
    }

    /**
     * Set the initial context factory.
     *
     * @param className
     *            The class name, must be an instance of
     *            {@link InitialContextFactory}
     */
    public static void setInitialContextFactory(String className) {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, className);
    }

    /**
     * Set the initial context factory.
     *
     * @see JNDIInitialContextFactory
     */
    public static void setDefaultInitialContextFactory() {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, JNDIInitialContextFactory.class.getName());
    }

    /**
     * Bind a ressource to JNDI.
     *
     * @param jndiName
     *            The path-separated name (that doesn't start with a slash) of
     *            the resource, relative to the default initial context
     *            "java:comp/env/".
     * @param factoryClassName
     *            The factory to use, must be an instance of
     *            {@link ObjectFactory}
     * @param className
     *            The class name of the resource, according to the factory.
     * @param data
     *            A list of name-value.
     *
     * @throws NamingException
     *             When a JNDI exception occurs
     */
    public static void bind(String jndiName, String factoryClassName, String className, String[][] data)
            throws NamingException {
        Context context = lookup();
        bind(context, jndiName, factoryClassName, className, data);
    }

    /**
     * Bind a ressource to JNDI.
     *
     * @param context
     *            The JNDI context.
     * @param jndiName
     *            The path-separated name (that doesn't start with a slash) of
     *            the resource, relative to the context.
     * @param factoryClassName
     *            The factory to use, must be an instance of
     *            {@link ObjectFactory}
     * @param className
     *            The class name of the resource, according to the factory.
     * @param data
     *            A list of name-value.
     *
     * @throws NamingException
     *             When a JNDI exception occurs
     */
    public static void bind(Context context, String jndiName, String factoryClassName, String className, String[][] data)
            throws NamingException {
        Reference ref = createReference(factoryClassName, className, data);
        bind(context, jndiName, ref);
    }

    /**
     * Bind a ressource to JNDI.
     *
     * @param context
     *            The JNDI context.
     * @param jndiName
     *            The path-separated name (that doesn't start with a slash) of
     *            the resource, relative to the context.
     * @param ref
     *            The reference of the resource to bind.
     * @throws NamingException
     *             When a JNDI exception occurs
     */
    public static void bind(Context context, String jndiName, Reference ref) throws NamingException {
        if (jndiName == null) {
            jndiName = "";
        }
        int sep = jndiName.indexOf('/');
        if (sep == -1) {
            context.bind(jndiName, ref);
        } else {
            String ctxtName = jndiName.substring(0, sep);
            Context ctxt;
            try {
                ctxt = (Context) context.lookup(ctxtName);
            } catch (NamingException ne) {
                ctxt = context.createSubcontext(ctxtName);
            }
            bind(ctxt, jndiName.substring(sep + 1), ref);
        }
    }

    /**
     * Bind an object to JNDI.
     *
     * @param context
     *            The JNDI context.
     * @param jndiName
     *            The path-separated name (that doesn't start with a slash) of
     *            the resource, relative to the context.
     * @param o
     *            The object to bind.
     * @throws NamingException
     *             When a JNDI exception occurs
     */
    public static void bind(Context context, String jndiName, Object o) throws NamingException {
        if (jndiName == null) {
            jndiName = "";
        }
        int sep = jndiName.indexOf('/');
        if (sep == -1) {
            context.bind(jndiName, o);
        } else {
            String ctxtName = jndiName.substring(0, sep);
            Context ctxt;
            try {
                ctxt = (Context) context.lookup(ctxtName);
            } catch (NamingException ne) {
                ctxt = context.createSubcontext(ctxtName);
            }
            bind(ctxt, jndiName.substring(sep + 1), o);
        }
    }

    /**
     * Create a reference to a resource.
     *
     * @param factoryClassName
     *            The factory to use, must be an instance of
     *            {@link ObjectFactory}
     * @param className
     *            The class name of the resource, according to the factory.
     * @param data
     *            A list of name-value.
     *
     * @return A reference to a resource.
     */
    public static Reference createReference(String factoryClassName, String className, String[][] data) {
        Reference ref = new Reference(className, new StringRefAddr(data[0][0], data[0][1]), factoryClassName, null);
        for (int i = 1; i < data.length; i++) {
            ref.add(new StringRefAddr(data[i][0], data[i][1]));
        }
        return ref;
    }

    /**
     * Lookup the context "java:comp/env"
     *
     * @return The context "java:comp/env"
     *
     * @throws NamingException
     *             When a JNDI exception occurs
     */
    public static Context lookup() throws NamingException {
        return lookup(JAVA_COMP_ENV);
    }

    /**
     * Lookup for a context.
     *
     * @param path
     *            The path of the context to lookup.
     *
     * @return The context to lookup.
     *
     * @throws NamingException
     *             When a JNDI exception occurs
     *
     * @see #lookup()
     */
    public static Context lookup(String path) throws NamingException {
        Context initContext = new InitialContext();
        Context context = (Context) initContext.lookup(path);
        return context;
    }

}
