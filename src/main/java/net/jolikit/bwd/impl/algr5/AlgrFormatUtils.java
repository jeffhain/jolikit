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
package net.jolikit.bwd.impl.algr5;

import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.algr5.jlib.AlgrPixelFormat;

import com.sun.jna.Pointer;

public class AlgrFormatUtils {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param format A bitmap format.
     * @return True if the specified format can be considered to be
     *         32 bits ARGB, false otherwise.
     */
    public static boolean isBitmapFormat32BitsArgbIsh(int format) {
        return (format == AlgrPixelFormat.ALLEGRO_PIXEL_FORMAT_ARGB_8888.intValue())
                || (format == AlgrPixelFormat.ALLEGRO_PIXEL_FORMAT_XRGB_8888.intValue());
    }

    /**
     * @param bitmap A bitmap.
     * @return Format to use for al_lock_bitmap(...) or al_lock_bitmap_region(...).
     */
    public static int getLockFormat(Pointer bitmap) {
        final int formatToUse;
        final int bitmapFormat = LIB.al_get_bitmap_format(bitmap);
        if (isBitmapFormat32BitsArgbIsh(bitmapFormat)) {
            /*
             * Bitmap format suits us (for a "memcopy" of array pixels):
             * using it for (way) faster locking.
             */
            formatToUse = bitmapFormat;
        } else {
            /*
             * TODO algr Allegro will have to convert the format,
             * which can be extremely slow
             * (from ALLEGRO_PIXEL_FORMAT_XRGB_8888
             * to ALLEGRO_PIXEL_FORMAT_ARGB_8888 for example).
             * If we ever actually pass here, we might want to use
             * bitmap format instead, and do the format conversion
             * ourselves when reading/writing pixels.
             */
            formatToUse = AlgrPixelFormat.ALLEGRO_PIXEL_FORMAT_ARGB_8888.intValue();
        }
        return formatToUse;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private AlgrFormatUtils() {
    }
}
