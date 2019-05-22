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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Atomically ensures that the specified AtomicInteger holds
     * min(its current value, specified value), using possibly multiple CASes,
     * and returns this min value.
     * 
     * If the specified AtomicInteger is initially found to hold
     * a value inferior or equal to the specified value, this method has
     * volatile read semantics, else, it has volatile read and write semantics.
     * 
     * @param atomic An AtomicInteger.
     * @param value A value.
     * @return min(atomic, value).
     */
    public static int ensureMinAndGet(AtomicInteger atomic, int value) {
        int tmpLastReturned;
        do {
            tmpLastReturned = atomic.get();
            if (tmpLastReturned <= value) {
                return tmpLastReturned;
            }
            // Here, value < tmpLastReturned,
            // so we will try to set it as new value.
        } while (!atomic.compareAndSet(tmpLastReturned, value));
        return value;
    }
    
    /**
     * Atomically ensures that the specified AtomicLong holds
     * min(its current value, specified value), using possibly multiple CASes,
     * and returns this min value.
     * 
     * If the specified AtomicLong is initially found to hold
     * a value inferior or equal to the specified value, this method has
     * volatile read semantics, else, it has volatile read and write semantics.
     * 
     * @param atomic An AtomicLong.
     * @param value A value.
     * @return min(atomic, value).
     */
    public static long ensureMinAndGet(AtomicLong atomic, long value) {
        long tmpLastReturned;
        do {
            tmpLastReturned = atomic.get();
            if (tmpLastReturned <= value) {
                return tmpLastReturned;
            }
            // Here, value < tmpLastReturned,
            // so we will try to set it as new value.
        } while (!atomic.compareAndSet(tmpLastReturned, value));
        return value;
    }
    
    /**
     * Atomically ensures that the specified AtomicInteger holds
     * max(its current value, specified value), using possibly multiple CASes,
     * and returns this max value.
     * 
     * If the specified AtomicInteger is initially found to hold
     * a value superior or equal to the specified value, this method has
     * volatile read semantics, else, it has volatile read and write semantics.
     * 
     * @param atomic An AtomicInteger.
     * @param value A value.
     * @return max(atomic, value).
     */
    public static int ensureMaxAndGet(AtomicInteger atomic, int value) {
        int tmpLastReturned;
        do {
            tmpLastReturned = atomic.get();
            if (tmpLastReturned >= value) {
                return tmpLastReturned;
            }
            // Here, value > tmpLastReturned,
            // so we will try to set it as new value.
        } while (!atomic.compareAndSet(tmpLastReturned, value));
        return value;
    }
    
    /**
     * Atomically ensures that the specified AtomicLong holds
     * max(its current value, specified value), using possibly multiple CASes,
     * and returns this max value.
     * 
     * If the specified AtomicLong is initially found to hold
     * a value superior or equal to the specified value, this method has
     * volatile read semantics, else, it has volatile read and write semantics.
     * 
     * @param atomic An AtomicLong.
     * @param value A value.
     * @return max(atomic, value).
     */
    public static long ensureMaxAndGet(AtomicLong atomic, long value) {
        long tmpLastReturned;
        do {
            tmpLastReturned = atomic.get();
            if (tmpLastReturned >= value) {
                return tmpLastReturned;
            }
            // Here, value > tmpLastReturned,
            // so we will try to set it as new value.
        } while (!atomic.compareAndSet(tmpLastReturned, value));
        return value;
    }
    
    /*
     * 
     */
    
    /**
     * Atomically ensures that the specified AtomicInteger holds
     * min(its current value, specified value), using possibly multiple CASes,
     * and returns value read before CAS or before finding out no CAS is needed.
     * 
     * If the specified AtomicInteger is initially found to hold
     * a value inferior or equal to the specified value, this method has
     * volatile read semantics, else, it has volatile read and write semantics.
     * 
     * @param atomic An AtomicInteger.
     * @param value A value.
     * @return value read before CAS or before finding out no CAS is needed.
     */
    public static int getAndEnsureMin(AtomicInteger atomic, int value) {
        int tmpLastReturned;
        do {
            tmpLastReturned = atomic.get();
            if (tmpLastReturned <= value) {
                return tmpLastReturned;
            }
            // Here, value < tmpLastReturned,
            // so we will try to set it as new value.
        } while (!atomic.compareAndSet(tmpLastReturned, value));
        return tmpLastReturned;
    }

    /**
     * Atomically ensures that the specified AtomicLong holds
     * min(its current value, specified value), using possibly multiple CASes,
     * and returns value read before CAS or before finding out no CAS is needed.
     * 
     * If the specified AtomicLong is initially found to hold
     * a value inferior or equal to the specified value, this method has
     * volatile read semantics, else, it has volatile read and write semantics.
     * 
     * @param atomic An AtomicLong.
     * @param value A value.
     * @return value read before CAS or before finding out no CAS is needed.
     */
    public static long getAndEnsureMin(AtomicLong atomic, long value) {
        long tmpLastReturned;
        do {
            tmpLastReturned = atomic.get();
            if (tmpLastReturned <= value) {
                return tmpLastReturned;
            }
            // Here, value < tmpLastReturned,
            // so we will try to set it as new value.
        } while (!atomic.compareAndSet(tmpLastReturned, value));
        return tmpLastReturned;
    }
    
    /**
     * Atomically ensures that the specified AtomicInteger holds
     * max(its current value, specified value), using possibly multiple CASes,
     * and returns value read before CAS or before finding out no CAS is needed.
     * 
     * If the specified AtomicInteger is initially found to hold
     * a value superior or equal to the specified value, this method has
     * volatile read semantics, else, it has volatile read and write semantics.
     * 
     * @param atomic An AtomicInteger.
     * @param value A value.
     * @return value read before CAS or before finding out no CAS is needed.
     */
    public static int getAndEnsureMax(AtomicInteger atomic, int value) {
        int tmpLastReturned;
        do {
            tmpLastReturned = atomic.get();
            if (tmpLastReturned >= value) {
                return tmpLastReturned;
            }
            // Here, value > tmpLastReturned,
            // so we will try to set it as new value.
        } while (!atomic.compareAndSet(tmpLastReturned, value));
        return tmpLastReturned;
    }
    
    /**
     * Atomically ensures that the specified AtomicLong holds
     * max(its current value, specified value), using possibly multiple CASes,
     * and returns value read before CAS or before finding out no CAS is needed.
     * 
     * If the specified AtomicLong is initially found to hold
     * a value superior or equal to the specified value, this method has
     * volatile read semantics, else, it has volatile read and write semantics.
     * 
     * @param atomic An AtomicLong.
     * @param value A value.
     * @return value read before CAS or before finding out no CAS is needed.
     */
    public static long getAndEnsureMax(AtomicLong atomic, long value) {
        long tmpLastReturned;
        do {
            tmpLastReturned = atomic.get();
            if (tmpLastReturned >= value) {
                return tmpLastReturned;
            }
            // Here, value > tmpLastReturned,
            // so we will try to set it as new value.
        } while (!atomic.compareAndSet(tmpLastReturned, value));
        return tmpLastReturned;
    }
}
