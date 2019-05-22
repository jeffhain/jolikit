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
package net.jolikit.bwd.impl.jfx;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.time.TimeUtils;

/**
 * Helper class to take snapshots of canvas when needed for pixel reading.
 * Must use one instance per host.
 * Doesn't hurt to share this instance among multiple graphics,
 * since painting is single-threaded for this binding
 * (could multi-thread by using one non-rooted canvas per graphics,
 * but this should mostly just add much overhead).
 */
public class JfxSnapshotHelper {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final BaseBwdBindingConfig bindingConfig;
    
    private final Canvas canvas;
    
    /**
     * Using this to create and grow the snapshot array.
     */
    private final IntArrayGraphicBuffer snapshotGraphicBuffer = new IntArrayGraphicBuffer(false);
    
    private GRect snapshotBox = GRect.DEFAULT_EMPTY;

    /**
     * Bounding box for pixels that were drawn and require a new snapshot
     * before reading.
     */
    private GRect snapshotDirtyBox = GRect.DEFAULT_HUGE;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JfxSnapshotHelper(
            BaseBwdBindingConfig bindingConfig,
            Canvas canvas) {
        this.bindingConfig = bindingConfig;
        this.canvas = canvas;
    }
    
    /*
     * Methods for managing snapshot.
     */
    
    /**
     * To be called whenever some point is drawn.
     * 
     * @param transform Transform between client frame (1) and user frame (2).
     */
    public void onPointDrawing(GTransform transform, int x, int y) {
        if (!this.bindingConfig.getMustImplementBestEffortPixelReading()) {
            return;
        }
        
        final int xInClient = transform.xIn1(x, y);
        final int yInClient = transform.yIn1(x, y);
        this.ensureDirtyBoxInSnapshotBox(xInClient, yInClient);
    }

    /**
     * To be called whenever some line is drawn.
     * 
     * @param transform Transform between client frame (1) and user frame (2).
     */
    public void onLineDrawing(GTransform transform, int x1, int y1, int x2, int y2) {
        if (!this.bindingConfig.getMustImplementBestEffortPixelReading()) {
            return;
        }
        
        final int x1InClient = transform.xIn1(x1, y1);
        final int y1InClient = transform.yIn1(x1, y1);
        final int x2InClient = transform.xIn1(x2, y2);
        final int y2InClient = transform.yIn1(x2, y2);
        this.ensureDirtyBoxInSnapshotBox(x1InClient, y1InClient);
        this.ensureDirtyBoxInSnapshotBox(x2InClient, y2InClient);
    }

    /**
     * To be called whenever some rectangle is drawn.
     * 
     * @param transform Transform between client frame (1) and user frame (2).
     */
    public void onRectDrawing(GTransform transform, int x, int y, int xSpan, int ySpan) {
        if (!this.bindingConfig.getMustImplementBestEffortPixelReading()) {
            return;
        }
        
        if ((xSpan <= 0) || (ySpan <= 0)) {
            return;
        }
        
        final int xInClient = transform.xIn1(x, y);
        final int yInClient = transform.yIn1(x, y);
        
        final GRotation rotation = transform.rotation();
        final int xSpanInClient = rotation.xSpanInOther(xSpan, ySpan);
        final int ySpanInClient = rotation.ySpanInOther(xSpan, ySpan);
        
        this.ensureDirtyBoxInSnapshotBox(
                xInClient,
                yInClient,
                xSpanInClient,
                ySpanInClient);
    }
    
    /**
     * To be called whenever some text is drawn.
     * 
     * @param transform Transform between client frame (1) and user frame (2).
     */
    public void onTextDrawing(GTransform transform, int x, int y, String text, InterfaceBwdFont font) {
        if (!this.bindingConfig.getMustImplementBestEffortPixelReading()) {
            return;
        }
        
        /*
         * TODO jfx This supposes that text will actually
         * not leak outside its theoretical box.
         */
        
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        final int xSpan = metrics.computeTextWidth(text);
        final int ySpan = metrics.fontHeight();
        
        this.onRectDrawing(transform, x, y, xSpan, ySpan);
    }

