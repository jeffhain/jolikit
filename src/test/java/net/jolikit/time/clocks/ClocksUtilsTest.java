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
package net.jolikit.time.clocks;

import net.jolikit.time.clocks.ClocksUtils;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.clocks.InterfaceClockModificationListener;
import net.jolikit.time.clocks.InterfaceEnslavedClock;
import net.jolikit.time.clocks.InterfaceListenableClock;
import junit.framework.TestCase;

public class ClocksUtilsTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyClock implements InterfaceClock {
        double timeSpeed;
        public MyClock(double timeSpeed) {
            this.timeSpeed = timeSpeed;
        }
        public MyClock() {
            this.timeSpeed = 1.0;
        }
        @Override
        public double getTimeS() {
            throw new UnsupportedOperationException();
        }
        @Override
        public long getTimeNs() {
            throw new UnsupportedOperationException();
        }
        @Override
        public double getTimeSpeed() {
            return this.timeSpeed;
        }
    }
    
    private class MyEnslavedClock extends MyClock implements InterfaceEnslavedClock {
        int factorToMasterTime;
        int offsetToMasterTime;
        private final InterfaceClock masterClock;
        public MyEnslavedClock(
                double timeSpeed,
                final InterfaceClock masterClock) {
            super(timeSpeed);
            this.masterClock = masterClock;
        }
        public MyEnslavedClock(InterfaceClock masterClock) {
            this.masterClock = masterClock;
        }
        @Override
        public InterfaceClock getMasterClock() {
            return this.masterClock;
        }
        @Override
        public long computeMasterTimeNs(long slaveTimeNs) {
            return slaveTimeNs * factorToMasterTime + offsetToMasterTime;
        }
    }

    private class MyListenableClock extends MyClock implements InterfaceListenableClock {
        InterfaceClockModificationListener added;
        InterfaceClockModificationListener removed;
        @Override
        public boolean addListener(InterfaceClockModificationListener listener) {
            this.added = listener;
            return true;
        }
        @Override
        public boolean removeListener(InterfaceClockModificationListener listener) {
            this.removed = listener;
            return true;
        }
    }

    private class MyEnslavedListenableClock extends MyEnslavedClock implements InterfaceListenableClock {
        InterfaceClockModificationListener added;
        InterfaceClockModificationListener removed;
        public MyEnslavedListenableClock(InterfaceClock masterClock) {
            super(masterClock);
        }
        @Override
        public boolean addListener(InterfaceClockModificationListener listener) {
            this.added = listener;
            return true;
        }
        @Override
        public boolean removeListener(InterfaceClockModificationListener listener) {
            this.removed = listener;
            return true;
        }
    }
    
    private class MyClockListener implements InterfaceClockModificationListener {
        @Override
        public void onClockModification(InterfaceClock clock) {
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public void test_computeTimeSpeedsProduct_2double() {
        
        /*
         * NaN's
         */
        
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(Double.NaN, 0.0));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(Double.NaN, -0.0));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(Double.NaN, Double.POSITIVE_INFINITY));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(Double.NaN, Double.NEGATIVE_INFINITY));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(Double.NaN, 1.25));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(Double.NaN, -1.25));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(0.0, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(-0.0, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(Double.POSITIVE_INFINITY, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(Double.NEGATIVE_INFINITY, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(1.25, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsProduct(-1.25, Double.NaN));

        /*
         * zeroes
         */
        
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsProduct(0.0, 0.0));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsProduct(0.0, -0.0));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsProduct(-0.0, 0.0));
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsProduct(-0.0, -0.0));

        /*
         * infinities
         */
        
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsProduct(0.0, Double.POSITIVE_INFINITY));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsProduct(0.0, Double.NEGATIVE_INFINITY));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsProduct(-0.0, Double.POSITIVE_INFINITY));
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsProduct(-0.0, Double.NEGATIVE_INFINITY));

        assertEquals(0.0, ClocksUtils.computeTimeSpeedsProduct(Double.POSITIVE_INFINITY, 0.0));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsProduct(Double.NEGATIVE_INFINITY, 0.0));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsProduct(Double.POSITIVE_INFINITY, -0.0));
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsProduct(Double.NEGATIVE_INFINITY, -0.0));

        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeTimeSpeedsProduct(1.25, Double.POSITIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeTimeSpeedsProduct(1.25, Double.NEGATIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeTimeSpeedsProduct(-1.25, Double.POSITIVE_INFINITY));
        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeTimeSpeedsProduct(-1.25, Double.NEGATIVE_INFINITY));

        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeTimeSpeedsProduct(Double.POSITIVE_INFINITY, 1.25));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeTimeSpeedsProduct(Double.NEGATIVE_INFINITY, 1.25));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeTimeSpeedsProduct(Double.POSITIVE_INFINITY, -1.25));
        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeTimeSpeedsProduct(Double.NEGATIVE_INFINITY, -1.25));

        /*
         * zero and regular
         */
        
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsProduct(0.0, 1.25));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsProduct(0.0, -1.25));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsProduct(-0.0, 1.25));
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsProduct(-0.0, -1.25));

        assertEquals(0.0, ClocksUtils.computeTimeSpeedsProduct(1.25, 0.0));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsProduct(-1.25, 0.0));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsProduct(1.25, -0.0));
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsProduct(-1.25, -0.0));

        /*
         * regular
         */
        
        assertEquals(1.875, ClocksUtils.computeTimeSpeedsProduct(1.5, 1.25));
        assertEquals(-1.875, ClocksUtils.computeTimeSpeedsProduct(1.5, -1.25));
        assertEquals(-1.875, ClocksUtils.computeTimeSpeedsProduct(-1.5, 1.25));
        assertEquals(1.875, ClocksUtils.computeTimeSpeedsProduct(-1.5, -1.25));
    }

    public void test_computeTimeSpeedsRatio_2double() {
        /*
         * NaN's
         */
        
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.NaN, 0.0));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.NaN, -0.0));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.NaN, Double.POSITIVE_INFINITY));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.NaN, Double.NEGATIVE_INFINITY));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.NaN, 1.25));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.NaN, -1.25));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(0.0, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(-0.0, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.POSITIVE_INFINITY, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.NEGATIVE_INFINITY, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(1.25, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(-1.25, Double.NaN));
        
        /*
         * zeroes
         */
        
        assertEquals(1.0, ClocksUtils.computeTimeSpeedsRatio(0.0, 0.0));
        assertEquals(-1.0, ClocksUtils.computeTimeSpeedsRatio(0.0, -0.0));
        assertEquals(-1.0, ClocksUtils.computeTimeSpeedsRatio(-0.0, 0.0));
        assertEquals(1.0, ClocksUtils.computeTimeSpeedsRatio(-0.0, -0.0));

        /*
         * infinities
         */
        
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsRatio(0.0, Double.POSITIVE_INFINITY));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsRatio(0.0, Double.NEGATIVE_INFINITY));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsRatio(-0.0, Double.POSITIVE_INFINITY));
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsRatio(-0.0, Double.NEGATIVE_INFINITY));

        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.POSITIVE_INFINITY, 0.0));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.NEGATIVE_INFINITY, 0.0));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.POSITIVE_INFINITY, -0.0));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(Double.NEGATIVE_INFINITY, -0.0));

        assertEquals(0.0, ClocksUtils.computeTimeSpeedsRatio(1.25, Double.POSITIVE_INFINITY));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsRatio(1.25, Double.NEGATIVE_INFINITY));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsRatio(-1.25, Double.POSITIVE_INFINITY));
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsRatio(-1.25, Double.NEGATIVE_INFINITY));

        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeTimeSpeedsRatio(Double.POSITIVE_INFINITY, 1.25));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeTimeSpeedsRatio(Double.NEGATIVE_INFINITY, 1.25));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeTimeSpeedsRatio(Double.POSITIVE_INFINITY, -1.25));
        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeTimeSpeedsRatio(Double.NEGATIVE_INFINITY, -1.25));

        /*
         * zero and regular
         */
        
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsRatio(0.0, 1.25));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsRatio(0.0, -1.25));
        assertEquals(-0.0, ClocksUtils.computeTimeSpeedsRatio(-0.0, 1.25));
        assertEquals(0.0, ClocksUtils.computeTimeSpeedsRatio(-0.0, -1.25));

        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(1.25, 0.0));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(-1.25, 0.0));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(1.25, -0.0));
        assertEquals(Double.NaN, ClocksUtils.computeTimeSpeedsRatio(-1.25, -0.0));

        /*
         * regular
         */
        
        assertEquals(1.2, ClocksUtils.computeTimeSpeedsRatio(1.5, 1.25));
        assertEquals(-1.2, ClocksUtils.computeTimeSpeedsRatio(1.5, -1.25));
        assertEquals(-1.2, ClocksUtils.computeTimeSpeedsRatio(-1.5, 1.25));
        assertEquals(1.2, ClocksUtils.computeTimeSpeedsRatio(-1.5, -1.25));
    }
    
    public void test_computeAbsoluteTimeSpeed_InterfaceClock() {
        InterfaceClock clock;
        
        clock = new MyClock(2.0);
        assertEquals(2.0, ClocksUtils.computeAbsoluteTimeSpeed(clock));
        
        clock = new MyEnslavedClock(3.0, clock);
        assertEquals(6.0, ClocksUtils.computeAbsoluteTimeSpeed(clock));
        
        clock = new MyEnslavedClock(5.0, clock);
        assertEquals(30.0, ClocksUtils.computeAbsoluteTimeSpeed(clock));
    }

    public void test_computeRootTimeNs_InterfaceEnslavedClock_long() {
        MyEnslavedClock clock;
        
        assertEquals(12, ClocksUtils.computeRootTimeNs(new MyClock(), 12));
        
        clock = new MyEnslavedClock(new MyClock());
        clock.factorToMasterTime = 2;
        clock.offsetToMasterTime = 3;
        assertEquals(12 * 2 + 3, ClocksUtils.computeRootTimeNs(clock, 12));

        clock = new MyEnslavedClock(clock);
        clock.factorToMasterTime = 3;
        clock.offsetToMasterTime = 4;
        assertEquals((13 * 3 + 4) * 2 + 3, ClocksUtils.computeRootTimeNs(clock, 13));
    }
    
    public void test_computeDtNsMulTimeSpeed_long_double() {
        assertEquals(0, ClocksUtils.computeDtNsMulTimeSpeed(0, Double.NaN));
        
        assertEquals(0, ClocksUtils.computeDtNsMulTimeSpeed(0, Double.POSITIVE_INFINITY));
        assertEquals(0, ClocksUtils.computeDtNsMulTimeSpeed(0, Double.NEGATIVE_INFINITY));

        assertEquals(Long.MAX_VALUE, ClocksUtils.computeDtNsMulTimeSpeed(2, Double.POSITIVE_INFINITY));
        assertEquals(Long.MIN_VALUE, ClocksUtils.computeDtNsMulTimeSpeed(2, Double.NEGATIVE_INFINITY));
        assertEquals(Long.MIN_VALUE, ClocksUtils.computeDtNsMulTimeSpeed(-2, Double.POSITIVE_INFINITY));
        assertEquals(Long.MAX_VALUE, ClocksUtils.computeDtNsMulTimeSpeed(-2, Double.NEGATIVE_INFINITY));

        assertEquals(15, ClocksUtils.computeDtNsMulTimeSpeed(10, 1.5));
        
        // No precision loss for timeSpeed = 1.
        for (long preciseNs : new long[] {
                Long.MIN_VALUE,
                Long.MIN_VALUE/2,
                -1, 0, 1,
                Long.MAX_VALUE/2,
                Long.MAX_VALUE,
        }) {
            assertEquals(preciseNs, ClocksUtils.computeDtNsMulTimeSpeed(preciseNs, 1.0));
        }
    }

    public void test_computeDtNsDivTimeSpeed_long_double() {
        assertEquals(0, ClocksUtils.computeDtNsDivTimeSpeed(0, Double.NaN));
        assertEquals(0, ClocksUtils.computeDtNsDivTimeSpeed(1, Double.NaN));
        
        assertEquals(0, ClocksUtils.computeDtNsDivTimeSpeed(0, 0.0));
        assertEquals(0, ClocksUtils.computeDtNsDivTimeSpeed(0, -0.0));

        assertEquals(Long.MAX_VALUE, ClocksUtils.computeDtNsDivTimeSpeed(2, 0.0));
        assertEquals(Long.MIN_VALUE, ClocksUtils.computeDtNsDivTimeSpeed(2, -0.0));
        assertEquals(Long.MIN_VALUE, ClocksUtils.computeDtNsDivTimeSpeed(-2, 0.0));
        assertEquals(Long.MAX_VALUE, ClocksUtils.computeDtNsDivTimeSpeed(-2, -0.0));

        assertEquals(5, ClocksUtils.computeDtNsDivTimeSpeed(15, 3.0));
        
        // No precision loss for timeSpeed = 1.
        for (long preciseNs : new long[] {
                Long.MIN_VALUE,
                Long.MIN_VALUE/2,
                -1, 0, 1,
                Long.MAX_VALUE/2,
                Long.MAX_VALUE,
        }) {
            assertEquals(preciseNs, ClocksUtils.computeDtNsDivTimeSpeed(preciseNs, 1.0));
        }
    }

    public void test_computeDtSMulTimeSpeed_2double() {
        assertEquals(Double.NaN, ClocksUtils.computeDtSMulTimeSpeed(0.0, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeDtSMulTimeSpeed(Double.NaN, 0.0));
        
        assertEquals(0.0, ClocksUtils.computeDtSMulTimeSpeed(0.0, Double.POSITIVE_INFINITY));
        assertEquals(-0.0, ClocksUtils.computeDtSMulTimeSpeed(0.0, Double.NEGATIVE_INFINITY));
        assertEquals(-0.0, ClocksUtils.computeDtSMulTimeSpeed(-0.0, Double.POSITIVE_INFINITY));
        assertEquals(0.0, ClocksUtils.computeDtSMulTimeSpeed(-0.0, Double.NEGATIVE_INFINITY));

        assertEquals(0.0, ClocksUtils.computeDtSMulTimeSpeed(Double.POSITIVE_INFINITY, 0.0));
        assertEquals(-0.0, ClocksUtils.computeDtSMulTimeSpeed(Double.POSITIVE_INFINITY, -0.0));
        assertEquals(-0.0, ClocksUtils.computeDtSMulTimeSpeed(Double.NEGATIVE_INFINITY, 0.0));
        assertEquals(0.0, ClocksUtils.computeDtSMulTimeSpeed(Double.NEGATIVE_INFINITY, -0.0));

        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeDtSMulTimeSpeed(Double.POSITIVE_INFINITY, 1.2));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeDtSMulTimeSpeed(Double.POSITIVE_INFINITY, -1.2));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeDtSMulTimeSpeed(Double.NEGATIVE_INFINITY, 1.2));
        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeDtSMulTimeSpeed(Double.NEGATIVE_INFINITY, -1.2));

        assertEquals(1.1 * 1.2, ClocksUtils.computeDtSMulTimeSpeed(1.1, 1.2));
    }

    public void test_computeDtSDivTimeSpeed_2double() {
        assertEquals(Double.NaN, ClocksUtils.computeDtSDivTimeSpeed(0.0, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeDtSDivTimeSpeed(1.0, Double.NaN));
        assertEquals(Double.NaN, ClocksUtils.computeDtSDivTimeSpeed(Double.NaN, 0.0));
        assertEquals(Double.NaN, ClocksUtils.computeDtSDivTimeSpeed(Double.NaN, 1.0));
        
        assertEquals(0.0, ClocksUtils.computeDtSDivTimeSpeed(0.0, 0.0));
        assertEquals(-0.0, ClocksUtils.computeDtSDivTimeSpeed(0.0, -0.0));
        assertEquals(-0.0, ClocksUtils.computeDtSDivTimeSpeed(-0.0, 0.0));
        assertEquals(0.0, ClocksUtils.computeDtSDivTimeSpeed(-0.0, -0.0));

        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeDtSDivTimeSpeed(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeDtSDivTimeSpeed(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeDtSDivTimeSpeed(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeDtSDivTimeSpeed(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));

        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeDtSDivTimeSpeed(Double.POSITIVE_INFINITY, 0.0));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeDtSDivTimeSpeed(Double.POSITIVE_INFINITY, -0.0));
        assertEquals(Double.NEGATIVE_INFINITY, ClocksUtils.computeDtSDivTimeSpeed(Double.NEGATIVE_INFINITY, 0.0));
        assertEquals(Double.POSITIVE_INFINITY, ClocksUtils.computeDtSDivTimeSpeed(Double.NEGATIVE_INFINITY, -0.0));

        assertEquals(0.75, ClocksUtils.computeDtSDivTimeSpeed(1.5, 2.0));
    }

    public void test_computeSystemWaitTimeNs_long_InterfaceClock() {
        InterfaceClock clock;
        
        // absolute time speed of 6 (to check we correctly compute absolute time speed)
        clock = new MyClock(2.0);
        clock = new MyEnslavedClock(3.0, clock);
        assertEquals(12 / 6, ClocksUtils.computeSystemWaitTimeNs(12, clock));

        clock = new MyClock(1.0);
        assertEquals(0, ClocksUtils.computeSystemWaitTimeNs(0, clock));

        // underflow from positive values
        clock = new MyClock(2.0);
        assertEquals(1, ClocksUtils.computeSystemWaitTimeNs(1, clock));
        
        // underflow from negative values
        clock = new MyClock(-2.0);
        assertEquals(0, ClocksUtils.computeSystemWaitTimeNs(1, clock));
    }

    public void test_computeSystemWaitTimeNs_long_double() {
        assertEquals(12 / 2, ClocksUtils.computeSystemWaitTimeNs(12, 2.0));
        
        assertEquals(0, ClocksUtils.computeSystemWaitTimeNs(0, 1.0));
        
        // underflow from positive values
        assertEquals(1, ClocksUtils.computeSystemWaitTimeNs(1, 2.0));
        
        // underflow from negative values
        assertEquals(0, ClocksUtils.computeSystemWaitTimeNs(1, -2.0));
    }

    public void test_addListenerToAll_InterfaceClock_InterfaceClockModificationListener() {
        InterfaceClock clock;
        MyListenableClock listenableClock1;
        MyEnslavedListenableClock listenableClock2;
        
        listenableClock1 = new MyListenableClock();
        clock = new MyEnslavedClock(listenableClock1);
        listenableClock2 = new MyEnslavedListenableClock(clock);
        clock = new MyEnslavedClock(listenableClock2);
        
        MyClockListener listener = new MyClockListener();
        
        ClocksUtils.addListenerToAll(listenableClock2, listener);
        assertEquals(listener, listenableClock1.added);
        assertEquals(listener, listenableClock2.added);
    }

    public void test_removeListenerFromAll_InterfaceClock_InterfaceClockModificationListener() {
        InterfaceClock clock;
        MyListenableClock listenableClock1;
        MyEnslavedListenableClock listenableClock2;
        
        listenableClock1 = new MyListenableClock();
        clock = new MyEnslavedClock(listenableClock1);
        listenableClock2 = new MyEnslavedListenableClock(clock);
        clock = new MyEnslavedClock(listenableClock2);
        
        MyClockListener listener = new MyClockListener();
        
        ClocksUtils.removeListenerFromAll(listenableClock2, listener);
        assertEquals(listener, listenableClock1.removed);
        assertEquals(listener, listenableClock2.removed);
    }
}
