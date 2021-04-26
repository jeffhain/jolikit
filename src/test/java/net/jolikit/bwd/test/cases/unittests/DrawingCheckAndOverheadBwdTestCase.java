/*
 * Copyright 2020-2021 Jeff Hain
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
package net.jolikit.bwd.test.cases.unittests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.bwd.test.cases.utils.AbstractUnitTestBwdTestCase;
import net.jolikit.bwd.test.cases.utils.BenchTimeoutManager;
import net.jolikit.bwd.test.utils.BwdClientMock;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.time.TimeUtils;

/**
 * Checks (written and read colors correctness, not figures correctness)
 * and overhead benches for drawing and colors reading methods,
 * for client graphics, image graphics, and images specific
 * (not in graphics API) methods.
 * 
 * Figures spans are small, to avoid benching per-pixel overhead
 * or drawing algorithms.
 * 
 * Doing both checks and benches, to make sure benched code is not trash,
 * and to factor test code.
 * 
 * These tests are helpful to choose between various backing library usages,
 * for even seemingly simple operations can have a tremendous overhead,
 * like drawing a pixel on an AWT BufferedImage with Graphics.drawLine(),
 * which SunGraphics2D implementation goes through a lot of code and is
 * orders of magnitude slower than could be expected.
 * 
 * NB: These benches can be biased for asynchronous libraries,
 * for not taking into account the rendering done in another thread.
 */
