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
package net.jolikit.bwd.impl.qtj4;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;

import net.jolikit.bwd.api.InterfaceBwdBinding;
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
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.ObjectWrapper;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

import com.trolltech.qt.core.QPoint;
import com.trolltech.qt.core.QRect;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QDesktopWidget;

public class QtjBwdBinding extends AbstractQtjBwdBinding {

    /*
     * TODO qtj On my 13.3 inch 2560 x 1600 retina,
     * whatever the used OS-level resolution from 1680 x 1050
     * to 1024 x 640, the following methods:
     * - QApplication.desktop().physicalDpiY()
     * - QApplication.desktop().logicalDpiY()
     * - QApplication.desktop().screen().physicalDpiY()
     * - QApplication.desktop().screen().logicalDpiY()
     * all return the value 72 (which happens to be the number of "points"
     * per inch: Qt seems to confuse "points", and pixels or dots),
     * so we couldn't use them.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * To get screen bounds.
     */
    private final QDesktopWidget desktopWidget;

    /*
     * 
     */
    
    private final QtjUiThreadScheduler uiThreadScheduler;
    
    private final QtjBwdFontHome fontHome;
    
    /**
     * To contain the reference to a pool of Qt objects,
     * shared among all images graphics for a same binding.
     * 
     * The pool itself is created and managed by the graphics class.
     */
    private final ObjectWrapper<Object> bindingQtStuffsPoolRef =
            new ObjectWrapper<Object>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates and inits the binding, and makes current thread its UI thread.
     * 
     * @param bindingConfig Must not be null.
     * @param args For QApplication.initialize(...).
     * @throws IllegalStateException if such a binding has already been created
     *         and not yet shut down.
     */
    public QtjBwdBinding(
            QtjBwdBindingConfig bindingConfig,
            String[] args) {
        super(bindingConfig);
        
        /*
         * This binding is now the current one
         * since we use a singleInstanceRef.
         */
        
        final String classSimpleName = this.getClass().getSimpleName();
        QApplication.initialize(classSimpleName, args);
        
        this.desktopWidget = new QDesktopWidget();

        final UncaughtExceptionHandler exceptionHandler =
                new ConfiguredExceptionHandler(bindingConfig);
        this.uiThreadScheduler = new QtjUiThreadScheduler(
                bindingConfig.getUiThreadSchedulerHardClockTimeType(),
                exceptionHandler);

        this.fontHome = new QtjBwdFontHome(
                bindingConfig,
                bindingConfig.getWritingSystem());
        
        this.terminateConstruction();
    }
    
    @Override
    public QtjBwdBindingConfig getBindingConfig() {
        return (QtjBwdBindingConfig) super.getBindingConfig();
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
        final AbstractQtjBwdBinding binding = this;
        final QtjBwdHost owner = null;
        final boolean modal = false;
        return new QtjBwdHost(
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
        /*
         * Parallel painting on a same painting device is not supported in Qt
         * (cf. http://doc.qt.io/qt-4.8/threads-modules.html:
         *  "Any number of threads can paint at any given time,
         *  however only one thread at a time can paint on a given paint device
         *  [QImage, QPrinter, or QPicture].")
         * That means that a QPainter (used to paint on a device) can't be used
         * in parallel, and that we can't parallelize painting on clients or
         * on writable images.
         */
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
        return true;
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
            /*
             * TODO qtj From Qt5, can use QScreen.geometry() to get that.
             */
            final int primaryScreen = this.desktopWidget.primaryScreen();
            final QRect geom = this.desktopWidget.screenGeometry(primaryScreen);
            ret = QtjUtils.toGRect(geom);
            
        } else if (screenBoundsType == ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE) {
            /*
             * TODO qtj From Qt5, can use QScreen.availableGeometry() to get that.
             */
            final int primaryScreen = this.desktopWidget.primaryScreen();
            final QRect geom = this.desktopWidget.availableGeometry(primaryScreen);
            ret = QtjUtils.toGRect(geom);
            
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
        final QPoint pos = QCursor.pos();
        return GPoint.valueOf(pos.x(), pos.y());
    }
    
    /*
     * Images.
     */

    @Override
    protected InterfaceBwdImage newImageImpl(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener) {
        return new QtjBwdImageFromFile(
                filePath,
                disposalListener);
    }
    
    @Override
    protected InterfaceBwdWritableImage newWritableImageImpl(
            int width,
            int height,
            InterfaceBwdImageDisposalListener disposalListener) {
        final InterfaceBwdBinding binding = this;
        return new QtjBwdWritableImage(
                binding,
                width,
                height,
                this.bindingQtStuffsPoolRef,
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

        this.desktopWidget.dispose();

        if (DEBUG) {
            Dbg.log("QApplication.quit()...");
        }
        QApplication.quit();
        if (DEBUG) {
            Dbg.log("...QApplication.quit()");
        }
    }
}
