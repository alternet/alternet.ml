package ml.alternet.misc;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Manages a set of mutexes unique in this context.
 *
 * <p>As mentionned in section 3.10.5 of the Java Language Spec 2.0,
 * "Literal strings within different classes in different packages
 * likewise represent references to the same String object."</p>
 * <p>This class allow to create a context in order to manage safe
 * string-based mutexes, that is to say without the inherent
 * conditions of the String class that can lead to a dead-lock.</p>
 *
 * <p>This class is not magic:</p>
 * <ul>
 * 		<li>It's up to the user to create a context per domain to avoid
 * 		ID collisions. The scope of such domain depends on the application.</li>
 * 		<li>It can't avoid dead-lock due to programming (it only avoids
 * 		the inherent possibility of dead-locks due to string usage)</li>
 * </ul>
 *
 * <p>A mutex doesn't have to be explicitly remove from this context,
 * it will be automatically removed after all references to it are
 * dropped.</p>
 *
 * @see Mutex
 *
 * @author Philippe Poulard
 */
public class MutexContext {

	// based on http://illegalargumentexception.blogspot.com/2008/04/java-synchronizing-on-transient-id.html

	private final Map<Mutex, WeakReference<Mutex>> mutexMap = new WeakHashMap<Mutex, WeakReference<Mutex>>();

	/**
	 * Return a lockable mutex.
	 *
	 * @param id The string ID.
	 *
	 * @return A synchronizable object, unique in this context.
	 */
	public Mutex getMutex(String id) {
		Mutex key = new Mutex(id);
		synchronized(mutexMap) {
			WeakReference<Mutex> ref = this.mutexMap.get(key);
			if (ref==null) {
				this.mutexMap.put(key, new WeakReference<Mutex>(key));
				return key;
			}
			Mutex mutex = ref.get();
			if (mutex==null) {
				this.mutexMap.put(key, new WeakReference<Mutex>(key));
				return key;
			}
			return mutex;
		}
	}

	/**
	 * Get the number of mutex objects being held
	 * 
	 * @return The number of items in the map
	 */
	public int getMutexCount() {
		synchronized(mutexMap) {
			return mutexMap.size();
		}
	}

}
