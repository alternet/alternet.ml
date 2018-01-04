package ml.alternet.properties;

import java.beans.FeatureDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;
import javax.el.StandardELContext;
import javax.el.StaticFieldELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import ml.alternet.misc.Thrower;
import ml.alternet.misc.Type;
import static ml.alternet.properties.NamesUtil.asPropName;
import static ml.alternet.properties.NamesUtil.asClassName;
import static java.util.stream.Collectors.joining;

/**
 * A property binder allow to unmarshall a property file to a tree of objects.
 *
 * The target objects can be either made by yourself or generated with a
 * properties template. The generator can be run with Maven.
 *
 * @author Philippe Poulard
 *
 * @see Generator
 */
public class Binder {

    static final Logger LOG = Logger.getLogger(Binder.class.getCanonicalName());

    static final WeakHashMap<Object, Map<String, Object>> FREEKEYS = new WeakHashMap<>();

    @SuppressWarnings("unchecked")
    static <T> T lookup($ that, String key) { // used by $.$()
        String[] keys = key.split("\\.");
        Map<String, Object> entries = Binder.FREEKEYS.get(that);
        if (entries == null) {
            return null;
        } else {
            Object part = entries.get(keys[0]);
            if (keys.length == 1) {
                return (T) part;
            } else {
                return lookup(($) part, Stream.of(keys).skip(1).collect(joining(".")));
            }
        }
    }

    /**
     * Various adapters can be used to convert a string to an item or a list of items.
     *
     * @author Philippe Poulard
     *
     * @param <K> The kind of adapter.
     * @param <T> The type of the target items.
     */
    public static class Adapter<K, T> {

        K key; // K ::: Class : generic adapter, or String : adapter for a key, or Pattern : adapter for a key
        Function<String, T> mapper;

        Adapter(K key, Function<String, T> mapper) {
            this.key = key;
            this.mapper = mapper;
        }

        /**
         * Create a mapping for fields of a given type.
         *
         * @param clazz The type of the field.
         * @param mapper Allow to produce an instance of that type.
         * @param <T> The actual type to produce.
         * @return An adapter.
         */
        public static <T> Adapter<Class<T>, T> map(Class<T> clazz, Function<String, T> mapper) {
            return new Adapter<Class<T>, T>(clazz, mapper);
        }

        /**
         * Create a mapping for fields with a given key ;
         * the key can contain '*' between parts of the key to
         * match any string for that part.
         *
         * @param key The key, e.g. "colors.background" or "colors.*"
         * @param mapper Allow to produce an instance of that type for that key.
         * @param <T> The actual type to produce.
         * @return An adapter.
         */
        public static <T> Adapter<String, T> map(String key, Function<String, T> mapper) {
            return new Adapter<String, T>(key, mapper);
        }

        /**
         * Create a mapping for fields with a given key that match a regular expression.
         *
         * @param keyRegexp The regular expression
         * @param mapper Allow to produce an instance of that type for the keys that match.
         * @param <T> The actual type to produce.
         * @return An adapter.
         */
        public static <T> Adapter<Pattern, T> map(Pattern keyRegexp, Function<String, T> mapper) {
            return new Adapter<Pattern, T>(keyRegexp, mapper);
        }

        /**
         * Create a mapping for multi-valued fields (list of items) with a given key ;
         * the key can contain '*' between parts of the key to
         * match any string for that part.
         *
         * @param key The key, e.g. "colors.background" or "colors.*"
         * @param mapper Allow to produce a single instance in the list of that type for that key.
         * @param <T> The actual type to produce.
         * @return An adapter.
         */
        public static <T> Adapter<String, List<T>> mapList(String key, Function<String, T> mapper) {
            return Adapter.<T, String> mapKList(key, mapper);
        }

