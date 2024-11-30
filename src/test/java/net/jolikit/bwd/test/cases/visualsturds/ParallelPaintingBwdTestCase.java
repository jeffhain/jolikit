/*
 * Copyright 2019-2024 Jeff Hain
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
package net.jolikit.bwd.test.cases.visualsturds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import net.jolikit.bwd.api.InterfaceBwdBinding;
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
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.lang.Dbg;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplittable;
import net.jolikit.time.sched.hard.HardScheduler;

/**
 * To test that painting can be done in parallel,
 * including when using (already-created) fonts and images.
 */
public class ParallelPaintingBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    private static final int TARGET_PARALLELISM = 4;
    
    /**
     * BOXSAMPLED to see image pixels accurately.
     */
    private static final BwdScalingType IMAGE_SCALING_TYPE = BwdScalingType.BOXSAMPLED;
    
    /**
     * To make sure our font is not identical to default font,
     * so that setting it actually does something.
     */
    private static final int FONT_SIZE_DELTA = 1;
    
    /**
     * With alpha, for the fun, and makes it easier to see graphics boxes,
     * with their painting thread related background color.
     */
    private static final String IMG_FILE_PATH = BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_ALPHA_PNG;

    private static final int MAX_NBR_OF_SPLITS = 2;
    
    private static final int MIN_THREAD_NUM = 1;
    
    private static final int INITIAL_AREA_WIDTH = 400;
    private static final int INITIAL_AREA_HEIGHT = 400;
    
    /**
     * Room for client and writable image graphics.
     */
    private static final int INITIAL_WIDTH = 2 * INITIAL_AREA_WIDTH;
    private static final int INITIAL_HEIGHT = INITIAL_AREA_HEIGHT;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private enum MyPaintingOrigin {
        CLIENT,
        WI_FROM_UI_THREAD,
        WI_FROM_BG_THREAD,
    }
    
    private class MySplittable implements InterfaceSplittable {
        private final MyPaintingOrigin paintingOrigin;
        /**
         * Creating a new graphics for this splittable at each split,
         * and storing the draw box to use as its box and initial clip,
         * which allows to make sure we won't leak outside of it while drawing.
         */
        private InterfaceBwdGraphics g;
        /**
         * Including splits from parents.
         */
        private int splitCount;
        /**
         * @param g this graphics won't be modified. A child graphics will be used instead.
         */
        public MySplittable(
                MyPaintingOrigin paintingOrigin,
                InterfaceBwdGraphics g,
                GRect drawBox,
                int splitCount) {
            this.paintingOrigin = paintingOrigin;
            this.g = g.newChildGraphics(drawBox);
            this.splitCount = splitCount;
        }
        @Override
        public boolean worthToSplit() {
            return (this.splitCount < MAX_NBR_OF_SPLITS);
        }
        @Override
        public InterfaceSplittable split() {
            final InterfaceBwdGraphics g = this.g;
            
            final GRect drawBox = g.getInitialClipInBase();
            final int x = drawBox.x();
            final int y = drawBox.y();
            final int xSpan = drawBox.xSpan();
            final int ySpan = drawBox.ySpan();
            
            /*
             * Cutting the longest span in half.
             */
            
            final GRect box1;
            final GRect box2;
            if (xSpan > ySpan) {
                final int halfish = (xSpan>>1);
                
                box1 = GRect.valueOf(
                        x,
                        y,
                        xSpan - halfish,
                        ySpan);
                box2 = GRect.valueOf(
                        x + (xSpan - halfish),
                        y,
                        halfish,
                        ySpan);
            } else {
                final int halfish = (ySpan>>1);
                
                box1 = GRect.valueOf(
                        x,
                        y,
                        xSpan,
                        ySpan - halfish);
                box2 = GRect.valueOf(
                        x,
                        y + (ySpan - halfish),
                        xSpan,
                        halfish);
            }
            
            this.splitCount++;
            
            this.g = g.newChildGraphics(box1);
            
            return new MySplittable(
                    this.paintingOrigin,
                    g,
                    box2,
                    this.splitCount);
        }
        @Override
        public void run() {
            final InterfaceBwdGraphics g = this.g;
            g.init();
            try {
                drawInBox_inInitFinish(g, this.paintingOrigin);
            } finally {
                g.finish();
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private boolean initialized = false;
    private InterfaceBwdFont font;
    private InterfaceBwdImage image;
    
    /**
     * Some made-up number for each drawing thread.
     * 
     * Guarded by synchronization on itself.
     */
    private final Map<Thread,Integer> numByThread = new HashMap<Thread,Integer>();
    
    private final HardScheduler bgScheduler = BwdTestUtils.newHardScheduler(1);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ParallelPaintingBwdTestCase() {
    }

    public ParallelPaintingBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ParallelPaintingBwdTestCase(binding);
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
     * 
     */
    
    @Override
    public void onWindowClosed(BwdWindowEvent event) {
        this.bgScheduler.shutdownNow(false);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        if (!this.initialized) {
            final InterfaceBwdFontHome fontHome = this.getBinding().getFontHome();
            final int fontSize = fontHome.getDefaultFont().size() + FONT_SIZE_DELTA;
            this.font = fontHome.newFontWithSize(fontSize);
            
            this.image = this.getBinding().newImage(IMG_FILE_PATH);
            
            this.initialized = true;
        }
        
        final GRect box = g.getBox();
        final GRect leftArea = GRect.valueOf(box.x(), box.y(), box.xSpan()/2, box.yMax());
        final GRect rightArea = box.withBordersDeltas(leftArea.xSpan(), 0, 0, 0);
        
        /*
         * Painting client left area.
         */
        
        this.paintWithParallelizer(
                MyPaintingOrigin.CLIENT,
                g,
                leftArea);
        
        /*
         * Painting client right area,
         * with writable image.
         */

        final InterfaceBwdWritableImage wi =
                this.createWritableImageAndPaintItInSomeThread(
                        rightArea.xSpan(),
                        rightArea.ySpan());

        g.setImageScalingType(IMAGE_SCALING_TYPE);
        g.drawImage(rightArea, wi);

        // Memory leak (writable images referenced by the binding
        // for cleanup on shutdown) if not doing this.
        wi.dispose();

        /*
         * Scheduling next painting.
         */
        
        if (DEBUG) {
            Dbg.log("tc: ensurePendingClientPainting(...)...");
        }
        getHost().ensurePendingClientPainting();
        
        return null;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void checkNoPaintingParallelismIfNotSupported(
            InterfaceBwdGraphics g) {
        final InterfaceParallelizer paintingParallelizer = g.getPaintingParallelizer();
        final int parallelism = paintingParallelizer.getParallelism();
        
        if ((!getBinding().isParallelPaintingSupported())
                && (parallelism > 1)) {
            throw new IllegalStateException(
                    "parallel painting not supported, but painting parallelism is "
                            + parallelism);
        }
    }
    
    /**
     * To be called in UI thread.
     * Decides whether painting is to be done in UI thread or
     * in a background thread, which itself might use a painting parallelizer.
     * 
     * @return The painted writable image.
     */
    private InterfaceBwdWritableImage createWritableImageAndPaintItInSomeThread(
            final int width,
            final int height) {
        final InterfaceBwdBinding binding = this.getBinding();
        
        final InterfaceBwdWritableImage wi;
        if (binding.isConcurrentWritableImageManagementSupported()) {
            final AtomicReference<InterfaceBwdWritableImage> wiRef =
                    new AtomicReference<InterfaceBwdWritableImage>();
            this.bgScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    final InterfaceBwdWritableImage myWi =
                            createWritableImageAndPaintItInCurrentThread(
                                    width,
                                    height);
                    wiRef.set(myWi);
                    synchronized (wiRef) {
                        wiRef.notifyAll();
                    }
                }
            });
            synchronized (wiRef) {
                try {
                    wiRef.wait(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            wi = wiRef.get();
        } else {
            wi = this.createWritableImageAndPaintItInCurrentThread(
                    width,
                    height);
        }
        return wi;
    }
    
    /**
     * @return The painted writable image.
     */
    private InterfaceBwdWritableImage createWritableImageAndPaintItInCurrentThread(
            int width,
            int height) {
        final InterfaceBwdBinding binding = this.getBinding();
        
        final InterfaceBwdWritableImage wi = binding.newWritableImage(width, height);
        
        final InterfaceBwdGraphics wig = wi.getGraphics();
        
        final boolean isUiThread = binding.getUiThreadScheduler().isWorkerThread();
        
        final MyPaintingOrigin paintingOrigin;
        if (isUiThread) {
            paintingOrigin = MyPaintingOrigin.WI_FROM_UI_THREAD;
        } else {
            paintingOrigin = MyPaintingOrigin.WI_FROM_BG_THREAD;
        }
        this.paintWithParallelizer(paintingOrigin, wig, wig.getBox());
        
        return wi;
    }
    
    /*
     * 
     */
    
    /**
     * If parallel painting is not supported, the parallelizer
     * must be sequential.
     */
    private void paintWithParallelizer(
            MyPaintingOrigin paintingOrigin,
            InterfaceBwdGraphics g,
            GRect box) {
        
        checkNoPaintingParallelismIfNotSupported(g);

        final InterfaceParallelizer paintingParallelizer = g.getPaintingParallelizer();

        final int splitCount = 0;
        final MySplittable fillSplittable = new MySplittable(
                paintingOrigin,
                g,
                box,
                splitCount);
        paintingParallelizer.execute(fillSplittable);
    }
    
    private int getNbrOfKnownDrawingThreads() {
        final int size;
        synchronized (this.numByThread) {
            size = this.numByThread.size();
        }
        return size;
    }
    
    private int getCurrentThreadNum() {
        final Thread key = Thread.currentThread();
        final int index;
        synchronized (this.numByThread) {
            final int size = this.numByThread.size();
            final int newValue = MIN_THREAD_NUM + size;
            final Integer oldValueRef = this.numByThread.putIfAbsent(key, newValue);
            index = (oldValueRef != null) ? oldValueRef.intValue() : newValue;
        }
        return index;
    }

    /*
     * 
     */
    
    private void drawInBox_inInitFinish(
            InterfaceBwdGraphics g,
            MyPaintingOrigin paintingOrigin) {
        
        final GRect box = g.getBox();
        
        final int x = box.x();
        final int y = box.y();
        final int xSpan = box.xSpan();
        final int ySpan = box.ySpan();
        
        /*
         * Filling.
         * Using a color based on thread hash code.
         */
        
        {
            final int thc = Thread.currentThread().hashCode();
            
            // We want it opaque, because we don't do
            // an initial clearing of client background.
            final int alpha8 = 0xFF;
            final int red8 = (thc * 3) & 0xFF;
            final int green8 = (thc * 5) & 0xFF;
            final int blue8 = (thc * 7) & 0xFF;
            final int argb32 = Argb32.toArgb32FromInt8(alpha8, red8, green8, blue8);
            
            g.setArgb32(argb32);

            g.fillRect(box);
        }
        
        /*
         * Primitive.
         */
        
        final GRect ovalRect = GRect.valueOf(x, y, xSpan, ySpan/2);
        
        g.setColor(BwdColor.WHITE);
        g.fillOval(ovalRect);
        
        /*
         * Computing text lines.
         */

        final ArrayList<String> lineList = new ArrayList<String>();

        if (paintingOrigin == MyPaintingOrigin.CLIENT) {
            lineList.add("CLI");
        } else if (paintingOrigin == MyPaintingOrigin.WI_FROM_UI_THREAD) {
            lineList.add("WI");
            lineList.add("(UI thread)");
        } else if (paintingOrigin == MyPaintingOrigin.WI_FROM_BG_THREAD) {
            lineList.add("WI");
            lineList.add("(BG thread)");
        } else {
            throw new IllegalArgumentException("" + paintingOrigin);
        }

        lineList.add("drawing thread:");

        final StringBuilder sb = new StringBuilder();
        final int threadNum = this.getCurrentThreadNum();
        // Must be retrieved after num,
        // to make sure its num will be covered.
        final int maxNbrOfThreads = this.getNbrOfKnownDrawingThreads();
        for (int i = MIN_THREAD_NUM; i < MIN_THREAD_NUM + maxNbrOfThreads; i++) {
            if (i == threadNum) {
                sb.append(threadNum);
            } else {
                sb.append(" ");
            }
        }
        lineList.add(sb.toString());

        lineList.add("paint count:");
        lineList.add(Integer.toString(this.getCallCount_paint()));
        
        /*
         * Drawing text lines.
         */
        
        final InterfaceBwdFont font = this.font;
        g.setFont(font);

        g.setColor(BwdColor.BLACK);
        
        final int fontHeight = font.metrics().height();
        final int size = lineList.size();
        for (int i = 0; i < size; i++) {
            final String line = lineList.get(i);
            final int yOffset = (i - size/2) * fontHeight;
            BwdTestUtils.drawTextCentered(
                    g,
                    ovalRect.x(),
                    ovalRect.y() + yOffset,
                    ovalRect.xSpan(),
                    ovalRect.ySpan(),
                    line);
        }
        
        /*
         * Image.
         */
        
        final GRect imageRect = box.withBordersDeltas(0, ovalRect.ySpan(), 0, 0);
        g.setImageScalingType(IMAGE_SCALING_TYPE);
        g.drawImage(imageRect, this.image);
    }
}
