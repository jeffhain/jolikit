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
package net.jolikit.bwd.impl.swt;

import java.util.Arrays;

import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.impl.utils.events.AbstractKeyConverter;

import org.eclipse.swt.SWT;

public class SwtKeyConverter extends AbstractKeyConverter<Integer> {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SwtKeyConverter() {
        
        mapTo(BwdKeys.NO_STATEMENT, SWT.NONE);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.NUM_LOCK, SWT.NUM_LOCK);
        mapTo(BwdKeys.CAPS_LOCK, SWT.CAPS_LOCK);
        mapTo(BwdKeys.SCROLL_LOCK, SWT.SCROLL_LOCK);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SHIFT, SWT.SHIFT);
        mapTo(BwdKeys.CONTROL, SWT.CONTROL);
        mapTo(BwdKeys.ALT, SWT.ALT);
        noMatch(BwdKeys.ALT_GRAPH); // Matches with (SWT.ALT, key location = SWT.RIGHT).
        // SWT doesn't even generate an event for MENU key. 
        noMatch(BwdKeys.META);
        mapTo(BwdKeys.SUPER, SWT.COMMAND);
        noMatch(BwdKeys.HYPER);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.F1, SWT.F1);
        mapTo(BwdKeys.F2, SWT.F2);
        mapTo(BwdKeys.F3, SWT.F3);
        mapTo(BwdKeys.F4, SWT.F4);
        mapTo(BwdKeys.F5, SWT.F5);
        mapTo(BwdKeys.F6, SWT.F6);
        mapTo(BwdKeys.F7, SWT.F7);
        mapTo(BwdKeys.F8, SWT.F8);
        mapTo(BwdKeys.F9, SWT.F9);
        mapTo(BwdKeys.F10, SWT.F10);
        mapTo(BwdKeys.F11, SWT.F11);
        mapTo(BwdKeys.F12, SWT.F12);

        /*
         * 
         */

        mapKeyToItsCp(BwdKeys.A);
        mapKeyToItsCp(BwdKeys.B);
        mapKeyToItsCp(BwdKeys.C);
        mapKeyToItsCp(BwdKeys.D);
        mapKeyToItsCp(BwdKeys.E);
        mapKeyToItsCp(BwdKeys.F);
        mapKeyToItsCp(BwdKeys.G);
        mapKeyToItsCp(BwdKeys.H);
        mapKeyToItsCp(BwdKeys.I);
        mapKeyToItsCp(BwdKeys.J);
        mapKeyToItsCp(BwdKeys.K);
        mapKeyToItsCp(BwdKeys.L);
        mapKeyToItsCp(BwdKeys.M);
        mapKeyToItsCp(BwdKeys.N);
        mapKeyToItsCp(BwdKeys.O);
        mapKeyToItsCp(BwdKeys.P);
        mapKeyToItsCp(BwdKeys.Q);
        mapKeyToItsCp(BwdKeys.R);
        mapKeyToItsCp(BwdKeys.S);
        mapKeyToItsCp(BwdKeys.T);
        mapKeyToItsCp(BwdKeys.U);
        mapKeyToItsCp(BwdKeys.V);
        mapKeyToItsCp(BwdKeys.W);
        mapKeyToItsCp(BwdKeys.X);
        mapKeyToItsCp(BwdKeys.Y);
        mapKeyToItsCp(BwdKeys.Z);

        /*
         * 
         */
        
        mapTo(BwdKeys.DIGIT_0, SWT.KEYPAD_0);
        mapTo(BwdKeys.DIGIT_1, SWT.KEYPAD_1);
        mapTo(BwdKeys.DIGIT_2, SWT.KEYPAD_2);
        mapTo(BwdKeys.DIGIT_3, SWT.KEYPAD_3);
        mapTo(BwdKeys.DIGIT_4, SWT.KEYPAD_4);
        mapTo(BwdKeys.DIGIT_5, SWT.KEYPAD_5);
        mapTo(BwdKeys.DIGIT_6, SWT.KEYPAD_6);
        mapTo(BwdKeys.DIGIT_7, SWT.KEYPAD_7);
        mapTo(BwdKeys.DIGIT_8, SWT.KEYPAD_8);
        mapTo(BwdKeys.DIGIT_9, SWT.KEYPAD_9);
        //
        mapTo(BwdKeys.DOT, SWT.KEYPAD_DECIMAL);

        /*
         * 
         */
        
        mapTo(BwdKeys.ESCAPE, (int) SWT.ESC);
        mapTo(BwdKeys.TAB, (int) SWT.TAB);
        mapTo(BwdKeys.BACKSPACE, (int) SWT.BS);
        mapTo(BwdKeys.EQUALS, SWT.KEYPAD_EQUAL);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SLASH, SWT.KEYPAD_DIVIDE);
        mapTo(BwdKeys.ASTERISK, SWT.KEYPAD_MULTIPLY);
        mapTo(BwdKeys.MINUS, SWT.KEYPAD_SUBTRACT);
        mapTo(BwdKeys.PLUS, SWT.KEYPAD_ADD);
        mapTo(BwdKeys.ENTER, (int) SWT.CR, SWT.KEYPAD_CR);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.HOME, SWT.HOME);
        mapTo(BwdKeys.END, SWT.END);
        //
        mapTo(BwdKeys.PAGE_UP, SWT.PAGE_UP);
        mapTo(BwdKeys.PAGE_DOWN, SWT.PAGE_DOWN);
        //
        mapTo(BwdKeys.LEFT, SWT.ARROW_LEFT);
        mapTo(BwdKeys.UP, SWT.ARROW_UP);
        mapTo(BwdKeys.RIGHT, SWT.ARROW_RIGHT);
        mapTo(BwdKeys.DOWN, SWT.ARROW_DOWN);
        //
        mapTo(BwdKeys.INSERT, SWT.INSERT);
        mapTo(BwdKeys.DELETE, (int) SWT.DEL);
        //
        noMatch(BwdKeys.CENTER);

        /*
         * 
         */
        
        mapTo(BwdKeys.PRINT_SCREEN, SWT.PRINT_SCREEN);
        mapTo(BwdKeys.PAUSE, SWT.PAUSE);

        /*
         * 
         */
        
        mapKeyToItsCp(BwdKeys.LEFT_PARENTHESIS);
        mapKeyToItsCp(BwdKeys.RIGHT_PARENTHESIS);
        mapKeyToItsCp(BwdKeys.LEFT_BRACKET);
        mapKeyToItsCp(BwdKeys.RIGHT_BRACKET);
        mapKeyToItsCp(BwdKeys.LEFT_BRACE);
        mapKeyToItsCp(BwdKeys.RIGHT_BRACE);

        /*
         * 
         */
        
        mapKeyToItsCp(BwdKeys.LESS_THAN);
        mapKeyToItsCp(BwdKeys.GREATER_THAN);
        
        mapKeyToItsCp(BwdKeys.SINGLE_QUOTE);
        mapKeyToItsCp(BwdKeys.DOUBLE_QUOTE);
        
        mapKeyToItsCp(BwdKeys.GRAVE_ACCENT);
        mapKeyToItsCp(BwdKeys.CIRCUMFLEX_ACCENT);
        
        /*
         * 
         */
        
        mapTo(BwdKeys.SPACE, (int) SWT.SPACE);
        mapKeyToItsCp(BwdKeys.UNDERSCORE);
        mapKeyToItsCp(BwdKeys.COMMA);
        mapKeyToItsCp(BwdKeys.SEMICOLON);
        mapKeyToItsCp(BwdKeys.COLON);
        mapKeyToItsCp(BwdKeys.VERTICAL_BAR);
        mapKeyToItsCp(BwdKeys.BACKSLASH);
        
        mapKeyToItsCp(BwdKeys.AMPERSAND);
        mapKeyToItsCp(BwdKeys.AT_SYMBOL);
        mapKeyToItsCp(BwdKeys.DOLLAR);
        mapKeyToItsCp(BwdKeys.HASH);
        mapKeyToItsCp(BwdKeys.PERCENT);
        
        mapKeyToItsCp(BwdKeys.QUESTION_MARK);
        mapKeyToItsCp(BwdKeys.EXCLAMATION_MARK);
        mapKeyToItsCp(BwdKeys.TILDE);
        
        /*
         * 
         */
        
        if (false) {
            /*
             * TODO swt In SWT class, it's not clear what is a key
             * and what is not.
             * We just don't bother figuring out additional keys.
             */
            mapUnmappedBackingKeys(Arrays.asList(0));
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * TODO swt SWT sometimes doesn't have constants for keys corresponding
     * to some (char) code points it can provide
     * (it does have only for: SWT.{BS|CR|DEL|ESC|LF|TAB|SPACE}),
     * in which cases we can't use mapTo(key, backingKey...).
     * This method allows to cover these cases, by mapping a key
     * to the corresponding code point if any, in case SWT provides
     * that code point.
     * 
     * @param key A key.
     */
    private void mapKeyToItsCp(int key) {
        final int cp = BwdKeys.codePointForKey(key);
        if (cp != -1) {
            this.mapTo(key, cp);
        }
    }
}