        /**
         * Create a mapping for multi-valued fields (list of items) with a given key
         * that match a regular expression.
         *
         * @param key The regular expression
         * @param mapper Allow to produce a single instance in the list of that type for that key.
         * @param <T> The actual type to produce.
         * @return An adapter.
         */
        public static <T> Adapter<Pattern, List<T>> mapList(Pattern key, Function<String, T> mapper) {
            return Adapter.<T, Pattern> mapKList(key, mapper);
        }

        static <T, K> Adapter<K, List<T>> mapKList(K key, Function<String, T> mapper) {
            return new Adapter<K, List<T>>(key,
                val -> {
                    String[] values = val.split("\\s*,\\s*");
                    List<T> list = Stream.of(values)
                        .map(str -> mapper.apply(str))
                        .collect(Collectors.toList());
                    return list;
                }
            );
        }

    }

    /**
     * Unmarshall a property file to a tree of objects.
     *
     * @param input The property file.
     * @param clazz The target result (tree root)
     * @param adapters Optionally, allow to map string values to items or list of items for specific classes or keys.
     * @param <T> The type of the target object.
     * @return The properties as a tree of objects.
     * @throws IOException When an I/O error occurs.
     */
    public static <T> T unmarshall(InputStream input, Class<T> clazz, Adapter<?, ?>... adapters) throws IOException {
        HierarchicProperties<T> p = new HierarchicProperties<T>(clazz, adapters);
        p.load(input);
        return p.properties;
    }

    /**
     * Unmarshall a property file to a tree of objects.
     *
     * @param input The property file.
     * @param clazz The target result (tree root)
     * @param adapters Optionally, allow to map string values to items or list of items for specific classes or keys.
     * @param <T> The type of the target object.
     * @return The properties as a tree of objects.
     * @throws IOException When an I/O error occurs.
     */
    public static <T> T unmarshall(Reader input, Class<T> clazz, Adapter<?, ?>... adapters) throws IOException {
        HierarchicProperties<T> p = new HierarchicProperties<T>(clazz, adapters);
        p.load(input);
        return p.properties;
    }

    /**
     * Unmarshall existing properties to a tree of objects.
     *
     * @param properties The existing properties.
     * @param clazz The target result (tree root)
     * @param adapters Optionally, allow to map string values to items or list of items for specific classes or keys.
     * @param <T> The type of the target object.
     * @return The properties as a tree of objects.
     * @throws IOException When an I/O error occurs.
     */
    public static <T> T unmarshall(Properties properties, Class<T> clazz, Adapter<?, ?>... adapters)
            throws IOException
    {
        HierarchicProperties<T> p = new HierarchicProperties<T>(clazz, adapters);
        properties.forEach((k, v) -> p.put(k, v));
        return p.properties;
    }

    /**
     * Unmarshall a property file to a tree of objects.
     *
     * @param input The property file.
     * @param defaultValues The target object (tree root) for receiving values.
     * @param adapters Optionally, allow to map string values to items or list of items for specific classes or keys.
     * @param <T> The type of the target object.
     * @return The properties as a tree of objects.
     * @throws IOException When an I/O error occurs.
     */
    public static <T> T unmarshall(InputStream input, T defaultValues, Adapter<?, ?>... adapters)
            throws IOException
    {
        HierarchicProperties<T> p = new HierarchicProperties<T>(defaultValues, adapters);
        p.load(input);
        return p.properties;
    }

    /**
     * Unmarshall a property file to a tree of objects.
     *
     * @param input The property file.
     * @param defaultValues The target object (tree root) for receiving values.
     * @param adapters Optionally, allow to map string values to items or list of items for specific classes or keys.
     * @param <T> The type of the target object.
     * @return The properties as a tree of objects.
     * @throws IOException When an I/O error occurs.
     */
    public static <T> T unmarshall(Reader input, T defaultValues, Adapter<?, ?>... adapters)
            throws IOException
    {
        HierarchicProperties<T> p = new HierarchicProperties<T>(defaultValues, adapters);
        p.load(input);
        return p.properties;
    }

