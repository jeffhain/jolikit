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
package net.jolikit.bwd.impl.sdl2;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Palette;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_PixelFormat;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Rect;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Surface;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;

public class SdlUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static GRect toGRect(SDL_Rect rect) {
        return GRect.valueOf(rect.x, rect.y, rect.w, rect.h);
    }

    /*
     * 
     */

    public static long pixelOffset(SDL_Surface surface, int x, int y) {
        return y * (long) surface.pitch + surface.format.BytesPerPixel * x;
    }

    /**
     * For surfaces that have a palette.
     */
    public static int paletteColorToArgb32(int paletteColor32) {
        return BindingColorUtils.toArgb32FromNativeRgba32(paletteColor32);
    }

    /**
     * For surfaces that don't have a palette.
     */
    public static int formattedPixelToArgb32(SDL_PixelFormat format, int surfPixel) {
        return BindingColorUtils.toArgb32FromPixelWithMasksAndShifts(
                surfPixel,
                format.Amask, format.Rmask, format.Gmask, format.Bmask,
                format.Ashift, format.Rshift, format.Gshift, format.Bshift);
    }

    /**
     * @param premul True if wanting output to be alpha-premultiplied.
     * @return An array of ARGB values.
     * @throws BindingError if the surface is inconsistent.
     */
    public static int[] surfToArgb32Arr(SDL_Surface surface, boolean premul) {
        final SDL_PixelFormat format = surface.format;

        final SDL_Palette palette = format.palette;
        
        final int[] argb32Arr;
        if (palette != null) {
            argb32Arr = surfToArgb32Arr_nonPremul_palette(surface);
        } else {
            argb32Arr = surfToArgb32Arr_nonPremul_noPalette(surface);
        }

        if (premul) {
            for (int i = 0; i < argb32Arr.length; i++) {
                argb32Arr[i] = BindingColorUtils.toPremulAxyz32(argb32Arr[i]);
            }
        }

        return argb32Arr;
    }

    /*
     * int[] to SDL_Surface.
     */

    /**
     * Copies all of src into dst, assuming they have the same origin,
     * and src length is at least srcScanlineStride * dst.h.
     * 
     * Takes care of clipping for dst.
     */
    public static void copyPixels(
            int[] src,
            int srcScanlineStride,
            GRect srcClip,
            //
            SDL_Surface dst) {
        final int xSpan = Math.min(srcClip.xSpan(), dst.w - srcClip.x());
        final int ySpan = Math.min(srcClip.ySpan(), dst.h - srcClip.y());
        copyPixels(
                src, srcScanlineStride,
                srcClip.x(), srcClip.y(),
                dst,
                srcClip.x(), srcClip.y(),
                xSpan, ySpan);
    }

    /**
     * Takes care of clipping for dst,
     * but not for src, so if you don't take care
     * you might get some IOOBE.
     * 
     * @param srcScanlineStride width of a line in src.
     * @param xSpan Can be negative.
     * @param ySpan Can be negative.
     */
    public static void copyPixels(
            int[] src,
            int srcScanlineStride,
            int sx,
            int sy,
            //
            SDL_Surface dst,
            int dx,
            int dy,
            //
            int xSpan,
            int ySpan) {
        /*
         * Computing clipped destination rectangle,
         * and eventually reducing span accordingly.
         */
        final int dxClipped;
        final int dyClipped;
        if (false) {
            /*
             * Garbage code.
             * Also throws in case of negative span.
             */
            final GRect dRect = GRect.valueOf(dx, dy, xSpan, ySpan);
            final GRect dClip = toGRect(dst.clip_rect);
            final GRect dRectClipped = dRect.intersected(dClip);
            dxClipped = dRectClipped.x();
            dyClipped = dRectClipped.y();
            xSpan = dRectClipped.xSpan();
            ySpan = dRectClipped.ySpan();
        } else {
            /*
             * GC-free flavor.
             * Also allows for negative spans.
             */
            dxClipped = GRect.intersectedPos(dx, dst.clip_rect.x);
            dyClipped = GRect.intersectedPos(dy, dst.clip_rect.y);
            xSpan = GRect.intersectedSpan(dx, xSpan, dst.clip_rect.x, dst.clip_rect.w);
            ySpan = GRect.intersectedSpan(dy, ySpan, dst.clip_rect.y, dst.clip_rect.h);
        }
        /*
         * Looping on clipped destination lines.
         */
        for (int j = 0; j < ySpan; j++) {
            final int dstX = dxClipped;
            final int dstY = dyClipped + j;
            final int srcX = sx + (dstX - dx);
            final int srcY = sy + (dstY - dy);
            final long dstOffset = pixelOffset(dst, dstX, dstY);
            final int srcIndex = srcX + srcScanlineStride * srcY;
            dst.pixels.write(dstOffset, src, srcIndex, xSpan);
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private SdlUtils() {
    }

    /*
     * 
     */

    /**
     * @param surface Must not be alpha-premultiplied,
     *        and must have a palette.
     */
    private static int[] surfToArgb32Arr_nonPremul_palette(SDL_Surface surface) {
        final SDL_PixelFormat format = surface.format;
        if (format.palette == null) {
            throw new IllegalArgumentException("must have a palette");
        }

        final int w = surface.w;
        final int h = surface.h;
        final int pixelCount = w * h;
        final int bytesPerPixel = format.BytesPerPixel;

        final SDL_Palette palette = format.palette;

        if (bytesPerPixel != 1) {
            // Surface too messy to go further.
            // Can happen at least in case of concurrent image load.
            throw new BindingError(
                    "bad BytesPerPixel: surface = " + surface
                    + ", format = " + format
                    + ", palette = " + palette);
        }

        /*
         * colors is an array of SDL_Color, which is {r8,g8,b8,a8},
         * so we can just read that as RGBA 32 pixels,
         * modulo endianness rework.
         */
        final long offsetFrom = 0;
        final int offsetTo = 0;
        final int colorCount = palette.ncolors;
        final int[] colorArr = new int[colorCount];
        palette.colors.read(offsetFrom, colorArr, offsetTo, colorCount);
        // Conversion from RGBA to ARGB.
        for (int i = 0; i < colorArr.length; i++) {
            colorArr[i] = paletteColorToArgb32(colorArr[i]);
        }

        final int rowByteLength = w;
        final byte[] tmpRowByteArr = new byte[w];

        final int[] argb32Arr = new int[pixelCount];
        for (int y = 0; y < h; y++) {
            final long lineOffsetFrom = pixelOffset(surface, 0, y);

            surface.pixels.read(lineOffsetFrom, tmpRowByteArr, 0, rowByteLength);

            for (int x = 0; x < rowByteLength; x++) {
                final int indexFrom = x;
                final int colorIndex = (tmpRowByteArr[indexFrom] & 0xFF);
                final int argb32 = colorArr[colorIndex];

                final int indexTo = x + w * y;
                argb32Arr[indexTo] = argb32;
            }
        }

        return argb32Arr;
    }

    /**
     * @param surface Must not be alpha-premultiplied,
     *        and must not have a palette.
     */
    private static int[] surfToArgb32Arr_nonPremul_noPalette(SDL_Surface surface) {
        final SDL_PixelFormat format = surface.format;
        if (format.palette != null) {
            throw new IllegalArgumentException("must not have a palette");
        }

        final int w = surface.w;
        final int h = surface.h;
        final int pixelCount = w * h;
        final int bytesPerPixel = format.BytesPerPixel;

        if ((format.Rmask == 0)
                && (format.Gmask == 0)
                && (format.Bmask == 0)) {
            // Surface too messy to go further.
            throw new BindingError(
                    "bad masks: surface = " + surface
                    + ", format = " + format);
        }

        final int[] pixelArr = new int[pixelCount];

        /*
         * First, reading raw pixels, as they are stored,
         * without taking format into account.
         */

        if (bytesPerPixel == 4) {
            final long offsetFrom = 0;
            final int offsetTo = 0;
            surface.pixels.read(offsetFrom, pixelArr, offsetTo, pixelCount);
        } else {
            final int rowByteLength = w * bytesPerPixel;
            final byte[] tmpRowByteArr = new byte[rowByteLength];

            for (int y = 0; y < h; y++) {
                final long lineOffsetFrom = pixelOffset(surface, 0, y);

                surface.pixels.read(lineOffsetFrom, tmpRowByteArr, 0, rowByteLength);
                if (bytesPerPixel == 3) {
                    if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
                        for (int x = 0; x < w; x++) {
                            final int indexFrom = 3 * x;
                            final int b3 = (tmpRowByteArr[indexFrom] & 0xFF);
                            final int b2 = (tmpRowByteArr[indexFrom+1] & 0xFF);
                            final int b1 = (tmpRowByteArr[indexFrom+2] & 0xFF);
                            final int pixel = (b1 << 16) | (b2 << 8) | b3;

                            final int indexTo = x + w * y;
                            pixelArr[indexTo] = pixel;
                        }
                    } else {
                        for (int x = 0; x < rowByteLength; x++) {
                            final int indexFrom = 3 * x;
                            final int b1 = (tmpRowByteArr[indexFrom] & 0xFF);
                            final int b2 = (tmpRowByteArr[indexFrom+1] & 0xFF);
                            final int b3 = (tmpRowByteArr[indexFrom+2] & 0xFF);
                            final int pixel = (b1 << 16) | (b2 << 8) | b3;

                            final int indexTo = x + w * y;
                            pixelArr[indexTo] = pixel;
                        }
                    }
                } else if (bytesPerPixel == 2) {
                    if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
                        for (int x = 0; x < rowByteLength; x++) {
                            final int indexFrom = 2 * x;
                            final int b2 = (tmpRowByteArr[indexFrom] & 0xFF);
                            final int b1 = (tmpRowByteArr[indexFrom+1] & 0xFF);
                            final int pixel = (b1 << 8) | b2;

                            final int indexTo = x + w * y;
                            pixelArr[indexTo] = pixel;
                        }
                    } else {
                        for (int x = 0; x < rowByteLength; x++) {
                            final int indexFrom = 2 * x;
                            final int b1 = (tmpRowByteArr[indexFrom] & 0xFF);
                            final int b2 = (tmpRowByteArr[indexFrom+1] & 0xFF);
                            final int pixel = (b1 << 8) | b2;

                            final int indexTo = x + w * y;
                            pixelArr[indexTo] = pixel;
                        }
                    }
                } else if (bytesPerPixel == 1) {
                    // NB: Theoretically this case only occurs with palettes,
                    // but doesn't hurt to handle it if ever comes up.
                    for (int x = 0; x < rowByteLength; x++) {
                        final int indexFrom = x;
                        final int b1 = (tmpRowByteArr[indexFrom] & 0xFF);
                        final int pixel = b1;

                        final int indexTo = x + w * y;
                        pixelArr[indexTo] = pixel;
                    }
                } else {
                    throw new BindingError("" + bytesPerPixel);
                }
            }
        }

        /*
         * Second, taking into account pixel format.
         */

        // Using the same array object.
        final int[] argb32Arr = pixelArr;

        final boolean isAlreadyArgb32 =
                (format.Amask == 0xFF000000)
                && (format.Rmask == 0x00FF0000)
                && (format.Gmask == 0x0000FF00)
                && (format.Bmask == 0x000000FF);
        if (!isAlreadyArgb32) {
            for (int i = 0; i < pixelArr.length; i++) {
                argb32Arr[i] = formattedPixelToArgb32(format, pixelArr[i]);
            }
        }

        return argb32Arr;
    }
}
