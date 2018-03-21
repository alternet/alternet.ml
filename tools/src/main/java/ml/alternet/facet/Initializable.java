package ml.alternet.facet;

/**
 * An object can be initialized.
 *
 * @param <T> The type of the initialized object.
 *
 * @author Philippe Poulard
 */
public interface Initializable<T> {

    /**
     * Perform an initialization.
     *
     * @return A value.
     */
    T init();

}
