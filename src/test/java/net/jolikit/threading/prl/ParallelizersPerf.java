/*
 * Copyright 2019 Jeff Hain
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
package net.jolikit.threading.prl;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.test.utils.TestUtils;

/**
 * Basic benches for parallelizers.
 */
public class ParallelizersPerf {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int FIBO_MIN_SEQ_N = 15;
    private static final int FIBO_MIN_N = 25;
    private static final int FIBO_MAX_N = FIBO_MIN_N;

    /*
     * 
     */
    
    private static final int NBR_OF_RUNS = 4;
    
    private static final int NBR_OF_CALLS = 1000;

    /*
     * 
     */
    
    /**
     * @return A new list of parallelizers to bench.
     */
    private static List<InterfaceParallelizerForTests> newParallelizerList() {
        final List<InterfaceParallelizerForTests> list = new ArrayList<InterfaceParallelizerForTests>();
        
        list.add(new SequentialParallelizerForTests());
        
        for (int[] prlDiscrArr : new int[][]{
                {2, 0},
                {2, 3},
                {4, 0},
                {4, 3}}) {
            final int parallelism = prlDiscrArr[0];
            final int discrepancy = prlDiscrArr[1];
            list.add(new ExecutorParallelizerForTests(parallelism, discrepancy));
        }
        
        return list;
    }

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyFiboSpm implements InterfaceSplitmergable {
        final int minSeqN;
        int n;
        int result;
        public MyFiboSpm(
                int minSeqN,
                int n) {
            this.minSeqN = minSeqN;
            this.n = n;
        }
        @Override
        public boolean worthToSplit() {
            return this.n > this.minSeqN;
        }
        @Override
        public InterfaceSplitmergable split() {
            final int oldN = this.n;
            this.n = oldN-1;
            return new MyFiboSpm(
                    this.minSeqN,
                    oldN-2);
        }
        @Override
        public void run() {
            this.result = fiboSeq(this.n);
        }
        @Override
        public void merge(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            final MyFiboSpm aImpl = (MyFiboSpm) a;
            final MyFiboSpm bImpl = (MyFiboSpm) b;
            this.result =
                ((aImpl == null) ? 0 : aImpl.result)
                + ((bImpl == null) ? 0 : bImpl.result);
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println(TestUtils.getJVMInfo());
        newRun(args);
    }

    public static void newRun(String[] args) {
        new ParallelizersPerf().run(args);
    }
    
    public ParallelizersPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void run(String[] args) {
        System.out.println("--- " + ParallelizersPerf.class.getSimpleName() + "... ---");
        
        for (int n = FIBO_MIN_N; n <= FIBO_MAX_N; n++) {
            
            System.out.println();
            
            bench_fibo(n);
        }
        
        System.out.println("--- ..." + ParallelizersPerf.class.getSimpleName() + " ---");
    }

    private static void bench_fibo(int n) {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            
            System.out.println();
            
            bench_fibo(n, parallelizer);
            
            parallelizer.shutdownAndWait();
        }
    }
    
    private static void bench_fibo(int n, InterfaceParallelizerForTests parallelizer) {
        System.out.println("bench_fibo(" + n + ", " + computeDescr(parallelizer) + ")");

        final int nbrOfCalls = NBR_OF_CALLS;

        final int expected = fiboSeq(n);
        
        for (int k = 0; k < NBR_OF_RUNS; k++) {

            final long a = System.nanoTime();
            for (int i = 0; i < nbrOfCalls; i++) {
                final MyFiboSpm spm = new MyFiboSpm(
                        FIBO_MIN_SEQ_N,
                        FIBO_MAX_N);
                parallelizer.execute(spm);
                
                // Result check for anti optim,
                // and to make sure we don't bench garbage.
                final int result = spm.result;
                if (result != expected) {
                    throw new AssertionError(result + " != " + expected);
                }
            }
            final long b = System.nanoTime();

            System.out.println(
                    nbrOfCalls + " computations of fibo(" + n + ") took "
                            + TestUtils.nsToSRounded(b-a) + " s");
        }
    }
    
    /*
     * 
     */
    
    private static int fiboSeq(int n) {
        if (n <= 1) {
            return n;
        }
        return fiboSeq(n-1) + fiboSeq(n-2);
    }

    private static String computeDescr(InterfaceParallelizerForTests parallelizer) {
        return parallelizer.getClass().getSimpleName()
                + "[prl = " + parallelizer.getParallelism() + "]"
                + parallelizer.getSpeDescr();
    }
}
