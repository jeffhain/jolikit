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
package net.jolikit.bwd.api;

import java.util.List;

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;

/**
 * Interface for BWD hosts.
 * 
 * A host corresponds to a window (eventually decorated) and a client area,
 * is given a client (InterfaceBwdClient) at creation, and must ensure
 * proper method calls on its client (events forwarding, etc.).
 * 
 * Painting:
 * Client painting must be ensured after window state or bounds change,
 * in case client area content depends on it.
 * Indeed, since window state and bounds normally don't change frantically,
 * it should not add much overheads, and makes things much simpler for the
 * client if what it must paint depends on window state and bounds.
 * 
 * Terminology:
 * We prefer to say "iconified" than "minimized", for "minimization" could
 * easily (and wrongly) be understood as being the opposite of "maximization",
 * in particular when having methods such as "minimize()" and "maximize()".
 * 
 * API style:
 * We prefer to have methods like show()/hide() and iconify()/deiconify(),
 * rather than setVisible(boolean) or setIconified(boolean), for their semantics
 * might be more complicated than just setting a boolean state value, as such
 * names seem to imply.
 * For example, with some (library,platform) combinations, showing also causes
 * deiconification and a focus request, which are much easier to implement
 * when it's not the case than to block when it's the case, so our show() method
 * is supposed to also deiconify and request focus, which can make it have effect
 * even if isShowing() was true prior to the call.
 * 
 * Synchronism/asynchronism of programmatic state changes
 * (by "state change", we mean change of the value retrieved
 * from corresponding isXxx() or getXxx() methods, not firing of
 * corresponding events: events are always allowed to be fired
 * synchronously):
 * iconify()/deiconify()/maximize()/demaximize() and requestFocusGain()
 * typically cause asynchronous state changes, but it should not hurt
 * for them to cause synchronous state changes.
 * show()/hide()/close() and bounds setting methods must cause synchronous
 * state changes (in case of distributed implementation, the methods should
 * either block until a report is received (or a timeout reached), or
 * assume some state and then eventually correct it on report reception).
 * 
 * Overflowing rectangles:
 * As specified for graphics API, rectangles that overflow out of int range
 * (due to position + span being too large) must be trimmed before use if any.
 * 
 * Blocked behavior and default values:
 * Depending on state, we require some methods to have no effect (such as
 * iconify() when not showing) or to return a default value (such as
 * isMaximized() when not showing or iconified), because in these cases,
 * for some (library,platform) combinations, backing library methods might
 * not work properly, which is much easier to make up for by avoiding to
 * call them than to try to get the binding to behave as if they would work.
 * 
 * Expected behavior on state change requests (show(), etc.):
 * These behaviors are always about eventually also ensuring another state,
 * or state change request (such as focus request), and not about not
 * having such side effects. Indeed, trying to ensure something is much easier
 * than trying to prevent some side effect, which might even not be possible
 * without huge glitches.
 * 
 * Bounds set/get:
 * When window is not visible, some libraries have trouble dealing with bounds.
 * To allow for consistent behavior whatever the backing library,
 * when the window is not showing or is iconified, returned bounds must be
 * GRect.DEFAULT_EMPTY.
 * Also, when the window is not showing or is iconified or is maximized,
 * bounds setting methods must just take note of the target bounds,
 * and set them when the window gets back to showing and iconified
 * and demaximized.
 * 
 * Native decoration:
 * Note that with some (library,platform) combinations, undecorated windows
 * might still have a (tiny) border (such as with SWT on Windows).
 */
public interface InterfaceBwdHost {
    
    /*
     * Fixed state.
     */
    
    /**
     * @return Whether this host is a dialog.
     */
    public boolean isDialog();
    
    /**
     * @return The owning host if this host is a dialog, else null.
     */
    public InterfaceBwdHost getOwner();

    /**
     * @return Host title. Must not be null.
     */
    public String getTitle();
    
    /**
     * @return Whether this host is decorated (regardless of eventual
     *         decoration in client area).
     */
    public boolean isDecorated();
    
    /**
     * @return Whether this host is modal.
     */
    public boolean isModal();
    
    /**
     * @return The client of this host. Must not be null.
     */
    public InterfaceBwdClient getClient();
    
