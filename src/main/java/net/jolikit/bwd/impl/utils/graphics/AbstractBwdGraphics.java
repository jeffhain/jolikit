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
package net.jolikit.bwd.impl.utils.graphics;

import java.util.ArrayList;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.Argb3264;
import net.jolikit.bwd.api.graphics.Argb64;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.gprim.GprimUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.SequentialParallelizer;

/**
 * Optional abstract class, that makes it easier to implement graphics.
 */
public abstract class AbstractBwdGraphics implements InterfaceBwdGraphics {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final InterfaceParallelizer SEQUENTIAL_PARALLELIZER = new SequentialParallelizer();
    
    /*
     * 
     */
    
    private final InterfaceBwdBinding binding;

    private final GRect boxInClient;
    
    /*
     * 
     */
    
    private final GRect baseClipInClient;
    
    /*
     * 
     */
    
    /**
     * Base clip in user coordinates.
     */
    private GRect baseClipInUser;
    
    /**
     * Clip in client coordinates.
     */
    private GRect clipInClient;
    
    /**
     * Clip in user coordinates.
     */
    private GRect clipInUser;
    
    /**
     * Lazily created.
     */
    private ArrayList<GRect> clipsBeforeLastAdd = null;
    
    /*
     * 
     */
    
    /**
     * Transform between client coordinates (frame 1)
     * and user coordinates (frame 2).
     * 
     * Never using the transform of the backing graphics,
     * for simplicity and consistency across bindings.
     */
    private GTransform transform = GTransform.IDENTITY;
    
    /**
     * Lazily created.
     */
    private ArrayList<GTransform> transformsBeforeLastAdd = null;
    
    /*
     * 
     */
    
    /**
     * The reference color.
     */
    private long argb64;
    
    /**
     * Lazily initialized.
     */
    private BwdColor colorLazy;
    
    private InterfaceBwdFont font;
    
    /**
     * For simple usage check.
     */
    private boolean initCalled = false;
    
    /**
     * For idempotence and usage check.
     */
    private boolean finishCalled = false;
    
    /**
     * Synthesis of initCalled and finishCalled
     * for quick usability check.
     */
    private boolean notUsable = true;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractBwdGraphics(
            InterfaceBwdBinding binding,
            GRect boxInClient,
            GRect clipInClient) {
        checkGraphicsInitialBoxAndClip(boxInClient, clipInClient);
        
        final BwdColor defaultColor = BwdColor.BLACK;
        // Implicit null check.
        final InterfaceBwdFont defaultFont = binding.getFontHome().getDefaultFont();
        
        this.binding = binding;
        
        this.boxInClient = boxInClient;
        
        this.baseClipInClient = clipInClient;
        
        this.baseClipInUser = clipInClient;
        this.clipInClient = clipInClient;
        this.clipInUser = clipInClient;
        
        this.argb64 = defaultColor.toArgb64();
        this.font = defaultFont;
    }
    
    @Override
    public String toString() {
        return "[boxInClient = "
                + this.boxInClient
                + ", clipInClient = "
                + this.clipInClient
                + ", baseClipInUser = "
                + this.baseClipInUser
                + ", clipInUser = "
                + this.clipInUser
                + ", transform = "
                + this.transform
                + ", color = "
                + Argb64.toString(this.argb64)
                + ", font = "
                + this.font
                + "]";
    }

    /*
     * 
     */
    
    public InterfaceBwdBinding getBinding() {
        return this.binding;
    }
    
    @Override
    public InterfaceParallelizer getPaintingParallelizer() {
        final InterfaceParallelizer paintingParallelizer;
        // Should be fast, saves a field.
        if (this.binding.isParallelPaintingSupported()) {
            paintingParallelizer = this.binding.getParallelizer();
        } else {
            paintingParallelizer = SEQUENTIAL_PARALLELIZER;
        }
        return paintingParallelizer;
    }

    /**
     * This method is final to make sure user will override initImpl() instead,
     * and that single-call ensuring still works.
     */
    @Override
    public final void init() {
        if (this.finishCalled) {
            throw new IllegalStateException("can't init() because finish() has been called");
        }
        if (this.initCalled) {
            return;
        }
        this.initCalled = true;
        this.notUsable = false;
        
        this.initImpl();
    }
    
