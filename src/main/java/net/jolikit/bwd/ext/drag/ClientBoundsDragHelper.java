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
package net.jolikit.bwd.ext.drag;

import java.util.HashMap;
import java.util.Map;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.ext.InterfaceHostSupplier;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NbrsUtils;

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
    
    private int clientMinXSpan;
    private int clientMinYSpan;

    private int clientMaxXSpan = Integer.MAX_VALUE;
    private int clientMaxYSpan = Integer.MAX_VALUE;

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
     * Uses zero min spans.
     */
    public <DC extends AbstractDragController> ClientBoundsDragHelper(
            InterfaceHostSupplier hostSupplier,
            InterfaceGripRectComputer gripRectComputer,
            Map<GripType,DC> dragControllerByGripType) {
        this(0, 0, hostSupplier, gripRectComputer, dragControllerByGripType);
    }
    
    /**
     * @param clientMinXSpan Must be >= 0.
     * @param clientMinYSpan Must be >= 0.
     */
    public <DC extends AbstractDragController> ClientBoundsDragHelper(
            int clientMinXSpan,
            int clientMinYSpan,
            InterfaceHostSupplier hostSupplier,
            InterfaceGripRectComputer gripRectComputer,
            Map<GripType,DC> dragControllerByGripType) {
        
        this.setClientMinMaxSpans_final(
            clientMinXSpan,
            clientMinYSpan,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE);
        
        this.hostSupplier = hostSupplier;
        
        this.gripRectComputer = gripRectComputer;
        
        this.dragControllerByGripType.putAll(dragControllerByGripType);
    }
    
    /**
     * Max spans are Integer.MAX_VALUE by default.
     * 
     * @param clientMinXSpan Must be >= 0.
     * @param clientMinYSpan Must be >= 0.
     * @param clientMaxXSpan Must be >= clientMinXSpan.
     * @param clientMaxYSpan Must be >= clientMaxYSpan.
     */
    public void setClientMinMaxSpans(
        int clientMinXSpan,
        int clientMinYSpan,
        int clientMaxXSpan,
        int clientMaxYSpan) {
        this.setClientMinMaxSpans_final(
            clientMinXSpan,
            clientMinYSpan,
            clientMaxXSpan,
            clientMaxYSpan);
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
            for (AbstractDragController dragController : this.dragControllerByGripType.values()) {
                dragController.mousePressed(event.posInClient());
            }
        }
    }

    public void onMouseReleased(BwdMouseEvent event) {
        if (event.hasDragButton()) {
            for (AbstractDragController dragController : this.dragControllerByGripType.values()) {
                dragController.mouseReleased(event.posInClient());
            }
        }
    }
    
    public void onMouseMoved(BwdMouseEvent event) {
        for (AbstractDragController dragController : this.dragControllerByGripType.values()) {
            dragController.mouseMoved(event.posInClient());
        }
    }

    public void onMouseDragged(BwdMouseEvent event) {
        
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
            final boolean isDragging =
                myDragController.mouseDragged(
                    event.posInClient());
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
        
        final GRect oldGripRect =
            this.gripRectComputer.computeGripRectInClientBox(
                oldClientBox, gripType);
        
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
                Dbg.log(
                    "onMouseDragged() : scheduling setClientBoundsSmart("
                        + newClientBounds
                        + "," + mustFixRight + "," + mustFixBottom
                        + "," + restoreBoundsIfNotExact + ")");
            }
            /*
             * NB: At once was using async here (with UI thread scheduler),
             * in case sync would hurt mouse event processing,
             * but if I recall I had trouble with Allegro5 on Mac,
             * so now we are synchronous (which also helps for debug).
             */
            if (DEBUG) {
                Dbg.log("setClientBoundsSmart("
                    + newClientBounds
                    + "," + mustFixRight + "," + mustFixBottom
                    + "," + restoreBoundsIfNotExact + ")");
            }
            host.setClientBoundsSmart(
                newClientBounds,
                mustFixRight,
                mustFixBottom,
                restoreBoundsIfNotExact);
        } else {
            if (DEBUG) {
                Dbg.log("onMouseDragged() : client already has target bounds");
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private final void setClientMinMaxSpans_final(
        int clientMinXSpan,
        int clientMinYSpan,
        int clientMaxXSpan,
        int clientMaxYSpan) {
        NbrsUtils.requireSupOrEq(0, clientMinXSpan, "clientMinXSpan");
        NbrsUtils.requireSupOrEq(0, clientMinYSpan, "clientMinYSpan");
        NbrsUtils.requireSupOrEq(clientMinXSpan, clientMaxXSpan, "clientMaxXSpan");
        NbrsUtils.requireSupOrEq(clientMinYSpan, clientMaxYSpan, "clientMaxYSpan");
        
        this.clientMinXSpan = clientMinXSpan;
        this.clientMinYSpan = clientMinYSpan;
        this.clientMaxXSpan = clientMaxXSpan;
        this.clientMaxYSpan = clientMaxYSpan;
    }
    
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

        final int newClientXSpan = NbrsUtils.toRange(
            this.clientMinXSpan,
            this.clientMaxXSpan,
            clientXSpan + (leftElseRightDrag ? -1 : 1) * desiredDx);
        
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

        final int newClientYSpan = NbrsUtils.toRange(
            this.clientMinYSpan,
            this.clientMaxYSpan,
            clientYSpan + (topElseBottomDrag ? -1 : 1) * desiredDy);
        
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
