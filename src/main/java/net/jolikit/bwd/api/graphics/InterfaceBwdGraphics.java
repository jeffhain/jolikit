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
package net.jolikit.bwd.api.graphics;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Coordinates system:
 * In BWD, (0,0) is the top-left corner of the client area, and, as usual
 * in UI libraries, x positive values are going right and y positive values
 * are going down.
 * Also, (x,y) position corresponds to the center of the pixel of indexes (x,y),
 * not to its top-left corner as done in libraries such as AWT, and a figure
 * with a span of 3 means a 3-pixel wide figure (span = max - min + 1).
 * We do that for the following reasons:
 * - By having an obvious bijection between coordinates and pixels,
 *   and an identity between spans and the number of pixels drawn,
 *   it makes it easier to reason about which pixels are drawn.
 * - The convention of integer coordinates being at pixels corners is meant to
 *   make things more consistent in some way between aliased and anti-aliased
 *   paintings, but we give priority to simplicity over anti-aliasing.
 * - It makes drawXXX(...) and fillXXX(...) methods consistent
 *   (in AWT, drawRect(,,3,3) will paint a 4-pixels wide square,
 *   whereas fillRect(,,3,3) will paint a 3-pixels wide square),
 *   in particular across rotations (in AWT, the bottom-right pixel
 *   being chosen after transforms to client area coordinates,
 *   painting a figure with both drawRect(...) and fillRect(...)
 *   will yield 1-pixel shifts between these methods effects depending
 *   on rotations, unless of course rotations are taken into account when
 *   using them, which complicates the drawing treatments).
 * 
 * Coordinates ranges and transforms:
 * The x and y coordinates of figures to draw must be contained in the
 * [Integer.MIN,VALUE,Integer.MAX_VALUE] range (i.e. if x = Integer.MAX_VALUE,
 * xSpan can only be 0 or 1), both in user frame of reference and once
 * transformed into client frame of reference.
 * A good practice would be to avoid both coordinates and translations
 * to remain inferior in magnitude to Integer.MAX_VALUE/4.
 * 
 * Children graphics:
 * A newChildGraphics(...) method allows for creation of a child graphics,
 * i.e. a graphics allowing to draw in a rectangular part of the client area
 * included in the base clip of its parent.
 * 
 * Negative spans:
 * Unlike the API of GRect, which is strict, methods defined here accept
 * negative spans without throwing, in which case they must have no
 * visible effect.
 * This can be convenient, and also should not hurt, because if something
 * is surprisingly not drawn, just adding spans checks before calling
 * graphics methods will tell whether this is a span issue.
 * 
 * Overflowing rectangles:
 * As a general rule, rectangle arguments that overflow out of int range
 * (due to position + span being too large), whether in x or y, and whether
 * they are specified as a GRect or as four ints, must be trimmed before use
 * (i.e., in user coordinates), that is, for methods that accepted them as
 * valid arguments.
 * The point is to allow user not to have to worry about specifying
 * non-overflowing rectangles, and to not complicate implementations
 * with having to deal with such pathological cases.
 * 
 * Overflows due to transforms:
 * Figures bounding boxes coordinates must not be moved out of int range
 * by transforms. If that ever happens, the binding should either try to paint
 * the wrong pixels (due to integer modulo for example), which should most likely
 * be out of clip though, or try to paint less pixels than the figure covers
 * (possibly none).
 * 
 * Clips and transforms:
 * We use the client coordinates for all graphics (initial one or descendants
 * of it). This makes events coordinates usable in all children graphics
 * without need for an offset, and makes figuring out what happens and
 * debugging easier.
 * Though, to simplify painting treatments (for example using same code
 * for different positions and orientations), and for performances reasons
 * (not computing the backing transform for each draw method call), a stack of
 * transforms is stored in each graphics. These transforms are not inherited
 * by children graphics.
 * Transformed coordinates are called user coordinates.
 * Similarly, a stack of clips can be added over the base clip.
 * Getters are provided for these transforms and clips properties.
 * Clips are always specified in client coordinates, but it's easy to compute
 * a clip in client coordinates from a clip in user coordinates using
 * GTransform.rectIn1(clipInUser).
 * 
 * Clipping:
 * How primitive drawing computes clipping is undefined. For example,
 * if a line has extremities outside the clip, new integer coordinates
 * can be computed within the clip (which typically doesn't preserve slope
 * accurately and can cause slightly different pixels to be drawn,
 * but should be the fastest way, especially on devices with slow
 * floating point arithmetics), or floating points coordinates be computed
 * within clip and used along with y = a * x + b equation and rounding
 * (and then, an extended clip could be considered, to make sure to draw
 * pixels corresponding to parts of the line that are out of clip, but that
 * round to pixels that are within the clip).
 * 
 * Blending:
 * All methods must draw using SRC_OVER blending (which corresponds to drawing
 * new color on top of previous one) ("the fundamental image compositing
 * operator" dixit Ray Smith), except of course clear method(s) which use
 * SRC blending (new color erasing previous one).
 * 
 * Colors:
 * Each graphics contains a current color, used by methods that draw or fill
 * primitives, or that draw text. The initial color for each graphics must be
 * opaque black (0xFF000000 32 bits ARGB).
 * Note that bindings might only be able to use approximations of the colors
 * set by users, depending on their capabilities (for example most libraries
 * don't support 64 bits ARGB, only 32 bits, and some libraries might not
 * support alpha and consider all colors opaque).
 * 
 * Fonts:
 * Each graphics contains a current font, used by methods that draw text.
 * The initial font for each graphics must be font home's default font.
 * 
 * Default pixels colors:
 * Pixels colors for pixels not yet painted with a BWD graphics
 * (such as at first client area painting, or on client area growth),
 * are undefined, which allows to avoid the overhead of an eventually
 * useless initial clearing, but if a default color can be chosen
 * without additional overhead it should rather be black, which is
 * the simplest (rgb = 0, no light on screen).
 * Pixels colors for pixels already painted in a fully opaque way with
 * a BWD graphics, should be preserved by the binding, such as pixels
 * not repainted would remain identical on screen.
 * Whenever the binding can't preserve them (due to window iconification
 * and then maximization, or whatever reason), it must ensure a pending
 * complete repaint, i.e. with a dirty rectangle containing the whole
 * client area. No such full dirty rectangle must be ensured at first
 * painting, since in this case the client should be aware that it didn't
 * paint anything yet and that everything needs to be painted.
 * Note that some bindings, such as Qt, do some unavoidable clear of the
 * whole client area at each painting, which means that they must specify
 * a full dirty rectangle at every painting.
 * Pixels colors for pixels that have only be painted with non fully opaque
 * colors, can be preserved until repaint too but that is not of much use
 * since they did blend with undefined colors, and are therefore also
 * undefined.
 * Furthermore, you can't count on blending with colors preserved from
 * previous paintings, in a cumulative manner, without a prior call to
 * clearRectOpaque(...) during the painting, due to the binding
 * eventually clearing the whole client area at each painting.
 * As a result, for consistency across paintings, every non fully opaque
 * painting should take place over an area previously cleared (i.e. with
 * a fully opaque color) during that painting.
 * 
 * Threading:
 * Graphics are by default not supposed to be thread-safe, but if the binding
 * supports parallel drawing, then the method newChildGraphics(...) must be
 * callable concurrently while the graphics is not being modified.
 */
