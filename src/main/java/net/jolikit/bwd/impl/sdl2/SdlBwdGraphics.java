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
package net.jolikit.bwd.impl.sdl2;

import java.util.ArrayList;

import com.sun.jna.Pointer;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Color;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Surface;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLibTtf;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaUtils;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.fonts.BindingTextUtils;
import net.jolikit.bwd.impl.utils.graphics.AbstractIntArrayBwdGraphics;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.Dbg;

public class SdlBwdGraphics extends AbstractIntArrayBwdGraphics {

    /*
     * Not using per-pixel SDL treatments, such as SDL_DRAW_PUTPIXEL_BPP_4,
     * for it's extremely slow (from Java a least).
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /**
     * TODO sdl For some reasons, SDL seem to draw the text with random
     * {r,g,b} components (but not alpha), and as if it would use some color's
     * instance-specific hash code instead (if the color instance is always
     * the same, the random color is always the same).
     * To work that around the best we can, we just replace whatever pixel
     * with non-zero alpha with the user-specified {r,g,b}, using a combination
     * of user-specified alpha and anti-aliased alpha.
     */
    private static final boolean MUST_ENFORCE_COLOR = true;

    /**
     * TODO sdl Using TTF_RenderGlyph_XXX(...) rather than
     * TTF_RenderText_XXX(...), else we can't seem to render code points
     * above 255 (Java/C string issue?), and the NUL character (cp = 0)
     * causes subsequent characters not to be drawn.
     * Seems only about twice slower than using TTF_RenderText_XXX(...).
     * 
     * NB: If using TTF_RenderGlyph_XXX(...), characters (*) for which there is
     * no glyph seem to always be drawn as a little rectangle, while with
     * TTF_RenderText_XXX(...), for some of these characters, '?' is used.
     * (*) And other than NUL, for which nothing is drawn,
     * even if the font has a glyph for it.
     * 
     * NB: Could special case to TTF_RenderText_XXX(...) when we only have
     * drawable code points in [0,255], but we prefer to keep things simple.
     */
    private static final boolean MUST_DRAW_TEXT_GLYPH_BY_GLYPH = true;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * Clipped Text Data Accessor.
     */
    private static class MyCtda {
        final int[] argb32Arr;
        final int scanlineStride;
        final GRect rectInText;
        public MyCtda(
                int[] argb32Arr,
                int scanlineStride,
                GRect rectInText) {
            this.argb32Arr = argb32Arr;
            this.scanlineStride = scanlineStride;
            this.rectInText = rectInText;
        }
    }

