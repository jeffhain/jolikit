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
    
    private static final BwdColor DEFAULT_COLOR = BwdColor.BLACK;
    
    private static final int SMALL_ARRAY_LIST_CAPACITY = 1;
    
    /*
     * 
     */
    
    private final InterfaceBwdBinding binding;

    /*
     * 
     */
    
    /**
     * Box in base coordinates.
     */
    private final GRect box;
    
    /**
     * Initial clip in base coordinates.
     */
    private final GRect initialClipInBase;
    
    /**
     * Initial clip in user coordinates.
     */
    private GRect initialClipInUser;
    
    /**
     * Clip in base coordinates.
     */
    private GRect clipInBase;
    
    /**
     * Clip in user coordinates.
     */
    private GRect clipInUser;
    
    /**
     * Lazily created.
     */
    private ArrayList<GRect> clipsBeforeLastAdd = null;
    
    /**
     * Transform between base coordinates (frame 1)
     * and user coordinates (frame 2).
     * 
     * Never using the transform of the backing graphics,
     * for simplicity and consistency across bindings.
     */
    private GTransform transform;
    
    /**
     * Lazily created.
     * Kept across resets.
     */
    private ArrayList<GTransform> transformsBeforeLastAdd = null;
    
    /**
     * The reference color.
     */
    private long argb64;
    
    /**
     * Lazily initialized after argb64 modification.
     */
    private BwdColor colorLazy;
    
    private InterfaceBwdFont font;
    
    /*
     * 
     */
    
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
            GRect box,
            GRect initialClip) {
        checkGraphicsBoxAndInitialClip(box, initialClip);
        
        // Implicit null check.
        final InterfaceBwdFont defaultFont = binding.getFontHome().getDefaultFont();
        
        this.binding = binding;
        
        this.box = box;
        this.initialClipInBase = initialClip;
        
        this.initialClipInUser = initialClip;
        this.clipInBase = initialClip;
        this.clipInUser = initialClip;
        
        if (this.clipsBeforeLastAdd != null) {
            this.clipsBeforeLastAdd.clear();
        }
        
        this.transform = GTransform.IDENTITY;
        
        if (this.transformsBeforeLastAdd != null) {
            this.transformsBeforeLastAdd.clear();
        }

        this.argb64 = DEFAULT_COLOR.toArgb64();
        this.colorLazy = DEFAULT_COLOR;

        this.font = defaultFont;
    }
    
    @Override
    public String toString() {
        return "[box = "
                + this.box
                + ", initialClipInBase = "
                + this.initialClipInBase
                + ", transform = "
                + this.transform
                + ", clipInUser = "
                + this.clipInUser
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
    public GRect getBox() {
        return this.box;
    }
    
    /*
     * 
     */

    @Override
    public GRect getInitialClipInBase() {
        return this.initialClipInBase;
    }
    
    @Override
    public GRect getInitialClipInUser() {
        return this.initialClipInUser;
    }

    @Override
    public GRect getClipInBase() {
        return this.clipInBase;
    }
    
    @Override
    public GRect getClipInUser() {
        return this.clipInUser;
    }

    @Override
    public void addClipInBase(GRect clip) {
        this.checkUsable();
        
        final GRect oldClip = this.clipInBase;
        // Implicit null check.
        final GRect newClip = oldClip.intersected(clip);
        
        ArrayList<GRect> preList = this.clipsBeforeLastAdd;
        if (preList == null) {
            preList = new ArrayList<GRect>(SMALL_ARRAY_LIST_CAPACITY);
            this.clipsBeforeLastAdd = preList;
        }
        preList.add(oldClip);
        
        if (!newClip.equals(oldClip)) {
            this.clipInBase = newClip;
            this.updateTransformedClips();
            this.setBackingClip(newClip);
        }
    }

    @Override
    public void addClipInUser(GRect clip) {
        final GRect clipInBase = this.getTransform().rectIn1(clip);
        this.addClipInBase(clipInBase);
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
        
        final GRect oldClip = this.clipInBase;
        final GRect newClip = preList.remove(size-1);
        
        if (!newClip.equals(oldClip)) {
            this.clipInBase = newClip;
            this.updateTransformedClips();
            this.setBackingClip(newClip);
        }
    }
    
    @Override
    public void removeAllAddedClips() {
        this.checkUsable();
        
        final ArrayList<GRect> preList = this.clipsBeforeLastAdd;
        if (preList != null) {
            preList.clear();
        }
        
        final GRect oldClip = this.clipInBase;
        final GRect newClip = this.initialClipInBase;
        
        if (!newClip.equals(oldClip)) {
            this.clipInBase = newClip;
            this.updateTransformedClips();
            this.setBackingClip(newClip);
        }
    }
    
    @Override
    public boolean isClipEmpty() {
        return this.getClipInBase().isEmpty();
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
        
        if (!newTransform.equals(oldTransform)) {
            this.transform = newTransform;
            this.updateTransformedClips();
            this.setBackingTransform(newTransform);
        }
    }

    @Override
    public void addTransform(GTransform transform) {
        this.checkUsable();
        
        ArrayList<GTransform> preList = this.transformsBeforeLastAdd;
        if (preList == null) {
            preList = new ArrayList<GTransform>(SMALL_ARRAY_LIST_CAPACITY);
            this.transformsBeforeLastAdd = preList;
        }
        
        final GTransform oldTransform = this.transform;
        // Implicit null check.
        final GTransform newTransform = oldTransform.composed(transform);
        preList.add(oldTransform);
        
        if (!newTransform.equals(oldTransform)) {
            this.transform = newTransform;
            this.updateTransformedClips();
            this.setBackingTransform(newTransform);
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
        
        if (!newTransform.equals(oldTransform)) {
            this.transform = newTransform;
            this.updateTransformedClips();
            this.setBackingTransform(newTransform);
        }
    }
    
    @Override
    public void removeAllAddedTransforms() {
        this.checkUsable();
        
        final ArrayList<GTransform> preList = this.transformsBeforeLastAdd;
        final int size = ((preList != null) ? preList.size() : 0);
        if (size == 0) {
            // Nothing to do.
            return;
        }
        
        final GTransform oldTransform = this.transform;
        final GTransform newTransform = preList.get(0);
        preList.clear();
        
        if (!newTransform.equals(oldTransform)) {
            this.transform = newTransform;
            this.updateTransformedClips();
            this.setBackingTransform(newTransform);
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
        
        this.colorLazy = color;
        final long argb64 = color.toArgb64();
        
        if (argb64 != this.argb64) {
            this.argb64 = argb64;
            this.setBackingArgb64(argb64);
        }
    }
    
    @Override
    public long getArgb64() {
        return this.argb64;
    }

    @Override
    public void setArgb64(long argb64) {
        this.checkUsable();
        
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
    public void clearRect(GRect rect) {
        this.clearRect(rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
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
                this.areHorVerFlipped());
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
                this.areHorVerFlipped());
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
                this.areHorVerFlipped());
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
        
        final GRect initialClip = this.getInitialClipInUser();
        if (!initialClip.contains(x, y)) {
            throw new IllegalArgumentException(
                    "position (" + x + "," + y + ") out of initial clip " + initialClip);
        }
        return 0;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Useful to choose to fill rectangles with vertical lines rather than
     * horizontal lines when being in user coordinates.
     * 
     * @return True if rotation is such that horizontal/vertical lines
     *         in user coordinates are vertical/horizontal lines
     *         in base coordinates. 
     */
    protected boolean areHorVerFlipped() {
        return this.getTransform().areHorVerFlipped();
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
        
        this.checkUsable();
        
        /*
         * Non-backing state is properly initialized at creation time,
         * so here we just need to initialize backing state.
         */
        
        final boolean mustSetClip = true;
        final boolean mustSetTransform = true;
        final boolean mustSetColor = true;
        final boolean mustSetFont = true;
        this.setBackingState(
                mustSetClip,
                this.initialClipInBase,
                //
                mustSetTransform,
                this.transform,
                //
                mustSetColor,
                this.argb64,
                //
                mustSetFont,
                this.font);
    }
    
    /**
     * Only called once even in case of multiple calls to finish(),
     * and only called if initImpl() has been called.
     */
    protected abstract void finishImpl();
    
    /*
     * Backing state setting methods.
     * 
     * There are individual methods (for clip, transform, color and font),
     * and a bulk method (for optimization, to avoid eventual redundancies
     * from calling all four individual methods).
     * Default implementations are proposed for each, the ones for individual
     * methods delegating to the bulk method, and vice versa.
     * 
     * When these methods are called, the non-backing state has already been
     * updated, but it is given as argument for convenience and performances.
     * 
     * NB: It is possible to take care of backing clip and transform in each
     * painting method call rather than in these methods (for example, to use
     * GTransform conversions before using non-transformed backing graphics,
     * in case that would be faster, or in case the actual transform to apply
     * on the backing graphics depends on the method called), in which case
     * these implementations could do nothing, or just set some dirty flag.
     */
    
    /**
     * Sets the backing clip, if any.
     */
    protected abstract void setBackingClip(GRect clipInBase);
    
    /**
     * A default implementation for setBackingClip(...).
     * Delegates to the bulk method.
     */
    protected void setBackingClipDefaultImpl(GRect clipInBase) {
        final boolean mustSetClip = true;
        final boolean mustSetTransform = false;
        final boolean mustSetColor = false;
        final boolean mustSetFont = false;
        this.setBackingState(
                mustSetClip,
                clipInBase,
                //
                mustSetTransform,
                null,
                //
                mustSetColor,
                0L,
                //
                mustSetFont,
                null);
    }
    
    /**
     * Sets the backing transform, if any,
     * and possibly also the backing clip
     * if impacted by the transform.
     */
    protected abstract void setBackingTransform(GTransform transform);
    
    /**
     * A default implementation for setBackingTransform(...).
     * Delegates to the bulk method.
     */
    protected void onNewTransformDefaultImpl(GTransform transform) {
        final boolean mustSetClip = false;
        final boolean mustSetTransform = true;
        final boolean mustSetColor = false;
        final boolean mustSetFont = false;
        this.setBackingState(
                mustSetClip,
                null,
                //
                mustSetTransform,
                transform,
                //
                mustSetColor,
                0L,
                //
                mustSetFont,
                null);
    }
    
    /**
     * Sets the backing color, if any.
     */
    protected abstract void setBackingArgb64(long argb64);
    
    /**
     * A default implementation for setBackingArgb64(...).
     * Delegates to the bulk method.
     */
    protected void setBackingArgb64DefaultImpl(long argb64)  {
        final boolean mustSetClip = false;
        final boolean mustSetTransform = false;
        final boolean mustSetColor = true;
        final boolean mustSetFont = false;
        this.setBackingState(
                mustSetClip,
                null,
                //
                mustSetTransform,
                null,
                //
                mustSetColor,
                argb64,
                //
                mustSetFont,
                null);
    }
    
    /**
     * Sets the backing font, if any.
     */
    protected abstract void setBackingFont(InterfaceBwdFont font);
    
    /**
     * A default implementation for setBackingFont(...).
     * Delegates to the bulk method.
     */
    protected void setBackingFontDefaultImpl(InterfaceBwdFont font) {
        final boolean mustSetClip = false;
        final boolean mustSetTransform = false;
        final boolean mustSetColor = false;
        final boolean mustSetFont = true;
        this.setBackingState(
                mustSetClip,
                null,
                //
                mustSetTransform,
                null,
                //
                mustSetColor,
                0L,
                //
                mustSetFont,
                font);
    }
    
    /**
     * Bulk version.
     * Sets all or parts of the backing state, if any, at once.
     * 
     * @param mustSetClip If false, clip argument must be ignored, and might be null.
     * @param mustSetTransform If false, transform argument must be ignored, and might be null.
     * @param mustSetColor If false, color argument must be ignored.
     * @param mustSetFont If false, font argument must be ignored, and might be null.
     */
    protected abstract void setBackingState(
        boolean mustSetClip,
        GRect clipInBase,
        //
        boolean mustSetTransform,
        GTransform transform,
        //
        boolean mustSetColor,
        long argb64,
        //
        boolean mustSetFont,
        InterfaceBwdFont font);
    
    /**
     * A default implementation for setBackingState(...).
     * Delegates to individual methods.
     */
    protected void setBackingStateDefaultImpl(
        boolean mustSetClip,
        GRect clipInBase,
        //
        boolean mustSetTransform,
        GTransform transform,
        //
        boolean mustSetColor,
        long argb64,
        //
        boolean mustSetFont,
        InterfaceBwdFont font) {
        
        if (mustSetClip) {
            this.setBackingClip(clipInBase);
        }
        
        if (mustSetTransform) {
            this.setBackingTransform(transform);
        }
        
        if (mustSetColor) {
            this.setBackingArgb64(argb64);
        }
        
        if (mustSetFont) {
            this.setBackingFont(font);
        }
    }
    
    /*
     * 
     */

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
     * @throws IllegalArgumentException if the specified initial clip
     *         is not empty and not contained in the specified box.
     */
    private static void checkGraphicsBoxAndInitialClip(
            GRect box,
            GRect initialClip) {
        LangUtils.requireNonNull(box);
        // Implicit null check on initialClip.
        if ((!initialClip.isEmpty()) && (!box.contains(initialClip))) {
            throwIae_boxAndInitialClip(box, initialClip);
        }
    }

    private static void checkNotDisposed(InterfaceBwdImage image) {
        // Implicit null check.
        if (image.isDisposed()) {
            throwIae_imageNotDisposed(image);
        }
    }

    private static void throwIae_boxAndInitialClip(GRect box, GRect initialClip) {
        throw new IllegalArgumentException(
                "non-empty initialClip " + initialClip
                + " must be contained in box " + box);
    }

    private static void throwIae_imageNotDisposed(InterfaceBwdImage image) {
        throw new IllegalArgumentException("image is disposed: " + image);
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
        this.initialClipInUser = this.transform.rectIn2(this.initialClipInBase);
        this.clipInUser = this.transform.rectIn2(this.clipInBase);
    }
}
