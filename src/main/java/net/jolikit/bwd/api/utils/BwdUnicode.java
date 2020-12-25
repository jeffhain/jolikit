/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.api.utils;

import net.jolikit.lang.NbrsUtils;

/**
 * Class to help deal with Unicode characters, i.e. code points,
 * which can appear in key events, and are the elementary unit
 * for text drawing.
 * 
 * This class is not designed to replace a comprehensive Unicode library,
 * but only to be able to deal with most basic use cases when not using one,
 * which is why it is prefixed to avoid name collision.
 * 
 * Contains code points corresponding to the first two blocks of Unicode:
 * - Basic Latin ("ASCII", values in [0,127]),
 * - C1 Controls and Latin-1 supplement ("Latin-1", values in [128,255]),
 * as well as some remarkable code points above that range.
 * 
 * For parentheses, braces and brackets, using left/right terms,
 * not open/close, not to assume that the writing/reading goes
 * left-to-right.
 * 
 * Also contains some static helper methods.
 * 
 * For code points, not using an enum, because as often in Java
 * it quickly gets in the way and makes things complicated:
 * for example, that would require to define and use some "codePoint()"
 * and "valueOfCodePoint(int)" methods to convert between enum values
 * and code points, etc.
 * 
 * @see #LangUtils.stringOfCodePoint(int)
 * 
 * Stuffs about Unicode:
 * - Defines code points (mathematical integers) and associated glyphs,
 *   or functionalities for control characters or such.
 *   Does not define the encoding (how to represent that integer in memory).
 * - Initial range for valid code points was [0x0,0xFFFF] (16 bits).
 *   Current range is [0x0,0x10FFFF].
 * - [0x0,0xFFFF] range = Basic Multilingual Plane (BMP).
 * - Characters above 0xFFFF are called "supplementary characters".
 * - Unicode conforming software should display right-to-left characters
 *   such as Hebrew letters as right-to-left simply from the properties
 *   of those characters.
 *   Unicode provides seven characters
 *   (U+200E, U+200F, U+202A, U+202B, U+202C, U+202D, U+202E)
 *   to help control these embedded bidirectional text levels
 *   up to 61 levels deep.
 * - In Java String class, encoding is UTF-16, and code points outside BMP
 *   are represented by two chars (16-bits each): the first in [0xD800,0xDBFF]
 *   range (high surrogate area, 0x400 values), the second in [0xDC00,0xDFFF]
 *   range (low surrogate area, 0x400 values as well), which allows
 *   for 0x160000 values.
 *   NB: As a result, no code point can be defined in [0xD800,0xDFFF].
 *   Formula : "codePoint = (c1 - 0xD800) * (c2 - 0xDC00) + 0x10000".
 *   Ex.:
 *   0x010000 = {0xD800,0xDC00}
 *   0x0103FF = {0xD800,0xDFFF}
 *   0x010400 = {0xD801,0xDC00}
 *   0w10FC00 = {0xDBFF,0xDC00}
 *   0x10FFFF = {0xDBFF,0xDFFF}
 * 
 * Links:
 * - http://www.unicode.org/charts/PDF/U0000.pdf
 * - http://www.unicode.org/charts/PDF/U0080.pdf
 * - http://www.iana.org/assignments/character-sets/character-sets.xml
 * - https://en.wikibooks.org/wiki/Unicode/Character_reference
 * - https://en.wikipedia.org/wiki/Whitespace_character
 * - http://www.unicode.org/charts/PDF
 * - http://www.fileformat.info
 * - http://www.utf8everywhere.org
 * - https://www.cl.cam.ac.uk/~mgk25/ucs/quotes.html
 * - https://en.wikipedia.org/wiki/Unicode_control_characters
 * - https://unicode-table.com/en/search/?q=U%2B0020
 * - https://unicodelookup.com
 * - https://richardjharris.github.io/unicode-in-five-minutes.html
 * - https://begriffs.com/posts/2019-05-23-unicode-icu.html
 */
public class BwdUnicode {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * ASCII: CO controls.
     */
    
    /**
     * 0/0x00: null
     * C escape sequence: \0
     */
    public static final int NUL = 0x0;
    
    /**
     * 1/0x01: start of heading
     */
    public static final int SOH = 0x1;
    
    /**
     * 2/0x02: start of text
     */
    public static final int STX = 0x2;
    
    /**
     * 3/0x03: end of text
     */
    public static final int ETC = 0x3;
    
    /**
     * 4/0x04: end of transmission
     */
    public static final int EOT = 0x4;
    
    /**
     * 5/0x05: enquiry
     */
    public static final int ENQ = 0x5;
    
    /**
     * 6/0x06: acknowledge
     */
    public static final int ACK = 0x6;
    
    /**
     * 7/0x07: bell
     * C escape sequence: \a
     */
    public static final int BEL = 0x7;
    
    /**
     * 8/0x08: backspace
     * C escape sequence: \b
     */
    public static final int BACKSPACE = 0x8;
    
