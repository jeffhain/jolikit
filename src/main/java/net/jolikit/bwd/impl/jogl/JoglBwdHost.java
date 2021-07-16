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
package net.jolikit.bwd.impl.jogl;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL3ES3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDebugListener;
import com.jogamp.opengl.GLDebugMessage;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.ConfiguredExceptionHandler;
import net.jolikit.bwd.impl.utils.InterfaceHostLifecycleListener;
import net.jolikit.bwd.impl.utils.basics.PixelCoordsConverter;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;
import net.jolikit.time.TimeUtils;

public class JoglBwdHost extends AbstractBwdHost {

    /*
     * TODO jogl If ever needed:
     * "If you wish to be notified when the framebuffer of a window is resized,
     * whether by the user or the system, set a size callback."
     * glfwSetFramebufferSizeCallback(window, framebuffer_size_callback);
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean MUST_PRESERVE_OB_CONTENT_ON_RESIZE = true;

    private static final boolean ALLOW_OB_SHRINKING = true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private class MyGlEl implements GLEventListener {
        public MyGlEl() {
        }
        @Override
        public void init(GLAutoDrawable drawable) {
            
            if (DEBUG) {
                hostLog(this, "GLEventListener.init(" + drawable.hashCode() + ")");
            }
            
            /*
             * 
             */
            
            final GRect defaultClientBoundsInOs =
                getBindingConfig().getDefaultClientOrWindowBoundsInOs();
            final int initialWidth = defaultClientBoundsInOs.xSpan();
            final int initialHeight = defaultClientBoundsInOs.ySpan();
            final PixelCoordsConverter pixelCoordsConverter = getBinding().getPixelCoordsConverter();
            final int initialDeviceWidth = pixelCoordsConverter.computeXSpanInDevicePixel(initialWidth);
            final int initialDeviceHeight = pixelCoordsConverter.computeYSpanInDevicePixel(initialHeight);
            reshape(drawable, 0, 0, initialDeviceWidth, initialDeviceHeight);
            
