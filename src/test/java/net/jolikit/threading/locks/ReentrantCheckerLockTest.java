/*
 * Copyright 2019-2020 Jeff Hain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jolikit.threading.locks;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import net.jolikit.lang.RethrowException;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.ConcUnit;
import junit.framework.TestCase;

public class ReentrantCheckerLockTest extends TestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final ConcUnit cu = new ConcUnit();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_ReentrantCheckerLock() {
        final ReentrantCheckerLock lock = new ReentrantCheckerLock();
        assertEquals(null,lock.getName());

        useLock(lock);
    }

    public void test_ReentrantCheckerLock_String() {
        for (String name : new String[]{null,"myName"}) {
            final ReentrantCheckerLock lock = new ReentrantCheckerLock(name);
            assertEquals(name,lock.getName());

            useLock(lock);
        }
    }

    public void test_ReentrantCheckerLock_boolean() {
        for (boolean fair : new boolean[]{false,true}) {
            final ReentrantCheckerLock lock = new ReentrantCheckerLock(fair);
            assertEquals(fair,lock.isFair());

            useLock(lock);
        }
    }

    public void test_ReentrantCheckerLock_String_boolean_InterfaceCheckerLock() {
        for (String name : new String[]{null,"myName"}) {
            for (boolean fair : new boolean[]{false,true}) {
                for (int nbrOfSmallerLocks : new int[]{0,1,2}) {
                    ArrayList<InterfaceCheckerLock> smallerLocksList = new ArrayList<InterfaceCheckerLock>();
                    for (int i = 0; i < nbrOfSmallerLocks; i++) {
                        smallerLocksList.add(new ReentrantCheckerLock());
                    }
                    final ReentrantCheckerLock lock = new ReentrantCheckerLock(
                            name,
                            fair,
                            smallerLocksList.toArray(new InterfaceCheckerLock[nbrOfSmallerLocks]));
                    assertEquals(name,lock.getName());
                    assertEquals(fair,lock.isFair());
                    
                    if (nbrOfSmallerLocks != 0) {
                        // Nullifying and checking twice, to check array is new.
                        for (int k = 0; k < 2; k++) {
                            final InterfaceCheckerLock[] resSmallerLocks = lock.getSmallerLockArr();
                            for (int i = 0; i < nbrOfSmallerLocks; i++) {
                                assertSame(smallerLocksList.get(i), resSmallerLocks[i]);
                                resSmallerLocks[i] = null;
                            }
                        }
                    } else {
                        assertEquals(0, lock.getSmallerLockArr().length);
                    }

                    useLock(lock);
                }
            }
        }
    }

    public void test_toString() {
        for (String name : new String[]{null,"myName"}) {
            for (boolean fair : new boolean[]{false,true}) {
                for (int nbrOfSmallerLocks : new int[]{0,1,2}) {
                    ArrayList<InterfaceCheckerLock> smallerLocksList = new ArrayList<InterfaceCheckerLock>();
                    for (int i = 0; i < nbrOfSmallerLocks; i++) {
                        smallerLocksList.add(new ReentrantCheckerLock());
                    }
                    final ReentrantCheckerLock lock = new ReentrantCheckerLock(
                            name,
                            fair,
                            smallerLocksList.toArray(new InterfaceCheckerLock[nbrOfSmallerLocks]));
                    for (boolean locked : new boolean[]{false,true}) {
                        final String string;
                        if (locked) {
                            lock.lock();
                            try {
                                string = lock.toString();
                            } finally {
                                lock.unlock();
                            }
                        } else {
                            string = lock.toString();
                        }
                        String expected =
                                lock.getClass().getName() + "@" + Integer.toHexString(lock.hashCode())
                                + (locked ? "[Locked by thread " + Thread.currentThread().getName() + "]" : "[Unlocked]");
                        if (name != null) {
                            expected += "["+name+"]";
                        }
                        if (nbrOfSmallerLocks != 0) {
                            expected += "[smallerLocks={";
                            for (int i = 0; i < nbrOfSmallerLocks; i++) {
                                final InterfaceCheckerLock smallerLock = smallerLocksList.get(i);
                                if (i != 0) {
                                    expected += ",";
                                }
                                final String littleString =
                                    smallerLock.getClass().getName()
                                    + "@"
                                    + Integer.toHexString(smallerLock.hashCode());
                                expected += littleString;
                            }
                            expected += "}]";
                        }
                        assertEquals(expected,string);
                    }
                }
            }
        }
    }

    public void test_getName() {
        // Already covered in constructors tests.
    }

    public void test_getSmallerLocks() {
        // Already covered in constructors tests.
    }

    public void test_getOwnerBestEffort() {
        // Already covered in constructors tests (usage test).
    }

    public void test_lockMethods() {
        for (int n=1;n<=2;n++) {
            final ReentrantCheckerLock[] smallerLocks = new ReentrantCheckerLock[n];
            for (int i = 0; i < n; i++) {
                smallerLocks[i] = new ReentrantCheckerLock();
            }
            final ReentrantCheckerLock lock = new ReentrantCheckerLock(null,false,smallerLocks);

            for (int lockType=0;lockType<=3;lockType++) {

                /*
                 * Current thread holding smaller lock, but also already holding lock.
                 */

                for (int i = 0; i < n; i++) {
                    lock.lock();
                    try {
                        smallerLocks[i].lock();
                        try {
                            lock(lock, lockType);
                            try {
                            } finally {
                                lock.unlock();
                            }
                        } finally {
                            smallerLocks[i].unlock();
                        }
                    } finally {
                        lock.unlock();
                    }
                }

                /*
                 * Current thread holding smaller lock, and not already holding lock.
                 */

                for (int i = 0; i < n; i++) {
                    smallerLocks[i].lock();
                    try {
                        try {
                            lock(lock, lockType);
                            fail();
                        } catch (IllegalStateException e) {
                            // ok
                        }
                    } finally {
                        smallerLocks[i].unlock();
                    }
                }
            }
        }
    }

    public void test_checkNotLocked() {
        final ReentrantCheckerLock lock = new ReentrantCheckerLock();

        @SuppressWarnings("unused")
        boolean held;
        @SuppressWarnings("unused")
        boolean byCurrentThread;

        /*
         * Not held.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                cu.assertTrue(lock.checkNotLocked());
            }
        }, lock, held = false, byCurrentThread = false);

        /*
         * Held by current thread.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.checkNotLocked();
                    cu.fail();
                } catch (IllegalStateException e) {
                    // ok
                }
            }
        }, lock, held = true, byCurrentThread = true);

        /*
         * Held by another thread.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.checkNotLocked();
                    cu.fail();
                } catch (IllegalStateException e) {
                    // ok
                }
            }
        }, lock, held = true, byCurrentThread = false);
    }

    public void test_checkNotHeldByAnotherThread() {
        final ReentrantCheckerLock lock = new ReentrantCheckerLock();

        @SuppressWarnings("unused")
        boolean held;
        @SuppressWarnings("unused")
        boolean byCurrentThread;

        /*
         * not held
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                cu.assertTrue(lock.checkNotHeldByAnotherThread());
            }
        }, lock, held = false, byCurrentThread = false);

        /*
         * Held by current thread.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                cu.assertTrue(lock.checkNotHeldByAnotherThread());
            }
        }, lock, held = true, byCurrentThread = true);

        /*
         * Held by another thread.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.checkNotHeldByAnotherThread();
                    cu.fail();
                } catch (ConcurrentModificationException e) {
                    // ok
                }
            }
        }, lock, held = true, byCurrentThread = false);
    }

    public void test_checkHeldByCurrentThread() {
        final ReentrantCheckerLock lock = new ReentrantCheckerLock();

        @SuppressWarnings("unused")
        boolean held;
        @SuppressWarnings("unused")
        boolean byCurrentThread;

        /*
         * Not held.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.checkHeldByCurrentThread();
                    cu.fail();
                } catch (ConcurrentModificationException e) {
                    // ok
                }
            }
        }, lock, held = false, byCurrentThread = false);

        /*
         * Held by current thread.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                cu.assertTrue(lock.checkHeldByCurrentThread());
            }
        }, lock, held = true, byCurrentThread = true);

        /*
         * Held by another thread.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.checkHeldByCurrentThread();
                    cu.fail();
                } catch (ConcurrentModificationException e) {
                    // ok
                }
            }
        }, lock, held = true, byCurrentThread = false);
    }

    public void test_checkNotHeldByCurrentThread() {
        final ReentrantCheckerLock lock = new ReentrantCheckerLock();

        @SuppressWarnings("unused")
        boolean held;
        @SuppressWarnings("unused")
        boolean byCurrentThread;

        /*
         * Not held.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                cu.assertTrue(lock.checkNotHeldByCurrentThread());
            }
        }, lock, held = false, byCurrentThread = false);

        /*
         * Held by current thread.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.checkNotHeldByCurrentThread();
                    cu.fail();
                } catch (IllegalStateException e) {
                    // ok
                }
            }
        }, lock, held = true, byCurrentThread = true);

        /*
         * Held by another thread.
         */

        callRunnableWhenLockInState(new Runnable() {
            @Override
            public void run() {
                cu.assertTrue(lock.checkNotHeldByCurrentThread());
            }
        }, lock, held = true, byCurrentThread = false);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void tearDown() {
        cu.assertNoError();
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * Tests usage.
     */
    private static void useLock(ReentrantCheckerLock lock) {
        assertFalse(lock.isLocked());
        assertFalse(lock.isHeldByCurrentThread());
        assertEquals(0,lock.getQueueLength());
        assertEquals(0,lock.getHoldCount());
        assertEquals(null,lock.getOwnerBestEffort());
        lock.checkNotHeldByAnotherThread();
        lock.checkNotHeldByCurrentThread();
        lock.checkNotLocked();
        lock.lock();
        try {
            assertTrue(lock.isLocked());
            assertTrue(lock.isHeldByCurrentThread());
            assertEquals(0,lock.getQueueLength());
            assertEquals(1,lock.getHoldCount());
            assertEquals(Thread.currentThread(),lock.getOwnerBestEffort());
            lock.checkHeldByCurrentThread();
        } finally {
            lock.unlock();
        }
        assertFalse(lock.isLocked());
        assertFalse(lock.isHeldByCurrentThread());
        assertEquals(0,lock.getQueueLength());
        assertEquals(0,lock.getHoldCount());
        assertEquals(null,lock.getOwnerBestEffort());
        lock.checkNotHeldByAnotherThread();
        lock.checkNotHeldByCurrentThread();
        lock.checkNotLocked();
    }

    /**
     * Lock type:
     * 0 = lock()
     * 1 = lockInterruptibly()
     * 2 = tryLock()
     * 3 = tryLock(long,TimeUnit)
     */
    private static void lock(Lock lock, int lockType) {
        if (lockType == 0) {
            lock.lock();
        } else if (lockType == 1) {
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                throw new RethrowException(e);
            }
        } else if (lockType == 2) {
            final boolean didIt = lock.tryLock();
            assertTrue(didIt);
        } else if (lockType == 3) {
            final boolean didIt;
            try {
                didIt = lock.tryLock(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                throw new RethrowException(e);
            }
            assertTrue(didIt);
        } else {
            throw new AssertionError(lockType);
        }
    }

    private void callRunnableWhenLockInState(
            final Runnable runnable,
            final InterfaceCheckerLock lock,
            boolean held,
            boolean byCurrentThread) {
        ExecutorService executor = null;
        final AtomicBoolean otherThreadHoldsLock = new AtomicBoolean();
        if (held) {
            if (byCurrentThread) {
                lock.lock();
            } else {
                executor = Executors.newCachedThreadPool();
                executor.execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                lock.lock();
                                try {
                                    otherThreadHoldsLock.set(true);
                                    synchronized (otherThreadHoldsLock) {
                                        while (otherThreadHoldsLock.get()) {
                                            Unchecked.wait(otherThreadHoldsLock);
                                        }
                                    }
                                } finally {
                                    lock.unlock();
                                }
                            }
                        });
                while (!otherThreadHoldsLock.get()) {
                    Unchecked.sleepMs(1L);
                }
            }
        }
        runnable.run();
        if (held) {
            if (byCurrentThread) {
                lock.unlock();
            } else {
                otherThreadHoldsLock.set(false);
                synchronized (otherThreadHoldsLock) {
                    otherThreadHoldsLock.notifyAll();
                }
                Unchecked.shutdownAndAwaitTermination(executor);
            }
        }
    }
}
