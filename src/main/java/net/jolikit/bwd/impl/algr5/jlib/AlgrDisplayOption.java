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
package net.jolikit.bwd.impl.algr5.jlib;

import net.jolikit.bwd.impl.utils.basics.IntValuedHelper;
import net.jolikit.bwd.impl.utils.basics.IntValuedHelper.InterfaceIntValued;

/**
 * enum ALLEGRO_DISPLAY_OPTIONS
 * 
 * Possible parameters for al_set_display_option.
 * Make sure to update ALLEGRO_EXTRA_DISPLAY_SETTINGS if you modify
 * anything here.
 */
public enum AlgrDisplayOption implements InterfaceIntValued {
    ALLEGRO_RED_SIZE,
    ALLEGRO_GREEN_SIZE,
    ALLEGRO_BLUE_SIZE,
    ALLEGRO_ALPHA_SIZE,
    ALLEGRO_RED_SHIFT,
    ALLEGRO_GREEN_SHIFT,
    ALLEGRO_BLUE_SHIFT,
    ALLEGRO_ALPHA_SHIFT,
    ALLEGRO_ACC_RED_SIZE,
    ALLEGRO_ACC_GREEN_SIZE,
    ALLEGRO_ACC_BLUE_SIZE,
    ALLEGRO_ACC_ALPHA_SIZE,
    ALLEGRO_STEREO,
    ALLEGRO_AUX_BUFFERS,
    ALLEGRO_COLOR_SIZE,
    ALLEGRO_DEPTH_SIZE,
    ALLEGRO_STENCIL_SIZE,
    ALLEGRO_SAMPLE_BUFFERS,
    ALLEGRO_SAMPLES,
    ALLEGRO_RENDER_METHOD,
    ALLEGRO_FLOAT_COLOR,
    ALLEGRO_FLOAT_DEPTH,
    ALLEGRO_SINGLE_BUFFER,
    ALLEGRO_SWAP_METHOD,
    ALLEGRO_COMPATIBLE_DISPLAY,
    ALLEGRO_UPDATE_DISPLAY_REGION,
    ALLEGRO_VSYNC,
    ALLEGRO_MAX_BITMAP_SIZE,
    ALLEGRO_SUPPORT_NPOT_BITMAP,
    ALLEGRO_CAN_DRAW_INTO_BITMAP,
    ALLEGRO_SUPPORT_SEPARATE_ALPHA,
    ALLEGRO_AUTO_CONVERT_BITMAPS,
    ALLEGRO_SUPPORTED_ORIENTATIONS,
    ALLEGRO_OPENGL_MAJOR_VERSION,
    ALLEGRO_OPENGL_MINOR_VERSION,
    ALLEGRO_DISPLAY_OPTIONS_COUNT;
    
    private static final IntValuedHelper<AlgrDisplayOption> HELPER =
            new IntValuedHelper<AlgrDisplayOption>(AlgrDisplayOption.values());
    
    @Override
    public int intValue() {
        return this.ordinal();
    }
    
    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static AlgrDisplayOption valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
