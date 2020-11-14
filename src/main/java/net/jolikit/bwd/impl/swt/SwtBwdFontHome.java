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
package net.jolikit.bwd.impl.swt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.basics.InterfaceDefaultFontInfoComputer;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFont;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontHome;
import net.jolikit.bwd.impl.utils.fonts.CodePointSet;
import net.jolikit.bwd.impl.utils.fonts.CodePointSetCfdc;
import net.jolikit.bwd.impl.utils.fonts.FontBoxHelper;
import net.jolikit.bwd.impl.utils.fonts.InterfaceCanFontDisplayComputer;
import net.jolikit.bwd.impl.utils.fonts.InterfaceFontDisposeCallListener;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

public class SwtBwdFontHome extends AbstractBwdFontHome<Font,SwtBwdFontHome.MyBfg> {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * Ignoring font families which start with an at sign ('@'),
     * because font home API says so, and for consistency with FontBox
     * which doesn't "create" them.
     */
    private static final boolean MUST_IGNORE_AT_SIGN_FAMILIES = true;
    
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
    
    private static class MySwtCfdc implements InterfaceCanFontDisplayComputer {
        public MySwtCfdc() {
        }
        @Override
        public boolean canFontDisplay(
                InterfaceBwdFont font,
                int codePoint) {
            /*
             * TODO swt No way to tell at all,
             * so in practice should use FontBox instead
             * when possible.
             */
            return false;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * TODO swt org.eclipse.swt.graphics.GC.getCharWidth(...) takes a char
     * as argument,
     * so we can assume SWT fonts can never display characters
     * outside of BMP.
     */
    private static final int MAX_DISPLAYABLE_CODE_POINT = BwdUnicode.MAX_FFFF;

    private static final MySwtCfdc MY_SWT_CAN_FONT_DISPLAY_COMPUTER = new MySwtCfdc();
    
    /*
     * 
     */

    private final BaseBwdBindingConfig bindingConfig;

    private final Display display;
    
    /**
     * TODO swt Has any effect? The locale is not preserved in the FontData,
     * after doing "new Font(display, fontData).getFontData()".
     */
    private final String locale;
    
    private final SwtFontMetricsHelper metricsHelper;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param locale Locale given to FontData.setLocale(String) method.
     *        Can be null or an empty string, which causes a default charset
     *        to be used.
     */
    public SwtBwdFontHome(
            BaseBwdBindingConfig bindingConfig,
            Display display,
            String locale) {
        this.bindingConfig = LangUtils.requireNonNull(bindingConfig);
        this.display = LangUtils.requireNonNull(display);
        this.locale = locale;
        
        this.metricsHelper = new SwtFontMetricsHelper(display);
        
        if (false) {
            /*
             * TODO swt Computes pointsPerOsPixel = 1
             * whatever the resolution (at least on my retina Mac),
             * so it doesn't seem to work, but we don't care
             * since we use font size factor instead anyway,
             * and also stick to the "1 point = 1.3333 pixel"
             * convention.
             */
            final Point dpiXY = display.getDPI();
            // Using DPI along Oy axis, and we will use it
            // event if graphics is rotated.
            final double pixelsPerInch = dpiXY.y;
            final int pointsPerInch = 72;
            final double pointsPerOsPixel =  (pointsPerInch / pixelsPerInch);
            Dbg.log("pointsPerOsPixel = " + pointsPerOsPixel);
        }

        this.initialize_final(
                this.bindingConfig.getMinRawFontSize(),
                this.bindingConfig.getMaxRawFontSize(),
                this.bindingConfig.getFontSizeFactor());
    }

    /*
     * 
     */
    
    @Override
    public void dispose() {
        super.dispose();
        
        this.metricsHelper.dispose();
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected Map<BwdFontKind,MyLoadedFontData> loadSystemFonts() {

        final Map<BwdFontKind,MyLoadedFontData> lfdByFontKind =
                new HashMap<BwdFontKind,MyLoadedFontData>();
        
        /*
         * TODO swt System fonts seem loaded by default,
         * so we just pick which fonts are loaded at this time,
         * compute their kind and populate the map.
         */
        
        final Map<BwdFontKind,MyBfg> bfgByLoadedFontKind =
                this.computeBfgByLoadedFontKind_homeMutexLocked();
        
        for (Map.Entry<BwdFontKind,MyBfg> entry : bfgByLoadedFontKind.entrySet()) {
            final BwdFontKind fontKind = entry.getKey();
            final MyBfg bfg = entry.getValue();
            
            if (!lfdByFontKind.containsKey(fontKind)) {
                final MyLoadedFontData lfd = new MyLoadedFontData(
                        MY_SWT_CAN_FONT_DISPLAY_COMPUTER,
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
         * TODO swt To figure out which font kinds get loaded,
         * we have no better way than to compare the set of fonts before
         * and after the load.
         * When these sets are identical, we have no way to know if it's
         * due to re-loading an already loaded font, or a different font
         * but that has the same kind (for example from a different file
         * format).
         * 
         * TODO swt On Mac, loadFont(...) returns true,
         * but the fonts don't appear among loaded fonts,
         * i.e. user fonts loading silently doesn't work.
         */
        
        final Map<BwdFontKind,MyBfg> oldBfgByLoadedFontKind = this.computeBfgByLoadedFontKind_homeMutexLocked();
        // Will eventually re-load, erasing previously loaded font of same kind.
        final boolean didLoad = this.display.loadFont(fontFilePath);
        if (!didLoad) {
            // SWT can't handle this font.
            if (DEBUG) {
                Dbg.log("could not load any font (1) from: " + fontFilePath);
            }
            return lfdByFontKind;
        }
        final Map<BwdFontKind,MyBfg> newBfgByLoadedFontKind = this.computeBfgByLoadedFontKind_homeMutexLocked();
        final Map<BwdFontKind,MyBfg> bfgByNewlyLoadedFontKind = computeAMinusB(
                newBfgByLoadedFontKind,
                oldBfgByLoadedFontKind);
        
        final int backingFontCount = bfgByNewlyLoadedFontKind.size();
        if (backingFontCount == 0) {
            /*
             * TODO swt Can happen normally,
             * when the font had already been loaded.
             * Since we can't know what the font kind is,
             * We just pretend we could not load it.
             */
            if (DEBUG) {
                Dbg.log("could not load any font (2) from: " + fontFilePath);
            }
            return lfdByFontKind;
        }
        
        /*
         * 
         */
        
        boolean mustUseFontBoxForFontKind = this.bindingConfig.getMustUseFontBoxForFontKind();
        boolean mustUseFontBoxForCanDisplay = this.bindingConfig.getMustUseFontBoxForCanDisplay();

        if (DEBUG) {
            Dbg.log("mustUseFontBoxForFontKind = " + mustUseFontBoxForFontKind);
            Dbg.log("mustUseFontBoxForCanDisplay = " + mustUseFontBoxForCanDisplay);
        }

        final List<BwdFontKind> fontKindList = new ArrayList<BwdFontKind>();
        final List<InterfaceCanFontDisplayComputer> cfdcList = new ArrayList<InterfaceCanFontDisplayComputer>();
        if (mustUseFontBoxForFontKind
                || mustUseFontBoxForCanDisplay) {
            final FontBoxHelper helper = new FontBoxHelper(fontFilePath);
            try {
                /*
                 * TODO swt If FontBox doesn't compute the same amount of fonts,
                 * we just don't use it, as we don't know which font
                 * computed data is related to.
                 */
                final int helperFontCount = helper.getFontCount();
                if (DEBUG) {
                    Dbg.log("fontFilePath = " + fontFilePath);
                    Dbg.log("backingFontCount = " + backingFontCount);
                    Dbg.log("helperFontCount = " + helperFontCount);
                }
                if (helperFontCount != backingFontCount) {
                    mustUseFontBoxForFontKind = false;
                    mustUseFontBoxForCanDisplay = false;
                }
                if (helperFontCount > 1) {
                    // We wouldn't know which BFG goes with which font kind,
                    // since SWT doesn't provide loaded fonts indices.
                    mustUseFontBoxForFontKind = false;
                }
                if ((!mustUseFontBoxForFontKind)
                        && (helperFontCount > 1)) {
                    // We wouldn't know which CFDC goes with which font kind,
                    // since SWT doesn't provide loaded fonts indices.
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
                            cfdcList.add(MY_SWT_CAN_FONT_DISPLAY_COMPUTER);
                        }
                    }
                }
            } finally {
                helper.close();
            }
        }
        
        final List<MyBfg> bfgList = new ArrayList<MyBfg>();
        
        if (mustUseFontBoxForFontKind) {
            if (backingFontCount > 1) {
                // We must have guards against that above.
                throw new AssertionError();
            }
        } else {
            if (mustUseFontBoxForCanDisplay) {
                if (backingFontCount > 1) {
                    // We must have guards against that above.
                    throw new AssertionError();
                }
            }
        }
        // No order needed.
        for (Map.Entry<BwdFontKind,MyBfg> entry : bfgByNewlyLoadedFontKind.entrySet()) {
            final BwdFontKind fontKind = entry.getKey();
            final MyBfg bfg = entry.getValue();
            
            if (mustUseFontBoxForFontKind) {
                if (fontKindList.get(0) == null) {
                    fontKindList.set(0, fontKind);
                }
            } else {
                fontKindList.add(fontKind);
            }
            bfgList.add(bfg);
        }

        if (!mustUseFontBoxForCanDisplay) {
            for (int fontIndex = 0; fontIndex < backingFontCount; fontIndex++) {
                cfdcList.add(MY_SWT_CAN_FONT_DISPLAY_COMPUTER);
            }
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
        
        if (DEBUG) {
            Dbg.log("fontFilePath = " + fontFilePath);
            Dbg.log("lfdByFontKind = " + lfdByFontKind);
        }
        
        return lfdByFontKind;
    }
    
    @Override
    protected List<String> getBonusSystemFontFilePathList() {
        return this.bindingConfig.getBonusSystemFontFilePathList();
    }

    @Override
    protected InterfaceDefaultFontInfoComputer getDefaultFontInfoComputer() {
        if (false) {
            /*
             * TODO swt Looks like some kind of default font.
             * Tahoma font on my platform.
             */
            final Font systemFont = this.display.getSystemFont();
        }
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
        
        /*
         * 
         */
        
        // Not sure whether it can return null.
        final FontData fontData;
        {
            /*
             * In specs: "the height of the desired font in points".
             */
            final int backingSizeInPointsInt =
                    BindingCoordsUtils.roundToInt(
                            this.computeBackingFontSizeInPointsFp(fontSize));
            final int height = backingSizeInPointsInt;
            final int style = computeFontBackingStyle(fontKind.isBold(), fontKind.isItalic());
            
            fontData = new FontData(
                    bfg.backingFamily,
                    height,
                    style);
            fontData.setLocale(this.locale);
        }
        
        /*
         * TODO swt "On most platforms, a single FontData suffices
         * to create the selected font.
         * However, the X Window System can require multiple FontData objects
         * to create a font."
         * We have no clue how to decide that we need multiple FontData,
         * so we just always use one.
         */
        
        final Font backingFont = new Font(this.display, fontData);
        
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
        return new SwtBwdFont(
                homeId,
                fontId,
                canFontDisplayComputer,
                disposeCallListener,
                backingFont,
                //
                this.metricsHelper);
    }

    @Override
    protected void disposeBackingFont(Font backingFont) {
        backingFont.dispose();
    }

    @Override
    protected void disposeBfg(MyBfg bfg) {
        // Nothing to do.
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static Map<BwdFontKind,MyBfg> computeAMinusB(
            Map<BwdFontKind,MyBfg> a,
            Map<BwdFontKind,MyBfg> b) {
        final Map<BwdFontKind,MyBfg> result = new HashMap<BwdFontKind,MyBfg>();
        for (Map.Entry<BwdFontKind,MyBfg> entry : a.entrySet()) {
            final BwdFontKind fontKind = entry.getKey();
            final MyBfg bfg = entry.getValue();
            
            if (!b.containsKey(fontKind)) {
                result.put(fontKind, bfg);
            }
        }
        return result;
    }

    private BwdFontKind computeFontKindFromData(FontData fontData) {
        final String backingFamily = fontData.getName();
        final int style = fontData.getStyle();
        final boolean bold = (style & SWT.BOLD) != 0;
        final boolean italic = (style & SWT.ITALIC) != 0;
        
        return new BwdFontKind(backingFamily, bold, italic);
    }

    private static int computeFontBackingStyle(boolean bold, boolean italic) {
        return (bold ? SWT.BOLD : SWT.NORMAL) | (italic ? SWT.ITALIC : SWT.NORMAL);
    }

    private static boolean mustIgnoreFontKind(BwdFontKind fontKind) {
        return MUST_IGNORE_AT_SIGN_FAMILIES
                && fontKind.family().startsWith("@");
    }
    
    private Map<BwdFontKind,MyBfg> computeBfgByLoadedFontKind_homeMutexLocked() {
        /*
         * TODO swt SWT is quite unintuitive/hacky here:
         * must use null faceName to get all fonts.
         */
        final String faceName = null;
        
        final Map<BwdFontKind,MyBfg> bfgByLoadedFontKind = new HashMap<BwdFontKind,MyBfg>();
        for (boolean scalable : new boolean[]{false,true}) {
            final FontData[] fontDataArr = this.display.getFontList(faceName, scalable);
            for (FontData fontData : fontDataArr) {
                final BwdFontKind fontKind = computeFontKindFromData(fontData);
                if (!mustIgnoreFontKind(fontKind)) {
                    final String backingFamily = fontData.getName();
                    final MyBfg bfg = new MyBfg(backingFamily);
                    bfgByLoadedFontKind.put(fontKind, bfg);
                }
            }
        }

        return bfgByLoadedFontKind;
    }
}
