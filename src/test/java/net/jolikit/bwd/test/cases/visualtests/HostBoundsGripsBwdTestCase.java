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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.ext.InterfaceHostSupplier;
import net.jolikit.bwd.ext.drag.ClientBoundsDragHelper;
import net.jolikit.bwd.ext.drag.DefaultGripRectComputer;
import net.jolikit.bwd.ext.drag.GripDragController;
import net.jolikit.bwd.ext.drag.GripType;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;

/**
 * To test dragging and resizing.
 * Cf. "mustFixBoundsDuringDrag" in BaseBwdBindingConfig.
 */
public class HostBoundsGripsBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int DEFAULT_GRIP_SPAN = 20;
    private static final int DEFAULT_MIN_CLIENT_X_SPAN = 150;
    private static final int DEFAULT_MIN_CLIENT_Y_SPAN = 150;

    private static final String IMG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG;
    
    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final DefaultGripRectComputer gripRectComputer =
            new DefaultGripRectComputer(DEFAULT_GRIP_SPAN);
    
    private final Map<GripType,GripDragController> dragControllerByGripType =
            new HashMap<GripType,GripDragController>();
    
    private ClientBoundsDragHelper dragHelper;
    
    private InterfaceBwdImage image;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public HostBoundsGripsBwdTestCase() {
    }

    @Override
    public void setHost(Object host) {
        super.setHost(host);
        this.initDragControllers();
        // In case getGripSpan() overridden.
        this.gripRectComputer.setGripSpan(this.getGripSpan());
    }

    public HostBoundsGripsBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
        // False else conflicts with our drag logic
        // (wasted a few days on this!).
        this.setDragOnLeftClickActivated(false);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new HostBoundsGripsBwdTestCase(binding);
    }
    
    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    /*
     * Window events.
     */
    
    @Override
    public void onWindowHidden(BwdWindowEvent event) {
        super.onWindowHidden(event);
        
        this.dragHelper.onDragStoppingEvent();
    }
    
    @Override
    public void onWindowIconified(BwdWindowEvent event) {
        super.onWindowIconified(event);
        
        this.dragHelper.onDragStoppingEvent();
    }
    
    @Override
    public void onWindowMaximized(BwdWindowEvent event) {
        super.onWindowMaximized(event);
        
        this.dragHelper.onDragStoppingEvent();
    }
    
    @Override
    public void onWindowDemaximized(BwdWindowEvent event) {
        super.onWindowDemaximized(event);
        
        this.dragHelper.onDragStoppingEvent();
    }

    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);
        
        this.dragHelper.onMousePressed(event);
    }

    @Override
    public void onMouseReleased(BwdMouseEvent event) {
        super.onMouseReleased(event);
        
        this.dragHelper.onMouseReleased(event);
    }
    
    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        super.onMouseMoved(event);
        
        this.dragHelper.onMouseMoved(event);
    }

    @Override
    public void onMouseDragged(BwdMouseEvent event) {
        super.onMouseDragged(event);
        
        this.dragHelper.onMouseDragged(event);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    /*
     * For extending classes.
     */
    
    protected int getGripSpan() {
        return DEFAULT_GRIP_SPAN;
    }

    protected int getMinClientXSpan() {
        return DEFAULT_MIN_CLIENT_X_SPAN;
    }

    protected int getMinClientYSpan() {
        return DEFAULT_MIN_CLIENT_Y_SPAN;
    }
    
    /**
     * To be called to take into account
     * getGripSpan()/getMinClientXSpan()/getMinClientYSpan()
     * results modification.
     */
    protected final void onSpanChange() {
        this.gripRectComputer.setGripSpan(this.getGripSpan());
        
        this.dragHelper.setClientMinMaxSpans(
            this.getMinClientXSpan(),
            this.getMinClientYSpan(),
            Integer.MAX_VALUE,
            Integer.MAX_VALUE);
    }
    
    protected boolean mustScaleImageToWithinGrip() {
        return false;
    }
    
    /*
     * 
     */

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        // Graphics box is client box.
        final GRect clientBox = g.getBox();
        
        /*
         * Updating grips boxes,
         * and painting them.
         * 
         * Since our grip boxes cover the whole client area,
         * no need for preliminary clearing.
         */
        
        for (int i = 0; i < GripType.valueList().size(); i++) {
            final GripType gt = GripType.valueList().get(i);

            final GripDragController dc =
                this.dragControllerByGripType.get(gt);
            final GRect gripBox =
                this.gripRectComputer.computeGripRectInClientBox(
                    clientBox, gt);
            dc.setGripPaintedBox(gripBox);

            g.setColor(BwdColor.BLACK);
            g.fillRect(gripBox);
            if (gt != GripType.CENTER) {
                g.setColor(BwdColor.WHITE);
                g.drawRect(gripBox);
            }
        }
        
        /*
         * Drawing some image, to be able to see eventual scaling issues.
         */
        
        InterfaceBwdImage image = this.image;
        if (image == null) {
            image = getBinding().newImage(IMG_FILE_PATH);
            this.image = image;
        }
        
        {
            final int border = this.getGripSpan();
            
            if (this.mustScaleImageToWithinGrip()) {
                final GRect imgDstRect =
                    clientBox.withBordersDeltasElseEmpty(
                        border,
                        border,
                        -border,
                        -border);
                g.drawImage(
                    imgDstRect,
                    image);
            } else {
                // Could be negative.
                final int partXSpan = Math.min(
                    image.getWidth(),
                    clientBox.xSpan() - 2*border);
                final int partYSpan = Math.min(
                    image.getHeight(),
                    clientBox.ySpan() - 2*border);
                g.drawImage(
                    border, border, partXSpan, partYSpan,
                    image,
                    0, 0, partXSpan, partYSpan);
            }
        }

        /*
         * 
         */
        
        return null;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void initDragControllers() {
        final InterfaceHostSupplier hostSupplier = this;

        for (int i = 0; i < GripType.valueList().size(); i++) {
            final GripType gripType = GripType.valueList().get(i);
            final int cursor = ClientBoundsDragHelper.getCursor(gripType);
            final GripDragController dc = new GripDragController(
                    cursor,
                    hostSupplier);
            this.dragControllerByGripType.put(gripType, dc);
        }
        
        this.dragHelper = new ClientBoundsDragHelper(
                DEFAULT_MIN_CLIENT_X_SPAN,
                DEFAULT_MIN_CLIENT_Y_SPAN,
                hostSupplier,
                this.gripRectComputer,
                this.dragControllerByGripType);
    }
}
