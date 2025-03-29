/*
 * Copyright 2020-2025 Jeff Hain
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
import net.jolikit.bwd.api.graphics.BwdScalingType;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
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
 * To bench drawings of large figures, i.e. over many pixels at once
 * (with primitives over large areas, large text, large images, etc.).
 * 
 * These tests are helpful to choose between various backing library usages.
 * 
 * NB: These benches can be biased for asynchronous libraries,
 * for not taking into account the rendering done in another thread.
 */
public class DrawingBenchBwdTestCase extends AbstractUnitTestBwdTestCase {

    /*
     * Derived from DrawingCheckAndOverheadBwdTestCase,
     * removed checks and drawing large figures.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final BwdScalingType SCALING_TYPE_NEAREST = BwdScalingType.NEAREST;
    
    /**
     * Normally 0.
     * Handy to start at a specific test.
     */
    private static final int FIRST_TARGET_TEST_INDEX = 0;

    private static final int NBR_OF_MEASURES = 1;

    /**
     * For class load and a bit more.
     */
    private static final int WARMUP_CALL_COUNT = 10;
    
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

    /*
     * 
     */
    
    private static final int CLEAR_ARGB_32_WALPHA = 0x0204080A;

    private static final int ARGB_32_COLOR_WALPHA = 0x022040FF;
    private static final int ARGB_32_COLOR_OPAQUE = Argb32.toOpaque(ARGB_32_COLOR_WALPHA);
    
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
     * Not just one letter, to be more general.
     */
    private static final String TEXT_TO_DRAW = "TEXT";

    /*
     * 
     */

    private static final String IMAGE_OPAQUE_PNG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG;
    private static final String IMAGE_WALPHA_PNG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_ALPHA_PNG;

    /*
     * Same dimensions than cat and mice image.
     * About 500k pixels.
     */

    private static final int FIGURE_WIDTH = 960;
    private static final int FIGURE_HEIGHT = 540;

    /**
     * Not too small, for algos with bad complexity to hurt.
     */
    private static final int POLYGON_POINT_COUNT = 1000;
    /**
     * Not too small, for algos with bad complexity to hurt.
     * 30 points per round, for spiral to be mostly roundish.
     */
    private static final double POLYGON_SPIRAL_ROUND_COUNT = POLYGON_POINT_COUNT / 30;

    private static final int[] POLYGON_ELLIPSE_X_ARR = new int[POLYGON_POINT_COUNT];
    private static final int[] POLYGON_ELLIPSE_Y_ARR = new int[POLYGON_POINT_COUNT];
    static {
        final int xMaxRadius = FIGURE_WIDTH / 2;
        final int yMaxRadius = FIGURE_HEIGHT / 2;
        final double stepRad = (2*Math.PI) / POLYGON_POINT_COUNT;
        for (int k = 0; k < POLYGON_POINT_COUNT; k++) {
            final double angRad = k * stepRad;
            POLYGON_ELLIPSE_X_ARR[k] = (int) (xMaxRadius * Math.sin(angRad));
            POLYGON_ELLIPSE_Y_ARR[k] = (int) (yMaxRadius * Math.cos(angRad));
        }
        // For centering, without having to add a transform.
        for (int k = 0; k < POLYGON_POINT_COUNT; k++) {
            POLYGON_ELLIPSE_X_ARR[k] += xMaxRadius;
            POLYGON_ELLIPSE_Y_ARR[k] += yMaxRadius;
        }
    }