    /**
     * This method is final to make sure user will override finishImpl() instead,
     * and that single-call ensuring still works.
     */
    @Override
    public final void finish() {
        if (DEBUG) {
            Dbg.log(this.getClass().getSimpleName() + "-" + this.hashCode() + ".finish()");
        }
        if (this.finishCalled) {
            return;
        }
        this.finishCalled = true;
        this.notUsable = true;
        // No calling finishImpl() if initImpl() has not been called,
        // for symmetry (can be required for proper stacks usages).
        if (this.initCalled) {
            this.finishImpl();
        }
    }
    
    /*
     * 
     */

    @Override
    public GRect getBoxInClient() {
        return this.boxInClient;
    }
    
    /*
     * 
     */

    @Override
    public GRect getBaseClipInClient() {
        return this.baseClipInClient;
    }
    
    @Override
    public GRect getBaseClipInUser() {
        return this.baseClipInUser;
    }

    @Override
    public GRect getClipInClient() {
        return this.clipInClient;
    }
    
    @Override
    public GRect getClipInUser() {
        return this.clipInUser;
    }

    @Override
    public void addClipInClient(GRect clip) {
        this.checkUsable();
        
        final GRect oldClip = this.clipInClient;
        // Implicit null check.
        final GRect newClip = oldClip.intersected(clip);
        
        ArrayList<GRect> preList = this.clipsBeforeLastAdd;
        if (preList == null) {
            preList = new ArrayList<GRect>();
            this.clipsBeforeLastAdd = preList;
        }
        preList.add(oldClip);
        
        // This test is optional but cheap.
        if (newClip != oldClip) {
            this.clipInClient = newClip;
            this.updateTransformedClips();
            this.onNewClip();
        }
    }

    @Override
    public void addClipInUser(GRect clip) {
        final GRect clipInClient = this.getTransform().rectIn1(clip);
        this.addClipInClient(clipInClient);
    }
    
    @Override
    public void removeLastAddedClip() {
        this.checkUsable();
        
        final ArrayList<GRect> preList = this.clipsBeforeLastAdd;
        final int size = ((preList != null) ? preList.size() : 0);
        if (size == 0) {
            // Nothing to do.
            return;
        }
        
        final GRect oldClip = this.clipInClient;
        final GRect newClip = preList.remove(size-1);
        
        // This test is optional but cheap.
        if (newClip != oldClip) {
            this.clipInClient = newClip;
            this.updateTransformedClips();
            this.onNewClip();
        }
    }
    
    @Override
    public void removeAllAddedClips() {
        this.checkUsable();
        
        final ArrayList<GRect> preList = this.clipsBeforeLastAdd;
        if (preList != null) {
            preList.clear();
        }
        
        final GRect oldClip = this.clipInClient;
        final GRect newClip = this.baseClipInClient;
        
        // This test is optional but cheap.
        if (newClip != oldClip) {
            this.clipInClient = newClip;
            this.updateTransformedClips();
            this.onNewClip();
        }
    }
    
    /*
     * 
     */
    
    @Override
    public GTransform getTransform() {
        return this.transform;
    }

    @Override
    public void setTransform(GTransform transform) {
        this.checkUsable();
        
        final ArrayList<GTransform> preList = this.transformsBeforeLastAdd;
        if (preList != null) {
            preList.clear();
        }
        
        final GTransform oldTransform = this.transform;
        final GTransform newTransform = LangUtils.requireNonNull(transform);
        
        // This test is optional but cheap.
        if (newTransform != oldTransform) {
            this.transform = newTransform;
            this.updateTransformedClips();
            this.onNewTransform();
        }
    }

    @Override
    public void addTransform(GTransform transform) {
        this.checkUsable();
        
        ArrayList<GTransform> preList = this.transformsBeforeLastAdd;
        if (preList == null) {
            preList = new ArrayList<GTransform>();
            this.transformsBeforeLastAdd = preList;
        }
        
        final GTransform oldTransform = this.transform;
        // Implicit null check.
        final GTransform newTransform = oldTransform.composed(transform);
        preList.add(oldTransform);
        
        // This test is optional but cheap.
        if (newTransform != oldTransform) {
            this.transform = newTransform;
            this.updateTransformedClips();
            this.onNewTransform();
        }
    }

    @Override
    public void removeLastAddedTransform() {
        this.checkUsable();
        
        final ArrayList<GTransform> preList = this.transformsBeforeLastAdd;
        final int size = (preList != null) ? preList.size() : 0;
        if (size == 0) {
            // Nothing to do.
            return;
        }
        
        final GTransform oldTransform = this.transform;
        final GTransform newTransform = preList.remove(size-1);
        
        // This test is optional but cheap.
        if (newTransform != oldTransform) {
            this.transform = newTransform;
            this.updateTransformedClips();
            this.onNewTransform();
        }
    }
    
