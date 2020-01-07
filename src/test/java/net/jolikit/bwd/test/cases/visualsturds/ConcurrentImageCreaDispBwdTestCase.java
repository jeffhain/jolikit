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
package net.jolikit.bwd.test.cases.visualsturds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.threading.prl.InterfaceSplitmergable;
import net.jolikit.threading.prl.InterfaceSplittable;
import net.jolikit.threading.prl.SequentialParallelizer;

/**
 * To test that fonts can be created and disposed concurrently.
 * 
 * Works if doesn't generate errors.
 */
public class ConcurrentImageCreaDispBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int PARALLELISM = 4;
    
    private static final int SPLIT_COUNT = 2;
    
    private static final String[] IMG_FILE_PATH_ARR = new String[]{
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PNG,
        BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_GREY_PNG,
        BwdTestResources.TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG,
        BwdTestResources.TEST_IMG_FILE_PATH_MOUSE_HEAD_PNG
    };

    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 150;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyImageInfo {
        /**
         * Using path for sorting images.
         */
        final String imageFilePath;
        final InterfaceBwdImage image;
        public MyImageInfo(
                String imageFilePath,
                InterfaceBwdImage image) {
            this.imageFilePath = imageFilePath;
            this.image = image;
        }
    }
    
    private class MyImageCreationSplitmergable implements InterfaceSplitmergable {
        private final AtomicInteger imageIndexGenerator;
        private final int imageIndex;
        private int remainingSplitCount;
        ArrayList<MyImageInfo> result = null;
        public MyImageCreationSplitmergable(
                AtomicInteger imageIndexGenerator,
                int remainingSplitCount) {
            this.imageIndexGenerator = imageIndexGenerator;
            this.imageIndex = (imageIndexGenerator.incrementAndGet() % IMG_FILE_PATH_ARR.length);
            this.remainingSplitCount = remainingSplitCount;
        }
        @Override
        public boolean worthToSplit() {
            return (this.remainingSplitCount > 0);
        }
        @Override
        public InterfaceSplitmergable split() {
            this.remainingSplitCount--;
            
            return new MyImageCreationSplitmergable(
                    this.imageIndexGenerator,
                    this.remainingSplitCount);
        }
        @Override
        public void run() {
            final String imageFilePath = IMG_FILE_PATH_ARR[this.imageIndex];
            final InterfaceBwdImage image = getBinding().newImage(imageFilePath);
            final MyImageInfo imageInfo = new MyImageInfo(imageFilePath, image);
            
            final ArrayList<MyImageInfo> result = new ArrayList<MyImageInfo>();
            result.add(imageInfo);
            this.result = result;
        }
        @Override
        public void merge(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            final MyImageCreationSplitmergable aa = (MyImageCreationSplitmergable) a;
            final MyImageCreationSplitmergable bb = (MyImageCreationSplitmergable) b;
            
            final ArrayList<MyImageInfo> merged = new ArrayList<MyImageInfo>();
            if (aa != null) {
                merged.addAll(aa.result);
            }
            if (bb != null) {
                merged.addAll(bb.result);
            }
            this.result = merged;
        }
    }

    private class MyImageDisposalSplittable implements InterfaceSplittable {
        private final List<InterfaceBwdImage> imageList;
        public MyImageDisposalSplittable(List<InterfaceBwdImage> imageList) {
            this.imageList = imageList;
        }
        @Override
        public boolean worthToSplit() {
            return (this.imageList.size() > 1);
        }
        @Override
        public InterfaceSplittable split() {
            final List<InterfaceBwdImage> splitList = new ArrayList<InterfaceBwdImage>();
            final int n = this.imageList.size();
            final int halfish = n/2;
            for (int i = 0; i < halfish; i++) {
                final InterfaceBwdImage lastImage = this.imageList.remove((n-1) - i);
                splitList.add(lastImage);
            }
            
            return new MyImageDisposalSplittable(splitList);
        }
        @Override
        public void run() {
            for (InterfaceBwdImage image : this.imageList) {
                image.dispose();
            }
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private long paintCounter = 0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ConcurrentImageCreaDispBwdTestCase() {
    }

    public ConcurrentImageCreaDispBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ConcurrentImageCreaDispBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ConcurrentImageCreaDispBwdTestCase(this.getBinding());
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
        return PARALLELISM;
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final long paintCount = ++this.paintCounter;
        
        final GRect box = g.getBoxInClient();
        final int x = box.x();
        final int y = box.y();
        final int xSpan = box.xSpan();
        final int ySpan = box.ySpan();

        final InterfaceBwdBinding binding = this.getBinding();
        
        final InterfaceParallelizer parallelizer;
        if (binding.isConcurrentImageCreationAndDisposalSupported()) {
            parallelizer = binding.getParallelizer();
        } else {
            parallelizer = SequentialParallelizer.getDefault();
        }
        
        /*
         * Parallel creation.
         */

        final List<MyImageInfo> imageInfoList;
        {
            final AtomicInteger imageIndexGenerator = new AtomicInteger();
            final MyImageCreationSplitmergable splitmergable = new MyImageCreationSplitmergable(
                    imageIndexGenerator,
                    SPLIT_COUNT);
            parallelizer.execute(splitmergable);
            imageInfoList = splitmergable.result;
        }
        
        /*
         * Usage for drawing.
         */
        
        g.setColor(BwdColor.WHITE);
        g.fillRect(box);
        {
            // Sorting to avoid useless visual shaking.
            Collections.sort(
                    imageInfoList,
                    new Comparator<MyImageInfo>() {
                        @Override
                        public int compare(
                                MyImageInfo o1,
                                MyImageInfo o2) {
                            return o1.imageFilePath.compareTo(o2.imageFilePath);
                        }
                    });
            
            final int n = (int) Math.ceil(Math.sqrt(imageInfoList.size()));
            final int cellWidth = xSpan / n;
            final int cellHeight = ySpan / n;
            final double cellRatio = cellWidth / (double) cellHeight;
            
            final int size = imageInfoList.size();
            for (int i = 0; i < size; i++) {
                final int cxi = i % n;
                final int cyi = i / n;
                
                final MyImageInfo imageInfo = imageInfoList.get(i);
                final InterfaceBwdImage image = imageInfo.image;
                final double imageRatio = image.getWidth() / (double) image.getHeight();
                
                // Fitting to cell, and preserving proportions.
                final int widthToUse;
                final int heightToUse;
                if (imageRatio < cellRatio) {
                    heightToUse = cellHeight;
                    widthToUse = (int) (heightToUse * imageRatio);
                } else {
                    widthToUse = cellWidth;
                    heightToUse = (int) (widthToUse / imageRatio);
                }
                g.drawImage(
                        x + cxi * cellWidth,
                        y + cyi * cellHeight,
                        widthToUse,
                        heightToUse,
                        image);
            }
        }
        {
            final String text = "paint count = " + paintCount;
            
            final InterfaceBwdFont font = g.getFont();
            
            g.setColor(BwdColor.WHITE);
            g.fillRect(
                    x,
                    y,
                    font.fontMetrics().computeTextWidth(text),
                    font.fontMetrics().fontHeight());
            
            g.setColor(BwdColor.BLACK);
            g.drawText(x, y, text);
        }
        
        /*
         * Parallel disposal.
         */
        
        {
            final List<InterfaceBwdImage> imageList = new ArrayList<InterfaceBwdImage>();
            for (MyImageInfo imageInfo : imageInfoList) {
                imageList.add(imageInfo.image);
            }
            
            final MyImageDisposalSplittable splittable = new MyImageDisposalSplittable(imageList);
            parallelizer.execute(splittable);
        }
        
        /*
         * Scheduling next painting.
         */
        
        getHost().ensurePendingClientPainting();
        
        return GRect.DEFAULT_HUGE_IN_LIST;
    }
}
