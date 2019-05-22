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

import net.jolikit.lang.AtomicUtils;
import junit.framework.TestCase;

public class AtomicUtilsTest extends TestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_ensureMinAndGet_AtomicInteger_int() {
        final AtomicInteger atomic = new AtomicInteger();

        assertEquals(0,AtomicUtils.ensureMinAndGet(atomic, 0));
        assertEquals(0,atomic.get());
        assertEquals(-1,AtomicUtils.ensureMinAndGet(atomic, -1));
        assertEquals(-1,atomic.get());
        assertEquals(-1,AtomicUtils.ensureMinAndGet(atomic, 1));
        assertEquals(-1,atomic.get());
    }

    public void test_ensureMinAndGet_AtomicLong_long() {
        final AtomicLong atomic = new AtomicLong();

        assertEquals(0L,AtomicUtils.ensureMinAndGet(atomic, 0L));
        assertEquals(0L,atomic.get());
        assertEquals(-1L,AtomicUtils.ensureMinAndGet(atomic, -1L));
        assertEquals(-1L,atomic.get());
        assertEquals(-1L,AtomicUtils.ensureMinAndGet(atomic, 1L));
        assertEquals(-1L,atomic.get());
    }

    public void test_ensureMaxAndGet_AtomicInteger_int() {
        final AtomicInteger atomic = new AtomicInteger();

        assertEquals(0,AtomicUtils.ensureMaxAndGet(atomic, 0));
        assertEquals(0,atomic.get());
        assertEquals(1,AtomicUtils.ensureMaxAndGet(atomic, 1));
        assertEquals(1,atomic.get());
        assertEquals(1,AtomicUtils.ensureMaxAndGet(atomic, -1));
        assertEquals(1,atomic.get());
    }

    public void test_ensureMaxAndGet_AtomicLong_long() {
        final AtomicLong atomic = new AtomicLong();

        assertEquals(0L,AtomicUtils.ensureMaxAndGet(atomic, 0L));
        assertEquals(0L,atomic.get());
        assertEquals(1L,AtomicUtils.ensureMaxAndGet(atomic, 1L));
        assertEquals(1L,atomic.get());
        assertEquals(1L,AtomicUtils.ensureMaxAndGet(atomic, -1L));
        assertEquals(1L,atomic.get());
    }
    
    /*
     * 
     */

    public void test_getAndEnsureMin_AtomicInteger_int() {
        final AtomicInteger atomic = new AtomicInteger();

        assertEquals(0,AtomicUtils.getAndEnsureMin(atomic, 0));
        assertEquals(0,atomic.get());
        assertEquals(0,AtomicUtils.getAndEnsureMin(atomic, -1));
        assertEquals(-1,atomic.get());
        assertEquals(-1,AtomicUtils.getAndEnsureMin(atomic, 1));
        assertEquals(-1,atomic.get());
    }

    public void test_getAndEnsureMin_AtomicLong_long() {
        final AtomicLong atomic = new AtomicLong();

        assertEquals(0L,AtomicUtils.getAndEnsureMin(atomic, 0L));
        assertEquals(0L,atomic.get());
        assertEquals(0L,AtomicUtils.getAndEnsureMin(atomic, -1L));
        assertEquals(-1L,atomic.get());
        assertEquals(-1L,AtomicUtils.getAndEnsureMin(atomic, 1L));
        assertEquals(-1L,atomic.get());
    }

    public void test_getAndEnsureMax_AtomicInteger_int() {
        final AtomicInteger atomic = new AtomicInteger();

        assertEquals(0,AtomicUtils.getAndEnsureMax(atomic, 0));
        assertEquals(0,atomic.get());
        assertEquals(0,AtomicUtils.getAndEnsureMax(atomic, 1));
        assertEquals(1,atomic.get());
        assertEquals(1,AtomicUtils.getAndEnsureMax(atomic, -1));
        assertEquals(1,atomic.get());
    }

    public void test_getAndEnsureMax_AtomicLong_long() {
        final AtomicLong atomic = new AtomicLong();

        assertEquals(0L,AtomicUtils.getAndEnsureMax(atomic, 0L));
        assertEquals(0L,atomic.get());
        assertEquals(0L,AtomicUtils.getAndEnsureMax(atomic, 1L));
        assertEquals(1L,atomic.get());
        assertEquals(1L,AtomicUtils.getAndEnsureMax(atomic, -1L));
        assertEquals(1L,atomic.get());
    }
}