    @Override
    public void removeAllAddedTransforms() {
        this.checkUsable();
        
        final ArrayList<GTransform> preList = this.transformsBeforeLastAdd;
        final int size = (preList != null) ? preList.size() : 0;
        if (size == 0) {
            // Nothing to do.
            return;
        }
        
        final GTransform oldTransform = this.transform;
        final GTransform newTransform = preList.get(0);
        preList.clear();
        
        // This test is optional but cheap.
        if (newTransform != oldTransform) {
            this.transform = newTransform;
            this.updateTransformedClips();
            this.onNewTransform();
        }
    }

    /*
     * 
     */
    
    @Override
    public BwdColor getColor() {
        BwdColor color = this.colorLazy;
        if (color == null) {
            color = BwdColor.valueOfArgb64(this.argb64);
            this.colorLazy = color;
        }
        return color;
    }

    @Override
    public void setColor(BwdColor color) {
        this.checkUsable();
        
        // Implicit null check.
        // This test is optional but cheap.
        if (!color.equals(this.colorLazy)) {
            this.colorLazy = color;
            this.argb64 = color.toArgb64();
            this.setBackingArgb64(this.argb64);
        }
    }
    
    @Override
    public long getArgb64() {
        return this.argb64;
    }

    @Override
    public void setArgb64(long argb64) {
        this.checkUsable();
        
        // This test is optional but cheap.
        if (argb64 != this.argb64) {
            this.argb64 = argb64;
            this.colorLazy = null;
            this.setBackingArgb64(argb64);
        }
    }

    @Override
    public int getArgb32() {
        return Argb3264.toArgb32(this.argb64);
    }

    @Override
    public void setArgb32(int argb32) {
        this.setArgb64(Argb3264.toArgb64(argb32));
    }

    /*
     * 
     */
    
    @Override
    public InterfaceBwdFont getFont() {
        return this.font;
    }
    
    @Override
    public void setFont(InterfaceBwdFont font) {
        this.checkUsable();
        
        this.font = LangUtils.requireNonNull(font);
        this.setBackingFont(font);
    }
    
    /*
     * 
     */
    