public interface InterfaceBwdGraphics {
    
    /*
     * Methods arguments order:
     * 1) where to draw to (location, box, etc.)
     * 2) what to draw (stipple, string, image, etc.)
     * 3) where to draw from (image part, etc.)
     */
    
    /**
     * If binding.isParallelPaintingSupported() is true,
     * must return binding.getParallelizer(),
     * else a parallelizer that just executes sequentially.
     * This allows to have a single parallelizer for both
     * painting and non-painting work done from UI thread,
     * whether or not parallel painting is supported.
     * 
     * When painting and deciding whether or not to go parallel,
     * instead of checking binding.isParallelPaintingSupported() and
     * binding.getParallelizer().getParallelism(), you just have to check
     * whether getPaintingParallelizer().getParallelism() is >= 2.
     * 
     * @return A parallelizer that can be used for parallel painting,
     *         or a sequential parallelizer if parallel painting
     *         is not supported be the binding.
     */
    public InterfaceParallelizer getPaintingParallelizer();
    
    /*
     * 
     */

    /**
     * Children graphics allow:
     * - to give treatments designed to paint only a sub part of the client area
     *   (such as a component's view) a graphics that makes sure they won't leak
     *   out of it,
     * - for parallel painting (for bindings that support it), in which case
     *   a child graphics must be created before switching to the thread
     *   that uses it for init/drawing/finish.
     * Children graphics don't inherit any state from their parent, except
     * that their base clip must be included in their parent's base clip.
     * If a same pixel is painted with multiple graphics (like a graphics and
     * a child of it, or two overlapping (dangerous practice) children graphics),
     * its resulting color is undefined (for example it might
     * depend on the order of calls to graphics.finish()).
     * 
     * This method must be usable as long as finish() has not been called,
     * and even before call to init().
     * 
     * @param childBox The box, in client coordinates, for the child graphics
     *        to create.
     *        Not necessarily included in this graphics box (for example
     *        in case of a component in a scroll pane).
     * @return A new child graphics with the specified box, and a base clip
     *         equal to the intersection of this graphics base clip with the
     *         specified box.
     * @throws NullPointerException if the specified box is null.
     * @throws IllegalStateException if finish() has been called.
     */
    public InterfaceBwdGraphics newChildGraphics(GRect childBox);
    
