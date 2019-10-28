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
package net.jolikit.bwd.impl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdEvent;
import net.jolikit.bwd.api.events.BwdEventType;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.test.utils.BwdClientMock;
import net.jolikit.lang.Dbg;
import net.jolikit.time.TimeUtils;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.misc.SoftTestHelper;

/**
 * Test of AbstractBwdHost API.
 */
public class AbztractBwdHostTest extends TestCase {
    
    /*
     * NB: HostUnitTestBwdTestCase does similar tests,
     * but is more complicated, since it deals
     * with hosts of real bindings, timeouts, etc.,
     * and uses hard scheduling.
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    private static final int NBR_OF_CALLS = 100 * 1000;
    
    private static final long RANDOM_SEED = 123456789L;
    
    /**
     * After this delay, all asynchronous but ASAP side effects must have
     * occurred.
     */
    private static final long POST_ASYNC_DELAY_NS = 1;

    /**
     * Should be enough to be past a few post-async checks.
     */
    private static final long INTER_STEP_DELAY_NS = 10;

    private static final GRect INSETS = GRect.valueOf(1, 2, 3, 4);
    
    private static final GRect MAXIMIZED_CLIENT_BOUNDS = GRect.valueOf(0, 0, 3000, 4000);
    
    private static final GRect BASE_CLIENT_BOUNDS = GRect.valueOf(100, 200, 300, 400);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private enum MyAction {
        SHOW,
        HIDE,
        REQUEST_FOCUS_GAIN,
        REQUEST_FOCUS_LOSS,
        ICONIFY,
        DEICONIFY,
        MAXIMIZE,
        DEMAXIMIZE,
        MOVE,
        RESIZE,
        CLOSE;
    }
    
    /**
     * Possible host states.
     */
    private enum MyState {
        HIDDEN,
        SHOWING,
        SHOWING_FOCUSED,
        SHOWING_ICONIFIED,
        SHOWING_MAXIMIZED,
        SHOWING_FOCUSED_MAXIMIZED,
        CLOSED;
    }
    
    /*
     * 
     */

    private class MyTestProcess extends AbstractProcess {
        final int nbrOfCalls;
        int callCount = 0;
        public MyTestProcess(
                InterfaceScheduler scheduler,
                int nbrOfCalls) {
            super(scheduler);
            this.nbrOfCalls = nbrOfCalls;
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {
            if (this.callCount == this.nbrOfCalls) {
                binding.shutdownAbruptly();
                // Not returning 0 to avoid "schedule in the past" exception,
                // since we don't call this.stop() before returning.
                return Long.MAX_VALUE;
            }
            this.callCount++;
            final int cc = this.callCount;
            
            /*
             * 
             */
            
            if (testedHost.isClosed()) {
                // Not doing it all the time,
                // so that we can check behavior while closed.
                final boolean mustRenewHosts = random.nextBoolean();
                if (DEBUG) {
                    Dbg.log();
                    Dbg.log("tested host closed : mustRenewHosts = " + mustRenewHosts);
                }
                if (mustRenewHosts) {
                    renewRefHostIfNullOrClosed();
                    renewTestedHostIfNullOrClosed();
                }
            }

            final MyAction action = MyAction_VALUES[random.nextInt(MyAction_VALUES.length)];
            final boolean mustJustCallBackingListeners = random.nextBoolean();
            if (DEBUG) {
                Dbg.log();
                Dbg.log("[" + cc + "] action = " + action + (mustJustCallBackingListeners ? " (just calling onBackingXxx())" : ""));
            }
            
            if (DEBUG) {
                Dbg.log("initial refHost state:");
                logInternalState_refHost();
                
                Dbg.log("initial testedHost state:");
                testedHost.logInternalState();
            }

            if (mustJustCallBackingListeners) {
                callOnBackingWindowXxx(action, refHost);
                callOnBackingWindowXxx(action, testedHost);
            } else {
                doAction(action, refHost);
                doAction(action, testedHost);
            }
            
            /*
             * 
             */
            
            getScheduler().executeAfterNs(checkRunnable, POST_ASYNC_DELAY_NS);

            return actualTimeNs + INTER_STEP_DELAY_NS;
        }
    };

