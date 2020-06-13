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
package net.jolikit.bwd.impl.utils;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdEventListenerUtils;
import net.jolikit.bwd.api.events.BwdEventType;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.events.ThrowingBwdEventListener;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.InterfaceDoubleSupplier;
import net.jolikit.bwd.impl.utils.events.KeyRepetitionHelper;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.Unchecked;
import net.jolikit.time.clocks.InterfaceClock;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

/**
 * Optional abstract class for hosts implementations.
 * 
 * Initially, for each host implementation, I tried to use backing hosts
 * states and events as much as possible, and only do workarounds or add
 * complementary treatments (state, polling, etc.) when required.
 * That resulted into much heterogeneous specific code across implementations,
 * because no two libraries handle windowing the same way (some events not being
 * available, or being fired in different orders, some states never being
 * readable, or only readable depending on other states, etc.).
 * This code was also brittle, since the behavior of a library typically changes
 * depending on the platform or from a version to the next.
 * The finally adopted approach is to implement our public API with generic code
 * relying on an internal and minimal API to the backing library (typically
 * abstract methods), that puts the least pressure on it being complete and
 * consistent etc.
 * NB: This remark also applies, to a lesser extend, to other parts (than hosts)
 * of bindings implementations (such as graphics or font homes).
 */
public abstract class AbstractBwdHost implements InterfaceBwdHost, InterfaceBackingWindowHolder {
    
    /*
     * TODO What the messiness of this class shows,
     * is that getting even simple features to work properly with even
     * well-known libraries is either quite hairy or not even possible.
     * Due to this state of affairs, contemplating the following ideas:
     * - Remove maximize/demaximize from the API.
     * - Implement maximize/demaximize not with backing libraries,
     *   but with a boolean and stored maximized/demaximized bounds
     *   in host, using screen bounds as target bounds on maximize.
     *   But that might not play nicely with maximization/demaximization
     *   from native decorations.
     * - Create a binding, or binding configuration, allowing to use a single
     *   backing window as playground ("screen"), and have each
     *   host have its own synthetic window in it.
     *   Would be library-independent, much faster (without all the weird
     *   latencies ("animations") of actual windowing libraries),
     *   and much simpler to use and to test (in unit test thread,
     *   and no weird issues to work around).
     */
    
    /*
     * If a backing event occurs before our conditions are met, such as focus
     * gained before showing, we don't try to enforce the required state, as
     * it might not work, or not synchronously, and we don't force-fire the
     * corresponding event, as it might be inconsistent with backing state.
     * Instead, we just ignore it until a more consistent backing state becomes
     * visible to our polling.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * Not private but protected, for use by hosts implementations,
     * because it's used in hostLog(...) to get rid of dependency to Dbg,
     * and having DEBUG true only in concrete classes would make logs
     * suprisingly not appear.
     * Also it makes it easier to enable/disable debug logs for hosts.
     * Same for DEBUG_SPAM, for consistency.
     */
    protected static final boolean DEBUG = false;
    protected static final boolean DEBUG_SPAM = DEBUG && false;

    /*
     * Static config.
     * 
     * Some are hacks needed for all or almost all (OS, library) combinations,
     * and that should not hurt when not needed, and that we prefer
     * to always do for consistency across (OS, library) combinations,
     * so we just have them configured here statically, to keep
     * dynamic config simple.
     */
    
    /**
     * If already in state, could cause much useless and possibly
     * messily harmful actions, such as deiconification while
     * already deiconified causing loss or maximized state
     * (such as on Mac with SDL), etc.
     */
    private static final boolean MUST_ABORT_PROG_MOD_IF_ALREADY_IN_STATE = true;
    
    /**
     * True to run event logic synchronously just after
     * programmatic window state modifications.
     * 
     * Can help speed things up, and should not hurt.
     */
    private static final boolean MUST_RUN_EVENT_LOGIC_AFTER_PROG_BACK_MOD = true;
    
    /**
     * Can often be useful, in particular if not ignoring
     * ico or deico or max or demax while unstable.
     * 
     * TODO Since MUST_IGNORE_BACKING_XXX_WHILE_UNSTABLE are true,
     * could maybe set this to false.
     */
    private static final boolean MUST_BLOCK_FLICKERING_FOR_ICO_DEICO_MAX_DEMAX = true;

    /**
     * Can be useful not to fire ICONIFIED/DEICONIFIED/ICONIFIED
     * on iconification, some libraries shortly indicating window
     * as iconified at iconification start (and then as deiconified,
     * and finally as iconified once iconification is done).
     */
    private static final boolean MUST_IGNORE_BACKING_ICO_WHILE_UNSTABLE = true;
    
    /**
     * Can be useful not to fire DEICONIFIED on hiding,
     * some libraries indicating window as deiconified on hiding,
     * and as hidden only later.
     */
    private static final boolean MUST_IGNORE_BACKING_DEICO_WHILE_UNSTABLE = true;
    
    /**
     * Can be useful not to fire MAXIMIZED/DEMAXIMIZED/MAXIMIZED
     * on maximization, some libraries shortly indicating window
     * as maximized at maximization start (and then as demaximized,
     * and finally as maximized once maximization is done).
     */
    private static final boolean MUST_IGNORE_BACKING_MAX_WHILE_UNSTABLE = true;

    /**
     * Can be useful not to fire DEMAXIMIZED on iconification,
     * some libraries indicating window as demaximized on iconification,
     * and as iconified only later after possibly many MOVED/RESIZED events.
     * 
     * Can also be useful not to fire DEMAXIMIZED on hiding,
     * some libraries indicating window as demaximized on hiding
     * and as hidden only later.
     */
    private static final boolean MUST_IGNORE_BACKING_DEMAX_WHILE_UNSTABLE = true;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * Keeps track of window events that are send to the client,
     * to update our internal state accordingly, and eventually
     * schedule some related state modifications.
     */
    private class MyClientWindowEventWrapper extends ThrowingBwdEventListener {
        public MyClientWindowEventWrapper() {
        }
        @Override
        public void onWindowShown(BwdWindowEvent event) {
            /*
             * This state change should only be due to a call to show(),
             * so not bothering to ensure deiconified and to request focus,
             * which we already do in show().
             */
            mustTryToRestoreClientMaxStateIntoHost =
                    (!clientWrapper.isIconified())
                    && bindingConfig.getMustRestoreMaximizedStateWhenBackToShowingAndDeiconified();
            if (!clientWrapper.isIconified()) {
                if (clientWrapper.isMaximized()) {
                    ensureBoundsEnforcingFlagOnShowDeicoMaxIfConfigured();
                } else {
                    ensureBoundsEnforcingFlagOnShowDeicoDemaxIfConfiguredAndHaveAny();
                }
            }
            //
            resetEventFlagsOnFiring(BwdEventType.WINDOW_SHOWN);
            try {
                beforeClientWindowShown_spe(event);
            } finally {
                clientWrapper.onWindowShown(event);
            }
        }
        @Override
        public void onWindowHidden(BwdWindowEvent event) {
            cancelDragIfDragPressDetected();
            //
            resetEventFlagsOnFiring(BwdEventType.WINDOW_HIDDEN);
            try {
                beforeClientWindowHidden_spe(event);
            } finally {
                clientWrapper.onWindowHidden(event);
            }
        }
        @Override
        public void onWindowFocusGained(BwdWindowEvent event) {
            // Programmatic focus gain request doesn't cause state flickering,
            // but backing focus gained event might be a sign of state flickering.
            setNewestPossibleUnstabilityTime();
            makeAllDirtyAndEnsurePendingClientPainting();
            hostOfFocusedClientHolder.setHostOfFocusedClient(AbstractBwdHost.this);
            //
            try {
                beforeClientWindowFocusGained_spe(event);
            } finally {
                clientWrapper.onWindowFocusGained(event);
            }
        }
        @Override
        public void onWindowFocusLost(BwdWindowEvent event) {
            // Backing focus lost event might be a sign of state flickering.
            setNewestPossibleUnstabilityTime();
            // Don't bothering with repaint if closing.
            if (!closed_nonVolatile) {
                makeAllDirtyAndEnsurePendingClientPainting();
            }
            keyRepetitionHelper.onWindowFocusLost();
            hostOfFocusedClientHolder.setHostOfFocusedClient(null);
            cancelDragIfDragPressDetected();
            //
            try {
                beforeClientWindowFocusLost_spe(event);
            } finally {
                clientWrapper.onWindowFocusLost(event);
            }
        }
        @Override
        public void onWindowIconified(BwdWindowEvent event) {
            cancelDragIfDragPressDetected();
            //
            resetEventFlagsOnFiring(BwdEventType.WINDOW_ICONIFIED);
            try {
                beforeClientWindowIconified_spe(event);
            } finally {
                clientWrapper.onWindowIconified(event);
            }
        }
        @Override
        public void onWindowDeiconified(BwdWindowEvent event) {
            callAsync(
                    (bindingConfig.getMustRequestFocusGainOnDeiconified() ? requestFocusGainIfNotFocusedRunnable : null));
            // Client necessarily already showing, so no need to test it.
            mustTryToRestoreClientMaxStateIntoHost =
                    bindingConfig.getMustRestoreMaximizedStateWhenBackToShowingAndDeiconified();
            if (clientWrapper.isMaximized()) {
                ensureBoundsEnforcingFlagOnShowDeicoMaxIfConfigured();
            } else {
                ensureBoundsEnforcingFlagOnShowDeicoDemaxIfConfiguredAndHaveAny();
            }
            //
            resetEventFlagsOnFiring(BwdEventType.WINDOW_DEICONIFIED);
            try {
                beforeClientWindowDeiconified_spe(event);
            } finally {
                clientWrapper.onWindowDeiconified(event);
            }
        }
        @Override
        public void onWindowMaximized(BwdWindowEvent event) {
            /*
             * This state change, if not due to a call to maximize(),
             * should occur while focused,
             * so not bothering to request focus,
             * which we already do in maximize().
             */
            cancelDragIfDragPressDetected();
            ensureBoundsEnforcingFlagOnShowDeicoMaxIfConfigured();
            //
            resetEventFlagsOnFiring(BwdEventType.WINDOW_MAXIMIZED);
            try {
                beforeClientWindowMaximized_spe(event);
            } finally {
                clientWrapper.onWindowMaximized(event);
            }
        }
        @Override
        public void onWindowDemaximized(BwdWindowEvent event) {
            /*
             * This state change, if not due to a call to demaximize(),
             * should occur while focused,
             * so not bothering to request focus,
             * which we already do in demaximize().
             */
            cancelDragIfDragPressDetected();
            ensureBoundsEnforcingFlagOnShowDeicoDemaxIfConfiguredAndHaveAny();
            //
            resetEventFlagsOnFiring(BwdEventType.WINDOW_DEMAXIMIZED);
            try {
                beforeClientWindowDemaximized_spe(event);
            } finally {
                clientWrapper.onWindowDemaximized(event);
            }
        }
        @Override
        public void onWindowMoved(BwdWindowEvent event) {
            try {
                beforeClientWindowMoved_spe(event);
            } finally {
                clientWrapper.onWindowMoved(event);
            }
        }
        @Override
        public void onWindowResized(BwdWindowEvent event) {
            try {
                beforeClientWindowResized_spe(event);
            } finally {
                clientWrapper.onWindowResized(event);
            }
        }
        @Override
        public void onWindowClosed(BwdWindowEvent event) {
            try {
                beforeClientWindowClosed_spe(event);
            } finally {
                clientWrapper.onWindowClosed(event);
            }
        }
    }

    /*
     * 
     */

    private class MyDialogLifecycleListener implements InterfaceHostLifecycleListener<AbstractBwdHost> {
        public MyDialogLifecycleListener() {
        }
        @Override
        public void onHostCreated(AbstractBwdHost dialog) {
            onDialogCreatedImpl(dialog);
        }
        @Override
        public void onHostClosing(AbstractBwdHost dialog) {
            onDialogClosingImpl(dialog);
        }
        @Override
        public void onHostClosedEventFiring(AbstractBwdHost dialog) {
            onDialogClosedEventFiringImpl(dialog);
        }
    }

    /**
     * Takes care of calling paintClientNowOrLater() ASAP, but
     * taking into account client paint delay to avoid too close calls.
     */
    private class MyPaintProcess extends AbstractProcess {
        private long lastPaintTimeNs = Long.MIN_VALUE;
        private long lastPaintLatenessNs = 0L;
        public MyPaintProcess(InterfaceScheduler uiTheadScheduler) {
            super(uiTheadScheduler);
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {

            if (DEBUG) {
                Dbg.logPr(this, "process(" + theoreticalTimeNs + ", " + actualTimeNs + ")");
            }
            
            if (!isShowing()) {
                if (DEBUG) {
                    hostLog(this, "process(...) : not showing: stopping");
                }
                // Not showing, possibly even closed.
                this.stop();
                return 0;
            }

            final long latenessNs = minusBounded(actualTimeNs, theoreticalTimeNs);

            final long clientPaintDelayNs = sToNsNoUnderflow(
                    bindingConfig.getClientPaintingDelayS());

            final long toleranceNs = clientPaintDelayNs / 2;

            final long minPaintTimeNs = plusBounded(this.lastPaintTimeNs, clientPaintDelayNs);

            final boolean mustPaintNow = (actualTimeNs >= minPaintTimeNs - toleranceNs);
            if (mustPaintNow) {
                /*
                 * Taking care NOT to call stop() after painting,
                 * which could cancel a call to start() done from painting
                 * to ensure another painting ASAP.
                 */
                this.stop();
                
                this.lastPaintLatenessNs = latenessNs;
                if (DEBUG) {
                    hostLog(this, "process(...) : host.paintClientNowOrLater()");
                }
                /*
                 * Updating last paint time here, and not in each host implementation
                 * just before each synchronous painting, for multiple reasons:
                 * - simplicity,
                 * - because ignoring additional and exceptional anti-glitch paintings
                 *   doesn't hurt here,
                 * - to avoid a time bias due to what happens between here and the actual
                 *   beginning of painting.
                 */
                this.lastPaintTimeNs = actualTimeNs;
                paintClientNowOrLater();
                
                // Here repetition is either stopped,
                // or restarted (due to a call to start()),
                // so what we return doesn't matter.
                return 0;
            } else {
                /*
                 * Subtracting the lateness we had for previous painting,
                 * to avoid getting too much late again and have a drift,
                 * but taking care not to be more early than tolerance,
                 * else we could pass here a lot of times in busy mode
                 * until we reach the next allowed paint time.
                 */
                final long nextTheoreticalTimeNs = minPaintTimeNs - Math.min(toleranceNs, this.lastPaintLatenessNs);
                if (DEBUG) {
                    hostLog(this, "process(...) : nextTheoreticalTimeNs = " + nextTheoreticalTimeNs);
                }
                return nextTheoreticalTimeNs;
            }
        }
    }

