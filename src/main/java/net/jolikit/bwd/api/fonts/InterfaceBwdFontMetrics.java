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
 * Interface for BWD fonts metrics.
 * 
 * Having a specific interface for metrics, even though we could merge it
 * into font interface, to keep concerns separated.
 * In particular, font metrics themselves are not really reliable, but font size
 * is in practice even less reliable as a metric, and is more useful just as
 * the part of font identifiers which increases monotonically with the heights,
 * such as it's clearer to isolate actual metrics in a separate object.
 * 
 * Metrics:
 * 
 * OOOO            -------------------------------------------
 * O  O                |                       |             |
 * OOOO  OO  O   O  ascent                     |             |
 * O O  O  O  O O      |                  font height        |
 * O  O  OO    O   ----------- baseline        |        line height
 *             O    descent                    |             |
 *             O   -----------------------------             |
 *                  leading                                  |
 * OOOO            -------------------------------------------
 * O  O
 * OOOO  OO  O   O
 * O O  O  O  O O
 * O  O  OO    O
 *             O
 *             O
 * 
 * NB:
 * - For some orthographies, baseline might not make sense,
 *   in this case it can be chose arbitrarily.
 * - The leading is just an advised spacing for lines of text.
 * - What we call "leading" is sometimes called "line height".
 * - Some libraries use "height" or "font height" to designate
 *   what we call "line height".
 * 
 * We ignore leading and line height in this API, because for most fonts
 * and libraries leading seems to always be 0, and because since fonts metrics
 * aren't really reliable (a lot of glyphs often leak outside, either on their
 * own or due to diacritical marks usage) lines spacing should be configurable
 * aside anyway.
 */
public interface InterfaceBwdFontMetrics {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * This value is >= 0.
     * 
     * @return Font ascent, i.e. max ascent for any of its glyphs.
     */
    public int ascent();
    
    /**
     * This value is >= 0.
     * 
     * @return Font descent, i.e. max descent for any of its glyphs.
     */
    public int descent();
    
    /**
     * This value is > 0.
     * 
     * Not allowing 0, to avoid useless pathological special cases,
     * which could easily cause trouble.
     * 
     * @return Font height, which is ascent + descent.
     */
    public int height();
    
    /**
     * Convenience method not to force a string creation
     * (overhead and boilerplate) when user just has a code point.
     * 
     * Must return the same value than computeTextWidth(...)
     * for a string just containing the specified code point,
     * and as a result, must return the advance width,
     * not just the "trimmed" width of glyph's painted pixels.
     * 
     * @param codePoint A valid code point.
     * @return Width of a text containing only the specified code point.
     *         Must be >= 0.
     * @throws IllegalArgumentException if the specified code point is invalid.
     */
    public int computeCharWidth(int codePoint);
    
    /**
     * Should behave consistently with InterfaceBwdGraphics.drawText(...).
     * For example, if drawText(...) doesn't stop early on NUL,
     * the computed width shouldn't stop early on NUL either.
     * 
     * The returned width might be different than the sum of the widths
     * of each code point's glyph, due to ligatures, kerning, or other
     * subtleties, such as the backing library not taking into account
     * post-glyph advance for the last glyph.
     * 
     * @return Width of the specified text. Must be >= 0.
     */
    public int computeTextWidth(String text);
}