    /**
     * Must be called before use of other methods, to ensure
     * proper configuration (clip, color, font, etc.) of the backing graphics,
     * which might previously have been configured differently by other
     * BWD graphics for previous drawings.
     * 
     * Not requiring graphics to be initialized by default,
     * for some implementations might only have a single backing graphics
     * per binding (such as JavaFX GraphicsContext), which therefore
     * needs to be initialized properly before each use with a BWT graphics.
     * 
     * Must not be called after this graphics has been modified
     * (color, transform, etc), so that implementations can use it
     * to save the backing initial state to restore on finish().
     * 
     * Must do nothing if already called and finish() has not been called yet,
     * to allow for efficient {n * init(), (painting), n * finish()} calls.
     * 
     * Not requiring drawing methods to throw IllegalStateException
     * if used before init() or after finish(), for performances reasons,
     * but they could do that.
     * 
     * @throws IllegalStateException if finish() has been called already
     *         (even if it did nothing due to init() not having been called yet).
     */
    public void init();
    
    /**
     * Must be called after graphics usage, because depending on the implementation,
     * not calling it properly could cause some resources not to be released.
     * 
     * Must do nothing if already called.
     */
    public void finish();
    
    /*
     * Painting box.
     */
    
    /**
     * The base clip is included in this box.
     * 
     * @return The box, in client coordinates, in which the visual object
     *         this graphics is used for must be painted.
     *         Must not be empty: if empty, the painting must not occur
     *         in the first place.
     */
    public GRect getBoxInClient();

    /*
     * Clips.
     */
    
    /**
     * Can be empty, for example when painting a non-visible part
     * in order to obtain values useful for painting a visible part.
     * 
     * If not empty, this clip is included in this graphics box.
     * 
     * @return The base clip for this graphics, in client coordinates.
     */
    public GRect getBaseClipInClient();

    /**
     * @return The base clip for this graphics, in user coordinates.
     */
    public GRect getBaseClipInUser();

    /**
     * @return The current clip, in client coordinates.
     */
    public GRect getClipInClient();
    
    /**
     * @return The current clip, in user coordinates.
     */
    public GRect getClipInUser();

    /**
     * Adds the specified clip over base clip and any already added clip.
     * 
     * @param clip Clip to add to current clip, in client coordinates.
     * @throws NullPointerException if the specified clip is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void addClipInClient(GRect clip);
    
    /**
     * Convenience method.
     * Equivalent to "addClipInClient(transform.rectIn1(clip))".
     * 
     * @param clip Clip to add to current clip, in user coordinates.
     * @throws NullPointerException if the specified clip is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void addClipInUser(GRect clip);
    
    /**
     * Removes last added clip if any.
     * If there is no added clip, does nothing, i.e. clip remains base clip.
     * 
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void removeLastAddedClip();
    
    /**
     * Removes all added clips if any.
     * If there is no added clip, does nothing, i.e. clip remains base clip.
     * 
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void removeAllAddedClips();

    /*
     * Transforms.
     */
    
    /**
     * Initial value for any graphics must be identity.
     * 
     * @return The current transform between client coordinates (frame 1)
     *         to user coordinates (frame 2).
     */
    public GTransform getTransform();

