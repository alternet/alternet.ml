package ml.alternet.util;

import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ml.alternet.misc.TodoException;

/**
 * Class-related utilities.
 *
 * @author Philippe Poulard
 */
@Util
public final class ClassUtil {

    private ClassUtil() {
    }

    /**
     * Load a class. The class loader used is those of the current thread, and
     * if the class not found, with those that was used to load this class.
     *
     * @param className
     *            The name of the class to load.
     *
     * @return The class.
     *
     * @throws ClassNotFoundException
     *             When the class was not found.
     */
    public static Class<?> load(String className) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            return Class.forName(className, true, cl);
        } catch (Exception e) {
            return Class.forName(className);
        }
    }

    /**
     * Stream of interfaces and classes of a given class hierarchy.
     *
     * @param clazz
     *            The actual class.
     *
     * @return A lazy stream over the interfaces and classes of the class and its
     *         ancestors except "Object".
     */
    public static Stream<Class<?>> getClasses(final Class<?> clazz) {
        return getClassesStream(clazz, new HashSet<Class<?>>());
    }

    private static final int FLAGS = Spliterator.CONCURRENT | Spliterator.DISTINCT | Spliterator.IMMUTABLE
            | Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SIZED;

    private static Stream<Class<?>> getClassesStream(Class<?> clazz, Set<Class<?>> interfaces) {
        if (clazz == null) {
            return Stream.empty();
        } else {
            if (clazz.isInterface()) {
                return Stream.concat(
                    Stream.of(clazz),
                    Stream.of(clazz.getInterfaces())
                        .filter(i -> interfaces.add(i))
                        .flatMap(i -> getClassesStream(i, interfaces)) // lazy by nature
                );
            } else if (clazz.getSuperclass() == null) {
                return Stream.empty(); // Object
            } else {
                // to make it lazy
                Spliterator<Class<?>> parent = new Spliterators.AbstractSpliterator<Class<?>>(Long.MAX_VALUE, FLAGS) {
                    Spliterator<Class<?>> classes;

                    @Override
                    public boolean tryAdvance(Consumer<? super Class<?>> action) {
                        if (classes == null) {
                            classes = getClassesStream(clazz.getSuperclass(), interfaces).spliterator();
                        }
                        return classes.tryAdvance(action);
                    }

                };
                return Stream.concat(
                    Stream.of(clazz),
                    Stream.concat(
                        Stream.of(clazz.getInterfaces())
                            .filter(i -> interfaces.add(i))
                            .flatMap(i -> getClassesStream(i, interfaces)),
                        StreamSupport.stream(parent, false)
                    )
                );
            }
        }
    }

}
