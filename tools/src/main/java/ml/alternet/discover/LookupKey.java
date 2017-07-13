package ml.alternet.discover;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ml.alternet.discover.gen.LookupKeyProcessor;

/**
 * Describe the lookup key for class discovery.
 *
 * Each annotation will produce an entry for the discovery service (in
 * META-INF/xservices/).
 *
 * @author Philippe Poulard
 *
 * @see LookupKeys
 * @see DiscoveryService
 * @see LookupKeyProcessor
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE })
@Repeatable(LookupKeys.class)
public @interface LookupKey {

    /**
     * By default, this annotation is set on the class to lookup, but if you
     * don't control it, you can set it elsewhere and supply the class to
     * lookup.
     *
     * @return The class to lookup.
     */
    Class<?> forClass() default Void.class;

    /**
     * The variant to lookup, if any.
     *
     * @return The variant.
     */
    String variant() default "";

    /**
     * By default, this annotation is set on the target implementation, but if
     * you don't control it or set it elsewhere (typically on the class to
     * lookup), you have to supply the target implementation.
     *
     * @return The target implementation.
     */
    Class<?> implClass() default Void.class;

    /**
     * Indicates whether this annotation should generate the default lookup ?
     *
     * @return Whether this annotation should generate the default lookup.
     */
    boolean byDefault() default true;

}