    /*
     * 
     */
    
    /**
     * @return The cursor manager of this host.
     */
    public InterfaceBwdCursorManager getCursorManager();
    
    /**
     * Convenience method, not to have to use a reference to the binding
     * just to know that.
     * 
     * @return Coordinates of the mouse pointer in screen coordinates.
     */
    public GPoint getMousePosInScreen();

    /*
     * 
     */

    /**
     * Creates a dialog host with this host as owner (parent).
     * Dialogs hosts are closed when their owner is closed
     * (some libraries don't do it, but some do, so our bindings
     * must do it as well).
     * 
     * Where the host appears when shown is undefined,
     * so you should set window or client bounds before showing it
     * (or after if you prefer, but it increases the chance of flickering).
     * 
     * NB: With some backing libraries, show() for dialogs blocks until the dialog
     * is closed, but UI events are still processed.
     * 
     * Title must not be null as it can be used even if not decorated,
     * such as in task bar.
     * 
     * Decoration flag is here to be able to use native decoration,
     * but it could also be implemented in the binding.
     * 
     * Modality must be as limited as possible, i.e. modality to this host
     * if possible, else to the application, else to the UI library, else to
     * the whole system.
     * This loose choice allows for both simplicity and freedom, and reduces
     * the probability of user being made grumpy by abusively-modal windows.
     * 
     * @param title Must not be null.
     * @param decorated Whether the created host should be decorated.
     * @param modal Whether the returned dialog should be modal or modeless.
     * @return A new dialog host with this host as owner.
     * @throws IllegalStateException if the binding is shut down
     *         or if this host is closed.
     */
    public InterfaceBwdHost newDialog(
            String title,
            boolean decorated,
            boolean modal,
            //
            InterfaceBwdClient client);
    
    /**
     * @return A new mutable list containing dialogs created from this host
     *         and not yet closed.
     */
    public List<InterfaceBwdHost> getDialogList();

    /*
     * Dynamic state.
     */
    
    /**
     * Ideally, we would like the possibility to have per-pixel alpha
     * in the final result, such as non-paint pixels could show up
     * what's behind the window as if it did not exist, and paint pixels
     * would blend with what's behind (with SRC_OVER rule, our default)
     * depending on their respective alpha, possibly being fully opaque.
     * Though, I couldn't figure out a way to obtain this behavior with
     * any backing library (no "compositing window manager"?).
     * On the other hand, some libraries allow to define some window-wide
     * alpha, that is forced upon each pixel (even if some pretend that
     * it's just used as a multiplying factor to obtain final pixel alpha)
     * after eventual blendings on some background done at paint time.
     * As a result, we define this method as the way to set such a window-wide
     * alpha, if supported. If not supported, it must just have no effect
     * other than the argument check.
     * If some backing library supports our ideality of having per-pixel alpha
     * in the final result, it could be supported by adding a
     * clearRectTransparent(...) method in BWD graphics.
     * 
     * Also, depending on the backing library, the specified alpha might be used
     * for each final pixel after eventual blending that would properly occur
     * during painting, or as the alpha of the background on which painting
     * would then be blended, or as a multiplicating factor to compute final
     * pixels alphas.
     * 
     * Might have no effect if used after show(), or if transparency is
     * not supported for this host (possibly due to window being decorated).
     * 
     * @param windowAlphaFp An opacity in [0,1].
     * @throws IllegalArgumentException if alpha is not in [0,1] or is NaN.
     */
    public void setWindowAlphaFp(double windowAlphaFp);
    
    /*
     * Showing.
     */

    /**
     * @return Whether this host is showing.
     */
    public boolean isShowing();
    
    /**
     * Must do nothing if close() has been called.
     * 
     * Shows this host.
     * 
     * Some libraries show() methods cause deiconification, and/or a focus gain
     * request, possibly according to quite complicated and surprising logic.
     * For example, for AWT/Swing, focus is not gained on show if window was
     * both iconified and maximized, and in case of two levels of dialogs with
     * last one having focus, hiding and showing root host causes much focus
     * events ending up with first level dialog focused.
     * To help this method's behavior to be more consistent across bindings,
     * it should attempt to both deiconify and focus the window,
     * even if already showing.
     * 
     * For modal hosts, it typically blocks until the host is closed.
     */
    public void show();

