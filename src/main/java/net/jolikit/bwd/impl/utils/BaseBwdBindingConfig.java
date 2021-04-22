/*
 * Copyright 2019-2021 Jeff Hain
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

import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.InterfaceDefaultFontInfoComputer;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.impl.utils.sched.HardClockTimeType;
import net.jolikit.lang.DefaultExceptionHandler;

/**
 * Base class containing configuration parameters common to all bindings
 * (even if current versions of some bindings don't yet support some of them).
 */
public class BaseBwdBindingConfig {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * For default exception handler.
     */
    private static final boolean DEFAULT_MUST_SWALLOW_ELSE_RETHROW = true;
    
    /**
     * Stream for bindings to log eventual issues that are not worth
     * throwing a BindingError for, or to log information before
     * throwing one.
     * 
     * Not designed to be a heavy logging framework: if you want to use one,
     * just provide a stream that forwards these logs to your logging framework
     * as warnings.
     * 
     * Sometimes exceptions might be just thrown and this stream not used,
     * to keep things simple.
     * 
     * Taking care to use System.out by default, and not System.err,
     * because we don't like using System.err, even for errors,
     * for using two different streams can cause inconsistent
     * logs ordering and logs intertwining.
     */
    private PrintStream issueStream = System.out;
    
    /**
     * UncaughtExceptionHandler is designed for exceptions uncaught
     * by developer's code and reaching a thread's runnable,
     * but it also works for exceptions thrown by runnables executed
     * in (binding's) scheduler(s), which is what this handler is mainly for,
     * and also eventually for exceptions thrown by clients on host events
     * or other client method calls.
     * 
     * About uncaughtException(Thread thread, Throwable throwable)
     * usage and expected behavior here:
     * The thread is the one in which the specified throwable was caught
     * (and typically thrown as well).
     * Must throw an exception (anything) if the calling code
     * is no match to handle the specified exception, or nothing if
     * it took care of the issue and treatments can keep going
     * (would be cleaner to just return a boolean to indicate if treatments
     * must continue, but we don't want to create a new API just for that).
     */
    private UncaughtExceptionHandler exceptionHandler =
            new DefaultExceptionHandler(
                    DEFAULT_MUST_SWALLOW_ELSE_RETHROW);
    
    /**
     * Whether exception handler must be used to handle exceptions thrown
     * by clients on host events or other client method calls.
     * True by default, because backing libraries often behave badly when
     * exceptions are thrown in their event listeners (but our bindings
     * are designed not to be hurt by client exceptions).
     * Set it to false if you want to use backing library exception handling
     * instead.
     */
    private boolean mustUseExceptionHandlerForClient = false;
    
    /*
     * 
     */
    
    /**
     * Parallelism for parallelizer.
     * 
     * 1 by default, for multi-threading not to hurt incognizant users.  
     */
    private int parallelizerParallelism = 1;
    
    /**
     * Parallelism for internal parallelizer.
     * 
     * This parallelism doesn't cause concurrency for user code,
     * only for trusted bindings code, so it's safe not to use 1 by default.
     */
    private int internalParallelism = Runtime.getRuntime().availableProcessors();

    /**
     * Type of time provided by the clock used for UI thread scheduler.
     * 
     * NANO_TIME_ZERO by default, to:
     * - Make it obvious that it is allowed to differ from system time
     *   (even though you could use system time for it, and rely on that).
     * - Have good scheduling precision/accuracy.
     * - Make it much easier to read, especially at start (small values).
     * - Have similar times across runs.
     */
    private HardClockTimeType uiThreadSchedulerHardClockTimeType = HardClockTimeType.NANO_TIME_ZERO;
    
    /*
     * 
     */
    
    private ScreenBoundsType screenBoundsType = ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE;
    
    /**
     * Supposed to be valid, and used,
     * only if screenBoundsType is CONFIGURED.
     */
    private GRect screenBoundsInOs = null;

    /*
     * 
     */
    
    /**
     * Period, in seconds, at which runWindowEventLogicLoop()
     * methods of hosts, which spy on backing state and
     * pending stuffs to do, are called.
     * This is kind of the ticking period of our state machine,
     * modulo the fact that the method can also be called
     * on backing events.
     */
    private double windowEventLogicPeriodS = 0.01;
    
    /**
     * Minimum delay, in seconds, between two paintings ensured by
     * host's ensurePendingClientPaintXxx(...) methods.
     * Not zero by default to avoid wasting CPU with a number of
     * paintings per second going up the roof.
     * 
     * Note that bindings are still allowed to trigger immediate paints
     * without considering this delay, if it can help avoiding glitches
     * in some special cases.
     * 
     * NB: Flush treatments might be quite slow
     * (found them to be fast on Windows7, but slow on Mac),
     * in which case it helps benches a lot not to flush on every painting.
     */
    private double clientPaintingDelayS = 1.0/60;

    /**
     * Delay, in seconds, since any move or resize event, after which, if no
     * such event is detected again, we consider that the related movement
     * or resizing stopped, and after which a repaint is ensured (and eventually
     * delayed by client painting delay) if either mustTryToPaintDuringMove or
     * mustTryToPaintDuringResize is false.
     * 
     * Here, host bounds refer to either window of client bounds
     * (doesn't matter, as they are supposed to be linked in some way).
     * 
     * Must not be too large else user might wait for too long for repaint
     * to take place after a movement or resize, and must not be too small
     * else movement or resize end might be detected while it still has some
     * noticeable speed.
     */
    private double hostBoundsCheckDelayS = 1.0/20;
    
