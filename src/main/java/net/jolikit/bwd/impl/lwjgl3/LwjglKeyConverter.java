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
package net.jolikit.bwd.impl.lwjgl3;

import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.impl.utils.events.AbstractKeyConverter;

import org.lwjgl.glfw.GLFW;

public class LwjglKeyConverter extends AbstractKeyConverter<Integer> {

    /*
     * http://www.glfw.org/docs/3.1/group__keys.html
     */

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public LwjglKeyConverter() {
        
        /*
         * TODO lwjgl GLFW.GLFW_KEY_UNKNOWN is -1,
         * so 0 could theoretically be a valid value.
         * Though, GLFW doesn't use 0 as a key code constant,
         * so we can assume that it's an invalid value,
         * and map it to BwdKeys.NO_STATEMENT as well.
         */
        
        mapTo(BwdKeys.NO_STATEMENT, GLFW.GLFW_KEY_UNKNOWN, 0);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.NUM_LOCK, GLFW.GLFW_KEY_NUM_LOCK);
        mapTo(BwdKeys.CAPS_LOCK, GLFW.GLFW_KEY_CAPS_LOCK);
        mapTo(BwdKeys.SCROLL_LOCK, GLFW.GLFW_KEY_SCROLL_LOCK);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SHIFT, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT);
        mapTo(BwdKeys.CONTROL, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL);
        mapTo(BwdKeys.ALT, GLFW.GLFW_KEY_LEFT_ALT);
        mapTo(BwdKeys.ALT_GRAPH, GLFW.GLFW_KEY_RIGHT_ALT);
        mapTo(BwdKeys.META, GLFW.GLFW_KEY_MENU);
        mapTo(BwdKeys.SUPER, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER);
        noMatch(BwdKeys.HYPER);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.F1, GLFW.GLFW_KEY_F1);
        mapTo(BwdKeys.F2, GLFW.GLFW_KEY_F2);
        mapTo(BwdKeys.F3, GLFW.GLFW_KEY_F3);
        mapTo(BwdKeys.F4, GLFW.GLFW_KEY_F4);
        mapTo(BwdKeys.F5, GLFW.GLFW_KEY_F5);
        mapTo(BwdKeys.F6, GLFW.GLFW_KEY_F6);
        mapTo(BwdKeys.F7, GLFW.GLFW_KEY_F7);
        mapTo(BwdKeys.F8, GLFW.GLFW_KEY_F8);
        mapTo(BwdKeys.F9, GLFW.GLFW_KEY_F9);
        mapTo(BwdKeys.F10, GLFW.GLFW_KEY_F10);
        mapTo(BwdKeys.F11, GLFW.GLFW_KEY_F11);
        mapTo(BwdKeys.F12, GLFW.GLFW_KEY_F12);

        /*
         * 
         */

        mapTo(BwdKeys.A, GLFW.GLFW_KEY_A);
        mapTo(BwdKeys.B, GLFW.GLFW_KEY_B);
        mapTo(BwdKeys.C, GLFW.GLFW_KEY_C);
        mapTo(BwdKeys.D, GLFW.GLFW_KEY_D);
        mapTo(BwdKeys.E, GLFW.GLFW_KEY_E);
        mapTo(BwdKeys.F, GLFW.GLFW_KEY_F);
        mapTo(BwdKeys.G, GLFW.GLFW_KEY_G);
        mapTo(BwdKeys.H, GLFW.GLFW_KEY_H);
        mapTo(BwdKeys.I, GLFW.GLFW_KEY_I);
        mapTo(BwdKeys.J, GLFW.GLFW_KEY_J);
        mapTo(BwdKeys.K, GLFW.GLFW_KEY_K);
        mapTo(BwdKeys.L, GLFW.GLFW_KEY_L);
        mapTo(BwdKeys.M, GLFW.GLFW_KEY_M);
        mapTo(BwdKeys.N, GLFW.GLFW_KEY_N);
        mapTo(BwdKeys.O, GLFW.GLFW_KEY_O);
        mapTo(BwdKeys.P, GLFW.GLFW_KEY_P);
        mapTo(BwdKeys.Q, GLFW.GLFW_KEY_Q);
        mapTo(BwdKeys.R, GLFW.GLFW_KEY_R);
        mapTo(BwdKeys.S, GLFW.GLFW_KEY_S);
        mapTo(BwdKeys.T, GLFW.GLFW_KEY_T);
        mapTo(BwdKeys.U, GLFW.GLFW_KEY_U);
        mapTo(BwdKeys.V, GLFW.GLFW_KEY_V);
        mapTo(BwdKeys.W, GLFW.GLFW_KEY_W);
        mapTo(BwdKeys.X, GLFW.GLFW_KEY_X);
        mapTo(BwdKeys.Y, GLFW.GLFW_KEY_Y);
        mapTo(BwdKeys.Z, GLFW.GLFW_KEY_Z);

        /*
         * 
         */
        
        mapTo(BwdKeys.DIGIT_0, GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_KP_0);
        mapTo(BwdKeys.DIGIT_1, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_KP_1);
        mapTo(BwdKeys.DIGIT_2, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_KP_2);
        mapTo(BwdKeys.DIGIT_3, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_KP_3);
        mapTo(BwdKeys.DIGIT_4, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_KP_4);
        mapTo(BwdKeys.DIGIT_5, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_KP_5);
        mapTo(BwdKeys.DIGIT_6, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_KP_6);
        mapTo(BwdKeys.DIGIT_7, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_KP_7);
        mapTo(BwdKeys.DIGIT_8, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_KP_8);
        mapTo(BwdKeys.DIGIT_9, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_KP_9);
        //
        mapTo(BwdKeys.DOT, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_KP_DECIMAL);

        /*
         * 
         */
        
        mapTo(BwdKeys.ESCAPE, GLFW.GLFW_KEY_ESCAPE);
        mapTo(BwdKeys.TAB, GLFW.GLFW_KEY_TAB);
        mapTo(BwdKeys.BACKSPACE, GLFW.GLFW_KEY_BACKSPACE);
        mapTo(BwdKeys.EQUALS, GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_EQUAL);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SLASH, GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_KP_DIVIDE);
        mapTo(BwdKeys.ASTERISK, GLFW.GLFW_KEY_KP_MULTIPLY);
        mapTo(BwdKeys.MINUS, GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT);
        mapTo(BwdKeys.PLUS, GLFW.GLFW_KEY_KP_ADD);
        mapTo(BwdKeys.ENTER, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER);
        
        /*
         * TODO lwjgl For some reason, whether NUM_LOCK is on or off,
         * LWJGL always uses GLFW_KEY_KP_0 .. GLFW_KEY_KP_9,
         * and GLFW_KEY_KP_DECIMAL.
         */
        
        mapTo(BwdKeys.HOME, GLFW.GLFW_KEY_HOME);
        mapTo(BwdKeys.END, GLFW.GLFW_KEY_END);
        //
        mapTo(BwdKeys.PAGE_UP, GLFW.GLFW_KEY_PAGE_UP);
        mapTo(BwdKeys.PAGE_DOWN, GLFW.GLFW_KEY_PAGE_DOWN);
        //
        mapTo(BwdKeys.LEFT, GLFW.GLFW_KEY_LEFT);
        mapTo(BwdKeys.UP, GLFW.GLFW_KEY_UP);
        mapTo(BwdKeys.RIGHT, GLFW.GLFW_KEY_RIGHT);
        mapTo(BwdKeys.DOWN, GLFW.GLFW_KEY_DOWN);
        //
        mapTo(BwdKeys.INSERT, GLFW.GLFW_KEY_INSERT);
        mapTo(BwdKeys.DELETE, GLFW.GLFW_KEY_DELETE);
        //
        noMatch(BwdKeys.CENTER);

        /*
         * 
         */
        
        mapTo(BwdKeys.PRINT_SCREEN, GLFW.GLFW_KEY_PRINT_SCREEN);
        mapTo(BwdKeys.PAUSE, GLFW.GLFW_KEY_PAUSE);

        /*
         * 
         */
        
        noMatch(BwdKeys.LEFT_PARENTHESIS);
        noMatch(BwdKeys.RIGHT_PARENTHESIS);
        mapTo(BwdKeys.LEFT_BRACKET, GLFW.GLFW_KEY_LEFT_BRACKET);
        mapTo(BwdKeys.RIGHT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET);
        noMatch(BwdKeys.LEFT_BRACE);
        noMatch(BwdKeys.RIGHT_BRACE);

        /*
         * 
         */
        
        noMatch(BwdKeys.LESS_THAN);
        noMatch(BwdKeys.GREATER_THAN);
        
        mapTo(BwdKeys.SINGLE_QUOTE, GLFW.GLFW_KEY_APOSTROPHE);
        noMatch(BwdKeys.DOUBLE_QUOTE);
        
        mapTo(BwdKeys.GRAVE_ACCENT, GLFW.GLFW_KEY_GRAVE_ACCENT);
        noMatch(BwdKeys.CIRCUMFLEX_ACCENT);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SPACE, GLFW.GLFW_KEY_SPACE);
        noMatch(BwdKeys.UNDERSCORE);
        mapTo(BwdKeys.COMMA, GLFW.GLFW_KEY_COMMA);
        mapTo(BwdKeys.SEMICOLON, GLFW.GLFW_KEY_SEMICOLON);
        noMatch(BwdKeys.COLON);
        noMatch(BwdKeys.VERTICAL_BAR);
        mapTo(BwdKeys.BACKSLASH, GLFW.GLFW_KEY_BACKSLASH);
        
        noMatch(BwdKeys.AMPERSAND);
        noMatch(BwdKeys.AT_SYMBOL);
        noMatch(BwdKeys.DOLLAR);
        noMatch(BwdKeys.HASH);
        noMatch(BwdKeys.PERCENT);
        
        noMatch(BwdKeys.QUESTION_MARK);
        noMatch(BwdKeys.EXCLAMATION_MARK);
        noMatch(BwdKeys.TILDE);
        
        /*
         * 
         */
        
        mapUnmappedBackingKeys(LwjglKeys.keyList());
    }
}
