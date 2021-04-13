/*
 * Copyright 2020-2021 Jeff Hain
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

import com.trolltech.qt.gui.QImage.Format;

import net.jolikit.bwd.impl.utils.graphics.PixelFormatConverter;

public class QtjPixelFormatUtils {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
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
    
    /**
     * @param format Format to use.
     * @return Corresponding converter (possibly for ARGB32).
     * @throws IllegalArgumentException if the specified format is not supported.
     */
    public static PixelFormatConverter getConverter(Format format) {
        
        /*
         * TODO qtj Some format are obscure (at least not obvious),
         * so preferring to not handle them.
         * When the binding throws due to encountering them, is the time
         * to figure out what they are and upgrade this treatment.
         */
        
        PixelFormatConverter ret = null;
        
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
                ret = PixelFormatConverter.valueOf(
                    0, (0xFF << 16), (0xFF << 8), 0xFF);
            } break;
            case Format_RGB32: {
                /*
                 * TODO qtj Assuming it's the non-alpha version
                 * of the next format (i.e. that it was called RGB24).
                 */
                ret = PixelFormatConverter.valueOf(
                    0, (0xFF << 16), (0xFF << 8), 0xFF);
            } break;
            case Format_ARGB32: {
                ret = PixelFormatConverter.ARGB32_CONVERTER;
            } break;
            case Format_ARGB32_Premultiplied: {
                ret = PixelFormatConverter.ARGB32_CONVERTER;
            } break;
            case Format_RGB16: {
                /*
                 * TODO qtj Assuming it's the non-alpha version
                 * of the next format (i.e. that it was called RGB565).
                 */
                ret = PixelFormatConverter.valueOf(
                    0, (0x1F << 11), (0x3F << 5), 0x1F);
            } break;
            case Format_ARGB8565_Premultiplied: {
                ret = PixelFormatConverter.valueOf(
                    (0xFF << 16), (0x1F << 11), (0x3F << 5), 0x1F);
            } break;
            case Format_RGB666: {
                ret = PixelFormatConverter.valueOf(
                    0, (0x3F << 12), (0x3F << 6), 0x3F);
            } break;
            case Format_ARGB6666_Premultiplied: {
                ret = PixelFormatConverter.valueOf(
                    (0x3F << 18), (0x3F << 12), (0x3F << 6), 0x3F);
            } break;
            case Format_RGB555: {
                ret = PixelFormatConverter.valueOf(
                    0, (0x1F << 10), (0x1F << 5), 0x1F);
            } break;
            case Format_ARGB8555_Premultiplied: {
                ret = PixelFormatConverter.valueOf(
                    (0xFF << 15), (0x1F << 10), (0x1F << 5), 0x1F);
            } break;
            case Format_RGB888: {
                ret = PixelFormatConverter.valueOf(
                    0, (0xFF << 16), (0xFF << 8), 0xFF);
            } break;
            case Format_RGB444: {
                ret = PixelFormatConverter.valueOf(
                    0, (0xF << 8), (0xF << 4), 0xF);
            } break;
            case Format_ARGB4444_Premultiplied: {
                ret = PixelFormatConverter.valueOf(
                    (0xF << 12), (0xF << 8), (0xF << 4), 0xF);
            } break;
            default: {
                // Not handled.
            } break;
        }
        
        if (ret == null) {
            throw new IllegalArgumentException(
                "format " + format + " is not supported");
        }
        
        return ret;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private QtjPixelFormatUtils() {
    }
}
