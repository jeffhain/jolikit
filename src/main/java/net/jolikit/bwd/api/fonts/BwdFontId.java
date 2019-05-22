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
package net.jolikit.bwd.api.fonts;

import net.jolikit.lang.LangUtils;

/**
 * Identifies a font from a same font home.
 * 
 * Immutable.
 * 
 * hashCode(), equals(Object) overridden so that can be used as key.
 */
public final class BwdFontId implements Comparable<BwdFontId> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final BwdFontKind kind;
    
    private final int size;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Construction.
     */
    
    /**
     * @param kind Font kind.
     * @param size Font size in pixels. Must be > 0.
     * @throws NullPointerException if the specified font kind is null.
     * @throws IllegalArgumentException is font size is <= 0.
     */
    public BwdFontId(
            BwdFontKind kind,
            int size) {
        this.kind = LangUtils.requireNonNull(kind);
        if (size <= 0) {
            throw new IllegalArgumentException("size [" + size + "] must be > 0");
        }
        this.size = size;
    }

    /**
     * Convenience constructor. Creates a font kind internally.
     * 
     * @see #BwdFontStyles
     * 
     * @param family Font family.
     * @param style Font style.
     * @param size Size in pixels. Must be > 0.
     * @throws NullPointerException if the specified font family is null.
     * @throws IllegalArgumentException is font size is <= 0.
     */
    public BwdFontId(
            String family,
            int style,
            int size) {
        this(
                new BwdFontKind(family, style),
                size);
    }
    
    /*
     * Generic.
     */

    @Override
    public String toString() {
        return "["
                + this.kind.fontFamily()
                + ", "
                + BwdFontStyles.toString(this.kind.fontStyle())
                + ", "
                + this.size
                + "]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hc = this.kind.hashCode();
        hc = prime * hc + this.size;
        return hc;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BwdFontId)) {
            return false;
        }
        final BwdFontId other = (BwdFontId) obj;
        return (this.size == other.size)
                && this.kind.equals(other.kind);
    }

    /**
     * Orders first by font kind, and then by font size.
     */
    @Override
    public int compareTo(BwdFontId other) {
        {
            final int cmp = this.kind.compareTo(other.kind);
            if (cmp != 0) {
                return cmp;
            }
        }
        return this.size - other.size;
    }
    
    /*
     * Getters.
     */
    
    /**
     * @return Font kind.
     */
    public BwdFontKind fontKind() {
        return this.kind;
    }
    
    /**
     * @return Font size in pixels. Always > 0.
     */
    public int fontSize() {
        return this.size;
    }
}

