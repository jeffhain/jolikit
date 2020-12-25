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
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.gprim.GprimUtils;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

public class ClippingBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * Using a quanta to make sure spans are of the form "2^k * some_integer".
     */
    private static final int SPAN_QUANTA = 5;
    
    private static final boolean MUST_BE_HORIZONTAL_MAJOR = true;
    
    private static final int CASE_COUNT = 14;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private interface MyInterfaceDrawer {
        /**
         * Draw something in DRAW_BOX_IN_USER,
         * only the part in CLIP_IN_USER should actually be drawn.
         * 
         * @param g Clip and transform must not be modified.
         */
        public void drawIt(InterfaceBwdGraphics g);
    };
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int CELL_X_SPAN = 8 * SPAN_QUANTA;
    private static final int CELL_Y_SPAN = 4 * SPAN_QUANTA;
    private static final int CELL_MAX_SPAN = Math.max(CELL_X_SPAN, CELL_Y_SPAN);
    
    private static final GRect DRAW_BOX_IN_USER = GRect.valueOf(1, 1, CELL_X_SPAN - 2, CELL_Y_SPAN - 2);
    private static final GRect CLIP_IN_USER = GRect.valueOf(CELL_X_SPAN/4, CELL_Y_SPAN/4, CELL_X_SPAN/2, CELL_Y_SPAN/2);
    private static final GRect ARC_BOX = GRect.valueOf(
            CLIP_IN_USER.x() - 2,
            CLIP_IN_USER.y() - 2,
            CLIP_IN_USER.xSpan() + 4,
            CLIP_IN_USER.ySpan() + 4);

    private static final int ROW_COUNT = MUST_BE_HORIZONTAL_MAJOR ? 4 : CASE_COUNT;
    private static final int COL_COUNT = MUST_BE_HORIZONTAL_MAJOR ? CASE_COUNT : 4;

    // Max span because we align rotated cells.
    private static final int INITIAL_WIDTH = COL_COUNT * CELL_MAX_SPAN;
    private static final int INITIAL_HEIGHT = ROW_COUNT * CELL_MAX_SPAN;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    /*
     * 
     */
    
    private InterfaceBwdImage image;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ClippingBwdTestCase() {
    }
    
    public ClippingBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ClippingBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ClippingBwdTestCase(this.getBinding());
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
        
        /*
         * Drawers.
         */
        
        final List<MyInterfaceDrawer> drawerList = new ArrayList<MyInterfaceDrawer>();
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.clearRect(DRAW_BOX_IN_USER);
            }
        });
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                for (int j = 0; j < DRAW_BOX_IN_USER.ySpan(); j += 3) {
                    for (int i = 0; i < DRAW_BOX_IN_USER.xSpan(); i += 3) {
                        g.drawPoint(
                                DRAW_BOX_IN_USER.x() + i,
                                DRAW_BOX_IN_USER.y() + j);
                    }
                }
            }
        });
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.drawLine(
                        DRAW_BOX_IN_USER.x(),
                        DRAW_BOX_IN_USER.y(),
                        DRAW_BOX_IN_USER.xMax(),
                        DRAW_BOX_IN_USER.yMax());
            }
        });
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.drawLineStipple(
                        DRAW_BOX_IN_USER.x(),
                        DRAW_BOX_IN_USER.y(),
                        DRAW_BOX_IN_USER.xMax(),
                        DRAW_BOX_IN_USER.yMax(),
                        1, GprimUtils.PLAIN_PATTERN, 0);
            }
        });
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.drawRect(
                        DRAW_BOX_IN_USER.x() + DRAW_BOX_IN_USER.xSpan()/2,
                        DRAW_BOX_IN_USER.y() + DRAW_BOX_IN_USER.ySpan()/2,
                        DRAW_BOX_IN_USER.xSpan()/2,
                        DRAW_BOX_IN_USER.ySpan()/2);
            }
        });

        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.fillRect(DRAW_BOX_IN_USER);
            }
        });
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.drawOval(ARC_BOX);
            }
        });
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.fillOval(ARC_BOX);
            }
        });

        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.drawArc(ARC_BOX, 0, 270);
            }
        });

        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.fillArc(ARC_BOX, 0, 270);
            }
        });
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                final InterfaceBwdFont oldFont = g.getFont();
                
                final int fontSize = CELL_Y_SPAN;
                final InterfaceBwdFontHome fontHome = getBinding().getFontHome();
                final InterfaceBwdFont font = fontHome.newFontWithSize(fontSize);
                try {
                    g.setFont(font);

                    final String text = "BOBOB";
                    /*
                     * Using multiple shifts, to help
                     * have glyphs cut all sides.
                     */
                    for (int shift : new int[]{
                            -CELL_Y_SPAN/4,
                            0,
                            CELL_Y_SPAN/4
                    }) {
                        g.drawText(
                                DRAW_BOX_IN_USER.x() + shift,
                                DRAW_BOX_IN_USER.y() + shift,
                                text);
                    }
                } finally {
                    g.setFont(oldFont);
                    font.dispose();
                }
            }
        });

        InterfaceBwdImage img = this.image;
        if (img == null) {
            img = this.getBinding().newImage(BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PNG);
            this.image = img;
        }
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.drawImage(
                        DRAW_BOX_IN_USER.x(),
                        DRAW_BOX_IN_USER.y(),
                        image);
            }
        });
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                g.drawImage(DRAW_BOX_IN_USER, image);
            }
        });
        
        drawerList.add(new MyInterfaceDrawer() {
            @Override
            public void drawIt(InterfaceBwdGraphics g) {
                final GRect sRect = GRect.valueOf(0, 0, image.getWidth()/2, image.getHeight()/2);
                g.drawImage(DRAW_BOX_IN_USER, image, sRect);
            }
        });

        /*
         * Painting using the drawers.
         */

        final GRect box = g.getBox();
        final int x = box.x();
        final int y = box.y();

        g.setColor(BwdColor.WHITE);
        g.fillRect(box);

        int i = 0;
        int j = 0;
        for (MyInterfaceDrawer drawer : drawerList) {
            for (GRotation rotation : GRotation.values()) {
                final int cellX = x + CELL_MAX_SPAN * j;
                final int cellY = y + CELL_MAX_SPAN * i;

                g.addTransform(GTransform.valueOf(rotation, cellX, cellY, CELL_MAX_SPAN, CELL_MAX_SPAN));
                {
                    // Black rectangle on inside border of the cell,
                    // drawing supposed to be done strictly in this rectangle.
                    g.setColor(BwdColor.BLACK);
                    g.drawRect(0, 0, CELL_X_SPAN, CELL_Y_SPAN);

                    // Gray rectangle just outside clip, to help figure out where it is.
                    g.setColor(BwdColor.GRAY);
                    g.drawRect(
                            CLIP_IN_USER.x() - 1,
                            CLIP_IN_USER.y() - 1,
                            CLIP_IN_USER.xSpan() + 2,
                            CLIP_IN_USER.ySpan() + 2);
                    
                    g.addClipInUser(CLIP_IN_USER);

                    g.setColor(BwdColor.BLACK);
                    drawer.drawIt(g);
                }
                g.removeLastAddedClip();
                g.removeLastAddedTransform();

                final int newI = nextI(i, j);
                final int newJ = nextJ(i, j);
                i = newI;
                j = newJ;
            }
        }
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static int nextI(int i, int j) {
        if (MUST_BE_HORIZONTAL_MAJOR) {
            if (i < 3) {
                return i + 1;
            } else {
                return 0;
            }
        } else {
            if (j < 3) {
                return i;
            } else {
                return i + 1;
            }
        }
    }
    
    private static int nextJ(int i, int j) {
        if (MUST_BE_HORIZONTAL_MAJOR) {
            if (i < 3) {
                return j;
            } else {
                return j + 1;
            }
        } else {
            if (j < 3) {
                return j + 1;
            } else {
                return 0;
            }
        }
    }
}
