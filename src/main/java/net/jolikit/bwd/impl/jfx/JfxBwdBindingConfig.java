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
package net.jolikit.bwd.impl.jfx;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.lang.OsUtils;

public class JfxBwdBindingConfig extends BaseBwdBindingConfig {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * TODO jfx On Windows, Stage.getWidth()/getHeight()
     * and Pane.getWidth()/getHeight() might be inconsistent
     * (in particular if setting backing bounds while hiding
     * or iconified, but not only), so we can't reliably compute
     * insets from them, nor consistent client and window bounds.
     */
    private GRect decorationInsets;

    /**
     * TODO jfx The value below which stage.getWidth() can go, but not the
     * actual decorated window as visible on screen, causing the pane to
     * occupy only a part of the visible client area.
     * Looks like it must be (100 + 3 * decoration margin span).
     */
    private int minWindowWidthIfDecorated = 112;

    /**
     * TODO jfx Without a large enough value here, at some point had crazy
     * resize flickering frenzy when trying to drag the top too far down,
     * due to mouse events coordinates being (0,0) for empty canvas, else
     * could just be (top + bottom) (possibly related to why zero-span
     * undecorated windows paints blank symmetrically to bottom-right corner).
     * Looks like it must be (top + bottom + 1).
     */
    private int minWindowHeightIfDecorated = 28;

    /**
     * TODO jfx On Mac, after iconification of a maximized window,
     * and then deiconification of it, the window properly has
     * its maximized bounds, but JavaFX indicates the window
     * as demaximized, and when calling maximize() on it
     * (as we do to restore maximized state), its state becomes
     * maximized but... it shrinks to non-maximized bounds!
     * Also, at some points, some test windows ended up quite large
     * but with (x,y) nowhere near top-left corner of screen,
     * as if their spans had been restored to maximized bounds
     * but not their position.
     * 
     * After having unsuccessfully tried a few necessarily ugly workarounds,
     * we want to consider that Stage.setMaximized(boolean) just
     * doesn't work.
     * Note that corresponding issues might still arise due to
     * maximizations from native decoration, but whoever uses native
     * hence non-binding-correctable stuffs directly should know the risk.
     */
    private boolean mustNotImplementMaxDemax = OsUtils.isMac();

    /**
     * TODO jfx With JavaFX public API, font metrics computations are
     * hacky and slow as hell, and we have to arbitrarily invent a baseline,
     * so it's better to use the internal API instead.
     */
    private boolean mustUseInternalApiForFontMetrics = true;

    /**
     * We could use in array graphics here, but it's already configured
     * for writable images, so using GraphicsContext based graphics
     * here allows for more versatility, and faster text drawing
     * on client (which should be useful, in particular for text editors,
     * which usually draw directly on client to minimize latency).
     * To use the best of both graphics, some background could also
     * be computed on a writable image using an int array graphics,
     * the image drawn on client, and then text be drawn on top of that.
     */
    private boolean mustUseIntArrayGraphicsForClients = false;
    
    /**
     * Needed to allow for transparency (initial, and with g.clearRect(...)).
     * Some tests will fail if using GraphicsContext based graphics
     * here instead.
     * Also a good idea to have a graphics with very fast primitives
     * for offscreen drawings, especially with drawPoint(),
     * for example if wanting to draw some fractals.
     */
    private boolean mustUseIntArrayGraphicsForWritableImages = true;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses default values for parameters not in arguments.
     * 
     * @param decorationInsets Rectangle containing (left,top,right,bottom) spans
     *        of border when window is decorated.
     */
    public JfxBwdBindingConfig(GRect decorationInsets) {
        this.decorationInsets = decorationInsets;
        
        /*
         * TODO jfx JavaFX is heavily asynchronous, down to splitting things that
         * ought to be kept consistent, such as bounds, into separate properties,
         * such as observing inconsistent states is easy (x modified but not yet
         * width, etc.).
         * It might causes issues with dragging, depending on how it's implemented,
         * such as window warping around.
         * But, now it seems we don't need it anymore.
         */
        this.setMustFixBoundsDuringDrag_final(false);
        
        /*
         * TODO jfx On Windows, deiconification can damage window position
         * (maybe only for dialogs?).
         * Ex.: "H5D2U7 : expected client bounds [880, 778, 150, 50], but got = [0, 1372, 150, 50]"
         */
        this.setMustRestoreBoundsOnShowDeicoDemax_final(OsUtils.isWindows());
    }
    
    /*
     * 
     */
    
    /**
     * @return Rectangle containing (left,top,right,bottom) spans
     *         of border when window is decorated.
     */
    public GRect getDecorationInsets() {
        return this.decorationInsets;
    }

    /**
     * @param decorationInsets Rectangle containing (left,top,right,bottom) spans
     *        of border when window is decorated.
     */
    public void setDecorationInsets(GRect decorationInsets) {
        this.decorationInsets = decorationInsets;
    }

    public int getMinWindowWidthIfDecorated() {
        return this.minWindowWidthIfDecorated;
    }

    public void setMinWindowWidthIfDecorated(int minWindowWidthIfDecorated) {
        this.minWindowWidthIfDecorated = minWindowWidthIfDecorated;
    }

    public int getMinWindowHeightIfDecorated() {
        return this.minWindowHeightIfDecorated;
    }

    public void setMinWindowHeightIfDecorated(int minWindowHeightIfDecorated) {
        this.minWindowHeightIfDecorated = minWindowHeightIfDecorated;
    }

    public boolean getMustNotImplementMaxDemax() {
        return this.mustNotImplementMaxDemax;
    }

    public void setMustNotImplementMaxDemax(boolean mustNotImplementMaxDemax) {
        this.mustNotImplementMaxDemax = mustNotImplementMaxDemax;
    }
    
    public boolean getMustUseInternalApiForFontMetrics() {
        return this.mustUseInternalApiForFontMetrics;
    }

    public void setMustUseInternalApiForFontMetrics(boolean mustUseInternalApiForFontMetrics) {
        this.mustUseInternalApiForFontMetrics = mustUseInternalApiForFontMetrics;
    }

    public boolean getMustUseIntArrayGraphicsForClients() {
        return this.mustUseIntArrayGraphicsForClients;
    }

    public void setMustUseIntArrayGraphicsForClients(boolean mustUseIntArrayGraphicsForClients) {
        this.mustUseIntArrayGraphicsForClients = mustUseIntArrayGraphicsForClients;
    }

    public boolean getMustUseIntArrayGraphicsForWritableImages() {
        return this.mustUseIntArrayGraphicsForWritableImages;
    }

    public void setMustUseIntArrayGraphicsForWritableImages(boolean mustUseIntArrayGraphicsForWritableImages) {
        this.mustUseIntArrayGraphicsForWritableImages = mustUseIntArrayGraphicsForWritableImages;
    }
}
