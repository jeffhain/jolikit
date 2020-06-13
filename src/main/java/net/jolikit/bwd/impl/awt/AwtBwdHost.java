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
package net.jolikit.bwd.impl.awt;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Frame;
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

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.InterfaceHostLifecycleListener;
import net.jolikit.bwd.impl.utils.basics.BindingCallIfExistsUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.InterfaceFactory;

public class AwtBwdHost extends AbstractBwdHost {
    
    /*
     * Can help: AWT/Swing paint mess description:
     * http://www.oracle.com/technetwork/java/painting-140037.html
     */
    
    /*
     * How to hunt down rogue fills by the backing library:
     * Setting special colors in host graphics at some points (so that rogue
     * fills use them), and overriding some backing painting methods and
     * introducing sleeps in backing window inner treatments, might allow
     * to see some of the rogue fills if any.
     */
    
    /*
     * TODO awt On some configurations involving Java 6,
     * no move or resize even is generated until the drag is released,
     * especially if resizing from bottom-right corner, in which case
     * already visible area might also not be paint properly.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * True so that offscreen buffer resizing does not leak trough areas that
     * are left unpaint, if any.
     */
    private static final boolean MUST_PRESERVE_OB_CONTENT_ON_RESIZE = true;
    
    private static final boolean ALLOW_OB_SHRINKING = true;
    
    /**
     * Can make things faster in some cases
     * (even though these cases might no longer occur with current code).
     */
    private static final boolean MUST_USE_INT_ARRAY_RASTER = true;

    /**
     * TODO awt Allows to avoid a useless fillRect.
     */
    private static final boolean ANTI_CHEEKY_FILL_RECT = true;

    /**
     * TODO awt Allows to get rid of a sneaky fillRect that clears
     * all our painting.
     * Can be useful at least for dialog hosts, and shouldn't hurt for
     * non-dialog hosts since additional painting(s) occur after blocked one.
     */
    private static final boolean MUST_BLOCK_GRAPHICS_AFTER_WINDOW_ACTIVATED = true;
    
    /**
     * If calling Frame.repaint(long,4int)  from our repaint(long,4int),
     * causes random overpaint by some fillRect, so we prefer just to
     * ensure a later client painting.
     */
    private static final int REPAINT_CALL_0_SUPER_1_EPCPWDR = 1;

    private static final int PAINT_CALL_0_SUPER_1_PAINT_COMPONENTS_2_PCNOG = (ANTI_CHEEKY_FILL_RECT ? 1 : 0);
    
    /**
     * TODO awt On Windows, ensuring pending paint, else,
     * if doing many little dirty paintings following
     * each other (like in a bench), first ones are visible, but then
     * at some point paintComponents(g) gets called (from paint(g)),
     * and the subsequent dirty paintings no longer have effect on screen,
     * unless if followed by a sleep of 1 to 3 milliseconds.
     */
    private static final int PAINT_COMPONENTS_CALL_0_SUPER_1_PCNOG_2_EPCPWDR = (ANTI_CHEEKY_FILL_RECT ? 2 : 0);
    
    private static final int PCNOL_CALL_0_REPAINT_1_PCNOWG = 1;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /*
     * MyFrame and MyDialog contain similar code, but would be over-engineering
     * to factor it due to necessary overrides for use of super.xxx methods, and
     * due to eventual differences.
     */
    