    /**
     * 9/0x09: horizontal tabulation / character tabulation
     * C escape sequence: \t
     */
    public static final int HT = 0x9;
    
    /**
     * 10/0x0A: line feed / new line / end of line
     * C escape sequence: \n
     */
    public static final int LF = 0xA;
    
    /**
     * 11/0x0B: vertical tabulation / line tabulation
     */
    public static final int VT = 0xB;
    
    /**
     * 12/0x0C: form feed / new page
     * C escape sequence: \f
     */
    public static final int FF = 0xC;
    
    /**
     * 13/0x0D: carriage return
     * C escape sequence: \r
     */
    public static final int CR = 0xD;
    
    /**
     * 14/0x0E: shift out
     */
    public static final int SO = 0xE;
    
    /**
     * 15/0x0F: shift in
     */
    public static final int SI = 0xF;
    
    /**
     * 16/0x10: data link escape
     */
    public static final int DLE = 0x10;
    
    /**
     * 17/0x11: device control one
     */
    public static final int DC1 = 0x11;
    
    /**
     * 18/0x12: device control two
     */
    public static final int DC2 = 0x12;
    
    /**
     * 19/0x13: device control three
     */
    public static final int DC3 = 0x13;
    
    /**
     * 20/0x14: device control four
     */
    public static final int DC4 = 0x14;
    
    /**
     * 21/0x15: negative acknowledge
     */
    public static final int NAK = 0x15;
    
    /**
     * 22/0x16: synchronous idle
     */
    public static final int SYN = 0x16;
    
    /**
     * 23/0x17: end of transmission block
     */
    public static final int ETB = 0x17;
    
    /**
     * 24/0x18: cancel
     */
    public static final int CAN = 0x18;
    
    /**
     * 25/0x19: end of medium
     */
    public static final int EM = 0x19;
    
    /**
     * 26/0x1A: substitute
     */
    public static final int SUB = 0x1A;
    
    /**
     * 27/0x1B: escape
     */
    public static final int ESC = 0x1B;
    
    /**
     * 28/0x1C: file separator / information separator four
     */
    public static final int FS = 0x1C;
    
    /**
     * 29/0x1D: group separator / information separator three
     */
    public static final int GS = 0x1D;
    
    /**
     * 30/0x1E: record separator / information separator two
     */
    public static final int RS = 0x1E;
    
    /**
     * 31/0x1F: unit separator / information separator one
     */
    public static final int US = 0x1F;
    
    /*
     * ASCII: punctuation and symbols.
     */
    
    /**
     * 32/0x20: space
     */
    public static final int SPACE = 0x20;
    
    /**
     * 33/0x21: exclamation mark: !
     */
    public static final int EXCLAMATION_MARK = 0x21;
    
    /**
     * 34/0x22: double quote / quotation mark / speech mark: "
     * HTML name: &quot;
     */
    public static final int DOUBLE_QUOTE = 0x22;
    
    /**
     * 35/0x23: number sign / hash / pound sign: #
     * 
     * Design: "NUMBER" is confusing, so we might want to name it "NUMBER_SIGN",
     * but that would be heterogeneous with other signs not ending with "_SIGN"
     * (to keep things simple), so we just use "HASH".
     */
    public static final int HASH = 0x23;
    
    /**
     * 36/0x24: dollar sign: $
     */
    public static final int DOLLAR = 0x24;
    
    /**
     * 37/0x25: percent sign: %
     */
    public static final int PERCENT = 0x25;
    
    /**
     * 38/0x26: ampersand: &
     * HTML name: &amp;
     */
    public static final int AMPERSAND = 0x26;
    
    /**
     * 39/0x27: single quote / apostrophe: '
     */
    public static final int SINGLE_QUOTE = 0x27;
    
    /**
     * 40/0x28: left parenthesis: (
     */
    public static final int LEFT_PARENTHESIS = 0x28;
    
    /**
     * 41/0x29: right parenthesis: )
     */
    public static final int RIGHT_PARENTHESIS = 0x29;
    
    /**
     * 42/0x2A: asterisk: *
     */
    public static final int ASTERISK = 0x2A;
    
    /**
     * 43/0x2B: plus sign: +
     */
    public static final int PLUS = 0x2B;
    
    /**
     * 44/0x2C: comma: ,
     */
    public static final int COMMA = 0x2C;
    
    /**
     * 45/0x2D: hyphen / minus: -
     */
    public static final int MINUS = 0x2D;
    
    /**
     * 46/0x2E: dot / period / full stop: .
     */
    public static final int DOT = 0x2E;
    
    /**
     * 47/0x2F: slash / divide / solidus: /
     */
    public static final int SLASH = 0x2F;
    
    /*
     * ASCII: digits.
     * 
     * 48/0x30-57/0x39: digits: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9
     */
    
    public static final int ZERO = 0x30;
    public static final int ONE = 0x31;
    public static final int TWO = 0x32;
    public static final int THREE = 0x33;
    public static final int FOUR = 0x34;
    public static final int FIVE = 0x35;
    public static final int SIX = 0x36;
    public static final int SEVEN = 0x37;
    public static final int HEIGHT = 0x38;
    public static final int NINE = 0x39;
    
