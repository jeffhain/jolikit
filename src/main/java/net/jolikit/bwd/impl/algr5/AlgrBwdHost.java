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
package net.jolikit.bwd.impl.algr5;

import java.util.List;

import com.sun.jna.Pointer;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_DISPLAY_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_KEYBOARD_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_KEYBOARD_STATE;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_MOUSE_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_TOUCH_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.AlgrDisplayFlag;
import net.jolikit.bwd.impl.algr5.jlib.AlgrDisplayOption;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.InterfaceHostLifecycleListener;
import net.jolikit.bwd.impl.utils.basics.PixelCoordsConverter;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;
import net.jolikit.lang.Dbg;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.InterfaceScheduler;

public class AlgrBwdHost extends AbstractBwdHost {

    /*
     * TODO algr Content is erased if moving another window
     * on top of this one. Possible to work around?
     */
    
    /*
     * TODO algr About paintings and moves/resizes (on Windows at least):
     * 
     * If resize from native decoration is due to dragging top or left borders,
     * it's possible to paint during the resize, but if it's due
     * to dragging bottom or right borders, the UI thread seem stuck
     * during the resize and it's not possible.
     * After a resize, ALLEGRO_EVENT_DISPLAY_RESIZE is fired,
     * but not during it.
     * Also, during a resize, read spans are not updated,
     * and the "display" (the clipping rectangle to the
     * full size of the backbuffer) is not resized, even if calling
     * al_resize_display(...), so what is paint is distorted
     * until the user releases the drag.
     * 
     * During a move, it's possible to paint (UI thread not stuck),
     * but neither during nor even after a move is any move-type event
     * fired.
     * 
     * During both moves and resizes, read positions are properly updated.
     * 
     * As a result, we can't detect resizes when top-left corner is not moving,
     * and can detect them as moves when top-left corner is moving, but then
     * we can't discriminate whether it's a resize or simply a move.
     * 
     * Our best effort handling of all of this is this:
     * - We use a periodic treatment to detect when top-left corner
     *   moved or stopped moving.
     * - When top-left corner moves, we paint if we must try to paint
     *   during moves (and not just during resizes, since we don't support
     *   that well: it's either not possible, or distorted, and that would
     *   trigger paints during moves too).
     * - When top-left corner stops moving, we paint if we were not already
     *   painting during moves.
     * 
     * Treatment's period must not be too small, for repaints not to waste
     * resources, nor too large else repaints would take place too long after
     * moves.
     * 
     * Note that since we compute move's end just by periodically checking
     * bounds, we will consider that move ended if the mouse/pointer stands
     * still, even though the user is still holding it for dragging.
     * We could upgrade that by taking into account current (last) mouse state,
     * but the complication doesn't seem worth it, as seldom unexpected
     * paintings should be rather a good surprise than a bad one.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean MUST_PRESERVE_OB_CONTENT_ON_RESIZE = true;

    private static final boolean ALLOW_OB_SHRINKING = true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * Because Allegro doesn't have a window moved event.
     */
    private class MyHostMoveDetectionProcess extends AbstractProcess {
        private final long delayNs;
        private GRect lastClientBoundsInOs;
        public MyHostMoveDetectionProcess(
                InterfaceScheduler scheduler,
                double delayS) {
            super(scheduler);
            this.delayNs = sToNsNoUnderflow(delayS);
        }
        @Override
        protected void onBegin() {
            this.lastClientBoundsInOs = GRect.DEFAULT_EMPTY;
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {
            final GRect oldBoundsInOs = this.lastClientBoundsInOs;
            final GRect newBoundsInOs = getClientBoundsInOs();
            
            final long nextNs = plusBounded(actualTimeNs, this.delayNs);
            
            if (!newBoundsInOs.isEmpty()) {
                this.lastClientBoundsInOs = newBoundsInOs;
                
                final boolean didTopLeftMove =
                        (newBoundsInOs.x() != oldBoundsInOs.x())
                        || (newBoundsInOs.y() != oldBoundsInOs.y());

                if (DEBUG) {
                    if (!newBoundsInOs.equals(oldBoundsInOs)) {
                        hostLog(this,
                            "process(...) : oldBoundsInOs = " + oldBoundsInOs
                            + ", newBoundsInOs = " + newBoundsInOs);
                    }
                }
                
                if (didTopLeftMove) {
                    if (isShowingAndDeiconified()) {
                        if (DEBUG) {
                            hostLog(this, "calling onBackingWindowMoved()");
                        }
                        onBackingWindowMoved();
                    }
                }
            }

            // Input time still valid, since we didn't do anything heavy
            // (no synchronous painting).
            return nextNs;
        }
    }

