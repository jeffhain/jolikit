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
package net.jolikit.bwd.impl.awt;

import java.awt.AWTEvent;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import net.jolikit.bwd.api.events.BwdKeyLocations;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.events.AbstractEventConverter;
import net.jolikit.bwd.impl.utils.events.CmnInputConvState;
import net.jolikit.lang.Dbg;

/**
 * For key events: KeyEvent
 * For mouse events: MouseEvent
 * For wheel events: MouseWheelEvent
 */
public class AwtEventConverter extends AbstractEventConverter {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final AwtKeyConverter keyConverter = new AwtKeyConverter();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AwtEventConverter(
            CmnInputConvState commonState,
            AbstractBwdHost host) {
        super(commonState, host);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void updateFromBackingEvent(Object backingEvent) {
        
        /*
         * Updating common state.
         */
        
        final CmnInputConvState commonState = this.getCommonState();
        
        if (backingEvent instanceof InputEvent) {
            final InputEvent inputEvent = (InputEvent) backingEvent;
            
            commonState.setShiftDown(inputEvent.isShiftDown());
            commonState.setControlDown(inputEvent.isControlDown());
            commonState.setAltDown(inputEvent.isAltDown());
            commonState.setAltGraphDown(inputEvent.isAltGraphDown());
            commonState.setMetaDown(inputEvent.isMetaDown());
       }
        
        if (backingEvent instanceof MouseEvent) {
            final MouseEvent mouseEvent = (MouseEvent) backingEvent;
            
            final GPoint mousePosInScreen = GPoint.valueOf(
                    mouseEvent.getXOnScreen(),
                    mouseEvent.getYOnScreen());
            commonState.setMousePosInScreen(mousePosInScreen);
            
            final int modEx = mouseEvent.getModifiersEx();
            commonState.setPrimaryButtonDown((modEx & MouseEvent.BUTTON1_DOWN_MASK) != 0);
            commonState.setMiddleButtonDown((modEx & MouseEvent.BUTTON2_DOWN_MASK) != 0);
            commonState.setSecondaryButtonDown((modEx & MouseEvent.BUTTON3_DOWN_MASK) != 0);
        }
        
        /*
         * Updating host-specific state.
         */
        
        if (backingEvent instanceof MouseEvent) {
            final MouseEvent mouseEvent = (MouseEvent) backingEvent;
            
            int x = mouseEvent.getX();
            int y = mouseEvent.getY();
            
            final AWTEvent awtEvent = (AWTEvent) backingEvent;
            final Object source = awtEvent.getSource();
            if (source instanceof Window) {
                /*
                 * Not coming from a content pane.
                 * Useful for AWT, for which we can't get events to come
                 * from a content pane as with Swing.
                 */
                final Window window = (Window) source;
                final Insets insets = window.getInsets();
                x -= insets.left;
                y -= insets.top;
            }
            
            final GPoint mousePosInClient = GPoint.valueOf(x, y);
            this.setMousePosInClient(mousePosInClient);
        }
    }
    
    /*
     * Key events.
     */
    
    @Override
    protected int getKey(Object backingEvent) {
        final KeyEvent keyEvent = (KeyEvent) backingEvent;
        
        if (DEBUG) {
            Dbg.log("");
            Dbg.log("awt key event : keyCode = " + keyEvent.getKeyCode());
            // TODO Java 7+
            //Dbg.log("awt key event : eKeyCode = " + keyEvent.getExtendedKeyCode());
        }
        
        final int backingKey = getBackingKey(keyEvent);
        
        return this.keyConverter.get(backingKey);
    }

    @Override
    protected int getKeyLocation(Object backingEvent) {
        final KeyEvent keyEvent = (KeyEvent) backingEvent;
        
        final int backingKey = getBackingKey(keyEvent);
        if (backingKey == KeyEvent.VK_UNDEFINED) {
            // Not a key, better not try to read its location.
            return BwdKeyLocations.NO_STATEMENT;
        }
        
        final int backingKeyLocation = keyEvent.getKeyLocation();
        switch (backingKeyLocation) {
        case KeyEvent.KEY_LOCATION_LEFT:
            return BwdKeyLocations.LEFT;
        case KeyEvent.KEY_LOCATION_RIGHT:
            return BwdKeyLocations.RIGHT;
        case KeyEvent.KEY_LOCATION_NUMPAD:
            return BwdKeyLocations.NUMPAD;
        default:
            return BwdKeyLocations.NO_STATEMENT;
        }
    }

    @Override
    protected int getCodePoint(Object backingEvent) {
        final KeyEvent keyEvent = (KeyEvent) backingEvent;
        // TODO awt Limited to BMP.
        final char keyChar = keyEvent.getKeyChar();
        return (int) keyChar;
    }

    /*
     * Mouse events.
     */
    
    @Override
    protected int getButton(Object backingEvent) {
        final MouseEvent mouseEvent = (MouseEvent) backingEvent;
        final int backingButton = mouseEvent.getButton();
        switch (backingButton) {
        case MouseEvent.BUTTON1: return BwdMouseButtons.PRIMARY;
        case MouseEvent.BUTTON2: return BwdMouseButtons.MIDDLE;
        case MouseEvent.BUTTON3: return BwdMouseButtons.SECONDARY;
        default:
            return BwdMouseButtons.NO_STATEMENT;
        }
    }

    /*
     * Wheel events.
     */

    @Override
    protected int getWheelXRoll(Object backingEvent) {
        return 0;
    }

    @Override
    protected int getWheelYRoll(Object backingEvent) {
        final MouseWheelEvent mouseWheelEvent = (MouseWheelEvent) backingEvent;
        return computeBwdRollAmount(mouseWheelEvent.getWheelRotation());
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int computeBwdRollAmount(int wheelRotation) {
        /*
         * TODO awt Maybe we should just use signum,
         * as done for some other bindings.
         */
        return wheelRotation;
    }
    
    private static int getBackingKey(KeyEvent event) {
        
        /*
         * TODO awt From Java 7, can use getExtendedKeyCode(),
         * which is said to depend on current keyboard layout,
         * which might be a bit better (even if our spec doesn't
         * mandate so).
         * TODO awt Contrary to what getExtendedKeyCode() spec says,
         * it appears that getKeyCode() already doesn't depend on layout
         * (at least for my config).
         */
        
        int backingKey = event.getKeyCode();
        
        /*
         * TODO awt When pressing AlgGraph, keyCode is Alt.
         * Using keyLocation to discriminate. 
         */
        
        if ((backingKey == KeyEvent.VK_ALT)
                && (event.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT)) {
            backingKey = KeyEvent.VK_ALT_GRAPH;
        }
        
        return backingKey;
    }
}
