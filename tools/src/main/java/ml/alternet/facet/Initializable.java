package ml.alternet.facet;

/**
 * An object can be initialized.
 *
 * @author Philippe Poulard
 */
public interface Initializable {

    /**
     * Perform an initialization.
     *
     * @return A value.
     *
     * @param <T> The return type.
     */
    <T> T init();

}
