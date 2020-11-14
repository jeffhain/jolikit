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

/**
 * Interface for BWD fonts.
 * 
 * What a font is in the messy world out there, is a mess, cf.
 * "http://v1.jontangerine.com/log/2008/08/typeface--font",
 * but for us, a font is:
 * - when we talk about a font to load, something identified
 *   by an instance of BwdFontKind, which corresponds to a "family" (String)
 *   and a "style" (int). 
 * - when we talk about a font to use for drawing text, something identified
 *   by an instance of BwdFontId, which corresponds to a BwdFontKind
 *   plus a "font size" (int).
 * This interface corresponds to the second meaning.
 * 
 * Also, in some libraries, such as AWT, a font metrics depends on the graphics
 * the font is used for, but BWD graphics have no effect on a font's metrics
 * so for us it doesn't.
 * 
 * Note that for a same font id, as many instances of (disposable) fonts
 * exist as created by font home's newFontXxx(...) methods, and the eventual
 * backing font can only be disposed once the last one is disposed.
 * 
 * hashCode() and equals(Object) must be overridden such as
 * two fonts of same font home and font id are considered equal.
 * 
 * Not requiring for Comparable interface to be implemented,
 * for it could be ambiguous if ever comparing fonts from
 * different bindings.
 */
public interface InterfaceBwdFont {

    /**
     * @return An int value that is the same for all fonts from
     *         a same font home that have equal font ids.
     */
    @Override
    public int hashCode();

    /**
     * @param obj An object.
     * @return True if the specified object is a font from the same font home
     *         than this one and their font id are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj);

    /**
     * @return The font id of this font, i.e. its identifier among fonts
     *         of a same font home, i.e. everything that defines it.
     */
    public BwdFontId id();
    
    /**
     * Convenience method, equivalent to id().kind().
     * 
     * @return The kind of this font, i.e. everything that defines it except its size.
     */
    public BwdFontKind kind();
    
    /**
     * Convenience method, equivalent to kind().family().
     * 
     * @return The family of this font.
     */
    public String family();

    /*
     * Style.
     */

    /**
     * Convenience method, equivalent to kind().style().
     * 
     * @return The style of this font.
     */
    public int style();
    
    /**
     * Convenience method, equivalent to kind().isBold().
     * 
     * @return Whether this font is bold.
     */
    public boolean isBold();

    /**
     * Convenience method, equivalent to kind().isItalic().
     * 
     * Using "italic" wording, because it's much more common that the two others,
     * and should not cause much confusion if ever.
     * 
     * @return Whether this font is italic, or oblique, i.e. slanted.
     */
    public boolean isItalic();
    
    /*
     * Size.
     */
    
    /**
     * Convenience method, equivalent to id().size().
     * 
     * Note that metrics().height() is a better measure
     * of the actual number of pixels used vertically by the font.
     * 
     * @return The size in pixels. Must be > 0.
     */
    public int size();
    
    /*
     * Glyphs.
     */
    
    /**
     * Usual font formats, such as TrueType or OpenType, have a 16-bits (only)
     * glyph index, and font designers typically don't want to bother defining
     * a glyph for each possible code point.
     * As a result, fonts typically can't display all possible code points.
     * This method allows to know whether this font can display a specified
     * code point.
     * This allows the user to decide on a font to use for a given individual
     * character or a given string, or, if no available font can draw it,
     * to decide to just draw corresponding hexadecimal code point values
     * instead (which could be done through using a logical font as well,
     * if your binding's font home can provide such a font).
     * It also allows, when drawing some text only with a font that can display
     * all of its code points, to know that the displayed text has been drawn
     * with the chosen font (in particular when using some libraries, such as
     * JavaFX, which silently use fallback fonts when the chosen font doesn't
     * have a glyph, which tricks the user into believing that what he sees
     * corresponds to its chosen font).
     * 
     * It could be more practical to return a set of ranges
     * of displayable code points, as some libraries do,
     * but it could be too heavy to compute depending on the
     * backing library API, especially in pathological cases,
     * so we prefer to just stick to this simple method.
     * 
     * In case of doubt, should return false, so that a font that really
     * can display the specified code point can be chosen instead.
     * In particular, if the backing library doesn't have such a method,
     * but allows to compute a glyph width for the specified code point,
     * should only return true if this width is > 0 (even thought some
     * code points are meant to be displayed with zero width glyphs).
     * And in case of total cluelessness (such as with SWT),
     * should just always return false.
     * 
     * @param codePoint A valid code point.
     * @return True if this font has a glyph for the specified code point,
     *         false otherwise.
     * @throws IllegalArgumentException if the specified code point is invalid.
     */
    public boolean canDisplay(int codePoint);
    
    /*
     * Metrics.
     */
    
    /**
     * @return This font's metrics.
     */
    public InterfaceBwdFontMetrics metrics();
    
    /*
     * Disposal.
     */
    
    /**
     * @return Whether this font is disposed, which can be due to a call
     *         to an effective dispose() method on this instance,
     *         or to another reason, such as binding shutdown.
     */
    public boolean isDisposed();

    /**
     * If this font is a "disposable font", i.e. if it has been created
     * using one of font home's newFontXxx(...) methods, disposes it,
     * else does nothing.
     */
    public void dispose();
}
