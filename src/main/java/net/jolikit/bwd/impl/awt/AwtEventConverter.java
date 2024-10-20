/*
 * Copyright 2019-2024 Jeff Hain
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
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import net.jolikit.bwd.api.events.BwdKeyLocations;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.basics.InterfaceBwdHostImpl;
import net.jolikit.bwd.impl.utils.events.AbstractEventConverter;
import net.jolikit.bwd.impl.utils.events.CmnInputConvState;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.lang.OsUtils;

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

    /**
     * TODO awt On Windows (at least), isMetaDown() seems
     * to only work on right-click,
     * so we set/unset it on Windows key press/release,
     * to have same behavior as in JavaFX
     * in both key and mouse events.
     */
    private static final boolean MUST_SET_META_DOWN_FROM_WINDOWS_KEY_EVENTS =
        OsUtils.isWindows();
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Switch to be able to run with just Java6
     * and not require Java7 just for this method.
     */
    private static final boolean PRECISE_WHEEL_ROTATION_AVAILABLE;
    static {
        // To check returned precise wheel rotation,
        // to make "sure" the compiler doesn't optimize the call away.
        final int expectedWheelRotation = 123;
        final MouseWheelEvent event = new MouseWheelEvent(
                new Panel(), 0, 0L, 0, 0, 0, 0,
                false, 0, 0, expectedWheelRotation);
        boolean couldCallIt = false;
        try {
            final double actualPwr = event.getPreciseWheelRotation();
            couldCallIt = true;
            if ((int) actualPwr != expectedWheelRotation) {
                throw new AssertionError();
            }
        } catch (NoSuchMethodError e) {
            // ignore
        }
        PRECISE_WHEEL_ROTATION_AVAILABLE = couldCallIt;
    }
    
    private final AwtKeyConverter keyConverter = new AwtKeyConverter();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AwtEventConverter(
            CmnInputConvState commonState,
            AbstractBwdHost host) {
        super(
            commonState,
            host,
            host.getBindingConfig().getScaleHelper());
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void updateFromBackingEvent(Object backingEvent) {
        
        final CmnInputConvState commonState = this.getCommonState();
        
        if (backingEvent instanceof InputEvent) {
            final InputEvent inputEvent = (InputEvent) backingEvent;
            
            /*
             * TODO awt For some reason, at least on Windows:
             * - when pressing mouse middle button,
             *   "alt" is indicated as down by AWT.
             * - when pressing mouse secondary button,
             *   "meta" is indicated as down by AWT.
             * We don't try to make up for this oddity
             * (which could be deliberate, given the messy context
             * the software world put itself in),
             * as it could cause other issues.
             */
            
            commonState.setShiftDown(inputEvent.isShiftDown());
            commonState.setControlDown(inputEvent.isControlDown());
            commonState.setAltDown(inputEvent.isAltDown());
            commonState.setAltGraphDown(inputEvent.isAltGraphDown());
            if (!MUST_SET_META_DOWN_FROM_WINDOWS_KEY_EVENTS) {
                commonState.setMetaDown(inputEvent.isMetaDown());
            }
        }
        
        if (backingEvent instanceof KeyEvent) {
            final KeyEvent keyEvent = (KeyEvent) backingEvent;
            
            if (MUST_SET_META_DOWN_FROM_WINDOWS_KEY_EVENTS) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_WINDOWS) {
                    final int eventId = keyEvent.getID();
                    if (eventId == KeyEvent.KEY_PRESSED) {
                        commonState.setMetaDown(true);
                    } else if (eventId == KeyEvent.KEY_RELEASED) {
                        commonState.setMetaDown(false);
                    }
                }
            }
        }
        
        if (backingEvent instanceof MouseEvent) {
            final MouseEvent mouseEvent = (MouseEvent) backingEvent;
            
            this.tryUpdateMousePosInScreenInOs(mouseEvent, commonState);
            
            final int modEx = mouseEvent.getModifiersEx();
            commonState.setPrimaryButtonDown((modEx & MouseEvent.BUTTON1_DOWN_MASK) != 0);
            commonState.setMiddleButtonDown((modEx & MouseEvent.BUTTON2_DOWN_MASK) != 0);
            commonState.setSecondaryButtonDown((modEx & MouseEvent.BUTTON3_DOWN_MASK) != 0);
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
        return keyChar;
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
        
        /*
         * Even though an int would suffice for us, we need to rely on
         * getPreciseWheelRotation(), and not (only) on getWheelRotation(),
         * for two reasons:
         * - TODO awt TODO mac On Mac when moving two fingers
         *   together on the touch pad, WHEEL_UNIT_SCROLL events generated
         *   when removing fingers have getWheelRotation() equal to 1,
         *   but getPreciseWheelRotation() equal to +-0.0.
         *   To avoid undesired scroll when removing fingers,
         *   we must use zero wheel rotation whenever getPreciseWheelRotation()
         *   is positive or negative zero.
         * - TODO awt On Windows at least, sometimes when changing wheel
         *   turning direction, getWheelRotation() is zero,
         *   while getPreciseWheelRotation() is +-1.0.
         * What we do:
         * - We primarily rely on getPreciseWheelRotation(), as it seems
         *   more well behaved, unless it's not available (JDK < 7).
         * - Still, other than on Mac, if getPreciseWheelRotation() is +-0.0,
         *   to avoid the risk of missing a roll, we fall back to using
         *   getWheelRotation().
         */
        
        final double backingPreciseWheelRotation;
        if (PRECISE_WHEEL_ROTATION_AVAILABLE) {
            backingPreciseWheelRotation = mouseWheelEvent.getPreciseWheelRotation();
        } else {
            backingPreciseWheelRotation = mouseWheelEvent.getWheelRotation();
        }
        
        final int wheelRotation;
        if (backingPreciseWheelRotation == 0.0) {
            if (OsUtils.isMac()) {
                wheelRotation = 0;
            } else {
                final int backingWheelRotation = mouseWheelEvent.getWheelRotation();
                wheelRotation = backingWheelRotation;
            }
        } else {
            final int backingPreciseWheelRotationRounded =
                    NbrsUtils.roundToInt(backingPreciseWheelRotation);
            if (OsUtils.isMac()
                    && mouseWheelEvent.isShiftDown()) {
                /*
                 * TODO awt TODO mac On Mac, at least when moving two fingers
                 * together on the touch pad, some WHEEL_UNIT_SCROLL events
                 * have opposite rotation value AND "shift down" flag,
                 * which must be a weird bug since shift is never down then.
                 * This is a best effort workaround.
                 */
                wheelRotation = -backingPreciseWheelRotationRounded;
            } else {
                wheelRotation = backingPreciseWheelRotationRounded;
            }
        }
        
        return computeBwdRollAmount(wheelRotation);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int computeBwdRollAmount(int wheelRotation) {
        /*
         * TODO awt Maybe we should just use signum,
         * as done for some other bindings,
         * or use precise (fp) rotation.
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
    
    /**
     * @param mouseEvent (in)
     * @param commonState (in,out)
     */
    private void tryUpdateMousePosInScreenInOs(
        MouseEvent mouseEvent,
        CmnInputConvState commonState) {
        
        final GPoint mousePosInScreenInOs;
        if (mouseEvent instanceof MouseWheelEvent) {
            /*
             * TODO awt For these events, getXOnScreen() and getYOnScreen()
             * always return zero, so we have to compute that.
             */
            
            final InterfaceBwdHostImpl host = this.getHost();
            final GRect clientBoundsInOs = host.getClientBoundsInOs();
            if (clientBoundsInOs.isEmpty()) {
                /*
                 * Invalid client bounds:
                 * can't compute position in screen.
                 */
                mousePosInScreenInOs = null;
            } else {
                int xInClientInOs = mouseEvent.getX();
                int yInClientInOs = mouseEvent.getY();
                
                final AWTEvent awtEvent = mouseEvent;
                final Object source = awtEvent.getSource();
                if (source instanceof Window) {
                    /*
                     * Not coming from a content pane.
                     * Useful for AWT, for which we can't get events to come
                     * from a content pane as with Swing.
                     */
                    final Window window = (Window) source;
                    final Insets insets = window.getInsets();
                    xInClientInOs -= insets.left;
                    yInClientInOs -= insets.top;
                }
                
                mousePosInScreenInOs = GPoint.valueOf(
                    clientBoundsInOs.x() + xInClientInOs,
                    clientBoundsInOs.y() + yInClientInOs);
            }
        } else {
            mousePosInScreenInOs = GPoint.valueOf(
                mouseEvent.getXOnScreen(),
                mouseEvent.getYOnScreen());
        }
        
        if (mousePosInScreenInOs != null) {
            commonState.setMousePosInScreenInOs(mousePosInScreenInOs);
        }
    }
}
