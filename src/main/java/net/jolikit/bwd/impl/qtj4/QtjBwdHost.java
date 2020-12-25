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
package net.jolikit.bwd.impl.qtj4;

import java.util.ArrayList;
import java.util.List;

import com.trolltech.qt.core.QEvent;
import com.trolltech.qt.core.QEvent.Type;
import com.trolltech.qt.core.QRect;
import com.trolltech.qt.core.Qt.FocusPolicy;
import com.trolltech.qt.core.Qt.FocusReason;
import com.trolltech.qt.core.Qt.WindowModality;
import com.trolltech.qt.core.Qt.WindowType;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QCloseEvent;
import com.trolltech.qt.gui.QDragMoveEvent;
import com.trolltech.qt.gui.QFrame;
import com.trolltech.qt.gui.QImage;
import com.trolltech.qt.gui.QKeyEvent;
import com.trolltech.qt.gui.QMouseEvent;
import com.trolltech.qt.gui.QMoveEvent;
import com.trolltech.qt.gui.QPaintEvent;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QResizeEvent;
import com.trolltech.qt.gui.QSizePolicy.Policy;
import com.trolltech.qt.gui.QStackedLayout;
import com.trolltech.qt.gui.QWheelEvent;
import com.trolltech.qt.gui.QWidget;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.InterfaceHostLifecycleListener;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.lang.ObjectWrapper;

public class QtjBwdHost extends AbstractBwdHost {
    
    /*
     * QDialog seems to work for dialogs, but it seems that just using
     * a properly configured QFrame works as well, so we go for it
     * for simplicity.
     * Also, QDialog.setModal(...) causes application modality, so we want
     * to use QWidget.setWindowModality(WindowModality.WindowModal) instead.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * TODO qtj Modal windows crash on show, at least on Mac,
     * so we use poor man's modal instead.
     * 
     * "Note that on some window managers on X11 you also have to pass
     * Qt::X11BypassWindowManagerHint for this flag to work correctly."
     */
    private static final boolean MUST_USE_POOR_MAN_S_MODAL = true;
    
    /*
     * 
     */
    
    /**
     * True so that offscreen buffer resizing does not leak trough areas that
     * are left unpaint, if any.
     */
    private static final boolean MUST_PRESERVE_OB_CONTENT_ON_RESIZE = true;
    
    private static final boolean ALLOW_OB_SHRINKING = true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private class MyFrame extends QFrame {
        private boolean firstPaintEventSeen = false;
        /**
         * @param parent null for a non-dialog.
         */
        public MyFrame(QWidget parent, WindowType... windowTypes) {
            super(parent, windowTypes);
        }
        
        /*
         * 
         */

        @Override
        protected void paintEvent(QPaintEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "paintEvent(" + backingEvent + ")");
            }
            /*
             * We only use this [frame] paint event for that,
             * actual painting is done in QWidget's paint event.
             */
            if (!this.firstPaintEventSeen) {
                /*
                 * TODO qtj Need that for window to go to front
                 * once it's first made visible.
                 * On Mac, doing it synchronously  causes
                 * "QWidget::repaint: Recursive repaint detected",
                 * so we do it asynchronously.
                 */
                getBinding().getUiThreadScheduler().execute(new Runnable() {
                    @Override
                    public void run() {
                        window.activateWindow();
                    }
                });
                this.firstPaintEventSeen = true;
            }
            
