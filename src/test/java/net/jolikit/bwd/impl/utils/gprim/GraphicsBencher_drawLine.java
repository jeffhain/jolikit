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
package net.jolikit.bwd.impl.utils.gprim;

import java.util.Random;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.test.utils.TestUtils;

public class GraphicsBencher_drawLine {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final long RANDOM_SEED = (false ? System.nanoTime() : 123456789L);

    private static final int DEFAULT_NBR_OF_RUNS = 2;

    private static final int DEFAULT_NBR_OF_CALLS = 100 * 1000 * 1000;

    /**
     * Can be less due to clip.
     */
    private static final int TARGET_MAX_LINE_LENGTH = 100;

    /**
     * Not too many, to avoid too much cache misses overhead.
     */
    private static final int CASE_MASK = 0xF;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final int nbrOfRuns;
    
    /**
     * Used as numbers of pixels hit,
     * i.e. if drawing a 100-pixels line,
     * will use a 100 times smaller value,
     * for homogeneous durations.
     */
    private final int nbrOfCalls;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public GraphicsBencher_drawLine(
            int nbrOfRuns,
            int nbrOfCalls) {
        this.nbrOfRuns = nbrOfRuns;
        this.nbrOfCalls = nbrOfCalls;
    }
    
    /*
     * 
     */

    /**
     * Uses a default bencher.
     */
    public static void bench_drawLine_static(InterfaceBwdGraphics g) {
        final GraphicsBencher_drawLine bencher = new GraphicsBencher_drawLine(
                DEFAULT_NBR_OF_RUNS,
                DEFAULT_NBR_OF_CALLS);
        bencher.bench_drawLine(g);
    }
    
    /*
     * 
     */
    
    public void bench_drawLine(InterfaceBwdGraphics g) {
        this.bench_drawLine_inClip(g);
        this.bench_drawLineStipple_inClip_factor1(g);
        this.bench_drawLineStipple_inClip_factor3(g);
        
        this.bench_drawLine_inClip_horizontal(g);
        this.bench_drawLineStipple_inClip_horizontal_factor1(g);
        this.bench_drawLineStipple_inClip_horizontal_factor3(g);
        
        this.bench_drawLine_fixed_outOfClipObvious(g);
        this.bench_drawLine_fixed_outOfClipNotObvious(g);
        this.bench_drawLine_varying_inClip(g);
        this.bench_drawLine_varying_toClip(g);
    }
    
