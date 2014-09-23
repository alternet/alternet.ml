package ml.alternet.discover;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.naming.InitialContext;

import ml.alternet.util.ClassUtil;
import ml.alternet.util.JNDIUtil;
import ml.alternet.web.WebAvailability;
import ml.alternet.web.WebContext;

/**
 * A simple service discovery component for retrieving classes.
 *
 * <p>
 * The lookup is done once for every class name to discover.
 * </p>
 *
 * <p>
 * An implementation can be render either as a raw {@link Class} or as a
 * singleton instance (or both across multiple calls). Singletons are
 * instanciated by their method "<tt>newInstance()</tt>" if they have one, or by
 * their default zero argument constructor. If the singleton fails to
 * instanciate, the user would still have the possibility to get the raw class
 * and build itself an instance.
 * </p>
 *
 * <h3>Lookup keys and variants</h3>
 *
 * <p>
 * The lookup key is usually the fully qualified name of a class (usually
 * abstract or an interface) : <tt>org.acme.Foo</tt>, and the resolved class
 * name should be a concrete implementation of it; the lookup key can also be a
 * name, even a JNDI name.
 * </p>
 *
 * <p>
 * The lookup key can be supplied with a variant, in order to bind several
 * implementations (this has to be taken in charge by the caller); for example,
 * <tt>ml.reflex.xml.SerializerFactory/image/png</tt>. Basically, it means that
 * we want a factory that can supply a serializer for "image/png".
 * </p>
 *
 * <h3>Class localization</h3>
 *
 * <p>
 * An implementation is found from the key supplied as follows:
 * </p>
 * <ol>
 * <li>The value of the system property with the name of the key supplied if it
 * exists and is accessible.</li>
 * <li>The value of the JNDI property with the name of the key supplied prepend
 * with "<tt>java:comp/env/</tt>" if it exists and is accessible; if the
 * resolved object is a string, it stands for a class name, otherwise for the
 * instance to return.</li>
 * <li>The value of the init parameter of the Web application with the name of
 * the key supplied if it exists and is accessible (
 * 
 * <pre>
 * &lt;web-app&gt;
 *    &lt;context-param&gt;
 * </pre>
 * 
 * ). During its initialization, the web application must have registered a
 * filter <tt>ml.alternet.web.WebFilter</tt>.</li>
 * <li>The contents of the file "<tt>[discoveryService.properties]</tt>" of the
 * current directory if it exists.</li>
 * <li>The contents of the file "
 * <tt>$USER_HOME/discovery-service.properties</tt>" if it exists.</li>
 * <li>The contents of the file "
 * <tt>$JAVA_HOME/jre/lib/discovery-service.properties</tt>" if it exists.</li>
 * <li>The Jar Service Provider discovery mechanism specified in the Jar File
 * Specification, and ammended by the special use of this class (see below). A
 * jar file can have a resource (i.e. an embedded file) such as
 * <tt>META-INF/xservices/package.Class</tt> (or
 * <tt>META-INF/xservices/package.Class/variant</tt> if the key contains a
 * variant) containing the name of the concrete class to instantiate.</li>
 * <li>The fallback default implementation, which is given by the
 * <tt>META-INF/xservices/</tt> of the user library (services found with a line
 * of comment before or ending with a comment that contains "default" will be
 * processed at the end)</li>
 * </ol>
 *
 * <p>
 * The first value found is returned. If one of those method fails, the next is
 * tried.
 * </p>
 *
 * <h3>META-INF services and xservices</h3>
 * <p>
 * The location used by this tool is <tt>META-INF/xservices/</tt>, not
 * <tt>META-INF/services/</tt>. Actually, the common <tt>META-INF/services/</tt>
 * directory is ruled by different conventions than those used here
 * (specifically due to the "variant" of the key). To avoid confusion, another
 * directory name has been retained, actually "xservices" (that stands for
 * "extended services").
 * </p>
 * 
 * <h3>Service names</h3>
 * <p>Inner class names contains a $ sign in their name ; all lookup are
 * performed on keys where the $ sign is replace by a dot. If you define
 * manually a META-INF/xservices/ entry, don't write file names or directory
 * names with a dollar, but with a dot instead.</p>
 *
 * @see LookupKey
 * @see ServiceLoader
 *
 * @author Philippe Poulard
 */
