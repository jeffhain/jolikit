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
package net.jolikit.bwd.api.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jolikit.bwd.api.utils.BwdUnicode;

/**
 * Basic keys that should be supported by each binding.
 * @see #BwdKeyEventPr
 * 
 * Bindings can support additional keys, as long as
 * they don't conflict with the values defined in this class.
 * 
 * Not using an enum class, to make it easier to deal transparently
 * with these default keys and with additional ones.
 * 
 * For keys that have an ASCII mapping, not trying to use the ASCII value,
 * and deliberately avoiding to do it, because it would cause holes in our
 * key values, because we don't want to force such a rule for additional keys
 * definition and thus it might not be the case for additional keys, and
 * because it could cause people to believe there is always a mapping and
 * accidentally use non-ASCII-mapped keys as ASCII values, or other kinds
 * of confusions.
 * It also allows us to order keys in a more consistent and logical manner
 * than they are ordered in ASCII.
 * 
 * Naming:
 * When multiple names are possible, we stick to one, generally following
 * Ascii enum convention, both for simplicity, and to avoid confusion
 * with multiple-parts names, which would require using another separator,
 * like "__", which would make things even longer.
 * @see #BwdUnicode
 * 
 * Note that keyboard keys often have multiple possible roles,
 * depending on which modifier keys are down, and various libraries
 * might refer to these keys using different roles, for example
 * some keyboards have a (1,&) key, which can be indicated as
 * either DIGIT_1 or AMPERSAND, depending on the backing library.
 * 
 * Dead keys:
 * Dead keys usage example: press '^' (dead) key, then press 'o' key,
 * and obtain a key typed event corresponding to a 'o' with a '^'
 * on top of it.
 * When such keys are pressed one time, some libraries (such as
 * AWT/Swing/JavaFX) emit a dead key event, some (such as
 * LWJGL3/JOGL/Allegro5/SDL2) a regular key event, and others
 * (like SWT/Qtj4) no key event at all.
 * Also, some (like JOGL/SDL2) produce a key typed event with
 * the proper Unicode character on first strike (so no dead key),
 * and others require a second key strike to produce a Unicode
 * character (allowing for the dead key feature).
 * To help reduce BWD inconsistencies across libraries, we just
 * don't deal with dead keys here.
 * In case of dead key event, bindings should just produce
 * a key event with either NO_STATEMENT or a more appropriate
 * value if it can be computed, or no event at all if
 * more desirable depending on use cases.
 * Anyway, the dead key feature doesn't scale well (!) with the
 * complexity of all Unicode range, so it's better to just ignore it.
 * 
 * Print screen key:
 * JavaFX/LWJGL3/SDL2 generate it, AWT/Swing/SWT/JOGL/Qt4/Allegro5 don't.
 * 
 * Language-specific keys:
 * There are none in this class, not to make it lean towards specific languages
 * (other than characters in [0..127] range), and to keep it generic and simple,
 * but they can always be dealt with through use of key codes (int values).
 * 
 * Key location:
 * For keys that can be at multiple locations, like left or right shift,
 * or arrows or digits in numeric pad or else where, we only have a single
 * value, for consistency in case some libraries don't provide a way
 * to discriminate between them, and not to depend on particular keyboards
 * layouts for these values.
 * @see #BwdKeyLocations
 */
public class BwdKeys {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    static final int MIN_KEY = 1;
    static final int MAX_KEY = 109;
    
    /**
     * Keeping some room for eventual new keys.
     */
    private static final int MIN_ADDITIONAL_KEY = 1024;

    private static final int NO_CODE_POINT = -1;

    private static final String[] NAME_BY_KEY = new String[MAX_KEY + 1];
    
    /**
     * Code point by key.
     * NO_CODE_POINT by default. 
     */
    private static final int[] CP_BY_KEY = new int[MAX_KEY + 1];
    static {
        for (int i = 0; i < CP_BY_KEY.length; i++) {
            CP_BY_KEY[i] = NO_CODE_POINT;
        }
    }
    