    /**
     * For non-dialog hosts.
     */
    private class MyFrame extends Frame {
        private static final long serialVersionUID = 1L;
        public MyFrame() {
        }
        @Override
        public Graphics getGraphics() {
            if (mustBlockNextGraphics) {
                if (DEBUG) {
                    Dbg.log(this + " : backing graphics blocked");
                }
                mustBlockNextGraphics = false;
                return DUMMY_G_FACTORY.createClippedGraphics();
            } else {
                return super.getGraphics();
            }
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
                     * TODO awt Need to make all dirty here, because sometimes
                     * (happens when left-clicking on the screen after first show)
                     * a sneaky fill rect gets its way to the screen just about now.
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
    
    /**
     * For dialog hosts.
     * TODO xxx dialog Why have grey sneaky fill rect on some kinds of resize?
     */
    private class MyDialog extends Dialog {
        private static final long serialVersionUID = 1L;
        public MyDialog(Window owner) {
            super(owner);
        }
        @Override
        public Graphics getGraphics() {
            if (mustBlockNextGraphics) {
                if (DEBUG) {
                    Dbg.log(this + " : backing graphics blocked");
                }
                mustBlockNextGraphics = false;
                return DUMMY_G_FACTORY.createClippedGraphics();
            } else {
                return super.getGraphics();
            }
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
                     * TODO awt Need to make all dirty here, because sometimes
                     * (happens when left-clicking on the screen after first show)
                     * a sneaky fill rect gets its way to the screen just about now.
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
            
            // Must make all dirty here,
            // but we don't need to ensure client painting.
            makeAllDirty();
            
            if (MUST_BLOCK_GRAPHICS_AFTER_WINDOW_ACTIVATED) {
                mustBlockNextGraphicsAfterPaintClient = true;
            }
            
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
        public void mouseClicked(MouseEvent event) {
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

    /**
     * For creating dummy graphics.
     */
    private static final AwtGraphicBuffer DUMMY_G_FACTORY = new AwtGraphicBuffer();
    
    /*
     * 
     */

    private final Window window;
    private final MyFrame windowAsFrame;
    private final MyDialog windowAsDialog;

    private final AwtHostBoundsHelper hostBoundsHelper;
    
    private final AwtEventConverter eventConverter;
    
    private final AwtBwdCursorManager cursorManager;
    
    private final AwtGraphicBuffer offscreenBuffer;
    
    private final AwtPaintUtils paintUtils = new AwtPaintUtils();
    
    private boolean mustBlockNextGraphicsAfterPaintClient = false;
    private boolean mustBlockNextGraphics = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param owner If not null, creates a dialog host, else a basic host.
     * @param title Must not be null (used in task bar even if not decorated).
     * @param modal Only used if creating a dialog host.
     */
    public AwtBwdHost(
            AbstractAwtBwdBinding binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            AwtBwdHost owner,
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
            window = new MyDialog(owner.getBackingWindow());
            this.windowAsFrame = null;
            this.windowAsDialog = (MyDialog) window;
        } else {
            window = new MyFrame();
            this.windowAsFrame = (MyFrame) window;
            this.windowAsDialog = null;
        }
        this.window = window;
        
        final AbstractBwdHost host = this;
        this.hostBoundsHelper = new AwtHostBoundsHelper(host);
        
        this.cursorManager = new AwtBwdCursorManager(window);

        this.offscreenBuffer = new AwtGraphicBuffer(
                MUST_PRESERVE_OB_CONTENT_ON_RESIZE,
                ALLOW_OB_SHRINKING,
                MUST_USE_INT_ARRAY_RASTER,
                AwtPaintUtils.BUFFERED_IMAGE_TYPE_FOR_CLIENT_G_DRAWING);

        /*
         * 
         */
        
        if (isDialog) {
            this.windowAsDialog.setUndecorated(!decorated);
            this.windowAsDialog.setTitle(title);
            if (modal) {
                this.windowAsDialog.setModalityType(ModalityType.DOCUMENT_MODAL);
            } else {
                this.windowAsDialog.setModalityType(ModalityType.MODELESS);
            }
        } else {
            this.windowAsFrame.setUndecorated(!decorated);
            this.windowAsFrame.setTitle(title);
        }

        /*
         * Events.
         */

        window.addComponentListener(new MyComponentListener());
        window.addWindowListener(new MyWindowListener());
        window.addWindowStateListener(new MyWindowStateListener());
        window.addFocusListener(new MyFocusListener());
        
        final AwtEventConverter eventConverter = new AwtEventConverter(
                binding.getEventsConverterCommonState(),
                this);
        this.eventConverter = eventConverter;
        
        // Need this else we don't receive TAB key events.
        window.setFocusTraversalKeysEnabled(false);
        
        window.addKeyListener(new MyKeyListener());
        
        window.addMouseListener(new MyMouseListener());
        window.addMouseMotionListener(new MyMouseMotionListener());
        window.addMouseWheelListener(new MyMouseWheelListener());
        
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
    public AwtBwdHost getOwner() {
        return (AwtBwdHost) super.getOwner();
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
        final AwtBwdHost owner = this;
        return new AwtBwdHost(
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
                    this.windowAsDialog,
                    (float) windowAlphaFp);
        } else {
            BindingCallIfExistsUtils.setOpacityElseNoOp(
                    this.windowAsFrame,
                    (float) windowAlphaFp);
        }
    }
    
    /*
     * Showing.
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
             * TODO awt On Windows, If client throws on window event,
             * the backing WINDOW_DEACTIVATED event is not fired on close,
             * and the backing window stays focused even though dispose()
             * has been called on it.
             * This might be a side-effect of throws on events for other
             * windows, since when throwing on events, focus gains requests
             * seem to be less successful, maybe due to corrupted focus
             * state in the backing library.
             */
            return false;
        }
        if (this.isBackingWindowIconified()) {
            /*
             * TODO awt On Mac (at least), had an "iconified" window
             * remaining "focused".
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
        if (this.isDialog()) {
            return false;
        }
        return true;
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
            final int es = this.windowAsFrame.getExtendedState();
            this.windowAsFrame.setExtendedState(iconified ? (es | Frame.ICONIFIED) : (es & ~Frame.ICONIFIED));
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
            return (this.windowAsFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
        }
    }

    @Override
    protected void setBackingWindowMaximizedOrNot(boolean maximized) {
        if (this.isDialog()) {
            // Can't.
        } else {
            final int es = this.windowAsFrame.getExtendedState();
            this.windowAsFrame.setExtendedState(maximized ? (es | Frame.MAXIMIZED_BOTH) : (es & ~Frame.MAXIMIZED_BOTH));
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
        /*
         * Already done if windowClosing(...) was called,
         * but doesn't hurt to call it twice.
         */
        this.window.dispose();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * TODO awt When iconifying a dialog, activate/deiconify events
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
        final Graphics g = this.window.getGraphics();
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

        final Container backingGContainer = this.window;
        this.paintUtils.paintClientOnObThenG(
                this.getBinding(),
                this.window,
                this.getPaintClientHelper(),
                this.offscreenBuffer,
                this.dirtyRectProvider,
                backingGContainer,
                g);

        if (mustBlockNextGraphicsAfterPaintClient) {
            mustBlockNextGraphicsAfterPaintClient = false;
            mustBlockNextGraphics = true;
        }
    }
}