    /*
     * ASCII: punctuation and symbols.
     */
    
    /**
     * 58/0x3A: colon: :
     */
    public static final int COLON = 0x3A;
    
    /**
     * 59/0x3B: semicolon: ;
     */
    public static final int SEMICOLON = 0x3B;
    
    /**
     * 60/0x3C: less-than sign: <
     * HTML name: &lt;
     */
    public static final int LESS_THAN = 0x3C;
    
    /**
     * 61/0x3D: equals sign: =
     */
    public static final int EQUALS = 0x3D;
    
    /**
     * 62/0x3E: greater-than sign: >
     * HTML name: &gt;
     */
    public static final int GREATER_THAN = 0x3E;
    
    /**
     * 63/0x3F: question mark: ?
     */
    public static final int QUESTION_MARK = 0x3F;
    
    /**
     * 64/0x40: at symbol / commercial at: @
     */
    public static final int AT_SYMBOL = 0x40;
    
    /*
     * ASCII: uppercase latin alphabet.
     * 
     * 65/0x41-90/0x5A: latin capital letters / uppercase letters
     */
    
    public static final int A = 0x41;
    public static final int B = 0x42;
    public static final int C = 0x43;
    public static final int D = 0x44;
    public static final int E = 0x45;
    public static final int F = 0x46;
    public static final int G = 0x47;
    public static final int H = 0x48;
    public static final int I = 0x49;
    public static final int J = 0x4A;
    public static final int K = 0x4B;
    public static final int L = 0x4C;
    public static final int M = 0x4D;
    public static final int N = 0x4E;
    public static final int O = 0x4F;
    public static final int P = 0x50;
    public static final int Q = 0x51;
    public static final int R = 0x52;
    public static final int S = 0x53;
    public static final int T = 0x54;
    public static final int U = 0x55;
    public static final int V = 0x56;
    public static final int W = 0x57;
    public static final int X = 0x58;
    public static final int Y = 0x59;
    public static final int Z = 0x5A;
    
    /*
     * ASCII: punctuation and symbols.
     */
    
    /**
     * 91/0x5B: left (or opening) (square) bracket: [
     */
    public static final int LEFT_BRACKET = 0x5B;
    
    /**
     * 92/0x5C: backslash / reverse solidus: \
     */
    public static final int BACKSLASH = 0x5C;
    
    /**
     * 93/0x5D: right (or closing) (square) bracket: ]
     */
    public static final int RIGHT_BRACKET = 0x5D;
    
    /**
     * 94/0x5E: circumflex accent: ^
     */
    public static final int CIRCUMFLEX_ACCENT = 0x5E;
    
    /**
     * 95/0x5F: underscore / low line: _
     */
    public static final int UNDERSCORE = 0x5F;
    
    /**
     * 96/0x60: grave accent: `
     */
    public static final int GRAVE_ACCENT = 0x60;
    
    /*
     * ASCII: lowercase latin alphabet.
     * 
     * 97/0x61-122/0x7A: latin small letters / lowercase letters
     */
    
    public static final int a = 0x61;
    public static final int b = 0x62;
    public static final int c = 0x63;
    public static final int d = 0x64;
    public static final int e = 0x65;
    public static final int f = 0x66;
    public static final int g = 0x67;
    public static final int h = 0x68;
    public static final int i = 0x69;
    public static final int j = 0x6A;
    public static final int k = 0x6B;
    public static final int l = 0x6C;
    public static final int m = 0x6D;
    public static final int n = 0x6E;
    public static final int o = 0x6F;
    public static final int p = 0x70;
    public static final int q = 0x71;
    public static final int r = 0x72;
    public static final int s = 0x73;
    public static final int t = 0x74;
    public static final int u = 0x75;
    public static final int v = 0x76;
    public static final int w = 0x77;
    public static final int x = 0x78;
    public static final int y = 0x79;
    public static final int z = 0x7A;
    
    /*
     * ASCII: punctuation and symbols.
     */
    
    /**
     * 123/0x7B: left (or opening) brace (or curly bracket): {
     */
    public static final int LEFT_BRACE = 0x7B;
    
    /**
     * 124/0x7C: vertical bar / vertical line: |
     */
    public static final int VERTICAL_BAR = 0x7C;
    
    /**
     * 125/0x7D: right (or closing) brace (or curly bracket): }
     */
    public static final int RIGHT_BRACE = 0x7D;
    
    /**
     * 126/0x7E: tilde / equivalency sign: ~
     */
    public static final int TILDE = 0x7E;
    
    /*
     * ASCII: control characters.
     */
    
    /**
     * 127/0x7F: delete
     */
    public static final int DELETE = 0x7F;
    
    /*
     * Latin-1: C1 controls.
     */
    
    /**
     * 128/0x80
     */
    public static final int XXX_80 = 0x80;
    
    /**
     * 129/0x81
     */
    public static final int XXX_81 = 0x81;
    
