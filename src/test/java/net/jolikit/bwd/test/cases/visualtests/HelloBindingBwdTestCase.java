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

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

public class HelloBindingBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int TARGET_FONT_HEIGHT = 26;
    
    private static final String IMG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG;
    
    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdFont font;
    
    private InterfaceBwdImage image;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public HelloBindingBwdTestCase() {
    }

    public HelloBindingBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new HelloBindingBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new HelloBindingBwdTestCase(this.getBinding());
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

        final GRect box = g.getBox();

        final int x = box.x();
        final int xSpan = box.xSpan();
        final int ySpan = box.ySpan();
        
        final InterfaceBwdBinding binding = this.getBinding();
        
        /*
         * 
         */
        
        g.setColor(BwdColor.WHITE);
        g.fillRect(box);
        
        /*
         * 
         */
        
        InterfaceBwdImage image = this.image;
        if (image == null) {
            image = binding.newImage(IMG_FILE_PATH);
            this.image = image;
        }
        
        {
            final int iw = image.getWidth();
            final int ih = image.getHeight();
            g.drawImage(
                    x, box.yMid(), xSpan / 2, ySpan / 2,
                    image,
                    iw / 10, ih / 5, iw / 3, ih / 3);
        }
        
        /*
         * 
         */
        
        g.setColor(BwdColor.BLUE.withAlphaFp(0.25));
        g.fillOval(
                box.xMid(),
                box.yMid(),
                xSpan / 2,
                ySpan / 2);
        
        g.setColor(BwdColor.RED.withAlphaFp(0.5));
        g.fillRect(
                box.xMid() + xSpan / 4,
                box.yMid() - ySpan / 4,
                xSpan / 8,
                ySpan / 2);
        
        g.setColor(BwdColor.GREEN.withAlphaFp(0.5));
        g.addTransform(GTransform.valueOf(
                0,
                (int) (box.xMid() * 1.5),
                (int) (box.yMid() * 1.5)));
        final int xMaxRadius = box.xSpan() / 4;
        final int yMaxRadius = box.ySpan() / 4;
        fillSpiralPolygon(g, xMaxRadius, yMaxRadius);
        g.removeLastAddedTransform();
        
        /*
         * 
         */

        final String[] strArr = this.getStrArr();

        InterfaceBwdFont font = this.font;
        if (font == null) {
            font = binding.getFontHome().newFontWithClosestHeight(
                    g.getFont().kind(),
                    TARGET_FONT_HEIGHT);
            this.font = font;
        }
        
        g.setFont(font);
        
        final int yJump = font.metrics().height();
        final GRect cb = computeCenteredBox(box, font, yJump, strArr);
        int tmpY = 0;
        
        g.setColor(BwdColor.WHITE.withAlphaFp(0.75));
        g.fillRect(cb.x(), tmpY, cb.xSpan(), cb.ySpan());
        
        g.setColor(BwdColor.BLACK);
        for (String str : strArr) {
            g.drawText(cb.x(), tmpY, str);
            tmpY += yJump;
        }
        
        /*
         * 
         */
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static GRect computeCenteredBox(
            GRect box,
            InterfaceBwdFont font,
            int yJump,
            String[] strArr) {
        int xSpan = 0;
        for (String str : strArr) {
            final int length = font.metrics().computeTextWidth(str);
            xSpan = Math.max(xSpan, length);
        }
        
        final int ySpan = strArr.length * yJump;
        
        return GRect.valueOf(
                box.x() + (box.xSpan()  - xSpan) / 2,
                box.y() + (box.ySpan()  - ySpan) / 2,
                xSpan,
                ySpan);
    }

    /**
     * Fills a spiral, centered on (0,0).
     */
    private static void fillSpiralPolygon(
            InterfaceBwdGraphics g,
            int xMaxRadius,
            int yMaxRadius) {
        
        final int pointCount = 30;
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
        g.fillPolygon(
                xArr,
                yArr,
                pointCount);
    }

    private String[] getStrArr() {
        final InterfaceBwdBinding binding = this.getBinding();
        final String bindingCsn = binding.getClass().getSimpleName();
        final String[] strArr;
        if (BwdTestUtils.isAwtBinding(binding)) {
            strArr = new String[]{"AWT"};
        } else if (BwdTestUtils.isSwingBinding(binding)) {
            strArr = new String[]{"Swing"};
        } else if (BwdTestUtils.isJfxBinding(binding)) {
            strArr = new String[]{"JavaFX"};
        } else if (BwdTestUtils.isSwtBinding(binding)) {
            strArr = new String[]{
                    "SWT (+FontBox)"};
        } else if (BwdTestUtils.isLwjglBinding(binding)) {
            strArr = new String[]{
                    "LWJGL3",
                    "AWT (fonts)"};
        } else if (BwdTestUtils.isJoglBinding(binding)) {
            strArr = new String[]{
                    "JOGL",
                    "NEWT (windowing)",
                    "AWT (fonts, images)"};
        } else if (BwdTestUtils.isQtjBinding(binding)) {
            strArr = new String[]{"QtJambi4"};
        } else if (BwdTestUtils.isAlgrBinding(binding)) {
            strArr = new String[]{"Allegro5"};
        } else if (BwdTestUtils.isSdlBinding(binding)) {
            strArr = new String[]{"SDL2"};
        } else {
            throw new AssertionError("" + bindingCsn);
        }

        return strArr;
    }
}
