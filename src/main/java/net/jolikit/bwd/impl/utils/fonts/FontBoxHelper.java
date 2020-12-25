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
package net.jolikit.bwd.impl.utils.fonts;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.utils.basics.BindingStringUtils;
import net.jolikit.bwd.impl.utils.fontbox.AFMParser;
import net.jolikit.bwd.impl.utils.fontbox.CharMetric;
import net.jolikit.bwd.impl.utils.fontbox.CmapSubtable;
import net.jolikit.bwd.impl.utils.fontbox.CmapTable;
import net.jolikit.bwd.impl.utils.fontbox.FontMetrics;
import net.jolikit.bwd.impl.utils.fontbox.HeaderTable;
import net.jolikit.bwd.impl.utils.fontbox.HorizontalMetricsTable;
import net.jolikit.bwd.impl.utils.fontbox.TTFParser;
import net.jolikit.bwd.impl.utils.fontbox.TrueTypeCollection;
import net.jolikit.bwd.impl.utils.fontbox.TrueTypeCollection.TrueTypeFontProcessor;
import net.jolikit.bwd.impl.utils.fontbox.TrueTypeFont;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.lang.RethrowException;
import net.jolikit.time.TimeUtils;

/**
 * The API through which we use Apache FontBox in our bindings.
 * 
 * We don't use FontBox for font rendering (would be much work, we prefer
 * to rely on backing UI libraries for that), but we use it for:
 * - Computing font family consistently across bindings, for different
 *   UI libraries usually compute different names from a same font file
 *   (ex.: "WenQuanYi Micro Hei" with Qt, "wqy-microhei" with Allegro5.).
 *   It's not required, but it can make cross-binding (test) code much easier.
 * - Computing style (bold/italic), because some UI libraries don't provide
 *   this information (such as Allegro5, or AWT for which all loaded fonts
 *   are "normal", style coming from font derivation), and we prefer that than
 *   relying on best effort computations such as from file name or family name.
 * - Computing code points for "canDisplay(...)" method, because some UI
 *   libraries don't provide this information, and when they do they don't
 *   always provide the same.
 */
public class FontBoxHelper implements Closeable {

