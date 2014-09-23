package ml.alternet.discover;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allow to repeat a lookup key annotation.
 * 
 * @see LookupKey
 * 
 * @author Philippe Poulard
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE })
public @interface LookupKeys {
    
    LookupKey[] value();

}
