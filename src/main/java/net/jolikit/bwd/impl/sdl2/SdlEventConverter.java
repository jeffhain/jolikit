/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.impl.sdl2;

import com.sun.jna.ptr.IntByReference;

import net.jolikit.bwd.api.events.BwdKeyLocations;
import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_KeyboardEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SdlKeycode;
import net.jolikit.bwd.impl.sdl2.jlib.SdlKeymod;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_Keysym;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_MouseButtonEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_MouseMotionEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SdlMouseWheelDirection;
import net.jolikit.bwd.impl.sdl2.jlib.SDL_MouseWheelEvent;
import net.jolikit.bwd.impl.sdl2.jlib.SdlJnaLib;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.events.AbstractEventConverter;
import net.jolikit.bwd.impl.utils.events.CmnInputConvState;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.lang.OsUtils;

/**
 * For key events: SDL_KeyboardEvent
 * For mouse events: SDL_MouseButtonEvent, SDL_MouseMotionEvent, SDL_WindowEvent (enter/exit)
 * For wheel events: SDL_MouseWheelEvent
 */
public class SdlEventConverter extends AbstractEventConverter {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * TODO sdl On Mac, can have backing mouse position in client
     * slightly outside client bounds, so we ensure it stays in.
     */
    private static final boolean MUST_ENSURE_MOUSE_POS_IN_CLIENT_STAYS_IN = OsUtils.isMac();
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final SdlJnaLib LIB = SdlJnaLib.INSTANCE;
    
    /**
     * TODO sdl SDL mixes the notion of key and of Unicode character,
     * storing (when it's done) "the" Unicode character in the
     * SDL_Keysym itself, so for example there is no way to get
     * an upper case 'A'.
     * To make up for it, we just upper-case letters in [a-z]
     * when shift is down.
     * 
     * Also, keysym.sym is either a code point (for SDL_Keycode values <= SDLK_z
     * and for SDLK_DELETE, plus cases not appearing in SDL_Keycode),
     * or some meaningless value, in which case there might still be
     * a code point to map to the key.
     * 
     * To compute code points, instead of re-coding some
     * big redundant switch-case, we first use our conversion
     * from SDL_Keysym to BWD key:
     * if there is a code point for the key, we use it
     * (modulo the shift trick for upper cases),
     * and if not, if the value is in [0,MAX_CP_HACK], we assume it's
     * a code point and just use it, else we use the specified
     * default (0).
     * 
     * Confining the hack to the [0,255] range should suffice
     * not to improperly consider forgotten or new keycode values
     * as code points.
     */
    private static final int MAX_CP_HACK = 255;
    
    private final SdlKeyConverter keyConverter = new SdlKeyConverter();
    
    /*
     * Temps.
     */
    
    private final IntByReference tmpIntRef1 = new IntByReference();
    private final IntByReference tmpIntRef2 = new IntByReference();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SdlEventConverter(
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
        
        {
            /*
             * TODO sdl Mouse buttons that are down can be retrieved from the
             * "state" field of SDL_MouseMotionEvent, but the "state" field
             * of SDL_MouseButtonEvent seems to always be "1".
             * 
             * To make sure we have these values up to date for all events,
             * and in a homogeneous way, we just always retrieve them,
             * from global state.
             * 
             * We also use this SDL_GetGlobalMouseState(...) call
             * to get mouse position in screen.
             */
            
            final IntByReference mouseXInScreen = this.tmpIntRef1;
            final IntByReference mouseYInScreen = this.tmpIntRef2;
            final int buttonsBits = LIB.SDL_GetGlobalMouseState(
                    mouseXInScreen,
                    mouseYInScreen);
            
            final GPoint mousePosInScreen = GPoint.valueOf(
                    mouseXInScreen.getValue(),
                    mouseYInScreen.getValue());
            commonState.setMousePosInScreen(mousePosInScreen);
            
            commonState.setPrimaryButtonDown(computeIsPrimaryButtonDown(buttonsBits));
            commonState.setMiddleButtonDown(computeIsMiddleButtonDown(buttonsBits));
            commonState.setSecondaryButtonDown(computeIsSecondaryButtonDown(buttonsBits));
        }
        
        if (backingEvent instanceof SDL_KeyboardEvent) {
            final SDL_KeyboardEvent event = (SDL_KeyboardEvent) backingEvent;
            
            final SDL_Keysym keysym = event.keysym;
            commonState.setShiftDown(computeIsShiftDown(keysym));
            commonState.setControlDown(computeIsControlDown(keysym));
            commonState.setAltDown(computeIsAltDown(keysym));
            commonState.setMetaDown(computeIsMetaDown(keysym));
        }

        /*
         * Updating host-specific state.
         */
        
        if (backingEvent instanceof SDL_MouseMotionEvent) {
            final SDL_MouseMotionEvent event = (SDL_MouseMotionEvent) backingEvent;
            
            int xInClient = event.x;
            int yInClient = event.y;
            
            if (MUST_ENSURE_MOUSE_POS_IN_CLIENT_STAYS_IN) {
                final GRect clientBounds = this.getHost().getClientBounds();
                if (!clientBounds.isEmpty()) {
                    xInClient = NbrsUtils.toRange(0, clientBounds.xSpan(), xInClient);
                    yInClient = NbrsUtils.toRange(0, clientBounds.ySpan(), yInClient);
                }
            }
            
            final GPoint mousePosInClient = GPoint.valueOf(
                    xInClient,
                    yInClient);
            this.setMousePosInClient(mousePosInClient);
            
        } else if (false) {
            /*
             * Not doing that, because it can compute positions
             * (slightly) outside client area.
             */
            final GRect clientBounds = this.getHost().getClientBounds();
            if (!clientBounds.isEmpty()) {
                final GPoint mousePosInScreen = commonState.getMousePosInScreen();
                final GPoint mousePosInClient = GPoint.valueOf(
                        mousePosInScreen.x() - clientBounds.x(),
                        mousePosInScreen.y() - clientBounds.y());
                this.setMousePosInClient(mousePosInClient);
            }
        }
    }
    
