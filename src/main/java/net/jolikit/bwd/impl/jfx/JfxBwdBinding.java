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
package net.jolikit.bwd.impl.jfx;

import java.lang.Thread.UncaughtExceptionHandler;

import javafx.application.Platform;
import javafx.stage.Screen;
import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.utils.ConfiguredExceptionHandler;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

/**
 * TODO jfx On Windows, use "-Dglass.win.uiScale=1.00" JVM option,
 * to avoid eventual Windows "scaling" to have an (unnatural)
 * squared-scaling effect
 * (cf. http://mail.openjdk.java.net/pipermail/openjfx-dev/2015-June/017337.html).
 */
public class JfxBwdBinding extends AbstractJfxBwdBinding {

    /*
     * TODO jfx Screen.getPrimary().getDpi() method returns dpi as double,
     * but yet causes rounding errors (227/2 = 113 on 13.3 inch 2560 x 1600
     * retina, instead of 113.5), so we can't use it to compute pixel size.
     * 
     * TODO jfx Log that can happen when using JavaFX,
     * for example in host unit testing with client throwing on event:
java.lang.NullPointerException
    at com.sun.prism.es2.ES2SwapChain.createGraphics(ES2SwapChain.java:218)
    at com.sun.prism.es2.ES2SwapChain.createGraphics(ES2SwapChain.java:40)
    at com.sun.javafx.tk.quantum.PresentingPainter.run(PresentingPainter.java:87)
    at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
    at java.util.concurrent.FutureTask.runAndReset(FutureTask.java:308)
    at com.sun.javafx.tk.RenderJob.run(RenderJob.java:58)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
    at com.sun.javafx.tk.quantum.QuantumRenderer$PipelineRunnable.run(QuantumRenderer.java:125)
    at java.lang.Thread.run(Thread.java:748)
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Cf. constructor javadoc.
     */
    private static final boolean MUST_SET_NO_IMPLICIT_EXIT_ON_CREATION = true; 
    
    /**
     * Cf. constructor javadoc.
     */
    private static final boolean MUST_EXIT_JFX_PLATFORM_ON_SHUTDOWN = true; 

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final JfxUiThreadScheduler uiThreadScheduler;
    
    private final JfxBwdFontHome fontHome;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * TODO jfx In JavaFX, hiding and closing a Stage is the same,
     * and by default when all stages are hidden (or closed),
     * the application exits.
     * Since we don't want the application to exit when all hosts are just
     * hidden (by BWD semantics), this constructor calls
     * javafx.application.Platform.setImplicitExit(false).
     * Also, found this:
     * "Additionally, if you aren't going to display the primary Stage in the
     * Application start(Stage) method then I also recommend you call:
     * Platform.setImplicitExit(false);
     * in the start method, since you will need to control the life-cycle of
     * the application yourself and call Platform.exit() to exit the FX runtime."
     * 
     * TODO jfx javafx.application.Platform.exit() is called on shutdown,
     * else the JVM typically stays up.
     * 
     * @param bindingConfig Must not be null.
     */
    public JfxBwdBinding(JfxBwdBindingConfig bindingConfig) {
        super(bindingConfig);
        
        if (MUST_SET_NO_IMPLICIT_EXIT_ON_CREATION) {
            Platform.setImplicitExit(false);
        }
        
        final UncaughtExceptionHandler exceptionHandler =
                new ConfiguredExceptionHandler(bindingConfig);
        this.uiThreadScheduler = new JfxUiThreadScheduler(
                bindingConfig.getUiThreadSchedulerHardClockTimeType(),
                exceptionHandler);
        
        this.fontHome = new JfxBwdFontHome(bindingConfig);
        
        this.terminateConstruction();
    }
    
    @Override
    public JfxBwdBindingConfig getBindingConfig() {
        return (JfxBwdBindingConfig) super.getBindingConfig();
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
        final AbstractJfxBwdBinding binding = this;
        final JfxBwdHost owner = null;
        final boolean modal = false;
        return new JfxBwdHost(
                binding,
                this.getHostLifecycleListener(),
                owner,
                //
                title,
                decorated,
                modal,
                //
                client);
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
        return false;
    }

    @Override
    public boolean isConcurrentFontManagementSupported() {
        return true;
    }

    @Override
    public boolean isConcurrentImageFromFileManagementSupported() {
        return true;
    }
    
    @Override
    public boolean isConcurrentWritableImageManagementSupported() {
        /*
         * False since for writable images, even though we use
         * an int array based graphics, we still use a GC
         * and snapshot for text drawing, which requires
         * to be called in UI thread.
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
            ret = JfxUtils.toGRect(Screen.getPrimary().getBounds());
            
        } else if (screenBoundsType == ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE) {
            ret = JfxUtils.toGRect(Screen.getPrimary().getVisualBounds());
            
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
        /*
         * TODO jfx No public API for it, so we do best effort.
         */
        return this.getEventsConverterCommonState().getMousePosInScreenInOs();
    }
    
    /*
     * Images.
     */

    @Override
    protected InterfaceBwdImage newImageImpl(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        return new JfxBwdImageFromFile(
                filePath,
                disposalListener);
    }

    @Override
    protected InterfaceBwdWritableImage newWritableImageImpl(
            int width,
            int height,
            InterfaceBwdImageDisposalListener disposalListener) {
        final AbstractJfxBwdBinding binding = this;
        return new JfxBwdWritableImage(
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
        this.uiThreadScheduler.shutdownAbruptly();
        
        if (MUST_EXIT_JFX_PLATFORM_ON_SHUTDOWN) {
            Platform.exit();
        }
    }
}
