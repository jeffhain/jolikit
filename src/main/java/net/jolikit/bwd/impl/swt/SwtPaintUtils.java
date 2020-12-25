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
package net.jolikit.bwd.impl.swt;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

public class SwtPaintUtils {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final PaletteData paletteData;

    /*
     * temps
     */

    private byte[] tmpByteArr = new byte[0];

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public SwtPaintUtils() {
        final int redMask = 0x00FF0000;
        final int greenMask = 0x0000FF00;
        final int blueMask = 0x000000FF;
        final PaletteData paletteData = new PaletteData(redMask, greenMask, blueMask);
        this.paletteData = paletteData;
    }

    public static int getArgb32At(ImageData imageData, int x, int y) {
        final PaletteData palette = imageData.palette;

        final int backingPixel = imageData.getPixel(x, y);
        final boolean isPixelTransparent =
                (imageData.transparentPixel != -1)
                && (backingPixel == imageData.transparentPixel);

        final int argb32;
        if (isPixelTransparent) {
            argb32 = 0x00000000;
        } else {
            final int alpha8 = imageData.getAlpha(x, y);
            final int red8;
            final int green8;
            final int blue8;
            if (palette.isDirect) {
                red8 = ((backingPixel & palette.redMask) >>> -palette.redShift);
                green8 = ((backingPixel & palette.greenMask) >>> -palette.greenShift);
                blue8 = ((backingPixel & palette.blueMask) >>> -palette.blueShift);
            } else {
                final RGB[] rgbArr = palette.colors;
                final int colorIndex = backingPixel;
                final RGB rgb = rgbArr[colorIndex];
                red8 = rgb.red;
                green8 = rgb.green;
                blue8 = rgb.blue;
            }
            argb32 = Argb32.toArgb32FromInt8(alpha8, red8, green8, blue8);
        }
        return argb32;
    }

    public void drawRectOnG(
            int[] pixelArr,
            int pixelArrScanlineStride,
            GRect paintedRect,
            Device device,
            GC backingG) {

        /*
         * 
         */

        final int rectWidth = paintedRect.xSpan();
        final int rectHeight = paintedRect.ySpan();

        final int bytesPerPixel = 3;
        final int rectSize = paintedRect.area();
        final int rectByteSize = NbrsUtils.timesExact(bytesPerPixel, rectSize);

        byte[] byteArr = this.tmpByteArr;
        if (byteArr.length < rectByteSize) {
            final int newByteArrLength;
            if (false) {
                /*
                 * NB: Could also just do that, i.e. piggybacking
                 * on client pixel array size smart growing logic.
                 */
                newByteArrLength = NbrsUtils.timesExact(bytesPerPixel, pixelArr.length);
            } else {
                newByteArrLength = LangUtils.increasedArrayLength(byteArr.length, rectByteSize);
            }
            
            byteArr = new byte[newByteArrLength];
            this.tmpByteArr = byteArr;
        }

        /*
         * NB: To avoid this conversion, we could use an AbstractByteArrayBwdGraphics
         * (and make an AbstractArrayBwdGraphics to factor code with
         * AbstractIntArrayBwdGraphics), which might also make blending faster.
         * But that would take more code (byte-based versions of all our int-based
         * low level treatments).
         */

        int red8 = 0;
        int green8 = 0;
        int blue8 = 0;
        int previousPremulArgb32 = 0x00000000;

        int byteIndex = 0;
        for (int y = paintedRect.y(); y <= paintedRect.yMax(); y++) {
            final int lineOffset = y * pixelArrScanlineStride;
            for (int x = paintedRect.x(); x <= paintedRect.xMax(); x++) {
                final int pixelIndex = lineOffset + x;
                final int premulArgb32 = pixelArr[pixelIndex];
                /*
                 * Only computing non-premul {r,g,b} if premul {a,r,g,b} changed.
                 * NB: Could optimize further, by inlining.
                 */
                if (premulArgb32 != previousPremulArgb32) {
                    final int argb32 = BindingColorUtils.toNonPremulAxyz32(premulArgb32);
                    red8 = Argb32.getRed8(argb32);
                    green8 = Argb32.getGreen8(argb32);
                    blue8 = Argb32.getBlue8(argb32);
                    previousPremulArgb32 = premulArgb32;
                }
                byteArr[byteIndex++] = (byte) red8;
                byteArr[byteIndex++] = (byte) green8;
                byteArr[byteIndex++] = (byte) blue8;
            }
        }

        final int bitsPerPixel = bytesPerPixel * 8;
        /*
         * "If one scanline of the image is not a multiple of this number,
         * it will be padded with zeros until it is."
         * 
         * Could use 1, since we don't need padding,
         * but bytesPerPixel makes more sense.
         */
        final int imgDataScanlinePad = bytesPerPixel;

        // Creates an opaque image, i.e. with 0xFF alpha.
        final ImageData imageData = new ImageData(
                rectWidth,
                rectHeight,
                bitsPerPixel,
                this.paletteData,
                imgDataScanlinePad,
                byteArr);

        final Image image = new Image(device, imageData);
        try {
            final GRect rect = paintedRect;
            final int x = rect.x();
            final int y = rect.y();
            backingG.drawImage(
                    image,
                    0, // srcX
                    0, // srcY
                    rectWidth, // srcWidth
                    rectHeight, // srcHeight
                    x, // destX
                    y, // destY
                    rectWidth, // destWidth
                    rectHeight); // destHeight
        } finally {
            image.dispose();
        }
    }
}
