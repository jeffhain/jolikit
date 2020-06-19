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
package net.jolikit.bwd.impl.jfx;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;

public class JfxBwdWritableImage extends AbstractJfxBwdImage implements InterfaceBwdWritableImage {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Dirty snapshots are always done on the entire image,
     * which is of fixed size, so it doesn't matter.
     */
    private static final boolean ALLOW_SNAPSHOT_STORAGE_SHRINKING = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceBwdGraphics graphics;

    private final JfxDirtySnapshotHelper dirtySnapshotHelper;
    
    private final int[] premulArgb32Arr;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param width Image width. Must be > 0.
     * @param height Image height. Must be > 0.
     * @throws NullPointerException if binding or disposalListener is null.
     * @throws IllegalArgumentException if width or height is <= 0.
     */
    public JfxBwdWritableImage(
            AbstractJfxBwdBinding binding,
            int width,
            int height,
            InterfaceBwdImageDisposalListener disposalListener) {
        super(disposalListener);
        
        this.checkAndSetWritableImageDims(width, height);
        
        final boolean isImageGraphics = true;
        final GRect box = this.getRect();
        
        final JfxBwdBindingConfig bindingConfig = binding.getBindingConfig();
        
        final InterfaceBwdGraphics graphics;
        if (bindingConfig.getMustUseIntArrayGraphicsForWritableImages()) {
            final int pixelCapacity = box.area();
            final int[] pixelArr = new int[pixelCapacity];
            final int pixelArrScanlineStride = width;
            
            graphics = new JfxBwdGraphicsWithIntArr(
                    binding,
                    isImageGraphics,
                    box,
                    pixelArr,
                    pixelArrScanlineStride);
            
            this.dirtySnapshotHelper = null;
            this.premulArgb32Arr = pixelArr;
        } else {
            /*
             * As long as not attached to a scene, the canvas doesn't need to
             * be used in JavaFX application thread... except for snapshots!
             */
            final Canvas canvas = JfxUtils.newCanvas(width, height);

            final GraphicsContext gc = canvas.getGraphicsContext2D();
            
            final JfxDirtySnapshotHelper dirtySnapshotHelper =
                    new JfxDirtySnapshotHelper(
                            canvas,
                            ALLOW_SNAPSHOT_STORAGE_SHRINKING);
            graphics = new JfxBwdGraphicsWithGc(
                    binding,
                    gc,
                    isImageGraphics,
                    box,
                    //
                    dirtySnapshotHelper);
            
            this.dirtySnapshotHelper = dirtySnapshotHelper;
            this.premulArgb32Arr = null;
        }
        this.graphics = graphics;
        
        graphics.init();
    }
    
    @Override
    public InterfaceBwdGraphics getGraphics() {
        return this.graphics;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected int getArgb32AtImpl(int x, int y) {
        final int[] premulArgb32Arr = this.premulArgb32Arr;
        
        final int argb32;
        if (premulArgb32Arr != null) {
            final int index = x + this.getWidth() * y;
            final int premulArgb32 = premulArgb32Arr[index];
            argb32 = BindingColorUtils.toNonPremulAxyz32(premulArgb32);
        } else {
            /*
             * Case of a writable image using a GC graphics.
             */
            final int defaultRes = 0;
            final int xInBase = x;
            final int yInBase = y;

            final InterfaceBwdWritableImage thiz =
                    (InterfaceBwdWritableImage) this;

            final JfxBwdGraphicsWithGc g = (JfxBwdGraphicsWithGc) thiz.getGraphics();
            final JfxDirtySnapshotHelper dirtySnapshotHelper = g.getDirtySnapshotHelper();

            dirtySnapshotHelper.beforePixelReading(xInBase, yInBase);
            final GRect snapshotBox = dirtySnapshotHelper.getSnapshotBox();
            if (!snapshotBox.contains(xInBase, yInBase)) {
                return defaultRes;
            }

            final int[] snapshotPremulArgb32Arr = dirtySnapshotHelper.getSnapshotPremulArgb32Arr();
            final int snapshotScanlineStride = dirtySnapshotHelper.getSnapshotScanlineStride();

            final int index = yInBase * snapshotScanlineStride + xInBase;
            final int premulArgb32 = snapshotPremulArgb32Arr[index];
            argb32 = BindingColorUtils.toNonPremulAxyz32(premulArgb32);
        }
        return argb32;
    }

    @Override
    protected void disposeImpl() {
        this.graphics.finish();
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    @Override
    int[] getPremulArgb32ArrElseNull() {
        return this.premulArgb32Arr;
    }

    @Override
    Image getBackingImageElseNull() {
        return null;
    }
    
    @Override
    Image getBackingImageForGcDrawOrRead() {
        final int[] premulArgb32Arr = this.premulArgb32Arr;
        final Image ret;
        if (premulArgb32Arr != null) {
            final JfxBwdGraphicsWithIntArr graphicsImpl =
                    (JfxBwdGraphicsWithIntArr) this.graphics;
            // Here, initial clip corresponds to the whole image.
            ret = graphicsImpl.getSnapshotOverInitialClip();
        } else {
            this.dirtySnapshotHelper.beforeWholeReading();
            /*
             * Never null: snapshot done due to above call,
             * with snapshot box not empty due to image dimensions
             * being > 0.
             */
            ret = this.dirtySnapshotHelper.getSnapshotImage();
            if (ret == null) {
                throw new AssertionError("snapshots of writable images must not be null");
            }
        }
        return ret;
    }
}
