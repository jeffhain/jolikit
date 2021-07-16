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
package net.jolikit.bwd.impl.sdl2;

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
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Event;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_KeyboardEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_MouseButtonEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_MouseMotionEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_MouseWheelEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Surface;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_WindowEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SdlEventType;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaUtils;
import net.jolikit.bwd.impl.sdl2.jlib.SdlStatics;
import net.jolikit.bwd.impl.sdl2.jlib.SdlWindowEventID;
import net.jolikit.bwd.impl.sdl2.jlib.SdlWindowFlag;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.InterfaceHostLifecycleListener;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;
import net.jolikit.lang.OsUtils;

public class SdlBwdHost extends AbstractBwdHost {
    
    /*
     * TODO sdl Content is erased if moving another window
     * on top of this one. Possible to work around?
     */

    /*
     * It doesn't seem possible to paint during moves or resizes,
     * such as we can't help but to use the "optimized" behavior of only
     * painting after them.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean MUST_PRESERVE_OB_CONTENT_ON_RESIZE = true;
    
    private static final boolean ALLOW_OB_SHRINKING = true;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;
    
    private final SdlEventConverter eventConverter;

    private final Pointer window;
    
    /**
     * Need to store it here, because after call to SDL_DestroyWindow,
     * SDL_GetWindowID returns 0 for the pointer, and we need it after
     * window has been closed, to clean up maps using it as key.
     */
    private final int windowId;
    
    private final SdlHostBoundsHelper hostBoundsHelper;
    
    private final SdlBwdCursorManager cursorManager;
    
    private final IntArrayGraphicBuffer offscreenBuffer;
    
    /*
     * 
     */
    
    private boolean mustConsiderBackingNotHidden = false;
    
    /**
     * TODO sdl Having this boolean because
     * hasFlag(SDL_WindowFlag.SDL_WINDOW_INPUT_GRABBED)
     * doesn't seem to work.
     */
    private boolean backingWindowFocused = false;
    
    /**
     * TODO sdl Having this boolean because
     * there are no clear SDL iconified/deiconified events.
     */
    private boolean lastBackingWindowIconified = false;
    
    /**
     * TODO sdl Having this boolean because
     * there are no clear SDL maximized/demaximized events.
     */
    private boolean lastBackingWindowMaximized = false;
    
    /*
     * 
     */
    
