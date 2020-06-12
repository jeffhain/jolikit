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

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.HertzHelper;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplittable;

/**
 * To test that basic parallel painting works and can provide some speed up.
 */
public class BenchParallelFillBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int PARALLELISM = 4;
    
    private static final int AREA_SPLIT_THRESHOLD = 5 * 100 * 100;
    
    /**
     * Might not see much speed-up if we only fill once, due to final rendering
     * or double buffering not being done in parallel.
     * A few fills should still be quite fast (very simple operation),
     * and simulate a fewer number of more complex drawing operations.
     */
    private static final int FILL_COUNT_PER_PAINT = 10;
    
    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 1000;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyFillSplittable implements InterfaceSplittable {
        private final InterfaceBwdGraphics g;
        /**
         * Changing the fill box as we split.
         */
        private GRect fillBox;
        /**
         * @param g this graphics won't be modified. A child graphics will be used instead.
         */
        public MyFillSplittable(InterfaceBwdGraphics g, GRect fillBox) {
            this.g = g.newChildGraphics(fillBox);
            this.fillBox = fillBox;
        }
        @Override
        public boolean worthToSplit() {
            final GRect box = this.fillBox;
            final int area = NumbersUtils.timesBounded(box.xSpan(),box.ySpan());
            return area > AREA_SPLIT_THRESHOLD;
        }
        @Override
        public InterfaceSplittable split() {
            final InterfaceBwdGraphics g = this.g;
            
            final GRect fillBox = this.fillBox;
            final int x = fillBox.x();
            final int y = fillBox.y();
            final int xSpan = fillBox.xSpan();
            final int ySpan = fillBox.ySpan();
            
            /*
             * Cutting in half, horizontally since in graphics
             * things are usually row-major.
             */
            
            final int halfish = (ySpan>>1);
            
            final GRect box1 = GRect.valueOf(
                    x,
                    y,
                    xSpan,
                    ySpan - halfish);
            final GRect box2 = GRect.valueOf(
                    x,
                    y + (ySpan - halfish),
                    xSpan,
                    halfish);
            
            this.fillBox = box1;
            
            return new MyFillSplittable(g, box2);
        }
        @Override
        public void run() {
            final InterfaceBwdGraphics g = this.g;
            g.init();
            try {
                fillBox_inInitFinish(g, this.fillBox);
            } finally {
                g.finish();
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private boolean mustUsePaintingParallelizer = true;
    
    private final HertzHelper ppsHelper = new HertzHelper();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public BenchParallelFillBwdTestCase() {
    }

    public BenchParallelFillBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new BenchParallelFillBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new BenchParallelFillBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    /*
     * 
     */
    
    @Override
    public Integer getParallelizerParallelismElseNull() {
        return PARALLELISM;
    }

    /*
     * 
     */
    
    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);
        
        if (event.getButton() == BwdMouseButtons.PRIMARY) {
            this.mustUsePaintingParallelizer = !this.mustUsePaintingParallelizer;
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final InterfaceBwdBinding binding = this.getBinding();
        
        /*
         * 
         */

        this.ppsHelper.onEvent();
        final int pps = this.ppsHelper.getFrequencyHzRounded();
        
        /*
         * 
         */
        
        final int parallelism;
        if (this.mustUsePaintingParallelizer) {
            final InterfaceParallelizer parallelizer = g.getPaintingParallelizer();
            parallelism = parallelizer.getParallelism();
            
            // Binding consistency check.
            if ((!binding.isParallelPaintingSupported())
                    && (parallelism > 1)) {
                throw new IllegalStateException("parallel painting not supported, but painting parallelism is " + parallelism);
            }
            
            final MyFillSplittable fillSplittable = new MyFillSplittable(
                    g,
                    g.getClipInUser());
            parallelizer.execute(fillSplittable);
        } else {
            parallelism = 1;
            
            fillBox_inInitFinish(g, g.getBox());
        }
        
        /*
         * 
         */
        
        drawInfo(
                g,
                parallelism,
                pps);
        
        /*
         * Scheduling next painting.
         */
        
        getHost().ensurePendingClientPainting();
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void fillBox_inInitFinish(InterfaceBwdGraphics g, GRect fillBox) {
        
        /*
         * Using a color based on thread hash code.
         */
        
        final int thc = Thread.currentThread().hashCode();
        final int alpha8 = 0xFF;
        final int red8 = (thc * 3) & 0xFF;
        final int green8 = (thc * 5) & 0xFF;
        final int blue8 = (thc * 7) & 0xFF;
        final int argb32 = Argb32.toArgb32FromInt8(alpha8, red8, green8, blue8);
        
        g.setArgb32(argb32);
        
        for (int i = 0; i < FILL_COUNT_PER_PAINT; i++) {
            g.fillRect(fillBox);
        }
    }
    
    private static void drawInfo(
            InterfaceBwdGraphics g,
            int parallelism,
            int pps) {
        
        final GRect box = g.getBox();

        final String prlHead = FILL_COUNT_PER_PAINT + " fills/paint, Parallelism = ";
        final String ppsHead = ", Paints Per Pecond = ";
        // To avoid digits of obsolete values to be visible.
        final String textForWidth = prlHead + "123456789" + ppsHead + "123456789";
        final String text = prlHead + parallelism + ppsHead + pps;

        final int textX = box.x();
        final int textY = box.y();
        
        final InterfaceBwdFont font = g.getFont();
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        final int textWidth = metrics.computeTextWidth(textForWidth);
        final int textHeight = metrics.fontHeight();
        
        BwdTestUtils.drawTextAndSpannedBg(
                g, BwdColor.WHITE, BwdColor.BLACK,
                textX, textY, text, textWidth, textHeight);
    }
}
