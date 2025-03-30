/*
 * Copyright 2019-2025 Jeff Hain
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
package net.jolikit.bwd.impl.awt;

import java.util.Locale;

import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.lang.OsUtils;

public class AwtBwdBindingConfig extends BaseBwdBindingConfig {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private Locale locale = Locale.ENGLISH;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses default values for parameters not in arguments.
     */
    public AwtBwdBindingConfig() {
        
        /*
         * TODO awt Issues for size of 1.
         * Ex.: family = Aharoni, style = bold, size = 1: backing ascent = 0, backing descent = 0.
         */
        this.setMinRawFontSize_final(2);
        
        /*
         * TODO awt Issues for huge sizes, but it seems only above Short.MAX_VALUE (default).
         * Ex.: family = Aharoni, style = bold, size = 61440: backing ascent = 0, backing descent = 0.
         */
        if (false) {
            this.setMaxRawFontSize_final(Short.MAX_VALUE);
        }
        
        /*
         * True because we use AWT drawImage() only in use cases
         * where it's both accurate and fast (all images being (ARGB,premul)),
         * and want to benefit from its speed.
         */
        this.setMustUseBackingImageScalingIfApplicable_final(true);

        /*
         * TODO awt iconified/deiconified/iconified flickering
         * can be quite long on Mac.
         * NB: Now that we have stall detections, and grow timeouts accordingly,
         * we might no longer need that, but could not really check it since,
         * on Mac, host unit test freezes after a few dozen steps.
         */
        this.setBackingWindowStateAntiFlickeringDelayS_final(OsUtils.isMac() ? 0.3 : 0.0);
        
        /*
         * TODO awt False by default because insets are not available while not showing,
         * and backing bounds are set on window.
         */
        this.setMustUseDefaultBoundsForClientElseWindow_final(false);
        
        /*
         * TODO awt We don't want to do that, because when hidden (or iconified?),
         * insets are not available, and therefore conversions between client
         * and window bounds are incorrect (if user specifies client bounds,
         * we need to convert to window bounds because that's how bounds
         * are set into the backing library).
         */
        this.setMustSetBackingDemaxBoundsWhileHiddenOrIconified_final(false);
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }
}
