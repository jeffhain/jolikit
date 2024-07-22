/*
 * Copyright 2019-2024 Jeff Hain
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

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Very basic and general utility methods.
 * This class must only depend on the JDK.
 */
public class LangUtils {

    /*
     * checkXXX methods return true if they don't throw,
     * so that they can be used in assertions (which then
     * would typically not throw AssertionError, so that
     * could be considered bad practice).
     */
    
    /*
     * For null object pattern, using "no-op" instead of "null", for null would
     * be more appropriate to a typed but null reference, or for a reference to
     * an object which all methods would throw.
     */
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * When a reference doesn't matter much, we can't always use Void for its
     * type, because it might still matter that it's not null, for consistency
     * with cases where some actual references to objects can be used.
     * For these cases, you can use Object type, and this reference.
     * NB: Usually done (for example in maps used at sets) using Boolean.TRUE,
     * but it causes more complexity, since one might think that whether
     * it's Boolean.TRUE or Boolean.FALSE matters, etc.
     */
    public static final Object NON_NULL = new Object();
    
    /*
     * 
     */
    
    public static final boolean[] EMPTY_BOOLEAN_ARR = new boolean[0];
    public static final byte[] EMPTY_BYTE_ARR = new byte[0];
    public static final short[] EMPTY_SHORT_ARR = new short[0];
    public static final char[] EMPTY_CHAR_ARR = new char[0];
    public static final int[] EMPTY_INT_ARR = new int[0];
    public static final long[] EMPTY_LONG_ARR = new long[0];
    public static final float[] EMPTY_FLOAT_ARR = new float[0];
    public static final double[] EMPTY_DOUBLE_ARR = new double[0];
    public static final Object[] EMPTY_OBJECT_ARR = new Object[0];
    
    /*
     * 
     */

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    public static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    /*
     * 
     */
    
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    
    /*
     * 
     */
    
    /**
     * Null object pattern, but using a better prefix than "NULL".
     */
    public static final Runnable NOOP_RUNNABLE = new Runnable() {
        @Override
        public void run() {
        }
    };

