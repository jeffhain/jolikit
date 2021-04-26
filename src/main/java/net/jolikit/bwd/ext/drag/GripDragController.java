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

import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.ext.InterfaceHostSupplier;
import net.jolikit.lang.LangUtils;

/**
 * Drag controller for our grips, which location within client bounds
 * is fixed, and which are rectangular.
 */
public class GripDragController extends AbstractDragController {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private GRect gripPaintedBox = GRect.DEFAULT_EMPTY;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates a grip drag controller without drag cursor.
     */
    public GripDragController() {
    }

    /**
     * @param dragCursor Cursor to use during drag. Can be BwdCursors.INVISIBLE.
     * @param hostSupplier Must not be null.
     */
    public GripDragController(
            int dragCursor,
            InterfaceHostSupplier hostSupplier) {
        super(dragCursor, hostSupplier);
    }
    
    public GRect getGripPaintedBox() {
        return this.gripPaintedBox;
    }

    public void setGripPaintedBox(GRect gripPaintedBox) {
        this.gripPaintedBox = LangUtils.requireNonNull(gripPaintedBox);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected boolean isOverDraggable(GPoint pos) {
        return this.gripPaintedBox.contains(pos);
    }
    
    @Override
    protected int getDraggableX() {
        return this.gripPaintedBox.x();
    }
    
    @Override
    protected int getDraggableY() {
        return this.gripPaintedBox.y();
    }
}