    /**
     * Clears added transforms stack, and then sets the specified one
     * (not making it part of added transforms stack, i.e. a subsequent
     * call to removeLastAddedTransform() won't do anything).
     * 
     * Using identity transform resets this graphics transform state.
     * 
     * @param transform The transform to use between client coordinates
     *        (frame 1) and user coordinates (frame 2).
     * @throws NullPointerException if the specified transform is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void setTransform(GTransform transform);

    /**
     * @param transform Transform to compose with the one currently in use,
     *        using right composition, i.e. current one is (1,2), specified
     *        one is (2,3), resulting one is (1,3).
     * @throws NullPointerException if the specified transform is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void addTransform(GTransform transform);

    /**
     * Removes last added transform if any.
     * If there is no added transform, does nothing, i.e. transform remains
     * previously set one if any, else identity.
     * 
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void removeLastAddedTransform();
    
    /**
     * Removes all added transforms if any.
     * If there is no added transform, does nothing, i.e. transform remains
     * previously set one if any, else identity.
     * 
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void removeAllAddedTransforms();

    /*
     * Colors.
     */
    
    /**
     * Initial value for any graphics must be black.
     * 
     * Must return a color corresponding exactly to the last set color,
     * whether it has been set with setColor(...), setArgb64(...)
     * or setArgb32(...), even if rendering can only use an
     * approximation of it.
     * 
     * @return The current color.
     */
    public BwdColor getColor();
    
    /**
     * @param color The color to use for painting (other than images).
     * @throws NullPointerException if the specified color is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void setColor(BwdColor color);
    
    /**
     * Initial value for any graphics must be black.
     * 
     * Must return a color corresponding exactly to the last set color,
     * whether it has been set with setColor(...), setArgb64(...)
     * or setArgb32(...), even if rendering can only use an
     * approximation of it.
     * 
     * @return The current color, as 64 bits ARGB.
     */
    public long getArgb64();

    /**
     * @param argb64 The color, as 64 bits ARGB,
     *        to use for painting (other than images).
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void setArgb64(long argb64);
    
    /**
     * Must be equivalent to Argb3264.toArgb32(getArgb64()).
     * 
     * @return The current color, as 32 bits ARGB.
     */
    public int getArgb32();

    /**
     * Must be equivalent to setArgb64(Argb3264.toArgb64(argb32)).
     * 
     * @param argb32 The color, as 32 bits ARGB,
     *        to use for painting (other than images).
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void setArgb32(int argb32);
    
    /*
     * Fonts.
     */
    
    /**
     * Initial value for any graphics must be default font.
     * 
     * @return The current font.
     */
    public InterfaceBwdFont getFont();
    
    /**
     * @param font The font to use for drawing text.
     * @throws NullPointerException if the specified font is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void setFont(InterfaceBwdFont font);

    /*
     * Clearing.
     */
    
    /**
     * Clears the specified rectangle, with the current color.
     * The rectangle can cover pixels outside of the clip,
     * in which case they must not be modified.
     * Must clear nothing if any span is <= 0.
     * 
     * Clearing means filling using SRC blend mode, with a fully opaque color
     * using {r,g,b} components of current color.
     * 
     * We don't provide a way to clear with semi or fully transparent colors,
     * because some libraries don't handle non-fully opaque clearings properly
     * (ex.: JavaFX, Qt4, Allegro5 and SDL2 seem to interpret 0x80FFFFFF
     * background as grey, instead of fully black), and a fully transparent
     * clear color is ambiguous (and often translates to either black or white)
     * for libraries not supporting truly (independently of window alpha)
     * transparent windows, which is the case for all those I used for the
     * design of this API.
     * 
     * Note that this method is a convenience method, since setting an opaque
     * color and calling fillRect(...) must yield the same result.
     * 
     * @param x X position, in user coordinates.
     * @param y Y position, in user coordinates.
     * @param xSpan X span, in user coordinates.
     * @param ySpan Y span, in user coordinates.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void clearRectOpaque(int x, int y, int xSpan, int ySpan);

    /**
     * Convenience method.
     * Equivalent to clearRectOpaque(rect.x(),rect.y(),rect.xSpan(),rect.ySpan()).
     * 
     * @param rect Rectangle to clear, in user coordinates.
     * @throws NullPointerException if the specified rectangle is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void clearRectOpaque(GRect rect);

    /*
     * Points.
     * 
     * Not defining a drawPoint(GPoint) method,
     * for it wouldn't help much (only one less argument),
     * and would create a dependency to GPoint.
     */

