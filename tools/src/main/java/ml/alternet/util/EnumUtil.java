package ml.alternet.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.discover.LookupKey;
import ml.alternet.misc.Thrower;

/**
 * Enum-related utilities.
 *
 * <h1>Java 9 usage</h1>
 * Ensure to import <code>sun.reflect.*</code> classes in your module :
 * <pre>module foo.bar {
 *    requires jdk.unsupported;
 *}</pre>
 *
 * @author Philippe Poulard
 */
@Util
public class EnumUtil {

    /**
     * Replace the value of an enum instance by another value.
     *
     * Helpful for enum values that are made of characters that
     * are not Java names.
     *
     * <h3>Example</h3>
     * An enum type made of values that contains a space.
     *
     * <pre>public enum MyEnum {
     *    VALUE_1,
     *    VALUE_2,
     *    VALUE_3;
     *
     *    MyEnum() {
     *        // replace the "_" in the name by a " "
     *       EnumUtil.replace(this, s -&gt; s.replace('_', ' '));
     *    }
     *}</pre>
     * Usage :
     * <pre> MyEnum val = MyEnum.valueOf("VALUE 2");</pre>
     *
     * @param enumValue The instance to change
     * @param transformer Apply a transformation on the name of the instance
     */
    public static void replace(Object enumValue, UnaryOperator<String> transformer) {
        Class<? extends Enum<?>> enumClass = ECF.getEnumClass();
        replace(enumClass, enumValue, transformer);
    }

    /**
     * Replace the value of an enum instance by another value.
     *
     * Helpful for enum values that are made of characters that
     * are not Java names.
     *
     * @param enumClass The enum class to patch
     * @param enumValue The instance to change
     * @param transformer Apply a transformation on the name of the instance
     */
    private static void replace(Class<? extends Enum<?>> enumClass, Object enumValue,
            UnaryOperator<String> transformer)
    {
        try {
            Field fieldName = enumClass.getSuperclass().getDeclaredField("name");
            AccessController.doPrivileged((PrivilegedAction<Void>) (() -> {
                fieldName.setAccessible(true);
                return null;
            }));
            String value = (String) fieldName.get(enumValue);
            value = transformer.apply(value);
            fieldName.set(enumValue, value);
            AccessController.doPrivileged((PrivilegedAction<Void>) (() -> {
                fieldName.setAccessible(false);
                return null;
            }));
        } catch (Exception e) {
            Logger.getLogger(enumClass.getName())
                .severe("Unable to access to \"name\" field of class " + enumClass.getName());
            Thrower.doThrow(e);
        }
    }

    /**
     * Replace the ordinal of an enum instance by another ordinal.
     *
     * @param enumValue The instance to change
     * @param transformer Apply a transformation on the ordinal of the instance
     */
    public static void reorder(Object enumValue, UnaryOperator<Integer> transformer) {
        Class<? extends Enum<?>> enumClass = ECF.getEnumClass();
        reorder(enumClass, enumValue, transformer);
    }

    /**
     * Replace the ordinal of an enum instance by another ordinal.
     *
     * @param enumClass The enum class to patch
     * @param enumValue The instance to change
     * @param transformer Apply a transformation on the ordinal of the instance
     */
    private static void reorder(Class<? extends Enum<?>> enumClass, Object enumValue,
            UnaryOperator<Integer> transformer)
    {
        try {
            Field fieldName = enumClass.getSuperclass().getDeclaredField("ordinal");
            AccessController.doPrivileged((PrivilegedAction<Void>) (() -> {
                fieldName.setAccessible(true);
                return null;
            }));
            int value = (int) fieldName.get(enumValue);
            value = transformer.apply(value);
            fieldName.set(enumValue, value);
            AccessController.doPrivileged((PrivilegedAction<Void>) (() -> {
                fieldName.setAccessible(false);
                return null;
            }));
        } catch (Exception e) {
            Logger.getLogger(enumClass.getName())
                .severe("Unable to access to \"ordinal\" field of class " + enumClass.getName());
            Thrower.doThrow(e);
        }
    }