    /**
     * 130/0x82: break permitted here
     * cf. 200B (zero width space)
     */
    public static final int BPH = 0x82;
    
    /**
     * 131/0x83: no break here
     * cf. 2060 (work joiner)
     */
    public static final int NBH = 0x83;
    
    /**
     * 132/0x84: formerly known as INDEX
     */
    public static final int IND = 0x84;
    
    /**
     * 133/0x85: next line
     */
    public static final int NEL = 0x85;
    
    /**
     * 134/0x86: start of selected area
     */
    public static final int SSA = 0x86;
    
    /**
     * 135/0x87: end of selected area
     */
    public static final int ESA = 0x87;
    
    /**
     * 136/0x88: character tabulation set
     */
    public static final int HTS = 0x88;
    
    /**
     * 137/0x89: character tabulation with justification
     */
    public static final int HTJ = 0x89;
    
    /**
     * 138/0x8A: line tabulation set
     */
    public static final int VTS = 0x8A;
    
    /**
     * 139/0x8B: partial line forward
     */
    public static final int PLD = 0x8B;
    
    /**
     * 140/0x8C: partial line backward
     */
    public static final int PLU = 0x8C;
    
    /**
     * 141/0x8D: reverse line feed
     */
    public static final int RI = 0x8D;
    
    /**
     * 142/0x8E: single shift two
     */
    public static final int SS2 = 0x8E;
    
    /**
     * 143/0x8F: single shift three
     */
    public static final int SS3 = 0x8F;
    
    /**
     * 144/0x90: device control string
     */
    public static final int DCS = 0x90;
    
    /**
     * 145/0x91: private use one
     */
    public static final int PU1 = 0x91;
    
    /**
     * 146/0x92: private use two
     */
    public static final int PU2 = 0x92;
    
    /**
     * 147/0x93: set transmit state
     */
    public static final int STS = 0x93;
    
    /**
     * 148/0x94: cancel character
     */
    public static final int CCH = 0x94;
    
    /**
     * 149/0x95: message waiting
     */
    public static final int MW = 0x95;
    
    /**
     * 150/0x96: start of guarded area
     */
    public static final int SPA = 0x96;
    
    /**
     * 151/0x97: end of guarded area
     */
    public static final int EPA = 0x97;
    
    /**
     * 152/0x98: start of string
     */
    public static final int SOS = 0x98;
    
    /**
     * 153/0x99
     */
    public static final int XXX_99 = 0x99;
    
    /**
     * 154/0x9A: single character introducer
     */
    public static final int SCI = 0x9A;
    
    /**
     * 155/0x9B: control sequence introducer
     */
    public static final int CSI = 0x9B;
    
    /**
     * 156/0x9C: string terminator
     */
    public static final int ST = 0x9C;
    
    /**
     * 157/0x9D: operating system command
     */
    public static final int OSC = 0x9D;
    
    /**
     * 158/0x9E: privacy message
     */
    public static final int PM = 0x9E;
    
    /**
     * 159/0x9F: application program command
     */
    public static final int APC = 0x9F;
    
    /*
     * Latin-1: punctuation and symbols
     * (based on ISO/IEC 8859-1 (aka Latin-1) from here)
     */
    
    /**
     * 160/0xA0: no-break space
     */
    public static final int NBSP = 0xA0;
    
    /**
     * 161/0xA1
     */
    public static final int INVERTED_EXCLAMATION_MARK = 0xA1;
    
    /**
     * 162/0xA2
     */
    public static final int CENT_SIGN = 0xA2;
    
    /**
     * 163/0xA3
     */
    public static final int POUND_SIGN = 0xA3;
    
    /**
     * 164/0xA4
     */
    public static final int CURRENCY_SIGN = 0xA4;
    
    /**
     * 165/0xA5
     */
    public static final int YEN_SIGN = 0xA5;
    
    /**
     * 166/0xA6
     */
    public static final int BROKEN_BAR = 0xA6;
    
    /**
     * 167/0xA7
     */
    public static final int SECTION_SIGN = 0xA7;
    
    /**
     * 168/0xA8
     */
    public static final int DIAERESIS = 0xA8;
    
    /**
     * 169/0xA9
     */
    public static final int COPYRIGHT_SIGN = 0xA9;
    
    /**
     * 170/0xAA
     */
    public static final int FEMININE_ORDINAL_INDICATOR = 0xAA;
    
    /**
     * 171/0xAB
     */
    public static final int LEFT_POINTING_DOUBLE_ANGLE_QUOTATION_MARK = 0xAB;
    
    /**
     * 172/0xAC
     */
    public static final int NOT_SIGN = 0xAC;
    
    /**
     * 173/0xAD: soft hyphen/shy
     */
    public static final int SOFT_HYPHEN = 0xAD;
    
    /**
     * 174/0xAE
     */
    public static final int REGISTERED_SIGN = 0xAE;
    
    /**
     * 175/0xAF
     */
    public static final int MACRON = 0xAF;
    
