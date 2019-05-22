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
package net.jolikit.bwd.impl.utils.drag;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.api.InterfaceBwdCursorManager;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.InterfaceHostSupplier;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * Factors code for dragging handling, such as for split panes or scroll bars.
 */
public abstract class AbstractDragController implements InterfaceDragController {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Can be BwdCursors.NO_STATEMENT.
     */
    private final int dragCursor;
    private final InterfaceHostSupplier hostSupplier;
    
    private Object cursorAddKey;
    
    private boolean dragging = false;

    /*
     * Draggable position in client, on drag start.
     */
    
    private int draggableXOnDragStart;
    private int draggableYOnDragStart;
    
    /*
     * Cursor position in client minus draggable position in client,
     * on drag start.
     */
    
    private int draggableToCursorXOffsetOnDragStart;
    private int draggableToCursorYOffsetOnDragStart;
    
    /*
     * Desired draggable position in client, depending on current cursor
     * position in client.
     */
    
    private int draggableDesiredX;
    private int draggableDesiredY;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates a drag controller without drag cursor.
     */
    public AbstractDragController() {
        this.dragCursor = BwdCursors.NO_STATEMENT;
        this.hostSupplier = null;
    }

    /**
     * @param dragCursor Cursor to use when mouse is over draggable area.
     *        Can be BwdCursors.INVISIBLE, but must not be BwdCursors.NO_STATEMENT.
     * @param hostSupplier Supplier from which cursor manager
     *        can be retrieved for cursor setting.
     *        Must not be null.
     */
    public AbstractDragController(
            int dragCursor,
            InterfaceHostSupplier hostSupplier) {
        if (dragCursor == BwdCursors.NO_STATEMENT) {
            throw new IllegalArgumentException("no cursor");
        }
        this.dragCursor = LangUtils.requireNonNull(dragCursor);
        this.hostSupplier = LangUtils.requireNonNull(hostSupplier);
    }
    
    /*
     * 
     */
    
    /**
     * To call typically on window events that you want to stop drag,
     * like HIDDEN/ICONIFIED/MAXIMIZED/DEMAXIMIZED.
     * 
     * @return True if stopped drag, false otherwise.
     */
    public boolean dragStoppingEventOccured() {
        final boolean wasDragging = this.dragging;
        if (wasDragging) {
            if (DEBUG) {
                Dbg.log("dragStoppingEventOccured : wasDragging");
            }
            this.dragging = false;
            
            if (this.cursorAddKey != null) {
                final boolean mustRemoveCursor;
                
                final InterfaceBwdHost host = this.hostSupplier.getHost();
                if (host == null) {
                    mustRemoveCursor = true;
                } else {
                    final GRect clientBounds = host.getClientBounds();
                    if (clientBounds.isEmpty()) {
                        // Bad state of pathological bounds.
                        mustRemoveCursor = true;
                    } else {
                        // Bounds look valid.
                        final GPoint mousePosInScreen = host.getMousePosInScreen();
                        final int mouseXInClient = mousePosInScreen.x() - clientBounds.x();
                        final int mouseYInClient = mousePosInScreen.y() - clientBounds.y();
                        
                        mustRemoveCursor = !this.isOverDraggable(mouseXInClient, mouseYInClient);
                    }
                }
                
                if (mustRemoveCursor) {
                    this.removeDragCursorIfAny();
                }
            }
        }
        return wasDragging;
    }

    /*
     * 
     */
    
    /**
     * @return True if started drag, false otherwise.
     */
    public boolean mousePressed(int x, int y) {
        final boolean overDraggable = this.isOverDraggable(x, y);
        if (overDraggable) {
            if (DEBUG) {
                Dbg.log("mousePressed(" + x + ", " + y + ") : overDraggable");
            }
            this.mousePressed_forceDrag(x, y);
        }
        return overDraggable;
    }

    /**
     * Starts dragging even if mouse is not over draggable.
     */
    public void mousePressed_forceDrag(int x, int y) {
        this.dragging = true;

        final int draggableX = this.getDraggableX();
        final int draggableY = this.getDraggableY();

        this.draggableXOnDragStart = draggableX;
        this.draggableYOnDragStart = draggableY;

        final int xOffset = x - draggableX;
        final int yOffset = y - draggableY;
        this.draggableToCursorXOffsetOnDragStart = xOffset;
        this.draggableToCursorYOffsetOnDragStart = yOffset;

        if (DEBUG) {
            Dbg.log("mousePressed_forceDrag : mouse pos = ("
                    + x + ", " + y + "), draggable = ("
                    + draggableX + ", " + draggableY + "), delta = ("
                    + xOffset + ", " + yOffset + ")");
        }
        
        // So that it can be used before actual drag.
        this.draggableDesiredX = draggableX;
        this.draggableDesiredY = draggableY;

        if (this.dragCursor != BwdCursors.NO_STATEMENT) {
            this.ensureDragCursorIfPossible();
        }
    }

