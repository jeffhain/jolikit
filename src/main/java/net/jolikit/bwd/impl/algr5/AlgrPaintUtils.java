/*
 * Copyright 2019-2024 Jeff Hain
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

import java.io.PrintStream;
import java.util.List;

import com.sun.jna.Pointer;

import net.jolikit.bwd.api.graphics.BwdScalingType;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_LOCKED_REGION;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.PixelCoordsConverter;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;
import net.jolikit.bwd.impl.utils.graphics.IntArrSrcPixels;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;
import net.jolikit.bwd.impl.utils.graphics.InterfaceRowDrawer;
import net.jolikit.bwd.impl.utils.graphics.PremulArgbHelper;
import net.jolikit.bwd.impl.utils.graphics.ScaledRectDrawing;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;

public class AlgrPaintUtils {
    
    /*
     * TODO algr Rendering is very slow if using al_put_pixel(...),
     * at least from Java, so we use JNA memory write methods instead.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * TODO algr Bitmap locking failure can happen for example when doing
     * ctrl+alt+suppr on Windows 7, while the selection screen that shows up
     * is alive.
     * (Also, al_get_errno() seems to return 34, whatever that means.)
     * Not to throw all over the place in this case, we abort as if we
     * didn't have to draw something, with a warn which could save days
     * of debugging in case of other failure cases.
     * 
     * Locking failure can also happen if specified area leaks out,
     * with either error code 32 or 34, so now we guard against it
     * (and maybe that solves the case described above as well).
     */
    private static final boolean MUST_JUST_WARN_IF_CANT_LOCK_BITMAP = true;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyRowDrawer implements InterfaceRowDrawer {
        private ALLEGRO_LOCKED_REGION region;
        private GRect regionClip;
        private Pointer dataPtr;
        public MyRowDrawer() {
        }
        /**
         * Clip useful to avoid leak when drawing
         * padding border, which is defined in BD
         * and goes out of client.
         * 
         * @param regionClip In device pixels.
         */
        public void configure(
                ALLEGRO_LOCKED_REGION region,
                GRect regionClip,
                Pointer dataPtr) {
            this.region = region;
            this.regionClip = regionClip;
            this.dataPtr = dataPtr;
        }
        @Override
        public void drawRow(
                int[] rowArr,
                int rowOffset,
                int dstX,
                int dstY,
                int length) {
            if ((dstY < this.regionClip.y())
                || (dstY > this.regionClip.yMax())) {
                return;
            }
            
            final int leftOver = Math.max(0,
                this.regionClip.x() - dstX);
            rowOffset += leftOver;
            dstX += leftOver;
            length -= leftOver;
            
            final int rightOver = Math.max(0,
                (dstX + length)
                - (this.regionClip.x() + this.regionClip.xSpan()));
            length -= rightOver;
            
            final long offset =
                dstY * this.region.pitch
                + dstX * this.region.pixel_size;
            this.dataPtr.write(
                offset,
                rowArr,
                rowOffset,
                length);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;
    
    /*
     * temps
     */
    
    private final IntArrSrcPixels tmpInputPixels = new IntArrSrcPixels();
    
    private final MyRowDrawer tmpRowDrawer = new MyRowDrawer();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AlgrPaintUtils() {
    }
    
    /*
     * 
     */
    
    public static int[] regionToArgb32Arr(
            ALLEGRO_LOCKED_REGION region,
            int width,
            int height) {
        final int format = region.format;
        
        // Quick check on the format.
        if (!AlgrFormatUtils.isBitmapFormat32BitsArgbIsh(format)) {
            throw new BindingError("" + format);
        }
        
        final int pixelCapacity = NbrsUtils.timesExact(width, height);
        final int[] argb32Arr = new int[pixelCapacity];
        
        for (int y = 0; y < height; y++) {
            // Pitch is in bytes.
            final long byteOffsetFrom = region.pitch * y;
            final int pixelOffsetTo = width * y;
            region.data.read(byteOffsetFrom, argb32Arr, pixelOffsetTo, width);
        }

        return argb32Arr;
    }

    /*
     * 
     */

    /**
     * Does not flip display, so that can be called multiple times
     * with different rectangles before costly display flip.
     * 
     * @param paintedRectList Parts of buffer to be copied.
     */
    public void paintPixelsOnClient(
        ScaleHelper scaleHelper,
        GPoint bufferPosInCliInOs,
        List<GRect> paintedRectList,
        //
        IntArrayGraphicBuffer bufferInBd,
        Pointer display,
        PixelCoordsConverter pixelCoordsConverter,
        InterfaceParallelizer parallelizer,
        PrintStream issueStream) {

        final int[] bufferArr = bufferInBd.getPixelArr();
        final int bufferArrScanlineStride = bufferInBd.getScanlineStride();
        final int bufferWidth = bufferInBd.getWidth();
        final int bufferHeight = bufferInBd.getHeight();
        
        final Pointer windowBitmap = LIB.al_get_backbuffer(display);
        LIB.al_set_target_bitmap(windowBitmap);
        
        /*
         * 
         */
        
        final int windowBitmapLockFormat = AlgrFormatUtils.getLockFormat(windowBitmap);
        
        /*
         * Bitmap rectangle, to make sure we stay in.
         */
        
        final int bitmapWidthInDevice = LIB.al_get_bitmap_width(windowBitmap);
        final int bitmapHeightInDevice = LIB.al_get_bitmap_height(windowBitmap);
        final GRect bitmapRectInDevice =
            GRect.valueOf(
                0,
                0,
                bitmapWidthInDevice,
                bitmapHeightInDevice);

        /*
         * TODO algr Only locking small parts if possible,
         * for benches are utterly slow when we have to lock
         * the whole client area and Allegro does format conversion.
         * ===> Maybe could just get away with NOT locking?
         */
        final boolean mustJustLockPaintedRectRegion = true;
        Pointer regionPtr = null;
        ALLEGRO_LOCKED_REGION region = null;
        if (!mustJustLockPaintedRectRegion) {
            regionPtr = LIB.al_lock_bitmap(
                    windowBitmap,
                    windowBitmapLockFormat,
                    AlgrJnaLib.ALLEGRO_LOCK_READWRITE);
            if (regionPtr == null) {
                if (MUST_JUST_WARN_IF_CANT_LOCK_BITMAP) {
                    issueStream.println("WARN: could not lock window bitmap (1): " + LIB.al_get_errno());
                    return;
                } else {
                    throw new BindingError("could not lock window bitmap: " + LIB.al_get_errno());
                }
            }
            region = AlgrJnaUtils.newAndRead(ALLEGRO_LOCKED_REGION.class, regionPtr);
        }
        try {
            final IntArrSrcPixels inputPixels = this.tmpInputPixels;
            final GRect bufferRect =
                GRect.valueOf(0, 0, bufferWidth, bufferHeight);
            inputPixels.configure(
                bufferRect,
                bufferArr,
                bufferArrScanlineStride);

            for (GRect prInBuffInBd : paintedRectList) {
                final GRect prInBuffInOs = scaleHelper.rectBdToOs(prInBuffInBd);
                
                final int prXInCliInOs = prInBuffInOs.x() + bufferPosInCliInOs.x();
                final int prYInCliInOs = prInBuffInOs.y() + bufferPosInCliInOs.y();
                
                // Might leak out of the device.
                final GRect rectInDevice = GRect.valueOf(
                    pixelCoordsConverter.computeXInDevicePixel(prXInCliInOs),
                    pixelCoordsConverter.computeYInDevicePixel(prYInCliInOs),
                    pixelCoordsConverter.computeXSpanInDevicePixel(prInBuffInOs.xSpan()),
                    pixelCoordsConverter.computeYSpanInDevicePixel(prInBuffInOs.ySpan()));
                
                /*
                 * Locking fails if we leak out of bitmap,
                 * so we need to clip.
                 */
                final GRect clipInDevice = bitmapRectInDevice;
                final GRect rectInDeviceClipped =
                    rectInDevice.intersected(clipInDevice);
                
                final GRect rectInRegion;
                final GRect clipInRegion;
                if (mustJustLockPaintedRectRegion) {
                    regionPtr = LIB.al_lock_bitmap_region(
                            windowBitmap,
                            rectInDeviceClipped.x(),
                            rectInDeviceClipped.y(),
                            rectInDeviceClipped.xSpan(),
                            rectInDeviceClipped.ySpan(),
                            windowBitmapLockFormat,
                            AlgrJnaLib.ALLEGRO_LOCK_READWRITE);
                    if (regionPtr == null) {
                        if (MUST_JUST_WARN_IF_CANT_LOCK_BITMAP) {
                            issueStream.println("WARN: could not lock window bitmap (2): " + LIB.al_get_errno());
                            return;
                        } else {
                            throw new BindingError("could not lock window bitmap: " + LIB.al_get_errno());
                        }
                    }
                    rectInRegion = rectInDeviceClipped.toThisRelative(rectInDevice);
                    clipInRegion = rectInDeviceClipped.toThisRelative(clipInDevice);
                } else {
                    rectInRegion = rectInDevice;
                    clipInRegion = clipInDevice;
                }
                try {
                    if (mustJustLockPaintedRectRegion) {
                        region = AlgrJnaUtils.newAndRead(ALLEGRO_LOCKED_REGION.class, regionPtr);
                    }
                    final Pointer dataPtr = region.data;
                    
                    /*
                     * Not using ALLEGRO_TRANSFORM for scaling, because it would require
                     * to use intermediary bitmaps, which would cause more ties with Allegro
                     * framework, more overhead (in memory, and possibly perfs, cf. slow
                     * formats conversions), more try/finally mess, and might not behave
                     * exactly as I want, which is consistently with default images scaling
                     * algorithms.
                     */
                    // NEAREST is best since scale is an integer.
                    final BwdScalingType scalingType = BwdScalingType.NEAREST;
                    final MyRowDrawer dstRowDrawer = this.tmpRowDrawer;
                    dstRowDrawer.configure(
                        region,
                        clipInRegion,
                        dataPtr);
                    final GRect srcRect = prInBuffInBd;
                    final GRect dstRect = rectInRegion;
                    final GRect dstClip = clipInRegion;
                    ScaledRectDrawing.drawScaledRect(
                        parallelizer,
                        scalingType,
                        PremulArgbHelper.getInstance(),
                        //
                        inputPixels,
                        srcRect,
                        //
                        dstRect,
                        dstClip,
                        dstRowDrawer);
                } finally {
                    if (mustJustLockPaintedRectRegion) {
                        LIB.al_unlock_bitmap(windowBitmap);
                    }
                }
            }
        } finally {
            if (!mustJustLockPaintedRectRegion) {
                LIB.al_unlock_bitmap(windowBitmap);
            }
        }
    }
}
