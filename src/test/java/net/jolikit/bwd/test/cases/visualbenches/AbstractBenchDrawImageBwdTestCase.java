/*
 * Copyright 2019-2021 Jeff Hain
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
import net.jolikit.lang.NbrsUtils;
import net.jolikit.test.utils.TestUtils;

/**
 * To bench image drawing, at with different scalings
 * (of image, not binding).
 */
public abstract class AbstractBenchDrawImageBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Image spans : 960x540
     */
    private static final String IMAGE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG;
    private static final String ALPHA_IMAGE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_ALPHA_PNG;

    private static final int NBR_OF_CALLS = 100;
    
    private static final int DST_W = 300;
    private static final int DST_H = 170;

    private static final int CELL_W = 310;
    private static final int CELL_H = 200;

    /**
     * 1 (not scaled: should be quick)
     * 2 (exact growth: should be quick (closest algo))
     * 0.5 (should be quick with closest algo, slower with sampling algo)
     * 0.3 and 3.1 (not exact: should use sampling algo)
     */
    private static final double[] SCALE_ARR = new double[] {
        0.32, 0.5, 1.0, 2.0, 3.1,
    };

    private static final int INITIAL_WIDTH = CELL_W * SCALE_ARR.length;
    private static final int INITIAL_HEIGHT = CELL_H * 2 + 50;
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

    public AbstractBenchDrawImageBwdTestCase() {
    }

    public AbstractBenchDrawImageBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    protected abstract boolean getMustEnsureAccurateImageScaling();
    
    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        g.setAccurateImageScaling(this.getMustEnsureAccurateImageScaling());
        
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
        
        int tmpX = box.x();
        int tmpY = box.y();
        
        final int textH = defaultFont.metrics().height() + 1;
        g.setColor(BwdColor.BLACK);
        
        {
            g.setFont(defaultFont);
            {
                final String comment =
                    "duration since last painting ended = "
                            + TestUtils.nsToSRounded(nanosBetweenPaintings) + " s";
                g.drawText(tmpX, tmpY, comment);
                tmpY += textH;
            }
            {
                final String comment =
                    NBR_OF_CALLS + " draw calls for each case";
                g.drawText(tmpX, tmpY, comment);
                tmpY += textH;
            }
        }
        
        tmpY += textH;
        
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
        
        for (int k = 0; k < this.imageList.size(); k++) {
            final InterfaceBwdImage image = this.imageList.get(k);
            final int imgWidth = image.getWidth();
            final int imgHeight = image.getHeight();
            
            final boolean hasAlpha = (k == 1);
            
            for (double scale : SCALE_ARR) {
                final int srcWidth = NbrsUtils.roundToInt(DST_W / scale);
                final int srcHeight = NbrsUtils.roundToInt(DST_H / scale);
                NbrsUtils.requireInfOrEq(imgWidth, srcWidth, "srcWidth");
                NbrsUtils.requireInfOrEq(imgHeight, srcHeight, "srcHeight");
                
                // Centering src rectangle in image.
                final int srcX = (imgWidth - srcWidth) / 2;
                final int srcY = (imgHeight - srcHeight) / 2;
                
                /*
                 * Drawing image a lot.
                 */
                
                // Letting room for text.
                tmpY += textH;
                
                final long startNs = System.nanoTime();
                for (int i = 0; i < NBR_OF_CALLS; i++) {
                    g.drawImage(
                        tmpX, tmpY, DST_W, DST_H,
                        image,
                        srcX, srcY, srcWidth, srcHeight);
                }
                final long endNs = System.nanoTime();
                
                // If has alpha, adding a final draw after a clear,
                // to avoid over-blended result and make the alpha obvious.
                if (hasAlpha) {
                    g.setColor(BwdColor.WHITE);
                    g.clearRect(tmpX, tmpY, DST_W, DST_H);
                    g.drawImage(
                        tmpX, tmpY, DST_W, DST_H,
                        image,
                        srcX, srcY, srcWidth, srcHeight);
                }
                
                tmpY -= textH;
                
                /*
                 * Drawing how long it took.
                 */
                
                final long dtNs = endNs - startNs;
                g.setFont(defaultFont);
                final String comment =
                        (hasAlpha ? "alpha" : "opaque")
                        + ", scale = " + scale
                        + ", took " + TestUtils.nsToSRounded(dtNs) + " s";
                g.setColor(BwdColor.BLACK);
                g.drawText(tmpX, tmpY, comment);
                
                tmpX += (CELL_W + 1);
            }
            
            tmpX = box.x();
            tmpY += (textH + CELL_H);
        }
        
        /*
         * 
         */
        
        this.lastPaintNanos = System.nanoTime();
        
        getHost().ensurePendingClientPainting();
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
