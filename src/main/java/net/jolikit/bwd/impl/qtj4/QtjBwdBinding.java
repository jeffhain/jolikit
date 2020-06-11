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
package net.jolikit.bwd.impl.qtj4;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ConcurrentModificationException;

import com.trolltech.qt.core.QPoint;
import com.trolltech.qt.core.QRect;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QDesktopWidget;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.ConfiguredExceptionHandler;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

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
        return false;
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
            /*
             * TODO qtj From Qt5, can use QScreen.geometry() to get that.
             */
            final int primaryScreen = this.desktopWidget.primaryScreen();
            final QRect geom = this.desktopWidget.screenGeometry(primaryScreen);
            return QtjUtils.toGRect(geom);
            
        } else if (screenBoundsType == ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE) {
            /*
             * TODO qtj From Qt5, can use QScreen.availableGeometry() to get that.
             */
            final int primaryScreen = this.desktopWidget.primaryScreen();
            final QRect geom = this.desktopWidget.availableGeometry(primaryScreen);
            return QtjUtils.toGRect(geom);
            
        } else {
            throw new IllegalArgumentException("" + screenBoundsType);
        }
    }
    
    /*
     * Mouse info.
     */
    
    @Override
    public GPoint getMousePosInScreen() {
        final QPoint pos = QCursor.pos();
        return GPoint.valueOf(pos.x(), pos.y());
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
        return new QtjBwdImage(
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
