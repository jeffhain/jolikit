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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdEvent;
import net.jolikit.bwd.api.events.BwdEventType;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.AbstractBwdBinding;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.HostStateUtils;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.test.cases.utils.AbstractUnitTestBwdTestCase;
import net.jolikit.bwd.test.cases.utils.WindowStateTestUtils;
import net.jolikit.bwd.test.utils.BwdClientMock;
import net.jolikit.bwd.test.utils.BwdEventTestHelper;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.OsUtils;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.time.TimeUtils;

/**
 * To test host state, behavior, controls, and events.
 * 
 * We create various kinds of hosts, play with them using host API,
 * and check what state they are in and what events they received.
 * 
 * Tested hosts are not controlled from within painting of main host,
 * because with some libraries (such as SWT), when a window is maximized,
 * none other can be paint, which would just block tests.
 * Instead, we use a specific repeated process, that also ensures
 * main host repaint.
 * 
 * In case of test failure:
 * - First check if the failure still occurs after increasing
 *   getMinQuiescentDelayBeforeNextStepS() for the tested binding.
 * - If it does, rework binding's config or implementation so that
 *   it doesn't anymore, eventually disabling functionalities that
 *   can't be implemented properly on top of the backing library.
 * 
 * Tests results, per OS:
 * 
 * - awt:
 *   - Windows: OK
 *   - Mac: KO: Freezes on hideBackingWindow() after a few hundreds steps.
 * - swing:
 *   - Windows: OK
 *   - Mac: KO: Freezes after a few hundreds steps.
 * - jfx:
 *   - Windows: OK
 *   - Mac: OK
 * 
 * - swt:
 *   - Windows: OK
 *   - Mac: OK
 * - lwjgl:
 *   - Windows: OK
 *   - Mac: KO: Crashes after 100+ steps.
 * - jogl:
 *   - Windows: OK
 *   - Mac: OK
 * 
 * - qtj:
 *   - Windows: OK
 *   - Mac: OK
 * - algr:
 *   - Windows: OK
 *   - Mac: OK (but sometimes fails, maybe bad luck with timeouts)
 * - sdl:
 *   - Windows: OK
 *   - Mac: KO: Crashes after a few hundreds steps.
 */
public class HostUnitTestBwdTestCase extends AbstractUnitTestBwdTestCase {

    /*
     * NB: These tests only test programmatic state modifications,
     * for which we do special things in our hosts (like not waiting
     * stability delay if a state transition is expected, or ignoring
     * maximized state change if an iconification or deiconification
     * is expected, etc.), so even if these tests pass, there might be
     * some issues when events are initiated non-programmatically,
     * such as from native decoration.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /**
     * For debug, better exit ASAP than just throw.
     */
    private static final boolean MUST_EXIT_ON_ERROR = true;
    private static final boolean MUST_PAUSE_ON_ERROR = (!MUST_EXIT_ON_ERROR);
    
    /*
     * 
     */

    private static final boolean MUST_CALL_SET_BOUNDS_METHODS = true;
    /**
     * Bonus span from initial client bounds,
     * to make sure to never specify window bounds
     * too small to be met.
     */
    private static final int RANDOM_BOUNDS_BONUS_SPAN = 50;
    
    private boolean mustCheckOtherWindowsBoundsMethods() {
        if (BwdTestUtils.isSwtBinding(this.getBinding())
                && OsUtils.isMac()) {
            /*
             * TODO swt False because on Mac, with SWT, setting bounds
             * on a window can have side-effect on bounds on its dialogs.
             * Ex. (both undecorated, so client bounds = window bounds):
             * H4D3U8 : window bounds =  [729, 461, 204, 104]
             * H4D3D1Ud : window bounds =  [730, 536, 150, 50]
             * H4D3U8.setWindowBounds([717, 454, 210, 110])
             * H4D3U8 : EVENT LOGIC : window resized, client bounds = [717, 454, 210, 110]
             * H4D3D1Ud : EVENT LOGIC : window moved, client bounds = [718, 529, 150, 50]
             */
            return false;
        }
        return true;
    }
    
    /*
     * 
     */
    
    private static boolean mustAllowUndecorated(InterfaceBwdBinding binding) {
        return true;
    }

    /**
     * To make sure step display is updated.
     * Also allows to test that painting doesn't cause
     * window state change.
     */
    private static final boolean MUST_REPAINT_HOSTS_AFTER_EACH_STEP = true;

    private static final int NBR_OF_STEPS = 2000;

    private static final double UNPAUSING_CHECK_PERIOD_S = 0.1;
    private static final double NEXT_STEP_POSSIBILITY_CHECK_PERIOD_S = 0.01;

    /**
     * A little bonus because when throwing, some events firing
     * can be delayed for the next time at which "event logic" runs.
     */
    private static final double TOLERANCE_S_BONUS_IF_THROW_ON_EVENT = 0.1;
    
    /*
     * 
     */

    /**
     * We always separate steps using a fixed delay, either because
     * some operation might take the whole time, or to check that
     * some time after quick operations the state is still fine.
     * 
     * Must not be too small, because modifications on windows
     * can have side-effects on other windows, and we want their
     * modifications to also stabilize before checks at next step
     * beginning.
     */
    private double getMinQuiescentDelayBeforeNextStepS() {
        final InterfaceBwdBinding binding = this.getBinding();
        if (BwdTestUtils.isJfxBinding(binding)) {
            return 0.2;
        } else if (BwdTestUtils.isSwtBinding(binding)) {
            /*
             * SWT can stall for a while, on deiconify for example,
             * which prevents "event logic" to run often, and can
             * cause focus gain to be delayed due to prior focus loss taking
             * time to occur too (we block focus gain until proper backing focus loss
             * occurred).
             */
            return 0.3;
        } else if (BwdTestUtils.isLwjglBinding(binding)) {
            /*
             * Can have large delays to end up with proper focus.
             */
            return 0.2;
        } else if (BwdTestUtils.isJoglBinding(binding)) {
            /*
             * JOGL sometimes does much weird focus ping-pong,
             * involving painting, before stabilizing.
             */
            return 0.2;
        } else if (BwdTestUtils.isQtjBinding(binding)) {
            /*
             * Can have large delays to end up with proper focus,
             * but focus state is not something that we fail on
             * so this special delay is due to other reasons.
             */
            return 0.2;
        } else if (BwdTestUtils.isAlgrBinding(binding)) {
            /*
             * Can take much time for expected bounds to be met,
             * or state to stabilize.
             */
            return 0.5;
        } else {
            return 0.1;
        }
    }
    
    /*
     * 
     */

    /**
     * To check whether things go well or go nuts when client throws
     * on each host event.
     * It's no big deal if things go nuts then, since our bindings
     * are by default configured to use exception handler around
     * client calls.
     * 
     * NB: Issues when we let exceptions reach the backing library:
     * - jfx:
     *   - Windows: Having the following exceptions after a few hundreds steps,
     *     and test freezes the next step:
java.lang.NullPointerException
    at com.sun.javafx.tk.quantum.WindowStage.lambda$setScene$388(WindowStage.java:253)
    at com.sun.javafx.tk.quantum.QuantumToolkit.runWithRenderLock(QuantumToolkit.java:407)
    at com.sun.javafx.tk.quantum.WindowStage.setScene(WindowStage.java:252)
    at javafx.stage.Window$9.invalidated(Window.java:845)
    at javafx.beans.property.BooleanPropertyBase.markInvalid(BooleanPropertyBase.java:109)
    at javafx.beans.property.BooleanPropertyBase.set(BooleanPropertyBase.java:144)
    at javafx.stage.Window.setShowing(Window.java:922)
    at javafx.stage.Window.show(Window.java:937)
    at javafx.stage.Stage.show(Stage.java:259)
    at (...our code...)
    at com.sun.javafx.application.PlatformImpl.lambda$null$173(PlatformImpl.java:295)
    at java.security.AccessController.doPrivileged(Native Method)
    at com.sun.javafx.application.PlatformImpl.lambda$runLater$174(PlatformImpl.java:294)
    at com.sun.glass.ui.InvokeLaterDispatcher$Future.run(InvokeLaterDispatcher.java:95)
    at com.sun.glass.ui.win.WinApplication._runLoop(Native Method)
    at com.sun.glass.ui.win.WinApplication.lambda$null$148(WinApplication.java:191)
    at java.lang.Thread.run(Thread.java:748)
Exception in thread "JavaFX Application Thread" java.lang.NullPointerException
    at com.sun.javafx.tk.quantum.GlassScene.getClearColor(GlassScene.java:339)
    at com.sun.javafx.tk.quantum.SceneState.update(SceneState.java:100)
    at com.sun.javafx.tk.quantum.GlassScene.updateSceneState(GlassScene.java:282)
    at com.sun.javafx.tk.quantum.GlassScene.releaseSynchronization(GlassScene.java:143)
    at javafx.scene.Scene$ScenePulseListener.pulse(Scene.java:2424)
    at com.sun.javafx.tk.Toolkit.lambda$runPulse$30(Toolkit.java:355)
    at java.security.AccessController.doPrivileged(Native Method)
    at com.sun.javafx.tk.Toolkit.runPulse(Toolkit.java:354)
    at com.sun.javafx.tk.Toolkit.firePulse(Toolkit.java:381)
    at com.sun.javafx.tk.quantum.QuantumToolkit.pulse(QuantumToolkit.java:510)
    at com.sun.javafx.tk.quantum.QuantumToolkit.pulse(QuantumToolkit.java:490)
    at com.sun.javafx.tk.quantum.QuantumToolkit.lambda$runToolkit$404(QuantumToolkit.java:319)
    at com.sun.glass.ui.InvokeLaterDispatcher$Future.run(InvokeLaterDispatcher.java:95)
    at com.sun.glass.ui.win.WinApplication._runLoop(Native Method)
    at com.sun.glass.ui.win.WinApplication.lambda$null$148(WinApplication.java:191)
    at java.lang.Thread.run(Thread.java:748)
     *   - Mac: The following exception happens more and more,
     *     and painting updates start to no longer occur, and test freezes
     *     after a few hundreds steps:
java.lang.NullPointerException
    at com.sun.prism.es2.ES2SwapChain.createGraphics(ES2SwapChain.java:218)
    at com.sun.prism.es2.ES2SwapChain.createGraphics(ES2SwapChain.java:40)
    at com.sun.javafx.tk.quantum.PresentingPainter.run(PresentingPainter.java:87)
    at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
    at java.util.concurrent.FutureTask.runAndReset(FutureTask.java:308)
    at com.sun.javafx.tk.RenderJob.run(RenderJob.java:58)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
    at com.sun.javafx.tk.quantum.QuantumRenderer$PipelineRunnable.run(QuantumRenderer.java:125)
    at java.lang.Thread.run(Thread.java:748)
     * 
     * - swt:
     *   - Windows: Error early, due to show() failing to deiconify
     *              (and at some point, we can see that iconifying a window
     *              doesn't hide its children when client throws).
     *   - Mac: Crashes after a few hundreds steps.
     *   
     * - lwjgl:
     *   - Windows: Error early, due to iconify() failing to iconify.
     *              Also, iconifying a window directly causing focus gain
     *              on one of its dialogs:
at org.lwjgl.glfw.GLFWWindowFocusCallbackI.callback(GLFWWindowFocusCallbackI.java:23)
at org.lwjgl.system.JNI.invokePV(Native Method)
at org.lwjgl.glfw.GLFW.glfwIconifyWindow(GLFW.java:1957)
     */
    private static final boolean MUST_THROW_ON_EVENT = false;

