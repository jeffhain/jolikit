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
package net.jolikit.bwd.test.cases.unitsturds;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * Paints furiously, loading and unloading resources every time (images, fonts),
 * which should cause no memory leak.
 */
public class NoLeakBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * Same PPS (paints per second) for everyone (60 should be fine),
     * so that we can compare leaks speeds if any.
     */
    private static final int TARGET_PPS = 60;
    
    /**
     * If false, could have some leaks.
     * 
     * Top leaks:
     * 1) SDL (whether or not we dispose: issue with fonts that don't seem to "close")
     * 2) LWJGL (only if not disposing)
     * 
     * Other bindings seem not to leak even if we don't dispose.
     */
    private static final boolean MUST_DISPOSE_PROPERLY = true;
    
    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 600;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private long lastPaintTimeNs;
    
    private int paintCount = 0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public NoLeakBwdTestCase() {
    }

    public NoLeakBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
        this.lastPaintTimeNs = binding.getUiThreadScheduler().getClock().getTimeNs();
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new NoLeakBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new NoLeakBwdTestCase(this.getBinding());
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
        final InterfaceScheduler scheduler = binding.getUiThreadScheduler();
        final long lastPaintTimeNs = this.lastPaintTimeNs;
        this.lastPaintTimeNs = scheduler.getClock().getTimeNs();

        /*
         * 
         */
        
        final GRect box = g.getBox();
        
        /*
         * 
         */

        g.setColor(BwdColor.BLACK);
        g.clearRect(box);
        
        /*
         * Image.
         */

        if (true) {
            final InterfaceBwdImage image = binding.newImage(BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG);
            try {
                g.drawImage(0, 0, image);
            } finally {
                if (MUST_DISPOSE_PROPERLY) {
                    image.dispose();
                }
            }
        }
        
        if (true) {
            final InterfaceBwdWritableImage wi = binding.newWritableImage(INITIAL_WIDTH, INITIAL_HEIGHT);
            try {
                final InterfaceBwdGraphics wig = wi.getGraphics();
                wig.clearRect(wig.getBox());
                g.drawImage(0, 0, wi);
            } finally {
                if (MUST_DISPOSE_PROPERLY) {
                    wi.dispose();
                }
            }
        }

        /*
         * Font.
         */
        
        if (true) {
            final InterfaceBwdFontHome fontHome = binding.getFontHome();
            
            final InterfaceBwdFont initialFont = g.getFont();
            
            final int size = 20;
            final InterfaceBwdFont font = fontHome.newFontWithSize(initialFont.kind(), size);
            
            final int pc = ++this.paintCount;
            
            g.setFont(font);
            try {
                g.setColor(BwdColor.WHITE);
                g.drawText(0, 0, "No Leak Please.");
                g.drawText(0, size, "Paint count = " + pc);
            } finally {
                g.setFont(initialFont);
                if (MUST_DISPOSE_PROPERLY) {
                    font.dispose();
                }
            }
        }
        
        /*
         * Scheduling next painting.
         */
        
        final double ppsDelayS = 1.0/TARGET_PPS;
        final long ppsDelayNs = TimeUtils.sToNs(ppsDelayS);
        final long nextTimeNs = NumbersUtils.plusBounded(lastPaintTimeNs, ppsDelayNs);
        scheduler.executeAtNs(new Runnable() {
            @Override
            public void run() {
                getHost().ensurePendingClientPainting();
            }
        }, nextTimeNs);
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
