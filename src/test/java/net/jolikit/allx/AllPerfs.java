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
package net.jolikit.allx;

import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtilsPerf;
import net.jolikit.bwd.impl.utils.fonts.FontBoxHelperPerf;
import net.jolikit.bwd.impl.utils.gprim.DefaultLineDrawerPerf;
import net.jolikit.bwd.impl.utils.gprim.DefaultPolyDrawerPerf;
import net.jolikit.bwd.impl.utils.gprim.GprimUtilsPerf;
import net.jolikit.bwd.impl.utils.gprim.MidPointArcDrawerPerf;
import net.jolikit.bwd.impl.utils.gprim.PolyArcDrawerPerf;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawerPerf;
import net.jolikit.lang.HeisenLoggerPerf;
import net.jolikit.lang.NbrsUtilsPerf;
import net.jolikit.lang.ThinTimePerf;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.prl.ParallelizersPerf;
import net.jolikit.time.sched.hard.HardExecutorsPerf;

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
        DefaultPolyDrawerPerf.newRun(args);
        GprimUtilsPerf.newRun(args);
        MidPointArcDrawerPerf.newRun(args);
        PolyArcDrawerPerf.newRun(args);
        
        /*
         * net.jolikit.bwd.impl.utils.graphics
         */
        
        ScaledRectDrawerPerf.newRun(args);
        
        /*
         * net.jolikit.bwd.impl.utils.fonts
         */
        
        FontBoxHelperPerf.newRun(args);
        
        /*
         * net.jolikit.lang
         */
        
        HeisenLoggerPerf.newRun(args);
        NbrsUtilsPerf.newRun(args);
        ThinTimePerf.newRun(args);
        
        /*
         * net.jolikit.threading.prl
         */
        
        ParallelizersPerf.newRun(args);
        
        /*
         * net.jolikit.time.sched.hard
         */
        
        HardExecutorsPerf.newRun(args);
    }
}