    private static final int[] POLYGON_SPIRAL_X_ARR = new int[POLYGON_POINT_COUNT];
    private static final int[] POLYGON_SPIRAL_Y_ARR = new int[POLYGON_POINT_COUNT];
    static {
        final int xMaxRadius = FIGURE_WIDTH / 2;
        final int yMaxRadius = FIGURE_HEIGHT / 2;
        BwdTestUtils.computeSpiralPolygonPoints(
                xMaxRadius,
                yMaxRadius,
                POLYGON_POINT_COUNT,
                POLYGON_SPIRAL_ROUND_COUNT,
                POLYGON_SPIRAL_X_ARR,
                POLYGON_SPIRAL_Y_ARR);
        // For centering, without having to add a transform.
        for (int k = 0; k < POLYGON_POINT_COUNT; k++) {
            POLYGON_SPIRAL_X_ARR[k] += xMaxRadius;
            POLYGON_SPIRAL_Y_ARR[k] += yMaxRadius;
        }
    }

    private static final int TEST_GRAPHICS_WIDTH = FIGURE_WIDTH;
    private static final int TEST_GRAPHICS_HEIGHT = FIGURE_HEIGHT;

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

    private static final List<String> B_TESTS_HEADER =
            Arrays.asList(
                    "Binding Methods",
                    "(Opaque)",
                    "(Alpha)");

    /*
     * 
     */

