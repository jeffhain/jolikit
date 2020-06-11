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
package net.jolikit.bwd.impl.lwjgl3;

import java.util.List;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglCharModsEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglCursorEnterEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglCursorPosEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglKeyEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglMouseButtonEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglScrollEvent;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.InterfaceHostLifecycleListener;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.OsUtils;
import net.jolikit.bwd.impl.utils.basics.PixelCoordsConverter;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.InterfaceScheduler;

public class LwjglBwdHost extends AbstractBwdHost {
    
    /*
     * TODO lwjgl As of 2017/06/28, transparent backgrounds don't work yet,
     * cf. https://github.com/glfw/glfw/issues/197.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean MUST_PRESERVE_OB_CONTENT_ON_RESIZE = true;

    private static final boolean ALLOW_OB_SHRINKING = true;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyHostEl extends AbstractLwjglHostEventListener {
        /**
         * Must set callbacks for it to work.
         */
        public MyHostEl(long window) {
            super(window);
        }
        
        /*
         * Window events.
         */
        
        @Override
        protected void onFramebufferSizeEvent(int width, int height) {
            if (DEBUG) {
                hostLog(this, "onFramebufferSizeEvent(" + width + ", " + height + ")");
            }
            // Ignoring this event, just using onWindowSizeEvent(...).
        }
        
        @Override
        protected void onWindowFocusEvent(boolean focused) {
            if (DEBUG) {
                hostLog(this, "onWindowFocusEvent(" + focused + ")");
            }
            if (focused) {
                /*
                 * try/finally so that we detect focus gain
                 * even if client throws on resize.
                 */
                try {
                    if (getBinding().getBindingConfig().getMustShakeClientBoundsOnFocusGained()) {
                        final GRect clientBounds = getClientBounds();
                        // We should be visible, but testing for safety.
                        if (!clientBounds.isEmpty()) {
                            // Not using setClientBounds(...),
                            // to make sure we don't set "target bounds".
                            try {
                                setBackingClientBounds(clientBounds.withSpansDeltas(1, 0));
                            } finally {
                                setBackingClientBounds(clientBounds);
                            }
                        }
                    }
                } finally {
                    backingWindowFocused = true;
                    onBackingWindowFocusGained();
                }
            } else {
                backingWindowFocused = false;
                onBackingWindowFocusLost();
            }
        }
        
        @Override
        protected void onWindowIconifyEvent(boolean iconified) {
            if (DEBUG) {
                hostLog(this, "onWindowIconifyEvent(" + iconified + ")");
            }
            if (iconified) {
                if (getBinding().getBindingConfig().getMustGuardAgainstHidingDueToIconification()) {
                    final boolean backingShowing = isBackingWindowShowing();
                    if (DEBUG) {
                        hostLog(this, "backingShowing = " + backingShowing);
                    }
                    if (!backingShowing) {
                        if (DEBUG) {
                            hostLog(this, "backing ICONIFIED while hidden : mustConsiderBackingNotHidden set to true");
                        }
                        mustConsiderBackingNotHidden = true;
                    }
                }
                onBackingWindowIconified();
            } else {
                onBackingWindowDeiconified();
            }
        }
        
        @Override
        protected void onWindowRefreshEvent() {
            if (DEBUG) {
                hostLog(this, "onWindowRefreshEvent()");
            }
            // Works for when the window gets de-iconified,
            // or gets raised (to screen front),
            // and possibly in other cases.
            // Maybe useless due to painting on focus gained,
            // but should not hurt.
            makeAllDirtyAndEnsurePendingClientPainting();
            
            // Maybe useful.
            onBackingWindowAnyEvent();
        }
        
        @Override
        protected void onWindowPosEvent(int xpos, int ypos) {
            if (DEBUG_SPAM) {
                hostLog(this, "onWindowPosEvent(" + xpos + ", " + ypos + ")");
            }
            onBackingWindowMoved();
        }
        
