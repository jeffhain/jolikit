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
package net.jolikit.bwd.impl.sdl2;

import java.util.Arrays;

import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.impl.sdl2.jlib.SdlKeycode;
import net.jolikit.bwd.impl.utils.events.AbstractKeyConverter;

/**
 * TODO sdl This converter might not handle all cases, as it only deals with
 * SDL_Keycode values, and SDL might provide keycode values for which there is
 * no SDL_Keycode (for example, when pressing "superscript two" key, I get 178
 * (which is the corresponding code point), which doesn't correspond to any
 * SDL_Keycode).
 */
public class SdlKeyConverter extends AbstractKeyConverter<SdlKeycode> {

    /*
     * https://wiki.libsdl.org/StuartPBentley/CombinedKeyTable
     */
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SdlKeyConverter() {

        /*
         * TODO sdl What are these?
         * SDLK_CLEAR (NUM_LOCK? CENTER?)
         * SDLK_KP_MEMCLEAR
         * SDLK_KP_MEMADD
         * SDLK_KP_MEMSUBTRACT
         * SDLK_KP_MEMMULTIPLY
         * SDLK_KP_MEMDIVIDE
         * SDLK_DECIMALSEPARATOR (DOT?)
         * SDLK_KP_DECIMAL (DOT?)
         */
        
        mapTo(BwdKeys.NO_STATEMENT, SdlKeycode.SDLK_UNKNOWN);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.NUM_LOCK, SdlKeycode.SDLK_NUMLOCKCLEAR);
        mapTo(BwdKeys.CAPS_LOCK, SdlKeycode.SDLK_CAPSLOCK);
        mapTo(BwdKeys.SCROLL_LOCK, SdlKeycode.SDLK_SCROLLLOCK);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SHIFT, SdlKeycode.SDLK_LSHIFT, SdlKeycode.SDLK_RSHIFT);
        mapTo(BwdKeys.CONTROL, SdlKeycode.SDLK_LCTRL, SdlKeycode.SDLK_RCTRL);
        mapTo(BwdKeys.ALT, SdlKeycode.SDLK_LALT);
        mapTo(BwdKeys.ALT_GRAPH, SdlKeycode.SDLK_RALT);
        mapTo(BwdKeys.META, SdlKeycode.SDLK_APPLICATION, SdlKeycode.SDLK_MENU);
        mapTo(BwdKeys.SUPER, SdlKeycode.SDLK_LGUI, SdlKeycode.SDLK_RGUI);
        noMatch(BwdKeys.HYPER);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.F1, SdlKeycode.SDLK_F1);
        mapTo(BwdKeys.F2, SdlKeycode.SDLK_F2);
        mapTo(BwdKeys.F3, SdlKeycode.SDLK_F3);
        mapTo(BwdKeys.F4, SdlKeycode.SDLK_F4);
        mapTo(BwdKeys.F5, SdlKeycode.SDLK_F5);
        mapTo(BwdKeys.F6, SdlKeycode.SDLK_F6);
        mapTo(BwdKeys.F7, SdlKeycode.SDLK_F7);
        mapTo(BwdKeys.F8, SdlKeycode.SDLK_F8);
        mapTo(BwdKeys.F9, SdlKeycode.SDLK_F9);
        mapTo(BwdKeys.F10, SdlKeycode.SDLK_F10);
        mapTo(BwdKeys.F11, SdlKeycode.SDLK_F11);
        mapTo(BwdKeys.F12, SdlKeycode.SDLK_F12);

        /*
         * 
         */

        mapTo(BwdKeys.A, SdlKeycode.SDLK_a);
        mapTo(BwdKeys.B, SdlKeycode.SDLK_b);
        mapTo(BwdKeys.C, SdlKeycode.SDLK_c);
        mapTo(BwdKeys.D, SdlKeycode.SDLK_d);
        mapTo(BwdKeys.E, SdlKeycode.SDLK_e);
        mapTo(BwdKeys.F, SdlKeycode.SDLK_f);
        mapTo(BwdKeys.G, SdlKeycode.SDLK_g);
        mapTo(BwdKeys.H, SdlKeycode.SDLK_h);
        mapTo(BwdKeys.I, SdlKeycode.SDLK_i);
        mapTo(BwdKeys.J, SdlKeycode.SDLK_j);
        mapTo(BwdKeys.K, SdlKeycode.SDLK_k);
        mapTo(BwdKeys.L, SdlKeycode.SDLK_l);
        mapTo(BwdKeys.M, SdlKeycode.SDLK_m);
        mapTo(BwdKeys.N, SdlKeycode.SDLK_n);
        mapTo(BwdKeys.O, SdlKeycode.SDLK_o);
        mapTo(BwdKeys.P, SdlKeycode.SDLK_p);
        mapTo(BwdKeys.Q, SdlKeycode.SDLK_q);
        mapTo(BwdKeys.R, SdlKeycode.SDLK_r);
        mapTo(BwdKeys.S, SdlKeycode.SDLK_s);
        mapTo(BwdKeys.T, SdlKeycode.SDLK_t);
        mapTo(BwdKeys.U, SdlKeycode.SDLK_u);
        mapTo(BwdKeys.V, SdlKeycode.SDLK_v);
        mapTo(BwdKeys.W, SdlKeycode.SDLK_w);
        mapTo(BwdKeys.X, SdlKeycode.SDLK_x);
        mapTo(BwdKeys.Y, SdlKeycode.SDLK_y);
        mapTo(BwdKeys.Z, SdlKeycode.SDLK_z);

        /*
         * 
         */
        
        mapTo(BwdKeys.DIGIT_0, SdlKeycode.SDLK_0, SdlKeycode.SDLK_KP_0);
        mapTo(BwdKeys.DIGIT_1, SdlKeycode.SDLK_1, SdlKeycode.SDLK_KP_1);
        mapTo(BwdKeys.DIGIT_2, SdlKeycode.SDLK_2, SdlKeycode.SDLK_KP_2);
        mapTo(BwdKeys.DIGIT_3, SdlKeycode.SDLK_3, SdlKeycode.SDLK_KP_3);
        mapTo(BwdKeys.DIGIT_4, SdlKeycode.SDLK_4, SdlKeycode.SDLK_KP_4);
        mapTo(BwdKeys.DIGIT_5, SdlKeycode.SDLK_5, SdlKeycode.SDLK_KP_5);
        mapTo(BwdKeys.DIGIT_6, SdlKeycode.SDLK_6, SdlKeycode.SDLK_KP_6);
        mapTo(BwdKeys.DIGIT_7, SdlKeycode.SDLK_7, SdlKeycode.SDLK_KP_7);
        mapTo(BwdKeys.DIGIT_8, SdlKeycode.SDLK_8, SdlKeycode.SDLK_KP_8);
        mapTo(BwdKeys.DIGIT_9, SdlKeycode.SDLK_9, SdlKeycode.SDLK_KP_9);
        //
        mapTo(BwdKeys.DOT, SdlKeycode.SDLK_PERIOD, SdlKeycode.SDLK_KP_PERIOD);

        /*
         * 
         */
        
        mapTo(BwdKeys.ESCAPE, SdlKeycode.SDLK_ESCAPE);
        mapTo(BwdKeys.TAB, SdlKeycode.SDLK_TAB);
        mapTo(BwdKeys.BACKSPACE, SdlKeycode.SDLK_BACKSPACE);
        mapTo(BwdKeys.EQUALS, SdlKeycode.SDLK_EQUALS, SdlKeycode.SDLK_KP_EQUALS, SdlKeycode.SDLK_KP_EQUALSAS400);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SLASH, SdlKeycode.SDLK_SLASH, SdlKeycode.SDLK_KP_DIVIDE);
        mapTo(BwdKeys.ASTERISK, SdlKeycode.SDLK_ASTERISK, SdlKeycode.SDLK_KP_MULTIPLY);
        mapTo(BwdKeys.MINUS, SdlKeycode.SDLK_MINUS, SdlKeycode.SDLK_KP_MINUS);
        mapTo(BwdKeys.PLUS, SdlKeycode.SDLK_PLUS, SdlKeycode.SDLK_KP_PLUS);
        mapTo(BwdKeys.ENTER, SdlKeycode.SDLK_RETURN, SdlKeycode.SDLK_RETURN2, SdlKeycode.SDLK_KP_ENTER);
        
        /*
         * TODO sdl For some reason, whether or not NUM_LOCK is on,
         * SDL always uses SDLK_KP_0 .. SDLK_KP_9, and SDLK_KP_PERIOD.
         */
        
        mapTo(BwdKeys.HOME, SdlKeycode.SDLK_HOME);
        mapTo(BwdKeys.END, SdlKeycode.SDLK_END);
        //
        mapTo(BwdKeys.PAGE_UP, SdlKeycode.SDLK_PAGEUP);
        mapTo(BwdKeys.PAGE_DOWN, SdlKeycode.SDLK_PAGEDOWN);
        //
        mapTo(BwdKeys.LEFT, SdlKeycode.SDLK_LEFT);
        mapTo(BwdKeys.UP, SdlKeycode.SDLK_UP);
        mapTo(BwdKeys.RIGHT, SdlKeycode.SDLK_RIGHT);
        mapTo(BwdKeys.DOWN, SdlKeycode.SDLK_DOWN);
        //
        mapTo(BwdKeys.INSERT, SdlKeycode.SDLK_INSERT);
        mapTo(BwdKeys.DELETE, SdlKeycode.SDLK_DELETE);
        //
        mapTo(BwdKeys.CENTER, SdlKeycode.SDLK_KP_CLEAR);

        /*
         * 
         */
        
        mapTo(BwdKeys.PRINT_SCREEN, SdlKeycode.SDLK_PRINTSCREEN);
        mapTo(BwdKeys.PAUSE, SdlKeycode.SDLK_PAUSE);

        /*
         * 
         */
        
        mapTo(BwdKeys.LEFT_PARENTHESIS, SdlKeycode.SDLK_LEFTPAREN);
        mapTo(BwdKeys.RIGHT_PARENTHESIS, SdlKeycode.SDLK_RIGHTPAREN);
        mapTo(BwdKeys.LEFT_BRACKET, SdlKeycode.SDLK_LEFTBRACKET);
        mapTo(BwdKeys.RIGHT_BRACKET, SdlKeycode.SDLK_RIGHTBRACKET);
        noMatch(BwdKeys.LEFT_BRACE);
        noMatch(BwdKeys.RIGHT_BRACE);

        /*
         * 
         */
        
        mapTo(BwdKeys.LESS_THAN, SdlKeycode.SDLK_LESS, SdlKeycode.SDLK_KP_LESS);
        mapTo(BwdKeys.GREATER_THAN, SdlKeycode.SDLK_GREATER, SdlKeycode.SDLK_KP_GREATER);
        
        mapTo(BwdKeys.SINGLE_QUOTE, SdlKeycode.SDLK_QUOTE);
        mapTo(BwdKeys.DOUBLE_QUOTE, SdlKeycode.SDLK_QUOTEDBL);
        
        mapTo(BwdKeys.GRAVE_ACCENT, SdlKeycode.SDLK_BACKQUOTE);
        mapTo(BwdKeys.CIRCUMFLEX_ACCENT, SdlKeycode.SDLK_KP_POWER);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SPACE, SdlKeycode.SDLK_SPACE, SdlKeycode.SDLK_KP_SPACE);
        mapTo(BwdKeys.UNDERSCORE, SdlKeycode.SDLK_UNDERSCORE);
        mapTo(BwdKeys.COMMA, SdlKeycode.SDLK_COMMA, SdlKeycode.SDLK_KP_COMMA);
        mapTo(BwdKeys.SEMICOLON, SdlKeycode.SDLK_SEMICOLON);
        mapTo(BwdKeys.COLON, SdlKeycode.SDLK_COLON, SdlKeycode.SDLK_KP_COLON);
        mapTo(BwdKeys.VERTICAL_BAR, SdlKeycode.SDLK_KP_VERTICALBAR);
        mapTo(BwdKeys.BACKSLASH, SdlKeycode.SDLK_BACKSLASH);
        
        mapTo(BwdKeys.AMPERSAND, SdlKeycode.SDLK_AMPERSAND, SdlKeycode.SDLK_KP_AMPERSAND);
        mapTo(BwdKeys.AT_SYMBOL, SdlKeycode.SDLK_AT, SdlKeycode.SDLK_KP_AT);
        mapTo(BwdKeys.DOLLAR, SdlKeycode.SDLK_DOLLAR);
        mapTo(BwdKeys.HASH, SdlKeycode.SDLK_HASH, SdlKeycode.SDLK_KP_HASH);
        mapTo(BwdKeys.PERCENT, SdlKeycode.SDLK_PERCENT, SdlKeycode.SDLK_KP_PERCENT);
        
        mapTo(BwdKeys.QUESTION_MARK, SdlKeycode.SDLK_QUESTION);
        mapTo(BwdKeys.EXCLAMATION_MARK, SdlKeycode.SDLK_EXCLAIM, SdlKeycode.SDLK_KP_EXCLAM);
        noMatch(BwdKeys.TILDE);
        
        /*
         * 
         */
        
        mapUnmappedBackingKeys(
                Arrays.asList(SdlKeycode.values()));
    }
}
