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
package net.jolikit.bwd.test.cases.unitbenches;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.sched.AbstractRepeatedProcess;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * Doesn't draw much, for the actual purpose of this mock
 * is to play with UI scheduler, and check that theoretical time
 * is properly preserved until actual execution.
 * 
 * Measured mean timed schedules latency by scheduler, in seconds:
 * On Windows7:
 * - qtj:   0.000041
 * - jfx:   0.00002
 * - lwjgl: 0.000014
 * - jogl:  0.000011
 * - awt:   0.000015
 * - swing: 0.000015
 * - swt:   0.000013
 * - algr:  0.0000019
 * - sdl:   0.0000019
 * On Mac:
 * - lwjgl: 0.00012
 * - qtj:   0.000091
 * - swt:   0.00008
 * - jfx:   0.000059
 * - jogl:  0.000024
 * - awt:   0.000019
 * - swing: 0.000019
 * - algr:  0.00000034
 * - sdl:   0.00000028
 */
public class UiSchedulerTimedBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 100;
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
    
    private long lastExpectedTheoNs = Long.MIN_VALUE;
    private long lastActualTheoNs;
    private double lastLatenessS;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public UiSchedulerTimedBwdTestCase() {
    }
    
    public UiSchedulerTimedBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new UiSchedulerTimedBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new UiSchedulerTimedBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final GRect box = g.getBoxInClient();
        final int x = box.x();
        final int y = box.y();

        g.setColor(BwdColor.WHITE);
        g.fillRect(box);
        
        final InterfaceBwdFont font = g.getFont();
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        final int dh = metrics.fontHeight() + 1;
        
        int textY = y;
        
        g.setColor(BwdColor.BLACK);
        g.drawText(x, textY, "meanLatencyS = " + meanLatencyS);
        textY += dh;
        if (DEBUG) {
            Dbg.log("meanLatencyS = " + meanLatencyS);
        }
        
        if (this.lastExpectedTheoNs != Long.MIN_VALUE) {
            g.drawText(x, textY, "lastExpectedTheoNs = " + lastExpectedTheoNs);
            textY += dh;
            g.drawText(x, textY, "lastActualTheoNs =   " + lastActualTheoNs);
            textY += dh;
            g.drawText(x, textY, "lastLatenessS = " + lastLatenessS);
            textY += dh;
            if (DEBUG) {
                Dbg.log("lastExpectedTheoNs = " + lastExpectedTheoNs);
                Dbg.log("lastActualTheoNs =   " + lastActualTheoNs);
                Dbg.log("lastLatenessS = " + lastLatenessS);
            }
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
                /**
                 * If we only use timed schedules in the past,
                 * and the scheduler gives priority to timed schedules,
                 * ASAP schedules are never executed.
                 * To avoid this issue, sometimes we re-schedule
                 * ourselves with an ASAP schedule instead.
                 */
                boolean flipFlop = false;
                @Override
                public void run() {
                    final long snt = System.nanoTime();
                    if (this.lastCallSnt != Long.MIN_VALUE) {
                        this.counter++;
                        
                        final long dtNs = snt - this.lastCallSnt;
                        this.sumNs += dtNs;
                        meanLatencyS = TimeUtils.nsToS(this.sumNs) / (double) this.counter;
                    }
                    
                    this.flipFlop = !this.flipFlop;
                    
                    if (this.flipFlop) {
                        // Re-executing self ASAP, with timed method.
                        this.lastCallSnt = snt;
                        final long onceUponATimeNs = Long.MIN_VALUE;
                        scheduler.executeAtNs(this, onceUponATimeNs);
                    } else {
                        scheduler.execute(this);
                    }
                }
            };
            scheduler.execute(runnable);
            
            final AbstractRepeatedProcess repeatedProcess = new AbstractRepeatedProcess(scheduler) {
                private long expectedTheoNs = Long.MIN_VALUE;
                @Override
                protected long process(long theoreticalTimeNs, long actualTimeNs) {
                    if (this.expectedTheoNs != Long.MIN_VALUE) {
                        lastExpectedTheoNs = this.expectedTheoNs;
                        lastActualTheoNs = theoreticalTimeNs;
                        lastLatenessS = nsToS(actualTimeNs - theoreticalTimeNs);
                        
                        // Throwing after lastXxx setting,
                        // to see them in client.
                        if (theoreticalTimeNs != this.expectedTheoNs) {
                            throw new AssertionError(theoreticalTimeNs + " != " + this.expectedTheoNs);
                        }
                    }
                    
                    final long nextTheoNs = theoreticalTimeNs + sToNs(DRAW_UPDATE_PERIOD_S);
                    this.expectedTheoNs = nextTheoNs;
                    
                    getHost().ensurePendingClientPainting();
                    return nextTheoNs;
                }
            };
            repeatedProcess.start();
        }
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
