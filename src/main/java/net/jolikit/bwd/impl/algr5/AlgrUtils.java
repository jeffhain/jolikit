/*
 * Copyright 2020 Jeff Hain
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
package net.jolikit.bwd.impl.algr5;

import net.jolikit.bwd.api.graphics.Argb64;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_COLOR;

public class AlgrUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static ALLEGRO_COLOR newColor(long argb64) {
        final float alphaFp = (float) Argb64.getAlphaFp(argb64);
        final float redFp = (float) Argb64.getRedFp(argb64);
        final float greenFp = (float) Argb64.getGreenFp(argb64);
        final float blueFp = (float) Argb64.getBlueFp(argb64);
        return newColor(alphaFp, redFp, greenFp, blueFp);
    }
    
    public static ALLEGRO_COLOR newColor(
            float alphaFp,
            float redFp,
            float greenFp,
            float blueFp) {
        final ALLEGRO_COLOR color = new ALLEGRO_COLOR();
        color.r = redFp;
        color.g = greenFp;
        color.b = blueFp;
        color.a = alphaFp;
        return color;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private AlgrUtils() {
    }
}
