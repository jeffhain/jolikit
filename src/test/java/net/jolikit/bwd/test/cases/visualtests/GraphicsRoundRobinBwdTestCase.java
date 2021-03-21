/*
 * Copyright 2021 Jeff Hain
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
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * To test that multiple graphics (parent/child/sibling,
 * on client and on a writable image)
 * can be used in an interleaved way
 * (g1/.../gn.init(), _use_them_all_, g1/.../gn.finish()).
 */
public class GraphicsRoundRobinBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final BwdColor CLIENT_BG_COLOR = BwdColor.BLACK;
    private static final BwdColor BG_COLOR = BwdColor.valueOfArgb32(0xFF404040);
    private static final BwdColor[] COLOR_ARR = new BwdColor[] {
        BwdColor.valueOfArgb32(0xFFFF0000),
        BwdColor.valueOfArgb32(0xFF00FF00),
        BwdColor.valueOfArgb32(0xFF0000FF),
        BwdColor.valueOfArgb32(0xFF808000),
        BwdColor.valueOfArgb32(0xFF008080),
        BwdColor.valueOfArgb32(0xFF800080),
    };
    
    private static final int FIG_WIDTH = 15;
    private static final int FIG_HEIGHT = 10;
    
    private static final int[] FONT_HEIGHT_ARR = new int[] {
        16, 20, 24, 28, 32, 36,
    };
    
    private static final int INITIAL_WIDTH = (COLOR_ARR.length * 100);
    private static final int INITIAL_HEIGHT = 240;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final String IMG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG;
    
    private InterfaceBwdFont[] fontArr;
    
    private InterfaceBwdImage image;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public GraphicsRoundRobinBwdTestCase() {
    }
    
    public GraphicsRoundRobinBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new GraphicsRoundRobinBwdTestCase(binding);
    }
    
    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new GraphicsRoundRobinBwdTestCase(this.getBinding());
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
        final InterfaceBwdFontHome fontHome = binding.getFontHome();
        
        InterfaceBwdFont[] fontArr = this.fontArr;
        if (fontArr == null) {
            fontArr = new InterfaceBwdFont[FONT_HEIGHT_ARR.length];
            for (int i = 0; i < FONT_HEIGHT_ARR.length; i++) {
                fontArr[i] =
                    fontHome.newFontWithFloorElseClosestHeight(
                        FONT_HEIGHT_ARR[i]);
            }
            this.fontArr = fontArr;
        }
        
        InterfaceBwdImage image = this.image;
        if (image == null) {
            image = binding.newImage(IMG_FILE_PATH);
            this.image = image;
        }
        
        /*
         * Boxes.
         */
        
        final GRect clientBox = g.getBox();
        final int widthSixth = clientBox.xSpan() / 6;
        final int widthHalf = 3 * widthSixth;
        
        // Need the whole client part box for parent, since it clips children.
        final GRect cliParentBox = clientBox.withXSpan(widthHalf);
        final GRect cliChild1Box =
            cliParentBox.withXSpan(widthSixth).withPosDeltas(widthSixth, 0);
        final GRect cliChild2Box = cliChild1Box.withPosDeltas(widthSixth, 0);
        
        // Need the whole writable image part box for parent, since it clips children.
        final GRect wiParentBox = clientBox.withPos(0, 0).withXSpan(widthHalf);
        final GRect wiChild1Box =
            wiParentBox.withXSpan(widthSixth).withPosDeltas(widthSixth, 0);
        final GRect wiChild2Box = wiChild1Box.withPosDeltas(widthSixth, 0);
        
        /*
         * Client graphics.
         */
        
        final InterfaceBwdGraphics cliGp = g.newChildGraphics(clientBox);
        
        final List<InterfaceBwdGraphics> gList =
            new ArrayList<InterfaceBwdGraphics>();
        
        final InterfaceBwdGraphics cliParent = cliGp.newChildGraphics(cliParentBox);
        gList.add(cliParent);
        
        cliGp.init();
        // parent and its children must remain functional
        // even if cliGp is finished here.
        cliGp.finish();
        
        gList.add(cliParent.newChildGraphics(cliChild1Box));
        gList.add(cliParent.newChildGraphics(cliChild2Box));
        
        /*
         * Writable image graphics.
         */
        
        final InterfaceBwdWritableImage wi =
            binding.newWritableImage(
                wiParentBox.xSpan(),
                wiParentBox.ySpan());
        
        final InterfaceBwdGraphics wigGp = wi.getGraphics();

        final InterfaceBwdGraphics wiParent = wigGp.newChildGraphics(wiParentBox);
        gList.add(wiParent);

        wigGp.init();
        // parent and its children must remain functional
        // even if wigGp is finished here.
        wigGp.finish();
        
        gList.add(wiParent.newChildGraphics(wiChild1Box));
        gList.add(wiParent.newChildGraphics(wiChild2Box));
        
        /*
         * One (clip, transform, color, font) configuration
         * for each graphic.
         */
        
        final GRect[] clipArr =
            new GRect[] {
                cliParentBox.withXSpan(widthSixth),
                cliChild1Box,
                cliChild2Box,
                //
                wiParentBox.withXSpan(widthSixth),
                wiChild1Box,
                wiChild2Box,
        };
        final GTransform[] transformArr =
            new GTransform[] {
                GTransform.valueOf(0, 0, 0),
                GTransform.valueOf(0, widthSixth, 0),
                GTransform.valueOf(0, 2*widthSixth, 0),
                //
                GTransform.valueOf(0, 0, 0),
                GTransform.valueOf(0, widthSixth, 0),
                GTransform.valueOf(0, 2*widthSixth, 0),
        };
        
        /*
         * Clearing the whole client area,
         * including part not covered by sub graphics
         * (due to client width being potentially larger
         * than the sum of sub graphics widths).
         */
        
        g.setColor(CLIENT_BG_COLOR);
        g.clearRect(g.getBox());
        
        /*
         * Drawing on graphics in the list.
         */
        
        for (InterfaceBwdGraphics gg : gList) {
            gg.init();
        }
        try {
            paintRoundRobin(
                clientBox,
                //
                clipArr,
                transformArr,
                COLOR_ARR,
                fontArr,
                //
                image,
                //
                gList);
        } finally {
            for (InterfaceBwdGraphics gg : gList) {
                gg.finish();
            }
        }
        
        /*
         * Drawing and disposing writable image.
         */
        
        g.drawImage(widthHalf, clientBox.y(), wi);
        
        wi.dispose();
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void paintRoundRobin(
        GRect clientBox,
        //
        GRect[] clipArr,
        GTransform[] transformArr,
        BwdColor[] colorArr,
        InterfaceBwdFont[] fontArr,
        //
        InterfaceBwdImage image,
        //
        List<InterfaceBwdGraphics> gList) {
        
        final int size = gList.size();
        for (int i = 0; i < size; i++) {
            final InterfaceBwdGraphics g = gList.get(i);
            g.addClipInBase(clipArr[i]);
            g.setTransform(transformArr[i]);
            g.setColor(colorArr[i]);
            g.setFont(fontArr[i]);
        }
        
        /*
         * Clearing BG.
         */
        
        for (InterfaceBwdGraphics g : gList) {
            final BwdColor oldColor = g.getColor();
            g.setColor(BG_COLOR);
            g.clearRect(g.getBox().withPos(0, 0));
            g.setColor(oldColor);
        }
        
        /*
         * Drawing figures.
         */
        
        int tmpY = clientBox.y();
        
        final int figWidth = FIG_WIDTH;
        final int figHeight = FIG_HEIGHT;
        int k = 0;
        MethodLoop : while (true) {
            final GRect rect =
                GRect.valueOf(0, tmpY, figWidth, figHeight);
            for (InterfaceBwdGraphics g : gList) {
                switch (k) {
                    case 0: {
                        final int minSpan = Math.min(figWidth, figHeight);
                        for (int i = 0; i < minSpan; i++) {
                            g.drawPoint(rect.x() + i, rect.y() + i);
                        }
                    } break;
                    case 1: {
                        g.drawLine(rect.x(), rect.y(), rect.xMax(), rect.y());
                    } break;
                    case 2: {
                        g.drawLineStipple(
                            rect.x(), rect.y(), rect.xMax(), rect.y(),
                            1, (short) 0xAAAA, 0);
                    } break;
                    case 3: {
                        g.drawRect(rect);
                    } break;
                    case 4: {
                        g.fillRect(rect);
                    } break;
                    case 5: {
                        g.drawOval(rect);
                    } break;
                    case 6: {
                        g.fillOval(rect);
                    } break;
                    case 7: {
                        g.drawArc(rect, 0.0, 270.0);
                    } break;
                    case 8: {
                        g.fillArc(rect, 0.0, 270.0);
                    } break;
                    case 9: {
                        g.drawPolygon(
                            new int[] {rect.x(), rect.xMax(), rect.xMax(), rect.x()},
                            new int[] {rect.y(), rect.y(), rect.yMax(), rect.yMax()},
                            4);
                    } break;
                    case 10: {
                        g.fillPolygon(
                            new int[] {rect.x(), rect.xMax(), rect.xMax(), rect.x()},
                            new int[] {rect.y(), rect.y(), rect.yMax(), rect.yMax()},
                            4);
                    } break;
                    default: {
                        // No more painting method to use.
                        break MethodLoop;
                    }
                }
            }
            k++;
            tmpY += (figHeight + 1);
        }
        
        /*
         * Drawing text.
         */
        
        for (InterfaceBwdGraphics g : gList) {
            g.drawText(0, tmpY, "text");
        }
        
        tmpY += (int) (FONT_HEIGHT_ARR[FONT_HEIGHT_ARR.length - 1] + 1);
        
        /*
         * Drawing image.
         */
        
        for (InterfaceBwdGraphics g : gList) {
            final int span = g.getClipInBase().xSpan();
            final GRect imgRect =
                GRect.valueOf(0, tmpY, span, span);
            g.drawImage(imgRect, image);
        }
    }
}
