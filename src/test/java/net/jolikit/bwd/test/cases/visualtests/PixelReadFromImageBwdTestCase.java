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
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * To test image's getArgb32At(x,y) method.
 */
public class PixelReadFromImageBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int INITIAL_WIDTH = 200;
    private static final int INITIAL_HEIGHT = 100;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    private static final String IMAGE_GREY_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_GREY_PNG;
    private static final String IMAGE_TRANSP_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_ALPHA_PNG;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdImage imageGrey;
    private InterfaceBwdImage imageTransparent;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public PixelReadFromImageBwdTestCase() {
    }
    
    public PixelReadFromImageBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new PixelReadFromImageBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new PixelReadFromImageBwdTestCase(this.getBinding());
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
        
        /*
         * 
         */
        
        if (this.imageGrey == null) {
            this.imageGrey = this.getBinding().newImage(IMAGE_GREY_FILE_PATH);
        }
        if (this.imageTransparent == null) {
            this.imageTransparent = this.getBinding().newImage(IMAGE_TRANSP_FILE_PATH);
        }

        // Filling with some non-pathological color.
        g.setArgb32(0xFF4080C0);
        g.fillRect(box);

        g.drawImage(
                box.x(),
                box.y(),
                this.imageGrey);
        
        drawImagePixelByPixel(
                g,
                box.xMid(),
                box.y(),
                this.imageGrey);

        g.drawImage(
                box.x(),
                box.yMid(),
                this.imageTransparent);
        
        drawImagePixelByPixel(
                g,
                box.xMid(),
                box.yMid(),
                this.imageTransparent);
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Must have the same effect than just using g.drawImage(x, y, image).
     */
    private static void drawImagePixelByPixel(
            InterfaceBwdGraphics g,
            int x,
            int y,
            InterfaceBwdImage image) {
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                final int argb32 = image.getArgb32At(i, j);
                g.setArgb32(argb32);
                g.drawPoint(x + i, y + j);
            }
        }
    }
}