    private SDL_Surface currentPainting_backingSurf;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param owner If not null, creates a dialog host, else a basic host.
     * @param title Must not be null (used in task bar even if not decorated).
     * @param modal Only used if creating a dialog host.
     */
    public SdlBwdHost(
            AbstractSdlBwdBinding binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            SdlBwdHost owner,
            //
            String title,
            boolean decorated,
            boolean modal,
            //
            InterfaceBwdClient client,
            //
            SdlCursorRepository backingCursorRepository) {
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
        
        final SdlBwdBindingConfig bindingConfig = binding.getBindingConfig();
        
        this.eventConverter = new SdlEventConverter(
                binding.getEventsConverterCommonState(),
                this);
        
        final boolean isDialog = (owner != null);
        
        /*
         * 
         */
        
        int windowFlags = 0;
        windowFlags |= SdlWindowFlag.SDL_WINDOW_HIDDEN.intValue();

        if (decorated) {
            // Need that else no resize borders.
            windowFlags |= SdlWindowFlag.SDL_WINDOW_RESIZABLE.intValue();
        } else {
            // Need that else we have the top bar.
            windowFlags |= SdlWindowFlag.SDL_WINDOW_BORDERLESS.intValue();
        }
        
        if (false) {
            /*
             * "window should be created in high-DPI mode if supported"
             * 
             * TODO sdl I don't know what it's supposed to do,
             * but it doesn't seem to have an effect on my retina Mac,
             * so we just don't use it, in case it would hurt
             * when high-DPI mode is not supported.
             * 
             * Also:
             * "If the window is created with the SDL_WINDOW_ALLOW_HIGHDPI flag, its size in pixels
             * may differ from its size in screen coordinates on platforms with high-DPI support
             * (e.g. iOS and Mac OS X). Use SDL_GetWindowSize() to query the client area's size
             * in screen coordinates, and SDL_GL_GetDrawableSize() or SDL_GetRendererOutputSize()
             * to query the drawable size in pixels."
             * And:
             * "SDL_WINDOW_OPENGL window usable with OpenGL context" (for SDL_GL_GetDrawableSize()?)
             * (if use that, must not use SDL_Renderer(...)).
             */
            windowFlags |= SdlWindowFlag.SDL_WINDOW_ALLOW_HIGHDPI.intValue();
        }
        
        final GRect defaultClientBoundsInOs =
            binding.getBindingConfig().getDefaultClientOrWindowBoundsInOs();
        
        final Pointer window = LIB.SDL_CreateWindow(
                title,
                defaultClientBoundsInOs.x(),
                defaultClientBoundsInOs.y(),
                defaultClientBoundsInOs.xSpan(),
                defaultClientBoundsInOs.ySpan(),
                windowFlags);
        this.window = window;
        
        if (isDialog) {
            final Pointer modal_window = window;
            final Pointer parent_window = owner.window;
            // Might not succeed: just doing best effort.
            LIB.SDL_SetWindowModalFor(modal_window, parent_window);
        }

        final int windowId = LIB.SDL_GetWindowID(window);
        if (windowId == 0) {
            /*
             * This is the value for closed windows,
             * i.e. it's not a valid value.
             * If we got it, we consider we're in trouble.
             */
            throw new BindingError("window id is zero (invalid)");
        }
        this.windowId = windowId;
        
        final AbstractBwdHost host = this;
        this.hostBoundsHelper = new SdlHostBoundsHelper(
                host,
                bindingConfig.getDecorationInsets());
        
        if (decorated) {
            /*
             * TODO sdl On Windows at least, if any of minWidth or minHeight
             * is zero, the non-zero constraint is ignored,
             * so we use a min height of 1, which is fine for us.
             */
            final int minHeight = 1;
            LIB.SDL_SetWindowMinimumSize(
                    window,
                    bindingConfig.getMinClientWidthIfDecorated(),
                    minHeight);
        }

        this.cursorManager = new SdlBwdCursorManager(backingCursorRepository);

        /*
         * 
         */
        
        this.offscreenBuffer = new IntArrayGraphicBuffer(
                MUST_PRESERVE_OB_CONTENT_ON_RESIZE,
                ALLOW_OB_SHRINKING);

        hostLifecycleListener.onHostCreated(this);
    }

    /*
     * 
     */

    @Override
    public SdlBwdBindingConfig getBindingConfig() {
        return (SdlBwdBindingConfig) super.getBindingConfig();
    }

    @Override
    public AbstractSdlBwdBinding getBinding() {
        return (AbstractSdlBwdBinding) super.getBinding();
    }

    /*
     * 
     */

    @Override
    public SdlBwdCursorManager getCursorManager() {
        return this.cursorManager;
    }

    /*
     * 
     */

    @Override
    public Pointer getBackingWindow() {
        return this.window;
    }
    
    public int getWindowId() {
        return this.windowId;
    }
    
    /*
     * 
     */