    /**
     * Key by code point.
     * 0 = NO_STATEMENT by default.
     */
    private static final int[] KEY_BY_CP = new int[128];
    
    /*
     * 
     */
    
    /**
     * Not a key, or a key for which no specific value could be computed.
     */
    public static final int NO_STATEMENT = 0;     static { setInfos(NO_STATEMENT, "NO_STATEMENT"); }
    
    /*
     * Locks.
     */
    
    /**
     * On Mac, can be mapped to "clear" key, but for some reason,
     * for a lot of libraries, "clear" key is instead what we call CENTER key
     * (numeric pad's 5 digit key when NUM_LOCK is off).
     * 
     * About Mac's clear key, from the web:
     * "In general, its function is to clear the selection, much like pressing
     * Delete or Forward Delete, but unlike with the Delete keys, it will not do
     * anything without something already selected. Luckily, in the Finder,
     * it selects the last item alphabetically, rather than "clearing" selected
     * files and folders, which could be bad.
     * It functions as a normal Num Lock key when running Windows or Linux on a
     * Mac, either through a VM or natively. This can be frustrating though,
     * because even old Apple USB keyboards that have "Num Lock" printed as
     * "alt" text on the Clear key don't have an indicator light for that key
     * (though older ADB keyboards did).
     * A long time ago, in a Mac OS far far away, I recall occasionally coming
     * across Mac apps that used the Clear key as a regular Num Lock key,
     * including a very old version of Excel. I don't recall seeing any Mac apps
     * use it in this way in recent history, perhaps not since the release of
     * OS X (or the obsolescence of the ADB keyboards)."
     */
    public static final int NUM_LOCK = MIN_KEY;     static { setInfos(NUM_LOCK, "NUM_LOCK"); }
    public static final int CAPS_LOCK = 2;     static { setInfos(CAPS_LOCK, "CAPS_LOCK"); }
    public static final int SCROLL_LOCK = 3;     static { setInfos(SCROLL_LOCK, "SCROLL_LOCK"); }

    /*
     * Modifiers.
     * 
     * For modifier keys other than shift and control,
     * which are usually present, the following mappings can be used
     * (inspiration source:
     * https://askubuntu.com/questions/19558/what-are-the-meta-super-and-hyper-keys):
     * 
     * keyboard \ key |   ALT   | ALT_GRAPH |  META   |  SUPER  | HYPER  | ([key mapping]'s layout, around space and control keys)
     * ---------------+---------+-----------+---------+---------+--------+
     * Lisp           |    -    |     -     |  META   |  SUPER  | HYPER  | ([HYPER]-[SUPER]-[META]-ctrl-space-ctrl-[META]-[SUPER]-[HYPER])
     * ---------------+---------+-----------+---------+---------+--------+
     * Windows        |   ALT   | ALT_GRAPH |  menu   | windows |   -    | (ctrl-[windows/SUPER]-[ALT]-space-[ALT_GRAPH]-[windows/SUPER]-[menu/META]-ctrl)
     * ---------------+---------+-----------+---------+---------+--------+
     * Mac            | option  |     -     |    -    | command |   -    | (ctrl-[option/ALT]-[command/SUPER]-space-[command/SUPER]-[option/ALT]-ctrl)
     * ---------------+---------+-----------+---------+---------+--------+
     * Sun            |   ALT   | ALT_GRAPH |  META   | compose |   -    | ([META]-[ALT]-space-[ALT_GRAPH]-[META]-[compose/SUPER])
     * ---------------+---------+-----------+---------+---------+--------+
     */
    
