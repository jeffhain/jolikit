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
package net.jolikit.bwd.impl.lwjgl3;

import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.awt.AwtBwdFontHome;
import net.jolikit.bwd.impl.utils.ConfiguredExceptionHandler;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.OsUtils;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;

public class LwjglBwdBinding extends AbstractLwjglBwdBinding {
    
    /*
     * NB: LWJGL uses OpenGL for drawing, for which (0,0) is bottom-left corner
     * (not top left corner "as usual"), so beware.
     */
    
    /*
     * TODO lwjgl In LWJGL2, can use Display.getPixelScaleFactor() to compute
     * points per pixel (if ever needed for fonts), but in LWJGL3 there seems
     * to be no way to compute it.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyBindingEl extends AbstractLwjglBindingEventListener {
        public MyBindingEl() {
        }
        @Override
        protected void onErrorEvent(int error, long description) {
            final PrintStream stream = getBindingConfig().getIssueStream();
            stream.println(
                    this.getClass().getSimpleName()
                    + ".onErrorEvent("
                    + error
                    + ", "
                    + GLFWErrorCallback.getDescription(description)
                    + ")");
        }
        @Override
        protected void onJoystickEvent(int joy, int event) {
            if (DEBUG) {
                Dbg.log("onJoystickEvent(" + joy + ", " + event + ")");
            }
        }
        @Override
        protected void onMonitorEvent(long monitor, int event) {
            if (DEBUG) {
                Dbg.log("onMonitorEvent(" + monitor + ", " + event + ")");
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final LwjglCursorRepository backingCursorRepository;

    /**
     * Need to keep a reference to it, for callbacks not to be GCed.
     */
    private final MyBindingEl bindingEventListener;
    
    private final LwjglUiThreadScheduler uiThreadScheduler;
    
    private final AwtBwdFontHome fontHome;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates and inits the binding, and makes current thread its UI thread.
     * 
     * @param bindingConfig Must not be null.
     * @throws IllegalStateException if such a binding has already been created
     *         and not yet shut down.
     */
    public LwjglBwdBinding(LwjglBwdBindingConfig bindingConfig) {
        super(bindingConfig);
        
        /*
         * This binding is now the current one
         * since we use a singleInstanceRef.
         */
        
        {
            final PrintStream stream = bindingConfig.getIssueStream();
            // Setup an error callback. The default implementation
            // will print the error message in our issue stream.
            final GLFWErrorCallback errorCb = GLFWErrorCallback.createPrint(stream);
            errorCb.set();
     
            // Initialize GLFW. Most GLFW functions will not work before doing this.
            if (!GLFW.glfwInit()) {
                throw new BindingError("could not init GLFW");
            }
     
            if (false) {
                // TODO lwjgl If ever wanting to enable v-sync
                GLFW.glfwSwapInterval(1);
            }
        }
        
        /*
         * Post-library-init stuffs.
         */
        
        final MyBindingEl bindingEventListener = new MyBindingEl();
        this.bindingEventListener = bindingEventListener;
        bindingEventListener.setCallbacks();
        
        this.backingCursorRepository = new LwjglCursorRepository(
                bindingConfig.getMustUseSystemCursorsWhenAvailable());
        
        final UncaughtExceptionHandler exceptionHandler =
                new ConfiguredExceptionHandler(bindingConfig);
        this.uiThreadScheduler = new LwjglUiThreadScheduler(
                bindingConfig.getUiThreadSchedulerHardClockTimeType(),
                exceptionHandler);
        
        /*
         * TODO lwjgl glfwCreateCursor(...) spec says
         * it must be called in "the main thread",
         * but we don't have access to "main thread" here.
         * If really need to use it in "main thread",
         * we could just specify that this binding must
         * be created in the main thread.
         */
        this.backingCursorRepository.init();
        
        this.fontHome = new AwtBwdFontHome(
                bindingConfig,
                bindingConfig.getLocale());
        
        this.terminateConstruction();
    }
    