    /**
     * Interchangeable constructor for Enum values.
     *
     * @see EnumUtil#extend(Class, Class)
     *
     * @author Philippe Poulard
     */
    public interface EnumConstruct {

        /**
         * Initialize this {@code EnumConstruct}
         *
         * @param baseEnumClass The base enum class, maybe used as argument in the constructor of the target enum class.
         * @param targetEnumClass The target enum class.
         *
         * @throws NoSuchMethodException When an error occurs
         * @throws SecurityException When an error occurs
         * @throws IllegalAccessException When an error occurs
         * @throws IllegalArgumentException When an error occurs
         * @throws InvocationTargetException When an error occurs
         */
        void init(Class<? extends Enum<?>> baseEnumClass, Class<? extends Enum<?>> targetEnumClass)
            throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException;

        /**
         * Build an enum value.
         *
         * @param name The name of the enum value.
         * @param i The order.
         * @param baseValue The base value it is made of, or {@code null}
         *
         * @return A new enum value.
         */
        Enum<?> build(String name, int i, Enum<?> baseValue);

        /**
         * Create a new instance of {@code EnumConstruct}
         *
         * @return A new instance of {@code EnumConstruct}
         *
         * @see DiscoveryService#newInstance(Class)
         *
         * @throws ClassNotFoundException When no implementation is found.
         * @throws InstantiationException When no implementation is found.
         * @throws IllegalAccessException When no implementation is found.
         */
        static EnumConstruct $() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
            return DiscoveryService.newInstance(EnumConstruct.class);
        }

        /**
         * Default implementation of EnumConstruct that relies on {@code sun.reflect}
         *
         * @author Philippe Poulard
         */
        @LookupKey(forClass = EnumConstruct.class, byDefault = true)
        class SunEnumConstruct implements EnumConstruct {

            @SuppressWarnings("restriction")
            sun.reflect.ConstructorAccessor ca;
            boolean withEnumParam = true;

            Constructor<?> getConstructor(Class<? extends Enum<?>> baseEnumClass,
                Class<? extends Enum<?>> targetEnumClass) throws NoSuchMethodException, SecurityException
            {
                try {
                    // looking for constructor : TargetEnumClass(BaseEnumClass base)
                    // that is to say with enum looking for :
                    //                           TargetEnumClass(String name, int order, BaseEnumClass base)
                    return targetEnumClass.getDeclaredConstructor(String.class, int.class, baseEnumClass);
                } catch (NoSuchMethodException e) {
                    this.withEnumParam = false;
                    // looking for constructor : TargetEnumClass()
                    // that is to say with enum looking for : TargetEnumClass(String name, int order)
                    return targetEnumClass.getDeclaredConstructor(String.class, int.class);
                }
            }

            @SuppressWarnings("restriction")
            @Override
            public void init(Class<? extends Enum<?>> baseEnumClass, Class<? extends Enum<?>> targetEnumClass)
                throws NoSuchMethodException, SecurityException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException
            {
                Constructor<?> c = getConstructor(baseEnumClass, targetEnumClass);
                AccessController.doPrivileged((PrivilegedAction<Void>) (() -> {
                    c.setAccessible(true);
                    return null;
                }));
                Method acquireConstructorAccessor = Constructor.class.getDeclaredMethod("acquireConstructorAccessor");
                acquireConstructorAccessor.setAccessible(true);
                // we need this because Constructor won't let us construct anything
                this.ca = (sun.reflect.ConstructorAccessor) acquireConstructorAccessor.invoke(c);
                AccessController.doPrivileged((PrivilegedAction<Void>) (() -> {
                    c.setAccessible(false);
                    return null;
                }));
            }