    /**
     * To be called before reading pixels from snapshot pixels.
     * 
     * If the specified pixel is considered dirty,
     * takes a snapshot of the whole client area,
     * else does nothing.
     */
    public void beforePixelReading(int xInClient, int yInClient) {
        if (!this.bindingConfig.getMustImplementBestEffortPixelReading()) {
            return;
        }
        if (this.snapshotDirtyBox.contains(xInClient, yInClient)) {
            this.snapshotDirtyBox = GRect.DEFAULT_EMPTY;
            this.takeSnapshot();
        }
    }
    
    /*
     * Methods for reading pixels.
     */
    
    public GRect getSnapshotBox() {
        return this.snapshotBox;
    }
    
    public int[] getSnapshotPremulArgb32Arr() {
        return this.snapshotGraphicBuffer.getPixelArr();
    }
    
    public int getSnapshotScanlineStride() {
        return this.snapshotGraphicBuffer.getScanlineStride();
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Only growing the dirty box within snapshot box.
     * This allows to avoid overflows in case of huge positions.
     */
    private void ensureDirtyBoxInSnapshotBox(int xInClient, int yInClient) {
        final GRect box = this.snapshotBox;
        xInClient = NumbersUtils.toRange(box.x(), box.xMax(), xInClient);
        yInClient = NumbersUtils.toRange(box.y(), box.yMax(), yInClient);
        this.snapshotDirtyBox = this.snapshotDirtyBox.unionBoundingBox(xInClient, yInClient);
    }

    private void ensureDirtyBoxInSnapshotBox(
            int xInClient,
            int yInClient,
            int xSpanInClient,
            int ySpanInClient) {
        
        // Dirty box is rectangular, so only need
        // to use two extremities on a diagonal.
        
        this.ensureDirtyBoxInSnapshotBox(xInClient, yInClient);
        
        final int xMax = (int) Math.min(Integer.MAX_VALUE, xInClient + (long) xSpanInClient + 1);
        final int yMax = (int) Math.min(Integer.MAX_VALUE, yInClient + (long) ySpanInClient + 1);
        this.ensureDirtyBoxInSnapshotBox(xMax, yMax);
    }

    /*
     * 
     */
    
    private void takeSnapshot() {
        
        final Canvas canvas = this.canvas;
        
        final int w = (int) canvas.getWidth();
        final int h = (int) canvas.getHeight();
        final GRect box = GRect.valueOf(0, 0, w, h);
        
        // WritableImage throws if empty.
        if ((w == 0) || (h == 0)) {
            this.snapshotBox = box;
            return;
        }
        
        /*
         * Snapshot overhead looks mostly proportional to canvas dimensions,
         * whatever the actual area to be retrieved, so we just snapshot
         * everything at each call.
         */
        
        final long a = (DEBUG ? System.nanoTime() : 0L);
        
        final WritableImage image = canvas.snapshot(null, new WritableImage(w, h));
        
        final long b = (DEBUG ? System.nanoTime() : 0L);
        if (DEBUG) {
            Dbg.log("snapshot took " + TimeUtils.nsToS(b-a) + " s");
        }
        
        final PixelReader pixelReader = image.getPixelReader();

        this.snapshotGraphicBuffer.setSize(w, h);
        this.snapshotBox = box;
        final int[] premulArgb32Arr = this.snapshotGraphicBuffer.getPixelArr();

        final int scanlineStride = this.snapshotGraphicBuffer.getScanlineStride();
        final int offset = 0;
        pixelReader.getPixels(
                0, 0, w, h,
                PixelFormat.getIntArgbPreInstance(),
                premulArgb32Arr,
                offset,
                scanlineStride);
    }
}