        @Override
        protected void onWindowSizeEvent(int width, int height) {
            if (DEBUG_SPAM) {
                hostLog(this, "onWindowSizeEvent(" + width + ", " + height + ")");
            }
            onBackingWindowResized();
        }
        
        @Override
        protected void onWindowCloseEvent() {
            if (DEBUG) {
                hostLog(this, "onWindowCloseEvent()");
            }
            if (false) {
                /*
                 * TODO lwjgl Some closing-related mechanism,
                 * if ever useful.
                 */
                GLFW.glfwSetWindowShouldClose(window, true);
                if (GLFW.glfwWindowShouldClose(window)) {
                }
            }
            onBackingWindowClosing();
        }
        
        /*
         * Key events.
         */
        
        @Override
        protected void onKeyEvent(int backingKey, int scancode, int action, int mods) {
            if (DEBUG) {
                hostLog(this, "onKeyEvent(" + backingKey + ", " + scancode + ", " + action + ", " + mods + ")");
            }
            if (!isFocused()) {
                /*
                 * TODO lwjgl After focus loss, key release events are generated
                 * by LWJGL, with proper "key", but with zero "scancode".
                 * To ignore these broken synthetic events, we ignore those
                 * occurring when not focused.
                 */
                return;
            }
            
            if (action == GLFW.GLFW_PRESS) {
                /*
                 * TODO lwjgl onCharModsEvent(...) (for our key typed events)
                 * are not generated for keys such as ESCAPE or TAB,
                 * so we have to generate corresponding key typed events here.
                 */

                final int key;
                {
                    final LwjglKeyEvent backingEvent = new LwjglKeyEvent(backingKey, scancode, action, mods);
                    final BwdKeyEventPr event = eventConverter.newKeyPressedEvent(backingEvent);
                    onBackingKeyPressed(event);
                    key = event.getKey();
                }
                {
                    final int codePoint;
                    if ((key == BwdKeys.ESCAPE)
                            || (key == BwdKeys.TAB)) {
                        codePoint = BwdKeys.codePointForKey(key);
                    } else {
                        codePoint = -1;
                    }
                    if (codePoint >= 0) {
                        final LwjglCharModsEvent backingEvent = new LwjglCharModsEvent(codePoint, mods);
                        final BwdKeyEventT event = eventConverter.newKeyTypedEventElseNull(backingEvent);
                        if (event != null) {
                            onBackingKeyTyped(event);
                        }
                    }
                }
            } else if (action == GLFW.GLFW_RELEASE) {
                final LwjglKeyEvent backingEvent = new LwjglKeyEvent(backingKey, scancode, action, mods);
                final BwdKeyEventPr event = eventConverter.newKeyReleasedEvent(backingEvent);
                onBackingKeyReleased(event);
            }
        }
        
        @Override
        protected void onCharModsEvent(int codepoint, int mods) {
            if (DEBUG) {
                hostLog(this, "onCharModsEvent(" + codepoint + ", " + mods + ")");
            }
            final LwjglCharModsEvent backingEvent = new LwjglCharModsEvent(codepoint, mods);
            final BwdKeyEventT event = eventConverter.newKeyTypedEventElseNull(backingEvent);
            if (event != null) {
                onBackingKeyTyped(event);
            }
        }
        
        @Override
        protected void onCharEvent(int codepoint) {
            if (DEBUG) {
                hostLog(this, "onCharEvent(" + codepoint + ")");
            }
            /*
             * TODO lwjgl CharEvent seems to always be preceded
             * by a CharModsEvent, so we ignore CharEvent and
             * just use CharModsEvent, which contains modifiers
             * in addition to the code point.
             */
        }
        
        /*
         * Mouse events.
         */
        
