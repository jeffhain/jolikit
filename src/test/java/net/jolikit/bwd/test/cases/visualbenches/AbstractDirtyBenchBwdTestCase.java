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
package net.jolikit.bwd.test.cases.visualbenches;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.time.TimeUtils;

/**
 * To bench repaints (first painting not taken into account,
 * for might be slow due to class load etc.).
 * 
 * Example with nbrOfRepaint = 3:
 * - painting 1
 *   computes start time
 * - painting 2
 * - painting 3
 * - painting 4
 *   computes end time
 * 
 * Note that PPS (Paintings Per Second) might be bound by configuration,
 * even though each dirty painting could be cheap on CPU.
 */
public abstract class AbstractDirtyBenchBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final int nbrOfRepaint;
    
    /*
     * For client mock.
     */
    
    private final CountDownLatch latch;
    
    private long startTimeNs;
    private long endTimeNs;
    
    private int paintCounter = 0;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Constructor for instance as BWD mock.
     * 
     * @param nbrOfRepaint Must be >= 0.
     */
    public AbstractDirtyBenchBwdTestCase(int nbrOfRepaint) {
        if (nbrOfRepaint < 0) {
            throw new IllegalArgumentException("" + nbrOfRepaint);
        }
        this.nbrOfRepaint = nbrOfRepaint;
        this.latch = null;
    }

    /**
     * Constructor for instance as client mock.
     * 
     * Must set host before use (of course, since it's a client!).
     */
    public AbstractDirtyBenchBwdTestCase(
            int nbrOfRepaint,
            InterfaceBwdBinding binding) {
        super(binding);
        this.nbrOfRepaint = nbrOfRepaint;
        this.latch = new CountDownLatch(1);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    /**
     * 0 during first painting, 1 for the next, etc.
     */
    protected int getPaintIndex() {
        return this.paintCounter - 1;
    }

    /**
     * Do your painting here.
     * @param nextDirtyRect (out) Put next dirty rectangle in first slot.
     * @return Painted rect list.
     */
    protected abstract List<GRect> paint_initDone_2(
            InterfaceBwdGraphics g,
            GRect dirtyRect,
            GRect[] nextDirtyRect);
    
    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        this.paintCounter++;
        
        final GRect[] nextDirtyRect = new GRect[1];
        
        final List<GRect> paintedRectList = this.paint_initDone_2(
                g,
                dirtyRect,
                nextDirtyRect);

        this.afterPaint(nextDirtyRect[0]);
        
        return paintedRectList;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void afterPaint(GRect nextDirtyRect) {
        final int repaintCounter = this.paintCounter - 1;
        if (repaintCounter < this.nbrOfRepaint) {
            if (DEBUG) {
                Dbg.log("gogogo : paintCounter = " + paintCounter);
            }
            if (this.paintCounter == 1) {
                this.startTimeNs = System.nanoTime();
            }
            // Painting again ASAP.
            this.getHost().makeDirtyAndEnsurePendingClientPainting(nextDirtyRect);
            
        } else {
            this.endTimeNs = System.nanoTime();
            final long durationNs = this.endTimeNs - this.startTimeNs;
            final double durationS = TimeUtils.nsToS(durationNs);
            final double meanDurationS = durationS / this.nbrOfRepaint;
            
            if (DEBUG) {
                Dbg.log(
                        this.nbrOfRepaint + " repaints, "
                                + this.getBinding().getClass().getSimpleName()
                                + ", took " + NumbersUtils.toStringNoCSN(durationS) + " s, mean = "
                                + NumbersUtils.toStringNoCSN(meanDurationS) + " s");
                Dbg.flush();
            }

            this.latch.countDown();

            this.getBinding().shutdownAbruptly();
        }
    }
}