    /**
     * 176/0xB0
     */
    public static final int DEGREE_SIGN = 0xB0;
    
    /**
     * 177/0xB1
     */
    public static final int PLUS_MINUS_SIGN = 0xB1;
    
    /**
     * 178/0xB2
     */
    public static final int SUPERSCRIPT_TWO = 0xB2;
    
    /**
     * 179/0xB3
     */
    public static final int SUPERSCRIPT_THREE = 0xB3;
    
    /**
     * 180/0xB4
     */
    public static final int ACUTE_ACCENT = 0xB4;
    
    /**
     * 181/0xB5
     */
    public static final int MICRO_SIGN = 0xB5;
    
    /**
     * 182/0xB6
     */
    public static final int PILCROW_SIGN = 0xB6;
    
    /**
     * 183/0xB7
     */
    public static final int MIDDLE_DOT = 0xB7;
    
    /**
     * 184/0xB8
     */
    public static final int CEDILLA = 0xB8;
    
    /**
     * 185/0xB9
     */
    public static final int SUPERSCRIPT_ONE = 0xB9;
    
    /**
     * 186/0xBA
     */
    public static final int MASCULINE_ORDINAL_INDICATOR = 0xBA;
    
    /**
     * 187/0xBB
     */
    public static final int RIGHT_POINTING_DOUBLE_ANGLE_QUOTATION_MARK = 0xBB;
    
    /**
     * 188/0xBC
     */
    public static final int VULGAR_FRACTION_ONE_QUARTER = 0xBC;
    
    /**
     * 189/0xBD
     */
    public static final int VULGAR_FRACTION_ONE_HALF = 0xBD;
    
    /**
     * 190/0xBE
     */
    public static final int VULGAR_FRACTION_THREE_QUARTERS = 0xBE;
    
    /**
     * 191/0xBF
     */
    public static final int INVERTED_QUESTION_MARK = 0xBF;
    
    /*
     * Latin-1: letters.
     */
    
    /**
     * 192/0xC0
     */
    public static final int LATIN_CAPITAL_LETTER_A_WITH_GRAVE = 0xC0;
    
    /**
     * 193/0xC1
     */
    public static final int LATIN_CAPITAL_LETTER_A_WITH_ACUTE = 0xC1;
    
    /**
     * 194/0xC2
     */
    public static final int LATIN_CAPITAL_LETTER_A_WITH_CIRCUMFLEX = 0xC2;
    
    /**
     * 195/0xC3
     */
    public static final int LATIN_CAPITAL_LETTER_A_WITH_TIDLE = 0xC3;
    
    /**
     * 196/0xC4
     */
    public static final int LATIN_CAPITAL_LETTER_A_WITH_DIAERESIS = 0xC4;
    
    /**
     * 197/0xC5
     */
    public static final int LATIN_CAPITAL_LETTER_A_WITH_RING_ABOVE = 0xC5;
    
    /**
     * 198/0xC6
     */
    public static final int LATIN_CAPITAL_LETTER_AE = 0xC6;
    
    /**
     * 199/0xC7
     */
    public static final int LATIN_CAPITAL_LETTER_C_WITH_CEDILLA = 0xC7;
    
    /**
     * 200/0xC8
     */
    public static final int LATIN_CAPITAL_LETTER_E_WITH_GRAVE = 0xC8;
    
    /**
     * 201/0xC9
     */
    public static final int LATIN_CAPITAL_LETTER_E_WITH_ACUTE = 0xC9;
    
    /**
     * 202/0xCA
     */
    public static final int LATIN_CAPITAL_LETTER_E_WITH_CIRCUMFLEX = 0xCA;
    
    /**
     * 203/0xCB
     */
    public static final int LATIN_CAPITAL_LETTER_E_WITH_DIAERESIS = 0xCB;
    
    /**
     * 204/0xCC
     */
    public static final int LATIN_CAPITAL_LETTER_I_WITH_GRAVE = 0xCC;
    
    /**
     * 205/0xCD
     */
    public static final int LATIN_CAPITAL_LETTER_I_WITH_ACUTE = 0xCD;
    
    /**
     * 206/0xCE
     */
    public static final int LATIN_CAPITAL_LETTER_I_WITH_CIRCUMFLEX = 0xCE;
    
    /**
     * 207/0xCF
     */
    public static final int LATIN_CAPITAL_LETTER_I_WITH_DIAERESIS = 0xCF;
    
    /**
     * 208/0xD0
     */
    public static final int LATIN_CAPITAL_LETTER_ETH = 0xD0;
    
    /**
     * 209/0xD1
     */
    public static final int LATIN_CAPITAL_LETTER_N_WITH_TILDE = 0xD1;
    
    /**
     * 210/0xD2
     */
    public static final int LATIN_CAPITAL_LETTER_O_WITH_GRAVE = 0xD2;
    
    /**
     * 211/0xD3
     */
    public static final int LATIN_CAPITAL_LETTER_O_WITH_ACUTE = 0xD3;
    
