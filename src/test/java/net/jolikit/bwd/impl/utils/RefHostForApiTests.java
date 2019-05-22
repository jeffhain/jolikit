package net.jolikit.bwd.impl.utils;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdCursorManager;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdEventListenerUtils;
import net.jolikit.bwd.api.events.BwdEventType;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;

/**
 * Simple implementation to serve as reference for testing AbstractBwdHost
 * API basic behavior, for state (bounds included) and related window events.
 * 
 * Designed to be simple and understandable, not to cover non-trivial topics
 * such as sturdiness to exceptions, timeouts, hard scheduling, state being
 * modified from event listener (from client), etc.
 */
public class RefHostForApiTests implements InterfaceBwdHost {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyOneState {
        boolean value = false;
        final BwdEventType toTrueEventType;
        final BwdEventType toFalseEventType;
        public MyOneState(
                BwdEventType toTrueEventType,
                BwdEventType toFalseEventType) {
            this.toTrueEventType = toTrueEventType;
            this.toFalseEventType = toFalseEventType;
        }
        public boolean get() {
            return this.value;
        }
        /**
         * Fires if changed, and event type defined.
         */
        public void set(boolean newValue) {
            final boolean changed = (newValue != this.value);
            this.value = newValue;
            if (changed) {
                final BwdEventType eventType;
                if (newValue) {
                    eventType = this.toTrueEventType;
                } else {
                    eventType = this.toFalseEventType;
                }
                if (eventType != null) {
                    createAndFireWindowEvent(eventType);
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceBwdClient client;
    
    private final MyOneState showing = new MyOneState(
            BwdEventType.WINDOW_SHOWN,
            BwdEventType.WINDOW_HIDDEN);
    
    private final MyOneState focused = new MyOneState(
            BwdEventType.WINDOW_FOCUS_GAINED,
            BwdEventType.WINDOW_FOCUS_LOST);
    
    private final MyOneState iconified = new MyOneState(
            BwdEventType.WINDOW_ICONIFIED,
            BwdEventType.WINDOW_DEICONIFIED);
    
    private final MyOneState maximized = new MyOneState(
            BwdEventType.WINDOW_MAXIMIZED,
            BwdEventType.WINDOW_DEMAXIMIZED);
    
    private final MyOneState closed = new MyOneState(
            BwdEventType.WINDOW_CLOSED,
            null);
    
    private final GRect insets;
    
    private final GRect maximizedClientBounds;
    
    private GRect backingClientBounds = GRect.DEFAULT_EMPTY;

    private GRect targetClientBounds_onShowingDeicoDemax = null;
    private GRect targetWindowBounds_onShowingDeicoDemax = null;

    private boolean clientEventMovedPending = false;
    private boolean clientEventResizedPending = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param maximizedClientBounds Set on maximization. Can be null.
     */
    public RefHostForApiTests(
            InterfaceBwdClient client,
            GRect insets,
            GRect maximizedClientBounds) {
        this.client = client;
        this.insets = insets;
        this.maximizedClientBounds = maximizedClientBounds;
    }
    
    /*
     * Internal state, for debug.
     */

    public GRect getBackingClientBounds() {
        return this.backingClientBounds;
    }
    
    public GRect getTargetClientBounds_onShowingDeicoDemax() {
        return this.targetClientBounds_onShowingDeicoDemax;
    }
    
    public GRect getTargetWindowBounds_onShowingDeicoDemax() {
        return this.targetWindowBounds_onShowingDeicoDemax;
    }
    
    public boolean getClientEventMovedPending() {
        return this.clientEventMovedPending;
    }
    
    public boolean getClientEventResizedPending() {
        return this.clientEventResizedPending;
    }
    
    /*
     * 
     */
    
    @Override
    public InterfaceBwdClient getClient() {
        return this.client;
    }

    /*
     * 
     */
    
    @Override
    public boolean isShowing() {
        return (!this.closed.get()) && this.showing.get();
    }

    @Override
    public void show() {
        if (this.closed.get()) {
            return;
        }
        this.showing.set(true);
        this.iconified.set(false);
        if (this.isShowingDeicoDemax()) {
            this.onShowingDeicoDemax();
        }
        if ((this.maximizedClientBounds != null) && this.isShowingDeicoMax()) {
            this.setClientBounds_raw(this.maximizedClientBounds);
        }
        this.flushBoundsEvent();
        this.focused.set(true);
    }

    @Override
    public void hide() {
        if (this.closed.get()) {
            return;
        }
        this.focused.set(false);
        this.showing.set(false);
    }

    /*
     * 
     */
    
    @Override
    public boolean isFocused() {
        return (!this.closed.get()) && this.showing.get() && (!this.iconified.get()) && this.focused.get();
    }

    @Override
    public void requestFocusGain() {
        if (this.closed.get() || (!this.showing.get()) || this.iconified.get()) {
            return;
        }
        this.focused.set(true);
    }

    public void requestFocusLoss() {
        if (this.closed.get() || (!this.showing.get()) || this.iconified.get()) {
            return;
        }
        this.focused.set(false);
    }

    /*
     * 
     */
    
    @Override
    public boolean isIconified() {
        return (!this.closed.get()) && this.showing.get() && this.iconified.get();
    }

    @Override
    public void iconify() {
        if (this.closed.get() || (!this.showing.get())) {
            return;
        }
        // NB: If already iconified, necessarily not focused.
        this.focused.set(false);
        this.iconified.set(true);
    }

    @Override
    public void deiconify() {
        if (this.closed.get() || (!this.showing.get())) {
            return;
        }
        this.iconified.set(false);
        if (this.isShowingDeicoDemax()) {
            this.onShowingDeicoDemax();
        }
        if ((this.maximizedClientBounds != null) && this.isShowingDeicoMax()) {
            this.setClientBounds_raw(this.maximizedClientBounds);
        }
        this.flushBoundsEvent();
        this.focused.set(true);
    }
    
    /*
     * 
     */

    @Override
    public boolean isMaximized() {
        return (!this.closed.get()) && this.showing.get() && (!this.iconified.get()) && this.maximized.get();
    }

    @Override
    public void maximize() {
        if (this.closed.get() || (!this.showing.get()) || this.iconified.get()) {
            return;
        }
        this.maximized.set(true);
        if ((this.maximizedClientBounds != null) && this.isShowingDeicoMax()) {
            this.setClientBounds_raw(this.maximizedClientBounds);
        }
        this.focused.set(true);
    }

    @Override
    public void demaximize() {
        if (this.closed.get() || (!this.showing.get()) || this.iconified.get()) {
            return;
        }
        this.maximized.set(false);
        if (this.isShowingDeicoDemax()) {
            this.onShowingDeicoDemax();
        }
        this.flushBoundsEvent();
        this.focused.set(true);
    }
    
    /*
     * 
     */

    @Override
    public boolean isClosed() {
        return this.closed.get();
    }

    @Override
    public void close() {
        if (this.closed.get()) {
            return;
        }
        this.focused.set(false);
        this.showing.set(false);
        this.closed.set(true);
    }

    /*
     * 
     */
    
    @Override
    public GRect getInsets() {
        if (this.closed.get() || (!this.showing.get()) || this.iconified.get()) {
            return GRect.DEFAULT_EMPTY;
        }
        return this.insets;
    }

    @Override
    public GRect getClientBounds() {
        if (this.closed.get() || (!this.showing.get()) || this.iconified.get()) {
            return GRect.DEFAULT_EMPTY;
        }
        return this.backingClientBounds;
    }

    @Override
    public GRect getWindowBounds() {
        if (this.closed.get() || (!this.showing.get()) || this.iconified.get()) {
            return GRect.DEFAULT_EMPTY;
        }
        return BindingCoordsUtils.computeWindowBounds(this.insets, this.backingClientBounds);
    }

    @Override
    public void setClientBounds(GRect targetClientBounds) {
        
        targetClientBounds = sanitizedTargetBounds(targetClientBounds);

        if (this.closed.get()) {
            return;
        }

        this.clearStoredBounds();

        if ((!this.showing.get()) || this.iconified.get() || this.maximized.get()) {
            this.targetClientBounds_onShowingDeicoDemax = targetClientBounds;
        } else {
            this.setClientBounds_raw(targetClientBounds);
        }
    }
    
    @Override
    public void setWindowBounds(GRect targetWindowBounds) {
        
        targetWindowBounds = sanitizedTargetBounds(targetWindowBounds);

        if (this.closed.get()) {
            return;
        }
        
        this.clearStoredBounds();
        
        if ((!this.showing.get()) || this.iconified.get() || this.maximized.get()) {
            this.targetWindowBounds_onShowingDeicoDemax = targetWindowBounds;
        } else {
            final GRect targetClientBounds = BindingCoordsUtils.computeClientBounds(this.insets, targetWindowBounds);
            this.setClientBounds_raw(targetClientBounds);
        }
    }
    
    /*
     * 
     */
    
    public void onBackingWindowMoved() {
        if (this.closed.get()) {
            return;
        }
        if ((!this.showing.get()) || this.iconified.get()) {
            this.clientEventMovedPending = true;
        } else {
            this.fireMovedEvent();
        }
    }
    
    public void onBackingWindowResized() {
        if (this.closed.get()) {
            return;
        }
        if ((!this.showing.get()) || (this.iconified.get())) {
            this.clientEventResizedPending = true;
        } else {
            this.fireResizedEvent();
        }
    }
    
    public void onBackingWindowClosing() {
        this.close();
    }
    
    /*
     * Unused.
     */

    @Override
    public boolean isDialog() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InterfaceBwdHost getOwner() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTitle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDecorated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isModal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InterfaceBwdCursorManager getCursorManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GPoint getMousePosInScreen() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InterfaceBwdHost newDialog(
            String title,
            boolean decorated,
            boolean modal,
            InterfaceBwdClient client) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InterfaceBwdHost> getDialogList() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWindowAlphaFp(double windowAlphaFp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setClientBoundsSmart(GRect targetClientBounds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setClientBoundsSmart(
            GRect targetClientBounds,
            boolean mustFixRight,
            boolean mustFixBottom,
            boolean mustRestoreBoundsIfNotExact) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setWindowBoundsSmart(GRect targetWindowBounds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setWindowBoundsSmart(
            GRect targetWindowBounds,
            boolean mustFixRight,
            boolean mustFixBottom,
            boolean mustRestoreBoundsIfNotExact) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeDirty(GRect dirtyRect) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeAllDirty() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void makeDirtyAndEnsurePendingClientPainting(GRect dirtyRect) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void makeAllDirtyAndEnsurePendingClientPainting() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void ensurePendingClientPainting() {
        throw new UnsupportedOperationException();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BwdWindowEvent newWindowEvent(BwdEventType eventType) {
        return new BwdWindowEvent(this, eventType);
    }
    
    private void createAndFireWindowEvent(BwdEventType eventType) {
        final BwdWindowEvent event = newWindowEvent(eventType);
        BwdEventListenerUtils.callProperMethod(this.client, event);
    }
    
    /*
     * 
     */
    
    private boolean isShowingDeicoMax() {
        return this.showing.get() && (!this.iconified.get()) && this.maximized.get();
    }
    
    private void clearStoredBounds() {
        this.targetClientBounds_onShowingDeicoDemax = null;
        this.targetWindowBounds_onShowingDeicoDemax = null;
        /*
         * Not clearing pending events flags,
         * because even if bounds are set back to their initial value
         * after a move/resize and before event firing could occur,
         * these events must still be fired.
         */
    }
    
    private boolean isShowingDeicoDemax() {
        return this.isShowing() && (!this.isIconified()) && (!this.isMaximized());
    }
    
    private void onShowingDeicoDemax() {
        
        /*
         * AbstractBWdHost does flush bounds events
         * before eventually applying stored bounds
         * (which might only be done after a delay),
         * so we do the same for tests to pass,
         * even though the flushed event could be
         * redundant with another event fired from
         * stored bounds restoring.
         */
        this.flushBoundsEvent();
        
        this.applyAndClearStoredBounds();
    }
    
    private void applyAndClearStoredBounds() {
        final GRect targetCb = this.targetClientBounds_onShowingDeicoDemax;
        final GRect targetWb = this.targetWindowBounds_onShowingDeicoDemax;

        this.clearStoredBounds();

        if (targetCb != null) {
            this.setClientBounds_raw(targetCb);
        } else if (targetWb != null) {
            final GRect targetclientBounds =
                    BindingCoordsUtils.computeClientBounds(this.insets, targetWb);
            this.setClientBounds_raw(targetclientBounds);
        }
    }

    private void flushBoundsEvent() {
        final boolean pendingMovedEvent = this.clientEventMovedPending;
        final boolean pendingResizedEvent = this.clientEventResizedPending;

        if (pendingMovedEvent) {
            this.fireMovedEvent();
        }
        if (pendingResizedEvent) {
            this.fireResizedEvent();
        }
    }

    private void fireMovedEvent() {
        this.clientEventMovedPending = false;
        this.createAndFireWindowEvent(BwdEventType.WINDOW_MOVED);
    }

    private void fireResizedEvent() {
        this.clientEventResizedPending = false;
        this.createAndFireWindowEvent(BwdEventType.WINDOW_RESIZED);
    }

    private void setClientBounds_raw(GRect newClientBounds) {
        final boolean boundsMoved = boundsMoved(
                this.backingClientBounds,
                newClientBounds);
        final boolean boundsResized = boundsResized(
                this.backingClientBounds,
                newClientBounds);
        this.backingClientBounds = newClientBounds;
        if (boundsMoved) {
            this.fireMovedEvent();
        }
        if (boundsResized) {
            this.fireResizedEvent();
        }
    }
    
    private static boolean boundsMoved(GRect oldBounds, GRect newBounds) {
        return (oldBounds.x() != newBounds.x())
                || (oldBounds.y() != newBounds.y());
    }
    
    private static boolean boundsResized(GRect oldBounds, GRect newBounds) {
        return (oldBounds.xSpan() != newBounds.xSpan())
                || (oldBounds.ySpan() != newBounds.ySpan());
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
}