    /**
     * Must do nothing if close() has been called.
     * 
     * Hides this host.
     * 
     * All hosts being hidden must not cause the application to exit
     * (as is the case by default with JavaFX) or cause UI thread
     * to take a nap (as is the case with LWJGL3).
     * 
     * If the backing library has no way to hide the backing window,
     * then this method should do nothing rather than hack around,
     * such as moving the backing window out of screen,
     * or deleting it and re-creating it on call to show().
     */
    public void hide();

    /*
     * Focused.
     */
    
    /**
     * Must return false if not showing or if iconified.
     * 
     * Host focus means both backing window and its client area focus,
     * which can be considered identical since the client area is
     * the only component in the backing window.
     * 
     * @return True if this host is focused, false otherwise.
     */
    public boolean isFocused();

    /**
     * Must do nothing if not showing or if iconified.
     * 
     * Requests both that the window gets raised and that it
     * (along with the client area, if separate) receives focus
     * (to keep things simple, we don't have separate notions of a window being
     * active, raised, or focused, for us a focused window should be both raised
     * and active).
     * 
     * Focus must only be assumed on FOCUS_GAINED event, which can be due
     * to other causes than a programmatic call to such a method.
     * 
     * If the backing library has no way to request focus,
     * then this method should do nothing rather than hack around,
     * such as deleting the backing window and re-creating it immediately.
     */
    public void requestFocusGain();

    /*
     * Iconified.
     */
    
    /**
     * Must return false if not showing, which is a case in which
     * some libraries don't allow to deal with this state.
     * 
     * @return Whether this host is iconified.
     */
    public boolean isIconified();

    /**
     * Must do nothing if not showing.
     * 
     * With some platforms (such as on Mac, for a lot of libraries),
     * when the window is maximized, on iconification,
     * readable maximized state might switch to demaximized right away,
     * while the backing state still appears deiconified for some time.
     * This is undistinguishable from a demaximization preceeding
     * the iconification.
     * Bindings should do their best to make up for that, which typically
     * involves some state spying and timeouts.
     */
    public void iconify();
    
    /**
     * Must do nothing if not showing.
     * 
     * Some libraries deiconify() methods cause focus gain request.
     * To help this method's behavior to be more consistent across bindings,
     * it should attempt to focus the window.
     */
    public void deiconify();

    /*
     * Maximized.
     */
    
    /**
     * Must return false if not showing or if iconified, which are cases
     * in which some libraries don't allow to deal with this state.
     * 
     * @return Whether this host is maximized.
     */
    public boolean isMaximized();

    /**
     * Must do nothing if not showing or if iconified.
     * 
     * Some libraries maximize() methods cause focus gain request.
     * To help this method's behavior to be more consistent across bindings,
     * it should attempt to focus the window.
     */
    public void maximize();
    
    /**
     * Must do nothing if not showing or if iconified.
     * 
     * Some libraries demaximize() methods cause focus gain request.
     * To help this method's behavior to be more consistent across bindings,
     * it should attempt to focus the window.
     */
    public void demaximize();
    
    /*
     * Closed.
     */

    /**
     * Must be callable from any thread.
     * This allows for host aliveness checks even after
     * UI thread scheduler has been shut down.
     * 
     * @return Whether close() has been called, in which case this host
     *         must be considered closed even if close() did not complete yet,
     *         for corresponding resources managed by the backing library
     *         might have been released already, in particular when the closing
     *         is initiated by the backing library on backing window closing event.
     */
    public boolean isClosed();
    
    /**
     * Closes this host, which must irreversibly unfocus and hide it,
     * and must dispose associated resources (such as the backing window).
     * 
     * Must be idempotent.
     * 
     * All hosts being closed must not cause the application to exit
     * (as is the case by default with JavaFX) or cause UI thread
     * to take a nap (as is the case with LWJGL3).
     */
    public void close();
    
    /*
     * Bounds.
     * 
     * Putting client area related methods before window related methods,
     * because they are more essential from the client area perspective,
     * since the effects of window related methods on client area depend
     * on the insets, which change depending on whether the window is decorated,
     * and may vary depending on the backing library or platform.
     * 
     * It's also always possible to compute window bounds from client bounds,
     * by eventually adding insets (unless overflow), whereas computing
     * client bounds from window bounds might not work if window bounds
     * are too small or insets too large.
     */
    
