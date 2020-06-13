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
package net.jolikit.bwd.impl.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.PrintStream;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.awt.AbstractAwtBwdBinding;
import net.jolikit.bwd.impl.awt.AwtBwdCursorManager;
import net.jolikit.bwd.impl.awt.AwtEventConverter;
import net.jolikit.bwd.impl.awt.AwtGraphicBuffer;
import net.jolikit.bwd.impl.awt.AwtHostBoundsHelper;
import net.jolikit.bwd.impl.awt.AwtPaintUtils;
import net.jolikit.bwd.impl.awt.AwtUtils;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.InterfaceHostLifecycleListener;
import net.jolikit.bwd.impl.utils.basics.BindingCallIfExistsUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.InterfaceFactory;

public class SwingBwdHost extends AbstractBwdHost {

    /*
     * Can help: AWT/Swing paint mess description:
     * http://www.oracle.com/technetwork/java/painting-140037.html
     */
    
    /*
     * How to hunt down rogue fills by the backing library:
     * Need to deactivate offscreen buffer, and not to paint anything,
     * such as whatever is behind the window shows up if no rogue fill.
     * Or maybe the same tactics as for AWT applies as well.
     */
    
    /*
     * TODO swing Repaint mess:
     * 
     * Overriding repaint(long,int,int,int,int) to make it do nothing, or call
     * itself some time later, and also shrinking the JFrame on either X or Y
     * (doesn't happen if only growing it), can make two weird things happen:
     * 
     * 1) Automatic creation (happens once) of a BufferedStrategy (previously null):
     *     getBufferStrategy() = java.awt.Component$BltSubRegionBufferStrategy@145c761
     *     getCapabilities() = java.awt.BufferCapabilities@24c672
     *     getCapabilities().getFlipContents() = null
     *     getCapabilities().getBackBufferCapabilities().isAccelerated() = true
     *     getCapabilities().getBackBufferCapabilities().isTrueVolatile() = false
     *     getCapabilities().getFrontBufferCapabilities().isAccelerated() = true
     *     getCapabilities().getFrontBufferCapabilities().isTrueVolatile() = false
     *   due to the call stack:
     *     at javax.swing.BufferStrategyPaintManager$BufferInfo.createBufferStrategy(BufferStrategyPaintManager.java:863)
     *     at javax.swing.BufferStrategyPaintManager$BufferInfo.createBufferStrategy(BufferStrategyPaintManager.java:814)
     *     at javax.swing.BufferStrategyPaintManager$BufferInfo.getBufferStrategy(BufferStrategyPaintManager.java:747)
     *     at javax.swing.BufferStrategyPaintManager.prepare(BufferStrategyPaintManager.java:523)
     *     at javax.swing.BufferStrategyPaintManager.paint(BufferStrategyPaintManager.java:281)
     *     at javax.swing.RepaintManager.paint(RepaintManager.java:1219)
     *     at javax.swing.JComponent._paintImmediately(JComponent.java:5168)
     *     at javax.swing.JComponent.paintImmediately(JComponent.java:4979)
     *     at javax.swing.RepaintManager.paintDirtyRegions(RepaintManager.java:782)
     *     (...)
     *   possibly because paint(RepaintManager.java:1219) failed some painting.
     * 
     * 2) At least 3 successive fillRect:
     *   One comes from content pane, due to the call stack
     *   (catched by printing call stack in getBackground()):
     *     at javax.swing.plaf.ComponentUI.update(ComponentUI.java:158)
     *     at javax.swing.JComponent.paintComponent(JComponent.java:778)
     *     at net.jolikit.bwt.hosts.DefaultContentPane.paintComponent(DefaultContentPane.java:192)
     *     at javax.swing.JComponent.paintToOffscreen(JComponent.java:5224)
     *     at javax.swing.RepaintManager$PaintManager.paintDoubleBuffered(RepaintManager.java:1503)
     *     at javax.swing.RepaintManager$PaintManager.paint(RepaintManager.java:1426)
     *     at javax.swing.BufferStrategyPaintManager.paint(BufferStrategyPaintManager.java:311)
     *     at javax.swing.RepaintManager.paint(RepaintManager.java:1219)
     *     at javax.swing.JComponent.paint(JComponent.java:1040)
     *     at net.jolikit.bwt.hosts.DefaultContentPane.paint(DefaultContentPane.java:174)
     *     at javax.swing.JComponent.paintChildren(JComponent.java:887)
     *     at javax.swing.JComponent.paint(JComponent.java:1063)
     *     at javax.swing.JLayeredPane.paint(JLayeredPane.java:585)
     *     (...)
     *   where update(ComponentUI.java:158) is:
     *     public void update(Graphics g, JComponent c) {
     *       if (c.isOpaque()) {
     *         g.setColor(c.getBackground());
     *         g.fillRect(0, 0, c.getWidth(),c.getHeight());
     *       }
     *       paint(g, c);
     *     }
     *   Other fillRects are at least two, and can be discriminated due to
     *   flickering, when setting a specific background color to root pane.
     * 
     * Automatic BufferedStrategy creation can be avoided by making root pane
     * not double buffered.
     * The first fillRect can be suppressed by making content pane not opaque,
     * and all of them by making root pane not opaque.
     * 
     * We actually call super.repaint(long,4int) from within our implementation,
     * but in case any too long delay would cause above annoyances (useless
     * overheads) to appear, we take care to always set double buffered to false
     * for root pane, and opaque to false for both content pane and root pane.
     * 
     * Note that if "swing.handleTopLevelPaint" property is false, the JFrame
     * will be opaque anyway.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * True so that offscreen buffer resizing does not leak trough areas that
     * are left unpaint, if any.
     * 
     * Since addition of pixel reading methods in BWD graphics,
     * must be true, so that pixels from previous painting can properly be read.
     */
    private static final boolean MUST_PRESERVE_OB_CONTENT_ON_RESIZE = true;

