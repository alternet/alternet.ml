package ml.alternet.facet;

import java.util.Optional;

/**
 * A <tt>Localizable</tt> component may have an absolute URI.
 *
 * <p>
 * A localizable component may be used to resolve relative paths.
 * </p>
 *
 * <p>
 * Classes that implement this interface may have instances that are not
 * localizable ; in this case, trying to resolve relative paths is irrelevant
 * for such instances.
 * </p>
 * 
 * @param <T>
 *            The type of URI (typically String, URI, URL, File, etc).
 *
 * @author Philippe Poulard
 */
public interface Localizable<T> {

    /**
     * Return the absolute localization of this component.
     *
     * @return The URI of this component.
     */
    Optional<T> getLocation();

    /**
     * Set the localization of this component.
     *
     * @param location
     *            The absolute URI of this component.
     */
    void setLocation(T location);

}