    /**
     * False so that we can see how things behave when we let
     * client exceptions reach backing library code,
     * and try to make up for it in our bindings.
     */
    private static final boolean MUST_USE_CATCHING_CLIENT = false;

    /*
     * 
     */

    private static final double HOST_CREATION_PROBA = 0.2;

    /**
     * Focus gain is so unreliable, and for some libraries works according
     * to such a complicated logic (even without using dialogs), that we
     * don't bother to try to characterize these logics in our tests,
     * which would make them complicated and brittle.
     * Instead, we just count and display how many times focus gain
     * was hoped for but did not occur.
     */
    private static final boolean MUST_NOT_DO_HARD_CHECKS_ON_FOCUS_GAIN = true;

    /**
     * Backing libraries can cause maximized state to immediately become false
     * on iconification (whether or not a backing event is generated for it),
     * or on deiconification (such as on Mac with JavaFX, mostly when window
     * has no focused dialog), and possibly just temporarily.
     * 
     * Though, since by default our bindings use stability/anti-flickering
     * delays, and maximized state restoration logics, we can set this flag
     * to false.
     */
    private static final boolean MUST_ALLOW_FOR_CLIENT_DEMAX_ON_ICONIFY = false;
    
    /*
     * 
     */

    private static final int MAX_NBR_OF_HOSTS = 6;

    private static final int MAX_NBR_OF_ROOT_HOSTS = 3;

    private static final int MAX_NBR_OF_DIALOGS_PER_OWNER = 2;

    /**
     * 0 = only root hosts, 1 = dialogs only from root hosts, etc.
     */
    private static final int MAX_HOST_DEPTH = 2;

    /**
     * Black background for client area not to illuminate the room
     * when window gets maximized.
     */
    private static final BwdColor COLOR_BG = BwdColor.BLACK;
    private static final BwdColor COLOR_FG = BwdColor.WHITE;

    /*
     * 
     */

    private static final int WINDOW_EVENT_INFO_LIST_MAX_SIZE = 20;

    private static final int SUB_CLIENT_WIDTH = 150;
    private static final int SUB_CLIENT_HEIGHT = 50;

    private static final int TEXT_X = 10;
    private static final int TEXT_Y = 10;

    private static final int INITIAL_WIDTH = 500;
    private static final int INITIAL_HEIGHT = 400;

    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /**
     * State boolean type.
     */
    private enum MyBt {
        SHOWING,
        FOCUSED,
        ICONIFIED,
        MAXIMIZED,
        CLOSED;
        private static final MyBt[] VALUES = MyBt.values();
    }