    /*
     * 
     */
    
    /**
     * True by default, for better user experience, in particular because
     * some libraries don't (re)paint properly on move, especially over area
     * moving back in screen bounds.
     */
    private boolean mustTryToPaintDuringMove = true;

    /**
     * True by default, for better user experience.
     */
    private boolean mustTryToPaintDuringResize = true;
    
    /**
     * Useful with some libraries, which are too asynchronous
     * and/or modify positions and spans separately,
     * to avoid bounds jumping around on drag.
     */
    private boolean mustFixBoundsDuringDrag = false;
    
    /**
     * Useful with some libraries, for which cheeky client area fillings
     * we can't seem to get rid of don't allow to preserve pixels
     * from a painting to the next.
     */
    private boolean mustMakeAllDirtyAtEachPainting = false;

    /*
     * 
     */
    
    /**
     * Can be null.
     * 
     * List of fonts to be loaded as system fonts by the bindings,
     * i.e. even if the user doesn't specify them to fonts loading method.
     * 
     * Especially useful for bindings using libraries that don't
     * load any system font.
     */
    private List<String> bonusSystemFontFilePathList = null;
    
    /**
     * Used during fonts load.
     */
    private InterfaceDefaultFontInfoComputer defaultFontInfoComputer = new DefaultDefaultFontInfoComputer();
    
    /**
     * Computing font kind with Apache FontBox (at least for user fonts,
     * for which we have a font file to parse), allows for consistent
     * family names and styles across bindings, and make generic testing
     * much easier.
     * 
     * If FontBox can't compute font kind, bindings should
     * compute it with backing library, or best effort techniques
     * if it can't, to compute family name and style.
     */
    private boolean mustUseFontBoxForFontKind = false;
    
    /**
     * Libraries frequently don't provide the information whether
     * a given code point can be displayed by a given font,
     * or compute different validity ranges.
     * Using Apache FontBox allows to have this information,
     * and in a way consistent across bindings if using it
     * for all of them.
     * 
     * Note that if using FontBox for code point displayability,
     * you might also want to use it for font kind computation,
     * else, in case of multiple fonts per file, the binding
     * (such as the one for SWT) might not know to which
     * font kind attribute each set of displayable code points,
     * and fall back to default best effort computation.
     * 
     * If FontBox can't compute that, bindings should compute it
     * with backing library, or best effort techniques if it can't.
     */
    private boolean mustUseFontBoxForCanDisplay = false;
    
    /**
     * Min user-specified font size,
     * when not multiplied by font size factor.
     * Must be >= 1.
     */
    private int minRawFontSize = 1;
    
    /**
     * Max user-specified font size,
     * when not multiplied by font size factor.
     * Must be >= minRawFontSize.
     * 
     * Only Short.MAX_VALUE by default,
     * not to go past the limit advised
     * in getMaxFontSize() spec.
     */
    private int maxRawFontSize = Short.MAX_VALUE;
    
    /**
     * Factor user-specified font size is multiplied with
     * (in addition to eventual pixel size / point size conversion),
     * before using backing library's font creation treatments.
     * 
     * Useful when using newFontWithSize(...) methods rather than
     * newFontWithXxxHeight(...) methods, because for a same font,
     * the relationship between font size and font height might depend
     * on the backing library, and also on the OS.
     * It can also make up for eventual pixel size / point size
     * confusions in the binding or backing library or wherever
     * down the stack.
     */
    private double fontSizeFactor = 1.0;
    
    /*
     * 
     */
    
    /**
     * Scale = size of a binding pixel ("BD pixel") in OS pixels.
     * 
     * Must be used at the lowest level, for even text pixels
     * to be grown rather than font size just scaled.
     * 
     * Mathematical integer, and same for X and Y,
     * for fast implementations and simplicity.
     * 
     * Having related helper directly here in binding config,
     * not to have to bother with a scale modification listener
     * to update helper internals.
     * 
     * Not having scale modification methods in public API,
     * for simplicity, and because it would be a kind of
     * earth-shaking event for the whole GUI.
     */
    private ScaleHelper scaleHelper = new ScaleHelper();
    
    /**
     * Use true to indicate to the binding that you prefer
     * accuracy over speed when an image is scaled up or down,
     * false to indicate that you prefer speed over accuracy.
     * 
     * False by default, not to hurt performances (possibly a lot)
     * for users that don't care much about accuracy,
     * or worse don't like the slight blurriness that can come with it.
     * 
     * For now, not allowing to configure that from public API
     * (such as with an argument for scaling drawImage() methods,
     * or a setMustUseAccurateImageScaling(boolean) on graphics),
     * because even if it's set to true, it's still possible
     * to draw small image icons quickly (typical use-case
     * for wanting fast drawing of scaled-down large images),
     * with the workaround of drawing properly scaled images once each
     * into a writable image, and then drawing these writable images
     * every time instead (which would also save memory).
     */
    private boolean mustEnsureAccurateImageScaling = false;
    
    /*
     * 
     */
    
    /**
     * Duration, in seconds, since key pressed and/or typed event,
     * after which key pressed and/or typed events repetition starts.
     */
    private double keyRepetitionTriggerDelayS = 0.3;
    
    /**
     * Period, in seconds, of key pressed and typed events repetition.
     */
    private double keyRepetitionPeriodS = 0.03;
    
