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
package net.jolikit.bwd.impl.awt;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.lang.Thread.UncaughtExceptionHandler;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.ConfiguredExceptionHandler;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

public class AwtBwdBinding extends AbstractAwtBwdBinding {

    /*
     * TODO awt Toolkit.getDefaultToolkit().getScreenResolution() returns dpi
     * as integer, so causes rounding errors (227/2 = 113 on 13.3 inch
     * 2560 x 1600 retina, instead of 113.5), so we can't use it to compute
     * pixel size, if we ever need to.
     */
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final AwtUiThreadScheduler uiThreadScheduler;
    
    private final AwtBwdFontHome fontHome;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param bindingConfig Must not be null.
     */
    public AwtBwdBinding(AwtBwdBindingConfig bindingConfig) {
        super(bindingConfig);
        
        final UncaughtExceptionHandler exceptionHandler =
                new ConfiguredExceptionHandler(bindingConfig);
        this.uiThreadScheduler = new AwtUiThreadScheduler(
                bindingConfig.getUiThreadSchedulerHardClockTimeType(),
                exceptionHandler);
        
        this.fontHome = new AwtBwdFontHome(
                bindingConfig,
                bindingConfig.getLocale());
        
        this.terminateConstruction();
    }

    @Override
    public AwtBwdBindingConfig getBindingConfig() {
        return (AwtBwdBindingConfig) super.getBindingConfig();
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
        final AbstractAwtBwdBinding binding = this;
        final AwtBwdHost owner = null;
        final boolean modal = false;
        return new AwtBwdHost(
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
            if (false) {
                // Not for us : eventually goes multi-screen.
                GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            }
            
            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice gd = ge.getDefaultScreenDevice();
            final GraphicsConfiguration gc = gd.getDefaultConfiguration();
            
            final Rectangle backingPrimaryScreenFullBounds = gc.getBounds();

            final GRect primaryScreenFullBounds =
                    AwtUtils.toGRect(
                            backingPrimaryScreenFullBounds);
            return primaryScreenFullBounds;
            
        } else if (screenBoundsType == ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE) {
            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice gd = ge.getDefaultScreenDevice();
            final GraphicsConfiguration gc = gd.getDefaultConfiguration();
            
            final Rectangle backingPrimaryScreenFullBounds = gc.getBounds();
            final Insets backingInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            
            final GRect primaryScreenFullBounds =
                    AwtUtils.toGRect(
                            backingPrimaryScreenFullBounds);
            final GRect insets = GRect.valueOf(
                    backingInsets.left,
                    backingInsets.top,
                    backingInsets.right,
                    backingInsets.bottom);
            
            final GRect screenBounds =
                    BindingCoordsUtils.computeClientBounds(
                            insets,
                            primaryScreenFullBounds);
            return screenBounds;
        } else {
            throw new IllegalArgumentException("" + screenBoundsType);
        }
    }
    
    /*
     * Mouse info.
     */
    
    @Override
    public GPoint getMousePosInScreen() {
        final Point point = MouseInfo.getPointerInfo().getLocation();
        return GPoint.valueOf(point.x, point.y);
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
        return new AwtBwdImageFromFile(
                filePath,
                disposalListener);
    }
    
    /*
     * Shutdown.
     */
    
    @Override
    protected void shutdownAbruptly_bindingSpecific() {
        this.uiThreadScheduler.shutdownAbruptly();
    }
}
