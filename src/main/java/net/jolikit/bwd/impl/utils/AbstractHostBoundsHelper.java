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

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * Makes it easier to implement window and client bounds
 * setting and retrieval.
 * 
 * Methods default implementations delegate to each other,
 * for easier implementations when it's the only way to compute
 * the requested values.
 */
public abstract class AbstractHostBoundsHelper {
    
    /*
     * Abstract even though has no abstract method, because it could have.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_GET = DEBUG && false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final InterfaceBackingWindowHolder holder;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractHostBoundsHelper(InterfaceBackingWindowHolder holder) {
        this.holder = LangUtils.requireNonNull(holder);
    }
    
    /*
     * 
     */
    
    /**
     * @return A rectangle with (left,top,right,bottom) spans of
     *         window border, or GRect.DEFAULT_EMPTY
     *         if holder says backing window is closed.
     */
    public final GRect getInsets() {
        final GRect insets;
        
        if (DEBUG_GET) {
            logWithHostPrefix("getInsets()");
        }
        
        if (this.holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                logWithHostPrefix("getInsets() : closed : ignoring");
            }
            insets = GRect.DEFAULT_EMPTY;
        } else if (!this.holder.isDecorated()) {
            insets = this.getInsetsUndecorated_raw();
        } else {
            insets = this.getInsetsDecorated_raw();
        }
        
        if (DEBUG_GET) {
            logWithHostPrefix("getInsets() : insets = " + insets);
        }
        
        return insets;
    }

    /*
     * 
     */

    /**
     * @return Client bounds in screen, or GRect.DEFAULT_EMPTY
     *         if holder says backing window is closed.
     */
    public final GRect getClientBounds() {
        
        if (DEBUG_GET) {
            logWithHostPrefix("getClientBounds()");
        }
        
        final GRect clientBounds;
        if (this.holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                logWithHostPrefix("getClientBounds() : closed : ignoring");
            }
            clientBounds = GRect.DEFAULT_EMPTY;
        } else {
            clientBounds = this.getClientBounds_raw();
        }
        
        if (DEBUG_GET) {
            logWithHostPrefix("getClientBounds() : clientBounds = " + clientBounds);
        }
        
        return clientBounds;
    }
    
    /**
     * @return Window bounds in screen, or GRect.DEFAULT_EMPTY
     *         if holder says backing window is closed.
     */
    public final GRect getWindowBounds() {
        
        if (DEBUG_GET) {
            logWithHostPrefix("getWindowBounds()");
        }
        
        final GRect windowBounds;
        if (this.holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                logWithHostPrefix("getWindowBounds() : closed : ignoring");
            }
            windowBounds = GRect.DEFAULT_EMPTY;
        } else {
            windowBounds = this.getWindowBounds_raw();
        }
        
        if (DEBUG_GET) {
            logWithHostPrefix("getWindowBounds() : windowBounds = " + windowBounds);
        }
        
        return windowBounds;
    }

    /*
     * 
     */
    
    /**
     * Does nothing if holder says backing window is closed.
     * 
     * @param Target client bounds in screen.
     */
    public final void setClientBounds(GRect targetClientBounds) {
        
        if (DEBUG) {
            logWithHostPrefix("setClientBounds(" + targetClientBounds + ")", new RuntimeException("for stack"));
        }
        
        if (this.holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                logWithHostPrefix("setClientBounds() : closed : ignoring");
            }
        } else {
            this.setClientBounds_raw(targetClientBounds);
        }
    }
    
    /**
     * Does nothing if holder says backing window is closed.
     * 
     * @param Target window bounds in screen.
     */
    public final void setWindowBounds(GRect targetWindowBounds) {
        
        if (DEBUG) {
            logWithHostPrefix("setWindowBounds(" + targetWindowBounds + ")", new RuntimeException("for stack"));
        }
        
        if (this.holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                logWithHostPrefix("setWindowBounds() : closed : ignoring");
            }
        } else {
            this.setWindowBounds_raw(targetWindowBounds);
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    protected InterfaceBackingWindowHolder getHolder() {
        return this.holder;
    }

    /**
     * Requires this class DEBUG to be true to work
     * (to make sure this class doesn't depend on Dbg when it's false).
     */
    protected void logWithHostPrefix(String message) {
        if (DEBUG) {
            Dbg.logPr(this, this.getHolder().getTitle() + " : " + message);
        }
    }
    
    /**
     * Requires this class DEBUG to be true to work
     * (to make sure this class doesn't depend on Dbg when it's false).
     */
    protected void logWithHostPrefix(String message, Throwable t) {
        if (DEBUG) {
            Dbg.logPr(this, this.getHolder().getTitle() + " : " + message, t);
        }
    }
    
    /*
     * 
     */
    
    /**
     * This default implementation returns GRect.DEFAULT_EMPTY.
     * 
     * @return A rectangle with (left,top,right,bottom) spans of
     *         undecorated border if any, else (0,0,0,0).
     */
    protected GRect getInsetsUndecorated_raw() {
        return GRect.DEFAULT_EMPTY;
    }
    
    /**
     * Only called if not closed and decorated.
     * 
     * Default implementation uses window and client bounds.
     * 
     * @return A rectangle with (left,top,right,bottom) spans of
     *         decoration border.
     */
    protected GRect getInsetsDecorated_raw() {
        return BindingCoordsUtils.computeInsets(
                this.getClientBounds_raw(),
                this.getWindowBounds_raw());
    }

    /**
     * Only called if not closed.
     */
    protected final GRect getInsets_raw() {
        final GRect insets;
        if (!this.holder.isDecorated()) {
            insets = this.getInsetsUndecorated_raw();
        } else {
            insets = this.getInsetsDecorated_raw();
        }
        return insets;
    }
    
    /*
     * 
     */

    /**
     * Only called if not closed.
     * 
     * Default implementation uses border rect and window bounds.
     * 
     * @return Client bounds in screen.
     */
    protected GRect getClientBounds_raw() {
        return BindingCoordsUtils.computeClientBounds(
                this.getInsets_raw(),
                this.getWindowBounds_raw());
    }
    
    /**
     * Only called if not closed.
     * 
     * Default implementation uses border rect and client bounds.
     * 
     * @return Window bounds in screen.
     */
    protected GRect getWindowBounds_raw() {
        return BindingCoordsUtils.computeWindowBounds(
                this.getInsets_raw(),
                this.getClientBounds_raw());
    }

    /*
     * 
     */
    
    /**
     * Only called if not closed.
     * 
     * Default implementation uses border rect and window bounds.
     * 
     * @param Target client bounds in screen.
     */
    protected void setClientBounds_raw(GRect targetClientBounds) {
        final GRect targetWindowBounds =
                BindingCoordsUtils.computeWindowBounds(
                        this.getInsets_raw(),
                        targetClientBounds);
        this.setWindowBounds_raw(targetWindowBounds);
    }
    
    /**
     * Only called if not closed.
     * 
     * Default implementation uses border rect and client bounds.
     * 
     * @param Target window bounds in screen.
     */
    protected void setWindowBounds_raw(GRect targetWindowBounds) {
        final GRect targetClientBounds =
                BindingCoordsUtils.computeClientBounds(
                        this.getInsets_raw(),
                        targetWindowBounds);
        this.setClientBounds_raw(targetClientBounds);
    }
}
