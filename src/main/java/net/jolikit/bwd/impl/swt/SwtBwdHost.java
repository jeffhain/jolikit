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
package net.jolikit.bwd.impl.swt;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.InterfaceHostLifecycleListener;
import net.jolikit.bwd.impl.utils.basics.OsUtils;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;
import net.jolikit.lang.LangUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

public class SwtBwdHost extends AbstractBwdHost {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean MUST_PRESERVE_OB_CONTENT_ON_RESIZE = true;
    
    private static final boolean ALLOW_OB_SHRINKING = true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * Using SWT untyped events API, so that we can work
     * with just one listener, and group events as we want.
     */
    private class MyEl implements Listener {
        @Override
        public void handleEvent(Event event) {
            handleEventImpl(event);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * Types of events we listen to.
     */
    private static final int[] BACKING_EVENT_TYPE_ARR = new int[]{
        
        /*
         * Painting.
         */
        
        // PaintListener
        SWT.Paint, // paintControl(PaintEvent e)

        /*
         * Window geometry.
         */
        
        // ControlListener
        SWT.Move, // controlMoved(ControlEvent e)
        SWT.Resize, // controlResized(ControlEvent e)
        
        /*
         * Window state.
         */
        
        // ShellListener
        SWT.Iconify, // shellIconified(ShellEvent e)
        SWT.Deiconify, // shellDeiconified(ShellEvent e)
        SWT.Activate, // shellActivated(ShellEvent e)
        SWT.Deactivate, // shellDeactivated(ShellEvent e)
        SWT.Close, // shellClosed(ShellEvent e)

        // DisposeListener
        SWT.Dispose, // widgetDisposed(DisposeEvent e)
        
        /*
         * Focus.
         */
        
        // FocusListener
        SWT.FocusIn, // focusGained(FocusEvent e)
        SWT.FocusOut, // focusLost(FocusEvent e)
        
        /*
         * Keyboard.
         */
        
        // KeyListener
        SWT.KeyDown, // keyPressed(KeyEvent e)
        SWT.KeyUp, // keyReleased(KeyEvent e)
        
        /*
         * Mouse.
         */
        
        // MouseListener
        SWT.MouseDown, // mouseDown(MouseEvent e)
        SWT.MouseUp, // mouseUp(MouseEvent e)
        SWT.MouseDoubleClick, // mouseDoubleClick(MouseEvent e)
        
        // MouseMoveListener
        SWT.MouseMove, // mouseMove(MouseEvent e)
        
        // MouseTrackListener
        SWT.MouseEnter, // mouseEnter(MouseEvent e)
        SWT.MouseExit, // mouseExit(MouseEvent e)
        SWT.MouseHover, // mouseHover(MouseEvent e)
        
        // MouseWheelListener
        SWT.MouseVerticalWheel, // mouseScrolled(MouseEvent e)
        SWT.MouseHorizontalWheel, // mouseScrolled(MouseEvent e)
        
        // DragDetectListener
        SWT.DragDetect, // dragDetected(DragDetectEvent e)
        
        // GestureListener
        SWT.Gesture, // gesture(GestureEvent e)
        
        // TouchListener
        SWT.Touch, // touch(TouchEvent e)
        
        /*
         * Misc.
         */
        
        // HelpListener
        SWT.Help, // helpRequested(HelpEvent e)

        // MenuDetectListener
        SWT.MenuDetect, // menuDetected(MenuDetectEvent e)
        
        // TraverseListener
        SWT.Traverse // keyTraversed(TraverseEvent e)
    };
    
    /*
     * 
     */
    
    private final AbstractSwtBwdBinding binding;
    
    private final SwtEventConverter eventConverter;
    
    private final Shell window;
    
    private final SwtHostBoundsHelper hostBoundsHelper;
    
    private final SwtBwdCursorManager cursorManager;
    
    private final IntArrayGraphicBuffer offscreenBuffer;
    
    private final SwtPaintUtils paintUtils = new SwtPaintUtils();
    
    private final MyEl eventListener = new MyEl();
    
    /**
     * TODO swt In case of resize, SWT doesn't just generate a resize event,
     * but also a paint event (even if using SWT.NO_REDRAW_RESIZE flag),
     * which we eventually ignore depending on "mustTryToPaintDuringResize"
     * configuration.
     */
    private boolean hadResizeEventSinceLastPainting = false;

    /*
     * 
     */
    
    /**
     * TODO swt Because SWT doesn't have maximized/demaximized events.
     */
    private boolean lastBackingWindowMaximized = false;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param owner If not null, creates a dialog host, else a basic host.
     * @param title Must not be null (used in task bar even if not decorated).
     * @param modal Only used if creating a dialog host.
     */
    public SwtBwdHost(
            AbstractSwtBwdBinding binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            SwtBwdHost owner,
            //
            String title,
            boolean decorated,
            boolean modal,
            //
            InterfaceBwdClient client,
            //
            SwtCursorRepository backingCursorRepository ) {
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
        
        this.binding = LangUtils.requireNonNull(binding);
        
        this.eventConverter = new SwtEventConverter(
                binding.getEventsConverterCommonState(),
                this);
        
        final boolean isDialog = (owner != null);
        
        int style = SWT.NO_BACKGROUND;
        if (false) {
            /*
             * TODO swt Doesn't seem to either help or hurt.
             */
            style |= SWT.DOUBLE_BUFFERED;
        }
        if (decorated) {
            style |= SWT.BORDER;
            style |= SWT.MIN;
            style |= SWT.MAX;
            style |= SWT.CLOSE;
            style |= SWT.RESIZE;
            style |= SWT.TITLE;
        }
        
        final Display display = binding.getDisplay();
        
        final Shell window;
        if (isDialog) {
            if (modal) {
                // Weakest modal flag, others being
                // APPLICATION_MODAL and SYSTEM_MODAL.
                style |= SWT.PRIMARY_MODAL;
            }
            window = new Shell(owner.window, style);
        } else {
            window = new Shell(display, style);
        }
        this.window = window;
        
        window.setText(title);
        
        final AbstractBwdHost host = this;
        this.hostBoundsHelper = new SwtHostBoundsHelper(
                host,
                binding.getBindingConfig().getUndecoratedInsets());

        /*
         * Initializing with tiny cornered bounds,
         * else could take a huge space until first bounds setting.
         */
        this.setBackingClientBounds(GRect.DEFAULT_EMPTY);
        
        /*
         * 
         */

        this.cursorManager = new SwtBwdCursorManager(
                backingCursorRepository,
                window);

        this.offscreenBuffer = new IntArrayGraphicBuffer(
                MUST_PRESERVE_OB_CONTENT_ON_RESIZE,
                ALLOW_OB_SHRINKING);

        for (int eventType : BACKING_EVENT_TYPE_ARR) {
            window.addListener(eventType, this.eventListener);
        }
        
        /*
         * 
         */
        
        hostLifecycleListener.onHostCreated(this);
    }

    /*
     * 
     */

    @Override
    public AbstractSwtBwdBinding getBinding() {
        return (AbstractSwtBwdBinding) super.getBinding();
    }

    /*
     * 
     */

    @Override
    public SwtBwdCursorManager getCursorManager() {
        return this.cursorManager;
    }

    /*
     * 
     */
    
    @Override
    public Shell getBackingWindow() {
        return this.window;
    }
    
    /*
     * 
     */

    @Override
    public SwtBwdHost getOwner() {
        return (SwtBwdHost) super.getOwner();
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
        final SwtBwdHost owner = this;
        return new SwtBwdHost(
                this.binding,
                this.getDialogLifecycleListener(),
                owner,
                //
                title,
                decorated,
                modal,
                //
                client,
                //
                this.cursorManager.getBackingCursorRepository());
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void paintClientNowOrLater() {
        if (this.window.isDisposed()) {
            // To avoid eventual
            // "org.eclipse.swt.SWTException: Widget is disposed".
            return;
        }
        
        if (false) {
            /*
             * TODO swt If ever wanting to block/postpone drawing.
             */
            this.window.setRedraw(false);
            this.window.setRedraw(true);
        }
        
        this.window.redraw();
    }

    @Override
    protected void setWindowAlphaFp_backing(double windowAlphaFp) {
        if (this.window.isDisposed()) {
            return;
        }
        final int windowAlpha8 = Argb32.toInt8FromFp(windowAlphaFp);
        this.window.setAlpha(windowAlpha8);
    }

    /*
     * Showing.
     */

    @Override
    protected boolean isBackingWindowShowing() {
        if (this.window.isDisposed()) {
            return false;
        }
        return this.window.isVisible();
    }

    @Override
    protected void showBackingWindow() {
        if (this.window.isDisposed()) {
            return;
        }
        this.window.setVisible(true);
    }

    @Override
    protected void hideBackingWindow() {
        if (this.window.isDisposed()) {
            return;
        }
        this.window.setVisible(false);
    }

    /*
     * Focus.
     */

    @Override
    protected boolean isBackingWindowFocused() {
        if (this.isBackingWindowIconified()) {
            /*
             * TODO swt In host unit test, if client throwing on event,
             * fail at step 76 because backing window state is iconified
             * but remains focused.
             * Returning false here as a workaround.
             */
            return false;
        }
        
        if (this.window.isDisposed()) {
            return false;
        }
        return this.window.isFocusControl();
    }
    
    @Override
    protected void requestBackingWindowFocusGain() {
        if (this.window.isDisposed()) {
            return;
        }
        if (false) {
            /*
             * Too brutal (or are we too soft?).
             */
            this.window.forceFocus();
        }
        this.window.setFocus();
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
        if (this.window.isDisposed()) {
            return false;
        }
        return this.window.getMinimized();
    }
    
    @Override
    protected void setBackingWindowIconifiedOrNot(boolean iconified) {
        if (this.window.isDisposed()) {
            return;
        }
        this.window.setMinimized(iconified);
    }

    /*
     * Maximized.
     */

    @Override
    protected boolean doesSetBackingWindowMaximizedOrNotWork() {
        if (OsUtils.isMac()) {
            /*
             * TODO swt On Mac, setting a state on a window can have the side effect
             * of also setting it in a recently created and unrelated (not a dialog)
             * window, as if there was some terrible shared (static ?) mutable state
             * or threading bug in SWT.
             * For example, if creating a window and then maximizing another window,
             * the newly created window will be maximized as well.
             * Yet, we still allow (de)maximizing for decorated non-dialog windows,
             * which should work properly as long as windows are not created in bursts.
             */
            if (!this.isDecorated()) {
                /*
                 * TODO swt On Mac, maximization doesn't work if window
                 * is undecorated: the top grows up by what appears to be
                 * the height of a decoration top, and then the window
                 * is stuck in this state and bounds.
                 */
                return false;
            }
            if (this.isDialog()) {
                /*
                 * TODO swt On Mac, setting bounds on a window can have
                 * side-effect on the maximized state of its dialogs,
                 * by changing it to false without changing their bounds.
                 * Ex.:
                 * H2D4U2b : host state =   [sho, unf, dei, max, ope]
                 * H2D4U2b : window bounds = [-8, 33, 1280, 752]
                 * H2U2a ; hostMethod = SET_WINDOW_BOUNDS
                 * H2D4U2b : host state =   [sho, unf, dei, dem, ope]
                 * H2D4U2b : window bounds = [-19, 51, 1280, 752]
                 * 
                 * TODO swt On Mac, demaximizing dialogs (at least
                 * dialogs on dialogs) might not work.
                 * Ex.:
                 * H2D1D4U3r ; hostMethod = DEMAXIMIZE
                 * ERROR : H2D1D4U3r : (final host state check) expected [sho, foc, dei, dem, ope], got [sho, unf, dei, max, ope]
                 * 
                 * To avoid these issues, we just don't allow maximization
                 * for dialogs.
                 */
                return false;
            }
        }
        return true;
    }
    
    @Override
    protected boolean isBackingWindowMaximized() {
        if (!this.doesSetBackingWindowMaximizedOrNotWork()) {
            /*
             * TODO swt On Mac, for some reason, undecorated windows
             * can be indicated maximized, from creation or showing time,
             * while they are not.
             */
            return false;
        }
        if (this.window.isDisposed()) {
            return false;
        }
        return this.window.getMaximized();
    }

    @Override
    protected void setBackingWindowMaximizedOrNot(boolean maximized) {
        if (this.window.isDisposed()) {
            return;
        }
        this.window.setMaximized(maximized);
    }

    /*
     * Bounds.
     */

    @Override
    protected GRect getBackingInsets() {
        if (this.window.isDisposed()) {
            return GRect.DEFAULT_EMPTY;
        }
        return this.hostBoundsHelper.getInsets();
    }
    
    @Override
    protected GRect getBackingClientBounds() {
        if (this.window.isDisposed()) {
            return GRect.DEFAULT_EMPTY;
        }
        return this.hostBoundsHelper.getClientBounds();
    }

    @Override
    protected GRect getBackingWindowBounds() {
        if (this.window.isDisposed()) {
            return GRect.DEFAULT_EMPTY;
        }
        return this.hostBoundsHelper.getWindowBounds();
    }

    @Override
    protected void setBackingClientBounds(GRect targetClientBounds) {
        if (this.window.isDisposed()) {
            return;
        }
        this.hostBoundsHelper.setClientBounds(targetClientBounds);
    }

    @Override
    protected void setBackingWindowBounds(GRect targetWindowBounds) {
        if (this.window.isDisposed()) {
            return;
        }
        this.hostBoundsHelper.setWindowBounds(targetWindowBounds);
    }
    
    /*
     * Closing.
     */

    @Override
    protected void closeBackingWindow() {
        this.window.dispose();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void handleEvent_Paint(Event backingEvent) {
        final boolean hadResizeEvent = this.hadResizeEventSinceLastPainting;
        if (hadResizeEvent) {
            this.hadResizeEventSinceLastPainting = false;
            if (DEBUG_SPAM) {
                hostLog(this, "hadResizeEventSinceLastPainting set to false");
            }
        }
        
        if (hadResizeEvent) {
            if (this.binding.getBindingConfig().getMustTryToPaintDuringResize()) {
                /*
                 * Making sure we don't paint synchronously at each resize event,
                 * but instead rely on ensurePendingClientPainting(...) to respect
                 * desired client painting delay.
                 * 
                 * Considering it as a full-dirty paint event.
                 */
                this.makeAllDirtyAndEnsurePendingClientPainting();
                return;
            } else {
                /*
                 * Making sure we don't paint during the paint event
                 * that follows each resize event.
                 */
                return;
            }
        }
        
        final GC backingG = backingEvent.gc;

        if (DEBUG_SPAM) {
            hostLog(this, "handleEvent_Paint(" + backingG + ")");
        }
        
        final InterfaceBwdClient client = this.getClientWithExceptionHandler();
        
        client.processEventualBufferedEvents();

        if (!this.canPaintClientNow()) {
            return;
        }

        final GRect contentRect = this.getClientBounds();
        final int width = contentRect.xSpan();
        final int height = contentRect.ySpan();
        if ((width <= 0) || (height <= 0)) {
            return;
        }
        
        final GRect box = GRect.valueOf(0, 0, width, height);

        final GRect dirtyRect = this.getAndResetDirtyRectBb();

        this.offscreenBuffer.setSize(width, height);

        final int[] pixelArr = this.offscreenBuffer.getPixelArr();
        final int pixelArrScanlineStride = this.offscreenBuffer.getScanlineStride();

        final SwtBwdGraphics g = new SwtBwdGraphics(
                this.binding,
                this.binding.getDisplay(),
                box,
                //
                pixelArr,
                pixelArrScanlineStride);

        final List<GRect> paintedRectList =
                this.getClientPainterNoRec().paintClientAndClipRects(
                        g,
                        dirtyRect);

        if (paintedRectList.size() != 0) {
            if (this.canPaintClientNow()) {
                /*
                 * NB: if closed (which can't be due to showing check),
                 * the offscreen buffer has been disposed.
                 */
                final Device device = this.binding.getDisplay();

                for (GRect paintedRect : paintedRectList) {
                    this.paintUtils.drawRectOnG(
                            pixelArr,
                            pixelArrScanlineStride,
                            paintedRect,
                            device,
                            backingG);
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private void handleEventImpl(Event backingEvent) {
        if (DEBUG_SPAM) {
            hostLog(this, "handleEventImpl(" + backingEvent + ")");
        }
        
        if (isClosed_nonVolatile()) {
            // Ignoring events, to avoid troubles, as host is now unusable
            // (offscreen buffer disposed, etc.).
            return;
        }
        
        final Display eventDisplay = backingEvent.display;
        if (eventDisplay != this.binding.getDisplay()) {
            // Not for us (if can ever happen).
            return;
        }
        
        final Widget eventWidget = backingEvent.widget;
        if (eventWidget != this.window) {
            // Not for us (if can ever happen).
            return;
        }
        
        final int type = backingEvent.type;
        switch (type) {
        
        /*
         * Painting.
         */
        
        case SWT.Paint: {
            this.handleEvent_Paint(backingEvent);
            
            if (false) {
                /*
                 * TODO swt Non-null only for paint events.
                 * Not sure if we have to dispose it here.
                 */
                final GC backingG = backingEvent.gc;
                if (backingG.isDisposed()) {
                    /*
                     * Could maybe happen if user did shut down the binding during a painting,
                     * in which case trying to dispose the graphics would cause
                     * "org.eclipse.swt.SWTException: Graphic is disposed".
                     */
                } else {
                    backingG.dispose();
                }
            }
        } break;
        
        /*
         * Window geometry.
         */
        
        case SWT.Move: this.handleEvent_Move(backingEvent); break;
        case SWT.Resize: this.handleEvent_Resize(backingEvent); break;
        
        /*
         * Window state.
         */
        
        case SWT.Iconify: this.handleEvent_Iconify(backingEvent); break;
        case SWT.Deiconify: this.handleEvent_Deiconify(backingEvent); break;
        case SWT.Activate: this.handleEvent_Activate(backingEvent); break;
        case SWT.Deactivate: this.handleEvent_Deactivate(backingEvent); break;
        case SWT.Close: this.handleEvent_Close(backingEvent); break;
        
        case SWT.Dispose: this.handleEvent_Dispose(backingEvent); break;
        
        /*
         * Focus.
         */
        
        case SWT.FocusIn: this.handleEvent_FocusIn(backingEvent); break;
        case SWT.FocusOut: this.handleEvent_FocusOut(backingEvent); break;
        
        /*
         * Keyboard.
         */
        
        case SWT.KeyDown: this.handleEvent_KeyDown(backingEvent); break;
        case SWT.KeyUp: this.handleEvent_KeyUp(backingEvent); break;
        
        /*
         * Mouse.
         */
        
        case SWT.MouseDown: this.handleEvent_MouseDown(backingEvent); break;
        case SWT.MouseUp: this.handleEvent_MouseUp(backingEvent); break;
        case SWT.MouseDoubleClick: this.handleEvent_MouseDoubleClick(backingEvent); break;
        
        case SWT.MouseMove: this.handleEvent_MouseMove(backingEvent); break;
        
        case SWT.MouseEnter: this.handleEvent_MouseEnter(backingEvent); break;
        case SWT.MouseExit: this.handleEvent_MouseExit(backingEvent); break;
        case SWT.MouseHover: this.handleEvent_MouseHover(backingEvent); break;
        
        case SWT.MouseVerticalWheel: this.handleEvent_MouseVerticalWheel(backingEvent); break;
        case SWT.MouseHorizontalWheel: this.handleEvent_MouseHorizontalWheel(backingEvent); break;

        case SWT.DragDetect: this.handleEvent_DragDetect(backingEvent); break;
        
        case SWT.Gesture: this.handleEvent_Gesture(backingEvent); break;
        
        case SWT.Touch: this.handleEvent_Touch(backingEvent); break;
        
        /*
         * Misc.
         */
        
        case SWT.Help: this.handleEvent_Help(backingEvent); break;
        
        case SWT.MenuDetect: this.handleEvent_MenuDetect(backingEvent); break;
        
        case SWT.Traverse: this.handleEvent_Traverse(backingEvent); break;
        
        default:
            throw new AssertionError("" + type);
        }
    }
    
    /*
     * Window geometry.
     */
    
    private void handleEvent_Move(Event backingEvent) {
        if (DEBUG_SPAM) {
            hostLog(this, "handleEvent_Move(" + backingEvent + ")");
        }
        
        this.onBackingWindowMoved();
    }
    
    private void handleEvent_Resize(Event backingEvent) {
        if (DEBUG_SPAM) {
            hostLog(this, "handleEvent_Resize(" + backingEvent + ")");
        }

        if (!this.hadResizeEventSinceLastPainting) {
            this.hadResizeEventSinceLastPainting = true;
            if (DEBUG_SPAM) {
                hostLog(this, "hadResizeEventSinceLastPainting set to true");
            }
        }

        try {
            this.onBackingWindowResized();
        } finally {
            this.onPossibleBackingWindowMaximizedStateEvent();
        }
    }
    
    /*
     * Window state.
     */
    
    private void handleEvent_Iconify(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_Iconify(" + backingEvent + ")");
        }
        this.onBackingWindowIconified();
    }
    
    private void handleEvent_Deiconify(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_Deiconify(" + backingEvent + ")");
        }
        /*
         * TODO swt Shell.getMinimized() keeps returning true
         * for some time after we pass here, but it's taken care of
         * by super class timeouts.
         */
        this.onBackingWindowDeiconified();
    }
    
    private void onPossibleBackingWindowMaximizedStateEvent() {
        final boolean oldMax = this.lastBackingWindowMaximized;
        final boolean newMax;
        
        final boolean canRelyOnIsBackingwindowMaximized =
                this.isBackingWindowShowing()
                && (!this.isBackingWindowIconified());
        if (canRelyOnIsBackingwindowMaximized) {
            newMax = this.isBackingWindowMaximized();
        } else {
            newMax = oldMax;
        }
        
        if (newMax != oldMax) {
            this.lastBackingWindowMaximized = newMax;
            if (newMax) {
                this.onBackingWindowMaximized();
            } else {
                this.onBackingWindowDemaximized();
            }
        }
    }
    
    private void handleEvent_Activate(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_Activate(" + backingEvent + ")");
        }
        /*
         * TODO swt Could also not call any callback here.
         */
        this.onBackingWindowAnyEvent();
    }
    
    private void handleEvent_Deactivate(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_Deactivate(" + backingEvent + ")");
        }
        /*
         * TODO swt Occurs in case of hiding, but also possibly
         * of focus lost.
         * Ex.: H0 had focus:
         * 247_247211617  1 Thread[main,5,main] ; SwtBwdHost ; H0D3 : requestFocusGain()
         * 247_247213774  1 Thread[main,5,main] ; SwtBwdHost ; H0D3 : requestBackingWindowFocusGain()
         * 247_247638699  1 Thread[main,5,main] ; SwtBwdHost ; H0 : handleEvent_Deactivate(Event {type=27 Shell {H0} time=5828214 data=null x=0 y=0 width=0 height=0 detail=0})
         */
        final boolean canAssumeItsHiddenEvent = (!isBackingWindowShowing());
        if (canAssumeItsHiddenEvent) {
            this.onBackingWindowHidden();
        } else {
            this.onBackingWindowAnyEvent();
        }
    }
    
    private void handleEvent_Close(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_Close(" + backingEvent + ")");
        }
        this.onBackingWindowClosing();
    }
    
    private void handleEvent_Dispose(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_Dispose(" + backingEvent + ")");
        }
        /*
         * Can't seem to get it to occur.
         */
    }
    
    /*
     * Focus.
     */
    
    private void handleEvent_FocusIn(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_FocusIn(" + backingEvent + ")");
        }
        this.onBackingWindowFocusGained();
    }
    