    /**
     * Unmarshall existing properties to a tree of objects.
     *
     * @param properties The existing properties.
     * @param defaultValues The target object (tree root) for receiving values.
     * @param adapters Optionally, allow to map string values to items or list of items for specific classes or keys.
     * @param <T> The type of the target object.
     * @return The properties as a tree of objects.
     * @throws IOException When an I/O error occurs.
     */
    public static <T> T unmarshall(Properties properties, T defaultValues, Adapter<?, ?>... adapters)
            throws IOException
    {
        HierarchicProperties<T> p = new HierarchicProperties<T>(defaultValues, adapters);
        properties.forEach((k, v) -> p.put(k, v));
        return p.properties;
    }

    @SuppressWarnings("serial")
    static class HierarchicProperties<T> extends Properties {

        T properties;
        Hashtable<String, Function<String, ?>> keyMappers = new Hashtable<>();
        Hashtable<Pattern, Function<String, Object>> regexpKeyMappers = new Hashtable<>();
        Hashtable<Class<?>, Function<String, ?>> classMappers = new Hashtable<>();
        Logger log;

        HierarchicProperties(T defaultValues, Adapter<?, ?>[] adapters) {
            LOG.info("Start populating " + defaultValues.getClass().getCanonicalName());
            this.properties = defaultValues;
            this.log = Logger.getLogger(defaultValues.getClass().getName());
            setAdapters(adapters);
        }

        HierarchicProperties(Class<T> clazz, Adapter<?, ?>[] adapters) {
            LOG.info("Start populating " + clazz.getCanonicalName());
            try {
                this.properties = clazz.newInstance();
                this.log = Logger.getLogger(clazz.getName());
                setAdapters(adapters);
            } catch (InstantiationException | IllegalAccessException e) {
                Thrower.doThrow(e);
            }
        }

        @SuppressWarnings("unchecked")
        void setAdapters(Adapter<?, ?>[] adapters) {
            // define a default mapper
            this.classMappers.put(Class.class, cl -> {
                try {
                    return Class.forName(cl);
                } catch (ClassNotFoundException e) {
                    log.warning("ClassNotFoundException " + cl);
                    return Thrower.doThrow(e);
                }
            });
            Stream.of(adapters).forEach(adapter -> {
                if (adapter.key instanceof String) {
                    String skey = (String) adapter.key;
                    if (skey.contains("*")) {
                        String pattern = skey.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".[^.]+");
                        Pattern regexp = Pattern.compile(pattern);
                        this.regexpKeyMappers.put(regexp, (Function<String, Object>) adapter.mapper);
                    } else {
                        this.keyMappers.put(skey, adapter.mapper);
                    }
                } else if (adapter.key instanceof Pattern) {
                    this.regexpKeyMappers.put((Pattern) adapter.key, (Function<String, Object>) adapter.mapper);
                } else { // adapter.key instanceof Class
                    this.classMappers.put((Class<?>) adapter.key, adapter.mapper);
                }
            });
        }