    /**
     * Checks tested host state against ref host state.
     */
    private class MyCheckRunnable implements Runnable {
        public MyCheckRunnable() {
        }
        @Override
        public void run() {

            /*
             * State check.
             */

            {
                final MyState expectedState = computeHostState(refHost);
                final MyState actualState = computeHostState(testedHost);
                if (DEBUG) {
                    Dbg.log("expectedState = " + expectedState);
                    Dbg.log("actualState = " + actualState);
                }
                assertEquals(expectedState, actualState);
            }

            /*
             * Bounds check.
             */

            {
                final GRect expectedClientBounds = refHost.getClientBounds();
                final GRect actualClientBounds = testedHost.getClientBounds();
                if (DEBUG) {
                    Dbg.log("expectedClientBounds = " + expectedClientBounds);
                    Dbg.log("actualClientBounds = " + actualClientBounds);
                }
                assertEquals(expectedClientBounds, actualClientBounds);
            }

            {
                final GRect expectedWindowBounds = refHost.getWindowBounds();
                final GRect actualWindowBounds = testedHost.getWindowBounds();
                if (DEBUG) {
                    Dbg.log("expectedWindowBounds = " + expectedWindowBounds);
                    Dbg.log("actualWindowBounds = " + actualWindowBounds);
                }
                assertEquals(expectedWindowBounds, actualWindowBounds);
            }

            /*
             * Event types check.
             */

            {
                final List<BwdEventType> expectedList = getAndClearEventList(refHost);
                final List<BwdEventType> actualList = getAndClearEventList(testedHost);
                if (DEBUG) {
                    Dbg.log("expectedList = " + expectedList);
                    Dbg.log("actualList = " + actualList);
                }
                assertEquals(expectedList, actualList);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final MyAction[] MyAction_VALUES = MyAction.values();

    private final MyCheckRunnable checkRunnable = new MyCheckRunnable();
    
    private final Random random = new Random(RANDOM_SEED);
    
    private BindingForApiTests binding;
    
    private RefHostForApiTests refHost;
    private HostForApiTests testedHost;

    private GRect showDeicoDemaxClientBounds_refHost;
    private GRect showDeicoDemaxClientBounds_testedHost;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbztractBwdHostTest() {
    }

    public void test_eventLogic_comparisonToRefHost() {
        this.init();

        final int nbrOfCalls = NBR_OF_CALLS;
        
        final SoftTestHelper helper = this.binding.getSoftTestHelper();
        final InterfaceScheduler scheduler = helper.getScheduler();

        final MyTestProcess testProcess = new MyTestProcess(scheduler, nbrOfCalls);
        
        testProcess.start();

        final long stopTimeNs = Long.MAX_VALUE;
        helper.startNowAndStopAtNs(stopTimeNs);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void doAction(MyAction action, InterfaceBwdHost host) {
        final boolean isRefHost = (host instanceof RefHostForApiTests);
        switch (action) {
        case SHOW: {
            host.show();
        } break;
        case HIDE: {
            host.hide();
        } break;
        case REQUEST_FOCUS_GAIN: {
            host.requestFocusGain();
        } break;
        case REQUEST_FOCUS_LOSS: {
            if (isRefHost) {
                ((RefHostForApiTests) host).requestFocusLoss();
            } else {
                ((HostForApiTests) host).requestFocusLoss();
            }
        } break;
        case ICONIFY: {
            host.iconify();
        } break;
        case DEICONIFY: {
            host.deiconify();
        } break;
        case MAXIMIZE: {
            host.maximize();
        } break;
        case DEMAXIMIZE: {
            host.demaximize();
        } break;
        case MOVE: {
            final GRect oldBounds = getShowDeicoDemaxClientBounds(isRefHost);
            final GRect newBounds = oldBounds.withPosDeltas(2, 0);
            
            host.setClientBounds(newBounds);
            
            this.setShowDeicoDemaxClientBounds(isRefHost, newBounds);
        } break;
        case RESIZE: {
            final GRect oldBounds = getShowDeicoDemaxClientBounds(isRefHost);
            final GRect newBounds = oldBounds.withSpansDeltas(3, 0);
            
            host.setClientBounds(newBounds);
            
            this.setShowDeicoDemaxClientBounds(isRefHost, newBounds);
        } break;
        case CLOSE: {
            host.close();
        } break;
        default:
            throw new AssertionError("" + action);
        }
    }

    private static void callOnBackingWindowXxx(
            MyAction action,
            InterfaceBwdHost host) {
        final boolean isRefHost = (host instanceof RefHostForApiTests);
        final RefHostForApiTests refHost = (isRefHost ? (RefHostForApiTests) host : null);
        final HostForApiTests testedHost = (isRefHost ? null : (HostForApiTests) host);
        final boolean isTestedHost = !isRefHost;
        switch (action) {
        case SHOW: {
            if (isTestedHost) {
                testedHost.onBackingWindowShown();
            }
        } break;
        case HIDE: {
            if (isTestedHost) {
                testedHost.onBackingWindowHidden();
            }
        } break;
        case REQUEST_FOCUS_GAIN: {
            if (isTestedHost) {
                testedHost.onBackingWindowFocusGained();
            }
        } break;
        case REQUEST_FOCUS_LOSS: {
            if (isTestedHost) {
                testedHost.onBackingWindowFocusLost();
            }
        } break;
        case ICONIFY: {
            if (isTestedHost) {
                testedHost.onBackingWindowIconified();
            }
        } break;
        case DEICONIFY: {
            if (isTestedHost) {
                testedHost.onBackingWindowDeiconified();
            }
        } break;
        case MAXIMIZE: {
            if (isTestedHost) {
                testedHost.onBackingWindowMaximized();
            }
        } break;
        case DEMAXIMIZE: {
            if (isTestedHost) {
                testedHost.onBackingWindowDemaximized();
            }
        } break;
        case MOVE: {
            if (isTestedHost) {
                testedHost.onBackingWindowMoved();
            } else {
                refHost.onBackingWindowMoved();
            }
        } break;
        case RESIZE: {
            if (isTestedHost) {
                testedHost.onBackingWindowResized();
            } else {
                refHost.onBackingWindowResized();
            }
        } break;
        case CLOSE: {
            if (isTestedHost) {
                testedHost.onBackingWindowClosing();
            } else {
                refHost.onBackingWindowClosing();
            }
        } break;
        default:
            throw new AssertionError("" + action);
        }
    }

    /*
     * 
     */
    
    private GRect getShowDeicoDemaxClientBounds(boolean forRefHost) {
        final GRect ret;
        if (forRefHost) {
            ret = this.showDeicoDemaxClientBounds_refHost;
        } else {
            ret = this.showDeicoDemaxClientBounds_testedHost;
        }
        return ret;
    }
    
    private void setShowDeicoDemaxClientBounds(boolean forRefHost, GRect bounds) {
        if (forRefHost) {
            this.showDeicoDemaxClientBounds_refHost = bounds;
        } else {
            this.showDeicoDemaxClientBounds_testedHost = bounds;
        }
    }
    
    /*
     * 
     */
    
    private static MyState computeHostState(InterfaceBwdHost host) {
        return computeState(
                host.isShowing(),
                host.isFocused(),
                host.isIconified(),
                host.isMaximized(),
                host.isClosed());
    }
    
    private static MyState computeState(
            boolean showing,
            boolean focused,
            boolean iconified,
            boolean maximized,
            boolean closed) {
        MyState state = null;
        if ((!closed) && (!showing) && (!focused) && (!iconified) && (!maximized)) {
            state = MyState.HIDDEN;
        } else if ((!closed) && showing && (!focused) && (!iconified) && (!maximized)) {
            state = MyState.SHOWING;
        } else if ((!closed) && showing && focused && (!iconified) && (!maximized)) {
            state = MyState.SHOWING_FOCUSED;
        } else if ((!closed) && showing && (!focused) && iconified && (!maximized)) {
            state = MyState.SHOWING_ICONIFIED;
        } else if ((!closed) && showing && (!focused) && (!iconified) && maximized) {
            state = MyState.SHOWING_MAXIMIZED;
        } else if ((!closed) && showing && focused && (!iconified) && maximized) {
            state = MyState.SHOWING_FOCUSED_MAXIMIZED;
        } else if (closed && (!showing) && (!focused) && (!iconified) && (!maximized)) {
            state = MyState.SHOWING_FOCUSED_MAXIMIZED;
        }
        if (state == null) {
            Dbg.log("showing = " + showing);
            Dbg.log("focused = " + focused);
            Dbg.log("iconified = " + iconified);
            Dbg.log("maximized = " + maximized);
            Dbg.log("closed = " + closed);
            throw new AssertionError("illegal state");
        }
        return state;
    }

    /*
     * 
     */

    /**
     * Actually just returns a list of events types.
     */
    private static List<BwdEventType> getAndClearEventList(InterfaceBwdHost host) {
        final BwdClientMock client = (BwdClientMock) host.getClient();
        final List<BwdEventType> list = new ArrayList<BwdEventType>();
        for (BwdEvent event : client.getEventList()) {
            final BwdEventType eventType = event.getEventType();
            list.add(eventType);
        }
        client.clearStoredEvents();
        return list;
    }

    /*
     * 
     */
    
    private void logInternalState_refHost() {
        final BwdClientMock client = (BwdClientMock) this.refHost.getClient();
        final String stateStr = HostStateUtils.toStringHostState(
                client.getClientStateShowing(),
                client.getClientStateFocused(),
                client.getClientStateIconified(),
                client.getClientStateMaximized(),
                this.refHost.getClientEventMovedPending(),
                this.refHost.getClientEventResizedPending(),
                client.getClientStateClosed());
        Dbg.logPr(this, "refHost stored client state = " + stateStr);
        
        Dbg.logPr(this, "refHost.getBackingClientBounds() = " + refHost.getBackingClientBounds());
        Dbg.logPr(this, "refHost.getTargetClientBounds_onShowingDeicoDemax() = " + refHost.getTargetClientBounds_onShowingDeicoDemax());
        Dbg.logPr(this, "refHost.getTargetWindowBounds_onShowingDeicoDemax() = " + refHost.getTargetWindowBounds_onShowingDeicoDemax());
    }
    
    /*
     * 
     */

    private static BwdClientMock newClientMock() {
        final BwdClientMock client = new BwdClientMock();
        // We read stored events.
        client.setMustStoreEvents(true);
        // Fail-fast on error.
        client.setMustCallOnEventError(true);
        return client;
    }

    private void renewRefHostIfNullOrClosed() {
        if ((this.refHost == null)
                || this.refHost.isClosed()) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("renewing ref host");
            }

            final BwdClientMock client = newClientMock();

            final RefHostForApiTests host = new RefHostForApiTests(
                    client,
                    INSETS,
                    MAXIMIZED_CLIENT_BOUNDS);
            
            host.setClientBounds(BASE_CLIENT_BOUNDS);

            this.refHost = host;
            this.showDeicoDemaxClientBounds_refHost = BASE_CLIENT_BOUNDS;
        }
    }

    private void renewTestedHostIfNullOrClosed() {
        if ((this.testedHost == null)
                || this.testedHost.isClosed()) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("renewing tested host");
            }

            final BwdClientMock client = newClientMock();

            final String title = "TestedHostTitle";
            final boolean decorated = false;
            final HostForApiTests host = (HostForApiTests) binding.newHost(
                    title,
                    decorated,
                    client);
            
            host.setClientBounds(BASE_CLIENT_BOUNDS);
            
            this.testedHost = host;
            this.showDeicoDemaxClientBounds_testedHost = BASE_CLIENT_BOUNDS;
        }
    }