    /**
     * 212/0xD4
     */
    public static final int LATIN_CAPITAL_LETTER_O_WITH_CIRCUMFLEX = 0xD4;
    
    /**
     * 213/0xD5
     */
    public static final int LATIN_CAPITAL_LETTER_O_WITH_TILDE = 0xD5;
    
    /**
     * 214/0xD6
     */
    public static final int LATIN_CAPITAL_LETTER_O_WITH_DIAERESIS = 0xD6;
    
    /*
     * Latin-1: mathematical operator.
     */
    
    /**
     * 215/0xD7
     */
    public static final int MULTIPLICATION_SIGN = 0xD7;
    
    /*
     * Latin-1: letters.
     */
    
    /**
     * 216/0xD8
     */
    public static final int LATIN_CAPITAL_LETTER_O_WITH_STROKE = 0xD8;
    
    /**
     * 217/0xD9
     */
    public static final int LATIN_CAPITAL_LETTER_U_WITH_GRAVE = 0xD9;
    
    /**
     * 218/0xDA
     */
    public static final int LATIN_CAPITAL_LETTER_U_WITH_ACUTE = 0xDA;
    
    /**
     * 219/0xDB
     */
    public static final int LATIN_CAPITAL_LETTER_U_WITH_CIRCUMFLEX = 0xDB;
    
    /**
     * 220/0xDC
     */
    public static final int LATIN_CAPITAL_LETTER_U_WITH_DIAERESIS = 0xDC;
    
    /**
     * 221/0xDD
     */
    public static final int LATIN_CAPITAL_LETTER_Y_WITH_ACUTE = 0xDD;
    
    /**
     * 222/0xDE
     */
    public static final int LATIN_CAPITAL_LETTER_THORN = 0xDE;
    
    /**
     * 223/0xDF
     */
    public static final int LATIN_SMALL_LETTER_SHARP_S = 0xDF;
    
    /**
     * 224/0xE0
     */
    public static final int LATIN_SMALL_LETTER_A_WITH_GRAVE = 0xE0;
    
    /**
     * 225/0xE1
     */
    public static final int LATIN_SMALL_LETTER_A_WITH_ACUTE = 0xE1;
    
    /**
     * 226/0xE2
     */
    public static final int LATIN_SMALL_LETTER_A_WITH_CIRCUMFLEX = 0xE2;
    
    /**
     * 227/0xE3
     */
    public static final int LATIN_SMALL_LETTER_A_WITH_TILDE = 0xE3;
    
    /**
     * 228/0xE4
     */
    public static final int LATIN_SMALL_LETTER_A_WITH_DIAERESIS = 0xE4;
    
    /**
     * 229/0xE5
     */
    public static final int LATIN_SMALL_LETTER_A_WITH_RING_ABOVE = 0xE5;
    
    /**
     * 230/0xE6
     */
    public static final int LATIN_SMALL_LETTER_AE = 0xE6;
    
    /**
     * 231/0xE7
     */
    public static final int LATIN_SMALL_LETTER_C_WITH_CEDILLA = 0xE7;
    
    /**
     * 232/0xE8
     */
    public static final int LATIN_SMALL_LETTER_E_WITH_GRAVE = 0xE8;
    
    /**
     * 233/0xE9
     */
    public static final int LATIN_SMALL_LETTER_E_WITH_ACUTE = 0xE9;
    
    /**
     * 234/0xEA
     */
    public static final int LATIN_SMALL_LETTER_E_WITH_CIRCUMFLEX = 0xEA;
    
    /**
     * 235/0xEB
     */
    public static final int LATIN_SMALL_LETTER_E_WITH_DIAERESIS = 0xEB;
    
    /**
     * 236/0xEC
     */
    public static final int LATIN_SMALL_LETTER_I_WITH_GRAVE = 0xEC;
    
    /**
     * 237/0xED
     */
    public static final int LATIN_SMALL_LETTER_I_WITH_ACUTE = 0xED;
    
    /**
     * 238/0xEE
     */
    public static final int LATIN_SMALL_LETTER_I_WITH_CIRCUMFLEX = 0xEE;
    
    /**
     * 239/0xEF
     */
    public static final int LATIN_SMALL_LETTER_I_WITH_DIAERESIS = 0xEF;
    
    /**
     * 240/0xF0
     */
    public static final int LATIN_SMALL_LETTER_ETH = 0xF0;
    
    /**
     * 241/0xF1
     */
    public static final int LATIN_SMALL_LETTER_N_WITH_TILDE = 0xF1;
    
    /**
     * 242/0xF2
     */
    public static final int LATIN_SMALL_LETTER_O_WITH_GRAVE = 0xF2;
    
    /**
     * 243/0xF3
     */
    public static final int LATIN_SMALL_LETTER_O_WITH_ACUTE = 0xF3;
    
    /**
     * 244/0xF4
     */
    public static final int LATIN_SMALL_LETTER_O_WITH_CIRCUMFLEX = 0xF4;
    
