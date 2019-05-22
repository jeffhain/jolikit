/*
 * Copyright 2019 Jeff Hain
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

import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_DISPLAY_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_KEYBOARD_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_MOUSE_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_MOUSE_STATE;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_TOUCH_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.AlgrEventKind;
import net.jolikit.bwd.impl.algr5.jlib.AlgrEventType;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaUtils;
import net.jolikit.bwd.impl.algr5.jlib.InterfaceAlgrAtExit;
import net.jolikit.bwd.impl.algr5.jlib.InterfaceAlgrTraceHandler;
import net.jolikit.bwd.impl.algr5.jlib.InterfaceAlgrUserMain;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLibFont;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLibImage;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLibTtf;
import net.jolikit.bwd.impl.utils.ConfiguredExceptionHandler;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.OsUtils;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

import com.sun.jna.Pointer;

/**
 * Binding based on Allegro V5 (tested with V5.2).
 * 
 * Can only have a single instance of this binding,
 * for Allegro doesn't allow for multiple contexts.
 */
public class AlgrBwdBinding extends AbstractAlgrBwdBinding {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SPAM = DEBUG && false;

    /**
     * TODO algr Sometimes, at least on Windows when resizing display,
     * mouseState.display is null, so the coordinates are meaningless
     * (and in practice they seem to stay the same).
     * Also, it's a bit weird to have mouse state depend on a window,
     * and not be some decoupled global state.
     * But we have some workarounds for eventual weird cases,
     * so we still use it.
     */
    private static final boolean MUST_USE_AL_GET_MOUSE_STATE = true;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private class MyAlgrEl implements InterfaceAlgrEventListener {
        @Override
        public void onEvent(ALLEGRO_EVENT eventUnion) {
            final AlgrEventType eventType = AlgrJnaUtils.getEventType(eventUnion);
            final AlgrEventKind eventKind = eventType.kind();
            if (eventType == AlgrEventType.ALLEGRO_EVENT_MOUSE_AXES) {
                if (DEBUG_SPAM) {
                    Dbg.log("binding algrEventListener.onEvent(type = " + eventType + ", source = " + eventUnion.any.source + ")");
                }
            } else {
                if (DEBUG) {
                    Dbg.log("binding algrEventListener.onEvent(type = " + eventType + ", source = " + eventUnion.any.source + ")");
                }
            }

            switch (eventKind) {
            case ALGR_WINDOW_EVENT_KIND: {
                final ALLEGRO_DISPLAY_EVENT backingEvent = AlgrJnaUtils.get_ALLEGRO_DISPLAY_EVENT(eventUnion);
                // For these events, source is the display (window) itself.
                final Pointer display = backingEvent.source;
                final AlgrBwdHost host = getHostForDisplay(display);
                if (host != null) {
                    this.forwardTohost_ALLEGRO_DISPLAY_EVENT(eventType, backingEvent, host);
                }
            } break;
            case ALGR_KEY_EVENT_KIND: {
                final ALLEGRO_KEYBOARD_EVENT backingEvent = AlgrJnaUtils.get_ALLEGRO_KEYBOARD_EVENT(eventUnion);
                /*
                 * NB: If ever wanting to ignore events that have "repeat" true here,
                 * need to take care not to do it if they are KEY_UP events,
                 * else repetition could go on forever.
                 */
                final AlgrBwdHost host = getHostForDisplay(backingEvent.display);
                if (host != null) {
                    this.forwardTohost_ALLEGRO_KEYBOARD_EVENT(eventType, backingEvent, host);
                }
            } break;
            case ALGR_MOUSE_EVENT_KIND: {
                final ALLEGRO_MOUSE_EVENT backingEvent = AlgrJnaUtils.get_ALLEGRO_MOUSE_EVENT(eventUnion);
                final AlgrBwdHost host = getHostForDisplay(backingEvent.display);
                if (host != null) {
                    this.forwardTohost_ALLEGRO_MOUSE_EVENT(eventType, backingEvent, host);
                }
            } break;
            case ALGR_TOUCH_EVENT_KIND: {
                final ALLEGRO_TOUCH_EVENT backingEvent = AlgrJnaUtils.get_ALLEGRO_TOUCH_EVENT(eventUnion);
                final AlgrBwdHost host = getHostForDisplay(backingEvent.display);
                if (host != null) {
                    this.forwardTohost_ALLEGRO_TOUCH_EVENT(eventType, backingEvent, host);
                }
            } break;
            case ALGR_JOYSTICK_EVENT_KIND: {
                // Ignoring.
            } break;
            case ALGR_TIMER_EVENT_KIND: {
                // Ignoring.
            } break;
            case ALGR_PHYSICAL_DISPLAY_EVENT_KIND: {
                // Ignoring.
            } break;
            case ALGR_USER_EVENT_KIND: {
                // Ignoring.
            } break;
            default:
                break;
            }
        }
        /**
         * @param display Display info from an event.
         * @return The corresponding host, or null if it has been closed.
         */
        private AlgrBwdHost getHostForDisplay(Pointer display) {
            return (AlgrBwdHost) hostByWindow_unmodView().get(display);
        }
        private void forwardTohost_ALLEGRO_DISPLAY_EVENT(
                AlgrEventType eventType,
                ALLEGRO_DISPLAY_EVENT backingEvent,
                AlgrBwdHost host) {
            switch (eventType) {
            case ALLEGRO_EVENT_DISPLAY_EXPOSE: host.onEvent_ALLEGRO_EVENT_DISPLAY_EXPOSE(backingEvent); break;
            case ALLEGRO_EVENT_DISPLAY_RESIZE: host.onEvent_ALLEGRO_EVENT_DISPLAY_RESIZE(backingEvent); break;
            case ALLEGRO_EVENT_DISPLAY_CLOSE: host.onEvent_ALLEGRO_EVENT_DISPLAY_CLOSE(backingEvent); break;
            case ALLEGRO_EVENT_DISPLAY_LOST: host.onEvent_ALLEGRO_EVENT_DISPLAY_LOST(backingEvent); break;
            case ALLEGRO_EVENT_DISPLAY_FOUND: host.onEvent_ALLEGRO_EVENT_DISPLAY_FOUND(backingEvent); break;
            case ALLEGRO_EVENT_DISPLAY_SWITCH_IN: host.onEvent_ALLEGRO_EVENT_DISPLAY_SWITCH_IN(backingEvent); break;
            case ALLEGRO_EVENT_DISPLAY_SWITCH_OUT: host.onEvent_ALLEGRO_EVENT_DISPLAY_SWITCH_OUT(backingEvent); break;
            case ALLEGRO_EVENT_DISPLAY_ORIENTATION: host.onEvent_ALLEGRO_EVENT_DISPLAY_ORIENTATION(backingEvent); break;
            case ALLEGRO_EVENT_DISPLAY_HALT_DRAWING: host.onEvent_ALLEGRO_EVENT_DISPLAY_HALT_DRAWING(backingEvent); break;
            case ALLEGRO_EVENT_DISPLAY_RESUME_DRAWING: host.onEvent_ALLEGRO_EVENT_DISPLAY_RESUME_DRAWING(backingEvent); break;
            default:
                break;
            }
        }
        private void forwardTohost_ALLEGRO_KEYBOARD_EVENT(
                AlgrEventType eventType,
                ALLEGRO_KEYBOARD_EVENT backingEvent,
                AlgrBwdHost host) {
            switch (eventType) {
            case ALLEGRO_EVENT_KEY_DOWN: host.onEvent_ALLEGRO_EVENT_KEY_DOWN(backingEvent); break;
            case ALLEGRO_EVENT_KEY_CHAR: host.onEvent_ALLEGRO_EVENT_KEY_CHAR(backingEvent); break;
            case ALLEGRO_EVENT_KEY_UP: host.onEvent_ALLEGRO_EVENT_KEY_UP(backingEvent); break;
            default:
                break;
            }
        }
        private void forwardTohost_ALLEGRO_MOUSE_EVENT(
                AlgrEventType eventType,
                ALLEGRO_MOUSE_EVENT backingEvent,
                AlgrBwdHost host) {
            switch (eventType) {
            case ALLEGRO_EVENT_MOUSE_AXES: host.onEvent_ALLEGRO_EVENT_MOUSE_AXES(backingEvent); break;
            case ALLEGRO_EVENT_MOUSE_BUTTON_DOWN: host.onEvent_ALLEGRO_EVENT_MOUSE_BUTTON_DOWN(backingEvent); break;
            case ALLEGRO_EVENT_MOUSE_BUTTON_UP: host.onEvent_ALLEGRO_EVENT_MOUSE_BUTTON_UP(backingEvent); break;
            case ALLEGRO_EVENT_MOUSE_ENTER_DISPLAY: host.onEvent_ALLEGRO_EVENT_MOUSE_ENTER_DISPLAY(backingEvent); break;
            case ALLEGRO_EVENT_MOUSE_LEAVE_DISPLAY: host.onEvent_ALLEGRO_EVENT_MOUSE_LEAVE_DISPLAY(backingEvent); break;
            case ALLEGRO_EVENT_MOUSE_WARPED: host.onEvent_ALLEGRO_EVENT_MOUSE_WARPED(backingEvent); break;
            default:
                break;
            }
        }
        private void forwardTohost_ALLEGRO_TOUCH_EVENT(
                AlgrEventType eventType,
                ALLEGRO_TOUCH_EVENT backingEvent,
                AlgrBwdHost host) {
            switch (eventType) {
            case ALLEGRO_EVENT_TOUCH_BEGIN: host.onEvent_ALLEGRO_EVENT_TOUCH_BEGIN(backingEvent); break;
            case ALLEGRO_EVENT_TOUCH_END: host.onEvent_ALLEGRO_EVENT_TOUCH_END(backingEvent); break;
            case ALLEGRO_EVENT_TOUCH_MOVE: host.onEvent_ALLEGRO_EVENT_TOUCH_MOVE(backingEvent); break;
            case ALLEGRO_EVENT_TOUCH_CANCEL: host.onEvent_ALLEGRO_EVENT_TOUCH_CANCEL(backingEvent); break;
            default:
                break;
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;
    private static final AlgrJnaLibImage LIB_IMAGE = AlgrJnaLibImage.INSTANCE;
    private static final AlgrJnaLibFont LIB_FONT = AlgrJnaLibFont.INSTANCE;
    private static final AlgrJnaLibTtf LIB_TTF = AlgrJnaLibTtf.INSTANCE;

    /*
     * 
     */

    private final AlgrBwdFontHome fontHome;

    private final AlgrCursorRepository backingCursorRepository;

    private final AlgrUiThreadScheduler uiThreadScheduler;

    private Pointer event_queue;

    private Pointer registered_keyboard_event_source;
    private Pointer registered_mouse_event_source;

    /*
     * Taking care to keep a reference to it, else the JVM might destroy it,
     * and backing library could no longer use it.
     */
    private final InterfaceAlgrTraceHandler traceHandler = new InterfaceAlgrTraceHandler() {
        @Override
        public void handle(String trace) {
            final PrintStream stream = getBindingConfig().getIssueStream();
            stream.println(this.getClass().getSimpleName() + ".handle(" + trace + ")");
        }
    };

    private AlgrBwdHost lastUsedHostForCursorPosInScreen = null;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * UI thread is set to be the constructing thread.
     * 
     * @param bindingConfig Must not be null.
     */
    public AlgrBwdBinding(AlgrBwdBindingConfig bindingConfig) {
        super(bindingConfig);

        if (DEBUG) {
            Dbg.log("init()...");
            Dbg.flush();
        }

        final String[] argv = new String[0];
        final int argc = argv.length;

        final InterfaceAlgrUserMain userMain = new InterfaceAlgrUserMain() {
            @Override
            public int main(int argc, String[] argv) {
                final InterfaceAlgrAtExit atExit = new InterfaceAlgrAtExit() {
                    @Override
                    public void atExit() {
                        if (DEBUG) {
                            Dbg.log("atExit()");
                            Dbg.flush();
                        }
                    }
                };
                if (DEBUG) {
                    Dbg.log("al_init(...)...");
                    Dbg.flush();
                }
                if (!al_init(atExit)) {
                    if (DEBUG) {
                        Dbg.log("...al_init(...)");
                        Dbg.flush();
                    }
                    return -1;
                }
                if (DEBUG) {
                    Dbg.log("...al_init(...)");
                    Dbg.flush();
                }

                // Image addon is also used for text drawing.
                if (!LIB_IMAGE.al_init_image_addon()) {
                    return -1;
                }

                // Must init font addon before ttf addon,
                // else inits don't complain but font loading
                // doesn't work.
                if (!LIB_FONT.al_init_font_addon()) {
                    return -1;
                }

                if (!LIB_TTF.al_init_ttf_addon()) {
                    return -1;
                }

                return 0;
            }
        };

        if (OsUtils.isMac()) {
            /*
             * TODO algr On Mac, if using al_run_main(...),
             * and NOT the -XstartOnFirstThread option,
             * we get the following error when calling al_run_main(...):
2017-09-23 01:00:24.669 java[883:60019] WARNING: nextEventMatchingMask should only be called from the Main Thread! This will throw an exception in the future.
2017-09-23 01:00:24.670 java[883:60019] *** Assertion failure in +[NSUndoManager _endTopLevelGroupings], /Library/Caches/com.apple.xbs/Sources/Foundation/Foundation-1349.91/Misc.subproj/NSUndoManager.m:363
2017-09-23 01:00:24.674 java[883:60019] *** Assertion failure in +[NSUndoManager _endTopLevelGroupings], /Library/Caches/com.apple.xbs/Sources/Foundation/Foundation-1349.91/Misc.subproj/NSUndoManager.m:363
2017-09-23 01:00:24.675 java[883:60019] *** Terminating app due to uncaught exception 'NSInternalInconsistencyException', reason: '+[NSUndoManager(NSInternal) _endTopLevelGroupings] is only safe to invoke on the main thread.'
             *** First throw call stack:
(
    0   CoreFoundation                      0x00007fff8078a2cb __exceptionPreprocess + 171
    1   libobjc.A.dylib                     0x00007fff9559a48d objc_exception_throw + 48
    2   CoreFoundation                      0x00007fff8078f042 +[NSException raise:format:arguments:] + 98
    3   Foundation                          0x00007fff821d7be0 -[NSAssertionHandler handleFailureInMethod:object:file:lineNumber:description:] + 195
    4   Foundation                          0x00007fff82162093 +[NSUndoManager(NSPrivate) _endTopLevelGroupings] + 170
    5   AppKit                              0x00007fff7e1ee4ed -[NSApplication run] + 1200
    6   liballegro.5.2.2.dylib              0x0000000124516b41 _al_osx_run_main + 1296
    7   jna2308682812658730321.tmp          0x00000001244b1f14 ffi_call_unix64 + 76
    8   ???                                 0x000070000fc04478 0x0 + 123145566569592
)
libc++abi.dylib: terminating with uncaught exception of type NSException
             * 
             * and using the -XstartOnFirstThread option replaces the error
             * with having the thread calling al_run_main(...)
             * just die in it (no throwing, no return).
             * 
             * By calling Allegro's "UserMain" directly instead of al_run_main(...),
             * with the -XstartOnFirstThread option, everything seem to execute normally
             * except that no window appears on screen.
             * 
             * Thank God, by calling Allegro's "UserMain" directly,
             * and without the -XstartOnFirstThread option, things just work
             * (well, except text drawing).
             */
            if (DEBUG) {
                Dbg.log("userMain.main(...)...");
                Dbg.flush();
            }
            userMain.main(argc, argv);
            if (DEBUG) {
                Dbg.log("...userMain.main(...)");
                Dbg.flush();
            }
        } else {
            if (DEBUG) {
                Dbg.log("LIB.al_run_main(...)...");
                Dbg.flush();
            }
            final int ret;
            try {
                ret = LIB.al_run_main(argc, argv, userMain);
            } catch (Throwable e) {
                if (DEBUG) {
                    Dbg.log("error in al_run_main",e);
                    Dbg.flush();
                }
                throw new BindingError("could not initialize allegro", e);
            } finally {
                if (DEBUG) {
                    Dbg.log("...LIB.al_run_main(...)");
                    Dbg.flush();
                }
            }
            if (DEBUG) {
                Dbg.log("ret = " + ret);
                Dbg.flush();
            }
            if (ret != 0) {
                throw new BindingError("could not initialize allegro");
            }
        }

        /*
         * 
         */

        if (DEBUG) {
            Dbg.log("LIB.al_register_trace_handler()...");
            Dbg.flush();
        }
        LIB.al_register_trace_handler(traceHandler);

        /*
         * Event queue and devices.
         */

        if (DEBUG) {
            Dbg.log("LIB.al_create_event_queue()...");
            Dbg.flush();
        }
        {
            final Pointer event_queue = LIB.al_create_event_queue();
            if (event_queue == null) {
                throw new BindingError("could not create event queue");
            }
            this.event_queue = event_queue;
        }

        if (DEBUG) {
            Dbg.log("LIB.al_install_keyboard()...");
            Dbg.flush();
        }
        {
            final boolean didInstall = LIB.al_install_keyboard();
            if (!didInstall) {
                throw new BindingError("could not install keyboard");
            }
            //
            final Pointer event_source = LIB.al_get_keyboard_event_source();
            LIB.al_register_event_source(this.event_queue, event_source);
            this.registered_keyboard_event_source = event_source;
        }

        if (DEBUG) {
            Dbg.log("LIB.al_install_mouse()...");
            Dbg.flush();
        }
        {
            final boolean didInstall = LIB.al_install_mouse();
            if (!didInstall) {
                throw new BindingError("could not install mouse");
            }
            //
            final Pointer event_source = LIB.al_get_mouse_event_source();
            LIB.al_register_event_source(this.event_queue, event_source);
            this.registered_mouse_event_source = event_source;
        }

        /*
         * 
         */

        final UncaughtExceptionHandler exceptionHandler = new ConfiguredExceptionHandler(bindingConfig);
        this.uiThreadScheduler = new AlgrUiThreadScheduler(
                bindingConfig.getUiThreadSchedulerHardClockTimeType(),
                exceptionHandler,
                this.event_queue,
                bindingConfig.getEventPollPeriodS(),
                new MyAlgrEl());

        /*
         * 
         */

        this.fontHome = new AlgrBwdFontHome(bindingConfig);

        this.backingCursorRepository = new AlgrCursorRepository(
                bindingConfig.getMustUseSystemCursorsWhenAvailable());
        this.backingCursorRepository.init();

        /*
         * 
         */

        this.terminateConstruction();

        /*
         * 
         */

        if (DEBUG) {
            Dbg.log("...init()");
            Dbg.flush();
        }
    }

    @Override
    public AlgrBwdBindingConfig getBindingConfig() {
        return (AlgrBwdBindingConfig) super.getBindingConfig();
    }

    /**
     * Must be called in UI thread.
     * 
     * @throws ConcurrentModificationException if current thread is not UI thread.
     * @throws IllegalStateException if has already been called.
     */
    public void processUntilShutdownUninterruptibly() {
        this.uiThreadScheduler.processUntilShutdownUninterruptibly();
    }

    /**
     * @throws IllegalStateException if event queue doesn't exist (yet or still).
     */
    @Override
    public Pointer get_event_queue() {
        final Pointer event_queue = this.event_queue;
        if (event_queue == null) {
            throw new IllegalStateException("no event queue");
        }
        return event_queue;
    }

    /*
     * Hosts.
     */

    @Override
    public InterfaceBwdHost newHost(
            String title,
            boolean decorated,
            //
            InterfaceBwdClient client) {
        final AbstractAlgrBwdBinding binding = this;
        final AlgrBwdHost owner = null;
        final boolean modal = false;
        return new AlgrBwdHost(
                binding,
                this.getHostLifecycleListener(),
                owner,
                //
                title,
                decorated,
                modal,
                //
                client,
                //
                this.backingCursorRepository);
    }

    /*
     * Threading.
     */

    @Override
    public InterfaceWorkerAwareScheduler getUiThreadScheduler() {
        return this.uiThreadScheduler;
    }

    @Override
    public boolean isParallelPaintingSupported() {
        /*
         * TODO algr False because drawText(...) might block when called concurrently,
         * completing only while subsequently killing the application.
         */
        return false;
    }

    @Override
    public boolean isConcurrentFontCreationAndDisposalSupported() {
        return true;
    }

    @Override
    public boolean isConcurrentImageCreationAndDisposalSupported() {
        return true;
    }

    /*
     * Graphics.
     */

    @Override
    public GRect getScreenBounds() {
        final ScreenBoundsType screenBoundsType = this.getBindingConfig().getScreenBoundsType();
        if (screenBoundsType == ScreenBoundsType.CONFIGURED) {
            final GRect screenBounds = this.getBindingConfig().getScreenBounds();
            return LangUtils.requireNonNull(screenBounds);

        } else if (screenBoundsType == ScreenBoundsType.PRIMARY_SCREEN_FULL) {
            /*
             * TODO algr I can't seem to get "al_get_monitor_info(adapter, info)"
             * to work (returns false).
             * It seems to be a known issue:
             * "And I agree, al_get_monitor_info() should work with
             * ALLEGRO_DEFAULT_DISPLAY_ADAPTER. I remember several discussions on
             * this on allegro.cc though without results, so a patch likely is not
             * easy to create. (I think the monitor with the taskbar on it is not
             * necessarily the monitor where the new display would be created or
             * something like that.)"
             */
            throw new IllegalArgumentException("" + screenBoundsType);

        } else if (screenBoundsType == ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE) {
            throw new IllegalArgumentException("" + screenBoundsType);

        } else {
            throw new IllegalArgumentException("" + screenBoundsType);
        }
    }

    /*
     * Mouse info.
     */

    @Override
    public GPoint getMousePosInScreen() {
        if (MUST_USE_AL_GET_MOUSE_STATE) {
            final ALLEGRO_MOUSE_STATE mouseState = new ALLEGRO_MOUSE_STATE();
            AlgrJnaLib.INSTANCE.al_get_mouse_state(mouseState);

            AlgrBwdHost host = null;
            if (mouseState.display == null) {
                host = this.lastUsedHostForCursorPosInScreen;
            } else {
                host = (AlgrBwdHost) this.hostByWindow_unmodView().get(mouseState.display);
            }
            if ((host != null)
                    && host.isClosed_nonVolatile()) {
                host = null;
            }
            if (host == null) {
                if (DEBUG) {
                    Dbg.logPr(this, "getMousePosInScreen() : null display and host : using last known pos in screen");
                }
                return this.getEventsConverterCommonState().getMousePosInScreen();
            }

            final GRect clientPosInScreen = host.getBackingClientBounds();
            if (DEBUG_SPAM) {
                Dbg.logPr(this, "getMousePosInScreen() : clientPosInScreen = " + clientPosInScreen);
            }

            final int mouseXInClient = this.getPixelCoordsConverter().computeXInOsPixel(mouseState.x);
            final int mouseYInClient = this.getPixelCoordsConverter().computeYInOsPixel(mouseState.y);

            final GPoint mousePosInScreen = GPoint.valueOf(
                    clientPosInScreen.x() + mouseXInClient,
                    clientPosInScreen.y() + mouseYInClient);
            if (DEBUG_SPAM) {
                Dbg.logPr(this, "getMousePosInScreen() : mouseXInClient = " + mouseXInClient);
                Dbg.logPr(this, "getMousePosInScreen() : mouseYInClient = " + mouseYInClient);
                Dbg.logPr(this, "getMousePosInScreen() : mousePosInScreen = " + mousePosInScreen);
            }

            return mousePosInScreen;
        } else {
            return this.getEventsConverterCommonState().getMousePosInScreen();
        }
    }

    /*
     * Fonts.
     */

    @Override
    public InterfaceBwdFontHome getFontHome() {
        return this.fontHome;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    /*
     * Images.
     */

    @Override
    protected InterfaceBwdImage newImageImpl(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        return new AlgrBwdImage(
                filePath,
                disposalListener);
    }

    /*
     * Shutdown.
     */

    @Override
    protected void shutdownAbruptly_bindingSpecific() {
        this.uiThreadScheduler.shutdownNow();

        this.backingCursorRepository.close();

        {
            final Pointer event_source = this.registered_mouse_event_source;
            LIB.al_unregister_event_source(this.event_queue, event_source);
            this.registered_mouse_event_source = null;
            //
            LIB.al_uninstall_mouse();
        }

        {
            final Pointer event_source = this.registered_keyboard_event_source;
            LIB.al_unregister_event_source(this.event_queue, event_source);
            this.registered_keyboard_event_source = null;
            //
            LIB.al_uninstall_keyboard();
        }

        {
            final Pointer event_queue = this.event_queue;
            if (event_queue == null) {
                throw new IllegalStateException("no event queue");
            }
            LIB.al_destroy_event_queue(event_queue);
            this.event_queue = null;
        }

        if (DEBUG) {
            Dbg.log("al_uninstall_system()...");
        }
        LIB.al_uninstall_system();
        if (DEBUG) {
            Dbg.log("...al_uninstall_system()");
        }

        if (DEBUG) {
            Dbg.log("al_shutdown_ttf_addon()...");
        }
        LIB_TTF.al_shutdown_ttf_addon();
        if (DEBUG) {
            Dbg.log("...al_shutdown_ttf_addon()");
        }

        if (DEBUG) {
            Dbg.log("al_shutdown_font_addon()...");
        }
        LIB_FONT.al_shutdown_font_addon();
        if (DEBUG) {
            Dbg.log("...al_shutdown_font_addon()");
        }

        if (DEBUG) {
            Dbg.log("al_shutdown_image_addon()...");
        }
        LIB_IMAGE.al_shutdown_image_addon();
        if (DEBUG) {
            Dbg.log("...al_shutdown_image_addon()");
        }

        if (DEBUG) {
            Dbg.log("...shutdownNow(...)");
            Dbg.flush();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * #define al_init() (al_install_system(ALLEGRO_VERSION_INT, atexit))
     */
    private static boolean al_init(InterfaceAlgrAtExit atexit) {
        return LIB.al_install_system(
                LIB.al_get_allegro_version(),
                atexit);
    }
}