            super.paintEvent(backingEvent);
        }

        /*
         * 
         */
        
        @Override
        protected void closeEvent(QCloseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "closeEvent(" + backingEvent + ")");
            }
            onBackingWindowClosing();
        }

        /*
         * 
         */

        @Override
        protected void keyPressEvent(QKeyEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "keyPressEvent(" + backingEvent + ") : repeat = " + backingEvent.isAutoRepeat());
            }

            if (backingEvent.isAutoRepeat()) {
                /*
                 * Optional return, since KeyRepetitionHelper takes care
                 * of blocking backing repetitions and synthesizing
                 * new ones at configured frequency, but should not hurt.
                 */
                if (DEBUG) {
                    hostLog(this, "ignoring backing repetition");
                }
                return;
            }
            
            {
                final BwdKeyEventPr event = eventConverter.newKeyPressedEvent(backingEvent);
                onBackingKeyPressed(event);
            }
            {
                final BwdKeyEventT event = eventConverter.newKeyTypedEventElseNull(backingEvent);
                if (event != null) {
                    onBackingKeyTyped(event);
                }
            }
        }

        @Override
        protected void keyReleaseEvent(QKeyEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "keyReleaseEvent(" + backingEvent + ") : repeat = " + backingEvent.isAutoRepeat());
            }
            
            if (!backingEvent.isAutoRepeat()) {
                final BwdKeyEventPr event = eventConverter.newKeyReleasedEvent(backingEvent);
                onBackingKeyReleased(event);
            }
        }
        
        /*
         * 
         */
        
        @Override
        protected void mousePressEvent(QMouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "mousePressEvent(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMousePressedEvent(backingEvent);
            onBackingMousePressed(event);
        }

        @Override
        protected void mouseReleaseEvent(QMouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "mouseReleaseEvent(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseReleasedEvent(backingEvent);
            onBackingMouseReleased(event);
        }

        @Override
        protected void enterEvent(QEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "enterEvent(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseEnteredClientEvent(backingEvent);
            onBackingMouseEnteredClient(event);
        }

        @Override
        protected void leaveEvent(QEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "leaveEvent(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseExitedClientEvent(backingEvent);
            onBackingMouseExitedClient(event);
        }

        @Override
        protected void mouseMoveEvent(QMouseEvent backingEvent) {
            if (DEBUG_SPAM) {
                hostLog(this, "mouseMoveEvent(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseMovedEvent(backingEvent);
            onBackingMouseMoved(event);
        }

        @Override
        protected void dragMoveEvent(QDragMoveEvent backingEvent) {
            if (DEBUG_SPAM) {
                hostLog(this, "dragMoveEvent(" + backingEvent + ")");
            }
            /*
             * TODO qtj Can't seem to get this event fired
             * (drag-and-drop related?),
             * but we just need MOUSE_MOVED backing event anyway.
             */
        }

        /*
         * 
         */

        @Override
        protected void wheelEvent(QWheelEvent backingEvent) {
            if (DEBUG_SPAM) {
                hostLog(this, "wheelEvent(" + backingEvent + ")");
            }
            final BwdWheelEvent event = eventConverter.newWheelEventElseNull(backingEvent);
            if (event != null) {
                onBackingWheelEvent(event);
            }
        }

        /*
         * 
         */

        @Override
        public boolean event(QEvent backingEvent) {
            if (isSpamEvent(backingEvent)) {
                if (DEBUG_SPAM) {
                    hostLog(this, "event(" + backingEvent + ")");
                }
            } else {
                if (DEBUG) {
                    hostLog(this, "event(" + backingEvent + ")");
                }
            }
            
            final Type type = backingEvent.type();
            
            /*
             * TODO qtj QWidget.focusInEvent(QFocusEvent)
             * and QWidget.focusOutEvent(QFocusEvent)
             * don't seem to be called,
             * but QEvent(type=WindowActivate)
             * and QEvent(type=WindowDeactivate)
             * seem to be called when focus is gained/lost,
             * so we rely on them instead.
             */

            if (type == Type.WindowDeactivate) {
                onBackingWindowFocusLost();
            } else if (type == Type.WindowActivate) {
                onBackingWindowFocusGained();
            }

            if (type == Type.Show) {
                onBackingWindowShown();
            } else if (type == Type.Hide) {
                onBackingWindowHidden();
            }
            
            if (backingEvent instanceof QMoveEvent) {
                onBackingWindowMoved();
            }
            
            if (backingEvent instanceof QResizeEvent) {
                hadResizeEventSinceLastPainting = true;
                onBackingWindowResized();
            }

            return super.event(backingEvent);
        }
    }
    
    /*
     * 
     */

    /**
     * TODO qtj It seems that painting directly
     * into the window has no effect,
     * so we paint in a widget instead.
     */
    private class MyPaintedWidget extends QWidget {
        public MyPaintedWidget() {
        }

        @Override
        protected void paintEvent(QPaintEvent backingEvent) {
            if (isSpamEvent(backingEvent)) {
                if (DEBUG_SPAM) {
                    hostLog(this, "paintEvent(" + backingEvent + ")");
                }
            } else {
                if (DEBUG) {
                    hostLog(this, "paintEvent(" + backingEvent + ")");
                }
            }
            if (isClosed_nonVolatile()) {
                /*
                 * Can happen on shut down.
                 * Need not to go further, because if shut down
                 * was done during painting, will cause recursive painting,
                 * and much trouble ensue...
                 */
                return;
            }
            
            /*
             * Painting now, even if this paint event is not due
             * to a call to paintClientNowOrLater(), to avoid
             * the client area being black on resize and possibly
             * other glitches.
             */
            paintClientNow_inPaintEvent(backingEvent.rect());
        }
        
        @Override
        public boolean event(QEvent backingEvent) {
            if (isSpamEvent(backingEvent)) {
                if (DEBUG_SPAM) {
                    hostLog(this, "event(" + backingEvent + ")");
                }
            } else {
                if (DEBUG) {
                    hostLog(this, "event(" + backingEvent + ")");
                }
            }
            
            return super.event(backingEvent);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final AbstractQtjBwdBinding binding;
    
    private final QtjEventConverter eventConverter;

    private final QFrame window;
    
    private final QtjHostBoundsHelper hostBoundsHelper;
    
    /**
     * TODO qtj Inside QtJambiGuiInternal, there is a map of QPainter by QWidget,
     * which is static and not used in a thread-safe way, so we can't do parallel
     * painting with QtJambi (while it's possible with Qt, using different painters
     * on different devices). Should make another QtJambiGuiInternal?
     */
    private final MyPaintedWidget paintedWidget;
    
    /**
     * To detect recursive painting, which is not supposed to happen,
     * and can cause Qt errors due to calling begin(QWidget) twice in a row
     * for the same QWidget (or QPainter, but we use a new one each time).
     */
    private boolean clientPaintingBeingDone = false;

    private final QtjBwdCursorManager cursorManager;
    
    /**
     * To ignore the systematic paint event that follow resize events,
     * so that we can implement our optimization of not painting
     * during resizes, or of doing it using "resize async processor",
     * depending on "mustTryToPaintDuringResize" configuration.
     */
    private boolean hadResizeEventSinceLastPainting = false;

    /**
     * To preserve pixels across dirty paintings.
     * Also useful for pixel reading.
     * Also makes our benches faster.
     */
    private QImage offscreenImage = new QImage(
            BindingBasicsUtils.MIN_STORAGE_SPAN,
            BindingBasicsUtils.MIN_STORAGE_SPAN,
            QtjPaintUtils.QIMAGE_FORMAT);
    
    /**
     * To contain the reference to a pool of Qt objects,
     * shared among all graphics for a same host.
     * 
     * The pool itself is created and managed by the graphics class.
     * 
     * This pool is tied to each host, and not to the binding,
     * so that it can be reclaimed by GC after closing a host
     * which painting created a crazy amount of graphics.
     */
    private final ObjectWrapper<Object> hostQtStuffsPoolRef =
            new ObjectWrapper<Object>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param owner If not null, creates a dialog host, else a basic host.
     * @param title Must not be null (used in task bar even if not decorated).
     * @param modal Only used if creating a dialog host.
     */
    public QtjBwdHost(
            AbstractQtjBwdBinding binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            QtjBwdHost owner,
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
        
        this.binding = LangUtils.requireNonNull(binding);
        
        final QtjBwdBindingConfig bindingConfig = binding.getBindingConfig();
        
        final AbstractBwdHost host = this;
        
        this.eventConverter = new QtjEventConverter(
                binding.getEventsConverterCommonState(),
                host,
                bindingConfig.getMustSynthesizeAltGraph(),
                bindingConfig.getAltGraphNativeScanCode());
        
        final List<WindowType> windowTypeList = new ArrayList<WindowType>();
        
        final boolean isDialog = (owner != null);
        if (isDialog) {
            /*
             * NB: If we don't do that, the new window is not a window,
             * but is included in the owner's window.
             */
            windowTypeList.add(WindowType.Dialog);
        }

        if (decorated) {
            /*
             * TODO qtj It seems these are by default,
             * but doesn't hurt to make them explicit.
             */
            
            windowTypeList.add(WindowType.WindowTitleHint);

            // "On some platforms this (min/max/close buttons) implies
            // Qt::WindowSystemMenuHint for it to work."
            windowTypeList.add(WindowType.WindowSystemMenuHint);
            
            windowTypeList.add(WindowType.WindowMinimizeButtonHint);
            windowTypeList.add(WindowType.WindowMaximizeButtonHint);
            windowTypeList.add(WindowType.WindowCloseButtonHint);
        } else {
            // "Turns off the default window title hints."
            // To get rid of the top bar.
            windowTypeList.add(WindowType.CustomizeWindowHint);
            // To get rid of the border.
            windowTypeList.add(WindowType.FramelessWindowHint);
        }

        if (MUST_USE_POOR_MAN_S_MODAL && modal) {
            windowTypeList.add(WindowType.WindowStaysOnTopHint);
            windowTypeList.add(WindowType.X11BypassWindowManagerHint);
        }

        final WindowType[] windowTypeArr = windowTypeList.toArray(
                new WindowType[windowTypeList.size()]);
        if (isDialog) {
            this.window = new MyFrame(
                    owner.getBackingWindow(),
                    windowTypeArr);

            if ((!MUST_USE_POOR_MAN_S_MODAL) && modal) {
                this.window.setWindowModality(WindowModality.WindowModal);
            } else {
                this.window.setWindowModality(WindowModality.NonModal);
            }
        } else {
            this.window = new MyFrame(
                    null,
                    windowTypeArr);
        }
        
        this.hostBoundsHelper = new QtjHostBoundsHelper(host);
        
        /*
         * 
         */
        
        this.window.setWindowTitle(title);
        
        /*
         * Widget.
         */

        {
            final QStackedLayout layout = new QStackedLayout(this.window);

            final MyPaintedWidget paintedWidget = new MyPaintedWidget();
            this.paintedWidget = paintedWidget;

            layout.addWidget(paintedWidget);
        }
        
        /*
         * 
         */

        this.cursorManager = new QtjBwdCursorManager(this.window);

        /*
         * NB: Doesn't really matter since we draw
         * on an offscreen image that preserves pixels,
         * but not clearing uselessly should help with
         * performances.
         */
        this.window.setAutoFillBackground(false);
        this.paintedWidget.setAutoFillBackground(false);

        // Need to do that on both window and widget.
        this.window.setMouseTracking(true);
        this.paintedWidget.setMouseTracking(true);
        
        /*
         * NoFocus = never
         * ClickFocus = on click
         * TabFocus = on tab
         * StrongFocus = on click or tab
         * WheelFocus = on click or tab or wheel
         * 
         * Libraries generally don't focus on wheel,
         * so we just use StrongFocus.
         * Ensuring NoFocus on widget, to make sure
         * it doesn't steal/damage window focus.
         */
        this.window.setFocusPolicy(FocusPolicy.StrongFocus);
        this.paintedWidget.setFocusPolicy(FocusPolicy.NoFocus);

        /*
         * TODO qtj Seems useless, but shouldn't hurt,
         * since we want bounds to be mutable.
         */
        this.window.setSizePolicy(Policy.Expanding, Policy.Expanding);
        this.paintedWidget.setSizePolicy(Policy.Expanding, Policy.Expanding);
        
        /*
         * Qt uses grey background by default, but most other
         * libraries use black by default, or as fall back when clearing
         * with "transparent" color when window alpha is not 0,
         * so we align on that.
         */
        this.window.setStyleSheet("background-color:black;");

        hostLifecycleListener.onHostCreated(this);
    }

    /*
     * 
     */

    @Override
    public AbstractQtjBwdBinding getBinding() {
        return (AbstractQtjBwdBinding) super.getBinding();
    }

    /*
     * 
     */

    @Override
    public QtjBwdCursorManager getCursorManager() {
        return this.cursorManager;
    }

    /*
     * 
     */

    @Override
    public QWidget getBackingWindow() {
        return this.window;
    }

    /*
     * 
     */

    @Override
    public QtjBwdHost getOwner() {
        return (QtjBwdHost) super.getOwner();
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
        final QtjBwdHost owner = this;
        return new QtjBwdHost(
                this.binding,
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
        /*
         * We can't just call our painting treatments synchronously here,
         * because painting must be done from within paintEvent(QPaintEvent)
         * method, appropriately called by Qt, else various kinds of
         * nasty errors occur, like red logs and exceptions.
         * 
         * We could either call update() or repaint(),
         * and prefer the later because it's synchronous.
         */
        
        /*
         * Bounds here are the clip to use.
         * Not using client box, to avoid too small clip in case
         * of growth before actual painting.
         */
        this.paintedWidget.repaint(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    protected void setWindowAlphaFp_backing(double windowAlphaFp) {
        this.window.setWindowOpacity(windowAlphaFp);
    }
    
    /*
     * 
     */

    @Override
    protected boolean isBackingWindowShowing() {
        return this.window.isVisible();
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
        if (false) {
            /*
             * "The active window is the window that contains the widget
             * that has keyboard focus (The window may still have focus
             * if it has no widgets or none of its widgets accepts
             * keyboard focus).
             * When popup windows are visible, this property is true
             * for both the active window and for the popup."
             */
            this.window.isActiveWindow();
        }
        if (false) {
            // Also works.
            return (QApplication.focusWidget() == this.window);
        }
        return this.window.hasFocus();
    }
    
    @Override
    protected void requestBackingWindowFocusGain() {
        // Need to do that first (raise() is not enough),
        // else setFocus() has no effect.
        this.window.activateWindow();
        
        this.window.setFocus(FocusReason.NoFocusReason);
    }

    /*
     * Iconified.
     */

    @Override
    protected boolean doesSetBackingWindowIconifiedOrNotWork() {
        return true;
    }

    @Override
    protected boolean isBackingWindowIconified() {
        return this.window.isMinimized();
    }

    @Override
    protected void setBackingWindowIconifiedOrNot(boolean iconified) {
        if (iconified) {
            this.window.showMinimized();
        } else {
            if (this.window.isMaximized()) {
                this.window.showMaximized();
            } else {
                this.window.showNormal();
            }
        }
    }

    /*
     * Maximized.
     */

    @Override
    protected boolean doesSetBackingWindowMaximizedOrNotWork() {
        return true;
    }
    
    @Override
    protected boolean isBackingWindowMaximized() {
        return this.window.isMaximized();
    }

    @Override
    protected void setBackingWindowMaximizedOrNot(boolean maximized) {
        if (maximized) {
            this.window.showMaximized();
        } else {
            this.window.showNormal();
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
        this.window.close();
        
        this.offscreenImage.dispose();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Paints synchronously.
     * Must be called from within the paintEvent(QPaintEvent) method.
     */
    private void paintClientNow_inPaintEvent(QRect backingClip) {
        if (DEBUG_SPAM) {
            hostLog(this, "paintClient_inPaintEvent(" + backingClip + ")");
        }

        if (this.clientPaintingBeingDone) {
            /*
             * Must not happen, else causes various exceptions and such.
             */
            throw new AssertionError("reentrant call, must not happen");
        }

        final boolean hadResizeEvent = this.hadResizeEventSinceLastPainting;
        this.hadResizeEventSinceLastPainting = false;
        if (hadResizeEvent) {
            if (!this.binding.getBindingConfig().getMustTryToPaintDuringResize()) {
                /*
                 * Making sure we don't paint during the paint event
                 * that follows each resize event.
                 */
                return;
            }
        }
        
        if (!this.canPaintClientNow()) {
            /*
             * Early check before the other check after processing
             * buffered events, in case bad state (closed etc.)
             * could cause trouble with QPainter's creation/init.
             */
            return;
        }
        
        /*
         * TODO qtj Can't reuse QPainter, so creating a new one each time, else we get that:
         * com.trolltech.qt.QNoNativeResourcesException: Function call on incomplete object of type: com.trolltech.qt.gui.QPainter
         *     at com.trolltech.qt.gui.QPainter.isActive(QPainter.java:1518)
         *     at com.trolltech.qt.QtJambiGuiInternal.beginPaint(QtJambiGuiInternal.java:50)
         *     at com.trolltech.qt.gui.QPainter.begin(QPainter.java:2129)
         *     (...)
         */

        this.clientPaintingBeingDone = true;
        try {
            final QPainter painter = new QPainter();
            painter.begin(this.paintedWidget);
            try {
                painter.setClipRect(backingClip);
                
                if (false) {
                    /*
                     * TODO qtj Doesn't seem to work.
                     */
                    painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                }
                this.paintClientNowOnPainter_inBeginEnd(painter);
            } finally {
                if (isClosed_nonVolatile()) {
                    /*
                     * Can happen if user did shut down during painting,
                     * in which case calling "painter.end()" would cause
                     * "com.trolltech.qt.QNoNativeResourcesException: Function call on incomplete object of type: com.trolltech.qt.gui.QPainter".
                     */
                } else {
                    painter.end();
                }
            }
        } finally {
            this.clientPaintingBeingDone = false;
        }
    }

    private void paintClientNowOnPainter_inBeginEnd(QPainter painter) {

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

        final boolean isImageGraphics = false;
        final GRect box = GRect.valueOf(0, 0, width, height);

        final GRect dirtyRect = this.getAndResetDirtyRectBb();
        
        this.updateOffscreenImageSize(width, height);

        final QtjBwdGraphics g = new QtjBwdGraphics(
                this.binding,
                isImageGraphics,
                box,
                //
                this.offscreenImage,
                //
                this.hostQtStuffsPoolRef);

        // No use for this list.
        @SuppressWarnings("unused")
        final List<GRect> paintedRectList =
        this.getPaintClientHelper().initPaintFinish(
                g,
                dirtyRect);

        if (this.canPaintClientNow()) {
            painter.drawImage(0, 0, this.offscreenImage);
        }
    }

    private void updateOffscreenImageSize(int minWidthCap, int minHeightCap) {
        NbrsUtils.requireSupOrEq(0, minWidthCap, "minWidthCap");
        NbrsUtils.requireSupOrEq(0, minHeightCap, "minHeightCap");
        
        final QImage oldImage = this.offscreenImage;
        final int oldWidthCap = oldImage.width();
        final int oldHeightCap = oldImage.height();

        final int newWidthCap = BindingBasicsUtils.computeStorageSpan(
                oldWidthCap,
                minWidthCap,
                ALLOW_OB_SHRINKING);
        final int newHeightCap = BindingBasicsUtils.computeStorageSpan(
                oldHeightCap,
                minHeightCap,
                ALLOW_OB_SHRINKING);

        final boolean needNewStorage =
                (newWidthCap != oldWidthCap)
                || (newHeightCap != oldHeightCap);

        if (needNewStorage) {
            final QImage newImage = new QImage(
                    newWidthCap,
                    newHeightCap,
                    oldImage.format());
            this.offscreenImage = newImage;

            if (MUST_PRESERVE_OB_CONTENT_ON_RESIZE) {
                final QPainter painterTo = new QPainter();
                painterTo.begin(newImage);
                painterTo.drawImage(0, 0, oldImage);
                painterTo.end();
            }

            oldImage.dispose();
        }
    }

    /*
     * 
     */
    
    private static boolean isSpamEvent(QEvent backingEvent) {
        final Type type = backingEvent.type();

        return (backingEvent instanceof QMoveEvent)
                || (backingEvent instanceof QResizeEvent)
                || (backingEvent instanceof QPaintEvent)
                || (backingEvent instanceof QMouseEvent)
                || (type == Type.NonClientAreaMouseMove)
                || (backingEvent instanceof QWheelEvent);
    }
}