    public static final int SHIFT = 4;     static { setInfos(SHIFT, "SHIFT"); }
    public static final int CONTROL = 5;     static { setInfos(CONTROL, "CONTROL"); }
    /**
     * On Mac, can be mapped to "option" key.
     * Also called "left alt".
     */
    public static final int ALT = 6;     static { setInfos(ALT, "ALT"); }
    /**
     * Also called "right alt".
     */
    public static final int ALT_GRAPH = 7;     static { setInfos(ALT_GRAPH, "ALT_GRAPH"); }
    /**
     * On Windows, can be mapped to "menu" (or "application") key.
     */
    public static final int META = 8;     static { setInfos(META, "META"); }
    /**
     * On Windows, can be mapped to "windows" key.
     * On Mac, can be mapped to "command" key.
     * On Sun, can be mapped to "compose" key (...which is not a modifier).
     */
    public static final int SUPER = 9;     static { setInfos(SUPER, "SUPER"); }
    /**
     * Not mapped in any of the original bindings,
     * but keeping it around to allow for more mappable modifiers,
     * and as tribute to now forgotten Lisp keyboards,
     * and because its name fits well after META and SUPER.
     */
    public static final int HYPER = 10;     static { setInfos(HYPER, "HYPER"); }

    /*
     * Function keys.
     * 
     * Allegro5 and SDL2 go up to F12,
     * SWT up to F20,
     * AWT/Swing, JavaFX and JOGL up to F24,
     * LWJGL3 up to F25, Qt4 up to F35.
     * 
     * Handy: value = 10 + K
     */
    
    public static final int F1 = 11;     static { setInfos(F1, "F1"); }
    public static final int F2 = 12;     static { setInfos(F2, "F2"); }
    public static final int F3 = 13;     static { setInfos(F3, "F3"); }
    public static final int F4 = 14;     static { setInfos(F4, "F4"); }
    public static final int F5 = 15;     static { setInfos(F5, "F5"); }
    public static final int F6 = 16;     static { setInfos(F6, "F6"); }
    public static final int F7 = 17;     static { setInfos(F7, "F7"); }
    public static final int F8 = 18;     static { setInfos(F8, "F8"); }
    public static final int F9 = 19;     static { setInfos(F9, "F9"); }
    public static final int F10 = 20;     static { setInfos(F10, "F10"); }
    public static final int F11 = 21;     static { setInfos(F11, "F11"); }
    public static final int F12 = 22;     static { setInfos(F12, "F12"); }

    /*
     * Alphabetic keys.
     */
    
    public static final int A = 23;     static { setInfos(A, "A", BwdUnicode.a); }
    public static final int B = 24;     static { setInfos(B, "B", BwdUnicode.b); }
    public static final int C = 25;     static { setInfos(C, "C", BwdUnicode.c); }
    public static final int D = 26;     static { setInfos(D, "D", BwdUnicode.d); }
    public static final int E = 27;     static { setInfos(E, "E", BwdUnicode.e); }
    public static final int F = 28;     static { setInfos(F, "F", BwdUnicode.f); }
    public static final int G = 29;     static { setInfos(G, "G", BwdUnicode.g); }
    public static final int H = 30;     static { setInfos(H, "H", BwdUnicode.h); }
    public static final int I = 31;     static { setInfos(I, "I", BwdUnicode.i); }
    public static final int J = 32;     static { setInfos(J, "J", BwdUnicode.j); }
    public static final int K = 33;     static { setInfos(K, "K", BwdUnicode.k); }
    public static final int L = 34;     static { setInfos(L, "L", BwdUnicode.l); }
    public static final int M = 35;     static { setInfos(M, "M", BwdUnicode.m); }
    public static final int N = 36;     static { setInfos(N, "N", BwdUnicode.n); }
    public static final int O = 37;     static { setInfos(O, "O", BwdUnicode.o); }
    public static final int P = 38;     static { setInfos(P, "P", BwdUnicode.p); }
    public static final int Q = 39;     static { setInfos(Q, "Q", BwdUnicode.q); }
    public static final int R = 40;     static { setInfos(R, "R", BwdUnicode.r); }
    public static final int S = 41;     static { setInfos(S, "S", BwdUnicode.s); }
    public static final int T = 42;     static { setInfos(T, "T", BwdUnicode.t); }
    public static final int U = 43;     static { setInfos(U, "U", BwdUnicode.u); }
    public static final int V = 44;     static { setInfos(V, "V", BwdUnicode.v); }
    public static final int W = 45;     static { setInfos(W, "W", BwdUnicode.w); }
    public static final int X = 46;     static { setInfos(X, "X", BwdUnicode.x); }
    public static final int Y = 47;     static { setInfos(Y, "Y", BwdUnicode.y); }
    public static final int Z = 49;     static { setInfos(Z, "Z", BwdUnicode.z); }

