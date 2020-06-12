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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.cases.utils.FontBoundingBoxTestUtils;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;

public class FontGlyphTableBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Used with default font kind, for labels.
     */
    private static final int INFO_FONT_TARGET_HEIGHT = 16;
    private static final int INFO_FONT_LINE_HEIGHT = INFO_FONT_TARGET_HEIGHT + 2;
    
    /**
     * Used for font to test.
     */
    private static final int MAX_GLYPHS_FONT_SIZE = 20;
    /**
     * Not max by default, which typically leaks a bit out of cells.
     */
    private static final int DEFAULT_GLYPHS_FONT_SIZE = MAX_GLYPHS_FONT_SIZE - 4;
    
    private static final int MAX_GLYPHS_FONT_LINE_HEIGHT = MAX_GLYPHS_FONT_SIZE + 2;
    
    /*
     * 
     */
    
    private static final int LEFT_BAR_WIDTH = 100;
    private static final int FONT_SIZE_BOX_OFFSET = 120;
    private static final int FONT_METRICS_OFFSET = FONT_SIZE_BOX_OFFSET + 40;
    
    /*
     * 
     */

    /**
     * NB: BONUS_SYSTEM_FONT_FILE_PATH_LIST contains FreeMono.ttf.
     */
    private static final String[] USER_FONT_FILE_PATH_ARR = new String[]{
        /**
         * Our usual default font.
         */
        BwdTestResources.TEST_FONT_LUCIDA_CONSOLE_TTF,
        /**
         * Unicode fonts, to check whether texts in various scripts.
         */
        BwdTestResources.TEST_FONT_LUCIDA_SANS_UNICODE_TTF,
        BwdTestResources.TEST_FONT_UNIFONT_8_0_01_TTF,
        /**
         * To check whether OTF format is supported.
         */
        BwdTestResources.TEST_FONT_FREE_MONO_OTF,
        /**
         * To check that style is properly computed.
         */
        BwdTestResources.TEST_FONT_FREE_MONO_BOLD_OBLIQUE_TTF,
        /**
         * To check whether TTC format (which can contain multiple TTF fonts) is supported.
         */
        BwdTestResources.TEST_FONT_WQY_MICROHEI_TTC,
        /**
         * To check whether AFM format is supported.
         */
        BwdTestResources.TEST_FONT_A010013L_AFM,
    };

    /**
     * 
     */
    private static final String[][] SCRIPT_AND_SAMPLE_ARR = new String[][]{
        // Left-to-right, ordered by Unicode values.
        {"Latin", "text"},
        {"Greek", "\u03BA\u03B5\u03AF\u03BC\u03B5\u03BD\u03BF"},
        {"Cyrillic", "\u0442\u0435\u043A\u0441\u0442"},
        {"Chinese", "\u6587\u672C"},
        // Right-to-left, ordered by Unicode values.
        {"Hebrew", "\u05D8\u05B6\u05E7\u05E1\u05D8"},
        {"Arabic", "\u0646\u0635"}
    };

    /*
     * 
     */
    
    /**
     * Spacing for when we draw characters one by one.
     */
    private static final int INFO_FONT_CHAR_SPACING = (int) (INFO_FONT_TARGET_HEIGHT * 0.7);
    
    private static final String ROW_COLL_TITLE = "cpHi\\cpLo";
    
    private static final int TOP_BAR_HEIGHT = (int) (7 * INFO_FONT_LINE_HEIGHT + 1.5 * MAX_GLYPHS_FONT_LINE_HEIGHT);

    /*
     * 
     */
    
    private static final BwdColor BG_COLOR = BwdColor.valueOfArgb32(0xFFF8F8F8);
    private static final BwdColor FG_COLOR = BwdColor.BLACK;
    private static final BwdColor FONT_SELECTED_BG_COLOR = BwdColor.valueOfArgb32(0xFFF0F0F0);
    private static final BwdColor COLOR_THEORETICAL_BBOX_LEAK = BwdColor.YELLOW;
    
    /**
     * Light alphaed grey.
     */
    private static final BwdColor COLOR_FILL_CANT_DISPLAY_FOR_SURE = BwdColor.valueOfArgb32(0x40404040);
    /**
     * Light alphaed red.
     */
    private static final BwdColor COLOR_FILL_LEAK = BwdColor.valueOfArgb32(0x40FF4040);
    /**
     * Semi-light alphaed green.
     */
    private static final BwdColor COLOR_THEORETICAL_BBOX_NO_LEAK = BwdColor.valueOfArgb32(0x8080FF80);

    private static final int BG_ARGB32 = BG_COLOR.toArgb32();
    
    /*
     * 
     */
    
    private static final int ROW_COUNT = 16;
    private static final int COL_COUNT = 16;
    
    /**
     * Interior width of cells.
     * 
     * Each cell contains the string formed by the concatenation of {'|', glyph, '|'}
     * (so that we can see the advance if any), enclosed in a box which indicates if
     * the glyph is considered displayable (blue) or not (red).
     * Cells themselves are not drawn, since the enclosing boxes suffice to indicate
     * where the cells are, and are just separated by a line of background color.
     */
    private static final int CELL_WIDTH = 2 * MAX_GLYPHS_FONT_SIZE + 2;

    /**
     * Interior height of cells.
     */
    private static final int CELL_HEIGHT = MAX_GLYPHS_FONT_SIZE + 2;

    /*
     * 
     */

    private static final int INITIAL_WIDTH =
            LEFT_BAR_WIDTH
            + (1 + COL_COUNT * (CELL_WIDTH + 1));
    /**
     * +1 since TTC file contains two fonts.
     */
    private static final int INITIAL_HEIGHT =
            TOP_BAR_HEIGHT
            + (1 + ROW_COUNT * (CELL_HEIGHT + 1));
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyFontKindIndexData {
        final List<BwdFontKind> fontKindList;
        final boolean isLeft;
        int index = -1;
        /**
         * Font kind text might leak outside on the right,
         * we just use it as hit box.
         */
        GRect fontKindBox = GRect.DEFAULT_EMPTY;
        GRect decrCharBox = GRect.DEFAULT_EMPTY;
        GRect incrCharBox = GRect.DEFAULT_EMPTY;
        public MyFontKindIndexData(
                List<BwdFontKind> fontKindList,
                boolean isLeft) {
            this.fontKindList = fontKindList;
            this.isLeft = isLeft;
            if (fontKindList.size() != 0) {
                this.index = 0;
            }
        }
        public boolean gotSomeFont() {
            return this.fontKindList.size() != 0;
        }
        public BwdFontKind getFontKind() {
            if (!this.gotSomeFont()) {
                return null;
            }
            return this.fontKindList.get(this.index);
        }
        public void updateBoxes(GRect clientBox) {
            final int fontKindX =
                    clientBox.x()
                    + INFO_FONT_CHAR_SPACING
                    + (this.isLeft ? 0 : clientBox.xSpan() / 2);
            final int fontKindY =
                    clientBox.y()
                    + INFO_FONT_LINE_HEIGHT
                    + INFO_FONT_LINE_HEIGHT / 2;
            this.fontKindBox = GRect.valueOf(
                    fontKindX,
                    fontKindY,
                    clientBox.xSpan() / 2 - INFO_FONT_CHAR_SPACING,
                    INFO_FONT_TARGET_HEIGHT);
            
            // On the left, top.
            this.decrCharBox = GRect.valueOf(
                    fontKindX - INFO_FONT_CHAR_SPACING,
                    fontKindY - INFO_FONT_LINE_HEIGHT/2,
                    INFO_FONT_CHAR_SPACING,
                    INFO_FONT_TARGET_HEIGHT);
            // On the left, bottom.
            this.incrCharBox = GRect.valueOf(
                    fontKindX - INFO_FONT_CHAR_SPACING,
                    fontKindY + INFO_FONT_LINE_HEIGHT/2,
                    INFO_FONT_CHAR_SPACING,
                    INFO_FONT_TARGET_HEIGHT);
        }
        /**
         * @return True if a box was hit, false otherwise.
         */
        public boolean onMousePressPrimary(int x, int y) {
            boolean hit = this.fontKindBox.contains(x, y);
            if (this.decrCharBox.contains(x, y)) {
                hit = true;
                if (this.index > 0) {
                    this.index--;
                } else {
                    this.index = this.fontKindList.size() - 1;
                }
            } else if (this.incrCharBox.contains(x, y)) {
                hit = true;
                if (this.index < this.fontKindList.size() - 1) {
                    this.index++;
                } else {
                    this.index = 0;
                }
            }
            return hit;
        }
    }

    /**
     * Since we have 0xFF code points per page, first code point of a page
     * always ends with 0x00.
     * 
     * First code point for last page is 0x10FF00.
     * 
     * Considering the first code point of a page is 0xabcd00,
     * and only storing {a,b,c,d} digits, so that we can increment
     * and decrement them separately.
     */
    private class My1stCpDigitData {
        final int digitIndex;
        final int bitShift;
        final int step;
        GRect digitBox = GRect.DEFAULT_EMPTY;
        GRect decrCharBox = GRect.DEFAULT_EMPTY;
        GRect incrCharBox = GRect.DEFAULT_EMPTY;
        public My1stCpDigitData(int digitIndex) {
            this.digitIndex = digitIndex;
            final int bitSize = 4;
            this.bitShift = (2+3 - digitIndex) * bitSize;
            this.step = (1 << this.bitShift);
            
            final GRect digitBox = compute1stCpDynDigitBox(this.digitIndex);
            this.digitBox = digitBox;
            // Left of digit.
            this.decrCharBox = digitBox.withPosDeltas(-digitBox.xSpan(), 0);
            // Right of digit.
            this.incrCharBox = digitBox.withPosDeltas(digitBox.xSpan(), 0);
        }
        public int getDigit() {
            return (minCp >>> this.bitShift) & 0xF;
        }
        public void onMousePressPrimary(int x, int y) {
            final int oldMinCp = minCp;
            int newMinCp = oldMinCp;
            if (this.decrCharBox.contains(x, y)) {
                newMinCp -= this.step;
            } else if (this.incrCharBox.contains(x, y)) {
                newMinCp += this.step;
            }
            if (newMinCp != oldMinCp) {
                if (isMinCpValid(newMinCp)) {
                    minCp = newMinCp;
                }
            }
        }
    }

    private class MyFontSizeNumberData {
        GRect numberBox = GRect.DEFAULT_EMPTY;
        GRect decrCharBox = GRect.DEFAULT_EMPTY;
        GRect incrCharBox = GRect.DEFAULT_EMPTY;
        public MyFontSizeNumberData() {
        }
        public void updateLocation(GRect clientBox) {
            final GRect numberBox = GRect.valueOf(
                    clientBox.xSpan() / 2 + FONT_SIZE_BOX_OFFSET,
                    (3 * INFO_FONT_LINE_HEIGHT) + INFO_FONT_LINE_HEIGHT / 2,
                    2 * INFO_FONT_CHAR_SPACING,
                    INFO_FONT_TARGET_HEIGHT);
            this.numberBox = numberBox;
            // Left of number.
            this.decrCharBox = numberBox.withPosDeltas(-INFO_FONT_CHAR_SPACING, 0).withXSpan(INFO_FONT_CHAR_SPACING);
            // Right of number.
            this.incrCharBox = numberBox.withPosDeltas(numberBox.xSpan(), 0).withXSpan(INFO_FONT_CHAR_SPACING);
        }
        public void onMousePressPrimary(int x, int y) {
            if (this.decrCharBox.contains(x, y)) {
                decrementGlyphsFontSize();
            } else if (this.incrCharBox.contains(x, y)) {
                incrementGlyphsFontSize();
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final FontBoundingBoxTestUtils bboxUtils = new FontBoundingBoxTestUtils(BG_ARGB32);
    
    private MyFontKindIndexData systemGlyphFontData = null;
    private MyFontKindIndexData userGlyphFontData = null;
    
    /**
     * Whether we use a system font or a user font as glyph font.
     */
    private boolean usingSystemGlyphFontElseUserGlyphFont = false;
    
    private InterfaceBwdFont glyphsFont;
    
    /*
     * 
     */
    
    private InterfaceBwdFont infoFont;
    
    /**
     * Must be a multiple of 0x100.
     */
    private int minCp = 0x0;
    
    /**
     * {a,b,c,d} and related data.
     */
    private final My1stCpDigitData[] digitDataArr = new My1stCpDigitData[4];
    {
        for (int i = 0; i < this.digitDataArr.length; i++) {
            this.digitDataArr[i] = new My1stCpDigitData(i);
        }
    }
    
    private final MyFontSizeNumberData fontSizeNumberData = new MyFontSizeNumberData();
    
    /*
     * For quick jumps to related glyphs pages,
     * by clicking on scripts and samples.
     */
    
    private final GRect[] scriptAndSampleBoxArr = new GRect[SCRIPT_AND_SAMPLE_ARR.length];
    {
        for (int i = 0; i < this.scriptAndSampleBoxArr.length; i++) {
            this.scriptAndSampleBoxArr[i] = GRect.DEFAULT_EMPTY;
        }
    }
    
    /*
     * For quick jumps to fonts, by typing characters.
     */
    
    private static final double LOOKUP_TIMEOUT_S = 1.0;
    private double lookupStartS;
    private final List<Integer> lookupCpList = new ArrayList<Integer>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public FontGlyphTableBwdTestCase() {
    }
    
    public FontGlyphTableBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new FontGlyphTableBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new FontGlyphTableBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    /*
     * 
     */
    
    @Override
    public List<String> getUserFontFilePathListElseNull() {
        return Arrays.asList(USER_FONT_FILE_PATH_ARR);
    }

    /*
     * 
     */
    
    @Override
    public void onKeyTyped(BwdKeyEventT event) {
        super.onKeyTyped(event);
        
        final double nowS = this.getBinding().getUiThreadScheduler().getClock().getTimeS();
        final double dtS = nowS - this.lookupStartS;
        if (dtS > LOOKUP_TIMEOUT_S) {
            this.lookupStartS = nowS;
            this.lookupCpList.clear();
        }
        
        this.lookupCpList.add(event.getCodePoint());
        final StringBuilder sb = new StringBuilder();
        for (Integer cp : this.lookupCpList) {
            sb.append(LangUtils.stringOfCodePoint(cp));
        }
        final String lookupString = sb.toString();
        
        final MyFontKindIndexData data;
        if (this.usingSystemGlyphFontElseUserGlyphFont) {
            data = this.systemGlyphFontData;
        } else {
            data = this.userGlyphFontData;
        }
        final List<BwdFontKind> fontKindList = data.fontKindList;
        
        /*
         * Looking for first font kind which family starts
         * with the lookup string, ignoring case.
         * Should be fast enough that we don't need a tree set.
         */
        int index = -1;
        for (int i = 0; i < fontKindList.size(); i++) {
            final BwdFontKind fontKind = fontKindList.get(i);
            final String family = fontKind.fontFamily();
            if (family.length() >= lookupString.length()) {
                final String familyStart = family.substring(0, lookupString.length());
                if (familyStart.equalsIgnoreCase(lookupString)) {
                    index = i;
                    break;
                }
            }
        }
        
        if ((index >= 0)
                && (index != data.index)) {
            data.index = index;
            this.getHost().ensurePendingClientPainting();
        }
    }
    
    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);
        
        if (event.getButton() == BwdMouseButtons.PRIMARY) {
            final int x = event.xInClient();
            final int y = event.yInClient();
            
            {
                if (this.systemGlyphFontData != null) {
                    final boolean hit = this.systemGlyphFontData.onMousePressPrimary(x, y);
                    if (hit) {
                        final boolean gotSome = (this.systemGlyphFontData.fontKindList.size() != 0);
                        if (gotSome) {
                            this.usingSystemGlyphFontElseUserGlyphFont = true;
                        }
                    }
                }
                if (this.userGlyphFontData != null) {
                    final boolean hit = this.userGlyphFontData.onMousePressPrimary(x, y);
                    if (hit) {
                        final boolean gotSome = (this.userGlyphFontData.fontKindList.size() != 0);
                        if (gotSome) {
                            this.usingSystemGlyphFontElseUserGlyphFont = false;
                        }
                    }
                }
            }
            
            {
                for (My1stCpDigitData digitData : this.digitDataArr) {
                    digitData.onMousePressPrimary(x, y);
                }
            }
            
            this.fontSizeNumberData.onMousePressPrimary(x, y);
            
            {
                for (int i = 0; i < this.scriptAndSampleBoxArr.length; i++) {
                    final GRect box = this.scriptAndSampleBoxArr[i];
                    if (box.contains(x, y)) {
                        final String sample = SCRIPT_AND_SAMPLE_ARR[i][1];
                        final int codePoint = sample.codePointAt(0);
                        final int newMinCp = (codePoint / 0x100) * 0x100;
                        if (newMinCp != this.minCp) {
                            this.minCp = newMinCp;
                        }
                        break;
                    }
                }
            }
            
            // Repainting on each click, not bothering
            // to compute whether it's actually needed.
            this.getHost().ensurePendingClientPainting();
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final GRect clientBox = g.getBox();
        final int xMin = clientBox.x();
        final int yMin = clientBox.y();
        final int xSpan = clientBox.xSpan();
        final int ySpan = clientBox.ySpan();

        final int minCp = this.minCp;
        
        /*
         * First, computing theoretical and actual bounding box for each glyph,
         * and eventually drawing them.
         */

        this.ensureGlyphFontAndDataCreated();

        final ArrayList<GRect> theoBboxByCpIndex = new ArrayList<GRect>();
        final ArrayList<GRect> actuBboxByCpIndex = new ArrayList<GRect>();
        {
            final InterfaceBwdFont font = this.glyphsFont;
            
            if (false) {
                /*
                 * Very slow with JavaFX binding, due to one screenshot per glyph.
                 */
                int cp = minCp;
                for (int row = 0; row < ROW_COUNT; row++) {
                    for (int col = 0; col < COL_COUNT; col++) {
                        final boolean canDisplay = font.canDisplay(cp);
                        if (canDisplay) {
                            final GRect theoBbox = GRect.valueOf(
                                    0,
                                    0,
                                    font.fontMetrics().computeCharWidth(cp),
                                    font.fontMetrics().fontHeight());
                            theoBboxByCpIndex.add(theoBbox);

                            final GRect actuBbox = this.bboxUtils.computeCpBoundingBox(g, font, cp);
                            actuBboxByCpIndex.add(actuBbox);
                        } else {
                            // The drawn glyph might be from another font,
                            // which would be inconsistent with our font's metrics.
                            theoBboxByCpIndex.add(null);
                            actuBboxByCpIndex.add(null);
                        }

                        cp++;
                    }
                }
            } else {
                final List<Integer> cpList = new ArrayList<Integer>();
                int cp = minCp;
                for (int i = 0; i < (ROW_COUNT * COL_COUNT); i++) {
                    final boolean canDisplay = font.canDisplay(cp);
                    if (canDisplay) {
                        cpList.add(cp);
                        final GRect theoBbox = GRect.valueOf(
                                0,
                                0,
                                font.fontMetrics().computeCharWidth(cp),
                                font.fontMetrics().fontHeight());
                        theoBboxByCpIndex.add(theoBbox);
                    } else {
                        // The drawn glyph might be from another font,
                        // which would be inconsistent with our font's metrics.
                        theoBboxByCpIndex.add(null);
                    }
                    cp++;
                }
                
                final List<GRect> bbList = this.bboxUtils.computeCpListBoundingBoxList(g, font, cpList);
                
                int j = 0;
                for (int i = 0; i < theoBboxByCpIndex.size(); i++) {
                    final GRect theoBbox = theoBboxByCpIndex.get(i);
                    if (theoBbox != null) {
                        final GRect bb = bbList.get(j++);
                        actuBboxByCpIndex.add(bb);
                    } else {
                        actuBboxByCpIndex.add(null);
                    }
                }
            }
        }
        
        /*
         * 
         */

        g.setColor(BG_COLOR);
        g.clearRect(clientBox);
        
        /*
         * 
         */
        
        this.ensureInfoFontCreated();
        
        this.systemGlyphFontData.updateBoxes(clientBox);
        this.userGlyphFontData.updateBoxes(clientBox);

        /*
         * Top info / controls, and text samples.
         */

        {
            g.setFont(this.infoFont);

            /*
             * System and user fonts labels.
             */
            
            int lineY = 0;
            {
                g.setColor(FG_COLOR);
                {
                    final int x = 0;
                    final int y = lineY;
                    final int fn = this.systemGlyphFontData.index + 1;
                    final int fc = this.systemGlyphFontData.fontKindList.size();
                    g.drawText(x, y, "System Fonts: (" + fn + " / " + fc + ")");
                }
                {
                    final int x = clientBox.xSpan() / 2;
                    final int y = lineY;
                    final int fn = this.userGlyphFontData.index + 1;
                    final int fc = this.userGlyphFontData.fontKindList.size();
                    g.drawText(x, y, "User Fonts: (" + fn + " / " + fc + ")");
                }
            }
            lineY += INFO_FONT_LINE_HEIGHT;
            
            /*
             * System and user fonts choices.
             */
            
            {
                for (MyFontKindIndexData data : new MyFontKindIndexData[]{
                        this.systemGlyphFontData,
                        this.userGlyphFontData
                }) {
                    final boolean isSelected =
                            (this.usingSystemGlyphFontElseUserGlyphFont == (data == this.systemGlyphFontData));
                    {
                        final GRect box = data.fontKindBox;
                        if (isSelected) {
                            g.setColor(FONT_SELECTED_BG_COLOR);
                            g.fillRect(box);
                            g.setColor(FG_COLOR);
                        }
                        final int x = box.x();
                        final int y = box.y();
                        if (data.gotSomeFont()) {
                            g.drawText(x, y, data.getFontKind().toString());
                        }
                    }
                    {
                        final GRect box = data.decrCharBox;
                        g.drawText(box.x(), box.y(), "-");
                        g.drawRect(box);
                    }
                    {
                        final GRect box = data.incrCharBox;
                        g.drawText(box.x(), box.y(), "+");
                        g.drawRect(box);
                    }
                }
            }
            lineY += 2 * INFO_FONT_LINE_HEIGHT;
            
            /*
             * 1st code point and font size choices.
             */
            
            {
                final int interLineY = lineY + INFO_FONT_LINE_HEIGHT / 2;
                
                g.setFont(this.infoFont);
                g.setColor(FG_COLOR);
                {
                    final String str = "1st CP:";
                    final int x = 0;
                    final int y = interLineY;
                    g.drawText(x, y, str);
                }
                {
                    for (int i = 0; i < this.digitDataArr.length; i++) {
                        final My1stCpDigitData digitData = this.digitDataArr[i];
                        g.drawText(
                                digitData.digitBox.x(),
                                digitData.digitBox.y(),
                                NumbersUtils.toString(digitData.getDigit(), 16));
                        {
                            final GRect box = digitData.incrCharBox;
                            g.drawText(box.x(), box.y(), "+");
                            g.drawRect(box);
                        }
                        {
                            final GRect box = digitData.decrCharBox;
                            g.drawText(box.x(), box.y(), "-");
                            g.drawRect(box);
                        }
                    }
                    // Non-modifiable least significant digits, always zero.
                    for (int digitIndex = 4; digitIndex <= 5; digitIndex++) {
                        final GRect digitRect = compute1stCpFixDigitBox(digitIndex);
                        g.drawText(
                                digitRect.x(),
                                digitRect.y(),
                                "0");
                    }
                }
                {
                    final String str = "Font Size:";
                    final int x = clientBox.xSpan() / 2;
                    final int y = interLineY;
                    g.drawText(x, y, str);
                }
                {
                    final MyFontSizeNumberData numberData = this.fontSizeNumberData;
                    numberData.updateLocation(clientBox);
                    g.drawText(
                            numberData.numberBox.x(),
                            numberData.numberBox.y(),
                            "" + this.glyphsFont.fontId().fontSize());
                    {
                        final GRect box = numberData.incrCharBox;
                        g.drawText(box.x(), box.y(), "+");
                        g.drawRect(box);
                    }
                    {
                        final GRect box = numberData.decrCharBox;
                        g.drawText(box.x(), box.y(), "-");
                        g.drawRect(box);
                    }
                }
                {
                    final String str = "" + this.glyphsFont.fontMetrics();
                    final int x = clientBox.xSpan() / 2 + FONT_METRICS_OFFSET;
                    final int y = interLineY;
                    g.drawText(x, y, str);
                }
            }
            lineY += 2 * INFO_FONT_LINE_HEIGHT;
            
            /*
             * Scripts and samples.
             */
            
            {
                int xOffset = clientBox.x();
                final int xJump = clientBox.xSpan() / SCRIPT_AND_SAMPLE_ARR.length;
                
                for (int i = 0; i < SCRIPT_AND_SAMPLE_ARR.length; i++) {
                    final String[] scriptAndSample = SCRIPT_AND_SAMPLE_ARR[i];
                    final String scriptHeader = scriptAndSample[0] + ":";
                    final String sample = scriptAndSample[1];

                    g.setFont(this.infoFont);
                    g.drawText(xOffset, lineY, scriptHeader);

                    g.setFont(this.glyphsFont);
                    g.drawText(xOffset, lineY + INFO_FONT_LINE_HEIGHT, sample);
                    
                    this.scriptAndSampleBoxArr[i] = GRect.valueOf(
                            xOffset,
                            lineY,
                            xJump,
                            2 * INFO_FONT_LINE_HEIGHT);

                    xOffset += xJump;
                }
            }
            lineY += (int) (INFO_FONT_LINE_HEIGHT + 1.5 * MAX_GLYPHS_FONT_LINE_HEIGHT);
            
            /*
             * Cell grid columns.
             */
            
            {
                g.setFont(this.infoFont);
                
                g.setColor(FG_COLOR);
                {
                    final int x = 0;
                    final int y = lineY;
                    g.drawText(x, y, ROW_COLL_TITLE);
                }
                for (int col = 0; col < COL_COUNT; col++) {
                    final int x = LEFT_BAR_WIDTH + col * (CELL_WIDTH + 1);
                    final int y = lineY;
                    final int cpLow = minCp % COL_COUNT + col;
                    g.drawText(x, y, toPaddedHexString(cpLow, 0));
                }
            }
        }
        
        /*
         * Left info / controls.
         */

        {
            g.setFont(this.infoFont);
            {
                g.setColor(FG_COLOR);
                for (int row = 0; row < ROW_COUNT; row++) {
                    final int x = 0;
                    final int y = TOP_BAR_HEIGHT + row * (CELL_HEIGHT + 1);
                    final int cpHi = minCp/COL_COUNT + row;
                    g.drawText(x, y, toPaddedHexString(cpHi, 5));
                }
            }
        }
        
        /*
         * Glyphs grid.
         */
        
        {
            g.setFont(this.glyphsFont);
            
            int cpIndex = 0;
            for (int row = 0; row < ROW_COUNT; row++) {
                final int y = TOP_BAR_HEIGHT + row * (CELL_HEIGHT + 1);
                for (int col = 0; col < COL_COUNT; col++) {
                    final int x = LEFT_BAR_WIDTH + col * (CELL_WIDTH + 1);
                    
                    final int cp = minCp + cpIndex;
                    final int cpx = x + 1;
                    final int cpy = y + 1;
                    
                    final boolean canDisplay = this.glyphsFont.canDisplay(cp);
                    
                    final GRect theoBbox = theoBboxByCpIndex.get(cpIndex);
                    final GRect actuBbox = actuBboxByCpIndex.get(cpIndex);
                    final boolean gotLeak =
                            (theoBbox != null)
                            && (actuBbox != null)
                            && (!actuBbox.isEmpty())
                            && (!theoBbox.contains(actuBbox));
                    
                    /*
                     * Filling background depending on leak or sure-displayability.
                     */

                    if (gotLeak) {
                        g.setColor(COLOR_FILL_LEAK);
                        g.fillRect(x, y, CELL_WIDTH, CELL_HEIGHT);
                    }
                    if (!canDisplay) {
                        g.setColor(COLOR_FILL_CANT_DISPLAY_FOR_SURE);
                        g.fillRect(x, y, CELL_WIDTH, CELL_HEIGHT);
                    }
                    
                    /*
                     * Drawing theoretical bounding box.
                     */

                    if (theoBbox != null) {
                        if (gotLeak) {
                            g.setColor(COLOR_THEORETICAL_BBOX_LEAK);
                        } else {
                            g.setColor(COLOR_THEORETICAL_BBOX_NO_LEAK);
                        }
                        g.drawRect(
                                cpx + theoBbox.x(),
                                cpy + theoBbox.y(),
                                theoBbox.xSpan(),
                                theoBbox.ySpan());
                    }
                    
                    g.setColor(FG_COLOR);
                    final String text = LangUtils.stringOfCodePoint(cp);
                    g.drawText(cpx, cpy, text);
                    
                    cpIndex++;
                }
            }
        }

        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
   
    private void ensureInfoFontCreated() {
        final InterfaceBwdFontHome home = this.getBinding().getFontHome();
        
        if (this.infoFont == null) {
            this.infoFont = home.newFontWithClosestHeight(
                    home.getDefaultFont().fontKind(),
                    INFO_FONT_TARGET_HEIGHT);
        }
    }
    
    private void ensureGlyphFontAndDataCreated() {
        final InterfaceBwdFontHome home = this.getBinding().getFontHome();
        
        if (this.systemGlyphFontData == null) {
            final List<BwdFontKind> systemFontKindList = new ArrayList<BwdFontKind>(home.getLoadedSystemFontKindSet());
            final List<BwdFontKind> userFontKindList = new ArrayList<BwdFontKind>(home.getLoadedUserFontKindSet());
            
            {
                final boolean isLeft = true;
                this.systemGlyphFontData = new MyFontKindIndexData(systemFontKindList, isLeft);
            }
            {
                final boolean isLeft = false;
                this.userGlyphFontData = new MyFontKindIndexData(userFontKindList, isLeft);
            }
        }
        
        /*
         * Taking care of cases where no system or user font could be loaded.
         */

        if (this.usingSystemGlyphFontElseUserGlyphFont) {
            if (!this.systemGlyphFontData.gotSomeFont()) {
                this.usingSystemGlyphFontElseUserGlyphFont = false;
            }
        } else {
            if (!this.userGlyphFontData.gotSomeFont()) {
                /*
                 * Can happen if binding fails to load any user font,
                 * or to figure out it loaded one.
                 */
                this.usingSystemGlyphFontElseUserGlyphFont = true;
            }
        }
        
        final List<BwdFontKind> fontKindList;
        final int fontKindIndex;
        if (this.usingSystemGlyphFontElseUserGlyphFont) {
            fontKindList = this.systemGlyphFontData.fontKindList;
            fontKindIndex = this.systemGlyphFontData.index;
        } else {
            fontKindList = this.userGlyphFontData.fontKindList;
            fontKindIndex = this.userGlyphFontData.index;
        }
        if (fontKindList.size() == 0) {
            throw new IllegalStateException("no system or user font could be loaded");
        }
        
        final BwdFontKind newGlyphFontKind = fontKindList.get(fontKindIndex);
        
        if ((this.glyphsFont != null)
                && (this.glyphsFont.fontKind().equals(newGlyphFontKind))) {
            // Already up.
        } else {
            final int fontSize;
            if (this.glyphsFont == null) {
                fontSize = DEFAULT_GLYPHS_FONT_SIZE;
            } else {
                fontSize = this.glyphsFont.fontSize();
            }
            
            if (this.glyphsFont != null) {
                this.glyphsFont.dispose();
            }
            this.glyphsFont = home.newFontWithSize(newGlyphFontKind, fontSize);
        }
    }
    
    private void decrementGlyphsFontSize() {
        if (this.glyphsFont == null) {
            return;
        }
        
        final BwdFontId oldFontId = this.glyphsFont.fontId();
        final int oldSize = oldFontId.fontSize();
        if (oldSize <= this.getBinding().getFontHome().getMinFontSize()) {
            return;
        }
        this.glyphsFont.dispose();
        
        final InterfaceBwdFontHome home = this.getBinding().getFontHome();
        final int newSize = oldSize - 1;
        this.glyphsFont = home.newFontWithSize(oldFontId.fontKind(), newSize);
    }

    private void incrementGlyphsFontSize() {
        if (this.glyphsFont == null) {
            return;
        }
        
        final BwdFontId oldFontId = this.glyphsFont.fontId();
        final int oldSize = oldFontId.fontSize();
        if (oldSize >= MAX_GLYPHS_FONT_SIZE) {
            return;
        }
        this.glyphsFont.dispose();
        
        final InterfaceBwdFontHome home = this.getBinding().getFontHome();
        final int newSize = oldSize + 1;
        this.glyphsFont = home.newFontWithSize(oldFontId.fontKind(), newSize);
    }

    /*
     * 
     */
    
    private static GRect compute1stCpDynDigitBox(int digitIndex) {
        return GRect.valueOf(
                LEFT_BAR_WIDTH + digitIndex * (3 * INFO_FONT_TARGET_HEIGHT),
                (3 * INFO_FONT_LINE_HEIGHT) + INFO_FONT_LINE_HEIGHT / 2,
                INFO_FONT_CHAR_SPACING,
                INFO_FONT_TARGET_HEIGHT);
    }

    private static GRect compute1stCpFixDigitBox(int digitIndex) {
        final int lastDynIndex = 3;
        final GRect lastDynBox = compute1stCpDynDigitBox(lastDynIndex);
        return GRect.valueOf(
                lastDynBox.x() + (digitIndex - lastDynIndex) * (2 * INFO_FONT_TARGET_HEIGHT),
                lastDynBox.y(),
                lastDynBox.xSpan(),
                lastDynBox.ySpan());
    }

    /*
     * 
     */
    
    private static String toPaddedHexString(int value, int minDigitCount) {
        String str = NumbersUtils.toString(value, 16);
        final int nToAdd = Math.max(0, minDigitCount - str.length());
        for (int i = 0; i < nToAdd; i++) {
            str = "0" + str;
        }
        str = "0x" + str;
        return str;
    }
    
    private static boolean isMinCpValid(int minCp) {
        return ((minCp >= 0)
                && (minCp <= BwdUnicode.MAX_10FFFF))
                && ((minCp & 0xFF) == 0);
    }
}