    /*
     * Window state : behavior.
     * 
     * Booleans to ensure expected window behavior in case the
     * backing library and/or OS wouldn't ensure it already.
     * 
     * "onShow" means when host.show() is called.
     * "onShown" means when SHOWN event is fired.
     * "onBackingShown" means on backing SHOWN event.
     * 
     * These booleans are true by default, to make it easier to implement hosts
     * that work as expected without having to figure out which must be set
     * to true to ensure proper behavior.
     * That also allows for a more consistent behavior across bindings,
     * and is more robust to behavior changes in libraries and OSes.
     * 
     * Set them to false if true hurts, or if you want your binding
     * to only do what is necessary to obtain the expected behavior.
     */
    
    /**
     * True if must request focus gain asynchronously just after
     * show() has been called, false otherwise.
     * 
     * Helps in case the backing library doesn't do it already.
     */
    private boolean mustRequestFocusGainOnShow = true;

    /**
     * True if must request focus gain asynchronously just after
     * DEICONIFIED event has been fired, false otherwise.
     * 
     * Helps in case the backing library doesn't do it already.
     */
    private boolean mustRequestFocusGainOnDeiconified = true;
    
    /**
     * True if must request focus gain asynchronously just after
     * deiconify() has been called, false otherwise.
     * 
     * Helps in case backing library doesn't do it.
     */
    private boolean mustRequestFocusGainOnDeiconify = true;

    /**
     * True if must request focus gain asynchronously just after
     * maximize() has been called, false otherwise.
     * (Not doing is on event, else, when we would request focus
     * on demaximizations due to iconifications.)
     * 
     * Helps in case the backing library doesn't do it already.
     */
    private boolean mustRequestFocusGainOnMaximize = true;
    
    /**
     * True if must request focus gain asynchronously just after
     * demaximize() has been called, false otherwise.
     * 
     * Helps in case backing library doesn't do it.
     */
    private boolean mustRequestFocusGainOnDemaximize = true;

    /*
     * Additional deiconification configuration.
     */
    
    /**
     * True if must deiconify asynchronously just after
     * show() has been called, false otherwise.
     * 
     * Helps in case the backing library doesn't do it already.
     */
    private boolean mustDeiconifyOnShow = true;
    
    /**
     * True if must attempt to restore into host, shortly after host and client
     * are back into stable showing and deiconified states, the maximized state
     * of the client, which should be the one of the host prior to hiding
     * or iconification.
     * Can help for libraries that lose or corrupt maximized state on hiding
     * or iconification.
     */
    private boolean mustRestoreMaximizedStateWhenBackToShowingAndDeiconified = true;
    
    /*
     * Window state : delays.
     */
    
    /**
     * Delay, in seconds, without window state or bounds events,
     * after which we consider that a window state is stable
     * and can be forwarded to the client.
     * 
     * Using a non-zero value by default, because, with our experience of
     * how messy window events can be depending on libraries and platforms,
     * we want to guard by default, for all bindings, against corresponding
     * possible messy behaviors, whatever the context we detected them in.
     */
    private double backingWindowStateStabilityDelayS = 0.1;
    
    /**
     * Delay, in seconds, after detection of hidden backing window state,
     * during which we consider it unstable and ignore it.
     * 
     * Using a separate delay than general state stability delay,
     * because usually showing/hidden state can be trusted without delay,
     * but with some libraries it cannot, and then the delay to use
     * can be much larger.
     * 
     * Can be useful not to fire HIDDEN on iconification,
     * due to some libraries wrongly indicating window as hidden
     * on simple iconification, or on iconification of parent window.
     */
    private double backingWindowHiddenStabilityDelayS = 0.0;
    
    /**
     * Delay, in seconds, after detection of a backing state difference
     * with client state, during which we ignore opposite backing state
     * to the point of not even considering it "detected" (which means
     * that stability delay will apply afterwards before it gets forwarded
     * to client).
     * 
     * We call these back and forth transitions "state flickering".
     * Examples:
     * - State shortly becoming deiconified just after iconification,
     *   due to some backing drawing event forcing its way to the screen.
     * - State shortly becoming maximized on maximization, then demaximized
     *   during resizes, then maximized once resizes are done.
     * - Focus bouncing back and forth between multiple windows as their state
     *   change.
     * 
     * This delay was added in complement to stability delay,
     * for when it was too small to prevent state flickering,
     * which allows to keep stability delay relatively small
     * while still preventing state flickering.
     * 
     * We only apply this delay to iconified and maximized states,
     * not to focused state which we allow to flicker since we're
     * not really supposed to be in full control of it anyway.
     * 
     * Note that in some contexts, during state flickering, the backing
     * library monopolizes UI thread, such as even if this delay is smaller
     * than flickering duration, it wouldn't cause our treatments to
     * mistakenly consider the state as stable too early.
     */
    private double backingWindowStateAntiFlickeringDelayS = 0.0;
    
    /*
     * Window and client bounds.
     */
    
    /**
     * Bounds used on showing, when no client or window bounds are set
     * before showing.
     * 
     * Can be used as either client or window bounds,
     * depending on mustUseDefaultBoundsForClientElseWindow.
     * 
     * Defining such default bounds for determinism, and not have them
     * depend on the backing library.
     * 
     * Using some top-left margin, for eventual native decoration to be visible.
     * 100 is a decorated window minimal width for many libraries,
     * but for some (such as JavaFX or SDL) it's a bit higher.
     * Using 120 for should keep us away from any issue.
     */
    private GRect defaultClientOrWindowBoundsInOs = GRect.valueOf(50, 50, 120, 100);
    
