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

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.AbstractBwdBinding;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

public class ScalingCliBwdTestCase extends HostBoundsGripsBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * Some libraries get crazy when font size starts to be big
     * (cf. max font size setting in SWT binding configuration),
     * so we use this limit for this test to be consistent
     * across bindings.
     */
    private static final int MAX_BIG_FONT_TARGET_HEIGHT = 100;

    /**
     * Not too large, for grip not to grow too much with scale.
     */
    private static final int GRIP_SPAN = 4;
    
    private static final int MIN_CELL_WIDTH = 180;
    private static final int CELL_HEIGHT = 20;
    
    private static final int INITIAL_WIDTH = 800;
    private static final int INITIAL_HEIGHT = 600;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private GRect lastInnerBox = GRect.DEFAULT_EMPTY;
    
    private BwdMouseEvent lastMouseEvent = null;
    
    private GRect icoDeicoCell = GRect.DEFAULT_EMPTY;
    private GRect maxDemaxCell = GRect.DEFAULT_EMPTY;

    private GRect incrScaleCell = GRect.DEFAULT_EMPTY;
    private GRect decrScaleCell = GRect.DEFAULT_EMPTY;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ScalingCliBwdTestCase() {
    }

    public ScalingCliBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ScalingCliBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ScalingCliBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);
        
        this.onMouseEvent(event);

        final int x = event.xInClient();
        final int y = event.yInClient();
        
        if (this.lastInnerBox.contains(x, y)) {
            final boolean mustIcoDeico = this.icoDeicoCell.contains(x, y);
            final boolean mustMaxDemax = this.maxDemaxCell.contains(x, y);
            final boolean mustIncrScale = this.incrScaleCell.contains(x, y);
            final boolean mustDecrScale = this.decrScaleCell.contains(x, y);
            if (mustIcoDeico
                || mustMaxDemax
                || mustIncrScale
                || mustDecrScale) {
                final InterfaceBwdHost host = this.getHost();
                this.getBinding().getUiThreadScheduler().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mustIcoDeico) {
                            if (host.isIconified()) {
                                host.deiconify();
                            } else {
                                host.iconify();
                            }
                        } else if (mustMaxDemax) {
                            if (host.isMaximized()) {
                                host.demaximize();
                            } else {
                                host.maximize();
                            }
                        } else {
                            boolean didRescale = false;
                            if (mustIncrScale) {
                                final int oldScale = getScale();
                                setScale(oldScale + 1);
                                didRescale = true;
                            } else if (mustDecrScale) {
                                final int oldScale = getScale();
                                if (oldScale > 1) {
                                    setScale(oldScale - 1);
                                    didRescale = true;
                                }
                            }
                            if (didRescale) {
                                onSpanChange();
                                host.ensurePendingClientPainting();
                            }
                        }
                    }
                });
            }
        } else {
            /*
             * Press on border grips:
             * just doing border grips stuffs.
             */
        }
    }
    
    @Override
    public void onMouseReleased(BwdMouseEvent event) {
        super.onMouseReleased(event);
        
        this.onMouseEvent(event);
    }

    @Override
    public void onMouseEnteredClient(BwdMouseEvent event) {
        super.onMouseEnteredClient(event);
        
        this.onMouseEvent(event);
    }

    @Override
    public void onMouseExitedClient(BwdMouseEvent event) {
        super.onMouseExitedClient(event);
        
        this.onMouseEvent(event);
    }
    
    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        super.onMouseMoved(event);
        
        this.onMouseEvent(event);
    }

    @Override
    public void onMouseDragged(BwdMouseEvent event) {
        super.onMouseDragged(event);
        
        this.onMouseEvent(event);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected int getGripSpan() {
        return GRIP_SPAN;
    }

    @Override
    protected int getMinClientXSpan() {
        return Math.max(1, super.getMinClientXSpan() / getScale());
    }

    @Override
    protected int getMinClientYSpan() {
        return Math.max(1, super.getMinClientYSpan() / getScale());
    }

    @Override
    protected boolean mustScaleImageToWithinGrip() {
        /*
         * True to see how image scaling
         * evolves with binding pixels scaling.
         */
        return true;
    }
    
    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final List<GRect> ret = super.paintClientImpl(g, dirtyRect);
        g.reset();

        final GRect box = g.getBox();
        final int gripSpan = this.getGripSpan();
        final GRect innerBox = box.withBordersDeltasElseEmpty(
            gripSpan, gripSpan, -gripSpan, -gripSpan);
        this.lastInnerBox = innerBox;
        
        this.paintInnerBox(g, innerBox);
        
        return ret;
    }
    
    protected void paintInnerBox(
            InterfaceBwdGraphics g,
            GRect innerBox) {
        
        g.addClipInUser(innerBox);
        
        /*
         * Filling a semi-transparent oval,
         * to check figures scaling.
         */
        
        {
            g.setColor(BwdColor.GREEN.withAlphaFp(0.5));
            g.fillOval(innerBox);
        }
        
        /*
         * Drawing the scale as a big digit,
         * to check font scaling.
         */
        
        if (innerBox.ySpan() != 0) {
            g.setColor(BwdColor.BLUE.withAlphaFp(0.5));
            // Must be >= 1.
            final int bigFontTargetHeight =
                Math.min(
                    MAX_BIG_FONT_TARGET_HEIGHT,
                    innerBox.ySpan());
            final InterfaceBwdFont bigFont =
                getBinding().getFontHome().newFontWithFloorElseClosestHeight(
                    bigFontTargetHeight);
            final InterfaceBwdFontMetrics bigFontMetrics = bigFont.metrics();
            try {
                g.setFont(bigFont);
                final String text = "" + getScale();
                g.drawText(
                    innerBox.xMid() - bigFontMetrics.computeTextWidth(text) / 2,
                    innerBox.y(),
                    text);
            } finally {
                bigFont.dispose();
            }
            g.setFont(getBinding().getFontHome().getDefaultFont());
        }
        
        /*
         * Drawing cells:
         * +------------+------------+
         * | insets     | max/demax  |
         * +------------+------------+
         * | win bounds | incr scale |
         * +------------+------------+
         * | cli bounds | decr scale |
         * +------------+------------+
         * | th. c. bds | ico/deico  |
         * +------------+------------+
         */
        
        final InterfaceBwdHost host = this.getHost();
        final GRect insets = host.getInsets();
        final GRect windowBounds = host.getWindowBounds();
        final GRect clientBounds = host.getClientBounds();
        final GRect theoClientBounds = BindingCoordsUtils.computeClientBounds(
            insets,
            windowBounds);

        final List<String> textList = new ArrayList<String>();
        final String textMaxDemax = inList(textList, "max/demax");
        final String textIcoDeico = inList(textList, "ico/deico");
        final String textInsets = inList(textList, "I = " + insets);
        final String textWindowBounds = inList(textList, "WB = " + windowBounds);
        final String textClientBounds = inList(textList, "CB = " + clientBounds);
        final String textTheoClientBounds = inList(textList, "TC = " + theoClientBounds);
        final String textIncrScale = inList(textList, "Incr. Scale");
        final String textDecrScale = inList(textList, "Decr. Scale");
        
        final InterfaceBwdFontMetrics fm = g.getFont().metrics();
        int cellWidth = MIN_CELL_WIDTH;
        for (String str : textList) {
            cellWidth = Math.max(cellWidth, fm.computeTextWidth(str) + 1);
        }
        
        final int xMid = innerBox.xMid();
        final int yMid = innerBox.yMid();
        //
        final int col0X = xMid - (cellWidth - 1);
        final int col1X = xMid;
        //
        final int row0Y = yMid - 2 * (CELL_HEIGHT - 1);
        final int row1Y = yMid - (CELL_HEIGHT - 1);
        final int row2Y = yMid;
        final int row3Y = yMid + (CELL_HEIGHT - 1);
        {
            final GRect cell = GRect.valueOf(
                col1X, row0Y, cellWidth, CELL_HEIGHT);
            this.maxDemaxCell = cell;
            g.setColor(BwdColor.WHITE);
            g.drawRect(cell);
            g.drawText(cell.x() + 2, cell.y() + 2, textMaxDemax);
        }
        {
            final GRect cell = GRect.valueOf(
                col1X, row3Y, cellWidth, CELL_HEIGHT);
            this.icoDeicoCell = cell;
            g.setColor(BwdColor.WHITE);
            g.drawRect(cell);
            g.drawText(cell.x() + 2, cell.y() + 2, textIcoDeico);
        }
        {
            final GRect cell = GRect.valueOf(
                col0X, row0Y, cellWidth, CELL_HEIGHT);
            g.setColor(BwdColor.WHITE);
            g.drawRect(cell);
            g.drawText(cell.x() + 2, cell.y() + 2, textInsets);
        }
        {
            final GRect cell = GRect.valueOf(
                col0X, row1Y, cellWidth, CELL_HEIGHT);
            g.setColor(BwdColor.WHITE);
            g.drawRect(cell);
            g.drawText(cell.x() + 2, cell.y() + 2, textWindowBounds);
        }
        {
            final GRect cell = GRect.valueOf(
                col0X, row2Y, cellWidth, CELL_HEIGHT);
            g.setColor(BwdColor.WHITE);
            g.drawRect(cell);
            g.drawText(cell.x() + 2, cell.y() + 2, textClientBounds);
        }
        {
            final GRect cell = GRect.valueOf(
                col0X, row3Y, cellWidth, CELL_HEIGHT);
            g.setColor(BwdColor.WHITE);
            g.drawRect(cell);
            g.drawText(cell.x() + 2, cell.y() + 2, textTheoClientBounds);
        }
        {
            final GRect cell = GRect.valueOf(
                col1X, row1Y, cellWidth, CELL_HEIGHT);
            this.incrScaleCell = cell;
            g.setColor(BwdColor.WHITE);
            g.drawRect(cell);
            g.drawText(cell.x() + 2, cell.y() + 2, textIncrScale);
        }
        {
            final GRect cell = GRect.valueOf(
                col1X, row2Y, cellWidth, CELL_HEIGHT);
            this.decrScaleCell = cell;
            g.setColor(BwdColor.WHITE);
            g.drawRect(cell);
            g.drawText(cell.x() + 2, cell.y() + 2, textDecrScale);
        }

        /*
         * 
         */
        
        {
            final BwdMouseEvent event = this.lastMouseEvent;
            if (event != null) {
                final int lineH = (g.getFont().metrics().height() + 2);
                g.setColor(BwdColor.WHITE);
                final String[] textArr = new String[] {
                    "Last mouse event:",
                    "in screen: (" + event.xInScreen() + ", " + event.yInScreen() + ")",
                    "in client: (" + event.xInClient() + ", " + event.yInClient() + ")",
                };
                for (int i = 0; i < textArr.length; i++) {
                    g.drawText(
                        innerBox.x() + 2,
                        innerBox.yMax() - lineH * (textArr.length - i),
                        textArr[i]);
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static String inList(List<String> textList, String text) {
        textList.add(text);
        return text;
    }
    
    private int getScale() {
        final AbstractBwdBinding binding =
            (AbstractBwdBinding) getBinding();
        return binding.getBindingConfig().getScale();
    }
    
    private void setScale(int scale) {
        final AbstractBwdBinding binding =
            (AbstractBwdBinding) getBinding();
        binding.getBindingConfig().setScale(scale);
    }
    
    private void onMouseEvent(BwdMouseEvent event) {
        this.lastMouseEvent = event;
        this.getHost().ensurePendingClientPainting();
    }
}
