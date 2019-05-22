/*
 * Copyright 2019 Jeff Hain
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
package net.jolikit.lang;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.jolikit.lang.RethrowException;
import net.jolikit.lang.Unchecked;
import junit.framework.TestCase;

public class UncheckedTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final long TOLERANCE_NS = 100L * 1000L * 1000L;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_sleepMS_long() {
        // Supposing we are not interrupted.
        assertFalse(Thread.currentThread().isInterrupted());

        // Not interrupted (no duration).
        Unchecked.sleepMs(0L);

        // Not interrupted (duration).
        Unchecked.sleepMs(10L);

        // Interrupted (no duration).
        Thread.currentThread().interrupt();
        try {
            Unchecked.sleepMs(0L);
            fail();
        } catch (RethrowException e) {
            assertEquals(InterruptedException.class, e.getCause().getClass());
        }
        // Interrupted status had been restored.
        assertTrue(Thread.currentThread().isInterrupted());
        // Clearing interrupted status.
        Thread.interrupted();

        // Interrupted (duration).
        Thread.currentThread().interrupt();
        try {
            Unchecked.sleepMs(10L);
            fail();
        } catch (RethrowException e) {
            assertEquals(InterruptedException.class, e.getCause().getClass());
        }
        // Interrupted status had been restored.
        assertTrue(Thread.currentThread().isInterrupted());
        // Clearing interrupted status.
        Thread.interrupted();

        // Duration.
        final long durationNs = 2 * TOLERANCE_NS;
        long a = System.nanoTime();
        Unchecked.sleepMs(durationNs/(1000L*1000L));
        long b = System.nanoTime();
        assertEquals((double)durationNs,(double)(b-a),(double)TOLERANCE_NS);
    }

    public void test_wait_Object() {
        final Object object = new Object();
        
        /*
         * 
         */
        
        try {
            Unchecked.wait(object);
            fail();
        } catch (IllegalMonitorStateException e) {
            // ok
        }
        
        /*
         * 
         */

        final long notifyTimeoutNs = 2 * TOLERANCE_NS;
        final ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Unchecked.sleepMs(notifyTimeoutNs/(1000L*1000L));
                synchronized (object) {
                    object.notifyAll();
                }
            }
        });
        long a = System.nanoTime();
        synchronized (object) {
            Unchecked.wait(object);
        }
        long b = System.nanoTime();
        assertEquals((double)notifyTimeoutNs,(double)(b-a),(double)TOLERANCE_NS);
        Unchecked.shutdownAndAwaitTermination(executor);
    }

    public void test_waitMS_Object_long() {
        final Object object = new Object();

        /*
         * 
         */
        
        try {
            Unchecked.waitMs(object,-1L);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            Unchecked.waitMs(object,0L);
            fail();
        } catch (IllegalMonitorStateException e) {
            // ok
        }
        
        /*
         * 
         */

        final long notifyTimeoutNs = 2 * TOLERANCE_NS;
        for (long waitTimeoutMs : new long[]{0L,1L,Long.MAX_VALUE}) {
            final ExecutorService executor = Executors.newCachedThreadPool();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Unchecked.sleepMs(notifyTimeoutNs/(1000L*1000L));
                    synchronized (object) {
                        object.notifyAll();
                    }
                }
            });
            long a = System.nanoTime();
            synchronized (object) {
                Unchecked.waitMs(object,waitTimeoutMs);
            }
            long b = System.nanoTime();
            if (waitTimeoutMs == 0L) {
                // Special case.
                assertEquals((double)notifyTimeoutNs,(double)(b-a),(double)TOLERANCE_NS);
            } else {
                assertEquals(Math.min((double)notifyTimeoutNs,waitTimeoutMs*1e6),(double)(b-a),(double)TOLERANCE_NS);
            }
            Unchecked.shutdownAndAwaitTermination(executor);
        }
    }

    public void test_awaitTermination_ExecutorService() {
        // Testing would be overkill.
    }

    public void test_shutdownAndAwaitTermination_ExecutorService() {
        // Testing would be overkill.
    }

    public void test_throwIt_Throwable() {
        // Quiet null.
        assertNull(Unchecked.throwIt(null));
        
        for (Throwable t : new Throwable[]{new RuntimeException(),new Error(), new Throwable()}) {
            boolean completedNormally = false;
            try {
                Unchecked.throwIt(t);
                completedNormally = true;
            } catch (Throwable e) {
                assertSame(t,e);
            }
            assertFalse(completedNormally);
        }
    }
}
