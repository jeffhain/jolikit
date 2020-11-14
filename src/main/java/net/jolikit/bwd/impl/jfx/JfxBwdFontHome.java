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
package net.jolikit.bwd.impl.jfx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.BwdFontStyles;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
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

public class JfxBwdFontHome extends AbstractBwdFontHome<Font,JfxBwdFontHome.MyBfg> {
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    static class MyBfg {
        final String backingFamily;
        public MyBfg(String backingFamily) {
            this.backingFamily = backingFamily;
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyJfxCfdc implements InterfaceCanFontDisplayComputer {
        public MyJfxCfdc() {
        }
        @Override
        public boolean canFontDisplay(
                InterfaceBwdFont font,
                int codePoint) {
            /*
             * TODO jfx No way to tell exactly. Doing best effort.
             */
            final JfxBwdFont fontImpl = (JfxBwdFont) font;
            final int width = fontImpl.metrics().computeCharWidth(codePoint);
            return (width > 0);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Not too small, for its metrics to give non-zero width
     * for its normal glyphs, and also to hopefully make sure that
     * the actually requested font is returned and not a default one.
     */
    private static final int DUMMY_FONT_SIZE = 10;

    private static final MyJfxCfdc MY_JFX_CAN_FONT_DISPLAY_COMPUTER = new MyJfxCfdc();
    
    private final JfxBwdBindingConfig bindingConfig;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @throws ExceptionInInitializerError if called before JavaFX
     *         initialization (due to screen DPI retrieval).
     */
    public JfxBwdFontHome(JfxBwdBindingConfig bindingConfig) {
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
        
        final Map<BwdFontKind,MyLoadedFontData> lfdByFontKind =
                new HashMap<BwdFontKind,MyLoadedFontData>();
        
        final Font[] backingFontArr = this.computeSystemFonts_homeMutexLocked();

        for (Font backingFont : backingFontArr) {
            final BwdFontKind fontKind = this.computeFontKindFromBackingFont(backingFont);
            
            if (!lfdByFontKind.containsKey(fontKind)) {
                final MyBfg bfg = new MyBfg(backingFont.getFamily());
                final MyLoadedFontData lfd = new MyLoadedFontData(
                        MY_JFX_CAN_FONT_DISPLAY_COMPUTER,
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

        /*
         * TODO jfx Terrible behavior. According to the Javadoc:
         * "If the application does not have the proper permission then this method
         *  will return the default system font with the specified font size."
         * But, thank God, actually the code seems to return null
         * in case of permission issue, else we would have no clean way
         * to tell whether the loading failed.
         */
        Font backingFont = null;
        try {
            backingFont = Font.loadFont("file:" + fontFilePath, DUMMY_FONT_SIZE);
        } catch (NullPointerException e) {
            /*
             * TODO jfx JavaFX can throw NPE (or else?)
             * when it can't handle the format:
java.lang.NullPointerException
    at com.sun.javafx.font.PrismFontFactory.createFontResource(PrismFontFactory.java:333)
    at com.sun.javafx.font.PrismFontFactory.loadEmbeddedFont(PrismFontFactory.java:1607)
    at com.sun.javafx.font.PrismFontFactory.loadEmbeddedFont(PrismFontFactory.java:1547)
    at com.sun.javafx.font.PrismFontLoader.loadFont(PrismFontLoader.java:99)
    at javafx.scene.text.Font.loadFont(Font.java:400)
             */
        }
        if (backingFont == null) {
            // JavaFX can't handle this font.
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

        final MyBfg bfg = new MyBfg(backingFont.getFamily());

        if (fontKind == null) {
            fontKind = this.computeFontKindFromBackingFont(backingFont);
        }
        
        // Don't need this backing font anymore.
        this.disposeBackingFont(backingFont);
        
        if (cfdc == null) {
            cfdc = MY_JFX_CAN_FONT_DISPLAY_COMPUTER;
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
    
    @Override
    protected AbstractBwdFont<Font> createBackingFontAndFont(
            BwdFontId fontId,
            MyDisposableFontDisposeCallListener disposeCallListener) {
        
        final int fontSize = fontId.size();
        
        final BwdFontKind fontKind = fontId.kind();

        final MyLoadedFontData lfd = this.getLfdForLoadedFontKind(fontKind);
        if (lfd == null) {
            throw new IllegalArgumentException("no font loaded for kind " + fontKind);
        }
        final MyBfg bfg = lfd.getBackingFontGenerator();
        final String backingFamily = bfg.backingFamily;
        
        /*
         * 
         */

        final FontWeight weight = computeWeight(fontKind.isBold());
        final FontPosture posture = computePosture(fontKind.isItalic());
        
        /*
         * TODO jfx Seems JavaFX wants size in pixels, not in points as it claims:
         * "size The point size of the font."
         */
        final double backingSizeInPixelsFp = this.computeBackingFontSizeInPixelsFp(fontSize);
        // Not sure whether it can return null.
        final Font backingFont = Font.font(backingFamily, weight, posture, backingSizeInPixelsFp);
        if (backingFont == null) {
            throw new BindingError("could not find a font of kind " + fontKind);
        }
        
        if (!this.bindingConfig.getMustUseFontBoxForFontKind()) {
            // Re-checking the kind, because JavaFX does a "best effort" search,
            // which means if might return pretty much whatever (theoretically). 
            final BwdFontKind actualFontKind = this.computeFontKindFromBackingFont(backingFont);
            if (!actualFontKind.equals(fontKind)) {
                // Must not happen since we check kind possibilities
                // on font load. Maybe someone did unload it in some way.
                throw new BindingError(
                        "searched for a font of kind "
                                + fontKind
                                + ", but could only find one of kind "
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
    protected AbstractBwdFont<Font> createFontReusingBackingFont(
            int homeId,
            BwdFontId fontId,
            InterfaceCanFontDisplayComputer canFontDisplayComputer,
            InterfaceFontDisposeCallListener disposeCallListener,
            Font backingFont) {
        return new JfxBwdFont(
                homeId,
                fontId,
                canFontDisplayComputer,
                disposeCallListener,
                backingFont,
                // Allowing dynamic change for that,
                // if user ever wants to compare at runtime.
                this.bindingConfig.getMustUseInternalApiForFontMetrics());
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
    
    private static FontPosture computePosture(boolean italic) {
        return italic ? FontPosture.ITALIC : FontPosture.REGULAR;
    }

    private static FontWeight computeWeight(boolean bold) {
        // If "BOLD" is not enough to obtain a bold font,
        // doesn't seem useful to try higher weights,
        // so we just work with these two values.
        return bold ? FontWeight.BOLD : FontWeight.NORMAL;
    }

    private boolean computeIsBackingFontStyleBold(String backingStyle) {
        /*
         * Cf. FontWeight values.
         */
        return BindingStringUtils.containsIgnoreCase(backingStyle, "Bold")
                || BindingStringUtils.containsIgnoreCase(backingStyle, "Black");
    }

    private boolean computeIsBackingFontStyleItalic(String backingStyle) {
        return BindingStringUtils.containsIgnoreCase(backingStyle, "Italic")
                || BindingStringUtils.containsIgnoreCase(backingStyle, "Oblique");
    }

    private BwdFontKind computeFontKindFromBackingFont(Font backingFont) {
        
        final String backingFamily = backingFont.getFamily();
        final String backingStyle = backingFont.getStyle();
        final boolean bold = computeIsBackingFontStyleBold(backingStyle);
        final boolean italic = computeIsBackingFontStyleItalic(backingStyle);
        
        return new BwdFontKind(backingFamily, bold, italic);
    }
    
    private BwdFontId computeFontIdFromBackingFont(Font backingFont) {
        return new BwdFontId(
                this.computeFontKindFromBackingFont(backingFont),
                BindingCoordsUtils.roundToInt(backingFont.getSize()));
    }
    
    /*
     * 
     */
    
    private Font[] computeSystemFonts_homeMutexLocked() {
        final List<String> backingFamilyList = Font.getFamilies();

        final ArrayList<Font> backingFontList = new ArrayList<Font>();

        // To take care not to load fonts of same kind (since Font.font(...)
        // is best effort and might return kind of whatever).
        // NB: Could also use Font.getName(), since it contains
        // family name and the style, but using font kind is cleaner.
        final Set<BwdFontKind> tmpKindSet = new HashSet<BwdFontKind>();
        
        /*
         * TODO jfx On Mac, this can happen:
         * - One of the families returned by Font.getFamilies() is "Apple SD Gothic Neo".
         * - Then the font returned by Font.font("Apple SD Gothic Neo", NORMAL, REGULAR, 10.0)
         *   has the following attributes:
         *   getFamily() = ".Apple SD Gothic NeoI"
         *   getName() = "Apple SD GothicNeo ExtraBold"
         *   getStyle() = Bold
         *   getSize() = 10.0
         *   Note the messy family name.
         *   This looks like some kind of weird best effort font
         *   (ExtraBold, when we asked NORMAL weight...).
         * - Then, when later we call Font.font(...) with the family
         *   ".Apple SD Gothic NeoI", we get a font of "System" family.
         * This made the safety check we have on font creation blow up.
         * To avoid this kind of issue, we added, as we should have already,
         * a safety check for Font.font(...) results here, to ignore fonts
         * that have different family, weight, posture or size than requested.
         */

        final boolean[] falseTrueArr = new boolean[]{false,true};
        for (String backingFamily : backingFamilyList) {
            for (boolean italic : falseTrueArr) {
                final FontPosture posture = computePosture(italic);
                for (boolean bold : falseTrueArr) {
                    final FontWeight weight = computeWeight(bold);
                    final Font backingFont = Font.font(backingFamily, weight, posture, DUMMY_FONT_SIZE);
                    if (backingFont != null) {
                        final BwdFontId expectedFontId = new BwdFontId(
                                backingFamily,
                                BwdFontStyles.of(bold, italic),
                                DUMMY_FONT_SIZE);
                        final BwdFontId actualFontId = this.computeFontIdFromBackingFont(backingFont);
                        
                        if (actualFontId.equals(expectedFontId)) {
                            final BwdFontKind fontKind = actualFontId.kind();
                            if (tmpKindSet.add(fontKind)) {
                                backingFontList.add(backingFont);
                            }
                        } else {
                            // Some kind of fallback font.
                        }
                    }
                }
            }
        }

        return backingFontList.toArray(new Font[backingFontList.size()]);
    }
}