    /**
     * Null object pattern, but using a better prefix than "NULL".
     */
    public static final Executor NOOP_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable runnable) {
        }
    };
    
    /*
     * Time.
     */
    
    /**
     * Having it here, and not in clocks, for common reference
     * among all clocks that must start at zero at program start.
     */
    private static final long CLASS_LOAD_NANO_TIME_NS = System.nanoTime();

    /*
     * Code points strings cache.
     */
    
    static final int STRING_BY_CODEPOINT_LENGTH = 256;
    private static final String[] STRING_BY_CODEPOINT = new String[STRING_BY_CODEPOINT_LENGTH];
    static {
        final int[] tmpArr = new int[1];
        for (int i = 0; i < STRING_BY_CODEPOINT.length; i++) {
            STRING_BY_CODEPOINT[i] = newString(i, tmpArr);
        }
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * arrays growth
     */
    
    /**
     * Method for logarithmic growth of arrays or similar containers.
     * 
     * @param previousLength Previous array length. Must be >= 0.
     * @param minLength Min returned length. Must be >= 0.
     * @return Logarithmically-incremented length. Is > previousLength and >= minLength.
     * @throws IllegalArgumentException if minLength is < 0 or if previousLength is not >= 0.
     * @throws OutOfMemoryError if previousLength is Integer.MAX_VALUE.
     */
    public static int increasedArrayLength(int previousLength, int minLength) {
        
        /*
         * Checks.
         * Making sure IAEs are thrown with priority over OMEs,
         * to help detecting problems over overflows.
         */
        
        if (minLength < 0) {
            throw new IllegalArgumentException(
                    "minLength [" + minLength + "] must be > 0");
        }
        
        if (previousLength < 0) {
            throw new IllegalArgumentException(
                    "previousLength [" + previousLength + "] must be >= 0");
        }
        
        if (previousLength >= MAX_ARRAY_LENGTH) {
            if (previousLength == Integer.MAX_VALUE) {
                throw new OutOfMemoryError(
                        "previousLength [" + previousLength + "] is already max value");
            }
            return Integer.MAX_VALUE;
        }
        
        int result;
        if (previousLength == 0) {
            result = 1;
        } else {
            // Grows even if previousLength is 1.
            result = previousLength + ((previousLength + 1) >> 1);
            if ((result < 0) || (result > MAX_ARRAY_LENGTH)) {
                // Overflow of some kind:
                // going first to "max array length",
                // before eventually adjusting with minLength,
                // or going to Integer.MAX_VALUE on next call.
                result = MAX_ARRAY_LENGTH;
            }
        }
        if (result < minLength) {
            result = minLength;
        }
        
        return result;
    }
    
    /*
     * List utils.
     * 
     * Not putting these in some CollUtils class,
     * because increasedArrayLength() is already in this class,
     * and here we just want the most basic and frequent use cases
     * Java collections don't provide an API for.
     */
    
    /**
     * Short for "list.get(list.size()-1)".
     * 
     * @param list Must no be null.
     * @return The last element.
     * @throws NullPointerException if the specified list is null.
     * @throws IndexOutOfBoundsException if the specified list is empty.
     */
    public static <E> E getLast(List<E> list) {
        return list.get(list.size() - 1);
    }
    
    /**
     * Short for "list.remove(list.size()-1)".
     * 
     * @param list Must no be null.
     * @return The last element, removed.
     * @throws NullPointerException if the specified list is null.
     * @throws IndexOutOfBoundsException if the specified list is empty.
     */
    public static <E> E removeLast(List<E> list) {
        return list.remove(list.size() - 1);
    }
    
    /*
     * hash codes and equals
     */
    
    public static int hashCode(boolean value) {
        return (value ? 1 : 0);
    }
    
    /**
     * Available in Java 7.
     * 
     * @param obj An object, possibly null.
     * @return Specified object's hashCode(), or 0 if it's null.
     */
    public static int hashCode(Object obj) {
        return (obj != null) ? obj.hashCode() : 0;
    }

    /**
     * @return True if the specified objects are equal or both null.
     */
    public static boolean equalOrBothNull(Object a, Object b) {
        if (a == b) {
            return true;
        } else {
            return (a != null) ? a.equals(b) : false;
        }
    }

    /*
     * assertions
     */
    
    /**
     * @return True if assertions are enabled (at least for this class),
     *         false otherwise.
     */
    public static boolean assertionsEnabled() {
        // Computing it on each call, in case a time comes
        // where it might get changed at runtime.
        boolean tmp = false;
        assert(tmp = !tmp);
        return tmp;
    }

    /**
     * Useful methods for assertions that are always made,
     * either because they don't cost much, or are done in tests.
     * 
     * @param value A boolean value.
     * @return True if the specified boolean value is true.
     * @throws AssertionError if the specified boolean value is false.
     */
    public static boolean azzert(boolean value) {
        if (!value) {
            throw new AssertionError();
        }
        return true;
    }

    /*
     * checks
     */

    /**
     * @param ref A reference.
     * @return true (if the specified reference is non-null).
     * @throws NullPointerException if the specified reference is null.
     */
    public static boolean checkNonNull(Object ref) {
        if (ref == null) {
            throw new NullPointerException();
        }
        return true;
    }

    /**
     * @param ref A reference.
     * @return The specified reference (if it is non-null).
     * @throws NullPointerException if the specified reference is null.
     */
    public static <T> T requireNonNull(T ref) {
        if (ref == null) {
            throw new NullPointerException();
        }
        return ref;
    }

    /**
     * @param ref A reference.
     * @param message Message for the NPE if it is thrown.
     * @return The specified reference (if it is non-null).
     * @throws NullPointerException if the specified reference is null.
     */
    public static <T> T requireNonNull(T ref, String message) {
        if (ref == null) {
            throw new NullPointerException(message);
        }
        return ref;
    }

    /**
     * @param limit Limit of an indexed collection.
     * @param from First index.
     * @param length Number of consecutive indexes from the first one included.
     * @return True if [from,from+length-1] is included in [0,limit[.
     * @throws IndexOutOfBoundsException if limit < 0, or from < 0, or length < 0,
     *         or from+length overflows, or from+length > limit.
     */
    public static boolean checkBounds(int limit, int from, int length) {
        if (false) {
            // Also works.
            if (((limit|length|from) < 0) || (length > limit - from)) {
                throwIOOBE(limit, from, length);
            }
        } else {
            // Similar code can be found in Buffer class, but it's
            // package-private.
            if ((limit|length|from|(from+length)|(limit-(from+length))) < 0) {
                throwIOOBE(limit, from, length);
            }
        }
        return true;
    }

    /**
     * @param limit Limit of an indexed collection.
     * @param from First index.
     * @param length Number of consecutive indexes from the first one included.
     * @return True if [from,from+length-1] is included in [0,limit[.
     * @throws IndexOutOfBoundsException if limit < 0, or from < 0, or length < 0,
     *         or from+length overflows, or from+length > limit.
     */
    public static boolean checkBounds(long limit, long from, long length) {
        if ((limit|length|from|(from+length)|(limit-(from+length))) < 0) {
            throwIOOBE(limit, from, length);
        }
        return true;
    }
    
    /*
     * toString
     */

    /**
     * @return A string identical to String.valueOf(object) if the specified
     *         object had its toString() and hashCode() methods not overridden.
     */
    public static String identityToString(Object object) {
        if (object == null) {
            return String.valueOf(object);
        } else {
            return object.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(object));
        }
    }
    
    /**
     * @return A string representing the specified thread,
     *         with thread id information in first position
     *         (not provided by Thread.toString()).
     */
    public static String toString(Thread thread) {
        if (thread == null) {
            return "" + null;
        } else {
            final String threadName = thread.getName();
            // No grow if low id/priority values and group name <= thread name.
            final StringBuilder sb = new StringBuilder(15 + 2*threadName.length());
            sb.append("Thread[");
            // TODO Could add the final id when available.
            sb.append(thread.getId());
            sb.append(",");
            sb.append(threadName);
            sb.append(",");
            sb.append(thread.getPriority());
            final ThreadGroup group = thread.getThreadGroup();
            if (group != null) {
                sb.append(",");
                sb.append(group.getName());
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * @param e A Throwable.
     * @return Corresponding String, as obtained in System.err
     *         by e.printStackTrace(), or "null" if the specified
     *         Throwable is null.
     */
    public static String toStringStackTrace(Throwable e) {
        final StringBuilder sb = new StringBuilder();
        appendStackTrace(sb, e);
        return sb.toString();
    }

    /**
     * @param sb StringBuilder where to append the String corresponding
     *        to the specified Throwable, as obtained in System.err
     *        by e.printStackTrace(), or "null" if the specified Throwable
     *        is null.
     * @param e A Throwable.
     */
    public static void appendStackTrace(
            StringBuilder sb,
            Throwable e) {
        sb.append(e);
        if (e != null) {
            sb.append(LangUtils.LINE_SEPARATOR);
            appendStackTraceOnly(sb, e, null);
        }
    }

    /*
     * Threading.
     */
    
    /**
     * Clears interrupt status before throwing InterruptedException.
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void throwIfInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
    
    /**
     * @param ns Duration to sleep, in nanoseconds.
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void sleepNs(long ns) throws InterruptedException {
        if (ns <= 0) {
            throwIfInterrupted();
            return;
        }
        long millis = ns / 1000000;
        int nanos = (int) (ns - millis * 1000000);
        Thread.sleep(millis, nanos);
    }

    /**
     * Current thread must own the monitor of the specified object.
     * 
     * @param waitObject Object to wait on.
     * @param ns Duration to wait, in nanoseconds.
     * @throws IllegalMonitorStateException if current thread does not own
     *         the monitor of the specified object.
     * @throws InterruptedException if current thread is interrupted.
     */
    public static void waitNs(Object waitObject, long ns) throws InterruptedException {
        if (ns >= Long.MAX_VALUE/2) {
            // Avoiding division and timing overhead.
            waitObject.wait();
        } else {
            if (ns <= 0) {
                // Doing this for homogeneity whatever the duration.
                if (!Thread.holdsLock(waitObject)) {
                    throw new IllegalMonitorStateException();
                }
                throwIfInterrupted();
                return;
            }
            long millis = ns / 1000000;
            int nanos = (int) (ns - millis * 1000000);
            waitObject.wait(millis, nanos);
        }
    }

    /*
     * Properties.
     */

    /**
     * Uses Boolean.parseBoolean(String).
     * 
     * @param key Property's key.
     * @param defaultValue Value to return if there is no property for the specified key.
     * @return Property's value or default value if there is none.
     */
    public static boolean getBooleanProperty(
            final String key,
            boolean defaultValue) {
        final String tmp = System.getProperty(key);
        if (tmp != null) {
            return Boolean.parseBoolean(tmp);
        } else {
            return defaultValue;
        }
    }
    
    /**
     * Uses Integer.parseInt(String).
     * 
     * @param key Property's key.
     * @param defaultValue Value to return if there is no property for the specified key.
     * @return Property's value or default value if there is none.
     */
    public static int getIntProperty(
            final String key,
            int defaultValue) {
        final String tmp = System.getProperty(key);
        if (tmp != null) {
            return Integer.parseInt(tmp);
        } else {
            return defaultValue;
        }
    }
    
    /**
     * Uses Long.parseLong(String).
     * 
     * @param key Property's key.
     * @param defaultValue Value to return if there is no property for the specified key.
     * @return Property's value or default value if there is none.
     */
    public static long getLongProperty(
            final String key,
            long defaultValue) {
        final String tmp = System.getProperty(key);
        if (tmp != null) {
            return Long.parseLong(tmp);
        } else {
            return defaultValue;
        }
    }
    
    /**
     * Uses Float.parseFloat(String).
     * 
     * @param key Property's key.
     * @param defaultValue Value to return if there is no property for the specified key.
     * @return Property's value or default value if there is none.
     */
    public static float getFloatProperty(
            final String key,
            float defaultValue) {
        final String tmp = System.getProperty(key);
        if (tmp != null) {
            return Float.parseFloat(tmp);
        } else {
            return defaultValue;
        }
    }
    
    /**
     * Uses Double.parseDouble(String).
     * 
     * @param key Property's key.
     * @param defaultValue Value to return if there is no property for the specified key.
     * @return Property's value or default value if there is none.
     */
    public static double getDoubleProperty(
            final String key,
            double defaultValue) {
        final String tmp = System.getProperty(key);
        if (tmp != null) {
            return Double.parseDouble(tmp);
        } else {
            return defaultValue;
        }
    }
    
    /*
     * Time.
     */
    
    /**
     * A time that starts from zero, which is handy and avoids wrapping,
     * and make it more readable (short) and reproducible (similar times
     * for each run).
     * 
     * This also allows to make sure very large time values translate into
     * very large durations from current time.
     * 
     * @return Time since class load, in nanoseconds, using System.nanoTime().
     */
    public static long getNanoTimeFromClassLoadNs() {
        return System.nanoTime() - CLASS_LOAD_NANO_TIME_NS;
    }

    /**
     * Useful if wanting to create timestamps
     * homogeneous with getTimeFromClassLoadNs().
     * 
     * @return Value of System.nanoTime() at class load.
     */
    public static long getClassLoadNanoTimeNs() {
        return CLASS_LOAD_NANO_TIME_NS;
    }
    
    /*
     * 
     */

    /**
     * Because there is no "String.valueOfCodePoint(int)" method,
     * but just a "String(int[],int,int)" constructor that forces garbage.
     * 
     * @throws IllegalArgumentException if the specified code point is invalid,
     *         i.e. is not in Character.[MIN_CODE_POINT,MAX_CODE_POINT].
     */
    public static String stringOfCodePoint(int codePoint) {
        if (codePoint < STRING_BY_CODEPOINT.length) {
            if (codePoint < 0) {
                throw new IllegalArgumentException(Integer.toString(codePoint));
            }
            return STRING_BY_CODEPOINT[codePoint];
        } else {
            return newString(codePoint);
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * int version, for wrapping if from+length < 0, as for long version.
     */
    private static void throwIOOBE(int limit, int from, int length) {
        throw new IndexOutOfBoundsException("[from..from+length[ ([" + from + ".." + (from+length) + "[) must be in [0..limit[ ([0.." + limit + "[)");
    }

    private static void throwIOOBE(long limit, long from, long length) {
        throw new IndexOutOfBoundsException("[from..from+length[ ([" + from + ".." + (from+length) + "[) must be in [0..limit[ ([0.." + limit + "[)");
    }

    /*
     * 
     */
    
    /**
     * This treatment is recursive.
     * 
     * Only appends stack trace, without exception's toString as header.
     * 
     * @param causedTrace Can be null, if the specified exception did not cause another.
     */
    private static void appendStackTraceOnly(
            StringBuilder sb,
            Throwable e,
            StackTraceElement[] causedTrace) {
        final StackTraceElement[] trace = e.getStackTrace();
        
        int m = trace.length-1;
        
        // Compute number of frames in common between this and caused.
        if (causedTrace != null) {
            int n = causedTrace.length-1;
            while ((m >= 0) && (n >=0) && trace[m].equals(causedTrace[n])) {
                m--;
                n--;
            }
        }
        final int framesInCommon = trace.length-1 - m;
        
        if (causedTrace != null) {
            sb.append("Caused by: " + e);
            sb.append(LangUtils.LINE_SEPARATOR);
        }
        for (int i = 0; i <= m; i++) {
            sb.append("\tat " + trace[i]);
            sb.append(LangUtils.LINE_SEPARATOR);
        }
        if (framesInCommon != 0) {
            sb.append("\t... " + framesInCommon + " more");
            sb.append(LangUtils.LINE_SEPARATOR);
        }

        // Recurse if we have a cause.
        final Throwable cause = e.getCause();
        if (cause != null) {
            appendStackTraceOnly(sb, cause, trace);
        }
    }
    
    /*
     * 
     */

    /**
     * @param codePoint Must be >= 0 (not checked),
     *        else might accept negative values.
     * @param tmpArr Length must be >= 1.
     */
    private static String newString(
            int codePoint,
            int[] tmpArr) {
        if (codePoint <= 0xFFFF) {
            return String.valueOf((char) codePoint);
        } else {
            return newString_above_0xFFFF(codePoint, tmpArr);
        }
    }

    /**
     * @param codePoint Must be >= 0 (not checked),
     *        else might accept negative values.
     */
    private static String newString(int codePoint) {
        if (codePoint <= 0xFFFF) {
            return String.valueOf((char) codePoint);
        } else {
            return newString_above_0xFFFF(codePoint, new int[1]);
        }
    }

    /**
     * @param tmpArr Length must be >= 1.
     */
    private static String newString_above_0xFFFF(
            int codePoint,
            int[] tmpArr) {
        tmpArr[0] = codePoint;
        return new String(tmpArr, 0, 1);
    }
}
