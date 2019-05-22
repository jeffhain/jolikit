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
package net.jolikit.bwd.impl.awt;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keys defined in AWT.
 * 
 * @see #java.awt.event.KeyEvent
 */
public class AwtKeys {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final List<Integer> KEY_LIST = Collections.unmodifiableList(newKeyList());
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return An unmodifiable list of all keys defined in AWT.
     */
    public static List<Integer> keyList() {
        return KEY_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private AwtKeys() {
    }
    
    private static List<Integer> newKeyList() {
        final List<Integer> list = new ArrayList<Integer>();
        list.add(KeyEvent.VK_ENTER);
        list.add(KeyEvent.VK_BACK_SPACE);
        list.add(KeyEvent.VK_TAB);
        list.add(KeyEvent.VK_CANCEL);
        list.add(KeyEvent.VK_CLEAR);
        list.add(KeyEvent.VK_SHIFT);
        list.add(KeyEvent.VK_CONTROL);
        list.add(KeyEvent.VK_ALT);
        list.add(KeyEvent.VK_PAUSE);
        list.add(KeyEvent.VK_CAPS_LOCK);
        list.add(KeyEvent.VK_ESCAPE);
        list.add(KeyEvent.VK_SPACE);
        list.add(KeyEvent.VK_PAGE_UP);
        list.add(KeyEvent.VK_PAGE_DOWN);
        list.add(KeyEvent.VK_END);
        list.add(KeyEvent.VK_HOME);
        list.add(KeyEvent.VK_LEFT);
        list.add(KeyEvent.VK_UP);
        list.add(KeyEvent.VK_RIGHT);
        list.add(KeyEvent.VK_DOWN);
        list.add(KeyEvent.VK_COMMA);
        list.add(KeyEvent.VK_MINUS);
        list.add(KeyEvent.VK_PERIOD);
        list.add(KeyEvent.VK_SLASH);
        list.add(KeyEvent.VK_0);
        list.add(KeyEvent.VK_1);
        list.add(KeyEvent.VK_2);
        list.add(KeyEvent.VK_3);
        list.add(KeyEvent.VK_4);
        list.add(KeyEvent.VK_5);
        list.add(KeyEvent.VK_6);
        list.add(KeyEvent.VK_7);
        list.add(KeyEvent.VK_8);
        list.add(KeyEvent.VK_9);
        list.add(KeyEvent.VK_SEMICOLON);
        list.add(KeyEvent.VK_EQUALS);
        list.add(KeyEvent.VK_A);
        list.add(KeyEvent.VK_B);
        list.add(KeyEvent.VK_C);
        list.add(KeyEvent.VK_D);
        list.add(KeyEvent.VK_E);
        list.add(KeyEvent.VK_F);
        list.add(KeyEvent.VK_G);
        list.add(KeyEvent.VK_H);
        list.add(KeyEvent.VK_I);
        list.add(KeyEvent.VK_J);
        list.add(KeyEvent.VK_K);
        list.add(KeyEvent.VK_L);
        list.add(KeyEvent.VK_M);
        list.add(KeyEvent.VK_N);
        list.add(KeyEvent.VK_O);
        list.add(KeyEvent.VK_P);
        list.add(KeyEvent.VK_Q);
        list.add(KeyEvent.VK_R);
        list.add(KeyEvent.VK_S);
        list.add(KeyEvent.VK_T);
        list.add(KeyEvent.VK_U);
        list.add(KeyEvent.VK_V);
        list.add(KeyEvent.VK_W);
        list.add(KeyEvent.VK_X);
        list.add(KeyEvent.VK_Y);
        list.add(KeyEvent.VK_Z);
        list.add(KeyEvent.VK_OPEN_BRACKET);
        list.add(KeyEvent.VK_BACK_SLASH);
        list.add(KeyEvent.VK_CLOSE_BRACKET);
        list.add(KeyEvent.VK_NUMPAD0);
        list.add(KeyEvent.VK_NUMPAD1);
        list.add(KeyEvent.VK_NUMPAD2);
        list.add(KeyEvent.VK_NUMPAD3);
        list.add(KeyEvent.VK_NUMPAD4);
        list.add(KeyEvent.VK_NUMPAD5);
        list.add(KeyEvent.VK_NUMPAD6);
        list.add(KeyEvent.VK_NUMPAD7);
        list.add(KeyEvent.VK_NUMPAD8);
        list.add(KeyEvent.VK_NUMPAD9);
        list.add(KeyEvent.VK_MULTIPLY);
        list.add(KeyEvent.VK_ADD);
        list.add(KeyEvent.VK_SEPARATER);
        list.add(KeyEvent.VK_SEPARATOR);
        list.add(KeyEvent.VK_SUBTRACT);
        list.add(KeyEvent.VK_DECIMAL);
        list.add(KeyEvent.VK_DIVIDE);
        list.add(KeyEvent.VK_DELETE);
        list.add(KeyEvent.VK_NUM_LOCK);
        list.add(KeyEvent.VK_SCROLL_LOCK);
        list.add(KeyEvent.VK_F1);
        list.add(KeyEvent.VK_F2);
        list.add(KeyEvent.VK_F3);
        list.add(KeyEvent.VK_F4);
        list.add(KeyEvent.VK_F5);
        list.add(KeyEvent.VK_F6);
        list.add(KeyEvent.VK_F7);
        list.add(KeyEvent.VK_F8);
        list.add(KeyEvent.VK_F9);
        list.add(KeyEvent.VK_F10);
        list.add(KeyEvent.VK_F11);
        list.add(KeyEvent.VK_F12);
        list.add(KeyEvent.VK_F13);
        list.add(KeyEvent.VK_F14);
        list.add(KeyEvent.VK_F15);
        list.add(KeyEvent.VK_F16);
        list.add(KeyEvent.VK_F17);
        list.add(KeyEvent.VK_F18);
        list.add(KeyEvent.VK_F19);
        list.add(KeyEvent.VK_F20);
        list.add(KeyEvent.VK_F21);
        list.add(KeyEvent.VK_F22);
        list.add(KeyEvent.VK_F23);
        list.add(KeyEvent.VK_F24);
        list.add(KeyEvent.VK_PRINTSCREEN);
        list.add(KeyEvent.VK_INSERT);
        list.add(KeyEvent.VK_HELP);
        list.add(KeyEvent.VK_META);
        list.add(KeyEvent.VK_BACK_QUOTE);
        list.add(KeyEvent.VK_QUOTE);
        list.add(KeyEvent.VK_KP_UP);
        list.add(KeyEvent.VK_KP_DOWN);
        list.add(KeyEvent.VK_KP_LEFT);
        list.add(KeyEvent.VK_KP_RIGHT);
        list.add(KeyEvent.VK_DEAD_GRAVE);
        list.add(KeyEvent.VK_DEAD_ACUTE);
        list.add(KeyEvent.VK_DEAD_CIRCUMFLEX);
        list.add(KeyEvent.VK_DEAD_TILDE);
        list.add(KeyEvent.VK_DEAD_MACRON);
        list.add(KeyEvent.VK_DEAD_BREVE);
        list.add(KeyEvent.VK_DEAD_ABOVEDOT);
        list.add(KeyEvent.VK_DEAD_DIAERESIS);
        list.add(KeyEvent.VK_DEAD_ABOVERING);
        list.add(KeyEvent.VK_DEAD_DOUBLEACUTE);
        list.add(KeyEvent.VK_DEAD_CARON);
        list.add(KeyEvent.VK_DEAD_CEDILLA);
        list.add(KeyEvent.VK_DEAD_OGONEK);
        list.add(KeyEvent.VK_DEAD_IOTA);
        list.add(KeyEvent.VK_DEAD_VOICED_SOUND);
        list.add(KeyEvent.VK_DEAD_SEMIVOICED_SOUND);
        list.add(KeyEvent.VK_AMPERSAND);
        list.add(KeyEvent.VK_ASTERISK);
        list.add(KeyEvent.VK_QUOTEDBL);
        list.add(KeyEvent.VK_LESS);
        list.add(KeyEvent.VK_GREATER);
        list.add(KeyEvent.VK_BRACELEFT);
        list.add(KeyEvent.VK_BRACERIGHT);
        list.add(KeyEvent.VK_AT);
        list.add(KeyEvent.VK_COLON);
        list.add(KeyEvent.VK_CIRCUMFLEX);
        list.add(KeyEvent.VK_DOLLAR);
        list.add(KeyEvent.VK_EURO_SIGN);
        list.add(KeyEvent.VK_EXCLAMATION_MARK);
        list.add(KeyEvent.VK_INVERTED_EXCLAMATION_MARK);
        list.add(KeyEvent.VK_LEFT_PARENTHESIS);
        list.add(KeyEvent.VK_NUMBER_SIGN);
        list.add(KeyEvent.VK_PLUS);
        list.add(KeyEvent.VK_RIGHT_PARENTHESIS);
        list.add(KeyEvent.VK_UNDERSCORE);
        list.add(KeyEvent.VK_WINDOWS);
        list.add(KeyEvent.VK_CONTEXT_MENU);
        list.add(KeyEvent.VK_FINAL);
        list.add(KeyEvent.VK_CONVERT);
        list.add(KeyEvent.VK_NONCONVERT);
        list.add(KeyEvent.VK_ACCEPT);
        list.add(KeyEvent.VK_MODECHANGE);
        list.add(KeyEvent.VK_KANA);
        list.add(KeyEvent.VK_KANJI);
        list.add(KeyEvent.VK_ALPHANUMERIC);
        list.add(KeyEvent.VK_KATAKANA);
        list.add(KeyEvent.VK_HIRAGANA);
        list.add(KeyEvent.VK_FULL_WIDTH);
        list.add(KeyEvent.VK_HALF_WIDTH);
        list.add(KeyEvent.VK_ROMAN_CHARACTERS);
        list.add(KeyEvent.VK_ALL_CANDIDATES);
        list.add(KeyEvent.VK_PREVIOUS_CANDIDATE);
        list.add(KeyEvent.VK_CODE_INPUT);
        list.add(KeyEvent.VK_JAPANESE_KATAKANA);
        list.add(KeyEvent.VK_JAPANESE_HIRAGANA);
        list.add(KeyEvent.VK_JAPANESE_ROMAN);
        list.add(KeyEvent.VK_KANA_LOCK);
        list.add(KeyEvent.VK_INPUT_METHOD_ON_OFF);
        list.add(KeyEvent.VK_CUT);
        list.add(KeyEvent.VK_COPY);
        list.add(KeyEvent.VK_PASTE);
        list.add(KeyEvent.VK_UNDO);
        list.add(KeyEvent.VK_AGAIN);
        list.add(KeyEvent.VK_FIND);
        list.add(KeyEvent.VK_PROPS);
        list.add(KeyEvent.VK_STOP);
        list.add(KeyEvent.VK_COMPOSE);
        list.add(KeyEvent.VK_ALT_GRAPH);
        list.add(KeyEvent.VK_BEGIN);
        return list;
    }
}
