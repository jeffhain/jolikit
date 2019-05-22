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
package net.jolikit.bwd.impl.jfx;

import javafx.event.EventType;
import javafx.scene.input.GestureEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
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
 * For wheel events: ScrollEvent
 */
public class JfxEventConverter extends AbstractEventConverter {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final JfxKeyConverter keyConverter = new JfxKeyConverter();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JfxEventConverter(
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
        
        /*
         * Not doing some else/if, not to depend on events hierarchy.
         */
        
        final CmnInputConvState commonState = this.getCommonState();
        
        if (backingEvent instanceof KeyEvent) {
            final KeyEvent keyEvent = (KeyEvent) backingEvent;
            
            commonState.setShiftDown(keyEvent.isShiftDown());
            commonState.setControlDown(keyEvent.isControlDown());
            commonState.setAltDown(keyEvent.isAltDown());
            commonState.setMetaDown(keyEvent.isMetaDown());
        }
        
        if (backingEvent instanceof MouseEvent) {
            final MouseEvent mouseEvent = (MouseEvent) backingEvent;
            
            final GPoint mousePosInScreen = GPoint.valueOf(
                    (int) mouseEvent.getScreenX(),
                    (int) mouseEvent.getScreenY());
            commonState.setMousePosInScreen(mousePosInScreen);
            
            commonState.setPrimaryButtonDown(mouseEvent.isPrimaryButtonDown());
            commonState.setMiddleButtonDown(mouseEvent.isMiddleButtonDown());
            commonState.setSecondaryButtonDown(mouseEvent.isSecondaryButtonDown());

            commonState.setShiftDown(mouseEvent.isShiftDown());
            commonState.setControlDown(mouseEvent.isControlDown());
            commonState.setAltDown(mouseEvent.isAltDown());
            commonState.setMetaDown(mouseEvent.isMetaDown());
        }
        
        if (backingEvent instanceof GestureEvent) {
            final GestureEvent gestureEvent = (GestureEvent) backingEvent;
            
            final GPoint mousePosInScreen = GPoint.valueOf(
                    (int) gestureEvent.getScreenX(),
                    (int) gestureEvent.getScreenY());
            commonState.setMousePosInScreen(mousePosInScreen);
            
            commonState.setShiftDown(gestureEvent.isShiftDown());
            commonState.setControlDown(gestureEvent.isControlDown());
            commonState.setAltDown(gestureEvent.isAltDown());
            commonState.setMetaDown(gestureEvent.isMetaDown());
        }
        
        /*
         * Updating host-specific state.
         */
        
        if (backingEvent instanceof MouseEvent) {
            final MouseEvent mouseEvent = (MouseEvent) backingEvent;
            final int x = (int) mouseEvent.getX();
            final int y = (int) mouseEvent.getY();
            final GPoint mousePosInClient = GPoint.valueOf(x, y);
            this.setMousePosInClient(mousePosInClient);
            
        } else if (backingEvent instanceof GestureEvent) {
            final GestureEvent gestureEvent = (GestureEvent) backingEvent;
            final int x = (int) gestureEvent.getX();
            final int y = (int) gestureEvent.getY();
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
            Dbg.log("jfx key event : code = " + keyEvent.getCode());
        }
        
        final KeyCode backingKey = keyEvent.getCode();
        
        return this.keyConverter.get(backingKey);
    }

    @Override
    protected int getKeyLocation(Object backingEvent) {
        final KeyEvent keyEvent = (KeyEvent) backingEvent;
        
        final KeyCode backingKey = keyEvent.getCode();
        
        /*
         * TODO jfx Don't have much location info.
         */

        switch (backingKey) {
        // Only works when NUM_LOCK is on.
        case NUMPAD0:
        case NUMPAD1:
        case NUMPAD2:
        case NUMPAD3:
        case NUMPAD4:
        case NUMPAD5:
        case NUMPAD6:
        case NUMPAD7:
        case NUMPAD8:
        case NUMPAD9:
            //
        case KP_UP:
        case KP_DOWN:
        case KP_LEFT:
        case KP_RIGHT:
            //
            return BwdKeyLocations.NUMPAD;
        default:
            return BwdKeyLocations.NO_STATEMENT;
        }
    }