    /**
     * 245/0xF5
     */
    public static final int LATIN_SMALL_LETTER_O_WITH_TILDE = 0xF5;
    
    /**
     * 246/0xF6
     */
    public static final int LATIN_SMALL_LETTER_O_WITH_DIAERESIS = 0xF6;
    
    /*
     * Latin-1: mathematical operator.
     */
    
    /**
     * 247/0xF7
     */
    public static final int DIVISION_SIGN = 0xF7;
    
    /*
     * Latin-1: letters.
     */
    
    /**
     * 248/0xF8
     */
    public static final int LATIN_SMALL_LETTER_O_WITH_STROKE = 0xF8;
    
    /**
     * 249/0xF9
     */
    public static final int LATIN_SMALL_LETTER_U_WITH_GRAVE = 0xF9;
    
    /**
     * 250/0xFA
     */
    public static final int LATIN_SMALL_LETTER_U_WITH_ACUTE = 0xFA;
    
    /**
     * 251/0xFB
     */
    public static final int LATIN_SMALL_LETTER_U_WITH_CIRCUMFLEX = 0xFB;
    
    /**
     * 252/0xFC
     */
    public static final int LATIN_SMALL_LETTER_U_WITH_DIAERESIS = 0xFC;
    
    /**
     * 253/0xFD
     */
    public static final int LATIN_SMALL_LETTER_Y_WITH_ACUTE = 0xFD;
    
    /**
     * 254/0xFE
     */
    public static final int LATIN_SMALL_LETTER_THORN = 0xFE;
    
    /**
     * 255/0xFF
     */
    public static final int LATIN_SMALL_LETTER_Y_WITH_DIAERESIS = 0xFF;

    /*
     * Others.
     */
    
    /**
     * 8192/0x2000
     */
    public static final int EN_QUAD = 0x2000;
    
    /**
     * 8193/0x2001
     */
    public static final int EM_QUAD = 0x2001;
    
    /**
     * 8194/0x2002
     */
    public static final int EN_SPACE = 0x2002;
    
    /**
     * 8195/0x2003
     */
    public static final int EM_SPACE = 0x2003;
    
    /**
     * 8196/0x2004
     */
    public static final int THREE_PER_EM_SPACE = 0x2004;
    
    /**
     * 8197/0x2005
     */
    public static final int FOUR_PER_EM_SPACE = 0x2005;
    
    /**
     * 8198/0x2006
     */
    public static final int SIX_PER_EM_SPACE = 0x2006;
    
    /**
     * 8199/0x2007
     */
    public static final int FIGURE_SPACE = 0x2007;
    
    /**
     * 8200/0x2008
     */
    public static final int PUNCTUATION_SPACE = 0x2008;
    
    /**
     * 8201/0x2009
     */
    public static final int THIN_SPACE = 0x2009;
    
    /**
     * 8202/0x200A
     */
    public static final int HAIR_SPACE = 0x200A;
    
    /**
     * 8203/0x200B: zero width space
     */
    public static final int ZWSP = 0x200B;
    
    /**
     * 8204/0x200C: zero width non-joiner
     */
    public static final int ZWNJ = 0x200C;
    
    /**
     * 8205/0x200D: zero width joiner
     */
    public static final int ZWJ = 0x200D;
    
    /*
     * 
     */

    /**
     * 8206/0x200E: left-to-right mark
     */
    public static final int LRM = 0x200E;
    
    /**
     * 8207/0x200F: right-to-left mark
     */
    public static final int RLM = 0x200F;

    /*
     * 
     */
    
    /**
     * 8232/0x2028: line separator
     */
    public static final int LSEP = 0x2028;

    /**
     * 8233/0x2029: paragraph separator
     */
    public static final int PSEP = 0x2029;
    
    /*
     * 
     */

    /**
     * 8234/0x202A: left-to-right embedding
     */
    public static final int LRE = 0x202A;
    
    /**
     * 8235/0x202B: right-to-left embedding
     */
    public static final int RLE = 0x202B;
    
    /**
     * 8236/0x202C: pop directional formatting
     */
    public static final int PDF = 0x202C;
    
    /**
     * 8237/0x202D: left-to-right override
     */
    public static final int LRO = 0x202D;
    
    /**
     * 8238/0x202E: right-to-left override
     */
    public static final int RLO = 0x202E;

    /**
     * 8239/0x202F: narrow no-break space
     */
    public static final int NNBSP = 0x202F;
    
    /*
     * 
     */

    /**
     * 8287/0x205F: medium mathematical space
     */
    public static final int MMSP = 0x205F;
    
    /**
     * 8288/0x2060: word joiner.
     */
    public static final int WJ = 0x2060;
    
    /*
     * 
     */
    
    /**
     * 8289/0x2061
     */
    public static final int FUNCTION_APPLICATION = 0x2061;
    
    /**
     * 8290/0x2062
     */
    public static final int INVISIBLE_TIMES = 0x2062;
    
    /**
     * 8291/0x2063
     */
    public static final int INVISIBLE_SEPARATOR = 0x2063;
    
