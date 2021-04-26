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
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;

/**
 * To test g.clearRect(...) behavior, depending on alpha,
 * and on whether it's called on a client or an image graphics.
 */
public class ClearBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final BwdColor TEXT_BG_COLOR = BwdColor.WHITE;
    private static final BwdColor TEXT_FG_COLOR = BwdColor.BLACK;
    
    private static final int ARGB_32_WALPHA = 0x7FAABBCC;
    private static final int ARGB_32_OPAQUE = Argb32.toOpaque(ARGB_32_WALPHA);
    
    private static final int CELL_WIDTH = 200;
    private static final int CELL_HEIGHT = 70;
    
    private static final int INITIAL_WIDTH = 2 * CELL_WIDTH;
    private static final int INITIAL_HEIGHT = 4 * CELL_HEIGHT;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    private static final String BG_IMAGE_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_ALPHA_PNG;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdImage bgImage;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ClearBwdTestCase() {
    }
    
    public ClearBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ClearBwdTestCase(binding);
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

        if (this.bgImage == null) {
            this.bgImage = this.getBinding().newImage(BG_IMAGE_FILE_PATH);
        }
        
        /*
         * Using a transparent image as background,
         * to be able to check:
         * - The default background of the client, when not cleared or drawn.
         * - The eventual transparency of image clearing.
         */
        
        // Only covering cells.
        g.drawImage(
                GRect.valueOf(0, 0, INITIAL_WIDTH, INITIAL_HEIGHT),
                this.bgImage);
        
        /*
         * 
         */
        
        // {client,image}
        for (int i = 0; i < 2; i++) {
            final boolean onImage = (i == 1);
            
            /*
             * No clear at the bottom, where BG image is also transparent,
             * so that we can see non-cleared-neither-drawn color.
             */
            // {clear opaque, clear with alpha, no clear}
            for (int j = 0; j < 4; j++) {
                final int cellX = i * CELL_WIDTH;
                final int cellY = j * CELL_HEIGHT;
                
                final boolean withClear = (j <= 2);
                final boolean withAlpha = (j == 1);
                final boolean fullyTransparent = (j == 2);
                
                final InterfaceBwdWritableImage image;
                final InterfaceBwdGraphics graphics;
                if (onImage) {
                    image = this.getBinding().newWritableImage(
                            CELL_WIDTH + j * 0,
                            CELL_HEIGHT);
                    graphics = image.getGraphics();
                } else {
                    image = null;
                    graphics = g;
                }
                
                final int rectX = (onImage ? 0 : cellX);
                final int rectY = (onImage ? 0 : cellY);

                final String text;
                if (withClear) {
                    final int argb32 = (fullyTransparent ? 0 : (withAlpha ? ARGB_32_WALPHA : ARGB_32_OPAQUE));
                    text = (onImage ? "ig" : "cg") + ".clearRect(" + Argb32.toString(argb32) + ")";
                    
                    if (onImage) {
                        /*
                         * First clearing in full yellow, to check that
                         * subsequent clearing won't blend with it
                         * but will replace it.
                         * ===> We should never see this yellow on screen.
                         */
                        graphics.setColor(BwdColor.YELLOW);
                        graphics.clearRect(rectX, rectY, CELL_WIDTH, CELL_HEIGHT);
                    } else {
                        // Clear will use opaque version of current color.
                    }
                    graphics.setArgb32(argb32);
                    graphics.clearRect(rectX, rectY, CELL_WIDTH, CELL_HEIGHT);
                } else {
                    text = (onImage ? "ig" : "cg") + " (no clear)";
                }
                
                BwdTestUtils.drawTextAndBg(
                        graphics, TEXT_BG_COLOR, TEXT_FG_COLOR,
                        rectX, rectY, text);
                
                if (onImage) {
                    g.drawImage(cellX, cellY, image);
                    image.dispose();
                }
            }
        }
        
        /*
         * 
         */
        
        return null;
    }
}
