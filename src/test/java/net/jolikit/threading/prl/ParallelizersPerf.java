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
package net.jolikit.threading.prl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import net.jolikit.lang.DefaultExceptionHandler;
import net.jolikit.lang.DefaultThreadFactory;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.execs.FixedThreadExecutor;

/**
 * Basic benches for parallelizers.
 */
public class ParallelizersPerf {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Small enough to show eventual differences between parallelizers.
     */
    private static final int FIBO_MIN_SEQ_N = 10;
    private static final int FIBO_MIN_N = 25;
    private static final int FIBO_MAX_N = FIBO_MIN_N;

    /*
     * 
     */
    
    private static final int NBR_OF_RUNS = 4;
    
    private static final int NBR_OF_CALLS = 1000;
    
    private static final int MIN_PARALLELISM = 32;
    
    private static final int MAX_PARALLELISM =
        2 * Runtime.getRuntime().availableProcessors();
    
    /*
     * 
     */
    
    private static final boolean MUST_BENCH_EP_WITH_TPE = true;
    
    private static final boolean MUST_BENCH_EP_WITH_FTE = true;
    
    /*
     * 
     */
    
    /**
     * @return A new list of parallelizers to bench.
     */
    private static List<InterfaceParallelizerForTests> newParallelizerList() {
        final List<InterfaceParallelizerForTests> list = new ArrayList<InterfaceParallelizerForTests>();
        
        list.add(new SequentialParallelizerForTests());
        
        for (int parallelism = MIN_PARALLELISM; parallelism <= MAX_PARALLELISM; parallelism *= 2) {
            for (int discrepancy = 0; discrepancy <= 2; discrepancy++) {
                if (MUST_BENCH_EP_WITH_TPE) {
                    // No need to swallow since TPE maintains parallelism.
                    final boolean mustSwallow = false;
                    final ThreadFactory threadFactory =
                        new DefaultThreadFactory(
                            null,
                            new DefaultExceptionHandler(mustSwallow));
                    final Executor executor =
                        Executors.newFixedThreadPool(
                            parallelism,
                            threadFactory);
                    list.add(new ExecutorParallelizerForTests(
                        executor,
                        parallelism,
                        discrepancy,
                        null));
                }
                if (MUST_BENCH_EP_WITH_FTE) {
                    final Executor executor =
                        FixedThreadExecutor.newInstance(
                            "HeEpTest",
                            true,
                            parallelism,
                            new DefaultThreadFactory());
                    list.add(new ExecutorParallelizerForTests(
                        executor,
                        parallelism,
                        discrepancy,
                        null));
                }
            }
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
        final long a = System.nanoTime();
        System.out.println("--- " + ParallelizersPerf.class.getSimpleName() + "... ---");
        
        for (int n = FIBO_MIN_N; n <= FIBO_MAX_N; n++) {
            
            System.out.println();
            
            bench_fibo(n);
        }
        
        final long b = System.nanoTime();
        System.out.println("--- ..." + ParallelizersPerf.class.getSimpleName()
            + ", " + TestUtils.nsToSRounded(b-a) + " s ---");
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
