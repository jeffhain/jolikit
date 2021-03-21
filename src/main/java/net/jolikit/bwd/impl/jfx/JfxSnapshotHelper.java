/*
 * Copyright 2019-2021 Jeff Hain
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
    
    /**
     * Empty by default because we didn't take any snapshot yet.
     */
    private GRect snapshotBox = GRect.DEFAULT_EMPTY;
    
    private WritableImage snapshotImage = null;
    
    /**
     * Using this to create and grow the snapshot array.
     */
    private final IntArrayGraphicBuffer snapshotPixels;
    
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
     * Static utility, doesn't modify any helper instance state.
     * 
     * @return Snapshot of the whole canvas.
     */
    public static WritableImage takeSnapshot(Canvas canvas) {
        final int cw = (int) canvas.getWidth();
        final int ch = (int) canvas.getHeight();
        return takeSnapshot(canvas, 0, 0, cw, ch);
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
        
        final WritableImage image = takeSnapshot(this.canvas, x, y, width, height);
        this.snapshotImage = image;
        
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
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * The specified rectangle must in included in canvas,
     * and not empty (else WritableImage throws).
     * 
     * @return Snapshot of the specified rectangle.
     * @param tmpParams (in,out) Temporary instance to use as parameters.
     */
    private static WritableImage takeSnapshot(
        Canvas canvas,
        int x, int y, int width, int height) {
        
        final SnapshotParameters params = new SnapshotParameters();
        final Rectangle2D viewPort = new Rectangle2D(
            x, y, width, height);
        params.setViewport(viewPort);
        
        /*
         * New image for each snapshot, because GraphicsContext.drawImage()
         * is asynchronous, which doesn't allow to reuse images.
         * 
         * NB: If can ever reuse images, need to ensure dimensions
         * before snapshot, else it just clamps on it.
         */
        final long a = (DEBUG ? System.nanoTime() : 0L);
        final WritableImage ret = canvas.snapshot(params, null);
        final long b = (DEBUG ? System.nanoTime() : 0L);
        if (DEBUG) {
            Dbg.log("snapshot took " + TimeUtils.nsToS(b-a) + " s");
        }
        return ret;
    }
}
