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
package net.jolikit.bwd.test.cases.utils;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.basics.InterfaceHostSupplier;
import net.jolikit.bwd.impl.utils.drag.ClientBoundsDragHelper;
import net.jolikit.bwd.impl.utils.drag.DefaultGripRectComputer;
import net.jolikit.bwd.impl.utils.drag.GripDragController;
import net.jolikit.bwd.impl.utils.drag.GripType;
import net.jolikit.bwd.impl.utils.drag.InterfaceGripRectComputer;
import net.jolikit.bwd.test.utils.AbstractBwdTestCaseHome;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.LangUtils;

/**
 * Optional abstract class for implementing BWD mocks.
 * 
 * Is also the client, to make it easier to implement mock clients
 * by overriding client methods.
 * 
 * Implements client drag with mouse primary button,
 * so that non-decorated hosts can be moved around,
 * and host close on middle button click, both of which
 * can be deactivated.
 */
public abstract class AbstractBwdTestCase extends AbstractBwdTestCaseHome implements InterfaceBwdTestCase, InterfaceBwdTestCaseClient {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * For client mock part.
     */
    
    private final InterfaceBwdBinding binding;
    
    /*
     * 
     */
    
    private boolean dragOnLeftClickActivated = true;
    private boolean closeOnMiddleClickActivated = true;
    
    /*
     * 
     */
    
    private final GripDragController dragController;
    private final ClientBoundsDragHelper dragHelper;

    /*
     * 
     */
    
    private SortedSet<String> loadedFontFilePathSet;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor for test case home.
     */
    public AbstractBwdTestCase() {
        this.binding = null;
        this.dragController = null;
        this.dragHelper = null;
    }
    
    /**
     * Constructor for test case and client.
     * 
     * For use as client, must (of course) set host before use.
     */
    public AbstractBwdTestCase(InterfaceBwdBinding binding) {
        this.binding = LangUtils.requireNonNull(binding);
        this.configureUiThreadCheck(binding.getUiThreadScheduler());
        
        final InterfaceHostSupplier hostSupplier = this;

        final int gripSpan = 0;
        final InterfaceGripRectComputer gripRectComputer =
                new DefaultGripRectComputer(gripSpan);

        final GripType gripType = GripType.CENTER;
        final GripDragController dragController = new GripDragController();
        this.dragController = dragController;
        
        final Map<GripType,GripDragController> dragControllerByGripType =
                new HashMap<GripType,GripDragController>();
        dragControllerByGripType.put(gripType, dragController);

        // Not used, since we don't use helper for resize.
        final int minClientXSpan = 0;
        final int minClientYSpan = 0;
        this.dragHelper = new ClientBoundsDragHelper(
                minClientXSpan,
                minClientYSpan,
                hostSupplier,
                gripRectComputer,
                dragControllerByGripType);
    }
    
    @Override
    public List<GRect> paintClient(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        if (this.dragOnLeftClickActivated) {
            this.dragController.setGripPaintedBox(g.getBoxInClient());
        }

        return super.paintClient(g, dirtyRect);
    }

    /*
     * Window events.
     */
    
    @Override
    public void onWindowHidden(BwdWindowEvent event) {
        super.onWindowHidden(event);
        
        if (this.dragOnLeftClickActivated) {
            this.dragHelper.dragStoppingEventOccured();
        }
    }
    
    @Override
    public void onWindowFocusLost(BwdWindowEvent event) {
        super.onWindowFocusLost(event);
        
        if (this.dragOnLeftClickActivated) {
            this.dragHelper.dragStoppingEventOccured();
        }
    }
    
    @Override
    public void onWindowIconified(BwdWindowEvent event) {
        super.onWindowIconified(event);
        
        if (this.dragOnLeftClickActivated) {
            this.dragHelper.dragStoppingEventOccured();
        }
    }
    
    @Override
    public void onWindowMaximized(BwdWindowEvent event) {
        super.onWindowMaximized(event);
        
        if (this.dragOnLeftClickActivated) {
            this.dragHelper.dragStoppingEventOccured();
        }
    }
    
    @Override
    public void onWindowDemaximized(BwdWindowEvent event) {
        super.onWindowDemaximized(event);
        
        if (this.dragOnLeftClickActivated) {
            this.dragHelper.dragStoppingEventOccured();
        }
    }

    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);
        
        if (this.dragOnLeftClickActivated) {
            this.dragHelper.onMousePressed(event);
        }

        if (this.closeOnMiddleClickActivated) {
            if (event.getButton() == BwdMouseButtons.MIDDLE) {
                this.getHost().close();
            }
        }
    }

    @Override
    public void onMouseReleased(BwdMouseEvent event) {
        super.onMouseReleased(event);
        
        if (this.dragOnLeftClickActivated) {
            this.dragHelper.onMouseReleased(event);
        }
    }

    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        super.onMouseMoved(event);
        
        if (this.dragOnLeftClickActivated) {
            this.dragHelper.onMouseMoved(event);
        }
    }

    @Override
    public void onMouseDragged(BwdMouseEvent event) {
        super.onMouseDragged(event);
        
        if (this.dragOnLeftClickActivated) {
            this.dragHelper.onMouseDragged(event);
        }
    }

    /*
     * 
     */
    
    /**
     * This default implementation returns null.
     */
    @Override
    public UncaughtExceptionHandler getExceptionHandlerElseNull() {
        return null;
    }

    /**
     * This default implementation returns false.
     */
    @Override
    public boolean getMustSequenceLaunches() {
        return false;
    }

    /**
     * This default implementation returns true.
     */
    @Override
    public boolean getHostDecorated() {
        return true;
    }
    
    /**
     * This default implementation returns 1.0.
     */
    @Override
    public double getWindowAlphaFp() {
        return 1.0;
    }
    
    /**
     * This default implementation returns true.
     */
    @Override
    public boolean getMustImplementBestEffortPixelReading() {
        return true;
    }
    
    /*
     * 
     */

    @Override
    public void setLoadedFontFilePathSet(SortedSet<String> loadedFontFilePathSet) {
        this.loadedFontFilePathSet = loadedFontFilePathSet;
    }
    
    public SortedSet<String> getLoadedFontFilePathSet() {
        return this.loadedFontFilePathSet;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * True by default.
     */
    protected final void setDragOnLeftClickActivated(boolean dragOnLeftClickActivated) {
        if (!dragOnLeftClickActivated) {
            this.dragHelper.dragStoppingEventOccured();
        }
        this.dragOnLeftClickActivated = dragOnLeftClickActivated;
    }
    
    /**
     * True by default.
     */
    protected final void setCloseOnMiddleClickActivated(boolean closeOnMiddleClickActivated) {
        this.closeOnMiddleClickActivated = closeOnMiddleClickActivated;
    }
    
    /*
     * For client mock part.
     */

    protected InterfaceBwdBinding getBinding() {
        return this.binding;
    }
}