    /**
     * Draws the specified pixel, with the current color.
     * The pixel can be outside of the clip, in which case
     * it must not be modified.
     * 
     * Not called "drawPixel" to stay in the realm of abstract geometry
     * nomenclature that inspires other methods names.
     * 
     * @param x X position, in user coordinates.
     * @param x Y position, in user coordinates.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawPoint(int x, int y);
    
    /*
     * Lines.
     */
    
    /**
     * Draws a line between the specified pixels, with the current color.
     * The line can cover pixels outside of the clip,
     * in which case they must not be modified.
     * 
     * The line should be clipped properly (visible pixels drawn as if there
     * was no clipping), and, except in case of bindings designed to use
     * anti-aliasing or thick drawing, be drawn such as the coordinate with
     * the most span always increments from a drawn pixel to the next.
     * 
     * @param x1 X position of first point, in user coordinates.
     * @param y1 Y position of first point, in user coordinates.
     * @param x2 X position of second point, in user coordinates.
     * @param y2 Y position of second point, in user coordinates.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawLine(int x1, int y1, int x2, int y2);
    
    /**
     * Draws a stippled line between the specified pixels,
     * with the current color.
     * The line can cover pixels outside of the clip,
     * in which case they must not be modified.
     * 
     * Derived from OpenGL API.
     * 
     * If pixelNum usage is not supported, the stipple should always start
     * "from scratch", which corresponds to a pixelNum of 0.
     * 
     * The pattern is read least significant bits first, i.e. for 0x00FF,
     * with a factor of 1, going from point 1 to point 2,
     * 8 pixels will be drawn then 8 pixels skipped.
     * 
     * Pixel num to continue drawing is computed and returned,
     * because the proper pixel num to continue the line depends
     * on how many pixel its drawing algorithm covered.
     * 
     * Pixel num must be incremented during drawing from non-clipped line start
     * to non-clipped line end, such as growing or shrinking the clip
     * should not shift the visible stipples.
     * We dare to specify this, because the amount of skipped pixels due to clip
     * should always be computable in O(1).
     * 
     * Since the pattern has 16 bits, we need (pixelNum / max_factor)
     * to covert at least 2^16 possible values. As a result, for factor
     * we need to have a max bound of at most (2^32 / 2^16) = 2^16.
     * We use 2^8 = 256 as max factor, as OpenGL does (by clamping it
     * if too large, not throwing as we do).
     * 
     * During drawing, if the specified pixelNum increased above
     * the limit (factor * 2^16), it must be taken back into
     * [0,limit[ range using modulo with limit, to avoid wrapping
     * which could corrupt pattern computation.
     * 
     * @param x1 X position of first point, in user coordinates.
     * @param y1 Y position of first point, in user coordinates.
     * @param x2 X position of second point, in user coordinates.
     * @param y2 Y position of second point, in user coordinates.
     * @param factor Must be in [1,256].
     * @param pattern Pattern of pixels to draw, read from least significant
     *        to most significant bits.
     * @param pixelNum Indicates, with factor, which part of the pattern
     *        the stipple must start at. Must be >= 0.
     * @return The pixelNum for continuing the stipple, or the specified
     *         pixelNum if its computation is not supported (whether the
     *         specified pixelNum has been used or not).
     * @throws IllegalArgumentException if factor is not in [1,256],
     *         or if pixelNum is < 0.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public int drawLineStipple(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum);

    /*
     * Rectangles.
     */
    
    /**
     * Draws the specified rectangle, with the current color.
     * The rectangle can cover pixels outside of the clip,
     * in which case they must not be modified.
     * Must drawn nothing if any span is <= 0.
     * 
     * @param x Rectangle x position, in user coordinates.
     * @param y Rectangle y position, in user coordinates.
     * @param xSpan Rectangle x span, in user coordinates.
     * @param ySpan Rectangle y span, in user coordinates.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawRect(int x, int y, int xSpan, int ySpan);
    
    /**
     * Convenience method.
     * Equivalent to drawRect(rect.x(),rect.y(),rect.xSpan(),rect.ySpan()).
     * 
     * @param rect Rectangle to draw, in user coordinates.
     * @throws NullPointerException if the specified rectangle is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawRect(GRect rect);

    /**
     * Fills the specified rectangle, with the current color.
     * The rectangle can cover pixels outside of the clip,
     * in which case they must not be modified.
     * Must fill nothing if any span is <= 0.
     * 
     * @param x Rectangle x position, in user coordinates.
     * @param y Rectangle y position, in user coordinates.
     * @param xSpan Rectangle x span, in user coordinates.
     * @param ySpan Rectangle y span, in user coordinates.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void fillRect(int x, int y, int xSpan, int ySpan);
    
    /**
     * Convenience method.
     * Equivalent to fillRect(rect.x(),rect.y(),rect.xSpan(),rect.ySpan()).
     * 
     * @param rect Rectangle to fill, in user coordinates.
     * @throws NullPointerException if the specified rectangle is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void fillRect(GRect rect);

    /*
     * Ovals.
     */
    
