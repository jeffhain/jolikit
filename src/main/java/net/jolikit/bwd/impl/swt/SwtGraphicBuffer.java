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
package net.jolikit.bwd.impl.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.graphics.AbstractGraphicBuffer;
import net.jolikit.lang.LangUtils;

/**
 * Resizable graphic buffer of pixels, using an hysteresis for spans of the
 * backing storage object to avoid systematic storage resizing and too much
 * useless memory usage.
 * 
 * This implementation uses a SWT Image as backing storage.
 * 
 * TODO swt Must not have too many instances of this class at once:
 * "This is because each GC requires an underlying platform resource, and
 * on some operating systems these may be scarce, such as Windows 98 that
 * only allows 5 GC objects to be created before it runs out resources."
 */
public class SwtGraphicBuffer extends AbstractGraphicBuffer<Image> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final Display display;
    
    private Image image;
    private int imageWidth;
    private int imageHeight;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param mustCopyOnImageResize If true, when a new backing image is created,
     *        copies pixels of previous image into it.
     */
    public SwtGraphicBuffer(
            Display display,
            boolean mustCopyOnImageResize,
            boolean allowShrinking) {
        super(
                mustCopyOnImageResize,
                allowShrinking);

        this.display = LangUtils.requireNonNull(display);
        
        final int initialStorageSpan = this.getInitialStorageSpan();
        this.createInitialStorage(
                initialStorageSpan,
                initialStorageSpan);
    }
    
    /**
     * Disposes and nullifies the image.
     */
    public void dispose() {
        final Image image = this.image;
        if (image != null) {
            this.disposeStorage(this.image);
            this.image = null;
        }
    }

    /**
     * @return The current backing image (can change on call to
     *         setSize(int,int)), which width and height can be larger than
     *         those of this buffer.
     */
    public Image getImage() {
        return this.image;
    }
    
    /**
     * Can only create one GC per image,
     * so you must dispose the returned GC
     * before trying to create another one.
     * 
     * @return A new graphics for the current backing image, with clip set to
     *         (0,0,width,height).
     */
    public GC createClippedGraphics() {
        /*
         * TODO swt Since have have to choose, we choose LTR
         * (or could we use RTL along with LTR?).
         */
        final int style = SWT.LEFT_TO_RIGHT;
        final GC g = new GC(this.image, style);

        g.setClipping(0, 0, this.getWidth(), this.getHeight());
        return g;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    @Override
    protected Image getStorage() {
        final Image ret = this.image;
        // Null check because can be nullified by dispose().
        if (ret == null) {
            throw new IllegalStateException("image has been disposed");
        }
        return ret;
    }

    @Override
    protected int getStorageWidth() {
        return this.imageWidth;
    }

    @Override
    protected int getStorageHeight() {
        return this.imageHeight;
    }

    @Override
    protected void createStorage(
            int newStorageWidth,
            int newStorageHeight,
            //
            Image oldStorageToCopy,
            int widthToCopy,
            int heightToCopy) {
        final Image image = new Image(
                this.display,
                newStorageWidth,
                newStorageHeight);
        final Rectangle bounds = image.getBounds();
        if ((bounds.x != 0)
                || (bounds.y != 0)
                || (bounds.width != newStorageWidth)
                || (bounds.height != newStorageHeight)) {
            throw new BindingError(
                    "expected (0, 0, "
                            + newStorageWidth + ", " + newStorageHeight
                            + "), got (" + bounds.x + ", " + bounds.y + ", "
                            + " + bounds.width + " + ", " + " + bounds.height + "
                            + ")");
        }
        
        if (oldStorageToCopy != null) {
            final GC g = this.createClippedGraphics();
            try {
                g.drawImage(
                        oldStorageToCopy,
                        0,
                        0,
                        widthToCopy,
                        heightToCopy,
                        0,
                        0,
                        widthToCopy,
                        heightToCopy);
            } finally {
                g.dispose();
            }
        }
        
        this.image = image;
        this.imageWidth = newStorageWidth;
        this.imageHeight = newStorageHeight;
    }

    @Override
    protected void disposeStorage(Image storage) {
        final Image image = storage;
        image.dispose();
    }
}
