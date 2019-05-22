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
package net.jolikit.bwd.impl.algr5;

import java.util.Arrays;

import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.impl.algr5.jlib.AlgrKey;
import net.jolikit.bwd.impl.utils.events.AbstractKeyConverter;

public class AlgrKeyConverter extends AbstractKeyConverter<AlgrKey> {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AlgrKeyConverter() {
        
        mapTo(BwdKeys.NO_STATEMENT, AlgrKey.UNKNOWN);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.NUM_LOCK, AlgrKey.NUMLOCK);
        mapTo(BwdKeys.CAPS_LOCK, AlgrKey.CAPSLOCK);
        mapTo(BwdKeys.SCROLL_LOCK, AlgrKey.SCROLLLOCK);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SHIFT, AlgrKey.LSHIFT, AlgrKey.RSHIFT);
        mapTo(BwdKeys.CONTROL, AlgrKey.LCTRL, AlgrKey.RCTRL);
        mapTo(BwdKeys.ALT, AlgrKey.ALT);
        mapTo(BwdKeys.ALT_GRAPH, AlgrKey.ALTGR);
        mapTo(BwdKeys.META, AlgrKey.MENU);
        mapTo(BwdKeys.SUPER, AlgrKey.LWIN, AlgrKey.RWIN, AlgrKey.COMMAND);
        noMatch(BwdKeys.HYPER);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.F1, AlgrKey.F1);
        mapTo(BwdKeys.F2, AlgrKey.F2);
        mapTo(BwdKeys.F3, AlgrKey.F3);
        mapTo(BwdKeys.F4, AlgrKey.F4);
        mapTo(BwdKeys.F5, AlgrKey.F5);
        mapTo(BwdKeys.F6, AlgrKey.F6);
        mapTo(BwdKeys.F7, AlgrKey.F7);
        mapTo(BwdKeys.F8, AlgrKey.F8);
        mapTo(BwdKeys.F9, AlgrKey.F9);
        mapTo(BwdKeys.F10, AlgrKey.F10);
        mapTo(BwdKeys.F11, AlgrKey.F11);
        mapTo(BwdKeys.F12, AlgrKey.F12);

        /*
         * 
         */

        mapTo(BwdKeys.A, AlgrKey.A);
        mapTo(BwdKeys.B, AlgrKey.B);
        mapTo(BwdKeys.C, AlgrKey.C);
        mapTo(BwdKeys.D, AlgrKey.D);
        mapTo(BwdKeys.E, AlgrKey.E);
        mapTo(BwdKeys.F, AlgrKey.F);
        mapTo(BwdKeys.G, AlgrKey.G);
        mapTo(BwdKeys.H, AlgrKey.H);
        mapTo(BwdKeys.I, AlgrKey.I);
        mapTo(BwdKeys.J, AlgrKey.J);
        mapTo(BwdKeys.K, AlgrKey.K);
        mapTo(BwdKeys.L, AlgrKey.L);
        mapTo(BwdKeys.M, AlgrKey.M);
        mapTo(BwdKeys.N, AlgrKey.N);
        mapTo(BwdKeys.O, AlgrKey.O);
        mapTo(BwdKeys.P, AlgrKey.P);
        mapTo(BwdKeys.Q, AlgrKey.Q);
        mapTo(BwdKeys.R, AlgrKey.R);
        mapTo(BwdKeys.S, AlgrKey.S);
        mapTo(BwdKeys.T, AlgrKey.T);
        mapTo(BwdKeys.U, AlgrKey.U);
        mapTo(BwdKeys.V, AlgrKey.V);
        mapTo(BwdKeys.W, AlgrKey.W);
        mapTo(BwdKeys.X, AlgrKey.X);
        mapTo(BwdKeys.Y, AlgrKey.Y);
        mapTo(BwdKeys.Z, AlgrKey.Z);

        /*
         * 
         */
        
        mapTo(BwdKeys.DIGIT_0, AlgrKey.DIGIT_0, AlgrKey.PAD_0);
        mapTo(BwdKeys.DIGIT_1, AlgrKey.DIGIT_1, AlgrKey.PAD_1);
        mapTo(BwdKeys.DIGIT_2, AlgrKey.DIGIT_2, AlgrKey.PAD_2);
        mapTo(BwdKeys.DIGIT_3, AlgrKey.DIGIT_3, AlgrKey.PAD_3);
        mapTo(BwdKeys.DIGIT_4, AlgrKey.DIGIT_4, AlgrKey.PAD_4);
        mapTo(BwdKeys.DIGIT_5, AlgrKey.DIGIT_5, AlgrKey.PAD_5);
        mapTo(BwdKeys.DIGIT_6, AlgrKey.DIGIT_6, AlgrKey.PAD_6);
        mapTo(BwdKeys.DIGIT_7, AlgrKey.DIGIT_7, AlgrKey.PAD_7);
        mapTo(BwdKeys.DIGIT_8, AlgrKey.DIGIT_8, AlgrKey.PAD_8);
        mapTo(BwdKeys.DIGIT_9, AlgrKey.DIGIT_9, AlgrKey.PAD_9);
        //
        mapTo(BwdKeys.DOT, AlgrKey.FULLSTOP);

        /*
         * 
         */
        
        mapTo(BwdKeys.ESCAPE, AlgrKey.ESCAPE);
        mapTo(BwdKeys.TAB, AlgrKey.TAB);
        mapTo(BwdKeys.BACKSPACE, AlgrKey.BACKSPACE);
        mapTo(BwdKeys.EQUALS, AlgrKey.EQUALS, AlgrKey.PAD_EQUALS);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SLASH, AlgrKey.SLASH, AlgrKey.PAD_SLASH);
        mapTo(BwdKeys.ASTERISK, AlgrKey.PAD_ASTERISK);
        mapTo(BwdKeys.MINUS, AlgrKey.MINUS, AlgrKey.PAD_MINUS);
        mapTo(BwdKeys.PLUS, AlgrKey.PAD_PLUS);
        mapTo(BwdKeys.ENTER, AlgrKey.PAD_ENTER);
        
        /*
         * TODO algr For some reason, whether NUM_LOCK is on or off,
         * Allegro always uses PAD_0 .. PAD_9, and PAD_DELETE.
         */
        
        mapTo(BwdKeys.HOME, AlgrKey.HOME);
        mapTo(BwdKeys.END, AlgrKey.END);
        //
        mapTo(BwdKeys.PAGE_UP, AlgrKey.PGUP);
        mapTo(BwdKeys.PAGE_DOWN, AlgrKey.PGDN);
        //
        mapTo(BwdKeys.LEFT, AlgrKey.LEFT, AlgrKey.DPAD_LEFT);
        mapTo(BwdKeys.UP, AlgrKey.UP, AlgrKey.DPAD_UP);
        mapTo(BwdKeys.RIGHT, AlgrKey.RIGHT, AlgrKey.DPAD_RIGHT);
        mapTo(BwdKeys.DOWN, AlgrKey.DOWN, AlgrKey.DPAD_DOWN);
        //
        mapTo(BwdKeys.INSERT, AlgrKey.INSERT);
        mapTo(BwdKeys.DELETE, AlgrKey.DELETE, AlgrKey.PAD_DELETE);
        //
        mapTo(BwdKeys.CENTER, AlgrKey.DPAD_CENTER);

        /*
         * 
         */
        
        mapTo(BwdKeys.PRINT_SCREEN, AlgrKey.PRINTSCREEN);
        mapTo(BwdKeys.PAUSE, AlgrKey.PAUSE);

        /*
         * 
         */
        
        noMatch(BwdKeys.LEFT_PARENTHESIS);
        noMatch(BwdKeys.RIGHT_PARENTHESIS);
        noMatch(BwdKeys.LEFT_BRACKET);
        noMatch(BwdKeys.RIGHT_BRACKET);
        mapTo(BwdKeys.LEFT_BRACE, AlgrKey.OPENBRACE);
        mapTo(BwdKeys.RIGHT_BRACE, AlgrKey.CLOSEBRACE);

        /*
         * 
         */
        
        noMatch(BwdKeys.LESS_THAN);
        noMatch(BwdKeys.GREATER_THAN);
        
        mapTo(BwdKeys.SINGLE_QUOTE, AlgrKey.QUOTE);
        noMatch(BwdKeys.DOUBLE_QUOTE);
        
        mapTo(BwdKeys.GRAVE_ACCENT, AlgrKey.BACKQUOTE);
        mapTo(BwdKeys.CIRCUMFLEX_ACCENT, AlgrKey.CIRCUMFLEX);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SPACE, AlgrKey.SPACE);
        noMatch(BwdKeys.UNDERSCORE);
        mapTo(BwdKeys.COMMA, AlgrKey.COMMA);
        mapTo(BwdKeys.SEMICOLON,
                AlgrKey.SEMICOLON,
                AlgrKey.SEMICOLON2);
        mapTo(BwdKeys.COLON, AlgrKey.COLON2);
        noMatch(BwdKeys.VERTICAL_BAR);
        mapTo(BwdKeys.BACKSLASH,
                AlgrKey.BACKSLASH,
                AlgrKey.BACKSLASH2);
        
        noMatch(BwdKeys.AMPERSAND);
        mapTo(BwdKeys.AT_SYMBOL, AlgrKey.AT);
        noMatch(BwdKeys.DOLLAR);
        noMatch(BwdKeys.HASH);
        noMatch(BwdKeys.PERCENT);
        
        noMatch(BwdKeys.QUESTION_MARK);
        noMatch(BwdKeys.EXCLAMATION_MARK);
        mapTo(BwdKeys.TILDE, AlgrKey.TILDE);
        
        /*
         * 
         */
        
        mapUnmappedBackingKeys(
                Arrays.asList(AlgrKey.values()));
    }
}
