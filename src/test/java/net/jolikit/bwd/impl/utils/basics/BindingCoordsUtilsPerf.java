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
package net.jolikit.bwd.impl.utils.basics;

import java.util.Random;

import net.jolikit.test.utils.TestUtils;

public class BindingCoordsUtilsPerf {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_CALLS = 100 * 1000 * 1000;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static void main(String[] args) {
        newRun(args);
    }

    public static void newRun(String[] args) {
        new BindingCoordsUtilsPerf().run(args);
    }
    
    public BindingCoordsUtilsPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void run(String[] args) {
        System.out.println("--- " + BindingCoordsUtilsPerf.class.getSimpleName() + "... ---");
        
        for (int maxMagnitude : new int[]{
                1000,
                1000 * 1000,
                1000 * 1000 * 1000,
                Integer.MAX_VALUE
        }) {
            System.out.println();
            bench_roundToInt_double_integers(maxMagnitude);
        }
        
        for (double maxMagnitude : new double[]{
                1e3,
                1e6,
                1.1 * Integer.MAX_VALUE,
                2.0 * Integer.MAX_VALUE,
                1e3 * Integer.MAX_VALUE,
                1e6 * Integer.MAX_VALUE,
                1e9 * Integer.MAX_VALUE
        }) {
            System.out.println();
            bench_roundToInt_double_nonIntegers(maxMagnitude);
        }

        System.out.println("--- ..." + BindingCoordsUtilsPerf.class.getSimpleName() + " ---");
    }
    
    private void bench_roundToInt_double_nonIntegers(double maxMagnitude) {
        final Random random = TestUtils.newRandom123456789L();
        
        // Power of two.
        final int nbrOfValues = (1<<10);
        final int mask = nbrOfValues - 1;
        final double[] values = new double[nbrOfValues];
        for (int i = 0; i < nbrOfValues; i++) {
            values[i] = (1.0 - 2.0  * random.nextDouble()) * maxMagnitude;
        }
        
        final int nbrOfRuns = 4;
        final int nbrOfCalls = NBR_OF_CALLS;
        
        int antiOptim = 0;
        
        for (int k = 0; k < nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < nbrOfCalls; i++) {
                antiOptim += BindingCoordsUtils.roundToInt(values[i & mask]);
            }
            long b = System.nanoTime();
            System.out.println(nbrOfCalls + " calls to roundToInt(double), non-integer values up to " + maxMagnitude + ", took " + TestUtils.nsToSRounded(b-a) + " s");
        }
        
        TestUtils.blackHole(antiOptim);
    }
    
    private void bench_roundToInt_double_integers(int maxMagnitude) {
        final Random random = TestUtils.newRandom123456789L();
        
        // Power of two.
        final int nbrOfValues = (1<<10);
        final int mask = nbrOfValues - 1;
        final double[] values = new double[nbrOfValues];
        for (int i = 0; i < nbrOfValues; i++) {
            values[i] = maxMagnitude - 2.0 * (1 + random.nextInt(maxMagnitude));
        }
        
        final int nbrOfRuns = 4;
        final int nbrOfCalls = NBR_OF_CALLS;
        
        int antiOptim = 0;
        
        for (int k = 0; k < nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < nbrOfCalls; i++) {
                antiOptim += BindingCoordsUtils.roundToInt(values[i & mask]);
            }
            long b = System.nanoTime();
            System.out.println(nbrOfCalls + " calls to roundToInt(double), integer values up to " + maxMagnitude + ", took " + TestUtils.nsToSRounded(b-a) + " s");
        }
        
        TestUtils.blackHole(antiOptim);
    }
}
