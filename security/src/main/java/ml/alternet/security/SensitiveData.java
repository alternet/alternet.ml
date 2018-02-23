package ml.alternet.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a data as sensitive data, that is to say
 * should be destroyed after use and not remain too much
 * time in memory.
 *
 * @author Philippe Poulard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER})
public @interface SensitiveData {

}
