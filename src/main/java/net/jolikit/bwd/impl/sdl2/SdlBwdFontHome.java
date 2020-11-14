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
package net.jolikit.bwd.impl.sdl2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLibTtf;
import net.jolikit.bwd.impl.sdl2.jlib.SdlTtfStyle;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.BindingStringUtils;
import net.jolikit.bwd.impl.utils.basics.InterfaceDefaultFontInfoComputer;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFont;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontHome;
import net.jolikit.bwd.impl.utils.fonts.CodePointSet;
import net.jolikit.bwd.impl.utils.fonts.CodePointSetCfdc;
import net.jolikit.bwd.impl.utils.fonts.FontBoxHelper;
import net.jolikit.bwd.impl.utils.fonts.InterfaceCanFontDisplayComputer;
import net.jolikit.bwd.impl.utils.fonts.InterfaceFontDisposeCallListener;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;

public class SdlBwdFontHome extends AbstractBwdFontHome<Pointer,SdlBwdFontHome.MyBfg> {
    
    /*
     * TODO sdl In SDL, there is no notion of "loaded font" from which fonts
     * of different sizes can be "derived" without reload from a font file,
     * i.e. for each new size we need to reload from the file.
     * 
     * NB: "Note: In many places, SDL_ttf will say "glyph" when it means "code point."
     * Unicode is hard, we learn as we go, and we apologize for adding to the confusion."
     */

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    static class MyBfg {
        final String fontFilePath;
        final int fontIndex;
        public MyBfg(
                String fontFilePath,
                int fontIndex) {
            this.fontFilePath = LangUtils.requireNonNull(fontFilePath);
            if (fontIndex < 0) {
                throw new IllegalArgumentException("" + fontIndex);
            }
            this.fontIndex = fontIndex;
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MySdlCfdc implements InterfaceCanFontDisplayComputer {
        public MySdlCfdc() {
        }
        @Override
        public boolean canFontDisplay(
                InterfaceBwdFont font,
                int codePoint) {
            /*
             * TODO sdl No way to tell exactly. Doing best effort.
             */
            final boolean result;
            if (BwdUnicode.isInBmp(codePoint)) {
                final SdlBwdFont fontImpl = (SdlBwdFont) font;
                final char c = (char) codePoint;
                final int index = LIB_TTF.TTF_GlyphIsProvided(fontImpl.getBackingFont(), c);
                // Yep, index 0 means it's not provided.
                result = (index > 0);
            } else {
                result = false;
            }
            return result;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;
    private static final SdlJnaLibTtf LIB_TTF = SdlJnaLibTtf.INSTANCE;
    
    /**
     * TODO sdl TTF_RenderGlyph_XXX(...) and TTF_GlyphIsProvided(...)
     * methods take an Uint16 as code point,
     * so we can assume SDL fonts can never display characters
     * outside of BMP.
     */
    private static final int MAX_DISPLAYABLE_CODE_POINT = BwdUnicode.MAX_FFFF;
    
    /**
     * Not too small, for its metrics to give non-zero width
     * for its normal glyphs.
     */
    private static final int DUMMY_FONT_SIZE = 10;

    private static final MySdlCfdc MY_SDL_CAN_FONT_DISPLAY_COMPUTER = new MySdlCfdc();
    
    /*
     * 
     */

    private final BaseBwdBindingConfig bindingConfig;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public SdlBwdFontHome(BaseBwdBindingConfig bindingConfig) {
        this.bindingConfig = LangUtils.requireNonNull(bindingConfig);
        
        if (false) {
            /*
             * Not using all that, we prefer to stick to the simplicity
             * and configurability of getFontSizeFactor().
             */
            // dot = pixel.
            final int displayIndex = 0;
            final FloatByReference ddpi = new FloatByReference();
            final FloatByReference hdpi = new FloatByReference();
            final FloatByReference vdpi = new FloatByReference();
            /*
             * TODO sdl This method returns dpi values that seem rounded
             * to the closest integer or half integer (113.5, 149, etc.),
             * and correspond to an actual diagonal of ~13.296215 inches
             * for my supposedly 13.3 inches screen.
             * Since the difference is so small, we give SDL
             * the benefit of the doubt and just trust it.
             */
            final int ret = LIB.SDL_GetDisplayDPI(displayIndex, ddpi, hdpi, vdpi);
            if (ret != 0) {
                throw new BindingError("SDL_GetDisplayDPI : " + LIB.SDL_GetError());
            }
            // Using DPI along Oy axis, and we will use it
            // event if graphics is rotated.
            final double dotsPerInch = vdpi.getValue();
        }
        
        this.initialize_final(
                this.bindingConfig.getMinRawFontSize(),
                this.bindingConfig.getMaxRawFontSize(),
                this.bindingConfig.getFontSizeFactor());
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected Map<BwdFontKind,MyLoadedFontData> loadSystemFonts() {
        /*
         * No system font to load.
         */
        return new HashMap<BwdFontKind,MyLoadedFontData>();
    }
    
    @Override
    protected Map<BwdFontKind,MyLoadedFontData> loadFontsAtPath(String fontFilePath) {
        
        final Map<BwdFontKind,MyLoadedFontData> lfdByFontKind =
                new HashMap<BwdFontKind,MyLoadedFontData>();
        
        final List<Pointer> backingFontList = new ArrayList<Pointer>();
        
        // At least 1. Will compute the actual value from first face loaded.
        int backingFontCount = 1;
        for (int fontIndex = 0; fontIndex < backingFontCount; fontIndex++) {
            final Pointer backingFont = handy_TTF_OpenFontIndex(fontFilePath, DUMMY_FONT_SIZE, fontIndex);
            if (backingFont == null) {
                // SDL can't handle this font.
                // Cleanup.
                for (Pointer bf : backingFontList) {
                    this.disposeBackingFont(bf);
                }
                return lfdByFontKind;
            }
            if (false) {
                /*
                 * TODO sdl TTF_SetFontStyle(...) might have no effect,
                 * and SDL (actually the backing FreeType library maybe)
                 * seems to properly recognize freemonobold.ttf as bold, etc,
                 * so we just take what load gets us, taking care to
                 * load all the faces available in the file,
                 * and don't bother to make styled derivations.
                 */
                final int backingStyle = SdlTtfStyle.TTF_STYLE_NORMAL.intValue();
                LIB_TTF.TTF_SetFontStyle(backingFont, backingStyle);
            }
            backingFontList.add(backingFont);
            if (fontIndex == 0) {
                backingFontCount = NumbersUtils.asInt(LIB_TTF.TTF_FontFaces(backingFont));
            }
        }

        /*
         * 
         */
        
        boolean mustUseFontBoxForFontKind = this.bindingConfig.getMustUseFontBoxForFontKind();
        boolean mustUseFontBoxForCanDisplay = this.bindingConfig.getMustUseFontBoxForCanDisplay();

        final List<BwdFontKind> fontKindList = new ArrayList<BwdFontKind>();
        final List<InterfaceCanFontDisplayComputer> cfdcList = new ArrayList<InterfaceCanFontDisplayComputer>();
        if (mustUseFontBoxForFontKind
                || mustUseFontBoxForCanDisplay) {
            final FontBoxHelper helper = new FontBoxHelper(fontFilePath);
            try {
                /*
                 * TODO sdl If helper doesn't compute the same amount of fonts,
                 * we just don't use it, as we don't know which font
                 * computed data is related to.
                 */
                final int helperFontCount = helper.getFontCount();
                if (helperFontCount != backingFontCount) {
                    mustUseFontBoxForFontKind = false;
                    mustUseFontBoxForCanDisplay = false;
                }
                for (int fontIndex = 0; fontIndex < helperFontCount; fontIndex++) {
                    if (mustUseFontBoxForFontKind) {
                        final BwdFontKind fontKind = helper.computeFontKindElseNull(fontIndex);
                        // If null, will use fallback later.
                        fontKindList.add(fontKind);
                    }
                    if (mustUseFontBoxForCanDisplay) {
                        final CodePointSet cps = helper.computeCodePointSetElseNull(
                                fontIndex,
                                MAX_DISPLAYABLE_CODE_POINT);
                        if (cps.getRangeCount() != 0) {
                            final InterfaceCanFontDisplayComputer cfdc = new CodePointSetCfdc(cps);
                            cfdcList.add(cfdc);
                        } else {
                            // Fallback.
                            cfdcList.add(MY_SDL_CAN_FONT_DISPLAY_COMPUTER);
                        }
                    }
                }
            } finally {
                helper.close();
            }
        }
        
        boolean foundNullFontKind = false;
        for (BwdFontKind fontKind : fontKindList) {
            if (fontKind == null) {
                foundNullFontKind = true;
                break;
            }
        }
        
        if ((!mustUseFontBoxForFontKind) || foundNullFontKind) {
            for (int fontIndex = 0; fontIndex < backingFontCount; fontIndex++) {
                if (!mustUseFontBoxForFontKind) {
                    // To get in same state than FontBox not managing to compute it.
                    fontKindList.add(null);
                }
                
                if (fontKindList.get(fontIndex) == null) {
                    final Pointer backingFont = backingFontList.get(fontIndex);
                    final BwdFontKind fontKind = this.computeFontKindFromBackingFont(backingFont);
                    fontKindList.set(fontIndex, fontKind);
                }
            }
        }
        
        // Don't need these fonts anymore.
        for (Pointer backingFont : backingFontList) {
            this.disposeBackingFont(backingFont);
        }

        if (!mustUseFontBoxForCanDisplay) {
            for (int fontIndex = 0; fontIndex < backingFontCount; fontIndex++) {
                cfdcList.add(MY_SDL_CAN_FONT_DISPLAY_COMPUTER);
            }
        }
        
        final List<MyBfg> bfgList = new ArrayList<MyBfg>();
        for (int fontIndex = 0; fontIndex < backingFontCount; fontIndex++) {
            final MyBfg bfg = new MyBfg(fontFilePath, fontIndex);
            bfgList.add(bfg);
        }
        
        /*
         * 
         */
        
        for (int fontIndex = 0; fontIndex < backingFontCount; fontIndex++) {
            final BwdFontKind fontKind = fontKindList.get(fontIndex);
            final InterfaceCanFontDisplayComputer cfdc = cfdcList.get(fontIndex);
            final MyBfg bfg = bfgList.get(fontIndex);
            
            final MyLoadedFontData lfd = new MyLoadedFontData(cfdc, bfg);
            lfdByFontKind.put(fontKind, lfd);
        }
        
        return lfdByFontKind;
    }
    
    @Override
    protected List<String> getBonusSystemFontFilePathList() {
        return this.bindingConfig.getBonusSystemFontFilePathList();
    }

    @Override
    protected InterfaceDefaultFontInfoComputer getDefaultFontInfoComputer() {
        return this.bindingConfig.getDefaultFontInfoComputer();
    }
    
    @Override
    protected AbstractBwdFont<Pointer> createBackingFontAndFont(
            BwdFontId fontId,
            MyDisposableFontDisposeCallListener disposeCallListener) {
        
        final int fontSize = fontId.size();
        
        final BwdFontKind fontKind = fontId.kind();

        final MyLoadedFontData lfd = this.getLfdForLoadedFontKind(fontKind);
        if (lfd == null) {
            throw new IllegalArgumentException("no font loaded for font kind " + fontKind);
        }
        final MyBfg bfg = lfd.getBackingFontGenerator();
        final String fontFilePath = bfg.fontFilePath;
        final int fontIndex = bfg.fontIndex;
        
        /*
         * 
         */
        
        /*
         * TODO sdl It seems that SDL wants the size in pixels,
         * not in points: "int ptsize".
         */
        final int backingSizeInPixelsInt = BindingCoordsUtils.roundToInt(this.computeBackingFontSizeInPixelsFp(fontSize));
        final Pointer backingFont = handy_TTF_OpenFontIndex(fontFilePath, backingSizeInPixelsInt, fontIndex);
        if (backingFont == null) {
            throw new BindingError("could not load already loaded font at " + fontFilePath);
        }

        /*
         * 
         */
        
        if (disposeCallListener != null) {
            disposeCallListener.incrementRefCount_atCreation();
        }

        return this.createFontReusingBackingFont(
                this.homeId(),
                fontId,
                lfd.getCanFontDisplayComputer(),
                disposeCallListener,
                backingFont);
    }

    @Override
    protected AbstractBwdFont<Pointer> createFontReusingBackingFont(
            int homeId,
            BwdFontId fontId,
            InterfaceCanFontDisplayComputer canFontDisplayComputer,
            InterfaceFontDisposeCallListener disposeCallListener,
            Pointer backingFont) {
        return new SdlBwdFont(
                homeId,
                fontId,
                canFontDisplayComputer,
                disposeCallListener,
                backingFont);
    }

    @Override
    protected void disposeBackingFont(Pointer backingFont) {
        /*
         * TODO sdl We have a memory leak with SDL fonts,
         * as if this call had no effect.
         */
        LIB_TTF.TTF_CloseFont(backingFont);
    }

    @Override
    protected void disposeBfg(MyBfg bfg) {
        // Nothing to do.
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BwdFontKind computeFontKindFromBackingFont(Pointer backingFont) {
        
        final String backingFamily = LIB_TTF.TTF_FontFaceFamilyName(backingFont);
        
        final int backingStyle = LIB_TTF.TTF_GetFontStyle(backingFont);
        final boolean bold = (backingStyle & SdlTtfStyle.TTF_STYLE_BOLD.intValue()) != 0;
        final boolean italic = (backingStyle & SdlTtfStyle.TTF_STYLE_ITALIC.intValue()) != 0;
        
        if (false) {
            /*
             * TODO sdl We should maybe ignore these fonts,
             * since we (deliberately) don't have corresponding BWD styles.
             */
            final boolean underline = (backingStyle & SdlTtfStyle.TTF_STYLE_UNDERLINE.intValue()) != 0;
            final boolean strikethrough = (backingStyle & SdlTtfStyle.TTF_STYLE_STRIKETHROUGH.intValue()) != 0;
        }
        
        return new BwdFontKind(backingFamily, bold, italic);
    }

    /*
     * 
     */
    
    /**
     * @return Null if could not load.
     */
    private static Pointer handy_TTF_OpenFontIndex(
            String fontFilePath,
            int ptsize,
            long faceIndex) {
        final Pointer backingFont;
        if (BindingStringUtils.endsWithIgnoreCase(fontFilePath, ".bdf")) {
            /*
             * TODO sdl These do load, but what is drawn is messy.
             */
            backingFont = null;
        } else {
            backingFont = LIB_TTF.TTF_OpenFontIndex(fontFilePath, ptsize, faceIndex);
        }
        return backingFont;
    }
}
