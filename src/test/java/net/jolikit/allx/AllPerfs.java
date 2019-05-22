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
package net.jolikit.allx;

import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtilsPerf;
import net.jolikit.bwd.impl.utils.fonts.FontBoxHelperPerf;
import net.jolikit.bwd.impl.utils.gprim.DefaultLineDrawerPerf;
import net.jolikit.bwd.impl.utils.gprim.GprimUtilsPerf;
import net.jolikit.bwd.impl.utils.gprim.OvalOrArcPerf;
import net.jolikit.lang.HeisenLoggerPerf;
import net.jolikit.lang.NumbersUtilsPerf;
import net.jolikit.lang.ThinTimePerf;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.prl.ParallelizersPerf;
import net.jolikit.time.sched.hard.HardSchedulersPerf;

public class AllPerfs {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        System.out.println(TestUtils.getJVMInfo());

        /*
         * net.jolikit.bwd.impl.util.basics
         */
        
        BindingCoordsUtilsPerf.newRun(args);
        
        /*
         * net.jolikit.bwd.impl.utils.gprim
         */
        
        DefaultLineDrawerPerf.newRun(args);
        GprimUtilsPerf.newRun(args);
        OvalOrArcPerf.newRun(args);
        
        /*
         * net.jolikit.bwd.impl.utils.fonts
         */
        
        FontBoxHelperPerf.newRun(args);
        
        /*
         * net.jolikit.lang
         */
        
        HeisenLoggerPerf.newRun(args);
        NumbersUtilsPerf.newRun(args);
        ThinTimePerf.newRun(args);
        
        /*
         * net.jolikit.threading.prl
         */
        
        ParallelizersPerf.newRun(args);
        
        /*
         * net.jolikit.time.sched.hard
         */
        
        HardSchedulersPerf.newRun(args);
    }
}