            @SuppressWarnings("restriction")
            @Override
            public Enum<?> build(String name, int i, Enum<?> baseValue) {
                Object[] params = new Object[2 + (this.withEnumParam ? 1 : 0)];
                params[0] = name;
                params[1] = i;
                if (this.withEnumParam) {
                    params[2] = baseValue;
                }
                return (Enum<?>) Thrower.safeCall(() -> this.ca.newInstance(params));
            }
        }

    }

    /**
     * Extend an enum class with the values of another enum. If the base enum
     * expose some logic in an interface, the target enum should have a
     * constructor with as its unique argument an instance of the base enum,
     * in order to cast them properly ; otherwise that logic will be lost.
     * Existing values can be copied either with the aforementioned constructor,
     * or with a zero-arg constructor.
     *
     * @param baseEnumClass The values to add.
     */
    public static void extend(Class<? extends Enum<?>> baseEnumClass) {
        Class<? extends Enum<?>> targetEnumClass = ECF.getEnumClass();
        extend(baseEnumClass, targetEnumClass);
    }

    // allow to find the caller enum class
    private static class EnumClassFinder extends SecurityManager  {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<? extends Enum<?>> getEnumClass() {
            Class[] classes = getClassContext();
            // call stack :
            // class ml.alternet.util.EnumUtil$EnumClassFinder,
            // class ml.alternet.util.EnumUtil,
            // enum org.example.YourEnum,
            // ...]
            return classes[2];
        };
    }
    private static final EnumClassFinder ECF = new EnumClassFinder();

    /**
     * Extend an enum class with the values of another enum. If the base enum
     * expose some logic in an interface, the target enum should have a
     * constructor with as its unique argument an instance of the base enum,
     * in order to cast them properly ; otherwise that logic will be lost.
     * Existing values can be copied either with the aforementioned constructor,
     * or with a zero-arg constructor.
     *
     * @param baseEnumClass The values to add.
     * @param targetEnumClass The target enum to extend.
     */
    private static void extend(Class<? extends Enum<?>> baseEnumClass, Class<? extends Enum<?>> targetEnumClass) {
        try {
            List<Enum<?>> baseValues = new LinkedList<>( // need to remove items (see below)
                Arrays.asList(baseEnumClass.getEnumConstants())
            );
            List<Enum<?>> targetValues = Arrays.asList(targetEnumClass.getEnumConstants());

            EnumConstruct ec = EnumConstruct.$();
            ec.init(baseEnumClass, targetEnumClass);

            int i[] = { 0 };
            List<Enum<?>> newValues = new ArrayList<>();
            // copy the target values, but if one is found within base values, remove it
            targetValues.stream().forEach(t -> {
                for (Iterator<Enum<?>> baseIt = baseValues.iterator() ; baseIt.hasNext() ; ) {
                    Enum<?> b = baseIt.next();
                    if (b.name().equals(t.name())) {
                        baseIt.remove();
                        break;
                    }
                }
                newValues.add(t);
                i[0]++;
            });
            // then copy the base values
            baseValues.stream().forEach(b -> {
                newValues.add(Thrower.safeCall(() -> ec.build(b.name(), i[0]++, b)));
            });

            // finally, replace the enum values in the class
            Field modifField = Field.class.getDeclaredField("modifiers");
            modifField.setAccessible(true);
            Field valuesField;
            try {
                valuesField = targetEnumClass.getDeclaredField("$VALUES");
            } catch (NoSuchFieldException e) {
                // I don't know why I need this one (well... I found it)
                valuesField = targetEnumClass.getDeclaredField("ENUM$VALUES");
            }
            valuesField.setAccessible(true);
            final int modifiers = valuesField.getModifiers();
            modifField.setInt(valuesField, modifiers & ~Modifier.FINAL);
            valuesField.setAccessible(true);
            Object[] newValuesArr = newValues.toArray((Object[]) Array.newInstance(targetEnumClass, newValues.size()));
            valuesField.set(targetEnumClass, newValuesArr);
            modifField.setInt(valuesField, modifiers);

            // reset internal cache
            Field enumConstants = Class.class.getDeclaredField("enumConstants");
            AccessController.doPrivileged((PrivilegedAction<Void>) (() -> {
                enumConstants.setAccessible(true);
                return null;
            }));
            enumConstants.set(targetEnumClass, null); // it will be recomputed on demand
            AccessController.doPrivileged((PrivilegedAction<Void>) (() -> {
                enumConstants.setAccessible(false);
                return null;
            }));
        } catch (Exception e) {
            Logger.getLogger(targetEnumClass.getName())
                .severe("Unable to access to internal fields of class " + targetEnumClass.getName());
            Thrower.doThrow(e);
        }
    }

}
