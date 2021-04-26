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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.BwdTestUtils;

/**
 * To implement N-layers alpha painting, with opaque or transparent background.
 */
public abstract class AbstractAlphaNLayersBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final double WINDOW_ALPHA_FP = 0.5;
    
    /*
     * 
     */
    
    private static final double ITEM_ALPHA_FP = 0.5;
    
    /**
     * RGB 32 by item index.
     */
    private static final int[] ITEM_RGB32_ARR = new int[]{
        0xFF0000,
        0x00FF00,
        0x0000FF,
    };
    private static final int NBR_OF_ITEMS = ITEM_RGB32_ARR.length;
    
    private static final int NBR_OF_ROWS = 4;
    private static final int NBR_OF_COLUMNS = 4;
    
    /*
     * Spans for items to draw (figures, text, image).
     */
    
    private static final int ITEM_X_SPAN = 50;
    private static final int ITEM_Y_SPAN = 30;
    
    /**
     * 0 means items 1 and 2 are drawn contiguously (horizontally),
     * 1 means they fully overlap.
     */
    private static final double ITEM_1_AND_2_X_OVERLAP_RATIO = 0.5;
    
    /**
     * 0 means items {1,2} and 3 are drawn contiguously (vertically),
     * 1 means they fully overlap.
     */
    private static final double ITEM_1_2_AND_3_Y_OVERLAP_RATIO = 0.5;
    
    private static final int BIG_TARGET_FONT_HEIGHT = ITEM_Y_SPAN;
    private static final String BIG_TEXT = "BOBOBOB";

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private interface InterfaceItemDrawer {
        /**
         * Color has already been set, must only draw the form.
         */
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY);
    }
    
    private static class MyItemDrawer_drawPoint implements InterfaceItemDrawer {
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            for (int dx = 0; dx < ITEM_X_SPAN; dx++) {
                for (int dy = 0; dy < ITEM_Y_SPAN; dy++) {
                    g.drawPoint(itemX + dx, itemY + dy);
                }
            }
        }
    }

    private static class MyItemDrawer_drawLine implements InterfaceItemDrawer {
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            final int x = itemX;
            final int xMax = itemX + ITEM_X_SPAN - 1;
            for (int dy = 0; dy < ITEM_Y_SPAN; dy++) {
                final int y = itemY + dy;
                g.drawLine(x, y, xMax, y);
            }
        }
    }

    private static class MyItemDrawer_drawLineStipple implements InterfaceItemDrawer {
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            final int factor = 1;
            final short pattern = (short) 0xFFFE;
            final int pixelNum = 0;
            
            final int x = itemX;
            final int xMax = itemX + ITEM_X_SPAN - 1;
            for (int dy = 0; dy < ITEM_Y_SPAN; dy++) {
                final int y = itemY + dy;
                g.drawLineStipple(
                        x, y, xMax, y,
                        factor, pattern, pixelNum);
            }
        }
    }

    private static class MyItemDrawer_drawRect implements InterfaceItemDrawer {
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            int x = itemX;
            int y = itemY;
            int xSpan = ITEM_X_SPAN;
            int ySpan = ITEM_Y_SPAN;
            while ((xSpan > 0) && (ySpan > 0)) {
                g.drawRect(x, y, xSpan, ySpan);
                x++;
                y++;
                xSpan -= 2;
                ySpan -= 2;
            }
        }
    }

    private static class MyItemDrawer_fillRect implements InterfaceItemDrawer {
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            g.fillRect(itemX, itemY, ITEM_X_SPAN, ITEM_Y_SPAN);
        }
    }

    private static class MyItemDrawer_drawOval implements InterfaceItemDrawer {
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            int x = itemX;
            int y = itemY;
            int xSpan = ITEM_X_SPAN;
            int ySpan = ITEM_Y_SPAN;
            while ((xSpan > 0) && (ySpan > 0)) {
                g.drawOval(x, y, xSpan, ySpan);
                x++;
                y++;
                xSpan -= 2;
                ySpan -= 2;
            }
        }
    }
    
    private static class MyItemDrawer_fillOval implements InterfaceItemDrawer {
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            g.fillOval(itemX, itemY, ITEM_X_SPAN, ITEM_Y_SPAN);
        }
    }
    
    private static class MyItemDrawer_drawArc implements InterfaceItemDrawer {
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            int x = itemX;
            int y = itemY;
            int xSpan = ITEM_X_SPAN;
            int ySpan = ITEM_Y_SPAN;
            while ((xSpan > 0) && (ySpan > 0)) {
                final double startDeg = 10.0;
                final double spanDeg = 350.0;
                g.drawArc(
                        x, y, xSpan, ySpan,
                        startDeg, spanDeg);
                x++;
                y++;
                xSpan -= 2;
                ySpan -= 2;
            }
        }
    }
    
    private static class MyItemDrawer_fillArc implements InterfaceItemDrawer {
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            final double startDeg = 10.0;
            final double spanDeg = 350.0;
            g.fillArc(
                    itemX, itemY, ITEM_X_SPAN, ITEM_Y_SPAN,
                    startDeg, spanDeg);
        }
    }
    
    private static class MyItemDrawer_drawOrFillPolygon implements InterfaceItemDrawer {
        private final boolean isFillElseDraw;
        public MyItemDrawer_drawOrFillPolygon(boolean isFillElseDraw) {
            this.isFillElseDraw = isFillElseDraw;
        }
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            final int xMaxRadius = ITEM_X_SPAN / 2;
            final int yMaxRadius = ITEM_Y_SPAN / 2;
            final int pointCount = 100;
            final double roundCount = 3.0;
            final int[] xArr = new int[pointCount];
            final int[] yArr = new int[pointCount];
            BwdTestUtils.computeSpiralPolygonPoints(
                    xMaxRadius,
                    yMaxRadius,
                    pointCount,
                    roundCount,
                    xArr,
                    yArr);
            g.addTransform(GTransform.valueOf(
                    0,
                    itemX + xMaxRadius,
                    itemY + yMaxRadius));
            if (isFillElseDraw) {
                g.drawPolygon(xArr, yArr, pointCount);
            } else {
                g.fillPolygon(xArr, yArr, pointCount);
            }
            g.removeLastAddedTransform();
        }
    }
    
    private class MyItemDrawer_drawText implements InterfaceItemDrawer {
        private InterfaceBwdFont bigBoldFont;
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            final GRect itemClip = GRect.valueOf(itemX, itemY, ITEM_X_SPAN, ITEM_Y_SPAN);
            // To make sure we don't draw outside item area
            // if the text or font is too large.
            g.addClipInUser(itemClip);
            try {
                final InterfaceBwdBinding binding = getBinding();
                final InterfaceBwdFontHome fontHome = binding.getFontHome();
                
                final InterfaceBwdFont initialFont = g.getFont();
                
                if (this.bigBoldFont == null) {
                    BwdFontKind bestKind = null;
                    // Looking for a bold kind.
                    for (BwdFontKind fontKind : fontHome.getLoadedFontKindSet()) {
                        if (fontKind.isBold()) {
                            bestKind = fontKind;
                        }
                    }
                    if (bestKind == null) {
                        // Using default font kind.
                        bestKind = fontHome.getDefaultFont().kind();
                    }
                    this.bigBoldFont = fontHome.newFontWithClosestHeight(bestKind, BIG_TARGET_FONT_HEIGHT);
                }
                
                try {
                    g.setFont(bigBoldFont);
                    /*
                     * Using multiple shifts, to help have glyphs
                     * (of different colors, but also accidentally same ones)
                     * overlap each other.
                     */
                    for (int shift : new int[]{
                            -ITEM_Y_SPAN/4,
                            0,
                            ITEM_Y_SPAN/4
                    }) {
                        g.drawText(
                                itemX + shift,
                                itemY + shift,
                                BIG_TEXT);
                    }
                } finally {
                    g.setFont(initialFont);
                }
            } finally {
                g.removeLastAddedClip();
            }
        }
    }
    
    private static class MyItemDrawer_drawImage implements InterfaceItemDrawer {
        private final InterfaceBwdImage image;
        public MyItemDrawer_drawImage(InterfaceBwdImage image) {
            this.image = image;
        }
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            g.drawImage(itemX, itemY, ITEM_X_SPAN, ITEM_Y_SPAN, this.image);
        }
    }
    
    private static class MyItemDrawer_flipColors_eachItem implements InterfaceItemDrawer {
        @Override
        public void drawItem(InterfaceBwdGraphics g, int itemX, int itemY) {
            g.fillRect(itemX, itemY, ITEM_X_SPAN, ITEM_Y_SPAN);
            g.flipColors(itemX, itemY, ITEM_X_SPAN, ITEM_Y_SPAN);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int CELL_X_SPAN = (int) Math.ceil(ITEM_X_SPAN * (2.0 - ITEM_1_AND_2_X_OVERLAP_RATIO));
    private static final int CELL_Y_SPAN = (int) Math.ceil(ITEM_Y_SPAN * (2.0 - ITEM_1_2_AND_3_Y_OVERLAP_RATIO));

    private static final int AREA_X_SPAN = NBR_OF_COLUMNS * (CELL_X_SPAN + 1) + 1;
    private static final int AREA_Y_SPAN = NBR_OF_ROWS * (CELL_Y_SPAN + 1) + 1;

    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(AREA_X_SPAN, AREA_Y_SPAN);

    /*
     * 
     */
    
    private InterfaceBwdImage image;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractAlphaNLayersBwdTestCase() {
    }

    public AbstractAlphaNLayersBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final InterfaceBwdBinding binding = this.getBinding();
        
        final GRect box = g.getBox();
        final int xMin = box.x();
        final int yMin = box.y();
        final int xSpan = box.xSpan();
        
        g.setColor(BwdColor.BLACK);
        g.clearRect(box);
        
        paintCells(g);
        
        //@SuppressWarnings("unused")
        int cellIndex = 0;
        
        /*
         * Geometry.
         */
        
        blendWith(g, cellIndex++, new MyItemDrawer_drawPoint());
        
        blendWith(g, cellIndex++, new MyItemDrawer_drawLine());
        
        blendWith(g, cellIndex++, new MyItemDrawer_drawLineStipple());
        
        blendWith(g, cellIndex++, new MyItemDrawer_drawRect());
        blendWith(g, cellIndex++, new MyItemDrawer_fillRect());
        
        blendWith(g, cellIndex++, new MyItemDrawer_drawOval());
        blendWith(g, cellIndex++, new MyItemDrawer_fillOval());
        
        blendWith(g, cellIndex++, new MyItemDrawer_drawArc());
        blendWith(g, cellIndex++, new MyItemDrawer_fillArc());
        
        blendWith(g, cellIndex++, new MyItemDrawer_drawOrFillPolygon(false));
        blendWith(g, cellIndex++, new MyItemDrawer_drawOrFillPolygon(true));

        blendWith(g, cellIndex++, new MyItemDrawer_drawText());
        
        InterfaceBwdImage image = this.image;
        if (image == null) {
            image = binding.newImage(BwdTestResources.TEST_IMG_FILE_PATH_FILLED_GREY_ALPHA_PNG);
            this.image = image;
        }
        
        blendWith(g, cellIndex++, new MyItemDrawer_drawImage(image));
        
        /*
         * Colors flipping (calling treatments even if not supported,
         * shouldn't hurt).
         */
        
        // Flipping colors over each item.
        blendWith(g, cellIndex++, new MyItemDrawer_flipColors_eachItem());
        
        // Flipping colors once, over the whole cell.
        {
            final int cellX = cellX(g, cellIndex);
            final int cellY = cellY(g, cellIndex);
            blendWith(g, cellIndex, new MyItemDrawer_fillRect());
            g.flipColors(cellX, cellY, CELL_X_SPAN, CELL_Y_SPAN);
            cellIndex++;
        }
        
        /*
         * Should be opaque text with opaque background behind it.
         */
        
        if (true) {
            final String text = this.getBinding().getClass().getSimpleName() + ", window alpha = " + WINDOW_ALPHA_FP;
            final int centerX = xMin + xSpan/2;
            final int textY = yMin;
            BwdTestUtils.drawTextAndBgXCentered(
                    g, BwdColor.BLACK, BwdColor.WHITE,
                    centerX,
                    textY,
                    text);
        }
        
        return null;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Cell border excluded.
     */
    private static int cellX(InterfaceBwdGraphics g, int cellIndex) {
        final int column = cellIndex % NBR_OF_COLUMNS;
        return g.getBox().x() + column * (CELL_X_SPAN + 1) + 1;
    }
    
    /**
     * Cell border excluded.
     */
    private static int cellY(InterfaceBwdGraphics g, int cellIndex) {
        final int row = cellIndex / NBR_OF_COLUMNS;
        return g.getBox().y() + row * (CELL_Y_SPAN + 1) + 1;
    }
    
    private static GPoint itemPos(int cellX, int cellY, int itemIndex) {
        switch (itemIndex) {
        case 0: {
            // Top-left in cell.
            return GPoint.valueOf(cellX, cellY);
        }
        case 1: {
            // Top-right in cell.
            return GPoint.valueOf(
                    cellX + (CELL_X_SPAN - ITEM_X_SPAN),
                    cellY);
        }
        case 2: {
            // Center-bottom in cell.
            return GPoint.valueOf(
                    cellX + (CELL_X_SPAN - ITEM_X_SPAN) / 2,
                    cellY + (CELL_Y_SPAN - ITEM_Y_SPAN));
        }
        default:
            throw new IllegalArgumentException("" + itemIndex);
        }
    }

    private static int itemArgb32(int itemIndex) {
        final int rgb32 = ITEM_RGB32_ARR[itemIndex];
        final int argb32 = Argb32.withAlphaFp(rgb32, ITEM_ALPHA_FP);
        return argb32;
    }

    private void paintCells(InterfaceBwdGraphics g) {
        final GRect box = g.getBox();
        final int x = box.x();
        final int y = box.y();
        final int xSpan = AREA_X_SPAN;
        final int ySpan = AREA_Y_SPAN;
        final int xMax = x + xSpan - 1;
        final int yMax = y + ySpan - 1;

        g.setColor(BwdColor.GREY);
        
        
        // Horizontals.
        for (int i = 0; i <= NBR_OF_ROWS; i++) {
            final int borderY = y + i * (CELL_Y_SPAN + 1);
            g.drawLine(x, borderY, xMax, borderY);
        }

        // Verticals.
        for (int i = 0; i <= NBR_OF_COLUMNS; i++) {
            final int borderX = x + i * (CELL_X_SPAN + 1);
            g.drawLine(borderX, y, borderX, yMax);
        }
    }

    /*
     * 
     */
    
    private void blendWith(InterfaceBwdGraphics g, int cellIndex, InterfaceItemDrawer itemDrawer) {
        final int cellX = cellX(g, cellIndex);
        final int cellY = cellY(g, cellIndex);
        
        for (int itemIndex = 0; itemIndex < NBR_OF_ITEMS; itemIndex++) {
            final GPoint itemPos = itemPos(cellX, cellY, itemIndex);
            final int argb32 = itemArgb32(itemIndex);
            g.setArgb32(argb32);
            
            itemDrawer.drawItem(g, itemPos.x(), itemPos.y());
        }
    }
}