    /**
     * 8292/0x2064
     */
    public static final int INVISIBLE_PLUS = 0x2064;
    
    /*
     * 
     */
    
    /**
     * 8294/0x2066
     */
    public static final int LEFT_TO_RIGHT_ISOLATE = 0x2066;
    
    /**
     * 8295/0x2067
     */
    public static final int RIGHT_TO_LEFT_ISOLATE = 0x2067;
    
    /**
     * 8296/0x2068
     */
    public static final int FIRST_STRONG_ISOLATE = 0x2068;
    
    /**
     * 8297/0x2069
     */
    public static final int POP_DIRECTIONAL = 0x2069;

    /*
     * 
     */
    
    /**
     * 65279/0xFEFF: zero width no-break space
     */
    public static final int ZWNBSP = 0xFEFF;

    /**
     * 65440/0xFFA0: halfwidth hangul filler
     */
    public static final int HWHF = 0xFFA0;
    
    /*
     * 
     */

    /**
     * 65529/0xFFF9: interlinear annotation anchor
     */
    public static final int IAA = 0xFFF9;
    
    /**
     * 65530/0xFFFA: interlinear annotation separator
     */
    public static final int IAS = 0xFFFA;
    
    /**
     * 65531/0xFFFB: interlinear annotation terminator
     */
    public static final int IAT = 0xFFFB;
    
    /*
     * 
     */

    /**
     * 65532/0xFFFC: object replacement character
     * 
     * Used as a placeholder in text for an otherwise unspecified object.
     */
    public static final int ORC = 0xFFFC;
    
    /**
     * 65533/0xFFFD: replacement character
     * 
     * Used to replace an incoming character whose value
     * is unknown or unrepresentable in Unicode.
     */
    public static final int RC = 0xFFFD;
    
    /**
     * 65534/0xFFFE: not a character
     */
    public static final int NAC_FFFE = 0xFFFE;
    
    /**
     * 65535/0xFFFF: last value in Basic Multilingual Plane
     */
    public static final int MAX_FFFF = 0xFFFF;

    /**
     * 12288/0x3000: ideographic space
     */
    public static final int IDSP = 0x3000;

    /**
     * 0x10FFFF: max code point value (so far)
     */
    public static final int MAX_10FFFF = 0x10FFFF;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Useful to print code points, or values that should be code points.
     * 
     * @param codePoint A code point. Can be any int value.
     * @return The specified value, with sign bit used as mantissa bit
     *         (i.e. unsigned), as an upper case hexadecimal string,
     *         prefixed with "0x" to make the radix explicit
     *         (not using Unicode "U+" prefix, which is less known
     *         and could be confused with an addition).
     */
    public static String toDisplayString(int codePoint) {
        if (codePoint < 0) {
            return "0x" + NbrsUtils.toString(toUnsignedLong(codePoint), 16);
        } else {
            return "0x" + NbrsUtils.toString(codePoint, 16);
        }
    }

    /**
     * @param codePoint The code point which validity must be checked.
     * @throws IllegalArgumentException if the specified code point is not valid,
     *         i.e. if Character.isValidCodePoint(codePoint) is false.
     */
    public static void checkCodePoint(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throwIAEInvalidCodePoint(codePoint);
        }
    }

    /**
     * @param codePoint A code point. Can be any int value.
     * @return True if the specified code point is in the Basic Multilingual Plane,
     *         which corresponds to the [0,0xFFFF] range, false otherwise.
     */
    public static boolean isInBmp(int codePoint) {
        return (codePoint >= 0) && (codePoint <= MAX_FFFF);
    }
    
    /**
     * @param codePoint A code point. Can be any int value.
     * @return True if the specified code point is an horizontal space,
     *         and an actual one (not a "zero width space"),
     *         false otherwise.
     */
    public static boolean isHorizontalSpace(int codePoint) {
        switch (codePoint) {
        case HT:
        case SPACE:
        case NBSP:
        case NNBSP:
        case MMSP:
        case HWHF:
        case IDSP:
            return true;
        default:
            break;
        }
        
        if ((codePoint >= EN_QUAD) && (codePoint <= HAIR_SPACE)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Note that a new line can also be CR+LF,
     * or LF+CR (Acorn BBC and RISC OS).
     * 
     * @param codePoint A code point. Can be any int value.
     * @return True if the specified code point is a new line
     *         or a vertical space, false otherwise.
     */
    public static boolean isNewlineOrVerticalSpace(int codePoint) {
        switch (codePoint) {
        case LF:
        case VT:
        case FF:
        case CR:
        case NEL:
        case LSEP:
        case PSEP:
            return true;
        default:
            break;
        }
        
        return false;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BwdUnicode() {
    }
    
    /*
     * 
     */
    
    private static void throwIAEInvalidCodePoint(int codePoint) {
        throw new IllegalArgumentException("codePoint [" + toDisplayString(codePoint) + "] is not valid");
    }
    
    private static long toUnsignedLong(int x) {
        return ((long) x) & 0xFFFFFFFFL;
    }
}
