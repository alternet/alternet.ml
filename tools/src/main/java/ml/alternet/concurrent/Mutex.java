package ml.alternet.concurrent;

/**
 * A mutex, based on a string ID, safe for synchronization for a given user
 * context.
 * 
 * <h3>Usage</h3>
 * 
 * <pre>
 *    MutextContext mutexContext = new MutextContext();
 *    ...
 *    
 *    String id = someObject.getCanonicalID();
 *    Mutex mutex = mutexContext.getMutex(id);
 *    synchronized(mutex) {
 *       ...
 *    }
 * </pre>
 * 
 * Instead of synchronizing on a <tt>Mutex</tt> it is also possible to get a
 * lock (exist in several flavours)
 * 
 * @see MutexContext
 * @see MutexContext#getLock(String)
 * @see MutexContext#getReadWriteLock(String)
 * @see MutexContext#getStampedLock(String)
 * 
 * @author Philippe Poulard
 */
public class Mutex {

    private final String id;

    Mutex(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this.getClass() == o.getClass()) {
            return this.id.equals(o.toString());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id;
    }

}
