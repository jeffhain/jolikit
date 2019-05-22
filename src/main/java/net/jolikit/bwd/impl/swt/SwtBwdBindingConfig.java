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
package net.jolikit.bwd.impl.swt;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.OsUtils;

public class SwtBwdBindingConfig extends BaseBwdBindingConfig {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * Can be null.
     */
    private String locale = null;
    
    /**
     * TODO swt On Windows, undecorated windows actually have some insets,
     * with a black border.
     */
    private GRect undecoratedInsets = (OsUtils.isWindows() ? GRect.valueOf(1, 1, 1, 1) : GRect.DEFAULT_EMPTY);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses default values for parameters not in arguments.
     */
    public SwtBwdBindingConfig() {
        
        /*
         * TODO swt On Mac, can't preserve pixels from previous paintings,
         * so we need to do that.
         */
        this.setMustMakeAllDirtyAtEachPainting_final(OsUtils.isMac());
        
        // True because it's advised if true for can display.
        this.setMustUseFontBoxForFontKind_final(true);
        // True because SWT offers really no way to tell.
        this.setMustUseFontBoxForCanDisplay_final(true);
        
        /*
         * TODO swt For size of 1, sometimes uses a default size instead.
         * Ex.: Aharoni, size = 1: backing ascent = 12, backing descent = 4.
         * 
         * Also for size of 2 to 7.
         * Ex.: Courier, size = 2: backing ascent = 11, backing descent = 2.
         * Ex.: Courier, size = 7: backing ascent = 11, backing descent = 2.
         */
        this.setMinRawFontSize_final(8);
        
        /*
         * TODO swt For huge or even just large sizes,
         * ascent/descent start to be crazy.
         * Ex.: Aharoni, size = 122880: backing ascent = 1, backing descent = 1. 
         * Ex.: FreeMonoOblique, size = 15360: backing ascent = -1.34248036E8, backing descent = 0.
         * Ex.: System, size = 240: backing ascent = 80, backing descent = 24 (same as for size = 120).
         */
        this.setMaxRawFontSize_final(120);

        /*
         * TODO swt On mac, if doing that, host unit test fails at step 1499
         * due to demaximize failing to demaximize.
         * NB: Weirdly, deactivating focus gain on maximize instead
         * also makes test pass.
         */
        this.setMustRequestFocusGainOnDemaximize_final(!OsUtils.isMac());
    }

    public String getLocale() {
        return this.locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public GRect getUndecoratedInsets() {
        return this.undecoratedInsets;
    }

    public void setUndecoratedInsets(GRect undecoratedInsets) {
        this.undecoratedInsets = undecoratedInsets;
    }
}
