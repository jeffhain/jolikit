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
package net.jolikit.bwd.impl.jogl;

import java.lang.Thread.UncaughtExceptionHandler;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.awt.AwtBwdFontHome;
import net.jolikit.bwd.impl.utils.ConfiguredExceptionHandler;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;

public class JoglBwdBinding extends AbstractJoglBwdBinding {

    /*
     * NB: JOGL uses OpenGL for drawing, for which (0,0) is bottom-left corner
     * (not top left corner "as usual").
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final Display display;
    private final Screen screen;

    private final JoglCursorRepository backingCursorRepository;

    private final JoglUiThreadScheduler uiThreadScheduler;

    private final AwtBwdFontHome fontHome;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param bindingConfig Must not be null.
     * @throws IllegalStateException if such a binding has already been created
     *         and not yet shut down.
     */
    public JoglBwdBinding(JoglBwdBindingConfig bindingConfig) {
        super(bindingConfig);

        /*
         * This binding is now the current one,
         * since we use a singleInstanceRef.
         */

        final String displayConnectionName = null;
        final Display display = NewtFactory.createDisplay(displayConnectionName);
        this.display = display;

        final Screen screen = NewtFactory.createScreen(display, 0);
        this.screen = screen;

        /*
         * Post-library-init stuffs.
         */

        this.backingCursorRepository = new JoglCursorRepository(display);

        final UncaughtExceptionHandler exceptionHandler =
                new ConfiguredExceptionHandler(bindingConfig);
        
        final JoglUiThreadScheduler uiThreadScheduler = new JoglUiThreadScheduler(
                bindingConfig.getUiThreadSchedulerHardClockTimeType(),
                exceptionHandler,
                display,
                bindingConfig.getNewtEdtPollPeriodS());
        this.uiThreadScheduler = uiThreadScheduler;

        /*
         * TODO jogl Need to call that here,
         * else, in backingCursorRepository.init(), we get:
         * java.lang.IllegalStateException: Display.createPointerIcon: Display invalid
         *  NEWT-Display[.windows_nil-1, excl false, refCount 0, hasEDT true, edtRunning true, null]
         *     at jogamp.newt.DisplayImpl$3.run(DisplayImpl.java:207)
         *     at jogamp.newt.DisplayImpl.runOnEDTIfAvail(DisplayImpl.java:450)
         *     at jogamp.newt.DisplayImpl.createPointerIcon(DisplayImpl.java:203)
         */
        display.createNative();
        
        this.backingCursorRepository.init();
        
        this.fontHome = new AwtBwdFontHome(
                bindingConfig,
                bindingConfig.getLocale());

        this.terminateConstruction();
    }

    @Override
    public JoglBwdBindingConfig getBindingConfig() {
        return (JoglBwdBindingConfig) super.getBindingConfig();
    }

    @Override
    public Screen getScreen() {
        return this.screen;
    }

    /**
     * Must NOT be called in UI thread.
     * 
     * Must be called in some non-daemon thread,
     * for example in main thread after you started all what you wanted,
     * else the JVM just terminates due to UI thread being a daemon thread
     * (TODO jogl We don't want to make it a non-daemon, in case there is
     * a reason it's one).
     * 
     * @throws IllegalStateException if current thread is UI thread.
     *         or if has already been called.
     */
    public void waitUntilShutdownUninterruptibly() {
        this.uiThreadScheduler.waitUntilShutdownUninterruptibly();
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
        final AbstractJoglBwdBinding binding = this;
        final JoglBwdHost owner = null;
        final boolean modal = false;
        return new JoglBwdHost(
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
        return true;
    }

    @Override
    public boolean isConcurrentFontManagementSupported() {
        return true;
    }

    @Override
    public boolean isConcurrentImageManagementSupported() {
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
            final RectangleImmutable viewPort = this.screen.getViewport();
            return GRect.valueOf(
                    viewPort.getX(),
                    viewPort.getY(),
                    viewPort.getWidth(),
                    viewPort.getHeight());

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
         * TODO jogl No API for it, so we do best effort.
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
        return new JoglBwdImage(
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

        this.screen.destroy();

        this.display.destroy();
    }
}
