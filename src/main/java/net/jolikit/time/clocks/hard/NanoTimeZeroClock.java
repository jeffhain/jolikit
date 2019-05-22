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
package net.jolikit.time.clocks.hard;

import net.jolikit.lang.LangUtils;
import net.jolikit.time.TimeUtils;

/**
 * Hard clock based on System.nanoTime(), starting at zero.
 */
public class NanoTimeZeroClock implements InterfaceHardClock {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final NanoTimeZeroClock DEFAULT_INSTANCE = new NanoTimeZeroClock();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public NanoTimeZeroClock() {
    }
    
    /**
     * @return A default instance.
     */
    public static NanoTimeZeroClock getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public double getTimeS() {
        return TimeUtils.nsToS(this.getTimeNs());
    }

    @Override
    public long getTimeNs() {
        return LangUtils.getNanoTimeFromClassLoadNs();
    }
    
    /**
     * @return 1.0
     */
    @Override
    public double getTimeSpeed() {
        return 1.0;
    }
}