    /**
     * Draws the specified oval, with the current color.
     * The oval can cover pixels outside of the clip,
     * in which case they must not be modified.
     * Must draw nothing if any span is <= 0.
     * 
     * @param x X of oval bounding box, in user coordinates.
     * @param y Y of oval bounding box, in user coordinates.
     * @param xSpan X span of oval bounding box, in user coordinates.
     * @param ySpan Y span of oval bounding box, in user coordinates.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawOval(int x, int y, int xSpan, int ySpan);
    
    /**
     * Convenience method.
     * Equivalent to drawOval(rect.x(),rect.y(),rect.xSpan(),rect.ySpan()).
     * 
     * @param rect The oval bounding box, in user coordinates.
     * @throws NullPointerException if the specified bounding box is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawOval(GRect rect);
    
    /**
     * Fills the specified oval, with the current color.
     * The oval can cover pixels outside of the clip,
     * in which case they must not be modified.
     * Must fill nothing if any span is <= 0.
     * 
     * @param x X of oval bounding box, in user coordinates.
     * @param y Y of oval bounding box, in user coordinates.
     * @param xSpan X span of oval bounding box, in user coordinates.
     * @param ySpan Y span of oval bounding box, in user coordinates.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void fillOval(int x, int y, int xSpan, int ySpan);
    
    /**
     * Convenience method.
     * Equivalent to fillOval(rect.x(),rect.y(),rect.xSpan(),rect.ySpan()).
     * 
     * @param rect The oval bounding box, in user coordinates.
     * @throws NullPointerException if the specified bounding box is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void fillOval(GRect rect);
    
    /*
     * Arcs.
     * NB: Arc angles are anti-clockwise on screen, but also
     * in our frame of reference, since X goes right and Y goes down.
     */
    
    /**
     * Draws the specified arc, with the current color.
     * The arc can cover pixels outside of the clip,
     * in which case they must not be modified.
     * Must draw nothing if any span is <= 0.
     * 
     * The specified angles are used directly in oval equation,
     * which is, with x and y relative to oval center:
     * x = rx * cos(ang)
     * y = ry * sin(ang)
     * 
     * @param x X of full arc bounding box, in user coordinates.
     * @param y Y of full arc bounding box, in user coordinates.
     * @param xSpan X span of full arc bounding box, in user coordinates.
     * @param ySpan Y span of full arc bounding box, in user coordinates.
     * @param startDeg Start angle, in degrees, from positive X axis
     *        and anti-clockwise.
     * @param spanDeg Drawn span, in degrees, anti-clockwise.
     *        Clamped into [-360,360].
     * @throws IllegalArgumentException if any angle is NaN or +-Infinity.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg);
    
    /**
     * Convenience method.
     * Equivalent to drawArc(rect.x(),rect.y(),rect.xSpan(),rect.ySpan(),startDeg,spanDeg).
     * 
     * @param rect Full arc bounding box, in user coordinates.
     * @param startDeg Start angle, in degrees, from positive X axis
     *        and anti-clockwise.
     * @param spanDeg Filled span, in degrees, anti-clockwise.
     *        Clamped into [-360,360].
     * @throws NullPointerException if the specified bounding box is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawArc(GRect rect, double startDeg, double spanDeg);
    
    /**
     * Fills the specified arc, with the current color.
     * The arc can cover pixels outside of the clip,
     * in which case they must not be modified.
     * Must fill nothing if any span is <= 0.
     * 
     * The specified angles are used directly in oval equation,
     * which is, with x and y relative to oval center:
     * x = rx * cos(ang)
     * y = ry * sin(ang)
     * 
     * @param x X of full arc bounding box, in user coordinates.
     * @param y Y of full arc bounding box, in user coordinates.
     * @param xSpan X span of full arc bounding box, in user coordinates.
     * @param ySpan Y span of full arc bounding box, in user coordinates.
     * @param startDeg Start angle, in degrees, from positive X axis
     *        and anti-clockwise.
     * @param spanDeg Filled span, in degrees, anti-clockwise.
     *        Clamped into [-360,360].
     * @throws IllegalArgumentException if any angle is NaN or +-Infinity.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void fillArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg);
    
    /**
     * Convenience method.
     * Equivalent to fillArc(rect.x(),rect.y(),rect.xSpan(),rect.ySpan(),startDeg,spanDeg).
     * 
     * @param rect Full arc bounding box, in user coordinates.
     * @param startDeg Start angle, in degrees, from positive X axis
     *        and anti-clockwise.
     * @param spanDeg Filled span, in degrees, anti-clockwise.
     *        Clamped into [-360,360].
     * @throws NullPointerException if the specified bounding box is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void fillArc(GRect rect, double startDeg, double spanDeg);
    
    /*
     * Text.
     */
    
