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
package net.jolikit.bwd.ext.drag;

import java.util.HashMap;
import java.util.Map;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.ext.InterfaceHostSupplier;
import net.jolikit.lang.Dbg;

/**
 * Helper to implement client area resizing using a draggable border.
 */
public class ClientBoundsDragHelper {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int minClientXSpan;
    private final int minClientYSpan;
    
    /*
     * 
     */
    
    private final InterfaceHostSupplier hostSupplier;
    
    private final InterfaceGripRectComputer gripRectComputer;
    
    private final Map<GripType,AbstractDragController> dragControllerByGripType =
            new HashMap<GripType,AbstractDragController>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param minClientXSpan Must be >= 0.
     * @param minClientYSpan Must be >= 0.
     */
    public <DC extends AbstractDragController> ClientBoundsDragHelper(
            int minClientXSpan,
            int minClientYSpan,
            InterfaceHostSupplier hostSupplier,
            InterfaceGripRectComputer gripRectComputer,
            Map<GripType,DC> dragControllerByGripType) {
        
        if (minClientXSpan < 0) {
            throw new IllegalArgumentException("minClientXSpan [" + minClientXSpan + "] must be >= 0");
        }
        if (minClientYSpan < 0) {
            throw new IllegalArgumentException("minClientYSpan [" + minClientYSpan + "] must be >= 0");
        }
        
        this.minClientXSpan = minClientXSpan;
        this.minClientYSpan = minClientYSpan;
        
        this.hostSupplier = hostSupplier;
        
        this.gripRectComputer = gripRectComputer;
        
        this.dragControllerByGripType.putAll(dragControllerByGripType);
    }
    
    /*
     * 
     */
    
    public static int getCursor(GripType gripType) {
        switch (gripType) {
        case CENTER: return BwdCursors.MOVE;
        //
        case TOP: return BwdCursors.RESIZE_NS;
        case LEFT: return BwdCursors.RESIZE_WE;
        case RIGHT: return BwdCursors.RESIZE_WE;
        case BOTTOM: return BwdCursors.RESIZE_NS;
        //
        case TOP_LEFT: return BwdCursors.RESIZE_NWSE;
        case TOP_RIGHT: return BwdCursors.RESIZE_NESW;
        case BOTTOM_LEFT: return BwdCursors.RESIZE_NESW;
        case BOTTOM_RIGHT: return BwdCursors.RESIZE_NWSE;
        default:
            return BwdCursors.ARROW;
        }
    }

    /*
     * Window events.
     */
    
    public void onDragStoppingEvent() {
        for (AbstractDragController dragController : this.dragControllerByGripType.values()) {
            dragController.onDragStoppingEvent();
        }
    }

    /*
     * Mouse events.
     */
    
    public void onMousePressed(BwdMouseEvent event) {
        if (event.hasDragButton()) {
            final int x = event.xInClient();
            final int y = event.yInClient();
            for (AbstractDragController dragController : this.dragControllerByGripType.values()) {
                dragController.mousePressed(x, y);
            }
        }
    }

    public void onMouseReleased(BwdMouseEvent event) {
        if (event.hasDragButton()) {
            final int x = event.xInClient();
            final int y = event.yInClient();
            for (AbstractDragController dragController : this.dragControllerByGripType.values()) {
                dragController.mouseReleased(x, y);
            }
        }
    }
    
    public void onMouseMoved(BwdMouseEvent event) {
        final int x = event.xInClient();
        final int y = event.yInClient();
        for (AbstractDragController dragController : this.dragControllerByGripType.values()) {
            dragController.mouseMoved(x, y);
        }
    }

