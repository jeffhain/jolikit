/*
 * Copyright 2020-2021 Jeff Hain
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
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.lang.NbrsUtils;

/**
 * To test that drawings are properly ordered, i.e. that drawn operations,
 * be it drawing a primitive, some text, an image, or flipping colors,
 * always apply in the order they were called.
 * 
 * This test is useful because in some bindings different drawing pipelines
 * can be used depending on what is to be drawn (for example, drawing directly
 * in an int array, or using backing drawing treatments), which can cause
 * some reordering.
 * 
 * Tests ordering for both client graphics (on the left) and writable image
 * graphics (on the right). Things of different kind are drawn top to bottom,
 * and overlapping so that ordering can be checked. We compare
 * {drawPoint, fillRect, drawText, drawImage(IFF), drawImage(WI), flipColors}
 * with each other, which makes (6*(6-1))/2) = 15 pairs,
 * and since for each pair we want to test with both possible orders,
 * it makes 30 tests per graphics, in 6 rows and 5 columns.
 * 
 * Each cell contains painted things in its left,
 * so that we can see how painting went on the screen,
 * and also a copy of these painted things in its right,
 * to check that graphics.getArgb32At(...) method
 * properly reflects what has been visibly painted.
 */
public class DrawOrderingBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final MyMethod[] MyMethod_VALUES = MyMethod.values();
    
    private static final BwdColor BG_COLOR = BwdColor.WHITE;
    private static final BwdColor FG_COLOR = BwdColor.BLACK;
    private static final BwdColor DRAW_POINT_COLOR = BwdColor.RED;
    private static final BwdColor FILL_RECT_COLOR = BwdColor.GREEN;
    private static final BwdColor DRAW_TEXT_COLOR = BwdColor.BLUE;
    
    private static final int CELL_WIDTH_DIV_2 = 50;
    private static final int CELL_WIDTH = 2 * CELL_WIDTH_DIV_2;
    private static final int CELL_HEIGHT_DIV_4 = 10;
    private static final int CELL_HEIGHT = 4 * CELL_HEIGHT_DIV_4;
    
    /**
     * Not drawing or copying in bottom and right parts of cells,
     * to keep drawings in different cells apart from each other.
     * Also, drawing only in the left part,
     * for room for copy on the right. 
     */
    private static final int DRAWING_WIDTH = (3 * CELL_WIDTH) / (4 * 2);
    private static final int DRAWING_HEIGHT = 2 * CELL_HEIGHT_DIV_4;
    
    /**
     * Short enough not to leak out of DRAWING_WIDTH.
     */
    private static final String TEXT_TO_DRAW = "TXT";

    /**
     * To create image from files.
     */
    private static final String IMAGE_FILE_PATH_OPAQUE = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PNG;
    /**
     * To create writable images.
     */
    private static final String IMAGE_FILE_PATH_WALPHA = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_ALPHA_PNG;
    
    private static final int NBR_OF_ROWS = 6;
    private static final int NBR_OF_COLS = 5;
    
    /**
     * For some libraries, things work differently when using transforms
     * (e.g. must have some dirty flag set to properly handle them),
     * so we always draw with a transform to make sure that's handled.
     * 
     * It also allows to test that coordinates are properly transformed.
     */
    private static final boolean MUST_USE_TRANSFORM = true;
    
    private static final int INITIAL_WIDTH = 2 * NBR_OF_COLS * CELL_WIDTH;
    private static final int INITIAL_HEIGHT = NBR_OF_ROWS * CELL_HEIGHT;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private enum MyMethod {
        /**
         * The primitive for which we are most likely to use
         * an int array (which is always fast) instead of backing treatment.
         */
        DRAW_POINT,
        /**
         * The primitive for which we are the least likely to use
         * an int array instead of backing treatment (which should
         * always be the fastest (per pixel) backing treatment). 
         */
        FILL_RECT,
        DRAW_TEXT,
        DRAW_IMAGE_IFF,
        DRAW_IMAGE_WI,
        FLIP_COLORS
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdFont font;
    
    private InterfaceBwdImage imageFromFileToDraw;
    private InterfaceBwdImage writableImageToDraw;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public DrawOrderingBwdTestCase() {
    }
    
    public DrawOrderingBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new DrawOrderingBwdTestCase(binding);
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

        if (this.font == null) {
            this.font = binding.getFontHome().newFontWithFloorElseClosestHeight(DRAWING_HEIGHT);
        }
        
        if (this.imageFromFileToDraw == null) {
            {
                this.imageFromFileToDraw = binding.newImage(IMAGE_FILE_PATH_OPAQUE);
            }
            {
                final InterfaceBwdImage image = binding.newImage(IMAGE_FILE_PATH_WALPHA);
                
                final InterfaceBwdWritableImage wi = binding.newWritableImage(
                        image.getWidth(),
                        image.getHeight());
                final InterfaceBwdGraphics wig = wi.getGraphics();
                wig.drawImage(0, 0, image);
                /*
                 * Finishing writable image graphics,
                 * so that we can check that even with finished graphics
                 * it can be drawn (i.e. that drawing it doesn't make use
                 * of no longer usable graphics methods).
                 */
                wig.finish();
                this.writableImageToDraw = wi;
                
                image.dispose();
            }
        }
        
        /*
         * Initial clearing of client.
         */
        
        g.setColor(BG_COLOR);
        g.fillRect(g.getBox());
        
        /*
         * 
         */
        
        final int widthLeft = INITIAL_WIDTH/2;
        final int widthRight = INITIAL_WIDTH - widthLeft;
        
        for (int k = 0; k < 2; k++) {
            final boolean isWritableImage = (k == 1);
            
            final InterfaceBwdGraphics myG;
            final InterfaceBwdWritableImage wi;
            if (isWritableImage) {
                wi = binding.newWritableImage(
                        widthRight,
                        INITIAL_HEIGHT);
                myG = wi.getGraphics();
                // Same initial clearing as for client,
                // so that we can expect identical final colors.
                myG.setColor(BG_COLOR);
                myG.fillRect(myG.getBox());
            } else {
                wi = null;
                myG = g;
            }
            
            final int cellXMax = (NBR_OF_COLS - 1) * CELL_WIDTH;
            int cellX = 0;
            int cellY = 0;
            for (int i = 0; i < MyMethod_VALUES.length; i++) {
                final MyMethod method1 = MyMethod_VALUES[i];
                for (int j = i + 1; j < MyMethod_VALUES.length; j++) {
                    final MyMethod method2 = MyMethod_VALUES[j];
                    
                    for (int order = 0; order < 2; order++) {
                        
                        /*
                         * Drawing.
                         */
                        
                        if (order == 0) {
                            drawMethod(myG, cellX, cellY, method1);
                            drawMethod(myG, cellX, cellY + CELL_HEIGHT_DIV_4, method2);
                        } else {
                            drawMethod(myG, cellX, cellY, method2);
                            drawMethod(myG, cellX, cellY + CELL_HEIGHT_DIV_4, method1);
                        }
                        
                        /*
                         * Copying.
                         */
                        
                        copyDrawnRectPixelToTheRight(myG, cellX, cellY);

                        /*
                         * 
                         */
                        
                        if (cellX == cellXMax) {
                            cellX = 0;
                            cellY += CELL_HEIGHT;
                        } else {
                            cellX += CELL_WIDTH;
                        }
                    }
                }
            }
            
            if (isWritableImage) {
                g.drawImage(widthLeft, 0, wi);
            }
        }
        
        /*
         * Line to separate client and writable image areas.
         */
        
        g.setColor(FG_COLOR);
        final int sepX = INITIAL_WIDTH/2 - CELL_WIDTH/8;
        g.drawLine(sepX, 0, sepX, INITIAL_HEIGHT);
        
        /*
         * 
         */
        
        return null;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void drawMethod(
            InterfaceBwdGraphics g,
            int x,
            int y,
            MyMethod method) {
        
        if (MUST_USE_TRANSFORM) {
            final int halfX = x/2;
            final int halfY = y/2;
            final GTransform transformToHalf = GTransform.valueOf(GRotation.ROT_0, halfX, halfY);
            g.addTransform(transformToHalf);
            x -= halfX;
            y -= halfY;
        }
        
        switch (method) {
            case DRAW_POINT: {
                g.setColor(DRAW_POINT_COLOR);
                for (int j = 0; j < DRAWING_HEIGHT; j++) {
                    for (int i = 0; i < DRAWING_WIDTH; i++) {
                        // Only drawing when sum is even,
                        // to make it obvious it's not fillRect().
                        if (NbrsUtils.isEven(i+j)) {
                            g.drawPoint(x + i, y + j);
                        }
                    }
                }
            } break;
            case FILL_RECT: {
                g.setColor(FILL_RECT_COLOR);
                g.fillRect(x, y, DRAWING_WIDTH, DRAWING_HEIGHT);
            } break;
            case DRAW_TEXT: {
                g.setColor(DRAW_TEXT_COLOR);
                g.setFont(this.font);
                g.drawText(x, y, TEXT_TO_DRAW);
            } break;
            case DRAW_IMAGE_IFF: {
                g.drawImage(x, y, DRAWING_WIDTH, DRAWING_HEIGHT, this.imageFromFileToDraw);
            } break;
            case DRAW_IMAGE_WI: {
                g.drawImage(x, y, DRAWING_WIDTH, DRAWING_HEIGHT, this.writableImageToDraw);
            } break;
            case FLIP_COLORS: {
                g.flipColors(x, y, DRAWING_WIDTH, DRAWING_HEIGHT);
            } break;
            default:
                throw new IllegalArgumentException("" + method);
        }
        
        if (MUST_USE_TRANSFORM) {
            g.removeLastAddedTransform();
        }
    }
    
    private static void copyDrawnRectPixelToTheRight(
            InterfaceBwdGraphics g,
            int x,
            int y) {
        
        // To cover both overlapping figures.
        final int totalDrawingHeight = 3 * CELL_HEIGHT_DIV_4;
        
        for (int j = 0; j < totalDrawingHeight; j++) {
            for (int i = 0; i < DRAWING_WIDTH; i++) {
                final int xFrom = x + i;
                final int yFrom = y + j;
                final int argb32 = g.getArgb32At(xFrom, yFrom);
                final int xTo = xFrom + DRAWING_WIDTH + 1;
                final int yTo = yFrom;
                g.setArgb32(argb32);
                g.drawPoint(xTo, yTo);
            }
        }
    }
}