    /**
     * @return True if stopped drag, false otherwise.
     */
    public boolean mouseReleased(int x, int y) {
        final boolean wasDragging = this.dragging;
        if (wasDragging) {
            if (DEBUG) {
                Dbg.log("mouseReleased : wasDragging");
            }
            this.dragging = false;
            
            if (this.cursorAddKey != null) {
                final InterfaceBwdCursorManager cursorManager = this.getCursorManager();
                if (cursorManager != null) {
                    if (!this.isOverDraggable(x, y)) {
                        this.removeDragCursorIfAny();
                    }
                }
            }
        }
        return wasDragging;
    }

    /**
     * @return True if there is a drag cursor and it's set, false otherwise.
     */
    public boolean mouseMoved(int x, int y) {
        if (this.dragCursor != BwdCursors.NO_STATEMENT) {
            final InterfaceBwdCursorManager cursorManager = this.getCursorManager();
            if (cursorManager != null) {
                if (this.isOverDraggable(x, y)) {
                    this.ensureDragCursorIfPossible();
                    return true;
                } else {
                    this.removeDragCursorIfAny();
                }
            }
        }
        return false;
    }

    /**
     * @return True if is dragging, in which case target draggable position has
     *         been computed and can be retrieved next, false otherwise.
     */
    public boolean mouseDragged(int x, int y) {
        final boolean isDragging = this.dragging;
        if (isDragging) {
            this.draggableDesiredX = x - this.draggableToCursorXOffsetOnDragStart;
            this.draggableDesiredY = y - this.draggableToCursorYOffsetOnDragStart;
            if (DEBUG) {
                Dbg.log("mouseDragged(" + x + ", " + y + ") : desired = (" + this.draggableDesiredX + ", " + this.draggableDesiredY + ")");
            }
        }
        return isDragging;
    }
    
    /**
     * Only defined after mousePressed(int,int) returns true,
     * and until mouseReleased(int,int) returns true.
     * 
     * @return Desired X, in client, for draggable object.
     */
    @Override
    public int getDesiredX() {
        if (DEBUG) {
            Dbg.log("getDesiredX() = " + this.draggableDesiredX);
        }
        return this.draggableDesiredX;
    }
    
    /**
     * Only defined after mousePressed(int,int) returns true,
     * and until mouseReleased(int,int) returns true.
     * 
     * @return Desired Y, in client, for draggable object.
     */
    @Override
    public int getDesiredY() {
        if (DEBUG) {
            Dbg.log("getDesiredY() = " + this.draggableDesiredY);
        }
        return this.draggableDesiredY;
    }
    
    /**
     * Only defined after mousePressed(int,int) returns true,
     * and until mouseReleased(int,int) returns true.
     */
    public int getXOnDragStart() {
        return this.draggableXOnDragStart;
    }

    /**
     * Only defined after mousePressed(int,int) returns true,
     * and until mouseReleased(int,int) returns true.
     */
    public int getYOnDragStart() {
        return this.draggableYOnDragStart;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Can test visibility or not, depending on whether this drag controller
     * will be used only after visibility checks or not.
     * 
     * @param x In client.
     * @param y In client.
     * @return True if the drag must be started in case of left-press at the
     *         specified position, or if eventual drag cursor must be set in
     *         when the mouse is at the specified position, false otherwise.
     */
    protected abstract boolean isOverDraggable(int x, int y);
    
    /**
     * @return X coordinate, in client, of draggable object.
     */
    protected abstract int getDraggableX();
    
    /**
     * @return Y coordinate, in client, of draggable object.
     */
    protected abstract int getDraggableY();
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdCursorManager getCursorManager() {
        final InterfaceBwdHost host = this.hostSupplier.getHost();
        if (host == null) {
            return null;
        }
        return host.getCursorManager();
    }
    
    /**
     * If there is no cursor manager, does nothing.
     */
    private void ensureDragCursorIfPossible() {
        if (this.cursorAddKey == null) {
            final InterfaceBwdCursorManager cursorManager = this.getCursorManager();
            if (cursorManager != null) {
                this.cursorAddKey = cursorManager.addCursorAndGetAddKey(this.dragCursor);
            }
        }
    }
    
    private void removeDragCursorIfAny() {
        if (this.cursorAddKey != null) {
            // Must not be null since we did add a cursor.
            final InterfaceBwdCursorManager cursorManager = this.getCursorManager();
            cursorManager.removeCursorForAddKey(this.cursorAddKey);
            this.cursorAddKey = null;
        }
    }
}