    private static final boolean ALLOW_OB_SHRINKING = true;
    
    /**
     * Can make things faster in some cases
     * (even though these cases might no longer occur with current code).
     */
    private static final boolean MUST_USE_INT_ARRAY_RASTER = true;

    /**
     * TODO swing Allows to avoid a useless fillRect.
     */
    private static final boolean ANTI_CHEEKY_FILL_RECT = true;
    
    /**
     * On Windows, on one hand, it seems alright if JFrame.repaint(long,4int)
     * just causes call to paintClientNowOrLater() later, but on the other hand,
     * it doesn't seem to add much overhead to just call super.repaint(...),
     * so we do it.
     */
    private static final int REPAINT_CALL_0_SUPER_1_EPCPWDR = 0;
    
    private static final int PAINT_CALL_0_SUPER_1_PAINT_COMPONENTS_2_PCNOG = (ANTI_CHEEKY_FILL_RECT ? 1 : 0);
    
    /**
     * TODO swing Seems needed for AWT (Frame), but not Swing (JFrame),
     * but we set it to true anyway, just in case.
     */
    private static final int PAINT_COMPONENTS_CALL_0_SUPER_1_PCNOG_2_EPCPWDR = (ANTI_CHEEKY_FILL_RECT ? 2 : 0);
    
    private static final int PCNOL_CALL_0_REPAINT_1_PCNOWG = 1;
    
    /**
     * To avoid surprise fillRect if repaint(long,4int) gets slacky
     * (see comments above).
     */
    private static final boolean NOT_OPAQUE_CONTENT_PANE = true;

    /**
     * To avoid surprise fillRect if repaint(long,4int) gets slacky
     * (see comments above).
     */
    private static final boolean NOT_OPAQUE_ROOT_PANE = true;

    /**
     * For content pane, only reshape(4int) seems always called and useful.
     * repaint(long,4int) is also called but seems useless.
     */
    private static final boolean CONTENT_PANE_REPAINT_DISABLED = true;

    /**
     * TODO swing Maybe using content pane graphics usage
     * makes some above workarounds useless, but it doesn't seem
     * to add any issue.
     * Doing it just for the sake of minimalism, and to be more different
     * than AWT host.
     * If causes some issue, should switch back to using window graphics.
     */
    private static final boolean MUST_PAINT_ON_CONTENT_PANE_GRAPHICS = true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /*
     * MyJFrame and MyJDialog contain similar code, but would be over-engineering
     * to factor it due to necessary overrides for use of super.xxx methods, and
     * due to eventual differences.
     */
    