    /**
     * There must be one instance for (x,y) events (moves),
     * and another for (width,height) events (resizes).
     */
    private class MyPaintAfterEventProcess extends AbstractProcess {
        private final long delayNs;
        private final boolean mustTryToPaintDuringBurst;
        private boolean firstCall;
        public MyPaintAfterEventProcess(
                InterfaceScheduler scheduler,
                double delayS,
                boolean mustTryToPaintDuringBurst) {
            super(scheduler);
            this.delayNs = sToNsNoUnderflow(delayS);
            this.mustTryToPaintDuringBurst = mustTryToPaintDuringBurst;
        }
        @Override
        protected void onBegin() {
            this.firstCall = true;
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {

            /*
             * Executes at most twice per call to start().
             */

            final boolean currentCallIsFirstOne = this.firstCall;
            if (currentCallIsFirstOne) {
                this.firstCall = false;
            }

            boolean mustRunAgain = currentCallIsFirstOne;

            if (this.mustTryToPaintDuringBurst) {
                if (currentCallIsFirstOne) {
                    makeAllDirtyAndEnsurePendingClientPainting();
                    mustRunAgain = false;
                }
            } else {
                if (currentCallIsFirstOne) {
                    // Nothing to do.
                } else {
                    // Here, we are a delay after burst end.
                    makeAllDirtyAndEnsurePendingClientPainting();
                }
            }

            if (mustRunAgain) {
                // Input time still valid, since we didn't do anything heavy
                // (no synchronous painting).
                return plusBounded(actualTimeNs, this.delayNs);
            } else {
                this.stop();
                return 0;
            }
        }
    }

    private class MyDeiconifyIfIconifiedRunnable implements Runnable {
        @Override
        public void run() {
            final boolean iconified = isIconified();
            if (DEBUG) {
                hostLog(this, "iconified = " + iconified);
            }
            if (iconified) {
                deiconify();
            }
        }
    }

    private class MyRequestFocusGainIfNotFocusedRunnable implements Runnable {
        @Override
        public void run() {
            final boolean focused = isFocused();
            if (DEBUG) {
                hostLog(this, "focused = " + focused);
            }
            if (!focused) {
                requestFocusGain();
            }
        }
    }

    /**
     * Schedules the specified runnables asynchronously,
     * each one being scheduled for asynchonous execution
     * after the preceding one.
     * Runnables can throw, that doesn't prevent others to be scheduled.
     * 
     * @param runnableArr Can be null or contain nulls.
     */
    private void callAsync(final Runnable... runnableArr) {
        callAsync_internal(runnableArr, 0);
    }
    
