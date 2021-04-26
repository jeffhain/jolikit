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
package net.jolikit.bwd.impl.misc;

import java.util.List;

import net.jolikit.bwd.api.AbstractBwdClient;
import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * Sample usage of BWD (window/mouse/key events, window alpha, graphics, fonts)
 * and (periodic) scheduling using UI thread scheduler.
 * 
 * Creates a window indicating mouse position on screen.
 */
public class SampleBwd {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final int MOUSE_POS_FONT_HEIGHT = 50;
    private static final double MOUSE_POS_POLL_PERIOD_S = 0.01;

    private static final int CLIENT_WIDTH = MOUSE_POS_FONT_HEIGHT * 15;
    private static final int CLIENT_HEIGHT = MOUSE_POS_FONT_HEIGHT + 20;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyClient extends AbstractBwdClient {
        private final InterfaceBwdBinding binding;
        private InterfaceBwdFont myFont = null;
        /**
         * (0,0) and not null by default.
         * When the backing library doesn't provide a way to read mouse position
         * aside from mouse events, binding's getMousePosInScreen() method typically
         * returns the position from the last mouse event.
         * When no event occurred yet, it typically returns (0,0).
         * Using this value by default prevents us from detecting a mouse position
         * change to (0,0) early on, which would cause us to top-left corner
         * the window on start.
         */
        private GPoint lastMousePos = GPoint.ZERO;
        /**
         * Periodic treatment to be aware of mouse position updates
         * when mouse moved outside of client area.
         * 
         * Won't help if the backing library doesn't provide a way
         * to read mouse position aside from mouse events.
         */
        private final AbstractProcess mousePoll;
        public MyClient(final InterfaceBwdBinding binding) {
            this.binding = binding;
            this.mousePoll = new AbstractProcess(binding.getUiThreadScheduler()) {
                @Override
                protected long process(long theoreticalTimeNs, long actualTimeNs) {
                    
                    final GPoint newMousePos = binding.getMousePosInScreen();
                    if (!newMousePos.equals(lastMousePos)) {
                        if (DEBUG) {
                            System.out.println("process : newMousePos = " + newMousePos);
                        }
                        lastMousePos = newMousePos;
                        
                        onNewMousePos();
                    }
                    
                    return actualTimeNs + sToNs(MOUSE_POS_POLL_PERIOD_S);
                }
            };
        }
        @Override
        public void onWindowShown(BwdWindowEvent event) {
            System.out.println(event);
            
            if (!this.getHost().isIconified()) {
                this.mousePoll.start();
            }
        }
        @Override
        public void onWindowHidden(BwdWindowEvent event) {
            System.out.println(event);
            
            this.mousePoll.stop();
        }
        @Override
        public void onWindowIconified(BwdWindowEvent event) {
            System.out.println(event);
            
            this.mousePoll.stop();
        }
        @Override
        public void onWindowDeiconified(BwdWindowEvent event) {
            System.out.println(event);
            
            this.mousePoll.start();
        }
        @Override
        public void onWindowClosed(BwdWindowEvent event) {
            System.out.println(event);
            
            InterfaceBwdFont myFont = this.myFont;
            if (myFont != null) {
                this.myFont = null;
                myFont.dispose();
            }
            
            this.mousePoll.stop();
        }
        @Override
        public void onKeyPressed(BwdKeyEventPr event) {
            System.out.println(event);
            
            if (event.getKey() == BwdKeys.X) {
                /*
                 * We don't want to just close the host,
                 * we want to terminate all the "app".
                 */
                if (false) {
                    this.getHost().close();
                } else {
                    this.binding.shutdownAbruptly();
                }
            }
        }
        @Override
        public void onMouseMoved(BwdMouseEvent event) {
            this.lastMousePos = event.posInScreen();
            
            onNewMousePos();
        }
        @Override
        public List<GRect> paintClient(
                InterfaceBwdGraphics g,
                GRect dirtyRect) {
            
            final GRect clientBox = g.getBox();
            if (DEBUG) {
                System.out.println("paintClient_gInitDone(...) : clientBox = " + clientBox);
            }

            g.setColor(BwdColor.WHITE);
            g.clearRect(clientBox);
            
            /*
             * 
             */
            
            g.setColor(BwdColor.BLACK);
            g.drawRect(clientBox);
            
            /*
             * 
             */
            
            final int defaultFontHeight = g.getFont().metrics().height();
            
            g.drawText(2, 2, "Press X key to close.");
            
            /*
             * 
             */
            
            InterfaceBwdFont myFont = this.myFont;
            if (myFont == null) {
                final InterfaceBwdFontHome fontHome = this.binding.getFontHome();
                myFont = fontHome.newFontWithClosestHeight(MOUSE_POS_FONT_HEIGHT);
                this.myFont = myFont;
            }
            g.setFont(myFont);
            
            final GPoint mousePosInScreen = this.binding.getMousePosInScreen();
            final String text;
            if (false) {
                /*
                 * TODO awt On Mac and with AWT,
                 * this mysteriously only draws "mouse   xxx, yyy",
                 * and not "mouse: [xxx, yyy]" as expected.
                 * Adding '_' causes ':', '[' and ']' to be properly drawn.
                 */
                text = "mouse: " + mousePosInScreen;
            } else {
                text = "mouse_pos: " + mousePosInScreen;
            }
            g.drawText(
                    2,
                    2 + (defaultFontHeight + 1),
                    text);
            
            return null;
        }
        private void onNewMousePos() {
            
            final GRect clientBounds = this.getHost().getClientBounds();
            if (DEBUG) {
                System.out.println("onNewMousePos() : clientBounds = " + clientBounds);
            }
            if (clientBounds.isEmpty()) {
                // Might be iconified or hidden.
            } else {
                final GPoint mousePos = this.lastMousePos;
                final int mx = mousePos.x();
                final int my = mousePos.y();
                final GRect screenBounds = this.binding.getScreenBounds();
                if (DEBUG) {
                    System.out.println("onNewMousePos() : mouse pos = " + mousePos);
                    System.out.println("onNewMousePos() : screenBounds = " + screenBounds);
                }

                // Negative to still work if no way to read mouse position
                // aside from events.
                final int leash = -15;
                
                final GRect targetClientBounds = GRect.valueOf(
                        mx + ((mx < screenBounds.xMid()) ? + leash : - leash - clientBounds.xSpan()),
                        my + ((my < screenBounds.yMid()) ? + leash : - leash - clientBounds.ySpan()),
                        CLIENT_WIDTH,
                        CLIENT_HEIGHT);
                // Safer to set bounds asynchronously, in case
                // it would ever generate mouse moved events synchronously.
                if (DEBUG) {
                    System.out.println("scheduling host.setClientBounds(" + targetClientBounds + ")");
                }
                this.binding.getUiThreadScheduler().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (DEBUG) {
                            System.out.println("host.setClientBounds(" + targetClientBounds + ")");
                        }
                        getHost().setClientBounds(targetClientBounds);
                        
                        if (false) {
                            // No need to do that, because bounds setting
                            // should ensure painting already.
                            getHost().ensurePendingClientPainting();
                        }
                    }
                });
            }
        }
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void playWithBinding(final InterfaceBwdBinding binding) {
        final InterfaceScheduler uiThreadScheduler = binding.getUiThreadScheduler();

        uiThreadScheduler.execute(new Runnable() {
            @Override
            public void run() {
                final InterfaceBwdFontHome fontHome = binding.getFontHome();
                fontHome.loadSystemAndUserFonts(null);
                
                final boolean decorated = false;
                final InterfaceBwdClient client = new MyClient(binding);
                final InterfaceBwdHost host = binding.newHost(
                        "Mouse Pos",
                        decorated,
                        client);
                
                // So that we can see what the mouse is over,
                // behind our window.
                // NB: Some bindings might not support window transparency.
                host.setWindowAlphaFp(0.5);

                final GRect screenBounds = binding.getScreenBounds();
                
                final GRect targetClientBounds = GRect.valueOf(
                        screenBounds.xMid(),
                        screenBounds.yMid(),
                        CLIENT_WIDTH,
                        CLIENT_HEIGHT);
                
                if (DEBUG) {
                    System.out.println("host.setClientBounds(" + targetClientBounds + ") (creation)");
                }
                host.setClientBounds(targetClientBounds);
                
                if (DEBUG) {
                    System.out.println("host.show()");
                }
                host.show();
            }
        });
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private SampleBwd() {
    }
}
