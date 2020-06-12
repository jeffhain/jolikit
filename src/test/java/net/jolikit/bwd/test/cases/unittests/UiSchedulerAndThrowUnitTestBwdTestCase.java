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
package net.jolikit.bwd.test.cases.unittests;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractUnitTestBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.lang.Unchecked;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * To test that UI schedulers properly use the configured exception handler.
 */
public class UiSchedulerAndThrowUnitTestBwdTestCase extends AbstractUnitTestBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * Large period, so that we can run test for all bindings together without issue.
     */
    private static final double PERIOD_S = 1.0;

    /**
     * Large tolerance, so that we can run test for all bindings together without issue.
     */
    private static final double TOLERANCE_S = 0.2;
    
    private static final int LAST_STEP_INDEX = 3;

    private static final int INITIAL_WIDTH = 400;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * A Throwable, to check that everything up to Throwable is taken care of.
     */
    private static class MyThrowable extends Throwable {
        private static final long serialVersionUID = 1L;
        public MyThrowable() {
        }
    }
    
    private static class MyExceptionHandler implements UncaughtExceptionHandler {
        final List<Throwable> catchedList = new ArrayList<Throwable>();
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            if (DEBUG) {
                Dbg.log("uncaughtException(...) : ", throwable);
            }
            this.catchedList.add(throwable);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final MyExceptionHandler exceptionHandler = new MyExceptionHandler();
    
    private int nextStepIndex = 0;
    
    private boolean done = false;
    
    /**
     * To check that execute(Runnable) still works after the exception.
     */
    private double executeTheoreticalTimeS = Double.NaN;
    private double executeActualTimeS = Double.NaN;
    
    /**
     * To check that executeAtXxx(Runnable) still works after the exception.
     */
    private double executeAtTheoreticalTimeS = Double.NaN;
    private double executeAtActualTimeS = Double.NaN;
    
    private final List<MyThrowable> thrownList = new ArrayList<MyThrowable>();
    
    private final List<String> errorList = new ArrayList<String>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public UiSchedulerAndThrowUnitTestBwdTestCase() {
    }
    
    public UiSchedulerAndThrowUnitTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new UiSchedulerAndThrowUnitTestBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new UiSchedulerAndThrowUnitTestBwdTestCase(this.getBinding());
    }

    @Override
    public UncaughtExceptionHandler getExceptionHandlerElseNull() {
        return this.exceptionHandler;
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
    protected void onTestEnd() {
        this.done = true;
    }
    
    @Override
    protected long testSome(long nowNs) {
        
        final double nowS = TimeUtils.nsToS(nowNs);
        
        final int stepIndex = this.nextStepIndex++;
        
        final InterfaceBwdBinding binding = this.getBinding();
        final InterfaceScheduler scheduler = binding.getUiThreadScheduler();
        
        final boolean isLastStep = (stepIndex == LAST_STEP_INDEX);
        
        if (stepIndex == 0) {
            /*
             * To test execution time and throwable catching for execute(...).
             */
            this.executeTheoreticalTimeS = nowS;
            if (DEBUG) {
                Dbg.log("execute(...) : calling");
            }
            scheduler.execute(new Runnable() {
                @Override
                public void run() {
                    executeActualTimeS = scheduler.getClock().getTimeS();
                    final MyThrowable t = new MyThrowable();
                    thrownList.add(t);
                    if (DEBUG) {
                        Dbg.log("execute(...) : throwing");
                    }
                    Unchecked.throwIt(t);
                }
            });
        } else if (stepIndex == 1) {
            if (DEBUG) {
                Dbg.log("execute(...) : checking");
            }
            
            final boolean ok = (Math.abs(this.executeActualTimeS - this.executeTheoreticalTimeS) < TOLERANCE_S);
            if (!ok) {
                this.onError(
                        "ERROR : execute(...) : expected time = " + this.executeTheoreticalTimeS + " s"
                        + ", actual time = " + this.executeActualTimeS + " s");
            }
            
            final MyThrowable lastThrown = getLast(this.thrownList);
            final Throwable lastCatched = getLast(this.exceptionHandler.catchedList);
            if (lastCatched != lastThrown) {
                this.onError(
                        "ERROR : last thrown = " + getSimpleName(lastThrown)
                        + ", last catched = " + getSimpleName(lastCatched));
            }
            
        } else if (stepIndex == 2) {
            /*
             * To test execution time and throwable catching for executeAtS(...).
             */
            final double delayS = PERIOD_S / 2;
            this.executeAtTheoreticalTimeS = (nowS + delayS);
            if (DEBUG) {
                Dbg.log("executeAtS(...) : calling, delay = " + delayS + " s");
            }
            scheduler.executeAtS(new Runnable() {
                @Override
                public void run() {
                    executeAtActualTimeS = scheduler.getClock().getTimeS();
                    final MyThrowable t = new MyThrowable();
                    thrownList.add(t);
                    if (DEBUG) {
                        Dbg.log("executeAfterS(...) : throwing");
                    }
                    Unchecked.throwIt(t);
                }
            }, this.executeAtTheoreticalTimeS);
        } else if (stepIndex == 3) {
            if (DEBUG) {
                Dbg.log("executeAtS(...) : checking");
            }
            
            final boolean ok = (Math.abs(this.executeAtActualTimeS - this.executeAtTheoreticalTimeS) < TOLERANCE_S);
            if (!ok) {
                this.onError(
                        "ERROR : executeAtS(...) : expected time = " + this.executeAtTheoreticalTimeS + " s"
                        + ", actual time = " + this.executeAtActualTimeS + " s");
            }

            final MyThrowable lastThrown = getLast(this.thrownList);
            final Throwable lastCatched = getLast(this.exceptionHandler.catchedList);
            if (lastCatched != lastThrown) {
                this.onError(
                        "ERROR : last thrown = " + getSimpleName(lastThrown)
                        + ", last catched = " + getSimpleName(lastCatched));
            }
        } else {
            /*
             * No more test.
             */
        }
        
        /*
         * 
         */
        
        this.getHost().ensurePendingClientPainting();

        if (isLastStep) {
            return Long.MAX_VALUE;
        } else {
            return NumbersUtils.plusBounded(nowNs, TimeUtils.sToNs(PERIOD_S));
        }
    }

    @Override
    protected void drawCurrentState(InterfaceBwdGraphics g) {
        
        final GRect box = g.getBox();
        final int x = box.x();
        final int y = box.y();

        g.setColor(BwdColor.WHITE);
        g.fillRect(box);

        final int dh = g.getFont().fontMetrics().fontHeight() + 1;
        int textX = x + 10;
        int textY = y + 10;
        
        g.setColor(BwdColor.BLACK);
        if (this.done) {
            g.drawText(textX, textY, "Done.");
        } else {
            final int stepNum = this.nextStepIndex;
            final int stepNbr = LAST_STEP_INDEX + 1;
            g.drawText(textX, textY, "Testing... (last step done = " + stepNum + "/" + stepNbr + ")");
        }
        textY += dh;
        
        for (String errorMsg : this.errorList) {
            g.drawText(textX, textY, errorMsg);
            textY += dh;
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static String getSimpleName(Object o) {
        if (o == null) {
            return "null";
        }
        return o.getClass().getSimpleName();
    }
    
    private static <T> T getLast(List<T> list) {
        final int size = list.size();
        if (size == 0) {
            return null;
        }
        return list.get(size - 1);
    }
    
    private void onError(String errorMsg) {
        Dbg.log("ERROR : " + errorMsg);
        this.errorList.add(errorMsg);
    }
}