    /**
     * On host created, whether default bounds, after having eventually be used
     * to initialize either client bounds or window bounds, must be used with
     * setClientBounds(...) or setWindowBounds(...).
     * 
     * Configurable because with some libraries insets are not available
     * while not showing (and/or iconified), and depending on whether
     * backing bounds setting is done in client or window, it could
     * cause some resizing glitch on first show up.
     */
    private boolean mustUseDefaultBoundsForClientElseWindow = true;
    
    /**
     * True if must restore bounds on showing/deiconified/demaximized
     * (other than enforcing of bounds set by user while hidden
     * or iconified or maximized).
     * 
     * Can be useful due to some libraries not updating bounds or position
     * properly on showing/deiconified/demaximized, or damaging them
     * while in another state, possibly as a side effect of other windows
     * modifications.
     */
    private boolean mustRestoreBoundsOnShowDeicoDemax = false;
    
    /**
     * True if must enforce bounds on showing/deiconified/maximized.
     * 
     * Can be useful due to some libraries not updating bounds or position
     * properly on maximization.
     */
    private boolean mustEnforceBoundsOnShowDeicoMax = false;
    
    /**
     * True to set backing bounds in setXxxBounds(...) methods when host
     * is not (showing and deiconified), when client is also demaximized,
     * in hope that it has effect on the backing window and make it appear
     * with proper bounds right away when it gets showing and deiconified,
     * before target bounds to enforce are effectively enforced.
     * 
     * True by default, but can be set to false for libraries that have issues
     * with setting bounds on hidden or iconified windows.
     */
    private boolean mustSetBackingDemaxBoundsWhileHiddenOrIconified = true;

    /*
     * Internals.
     */
    
    /**
     * Required for AbstractBwdGraphics.getArgb32At(x,y) to work properly with
     * some bindings (such as the ones for JavaFX), at the cost of
     * some additional overhead (typically due to using some intermediary
     * offscreen image).
     * 
     * Set it to false for eventual performances upgrade
     * if you don't need to read pixel colors.
     */
    private boolean mustImplementBestEffortPixelReading = true;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Creates a default configuration.
     */
    public BaseBwdBindingConfig() {
    }
    
    /*
     * 
     */
    
    public PrintStream getIssueStream() {
        return this.issueStream;
    }

    public void setIssueStream(PrintStream issueStream) {
        this.setIssueStream_final(issueStream);
    }
    
    public UncaughtExceptionHandler getExceptionHandler() {
        return this.exceptionHandler;
    }

    public void setExceptionHandler(UncaughtExceptionHandler exceptionHandler) {
        this.setExceptionHandler_final(exceptionHandler);
    }
    
    public boolean getMustUseExceptionHandlerForClient() {
        return this.mustUseExceptionHandlerForClient;
    }

    public void setMustUseExceptionHandlerForClient(boolean mustUseExceptionHandlerForClient) {
        this.setMustUseExceptionHandlerForClient_final(mustUseExceptionHandlerForClient);
    }
    
    /*
     * 
     */

    public int getParallelizerParallelism() {
        return this.parallelizerParallelism;
    }

    public void setParallelizerParallelism(int parallelizerParallelism) {
        this.setParallelizerParallelism_final(parallelizerParallelism);
    }

    public int getInternalParallelism() {
        return this.internalParallelism;
    }

    public void setInternalParallelism(int internalParallelism) {
        this.setInternalParallelism_final(internalParallelism);
    }

    public HardClockTimeType getUiThreadSchedulerHardClockTimeType() {
        return this.uiThreadSchedulerHardClockTimeType;
    }

    public void setUiThreadSchedulerHardClockTimeType(HardClockTimeType uiThreadSchedulerHardClockTimeType) {
        this.setUiThreadSchedulerHardClockTimeType_final(uiThreadSchedulerHardClockTimeType);
    }

    /*
     * 
     */

    public ScreenBoundsType getScreenBoundsType() {
        return this.screenBoundsType;
    }

    public void setScreenBoundsType(ScreenBoundsType screenBoundsType) {
        this.setScreenBoundsType_final(screenBoundsType);
    }

    public GRect getScreenBoundsInOs() {
        return this.screenBoundsInOs;
    }

    public void setScreenBoundsInOs(GRect screenBoundsInOs) {
        this.setScreenBoundsInOs_final(screenBoundsInOs);
    }
    
    /*
     * 
     */

    public double getWindowEventLogicPeriodS() {
        return this.windowEventLogicPeriodS;
    }

    public void setWindowEventLogicPeriodS(double windowEventLogicPeriodS) {
        this.setWindowEventLogicPeriodS_final(windowEventLogicPeriodS);
    }
    
    public double getClientPaintingDelayS() {
        return this.clientPaintingDelayS;
    }

    public void setClientPaintingDelayS(double clientPaintingDelayS) {
        this.setClientPaintingDelayS_final(clientPaintingDelayS);
    }

    public double getHostBoundsCheckDelayS() {
        return this.hostBoundsCheckDelayS;
    }

    public void setHostBoundsCheckDelayS(double hostBoundsCheckDelayS) {
        this.setHostBoundsCheckDelayS_final(hostBoundsCheckDelayS);
    }

    public boolean getMustTryToPaintDuringMove() {
        return this.mustTryToPaintDuringMove;
    }
    
