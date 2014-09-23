package ml.alternet.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

import ml.alternet.concurrent.Mutex;
import ml.alternet.concurrent.MutexContext;

import org.testng.annotations.Test;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Fail;

public class MutexTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void getNull_Should_ThrowException() {
        MutexContext imp = new MutexContext();
        imp.getMutex(null);
    }

    @Test
    public void mutexesEquality_Should_ApplyOnValues() {
        MutexContext imp = new MutexContext();
        // an id
        String id1a = "id1";
        // same id value; different key instance
        String id1b = new String(id1a);
        // a different id
        String id2 = "id2";

        // assert inequality of String id reference values
        Assertions.assertThat(id1a).isNotSameAs(id1b);
        Assertions.assertThat(id1a).isNotSameAs(id2);

        Mutex m1a = imp.getMutex(id1a);
        System.out.println(m1a);
        Assertions.assertThat(m1a).isNotNull();
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(1);

        Mutex m1b = imp.getMutex(id1b);
        System.out.println(m1b);
        Assertions.assertThat(m1b).isNotNull();
        Assertions.assertThat(m1a).isSameAs(m1b);
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(1);

        Mutex m2 = imp.getMutex(id2);
        System.out.println(m2);
        Assertions.assertThat(m2).isNotNull();
        Assertions.assertThat(m2).isNotSameAs(m1a);
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(2);
    }

    @Test
    public void mutexContext_Should_DropUnusedKeys() {
        System.out.println("Testing for memory leaks; wait...");

        MutexContext imp = new MutexContext();

        int creationCount = 0;
        while (true) {
            if (creationCount == Integer.MAX_VALUE) {
                Assertions.fail("Memory leak");
            }

            creationCount++;
            imp.getMutex("" + creationCount);
            if (imp.getMutexCount() < creationCount) {
                // then some garbage collection has
                // removed entries from the map
                break;
            }

            // encourage the garbage collector
            if (creationCount % 10000 == 0) {
                System.out.println(creationCount);
                System.gc();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    @Test
    public void mutexContext_Should_DropUnusedLockKeys() {
        System.out.println("Testing for memory leaks; wait...");

        MutexContext imp = new MutexContext();

        int creationCount = 0;
        while (true) {
            if (creationCount == Integer.MAX_VALUE) {
                Assertions.fail("Memory leak");
            }

            creationCount++;
            ReadWriteLock rwl = imp.getReadWriteLock("" + creationCount);
            Lock wl = rwl.writeLock();
            wl.lock();
            try {
                if (imp.getMutexCount() < creationCount) {
                    // then some garbage collection has
                    // removed entries from the map
                    break;
                }
            } finally {
                wl.unlock();
            }
            // encourage the garbage collector
            if (creationCount % 10000 == 0) {
                System.out.println(creationCount);
                System.gc();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    @Test
    public void lockEquality_Should_ApplyOnValues() {
        MutexContext imp = new MutexContext();
        // an id
        String id1a = "id1";
        // same id value; different key instance
        String id1b = new String(id1a);
        // a different id
        String id2 = "id2";

        // assert inequality of String id reference values
        Assertions.assertThat(id1a).isNotSameAs(id1b);
        Assertions.assertThat(id1a).isNotSameAs(id2);

        Lock m1a = imp.getLock(id1a);
        System.out.println(m1a);
        Assertions.assertThat(m1a).isNotNull();
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(1);

        Lock m1b = imp.getLock(id1b);
        System.out.println(m1b);
        Assertions.assertThat(m1b).isNotNull();
        Assertions.assertThat(m1a).isSameAs(m1b);
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(1);

        Lock m2 = imp.getLock(id2);
        System.out.println(m2);
        Assertions.assertThat(m2).isNotNull();
        Assertions.assertThat(m2).isNotSameAs(m1a);
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(2);
    }

    @Test
    public void readWriteLockEquality_Should_ApplyOnValues() {
        MutexContext imp = new MutexContext();
        // an id
        String id1a = "id1";
        // same id value; different key instance
        String id1b = new String(id1a);
        // a different id
        String id2 = "id2";

        // assert inequality of String id reference values
        Assertions.assertThat(id1a).isNotSameAs(id1b);
        Assertions.assertThat(id1a).isNotSameAs(id2);

        ReadWriteLock m1a = imp.getReadWriteLock(id1a);
        System.out.println(m1a);
        Assertions.assertThat(m1a).isNotNull();
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(1);

        ReadWriteLock m1b = imp.getReadWriteLock(id1b);
        System.out.println(m1b);
        Assertions.assertThat(m1b).isNotNull();
        Assertions.assertThat(m1a).isSameAs(m1b);
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(1);

        ReadWriteLock m2 = imp.getReadWriteLock(id2);
        System.out.println(m2);
        Assertions.assertThat(m2).isNotNull();
        Assertions.assertThat(m2).isNotSameAs(m1a);
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(2);
    }

    @Test
    public void stampedLockEquality_Should_ApplyOnValues() {
        MutexContext imp = new MutexContext();
        // an id
        String id1a = "id1";
        // same id value; different key instance
        String id1b = new String(id1a);
        // a different id
        String id2 = "id2";

        // assert inequality of String id reference values
        Assertions.assertThat(id1a).isNotSameAs(id1b);
        Assertions.assertThat(id1a).isNotSameAs(id2);

        StampedLock m1a = imp.getStampedLock(id1a);
        System.out.println(m1a);
        Assertions.assertThat(m1a).isNotNull();
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(1);

        StampedLock m1b = imp.getStampedLock(id1b);
        System.out.println(m1b);
        Assertions.assertThat(m1b).isNotNull();
        Assertions.assertThat(m1a).isSameAs(m1b);
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(1);

        StampedLock m2 = imp.getStampedLock(id2);
        System.out.println(m2);
        Assertions.assertThat(m2).isNotNull();
        Assertions.assertThat(m2).isNotSameAs(m1a);
        Assertions.assertThat(imp.getMutexCount()).isEqualTo(2);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void lockKind_Should_RemainTheSame() {
        MutexContext imp = new MutexContext();
        // an id
        String id1a = "id1";
        // same id value; different key instance
        String id1b = new String(id1a);
        // a different id
        String id2 = "id2";

        // assert inequality of String id reference values
        Assertions.assertThat(id1a).isNotSameAs(id1b);
        Assertions.assertThat(id1a).isNotSameAs(id2);

        Lock lock = imp.getLock(id1a);
        StampedLock slock = imp.getStampedLock(id1a);

        Fail.fail("Lock " + lock + " and StampedLock " + slock + " has been returned for the same ID");
    }

}