    /**
     * Immutable.
     */
    private static class MyState {
        final Map<MyBt,Boolean> valueByBt;
        public MyState(
                boolean showing,
                boolean focused,
                boolean iconified,
                boolean maximized,
                boolean closed) {
            final Map<MyBt,Boolean> valueByBt = new HashMap<MyBt,Boolean>();
            valueByBt.put(MyBt.SHOWING, showing);
            valueByBt.put(MyBt.FOCUSED, focused);
            valueByBt.put(MyBt.ICONIFIED, iconified);
            valueByBt.put(MyBt.MAXIMIZED, maximized);
            valueByBt.put(MyBt.CLOSED, closed);
            this.valueByBt = Collections.unmodifiableMap(valueByBt);
        }
        /**
         * @param valueByBt Must contain a boolean for each BT.
         */
        public MyState(Map<MyBt,Boolean> valueByBt) {
            this(
                    valueByBt.get(MyBt.SHOWING),
                    valueByBt.get(MyBt.FOCUSED),
                    valueByBt.get(MyBt.ICONIFIED),
                    valueByBt.get(MyBt.MAXIMIZED),
                    valueByBt.get(MyBt.CLOSED));
        }
        /*
         * 
         */
        public MyState with(MyBt bt, boolean valued) {
            LangUtils.requireNonNull(bt);
            final Map<MyBt,Boolean> newValueByBt = new HashMap<MyBt,Boolean>(this.valueByBt);
            newValueByBt.put(bt, valued);
            return new MyState(newValueByBt);
        }
        public boolean is(MyBt bt) {
            return this.valueByBt.get(bt);
        }
        /*
         * 
         */
        public boolean isShowing() {
            return this.valueByBt.get(MyBt.SHOWING);
        }
        public boolean isFocused() {
            return this.valueByBt.get(MyBt.FOCUSED);
        }
        public boolean isIconified() {
            return this.valueByBt.get(MyBt.ICONIFIED);
        }
        public boolean isMaximized() {
            return this.valueByBt.get(MyBt.MAXIMIZED);
        }
        public boolean isClosed() {
            return this.valueByBt.get(MyBt.CLOSED);
        }
        /*
         * 
         */
        @Override
        public String toString() {
            return HostStateUtils.toStringHostState(
                    this.is(MyBt.SHOWING),
                    this.is(MyBt.FOCUSED),
                    this.is(MyBt.ICONIFIED),
                    this.is(MyBt.MAXIMIZED),
                    this.is(MyBt.CLOSED));
        }
        @Override
        public int hashCode() {
            return this.valueByBt.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MyState)) {
                return false;
            }
            final MyState other = (MyState) obj;
            return this.valueByBt.equals(other.valueByBt);
        }
    }

    private enum MyHostMethod {
        SHOW,
        HIDE,
        REQUEST_WINDOW_FOCUS_GAIN,
        ICONIFY,
        DEICONIFY,
        MAXIMIZE,
        DEMAXIMIZE,
        SET_CLIENT_BOUNDS,
        SET_WINDOW_BOUNDS,
        CLOSE;
        private static final MyHostMethod[] VALUES = MyHostMethod.values();
    }

    private class MyExceptionForTest extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public MyExceptionForTest(String message) {
            super(message);
        }
    }

    private class MyNewStateComputerAndChecker {
        private final Object hostId;
        private final MyState newState;
        private MyState expectedFinalState;
        public MyNewStateComputerAndChecker(
                Object hostId,
                MyState oldState,
                MyState newState) {
            this.hostId = hostId;
            this.newState = newState;
            this.expectedFinalState = oldState;
        }
        public void setExpectedFinalStateBt(MyBt bt, boolean expectedNewValue) {
            this.expectedFinalState = this.expectedFinalState.with(bt, expectedNewValue);
        }
        public void setExpectedFinalStateBtAndCheckAlreadyMet(MyBt bt, boolean expectedNewValue) {
            this.setExpectedFinalStateBt(bt, expectedNewValue);
            checkStateBtEqualTo(
                    this.hostId,
                    this.newState,
                    bt,
                    expectedNewValue);
        }
        /**
         * @return Current expected final state.
         */
        public MyState getExpectedFinalState() {
            return this.expectedFinalState;
        }
    }

    /*
     * 
     */

    private class MyClientMock extends BwdClientMock {
        public MyClientMock() {
            this.configureUiThreadCheck(getBinding().getUiThreadScheduler());
            // Storing them because we check them.
            this.setMustStoreEvents(true);
            this.setMustCallOnEventError(true);
        }
        @Override
        protected List<GRect> paintClientImpl(
                InterfaceBwdGraphics g,
                GRect dirtyRect) {

            final GRect box = g.getBox();

            g.setColor(COLOR_BG);
            g.clearRect(box);

            g.setColor(COLOR_FG);
            BwdTestUtils.drawRectStipple(g, box);

            final int h = g.getFont().metrics().height() + 1;
            int textY = 2;

            g.drawText(box.xMid(), textY, this.getHost().getTitle());
            textY += h;

            final int stepNum = lastStepNumStarted;
            g.drawText(2, textY, "stepNum = " + stepNum);
            textY += h;

            return GRect.DEFAULT_HUGE_IN_LIST;
        }
        @Override
        protected void onAnyEvent(BwdEventType expectedEventType, BwdEvent event) {
            super.onAnyEvent(expectedEventType, event);
            if ((event.getEventType() == BwdEventType.MOUSE_MOVED)
                    || (event.getEventType() == BwdEventType.WHEEL_ROLLED)) {
                // To avoid spam.
                return;
            }

            if (event.getEventType().isWindowEventType()) {
                final long eventTimeNs = getBinding().getUiThreadScheduler().getClock().getTimeNs();
                eventTestHelper.addBwdEventInfo(event, eventTimeNs);
            }

            if (MUST_THROW_ON_EVENT) {
                throw new MyExceptionForTest("for test - throwing on " + event + " on host " + hid(this.getHost()));
            } else {
                if (false) {
                    // To make logs easier to compare.
                    final MyExceptionForTest exception = new MyExceptionForTest("for test - NOT throwing on " + event);
                    Dbg.log(exception);
                }
            }
        }
        @Override
        protected void onEventError(String errorMsg) {
            onError(errorMsg);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * For determinism, in case some treatments depend on the order on which
     * we iterate over hosts.
     * Supposes that each host has its own title.
     */
    private static final Comparator<InterfaceBwdHost> HOST_COMPARATOR = new Comparator<InterfaceBwdHost>() {
        @Override
        public int compare(InterfaceBwdHost o1, InterfaceBwdHost o2) {
            return o1.getTitle().compareTo(o2.getTitle());
        }
    };

    /*
     * 
     */
    
    /**
     * Should not be mandatory, since even without it we wouldn't have
     * two hosts with a same title at the same time, but it's safer
     * in case we don't clean everything on host closed.
     * Also helps for debug in logs.
     */
    private final AtomicInteger hostTitleUniquifier = new AtomicInteger();

    /*
     * 
     */

    /**
     * 0 means no step started yet.
     */
    private int lastStepNumStarted = 0;

    /**
     * Allowing for pausing by left-clicking on main window,
     * to observe info on the window or logs.
     * Doesn't hurt, since we don't fail on focus issues.
     */
    private boolean paused = false;

    private boolean done = false;

    private final Random random = TestUtils.newRandom123456789L();

    /**
     * To avoid hosts being superimposed.
     */
    private final List<Integer> availablePosIndex = new ArrayList<Integer>();
    {
        for (int i = 0; i < MAX_NBR_OF_HOSTS; i++) {
            this.availablePosIndex.add(i);
        }
    }
    private final Map<InterfaceBwdHost,Integer> posIndexByHost =
            new TreeMap<InterfaceBwdHost,Integer>(HOST_COMPARATOR); 

    /**
     * Key = a hosts we work on.
     * Value = the corresponding client.
     */
    private final Map<InterfaceBwdHost,BwdClientMock> clientByHost =
            new TreeMap<InterfaceBwdHost,BwdClientMock>(HOST_COMPARATOR);

    /**
     * We can't retrieve maximized-when-showing-and-not-iconified state
     * from client state (to know expected maximized state once host
     * gets no longer hidden nor iconified), because on some platforms
     * (such as Mac), some state transitions such as iconification
     * are progressive and make state temporary inconsistent.
     * More precisely, when a maximized window gets iconified,
     * isBackingWindowIconified() can still return false for some time,
     * but isBackingWindowMaximized() return false immediately,
     * which can make the host wrongly fire a DEMAXIMIZED event
     * to the client.
     * 
     * As a result, we use a map to keep track of expected/shadowed
     * maximized state that must be applied when the window is
     * showing-and-not-iconified, for each host.
     */
    private final Map<InterfaceBwdHost,Boolean> shadowedMaxStateByHost =
            new TreeMap<InterfaceBwdHost,Boolean>(HOST_COMPARATOR);

    /**
     * Client bounds when showing/deiconified/demaximized.
     */
    private final Map<InterfaceBwdHost,GRect> expectedDemaxedClientBoundsByHost =
            new TreeMap<InterfaceBwdHost,GRect>(HOST_COMPARATOR);

    /**
     * Window bounds when showing/deiconified/demaximized.
     * (Storing both expected client and window demaximized bounds,
     * otherwise we would need to do some conversions using insets,
     * and insets might not be available when hiding or iconified.)
     */
    private final Map<InterfaceBwdHost,GRect> expectedDemaxedWindowBoundsByHost =
            new TreeMap<InterfaceBwdHost,GRect>(HOST_COMPARATOR);

    /**
     * Window bounds when showing/deiconified/maximized.
     */
    private final Map<InterfaceBwdHost,GRect> expectedMaxedWindowBoundsByHost =
            new TreeMap<InterfaceBwdHost,GRect>(HOST_COMPARATOR);
    
    /**
     * For last window events from test hosts,
     * and the host method calls that cause them.
     */
    private final BwdEventTestHelper eventTestHelper =
            new BwdEventTestHelper(WINDOW_EVENT_INFO_LIST_MAX_SIZE);

    private InterfaceBwdHost hostForPostStepCheck = null;
    private MyState expectedFinalHostState = null;
    private MyState expectedFinalClientState = null;

    private MyState lastOldHostState = null;
    private MyHostMethod lastHostMethod = null;

    private final int[] nbrOfFocusGainedByMethodOrdinal = new int[MyHostMethod.VALUES.length];
    private final int[] nbrOfExpectedFocusGainByMethodOrdinal = new int[MyHostMethod.VALUES.length];

    private int errorCount = 0;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public HostUnitTestBwdTestCase() {
    }

    public HostUnitTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }

    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new HostUnitTestBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new HostUnitTestBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    /*
     * 
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);

        if (event.isPrimaryDown()
                && (!this.done)) {
            this.paused = !this.paused;
            this.getHost().ensurePendingClientPainting();
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void onTestBegin() {
        if (DEBUG) {
            Dbg.log("onTestBegin()");
        }

        this.lastStepNumStarted = 0;
        
        final AbstractBwdBinding binding = (AbstractBwdBinding) this.getBinding();
        binding.getBindingConfig().setMustUseExceptionHandlerForClient(MUST_USE_CATCHING_CLIENT);
    }

    @Override
    protected void onTestEnd() {
        if (DEBUG) {
            Dbg.log("onTestEnd()");
        }

        // Setting flag before closing, which can throw.
        this.done = true;
        this.paused = false;

        this.closeAllHosts();
    }

    @Override
    protected long testSome(long nowNs) {

        final double nowS = TimeUtils.nsToS(nowNs);

        if (this.paused) {
            // Will re-check soon.
            return TimeUtils.sToNs(nowS + UNPAUSING_CHECK_PERIOD_S);
        }
        
        final double minQuiescentDelayBeforeNextStepS = getMinQuiescentDelayBeforeNextStepS();
        
        // Checking that all hosts states are stable.
        for (InterfaceBwdHost host : this.getHostList(Integer.MAX_VALUE)) {
            final AbstractBwdHost hostAbs = (AbstractBwdHost) host;
            final double npsfTimeS = hostAbs.getNewestPossibleUnstabilityTimeS();
            final double dtS = nowS - npsfTimeS;
            // Assumes we use AbstractBwdBinding.
            final BaseBwdBindingConfig bindingConfig =
                    ((AbstractBwdBinding) getBinding()).getBindingConfig();

            double thresholdDurS = bindingConfig.getBackingWindowStateStabilityDelayS();
            thresholdDurS = Math.max(thresholdDurS, bindingConfig.getBackingWindowHiddenStabilityDelayS());
            thresholdDurS = Math.max(thresholdDurS, bindingConfig.getBackingWindowStateAntiFlickeringDelayS());
            // If delay is always short, we still want to wait some time;
            // to check that expected state is not valid only just after action.
            thresholdDurS = Math.max(thresholdDurS, minQuiescentDelayBeforeNextStepS);
            if (MUST_THROW_ON_EVENT) {
                thresholdDurS += TOLERANCE_S_BONUS_IF_THROW_ON_EVENT;
            }

            final boolean mightStillBeFlickering = (dtS < thresholdDurS);
            if (mightStillBeFlickering) {
                return TimeUtils.sToNs(nowS + NEXT_STEP_POSSIBILITY_CHECK_PERIOD_S);
            } else {
                // Now window state should be stable for this host.
            }
        }
        
        /*
         * 
         */

        if (DEBUG) {
            Dbg.log();
            Dbg.log();
            Dbg.log();
        }
        
        final int stepNum = ++this.lastStepNumStarted;
        // ">=" in case weird things happen.
        final boolean isLastStep = (stepNum >= NBR_OF_STEPS);
        if (isLastStep) {
            if (DEBUG) {
                Dbg.log("process(,) : stepNum = " + stepNum + " starting, is last step");
            }
        } else {
            if (DEBUG) {
                Dbg.log("process(,) : stepNum = " + stepNum + " starting");
            }
        }
        
        if (DEBUG) {
            this.logAllStates();
        }

        testStep();

        /*
         * 
         */

        if (MUST_REPAINT_HOSTS_AFTER_EACH_STEP) {
            for (InterfaceBwdHost host : this.clientByHost.keySet()) {
                host.ensurePendingClientPainting();
            }
        }

        this.getHost().ensurePendingClientPainting();

        /*
         * 
         */
        
        final long nextTheoreticalDateNs;
        if (isLastStep) {
            if (DEBUG) {
                Dbg.log("process(,) : stepNum = " + stepNum + " done, was last step");
            }
            this.eventTestHelper.addCustomEventInfo("DONE");
            nextTheoreticalDateNs = Long.MAX_VALUE;
        } else {
            final double timeAfterTestS = getBinding().getUiThreadScheduler().getClock().getTimeS();
            final double minDelayS = minQuiescentDelayBeforeNextStepS;
            if (DEBUG) {
                Dbg.log("process(,) : stepNum = " + stepNum + " done, next step in at least " + minDelayS + " s");
            }
            nextTheoreticalDateNs = TimeUtils.sToNs(timeAfterTestS + minDelayS);
        }

        return nextTheoreticalDateNs;
    }

    @Override
    protected BwdColor getClearColor() {
        return COLOR_BG;
    }

    @Override
    protected void drawCurrentState(InterfaceBwdGraphics g) {
        final int stepNum = this.lastStepNumStarted;

        if (DEBUG) {
            if (this.paused) {
                // No log spam while paused.
            } else {
                Dbg.log();
                Dbg.log("drawCurrentState(...) : stepNum = " + stepNum);
            }
        }

        final int h = g.getFont().metrics().height() + 1;
        g.setColor(COLOR_FG);

        int textY = TEXT_Y;
        if (this.done) {
            g.drawText(TEXT_X, textY, "Done.");
        } else {
            if (this.paused) {
                g.drawText(TEXT_X, textY, "Testing... (paused, left-click to unpause)");
            } else {
                g.drawText(TEXT_X, textY, "Testing... (left-click to pause)");
            }
        }
        textY += h;

        g.drawText(TEXT_X, textY, "step num = " + stepNum);
        textY += h;

        g.drawText(TEXT_X, textY, "focus gained (count / expected count) per method:");
        textY += h;
        for (int i = 0; i < MyHostMethod.VALUES.length; i++) {
            final MyHostMethod hostMethod = MyHostMethod.VALUES[i];
            if (!canHostMethodCauseFocusGain(hostMethod)) {
                // Not bothering.
                continue;
            }
            final int nbr = this.nbrOfFocusGainedByMethodOrdinal[i];
            final int nbrExpected = this.nbrOfExpectedFocusGainByMethodOrdinal[i];
            g.drawText(TEXT_X, textY, "(" + nbr + " / " + nbrExpected + ") for " + hostMethod);
            textY += h;
        }
        
        g.drawText(TEXT_X, textY, "error count = " + this.errorCount);
        textY += h;

        for (Object eventInfo : this.eventTestHelper.getEventInfoList()) {
            final String infoStr = eventInfo.toString();
            g.drawText(TEXT_X, textY, infoStr);
            textY += h;
        }
        
        if (DEBUG) {
            if (this.paused) {
                // No log spam while paused.
            } else {
                Dbg.log("drawCurrentState(...) : done");
                Dbg.flush();
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void setShadowedMaxState(InterfaceBwdHost host, boolean value) {
        this.shadowedMaxStateByHost.put(host, value);
    }

    private boolean getShadowedMaxState(InterfaceBwdHost host) {
        // Works even if no mapping for the specified host.
        return this.shadowedMaxStateByHost.get(host) == Boolean.TRUE;
    }

    /**
     * Must be called each time client bounds or target client bounds are updated,
     * i.e. on calls to setClientBounds(...).
     * 
     * Nullifies expected demaxed window bounds.
     */
    private void setExpectedDemaxedClientBounds(InterfaceBwdHost host, GRect clientBounds) {
        if (DEBUG) {
            Dbg.log(hid(host) + " : setting expected demaxed client bounds : " + clientBounds);
        }

        // Should not be maximized-like bounds.
        final GRect screenBounds = getBinding().getScreenBounds();
        final GRect windowBounds = BindingCoordsUtils.computeWindowBounds(
                host.getInsets(),
                clientBounds);
        if (WindowStateTestUtils.doesWindowLookMaximized(screenBounds, windowBounds)) {
            onHostError(hid(host), "setting maximized-like bounds as demaxed client bounds : " + clientBounds);
        }

        this.expectedDemaxedClientBoundsByHost.put(host, clientBounds);
        this.expectedDemaxedWindowBoundsByHost.remove(host);
    }

    /**
     * @return Null if none, i.e. if not defined yet.
     */
    private GRect getExpectedDemaxedClientBounds(InterfaceBwdHost host) {
        return this.expectedDemaxedClientBoundsByHost.get(host);
    }

    /**
     * Must be called each time window bounds or target window bounds are updated,
     * i.e. on calls to setWindowBounds(...).
     * 
     * Nullifies expected demaxed client bounds.
     */
    private void setExpectedDemaxedWindowBounds(InterfaceBwdHost host, GRect windowBounds) {
        if (DEBUG) {
            Dbg.log(hid(host) + " : setting expected demaxed window bounds : " + windowBounds);
        }

        // Should not be maximized-like bounds.
        final GRect screenBounds = getBinding().getScreenBounds();
        if (WindowStateTestUtils.doesWindowLookMaximized(screenBounds, windowBounds)) {
            onHostError(hid(host), "setting maximized-like bounds as demaxed window bounds : " + windowBounds);
        }

        this.expectedDemaxedClientBoundsByHost.remove(host);
        this.expectedDemaxedWindowBoundsByHost.put(host, windowBounds);
    }

    /**
     * @return Null if none, i.e. if not defined yet.
     */
    private GRect getExpectedDemaxedWindowBounds(InterfaceBwdHost host) {
        return this.expectedDemaxedWindowBoundsByHost.get(host);
    }

    private void setExpectedMaxedWindowBounds(InterfaceBwdHost host, GRect windowBounds) {
        if (DEBUG) {
            Dbg.log(hid(host) + " : setting expected maxed window bounds : " + windowBounds);
        }
        this.expectedMaxedWindowBoundsByHost.put(host, windowBounds);
    }

    private GRect getExpectedMaxedWindowBounds(InterfaceBwdHost host) {
        return this.expectedMaxedWindowBoundsByHost.get(host);
    }
    
    /**
     * @param owner Can be null.
     */
    private String getNextHostTitle(
            InterfaceBwdHost owner,
            int posIndex) {
        final int uniq = this.hostTitleUniquifier.incrementAndGet();
        final String uniqStr = Integer.toString(uniq, Character.MAX_RADIX);
        if (owner == null) {
            return "H" + posIndex + "U" + uniqStr;
        } else {
            final String ownerTitle = owner.getTitle();
            final String prefix = ownerTitle.substring(0, ownerTitle.indexOf('U'));
            return prefix + "D" + posIndex + "U" + uniqStr;
        }
    }

    private void removeHostsOfClosedClients() {
        final List<InterfaceBwdHost> toRemoveList = new ArrayList<InterfaceBwdHost>();

        for (Map.Entry<InterfaceBwdHost,BwdClientMock> entry : this.clientByHost.entrySet()) {
            final InterfaceBwdHost host = entry.getKey();
            final BwdClientMock client = entry.getValue();
            if (client.getClientStateClosed()) {
                toRemoveList.add(host);
            }
        }

        for (InterfaceBwdHost host : toRemoveList) {
            if (DEBUG) {
                Dbg.log("removing host (client closed) : " + hid(host));
            }
            this.clientByHost.remove(host);

            final int posIndex = this.posIndexByHost.remove(host);
            this.availablePosIndex.add(posIndex);

            this.shadowedMaxStateByHost.remove(host);
            
            this.expectedDemaxedClientBoundsByHost.remove(host);
            this.expectedDemaxedWindowBoundsByHost.remove(host);
            this.expectedMaxedWindowBoundsByHost.remove(host);
        }
    }

    private GRect computeInitialClientBounds(InterfaceBwdHost host) {
        final int posIndex = this.posIndexByHost.get(host);

        final GRect screenBounds = this.getBinding().getScreenBounds();
        
        final InterfaceBwdHost owner = host.getOwner();

        final GRect initialClientBounds;
        if (owner == null) {
            initialClientBounds = GRect.valueOf(
                    10 + (SUB_CLIENT_WIDTH + 20) * posIndex,
                    screenBounds.ySpan() / 2,
                    SUB_CLIENT_WIDTH,
                    SUB_CLIENT_HEIGHT);
        } else {
            final GRect initialOwnerClientBounds = computeInitialClientBounds(owner);
            initialClientBounds = GRect.valueOf(
                    initialOwnerClientBounds.x() + 10 * posIndex,
                    initialOwnerClientBounds.y() + (SUB_CLIENT_HEIGHT + 30),
                    SUB_CLIENT_WIDTH,
                    SUB_CLIENT_HEIGHT);
        }

        return initialClientBounds;
    }

    private int computeNbrOfRootHosts() {
        int nbrOfRootHosts = 0;
        for (InterfaceBwdHost host : this.clientByHost.keySet()) {
            if (host.getOwner() == null) {
                nbrOfRootHosts++;
            }
        }
        return nbrOfRootHosts;
    }

    /**
     * Doesn't create if our quota are reached.
     */
    private InterfaceBwdHost createOneHostIfRoom() {
        final InterfaceBwdBinding binding = this.getBinding();

        final int nbrOfHosts = this.clientByHost.size();
        if (nbrOfHosts == MAX_NBR_OF_HOSTS) {
            // Global host quota reached.
            return null;
        }

        final int nbrOfRootHosts = this.computeNbrOfRootHosts();

        final boolean rootAllowed = (nbrOfRootHosts < MAX_NBR_OF_ROOT_HOSTS);
        final boolean dialogAllowed = (MAX_HOST_DEPTH > 0);
        if (rootAllowed || dialogAllowed) {
            final boolean decorated =
                    (!mustAllowUndecorated(binding))
                    || this.random.nextBoolean();
            final MyClientMock client = new MyClientMock();

            final List<InterfaceBwdHost> potentialOwnerList = new ArrayList<InterfaceBwdHost>();
            for (InterfaceBwdHost host : this.clientByHost.keySet()) {
                final int hostDepth = getHostDepth(host);
                if ((hostDepth < MAX_HOST_DEPTH)
                        && (host.getDialogList().size() < MAX_NBR_OF_DIALOGS_PER_OWNER)) {
                    potentialOwnerList.add(host);
                }
            }

            final int posIndex = this.availablePosIndex.remove(0);

            final boolean mustCreateRootHost =
                    rootAllowed
                    && ((potentialOwnerList.size() == 0)
                            || this.random.nextBoolean());

            final InterfaceBwdHost owner;
            if (mustCreateRootHost) {
                owner = null;
            } else {
                owner = randomHost(potentialOwnerList);
            }

            final String title = getNextHostTitle(owner, posIndex);

            final InterfaceBwdHost host;
            if (mustCreateRootHost) {
                host = binding.newHost(title, decorated, client);
            } else {
                // If modal, can block on show()
                // (seems a common practice in UI libraries).
                final boolean modal = false;
                host = owner.newDialog(title, decorated, modal, client);
            }
            if (DEBUG) {
                Dbg.log("adding new host : " + hid(host));
            }
            this.clientByHost.put(host, client);

            this.posIndexByHost.put(host, posIndex);

            return host;
        } else {
            return null;
        }
    }
    /**
     * @param hostList Must not be empty.
     */
    private InterfaceBwdHost randomHost(List<InterfaceBwdHost> hostList) {
        final int index = this.random.nextInt(hostList.size());
        return hostList.get(index);
    }

    /**
     * @return List of hosts of depth <= maxHostDepth.
     */
    private List<InterfaceBwdHost> getHostList(int maxHostDepth) {
        final List<InterfaceBwdHost> hostList = new ArrayList<InterfaceBwdHost>();
        for (InterfaceBwdHost host : this.clientByHost.keySet()) {
            final int hostDepth = getHostDepth(host);
            if (hostDepth <= maxHostDepth) {
                hostList.add(host);
            }
        }
        return hostList;
    }

    /**
     * @return 0 for root host, 1 for a dialog from a root host, etc.
     */
    private int getHostDepth(InterfaceBwdHost host) {
        int depth = 0;
        while ((host = host.getOwner()) != null) {
            depth++;
        }
        return depth;
    }

    /*
     * 
     */

    private void eventuallyAlignShadowedMaxStateOnActualState() {
        for (InterfaceBwdHost host : this.clientByHost.keySet()) {
            if (host.isShowing()
                    && (!host.isIconified())) {
                final boolean oldShadowedMaximizedState = getShadowedMaxState(host);
                final boolean newMaximizedState = host.isMaximized();
                if (newMaximizedState != oldShadowedMaximizedState) {
                    if (DEBUG) {
                        Dbg.log(hid(host), "changing shadowed maximized state to host value : " + newMaximizedState);
                    }
                    setShadowedMaxState(host, newMaximizedState);
                }
            }
        }
    }

    private void checkAllHostsAndClientsStatesConsistency_stable() {
        for (InterfaceBwdHost host : this.clientByHost.keySet()) {
            checkHostAndClientStateConsistency_stable_oneArg(host);
        }
    }

    private void checkHostAndClientStateConsistency_stable_oneArg(InterfaceBwdHost host) {
        final BwdClientMock client = this.clientByHost.get(host);
        if (client == null) {
            throw new AssertionError("no more client for " + hid(host));
        }

        final MyState hostState = hostState(host);
        final MyState clientState = clientState(client);
        final GRect insets = host.getInsets();
        final GRect clientBounds = host.getClientBounds();
        final GRect windowBounds = host.getWindowBounds();
        if (DEBUG) {
            Dbg.log(hid(host), "checking states consistency...");
        }
        checkHostAndClientStateConsistencyAndShadowed_stable(
                host,
                hostState,
                clientState,
                insets,
                clientBounds,
                windowBounds);
    }

    private void checkHostForPostStepCheck() {
        final InterfaceBwdHost host = this.hostForPostStepCheck;
        if (host == null) {
            return;
        }
        this.hostForPostStepCheck = null;

        final Object hostId = hid(host);
        
        final BwdClientMock client = this.clientByHost.get(host);
        if (client == null) {
            throw new AssertionError("no more client for " + hid(host));
        }

        final MyState finalHostState = hostState(host);
        final MyState finalClientState = clientState(client);
        final GRect finalInsets = host.getInsets();
        final GRect finalClientBounds = host.getClientBounds();
        final GRect finalWindowBounds = host.getWindowBounds();
        if (DEBUG) {
            Dbg.log();
            Dbg.log(hid(host), "finalHostState = " + finalHostState);
            Dbg.log(hid(host), "finalClientState = " + finalClientState);
            Dbg.log(hid(host), "finalInsets = " + finalInsets);
            Dbg.log(hid(host), "finalClientBounds = " + finalClientBounds);
            Dbg.log(hid(host), "finalWindowBounds = " + finalWindowBounds);
        }
        checkHostAndClientStateConsistencyAndShadowed_stable(
                host,
                finalHostState,
                finalClientState,
                finalInsets,
                finalClientBounds,
                finalWindowBounds);

        if (this.expectedFinalHostState != null) {
            final MyState efhs = this.expectedFinalHostState;
            this.expectedFinalHostState = null;
            final boolean isClientState = false;
            checkStateEqual(
                    hostId,
                    finalHostState,
                    efhs,
                    "(final host state check)",
                    isClientState);
        }
        if (this.expectedFinalClientState != null) {
            final MyState efcs = this.expectedFinalClientState;
            this.expectedFinalClientState = null;
            final boolean isClientState = true;
            checkStateEqual(
                    hostId,
                    finalClientState,
                    efcs,
                    "(final client state check)",
                    isClientState);
        }

        if (MUST_NOT_DO_HARD_CHECKS_ON_FOCUS_GAIN) {
            this.updateFocusGainStatsIfNeeded(finalHostState);
        }
        
        /*
         * Client events flickering.
         */
        
        final List<BwdEventType> finalFlickerEventTypeList =
                createFlickerEventTypeList(client);
        final Set<BwdEventType> finalFlickerEventTypeSet = new HashSet<BwdEventType>(
                finalFlickerEventTypeList);
        if (finalFlickerEventTypeSet.size() != finalFlickerEventTypeList.size()) {
            onHostError(hid(host), "window event flickering : " + finalFlickerEventTypeList);
        }
    }
    
    /*
     * 
     */
    
    /**
     * @return A new list of event types corresponding to events
     *         to check for state flickering detection.
     */
    private static List<BwdEventType> createFlickerEventTypeList(BwdClientMock client) {
        final List<BwdEventType> list = new ArrayList<BwdEventType>();
        for (BwdEvent event : client.getEventList()) {
            final BwdEventType eventType = event.getEventType();
            if ((eventType == BwdEventType.WINDOW_SHOWN)
                    || (eventType == BwdEventType.WINDOW_HIDDEN)
                    || (eventType == BwdEventType.WINDOW_ICONIFIED)
                    || (eventType == BwdEventType.WINDOW_DEICONIFIED)
                    || (eventType == BwdEventType.WINDOW_MAXIMIZED)
                    || (eventType == BwdEventType.WINDOW_DEMAXIMIZED)) {
                list.add(eventType);
            }
        }
        return list;
    }

    /*
     * 
     */

    private void testStep() {

        /*
         * In some cases, such as on Mac with JavaFX,
         * when having a showing/iconified/maximized root host
         * with a dialog, and closing the dialog, the root host
         * becomes showing/deiconified/demaximized.
         * To be robust to these kind of surprising side effects,
         * here we align "shadowed maximized state" to maximized state
         * whenever the host is showing and deiconified.
         * 
         * As a result, we don't need to check shadowed state consistency
         * when we do consistency checks, but we still let the check code
         * in case this piece of code is ever removed.
         */
        this.eventuallyAlignShadowedMaxStateOnActualState();

        /*
         * Checking state and bounds consistency of all hosts,
         * because actions on some hosts can have
         * side-effects on others.
         */
        this.checkAllHostsAndClientsStatesConsistency_stable();

        this.checkHostForPostStepCheck();

        this.checkAllClientsErrors();

        /*
         * 
         */

        // We do it AFTER final state check, so that we can properly check
        // hosts that we just closed, and which clients are closed.
        this.removeHostsOfClosedClients();

        /*
         * 
         */

        this.checkNoOrphanDialog();

        /*
         * Eventually creating a new host.
         */

        InterfaceBwdHost justCreatedHost = null;
        
        if ((this.clientByHost.size() == 0)
                || (this.random.nextDouble() > HOST_CREATION_PROBA)) {
            final InterfaceBwdHost host = this.createOneHostIfRoom();
            if (host != null) {
                justCreatedHost = host;
                
                final GRect targetClientBounds = this.computeInitialClientBounds(host);

                // Setting initial bounds for when it shows up.
                host.setClientBounds(targetClientBounds);
                this.setExpectedDemaxedClientBounds(host, targetClientBounds);
            }
        }
        
        if (justCreatedHost != null) {
            if (BwdTestUtils.isSwtBinding(getBinding())
                    && OsUtils.isMac()) {
                /*
                 * TODO swt Returning to end the step after host creation,
                 * to avoid a terrible issue on Mac with SWT,
                 * where setting a state on a window can have the
                 * side effect of also setting it in a recently created
                 * and unrelated (not a dialog) window, cf. SwtBwdHost.
                 */
                return;
            }
            if (BwdTestUtils.isAlgrBinding(getBinding())
                    && OsUtils.isWindows()) {
                /*
                 * TODO algr On Windows with Allegro, JVM can crash or freeze
                 * if closing a window just after having created a dialog on it.
                 */
                return;
            }
        }

        /*
         * Getting a random host to work on.
         */

        final InterfaceBwdHost host;
        {
            final List<InterfaceBwdHost> hostList = this.getHostList(Integer.MAX_VALUE);
            host = randomHost(hostList);
        }
        final Object hostId = hid(host);
        
        final BwdClientMock client = this.clientByHost.get(host);
        if (client == null) {
            throw new AssertionError("no more client for " + hid(host));
        }

        final MyState oldHostState = hostState(host);
        this.lastOldHostState = oldHostState;
        final MyState oldClientState = clientState(client);
        final GRect oldInsets = host.getInsets();
        final GRect oldClientBounds = host.getClientBounds();
        final GRect oldWindowBounds = host.getWindowBounds();
        if (DEBUG) {
            Dbg.log();
            Dbg.log(hid(host), "oldHostState = " + oldHostState);
            Dbg.log(hid(host), "oldClientState = " + oldClientState);
            Dbg.log(hid(host), "oldInsets = " + oldInsets);
            Dbg.log(hid(host), "oldClientBounds = " + oldClientBounds);
            Dbg.log(hid(host), "oldWindowBounds = " + oldWindowBounds);
        }
        {
            final boolean possibleUnstability = (justCreatedHost != null);
            if (possibleUnstability) {
                /*
                 * TODO algr With Allegro, on Windows, creating a window
                 * might immediately remove focus from focused window,
                 * before we have time to fire FOCUS_LOST event to its client,
                 * in which case consistency checks would fail.
                 * For safety, we don't do these checks if we just created a host.
                 * NB: At some point we didn't have this issue (or did less checks?).
                 */
            } else {
                checkHostAndClientStateConsistencyAndShadowed_stable(
                        host,
                        oldHostState,
                        oldClientState,
                        oldInsets,
                        oldClientBounds,
                        oldWindowBounds);
            }
        }
        
        /*
         * Calling a random host method,
         * or closing the host if it's a dangerous one.
         */

        final MyHostMethod hostMethod;
        if (isHostGoingNuts(host)) {
            if (DEBUG) {
                Dbg.log(hid(host), "host is going nuts: closing it");
            }
            // Let's defuse the bomb.
            hostMethod = MyHostMethod.CLOSE;
        } else {
            hostMethod = computeRandomHostMethod(oldHostState);
        }
        if (DEBUG) {
            Dbg.log(hid(host), "hostMethod = " + hostMethod);
        }
        
        final AbstractBwdHost hostAbs = (AbstractBwdHost) host;

        final boolean iconificationExpectedToWork =
                hostAbs.gest_doesSetBackingWindowIconifiedOrNotWork();
        final boolean maximizationExpectedToWork =
                hostAbs.gest_doesSetBackingWindowMaximizedOrNotWork();

        // Eventually updating shadowed maximized state.
        if (maximizationExpectedToWork) {
            if (oldHostState.isShowing()
                    && (!oldHostState.isIconified())) {
                if (hostMethod == MyHostMethod.MAXIMIZE) {
                    this.setShadowedMaxState(host, true);
                } else if (hostMethod == MyHostMethod.DEMAXIMIZE) {
                    this.setShadowedMaxState(host, false);
                }
            }
        }
        
        // Eventually updating expected bounds.
        if (oldHostState.isShowing()
                && (!oldHostState.isIconified())) {
            if (oldHostState.isMaximized()) {
                this.setExpectedMaxedWindowBounds(host, oldWindowBounds);
            } else {
                if (!mustCheckOtherWindowsBoundsMethods()) {
                    /*
                     * Bounds might have been changed,
                     * if we allow for side effects on bounds of other windows.
                     */
                    this.setExpectedDemaxedClientBounds(host, oldClientBounds);
                }
            }
        }

        {
            final long nowNs = getBinding().getUiThreadScheduler().getClock().getTimeNs();
            final String timeStr = BwdTestUtils.timeNsToStringS(nowNs);
            this.eventTestHelper.addCustomEventInfo(
                    timeStr
                    + " "
                    + hid(host)
                    + "."
                    + hostMethod
                    + "     (was "
                    + oldHostState
                    + ")");
        }
        this.lastHostMethod = hostMethod;
        
        /*
         * 
         */
        
        // We just check what happens next to each host method call.
        client.clearStoredEvents();
        
        final GRect boundsToSet;
        if ((hostMethod == MyHostMethod.SET_CLIENT_BOUNDS)
                || (hostMethod == MyHostMethod.SET_WINDOW_BOUNDS)) {
            /*
             * Random bounds around initial client bounds.
             * Taking case not to specify too small bounds,
             * in particular considering eventual window decoration,
             * else they would not exactly be met.
             */
            final GRect initialClientBounds = this.computeInitialClientBounds(host);
            boundsToSet = GRect.valueOf(
                    initialClientBounds.x() + this.randomPlusMinus(10),
                    initialClientBounds.y() + this.randomPlusMinus(10),
                    initialClientBounds.xSpan() + RANDOM_BOUNDS_BONUS_SPAN + this.random.nextInt(11),
                    initialClientBounds.ySpan() + RANDOM_BOUNDS_BONUS_SPAN + this.random.nextInt(11));
            if (DEBUG) {
                Dbg.log(hid(host), "boundsToSet = " + boundsToSet);
            }
        } else {
            boundsToSet = null;
        }
        
        try {
            if (DEBUG) {
                Dbg.log();
            }
            callHostMethod(host, hostMethod, boundsToSet);
        } catch (Throwable t) {
            Dbg.log();
            Dbg.log(hid(host), "exception on " + hostMethod + " host method call", t);
            if (MUST_THROW_ON_EVENT) {
                /*
                 * Expected.
                 * Need to catch, else we don't run subsequent test code,
                 * and tests might stop due to exception reaching test scheduling.
                 */
            } else {
                /*
                 * Not expected.
                 */
                
                // Logging fresh states.
                this.logAllStates();
                
                onHostError(hid(host), "exception on " + hostMethod + " host method call");
            }
        }
        
        /*
         * 
         */
        
        if (host.isClosed()) {
            /*
             * Checking closes hosts are not too functional.
             */
            final boolean decorated = false;
            final boolean modal = false;
            final MyClientMock dummyClient = new MyClientMock();
            try {
                host.newDialog("title", decorated, modal, dummyClient);
                
                onHostError(hid(host), "no ISE when creating dialog on closed host");
                throw new AssertionError();
            } catch (IllegalStateException e) {
                // ok
            }
        }
        
        /*
         * Checking state consistency (not necessarily expecting
         * final state yet) just after method call.
         */

        final MyState newHostState = hostState(host);
        final MyState newClientState = clientState(client);
        final GRect newInsets = host.getInsets();
        final GRect newClientBounds = host.getClientBounds();
        final GRect newWindowBounds = host.getWindowBounds();
        if (DEBUG) {
            Dbg.log();
            Dbg.log(hid(host), "newHostState = " + newHostState);
            Dbg.log(hid(host), "newClientState = " + newClientState);
            Dbg.log(hid(host), "newInsets = " + newInsets);
            Dbg.log(hid(host), "newClientBounds = " + newClientBounds);
            Dbg.log(hid(host), "newWindowBounds = " + newWindowBounds);
        }
        {
            final boolean isStable = false;
            checkHostAndClientStateConsistency_stableOrNot(
                    isStable,
                    //
                    hostId,
                    newHostState,
                    newClientState,
                    newInsets,
                    newClientBounds,
                    newWindowBounds);
        }
        
        /*
         * Computing expected final (i.e. possibly after some time)
         * host and client states, and doing checks when a new state
         * is expected to be met just after method call.
         */
        
        final MyNewStateComputerAndChecker hNscac = new MyNewStateComputerAndChecker(
                hostId,
                oldHostState,
                newHostState);
        final MyNewStateComputerAndChecker cNscac = new MyNewStateComputerAndChecker(
                "" + hostId + "(client)",
                oldClientState,
                newClientState);
        
        if (hostMethod == MyHostMethod.SHOW) {
            if (!oldHostState.isShowing()) {
                hNscac.setExpectedFinalStateBtAndCheckAlreadyMet(MyBt.SHOWING, true);
                cNscac.setExpectedFinalStateBt(MyBt.SHOWING, true);
            }

            {
                hNscac.setExpectedFinalStateBt(MyBt.FOCUSED, true);
                cNscac.setExpectedFinalStateBt(MyBt.FOCUSED, true);
            }

            {
                if (oldHostState.isIconified()) {
                    // host.show() must try to deiconify,
                    // and host.isIconified() must return the client state when showing.
                    hNscac.setExpectedFinalStateBt(MyBt.ICONIFIED, false);
                }
                if (oldClientState.isIconified()) {
                    // host.show() must try to deiconify.
                    cNscac.setExpectedFinalStateBt(MyBt.ICONIFIED, false);
                }
            }

            {
                // show() supposed to ensure pre-hiding or pre-iconification maximized state.
                final boolean expectedNewValue = getShadowedMaxState(host);
                hNscac.setExpectedFinalStateBt(MyBt.MAXIMIZED, expectedNewValue);
                cNscac.setExpectedFinalStateBt(MyBt.MAXIMIZED, expectedNewValue);
            }
        } else if (hostMethod == MyHostMethod.HIDE) {
            if (oldHostState.isShowing()) {
                {
                    hNscac.setExpectedFinalStateBtAndCheckAlreadyMet(MyBt.SHOWING, false);
                    cNscac.setExpectedFinalStateBt(MyBt.SHOWING, false);
                }

                if (oldHostState.isFocused()) {
                    // host.isFocused() must return false when hidden.
                    hNscac.setExpectedFinalStateBtAndCheckAlreadyMet(MyBt.FOCUSED, false);
                    cNscac.setExpectedFinalStateBt(MyBt.FOCUSED, false);
                }

                if (oldHostState.isIconified()) {
                    // host.isIconified() must return false when hidden.
                    hNscac.setExpectedFinalStateBtAndCheckAlreadyMet(MyBt.ICONIFIED, false);
                }

                if (oldHostState.isMaximized()) {
                    // host.isMaximized() must return false when hidden.
                    hNscac.setExpectedFinalStateBtAndCheckAlreadyMet(MyBt.MAXIMIZED, false);
                }
            }
        } else if (hostMethod == MyHostMethod.REQUEST_WINDOW_FOCUS_GAIN) {
            if (oldHostState.isShowing()
                    && (!oldHostState.isIconified())
                    && (!oldHostState.isFocused())) {
                hNscac.setExpectedFinalStateBt(MyBt.FOCUSED, true);
                cNscac.setExpectedFinalStateBt(MyBt.FOCUSED, true);
            }
        } else if (hostMethod == MyHostMethod.ICONIFY) {
            if (oldHostState.isShowing()) {
                if (iconificationExpectedToWork) {
                    if (oldHostState.isFocused()) {
                        // Allowing delayed change due to iconified state flickering,
                        // causing host.isFocused() to still return true.
                        hNscac.setExpectedFinalStateBt(MyBt.FOCUSED, false);
                        cNscac.setExpectedFinalStateBt(MyBt.FOCUSED, false);
                    }

                    {
                        // Allowing delayed change due to iconified state flickering.
                        hNscac.setExpectedFinalStateBt(MyBt.ICONIFIED, true);
                        cNscac.setExpectedFinalStateBt(MyBt.ICONIFIED, true);
                    }

                    if (oldHostState.isMaximized()) {
                        // host.isMaximized() must return false when iconified.
                        // Allowing delayed change due to maximized state flickering.
                        hNscac.setExpectedFinalStateBt(MyBt.MAXIMIZED, false);
                    }
                }
            }
        } else if (hostMethod == MyHostMethod.DEICONIFY) {
            if (oldHostState.isShowing()) {
                {
                    // Allowing delayed change due to iconified state flickering.
                    hNscac.setExpectedFinalStateBt(MyBt.ICONIFIED, false);
                    cNscac.setExpectedFinalStateBt(MyBt.ICONIFIED, false);
                }

                {
                    final boolean expectedNewValue = getShadowedMaxState(host);
                    // Allowing delayed change due to maximized state flickering.
                    hNscac.setExpectedFinalStateBt(MyBt.MAXIMIZED, expectedNewValue);
                    cNscac.setExpectedFinalStateBt(MyBt.MAXIMIZED, expectedNewValue);
                }

                {
                    hNscac.setExpectedFinalStateBt(MyBt.FOCUSED, true);
                    cNscac.setExpectedFinalStateBt(MyBt.FOCUSED, true);
                }
            }
        } else if (hostMethod == MyHostMethod.MAXIMIZE) {
            if (oldHostState.isShowing()
                    && (!oldHostState.isIconified())) {
                if (!oldHostState.isMaximized()) {
                    if (maximizationExpectedToWork) {
                        // Allowing delayed change due to maximized state flickering.
                        hNscac.setExpectedFinalStateBt(MyBt.MAXIMIZED, true);
                        cNscac.setExpectedFinalStateBt(MyBt.MAXIMIZED, true);
                    }
                }

                {
                    hNscac.setExpectedFinalStateBt(MyBt.FOCUSED, true);
                    cNscac.setExpectedFinalStateBt(MyBt.FOCUSED, true);
                }
            }
        } else if (hostMethod == MyHostMethod.DEMAXIMIZE) {
            if (oldHostState.isShowing()
                    && (!oldHostState.isIconified())) {
                if (oldHostState.isMaximized()) {
                    // Allowing delayed change due to maximized state flickering.
                    hNscac.setExpectedFinalStateBt(MyBt.MAXIMIZED, false);
                    cNscac.setExpectedFinalStateBt(MyBt.MAXIMIZED, false);
                }

                {
                    hNscac.setExpectedFinalStateBt(MyBt.FOCUSED, true);
                    cNscac.setExpectedFinalStateBt(MyBt.FOCUSED, true);
                }
            }
        } else if (hostMethod == MyHostMethod.SET_CLIENT_BOUNDS) {
            if (boundsToSet != null) {
                this.setExpectedDemaxedClientBounds(host, boundsToSet);
            }
        } else if (hostMethod == MyHostMethod.SET_WINDOW_BOUNDS) {
            if (boundsToSet != null) {
                this.setExpectedDemaxedWindowBounds(host, boundsToSet);
            }
        } else if (hostMethod == MyHostMethod.CLOSE) {
            if (!oldHostState.isClosed()) {
                {
                    hNscac.setExpectedFinalStateBtAndCheckAlreadyMet(MyBt.CLOSED, true);
                    cNscac.setExpectedFinalStateBt(MyBt.CLOSED, true);
                }

                if (oldHostState.isShowing()) {
                    // host.isShowing() must return false when closed.
                    hNscac.setExpectedFinalStateBtAndCheckAlreadyMet(MyBt.SHOWING, false);
                    // Hidden client state must be ensured on closing.
                    cNscac.setExpectedFinalStateBt(MyBt.SHOWING, false);
                }

                if (oldHostState.isFocused()) {
                    // host.isFocused() must return false when closed.
                    hNscac.setExpectedFinalStateBtAndCheckAlreadyMet(MyBt.FOCUSED, false);
                    // Unfocused client state must be ensured on closing.
                    cNscac.setExpectedFinalStateBt(MyBt.FOCUSED, false);
                }

                if (oldHostState.isIconified()) {
                    // host.isIconified() must return false when closed (hence not showing).
                    hNscac.setExpectedFinalStateBtAndCheckAlreadyMet(MyBt.ICONIFIED, false);
                }

                if (oldHostState.isMaximized()) {
                    // host.isMaximized() must return false when closed (hence not showing).
                    hNscac.setExpectedFinalStateBtAndCheckAlreadyMet(MyBt.MAXIMIZED, false);
                }
            }
        }

        /*
         * Checking new states just after host method call
         * (only checking when a state is specified as not delayable,
         * to avoid issues with transient states, which can also occur
         * for states that will end up unchanged after eventual flickering),
         * and computing expected final states,
         * and taking invariants that can make host and client
         * states different into account.
         */

        final MyState expectedFinalHostState = hNscac.getExpectedFinalState();
        final MyState expectedFinalClientState = cNscac.getExpectedFinalState();
        
        if (DEBUG) {
            Dbg.log();
            Dbg.log(hid(host), "expectedFinalHostState = " + expectedFinalHostState);
            Dbg.log(hid(host), "expectedFinalClientState = " + expectedFinalClientState);
            Dbg.log(hid(host), "expected demaxed client bounds = " + getExpectedDemaxedClientBounds(host));
            Dbg.log(hid(host), "expected demaxed window bounds = " + getExpectedDemaxedWindowBounds(host));
            Dbg.log(hid(host), "expected maxed window bounds = " + getExpectedMaxedWindowBounds(host));
            Dbg.log();
        }
        this.hostForPostStepCheck = host;
        this.expectedFinalHostState = expectedFinalHostState;
        this.expectedFinalClientState = expectedFinalClientState;
    }

    /*
     * 
     */

    /**
     * @return Whether the specified host looks like it's going nuts,
     *         and we better close it ASAP.
     */
    private boolean isHostGoingNuts(InterfaceBwdHost host) {
        /*
         * TODO jfx Can have decorated dialogs (or regular hosts as well?)
         * with negative position (which itself might be due to another JavaFX issue)
         * stay maximized on demaximization (cf. JfxBwdHost.isBackingWindowMaximized()),
         * or have bad bounds on deiconification, or maybe also on show or else.
         * NB: At some point we didn't have these issues.
         *     Maybe due to changes from Windows Update.
         */
        if (BwdTestUtils.isJfxBinding(getBinding())
                && OsUtils.isWindows()) {
            if (host.isDecorated()) {
                if (host.isShowing()
                        && (!host.isIconified())) {
                    final GRect windowBounds = host.getWindowBounds();
                    if ((windowBounds.x() < 0)
                            || (windowBounds.y() <= 0)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private MyHostMethod computeRandomHostMethod(MyState state) {
        final int nbrOfMethods = MyHostMethod.VALUES.length;
        MyHostMethod hostMethod = MyHostMethod.VALUES[random.nextInt(nbrOfMethods)];
        
        /*
         * Helping getting out of hidden/iconified states,
         * by forcing out with the most obvious method half the time
         * (but not more often, to also often use other methods).
         */
        if ((!state.isShowing())
                && (hostMethod != MyHostMethod.SHOW)
                && (this.random.nextDouble() < 0.5)) {
            hostMethod = MyHostMethod.SHOW;
        } else if (state.isIconified()
                && (hostMethod != MyHostMethod.DEICONIFY)
                && (this.random.nextDouble() < 0.5)) {
            hostMethod = MyHostMethod.DEICONIFY;
        }
        
        if (!MUST_CALL_SET_BOUNDS_METHODS) {
            if (hostMethod == MyHostMethod.SET_CLIENT_BOUNDS) {
                hostMethod = MyHostMethod.ICONIFY;
            }
            if (hostMethod == MyHostMethod.SET_WINDOW_BOUNDS) {
                hostMethod = MyHostMethod.DEICONIFY;
            }
        }

        return hostMethod;
    }

    /*
     * 
     */

    private void checkNoOrphanDialog() {
        for (InterfaceBwdHost host : this.clientByHost.keySet()) {
            // Ignoring "closed" dialogs that still appear in the map
            // due to CLOSED event not having being fired for their client yet,
            // which can occur in case of exceptions in user listeners.
            if (host.isDialog()
                    && (!host.isClosed())) {
                final InterfaceBwdHost owner = host.getOwner();
                if (owner.isClosed()) {
                    onHostError(hid(host), "dialog is not closed, but its owner " + hid(owner) + " is closed");
                }
            }
        }
    }

    /*
     * 
     */

    private void checkHostOrClientStateConsistency_stableOrNot(
            Object hostId,
            MyState state) {
        if (state.isClosed()) {
            if (state.isShowing()) {
                onHostError(hostId, "state closed but also showing : " + state);
            }
            if (state.isFocused()) {
                onHostError(hostId, "state closed but also focused : " + state);
            }
        }
        if (!state.isShowing()) {
            if (state.isFocused()) {
                onHostError(hostId, "state hidden but also focused : " + state);
            }
        }
        if (state.isIconified()) {
            if (state.isFocused()) {
                onHostError(hostId, "state iconified but also focused : " + state);
            }
        }
    }

    /**
     * Check constraints specific to host state,
     * i.e. that don't apply to client state.
     */
    private void checkHostOnlyStateConsistency_stableOrNot(
            Object hostId,
            MyState hostState) {
        if (!hostState.isShowing()) {
            /*
             * Not allowing these, to make binding implementations easier,
             * because some libraries "isIconified()" or "isMaximized()" methods
             * (might) return false when hidden, even though showing then brings back
             * a iconified or maximized window.
             */
            if (hostState.isIconified()) {
                onHostError(hostId, "host state closed but also iconified : " + hostState);
            }
            if (hostState.isMaximized()) {
                onHostError(hostId, "host state closed but also maximized : " + hostState);
            }
        }
        if (hostState.isIconified()) {
            if (hostState.isMaximized()) {
                /*
                 * Not allowing that, to make binding implementations easier,
                 * because some libraries "isMaximized()" methods (might) return false
                 * when iconified, even though de-iconification then brings back
                 * a maximized window.
                 */
                onHostError(hostId, "host state iconified but also maximized : " + hostState);
            }
        }
    }
    
    private void checkHostAndClientBoundsConsistency_stableOrNot(
            Object hostId,
            MyState hostState,
            GRect insets,
            GRect clientBounds,
            GRect windowBounds) {
        
        if (hostState.isShowing()
                && (!hostState.isIconified())) {
            if (insets.x() < 0) {
                onHostError(hostId, "insets.left is negative : " + insets.x());
            }
            if (insets.y() < 0) {
                onHostError(hostId, "insets.top is negative : " + insets.y());
            }
            
            final GRect consistentWindowBounds =
                    BindingCoordsUtils.computeWindowBounds(
                            insets,
                            clientBounds);
            
            if (!windowBounds.equals(consistentWindowBounds)) {
                onHostError(hostId, "inconsistent bounds : expected window bounds = "
                        + consistentWindowBounds + ", actual window bounds = " + windowBounds);
            }
        } else {
            if (!insets.equals(GRect.DEFAULT_EMPTY)) {
                onHostError(hostId, "insets not DEFAULT_EMPTY : " + insets);
            }
            if (!clientBounds.equals(GRect.DEFAULT_EMPTY)) {
                onHostError(hostId, "client bounds not DEFAULT_EMPTY : " + insets);
            }
            if (!windowBounds.equals(GRect.DEFAULT_EMPTY)) {
                onHostError(hostId, "window bounds not DEFAULT_EMPTY : " + insets);
            }
        }
    }
    
    /*
     * 
     */

    /**
     * To check possibly unstable states consistencies
     * (during eventual transient states).
     * 
     * @param isStable True if state should be stable (i.e. some time after last action),
     *        false if it might still be unstable.
     */
    private void checkHostAndClientStateConsistency_stableOrNot(
            boolean isStable,
            //
            Object hostId,
            MyState hostState,
            MyState clientState,
            GRect insets,
            GRect clientBounds,
            GRect windowBounds) {

        checkHostOrClientStateConsistency_stableOrNot(hostId, clientState);
        checkHostOrClientStateConsistency_stableOrNot(hostId, hostState);

        checkHostOnlyStateConsistency_stableOrNot(hostId, hostState);
        
        final boolean mustCheckBoundsConsistency;
        if (isStable) {
            mustCheckBoundsConsistency = true;
        } else {
            /*
             * TODO jfx Can (if unlucky) have inconsistent bounds
             * when state is not stable, due to JavaFX splitting
             * bounds in multiple values updated separately,
             * so in this case we don't do bounds consistency checks.
             */
            final boolean canBindingHaveInconsistentBounds =
                    BwdTestUtils.isJfxBinding(getBinding());
            mustCheckBoundsConsistency = !canBindingHaveInconsistentBounds;
        }
        if (mustCheckBoundsConsistency) {
            checkHostAndClientBoundsConsistency_stableOrNot(
                    hostId,
                    hostState,
                    insets,
                    clientBounds,
                    windowBounds);
        }
    }
    
    /**
     * To check stable states consistencies (after eventual transient states).
     */
    private void checkHostAndClientStateConsistencyAndShadowed_stable(
            InterfaceBwdHost host,
            MyState hostState,
            MyState clientState,
            GRect insets,
            GRect clientBounds,
            GRect windowBounds) {

        final Object hostId = hid(host);
        
        final boolean isStable = true;
        checkHostAndClientStateConsistency_stableOrNot(
                isStable,
                //
                hostId,
                hostState,
                clientState,
                insets,
                clientBounds,
                windowBounds);
        
        /*
         * Cross-state checks.
         * NB: Errors here are often due to not waiting long enough
         * before checks, unless a bug causes an inconsistency to be
         * a final state.
         */
        
        final boolean shadowedMaxState = getShadowedMaxState(host);

        if ((hostState.isShowing() != clientState.isShowing())
                || (hostState.isFocused() != clientState.isFocused())
                || (hostState.isClosed() != clientState.isClosed())) {
            onHostError(hostId, "{showing/focused/closed} states differ : host = " + hostState + ", client = " + clientState);
        }
        if (hostState.isShowing()) {
            if (hostState.isIconified() != clientState.isIconified()) {
                onHostError(hostId, "host state showing : " + hostState + ", but client has different iconified state : " + clientState);
            }
        }
        if (hostState.isShowing()
                && (!hostState.isIconified())) {
            if (hostState.isMaximized() != clientState.isMaximized()) {
                onHostError(hostId, "host state showing and not iconified : " + hostState + ", but client has different maximizedState : " + clientState);
            }
            if (hostState.isMaximized() != shadowedMaxState) {
                onHostError(hostId, "host state showing and not iconified : " + hostState + ", but shadowed maximizedState = " + shadowedMaxState);
            }
        }
        
        /*
         * Stable bounds checks.
         */
        
        if ((!mustCheckOtherWindowsBoundsMethods())
                && (host != this.hostForPostStepCheck)) {
            if (DEBUG) {
                Dbg.log("not checking bounds for host " + hid(host));
            }
            return;
        }

        // Null if no value to check (not defined,
        // or not bothering due to having the other value
        // to check already).
        final GRect expectedClientBounds;
        final GRect expectedWindowBounds;
        if (hostState.isShowing()
                && (!hostState.isIconified())) {
            if (hostState.isMaximized()) {
                // Checking that close enough from screen bounds.
                final GRect screenBounds = this.getBinding().getScreenBounds();
                if (!WindowStateTestUtils.doesWindowLookMaximized(screenBounds, windowBounds)) {
                    onHostError(hostId,
                            "screen bounds = " + screenBounds
                            + ", maximized window bounds = " + windowBounds);
                }

                expectedClientBounds = null;
                expectedWindowBounds = this.getExpectedMaxedWindowBounds(host);
            } else {
                expectedClientBounds = this.getExpectedDemaxedClientBounds(host);
                expectedWindowBounds = this.getExpectedDemaxedWindowBounds(host);
            }
        } else {
            expectedClientBounds = GRect.DEFAULT_EMPTY;
            expectedWindowBounds = GRect.DEFAULT_EMPTY;
        }

        if (expectedClientBounds != null) {
            if (!clientBounds.equals(expectedClientBounds)) {
                onHostError(hostId,
                        "expected client bounds " + expectedClientBounds
                        + ", but got = " + clientBounds);
            }
        }
        if (expectedWindowBounds != null) {
            if (!windowBounds.equals(expectedWindowBounds)) {
                onHostError(hostId,
                        "expected window bounds " + expectedWindowBounds
                        + ", but got = " + windowBounds);
            }
        }
    }

    /*
     * 
     */

    private void checkAllClientsErrors() {
        for (BwdClientMock client : this.clientByHost.values()) {
            final List<String> errorList = client.getErrorList();
            for (String error : errorList) {
                onError(error);
            }
        }
    }

    /*
     * 
     */

    private void checkStateBtEqualTo(
            Object hostId,
            MyState state,
            MyBt bt,
            boolean expected) {
        if (state.is(bt) != expected) {
            if (MUST_NOT_DO_HARD_CHECKS_ON_FOCUS_GAIN
                    && (bt == MyBt.FOCUSED)) {
                /*
                 * Never passing here for focused state,
                 * which is always either delayed (so checked in final state),
                 * or unchanged (so as expected).
                 */
                throw new AssertionError();
            } else {
                onHostError(hostId, "expected " + bt + " = " + expected + ", got " + state);
            }
        }
    }

    private void checkStateEqual(
            Object hostId,
            MyState state,
            MyState expected,
            String prefix,
            boolean isClientState) {
        boolean foundError = false;
        for (MyBt bt : MyBt.VALUES) {
            if (state.is(bt) != expected.is(bt)) {
                if (MUST_NOT_DO_HARD_CHECKS_ON_FOCUS_GAIN
                        && (bt == MyBt.FOCUSED)) {
                    // Ignoring.
                } else if (MUST_ALLOW_FOR_CLIENT_DEMAX_ON_ICONIFY
                        && isClientState
                        && (bt == MyBt.MAXIMIZED)
                        && (state.is(MyBt.ICONIFIED))
                        && expected.isMaximized()) {
                    // Allowing client maximized state to become false.
                } else {
                    foundError = true;
                    break;
                }
            }
        }
        if (foundError) {
            onHostError(hostId, prefix + " expected " + expected + ", got " + state);
        }
    }
    
    private static boolean canHostMethodCauseFocusGain(MyHostMethod hostMethod) {
        return (hostMethod == MyHostMethod.SHOW)
                || (hostMethod == MyHostMethod.REQUEST_WINDOW_FOCUS_GAIN)
                || (hostMethod == MyHostMethod.DEICONIFY)
                || (hostMethod == MyHostMethod.MAXIMIZE)
                || (hostMethod == MyHostMethod.DEMAXIMIZE);
    }

    private static boolean shouldHostMethodCauseFocusGain(
            MyHostMethod hostMethod,
            MyState lastOldHostState) {
        if (hostMethod == MyHostMethod.SHOW) {
            // Always true because should also deiconify.
            return true;
        }
        if (hostMethod == MyHostMethod.DEICONIFY) {
            return lastOldHostState.isShowing();
        }
        if ((hostMethod == MyHostMethod.REQUEST_WINDOW_FOCUS_GAIN)
                || (hostMethod == MyHostMethod.MAXIMIZE)
                || (hostMethod == MyHostMethod.DEMAXIMIZE)) {
            return lastOldHostState.isShowing()
                    && (!lastOldHostState.isIconified());
        }
        return false;
    }

    private void updateFocusGainStatsIfNeeded(MyState finalHostState) {
        
        final MyState lastOldHostState = this.lastOldHostState;
        final MyHostMethod hostMethod = this.lastHostMethod;
        
        if (!lastOldHostState.isFocused()) {
            if (finalHostState.isFocused()) {
                this.nbrOfFocusGainedByMethodOrdinal[hostMethod.ordinal()]++;
            }
            if (shouldHostMethodCauseFocusGain(
                    hostMethod,
                    lastOldHostState)) {
                this.nbrOfExpectedFocusGainByMethodOrdinal[hostMethod.ordinal()]++;
            }
        }
    }
    
    /**
     * @param boundsToSet Only used if host method is SET_CLIENT_BOUNDS
     *        or SET_WINDOW_BOUNDS.
     *        Can be null, in which case no bounds setting is done.
     */
    private static void callHostMethod(
            InterfaceBwdHost host,
            MyHostMethod hostMethod,
            GRect boundsToSet) {
        switch (hostMethod) {
        case SHOW: {
            host.show();
        } break;
        case HIDE: {
            host.hide();
        } break;
        case REQUEST_WINDOW_FOCUS_GAIN: {
            host.requestFocusGain();
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
        case SET_CLIENT_BOUNDS: {
            if (boundsToSet != null) {
                host.setClientBounds(boundsToSet);
            }
        } break;
        case SET_WINDOW_BOUNDS: {
            if (boundsToSet != null) {
                host.setWindowBounds(boundsToSet);
            }
        } break;
        case CLOSE: {
            host.close();
        } break;
        default:
            throw new AssertionError("" + hostMethod);
        }
    }

    private static MyState hostState(InterfaceBwdHost host) {
        return new MyState(
                host.isShowing(),
                host.isFocused(),
                host.isIconified(),
                host.isMaximized(),
                host.isClosed());
    }

    private static MyState clientState(BwdClientMock client) {
        return new MyState(
                client.getClientStateShowing(),
                client.getClientStateFocused(),
                client.getClientStateIconified(),
                client.getClientStateMaximized(),
                client.getClientStateClosed());
    }

    /*
     * 
     */

    private void closeAllHosts() {
        for (InterfaceBwdHost host : this.clientByHost.keySet()) {
            try {
                host.close();
            } catch (MyExceptionForTest ignore) {
            }
        }
        this.clientByHost.clear();
    }
    
    /*
     * 
     */
    
    private int randomPlusMinus(int posVal) {
        return BindingCoordsUtils.roundToInt((posVal + 0.5) * (1.0 - 2.0 * this.random.nextDouble()));
    }

    /*
     * 
     */

    /**
     * To help identity the host in logs.
     */
    private static Object hid(InterfaceBwdHost host) {
        return host.getTitle();
    }

    private void logAllStates() {
        Dbg.log("all states:");
        for (InterfaceBwdHost host : this.getHostList(Integer.MAX_VALUE)) {
            final AbstractBwdHost hostAbs = (AbstractBwdHost) host;
            final BwdClientMock client = this.clientByHost.get(host);
            
            hostAbs.logInternalState();
            
            Dbg.log(hid(host) + " : (decorated = " + host.isDecorated() + ")");
            Dbg.log(hid(host) + " : host state =   " + hostState(host));
            Dbg.log(hid(host) + " : client state = " + clientState(client));
            Dbg.log(hid(host) + " : (test state) shadowed maximized state =  " + getShadowedMaxState(host));
            Dbg.log(hid(host) + " : insets =  " + host.getInsets());
            Dbg.log(hid(host) + " : client bounds = " + host.getClientBounds());
            Dbg.log(hid(host) + " : window bounds = " + host.getWindowBounds());
            Dbg.log(hid(host) + " : (test state) expected demaxed client bounds = " + getExpectedDemaxedClientBounds(host));
            Dbg.log(hid(host) + " : (test state) expected demaxed window bounds = " + getExpectedDemaxedWindowBounds(host));
            Dbg.log(hid(host) + " : (test state) expected maxed window bounds = " + getExpectedMaxedWindowBounds(host));
        }
    }

    private void onHostError(Object hostId, String errorMsg) {
        this.onError(hostId + " : " + errorMsg);
    }
    
    private void onError(String errorMsg) {
        this.errorCount++;
        Dbg.log("ERROR (step " + this.lastStepNumStarted + ") : " + errorMsg);

        if (MUST_EXIT_ON_ERROR) {
            Dbg.flushAndExit();
        } else if (MUST_PAUSE_ON_ERROR) {
            if (!this.done) {
                this.paused = true;
                this.getHost().ensurePendingClientPainting();
            }
            Dbg.logStrackTrace();
        } else {
            // Just counting on error logs.
        }
    }
}
