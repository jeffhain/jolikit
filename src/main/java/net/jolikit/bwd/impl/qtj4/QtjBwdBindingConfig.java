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
package net.jolikit.bwd.impl.qtj4;

import com.trolltech.qt.gui.QFontDatabase.WritingSystem;

import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.lang.OsUtils;

public class QtjBwdBindingConfig extends BaseBwdBindingConfig {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * The closest thing to a Java Locale (QLocale only
     * concerns numbers and currencies).
     * Must not be null.
     */
    private WritingSystem writingSystem = WritingSystem.Any;
    
    /**
     * TODO qtj On Windows, when ALT_GRAPH is pressed, Qt generates:
     * CONTROL-pressed, then ALT-pressed, and on release:
     * CONTROL-released, and then ALT-released.
     * We have no way to rule out the rogue Control press/release,
     * which looks in every way like an actual one, but we can
     * properly figure out that Alt is actually AlrGraph using
     * the configured native scan code, which is what we do
     * when this boolean is true.
     * On Mac, ALT is generated for both keys, but QKeyEvent.nativeScanCode()
     * seem to be broken as it returns 0 for a lot of keys including
     * Alt and AltGraph, so we can't do the trick.
     */
    private boolean mustSynthesizeAltGraph = OsUtils.isWindows();
    
    /**
     * QKeyEvent.nativeScanCode() value for AltGraph.
     * Only used if mustSynthesizeAltGraph is true.
     */
    private int altGraphNativeScanCode = 312;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses default values for parameters not in arguments.
     */
    public QtjBwdBindingConfig() {
        
        /*
         * TODO qtj Issues with fonts of size 1 to 4.
         * Ex.: family = MS Sans Serif, size = 1: backing ascent = 11, backing descent = 1.
         *      (...)
         * Ex.: family = MS Sans Serif, size = 4: backing ascent = 11, backing descent = 1.
         * And size 5:
         * Ex.: family = System, size = 5: backing ascent = 13, backing descent = 2.
         */
        this.setMinRawFontSize_final(6);
        
        /*
         * TODO qtj Issues with fonts of huge sizes.
         * Ex.: family = Aharoni, size = 122880: backing ascent = 1, backing descent = 0.
         * Ex.: family = FreeMono, style = italic, size = 15000: backing ascent = 0, backing descent = -1.
         * Also, Qt seems to output the following errors in these cases:
         * "QFontEngine::loadEngine: GetTextMetrics failed ()"
         * "QFontEngineWin: GetTextMetrics failed ()" (twice)
         */
        this.setMaxRawFontSize_final(Short.MAX_VALUE / 3);
        
        /*
         * TODO qtj On Mac, on maximization, readable spans are properly updated,
         * but readable position is unchanged, even though the window is visually
         * properly maximized, and there is a similar issue on demaximization.
         */
        this.setMustRestoreBoundsOnShowDeicoDemax_final(OsUtils.isMac());
        this.setMustEnforceBoundsOnShowDeicoMax_final(OsUtils.isMac());
    }
    
    /*
     * 
     */

    public WritingSystem getWritingSystem() {
        return this.writingSystem;
    }

    public void setWritingSystem(WritingSystem writingSystem) {
        this.writingSystem = writingSystem;
    }

    public boolean getMustSynthesizeAltGraph() {
        return this.mustSynthesizeAltGraph;
    }

    public void setMustSynthesizeAltGraph(boolean mustSynthesizeAltGraph) {
        this.mustSynthesizeAltGraph = mustSynthesizeAltGraph;
    }

    public int getAltGraphNativeScanCode() {
        return this.altGraphNativeScanCode;
    }

    public void setAltGraphNativeScanCode(int altGraphNativeScanCode) {
        this.altGraphNativeScanCode = altGraphNativeScanCode;
    }
}
