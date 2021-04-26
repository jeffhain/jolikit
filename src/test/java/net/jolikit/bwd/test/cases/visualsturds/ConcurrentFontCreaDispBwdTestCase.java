/*
 * Copyright 2019-2021 Jeff Hain
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
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
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
public class ConcurrentFontCreaDispBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int PARALLELISM = 4;
    
    private static final int SPLIT_COUNT = 2;
    
    private static final int FONT_SIZE_INCREMENT = 4;

    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyFontCreationSplitmergable implements InterfaceSplitmergable {
        private final BwdFontKind fontKind;
        private final AtomicInteger fontSizeGenerator;
        private final int fontSize;
        private int remainingSplitCount;
        ArrayList<InterfaceBwdFont> result = null;
        public MyFontCreationSplitmergable(
                BwdFontKind fontKind,
                AtomicInteger fontSizeGenerator,
                int remainingSplitCount) {
            this.fontKind = fontKind;
            this.fontSizeGenerator = fontSizeGenerator;
            this.fontSize = fontSizeGenerator.addAndGet(FONT_SIZE_INCREMENT);
            this.remainingSplitCount = remainingSplitCount;
        }
        @Override
        public boolean worthToSplit() {
            return (this.remainingSplitCount > 0);
        }
        @Override
        public InterfaceSplitmergable split() {
            this.remainingSplitCount--;
            
            return new MyFontCreationSplitmergable(
                    this.fontKind,
                    this.fontSizeGenerator,
                    this.remainingSplitCount);
        }
        @Override
        public void run() {
            final InterfaceBwdFont font = getBinding().getFontHome().newFontWithSize(this.fontKind, this.fontSize);
            
            final ArrayList<InterfaceBwdFont> result = new ArrayList<InterfaceBwdFont>();
            result.add(font);
            this.result = result;
        }
        @Override
        public void merge(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            final MyFontCreationSplitmergable aa = (MyFontCreationSplitmergable) a;
            final MyFontCreationSplitmergable bb = (MyFontCreationSplitmergable) b;
            
            final ArrayList<InterfaceBwdFont> merged = new ArrayList<InterfaceBwdFont>();
            if (aa != null) {
                merged.addAll(aa.result);
            }
            if (bb != null) {
                merged.addAll(bb.result);
            }
            this.result = merged;
        }
    }

    private class MyFontDisposalSplittable implements InterfaceSplittable {
        private final List<InterfaceBwdFont> fontList;
        public MyFontDisposalSplittable(List<InterfaceBwdFont> fontList) {
            this.fontList = fontList;
        }
        @Override
        public boolean worthToSplit() {
            return (this.fontList.size() > 1);
        }
        @Override
        public InterfaceSplittable split() {
            final List<InterfaceBwdFont> splitList = new ArrayList<InterfaceBwdFont>();
            final int n = this.fontList.size();
            final int halfish = n/2;
            for (int i = 0; i < halfish; i++) {
                final InterfaceBwdFont lastFont = this.fontList.remove((n-1) - i);
                splitList.add(lastFont);
            }
            
            return new MyFontDisposalSplittable(splitList);
        }
        @Override
        public void run() {
            for (InterfaceBwdFont font : this.fontList) {
                font.dispose();
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

    public ConcurrentFontCreaDispBwdTestCase() {
    }

    public ConcurrentFontCreaDispBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ConcurrentFontCreaDispBwdTestCase(binding);
    }

    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ConcurrentFontCreaDispBwdTestCase(this.getBinding());
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
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final long paintCount = ++this.paintCounter;
        
        final GRect box = g.getBox();
        final int x = box.x();
        final int y = box.y();

        final InterfaceBwdBinding binding = this.getBinding();
        
        final InterfaceParallelizer parallelizer;
        if (binding.isConcurrentFontManagementSupported()) {
            parallelizer = binding.getParallelizer();
        } else {
            parallelizer = SequentialParallelizer.getDefault();
        }
        
        /*
         * Parallel creation.
         */

        final List<InterfaceBwdFont> fontList;
        {
            final InterfaceBwdFont font = g.getFont();
            final AtomicInteger fontSizeGenerator = new AtomicInteger(font.size());
            final MyFontCreationSplitmergable splitmergable = new MyFontCreationSplitmergable(
                    font.kind(),
                    fontSizeGenerator,
                    SPLIT_COUNT);
            parallelizer.execute(splitmergable);
            fontList = splitmergable.result;
        }
        
        /*
         * Usage for drawing.
         */
        
        g.setColor(BwdColor.WHITE);
        g.fillRect(box);
        {
            final String text = "paint count = " + paintCount;
            
            g.setColor(BwdColor.BLACK);
            
            int yOffset = 0;
            
            // Sorting to avoid useless visual shaking.
            Collections.sort(
                    fontList,
                    new Comparator<InterfaceBwdFont>() {
                        @Override
                        public int compare(
                                InterfaceBwdFont o1,
                                InterfaceBwdFont o2) {
                            return o1.id().compareTo(o2.id());
                        }
                    });
            
            for (InterfaceBwdFont font : fontList) {
                g.setFont(font);
                g.drawText(x, y + yOffset, text);
                yOffset += font.metrics().height();
            }
        }
        
        /*
         * Parallel disposal.
         */
        
        {
            final MyFontDisposalSplittable splittable = new MyFontDisposalSplittable(fontList);
            parallelizer.execute(splittable);
        }
        
        /*
         * Scheduling next painting.
         */
        
        getHost().ensurePendingClientPainting();
        
        return null;
    }
}
