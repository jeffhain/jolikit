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

import java.util.SortedSet;

import net.jolikit.lang.LangUtils;

/**
 * Class for KEY_TYPED events.
 * 
 * We use this specific class for these events, instead of a common class
 * also used for key pressed/released events, to avoid the boilerplate, the
 * conventions and the probable confusion about whether key code or Unicode
 * character are relevant to look at depending on the event type.
 * 
 * Must never contain NUL character (0), i.e. must not be generated in case
 * the backing library generated a key typed event with NUL character,
 * which some libraries do in cases no Unicode character was typed,
 * while in that case other libraries just don't generate an event.
 */
public class BwdKeyEventT extends AbstractBwdModAwareEvent {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Using a string, not just an int code point, so that users needing
     * a string can find it here, and not create it each time.
     * Int code point is easily accessible with String.codePointAt(0).
     */
    private final String unicodeCharacter;
    
    /**
     * Whether this event is a repetition of a previous event
     * with same code point.
     */
    private final boolean isRepeat;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param codePoint Must be a valid code point, i.e. positive
     *        and <= 0x10FFFF. Must not be the NUL character (0).
     * @param isRepeat Whether this event is a repetition of a previous event
     *        with same code point.
     */
    public BwdKeyEventT(
            Object source,
            //
            int codePoint,
            boolean isRepeat,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        this(
                null,
                //
                source,
                //
                checkedUnicodeCharacter(codePoint),
                isRepeat,
                //
                newImmutableSortedSet(modifierKeyDownSet));
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("[").append(this.getEventType());
        
        /*
         * 
         */
        
        sb.append(", Unicode character = ").append(this.getUnicodeCharacter());
        
        sb.append(" (code point = ").append(this.getCodePoint()).append(")");
        
        sb.append(", is repeat = ").append(this.isRepeat());
        
        /*
         * 
         */
        
        this.appendModifiers(sb);
        
        sb.append("]");
        
        return sb.toString();
    }
    
    /*
     * 
     */
    
    /**
     * Note: the fact that we preserve modifiers should not hurt,
     * since on key press or release the repetition should stop.
     * 
     * @return An event, identical to this one modulo that its isRepeat
     *         boolean is the specified one.
     */
    public BwdKeyEventT withIsRepeat(boolean isRepeat) {
        if (isRepeat == this.isRepeat) {
            return this;
        }
        return new BwdKeyEventT(
                null,
                //
                this.getSource(),
                //
                this.unicodeCharacter,
                isRepeat,
                //
                this.getModifierKeyDownSet());
    }

    /*
     * 
     */
    
    /**
     * The code point can be obtained by using String.codePointAt(0)
     * on the returned string, or by using getCodePoint().
     * 
     * @return A String corresponding to the Unicode character typed.
     *         Is never NUL character, since this event cannot be created
     *         for NUL character.
     */
    public String getUnicodeCharacter() {
        return this.unicodeCharacter;
    }
    
    /**
     * Convenience method.
     * 
     * @return The code point.
     *         Is never 0, since this event cannot be created
     *         for NUL character.
     */
    public int getCodePoint() {
        return this.unicodeCharacter.codePointAt(0);
    }
    
    /**
     * @return Whether this event is a repetition of a previous event
     *         with same code point.
     */
    public boolean isRepeat() {
        return this.isRepeat;
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * For usage by trusted code.
     * 
     * @param unicodeCharacter Must be a valid Unicode character
     *        (not empty, not NUL character) (not checked).
     * @param modifierKeyDownSet Instance to use internally. Must never be modified.
     */
    BwdKeyEventT(
            Void nnul,
            //
            Object source,
            //
            String unicodeCharacter,
            boolean isRepeat,
            //
            SortedSet<Integer> modifierKeyDownSet) {
        super(
                nnul,
                //
                source,
                BwdEventType.KEY_TYPED,
                //
                modifierKeyDownSet);
        
        this.unicodeCharacter = unicodeCharacter;
        
        this.isRepeat = isRepeat;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @param codePoint Must be a valid code point, and must not be 0 (NUL character).
     * @return The specified code point.
     * @throws IllegalArgumentException if the specified code point is 0 (NUL character).
     */
    private static String checkedUnicodeCharacter(int codePoint) {
        if (codePoint == 0) {
            throw new IllegalArgumentException("must not be NUL character");
        }
        return LangUtils.stringOfCodePoint(codePoint);
    }
}
