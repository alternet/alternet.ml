package ml.alternet.properties;

/**
 * A class that extends this interface allow to use
 * the $() method for key lookup.
 *
 * @author Philippe Poulard
 */
public interface $ {

    /**
     * Return the value bound to the given key in the
     * context of this object.
     *
     * This method should not be override.
     *
     * @param key The key to lookup
     * @param <T> The return type
     * @return The value.
     */
    default <T> T $(String key) {
        return Binder.lookup(this, key);
    }

}