public class DrawingCheckAndOverheadBwdTestCase extends AbstractUnitTestBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * Normally 0.
     * Handy to start at a specific test.
     */
    private static final int FIRST_TARGET_TEST_INDEX = 0;
    
    private static final int NBR_OF_MEASURES = 1;

    /**
     * Not more else can cause much piling-up
     * in asynchronous libraries.
     */
    private static final int MAX_NBR_OF_CALLS = 1000 * 1000;
    private static final double MAX_MEASURE_DURATION_S = 0.2;

    /**
     * Some rest time, else with some bindings no refresh is done until end.
     */
    private static final double REST_TIME_BETWEEN_BENCHES_S = 0.1;

    /**
     * On Windows, even when configured OS resolution remains the same as
     * screen physical resolution, using "Make text and other items larger"
     * to use larger icons and fonts causes some libraries (such as JavaFX)
     * to use the related "modified resolution" (which we have no way to know),
     * causing scaling and (additional) anti-aliasing in drawings,
     * with unexpected pixels being lit with unexpected colors.
     */
    private static final boolean MUST_BE_ROBUST_TO_WINDOWS_SCALING = true;
    
    /*
     * 
     */

    private static final int TEST_GRAPHICS_WIDTH = 100;
    private static final int TEST_GRAPHICS_HEIGHT = 100;

    /**
     * Alpha/components:
     * - Low enough to allow not to reach opaque/black too fast
     *   when doing alpha blending benches.
     * - High enough to be able to check usage of initial color
     *   when doing alpha blending.
     */
    private static final int CLEAR_ARGB_32_WALPHA = 0x0204080A;
    private static final int CLEAR_ARGB_32_OPAQUE = Argb32.toOpaque(CLEAR_ARGB_32_WALPHA);

    /**
     * Walpha = with alpha != Wolfram Alpha.
     * 
     * Alpha and components such as conversion between
     * alpha-premultiplied and non alpha-premultiplied
     * are stable, to minimize error accumulation.
     */
    private static final int ARGB_32_COLOR_WALPHA = 0x082040FF;
    static {
        final int initial = ARGB_32_COLOR_WALPHA;
        int tmp = initial;
        tmp = BindingColorUtils.toPremulAxyz32(tmp);
        tmp = BindingColorUtils.toNonPremulAxyz32(tmp);
        checkArgb32(initial, tmp);
    }

    private static final int ARGB_32_COLOR_OPAQUE = Argb32.toOpaque(ARGB_32_COLOR_WALPHA);
    
    /**
     * Number of alpha blending of drawing color, over initial background,
     * after which either a fully opaque color, or a stable non-opaque color
     * (can happen due to alpha premultiplication approximations),
     * is reached, after which we re-clear the background to avoid
     * special cases when blending.
     * NB1: For drawing images from files, we use the same threshold,
     *      which should still help avoiding reaching opacity too often.
     * NB2: With our low-alpha colors, we actually reach stability
     *      before opacity, at least with our ways to do alpha blending.
     */
    private static final int BENCH_BOX_SCAN_COUNT_BEFORE_RESET;
    static {
        int scanCount = 1;
        
        int c = CLEAR_ARGB_32_WALPHA;
        int prevC = c - 1;
        while (true) {
            c = blendArgb32_srcOver(ARGB_32_COLOR_WALPHA, c);
            if (c == prevC) {
                // Stability reached.
                break;
            }
            prevC = c;
            scanCount++;
        }
        if (scanCount < 10) {
            // Reset would add too much overhead.
            throw new AssertionError("scan count too low : " + scanCount);
        }
        BENCH_BOX_SCAN_COUNT_BEFORE_RESET = scanCount;
    }

    /*
     * 
     */
    
    /**
     * Such as center pixels are likely to be painted.
     */
    private static final String TEXT_TO_DRAW = "W";

    /**
     * Enough for default font.
     */
    private static final int TEXT_PIXEL_READ_RADIUS = 5;

    /*
     * Only testing images loadable by all our bindings.
     * Using PNG format for opaque and alpha tests,
     * and also 4 bits BMP format to check and bench eventual
     * different code that bindings would use for that.
     */

    private static final String IMAGE_OPAQUE_PNG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PNG;
    private static final String IMAGE_OPAQUE_BMP4_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_BMP_04_BIT;
    private static final String IMAGE_WALPHA_PNG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_ALPHA_PNG;

    /*
     * Same dimensions than our image from file.
     */

    private static final int SRC_WI_WIDTH = 41;
    private static final int SRC_WI_HEIGHT = 21;

    /*
     * Not only testing image center pixel,
     * Tested pixel coordinates: not of a grey pixel,
     * i.e. such as (r,g,b) of tested pixel are different,
     * to check that they are properly read.
     */

    private static final int IMAGE_TESTED_PIXEL_X = 39;
    private static final int IMAGE_TESTED_PIXEL_Y = 10;

    private static final int IMAGE_FROM_FILE_TESTED_PIXEL_ARGB32_OPAQUE = 0xFFFF8028;
    private static final int IMAGE_FROM_FILE_TESTED_PIXEL_ARGB32_WALPHA = 0x80FF8028;

    private static final int IMAGE_FROM_FILE_TESTED_PIXEL_ARGB32_BMP4 = 0xFFFF0000;

    /*
     * 
     */

    private static final GRect ADDED_CLIP = GRect.valueOf(0, 0, 10, 20);
    private static final GTransform ADDED_TRANSFORM = GTransform.valueOf(0, 3, 4);

    /*
     * 
     */

    private static boolean isDrawnColorBad(
            InterfaceBwdBinding binding,
            MyMethodType methodType,
            MyImageType srcImageType,
            MyGraphicsType graphicsType,
            boolean withAlpha) {
        
        final boolean ret;
        if (MUST_BE_ROBUST_TO_WINDOWS_SCALING
                && BwdTestUtils.isJfxBinding(binding)) {
            if (methodType == MyMethodType.DRAW_LINE) {
                /*
                 * ERROR : CLI_GRAPHICS.DRAW_LINE (opaque)
                 * ERROR : 0xFF1C38DF too different from 0xFF2040FF
                 * ERROR : CLI_GRAPHICS.DRAW_LINE (alpha)
                 * ERROR : 0xFF050910 too different from 0xFF050A12
                 */
                ret = (graphicsType == MyGraphicsType.CLI_GRAPHICS);
            } else if (methodType == MyMethodType.DRAW_IMAGE) {
                /*
                 * ERROR : CLI_GRAPHICS.DRAW_IMAGE(IFF_PNG) (opaque)
                 * ERROR : 0xFFEF7F32 too different from 0xFFFF8028
                 * ERROR : CLI_GRAPHICS.DRAW_IMAGE(IFF_PNG) (alpha)
                 * ERROR : 0xFF7A431E too different from 0xFF824419
                 * ERROR : CLI_GRAPHICS.DRAW_IMAGE(IFF_BMP4) (opaque)
                 * ERROR : 0xFFEF1010 too different from 0xFFFF0000
                 * 
                 * ERROR : CLI_GRAPHICS.DRAW_IMAGE(WI) (opaque)
                 * ERROR : 0xFF142794 too different from 0xFF2040FF
                 * ERROR : CLI_GRAPHICS.DRAW_IMAGE(WI) (alpha)
                 * ERROR : 0xFF623D26 too different from 0xFF050A12
                 */
                ret = (graphicsType == MyGraphicsType.CLI_GRAPHICS);
            } else {
                ret = false;
            }
            
        } else if (MUST_BE_ROBUST_TO_WINDOWS_SCALING
                && BwdTestUtils.isSwtBinding(binding)) {
            /*
             * ERROR : CLI_GRAPHICS.DRAW_TEXT (opaque)
             * ERROR : 0xFF1E3DF3 too different from 0xFF2040FF
             * ERROR : WI_GRAPHICS.DRAW_TEXT (opaque)
             * ERROR : 0xF31F40FF too different from 0xFF2040FF
             */
            ret = (methodType == MyMethodType.DRAW_TEXT)
                    && (!withAlpha);
            
        } else if (BwdTestUtils.isQtjBinding(binding)) {
            /*
             * ERROR : CLI_GRAPHICS.DRAW_TEXT (opaque)
             * ERROR : 0xFF2041DB too different from 0xFF2040FF
             * ERROR : WI_GRAPHICS.DRAW_TEXT (opaque)
             * ERROR : 0xF82041FF too different from 0xFF2040FF
             */
            ret = (methodType == MyMethodType.DRAW_TEXT)
                    && (!withAlpha);
            
        } else if (BwdTestUtils.isSdlBinding(binding)) {
            /*
             * ERROR : CLI_GRAPHICS.DRAW_TEXT (alpha)
             * ERROR : 0xFF091235 too different from 0xFF050A12
             * ERROR : WI_GRAPHICS.DRAW_TEXT (alpha)
             * ERROR : 0x2F213CF4 too different from 0x0A1A33CC
             */
            ret = (methodType == MyMethodType.DRAW_TEXT)
                    && withAlpha;
            
        } else {
            ret = false;
        }
        return ret;
    }

    /**
     * Absolute tolerance on 8 bits component value.
     * Useful for values small in magnitude.
     */
    private static int getBadColorAbsTol(
            InterfaceBwdBinding binding,
            MyMethodType methodType,
            MyImageType srcImageType,
            MyGraphicsType graphicsType,
            boolean withAlpha) {
        final int ret;
        if (BwdTestUtils.isJfxBinding(binding)) {
            if ((methodType == MyMethodType.DRAW_IMAGE)
                    && (srcImageType == MyImageType.WI)) {
                ret = 0x80;
            } else {
                ret = 0x10;
            }
        } else if (BwdTestUtils.isSwtBinding(binding)) {
            ret = 0x01;
        } else if (BwdTestUtils.isQtjBinding(binding)) {
            ret = 0x10;
        } else if (BwdTestUtils.isSdlBinding(binding)) {
            ret = 0x25;
        } else {
            ret = 0;
        }
        return ret;
    }
    
    /**
     * Relative tolerance on 8 bits component value.
     */
    private static double getBadColorRelTol(
            InterfaceBwdBinding binding,
            MyMethodType methodType,
            MyImageType srcImageType,
            MyGraphicsType graphicsType,
            boolean withAlpha) {
        final double ret;
        if (BwdTestUtils.isJfxBinding(binding)) {
            ret = 0.2;
        } else if (BwdTestUtils.isSwtBinding(binding)) {
            ret = 0.1;
        } else if (BwdTestUtils.isQtjBinding(binding)) {
            ret = 0.2;
        } else if (BwdTestUtils.isSdlBinding(binding)) {
            ret = 0.3;
        } else {
            ret = 0.0;
        }
        return ret;
    }
    
    /*
     * 
     */

    private static final List<String> G_TESTS_HEADER =
            Arrays.asList(
                    "Graphics Methods",
                    "(Opaque, CLI)",
                    "(Opaque, WI)",
                    "(Alpha, CLI)",
                    "(Alpha, WI)");
    private static final List<String> I_TESTS_HEADER =
            Arrays.asList(
                    "Image Methods",
                    "(Opaque)",
                    "(Alpha)");
    
    /*
     * 
     */

    private static final int INITIAL_WIDTH = 800 + (NBR_OF_MEASURES * 300);
    private static final int INITIAL_HEIGHT = 500;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private enum MyInstanceKind {
        /**
         * A graphics, either client one or a writable image one.
         */
        GRAPHICS,
        /**
         * An image, either read from file or a writable one.
         */
        IMAGE;
    }

    private enum MyOpType {
        /**
         * Just configuration, no immediate effect on pixels.
         */
        CONFIG,
        /**
         * Sets pixels to current color.
         */
        SET,
        /**
         * Blends pixels using current color.
         */
        BLEND,
        /**
         * Replacing each component8 by 255-component8
         * (or alpha8-component8, if alpha premultiplied),
         * except for alpha which is not modified.
         */
        INVERT,
        /**
         * Reads pixels colors.
         */
        GET;
    }

    private enum MyMethodType {
        CONFIG_AND_RESET(MyInstanceKind.GRAPHICS, MyOpType.CONFIG),
        //
        CLEAR_RECT(MyInstanceKind.GRAPHICS, MyOpType.SET),
        //
        DRAW_POINT(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_LINE(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_RECT(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        FILL_RECT(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_OVAL(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        FILL_OVAL(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_ARC(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        FILL_ARC(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_POLYLINE(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_POLYGON(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        FILL_POLYGON(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        //
        DRAW_TEXT(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        //
        DRAW_IMAGE(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        //
        FLIP_COLORS(MyInstanceKind.GRAPHICS, MyOpType.INVERT),
        //
        G_GET_ARGB_32_AT(MyInstanceKind.GRAPHICS, MyOpType.GET),
        //
        I_GET_ARGB_32_AT(MyInstanceKind.IMAGE, MyOpType.GET);
        final MyInstanceKind instanceKind;
        final MyOpType opType;
        private MyMethodType(
                MyInstanceKind instanceKind,
                MyOpType opType) {
            this.instanceKind = instanceKind;
            this.opType = opType;
        }
    }

    private enum MyGraphicsType {
        CLI_GRAPHICS,
        WI_GRAPHICS;
    }

    private enum MyImageType {
        /**
         * Images From File : PNG (opaque or not).
         */
        IFF_PNG,
        /**
         * Images From File : 4 bits BMP.
         */
        IFF_BMP4,
        /**
         * Writable Image.
         */
        WI;
    }

    private class MyTestable {
        final MyMethodType methodType;
        final MyImageType srcImageType;
        InterfaceBwdGraphics current_testG;
        MyGraphicsType current_graphicsType;
        boolean current_withAlpha;
        /**
         * If instance kind is GRAPHICS, this is the src image
         * for calls to drawImage(...).
         * If instance kind is IMAGE, this is also the test image
         * on which methods calls are done.
         */
        InterfaceBwdImage current_srcImage;
        /**
         * @param methodType Must not be null.
         */
        public MyTestable(MyMethodType methodType) {
            this(methodType, null);
        }
        /**
         * @param methodType Must not be null.
         * @param srcImageType Only useful for DRAW_IMAGE methods.
         */
        public MyTestable(
                MyMethodType methodType,
                MyImageType srcImageType) {
            this.methodType = LangUtils.requireNonNull(methodType);
            if (methodType == MyMethodType.DRAW_IMAGE) {
                LangUtils.requireNonNull(srcImageType);
            }
            this.srcImageType = srcImageType;
        }
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(this.methodType);
            if (this.srcImageType != null) {
                sb.append(" (src = ");
                sb.append(this.srcImageType);
                sb.append(")");
            }
            return sb.toString();
        }
        public MyInstanceKind instanceKind() {
            return this.methodType.instanceKind;
        }
        public MyOpType opType() {
            return this.methodType.opType;
        }
        /**
         * For images drawing, it must be the color of the tested pixel.
         * 
         * @return The ARGB32 used to draw.
         */
        public int getArgb32ToDraw(boolean withAlpha) {
            final int ret;
            if (this.opType() == MyOpType.CONFIG) {
                /*
                 * Not drawing, so irrelevant.
                 */
                ret = 0;
            } else if ((this.methodType == MyMethodType.DRAW_IMAGE)
                    && (this.srcImageType != MyImageType.WI)) {
                ret = getImageFromFileTestedPixelArgb32(
                        this.srcImageType,
                        withAlpha);
            } else {
                ret = (withAlpha ? ARGB_32_COLOR_WALPHA : ARGB_32_COLOR_OPAQUE);
            }
            return ret;
        }
        public int getImageFromFileTestedPixelArgb32(
                MyImageType imageType,
                boolean withAlpha) {
            final int ret;
            if (imageType == MyImageType.IFF_BMP4) {
                ret = IMAGE_FROM_FILE_TESTED_PIXEL_ARGB32_BMP4;
            } else {
                if (withAlpha) {
                    ret = IMAGE_FROM_FILE_TESTED_PIXEL_ARGB32_WALPHA;
                } else {
                    ret = IMAGE_FROM_FILE_TESTED_PIXEL_ARGB32_OPAQUE;
                }
            }
            return ret;
        }
        public void configureTestable(
                InterfaceBwdGraphics testG,
                MyGraphicsType graphicsType,
                boolean withAlpha) {
            this.current_testG = testG;
            this.current_graphicsType = graphicsType;
            this.current_withAlpha = withAlpha;

            // Early and systematic nullification,
            // for throw if used before being set.
            this.current_srcImage = null;

            if (this.instanceKind() == MyInstanceKind.GRAPHICS) {
                LangUtils.requireNonNull(testG);
                LangUtils.requireNonNull(graphicsType);
            } else {
                if (testG != null) {
                    throw new IllegalArgumentException("" + testG);
                }
                if (graphicsType != null) {
                    throw new IllegalArgumentException("" + graphicsType);
                }
            }

            if (this.srcImageType == MyImageType.IFF_PNG) {
                if (withAlpha) {
                    this.current_srcImage = srcImage_alpha_png;
                } else {
                    this.current_srcImage = srcImage_opaque_png;
                }

            } else if (this.srcImageType == MyImageType.IFF_BMP4) {
                this.current_srcImage = srcImage_opaque_bmp4;

            } else if (this.srcImageType == MyImageType.WI) {
                this.current_srcImage = srcImage_wi;
                
            } else {
                // No src image.
            }
        }
        public void resetPixelsForCorrectnessTest() {
            final InterfaceBwdGraphics testG = this.current_testG;
            final int argb32ToDraw = this.getArgb32ToDraw(this.current_withAlpha);
            
            if (this.instanceKind() == MyInstanceKind.GRAPHICS) {
                clearWithClearColor(testG);
                
                if ((this.opType() == MyOpType.GET)
                        || (this.opType() == MyOpType.INVERT)) {
                    final int prevArgb32 = testG.getArgb32();
                    
                    // Setting pixel to get/invert to color to draw
                    // (reseted pixel color is too specific
                    // for check to check much).
                    final int x = this.getCorrectnessTestX();
                    final int y = this.getCorrectnessTestY();
                    testG.setArgb32(argb32ToDraw);
                    testG.clearRect(x, y, 1, 1);
                    
                    testG.setArgb32(prevArgb32);
                }
            }
            
            if (this.srcImageType == MyImageType.WI) {
                /*
                 * for G.DRAW_IMAGE(WI), or WI.GET_ARGB_32_AT().  
                 */
                
                final InterfaceBwdWritableImage srcWi =
                        (InterfaceBwdWritableImage) this.current_srcImage;
                final InterfaceBwdGraphics wig = srcWi.getGraphics();

                final int prevArgb32 = wig.getArgb32();
                
                final int x = IMAGE_TESTED_PIXEL_X;
                final int y = IMAGE_TESTED_PIXEL_Y;
                wig.setArgb32(argb32ToDraw);
                wig.clearRect(x, y, 1, 1);
                
                wig.setArgb32(prevArgb32);
            }
        }
        public void resetPixelsForBench() {
            final InterfaceBwdGraphics testG = this.current_testG;

            if (this.instanceKind() == MyInstanceKind.GRAPHICS) {
                if ((this.opType() == MyOpType.SET)
                        || (this.opType() == MyOpType.BLEND)) {
                    clearWithClearColor(testG);
                } else if ((this.opType() == MyOpType.GET)
                        || (this.opType() == MyOpType.INVERT)) {
                    clearWithAnImage(testG);
                } else {
                    // No need to clear, but clearing anyway.
                    clearWithClearColor(testG);
                }
            }
            
            if (this.srcImageType == MyImageType.WI) {
                /*
                 * for G.DRAW_IMAGE(WI), or WI.GET_ARGB_32_AT().  
                 */
                
                final InterfaceBwdWritableImage srcWi =
                        (InterfaceBwdWritableImage) this.current_srcImage;
                final InterfaceBwdGraphics wig = srcWi.getGraphics();

                clearWithAnImage(wig);
            }
        }
        private void clearWithClearColor(InterfaceBwdGraphics g) {
            final int prevArgb32 = g.getArgb32();
            
            // For client graphics, clearRect() will use opaque
            // flavor of it: no need for us to specify it.
            g.setArgb32(CLEAR_ARGB_32_WALPHA);
            g.clearRect(g.getBox());
            
            g.setArgb32(prevArgb32);
        }
        private void clearWithAnImage(InterfaceBwdGraphics g) {
            /*
             * Setting various pixel values, using images.
             * Taking care to cover the whole box with the image.
             */
            if (this.current_withAlpha) {
                final int prevArgb32 = g.getArgb32();
                
                g.setColor(BwdColor.TRANSPARENT);
                g.clearRect(g.getBox());
                
                g.drawImage(g.getBox(), srcImage_alpha_png);
                
                g.setArgb32(prevArgb32);
            } else {
                g.drawImage(g.getBox(), srcImage_opaque_png);
            }
        }
        /*
         * Not using (0,0) to make sure methods
         * compute (x,y) location properly.
         */
        /**
         * @return Pixel X for correctness test,
         *         in test graphics or test image frame.
         */
        public int getCorrectnessTestX() {
            if (this.instanceKind() == MyInstanceKind.IMAGE) {
                return IMAGE_TESTED_PIXEL_X;
            } else {
                return TEST_GRAPHICS_WIDTH/2 + 11;
            }
        }
        /**
         * @return Pixel Y for correctness test,
         *         in test graphics or test image frame.
         */
        public int getCorrectnessTestY() {
            if (this.instanceKind() == MyInstanceKind.IMAGE) {
                return IMAGE_TESTED_PIXEL_Y;
            } else {
                return TEST_GRAPHICS_HEIGHT/2 + 13;
            }
        }
        public boolean canDoAlpha() {
            return (this.srcImageType != MyImageType.IFF_BMP4);
        }
        /**
         * For color reading operations, this default implementation throws.
         */
        public int getExpectedArgb32AfterCall() {

            final MyInstanceKind instanceKind = this.instanceKind();

            final int expectedArgb32;
            if (instanceKind == MyInstanceKind.GRAPHICS) {
                final int argb32ToDraw = this.getArgb32ToDraw(this.current_withAlpha);

                if (this.opType() == MyOpType.SET) {
                    if (this.current_graphicsType == MyGraphicsType.WI_GRAPHICS) {
                        expectedArgb32 = argb32ToDraw;
                    } else {
                        expectedArgb32 = Argb32.toOpaque(argb32ToDraw);
                    }
                } else if (this.opType() == MyOpType.BLEND) {
                    if (this.current_graphicsType == MyGraphicsType.WI_GRAPHICS) {
                        expectedArgb32 = blendArgb32_srcOver(argb32ToDraw, CLEAR_ARGB_32_WALPHA);
                    } else {
                        expectedArgb32 = blendArgb32_srcOver(argb32ToDraw, CLEAR_ARGB_32_OPAQUE);
                    }
                } else if (this.opType() == MyOpType.INVERT) {
                    final int argb32BeforeInvert;
                    if (this.current_graphicsType == MyGraphicsType.WI_GRAPHICS) {
                        argb32BeforeInvert = argb32ToDraw;
                    } else {
                        argb32BeforeInvert = Argb32.toOpaque(argb32ToDraw);
                    }
                    expectedArgb32 = Argb32.inverted(argb32BeforeInvert);
                } else {
                    throw new IllegalArgumentException("" + this.opType());
                }
            } else if (instanceKind == MyInstanceKind.IMAGE) {
                if (this.methodType == MyMethodType.I_GET_ARGB_32_AT) {
                    if (this.srcImageType == MyImageType.WI) {
                        final int x = this.getCorrectnessTestX();
                        final int y = this.getCorrectnessTestY();
                        final InterfaceBwdWritableImage srcWi = (InterfaceBwdWritableImage) this.current_srcImage;
                        // graphics.get() is our reference for everything,
                        // including for image.get(), except for images
                        // that have no graphics.
                        expectedArgb32 = srcWi.getGraphics().getArgb32At(x, y);
                    } else {
                        expectedArgb32 = getImageFromFileTestedPixelArgb32(
                                this.srcImageType,
                                this.current_withAlpha);
                    }
                } else {
                    throw new IllegalArgumentException("" + this.methodType);
                }
            } else {
                throw new IllegalArgumentException("" + instanceKind);
            }

            return expectedArgb32;
        }
        private String getRunCaseContext() {

            final StringBuilder sb = new StringBuilder();

            if (this.instanceKind() == MyInstanceKind.GRAPHICS) {
                sb.append(this.current_graphicsType);
                sb.append(".");
                sb.append(this.methodType);
                if (this.srcImageType != null) {
                    sb.append("(");
                    sb.append(this.srcImageType);
                    sb.append(")");
                }
            } else {
                sb.append(this.srcImageType);
                sb.append(".");
                sb.append(this.methodType);
            }

            if (this.current_withAlpha) {
                sb.append(" (alpha)");
            } else {
                sb.append(" (opaque)");
            }

            return sb.toString();
        }
        public int readDrawnArgb32At(
                int x,
                int y) {
            final InterfaceBwdGraphics g = this.current_testG;
            if (this.methodType == MyMethodType.DRAW_TEXT) {
                /*
                 * Taking max of nearby pixels, since in our world
                 * it's hard to know where the text is exactly.
                 * Just using Math.max(long,long), since components
                 * should go up or down together.
                 */
                long max = 0;
                final int r = TEXT_PIXEL_READ_RADIUS;
                for (int j = -r; j <= r; j++) {
                    for (int i = -r; i <= r; i++) {
                        final int px = x + i;
                        final int py = y + j;
                        if (g.getInitialClipInUser().contains(px, py)) {
                            final int argb32 = g.getArgb32At(px, py);
                            final long argb32L = (argb32 & 0xFFFFFFFFL);
                            max = Math.max(max, argb32L);
                        }
                    }
                }
                return (int) max;
            } else {
                return g.getArgb32At(x, y);
            }
        }
        /**
         * Only relevant for non-config methods,
         * and also irrelevant for graphics.getArgb32At()
         * which is the usual reference for correctness tests.
         * 
         * @throws Something if something goes wrong.
         */
        public void doCorrectnessTest() {
            
            final InterfaceBwdGraphics testG = this.current_testG;
            
            this.resetPixelsForCorrectnessTest();
            
            if (this.instanceKind() == MyInstanceKind.GRAPHICS) {
                final int argb32ToDraw = this.getArgb32ToDraw(this.current_withAlpha);
                testG.setArgb32(argb32ToDraw);
            }

            final int x = this.getCorrectnessTestX();
            final int y = this.getCorrectnessTestY();

            final int expectedArgb32 = this.getExpectedArgb32AfterCall();

            final int returnedArgb32 = this.callMethodForPixelAt(
                    testG,
                    x,
                    y);

            final int actualArgb32;
            if (this.opType() == MyOpType.GET) {
                actualArgb32 = returnedArgb32;
            } else {
                actualArgb32 = this.readDrawnArgb32At(x, y);
            }

            final InterfaceBwdBinding binding = getBinding();
            
            final boolean checkOk;
            if (isDrawnColorBad(
                    binding,
                    this.methodType,
                    this.srcImageType,
                    this.current_graphicsType,
                    this.current_withAlpha)) {
                final double absTol = getBadColorAbsTol(
                        binding,
                        this.methodType,
                        this.srcImageType,
                        this.current_graphicsType,
                        this.current_withAlpha);
                final double relTol = getBadColorRelTol(
                        binding,
                        this.methodType,
                        this.srcImageType,
                        this.current_graphicsType,
                        this.current_withAlpha);
                checkOk = areSomehowEqual_argb32(
                        expectedArgb32,
                        actualArgb32,
                        absTol,
                        relTol);
            } else {
                checkOk = areAboutEqual_argb32(expectedArgb32, actualArgb32);
            }
            if (!checkOk) {
                throw new AssertionError("ERROR : "
                        + Argb32.toString(actualArgb32)
                        + " too different from "
                        + Argb32.toString(expectedArgb32));
            }
        }
        /**
         * @return For anti-optim.
         */
        public int callConfigMethods(InterfaceBwdGraphics testG) {
            return callConfigMethods_static(
                    this.methodType,
                    testG,
                    nonDefaultFont);
        }
        /**
         * @return The box on pixels of which to do the bench.
         *         Must not be empty.
         */
        public GRect getBenchBox(InterfaceBwdGraphics testG) {
            /*
             * Cycling on all pixels, to avoid reaching
             * full opacity (too quickly) when benching
             * drawing with alpha.
             */
            if (this.instanceKind() == MyInstanceKind.GRAPHICS) {
                return testG.getBox();
            } else {
                return this.current_srcImage.getRect();
            }
        }
        /**
         * The pixel at the specified location must be drawn (used for check),
         * and eventually a few pixels around (but not too many
         * since we only want to check overhead).
         * 
         * The returned value is useful to avoid reading calls
         * being optimized away while benching.
         * 
         * @param testG Graphics to draw on, if any.
         * @return Read ARGB32 at (x,y), or 0 if not a read method.
         */
        public int callMethodForPixelAt(
                InterfaceBwdGraphics testG,
                int x,
                int y) {
            return callMethodForPixelAt_static(
                    this.methodType,
                    testG,
                    this.current_srcImage,
                    x,
                    y);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final boolean[] FALSE_TRUE = new boolean[]{false,true};

    private final List<MyTestable> testableList = new ArrayList<MyTestable>();

    /**
     * For each line, storing each element in a list,
     * for columns width and padding to be computed at each drawing.
     */
    private final List<List<String>> lineElemListList = new ArrayList<List<String>>();

    private int targetTestIndex = FIRST_TARGET_TEST_INDEX;

    private boolean didAddImageMethodsTestsHeader = false;

    private int lastDoneTestIndex = -1;

    private InterfaceBwdHost testHost;

    private InterfaceBwdWritableImage testGWi;

    private InterfaceBwdFont nonDefaultFont;

    private InterfaceBwdImage srcImage_opaque_png;
    private InterfaceBwdImage srcImage_opaque_bmp4;
    private InterfaceBwdImage srcImage_alpha_png;
    private InterfaceBwdWritableImage srcImage_wi;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public DrawingCheckAndOverheadBwdTestCase() {
    }

    public DrawingCheckAndOverheadBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }

    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new DrawingCheckAndOverheadBwdTestCase(binding);
    }
    
    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    /*
     * 
     */

    @Override
    public void onWindowShown(BwdWindowEvent event) {
        this.tryMoveTestHostNextToResultHost();
    }

    @Override
    public void onWindowMoved(BwdWindowEvent event) {
        this.tryMoveTestHostNextToResultHost();
    }

    @Override
    public void onWindowResized(BwdWindowEvent event) {
        this.tryMoveTestHostNextToResultHost();
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void onTestBegin() {

        final InterfaceBwdBinding binding = this.getBinding();

        // Using a specific client for checks and benches,
        // not to damage result client area.
        final BwdClientMock testClient = new BwdClientMock() {
            @Override
            protected List<GRect> paintClientImpl(
                    InterfaceBwdGraphics g,
                    GRect dirtyRect) {

                paintTestHostClient(g);

                return null;
            }
        };
        
        final boolean decorated = false;
        final InterfaceBwdHost testHost = binding.newHost(
                "TestHost", decorated, testClient);
        this.testHost = testHost;
        this.tryMoveNextToResultHost(testHost);
        testHost.show();

        this.testGWi = binding.newWritableImage(
                TEST_GRAPHICS_WIDTH,
                TEST_GRAPHICS_HEIGHT);

        final InterfaceBwdFontHome fontHome = getBinding().getFontHome();
        final InterfaceBwdFont defaultFont = fontHome.getDefaultFont();
        this.nonDefaultFont = fontHome.newFontWithSize(
                defaultFont.size() + 1);
        
        this.srcImage_opaque_png = binding.newImage(IMAGE_OPAQUE_PNG_FILE_PATH);
        this.srcImage_opaque_bmp4 = binding.newImage(IMAGE_OPAQUE_BMP4_FILE_PATH);
        this.srcImage_alpha_png = binding.newImage(IMAGE_WALPHA_PNG_FILE_PATH);
        this.srcImage_wi = binding.newWritableImage(SRC_WI_WIDTH, SRC_WI_HEIGHT);

        /*
         * 
         */

        this.createTestables();

        this.lineElemListList.add(Arrays.asList("Times are per call."));
        this.lineElemListList.add(Arrays.asList("(time) = color changed"));
        this.lineElemListList.add(Arrays.asList("before each call."));
        this.lineElemListList.add(Arrays.asList(""));
        this.lineElemListList.add(G_TESTS_HEADER);
    }

    @Override
    protected void onTestEnd() {
        
        this.lineElemListList.add(Arrays.asList(""));
        this.lineElemListList.add(Arrays.asList("Done."));

        this.testHost.close();
        this.testHost = null;

        this.testGWi.dispose();
        this.testGWi = null;

        this.nonDefaultFont.dispose();
        this.nonDefaultFont = null;

        this.srcImage_opaque_png.dispose();
        this.srcImage_opaque_png = null;

        this.srcImage_opaque_bmp4.dispose();
        this.srcImage_opaque_bmp4 = null;

        this.srcImage_alpha_png.dispose();
        this.srcImage_alpha_png = null;

        this.srcImage_wi.dispose();
        this.srcImage_wi = null;
    }

    @Override
    protected long testSome(long nowNs) {
        final int nbrOfTestable =
                this.testableList.size();
        if (this.targetTestIndex == nbrOfTestable) {
            // Done.
            return Long.MAX_VALUE;
        } else {
            // To trigger test host painting.
            this.testHost.ensurePendingClientPainting();

            return nowNs + TimeUtils.sToNs(REST_TIME_BETWEEN_BENCHES_S);
        }
    }

    @Override
    protected void drawCurrentState(InterfaceBwdGraphics g) {

        final InterfaceBwdFont font = g.getFont();
        final int lineHeight = font.metrics().height() + 1;

        g.setColor(BwdColor.BLACK);

        /*
         * 
         */

        final int colCount;
        {
            int tmp = 0;
            for (List<String> lineElemList : this.lineElemListList) {
                tmp = Math.max(tmp, lineElemList.size());
            }
            colCount = tmp;
        }

        final int[] colWidthArr = new int[colCount]; 
        {
            for (int col = 0; col < colCount; col++) {
                int tmp = 0;
                for (List<String> lineElemList : this.lineElemListList) {
                    if (lineElemList.size() > col) {
                        final String lineElem = lineElemList.get(col);
                        tmp = Math.max(tmp, lineElem.length());
                    }
                }
                colWidthArr[col] = tmp;
            }
        }

        final int x = 0;
        int tmpY = 0;

        for (List<String> lineElemList : this.lineElemListList) {
            final StringBuilder lineSb = new StringBuilder();

            int targetLineWidth = 0;

            for (int col = 0; col < lineElemList.size(); col++) {
                final int colWidth = colWidthArr[col];
                targetLineWidth += colWidth;

                lineSb.append(lineElemList.get(col));
                while (lineSb.length() < targetLineWidth) {
                    lineSb.append(" ");
                }

                final String interColSep = " | ";
                lineSb.append(interColSep);
                targetLineWidth += interColSep.length();
            }

            g.drawText(x, tmpY, lineSb.toString());
            tmpY += lineHeight;
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * Blending method working on non alpha-premultiplied colors,
     * but using intermediary alpha-premultiplied colors for blending
     * as done in our bindings. 
     */
    private static int blendArgb32_srcOver(int srcArgb32, int dstArgb32) {
        /*
         * Does not need to be optimized (for example for opaque case),
         * just used for checks.
         */
        final int srcPremulArgb32 = BindingColorUtils.toPremulAxyz32(srcArgb32);
        final int dstPremulArgb32 = BindingColorUtils.toPremulAxyz32(dstArgb32);
        final int newPremulArgb32 = BindingColorUtils.blendPremulAxyz32_srcOver(srcPremulArgb32, dstPremulArgb32);
        final int newArgb32 = BindingColorUtils.toNonPremulAxyz32(newPremulArgb32);
        return newArgb32;
    }

    /*
     * 
     */

    private static void checkArgb32(int expectedArgb32, int actualArgb32) {
        if (actualArgb32 != expectedArgb32) {
            throw new AssertionError(
                    Argb32.toString(actualArgb32)
                    + " != "
                    + Argb32.toString(expectedArgb32));
        }
    }

    /*
     * "about" equality methods, to be tolerant to different rounding methods
     * (in images formats or in bindings code).
     * Must use upper value as expected value, since we only allow actual
     * to be below it.
     */

    /**
     * Not being tolerant for alpha, for we didn't observe differences for it.
     * 
     * @return True if actual is same as expected, or with one or more
     *         non-alpha components inferior by one.
     */
    private static boolean areAboutEqual_argb32(int expectedArgb32, int actualArgb32) {
        final int a1 = Argb32.getAlpha8(expectedArgb32);
        final int r1 = Argb32.getRed8(expectedArgb32);
        final int g1 = Argb32.getGreen8(expectedArgb32);
        final int b1 = Argb32.getBlue8(expectedArgb32);

        final int a2 = Argb32.getAlpha8(actualArgb32);
        final int r2 = Argb32.getRed8(actualArgb32);
        final int g2 = Argb32.getGreen8(actualArgb32);
        final int b2 = Argb32.getBlue8(actualArgb32);

        return (a1 == a2)
                && areAboutEqual_cpt8(r1, r2)
                && areAboutEqual_cpt8(g1, g2)
                && areAboutEqual_cpt8(b1, b2);
    }

    /**
     * @return True if actual is same as expected, or inferior to it by one.
     */
    private static boolean areAboutEqual_cpt8(int expected8, int actual8) {
        return (actual8 == expected8) || (actual8 == expected8 - 1);
    }

    /*
     * "Somehow" equality methods, when colors get bad.
     * Applies tolerance on all components, alpha included.
     */

    private static boolean areSomehowEqual_argb32(
            int expectedArgb32,
            int actualArgb32,
            double absTol,
            double relTol) {
        final int a1 = Argb32.getAlpha8(expectedArgb32);
        final int r1 = Argb32.getRed8(expectedArgb32);
        final int g1 = Argb32.getGreen8(expectedArgb32);
        final int b1 = Argb32.getBlue8(expectedArgb32);

        final int a2 = Argb32.getAlpha8(actualArgb32);
        final int r2 = Argb32.getRed8(actualArgb32);
        final int g2 = Argb32.getGreen8(actualArgb32);
        final int b2 = Argb32.getBlue8(actualArgb32);

        return areSomehowEqual_cpt8(a1, a2, absTol, relTol)
                && areSomehowEqual_cpt8(r1, r2, absTol, relTol)
                && areSomehowEqual_cpt8(g1, g2, absTol, relTol)
                && areSomehowEqual_cpt8(b1, b2, absTol, relTol);
    }

    /**
     * @return True if values are close enough according to at least
     *         one tolerance, false otherwise.
     */
    private static boolean areSomehowEqual_cpt8(
            int expected8,
            int actual8,
            double absTol,
            double relTol) {
        final int absErr = Math.abs(actual8 - expected8);
        if (absErr <= absTol) {
            return true;
        }
        final double relErr = absErr / (double) Math.max(actual8, expected8);
        if (relErr <= relTol) {
            return true;
        }
        return false;
    }

    /*
     * 
     */

    private void paintTestHostClient(InterfaceBwdGraphics testClientG) {
        final int testIndex = this.targetTestIndex;
        final boolean mustDoTest =
                (testIndex != this.lastDoneTestIndex)
                && (testIndex < this.testableList.size());
        if (mustDoTest) {
            this.targetTestIndex++;
            this.lastDoneTestIndex = testIndex;

            final MyTestable testable = this.testableList.get(testIndex);

            testTestable(testable, testClientG);
        }
    }

    private void testTestable(
            MyTestable testable,
            InterfaceBwdGraphics testClientG) {

        final List<String> lineElemList = new ArrayList<String>();
        lineElemList.add(testable.toString());

        for (boolean withAlpha : FALSE_TRUE) {
            if (withAlpha
                    && (!testable.canDoAlpha())) {
                continue;
            }

            // +1 for check, class loading and warmup (first)
            final int baseNbrOfRuns = 1 + NBR_OF_MEASURES;

            final MyInstanceKind instanceKind = testable.instanceKind();
            if (instanceKind == MyInstanceKind.GRAPHICS) {

                final boolean makesSenseToConfigureGBeforeEachCall =
                        ((testable.opType() == MyOpType.SET)
                                || (testable.opType() == MyOpType.BLEND))
                                && (testable.methodType != MyMethodType.DRAW_IMAGE);
                final int nbrOfRuns;
                if (makesSenseToConfigureGBeforeEachCall) {
                    // +1 for configure before each call (last)
                    nbrOfRuns = baseNbrOfRuns + 1;
                } else {
                    nbrOfRuns = baseNbrOfRuns;
                }

                for (MyGraphicsType graphicsType : MyGraphicsType.values()) {
                    final InterfaceBwdGraphics testG;
                    if (graphicsType == MyGraphicsType.WI_GRAPHICS) {
                        testG = testGWi.getGraphics();
                    } else {
                        testG = testClientG;
                    }

                    final StringBuilder benchSb = new StringBuilder();

                    for (int k = 0; k < nbrOfRuns; k++) {

                        final boolean isCheckAndWarmupRound = (k == 0);
                        final boolean mustConfigureGBeforeEachCall =
                                makesSenseToConfigureGBeforeEachCall
                                && (k == nbrOfRuns - 1);

                        final boolean keepGoing = testTestable_oneRound(
                                testable,
                                testG,
                                graphicsType,
                                withAlpha,
                                isCheckAndWarmupRound,
                                mustConfigureGBeforeEachCall,
                                benchSb);
                        if (!keepGoing) {
                            break;
                        }
                    }

                    lineElemList.add(benchSb.toString());
                }
                
            } else if (instanceKind == MyInstanceKind.IMAGE) {
                if (!this.didAddImageMethodsTestsHeader) {
                    this.lineElemListList.add(Arrays.asList(""));
                    this.lineElemListList.add(I_TESTS_HEADER);
                    this.didAddImageMethodsTestsHeader = true;
                }

                final StringBuilder benchSb = new StringBuilder();

                final int nbrOfRuns = baseNbrOfRuns;
                
                for (int k = 0; k < nbrOfRuns; k++) {
                    final InterfaceBwdGraphics testG = null;
                    final MyGraphicsType graphicsType = null;

                    final boolean isCheckAndWarmupRound = (k == 0);
                    final boolean mustConfigureGBeforeEachCall = false;

                    final boolean keepGoing = testTestable_oneRound(
                            testable,
                            testG,
                            graphicsType,
                            withAlpha,
                            isCheckAndWarmupRound,
                            mustConfigureGBeforeEachCall,
                            benchSb);
                    if (!keepGoing) {
                        break;
                    }
                }

                lineElemList.add(benchSb.toString());
                
            } else {
                throw new IllegalArgumentException("" + instanceKind);
            }
        }

        this.lineElemListList.add(lineElemList);

        // To redraw results.
        this.getHost().ensurePendingClientPainting();
    }

    /**
     * @param benchSb (in,out)
     * @return True if must keep rounds for the use case.
     */
    private boolean testTestable_oneRound(
            MyTestable testable,
            InterfaceBwdGraphics testG,
            MyGraphicsType graphicsType,
            boolean withAlpha,
            boolean isCheckAndWarmupRound,
            boolean mustConfigureGBeforeEachCall,
            StringBuilder benchSb) {

        /*
         * Configuring.
         */

        testable.configureTestable(testG, graphicsType, withAlpha);

        /*
         * Check.
         * G_GET_ARGB_32_AT is the read we use to check other methods,
         * so we don't test it like this (it's tested by working
         * in tests where we compare its result against
         * expected colors from known images).
         */

        if ((testable.opType() != MyOpType.CONFIG)
                && isCheckAndWarmupRound
                && (testable.methodType != MyMethodType.G_GET_ARGB_32_AT)) {
            try {
                testable.doCorrectnessTest();
            } catch (Throwable e) {
                System.out.println("ERROR : " + testable.getRunCaseContext());
                e.printStackTrace(System.out);
                benchSb.append("KO");
                return false;
            }
        }

        /*
         * Bench.
         */

        final boolean mustDoTiming = !isCheckAndWarmupRound;
        final String timingStr = doBench(
                testable,
                testG,
                mustDoTiming,
                mustConfigureGBeforeEachCall);
        if (timingStr != null) {
            if (benchSb.length() != 0) {
                benchSb.append(", ");
            }
            benchSb.append(timingStr);
        }

        return true;
    }

    /**
     * @return A string representing the time, or null if no timing.
     */
    private String doBench(
            MyTestable testable,
            InterfaceBwdGraphics testG,
            boolean mustDoTiming,
            boolean mustConfigureGBeforeEachCall) {

        final GRect benchBox = testable.getBenchBox(testG);
        if (benchBox.isEmpty()) {
            throw new IllegalArgumentException("" + benchBox);
        }
        int x = benchBox.x();
        int y = benchBox.y();

        final int argb32_1 = testable.getArgb32ToDraw(testable.current_withAlpha);
        final int argb32_2 = Argb32.inverted(argb32_1);
        
        testable.resetPixelsForBench();
        
        if (testable.instanceKind() == MyInstanceKind.GRAPHICS) {
            testG.setArgb32(argb32_1);
        }
        
        int antiOptim = 0;

        int callCount = 0;

        final BenchTimeoutManager btm = new BenchTimeoutManager();
        btm.onBenchStart(MAX_MEASURE_DURATION_S);
        
        long t0Ns = System.nanoTime();
        long lastNs = t0Ns;
        if (testable.opType() == MyOpType.CONFIG) {
            for (int i = 0; i < MAX_NBR_OF_CALLS; i++) {

                antiOptim += testable.callConfigMethods(testG);

                callCount++;

                if (btm.isTimeoutReached()) {
                    break;
                }
            }
        } else {
            
            int scanCount = 0;
            
            for (int i = 0; i < MAX_NBR_OF_CALLS; i++) {

                if (mustConfigureGBeforeEachCall) {
                    final boolean flipElseFlop = NbrsUtils.isEven(i);
                    if (flipElseFlop) {
                        testG.setArgb32(argb32_1);
                    } else {
                        testG.setArgb32(argb32_2);
                    }
                }

                // Always 0 for non-read methods,
                // so then it doesn't work as anti optim,
                // but we prefer not to compute one
                // not to bias measures for these small calls.
                antiOptim += testable.callMethodForPixelAt(testG, x, y);

                callCount++;

                if (btm.isTimeoutReached()) {
                    break;
                }

                if (x < benchBox.xMax()) {
                    x++;
                } else {
                    x = benchBox.x();
                    if (y < benchBox.yMax()) {
                        y++;
                    } else {
                        y = benchBox.y();
                        
                        scanCount++;
                        if ((scanCount % BENCH_BOX_SCAN_COUNT_BEFORE_RESET) == 0) {
                            if ((testable.opType() == MyOpType.SET)
                                    || (testable.opType() == MyOpType.BLEND)) {
                                testable.resetPixelsForBench();
                            }
                        }
                    }
                }
            }
        }
        lastNs = System.nanoTime();

        btm.onBenchEnd();
        
        final String ret;
        if (mustDoTiming) {
            final long dtNs = lastNs - t0Ns;
            final long dtPerCallNs = dtNs / callCount;
            final double timePerCallUs = TestUtils.nsToUs(dtPerCallNs);
            final String measure = timePerCallUs + " us";
            if (mustConfigureGBeforeEachCall) {
                ret = "(" + measure + ")";
            } else {
                ret = measure;
            }
        } else {
            ret = null;
        }

        blackHole(antiOptim);

        return ret;
    }

    private static void blackHole(int value) {
        if (Math.cos(value) == 1.0/Math.PI) {
            System.out.println("can't happen, but compiler doesn't know");
        }
    }

    /*
     * 
     */

    /**
     * To stick test host to result host,
     * to help user figure out what that is.
     */
    private void tryMoveTestHostNextToResultHost() {
        final InterfaceBwdHost hostToMove = this.testHost;
        tryMoveNextToResultHost(hostToMove);
    }
    
    /**
     * @param hostToMove Can be null.
     */
    private void tryMoveNextToResultHost(InterfaceBwdHost hostToMove) {
        final GRect windowBounds = this.getHost().getWindowBounds();
        if ((hostToMove != null)
                && (!windowBounds.isEmpty())) {
            hostToMove.setWindowBounds(
                    GRect.valueOf(
                            windowBounds.xMax() + 1,
                            windowBounds.y(),
                            TEST_GRAPHICS_WIDTH,
                            TEST_GRAPHICS_HEIGHT));
        }
    }

    /**
     * Draws image center pixel at the specified location.
     */
    private static void drawImageTestedPixelAt(
            InterfaceBwdGraphics g,
            int x,
            int y,
            InterfaceBwdImage image) {
        g.drawImage(
                x, y, 1, 1,
                image,
                IMAGE_TESTED_PIXEL_X,
                IMAGE_TESTED_PIXEL_Y,
                1,
                1);
    }

    /*
     * 
     */

    /**
     * Is quick.
     * 
     * @return A value depending on current color, clip, transform and font.
     */
    private static int computeConfigAntiOptim(InterfaceBwdGraphics testG) {
        return testG.getArgb32()
                + testG.getClipInBase().x()
                + testG.getTransform().frame2XIn1()
                + testG.getFont().size();
    }

    private static int callConfigMethods_static(
            MyMethodType methodType,
            InterfaceBwdGraphics testG,
            InterfaceBwdFont font) {

        int antiOptim = 0;

        testG.setArgb32(ARGB_32_COLOR_WALPHA);
        testG.addClipInBase(ADDED_CLIP);
        testG.addTransform(ADDED_TRANSFORM);
        testG.setFont(font);

        antiOptim += computeConfigAntiOptim(testG);

        testG.reset();

        antiOptim += computeConfigAntiOptim(testG);

        return antiOptim;
    }

    private static int callMethodForPixelAt_static(
            MyMethodType methodType,
            InterfaceBwdGraphics testG,
            InterfaceBwdImage srcImg,
            int x,
            int y) {

        int retArgb32 = 0;

        /*
         * Values to avoid special cases like
         * points, lines, rects, circles or ovals.
         */
        switch (methodType) {
            case CLEAR_RECT: {
                testG.clearRect(x, y, 5, 3);
            } break;
            /*
             * 
             */
            case DRAW_POINT: {
                testG.drawPoint(x, y);
            } break;
            case DRAW_LINE: {
                testG.drawLine(x, y, x+1, y+2);
            } break;
            case DRAW_RECT: {
                testG.drawRect(x, y, 5, 3);
            } break;
            case FILL_RECT: {
                testG.fillRect(x, y, 5, 3);
            } break;
            case DRAW_OVAL: {
                // (x,y) is top pixel.
                testG.drawOval(x - 2, y, 5, 3);
            } break;
            case FILL_OVAL: {
                // (x,y) is top pixel.
                testG.fillOval(x - 2, y, 5, 3);
            } break;
            case DRAW_ARC: {
                // (x,y) is top pixel.
                testG.drawArc(
                        x - 2, y, 5, 3,
                        0, 315);
            } break;
            case FILL_ARC: {
                // (x,y) is top pixel.
                testG.fillArc(
                        x - 2, y, 5, 3,
                        0, 315);
            } break;
            case DRAW_POLYLINE: {
                // (x,y) is top pixel.
                testG.drawPolyline(
                        new int[] {x, x + 2, x - 2},
                        new int[] {y, y + 2, y + 2},
                        3);
            } break;
            case DRAW_POLYGON: {
                // (x,y) is top pixel.
                testG.drawPolygon(
                        new int[] {x, x + 2, x - 2},
                        new int[] {y, y + 2, y + 2},
                        3);
            } break;
            case FILL_POLYGON: {
                // (x,y) is top pixel.
                testG.fillPolygon(
                        new int[] {x, x + 2, x - 2},
                        new int[] {y, y + 2, y + 2},
                        3);
            } break;
            /*
             * 
             */
            case DRAW_TEXT: {
                BwdTestUtils.drawTextCentered(testG, x, y, TEXT_TO_DRAW);
            } break;
            /*
             * 
             */
            case DRAW_IMAGE: {
                drawImageTestedPixelAt(testG, x, y, srcImg);
            } break;
            /*
             * 
             */
            case FLIP_COLORS: {
                testG.flipColors(x, y, 1, 1);
            } break;
            /*
             * 
             */
            case G_GET_ARGB_32_AT: {
                retArgb32 = testG.getArgb32At(x, y);
            } break;
            /*
             * 
             */
            case I_GET_ARGB_32_AT: {
                retArgb32 = srcImg.getArgb32At(x, y);
            } break;
            /*
             * 
             */
            default:
                throw new IllegalArgumentException("" + methodType);
        }
        
        return retArgb32;
    }

    /*
     * 
     */

    private void createTestables() {
        for (MyMethodType methodType : MyMethodType.values()) {
            if ((methodType == MyMethodType.DRAW_IMAGE)
                    || (methodType == MyMethodType.I_GET_ARGB_32_AT)) {
                this.testableList.add(new MyTestable(methodType, MyImageType.IFF_PNG));
                this.testableList.add(new MyTestable(methodType, MyImageType.IFF_BMP4));
                this.testableList.add(new MyTestable(methodType, MyImageType.WI));
            } else {
                this.testableList.add(new MyTestable(methodType));
            }
        }
    }
}
