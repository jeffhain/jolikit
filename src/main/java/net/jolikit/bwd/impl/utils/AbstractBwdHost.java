/*
 * Copyright 2019-2025 Jeff Hain
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.basics.InterfaceBwdHostImpl;
import net.jolikit.bwd.impl.utils.basics.InterfaceDoubleSupplier;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;
import net.jolikit.bwd.impl.utils.events.KeyRepetitionHelper;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.Unchecked;
import net.jolikit.time.TimeUtils;
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
public abstract class AbstractBwdHost implements InterfaceBwdHostImpl, InterfaceBackingWindowHolder {
    
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

    /**
     * For eventual black-padding around scaled client,
     * when scale > 1.
     * Span in binding (not OS) coordinates.
     * 
     * Could just use 1, but we prefer to use 2,
     * for pixels coordinates evenness to be preserved,
     * as it could impact rounding.
     * 
     * Can use larger values to make issue of not taking that
     * into account more visible, for example when testing with
     * DrawingMethodsCliScaledBwdTestCase and dragging
     * left side to the right or top side to bottom.
     */
    public static final int PADDING_BORDER_SPAN = 2;
    
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
            final long nextNs;
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
                nextNs = 0;
            } else {
                /*
                 * Subtracting the lateness we had for previous painting,
                 * to avoid getting too much late again and have a drift,
                 * but taking care not to be more early than tolerance,
                 * else we could pass here a lot of times in busy mode
                 * until we reach the next allowed paint time.
                 */
                final long nextTheoreticalTimeNs =
                    minPaintTimeNs
                    - Math.min(toleranceNs, this.lastPaintLatenessNs);
                if (DEBUG) {
                    hostLog(this, "process(...) : nextTheoreticalTimeNs = " + nextTheoreticalTimeNs);
                }
                nextNs = nextTheoreticalTimeNs;
            }
            return nextNs;
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

            final long nextNs;
            if (mustRunAgain) {
                // Input time still valid, since we didn't do anything heavy
                // (no synchronous painting).
                nextNs = plusBounded(actualTimeNs, this.delayNs);
            } else {
                this.stop();
                nextNs = 0;
            }
            return nextNs;
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

    private final MyClientWindowEventWrapper clientWindowEventWrapper =
        new MyClientWindowEventWrapper();

    private final BaseBwdBindingConfig bindingConfig;

    private final InterfaceBwdBindingImpl binding;

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
    
    private GRect clientBoundsInOsOnDragPress = null;
    private GRect windowBoundsInOsOnDragPress = null;
    private boolean dragMoveDetected = false;
    private GRect targetClientBoundsInOsOnDragEnd = null;
    private GRect targetWindowBoundsInOsOnDragEnd = null;

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
    
    private GRect clientBoundsInOsToEnforceOnShowDeicoDemax = null;
    private GRect windowBoundsInOsToEnforceOnShowDeicoDemax = null;

    /*
     * For mouse events synthesizing and/or blocking in case of scaling,
     * but also useful when scale is 1 due to libraries glitches
     * (such as SDL2 on Mac).
     */
    
    private boolean isLastMouseEventInClient = false;
    private final Set<Integer> mouseButtonPressedInClientSet = new HashSet<Integer>();
    
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
            InterfaceBwdBindingImpl binding,
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
    
    public ScaleHelper getScaleHelper() {
        return this.bindingConfig.getScaleHelper();
    }

    public InterfaceBwdBindingImpl getBinding() {
        return this.binding;
    }
    
    /*
     * 
     */
    
    @Override
    public GPoint getMousePosInScreen() {
        return this.binding.getMousePosInScreen();
    }
    
    @Override
    public GPoint getMousePosInScreenInOs() {
        return this.binding.getMousePosInScreenInOs();
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
        
        synchronized (this.dirtyRectBbMutex) {
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
        return this.getScaleHelper().insetsOsToBdContaining(
            this.getInsetsInOs());
    }

    @Override
    public GRect getInsetsInOs() {
        if (!this.isHostShowingDeico()) {
            return GRect.DEFAULT_EMPTY;
        }
        /*
         * Not caching it, because computation might not be reliable,
         * if computed as a difference between window bounds and client bounds.
         */
        return this.getBackingInsetsInOs();
    }

    @Override
    public GRect getClientBounds() {
        return this.getScaleHelper().rectOsToBdContained(
            this.getClientBoundsInOs());
    }

    @Override
    public GRect getClientBoundsInOs() {
        if (!this.isHostShowingDeico()) {
            return GRect.DEFAULT_EMPTY;
        }
        return this.getBackingOrOnDragBeginClientBoundsInOs();
    }

    @Override
    public GRect getWindowBounds() {
        /*
         * To ensure (insets,clientBounds,windowBounds) consistency
         * in case of scaling, we always recompute window bounds
         * from client bounds and insets in BD.
         */
        if (!this.isHostShowingDeico()) {
            return GRect.DEFAULT_EMPTY;
        }
        final GRect insetsInBd =
            this.getScaleHelper().insetsOsToBdContaining(
                this.getInsetsInOs());
        final GRect clientBoundsInBd =
            this.getScaleHelper().rectOsToBdContained(
                this.getBackingOrOnDragBeginClientBoundsInOs());
        return BindingCoordsUtils.computeWindowBounds(
            insetsInBd,
            clientBoundsInBd);
    }

    @Override
    public GRect getWindowBoundsInOs() {
        if (!this.isHostShowingDeico()) {
            return GRect.DEFAULT_EMPTY;
        }
        return this.getBackingOrOnDragBeginWindowBoundsInOs();
    }

    @Override
    public void setClientBounds(GRect targetClientBounds) {
        this.setClientBoundsInOs(
            this.getScaleHelper().rectBdToOs(
                targetClientBounds));
    }
    
    public void setClientBoundsInOs(GRect targetClientBoundsInOs) {
        
        // Implicit null check.
        targetClientBoundsInOs = sanitizedTargetBounds(targetClientBoundsInOs);

        final boolean hostShowingDeico = this.isHostShowingDeico();
        if (hostShowingDeico && (!this.isMaximized())) {
            // Making sure we won't overwrite specified bounds
            // with some previous bounds to enforce on demax.
            this.clearBoundsEnforcingOnShowDeicoDemax();
            
            if (this.bindingConfig.getMustFixBoundsDuringDrag()
                    && this.isDragMoveDetected()) {
                this.setTargetClientBoundsInOsOnDragEnd(targetClientBoundsInOs);
            } else {
                if (DEBUG) {
                    hostLog(this, "calling setBackingClientBoundsInOs(" + targetClientBoundsInOs + ")");
                }
                this.setBackingClientBoundsInOs(targetClientBoundsInOs);
            }
        } else {
            this.setClientBoundsInOsToEnforceOnShowDeicoDemax(targetClientBoundsInOs);
            this.ensureBoundsEnforcingFlagOnShowDeicoDemax();
            
            if (this.bindingConfig.getMustSetBackingDemaxBoundsWhileHiddenOrIconified()
                    && (!hostShowingDeico)
                    // isMaximized() always returns false
                    // when not showing and deiconified,
                    // so using client state.
                    && (!this.clientWrapper.isMaximized())) {
                if (DEBUG) {
                    hostLog(this, "(anti-glitch) calling setBackingClientBoundsInOs(" + targetClientBoundsInOs + ")");
                }
                this.setBackingClientBoundsInOs(targetClientBoundsInOs);
            }
        }
    }

    @Override
    public void setWindowBounds(GRect targetWindowBounds) {
        /*
         * In case of scaling:
         * We can't compute target client bounds in BD
         * from target window bounds (in BD) and insets (in BD),
         * since if window is not showing or is iconified
         * we can't properly reads these values.
         * As a result, unlike for getWindowBounds(),
         * we can't delegate to client bounds treatment.
         * But we don't need to anyway, since conversions from
         * coordinates in BD to coordinates in OS are always exact
         * and aligned on BD pixels: the window bounds in BD retrieved
         * after this setting should be exactly the one specified,
         * both when computed from insets and client bounds in BD,
         * or directly from window bounds in BD.
         */
        this.setWindowBoundsInOs(
            this.getScaleHelper().rectBdToOs(
                targetWindowBounds));
    }
    
    public void setWindowBoundsInOs(GRect targetWindowBoundsInOs) {
        
        // Implicit null check.
        targetWindowBoundsInOs = sanitizedTargetBounds(targetWindowBoundsInOs);
        
        final boolean hostShowingDeico = this.isHostShowingDeico();
        if (hostShowingDeico && (!this.isMaximized())) {
            // Making sure we won't overwrite specified bounds
            // with some previous bounds to enforce on demax.
            this.clearBoundsEnforcingOnShowDeicoDemax();
            
            if (this.bindingConfig.getMustFixBoundsDuringDrag()
                    && this.isDragMoveDetected()) {
                this.setTargetWindowBoundsInOsOnDragEnd(targetWindowBoundsInOs);
            } else {
                if (DEBUG) {
                    hostLog(this, "calling setBackingWindowBoundsInOs(" + targetWindowBoundsInOs + ")");
                }
                this.setBackingWindowBoundsInOs(targetWindowBoundsInOs);
            }
        } else {
            this.setWindowBoundsInOsToEnforceOnShowDeicoDemax(targetWindowBoundsInOs);
            this.ensureBoundsEnforcingFlagOnShowDeicoDemax();

            if (this.bindingConfig.getMustSetBackingDemaxBoundsWhileHiddenOrIconified()
                    && (!hostShowingDeico)
                    // isMaximized() always returns false
                    // when not showing and deiconified,
                    // so using client state.
                    && (!this.clientWrapper.isMaximized())) {
                if (DEBUG) {
                    hostLog(this, "(anti-glitch) calling setBackingWindowBoundsInOs(" + targetWindowBoundsInOs + ")");
                }
                this.setBackingWindowBoundsInOs(targetWindowBoundsInOs);
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
        final GRect targetClientBoundsInOs =
            this.getScaleHelper().rectBdToOs(
                targetClientBounds);
        return this.setClientBoundsSmartInOs(
            targetClientBoundsInOs,
            mustFixRight,
            mustFixBottom,
            mustRestoreBoundsIfNotExact);
    }

    public boolean setClientBoundsSmartInOs(
        GRect targetClientBoundsInOs,
        boolean mustFixRight,
        boolean mustFixBottom,
        boolean mustRestoreBoundsIfNotExact) {
        final boolean clientElseWindow = true;
        return this.setXxxBoundsInOsSmart(
            clientElseWindow,
            //
            targetClientBoundsInOs,
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
        final GRect targetWindowBoundsInOs =
            this.getScaleHelper().rectBdToOs(
                targetWindowBounds);
        return this.setWindowBoundsSmartInOs(
            targetWindowBoundsInOs,
            mustFixRight,
            mustFixBottom,
            mustRestoreBoundsIfNotExact);
    }

    public boolean setWindowBoundsSmartInOs(
        GRect targetWindowBoundsInOs,
        boolean mustFixRight,
        boolean mustFixBottom,
        boolean mustRestoreBoundsIfNotExact) {
        final boolean clientElseWindow = false;
        return this.setXxxBoundsInOsSmart(
            clientElseWindow,
            //
            targetWindowBoundsInOs,
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
        hostLog(this, "clientBoundsToEnforceOnShowDeicoDemax = " + this.clientBoundsInOsToEnforceOnShowDeicoDemax);
        hostLog(this, "windowBoundsToEnforceOnShowDeicoDemax = " + this.windowBoundsInOsToEnforceOnShowDeicoDemax);
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

    protected GRect getBackingOrOnDragBeginClientBoundsInOs() {
        if (this.bindingConfig.getMustFixBoundsDuringDrag()) {
            if (this.isDragMoveDetected()) {
                final GRect boundsInOsAtDragStart = this.clientBoundsInOsOnDragPress;
                if (DEBUG) {
                    hostLog(this,
                        "returning client bounds in OS at drag start : "
                            + boundsInOsAtDragStart);
                }
                return boundsInOsAtDragStart;
            }
        }
        return this.getBackingClientBoundsInOs();
    }

    protected GRect getBackingOrOnDragBeginWindowBoundsInOs() {
        if (this.bindingConfig.getMustFixBoundsDuringDrag()) {
            if (this.isDragMoveDetected()) {
                final GRect boundsAtDragStart = this.windowBoundsInOsOnDragPress;
                if (DEBUG) {
                    hostLog(this, "returning window bounds at drag start : " + boundsAtDragStart);
                }
                return boundsAtDragStart;
            }
        }
        return this.getBackingWindowBoundsInOs();
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
        try {
            this.eventuallyGenerateEnteredExitedEvent(event);
        } finally {
            if (event.isPosInClient()) {
                this.mouseButtonPressedInClientSet.add(event.getButton());
                if (this.bindingConfig.getMustFixBoundsDuringDrag()
                    && event.hasDragButton()) {
                    this.onDragPress();
                }
                this.clientWrapper.onMousePressed(event);
            } else {
                this.mouseButtonPressedInClientSet.remove(event.getButton());
            }
        }
    }

    protected void onBackingMouseReleased(BwdMouseEvent event) {
        try {
            this.eventuallyGenerateEnteredExitedEvent(event);
        } finally {
            if (this.mouseButtonPressedInClientSet.remove(event.getButton())) {
                // try/finally because onDragEnd() might throw.
                try {
                    if (this.isDragPressDetected()
                            && event.hasDragButton()) {
                        this.onDragEnd();
                    }
                } finally {
                    this.clientWrapper.onMouseReleased(event);
                }
            } else {
                /*
                 * Mouse released not to be generated if not pressed in client.
                 */
            }
        }
    }

    protected void onBackingMouseEnteredClient(BwdMouseEvent event) {
        if (this.isLastMouseEventInClient) {
            /*
             * We already synthesized entered event.
             */
        } else {
            if (event.isPosInClient()) {
                /*
                 * Backing event is in client:
                 * no need to synthesize it later. 
                 */
                this.isLastMouseEventInClient = true;
                this.clientWrapper.onMouseEnteredClient(event);
            }
        }
    }

    protected void onBackingMouseExitedClient(BwdMouseEvent event) {
        if (this.isLastMouseEventInClient) {
            /*
             * We didn't synthesize exited event:
             * forwarding backing one.
             */
            this.isLastMouseEventInClient = false;
            this.clientWrapper.onMouseExitedClient(event);
        } else {
            /*
             * We already synthesized exited event.
             */
        }
    }

    /**
     * Does the choice between MOUSE_MOVED and MOUSE_DRAGGED events.
     * 
     * @param event A MOUSE_MOVED event.
     */
    protected void onBackingMouseMoved(BwdMouseEvent event) {
        try {
            this.eventuallyGenerateEnteredExitedEvent(event);
        } finally {
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
                // Mouse moved events must only occur
                // when the mouse is in client bounds.
                if (this.isLastMouseEventInClient) {
                    this.clientWrapper.onMouseMoved(event);
                }
            }
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
     * Painting.
     */
    
    /**
     * @param windowAlphaFp Always in range.
     */
    protected abstract void setWindowAlphaFp_backing(double windowAlphaFp);

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
    
    /**
     * @param boxWithBorder Client box, in binding coordinates,
     *        eventually enlarged to make room for the padding border
     *        used to fill unused backing client area in case of scaling.
     * @return A new root graphics with the specified box.
     */
    protected abstract InterfaceBwdGraphics newRootGraphics(GRect boxWithBorder);
    
    /**
     * "Buffer" means the array of pixels corresponding to the box
     * of (actual) root graphics, and "buffer" frame of reference
     * is the one with (0,0) for buffer's top-left pixel
     * (even if root box's top-left x() and y() are negative,
     * which can be the case to make sure that top-left pixel
     * of non-enlarged box (the one used by client's graphics
     * and therefore user code) is always (0,0) regardless
     * of scale-padding).
     * 
     * @param paintedRectList In buffer frame of reference,
     *        and in binding coordinates. Can be empty.
     */
    protected abstract void paintBackingClient(
        ScaleHelper scaleHelper,
        GPoint clientSpansInOs,
        GPoint bufferPosInCliInOs,
        GPoint bufferSpansInBd,
        List<GRect> paintedRectList);
    
    protected void paintBwdClientNowAndBackingClient(
        BaseBwdBindingConfig bindingConfig,
        AbstractHostBoundsHelper hostBoundsHelper) {
        
        if (DEBUG) {
            hostLog(this, "paintClientNow(...)");
        }
        
        final InterfaceBwdClient client = this.getClientWithExceptionHandler();

        client.processEventualBufferedEvents();

        if (!this.canPaintClientNow()) {
            if (DEBUG) {
                hostLog(this, "can't paint now");
            }
            return;
        }
        
        final GRect clientBoundsInOs = hostBoundsHelper.getClientBoundsInOs();
        if (clientBoundsInOs.isEmpty()) {
            if (DEBUG) {
                hostLog(this, "clientBoundsInOs is empty");
            }
            return;
        }

        final ScaleHelper scaleHelper = bindingConfig.getScaleHelper();
        final int scale = scaleHelper.getScale();
        
        final GRect scaledClientBoundsInBd =
            scaleHelper.rectOsToBdContained(
                clientBoundsInOs);
        
        final GRect scaledClientBoundsInOs =
            scaleHelper.rectBdToOs(
                scaledClientBoundsInBd);
        
        final GRect scaledClientInsetsInOs =
            ScaleHelper.computeScaledClientInsetsInOs(
                clientBoundsInOs,
                scaledClientBoundsInOs);
        
        final boolean gotScalingBorder =
            !scaledClientInsetsInOs.equals(GRect.DEFAULT_EMPTY);
        
        final GRect boxForClient =
            scaledClientBoundsInBd.withPos(0, 0);
        /*
         * Box with border so that box without border
         * (used by client) starts at (0,0)
         * (we want our border hack not to have
         * any visible effect from client code).
         */
        final GRect boxWithBorder;
        if (gotScalingBorder) {
            boxWithBorder =
                boxForClient.withBordersDeltas(
                    -PADDING_BORDER_SPAN,
                    -PADDING_BORDER_SPAN,
                    PADDING_BORDER_SPAN,
                    PADDING_BORDER_SPAN);
        } else {
            boxWithBorder = boxForClient;
        }
        
        final GPoint clientSpansInOs =
            GPoint.valueOf(
                clientBoundsInOs.xSpan(),
                clientBoundsInOs.ySpan());
        final GPoint bufferPosInCliInOs;
        if (gotScalingBorder) {
            bufferPosInCliInOs =
                GPoint.valueOf(
                    scaledClientInsetsInOs.x() - PADDING_BORDER_SPAN * scale,
                    scaledClientInsetsInOs.y() - PADDING_BORDER_SPAN * scale);
        } else {
            bufferPosInCliInOs = GPoint.ZERO;
        }
        
        final GRect dirtyRect = this.getAndResetDirtyRectBb();
        
        /*
         * Painting into offscreen buffer.
         */
        
        final InterfaceBwdGraphics gForBorder =
            this.newRootGraphics(boxWithBorder);

        final InterfaceBwdGraphics gForClient;
        if (gotScalingBorder) {
            gForClient = gForBorder.newChildGraphics(boxForClient);
        } else {
            gForClient = gForBorder;
        }
        
        /*
         * The graphics we give to client might not actually be
         * a root graphics (as client's paintClient() method indicates),
         * but nothing in the API makes it visible so it's fine.
         */
        final List<GRect> paintedRectList =
            this.getPaintClientHelper().initPaintFinish(
                gForClient,
                dirtyRect);
        
        if (gotScalingBorder) {
            gForBorder.init();
            try {
                /*
                 * Only need to draw border's inner rectangle,
                 * which is the only part that can be visible.
                 */
                final int ib = (PADDING_BORDER_SPAN - 1);
                final GRect borderInnerRect =
                    boxWithBorder.withBordersDeltasElseEmpty(
                        ib, ib, -ib, -ib);
                gForBorder.drawRect(borderInnerRect);
            } finally {
                gForBorder.finish();
            }
            
            final boolean isClientToFullyDraw =
                (paintedRectList.size() == 1)
                && (paintedRectList.get(0).contains(boxForClient));
            if (isClientToFullyDraw) {
                // Replacing with enlarged box.
                paintedRectList.clear();
                paintedRectList.add(boxWithBorder);
            } else {
                // Just adding the borders.
                for (GRect borderRect :
                    ScaleHelper.computeBorderRectsInBd(
                        boxWithBorder,
                        PADDING_BORDER_SPAN)) {
                    paintedRectList.add(borderRect);
                }
            }
        }
        
        /*
         * Copying offscreen buffer into backing graphics.
         * 
         * Only doing it if canPaintClientNow() returns true,
         * to avoid issues in case closing or shutdown occurred
         * during offscreen buffer painting.
         * (ex. : getGL() that returns null in GL bindings, etc.)
         */
        
        if (this.canPaintClientNow()) {
            
            /*
             * Ensuring painted rectangles coordinates
             * to be in buffer frame of reference.
             * Else would have to deal with rootBoxTopLeft.
             */
            if (gotScalingBorder) {
                final int size = paintedRectList.size();
                for (int i = 0; i < size; i++) {
                    final GRect rect1 = paintedRectList.get(i);
                    final GRect rect2 = rect1.withPosDeltas(
                        PADDING_BORDER_SPAN,
                        PADDING_BORDER_SPAN);
                    paintedRectList.set(i, rect2);
                }
            }
            
            final GPoint bufferSpansInBd =
                GPoint.valueOf(
                    boxWithBorder.xSpan(),
                    boxWithBorder.ySpan());
            
            final long a = (DEBUG ? System.nanoTime() : 0L);
            
            this.paintBackingClient(
                scaleHelper,
                clientSpansInOs,
                bufferPosInCliInOs,
                bufferSpansInBd,
                paintedRectList);
            
            final long b = (DEBUG ? System.nanoTime() : 0L);
            if (DEBUG) {
                hostLog(this, "painting into backing of "
                    + paintedRectList.size()
                    + " rects took " + TimeUtils.nsToS(b-a) + " s");
            }
        }
    }

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
     *         around client area, in OS pixels.
     */
    protected abstract GRect getBackingInsetsInOs();

    /**
     * @return Backing client bounds, in OS pixels.
     */
    protected abstract GRect getBackingClientBoundsInOs();

    /**
     * @return Backing window bounds, in OS pixels.
     */
    protected abstract GRect getBackingWindowBoundsInOs();

    /**
     * @param targetClientBoundsInOs Target backing client bounds, in OS pixels.
     */
    protected abstract void setBackingClientBoundsInOs(GRect targetClientBoundsInOs);

    /**
     * @param targetWindowBoundsInOs Target backing window bounds, in OS pixels.
     */
    protected abstract void setBackingWindowBoundsInOs(GRect targetWindowBoundsInOs);

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
     * @param windowBoundsInOsToEnforce Bounds, in OS pixels, to enforce
     *        to go from hidden or iconified, back to showing/deico/max.
     */
    protected void applyWindowBoundsInOsToEnforceOnShowDeicoMax_protected(GRect windowBoundsInOsToEnforce) {
        this.applyWindowBoundsInOsToEnforceOnShowDeicoMax(windowBoundsInOsToEnforce);
    }
    
    /**
     * Sets the bounds, and the flag indicating that they must be applied
     * once back to showing/deico/demax.
     * 
     * @param windowBoundsInOsToEnforce Bounds, in OS pixels, to enforce
     *        to go from hidden or iconified, back to showing/deico/demax.
     */
    protected void setWindowBoundsInOsToEnforceOnShowDeicoDemaxAndFlag_protected(GRect windowBoundsInOsToEnforce) {
        this.setWindowBoundsInOsToEnforceOnShowDeicoDemax(windowBoundsInOsToEnforce);
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
     * Event logic.
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
    
    private void eventuallyGenerateEnteredExitedEvent(BwdMouseEvent event) {
        final boolean isInClient = event.isPosInClient();
        if (isInClient != this.isLastMouseEventInClient) {
            this.isLastMouseEventInClient = isInClient;
            if (isInClient) {
                final BwdMouseEvent enteredEvent = event.asMouseEnteredClientEvent();
                this.clientWrapper.onMouseEnteredClient(enteredEvent);
            } else {
                final BwdMouseEvent exitedEvent = event.asMouseExitedClientEvent();
                this.clientWrapper.onMouseExitedClient(exitedEvent);
            }
        }
    }
    
    /*
     * 
     */

    private static void fillWith(double[] arr, double value) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = value;
        }
    }
    
    /*
     * State.
     */

    private void logInternalStateIfDebug() {
        if (DEBUG) {
            this.logInternalState();
        }
    }
    
    /**
     * Useful delay to wait, but not too much, before restoring or enforcing
     * some state or bounds, due to some libraries erasing recently set state
     * or bounds with other values due to other events
     * (such as with JavaFX erasing recently set bounds on focus gain).
     */
    private double getHalfStabilityDelayS() {
        return this.bindingConfig.getBackingWindowStateStabilityDelayS() * 0.5;
    }
    
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
        return this.clientWrapper.isShowing()
            && (!this.clientWrapper.isIconified())
            && this.isHostShowingDeico();
    }

    private boolean areHostAndClientShowingDeicoDemax() {
        // Quick client checks first.
        return this.clientWrapper.isShowing()
            && (!this.clientWrapper.isIconified())
            && (!this.clientWrapper.isMaximized())
            && this.isHostShowingDeicoDemax();
    }

    private boolean areHostAndClientShowingDeicoMax() {
        // Quick client checks first.
        return this.clientWrapper.isShowing()
            && (!this.clientWrapper.isIconified())
            && this.clientWrapper.isMaximized()
            && this.isHostShowingDeico()
            && this.isMaximized();
    }
    
    private boolean areHostAndClientShowingDeicoFocused() {
        // Quick client checks first.
        return this.clientWrapper.isShowing()
            && (!this.clientWrapper.isIconified())
            && this.clientWrapper.isFocused()
            && this.isHostShowingDeico()
            && this.isFocused();
    }

    /*
     * Drag.
     */
    
    private boolean isDragPressDetected() {
        return (this.clientBoundsInOsOnDragPress != null);
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
        this.clientBoundsInOsOnDragPress = this.getClientBoundsInOs();
        this.windowBoundsInOsOnDragPress = this.getWindowBoundsInOs();
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
        
        this.clientBoundsInOsOnDragPress = null;
        this.windowBoundsInOsOnDragPress = null;

        this.dragMoveDetected = false;
        
        this.targetClientBoundsInOsOnDragEnd = null;
        this.targetWindowBoundsInOsOnDragEnd = null;
    }

    private void cancelDragIfDragPressDetected() {
        if (this.isDragPressDetected()) {
            this.onDragCancel();
        }
    }
    
    /**
     * Must be called on mouse drag button released,
     * or when window gets hidden, iconified, or loses focus,
     * but not on mouse exited because it can happen during drag
     * and must not stop it.
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
        this.clientBoundsInOsOnDragPress = null;
        this.windowBoundsInOsOnDragPress = null;
        
        final GRect targetClientBoundsInOs = this.targetClientBoundsInOsOnDragEnd;
        this.targetClientBoundsInOsOnDragEnd = null;

        final GRect targetWindowBoundsInOs = this.targetWindowBoundsInOsOnDragEnd;
        this.targetWindowBoundsInOsOnDragEnd = null;

        // Taking care not to mess up bounds
        // if no drag move was detected.
        if (this.dragMoveDetected) {
            this.dragMoveDetected = false;
            
            if (targetClientBoundsInOs != null) {
                this.setClientBoundsInOs(targetClientBoundsInOs);
            } else if (targetWindowBoundsInOs != null) {
                this.setWindowBoundsInOs(targetWindowBoundsInOs);
            }
        }
    }

    /**
     * Nullifies targetWindowBoundsInOsOnDragEnd.
     * @param targetClientBoundsInOs Must not be null.
     */
    private void setTargetClientBoundsInOsOnDragEnd(
        GRect targetClientBoundsInOs) {
        LangUtils.requireNonNull(targetClientBoundsInOs);
        this.targetClientBoundsInOsOnDragEnd = targetClientBoundsInOs;
        this.targetWindowBoundsInOsOnDragEnd = null;
    }

    /**
     * Nullifies targetClientBoundsInOsOnDragEnd.
     * @param targetWindowBoundsInOs Must not be null.
     */
    private void setTargetWindowBoundsInOsOnDragEnd(
        GRect targetWindowBoundsInOs) {
        LangUtils.requireNonNull(targetWindowBoundsInOs);
        this.targetClientBoundsInOsOnDragEnd = null;
        this.targetWindowBoundsInOsOnDragEnd = targetWindowBoundsInOs;
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

    private void applyWindowBoundsInOsToEnforceOnShowDeicoMax(
        GRect windowBoundsInOsToEnforce) {
        this.clearBoundsEnforcingFlagOnShowDeicoMax();

        if (DEBUG) {
            hostLog(this,
                "show/deico/max : setting windowBoundsInOsToEnforce : "
                    + windowBoundsInOsToEnforce);
        }
        this.setBackingWindowBoundsInOs(windowBoundsInOsToEnforce);
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
        return (this.clientBoundsInOsToEnforceOnShowDeicoDemax != null)
                || (this.windowBoundsInOsToEnforceOnShowDeicoDemax != null);
    }
    
    /**
     * Clears bounds to enforce and enforcing flag.
     */
    private void clearBoundsEnforcingOnShowDeicoDemax() {
        this.clearBoundsEnforcingFlagOnShowDeicoDemax();
        
        this.clientBoundsInOsToEnforceOnShowDeicoDemax = null;
        this.windowBoundsInOsToEnforceOnShowDeicoDemax = null;
    }
    
    private void setClientBoundsInOsToEnforceOnShowDeicoDemax(
        GRect clientBoundsInOsToEnforce) {
        LangUtils.requireNonNull(clientBoundsInOsToEnforce);
        
        if (DEBUG) {
            hostLog(this,
                "clientBoundsInOsToEnforceOnShowDeicoDemax set to "
            + clientBoundsInOsToEnforce);
        }
        this.clientBoundsInOsToEnforceOnShowDeicoDemax = clientBoundsInOsToEnforce;
        this.windowBoundsInOsToEnforceOnShowDeicoDemax = null;
    }
    
    private void setWindowBoundsInOsToEnforceOnShowDeicoDemax(
        GRect windowBoundsInOsToEnforce) {
        LangUtils.requireNonNull(windowBoundsInOsToEnforce);
        
        if (DEBUG) {
            hostLog(this,
                "windowBoundsInOsToEnforceOnShowDeicoDemax set to "
            + windowBoundsInOsToEnforce);
        }
        this.clientBoundsInOsToEnforceOnShowDeicoDemax = null;
        this.windowBoundsInOsToEnforceOnShowDeicoDemax = windowBoundsInOsToEnforce;
    }
    
    private void setClientBoundsToEnforceOnShowDeicoDemaxWithCurrent() {
        // NB: Could also just use backing bounds directly.
        final GRect clientBoundsInOs =
            this.getBackingOrOnDragBeginClientBoundsInOs();
        if ((!clientBoundsInOs.isEmpty())
                && (!clientBoundsInOs.equals(this.clientBoundsInOsToEnforceOnShowDeicoDemax))) {
            if (DEBUG) {
                hostLog(this, "setting clientBoundsInOsToEnforceOnShowDeicoDemax set to current client bounds");
            }
            this.setClientBoundsInOsToEnforceOnShowDeicoDemax(clientBoundsInOs);
        }
    }
    
    /**
     * Also clears bounds to enforce and enforcing flag.
     * 
     * @throws IllegalStateException if there are no bounds to apply.
     */
    private void applyBoundsToEnforceOnShowDeicoDemax() {
        final GRect targetClientBoundsInOs = this.clientBoundsInOsToEnforceOnShowDeicoDemax;
        final GRect targetWindowBoundsInOs = this.windowBoundsInOsToEnforceOnShowDeicoDemax;
        
        this.clearBoundsEnforcingOnShowDeicoDemax();
        
        // Only supposed to have one non-null.
        // If for some reason both are non-null,
        // priority to client bounds.
        if (targetClientBoundsInOs != null) {
            if (DEBUG) {
                hostLog(this, "show/deico/demax : setting targetClientBoundsInOs : " + targetClientBoundsInOs);
            }
            this.setBackingClientBoundsInOs(targetClientBoundsInOs);
        } else if (targetWindowBoundsInOs != null) {
            if (DEBUG) {
                hostLog(this, "show/deico/demax : setting targetWindowBoundsInOs : " + targetWindowBoundsInOs);
            }
            this.setBackingWindowBoundsInOs(targetWindowBoundsInOs);
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

    private GRect getXxxBoundsInOs(boolean clientElseWindow) {
        if (clientElseWindow) {
            return this.getClientBoundsInOs();
        } else {
            return this.getWindowBoundsInOs();
        }
    }

    private void setTargetXxxBoundsInOs(
            boolean clientElseWindow,
            //
            GRect targetBoundsInOs) {
        if (clientElseWindow) {
            this.setClientBoundsInOs(targetBoundsInOs);
        } else {
            this.setWindowBoundsInOs(targetBoundsInOs);
        }
    }

    private boolean setXxxBoundsInOsSmart(
            boolean clientElseWindow,
            //
            GRect targetBoundsInOs,
            boolean fixRight,
            boolean fixBottom,
            boolean restoreBoundsIfNotExact) {

        // Implicit null check.
        targetBoundsInOs = sanitizedTargetBounds(targetBoundsInOs);

        if (!this.isHostShowingDeicoDemax()) {
            return false;
        }

        /*
         * 
         */

        final GRect oldBoundsInOs = this.getXxxBoundsInOs(clientElseWindow);

        if (oldBoundsInOs.equals(targetBoundsInOs)) {
            // Nothing to do.
            return false;
        }

        /*
         * Specified bounds spans are ensured to be >= 0, otherwise
         * it could cause weird "quiet" behaviors, such as with AWT or Swing.
         */
        
        this.setTargetXxxBoundsInOs(
                clientElseWindow,
                //
                targetBoundsInOs);

        GRect newBoundsInOs = this.getXxxBoundsInOs(clientElseWindow);

        /*
         * 
         */

        boolean didReworkNewBounds = false;

        // Could as well just use >.
        if (fixRight && (newBoundsInOs.xMax() != oldBoundsInOs.xMax())) {
            // Assuming it would succeed.
            newBoundsInOs = newBoundsInOs.withPosDeltas(
                -(newBoundsInOs.xMax() - oldBoundsInOs.xMax()),
                0);
            didReworkNewBounds = true;
        }

        // Could as well just use >.
        if (fixBottom && (newBoundsInOs.yMax() != oldBoundsInOs.yMax())) {
            // Assuming it would succeed.
            newBoundsInOs = newBoundsInOs.withPosDeltas(
                0,
                -(newBoundsInOs.yMax() - oldBoundsInOs.yMax()));
            didReworkNewBounds = true;
        }

        if (restoreBoundsIfNotExact && (!newBoundsInOs.equals(targetBoundsInOs))) {
            // Assuming it would succeed.
            newBoundsInOs = oldBoundsInOs;
            didReworkNewBounds = true;
        }

        if (didReworkNewBounds) {
            if (DEBUG) {
                Dbg.log("***************** DID REWORK NEW BOUNDS ************");
            }
            this.setTargetXxxBoundsInOs(
                    clientElseWindow,
                    //
                    newBoundsInOs);
        }

        return !newBoundsInOs.equals(oldBoundsInOs);
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
    private static String toBen(BwdEventType eventType) {
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
                            "EVENT LOGIC : ignoring backing " + toBen(eventType)
                            + " : opposite state detected and not yet fired");
                }
                return true;
            }
            final double antiFlickeringDelayS = this.bindingConfig.getBackingWindowStateAntiFlickeringDelayS();
            final double dtS = nowS - this.newestBackingStateDiffDetTimeSByOrdinal[oppOrd];
            if (dtS < antiFlickeringDelayS) {
                if (DEBUG) {
                    hostLog(this,
                            "EVENT LOGIC : ignoring backing " + toBen(eventType)
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
                        "EVENT LOGIC : using backing " + toBen(eventType)
                        + " without delay : programmatic modification pending");
            }
            return false;
        }
        
        if (justDetected) {
            if (DEBUG) {
                hostLog(this,
                        "EVENT LOGIC : ignoring backing " + toBen(eventType)
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
                        "EVENT LOGIC : ignoring backing " + toBen(eventType)
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
                            "EVENT LOGIC : ignoring backing " + toBen(eventType)
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
                            "EVENT LOGIC : ignoring backing " + toBen(eventType)
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
                        "EVENT LOGIC : ignoring backing " + toBen(eventType)
                        + " : programmatic deiconification is pending.");
            }
            return true;
        }
        if ((eventType == BwdEventType.WINDOW_DEICONIFIED)
                && this.pendingBackingStateProgModByOrdinal[BwdEventType.WINDOW_ICONIFIED.ordinal()]) {
            if (DEBUG) {
                hostLog(this,
                        "EVENT LOGIC : ignoring backing " + toBen(eventType)
                        + " : programmatic iconification is pending.");
            }
            return true;
        }

        return false;
    }
    
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
        final GRect windowBoundsInOsToEnforce =
            getBinding().getScreenBoundsInOs();
        
        this.applyWindowBoundsInOsToEnforceOnShowDeicoMax(windowBoundsInOsToEnforce);
    }
    
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
        final double oneTenthOfStabDelayS =
            (1.0/10) * this.getBindingConfig().getBackingWindowStateStabilityDelayS();
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
                            final InterfaceBwdHost hostOfFocusedClient =
                                this.hostOfFocusedClientHolder.getHostOfFocusedClient();
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
