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
package net.jolikit.bwd.test.utils;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdEvent;
import net.jolikit.bwd.api.events.BwdEventType;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.basics.InterfaceHostSupplier;
import net.jolikit.lang.LangUtils;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

public class BwdClientMock extends BwdClientEventListenerMock implements InterfaceBwdClient, InterfaceHostSupplier {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private InterfaceBwdHost host;
    
    /**
     * When set, checking that events occur in UI thread.
     */
    private InterfaceWorkerAwareScheduler uiThreadScheduler;
    
    /*
     * Call counts.
     */
    
    private int callCount_processEventualBufferedEvents;
    
    private int callCount_paint;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public BwdClientMock() {
    }
    
    /**
     * If called, on any event, check is done
     * that current thread is the UI thread.
     * 
     * @param uiThreadScheduler The UI thread scheduler.
     */
    public final void configureUiThreadCheck(InterfaceWorkerAwareScheduler uiThreadScheduler) {
        this.uiThreadScheduler = LangUtils.requireNonNull(uiThreadScheduler);
    }
    
    /*
     * 
     */
    
    @Override
    public InterfaceBwdHost getHost() {
        return this.host;
    }
    
    /*
     * Call counts.
     */

    public int getCallCount_processEventualBufferedEvents() {
        return this.callCount_processEventualBufferedEvents;
    }

    public int getCallCount_paint() {
        return this.callCount_paint;
    }

    /*
     * 
     */
    
    @Override
    public void setHost(Object host) {
        this.host = (InterfaceBwdHost) host;
    }

    @Override
    public void processEventualBufferedEvents() {
        this.callCount_processEventualBufferedEvents++;
    }
    
    @Override
    public List<GRect> paintClient(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        this.callCount_paint++;
        
        return this.paintClientImpl(
                g,
                dirtyRect);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void onAnyEvent(BwdEventType expectedEventType, BwdEvent event) {
        if (this.uiThreadScheduler != null) {
            this.uiThreadScheduler.checkIsWorkerThread();
        }
        super.onAnyEvent(expectedEventType, event);
    }
    
    /**
     * Override it to implement your painting.
     */
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