    /*
     * Keys that can be in numeric pad when NUM_LOCK is on.
     * 
     * Handy: value = 50 + digit
     */
    
    public static final int DIGIT_0 = 50;     static { setInfos(DIGIT_0, "DIGIT_0", BwdUnicode.ZERO); }
    public static final int DIGIT_1 = 51;     static { setInfos(DIGIT_1, "DIGIT_1", BwdUnicode.ONE); }
    public static final int DIGIT_2 = 52;     static { setInfos(DIGIT_2, "DIGIT_2", BwdUnicode.TWO); }
    public static final int DIGIT_3 = 53;     static { setInfos(DIGIT_3, "DIGIT_3", BwdUnicode.THREE); }
    public static final int DIGIT_4 = 54;     static { setInfos(DIGIT_4, "DIGIT_4", BwdUnicode.FOUR); }
    public static final int DIGIT_5 = 55;     static { setInfos(DIGIT_5, "DIGIT_5", BwdUnicode.FIVE); }
    public static final int DIGIT_6 = 56;     static { setInfos(DIGIT_6, "DIGIT_6", BwdUnicode.SIX); }
    public static final int DIGIT_7 = 57;     static { setInfos(DIGIT_7, "DIGIT_7", BwdUnicode.SEVEN); }
    public static final int DIGIT_8 = 58;     static { setInfos(DIGIT_8, "DIGIT_8", BwdUnicode.HEIGHT); }
    public static final int DIGIT_9 = 59;     static { setInfos(DIGIT_9, "DIGIT_9", BwdUnicode.NINE); }
    //
    /**
     * Also called "period" (american english)
     * or "full stop" (commonwealth english),
     * or "decimal point", as the decimal separator in numbers.
     */
    public static final int DOT = 60;     static { setInfos(DOT, "DOT", BwdUnicode.DOT); }
    
    /*
     * Keys that can be in numeric pad, whether NUM_LOCK is on or off.
     */
    
    public static final int ESCAPE = 61;     static { setInfos(ESCAPE, "ESCAPE", BwdUnicode.ESC); }
    public static final int TAB = 62;     static { setInfos(TAB, "TAB", BwdUnicode.HT); }
    public static final int BACKSPACE = 63;     static { setInfos(BACKSPACE, "BACKSPACE", BwdUnicode.BACKSPACE); }
    public static final int EQUALS = 64;     static { setInfos(EQUALS, "EQUALS", BwdUnicode.EQUALS); }
    //
    /**
     * Also called "divide".
     * It's a key that does a slash,
     * not Unicode's division sign.
     */
    public static final int SLASH = 65;     static { setInfos(SLASH, "SLASH", BwdUnicode.SLASH); }
    /**
     * Also called "multiply".
     * It's a key that does an asterisk,
     * not Unicode's multiplication sign.
     */
    public static final int ASTERISK = 66;     static { setInfos(ASTERISK, "ASTERISK", BwdUnicode.ASTERISK); }
    /**
     * Also called "subtract" or "hyphen".
     */
    public static final int MINUS = 67;     static { setInfos(MINUS, "MINUS", BwdUnicode.MINUS); }
    /**
     * Also called "add", but "plus" is much better:
     * - Is consistent with using "minus" and not "remove" for '-'.
     * - Is more general and pure: matches with the idea of "more"
     *   (as for an expandable node in a tree), and doesnt
     *   imply some kind of mutability.
     */
    public static final int PLUS = 68;     static { setInfos(PLUS, "PLUS", BwdUnicode.PLUS); }
    //
    public static final int ENTER = 69;     static { setInfos(ENTER, "ENTER", BwdUnicode.CR); }
    
