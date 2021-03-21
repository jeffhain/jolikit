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
package net.jolikit.bwd.impl.lwjgl3;

import org.lwjgl.glfw.GLFW;

import net.jolikit.bwd.api.events.BwdKeyLocations;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglCharEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglCharModsEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglCursorEnterEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglCursorPosEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglKeyEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglMouseButtonEvent;
import net.jolikit.bwd.impl.lwjgl3.LwjglEvents.LwjglScrollEvent;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.events.AbstractEventConverter;
import net.jolikit.bwd.impl.utils.events.CmnInputConvState;
import net.jolikit.lang.Dbg;

/**
 * For key events: LwjKeyEvent, LwjCharEvent, LwjCharModsEvent 
 * For mouse events: LwjCursorPosEvent, LwjCursorEnterEvent, LwjMouseButtonEvent
 * For wheel events: LwjScrollEvent
 */
public class LwjglEventConverter extends AbstractEventConverter {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final LwjglKeyConverter keyConverter = new LwjglKeyConverter();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public LwjglEventConverter(
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
        
        final AbstractBwdHost host = (AbstractBwdHost) this.getHost();
        if (host.isClosed_nonVolatile()) {
            // Aborting to avoid using possibly disposed backing window.
            if (DEBUG) {
                Dbg.logPr(this, "updateFromBackingEvent() : host closed, aborting");
            }
            return;
        }
        
        final CmnInputConvState commonState = this.getCommonState();
        
        {
            /*
             * TODO lwjgl For some reason CONTROL and SUPER keys are not considered
             * modifiers (don't cause generation of a CharModsEvent event as SHIFT does).
             * As a result, for consistency, we just compute all modifiers here from
             * current keyboard state.
             */
            
            final long window = (Long) host.getBackingWindow();
            commonState.setShiftDown(
                    computeKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || computeKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT));
            commonState.setControlDown(
                    computeKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || computeKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL));
            commonState.setAltDown(
                    computeKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT));
            commonState.setAltGraphDown(
                    computeKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT));
            // Best effort.
            commonState.setMetaDown(
                    computeKeyDown(window, GLFW.GLFW_KEY_LEFT_SUPER)
                    || computeKeyDown(window, GLFW.GLFW_KEY_RIGHT_SUPER));
        }
        
        if (backingEvent instanceof LwjglCharModsEvent) {
            @SuppressWarnings("unused")
            final LwjglCharModsEvent event = (LwjglCharModsEvent) backingEvent;
        }
        
        if (backingEvent instanceof LwjglMouseButtonEvent) {
            final LwjglMouseButtonEvent event = (LwjglMouseButtonEvent) backingEvent;
            
            final int button = event.button;
            final int action = event.action;
            final boolean buttonDown = (action == GLFW.GLFW_PRESS);
            
            /*
             * TODO lwjgl Issue due to not having a API
             * to read which buttons are down:
             * If moving the mouse with buttons down,
             * and then loosing focus (for example due to alt+tab),
             * mouse button released events get generated
             * (but no mouse button pressed event when gaining focus),
             * causing buttons not to properly appear down once focus
             * is gained back.
             */

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                commonState.setPrimaryButtonDown(buttonDown);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                commonState.setMiddleButtonDown(buttonDown);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                commonState.setSecondaryButtonDown(buttonDown);
            }
        }
        
        if (backingEvent instanceof LwjglCursorEnterEvent) {
            @SuppressWarnings("unused")
            final LwjglCursorEnterEvent event = (LwjglCursorEnterEvent) backingEvent;
        }
        
        if (backingEvent instanceof LwjglCursorPosEvent) {
            final LwjglCursorPosEvent event = (LwjglCursorPosEvent) backingEvent;
            final int mouseXInClientInOs = BindingCoordsUtils.roundToInt(event.xpos);
            final int mouseYInClientInOs = BindingCoordsUtils.roundToInt(event.ypos);
            
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
        final LwjglKeyEvent event = (LwjglKeyEvent) backingEvent;
        
        final int backingKey = getBackingKey(event);
        
        if (DEBUG) {
            final int scancode = event.scancode;
            Dbg.log("");
            Dbg.log("lwjgl key event : key = " + backingKey + ", scancode = " + scancode);
        }
        
        return this.keyConverter.get(backingKey);
    }

    @Override
    protected int getKeyLocation(Object backingEvent) {
        final LwjglKeyEvent event = (LwjglKeyEvent) backingEvent;

        final int backingKey = event.key;

        switch (backingKey) {
        case GLFW.GLFW_KEY_LEFT_SHIFT:
        case GLFW.GLFW_KEY_LEFT_CONTROL:
        case GLFW.GLFW_KEY_LEFT_ALT:
        case GLFW.GLFW_KEY_LEFT_SUPER:
            //
            return BwdKeyLocations.LEFT;
            //
        case GLFW.GLFW_KEY_RIGHT_SHIFT:
        case GLFW.GLFW_KEY_RIGHT_CONTROL:
        case GLFW.GLFW_KEY_RIGHT_ALT:
        case GLFW.GLFW_KEY_RIGHT_SUPER:
            //
            return BwdKeyLocations.RIGHT;
            //
        case GLFW.GLFW_KEY_KP_0:
        case GLFW.GLFW_KEY_KP_1:
        case GLFW.GLFW_KEY_KP_2:
        case GLFW.GLFW_KEY_KP_3:
        case GLFW.GLFW_KEY_KP_4:
        case GLFW.GLFW_KEY_KP_5:
        case GLFW.GLFW_KEY_KP_6:
        case GLFW.GLFW_KEY_KP_7:
        case GLFW.GLFW_KEY_KP_8:
        case GLFW.GLFW_KEY_KP_9:
            //
        case GLFW.GLFW_KEY_KP_ADD:
        case GLFW.GLFW_KEY_KP_DECIMAL:
        case GLFW.GLFW_KEY_KP_DIVIDE:
        case GLFW.GLFW_KEY_KP_ENTER:
        case GLFW.GLFW_KEY_KP_EQUAL:
        case GLFW.GLFW_KEY_KP_MULTIPLY:
        case GLFW.GLFW_KEY_KP_SUBTRACT:
            //
            return BwdKeyLocations.NUMPAD;
        default:
            return BwdKeyLocations.NO_STATEMENT;
        }
    }

    @Override
    protected int getCodePoint(Object backingEvent) {
        final int codePoint;
        if (backingEvent instanceof LwjglCharEvent) {
            final LwjglCharEvent event = (LwjglCharEvent) backingEvent;
            codePoint = event.codepoint;
        } else {
            final LwjglCharModsEvent event = (LwjglCharModsEvent) backingEvent;
            codePoint = event.codepoint;
        }
        return codePoint;
    }

    /*
     * Mouse events.
     */
    
    @Override
    protected int getButton(Object backingEvent) {
        if (backingEvent instanceof LwjglMouseButtonEvent) {
            final LwjglMouseButtonEvent event = (LwjglMouseButtonEvent) backingEvent;
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
        final LwjglScrollEvent event = (LwjglScrollEvent) backingEvent;
        return computeBwdRollAmount(event.xoffset);
    }

    @Override
    protected int getWheelYRoll(Object backingEvent) {
        final LwjglScrollEvent event = (LwjglScrollEvent) backingEvent;
        return computeBwdRollAmount(event.yoffset);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static boolean computeKeyDown(long window, int key) {
        return (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS);
    }
    
    private static int computeButton(LwjglMouseButtonEvent event) {
        final int backingButton = event.button;
        switch (backingButton) {
        case GLFW.GLFW_MOUSE_BUTTON_LEFT: return BwdMouseButtons.PRIMARY;
        case GLFW.GLFW_MOUSE_BUTTON_MIDDLE: return BwdMouseButtons.MIDDLE;
        case GLFW.GLFW_MOUSE_BUTTON_RIGHT: return BwdMouseButtons.SECONDARY;
        default:
            return BwdMouseButtons.NO_STATEMENT;
        }
    }
    
    private static int computeBwdRollAmount(double offset) {
        /*
         * TODO lwjgl Maybe we should accumulate.
         */
        return (int) Math.signum(-offset);
    }
    
    private static int getBackingKey(LwjglKeyEvent event) {
        
        /*
         * Using scan code should yield different values even for keys
         * unknown to the backing library, but it's hard to figure out
         * to what key the correspond, so we use virtual key code instead.
         */
        final boolean mustUseVirtualKeyCode = true;
        
        final int backingKey;
        if (mustUseVirtualKeyCode) {
            backingKey = event.key;
        } else {
            /*
             * TODO lwjgl No official "undefined" value for these scan codes,
             * but 0 is used on synthetic key release events generated by lwjgl
             * on focus loss.
             * 0 could theoretically be a valid value, but it's a common error value
             * for scan codes, so we could assume it's an invalid value.
             */
            backingKey = event.scancode;
        }

        return backingKey;
    }
}
