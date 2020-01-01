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
package net.jolikit.bwd.test.cases.visualsturds;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplittable;

/**
 * To test that painting can be done in parallel,
 * including when using (already-created) fonts and images.
 */
public class ParallelDrawingBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    private static final int PARALLELISM = 4;
    
    /**
     * To make sure our font is not identical to default font,
     * so that setting it does something for sure.
     */
    private static final int FONT_SIZE_DELTA = 1;
    
    private static final String IMG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PNG;

    private static final int SPLIT_COUNT = 2;
    
    private static final int INITIAL_WIDTH = 200;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MySplittable implements InterfaceSplittable {
        /**
         * Creating a new graphics for this splittable at each split,
         * and storing the draw box to use as its box and base clip,
         * which allows to make sure we won't leak outside of it while drawing.
         */
        private InterfaceBwdGraphics g;
        private int remainingSplitCount;
        /**
         * @param g this graphics won't be modified. A child graphics will be used instead.
         */
        public MySplittable(
                InterfaceBwdGraphics g,
                GRect drawBox,
                int remainingSplitCount) {
            this.g = g.newChildGraphics(drawBox);
            this.remainingSplitCount = remainingSplitCount;
        }
        @Override
        public boolean worthToSplit() {
            return (this.remainingSplitCount > 0);
        }
        @Override
        public InterfaceSplittable split() {
            final InterfaceBwdGraphics g = this.g;
            
            final GRect drawBox = g.getBaseClipInClient();
            final int x = drawBox.x();
            final int y = drawBox.y();
            final int xSpan = drawBox.xSpan();
            final int ySpan = drawBox.ySpan();
            
            /*
             * Cutting the longest span in half.
             */
            
            final GRect box1;
            final GRect box2;
            if (xSpan > ySpan) {
                final int halfish = (xSpan>>1);
                
                box1 = GRect.valueOf(
                        x,
                        y,
                        xSpan - halfish,
                        ySpan);
                box2 = GRect.valueOf(
                        x + (xSpan - halfish),
                        y,
                        halfish,
                        ySpan);
            } else {
                final int halfish = (ySpan>>1);
                
                box1 = GRect.valueOf(
                        x,
                        y,
                        xSpan,
                        ySpan - halfish);
                box2 = GRect.valueOf(
                        x,
                        y + (ySpan - halfish),
                        xSpan,
                        halfish);
            }
            
            this.remainingSplitCount--;
            
            this.g = g.newChildGraphics(box1);
            
            return new MySplittable(g, box2, this.remainingSplitCount);
        }
        @Override
        public void run() {
            final InterfaceBwdGraphics g = this.g;
            g.init();
            try {
                drawInBox_inInitFinish(g);
            } finally {
                g.finish();
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private boolean initialized = false;
    private InterfaceBwdFont font;
    private InterfaceBwdImage image;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ParallelDrawingBwdTestCase() {
    }

    public ParallelDrawingBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ParallelDrawingBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ParallelDrawingBwdTestCase(this.getBinding());
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
        
        if (!this.initialized) {
            final InterfaceBwdFontHome fontHome = this.getBinding().getFontHome();
            final int fontSize = g.getFont().fontSize() + FONT_SIZE_DELTA;
            this.font = fontHome.newFontWithSize(g.getFont().fontKind(), fontSize);
            
            this.image = this.getBinding().newImage(IMG_FILE_PATH);
            
            this.initialized = true;
        }
        
        /*
         * 
         */
        
        {
            final InterfaceParallelizer parallelizer = g.getPaintingParallelizer();
            final int parallelism = parallelizer.getParallelism();
            
            // Binding consistency check.
            if ((!binding.isParallelPaintingSupported())
                    && (parallelism > 1)) {
                throw new IllegalStateException("parallel painting not supported, but painting parallelism is " + parallelism);
            }

            final MySplittable fillSplittable = new MySplittable(
                    g,
                    g.getClipInUser(),
                    SPLIT_COUNT);
            parallelizer.execute(fillSplittable);
        }
        
        /*
         * Scheduling next painting.
         */
        
        if (DEBUG) {
            Dbg.log("tc: ensurePendingClientPainting(...)...");
        }
        getHost().ensurePendingClientPainting();
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void drawInBox_inInitFinish(InterfaceBwdGraphics g) {
        final GRect box = g.getBoxInClient();
        
        final int x = box.x();
        final int y = box.y();
        final int xSpan = box.xSpan();
        final int ySpan = box.ySpan();
        
        /*
         * Filling.
         * Using a color based on thread hash code.
         */
        
        final int thc = Thread.currentThread().hashCode();
        {
            final int alpha8 = 0xFF;
            final int red8 = (thc * 3) & 0xFF;
            final int green8 = (thc * 5) & 0xFF;
            final int blue8 = (thc * 7) & 0xFF;
            final int argb32 = Argb32.toArgb32FromInt8(alpha8, red8, green8, blue8);
            
            g.setArgb32(argb32);

            g.fillRect(box);
        }
        
        /*
         * Images.
         */
        
        {
            // Only drawing on upper half, so that we can see the background.
            g.drawImage(x, y, xSpan, ySpan/2, this.image);
        }
        
        /*
         * Text.
         */
        
        {
            final String text = "THC = " + thc;
            
            final InterfaceBwdFont initialFont = g.getFont();
            final InterfaceBwdFont font = this.font;
            g.setFont(font);
            try {
                final InterfaceBwdFontMetrics metrics = font.fontMetrics();
                final int textHeight = metrics.fontHeight();
                final int textWidth = metrics.computeTextWidth(text);
                
                g.setColor(BwdColor.WHITE);
                g.fillRect(x, y, textWidth, textHeight);

                g.setColor(BwdColor.BLACK);
                g.drawText(x, y, text);
            } finally {
                g.setFont(initialFont);
            }
        }
    }
}
