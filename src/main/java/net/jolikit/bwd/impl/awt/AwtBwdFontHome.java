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
package net.jolikit.bwd.impl.awt;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
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
import net.jolikit.lang.RethrowException;

public class AwtBwdFontHome extends AbstractBwdFontHome<Font,AwtBwdFontHome.MyBfg> {

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    static class MyBfg {
        final Font backingFont;
        public MyBfg(Font backingFont) {
            this.backingFont = backingFont;
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyAwtCfdc implements InterfaceCanFontDisplayComputer {
        public MyAwtCfdc() {
        }
        @Override
        public boolean canFontDisplay(
                InterfaceBwdFont font,
                int codePoint) {
            final AwtBwdFont fontImpl = (AwtBwdFont) font;
            return fontImpl.getBackingFont().canDisplay(codePoint);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final MyAwtCfdc MY_AWT_CAN_FONT_DISPLAY_COMPUTER = new MyAwtCfdc();
    
    private final BaseBwdBindingConfig bindingConfig;

    private final Locale locale;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param locale Locale to use for font family names.
     */
    public AwtBwdFontHome(
            BaseBwdBindingConfig bindingConfig,
            Locale locale) {
        this.bindingConfig = LangUtils.requireNonNull(bindingConfig);
        this.locale = LangUtils.requireNonNull(locale);
        
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

        final Map<BwdFontKind,MyLoadedFontData> lfdByFontKind =
                new HashMap<BwdFontKind,MyLoadedFontData>();
        
        final Font[] backingFontArr = computeSystemFonts_homeMutexLocked();

        for (Font backingFont : backingFontArr) {
            final BwdFontKind fontKind = this.computeFontKindFromBackingFont(backingFont);
            
            if (!lfdByFontKind.containsKey(fontKind)) {
                final MyBfg bfg = new MyBfg(backingFont);
                final MyLoadedFontData lfd = new MyLoadedFontData(
                        MY_AWT_CAN_FONT_DISPLAY_COMPUTER,
                        bfg);
                lfdByFontKind.put(fontKind, lfd);
            }
        }
        
        return lfdByFontKind;
    }
    
    @Override
    protected Map<BwdFontKind,MyLoadedFontData> loadFontsAtPath(String fontFilePath) {

        final Map<BwdFontKind,MyLoadedFontData> lfdByFontKind =
                new HashMap<BwdFontKind,MyLoadedFontData>();

        final int backingFormat = computeBackingFontFormat(fontFilePath);
        if (backingFormat == -1) {
            return lfdByFontKind;
        }
        Font backingFont = null;
        try {
            /*
             * NB: Never calling GraphicsEnvironment.registerFont(...)
             * on these fonts, so that they don't appear in
             * GraphicsEnvironment.getAllFonts() that we use
             * to get system fonts.
             */
            backingFont = Font.createFont(backingFormat, new File(fontFilePath));
        } catch (FontFormatException e) {
            // Quiet.
        } catch (IOException e) {
            // Passing here if file could not be read.
            throw new RethrowException(e);
        }
        if (backingFont == null) {
            // AWT can't handle this font.
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

        final MyBfg bfg = new MyBfg(backingFont);
        
        if (fontKind == null) {
            fontKind = this.computeFontKindFromBackingFont(backingFont);
        }

        if (cfdc == null) {
            cfdc = MY_AWT_CAN_FONT_DISPLAY_COMPUTER;
        }
        
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

    /**
     * @param fontId Must not be null.
     * @param disposeCallListener Can be null. If not null, ref count must be 0.
     */
    @Override
    protected AbstractBwdFont<Font> createBackingFontAndFont(
            BwdFontId fontId,
            MyDisposableFontDisposeCallListener disposeCallListener) {
        
        final int fontSize = fontId.fontSize();
        
        final BwdFontKind fontKind = fontId.fontKind();

        final MyLoadedFontData lfd = this.getLfdForLoadedFontKind(fontKind);
        if (lfd == null) {
            throw new IllegalArgumentException("no font loaded for kind " + fontKind);
        }
        final MyBfg bfg = lfd.getBackingFontGenerator();

        /*
         * TODO awt Font.deriveFont(float) seem to actually use a size in pixels,
         * not in points as some JDK variables names indicate (Font constructor
         * using arg "float sizePts", set with the value we give here).
         */
        final float backingSizeInPixelsFp = (float) this.computeBackingFontSizeInPixelsFp(fontSize);
        final Font backingFont = bfg.backingFont.deriveFont(backingSizeInPixelsFp);

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
    protected AwtBwdFont createFontReusingBackingFont(
            int homeId,
            BwdFontId fontId,
            InterfaceCanFontDisplayComputer canFontDisplayComputer,
            InterfaceFontDisposeCallListener disposeCallListener,
            Font backingFont) {
        return new AwtBwdFont(
                homeId,
                fontId,
                canFontDisplayComputer,
                disposeCallListener,
                backingFont);
    }

    @Override
    protected void disposeBackingFont(Font backingFont) {
        // Nothing to do.
    }

    @Override
    protected void disposeBfg(MyBfg bfg) {
        // Nothing to do.
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @return Backing font format, or -1 if could not figure it out.
     */
    private static int computeBackingFontFormat(String fontUrl) {
        
        if (BindingStringUtils.endsWithIgnoreCase(fontUrl,".ttf")
                || BindingStringUtils.endsWithIgnoreCase(fontUrl,".ttc")) {
            return Font.TRUETYPE_FONT;
        }
        
        if (BindingStringUtils.endsWithIgnoreCase(fontUrl,".pfb") // Win/OS2
                || BindingStringUtils.endsWithIgnoreCase(fontUrl,".pfm") // Win/OS2/MacOS
                || BindingStringUtils.endsWithIgnoreCase(fontUrl,".afm") // MacOS/Linux
                || BindingStringUtils.endsWithIgnoreCase(fontUrl,".pfa") // Linux
                || BindingStringUtils.endsWithIgnoreCase(fontUrl,".ofm")) { // OS2
            return Font.TYPE1_FONT;
        }
        
        if (BindingStringUtils.endsWithIgnoreCase(fontUrl,".otf")
                || BindingStringUtils.endsWithIgnoreCase(fontUrl,".otc")) { // OS2
            /*
             * TODO awt Should work with Java 7+:
             * http://bugs.java.com/view_bug.do?bug_id=6954424:
             * "OpenType is the name given to the evolution of the TrueType font specification"
             * so our best effort is to consider it as a TrueType font.
             */
            return Font.TRUETYPE_FONT;
        }
        
        return -1;
    }
    
    private BwdFontKind computeFontKindFromBackingFont(Font backingFont) {
        
        final String backingFamily = backingFont.getFamily(this.locale);
        
        /*
         * TODO awt All loaded fonts are "plain" by default,
         * only derived fonts can have style for AWT.
         * So, we do best effort, and look for style-indicating words
         * in family name.
         */
        
        final boolean bold;
        final boolean italic;
        if (false) {
            bold = backingFont.isBold();
            italic = backingFont.isItalic();
        } else {
            final String fontNameEn = backingFont.getFontName(Locale.ENGLISH);
            
            // Useful on Mac (ex.: name = "AlBayan", family = "Al Bayan").
            String fontNameEnNorm = fontNameEn.replaceAll(" ", "");
            String backingFamilyNorm = backingFamily.replaceAll(" ", "");
            
            final String strForStyleSearch;
            if (fontNameEnNorm.startsWith(backingFamilyNorm)) {
                /*
                 * On Windows, it seems we always pass here.
                 */
                strForStyleSearch = fontNameEnNorm.substring(backingFamilyNorm.length());
            } else {
                /*
                 * Can happen, ex.:
                 * fontNameEn = HiraKakuPro-W3
                 * backingFamily = Hiragino Kaku Gothic Pro
                 */
                strForStyleSearch = fontNameEn;
            }
            bold = BindingTextUtils.containsBoldInfo(strForStyleSearch);
            italic = BindingTextUtils.containsItalicInfo(strForStyleSearch);
        }

        return new BwdFontKind(backingFamily, bold, italic);
    }

    /*
     * 
     */

    private static Font[] computeSystemFonts_homeMutexLocked() {
        final GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        /*
         * We need to use getAllFonts(), not just getAvailableFontFamilyNames(Locale),
         * because we also want styled variants (which contains the style in their name,
         * not in their actual style which is always plain).
         */
        return gEnv.getAllFonts();
    }
}