            if (DEBUG) {
                window.getContext().addGLDebugListener(new GLDebugListener() {
                    @Override
                    public void messageSent(GLDebugMessage message) {
                        hostLog(this, message);
                    }
                });
            }
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int widthInDevice, int heightInDevice) {

            final PixelCoordsConverter pixelCoordsConverter =
                getBinding().getPixelCoordsConverter();
            final int lastReshapedWidthInOs =
                pixelCoordsConverter.computeXSpanInOsPixel(
                    widthInDevice);
            final int lastReshapedHeightInOs =
                pixelCoordsConverter.computeYSpanInOsPixel(
                    heightInDevice);
            
            if (DEBUG) {
                hostLog(this,
                        "GLEventListener.reshape(" + drawable.hashCode()
                        + ", " + x + ", " + y
                        + ", " + widthInDevice + ", " + heightInDevice + ")");
                hostLog(this, "lastReshapedWidthInOs = " + lastReshapedWidthInOs);
                hostLog(this, "lastReshapedHeightInOs = " + lastReshapedHeightInOs);
            }
            
            /*
             * 
             */
            
            hostBoundsHelper.setLastReshapedClientSpansInOs(
                lastReshapedWidthInOs,
                lastReshapedHeightInOs);

            if (getBindingConfig().getMustTryToPaintDuringResize()) {
                // Making all dirty on reshape, so that everything gets repainted.
                makeAllDirty();
            }

            /*
             * When a context is first attached to a window,
             * the viewport will be set to the dimensions of the window,
             * i.e. the equivalent of glViewport(0,0,width,height).
             * So it's only necessary to call glViewport(...)
             * when the window size changes or if you want to set
             * the viewport to something other than the entire window.
             */
            final GL3ES3 gl = JoglPaintHelper.getGlToUse(drawable);
            gl.glViewport(x, y, widthInDevice, heightInDevice);

            onBackingWindowResized();
        }
        @Override
        public void display(GLAutoDrawable drawable) {
            
            if (DEBUG) {
                hostLog(this, "GLEventListener.display(" + drawable.hashCode() + ")");
            }
            
            /*
             * We might not be in UI thread here, but it looks like jogl takes care
             * of not calling current method concurrently, and also at this time
             * our UI thread should be blocked in a call to display() or something
             * so we should not have concurrent call to helper dispose method
             * during host closing.
             */
            
            paintClientNowOnGlWindow();
        }
        @Override
        public void dispose(GLAutoDrawable drawable) {
            if (DEBUG) {
                hostLog(this, "GLEventListener.dispose(" + drawable.hashCode() + ")"/*, new RuntimeException("for stack")*/);
            }
        }
    }

    /*
     * 
     */

    private class MyWindowListener implements WindowListener {
        public MyWindowListener() {
        }
        @Override
        public void windowRepaint(WindowUpdateEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowRepaint(" + backingEvent + ")");
            }
            /*
             * TODO jogl These events come with much delay.
             */
        }
        @Override
        public void windowMoved(WindowEvent backingEvent) {
            if (DEBUG_SPAM) {
                hostLog(this, "windowMoved(" + backingEvent + ")");
            }
            
            onBackingWindowMoved();
        }
        @Override
        public void windowResized(WindowEvent backingEvent) {
            if (DEBUG_SPAM) {
                hostLog(this, "windowResized(" + backingEvent + ")");
            }
            /*
             * TODO jogl These events come with much delay.
             * Nothing to do.
             * We listen to resizes in GLEventListener.reshape(...).
             */
        }
        @Override
        public void windowGainedFocus(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowGainedFocus(" + backingEvent + ")");
            }
            /*
             * try/finally so that we detect focus gain
             * even if client throws on resize.
             */
            try {
                if (getBinding().getBindingConfig().getMustShakeClientBoundsOnFocusGained()) {
                    /*
                     * TODO jogl It seems that with JOGL/NEWT, bounds settings
                     * causes focus gain (maybe due to painting causing focus gain),
                     * so we only do the shaking if nothing did shake recently,
                     * else it can get windows into a shaking frenzy when there
                     * are multiple ones not showing up.
                     */
                    final double nowS = getUiThreadSchedulerClock().getTimeS();
                    final double npsfTimeS = getNewestPossibleUnstabilityTimeS();
                    final double dtS = nowS - npsfTimeS;
                    final boolean beenQuiescentForSomeTime =
                            (dtS > getBindingConfig().getBackingWindowStateStabilityDelayS());
                    if (beenQuiescentForSomeTime) {
                        final GRect clientBoundsInOs = getClientBoundsInOs();
                        // We should be visible, but testing for safety.
                        if (!clientBoundsInOs.isEmpty()) {
                            // Not using setClientBounds(...),
                            // to make sure we don't set "target bounds".
                            try {
                                setBackingClientBoundsInOs(
                                    clientBoundsInOs.withSpansDeltas(1, 0));
                            } finally {
                                setBackingClientBoundsInOs(clientBoundsInOs);
                            }
                        }
                    }
                }
            } finally {
                onBackingWindowFocusGained();
            }
        }
        @Override
        public void windowLostFocus(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowLostFocus(" + backingEvent + ")");
            }
            onBackingWindowFocusLost();
        }
        @Override
        public void windowDestroyNotify(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowDestroyNotify(" + backingEvent + ")");
            }
            onBackingWindowClosing();
        }
        @Override
        public void windowDestroyed(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "windowDestroyed(" + backingEvent + ")");
            }
            onBackingWindowClosing();
        }
    }

    private class MyKeyListener implements KeyListener {
        public MyKeyListener() {
        }
        @Override
        public void keyPressed(KeyEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "keyPressed(" + backingEvent + ")");
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
                if (DEBUG) {
                    hostLog(this, "KeyListener.keyPressed(...) : " + event);
                }
                onBackingKeyPressed(event);
            }
            {
                final BwdKeyEventT event = eventConverter.newKeyTypedEventElseNull(backingEvent);
                if (DEBUG) {
                    hostLog(this, "KeyListener.keyPressed(...) : " + event);
                }
                if (event != null) {
                    onBackingKeyTyped(event);
                }
            }
        }
        @Override
        public void keyReleased(KeyEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "keyReleased(" + backingEvent + ")");
            }
            final BwdKeyEventPr event = eventConverter.newKeyReleasedEvent(backingEvent);

            if (!backingEvent.isAutoRepeat()) {
                onBackingKeyReleased(event);
            }
        }
    }

    private class MyMouseListener implements MouseListener {
        public MyMouseListener() {
        }
        @Override
        public void mousePressed(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "mousePressed(" + backingEvent + ")");
            }
            
            if (mustIgnoreBackingMousePressedEvent(backingEvent)) {
                if (DEBUG) {
                    hostLog(this, "ignoring backing mouse pressed event (looks like a duplicate one)");
                }
            } else {
                final BwdMouseEvent event = eventConverter.newMousePressedEventElseNull(backingEvent);
                if (event != null) {
                    onBackingMousePressed(event);
                }
            }
        }
        @Override
        public void mouseReleased(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "mouseReleased(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseReleasedEventElseNull(backingEvent);
            if (event != null) {
                onBackingMouseReleased(event);
            }
        }
        @Override
        public void mouseClicked(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "mouseClicked(" + backingEvent + ")");
            }
        }
        @Override
        public void mouseMoved(MouseEvent backingEvent) {
            if (DEBUG_SPAM) {
                hostLog(this, "mouseMoved(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseMovedEventElseNull(backingEvent);
            if (event != null) {
                onBackingMouseMoved(event);
            }
        }
        @Override
        public void mouseEntered(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "mouseEntered(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseEnteredClientEventElseNull(backingEvent);
            if (event != null) {
                onBackingMouseEnteredClient(event);
            }
        }
        @Override
        public void mouseExited(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "mouseExited(" + backingEvent + ")");
            }
            clearMousePressedEventNotToDuplicate();
            final BwdMouseEvent event = eventConverter.newMouseExitedClientEventElseNull(backingEvent);
            if (event != null) {
                onBackingMouseExitedClient(event);
            }
        }
        @Override
        public void mouseDragged(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "mouseDragged(" + backingEvent + ")");
            }
            // Bindings must not generate mouse dragged events.
            final BwdMouseEvent event = eventConverter.newMouseMovedEventElseNull(backingEvent);
            if (event != null) {
                onBackingMouseMoved(event);
            }
        }
        @Override
        public void mouseWheelMoved(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "mouseWheelMoved(" + backingEvent + ")");
            }
            final BwdWheelEvent event = eventConverter.newWheelEventElseNull(backingEvent);
            if (event != null) {
                onBackingWheelEvent(event);
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * Might be good to keep hard references to these listeners
     * (as for lwjgl3), so we do.
     */
    
    private final WindowListener windowListener;
    private final KeyListener keyListener;
    private final MouseListener mouseListener;
    
    /*
     * 
     */

    private final JoglEventConverter eventConverter;

    private final GLWindow window;

    private final JoglHostBoundsHelper hostBoundsHelper;

    private final JoglBwdCursorManager cursorManager;

    private final IntArrayGraphicBuffer offscreenBuffer;

    private final JoglPaintHelper paintHelper = new JoglPaintHelper();

    private double lastBackingBoundsSettingTimeS = Double.NEGATIVE_INFINITY;

    /*
     * Cf. duplicateMousePressedBlockingDelayS.
     */
    
    private boolean backingBoundsBeingSet;
    
    private MouseEvent mousePressedEventNotToDuplicate = null;
    private double mousePressedEventNotToDuplicateTimeS = Double.NEGATIVE_INFINITY;
    
    /*
     * 
     */
    
    /**
     * Only used if mustDoBestEffortIconification is true.
     */
    private boolean backingWindowIconified_hack = false;
    
    /**
     * Only used if mustDoBestEffortIconification is true.
     * 
     * To know where to move the window back to when back to showing
     * and deiconified, we use super class target showing/deico/demax bounds
     * to enforce for demaximized windows, and this field for maximized windows.
     */
    private GRect windowBoundsInOsToRestoreOnShowingDeicoMax = null;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param openGlThreadScheduler Null if JOGL is not single threaded.
     *        Can be the UI thread scheduler.
     */
    public JoglBwdHost(
            AbstractJoglBwdBinding binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            JoglBwdHost owner,
            //
            String title,
            boolean decorated,
            boolean modal,
            //
            InterfaceBwdClient client,
            //
            JoglCursorRepository backingCursorRepository) {
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

        final JoglBwdBindingConfig bindingConfig = binding.getBindingConfig();
        
        final AbstractBwdHost host = this;
        this.hostBoundsHelper = new JoglHostBoundsHelper(host);
        
        this.eventConverter = new JoglEventConverter(
                binding.getEventsConverterCommonState(),
                host,
                binding.getPixelCoordsConverter());
        
        this.windowListener = new JoglWindowListenerUiThreader(
                new MyWindowListener(),
                binding.getUiThreadScheduler());
        this.keyListener = new MyKeyListener();
        this.mouseListener = new MyMouseListener();

        final Screen screen = binding.getScreen();
        {
            final GLProfile glProfile = JoglPaintHelper.getGlProfiletoUse();
            final GLCapabilities glCapabilities = new GLCapabilities(glProfile);

            glCapabilities.setDoubleBuffered(
                bindingConfig.getGlDoubleBuffered());

            if (false) {
                // TODO jogl Would help? In what?
                glCapabilities.setHardwareAccelerated(true);
            }
            
            // TODO jogl Doesn't seem to work: background is still opaque.
            glCapabilities.setBackgroundOpaque(false);
            
            /*
             * 
             */

            final GLWindow window = GLWindow.create(screen, glCapabilities);

            this.window = window;

            this.cursorManager = new JoglBwdCursorManager(
                    backingCursorRepository,
                    window);

            if (DEBUG) {
                window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
            }

            final GRect defaultClientBoundsInOs =
                bindingConfig.getDefaultClientOrWindowBoundsInOs();
            final int initialWidth = defaultClientBoundsInOs.xSpan();
            final int initialHeight = defaultClientBoundsInOs.ySpan();

            window.setUndecorated(!decorated);

            if (owner != null) {
                // TODO jogl Best effort (we would like modal-for-owner).
                window.setAlwaysOnTop(modal);
            }

            window.setTitle(title);

            window.setResizable(true);
            window.setSize(initialWidth, initialHeight);

            if (false) {
                // TODO jogl What is this? Docs don't say much.
                window.setSticky(true);
            }

            // TODO jogl Seems optional, but docs tell to do it.
            window.setRealized(true);

            final UncaughtExceptionHandler exceptionHandler = new ConfiguredExceptionHandler(bindingConfig);
            final MyGlEl glEventListener = new MyGlEl();
            final JoglGLEventListenerHandler glEventListenerWrapper = new JoglGLEventListenerHandler(
                    exceptionHandler,
                    glEventListener);
            window.addGLEventListener(glEventListenerWrapper);

            window.addWindowListener(this.windowListener);
            window.addKeyListener(this.keyListener);
            window.addMouseListener(this.mouseListener);
        }

        this.offscreenBuffer = new IntArrayGraphicBuffer(
                MUST_PRESERVE_OB_CONTENT_ON_RESIZE,
                ALLOW_OB_SHRINKING);

        hostLifecycleListener.onHostCreated(this);
    }

    /*
     * 
     */

    @Override
    public JoglBwdBindingConfig getBindingConfig() {
        return (JoglBwdBindingConfig) super.getBindingConfig();
    }

    @Override
    public AbstractJoglBwdBinding getBinding() {
        return (AbstractJoglBwdBinding) super.getBinding();
    }
    
    /*
     * 
     */

    @Override
    public JoglBwdCursorManager getCursorManager() {
        return this.cursorManager;
    }

    /*
     * 
     */

    @Override
    public GLWindow getBackingWindow() {
        return this.window;
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
        final JoglBwdHost owner = this;
        return new JoglBwdHost(
            this.getBinding(),
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
        
        try {
            if (DEBUG) {
                hostLog(this, "window.display()...");
            }
            final long a = (DEBUG ? System.nanoTime() : 0L);
            
            this.window.display();
            
            final long b = (DEBUG ? System.nanoTime() : 0L);
            if (DEBUG) {
                hostLog(this, "window.display() took " + TimeUtils.nsToS(b-a) + " s");
            }
        } catch (GLException ignore) {
            /*
             * TODO jogl Can happen in case user shuts down during painting,
             * in which case we get that:
             * com.jogamp.opengl.GLException: Caught NullPointerException: null on thread main
             *     at com.jogamp.opengl.GLException.newGLException(GLException.java:76)
             *     at jogamp.opengl.GLDrawableHelper.invokeGLImpl(GLDrawableHelper.java:1331)
             *     at jogamp.opengl.GLDrawableHelper.invokeGL(GLDrawableHelper.java:1147)
             *     at com.jogamp.newt.opengl.GLWindow.display(GLWindow.java:759)
             *     at [current method]
             *     (...)
             * Caused by: java.lang.NullPointerException
             *     at jogamp.newt.WindowImpl.getGraphicsConfiguration(WindowImpl.java:1122)
             *     at jogamp.newt.WindowImpl.unlockSurface(WindowImpl.java:1065)
             *     at jogamp.opengl.GLDrawableImpl.unlockSurface(GLDrawableImpl.java:332)
             *     at jogamp.opengl.GLContextImpl.release(GLContextImpl.java:425)
             *     at jogamp.opengl.GLContextImpl.release(GLContextImpl.java:382)
             *     at jogamp.opengl.GLDrawableHelper.invokeGLImpl(GLDrawableHelper.java:1308)
             *     at jogamp.opengl.GLDrawableHelper.invokeGL(GLDrawableHelper.java:1147)
             *     at com.jogamp.newt.opengl.GLWindow.display(GLWindow.java:759)
             *     at [current method]
             *     (...)
             */
        }
    }

    @Override
    protected void setWindowAlphaFp_backing(double windowAlphaFp) {
        // Not supported.
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
        if (this.getBindingConfig().getMustDoBestEffortIconification()) {
            if (this.isBackingWindowShowing()) {
                return;
            }
            
            this.window.setVisible(true);

            if (!this.isBackingWindowIconified()) {
                final boolean maximized = this.isMaximized();
                this.icoHack_restoreBackingBoundsOnShowingDeico(maximized);
            }
        } else {
            this.window.setVisible(true);
        }
    }

    @Override
    protected void hideBackingWindow() {
        if (this.getBindingConfig().getMustDoBestEffortIconification()) {
            if (!this.isBackingWindowShowing()) {
                return;
            }
            
            final GRect windowBoundsInOs = this.getWindowBoundsInOs();
            if (windowBoundsInOs.isEmpty()) {
                // Can happen if iconified.
            } else {
                this.icoHack_setWindowBoundsInOsToRestoreWith(
                    windowBoundsInOs);
            }

            this.window.setVisible(false);
            
            if (!windowBoundsInOs.isEmpty()) {
                this.icoHack_moveBackingWindowToHackPosition();
            }
        } else {
            this.window.setVisible(false);
        }
    }

    /*
     * Focus.
     */

    @Override
    protected boolean isBackingWindowFocused() {
        if (this.getBindingConfig().getMustDoBestEffortIconification()) {
            if (this.isBackingWindowIconified()) {
                return false;
            }
        }
        return this.window.hasFocus();
    }
    
    @Override
    protected void requestBackingWindowFocusGain() {
        final boolean wait = false;
        this.window.requestFocus(wait);
    }

    /*
     * Iconified.
     */

    @Override
    protected boolean doesSetBackingWindowIconifiedOrNotWork() {
        if (this.getBindingConfig().getMustDoBestEffortIconification()) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    protected boolean isBackingWindowIconified() {
        if (this.getBindingConfig().getMustDoBestEffortIconification()) {
            return this.backingWindowIconified_hack;
        } else {
            return false;
        }
    }
    
    @Override
    protected void setBackingWindowIconifiedOrNot(boolean iconified) {
        if (this.getBindingConfig().getMustDoBestEffortIconification()) {
            if (!this.isBackingWindowShowing()) {
                // Allowed not to work in this case.
                return;
            }
            
            final boolean oldIconified = this.backingWindowIconified_hack;
            if (iconified == oldIconified) {
                return;
            }
            
            if (iconified) {
                final GRect windowBoundsInOs = this.getWindowBoundsInOs();
                
                // Set before, to block eventual window moved events
                // on backing bounds setting.
                this.backingWindowIconified_hack = true;
                
                if (windowBoundsInOs.isEmpty()) {
                    // Should not happen, but making sure we don't set crazy bounds.
                } else {
                    this.icoHack_setWindowBoundsInOsToRestoreWith(
                        windowBoundsInOs);
                    
                    this.icoHack_moveBackingWindowToHackPosition();
                }
            } else {
                try {
                    /*
                     * Not using isMaximized(), to get the actual state
                     * even if we did not already deiconify.
                     */
                    final boolean maximized = this.isBackingWindowMaximized();
                    this.icoHack_restoreBackingBoundsOnShowingDeico(maximized);
                } finally {
                    // Set after, for eventual moved events on backing bounds
                    // setting to be blocked during bounds restoration.
                    this.backingWindowIconified_hack = false;
                }
            }
            
            // For event to be generated ASAP.
            this.onBackingWindowAnyEvent();
        } else {
            // Nothing to do.
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
        return this.window.isMaximizedHorz()
                && this.window.isMaximizedVert();
    }

    @Override
    protected void setBackingWindowMaximizedOrNot(boolean maximized) {
        final boolean horz = maximized;
        final boolean vert = maximized;
        this.window.setMaximized(horz, vert);
    }

    /*
     * Bounds.
     */

    @Override
    protected GRect getBackingInsetsInOs() {
        return this.hostBoundsHelper.getInsetsInOs();
    }

    @Override
    protected GRect getBackingClientBoundsInOs() {
        return this.hostBoundsHelper.getClientBoundsInOs();
    }

    @Override
    protected GRect getBackingWindowBoundsInOs() {
        return this.hostBoundsHelper.getWindowBoundsInOs();
    }

    @Override
    protected void setBackingClientBoundsInOs(GRect targetClientBoundsInOs) {
        this.backingBoundsBeingSet = true;
        try {
            this.lastBackingBoundsSettingTimeS = this.getUiThreadSchedulerClock().getTimeS();
            this.hostBoundsHelper.setClientBoundsInOs(targetClientBoundsInOs);
        } finally {
            this.backingBoundsBeingSet = false;
        }
    }

    @Override
    protected void setBackingWindowBoundsInOs(GRect targetWindowBoundsInOs) {
        this.backingBoundsBeingSet = true;
        try {
            this.lastBackingBoundsSettingTimeS = this.getUiThreadSchedulerClock().getTimeS();
            this.hostBoundsHelper.setWindowBoundsInOs(targetWindowBoundsInOs);
        } finally {
            this.backingBoundsBeingSet = false;
        }
    }

    /*
     * Closing.
     */
    
    @Override
    protected void closeBackingWindow() {
        try {
            this.window.destroy();
        } catch (RuntimeException ignore) {
            /*
             * TODO jogl On Mac:
             * Thread[main-Display-.macosx_nil-1-EDT-1,5,main]: Warning: Default-EDT about (2)
             *  to stop, task executed. Remaining tasks: 1 - Thread[main-Display-.macosx_nil-1-EDT-1,5,main]
             * Exception in thread "main" java.lang.RuntimeException: java.lang.Throwable:
             *  main-Display-.macosx_nil-1-EDT-1: Default-EDT finished w/ 1 left, task #0
             *     at jogamp.newt.DefaultEDTUtil.invokeImpl(DefaultEDTUtil.java:252)
             *     at jogamp.newt.DefaultEDTUtil.invoke(DefaultEDTUtil.java:165)
             *     at jogamp.newt.DisplayImpl.runOnEDTIfAvail(DisplayImpl.java:442)
             *     at jogamp.newt.WindowImpl.runOnEDTIfAvail(WindowImpl.java:2782)
             *     at jogamp.newt.WindowImpl.destroy(WindowImpl.java:1522)
             *     at com.jogamp.newt.opengl.GLWindow.destroy(GLWindow.java:568)
             */
        }

        this.paintHelper.dispose();
    }

    /*
     * 
     */
    
    @Override
    protected boolean mustBlockDragMoveDetection() {
        final double nowS = this.getUiThreadSchedulerClock().getTimeS();
        final double dtS = nowS - this.lastBackingBoundsSettingTimeS;
        final boolean mustBlock =
                (dtS < this.getBindingConfig().getPostBoundsSettingDragMoveBlockingDelayS());
        if (mustBlock) {
            if (DEBUG) {
                hostLog(this, "blocking drag move detection, because of recent bounds setting : dtS = " + dtS);
            }
        }
        return mustBlock;
    }

    /*
     * Painting.
     */
    
    @Override
    protected InterfaceBwdGraphics newRootGraphics(GRect boxWithBorder) {
        
        final boolean isImageGraphics = false;
        
        this.offscreenBuffer.setSize(
            boxWithBorder.xSpan(),
            boxWithBorder.ySpan());
        final int[] pixelArr =
            this.offscreenBuffer.getPixelArr();
        final int pixelArrScanlineStride =
            this.offscreenBuffer.getScanlineStride();
        
        return new JoglBwdGraphics(
            this.getBinding(),
            boxWithBorder,
            //
            isImageGraphics,
            pixelArr,
            pixelArrScanlineStride);
    }
    
    @Override
    protected void paintBackingClient(
        ScaleHelper scaleHelper,
        GPoint clientSpansInOs,
        GPoint bufferPosInCliInOs,
        GPoint bufferSpansInBd,
        List<GRect> paintedRectList) {
        
        if (paintedRectList.isEmpty()) {
            return;
        }
        
        final boolean glDoubleBuffered =
            this.getBindingConfig().getGlDoubleBuffered();
        if (glDoubleBuffered) {
            /*
             * Can't do partial painting if double buffered,
             * else does flip-flop painting.
             */
            paintedRectList.clear();
            final GRect bufferFullBox =
                GRect.valueOf(
                    0,
                    0,
                    bufferSpansInBd.x(),
                    bufferSpansInBd.y());
            paintedRectList.add(bufferFullBox);
        }

        this.paintHelper.paintPixelsIntoOpenGl(
            scaleHelper,
            clientSpansInOs,
            bufferPosInCliInOs,
            paintedRectList,
            //
            this.offscreenBuffer,
            this.window);
        
        this.flushPainting();
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void icoHack_setWindowBoundsInOsToRestoreWith(
        GRect windowBoundsInOsToRestore) {
        /*
         * Not using isMaximized(), to get the actual state
         * even if we did already iconify.
         */
        final boolean maximized = this.isBackingWindowMaximized();
        if (maximized) {
            if (DEBUG) {
                hostLog(this,
                    "windowBoundsInOsToRestoreOnShowingDeicoMax set to "
                        + windowBoundsInOsToRestore);
            }
            this.windowBoundsInOsToRestoreOnShowingDeicoMax =
                windowBoundsInOsToRestore;
        } else {
            this.setWindowBoundsInOsToEnforceOnShowDeicoDemaxAndFlag_protected(
                windowBoundsInOsToRestore);
        }
    }
    
    private void icoHack_moveBackingWindowToHackPosition() {
        /*
         * Not using getClientBoundsInOs(), to get the actual bounds
         * even if we did already hide.
         * Might be maximized bounds.
         */
        final GRect windowBoundsInOs =
            this.getBackingOrOnDragBeginWindowBoundsInOs();
        
        final GRect hidOrIcoWindowBoundsInOs = windowBoundsInOs.withPos(
                getBinding().getBindingConfig().getHiddenHackClientXInOs(),
                getBinding().getBindingConfig().getHiddenHackClientYInOs());
        if (DEBUG) {
            hostLog(this,
                "backing window bounds set to hidOrIcoWindowBoundsInOs = "
                    + hidOrIcoWindowBoundsInOs);
        }
        this.setBackingWindowBoundsInOs(hidOrIcoWindowBoundsInOs);
    }
    
    private void icoHack_restoreBackingBoundsOnShowingDeico(boolean maximized) {
        if (maximized) {
            final GRect windowBoundsInOsToEnforce =
                this.windowBoundsInOsToRestoreOnShowingDeicoMax;
            this.windowBoundsInOsToRestoreOnShowingDeicoMax = null;
            if (windowBoundsInOsToEnforce != null) {
                if (DEBUG) {
                    hostLog(this,
                        "backing window bounds set to windowBoundsInOsToRestoreOnShowingDeicoMax = "
                            + windowBoundsInOsToEnforce);
                }
                this.applyWindowBoundsInOsToEnforceOnShowDeicoMax_protected(
                    windowBoundsInOsToEnforce);
            }
        } else {
            // Cleanup, in case maximized state changed while hidden or iconified
            // (which is not supposed to happen).
            this.windowBoundsInOsToRestoreOnShowingDeicoMax = null;
            
            this.applyBoundsToEnforceOnShowDeicoDemaxIfAny_protected();
        }
    }
    
    /*
     * 
     */

    private void clearMousePressedEventNotToDuplicate() {
        if (DEBUG) {
            hostLog(this, "clearing mouse pressed event not to duplicate");
        }
        this.mousePressedEventNotToDuplicate = null;
        this.mousePressedEventNotToDuplicateTimeS = Double.NEGATIVE_INFINITY;
    }
    
    private static boolean haveSameTypeAndButtonsAndPos(MouseEvent e1, MouseEvent e2) {
        return (e1.getEventType() == e2.getEventType())
                && (e1.getButton() == e2.getButton())
                && (e1.getModifiers() == e2.getModifiers())
                && (e1.getX() == e2.getX())
                && (e1.getY() == e2.getY());
    }
    
    /**
     * @param backingEvent Backing mouse pressed event.
     * @return True if must ignore it, false if everything is fine
     *         and it must be accepted.
     */
    private boolean mustIgnoreBackingMousePressedEvent(MouseEvent backingEvent) {
        final double nowS = getUiThreadSchedulerClock().getTimeS();
        
        final MouseEvent lastEvent = this.mousePressedEventNotToDuplicate;
        final double lastTimeS = this.mousePressedEventNotToDuplicateTimeS;
        if (this.backingBoundsBeingSet) {
            if (DEBUG) {
                hostLog(this, "setting mouse pressed event as not to duplicate");
            }
            this.mousePressedEventNotToDuplicate = backingEvent;
            this.mousePressedEventNotToDuplicateTimeS = nowS;
        } else {
            this.clearMousePressedEventNotToDuplicate();
        }
        
        final boolean ret;
        if (lastEvent != null) {
            final double dtS = nowS - lastTimeS;
            ret = haveSameTypeAndButtonsAndPos(backingEvent, lastEvent)
                    && (dtS < getBinding().getBindingConfig().getDuplicateMousePressedBlockingDelayS());
        } else {
            ret = false;
        }
        return ret;
    }

    /*
     * 
     */
    
    private void paintClientNowOnGlWindow() {
        this.paintBwdClientNowAndBackingClient(
            getBinding().getBindingConfig(),
            this.hostBoundsHelper);
    }

    private void flushPainting() {
        // Not much more we can do.
        this.paintHelper.flushPainting(this.window);
    }
}
