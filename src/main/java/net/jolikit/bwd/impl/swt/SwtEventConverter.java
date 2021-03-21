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
package net.jolikit.bwd.impl.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import net.jolikit.bwd.api.events.BwdKeyLocations;
import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.basics.InterfaceBwdHostInOs;
import net.jolikit.bwd.impl.utils.events.AbstractEventConverter;
import net.jolikit.bwd.impl.utils.events.CmnInputConvState;
import net.jolikit.lang.Dbg;

/**
 * For key events: KeyDown, KeyUp
 * For mouse events: MouseDown, MouseUp, MouseMove, MouseEnter, MouseExit
 * For wheel events: MouseVerticalWheel, MouseHorizontalWheel
 */
public class SwtEventConverter extends AbstractEventConverter {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final int SWT_BUTTON_PRIMARY = 1;
    private static final int SWT_BUTTON_MIDDLE = 2;
    private static final int SWT_BUTTON_SECONDARY = 3;
    
    private final SwtKeyConverter keyConverter = new SwtKeyConverter();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SwtEventConverter(
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
        final InterfaceBwdHostInOs host = this.getHost();
        
        final Event event = (Event) backingEvent;
        
        {
            final GRect clientBoundsInOs = host.getClientBoundsInOs();
            if (!clientBoundsInOs.isEmpty()) {
                // Position in event is valid
                // even if not a mouse event.
                final GPoint mousePosInScreenInOs = GPoint.valueOf(
                    clientBoundsInOs.x() + event.x,
                    clientBoundsInOs.y() + event.y);
                commonState.setMousePosInScreenInOs(mousePosInScreenInOs);
            }
        }
        
        commonState.setPrimaryButtonDown(isButtonDown(event, SWT_BUTTON_PRIMARY, SWT.BUTTON1));
        commonState.setMiddleButtonDown(isButtonDown(event, SWT_BUTTON_MIDDLE, SWT.BUTTON2));
        commonState.setSecondaryButtonDown(isButtonDown(event, SWT_BUTTON_SECONDARY, SWT.BUTTON3));
        
        commonState.setShiftDown((event.stateMask & SWT.SHIFT) != 0);
        commonState.setControlDown((event.stateMask & SWT.CONTROL) != 0);
        commonState.setAltDown((event.stateMask & SWT.ALT) != 0);
        commonState.setMetaDown((event.stateMask & SWT.COMMAND) != 0);
    }
    
    /*
     * Key events.
     */
    
    @Override
    protected int getKey(Object backingEvent) {
        final Event event = (Event) backingEvent;
        
        if (DEBUG) {
            Dbg.log("");
            Dbg.log("swt key event : " + event);
            Dbg.log("swt key event : keyCode = " + event.keyCode);
        }
        
        /*
         * event.character:
         * "depending on the event, the character represented by the key
         * that was typed.  This is the final character that results
         * after all modifiers have been applied.  For example, when the
         * user types Control+A, the character value is 0x01 (ASCII SOH).
         * It is important that applications do not attempt to modify the
         * character value based on a stateMask (such as SWT.CTRL) or the
         * resulting character will not be correct."
         * 
         * event.keyCode:
         * "depending on the event, the key code of the key that was typed,
         * as defined by the key code constants in class <code>SWT</code>.
         * When the character field of the event is ambiguous, this field
         * contains the unaffected value of the original character.  For
         * example, typing Control+M or Enter both result in the character '\r'
         * but the keyCode field will also contain '\r' when Enter was typed
         * and 'm' when Control+M was typed."
         */
        final int backingKey = event.keyCode;
        
        // SWT.LEFT, SWT.RIGHT, SWT.KEYPAD, or SWT.NONE.
        final int keyLocation = event.keyLocation;
        
        int key = this.keyConverter.get(backingKey);
        
        /*
         * TODO swt SWT doesn't have an ALT_GRAPH key,
         * and our converter always converts SWT.ALT into BwdKeys.ALT.
         * As a result, if backing key location is SWT.RIGHT,
         * we use BwdKeys.ALT_GRAPH instead of BwdKeys.ALT.
         */
        if ((key == BwdKeys.ALT)
                && (keyLocation == SWT.RIGHT)) {
            key = BwdKeys.ALT_GRAPH;
        }
        
        return key;
    }

