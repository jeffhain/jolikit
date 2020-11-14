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

import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.lang.NumbersUtils;

/**
 * Helper class to take snapshots of canvas
 * only when needed for pixel reading.
 */
public class JfxDirtySnapshotHelper {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final JfxSnapshotHelper snapshotHelper;
    
    /**
     * Bounding box for pixels that were drawn and require a new snapshot
     * before reading.
     * Huge by default, for any actual pixel to be considered dirty.
     */
    private GRect snapshotDirtyBox = GRect.DEFAULT_HUGE;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param allowStorageShrinking For snapshot image and pixel array.
     */
    public JfxDirtySnapshotHelper(
            Canvas canvas,
            boolean allowStorageShrinking) {
        this.snapshotHelper = new JfxSnapshotHelper(
                canvas,
                allowStorageShrinking);
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
        if (this.isDisabled()) {
            return;
        }
        
        final int xInBase = transform.xIn1(x, y);
        final int yInBase = transform.yIn1(x, y);
        this.ensureDirtyBoxInSnapshotBox(xInBase, yInBase);
    }

    /**
     * To be called whenever some line is drawn.
     * 
     * @param transform Transform between client frame (1) and user frame (2).
     */
    public void onLineDrawing(GTransform transform, int x1, int y1, int x2, int y2) {
        if (this.isDisabled()) {
            return;
        }
        
        final int x1InBase = transform.xIn1(x1, y1);
        final int y1InBase = transform.yIn1(x1, y1);
        final int x2InBase = transform.xIn1(x2, y2);
        final int y2InBase = transform.yIn1(x2, y2);
        this.ensureDirtyBoxInSnapshotBox(x1InBase, y1InBase);
        this.ensureDirtyBoxInSnapshotBox(x2InBase, y2InBase);
    }

    /**
     * To be called whenever some rectangle is drawn.
     * 
     * @param transform Transform between client frame (1) and user frame (2).
     */
    public void onRectDrawing(GTransform transform, int x, int y, int xSpan, int ySpan) {
        if (this.isDisabled()) {
            return;
        }
        
        if ((xSpan <= 0) || (ySpan <= 0)) {
            return;
        }
        
        final int xInBase = transform.xIn1(x, y);
        final int yInBase = transform.yIn1(x, y);
        
        final GRotation rotation = transform.rotation();
        final int xSpanInBase = rotation.xSpanInOther(xSpan, ySpan);
        final int ySpanInBase = rotation.ySpanInOther(xSpan, ySpan);
        
        this.ensureDirtyBoxInSnapshotBox(
                xInBase,
                yInBase,
                xSpanInBase,
                ySpanInBase);
    }
    
    /**
     * To be called whenever some text is drawn.
     * 
     * @param transform Transform between client frame (1) and user frame (2).
     */
    public void onTextDrawing(
            GTransform transform,
            int x,
            int y,
            String text,
            InterfaceBwdFont font) {
        if (this.isDisabled()) {
            return;
        }
        
        /*
         * TODO jfx This supposes that text will actually
         * not leak outside its theoretical box.
         */
        
        final InterfaceBwdFontMetrics metrics = font.metrics();
        final int xSpan = metrics.computeTextWidth(text);
        final int ySpan = metrics.height();
        
        this.onRectDrawing(transform, x, y, xSpan, ySpan);
    }

    /**
     * To be called before reading pixels from snapshot pixels.
     * 
     * If the specified pixel is considered dirty,
     * takes a snapshot of the whole client area,
     * else does nothing.
     */
    public void beforePixelReading(int xInBase, int yInBase) {
        if (this.isDisabled()) {
            return;
        }
        if (this.snapshotDirtyBox.contains(xInBase, yInBase)) {
            this.snapshotDirtyBox = GRect.DEFAULT_EMPTY;
            this.takeSnapshot();
        }
    }

    /**
     * To be called before reading all pixels from snapshot pixels.
     * 
     * If any pixel is dirty,
     * takes a snapshot of the whole client area,
     * else does nothing.
     */
    public void beforeWholeReading() {
        if (this.isDisabled()) {
            return;
        }
        if (!this.snapshotDirtyBox.isEmpty()) {
            this.snapshotDirtyBox = GRect.DEFAULT_EMPTY;
            this.takeSnapshot();
        }
    }
    
    /*
     * Methods for reading pixels.
     */
    
    public GRect getSnapshotBox() {
        return this.snapshotHelper.getSnapshotBox();
    }
    
    /**
     * @return The last snapshot image, or null if none.
     */
    public WritableImage getSnapshotImage() {
        return this.snapshotHelper.getSnapshotImage();
    }
    
    public int[] getSnapshotPremulArgb32Arr() {
        return this.snapshotHelper.getArgb32Arr();
    }
    
    public int getSnapshotScanlineStride() {
        return this.snapshotHelper.getScanlineStride();
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    /**
     * This default implementation returns false.
     * Override to be able to disable it dynamically.
     * 
     * @return Whether snapshots must be disabled.
     */
    protected boolean isDisabled() {
        return false;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Only growing the dirty box within snapshot box.
     * This allows to avoid overflows in case of huge positions.
     */
    private void ensureDirtyBoxInSnapshotBox(int xInBase, int yInBase) {
        final GRect snapshotBox = this.snapshotHelper.getSnapshotBox();
        if (snapshotBox.isEmpty()) {
            /*
             * Dirty box must remain empty.
             */
        } else {
            /*
             * toRange(...) only works for non-zero spans.
             */
            xInBase = NumbersUtils.toRange(snapshotBox.x(), snapshotBox.xMax(), xInBase);
            yInBase = NumbersUtils.toRange(snapshotBox.y(), snapshotBox.yMax(), yInBase);
            this.snapshotDirtyBox = this.snapshotDirtyBox.unionBoundingBox(xInBase, yInBase);
        }
    }

    private void ensureDirtyBoxInSnapshotBox(
            int xInBase,
            int yInBase,
            int xSpanInBase,
            int ySpanInBase) {
        
        // Dirty box is rectangular, so only need
        // to use two extremities on a diagonal.
        
        // Top-left.
        this.ensureDirtyBoxInSnapshotBox(xInBase, yInBase);
        
        // Bottom-right.
        final int xMax = (int) Math.min(Integer.MAX_VALUE, xInBase + (long) xSpanInBase + 1);
        final int yMax = (int) Math.min(Integer.MAX_VALUE, yInBase + (long) ySpanInBase + 1);
        this.ensureDirtyBoxInSnapshotBox(xMax, yMax);
    }

    /*
     * 
     */
    
    /**
     * Takes a snapshot of the whole canvas.
     */
    private void takeSnapshot() {
        
        final Canvas canvas = this.snapshotHelper.getCanvas();
        
        final int width = (int) canvas.getWidth();
        final int height = (int) canvas.getHeight();
        
        /*
         * Snapshot overhead looks mostly proportional to canvas dimensions,
         * whatever the actual area to be retrieved, so we just snapshot
         * everything at each call.
         */
        
        this.snapshotHelper.takeSnapshot(0, 0, width, height);
    }
}
