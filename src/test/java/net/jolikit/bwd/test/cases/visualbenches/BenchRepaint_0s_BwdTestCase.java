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
package net.jolikit.bwd.test.cases.visualbenches;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.HertzHelper;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;

/**
 * To test that when requesting ASAP repaint, things gets properly
 * (programmatically and visibly) repaint ASAP.
 */
public class BenchRepaint_0s_BwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG_SPAM = false;
    
    private static final String IMG_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_ALPHA_PNG;
    
    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private long paintCounter = 0;
    
    private final HertzHelper ppsHelper = new HertzHelper();

    /**
     * Drawing an image (primitives and text are already covered),
     * to check that it also shows up properly (had some issues in benches
     * with some kind of paintings making it to the screen, and others not).
     */
    private InterfaceBwdImage image = null;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public BenchRepaint_0s_BwdTestCase() {
    }

    public BenchRepaint_0s_BwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new BenchRepaint_0s_BwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new BenchRepaint_0s_BwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    /*
     * 
     */

    @Override
    public Double getClientPaintDelaySElseNull() {
        return 0.0;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        if (DEBUG_SPAM) {
            Dbg.log("paintNum = " + (this.paintCounter + 1));
        }

        final GRect box = g.getBoxInClient();
        
        this.ppsHelper.onEvent();
        final int pps = this.ppsHelper.getFrequencyHzRounded();
        
        /*
         * 
         */

        g.setColor(BwdColor.WHITE);
        g.fillRect(box);

        /*
         * 
         */
        
        if (this.image == null) {
            this.image = this.getBinding().newImage(IMG_PATH);
        }
        g.drawImage(box.xMid(), box.yMid(), this.image);
        
        /*
         * 
         */

        // Moving line, to easily see frequency (ir)regularities,
        // and eventual ignorings of some paintings.
        {
            final int x = (int) (this.paintCounter % box.xSpan());
            g.setColor(BwdColor.BLACK);
            g.drawLine(x, 0, x, box.yMax());
        }
        
        /*
         * 
         */
        
        g.setColor(BwdColor.BLACK);
        g.drawText(10, box.yMid(), "PPS = " + pps);

        this.paintCounter++;
        
        getHost().ensurePendingClientPainting();
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