    /*
     * Key events.
     */
    
    @Override
    protected int getKey(Object backingEvent) {
        final SDL_KeyboardEvent event = (SDL_KeyboardEvent) backingEvent;
        
        final SDL_Keysym keysym = event.keysym;
        
        if (DEBUG) {
            Dbg.log("");
            Dbg.log("sdl keysym : " + keysym);
        }
        
        final SdlKeycode backingKey = getBackingKey(keysym);
        if (backingKey == null) {
            /*
             * Orphan keycode (see SdlKeyConverter Javadoc).
             * We assume it's a code point, and if it's also
             * the code point corresponding to a BWD key,
             * we return that key.
             */
            final int keycode = keysym.sym;
            final int codePoint = keycode;
            final int key = BwdKeys.keyForCodePoint(codePoint);
            if (key != BwdKeys.NO_STATEMENT) {
                return key;
            }
        }
        
        return this.keyConverter.get(backingKey);
    }

    @Override
    protected int getKeyLocation(Object backingEvent) {
        final SDL_KeyboardEvent event = (SDL_KeyboardEvent) backingEvent;
        final SDL_Keysym keysym = event.keysym;

        final SdlKeycode backingKey = getBackingKey(keysym);
        if (backingKey == null) {
            return BwdKeyLocations.NO_STATEMENT;
        }

        /*
         * TODO sdl Many scancodes starting with "SDL_SCANCODE_KP_"
         * (which we abbreviate as "SDKL_KP_"),
         * but with no "KP_"-less equivalent.
         * This might be related to the fact that SDL handles
         * keyboard that are split in two: the right part
         * is maybe be the "KP".
         * We assume that these scancodes are for keys on NUMPAD
         * only for digits.
         */

        switch (backingKey) {
        case SDLK_LSHIFT:
        case SDLK_LCTRL:
        case SDLK_LGUI:
            //
            return BwdKeyLocations.LEFT;
            //
        case SDLK_RSHIFT:
        case SDLK_RCTRL:
        case SDLK_RGUI:
            //
            return BwdKeyLocations.RIGHT;
            //
        case SDLK_KP_0:
        case SDLK_KP_1:
        case SDLK_KP_2:
        case SDLK_KP_3:
        case SDLK_KP_4:
        case SDLK_KP_5:
        case SDLK_KP_6:
        case SDLK_KP_7:
        case SDLK_KP_8:
        case SDLK_KP_9:
            //
            return BwdKeyLocations.NUMPAD;
            //
        default:
            return BwdKeyLocations.NO_STATEMENT;
        }
    }

    @Override
    protected int getCodePoint(Object backingEvent) {
        
        final int key = this.getKey(backingEvent);
        
        final SDL_KeyboardEvent event = (SDL_KeyboardEvent) backingEvent;
        final SDL_Keysym keysym = event.keysym;
        
        final int codePoint;
        if (key == BwdKeys.NO_STATEMENT) {
            // No actual BWD key for this value.
            final int keycode = keysym.sym;
            final boolean consideringThatKeyCodeIsACodePoint =
                    (keycode >= 0) && (keycode <= MAX_CP_HACK);
            if (consideringThatKeyCodeIsACodePoint) {
                codePoint = keycode;
            } else {
                codePoint = 0;
            }
        } else {
            final int cp = BwdKeys.codePointForKey(key);
            if (cp == -1) {
                codePoint = 0;
            } else {
                final boolean shiftDown = this.getCommonState().getShiftDown();
                final int lowerCaseCp = cp;
                if (shiftDown
                        && (lowerCaseCp >= BwdUnicode.a)
                        && (lowerCaseCp <= BwdUnicode.z)) {
                    codePoint = lowerCaseCp + (BwdUnicode.A - BwdUnicode.a);
                } else {
                    codePoint = lowerCaseCp;
                }
            }
        }
        return codePoint;
    }

