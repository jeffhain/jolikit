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

public enum AlgrPixelFormat implements InterfaceIntValued {
    ALLEGRO_PIXEL_FORMAT_ANY,
    ALLEGRO_PIXEL_FORMAT_ANY_NO_ALPHA,
    ALLEGRO_PIXEL_FORMAT_ANY_WITH_ALPHA,
    ALLEGRO_PIXEL_FORMAT_ANY_15_NO_ALPHA,
    ALLEGRO_PIXEL_FORMAT_ANY_16_NO_ALPHA,
    ALLEGRO_PIXEL_FORMAT_ANY_16_WITH_ALPHA,
    ALLEGRO_PIXEL_FORMAT_ANY_24_NO_ALPHA,
    ALLEGRO_PIXEL_FORMAT_ANY_32_NO_ALPHA,
    ALLEGRO_PIXEL_FORMAT_ANY_32_WITH_ALPHA,
    ALLEGRO_PIXEL_FORMAT_ARGB_8888,
    ALLEGRO_PIXEL_FORMAT_RGBA_8888,
    ALLEGRO_PIXEL_FORMAT_ARGB_4444,
    /**
     * 24 bit format
     */
    ALLEGRO_PIXEL_FORMAT_RGB_888,
    ALLEGRO_PIXEL_FORMAT_RGB_565,
    ALLEGRO_PIXEL_FORMAT_RGB_555,
    ALLEGRO_PIXEL_FORMAT_RGBA_5551,
    ALLEGRO_PIXEL_FORMAT_ARGB_1555,
    ALLEGRO_PIXEL_FORMAT_ABGR_8888,
    ALLEGRO_PIXEL_FORMAT_XBGR_8888,
    /**
     * 24 bit format
     */
    ALLEGRO_PIXEL_FORMAT_BGR_888,
    ALLEGRO_PIXEL_FORMAT_BGR_565,
    ALLEGRO_PIXEL_FORMAT_BGR_555,
    ALLEGRO_PIXEL_FORMAT_RGBX_8888,
    ALLEGRO_PIXEL_FORMAT_XRGB_8888,
    ALLEGRO_PIXEL_FORMAT_ABGR_F32,
    ALLEGRO_PIXEL_FORMAT_ABGR_8888_LE,
    ALLEGRO_PIXEL_FORMAT_RGBA_4444,
    ALLEGRO_PIXEL_FORMAT_SINGLE_CHANNEL_8,
    ALLEGRO_PIXEL_FORMAT_COMPRESSED_RGBA_DXT1,
    ALLEGRO_PIXEL_FORMAT_COMPRESSED_RGBA_DXT3,
    ALLEGRO_PIXEL_FORMAT_COMPRESSED_RGBA_DXT5,
    ALLEGRO_NUM_PIXEL_FORMATS;
    
    private static final IntValuedHelper<AlgrPixelFormat> HELPER =
            new IntValuedHelper<AlgrPixelFormat>(AlgrPixelFormat.values());
    
    @Override
    public int intValue() {
        return this.ordinal();
    }
    
    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static AlgrPixelFormat valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
