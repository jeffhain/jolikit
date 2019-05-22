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
package net.jolikit.bwd.impl.qtj4;

import java.util.Arrays;

import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.impl.utils.events.AbstractKeyConverter;

import com.trolltech.qt.core.Qt.Key;

public class QtjKeyConverter extends AbstractKeyConverter<Key> {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public QtjKeyConverter() {
        
        mapTo(BwdKeys.NO_STATEMENT, Key.Key_unknown);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.NUM_LOCK, Key.Key_NumLock);
        mapTo(BwdKeys.CAPS_LOCK, Key.Key_CapsLock);
        mapTo(BwdKeys.SCROLL_LOCK, Key.Key_ScrollLock);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SHIFT, Key.Key_Shift);
        mapTo(BwdKeys.CONTROL, Key.Key_Control);
        mapTo(BwdKeys.ALT, Key.Key_Alt);
        /**
         * TODO qtj When ALT_GRAPH is pressed, Qt generates:
         * CONTROL-pressed, then ALT-pressed, and on release:
         * CONTROL-released, and then ALT-released.
         * As a result, this mapping is never used.
         */
        mapTo(BwdKeys.ALT_GRAPH, Key.Key_AltGr);
        mapTo(BwdKeys.META, Key.Key_Meta, Key.Key_ApplicationLeft, Key.Key_ApplicationRight);
        /**
         * TODO qtj When a Windows key (left or right) is pressed/released,
         * Qt generates Key.Key_Meta key events, not Key.Key_Super_L
         * or Key.Key_Super_R events.
         * As a result, this mapping is never used.
         */
        mapTo(BwdKeys.SUPER, Key.Key_Super_L, Key.Key_Super_R);
        noMatch(BwdKeys.HYPER);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.F1, Key.Key_F1);
        mapTo(BwdKeys.F2, Key.Key_F2);
        mapTo(BwdKeys.F3, Key.Key_F3);
        mapTo(BwdKeys.F4, Key.Key_F4);
        mapTo(BwdKeys.F5, Key.Key_F5);
        mapTo(BwdKeys.F6, Key.Key_F6);
        mapTo(BwdKeys.F7, Key.Key_F7);
        mapTo(BwdKeys.F8, Key.Key_F8);
        mapTo(BwdKeys.F9, Key.Key_F9);
        mapTo(BwdKeys.F10, Key.Key_F10);
        mapTo(BwdKeys.F11, Key.Key_F11);
        mapTo(BwdKeys.F12, Key.Key_F12);

        /*
         * 
         */

        mapTo(BwdKeys.A, Key.Key_A);
        mapTo(BwdKeys.B, Key.Key_B);
        mapTo(BwdKeys.C, Key.Key_C);
        mapTo(BwdKeys.D, Key.Key_D);
        mapTo(BwdKeys.E, Key.Key_E);
        mapTo(BwdKeys.F, Key.Key_F);
        mapTo(BwdKeys.G, Key.Key_G);
        mapTo(BwdKeys.H, Key.Key_H);
        mapTo(BwdKeys.I, Key.Key_I);
        mapTo(BwdKeys.J, Key.Key_J);
        mapTo(BwdKeys.K, Key.Key_K);
        mapTo(BwdKeys.L, Key.Key_L);
        mapTo(BwdKeys.M, Key.Key_M);
        mapTo(BwdKeys.N, Key.Key_N);
        mapTo(BwdKeys.O, Key.Key_O);
        mapTo(BwdKeys.P, Key.Key_P);
        mapTo(BwdKeys.Q, Key.Key_Q);
        mapTo(BwdKeys.R, Key.Key_R);
        mapTo(BwdKeys.S, Key.Key_S);
        mapTo(BwdKeys.T, Key.Key_T);
        mapTo(BwdKeys.U, Key.Key_U);
        mapTo(BwdKeys.V, Key.Key_V);
        mapTo(BwdKeys.W, Key.Key_W);
        mapTo(BwdKeys.X, Key.Key_X);
        mapTo(BwdKeys.Y, Key.Key_Y);
        mapTo(BwdKeys.Z, Key.Key_Z);

        /*
         * 
         */
        
        mapTo(BwdKeys.DIGIT_0, Key.Key_0);
        mapTo(BwdKeys.DIGIT_1, Key.Key_1);
        mapTo(BwdKeys.DIGIT_2, Key.Key_2);
        mapTo(BwdKeys.DIGIT_3, Key.Key_3);
        mapTo(BwdKeys.DIGIT_4, Key.Key_4);
        mapTo(BwdKeys.DIGIT_5, Key.Key_5);
        mapTo(BwdKeys.DIGIT_6, Key.Key_6);
        mapTo(BwdKeys.DIGIT_7, Key.Key_7);
        mapTo(BwdKeys.DIGIT_8, Key.Key_8);
        mapTo(BwdKeys.DIGIT_9, Key.Key_9);
        //
        mapTo(BwdKeys.DOT, Key.Key_Period, Key.Key_periodcentered);

        /*
         * 
         */
        
        mapTo(BwdKeys.ESCAPE, Key.Key_Escape);
        mapTo(BwdKeys.TAB, Key.Key_Tab);
        mapTo(BwdKeys.BACKSPACE, Key.Key_Backspace);
        mapTo(BwdKeys.EQUALS, Key.Key_Equal);
        
        /*
         * 
         */
        
        /**
         * TODO qtj If only the (SLASH,COLON) key is pressed,
         * Qt generates Key.Key_Colon events,
         * but if SHIFT is down while pressing the (SLASH,COLON) key,
         * it generates Key.Key_Slash events,
         * as when the SLASH key of numeric pad is pressed alone:
         * it mixes up the notions of keys and of Unicode characters.
         */
        mapTo(BwdKeys.SLASH, Key.Key_Slash, Key.Key_division);
        mapTo(BwdKeys.ASTERISK, Key.Key_Asterisk, Key.Key_multiply);
        mapTo(BwdKeys.MINUS, Key.Key_Minus, Key.Key_hyphen);
        mapTo(BwdKeys.PLUS, Key.Key_Plus);
        mapTo(BwdKeys.ENTER, Key.Key_Enter, Key.Key_Return);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.HOME, Key.Key_Home);
        mapTo(BwdKeys.END, Key.Key_End);
        //
        mapTo(BwdKeys.PAGE_UP, Key.Key_PageUp);
        mapTo(BwdKeys.PAGE_DOWN, Key.Key_PageDown);
        //
        mapTo(BwdKeys.LEFT, Key.Key_Left);
        mapTo(BwdKeys.UP, Key.Key_Up);
        mapTo(BwdKeys.RIGHT, Key.Key_Right);
        mapTo(BwdKeys.DOWN, Key.Key_Down);
        //
        mapTo(BwdKeys.INSERT, Key.Key_Insert);
        mapTo(BwdKeys.DELETE, Key.Key_Delete);
        //
        mapTo(BwdKeys.CENTER, Key.Key_Clear);

        /*
         * 
         */
        
        mapTo(BwdKeys.PRINT_SCREEN, Key.Key_Print);
        mapTo(BwdKeys.PAUSE, Key.Key_Pause);

        /*
         * 
         */
        
        mapTo(BwdKeys.LEFT_PARENTHESIS, Key.Key_ParenLeft);
        mapTo(BwdKeys.RIGHT_PARENTHESIS, Key.Key_ParenRight);
        mapTo(BwdKeys.LEFT_BRACKET, Key.Key_BracketLeft);
        mapTo(BwdKeys.RIGHT_BRACKET, Key.Key_BracketRight);
        mapTo(BwdKeys.LEFT_BRACE, Key.Key_BraceLeft);
        mapTo(BwdKeys.RIGHT_BRACE, Key.Key_BraceRight);

        /*
         * 
         */
        
        mapTo(BwdKeys.LESS_THAN, Key.Key_Less);
        mapTo(BwdKeys.GREATER_THAN, Key.Key_Greater);
        
        mapTo(BwdKeys.SINGLE_QUOTE, Key.Key_Apostrophe);
        mapTo(BwdKeys.DOUBLE_QUOTE, Key.Key_QuoteDbl);
        
        mapTo(BwdKeys.GRAVE_ACCENT, Key.Key_QuoteLeft);
        mapTo(BwdKeys.CIRCUMFLEX_ACCENT, Key.Key_AsciiCircum);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SPACE, Key.Key_Space);
        mapTo(BwdKeys.UNDERSCORE, Key.Key_Underscore);
        mapTo(BwdKeys.COMMA, Key.Key_Comma);
        mapTo(BwdKeys.SEMICOLON, Key.Key_Semicolon);
        mapTo(BwdKeys.COLON, Key.Key_Colon);
        noMatch(BwdKeys.VERTICAL_BAR);
        mapTo(BwdKeys.BACKSLASH, Key.Key_Backslash);
        
        mapTo(BwdKeys.AMPERSAND, Key.Key_Ampersand);
        mapTo(BwdKeys.AT_SYMBOL, Key.Key_At);
        mapTo(BwdKeys.DOLLAR, Key.Key_Dollar);
        mapTo(BwdKeys.HASH, Key.Key_NumberSign);
        mapTo(BwdKeys.PERCENT, Key.Key_Percent);
        
        mapTo(BwdKeys.QUESTION_MARK, Key.Key_Question);
        mapTo(BwdKeys.EXCLAMATION_MARK, Key.Key_Exclam);
        mapTo(BwdKeys.TILDE, Key.Key_AsciiTilde);
        
        /*
         * 
         */
        
        mapUnmappedBackingKeys(
                Arrays.asList(Key.values()));
    }
}
