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

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

public class ImageScaling_s1_i05_BwdTestCase extends AbstractImageScalingBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final String IMG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ImageScaling_s1_i05_BwdTestCase() {
    }

    public ImageScaling_s1_i05_BwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ImageScaling_s1_i05_BwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ImageScaling_s1_i05_BwdTestCase(this.getBinding());
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected InterfaceBwdImage newImageToDraw() {
        
        final InterfaceBwdImage baseImage =
            this.getBinding().newImage(IMG_FILE_PATH);
        final int w = baseImage.getWidth();
        final int h = baseImage.getHeight();
        
        final InterfaceBwdWritableImage wi =
            this.getBinding().newWritableImage(2 * w, 2 * h);
        final InterfaceBwdGraphics wig = wi.getGraphics();
        wig.drawImage(0, 0, baseImage);
        wig.drawImage(w, 0, baseImage);
        wig.drawImage(0, h, baseImage);
        wig.drawImage(w, h, baseImage);
        
        baseImage.dispose();
        
        return wi;
    }

    @Override
    protected double getBaseImageScaling() {
        return 0.5;
    }
}