    @Override
    protected int getCodePoint(Object backingEvent) {
        final KeyEvent keyEvent = (KeyEvent) backingEvent;
        final String cpStr = keyEvent.getCharacter();
        if ((cpStr.length() == 0)
                || (cpStr == KeyEvent.CHAR_UNDEFINED)) {
            return 0;
        }
        return cpStr.codePointAt(0);
    }

    /*
     * Mouse events.
     */
    
    @Override
    protected int getButton(Object backingEvent) {
        final MouseEvent mouseEvent = (MouseEvent) backingEvent;
        if (isMouseButtonEvent(mouseEvent)) {
            return computeButton(mouseEvent);
        } else {
            /*
             * TODO jfx Need to do that, because mouse moved events
             * have last pressed button in it, which might not be
             * actually pressed any more.
             */
            return BwdMouseButtons.NO_STATEMENT;
        }
    }

    /*
     * Wheel events.
     */

    @Override
    protected int getWheelXRoll(Object backingEvent) {
        final ScrollEvent scrollEvent = (ScrollEvent) backingEvent;
        
        final boolean from1DWheel = computeIsFrom1DDevice(scrollEvent);
        final int xRoll;
        if (from1DWheel) {
            xRoll = 0;
        } else {
            xRoll = computeBwdRollAmount(scrollEvent.getDeltaX());
        }
        return xRoll;
    }

    @Override
    protected int getWheelYRoll(Object backingEvent) {
        final ScrollEvent scrollEvent = (ScrollEvent) backingEvent;
        
        final int yRoll = computeBwdRollAmount(scrollEvent.getDeltaY());
        return yRoll;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static boolean computeIsFrom1DDevice(ScrollEvent event) {
        /*
         * TODO jfx Best effort.
         * Glitchy side effect: if the device is 2D, and deltaX is zero,
         * and shift is down, the delta will be put in X instead of Y.
         */
        return (event.getDeltaX() == 0.0);
    }
    
    private static int computeBwdRollAmount(double delta) {
        /*
         * TODO jfx For now, this simple code makes sure that
         * we don't miss any roll quantity, but it could lead
         * to abnormally high sensitivity.
         * Instead, we should maybe accumulate, possibly dividing
         * by ScrollEvent.getMultiplierX/Y(), and have 0-roll events
         * or ignore events until 1 is reached, at which point
         * we would generate a 1-roll event.
         * Multiplier: "On Mac this will always be 10.
         * On Windows, the Multiplier is more complex and rea
         * from the system." (40 for scroll wheel, 40/13 for my touchpad X/Y).
         */
        return (int) Math.signum(-delta);
    }

    private static boolean isMouseButtonEvent(MouseEvent event) {
        final EventType<?> eventType = event.getEventType();
        return (eventType == MouseEvent.MOUSE_PRESSED)
                || (eventType == MouseEvent.MOUSE_RELEASED)
                || (eventType == MouseEvent.MOUSE_CLICKED);
    }
    
    private static int computeButton(MouseEvent event) {
        final MouseButton backingButton = event.getButton();
        if (backingButton == null) {
            /*
             * TODO jfx Should not happen, since MouseButton.NONE exists,
             * but (button != null) is tested at some point in JavaFX.
             * The later is maybe due to the observable habit in JavaFX code,
             * of aborting quietly when something is null, even if it should
             * never be null in practice.
             */
            return BwdMouseButtons.NO_STATEMENT;
        }
        switch (backingButton) {
        case NONE: return BwdMouseButtons.NO_STATEMENT;
        case PRIMARY: return BwdMouseButtons.PRIMARY;
        case MIDDLE: return BwdMouseButtons.MIDDLE;
        case SECONDARY: return BwdMouseButtons.SECONDARY;
        default:
            return BwdMouseButtons.NO_STATEMENT;
        }
    }
}
