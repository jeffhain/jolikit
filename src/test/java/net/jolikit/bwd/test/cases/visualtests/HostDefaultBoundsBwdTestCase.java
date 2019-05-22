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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.AbstractBwdBinding;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdClientMock;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * To test that default bounds properly apply soon,
 * and that there is not too much flickering until they apply,
 * for example by using them (or not!) as default bounds
 * when creating the backing window, before showing and
 * actually (also) applying them.
 * 
 * Main test host allows to create hosts of various types,
 * using either configured default bounds,
 * other bounds set with setClientBounds(...) method,
 * or other bounds set with setWindowBounds(...) method.
 */
public class HostDefaultBoundsBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int TARGET_FONT_HEIGHT = 13;

    private static final int WIDTH_STEP = 6 * TARGET_FONT_HEIGHT;
    private static final int HEIGHT_STEP = 2 * TARGET_FONT_HEIGHT;
    
    private static final int INITIAL_WIDTH = 500;
    private static final int INITIAL_HEIGHT = 450;
    
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private enum MyBoundsTypeOnShow {
        DEFAULT_BOUNDS,
        USER_CLIENT_BOUNDS,
        USER_WINDOW_BOUNDS;
    }
    
    private static class MyButton {
        final String text;
        final GRect box;
        final Runnable command;
        public MyButton(
                String text,
                GRect bBox,
                Runnable command) {
            this.text = text;
            this.box = bBox;
            this.command = command;
        }
    }
    
    private class MyClient extends BwdClientMock {
        public MyClient() {
            this.configureUiThreadCheck(getBinding().getUiThreadScheduler());
        }
        @Override
        public void onMousePressed(BwdMouseEvent event) {
            super.onMousePressed(event);
            this.getHost().close();
        }
        @Override
        protected List<GRect> paint_initDone(
                InterfaceBwdGraphics g,
                GRect dirtyRect) {
            
            final GRect box = g.getBoxInClient();
            
            g.setColor(BwdColor.WHITE);
            g.clearRectOpaque(box);
            
            g.setColor(BwdColor.BLACK);
            BwdTestUtils.drawRectStipple(g, box);
            
            // Font created by main test host.
            g.setFont(font);
            
            final int fh = font.fontMetrics().fontHeight();
            final int lineH = fh + 1;
            int textY = 1;
            
            g.drawText(1, textY, this.getHost().getTitle());
            textY += lineH;
            
            final GRect clientBounds = this.getHost().getClientBounds();
            g.drawText(1, textY, "x = " + clientBounds.x());
            textY += lineH;
            g.drawText(1, textY, "y = " + clientBounds.y());
            textY += lineH;
            g.drawText(1, textY, "xSpan = " + clientBounds.xSpan());
            textY += lineH;
            g.drawText(1, textY, "ySpan = " + clientBounds.ySpan());
            textY += lineH;
            
            return GRect.DEFAULT_HUGE_IN_LIST;
        }
    }

    private class MyNewHostButtonCommand implements Runnable {
        /**
         * Whether must create a dialog from button host,
         * or a root host from binding.
         */
        private final boolean dialog;
        private final boolean decorated;
        /**
         * Only relevant for dialogs.
         */
        private final boolean modal;
        private final MyBoundsTypeOnShow boundsTypeOnShow;
        private final InterfaceBwdHost buttonHost;
        public MyNewHostButtonCommand(
                boolean dialog,
                boolean decorated,
                boolean modal,
                MyBoundsTypeOnShow boundsTypeOnShow,
                InterfaceBwdHost buttonHost) {
            this.dialog = dialog;
            this.decorated = decorated;
            this.modal = modal;
            this.boundsTypeOnShow = boundsTypeOnShow;
            this.buttonHost = buttonHost;
        }
        @Override
        public void run() {
            final String title = "H" + hostIdProvider.incrementAndGet()
                    + (this.dialog ? "-Di" : "")
                    + (this.decorated ? "-De" : "")
                    + (this.modal ? "-Mo" : "");
            
            final MyClient client = new MyClient();
            
            final InterfaceBwdHost newHost;
            if (this.dialog) {
                newHost = this.buttonHost.newDialog(
                        title,
                        this.decorated,
                        this.modal,
                        client);
            } else {
                newHost = getBinding().newHost(
                        title,
                        this.decorated,
                        client);
            }
            
            if (this.boundsTypeOnShow != MyBoundsTypeOnShow.DEFAULT_BOUNDS) {
                final AbstractBwdBinding binding = (AbstractBwdBinding) getBinding();
                final GRect defaultBounds = binding.getBindingConfig().getDefaultClientOrWindowBounds();
                final int dxy = 200;
                final GRect userBounds = defaultBounds.withPosDeltas(dxy, dxy);
                if (this.boundsTypeOnShow == MyBoundsTypeOnShow.USER_CLIENT_BOUNDS) {
                    newHost.setClientBounds(userBounds);
                } else {
                    newHost.setWindowBounds(userBounds);
                }
            }

            newHost.show();
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final AtomicInteger hostIdProvider = new AtomicInteger();
    
    private InterfaceBwdFont font;
    
    private final List<MyButton> buttonList = new ArrayList<MyButton>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public HostDefaultBoundsBwdTestCase() {
    }

    public HostDefaultBoundsBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
        // We want to see a stack trace for each error.
        this.setMustCallOnEventError(true);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new HostDefaultBoundsBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new HostDefaultBoundsBwdTestCase(this.getBinding());
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
        
        if (event.getButton() == BwdMouseButtons.PRIMARY) {
            final int x = event.xInClient();
            final int y = event.yInClient();
            
            for (MyButton button : this.buttonList) {
                if (button.box.contains(x, y)) {
                    /*
                     * Executing command asynchronously,
                     * to avoid concurrent modification issue
                     * if it causes synchronous painting.
                     */
                    getBinding().getUiThreadScheduler().execute(button.command);
                }
            }
        }
        
        /*
         * Async repaint ensuring, to make sure it gets executed AFTER
         * an eventual previous command of which we want to see the effect.
         */
        getBinding().getUiThreadScheduler().execute(new Runnable() {
            @Override
            public void run() {
                getHost().ensurePendingClientPainting();
            }
        });
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        if (this.font == null) {
            final InterfaceBwdFontHome fontHome = this.getBinding().getFontHome();
            this.font = fontHome.newFontWithClosestHeight(
                    fontHome.getDefaultFont().fontKind(),
                    TARGET_FONT_HEIGHT);
        }
        g.setFont(this.font);

        final GRect box = g.getBoxInClient();
        
        /*
         * 
         */
        
        final BwdColor bgColor = BwdColor.WHITE;
        g.setColor(bgColor);
        g.clearRectOpaque(box);
        
        /*
         * 
         */
        
        this.buttonList.clear();
        
        /*
         * 
         */
        
        this.updateClient(g, box);
        
        /*
         * Drawing buttons.
         */
        
        g.setColor(BwdColor.BLACK);
        
        for (MyButton button : this.buttonList) {
            final GRect bBox = button.box;
            g.drawRect(bBox);
            
            final String text = button.text;
            drawTextCentered(g, bBox.x(), bBox.y(), bBox.xSpan(), bBox.ySpan(), text);
        }

        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void updateClient(
            InterfaceBwdGraphics g,
            GRect box) {
        
        g.setColor(BwdColor.BLACK);
        g.drawRect(box);
        
        this.updateHostCreationArea(
                g,
                box);
    }
    
    /*
     * 
     */
    
    private void updateHostCreationArea(
            InterfaceBwdGraphics g,
            GRect box) {
        int remainingWidth = box.xSpan();
        final int subWidth = remainingWidth / 3;
        final int subHeight = box.ySpan() / 3;
        
        g.setColor(BwdColor.BLACK);
        g.drawRect(box);
        
        int tmpY = box.y();
        
        for (MyBoundsTypeOnShow boundsTypeOnShow : MyBoundsTypeOnShow.values()) {
            int tmpX = box.x();
            
            // {dialog, modal, title}
            for (Object[] propArr : new Object[][] {
                {false, false, "New Root Host:"},
                {true, false, "New Dialog Host:"},
                {true, true, "New Dialog Host (modal):"},
            }) {
                final boolean dialog = (Boolean) propArr[0];
                final boolean modal = (Boolean) propArr[1];
                final String title = (String) propArr[2];
                
                final GRect subBox = GRect.valueOf(
                        tmpX,
                        tmpY,
                        subWidth,
                        subHeight);
                this.updateNewHostControlPanel(
                        g,
                        subBox,
                        boundsTypeOnShow,
                        title,
                        dialog,
                        modal,
                        this.getHost());
                tmpX += subWidth;
            }
            tmpY += subHeight;
        }
    }
    
    /*
     * 
     */
    
    private static void drawTextCentered(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan,
            String text) {
        final InterfaceBwdFont font = g.getFont();
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        final int textWidth = metrics.computeTextWidth(text);
        final int textHeight = metrics.fontHeight();
        final int textX = x + (xSpan - textWidth) / 2;
        final int textY = y + (ySpan - textHeight) / 2;
        g.drawText(textX, textY, text);
    }

    private void updateNewHostControlPanel(
            InterfaceBwdGraphics g,
            GRect box,
            MyBoundsTypeOnShow boundsTypeOnShow,
            String title,
            boolean dialog,
            boolean modal,
            InterfaceBwdHost host) {
        
        int tmpY = box.y();
        g.drawText(box.x(), tmpY, "" + boundsTypeOnShow);
        tmpY += HEIGHT_STEP;
        
        /*
         * new root host:  new dialog (non-modal):  new dialog host (modal):
         * (decorated)     (decorated)              (decorated)
         * (naked)         (naked)                  (naked)
         */
        g.setColor(BwdColor.BLACK);
        g.drawText(box.x(), tmpY, title);
        tmpY += HEIGHT_STEP;
        
        for (int i = 0; i < 2; i++) {
            final boolean decorated = (i == 0);
            final GRect bBox = GRect.valueOf(
                    box.x(),
                    tmpY,
                    WIDTH_STEP,
                    HEIGHT_STEP);
            final MyNewHostButtonCommand command = new MyNewHostButtonCommand(
                    dialog,
                    decorated,
                    modal,
                    boundsTypeOnShow,
                    this.getHost());
            final MyButton button = new MyButton(
                    (decorated ? "decorated" : "naked"),
                    bBox,
                    command);
            this.buttonList.add(button);
            
            tmpY += HEIGHT_STEP;
        }
    }
}
