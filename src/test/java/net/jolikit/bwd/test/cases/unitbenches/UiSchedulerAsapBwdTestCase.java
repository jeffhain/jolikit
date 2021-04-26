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
package net.jolikit.bwd.test.cases.unitbenches;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * Doesn't draw much, for the actual purpose of this mock
 * is to play with UI scheduler, and check:
 * - That its ASAP schedules are quickly executed
 *   (i.e. no under-the-hood polling).
 * - That its ASAP schedules don't have priority over timed schedules.
 * 
 * What we do it have a runnable re-schedule itself for ASAP systematically,
 * to measure the time it takes, and a treatment that periodically draws
 * the last measure, which allows to check that ASAP schedules don't have priority
 * else it would never execute.
 * 
 * Measured mean ASAP schedules latency by scheduler, in seconds:
 * On Windows7:
 * - qtj:   0.00002 (Using QCoreApplication.invokeLater(...))
 * - jfx:   0.00001 (Using Platform.runLater(...))
 * - jogl:  0.000007 (Using EDTUtil.invoke(...))
 * - lwjgl: 0.0000045 (Using GLFW.glfwPostEmptyEvent())
 * - awt:   0.0000016 (Using EventQueue.invokeLater(...))
 * - swing: 0.0000016 (Using EventQueue.invokeLater(...))
 * - swt:   0.0000015 (Using Display.asyncExec(...))
 * - algr:  0.0000001 (Using HardScheduler.execute(...))
 * - sdl:   0.0000001 (Using HardScheduler.execute(...))
 * On Mac:
 * - qtj:   0.000054
 * - lwjgl: 0.000053
 * - swt:   0.000043
 * - jfx:   0.000033
 * - jogl:  0.000016
 * - awt:   0.0000026
 * - swing: 0.0000026
 * - sdl:   0.00000018
 * - algr:  0.00000016
 */
public class UiSchedulerAsapBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 50;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    private static final double DRAW_UPDATE_PERIOD_S = 1.0;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * In case painting is called concurrently due to some bug.
     */
    private final AtomicBoolean started = new AtomicBoolean();
    
    private double meanLatencyS;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public UiSchedulerAsapBwdTestCase() {
    }
    
    public UiSchedulerAsapBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new UiSchedulerAsapBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new UiSchedulerAsapBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    /*
     * 
     */
    
    @Override
    public void onWindowClosed(BwdWindowEvent event) {
        this.getBinding().shutdownAbruptly();
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final GRect box = g.getBox();
        final int x = box.x();
        final int y = box.y();

        g.setColor(BwdColor.WHITE);
        g.fillRect(box);

        g.setColor(BwdColor.BLACK);
        g.drawText(x, y, "meanLatencyS = " + meanLatencyS);
        if (DEBUG) {
            Dbg.log("meanLatencyS = " + meanLatencyS);
        }
        
        /*
         * 
         */
        
        if (started.compareAndSet(false, true)) {
            final InterfaceScheduler scheduler = this.getBinding().getUiThreadScheduler();
            final Runnable runnable = new Runnable() {
                long lastCallSnt = Long.MIN_VALUE;
                long counter = 0;
                long sumNs = 0;
                @Override
                public void run() {
                    final long snt = System.nanoTime();
                    if (this.lastCallSnt != Long.MIN_VALUE) {
                        this.counter++;
                        
                        final long dtNs = snt - this.lastCallSnt;
                        this.sumNs += dtNs;
                        meanLatencyS = TimeUtils.nsToS(this.sumNs) / (double) this.counter;
                    }
                    
                    // Re-executing self ASAP, with ASAP method.
                    this.lastCallSnt = snt;
                    scheduler.execute(this);
                }
            };
            scheduler.execute(runnable);
            
            final AbstractProcess process = new AbstractProcess(scheduler) {
                @Override
                protected long process(long theoreticalTimeNs, long actualTimeNs) {
                    getHost().ensurePendingClientPainting();
                    return theoreticalTimeNs + sToNs(DRAW_UPDATE_PERIOD_S);
                }
            };
            process.start();
        }
        
        return null;
    }
}
