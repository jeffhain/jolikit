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
package net.jolikit.lang;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;
import net.jolikit.test.utils.TestUtils;

public class LangUtilsTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final long TOLERANCE_NS = 100L * 1000L * 1000L;

    private static final int NBR_OF_CASES = 100 * 1000;
    
    private final Random random = TestUtils.newRandom123456789L();

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyRecordingPrintStream extends PrintStream {
        public MyRecordingPrintStream() {
            super(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    // no write
                }
            }, false);
        }
        final StringBuilder recorded = new StringBuilder();
        @Override
        public void println(Object x) {
            this.recorded.append(x);
            this.recorded.append(LangUtils.LINE_SEPARATOR);
        }
        @Override
        public void println(String x) {
            this.recorded.append(x);
            this.recorded.append(LangUtils.LINE_SEPARATOR);
        }
    };
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final String UNDEFINED_PROPERTY = "undefined property";
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_increasedArrayLength_2int() {
        
        final double growthFactor = 1.5;
        
        final int[] specialValues = new int[]{
                -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                (Integer.MAX_VALUE/2)-1,
                (Integer.MAX_VALUE/2),
                (Integer.MAX_VALUE/2)+1,
                (int)(Integer.MAX_VALUE/growthFactor)-1,
                (int)(Integer.MAX_VALUE/growthFactor),
                (int)(Integer.MAX_VALUE/growthFactor)+1,
                (int)(LangUtils.MAX_ARRAY_LENGTH/growthFactor)-1,
                (int)(LangUtils.MAX_ARRAY_LENGTH/growthFactor),
                (int)(LangUtils.MAX_ARRAY_LENGTH/growthFactor)+1,
                LangUtils.MAX_ARRAY_LENGTH-1,
                LangUtils.MAX_ARRAY_LENGTH,
                LangUtils.MAX_ARRAY_LENGTH+1,
                Integer.MAX_VALUE-1,
                Integer.MAX_VALUE,
        };
        
        final int n = specialValues.length;
        // Number of special cases.
        final int n2 = n*n;
        
        // Making sure we do some random cases.
        final int m = n2 + 10*1000;
        
        final Random random = TestUtils.newRandom123456789L();
        for (int i = 0; i < m; i++) {
            final int previousLength;
            final int minLength;
            if (i < n2) {
                previousLength = specialValues[i/n];
                minLength = specialValues[i%n];
            } else {
                previousLength = random.nextInt();
                minLength = random.nextInt();
            }
            
            if (DEBUG) {
                System.out.println("previousLength = " + previousLength);
                System.out.println("minLength = " + minLength);
            }
            
            final int newLength;
            try {
                newLength = LangUtils.increasedArrayLength(previousLength, minLength);
            } catch (IllegalArgumentException e) {
                assertTrue(
                        (previousLength < 0)
                        || (previousLength > LangUtils.MAX_ARRAY_LENGTH)
                        || (minLength < 0));
                continue;
            } catch (OutOfMemoryError e) {
                assertTrue(previousLength == Integer.MAX_VALUE);
                continue;
            }

            if (DEBUG) {
                System.out.println("newLength = " + newLength);
            }
            
            assertTrue(
                    (previousLength >= 0)
                    && (previousLength < Integer.MAX_VALUE)
                    && (minLength >= 0));
            
            // Growth.
            assertTrue(newLength > previousLength);
            // Enough growth.
            if (growthFactor * previousLength < LangUtils.MAX_ARRAY_LENGTH) {
                assertTrue(newLength >= (int)(growthFactor * previousLength));
            } else {
                if ((previousLength < LangUtils.MAX_ARRAY_LENGTH)
                        && (minLength <= LangUtils.MAX_ARRAY_LENGTH)) {
                    assertEquals(LangUtils.MAX_ARRAY_LENGTH, newLength);
                } else {
                    if (previousLength >= LangUtils.MAX_ARRAY_LENGTH) {
                        assertEquals(Integer.MAX_VALUE, newLength);
                    } else {
                        assertEquals(minLength, newLength);
                    }
                }
            }
            assertTrue(newLength >= minLength);
            if (previousLength < LangUtils.MAX_ARRAY_LENGTH) {
                if (minLength < LangUtils.MAX_ARRAY_LENGTH) {
                    // Not too much growth.
                    assertTrue(newLength <= LangUtils.MAX_ARRAY_LENGTH);
                } else {
                    assertEquals(minLength, newLength);
                }
            } else {
                assertEquals(Integer.MAX_VALUE, newLength);
            }
        }
    }
    
    public void test_assertionsEnabled() {
        // Supposing same enabling for assertions
        // in LangUtils and in this class.
        boolean enabled = false;
        assert(enabled = !enabled);
        assertEquals(enabled,LangUtils.assertionsEnabled());
    }

    public void test_azzert_boolean() {
        try {
            LangUtils.azzert(false);
            fail();
        } catch (AssertionError e) {
            // ok
        }
        assertTrue(LangUtils.azzert(true));
    }

    public void test_hashCode_boolean() {
        assertEquals(0, LangUtils.hashCode(false));
        assertEquals(1, LangUtils.hashCode(true));
    }

    public void test_hashCode_Object() {
        assertEquals(0, LangUtils.hashCode(null));
        
        {
            final Object obj = new Object();
            assertEquals(obj.hashCode(), LangUtils.hashCode(obj));
        }
    }
    
    public void test_equalOrBothNull_2Object() {
        assertTrue(LangUtils.equalOrBothNull(null, null));
        assertTrue(LangUtils.equalOrBothNull(1, 1));

        assertFalse(LangUtils.equalOrBothNull(null, 1));
        assertFalse(LangUtils.equalOrBothNull(1, null));
        assertFalse(LangUtils.equalOrBothNull(1, 1L));
    }
    
    public void test_checkNonNull_Object() {
        try {
            LangUtils.checkNonNull(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        assertTrue(LangUtils.checkNonNull(new Object()));
        assertTrue(LangUtils.checkNonNull(""));
    }

    public void test_requireNonNull_Object() {
        try {
            LangUtils.requireNonNull(null);
            fail();
        } catch (NullPointerException e) {
            assertEquals(null, e.getMessage());
        }
        
        Object ref = new Object();
        assertSame(ref, LangUtils.requireNonNull(ref));
    }

    public void test_requireNonNull_Object_String() {
        for (String message : new String[]{null, "", "foo"}) {
            try {
                LangUtils.requireNonNull(null, message);
                fail();
            } catch (NullPointerException e) {
                assertEquals(message, e.getMessage());
            }
        }
        
        Object ref = new Object();
        assertSame(ref, LangUtils.requireNonNull(ref, ""));
    }

    public void test_checkBounds_3int() {
        assertTrue(LangUtils.checkBounds(10, 0, 10));
        assertTrue(LangUtils.checkBounds(10, 10, 0));
        assertTrue(LangUtils.checkBounds(10, 5, 5));
        assertTrue(LangUtils.checkBounds(10, 9, 1));

        assertTrue(LangUtils.checkBounds(Integer.MAX_VALUE, 0, Integer.MAX_VALUE));
        assertTrue(LangUtils.checkBounds(Integer.MAX_VALUE, Integer.MAX_VALUE-1, 1));

        // limit < 0
        try {
            LangUtils.checkBounds(-1, 0, 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        
        // from < 0
        try {
            LangUtils.checkBounds(10, -1, 5);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        // length < 0
        try {
            LangUtils.checkBounds(10, 5, -1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        // from+length overflow
        try {
            LangUtils.checkBounds(10, Integer.MAX_VALUE, 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            LangUtils.checkBounds(10, 1, Integer.MAX_VALUE);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        // from+length > limit
        try {
            LangUtils.checkBounds(10, 0, 11);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            LangUtils.checkBounds(10, 11, 0);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        
        /*
         * random
         */
        
        for (int i = 0; i < NBR_OF_CASES; i++) {
            // Bit shift so that it's not too rare to end up with
            // large or small magnitudes.
            final int limit = (random.nextInt()>>Math.min(31,random.nextInt(5)*8));
            final int from = (random.nextInt()>>Math.min(31,random.nextInt(5)*8));
            final int length = (random.nextInt()>>Math.min(31,random.nextInt(5)*8));
            try {
                LangUtils.checkBounds(limit,from,length);
                assertTrue(limit >= 0);
                assertTrue(from >= 0);
                assertTrue(length >= 0);
                assertTrue(from+length >= 0);
                assertTrue(from+length <= limit);
            } catch (IndexOutOfBoundsException e) {
                assertTrue((limit < 0) || (from < 0) || (length < 0) || (from+length < 0) || (from+length > limit));
            }
        }
    }

    public void test_checkBounds_3long() {
        assertTrue(LangUtils.checkBounds(10L, 0L, 10L));
        assertTrue(LangUtils.checkBounds(10L, 10L, 0L));
        assertTrue(LangUtils.checkBounds(10L, 5L, 5L));
        assertTrue(LangUtils.checkBounds(10L, 9L, 1L));

        assertTrue(LangUtils.checkBounds(Long.MAX_VALUE, 0L, Long.MAX_VALUE));
        assertTrue(LangUtils.checkBounds(Long.MAX_VALUE, Long.MAX_VALUE-1L, 1L));

        // limit < 0
        try {
            LangUtils.checkBounds(-1L, 0L, 1L);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        
        // from < 0
        try {
            LangUtils.checkBounds(10L, -1L, 5L);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        // length < 0
        try {
            LangUtils.checkBounds(10L, 5L, -1L);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        // from+length overflow
        try {
            LangUtils.checkBounds(10L, Long.MAX_VALUE, 1L);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            LangUtils.checkBounds(10L, 1L, Long.MAX_VALUE);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }

        // from+length > limit
        try {
            LangUtils.checkBounds(10L, 0L, 11L);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        try {
            LangUtils.checkBounds(10L, 11L, 0L);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // ok
        }
        
        /*
         * random
         */
        
        for (int i = 0; i < NBR_OF_CASES; i++) {
            // Bit shift so that it's not too rare to end up with
            // large or small magnitudes.
            final long limit = (random.nextLong()>>Math.min(63,random.nextInt(5)*16));
            final long from = (random.nextLong()>>Math.min(63,random.nextInt(5)*16));
            final long length = (random.nextLong()>>Math.min(63,random.nextInt(5)*16));
            try {
                LangUtils.checkBounds(limit,from,length);
                assertTrue(limit >= 0);
                assertTrue(from >= 0);
                assertTrue(length >= 0);
                assertTrue(from+length >= 0);
                assertTrue(from+length <= limit);
            } catch (IndexOutOfBoundsException e) {
                assertTrue((limit < 0) || (from < 0) || (length < 0) || (from+length < 0) || (from+length > limit));
            }
        }
    }

    public void test_identityToString_Object() {
        for (Object object : new Object[]{
                null,
                new Object()}) {
            assertEquals(String.valueOf(object), LangUtils.identityToString(object));
        }
        
        {
            Object object = new Object() {
                @Override
                public String toString() {
                    return "foo";
                }
                @Override
                public int hashCode() {
                    return 7;
                }
            };
            String expected = object.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(object));
            assertEquals(expected, LangUtils.identityToString(object));
        }
    }

    public void test_toString_Thread() {
        assertEquals("null", LangUtils.toString((Thread)null));

        Thread thread = new Thread();
        long id = thread.getId();
        String name = "myName";
        thread.setName(name);
        int priority = thread.getPriority();
        if (thread.getThreadGroup() != null) {
            String groupName = thread.getThreadGroup().getName();
            assertEquals("Thread[" + id + "," + name + "," + priority + "," + groupName + "]", LangUtils.toString(thread));
        } else {
            assertEquals("Thread[" + id + "," + name + "," + priority + "]", LangUtils.toString(thread));
        }
    }

    public void test_toStringStackTrace_Throwable() {
        assertEquals("null", LangUtils.toStringStackTrace(null));
        
        {
            MyRecordingPrintStream stream = new MyRecordingPrintStream();
            RuntimeException e = new RuntimeException();
            e.printStackTrace(stream);
            assertEquals(stream.recorded.toString(), LangUtils.toStringStackTrace(e));
        }
        
        {
            MyRecordingPrintStream stream = new MyRecordingPrintStream();
            RuntimeException c1 = new RuntimeException();
            IllegalArgumentException e = new IllegalArgumentException(c1);
            e.printStackTrace(stream);
            assertEquals(stream.recorded.toString(), LangUtils.toStringStackTrace(e));
        }
        
        {
            MyRecordingPrintStream stream = new MyRecordingPrintStream();
            IllegalStateException c2 = new IllegalStateException();
            RuntimeException c1 = new RuntimeException(c2);
            IllegalArgumentException e = new IllegalArgumentException(c1);
            e.printStackTrace(stream);
            assertEquals(stream.recorded.toString(), LangUtils.toStringStackTrace(e));
        }
    }

    public void test_appendStackTrace_StringBuilder_Throwable() {
        try {
            LangUtils.appendStackTrace(null, new RuntimeException());
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        /*
         * 
         */
        
        {
            StringBuilder sb = new StringBuilder("header");
            LangUtils.appendStackTrace(sb, null);
            assertEquals("headernull", sb.toString());
        }
        
        {
            StringBuilder sb = new StringBuilder("header");
            MyRecordingPrintStream stream = new MyRecordingPrintStream();
            
            RuntimeException e = new RuntimeException();
            
            e.printStackTrace(stream);
            LangUtils.appendStackTrace(sb,e);
            assertEquals("header" + stream.recorded.toString(), sb.toString());
        }
        
        {
            StringBuilder sb = new StringBuilder("header");
            MyRecordingPrintStream stream = new MyRecordingPrintStream();
            
            RuntimeException c1 = new RuntimeException();
            IllegalArgumentException e = new IllegalArgumentException(c1);
            
            e.printStackTrace(stream);
            LangUtils.appendStackTrace(sb,e);
            assertEquals("header" + stream.recorded.toString(), sb.toString());
        }

        {
            StringBuilder sb = new StringBuilder("header");
            MyRecordingPrintStream stream = new MyRecordingPrintStream();
            
            IllegalStateException c2 = new IllegalStateException();
            RuntimeException c1 = new RuntimeException(c2);
            IllegalArgumentException e = new IllegalArgumentException(c1);
            
            e.printStackTrace(stream);
            LangUtils.appendStackTrace(sb,e);
            assertEquals("header" + stream.recorded.toString(), sb.toString());
        }
    }

    /*
     * 
     */
    
    public void test_throwIfInterrupted() {
        // Supposing we are not interrupted.
        assertFalse(Thread.currentThread().isInterrupted());

        // Not interrupted.
        try {
            LangUtils.throwIfInterrupted();
            // ok
        } catch (InterruptedException e) {
            fail();
        }

        // Interrupted.
        Thread.currentThread().interrupt();
        try {
            LangUtils.throwIfInterrupted();
            fail();
        } catch (InterruptedException e) {
            // ok
        }
        // Interrupt status cleared before exception thrown.
        assertFalse(Thread.currentThread().isInterrupted());
    }

    public void test_sleepNS_long() {
        // Supposing we are not interrupted.
        assertFalse(Thread.currentThread().isInterrupted());

        // Not interrupted (no duration).
        try {
            LangUtils.sleepNs(0L);
            // ok
        } catch (InterruptedException e) {
            fail();
        }

        // Not interrupted (duration).
        try {
            LangUtils.sleepNs(10L);
            // ok
        } catch (InterruptedException e) {
            fail();
        }

        // Interrupted (no duration).
        Thread.currentThread().interrupt();
        try {
            LangUtils.sleepNs(0L);
            fail();
        } catch (InterruptedException e) {
            // ok
        }
        assertFalse(Thread.currentThread().isInterrupted());

        // Interrupted (duration).
        Thread.currentThread().interrupt();
        try {
            LangUtils.sleepNs(10L);
            fail();
        } catch (InterruptedException e) {
            // ok
        }
        assertFalse(Thread.currentThread().isInterrupted());

        // Duration.
        final long durationNs = 2 * TOLERANCE_NS;
        long a = System.nanoTime();
        try {
            LangUtils.sleepNs(durationNs);
            // ok
        } catch (InterruptedException e) {
            fail();
        }
        long b = System.nanoTime();
        assertEquals((double)durationNs,(double)(b-a),(double)TOLERANCE_NS);
    }

    public void test_waitNs_Object_long() {
        // Supposing we are not interrupted.
        assertFalse(Thread.currentThread().isInterrupted());

        // To stop long waits.
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        // Needed in case notify happens before wait start.
        final AtomicBoolean waitDone = new AtomicBoolean();

        final Object waitObject = new Object();

        // Monitor not taken (no duration).
        try {
            LangUtils.waitNs(waitObject,0L);
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (IllegalMonitorStateException e) {
            // ok
        }

        // Monitor not taken (short duration).
        try {
            LangUtils.waitNs(waitObject,10L);
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (IllegalMonitorStateException e) {
            // ok
        }

        // Monitor not taken (long duration).
        try {
            LangUtils.waitNs(waitObject,Long.MAX_VALUE);
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (IllegalMonitorStateException e) {
            // ok
        }

        // Not interrupted (no duration).
        try {
            synchronized (waitObject) {
                LangUtils.waitNs(waitObject,0L);
            }
            // ok
        } catch (InterruptedException e) {
            fail();
        }

        // Not interrupted (small duration).
        try {
            synchronized (waitObject) {
                LangUtils.waitNs(waitObject,10L);
            }
            // ok
        } catch (InterruptedException e) {
            fail();
        }

        // Not interrupted (long duration).
        waitDone.set(false);
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                while (!waitDone.get()) {
                    synchronized (waitObject) {
                        waitObject.notifyAll();
                    }
                    Unchecked.sleepMs(100);
                }
            }
        }, TOLERANCE_NS, TimeUnit.NANOSECONDS);
        try {
            synchronized (waitObject) {
                LangUtils.waitNs(waitObject,Long.MAX_VALUE);
            }
            waitDone.set(true);
            // ok
        } catch (InterruptedException e) {
            waitDone.set(true);
            fail();
        }

        // Interrupted (no duration).
        Thread.currentThread().interrupt();
        try {
            synchronized (waitObject) {
                LangUtils.waitNs(waitObject,0L);
            }
            fail();
        } catch (InterruptedException e) {
            // ok
        }
        assertFalse(Thread.currentThread().isInterrupted());

        // Interrupted (short duration).
        Thread.currentThread().interrupt();
        try {
            synchronized (waitObject) {
                LangUtils.waitNs(waitObject,10L);
            }
            fail();
        } catch (InterruptedException e) {
            // ok
        }
        assertFalse(Thread.currentThread().isInterrupted());

        // Interrupted (long duration).
        // (No need to schedule a signal, due to interrupt)
        Thread.currentThread().interrupt();
        try {
            synchronized (waitObject) {
                LangUtils.waitNs(waitObject,Long.MAX_VALUE);
            }
            fail();
        } catch (InterruptedException e) {
            // ok
        }
        assertFalse(Thread.currentThread().isInterrupted());

        // Monitor not taken and interrupted (monitor priority) (no duration).
        Thread.currentThread().interrupt();
        try {
            LangUtils.waitNs(waitObject,0L);
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (IllegalMonitorStateException e) {
            // ok
        }
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clearing

        // Monitor not taken and interrupted (monitor priority) (small duration).
        Thread.currentThread().interrupt();
        try {
            LangUtils.waitNs(waitObject,10L);
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (IllegalMonitorStateException e) {
            // ok
        }
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clearing

        // Monitor not taken and interrupted (monitor priority) (long duration).
        Thread.currentThread().interrupt();
        try {
            LangUtils.waitNs(waitObject,Long.MAX_VALUE);
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (IllegalMonitorStateException e) {
            // ok
        }
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clearing

        // Duration.
        final long durationNs = 2 * TOLERANCE_NS;
        long a = System.nanoTime();
        try {
            synchronized (waitObject) {
                LangUtils.waitNs(waitObject,durationNs);
            }
            // ok
        } catch (InterruptedException e) {
            fail();
        }
        long b = System.nanoTime();
        assertEquals((double)durationNs,(double)(b-a),(double)TOLERANCE_NS);

        Unchecked.shutdownAndAwaitTermination(executor);
    }

    public void test_getBooleanProperty_String_boolean() {
        assertEquals(true,LangUtils.getBooleanProperty(UNDEFINED_PROPERTY, true));
        assertEquals(false,LangUtils.getBooleanProperty(UNDEFINED_PROPERTY, false));
    }

    public void test_getIntProperty_String_int() {
        assertEquals(17,LangUtils.getIntProperty(UNDEFINED_PROPERTY, 17));
    }

    public void test_getLongProperty_String_long() {
        assertEquals(17L,LangUtils.getLongProperty(UNDEFINED_PROPERTY, 17L));
    }

    public void test_getFloatProperty_String_float() {
        assertEquals(17.0f,LangUtils.getFloatProperty(UNDEFINED_PROPERTY, 17.0f));
    }

    public void test_getDoubleProperty_String_double() {
        assertEquals(17.0,LangUtils.getDoubleProperty(UNDEFINED_PROPERTY, 17.0));
    }
    
    /*
     * 
     */
    
    public void test_getNanoTimeFromClassLoadNs() {
        final long expectedNs = System.nanoTime() - LangUtils.getClassLoadNanoTimeNs();
        final long actualNs = LangUtils.getNanoTimeFromClassLoadNs();
        assertEquals(expectedNs, actualNs, TOLERANCE_NS);
    }

    public void test_getClassLoadNanoTimeNs() {
        // Can't test it much...
        // ...other than positive...
        assertTrue(LangUtils.getClassLoadNanoTimeNs() >= 0);
        // ...and not changing.
        assertEquals(LangUtils.getClassLoadNanoTimeNs(), LangUtils.getClassLoadNanoTimeNs());
    }
    
    /*
     * 
     */
    
    public void test_stringOfCodePoint_int() {
        
        for (int badCp : new int[]{Integer.MIN_VALUE, -1, Character.MAX_CODE_POINT + 1, Integer.MAX_VALUE}) {
            try {
                LangUtils.stringOfCodePoint(badCp);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        /*
         * Just testing each possible value, it's quick.
         */
        
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
            final String expected = new String(new int[]{cp}, 0, 1);
            
            // Proper value.
            final String actual = LangUtils.stringOfCodePoint(cp);
            assertEquals(expected, actual);
            
            final String actual2 = LangUtils.stringOfCodePoint(cp);
            if (cp < LangUtils.STRING_BY_CODEPOINT_LENGTH) {
                // Cached instance.
                assertSame(actual, actual2);
            } else {
                // Not a cached instance.
                assertNotSame(actual, actual2);
            }
        }
    }
}