        @Override
        protected void onCursorPosEvent(double xpos, double ypos) {
            if (DEBUG_SPAM) {
                hostLog(this, "onCursorPosEvent(" + xpos + ", " + ypos + ")");
            }
            final boolean mustRelyOnBackingMouseMovedEvents =
                    (synthMmeProcess == null);
            if (mustRelyOnBackingMouseMovedEvents) {
                final LwjglCursorPosEvent backingEvent = new LwjglCursorPosEvent(xpos, ypos);
                final BwdMouseEvent event = eventConverter.newMouseMovedEvent(backingEvent);
                if (DEBUG_SPAM) {
                    hostLog(this, "onCursorPosEvent(" + xpos + ", " + ypos + ") : " + event);
                }
                onBackingMouseMoved(event);
            }
        }
        
        @Override
        protected void onCursorEnterEvent(boolean entered) {
            if (DEBUG) {
                hostLog(this, "onCursorEnterEvent(" + entered + ")");
            }
            
            final LwjglCursorEnterEvent backingEvent = new LwjglCursorEnterEvent(entered);
            
            if (entered) {
                final BwdMouseEvent event = eventConverter.newMouseEnteredClientEvent(backingEvent);
                onBackingMouseEnteredClient(event);
            } else {
                final BwdMouseEvent event = eventConverter.newMouseExitedClientEvent(backingEvent);
                onBackingMouseExitedClient(event);
            }
        }
        
        @Override
        protected void onMouseButtonEvent(int button, int action, int mods) {
            if (DEBUG) {
                hostLog(this, "onMouseButtonEvent(" + button + ", " + action + ", " + mods + ")");
            }
            
            final LwjglMouseButtonEvent backingEvent = new LwjglMouseButtonEvent(button, action, mods);
            
            if (action == GLFW.GLFW_PRESS) {
                final BwdMouseEvent event = eventConverter.newMousePressedEvent(backingEvent);
                onBackingMousePressed(event);
            } else if (action == GLFW.GLFW_RELEASE) {
                final BwdMouseEvent event = eventConverter.newMouseReleasedEvent(backingEvent);
                onBackingMouseReleased(event);
            }
        }
        
        /*
         * Wheel events.
         */
        
        @Override
        protected void onScrollEvent(double xoffset, double yoffset) {
            if (DEBUG_SPAM) {
                hostLog(this, "onScrollEvent(" + xoffset + ", " + yoffset + ")");
            }
            
            final LwjglScrollEvent backingEvent = new LwjglScrollEvent(xoffset, yoffset);
            
            final BwdWheelEvent event = eventConverter.newWheelEventElseNull(backingEvent);
            if (event != null) {
                onBackingWheelEvent(event);
            }
        }
        
        /*
         * Drop event.
         */
        
