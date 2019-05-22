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
package net.jolikit.bwd.test.cases.utils;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.lang.Dbg;
import net.jolikit.time.sched.AbstractRepeatedProcess;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * Optional abstract class for unit tests (programmatic testings),
 * so as to execute the test by small steps with the possibility
 * to display current state (progress, report) in the client area.
 * 
 * Test execution is NOT tied with painting, because it might be blocked
 * when another window is maximized, when using some libraries (such as SWT).
 * 
 * Also suited for benches, by benching in test method, which obviously blocks
 * rendering for the time of the bench since we use UI thread.
 */
public abstract class AbstractUnitTestBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * Making sure initial state is actually rendered
     * before we block the UI thread for some tests.
     */
    private static final int NBR_OF_INIT_PAINT = 10;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyTestProcess extends AbstractRepeatedProcess {
        public MyTestProcess(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        @Override
        protected void onBegin() {
            onTestBegin();
        }
        @Override
        protected void onEnd() {
            onTestEnd();
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {
            final long nowNs = actualTimeNs;
            
            final long nextTheoreticalTimeNs = testSome(nowNs);
            
            if (nextTheoreticalTimeNs == Long.MAX_VALUE) {
                this.stop();
                return 0;
            } else {
                return nextTheoreticalTimeNs;
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final MyTestProcess testProcess;
    
    private int paintCallCount = 0;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractUnitTestBwdTestCase() {
        this.testProcess = null;
    }
    
    public AbstractUnitTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
        this.testProcess = new MyTestProcess(binding.getUiThreadScheduler());
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    protected void onTestBegin() {
    }

    protected void onTestEnd() {
    }
    
    /**
     * Must take care of ensuring client paint when needed
     * (typically at each call, unless nothing was done).
     * 
     * @param nowNs Time, in nanoseconds, and in UI scheduler's clock's
     *        time reference, at which this method is called.
     * @return Next theoretical time, in nanoseconds, at which this method
     *         is to be called again. Long.MAX_VALUE means testing must stop.
     */
    protected abstract long testSome(long nowNs);

    /**
     * Called at each painting.
     */
    protected abstract void drawCurrentState(InterfaceBwdGraphics g);

    /*
     * 
     */
    
    /**
     * Used to clear client area before each drawing.
     */
    protected BwdColor getClearColor() {
        return BwdColor.WHITE;
    }

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        this.paintCallCount++;
        if (DEBUG) {
            Dbg.log("paint_initDone(...) : paintCallCount = " + this.paintCallCount);
        }
        
        // For tests that test initial color.
        final BwdColor initialColor = g.getColor();
        
        final GRect box = g.getBoxInClient();
        
        g.setColor(this.getClearColor());
        g.clearRectOpaque(box);
        
        g.setColor(initialColor);
        
        if (this.paintCallCount < NBR_OF_INIT_PAINT) {
            this.getHost().ensurePendingClientPainting();
        } else if (this.paintCallCount == NBR_OF_INIT_PAINT) {
            this.testProcess.start();
        }
        
        this.drawCurrentState(g);
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