    /*
     * Keys that can be in numeric pad when NUM_LOCK is off.
     */
    
    /**
     * From the web:
     * "The Home key is commonly found on computer keyboards.
     * The key has the opposite effect of the End key."
     */
    public static final int HOME = 70;     static { setInfos(HOME, "HOME"); }
    public static final int END = 71;     static { setInfos(END, "END"); }
    //
    public static final int PAGE_UP = 72;     static { setInfos(PAGE_UP, "PAGE_UP"); }
    public static final int PAGE_DOWN = 73;     static { setInfos(PAGE_DOWN, "PAGE_DOWN"); }
    //
    public static final int LEFT = 74;     static { setInfos(LEFT, "LEFT"); }
    public static final int UP = 75;     static { setInfos(UP, "UP"); }
    public static final int RIGHT = 76;     static { setInfos(RIGHT, "RIGHT"); }
    public static final int DOWN = 77;     static { setInfos(DOWN, "DOWN"); }
    //
    public static final int INSERT = 78;     static { setInfos(INSERT, "INSERT"); }
    public static final int DELETE = 79;     static { setInfos(DELETE, "DELETE", BwdUnicode.DELETE); }
    //
    /**
     * Corresponds to 5 digit key, which is at the center of numeric pad
     * (hence the name), when NUM_LOCK is off.
     * Also called "begin" (cf. http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4850137)
     * (why "begin"? it causes confusion with HOME key, which is the actual opposite of END key),
     * or also "clear" in various libraries - even though "clear" can also mean NUM_LOCK...
     */
    public static final int CENTER = 80;     static { setInfos(CENTER, "CENTER"); }
    
    /*
     * 
     */
    
    public static final int PRINT_SCREEN = 81;     static { setInfos(PRINT_SCREEN, "PRINT_SCREEN"); }
    public static final int PAUSE = 82;     static { setInfos(PAUSE, "PAUSE"); }

    /*
     * 
     */
    
    public static final int LEFT_PARENTHESIS = 83;     static { setInfos(LEFT_PARENTHESIS, "LEFT_PARENTHESIS", BwdUnicode.LEFT_PARENTHESIS); }
    public static final int RIGHT_PARENTHESIS = 84;     static { setInfos(RIGHT_PARENTHESIS, "RIGHT_PARENTHESIS", BwdUnicode.RIGHT_PARENTHESIS); }
    public static final int LEFT_BRACKET = 85;     static { setInfos(LEFT_BRACKET, "LEFT_BRACKET", BwdUnicode.LEFT_BRACKET); }
    public static final int RIGHT_BRACKET = 86;     static { setInfos(RIGHT_BRACKET, "RIGHT_BRACKET", BwdUnicode.RIGHT_BRACKET); }
    /**
     * Also called "left curly bracket".
     */
    public static final int LEFT_BRACE = 87;     static { setInfos(LEFT_BRACE, "LEFT_BRACE", BwdUnicode.LEFT_BRACE); }
    /**
     * Also called "right curly bracket".
     */
    public static final int RIGHT_BRACE = 88;     static { setInfos(RIGHT_BRACE, "RIGHT_BRACE", BwdUnicode.RIGHT_BRACE); }

    /*
     * 
     */
    