    /**
     * Draws the specified text at the specified position,
     * using the current font and the current color.
     * 
     * The semantics of this method is voluntarily loose,
     * so as to allow for direct delegation to backing library
     * text drawing treatments, which can behave differently
     * between libraries, in particular with respect to control
     * character or other special code points.
     * For example, in case of a new line character, some libraries
     * will draw a fallback glyph and continue on the same line,
     * while others, such as JavaFX, will continue drawing
     * on "the next line" (such as y += line height and x = 0).
     * Or, in case of a control characters, some libraries could "obey" it,
     * and others just display a fallback glyph.
     * If the user wants the text to be drawn in a certain way,
     * it's always possible to handle control or other special
     * characters at a higher level, and to use this method only
     * to draw "regular" characters.
     * 
     * We still specify that character for which the font has no glyph
     * should not cause an exception, but instead be either ignored
     * or drawn with a fallback glyph, and that encountering the NUL
     * character (code point = 0) should not prematurely end text drawing
     * (if the backing library does prematurely end it, the binding should
     * either draw glyph by glyph, or just draw the text cleaned of its
     * NUL characters).
     * 
     * For characters in script systems such as Hebrew and Arabic,
     * the glyphs can be rendered from right to left,
     * in which case the coordinate supplied should be the
     * location of the leftmost character.
     * 
     * (x,y) coordinates are those of the top-left corner of text's
     * bounding box, not of baseline left.
     * It makes it easier to fit text into boxes, and simplifies things
     * by getting rid of the notion of baseline, in particular for
     * orthographies for which it doesn't make sense.
     * 
     * Only glyphs shapes must be drawn (possibly with anti-aliasing),
     * no background filling must be done (an no such thing as a
     * "background color" is defined anyway).
     * 
     * Whether all or only a subset of Unicode characters are supported
     * depends on both the binding and the font in use.
     * Use font's canDisplay(codePoint) method to know if a character
     * can be displayed for sure.
     * 
     * @param x X position, in user coordinates,
     *          of leftmost character's top-left corner.
     * @param y Y position, in user coordinates,
     *          of leftmost character's top-left corner.
     * @param text The text to draw.
     * @throws NullPointerException if the specified text is null.
     * @throws IllegalStateException if the current font is disposed.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawText(
            int x, int y,
            String text);
    
    /*
     * Images.
     */
    
