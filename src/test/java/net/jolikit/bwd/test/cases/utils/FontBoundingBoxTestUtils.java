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
package net.jolikit.bwd.test.cases.utils;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

public class FontBoundingBoxTestUtils {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int bgArgb32;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public FontBoundingBoxTestUtils(int bgArgb32) {
        this.bgArgb32 = bgArgb32;
    }
    
    /**
     * Uses the whole client box as start rectangle.
     */
    public GRect computeDrawnRect(InterfaceBwdGraphics g) {
        return this.computeDrawnRect(g, g.getBox());
    }
    
    public GRect computeCpBoundingBox(
            InterfaceBwdGraphics g,
            InterfaceBwdFont font,
            int codePoint) {
        final GRect box = g.getBox();
        
        final String text = LangUtils.stringOfCodePoint(codePoint);
        
        final int fontSize = font.size();
        
        final int textTheoWidth = font.metrics().computeCharWidth(codePoint);
        // Possibly larger than font height.
        final int textTheoHeight = fontSize;
        
        final int leakBonus = fontSize;
        
        final int cpx = box.xMid();
        final int cpy = box.yMid();
        
        final GRect searchRect = GRect.valueOf(
                cpx - 2 * leakBonus,
                cpy - 2 * leakBonus,
                textTheoWidth + 4 * leakBonus,
                textTheoHeight + 4 * leakBonus);
        
        g.setArgb32(this.bgArgb32);
        g.fillRect(searchRect);
        
        final int fgColor = Argb32.inverted(this.bgArgb32);
        g.setArgb32(fgColor);
        g.setFont(font);
        g.drawText(cpx, cpy, text);
        
        final GRect drawnRect = this.computeDrawnRect(g, searchRect);
        
        return GRect.valueOf(
                drawnRect.x() - cpx,
                drawnRect.y() - cpy,
                drawnRect.xSpan(),
                drawnRect.ySpan());
    }

    /**
     * Bulk method.
     * 
     * Some bindings, such as for JavaFX, need to do some kind
     * of screenshot of what was drawn before reading pixels,
     * which might take much time.
     * This method helps reducing the number of such screenshots,
     * by first drawing as many glyphs as it can on the client area,
     * and then only reading drawn pixels, iteratively until all
     * bounding boxes have been computed.
     */
    public List<GRect> computeCpListBoundingBoxList(
            InterfaceBwdGraphics g,
            InterfaceBwdFont font,
            List<Integer> cpList) {
        
        final List<GRect> bbList = new ArrayList<GRect>();
        
        final GRect clientBox = g.getBox();
        
        final int fontHeight = font.metrics().height();
        final int textTheoHeight = fontHeight;
        final int leakBonus = fontHeight;
        final int srBorder = 2 * leakBonus;
        final int srHeight = textTheoHeight + 2 * srBorder;
        
        if (srHeight > clientBox.ySpan()) {
            throw new AssertionError("client area not tall enough for " + srHeight);
        }

        boolean startOver = true;
        boolean newLine = false;
        // Search rectangle coordinates.
        int srx = Integer.MIN_VALUE;
        int sry = Integer.MIN_VALUE;
        
        final List<GRect> srList = new ArrayList<GRect>();
        
        int cpi = 0;
        while (cpi < cpList.size()) {
            if (DEBUG) {
                Dbg.log("cpi = " + cpi);
                Dbg.log("startOver = " + startOver);
                Dbg.log("newLine = " + newLine);
            }

            final int cp = cpList.get(cpi);
            
            if (startOver) {
                startOver = false;
                g.setArgb32(this.bgArgb32);
                g.fillRect(clientBox);
                srx = clientBox.x();
                sry = clientBox.y();
            } else if (newLine) {
                newLine = false;
                srx = clientBox.x();
                sry += srHeight;
            }
            
            final String text = LangUtils.stringOfCodePoint(cp);
            final int textTheoWidth = font.metrics().computeCharWidth(cp);
            final int srWidth = textTheoWidth + 2 * srBorder;
            
            final GRect searchRect = GRect.valueOf(
                    srx,
                    sry,
                    srWidth,
                    srHeight);
            if (DEBUG) {
                Dbg.log("searchRect = " + searchRect);
            }
            final boolean rightOut = (searchRect.xMax() > clientBox.xMax());
            if (DEBUG) {
                Dbg.log("rightOut = " + rightOut);
            }
            if (rightOut) {
                if (searchRect.x() == clientBox.x()) {
                    throw new AssertionError("client area not wide enough for " + searchRect.xSpan());
                }
                // We want to start on a new line.
                final boolean roomBelow = (searchRect.yMax() <= clientBox.yMax() - srHeight);
                if (DEBUG) {
                    Dbg.log("roomBelow = " + roomBelow);
                }
                if (roomBelow) {
                    newLine = true;
                } else {
                    if (srList.size() != 0) {
                        computeAndAddBoundingBoxesInto(
                                g,
                                srBorder,
                                srList, // in
                                bbList); // in,out
                        if (DEBUG) {
                            Dbg.log("bbList.size() (1) = " + bbList.size());
                        }
                        srList.clear();
                    }
                    startOver = true;
                }
                // Will try another location for current code point.
                continue;
            }
            
            /*
             * Here we know that search rectangle is in client area,
             * since we take care to never make it leak bellow,
             * so we can draw and measure.
             */
            
            srList.add(searchRect);

            final int fgColor = Argb32.inverted(this.bgArgb32);
            g.setArgb32(fgColor);
            g.setFont(font);
            final int cpx = srx + srBorder;
            final int cpy = sry + srBorder;
            g.drawText(cpx, cpy, text);
            
            // Going to next code point, if any.
            cpi++;
            srx += srWidth;
        }

        if (srList.size() != 0) {
            computeAndAddBoundingBoxesInto(
                    g,
                    srBorder,
                    srList, // in
                    bbList); // in,out
            if (DEBUG) {
                Dbg.log("bbList.size() (2) = " + bbList.size());
            }
            srList.clear();
        }
        
        if (bbList.size() != cpList.size()) {
            throw new AssertionError(bbList.size() + " != " + cpList.size());
        }

        return bbList;
    }
    