    public void setMustTryToPaintDuringMove(boolean mustTryToPaintDuringMove) {
        this.setMustTryToPaintDuringMove_final(mustTryToPaintDuringMove);
    }

    public boolean getMustTryToPaintDuringResize() {
        return this.mustTryToPaintDuringResize;
    }

    public void setMustTryToPaintDuringResize(boolean mustTryToPaintDuringResize) {
        this.setMustTryToPaintDuringResize_final(mustTryToPaintDuringResize);
    }
    
    public boolean getMustFixBoundsDuringDrag() {
        return this.mustFixBoundsDuringDrag;
    }

    public void setMustFixBoundsDuringDrag(boolean mustFixBoundsDuringDrag) {
        this.setMustFixBoundsDuringDrag_final(mustFixBoundsDuringDrag);
    }

    public boolean getMustMakeAllDirtyAtEachPainting() {
        return this.mustMakeAllDirtyAtEachPainting;
    }

    public void setMustMakeAllDirtyAtEachPainting(boolean mustMakeAllDirtyAtEachPainting) {
        this.setMustMakeAllDirtyAtEachPainting_final(mustMakeAllDirtyAtEachPainting);
    }

    /*
     * 
     */
    
    public List<String> getBonusSystemFontFilePathList() {
        return this.bonusSystemFontFilePathList;
    }

    public void setBonusSystemFontFilePathList(List<String> bonusSystemFontFilePathList) {
        this.setBonusSystemFontFilePathList_final(bonusSystemFontFilePathList);
    }
    
    public InterfaceDefaultFontInfoComputer getDefaultFontInfoComputer() {
        return this.defaultFontInfoComputer;
    }

    public void setDefaultFontInfoComputer(InterfaceDefaultFontInfoComputer defaultFontInfoComputer) {
        this.setDefaultFontInfoComputer_final(defaultFontInfoComputer);
    }

    public boolean getMustUseFontBoxForFontKind() {
        return this.mustUseFontBoxForFontKind;
    }

    public void setMustUseFontBoxForFontKind(boolean mustUseFontBoxForFontKind) {
        this.setMustUseFontBoxForFontKind_final(mustUseFontBoxForFontKind);
    }
    
    public boolean getMustUseFontBoxForCanDisplay() {
        return this.mustUseFontBoxForCanDisplay;
    }

    public void setMustUseFontBoxForCanDisplay(boolean mustUseFontBoxForCanDisplay) {
        this.setMustUseFontBoxForCanDisplay_final(mustUseFontBoxForCanDisplay);
    }

    public int getMinRawFontSize() {
        return this.minRawFontSize;
    }

    public void setMinRawFontSize(int minRawFontSize) {
        this.setMinRawFontSize_final(minRawFontSize);
    }

    public int getMaxRawFontSize() {
        return this.maxRawFontSize;
    }

    public void setMaxRawFontSize(int maxRawFontSize) {
        this.setMaxRawFontSize_final(maxRawFontSize);
    }

    public double getFontSizeFactor() {
        return this.fontSizeFactor;
    }

    public void setFontSizeFactor(double fontSizeFactor) {
        this.setFontSizeFactor_final(fontSizeFactor);
    }

    /*
     * 
     */
    
    /**
     * @return Always the same instance (so can be retrieved early),
     *         but its scale can be modified at runtime (in UI thread).
     */
    public ScaleHelper getScaleHelper() {
        return this.scaleHelper;
    }
    
    /**
     * @return Size of a binding pixel in OS pixels.
     */
    public int getScale() {
        return this.scaleHelper.getScale();
    }
    
    /**
     * @param scale Size of a binding pixel in OS pixels.
     *        Must be >= 1.
     */
    public void setScale(int scale) {
        this.setScale_final(scale);
    }
    
    public boolean getMustEnsureAccurateImageScaling() {
        return this.mustEnsureAccurateImageScaling;
    }

    public void setMustEnsureAccurateImageScaling(boolean mustEnsureAccurateImageScaling) {
        this.setMustEnsureAccurateImageScaling_final(mustEnsureAccurateImageScaling);
    }

    /*
     * 
     */

    public double getKeyRepetitionTriggerDelayS() {
        return this.keyRepetitionTriggerDelayS;
    }

    public void setKeyRepetitionTriggerDelayS(double keyRepetitionTriggerDelayS) {
        this.setKeyRepetitionTriggerDelayS_final(keyRepetitionTriggerDelayS);
    }

    public double getKeyRepetitionPeriodS() {
        return this.keyRepetitionPeriodS;
    }

    public void setKeyRepetitionPeriodS(double keyRepetitionPeriodS) {
        this.setKeyRepetitionPeriodS_final(keyRepetitionPeriodS);
    }

    /*
     * 
     */

    public boolean getMustRequestFocusGainOnShow() {
        return this.mustRequestFocusGainOnShow;
    }

    public void setMustRequestFocusGainOnShow(boolean mustRequestFocusGainOnShow) {
        this.setMustRequestFocusGainOnShow_final(mustRequestFocusGainOnShow);
    }

    public boolean getMustRequestFocusGainOnDeiconified() {
        return this.mustRequestFocusGainOnDeiconified;
    }

    public void setMustRequestFocusGainOnDeiconified(boolean mustRequestFocusGainOnDeiconified) {
        this.setMustRequestFocusGainOnDeiconified_final(mustRequestFocusGainOnDeiconified);
    }

