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
package net.jolikit.threading.prl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.prl.InterfaceSplittable;
import junit.framework.TestCase;

public class RunnableSplittableTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyRunnable implements Runnable {
        int runCount;
        @Override
        public void run() {
            this.runCount++;
        }
    }

    private static class MySplittable extends MyRunnable implements InterfaceSplittable {
        final int runQuota;
        final List<MySplittable> created;
        int runRemaining;
        /**
         * @param runQuota Number of individual calls to run() it can allow
         *        through splits, i.e. maxNbrOfSplits+1.
         * @param created Created splits are added into this list.
         */
        public MySplittable(
                int runQuota,
                List<MySplittable> created) {
            this.runQuota = runQuota;
            this.created = created;
            this.runRemaining = runQuota;
        }
        @Override
        public boolean worthToSplit() {
            return this.runRemaining > 1;
        }
        @Override
        public InterfaceSplittable split() {
            final int halfish = (this.runRemaining>>1);
            this.runRemaining -= halfish;
            MySplittable split = new MySplittable(
                    halfish,
                    this.created);
            this.created.add(split);
            return split;
        }
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_RunnableSplittable_arrayOfRunnable_2int() {
        // null array.
        try {
            new RunnableSplittable(null, 0, 1);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        // Bad from.
        for (int badFrom : new int[]{Integer.MIN_VALUE,-1,1,Integer.MAX_VALUE}) {
            try {
                new RunnableSplittable(new Runnable[1], badFrom, 1);
                fail();
            } catch (IndexOutOfBoundsException e) {
                // ok
            }
        }

        // Bad length.
        for (int badLength : new int[]{Integer.MIN_VALUE,-1,0,2,Integer.MAX_VALUE}) {
            try {
                new RunnableSplittable(new Runnable[1], 0, badLength);
                fail();
            } catch (IndexOutOfBoundsException e) {
                assertTrue(badLength != 0);
                // ok
            } catch (IllegalArgumentException e) {
                assertEquals(0, badLength);
                // ok
            }
        }

        // Ok even if no Runnable in array.
        new RunnableSplittable(new Runnable[1], 0, 1);
        
        /*
         * 
         */
        
        {
            MyRunnable r1 = new MyRunnable();
            RunnableSplittable rs = new RunnableSplittable(new Runnable[]{r1}, 0, 1);
            
            assertFalse(rs.worthToSplit());
            rs.run();
            
            assertEquals(1, r1.runCount);
        }
        
        for (boolean split : new boolean[]{false,true}) {
            MyRunnable r1 = new MyRunnable();
            MyRunnable r2 = new MyRunnable();
            RunnableSplittable rs = new RunnableSplittable(new Runnable[]{r1,r2}, 0, 2);
            
            assertTrue(rs.worthToSplit());
            if (split) {
                InterfaceSplittable splittable = rs.split();
                assertFalse(splittable.worthToSplit());
                splittable.run();
            }
            rs.run();
            
            assertEquals(1, r1.runCount);
            assertEquals(1, r2.runCount);
        }
    }

    public void test_RunnableSplittable_collectionOfRunnable() {
        // null collection.
        try {
            new RunnableSplittable(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        // Empty collection.
        try {
            new RunnableSplittable(new ArrayList<MyRunnable>());
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }

        // Ok even if no Runnable in collection.
        {
            ArrayList<MyRunnable> collection = new ArrayList<MyRunnable>();
            collection.add(null);
            new RunnableSplittable(collection);
        }
        
        /*
         * 
         */
        
        {
            MyRunnable r1 = new MyRunnable();
            ArrayList<MyRunnable> collection = new ArrayList<MyRunnable>();
            collection.add(r1);
            RunnableSplittable rs = new RunnableSplittable(collection);
            
            assertFalse(rs.worthToSplit());
            rs.run();
            
            assertEquals(1, r1.runCount);
        }
        
        for (boolean split : new boolean[]{false,true}) {
            MyRunnable r1 = new MyRunnable();
            MyRunnable r2 = new MyRunnable();
            ArrayList<MyRunnable> collection = new ArrayList<MyRunnable>();
            collection.add(r1);
            collection.add(r2);
            RunnableSplittable rs = new RunnableSplittable(collection);
            
            assertTrue(rs.worthToSplit());
            if (split) {
                InterfaceSplittable splittable = rs.split();
                assertFalse(splittable.worthToSplit());
                splittable.run();
            }
            rs.run();
            
            assertEquals(1, r1.runCount);
            assertEquals(1, r2.runCount);
        }
    }

    /**
     * Tests with only regular Runnables.
     */
    public void test_runnablesOnly() {
        final Random random = TestUtils.newRandom123456789L();
        final int size = 20;
        
        for (double splitProba : new double[]{0.0, 0.1, 0.5, 0.9, 1.0}) {
            final MyRunnable[] runnables = newMyRunnableArr(size);
            
            final RunnableSplittable splittable = new RunnableSplittable(
                    runnables,
                    0,
                    size);
            
            final int actualSplitCount = processSplitting(splittable, random, splitProba);
            
            checkEachRanOnce(runnables);
            
            if (splitProba == 1.0) {
                final int expectedSplitCount = size-1;
                assertEquals(expectedSplitCount, actualSplitCount);
            }
        }
    }

    /**
     * Tests with both regular Runnables, and splittables.
     */
    public void test_runnablesAndSplittables() {
        final Random random = TestUtils.newRandom123456789L();
        final int size = 20;
        final double splittableProba = 0.5;
        final int maxRunQuotaPerSplittable = 5;
        
        for (double splitProba : new double[]{0.0, 0.1, 0.5, 0.9, 1.0}) {
            final ArrayList<MySplittable> created = new ArrayList<MySplittable>();
            
            final MyRunnable[] runnables = newMyRunnableOrSplittableArr(
                    size,
                    random,
                    splittableProba,
                    maxRunQuotaPerSplittable,
                    created);
            
            final RunnableSplittable splittable = new RunnableSplittable(
                    runnables,
                    0,
                    size);
            
            final int actualSplitCount = processSplitting(splittable, random, splitProba);
            
            checkEachRanOnce(runnables);
            checkEachRanOnce(created.toArray(new MyRunnable[created.size()]));
            
            if (splitProba == 1.0) {
                final int expectedSplitCount = computeMaxNbrOfSplits(runnables);
                assertEquals(expectedSplitCount, actualSplitCount);
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static MyRunnable[] newMyRunnableArr(int size) {
        final MyRunnable[] runnables = new MyRunnable[size];
        for (int i = 0; i < size; i++) {
            runnables[i] = new MyRunnable();
        }
        return runnables;
    }

    private static MyRunnable[] newMyRunnableOrSplittableArr(
            int size,
            Random random,
            double splittableProba,
            int maxRunQuotaPerSplittable,
            List<MySplittable> created) {
        final MyRunnable[] runnables = new MyRunnable[size];
        for (int i = 0; i < size; i++) {
            if (randomBoolean(random, splittableProba)) {
                runnables[i] = new MySplittable(1+random.nextInt(maxRunQuotaPerSplittable), created);
            } else {
                runnables[i] = new MyRunnable();
            }
        }
        return runnables;
    }

    private static int computeMaxNbrOfSplits(MyRunnable[] runnables) {
        int res = 0;
        for (MyRunnable runnable : runnables) {
            if (runnable instanceof MySplittable) {
                MySplittable impl = (MySplittable)runnable;
                res += impl.runQuota;
            } else {
                res++;
            }
        }
        return res - 1;
    }
            

    private static boolean randomBoolean(
            Random random,
            double proba) {
        if (proba == 0.0) {
            return false;
        }
        if (proba == 1.0) {
            return true;
        }
        // Would work even without quick cases handling.
        return random.nextDouble() < proba;
    }
    
    /**
     * Process the specified splittable, sequentially.
     * 
     * This method is recursive.
     * 
     * @param splitProba Probability to split if it's worth it.
     * @return Number of splits done.
     */
    private static int processSplitting(
            InterfaceSplittable splittable,
            Random random,
            double splitProba) {
        int splitCount = 0;
        while (splittable.worthToSplit() && randomBoolean(random, splitProba)) {
            splitCount += 1 + processSplitting(
                    splittable.split(),
                    random,
                    splitProba);
        }
        // Done splitting.
        splittable.run();
        return splitCount;
    }
    
    private static void checkEachRanOnce(
            MyRunnable[] runnables) {
        for (MyRunnable runnable : runnables) {
            assertEquals(1, runnable.runCount);
        }
    }
}