    @Override
    public void clearRectOpaque(GRect rect) {
        this.clearRectOpaque(rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
    }
    
    /*
     * 
     */
    
    @Override
    public void drawPoint(int x, int y) {
        this.checkUsable();
        
        this.getPrimitives().drawPoint(
                this.getClipInUser(),
                x, y);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        this.checkUsable();
        
        this.getPrimitives().drawLine(
                this.getClipInUser(),
                x1, y1, x2, y2);
    }

    @Override
    public int drawLineStipple(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        this.checkUsable();
        
        GprimUtils.checkFactorAndPixelNum(factor, pixelNum);
        // Normalizing before, not to have to care about
        // int-wrapping during computations.
        pixelNum = GprimUtils.pixelNumNormalized(factor, pixelNum);
        
        return this.getPrimitives().drawLine(
                this.getClipInUser(),
                x1, y1, x2, y2,
                factor, pattern, pixelNum);
    }
    
    /*
     * 
     */
    
    @Override
    public void drawRect(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        this.getPrimitives().drawRect(
                this.getClipInUser(),
                x, y, xSpan, ySpan);
    }

    @Override
    public void drawRect(GRect rect) {
        this.drawRect(rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
    }

    @Override
    public void fillRect(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        this.getPrimitives().fillRect(
                this.getClipInUser(),
                x, y, xSpan, ySpan,
                this.areHorVerflipped());
    }
    
    @Override
    public void fillRect(GRect rect) {
        this.fillRect(rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
    }
    
    /*
     * 
     */
    
    @Override
    public void drawOval(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        this.getPrimitives().drawOval(
                this.getClipInUser(),
                x, y, xSpan, ySpan);
    }
    
    @Override
    public void drawOval(GRect rect) {
        this.drawOval(rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
    }
    
    @Override
    public void fillOval(int x, int y, int xSpan, int ySpan) {
        this.checkUsable();
        
        this.getPrimitives().fillOval(
                this.getClipInUser(),
                x, y, xSpan, ySpan,
                this.areHorVerflipped());
    }
    
    @Override
    public void fillOval(GRect rect) {
        this.fillOval(rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
    }
    
    /*
     * 
     */
    
    @Override
    public void drawArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        this.checkUsable();
        GprimUtils.checkArcAngles(startDeg, spanDeg);

        this.getPrimitives().drawArc(
                this.getClipInUser(),
                x, y, xSpan, ySpan,
                startDeg, spanDeg);
    }
    
    @Override
    public void drawArc(GRect rect, double startDeg, double spanDeg) {
        this.drawArc(
                rect.x(), rect.y(), rect.xSpan(), rect.ySpan(),
                startDeg, spanDeg);
    }
    
    @Override
    public void fillArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        this.checkUsable();
        GprimUtils.checkArcAngles(startDeg, spanDeg);

        this.getPrimitives().fillArc(
                this.getClipInUser(),
                x, y, xSpan, ySpan,
                startDeg, spanDeg,
                this.areHorVerflipped());
    }
    
    @Override
    public void fillArc(GRect rect, double startDeg, double spanDeg) {
        this.fillArc(
                rect.x(), rect.y(), rect.xSpan(), rect.ySpan(),
                startDeg, spanDeg);
    }
    
    /*
     * 
     */
    
    @Override
    public void drawImage(
            int x, int y,
            InterfaceBwdImage image) {
        this.checkUsable();
        
        checkNotDisposed(image);

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        if ((imageWidth <= 0) || (imageHeight <= 0)) {
            return;
        }
        
        final int xSpan = GRect.trimmedSpan(x, imageWidth);
        final int ySpan = GRect.trimmedSpan(y, imageHeight);

        this.drawImageImpl(
                x, y, xSpan, ySpan,
                image,
                0, 0, imageWidth, imageHeight);
    }

    @Override
    public void drawImage(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image) {
        this.checkUsable();
        
        checkNotDisposed(image);

        if ((xSpan <= 0) || (ySpan <= 0)) {
            return;
        }

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        if ((imageWidth <= 0) || (imageHeight <= 0)) {
            return;
        }
        
        xSpan = GRect.trimmedSpan(x, xSpan);
        ySpan = GRect.trimmedSpan(y, ySpan);

        this.drawImageImpl(
                x, y, xSpan, ySpan,
                image,
                0, 0, imageWidth, imageHeight);
    }
    
    @Override
    public void drawImage(GRect rect, InterfaceBwdImage image) {
        this.drawImage(
                rect.x(), rect.y(), rect.xSpan(), rect.ySpan(),
                image);
    }

    @Override
    public void drawImage(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image,
            int sx, int sy, int sxSpan, int sySpan) {
        this.checkUsable();
        
        checkNotDisposed(image);

        if ((xSpan <= 0) || (ySpan <= 0)) {
            return;
        }

        if ((sxSpan <= 0) || (sySpan <= 0)) {
            return;
        }

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        if ((imageWidth <= 0) || (imageHeight <= 0)) {
            return;
        }
        
        xSpan = GRect.trimmedSpan(x, xSpan);
        ySpan = GRect.trimmedSpan(y, ySpan);
        sxSpan = GRect.trimmedSpan(sx, sxSpan);
        sySpan = GRect.trimmedSpan(sy, sySpan);

        // Spec says so.
        final boolean mustApplyImgBoxToInitialSrcRect = true;
        if (mustApplyImgBoxToInitialSrcRect) {
            final GRect imgBox = GRect.valueOf(0, 0, imageWidth, imageHeight);
            sxSpan = GRect.intersectedSpan(imgBox.x(), imgBox.xSpan(), sx, sxSpan);
            if (sxSpan <= 0) {
                return;
            }
            sySpan = GRect.intersectedSpan(imgBox.y(), imgBox.ySpan(), sy, sySpan);
            if (sySpan <= 0) {
                return;
            }
            sx = GRect.intersectedPos(imgBox.x(), sx);
            sy = GRect.intersectedPos(imgBox.y(), sy);
        }
        
        this.drawImageImpl(
                x, y, xSpan, ySpan,
                image,
                sx, sy, sxSpan, sySpan);
    }
    
    @Override
    public void drawImage(GRect rect, InterfaceBwdImage image, GRect sRect) {
        this.drawImage(
                rect.x(), rect.y(), rect.xSpan(), rect.ySpan(),
                image,
                sRect.x(), sRect.y(), sRect.xSpan(), sRect.ySpan());
    }
    
    /*
     * 
     */

    @Override
    public void flipColors(GRect rect) {
        this.flipColors(rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
    }
    
    /*
     * 
     */

    /**
     * This default implementation delegates to getArgb32At(...).
     */
    @Override
    public long getArgb64At(int x, int y) {
        final int argb32 = this.getArgb32At(x, y);
        final long argb64 = Argb3264.toArgb64(argb32);
        return argb64;
    }

    /**
     * This default implementation returns 0 (fully transparent color).
     */
    @Override
    public int getArgb32At(int x, int y) {
        this.checkUsable();
        
        final GRect baseClip = this.getBaseClipInUser();
        if (!baseClip.contains(x, y)) {
            throw new IllegalArgumentException("position (" + x + "," + y + ") out of base clip " + baseClip);
        }
        return 0;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected boolean areHorVerflipped() {
        return this.getTransform().rotation().sin() != 0;
    }

    /*
     * 
     */
    
    /**
     * Checks that init() has been called, and finish() not yet.
     */
    protected final void checkUsable() {
        if (this.notUsable) {
            this.throwIaeIfNotUsable();
        }
    }

    /**
     * Checks that finish() has not been called.
     */
    protected final void checkFinishNotCalled() {
        if (this.finishCalled) {
            this.throwIae_finishCalled();
        }
    }

    /*
     * 
     */
    
    /**
     * Only called once even in case of multiple calls to init().
     * 
     * If overriding it, must call super.initImpl() at some point in it.
     */
    protected void initImpl() {
        this.removeAllAddedClips();
        this.setTransform(GTransform.IDENTITY);
        this.setArgb64(this.argb64);
        this.setFont(this.font);
        // Setting backing values explicitly, because they might never have
        // been set yet, and above methods might not have set them if they saw
        // they didn't change.
        this.onNewClip();
        this.onNewTransform();
        this.setBackingArgb64(this.argb64);
        this.setBackingFont(this.font);
    }
    
    /**
     * Only called once even in case of multiple calls to finish(),
     * and only called if initImpl() has been called.
     */
    protected abstract void finishImpl();
    
    /*
     * It is allowed to take care, of backing clip/transform in each
     * method call rather than in these methods (for example, to use GTransform
     * conversions before using non-transformed backing graphics, in case that
     * would be faster, or in case the actual transform to apply on the backing
     * graphics depends on the method called), in which case these implementations
     * could do nothing, or just set some dirty flag.
     */
    
    /**
     * Must update backing clip with current clip (transformed or not,
     * depending on whether backing graphics has transform applied).
     */
    protected abstract void onNewClip();
    
    /**
     * Must update backing transform according to current transform,
     * or not if it is transformed and de-transformed for each draw.
     */
    protected abstract void onNewTransform();
    
    protected abstract void setBackingArgb64(long argb64);
    
    protected abstract void setBackingFont(InterfaceBwdFont font);
    
    /**
     * If you override all primitive operations, this method won't be used,
     * and can just be made to throw UOE for example.
     * 
     * Not making it throw UOE by default, to make sure the user is aware
     * of its existence and interaction with default methods implementations.
     * 
     * @return Primitives drawing operations used by default implementations.
     */
    protected abstract AbstractBwdPrimitives getPrimitives();

    /*
     * Images.
     */
    
    /**
     * Source, destination and image spans are all > 0,
     * rectangles don't overflow out of int range,
     * and source has been clipped into image rectangle
     * (as spec says).
     */
    protected abstract void drawImageImpl(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image,
            int sx, int sy, int sxSpan, int sySpan);

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Checks for graphics creation.
     * @throws IllegalArgumentException if the specified base clip is not empty
     *         and not contained in the specified box.
     */
    private static void checkGraphicsInitialBoxAndClip(
            GRect box,
            GRect baseClip) {
        LangUtils.requireNonNull(box);
        // Implicit null check.
        if ((!baseClip.isEmpty()) && (!box.contains(baseClip))) {
            throw new IllegalArgumentException("non-empty baseClip " + baseClip + " must be contained in box " + box);
        }
    }

    private void throwIaeIfNotUsable() {
        if (!this.initCalled) {
            throwIae_initNotCalled();
        }
        if (this.finishCalled) {
            throwIae_finishCalled();
        }
    }

    private void throwIae_initNotCalled() {
        throw new IllegalStateException("init() has not been called yet");
    }

    private void throwIae_finishCalled() {
        throw new IllegalStateException("finish() has been called already");
    }

    private void updateTransformedClips() {
        this.baseClipInUser = this.transform.rectIn2(this.baseClipInClient);
        this.clipInUser = this.transform.rectIn2(this.clipInClient);
    }
    
    private static void checkNotDisposed(InterfaceBwdImage image) {
        // Implicit null check.
        if (image.isDisposed()) {
            throw new IllegalArgumentException("image is disposed: " + image);
        }
    }
}