    /*
     * About font files formats:
     * https://nwalsh.com/comp.fonts/FAQ/cf_15.htm (1996)
     * http://www.acutesystems.com/fonts.htm
     * http://www.microsoft.com/typography/specs/default.htm
     * http://www.microsoft.com/typography/otspec/otff.htm
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Much faster than using a HashSet when values are in BMP,
     * especially if there are many.
     */
    private static class MyCpSet {
        /**
         * This quick set is only sized to handle code points in BMP,
         * to avoid the array being too big (slow) in most common cases.
         * Any code point outside of BMP is stored in the hash set.
         */
        final boolean[] bmpCpSet = new boolean[BwdUnicode.MAX_FFFF + 1];
        final HashSet<Integer> outOfBmpCpSet = new HashSet<Integer>();
        public MyCpSet() {
        }
        public void clear() {
            // Falseizing.
            for (int i = 0; i < this.bmpCpSet.length; i++) {
                this.bmpCpSet[i] = false;
            }
            this.outOfBmpCpSet.clear();
        }
        public void add(int cp) {
            if (cp < this.bmpCpSet.length) {
                this.bmpCpSet[cp] = true;
            } else {
                this.outOfBmpCpSet.add(cp);
            }
        }
        public int[] toSortedCpArr() {
            int cpCountInBmp = 0;
            for (boolean p : this.bmpCpSet) {
                if (p) {
                    cpCountInBmp++;
                }
            }
            final int cpCount = cpCountInBmp + this.outOfBmpCpSet.size();

            final int[] cpArr = new int[cpCount];
            int cpi = 0;
            for (int cp = 0; cp < this.bmpCpSet.length; cp++) {
                if (this.bmpCpSet[cp]) {
                    cpArr[cpi++] = cp;
                }
            }
            for (Integer ref : this.outOfBmpCpSet) {
                cpArr[cpi++] = ref.intValue();
            }
            
            if (cpCount > cpCountInBmp) {
                // Part in BMP is already sorted.
                Arrays.sort(cpArr, cpCountInBmp, cpArr.length);
            }
            final int[] sortedCpArr = cpArr;
            return sortedCpArr;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final String fontFilePath;

    /*
     * 
     */

    private TrueTypeFont[] ttfArr;

    /**
     * For .afm format.
     */
    private List<CharMetric> cmList;

    private Closeable closeable;

    /*
     * temps
     */
    
    /**
     * Saves a bit of gc for fonts collections,
     * and helper is not supposed to be anything thread-safe.
     */
    private MyCpSet tmpCpSet = null;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public FontBoxHelper(String fontFilePath) {
        this.fontFilePath = LangUtils.requireNonNull(fontFilePath);
    }

    /**
     * @return The font count, or 0 if could not be computed,
     *         for example due to unsupported format.
     */
    public int getFontCount() {
        this.initIfNeeded();
        if (this.ttfArr != null) {
            return this.ttfArr.length;
        } else if (this.cmList != null) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * @return The font kind, or null if could not be computed,
     *         for example due to unsupported format.
     */
    public BwdFontKind computeFontKindElseNull(int fontIndex) {
        this.initIfNeeded();
        try {
            if (this.ttfArr != null) {
                return computeFontKind_ttf(this.ttfArr[fontIndex]);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RethrowException(e);
        }
    }

    /**
     * Equivalent to computeCodePointSetElseNull(_, Integer.MAX_VALUE).
     */
    public CodePointSet computeCodePointSetElseNull(int fontIndex) {
        final int maxDisplayableCodePoint = Integer.MAX_VALUE;
        return this.computeCodePointSetElseNull(fontIndex, maxDisplayableCodePoint);
    }
    
    /**
     * @param fontIndex Index of the font, in [0,getFontCount()[.
     * @param maxDisplayableCodePoint Code point value above which code points
     *        are not considered as displayable. Must be >= 0.
     *        Useful for libraries that only support a limited Unicode range,
     *        typically [0,0xFF] or [0,0xFFFF] (BMP).
     * @return The set of surely displayable code points that could be computed,
     *         or null if no displayable code point could be computed,
     *         for example due to unsupported format.
     */
    public CodePointSet computeCodePointSetElseNull(
            int fontIndex,
            int maxDisplayableCodePoint) {
        NbrsUtils.requireSupOrEq(0, maxDisplayableCodePoint, "maxDisplayableCodePoint");
        this.initIfNeeded();
        try {
            final CodePointSet cps;
            if (this.ttfArr != null) {
                cps = computeCodePointSet_ttf(
                        this.ttfArr[fontIndex],
                        maxDisplayableCodePoint);
            } else if (this.cmList != null) {
                cps = computeCodePointSet_afm(
                        fontIndex,
                        maxDisplayableCodePoint);
            } else {
                cps = null;
            }
            if ((cps != null) && (cps.getCodePointCount() == 0)) {
                // Backing library might do better.
                return null;
            } else {
                return cps;
            }
        } catch (IOException e) {
            throw new RethrowException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (this.closeable != null) {
                this.closeable.close();
                this.closeable = null;
            }
        } catch (IOException e) {
            // quiet
        }
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    static int[] toIntArr(Set<Integer> set) {
        final int[] intArr = new int[set.size()];
        int i = 0;
        for (Integer ref : set) {
            intArr[i++] = ref.intValue();
        }
        return intArr;
    }

    static int computeRangeCount(int[] sortedCpArr) {
        int rangeCount = 0;
        if (sortedCpArr.length != 0) {
            rangeCount++;

            int prev = sortedCpArr[0];
            for (int i = 1; i < sortedCpArr.length; i++) {
                final int cp = sortedCpArr[i];
                final boolean gotJump = (cp - 1 != prev);
                if (gotJump) {
                    rangeCount++;
                }
                prev = cp;
            }
        }
        return rangeCount;
    }

    static int[] toMinMaxCpArr(int[] sortedCpArr) {
        final int rangeCount = computeRangeCount(sortedCpArr);

        final int capacity = NbrsUtils.timesExact(2, rangeCount);
        final int[] minMaxCpArr = new int[capacity];

        int prevCp = -2;
        int a = -1;

        int i = 0;

        for (int cp : sortedCpArr) {
            if (a == -1) {
                a = cp;
            } else {
                final boolean gotJump = (cp - 1 > prevCp);
                if (gotJump) {
                    minMaxCpArr[i++] = a;
                    minMaxCpArr[i++] = prevCp;
                    a = cp;
                }
            }
            prevCp = cp;
        }

        if (a != -1) {
            final int b = prevCp;
            minMaxCpArr[i++] = a;
            minMaxCpArr[i++] = b;
        }

        return minMaxCpArr;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void initIfNeeded() {
        if (this.closeable != null) {
            return;
        }

        try {
            this.init();
        } catch (IOException e) {
            throw new RuntimeException("fontFilePath = " + this.fontFilePath, e);
        }
    }

    private void init() throws IOException {

        final File file = new File(this.fontFilePath);

        final ArrayList<TrueTypeFont> ttfList = new ArrayList<TrueTypeFont>();
        final TrueTypeFontProcessor processor = new TrueTypeFontProcessor() {
            @Override
            public void process(TrueTypeFont ttf) throws IOException {
                ttfList.add(ttf);
            }
        };

        if (BindingStringUtils.endsWithIgnoreCase(this.fontFilePath, ".ttc")
                || BindingStringUtils.endsWithIgnoreCase(this.fontFilePath, ".otc")) {
            
            final TrueTypeCollection ttc = new TrueTypeCollection(file);
            ttc.processAllFonts(processor);
            this.closeable = ttc;

            this.ttfArr = ttfList.toArray(new TrueTypeFont[ttfList.size()]);
            
        } else if (BindingStringUtils.endsWithIgnoreCase(this.fontFilePath, ".ttf")
                || BindingStringUtils.endsWithIgnoreCase(this.fontFilePath, ".otf")) {
            
            final TTFParser parser = new TTFParser();
            final TrueTypeFont ttf = parser.parse(file);
            
            if (DEBUG) {
                Dbg.log("processor.process(...)...");
            }
            final long a = (DEBUG ? System.nanoTime() : 0L);
            
            processor.process(ttf);
            
            final long b = (DEBUG ? System.nanoTime() : 0L);
            if (DEBUG) {
                Dbg.log("...processor.process(...) took " + TimeUtils.nsToS(b-a) + " s");
            }
            
            this.closeable = ttf;

            this.ttfArr = ttfList.toArray(new TrueTypeFont[ttfList.size()]);
        
        } else if (BindingStringUtils.endsWithIgnoreCase(this.fontFilePath, ".afm")) {
            
            final FileInputStream fis = new FileInputStream(file);
            try {
                final AFMParser parser = new AFMParser(fis);
                final FontMetrics fm = parser.parse();
                final List<CharMetric> cmList = fm.getCharMetrics();
                this.cmList = cmList;
            } finally {
                fis.close();
            }
        }
    }

    private static BwdFontKind computeFontKind_ttf(TrueTypeFont ttf) throws IOException {

        final String family = ttf.getName();

        final boolean bold;
        final boolean italic;
        {
            /*
             * https://www.microsoft.com/typography/otspec/head.htm
             */
            final HeaderTable headerTable = ttf.getHeader();
            final int macStyle = headerTable.getMacStyle();
            bold = ((macStyle & (1<<0)) != 0);
            italic = ((macStyle & (1<<1)) != 0);
        }


        final BwdFontKind fontKind = new BwdFontKind(family, bold, italic);
        return fontKind;
    }

    private static CodePointSet toCodePointSet(MyCpSet cpSet) {
        final int[] sortedCpArr = cpSet.toSortedCpArr();
        final int[] minMaxCpArr = toMinMaxCpArr(sortedCpArr);
        final CodePointSet codePointSet = new CodePointSet(minMaxCpArr);
        return codePointSet;
    }
    
    private CodePointSet computeCodePointSet_afm(
            int fontIndex,
            int maxDisplayableCodePoint) {
        if (fontIndex != 0) {
            throw new IllegalArgumentException("" + fontIndex);
        }
        final MyCpSet cpSet = new MyCpSet();
        for (CharMetric cm : this.cmList) {
            final int cp = cm.getCharacterCode();
            if (cp <= 0) {
                /*
                 * TODO fontbox Can happen, even with negative values! (-1).
                 */
                continue;
            }
            if (cp > maxDisplayableCodePoint) {
                continue;
            }
            cpSet.add(cp);
        }
        return toCodePointSet(cpSet);
    }
    
    private CodePointSet computeCodePointSet_ttf(
            TrueTypeFont ttf,
            int maxDisplayableCodePoint) throws IOException {

        MyCpSet cpSet = this.tmpCpSet;
        if (cpSet == null) {
            cpSet = new MyCpSet();
            this.tmpCpSet = cpSet;
        } else {
            cpSet.clear();
        }

        if (false) {
            /*
             * TODO fontbox We use per-table glyph count,
             * to avoid useless looping.
             */
            final int glyphCount = ttf.getNumberOfGlyphs();
        }
        final HorizontalMetricsTable hmTable = ttf.getHorizontalMetrics();

        final CmapTable cmapTable = ttf.getCmap();
        final CmapSubtable[] cmapSubtableArr = cmapTable.getCmaps();
        /*
         * cmap subtables (platformId, platformIdEncoding):
         * (0,3) : Unicode, BMP (UCS-2).
         * (0,4) : Unicode, UCS-4.
         * (1,0) : Mac, not many code points, and weird ones (like 0x8, 0x9, 0xD, 0x1D).
         * (3,1) : Windows, BMP (UCS-2).
         * (3,10) : Windows, UCS-4.
         * 
         * We loop on all tables, even if some seem to include others,
         * because experimentally there are often some displayable code points
         * that are in some tables and not in others.
         */
        for (CmapSubtable cmapSubtable : cmapSubtableArr) {

            // Gid 0 corresponds to ".notdef" glyph, so we ignore it.
            final int glyphCount = cmapSubtable.getMaxGid();
            for (int gid = 1; gid < glyphCount; gid++) {
                final int[] charCodeArr = cmapSubtable.getCharCodesArr(gid);
                if (charCodeArr == null) {
                    continue;
                }
                /*
                 * Using advance to know whether we surely have a glyph,
                 * and not glyph table, even though space or such might have advance,
                 * for the following reasons:
                 * First, our spec for InterfaceBwdFont.canDisplay(codePoint)
                 * allows true results even if there is no actual glyph,
                 * but the font can display the code point "as intended",
                 * for example as a space.
                 * Second, glyph table is not always available, is a bit slower (*),
                 * and never using it make things more consistent across font formats.
                 * 
                 * (*) Actually, extremely slow if using FontBox's GlyphTable.getGlyph(gid)
                 * method to know whether there is a glyph, but we added a much faster
                 * GlyphTable.hasGlyph(gid) method for that.
                 */
                final int width = hmTable.getAdvanceWidth(gid);
                if (width <= 0) {
                    continue;
                }
                for (int i = 0; i < charCodeArr.length; i++) {
                    final int charCode = charCodeArr[i];
                    if (charCode <= 0) {
                        /*
                         * TODO fontbox For some reason, 0 can happen,
                         * even when font has no glyph for it (???).
                         * We also guards against negative values,
                         * in case such values could occur too.
                         */
                        continue;
                    }
                    if (charCode > maxDisplayableCodePoint) {
                        continue;
                    }
                    cpSet.add(charCode);
                }
            }
        }
        
        /*
         * 
         */

        return toCodePointSet(cpSet);
    }
}