    /**
     * If not showing or if iconified, must return GRect.DEFAULT_EMPTY.
     * 
     * @return {x=left, y=top, xSpan=right, ySpan=bottom} border span
     *         around client area.
     */
    public GRect getInsets();
    
    /**
     * If not showing or if iconified, must return GRect.DEFAULT_EMPTY.
     * 
     * @return The bounds of the client area (where BWD is paint), in screen.
     */
    public GRect getClientBounds();
    
    /**
     * If not showing or if iconified, must return GRect.DEFAULT_EMPTY.
     * 
     * @return The bounds of the window, in screen.
     */
    public GRect getWindowBounds();
    
    /**
     * If not showing, if iconified, or if maximized,
     * must store the specified bounds for use when back to
     * showing and deiconified and demaximized.
     * 
     * The target bounds might not be honored, and in a way that is undefined.
     * For example, (x,y) might be honored, but not (xSpan,ySpan),
     * or the opposite.
     * 
     * @see #setClientBoundsSmart(GRect)
     * 
     * @param targetClientBounds Target client bounds.
     * @throws NullPointerException if the specified bounds is null.
     */
    public void setClientBounds(GRect targetClientBounds);

    /**
     * If not showing, if iconified, or if maximized,
     * must store the specified bounds for use when back to
     * showing and deiconified and demaximized.
     * 
     * The target bounds might not be honored, and in a way that is undefined.
     * For example, (x,y) might be honored, but not (xSpan,ySpan),
     * or the opposite.
     * 
     * @see #setWindowBoundsSmart(GRect)
     * 
     * @param targetWindowBounds Target window bounds.
     * @throws NullPointerException if the specified bounds is null.
     */
    public void setWindowBounds(GRect targetWindowBounds);

    /**
     * Convenience method.
     * Equivalent to setClientBoundsSmart(_,false,false,false).
     * 
     * @param targetClientBounds Target client bounds, in screen.
     * @return True if bounds changed or might have changed, false otherwise.
     * @throws NullPointerException if the specified bounds is null.
     */
    public boolean setClientBoundsSmart(GRect targetClientBounds);
    
    /**
     * If not showing, if iconified, or if maximized,
     * or if bounds setting is not synchronously effective,
     * must just return false, because then getClientBounds()
     * can't be used for checks.
     * 
     * @param targetClientBounds Target client bounds, in screen.
     * @param mustFixRight Allows to avoid right-drift when x increases but x span can't decrease enough.
     * @param mustFixBottom Allows to avoid bottom-drift when y increases but y span can't decrease enough.
     * @param mustRestoreBoundsIfNotExact True if must restore old bounds if specified bounds
     *        could not be set, false if must do best effort.
     * @return True if bounds changed or might have changed, false otherwise.
     * @throws NullPointerException if the specified bounds is null.
     */
    public boolean setClientBoundsSmart(
            GRect targetClientBounds,
            boolean mustFixRight,
            boolean mustFixBottom,
            boolean mustRestoreBoundsIfNotExact);
    
    /**
     * Convenience method.
     * Equivalent to setWindowBoundsSmart(_,false,false,false).
     * 
     * @param targetWindowBounds Target window bounds, in screen.
     * @return True if bounds changed or might have changed, false otherwise.
     * @throws NullPointerException if the specified bounds is null.
     */
    public boolean setWindowBoundsSmart(GRect targetWindowBounds);
    
    /**
     * If not showing, if iconified, or if maximized,
     * or if bounds setting is not synchronously effective,
     * must just return false, because then getWindowBounds()
     * can't be used for checks.
     * 
     * @param targetWindowBounds Target window bounds, in screen.
     * @param mustFixRight Allows to avoid right-drift when x increases but x span can't decrease enough.
     * @param mustFixBottom Allows to avoid bottom-drift when y increases but y span can't decrease enough.
     * @param mustRestoreBoundsIfNotExact True if must restore old bounds if specified bounds
     *        could not be set, false if must do best effort.
     * @return True if bounds changed or might have changed, false otherwise.
     * @throws NullPointerException if the specified bounds is null.
     */
    public boolean setWindowBoundsSmart(
            GRect targetWindowBounds,
            boolean mustFixRight,
            boolean mustFixBottom,
            boolean mustRestoreBoundsIfNotExact);
    
