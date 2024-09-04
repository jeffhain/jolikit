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
package net.jolikit.bwd.impl.utils.fonts;

import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.test.utils.TestUtils;

public class FontBoxHelperPerf {
    
    /*
     * With a few FontBox and array-sets optimizations,
     * for Unicode font, went from
     * 100 calls to computeCodePointSetElseNull(int) took 0.355 s
     * to 
     * 100 calls to computeCodePointSetElseNull(int) took 0.075 s
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int NBR_OF_RUNS = 4 * 1000;
    
    private static final int NBR_OF_CALLS = 100;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println(TestUtils.getJVMInfo());
        newRun(args);
    }

    public static void newRun(String[] args) {
        new FontBoxHelperPerf().run(args);
    }

    public FontBoxHelperPerf() {
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void run(String[] args) {
        final long a = System.nanoTime();
        System.out.println("--- " + FontBoxHelperPerf.class.getSimpleName() + "... ---");
        System.out.println("number of calls = " + NBR_OF_CALLS);
        
        bench_getDisplayableCodePointSet_int(BwdTestResources.TEST_FONT_UNIFONT_8_0_01_TTF);
        
        final long b = System.nanoTime();
        System.out.println("--- ..." + FontBoxHelperPerf.class.getSimpleName()
            + ", " + TestUtils.nsToSRounded(b-a) + " s ---");
    }
    
    /*
     * 
     */
    
    private static void bench_getDisplayableCodePointSet_int(String fontFilePath) {
        final FontBoxHelper helper = new FontBoxHelper(fontFilePath);
        try {
            final int fontIndex = 0;
            
            for (int k = 0; k < NBR_OF_RUNS; k++) {
                long a = System.nanoTime();
                for (int i = 0; i < NBR_OF_CALLS; i++) {
                    final CodePointSet cps = helper.computeCodePointSetElseNull(fontIndex);
                    // Anti-optim check.
                    if (cps.getCodePointCount() == 0) {
                        throw new AssertionError();
                    }
                }
                long b = System.nanoTime();
                System.out.println(NBR_OF_CALLS + " calls to computeCodePointSetElseNull(int) took " + TestUtils.nsToSRounded(b-a) + " s");
            }
        } finally {
            helper.close();
        }
    }
}