    @Override
    public SdlBwdHost getOwner() {
        return (SdlBwdHost) super.getOwner();
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
        final SdlBwdHost owner = this;
        return new SdlBwdHost(
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
     * Events.
     */

    /**
     * @param evenUnion Even for this window, either because it's a window event for it,
     *        or a device event while this window has focus (TODO or is "raised" ?).
     */
    public void onEvent(SDL_Event backingEventUnion) {
        final int backingEventTypeInt = backingEventUnion.type();
        final SdlEventType backingEventType = SdlEventType.valueOf(backingEventTypeInt);
        
        switch (backingEventType) {
        /*
         * Life cycle.
         */
        case SDL_QUIT: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : type = " + backingEventUnion.toStringType());
            }
            // Shouldn't hurt.
            onBackingWindowClosing();
        } break;
        /*
         * Window.
         */
        case SDL_WINDOWEVENT: {
            final SDL_WindowEvent backingEvent = SdlJnaUtils.getWindowEvent(backingEventUnion);
            final SdlWindowEventID backingEventId = SdlWindowEventID.valueOf(backingEvent.event);
            if (DEBUG) {
                hostLog(this, "event ID = " + backingEventId);
            }
            
            switch (backingEventId) {
            case SDL_WINDOWEVENT_NONE: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_NONE");
                }
            } break;
            case SDL_WINDOWEVENT_SHOWN: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_SHOWN");
                }
                if (getBinding().getBindingConfig().getMustGuardAgainstHidingDueToIconification()
                        && mustConsiderBackingNotHidden) {
                    if (DEBUG) {
                        hostLog(this, "backing SHOWN : mustConsiderBackingNotHidden set to false");
                    }
                    mustConsiderBackingNotHidden = false;
                }
                onBackingWindowShown();
            } break;
            case SDL_WINDOWEVENT_HIDDEN: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_HIDDEN");
                }
                if (getBinding().getBindingConfig().getMustGuardAgainstHidingDueToIconification()
                        && mustConsiderBackingNotHidden) {
                    if (DEBUG) {
                        hostLog(this, "backing HIDDEN : mustConsiderBackingNotHidden set to false");
                    }
                    mustConsiderBackingNotHidden = false;
                }
                onBackingWindowHidden();
            } break;
            case SDL_WINDOWEVENT_EXPOSED: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_EXPOSED");
                }
                /*
                 * TODO sdl This event is generated many times when the window is grown
                 * (with border drag), but then delivered only when the growth ends,
                 * so ensuring painting here would cause a lot of useless schedules.
                 * Instead, we count on SDL_WINDOWEVENT_FOCUS_GAINED,
                 * which happens once when window appears, for example
                 * when it's de-iconified.
                 * Also, this event happens "by itself" like 3 seconds after
                 * the window (first?) shows up, which makes it a bit nonsensical.
                 */
            } break;
            case SDL_WINDOWEVENT_MOVED: {
                if (DEBUG_SPAM) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_MOVED");
                }
                onBackingWindowMoved();
            } break;
            case SDL_WINDOWEVENT_RESIZED: {
                if (DEBUG_SPAM) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_RESIZED");
                }
                /*
                 * Occurs after window size changed only due to something external
                 * (UI draw, etc.): we ignore that.
                 */
            } break;
            case SDL_WINDOWEVENT_SIZE_CHANGED: {
                if (DEBUG_SPAM) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_SIZE_CHANGED");
                }
                /*
                 * Occurs after window size changed due to anything
                 * (external, or programmatic), once it's completely done.
                 * 
                 * TODO sdl When slowly dragging bottom host border further down,
                 * over like just one or two pixels, a big jump can happen,
                 * down to bottom task bar (not window bottom),
                 * as shown by the following logs:
                 * 555472604531 1 ; event ID = SDL_WINDOWEVENT_SIZE_CHANGED
                 * 555472612808 1 ; SDL_WINDOWEVENT_SIZE_CHANGED : (660,242)
                 * 555480093630 1 ; event ID = SDL_WINDOWEVENT_RESIZED
                 * 555480165475 1 ; event ID = SDL_WINDOWEVENT_EXPOSED
                 * 556120821152 1 ; event ID = SDL_WINDOWEVENT_EXPOSED
                 * 556121058539 1 ; event ID = SDL_WINDOWEVENT_SIZE_CHANGED
                 * 556121079397 1 ; SDL_WINDOWEVENT_SIZE_CHANGED : (660,244)
                 * 556144585633 1 ; event ID = SDL_WINDOWEVENT_RESIZED
                 * 556144832952 1 ; event ID = SDL_WINDOWEVENT_EXPOSED
                 * 556356160297 1 ; event ID = SDL_WINDOWEVENT_SIZE_CHANGED
                 * 556356211946 1 ; SDL_WINDOWEVENT_SIZE_CHANGED : (660,1369)
                 */
                final int newWidth = backingEvent.data1;
                final int newHeight = backingEvent.data2;
                if (DEBUG) {
                    hostLog(this, "SDL_WINDOWEVENT_SIZE_CHANGED : (" + newWidth + "," + newHeight + ")");
                    hostLog(this, "window bounds = " + getWindowBounds());
                    hostLog(this, "client bounds = " + getClientBounds());
                }
                
                onBackingWindowResized();
            } break;
            case SDL_WINDOWEVENT_MINIMIZED: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_MINIMIZED");
                }
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
            } break;
            case SDL_WINDOWEVENT_MAXIMIZED: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_MAXIMIZED");
                }
                onBackingWindowMaximized();
            } break;
            case SDL_WINDOWEVENT_RESTORED: {
                if (DEBUG || true) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_RESTORED");
                }
                /*
                 * Occurs after window got deiconified or demaximized.
                 */
                this.onPossibleBackingWindowDeicoOrDemaxEvent();
            } break;
            case SDL_WINDOWEVENT_ENTER: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_ENTER");
                }
                final BwdMouseEvent event = eventConverter.newMouseEnteredClientEventElseNull(backingEvent);
                if (event != null) {
                    this.onBackingMouseEnteredClient(event);
                }
            } break;
            case SDL_WINDOWEVENT_LEAVE: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_LEAVE");
                }
                final BwdMouseEvent event = eventConverter.newMouseExitedClientEventElseNull(backingEvent);
                if (event != null) {
                    this.onBackingMouseExitedClient(event);
                }
            } break;
            case SDL_WINDOWEVENT_FOCUS_GAINED: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_FOCUS_GAINED");
                }
                backingWindowFocused = true;
                onBackingWindowFocusGained();
            } break;
            case SDL_WINDOWEVENT_FOCUS_LOST: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_FOCUS_LOST");
                }
                backingWindowFocused = false;
                onBackingWindowFocusLost();
            } break;
            case SDL_WINDOWEVENT_CLOSE: {
                if (DEBUG) {
                    hostLog(this, "onEvent(...) : SDL_WINDOWEVENT_CLOSE");
                }
                onBackingWindowClosing();
            } break;
            /*
             * 
             */
            default:
                break;
            }
        } break;
        /*
         * Keyboard.
         */
        case SDL_KEYDOWN: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_KEYDOWN");
            }
            final SDL_KeyboardEvent backingEvent = SdlJnaUtils.getKeyboardEvent(backingEventUnion);
            
            if (backingEvent.repeat != 0) {
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
                this.onBackingKeyPressed(event);
            }
            {
                final BwdKeyEventT event = eventConverter.newKeyTypedEventElseNull(backingEvent);
                if (event != null) {
                    this.onBackingKeyTyped(event);
                }
            }
        } break;
        case SDL_KEYUP: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_KEYUP");
            }
            final SDL_KeyboardEvent backingEvent = SdlJnaUtils.getKeyboardEvent(backingEventUnion);
            {
                final BwdKeyEventPr event = eventConverter.newKeyReleasedEvent(backingEvent);
                this.onBackingKeyReleased(event);
            }
        } break;
        case SDL_TEXTEDITING: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_TEXTEDITING");
            }
        } break;
        case SDL_TEXTINPUT: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_TEXTINPUT");
            }
        } break;
        case SDL_KEYMAPCHANGED: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_KEYMAPCHANGED");
            }
        } break;
        /*
         * Mouse.
         */
        case SDL_MOUSEMOTION: {
            if (DEBUG_SPAM) {
                hostLog(this, "onEvent(...) : SDL_MOUSEMOTION");
            }
            final SDL_MouseMotionEvent backingEvent = SdlJnaUtils.getMouseMotionEvent(backingEventUnion);
            if (OsUtils.isMac()) {
                /*
                 * TODO sdl On Mac, with the +1 hack on set spans,
                 * need to put fresh (x,y) here to avoid fancy drags.
                 */
                final GRect clientBounds = this.getClientBounds();
                if (!clientBounds.isEmpty()) {
                    final GPoint mousePosInScreen = this.getBinding().getMousePosInScreen();
                    backingEvent.x = mousePosInScreen.x() - clientBounds.x();
                    backingEvent.y = mousePosInScreen.y() - clientBounds.y();
                }
            }
            final BwdMouseEvent event = eventConverter.newMouseMovedEventElseNull(backingEvent);
            if (event != null) {
                this.onBackingMouseMoved(event);
            }
        } break;
        case SDL_MOUSEBUTTONDOWN: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_MOUSEBUTTONDOWN");
            }
            final SDL_MouseButtonEvent backingEvent = SdlJnaUtils.getMouseButtonEvent(backingEventUnion);
            final BwdMouseEvent event = eventConverter.newMousePressedEventElseNull(backingEvent);
            if (event != null) {
                this.onBackingMousePressed(event);
            }
        } break;
        case SDL_MOUSEBUTTONUP: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_MOUSEBUTTONUP");
            }
            final SDL_MouseButtonEvent backingEvent = SdlJnaUtils.getMouseButtonEvent(backingEventUnion);
            final BwdMouseEvent event = eventConverter.newMouseReleasedEventElseNull(backingEvent);
            if (event != null) {
                this.onBackingMouseReleased(event);
            }
        } break;
        case SDL_MOUSEWHEEL: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_MOUSEWHEEL");
            }
            final SDL_MouseWheelEvent backingEvent = SdlJnaUtils.getMouseWheelEvent(backingEventUnion);
            final BwdWheelEvent event = eventConverter.newWheelEventElseNull(backingEvent);
            if (event != null) {
                this.onBackingWheelEvent(event);
            }
        } break;
        /*
         * Touch.
         */
        case SDL_FINGERDOWN: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_FINGERDOWN");
            }
        } break;
        case SDL_FINGERUP: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_FINGERUP");
            }
        } break;
        case SDL_FINGERMOTION: {
            if (DEBUG_SPAM) {
                hostLog(this, "onEvent(...) : SDL_FINGERMOTION");
            }
        } break;
        /*
         * Gesture.
         */
        case SDL_DOLLARGESTURE: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_DOLLARGESTURE");
            }
        } break;
        case SDL_DOLLARRECORD: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_DOLLARRECORD");
            }
        } break;
        case SDL_MULTIGESTURE: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_MULTIGESTURE");
            }
        } break;
        /*
         * Clipboard.
         */
        case SDL_CLIPBOARDUPDATE: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_CLIPBOARDUPDATE");
            }
        } break;
        /*
         * Drag and drop.
         */
        case SDL_DROPFILE: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_DROPFILE");
            }
        } break;
        /*
         * Render.
         */
        case SDL_RENDER_TARGETS_RESET: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_RENDER_TARGETS_RESET");
            }
            /*
             * "sent when D3D9 render targets are reset after the device has been restored."
             */
        } break;
        case SDL_RENDER_DEVICE_RESET: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_RENDER_DEVICE_RESET");
            }
        } break;
        /*
         * User.
         */
        case SDL_USEREVENT: {
            if (DEBUG) {
                hostLog(this, "onEvent(...) : SDL_USEREVENT");
            }
        } break;
        /*
         * 
         */
        default: {
            // Ignoring.
        } break;
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void paintClientNowOrLater() {

        final Pointer window = this.getBackingWindow();

        final Pointer windowSurfPtr = LIB.SDL_GetWindowSurface(window);
        final SDL_Surface windowSurf = SdlJnaUtils.newAndRead(SDL_Surface.ByValue.class, windowSurfPtr);

        /*
         * Can directly flush our int[] into window surface,
         * and we also don't need to use an offset as it actually
         * corresponds to client area (at least when scale is 1).
         */
        this.paintClientNowOnSurface(windowSurf);
    }

    @Override
    protected void setWindowAlphaFp_backing(double windowAlphaFp) {
        if (this.isClosed_nonVolatile()) {
            return;
        }
        
        final float opacity = (float) windowAlphaFp;
        // Might not succeed: just doing best effort.
        LIB.SDL_SetWindowOpacity(this.window, opacity);
    }
    
    /*
     * Showing.
     */
    
    @Override
    protected boolean isBackingWindowShowing() {
        final boolean backingShowing = this.hasFlag(SdlWindowFlag.SDL_WINDOW_SHOWN);
        if (getBinding().getBindingConfig().getMustGuardAgainstHidingDueToIconification()) {
            if ((!backingShowing)
                    && this.mustConsiderBackingNotHidden
                    && (!this.isClosed_nonVolatile())) {
                return true;
            }
        }
        return backingShowing;
    }

    @Override
    protected void showBackingWindow() {
        if (this.isClosed_nonVolatile()) {
            return;
        }
        LIB.SDL_ShowWindow(this.window);
    }

    @Override
    protected void hideBackingWindow() {
        if (this.isClosed_nonVolatile()) {
            return;
        }
        
        if (getBinding().getBindingConfig().getMustGuardAgainstHidingDueToIconification()
                && mustConsiderBackingNotHidden) {
            /*
             * Need to do that, else we can't programmatically hide
             * while iconified.
             */
            if (DEBUG) {
                hostLog(this, "backing hiding : mustConsiderBackingNotHidden set to false");
            }
            mustConsiderBackingNotHidden = false;
        }
        LIB.SDL_HideWindow(this.window);
    }

    /*
     * Focus.
     */

    @Override
    protected boolean isBackingWindowFocused() {
        if (!this.isShowing()) {
            /*
             * TODO sdl Backing focused state can stay true
             * for hidden windows.
             * 
             * TODO sdl No focus lost event is generated on closing,
             * so we need to return false when closed
             * (in which case we pass here as well),
             * else CLOSED event firing waits for focus lost forever.
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
        
        LIB.SDL_RaiseWindow(this.window);
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
        return this.hasFlag(SdlWindowFlag.SDL_WINDOW_MINIMIZED);
    }
    
    @Override
    protected void setBackingWindowIconifiedOrNot(boolean iconified) {
        if (this.isClosed_nonVolatile()) {
            return;
        }
        
        if (iconified) {
            LIB.SDL_MinimizeWindow(this.window);
        } else {
            LIB.SDL_RestoreWindow(this.window);
            /*
             * In case "restoring" causes demaximization,
             * we count on super class treatments to ensure
             * proper maximized state afterwards.
             */
        }
    }

    /*
     * Maximized.
     */

    @Override
    protected boolean doesSetBackingWindowMaximizedOrNotWork() {
        if (OsUtils.isMac()
                && (!this.isDecorated())) {
            /*
             * TODO sdl On Mac, maximization doesn't work if window
             * is undecorated: the top grows up by what appears to be
             * the height of a decoration top, and then the window
             * is stuck in this state and bounds.
             */
            return false;
        }
        return true;
    }
    
    @Override
    protected boolean isBackingWindowMaximized() {
        if (!this.doesSetBackingWindowMaximizedOrNotWork()) {
            /*
             * TODO sdl On Mac, for some reason, undecorated windows
             * can be indicated maximized, from creation or showing time,
             * while they are not.
             */
            return false;
        }
        return this.hasFlag(SdlWindowFlag.SDL_WINDOW_MAXIMIZED);
    }

    @Override
    protected void setBackingWindowMaximizedOrNot(boolean maximized) {
        if (this.isClosed_nonVolatile()) {
            return;
        }
        if (maximized) {
            LIB.SDL_MaximizeWindow(this.window);
        } else {
            LIB.SDL_RestoreWindow(this.window);
        }
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
        LIB.SDL_DestroyWindow(this.window);
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

        return new SdlBwdGraphics(
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
        
        final SDL_Surface surface = this.currentPainting_backingSurf;
        
        final int[] pixelArr =
            this.offscreenBuffer.getPixelArr();
        final int pixelArrScanlineStride =
            this.offscreenBuffer.getScanlineStride();

        /*
         * TODO sdl Seems to always be false, but since SDL docs
         * don't tell in which cases a surface requires locking,
         * we don't know, so we handle it just in case.
         */
        final boolean mustLock = SdlStatics.SDL_MUSTLOCK(surface);
        if (mustLock) {
            final int ret = LIB.SDL_LockSurface(surface);
            if (ret < 0) {
                throw new BindingError("could not lock the surface: " + LIB.SDL_GetError());
            }
        }
        try {
            for (GRect paintedRect : paintedRectList) {
                SdlUtils.copyPixels(
                    scaleHelper,
                    bufferPosInCliInOs,
                    //
                    pixelArr,
                    pixelArrScanlineStride,
                    paintedRect,
                    //
                    surface);
            }
        } finally {
            if (mustLock) {
                LIB.SDL_UnlockSurface(surface);
            }
        }
        
        this.flushPainting();
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void onPossibleBackingWindowDeicoOrDemaxEvent() {
        
        /*
         * Updating internal states.
         */
        
        final boolean oldIco = this.lastBackingWindowIconified;
        final boolean newIco;
        if (this.isBackingWindowShowing()) {
            newIco = this.isBackingWindowIconified();
        } else {
            newIco = oldIco;
        }
        if (newIco != oldIco) {
            this.lastBackingWindowIconified = newIco;
        }
        
        final boolean oldMax = this.lastBackingWindowMaximized;
        final boolean newMax;
        if (this.isBackingWindowShowing()) {
            if (this.isBackingWindowIconified()) {
                newMax = oldMax;
            } else {
                newMax = this.isBackingWindowMaximized();
            }
        } else {
            newMax = oldMax;
        }
        if (newMax != oldMax) {
            this.lastBackingWindowMaximized = newMax;
        }
        
        /*
         * Calling proper hook.
         */
        
        if ((newIco != oldIco)
                && newIco) {
            // ico + max/demax/nothing.
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
            if ((newIco != oldIco)
                    || (newMax != oldMax)) {
                // deico/max/demax.
                this.onBackingWindowAnyEvent();
            }
        }
    }

    /*
     * 
     */
    
    private void paintClientNowOnSurface(SDL_Surface surface) {
        this.currentPainting_backingSurf = surface;
        this.paintBwdClientNowAndBackingClient(
            getBinding().getBindingConfig(),
            this.hostBoundsHelper);
    }
    
    private void flushPainting() {
        if (this.isClosed_nonVolatile()) {
            return;
        }
        
        final int ret = LIB.SDL_UpdateWindowSurface(this.window); 
        if (ret != 0) {
            /*
             * Can pass here when having the SDL error:
             * "Window surface is invalid, please call SDL_GetWindowSurface() to get a new surface"
             * We do as told, and retry.
             */
            final Pointer windowSurfPtr2 = LIB.SDL_GetWindowSurface(this.window);
            if (windowSurfPtr2 == null) {
                throw new BindingError("could not get a new window surface : " + LIB.SDL_GetError());
            }
            final int ret2 = LIB.SDL_UpdateWindowSurface(this.window);
            if (ret2 != 0) {
                throw new BindingError("could not update window surface : " + LIB.SDL_GetError());
            }
        }
    }

    /*
     * 
     */

    private boolean hasFlag(SdlWindowFlag flag) {
        if (this.isClosed_nonVolatile()) {
            return false;
        }
        
        final int flags = LIB.SDL_GetWindowFlags(this.window);
        return (flags & flag.intValue()) != 0;
    }
}
