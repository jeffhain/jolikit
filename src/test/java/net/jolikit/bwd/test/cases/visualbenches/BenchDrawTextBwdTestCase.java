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

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.test.utils.TestUtils;

/**
 * To bench text drawing.
 */
public class BenchDrawTextBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int[] FONT_SIZE_ARR = new int[]{12, 24, 48};

    private static final String TEXT = "0123456789abcdefghijklmnopqrstuvwxyz";
    
    private int getNbrOfCalls() {
        final InterfaceBwdBinding binding = this.getBinding();
        final String bindingCsn = binding.getClass().getSimpleName();
        
        final int ret;
        if (BwdTestUtils.isAwtBinding(binding)) {
            ret = 10 * 1000;
        } else if (BwdTestUtils.isSwingBinding(binding)) {
            ret = 10 * 1000;
        } else if (BwdTestUtils.isJfxBinding(binding)) {
            ret = 10 * 1000;
            
        } else if (BwdTestUtils.isSwtBinding(binding)) {
            ret = 100;
        } else if (BwdTestUtils.isLwjglBinding(binding)) {
            ret = 1000;
        } else if (BwdTestUtils.isJoglBinding(binding)) {
            ret = 1000;
            
        } else if (BwdTestUtils.isQtjBinding(binding)) {
            ret = 1000;
        } else if (BwdTestUtils.isAlgrBinding(binding)) {
            ret = 100;
        } else if (BwdTestUtils.isSdlBinding(binding)) {
            ret = 100;
        } else {
            throw new AssertionError("" + bindingCsn);
        }

        return ret;
    }

    private static final int INITIAL_WIDTH = 1100;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final List<InterfaceBwdFont> fontList = new ArrayList<InterfaceBwdFont>();
    
    private long lastPaintNanos = Long.MAX_VALUE;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public BenchDrawTextBwdTestCase() {
    }

    public BenchDrawTextBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new BenchDrawTextBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new BenchDrawTextBwdTestCase(this.getBinding());
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
        
        final GRect box = g.getBoxInClient();
        
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
         * Ensuring fonts to use.
         */
        
        if (this.fontList.size() == 0) {
            final BwdFontKind defaultFontKind = defaultFont.fontKind();
            for (int fontSize : FONT_SIZE_ARR) {
                this.fontList.add(fontHome.newFontWithSize(defaultFontKind, fontSize));
            }
        }

        /*
         * Benching.
         */
        
        final int nbrOfCalls = this.getNbrOfCalls();
        
        final String text = TEXT;
        
        for (InterfaceBwdFont font : this.fontList) {
            final int fh = font.fontMetrics().fontHeight() + 1;
            
            g.setFont(font);
            
            /*
             * Drawing a lot.
             */
            
            y += dh;
            
            final long startNs = System.nanoTime();
            for (int i = 0; i < nbrOfCalls; i++) {
                g.drawText(0, y, text);
            }
            final long endNs = System.nanoTime();
            
            y -= dh;
            
            /*
             * Drawing how long it took.
             */
            
            final long dtNs = endNs - startNs;
            g.setFont(defaultFont);
            final String comment =
                    nbrOfCalls + " calls, "
                            + text.length() + " chars, fontSize = "
                            + font.fontSize() + ", tooks " + TestUtils.nsToSRounded(dtNs) + " s";
            g.drawText(0, y, comment);
            
            y += (2 * dh + fh);
        }
        
        /*
         * 
         */
        
        this.lastPaintNanos = System.nanoTime();
        
        getHost().ensurePendingClientPainting();
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
