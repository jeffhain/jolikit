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
package net.jolikit.time;

import net.jolikit.time.TimeUtils;
import junit.framework.TestCase;

public class TimeUtilsTest extends TestCase {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final double DEFAULT_EPSILON_S = 1e-15;

    private static final long DEFAULT_EPSILON_NS = 1;
    private static final long DEFAULT_EPSILON_MILLIS = 1;

    private static final long NS_PER_S = 1000L * 1000L * 1000L;
    private static final long MILLIS_PER_S = 1000L;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_nsToS_long() {
        assertEquals(0.0, TimeUtils.nsToS(0L));
        assertEquals(0.1, TimeUtils.nsToS(NS_PER_S/10));
        assertEquals(1e-9, TimeUtils.nsToS(1L), DEFAULT_EPSILON_S);
        assertEquals(3e-9, TimeUtils.nsToS(3L), DEFAULT_EPSILON_S);
        
        assertEquals(1e-9 * Long.MIN_VALUE, TimeUtils.nsToS(Long.MIN_VALUE), DEFAULT_EPSILON_S);
        assertEquals(1e-9 * Long.MAX_VALUE, TimeUtils.nsToS(Long.MAX_VALUE), DEFAULT_EPSILON_S);
    }

    public void test_nsToS_double() {
        assertEquals(0.0, TimeUtils.nsToS(0.0));
        assertEquals(0.1, TimeUtils.nsToS(NS_PER_S/10.0), DEFAULT_EPSILON_S);
        assertEquals(1e-9, TimeUtils.nsToS(1.0), DEFAULT_EPSILON_S);
        assertEquals(3e-9, TimeUtils.nsToS(3.0), DEFAULT_EPSILON_S);
        
        assertEquals(1e-9 * (double)Long.MIN_VALUE, TimeUtils.nsToS((double)Long.MIN_VALUE), DEFAULT_EPSILON_S);
        assertEquals(1e-9 * (double)Long.MAX_VALUE, TimeUtils.nsToS((double)Long.MAX_VALUE), DEFAULT_EPSILON_S);
    }

    public void test_sToNs_double() {
        assertEquals(0L, TimeUtils.sToNs(0.0));
        assertEquals(NS_PER_S/10, TimeUtils.sToNs(0.1), DEFAULT_EPSILON_NS);
        assertEquals(NS_PER_S, TimeUtils.sToNs(1.0), DEFAULT_EPSILON_NS);
        assertEquals(3*NS_PER_S, TimeUtils.sToNs(3.0), DEFAULT_EPSILON_NS);

        // Underflow.
        assertEquals(0L, TimeUtils.sToNs(0.5/NS_PER_S));
        assertEquals(0L, TimeUtils.sToNs(-0.5/NS_PER_S));
        
        assertEquals((long)(1e9 * (double)Long.MIN_VALUE), TimeUtils.sToNs((double)Long.MIN_VALUE), DEFAULT_EPSILON_NS);
        assertEquals((long)(1e9 * (double)Long.MAX_VALUE), TimeUtils.sToNs((double)Long.MAX_VALUE), DEFAULT_EPSILON_NS);
    }

    public void test_sToNsNoUnderflow_double() {
        assertEquals(0L, TimeUtils.sToNsNoUnderflow(0.0));
        assertEquals(NS_PER_S/10, TimeUtils.sToNsNoUnderflow(0.1), DEFAULT_EPSILON_NS);
        assertEquals(NS_PER_S, TimeUtils.sToNsNoUnderflow(1.0), DEFAULT_EPSILON_NS);
        assertEquals(3*NS_PER_S, TimeUtils.sToNsNoUnderflow(3.0), DEFAULT_EPSILON_NS);

        // No underflow.
        assertEquals(1L, TimeUtils.sToNsNoUnderflow(0.5/NS_PER_S));
        assertEquals(-1L, TimeUtils.sToNsNoUnderflow(-0.5/NS_PER_S));

        assertEquals((long)(1e9 * (double)Long.MIN_VALUE), TimeUtils.sToNsNoUnderflow((double)Long.MIN_VALUE), DEFAULT_EPSILON_NS);
        assertEquals((long)(1e9 * (double)Long.MAX_VALUE), TimeUtils.sToNsNoUnderflow((double)Long.MAX_VALUE), DEFAULT_EPSILON_NS);
    }

    public void test_nsDoubleToNsLong_double() {
        assertEquals(0L, TimeUtils.nsDoubleToNsLong(0.0));
        assertEquals(0L, TimeUtils.nsDoubleToNsLong(0.1), DEFAULT_EPSILON_NS);
        assertEquals(1L, TimeUtils.nsDoubleToNsLong(1.0));
        assertEquals(3L, TimeUtils.nsDoubleToNsLong(3.0), DEFAULT_EPSILON_NS);
        
        assertEquals((long)(double)(Long.MIN_VALUE/10), TimeUtils.nsDoubleToNsLong((double)(Long.MIN_VALUE/10)), DEFAULT_EPSILON_NS);
        assertEquals((long)(double)(Long.MAX_VALUE/10), TimeUtils.nsDoubleToNsLong((double)(Long.MAX_VALUE/10)), DEFAULT_EPSILON_NS);
    }
    