        @Override
        public synchronized Object put(Object key, Object value) {
            // aaaaa.bbb.ccccc.dd -> [aaaaa, bbb, ccccc, dd]
            String[] keys = ((String) key).split("\\.");
            Object current = this.properties;
            for (int i = 0 ; i < keys.length ; i++) {
                String propName = asPropName(keys[i]);
                // values that FAIL to parse rise errors
                // values with unknown keys (and no $) rise a warning
                try {
                    Field f = current.getClass().getField(propName);
                    Object node = f.get(current);
                    if (i == keys.length - 1) { // leaf
                        if (node == null) {
                            if (set(f, current, value)) {
                                this.log.fine(() -> "Setting " + key + " to " + escapeForLog(value));
                            } else {
                                // can't assign value
// TODO : handle non-static nested class
                                Class<?> c = f.getType();
                                node = c.newInstance();
                                f.set(current, node);
                                this.log.fine(() -> "Setting " + key + " to " + c.getCanonicalName());
                                try {
                                    // MUST have $ field
                                    f = node.getClass().getField("$");
                                    set(f, node, value);
                                    this.log.fine(() -> "Setting " + key + ".$ to " + escapeForLog(value));
                                } catch (NoSuchFieldException e) {
                                    // ERROR : can't assign the value
                                    throw new IllegalArgumentException("Adapter missing for the target class : Unable to create " + f.getType() + " from " + value);
                                }
                            }
                        } else {
                            try { // MAY have $ field
                                f = node.getClass().getField("$");
                                set(f, node, value);
                                this.log.fine(() -> "Setting " + key + ".$ to " + escapeForLog(value));
                            } catch (NoSuchFieldException e) {
                                // assume overriding existing value
                                if (set(f, current, value)) {
                                    this.log.fine(() -> "Setting " + key + " to " + escapeForLog(value));
                                } else {
                                    // ERROR : can't assign the value
                                    throw new IllegalArgumentException("Adapter missing for the target class : Unable to create " + f.getType() + " from " + value);
                                }
                            }
                        }
                    } else {
                        if (node == null) {
// TODO : handle non-static nested class
                            Class<?> c = f.getType();
                            node = c.newInstance();
                            f.set(current, node);
                            {
                                int iFinal = i;
                                this.log.fine(() -> "Setting " + Stream.of(keys).limit(iFinal).collect(joining( "." ))
                                    + " to " + c.getCanonicalName());
                            }
                        } // else nothing
                        current = node;
                    }
                } catch (NoSuchFieldException e) {
                    // try free keys
                    if (current instanceof $) {
                        Object next = null;
                        Map<String, Object> freeKeys = FREEKEYS.get(current);
                        if (freeKeys != null) {
                            next = freeKeys.get(propName);
                        }
                        if (next == null) {
                            // try nested class
                            Class<?> currentClass = current.getClass();
                            // nestedConfMember -> NestedConfMember
                            String keyName = asClassName(keys[i]);
                            String nestedClassName = current.getClass().getName() + "$" + keyName;
                            try {
                                next = Stream.of(current.getClass().getDeclaredClasses())
                                    .filter(c -> c.getName().equals(nestedClassName))
                                    .findFirst()
                                    .map(c -> {
                                            try {
                                                return c.getDeclaredConstructor(currentClass);
                                            } catch (NoSuchMethodException | SecurityException e1) {
                                                return null;
                                            }
                                    })
                                    .filter(c -> c != null)
                                    .get()
                                    .newInstance(current);
                                {
                                    Object nextFinal = next;
                                    int iFinal = i;
                                    this.log.fine(() -> "Setting "
                                        + Stream.of(keys).limit(iFinal).collect(joining( "." ))
                                        + " to " + nextFinal.getClass().getCanonicalName());
                                }
                            } catch (InstantiationException | NoSuchElementException ie) {
                                if (i == keys.length - 1) { // leaf
                                    // lookup for adapter or assume String
                                    Function<String, ?> f = keyMappers.get(key);
                                    if (f == null) {
                                        next = regexpKeyMappers.entrySet().stream()
                                            .filter(entry -> entry.getKey().matcher((String) key).matches())
                                            .map(entry -> entry.getValue())
                                            .findFirst()
                                            .map(fun -> fun.apply((String) value))
                                            .orElse(value);
                                    } else {
                                        next = f.apply((String) value);
                                    }
                                    {
                                        Object nextFinal = next;
                                        int iFinal = i;
                                        this.log.fine(() -> "Setting "
                                            + Stream.of(keys).limit(iFinal).collect(joining( "." ))
                                            + " to " + nextFinal.getClass().getCanonicalName());
                                    }
                                } else {
                                    // assume empty FreeKeys
                                    next = new $() { }; // just a subcontainer
                                    {
                                        int iFinal = i;
                                        this.log.fine(() -> "Setting "
                                            + Stream.of(keys).limit(iFinal).collect(joining( "." ))
                                            + " to new $()");
                                    }
                                }
                            } catch (IllegalAccessException | IllegalArgumentException
                                    | InvocationTargetException | SecurityException ie)
                            {
                                Thrower.doThrow(ie); // we have it but we can't use it
                            }
                        }
                        if (freeKeys == null) {
                            freeKeys = new Hashtable<>();
                            FREEKEYS.put(current, freeKeys);
                        }
                        freeKeys.put(propName, next);
                        current = next;
                    }
                } catch (SecurityException | IllegalArgumentException
                        | IllegalAccessException | InstantiationException e)
                {
                    Thrower.doThrow(e);
                }
            }
            return null;
        }