    /**
     * Delayed client resize process.
     * TODO algr Maybe don't need it on Mac, but shouldn't hurt.
     */
    private class MyDcrp extends AbstractProcess {
        private boolean firstCall;
        public MyDcrp(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        @Override
        protected void onBegin() {
            this.firstCall = true;
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {
            if (this.firstCall) {
                this.firstCall = false;
                final long delayNs = sToNsNoUnderflow(
                        getBinding().getBindingConfig().getClientResizeDelayS());
                return plusBounded(actualTimeNs, delayNs);
            }
            
            resizeDisplayAndEnsurePainting(
                    lastMustResizeDisplay,
                    lastDisplayResizeWidth,
                    lastDisplayResizeHeight);
            
            this.stop();
            return 0;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;
    
    private final AlgrEventConverter eventConverter;

    /**
     * Allegro's "display", i.e. the backing window.
     */
    private final Pointer display;
    
    private final Pointer display_event_source;

    private final AlgrBwdCursorManager cursorManager;

    private final IntArrayGraphicBuffer offscreenBuffer;

    private final MyHostMoveDetectionProcess hostMoveDetectionProcess;
    
    private final AlgrHostBoundsHelper hostBoundsHelper;
    
    private final AlgrPaintUtils paintUtils = new AlgrPaintUtils();
    
    /*
     * 
     */
    
    private boolean lastMustResizeDisplay = false;
    private int lastDisplayResizeWidth = 0;
    private int lastDisplayResizeHeight = 0;

    /**
     * Needs to be a different default value than lastDisplayResizeWidth,
     * else won't consider something needs to be done.
     */
    private int lastResizeTargetWidth = Integer.MIN_VALUE;
    private int lastResizeTargetHeight = Integer.MIN_VALUE;

    private final AbstractProcess delayedClientResizeProcess;

    private int lastMouseAxesXInScreen = Integer.MIN_VALUE;
    private int lastMouseAxesYInScreen = Integer.MIN_VALUE;
    
    /*
     * 
     */

    /**
     * TODO algr Because allegro has no such information.
     */
    private boolean backingWindowShowing = false;
    
    /**
     * Only used if not relying on backing iconified,
     * cf. mustRelyOnBackingIconification in config.
     */
    private boolean backingWindowIconified_hack = false;
    
    /**
     * TODO algr "hidIcoHack": Allegro doesn't have a hiding feature,
     * and iconification might not work.
     * To make up for it, we move the window out of screen as best effort
     * hiding or iconification.
     * To know where to move the window back to when back to showing and
     * deiconified, we use super class target showing/deico/demax bounds
     * to enforce for demaximized windows, and this field for maximized windows.
     */
    private GRect windowBoundsInOsToRestoreOnShowingDeicoMax = null;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * TODO algr If owner is not null, the created host is considered a "dialog"
     * in our API (has an owner, is closed when owner is closed, etc.),
     * but there is no way to create a dialog in Allegro so the backing window
     * is just a regular one.
     * 
     * @param owner If not null, creates a dialog host, else a basic host.
     * @param title Must not be null (used in task bar even if not decorated).
     * @param modal Only used if creating a dialog host.
     */
    public AlgrBwdHost(
            AbstractAlgrBwdBinding binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            AlgrBwdHost owner,
            //
            String title,
            boolean decorated,
            boolean modal,
            //
            InterfaceBwdClient client,
            //
            AlgrCursorRepository backingCursorRepository) {
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

        final AlgrBwdBindingConfig bindingConfig = binding.getBindingConfig();
        
        this.offscreenBuffer = new IntArrayGraphicBuffer(
                MUST_PRESERVE_OB_CONTENT_ON_RESIZE,
                ALLOW_OB_SHRINKING);

        // Started on show, and then only stopped on close,
        // for re-showings might not pass through our code.
        this.hostMoveDetectionProcess = new MyHostMoveDetectionProcess(
                getUiThreadScheduler(),
                bindingConfig.getHostBoundsCheckDelayS());

        final PixelCoordsConverter pixelCoordsConverter =
            binding.getPixelCoordsConverter();
        
        final AbstractBwdHost host = this;
        final AlgrHostBoundsHelper hostBoundsHelper = new AlgrHostBoundsHelper(
                host,
                pixelCoordsConverter,
                bindingConfig.getDecorationInsets(),
                bindingConfig.getMustResizeDisplayExplicitlyOnBoundsSetting(),
                bindingConfig.getMustRestoreWindowPositionAfterDisplayResize());
        this.hostBoundsHelper = hostBoundsHelper;

        this.eventConverter = new AlgrEventConverter(
                binding.getEventsConverterCommonState(),
                host,
                pixelCoordsConverter);
        
        /*
         * Creating window.
         */
        
        final GRect defaultClientBoundsInOs =
            bindingConfig.getDefaultClientOrWindowBoundsInOs();
        
        final int initialWidthInOs = defaultClientBoundsInOs.xSpan();
        final int initialHeightInOs = defaultClientBoundsInOs.ySpan();
        final Pointer display;
        {
            setStaticAlgrStuffsForDisplayCreation(
                    title,
                    decorated,
                    initialWidthInOs,
                    initialHeightInOs,
                    //
                    bindingConfig.getHiddenHackClientXInOs(),
                    bindingConfig.getHiddenHackClientYInOs());
            
            display = LIB.al_create_display(initialWidthInOs, initialHeightInOs);
            if (display == null) {
                throw new IllegalStateException(
                    "could not create display: " + LIB.al_get_errno());
            }
            this.display = display;
            
            final Pointer display_event_source =
                LIB.al_get_display_event_source(display);
            if (display_event_source == null) {
                throw new IllegalStateException(
                    "could not get display event source: " + LIB.al_get_errno());
            }
            LIB.al_register_event_source(
                    binding.get_event_queue(),
                    display_event_source);
            this.display_event_source = display_event_source;
        }
        
        /*
         * 
         */
        
        this.delayedClientResizeProcess = new MyDcrp(
                getUiThreadScheduler());

        this.cursorManager = new AlgrBwdCursorManager(
                backingCursorRepository,
                display);

        /*
         * 
         */

        hostLifecycleListener.onHostCreated(this);
    }

    /*
     * 
     */

    @Override
    public AlgrBwdBindingConfig getBindingConfig() {
        return (AlgrBwdBindingConfig) super.getBindingConfig();
    }

    @Override
    public AbstractAlgrBwdBinding getBinding() {
        return (AbstractAlgrBwdBinding) super.getBinding();
    }
    
    /*
     * 
     */

    @Override
    public AlgrBwdCursorManager getCursorManager() {
        return this.cursorManager;
    }

    /*
     * 
     */

    @Override
    public Pointer getBackingWindow() {
        return this.display;
    }

    /*
     * 
     */

    @Override
    public AlgrBwdHost getOwner() {
        return (AlgrBwdHost) super.getOwner();
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
        final AlgrBwdHost owner = this;
        return new AlgrBwdHost(
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

    /*
     * Window events.
     */

    public void onEvent_ALLEGRO_EVENT_DISPLAY_EXPOSE(ALLEGRO_DISPLAY_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_DISPLAY_EXPOSE(...) : (" + backingEvent.x +", " + backingEvent.y + ", " + backingEvent.width + ", " + backingEvent.height + ")");
        }
        
        if (getBinding().getBindingConfig().getMustUseDisplayExposeAsDisplayResize()) {
            final boolean mustResizeDisplay = false;
            onBackingWindowResizishEvent(
                    backingEvent,
                    mustResizeDisplay);
        } else {
            this.makeAllDirtyAndEnsurePendingClientPainting();

            this.onBackingWindowShown();
        }
    }
    
    public void onEvent_ALLEGRO_EVENT_DISPLAY_RESIZE(ALLEGRO_DISPLAY_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_DISPLAY_RESIZE(...) : (" + backingEvent.x +", " + backingEvent.y + ", " + backingEvent.width + ", " + backingEvent.height + ")");
        }
        
        final boolean mustResizeDisplay = true;
        onBackingWindowResizishEvent(
                backingEvent,
                mustResizeDisplay);
    }

    public void onEvent_ALLEGRO_EVENT_DISPLAY_CLOSE(ALLEGRO_DISPLAY_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_DISPLAY_CLOSE(...)");
        }
        onBackingWindowClosing();
    }

    public void onEvent_ALLEGRO_EVENT_DISPLAY_LOST(ALLEGRO_DISPLAY_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_DISPLAY_LOST(...)");
        }
        /*
         * "When using Direct3D, displays can enter a "lost" state.  In that state,
         * drawing calls are ignored, and upon entering the state, bitmap's pixel
         * data can become undefined."
         * 
         * We are helpless.
         */
    }

    public void onEvent_ALLEGRO_EVENT_DISPLAY_FOUND(ALLEGRO_DISPLAY_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_DISPLAY_FOUND(...)");
        }
        // Full repaint in case it could help.
        this.makeAllDirtyAndEnsurePendingClientPainting();
    }

