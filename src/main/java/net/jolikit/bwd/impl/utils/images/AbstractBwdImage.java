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
package net.jolikit.bwd.impl.utils.images;

import java.util.concurrent.atomic.AtomicBoolean;

import net.jolikit.bwd.api.graphics.Argb3264;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

public abstract class AbstractBwdImage implements InterfaceBwdImage {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceBwdImageDisposalListener disposalListener;
    
    private GRect imgRect = GRect.DEFAULT_EMPTY;
    
    private final AtomicBoolean disposedRef = new AtomicBoolean();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param disposalListener Must not be null, so that bindings can keep track
     *        of which images still need disposal on shutdown, so that
     *        after shutdown all images consistently have an empty rect
     *        (cf. dispose() code).
     * @throws NullPointerException if disposalListener is null.
     */
    public AbstractBwdImage(InterfaceBwdImageDisposalListener disposalListener) {
        this.disposalListener = LangUtils.requireNonNull(disposalListener);
    }
    
    /*
     * 
     */
    
    @Override
    public int getWidth() {
        return this.imgRect.xSpan();
    }

    @Override
    public int getHeight() {
        return this.imgRect.ySpan();
    }
    
    @Override
    public GRect getRect() {
        return this.imgRect;
    }
    
    /*
     * 
     */
    
    @Override
    public long getArgb64At(int x, int y) {
        final int argb32 = this.getArgb32At(x, y);
        final long argb64 = Argb3264.toArgb64(argb32);
        return argb64;
    }

    @Override
    public int getArgb32At(int x, int y) {
        final GRect rect = this.imgRect;
        if (!rect.contains(x, y)) {
            throw new IllegalArgumentException(
                    "position (" + x + "," + y
                    + ") must be in " + rect);
        }
        return this.getArgb32AtImpl(x, y);
    }

    /*
     * 
     */
    
    @Override
    public boolean isDisposed() {
        return this.disposedRef.get();
    }
    
    @Override
    public void dispose() {
        if (this.disposedRef.compareAndSet(false, true)) {
            try {
                this.disposeImpl();
            } finally {
                /*
                 * getXxx() methods behavior is undefined if the image has been
                 * disposed, and since we like fail-fast, to make usage errors
                 * easily spottable, we make them throw if disposal is detected.
                 * We prefer that than the forgiving "the show must go on"
                 * philosophy of JavaScript and JavaFX.
                 * To do that, we just zeroize the bounds, which allows for
                 * getArgb32At(...) method to throw for any coordinates,
                 * without any additional check needed.
                 * GRect is immutable with finals so an eventual concurrent
                 * modification of this field is fine even if not volatile.
                 * It's also fine for us if this new value is not seen by some
                 * thread, as it would just be a rare case of not detecting
                 * a bad usage.
                 */
                this.imgRect = GRect.DEFAULT_EMPTY;
                
                // Never null (since we mandate it, to be able
                // to ensure all images rect emptying on shutdown).
                this.disposalListener.onImageDisposed(this);
            }
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    /**
     * @param width Writable image width.
     * @param height Writable image height.
     * @throws IllegalArgumentException if width or height is <= 0.
     */
    protected final void checkAndSetWritableImageDims(int width, int height) {
        checkWritableImageDims(width, height);
        this.setWidth_final(width);
        this.setHeight_final(height);
    }

    /**
     * To be called in constructor.
     */
    protected final void setWidth_final(int width) {
        final GRect rect = this.imgRect;
        this.imgRect = rect.withSpans(width, rect.ySpan());
    }
    
    /**
     * To be called in constructor.
     */
    protected final void setHeight_final(int height) {
        final GRect rect = this.imgRect;
        this.imgRect = rect.withSpans(rect.xSpan(), height);
    }
    
    /**
     * Coordinates check already done when this method gets called.
     */
    protected abstract int getArgb32AtImpl(int x, int y);

    /**
     * Called only once.
     */
    protected abstract void disposeImpl();

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param width Writable image width.
     * @param height Writable image height.
     * @throws IllegalArgumentException if width or height is <= 0.
     */
    private static void checkWritableImageDims(int width, int height) {
        NbrsUtils.requireSup(0, width, "width");
        NbrsUtils.requireSup(0, height, "height");
    }
}
