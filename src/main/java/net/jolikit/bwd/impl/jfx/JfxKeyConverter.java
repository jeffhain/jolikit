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

import java.util.Arrays;

import javafx.scene.input.KeyCode;
import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.impl.utils.events.AbstractKeyConverter;

public class JfxKeyConverter extends AbstractKeyConverter<KeyCode> {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JfxKeyConverter() {
        
        mapTo(BwdKeys.NO_STATEMENT, KeyCode.UNDEFINED);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.NUM_LOCK, KeyCode.NUM_LOCK);
        mapTo(BwdKeys.CAPS_LOCK, KeyCode.CAPS);
        mapTo(BwdKeys.SCROLL_LOCK, KeyCode.SCROLL_LOCK);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SHIFT, KeyCode.SHIFT);
        mapTo(BwdKeys.CONTROL, KeyCode.CONTROL);
        mapTo(BwdKeys.ALT, KeyCode.ALT);
        mapTo(BwdKeys.ALT_GRAPH, KeyCode.ALT_GRAPH);
        mapTo(BwdKeys.META, KeyCode.META, KeyCode.CONTEXT_MENU);
        mapTo(BwdKeys.SUPER, KeyCode.WINDOWS, KeyCode.COMMAND, KeyCode.COMPOSE);
        noMatch(BwdKeys.HYPER);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.F1, KeyCode.F1);
        mapTo(BwdKeys.F2, KeyCode.F2);
        mapTo(BwdKeys.F3, KeyCode.F3);
        mapTo(BwdKeys.F4, KeyCode.F4);
        mapTo(BwdKeys.F5, KeyCode.F5);
        mapTo(BwdKeys.F6, KeyCode.F6);
        mapTo(BwdKeys.F7, KeyCode.F7);
        mapTo(BwdKeys.F8, KeyCode.F8);
        mapTo(BwdKeys.F9, KeyCode.F9);
        mapTo(BwdKeys.F10, KeyCode.F10);
        mapTo(BwdKeys.F11, KeyCode.F11);
        mapTo(BwdKeys.F12, KeyCode.F12);

        /*
         * 
         */

        mapTo(BwdKeys.A, KeyCode.A);
        mapTo(BwdKeys.B, KeyCode.B);
        mapTo(BwdKeys.C, KeyCode.C);
        mapTo(BwdKeys.D, KeyCode.D);
        mapTo(BwdKeys.E, KeyCode.E);
        mapTo(BwdKeys.F, KeyCode.F);
        mapTo(BwdKeys.G, KeyCode.G);
        mapTo(BwdKeys.H, KeyCode.H);
        mapTo(BwdKeys.I, KeyCode.I);
        mapTo(BwdKeys.J, KeyCode.J);
        mapTo(BwdKeys.K, KeyCode.K);
        mapTo(BwdKeys.L, KeyCode.L);
        mapTo(BwdKeys.M, KeyCode.M);
        mapTo(BwdKeys.N, KeyCode.N);
        mapTo(BwdKeys.O, KeyCode.O);
        mapTo(BwdKeys.P, KeyCode.P);
        mapTo(BwdKeys.Q, KeyCode.Q);
        mapTo(BwdKeys.R, KeyCode.R);
        mapTo(BwdKeys.S, KeyCode.S);
        mapTo(BwdKeys.T, KeyCode.T);
        mapTo(BwdKeys.U, KeyCode.U);
        mapTo(BwdKeys.V, KeyCode.V);
        mapTo(BwdKeys.W, KeyCode.W);
        mapTo(BwdKeys.X, KeyCode.X);
        mapTo(BwdKeys.Y, KeyCode.Y);
        mapTo(BwdKeys.Z, KeyCode.Z);

        /*
         * 
         */
        
        mapTo(BwdKeys.DIGIT_0, KeyCode.DIGIT0, KeyCode.NUMPAD0);
        mapTo(BwdKeys.DIGIT_1, KeyCode.DIGIT1, KeyCode.NUMPAD1);
        mapTo(BwdKeys.DIGIT_2, KeyCode.DIGIT2, KeyCode.NUMPAD2);
        mapTo(BwdKeys.DIGIT_3, KeyCode.DIGIT3, KeyCode.NUMPAD3);
        mapTo(BwdKeys.DIGIT_4, KeyCode.DIGIT4, KeyCode.NUMPAD4);
        mapTo(BwdKeys.DIGIT_5, KeyCode.DIGIT5, KeyCode.NUMPAD5);
        mapTo(BwdKeys.DIGIT_6, KeyCode.DIGIT6, KeyCode.NUMPAD6);
        mapTo(BwdKeys.DIGIT_7, KeyCode.DIGIT7, KeyCode.NUMPAD7);
        mapTo(BwdKeys.DIGIT_8, KeyCode.DIGIT8, KeyCode.NUMPAD8);
        mapTo(BwdKeys.DIGIT_9, KeyCode.DIGIT9, KeyCode.NUMPAD9);
        //
        mapTo(BwdKeys.DOT, KeyCode.PERIOD, KeyCode.DECIMAL);

        /*
         * 
         */
        
        mapTo(BwdKeys.ESCAPE, KeyCode.ESCAPE);
        mapTo(BwdKeys.TAB, KeyCode.TAB);
        mapTo(BwdKeys.BACKSPACE, KeyCode.BACK_SPACE);
        mapTo(BwdKeys.EQUALS, KeyCode.EQUALS);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SLASH, KeyCode.SLASH, KeyCode.DIVIDE);
        mapTo(BwdKeys.ASTERISK, KeyCode.ASTERISK, KeyCode.MULTIPLY);
        mapTo(BwdKeys.MINUS, KeyCode.MINUS, KeyCode.SUBTRACT);
        mapTo(BwdKeys.PLUS, KeyCode.PLUS, KeyCode.ADD);
        mapTo(BwdKeys.ENTER, KeyCode.ENTER);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.HOME, KeyCode.HOME);
        mapTo(BwdKeys.END, KeyCode.END);
        //
        mapTo(BwdKeys.PAGE_UP, KeyCode.PAGE_UP);
        mapTo(BwdKeys.PAGE_DOWN, KeyCode.PAGE_DOWN);
        //
        mapTo(BwdKeys.LEFT, KeyCode.LEFT, KeyCode.KP_LEFT);
        mapTo(BwdKeys.UP, KeyCode.UP, KeyCode.KP_UP);
        mapTo(BwdKeys.RIGHT, KeyCode.RIGHT, KeyCode.KP_RIGHT);
        mapTo(BwdKeys.DOWN, KeyCode.DOWN, KeyCode.KP_DOWN);
        //
        mapTo(BwdKeys.INSERT, KeyCode.INSERT);
        mapTo(BwdKeys.DELETE, KeyCode.DELETE);
        //
        /**
         * TODO jfx JavaFX uses CLEAR when we hit CENTER,
         * and not BEGIN which I thought was designed for that.
         */
        mapTo(BwdKeys.CENTER, KeyCode.BEGIN, KeyCode.CLEAR);

        /*
         * 
         */
        
        mapTo(BwdKeys.PRINT_SCREEN, KeyCode.PRINTSCREEN);
        mapTo(BwdKeys.PAUSE, KeyCode.PAUSE);

        /*
         * 
         */
        
        mapTo(BwdKeys.LEFT_PARENTHESIS, KeyCode.LEFT_PARENTHESIS);
        mapTo(BwdKeys.RIGHT_PARENTHESIS, KeyCode.RIGHT_PARENTHESIS);
        mapTo(BwdKeys.LEFT_BRACKET, KeyCode.OPEN_BRACKET);
        mapTo(BwdKeys.RIGHT_BRACKET, KeyCode.CLOSE_BRACKET);
        mapTo(BwdKeys.LEFT_BRACE, KeyCode.BRACELEFT);
        mapTo(BwdKeys.RIGHT_BRACE, KeyCode.BRACERIGHT);

        /*
         * 
         */
        
        mapTo(BwdKeys.LESS_THAN, KeyCode.LESS);
        mapTo(BwdKeys.GREATER_THAN, KeyCode.GREATER);
        
        mapTo(BwdKeys.SINGLE_QUOTE, KeyCode.QUOTE);
        mapTo(BwdKeys.DOUBLE_QUOTE, KeyCode.QUOTEDBL);
        
        mapTo(BwdKeys.GRAVE_ACCENT, KeyCode.BACK_QUOTE);
        mapTo(BwdKeys.CIRCUMFLEX_ACCENT, KeyCode.CIRCUMFLEX);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SPACE, KeyCode.SPACE);
        mapTo(BwdKeys.UNDERSCORE, KeyCode.UNDERSCORE);
        mapTo(BwdKeys.COMMA, KeyCode.COMMA);
        mapTo(BwdKeys.SEMICOLON, KeyCode.SEMICOLON);
        mapTo(BwdKeys.COLON, KeyCode.COLON);
        noMatch(BwdKeys.VERTICAL_BAR);
        mapTo(BwdKeys.BACKSLASH, KeyCode.BACK_SLASH);
        
        mapTo(BwdKeys.AMPERSAND, KeyCode.AMPERSAND);
        mapTo(BwdKeys.AT_SYMBOL, KeyCode.AT);
        mapTo(BwdKeys.DOLLAR, KeyCode.DOLLAR);
        mapTo(BwdKeys.HASH, KeyCode.NUMBER_SIGN);
        noMatch(BwdKeys.PERCENT);
        
        noMatch(BwdKeys.QUESTION_MARK);
        mapTo(BwdKeys.EXCLAMATION_MARK, KeyCode.EXCLAMATION_MARK);
        mapTo(BwdKeys.TILDE, KeyCode.DEAD_TILDE);
        
        /*
         * 
         */
        
        mapUnmappedBackingKeys(
                Arrays.asList(KeyCode.values()));
    }
}
