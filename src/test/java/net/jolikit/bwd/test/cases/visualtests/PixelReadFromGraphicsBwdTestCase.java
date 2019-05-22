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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.graphics.AbstractBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * To test graphic's getArgb32At(x,y) method.
 */
public class PixelReadFromGraphicsBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int INITIAL_WIDTH = 200;
    private static final int INITIAL_HEIGHT = 100;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    private static final String IMAGE_OPAQUE_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PNG;
    private static final String IMAGE_TRANSP_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_ALPHA_PNG;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdImage imageOpaque;
    private InterfaceBwdImage imageTransparent;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public PixelReadFromGraphicsBwdTestCase() {
    }
    
    public PixelReadFromGraphicsBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new PixelReadFromGraphicsBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new PixelReadFromGraphicsBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final GRect box = g.getBoxInClient();
        
        /*
         * 
         */
        
        if (this.imageOpaque == null) {
            this.imageOpaque = this.getBinding().newImage(IMAGE_OPAQUE_FILE_PATH);
        }
        if (this.imageTransparent == null) {
            this.imageTransparent = this.getBinding().newImage(IMAGE_TRANSP_FILE_PATH);
        }

        /*
         * Drawing on the left half,
         * and will read and draw what was read on the right,
         * without prior clearing (might have undefined alpha,
         * to check that read methods properly read alpha as well).
         */
        
        final GRect leftBox = box.withSpans(box.xSpan() / 2, box.ySpan());
        final GRect rightBox = leftBox.withPosDeltas(leftBox.xSpan(), 0);
        
        final int noClearBorder = 10;
        {
            // Not clearing everything.
            g.setColor(BwdColor.WHITE);
            g.clearRectOpaque(
                    leftBox.x() + noClearBorder,
                    leftBox.y() + noClearBorder,
                    leftBox.xSpan() - 2 * noClearBorder,
                    leftBox.ySpan() - 2 * noClearBorder);
            
            // Drawing with alpha color,
            // partially leaking outside cleared area.
            g.setArgb32(0x804080C0);
            final int noAlphaFillBorder = noClearBorder / 2;
            g.fillRect(
                    leftBox.x() + noAlphaFillBorder,
                    leftBox.y() + noAlphaFillBorder,
                    leftBox.xSpan() - 2 * noAlphaFillBorder,
                    leftBox.ySpan() - 2 * noAlphaFillBorder);
            
            // Top left.
            g.drawImage(
                    leftBox.x(),
                    leftBox.y(),
                    this.imageOpaque);
            
            // Bottom left (with most transparent alpha over non cleared border).
            g.drawImage(
                    leftBox.x(),
                    leftBox.yMax() - this.imageTransparent.getHeight() + 1,
                    this.imageTransparent);
        }
        
        /*
         * Copying drawn pixels on the right, if possible.
         */
        
        if (g instanceof AbstractBwdGraphics) {
            final AbstractBwdGraphics g2 = (AbstractBwdGraphics) g;
            for (int y = leftBox.y(); y <= leftBox.yMax(); y++) {
                for (int x = leftBox.x(); x <= leftBox.xMax(); x++) {
                    final int argb32 = g2.getArgb32At(x, y);
                    g.setArgb32(argb32);
                    g.drawPoint(x + leftBox.xSpan(), y);
                }
            }
        } else {
            g.setColor(BwdColor.WHITE);
            g.clearRectOpaque(
                    rightBox.x(),
                    rightBox.y(),
                    rightBox.xSpan(),
                    rightBox.ySpan());
            g.setColor(BwdColor.BLACK);
            g.drawText(
                    rightBox.x(),
                    rightBox.y(),
                    "no pixel read method on graphics class:");
            g.drawText(
                    rightBox.x(),
                    rightBox.y() + g.getFont().fontMetrics().fontHeight() + 1,
                    g.getClass().toString());
        }
        
        /*
         * Labels.
         */
        
        {
            g.setColor(BwdColor.WHITE);
            g.drawText(
                    leftBox.x() + 2 * noClearBorder,
                    leftBox.y() + leftBox.ySpan() / 2,
                    "Original");
            g.drawText(
                    rightBox.x() + 2 * noClearBorder,
                    rightBox.y() + rightBox.ySpan() / 2,
                    "Copy");
        }
        
        /*
         * 
         */
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