        @Override
        protected void onDropEvent(int count, long names) {
            if (DEBUG) {
                hostLog(this, "onDropEvent(" + count + ", " + names + ")");
            }
            /*
             * TODO lwjgl Not using it for anything, but still listening to it,
             * in case the log could help for debug.
             */
        }
    }

    /*
     * 
     */
    
    /**
     * To generate synthetic mouse moved events, cf. config.
     */
    private class MySynthMmeProcess extends AbstractProcess {
        private final double periodS;
        private GPoint lastPos = GPoint.valueOf(-1, -1);
        private final double[] tmpXPosArr = new double[1];
        private final double[] tmpYPosArr = new double[1];
        public MySynthMmeProcess(
                InterfaceScheduler uiThreadScheduler,
                double periodS) {
            super(uiThreadScheduler);
            this.periodS = periodS;
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {
            // Relying on client event state, for consistency
            // with other start/stop, and also for minimal overhead.
            if ((!isClientState_Showing()) || isClientState_iconified()) {
                this.stop();
                return 0;
            }
            
            final double[] xPosArr = this.tmpXPosArr;
            final double[] yPosArr = this.tmpYPosArr;
            GLFW.glfwGetCursorPos(window, xPosArr, yPosArr);
            final double xpos = xPosArr[0];
            final double ypos = yPosArr[0];
            
            final int x = BindingCoordsUtils.roundToInt(xpos);
            final int y = BindingCoordsUtils.roundToInt(ypos);
            final GPoint pos = GPoint.valueOf(x, y);
            if (!pos.equals(this.lastPos)) {
                this.lastPos = pos;
                // Async for this process not be stopped
                // if onBackingMouseMoved(...) throws.
                this.getScheduler().execute(new Runnable() {
                    @Override
                    public void run() {
                        final LwjglCursorPosEvent backingEvent = new LwjglCursorPosEvent(xpos, ypos);
                        final BwdMouseEvent event = eventConverter.newMouseMovedEvent(backingEvent);
                        if (DEBUG_SPAM) {
                            hostLog(this, "(synthetic) onCursorPosEvent(" + xpos + ", " + ypos + ") : " + event);
                        }
                        onBackingMouseMoved(event);
                    }
                });
            }
            return plusBounded(actualTimeNs, sToNsNoUnderflow(this.periodS));
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final AbstractLwjglBwdBinding binding;
    
    private final LwjglEventConverter eventConverter;
    
    /**
     * This handle is tied to both a window and a GL context.
     */
    private final long window;
    
    /**
     * Need to keep a reference to it, for callbacks not to be GCed.
     */
    private final MyHostEl hostEventListener;
    
    private final LwjglHostBoundsHelper hostBoundsHelper;
    
    private final GLCapabilities capabilities;
    
    private final LwjglPaintHelper paintHelper = new LwjglPaintHelper();
    
    private final LwjglBwdCursorManager cursorManager;

    private final IntArrayGraphicBuffer offscreenBuffer;
    
    /*
     * 
     */
    
    /**
     * Can be null.
     */
    private final MySynthMmeProcess synthMmeProcess;

    /*
     * 
     */

    private boolean mustConsiderBackingNotHidden = false;
    
    /**
     * TODO lwjgl Because LWJGL offers no getter for that.
     */
    private boolean backingWindowFocused = false;
    
    /**
     * TODO lwjgl Because LWJGL offers no way to read iconified state,
     * nor (clear) events to be notified of iconified state change
     * (as a result this value is wrong if updated from native decoration).
     */
    private boolean backingWindowIconified = false;
    
    /**
     * TODO lwjgl Because LWJGL offers no way to read maximized state,
     * nor events to be notified of maximized state change
     * (as a result this value is wrong if updated from native decoration).
     */
    private boolean backingWindowMaximized = false;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public LwjglBwdHost(
            AbstractLwjglBwdBinding binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            LwjglBwdHost owner,
            //
            String title,
            boolean decorated,
            boolean modal,
            //
            InterfaceBwdClient client,
            //
            LwjglCursorRepository backingCursorRepository) {
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
        
        final LwjglBwdBindingConfig bindingConfig = binding.getBindingConfig();
        
        this.binding = LangUtils.requireNonNull(binding);
        
        this.eventConverter = new LwjglEventConverter(
                binding.getEventsConverterCommonState(),
                this);
        
        /*
         * 
         */
        
        setStaticGlfwStuffsForWindowCreation(
                bindingConfig.getGlDoubleBuffered(),
                decorated);
        
        final GRect defaultClientBounds = bindingConfig.getDefaultClientOrWindowBounds();
        final int initialWidth = defaultClientBounds.xSpan();
        final int initialHeight = defaultClientBounds.ySpan();
        // GLFWmonitor *monitor
        final long monitorHandle = MemoryUtil.NULL;
        // GLFWwindow *share
        final long shareHandle = MemoryUtil.NULL;
        final long window = GLFW.glfwCreateWindow(
                initialWidth,
                initialHeight,
                title,
                monitorHandle,
                shareHandle);
        if (window == MemoryUtil.NULL) {
            // Can happen if required OpenGL version's features are not available.
            throw new BindingError("could not create window");
        }
        this.window = window;
        
        final AbstractBwdHost host = this;
        this.hostBoundsHelper = new LwjglHostBoundsHelper(
                host,
                bindingConfig.getDecorationInsets());
        
        final MyHostEl hostEventListener = new MyHostEl(window);
        this.hostEventListener = hostEventListener;
        hostEventListener.setCallbacks();
        
        this.cursorManager = new LwjglBwdCursorManager(
                backingCursorRepository,
                window);
        
        // Need to call it before capabilities creation.
        GLFW.glfwMakeContextCurrent(this.window);

        // True to make sure that things deprecated
        // since some recent version are not available.
        final boolean forwardCompatible = true;
        this.capabilities = GL.createCapabilities(forwardCompatible);
 
        this.offscreenBuffer = new IntArrayGraphicBuffer(
                MUST_PRESERVE_OB_CONTENT_ON_RESIZE,
                ALLOW_OB_SHRINKING);
        
        if (bindingConfig.getMustGenerateSyntheticMouseMovedEvents()) {
            this.synthMmeProcess = new MySynthMmeProcess(
                    binding.getUiThreadScheduler(),
                    bindingConfig.getSyntheticMouseMovesPeriodS());
        } else {
            this.synthMmeProcess = null;
        }

        if (false) {
            /*
             * TODO lwjgl If ever wanting to be notified
             * on bounds modifications.
             */
            GLFWFramebufferSizeCallbackI cb = new GLFWFramebufferSizeCallbackI() {
                @Override
                public void invoke(long window, int width, int height) {
                }
            };
            GLFW.glfwSetFramebufferSizeCallback(window, cb);
        }

        hostLifecycleListener.onHostCreated(this);
    }

    /*
     * 
     */

    @Override
    public AbstractLwjglBwdBinding getBinding() {
        return (AbstractLwjglBwdBinding) super.getBinding();
    }
    
    /*
     * 
     */

    @Override
    public LwjglBwdCursorManager getCursorManager() {
        return this.cursorManager;
    }

    /*
     * 
     */

    @Override
    public Long getBackingWindow() {
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
        final LwjglBwdHost owner = this;
        return new LwjglBwdHost(
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
    protected void beforeClientWindowShown_spe(BwdWindowEvent event) {
        if (!this.isClientState_iconified()) {
            if (this.synthMmeProcess != null) {
                this.synthMmeProcess.start();
            }
        }
    }
    
    @Override
    protected void beforeClientWindowHidden_spe(BwdWindowEvent event) {
        if (this.synthMmeProcess != null) {
            this.synthMmeProcess.stop();
        }
    }
    
    @Override
    protected void beforeClientWindowIconified_spe(BwdWindowEvent event) {
        if (this.synthMmeProcess != null) {
            this.synthMmeProcess.stop();
        }
    }
    
    @Override
    protected void beforeClientWindowDeiconified_spe(BwdWindowEvent event) {
        // Client state necessarily showing here.
        if (this.synthMmeProcess != null) {
            this.synthMmeProcess.start();
        }
    }
    
    /*
     * 
     */

    @Override
    protected void paintClientNowOrLater() {
        if (DEBUG) {
            hostLog(this, "paintClientNowOrLater()");
        }
        
        final InterfaceBwdClient client = this.getClientWithExceptionHandler();
        
        client.processEventualBufferedEvents();
        
        if (!this.canPaintClientNow()) {
            return;
        }
        
        final GRect clientBounds = this.getClientBounds();
        final int width = clientBounds.xSpan();
        final int height = clientBounds.ySpan();
        if ((width <= 0) || (height <= 0)) {
            return;
        }

        final GRect box = GRect.valueOf(0, 0, width, height);

        final GRect dirtyRect = this.getAndResetDirtyRectBb();
        
        /*
         * Painting into offscreen buffer.
         */
        
        this.offscreenBuffer.setSize(width, height);
        final int[] clientPixelArr = this.offscreenBuffer.getPixelArr();
        final int clientPixelArrScanlineStride = this.offscreenBuffer.getScanlineStride();

        final LwjglBwdGraphics g = new LwjglBwdGraphics(
                this.binding,
                box,
                //
                clientPixelArr,
                clientPixelArrScanlineStride);

        final List<GRect> paintedRectList = this.getClientPainterNoRec().paintClientAndClipRects(
                g,
                dirtyRect);
        
        /*
         * Copying offscreen buffer into OpenGL.
         */
        
        if (paintedRectList.size() != 0) {
            if (this.canPaintClientNow()) {
                final boolean glDoubleBuffered =
                        binding.getBindingConfig().getGlDoubleBuffered();
                if (glDoubleBuffered) {
                    /*
                     * Can't do partial painting if double buffered,
                     * else does flip-flop painting.
                     */
                    paintedRectList.clear();
                    final GRect pseudoPaintedRect = GRect.valueOf(
                            0,
                            0,
                            offscreenBuffer.getWidth(),
                            offscreenBuffer.getHeight());
                    paintedRectList.add(pseudoPaintedRect);
                }

                final PixelCoordsConverter pixelCoordsConverter = getBinding().getPixelCoordsConverter();
                this.paintHelper.paintPixelsIntoOpenGl(
                        pixelCoordsConverter,
                        this.offscreenBuffer,
                        paintedRectList,
                        this.window,
                        this.capabilities);

                this.flushPainting();
            }
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
        if (this.isClosed_nonVolatile()) {
            // To avoid crashes due to disposed window,
            // and for logic below.
            return false;
        }
        
        final boolean backingShowing = (GLFW.glfwGetWindowAttrib(this.window, GLFW.GLFW_VISIBLE) != 0);
        final boolean ret;
        if (getBinding().getBindingConfig().getMustGuardAgainstHidingDueToIconification()) {
            if (backingShowing) {
                ret = true;
                /*
                 * TODO lwjgl No backing shown or hidden event,
                 * so we brutally reset it on retrieval
                 * if no longer applicable.
                 */
                this.mustConsiderBackingNotHidden = false;
            } else {
                ret = this.mustConsiderBackingNotHidden;
            }
        } else {
            ret = backingShowing;
        }
        return ret;
    }

    @Override
    protected void showBackingWindow() {
        if (this.isClosed_nonVolatile()) {
            return;
        }
        GLFW.glfwShowWindow(this.window);
    }

    @Override
    protected void hideBackingWindow() {
        if (getBinding().getBindingConfig().getMustGuardAgainstHidingDueToIconification()
                && this.mustConsiderBackingNotHidden) {
            /*
             * Need to do that, else we can't programmatically hide
             * while iconified.
             */
            if (DEBUG) {
                hostLog(this, "backing hiding : mustConsiderBackingNotHidden set to false");
            }
            this.mustConsiderBackingNotHidden = false;
        }
        if (this.isClosed_nonVolatile()) {
            return;
        }
        GLFW.glfwHideWindow(this.window);
    }

    /*
     * Focus.
     */

    @Override
    protected boolean isBackingWindowFocused() {
        if (this.isClosed_nonVolatile()) {
            /*
             * TODO lwjgl No focus lost event is generated on closing,
             * so we need to do this, else CLOSED event firing waits for
             * focus lost forever.
             */
            return false;
        }
        return this.backingWindowFocused;
    }
    
    @Override
    protected void requestBackingWindowFocusGain() {
        if (this.isClosed_nonVolatile()) {
            return;
        }
        
        if (false) {
            // TODO lwjgl Attention, sounds a bit like focus.
            GLFW.glfwRequestWindowAttention(this.window);
        }
        
        GLFW.glfwFocusWindow(this.window);
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
        return this.backingWindowIconified;
    }

    @Override
    protected void setBackingWindowIconifiedOrNot(boolean iconified) {
        if (this.isClosed_nonVolatile()) {
            return;
        }
        
        if (iconified) {
            if (getBinding().getBindingConfig().getMustGuardAgainstHidingDueToIconification()
                    && this.mustConsiderBackingNotHidden) {
                if (DEBUG) {
                    hostLog(this, "setBackingWindowIconified : mustConsiderBackingNotHidden set to false");
                }
                this.mustConsiderBackingNotHidden = false;
            }
            GLFW.glfwIconifyWindow(this.window);
        } else {
            GLFW.glfwRestoreWindow(this.window);
            // In case "restoring" causes demaximization,
            // we count on super class treatments to ensure
            // proper maximized state afterwards.
        }
        
        final boolean oldIconified = this.backingWindowIconified;
        if (iconified != oldIconified) {
            this.backingWindowIconified = iconified;
            if (iconified) {
                if (getBinding().getBindingConfig().getMustGuardAgainstHidingDueToIconification()) {
                    final boolean backingShowing = isBackingWindowShowing();
                    if (DEBUG) {
                        hostLog(this, "backingShowing = " + backingShowing);
                    }
                    if (!backingShowing) {
                        if (DEBUG) {
                            hostLog(this, "backing ICONIFIED while hidden : mustConsiderBackingNotHidden set to true");
                        }
                        this.mustConsiderBackingNotHidden = true;
                    }
                }
                this.onBackingWindowIconified();
            } else {
                this.onBackingWindowDeiconified();
            }
        }
    }

    /*
     * Maximized.
     */

    @Override
    protected boolean doesSetBackingWindowMaximizedOrNotWork() {
        if (OsUtils.isMac()) {
            /*
             * TODO lwjgl On Mac, if not decorated,
             * maximized flag changes but not bounds.
             */
            return this.isDecorated();
        }
        return true;
    }
    
    @Override
    protected boolean isBackingWindowMaximized() {
        return this.backingWindowMaximized;
    }

    @Override
    protected void setBackingWindowMaximizedOrNot(boolean maximized) {
        if (this.isClosed_nonVolatile()) {
            return;
        }
        if (maximized) {
            GLFW.glfwMaximizeWindow(this.window);
        } else {
            GLFW.glfwRestoreWindow(this.window);
        }
        final boolean oldMax = this.backingWindowMaximized;
        if (maximized != oldMax) {
            this.backingWindowMaximized = maximized;
            if (maximized) {
                this.onBackingWindowMaximized();
            } else {
                this.onBackingWindowDemaximized();
            }
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
        
        if (this.synthMmeProcess != null) {
            // Better stop it here, than on client close event
            // which should come after, or to let it stop itself
            // next time it runs.
            this.synthMmeProcess.stop();
        }
        
        this.paintHelper.dispose();
        
        Callbacks.glfwFreeCallbacks(this.window);
        GLFW.glfwDestroyWindow(this.window);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void flushPainting() {
        final boolean glDoubleBuffered =
                binding.getBindingConfig().getGlDoubleBuffered();
        // Not much more we can do.
        this.paintHelper.flushPainting(this.window, glDoubleBuffered);
    }
    
    /*
     * 
     */
    
    private static void setStaticGlfwStuffsForWindowCreation(
            boolean glDoubleBuffered,
            boolean decorated) {
        
        // Optional, the current window hints are already the default.
        GLFW.glfwDefaultWindowHints();
        
        // Ensuring that no old/deprecated feature will be used.
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        if (glDoubleBuffered) {
            GLFW.glfwWindowHint(GLFW.GLFW_DOUBLEBUFFER, GLFW.GLFW_TRUE);
        } else {
            GLFW.glfwWindowHint(GLFW.GLFW_DOUBLEBUFFER, GLFW.GLFW_FALSE);
        }

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
 
        if (decorated) {
            GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
        } else {
            GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        }
    }
}