    private void init() {
        final BaseBwdBindingConfig bindingConfig = new BaseBwdBindingConfig();

        /*
         * Zeroizing some delays for commands to have immediate effects, which
         * make soft-scheduled tests much simpler to write.
         */
        
        bindingConfig.setClientPaintingDelayS(0.0);
        bindingConfig.setBackingWindowStateStabilityDelayS(0.0);
        bindingConfig.setBackingWindowHiddenStabilityDelayS(0.0);
        bindingConfig.setBackingWindowStateAntiFlickeringDelayS(0.0);
        
        // We don't implement such a subtlety in our ref host.
        bindingConfig.setMustSetBackingDemaxBoundsWhileHiddenOrIconified(false);
        
        /*
         * Window event logic period : small enough for MOVE/RESIZE actions
         * to cause corresponding events before we do checks and more tests.
         */
        
        final double windowEventLogicPeriodS = TimeUtils.nsToS(INTER_STEP_DELAY_NS) / 2;
        bindingConfig.setWindowEventLogicPeriodS(windowEventLogicPeriodS);
        
        /*
         * 
         */

        final BindingForApiTests binding = new BindingForApiTests(
                bindingConfig,
                INSETS,
                MAXIMIZED_CLIENT_BOUNDS);
        this.binding = binding;

        this.renewRefHostIfNullOrClosed();
        this.renewTestedHostIfNullOrClosed();
    }
}
