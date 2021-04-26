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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.ext.drag.AbstractDragController;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.cases.utils.fractals.FracColoring;
import net.jolikit.bwd.test.cases.utils.fractals.FracPoint;
import net.jolikit.bwd.test.cases.utils.fractals.FracRect;
import net.jolikit.bwd.test.cases.utils.fractals.FracView;
import net.jolikit.bwd.test.cases.utils.fractals.FracColorComputer;
import net.jolikit.bwd.test.cases.utils.fractals.MandDrawer;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplitmergable;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.hard.HardScheduler;

/**
 * Tests doing a typical usage of writable images:
 * to draw something costly in the background, possibly in parallel,
 * and then draw it fast on client graphics whenever needed.
 * 
 * Using Mandelbrot fractal as an obvious case of something not computed
 * every time client is painted (when just using CPU like us).
 * 
 * We deliberately compute iterations in the same thread(s) that we
 * compute colors and build the writable image in, to allow to make it obvious
 * (i.e. slow, with MUST_ALWAYS_COMPUTE_IMAGE_IN_BG_THREAD set to false)
 * when writable images can't safely be drawn (primitives, text, images)
 * from other threads than the UI thread.
 */
public class MandelbrotBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /**
     * Dividing by two, to avoid taking the machine too much on its knees
     * in case of hyper-threading (availableProcessors() takes hyper threading
     * into account, i.e. might return more parallelism than actually possible).
     */
    private static final int TARGET_PARALLELISM = Runtime.getRuntime().availableProcessors() / 2;

    /**
     * For our bindings, the only reason why we might not be able
     * to draw on writable images in a BG thread is because
     * of text drawing limitations, but we don't draw text on our image
     * so we can do that.
     * We still limit ourselves to sequential painting when parallel painting
     * is not fully supported by the binding, even if it would be possible
     * for our mere drawPoint() usage.
     */
    private static final boolean MUST_ALWAYS_COMPUTE_IMAGE_IN_BG_THREAD = true;

    /*
     * 
     */

    /**
     * Coloring logic for when iter is in [1, maxIter - 1]
     * (for iter = 0, color is always fully transparent).
     */
    private static final FracColoring DEFAULT_COLORING = FracColoring.CYCLIC_RGB;

    /*
     * 
     */

    private static final int MIN_MAX_ITER = 1;

    private static final int MAX_MAX_ITER = 2000;
    
    private static final int DEFAULT_MAX_ITER = 500;

    /*
     * 
     */

    /**
     * Starts to get wrong around that, due to double precision.
     */
    private static final double MIN_SCALE = 1.0/(1L << 53);

    /**
     * Else too easy to loose the fractal from sight.
     */
    private static final int MAX_INITIAL_SCALE_MULTIPLE = 2;

    /**
     * For multiplication/division to be exact,
     * not to damage scale mantissa,
     * and for dezooming to be exact the inverse of zooming.
     */
    private static final double ZOOM_GROWTH_FACTOR = 2.0;
    
    /*
     * 
     */

    private static final FracRect DEFAULT_FRACTAL_RECT = new FracRect(-2.0, -2.0, 2.0, 2.0);

    /**
     * Rectangles with strictly inferior area are not split.
     * 
     * Get speedup from an order of magnitude of 100, but using two orders above
     * not to create a huge amount of split task (could spam memory and logs).
     */
    private static final int MIN_GRECT_AREA_FOR_SPLIT = 10 * 1000;

    /*
     * 
     */

    /**
     * Color for when iter = maxIter (i.e. in the fractal).
     */
    private static final BwdColor COLOR_FOR_MAX_ITER = BwdColor.BLACK;

    private static final BwdColor BG_COLOR = BwdColor.DARKGOLDENROD;

    private static final BwdColor TEXT_BG_COLOR = BwdColor.BLACK;
    private static final BwdColor TEXT_FG_COLOR = BwdColor.WHITE;

    /*
     * 
     */

    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 800;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private class MyBgProcess extends AbstractProcess {
        private int offscreenImageNum = 0;
        public MyBgProcess(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {
            // Once started, just running once.
            this.stop();

            /*
             * Need to read inputId before reading actual inputs,
             * else might assume old inputs correspond to new id.
             */
            
            final int inputId = lastInputIdRef.get();
            
            /*
             * These volatile values must be retrieved after stop(),
             * since isStarted() is checked after modifying them,
             * for eventual restart.
             */
            
            final GPoint spans = targetSpans;
            final FracView view = targetView;
            final int maxIter = targetMaxIter;
            final FracColoring coloring = targetColoring;
            final int colorIterPeriod = targetColorIterPeriod;

            final int newImgNum = this.offscreenImageNum + 1;

            final InterfaceBwdWritableImage newWi = newDrawnWritableImage(
                    inputId,
                    spans,
                    view,
                    maxIter,
                    coloring,
                    colorIterPeriod,
                    newImgNum);
            if (newWi == null) {
                /*
                 * Aborted.
                 * maxIter and image num not modified.
                 */
            } else {
                this.offscreenImageNum = newImgNum;

                final MyImgData newImgData = new MyImgData(
                        newWi,
                        view,
                        newImgNum);
                synchronized (imgDataToUseList) {
                    imgDataToUseList.add(newImgData);
                }

                // For it to be displayed.
                getHost().ensurePendingClientPainting();
            }

            return 0;
        }
    }

    private static class MyImgData {
        final InterfaceBwdWritableImage image;
        final FracView view;
        final int imageNum;
        public MyImgData(
                InterfaceBwdWritableImage image,
                FracView view,
                int imageNum) {
            this.image = image;
            this.view = view;
            this.imageNum = imageNum;
        }
    }

    /**
     * To drag fractal image in client.
     */
    private class MyDragController extends AbstractDragController {
        @Override
        protected boolean isOverDraggable(GPoint pos) {
            // Anywhere in client fits.
            return true;
        }
        @Override
        protected int getDraggableX() {
            return targetView.fToGX(draggableFracPoint.x(), draggableGBox);
        }
        @Override
        protected int getDraggableY() {
            return targetView.fToGY(draggableFracPoint.y(), draggableGBox);
        }
    }

    private class MySplitmergable implements InterfaceSplitmergable {
        private final int inputId;
        private FracView view;
        private InterfaceBwdGraphics g;
        private boolean aborted = false;
        public MySplitmergable(
                int inputId,
                FracView view,
                InterfaceBwdGraphics g) {
            this.inputId = inputId;
            this.view = view;
            this.g = g;
        }
        @Override
        public boolean worthToSplit() {
            final GRect box = this.g.getBox();
            /*
             * Box large enough,
             * and that can be split
             * either horizontally or vertically.
             */
            return (box.area() >= MIN_GRECT_AREA_FOR_SPLIT)
                    && (box.maxSpan() >= 2 * MIN_GRECT_SPAN);
        }
        @Override
        public InterfaceSplitmergable split() {
            final InterfaceBwdGraphics oldG = this.g;
            final GRect oldBox = oldG.getBox();
            final FracView oldView = this.view;
            final FracPoint oldCenter = oldView.center();
            final double scale = oldView.scale();

            final boolean canSplitHorizontally =
                    (oldBox.ySpan() >= (2 * MIN_GRECT_SPAN));

            final GRect box1;
            final GRect box2;
            if (canSplitHorizontally) {
                // Top.
                box1 = oldBox.withSpans(
                        oldBox.xSpan(),
                        oldBox.ySpan()/2);
                // Bottom.
                box2 = oldBox.withBordersDeltas(
                        0, box1.ySpan(), 0, 0);
            } else {
                // Left.
                box1 = oldBox.withSpans(
                        oldBox.xSpan()/2,
                        oldBox.ySpan());
                // Right.
                box2 = oldBox.withBordersDeltas(
                        box1.xSpan(), 0, 0, 0);
            }

            // center is middle of mid pixel, so no x/yMidFp().
            final int dxMidTo1 = box1.xMid() - oldBox.xMid();
            final int dxMidTo2 = box2.xMid() - oldBox.xMid();
            final int dyMidTo1 = box1.yMid() - oldBox.yMid();
            final int dyMidTo2 = box2.yMid() - oldBox.yMid();

            final FracPoint center1 = new FracPoint(
                    oldCenter.x() + dxMidTo1 * scale,
                    oldCenter.y() + dyMidTo1 * scale);
            final FracPoint center2 = new FracPoint(
                    oldCenter.x() + dxMidTo2 * scale,
                    oldCenter.y() + dyMidTo2 * scale);

            final FracView view1 = new FracView(center1, scale);
            final FracView view2 = new FracView(center2, scale);

            final InterfaceBwdGraphics g1 = oldG.newChildGraphics(box1);
            final InterfaceBwdGraphics g2 = oldG.newChildGraphics(box2);

            this.view = view1;
            this.g = g1;

            return new MySplitmergable(
                    this.inputId,
                    view2,
                    g2);
        }
        @Override
        public void run() {
            final InterfaceBwdGraphics g = this.g;
            g.init();
            try {
                final GRect rect = g.getBox();
                final FracRect fracRect = this.view.computeFRect(rect);
                this.aborted = mandelbrotDrawer.drawFractal(
                        this.inputId,
                        lastInputIdRef,
                        fracRect,
                        rect,
                        g);
            } finally {
                g.finish();
            }
        }
        @Override
        public void merge(InterfaceSplitmergable t1, InterfaceSplitmergable t2) {
            final boolean t1Aborted = (t1 != null) && ((MySplitmergable) t1).aborted;
            final boolean t2Aborted = (t2 != null) && ((MySplitmergable) t2).aborted;
            this.aborted = t1Aborted || t2Aborted;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /**
     * At least 2, because fRectSpan is gRectSpan-1
     * (since we use pixels center position in fractal coordinates).
     * 1 would cause fractal rectangle to have a span of zero.
     * 0 would make us split 1-line boxes forever into an empty box and itself.
     */
    private static final int MIN_GRECT_SPAN = 2;

    private static final FracColoring[] FColoring_VALUES = FracColoring.values();

    /*
     * 
     */

    /**
     * To compute fractal, hopefully from a BG thread,
     * and hopefully in parallel.
     */
    private final MandDrawer mandelbrotDrawer = new MandDrawer();

    private final double initialScale;

    private final InterfaceScheduler bgScheduler;

    private final MyBgProcess bgProcess;

    private final MyDragController dragController = new MyDragController();

    /*
     * Inputs for fractal to compute.
     */

    /**
     * Updated any time from UI thread.
     */
    private volatile GPoint targetSpans;

    /**
     * Updated any time from UI thread.
     */
    private volatile FracView targetView;

    /**
     * Updated any time from UI thread.
     */
    private volatile int targetMaxIter = DEFAULT_MAX_ITER;

    private volatile FracColoring targetColoring = DEFAULT_COLORING;
    
    /**
     * In [1,256].
     */
    private volatile int targetColorIterPeriod = 0x80;

    /**
     * Set in UI thread after input change.
     * Read in computing thread to know if it must
     * abort its current computation.
     */
    private final AtomicInteger lastInputIdRef = new AtomicInteger();

    /*
     * If always dragging the same fractal point,
     * such as (0.0, 0.0), will cause integer overflow
     * on conversion to graphic coordinates,
     * if the scale is too small.
     * Instead, we update the draggable point on scale change,
     * by using a point within visual range.
     */

    private FracPoint draggableFracPoint;
    private GRect draggableGBox;

    /*
     * Computed fractal.
     */

    private volatile long lastCompTimeMs;

    /**
     * Guarded by synchronization on self.
     * 
     * Images created and written in background thread,
     * and then drawn on client and disposed in UI thread.
     */
    private final ArrayList<MyImgData> imgDataToUseList =
            new ArrayList<MyImgData>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public MandelbrotBwdTestCase() {
        this.initialScale = 0.0;
        this.bgScheduler = null;
        this.bgProcess = null;
    }

    public MandelbrotBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);

        final FracColorComputer colorComputer =
                this.mandelbrotDrawer.getColorComputer();
        colorComputer.setArgb32ForIterMax(COLOR_FOR_MAX_ITER.toArgb32());

        final FracRect defaultFRect = DEFAULT_FRACTAL_RECT;

        final FracPoint center = new FracPoint(
                defaultFRect.xMid(),
                defaultFRect.yMid());

        final double defaultXScale =
                (defaultFRect.xMax() - defaultFRect.xMin()) / INITIAL_WIDTH;
        final double defaultYScale =
                (defaultFRect.yMax() - defaultFRect.yMin()) / INITIAL_HEIGHT;

        double scale = Math.max(defaultXScale, defaultYScale);
        this.initialScale = scale;

        this.targetSpans = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

        this.targetView = new FracView(center, scale);

        final InterfaceScheduler bgScheduler;
        if (MUST_ALWAYS_COMPUTE_IMAGE_IN_BG_THREAD
                || binding.isConcurrentWritableImageManagementSupported()) {
            bgScheduler = BwdTestUtils.newHardScheduler(
                    binding.getClass().getSimpleName(), 1);
        } else {
            // BG = UI (yikes!).
            bgScheduler = binding.getUiThreadScheduler();
        }
        this.bgScheduler = bgScheduler;
        this.bgProcess = new MyBgProcess(bgScheduler);

        this.updateDraggableFPoint(GRect.DEFAULT_EMPTY);

        // On left click, we drag our image, not the window.
        this.setDragOnLeftClickActivated(false);
    }

    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new MandelbrotBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new MandelbrotBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    /*
     * 
     */

    @Override
    public Integer getParallelizerParallelismElseNull() {
        return TARGET_PARALLELISM;
    }

    /*
     * Window events.
     */

    @Override
    public void onWindowShown(BwdWindowEvent event) {
        super.onWindowShown(event);

        this.onClientBoundsChange();
    }

    @Override
    public void onWindowDeiconified(BwdWindowEvent event) {
        super.onWindowDeiconified(event);

        this.onClientBoundsChange();
    }

    @Override
    public void onWindowResized(BwdWindowEvent event) {
        super.onWindowResized(event);

        this.onClientBoundsChange();
    }

    @Override
    public void onWindowClosed(BwdWindowEvent event) {
        super.onWindowClosed(event);

        final InterfaceScheduler bgs = this.bgScheduler;
        if ((bgs != getBinding().getUiThreadScheduler())
                && (bgs instanceof HardScheduler)) {
            ((HardScheduler) bgs).shutdownNow(false);
        }
    }

    /*
     * Key events.
     */
    
    @Override
    public void onKeyPressed(BwdKeyEventPr event) {
        super.onKeyPressed(event);
        
        if ((event.getKey() == BwdKeys.C)
                && event.getModifierKeyDownSet().isEmpty()) {
            
            final FracColoring oldValue = this.targetColoring;
            final int newOrdinal =
                    (oldValue.ordinal() + 1) % FColoring_VALUES.length;
            final FracColoring newValue = FColoring_VALUES[newOrdinal];
            this.targetColoring = newValue;

            this.onDrawingInputUpdate();
        }
    }
    
    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);

        this.dragController.mousePressed(event.posInClient());
    }

    @Override
    public void onMouseReleased(BwdMouseEvent event) {
        super.onMouseReleased(event);

        this.dragController.mouseReleased(event.posInClient());
    }

    @Override
    public void onMouseMoved(BwdMouseEvent event) {
        super.onMouseMoved(event);

        this.dragController.mouseMoved(event.posInClient());
    }

    @Override
    public void onMouseDragged(BwdMouseEvent event) {
        super.onMouseDragged(event);

        if (this.dragController.mouseDragged(event.posInClient())) {
            final int cx = this.dragController.getDesiredX();
            final int cy = this.dragController.getDesiredY();

            final FracView oldView = this.targetView;

            final double fdx = oldView.gToFX(cx, this.draggableGBox) - this.draggableFracPoint.x();
            final double fdy = oldView.gToFY(cy, this.draggableGBox) - this.draggableFracPoint.y();

            final FracPoint oldCenter = oldView.center();
            final FracPoint newCenter = new FracPoint(
                    oldCenter.x() - fdx,
                    oldCenter.y() - fdy);

            this.targetView = new FracView(
                    newCenter,
                    oldView.scale());
            
            this.onDrawingInputUpdate();
        }
    }

    /*
     * Wheel events.
     */

    @Override
    public void onWheelRolled(BwdWheelEvent event) {
        super.onWheelRolled(event);

        if (event.isControlDown()) {
            /*
             * Doing it even if auto growth is on,
             * to avoid having user accidentally zoom
             * while trying to change maxIter.
             * 
             * Sign such as turning wheel for more detail
             * is the same as turning wheel for zooming.
             */
            final int toAdd = -event.yRoll();
            if (this.tryModifyMaxIter(toAdd)) {
                this.onDrawingInputUpdate();
            }
        } else if (event.isShiftDown()) {
            int sign = NbrsUtils.signum(event.yRoll());
            if (sign == 0) {
                /*
                 * For some libraries, when holding shift,
                 * y rolls become x rolls.
                 */
                sign = NbrsUtils.signum(event.xRoll());
            }
            if (sign != 0) {
                final int min = 1;
                final int max = 256;
                final int length = (max - min + 1);
                final int oldValue = this.targetColorIterPeriod;
                final int newValue = min + (((oldValue - min) - sign + length) % length);
                this.targetColorIterPeriod = newValue;

                this.onDrawingInputUpdate();
            }
        } else {
            final double zoomFactor =
                    (event.yRoll() < 0) ? 1.0/ZOOM_GROWTH_FACTOR : ZOOM_GROWTH_FACTOR;

            final GRect clientBounds = this.getHost().getClientBounds();
            if (!clientBounds.isEmpty()) {
                final int cx = event.xInClient();
                final int cy = event.yInClient();

                final FracView oldView = this.targetView;
                final FracPoint oldCenter = oldView.center();
                final double oldScale = oldView.scale();

                final double newScale =
                        Math.max(
                                MIN_SCALE,
                                Math.min(
                                        this.initialScale * MAX_INITIAL_SCALE_MULTIPLE,
                                        oldScale * zoomFactor));
                if (newScale != oldScale) {
                    // If zooming in, < 1.
                    final double scaleRatio = newScale / oldScale;

                    final GRect clientGBox = clientBounds.withPos(0, 0);

                    // (cx,cy) aka (fx,fy) is our zooming point.
                    final double fx = oldView.gToFX(cx, clientGBox);
                    final double fy = oldView.gToFY(cy, clientGBox);

                    final double oldDfx = fx - oldCenter.x();
                    final double oldDfy = fy - oldCenter.y();

                    final double newDfx = oldDfx * scaleRatio;
                    final double newDfy = oldDfy * scaleRatio;

                    final FracPoint newCenter = new FracPoint(
                            fx - newDfx,
                            fy - newDfy);

                    this.targetView = new FracView(
                            newCenter,
                            newScale);

                    this.updateDraggableFPoint(clientGBox);

                    this.onDrawingInputUpdate();
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final InterfaceBwdBinding binding = this.getBinding();

        final GRect box = g.getBox();

        /*
         * 
         */

        final GRect clientBounds = getHost().getClientBounds();
        if (clientBounds.isEmpty()) {
            // We don't want empty client bounds further.
            return GRect.DEFAULT_HUGE_IN_LIST;
        }

        // Useful in case some client bounds change is missed.
        this.updateTargetSpans(
                clientBounds.xSpan(),
                clientBounds.ySpan());

        /*
         * 
         */

        final MyImgData newestImgData = this.getNewestImgDataAndRemoveObsolete();
        if (newestImgData == null) {
            /*
             * Offscreen image not yet created.
             */
            this.ensureBgPainting();
        }

        g.setColor(BG_COLOR);
        g.clearRect(box);

        /*
         * Painting image.
         */

        final FracView targetView = this.targetView;
        
        if (newestImgData != null) {
            final InterfaceBwdWritableImage img = newestImgData.image;
            final FracView imgView = newestImgData.view;
            final FracView clientView = targetView;

            final GRect imgGBox = img.getRect();
            final GRect clientGBox = clientBounds.withPos(0, 0);

            final FracRect imgFRect = imgView.computeFRect(imgGBox);
            final FracRect clientFRect = clientView.computeFRect(clientGBox);

            final FracRect cmnFRect = clientFRect.intersected(imgFRect);
            if (cmnFRect.isEmpty()) {
                /*
                 * Part to draw is out of current image:
                 * nothing to draw.
                 */
            } else {
                final GRect srcRect = imgView.fToGRect(cmnFRect, imgGBox);
                final GRect dstRect = clientView.fToGRect(cmnFRect, clientGBox);

                g.drawImage(dstRect, img, srcRect);
            }
        }

        /*
         * Computing text lines.
         */

        final ArrayList<String> lineList = new ArrayList<String>();

        final boolean isBgUi = (this.bgScheduler == binding.getUiThreadScheduler());
        final String bgThreadCountStr = (isBgUi ? "0" : "1");

        final int parallelism;
        if (newestImgData != null) {
            parallelism = newestImgData.image.getGraphics().getPaintingParallelizer().getParallelism();
        } else {
            parallelism = -1;
        }
        final String prlThreadCountStr = ((parallelism <= 1) ? ((parallelism < 0) ? "?" : "0") : "" + parallelism);

        lineList.add("t. center [lef-click/drag]: " + targetView.center());
        lineList.add("t. scale [wheel]: " + targetView.scale());
        lineList.add("t. coloring [c]: " + this.targetColoring);
        lineList.add("t. color iter period [shift+wheel]: " + this.targetColorIterPeriod);
        lineList.add("t. maxIter [ctrl+wheel]: " + this.targetMaxIter);
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("image num: ");
            final int imgNum = ((newestImgData == null) ? 0 : newestImgData.imageNum);
            sb.append(imgNum);
            sb.append( " (" + this.lastCompTimeMs + " ms)");
            lineList.add(sb.toString());
        }
        lineList.add("painting num: " + this.getCallCount_paint());
        lineList.add("bg+prl: " + bgThreadCountStr + "+" + prlThreadCountStr);

        /*
         * Drawing text lines.
         */

        final InterfaceBwdFont font = g.getFont();
        final int fontHeight = font.metrics().height();
        final int size = lineList.size();
        for (int i = 0; i < size; i++) {
            final String line = lineList.get(i);
            final int textY = i * fontHeight;
            BwdTestUtils.drawTextAndBg(
                    g,
                    TEXT_BG_COLOR,
                    TEXT_FG_COLOR,
                    0,
                    textY,
                    line);
        }

        /*
         * 
         */

        return GRect.DEFAULT_HUGE_IN_LIST;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void onClientBoundsChange() {
        final GRect clientBounds = this.getHost().getClientBounds();
        if (!clientBounds.isEmpty()) {
            this.updateTargetSpans(
                    clientBounds.xSpan(),
                    clientBounds.ySpan());
        }
    }

    private void updateTargetSpans(int xSpan, int ySpan) {
        final GPoint oldSpans = this.targetSpans;
        final GPoint newSpans = GPoint.valueOf(xSpan, ySpan);
        if (!newSpans.equals(oldSpans)) {
            this.targetSpans = newSpans;
            this.onDrawingInputUpdate();
        }
    }

    /**
     * To be called only in UI thread
     * (mutates targetMaxIter without CAS).
     * 
     * @return True if did modify, false otherwise.
     */
    private boolean tryModifyMaxIter(int toAdd) {
        final int oldValue = this.targetMaxIter;
        final int newValue = NbrsUtils.toRange(
                MIN_MAX_ITER,
                MAX_MAX_ITER,
                NbrsUtils.plusBounded(oldValue, toAdd));
        final boolean modif = (newValue != oldValue);
        if (modif) {
            this.targetMaxIter = newValue;
        }
        return modif;
    }

    /**
     * To be called only in UI thread.
     */
    private void onDrawingInputUpdate() {
        // For ASAP (view drawing, and info text).
        this.getHost().ensurePendingClientPainting();

        // For later.
        this.restartBgPainting();
    }

    /**
     * To be called only in UI thread
     * (might set a new lastCompMonit).
     */
    private void restartBgPainting() {
        if (this.bgProcess.isStarted()) {
            /*
             * Already started, and not yet stopped
             * (didn't read inputs yet).
             */
        } else {
            // Pretending things changed
            // (which is usually the case).
            this.lastInputIdRef.incrementAndGet();

            this.bgProcess.start();
        }
    }

    private void ensureBgPainting() {
        if (this.bgProcess.isStarted()) {
            /*
             * Already started, and not yet stopped
             * (didn't read inputs yet).
             */
        } else {
            this.bgProcess.start();
        }
    }

    private final void updateDraggableFPoint(GRect clientGBox) {
        this.draggableFracPoint = this.targetView.center();
        this.draggableGBox = clientGBox;
    }

    /**
     * @return Can be null.
     */
    private MyImgData getNewestImgDataAndRemoveObsolete() {
        MyImgData newestImgData;
        synchronized (this.imgDataToUseList) {
            final ArrayList<MyImgData> list = this.imgDataToUseList;
            final int size = list.size();
            if (size != 0) {
                newestImgData = list.get(size - 1);
                if (size >= 2) {
                    for (int i = 0; i < size - 1; i++) {
                        final MyImgData obsoleteImgData = list.get(i);
                        obsoleteImgData.image.dispose();
                    }
                    list.clear();
                    list.add(newestImgData);
                }
            } else {
                newestImgData = null;
            }
        }
        return newestImgData;
    }

    /**
     * To be called in BG scheduler (which might be UI scheduler).
     * 
     * @return A new writable image, with things drawn on it,
     *         or null if its computation was aborted.
     */
    private InterfaceBwdWritableImage newDrawnWritableImage(
            int inputId,
            GPoint spans,
            FracView view,
            int maxIter,
            FracColoring coloring,
            int colorIterPeriod,
            int imageNum) {

        final InterfaceBwdBinding binding = getBinding();

        final int width = spans.x();
        final int height = spans.y();

        final InterfaceBwdWritableImage wi = binding.newWritableImage(
                width,
                height);

        final InterfaceBwdGraphics wig = wi.getGraphics();

        this.mandelbrotDrawer.setMaxIter(maxIter);

        final FracColorComputer colorComputer =
                this.mandelbrotDrawer.getColorComputer();
        colorComputer.setColoring(coloring);
        colorComputer.setColorIterPeriod(colorIterPeriod);

        /*
         * NB: Sometimes, if we move around too much, the ThreadPoolExecutor
         * we use for parallelization get stressed of some sort and complains
         * (throws RejectedExecutionException, and then shuts itself down (!)).
         */
        final InterfaceParallelizer parallelizer = wig.getPaintingParallelizer();

        final MySplitmergable splitmergable = new MySplitmergable(
                inputId,
                view,
                wig);

        final long t1 = System.nanoTime();
        parallelizer.execute(splitmergable);
        if (splitmergable.aborted) {
            // Ignoring aborted timings.
        } else {
            final long t2 = System.nanoTime();
            this.lastCompTimeMs = (t2 - t1) / (1000 * 1000);
        }

        final InterfaceBwdWritableImage ret;
        if (splitmergable.aborted) {
            wi.dispose();
            ret = null;
        } else {
            ret = wi;
        }
        return ret;
    }
}
