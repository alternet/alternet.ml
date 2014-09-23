package ml.alternet.facet;

/**
 * A <tt>Pluggable</tt> object can be notified that it has been plugged.
 *
 * <p>
 * If several connectors can be plugged, the method defined by this interface
 * indicates that all connectors have been plugged.
 * </p>
 *
 * @author Philippe Poulard
 */
public interface Pluggable {

    /**
     * Notify that this pluggable object has been fully connected.
     */
    void connected();

}
