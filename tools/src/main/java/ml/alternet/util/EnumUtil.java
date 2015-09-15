package ml.alternet.util;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

import ml.alternet.misc.Thrower;

/**
 * Enum-related utilities.
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
     *       EnumUtil.replace(MyEnum.class, this, s -> s.replace('_', ' '));
     *    }
     *}</pre>
     * Usage :
     * <pre> MyEnum val = MyEnum.valueOf("VALUE 2");</pre>
     *
     * @param enumClass The enum class to patch
     * @param enumValue The instance to change
     * @param transformer Apply a transformation on the name of the instance
     */
    public static void replace(Class<? extends Enum<?>> enumClass, Object enumValue, UnaryOperator<String> transformer) {
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

}