    /**
     * @param x Destination x, in user coordinates.
     * @param y Destination y, in user coordinates.
     * @param image Image to draw. Must not be null.
     * @throws NullPointerException if the specified image is null.
     * @throws IllegalArgumentException if the specified image is disposed.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawImage(
            int x, int y,
            InterfaceBwdImage image);
    
    /**
     * @param x Destination x, in user coordinates.
     * @param y Destination y, in user coordinates.
     * @param xSpan Destination X span.
     * @param ySpan Destination Y span.
     * @param image Image to draw. Must not be null.
     * @throws NullPointerException if the specified image is null.
     * @throws IllegalArgumentException if the specified image is disposed.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawImage(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image);
    
    /**
     * Convenience method.
     * Equivalent to drawImage(rect.x(),rect.y(),rect.xSpan(),rect.ySpan(),image).
     * 
     * @param rect Destination rectangle. Must not be null.
     * @param image Image to draw. Must not be null.
     * @throws NullPointerException if the specified rectangle or image is null.
     * @throws IllegalArgumentException if the specified image is disposed.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawImage(GRect rect, InterfaceBwdImage image);

    /**
     * If the source rectangle exceeds image rectangle,
     * it must be clipped into it (it's how most libraries work,
     * and it's easier to ensure than to avoid).
     * 
     * @param x Destination x, in user coordinates.
     * @param y Destination y, in user coordinates.
     * @param xSpan Destination X span.
     * @param ySpan Destination Y span.
     * @param image Image to draw. Must not be null.
     * @param sx Source x, in image coordinates.
     * @param sy Source y, in image coordinates.
     * @param sxSpan Source X span.
     * @param sySpan Source Y span.
     * @throws NullPointerException if the specified image is null.
     * @throws IllegalArgumentException if the specified image is disposed.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawImage(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image,
            int sx, int sy, int sxSpan, int sySpan);
    
    /**
     * Convenience method.
     * Equivalent to drawImage(
     *    rect.x(),rect.y(),rect.xSpan(),rect.ySpan(),
     *    image,
     *    sRect.x(),sRect.y(),sRect.xSpan(),sRect.ySpan()).
     * 
     * @param rect Destination rectangle. Must not be null.
     * @param image Image to draw. Must not be null.
     * @param sRect Source rectangle. Must not be null.
     * @throws NullPointerException if the any of the specified rectangles or image is null.
     * @throws IllegalArgumentException if the specified image is disposed.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void drawImage(GRect rect, InterfaceBwdImage image, GRect sRect);
    
    /*
     * Colors rework.
     */
    
    /**
     * This method must modify the colors of the corresponding area,
     * for example to indicate a selection.
     * Typically, it could be implemented with a kind of "XOR mode"
     * (such as: cpt -> 0xFF - cpt), even though such a function transforms
     * grey into grey, so is not always effective.
     * Other functions (such as: cpt -> ((cpt + 0x80) & 0xFF), which works
     * for any color) could be considered instead.
     * Whether calling it again should revert to initial colors (such as
     * with an "XOR mode"), change colors again, or have no effect,
     * is undefined.
     * 
     * With some backing libraries, for example one with an asynchronous
     * rendering pipeline making it possibly unpractical to read drawn pixels
     * (such as JavaFX), and not providing such a feature in its API
     * (such as SWT), this method might be impossible to implement.
     * In that case, this call should just either do nothing, or do some
     * best effort such as painting some pixels in black or white using
     * a pseudo-random or prime number based logic, but it must not throw.
     * That said, hopefully libraries that are complex enough to have an
     * asynchronous rendering pipeline also are rich enough to
     * provide such a feature.
     * 
     * Alpha (opacity) value must be preserved.
     * 
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void flipColors(int x, int y, int xSpan, int ySpan);
    
    /**
     * Convenience method.
     * Equivalent to flipColors(rect.x(),rect.y(),rect.xSpan(),rect.ySpan()).
     * 
     * @param rect Must not be null.
     * @throws NullPointerException if the specified rectangle is null.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public void flipColors(GRect rect);
    
    /*
     * Colors reading.
     */
    
    /**
     * The alpha of the returned color might not be fully opaque
     * in case of alpha-blending without prior clear with a fully
     * opaque color, but that's not an advised practice.
     * 
     * If it's not possible for the binding to retrieve the actual color,
     * 0 (fully transparent color) must be returned.
     * It can happen in case of asynchronous rendering pipeline,
     * without snapshot feature, or with a too slow one that would
     * make things impractical.
     * 
     * Note that you might want to use getArgb32At(...) instead
     * if your binding only supports 32 bits ARGB internally,
     * for that would be as accurate and faster.
     * 
     * @param x X in user coordinates.
     * @param y Y in user coordinates.
     * @return The current 64 bits ARGB at the specified location,
     *         or 0 (fully transparent color) if it can't be computed.
     * @throws IllegalArgumentException if the specified coordinates are out of
     *         the base clip of this graphics.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public long getArgb64At(int x, int y);
    
    /**
     * 32 bits flavor of getArgb64At(...).
     * 
     * @param x X in user coordinates.
     * @param y Y in user coordinates.
     * @return The current 32 bits ARGB at the specified location,
     *         or 0 (fully transparent color) if it can't be computed.
     * @throws IllegalArgumentException if the specified coordinates are out of
     *         the base clip of this graphics.
     * @throws IllegalStateException if init() has not been called
     *         or if finish() has been called.
     */
    public int getArgb32At(int x, int y);
}
