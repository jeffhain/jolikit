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
package net.jolikit.bwd.test.cases.visualbenches;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.HertzHelper;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.NumbersUtils;

/**
 * Mock to play with image scaling, rotation,
 * and max drawing PPS (Paintings Per Second),
 * with full painted rect (when scrolling background)
 * or one painted rect per sprite (when not scrolling background)
 * (left-click to toggle).
 * 
 * Note that PPS (Paintings Per Second) might be bound by configuration,
 * and that returning one painted rect per sprite might make things
 * slower if the binding has a too large overhead per painted rect handling,
 * such as it should be better to always indicate everything as repainted.
 * 
 * TODO sdl On Mac, with SDL, if initial height is too large for the screen,
 * the window is automatically distorted to fit the screen, but programmatically
 * its size remains the same (SDL_GetWindowSize() and SDL_GetWindowSurface()
 * still give initial width and height, so there is no way to tell),
 * until its resized manually after which its height is reduced
 * to fit the screen without distortion.
 */
public abstract class AbstractBenchPacMiceBwdTestCase extends AbstractBwdTestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final String BG_IMAGE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG;
    private static final String BG_ALPHA_IMAGE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_ALPHA_PNG;

    private static final String MOUSE_HEAD_IMAGE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_MOUSE_HEAD_PNG;
    private static final String MOUSE_HEAD_ALPHA_IMAGE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_MOUSE_HEAD_ALPHA_PNG;

    /*
     * Stuffs for when using alpha.
     * 
     * Two BG clear colors, depending on the client part,
     * to make it obvious there is alpha going on and not just a modified BG image.
     */
    
    private static final int CLEAR_BG_RGB32_LEFT = 0x4080C0;
    private static final int CLEAR_BG_RGB32_RIGHT = 0xC08040;
    
    private static final double PAC_MAN_ALPHA_FP = 0.5;
    
    /*
     * 
     */
    
    /**
     * When scroll activated (left-click on client area to toggle).
     */
    private static final int BG_IMAGE_XY_SCROLL_PER_PAINTING = 1;
    
    private static final double BG_IMAGE_SCALE_FACTOR = 0.5;
    
    /*
     * Sprites, to bench dirty painting effect, and have Pac-Man.
     */

    private static final int NBR_OF_MOUSE_HEAD = 10;
    
    private static final int SPRITES_SPANS = 100;
    
    private static final int PAC_MAN_JAW_MAX_SPAN_DEG = 120;
    private static final int PAC_MAN_JAW_SPEED = 2;
    
    private static final BwdColor PAC_MAN_COLOR = BwdColor.YELLOW;
    
    private static final double SPRITE_KINE_ORIENTATION_CHANGE_PROBA = 0.0005;
    
    /*
     * 
     */
    
    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 1000;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    private static final GRect INITIAL_SPRITES_BOX = GRect.valueOf(
            0,
            0,
            INITIAL_WIDTH - SPRITES_SPANS,
            INITIAL_HEIGHT - SPRITES_SPANS);

    /**
     * For dirty painting (when not painting the whole background each time),
     * instead of adding clip for old sprite position, and clip for new sprite
     * position, we just add one of them, 1-pixel-extended in all directions
     * to cover old (with restored BG) and new sprite position.
     */
    private static final boolean HACK_EXTENDED_CLIP_PER_SPRITE = true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MySpriteInfo {
        /**
         * If null, means it's Pac-Man.
         */
        final InterfaceBwdImage image;
        int x;
        int y;
        boolean xIncreasing;
        boolean yIncreasing;
        public MySpriteInfo(
                InterfaceBwdImage image,
                int lastX,
                int lastY,
                boolean xIncreasing,
                boolean yIncreasing) {
            this.image = image;
            this.x = lastX;
            this.y = lastY;
            this.xIncreasing = xIncreasing;
            this.yIncreasing = yIncreasing;
        }
        @Override
        public String toString() {
            return "[" + this.image
                    + "," + this.x
                    + ", " + this.y
                    + "," + this.xIncreasing
                    + ", " + this.yIncreasing
                    + "]";
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final boolean alphaBg;
    private final boolean alphaFg;
    
    private final Random random = new Random(123456789L);
    
    private boolean initCalled = false;
    
    private InterfaceBwdImage bgImage;
    private InterfaceBwdImage mouseHeadImage;
    
    private final ArrayList<MySpriteInfo> spriteInfoList = new ArrayList<MySpriteInfo>();
    
    private final HertzHelper ppsHelper = new HertzHelper();
    
    private boolean mustScrollBackground = true;
    private int xShift = 0;
    private int yShift = 0;
    
    private int paintCounter = 0;
    
    /**
     * With some bindings, such as Allegro5 one, it can take some time
     * for client to reach specified initial spans, or at least the spans
     * it can reach taking screen bounds into account, so we just do nothing
     * until it happens, to avoid having all sprites initially cornered
     * due to tiny initial spans.
     */
    private boolean largeEnoughBoundsReached = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param alphaBg True if must use semi transparent colors for background.
     * @param alphaFg True if must use semi transparent colors for sprites.
     */
    public AbstractBenchPacMiceBwdTestCase(
            boolean alphaBg,
            boolean alphaFg) {
        this.alphaBg = alphaBg;
        this.alphaFg = alphaFg;
    }

    public AbstractBenchPacMiceBwdTestCase(
            InterfaceBwdBinding binding,
            boolean alphaBg,
            boolean alphaFg) {
        super(binding);
        this.alphaBg = alphaBg;
        this.alphaFg = alphaFg;
    }
    
    /*
     * 
     */
    
    @Override
    public boolean getMustSequenceLaunches() {
        return true;
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    /*
     * 
     */
    
    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);
        
        if (event.getButton() == BwdMouseButtons.PRIMARY) {
            this.mustScrollBackground = !this.mustScrollBackground;
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final ArrayList<GRect> paintedRectList = new ArrayList<GRect>();
        
        final GRect box = g.getBox();

        final boolean allIsDirty = dirtyRect.contains(box);
        
        final boolean mustPaintAll =
                allIsDirty
                || this.mustScrollBackground;
        if (mustPaintAll) {
            paintedRectList.add(box);
        } else {
            // Will only add sprites rects.
        }

        if (!this.wereLargeEnoughBoundsReached(box)) {
            if (DEBUG) {
                Dbg.log("large enough bounds not reached: not drawing");
            }
            return paintedRectList;
        }

        final int paintCount = ++this.paintCounter;
                
        this.ppsHelper.onEvent();
        final int pps = this.ppsHelper.getFrequencyHzRounded();

        this.initIfNeeded();
        
        /*
         * 
         */
        
        final GTransform oldTransform = g.getTransform();
        {
            this.drawBackground_beforeSpritesKineUpdate(
                    g,
                    mustPaintAll,
                    paintedRectList);
            
            updateSpritesKinematics(
                    this.random,
                    g,
                    this.spriteInfoList);
            
            this.drawBackground_afterSpritesKineUpdate(
                    g,
                    mustPaintAll,
                    paintedRectList);
        }
        g.setTransform(oldTransform);
        
        /*
         * 
         */
        
        drawSprites(
                this.random,
                g,
                paintCount,
                this.spriteInfoList,
                mustPaintAll,
                paintedRectList);
        
        /*
         * 
         */
        
        drawPps(
                g,
                pps,
                mustPaintAll,
                paintedRectList);
        
        /*
         * Scheduling next painting.
         */
        
        this.getHost().ensurePendingClientPainting();
        
        return paintedRectList;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param box Client box.
     */
    private boolean wereLargeEnoughBoundsReached(GRect box) {
        if (!this.largeEnoughBoundsReached) {
            final GRect screenBounds = getBinding().getScreenBounds();
            final GRect windowBounds = getHost().getWindowBounds();
            
            final boolean isXSpanOk =
                    ((box.xSpan() >= INITIAL_CLIENT_SPANS.x())
                            || (windowBounds.xSpan() >= screenBounds.xSpan() / 2));

            final boolean isYSpanOk =
                    ((box.ySpan() >= INITIAL_CLIENT_SPANS.y())
                            || (windowBounds.ySpan() >= screenBounds.ySpan() / 2));

            this.largeEnoughBoundsReached = (isXSpanOk && isYSpanOk);
        }
        
        return this.largeEnoughBoundsReached;
    }
    
    /*
     * 
     */
    
    private static void clearBackground(
            InterfaceBwdGraphics g,
            GRect clientBox,
            GRect clearRect) {
        final int clientLeftSpan = clientBox.xSpan() / 2;
        final int clientRightSpan = clientBox.xSpan() - clientLeftSpan;
        
        final GRect clientLeft = clientBox.withSpans(clientLeftSpan, clientBox.ySpan());
        final GRect clientRight = clientBox.withPos(clientBox.x() + clientLeftSpan, clientBox.y()).withSpans(clientRightSpan, clientBox.ySpan());
        
        final GRect clearRectLeft = clearRect.intersected(clientLeft);
        if (!clearRectLeft.isEmpty()) {
            g.setArgb32(CLEAR_BG_RGB32_LEFT);
            g.clearRect(clearRectLeft);
        }
        
        final GRect clearRectRight = clearRect.intersected(clientRight);
        if (!clearRectRight.isEmpty()) {
            g.setArgb32(CLEAR_BG_RGB32_RIGHT);
            g.clearRect(clearRectRight);
        }
    }
    
    private void drawBackground_beforeSpritesKineUpdate(
            InterfaceBwdGraphics g,
            boolean mustPaintAll,
            List<GRect> paintedRectList) {
        
        final GRect box = g.getBox();

        final InterfaceBwdImage bgImage = this.bgImage;
        final int srcImageWidth = bgImage.getWidth();
        final int srcImageHeight = bgImage.getHeight();
        final int dstImageWidth = (int) (srcImageWidth * BG_IMAGE_SCALE_FACTOR);
        final int dstImageHeight = (int) (srcImageHeight * BG_IMAGE_SCALE_FACTOR);
        if (this.mustScrollBackground) {
            this.xShift = (this.xShift + BG_IMAGE_XY_SCROLL_PER_PAINTING) % dstImageWidth;
            this.yShift = (this.yShift + BG_IMAGE_XY_SCROLL_PER_PAINTING) % dstImageHeight;
        }
        
        int tmpImageY = (box.y() - dstImageHeight + 1) + this.yShift;
        while (tmpImageY <= box.yMax()) {
            int tmpImageX = (box.x() - dstImageWidth + 1) + this.xShift;
            while (tmpImageX <= box.xMax()) {
                final GRect dstImageRect = GRect.valueOf(
                        tmpImageX, tmpImageY,
                        dstImageWidth, dstImageHeight);
                if (mustPaintAll) {
                    /*
                     * Always drawing the whole image.
                     */
                    if (this.alphaBg) {
                        clearBackground(g, box, dstImageRect);
                    }
                    g.drawImage(dstImageRect, this.bgImage);
                } else {
                    /*
                     * Only drawing the background image where a sprite was located,
                     * to "erase" previous sprite drawing.
                     * 
                     * NB: Could be even more lazy and just draw background where
                     * sprites are and won't be after current painting.
                     */
                    for (MySpriteInfo info : this.spriteInfoList) {
                        final GRect spriteRect = GRect.valueOf(
                                info.x,
                                info.y,
                                SPRITES_SPANS,
                                SPRITES_SPANS);
                        final GRect spriteOverImageRect = dstImageRect.intersected(spriteRect);
                        if (!spriteOverImageRect.isEmpty()) {
                            if (this.alphaBg) {
                                clearBackground(g, box, spriteOverImageRect);
                            }
                            final GRect sRect = GRect.valueOf(
                                    (int) ((spriteOverImageRect.x() - dstImageRect.x()) * (1.0/BG_IMAGE_SCALE_FACTOR)),
                                    (int) ((spriteOverImageRect.y() - dstImageRect.y()) * (1.0/BG_IMAGE_SCALE_FACTOR)),
                                    (int) (spriteOverImageRect.xSpan() * (1.0/BG_IMAGE_SCALE_FACTOR)),
                                    (int) (spriteOverImageRect.ySpan() * (1.0/BG_IMAGE_SCALE_FACTOR)));
                            g.drawImage(
                                    spriteOverImageRect,
                                    this.bgImage,
                                    sRect);

                            if (HACK_EXTENDED_CLIP_PER_SPRITE) {
                                // No need.
                            } else {
                                paintedRectList.add(spriteOverImageRect);
                            }
                        }
                    }
                }
                tmpImageX += dstImageWidth;
            }
            tmpImageY += dstImageHeight;
        }
    }

    private void drawBackground_afterSpritesKineUpdate(
            InterfaceBwdGraphics g,
            boolean mustPaintAll,
            List<GRect> paintedRectList) {
        if (mustPaintAll) {
            // All background already drawn.
            return;
        }
        
        if (!this.alphaFg) {
            // No need to prepare sprite background.
            return;
        }
        
        /*
         * Drawing the background where a sprite is now located,
         * to prepare background for new one.
         */

        final GRect box = g.getBox();

        final InterfaceBwdImage bgImage = this.bgImage;
        final int srcImageWidth = bgImage.getWidth();
        final int srcImageHeight = bgImage.getHeight();
        final int dstImageWidth = (int) (srcImageWidth * BG_IMAGE_SCALE_FACTOR);
        final int dstImageHeight = (int) (srcImageHeight * BG_IMAGE_SCALE_FACTOR);
        
        int tmpImageY = (box.y() - dstImageHeight + 1) + this.yShift;
        while (tmpImageY <= box.yMax()) {
            int tmpImageX = (box.x() - dstImageWidth + 1) + this.xShift;
            while (tmpImageX <= box.xMax()) {
                final GRect dstImageRect = GRect.valueOf(
                        tmpImageX, tmpImageY,
                        dstImageWidth, dstImageHeight);
                for (MySpriteInfo info : this.spriteInfoList) {
                    final GRect spriteRect = GRect.valueOf(
                            info.x,
                            info.y,
                            SPRITES_SPANS,
                            SPRITES_SPANS);
                    final GRect spriteOverImageRect = dstImageRect.intersected(spriteRect);
                    if (!spriteOverImageRect.isEmpty()) {
                        if (this.alphaBg) {
                            clearBackground(g, box, spriteOverImageRect);
                        }
                        final GRect sRect = GRect.valueOf(
                                (int) ((spriteOverImageRect.x() - dstImageRect.x()) * (1.0/BG_IMAGE_SCALE_FACTOR)),
                                (int) ((spriteOverImageRect.y() - dstImageRect.y()) * (1.0/BG_IMAGE_SCALE_FACTOR)),
                                (int) (spriteOverImageRect.xSpan() * (1.0/BG_IMAGE_SCALE_FACTOR)),
                                (int) (spriteOverImageRect.ySpan() * (1.0/BG_IMAGE_SCALE_FACTOR)));
                        g.drawImage(
                                spriteOverImageRect,
                                this.bgImage,
                                sRect);
                    }
                }
                tmpImageX += dstImageWidth;
            }
            tmpImageY += dstImageHeight;
        }
    }

    private static void updateSpritesKinematics(
            Random random,
            InterfaceBwdGraphics g,
            List<MySpriteInfo> spriteInfoList) {
        
        final GRect box = g.getBox();

        // Spans of at least 2, to allow for moves.
        final GRect spritesMovingBox = GRect.valueOf(
                0,
                0,
                Math.max(2, box.xSpan() - SPRITES_SPANS),
                Math.max(2, box.ySpan() - SPRITES_SPANS));

        for (MySpriteInfo info : spriteInfoList) {
            updateSpriteKinematics(
                    random,
                    spritesMovingBox,
                    info);
        }
    }

    private void drawSprites(
            Random random,
            InterfaceBwdGraphics g,
            int paintCount,
            List<MySpriteInfo> spriteInfoList,
            boolean mustPaintAll,
            List<GRect> paintedRectList) {
        
        for (MySpriteInfo info : spriteInfoList) {
            final InterfaceBwdImage image = info.image;
            if (image != null) {
                g.drawImage(info.x, info.y, image);
            } else {
                drawPacMan(g, info, paintCount);
            }

            if (mustPaintAll) {
                // Already added box.
            } else {
                if (HACK_EXTENDED_CLIP_PER_SPRITE) {
                    final GRect spriteClip = GRect.valueOf(info.x - 1, info.y - 1, SPRITES_SPANS + 2, SPRITES_SPANS + 2);
                    paintedRectList.add(spriteClip);
                } else {
                    final GRect spriteClip = GRect.valueOf(info.x, info.y, SPRITES_SPANS, SPRITES_SPANS);
                    paintedRectList.add(spriteClip);
                }
            }
        }
    }
    
    private static void updateSpriteKinematics(
            Random random,
            GRect spritesMovingBox,
            MySpriteInfo info) {

        // Back into the box, in case of resize.
        info.x = NumbersUtils.toRange(spritesMovingBox.x(), spritesMovingBox.xMax(), info.x);
        info.y = NumbersUtils.toRange(spritesMovingBox.y(), spritesMovingBox.yMax(), info.y);
        
        // Seldom random orientation change.
        if (random.nextDouble() < SPRITE_KINE_ORIENTATION_CHANGE_PROBA) {
            // Making sure we change at least one for sure.
            if (random.nextBoolean()) {
                info.xIncreasing = !info.xIncreasing;
                if (random.nextBoolean()) {
                    info.yIncreasing = !info.yIncreasing;
                }
            } else {
                info.yIncreasing = !info.yIncreasing;
                if (random.nextBoolean()) {
                    info.xIncreasing = !info.xIncreasing;
                }
            }
        }
        
        // Eventually bouncing.
        if (info.xIncreasing) {
            if (info.x == spritesMovingBox.xMax()) {
                info.xIncreasing = false;
            }
        } else {
            if (info.x == spritesMovingBox.x()) {
                info.xIncreasing = true;
            }
        }
        if (info.yIncreasing) {
            if (info.y == spritesMovingBox.yMax()) {
                info.yIncreasing = false;
            }
        } else {
            if (info.y == spritesMovingBox.y()) {
                info.yIncreasing = true;
            }
        }

        info.x += (info.xIncreasing ? 1 : -1);
        info.y += (info.yIncreasing ? 1 : -1);
    }
    
    private void drawPacMan(
            InterfaceBwdGraphics g,
            MySpriteInfo info,
            int paintCount) {
        
        if (this.alphaFg) {
            g.setColor(PAC_MAN_COLOR.withAlphaFp(PAC_MAN_ALPHA_FP));
        } else {
            g.setColor(PAC_MAN_COLOR);
        }
        
        final int jawOpening = (paintCount * PAC_MAN_JAW_SPEED);
        
        // Numberic hack to get bouncing jaws.
        final int joMod1 = (jawOpening % PAC_MAN_JAW_MAX_SPAN_DEG);
        final int joMod2 = (jawOpening % (2 * PAC_MAN_JAW_MAX_SPAN_DEG));
        final double spanDeg;
        if (joMod1 == joMod2) {
            spanDeg = 360 - (joMod1);
        } else {
            spanDeg = 360 - (PAC_MAN_JAW_MAX_SPAN_DEG - joMod1);
        }
        
        final double orientationDeg;
        if (info.xIncreasing) {
            if (info.yIncreasing) {
                orientationDeg = 135;
            } else {
                orientationDeg = 225;
            }
        } else {
            if (info.yIncreasing) {
                orientationDeg = 45;
            } else {
                orientationDeg = 315;
            }
        }
        
        final double startDeg = orientationDeg - spanDeg * 0.5;
        
        g.fillArc(
                info.x, info.y,
                SPRITES_SPANS, SPRITES_SPANS,
                startDeg, spanDeg);
    }
    
    private static void drawPps(
            InterfaceBwdGraphics g,
            int pps,
            boolean mustScrollBackground,
            List<GRect> paintedRectList) {
        
        final GRect box = g.getBox();

        final String ppsHead = "Paints Per Second = ";
        // To avoid digits of obsolete values to be visible.
        final String textForWidth = ppsHead + "123456789";
        final String text = ppsHead + pps;

        final int textX = box.x();
        final int textY = box.y();
        
        final InterfaceBwdFont font = g.getFont();
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        final int textHeight = metrics.fontHeight();
        final int textWidth = metrics.computeTextWidth(textForWidth);
        
        BwdTestUtils.drawTextAndSpannedBg(
                g, BwdColor.WHITE, BwdColor.BLACK,
                textX, textY, text, textWidth, textHeight);

        if (mustScrollBackground) {
            // Already added box.
        } else {
            final GRect textClip = GRect.valueOf(textX, textY, textWidth, textHeight);
            paintedRectList.add(textClip);
        }
    }
    
    /*
     * 
     */
    
    private void initIfNeeded() {
        if (this.initCalled) {
            return;
        }
        this.initCalled = true;
        this.init();
    }
    
    private void init() {

        final InterfaceBwdBinding binding = this.getBinding();
        
        if (this.alphaBg) {
            this.bgImage = binding.newImage(BG_ALPHA_IMAGE_PATH);
        } else {
            this.bgImage = binding.newImage(BG_IMAGE_PATH);
        }
        
        if (this.alphaFg) {
            this.mouseHeadImage = binding.newImage(MOUSE_HEAD_ALPHA_IMAGE_PATH);
        } else {
            this.mouseHeadImage = binding.newImage(MOUSE_HEAD_IMAGE_PATH);
        }
        
        /*
         * 
         */

        final InterfaceBwdImage image = this.mouseHeadImage;

        for (int i = 0; i < NBR_OF_MOUSE_HEAD; i++) {
            final int initialX = this.random.nextInt(INITIAL_SPRITES_BOX.xSpan());
            final int initialY = this.random.nextInt(INITIAL_SPRITES_BOX.ySpan());
            final boolean xIncreasing = this.random.nextBoolean();
            final boolean yIncreasing = this.random.nextBoolean();
            final MySpriteInfo spriteInfo = new MySpriteInfo(
                    image,
                    initialX,
                    initialY,
                    xIncreasing,
                    yIncreasing);
            this.spriteInfoList.add(spriteInfo);
        }

        // Adding Pac-Man last, so that it's never hidden.
        final int initialX = this.random.nextInt(INITIAL_SPRITES_BOX.xSpan());
        final int initialY = this.random.nextInt(INITIAL_SPRITES_BOX.ySpan());
        final boolean xIncreasing = this.random.nextBoolean();
        final boolean yIncreasing = this.random.nextBoolean();
        final MySpriteInfo spriteInfo = new MySpriteInfo(
                null, // null means Pac-Man.
                initialX,
                initialY,
                xIncreasing,
                yIncreasing);
        this.spriteInfoList.add(spriteInfo);
    }
}