    /*
     * 
     */

    public void test_millisToS_long() {
        assertEquals(0.0, TimeUtils.millisToS(0L));
        assertEquals(0.1, TimeUtils.millisToS(MILLIS_PER_S/10));
        assertEquals(1e-3, TimeUtils.millisToS(1L), DEFAULT_EPSILON_S);
        assertEquals(3e-3, TimeUtils.millisToS(3L), DEFAULT_EPSILON_S);
        
        assertEquals(1e-3 * Long.MIN_VALUE, TimeUtils.millisToS(Long.MIN_VALUE), DEFAULT_EPSILON_S);
        assertEquals(1e-3 * Long.MAX_VALUE, TimeUtils.millisToS(Long.MAX_VALUE), DEFAULT_EPSILON_S);
    }

    public void test_millisToS_double() {
        assertEquals(0.0, TimeUtils.millisToS(0.0));
        assertEquals(0.1, TimeUtils.millisToS(MILLIS_PER_S/10.0), DEFAULT_EPSILON_S);
        assertEquals(1e-3, TimeUtils.millisToS(1.0), DEFAULT_EPSILON_S);
        assertEquals(3e-3, TimeUtils.millisToS(3.0), DEFAULT_EPSILON_S);
        
        assertEquals(1e-3 * (double)Long.MIN_VALUE, TimeUtils.millisToS((double)Long.MIN_VALUE), DEFAULT_EPSILON_S);
        assertEquals(1e-3 * (double)Long.MAX_VALUE, TimeUtils.millisToS((double)Long.MAX_VALUE), DEFAULT_EPSILON_S);
    }

    public void test_sToMillis_double() {
        assertEquals(0L, TimeUtils.sToMillis(0.0));
        assertEquals(MILLIS_PER_S/10, TimeUtils.sToMillis(0.1), DEFAULT_EPSILON_MILLIS);
        assertEquals(MILLIS_PER_S, TimeUtils.sToMillis(1.0), DEFAULT_EPSILON_MILLIS);
        assertEquals(3*MILLIS_PER_S, TimeUtils.sToMillis(3.0), DEFAULT_EPSILON_MILLIS);

        // Underflow.
        assertEquals(0L, TimeUtils.sToMillis(0.5/MILLIS_PER_S));
        assertEquals(0L, TimeUtils.sToMillis(-0.5/MILLIS_PER_S));
        
        assertEquals((long)(1e3 * (double)Long.MIN_VALUE), TimeUtils.sToMillis((double)Long.MIN_VALUE), DEFAULT_EPSILON_MILLIS);
        assertEquals((long)(1e3 * (double)Long.MAX_VALUE), TimeUtils.sToMillis((double)Long.MAX_VALUE), DEFAULT_EPSILON_MILLIS);
    }

    public void test_sToMillisNoUnderflow_double() {
        assertEquals(0L, TimeUtils.sToMillisNoUnderflow(0.0));
        assertEquals(MILLIS_PER_S/10, TimeUtils.sToMillisNoUnderflow(0.1), DEFAULT_EPSILON_MILLIS);
        assertEquals(MILLIS_PER_S, TimeUtils.sToMillisNoUnderflow(1.0), DEFAULT_EPSILON_MILLIS);
        assertEquals(3*MILLIS_PER_S, TimeUtils.sToMillisNoUnderflow(3.0), DEFAULT_EPSILON_MILLIS);

        // No underflow.
        assertEquals(1L, TimeUtils.sToMillisNoUnderflow(0.5/MILLIS_PER_S));
        assertEquals(-1L, TimeUtils.sToMillisNoUnderflow(-0.5/MILLIS_PER_S));

        assertEquals((long)(1e3 * (double)Long.MIN_VALUE), TimeUtils.sToMillisNoUnderflow((double)Long.MIN_VALUE), DEFAULT_EPSILON_MILLIS);
        assertEquals((long)(1e3 * (double)Long.MAX_VALUE), TimeUtils.sToMillisNoUnderflow((double)Long.MAX_VALUE), DEFAULT_EPSILON_MILLIS);
    }

    public void test_millisDoubleToMillisLong_double() {
        assertEquals(0L, TimeUtils.millisDoubleToMillisLong(0.0));
        assertEquals(0L, TimeUtils.millisDoubleToMillisLong(0.1), DEFAULT_EPSILON_MILLIS);
        assertEquals(1L, TimeUtils.millisDoubleToMillisLong(1.0));
        assertEquals(3L, TimeUtils.millisDoubleToMillisLong(3.0), DEFAULT_EPSILON_MILLIS);
        
        assertEquals((long)(double)(Long.MIN_VALUE/10), TimeUtils.millisDoubleToMillisLong((double)(Long.MIN_VALUE/10)), DEFAULT_EPSILON_MILLIS);
        assertEquals((long)(double)(Long.MAX_VALUE/10), TimeUtils.millisDoubleToMillisLong((double)(Long.MAX_VALUE/10)), DEFAULT_EPSILON_MILLIS);
    }
}
