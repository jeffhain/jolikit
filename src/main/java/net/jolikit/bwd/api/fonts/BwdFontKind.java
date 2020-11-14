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
package net.jolikit.bwd.api.fonts;

import net.jolikit.lang.LangUtils;

/**
 * Identifies a font from a same font home, regardless of its size.
 * 
 * Immutable.
 * 
 * hashCode(), equals(Object) overridden so that can be used as key.
 */
public final class BwdFontKind implements Comparable<BwdFontKind> {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final String family;
    
    /*
     * Style.
     * 
     * Using an int as bit field, even for just two values,
     * because it can be handy to have it as a single primitive type,
     * and it doesn't cost much to have additional convenience methods
     * dealing with booleans.
     */
    
    private final int style;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Construction.
     */
    
    /**
     * Uses normal font style.
     * 
     * @param family Font family.
     * @throws NullPointerException if the specified font family is null.
     */
    public BwdFontKind(String family) {
        this(family, BwdFontStyles.NORMAL);
    }

    /**
     * @see #BwdFontStyles
     * 
     * @param family Font family.
     * @param style Font style.
     * @throws NullPointerException if the specified font family is null.
     */
    public BwdFontKind(
            String family,
            int style) {
        this.family = LangUtils.requireNonNull(family);
        this.style = style;
    }

    /**
     * Convenience constructor.
     * 
     * @param family Font family.
     * @param bold Whether the style to use must have bold bit set.
     * @param italic Whether the style to use must have italic bit set.
     * @throws NullPointerException if the specified font family is null.
     */
    public BwdFontKind(
            String family,
            boolean bold,
            boolean italic) {
        this.family = LangUtils.requireNonNull(family);
        this.style = BwdFontStyles.of(bold, italic);
    }
    
    /*
     * Derivation.
     */
    
    /**
     * @param style Style to use for the derived font kind.
     * @return The corresponding derived font kind.
     */
    public BwdFontKind withStyle(int style) {
        return new BwdFontKind(this.family, style);
    }
    
    /**
     * @param bold Whether the style to use must have bold bit set,
     *        other bits not being modified.
     * @return The corresponding derived font kind.
     */
    public BwdFontKind withBold(boolean bold) {
        return new BwdFontKind(this.family, BwdFontStyles.withBold(this.style, bold));
    }
    
    /**
     * @param italic Whether the style to use must have italic bit set,
     *        other bits not being modified.
     * @return The corresponding derived font kind.
     */
    public BwdFontKind withItalic(boolean italic) {
        return new BwdFontKind(this.family, BwdFontStyles.withItalic(this.style, italic));
    }

    /*
     * Generic.
     */
    
    @Override
    public String toString() {
        return "[" + this.family + ", " + BwdFontStyles.toString(this.style) + "]";
    }
    
    @Override
    public int hashCode() {
        int hc = 0;
        final int prime = 31;
        hc = hc * prime + this.family.hashCode();
        hc = hc * prime + this.style;
        return hc;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BwdFontKind)) {
            return false;
        }
        final BwdFontKind other = (BwdFontKind) obj;
        // Easy check first.
        return (this.style == other.style)
                && this.family.equals(other.family);
    }
    
    /**
     * Orders first by font family, and then by font style.
     */
    @Override
    public int compareTo(BwdFontKind other) {
        {
            final int cmp = this.family.compareTo(other.family);
            if (cmp != 0) {
                return cmp;
            }
        }
        {
            final int cmp = this.style - other.style;
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }
    
    /*
     * Getters.
     */

    /**
     * @return Font family.
     */
    public String family() {
        return this.family;
    }
    
    /**
     * @return Font style.
     */
    public int style() {
        return this.style;
    }
    
    /*
     * Computations.
     */
    
    /**
     * @return Whether font style has bold bit set.
     */
    public boolean isBold() {
        return BwdFontStyles.isBold(this.style);
    }
    
    /**
     * @return Whether font style has italic bit set.
     */
    public boolean isItalic() {
        return BwdFontStyles.isItalic(this.style);
    }
}
