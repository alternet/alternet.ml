package ml.alternet.misc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that allow to access members with restricted access.
 *
 * @author Philippe Poulard
 */
public class Invoker {

    private static final Logger LOGGER = Logger.getLogger(Invoker.class.getName());

    /**
     * Unused and unusable constructor.
     */
    private Invoker() { }

    /**
     * Get a field of a class or one of its parent class.
     *
     * @param clazz
     *            The class.
     * @param field
     *            The name of the field.
     *
     * @return The non-<code>null</code> field of the class.
     *
     * @throws NoSuchFieldException
     *             If the field doesn't exist in the class nor in the ancestor
     *             classes.
     */
    public static Field getField(Class<?> clazz, String field) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(field);
        } catch (NoSuchFieldException nsfe) {
            Class<?> parent = clazz.getSuperclass();
            if (parent == null) {
                throw nsfe;
            } else {
                try {
                    return getField(parent, field);
                } catch (NoSuchFieldException nestedNsfe) {
                    throw nsfe;
                }
            }
        }
    }

    /**
     * Set a value to a field of an object.
     *
     * <p>
     * The operation succeed even if the field is defined by a superclass of the
     * target object.
     * </p>
     *
     * @param target
     *            The target object.
     * @param field
     *            The name of the field.
     * @param value
     *            The value to set.
     *
     * @throws NoSuchFieldException
     *             When the field to set is missing in the target object.
     * @throws IllegalArgumentException
     *             When the value to set is not of the type expected.
     * @throws IllegalAccessException
     *             When the field is not visible and a security manager doesn't
     *             allow to change its visibility.
     */
    public static void set(Object target, String field, Object value) throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException
    {
        Field f = getField(target.getClass(), field);
        try {
            f.setAccessible(true);
        } catch (SecurityException se) {
            LOGGER.log(Level.INFO, se.getMessage(), se);
        } // perhaps there is enough visibility on the field
        f.set(target, value);
    }

    /**
     * Get the value of a field.
     *
     * <p>
     * The operation succeed even if the field is defined by a superclass of the
     * target object.
     * </p>
     *
     * @param target
     *            The target object.
     * @param field
     *            The name of the field.
     * @param <T>
     *            The type of the field.
     *
     * @return The value of the field.
     *
     * @throws NoSuchFieldException
     *             When the field to get is missing in the target object.
     * @throws IllegalAccessException
     *             When the field is not visible and a security manager doesn't
     *             allow to change its visibility.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Object target, String field) throws NoSuchFieldException, IllegalAccessException {
        Field f = getField(target.getClass(), field);
        try {
            f.setAccessible(true);
        } catch (SecurityException se) {
            LOGGER.log(Level.INFO, se.getMessage(), se);
        } // perhaps there is enough visibility on the field
        return (T) f.get(target);
    }

    /**
     * Get a method of a class or one of its parent class.
     *
     * @param clazz
     *            The class.
     * @param method
     *            The name of the method.
     * @param paramTypes
     *            The types of the parameters of the method, can be
     *            <code>null</code>.
     *
     * @return The non-<code>null</code> method of the class.
     *
     * @throws NoSuchMethodException
     *             If the method doesn't exist in the class nor in the ancestor
     *             classes.
     */
    public static Method getMethod(Class<?> clazz, String method, Class<?>[] paramTypes) throws NoSuchMethodException {
        try {
            return clazz.getDeclaredMethod(method, paramTypes);
        } catch (NoSuchMethodException nsme) {
            Class<?> parent = clazz.getSuperclass();
            if (parent == null) {
                throw nsme;
            } else {
                try {
                    return getMethod(parent, method, paramTypes);
                } catch (NoSuchMethodException nestedNsme) {
                    throw nsme;
                }
            }
        }
    }

    /**
     * Get a method of a class or one of its parent class.
     *
     * @param clazz
     *            The class.
     * @param method
     *            The name of the method.
     *
     * @return The non-<code>null</code> method of the class that has the name
     *         given and accept no parameter.
     *
     * @throws NoSuchMethodException
     *             If the method doesn't exist in the class nor in the ancestor
     *             classes.
     */
    public static Method getMethod(Class<?> clazz, String method) throws NoSuchMethodException {
        return getMethod(clazz, method, null);
    }

    /**
     * Call a method without arguments.
     *
     * <p>
     * The call succeed even if the method is defined by a superclass of the
     * target object.
     * </p>
     *
     * @param target
     *            The target object.
     * @param method
     *            The name of the method to call.
     * @param <T>
     *            The type of the result to return.
     *
     * @return The result of the call.
     *
     * @throws NoSuchMethodException
     *             When the method to call is missing in the target object.
     * @throws IllegalAccessException
     *             When the method is not visible and a security manager doesn't
     *             allow to change its visibility.
     * @throws InvocationTargetException
     *             If the underlying method throws an exception.
     */
    @SuppressWarnings("unchecked")
    public static <T> T call(Object target, String method) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException
    {
        Method m = getMethod(target.getClass(), method);
        try {
            m.setAccessible(true);
        } catch (SecurityException se) {
            LOGGER.log(Level.INFO, se.getMessage(), se);
        } // perhaps there is enough visibility on the method
        return (T) m.invoke(target, (Object[]) null);
    }

    /**
     * Call a method.
     *
     * <p>
     * The call succeed even if the method is defined by a superclass of the
     * target object.
     * </p>
     *
     * @param target
     *            The target object.
     * @param method
     *            The name of the method to call.
     * @param args
     *            An array of arguments.
     * @param paramTypes
     *            An array of types.
     * @param <T>
     *            The type of the result to return.
     *
     * @return The result of the call.
     *
     * @throws NoSuchMethodException
     *             When the method to call is missing in the target object.
     * @throws IllegalArgumentException
     *             When one of the arguments is not of the type expected.
     * @throws IllegalAccessException
     *             When the method is not visible and a security manager doesn't
     *             allow to change the visibility.
     * @throws InvocationTargetException
     *             If the underlying method throws an exception.
     */
    @SuppressWarnings("unchecked")
    public static <T> T call(Object target, String method, Object[] args, Class<?>[] paramTypes)
            throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        Method m = getMethod(target.getClass(), method, paramTypes);
        try {
            m.setAccessible(true);
        } catch (SecurityException se) {
            LOGGER.log(Level.INFO, se.getMessage(), se);
        } // perhaps there is enough visibility on the method
        return (T) m.invoke(target, args);
    }

}