    private static class MyGlyphData {
        final int[] argb32Arr;
        final int width;
        final int height;
        public MyGlyphData(
                int[] argb32Arr,
                int width,
                int height) {
            this.argb32Arr = argb32Arr;
            this.width = width;
            this.height = height;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;
    private static final SdlJnaLibTtf LIB_TTF = SdlJnaLibTtf.INSTANCE;

    /**
     * Only used if MUST_MONOCHROMATIZE is true.
     */
    private static final SDL_Color SDL_TEXT_RENDERING_COLOR = new SDL_Color();
    static {
        SDL_TEXT_RENDERING_COLOR.r = (byte) 0xFF;
        SDL_TEXT_RENDERING_COLOR.g = (byte) 0xFF;
        SDL_TEXT_RENDERING_COLOR.b = (byte) 0xFF;
        SDL_TEXT_RENDERING_COLOR.a = (byte) 0xFF;
    }

    private static final MyCtda EMPTY_CLIPPED_TEXT_DATA_ACCESSOR =
            new MyCtda(
                    new int[0],
                    0,
                    GRect.DEFAULT_EMPTY);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor for root graphics.
     */
    public SdlBwdGraphics(
        InterfaceBwdBindingImpl binding,
        GRect box,
        //
        boolean isImageGraphics,
        int[] pixelArr,
        int pixelArrScanlineStride) {
        this(
            binding,
            topLeftOf(box),
            box,
            box, // initialClip
            //
            isImageGraphics,
            pixelArr,
            pixelArrScanlineStride);
    }

    /*
     * 
     */

    @Override
    public SdlBwdGraphics newChildGraphics(
            GRect childBox,
            GRect childMaxInitialClip) {
        this.checkFinishNotCalled();
        
        if (DEBUG) {
            Dbg.log(
                    this.getClass().getSimpleName() + "-" + this.hashCode()
                    + ".newChildGraphics(" + childBox
                    + "," + childMaxInitialClip + ")");
        }
        
        final GRect childInitialClip =
                this.getInitialClipInBase().intersected(
                        childMaxInitialClip.intersected(childBox));
        
        return new SdlBwdGraphics(
                this.getBinding(),
                this.getRootBoxTopLeft(),
                childBox,
                childInitialClip,
                //
                this.isImageGraphics(),
                this.getPixelArr(),
                this.getPixelArrScanlineStride());
    }

    /*
     * 
     */

    @Override
    public SdlBwdFont getFont() {
        return (SdlBwdFont) super.getFont();
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void finishImpl() {
        // Nothing to do.
    }

    /*
     * 
     */

    @Override
    protected void setBackingFont(InterfaceBwdFont font) {
        // Nothing to do.
    }

    @Override
    protected void setBackingState(
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

        this.setBackingStateDefaultImpl(
                mustSetClip,
                clipInBase,
                //
                mustSetTransform,
                transform,
                //
                mustSetColor,
                argb32,
                colorElseNull,
                //
                mustSetFont,
                font);
    }

    /*
     * 
     */

    @Override
    protected int getArrayColor32FromArgb32(int argb32) {
        return BindingColorUtils.toPremulAxyz32(argb32);
    }

    @Override
    protected int getArgb32FromArrayColor32(int premulArgb32) {
        return BindingColorUtils.toNonPremulAxyz32(premulArgb32);
    }

    @Override
    protected int toInvertedArrayColor32(int premulArgb32) {
        return BindingColorUtils.toInvertedPremulAxyz32_noCheck(premulArgb32);
    }

    @Override
    protected int getArrayColorAlpha8(int premulArgb32) {
        return Argb32.getAlpha8(premulArgb32);
    }

    @Override
    protected int blendArrayColor32(int srcPremulArgb32, int dstPremulArgb32) {
        return BindingColorUtils.blendPremulAxyz32_srcOver(srcPremulArgb32, dstPremulArgb32);
    }

    /*
     * Text.
     */

    @Override
    protected Object getClippedTextDataAccessor(
            String text,
            GRect maxClippedTextRectInText) {

        final int argb32 = this.getArgb32();

        final SDL_Color backingFgColor;
        if (MUST_ENFORCE_COLOR) {
            /*
             * Since we don't use SDL text's (r,g,b),
             * nor its alpha directly, we don't bother
             * creating a color each time.
             */
            backingFgColor = SDL_TEXT_RENDERING_COLOR;
        } else {
            backingFgColor = new SDL_Color();
            backingFgColor.r = (byte) Argb32.getRed8(argb32);
            backingFgColor.g = (byte) Argb32.getGreen8(argb32);
            backingFgColor.b = (byte) Argb32.getBlue8(argb32);
            backingFgColor.a = (byte) Argb32.getAlpha8(argb32);
        }

        final SdlBwdFont font = (SdlBwdFont) this.getFont();

        /*
         * TODO sdl TTF_RenderText/Glyph_Shaded(...) paints
         * non-transparent backgrounds,
         * and TTF_RenderText/Glyph_Solid(...) draws terribly
         * (characters not even recognizable in non-large sizes),
         * so we stick to TTF_RenderText/Glyph_Blended(...).
         */

        final MyCtda accessor;
        if (MUST_DRAW_TEXT_GLYPH_BY_GLYPH) {
            accessor = newTextDataAccessor_RenderGlyph_Blended(
                    text,
                    maxClippedTextRectInText,
                    font,
                    backingFgColor,
                    argb32);
        } else {
            accessor = newTextDataAccessor_RenderText_Blended(
                    text,
                    maxClippedTextRectInText,
                    font,
                    backingFgColor,
                    argb32);
        }
        return accessor;
    }

    @Override
    protected void disposeClippedTextDataAccessor(
            Object clippedTextDataAccessor) {
        // Nothing to do.
    }

    @Override
    protected GRect getRenderedClippedTextRectInText(
            Object clippedTextDataAccessor) {
        final MyCtda accessor = (MyCtda) clippedTextDataAccessor;
        return accessor.rectInText;
    }

    @Override
    protected int getTextColor32(
            String text,
            Object clippedTextDataAccessor,
            int xInClippedText,
            int yInClippedText) {
        final MyCtda accessor = (MyCtda) clippedTextDataAccessor;
        final int index = yInClippedText * accessor.scanlineStride + xInClippedText;
        final int argb32 = accessor.argb32Arr[index];
        final int premulArgb32 = this.getArrayColor32FromArgb32(argb32);
        return premulArgb32;
    }

    /*
     * Images.
     */

    @Override
    protected Object getImageDataAccessor(InterfaceBwdImage image) {
        final AbstractSdlBwdImage imageImpl = (AbstractSdlBwdImage) image;
        return imageImpl.getPremulArgb32Arr();
    }

    @Override
    protected void disposeImageDataAccessor(Object imageDataAccessor) {
        // Nothing to do.
    }

    @Override
    protected int getImageColor32(
            InterfaceBwdImage image,
            Object imageDataAccessor,
            int xInImage,
            int yInImage) {
        final int[] premulArgb32Arr = (int[]) imageDataAccessor;
        final int index = yInImage * image.getWidth() + xInImage;
        return premulArgb32Arr[index];
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private SdlBwdGraphics(
        InterfaceBwdBindingImpl binding,
        GPoint rootBoxTopLeft,
        GRect box,
        GRect initialClip,
        //
        boolean isImageGraphics,
        int[] pixelArr,
        int pixelArrScanlineStride) {
        super(
            binding,
            rootBoxTopLeft,
            box,
            initialClip,
            //
            isImageGraphics,
            pixelArr,
            pixelArrScanlineStride);
    }

    /*
     * 
     */

    /**
     * Uses TTF_RenderGlyph_Blended(...).
     */
    private static MyCtda newTextDataAccessor_RenderGlyph_Blended(
            String text,
            GRect maxClippedTextRectInText,
            //
            SdlBwdFont font,
            SDL_Color backingFgColor,
            int argb32) {

        final Pointer backingFont = font.getBackingFont();

        final InterfaceBwdFontMetrics fontMetrics = font.metrics();

        final ArrayList<MyGlyphData> glyphDataList =
                new ArrayList<MyGlyphData>();

        // -1 means invalid.
        int firstGlyphXInText = -1;
        {
            // To ignore glyphs out of paintable range in X.
            // We don't bother considering paintable range in Y
            // for these ignorings.
            final int mcXInText = maxClippedTextRectInText.x();
            final int mcXMaxInText = maxClippedTextRectInText.xMax();
            int currentGlyphXInText = 0;

            int ci = 0;
            while (ci < text.length()) {
                final int cp = text.codePointAt(ci);
                if (cp <= BwdUnicode.MAX_FFFF) {
                    final int theoreticalWidth = fontMetrics.computeCharWidth(cp);
                    if (theoreticalWidth == 0) {
                        /*
                         * TODO sdl TTF_RenderGlyph_Blended(...)
                         * returns null if glyph width is zero,
                         * with the error "Text has zero width".
                         */
                    } else {
                        final char ch = (char) cp;
                        final Pointer surfPtr = LIB_TTF.TTF_RenderGlyph_Blended(backingFont, ch, backingFgColor);
                        if (surfPtr == null) {
                            throw new BindingError(
                                    "could not render code point: " + LIB.SDL_GetError()
                                    + ", code point = " + BwdUnicode.toDisplayString(cp));
                        }
                        final SDL_Surface surface = SdlJnaUtils.newAndRead(SDL_Surface.ByValue.class, surfPtr);

                        final int[] glyphArgb32Arr;
                        final int glyphWidth;
                        final int glyphHeight;
                        try {
                            glyphWidth = surface.w;
                            glyphHeight = surface.h;
                            final boolean premul = false;
                            glyphArgb32Arr = SdlUtils.surfToArgb32Arr(surface, premul);
                        } finally {
                            LIB.SDL_FreeSurface(surface);
                        }

                        final int glyphXMaxInText = currentGlyphXInText + glyphWidth - 1;

                        if (glyphXMaxInText < mcXInText) {
                            // Glyph before rect.
                        } else if (currentGlyphXInText <= mcXMaxInText) {
                            // Glyph overlaps rect.
                            glyphDataList.add(new MyGlyphData(
                                    glyphArgb32Arr,
                                    glyphWidth,
                                    glyphHeight));
                            if (firstGlyphXInText < 0) {
                                firstGlyphXInText = currentGlyphXInText;
                            }
                            if (currentGlyphXInText == mcXMaxInText) {
                                // Glyph last in rect: we are done.
                                break;
                            }
                        } else {
                            // Glyph after rect: we are done.
                            break;
                        }

                        currentGlyphXInText += glyphWidth;
                    }
                } else {
                    /*
                     * Can't display it: ignoring
                     * (We assume that matches SDL text width computation,
                     * for which we use TTF_SizeText(...)).
                     */
                }
                ci += Character.charCount(cp);
            }
        }

        final int glyphCount = glyphDataList.size();
        if (glyphCount <= 0) {
            return EMPTY_CLIPPED_TEXT_DATA_ACCESSOR;
        }
        if (firstGlyphXInText < 0) {
            throw new AssertionError();
        }

        /*
         * (width, height) corresponding to all non-ignored glyphs,
         * even if parts of them (or all of them in case of pathological
         * Y clipping) are out of visible coordinates ranges.
         */

        int totalGlyphWidth = 0;
        int totalGlyphHeight = 0;
        for (int i = 0; i < glyphCount; i++) {
            final MyGlyphData glyphData = glyphDataList.get(i);
            totalGlyphWidth += glyphData.width;
            totalGlyphHeight = Math.max(totalGlyphHeight, glyphData.height);
        }

        /*
         * 
         */

        {
            final int glyphXMaxInText = firstGlyphXInText + totalGlyphWidth - 1;
            final int glyphYMaxInText = totalGlyphHeight - 1;

            maxClippedTextRectInText = reducedClippedTextRectInText(
                    maxClippedTextRectInText,
                    glyphXMaxInText,
                    glyphYMaxInText);
        }

        if (maxClippedTextRectInText.isEmpty()) {
            return EMPTY_CLIPPED_TEXT_DATA_ACCESSOR;
        }

        /*
         * Copying all clipped parts of non-ignored glyphs,
         * into the array of pixels.
         */

        final int mcXInText = maxClippedTextRectInText.x();
        final int mcYInText = maxClippedTextRectInText.y();
        final int mcTextWidth = maxClippedTextRectInText.xSpan();
        final int mcTextHeight = maxClippedTextRectInText.ySpan();

        // For Y coordinates, "in text" and "in glyph" are the same.
        final int mcYInGlyph = mcYInText;

        final int pixelCapacity = maxClippedTextRectInText.area();
        final int[] mcArgb32Arr = new int[pixelCapacity];
        // First (ignored or not) glyph (0,0) is in (0,0) in text.
        int currentGlyphXInText = firstGlyphXInText;
        int currentXInMc = 0;
        int currentRemainingXInMc = mcTextWidth;

        for (int gi = 0; gi < glyphCount; gi++) {
            final MyGlyphData glyphData = glyphDataList.get(gi);
            final int[] glyphArgb32Arr = glyphData.argb32Arr;
            final int glyphWidth = glyphData.width;
            final int glyphHeight = glyphData.height;

            final int currentGlyphXMaxInText =
                    currentGlyphXInText + glyphWidth - 1;

            // Glyph part.
            final int partXInText = Math.max(mcXInText, currentGlyphXInText);
            final int partXSpan = Math.min(
                    glyphWidth,
                    Math.min(
                            currentGlyphXMaxInText - partXInText + 1,
                            currentRemainingXInMc));
            final int partYSpan = Math.min(mcTextHeight, glyphHeight);

            for (int j = 0; j < partYSpan; j++) {
                final int srcYInGlyph = mcYInGlyph + j;
                final int dstYInMc = j;

                for (int i = 0; i < partXSpan; i++) {
                    final int srcXInGlyph = (partXInText - currentGlyphXInText) + i;
                    final int dstXInMc = currentXInMc + i;

                    final int srcIndex = srcYInGlyph * glyphWidth + srcXInGlyph;
                    final int dstIndex = dstYInMc * mcTextWidth + dstXInMc;

                    mcArgb32Arr[dstIndex] = glyphArgb32Arr[srcIndex];
                }
            }

            currentGlyphXInText += glyphWidth;

            currentXInMc += partXSpan;
            currentRemainingXInMc -= partXSpan;
        }

        final int scanlineStride = maxClippedTextRectInText.xSpan();

        return newClippedTextDataAccessor(
                mcArgb32Arr,
                scanlineStride,
                maxClippedTextRectInText,
                argb32);
    }

    /**
     * Uses TTF_RenderText_Blended(...).
     */
    private static MyCtda newTextDataAccessor_RenderText_Blended(
            String text,
            GRect maxClippedTextRectInText,
            //
            SdlBwdFont font,
            SDL_Color backingFgColor,
            int argb32) {

        final Pointer backingFont = font.getBackingFont();

        final InterfaceBwdFontMetrics fontMetrics = font.metrics();

        /*
         * TODO sdl TTF_RenderText_Blended(...)
         * returns null if text width is zero,
         * with the error "Text has zero width".
         */
        final int theoreticalWidth = fontMetrics.computeTextWidth(text);
        if (theoreticalWidth == 0) {
            return EMPTY_CLIPPED_TEXT_DATA_ACCESSOR;
        }

        final int[] argb32Arr;

        /*
         * TODO sdl TTF_RenderText_Blended(...) stops drawing on first NUL,
         * so we get rid of them.
         */
        text = BindingTextUtils.withoutNul(text);

        final Pointer surfPtr = LIB_TTF.TTF_RenderText_Blended(backingFont, text, backingFgColor);
        if (surfPtr == null) {
            throw new BindingError(
                    "could not render text: " + LIB.SDL_GetError()
                    + ", first code point = " + BwdUnicode.toDisplayString(text.codePointAt(0))
                    + ", text = " + text);
        }
        final SDL_Surface surface = SdlJnaUtils.newAndRead(SDL_Surface.ByValue.class, surfPtr);

        final int renderedWidth;
        final int renderedHeight;
        try {
            renderedWidth = surface.w;
            renderedHeight = surface.h;
            final boolean premul = false;
            argb32Arr = SdlUtils.surfToArgb32Arr(surface, premul);
        } finally {
            LIB.SDL_FreeSurface(surface);
        }

        final int scanlineStride = renderedWidth;

        final int glyphXMaxInText = renderedWidth - 1;
        final int glyphYMaxInText = renderedHeight - 1;

        final GRect clippedTextRectInText = reducedClippedTextRectInText(
                maxClippedTextRectInText,
                glyphXMaxInText,
                glyphYMaxInText);

        return newClippedTextDataAccessor(
                argb32Arr,
                scanlineStride,
                clippedTextRectInText,
                argb32);
    }

    /**
     * To reduce max clipped text rect, taking into account:
     * - the fact that we don't use its negative coordinates
     *   (since we draw our glyphs pixels at (0+,0+) coordinates),
     * - max rendered coordinates.
     */
    private static GRect reducedClippedTextRectInText(
            GRect maxClippedTextRectInText,
            int glyphXMaxInText,
            int glyphYMaxInText) {

        final int lowXSurplus = Math.max(0, -maxClippedTextRectInText.x());
        final int lowYSurplus = Math.max(0, -maxClippedTextRectInText.y());
        final int highXSurplus = Math.max(0, maxClippedTextRectInText.xMax() - glyphXMaxInText);
        final int highYSurplus = Math.max(0, maxClippedTextRectInText.yMax() - glyphYMaxInText);

        final int totalXSurplus = lowXSurplus + highXSurplus;
        final int totalYSurplus = lowYSurplus + highYSurplus;

        final int initialXSpan = maxClippedTextRectInText.xSpan();
        final int initialYSpan = maxClippedTextRectInText.ySpan();

        final GRect ret;
        if ((totalXSurplus >= initialXSpan)
                || (totalYSurplus >= initialYSpan)) {
            // Nothing to draw.
            ret = GRect.DEFAULT_EMPTY;
        } else {
            final int dxMin = lowXSurplus;
            final int dyMin = lowYSurplus;
            final int dxMax = -highXSurplus;
            final int dyMax = -highYSurplus;

            if ((dxMin|dyMin|dxMax|dyMax) != 0) {
                ret = maxClippedTextRectInText.withBordersDeltas(
                        dxMin, dyMin, dxMax, dyMax);
            } else {
                ret = maxClippedTextRectInText;
            }
        }
        return ret;
    }

    /**
     * Does eventual color enforcing.
     */
    private static MyCtda newClippedTextDataAccessor(
            int[] argb32Arr,
            int scanlineStride,
            GRect clippedTextRectInText,
            int argb32) {
        if (MUST_ENFORCE_COLOR) {
            enforceColor(argb32Arr, argb32);
        }

        return new MyCtda(
                argb32Arr,
                scanlineStride,
                clippedTextRectInText);
    }

    /**
     * Replaces non-fully-transparent pixels with the specified ARGB 32.
     * 
     * @param argb32Arr (in,out)
     */
    private static void enforceColor(
            int[] argb32Arr,
            int argb32) {
        final int alpha8 = Argb32.getAlpha8(argb32);
        final double alphaFp = Argb32.toFpFromInt8(alpha8);
        for (int i = 0; i < argb32Arr.length; i++) {
            final int funnyArgb32 = argb32Arr[i];
            final int antiAliasedAlpha8 = Argb32.getAlpha8(funnyArgb32);
            if (antiAliasedAlpha8 != 0x00) {
                final double antiAliasedAlphaFp = Argb32.toFpFromInt8(antiAliasedAlpha8);
                final int reworkedArgb32 = Argb32.withAlphaFp(argb32, Math.sqrt(alphaFp * antiAliasedAlphaFp));
                argb32Arr[i] = reworkedArgb32;
            }
        }
    }
}
