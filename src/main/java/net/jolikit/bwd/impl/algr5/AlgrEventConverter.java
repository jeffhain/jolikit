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
package net.jolikit.bwd.impl.algr5;

import net.jolikit.bwd.api.events.BwdKeyLocations;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_KEYBOARD_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_KEYBOARD_STATE;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_MOUSE_EVENT;
import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_MOUSE_STATE;
import net.jolikit.bwd.impl.algr5.jlib.AlgrEventType;
import net.jolikit.bwd.impl.algr5.jlib.AlgrJnaLib;
import net.jolikit.bwd.impl.algr5.jlib.AlgrKey;
import net.jolikit.bwd.impl.algr5.jlib.AlgrKeymod;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.basics.PixelCoordsConverter;
import net.jolikit.bwd.impl.utils.events.AbstractEventConverter;
import net.jolikit.bwd.impl.utils.events.CmnInputConvState;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * For key events: ALLEGRO_KEYBOARD_EVENT
 * For mouse events: ALLEGRO_MOUSE_EVENT
 * For wheel events: ALLEGRO_MOUSE_EVENT
 */
public class AlgrEventConverter extends AbstractEventConverter {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final AlgrJnaLib LIB = AlgrJnaLib.INSTANCE;
    
    private final AlgrKeyConverter keyConverter = new AlgrKeyConverter();
    
    private final PixelCoordsConverter pixelCoordsConverter;
    
    /*
     * Temps.
     */
    
    private final ALLEGRO_MOUSE_STATE tmpMouseState = new ALLEGRO_MOUSE_STATE();
    
