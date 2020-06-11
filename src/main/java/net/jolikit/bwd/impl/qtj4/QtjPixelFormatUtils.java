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
package net.jolikit.bwd.impl.qtj4;

import net.jolikit.bwd.impl.utils.graphics.PixelFormatHelper;

import com.trolltech.qt.gui.QImage.Format;

public class QtjPixelFormatUtils {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final PixelFormatHelper HELPER = new PixelFormatHelper();
    static {
        addFormatsInto(HELPER);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param format Format of the specified pixel.
     * @param pixel Pixel in the specified format.
     * @return The corresponding ARGB32 value, with an alpha of 0xFF (opaque)
     *         if the specified format does not have alpha.
     * @throws IllegalArgumentException if the specified format is not supported.
     * @throws NullPointerException if the specified format is null.
     */
    public static int toArgb32(Format format, int pixel) {
        return HELPER.toArgb32(format.ordinal(), pixel);
    }
    
    /**
     * @param format A pixel format.
     * @return True if the specified format is alpha-premultiplied,
     *         false otherwise.
     */
    public static boolean isAlphaPremultiplied(Format format) {
        boolean ret = false;
        switch (format) {
            case Format_ARGB32_Premultiplied:
            case Format_ARGB8565_Premultiplied:
            case Format_ARGB6666_Premultiplied:
            case Format_ARGB8555_Premultiplied:
            case Format_ARGB4444_Premultiplied:
                ret = true;
            default:
                break;
        }
        return ret;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private QtjPixelFormatUtils() {
    }
    
    private static void addFormatsInto(PixelFormatHelper helper) {
        
        /*
         * TODO qtj Some format are obscure (at least not obvious),
         * so preferring to not handle them.
         * When the binding throws due to encountering them, is the time
         * to figure out what they are and upgrade this treatment.
         */
        
        for (Format format : Format.values() ) {
            switch (format) {
                case Format_Invalid: {
                    // Not handled.
                } break;
                case Format_Mono: {
                    // Not handled.
                } break;
                case Format_MonoLSB: {
                    // Not handled.
                } break;
                case Format_Indexed8: {
                    /*
                     * TODO qtj Could have that for a 4 bits BMP image,
                     * for which pixels were in ARGB32 format.
                     * We use zero alpha mask, in case alpha would ever
                     * be missing from backing pixel.
                     */
                    helper.addFormat(format.ordinal(), 0, (0xFF << 16), (0xFF << 8), 0xFF);
                } break;
                case Format_RGB32: {
                    /*
                     * TODO qtj Assuming it's the non-alpha version
                     * of the next format (i.e. that it was called RGB24).
                     */
                    helper.addFormat(format.ordinal(), 0, (0xFF << 16), (0xFF << 8), 0xFF);
                } break;
                case Format_ARGB32: {
                    helper.addFormat(format.ordinal(), (0xFF << 24), (0xFF << 16), (0xFF << 8), 0xFF);
                } break;
                case Format_ARGB32_Premultiplied: {
                    helper.addFormat(format.ordinal(), (0xFF << 24), (0xFF << 16), (0xFF << 8), 0xFF);
                } break;
                case Format_RGB16: {
                    /*
                     * TODO qtj Assuming it's the non-alpha version
                     * of the next format (i.e. that it was called RGB565).
                     */
                    helper.addFormat(format.ordinal(), 0, (0x1F << 11), (0x3F << 5), 0x1F);
                } break;
                case Format_ARGB8565_Premultiplied: {
                    helper.addFormat(format.ordinal(), (0xFF << 16), (0x1F << 11), (0x3F << 5), 0x1F);
                } break;
                case Format_RGB666: {
                    helper.addFormat(format.ordinal(), 0, (0x3F << 12), (0x3F << 6), 0x3F);
                } break;
                case Format_ARGB6666_Premultiplied: {
                    helper.addFormat(format.ordinal(), (0x3F << 18), (0x3F << 12), (0x3F << 6), 0x3F);
                } break;
                case Format_RGB555: {
                    helper.addFormat(format.ordinal(), 0, (0x1F << 10), (0x1F << 5), 0x1F);
                } break;
                case Format_ARGB8555_Premultiplied: {
                    helper.addFormat(format.ordinal(), (0xFF << 15), (0x1F << 10), (0x1F << 5), 0x1F);
                } break;
                case Format_RGB888: {
                    helper.addFormat(format.ordinal(), 0, (0xFF << 16), (0xFF << 8), 0xFF);
                } break;
                case Format_RGB444: {
                    helper.addFormat(format.ordinal(), 0, (0xF << 8), (0xF << 4), 0xF);
                } break;
                case Format_ARGB4444_Premultiplied: {
                    helper.addFormat(format.ordinal(), (0xF << 12), (0xF << 8), (0xF << 4), 0xF);
                } break;
                default: {
                    // Not handled.
                } break;
            }
        }
    }
}
