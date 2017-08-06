package ml.alternet.misc;

import ml.alternet.util.JNDIUtil;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * A factory that supplies an initial context that stores bindings in a map.
 *
 * @author Philippe Poulard
 *
 * @see JNDIUtil#setDefaultInitialContextFactory()
 */
public class JNDIInitialContextFactory implements InitialContextFactory {

    private static Context CONTEXT;

    static {
        try {
            CONTEXT = new InitialContext(true) {
                Map<String, Object> bindings = new HashMap<>();

                @Override
                public void bind(String name, Object obj) throws NamingException {
                    this.bindings.put(name, obj);
                }

                @Override
                public Object lookup(String name) throws NamingException {
                    return this.bindings.get(name);
                }
            };
        } catch (NamingException e) { // can't happen.
            throw WtfException.throwException(e);
        }
    }

    /**
     * Create an initial context.
     *
     * @param environment Unused.
     */
    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return CONTEXT;
    }

    /**
     * Bind an object to a name.
     *
     * @param name The name.
     * @param obj The object.
     */
    public static void bind(String name, Object obj) {
        try {
            CONTEXT.bind(name, obj);
        } catch (NamingException e) { // can't happen.
            throw WtfException.throwException(e);
        }
    }

}