    public void onEvent_ALLEGRO_EVENT_DISPLAY_SWITCH_IN(ALLEGRO_DISPLAY_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_DISPLAY_SWITCH_IN(...)");
        }
        onBackingWindowFocusGained();
        
        this.makeAllDirtyAndEnsurePendingClientPainting();
    }

    public void onEvent_ALLEGRO_EVENT_DISPLAY_SWITCH_OUT(ALLEGRO_DISPLAY_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_DISPLAY_SWITCH_OUT(...)");
        }
        
        /*
         * Passing here when window gets into the background,
         * looses focus, or gets iconified due to native decoration.
         */
        
        /*
         * TODO algr We can pass here multiple times without interleaving
         * SWITCH_IN events, so we add a guard not to call the callback
         * uselessly, even if generic treatments can handle it.
         */
        if (this.isFocused()) {
            this.onBackingWindowFocusLost();
        }
    }

    public void onEvent_ALLEGRO_EVENT_DISPLAY_ORIENTATION(ALLEGRO_DISPLAY_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_DISPLAY_ORIENTATION(...)");
        }
        /*
         * Our library is not supposed to handle that,
         * other than as a non-notified change in screen resolution,
         * but we ensure a full repaint in case it could help.
         */
        this.makeAllDirtyAndEnsurePendingClientPainting();
    }

    public void onEvent_ALLEGRO_EVENT_DISPLAY_HALT_DRAWING(ALLEGRO_DISPLAY_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_DISPLAY_HALT_DRAWING(...)");
        }
        /*
         * TODO algr Maybe we could not bother with this,
         * since as a general rule we don't paint while not showing.
         * 
         * "al_acknowledge_drawing_halt(1)
         * Call this in response to the ALLEGRO_EVENT_DISPLAY_HALT_DRAWING(3alleg5) event.
         * This is currently necessary for Android and iOS as you are not allowed to draw
         * to your display while it is not being shown.
         * If you do not call this function to let the operating system know that you have
         * stopped drawing or if you call it to late the application likely will be considered
         * misbehaving and get terminated."
         */
        LIB.al_acknowledge_drawing_halt(this.display);
    }

    public void onEvent_ALLEGRO_EVENT_DISPLAY_RESUME_DRAWING(ALLEGRO_DISPLAY_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_DISPLAY_RESUME_DRAWING(...)");
        }
        LIB.al_acknowledge_drawing_resume(this.display);
    }

    /*
     * Key events.
     */

    public void onEvent_ALLEGRO_EVENT_KEY_DOWN(ALLEGRO_KEYBOARD_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_KEY_DOWN(...)");
        }
        if (!this.isShowingAndDeiconified()) {
            if (DEBUG) {
                hostLog(this, "assuming not focused");
            }
            return;
        }

        final BwdKeyEventPr event = this.eventConverter.newKeyPressedEvent(backingEvent);
        this.onBackingKeyPressed(event);
    }

    public void onEvent_ALLEGRO_EVENT_KEY_CHAR(ALLEGRO_KEYBOARD_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_KEY_CHAR(...)");
        }
        if (!this.isShowingAndDeiconified()) {
            if (DEBUG) {
                hostLog(this, "assuming not focused");
            }
            return;
        }
        
        if (backingEvent.repeat) {
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
            /*
             * TODO algr When a repetition is going on while focus is gained,
             * most libraries generate a synthetic key pressed event
             * before key typed events repetition.
             * For behavioral homogeneity, we generate a synthetic
             * key pressed event before each key typed events.
             * It will just be ignored by repetition helper if the key
             * has not been released yet.
             */
            final BwdKeyEventPr event = this.eventConverter.newKeyPressedEvent(backingEvent);
            this.onBackingKeyPressed(event);
        }

        final BwdKeyEventT event = this.eventConverter.newKeyTypedEventElseNull(backingEvent);
        if (event != null) {
            this.onBackingKeyTyped(event);
        }
    }

    public void onEvent_ALLEGRO_EVENT_KEY_UP(ALLEGRO_KEYBOARD_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_KEY_UP(...)");
        }
        if (!this.isShowingAndDeiconified()) {
            if (DEBUG) {
                hostLog(this, "assuming not focused");
            }
            return;
        }
        
        final BwdKeyEventPr event = this.eventConverter.newKeyReleasedEvent(backingEvent);
        this.onBackingKeyReleased(event);
    }

    /*
     * Mouse events.
     */

    public void onEvent_ALLEGRO_EVENT_MOUSE_AXES(ALLEGRO_MOUSE_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_MOUSE_AXES(...) : (" + backingEvent.x +", " + backingEvent.y + ", " + backingEvent.dx + ", " + backingEvent.dy + ")");
        }
        if (getBinding().getBindingConfig().getMustSetFreshPosInMouseAxesEvent()) {
            final GRect clientBoundsInOs = getClientBoundsInOs();
            if (!clientBoundsInOs.isEmpty()) {
                // Not modifying the input event, in case some other listener
                // would need the crazy values.
                backingEvent = backingEvent.duplicate();

                final GPoint mousePosInScreenInOs =
                    getBinding().getMousePosInScreenInOs();
                final int oldX = backingEvent.x;
                final int oldY = backingEvent.y;
                final PixelCoordsConverter pixelCoordsConverter =
                    getBinding().getPixelCoordsConverter();
                final int newX =
                    pixelCoordsConverter.computeXInDevicePixel(
                        mousePosInScreenInOs.x() - clientBoundsInOs.x());
                final int newY =
                    pixelCoordsConverter.computeYInDevicePixel(
                        mousePosInScreenInOs.y() - clientBoundsInOs.y());;
                backingEvent.x = newX;
                backingEvent.y = newY;
                backingEvent.dx += (newX - oldX);
                backingEvent.dy += (newY - oldY);
                if (DEBUG_SPAM) {
                    hostLog(this, "clientBoundsInOs = " + clientBoundsInOs);
                    hostLog(this, "mousePosInScreenInOs = " + mousePosInScreenInOs);
                }
                if (DEBUG) {
                    hostLog(this, "reworked backingEvent : (" + backingEvent.x + ", " + backingEvent.y + ", " + backingEvent.dx + ", " + backingEvent.dy + ")");
                }
            }
        }
        
        // NB: Sometimes both true, sometimes both false.
        final boolean hasMouseDelta = (backingEvent.dx != 0) || (backingEvent.dy != 0);
        final boolean hasWheelDelta = (backingEvent.dz != 0);
        
        try {
            if (hasMouseDelta) {
                final BwdMouseEvent event = this.eventConverter.newMouseMovedEvent(backingEvent);
                /*
                 * TODO algr MOUSE_AXES event is generated,
                 * not (just) when mouse moves relatively to the screen,
                 * but (also) whenever it moves relatively to the window,
                 * i.e. when mouse is still but window moves.
                 */
                final int xInScreen = event.xInScreen();
                final int yInScreen = event.yInScreen();
                if ((xInScreen == this.lastMouseAxesXInScreen)
                        && (yInScreen == this.lastMouseAxesYInScreen)) {
                    if (DEBUG) {
                        hostLog(this, "ignoring MOUSE_AXES (dx,dy) (same absolute pos as before)");
                    }
                } else {
                    this.lastMouseAxesXInScreen = xInScreen;
                    this.lastMouseAxesYInScreen = yInScreen;
                    this.onBackingMouseMoved(event);
                }
            }
        } finally {
            if (hasWheelDelta) {
                final BwdWheelEvent event = this.eventConverter.newWheelEventElseNull(backingEvent);
                if (event != null) {
                    this.onBackingWheelEvent(event);
                }
            }
        }
    }

    public void onEvent_ALLEGRO_EVENT_MOUSE_BUTTON_DOWN(ALLEGRO_MOUSE_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_MOUSE_BUTTON_DOWN(...)");
        }
        final BwdMouseEvent event = this.eventConverter.newMousePressedEvent(backingEvent);
        this.onBackingMousePressed(event);
    }

    public void onEvent_ALLEGRO_EVENT_MOUSE_BUTTON_UP(ALLEGRO_MOUSE_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_MOUSE_BUTTON_UP(...)");
        }
        final BwdMouseEvent event = this.eventConverter.newMouseReleasedEvent(backingEvent);
        this.onBackingMouseReleased(event);
    }

    public void onEvent_ALLEGRO_EVENT_MOUSE_ENTER_DISPLAY(ALLEGRO_MOUSE_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_MOUSE_ENTER_DISPLAY(...)");
        }
        final BwdMouseEvent event = this.eventConverter.newMouseEnteredClientEvent(backingEvent);
        this.onBackingMouseEnteredClient(event);
    }

    public void onEvent_ALLEGRO_EVENT_MOUSE_LEAVE_DISPLAY(ALLEGRO_MOUSE_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_MOUSE_LEAVE_DISPLAY(...)");
        }
        final BwdMouseEvent event = this.eventConverter.newMouseExitedClientEvent(backingEvent);
        this.onBackingMouseExitedClient(event);
    }

    public void onEvent_ALLEGRO_EVENT_MOUSE_WARPED(ALLEGRO_MOUSE_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_MOUSE_WARPED(...)");
        }
        // TODO algr Shouldn't hurt to consider warpings (???) as moves (micro-warps).
        final BwdMouseEvent event = this.eventConverter.newMouseMovedEvent(backingEvent);
        this.onBackingMouseMoved(event);
    }

    /*
     * Touch events.
     * 
     * TODO algr See how to make mouse events out of them, if wanting to.
     */

    public void onEvent_ALLEGRO_EVENT_TOUCH_BEGIN(ALLEGRO_TOUCH_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_TOUCH_BEGIN(...)");
        }
    }

    public void onEvent_ALLEGRO_EVENT_TOUCH_END(ALLEGRO_TOUCH_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_TOUCH_END(...)");
        }
    }

    public void onEvent_ALLEGRO_EVENT_TOUCH_MOVE(ALLEGRO_TOUCH_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_TOUCH_MOVE(...)");
        }
    }

    public void onEvent_ALLEGRO_EVENT_TOUCH_CANCEL(ALLEGRO_TOUCH_EVENT backingEvent) {
        if (DEBUG) {
            hostLog(this, "onEvent_ALLEGRO_EVENT_TOUCH_CANCEL(...)");
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void paintClientNowOrLater() {
        this.paintBwdClientNowAndBackingClient(
            getBindingConfig(),
            this.hostBoundsHelper);
    }

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
        
        return new AlgrBwdGraphics(
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
        
        this.paintUtils.paintPixelsOnClient(
            scaleHelper,
            clientSpansInOs,
            bufferPosInCliInOs,
            paintedRectList,
            //
            this.offscreenBuffer,
            this.display,
            this.getBinding().getPixelCoordsConverter(),
            this.getBinding().getInternalParallelizer(),
            this.getBindingConfig().getMustEnsureAccurateImageScaling(),
            this.getBindingConfig().getIssueStream());
        
        this.flushPainting();
    }

    @Override
    protected void setWindowAlphaFp_backing(double windowAlphaFp) {
        // TODO algr No way to set alpha.
    }

    @Override
    protected boolean isBackingWindowShowing() {
        if (this.isClosed_nonVolatile()) {
            /*
             * Need to do that else we postpone closing indefinately
             * waiting for window to be hidden so that we can fire
             * HIDDEN event.
             */
            return false;
        }
        return this.backingWindowShowing;
    }

    @Override
    protected void showBackingWindow() {
        if (this.backingWindowShowing) {
            return;
        }
        
        this.backingWindowShowing = true;

        makeAllDirtyAndEnsurePendingClientPainting();

        this.hostMoveDetectionProcess.start();
        
        if (!this.isBackingWindowIconified()) {
            final boolean maximized = this.isMaximized();
            this.hidIcoHack_restoreBackingBoundsOnShowingDeico(maximized);
        }
    }

    @Override
    protected void hideBackingWindow() {
        if (!this.backingWindowShowing) {
            return;
        }
        
        final GRect windowBoundsInOs = this.getWindowBoundsInOs();
        if (windowBoundsInOs.isEmpty()) {
            // Can happen if iconified.
        } else {
            this.hidIcoHack_setWindowBoundsInOsToRestoreWith(windowBoundsInOs);
        }
        
        this.backingWindowShowing = false;

        if (!windowBoundsInOs.isEmpty()) {
            this.hidIcoHack_moveBackingWindowToHackPosition();
        }
    }

    /*
     * Focus.
     */

    @Override
    protected boolean isBackingWindowFocused() {
        if (!this.isShowingAndDeiconified()) {
            return false;
        }
        final ALLEGRO_KEYBOARD_STATE keyboardState = new ALLEGRO_KEYBOARD_STATE();
        LIB.al_get_keyboard_state(keyboardState);
        final boolean focused = this.display.equals(keyboardState.display);
        return focused;
    }
    
    @Override
    protected void requestBackingWindowFocusGain() {
        // TODO algr No way to request focus.
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
        if (getBinding().getBindingConfig().getMustRelyOnBackingIconification()) {
            final int flags = LIB.al_get_display_flags(this.display);
            return (flags & AlgrDisplayFlag.ALLEGRO_MINIMIZED.intValue()) != 0;
        } else {
            return this.backingWindowIconified_hack;
        }
    }

    @Override
    protected void setBackingWindowIconifiedOrNot(boolean iconified) {
        if (getBinding().getBindingConfig().getMustRelyOnBackingIconification()) {
            LIB.al_set_display_flag(this.display, AlgrDisplayFlag.ALLEGRO_MINIMIZED.intValue(), iconified);
        } else {
            if (!this.backingWindowShowing) {
                // Allowed not to work in this case.
                return;
            }
            
            final boolean oldIconified = this.backingWindowIconified_hack;
            if (iconified == oldIconified) {
                return;
            }
            
            if (iconified) {
                final GRect windowBounds = this.getWindowBounds();
                
                // Set before, to block window moved events
                // on backing bounds setting.
                this.backingWindowIconified_hack = true;
                
                if (windowBounds.isEmpty()) {
                    // Should not happen, but making sure we don't set crazy bounds.
                } else {
                    this.hidIcoHack_setWindowBoundsInOsToRestoreWith(windowBounds);
                    
                    this.hidIcoHack_moveBackingWindowToHackPosition();
                }
            } else {
                try {
                    /*
                     * Not using isMaximized(), to get the actual state
                     * even if we did not already deiconify.
                     */
                    final boolean maximized = this.isBackingWindowMaximized();
                    this.hidIcoHack_restoreBackingBoundsOnShowingDeico(maximized);
                } finally {
                    // Set after, for moved events on backing bounds
                    // setting to be blocked during bounds restoration.
                    this.backingWindowIconified_hack = false;
                }
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
        final int flags = LIB.al_get_display_flags(this.display);
        return (flags & AlgrDisplayFlag.ALLEGRO_MAXIMIZED.intValue()) != 0;
    }

    @Override
    protected void setBackingWindowMaximizedOrNot(boolean maximized) {
        LIB.al_set_display_flag(this.display, AlgrDisplayFlag.ALLEGRO_MAXIMIZED.intValue(), maximized);
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
        this.hostBoundsHelper.setClientBoundsInOs(targetClientBoundsInOs);
    }

    @Override
    protected void setBackingWindowBoundsInOs(GRect targetWindowBoundsInOs) {
        this.hostBoundsHelper.setWindowBoundsInOs(targetWindowBoundsInOs);
    }
    
    /*
     * Closing.
     */
    
    @Override
    protected void closeBackingWindow() {
        LIB.al_unregister_event_source(
                this.getBinding().get_event_queue(),
                this.display_event_source);

        LIB.al_destroy_display(this.display);

        this.hostMoveDetectionProcess.stop();
        
        this.delayedClientResizeProcess.stop();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private boolean isShowingAndDeiconified( ) {
        return this.backingWindowShowing
                && (!this.isBackingWindowIconified());
    }
    
    /*
     * 
     */

    private void hidIcoHack_setWindowBoundsInOsToRestoreWith(
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
            this.windowBoundsInOsToRestoreOnShowingDeicoMax = windowBoundsInOsToRestore;
        } else {
            this.setWindowBoundsInOsToEnforceOnShowDeicoDemaxAndFlag_protected(
                windowBoundsInOsToRestore);
        }
    }
    
    private void hidIcoHack_moveBackingWindowToHackPosition() {
        /*
         * Not using getClientBoundsInOs(), to get the actual bounds
         * even if we did already hide.
         * Might be maximized bounds.
         */
        final GRect windowBounds =
            this.getBackingOrOnDragBeginWindowBoundsInOs();
        
        final GRect hidOrIcoWindowBoundsInOs = windowBounds.withPos(
                getBinding().getBindingConfig().getHiddenHackClientXInOs(),
                getBinding().getBindingConfig().getHiddenHackClientYInOs());
        if (DEBUG) {
            hostLog(
                this,
                "backing window bounds set to hidOrIcoWindowBounds = "
                    + hidOrIcoWindowBoundsInOs);
        }
        this.setBackingWindowBoundsInOs(hidOrIcoWindowBoundsInOs);
    }
    
    private void hidIcoHack_restoreBackingBoundsOnShowingDeico(boolean maximized) {
        if (maximized) {
            final GRect windowBoundsInOsToEnforce = this.windowBoundsInOsToRestoreOnShowingDeicoMax;
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

    /**
     * @param backingEvent DISPLAY_RESIZE or DISPLAY_EXPOSE
     *        (in some cases generated instead of DISPLAY_RESIZE).
     * @param mustResizeDisplay If true, target client bounds are retrieved
     *        from the backing event, and used for actual resize.
     */
    private void onBackingWindowResizishEvent(
            ALLEGRO_DISPLAY_EVENT backingEvent,
            boolean mustResizeDisplay) {
        /*
         * TODO algr DISPLAY_RESIZE actually means window and client area resize.
         * The display size, as retrieved with al_get_display_width(...)
         * and al_get_display_height(...), is not modified.
         * 
         * As a result, we modify it here accordingly.
         * 
         * Also, this allows us to use these methods to retrieve
         * client area size, from which we can compute window size,
         * because Allegro doesn't provide a way to retrieve window size.
         */
        
        final int targetDisplayWidth;
        final int targetDisplayHeight;
        if (mustResizeDisplay) {
            final int widthInDevice = backingEvent.width;
            final int heightInDevice = backingEvent.height;
            final PixelCoordsConverter pixelCoordsConverter = getBinding().getPixelCoordsConverter();
            targetDisplayWidth = pixelCoordsConverter.computeXSpanInOsPixel(widthInDevice);
            targetDisplayHeight = pixelCoordsConverter.computeYSpanInOsPixel(heightInDevice);
            if (DEBUG) {
                hostLog(this, "lastDisplayResizeWidth set to " + targetDisplayWidth);
                hostLog(this, "lastDisplayResizeHeight set to " + targetDisplayHeight);
            }
            this.lastDisplayResizeWidth = targetDisplayWidth;
            this.lastDisplayResizeHeight = targetDisplayHeight;
        } else {
            // Not used.
            targetDisplayWidth = Integer.MIN_VALUE;
            targetDisplayHeight = Integer.MIN_VALUE;
        }
        this.lastMustResizeDisplay = mustResizeDisplay;
        
        if ((targetDisplayWidth == 0)
                || (targetDisplayHeight == 0)) {
            // Must be hiding of some kind.
            // We don't want to make client area totally disappear.
            return;
        }
        
        if (getBinding().getBindingConfig().getMustDelayDisplayResizeOnBackingResizeEvent()) {
            this.delayedClientResizeProcess.start();
        } else {
            resizeDisplayAndEnsurePainting(
                    mustResizeDisplay,
                    targetDisplayWidth,
                    targetDisplayHeight);
        }
    }
    
    private void resizeDisplayAndEnsurePainting(
            boolean mustResizeDisplay,
            int targetWidth,
            int targetHeight) {
        
        if (mustResizeDisplay
                && (targetWidth == this.lastResizeTargetWidth)
                && (targetHeight == this.lastResizeTargetHeight)) {
            if (DEBUG) {
                hostLog(this, "resizeDisplayAndEnsurePainting : same (width,height) = ("
                        + targetWidth + ", " + targetHeight + ") : ignoring");
            }
            return;
        }
        if (mustResizeDisplay) {
            if (DEBUG) {
                Dbg.log("resizeDisplayAndEnsurePainting(...) : (before resize) al_get_display_width = " + LIB.al_get_display_width(this.display));
                Dbg.log("resizeDisplayAndEnsurePainting(...) : (before resize) al_get_display_height = " + LIB.al_get_display_height(this.display));
            }
            if (false) {
                /*
                 * TODO algr Sometimes we want to resize using specific spans,
                 * so we can't always use that, so for consistency we never use it.
                 */
                LIB.al_acknowledge_resize(this.display);
            } else {
                this.hostBoundsHelper.resizeDisplay(targetWidth, targetHeight);
            }
            if (DEBUG) {
                Dbg.log("resizeDisplayAndEnsurePainting(...) : (after resize) al_get_display_width = " + LIB.al_get_display_width(this.display));
                Dbg.log("resizeDisplayAndEnsurePainting(...) : (after resize) al_get_display_height = " + LIB.al_get_display_height(this.display));
            }
        }
        this.lastResizeTargetWidth = targetWidth;
        this.lastResizeTargetHeight = targetHeight;
        
        if (isShowingAndDeiconified()) {
            // Painting ensuring done in callback.
            onBackingWindowResized();
        }
    }
    
    private void flushPainting() {
        LIB.al_flip_display();
    }
    
    /*
     * 
     */
    
    private static void setStaticAlgrStuffsForDisplayCreation(
            String title,
            boolean decorated,
            int initialWidth,
            int initialHeight,
            //
            int hiddenHackClientX,
            int hiddenHackClientY) {
        
        LIB.al_set_new_window_title(title);

        // Out of screen by default, to avoid visible flickering
        // during creation, since Allegro window is always showing.
        LIB.al_set_new_window_position(
                hiddenHackClientX,
                hiddenHackClientY);

        {
            LIB.al_reset_new_display_options();

            {
                /*
                 * TODO algr Using "ALLEGRO_SUGGEST", in case "ALLEGRO_REQUIRE"
                 * would cause exceptions if fulfilling is not possible.
                 * TODO algr Not waiting for vsync, which doesn't seem to hurt
                 * (no particular flickering), and allows us to call al_flip_display()
                 * in UI thread without blocking it for too long (else it would block
                 * for some time, or we would have to cross our fingers and call it
                 * in another thread hoping it wouldn't hurt).
                 */
                final boolean mustWaitForVsync = false;
                final int vsync = (mustWaitForVsync ? 1 : 2);
                LIB.al_set_new_display_option(
                        AlgrDisplayOption.ALLEGRO_VSYNC.intValue(),
                        vsync,
                        AlgrJnaLib.ALLEGRO_SUGGEST);
            }

            {
                int windowFlags = 0;
                
                // Needed to make window programmatically resizable.
                windowFlags |= AlgrDisplayFlag.ALLEGRO_RESIZABLE.intValue();
                
                if (decorated) {
                    windowFlags |= AlgrDisplayFlag.ALLEGRO_WINDOWED.intValue();
                } else {
                    /*
                     * TODO algr On Mac, issues with undecorated hosts:
                     * - Sometimes not showing, even though programmatically
                     *   things look good.
                     * - Can cause crashes, or the error log:
                     *   "2019-03-28 21:32:54.150 java[710:21778] *** Assertion failure in -[ALWindow _changeJustMain],
                     *   /Library/Caches/com.apple.xbs/Sources/AppKit/AppKit-1504.83.101/AppKit.subproj/NSWindow.m:14861"
                     */
                    windowFlags |= AlgrDisplayFlag.ALLEGRO_FRAMELESS.intValue();
                }

                /*
                 * TODO algr Doesn't seem to work (at least on Mac),
                 * but we don't use it anyway.
                 */
                windowFlags |= AlgrDisplayFlag.ALLEGRO_GENERATE_EXPOSE_EVENTS.intValue();

                LIB.al_set_new_display_flags(windowFlags);
            }

            {
                /*
                 * TODO algr What is the unit?
                 * Spec says 0 = dont care (and default).
                 */
                final int refresh_rate = 0;
                LIB.al_set_new_display_refresh_rate(refresh_rate);
            }
        }
    }
}