    public boolean getMustRequestFocusGainOnDeiconify() {
        return this.mustRequestFocusGainOnDeiconify;
    }

    public void setMustRequestFocusGainOnDeiconify(boolean mustRequestFocusGainOnDeiconify) {
        this.setMustRequestFocusGainOnDeiconify_final(mustRequestFocusGainOnDeiconify);
    }

    public boolean getMustRequestFocusGainOnMaximize() {
        return this.mustRequestFocusGainOnMaximize;
    }

    public void setMustRequestFocusGainOnMaximize(boolean mustRequestFocusGainOnMaximize) {
        this.setMustRequestFocusGainOnMaximize_final(mustRequestFocusGainOnMaximize);
    }

    public boolean getMustRequestFocusGainOnDemaximize() {
        return this.mustRequestFocusGainOnDemaximize;
    }

    public void setMustRequestFocusGainOnDemaximize(boolean mustRequestFocusGainOnDemaximize) {
        this.setMustRequestFocusGainOnDemaximize_final(mustRequestFocusGainOnDemaximize);
    }

    public boolean getMustDeiconifyOnShow() {
        return this.mustDeiconifyOnShow;
    }

    public void setMustDeiconifyOnShow(boolean mustDeiconifyOnShow) {
        this.setMustDeiconifyOnShow_final(mustDeiconifyOnShow);
    }

    public boolean getMustRestoreMaximizedStateWhenBackToShowingAndDeiconified() {
        return this.mustRestoreMaximizedStateWhenBackToShowingAndDeiconified;
    }

    public void setMustRestoreMaximizedStateWhenBackToShowingAndDeiconified(
            boolean mustRestoreMaximizedStateWhenBackToShowingAndDeiconified) {
        this.setMustRestoreMaximizedStateWhenBackToShowingAndDeiconified_final(mustRestoreMaximizedStateWhenBackToShowingAndDeiconified);
    }

    public double getBackingWindowStateStabilityDelayS() {
        return this.backingWindowStateStabilityDelayS;
    }

    public void setBackingWindowStateStabilityDelayS(double backingWindowStateStabilityDelayS) {
        this.setBackingWindowStateStabilityDelayS_final(backingWindowStateStabilityDelayS);
    }
    
    public double getBackingWindowHiddenStabilityDelayS() {
        return this.backingWindowHiddenStabilityDelayS;
    }

    public void setBackingWindowHiddenStabilityDelayS(double backingWindowHiddenStabilityDelayS) {
        this.setBackingWindowHiddenStabilityDelayS_final(backingWindowHiddenStabilityDelayS);
    }
    
    public double getBackingWindowStateAntiFlickeringDelayS() {
        return this.backingWindowStateAntiFlickeringDelayS;
    }

    public void setBackingWindowStateAntiFlickeringDelayS(double backingWindowStateAntiFlickeringDelayS) {
        this.setBackingWindowStateAntiFlickeringDelayS_final(backingWindowStateAntiFlickeringDelayS);
    }
    
    /*
     * 
     */
    
    public GRect getDefaultClientOrWindowBoundsInOs() {
        return this.defaultClientOrWindowBoundsInOs;
    }

    public void setDefaultClientOrWindowBoundsInOs(GRect defaultClientOrWindowBoundsInOs) {
        this.setDefaultClientOrWindowBoundsInOs_final(defaultClientOrWindowBoundsInOs);
    }
    
    public boolean getMustUseDefaultBoundsForClientElseWindow() {
        return this.mustUseDefaultBoundsForClientElseWindow;
    }

    public void setMustUseDefaultBoundsForClientElseWindow(boolean mustUseDefaultBoundsForClientElseWindow) {
        this.setMustUseDefaultBoundsForClientElseWindow_final(mustUseDefaultBoundsForClientElseWindow);
    }
    
    public boolean getMustRestoreBoundsOnShowDeicoDemax() {
        return this.mustRestoreBoundsOnShowDeicoDemax;
    }

    public void setMustRestoreBoundsOnShowDeicoDemax(boolean mustRestoreBoundsOnShowDeicoDemax) {
        this.setMustRestoreBoundsOnShowDeicoDemax_final(mustRestoreBoundsOnShowDeicoDemax);
    }
    
    public boolean getMustEnforceBoundsOnShowDeicoMax() {
        return this.mustEnforceBoundsOnShowDeicoMax;
    }

    public void setMustEnforceBoundsOnShowDeicoMax(boolean mustEnforceBoundsOnShowDeicoMax) {
        this.setMustEnforceBoundsOnShowDeicoMax_final(mustEnforceBoundsOnShowDeicoMax);
    }
    
    public boolean getMustSetBackingDemaxBoundsWhileHiddenOrIconified() {
        return this.mustSetBackingDemaxBoundsWhileHiddenOrIconified;
    }

    public void setMustSetBackingDemaxBoundsWhileHiddenOrIconified(boolean mustSetBackingDemaxBoundsWhileHiddenOrIconified) {
        this.setMustSetBackingDemaxBoundsWhileHiddenOrIconified_final(mustSetBackingDemaxBoundsWhileHiddenOrIconified);
    }

    /*
     * 
     */

    public boolean getMustImplementBestEffortPixelReading() {
        return this.mustImplementBestEffortPixelReading;
    }

