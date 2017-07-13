package ml.alternet.discover;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.PACKAGE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This interface contains qualifiers used in conjunction with a CDI container
 * (JSR 330).
 *
 * You are not compelled to use the discovery service with a CDI container, but
 * if you want to inject classes to lookup (specifically with a variant), the
 * annotations defined here may help.
 *
 * Even if you used a CDI container, you are not compelled to inject every
 * classes to lookup, just the ones you want to or have to inject.
 *
 * <ul>
 * <li>{@link LookupKey} generates an entry in <tt>META-INF/xservices/</tt></li>
 * <li>{@link Injection.LookupKey} qualifies an instance to inject</li>
 * <li>{@link Injection.Producer} generates a class producer for injections</li>
 * </ul>
 *
 * <h3>Usage</h3>
 *
 * <h4>Start situation (without injection)</h4>
 *
 * For example, consider a constructor :
 *
 * <pre>
 * public A(B b) {
 *     //
 * }
 * </pre>
 *
 * where B is an interface :
 *
 * <pre>
 * public interface B {
 *     //
 * }
 * </pre>
 *
 * which has several implementations :
 *
 * <pre>
 * &#064;LookupKey(forClass = B.class, variant = &quot;variant1&quot;)
 * public class B1 implements B {
 *     //
 * }
 * </pre>
 *
 * and :
 *
 * <pre>
 * &#064;LookupKey(forClass = B.class, variant = &quot;variant2&quot;)
 * public class B2 implements B {
 *     //
 * }
 * </pre>
 *
 * You want to inject the right class ? See below...
 *
 * <h4>Target situation (with injection)</h4>
 *
 * First, in the constructor (or directly in the field, up to you) one have to
 * indicates that an instance have to be injected :
 *
 * <pre>
 * &#064;javax.inject.Inject
 * public A(@Injection.LookupKey(&quot;variant1&quot;) B b) {
 *     //
 * }
 * </pre>
 *
 * Then you have to set on the interface which variant you want to produce :
 *
 * <pre>
 * &#064;Injection.Producer(variant = &quot;variant1&quot;)
 * public interface B {
 *     //
 * }
 * </pre>
 *
 * A producer class will be generated at compile time, that will perform the
 * lookup with the specified variant. At runtime, the container will inject the
 * instance supplied by the producer that matches both the expected target class
 * and the variant, if any.
 *
 * <h3>Other usage</h3>
 *
 * Sometimes, the developer doesn't own the type to inject neither their
 * implementations ; it is still possible to set the annotations on some code
 * owned by the developer, for example one of its package and specify both the
 * implementation and the target class.
 *
 * If a specific producer have to match a general-purpose variant, and have to
 * lookup for a different variant, it is also possible to specify a different
 * variant target.
 *
 * @author Philippe Poulard
 */
public interface Injection {

    /**
     * Use this qualifier to inject an instance to lookup with the discovery
     * service.
     */
    @javax.inject.Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ METHOD, FIELD, PARAMETER /* , TYPE, CONSTRUCTOR */})
    public @interface LookupKey {

        /**
         * The variant to inject, may match a producer.
         *
         * @return The variant to inject.
         *
         * @see Producer
         */
        String variant() default "";

    }

    /**
     * Generates a producer for the class or interface that will be used with
     * the lookup key qualifier. The producer will supply an instance found by
     * the discovery service.
     *
     * @see LookupKey
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ TYPE, PACKAGE })
    @Repeatable(Producers.class)
    public @interface Producer {

        /**
         * The class to produce ; by default it is the class on which this
         * annotation is set ; if this annotation is set on a package, the value
         * MUST be supplied. The discovery service will perform a lookup for
         * that class.
         *
         * @return The class to produce
         */
        Class<?> forClass() default Void.class;

        /**
         * The variant to inject by this producer, that a lookup key may match.
         *
         * @return The variant to inject
         */
        String variant() default "";

        /**
         * The variant to lookup ; if not specified, the lookup variant will be
         * the same as the variant of the qualifier lookup key. For the unlikely
         * case where the inject lookup key would have a variant, but for which
         * the producer have to lookup for an implementation without a variant,
         * fill this value with only whitespace.
         *
         * @return The variant to lookup.
         */
        String lookupVariant() default "";

    }

    /**
     * Allow to repeat a producer annotation.
     *
     * @see Producer
     *
     * @author Philippe Poulard
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    public @interface Producers {

        Producer[] value();

    }

}
