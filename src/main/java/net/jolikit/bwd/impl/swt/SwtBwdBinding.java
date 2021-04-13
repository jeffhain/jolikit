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
package net.jolikit.bwd.impl.swt;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.utils.ConfiguredExceptionHandler;
import net.jolikit.bwd.impl.utils.InterfaceBwdBindingImpl;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

/**
 * TODO swt It could be possible to use multiple such bindings in a same JVM,
 * without interferences (at least at the Java level): each would use
 * its own display, and the thread in which it was created as UI thread.
 * Though, I could not get it to work, due to this error:
 * org.eclipse.swt.SWTError: Not implemented [multiple displays]
 *    at org.eclipse.swt.SWT.error(Unknown Source)
 *    at org.eclipse.swt.widgets.Display.checkDisplay(Unknown Source)
 *    at org.eclipse.swt.widgets.Display.create(Unknown Source)
 *    at org.eclipse.swt.graphics.Device.<init>(Unknown Source)
 *    at org.eclipse.swt.widgets.Display.<init>(Unknown Source)
 *    at org.eclipse.swt.widgets.Display.<init>(Unknown Source)
 * Or, on Mac, this one that can also happen:
 * ***WARNING: Display must be created on main thread due to Cocoa restrictions.
 * org.eclipse.swt.SWTException: Invalid thread access
 *    at org.eclipse.swt.SWT.error(Unknown Source)
 *    at org.eclipse.swt.SWT.error(Unknown Source)
 *    at org.eclipse.swt.SWT.error(Unknown Source)
 *    at org.eclipse.swt.widgets.Display.error(Unknown Source)
 *    at org.eclipse.swt.widgets.Display.createDisplay(Unknown Source)
 *    at org.eclipse.swt.widgets.Display.create(Unknown Source)
 *    at org.eclipse.swt.graphics.Device.<init>(Unknown Source)
 *    at org.eclipse.swt.widgets.Display.<init>(Unknown Source)
 *    at org.eclipse.swt.widgets.Display.<init>(Unknown Source)
 */
public class SwtBwdBinding extends AbstractSwtBwdBinding {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Display display = new Display();
    
    private final SwtBwdFontHome fontHome;
    
    private final SwtCursorRepository backingCursorRepository =
            new SwtCursorRepository(this.display);
    