    private class MyJFrame extends JFrame {
        private static final long serialVersionUID = 1L;
        public MyJFrame() {
        }
        @Override
        public void repaint(long time, int x, int y, int width, int height) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".repaint(" + time + "," + x + "," + y + "," + width + "," + height + ")");
            }
            if (REPAINT_CALL_0_SUPER_1_EPCPWDR == 0) {
                // paintClientNowOrLater() shall be called later anyway.
                super.repaint(time, x, y, width, height);
            } else if (REPAINT_CALL_0_SUPER_1_EPCPWDR == 1) {
                final GRect dirtyRect = AwtUtils.dirtyRectMovedFromWindowToClient(
                        this,
                        x, y, width, height);
                makeDirtyAndEnsurePendingClientPainting(dirtyRect);
            }
        }
        @Override
        public void paintAll(Graphics g) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".paintAll(" + g + ")");
            }
            super.paintAll(g);
        }

        @Override
        public void paint(Graphics g) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".paint(" + g + ")");
            }
            if (PAINT_CALL_0_SUPER_1_PAINT_COMPONENTS_2_PCNOG == 0) {
                super.paint(g);
            } else if (PAINT_CALL_0_SUPER_1_PAINT_COMPONENTS_2_PCNOG == 1) {
                this.paintComponents(g);
            } else if (PAINT_CALL_0_SUPER_1_PAINT_COMPONENTS_2_PCNOG == 2) {
                makeAllDirty();
                paintClientNowOnG((Graphics2D) g);
            }
        }
        @Override
        public void update(Graphics g) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".update(" + g + ")");
            }
            super.update(g);
        }
        @Override
        public void paintComponents(Graphics g) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".paintComponents(" + g + ")");
            }
            if (isShowing()) {
                final GRect dirtyRect;
                if (false) {
                    dirtyRect = AwtUtils.dirtyRectMovedFromWindowToClient(this, g);
                } else {
                    /*
                     * Don't seem we need it for Swing, but making all dirty
                     * as done in AWT, just in case, since we don't pass here often.
                     */
                    dirtyRect = GRect.DEFAULT_HUGE;
                }
                if (PAINT_COMPONENTS_CALL_0_SUPER_1_PCNOG_2_EPCPWDR == 0) {
                    super.paintComponents(g);
                } else if (PAINT_COMPONENTS_CALL_0_SUPER_1_PCNOG_2_EPCPWDR == 1) {
                    makeDirty(dirtyRect);
                    paintClientNowOnG((Graphics2D) g);
                } else if (PAINT_COMPONENTS_CALL_0_SUPER_1_PCNOG_2_EPCPWDR == 2) {
                    makeDirtyAndEnsurePendingClientPainting(dirtyRect);
                }
            }
        }
        @Override
        public boolean requestFocus(boolean temporary) {
            // Overriding because super is protected.
            return super.requestFocus(temporary);
        }
    }
    
    private class MyJDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        public MyJDialog(Window owner) {
            super(owner);
        }
        @Override
        public void repaint(long time, int x, int y, int width, int height) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".repaint(" + time + "," + x + "," + y + "," + width + "," + height + ")");
            }
            if (REPAINT_CALL_0_SUPER_1_EPCPWDR == 0) {
                // paintClientNowOrLater() shall be called later anyway.
                super.repaint(time, x, y, width, height);
            } else if (REPAINT_CALL_0_SUPER_1_EPCPWDR == 1) {
                final GRect dirtyRect = AwtUtils.dirtyRectMovedFromWindowToClient(
                        this,
                        x, y, width, height);
                makeDirtyAndEnsurePendingClientPainting(dirtyRect);
            }
        }
        @Override
        public void paintAll(Graphics g) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".paintAll(" + g + ")");
            }
            super.paintAll(g);
        }

        @Override
        public void paint(Graphics g) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".paint(" + g + ")");
            }
            if (PAINT_CALL_0_SUPER_1_PAINT_COMPONENTS_2_PCNOG == 0) {
                super.paint(g);
            } else if (PAINT_CALL_0_SUPER_1_PAINT_COMPONENTS_2_PCNOG == 1) {
                this.paintComponents(g);
            } else if (PAINT_CALL_0_SUPER_1_PAINT_COMPONENTS_2_PCNOG == 2) {
                makeAllDirty();
                paintClientNowOnG((Graphics2D) g);
            }
        }
        @Override
        public void update(Graphics g) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".update(" + g + ")");
            }
            super.update(g);
        }
        @Override
        public void paintComponents(Graphics g) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".paintComponents(" + g + ")");
            }
            if (isShowing()) {
                final GRect dirtyRect;
                if (false) {
                    dirtyRect = AwtUtils.dirtyRectMovedFromWindowToClient(this, g);
                } else {
                    /*
                     * Don't seem we need it for Swing, but making all dirty
                     * as done in AWT, just in case, since we don't pass here often.
                     */
                    dirtyRect = GRect.DEFAULT_HUGE;
                }
                if (PAINT_COMPONENTS_CALL_0_SUPER_1_PCNOG_2_EPCPWDR == 0) {
                    super.paintComponents(g);
                } else if (PAINT_COMPONENTS_CALL_0_SUPER_1_PCNOG_2_EPCPWDR == 1) {
                    makeDirty(dirtyRect);
                    paintClientNowOnG((Graphics2D) g);
                } else if (PAINT_COMPONENTS_CALL_0_SUPER_1_PCNOG_2_EPCPWDR == 2) {
                    makeDirtyAndEnsurePendingClientPainting(dirtyRect);
                }
            }
        }
        @Override
        public boolean requestFocus(boolean temporary) {
            // Overriding because super is protected.
            return super.requestFocus(temporary);
        }
    }
    
    /*
     * 
     */
    
    private static class MyContentPane extends JPanel {
        private static final long serialVersionUID = 1L;
        public MyContentPane() {
            // As done in JDK.
            this.setLayout(new BorderLayout() {
                private static final long serialVersionUID = 1L;
                /*
                 * This BorderLayout subclass maps a null constraint to CENTER.
                 * Although the reference BorderLayout also does this, some VMs
                 * throw an IllegalArgumentException.
                 */
                @Override
                public void addLayoutComponent(Component comp, Object constraints) {
                    if (constraints == null) {
                        constraints = BorderLayout.CENTER;
                    }
                    super.addLayoutComponent(comp, constraints);
                }
            });
        }
        @Override
        public void repaint(long tm, int x, int y, int width, int height) {
            if (DEBUG) {
                Dbg.log(this.getClass().getSimpleName() + ".repaint(" + tm + "," + x + "," + y + "," + width + "," + height + ")");
            }
            if (!CONTENT_PANE_REPAINT_DISABLED) {
                super.repaint(tm, x, y, width, height);
            }
        }
    }
    
    /*
     * Listeners.
     */
    
    private class MyWindowListener implements WindowListener {
        public MyWindowListener() {
        }
        @Override
        public void windowOpened(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowOpened(" + backingEvent + ")");
            }
            onBackingWindowAnyEvent();
        }
        @Override
        public void windowIconified(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowIconified(" + backingEvent + ")");
            }
            // Redundant with more general window state listener,
            // but doesn't hurt, and it might occur earlier.
            onBackingWindowIconified();
        }
        @Override
        public void windowDeiconified(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowDeiconified(" + backingEvent + ")");
            }
            // Redundant with more general window state listener,
            // but doesn't hurt, and it might occur earlier.
            onBackingWindowDeiconified();
        }
        @Override
        public void windowDeactivated(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowDeactivated(" + backingEvent + ")");
            }
            onBackingWindowAnyEvent_withWindowCheck(backingEvent);
        }
        @Override
        public void windowClosing(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowClosing(" + backingEvent + ")");
            }
            // Need to dispose window for windowClosed(...) to be called.
            window.dispose();
        }
        @Override
        public void windowClosed(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowClosed(" + backingEvent + ")");
            }
            onBackingWindowClosing();
        }
        @Override
        public void windowActivated(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowActivated(" + backingEvent + ")");
            }

            // Must paint even what didn't change.
            makeAllDirtyAndEnsurePendingClientPainting();
            
            onBackingWindowAnyEvent();
        }
    }
    
    private class MyWindowStateListener implements WindowStateListener {
        public MyWindowStateListener() {
        }
        @Override
        public void windowStateChanged(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowStateChanged(" + backingEvent + ")");
            }
            onBackingWindowAnyEvent_withWindowCheck(backingEvent);
        }
    }
    
    private class MyFocusListener implements FocusListener {
        public MyFocusListener() {
        }
        @Override
        public void focusGained(FocusEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "focusGained(" + backingEvent + ")");
            }
            onBackingWindowFocusGained();
        }
        @Override
        public void focusLost(FocusEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "focusLost(" + backingEvent + ")");
            }
            onBackingWindowFocusLost();
        }
    }
    
    private class MyComponentListener implements ComponentListener {
        public MyComponentListener() {
        }
        @Override
        public void componentShown(ComponentEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "componentShown(" + backingEvent + ")");
            }
            onBackingWindowShown();
        }
        @Override
        public void componentHidden(ComponentEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "componentHidden(" + backingEvent + ")");
            }
            onBackingWindowHidden();
        }
        @Override
        public void componentMoved(ComponentEvent backingEvent) {
            if (DEBUG_SPAM) {
                hostLog(this, "componentMoved(" + backingEvent + ")");
            }
            onBackingWindowMoved();
        }
        @Override
        public void componentResized(ComponentEvent backingEvent) {
            if (DEBUG_SPAM) {
                hostLog(this, "componentResized(" + backingEvent + ")");
            }
            onBackingWindowResized();
        }
    }
    
    /*
     * 
     */
    
    private class MyKeyListener implements KeyListener {
        public MyKeyListener() {
        }
        @Override
        public void keyPressed(KeyEvent backingEvent) {
            final BwdKeyEventPr event = eventConverter.newKeyPressedEvent(backingEvent);
            onBackingKeyPressed(event);
        }
        @Override
        public void keyTyped(KeyEvent backingEvent) {
            final BwdKeyEventT event = eventConverter.newKeyTypedEventElseNull(backingEvent);
            if (event != null) {
                onBackingKeyTyped(event);
            }
        }
        @Override
        public void keyReleased(KeyEvent backingEvent) {
            final BwdKeyEventPr event = eventConverter.newKeyReleasedEvent(backingEvent);
            onBackingKeyReleased(event);
        }
    }
    
    /*
     * 
     */
    
    private class MyMouseListener implements MouseListener {
        public MyMouseListener() {
        }
        @Override
        public void mousePressed(MouseEvent backingEvent) {
            final BwdMouseEvent event = eventConverter.newMousePressedEvent(backingEvent);
            onBackingMousePressed(event);
        }
        @Override
        public void mouseReleased(MouseEvent backingEvent) {
            final BwdMouseEvent event = eventConverter.newMouseReleasedEvent(backingEvent);
            onBackingMouseReleased(event);
        }
        @Override
        public void mouseClicked(MouseEvent backingEvent) {
            // Bindings don't generate these events.
        }
        @Override
        public void mouseEntered(MouseEvent backingEvent) {
            final BwdMouseEvent event = eventConverter.newMouseEnteredClientEvent(backingEvent);
            onBackingMouseEnteredClient(event);
        }
        @Override
        public void mouseExited(MouseEvent backingEvent) {
            final BwdMouseEvent event = eventConverter.newMouseExitedClientEvent(backingEvent);
            onBackingMouseExitedClient(event);
        }
    }

    private class MyMouseMotionListener implements MouseMotionListener {
        public MyMouseMotionListener() {
        }
        @Override
        public void mouseMoved(MouseEvent backingEvent) {
            final BwdMouseEvent event = eventConverter.newMouseMovedEvent(backingEvent);
            onBackingMouseMoved(event);
        }
        @Override
        public void mouseDragged(MouseEvent backingEvent) {
            // Bindings don't generate these events.
            // Interpreting drags as a moves.
            this.mouseMoved(backingEvent);
        }
    }
    
    private class MyMouseWheelListener implements MouseWheelListener {
        public MyMouseWheelListener() {
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent backingEvent) {
            final BwdWheelEvent event = eventConverter.newWheelEventElseNull(backingEvent);
            if (event != null) {
                onBackingWheelEvent(event);
            }
        }
    }
    
    /*
     * 
     */
    
    private class MyDirtyRectProvider implements InterfaceFactory<GRect> {
        public MyDirtyRectProvider() {
        }
        @Override
        public GRect newInstance() {
            return getAndResetDirtyRectBb();
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final MyDirtyRectProvider dirtyRectProvider = new MyDirtyRectProvider();
    
    private final Window window;
    private final MyJFrame windowAsJFrame;
    private final MyJDialog windowAsJDialog;
    
    private final MyContentPane contentPane;
    
    private final AwtHostBoundsHelper hostBoundsHelper;
    
    private final AwtEventConverter eventConverter;
    
    private final AwtBwdCursorManager cursorManager;
    
    private final AwtGraphicBuffer offscreenBuffer;
    
    private final AwtPaintUtils paintUtils = new AwtPaintUtils();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param owner If not null, creates a dialog host, else a basic host.
     * @param title Must not be null (used in task bar even if not decorated).
     * @param modal Only used if creating a dialog host.
     */
    public SwingBwdHost(
            AbstractAwtBwdBinding binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            SwingBwdHost owner,
            //
            String title,
            boolean decorated,
            boolean modal,
            //
            InterfaceBwdClient client) {
        super(
                binding.getBindingConfig(),
                binding,
                hostLifecycleListener,
                binding.getHostOfFocusedClientHolder(),
                owner,
                title,
                decorated,
                modal,
                client);
        
        final boolean isDialog = (owner != null);

        final Window window;
        if (isDialog) {
            window = new MyJDialog(owner.getBackingWindow());
            this.windowAsJFrame = null;
            this.windowAsJDialog = (MyJDialog) window;
        } else {
            window = new MyJFrame();
            this.windowAsJFrame = (MyJFrame) window;
            this.windowAsJDialog = null;
        }
        this.window = window;
        
        final AbstractBwdHost host = this;
        this.hostBoundsHelper = new AwtHostBoundsHelper(host);
        
        /*
         * 
         */
        
        if (isDialog) {
            this.windowAsJDialog.setUndecorated(!decorated);
            this.windowAsJDialog.setTitle(title);
            if (modal) {
                this.windowAsJDialog.setModalityType(ModalityType.DOCUMENT_MODAL);
            } else {
                this.windowAsJDialog.setModalityType(ModalityType.MODELESS);
            }
        } else {
            this.windowAsJFrame.setUndecorated(!decorated);
            this.windowAsJFrame.setTitle(title);
        }

        /*
         * 
         */
        
        this.cursorManager = new AwtBwdCursorManager(window);

        this.offscreenBuffer = new AwtGraphicBuffer(
                MUST_PRESERVE_OB_CONTENT_ON_RESIZE,
                ALLOW_OB_SHRINKING,
                MUST_USE_INT_ARRAY_RASTER,
                AwtPaintUtils.BUFFERED_IMAGE_TYPE_FOR_CLIENT_G_DRAWING);
        
        // To prevent buffer strategy to be created automatically.
        if (isDialog) {
            this.windowAsJDialog.getRootPane().setDoubleBuffered(false);
        } else {
            this.windowAsJFrame.getRootPane().setDoubleBuffered(false);
        }

        final MyContentPane contentPane = new MyContentPane();
        this.contentPane = contentPane;
        
        if (NOT_OPAQUE_CONTENT_PANE) {
            contentPane.setOpaque(false);
        }

        if (isDialog) {
            this.windowAsJDialog.setContentPane(contentPane);
        } else {
            this.windowAsJFrame.setContentPane(contentPane);
        }
        
        if (NOT_OPAQUE_ROOT_PANE) {
            this.getRootPane().setOpaque(false);
        }

        // Else we don't get key events.
        contentPane.setFocusable(true);

        /*
         * Events.
         */

        window.addComponentListener(new MyComponentListener());
        window.addWindowListener(new MyWindowListener());
        window.addWindowStateListener(new MyWindowStateListener());
        // If using window here, focus events won't go to BWD.
        contentPane.addFocusListener(new MyFocusListener());
        
        final AwtEventConverter eventConverter = new AwtEventConverter(
                binding.getEventsConverterCommonState(),
                this);
        this.eventConverter = eventConverter;
        
        // Need this else we don't receive TAB key events.
        contentPane.setFocusTraversalKeysEnabled(false);
        contentPane.addKeyListener(new MyKeyListener());
        
        contentPane.addMouseListener(new MyMouseListener());
        contentPane.addMouseMotionListener(new MyMouseMotionListener());
        contentPane.addMouseWheelListener(new MyMouseWheelListener());

        /*
         * 
         */
        
        hostLifecycleListener.onHostCreated(this);
    }
    
    /*
     * 
     */
    
    @Override
    public AbstractAwtBwdBinding getBinding() {
        return (AbstractAwtBwdBinding) super.getBinding();
    }
    
    /*
     * 
     */

    @Override
    public AwtBwdCursorManager getCursorManager() {
        return this.cursorManager;
    }

    /*
     * 
     */
    
    @Override
    public Window getBackingWindow() {
        return this.window;
    }
    
    /*
     * 
     */

    @Override
    public SwingBwdHost getOwner() {
        return (SwingBwdHost) super.getOwner();
    }
    
    /*
     * 
     */

    @Override
    public InterfaceBwdHost newDialog(
            String title,
            boolean decorated,
            boolean modal,
            //
            InterfaceBwdClient client) {
        final SwingBwdHost owner = this;
        return new SwingBwdHost(
                this.getBinding(),
                this.getDialogLifecycleListener(),
                owner,
                //
                title,
                decorated,
                modal,
                //
                client);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void paintClientNowOrLater() {
        if (PCNOL_CALL_0_REPAINT_1_PCNOWG == 0) {
            this.window.repaint();
        } else if (PCNOL_CALL_0_REPAINT_1_PCNOWG == 1) {
            this.paintClientNowOnWindowG();
        }
    }

    @Override
    protected void setWindowAlphaFp_backing(double windowAlphaFp) {
        if (this.isDialog()) {
            BindingCallIfExistsUtils.setOpacityElseNoOp(
                    this.windowAsJDialog,
                    (float) windowAlphaFp);
        } else {
            BindingCallIfExistsUtils.setOpacityElseNoOp(
                    this.windowAsJFrame,
                    (float) windowAlphaFp);
        }
    }

    /*
     * 
     */
    
    @Override
    protected boolean isBackingWindowShowing() {
        return AwtUtils.isShowing(this.window);
    }

    @Override
    protected void showBackingWindow() {
        this.window.setVisible(true);
    }

    @Override
    protected void hideBackingWindow() {
        this.window.setVisible(false);
    }

    /*
     * Focus.
     */

    @Override
    protected boolean isBackingWindowFocused() {
        if (this.isClosed_nonVolatile()) {
            /*
             * TODO swing Same issue as for AWT.
             */
            return false;
        }
        if (this.isBackingWindowIconified()) {
            /*
             * TODO swing Most likely same issue as for AWT.
             */
            return false;
        }
        return this.window.isFocused();
    }
    
    @Override
    protected void requestBackingWindowFocusGain() {
        this.window.requestFocus();
    }

    /*
     * Iconified.
     */
    
    @Override
    protected boolean doesSetBackingWindowIconifiedOrNotWork() {
        return !this.isDialog();
    }

    @Override
    protected boolean isBackingWindowIconified() {
        return AwtUtils.isIconified(this.window);
    }
    
    @Override
    protected void setBackingWindowIconifiedOrNot(boolean iconified) {
        if (this.isDialog()) {
            // Can't.
        } else {
            final int es = this.windowAsJFrame.getExtendedState();
            this.windowAsJFrame.setExtendedState(iconified ? (es | JFrame.ICONIFIED) : (es & ~JFrame.ICONIFIED));
        }
    }

    /*
     * Maximized.
     */

    @Override
    protected boolean doesSetBackingWindowMaximizedOrNotWork() {
        if (this.isDialog()) {
            return false;
        }
        return true;
    }
    
    @Override
    protected boolean isBackingWindowMaximized() {
        if (this.isDialog()) {
            // Never maximized.
            return false;
        } else {
            return (this.windowAsJFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0;
        }
    }

    @Override
    protected void setBackingWindowMaximizedOrNot(boolean maximized) {
        if (this.isDialog()) {
            // Can't.
        } else {
            final int es = this.windowAsJFrame.getExtendedState();
            this.windowAsJFrame.setExtendedState(maximized ? (es | JFrame.MAXIMIZED_BOTH) : (es & ~JFrame.MAXIMIZED_BOTH));
        }
    }
    
    /*
     * Bounds.
     */

    @Override
    protected GRect getBackingInsets() {
        return this.hostBoundsHelper.getInsets();
    }
    
    @Override
    protected GRect getBackingClientBounds() {
        return this.hostBoundsHelper.getClientBounds();
    }
    
    @Override
    protected GRect getBackingWindowBounds() {
        return this.hostBoundsHelper.getWindowBounds();
    }

    @Override
    protected void setBackingClientBounds(GRect targetClientBounds) {
        this.hostBoundsHelper.setClientBounds(targetClientBounds);
    }

    @Override
    protected void setBackingWindowBounds(GRect targetWindowBounds) {
        this.hostBoundsHelper.setWindowBounds(targetWindowBounds);
    }

    /*
     * Closing.
     */
    
    @Override
    protected void closeBackingWindow() {
        // Already done if windowClosing(...) was called,
        // but doesn't hurt to call it twice.
        this.window.dispose();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * TODO swing [can happen with AWT, so maybe with Swing too]
     * When iconifying a dialog, activate/deiconify events
     * from owner window seem to arrive to the dialog.
     * This method ignores the event if it's not related to our window.
     */
    private void onBackingWindowAnyEvent_withWindowCheck(WindowEvent backingEvent) {
        if (DEBUG) {
            hostLog(this, "onBackingWindowAnyEvent_withWindowCheck(" + backingEvent + ")");
        }
        if (backingEvent.getWindow() != window) {
            final PrintStream stream = getBindingConfig().getIssueStream();
            stream.println("WARN : event not from our window: " + backingEvent);
            return;
        }
        
        this.onBackingWindowAnyEvent();
    }

    /*
     * 
     */

    private void paintClientNowOnWindowG() {
        final Graphics g;
        if (MUST_PAINT_ON_CONTENT_PANE_GRAPHICS) {
            g = this.contentPane.getGraphics();
        } else {
            g = this.window.getGraphics();
        }
        if (g != null) {
            try {
                this.paintClientNowOnG((Graphics2D) g);
            } finally {
                g.dispose();
            }
        } else {
            /*
             * Can occur if hidden.
             */
        }
    }
    
    private void paintClientNowOnG(Graphics2D g) {
        
        final InterfaceBwdClient client = this.getClientWithExceptionHandler();
        
        client.processEventualBufferedEvents();

        if (!this.canPaintClientNow()) {
            return;
        }

        final Container backingGContainer;
        if (MUST_PAINT_ON_CONTENT_PANE_GRAPHICS) {
            backingGContainer = this.contentPane;
        } else {
            backingGContainer = this.window;
        }
        this.paintUtils.paintClientOnObThenG(
                this.getBinding(),
                this.window,
                this.getPaintClientHelper(),
                this.offscreenBuffer,
                this.dirtyRectProvider,
                backingGContainer,
                g);
    }
    
    /*
     * 
     */
    
    private JRootPane getRootPane() {
        if (this.isDialog()) {
            return this.windowAsJDialog.getRootPane();
        } else {
            return this.windowAsJFrame.getRootPane();
        }
    }
}