        boolean set(Field f, Object that, Object value) throws IllegalArgumentException, IllegalAccessException {
            Class<?> c = f.getType();
            String sVal = (String) value;
            // try expression resolver, e.g. "${whatever}"
            value = asExpression(sVal, c);
            // try file, e.g. "file:" or File object
            if (asFileToLoad(value, c)) {
                return true;
            }
            if (value != null) { // after expression evaluation
                if (c.isAssignableFrom(value.getClass())
                       // ensure that primitive types are compatible with boxed types and conversely
                    || Type.of(value.getClass()).unbox().equals(Type.of(c).unbox()))
                {
                    f.set(that, value);
                    return true;
                } else {
                    sVal = value.toString();
                }
            }
            final String sFinal = sVal;
            value = cast(c, sFinal)
                .orElseGet(() -> {
                    if (List.class.isAssignableFrom(c)) {
                        ParameterizedType genericType = (ParameterizedType) f.getGenericType();
                        Class<?> paramType = (Class<?>) genericType.getActualTypeArguments()[0];
                        String[] values = sFinal.split("\\s*,\\s*");
                        try {
                            List<?> list = Stream.of(values)
                                .map(str -> cast(paramType, str))
                                .map(Optional::get)
                                .collect(Collectors.toList());
                            return list;
                        } catch (NoSuchElementException e) {
                            throw new IllegalArgumentException("Adapter missing for the target class : "
                                    + "Unable to create list of "
                                    + paramType + " from " + sFinal, e);
                        }
                    } else {
                        return null;
                    }
                });
            f.set(that, value);
            return value != null;
        }

        Optional<Object> cast(Class<?> c, String s) {
            try {
                // try scalar types
                return Optional.of(Type.of(c).parse(s));
            } catch (UnsupportedOperationException uoe) {
                return Optional.ofNullable(classMappers.getOrDefault(c, k -> {
                    try {
                        return c.getConstructor(String.class).newInstance(s);
                    } catch (InstantiationException | IllegalAccessException
                            | IllegalArgumentException | InvocationTargetException e)
                    {
                        return Thrower.doThrow(e);
                    } catch (NoSuchMethodException | SecurityException e) {
                        return null;
                    }
                }).apply(s));
            }
        }

        boolean asFileToLoad(Object value, Class<?> c) {
            if (File.class.equals(c)) {
                return false; // if the field is a File, we have the value of that field
            }
            File file = null;
            if (value instanceof File) {
                file = (File) value;
            } else if (value instanceof String && ((String) value).startsWith("file:")) {
                try {
                    URI uri = new URI((String) value);
                    if (uri.isOpaque()) {
                        file = new File(uri.getSchemeSpecificPart());
                    } else {
                        file = new File((String) value);
                    }
                } catch (URISyntaxException e) { }
            }
            if (file != null) {
                // otherwise we are loading that file
                try {
                    // TODO : if a value is a File, and the target object has one of the
                    // static unmarshall methods, load the file to the target conf
                    // Because it may have its own Adapters
                    // => check the static methods of the Class c
                    File fileFinal = file;
                    this.log.info(() -> "Loading " + fileFinal);
                    FileInputStream input = new FileInputStream(file);
                    Properties p = new Properties();
                    p.load(input);
                    p.forEach((k, v) -> put(k, v));
                    this.log.info(() -> "End loading " + fileFinal);
                    return true;
                } catch (IOException e) {
                    Thrower.doThrow(e);
                }
            }
            return false; // it was not a file to load
        }