    /*
     * Mouse events.
     */
    
    @Override
    protected int getButton(Object backingEvent) {
        if (backingEvent instanceof SDL_MouseButtonEvent) {
            final SDL_MouseButtonEvent event = (SDL_MouseButtonEvent) backingEvent;
            return computeButton(event);
        } else {
            return BwdMouseButtons.NO_STATEMENT;
        }
    }

    /*
     * Wheel events.
     */

    @Override
    protected int getWheelXRoll(Object backingEvent) {
        final SDL_MouseWheelEvent event = (SDL_MouseWheelEvent) backingEvent;
        
        final int direction = event.direction;
        
        // Floating point to avoid integer overflow.
        final double sign;
        if (direction == SdlMouseWheelDirection.SDL_MOUSEWHEEL_NORMAL.intValue()) {
            sign = -1.0;
        } else {
            sign = 1.0;
        }

        final int delta = event.x;
        return computeBwdRollAmount(sign, delta);
    }

    @Override
    protected int getWheelYRoll(Object backingEvent) {
        final SDL_MouseWheelEvent event = (SDL_MouseWheelEvent) backingEvent;
        
        final int direction = event.direction;
        
        // Floating point to avoid integer overflow.
        // Need to negate.
        final double sign;
        if (direction == SdlMouseWheelDirection.SDL_MOUSEWHEEL_NORMAL.intValue()) {
            sign = -1.0;
        } else {
            sign = 1.0;
        }

        final int delta = event.y;
        return computeBwdRollAmount(sign, delta);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int computeBwdRollAmount(double sign, int delta) {
        /*
         * TODO sdl Maybe we should just use signum,
         * as done for some other bindings.
         * 
         * Sign as double to avoid integer overflow.
         */
        return (int) (sign * delta);
    }
    
    /*
     * 
     */

    private static boolean computeIsShiftDown(SDL_Keysym keysym) {
        return (keysym.mod & SdlKeymod.KMOD_SHIFT.intValue()) != 0;
    }

    private static boolean computeIsControlDown(SDL_Keysym keysym) {
        return (keysym.mod & SdlKeymod.KMOD_CTRL.intValue()) != 0;
    }

    private static boolean computeIsAltDown(SDL_Keysym keysym) {
        return (keysym.mod & SdlKeymod.KMOD_ALT.intValue()) != 0;
    }

    private static boolean computeIsMetaDown(SDL_Keysym keysym) {
        /*
         * TODO sdl "often the Windows key"
         */
        return (keysym.mod & SdlKeymod.KMOD_GUI.intValue()) != 0;
    }
    
    /*
     * 
     */

    /**
     * @param buttonsBits As returned by SDL_GetGlobalMouseState(...).
     */
    private static boolean computeIsPrimaryButtonDown(int buttonsBits) {
        return (buttonsBits & SdlJnaLib.SDL_BUTTON_LMASK) != 0;
    }

    /**
     * @param buttonsBits As returned by SDL_GetGlobalMouseState(...).
     */
    private static boolean computeIsMiddleButtonDown(int buttonsBits) {
        return (buttonsBits & SdlJnaLib.SDL_BUTTON_MMASK) != 0;
    }

    /**
     * @param buttonsBits As returned by SDL_GetGlobalMouseState(...).
     */
    private static boolean computeIsSecondaryButtonDown(int buttonsBits) {
        return (buttonsBits & SdlJnaLib.SDL_BUTTON_RMASK) != 0;
    }
    
    /*
     * 
     */

    private static int computeButton(SDL_MouseButtonEvent event) {
        
        final int backingButton = event.button;
        
        switch (backingButton) {
        case SdlJnaLib.SDL_BUTTON_LEFT: return BwdMouseButtons.PRIMARY;
        case SdlJnaLib.SDL_BUTTON_MIDDLE: return BwdMouseButtons.MIDDLE;
        case SdlJnaLib.SDL_BUTTON_RIGHT: return BwdMouseButtons.SECONDARY;
        default:
            return BwdMouseButtons.NO_STATEMENT;
        }
    }
    
    private static SdlKeycode getBackingKey(SDL_Keysym keysym) {
        if (false) {
            // Doesn't follow keyboard layout, so we ignore it.
            final int scancode = keysym.scancode;
        }
        
        final int keycode = keysym.sym;
        
        return SdlKeycode.valueOf(keycode);
    }
}