    public void onMouseDragged(BwdMouseEvent event) {
        
        final int x = event.xInClient();
        final int y = event.yInClient();
        
        final InterfaceBwdHost host = this.hostSupplier.getHost();
        
        /*
         * 
         */
        
        GRect oldClientBounds = host.getClientBounds();
        if (oldClientBounds.isEmpty()) {
            // Bad state or pathological bounds.
            return;
        }
        
        if (DEBUG) {
            Dbg.log("oldClientBounds = " + oldClientBounds);
        }
        
        final int oldClientXInScreen = oldClientBounds.x();
        final int oldClientYInScreen = oldClientBounds.y();
        final int oldClientXSpan = oldClientBounds.xSpan();
        final int oldClientYSpan = oldClientBounds.ySpan();
        
        final GRect oldClientBox = oldClientBounds.withPos(0, 0);

        int newClientXInScreen = oldClientXInScreen;
        int newClientYInScreen = oldClientYInScreen;
        int newClientXSpan = oldClientXSpan;
        int newClientYSpan = oldClientYSpan;

        GripType gripType = null;
        AbstractDragController dragController = null;
        for (Map.Entry<GripType,AbstractDragController> entry : this.dragControllerByGripType.entrySet()) {
            final GripType myGripType = entry.getKey();
            final AbstractDragController myDragController = entry.getValue();
            final boolean isDragging = myDragController.mouseDragged(x, y);
            if (isDragging) {
                gripType = myGripType;
                dragController = myDragController;
                break;
            }
        }
        
        if (DEBUG) {
            Dbg.log("gripType = " + gripType);
        }
        
        if (gripType == null) {
            return;
        }

        // Set to true if left involved.
        final boolean mustFixRight = (gripType.xSide < 0);
        // Set to true if top involved.
        final boolean mustFixBottom = (gripType.ySide < 0);
        
        final GRect oldGripRect = this.gripRectComputer.computeGripRectInClientBox(oldClientBox, gripType);
        
        switch (gripType) {
        /*
         * Drag from center : move.
         */
        case CENTER: {
            final int clientXInScreen = oldClientBounds.x();
            final int clientYInScreen = oldClientBounds.y();

            final int desiredX = dragController.getDesiredX();
            final int desiredY = dragController.getDesiredY();
            if (DEBUG) {
                Dbg.log("dragController = " + dragController);
                Dbg.log("desiredX = " + desiredX);
                Dbg.log("desiredY = " + desiredY);
            }
            
            newClientXInScreen = clientXInScreen + (desiredX - oldGripRect.x());
            newClientYInScreen = clientYInScreen + (desiredY - oldGripRect.y());
        } break;
        /*
         * Drag from border : resize and possibly move.
         */
        default: {
            newClientXSpan = this.computeNewClientXSpanOnBorderDrag(
                    dragController,
                    oldGripRect,
                    oldClientBounds,
                    gripType);
            newClientYSpan = this.computeNewClientYSpanOnBorderDrag(
                    dragController,
                    oldGripRect,
                    oldClientBounds,
                    gripType);
            if (gripType.xSide < 0) {
                newClientXInScreen = oldClientXInScreen - (newClientXSpan - oldClientXSpan);
            }
            if (gripType.ySide < 0) {
                newClientYInScreen = oldClientYInScreen - (newClientYSpan - oldClientYSpan);
            }
        } break;
        }
        
        if (DEBUG) {
            Dbg.log("newClientXInScreen = " + newClientXInScreen);
            Dbg.log("newClientYInScreen = " + newClientYInScreen);
            Dbg.log("newClientXSpan = " + newClientXSpan);
            Dbg.log("newClientYSpan = " + newClientYSpan);
        }
        
        final GRect newClientBounds = GRect.valueOf(
                newClientXInScreen,
                newClientYInScreen,
                newClientXSpan,
                newClientYSpan);
        
        if (!newClientBounds.equals(oldClientBounds)) {
            final boolean restoreBoundsIfNotExact = false;
            if (DEBUG) {
                Dbg.log("onMouseDragged() : scheduling setClientBoundsSmart(" + newClientBounds + "," + mustFixRight + "," + mustFixBottom + "," + restoreBoundsIfNotExact + ")");
            }
            /*
             * Not setting bounds synchronously,
             * in case it would hurt mouse event processing.
             */
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) {
                        Dbg.log("setClientBoundsSmart(" + newClientBounds + "," + mustFixRight + "," + mustFixBottom + "," + restoreBoundsIfNotExact + ")");
                    }
                    host.setClientBoundsSmart(
                            newClientBounds,
                            mustFixRight,
                            mustFixBottom,
                            restoreBoundsIfNotExact);
                }
            };
            /*
             * NB: At once was using async here (with UI thread scheduler),
             * but if I recall I had trouble with Allegro5 on Mac,
             * so now we are synchronous (which also helps for debug).
             */
            runnable.run();
        } else {
            if (DEBUG) {
                Dbg.log("onMouseDragged() : client already has target bounds");
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private int computeNewClientXSpanOnBorderDrag(
            InterfaceDragController horizDragController,
            GRect oldGripRect,
            GRect clientBounds,
            GripType gripType) {
        
        final int clientXSpan = clientBounds.xSpan();
        
        if (gripType.xSide == 0) {
            return clientXSpan;
        }
        
        final boolean leftElseRightDrag = (gripType.xSide < 0);

        final int desiredX = horizDragController.getDesiredX();
        
        final int desiredDx = (desiredX - oldGripRect.x());

        final int newClientXSpan = Math.max(
                clientXSpan + (leftElseRightDrag ? -1 : 1) * desiredDx,
                this.minClientXSpan);
        
        if (DEBUG) {
            Dbg.log("---");
            Dbg.log("gripType = " + gripType);
            Dbg.log("clientXSpan = " + clientXSpan);
            Dbg.log("desiredX = " + desiredX);
            Dbg.log("oldGripRect = " + oldGripRect);
            Dbg.log("desiredDx = " + desiredDx);
            Dbg.log("newClientXSpan = " + newClientXSpan);
        }

        return newClientXSpan;
    }

    private int computeNewClientYSpanOnBorderDrag(
            InterfaceDragController verticalDragController,
            GRect oldGripRect,
            GRect clientBounds,
            GripType gripType) {
        
        final int clientYSpan = clientBounds.ySpan();

        if (gripType.ySide == 0) {
            return clientYSpan;
        }

        final boolean topElseBottomDrag = (gripType.ySide < 0);
        
        final int desiredY = verticalDragController.getDesiredY();
        
        final int desiredDy = (desiredY - oldGripRect.y());

        final int newClientYSpan = Math.max(
                clientYSpan + (topElseBottomDrag ? -1 : 1) * desiredDy,
                this.minClientYSpan);
        
        if (DEBUG) {
            Dbg.log("---");
            Dbg.log("gripType = " + gripType);
            Dbg.log("clientYSpan = " + clientYSpan);
            Dbg.log("desiredY = " + desiredY);
            Dbg.log("oldGripRect = " + oldGripRect);
            Dbg.log("desiredDy = " + desiredDy);
            Dbg.log("newClientYSpan = " + newClientYSpan);
        }

        return newClientYSpan;
    }
}