    private final SwtUiThreadScheduler uiThreadScheduler;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * The thread that calls the constructor becomes the UI thread for the
     * created binding.
     * 
     * @param bindingConfig Must not be null.
     */
    public SwtBwdBinding(SwtBwdBindingConfig bindingConfig) {
        super(bindingConfig);
        
        if (!SWT.isLoadable()) {
            throw new BindingError("SWT is not loadable on this platform");
        }
        
        final UncaughtExceptionHandler exceptionHandler =
                new ConfiguredExceptionHandler(bindingConfig);
        this.uiThreadScheduler = new SwtUiThreadScheduler(
                bindingConfig.getUiThreadSchedulerHardClockTimeType(),
                exceptionHandler,
                this.display);
        
        this.fontHome = new SwtBwdFontHome(
                bindingConfig,
                this.display,
                bindingConfig.getLocale());
        
        this.terminateConstruction();
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
     * 
     */

    @Override
    public Display getDisplay() {
        return this.display;
    }

    /*
     * 
     */
    
    @Override
    public InterfaceBwdHost newHost(
            String title,
            boolean decorated,
            //
            InterfaceBwdClient client) {
        final AbstractSwtBwdBinding binding = this;
        final SwtBwdHost owner = null;
        final boolean modal = false;
        return new SwtBwdHost(
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

    @Override
    public InterfaceWorkerAwareScheduler getUiThreadScheduler() {
        return this.uiThreadScheduler;
    }

    @Override
    public boolean isParallelPaintingSupported() {
        /*
         * TODO swt Parallel painting seems to work for drawing primitives
         * such as fillRect(...) etc., and images loading/unloading,
         * but not for fonts, for which we can get that:
org.eclipse.swt.SWTException: Invalid thread access
    at org.eclipse.swt.SWT.error(Unknown Source)
    at org.eclipse.swt.SWT.error(Unknown Source)
    at org.eclipse.swt.SWT.error(Unknown Source)
    at org.eclipse.swt.widgets.Widget.error(Unknown Source)
    at org.eclipse.swt.widgets.Widget.checkWidget(Unknown Source)
    at org.eclipse.swt.widgets.Text.setFont(Unknown Source)
         */
        return false;
    }

    @Override
    public boolean isConcurrentFontManagementSupported() {
        /*
         * TODO swt We can't create fonts outside of UI thread,
         * for we have to use rendering to compute metrics,
         * which outside UI thread gives that:
org.eclipse.swt.SWTException: Invalid thread access
    at org.eclipse.swt.SWT.error(Unknown Source)
    at org.eclipse.swt.SWT.error(Unknown Source)
    at org.eclipse.swt.SWT.error(Unknown Source)
    at org.eclipse.swt.widgets.Widget.error(Unknown Source)
    at org.eclipse.swt.widgets.Widget.checkWidget(Unknown Source)
    at org.eclipse.swt.widgets.Text.setFont(Unknown Source)
    at net.jolikit.bwd.impl.swt.SwtFontMetricsHelper.computeWidth(SwtFontMetricsHelper.java:87)
         */
        return false;
    }

    @Override
    public boolean isConcurrentImageFromFileManagementSupported() {
        return true;
    }
    
    @Override
    public boolean isConcurrentWritableImageManagementSupported() {
        /*
         * TODO swt False else we can get that:
Exception in thread "BwdTestCase-BG-1" org.eclipse.swt.SWTException: Invalid thread access
    at org.eclipse.swt.SWT.error(Unknown Source)
    at org.eclipse.swt.SWT.error(Unknown Source)
    at org.eclipse.swt.SWT.error(Unknown Source)
    at org.eclipse.swt.widgets.Widget.error(Unknown Source)
    at org.eclipse.swt.widgets.Widget.checkWidget(Unknown Source)
    at org.eclipse.swt.widgets.Text.setFont(Unknown Source)
    at net.jolikit.bwd.impl.swt.SwtFontMetricsHelper.computeTextWidth(SwtFontMetricsHelper.java:131)
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
        /*
         * As the spec say, for safety we prefer
         * to just stick to first monitor.
         */
        final boolean mustUseWholeDisplayElseFirstMonitor = false;
        
        final ScreenBoundsType screenBoundsType =
            this.getBindingConfig().getScreenBoundsType();
        
        final GRect ret;
        if (screenBoundsType == ScreenBoundsType.CONFIGURED) {
            final GRect screenBoundsInOs =
                this.getBindingConfig().getScreenBoundsInOs();
            ret = LangUtils.requireNonNull(screenBoundsInOs);
            
        } else if (screenBoundsType == ScreenBoundsType.PRIMARY_SCREEN_FULL) {
            final Rectangle rectangle;
            if (mustUseWholeDisplayElseFirstMonitor) {
                rectangle = this.display.getBounds();
            } else {
                final Monitor monitor = this.display.getMonitors()[0];
                rectangle = monitor.getBounds();
            }
            ret = SwtUtils.toGRect(rectangle);
            
        } else if (screenBoundsType == ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE) {
            final Rectangle rectangle;
            if (mustUseWholeDisplayElseFirstMonitor) {
                rectangle = this.display.getClientArea();
            } else {
                final Monitor monitor = this.display.getMonitors()[0];
                rectangle = monitor.getClientArea();
            }
            ret = SwtUtils.toGRect(rectangle);
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
        final Point point = this.display.getCursorLocation();
        return SwtUtils.toGPoint(point);
    }
    
    /*
     * Images.
     */
    
    @Override
    protected InterfaceBwdImage newImageImpl(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        return new SwtBwdImageFromFile(
                filePath,
                this.display,
                disposalListener);
    }

    @Override
    protected InterfaceBwdWritableImage newWritableImageImpl(
        int width,
        int height,
        InterfaceBwdImageDisposalListener disposalListener) {
        final InterfaceBwdBindingImpl binding = this;
        return new SwtBwdWritableImage(
            binding,
            width,
            height,
            this.display,
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

        this.display.dispose();
    }
}
