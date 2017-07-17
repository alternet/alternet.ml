package ml.alternet.util.gen;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation specifies how to generate a byte code factory instance.
 * The supplied byte code factory will be able to generate instances of any interface
 * without the need of implementing all the methods of that interface.
 *
 * @see ByteCodeFactoryGenerator
 *
 * @author Philippe Poulard
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.FIELD })
public @interface ByteCodeSpec {

    /**
     * The package of the byte code factory class.
     *
     * @return By default : "ml.alternet.util"
     */
    String factoryPkg() default "ml.alternet.util";

    /**
     * The class name of the byte code factory class.
     *
     * @return By default : "ByteCodeFactory$"
     */
    String factoryClassName() default "ByteCodeFactory$";

    /**
     * The parent class of the classes generated.
     *
     * @return By default : "Object.class"
     */
    Class<?> parentClass() default Object.class;

    /**
     * The name of the field that holds the singleton.
     *
     * @return By default : "SINGLETON"
     */
    String singletonName() default "SINGLETON";

    /**
     * The template used to generate the implementations of interfaces.
     *
     * @return By default : "/ml/alternet/util/gen/ByteCodeFactory$.java.template"
     */
    String template() default "/ml/alternet/util/gen/ByteCodeFactory$.java.template";

}