    @Override
    protected int getKeyLocation(Object backingEvent) {
        final Event event = (Event) backingEvent;
        
        final int backingKey = event.keyCode;
        if (backingKey == SWT.NONE) {
            // Not a key, better not try to read its location.
            return BwdKeyLocations.NO_STATEMENT;
        }
        
        /*
         * TODO swt For shift key, location doesn't work
         * on key released event (but does well for control key).
         */
        
        // SWT.LEFT, SWT.RIGHT, SWT.KEYPAD, or SWT.NONE (key is nowhere!).
        final int backingKeyLocation = event.keyLocation;
        switch (backingKeyLocation) {
        case SWT.LEFT:
            return BwdKeyLocations.LEFT;
        case SWT.RIGHT:
            return BwdKeyLocations.RIGHT;
        case SWT.KEYPAD:
            return BwdKeyLocations.NUMPAD;
        default:
            return BwdKeyLocations.NO_STATEMENT;
        }
    }

    @Override
    protected int getCodePoint(Object backingEvent) {
        final Event event = (Event) backingEvent;
        // TODO swt Limited to BMP.
        final char keyChar = event.character;
        return (int) keyChar;
    }

    /*
     * Mouse events.
     */
    
    @Override
    protected int getButton(Object backingEvent) {
        final Event event = (Event) backingEvent;
        return computeButton(event);
    }

    /*
     * Wheel events.
     */
    
    @Override
    protected int getWheelXRoll(Object backingEvent) {
        final Event event = (Event) backingEvent;
        final boolean isHorizontalWheel = (event.type == SWT.MouseHorizontalWheel);
        final int xRoll;
        if (isHorizontalWheel) {
            // TODO swt Sign here? (I don't have a horizontal wheel).
            // For now using negative sign, as done for vertical wheel.
            xRoll = -computeBwdRollAmount(event.count);
        } else {
            xRoll = 0;
        }
        return xRoll;
    }

    @Override
    protected int getWheelYRoll(Object backingEvent) {
        final Event event = (Event) backingEvent;
        final boolean isVerticalWheel = (event.type == SWT.MouseVerticalWheel);
        final int yRoll;
        if (isVerticalWheel) {
            yRoll = -computeBwdRollAmount(event.count);
        } else {
            yRoll = 0;
        }
        return yRoll;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int computeBwdRollAmount(int count) {
        /*
         * TODO swt Maybe we should accumulate.
         * 
         * Count is 3 per wheel "click" on dev platform.
         * We just want to use 1 per wheel "click".
         */
        return (int) Math.signum(count);
    }
    
    /*
     * 
     */

    private static int computeButton(Event event) {
        final int backingButton = event.button;
        switch (backingButton) {
        case SWT_BUTTON_PRIMARY: return BwdMouseButtons.PRIMARY;
        case SWT_BUTTON_MIDDLE: return BwdMouseButtons.MIDDLE;
        case SWT_BUTTON_SECONDARY: return BwdMouseButtons.SECONDARY;
        default:
            return BwdMouseButtons.NO_STATEMENT;
        }
    }
    
    private static boolean isButtonDown(Event event, int button, int buttonMask) {
        final boolean down;
        if (event.button == button) {
            if (event.type == SWT.MouseDown) {
                /*
                 * TODO swt When pressing a button,
                 * stateMask still indicates it as not down,
                 * so we ignore it.
                 */
                down = true;
            } else if (event.type == SWT.MouseUp) {
                /*
                 * TODO swt When releasing a button,
                 * stateMask still indicates it as down,
                 * so we ignore it.
                 */
                down = false;
            } else {
                down = ((event.stateMask & buttonMask) != 0);
            }
        } else {
            down = ((event.stateMask & buttonMask) != 0);
        }
        return down;
    }
}
