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
package net.jolikit.bwd.impl.jfx;

import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.TimeUtils;

/**
 * Helper class to take snapshots of canvas and read corresponding pixels.
 */
public class JfxSnapshotHelper {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * No need since we only resize before taking a fresh snapshot.
     */
    private static final boolean MUST_COPY_ON_STORAGE_RESIZE = false;
    
    private final Canvas canvas;
    
    private final boolean allowStorageShrinking;
    
    /**
     * Empty by default because we didn't take any snapshot yet.
     */
    private GRect snapshotBox = GRect.DEFAULT_EMPTY;
    
    private WritableImage snapshotImage = null;
    
    /**
     * Using this to create and grow the snapshot array.
     */
    private final IntArrayGraphicBuffer snapshotPixels;
    
    /*
     * temps
     */
    
    private final SnapshotParameters tmpParams = new SnapshotParameters();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param allowStorageShrinking For snapshot image and pixel array.
     */
    public JfxSnapshotHelper(
            Canvas canvas,
            boolean allowStorageShrinking) {
        this.canvas = LangUtils.requireNonNull(canvas);
        this.allowStorageShrinking = allowStorageShrinking;
        this.snapshotPixels =
                new IntArrayGraphicBuffer(
                        MUST_COPY_ON_STORAGE_RESIZE,
                        allowStorageShrinking);
    }
    
    public Canvas getCanvas() {
        return this.canvas;
    }
    
    /**
     * The returned image is reused by this helper
     * for subsequent snapshots, so it should not be used
     * as argument to GraphicContext.drawImage(),
     * which is asynchronous.
     * 
     * @return The last snapshot image, or null if none.
     */
    public WritableImage getSnapshotImage() {
        return this.snapshotImage;
    }
    
    /**
     * @return The box corresponding to last snapshot,
     *         both in canvas and in snapshot pixel array.
     */
    public GRect getSnapshotBox() {
        return this.snapshotBox;
    }
    
    /**
     * @return The array containing the last snapshot,
     *         as int ARGB pixels.
     */
    public int[] getArgb32Arr() {
        return this.snapshotPixels.getPixelArr();
    }
    
    public int getScanlineStride() {
        return this.snapshotPixels.getScanlineStride();
    }
    
    /**
     * The specified rectangle must be included in the canvas,
     * and it can be empty, in which case no snapshot is taken.
     * 
     * @param canvas (in) Canvas to take snapshot from.
     * @param x Source X in canvas.
     * @param y Source Y in canvas.
     * @param width Width. Must be >= 0.
     * @param height Height. Must be >= 0.
     * @param graphicBuffer (in,out) Buffer where to draw the read pixels,
     *        at (0,0) position, and as (non-premul) argb32.
     */
    public void takeSnapshot(int x, int y, int width, int height) {
        
        /*
         * Bounds checks.
         */
        
        {
            final int cw = (int) this.canvas.getWidth();
            final int ch = (int) this.canvas.getHeight();
            LangUtils.checkBounds(cw, x, width);
            LangUtils.checkBounds(ch, y, height);
        }
        
        /*
         * 
         */
        
        final GRect newSnapshotBox = GRect.valueOf(x, y, width, height);
        this.snapshotBox = newSnapshotBox;

        if (newSnapshotBox.isEmpty()) {
            /*
             * Can't take snapshots of empty rectangles
             * (WritableImage throws if empty).
             */
            return;
        }

        /*
         * Taking snapshot.
         */
        
        final long a = (DEBUG ? System.nanoTime() : 0L);
        
        final SnapshotParameters params = this.tmpParams;
        // Immutable, not bothering to check if unchanged.
        final Rectangle2D viewPort = new Rectangle2D(x, y, width, height);
        params.setViewport(viewPort);
        
        // Need that, else snapshot just clamps on image.
        this.updateSnapshotImageSize(width, height);
        
        /*
         * New image for each snapshot, because GraphicsContext.drawImage()
         * is asynchronous, which doesn't allow to reuse images.
         */
        final WritableImage image = this.canvas.snapshot(params, null);
        this.snapshotImage = image;
        
        final long b = (DEBUG ? System.nanoTime() : 0L);
        if (DEBUG) {
            Dbg.log("snapshot took " + TimeUtils.nsToS(b-a) + " s");
        }
        
        /*
         * Writing graphic buffer.
         */
        
        final PixelReader pixelReader = image.getPixelReader();

        this.snapshotPixels.setSize(width, height);
        final int[] argb32Arr = this.snapshotPixels.getPixelArr();

        final int offset = 0;
        final int scanlineStride = this.snapshotPixels.getScanlineStride();
        
        pixelReader.getPixels(
                0, 0, width, height,
                PixelFormat.getIntArgbPreInstance(),
                argb32Arr,
                offset,
                scanlineStride);
    }
    
    private void updateSnapshotImageSize(int minWidthCap, int minHeightCap) {
        if (minWidthCap < 0) {
            throw new IllegalArgumentException("minWidthCap [" + minWidthCap + "] must be >= 0");
        }
        if (minHeightCap < 0) {
            throw new IllegalArgumentException("minHeightCap [" + minHeightCap + "] must be >= 0");
        }

        final WritableImage oldImage = this.snapshotImage;
        if (oldImage == null) {
            final WritableImage newImage =
                    new WritableImage(
                            minWidthCap,
                            minHeightCap);
            this.snapshotImage = newImage;
        } else {
            final int oldWidthCap = (int) oldImage.getWidth();
            final int oldHeightCap = (int) oldImage.getHeight();

            final int newWidthCap = BindingBasicsUtils.computeStorageSpan(
                    oldWidthCap,
                    minWidthCap,
                    this.allowStorageShrinking);
            final int newHeightCap = BindingBasicsUtils.computeStorageSpan(
                    oldHeightCap,
                    minHeightCap,
                    this.allowStorageShrinking);

            final boolean needNewStorage =
                    (newWidthCap != oldWidthCap)
                    || (newHeightCap != oldHeightCap);

            if (needNewStorage) {
                final WritableImage newImage =
                        new WritableImage(
                                minWidthCap,
                                minHeightCap);
                this.snapshotImage = newImage;
            }
        }
    }
}