    private static final int INITIAL_WIDTH = 500 + (NBR_OF_MEASURES * 300);
    private static final int INITIAL_HEIGHT = 820;
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
        IMAGE,
        /**
         * The binding. Some of its methods can call drawing treatments,
         * such as during images creations.
         */
        BINDING;
    }

    private enum MyOpType {
        NO_OP,
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
        CLEAR_RECT(MyInstanceKind.GRAPHICS, MyOpType.SET),
        //
        DRAW_POINT_HL(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_POINT_VL(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_POINT_RECT(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_LINE_H(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_LINE_V(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_LINE_GEN(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_LINE_HRECT(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_LINE_VRECT(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_RECT(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        FILL_RECT(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_OVAL(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        FILL_OVAL(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_ARC(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        FILL_ARC(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        /*
         * Not bothering to bench polyline.
         */
        DRAW_POLYGON_ELLIPSE(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_POLYGON_SPIRAL(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        FILL_POLYGON_ELLIPSE(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        FILL_POLYGON_SPIRAL(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        //
        DRAW_TEXT(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        //
        DRAW_IMAGE_NEAREST(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        DRAW_IMAGE_BICU(MyInstanceKind.GRAPHICS, MyOpType.BLEND),
        //
        FLIP_COLORS(MyInstanceKind.GRAPHICS, MyOpType.INVERT),
        //
        G_GET_ARGB_32_AT_RECT(MyInstanceKind.GRAPHICS, MyOpType.GET),
        //
        I_GET_ARGB_32_AT_RECT(MyInstanceKind.IMAGE, MyOpType.GET),
        //
        B_NEW_IMAGE(MyInstanceKind.BINDING, MyOpType.NO_OP),
        //
        B_NEW_WRITABLE_IMAGE(MyInstanceKind.BINDING, MyOpType.NO_OP);
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
         * Images From File (opaque or not).
         */
        IFF,
        /**
         * Writable Image.
         */
        WI;
    }

    private class MyTestable {
        final MyMethodType methodType;
        /**
         * Useful when >= 2, to check that eventually delegating
         * to methods with lower overhead when area is small.
         */
        final int spanDiv;
        final MyImageType srcImageType;
        InterfaceBwdGraphics current_testG;
        boolean current_withAlpha;
        InterfaceBwdFont current_font;
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
            this(methodType, 1, null);
        }
        /**
         * @param methodType Must not be null.
         * @param spanDiv Only used for area methods. Must be >= 1.
         */
        public MyTestable(
            MyMethodType methodType,
            int spanDiv) {
            this(methodType, spanDiv, null);
        }
        /**
         * @param methodType Must not be null.
         * @param spanDiv Only used for area methods. Must be >= 1.
         * @param srcImageType Only useful for DRAW_IMAGE_XXX methods.
         */
        public MyTestable(
                MyMethodType methodType,
                int spanDiv,
                MyImageType srcImageType) {
            this.methodType = LangUtils.requireNonNull(methodType);
            this.spanDiv = NbrsUtils.requireSupOrEq(1, spanDiv, "spanDiv");
            if ((methodType == MyMethodType.DRAW_IMAGE_NEAREST)
                || (methodType == MyMethodType.DRAW_IMAGE_BICU)) {
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
            if (this.spanDiv >= 2) {
                sb.append(" (spans/");
                sb.append(this.spanDiv);
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
         * @return The base ARGB32 used to draw,
         *         along with a derived color for flip/flop.
         */
        public int getBaseArgb32ToDraw(boolean withAlpha) {
            return (withAlpha ? ARGB_32_COLOR_WALPHA : ARGB_32_COLOR_OPAQUE);
        }
        public void configureTestable(
                InterfaceBwdGraphics testG,
                MyGraphicsType graphicsType,
                boolean withAlpha) {
            this.current_testG = testG;
            this.current_withAlpha = withAlpha;

            // Early and systematic nullification,
            // for throw if used before being set.
            this.current_font = null;
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

            this.current_font = figureHeightFont;

            if (this.srcImageType == MyImageType.IFF) {
                if (withAlpha) {
                    this.current_srcImage = srcImage_alpha_png;
                } else {
                    this.current_srcImage = srcImage_opaque_png;
                }

            } else if (this.srcImageType == MyImageType.WI) {
                this.current_srcImage = srcImage_wi;
                
            } else {
                // No src image.
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
                 * for G.DRAW_IMAGE_XXX(WI), or WI.GET_ARGB_32_AT().  
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
            g.setImageScalingType(SCALING_TYPE_NEAREST);
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
        /**
         * The pixel at the specified location must be drawn (used for check),
         * and eventually a few pixels around (but not too many
         * since we only want to check overhead).
         * 
         * The returned value is useful to avoid reading calls
         * being optimized away while benching.
         * 
         * @param g Graphics to draw on.
         * @return A value for anti-optim.
         */
        public int callMethodAt(
                InterfaceBwdGraphics testG,
                int x,
                int y) {
            final String imageFilePath;
            if (this.current_withAlpha) {
                imageFilePath = IMAGE_WALPHA_PNG_FILE_PATH;
            } else {
                imageFilePath = IMAGE_OPAQUE_PNG_FILE_PATH;
            }
            return callMethodAt_static(
                    getBinding(),
                    this.methodType,
                    this.spanDiv,
                    testG,
                    this.current_srcImage,
                    imageFilePath,
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
    private boolean didAddBindingMethodsTestsHeader = false;

    private int lastDoneTestIndex = -1;

    private InterfaceBwdHost testHost;

    private InterfaceBwdWritableImage testGWi;

    private InterfaceBwdFont figureHeightFont;

    private InterfaceBwdImage srcImage_opaque_png;
    private InterfaceBwdImage srcImage_alpha_png;
    private InterfaceBwdWritableImage srcImage_wi;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public DrawingBenchBwdTestCase() {
    }

    public DrawingBenchBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }

    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new DrawingBenchBwdTestCase(binding);
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
        this.tryMoveNextToResultHost(testHost);
        testHost.show();
        
        this.testHost = testHost;

        this.testGWi = binding.newWritableImage(
                TEST_GRAPHICS_WIDTH,
                TEST_GRAPHICS_HEIGHT);

        final InterfaceBwdFontHome fontHome = binding.getFontHome();
        this.figureHeightFont = fontHome.newFontWithClosestHeight(
                FIGURE_HEIGHT);

        this.srcImage_opaque_png = binding.newImage(IMAGE_OPAQUE_PNG_FILE_PATH);
        this.srcImage_alpha_png = binding.newImage(IMAGE_WALPHA_PNG_FILE_PATH);
        this.srcImage_wi = binding.newWritableImage(FIGURE_WIDTH, FIGURE_HEIGHT);

        /*
         * 
         */

        this.createTestables();

        this.lineElemListList.add(Arrays.asList(
                "Times are per figure (" + FIGURE_WIDTH
                + " x " + FIGURE_HEIGHT + ")."));
        this.lineElemListList.add(Arrays.asList("Color changed before"));
        this.lineElemListList.add(Arrays.asList("each figure (when used)."));
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

        this.figureHeightFont.dispose();
        this.figureHeightFont = null;

        this.srcImage_opaque_png.dispose();
        this.srcImage_opaque_png = null;

        this.srcImage_alpha_png.dispose();
        this.srcImage_alpha_png = null;

        this.srcImage_wi.dispose();
        this.srcImage_wi = null;
        
        // To see "Done".
        this.getHost().ensurePendingClientPainting();
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

            // +1 for class loading and warmup (first)
            final int nbrOfRuns = 1 + NBR_OF_MEASURES;

            final MyInstanceKind instanceKind = testable.instanceKind();
            if (instanceKind == MyInstanceKind.GRAPHICS) {

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

                        testTestable_oneRound(
                                testable,
                                testG,
                                graphicsType,
                                withAlpha,
                                isCheckAndWarmupRound,
                                benchSb);
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

                for (int k = 0; k < nbrOfRuns; k++) {
                    final InterfaceBwdGraphics testG = null;
                    final MyGraphicsType graphicsType = null;

                    final boolean isCheckAndWarmupRound = (k == 0);

                    testTestable_oneRound(
                            testable,
                            testG,
                            graphicsType,
                            withAlpha,
                            isCheckAndWarmupRound,
                            benchSb);
                }

                lineElemList.add(benchSb.toString());
                
            } else if (instanceKind == MyInstanceKind.BINDING) {
                if (!this.didAddBindingMethodsTestsHeader) {
                    this.lineElemListList.add(Arrays.asList(""));
                    this.lineElemListList.add(B_TESTS_HEADER);
                    this.didAddBindingMethodsTestsHeader = true;
                }

                final StringBuilder benchSb = new StringBuilder();

                for (int k = 0; k < nbrOfRuns; k++) {
                    final InterfaceBwdGraphics testG = null;
                    final MyGraphicsType graphicsType = null;

                    final boolean isCheckAndWarmupRound = (k == 0);

                    testTestable_oneRound(
                            testable,
                            testG,
                            graphicsType,
                            withAlpha,
                            isCheckAndWarmupRound,
                            benchSb);
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
     */
    private void testTestable_oneRound(
            MyTestable testable,
            InterfaceBwdGraphics testG,
            MyGraphicsType graphicsType,
            boolean withAlpha,
            boolean isCheckAndWarmupRound,
            StringBuilder benchSb) {

        /*
         * Configuring.
         */

        testable.configureTestable(
                testG,
                graphicsType,
                withAlpha);

        /*
         * Bench.
         */

        final boolean mustDoTiming = !isCheckAndWarmupRound;
        final String timingStr = doBench(
                testable,
                testG,
                mustDoTiming);
        if (timingStr != null) {
            if (benchSb.length() != 0) {
                benchSb.append(", ");
            }
            benchSb.append(timingStr);
        }
    }

    /**
     * @return A string representing the time, or null if no timing.
     */
    private String doBench(
            MyTestable testable,
            InterfaceBwdGraphics testG,
            boolean mustDoTiming) {

        final boolean isCurrentColorUsed =
                (testable.instanceKind() == MyInstanceKind.GRAPHICS)
                && (testable.methodType != MyMethodType.DRAW_IMAGE_NEAREST)
                && (testable.methodType != MyMethodType.DRAW_IMAGE_BICU)
                && ((testable.opType() == MyOpType.SET)
                        || (testable.opType() == MyOpType.BLEND));

        final int x0 = 0;
        final int y0 = 0;
        
        testable.resetPixelsForBench();

        final int argb32_1 = testable.getBaseArgb32ToDraw(testable.current_withAlpha);
        final int argb32_2 = Argb32.inverted(argb32_1);

        int antiOptim = 0;
        for (int i = 0; i < WARMUP_CALL_COUNT; i++) {
            antiOptim += testable.callMethodAt(testG, x0, y0);
        }

        int callCount = 0;

        if (testable.methodType == MyMethodType.DRAW_TEXT) {
            // No need to test before each drawText() call.
            testG.setFont(testable.current_font);
        }

        final BenchTimeoutManager btm = new BenchTimeoutManager();
        btm.onBenchStart(MAX_MEASURE_DURATION_S);

        long t0Ns = System.nanoTime();
        long lastNs = t0Ns;
        for (int i = 0; i < MAX_NBR_OF_CALLS; i++) {

            if (((i + 1) % BENCH_BOX_SCAN_COUNT_BEFORE_RESET) == 0) {
                testable.resetPixelsForBench();
            }

            final boolean flipElseFlop = NbrsUtils.isEven(i);

            final boolean isDrawing =
                    (testable.opType() == MyOpType.SET)
                    || (testable.opType() == MyOpType.BLEND)
                    || (testable.opType() == MyOpType.INVERT);
            final int x;
            if (isDrawing) {
                if (isCurrentColorUsed) {
                    /*
                     * Flip/flop on color, for pixels to change
                     * even when drawing a same figure over and over.
                     */
                    if (flipElseFlop) {
                        testG.setArgb32(argb32_1);
                    } else {
                        testG.setArgb32(argb32_2);
                    }
                    x = x0;
                } else {
                    /*
                     * Flip/flop on x, for pixels to change
                     * even when drawing a same image over and over.
                     */
                    if (flipElseFlop) {
                        x = x0;
                    } else {
                        x = x0 + 1;
                    }
                }
            } else {
                x = x0;
            }

            antiOptim += testable.callMethodAt(testG, x, y0);

            callCount++;

            if (btm.isTimeoutReached()) {
                break;
            }
        }
        lastNs = System.nanoTime();

        btm.onBenchEnd();

        final String ret;
        if (mustDoTiming) {
            final long dtNs = lastNs - t0Ns;
            final long dtPerCallNs = dtNs / callCount;
            final double timePerCallMs = TestUtils.nsToMsRounded(dtPerCallNs);
            ret = timePerCallMs + " ms";
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

    /*
     * 
     */

    /**
     * @return Min span, for fair H/V comparison.
     */
    private static int computeMinSpan(int xSpan, int ySpan) {
        return Math.min(xSpan, ySpan);
    }

    /**
     * @return A value for anti-optim.
     */
    private static int callMethodAt_static(
            InterfaceBwdBinding binding,
            MyMethodType methodType,
            int spanDiv,
            InterfaceBwdGraphics testG,
            InterfaceBwdImage srcImg,
            String imageFilePath,
            int x,
            int y) {

        int antiOptim = 0;

        final int xSpan = FIGURE_WIDTH;
        final int ySpan = FIGURE_HEIGHT;
        
        final int pointCount = POLYGON_POINT_COUNT;

        /*
         * Values to avoid special cases like
         * points, lines, rects, circles or ovals.
         */
        switch (methodType) {
            case CLEAR_RECT: {
                testG.clearRect(x, y, xSpan, ySpan);
            } break;
            /*
             * 
             */
            case DRAW_POINT_HL: {
                final int minSpan = computeMinSpan(xSpan, ySpan);
                for (int i = 0; i < minSpan; i++) {
                    testG.drawPoint(x + i, y);
                }
            } break;
            case DRAW_POINT_VL: {
                final int minSpan = computeMinSpan(xSpan, ySpan);
                for (int j = 0; j < minSpan; j++) {
                    testG.drawPoint(x, y + j);
                }
            } break;
            case DRAW_POINT_RECT: {
                for (int j = 0; j < ySpan; j++) {
                    final int _y = y + j;
                    for (int i = 0; i < xSpan; i++) {
                        testG.drawPoint(x + i, _y);
                    }
                }
            } break;
            case DRAW_LINE_H: {
                final int minSpan = computeMinSpan(xSpan, ySpan);
                final int xMax = x + minSpan - 1;
                testG.drawLine(x, y, xMax, y);
            } break;
            case DRAW_LINE_V: {
                final int minSpan = computeMinSpan(xSpan, ySpan);
                final int yMax = y + minSpan - 1;
                testG.drawLine(x, y, x, yMax);
            } break;
            case DRAW_LINE_GEN: {
                final int xMax = x + xSpan - 1;
                final int yMax = y + ySpan - 1;
                testG.drawLine(x, y, xMax, yMax);
            } break;
            case DRAW_LINE_HRECT: {
                final int xMax = x + xSpan - 1;
                for (int j = 0; j < ySpan; j++) {
                    final int _y = y + j;
                    testG.drawLine(x, _y, xMax, _y);
                }
            } break;
            case DRAW_LINE_VRECT: {
                final int yMax = y + ySpan - 1;
                for (int i = 0; i < xSpan; i++) {
                    final int _x = x + i;
                    testG.drawLine(_x, y, _x, yMax);
                }
            } break;
            case DRAW_RECT: {
                testG.drawRect(x, y, xSpan, ySpan);
            } break;
            case FILL_RECT: {
                testG.fillRect(x, y, xSpan, ySpan);
            } break;
            case DRAW_OVAL: {
                testG.drawOval(x, y, xSpan, ySpan);
            } break;
            case FILL_OVAL: {
                testG.fillOval(x, y, xSpan, ySpan);
            } break;
            case DRAW_ARC: {
                testG.drawArc(
                        x, y, xSpan, ySpan,
                        0, 315);
            } break;
            case FILL_ARC: {
                testG.fillArc(
                        x, y, xSpan, ySpan,
                        0, 315);
            } break;
            case DRAW_POLYGON_ELLIPSE: {
                testG.drawPolygon(
                        POLYGON_ELLIPSE_X_ARR,
                        POLYGON_ELLIPSE_Y_ARR,
                        pointCount);
            } break;
            case DRAW_POLYGON_SPIRAL: {
                testG.drawPolygon(
                        POLYGON_SPIRAL_X_ARR,
                        POLYGON_SPIRAL_Y_ARR,
                        pointCount);
            } break;
            case FILL_POLYGON_ELLIPSE: {
                testG.fillPolygon(
                        POLYGON_ELLIPSE_X_ARR,
                        POLYGON_ELLIPSE_Y_ARR,
                        pointCount);
            } break;
            case FILL_POLYGON_SPIRAL: {
                testG.fillPolygon(
                        POLYGON_SPIRAL_X_ARR,
                        POLYGON_SPIRAL_Y_ARR,
                        pointCount);
            } break;
            /*
             * 
             */
            case DRAW_TEXT: {
                testG.drawText(x, y, TEXT_TO_DRAW);
            } break;
            /*
             * 
             */
            case DRAW_IMAGE_NEAREST: {
                // No need to scale, image is of proper size.
                if ((srcImg.getWidth() != xSpan)
                        || (srcImg.getHeight() != ySpan)) {
                    throw new AssertionError("" + srcImg.getRect());
                }
                final int width = xSpan / spanDiv;
                final int height = ySpan / spanDiv;
                testG.drawImage(
                    x, y, width, height,
                    srcImg,
                    0, 0, width, height);
            } break;
            case DRAW_IMAGE_BICU: {
                if ((srcImg.getWidth() != xSpan)
                    || (srcImg.getHeight() != ySpan)) {
                    throw new AssertionError("" + srcImg.getRect());
                }
                testG.setImageScalingType(BwdScalingType.ITERATIVE_BICUBIC);
                // 1/4th of the image scaled up to x/y spans.
                testG.drawImage(
                    x,
                    y,
                    srcImg.getWidth(),
                    srcImg.getHeight(),
                    //
                    srcImg,
                    0,
                    0,
                    srcImg.getWidth() / 2,
                    srcImg.getHeight() / 2);
                testG.setImageScalingType(SCALING_TYPE_NEAREST);
            } break;
            /*
             * 
             */
            /*
             * 
             */
            case FLIP_COLORS: {
                final int width = xSpan / spanDiv;
                final int height = ySpan / spanDiv;
                testG.flipColors(x, y, width, height);
            } break;
            /*
             * 
             */
            case G_GET_ARGB_32_AT_RECT: {
                for (int j = 0; j < ySpan; j++) {
                    final int _y = y + j;
                    for (int i = 0; i < xSpan; i++) {
                        antiOptim += testG.getArgb32At(x + i, _y);
                    }
                }
            } break;
            /*
             * 
             */
            case I_GET_ARGB_32_AT_RECT: {
                for (int j = 0; j < ySpan; j++) {
                    final int _y = y + j;
                    for (int i = 0; i < xSpan; i++) {
                        antiOptim += srcImg.getArgb32At(x + i, _y);
                    }
                }
            } break;
            /*
             * 
             */
            case B_NEW_IMAGE: {
                final InterfaceBwdImage image =
                        binding.newImage(imageFilePath);
                antiOptim = image.getArgb32At(xSpan/2, ySpan/2);
                image.dispose();
            } break;
            case B_NEW_WRITABLE_IMAGE: {
                final InterfaceBwdWritableImage image =
                        binding.newWritableImage(xSpan, ySpan);
                antiOptim = image.getArgb32At(xSpan/2, ySpan/2);
                image.dispose();
            } break;
            /*
             * 
             */
            default:
                throw new IllegalArgumentException("" + methodType);
        }
        
        final MyInstanceKind instanceKind = methodType.instanceKind;
        if (instanceKind == MyInstanceKind.GRAPHICS) {
            antiOptim = testG.getArgb32At(x, y);
        } else if (instanceKind == MyInstanceKind.IMAGE) {
            antiOptim = srcImg.getArgb32At(x, y);
        } else if (instanceKind == MyInstanceKind.BINDING) {
            // Already set.
        } else {
            throw new IllegalArgumentException("" + instanceKind);
        }

        return antiOptim;
    }

    /*
     * 
     */

    private void createTestables() {
        final int log2MinSpan =
            NbrsUtils.log2(
                Math.min(FIGURE_WIDTH, FIGURE_HEIGHT));
        
        for (MyMethodType methodType : MyMethodType.values()) {
            if (methodType == MyMethodType.DRAW_IMAGE_NEAREST) {
                this.testableList.add(new MyTestable(methodType, 1, MyImageType.IFF));
                for (int i = 0; i < log2MinSpan; i++) {
                    final int spanDiv = (1 << i);
                    this.testableList.add(new MyTestable(methodType, spanDiv, MyImageType.WI));
                }
            } else if ((methodType == MyMethodType.DRAW_IMAGE_BICU)
                    || (methodType == MyMethodType.I_GET_ARGB_32_AT_RECT)) {
                this.testableList.add(new MyTestable(methodType, 1, MyImageType.IFF));
                this.testableList.add(new MyTestable(methodType, 1, MyImageType.WI));
            } else if (methodType == MyMethodType.FLIP_COLORS) {
                for (int i = 0; i < log2MinSpan; i++) {
                    final int spanDiv = (1 << i);
                    this.testableList.add(new MyTestable(methodType, spanDiv));
                }
            } else {
                this.testableList.add(new MyTestable(methodType));
            }
        }
    }
}