    public void bench_drawLine_inClip_horizontal(InterfaceBwdGraphics g) {
        final GRect clip = g.getClipInUser();
        
        final int lineLength = getLineLength(clip);
        final int myNbrOfCalls = this.nbrOfCalls / lineLength;

        for (int k = 0; k < this.nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < myNbrOfCalls; i++) {
                g.drawLine(clip.x(), clip.y(), clip.xMax(), clip.y());
            }
            long b = System.nanoTime();
            System.out.println(myNbrOfCalls + " calls to drawLine(4int), in clip, horizontal, took " + TestUtils.nsToSRounded(b-a) + " s");
        }
    }
    
    public void bench_drawLineStipple_inClip_horizontal_factor1(InterfaceBwdGraphics g) {
        final GRect clip = g.getClipInUser();
        
        final int lineLength = getLineLength(clip);
        final int myNbrOfCalls = this.nbrOfCalls / lineLength;

        for (int k = 0; k < this.nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < myNbrOfCalls; i++) {
                g.drawLineStipple(clip.x(), clip.y(), clip.xMax(), clip.y(), 1, (short) 0x5555, 0);
            }
            long b = System.nanoTime();
            System.out.println(
                    myNbrOfCalls
                    + " calls to drawLineStipple(5int,short,int), in clip, horizontal, factor 1, took "
                    + TestUtils.nsToSRounded(b-a) + " s");
        }
    }
    
    public void bench_drawLineStipple_inClip_horizontal_factor3(InterfaceBwdGraphics g) {
        final GRect clip = g.getClipInUser();
        
        final int lineLength = getLineLength(clip);
        final int myNbrOfCalls = this.nbrOfCalls / lineLength;

        for (int k = 0; k < this.nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < myNbrOfCalls; i++) {
                g.drawLineStipple(clip.x(), clip.y(), clip.xMax(), clip.y(), 3, (short) 0x5555, 0);
            }
            long b = System.nanoTime();
            System.out.println(
                    myNbrOfCalls
                    + " calls to drawLineStipple(5int,short,int), in clip, horizontal, factor 3, took "
                    + TestUtils.nsToSRounded(b-a) + " s");
        }
    }
    
    public void bench_drawLine_inClip_vertical(InterfaceBwdGraphics g) {
        final GRect clip = g.getClipInUser();
        
        final int lineLength = getLineLength(clip);
        final int myNbrOfCalls = this.nbrOfCalls / lineLength;

        for (int k = 0; k < this.nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < myNbrOfCalls; i++) {
                g.drawLine(clip.x(), clip.y(), clip.x(), clip.yMax());
            }
            long b = System.nanoTime();
            System.out.println(myNbrOfCalls + " calls to drawLine(4int), in clip, vertical, took " + TestUtils.nsToSRounded(b-a) + " s");
        }
    }
    
    /**
     * Fixed coordinates, to exclude cache misses overhead, in clip.
     */
    public void bench_drawLine_inClip(InterfaceBwdGraphics g) {
        final Random random = newRandom(RANDOM_SEED);
        
        final GRect clip = g.getClipInUser();
        
        final int lineLength = getLineLength(clip);
        final int myNbrOfCalls = this.nbrOfCalls / lineLength;

        final int caseMask = CASE_MASK;
        final int nbrOfCases = (caseMask + 1);
        final int[] ptsArr = new int[4 * nbrOfCases];
        for (int i = 0; i < nbrOfCases; i++) {
            final int from = 4 * i;
            addRandomLinePointsInClipInto(
                    random,
                    clip,
                    lineLength,
                    from,
                    ptsArr);
        }
        
        for (int k = 0; k < this.nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < myNbrOfCalls; i++) {
                final int caseIndex = (i & caseMask);
                final int from = (caseIndex << 2);
                g.drawLine(
                        ptsArr[from],
                        ptsArr[from + 1],
                        ptsArr[from + 2],
                        ptsArr[from + 3]);
            }
            long b = System.nanoTime();
            System.out.println(
                    myNbrOfCalls
                    + " calls to drawLine(4int), fixed, in clip, took "
                    + TestUtils.nsToSRounded(b-a) + " s");
        }
    }

    public void bench_drawLineStipple_inClip_factor1(InterfaceBwdGraphics g) {
        final Random random = newRandom(RANDOM_SEED);
        
        final GRect clip = g.getClipInUser();
        
        final int lineLength = getLineLength(clip);
        final int myNbrOfCalls = this.nbrOfCalls / lineLength;

        final int caseMask = CASE_MASK;
        final int nbrOfCases = (caseMask + 1);
        final int[] ptsArr = new int[4 * nbrOfCases];
        for (int i = 0; i < nbrOfCases; i++) {
            final int from = 4 * i;
            addRandomLinePointsInClipInto(
                    random,
                    clip,
                    lineLength,
                    from,
                    ptsArr);
        }
        
        for (int k = 0; k < this.nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < myNbrOfCalls; i++) {
                final int caseIndex = (i & caseMask);
                final int from = (caseIndex << 2);
                g.drawLineStipple(
                        ptsArr[from],
                        ptsArr[from + 1],
                        ptsArr[from + 2],
                        ptsArr[from + 3],
                        1, (short) 0x5555, 0);
            }
            long b = System.nanoTime();
            System.out.println(
                    myNbrOfCalls
                    + " calls to drawLineStipple(5int,short,int), fixed, in clip, factor 1, took "
                    + TestUtils.nsToSRounded(b-a) + " s");
        }
    }
    
    public void bench_drawLineStipple_inClip_factor3(InterfaceBwdGraphics g) {
        final Random random = newRandom(RANDOM_SEED);
        
        final GRect clip = g.getClipInUser();
        
        final int lineLength = getLineLength(clip);
        final int myNbrOfCalls = this.nbrOfCalls / lineLength;

        final int caseMask = CASE_MASK;
        final int nbrOfCases = (caseMask + 1);
        final int[] ptsArr = new int[4 * nbrOfCases];
        for (int i = 0; i < nbrOfCases; i++) {
            final int from = 4 * i;
            addRandomLinePointsInClipInto(
                    random,
                    clip,
                    lineLength,
                    from,
                    ptsArr);
        }
        
        for (int k = 0; k < this.nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < myNbrOfCalls; i++) {
                final int caseIndex = (i & caseMask);
                final int from = (caseIndex << 2);
                g.drawLineStipple(
                        ptsArr[from],
                        ptsArr[from + 1],
                        ptsArr[from + 2],
                        ptsArr[from + 3],
                        3, (short) 0x5555, 0);
            }
            long b = System.nanoTime();
            System.out.println(
                    myNbrOfCalls
                    + " calls to drawLineStipple(5int,short,int), fixed, in clip, factor 3, took "
                    + TestUtils.nsToSRounded(b-a) + " s");
        }
    }

    public void bench_drawLine_fixed_outOfClipObvious(InterfaceBwdGraphics g) {

        final GRect clip = g.getClipInUser();

        for (int k = 0; k < this.nbrOfRuns; k++) {
            // Above clip.
            final int x1 = clip.x() - 1;
            final int y1 = clip.y() - 1;
            final int x2 = clip.xMax() + 1;
            final int y2 = clip.y() - 1;
            
            long a = System.nanoTime();
            for (int i = 0; i < this.nbrOfCalls; i++) {
                g.drawLine(x1, y1, x2, y2);
            }
            long b = System.nanoTime();
            System.out.println(this.nbrOfCalls + " calls to drawLine(4int), fixed, obviously out of clip, took " + TestUtils.nsToSRounded(b-a) + " s");
        }
    }

    public void bench_drawLine_fixed_outOfClipNotObvious(InterfaceBwdGraphics g) {

        final GRect clip = g.getClipInUser();

        for (int k = 0; k < this.nbrOfRuns; k++) {
            // Left and above clip.
            final int x1 = clip.x() - 11;
            final int y1 = clip.y() + 10;
            final int x2 = clip.x() + 10;
            final int y2 = clip.y() - 11;
            
            long a = System.nanoTime();
            for (int i = 0; i < this.nbrOfCalls; i++) {
                g.drawLine(x1, y1, x2, y2);
            }
            long b = System.nanoTime();
            System.out.println(
                    this.nbrOfCalls
                    + " calls to drawLine(4int), fixed, not obviously out of clip, took "
                    + TestUtils.nsToSRounded(b-a) + " s");
        }
    }

    /**
     * Varying coordinates, to include cache misses overhead, in clip.
     */
    public void bench_drawLine_varying_inClip(InterfaceBwdGraphics g) {
        final Random random = newRandom(RANDOM_SEED);

        final GRect clip = g.getClipInUser();
        
        final int lineLength = getLineLength(clip);
        final int myNbrOfCalls = this.nbrOfCalls / lineLength;

        final int caseMask = CASE_MASK;
        final int nbrOfCases = (caseMask + 1);
        final int[] ptsArr = new int[4 * nbrOfCases];
        for (int i = 0; i < nbrOfCases; i++) {
            final int from = 4 * i;
            addRandomLinePointsInClipInto(
                    random,
                    clip,
                    lineLength,
                    from,
                    ptsArr);
        }

        for (int k = 0; k < this.nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < myNbrOfCalls; i++) {
                final int caseIndex = (i & caseMask);
                final int from = (caseIndex << 2);
                g.drawLine(
                        ptsArr[from],
                        ptsArr[from + 1],
                        ptsArr[from + 2],
                        ptsArr[from + 3]);
            }
            long b = System.nanoTime();
            long ns = Math.max(0, b-a);
            System.out.println(
                    myNbrOfCalls
                    + " calls to drawLine(4int), varying, in clip, took "
                    + TestUtils.nsToSRounded(ns) + " s");
        }
    }
    
    /**
     * Varying coordinates, to include cache misses overhead, to clip.
     */
    public void bench_drawLine_varying_toClip(InterfaceBwdGraphics g) {
        final Random random = newRandom(RANDOM_SEED);

        final GRect clip = g.getClipInUser();
        
        final int lineLength = getLineLength(clip);
        final int myNbrOfCalls = this.nbrOfCalls / lineLength;

        final int caseMask = CASE_MASK;
        final int nbrOfCases = (caseMask + 1);
        final int[] ptsArr = new int[4 * nbrOfCases];
        for (int i = 0; i < nbrOfCases; i++) {
            final int from = 4 * i;
            addRandomLinePointsToClipInto(
                    random,
                    clip,
                    lineLength,
                    from,
                    ptsArr);
        }
        
        for (int k = 0; k < this.nbrOfRuns; k++) {
            long a = System.nanoTime();
            for (int i = 0; i < myNbrOfCalls; i++) {
                final int caseIndex = (i & caseMask);
                final int from = (caseIndex << 2);
                g.drawLine(
                        ptsArr[from],
                        ptsArr[from + 1],
                        ptsArr[from + 2],
                        ptsArr[from + 3]);
            }
            long b = System.nanoTime();
            
            System.out.println(
                    myNbrOfCalls
                    + " calls to drawLine(4int), varying, to clip, took "
                    + TestUtils.nsToSRounded(b-a) + " s");
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static Random newRandom(long seed) {
        return new Random(seed);
    }
    
    private static int getLineLength(GRect clip) {
        return Math.min(TARGET_MAX_LINE_LENGTH, clip.minSpan());
    }
    
    /**
     * @param ptsArr (in,out)
     */
    private static void addRandomLinePointsInClipInto(
            Random random,
            GRect clip,
            int lineLength,
            //
            int from,
            int[] ptsArr) {
        
        final int halfLineLength = lineLength/2;
        
        // p1 left, in middle.
        final int x1 = clip.x() + 1;
        final int y1 = clip.y() + clip.ySpan()/2;
        // p2 to the right, a bit above or below.
        final int x2 = clip.x() + lineLength;
        // Giving some slope, not to be too horizontal.
        final int y2 = y1 + (random.nextBoolean() ? -halfLineLength : halfLineLength);

        ptsArr[from] = x1;
        ptsArr[from + 1] = y1;
        ptsArr[from + 2] = x2;
        ptsArr[from + 3] = y2;
    }
    
    /**
     * @param ptsArr (in,out)
     */
    private static void addRandomLinePointsToClipInto(
            Random random,
            GRect clip,
            int lineLength,
            //
            int from,
            int[] ptsArr) {
        
        final int x1 = clip.x() + lineLength - 2;
        final int y1 = clip.y() + 2;
        // p2 out of clip, line crossing either left or top border.
        final int x2 = clip.x() - 1;
        final int y2 = clip.y() - (random.nextBoolean() ? -1 : 0);

        ptsArr[from] = x1;
        ptsArr[from + 1] = y1;
        ptsArr[from + 2] = x2;
        ptsArr[from + 3] = y2;
    }
}
