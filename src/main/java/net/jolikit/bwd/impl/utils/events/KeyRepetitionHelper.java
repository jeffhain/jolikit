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
package net.jolikit.bwd.impl.utils.events;

import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.InterfaceBwdEventListener;
import net.jolikit.bwd.impl.utils.basics.InterfaceDoubleSupplier;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.time.sched.AbstractProcess;
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
 * and release methods, else repetition would occur at least at the same rate
 * as for the backing library.
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

    /**
     * Repeats both key pressed and key types events,
     * but in a specific process() call for each,
     * to avoid try/finally and allow exception eventually thrown
     * by each call to reach proper exception handler if any.
     */
    private class MyRepetitionProcess extends AbstractProcess {
        private boolean firstCall;
        private BwdKeyEventPr repeatEventP;
        private BwdKeyEventT repeatEventT;
        public MyRepetitionProcess(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        @Override
        protected void onBegin() {
            this.firstCall = true;
            this.repeatEventP = null;
            this.repeatEventT = null;
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {

            if (DEBUG) {
                Dbg.log(
                        "repetitor ... : theoS = "
                                + NbrsUtils.toStringNoCSN(nsToS(theoreticalTimeNs))
                                + ", actualS = "
                                + NbrsUtils.toStringNoCSN(nsToS(actualTimeNs)));
            }

            final long delayNs;
            if (this.firstCall) {
                this.firstCall = false;
                delayNs = sToNsNoUnderflow(
                        repetitionTriggerDelaySSupplier.get());
            } else {
                /*
                 * Handling the case where key pressed to repeat is null,
                 * even if it should not occur in practice
                 * (bindings generating key pressed events before key typed),
                 * for symmetry, safety, and in case it does occur.
                 */
                final BwdKeyEventPr eventP = keyPressedToRepeat;
                final BwdKeyEventT eventT = keyTypedToRepeat;
                if ((eventP == null)
                    && (eventT == null)) {
                    // Nothing to repeat.
                    this.stop();
                    return 0;
                }
                
                BwdKeyEventPr repeatEventP = null;
                if (eventP != null) {
                    repeatEventP = this.repeatEventP;
                    if (repeatEventP == null) {
                        repeatEventP = eventP.withIsRepeat(true);
                        this.repeatEventP = repeatEventP;
                    }
                }
                BwdKeyEventT repeatEventT = null;
                if (eventT != null) {
                    repeatEventT = this.repeatEventT;
                    if (repeatEventT == null) {
                        repeatEventT = eventT.withIsRepeat(true);
                        this.repeatEventT = repeatEventT;
                    }
                }
                
                /*
                 * If any listener call throws, the repetition stops.
                 */

                if (repeatEventP != null) {
                    eventListener.onKeyPressed(repeatEventP);
                }
                if (repeatEventT != null) {
                    eventListener.onKeyTyped(repeatEventT);
                }
                
                delayNs = sToNsNoUnderflow(
                    repetitionPeriodSSupplier.get());
            }
            
            /*
             * Using theoretical time as reference,
             * for repetition speed to match user's mental extrapolation,
             * but still taking care not to schedule in the past,
             * to avoid lateness drift that could prevent other repetitions
             * and overbusiness once we can keep up again.
             */
            final long nextNs = Math.max(
                plusBounded(theoreticalTimeNs, delayNs),
                actualTimeNs);
            if (DEBUG) {
                Dbg.log("nextS = " + NbrsUtils.toStringNoCSN(nsToS(nextNs)));
            }
            return nextNs;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceDoubleSupplier repetitionTriggerDelaySSupplier;
    private final InterfaceDoubleSupplier repetitionPeriodSSupplier;
    
    private final InterfaceBwdEventListener eventListener;
    
    private BwdKeyEventPr keyPressedToRepeat = null;
    private BwdKeyEventT keyTypedToRepeat = null;
    private final MyRepetitionProcess repetitionProcess;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Using suppliers for config values, to use fresh values all the time
     * in case user wants to do fancy dynamic things, and to avoid
     * cyclic dependency with config class.
     */
    public KeyRepetitionHelper(
            InterfaceDoubleSupplier repetitionTriggerDelaySSupplier,
            InterfaceDoubleSupplier repetitionPeriodSSupplier,
            InterfaceScheduler scheduler,
            InterfaceBwdEventListener eventListener) {
        
        this.repetitionTriggerDelaySSupplier =
                LangUtils.requireNonNull(repetitionTriggerDelaySSupplier);
        
        this.repetitionPeriodSSupplier =
                LangUtils.requireNonNull(repetitionPeriodSSupplier);
        
        this.eventListener = LangUtils.requireNonNull(eventListener);
        
        this.repetitionProcess = new MyRepetitionProcess(scheduler);
    }

    /**
     * Should be called even in case it's known to be a repetition,
     * because some libraries (such as jogl and qt4) indicate the
     * first event after focus gain as a repetition.
     */
    public void onKeyPressed(BwdKeyEventPr event) {
        final BwdKeyEventPr eventToRepeat = this.keyPressedToRepeat;
        if (DEBUG) {
            Dbg.log("keyPressedToRepeat = " + eventToRepeat);
        }
        if (eventToRepeat != null) {
            if (haveSameKeyAndLocation(eventToRepeat, event)) {
                // Ignoring, we do repetitions ourselves.
                return;
            } else {
                /*
                 * A new key has been pressed.
                 * No need to stop repetition
                 * due to following start.
                 */
            }
        }
        
        this.startKeyPressedRepetition(event);
        
        final boolean isRepeat = false;
        this.eventListener.onKeyPressed(event.withIsRepeat(isRepeat));
    }
    
    /**
     * Should be called even in case it's known to be a repetition,
     * because some libraries (such as jogl and qt4) indicate the
     * first event after focus gain as a repetition.
     */
    public void onKeyTyped(BwdKeyEventT event) {
        // Not true in case of multiple presses of a same key,
        // since we nullify the field on key release.
        final BwdKeyEventT eventToRepeat = this.keyTypedToRepeat;
        if (DEBUG) {
            Dbg.log("keyTypedToRepeat = " + eventToRepeat);
        }
        if (eventToRepeat != null) {
            if (haveSameCodePoint(eventToRepeat, event)) {
                // Ignoring, we do repetitions ourselves.
                return;
            } else {
                /*
                 * A new key has been typed.
                 */
                this.stopAndClearRepetition();
            }
        }
        
        /*
         * If key pressed is being repeated,
         * we don't want to stop it.
         */
        this.ensureKeyTypedRepetition(event);
        
        final boolean isRepeat = false;
        this.eventListener.onKeyTyped(event.withIsRepeat(isRepeat));
    }
    
    public void onKeyReleased(BwdKeyEventPr event) {
        this.stopAndClearRepetition();
        
        this.eventListener.onKeyReleased(event);
    }
    
    public void onWindowFocusLost() {
        if (DEBUG) {
            Dbg.log("repetitionHelper.onFocusLost()");
        }

        this.stopAndClearRepetition();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void startKeyPressedRepetition(BwdKeyEventPr event) {
        if (DEBUG) {
            Dbg.log("startKeyPressedRepetition(" + event + ")");
        }
        this.keyPressedToRepeat = event;
        /*
         * Starting key pressed repetition
         * resets key typed repetition.
         */
        this.keyTypedToRepeat = null;
        this.repetitionProcess.start();
    }
    
    private void ensureKeyTypedRepetition(BwdKeyEventT event) {
        if (DEBUG) {
            Dbg.log("ensureKeyTypedRepetition(" + event + ")");
        }
        this.keyTypedToRepeat = event;
        if (!this.repetitionProcess.isStarted()) {
            this.repetitionProcess.start();
        }
    }
    
    private void stopAndClearRepetition() {
        if (DEBUG) {
            Dbg.log("stopAndClearRepetition()");
        }
        this.repetitionProcess.stop();
        this.keyPressedToRepeat = null;
        this.keyTypedToRepeat = null;
    }
    
    private static boolean haveSameKeyAndLocation(
        BwdKeyEventPr event1,
        BwdKeyEventPr event2) {
        /*
         * NB: True if both are "unknown", but shouldn't hurt.
         */
        return (event1.getKey() == event2.getKey())
            && (event1.getKeyLocation() == event2.getKeyLocation());
    }
    
    private static boolean haveSameCodePoint(BwdKeyEventT event1, BwdKeyEventT event2) {
        return (event1.getCodePoint() == event2.getCodePoint());
    }
}