    private final ALLEGRO_KEYBOARD_STATE tmpKeyboardState = new ALLEGRO_KEYBOARD_STATE();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AlgrEventConverter(
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
        
        {
            final ALLEGRO_MOUSE_STATE mouseState = this.tmpMouseState;
            LIB.al_get_mouse_state(mouseState);
            
            commonState.setPrimaryButtonDown((mouseState.buttons & AlgrJnaLib.ALGR_PRIMARY_MOUSE_BUTTON_MASK) != 0);
            commonState.setMiddleButtonDown((mouseState.buttons & AlgrJnaLib.ALGR_MIDDLE_MOUSE_BUTTON_MASK) != 0);
            commonState.setSecondaryButtonDown((mouseState.buttons & AlgrJnaLib.ALGR_SECONDARY_MOUSE_BUTTON_MASK) != 0);
        }
        
        /*
         * TODO algr ALLEGRO_KEYBOARD_EVENT modifiers seem to only work
         * for KEY_CHAR events, not for KEY_DOWN and KEY_UP events,
         * so for these we need to read keyboard state.
         * For simplicity and consistency, we just read down keys from
         * keyboard state all the time, whatever the event type.
         */
        
        final boolean mustComputeKeyDownFromKeyboardStateAndForAllEvents = true;
        
        {
            final boolean mustComputeKeyDown;
            final boolean mustUseKeyboardStateForKeyDownComputation;
            if (mustComputeKeyDownFromKeyboardStateAndForAllEvents) {
                mustComputeKeyDown = true;
                mustUseKeyboardStateForKeyDownComputation = true;
            } else {
                if (backingEvent instanceof ALLEGRO_KEYBOARD_EVENT) {
                    mustComputeKeyDown = true;
                    
                    final ALLEGRO_KEYBOARD_EVENT event = (ALLEGRO_KEYBOARD_EVENT) backingEvent;
                    final AlgrEventType eventType = AlgrEventType.valueOf(event.type);
                    final boolean canRelyOnEventModifiers =
                            (eventType == AlgrEventType.ALLEGRO_EVENT_KEY_CHAR);
                    mustUseKeyboardStateForKeyDownComputation = (!canRelyOnEventModifiers);
                } else {
                    mustComputeKeyDown = false;
                    mustUseKeyboardStateForKeyDownComputation = false;
                }
            }
            
            if (mustComputeKeyDown) {
                if (mustUseKeyboardStateForKeyDownComputation) {
                    final ALLEGRO_KEYBOARD_STATE keyboardState = this.tmpKeyboardState;
                    LIB.al_get_keyboard_state(keyboardState);
                    
                    commonState.setShiftDown(
                            LIB.al_key_down(keyboardState, AlgrKey.LSHIFT.intValue())
                            || LIB.al_key_down(keyboardState, AlgrKey.RSHIFT.intValue()));
                    commonState.setControlDown(
                            LIB.al_key_down(keyboardState, AlgrKey.LCTRL.intValue())
                            || LIB.al_key_down(keyboardState, AlgrKey.RCTRL.intValue()));
                    commonState.setAltDown(
                            LIB.al_key_down(keyboardState, AlgrKey.ALT.intValue()));
                    commonState.setAltGraphDown(
                            LIB.al_key_down(keyboardState, AlgrKey.ALTGR.intValue()));
                    commonState.setMetaDown(
                            LIB.al_key_down(keyboardState, AlgrKey.MENU.intValue()));
                } else {
                    final ALLEGRO_KEYBOARD_EVENT event = (ALLEGRO_KEYBOARD_EVENT) backingEvent;
                    final int modifiers = event.modifiers;
                    commonState.setShiftDown((modifiers & AlgrKeymod.ALLEGRO_KEYMOD_SHIFT.intValue()) != 0);
                    commonState.setControlDown((modifiers & AlgrKeymod.ALLEGRO_KEYMOD_CTRL.intValue()) != 0);
                    commonState.setAltDown((modifiers & AlgrKeymod.ALLEGRO_KEYMOD_ALT.intValue()) != 0);
                    commonState.setAltGraphDown((modifiers & AlgrKeymod.ALLEGRO_KEYMOD_ALTGR.intValue()) != 0);
                    commonState.setMetaDown((modifiers & AlgrKeymod.ALLEGRO_KEYMOD_MENU.intValue()) != 0);
                }
            }
        }
        
        /*
         * Mouse position.
         */
        
        if (backingEvent instanceof ALLEGRO_MOUSE_EVENT) {
            final ALLEGRO_MOUSE_EVENT event = (ALLEGRO_MOUSE_EVENT) backingEvent;
            
            if (DEBUG) {
                Dbg.logPr(this, "ALLEGRO_MOUSE_EVENT : in client, in device pixels : (" + event.x + ", " + event.y +  ")");
            }
            
            /*
             * TODO algr Could instead retrieve mouse position in client
             * from the mouse state read with al_get_mouse_state(...),
             * but we prefer to retrieve it from mouse events,
             * to make sure we don't use position in the client
             * of another host in case of focus switch.
             */
            
            final int mouseXInClientInOs = this.pixelCoordsConverter.computeXInOsPixel(event.x);
            final int mouseYInClientInOs = this.pixelCoordsConverter.computeYInOsPixel(event.y);
            if (DEBUG) {
                Dbg.logPr(this, "ALLEGRO_MOUSE_EVENT : in client in OS : (" + mouseXInClientInOs + ", " + mouseYInClientInOs +  ")");
            }
            
            final AbstractBwdHost host = (AbstractBwdHost) this.getHost();
            final GRect clientBoundsInOs = host.getClientBoundsInOs();
            if (!clientBoundsInOs.isEmpty()) {
                if (DEBUG) {
                    Dbg.logPr(this, "ALLEGRO_MOUSE_EVENT : clientBoundsInOs = " + clientBoundsInOs);
                }
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
        final ALLEGRO_KEYBOARD_EVENT event = (ALLEGRO_KEYBOARD_EVENT) backingEvent;
        
        if (DEBUG) {
            Dbg.log("");
            Dbg.log("algr key event : " + event);
        }

        final int backingKeyCode = event.keycode;
        final AlgrKey backingKey = AlgrKey.valueOf(backingKeyCode);
        
        return this.keyConverter.get(backingKey);
    }

    @Override
    protected int getKeyLocation(Object backingEvent) {
        final ALLEGRO_KEYBOARD_EVENT event = (ALLEGRO_KEYBOARD_EVENT) backingEvent;
        
        final int backingKeyCode = event.keycode;
        final AlgrKey backingKey = AlgrKey.valueOf(backingKeyCode);
        if (backingKey == null) {
            return BwdKeyLocations.NO_STATEMENT;
        }

        switch (backingKey) {
        case LSHIFT:
        case LCTRL:
        case LWIN:
            //
            return BwdKeyLocations.LEFT;
            //
        case RSHIFT:
        case RCTRL:
        case RWIN:
            //
            return BwdKeyLocations.RIGHT;
            //
        case PAD_0:
        case PAD_1:
        case PAD_2:
        case PAD_3:
        case PAD_4:
        case PAD_5:
        case PAD_6:
        case PAD_7:
        case PAD_8:
        case PAD_9:
            //
        case PAD_SLASH:
        case PAD_ASTERISK:
        case PAD_MINUS:
        case PAD_PLUS:
        case PAD_DELETE:
        case PAD_ENTER:
            //
        case DPAD_CENTER:
        case DPAD_UP:
        case DPAD_DOWN:
        case DPAD_LEFT:
        case DPAD_RIGHT:
            //
            return BwdKeyLocations.NUMPAD;
        default:
            return BwdKeyLocations.NO_STATEMENT;
        }
    }

    @Override
    protected int getCodePoint(Object backingEvent) {
        final ALLEGRO_KEYBOARD_EVENT event = (ALLEGRO_KEYBOARD_EVENT) backingEvent;
        final int unichar = event.unichar;
        final int codePoint;
        if (unichar < 0) {
            codePoint = 0;
        } else {
            codePoint = unichar;
        }
        return codePoint;
    }

    /*
     * Mouse events.
     */
    
    @Override
    protected int getButton(Object backingEvent) {
        final ALLEGRO_MOUSE_EVENT event = (ALLEGRO_MOUSE_EVENT) backingEvent;
        final int backingButton = event.button;
        switch (backingButton) {
        case AlgrJnaLib.ALGR_PRIMARY_MOUSE_BUTTON: return BwdMouseButtons.PRIMARY;
        case AlgrJnaLib.ALGR_MIDDLE_MOUSE_BUTTON: return BwdMouseButtons.MIDDLE;
        case AlgrJnaLib.ALGR_SECONDARY_MOUSE_BUTTON: return BwdMouseButtons.SECONDARY;
        default:
            return BwdMouseButtons.NO_STATEMENT;
        }
    }

    /*
     * Wheel events.
     */

    @Override
    protected int getWheelXRoll(Object backingEvent) {
        final ALLEGRO_MOUSE_EVENT event = (ALLEGRO_MOUSE_EVENT) backingEvent;
        /*
         * TODO algr I could not test "w" values (no 2D ball),
         * so here we assume that "w" behaves for "x" axis
         * as "z" does for "y" axis, i.e. that "w" decreases
         * when "x" increases (to the right), as "z" decreases
         * when "y" increases (going closer to user).
         */
        return -computeBwdRollAmount(event.dw);
    }

    @Override
    protected int getWheelYRoll(Object backingEvent) {
        final ALLEGRO_MOUSE_EVENT event = (ALLEGRO_MOUSE_EVENT) backingEvent;
        return -computeBwdRollAmount(event.dz);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static int computeBwdRollAmount(int delta) {
        /*
         * TODO algr Maybe we should accumulate.
         */
        return (int) Math.signum(delta);
    }
}
