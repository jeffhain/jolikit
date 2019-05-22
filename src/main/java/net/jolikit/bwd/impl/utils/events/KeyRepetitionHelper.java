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
package net.jolikit.bwd.impl.utils.events;

import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.InterfaceBwdEventListener;
import net.jolikit.bwd.impl.utils.basics.InterfaceDoubleSupplier;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.sched.AbstractRepeatedProcess;
import net.jolikit.time.sched.InterfaceScheduler;

/**
 * Class to factor code related to blocking eventual keys repetitions
 * of backing libraries, and ensuring keys repetitions ourselves,
 * not to make AbstractBwdHost class too big.
 * Should not hurt to use it even if not needed.
 * 
 * If we want to be able to choose repetition frequency ourselves,
 * and if the backing library repeats both key press and release events,
 * then there must be a way to know whether backing events are repetitions,
 * and in these cases not call press (nor typed, if it's the same backing event)
 * and release methods, in which case repetition would occur at least at
 * the same rate than for the backing library.
 * 
 * Repetition is stopped on any key release, even if it's a modifier
 * (consistent with what JavaFX does, but not with AWT/Swing for example).
 */
public class KeyRepetitionHelper {
    
    /*
     * onXxx methods deliberately don't implement BWD event listener interface,
     * even though they happen to have identical signatures, to make it easier
     * to see where they are called.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private class MyKeyTypedRepetitionProcess extends AbstractRepeatedProcess {
        private boolean firstCall;
        private BwdKeyEventT repeatEvent;
        public MyKeyTypedRepetitionProcess(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        @Override
        protected void onBegin() {
            this.firstCall = true;
            this.repeatEvent = null;
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {

            if (DEBUG) {
                Dbg.log(
                        "repetitor ... : theoS = "
                                + NumbersUtils.toStringNoCSN(nsToS(theoreticalTimeNs))
                                + ", actualS = "
                                + NumbersUtils.toStringNoCSN(nsToS(actualTimeNs)));
            }

            final long nextNs;
            
            if (this.firstCall) {
                this.firstCall = false;
                final long delayNs = sToNsNoUnderflow(
                        keyRepetitionTriggerDelaySSupplier.get());
                nextNs = plusBounded(actualTimeNs, delayNs);
            } else {
                final BwdKeyEventT event = keyTyped_eventToBlockAndRepeat;
                if (event == null) {
                    // NB: Should never happen, but just in case.
                    this.stop();
                    return 0;
                }
                BwdKeyEventT repeatEvent = this.repeatEvent;
                if (repeatEvent == null) {
                    final boolean isRepeat = true;
                    repeatEvent = event.withIsRepeat(isRepeat);
                    this.repeatEvent = repeatEvent;
                }
                eventListener.onKeyTyped(repeatEvent);
                
                final long periodNs = TimeUtils.sToNsNoUnderflow(
                        keyRepetitionPeriodSSupplier.get());
                // Using theoretical time, for repetition speed to match user's
                // mental extrapolation.
                nextNs = plusBounded(theoreticalTimeNs, periodNs);
            }
            
            if (DEBUG) {
                Dbg.log("nextS = " + NumbersUtils.toStringNoCSN(nsToS(nextNs)));
            }
            
            return nextNs;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceDoubleSupplier keyRepetitionTriggerDelaySSupplier;
    private final InterfaceDoubleSupplier keyRepetitionPeriodSSupplier;
    
    private final InterfaceBwdEventListener eventListener;
    
    private BwdKeyEventPr keyPressed_eventToBlock = null;
    
    private BwdKeyEventT keyTyped_eventToBlockAndRepeat = null;
    private final MyKeyTypedRepetitionProcess keyTyped_repetitionProcess;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Using suppliers for config values, to use fresh values all the time
     * in case user wants to do fancy dynamic things, and to avoid
     * cyclic dependency with config class.
     */
    public KeyRepetitionHelper(
            InterfaceDoubleSupplier keyRepetitionTriggerDelaySSupplier,
            InterfaceDoubleSupplier keyRepetitionPeriodSSupplier,
            InterfaceScheduler scheduler,
            InterfaceBwdEventListener eventListener) {
        
        this.keyRepetitionTriggerDelaySSupplier =
                LangUtils.requireNonNull(keyRepetitionTriggerDelaySSupplier);
        
        this.keyRepetitionPeriodSSupplier =
                LangUtils.requireNonNull(keyRepetitionPeriodSSupplier);
        
        this.eventListener = LangUtils.requireNonNull(eventListener);
        
        this.keyTyped_repetitionProcess = new MyKeyTypedRepetitionProcess(
                scheduler);
    }