public final class DiscoveryService {

    /**
     * A map of {key, classes or singleton}.
     *
     * The key is usually the name of a class (an interface), the value a class
     * that implement or extend it or a singleton of it.
     *
     * The key can be of the form <tt>package.Class/variant</tt> if several
     * variants are available.
     */
    @SuppressWarnings("rawtypes")
    private static final Map CLASSES = new HashMap();

    /**
     * A marker that indicates that the class for the given key was already
     * searched and not found.
     */
    private static final Object NOT_FOUND = new Object();

    /**
     * The file "<tt>discoveryService.properties</tt>", to load once.
     */
    private static Properties PROPERTIES;

    private DiscoveryService() {}

    /**
     * Load the property file (once) in various locations (current directory,
     * user home, java home). If a property file has been already loaded, return
     * it.
     *
     * @return The loaded properties, might be empty but not <code>null</code>.
     */
    private static Properties loadProperties() {
        if (PROPERTIES == null) {
            PROPERTIES = new Properties();
            // Pairs of {sytemProp, fileName}
            // The sytem property is used to retrieve the base file
            // from which to resolve the relative file name.
            String[] propertiesFile = new String[] { null /* current dir */,
                    "discoveryService.properties", "user.home",
                    "discoveryService.properties", "java.home",
                    "lib" + File.separator + "discovery-service.properties" };
            // try with the files, the first found is used
            // and the other are ignored
            for (int i = 0; i < propertiesFile.length;) {
                String systemProperty = propertiesFile[i++];
                String fileName = propertiesFile[i++];
                File file;
                if (systemProperty != null) {
                    try { // try to find a system property
                        systemProperty = System.getProperty(systemProperty);
                        if (systemProperty == null) {
                            continue;
                        }
                    } catch (SecurityException se) {
                        continue;
                    }
                    file = new File(systemProperty, fileName);
                } else {
                    file = new File(fileName);
                }
                if (file.exists()) {
                    try {
                        PROPERTIES.load(new FileInputStream(file));
                        break;
                    } catch (FileNotFoundException e) {
                    } catch (IOException e) {
                    }
                }
            }
        }
        return PROPERTIES;
    }