    /**
     * @param g Graphics filled with background color, except at the
     *          drawn places the bounding box of which must be computed.
     * @param Rectangle to shrink from.
     */
    public GRect computeDrawnRect(
            InterfaceBwdGraphics g,
            GRect startRect) {
        
        GRect newRect = startRect;
        
        // Left to right.
        while (newRect.xSpan() > 0) {
            final GRect shrunk = newRect.withBordersDeltas(1, 0, 0, 0);
            if (!isDrawnOnTheLeft(g, shrunk)) {
                newRect = shrunk;
            } else {
                break;
            }
        }
        
        if (newRect.xSpan() == 0) {
            // Nothing drawn.
            return GRect.DEFAULT_EMPTY;
        }
        
        // Right to left.
        while (newRect.xSpan() > 0) {
            final GRect shrunk = newRect.withBordersDeltas(0, 0, -1, 0);
            if (!isDrawnOnTheRight(g, shrunk)) {
                newRect = shrunk;
            } else {
                break;
            }
        }
        
        // Top to bottom.
        while (newRect.ySpan() > 0) {
            final GRect shrunk = newRect.withBordersDeltas(0, 1, 0, 0);
            if (!isDrawnOnTheTop(g, shrunk)) {
                newRect = shrunk;
            } else {
                break;
            }
        }
        
        // Bottom to top.
        while (newRect.ySpan() > 0) {
            final GRect shrunk = newRect.withBordersDeltas(0, 0, 0, -1);
            if (!isDrawnOnTheBottom(g, shrunk)) {
                newRect = shrunk;
            } else {
                break;
            }
        }
        
        return newRect;
    }
    
    /*
     * Left/right/top/bottom.
     */

    public boolean isDrawnOnTheLeft(
            InterfaceBwdGraphics g,
            GRect rect) {
        final int x = rect.x() - 1;
        final int yMin = rect.y();
        final int yMax = rect.yMax();
        return isDrawnOnVertical(g, x, yMin, yMax);
    }

    public boolean isDrawnOnTheRight(
            InterfaceBwdGraphics g,
            GRect rect) {
        final int x = rect.xMax() + 1;
        final int yMin = rect.y();
        final int yMax = rect.yMax();
        return isDrawnOnVertical(g, x, yMin, yMax);
    }

    public boolean isDrawnOnTheTop(
            InterfaceBwdGraphics g,
            GRect rect) {
        final int xMin = rect.x();
        final int xMax = rect.xMax();
        final int y = rect.y() - 1;
        return isDrawnOnHorizontal(g, xMin, xMax, y);
    }

    public boolean isDrawnOnTheBottom(
            InterfaceBwdGraphics g,
            GRect rect) {
        final int xMin = rect.x();
        final int xMax = rect.xMax();
        final int y = rect.yMax() + 1;
        return isDrawnOnHorizontal(g, xMin, xMax, y);
    }

    /*
     * 
     */

    public boolean isDrawnOnHorizontal(
            InterfaceBwdGraphics g,
            int xMin,
            int xMax,
            int y) {
        for (int x = xMin; x <= xMax; x++) {
            if (isDrawnOnPoint(g, x, y)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isDrawnOnVertical(
            InterfaceBwdGraphics g,
            int x,
            int yMin,
            int yMax) {
        for (int y = yMin; y <= yMax; y++) {
            if (isDrawnOnPoint(g, x, y)) {
                return true;
            }
        }
        return false;
    }
    
    /*
     * 
     */
    
    public boolean isDrawnOnPoint(
            InterfaceBwdGraphics g,
            int x,
            int y) {
        if (!g.getBox().contains(x, y)) {
            // Not throwing, since we can,
            // to simplify loops conditions.
            return false;
        }
        final int argb32 = g.getArgb32At(x, y);
        if (argb32 == 0) {
            // Means pixel reading not supported.
            // This test case only make sense if it's supported.
            throw new UnsupportedOperationException("pixel reading not supported");
        }
        // Considering drawn as long as not exactly same as background
        // (might be also different than foreground, due to anti-aliasing).
        return argb32 != this.bgArgb32;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param srList (in)
     * @param bbList (in,out)
     */
    private void computeAndAddBoundingBoxesInto(
            InterfaceBwdGraphics g,
            int srBorder,
            List<GRect> srList,
            List<GRect> bbList) {
        for (int i = 0; i < srList.size(); i++) {
            final GRect searchRect = srList.get(i);

            final GRect drawnRect = this.computeDrawnRect(g, searchRect);

            final int cpx = searchRect.x() + srBorder;
            final int cpy = searchRect.y() + srBorder;
            final GRect bb = GRect.valueOf(
                    drawnRect.x() - cpx,
                    drawnRect.y() - cpy,
                    drawnRect.xSpan(),
                    drawnRect.ySpan());
            bbList.add(bb);
        }
    }

}
