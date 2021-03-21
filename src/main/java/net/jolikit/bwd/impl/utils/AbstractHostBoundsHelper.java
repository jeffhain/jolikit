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
 * 
 * Only deals with coordinates in OS pixels (no scaling).
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

    public AbstractHostBoundsHelper(
        InterfaceBackingWindowHolder holder) {
        this.holder = LangUtils.requireNonNull(holder);
    }
    
    /*
     * 
     */
    
    /**
     * @return A rectangle with (left,top,right,bottom) spans of
     *         window border, in OS pixels, or GRect.DEFAULT_EMPTY
     *         if holder says backing window is closed.
     */
    public final GRect getInsetsInOs() {
        if (DEBUG_GET) {
            logWithHostPrefix("getInsetsInOs()");
        }
        
        final GRect ret;
        if (this.holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                logWithHostPrefix("getInsetsInOs() : closed : ignoring");
            }
            ret = GRect.DEFAULT_EMPTY;
        } else {
            ret = this.getInsets_rawInOs();
        }
        
        if (DEBUG_GET) {
            logWithHostPrefix("getInsetsInOs() : insetsInOs = " + ret);
        }
        
        return ret;
    }

    /*
     * 
     */
    
    /**
     * @return Client bounds in screen, in OS pixels,
     *         or GRect.DEFAULT_EMPTY if holder says that
     *         backing window is closed.
     */
    public final GRect getClientBoundsInOs() {
        if (DEBUG_GET) {
            logWithHostPrefix("getClientBoundsOs()");
        }
        
        final GRect ret;
        if (this.holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                logWithHostPrefix("getClientBoundsOs() : closed : ignoring");
            }
            ret = GRect.DEFAULT_EMPTY;
        } else {
            ret = this.getClientBounds_rawInOs();
        }
        
        if (DEBUG_GET) {
            logWithHostPrefix("getClientBoundsOs() : clientBoundsInOs = " + ret);
        }
        
        return ret;
    }
    
    /**
     * @return Window bounds in screen, in OS pixels,
     *         or GRect.DEFAULT_EMPTY if holder says that
     *         backing window is closed.
     */
    public final GRect getWindowBoundsInOs() {
        if (DEBUG_GET) {
            logWithHostPrefix("getWindowBoundsInOs()");
        }
        
        final GRect ret;
        if (this.holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                logWithHostPrefix("getWindowBoundsInOs() : closed : ignoring");
            }
            ret = GRect.DEFAULT_EMPTY;
        } else {
            ret = this.getWindowBounds_rawInOs();
        }
        
        if (DEBUG_GET) {
            logWithHostPrefix("getWindowBoundsInOs() : windowBoundsInOs = " + ret);
        }
        
        return ret;
    }

    /*
     * 
     */
    
    /**
     * Does nothing if holder says that backing window is closed.
     * 
     * @param Target client bounds in screen, in OS pixels.
     */
    public final void setClientBoundsInOs(GRect targetClientBoundsInOs) {
        if (DEBUG) {
            logWithHostPrefix("setClientBoundsInOs(" + targetClientBoundsInOs + ")", new RuntimeException("for stack"));
        }
        
        if (this.holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                logWithHostPrefix("setClientBoundsInOs() : closed : ignoring");
            }
        } else {
            this.setClientBounds_rawInOs(targetClientBoundsInOs);
        }
    }
    
    /**
     * Does nothing if holder says that backing window is closed.
     * 
     * @param Target window bounds in screen, in OS pixels.
     */
    public final void setWindowBoundsInOs(GRect targetWindowBoundsInOs) {
        if (DEBUG) {
            logWithHostPrefix("setWindowBoundsInOs(" + targetWindowBoundsInOs + ")", new RuntimeException("for stack"));
        }
        
        if (this.holder.isClosed_nonVolatile()) {
            if (DEBUG) {
                logWithHostPrefix("setWindowBoundsInOs() : closed : ignoring");
            }
        } else {
            this.setWindowBounds_rawInOs(targetWindowBoundsInOs);
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
     *         undecorated border if any, in OS pixels, else (0,0,0,0).
     */
    protected GRect getInsetsUndecorated_rawInOs() {
        return GRect.DEFAULT_EMPTY;
    }
    
    /**
     * Only called if decorated and not closed.
     * 
     * Default implementation uses window and client bounds.
     * 
     * @return A rectangle with (left,top,right,bottom) spans of
     *         decoration border, in OS pixels.
     */
    protected GRect getInsetsDecorated_rawInOs() {
        return BindingCoordsUtils.computeInsets(
                this.getClientBounds_rawInOs(),
                this.getWindowBounds_rawInOs());
    }
    
    /**
     * Only called if not closed.
     */
    protected final GRect getInsets_rawInOs() {
        final GRect ret;
        if (!this.holder.isDecorated()) {
            ret = this.getInsetsUndecorated_rawInOs();
        } else {
            ret = this.getInsetsDecorated_rawInOs();
        }
        return ret;
    }
    
    /*
     * 
     */

    /**
     * Only called if not closed.
     * 
     * Default implementation uses insets and window bounds.
     * 
     * @return Client bounds in screen, in OS pixels.
     */
    protected GRect getClientBounds_rawInOs() {
        return BindingCoordsUtils.computeClientBounds(
            this.getInsets_rawInOs(),
            this.getWindowBounds_rawInOs());
    }
    
    /**
     * Only called if not closed.
     * 
     * Default implementation uses insets and client bounds.
     * 
     * @return Window bounds in screen, in OS pixels.
     */
    protected GRect getWindowBounds_rawInOs() {
        return BindingCoordsUtils.computeWindowBounds(
            this.getInsets_rawInOs(),
            this.getClientBounds_rawInOs());
    }
    
    /*
     * 
     */
    
    /**
     * Only called if not closed.
     * 
     * Default implementation uses insets and window bounds.
     * 
     * @param Target client bounds in screen, in OS pixels.
     */
    protected void setClientBounds_rawInOs(GRect targetClientBoundsInOs) {
        final GRect targetWindowBoundsInOs =
                BindingCoordsUtils.computeWindowBounds(
                        this.getInsets_rawInOs(),
                        targetClientBoundsInOs);
        this.setWindowBounds_rawInOs(targetWindowBoundsInOs);
    }
    
    /**
     * Only called if not closed.
     * 
     * Default implementation uses insets and client bounds.
     * 
     * @param Target window bounds in screen, in OS pixels.
     */
    protected void setWindowBounds_rawInOs(GRect targetWindowBoundsInOs) {
        final GRect targetClientBoundsInOs =
                BindingCoordsUtils.computeClientBounds(
                        this.getInsets_rawInOs(),
                        targetWindowBoundsInOs);
        this.setClientBounds_rawInOs(targetClientBoundsInOs);
    }
}
