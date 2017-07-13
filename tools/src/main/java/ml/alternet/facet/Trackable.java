package ml.alternet.facet;

import java.util.Optional;

import ml.alternet.misc.Position;

/**
 * A trackable object can supply informations about its position (within an
 * input stream).
 *
 * @author Philippe Poulard
 */
public interface Trackable {

    /**
     * When available, return the given position.
     *
     * @return The current position.
     *
     * @see Position#$(long, long, long)
     */
    Optional<Position> getPosition();

}
