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
package net.jolikit.bwd.impl.jogl;

import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.impl.utils.events.AbstractKeyConverter;

import com.jogamp.newt.event.KeyEvent;

public class JoglKeyConverter extends AbstractKeyConverter<Short> {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JoglKeyConverter() {
        
        mapTo(BwdKeys.NO_STATEMENT, KeyEvent.VK_UNDEFINED, KeyEvent.VK_KEYBOARD_INVISIBLE);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.NUM_LOCK, KeyEvent.VK_NUM_LOCK);
        mapTo(BwdKeys.CAPS_LOCK, KeyEvent.VK_CAPS_LOCK);
        mapTo(BwdKeys.SCROLL_LOCK, KeyEvent.VK_SCROLL_LOCK);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SHIFT, KeyEvent.VK_SHIFT);
        mapTo(BwdKeys.CONTROL, KeyEvent.VK_CONTROL);
        mapTo(BwdKeys.ALT, KeyEvent.VK_ALT);
        mapTo(BwdKeys.ALT_GRAPH, KeyEvent.VK_ALT_GRAPH);
        mapTo(BwdKeys.META, KeyEvent.VK_META, KeyEvent.VK_CONTEXT_MENU);
        mapTo(BwdKeys.SUPER, KeyEvent.VK_WINDOWS, KeyEvent.VK_COMPOSE);
        noMatch(BwdKeys.HYPER);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.F1, KeyEvent.VK_F1);
        mapTo(BwdKeys.F2, KeyEvent.VK_F2);
        mapTo(BwdKeys.F3, KeyEvent.VK_F3);
        mapTo(BwdKeys.F4, KeyEvent.VK_F4);
        mapTo(BwdKeys.F5, KeyEvent.VK_F5);
        mapTo(BwdKeys.F6, KeyEvent.VK_F6);
        mapTo(BwdKeys.F7, KeyEvent.VK_F7);
        mapTo(BwdKeys.F8, KeyEvent.VK_F8);
        mapTo(BwdKeys.F9, KeyEvent.VK_F9);
        mapTo(BwdKeys.F10, KeyEvent.VK_F10);
        mapTo(BwdKeys.F11, KeyEvent.VK_F11);
        mapTo(BwdKeys.F12, KeyEvent.VK_F12);

        /*
         * 
         */

        mapTo(BwdKeys.A, KeyEvent.VK_A);
        mapTo(BwdKeys.B, KeyEvent.VK_B);
        mapTo(BwdKeys.C, KeyEvent.VK_C);
        mapTo(BwdKeys.D, KeyEvent.VK_D);
        mapTo(BwdKeys.E, KeyEvent.VK_E);
        mapTo(BwdKeys.F, KeyEvent.VK_F);
        mapTo(BwdKeys.G, KeyEvent.VK_G);
        mapTo(BwdKeys.H, KeyEvent.VK_H);
        mapTo(BwdKeys.I, KeyEvent.VK_I);
        mapTo(BwdKeys.J, KeyEvent.VK_J);
        mapTo(BwdKeys.K, KeyEvent.VK_K);
        mapTo(BwdKeys.L, KeyEvent.VK_L);
        mapTo(BwdKeys.M, KeyEvent.VK_M);
        mapTo(BwdKeys.N, KeyEvent.VK_N);
        mapTo(BwdKeys.O, KeyEvent.VK_O);
        mapTo(BwdKeys.P, KeyEvent.VK_P);
        mapTo(BwdKeys.Q, KeyEvent.VK_Q);
        mapTo(BwdKeys.R, KeyEvent.VK_R);
        mapTo(BwdKeys.S, KeyEvent.VK_S);
        mapTo(BwdKeys.T, KeyEvent.VK_T);
        mapTo(BwdKeys.U, KeyEvent.VK_U);
        mapTo(BwdKeys.V, KeyEvent.VK_V);
        mapTo(BwdKeys.W, KeyEvent.VK_W);
        mapTo(BwdKeys.X, KeyEvent.VK_X);
        mapTo(BwdKeys.Y, KeyEvent.VK_Y);
        mapTo(BwdKeys.Z, KeyEvent.VK_Z);

        /*
         * 
         */
        
        mapTo(BwdKeys.DIGIT_0, KeyEvent.VK_0, KeyEvent.VK_NUMPAD0);
        mapTo(BwdKeys.DIGIT_1, KeyEvent.VK_1, KeyEvent.VK_NUMPAD1);
        mapTo(BwdKeys.DIGIT_2, KeyEvent.VK_2, KeyEvent.VK_NUMPAD2);
        mapTo(BwdKeys.DIGIT_3, KeyEvent.VK_3, KeyEvent.VK_NUMPAD3);
        mapTo(BwdKeys.DIGIT_4, KeyEvent.VK_4, KeyEvent.VK_NUMPAD4);
        mapTo(BwdKeys.DIGIT_5, KeyEvent.VK_5, KeyEvent.VK_NUMPAD5);
        mapTo(BwdKeys.DIGIT_6, KeyEvent.VK_6, KeyEvent.VK_NUMPAD6);
        mapTo(BwdKeys.DIGIT_7, KeyEvent.VK_7, KeyEvent.VK_NUMPAD7);
        mapTo(BwdKeys.DIGIT_8, KeyEvent.VK_8, KeyEvent.VK_NUMPAD8);
        mapTo(BwdKeys.DIGIT_9, KeyEvent.VK_9, KeyEvent.VK_NUMPAD9);
        //
        mapTo(BwdKeys.DOT, KeyEvent.VK_PERIOD, KeyEvent.VK_DECIMAL);

        /*
         * 
         */
        
        mapTo(BwdKeys.ESCAPE, KeyEvent.VK_ESCAPE);
        mapTo(BwdKeys.TAB, KeyEvent.VK_TAB);
        mapTo(BwdKeys.BACKSPACE, KeyEvent.VK_BACK_SPACE);
        mapTo(BwdKeys.EQUALS, KeyEvent.VK_EQUALS);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SLASH, KeyEvent.VK_SLASH, KeyEvent.VK_DIVIDE);
        mapTo(BwdKeys.ASTERISK, KeyEvent.VK_ASTERISK, KeyEvent.VK_MULTIPLY);
        mapTo(BwdKeys.MINUS, KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT);
        mapTo(BwdKeys.PLUS, KeyEvent.VK_PLUS, KeyEvent.VK_ADD);
        mapTo(BwdKeys.ENTER, KeyEvent.VK_ENTER);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.HOME, KeyEvent.VK_HOME);
        mapTo(BwdKeys.END, KeyEvent.VK_END);
        //
        mapTo(BwdKeys.PAGE_UP, KeyEvent.VK_PAGE_UP);
        mapTo(BwdKeys.PAGE_DOWN, KeyEvent.VK_PAGE_DOWN);
        //
        mapTo(BwdKeys.LEFT, KeyEvent.VK_LEFT);
        mapTo(BwdKeys.UP, KeyEvent.VK_UP);
        mapTo(BwdKeys.RIGHT, KeyEvent.VK_RIGHT);
        mapTo(BwdKeys.DOWN, KeyEvent.VK_DOWN);
        //
        mapTo(BwdKeys.INSERT, KeyEvent.VK_INSERT);
        mapTo(BwdKeys.DELETE, KeyEvent.VK_DELETE);
        //
        mapTo(BwdKeys.CENTER, KeyEvent.VK_BEGIN, KeyEvent.VK_CLEAR);

        /*
         * 
         */
        
        mapTo(BwdKeys.PRINT_SCREEN, KeyEvent.VK_PRINTSCREEN);
        mapTo(BwdKeys.PAUSE, KeyEvent.VK_PAUSE);

        /*
         * 
         */
        
        mapTo(BwdKeys.LEFT_PARENTHESIS, KeyEvent.VK_LEFT_PARENTHESIS);
        mapTo(BwdKeys.RIGHT_PARENTHESIS, KeyEvent.VK_RIGHT_PARENTHESIS);
        mapTo(BwdKeys.LEFT_BRACKET, KeyEvent.VK_OPEN_BRACKET);
        mapTo(BwdKeys.RIGHT_BRACKET, KeyEvent.VK_CLOSE_BRACKET);
        mapTo(BwdKeys.LEFT_BRACE, KeyEvent.VK_LEFT_BRACE);
        mapTo(BwdKeys.RIGHT_BRACE, KeyEvent.VK_RIGHT_BRACE);

        /*
         * 
         */
        
        mapTo(BwdKeys.LESS_THAN, KeyEvent.VK_LESS);
        mapTo(BwdKeys.GREATER_THAN, KeyEvent.VK_GREATER);
        
        mapTo(BwdKeys.SINGLE_QUOTE, KeyEvent.VK_QUOTE);
        mapTo(BwdKeys.DOUBLE_QUOTE, KeyEvent.VK_QUOTEDBL);
        
        mapTo(BwdKeys.GRAVE_ACCENT, KeyEvent.VK_BACK_QUOTE);
        mapTo(BwdKeys.CIRCUMFLEX_ACCENT, KeyEvent.VK_CIRCUMFLEX);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SPACE, KeyEvent.VK_SPACE);
        mapTo(BwdKeys.UNDERSCORE, KeyEvent.VK_UNDERSCORE);
        mapTo(BwdKeys.COMMA, KeyEvent.VK_COMMA);
        mapTo(BwdKeys.SEMICOLON, KeyEvent.VK_SEMICOLON);
        mapTo(BwdKeys.COLON, KeyEvent.VK_COLON);
        mapTo(BwdKeys.VERTICAL_BAR, KeyEvent.VK_PIPE);
        mapTo(BwdKeys.BACKSLASH, KeyEvent.VK_BACK_SLASH);
        
        mapTo(BwdKeys.AMPERSAND, KeyEvent.VK_AMPERSAND);
        mapTo(BwdKeys.AT_SYMBOL, KeyEvent.VK_AT);
        mapTo(BwdKeys.DOLLAR, KeyEvent.VK_DOLLAR);
        mapTo(BwdKeys.HASH, KeyEvent.VK_NUMBER_SIGN);
        mapTo(BwdKeys.PERCENT, KeyEvent.VK_PERCENT);
        
        mapTo(BwdKeys.QUESTION_MARK, KeyEvent.VK_QUESTIONMARK);
        mapTo(BwdKeys.EXCLAMATION_MARK, KeyEvent.VK_EXCLAMATION_MARK);
        mapTo(BwdKeys.TILDE, KeyEvent.VK_TILDE);
        
        /*
         * 
         */
        
        mapUnmappedBackingKeys(JoglNewtKeys.keyList());
    }
}