    public void setMustImplementBestEffortPixelReading(boolean mustImplementBestEffortPixelReading) {
        this.setMustImplementBestEffortPixelReading_final(mustImplementBestEffortPixelReading);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    /*
     * Final setters, for usage in constructors,
     * if default value is not suited for the binding.
     */
    
    /**
     * Default value is System.out.
     */
    protected final void setIssueStream_final(PrintStream issueStream) {
        this.issueStream = issueStream;
    }

    /**
     * Default handler logs as JDK default, and keeps going.
     */
    protected void setExceptionHandler_final(UncaughtExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }
    
    /**
     * Default value is true.
     */
    protected void setMustUseExceptionHandlerForClient_final(boolean mustUseExceptionHandlerForClient) {
        this.mustUseExceptionHandlerForClient = mustUseExceptionHandlerForClient;
    }
    
    /*
     * 
     */

    /**
     * Default value is 1.
     */
    protected final void setParallelizerParallelism_final(int parallelizerParallelism) {
        this.parallelizerParallelism = parallelizerParallelism;
    }

    /**
     * Default value is Runtime.getRuntime().availableProcessors().
     */
    protected final void setInternalParallelism_final(int internalParallelism) {
        this.internalParallelism = internalParallelism;
    }
    
    /**
     * Default value is SYSTEM_NANO_TIME.
     */
    protected final void setUiThreadSchedulerHardClockTimeType_final(HardClockTimeType uiThreadSchedulerHardClockTimeType) {
        this.uiThreadSchedulerHardClockTimeType = uiThreadSchedulerHardClockTimeType;
    }
    
    /*
     * 
     */

    /**
     * Default value is PRIMARY_SCREEN_AVAILABLE.
     */
    protected final void setScreenBoundsType_final(ScreenBoundsType screenBoundsType) {
        this.screenBoundsType = screenBoundsType;
    }
    
    /**
     * Default value is null.
     */
    protected final void setScreenBoundsInOs_final(GRect screenBoundsInOs) {
        this.screenBoundsInOs = screenBoundsInOs;
    }
    
    /*
     * 
     */

    /**
     * Default value is 0.01.
     */
    protected final void setWindowEventLogicPeriodS_final(double windowEventLogicPeriodS) {
        this.windowEventLogicPeriodS = windowEventLogicPeriodS;
    }
    
    /**
     * Default value is 1.0/60.
     */
    protected final void setClientPaintingDelayS_final(double clientPaintingDelayS) {
        this.clientPaintingDelayS = clientPaintingDelayS;
    }
    
    /**
     * Default value is 1.0/20.
     */
    protected final void setHostBoundsCheckDelayS_final(double hostBoundsCheckDelayS) {
        this.hostBoundsCheckDelayS = hostBoundsCheckDelayS;
    }

    /**
     * Default value is true.
     */
    protected final void setMustTryToPaintDuringMove_final(boolean mustTryToPaintDuringMove) {
        this.mustTryToPaintDuringMove = mustTryToPaintDuringMove;
    }

    /**
     * Default value is true.
     */
    protected final void setMustTryToPaintDuringResize_final(boolean mustTryToPaintDuringResize) {
        this.mustTryToPaintDuringResize = mustTryToPaintDuringResize;
    }

    /**
     * Default value is false.
     */
    protected final void setMustFixBoundsDuringDrag_final(boolean mustFixBoundsDuringDrag) {
        this.mustFixBoundsDuringDrag = mustFixBoundsDuringDrag;
    }

    /**
     * Default value is false.
     */
    protected final void setMustMakeAllDirtyAtEachPainting_final(boolean mustMakeAllDirtyAtEachPainting) {
        this.mustMakeAllDirtyAtEachPainting = mustMakeAllDirtyAtEachPainting;
    }

    /*
     * 
     */
    
    /**
     * Default value is null.
     */
    protected final void setBonusSystemFontFilePathList_final(List<String> bonusSystemFontFilePathList) {
        this.bonusSystemFontFilePathList = bonusSystemFontFilePathList;
    }

    /**
     * Default value is a DefaultDefaultFontInfoComputer instance.
     */
    protected final void setDefaultFontInfoComputer_final(InterfaceDefaultFontInfoComputer defaultFontInfoComputer) {
        this.defaultFontInfoComputer = defaultFontInfoComputer;
    }

    /**
     * Default value is false.
     */
    protected final void setMustUseFontBoxForFontKind_final(boolean mustUseFontBoxForFontKind) {
        this.mustUseFontBoxForFontKind = mustUseFontBoxForFontKind;
    }
    
    /**
     * Default value is false.
     */
    protected final void setMustUseFontBoxForCanDisplay_final(boolean mustUseFontBoxForCanDisplay) {
        this.mustUseFontBoxForCanDisplay = mustUseFontBoxForCanDisplay;
    }

    /**
     * Default value is 1.
     */
    protected final void setMinRawFontSize_final(int minRawFontSize) {
        this.minRawFontSize = minRawFontSize;
    }
    
    /**
     * Default value is Short.MAX_VALUE.
     */
    protected final void setMaxRawFontSize_final(int maxRawFontSize) {
        this.maxRawFontSize = maxRawFontSize;
    }

    /**
     * Default value is 1.0.
     */
    protected final void setFontSizeFactor_final(double fontSizeFactor) {
        this.fontSizeFactor = fontSizeFactor;
    }
    
    /*
     * 
     */
    
    /**
     * Default value is 1.
     */
    protected final void setScale_final(int scale) {
        this.scaleHelper.setScale(scale);
    }
    
    /**
     * Default value is false.
     */
    protected final void setMustEnsureAccurateImageScaling_final(boolean mustEnsureAccurateImageScaling) {
        this.mustEnsureAccurateImageScaling = mustEnsureAccurateImageScaling;
    }

    /*
     * 
     */
    
    /**
     * Default value is 0.5.
     */
    protected final void setKeyRepetitionTriggerDelayS_final(double keyRepetitionTriggerDelayS) {
        this.keyRepetitionTriggerDelayS = keyRepetitionTriggerDelayS;
    }

    /**
     * Default value is 0.1.
     */
    protected final void setKeyRepetitionPeriodS_final(double keyRepetitionPeriodS) {
        this.keyRepetitionPeriodS = keyRepetitionPeriodS;
    }
    
    /*
     * 
     */
    
    /**
     * Default value is true.
     */
    protected final void setMustRequestFocusGainOnShow_final(boolean mustRequestFocusGainOnShow) {
        this.mustRequestFocusGainOnShow = mustRequestFocusGainOnShow;
    }

    /**
     * Default value is true.
     */
    protected final void setMustRequestFocusGainOnDeiconified_final(boolean mustRequestFocusGainOnDeiconified) {
        this.mustRequestFocusGainOnDeiconified = mustRequestFocusGainOnDeiconified;
    }

    /**
     * Default value is true.
     */
    protected final void setMustRequestFocusGainOnDeiconify_final(boolean mustRequestFocusGainOnDeiconify) {
        this.mustRequestFocusGainOnDeiconify = mustRequestFocusGainOnDeiconify;
    }

    /**
     * Default value is true.
     */
    protected final void setMustRequestFocusGainOnMaximize_final(boolean mustRequestFocusGainOnMaximize) {
        this.mustRequestFocusGainOnMaximize = mustRequestFocusGainOnMaximize;
    }

    /**
     * Default value is true.
     */
    protected final void setMustRequestFocusGainOnDemaximize_final(boolean mustRequestFocusGainOnDemaximize) {
        this.mustRequestFocusGainOnDemaximize = mustRequestFocusGainOnDemaximize;
    }

    /**
     * Default value is true.
     */
    protected final void setMustDeiconifyOnShow_final(boolean mustDeiconifyOnShow) {
        this.mustDeiconifyOnShow = mustDeiconifyOnShow;
    }

    /**
     * Default value is true.
     */
    protected final void setMustRestoreMaximizedStateWhenBackToShowingAndDeiconified_final(
            boolean mustRestoreMaximizedStateWhenBackToShowingAndDeiconified) {
        this.mustRestoreMaximizedStateWhenBackToShowingAndDeiconified = mustRestoreMaximizedStateWhenBackToShowingAndDeiconified;
    }

    /**
     * Default value is 0.1.
     */
    protected final void setBackingWindowStateStabilityDelayS_final(double backingWindowStateStabilityDelayS) {
        this.backingWindowStateStabilityDelayS = backingWindowStateStabilityDelayS;
    }
    
    /**
     * Default value is 0.0.
     */
    protected final void setBackingWindowHiddenStabilityDelayS_final(double backingWindowHiddenStabilityDelayS) {
        this.backingWindowHiddenStabilityDelayS = backingWindowHiddenStabilityDelayS;
    }
    
    /**
     * Default value is 0.0.
     */
    protected final void setBackingWindowStateAntiFlickeringDelayS_final(double backingWindowStateAntiFlickeringDelayS) {
        this.backingWindowStateAntiFlickeringDelayS = backingWindowStateAntiFlickeringDelayS;
    }
    
    /*
     * 
     */
    
    /**
     * Default value is [50,50,120,100].
     */
    protected final void setDefaultClientOrWindowBoundsInOs_final(GRect defaultClientOrWindowBoundsInOs) {
        this.defaultClientOrWindowBoundsInOs = defaultClientOrWindowBoundsInOs;
    }

    /**
     * Default value is true.
     */
    protected final void setMustUseDefaultBoundsForClientElseWindow_final(boolean mustUseDefaultBoundsForClientElseWindow) {
        this.mustUseDefaultBoundsForClientElseWindow = mustUseDefaultBoundsForClientElseWindow;
    }
    
    /**
     * Default value is false.
     */
    protected final void setMustRestoreBoundsOnShowDeicoDemax_final(boolean mustRestoreBoundsOnShowDeicoDemax) {
        this.mustRestoreBoundsOnShowDeicoDemax = mustRestoreBoundsOnShowDeicoDemax;
    }

    /**
     * Default value is false.
     */
    protected final void setMustEnforceBoundsOnShowDeicoMax_final(boolean mustEnforceBoundsOnShowDeicoMax) {
        this.mustEnforceBoundsOnShowDeicoMax = mustEnforceBoundsOnShowDeicoMax;
    }

    /**
     * Default value is true.
     */
    protected final void setMustSetBackingDemaxBoundsWhileHiddenOrIconified_final(boolean mustSetBackingDemaxBoundsWhileHiddenOrIconified) {
        this.mustSetBackingDemaxBoundsWhileHiddenOrIconified = mustSetBackingDemaxBoundsWhileHiddenOrIconified;
    }
    
    /*
     * 
     */

    /**
     * Default value is true.
     */
    protected final void setMustImplementBestEffortPixelReading_final(boolean mustImplementBestEffortPixelReading) {
        this.mustImplementBestEffortPixelReading = mustImplementBestEffortPixelReading;
    }
}