    /**
     * Get the available classes of a service defined in jar libraries.
     *
     * @param service
     *            A service, something like "
     *            <tt>META-INF/xservices/package.Class</tt>". Variants are
     *            supported.
     *
     * @return A non-<code>null</code> iterator on the class names found.
     */
    @SuppressWarnings("rawtypes")
    private static Iterator getClasses(String service) {
        Enumeration e = null;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = DiscoveryService.class.getClassLoader();
            }
            e = cl.getResources(service);
        } catch (IOException ioe) {
        }
        if (e == null || !e.hasMoreElements()) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            final Enumeration enumer = e;
            return new Iterator() {
                Enumeration e = enumer; // URLs
                String clazz = null; // class name
                String defaultClass = null; // default class name, if a comment
                                            // "# default" is found
                InputStream is = null; // current URL content
                Reader r = null;
                BufferedReader br = null;

                @Override
                public boolean hasNext() {
                    if (clazz == null) {
                        if (is == null) {
                            if (e == null) {
                                if (defaultClass == null) {
                                    return false;
                                } else {
                                    // use the default class in last resort
                                    clazz = defaultClass;
                                    defaultClass = null;
                                    return true;
                                }
                            } else if (e.hasMoreElements()) {
                                try {
                                    URL u = (URL) e.nextElement();
                                    is = u.openStream();
                                    r = new InputStreamReader(is, "UTF-8");
                                    br = new BufferedReader(r);
                                } catch (IOException e) {
                                }
                            } else {
                                e = null;
                                return hasNext();
                            }
                        }
                        try {
                            clazz = br.readLine();
                            boolean isDefault = false;
                            while (clazz != null) {
                                // Strip comment
                                int sharp = clazz.indexOf('#');
                                if (sharp != -1) {
                                    if (clazz.substring(sharp).indexOf(
                                            "default") != -1) {
                                        // this is the default
                                        isDefault = true;
                                    }
                                    clazz = clazz.substring(0, sharp);
                                }
                                clazz = clazz.trim();
                                if (clazz.length() == 0) {
                                    clazz = br.readLine();
                                    continue;
                                }
                                if (isDefault) {
                                    // use the default later
                                    defaultClass = clazz;
                                    isDefault = false;
                                    // try to find another implementation
                                    clazz = br.readLine();
                                    continue;
                                }
                                break; // found "package.ConcreteClass"
                            }
                        } catch (IOException e) {
                        }
                        if (clazz == null) { // end of stream
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException ioe) {
                                }
                                is = null;
                            }
                            if (r != null) {
                                try {
                                    r.close();
                                } catch (IOException ioe) {
                                }
                                r = null;
                            }
                            if (br == null) {
                                try {
                                    br.close();
                                } catch (IOException ioe) {
                                }
                                br = null;
                            }
                            // try next URL in enum if some available
                            return hasNext();
                        } else {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }

                @Override
                public Object next() {
                    if (clazz == null && !hasNext()) {
                        throw new NoSuchElementException();
                    }
                    String next = clazz;
                    clazz = null;
                    return next;
                }

                @Override
                public void remove() {
                }
            };
        }
    }

    /**
     * Find a key: either by returning the value if a key has been already
     * processed, or by applying the lookup strategy.
     *
     * @param key
     *            The key to find, a class name + an optional variant.
     *
     * @return The value bound to the key, either a class or a singleton;
     *         <code>null</code> if not found.
     *
     * @throws ClassNotFoundException
     *             When the class name has been resolved but the concrete
     *             implementation was not found.
     */
    @SuppressWarnings("unchecked")
    private static Object find(String key) throws ClassNotFoundException {
        Object o = CLASSES.get(key);
        if (o == null) {
            String impl = null;
            try {
                // try to find a system property
                impl = System.getProperty(key);
            } catch (SecurityException se) {
            }
            if (impl == null) {
                // try JNDI java:comp/env/
                try {
                    Object resolved = new InitialContext()
                            .lookup(JNDIUtil.JAVA_COMP_ENV + "/" + key);
                    if (resolved instanceof String) {
                        impl = (String) resolved;
                    } else {
                        return resolved;
                    }
                } catch (Exception e) {
                }
                if (impl == null) {
                    // try <web-app><context-param>... for registered web apps
                    if (WebAvailability.servletAvailable()) {
                        impl = WebContext.getInitParameter(key);
                    }
                    if (impl == null) {
                        // try to find the file "discoveryService.properties" in
                        // various locations
                        Properties properties = loadProperties();
                        impl = properties.getProperty(key);
                        if (impl == null) {
                            // try to find services in the CLASSPATH
                            // note that ufos-xxx.jar will be the last used
                            // (this is done automatically)
                            String serviceId = "META-INF/xservices/" + key;
                            @SuppressWarnings("rawtypes")
                            Iterator it;
                            for (it = getClasses(serviceId); it.hasNext();) {
                                impl = (String) it.next();
                                // ignore others
                                while (it.hasNext()) {
                                    // consume everything (force to close
                                    // streams)
                                    it.next();
                                }
                            }
                        }
                    }
                }
            }
            try {
                if (impl == null || impl.length() == 0) {
                    o = NOT_FOUND;
                } else {
                    o = ClassUtil.load(impl);
                }
            } catch (ClassNotFoundException cnfe) {
                o = NOT_FOUND;
                throw cnfe;
            } finally {
                CLASSES.put(key, o);
            }
        }
        if (o == NOT_FOUND) {
            return null;
        } else {
            return o;
        }
    }

    /**
     * Return an implementation of a class.
     *
     * @param key
     *            The class name to lookup, with eventually a variant.
     * @param <T>
     *            The type of the class to lookup.
     *
     * @return The implementation of that class.
     *
     * @throws ClassNotFoundException
     *             When the class is not found.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> lookup(String key) throws ClassNotFoundException {
        key = key.replace('$', '.');
        Object o = find(key);
        if (o == null) {
            return null;
        } else if (o instanceof Class) {
            return (Class<T>) find(key);
        } else {
            return (Class<T>) o.getClass();
        }
    }

    /**
     * Return the singleton of a class.
     *
     * @param key
     *            The class name to lookup, with eventually a variant.
     * @param <T>
     *            The type of the class to lookup.
     *
     * @return The singleton instance of that class. The singleton is built with
     *         the static method "newInstance()" of the class if it exists,
     *         otherwise with the default constructor.
     *
     * @throws InstantiationException
     *             When the class can't be instanciated.
     * @throws IllegalAccessException
     *             When the class can't be accessed.
     * @throws ClassNotFoundException
     *             When the class is not found.
     */
    @SuppressWarnings("unchecked")
    public static <T> T lookupSingleton(String key)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        key = key.replace('$', '.');
        Object o = find(key);
        if (o instanceof Class) {
            Class<T> c = (Class<T>) o;
            try {
                Method method = c.getMethod("newInstance", (Class<T>) null);
                o = method.invoke((Object) null, (Object) null);
            } catch (Exception e) {
                o = c.newInstance();
            }
            // replace the class by an instance
            CLASSES.put(key, o);
        }
        return (T) o;
    }

    /**
     * Return the singleton of a class.
     *
     * @param clazz
     *            The class to lookup, without a variant.
     * @param <T>
     *            The type of the class to lookup.
     *
     * @return The singleton instance of that class. The singleton is built with
     *         the static method "newInstance()" of the class if it exists,
     *         otherwise with the default constructor.
     *
     * @throws InstantiationException
     *             When the class can't be instanciated.
     * @throws IllegalAccessException
     *             When the class can't be accessed.
     * @throws ClassNotFoundException
     *             When the class is not found.
     */
    public static <T> T lookupSingleton(Class<T> clazz)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        return lookupSingleton(clazz.getName());
    }

    /**
     * Return a new instance of a class.
     *
     * @param key
     *            The class name to lookup, with eventually a variant.
     * @param <T>
     *            The type of the class to return.
     *
     * @return The new instance of that class. It is built with the static
     *         method "newInstance()" of the class if it exists, otherwise with
     *         the default constructor.
     *
     * @throws InstantiationException
     *             When the class can't be instanciated.
     * @throws IllegalAccessException
     *             When the class can't be accessed.
     * @throws ClassNotFoundException
     *             When the class is not found.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> T newInstance(String key) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        key = key.replace('$', '.');
        Class clazz = DiscoveryService.lookup(key);
        if (clazz == null) {
            // key might be simply an instanciable class
            clazz = ClassUtil.load(key);
            CLASSES.put(key, clazz);
        }
        return newInstance(clazz);
    }

    /**
     * Return a new instance of a class.
     *
     * @param clazz
     *            The class.
     * @param <T>
     *            The type of the class to lookup.
     *
     * @return The new instance of that class. It is built with the static
     *         method "newInstance()" of the class if it exists, otherwise with
     *         the default constructor. If the given class is an interface or is
     *         abstract, it is looked up with no variant.
     *
     * @throws InstantiationException
     *             When the class can't be instanciated.
     * @throws IllegalAccessException
     *             When the class can't be accessed.
     * @throws ClassNotFoundException
     *             When the class is not found.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T newInstance(Class clazz) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        // check first if the class is instanciable
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            // lookup for a concrete implementation...
            String className = clazz.getName();
            clazz = DiscoveryService.lookup(className);
            if (clazz == null) {
                throw new ClassNotFoundException(
                        "Unable to find a concrete implementation of "
                                + className);
            }
        }
        // get an instance of that class
        try {
            Method method = clazz.getMethod("newInstance", (Class) null);
            return (T) method.invoke((Object) null, (Object) null);
        } catch (Exception e) {
            return (T) clazz.newInstance();
        }
    }

}
