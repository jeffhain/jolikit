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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdEvent;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdEventTestHelper;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * To test host state, behavior, controls, and events.
 */
public class HostBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    private static final int EVENT_INFO_LIST_MAX_SIZE = 20;

    private static final int TARGET_FONT_HEIGHT = 13;
    
    private static final double REPAINT_DELAY_S = 0.2;

    /**
     * Only hiding or iconifying for a small duration,
     * else it's a black hole.
     */
    private static final int HIDDEN_OR_ICONIFIED_DELAY_S = 3;

    private static final BwdColor COLOR_BG_HOST_NOT_FOCUSED = BwdColor.WHITE;
    private static final BwdColor COLOR_BG_HOST_FOCUSED = BwdColor.YELLOW;
    
    private static final int WIDTH_STEP = 6 * TARGET_FONT_HEIGHT;
    private static final int HEIGHT_STEP = 2 * TARGET_FONT_HEIGHT;

    private static final int CONTROL_AREA_HEIGHT = 6 * HEIGHT_STEP;
    private static final int CREATION_AREA_HEIGHT = 120;
    private static final int INITIAL_HOST_EVENT_INFO_AREA_HEIGHT = EVENT_INFO_LIST_MAX_SIZE * (TARGET_FONT_HEIGHT + 1);

    private static final int INITIAL_WIDTH = 700;
    private static final int INITIAL_HEIGHT = CONTROL_AREA_HEIGHT + CREATION_AREA_HEIGHT + INITIAL_HOST_EVENT_INFO_AREA_HEIGHT;
    
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
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

    private class MyNewHostButtonCommand implements Runnable {
        private final GRect buttonBox;
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
        private final InterfaceBwdHost buttonHost;
        private final InterfaceBwdClient newHostClient;
        public MyNewHostButtonCommand(
                GRect buttonBox,
                boolean dialog,
                boolean decorated,
                boolean modal,
                InterfaceBwdHost buttonHost,
                InterfaceBwdClient newHostClient) {
            this.buttonBox = buttonBox;
            this.dialog = dialog;
            this.decorated = decorated;
            this.modal = modal;
            this.buttonHost = buttonHost;
            this.newHostClient = newHostClient;
        }
        @Override
        public void run() {
            final String title =
                    this.buttonHost.getTitle()
                    + (this.dialog ? "-Di" : "")
                    + (this.decorated ? "-De" : "")
                    + (this.modal ? "-Mo" : "");
            
            final InterfaceBwdHost newHost;
            if (this.dialog) {
                newHost = this.buttonHost.newDialog(
                        title,
                        this.decorated,
                        this.modal,
                        this.newHostClient);
            } else {
                newHost = getBinding().newHost(
                        title,
                        this.decorated,
                        this.newHostClient);
            }
            lastCreatedHost = newHost;
            
            final GRect oldClientBounds = this.buttonHost.getClientBounds();
            final GRect newClientBounds = oldClientBounds.withPosDeltas(
                    10 + this.buttonBox.x(),
                    10 + this.buttonBox.y());
            newHost.setClientBounds(newClientBounds);

            if (DEBUG) {
                Dbg.log("show()...");
            }
            newHost.show();
            if (DEBUG) {
                Dbg.log("...show()");
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdFont font;
    
    private final List<MyButton> buttonList = new ArrayList<MyButton>();
    
    /**
     * Last host, dialog or not, created using buttons of this client's host.
     * Can be null.
     * If closed, must be nullified.
     */
    private InterfaceBwdHost lastCreatedHost;
    
    /**
     * For last events from this client's host.
     */
    private final BwdEventTestHelper eventTestHelper =
            new BwdEventTestHelper(EVENT_INFO_LIST_MAX_SIZE);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public HostBwdTestCase() {
    }

    public HostBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
        // We want to see a stack trace for each error.
        this.setMustCallOnEventError(true);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new HostBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new HostBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    /*
     * Window events.
     */
    
    @Override
    public void onWindowShown(BwdWindowEvent event) {
        super.onWindowShown(event);
        this.onAnyEvent(event);
    }
    
    @Override
    public void onWindowHidden(BwdWindowEvent event) {
        super.onWindowHidden(event);
        this.onAnyEvent(event);
    }
    
    @Override
    public void onWindowFocusGained(BwdWindowEvent event) {
        super.onWindowFocusGained(event);
        this.onAnyEvent(event);
    }
    
    @Override
    public void onWindowFocusLost(BwdWindowEvent event) {
        super.onWindowFocusLost(event);
        this.onAnyEvent(event);
    }
    
    @Override
    public void onWindowIconified(BwdWindowEvent event) {
        super.onWindowIconified(event);
        this.onAnyEvent(event);
    }
    
    @Override
    public void onWindowDeiconified(BwdWindowEvent event) {
        super.onWindowDeiconified(event);
        this.onAnyEvent(event);
    }
    
    @Override
    public void onWindowMaximized(BwdWindowEvent event) {
        super.onWindowMaximized(event);
        this.onAnyEvent(event);
    }
    
    @Override
    public void onWindowDemaximized(BwdWindowEvent event) {
        super.onWindowDemaximized(event);
        this.onAnyEvent(event);
    }

    @Override
    public void onWindowMoved(BwdWindowEvent event) {
        super.onWindowMoved(event);
        this.onAnyEvent(event);
    }

    @Override
    public void onWindowResized(BwdWindowEvent event) {
        super.onWindowResized(event);
        this.onAnyEvent(event);
    }

    @Override
    public void onWindowClosed(BwdWindowEvent event) {
        super.onWindowClosed(event);
        this.onAnyEvent(event);
    }

    /*
     * Key events.
     */
    
    @Override
    public void onKeyPressed(BwdKeyEventPr event) {
        super.onKeyPressed(event);
        this.onAnyEvent(event);
    }
    
    @Override
    public void onKeyTyped(BwdKeyEventT event) {
        super.onKeyTyped(event);
        this.onAnyEvent(event);
    }
    
    @Override
    public void onKeyReleased(BwdKeyEventPr event) {
        super.onKeyReleased(event);
        this.onAnyEvent(event);
    }
    
    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);
        this.onAnyEvent(event);
        
        if (event.getButton() == BwdMouseButtons.PRIMARY) {
            for (MyButton button : this.buttonList) {
                if (button.box.contains(event.posInClient())) {
                    /*
                     * Executing command asynchronously,
                     * to avoid list's concurrent modification issue
                     * if it causes synchronous painting.
                     */
                    getBinding().getUiThreadScheduler().execute(button.command);
                }
            }
        }
        
        /*
         * Ensuring painting asynchronously, to make sure it gets executed AFTER
         * an eventual previous command of which we want to see the effect.
         */
        getBinding().getUiThreadScheduler().execute(new Runnable() {
            @Override
            public void run() {
                getHost().ensurePendingClientPainting();
            }
        });
    }

    @Override
    public void onMouseReleased(BwdMouseEvent event) {
        super.onMouseReleased(event);
        this.onAnyEvent(event);
    }

    @Override
    public void onMouseClicked(BwdMouseEvent event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMouseEnteredClient(BwdMouseEvent event) {
        super.onMouseEnteredClient(event);
        this.onAnyEvent(event);
    }

    @Override
    public void onMouseExitedClient(BwdMouseEvent event) {
        super.onMouseExitedClient(event);
        this.onAnyEvent(event);
    }

    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        super.onMouseMoved(event);
        this.onAnyEvent(event);
    }

    @Override
    public void onMouseDragged(BwdMouseEvent event) {
        super.onMouseDragged(event);
        this.onAnyEvent(event);
    }

    /*
     * Wheel events.
     */

    @Override
    public void onWheelRolled(BwdWheelEvent event) {
        super.onWheelRolled(event);
        this.onAnyEvent(event);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        if (this.font == null) {
            final InterfaceBwdFontHome fontHome = this.getBinding().getFontHome();
            this.font = fontHome.newFontWithClosestHeight(TARGET_FONT_HEIGHT);
        }
        g.setFont(this.font);

        final GRect box = g.getBox();
        
        /*
         * 
         */
        
        final BwdColor bgColor;
        if (this.getHost().isFocused()) {
            bgColor = COLOR_BG_HOST_FOCUSED;
        } else {
            bgColor = COLOR_BG_HOST_NOT_FOCUSED;
        }
        g.setColor(bgColor);
        g.clearRect(box);
        
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
            BwdTestUtils.drawTextCentered(
                    g,
                    bBox.x(), bBox.y(), bBox.xSpan(), bBox.ySpan(),
                    text);
        }

        return null;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void updateClient(
            InterfaceBwdGraphics g,
            GRect box) {
        
        
        
        g.setColor(BwdColor.BLACK);
        g.drawRect(box);
        
        this.updateHostControlArea(
                g,
                box.withYSpan(CONTROL_AREA_HEIGHT));
        
        this.updateHostCreationArea(
                g,
                box.withPosDeltas(0, CONTROL_AREA_HEIGHT).withYSpan(CREATION_AREA_HEIGHT));
        
        this.updateHostEventArea(
                g,
                box.withPosDeltas(0, CONTROL_AREA_HEIGHT + CREATION_AREA_HEIGHT)
                .withYSpan(Math.max(0, box.ySpan() - (CONTROL_AREA_HEIGHT + CREATION_AREA_HEIGHT))));
    }
    
    /*
     * 
     */
    
    private void updateHostControlArea(
            InterfaceBwdGraphics g,
            GRect box) {
        int remainingWidth = box.xSpan();
        final int subWidth = remainingWidth / 3;
        int tmpX = box.x();
        
        g.setColor(BwdColor.BLACK);
        g.drawRect(box);
        
        {
            final InterfaceBwdHost owner = this.getHost().getOwner();
            if (owner != null) {
                final GRect subBox = GRect.valueOf(
                        tmpX,
                        box.y(),
                        subWidth,
                        box.ySpan());
                this.updateExistingHostControlPanel(g, subBox, "Owner Host:", owner);
            }
            tmpX += subWidth;
        }
        
        {
            final GRect subBox = GRect.valueOf(
                    tmpX,
                    box.y(),
                    subWidth,
                    box.ySpan());
            this.updateExistingHostControlPanel(g, subBox, "This Host:", this.getHost());
            tmpX += subWidth;
        }
        
        if (lastCreatedHost != null) {
            if (lastCreatedHost.isClosed()) {
                lastCreatedHost = null;
            } else {
                final GRect subBox = GRect.valueOf(
                        tmpX,
                        box.y(),
                        subWidth,
                        box.ySpan());
                this.updateExistingHostControlPanel(g, subBox, "Last Created Host:", lastCreatedHost);
            }
            tmpX += subWidth;
        }
    }

    
    private void updateHostCreationArea(
            InterfaceBwdGraphics g,
            GRect box) {
        int remainingWidth = box.xSpan();
        final int subWidth = remainingWidth / 3;
        int tmpX = box.x();
        
        g.setColor(BwdColor.BLACK);
        g.drawRect(box);
        
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
                    box.y(),
                    subWidth,
                    box.ySpan());
            this.updateNewHostControlPanel(
                    g,
                    subBox,
                    title,
                    dialog,
                    modal,
                    this.getHost());
            tmpX += subWidth;
        }
    }
    
    private void updateHostEventArea(
            InterfaceBwdGraphics g,
            GRect box) {
        
        final int h = g.getFont().metrics().height() + 1;
        
        int tmpY = box.y();
        
        g.setColor(BwdColor.BLACK);
        
        for (Object eventInfo : this.eventTestHelper.getEventInfoList()) {
            final String infoStr = eventInfo.toString();
            g.drawText(box.x(), tmpY, infoStr);
            tmpY += h;
        }
    }

    /*
     * 
     */
    
    private void updateExistingHostControlPanel(
            InterfaceBwdGraphics g,
            GRect box,
            String title,
            final InterfaceBwdHost targetHost) {
        
        int tmpX = box.x();
        int tmpY = box.y();
        
        g.setColor(BwdColor.BLACK);
        BwdTestUtils.drawTextCentered(
                g,
                tmpX, tmpY, box.xSpan(), HEIGHT_STEP,
                title);
        tmpY += HEIGHT_STEP;
        
        final int hoiDelayS = HIDDEN_OR_ICONIFIED_DELAY_S;
        
        /*
         * show/hide.
         */

        {
            final int bw = WIDTH_STEP;
            final int bh = HEIGHT_STEP;
            {
                final GRect bBox = GRect.valueOf(tmpX, tmpY, bw, bh);
                drawStateMark(g, bBox, targetHost.isShowing());
                this.buttonList.add(new MyButton("Show", bBox, new Runnable() {
                    @Override
                    public void run() {
                        targetHost.show();
                        afterTargetHostCommand();
                    }
                }));
            }
            {
                final GRect bBox = GRect.valueOf(tmpX + bw, tmpY, bw, bh);
                drawStateMark(g, bBox, !targetHost.isShowing());
                this.buttonList.add(new MyButton("Hide (" + hoiDelayS + "s)", bBox, new Runnable() {
                    @Override
                    public void run() {
                        targetHost.hide();
                        afterTargetHostCommand();
                        
                        getBinding().getUiThreadScheduler().executeAfterS(new Runnable() {
                            @Override
                            public void run() {
                                targetHost.show();
                                afterTargetHostCommand();
                            }
                        }, hoiDelayS);
                    }
                }));
            }
            tmpY += bh;
        }
        
        /*
         * iconify/deiconify.
         */

        {
            final int bw = WIDTH_STEP;
            final int bh = HEIGHT_STEP;
            {
                final GRect bBox = GRect.valueOf(tmpX, tmpY, bw, bh); 
                drawStateMark(g, bBox, !targetHost.isIconified());
                this.buttonList.add(new MyButton("Deico", bBox, new Runnable() {
                    @Override
                    public void run() {
                        targetHost.deiconify();
                        afterTargetHostCommand();
                    }
                }));
            }
            {
                final GRect bBox = GRect.valueOf(tmpX + bw, tmpY, bw, bh); 
                drawStateMark(g, bBox, targetHost.isIconified());
                this.buttonList.add(new MyButton("Ico (" + hoiDelayS + "s)", bBox, new Runnable() {
                    @Override
                    public void run() {
                        targetHost.iconify();
                        afterTargetHostCommand();
                        
                        
                        getBinding().getUiThreadScheduler().executeAfterS(new Runnable() {
                            @Override
                            public void run() {
                                targetHost.deiconify();
                                afterTargetHostCommand();
                            }
                        }, hoiDelayS);
                    }
                }));
            }
            tmpY += bh;
        }
        
        /*
         * maximize/demaximize.
         */
        
        {
            final int bw = WIDTH_STEP;
            final int bh = HEIGHT_STEP;
            {
                final GRect bBox = GRect.valueOf(tmpX, tmpY, bw, bh); 
                drawStateMark(g, bBox, !targetHost.isMaximized());
                this.buttonList.add(new MyButton("Demax", bBox, new Runnable() {
                    @Override
                    public void run() {
                        targetHost.demaximize();
                        afterTargetHostCommand();
                    }
                }));
            }
            {
                final GRect bBox = GRect.valueOf(tmpX + bw, tmpY, bw, bh); 
                drawStateMark(g, bBox, targetHost.isMaximized());
                this.buttonList.add(new MyButton("Max", bBox, new Runnable() {
                    @Override
                    public void run() {
                        targetHost.maximize();
                        afterTargetHostCommand();
                    }
                }));
            }
            tmpY += bh;
        }
        
        /*
         * Host focus.
         */
        
        {
            final int bw = 2 * WIDTH_STEP;
            final int bh = HEIGHT_STEP;
            {
                final GRect bBox = GRect.valueOf(tmpX, tmpY, bw, bh); 
                drawStateMark(g, bBox, targetHost.isFocused());
                this.buttonList.add(new MyButton("req. focus", bBox, new Runnable() {
                    @Override
                    public void run() {
                        targetHost.requestFocusGain();
                        afterTargetHostCommand();
                    }
                }));
            }
            tmpY += bh;
        }
        
        /*
         * Closing.
         */
        
        {
            final int bw = 2 * WIDTH_STEP;
            final int bh = HEIGHT_STEP;
            final GRect bBox = GRect.valueOf(tmpX, tmpY, bw, bh);
            this.buttonList.add(new MyButton("Close", bBox, new Runnable() {
                @Override
                public void run() {
                    targetHost.close();
                    afterTargetHostCommand();
                }
            }));
            tmpY += bh;
        }
    }
    
    private void afterTargetHostCommand() {
        // To update state display, for all hosts
        // (since some display state of others).
        final InterfaceBwdBinding binding = getBinding();
        final InterfaceScheduler scheduler = binding.getUiThreadScheduler();
        scheduler.executeAfterS(new Runnable() {
            @Override
            public void run() {
                for (InterfaceBwdHost host : binding.getAllHostList()) {
                    host.makeAllDirtyAndEnsurePendingClientPainting();
                }
            }
        }, REPAINT_DELAY_S);
    }
    
    private static void drawStateMark(InterfaceBwdGraphics g, GRect rect, boolean state) {
        if (state) {
            g.setColor(BwdColor.BLACK);
            g.drawOval(rect);
        }
    }

    private void updateNewHostControlPanel(
            InterfaceBwdGraphics g,
            GRect box,
            String title,
            boolean dialog,
            boolean modal,
            InterfaceBwdHost host) {
        /*
         * new root host:  new dialog (non-modal):  new dialog host (modal):
         * (decorated)     (decorated)              (decorated)
         * (naked)         (naked)                  (naked)
         */
        g.setColor(BwdColor.BLACK);
        g.drawText(box.x(), box.y(), title);
        
        for (int i = 0; i < 2; i++) {
            final boolean decorated = (i == 0);
            final GRect bBox = GRect.valueOf(
                    box.x(),
                    box.y() + (i + 1) * HEIGHT_STEP,
                    WIDTH_STEP,
                    HEIGHT_STEP);
            final MyNewHostButtonCommand command = new MyNewHostButtonCommand(
                    bBox,
                    dialog,
                    decorated,
                    modal,
                    this.getHost(),
                    this.newClient());
            final MyButton button = new MyButton(
                    (decorated ? "decorated" : "naked"),
                    bBox,
                    command);
            this.buttonList.add(button);
        }
    }
    
    /*
     * 
     */
    
    private void onAnyEvent(BwdEvent event) {
        final long eventTimeNs = this.getBinding().getUiThreadScheduler().getClock().getTimeNs();
        this.eventTestHelper.addBwdEventInfo(event, eventTimeNs);
        
        /*
         * Ensuring pending repaint on each event,
         * not bothering to compute whether it's actually needed.
         */
        getHost().ensurePendingClientPainting();
    }
}
