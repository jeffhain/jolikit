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
package net.jolikit.bwd.impl.utils.graphics;

import java.util.ArrayList;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.Argb3264;
import net.jolikit.bwd.api.graphics.Argb64;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
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
    private static final int DEFAULT_ARGB_32 = DEFAULT_COLOR.toArgb32();
    
    private static final int SMALL_ARRAY_LIST_CAPACITY = 1;
    
    /*
     * 
     */
    
    private final InterfaceBwdBindingImpl binding;

    /*
     * 
     */
    
    /**
     * Coordinates, in base frame of reference,
     * of root box's top-left pixel.
     */
    private final GPoint rootBoxTopLeft;
    
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
     * Kept across resets.
     */
    private ArrayList<GRect> clipsBeforeLastAdd = null;
    
    /**
     * Transform between base coordinates (frame 1)
     * and user coordinates (frame 2).
     * 
     * Never using the transform of the backing graphics,
     * for simplicity and consistency across bindings.
     */
    private GTransform transform = GTransform.IDENTITY;
    
    /**
     * Lazily created.
     * Kept across resets.
     */
    private ArrayList<GTransform> transformsBeforeLastAdd = null;
    
    /*
     * Color.
     * If user uses argb32 rather than argb64, that could be for performances,
     * so we optimize for this case:
     * - We always keep an argb32 value up to date.
     * - We use the field of type BwdColor to contain the 64 bits
     *   information if specified or needed by the user.
     */
    
    private int argb32 = DEFAULT_ARGB_32;
    
    /**
     * For optimization, because bindings often need it.
     */
    private int premulArgb32 = BindingColorUtils.toPremulAxyz32(DEFAULT_ARGB_32);

    /**
     * Set lazily on retrieval, or when user specifies
     * a color with 64 bits precision.
     */
    private BwdColor colorElseNull = DEFAULT_COLOR;
    
    /*
     * 
     */
    
    private InterfaceBwdFont font;
    
    /*
     * 
     */
    
    /**
     * For this flag, not providing a way to set a corresponding
     * "backing" state: drawImage() methods implementations must
     * simply read and apply the flag at call time, if they have
     * multiple algorithms to choose from.
     */
    private boolean accurateImageScaling;
    
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
        InterfaceBwdBindingImpl binding,
        GPoint rootBoxTopLeft,
        GRect box,
        GRect initialClip) {
        
        checkGraphicsBoxAndInitialClip(box, initialClip);
        
        // Implicit null check.
        final InterfaceBwdFont defaultFont = binding.getFontHome().getDefaultFont();
        
        this.binding = binding;
        
        this.rootBoxTopLeft = LangUtils.requireNonNull(rootBoxTopLeft);
        this.box = box;
        this.initialClipInBase = initialClip;
        
        this.initialClipInUser = initialClip;
        this.clipInBase = initialClip;
        this.clipInUser = initialClip;
        
        this.font = defaultFont;
        
        this.accurateImageScaling = false;
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
            + Argb64.toString(this.getArgb64())
            + ", font = "
            + this.font
            + "]";
    }

    /*
     * 
     */
    
    public InterfaceBwdBindingImpl getBinding() {
        return this.binding;
    }
    
    /**
     * Convenience method.
     */
    public BaseBwdBindingConfig getBindingConfig() {
        return this.binding.getBindingConfig();
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
    
    @Override
    public InterfaceBwdGraphics newChildGraphics(GRect childBox) {
        return this.newChildGraphics(childBox, childBox);
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
        } else {
            this.finishWithoutInitImpl();
        }
    }
    
    @Override
    public void reset() {
        this.checkUsable();
        
        /*
         * Clip.
         */
        
        final GRect oldClipInBase = this.clipInBase;
        final GRect newClipInBase = this.initialClipInBase;
        final boolean mustResetClip = !newClipInBase.equals(oldClipInBase);
        if (mustResetClip) {
            this.clipInBase = newClipInBase;
        }
        // Always resetting clips in user, in case of transform change.
        this.initialClipInUser = newClipInBase;
        this.clipInUser = newClipInBase;
        
        // Always resetting clip stack.
        if (this.clipsBeforeLastAdd != null) {
            this.clipsBeforeLastAdd.clear();
        }
        
        /*
         * Transform.
         */
        
        final GTransform oldTransform = this.transform;
        final GTransform newTransform = GTransform.IDENTITY;
        final boolean mustResetTransform = !newTransform.equals(oldTransform);
        if (mustResetTransform) {
            this.setInternalTransform(newTransform);
        }
        
        // Always resetting transform stack.
        if (this.transformsBeforeLastAdd != null) {
            this.transformsBeforeLastAdd.clear();
        }
        
        /*
         * Color.
         */

        final int oldArgb32 = this.argb32;
        final BwdColor oldColorElseNull = this.colorElseNull;
        final int newArgb32 = DEFAULT_ARGB_32;
        final BwdColor newColor = DEFAULT_COLOR;
        final boolean isOldColorDefined = (oldColorElseNull != null);
        final boolean mustResetColor =
                (newArgb32 != oldArgb32)
                || (isOldColorDefined
                        && (!newColor.equals(oldColorElseNull)));
        if (mustResetColor) {
            this.argb32 = newArgb32;
            this.premulArgb32 = BindingColorUtils.toPremulAxyz32(newArgb32);
        }
        this.colorElseNull = newColor;

        /*
         * Font.
         */
        
        final InterfaceBwdFont oldFont = this.font;
        final InterfaceBwdFont newFont = this.binding.getFontHome().getDefaultFont();
        final boolean mustResetFont = !newFont.equals(oldFont);
        if (mustResetFont) {
            this.font = newFont;
        }
        
        /*
         * Images.
         */
        
        this.accurateImageScaling = false;

        /*
         * Backing state.
         */
        
        this.setBackingState(
                mustResetClip,
                newClipInBase,
                //
                mustResetTransform,
                newTransform,
                //
                mustResetColor,
                newArgb32,
                newColor,
                //
                mustResetFont,
                newFont);
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
            this.setInternalTransform(newTransform);
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
            this.setInternalTransform(newTransform);
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
            this.setInternalTransform(newTransform);
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
            this.setInternalTransform(newTransform);
            this.updateTransformedClips();
            this.setBackingTransform(newTransform);
        }
    }

    /*
     * 
     */
    
    @Override
    public BwdColor getColor() {
        BwdColor color = this.colorElseNull;
        if (color == null) {
            color = BwdColor.valueOfArgb32(this.argb32);
            this.colorElseNull = color;
        }
        return color;
    }

    @Override
    public void setColor(BwdColor color) {
        this.updateColorsIfNeeded(
                color.toArgb32(),
                color);
    }
    
    @Override
    public long getArgb64() {
        BwdColor color = this.colorElseNull;
        if (color == null) {
            color = BwdColor.valueOfArgb32(this.argb32);
            this.colorElseNull = color;
        }
        return color.toArgb64();
    }

    @Override
    public void setArgb64(long argb64) {
        this.updateColorsIfNeeded(
                Argb3264.toArgb32(argb64),
                BwdColor.valueOfArgb64(argb64));
    }

    @Override
    public int getArgb32() {
        return this.argb32;
    }

    @Override
    public void setArgb32(int argb32) {
        final BwdColor colorElseNull = null;
        this.updateColorsIfNeeded(
                argb32,
                colorElseNull);
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
        
        if (GprimUtils.mustComputePixelNum(pattern)) {
            // Normalizing before, not to have to care about
            // int-wrapping during computations.
            pixelNum = GprimUtils.pixelNumNormalized(factor, pixelNum);
        }
        
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
    public void drawPolyline(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        this.checkUsable();
        GprimUtils.checkPolyArgs(xArr, yArr, pointCount);

        this.getPrimitives().drawPolyline(
                this.getClipInUser(),
                xArr,
                yArr,
                pointCount);
    }

    @Override
    public void drawPolygon(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        this.checkUsable();
        GprimUtils.checkPolyArgs(xArr, yArr, pointCount);

        this.getPrimitives().drawPolygon(
                this.getClipInUser(),
                xArr,
                yArr,
                pointCount);
    }

    @Override
    public void fillPolygon(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        this.checkUsable();
        GprimUtils.checkPolyArgs(xArr, yArr, pointCount);

        this.getPrimitives().fillPolygon(
                this.getClipInUser(),
                xArr,
                yArr,
                pointCount,
                this.areHorVerFlipped());
    }
    
    /*
     * 
     */
    
    @Override
    public void setAccurateImageScaling(boolean accurate) {
        this.checkUsable();
        
        this.accurateImageScaling = accurate;
    }
    
    @Override
    public void drawImage(
            int x, int y,
            InterfaceBwdImage image) {
        this.checkUsable();
        
        this.checkIsNotThisGraphicsImage(image);
        
        checkNotDisposed(image);

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        if ((imageWidth <= 0) || (imageHeight <= 0)) {
            if (DEBUG) {
                Dbg.log("drawImage(...) : empty image");
            }
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
        
        this.checkIsNotThisGraphicsImage(image);
        
        checkNotDisposed(image);

        if ((xSpan <= 0) || (ySpan <= 0)) {
            if (DEBUG) {
                Dbg.log("drawImage(...) : empty dst span");
            }
            return;
        }

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        if ((imageWidth <= 0) || (imageHeight <= 0)) {
            if (DEBUG) {
                Dbg.log("drawImage(...) : empty image");
            }
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
        
        this.checkIsNotThisGraphicsImage(image);
        
        checkNotDisposed(image);

        if ((xSpan <= 0) || (ySpan <= 0)) {
            if (DEBUG) {
                Dbg.log("drawImage(...) : empty dst span");
            }
            return;
        }

        if ((sxSpan <= 0) || (sySpan <= 0)) {
            if (DEBUG) {
                Dbg.log("drawImage(...) : empty src span");
            }
            return;
        }

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        if ((imageWidth <= 0) || (imageHeight <= 0)) {
            if (DEBUG) {
                Dbg.log("drawImage(...) : empty image");
            }
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
                if (DEBUG) {
                    Dbg.log("drawImage(...) : empty reduced sxSpan");
                }
                return;
            }
            sySpan = GRect.intersectedSpan(imgBox.y(), imgBox.ySpan(), sy, sySpan);
            if (sySpan <= 0) {
                if (DEBUG) {
                    Dbg.log("drawImage(...) : empty reduced sySpan");
                }
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
     * For use for or in constructors for root graphics,
     * to properly handle root boxes with negative
     * top left (x,y) coordinates.
     */
    protected static GPoint topLeftOf(GRect box) {
        return GPoint.valueOf(box.x(), box.y());
    }

    /**
     * @return Base coordinates of root box top-left pixel.
     */
    protected final GPoint getRootBoxTopLeft() {
        return this.rootBoxTopLeft;
    }
    
    protected final GTransform getTransform_final() {
        return this.transform;
    }

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

    /**
     * Useful for performances when you only need
     * the alpha premultiplied flavor.
     * 
     * @return The alpha premultiplied flavor of current argb32.
     */
    protected int getPremulArgb32() {
        return this.premulArgb32;
    }
    
    /**
     * This base implementation just sets the internal instance.
     * 
     * To be overridden if wanting to update related data.
     * Overriding must call super.
     * 
     * @param transform Transform to use for this graphics.
     */
    protected void setInternalTransform(GTransform newTransform) {
        this.transform = newTransform;
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
                this.argb32,
                this.colorElseNull,
                //
                mustSetFont,
                this.font);
    }
    
    /**
     * Only called once even in case of multiple calls to finish(),
     * and only called if initImpl() has been called.
     */
    protected abstract void finishImpl();
    
    /**
     * Called on a call to finish() without prior call to init().
     * Useful to release resources created or shared on graphics creation.
     * This default implementation does nothing.
     */
    protected void finishWithoutInitImpl() {
    }
    
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
                0,
                null,
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
                0,
                null,
                //
                mustSetFont,
                null);
    }
    
    /**
     * Sets the backing color, if any.
     * 
     * @param colorElseNull Can be null. Non null if color has 64 bits precision,
     *        or if has already been computed from 32 bits precision color.
     */
    protected abstract void setBackingArgb(int argb32, BwdColor colorElseNull);
    
    /**
     * A default implementation for setBackingArgb(...).
     * Delegates to the bulk method.
     */
    protected void setBackingArgbDefaultImpl(int argb32, BwdColor colorElseNull)  {
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
                argb32,
                colorElseNull,
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
                0,
                null,
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
     * @param mustSetColor If false, color arguments must be ignored.
     * @param colorElseNull Can be null. Non null if color has 64 bits precision,
     *        or if has already been computed from 32 bits precision color.
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
        int argb32,
        BwdColor colorElseNull,
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
        int argb32,
        BwdColor colorElseNull,
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
            this.setBackingArgb(argb32, colorElseNull);
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
     * For use in drawImageImpl().
     * 
     * @return Whether drawImageImpl() must use accurate or fast
     *         image scaling algorithms, if there are multiple ones
     *         to choose from.
     */
    protected final boolean getAccurateImageScaling() {
        return this.accurateImageScaling;
    }
    
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
    
    private void checkIsNotThisGraphicsImage(InterfaceBwdImage image) {
        if (image instanceof InterfaceBwdWritableImage) {
            final InterfaceBwdWritableImage wi =
                    (InterfaceBwdWritableImage) image;
            final InterfaceBwdGraphics wig = wi.getGraphics();
            // Works as long as bindings don't get fancy with wrappers.
            if (wig == this) {
                throw new IllegalArgumentException("can't draw an image into itself");
            }
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
    
    /**
     * If newColorElseNull is not null, argb32 must be
     * the same as newColorElseNull.toArgb32().
     */
    private void updateColorsIfNeeded(int newArgb32, BwdColor newColorElseNull) {
        this.checkUsable();
        
        final int oldArgb32 = this.argb32;
        final BwdColor oldColorElseNull = this.colorElseNull;
        
        final boolean mustUpdateColors;
        if (oldColorElseNull != null) {
            if (newColorElseNull != null) {
                /*
                 * 64 bits representation changed: must update.
                 */
                mustUpdateColors = (!newColorElseNull.equals(oldColorElseNull));
            } else {
                /*
                 * No new 64 bits representation: we only want to update
                 * if old 64 bits representation no longer corresponds
                 * to new 32 bits representation converted to 64 bits
                 * (for example to make sure that someone setting
                 * full black as argb32 doesn't end up with something
                 * almost but not quite black in 64 bits representation).
                 */
                final long newArgb64 = Argb3264.toArgb64(newArgb32);
                mustUpdateColors = (oldColorElseNull.toArgb64() != newArgb64);
            }
        } else {
            /*
             * No old 64 bits representation: must only update
             * if 32 bits representation changed.
             */
            mustUpdateColors = (newArgb32 != oldArgb32);
        }
        if (mustUpdateColors) {
            this.argb32 = newArgb32;
            this.premulArgb32 = BindingColorUtils.toPremulAxyz32(newArgb32);
            this.colorElseNull = newColorElseNull;
            this.setBackingArgb(newArgb32, newColorElseNull);
        } else {
            if (newColorElseNull != null) {
                /*
                 * Not required, but updating it here,
                 * to avoid eventual lazy update later.
                 */
                this.colorElseNull = newColorElseNull;
            }
        }
    }
}