        Object asExpression(String value, Class<?> c) {
            if (value.contains("${")) {
                this.log.finest(() -> "Found expression " + value);
                // e.g. "${whatever}"
                ExpressionFactory elFactory = ExpressionFactory.newInstance();
                StandardELContext elCtxt = new StandardELContext(elFactory) {
                    VariableMapper vm = new VariableMapper() {
                        @Override
                        public ValueExpression setVariable(String variable, ValueExpression expression) {
                            throw new UnsupportedOperationException();
                        }
                        @Override
                        public ValueExpression resolveVariable(String variable) {
                            // the variable is not "a.b.c.d" but only "a"
                            // the remainder will be resolved by FieldELResolver
                            try {
                                Object node = properties.getClass().getField(variable).get(properties);
                                return elFactory.createValueExpression(node, Object.class);
                            } catch (IllegalArgumentException | IllegalAccessException
                                    | NoSuchFieldException | SecurityException e)
                            {
                                return Thrower.doThrow(e);
                            }
                        }
                    };
                    @Override
                    public VariableMapper getVariableMapper() {
                        return vm;
                    };
                    @Override
                    public ELResolver getELResolver() {
                        CompositeELResolver resolver = new CompositeELResolver();
                        resolver.add(new StaticFieldELResolver());
                        resolver.add(new MapELResolver());
                        resolver.add(new ResourceBundleELResolver());
                        resolver.add(new ListELResolver());
                        resolver.add(new ArrayELResolver());
                        resolver.add(new ELResolver() {
                            @Override
                            public void setValue(ELContext context, Object base, Object property, Object value) {
                            }
                            @Override
                            public boolean isReadOnly(ELContext context, Object base, Object property) {
                                return true;
                            }
                            @Override
                            public Object getValue(ELContext context, Object base, Object property) {
                                if (base != null && property instanceof String) {
                                    try {
                                        Field f = base.getClass().getField((String) property);
                                        Object node = f.get(base);
                                        context.setPropertyResolved(base, property);
                                        return node;
                                    } catch (NoSuchFieldException | SecurityException
                                        | IllegalArgumentException | IllegalAccessException e)
                                    { }
                                }
                                return null;
                            }
                            @Override
                            public Class<?> getType(ELContext context, Object base, Object property) {
                                return null;
                            }
                            @Override
                            public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
                                return null;
                            }
                            @Override
                            public Class<?> getCommonPropertyType(ELContext context, Object base) {
                                return null;
                            }
                        });
                        resolver.add(new BeanELResolver());
                        return resolver;
                    }
                };
                // TODO : sequences of ${el1} ${el2} ${el3} ...
//                Pattern PATTERN = Pattern.compile("#\\{(.+?)\\}");
//                StringBuffer sb = new StringBuffer();
//                Matcher matcher = PATTERN.matcher(s);
//                while (matcher.find()) {
//                    String expression = toExpression(matcher.group(1));
//                    Object result = evaluateValueExpression(expression);
//                    matcher.appendReplacement(sb, result != null ? result.toString() : "");
//                }
//                matcher.appendTail(sb);
//                return sb.toString();
                ValueExpression elValue = elFactory.createValueExpression(elCtxt, value, c); // "Object.class" or "c" ?
                return elValue.getValue(elCtxt);
            }
            return value;
        }

        Object escapeForLog(Object value) {
            if (value instanceof char[]) {
                return "*****";
            } else {
                return value;
            }
        }

    }

}