    public static final int LESS_THAN = 89;     static { setInfos(LESS_THAN, "LESS_THAN", BwdUnicode.LESS_THAN); }
    public static final int GREATER_THAN = 90;     static { setInfos(GREATER_THAN, "GREATER_THAN", BwdUnicode.GREATER_THAN); }
    
    /**
     * Also called apostrophe. Quoteright is a deprecated synonym.
     */
    public static final int SINGLE_QUOTE = 91;     static { setInfos(SINGLE_QUOTE, "SINGLE_QUOTE", BwdUnicode.SINGLE_QUOTE); }
    public static final int DOUBLE_QUOTE = 92;     static { setInfos(DOUBLE_QUOTE, "DOUBLE_QUOTE", BwdUnicode.DOUBLE_QUOTE); }
    
    /**
     * Also called grave, back quote, left quote, open quote, or push.
     * NB: Must not be confused with acute accent, which is vertically
     * symmetrical to this one.
     */
    public static final int GRAVE_ACCENT = 93;     static { setInfos(GRAVE_ACCENT, "GRAVE_ACCENT", BwdUnicode.GRAVE_ACCENT); }
    public static final int CIRCUMFLEX_ACCENT = 94;     static { setInfos(CIRCUMFLEX_ACCENT, "CIRCUMFLEX_ACCENT", BwdUnicode.CIRCUMFLEX_ACCENT); }
    
    /*
     * The order for these keys is not random:
     * they are grouped together per sort of kinds,
     * and put next to each other pet sort of similarities.
     * Also:
     * - Firsts are space (void) and underscore (space-like separator),
     *   and lasts are exclamation mark (not) and tilde (binary not),
     *   meaning things start and end with sort of nothingness (obviously!).
     * - '&' (and) is in the middle (to "and" left and right),
     *   and escaped (i.e. maybe no "anding").
     * [ _,;:|\&@$#%?!~]
     */
    
    public static final int SPACE = 95;     static { setInfos(SPACE, "SPACE", BwdUnicode.SPACE); }
    public static final int UNDERSCORE = 96;     static { setInfos(UNDERSCORE, "UNDERSCORE", BwdUnicode.UNDERSCORE); }
    public static final int COMMA = 97;     static { setInfos(COMMA, "COMMA", BwdUnicode.COMMA); }
    public static final int SEMICOLON = 98;     static { setInfos(SEMICOLON, "SEMICOLON", BwdUnicode.SEMICOLON); }
    public static final int COLON = 99;     static { setInfos(COLON, "COLON", BwdUnicode.COLON); }
    /**
     * Also called "pipe".
     */
    public static final int VERTICAL_BAR = 100;     static { setInfos(VERTICAL_BAR, "VERTICAL_BAR", BwdUnicode.VERTICAL_BAR); }
    public static final int BACKSLASH = 101;     static { setInfos(BACKSLASH, "BACKSLASH", BwdUnicode.BACKSLASH); }
    
    public static final int AMPERSAND = 102;     static { setInfos(AMPERSAND, "AMPERSAND", BwdUnicode.AMPERSAND); }
    public static final int AT_SYMBOL = 103;     static { setInfos(AT_SYMBOL, "AT_SYMBOL", BwdUnicode.AT_SYMBOL); }
    /**
     * Not including it because it's a currency,
     * but because it's a basic character in computing.
     */
    public static final int DOLLAR = 104;     static { setInfos(DOLLAR, "DOLLAR", BwdUnicode.DOLLAR); }
    public static final int HASH = 105;     static { setInfos(HASH, "HASH", BwdUnicode.HASH); }
    public static final int PERCENT = 106;     static { setInfos(PERCENT, "PERCENT", BwdUnicode.PERCENT); }
    
    public static final int QUESTION_MARK = 107;     static { setInfos(QUESTION_MARK, "QUESTION_MARK", BwdUnicode.QUESTION_MARK); }
    public static final int EXCLAMATION_MARK = 108;     static { setInfos(EXCLAMATION_MARK, "EXCLAMATION_MARK", BwdUnicode.EXCLAMATION_MARK); }
    public static final int TILDE = MAX_KEY;     static { setInfos(TILDE, "TILDE", BwdUnicode.TILDE); }
    
