package ml.alternet.concurrent;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

/**
 * Manages a set of mutexes unique in this context ; also allows to get a mutex
 * in the shape of a lock.
 *
 * <p>
 * As mentionned in section 3.10.5 of the Java Language Spec 2.0, "Literal
 * strings within different classes in different packages likewise represent
 * references to the same String object."
 * </p>
 * <p>
 * This class allow to create a context in order to manage safe string-based
 * mutexes, that is to say without the inherent conditions of the String class
 * that can lead to a dead-lock.
 * </p>
 *
 * <p>
 * This class is not magic:
 * </p>
 * <ul>
 * <li>It's up to the user to create a context per domain to avoid ID
 * collisions. The scope of such domain depends on the application.</li>
 * <li>It can't avoid dead-lock due to programming (it only avoids the inherent
 * possibility of dead-locks due to string usage)</li>
 * </ul>
 *
 * <p>
 * A mutex or a lock doesn't have to be explicitly remove from this context, it
 * will be automatically removed after all references to it are dropped.
 * </p>
 *
 * @see Mutex
 *
 * @author Philippe Poulard
 */
public class MutexContext {

    // based on
    // http://illegalargumentexception.blogspot.com/2008/04/java-synchronizing-on-transient-id.html

    private final Map<LockableMutex<?>, WeakReference<LockableMutex<?>>> mutexMap = new WeakHashMap<LockableMutex<?>, WeakReference<LockableMutex<?>>>();
    private final StampedLock lock = new StampedLock();

    private LockableMutex<?> getLockableMutex(String id) {
        @SuppressWarnings("rawtypes")
        LockableMutex key = new LockableMutex(id);

        WeakReference<LockableMutex<?>> ref;
        LockableMutex<?> mutex;

        // try a read without contention
        long stamp = this.lock.tryOptimisticRead();
        if (stamp != 0L) { // no Write Lock right now
            ref = this.mutexMap.get(key);
            if (ref != null) {
                mutex = ref.get();
                if (mutex != null) {
                    return mutex; // we win
                }
            }
        }

        // no entry found or Write in progress (or even achieved so far)
        stamp = this.lock.readLock();
        try {
            while (true) {
                ref = this.mutexMap.get(key);
                if (ref != null) {
                    mutex = ref.get();
                    if (mutex != null) {
                        return mutex;
                    }
                }
                long ws = this.lock.tryConvertToWriteLock(stamp);
                if (ws == 0L) {
                    this.lock.unlockRead(stamp);
                    // if there is a race...
                    stamp = this.lock.writeLock();
                    // ...the first that acquires the WL will store the weak reference.
                    // At the next loop, the next ones will find it
                } else {
                    // the thread that acquires the WL goes here
                    stamp = ws;
                    this.mutexMap.put(key, new WeakReference<LockableMutex<?>>(key));
                    return key;
                }
            }
        } finally {
            this.lock.unlock(stamp);
        }
    }

    // L is Lock, ReadWriteLock, or StampedLock
    private static class LockableMutex<L> extends Mutex {
        // volatile avoid "out-of-order rewrites"
        // with "double-checked locking" idiom
        volatile L lock;

        LockableMutex(String id) {
            super(id);
        }

        L getLock(Function<Mutex, L> supplier) {
            // local : used for reduce read calls to the volatile ref :
            // lock is read only twice instead of 3 times
            // (the return statement would have count for 1)
            L local = this.lock;
            if (local == null) {
                synchronized (this) {
                    local = this.lock;
                    if (local == null) {
                        local = supplier.apply(this);
                        lock = local;
                    }
                }
            }
            return local;
        }

        static final Function<Mutex, Lock> LOCK_SUPPLIER = new Function<Mutex, Lock>() {
            @Override
            public Lock apply(final Mutex mutex) {
                return new ReentrantLock() {
                    private static final long serialVersionUID = 7427115700635757451L;
                    public String toString() {
                        return mutex.toString();
                    }
                };
            }
        };

