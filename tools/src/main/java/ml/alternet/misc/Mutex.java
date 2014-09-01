package ml.alternet.misc;

/**
 * A mutex, based on a string ID, safe for
 * synchronization for a given user context.
 * 
 * <h3>Usage</h3>
 * <pre>    MutextContext mutexContext = new MutextContext();
 *    ...
 *    
 *    String id = someObject.getCanonicalID();
 *    Mutex mutex = mutexContext.getMutex(id);
 *    synchronized(mutex) {
 *       ...
 *    }
 * </pre>
 * 
 * @see MutexContext
 * 
 * @author Philippe Poulard
 */
public class Mutex {

	private final String id;

	Mutex(String id) {
		this.id = id;
	}

	public boolean equals(Object o) {
		if (o==null) {
			return false;
		}
		if (this.getClass()==o.getClass()) {
			return this.id.equals(o.toString());
		}
		return false;
	}

	public int hashCode() {
		return this.id.hashCode();
	}

	public String toString() {
		return this.id;
	}

}