    /*
     * 
     */

    private static final List<Integer> KEY_LIST;
    static {
        final List<Integer> list = new ArrayList<Integer>();
        // Our values are contiguous.
        for (int key = MIN_KEY; key <= MAX_KEY; key++) {
            list.add(key);
        }
        KEY_LIST = Collections.unmodifiableList(list);
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Methods rather than constants, to avoid confusion with constants.
     */
    
    /**
     * @return An unmodifiable list of the keys defined in this class
     *         (NO_STATEMENT excluded), in increasing order. 
     */
    public static List<Integer> keyList() {
        return KEY_LIST;
    }
    
    /**
     * @return Min value for eventual additional keys that can be generated
     *         by a binding.
     */
    public static int minAdditionalKey() {
        return MIN_ADDITIONAL_KEY;
    }
    
    /*
     * 
     */
    
    /**
     * @param key A key. Can be any int value.
     * @return A string representation of the specified key.
     */
    public static String toString(int key) {
        String name = null;
        if ((key >= 0) && (key < NAME_BY_KEY.length)) {
            name = NAME_BY_KEY[key];
        }
        if (name == null) {
            name = "UNKNOWN_KEY(" + key + ")";
        }
        return name;
    }
    
    /*
     * 
     */
    
    /**
     * @param codePoint A code point. Can be any int value.
     * @return The key defined in this class corresponding to the specified
     *         code point value, or NO_STATEMENT if none.
     */
    public static int keyForCodePoint(int codePoint) {
        int key = NO_STATEMENT;
        if ((codePoint >= 0) && (codePoint < KEY_BY_CP.length)) {
            key = KEY_BY_CP[codePoint];
        }
        return key;
    }
    
    /**
     * @param key A key. Can be any value.
     * @return The code point corresponding to the specified key
     *         defined in this class, or -1 if none.
     */
    public static int codePointForKey(int key) {
        int codePoint = NO_CODE_POINT;
        if ((key >= 0) && (key < CP_BY_KEY.length)) {
            codePoint = CP_BY_KEY[key];
        }
        return codePoint;
    }

    /*
     * 
     */
    
    /**
     * @return True if the specified key is a function key defined in this class.
     */
    public static boolean isFunctionKey(int key) {
        return (key >= F1) && (key <= F12);
    }

    /**
     * @return True if the specified key is a navigation key, possibly in keypad,
     *         defined in this class.
     */
    public static boolean isNavigationKey(int key) {
        return (key >= HOME) && (key <= DOWN);
    }

    /**
     * @return True if the specified key is an arrow key, possibly in keypad,
     *         defined in this class.
     */
    public static boolean isArrowKey(int key) {
        return (key >= LEFT) && (key <= DOWN);
    }

    /**
     * @return True if the specified key is a modifier key (i.e. could act as a modifier),
     *         defined in this class.
     */
    public static boolean isModifierKey(int key) {
        return (key >= SHIFT) && (key <= HYPER);
    }

    /**
     * @return True if the specified key is a letter key defined in this class.
     */
    public static boolean isLetterKey(int key) {
        return (key >= A) && (key <= Z);
    }

    /**
     * @return True if the specified key is a digit (0-9) key, possibly in keypad,
     *         defined in this class.
     */
    public static boolean isDigitKey(int key) {
        return (key >= DIGIT_0) && (key <= DIGIT_9);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private BwdKeys() {
    }
    
    private static void setInfos(int key, String name) {
        NAME_BY_KEY[key] = name;
    }
    
    private static void setInfos(int key, String name, int codePoint) {
        setInfos(key, name);
        CP_BY_KEY[key] = codePoint;
        KEY_BY_CP[codePoint] = key;
    }
}