    @Override
    public LwjglBwdBindingConfig getBindingConfig() {
        return (LwjglBwdBindingConfig) super.getBindingConfig();
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

    /*
     * Hosts.
     */
    
    @Override
    public InterfaceBwdHost newHost(
            String title,
            boolean decorated,
            //
            InterfaceBwdClient client) {
        final AbstractLwjglBwdBinding binding = this;
        final LwjglBwdHost owner = null;
        final boolean modal = false;
        return new LwjglBwdHost(
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
         * TODO lwjgl On Windows parallel painting looks fine,
         * but on Mac, got that:
         * 
RAX={method} {0x000000010fb0ee80} 'get' '(I)I' in 'java/nio/DirectIntBufferU'
RBX=0x0000000797b82a28 is an oop
[error occurred during error reporting (printing register info), id 0xa]
Stack: [0x00007fff56672000,0x00007fff56e72000],  sp=0x00007fff56e6e388,  free space=8176k
Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)
C  0x000000010fb0ee80
J 1629  org.lwjgl.system.JNI.callPV(JIIIIIIII[I)V (0 bytes) @ 0x000000011169eb6e [0x000000011169e9e0+0x18e]
j  org.lwjgl.opengl.GL11.glTexImage2D(IIIIIIII[I)V+24
j  net.jolikit.bwd.impl.lwjgl3.LwjglPaintHelper.createTexture(Lnet/jolikit/bwd/impl/lwjgl3/LwjglPaintHelper$MyTextureData;)I+123
J 1534 C1 net.jolikit.bwd.impl.lwjgl3.LwjglPaintHelper.paintPixelsIntoOpenGl(Lnet/jolikit/bwd/impl/lwjgl3/LwjglHostBoundsHelper;Lnet/jolikit/bwd/impl/utils/IntArrayGraphicBuffer;Ljava/util/List;JLorg/lwjgl/opengl/GLCapabilities;)V (247 bytes) @ 0x0000000111661c0c [0x0000000111661120+0xaec]
         * 
Register to memory mapping:
RAX=0x00007f963a808000 is a thread
RBX=0x0000000797b34020 is an oop
[error occurred during error reporting (printing register info), id 0xa]
Stack: [0x00007fff52039000,0x00007fff52839000],  sp=0x00007fff528353f8,  free space=8176k
Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)
C  0x00007f963a808000
J 1677  org.lwjgl.system.JNI.callPV(JIIIIIIII[I)V (0 bytes) @ 0x00000001105c116e [0x00000001105c0fe0+0x18e]
J 1676 C1 org.lwjgl.opengl.GL11.glTexImage2D(IIIIIIII[I)V (28 bytes) @ 0x00000001105c15ec [0x00000001105c1420+0x1cc]
j  net.jolikit.bwd.impl.lwjgl3.LwjglPaintHelper.createTexture(Lnet/jolikit/bwd/impl/lwjgl3/LwjglPaintHelper$MyTextureData;)I+123
j  net.jolikit.bwd.impl.lwjgl3.LwjglPaintHelper.paintPixelsIntoOpenGl(Lnet/jolikit/bwd/impl/lwjgl3/LwjglHostBoundsHelper;Lnet/jolikit/bwd/impl/utils/IntArrayGraphicBuffer;Ljava/util/List;JLorg/lwjgl/opengl/GLCapabilities;)V+125
         */
        return !OsUtils.isMac();
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
            final long monitor = GLFW.glfwGetPrimaryMonitor();
            if (monitor == MemoryUtil.NULL) {
                throw new BindingError("glfwGetPrimaryMonitor");
            }
            final GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);
            return GRect.valueOf(
                    0,
                    0,
                    videoMode.width(),
                    videoMode.height());
            
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
        /*
         * TODO lwjgl No window-agnostic API for this,
         * so we just use last value from events
         * (which among other things use the
         * glfwSetCursorPosCallback(...) window-specific API).
         */
        return this.getEventsConverterCommonState().getMousePosInScreen();
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
        return new LwjglBwdImage(
                filePath,
                disposalListener);
    }
    
    /*
     * Shutdown.
     */

    @Override
    protected void shutdownAbruptly_bindingSpecific() {
        if (DEBUG) {
            Dbg.log("uiThreadScheduler.shutdownNow()...");
        }
        this.uiThreadScheduler.shutdownNow();
        if (DEBUG) {
            Dbg.log("...uiThreadScheduler.shutdownNow()");
        }

        this.backingCursorRepository.close();

        if (DEBUG) {
            Dbg.log("glfwTerminate()...");
        }
        GLFW.glfwTerminate();
        if (DEBUG) {
            Dbg.log("...glfwTerminate()");
        }

        if (DEBUG) {
            Dbg.log("glfwSetErrorCallback(null).free()...");
        }
        {
            final GLFWErrorCallback prevCb = GLFW.glfwSetErrorCallback(null);
            prevCb.free();
        }
        if (DEBUG) {
            Dbg.log("...glfwSetErrorCallback(null).free()");
        }
    }
}
