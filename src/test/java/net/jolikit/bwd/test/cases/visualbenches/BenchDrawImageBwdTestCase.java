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
package net.jolikit.bwd.test.cases.visualbenches;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.test.utils.TestUtils;

/**
 * To bench image drawing.
 */
public class BenchDrawImageBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final String IMAGE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG;
    private static final String ALPHA_IMAGE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_ALPHA_PNG;

    /**
     * To bench resized image drawing.
     */
    private static final double IMAGE_SHRINK_FACTOR = 0.7;
    
    private static final int NBR_OF_CALLS = 100;

    private static final int INITIAL_WIDTH = 1600;
    private static final int INITIAL_HEIGHT = 1150;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * First is opaque, second has alpha.
     */
    private final List<InterfaceBwdImage> imageList = new ArrayList<InterfaceBwdImage>();
    
    private long lastPaintNanos = Long.MAX_VALUE;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public BenchDrawImageBwdTestCase() {
    }

    public BenchDrawImageBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new BenchDrawImageBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new BenchDrawImageBwdTestCase(this.getBinding());
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
        
        final GRect box = g.getBox();
        
        final InterfaceBwdFontHome fontHome = getBinding().getFontHome(); 
        final InterfaceBwdFont defaultFont = fontHome.getDefaultFont();
        
        /*
         * Computing delay since last painting ended.
         * When large, means that the backing library took time digesting
         * previous drawing, which can happen in case of asynchronous
         * text drawing.
         */
        
        final long nanosBetweenPaintings;
        if (this.lastPaintNanos == Long.MAX_VALUE) {
            nanosBetweenPaintings = 0;
        } else {
            nanosBetweenPaintings = System.nanoTime() - this.lastPaintNanos;
        }

        /*
         * Clearing.
         */

        g.setColor(BwdColor.WHITE);
        g.fillRect(box);
        
        /*
         * 
         */
        
        final int dh = defaultFont.fontMetrics().fontHeight() + 1;
        g.setColor(BwdColor.BLACK);
        int y = 0;
        
        {
            g.setFont(defaultFont);
            final String comment =
                    "duration since last painting ended = "
                            + TestUtils.nsToSRounded(nanosBetweenPaintings) + " s";
            g.drawText(0, y, comment);
        }
        
        y += (2 * dh);
        
        /*
         * Ensuring images to use.
         */
        
        if (this.imageList.size() == 0) {
            this.imageList.add(getBinding().newImage(IMAGE_PATH));
            this.imageList.add(getBinding().newImage(ALPHA_IMAGE_PATH));
        }

        /*
         * Benching.
         */
        
        final int nbrOfCalls = NBR_OF_CALLS;
        
        int x = 0;
        
        for (int k = 0; k < this.imageList.size(); k++) {
            final InterfaceBwdImage image = this.imageList.get(k);
            
            final boolean hasAlpha = (k == 1);
            
            for (boolean shrunk : new boolean[]{false,true}) {
                if (shrunk) {
                    y -= (2 * dh + image.getHeight());
                    x = image.getWidth() + 1;
                } else {
                    x = 0;
                }
                
                final int dstXSpan;
                final int dstYSpan;
                if (shrunk) {
                    dstXSpan = (int) (image.getWidth() * IMAGE_SHRINK_FACTOR);
                    dstYSpan = (int) (image.getHeight() * IMAGE_SHRINK_FACTOR);
                } else {
                    dstXSpan = image.getWidth();
                    dstYSpan = image.getHeight();
                }
                
                /*
                 * Drawing a lot.
                 */
                
                y += dh;
                
                final long startNs = System.nanoTime();
                for (int i = 0; i < nbrOfCalls; i++) {
                    g.drawImage(x, y, dstXSpan, dstYSpan, image);
                }
                final long endNs = System.nanoTime();
                
                // If has alpha, adding a final draw after a clear,
                // to avoid over-blended result and make the alpha obvious.
                if (hasAlpha) {
                    g.setColor(BwdColor.WHITE);
                    g.clearRect(x, y, dstXSpan, dstYSpan);
                    g.drawImage(x, y, dstXSpan, dstYSpan, image);
                }
                
                y -= dh;
                
                /*
                 * Drawing how long it took.
                 */
                
                final long dtNs = endNs - startNs;
                g.setFont(defaultFont);
                final String comment =
                        nbrOfCalls + " calls, hasAlpha = " + hasAlpha
                        + ", shrunk = " + shrunk
                        + ", took " + TestUtils.nsToSRounded(dtNs) + " s";
                g.setColor(BwdColor.BLACK);
                g.drawText(x, y, comment);
                
                y += (2 * dh + image.getHeight());
            }
        }
        
        /*
         * 
         */
        
        this.lastPaintNanos = System.nanoTime();
        
        getHost().ensurePendingClientPainting();
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
