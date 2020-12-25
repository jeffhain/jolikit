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
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * Helps figuring out what angles mean in various libraries
 * (when not using our default implementations in bindings).
 */
public class ArcDrawFillBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int TARGET_FONT_HEIGHT = 13;
    
    private static final double START_DEG = 45.0;

    private static final double SPAN_STEP_DEG = 90.0 * 2;
    private static final int NBR_OF_STEPS = 6 + 1;
    /**
     * Negative to see how behaves with negative spans.
     */
    private static final double FIRST_SPAN_DEG = -SPAN_STEP_DEG - 360;

    private static final int RY = 25;
    /**
     * We want rx = 2 * ry, to easily figure out
     * whether the "diagonal" for 45 deg properly changes y
     * every two x.
     */
    private static final int RX = 2 * RY;
    private static final int OVAL_HEIGHT = 2 * RY + 1;
    private static final int OVAL_WIDTH = 2 * RX + 1;
    
    private static final int CELL_WIDTH = OVAL_WIDTH + 2;
    private static final int CELL_HEIGHT = OVAL_HEIGHT + 2;
    
    private static final int INITIAL_WIDTH = 3 * CELL_WIDTH;
    private static final int INITIAL_HEIGHT = NBR_OF_STEPS * CELL_HEIGHT;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdFont font;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ArcDrawFillBwdTestCase() {
    }

    public ArcDrawFillBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ArcDrawFillBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ArcDrawFillBwdTestCase(this.getBinding());
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
        
        final InterfaceBwdBinding binding = this.getBinding();
        
        /*
         * 
         */
        
        g.setColor(BwdColor.WHITE);
        g.fillRect(box);
        
        /*
         * 
         */

        InterfaceBwdFont font = this.font;
        if (font == null) {
            font = binding.getFontHome().newFontWithClosestHeight(TARGET_FONT_HEIGHT);
            this.font = font;
        }
        g.setFont(font);
        
        /*
         * 
         */
        
        g.setColor(BwdColor.BLACK);
        
        int tmpY = 0;
        double spanDeg = FIRST_SPAN_DEG;
        for (int i = 0; i < NBR_OF_STEPS; i++) {
            
            GRect cellRect = GRect.valueOf(0, tmpY, CELL_WIDTH, CELL_HEIGHT);
            
            g.drawRect(cellRect);
            g.drawText(cellRect.x() + 1, cellRect.y() + 1, "startDeg = " + START_DEG);
            g.drawText(cellRect.x() + 1, cellRect.yMid(), "spanDeg = " + spanDeg);
            
            cellRect = cellRect.withPosDeltas(CELL_WIDTH, 0);
            g.drawRect(cellRect);
            g.drawArc(
                    cellRect.x() + 1,
                    cellRect.y() + 1,
                    OVAL_WIDTH,
                    OVAL_HEIGHT,
                    //
                    START_DEG,
                    spanDeg);
            
            cellRect = cellRect.withPosDeltas(CELL_WIDTH, 0);
            g.drawRect(cellRect);
            g.fillArc(
                    cellRect.x() + 1,
                    cellRect.y() + 1,
                    OVAL_WIDTH,
                    OVAL_HEIGHT,
                    //
                    START_DEG,
                    spanDeg);
            
            tmpY += CELL_HEIGHT;
            spanDeg += SPAN_STEP_DEG;
        }
        
        /*
         * 
         */
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