    private void handleEvent_FocusOut(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_FocusOut(" + backingEvent + ")");
        }
        this.onBackingWindowFocusLost();
    }
    
    /*
     * Keyboard.
     */
    
    private void handleEvent_KeyDown(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_KeyDown(" + backingEvent + ")");
        }
        
        {
            final BwdKeyEventPr event = this.eventConverter.newKeyPressedEvent(backingEvent);
            this.onBackingKeyPressed(event);
        }
        
        {
            final BwdKeyEventT event = this.eventConverter.newKeyTypedEventElseNull(backingEvent);
            if (event != null) {
                this.onBackingKeyTyped(event);
            }
        }
    }
    
    private void handleEvent_KeyUp(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_KeyUp(" + backingEvent + ")");
        }
        
        final BwdKeyEventPr event = this.eventConverter.newKeyReleasedEvent(backingEvent);
        this.onBackingKeyReleased(event);
    }
    
    /*
     * Mouse.
     */
    
    private void handleEvent_MouseDown(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_MouseDown(" + backingEvent + ")");
        }
        
        final BwdMouseEvent event = this.eventConverter.newMousePressedEvent(backingEvent);
        this.onBackingMousePressed(event);
    }
    
    private void handleEvent_MouseUp(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_MouseUp(" + backingEvent + ")");
        }
        final BwdMouseEvent event = this.eventConverter.newMouseReleasedEvent(backingEvent);
        this.onBackingMouseReleased(event);
    }
    
    private void handleEvent_MouseDoubleClick(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_MouseDoubleClick(" + backingEvent + ")");
        }
        /*
         * Bindings must not generate mouse click events,
         * least double click events!
         */
    }
    
    private void handleEvent_MouseMove(Event backingEvent) {
        if (DEBUG_SPAM) {
            hostLog(this, "handleEvent_MouseMove(" + backingEvent + ")");
        }
        final BwdMouseEvent event = this.eventConverter.newMouseMovedEvent(backingEvent);
        this.onBackingMouseMoved(event);
    }
    
    private void handleEvent_MouseEnter(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_MouseEnter(" + backingEvent + ")");
        }
        final BwdMouseEvent event = this.eventConverter.newMouseEnteredClientEvent(backingEvent);
        this.onBackingMouseEnteredClient(event);
    }
    
    private void handleEvent_MouseExit(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_MouseExit(" + backingEvent + ")");
        }
        final BwdMouseEvent event = this.eventConverter.newMouseExitedClientEvent(backingEvent);
        this.onBackingMouseExitedClient(event);
    }
    
    private void handleEvent_MouseHover(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_MouseHover(" + backingEvent + ")");
        }
        /*
         * Occurs 400ms after mouse stopped moving over client area.
         */
    }
    
    private void handleEvent_MouseVerticalWheel(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_MouseVerticalWheel(" + backingEvent + ")");
        }
        final BwdWheelEvent event = this.eventConverter.newWheelEventElseNull(backingEvent);
        if (event != null) {
            this.onBackingWheelEvent(event);
        }
    }
    
    private void handleEvent_MouseHorizontalWheel(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_MouseHorizontalWheel(" + backingEvent + ")");
        }
        final BwdWheelEvent event = this.eventConverter.newWheelEventElseNull(backingEvent);
        if (event != null) {
            this.onBackingWheelEvent(event);
        }
    }

    private void handleEvent_DragDetect(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_DragDetect(" + backingEvent + ")");
        }
    }
    
    private void handleEvent_Gesture(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_Gesture(" + backingEvent + ")");
        }
    }
    
    private void handleEvent_Touch(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_Touch(" + backingEvent + ")");
        }
    }
    
    /*
     * Misc.
     */
    
    private void handleEvent_Help(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_Help(" + backingEvent + ")");
        }
    }
    
    private void handleEvent_MenuDetect(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_MenuDetect(" + backingEvent + ")");
        }
    }
    
    private void handleEvent_Traverse(Event backingEvent) {
        if (DEBUG) {
            hostLog(this, "handleEvent_Traverse(" + backingEvent + ")");
        }
    }
}
