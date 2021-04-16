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

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

public class ImageClippingCliBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final String IMG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_ALPHA_PNG;
    
    private static final int INITIAL_WIDTH = 650;
    private static final int INITIAL_HEIGHT = 650;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdImage image;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ImageClippingCliBwdTestCase() {
    }
    
    public ImageClippingCliBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ImageClippingCliBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ImageClippingCliBwdTestCase(this.getBinding());
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

        InterfaceBwdImage image = this.image;
        if (image == null) {
            image = binding.newImage(IMG_FILE_PATH);
            this.image = image;
        }

        g.setColor(BwdColor.GOLDENROD);
        g.fillRect(box);

        final GRect clip = box.withBordersDeltasElseEmpty(
            box.xSpan() / 5,
            box.ySpan() / 5,
            -box.xSpan() / 5,
            -box.ySpan() / 5);
        g.addClipInBase(clip);

        g.setColor(BwdColor.WHITE);
        g.fillRect(box);

        /*
         * Images at center and on client borders.
         */
        
        // Just cat's face, because the image is too large.
        final GRect imageRect = GRect.valueOf(
            130,
            130,
            130,
            130);
        final GPoint imgSpans = GPoint.valueOf(imageRect.xSpan(), imageRect.ySpan());
        // Shrinked, exact.
        final GPoint imgSpansLo1 = GPoint.valueOf(
            imgSpans.x() - imgSpans.x() / 2,
            imgSpans.y() - imgSpans.y() / 2);
        // Shrinked, not exact.
        final GPoint imgSpansLo2 = GPoint.valueOf(
            imgSpans.x() - imgSpans.x() / 3,
            imgSpans.y() - imgSpans.y() / 3);
        // Shrinked, exact.
        final GPoint imgSpansHi1 = GPoint.valueOf(
            2 * imgSpans.x(),
            2 * imgSpans.y());
        // Shrinked, not exact.
        final GPoint imgSpansHi2 = GPoint.valueOf(
            2 * imgSpans.x() + imgSpans.x() / 2,
            2 * imgSpans.y() + imgSpans.y() / 2);
        
        for (GPoint ds : new GPoint[] {
            imgSpansLo2,
            imgSpansLo1,
            imgSpans,
            imgSpansHi1,
            imgSpansHi2,
        }) {
            for (GTransform transform : new GTransform[]{
                // Centered.
                GTransform.valueOf(GRotation.ROT_0, box.xMid(), box.yMid()),
                // Client top.
                GTransform.valueOf(GRotation.ROT_0, box.xMid(), 0),
                // Client right.
                GTransform.valueOf(GRotation.ROT_90, box.xMax(), box.yMid()),
                // Client bottom.
                GTransform.valueOf(GRotation.ROT_180, box.xMid(), box.yMax()),
                // Client left.
                GTransform.valueOf(GRotation.ROT_270, 0, box.yMid()),
            }) {
                g.addTransform(transform);
                
                final GRect dstRect = GRect.valueOf(
                    -ds.x()/2,
                    -ds.y()/2,
                    ds.x(),
                    ds.y());
                final GRect srcRect = imageRect;
                g.drawImage(
                    dstRect,
                    image,
                    srcRect);
                
                g.removeLastAddedTransform();
            }
        }
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
