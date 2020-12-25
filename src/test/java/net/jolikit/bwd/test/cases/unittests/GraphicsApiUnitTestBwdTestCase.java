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
package net.jolikit.bwd.test.cases.unittests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.Argb3264;
import net.jolikit.bwd.api.graphics.Argb64;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GRotation;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.utils.gprim.GprimUtils;
import net.jolikit.bwd.test.cases.utils.AbstractUnitTestBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.Dbg;
import net.jolikit.time.TimeUtils;

/**
 * Tests the behavior of InterfaceBwdGraphics API,
 * but not actual drawings (i.e. whether colors are written or read properly).
 */
public class GraphicsApiUnitTestBwdTestCase extends AbstractUnitTestBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final double CHECK_PERIOD_S = 0.1;

    /*
     * 
     */

    /**
     * Left part of client for client graphics result,
     * right part of client for image graphics results.
     */
    private static final int WIDTH_PER_GRAPHICS_RESULTS = 200;
    private static final int HEIGHT_PER_GRAPHICS_RESULTS = 200;

    private static final int NBR_OF_GRAPHICS = 2;

    private static final int INITIAL_WIDTH = NBR_OF_GRAPHICS * WIDTH_PER_GRAPHICS_RESULTS;
    private static final int INITIAL_HEIGHT = HEIGHT_PER_GRAPHICS_RESULTS;

    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int MIN = Integer.MIN_VALUE;
    private static final int MAX = Integer.MAX_VALUE;
    
    /**
     * Positions in all int range.
     * 
     * To check that drawing methods don't throw or take forever
     * in case of drawable and/or huge positions or spans.
     */
    private static final int[] POS_ANY_ARR = new int [] {
        MIN,
        MIN / 2,
        -1, 0, 1,
        MAX / 2,
        MAX
    };

    /**
     * Contains negative spans, which are allowed.
     */
    private static final int[] SPAN_ANY_ARR = new int [] {
        MIN,
        -1, 0, 1, 2, 3, 4, 5,
        MAX / 3,
        MAX / 2,
        MAX
    };

    /**
     * General and special cases.
     */
    private static final List<GRect> RECT_ANY_LIST;
    static {
        final List<GRect> list = new ArrayList<GRect>();
        /*
         * Separating X and Y cases,
         * not to have too many zillions of rectangles.
         */
        /*
         * X cases.
         */
        for (int x : POS_ANY_ARR) {
            for (int xSpan : SPAN_ANY_ARR) {
                if (xSpan < 0) {
                    continue;
                }
                // Empty in y.
                list.add(GRect.valueOf(x, 0, xSpan, 0));
                // Not empty in y.
                list.add(GRect.valueOf(x, 0, xSpan, 1));
            }
        }
        /*
         * Y cases.
         */
        for (int y : POS_ANY_ARR) {
            for (int ySpan : SPAN_ANY_ARR) {
                if (ySpan < 0) {
                    continue;
                }
                // Empty in x.
                list.add(GRect.valueOf(0, y, 0, ySpan));
                // Not empty in x.
                list.add(GRect.valueOf(0, y, 1, ySpan));
            }
        }
        RECT_ANY_LIST = Collections.unmodifiableList(list);
    }

    private static final double[] BAD_ANG_ARR = new double[]{
        Double.NaN,
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY
    };

    private static final int[] BAD_POINT_COUNT = new int[]{
            Integer.MIN_VALUE,
            Integer.MIN_VALUE/2,
            -1
        };

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private enum MyTest {
        LIFE_CYCLE,
        CLIPS,
        TRANSFORMS,
        COLORS,
        FONTS,
        PRIMITIVES,
        TEXT,
        IMAGES,
        COLORS_REWORK,
        COLORS_READING;
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final MyTest[] MyTest_VALUES = MyTest.values();
    
    private boolean mustTest = false;
    
    private int nextTextOrdinal = 0;
    
    private final Boolean[] testPassedByOrdinal_client = new Boolean[MyTest_VALUES.length];
    private final Boolean[] testPassedByOrdinal_image = new Boolean[MyTest_VALUES.length];
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public GraphicsApiUnitTestBwdTestCase() {
    }
    
    public GraphicsApiUnitTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new GraphicsApiUnitTestBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new GraphicsApiUnitTestBwdTestCase(this.getBinding());
    }
    
    /*
     * 
     */

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected long testSome(long nowNs) {
        
        if (this.nextTextOrdinal == MyTest_VALUES.length) {
            this.mustTest = false;
            return Long.MAX_VALUE;
        } else {
            this.mustTest = true;
            // ensuring drawings (in which we do tests) until we're done.
            this.getHost().ensurePendingClientPainting();
            
            return TimeUtils.sToNs(TimeUtils.nsToS(nowNs) + CHECK_PERIOD_S);
        }
    }

    @Override
    protected void drawCurrentState(InterfaceBwdGraphics g) {
        
        final InterfaceBwdWritableImage image = this.getBinding().newWritableImage(
                WIDTH_PER_GRAPHICS_RESULTS,
                HEIGHT_PER_GRAPHICS_RESULTS);
        
        try {
            if (this.mustTest) {
                final int testOrdinal = this.nextTextOrdinal++;
                if (DEBUG) {
                    System.out.println("testOrdinal = " + testOrdinal);
                }
                final MyTest test = MyTest_VALUES[testOrdinal];
                if (DEBUG) {
                    System.out.println("test = " + test);
                }

                /*
                 * Client graphics.
                 */

                if (DEBUG) {
                    System.out.println("testing on client graphics");
                }
                this.runNextTest(
                        g,
                        testOrdinal,
                        this.testPassedByOrdinal_client);
                
                /*
                 * Image graphics.
                 * 
                 * Drawing image graphics results on image graphics
                 * (as we draw client graphics results on client graphics),
                 * and then drawing image on client,
                 * which allows to test that each graphics actually
                 * allows to draw at least a bit.
                 */
                
                if (DEBUG) {
                    System.out.println("testing on image graphics");
                }
                this.runNextTest(
                        image.getGraphics(),
                        testOrdinal,
                        this.testPassedByOrdinal_image);
            }
        } finally {
            if (this.nextTextOrdinal == MyTest_VALUES.length) {
                this.mustTest = false;
            } else {
                // Next test ASAP.
                this.getHost().ensurePendingClientPainting();
            }

            drawGraphicsResults(
                    g,
                    "Client graphics:",
                    this.testPassedByOrdinal_client);
            
            drawGraphicsResults(
                    image.getGraphics(),
                    "Image graphics:",
                    this.testPassedByOrdinal_image);
            
            g.drawImage(WIDTH_PER_GRAPHICS_RESULTS, 0, image);
            
            image.dispose();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Clears the whole graphics box
     * and then draws results.
     * 
     * @param graphicsDescrStr Description of the tested graphics.
     * @param testPassedByOrdinal (in)
     */
    private static void drawGraphicsResults(
            InterfaceBwdGraphics g,
            String graphicsDescrStr,
            Boolean[] testPassedByOrdinal) {
        
        g.setColor(BwdColor.WHITE);
        g.clearRect(g.getBox());

        g.setColor(BwdColor.BLACK);
        
        final InterfaceBwdFontMetrics fontMetrics = g.getFont().metrics();
        final int dy = 1 + fontMetrics.height();
        
        int tmpY = 10;
        g.drawText(10, tmpY, graphicsDescrStr);
        tmpY += dy;

        g.drawLine(10, tmpY, 10 + fontMetrics.computeTextWidth(graphicsDescrStr), tmpY);
        tmpY += dy;

        for (int i = 0; i < MyTest_VALUES.length; i++) {
            final MyTest test = MyTest_VALUES[i];
            final Boolean bRef = testPassedByOrdinal[i];
            final String res = ((bRef == null) ? "" : (bRef ? "ok" : "---KO---"));
            g.drawText(10, tmpY, "" + test + " : " + res);
            tmpY += dy;
        }
    }
    
    /**
     * @param testPassedByOrdinal (in,out)
     */
    private void runNextTest(
            InterfaceBwdGraphics g,
            int testOrdinal,
            Boolean[] testPassedByOrdinal) {
        
        final MyTest test = MyTest_VALUES[testOrdinal];
        
        boolean completedNormally = false;
        try {
            switch (test) {
            case LIFE_CYCLE: {
                test_life_cycle(g);
            } break;
            case CLIPS: {
                test_clips(g);
            } break;
            case TRANSFORMS: {
                test_transfoms(g);
            } break;
            case COLORS: {
                test_colors(g);
            } break;
            case FONTS: {
                test_fonts(g);
            } break;
            case PRIMITIVES: {
                test_primitives(g);
            } break;
            case TEXT: {
                test_text(g);
            } break;
            case IMAGES: {
                test_images(g);
            } break;
            case COLORS_REWORK: {
                test_colors_rework(g);
            } break;
            case COLORS_READING: {
                test_colors_reading(g);
            } break;
            default:
                throw new AssertionError("" + test);
            }
            completedNormally = true;
        } finally {
            testPassedByOrdinal[testOrdinal] = completedNormally;
        }
    }
    
    /*
     * LIFE_CYCLE
     */
    
    private void test_life_cycle(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_life_cycle(...)");
        }

        try {
            g.newChildGraphics(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        try {
            g.newChildGraphics(GRect.DEFAULT_EMPTY, null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        try {
            g.newChildGraphics(null, GRect.DEFAULT_EMPTY);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        /*
         * 
         */
        
        final GRect box = g.getBox();
        
        /*
         * Box and initial clip.
         */
        
        {
            final GRect subBoxA = box.withBordersDeltas(0, 0, -2, -2);
            final GRect subBoxB = box.withBordersDeltas(2, 2, 0, 0);
            final GRect subBoxAB = subBoxA.intersected(subBoxB);
            
            final InterfaceBwdGraphics childG1 = g.newChildGraphics(subBoxA);
            checkEqual(subBoxA, childG1.getBox());
            checkEqual(subBoxA, childG1.getInitialClipInBase());
            
            final InterfaceBwdGraphics childG2 = childG1.newChildGraphics(subBoxB);
            checkEqual(subBoxB, childG2.getBox());
            checkEqual(subBoxAB, childG2.getInitialClipInBase());
            
            final InterfaceBwdGraphics childG3 = g.newChildGraphics(
                    subBoxA,
                    subBoxB);
            checkEqual(subBoxA, childG3.getBox());
            checkEqual(subBoxAB, childG3.getInitialClipInBase());
        }
        
        /*
         * Nested calls.
         */

        {
            final InterfaceBwdGraphics childG = g.newChildGraphics(box);
            
            childG.init();
            childG.init();
            
            childG.finish();
            // Idempotent...
            childG.finish();
            // ...even if called more times than init().
            childG.finish();
        }
        
        /*
         * init() after finish().
         */
        
        {
            final InterfaceBwdGraphics childG = g.newChildGraphics(box);
            childG.finish();
            try {
                childG.init();
                fail();
            } catch (IllegalStateException e) {
                // ok
            }
        }
        
        /*
         * reset().
         */
        
        {
            final InterfaceBwdGraphics childG = g.newChildGraphics(box);
            childG.init();
            try {
                final int initialArgb32 = childG.getArgb32();
                final GRect initialClip = childG.getClipInUser();
                final GTransform initialTransform = childG.getTransform();
                final InterfaceBwdFont initialFont = childG.getFont();

                final InterfaceBwdFont otherFont =
                        getBinding().getFontHome().newFontWithSize(
                                initialFont.kind(),
                                initialFont.size() + 1);

                childG.setArgb32(Argb32.inverted(initialArgb32));
                childG.addClipInUser(initialClip.withSpans(1, 1));
                childG.setTransform(initialTransform.inverted());
                childG.setFont(otherFont);
                
                childG.reset();
                
                checkEqualArgb32(initialArgb32, childG.getArgb32());
                checkEqual(initialClip, childG.getClipInUser());
                checkEqual(initialTransform, childG.getTransform());
                checkEqual(initialFont, childG.getFont());
                
                otherFont.dispose();
            } finally {
                childG.finish();
            }
        }
        
        /*
         * IllegalStateException,
         * typically when not called between init() and finish().
         * 
         * Not bothering to test callability between init() and finish(),
         * as our set of functionnal tests already take care of that.
         */
        
        {
            final InterfaceBwdFont font = g.getFont();
            final double startDeg = 123.456;
            final double spanDeg = 78.9;
            final int[] xArr = new int[]{1, 2, 3};
            final int[] yArr = new int[]{4, 5, 6};
            final int pointCount = 3;
            final String text = "text";
            final InterfaceBwdImage image = getBinding().newImage(BwdTestResources.TEST_IMG_FILE_PATH_MOUSE_HEAD_PNG);
            
            final InterfaceBwdGraphics childG = g.newChildGraphics(box);
            // First round: calls before init().
            // Second round: calls after finish().
            for (int k = 0; k < 2; k++) {
                final boolean beforeInitElseAfterFinish = (k == 0);
                if (beforeInitElseAfterFinish) {
                    childG.newChildGraphics(box);
                } else {
                    try {
                        childG.newChildGraphics(box);
                        fail();
                    } catch (IllegalStateException ok) {}
                }
                /*
                 * 
                 */
                try {
                    childG.reset();
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.addClipInBase(box);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.addClipInUser(box);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.removeLastAddedClip();
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.removeAllAddedClips();
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.setTransform(GTransform.IDENTITY);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.addTransform(GTransform.IDENTITY);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.removeLastAddedTransform();
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.removeAllAddedTransforms();
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.setColor(BwdColor.BLACK);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.setArgb64(0L);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.setArgb32(0);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.setFont(font);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.clearRect(0, 0, 1, 1);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.clearRect(GRect.DEFAULT_HUGE);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.drawPoint(0, 0);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.drawLine(0, 0, 0, 0);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.drawLineStipple(
                            0, 0, 0, 0,
                            1, GprimUtils.PLAIN_PATTERN, 0);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.drawRect(0, 0, 0, 0);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.drawRect(GRect.DEFAULT_HUGE);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.fillRect(0, 0, 0, 0);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.fillRect(GRect.DEFAULT_HUGE);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.drawOval(0, 0, 0, 0);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.drawOval(GRect.DEFAULT_HUGE);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.fillOval(0, 0, 0, 0);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.fillOval(GRect.DEFAULT_HUGE);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.drawArc(0, 0, 0, 0, startDeg, spanDeg);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.drawArc(GRect.DEFAULT_HUGE, startDeg, spanDeg);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.fillArc(0, 0, 0, 0, startDeg, spanDeg);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.fillArc(GRect.DEFAULT_HUGE, startDeg, spanDeg);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.drawPolyline(xArr, yArr, pointCount);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.drawPolygon(xArr, yArr, pointCount);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.fillPolygon(xArr, yArr, pointCount);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.drawText(0, 0, text);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.drawImage(0, 0, image);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.drawImage(0, 0, 100, 100, image);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.drawImage(GRect.valueOf(0, 0, 100, 100), image);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.drawImage(0, 0, 100, 100, image, 0, 0, 10, 10);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.drawImage(GRect.valueOf(0, 0, 100, 100), image, GRect.valueOf(0, 0, 10, 10));
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                try {
                    childG.getArgb64At(0, 0);
                    fail();
                } catch (IllegalStateException ok) {}
                try {
                    childG.getArgb32At(0, 0);
                    fail();
                } catch (IllegalStateException ok) {}
                /*
                 * 
                 */
                if (k == 0) {
                    childG.init();
                    childG.finish();
                }
            }
            
            image.dispose();
        }
    }
    
    /*
     * CLIPS
     */
    
    private void test_clips(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_clips(...)");
        }
        
        final GRect box = g.getBox();
        
        final GRect initialClipInBase = g.getInitialClipInBase();
        
        /*
         * NullPointerException
         */
        
        try {
            g.addClipInBase(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            g.addClipInUser(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        /*
         * Invariants.
         */
        
        checkContains(box, initialClipInBase);
        checkContains(initialClipInBase, g.getClipInBase());
        
        /*
         * Empty or not.
         */
        
        {
            checkFalse(g.isClipEmpty());
            g.addClipInUser(GRect.DEFAULT_EMPTY);
            checkTrue(g.isClipEmpty());
            g.removeLastAddedClip();
        }
        
        /*
         * Adds and removals.
         */
        
        {
            final GRect clip0 = g.getClipInBase();
            GRect oldClip = clip0;
            for (int i = 0; i < 10; i++) {
                // Shrinking.
                final GRect addedClip = oldClip.withPosDeltas(1, 1);
                g.addClipInBase(addedClip);
                final GRect expectedClip = oldClip.intersected(addedClip);

                final GRect actualClip = g.getClipInBase();
                checkEqual(expectedClip, actualClip);
                
                oldClip = actualClip;
            }
            for (int i = 0; i < 5; i++) {
                // Growing.
                g.removeLastAddedClip();
                final GRect expectedClip = oldClip.withBordersDeltas(-1, -1, 0, 0);
                
                final GRect actualClip = g.getClipInBase();
                checkEqual(expectedClip, actualClip);
                
                oldClip = actualClip;
            }
            {
                // Growing.
                g.removeAllAddedClips();
                final GRect expectedClip = oldClip.withBordersDeltas(-5, -5, 0, 0);
                
                final GRect actualClip = g.getClipInBase();
                checkEqual(expectedClip, actualClip);
                
                oldClip = actualClip;
            }
        }
        
        /*
         * Checking that clip add counts whatever the clip.
         */
        
        {
            final GRect clip0 = g.getClipInBase();
            final GRect clip1 = clip0.withPosDeltas(1, 1);
            final GRect clip1And0 = clip1.intersected(clip0);
            final GRect clipEmptyCustom = clip1And0.withSpans(clip1And0.xSpan(), 0);

            g.addClipInBase(clip1);
            // Adding same.
            g.addClipInBase(clip1);
            // Adding equals.
            g.addClipInBase(GRect.valueOf(clip1.x(), clip1.y(), clip1.xSpan(), clip1.ySpan()));
            // Adding containing small.
            g.addClipInBase(clip0);
            // Adding containing huge.
            g.addClipInBase(GRect.DEFAULT_HUGE);
            // Adding empty custom.
            g.addClipInBase(clipEmptyCustom);
            // Adding empty default.
            g.addClipInBase(GRect.DEFAULT_EMPTY);
            
            // Our bindings preserve information of empty clips.
            // Empty custom's position preserved,
            // but spans were zeroized by EMPTY_DEFAULT.
            checkEqual(clipEmptyCustom.withSpans(0, 0), g.getClipInBase());
            
            // Removing empty default.
            g.removeLastAddedClip();
            // Our bindings preserve information of empty clips.
            checkEqual(clipEmptyCustom, g.getClipInBase());
            // Removing empty custom.
            g.removeLastAddedClip();
            checkEqual(clip1And0, g.getClipInBase());
            // Removing containing huge.
            g.removeLastAddedClip();
            checkEqual(clip1And0, g.getClipInBase());
            // Removing containing small.
            g.removeLastAddedClip();
            checkEqual(clip1And0, g.getClipInBase());
            // Removing equals.
            g.removeLastAddedClip();
            checkEqual(clip1And0, g.getClipInBase());
            // Removing same.
            g.removeLastAddedClip();
            checkEqual(clip1And0, g.getClipInBase());
            // Removing first.
            g.removeLastAddedClip();
            checkEqual(clip0, g.getClipInBase());
        }
        
        /*
         * Clips transforms.
         */
        
        {
            g.addClipInBase(initialClipInBase.withPosDeltas(
                    initialClipInBase.xSpan()/2,
                    initialClipInBase.ySpan()/2));
            final GRect userClipInBase = g.getClipInBase();
            for (GRotation rotation : GRotation.values()) {
                final GTransform transform = GTransform.valueOf(
                        rotation,
                        rotation.ordinal(),
                        2 * rotation.ordinal());
                g.setTransform(transform);
                
                checkEqual(initialClipInBase, g.getInitialClipInBase());
                checkEqual(userClipInBase, g.getClipInBase());
                
                checkConsistent(initialClipInBase, transform, g.getInitialClipInUser());
                checkConsistent(userClipInBase, transform, g.getClipInUser());
            }
        }
    }

    /*
     * TRANSFORMS
     */
    
    private void test_transfoms(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_transfoms(...)");
        }
        
        checkEqual(GTransform.IDENTITY, g.getTransform());
        
        /*
         * NullPointerException
         */
        
        try {
            g.setTransform(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        // Not changed.
        checkEqual(GTransform.IDENTITY, g.getTransform());
        
        /*
         * 
         */
        
        // Transforms we play with.
        final GTransform transform0 = GTransform.valueOf(GRotation.ROT_90, 5, 7);
        final GTransform added1 = GTransform.valueOf(GRotation.ROT_0, 11, 17);
        final GTransform transform1 = GTransform.valueOf(GRotation.ROT_90, 5-17, 7+11);
        final GTransform added2 = GTransform.valueOf(GRotation.ROT_90, 0, 0);
        final GTransform transform2 = GTransform.valueOf(GRotation.ROT_180, 5-17, 7+11);
        final GTransform added3 = GTransform.valueOf(GRotation.ROT_270, 0, 0);
        final GTransform transform3 = GTransform.valueOf(GRotation.ROT_90, 5-17, 7+11);
        
        // Setting the transform.
        g.setTransform(transform0);
        
        checkEqual(transform0, g.getTransform());
        // Could be a different one, if identical,
        // but here we know it changed in which case
        // our bindings use the specified one.
        if (g.getTransform() != transform0) {
            fail();
        }
        
        // Adding first.
        g.addTransform(added1);
        checkEqual(transform1, g.getTransform());
        
        // Adding a second one.
        g.addTransform(added2);
        checkEqual(transform2, g.getTransform());
        
        // Adding a third one.
        g.addTransform(added3);
        checkEqual(transform3, g.getTransform());
        
        // Removing third one.
        g.removeLastAddedTransform();
        checkEqual(transform2, g.getTransform());
        
        // Removing first and second ones.
        g.removeAllAddedTransforms();
        checkEqual(transform0, g.getTransform());
        
        /*
         * Checking that transform add counts whatever the transform.
         */
        
        final GTransform tr_0_1 = transform0.composed(added1);
        final GTransform tr_0_1P2 = tr_0_1.composed(added1);
        final GTransform tr_0_1P3 = tr_0_1P2.composed(added1);
        
        g.addTransform(added1);
        // Adding same.
        g.addTransform(added1);
        // Adding equals.
        g.addTransform(GTransform.valueOf(added1.rotation(), added1.frame2XIn1(), added1.frame2YIn1()));
        // Adding identity.
        g.addTransform(GTransform.IDENTITY);
        checkEqual(tr_0_1P3, g.getTransform());
        
        // Removing identity.
        g.removeLastAddedTransform();
        checkEqual(tr_0_1P3, g.getTransform());
        // Removing equals.
        g.removeLastAddedTransform();
        checkEqual(tr_0_1P2, g.getTransform());
        // Removing same.
        g.removeLastAddedTransform();
        checkEqual(tr_0_1, g.getTransform());
        // Removing the first added.
        g.removeLastAddedTransform();
        checkEqual(transform0, g.getTransform());
    }
    
    /*
     * COLORS
     */
    
    private void test_colors(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_colors(...)");
        }
        
        checkEqual(BwdColor.BLACK, g.getColor());
        
        /*
         * NullPointerException
         */
        
        try {
            g.setColor(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        // Not changed.
        checkEqual(BwdColor.BLACK, g.getColor());
        
        /*
         * 
         */
        
        final BwdColor c1 = BwdColor.valueOfArgb64(0x123456789ABCDEF0L);
        final BwdColor c2 = BwdColor.valueOfArgb64(0x0102030405060708L);
        final BwdColor c3 = BwdColor.TRANSPARENT;
        // Ending with black for safety.
        final BwdColor c4 = BwdColor.BLACK;
        final List<BwdColor> cList = Arrays.asList(c1, c2, c3, c4);
        
        for (BwdColor c : cList) {
            final int argb32 = c.toArgb32();
            final long argb64 = c.toArgb64();
            
            g.setColor(c);
            checkEqual(c, g.getColor());
            checkEqualArgb32(argb32, g.getArgb32());
            checkEqualArgb64(argb64, g.getArgb64());
            
            g.setArgb32(argb32);
            checkEqual(BwdColor.valueOfArgb32(argb32), g.getColor());
            checkEqualArgb32(argb32, g.getArgb32());
            checkEqualArgb64(Argb3264.toArgb64(argb32), g.getArgb64());
            
            g.setArgb64(argb64);
            checkEqual(c, g.getColor());
            checkEqualArgb32(argb32, g.getArgb32());
            checkEqualArgb64(argb64, g.getArgb64());
        }
    }
    
    /*
     * FONTS
     */
    
    private void test_fonts(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_fonts(...)");
        }
        
        final InterfaceBwdFontHome fontHome = this.getBinding().getFontHome();
        final InterfaceBwdFont defaultFont = fontHome.getDefaultFont();
        
        InterfaceBwdFont font = g.getFont();
        checkSame(defaultFont, font);
        
        /*
         * NullPointerException
         */
        
        try {
            g.setFont(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        // Not changed.
        font = g.getFont();
        checkSame(defaultFont, font);
        
        /*
         * 
         */
        
        final InterfaceBwdFont createdFont = fontHome.newFontWithSize(
                defaultFont.size() + 1);
        g.setFont(createdFont);
        checkSame(createdFont, g.getFont());
    }

    /*
     * PRIMITIVES
     */

    private void test_primitives(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_primitives(...)");
        }
        
        test_primitives_points(g);
        test_primitives_lines(g);
        test_primitives_rects(g);
        test_primitives_ovals(g);
        test_primitives_arcs(g);
        test_primitives_polygons(g);
    }

    private void test_primitives_points(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_primitives_points(...)");
        }
        
        for (int x : POS_ANY_ARR) {
            for (int y : POS_ANY_ARR) {
                g.drawPoint(x, y);
            }
        }
    }
    
    private void test_primitives_lines(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_primitives_lines(...)");
        }
        
        final short pattern = (short) 0x81;
        
        /*
         * IllegalArgumentException
         */
        
        for (int badFactor : new int[] {
                MIN, -1, 0,
                //
                257, MAX
        }) {
            try {
                g.drawLineStipple(
                        1, 2, 3, 4,
                        badFactor, pattern, 0);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        for (int badPixelNum : new int[] {
                MIN, -1
        }) {
            try {
                g.drawLineStipple(
                        1, 2, 3, 4,
                        1, pattern, badPixelNum);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        
        /*
         * 
         */
        
        {
            int pixelNum = 0;
            for (int factor : new int[] {1, 256}) {
                pixelNum = g.drawLineStipple(
                        1, 2, 300, 400,
                        factor, pattern, pixelNum);
                if (pixelNum < 0) {
                    throw new AssertionError("returned pixelNum must be >= 0");
                }
                
                pixelNum = MAX;
                pixelNum = g.drawLineStipple(
                        1, 2, 300, 400,
                        factor, pattern, pixelNum);
                if (pixelNum < 0) {
                    throw new AssertionError("returned pixelNum must be >= 0");
                }
            }
        }
        
        /*
         * 
         */
        
        {
            int pixelNum = 0;
            for (int x : POS_ANY_ARR) {
                for (int y : POS_ANY_ARR) {
                    final int x2 = -y;
                    final int y2 = -x;
                    
                    g.drawLine(x, y, x2, y2);
                    
                    // Sometimes 1, sometimes max.
                    final int factor = ((x < 0) ? 1 : 256);
                    pixelNum = g.drawLineStipple(
                            x, y, x2, y2,
                            factor, pattern, pixelNum);
                    if (pixelNum < 0) {
                        throw new AssertionError("returned pixelNum must be >= 0");
                    }
                }
            }
        }
    }
    
    private void test_primitives_rects(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_primitives_rects(...)");
        }
        
        /*
         * NullPointerException
         */
        
        try {
            g.drawRect(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            g.fillRect(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        /*
         * 
         */
        
        for (int xSpan : SPAN_ANY_ARR) {
            for (int ySpan : SPAN_ANY_ARR) {
                g.drawRect(0, 0, xSpan, ySpan);
                g.fillRect(0, 0, xSpan, ySpan);
            }
        }
        
        for (GRect rect : RECT_ANY_LIST) {
            g.drawRect(rect);
            g.fillRect(rect);
        }
    }
    
    private void test_primitives_ovals(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_primitives_ovals(...)");
        }
        
        /*
         * NullPointerException
         */
        
        try {
            g.drawOval(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            g.fillOval(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        /*
         * Clip starting as (0,0), these should cause huge oval curve
         * to (potentially) pass within clip, and cause usage of
         * huge-specific algorithm, instead of taking ages
         * using regular algorithm.
         */
        
        {
            final int span = MAX;
            
            callDrawOval(g, -span/2, 0, span, span);
            callDrawOval(g, 0, -span/2, span, span);
            callFillOval(g, -span/2, 0, span, span);
            callFillOval(g, 0, -span/2, span, span);
        }

        /*
         * 
         */
        
        for (int xSpan : SPAN_ANY_ARR) {
            for (int ySpan : SPAN_ANY_ARR) {
                callDrawOval(g, 0, 0, xSpan, ySpan);
                callFillOval(g, 0, 0, xSpan, ySpan);
            }
        }
        
        for (GRect rect : RECT_ANY_LIST) {
            callDrawOval(g, rect);
            callFillOval(g, rect);
        }
    }
    
    private void test_primitives_arcs(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_primitives_arcs(...)");
        }
        
        final double startDeg = 123.456;
        final double spanDeg = 78.9;
        
        /*
         * NullPointerException
         */
        
        try {
            g.drawArc(null, startDeg, spanDeg);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            g.fillArc(null, startDeg, spanDeg);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        /*
         * IllegalArgumentException
         */

        for (double badStartDeg : BAD_ANG_ARR) {
            try {
                g.drawArc(0, 0, 10, 10, badStartDeg, spanDeg);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
            try {
                g.fillArc(0, 0, 10, 10, badStartDeg, spanDeg);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        for (double badSpanDeg : BAD_ANG_ARR) {
            try {
                g.drawArc(0, 0, 10, 10, startDeg, badSpanDeg);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
            try {
                g.fillArc(0, 0, 10, 10, startDeg, badSpanDeg);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        /*
         * Clip starting as (0,0), these should cause huge oval curve
         * to (potentially) pass within clip, and cause usage of
         * huge-specific algorithm, instead of taking ages
         * using regular algorithm.
         */
        
        {
            final int span = MAX;
            
            callDrawArc(g, -span/2, 0, span, span, startDeg, spanDeg);
            callDrawArc(g, 0, -span/2, span, span, startDeg, spanDeg);
            callFillArc(g, -span/2, 0, span, span, startDeg, spanDeg);
            callFillArc(g, 0, -span/2, span, span, startDeg, spanDeg);
        }

        /*
         * 
         */
        
        for (int xSpan : SPAN_ANY_ARR) {
            for (int ySpan : SPAN_ANY_ARR) {
                callDrawArc(g, 0, 0, xSpan, ySpan, startDeg, spanDeg);
                callFillArc(g, 0, 0, xSpan, ySpan, startDeg, spanDeg);
            }
        }
        
        for (GRect rect : RECT_ANY_LIST) {
            callDrawArc(g, rect, startDeg, spanDeg);
            callFillArc(g, rect, startDeg, spanDeg);
        }
    }
    
    private void test_primitives_polygons(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_primitives_polygons(...)");
        }
        
        final int[] xArr = new int[] {1, 2, 3};
        final int[] yArr = new int[]{4, 5, 6};
        final int pointCount = 3;

        /*
         * NullPointerException
         */
        
        try {
            g.drawPolyline(null, yArr, pointCount);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            g.drawPolygon(null, yArr, pointCount);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            g.fillPolygon(null, yArr, pointCount);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        try {
            g.drawPolyline(xArr, null, pointCount);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            g.drawPolygon(xArr, null, pointCount);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        try {
            g.fillPolygon(xArr, null, pointCount);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        /*
         * IllegalArgumentException
         */

        for (int badPointCount : BAD_POINT_COUNT) {
            try {
                g.drawPolyline(xArr, yArr, badPointCount);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
            try {
                g.drawPolygon(xArr, yArr, badPointCount);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
            try {
                g.fillPolygon(xArr, yArr, badPointCount);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }

        /*
         * Huge polygon: must not blow up,
         * with appropriate usage of clip.
         */

        callDrawPolygon(
                g,
                new int[] {MIN, MAX, MAX, MIN},
                new int[] {MIN, MIN, MAX, MAX},
                4);
        callFillPolygon(
                g,
                new int[] {MIN, MAX, MAX, MIN},
                new int[] {MIN, MIN, MAX, MAX},
                4);
    }

    /*
     * TEXT
     */
    
    private void test_text(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_text(...)");
        }
        
        /*
         * NullPointerException
         */
        
        try {
            g.drawText(0, 0, null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
        
        /*
         * Basic characters in [0,127].
         */
        
        {
            final StringBuilder sb = new StringBuilder();

            for (int c : new int[]{
                    BwdUnicode.HT,
                    BwdUnicode.SPACE,
                    BwdUnicode.EXCLAMATION_MARK,
                    BwdUnicode.DOUBLE_QUOTE,
                    BwdUnicode.HASH,
                    BwdUnicode.DOLLAR,
                    BwdUnicode.PERCENT,
                    BwdUnicode.AMPERSAND,
                    BwdUnicode.SINGLE_QUOTE,
                    BwdUnicode.LEFT_PARENTHESIS,
                    BwdUnicode.RIGHT_PARENTHESIS,
                    BwdUnicode.ASTERISK,
                    BwdUnicode.PLUS,
                    BwdUnicode.COMMA,
                    BwdUnicode.MINUS,
                    BwdUnicode.DOT,
                    BwdUnicode.SLASH,
            }) {
                sb.append((char) c);
            }
            
            for (int c = BwdUnicode.ZERO; c <= BwdUnicode.NINE; c++) {
                sb.append((char) c);
            }
            
            for (int c : new int[]{
                    BwdUnicode.COLON,
                    BwdUnicode.SEMICOLON,
                    BwdUnicode.LESS_THAN,
                    BwdUnicode.EQUALS,
                    BwdUnicode.GREATER_THAN,
                    BwdUnicode.QUESTION_MARK,
                    BwdUnicode.AT_SYMBOL,
            }) {
                sb.append((char) c);
            }
            
            for (int c = BwdUnicode.A; c <= BwdUnicode.Z; c++) {
                sb.append((char) c);
            }
            
            for (int c : new int[]{
                    BwdUnicode.LEFT_BRACKET,
                    BwdUnicode.BACKSLASH,
                    BwdUnicode.RIGHT_BRACKET,
                    BwdUnicode.CIRCUMFLEX_ACCENT,
                    BwdUnicode.UNDERSCORE,
                    BwdUnicode.GRAVE_ACCENT,
            }) {
                sb.append((char) c);
            }

            for (int c = BwdUnicode.a; c <= BwdUnicode.z; c++) {
                sb.append((char) c);
            }
            
            for (int c : new int[]{
                    BwdUnicode.LEFT_BRACE,
                    BwdUnicode.VERTICAL_BAR,
                    BwdUnicode.RIGHT_BRACE,
                    BwdUnicode.TILDE,
            }) {
                sb.append((char) c);
            }
            
            for (int c : new int[]{
                    // LF
                    BwdUnicode.LF,
                    // CR
                    BwdUnicode.CR,
                    // CR+LF
                    BwdUnicode.CR,
                    BwdUnicode.LF,
            }) {
                sb.append((char) c);
            }

            final String text = sb.toString();
            {
                g.drawText(0, 0, text);
            }
            
            for (char c : text.toCharArray()) {
                final String cStr = String.valueOf(c);
                g.drawText(0, 0, cStr);
            }
        }
    }
    
    /*
     * IMAGES
     */
    
    
    private void test_images(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_images(...)");
        }
        
        for (boolean isWritableImage : new boolean[] {false, true}) {
            final InterfaceBwdImage image;
            if (isWritableImage) {
                final int width = 200;
                final int height = 100;
                image = getBinding().newWritableImage(width, height);
            } else {
                image = getBinding().newImage(BwdTestResources.TEST_IMG_FILE_PATH_MOUSE_HEAD_PNG);
            }
            
            /*
             * NullPointerException
             */

            try {
                g.drawImage(0, 0, null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }

            try {
                g.drawImage(0, 0, 0, 0, null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }

            try {
                g.drawImage(null, image);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
            try {
                g.drawImage(GRect.DEFAULT_EMPTY, null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }

            try {
                g.drawImage(0, 0, 0, 0, null, 0, 0, 0, 0);
                fail();
            } catch (NullPointerException e) {
                // ok
            }

            try {
                g.drawImage(null, image, GRect.DEFAULT_EMPTY);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
            try {
                g.drawImage(GRect.DEFAULT_EMPTY, null, GRect.DEFAULT_EMPTY);
                fail();
            } catch (NullPointerException e) {
                // ok
            }
            try {
                g.drawImage(GRect.DEFAULT_EMPTY, image, null);
                fail();
            } catch (NullPointerException e) {
                // ok
            }

            /*
             * 
             */

            g.drawImage(0, 0, image);

            g.drawImage(0, 0, 100, 100, image);
            g.drawImage(GRect.valueOf(0, 0, 100, 100), image);

            g.drawImage(0, 0, 100, 100, image, 0, 0, 10, 10);
            g.drawImage(GRect.valueOf(0, 0, 100, 100), image, GRect.valueOf(0, 0, 10, 10));
            
            /*
             * Can't draw a writable image in itself.
             */
            
            if (isWritableImage) {
                final InterfaceBwdWritableImage wi =
                        (InterfaceBwdWritableImage) image;
                final InterfaceBwdGraphics wig = wi.getGraphics();
                wig.init();
                try {
                    try {
                        wig.drawImage(0, 0, image);
                        fail();
                    } catch (IllegalArgumentException e) {
                        // ok
                    }
                    try {
                        wig.drawImage(0, 0, 100, 100, image);
                        fail();
                    } catch (IllegalArgumentException e) {
                        // ok
                    }
                    try {
                        wig.drawImage(GRect.valueOf(0, 0, 100, 100), image);
                        fail();
                    } catch (IllegalArgumentException e) {
                        // ok
                    }
                    try {
                        wig.drawImage(
                                0, 0, 100, 100,
                                image,
                                0, 0, 10, 10);
                        fail();
                    } catch (IllegalArgumentException e) {
                        // ok
                    }
                    try {
                        wig.drawImage(
                                GRect.valueOf(0, 0, 100, 100),
                                image,
                                GRect.valueOf(0, 0, 10, 10));
                        fail();
                    } catch (IllegalArgumentException e) {
                        // ok
                    }
                } finally {
                    wig.finish();
                }
            }

            /*
             * Disposing image.
             */

            image.dispose();

            /*
             * IllegalArgumentException
             */

            try {
                g.drawImage(0, 0, image);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }

            try {
                g.drawImage(0, 0, 100, 100, image);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
            try {
                g.drawImage(GRect.valueOf(0, 0, 100, 100), image);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }

            try {
                g.drawImage(
                        0, 0, 100, 100,
                        image,
                        0, 0, 10, 10);
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
            try {
                g.drawImage(
                        GRect.valueOf(0, 0, 100, 100),
                        image,
                        GRect.valueOf(0, 0, 10, 10));
                fail();
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
    }

    /*
     * COLORS_REWORK
     */
    
    
    private void test_colors_rework(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_colors_rework(...)");
        }
        
        /*
         * NullPointerException
         */
        
        try {
            g.flipColors(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        /*
         * 
         */
        
        for (int xSpan : SPAN_ANY_ARR) {
            for (int ySpan : SPAN_ANY_ARR) {
                g.flipColors(0, 0, xSpan, ySpan);
            }
        }
        
        for (GRect rect : RECT_ANY_LIST) {
            g.flipColors(rect.x(), rect.y(), rect.xSpan(), rect.ySpan());
            g.flipColors(rect);
        }
    }

    /*
     * COLORS_READING
     */
    
    private void test_colors_reading(InterfaceBwdGraphics g) {
        if (DEBUG) {
            System.out.println("test_colors_reading(...)");
        }

        final GRect clip = g.getInitialClipInUser();
        
        for (int x : POS_ANY_ARR) {
            for (int y : POS_ANY_ARR) {
                if (clip.contains(x, y)) {
                    g.getArgb64At(x, y);
                    g.getArgb32At(x, y);
                } else {
                    try {
                        g.getArgb64At(x, y);
                        fail();
                    } catch (IllegalArgumentException e) {
                        // ok
                    }
                    try {
                        g.getArgb32At(x, y);
                        fail();
                    } catch (IllegalArgumentException e) {
                        // ok
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    private static void callDrawOval(
            InterfaceBwdGraphics g,
            GRect oval) {
        callDrawOval(
                g,
                oval.x(), oval.y(), oval.xSpan(), oval.ySpan());
    }
    
    private static void callDrawOval(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan) {
        if (DEBUG) {
            Dbg.log("calling drawOval("
                    + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ")...");
        }
        g.drawOval(x, y, xSpan, ySpan);
        if (DEBUG) {
            Dbg.log("...done");
        }
    }
    
    private static void callFillOval(
            InterfaceBwdGraphics g,
            GRect oval) {
        callFillOval(
                g,
                oval.x(), oval.y(), oval.xSpan(), oval.ySpan());
    }
    
    private static void callFillOval(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan) {
        if (DEBUG) {
            Dbg.log("calling fillOval("
                    + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ")...");
        }
        g.fillOval(x, y, xSpan, ySpan);
        if (DEBUG) {
            Dbg.log("...done");
        }
    }
    
    private static void callDrawArc(
            InterfaceBwdGraphics g,
            GRect oval,
            double startDeg, double spanDeg) {
        callDrawArc(
                g,
                oval.x(), oval.y(), oval.xSpan(), oval.ySpan(),
                startDeg, spanDeg);
    }
    
    private static void callDrawArc(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        if (DEBUG) {
            Dbg.log("calling drawArc("
                    + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + startDeg + ", " + spanDeg
                    + ")...");
        }
        g.drawArc(
                x, y, xSpan, ySpan,
                startDeg, spanDeg);
        if (DEBUG) {
            Dbg.log("...done");
        }
    }
    
    private static void callFillArc(
            InterfaceBwdGraphics g,
            GRect oval,
            double startDeg, double spanDeg) {
        callFillArc(
                g,
                oval.x(), oval.y(), oval.xSpan(), oval.ySpan(),
                startDeg, spanDeg);
    }
    
    private static void callFillArc(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        if (DEBUG) {
            Dbg.log("calling fillArc("
                    + x + ", " + y + ", " + xSpan + ", " + ySpan
                    + ", " + startDeg + ", " + spanDeg
                    + ")...");
        }
        g.fillArc(
                x, y, xSpan, ySpan,
                startDeg, spanDeg);
        if (DEBUG) {
            Dbg.log("...done");
        }
    }

    private static void callDrawPolygon(
            InterfaceBwdGraphics g,
            int[] xArr,
            int[] yArr,
            int pointCount) {
        if (DEBUG) {
            Dbg.log("calling drawPolygon(" + Arrays.toString(xArr)
            + ", " + Arrays.toString(yArr)
            + ", " + pointCount
            + ")...");
        }
        g.drawPolygon(xArr, yArr, pointCount);
        if (DEBUG) {
            Dbg.log("...done");
        }
    }
    
    private static void callFillPolygon(
            InterfaceBwdGraphics g,
            int[] xArr,
            int[] yArr,
            int pointCount) {
        if (DEBUG) {
            Dbg.log("calling fillPolygon(" + Arrays.toString(xArr)
            + ", " + Arrays.toString(yArr)
            + ", " + pointCount
            + ")...");
        }
        g.fillPolygon(xArr, yArr, pointCount);
        if (DEBUG) {
            Dbg.log("...done");
        }
    }
    
    /*
     * 
     */
    
    private static void fail() {
        throw new AssertionError();
    }
    
    private static void checkSame(Object expected, Object actual) {
        if (actual != expected) {
            throw new AssertionError(
                    "expected " + expected + " (" + System.identityHashCode(expected) + ")"
                    + ", got " + actual + " (" + System.identityHashCode(actual) + ")");
        }
    }

    private static void checkTrue(boolean actual) {
        final boolean expected = true;
        checkEqual(expected, actual);
    }
    
    private static void checkFalse(boolean actual) {
        final boolean expected = false;
        checkEqual(expected, actual);
    }
    
    private static void checkEqual(Object expected, Object actual) {
        if (!actual.equals(expected)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }

    private static void checkEqualArgb32(int expected, int actual) {
        checkEqual(Argb32.toString(expected), Argb32.toString(actual));
    }

    private static void checkEqualArgb64(long expected, long actual) {
        checkEqual(Argb64.toString(expected), Argb64.toString(actual));
    }

    private static void checkContains(GRect container, GRect contained) {
        if (!container.contains(contained)) {
            throw new AssertionError(contained + " not contained in " + container);
        }
    }
    
    private static void checkConsistent(
            GRect rectInBase,
            GTransform transform,
            GRect rectInUser) {
        final GRect expectedRectInUser = transform.rectIn2(rectInBase);
        if (!rectInUser.equals(expectedRectInUser)) {
            throw new AssertionError(
                    "expected " + expectedRectInUser
                    + ", got " + rectInUser
                    + ", transform = " + transform);
        }
    }
}
