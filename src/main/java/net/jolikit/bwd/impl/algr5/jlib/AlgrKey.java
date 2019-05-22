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
package net.jolikit.bwd.impl.algr5.jlib;

import net.jolikit.bwd.impl.utils.basics.IntValuedHelper;
import net.jolikit.bwd.impl.utils.basics.IntValuedHelper.InterfaceIntValued;

/**
 * enum ALLEGRO_KEY
 */
public enum AlgrKey implements InterfaceIntValued {
    A(1),
    B(2),
    C(3),
    D(4),
    E(5),
    F(6),
    G(7),
    H(8),
    I(9),
    J(10),
    K(11),
    L(12),
    M(13),
    N(14),
    O(15),
    P(16),
    Q(17),
    R(18),
    S(19),
    T(20),
    U(21),
    V(22),
    W(23),
    X(24),
    Y(25),
    Z(26),
    /*
     * 
     */
    DIGIT_0(27),
    DIGIT_1(28),
    DIGIT_2(29),
    DIGIT_3(30),
    DIGIT_4(31),
    DIGIT_5(32),
    DIGIT_6(33),
    DIGIT_7(34),
    DIGIT_8(35),
    DIGIT_9(36),
    /*
     * 
     */
    PAD_0(37),
    PAD_1(38),
    PAD_2(39),
    PAD_3(40),
    PAD_4(41),
    PAD_5(42),
    PAD_6(43),
    PAD_7(44),
    PAD_8(45),
    PAD_9(46),
    /*
     * 
     */
    F1(47),
    F2(48),
    F3(49),
    F4(50),
    F5(51),
    F6(52),
    F7(53),
    F8(54),
    F9(55),
    F10(56),
    F11(57),
    F12(58),
    /*
     * 
     */
    ESCAPE(59),
    TILDE(60),
    MINUS(61),
    EQUALS(62),
    BACKSPACE(63),
    TAB(64),
    OPENBRACE(65),
    CLOSEBRACE(66),
    ENTER(67),
    SEMICOLON(68),
    QUOTE(69),
    BACKSLASH(70),
    /**
     * DirectInput calls this DIK_OEM_102: "< > | on UK/Germany keyboards"
     */
    BACKSLASH2(71),
    COMMA(72),
    FULLSTOP(73),
    SLASH(74),
    SPACE(75),
    /*
     * 
     */
    INSERT(76),
    DELETE(77),
    HOME(78),
    END(79),
    PGUP(80),
    PGDN(81),
    LEFT(82),
    RIGHT(83),
    UP(84),
    DOWN(85),
    /*
     * 
     */
    PAD_SLASH(86),
    PAD_ASTERISK(87),
    PAD_MINUS(88),
    PAD_PLUS(89),
    PAD_DELETE(90),
    PAD_ENTER(91),
    /*
     * 
     */
    PRINTSCREEN(92),
    PAUSE(93),
    /*
     * 
     */
    ABNT_C1(94),
    YEN(95),
    KANA(96),
    CONVERT(97),
    NOCONVERT(98),
    AT(99),
    CIRCUMFLEX(100),
    COLON2(101),
    KANJI(102),
    /*
     * 
     */
    PAD_EQUALS(103),  // MacOS X
    BACKQUOTE(104),  // MacOS X
    SEMICOLON2(105),  // MacOS X -- TODO: ask lillo what this should be
    COMMAND(106),  // MacOS X
    /*
     * 
     */
    BACK(107),        // Android back key
    VOLUME_UP(108),
    VOLUME_DOWN(109),
    /*
     * Android game keys
     */
    SEARCH(110),
    DPAD_CENTER(111),
    BUTTON_X(112),
    BUTTON_Y(113),
    DPAD_UP(114),
    DPAD_DOWN(115),
    DPAD_LEFT(116),
    DPAD_RIGHT(117),
    SELECT(118),
    START(119),
    BUTTON_L1(120),
    BUTTON_R1(121),
    BUTTON_L2(122),
    BUTTON_R2(123),
    BUTTON_A(124),
    BUTTON_B(125),
    THUMBL(126),
    THUMBR(127),
    /*
     * 
     */
    UNKNOWN(128),
    /* 
     * Modifiers.
     * All codes up to before modifiers can be freely
     * assigned as additional unknown keys; like various multimedia
     * and application keys keyboards may have.
     */
    LSHIFT(215),
    RSHIFT(216),
    LCTRL(217),
    RCTRL(218),
    ALT(219),
    ALTGR(220),
    LWIN(221),
    RWIN(222),
    MENU(223),
    SCROLLLOCK(224),
    NUMLOCK(225),
    CAPSLOCK(226);
    
    /**
     * Exclusive.
     */
    public static final int ALLEGRO_KEY_MAX = LSHIFT.intValue();
    
    private static final IntValuedHelper<AlgrKey> HELPER =
            new IntValuedHelper<AlgrKey>(AlgrKey.values());
    
    private final int intValue;
    
    private AlgrKey(int intValue) {
        this.intValue = intValue;
    }
    
    @Override
    public int intValue() {
        return this.intValue;
    }
    
    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static AlgrKey valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
