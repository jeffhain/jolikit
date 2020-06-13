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
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

public class ImageFormatsBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int IMG_WIDTH = 41;
    private static final int IMG_HEIGHT = 21;
    
    private static final String[] IMAGE_PATH_ARR = new String[]{
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_BMP_01_BIT,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_BMP_04_BIT,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_BMP_08_BIT,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_BMP,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_GIF,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_JPG,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PGM,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PNG,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PPM,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_TGA,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_TIF,
    };

    private static final int INITIAL_WIDTH = 400;
    private static final int INITIAL_HEIGHT = IMG_HEIGHT * IMAGE_PATH_ARR.length;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    /*
     * 
     */
    
    private InterfaceBwdImage[] imageByPathIndex;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ImageFormatsBwdTestCase() {
    }
    
    public ImageFormatsBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ImageFormatsBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ImageFormatsBwdTestCase(this.getBinding());
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
        
        /*
         * 
         */
        
        g.setColor(BwdColor.WHITE);
        g.clearRect(box);
        
        /*
         * 
         */

        if (this.imageByPathIndex == null) {
            final InterfaceBwdImage[] imgArr = new InterfaceBwdImage[IMAGE_PATH_ARR.length];
            this.imageByPathIndex = imgArr;
            for (int i = 0; i < IMAGE_PATH_ARR.length; i++) {
                final String imgPath = IMAGE_PATH_ARR[i];
                
                InterfaceBwdImage image = null;
                try {
                    image = binding.newImage(imgPath);
                } catch (IllegalArgumentException e) {
                    System.out.println("image format not supported: " + e.getMessage());
                }
                
                imgArr[i] = image;
            }
        }
        
        /*
         * 
         */
        
        g.setColor(BwdColor.BLACK);
        
        int currentY = 0;
        
        for (int i = 0; i < IMAGE_PATH_ARR.length; i++) {
            final String imgPath = IMAGE_PATH_ARR[i];
            final InterfaceBwdImage image = this.imageByPathIndex[i];
            
            final String imgName = imgPath.substring(imgPath.lastIndexOf("/") + 1);
            
            if (image != null) {
                g.drawImage(0, currentY, image);
                
                g.drawText(IMG_WIDTH + 10, currentY, "OK: " + imgName);
            } else {
                g.drawText(0, currentY, "KO: " + imgName);
            }
            
            currentY += IMG_HEIGHT;
        }
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
