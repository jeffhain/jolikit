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
package net.jolikit.bwd.impl.algr5;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_FONT;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaUtils;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLibFont;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLibTtf;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.basics.BindingStringUtils;
import net.jolikit.bwd.impl.utils.basics.InterfaceDefaultFontInfoComputer;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFont;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontHome;
import net.jolikit.bwd.impl.utils.fonts.BindingTextUtils;
import net.jolikit.bwd.impl.utils.fonts.CodePointSet;
import net.jolikit.bwd.impl.utils.fonts.CodePointSetCfdc;
import net.jolikit.bwd.impl.utils.fonts.FontBoxHelper;
import net.jolikit.bwd.impl.utils.fonts.InterfaceCanFontDisplayComputer;
import net.jolikit.bwd.impl.utils.fonts.InterfaceFontDisposeCallListener;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;

import com.sun.jna.Pointer;

public class AlgrBwdFontHome extends AbstractBwdFontHome<ALLEGRO_FONT,AlgrBwdFontHome.MyBfg> {
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    static class MyBfg {
        final String fontFilePath;
        public MyBfg(String fontFilePath) {
            this.fontFilePath = LangUtils.requireNonNull(fontFilePath);
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Must use one instance per font kind,
     * so that ranges don't get recomputed for each font.
     */
    private static class MyCfdc implements InterfaceCanFontDisplayComputer {
        private CodePointSet displayableCodePointSet;
        public MyCfdc() {
        }
        void init(ALLEGRO_FONT backingFont) {
            this.displayableCodePointSet =
                    computeDisplayableCodePointSetWithAllegro(
                            backingFont);
        }
        @Override
        public boolean canFontDisplay(
                InterfaceBwdFont font,
                int codePoint) {
            return this.displayableCodePointSet.contains(codePoint);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final AlgrJnaLibFont LIB_FONT = AlgrJnaLibFont.INSTANCE;
    
    /**
     * Not too small, for its metrics to give non-zero width
     * for its normal glyphs.
     */
    private static final int DUMMY_FONT_SIZE = 10;
    
    /*
     * 
     */
    
    private final BaseBwdBindingConfig bindingConfig;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AlgrBwdFontHome(BaseBwdBindingConfig bindingConfig) {
        this.bindingConfig = LangUtils.requireNonNull(bindingConfig);
        
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
        
        final ALLEGRO_FONT backingFont = handy_al_load_font(fontFilePath, DUMMY_FONT_SIZE);
        if (backingFont == null) {
            // Allegro can't handle this font.
            return lfdByFontKind;
        }
        
        /*
         * 
         */

        final boolean mustUseFontBoxForFontKind = this.bindingConfig.getMustUseFontBoxForFontKind();
        final boolean mustUseFontBoxForCanDisplay = this.bindingConfig.getMustUseFontBoxForCanDisplay();
        
        BwdFontKind fontKind = null;
        InterfaceCanFontDisplayComputer cfdc = null;
        if (mustUseFontBoxForFontKind
                || mustUseFontBoxForCanDisplay) {
            final FontBoxHelper helper = new FontBoxHelper(fontFilePath);
            try {
                final int fontIndex = 0;
                if (mustUseFontBoxForFontKind) {
                    fontKind = helper.computeFontKindElseNull(fontIndex);
                }
                if (mustUseFontBoxForCanDisplay) {
                    final CodePointSet cps = helper.computeCodePointSetElseNull(fontIndex);
                    if (cps != null) {
                        cfdc = new CodePointSetCfdc(cps);
                    }
                }
            } finally {
                helper.close();
            }
        }
        
        /*
         * 
         */
        
        final MyBfg bfg = new MyBfg(fontFilePath);
        
        if (fontKind == null) {
            fontKind = this.computeFontKindFromBfg(bfg);
        }
        
        if (cfdc == null) {
            final MyCfdc cfdcImpl = new MyCfdc();
            cfdcImpl.init(backingFont);
            
            cfdc = cfdcImpl;
        }
        
        // Don't need that font anymore.
        this.disposeBackingFont(backingFont);
        
        /*
         * 
         */
        
        final MyLoadedFontData lfd = new MyLoadedFontData(cfdc, bfg);
        lfdByFontKind.put(fontKind, lfd);
        
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
    protected AbstractBwdFont<ALLEGRO_FONT> createBackingFontAndFont(
            BwdFontId fontId,
            MyDisposableFontDisposeCallListener disposeCallListener) {
        
        final int fontSize = fontId.size();
        
        final BwdFontKind fontKind = fontId.kind();

        final MyLoadedFontData lfd = this.getLfdForLoadedFontKind(fontKind);
        if (lfd == null) {
            throw new IllegalArgumentException("no font loaded for kind " + fontKind);
        }
        final MyBfg bfg = lfd.getBackingFontGenerator();
        final String fontFilePath = bfg.fontFilePath;
        
        /*
         * 
         */
        
        final ALLEGRO_FONT backingFont = handy_al_load_font(
                fontFilePath,
                fontSize);
        if (backingFont == null) {
            /*
             * Not supposed to happen for loaded font kinds.
             * Had it happen with some ".afm" font.
             */
            throw new BindingError("could not load font at " + fontFilePath);
        }
        
        if (!this.bindingConfig.getMustUseFontBoxForFontKind()) {
            final BwdFontKind actualFontKind = this.computeFontKindFromBfg(bfg);
            if (!actualFontKind.equals(fontKind)) {
                throw new BindingError(
                        "tried to create a font of kind "
                                + fontKind
                                + ", but could only create one of kind "
                                + actualFontKind);
            }
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
    protected AbstractBwdFont<ALLEGRO_FONT> createFontReusingBackingFont(
            int homeId,
            BwdFontId fontId,
            InterfaceCanFontDisplayComputer canFontDisplayComputer,
            InterfaceFontDisposeCallListener disposeCallListener,
            ALLEGRO_FONT backingFont) {
        return new AlgrBwdFont(
                homeId,
                fontId,
                canFontDisplayComputer,
                disposeCallListener,
                backingFont);
    }

    @Override
    protected void disposeBackingFont(ALLEGRO_FONT backingFont) {
        LIB_FONT.al_destroy_font(backingFont);
    }

    @Override
    protected void disposeBfg(MyBfg bfg) {
        // Nothing to do.
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BwdFontKind computeFontKindFromBfg(MyBfg backingFontGenerator) {
        
        final File file = new File(backingFontGenerator.fontFilePath);
        final String fileName = file.getName();
        
        final int dotIndex = fileName.indexOf('.');
        // TODO algr Best effort.
        final String backingFamily;
        if (dotIndex >= 0) {
            backingFamily = fileName.substring(0, dotIndex);
        } else {
            backingFamily = fileName;
        }
        
        // TODO algr Best effort.
        final boolean bold = BindingTextUtils.containsBoldInfo(fileName);
        final boolean italic = BindingTextUtils.containsItalicInfo(fileName);

        return new BwdFontKind(backingFamily, bold, italic);
    }

    /*
     * 
     */
    
    /**
     * @param size Size in pixels.
     * @return Null if could not load.
     */
    private ALLEGRO_FONT handy_al_load_font(String fontFilePath, int fontSize) {
        /*
         * The size parameter determines the size the font will be rendered at, specified in pixel.
         * The standard font size is measured in units per EM, if you instead want to specify
         * the size as the total height of glyphs in pixel, pass it as a negative value.
         * 
         * Note: If you want to display text at multiple sizes, load the font multiple times
         * with different size parameters.
         * The following flags are supported:
         * 
         * ALLEGRO_TTF_NO_KERNING - Do not use any kerning even if the font file supports it.
         * ALLEGRO_TTF_MONOCHROME - Load as a monochrome font (Which means no anti-aliasing of the font is done)
         */
        final int backingSizeInPixelsInt = BindingCoordsUtils.roundToInt(this.computeBackingFontSizeInPixelsFp(fontSize));
        // Flags for most simplicity.
        final int flags = AlgrJnaLibTtf.ALLEGRO_TTF_NO_KERNING | AlgrJnaLibTtf.ALLEGRO_TTF_MONOCHROME;
        Pointer backingFontPtr = null;
        if (BindingStringUtils.endsWithIgnoreCase(fontFilePath, ".bdf")) {
            // TODO algr These do load, but what is drawn is messy
            // (maybe due to not using the proper encoding?).
            backingFontPtr = null;
        } else {
            // TODO algr What if multiple faces in the file?
            backingFontPtr = LIB_FONT.al_load_font(fontFilePath, backingSizeInPixelsInt, flags);
        }
        if (backingFontPtr == null) {
            return null;
        } else {
            return AlgrJnaUtils.newAndRead(ALLEGRO_FONT.class, backingFontPtr);
        }
    }
    
    private static CodePointSet computeDisplayableCodePointSetWithAllegro(ALLEGRO_FONT backingFont) {
        final int actual_range_count;
        {
            final int max_ranges_count = 0;
            final int[] ranges = new int[2 * max_ranges_count];
            actual_range_count = LIB_FONT.al_get_font_ranges(
                    backingFont,
                    max_ranges_count,
                    ranges);
        }

        final int capacity = NumbersUtils.timesExact(2, actual_range_count);
        final int[] ranges = new int[capacity];
        LIB_FONT.al_get_font_ranges(
                backingFont,
                actual_range_count,
                ranges);
        
        final CodePointSet codePointSet = new CodePointSet(ranges);
        return codePointSet;
    }
}
