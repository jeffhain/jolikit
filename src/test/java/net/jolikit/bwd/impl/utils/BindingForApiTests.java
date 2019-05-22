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
package net.jolikit.bwd.impl.utils;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;
import net.jolikit.time.sched.misc.SoftTestHelper;

/**
 * For non-graphical unit tests.
 */
public class BindingForApiTests extends AbstractBwdBinding {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyUiThreadScheduler extends AbstractWorkerAwareSchedulerAdapter {
        public MyUiThreadScheduler(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        @Override
        public boolean isWorkerThread() {
            // Soft: always true.
            return true;
        }
        @Override
        public void checkIsWorkerThread() {
            // Soft: no check.
        }
        @Override
        public void checkIsNotWorkerThread() {
            // Soft: no check.
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final SoftTestHelper softTestHelper = new SoftTestHelper();
    
    private final MyUiThreadScheduler uiThreadScheduler = new MyUiThreadScheduler(
            this.softTestHelper.getScheduler());
    
    private final FontHomeForApiTests fontHome = new FontHomeForApiTests();
    
    private final GRect insets;
    
    private final GRect maximizedClientBounds;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param insets For hosts.
     * @param maximizedClientBounds Set on maximization. Can be null.
     */
    public BindingForApiTests(
            BaseBwdBindingConfig bindingConfig,
            //
            GRect insets,
            GRect maximizedClientBounds) {
        super(bindingConfig, null);
        
        this.insets = insets;
        
        this.maximizedClientBounds = maximizedClientBounds;
        
        this.terminateConstruction();
    }
    
    public SoftTestHelper getSoftTestHelper() {
        return this.softTestHelper;
    }

    @Override
    public InterfaceBwdHost newHost(
            String title,
            boolean decorated,
            InterfaceBwdClient client) {
        final boolean modal = false;
        final HostForApiTests host = new HostForApiTests(
                this.getBindingConfig(),
                this, // binding
                this.getHostLifecycleListener(),
                this.getHostOfFocusedClientHolder(),
                null, // owner
                title,
                decorated,
                modal,
                client,
                //
                this.insets,
                this.maximizedClientBounds);
        return host;
    }

    @Override
    public InterfaceWorkerAwareScheduler getUiThreadScheduler() {
        return this.uiThreadScheduler;
    }

    @Override
    public boolean isParallelPaintingSupported() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConcurrentFontCreationAndDisposalSupported() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConcurrentImageCreationAndDisposalSupported() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GRect getScreenBounds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GPoint getMousePosInScreen() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InterfaceBwdFontHome getFontHome() {
        return this.fontHome;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected InterfaceBwdImage newImageImpl(
            String filePath,
            InterfaceBwdImageDisposalListener imageDisposalListener) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected void shutdownAbruptly_bindingSpecific() {
    }
}
