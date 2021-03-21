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
package net.jolikit.bwd.impl.jogl;

import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;

import net.jolikit.bwd.api.events.BwdKeyLocations;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.basics.PixelCoordsConverter;
import net.jolikit.bwd.impl.utils.events.AbstractEventConverter;
import net.jolikit.bwd.impl.utils.events.CmnInputConvState;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * For key events: KeyEvent
 * For mouse events: MouseEvent
 * For wheel events: MouseEvent
 */
public class JoglEventConverter extends AbstractEventConverter {

    /*
     * TODO jogl Taking any pointer into account,
     * not just the one of id 0.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final JoglKeyConverter keyConverter = new JoglKeyConverter();
    
    private final PixelCoordsConverter pixelCoordsConverter;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public JoglEventConverter(
            CmnInputConvState commonState,
            AbstractBwdHost host,
            PixelCoordsConverter pixelCoordsConverter) {
        super(
            commonState,
            host,
            host.getBindingConfig().getScaleHelper());
        this.pixelCoordsConverter = LangUtils.requireNonNull(pixelCoordsConverter);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void updateFromBackingEvent(Object backingEvent) {
        
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
          
            final int eventType = mouseEvent.getEventType();
            
            final boolean isPress = (eventType == MouseEvent.EVENT_MOUSE_PRESSED);
            final boolean isRelease = (eventType == MouseEvent.EVENT_MOUSE_RELEASED);
            final boolean isPressOrRelease = (isPress || isRelease);
            
            /*
             * TODO jogl Must take care not to use button if it's not either
             * a press or a release, because otherwise button can have funny values.
             * For example, in case of mouse wheel roll, button is set to
             * primary button (1), even if it's not pressed, and buttons down is [1],
             * and if wheel button (2) is pressed during the roll, button is 1 as well,
             * and buttons down is [1, 2].
             */
            if (isPressOrRelease) {
                final int button = mouseEvent.getButton();
                if (button == MouseEvent.BUTTON1) {
                    commonState.setPrimaryButtonDown(isPress);
                } else if (button == MouseEvent.BUTTON2) {
                    commonState.setMiddleButtonDown(isPress);
                } else if (button == MouseEvent.BUTTON3) {
                    commonState.setSecondaryButtonDown(isPress);
                }
            }
            
            final int mouseXInClientInDevice = mouseEvent.getX();
            final int mouseYInClientInDevice = mouseEvent.getY();

            final int mouseXInClientInOs =
                this.pixelCoordsConverter.computeXInOsPixel(
                    mouseXInClientInDevice);
            final int mouseYInClientInOs =
                this.pixelCoordsConverter.computeYInOsPixel(
                    mouseYInClientInDevice);
            
            final AbstractBwdHost host = (AbstractBwdHost) this.getHost();
            final GRect clientBoundsInOs = host.getClientBoundsInOs();
            if (!clientBoundsInOs.isEmpty()) {
                final GPoint mousePosInScreenInOs = GPoint.valueOf(
                    clientBoundsInOs.x() + mouseXInClientInOs,
                    clientBoundsInOs.y() + mouseYInClientInOs);
                commonState.setMousePosInScreenInOs(mousePosInScreenInOs);
            }
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
            Dbg.log("jogl key event : " + keyEvent);
        }

        final short backingKey = getBackingKey(keyEvent);

        return this.keyConverter.get(backingKey);
    }

    @Override
    protected int getKeyLocation(Object backingEvent) {
        final KeyEvent keyEvent = (KeyEvent) backingEvent;
        
        /*
         * TODO jogl Don't have much location info.
         */

        final int backingKey = getBackingKey(keyEvent);
        
        switch (backingKey) {
        // Only works when NUM_LOCK is on.
        case KeyEvent.VK_NUMPAD0:
        case KeyEvent.VK_NUMPAD1:
        case KeyEvent.VK_NUMPAD2:
        case KeyEvent.VK_NUMPAD3:
        case KeyEvent.VK_NUMPAD4:
        case KeyEvent.VK_NUMPAD5:
        case KeyEvent.VK_NUMPAD6:
        case KeyEvent.VK_NUMPAD7:
        case KeyEvent.VK_NUMPAD8:
        case KeyEvent.VK_NUMPAD9:
            //
            return BwdKeyLocations.NUMPAD;
        default:
            return BwdKeyLocations.NO_STATEMENT;
        }
    }

    @Override
    protected int getCodePoint(Object backingEvent) {
        final KeyEvent keyEvent = (KeyEvent) backingEvent;
        /*
         * TODO jogl Limited to BMP.
         */
        final char keyChar = keyEvent.getKeyChar();
        return (int) keyChar;
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
             * TODO jogl Need to do that, because mouse moved events
             * have last pressed button in them.
             */
            return BwdMouseButtons.NO_STATEMENT;
        }
    }

    /*
     * Wheel events.
     * 
     * TODO jogl Could also generate wheel events from GestureEvent.
     */

    @Override
    protected int getWheelXRoll(Object backingEvent) {
        final MouseEvent mouseEvent = (MouseEvent) backingEvent;
        final float[] rotationArr = mouseEvent.getRotation();
        // {x,y,z}
        final float rotation = rotationArr[0];
        final float scale = mouseEvent.getRotationScale();
        return computeBwdRollAmount(rotation, scale);
    }

    @Override
    protected int getWheelYRoll(Object backingEvent) {
        final MouseEvent mouseEvent = (MouseEvent) backingEvent;
        final float[] rotationArr = mouseEvent.getRotation();
        // {x,y,z}
        final float rotation = rotationArr[1];
        final float scale = mouseEvent.getRotationScale();
        return computeBwdRollAmount(rotation, scale);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static int computeBwdRollAmount(float rotation, float scale) {
        /*
         * TODO jogl Maybe we should accumulate.
         * 
         * x rotation is usually (*) < 0 when x increases (left),
         * and y rotation < 0 when y increases (down),
         * so we negate that.
         * (*) that's the opposite on some OSes.
         */
        final double sign = -1.0;
        return (int) BindingCoordsUtils.roundToInt(sign * rotation * scale);
    }
    
    private static boolean isMouseButtonEvent(MouseEvent event) {
        final int eventType = event.getEventType();
        return (eventType == MouseEvent.EVENT_MOUSE_PRESSED)
                || (eventType == MouseEvent.EVENT_MOUSE_RELEASED)
                || (eventType == MouseEvent.EVENT_MOUSE_CLICKED);
    }
    
    private static int computeButton(MouseEvent event) {
        final int backingButton = event.getButton();
        switch (backingButton) {
        case MouseEvent.BUTTON1: return BwdMouseButtons.PRIMARY;
        case MouseEvent.BUTTON2: return BwdMouseButtons.MIDDLE;
        case MouseEvent.BUTTON3: return BwdMouseButtons.SECONDARY;
        default:
            return BwdMouseButtons.NO_STATEMENT;
        }
    }
    
    private static short getBackingKey(KeyEvent keyEvent) {
        /*
         * "Returns the virtual key symbol reflecting the current keyboard layout.
         * For printable keys, the key symbol is the unmodified representation
         * of the UTF-16 key char.
         * E.g. symbol [VK_A, 'A'] for char 'a'."
         */
        final short keySymbol = keyEvent.getKeySymbol();
        
        final short backingKey;
        if (keySymbol == KeyEvent.VK_ALT) {
            /*
             * TODO jogl When pressing AltGraph, keySymbol is Alt.
             * Using keyCode to discriminate. 
             */
            backingKey = keyEvent.getKeyCode();
        } else {
            backingKey = keySymbol;
        }
        return backingKey;
    }
}
