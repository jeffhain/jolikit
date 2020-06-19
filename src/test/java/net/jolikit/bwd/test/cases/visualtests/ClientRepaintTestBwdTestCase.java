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
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * Mock to test painting and re-painting (making all dirty + painting).
 * 
 * Makes it easy to figure out whether/when/how last painting of client
 * did take place, by painting cornered images, an eye pointing
 * to screen center, and paint count.
 */
public class ClientRepaintTestBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * For client mock part.
     */
    
    private InterfaceBwdImage image;
    
    private int paintCount = 0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ClientRepaintTestBwdTestCase() {
    }

    public ClientRepaintTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ClientRepaintTestBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ClientRepaintTestBwdTestCase(this.getBinding());
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
        
        final ArrayList<GRect> paintedRectList = new ArrayList<GRect>();
        if (dirtyRect.isEmpty()) {
            /*
             * Nothing dirty: painting nothing.
             */
            return paintedRectList;
        } else {
            /*
             * Dirty painting : we only paint, and indicate to have painted,
             * the dirty parts, to check that the binding properly computes
             * them when full repaint is needed (during or after moves,
             * during or after resizes, after show-up, and after focus gain
             * or focus loss).
             */
            g.addClipInBase(dirtyRect);
            paintedRectList.add(dirtyRect);
        }
        
        /*
         * 
         */

        final GRect box = g.getBox();
        
        /*
         * Background.
         */
        
        {
            g.setColor(BwdColor.WHITE);
            g.fillRect(box);
        }
        
        /*
         * Cornered images.
         */
        
        {
            InterfaceBwdImage image = this.image;
            if (image == null) {
                final String filePath = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_BMP;
                image = this.getBinding().newImage(filePath);
                this.image = image;
            }

            final int x0 = box.x();
            final int x1 = box.xMax() - image.getWidth() + 1;

            final int y0 = box.y();
            final int y1 = box.yMax() - image.getHeight() + 1;

            // Top-left.
            g.drawImage(x0, y0, image);
            
            // Top-right.
            g.drawImage(x1, y0, image);
            
            // Bottom-left.
            g.drawImage(x0, y1, image);
            
            // Bottom-right.
            g.drawImage(x1, y1, image);
        }
        
        /*
         * Center-fixing centered eye.
         */

        {
            final int minBoxSpan = box.minSpan();
            final int boxCenterX = (box.xMax() - box.x()) / 2;
            final int boxCenterY = (box.yMax() - box.y()) / 2;
            
            final int eyeRadius = minBoxSpan / 4;
            final int eyeInternalRadius = eyeRadius / 4;
            final int eyeMaxInternalDelta = eyeRadius - 2 * eyeInternalRadius;
            
            g.setColor(BwdColor.GRAY);
            fillCircle(g, boxCenterX, boxCenterY, eyeRadius);

            final GRect screenBounds = this.getBinding().getScreenBounds();
            final int screenCenterXInScreen = (screenBounds.xMax() - screenBounds.x()) / 2;
            final int screenCenterYInScreen = (screenBounds.yMax() - screenBounds.y()) / 2;
            
            final GRect clientBounds = this.getHost().getClientBounds();
            final int screenCenterXInClient = screenCenterXInScreen - clientBounds.x();
            final int screenCenterYInClient = screenCenterYInScreen - clientBounds.y();
            
            final int boxCenterToScreenCenterDx = screenCenterXInClient - boxCenterX;
            final int boxCenterToScreenCenterDy = screenCenterYInClient - boxCenterY;
            final int boxCenterToScreenCenterDelta = (int) Math.sqrt(
                    pow2(boxCenterToScreenCenterDx)
                    + pow2(boxCenterToScreenCenterDy));
            final double angRad = Math.atan2(boxCenterToScreenCenterDy, boxCenterToScreenCenterDx);
            
            final int eyeInternalDelta = Math.min(eyeMaxInternalDelta, boxCenterToScreenCenterDelta);
            final int eyeInternalDx = BindingCoordsUtils.roundToInt(eyeInternalDelta * Math.cos(angRad));
            final int eyeInternalDy = BindingCoordsUtils.roundToInt(eyeInternalDelta * Math.sin(angRad));
            
            g.setColor(BwdColor.BLACK);
            fillCircle(
                    g,
                    boxCenterX + eyeInternalDx,
                    boxCenterY + eyeInternalDy,
                    eyeInternalRadius);
        }
        
        /*
         * Paint count.
         */
        
        {
            g.setColor(BwdColor.RED);
            
            final InterfaceBwdFontHome fontHome = this.getBinding().getFontHome();
            final InterfaceBwdFont font = fontHome.getDefaultFont();
            final InterfaceBwdFontMetrics fontMetrics = font.fontMetrics();
            
            final int pseudoMaxTextWidth = fontMetrics.computeTextWidth("paintCount = 999");
            int x = (box.xMax() - box.x() - pseudoMaxTextWidth) / 2;
            int y = 0;

            this.paintCount++;
            final String paintCountStr = "paintCount = " + this.paintCount;
            g.drawText(x, y, paintCountStr);
        }
        
        return paintedRectList;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static double pow2(double value) {
        return value * value;
    }
    
    private static void fillCircle(
            InterfaceBwdGraphics g,
            int centerX,
            int centerY,
            int radius) {
        final int diameter = 2 * radius + 1;
        g.fillOval(
                centerX - radius, centerY - radius,
                diameter, diameter);
    }
}
