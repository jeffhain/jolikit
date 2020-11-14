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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdClientMock;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;

/**
 * Uses host.setClientBounds(...) or host.setWindowBounds(...)
 * to set bounds at each left click, such as the drawn cross center
 * should not move, and such as bounds should change cyclically
 * and not drift, and shows bounds obtained from corresponding
 * getters, which must correspond to those set.
 * 
 * Middle click closes the host.
 * 
 * Other than initial host, which is not resized,
 * four resized-on-click hosts are created:
 * dialog or not, decorated or not.
 * Clicking on initial host resize all four of them,
 * clicking on a resized-on-click host only resizes it.
 * 
 * - awt: ok
 * - swing: ok
 * - jfx: ok
 * 
 * - swt: ok
 * - lwjgl: ok
 * - jogl: ok
 * 
 * - qtj: ok
 * - algr: ok
 * - sdl: ok
 */
public class HostBoundsSetGetBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final int GROWN_SHRINK_BASE_AMOUNT = 10;

    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyClientMock extends BwdClientMock {
        public MyClientMock() {
            this.configureUiThreadCheck(getBinding().getUiThreadScheduler());
        }
        @Override
        public void onMousePressed(BwdMouseEvent event) {
            if (DEBUG) {
                Dbg.logPr(this, "onMousePressed(" + event + ")");
            }
            super.onMousePressed(event);
            
            final InterfaceBwdHost host = this.getHost();
            
            if (event.getButton() == BwdMouseButtons.PRIMARY) {
                final MyHostData data = dataByHost.get(host);
                setBoundsAndUpdateData(host, data);
            } else if (event.getButton() == BwdMouseButtons.MIDDLE) {
                host.close();
            }
        }
        @Override
        public void onWindowClosed(BwdWindowEvent event) {
            if (DEBUG) {
                Dbg.logPr(this, "onWindowClosed(" + event + ")");
            }
            super.onWindowClosed(event);
            
            removeHost(this.getHost());
        }
        @Override
        protected List<GRect> paintClientImpl(
                InterfaceBwdGraphics g,
                GRect dirtyRect) {

            final GRect gBox = g.getBox();
            
            final InterfaceBwdHost host = this.getHost();
            final MyHostData data = dataByHost.get(host);
            
            final GRect insets = host.getInsets();
            
            final GRect expectedClientBounds;
            if (data.targetClientBounds != null) {
                expectedClientBounds = data.targetClientBounds;
            } else if (data.targetWindowBounds != null) {
                final int left = insets.x();
                final int top = insets.y();
                final int right = insets.xSpan();
                final int bottom = insets.ySpan();
                expectedClientBounds = GRect.valueOf(
                        data.targetWindowBounds.x() + left,
                        data.targetWindowBounds.y() + top,
                        data.targetWindowBounds.xSpan() - (left + right),
                        data.targetWindowBounds.ySpan() - (top + bottom));
            } else {
                expectedClientBounds = null;
            }
            
            final GRect readClientBounds = host.getClientBounds();

            /*
             * 
             */
            
            g.setColor(BwdColor.WHITE);
            g.fillRect(gBox);
            
            /*
             * 
             */
            
            g.setColor(BwdColor.BLACK);
            
            final int fh = g.getFont().metrics().height();
            final int tlo = 10;
            final int th = fh + 1;
            
            g.drawText(th, th, host.getTitle());
            
            int textY = gBox.yMid();

            if (data.targetClientBounds != null) {
                g.drawText(tlo, textY, "last set bounds type: client:");
                g.drawText(tlo, textY + th, "" + data.targetClientBounds);
            } else if (data.targetWindowBounds != null) {
                g.drawText(tlo, textY, "last set bounds type: window:");
                g.drawText(tlo, textY + th, "" + data.targetWindowBounds);
            }
            textY += 2 * th;

            g.drawText(tlo, textY, "insets = " + insets);
            textY += th;
            
            if (expectedClientBounds != null) {
                // To check that figure fits exactly in actual client bounds.
                final GRect clientInClient = expectedClientBounds.withPos(0, 0);
                drawFigureInRect(g, clientInClient);
            }
            
            if (expectedClientBounds != null) {
                g.drawText(tlo, textY, "exp. c.b. = " + expectedClientBounds);
            }
            textY += th;
            
            // To compare read client bounds to expected client bounds.
            g.drawText(tlo, textY, "read c.b. = " + readClientBounds);
            textY += th;

            return GRect.DEFAULT_HUGE_IN_LIST;
        }
    }
    
    private static class MyHostData {
        /**
         * Counter to know how to change bounds on click.
         */
        int counter = 0;
        /**
         * Only non-null if did set client bounds last.
         */
        GRect targetClientBounds = null;
        /**
         * Only non-null if did set window bounds last.
         */
        GRect targetWindowBounds = null;
        public MyHostData() {
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * For determinism, in case some treatments depend on the order on which
     * we iterate over hosts.
     * Supposes that each host has its own title.
     */
    private static final Comparator<InterfaceBwdHost> HOST_COMPARATOR = new Comparator<InterfaceBwdHost>() {
        @Override
        public int compare(InterfaceBwdHost o1, InterfaceBwdHost o2) {
            return o1.getTitle().compareTo(o2.getTitle());
        }
    };
    
    private boolean hostsCreated = false;
    
    private final Set<InterfaceBwdHost> hostSet = new TreeSet<InterfaceBwdHost>(HOST_COMPARATOR);
    
    private final Map<InterfaceBwdHost,MyHostData> dataByHost = new TreeMap<InterfaceBwdHost,MyHostData>(HOST_COMPARATOR);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public HostBoundsSetGetBwdTestCase() {
    }

    public HostBoundsSetGetBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new HostBoundsSetGetBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new HostBoundsSetGetBwdTestCase(this.getBinding());
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
        if (DEBUG) {
            Dbg.logPr(this, "onMousePressed(" + event + ")");
        }
        super.onMousePressed(event);

        if (event.getButton() == BwdMouseButtons.PRIMARY) {
            if (!this.hostsCreated) {
                /*
                 * Only creating sub-host on first click,
                 * not at first painting, because with some bindings
                 * it takes time for main window to end up with
                 * specified initial bounds, and that could cause
                 * sub-hosts to be out of screen or with tiny spans. 
                 */
                this.createHosts();
                this.hostsCreated = true;
            } else {
                for (Map.Entry<InterfaceBwdHost,MyHostData> entry : this.dataByHost.entrySet()) {
                    final InterfaceBwdHost host = entry.getKey();
                    final MyHostData data = entry.getValue();
                    setBoundsAndUpdateData(host, data);
                }
            }
        } else if (event.getButton() == BwdMouseButtons.MIDDLE) {
            for (Object obj : this.dataByHost.keySet().toArray()) {
                final InterfaceBwdHost host = (InterfaceBwdHost) obj;
                host.close();
            }
            this.getHost().close();
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final GRect gBox = g.getBox();

        g.setColor(BwdColor.WHITE);
        g.fillRect(gBox);
        
        g.setColor(BwdColor.BLACK);
        g.drawText(gBox.xMid(), gBox.yMid(), "Click Here");
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void createHosts() {
        if (this.hostsCreated) {
            throw new IllegalStateException("hosts already created");
        }
        
        final InterfaceBwdHost mainHost = this.getHost();
        final GRect mainClientBounds = mainHost.getClientBounds();
        
        for (boolean dialog : new boolean[]{false,true}) {
            for (boolean decorated : new boolean[]{false,true}) {
                final int dx = (decorated ? -INITIAL_WIDTH : INITIAL_WIDTH);
                final int dy = (dialog ? INITIAL_HEIGHT : -INITIAL_HEIGHT);
                final GRect subClientBounds = mainClientBounds.withPosDeltas(dx, dy);
                
                final InterfaceBwdHost subHost;
                if (dialog) {
                    final String title = "DIA-" + (decorated ? "DEC" : "UND");
                    final boolean modal = false;
                    final MyClientMock subClient = new MyClientMock();
                    subHost = mainHost.newDialog(title, decorated, modal, subClient);
                } else {
                    final String title = "REG-" + (decorated ? "DEC" : "UND");
                    final MyClientMock subClient = new MyClientMock();
                    subHost = this.getBinding().newHost(title, decorated, subClient);
                }
                
                this.hostSet.add(subHost);
                
                final MyHostData data = new MyHostData();
                data.targetClientBounds = subClientBounds;
                this.dataByHost.put(subHost, data);
                
                subHost.setClientBounds(subClientBounds);
                subHost.show();
            }
        }
    }
    
    private static void setBoundsAndUpdateData(InterfaceBwdHost host, MyHostData data) {
        final int value_0_3 = (data.counter++ % 4);
        
        // Starting by growths, to avoid to ever get smaller
        // than initial bounds, which might not be possible.
        final boolean mustGrowElseShrink = (value_0_3 < 2);
        final boolean mustSetClientElseWindowBounds = ((value_0_3 & 1) == 0);
        
        final int deltaSignFactor = (mustGrowElseShrink ? 1 : -1);
        final int deltaScaleFactor = (mustSetClientElseWindowBounds ? 1 : 2);
        final int delta = deltaSignFactor * deltaScaleFactor * GROWN_SHRINK_BASE_AMOUNT;
        
        /*
         * Applying grow/shrink to current bounds,
         * to detect eventual drift due to target bounds
         * not being met.
         * Changing both position and spans,
         * to check that both are updated correctly.
         */
        
        final GRect oldBounds = (mustSetClientElseWindowBounds ? host.getClientBounds() : host.getWindowBounds());
        if (DEBUG) {
            Dbg.log(hid(host) + " : mustGrowElseShrink = " + mustGrowElseShrink);
            Dbg.log(hid(host) + " : mustSetClientElseWindowBounds = " + mustSetClientElseWindowBounds);
            Dbg.log(hid(host) + " : delta = " + delta);
            Dbg.log(hid(host) + " : oldBounds = " + oldBounds);
        }
        
        final GRect newBounds = GRect.valueOf(
                oldBounds.x() - delta,
                oldBounds.y() - delta,
                oldBounds.xSpan() + 2 * delta,
                oldBounds.ySpan() + 2 * delta);
        if (DEBUG) {
            Dbg.log(hid(host) + " : newBounds = " + newBounds);
        }
        
        if (mustSetClientElseWindowBounds) {
            data.targetClientBounds = newBounds;
            data.targetWindowBounds = null;
            host.setClientBounds(newBounds);
        } else {
            data.targetClientBounds = null;
            data.targetWindowBounds = newBounds;
            host.setWindowBounds(newBounds);
        }
    }
    
    /**
     * Draws lines, to help figure out client positioning.
     * 
     * Using theoretical spans, so that we can check that client area spans
     * used for drawing are the proper ones (in OS pixels by default,
     * and not in device pixels).
     */
    private static void drawFigureInRect(
            InterfaceBwdGraphics g,
            GRect rect) {
        
        /*
         * Border, for boundaries.
         */
        
        BwdTestUtils.drawRectStipple(g, rect);
        
        /*
         * Diamond, for boundaries.
         */
        
        g.drawLine(rect.xMid(), rect.y(), rect.xMax(), rect.yMid());
        g.drawLine(rect.xMax(), rect.yMid(), rect.xMid(), rect.yMax());
        g.drawLine(rect.xMid(), rect.yMax(), rect.x(), rect.yMid());
        g.drawLine(rect.x(), rect.yMid(), rect.xMid(), rect.y());
        
        /*
         * Cross, for location.
         */
        
        g.drawLine(rect.xMid(), rect.y(), rect.xMid(), rect.yMax());
        g.drawLine(rect.x(), rect.yMid(), rect.xMax(), rect.yMid());
    }
    
    private void removeHost(InterfaceBwdHost host) {
        this.hostSet.remove(host);
        this.dataByHost.remove(host);
    }
    
    /**
     * To help identity the host in logs.
     */
    private static Object hid(InterfaceBwdHost host) {
        return host.getTitle();
    }
}
