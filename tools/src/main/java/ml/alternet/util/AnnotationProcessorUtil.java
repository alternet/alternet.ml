package ml.alternet.util;

import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Utilities for annotation processor.
 *
 * @author Philippe Poulard
 */
@Util
public final class AnnotationProcessorUtil {

    private AnnotationProcessorUtil() {
    }

    /**
     * Return the qualified name of a type (those that contain a $ sign for
     * nested classes).
     * 
     * @param tm
     *            Represent the class
     * @return The class name
     */
    public static String getClassName(TypeMirror tm) {
        if (tm.getKind().equals(TypeKind.DECLARED)) {
            TypeElement el = (TypeElement) ((DeclaredType) tm).asElement();
            return getClassName(el);
        } else {
            return tm.toString();
        }
    }

    /**
     * Return the qualified name of a type (those that contain a $ sign for
     * nested classes).
     * 
     * @param el
     *            Represent the class
     * @return The class name
     */
    public static String getClassName(TypeElement el) {
        if (el.getNestingKind() == NestingKind.TOP_LEVEL) {
            return el.getQualifiedName().toString();
        } else {
            return getClassName((TypeElement) el.getEnclosingElement()) + "$" + el.getSimpleName();
        }
    }

}
