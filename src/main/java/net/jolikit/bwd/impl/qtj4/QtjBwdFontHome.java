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
package net.jolikit.bwd.impl.qtj4;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.trolltech.qt.gui.QFont;
import com.trolltech.qt.gui.QFontDatabase;
import com.trolltech.qt.gui.QFontDatabase.WritingSystem;
import com.trolltech.qt.gui.QFontMetrics;

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.BwdFontStyles;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
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

public class QtjBwdFontHome extends AbstractBwdFontHome<QtjCompleteBackingFont,QtjBwdFontHome.MyBfg> {
    
    /*
     * TODO qtj As said in Qt docs (http://doc.qt.io/qt-4.8/qrawfont.html):
     * 
     * "The QRawFont class provides access to a single physical instance of
     * a font.
     * Note: QRawFont is a low level class. For most purposes QFont is a more
     * appropriate class.
     * Most commonly, when presenting text in a user interface, the exact fonts
     * used to render the characters is to some extent unknown.
     * This can be the case for several reasons: For instance, the actual,
     * physical fonts present on the target system could be unexpected to the
     * developers, or the text could contain user selected styles, sizes or
     * writing systems that are not supported by font chosen in the code.
     * Therefore, Qt's QFont class really represents a query for fonts.
     * When text is interpreted, Qt will do its best to match the text to the
     * query, but depending on the support, different fonts can be used behind
     * the scenes."
     * 
     * QtJambi doesn't map QRawFont to Java, so we have to deal with QFont
     * and its obscure "best" effort behavior.
     * 
     * Also, QFontDatabase.addApplicationFont(fontFilePath) returns an int "id",
     * from which we can obtain a list of families using
     * QFontDatabase.applicationFontFamilies(id).
     */
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    static class MyBfg {
        final QFont backingFont;
        public MyBfg(QFont backingFont) {
            this.backingFont = backingFont;
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyQtjCfdc implements InterfaceCanFontDisplayComputer {
        public MyQtjCfdc() {
        }
        @Override
        public boolean canFontDisplay(
                InterfaceBwdFont font,
                int codePoint) {
            /*
             * TODO qtj No way to tell exactly. Doing best effort.
             */
            final QtjBwdFont fontImpl = (QtjBwdFont) font;
            final QtjCompleteBackingFont myBackingFont = fontImpl.getBackingFont();
            final QFontMetrics backingMetrics = myBackingFont.backingMetrics();
            if (false) {
                /*
                 * QFontMetrics.inFontUcs4:
                 * "Returns true if the character given by ch, encoded in UCS-4/UTF-32,
                 * is a valid character in the font; otherwise returns false."
                 * 
                 * From "https://en.wikipedia.org/wiki/UTF-32":
                 * "The original ISO 10646 standard defines a 32-bit encoding form called UCS-4,
                 * in which each encoded character in the Universal Character Set (UCS) is
                 * represented by a 31-bit value between 0 and 0x7FFFFFFF (the sign bit was
                 * unused and zero).
                 * In November 2003, Unicode was restricted by RFC 3629 to match the constraints
                 * of the UTF-16 character encoding: explicitly prohibiting code points greater
                 * than U+10FFFF (and also the high and low surrogates U+D800 through U+DFFF)."
                 * 
                 * TODO qtj We don't use it for quick return,
                 * for it can actually be very slow (in addition to
                 * char width computation slowness), for example on Mac.
                 */
                backingMetrics.inFontUcs4(codePoint);
            }
            final int width = fontImpl.fontMetrics().computeCharWidth(codePoint);
            return (width > 0);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int INVALID_ID = -1;
    
    private static final MyQtjCfdc MY_QTJ_CAN_FONT_DISPLAY_COMPUTER = new MyQtjCfdc();
    
    /*
     * 
     */

    private final BaseBwdBindingConfig bindingConfig;

    private final WritingSystem writingSystem;

    /**
     * TODO qtj Why do we need an instance? What state does it store?
     */
    private final QFontDatabase fontDatabase = new QFontDatabase();
    
    /**
     * Guarded by synchronization on itself.
     * 
     * ids returned by QFontDatabase.addApplicationFont(String).
     */
    private final Set<Integer> loadIdSet = new HashSet<Integer>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public QtjBwdFontHome(
            BaseBwdBindingConfig bindingConfig,
            WritingSystem writingSystem) {
        this.bindingConfig = LangUtils.requireNonNull(bindingConfig);
        this.writingSystem = LangUtils.requireNonNull(writingSystem);
        
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
        
        final Integer[] loadedIdArr;
        synchronized (this.loadIdSet) {
            loadedIdArr = this.loadIdSet.toArray(new Integer[this.loadIdSet.size()]);
            this.loadIdSet.clear();
        }
        
        for (Integer loadedId : loadedIdArr) {
            QFontDatabase.removeApplicationFont(loadedId);
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected Map<BwdFontKind,MyLoadedFontData> loadSystemFonts() {
        
        final Map<BwdFontKind,MyLoadedFontData> lfdByFontKind =
                new HashMap<BwdFontKind,MyLoadedFontData>();
        
        final QFont[] backingFontArr = this.computeSystemFonts_homeMutexLocked();
        
        for (QFont backingFont : backingFontArr) {
            // Metrics not used.
            final QtjCompleteBackingFont myBackingFont = new QtjCompleteBackingFont(backingFont, null);
            final BwdFontKind fontKind = this.computeFontKindFromBackingFont(myBackingFont);
            
            if (!lfdByFontKind.containsKey(fontKind)) {
                final MyBfg bfg = new MyBfg(backingFont);
                final MyLoadedFontData lfd = new MyLoadedFontData(
                        MY_QTJ_CAN_FONT_DISPLAY_COMPUTER,
                        bfg);
                lfdByFontKind.put(fontKind, lfd);
            } else {
                // Just disposes the QFont.
                this.disposeBackingFont(myBackingFont);
            }
        }
        
        return lfdByFontKind;
    }
    
    @Override
    protected Map<BwdFontKind,MyLoadedFontData> loadFontsAtPath(String fontFilePath) {
        
        final Map<BwdFontKind,MyLoadedFontData> lfdByFontKind =
                new HashMap<BwdFontKind,MyLoadedFontData>();

        final List<QFont> backingFontList = this.loadBackingFontsAtPath(fontFilePath);
        final int backingFontCount = backingFontList.size();
        if (backingFontCount == 0) {
            // Qt can't handle this font.
            return lfdByFontKind;
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
                 * TODO qtj If helper doesn't compute the same amount of fonts,
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
                        final CodePointSet cps = helper.computeCodePointSetElseNull(fontIndex);
                        if (cps != null) {
                            final InterfaceCanFontDisplayComputer cfdc = new CodePointSetCfdc(cps);
                            cfdcList.add(cfdc);
                        } else {
                            // Fallback.
                            cfdcList.add(MY_QTJ_CAN_FONT_DISPLAY_COMPUTER);
                        }
                    }
                }
            } finally {
                helper.close();
            }
        }
        
        /*
         * 
         */
        
        final List<MyBfg> bfgList = new ArrayList<MyBfg>();

        for (int fontIndex = 0; fontIndex < backingFontCount; fontIndex++) {
            final QFont backingFont = backingFontList.get(fontIndex);

            if (!mustUseFontBoxForFontKind) {
                // To get in same state than FontBox not managing to compute it.
                fontKindList.add(null);
            }
            
            if (fontKindList.get(fontIndex) == null) {
                // Metrics not used.
                final QtjCompleteBackingFont myBackingFont = new QtjCompleteBackingFont(
                        backingFont,
                        null);
                BwdFontKind fontKind = this.computeFontKindFromBackingFont(myBackingFont);

                /*
                 * Eventually ensuring proper style.
                 * 
                 * TODO qtj (same as for AWT) All loaded fonts are "plain"
                 * by default (until QFont.setBold(boolean) etc. are called).
                 * So, we do best effort, and look for style-indicating words
                 * in file name (family name seems to never contain style information,
                 * which is why we can't do this hack for system fonts).
                 */
                final File file = new File(fontFilePath);
                final String fileName = file.getName();
                final boolean forcedBold = BindingTextUtils.containsBoldInfo(fileName);
                final boolean forcedItalic = BindingTextUtils.containsItalicInfo(fileName);
                final int forcedStyle = BwdFontStyles.of(forcedBold, forcedItalic);
                fontKind = fontKind.withStyle(forcedStyle);
                
                fontKindList.set(fontIndex, fontKind);
            }
            
            if (!mustUseFontBoxForCanDisplay) {
                cfdcList.add(MY_QTJ_CAN_FONT_DISPLAY_COMPUTER);
            }
            
            final MyBfg bfg = new MyBfg(backingFont);
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
    protected AbstractBwdFont<QtjCompleteBackingFont> createBackingFontAndFont(
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
         * 
         */
        
        final QFont backingFont;
        if (false) {
            // Should work as well.
            final String family = fontKind.fontFamily();
            backingFont = new QFont(family);
        } else {
            backingFont = bfg.backingFont.clone();
        }
        backingFont.setBold(fontKind.isBold());
        backingFont.setItalic(fontKind.isItalic());
        
        final int backingSizeInPixelsInt = BindingCoordsUtils.roundToInt(this.computeBackingFontSizeInPixelsFp(fontSize));
        backingFont.setPixelSize(backingSizeInPixelsInt);
        
        final QFontMetrics backingMetrics = new QFontMetrics(backingFont);
        
        final QtjCompleteBackingFont myBackingFont = new QtjCompleteBackingFont(
                backingFont,
                backingMetrics);

        if (disposeCallListener != null) {
            disposeCallListener.incrementRefCount_atCreation();
        }

        return this.createFontReusingBackingFont(
                this.homeId(),
                fontId,
                lfd.getCanFontDisplayComputer(),
                disposeCallListener,
                myBackingFont);
    }

    @Override
    protected AbstractBwdFont<QtjCompleteBackingFont> createFontReusingBackingFont(
            int homeId,
            BwdFontId fontId,
            InterfaceCanFontDisplayComputer canFontDisplayComputer,
            InterfaceFontDisposeCallListener disposeCallListener,
            QtjCompleteBackingFont myBackingFont) {
        return new QtjBwdFont(
                homeId,
                fontId,
                canFontDisplayComputer,
                disposeCallListener,
                myBackingFont);
    }

    @Override
    protected void disposeBackingFont(QtjCompleteBackingFont myBackingFont) {
        myBackingFont.dispose();
    }

    @Override
    protected void disposeBfg(MyBfg bfg) {
        bfg.backingFont.dispose();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * NB: Always normal style for just-loaded backing fonts.
     */
    private BwdFontKind computeFontKindFromBackingFont(QtjCompleteBackingFont myBackingFont) {
        
        final QFont backingFont = myBackingFont.backingFont();
        
        final String backingFamily = backingFont.family();
        final boolean bold = backingFont.bold();
        final boolean italic = backingFont.italic();
        
        return new BwdFontKind(backingFamily, bold, italic);
    }
    
    /*
     * 
     */

    private QFont[] computeSystemFonts_homeMutexLocked() {
        final List<String> families = this.fontDatabase.families(this.writingSystem);

        final ArrayList<QFont> backingFontList = new ArrayList<QFont>();

        for (String family : families) {
            final List<String> styles = this.fontDatabase.styles(family);
            for (String style : styles) {
                final QFont backingFont = new QFont(family);
                backingFont.setStyleName(style);
                backingFont.setPixelSize(1);
                
                backingFontList.add(backingFont);
            }
        }

        return backingFontList.toArray(new QFont[backingFontList.size()]);
    }

    /**
     * @return An empty list if could not load.
     */
    private List<QFont> loadBackingFontsAtPath(String fontFilePath) {

        final List<QFont> loadedBackingFontList = new ArrayList<QFont>();

        final int loadId = QFontDatabase.addApplicationFont(fontFilePath);
        if (loadId != INVALID_ID) {
            final boolean didAdd;
            synchronized (this.loadIdSet) {
                didAdd = this.loadIdSet.add(loadId);
            }
            if (!didAdd) {
                throw new BindingError("load id still in use: " + loadId);
            }

            final List<String> familyList =
                    QFontDatabase.applicationFontFamilies(loadId);

            for (String family : familyList) {
                final QFont backingFont = new QFont(family);
                // Both setting initial size, and making sure
                // that pixel size is used, not point size.
                backingFont.setPixelSize(1);

                loadedBackingFontList.add(backingFont);
            }
        }

        return loadedBackingFontList;
    }
}