    /*
     * Painting.
     */
    
    /**
     * Should be callable from any thread
     * (for consistency with other host painting methods).
     * 
     * The default value for this parameter is false.
     * 
     * If this binding does not support multiple client scaling algorithms,
     * this method must do nothing (and not throw).
     * 
     * See also setAccurateImageScaling() in graphics API.
     * 
     * @param accurate True if eventual client scaling (due to backing library
     *        using a different resolution than BWD) should use an accurate
     *        (but possibly slow) algorithm, rather than a fast (but typically
     *        less accurate) one.
     */
    public void setAccurateClientScaling(boolean accurate);
    
    /**
     * Should be callable from any thread.
     * 
     * It's up to the binding to decide, for next client painting,
     * whether it must do one painting for each specified dirty rectangle,
     * or a single painting using the bounding box of all specified
     * dirty rectangles, which should be the safer choice to avoid
     * the overhead of too many paintings.
     * 
     * @param dirtyRect Rectangle covering dirty pixels that must be repaint
     *        on next painting, whether it has been triggered by this binding
     *        or by the backing library, even if the corresponding
     *        displayable state or model didn't change.
     *        Can cover pixels outside of client area.
     *        Must not be null.
     */
    public void makeDirty(GRect dirtyRect);
    
    /**
     * Convenience method.
     * 
     * Equivalent to calling makeDirty(GRect.DEFAULT_HUGE).
     */
    public void makeAllDirty();
    
    /**
     * Convenience method.
     * 
     * Equivalent to calling makeDirty(GRect)
     * and then ensurePendingClientPainting().
     */
    public void makeDirtyAndEnsurePendingClientPainting(GRect dirtyRect);
    
    /**
     * Convenience method.
     * 
     * Equivalent to calling makeDirty(GRect.DEFAULT_HUGE)
     * and then ensurePendingClientPainting().
     */
    public void makeAllDirtyAndEnsurePendingClientPainting();

    /**
     * Should be callable from any thread.
     * 
     * Ensures that a call to client.paintClient(...) is pending,
     * and shall occur within a short period of time, typically ASAP or after
     * 1/30th or 1/60th of a second.
     * Multiple calls to client.paintClient(...) might actually be conducted,
     * if multiple dirty rectangles were specified with makeDirty(GRect)
     * and the binding decides that it's better to do one painting
     * per dirty rectangle rather than a single painting using
     * dirty rectangles bounding box.
     * The client.paintClient(...) call(s) might actually be aborted if painting
     * is not possible, for example, depending on the backing library,
     * if the window is hidden, in which case a subsequent painting
     * (when window shows up again) should be ensured with
     * the whole client area dirty.
     * 
     * Meant to be used both from binding code, for example to trigger
     * a repaint after a resize, or from client code, to trigger UI update
     * on model change.
     * 
     * We don't provide a method for synchronous or ASAP painting, for multiple
     * reasons.
     * First, it might not be possible to implement, for example if the backing
     * library only allows for periodic painting.
     * Second, it would be a CPU waste to allow for many useless paintings that
     * could not make it to the screen due to actual FPS limitations, such as
     * the binding might want to limit the PPS (paintings per second) depending
     * on the FPS limit. Note that the CPU waste would come both from client
     * painting code, and from eventual backing library painting flush
     * treatments, that can be slow on some platforms.
     * Third, some libraries flush drawing to screen asynchronously, such as
     * painting ASAP cyclically would make the PPS go through the roof, while
     * the FPS would remain constant and much lower, which would add to the
     * wasted cycles a glitchy and typically irregular decorrelation effect
     * between what is drawn and what gets visible.
     * 
     * Note that a synchronous painting method would require host to implement
     * some throwing non-recursion checks when calling it, to avoid recursive
     * painting when calling it from painting code, resulting into
     * stack overflows or other nasty errors.
     */
    public void ensurePendingClientPainting();
}