    /**
     * Should be called even in case it's known to be a repetition,
     * for behavioral homogeneity, because when a key is held down,
     * most libraries generate such an event just after focus gain
     * before the repeated key typed events.
     */
    public void onKeyPressed(BwdKeyEventPr event) {
        final BwdKeyEventPr eventToBlock = this.keyPressed_eventToBlock;
        if (eventToBlock != null) {
            if (haveSameKey(eventToBlock, event)) {
                // Ignoring, in case backing library does repeat these ones.
                return;
            } else {
                /*
                 * A new key has been pressed: stopping current repetition if any.
                 */
                this.keyTyped_repetitionProcess.stop();
                this.keyTyped_eventToBlockAndRepeat = null;
            }
        }
        this.keyPressed_eventToBlock = event;
        
        this.eventListener.onKeyPressed(event);
    }
    
    /**
     * Should be called even in case it's known to be a repetition,
     * because some libraries (such as jogl and qt4) indicate the
     * first event after focus gain as a repetition, and ignoring it
     * would prevent us from pursuing the repetition by starting
     * our own.
     */
    public void onKeyTyped(BwdKeyEventT event) {
        // Not true in case of multiple presses of a same key,
        // since we nullify the field on key release.
        final BwdKeyEventT eventToBlock = this.keyTyped_eventToBlockAndRepeat;
        if (DEBUG) {
            Dbg.log("eventToBlock = " + eventToBlock);
        }
        if (eventToBlock != null) {
            if (haveSameCodePoint(eventToBlock, event)) {
                // Ignoring, we do repetitions ourselves.
                return;
            }
        }
        
        this.keyTyped_eventToBlockAndRepeat = event;
        this.keyTyped_repetitionProcess.start();
        
        final boolean isRepeat = false;
        this.eventListener.onKeyTyped(event.withIsRepeat(isRepeat));
    }
    
    public void onKeyReleased(BwdKeyEventPr event) {
        this.keyTyped_repetitionProcess.stop();
        this.keyTyped_eventToBlockAndRepeat = null;
        
        final BwdKeyEventPr eventToBlock = this.keyPressed_eventToBlock;
        if (eventToBlock != null) {
            if (haveSameKey(eventToBlock, event)) {
                // Making sure we don't ignore next press of this key.
                this.keyPressed_eventToBlock = null;
            } else {
                // Another key has been released.
                // We might still have to block the repetition so we don't nullify.
            }
        }
        
        this.eventListener.onKeyReleased(event);
    }
    
    public void onWindowFocusLost() {
        if (DEBUG) {
            Dbg.log("repetitionHelper.onFocusLost()");
        }

        this.keyPressed_eventToBlock = null;
        
        this.keyTyped_repetitionProcess.stop();
        this.keyTyped_eventToBlockAndRepeat = null;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static boolean haveSameKey(BwdKeyEventPr event1, BwdKeyEventPr event2) {
        // NB: True if both are "unknown", but shouldn't hurt.
        return (event1.getKey() == event2.getKey());
    }
    
    private static boolean haveSameCodePoint(BwdKeyEventT event1, BwdKeyEventT event2) {
        return (event1.getCodePoint() == event2.getCodePoint());
    }
}