        static final Function<Mutex, ReadWriteLock> READWRITE_LOCK_SUPPLIER = new Function<Mutex, ReadWriteLock>() {
            @Override
            public ReadWriteLock apply(final Mutex mutex) {
                return new ReentrantReadWriteLock() {
                    private static final long serialVersionUID = -1093907045866881049L;
                    public String toString() {
                        return mutex.toString();
                    }
                };
            }
        };

        static final Function<Mutex, StampedLock> STAMPED_LOCK_SUPPLIER = new Function<Mutex, StampedLock>() {
            @Override
            public StampedLock apply(final Mutex mutex) {
                return new StampedLock() {
                    private static final long serialVersionUID = -8226412869978768092L;
                    public String toString() {
                        return mutex.toString();
                    }
                };
            }
        };
    }

    /**
     * Return a lockable mutex.
     *
     * @param id The string ID.
     *
     * @return A synchronizable object, unique in this context.
     */
    public Mutex getMutex(String id) {
        return getLockableMutex(id);
    }

    /**
     * Return a lock that encapsulates a mutex.
     *
     * @param id The string ID.
     *
     * @return A unique lock for the given ID in this context.
     * 
     * @throws IllegalStateException
     *             If another kind of lock has been already requested for this
     *             ID
     */
    public Lock getLock(String id) {
        @SuppressWarnings("unchecked")
        LockableMutex<Lock> mutex = (LockableMutex<Lock>) getLockableMutex(id);
        try {
            return mutex.getLock(LockableMutex.LOCK_SUPPLIER);
        } catch (ClassCastException e) {
            throw new IllegalStateException(getBadLockTypeMessage(id, mutex));
        }
    }

    /**
     * Return a RW lock that encapsulates a mutex.
     *
     * @param id The string ID.
     *
     * @return A unique RW lock for the given ID in this context.
     * 
     * @throws IllegalStateException
     *             If another kind of lock has been already requested for this ID
     */
    public ReadWriteLock getReadWriteLock(String id) {
        @SuppressWarnings("unchecked")
        LockableMutex<ReadWriteLock> mutex = (LockableMutex<ReadWriteLock>) getLockableMutex(id);
        try {
            return mutex.getLock(LockableMutex.READWRITE_LOCK_SUPPLIER);
        } catch (ClassCastException e) {
            throw new IllegalStateException(getBadLockTypeMessage(id, mutex));
        }
    }

    /**
     * Return a stamped lock that encapsulates a mutex.
     *
     * @param id
     *            The string ID.
     *
     * @return A unique stamped lock for the given ID in this context.
     * 
     * @throws IllegalStateException
     *             If another kind of lock has been already requested for this
     *             ID
     */
    public StampedLock getStampedLock(String id) {
        @SuppressWarnings("unchecked")
        LockableMutex<StampedLock> mutex = (LockableMutex<StampedLock>) getLockableMutex(id);
        try {
            return mutex.getLock(LockableMutex.STAMPED_LOCK_SUPPLIER);
        } catch (ClassCastException e) {
            throw new IllegalStateException(getBadLockTypeMessage(id, mutex));
        }
    }

    private String getBadLockTypeMessage(String id, LockableMutex<? extends Object> mutex) {
        return "An existing lock exist for ID \"" + id + "\" but with a different type : " + getLockType(mutex);
    }

    private String getLockType(LockableMutex<? extends Object> mutex) {
        return mutex.lock.getClass().getSuperclass().getCanonicalName();
        // parent class is java.util.concurrent.locks.ReentrantLock or Lock or
        // StampedLock
    }

    /**
     * Get the number of mutex objects being held
     * 
     * @return The number of items in the map
     */
    public int getMutexCount() {
        long stamp = this.lock.tryOptimisticRead();
        int size = mutexMap.size();
        if (!this.lock.validate(stamp)) {
            stamp = this.lock.readLock();
            try {
                size = mutexMap.size();
            } finally {
                this.lock.unlockRead(stamp);
            }
        }
        return size;
    }

}