    /**
     * This method is asynchronously recursive.
     * 
     * @param runnableArr Can be null or contain nulls.
     * @param offset Index of first runnable to schedule.
     */
    private void callAsync_internal(
            final Runnable[] runnableArr,
            final int offset) {
        if (runnableArr == null) {
            return;
        }
        boolean foundNonNull = false;
        for (int i = offset; i < runnableArr.length; i++) {
            if (runnableArr[i] != null) {
                foundNonNull = true;
                break;
            }
        }
        if (!foundNonNull) {
            // Nothing to execute.
            return;
        }

        final InterfaceScheduler uiThreadScheduler = getBinding().getUiThreadScheduler();

        /*
         * Scheduling even if this runnable is null,
         * to make sure it doesn't modify the asynchronism
         * of other runnables, which could be a troublesome
         * side effect if they need that asynchronism.
         */
        final Runnable runnable = runnableArr[offset];
        uiThreadScheduler.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (runnable != null) {
                        runnable.run();
                    }
                } finally {
                    if (offset < runnableArr.length - 1) {
                        // Got more to call.
                        // Scheduling after call to run(),
                        // to that it runs after ASAP schedules
                        // done in run(), if it can ever help.
                        callAsync_internal(runnableArr, offset + 1);
                    }
                }
            }
        });
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final MyClientWindowEventWrapper clientWindowEventWrapper = new MyClientWindowEventWrapper();

    private final BaseBwdBindingConfig bindingConfig;

    private final InterfaceBwdBinding binding;

    /**
     * To listen to life cycle of this host or dialogs created from it.
     */
    private final InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener;

    /**
     * To listen to life cycle of dialogs created from this host.
     * 
     * Calls hostLifecycleListener as needed.
     */
    private final InterfaceHostLifecycleListener<AbstractBwdHost> dialogLifecycleListener = new MyDialogLifecycleListener();

    private final InterfaceBwdHost owner;

    private final String title;

    private final boolean decorated;
    private final boolean modal;

    /**
     * List of dialogs created from this host, for which close() method
     * has not been called yet.
     * 
     * List and not a set, for determinism, and it should not hurt
     * since we usually don't create thousands of dialogs.
     * 
     * In particular, allows to close dialogs on owner closing,
     * because some libraries don't do it.
     */
    private final List<AbstractBwdHost> dialogList = new ArrayList<AbstractBwdHost>();

    /**
     * @see ClientWrapperForHost
     */
    private final ClientWrapperForHost clientWrapper;

    private final PaintClientHelper paintClientHelper;

    private boolean closed_nonVolatile = false;

    /**
     * Volatile for usage in isClosed(),
     * which must be callable from any thread.
     */
    private volatile boolean closed_volatile = false;

    /*
     * MOVED and RESIZED events for client:
     * Ensuring pending MOVED or RESIZED events, instead of firing them
     * synchronously on every backing move or resize, which allows to make sure
     * subsequent code will be executed even if a user listener throws, and to
     * avoid useless spam in case of multiple consecutive calls, in particular
     * with libraries (such as JavaFX) that update x/y/width/height properties
     * separately.
     */

    private double newestWindowEventLogicLoopPeriodicCallTimeS = Double.NEGATIVE_INFINITY;
    
    /**
     * Must be checked after each call to runWindowEventLogicOnce(),
     * typically in the finally of a try/finally in case
     * a user's listener throws.
     */
    private boolean mustRunWindowEventLogicAgain = false;

    /*
     * 
     */

    private final MyPaintProcess paintProcess;

    /*
     * 
     */

    private final Object dirtyRectBbMutex = new Object();

    /**
     * Our bindings just always use dirty rectangles bounding box,
     * instead of doing one client.paintClient(...) call
     * per dirty rectangle.
     * 
     * Reset to GRect.DEFAULT_EMPTY on use.
     * 
     * Guarded by dirtyRectBbMutex.
     */
    private GRect dirtyRectBb = GRect.DEFAULT_EMPTY;

    /*
     * 
     */

    private final KeyRepetitionHelper keyRepetitionHelper;

    /*
     * 
     */

    private final MyPaintAfterEventProcess paintAfterMoveProcess;
    private final MyPaintAfterEventProcess paintAfterResizeProcess;

    /*
     * 
     */

    private final HostOfFocusedClientHolder hostOfFocusedClientHolder;

    /*
     * 
     */

    private final Runnable deiconifyIfIconifiedRunnable = new MyDeiconifyIfIconifiedRunnable();
    private final Runnable requestFocusGainIfNotFocusedRunnable = new MyRequestFocusGainIfNotFocusedRunnable();

    /**
     * To be set to current time (from the clock we use),
     * whenever we detect something happening that might
     * cause state flickering, typically iconified or maximized
     * state flickering.
     * Allows to only ensure some state some duration after
     * eventual flickerings.
     */
    private double newestPossibleUnstabilityTimeS = Double.NEGATIVE_INFINITY;

    /**
     * Only used for some types of events.
     */
    private final boolean[] backingStateDetectedAndNotFiredByOrdinal = new boolean[BwdEventType.valueList().size()];
    
    /**
     * Only used for some types of events.
     * 
     * Newest time of detection for a difference between backing and client state.
     */
    private final double[] newestBackingStateDiffDetTimeSByOrdinal = new double[BwdEventType.valueList().size()];
    {
        fillWith(this.newestBackingStateDiffDetTimeSByOrdinal, Double.NEGATIVE_INFINITY);
    }
    
    /**
     * Only used for some types of events.
     * 
     * True if backing state modification from corresponding
     * programmatic modification is pending.
     */
    private final boolean[] pendingBackingStateProgModByOrdinal = new boolean[BwdEventType.valueList().size()];
    
    private boolean mustTryToRestoreClientMaxStateIntoHost = false;

    /*
     * TODO With some backing libraries, there is
     * some asynchronism in some coordinates updates.
     * Also, some libraries (Allegro5 and JOGL/NEWT) generate mouse moved
     * events when the mouse stands still but the window moves.
     * 
     * Due to these issues, trying to drag or resize the window from a grip
     * painted in client area (and not from native decoration borders),
     * can cause crazy bounds updates such as erratic or cyclic jumps,
     * or window being thrown out of the screen in the blink of an eye.
     * 
     * To make up for that, during a drag, we allow for disabling of client and
     * window actual bounds setting, and fix the bounds to read with
     * getXxxBounds() methods, and for setting once the drag stops (to apply drag result).
     * Cf. mustFixBoundsDuringDrag in BaseBwdBindingConfig.
     * 
     * NB: This need for hacking in case of drags is why we want to know
     * which button is the drag button at the binding level, instead of having
     * bindings only generate MOUSE_MOVED events and let clients decide whether
     * they want to consider them as MOUSE_DRAGGED events depending on whether
     * a button of their choice is down.
     */
    
    private GRect clientBoundsOnDragPress = null;
    private GRect windowBoundsOnDragPress = null;
    private boolean dragMoveDetected = false;
    private GRect targetClientBoundsOnDragEnd = null;
    private GRect targetWindowBoundsOnDragEnd = null;

    /*
     * 
     */
    
    private boolean isBoundsEnforcingOnShowDeicoMaxPending = false;
    
    /*
     * 
     */
    
    /**
     * Enforcing due to a previous call to setXxxBounds(...),
     * or just restoration of previous demax bounds.
     * 
     * Invariant : never true if there are no bounds to enforce.
     * 
     * This flag is useful because we might set bounds to enforce
     * while the host is showing/deico/demax and stable,
     * i.e. while their usage is not pending yet.
     */
    private boolean isBoundsEnforcingOnShowDeicoDemaxPending = false;

    /*
     * Used to store both target bounds specified by user with public
     * bounds setting methods, and, if so configured, (stable) bounds
     * to enforce on demaximization.
     * NB: Could try to merge these with targetXxxBoundsOnDragEnd,
     * unless we prefer to keep these treatments decoupled.
     * 
     * Invariant : only one can be non-null at once.
     */
    
    private GRect clientBoundsToEnforceOnShowDeicoDemax = null;
    private GRect windowBoundsToEnforceOnShowDeicoDemax = null;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Calls setHost(...) on the specified client.
     * 
     * @param binding Must not be null.
     * @param hostLifecycleListener Must not be null.
     * @param owner Owner, if this host is a dialog.
     * @param title Must not be null.
     * @param client The client tied to this host. Must not be null.
     * @throws IllegalStateException if the binding is shut down.
     */
    public AbstractBwdHost(
            final BaseBwdBindingConfig bindingConfig,
            InterfaceBwdBinding binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            HostOfFocusedClientHolder hostOfFocusedClientHolder,
            InterfaceBwdHost owner,
            String title,
            boolean decorated,
            boolean modal,
            InterfaceBwdClient client) {

        LangUtils.requireNonNull(bindingConfig);
        LangUtils.requireNonNull(binding);
        LangUtils.requireNonNull(hostLifecycleListener);
        LangUtils.requireNonNull(hostOfFocusedClientHolder);
        LangUtils.requireNonNull(title);
        LangUtils.requireNonNull(client);
        
        // As specified in binding.newHost(...).
        if (binding.isShutdown()) {
            throw new IllegalStateException("binding is shut down: can't create a new host");
        }
        if (owner != null) {
            // As specified in host.newDialog(...).
            if (owner.isClosed()) {
                throw new IllegalStateException("owner is closed: can't create a new dialog");
            }
        }

        final InterfaceScheduler uiThreadScheduler = binding.getUiThreadScheduler();

        this.bindingConfig = bindingConfig;

        this.binding = binding;

        this.hostLifecycleListener = hostLifecycleListener;

        this.hostOfFocusedClientHolder = hostOfFocusedClientHolder;

        this.owner = owner;

        this.title = LangUtils.requireNonNull(title);

        this.decorated = decorated;
        this.modal = modal;

        final ClientWrapperForHost clientWrapper =
                new ClientWrapperForHost(
                        bindingConfig,
                        client);
        /*
         * Doing is early, even though it maximizes the risk of
         * "this publication" issue, in case other methods of client
         * get called during host construction and the client
         * needs to know host reference in them.
         */
        clientWrapper.setHost(this);
        
        this.clientWrapper = clientWrapper;

        this.paintClientHelper = new PaintClientHelper(clientWrapper);

        final InterfaceDoubleSupplier keyRepetitionTriggerDelaySSupplier =
                new InterfaceDoubleSupplier() {
            @Override
            public double get() {
                return bindingConfig.getKeyRepetitionTriggerDelayS();
            }
        };
        final InterfaceDoubleSupplier keyRepetitionPeriodSSupplier =
                new InterfaceDoubleSupplier() {
            @Override
            public double get() {
                return bindingConfig.getKeyRepetitionPeriodS();
            }
        };
        this.keyRepetitionHelper = new KeyRepetitionHelper(
                keyRepetitionTriggerDelaySSupplier,
                keyRepetitionPeriodSSupplier,
                uiThreadScheduler,
                clientWrapper);

        this.paintProcess = new MyPaintProcess(uiThreadScheduler);

        this.paintAfterMoveProcess = new MyPaintAfterEventProcess(
                uiThreadScheduler,
                bindingConfig.getHostBoundsCheckDelayS(),
                bindingConfig.getMustTryToPaintDuringMove());
        this.paintAfterResizeProcess = new MyPaintAfterEventProcess(
                uiThreadScheduler,
                bindingConfig.getHostBoundsCheckDelayS(),
                bindingConfig.getMustTryToPaintDuringResize());
        
        // We prefer to initialize it with creation time
        // than to let -Infinity in it.
        this.setNewestPossibleUnstabilityTime();
    }

    @Override
    public String toString() {
        return this.title;
    }

    public BaseBwdBindingConfig getBindingConfig() {
        return this.bindingConfig;
    }

    public InterfaceBwdBinding getBinding() {
        return this.binding;
    }
    
    /*
     * 
     */
    
    @Override
    public GPoint getMousePosInScreen() {
        return this.binding.getMousePosInScreen();
    }

    /*
     * 
     */

    /**
     * Useful in particular as map key, to retrieve this host from
     * backing events that indicate the backing window they correspond to.
     * 
     * @return The corresponding window (or a key allowing to retrieve it)
     *         for the backing library, for comparison with "==" or equals(Object).
     */
    @Override
    public abstract Object getBackingWindow();

    /**
     * For tests.
     * 
     * @return The newest time, in seconds, and in UI thread scheduler clock
     *         time reference, at which an event occurred which might be
     *         or be a sign of a state change of the backing window.
     */
    public double getNewestPossibleUnstabilityTimeS() {
        return this.newestPossibleUnstabilityTimeS;
    }

    /*
     * 
     */

    @Override
    public boolean isDialog() {
        return (this.owner != null);
    }

    @Override
    public InterfaceBwdHost getOwner() {
        return this.owner;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public boolean isDecorated() {
        return this.decorated;
    }

    @Override
    public boolean isModal() {
        return this.modal;
    }

    @Override
    public InterfaceBwdClient getClient() {
        return this.clientWrapper.getClient();
    }

    /**
     * @return A client wrapper that catches client exceptions
     *         if so configured.
     */
    public InterfaceBwdClient getClientWithExceptionHandler() {
        return this.clientWrapper;
    }

    /*
     * 
     */
    
    @Override
    public void makeDirty(GRect dirtyRect) {
        // Constraining dirty rectangles within DEFAULT_HUGE bounds,
        // to avoid ArithmeticException when computing bounding boxes.
        // Also allows to trim it, which makes the job easier
        // for treatments that use it.
        // Implicit null check.
        dirtyRect = dirtyRect.intersected(GRect.DEFAULT_HUGE);
        
        synchronized (dirtyRectBbMutex) {
            this.dirtyRectBb = this.dirtyRectBb.unionBoundingBox(dirtyRect);
        }
    }

    @Override
    public void makeAllDirty() {
        this.makeDirty(GRect.DEFAULT_HUGE);
    }
    
    @Override
    public void makeDirtyAndEnsurePendingClientPainting(GRect dirtyRect) {
        this.makeDirty(dirtyRect);
        this.ensurePendingClientPainting();
    }

    @Override
    public void makeAllDirtyAndEnsurePendingClientPainting() {
        this.makeAllDirty();
        this.ensurePendingClientPainting();
    }

    @Override
    public void ensurePendingClientPainting() {
        this.paintProcess.start();
    }

    /*
     * 
     */

    @Override
    public List<InterfaceBwdHost> getDialogList() {
        return new ArrayList<InterfaceBwdHost>(this.dialogList);
    }

    /*
     * 
     */

    @Override
    public void setWindowAlphaFp(double windowAlphaFp) {
        if (!((windowAlphaFp >= 0.0) && (windowAlphaFp <= 1.0))) {
            throw new IllegalArgumentException("" + windowAlphaFp);
        }
        this.setWindowAlphaFp_backing(windowAlphaFp);
    }

    /*
     * 
     */

    @Override
    public boolean isShowing() {
        if (this.closed_nonVolatile) {
            return false;
        }
        return this.isBackingWindowShowing();
    }

    @Override
    public void show() {
        if (DEBUG) {
            hostLog(this, "show()");
        }
        if (this.closed_nonVolatile) {
            if (DEBUG) {
                hostLog(this, "bad state");
            }
            return;
        }

        /*
         * Doing it even if already showing.
         */
        callAsync(
                (bindingConfig.getMustDeiconifyOnShow() ? deiconifyIfIconifiedRunnable : null),
                (bindingConfig.getMustRequestFocusGainOnShow() ? requestFocusGainIfNotFocusedRunnable : null));

        if (MUST_ABORT_PROG_MOD_IF_ALREADY_IN_STATE) {
            if (this.isBackingWindowShowing()) {
                if (DEBUG) {
                    hostLog(this, "already showing");
                }
                return;
            }
        }

        this.beforeBackingStateProgMod(BwdEventType.WINDOW_SHOWN);

        if (DEBUG) {
            hostLog(this, "showBackingWindow()");
        }
        this.showBackingWindow();
        
        this.afterBackingStateProgMod();
    }

    @Override
    public void hide() {
        if (DEBUG) {
            hostLog(this, "hide()");
        }
        if (this.closed_nonVolatile) {
            if (DEBUG) {
                hostLog(this, "bad state");
            }
            return;
        }
        
        if (MUST_ABORT_PROG_MOD_IF_ALREADY_IN_STATE) {
            if (!this.isBackingWindowShowing()) {
                if (DEBUG) {
                    hostLog(this, "already hidden");
                }
                return;
            }
        }

        this.beforeBackingStateProgMod(BwdEventType.WINDOW_HIDDEN);

        if (DEBUG) {
            hostLog(this, "hideBackingWindow()");
        }
        this.hideBackingWindow();
        
        this.afterBackingStateProgMod();
    }

    /*
     * Focus.
     */

    @Override
    public boolean isFocused() {
        if (!this.isHostShowingDeico()) {
            return false;
        }
        return this.isBackingWindowFocused();
    }

    @Override
    public void requestFocusGain() {
        if (DEBUG) {
            hostLog(this, "requestFocusGain()");
        }
        if (!this.isHostShowingDeico()) {
            if (DEBUG) {
                hostLog(this, "bad state");
            }
            return;
        }

        if (MUST_ABORT_PROG_MOD_IF_ALREADY_IN_STATE) {
            if (this.isBackingWindowFocused()) {
                if (DEBUG) {
                    hostLog(this, "already focused");
                }
                return;
            }
        }
        
        this.beforeBackingStateProgMod(BwdEventType.WINDOW_FOCUS_GAINED);
        
        if (DEBUG) {
            hostLog(this, "requestBackingWindowFocusGain()");
        }
        this.requestBackingWindowFocusGain();
        
        this.afterBackingStateProgMod();
    }

    /*
     * Iconified.
     */

    @Override
    public boolean isIconified() {
        if (!this.isShowing()) {
            return false;
        }
        return this.isBackingWindowIconified();
    }

    @Override
    public void iconify() {
        if (DEBUG) {
            hostLog(this, "iconify()");
        }
        if (!this.isShowing()) {
            if (DEBUG) {
                hostLog(this, "bad state");
            }
            return;
        }
        if (!this.doesSetBackingWindowIconifiedOrNotWork()) {
            if (DEBUG) {
                hostLog(this, "setBackingWindowIconifiedOrNot(boolean) not expected to work, not trying");
            }
            return;
        }

        if (MUST_ABORT_PROG_MOD_IF_ALREADY_IN_STATE) {
            if (this.isBackingWindowIconified()) {
                if (DEBUG) {
                    hostLog(this, "already iconified");
                }
                return;
            }
        }

        this.beforeBackingStateProgMod(BwdEventType.WINDOW_ICONIFIED);
        
        if (DEBUG) {
            hostLog(this, "setBackingWindowIconifiedOrNot(true)");
        }
        this.setBackingWindowIconifiedOrNot(true);
        
        this.afterBackingStateProgMod();
    }

    @Override
    public void deiconify() {
        if (DEBUG) {
            hostLog(this, "deiconify()");
        }
        if (!this.isShowing()) {
            if (DEBUG) {
                hostLog(this, "bad state");
            }
            return;
        }
        
        /*
         * Doing it even if already deiconified
         * (consistent with what we do in show()).
         */
        callAsync(
                (this.bindingConfig.getMustRequestFocusGainOnDeiconify() ? requestFocusGainIfNotFocusedRunnable : null));

        if (!this.doesSetBackingWindowIconifiedOrNotWork()) {
            if (DEBUG) {
                hostLog(this, "setBackingWindowIconifiedOrNot(boolean) not expected to work, not trying");
            }
            return;
        }

        if (MUST_ABORT_PROG_MOD_IF_ALREADY_IN_STATE) {
            if (!this.isBackingWindowIconified()) {
                if (DEBUG) {
                    hostLog(this, "already deiconified");
                }
                return;
            }
        }
        
        this.beforeBackingStateProgMod(BwdEventType.WINDOW_DEICONIFIED);
        
        if (DEBUG) {
            hostLog(this, "setBackingWindowIconifiedOrNot(false)");
        }
        this.setBackingWindowIconifiedOrNot(false);
        
        this.afterBackingStateProgMod();
    }

    /*
     * Maximized.
     */

    @Override
    public boolean isMaximized() {
        if ((!this.isShowing())
                || this.isBackingWindowIconified()) {
            return false;
        }
        return this.isBackingWindowMaximized();
    }

    @Override
    public void maximize() {
        if (DEBUG) {
            hostLog(this, "maximize()");
        }
        if (!this.isHostShowingDeico()) {
            if (DEBUG) {
                hostLog(this, "bad state");
            }
            return;
        }

        /*
         * Doing it even if already maximized
         * (consistent with what we do in show()).
         */
        callAsync(
                (this.bindingConfig.getMustRequestFocusGainOnMaximize() ? requestFocusGainIfNotFocusedRunnable : null));

        if (!this.doesSetBackingWindowMaximizedOrNotWork()) {
            if (DEBUG) {
                hostLog(this, "setBackingWindowMaximizedOrNot(boolean) not expected to work, not trying");
            }
            return;
        }

        if (MUST_ABORT_PROG_MOD_IF_ALREADY_IN_STATE) {
            if (this.isBackingWindowMaximized()) {
                if (DEBUG) {
                    hostLog(this, "already maximized");
                }
                return;
            }
        }

        this.beforeBackingStateProgMod(BwdEventType.WINDOW_MAXIMIZED);
        
        if (DEBUG) {
            hostLog(this, "setBackingWindowMaximizedOrNot(true)");
        }
        this.setBackingWindowMaximizedOrNot(true);
        
        this.afterBackingStateProgMod();
    }

    @Override
    public void demaximize() {
        if (DEBUG) {
            hostLog(this, "demaximize()");
        }
        if (!this.isHostShowingDeico()) {
            if (DEBUG) {
                hostLog(this, "bad state");
            }
            return;
        }

        /*
         * Doing it even if already demaximized
         * (consistent with what we do in show()).
         */
        callAsync(
                (this.bindingConfig.getMustRequestFocusGainOnDemaximize() ? requestFocusGainIfNotFocusedRunnable : null));

        if (!this.doesSetBackingWindowMaximizedOrNotWork()) {
            if (DEBUG) {
                hostLog(this, "setBackingWindowMaximizedOrNot(boolean) not expected to work, not trying");
            }
            return;
        }

        if (MUST_ABORT_PROG_MOD_IF_ALREADY_IN_STATE) {
            if (!this.isBackingWindowMaximized()) {
                if (DEBUG) {
                    hostLog(this, "already demaximized");
                }
                return;
            }
        }

        this.beforeBackingStateProgMod(BwdEventType.WINDOW_DEMAXIMIZED);
        
        if (DEBUG) {
            hostLog(this, "setBackingWindowMaximizedOrNot(false)");
        }
        this.setBackingWindowMaximizedOrNot(false);
        
        this.afterBackingStateProgMod();
    }

    /*
     * Bounds.
     */

    @Override
    public GRect getInsets() {
        if (!this.isHostShowingDeico()) {
            return GRect.DEFAULT_EMPTY;
        }
        /*
         * Not caching it, because computation might not be reliable,
         * if computed as a difference between window bounds and client bounds.
         */
        return this.getBackingInsets();
    }

    @Override
    public GRect getClientBounds() {
        if (!this.isHostShowingDeico()) {
            return GRect.DEFAULT_EMPTY;
        }
        return this.getBackingOrOnDragBeginClientBounds();
    }

    @Override
    public GRect getWindowBounds() {
        if (!this.isHostShowingDeico()) {
            return GRect.DEFAULT_EMPTY;
        }
        return this.getBackingOrOnDragBeginWindowBounds();
    }

    @Override
    public void setClientBounds(GRect targetClientBounds) {
        
        // Implicit null check.
        targetClientBounds = sanitizedTargetBounds(targetClientBounds);

        final boolean hostShowingDeico = this.isHostShowingDeico();
        if (hostShowingDeico && (!this.isMaximized())) {
            // Making sure we won't overwrite specified bounds
            // with some previous bounds to enforce on demax.
            this.clearBoundsEnforcingOnShowDeicoDemax();
            
            if (this.bindingConfig.getMustFixBoundsDuringDrag()
                    && this.isDragMoveDetected()) {
                this.setTargetClientBoundsOnDragEnd(targetClientBounds);
            } else {
                if (DEBUG) {
                    hostLog(this, "calling setBackingClientBounds(" + targetClientBounds + ")");
                }
                this.setBackingClientBounds(targetClientBounds);
            }
        } else {
            this.setClientBoundsToEnforceOnShowDeicoDemax(targetClientBounds);
            this.ensureBoundsEnforcingFlagOnShowDeicoDemax();
            
            if (this.bindingConfig.getMustSetBackingDemaxBoundsWhileHiddenOrIconified()
                    && (!hostShowingDeico)
                    // isMaximized() always returns false
                    // when not showing and deiconified,
                    // so using client state.
                    && (!this.clientWrapper.isMaximized())) {
                if (DEBUG) {
                    hostLog(this, "(anti-glitch) calling setBackingClientBounds(" + targetClientBounds + ")");
                }
                this.setBackingClientBounds(targetClientBounds);
            }
        }
    }

    @Override
    public void setWindowBounds(GRect targetWindowBounds) {
        
        // Implicit null check.
        targetWindowBounds = sanitizedTargetBounds(targetWindowBounds);
        
        final boolean hostShowingDeico = this.isHostShowingDeico();
        if (hostShowingDeico && (!this.isMaximized())) {
            // Making sure we won't overwrite specified bounds
            // with some previous bounds to enforce on demax.
            this.clearBoundsEnforcingOnShowDeicoDemax();
            
            if (this.bindingConfig.getMustFixBoundsDuringDrag()
                    && this.isDragMoveDetected()) {
                this.setTargetWindowBoundsOnDragEnd(targetWindowBounds);
            } else {
                if (DEBUG) {
                    hostLog(this, "calling setBackingWindowBounds(" + targetWindowBounds + ")");
                }
                this.setBackingWindowBounds(targetWindowBounds);
            }
        } else {
            this.setWindowBoundsToEnforceOnShowDeicoDemax(targetWindowBounds);
            this.ensureBoundsEnforcingFlagOnShowDeicoDemax();

            if (this.bindingConfig.getMustSetBackingDemaxBoundsWhileHiddenOrIconified()
                    && (!hostShowingDeico)
                    // isMaximized() always returns false
                    // when not showing and deiconified,
                    // so using client state.
                    && (!this.clientWrapper.isMaximized())) {
                if (DEBUG) {
                    hostLog(this, "(anti-glitch) calling setBackingWindowBounds(" + targetWindowBounds + ")");
                }
                this.setBackingWindowBounds(targetWindowBounds);
            }
        }
    }

    @Override
    public boolean setClientBoundsSmart(GRect targetClientBounds) {
        final boolean mustFixRight = false;
        final boolean mustFixBottom = false;
        final boolean mustRestoreBoundsIfNotExact = false;
        return this.setClientBoundsSmart(
                targetClientBounds,
                mustFixRight,
                mustFixBottom,
                mustRestoreBoundsIfNotExact);
    }

    @Override
    public boolean setClientBoundsSmart(
            GRect targetClientBounds,
            boolean mustFixRight,
            boolean mustFixBottom,
            boolean mustRestoreBoundsIfNotExact) {
        final boolean clientElseWindow = true;
        return this.setXxxBoundsSmart(
                clientElseWindow,
                //
                targetClientBounds,
                mustFixRight,
                mustFixBottom,
                mustRestoreBoundsIfNotExact);
    }

    @Override
    public boolean setWindowBoundsSmart(GRect targetWindowBounds) {
        final boolean mustFixRight = false;
        final boolean mustFixBottom = false;
        final boolean mustRestoreBoundsIfNotExact = false;
        return this.setWindowBoundsSmart(
                targetWindowBounds,
                mustFixRight,
                mustFixBottom,
                mustRestoreBoundsIfNotExact);
    }

    @Override
    public boolean setWindowBoundsSmart(
            GRect targetWindowBounds,
            boolean mustFixRight,
            boolean mustFixBottom,
            boolean mustRestoreBoundsIfNotExact) {
        final boolean clientElseWindow = false;
        return this.setXxxBoundsSmart(
                clientElseWindow,
                //
                targetWindowBounds,
                mustFixRight,
                mustFixBottom,
                mustRestoreBoundsIfNotExact);
    }

    /*
     * Closing.
     */

    @Override
    public boolean isClosed() {
        return this.closed_volatile;
    }

    @Override
    public boolean isClosed_nonVolatile() {
        return this.closed_nonVolatile;
    }

    @Override
    public void close() {
        if (DEBUG) {
            hostLog(this, "close()");
        }

        if (this.closed_nonVolatile) {
            return;
        }

        this.closed_nonVolatile = true;
        this.closed_volatile = true;

        this.paintProcess.stop();
        this.paintAfterMoveProcess.stop();
        this.paintAfterResizeProcess.stop();

        /*
         * Closing descendants, recursively, because some libraries (such as JavaFX)
         * don't do it, and we don't want to have orphan dialogs.
         */

        /*
         * With some libraries (such as JavaFX), closing the backing window
         * synchronously generates backing events on which we run event logic,
         * eventually causing us to fire events, with user listener eventually
         * throwing, which we want to be robust to.
         * As a result, we just catch anything thrown during backing windows
         * closing, and at the end fire the first throwable that was encountered,
         * eventual other exceptions added as suppressed exceptions.
         */
        Throwable thr = null;

        final List<InterfaceBwdHost> dialogList = this.getDialogList();
        for (InterfaceBwdHost dialog_ : dialogList) {
            final AbstractBwdHost dialog = (AbstractBwdHost) dialog_;
            try {
                dialog.close();
            } catch (Throwable t) {
                if (thr == null) {
                    thr = t;
                } else {
                    // TODO Java 7+: Can use Throwable.addSuppressed(...).
                    //thr.addSuppressed(t);
                }
            }
        }

        /*
         * Closing self after descendants.
         */
        if (DEBUG) {
            Dbg.log("calling closeBackingWindow()");
        }
        try {
            this.closeBackingWindow();
        } catch (Throwable t) {
            if (thr == null) {
                thr = t;
            } else {
                // TODO Java 7+: Can use Throwable.addSuppressed(...).
                //thr.addSuppressed(t);
            }
        }

        if (DEBUG) {
            hostLog(this, "calling onHostClosing(...)");
        }
        this.hostLifecycleListener.onHostClosing(this);

        /*
         * Running event logic to get CLOSED fired
         * (with eventual preliminary FOCUS_LOST and HIDDEN).
         * 
         * Even though client events can always be fired asynchronously,
         * we fire them synchronously here, in case that could be useful.
         * If synchronous firing is not desired
         * (for example, if not wanting events to occur synchronously
         * on abrupt shutdown, so that it can complete faster),
         * the user can add asynchronism in its client implementation
         * (or just ignore events if the binding is shut down).
         */
        if (DEBUG) {
            hostLog(this, "close() : runWindowEventLogicLoopOnEvent()");
        }
        try {
            this.runWindowEventLogicLoopOnEvent();
        } catch (Throwable t) {
            if (thr == null) {
                thr = t;
            } else {
                // TODO Java 7+: Can use Throwable.addSuppressed(...).
                //thr.addSuppressed(t);
            }
        }

        if (thr != null) {
            Unchecked.throwIt(thr);
        }
    }

    /*
     * Gesters: getters for tests.
     * Not just making backing methods public,
     * so that they can be located in protected part
     * near next to related methods.
     */

    public boolean gest_isBackingWindowShowing() {
        return this.isBackingWindowShowing();
    }

    public boolean gest_isBackingWindowFocused() {
        return this.isBackingWindowFocused();
    }

    public boolean gest_doesSetBackingWindowIconifiedOrNotWork() {
        return this.doesSetBackingWindowIconifiedOrNotWork();
    }

    public boolean gest_isBackingWindowIconified() {
        return this.isBackingWindowIconified();
    }

    public boolean gest_doesSetBackingWindowMaximizedOrNotWork() {
        return this.doesSetBackingWindowMaximizedOrNotWork();
    }

    public boolean gest_isBackingWindowMaximized() {
        return this.isBackingWindowMaximized();
    }

    /*
     * 
     */
    
    /**
     * Public for tests/debug.
     */
    public void logBackingState() {
        final String stateStr = HostStateUtils.toStringBackingState(
                this.isBackingWindowShowing(),
                this.isBackingWindowFocused(),
                this.isBackingWindowIconified(),
                this.isBackingWindowMaximized());
        hostLog(this, "backing state = " + stateStr);
    }

    /**
     * Public for tests/debug.
     */
    public void logStoredClientState() {
        final String stateStr = HostStateUtils.toStringHostState(
                this.clientWrapper.isShowing(),
                this.clientWrapper.isFocused(),
                this.clientWrapper.isIconified(),
                this.clientWrapper.isMaximized(),
                this.clientWrapper.isMovedPending(),
                this.clientWrapper.isResizedPending(),
                this.clientWrapper.isClosed());
        hostLog(this, "stored client state = " + stateStr);
    }
    
    /**
     * Public for tests/debug.
     */
    public void logInternalState() {
        this.logBackingState();
        
        this.logStoredClientState();
        
        hostLog(this, "mustTryToRestoreClientMaxStateIntoHost = " + this.mustTryToRestoreClientMaxStateIntoHost);
        hostLog(this, "isBoundsEnforcingOnShowDeicoMaxPending = " + this.isBoundsEnforcingOnShowDeicoMaxPending);
        hostLog(this, "isBoundsEnforcingOnShowDeicoDemaxPending = " + this.isBoundsEnforcingOnShowDeicoDemaxPending);
        hostLog(this, "clientBoundsToEnforceOnShowDeicoDemax = " + this.clientBoundsToEnforceOnShowDeicoDemax);
        hostLog(this, "windowBoundsToEnforceOnShowDeicoDemax = " + this.windowBoundsToEnforceOnShowDeicoDemax);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    protected InterfaceWorkerAwareScheduler getUiThreadScheduler() {
        return getBinding().getUiThreadScheduler();
    }

    protected InterfaceClock getUiThreadSchedulerClock() {
        return getUiThreadScheduler().getClock();
    }
    
    /*
     * 
     */

    /**
     * Don't override that, override corresponding xxxImpl methods instead.
     */
    protected final InterfaceHostLifecycleListener<AbstractBwdHost> getDialogLifecycleListener() {
        return this.dialogLifecycleListener;
    }

    /**
     * Overriding implementations must call super.
     */
    protected void onDialogCreatedImpl(AbstractBwdHost dialog) {
        if (this.closed_nonVolatile) {
            throw new IllegalStateException("host is closed: can't create dialog");
        }
        
        if (this.dialogList.contains(dialog)) {
            throw new AssertionError();
        }
        this.dialogList.add(dialog);
        
        // Might throw, so done last.
        this.hostLifecycleListener.onHostCreated(dialog);
    }

    /**
     * Overriding implementations must call super.
     */
    protected void onDialogClosingImpl(AbstractBwdHost dialog) {
        final boolean didRemove = this.dialogList.remove(dialog);
        if (!didRemove) {
            throw new AssertionError();
        }

        this.hostLifecycleListener.onHostClosing(dialog);
    }

    /**
     * Overriding implementations must call super.
     */
    protected void onDialogClosedEventFiringImpl(AbstractBwdHost dialog) {
        this.hostLifecycleListener.onHostClosedEventFiring(dialog);
    }

    /*
     * 
     */

    protected GRect getBackingOrOnDragBeginClientBounds() {
        if (this.bindingConfig.getMustFixBoundsDuringDrag()) {
            if (this.isDragMoveDetected()) {
                final GRect boundsAtDragStart = this.clientBoundsOnDragPress;
                if (DEBUG) {
                    hostLog(this, "returning client bounds at drag start : " + boundsAtDragStart);
                }
                return boundsAtDragStart;
            }
        }
        return this.getBackingClientBounds();
    }

    protected GRect getBackingOrOnDragBeginWindowBounds() {
        if (this.bindingConfig.getMustFixBoundsDuringDrag()) {
            if (this.isDragMoveDetected()) {
                final GRect boundsAtDragStart = this.windowBoundsOnDragPress;
                if (DEBUG) {
                    hostLog(this, "returning window bounds at drag start : " + boundsAtDragStart);
                }
                return boundsAtDragStart;
            }
        }
        return this.getBackingWindowBounds();
    }

    /*
     * 
     */
    
    /*
     * Backing window events listening.
     * 
     * We take backing window state into account through polling,
     * except for bounds and closing, so only calls to bounds
     * and closing callbacks are mandatory.
     * 
     * For flip/flop states (such as showing/hidden), we don't want
     * to rely much on callbacks, because at the time they are called
     * the state might have changed again already, or they might be called
     * slightly before state change, that's why we only use them to run
     * our "event logic" sooner in case it would help to make things faster.
     * 
     * For bounds, we prefer to just rely on callbacks than on polling,
     * to minimize latency in case the user would care about it,
     * not to bother reading backing bounds on each event,
     * and to preserve backing library's quantity of events.
     * TODO These are not strong reasons, and we might want
     * to use polling instead, to make sure bounds events only occur
     * after bounds actually changed, and to ensure an order between
     * MOVED and RESIZED events, and to avoid supernumerary events
     * if using "enlarged" pixels.
     * Could make it configurable, including for mouse moves events.
     */

    /**
     * Should be called (not mandatory) on backing window events, if any,
     * not corresponding to a more specific onBackingWindowXxx() method.
     * 
     * Updates possible state flickering time, and runs event logic
     * (in addition to periodic calls, to make things faster),
     * so it will throw if a client listener throws.
     */
    protected void onBackingWindowAnyEvent() {
        if (DEBUG) {
            hostLog(this, "onBackingWindowAnyEvent()");
        }

        /*
         * Doing it for newest possible unstability time update,
         * which we must do here, and also to eventually fire faster,
         * in particular synchronously, before polling calls.
         */
        this.runWindowEventLogicLoopOnEvent();
    }

    /**
     * Should be called (not mandatory) on backing window shown event, if any.
     */
    protected void onBackingWindowShown() {
        if (DEBUG) {
            hostLog(this, "onBackingWindowShown()");
        }
        this.onBackingWindowAnyEvent();
    }

    /**
     * Should be called (not mandatory) on backing window hidden event, if any.
     */
    protected void onBackingWindowHidden() {
        if (DEBUG) {
            hostLog(this, "onBackingWindowHidden()");
        }
        this.onBackingWindowAnyEvent();
    }

    /**
     * Should be called (not mandatory) on backing window focus gained event, if any.
     */
    protected void onBackingWindowFocusGained() {
        if (DEBUG) {
            hostLog(this, "onBackingWindowFocusGained()");
        }
        this.onBackingWindowAnyEvent();
    }

    /**
     * Should be called (not mandatory) on backing window focus lost event, if any.
     */
    protected void onBackingWindowFocusLost() {
        if (DEBUG) {
            hostLog(this, "onBackingWindowFocusLost()");
        }
        this.onBackingWindowAnyEvent();
    }

    /**
     * Should be called (not mandatory) on backing window iconified event, if any.
     */
    protected void onBackingWindowIconified() {
        if (DEBUG) {
            hostLog(this, "onBackingWindowIconified()");
        }
        this.onBackingWindowAnyEvent();
    }

    /**
     * Should be called (not mandatory) on backing window deiconified event, if any.
     */
    protected void onBackingWindowDeiconified() {
        if (DEBUG) {
            hostLog(this, "onBackingWindowDeiconified()");
        }
        this.onBackingWindowAnyEvent();
    }

    /**
     * Should be called (not mandatory) on backing window maximized event, if any.
     */
    protected void onBackingWindowMaximized() {
        if (DEBUG) {
            hostLog(this, "onBackingWindowMaximized()");
        }
        this.onBackingWindowAnyEvent();
    }

    /**
     * Should be called (not mandatory) on backing window demaximized event, if any.
     */
    protected void onBackingWindowDemaximized() {
        if (DEBUG) {
            hostLog(this, "onBackingWindowDemaximized()");
        }
        this.onBackingWindowAnyEvent();
    }

    /**
     * Must be called on backing window moved event.
     */
    protected void onBackingWindowMoved() {
        if (DEBUG_SPAM) {
            hostLog(this, "onBackingWindowMoved()");
        }

        /*
         * Won't cause expected paintings for libraries that block painting
         * during window move, but that doesn't hurt.
         */
        this.getPaintAfterMoveProcess().start();
        
        this.clientWrapper.setMovedPending();

        this.onBackingWindowAnyEvent();
    }

    /**
     * Must be called on backing window resized event.
     */
    protected void onBackingWindowResized() {
        if (DEBUG_SPAM) {
            hostLog(this, "onBackingWindowResized()");
        }

        /*
         * In case the resize that is also a move,
         * we stop this process, hoping that it didn't execute yet,
         * to try not to cause redundant painting.
         */
        this.getPaintAfterMoveProcess().stop();

        this.getPaintAfterResizeProcess().start();

        this.clientWrapper.setResizedPending();

        this.onBackingWindowAnyEvent();
    }

    /**
     * Must be called on backing window closing and/or closed events.
     */
    protected void onBackingWindowClosing() {
        if (DEBUG) {
            hostLog(this, "onBackingWindowClosing()");
        }
        this.close();
    }

    /*
     * Backing key events listening.
     */

    protected void onBackingKeyPressed(BwdKeyEventPr event) {
        this.keyRepetitionHelper.onKeyPressed(event);
    }

    protected void onBackingKeyTyped(BwdKeyEventT event) {
        this.keyRepetitionHelper.onKeyTyped(event);
    }

    protected void onBackingKeyReleased(BwdKeyEventPr event) {
        this.keyRepetitionHelper.onKeyReleased(event);
    }

    /*
     * Backing mouse events listening.
     * 
     * No method for MOUSE_CLICKED (not to be generated by bindings)
     * nor for MOUSE_DRAGGED (we generate it from MOUSE_MOVED
     * depending on drag button).
     */

    protected void onBackingMousePressed(BwdMouseEvent event) {
        if (this.bindingConfig.getMustFixBoundsDuringDrag()
                && event.hasDragButton()) {
            this.onDragPress();
        }
        this.clientWrapper.onMousePressed(event);
    }

    protected void onBackingMouseReleased(BwdMouseEvent event) {
        try {
            if (this.isDragPressDetected()
                    && event.hasDragButton()) {
                this.onDragEnd();
            }
        } finally {
            this.clientWrapper.onMouseReleased(event);
        }
    }

    protected void onBackingMouseEnteredClient(BwdMouseEvent event) {
        this.clientWrapper.onMouseEnteredClient(event);
    }

    protected void onBackingMouseExitedClient(BwdMouseEvent event) {
        this.clientWrapper.onMouseExitedClient(event);
    }

    /**
     * Does the choice between MOUSE_MOVED and MOUSE_DRAGGED events.
     * 
     * @param event A MOUSE_MOVED event.
     */
    protected void onBackingMouseMoved(BwdMouseEvent event) {
        if (event.isDragButtonDown()) {
            if (this.isDragPressDetected()) {
                this.onDragMove();
            }
            final BwdMouseEvent draggedEvent = event.asMouseDraggedEvent();
            this.clientWrapper.onMouseDragged(draggedEvent);
        } else {
            if (this.isDragPressDetected()) {
                // Useful in case we missed
                // some drag-canceling condition.
                this.onDragCancel();
            }
            this.clientWrapper.onMouseMoved(event);
        }
    }

    /*
     * Backing wheel events listening.
     */

    protected void onBackingWheelEvent(BwdWheelEvent event) {
        this.clientWrapper.onWheelRolled(event);
    }
    
    /*
     * Client event state, i.e. the state observed
     * by the client due to events.
     */

    protected boolean isClientState_Showing() {
        return this.clientWrapper.isShowing();
    }
    
    protected boolean isClientState_focused() {
        return this.clientWrapper.isFocused();
    }
    
    protected boolean isClientState_iconified() {
        return this.clientWrapper.isIconified();
    }
    
    protected boolean isClientState_maximized() {
        return this.clientWrapper.isMaximized();
    }
    
    protected boolean isClientState_closed() {
        return this.clientWrapper.isClosed();
    }

    /*
     * BWD window events hooks for implementation specific treatments.
     * Default implementations do nothing.
     * 
     * Called just before changing client event state
     * and forwarding event to client.
     */
    
    protected void beforeClientWindowShown_spe(BwdWindowEvent event) {
    }
    
    protected void beforeClientWindowHidden_spe(BwdWindowEvent event) {
    }
    
    protected void beforeClientWindowFocusGained_spe(BwdWindowEvent event) {
    }
    
    protected void beforeClientWindowFocusLost_spe(BwdWindowEvent event) {
    }
    
    protected void beforeClientWindowIconified_spe(BwdWindowEvent event) {
    }
    
    protected void beforeClientWindowDeiconified_spe(BwdWindowEvent event) {
    }
    
    protected void beforeClientWindowMaximized_spe(BwdWindowEvent event) {
    }
    
    protected void beforeClientWindowDemaximized_spe(BwdWindowEvent event) {
    }
    
    protected void beforeClientWindowMoved_spe(BwdWindowEvent event) {
    }
    
    protected void beforeClientWindowResized_spe(BwdWindowEvent event) {
    }
    
    protected void beforeClientWindowClosed_spe(BwdWindowEvent event) {
    }

    /*
     * 
     */

    /**
     * If the backing library doesn't repeat-or-not-repeat key events as expected,
     * or even if it does, you can just make them all go through this instead
     * of directly forwarding them to client.
     */
    protected KeyRepetitionHelper getKeyRepetitionHelper() {
        return this.keyRepetitionHelper;
    }

    /**
     * @return A helper class to call client's paintClient() method.
     */
    protected PaintClientHelper getPaintClientHelper() {
        return this.paintClientHelper;
    }

    /*
     * 
     */
    
    /**
     * @return The dirty rectangles bounding box to use for next painting,
     *         whether it has been triggered by the binding or by the
     *         backing library, or an empty rectangle if no dirty rectangles
     *         have been specified.
     */
    protected GRect getAndResetDirtyRectBb() {
        GRect dirtyRectBb;
        synchronized (this.dirtyRectBbMutex) {
            dirtyRectBb = this.dirtyRectBb;
            this.dirtyRectBb = GRect.DEFAULT_EMPTY;
        }
        if (getBindingConfig().getMustMakeAllDirtyAtEachPainting()) {
            dirtyRectBb = GRect.DEFAULT_HUGE;
        }
        return dirtyRectBb;
    }

    /*
     * 
     */

    /**
     * Must be called just before eventually painting client,
     * in particular AFTER call to processEventualBufferedEvents(),
     * which might cause host state change.
     * 
     * A similar treatment can be used after client painting
     * with BWD graphics, to know whether eventual flushing
     * into backing window graphics can be done, in case client
     * changed host state during painting.
     * This could also be done even if the backing library has no issue
     * with flushing while hidden or iconified or closed,
     * for consistency across bindings.
     * 
     * @return True if client painting can be done just now, false otherwise.
     */
    protected boolean canPaintClientNow() {
        /*
         * We don't want to paint if not showing or if iconified, for:
         * - bounds methods might not work, causing improper painting computations.
         * - with some libraries/bindings, painting while window is being closed
         *   cant cause weird issues, in particular about backing window state.
         */
        if (!this.isShowing()) {
            if (DEBUG) {
                hostLog(this, "hidden : not painting");
            }
            return false;
        }
        if (this.isIconified()) {
            if (DEBUG) {
                hostLog(this, "iconified : not painting");
            }
            return false;
        }

        return true;
    }
    
    /*
     * 
     */

    /**
     * Start it on (x,y) change, if you want to cause a corresponding
     * asynchronous repaint.
     */
    protected AbstractProcess getPaintAfterMoveProcess() {
        return this.paintAfterMoveProcess;
    }

    /**
     * Start it on (width,height) change, if you want to cause a corresponding
     * asynchronous repaint.
     */
    protected AbstractProcess getPaintAfterResizeProcess() {
        return this.paintAfterResizeProcess;
    }

    /*
     * 
     */

    /**
     * Called in UI thread.
     * 
     * Not called if isShowing() is false, which could cause issues
     * with some libraries, especially if window has been disposed,
     * but you must still do the canPaintClientNow() check synchronously
     * just before actual painting.
     * 
     * Should only be called from internal paint process,
     * to respect the eventually configured painting delay,
     * but can be called directly from some places of hosts
     * implementations if it allows to avoid some glitches.
     * 
     * The dirty rectangle to use for client.paintClient(...) call
     * must be retrieved with getAndResetDirtyRectBb() only once
     * the painting has been decided, to make sure not to waste it.
     */
    protected abstract void paintClientNowOrLater();

    /**
     * @param windowAlphaFp Always in range.
     */
    protected abstract void setWindowAlphaFp_backing(double windowAlphaFp);

    /*
     * 
     */

    /**
     * Not trusted if closed.
     */
    protected abstract boolean isBackingWindowShowing();

    /**
     * Not called if closed.
     */
    protected abstract void showBackingWindow();

    /**
     * Not called if closed.
     */
    protected abstract void hideBackingWindow();

    /*
     * Focus.
     */
    
    /**
     * Not trusted if not (showing and not iconified).
     */
    protected abstract boolean isBackingWindowFocused();

    /**
     * Not called if not (showing and not iconified).
     */
    protected abstract void requestBackingWindowFocusGain();

    /*
     * Iconified methods.
     * These are only called by this class when the window is showing
     * (unless for debug logs).
     */

    /**
     * @return True if setBackingWindowIconifiedOrNot(boolean) works
     *         for this host, whatever the specified value, false otherwise.
     */
    protected abstract boolean doesSetBackingWindowIconifiedOrNotWork();

    /**
     * Not trusted if not showing.
     */
    protected abstract boolean isBackingWindowIconified();

    /**
     * Not called if not showing.
     * 
     * If not supported, must just do nothing.
     */
    protected abstract void setBackingWindowIconifiedOrNot(boolean desiredIconified);

    /*
     * Maximized methods.
     * These are only called by this class when the window is showing.
     */

    /**
     * @return True if setBackingWindowMaximizedOrNot(boolean) works
     *         for this host, whatever the specified value, false otherwise.
     */
    protected abstract boolean doesSetBackingWindowMaximizedOrNotWork();

    /**
     * Not trusted if not (showing and not iconified).
     */
    protected abstract boolean isBackingWindowMaximized();

    /**
     * Not called if not (showing and not iconified).
     */
    protected abstract void setBackingWindowMaximizedOrNot(boolean desiredMaximized);

    /*
     * Bounds methods.
     * These should only be called when the window is showing,
     * unless you are confident they can work when hidden.
     */

    /**
     * @return {x=left, y=top, xSpan=right, ySpan=bottom} border span
     *         around client area.
     */
    protected abstract GRect getBackingInsets();

    protected abstract GRect getBackingClientBounds();

    protected abstract GRect getBackingWindowBounds();

    protected abstract void setBackingClientBounds(GRect targetClientBounds);

    protected abstract void setBackingWindowBounds(GRect targetWindowBounds);

    /*
     * Closing.
     */

    /**
     * Only called once.
     */
    protected abstract void closeBackingWindow();

    /*
     * Protected methods providing access to otherwise private methods,
     * to help implementing hide/iconify by moving windows out of screen,
     * when the backing library doesn't have these features.
     */
    
    /**
     * @param windowBoundsToEnforce Bounds to enforce to go from hidden or iconified,
     *        back to showing/deico/max.
     */
    protected void applyWindowBoundsToEnforceOnShowDeicoMax_protected(GRect windowBoundsToEnforce) {
        this.applyWindowBoundsToEnforceOnShowDeicoMax(windowBoundsToEnforce);
    }
    
    /**
     * Sets the bounds, and the flag indicating that they must be applied
     * once back to showing/deico/demax.
     * 
     * @param windowBoundsToEnforce Bounds to enforce to go from hidden or iconified,
     *        back to showing/deico/demax.
     */
    protected void setWindowBoundsToEnforceOnShowDeicoDemaxAndFlag_protected(GRect windowBoundsToEnforce) {
        this.setWindowBoundsToEnforceOnShowDeicoDemax(windowBoundsToEnforce);
        this.ensureBoundsEnforcingFlagOnShowDeicoDemax();
    }

    /**
     * To apply bounds to restore when back to showing/deico/demax,
     * synchronously as soon as back in this state.
     * 
     * @return True if did apply.
     */
    protected boolean applyBoundsToEnforceOnShowDeicoDemaxIfAny_protected() {
        return this.applyBoundsToEnforceOnShowDeicoDemaxIfAny();
    }
    
    /*
     * 
     */
    
    /**
     * Default implementation returns false.
     * 
     * @return Whether drag move detection (cf. getMustFixBoundsDuringDrag())
     *         must currently be blocked.
     */
    protected boolean mustBlockDragMoveDetection() {
        return false;
    }

    /*
     * 
     */

    /**
     * @return Something to identify the host in logs.
     */
    protected static Object hid(InterfaceBwdHost host) {
        return host.getTitle();
    }

    /**
     * @return Something to identify the host in logs.
     */
    protected Object hid() {
        return hid(this);
    }

    /**
     * Requires this class DEBUG to be true to work
     * (to make sure this class doesn't depend on Dbg when it's false).
     */
    protected void hostLog(Object caller, Object message) {
        if (DEBUG) {
            Dbg.logPr(caller, hid() + " : " + message);
        }
    }

    /*
     * 
     */

    /**
     * To be called periodically, to check state changes or delays
     * and act accordingly.
     * 
     * Protected for visibility from extending classes, in case of virtual hosts
     * implementations that would have their own event logic process.
     * 
     * @param nowS Must be a freshly retrieved current time,
     *        cf. spec of called method.
     */
    protected void runWindowEventLogicLoopOnPeriod(double nowS) {
        final boolean isPeriodicCall = true;
        this.runWindowEventLogicLoop(
                isPeriodicCall,
                nowS);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static void fillWith(double[] arr, double value) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = value;
        }
    }
    
    /*
     * 
     */

    /**
     * Useful delay to wait, but not too much, before restoring or enforcing
     * some state or bounds, due to some libraries erasing recently set state
     * or bounds with other values due to other events
     * (such as with JavaFX erasing recently set bounds on focus gain).
     */
    private double getHalfStabilityDelayS() {
        return this.bindingConfig.getBackingWindowStateStabilityDelayS() * 0.5;
    }
    
    /*
     * 
     */
    
    private boolean isHostShowingDeico() {
        return this.isShowing()
                && (!this.isIconified());
    }

    private boolean isHostShowingDeicoDemax() {
        return this.isHostShowingDeico()
                && (!this.isMaximized());
    }

    /**
     * Useful to check that (showing, deiconified) state is "stable"
     * (client had time to align on host state,
     * and host didn't change to a new state yet).
     */
    private boolean areHostAndClientShowingDeico() {
        // Quick client checks first.
        return (this.clientWrapper.isShowing() && (!this.clientWrapper.isIconified()))
                && this.isHostShowingDeico();
    }

    private boolean areHostAndClientShowingDeicoDemax() {
        // Quick client checks first.
        return (this.clientWrapper.isShowing() && (!this.clientWrapper.isIconified())) && (!this.clientWrapper.isMaximized())
                && this.isHostShowingDeicoDemax();
    }

    private boolean areHostAndClientShowingDeicoMax() {
        // Quick client checks first.
        return (this.clientWrapper.isShowing() && (!this.clientWrapper.isIconified())) && this.clientWrapper.isMaximized()
                && (this.isHostShowingDeico() && this.isMaximized());
    }
    
    private boolean areHostAndClientShowingDeicoFocused() {
        // Quick client checks first.
        return (this.clientWrapper.isShowing() && (!this.clientWrapper.isIconified())) && this.clientWrapper.isFocused()
                && (this.isHostShowingDeico() && this.isFocused());
    }

    /*
     * 
     */
    
    private boolean isDragPressDetected() {
        return (this.clientBoundsOnDragPress != null);
    }

    private boolean isDragMoveDetected() {
        return this.dragMoveDetected;
    }

    /**
     * Must be called on drag button pressed,
     * if which case window should have or soon have focus.
     * 
     * Separating drag press and drag move detections,
     * not to return special client and window bounds
     * just due to a press without drag, which could
     * cause trouble for example when clicking a window
     * to get its bounds recomputed properly due to
     * some backing library issue.
     */
    private void onDragPress() {
        if (DEBUG) {
            hostLog(this, "onDragPress()");
        }
        /*
         * NB: If window recently hidden for example,
         * bounds should be empty here (could cause glitches?).
         */
        this.clientBoundsOnDragPress = this.getClientBounds();
        this.windowBoundsOnDragPress = this.getWindowBounds();
    }

    private void onDragMove() {
        if (this.mustBlockDragMoveDetection()) {
            if (DEBUG) {
                hostLog(this, "onDragMove() (blocked)");
            }
        } else {
            if (DEBUG) {
                hostLog(this, "onDragMove()");
            }
            this.dragMoveDetected = true;
        }
    }
    
    /**
     * Must be called when drag (drag press detected,
     * with or without drag move detected) must be cancelled,
     * for example on iconified/maximized state change.
     * 
     * Doesn't throw.
     */
    private void onDragCancel() {
        if (DEBUG) {
            hostLog(this, "onDragCancel()");
        }
        
        this.clientBoundsOnDragPress = null;
        this.windowBoundsOnDragPress = null;

        this.dragMoveDetected = false;
        
        this.targetClientBoundsOnDragEnd = null;
        this.targetWindowBoundsOnDragEnd = null;
    }

    private void cancelDragIfDragPressDetected() {
        if (this.isDragPressDetected()) {
            this.onDragCancel();
        }
    }
    
    /**
     * Must be called on mouse drag button released,
     * or when window gets hidden, iconified, or loses focus,
     * but not on mouse exited because it can happen during drag.
     * 
     * Might throw, since might set bounds, which might cause
     * synchronous calls to user code.
     */
    private void onDragEnd() {
        if (DEBUG) {
            hostLog(this, "onDragEnd()");
        }
        
        // Need to be nullified before we set target bounds,
        // for setXxxBounds(...) methods to actually set bounds.
        this.clientBoundsOnDragPress = null;
        this.windowBoundsOnDragPress = null;
        
        final GRect targetClientBounds = this.targetClientBoundsOnDragEnd;
        this.targetClientBoundsOnDragEnd = null;

        final GRect targetWindowBounds = this.targetWindowBoundsOnDragEnd;
        this.targetWindowBoundsOnDragEnd = null;

        // Taking care not to mess up bounds
        // if no drag move was detected.
        if (this.dragMoveDetected) {
            this.dragMoveDetected = false;
            
            if (targetClientBounds != null) {
                this.setClientBounds(targetClientBounds);
            } else if (targetWindowBounds != null) {
                this.setWindowBounds(targetWindowBounds);
            }
        }
    }

    /**
     * Nullifies targetWindowBoundsOnDragEnd.
     * @param targetClientBounds Must not be null.
     */
    private void setTargetClientBoundsOnDragEnd(GRect targetClientBounds) {
        LangUtils.requireNonNull(targetClientBounds);
        this.targetClientBoundsOnDragEnd = targetClientBounds;
        this.targetWindowBoundsOnDragEnd = null;
    }

    /**
     * Nullifies targetClientBounds.
     * @param targetWindowBoundsOnDragEnd Must not be null.
     */
    private void setTargetWindowBoundsOnDragEnd(GRect targetClientBounds) {
        LangUtils.requireNonNull(targetClientBounds);
        this.targetClientBoundsOnDragEnd = null;
        this.targetWindowBoundsOnDragEnd = targetClientBounds;
    }
    
    /*
     * Max bounds enforcing.
     */

    private void ensureBoundsEnforcingFlagOnShowDeicoMaxIfConfigured() {
        if (this.bindingConfig.getMustEnforceBoundsOnShowDeicoMax()) {
            this.ensureBoundsEnforcingFlagOnShowDeicoMax();
        }
    }
    
    private void ensureBoundsEnforcingFlagOnShowDeicoMax() {
        if (!this.isBoundsEnforcingOnShowDeicoMaxPending) {
            if (DEBUG) {
                hostLog(this, "isBoundsEnforcingOnShowDeicoMaxPending set to true");
            }
            this.isBoundsEnforcingOnShowDeicoMaxPending = true;
        }
    }
    
    private void clearBoundsEnforcingFlagOnShowDeicoMax() {
        if (this.isBoundsEnforcingOnShowDeicoMaxPending) {
            if (DEBUG) {
                hostLog(this, "isBoundsEnforcingOnShowDeicoMaxPending set to false");
            }
            this.isBoundsEnforcingOnShowDeicoMaxPending = false;
        }
    }

    private void applyWindowBoundsToEnforceOnShowDeicoMax(GRect windowBoundsToEnforce) {
        this.clearBoundsEnforcingFlagOnShowDeicoMax();

        if (DEBUG) {
            hostLog(this, "show/deico/max : setting target window bounds : " + windowBoundsToEnforce);
        }
        this.setBackingWindowBounds(windowBoundsToEnforce);
    }

    /*
     * Demax bounds enforcing.
     */
    
    private void ensureBoundsEnforcingFlagOnShowDeicoDemaxIfConfiguredAndHaveAny() {
        if (this.bindingConfig.getMustRestoreBoundsOnShowDeicoDemax()
                && this.haveBoundsToEnforceOnShowDeicoDemax()) {
            this.ensureBoundsEnforcingFlagOnShowDeicoDemax();
        }
    }
    
    private void ensureBoundsEnforcingFlagOnShowDeicoDemax() {
        if (!this.isBoundsEnforcingOnShowDeicoDemaxPending) {
            if (DEBUG) {
                hostLog(this, "isBoundsEnforcingOnShowDeicoDemaxPending set to true");
            }
            this.isBoundsEnforcingOnShowDeicoDemaxPending = true;
        }
    }
    
    private void clearBoundsEnforcingFlagOnShowDeicoDemax() {
        if (this.isBoundsEnforcingOnShowDeicoDemaxPending) {
            if (DEBUG) {
                hostLog(this, "isBoundsEnforcingOnShowDeicoDemaxPending set to false");
            }
            this.isBoundsEnforcingOnShowDeicoDemaxPending = false;
        }
    }
    
    private boolean haveBoundsToEnforceOnShowDeicoDemax() {
        return (this.clientBoundsToEnforceOnShowDeicoDemax != null)
                || (this.windowBoundsToEnforceOnShowDeicoDemax != null);
    }
    
    /**
     * Clears bounds to enforce and enforcing flag.
     */
    private void clearBoundsEnforcingOnShowDeicoDemax() {
        this.clearBoundsEnforcingFlagOnShowDeicoDemax();
        
        this.clientBoundsToEnforceOnShowDeicoDemax = null;
        this.windowBoundsToEnforceOnShowDeicoDemax = null;
    }
    
    private void setClientBoundsToEnforceOnShowDeicoDemax(GRect clientBoundsToEnforce) {
        LangUtils.requireNonNull(clientBoundsToEnforce);
        
        if (DEBUG) {
            hostLog(this, "clientBoundsToEnforceOnShowDeicoDemax set to " + clientBoundsToEnforce);
        }
        this.clientBoundsToEnforceOnShowDeicoDemax = clientBoundsToEnforce;
        this.windowBoundsToEnforceOnShowDeicoDemax = null;
    }
    
    private void setWindowBoundsToEnforceOnShowDeicoDemax(GRect windowBoundsToEnforce) {
        LangUtils.requireNonNull(windowBoundsToEnforce);
        
        if (DEBUG) {
            hostLog(this, "windowBoundsToEnforceOnShowDeicoDemax set to " + windowBoundsToEnforce);
        }
        this.clientBoundsToEnforceOnShowDeicoDemax = null;
        this.windowBoundsToEnforceOnShowDeicoDemax = windowBoundsToEnforce;
    }
    
    private void setClientBoundsToEnforceOnShowDeicoDemaxWithCurrent() {
        // NB: Could also just use backing bounds directly.
        final GRect clientBounds = this.getBackingOrOnDragBeginClientBounds();
        if ((!clientBounds.isEmpty())
                && (!clientBounds.equals(this.clientBoundsToEnforceOnShowDeicoDemax))) {
            if (DEBUG) {
                hostLog(this, "setting clientBoundsToEnforceOnShowDeicoDemax set to current client bounds");
            }
            this.setClientBoundsToEnforceOnShowDeicoDemax(clientBounds);
        }
    }
    
    /**
     * Also clears bounds to enforce and enforcing flag.
     * 
     * @throws IllegalStateException if there are no bounds to apply.
     */
    private void applyBoundsToEnforceOnShowDeicoDemax() {
        final GRect targetClientBounds = this.clientBoundsToEnforceOnShowDeicoDemax;
        final GRect targetWindowBounds = this.windowBoundsToEnforceOnShowDeicoDemax;
        
        this.clearBoundsEnforcingOnShowDeicoDemax();
        
        // Only supposed to have one non-null.
        // If for some reason both are non-null,
        // priority to client bounds.
        if (targetClientBounds != null) {
            if (DEBUG) {
                hostLog(this, "show/deico/demax : setting client bounds : " + targetClientBounds);
            }
            this.setBackingClientBounds(targetClientBounds);
        } else if (targetWindowBounds != null) {
            if (DEBUG) {
                hostLog(this, "show/deico/demax : setting window bounds : " + targetWindowBounds);
            }
            this.setBackingWindowBounds(targetWindowBounds);
        } else {
            throw new IllegalStateException("no bounds to apply");
        }
    }
    
    /**
     * @return True if did apply.
     */
    private boolean applyBoundsToEnforceOnShowDeicoDemaxIfAny() {
        final boolean gotSome = this.haveBoundsToEnforceOnShowDeicoDemax();
        if (gotSome) {
            this.applyBoundsToEnforceOnShowDeicoDemax();
        }
        return gotSome;
    }
    
    /*
     * 
     */
    
    private static GRect sanitizedTargetBounds(GRect rect) {
        
        // Implicit null check.
        rect = rect.trimmed();
        
        /*
         * Making sure no span is zero, else area might disappear
         * (even if is decorated, with some minimum spans!)
         * and user get mad.
         */

        if (rect.xSpan() == 0) {
            rect = rect.withXSpan(1);
        }
        if (rect.ySpan() == 0) {
            rect = rect.withYSpan(1);
        }
        
        return rect;
    }

    private GRect getXxxBounds(boolean clientElseWindow) {
        if (clientElseWindow) {
            return this.getClientBounds();
        } else {
            return this.getWindowBounds();
        }
    }

    private void setTargetXxxBounds(
            boolean clientElseWindow,
            //
            GRect targetBounds) {
        if (clientElseWindow) {
            this.setClientBounds(targetBounds);
        } else {
            this.setWindowBounds(targetBounds);
        }
    }

    private boolean setXxxBoundsSmart(
            boolean clientElseWindow,
            //
            GRect targetBounds,
            boolean fixRight,
            boolean fixBottom,
            boolean restoreBoundsIfNotExact) {

        // Implicit null check.
        targetBounds = sanitizedTargetBounds(targetBounds);

        if (!this.isHostShowingDeicoDemax()) {
            return false;
        }

        /*
         * 
         */

        final GRect oldBounds = this.getXxxBounds(clientElseWindow);

        if (oldBounds.equals(targetBounds)) {
            // Nothing to do.
            return false;
        }

        /*
         * Specified bounds spans are ensured to be >= 0, otherwise
         * it could cause weird "quiet" behaviors, such as with AWT or Swing.
         */
        
        this.setTargetXxxBounds(
                clientElseWindow,
                //
                targetBounds);

        GRect newBounds = this.getXxxBounds(clientElseWindow);

        /*
         * 
         */

        boolean didReworkNewBounds = false;

        // Could as well just use >.
        if (fixRight && (newBounds.xMax() != oldBounds.xMax())) {
            // Assuming it would succeed.
            newBounds = newBounds.withPosDeltas(-(newBounds.xMax() - oldBounds.xMax()), 0);
            didReworkNewBounds = true;
        }

        // Could as well just use >.
        if (fixBottom && (newBounds.yMax() != oldBounds.yMax())) {
            // Assuming it would succeed.
            newBounds = newBounds.withPosDeltas(0, -(newBounds.yMax() - oldBounds.yMax()));
            didReworkNewBounds = true;
        }

        if (restoreBoundsIfNotExact && (!newBounds.equals(targetBounds))) {
            // Assuming it would succeed.
            newBounds = oldBounds;
            didReworkNewBounds = true;
        }

        if (didReworkNewBounds) {
            if (DEBUG) {
                Dbg.log("***************** DID REWORK NEW BOUNDS ************");
            }
            this.setTargetXxxBounds(
                    clientElseWindow,
                    //
                    newBounds);
        }

        return !newBounds.equals(oldBounds);
    }

    /*
     * Event logic.
     */

    private void createAndFireWindowEvent(BwdEventType eventType) {
        final BwdWindowEvent event = new BwdWindowEvent(this, eventType);
        if (DEBUG) {
            hostLog(this, "firing " + event);
        }
        BwdEventListenerUtils.callProperMethod(this.clientWindowEventWrapper, event);
    }

    /**
     * Two reasons to run again: more firing might have to be done already,
     * or new firing might have to be done due to having called user listeners,
     * which might have triggered event changes.
     */
    private void setMustRunAgain() {
        this.mustRunWindowEventLogicAgain = true;
    }

    private void setMust_NOT_RunAgain() {
        this.mustRunWindowEventLogicAgain = false;
    }

    /**
     * @return True if must stop logic run here.
     */
    private boolean flush_MOVED_or_RESIZED() {
        if (this.clientWrapper.isMovedPending()) {
            this.logInternalStateIfDebug();
            if (DEBUG) {
                hostLog(this, "EVENT LOGIC : window moved, client bounds = " + this.getClientBounds());
            }
            this.setMustRunAgain();
            // Allowed to throw.
            this.createAndFireWindowEvent(BwdEventType.WINDOW_MOVED);
            return true;
        }
        if (this.clientWrapper.isResizedPending()) {
            this.logInternalStateIfDebug();
            if (DEBUG) {
                hostLog(this, "EVENT LOGIC : window resized, client bounds = " + this.getClientBounds());
            }
            this.setMustRunAgain();
            // Allowed to throw.
            this.createAndFireWindowEvent(BwdEventType.WINDOW_RESIZED);
            return true;
        }
        return false;
    }

    private void logInternalStateIfDebug() {
        if (DEBUG) {
            this.logInternalState();
        }
    }
    
    /*
     * 
     */

    /**
     * @return True if (nowS - timeS) < minDelayS, false otherwise.
     */
    private boolean isTooEarly(
            double nowS,
            double timeS,
            double minDelayS) {
        final double dtS = nowS - timeS;
        // "<" so that always false if threshold is zero.
        final boolean tooEarly = (dtS < minDelayS);
        if (DEBUG_SPAM) {
            final String supOrInfEq = (tooEarly ? "<" : ">=");
            hostLog(this,
                    "isTooEarly(...) : dtS = " + dtS + " " + supOrInfEq
                    + " " + minDelayS + " (dtS = " + nowS + " - " + timeS + ")");
        }
        return tooEarly;
    }
    
    /*
     * 
     */
    
    /**
     * Must be called when we programmatically modify backing state.
     */
    private void beforeBackingStateProgMod(BwdEventType eventType) {
        /*
         * Backing state modification might cause unstability.
         */
        this.setNewestPossibleUnstabilityTime();

        this.eventuallyResetMustTryToRestoreMaxDemaxOnProgMod(eventType);
        
        final BwdEventType oppEventType = eventType.opposite();
        LangUtils.requireNonNull(oppEventType);
        
        this.pendingBackingStateProgModByOrdinal[eventType.ordinal()] = true;
        this.pendingBackingStateProgModByOrdinal[oppEventType.ordinal()] = false;
    }
    
    /**
     * Must be called after programmatic modifications
     * of backing window state or bounds.
     */
    protected void afterBackingStateProgMod() {
        if (MUST_RUN_EVENT_LOGIC_AFTER_PROG_BACK_MOD) {
            /*
             * This also updates newest possible unstability time,
             * and we want that, even though we "just" did it
             * before backing state modification, because it
             * might have taken some time.
             */
            this.runWindowEventLogicLoopOnEvent();
        }
    }
    
    private void resetEventFlagsOnFiring(BwdEventType eventType) {
        final int ord = eventType.ordinal();
        this.backingStateDetectedAndNotFiredByOrdinal[ord] = false;
        this.pendingBackingStateProgModByOrdinal[ord] = false;
    }
    
    private void eventuallyResetMustTryToRestoreMaxDemaxOnProgMod(BwdEventType eventType) {
        if (this.mustTryToRestoreClientMaxStateIntoHost) {
            if (this.clientWrapper.isMaximized()) {
                if (eventType == BwdEventType.WINDOW_DEMAXIMIZED) {
                    if (DEBUG) {
                        hostLog(this, "mustTryToRestoreClientMaxStateIntoHost set to false (programmatic demaximize)");
                    }
                    this.mustTryToRestoreClientMaxStateIntoHost = false;
                }
            } else {
                if (eventType == BwdEventType.WINDOW_MAXIMIZED) {
                    if (DEBUG) {
                        hostLog(this, "mustTryToRestoreClientMaxStateIntoHost set to false (programmatic maximize)");
                    }
                    this.mustTryToRestoreClientMaxStateIntoHost = false;
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private double getStateStabilityDelayToUseS(BwdEventType eventType) {
        final double ret;
        if (eventType == BwdEventType.WINDOW_HIDDEN) {
            ret = this.bindingConfig.getBackingWindowHiddenStabilityDelayS();
            
        } else if ((eventType == BwdEventType.WINDOW_ICONIFIED)
                || (eventType == BwdEventType.WINDOW_DEICONIFIED)
                || (eventType == BwdEventType.WINDOW_MAXIMIZED)
                || (eventType == BwdEventType.WINDOW_DEMAXIMIZED)) {
            ret = this.bindingConfig.getBackingWindowStateStabilityDelayS();
        } else {
            ret = 0.0;
        }
        return ret;
    }

    /**
     * @return Corresponding backing event name.
     */
    private static String tben(BwdEventType eventType) {
        String tmp = eventType.name();
        // A bit hacky, but only used for debug.
        tmp = tmp.substring("WINDOW_".length());
        tmp = tmp.toLowerCase();
        return tmp;
    }
    
    /**
     * To be called in event logic,
     * when backing state and client state differ.
     * 
     * @return True if must ignore the new backing state for now.
     */
    private boolean eventLogicOnWindowStateDiff(
            double nowS,
            //
            boolean mustBlockFlickering,
            boolean mustIgnoreBackingStateWhileUnstable,
            //
            double stateStabilityDelayS,
            //
            BwdEventType eventType) {
        
        final BwdEventType oppositeEventType = eventType.opposite();
        LangUtils.requireNonNull(oppositeEventType);
        
        final int ord = eventType.ordinal();
        final int oppOrd = oppositeEventType.ordinal();
        
        if (mustBlockFlickering) {
            if (this.backingStateDetectedAndNotFiredByOrdinal[oppOrd]) {
                if (DEBUG) {
                    hostLog(this,
                            "EVENT LOGIC : ignoring backing " + tben(eventType)
                            + " : opposite state detected and not yet fired");
                }
                return true;
            }
            final double antiFlickeringDelayS = this.bindingConfig.getBackingWindowStateAntiFlickeringDelayS();
            final double dtS = nowS - this.newestBackingStateDiffDetTimeSByOrdinal[oppOrd];
            if (dtS < antiFlickeringDelayS) {
                if (DEBUG) {
                    hostLog(this,
                            "EVENT LOGIC : ignoring backing " + tben(eventType)
                            + " : opposite state detected too recently");
                }
                return true;
            }
        } else {
            this.backingStateDetectedAndNotFiredByOrdinal[oppOrd] = false;
        }
        
        /*
         * Only "detecting and considering as to be fired"
         * after eventual blocking, to make sure opposite
         * "detected and to be fired" booleans are not both true.
         */
        final boolean justDetected;
        if (!this.backingStateDetectedAndNotFiredByOrdinal[ord]) {
            if (DEBUG) {
                hostLog(this,
                        "detected state diff, backing is " + eventType
                        + ", client is " + oppositeEventType);
            }
            this.backingStateDetectedAndNotFiredByOrdinal[ord] = true;
            this.newestBackingStateDiffDetTimeSByOrdinal[ord] = nowS;
            
            this.setNewestPossibleUnstabilityTime();
            
            justDetected = true;
        } else {
            justDetected = false;
        }

        if ((!mustIgnoreBackingStateWhileUnstable)
                || (!(stateStabilityDelayS > 0.0))) {
            return false;
        }
        
        final boolean havePendingProgMod = this.pendingBackingStateProgModByOrdinal[ord];
        if (DEBUG) {
            hostLog(this, "havePendingProgMod = " + havePendingProgMod);
        }
        if (havePendingProgMod) {
            if (DEBUG) {
                hostLog(this,
                        "EVENT LOGIC : using backing " + tben(eventType)
                        + " without delay : programmatic modification pending");
            }
            return false;
        }
        
        if (justDetected) {
            if (DEBUG) {
                hostLog(this,
                        "EVENT LOGIC : ignoring backing " + tben(eventType)
                        + " : just detected");
            }
            return true;
        }

        final double dtFromDetS = nowS - this.newestBackingStateDiffDetTimeSByOrdinal[ord];
        if (DEBUG) {
            hostLog(this, "dtFromDetS = " + dtFromDetS);
        }
        if (dtFromDetS < stateStabilityDelayS) {
            if (DEBUG) {
                hostLog(this,
                        "EVENT LOGIC : ignoring backing " + tben(eventType)
                        + " : detected too recently");
            }
            return true;
        }

        if (mustIgnoreBackingStateWhileUnstable) {
            if (this.isTooEarly(
                    nowS,
                    this.newestPossibleUnstabilityTimeS,
                    stateStabilityDelayS)) {
                if (DEBUG) {
                    hostLog(this,
                            "EVENT LOGIC : ignoring backing " + tben(eventType)
                            + " : window state still unstable");
                }
                return true;
            }
        }
        
        /*
         * Special case: with some libraries, on iconification,
         * window gets demaximized first, possibly a long time
         * before iconified flag turns true, which might make us
         * fire DEMAX on iconification.
         * 
         * To avoid this, at least in case of programmatic iconification,
         * we block DEMAX if programmatic iconification is pending.
         * NB: That means we ignore demaximize() calls while iconification
         * is pending, but that could be considered as being in spec,
         * since we must ignore them while iconified.
         * 
         * Also, for safety and consistency, we extend this blocking logic
         * to block both MAX and DEMAX while ICO or DEICO are pending,
         * while should not hurt.
         */
        if ((eventType == BwdEventType.WINDOW_MAXIMIZED)
                || (eventType == BwdEventType.WINDOW_DEMAXIMIZED)) {
            if (this.pendingBackingStateProgModByOrdinal[BwdEventType.WINDOW_ICONIFIED.ordinal()]
                    || this.pendingBackingStateProgModByOrdinal[BwdEventType.WINDOW_DEICONIFIED.ordinal()]) {
                if (DEBUG) {
                    hostLog(this,
                            "EVENT LOGIC : ignoring backing " + tben(eventType)
                            + " : programmatic (de)iconification is pending.");
                }
                return true;
            }
        }
        /*
         * Also blocking ICO/DEICO if programmatic DEICO/ICO are pending.
         */
        if ((eventType == BwdEventType.WINDOW_ICONIFIED)
                && this.pendingBackingStateProgModByOrdinal[BwdEventType.WINDOW_DEICONIFIED.ordinal()]) {
            if (DEBUG) {
                hostLog(this,
                        "EVENT LOGIC : ignoring backing " + tben(eventType)
                        + " : programmatic deiconification is pending.");
            }
            return true;
        }
        if ((eventType == BwdEventType.WINDOW_DEICONIFIED)
                && this.pendingBackingStateProgModByOrdinal[BwdEventType.WINDOW_ICONIFIED.ordinal()]) {
            if (DEBUG) {
                hostLog(this,
                        "EVENT LOGIC : ignoring backing " + tben(eventType)
                        + " : programmatic iconification is pending.");
            }
            return true;
        }

        return false;
    }

    /*
     * 
     */

    private void eventuallyRestoreClientMaxStateIntoHost(double nowS) {
        if (!this.mustTryToRestoreClientMaxStateIntoHost) {
            return;
        }

        if (!this.areHostAndClientShowingDeico()) {
            if (DEBUG) {
                hostLog(this, "eventuallyRestoreClientMaxStateIntoHost : host and client no longer showing and deiconified : aborting.");
            }
            // State changed, too late.
            // Aborting for good for now.
            // Will be restored next time we get showing+deiconified.
            this.mustTryToRestoreClientMaxStateIntoHost = false;
            return;
        }

        /*
         * Not restoring state immediately, which with some libraries
         * might not work properly, for example by only being effective
         * for a short time if state another modification is initiated
         * afterwards (such as, on Mac with AWT, a focus gain request).
         * 
         * We also do this check before eventually aborting restoration
         * due to current state being target state, not to do it
         * taking a possibly transient current state into account.
         */
        if (this.isTooEarly(
                nowS,
                this.newestPossibleUnstabilityTimeS,
                this.getHalfStabilityDelayS())) {
            if (DEBUG) {
                hostLog(this,
                        "eventuallyRestoreClientMaxStateIntoHost : too early to restore state");
            }
            return;
        }

        // Can read backing state since host showing and deiconified.
        final boolean hostMaximized = this.isBackingWindowMaximized();
        final boolean clientMaximized = this.clientWrapper.isMaximized();
        if (hostMaximized == clientMaximized) {
            if (DEBUG) {
                hostLog(this,
                        "eventuallyRestoreClientMaxStateIntoHost : host and client have same state ("
                                + clientMaximized + ") : aborting.");
            }
            this.mustTryToRestoreClientMaxStateIntoHost = false;
            return;
        }
        
        if (clientMaximized) {
            if (DEBUG) {
                hostLog(this, "calling maximize() to restore state");
            }
            this.maximize();
        } else {
            if (DEBUG) {
                hostLog(this, "calling demaximize() to restore state");
            }
            this.demaximize();
        }
    }

    /*
     * 
     */
    
    private void eventuallyEndDrag() {
        if (this.isDragPressDetected()) {
            // Shouldn't hurt to require both host and client states
            // to be appropriate: we want to end as soon as something
            // gets hairy.
            if (!this.areHostAndClientShowingDeicoFocused()) {
                this.onDragEnd();
            }
        }
    }

    /*
     * 
     */
    
    private void eventuallySetOrUseTargetDemaxBoundsIfAny(double nowS) {
        final boolean boundsEnforcingOnShowDeicoDemaxPending =
                this.isBoundsEnforcingOnShowDeicoDemaxPending;
        final boolean mustStoreCurrentDemaxBoundsForRestoration =
                this.bindingConfig.getMustRestoreBoundsOnShowDeicoDemax();
        if ((!boundsEnforcingOnShowDeicoDemaxPending)
                && (!mustStoreCurrentDemaxBoundsForRestoration)) {
            // No need to go further.
            return;
        }
        
        if (!this.areHostAndClientShowingDeicoDemax()) {
            return;
        }
        
        if (this.isTooEarly(
                nowS,
                this.newestPossibleUnstabilityTimeS,
                this.getHalfStabilityDelayS())) {
            // Means too early for stable too.
            return;
        }
        
        if (boundsEnforcingOnShowDeicoDemaxPending) {
            if (DEBUG) {
                hostLog(this, "eventually(...) : boundsEnforcingOnShowDeicoDemaxPending = true");
            }
            
            this.applyBoundsToEnforceOnShowDeicoDemax();
            
            /*
             * No need to try to store, during this call,
             * current client bounds to restore on demax,
             * and even need not to, since we did just set bounds,
             * potentially causing some instability.
             */
            return;
        }

        /*
         * Doing it last to make sure we use eventual target bounds
         * set by user before eventually storing newest current bounds
         * (for restoration on demax) into the field.
         */
        
        if (mustStoreCurrentDemaxBoundsForRestoration) {
            if (this.isTooEarly(
                    nowS,
                    this.newestPossibleUnstabilityTimeS,
                    this.bindingConfig.getBackingWindowStateStabilityDelayS())) {
                return;
            }
            
            this.setClientBoundsToEnforceOnShowDeicoDemaxWithCurrent();
        }
    }
    
    /*
     * 
     */
    
    private void eventuallyUseTargetMaxBoundsIfPending(double nowS) {
        if (!this.isBoundsEnforcingOnShowDeicoMaxPending) {
            return;
        }
        
        if (!this.areHostAndClientShowingDeicoMax()) {
            return;
        }
        
        if (this.isTooEarly(
                nowS,
                this.newestPossibleUnstabilityTimeS,
                this.getHalfStabilityDelayS())) {
            return;
        }
        
        // Using fresh screen bounds.
        final GRect windowBoundsToEnforce = getBinding().getScreenBounds();
        
        this.applyWindowBoundsToEnforceOnShowDeicoMax(windowBoundsToEnforce);
    }
    
    /*
     * 
     */
    
    private void setNewestPossibleUnstabilityTime() {
        final double nowS = this.getUiThreadSchedulerClock().getTimeS();
        this.setNewestPossibleUnstabilityTime(nowS);
    }
    
    private void setNewestPossibleUnstabilityTime(double nowS) {
        this.newestPossibleUnstabilityTimeS = nowS;
        if (DEBUG) {
            hostLog(this, "newestPossibleUnstabilityTimeS set to " + nowS);
            if (DEBUG_SPAM) {
                Dbg.logPr(this, new RuntimeException("for stack"));
            }
        }
    }
    
    /*
     * 
     */

    /**
     * Also sets newest possible unstability time,
     * to factor clock call.
     */
    private void runWindowEventLogicLoopOnEvent() {
        final boolean isPeriodicCall = false;
        
        final double nowS = this.getUiThreadSchedulerClock().getTimeS();
        
        /*
         * Event might not be itself an unstability,
         * but it might be a sign of some unstability,
         * or correspond to a time where unstabilities
         * get initiated.
         */
        this.setNewestPossibleUnstabilityTime(nowS);
        
        this.runWindowEventLogicLoop(
                isPeriodicCall,
                nowS);
    }
    
    private void onWindowEventLogicLoopCall(
            boolean isPeriodicCall,
            double nowS) {
        /*
         * We are supposed to pass here frequently.
         * We measure the delay from last call, as a way to detect stalls
         * (due to GC, or the backing library being busy, or something else
         * holding UI thread or the CPU, etc.), and update unstability time
         * accordingly, not to consider too early that the state already
         * stabilized without having to use a huge stability time.
         */
        final double previousPeriodicCallTimeS = this.newestWindowEventLogicLoopPeriodicCallTimeS;
        if (isPeriodicCall) {
            this.newestWindowEventLogicLoopPeriodicCallTimeS = nowS;
        }

        final double oldUnstabTimeS = this.newestPossibleUnstabilityTimeS;
        
        if ((previousPeriodicCallTimeS == Double.NEGATIVE_INFINITY)
                || (oldUnstabTimeS == Double.NEGATIVE_INFINITY)) {
            // Too early to bother.
            return;
        }
        
        final double dtS = (nowS - previousPeriodicCallTimeS);
        final double theoreticalDtS = this.getBindingConfig().getWindowEventLogicPeriodS();
        final double latenessS = (dtS - theoreticalDtS);
        
        /*
         * For threshold, using theoretical period,
         * plus one tenth of stability delay.
         * This guard against too frequent and useless stall detections
         * (which doesn't hurt, but can spam when using debug)
         * when either of them is small compared to the other.
         * Ex.: With our AWT, Swing, SWT, LWJGL3 and Qt4 bindings,
         * possibly due to using these libraries asynchronous execution
         * methods in our UI thread schedulers, we often are about 10ms late,
         * which is our default theoretical period.
         */
        final double oneTenthOfStabDelayS = (1.0/10) * this.getBindingConfig().getBackingWindowStateStabilityDelayS();
        final double latenessThresholdS = theoreticalDtS + oneTenthOfStabDelayS;
        if (latenessS > latenessThresholdS) {
            /*
             * NB: Could also just set to nowS, but it might be overkill,
             * and postpone actions too much in case of busy CPU.
             */
            final double newUnstabTimeS = Math.min(nowS, oldUnstabTimeS + latenessS);
            if (DEBUG) {
                hostLog(this,
                        "stall detected (lateness = " + latenessS
                        + " s) : newestPossibleUnstabilityTimeS set to " + newUnstabTimeS
                        + " (old = " + oldUnstabTimeS + ")");
            }
            this.newestPossibleUnstabilityTimeS = newUnstabTimeS;
        }
    }
    
    /**
     * Using the specified current time as current time during the whole
     * method call, even though it might do a lot of things and take
     * much time (typically due to stalls in the backing library),
     * after having done stall detection based on it, with eventual
     * corresponding newest possible unstability time increase.
     * 
     * Indeed, we need to do stall detection and eventual unstability time
     * increase after each current time retrieval, else, just after stall
     * corresponding to some unstability, we might wrongly think
     * that we are late enough for state to be considered stable,
     * and make bad moves.
     * Also, we want not to call the clock too often.
     * And last, it doesn't hurt to use a somewhat old current time:
     * at worse, we might wrongly think that it's too early to take actions,
     * and postpone them to some later call.
     * 
     * @param nowS Must be a freshly retrieved current time,
     *        in particular nothing must have occurred in
     *        current (and UI) thread in the backing library
     *        since it has been retrieved, for it'll be used
     *        to detect stalls.
     */
    private void runWindowEventLogicLoop(
            boolean isPeriodicCall,
            double nowS) {
        
        this.onWindowEventLogicLoopCall(
                isPeriodicCall,
                nowS);
        
        do {
            this.runWindowEventLogicOnce(nowS);
        } while (this.mustRunWindowEventLogicAgain);
        
        this.eventuallyRestoreClientMaxStateIntoHost(nowS);
        
        this.eventuallyEndDrag();
        
        this.eventuallySetOrUseTargetDemaxBoundsIfAny(nowS);
        
        this.eventuallyUseTargetMaxBoundsIfPending(nowS);
    }
    
    /*
     * 
     */

    /**
     * Must set mustRunWindowEventLogicAgain to true if fires an event,
     * to false otherwise, which allows to be robust to client listener
     * throwing.
     */
    private void runWindowEventLogicOnce(double nowS) {
        if (DEBUG_SPAM) {
            hostLog(this, "runWindowEventLogicOnce()");
        }
        this.setMust_NOT_RunAgain();

        if (this.clientWrapper.isClosed()) {
            /*
             * Might happen when the backing library causes calls to
             * onBackingWindowXxx() methods after we fired CLOSED event.
             */
            if (DEBUG) {
                hostLog(this, "EVENT LOGIC : WARN : already closed");
            }
            /*
             * Making sure we won't try to fire anything more.
             */
            return;
        }

        /*
         * Returning after each event firing, to make sure we always
         * use fresh state, for user might have triggered arbitrary
         * state change and even firing in its listener, possibly
         * synchronously.
         */

        // Flushing MOVED or RESIZED even if close() has been called,
        // which should not hurt as long as happens before CLOSED event.
        if (this.clientWrapper.isShowing()
                && (!this.clientWrapper.isIconified())) {
            if (this.flush_MOVED_or_RESIZED()) {
                return;
            }
        }

        if (this.closed_nonVolatile) {
            /*
             * We want to ensure FOCUS_LOST and then HIDDEN events,
             * if needed, before CLOSED.
             */
            if (this.clientWrapper.isFocused()) {
                if (this.isBackingWindowFocused()) {
                    if (DEBUG) {
                        hostLog(this, "EVENT LOGIC : backing window closing, but still focused : not firing focus lost yet");
                    }
                    return;
                }
                this.logInternalStateIfDebug();
                if (DEBUG) {
                    hostLog(this, "EVENT LOGIC : focus lost (before closed)");
                }
                this.setMustRunAgain();
                // Allowed to throw.
                this.createAndFireWindowEvent(BwdEventType.WINDOW_FOCUS_LOST);
                return;
            }
            if (this.clientWrapper.isShowing()) {
                final boolean backingWindowShowing = this.isBackingWindowShowing();
                if (backingWindowShowing) {
                    if (DEBUG) {
                        hostLog(this, "EVENT LOGIC : backing window closing, but still showing : not firing hidden yet");
                    }
                    return;
                }
                this.logInternalStateIfDebug();
                if (DEBUG) {
                    hostLog(this, "EVENT LOGIC : hidden (before closed)");
                }
                this.setMustRunAgain();
                // Allowed to throw.
                this.createAndFireWindowEvent(BwdEventType.WINDOW_HIDDEN);
                return;
            }
            this.logInternalStateIfDebug();
            if (DEBUG) {
                hostLog(this, "EVENT LOGIC : closed");
            }
            // After firing CLOSED, we're done.
            this.setMust_NOT_RunAgain();
            this.hostLifecycleListener.onHostClosedEventFiring(this);
            // Allowed to throw.
            this.createAndFireWindowEvent(BwdEventType.WINDOW_CLOSED);
            return;
        }

        // Callable if close() not called.
        final boolean backingShowing = this.isBackingWindowShowing();
        if (backingShowing != this.clientWrapper.isShowing()) {
            if (backingShowing) {
                final double stateStabilityDelayS =
                        this.getStateStabilityDelayToUseS(BwdEventType.WINDOW_SHOWN);
                final boolean mustBlockFlickering = false;
                final boolean mustIgnoreBackingStateWhileUnstable =
                        (stateStabilityDelayS > 0.0);
                if (this.eventLogicOnWindowStateDiff(
                        nowS,
                        mustBlockFlickering,
                        mustIgnoreBackingStateWhileUnstable,
                        stateStabilityDelayS,
                        BwdEventType.WINDOW_SHOWN)) {
                    return;
                }

                this.logInternalStateIfDebug();
                if (DEBUG) {
                    hostLog(this, "EVENT LOGIC : shown");
                }
                this.setMustRunAgain();
                // Allowed to throw.
                this.createAndFireWindowEvent(BwdEventType.WINDOW_SHOWN);
                return;
            } else {
                final double stateStabilityDelayS =
                        this.getStateStabilityDelayToUseS(BwdEventType.WINDOW_HIDDEN);
                final boolean mustBlockFlickering = false;
                final boolean mustIgnoreBackingStateWhileUnstable =
                        (stateStabilityDelayS > 0.0);
                if (this.eventLogicOnWindowStateDiff(
                        nowS,
                        mustBlockFlickering,
                        mustIgnoreBackingStateWhileUnstable,
                        stateStabilityDelayS,
                        BwdEventType.WINDOW_HIDDEN)) {
                    return;
                }

                if (this.clientWrapper.isFocused()) {
                    if (this.isBackingWindowFocused()) {
                        if (DEBUG) {
                            hostLog(this, "EVENT LOGIC : backing window hidden, but still focused : not firing focus lost yet");
                        }
                        return;
                    }
                    this.logInternalStateIfDebug();
                    if (DEBUG) {
                        hostLog(this, "EVENT LOGIC : focus lost (before hidden)");
                    }
                    this.setMustRunAgain();
                    // Allowed to throw.
                    this.createAndFireWindowEvent(BwdEventType.WINDOW_FOCUS_LOST);
                    return;
                }
                this.logInternalStateIfDebug();
                if (DEBUG) {
                    hostLog(this, "EVENT LOGIC : hidden");
                }
                this.setMustRunAgain();
                // Allowed to throw.
                this.createAndFireWindowEvent(BwdEventType.WINDOW_HIDDEN);
                return;
            }
        }

        if (backingShowing) {
            // Callable if not closed and showing.
            final boolean backingIconified = this.isBackingWindowIconified();

            if (backingIconified != this.clientWrapper.isIconified()) {
                if (backingIconified) {
                    if (this.eventLogicOnWindowStateDiff(
                            nowS,
                            MUST_BLOCK_FLICKERING_FOR_ICO_DEICO_MAX_DEMAX,
                            MUST_IGNORE_BACKING_ICO_WHILE_UNSTABLE,
                            this.getStateStabilityDelayToUseS(BwdEventType.WINDOW_ICONIFIED),
                            BwdEventType.WINDOW_ICONIFIED)) {
                        return;
                    }

                    if (this.clientWrapper.isFocused()) {
                        if (this.isBackingWindowFocused()) {
                            if (DEBUG) {
                                hostLog(this, "EVENT LOGIC : backing window iconifed, but still focused : not firing focus lost yet");
                            }
                            return;
                        }
                        this.logInternalStateIfDebug();
                        if (DEBUG) {
                            hostLog(this, "EVENT LOGIC : focus lost (before iconifed)");
                        }
                        this.setMustRunAgain();
                        this.createAndFireWindowEvent(BwdEventType.WINDOW_FOCUS_LOST);
                        return;
                    }
                    this.logInternalStateIfDebug();
                    if (DEBUG) {
                        hostLog(this, "EVENT LOGIC : iconified");
                    }
                    this.setMustRunAgain();
                    this.createAndFireWindowEvent(BwdEventType.WINDOW_ICONIFIED);
                    return;
                } else {
                    if (this.eventLogicOnWindowStateDiff(
                            nowS,
                            MUST_BLOCK_FLICKERING_FOR_ICO_DEICO_MAX_DEMAX,
                            MUST_IGNORE_BACKING_DEICO_WHILE_UNSTABLE,
                            this.getStateStabilityDelayToUseS(BwdEventType.WINDOW_DEICONIFIED),
                            BwdEventType.WINDOW_DEICONIFIED)) {
                        return;
                    }

                    this.logInternalStateIfDebug();
                    if (DEBUG) {
                        hostLog(this, "EVENT LOGIC : deiconified");
                    }
                    this.setMustRunAgain();
                    this.createAndFireWindowEvent(BwdEventType.WINDOW_DEICONIFIED);
                    return;
                }
            }

            if (!backingIconified) {
                // Callable if not closed, showing and not iconified.
                final boolean backingMaximized = this.isBackingWindowMaximized();

                if (backingMaximized != this.clientWrapper.isMaximized()) {
                    if (this.mustTryToRestoreClientMaxStateIntoHost) {
                        if (DEBUG) {
                            if (backingMaximized) {
                                hostLog(this, "EVENT LOGIC : ignoring backing maximized : restoration from client state pending");
                            } else {
                                hostLog(this, "EVENT LOGIC : ignoring backing demaximized : restoration from client state pending");
                            }
                        }
                        return;
                    }
                    if (backingMaximized) {
                        if (this.eventLogicOnWindowStateDiff(
                                nowS,
                                MUST_BLOCK_FLICKERING_FOR_ICO_DEICO_MAX_DEMAX,
                                MUST_IGNORE_BACKING_MAX_WHILE_UNSTABLE,
                                this.getStateStabilityDelayToUseS(BwdEventType.WINDOW_MAXIMIZED),
                                BwdEventType.WINDOW_MAXIMIZED)) {
                            return;
                        }

                        this.logInternalStateIfDebug();
                        if (DEBUG) {
                            hostLog(this, "EVENT LOGIC : maximized");
                        }
                        this.setMustRunAgain();
                        this.createAndFireWindowEvent(BwdEventType.WINDOW_MAXIMIZED);
                        return;
                    } else {
                        if (this.eventLogicOnWindowStateDiff(
                                nowS,
                                MUST_BLOCK_FLICKERING_FOR_ICO_DEICO_MAX_DEMAX,
                                MUST_IGNORE_BACKING_DEMAX_WHILE_UNSTABLE,
                                this.getStateStabilityDelayToUseS(BwdEventType.WINDOW_DEMAXIMIZED),
                                BwdEventType.WINDOW_DEMAXIMIZED)) {
                            return;
                        }

                        this.logInternalStateIfDebug();
                        if (DEBUG) {
                            hostLog(this, "EVENT LOGIC : demaximized");
                        }
                        this.setMustRunAgain();
                        this.createAndFireWindowEvent(BwdEventType.WINDOW_DEMAXIMIZED);
                        return;
                    }
                }

                // Callable if not closed, showing and not iconified.
                final boolean backingFocused = this.isBackingWindowFocused();
                if (backingFocused != this.clientWrapper.isFocused()) {
                    if (backingFocused) {
                        {
                            final InterfaceBwdHost hostOfFocusedClient = this.hostOfFocusedClientHolder.getHostOfFocusedClient();
                            if ((hostOfFocusedClient != null)
                                    && (hostOfFocusedClient != this)) {
                                if (DEBUG) {
                                    hostLog(this, "EVENT LOGIC : backing window focused, but still another host ("
                                            + hid(hostOfFocusedClient)
                                            + ") with a client focused : not firing focus gained yet");
                                }
                                return;
                            }
                        }
                        this.logInternalStateIfDebug();
                        if (DEBUG) {
                            hostLog(this, "EVENT LOGIC : focus gained");
                        }
                        this.setMustRunAgain();
                        this.createAndFireWindowEvent(BwdEventType.WINDOW_FOCUS_GAINED);
                        return;
                    } else {
                        this.logInternalStateIfDebug();
                        this.setMustRunAgain();
                        this.createAndFireWindowEvent(BwdEventType.WINDOW_FOCUS_LOST);
                        return;
                    }
                }
            }
        }
    }
}
