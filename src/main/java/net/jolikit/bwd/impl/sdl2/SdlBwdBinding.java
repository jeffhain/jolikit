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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Event;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Rect;
import net.jolikit.bwd.impl.sdl2.jlib.SdlHint;
import net.jolikit.bwd.impl.sdl2.jlib.SdlImgInitFlag;
import net.jolikit.bwd.impl.sdl2.jlib.SdlInitFlag;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLibImage;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLibTtf;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaUtils;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.ConfiguredExceptionHandler;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.OsUtils;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Can only have a single instance of this binding,
 * for SDL doesn't allow for multiple contexts.
 */
public class SdlBwdBinding extends AbstractSdlBwdBinding {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * TODO sdl For some reason, on Mac, IMG_Init(...) returns an error
     * (i.e. not 0), so we don't do it, and it seems fine not to call it
     * even without loading SDL2_image binary by hand with System.load(...).
     * 
     * TODO sdl For some reason, on Windows, now actually need to load (all)
     * DLL's "by hand" with System.load(...), else, in spite of IMG_Init(...)
     * having being called with proper flags and without error,
     * I get the following error when trying to load a jpg image
     * (but no issue for bmp or png):
     * "Failed loading libjpeg-9.dll: The specified module could not be found.".
     * Also, as a result of the "manual" load, we need NOT to call
     * IMG_Init(...) (which appears to be optional),
     * else we get the following error:
     * "IMG_Init: Failed loading SHCORE.DLL: The specified module could not be found.".
     */
    public static final boolean MUST_CALL_IMG_Init_ON_CREATION = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MySystemSdlEl implements InterfaceSdlEventListener {
        @Override
        public void onEvent(SDL_Event eventUnion) {
            final Integer windowIdElseNull = SdlJnaUtils.getWindowIdIfAny(eventUnion);
            if (DEBUG) {
                Dbg.logPr(this, "windowIdElseNull = " + windowIdElseNull);
            }
            if (windowIdElseNull != null) {
                final Pointer window = windowById.get(windowIdElseNull);
                final SdlBwdHost host = (SdlBwdHost) hostByWindow_unmodView().get(window);
                if (DEBUG) {
                    Dbg.logPr(this, "host = " + host);
                }
                if (host == null) {
                    /*
                     * Host might have been closed already.
                     * Ignoring the event.
                     */
                } else {
                    host.onEvent(eventUnion);
                }
            } else {
                /*
                 * Can happen, for example with SDL_CLIPBOARDUPDATE events.
                 */
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;
    private static final SdlJnaLibTtf LIB_TTF = SdlJnaLibTtf.INSTANCE;
    private static final SdlJnaLibImage LIB_IMG = SdlJnaLibImage.INSTANCE;
    
    /*
     * 
     */
    
    private final SdlCursorRepository backingCursorRepository = new SdlCursorRepository();

    private final SdlUiThreadScheduler uiThreadScheduler;
    
    private final SdlBwdFontHome fontHome;
    
    /**
     * SDL events indicate the window using an int window id,
     * so we need this map before we can use hostByWindow
     * to retrieve the corresponding host.
     */
    private final Map<Integer,Pointer> windowById = new HashMap<Integer,Pointer>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates and inits the binding, and makes current thread its UI thread.
     * 
     * @throws IllegalStateException if such a binding has already been created
     *         and not yet shut down.
     */
    public SdlBwdBinding(SdlBwdBindingConfig bindingConfig) {
        super(bindingConfig);

        /*
         * This binding is now the current one
         * since we use a singleInstanceRef.
         */
        
        final UncaughtExceptionHandler exceptionHandler =
                new ConfiguredExceptionHandler(bindingConfig);
        this.uiThreadScheduler = new SdlUiThreadScheduler(
                bindingConfig.getUiThreadSchedulerHardClockTimeType(),
                new MySystemSdlEl(),
                bindingConfig.getEventPollPeriodS(),
                exceptionHandler);
        
        /*
         * Only initializing what we use.
         * Video implies events, but better be explicit.
         */
        
        {
            int flags = 0;
            flags |= SdlInitFlag.SDL_INIT_VIDEO.intValue();
            flags |= SdlInitFlag.SDL_INIT_EVENTS.intValue();
            if (LIB.SDL_Init(flags) != 0) {
                throw new BindingError("SDL_Init: " + LIB.SDL_GetError());
            }
        }
        
        /*
         * Hints.
         */
        
        if (!LIB.SDL_SetHint(SdlHint.SDL_MOUSE_FOCUS_CLICKTHROUGH.toString(), "1")) {
            throw new BindingError("SDL_SetHint: " + LIB.SDL_GetError());
        }
        
        /*
         * Jim Black about John Carmack and Steeve Jobs mouse:
         * - John next told Steve point blank that the iMac mouse "sucked."
         * - Steve sighed and explained that "iMac was for first-time computer buyers
         *   and every study showed that if you put more than one button on the mouse,
         *   the users ended up staring at the mouse."
         * - John sat expressionless for 2 seconds, then moved on to another topic
         *   without comment.
         *   (...)
         * - John replied, "I wanted to ask him what would happen if you put more than
         *   one key on a keyboard. But I didn't."
         * 
         * So, we could like to emulate right click with ctrl + left click here,
         * but we prefer to let client code do it if so desired
         * (which would then work with all bindings), and not to
         * mask out ctrl + left click cases.
         */
        if (false) {
            if (OsUtils.isMac()) {
                if (!LIB.SDL_SetHint(SdlHint.SDL_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK.toString(), "present")) {
                    throw new BindingError("SDL_SetHint: " + LIB.SDL_GetError());
                }
            }
        }
        
        /*
         * 
         */

        if (LIB_TTF.TTF_Init() != 0) {
            throw new BindingError("TTF_Init: " + LIB.SDL_GetError());
        }
        
        if (MUST_CALL_IMG_Init_ON_CREATION) {
            // These are in addition to BMP.
            int flags = 0;
            flags |= SdlImgInitFlag.IMG_INIT_JPG.intValue();
            flags |= SdlImgInitFlag.IMG_INIT_PNG.intValue();
            flags |= SdlImgInitFlag.IMG_INIT_TIF.intValue();
            flags |= SdlImgInitFlag.IMG_INIT_WEBP.intValue();
            if (LIB_IMG.IMG_Init(flags) != 0) {
                throw new BindingError("IMG_Init: " + LIB.SDL_GetError());
            }
        }

        this.backingCursorRepository.init();
        
        this.fontHome = new SdlBwdFontHome(bindingConfig);
        
        this.terminateConstruction();
    }
    
    @Override
    public SdlBwdBindingConfig getBindingConfig() {
        return (SdlBwdBindingConfig) super.getBindingConfig();
    }

    /*
     * Life cycle.
     */
    
    /**
     * Must be called in UI thread.
     * 
     * @throws ConcurrentModificationException if current thread is not UI thread.
     * @throws IllegalStateException if has already been called.
     */
    public void processUntilShutdownUninterruptibly() {
        this.uiThreadScheduler.processUntilShutdownUninterruptibly();
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
        final AbstractSdlBwdBinding binding = this;
        final SdlBwdHost owner = null;
        final boolean modal = false;
        return new SdlBwdHost(
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
         * TODO sdl Can have issues when rendering text concurrently
         * (did not encounter any issue with non-concurrent usages),
         * such as:
java.lang.Error: Invalid memory access
    at com.sun.jna.Native.invokePointer(Native Method)
    at com.sun.jna.Function.invokePointer(Function.java:477)
    at com.sun.jna.Function.invoke(Function.java:411)
    at com.sun.jna.Function.invoke(Function.java:323)
    at com.sun.jna.Library$Handler.invoke(Library.java:236)
    at com.sun.proxy.$Proxy1.TTF_RenderText_Solid(Unknown Source)
    at net.jolikit.bwd.impl.sdl2.SdlBwdGraphics.getTextDataAccessor(SdlBwdGraphics.java:213)
         */
        return false;
    }

    @Override
    public boolean isConcurrentFontManagementSupported() {
        return true;
    }

    @Override
    public boolean isConcurrentImageFromFileManagementSupported() {
        /*
         * TODO sdl Can have issues when loading images with SDL concurrently
         * (did not encounter any issue with non-concurrent usages),
         * in which case structures can contain more or less messy data,
         * such as:
format = JnaLibSdl$SDL_PixelFormat$ByReference(native@0x17b878) (56 bytes) {
int format@0=16762004
JnaLibSdl$SDL_Palette$ByReference palette@8=JnaLibSdl$SDL_Palette$ByReference(native@0x6c86dd30) (24 bytes) {...}
byte BitsPerPixel@10=20
byte BytesPerPixel@11=4
byte padding1@12=0
byte padding2@13=0
int Rmask@14=ff
int Gmask@18=ff00
int Bmask@1c=ff0000
int Amask@20=ff000000
byte Rloss@24=0
byte Gloss@25=0
byte Bloss@26=0
byte Aloss@27=0
byte Rshift@28=0
byte Gshift@29=8
byte Bshift@2a=10
byte Ashift@2b=18
int refcount@2c=0
Pointer next@30=native@0x17a520
}
palette = JnaLibSdl$SDL_Palette$ByReference(native@0x6c86dd30) (24 bytes) {
int ncolors@0=6c86dd20
Pointer colors@8=native@0x6c86dd20
int version@10=17b868
int refcount@14=0
}
         * or:
format = JnaLibSdl$SDL_PixelFormat$ByReference(native@0x1cb038) (56 bytes) {
  int format@0=6c86dcc0
  JnaLibSdl$SDL_Palette$ByReference palette@8=JnaLibSdl$SDL_Palette$ByReference(native@0x6c86dcc0) (24 bytes) {...}
  byte BitsPerPixel@10=29
  byte BytesPerPixel@11=0
  byte padding1@12=0
  byte padding2@13=0
  int Rmask@14=15
  int Gmask@18=a4
  int Bmask@1c=0
  int Amask@20=1cb510
  byte Rloss@24=0
  byte Gloss@25=0
  byte Bloss@26=0
  byte Aloss@27=0
  byte Rshift@28=0
  byte Gshift@29=0
  byte Bshift@2a=0
  byte Ashift@2b=0
  int refcount@2c=0
  Pointer next@30=null
}
palette = JnaLibSdl$SDL_Palette$ByReference(native@0x6c86dcc0) (24 bytes) {
  int ncolors@0=6c86dcb0
  Pointer colors@8=native@0x6c86dcb0
  int version@10=1cb028
  int refcount@14=0
}
         * or:
format = JnaLibSdl$SDL_PixelFormat$ByReference(native@0x6c86dcc0) (56 bytes) {
  int format@0=6c86dcb0
  JnaLibSdl$SDL_Palette$ByReference palette@8=JnaLibSdl$SDL_Palette$ByReference(native@0x6c86dcb0) (24 bytes) {...}
  byte BitsPerPixel@10=28
  byte BytesPerPixel@11=ffffffb0
  byte padding1@12=1c
  byte padding2@13=0
  int Rmask@14=0
  int Gmask@18=1cb028
  int Bmask@1c=0
  int Amask@20=6c86dcd0
  byte Rloss@24=0
  byte Gloss@25=0
  byte Bloss@26=0
  byte Aloss@27=0
  byte Rshift@28=ffffffd0
  byte Gshift@29=ffffffdc
  byte Bshift@2a=ffffff86
  byte Ashift@2b=6c
  int refcount@2c=0
  Pointer next@30=native@0x6c86dce0
}
palette = JnaLibSdl$SDL_Palette$ByReference(native@0x6c86dcb0) (24 bytes) {
  int ncolors@0=6c86dca0
  Pointer colors@8=native@0x6c86dca0
  int version@10=6c86dcb0
  int refcount@14=0
}
         */
        return false;
    }
    
    @Override
    public boolean isConcurrentWritableImageManagementSupported() {
        /*
         * TODO sdl2 False else we can get:
Exception in thread "main" java.lang.Error: Invalid memory access
    at com.sun.jna.Native.invokePointer(Native Method)
    at com.sun.jna.Function.invokePointer(Function.java:490)
    at com.sun.jna.Function.invoke(Function.java:434)
    at com.sun.jna.Function.invoke(Function.java:354)
    at com.sun.jna.Library$Handler.invoke(Library.java:244)
    at com.sun.proxy.$Proxy1.TTF_RenderGlyph_Blended(Unknown Source)
    at net.jolikit.bwd.impl.sdl2.SdlBwdGraphics.newTextDataAccessor_RenderGlyph_Blended(SdlBwdGraphics.java:405)
         */
        return false;
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
     * Screen info.
     */
    
    @Override
    protected GRect getScreenBounds_rawInOs() {
        final ScreenBoundsType screenBoundsType =
            this.getBindingConfig().getScreenBoundsType();
        
        final GRect ret;
        if (screenBoundsType == ScreenBoundsType.CONFIGURED) {
            final GRect screenBoundsInOs =
                this.getBindingConfig().getScreenBoundsInOs();
            ret = LangUtils.requireNonNull(screenBoundsInOs);
            
        } else if (screenBoundsType == ScreenBoundsType.PRIMARY_SCREEN_FULL) {
            final int displayIndex = 0;
            final SDL_Rect rect = new SDL_Rect();
            if (LIB.SDL_GetDisplayBounds(displayIndex, rect) != 0) {
                throw new BindingError("SDL_GetDisplayBounds : " + LIB.SDL_GetError());
            }
            ret = SdlUtils.toGRect(rect);
            
        } else if (screenBoundsType == ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE) {
            final int displayIndex = 0;
            final SDL_Rect rect = new SDL_Rect();
            if (LIB.SDL_GetDisplayUsableBounds(displayIndex, rect) != 0) {
                throw new BindingError("SDL_GetDisplayUsableBounds : " + LIB.SDL_GetError());
            }
            ret = SdlUtils.toGRect(rect);
            
        } else {
            throw new IllegalArgumentException("" + screenBoundsType);
        }
        return ret;
    }
    
    /*
     * Mouse info.
     */
    
    @Override
    protected GPoint getMousePosInScreen_rawInOs() {
        
        final IntByReference xRef = new IntByReference();
        final IntByReference yRef = new IntByReference();
        
        @SuppressWarnings("unused")
        final int buttonStateBits_unused =
                LIB.SDL_GetGlobalMouseState(xRef, yRef);
        
        return GPoint.valueOf(xRef.getValue(), yRef.getValue());
    }
    
    /*
     * Hosts.
     */
    
    @Override
    protected void onHostCreatedImpl(AbstractBwdHost host) {
        super.onHostCreatedImpl(host);
        
        final Pointer window = (Pointer) host.getBackingWindow();
        
        final int windowId = LIB.SDL_GetWindowID(window);
        if (DEBUG) {
            Dbg.log("host created:");
            Dbg.log("windowId = " + windowId);
            Dbg.log("window = " + window);
            Dbg.log("host = " + host);
        }
        final Object forCheck = this.windowById.put(windowId, window);
        if (forCheck != null) {
            // Maybe due to bad concurrent usage.
            throw new BindingError("already had a window with id " + windowId);
        }
    }
    
    @Override
    protected void onHostClosingImpl(AbstractBwdHost host) {
        final SdlBwdHost hostImpl = (SdlBwdHost) host;
        final Pointer window = hostImpl.getBackingWindow();
        final int windowId = hostImpl.getWindowId();
        
        final Object forCheck = this.windowById.remove(windowId);
        if (forCheck != window) {
            // Maybe due to bad concurrent usage.
            throw new BindingError("expected " + window + ", got " + forCheck);
        }
        
        // Ideally best to call super closing last.
        super.onHostClosingImpl(host);
    }
    
    /*
     * Images.
     */

    @Override
    protected InterfaceBwdImage newImageImpl(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        return new SdlBwdImageFromFile(
                filePath,
                disposalListener);
    }

    @Override
    protected InterfaceBwdWritableImage newWritableImageImpl(
            int width,
            int height,
            InterfaceBwdImageDisposalListener disposalListener) {
        final InterfaceBwdBinding binding = this;
        return new SdlBwdWritableImage(
                binding,
                width,
                height,
                disposalListener);
    }

    /*
     * Shutdown.
     */

    @Override
    protected void shutdownAbruptly_bindingSpecific() {
        // Must be done before SDL_Quit, because it might use a poison-pill user
        // SDL event, which requires SDL to be alive.
        if (DEBUG) {
            Dbg.log("uiThreadScheduler.shutdownNow()...");
        }
        this.uiThreadScheduler.shutdownNow();
        if (DEBUG) {
            Dbg.log("...uiThreadScheduler.shutdownNow()");
        }

        this.backingCursorRepository.close();

        if (DEBUG) {
            Dbg.log("IMG_Quit()...");
        }
        SdlJnaLibImage.INSTANCE.IMG_Quit();
        if (DEBUG) {
            Dbg.log("...IMG_Quit()");
        }

        if (DEBUG) {
            Dbg.log("TTF_Quit()...");
        }
        SdlJnaLibTtf.INSTANCE.TTF_Quit();
        if (DEBUG) {
            Dbg.log("...TTF_Quit()");
        }

        if (DEBUG) {
            Dbg.log("SDL_Quit()...");
        }
        LIB.SDL_Quit();
        if (DEBUG) {
            Dbg.log("...SDL_Quit()");
        }
    }
}
