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
package net.jolikit.threading.prl;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.RethrowException;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.ConcUnit;

/**
 * Basic tests for parallelizers.
 */
public class ParallelizersTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /**
     * +1 with main thread, since calling thread is used for work
     * even if it's not a worker thread.
     */
    private static final int DEFAULT_PARALLELISM = 4;
    private static final int DEFAULT_DISCREPANCY = 4;

    /*
     * 
     */

    private static final int FIBO_MIN_SEQ_N = 3;
    private static final int FIBO_MAX_N = 13;
    private static final int NBR_OF_RUNS_PER_CASE = 10 * 1000;

    /*
     * 
     */
    
    private static final double DEFAULT_EXCEPTION_PROBABILITY = 0.1;
    
    /*
     * 
     */

    /**
     * @return A new list of parallelizers to test.
     */
    private static List<InterfaceParallelizerForTests> newParallelizerList() {
        return Arrays.asList(
                (InterfaceParallelizerForTests)
                new ExecutorParallelizerForTests(
                        DEFAULT_PARALLELISM,
                        DEFAULT_DISCREPANCY));
    }

    /**
     * @return A new list of parallelizers to test.
     */
    private static List<InterfaceParallelizerForTests> newParallelizerList(
            final UncaughtExceptionHandler exceptionHandler) {
        
        // Thread factory quietly swallowing RejectedExecutionException
        // and MyThrowables, to avoid spam.
        final ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable runnable) {
                final Runnable wrapper = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            runnable.run();
                        } catch (RejectedExecutionException e) {
                            // Can happen on shutdown: quiet.
                        } catch (Throwable t) {
                            if (isOrContainsMyThrowable(t)) {
                                // Quiet (to avoid spam).
                            } else {
                                Unchecked.throwIt(t);
                            }
                        }
                    }
                };
                return new Thread(wrapper);
            }
        };
        
        return Arrays.asList(
                (InterfaceParallelizerForTests)
                new ExecutorParallelizerForTests(
                        DEFAULT_PARALLELISM,
                        DEFAULT_DISCREPANCY,
                        threadFactory,
                        exceptionHandler));
    }

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    /*
     * For correctness test.
     */

    private static class MyCountingRunnable implements Runnable {
        private int runCount = 0;
        public int getRunCount() {
            return this.runCount;
        }
        @Override
        public void run() {
            this.runCount++;
        }
    }
    
    private static class MyFiboSp implements InterfaceSplittable {
        final int minSeqN;
        int n;
        int result;
        private final AtomicInteger sum;
        /**
         * @param sum If not null, result is added to it. Useful if no merge.
         */
        public MyFiboSp(
                int minSeqN,
                int n,
                AtomicInteger sum) {
            this.minSeqN = minSeqN;
            this.n = n;
            this.sum = sum;
        }
        @Override
        public boolean worthToSplit() {
            return this.n > this.minSeqN;
        }
        @Override
        public InterfaceSplittable split() {
            final int oldN = this.n;
            this.n = oldN-1;
            return new MyFiboSp(
                    this.minSeqN,
                    oldN-2,
                    this.sum);
        }
        @Override
        public void run() {
            final int result = fiboSeq(this.n);
            if (DEBUG) {
                Dbg.logPr(this, "[" + this.hashCode() + "].run() fibo(" + this.n + ") = " + result);
            }
            this.result = result;
            if (this.sum != null) {
                this.sum.addAndGet(result);
            }
        }
    }

    private static class MyFiboSpm extends MyFiboSp implements InterfaceSplitmergable {
        public MyFiboSpm(
                int minSeqN,
                int n) {
            super(minSeqN, n, null);
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
        public void merge(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            final MyFiboSpm aImpl = (MyFiboSpm) a;
            final MyFiboSpm bImpl = (MyFiboSpm) b;
            this.result =
                ((aImpl == null) ? 0 : aImpl.result)
                + ((bImpl == null) ? 0 : bImpl.result);
        }
    }
    
    /*
     * For reentrancy test.
     */
    
    private class MyReentrantRunnable implements Runnable {
        private final InterfaceParallelizer parallelizer;
        private int nbrOfReenterings;
        public MyReentrantRunnable(
                InterfaceParallelizer parallelizer,
                int nbrOfReenterings) {
            this.parallelizer = parallelizer;
            this.nbrOfReenterings = nbrOfReenterings;
        }
        @Override
        public void run() {
            if (DEBUG) {
                Dbg.logPr(this, "[" + this.hashCode() + "].run() nbrOfReenterings = " + this.nbrOfReenterings);
            }
            if (this.nbrOfReenterings > 0) {
                this.nbrOfReenterings--;
                // Shouldn't hurt to use "this" instead of a new runnable.
                this.parallelizer.execute(this);
                // Tests that execute(...) blocks until completion,
                // i.e. until all reentrant calls done.
                cu.assertEquals(0, this.nbrOfReenterings);
            }
        }
    }

    private class MyReentrantSp implements InterfaceSplittable {
        /**
         * For random split and reentrancy patterns.
         */
        final Random random;
        final InterfaceParallelizer parallelizer;
        int n;
        /**
         * Must be n.
         */
        int result;
        private final AtomicInteger sum;
        /**
         * Uses ConcUnit, so must call ConcUnit.assertNoError() at test end.
         * @param sum Must not be null.
         */
        public MyReentrantSp(
                Random random,
                InterfaceParallelizer parallelizer,
                int n,
                AtomicInteger sum) {
            this.random = random;
            this.parallelizer = parallelizer;
            this.n = n;
            this.sum = sum;
        }
        @Override
        public boolean worthToSplit() {
            return (this.n >= 2) && this.random.nextBoolean();
        }
        @Override
        public InterfaceSplittable split() {
            final int halfish = this.n / 2;
            this.n -= halfish;
            return new MyReentrantSp(
                    this.random,
                    this.parallelizer,
                    halfish,
                    this.sum);
        }
        @Override
        public void run() {
            // Often going reentrant, to help testing against deadlocks,
            // but not always else can get call stack overflow.
            final boolean mustReenter = (this.random.nextDouble() < 0.9);
            if (DEBUG) {
                Dbg.logPr(this, "[" + this.hashCode() + "].run() mustReenter = " + mustReenter);
            }
            if (mustReenter) {
                final AtomicInteger innerSum = new AtomicInteger();
                final MyReentrantSp innerSp = new MyReentrantSp(
                        this.random,
                        this.parallelizer,
                        this.n,
                        innerSum);
                if (DEBUG) {
                    Dbg.logPr(this, "[" + this.hashCode() + "].run() execute... innerSp.n = " + innerSp.n);
                }
                this.parallelizer.execute(innerSp);
                final int innerSumValue = innerSum.get();
                if (DEBUG) {
                    Dbg.logPr(this, "[" + this.hashCode() + "].run() ...execute innerSumValue = " + innerSumValue);
                }
                // Tests that execute(...) blocks until parallelization
                // completion.
                cu.assertEquals(this.n, innerSumValue);
                this.result = innerSumValue;
            } else {
                this.result = this.n;
            }
            this.sum.addAndGet(this.result);
        }
    }

    private class MyReentrantSpm implements InterfaceSplitmergable {
        /**
         * For random split and reentrancy patterns.
         */
        final Random random;
        final InterfaceParallelizer parallelizer;
        int n;
        /**
         * Must be n.
         */
        int result;
        /**
         * Uses ConcUnit, so must call ConcUnit.assertNoError() at test end.
         */
        public MyReentrantSpm(
                Random random,
                InterfaceParallelizer parallelizer,
                int n) {
            this.random = random;
            this.parallelizer = parallelizer;
            this.n = n;
        }
        @Override
        public boolean worthToSplit() {
            return (this.n >= 2) && this.random.nextBoolean();
        }
        @Override
        public InterfaceSplitmergable split() {
            final int halfish = this.n / 2;
            this.n -= halfish;
            return new MyReentrantSpm(
                    this.random,
                    this.parallelizer,
                    halfish);
        }
        @Override
        public void run() {
            // Often going reentrant, to help testing against deadlocks,
            // but not always else can get call stack overflow.
            final boolean mustReenter = (this.random.nextDouble() < 0.9);
            if (DEBUG) {
                Dbg.logPr(this, "[" + this.hashCode() + "].run() mustReenter = " + mustReenter);
            }
            if (mustReenter) {
                final MyReentrantSpm innerSpm = new MyReentrantSpm(
                        this.random,
                        this.parallelizer,
                        this.n);
                if (DEBUG) {
                    Dbg.logPr(this, "[" + this.hashCode() + "].run() execute... innerSpm.n = " + innerSpm.n);
                }
                this.parallelizer.execute(innerSpm);
                if (DEBUG) {
                    Dbg.logPr(this, "[" + this.hashCode() + "].run() ...execute innerSpm.result = " + innerSpm.result);
                }
                // Tests that execute(...) blocks until parallelization
                // completion.
                cu.assertEquals(this.n, innerSpm.result);
                this.result = innerSpm.result;
            } else {
                this.result = this.n;
            }
        }
        @Override
        public void merge(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            final MyReentrantSpm aImpl = (MyReentrantSpm) a;
            final MyReentrantSpm bImpl = (MyReentrantSpm) b;
            if (aImpl != null) {
                if (bImpl != null) {
                    this.result = aImpl.result + bImpl.result;
                } else {
                    this.result = aImpl.result;
                }
            } else {
                this.result = bImpl.result;
            }
        }
    }

    /*
     * For exceptions test.
     */
    
    private static class MyThrowable extends Throwable {
        private static final long serialVersionUID = 1L;
        public MyThrowable(String message) {
            super("for test : " + message);
        }
    }

    private static class MyThrower {
        private final double exceptionProbability;
        private final AtomicReference<Throwable> exceptionThrown = new AtomicReference<Throwable>();
        private final Random random = new Random(123456789L);
        public MyThrower() {
            this(DEFAULT_EXCEPTION_PROBABILITY);
        }
        public MyThrower(double exceptionProbability) {
            this.exceptionProbability = exceptionProbability;
        }
        private void throwOrNot(String methodName) {
            if (random.nextDouble() < this.exceptionProbability) {
                final MyThrowable t = new MyThrowable("exception for test in " + methodName + " from thread " + Thread.currentThread());
                this.exceptionThrown.compareAndSet(null, t);
                Unchecked.throwIt(t);
            }
        }
    }

    private static class MyThrowingRunnable implements Runnable {
        private final MyThrower thrower;
        public MyThrowingRunnable(MyThrower thrower) {
            this.thrower = thrower;
        }
        @Override
        public void run() {
            thrower.throwOrNot("run()");
        }
    }

    private static class MyThrowingSp implements InterfaceSplittable {
        private int minSeqN;
        private int n;
        private final MyThrower thrower_worthToSplit;
        private final MyThrower thrower;
        public MyThrowingSp(
                int minSeqN,
                int n,
                MyThrower thrower) {
            this(minSeqN, n, thrower, thrower);
        }
        public MyThrowingSp(
                int minSeqN,
                int n,
                MyThrower thrower_worthToSplit,
                MyThrower thrower) {
            this.minSeqN = minSeqN;
            this.n = n;
            this.thrower_worthToSplit = thrower_worthToSplit;
            this.thrower = thrower;
        }
        @Override
        public boolean worthToSplit() {
            thrower_worthToSplit.throwOrNot("worthToSplit()");
            return this.n > this.minSeqN;
        }
        @Override
        public InterfaceSplittable split() {
            thrower.throwOrNot("split()");
            final int oldN = this.n;
            this.n = oldN-1;
            return new MyThrowingSp(
                    this.minSeqN,
                    oldN-2,
                    this.thrower);
        }
        @Override
        public void run() {
            thrower.throwOrNot("run()");
        }
    }

    private static class MyThrowingSpm implements InterfaceSplitmergable {
        private int minSeqN;
        private int n;
        private final MyThrower thrower_worthToSplit;
        private final MyThrower thrower;
        public MyThrowingSpm(
                int minSeqN,
                int n,
                MyThrower thrower) {
            this(minSeqN, n, thrower, thrower);
        }
        public MyThrowingSpm(
                int minSeqN,
                int n,
                MyThrower thrower_worthToSplit,
                MyThrower thrower) {
            this.minSeqN = minSeqN;
            this.n = n;
            this.thrower_worthToSplit = thrower_worthToSplit;
            this.thrower = thrower;
        }
        @Override
        public boolean worthToSplit() {
            thrower_worthToSplit.throwOrNot("worthToSplit()");
            return this.n > this.minSeqN;
        }
        @Override
        public InterfaceSplitmergable split() {
            thrower.throwOrNot("split()");
            final int oldN = this.n;
            this.n = oldN-1;
            return new MyThrowingSpm(
                    this.minSeqN,
                    oldN-2,
                    this.thrower);
        }
        @Override
        public void run() {
            thrower.throwOrNot("run()");
        }
        @Override
        public void merge(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            thrower.throwOrNot("merge(...)");
        }
    }
    
    /*
     * For interrupt test.
     */
    
    private class MyInterruptingFiboSpm extends MyFiboSpm {
        private final boolean mustInterruptInConstructorElseRun;
        public MyInterruptingFiboSpm(
                int minSeqN,
                int n,
                boolean mustInterruptInConstructorElseRun) {
            super(minSeqN, n);
            this.mustInterruptInConstructorElseRun = mustInterruptInConstructorElseRun;
            if (this.mustInterruptInConstructorElseRun) {
                Thread.currentThread().interrupt();
            }
        }
        @Override
        public InterfaceSplitmergable split() {
            final int oldN = this.n;
            this.n = oldN-1;
            return new MyInterruptingFiboSpm(
                    this.minSeqN,
                    oldN-2,
                    this.mustInterruptInConstructorElseRun);
        }
        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                /*
                 * Not running.
                 */
            } else {
                super.run();
                if (!this.mustInterruptInConstructorElseRun) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /*
     * For exception handler test.
     */

    private static class MyExceptionHandler implements UncaughtExceptionHandler {
        final AtomicReference<Throwable> firstHandledRef = new AtomicReference<Throwable>();
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            this.firstHandledRef.compareAndSet(null, e);
            /*
             * Does rethrow: the parallelizer must be robust to that.
             * so that parallelization still terminate (doesn't block),
             * even if computations are aborted.
             */
            Unchecked.throwIt(e);
        }
    };
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final ConcUnit cu = new ConcUnit();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor test.
     */
    public void test_ExecutorParallelizer() {
        try {
            new ExecutorParallelizer(null, 1, 0);
            fail();
        } catch (NullPointerException e) {
        }
        
        final Executor executor = Executors.newSingleThreadExecutor();
        
        for (int badParallelism : new int[]{Integer.MIN_VALUE, 0}) {
            try {
                new ExecutorParallelizer(executor, badParallelism, 0);
                fail();
            } catch (IllegalArgumentException e) {
            }
        }
        
        for (int badMaxDepth : new int[]{Integer.MIN_VALUE, -1}) {
            try {
                new ExecutorParallelizer(executor, 1, badMaxDepth);
                fail();
            } catch (IllegalArgumentException e) {
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_correctness_runnable() {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            test_correctness_runnable(parallelizer);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_correctness_runnable(InterfaceParallelizerForTests parallelizer) {
        if (DEBUG) {
            Dbg.log("test_correctness_runnable(" + computeDescr(parallelizer) + ")");
        }
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final MyCountingRunnable runnable = new MyCountingRunnable();
            parallelizer.execute(runnable);
            
            assertEquals(1, runnable.getRunCount());
        }
    }
    
    public void test_correctness_splittable() {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            test_correctness_splittable(parallelizer);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_correctness_splittable(InterfaceParallelizerForTests parallelizer) {
        if (DEBUG) {
            Dbg.log("test_correctness_splittable(" + computeDescr(parallelizer) + ")");
        }

        final int n = FIBO_MAX_N;
        final int expected = fiboSeq(n);

        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final AtomicInteger sum = new AtomicInteger();
            final MyFiboSp sp = new MyFiboSp(FIBO_MIN_SEQ_N, n, sum);
            parallelizer.execute(sp);

            final int actual = sum.get();
            if (DEBUG) {
                Dbg.log("expected = " + expected + ", actual = " + actual);
            }
            assertEquals(expected, actual);
        }
    }
    
    public void test_correctness_splitmergable() {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            test_correctness_splitmergable(parallelizer);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_correctness_splitmergable(InterfaceParallelizerForTests parallelizer) {
        if (DEBUG) {
            Dbg.log("test_correctness_splitmergable(" + computeDescr(parallelizer) + ")");
        }
        
        final int n = FIBO_MAX_N;
        final int expected = fiboSeq(n);

        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final MyFiboSpm spm = new MyFiboSpm(FIBO_MIN_SEQ_N, n);
            parallelizer.execute(spm);

            final int actual = spm.result;
            if (DEBUG) {
                Dbg.log("expected = " + expected + ", actual = " + actual);
            }
            assertEquals(expected, actual);
        }
    }
    
    /*
     * 
     */
    
    public void test_reentrant_runnable() {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            if (parallelizer.isReentrant()) {
                test_reentrant_runnable(parallelizer);
            }
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_reentrant_runnable(InterfaceParallelizerForTests parallelizer) {
        if (DEBUG) {
            Dbg.log("test_reentrant_runnable(" + computeDescr(parallelizer) + ")");
        }
        
        final int nbrOfReenterings = 10;
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final MyReentrantRunnable runnable = new MyReentrantRunnable(
                    parallelizer,
                    nbrOfReenterings);
            parallelizer.execute(runnable);
            
            assertEquals(0, runnable.nbrOfReenterings);
        }
    }

    public void test_reentrant_splittable() {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            if (parallelizer.isReentrant()) {
                test_reentrant_splittable(parallelizer);
            }
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_reentrant_splittable(InterfaceParallelizerForTests parallelizer) {
        if (DEBUG) {
            Dbg.log("test_reentrant_splittable(" + computeDescr(parallelizer) + ")");
        }
        
        final Random random = new Random(123456789L);
        
        // Rather large, to allow for many splits.
        final int n = 10 * parallelizer.getParallelism();
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final AtomicInteger sum = new AtomicInteger();
            final MyReentrantSp sp = new MyReentrantSp(
                    random,
                    parallelizer,
                    n,
                    sum);
            parallelizer.execute(sp);
            
            final int expected = n;
            final int actual = sum.get();
            if (DEBUG) {
                Dbg.log("expected = " + expected + ", actual = " + actual);
            }
            assertEquals(expected, actual);
        }
        
        this.cu.assertNoError();
    }
    
    public void test_reentrant_splitmergable() {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            if (parallelizer.isReentrant()) {
                test_reentrant_splitmergable(parallelizer);
            }
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_reentrant_splitmergable(InterfaceParallelizerForTests parallelizer) {
        if (DEBUG) {
            Dbg.log("test_reentrant_splitmergable(" + computeDescr(parallelizer) + ")");
        }
        
        final Random random = new Random(123456789L);
        
        // Rather large, to allow for many splits.
        final int n = 10 * parallelizer.getParallelism();
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final MyReentrantSpm spm = new MyReentrantSpm(
                    random,
                    parallelizer,
                    n);
            parallelizer.execute(spm);
            
            final int expected = n;
            final int actual = spm.result;
            if (DEBUG) {
                Dbg.log("expected = " + expected + ", actual = " + actual);
            }
            assertEquals(expected, actual);
        }
        
        this.cu.assertNoError();
    }
    
    /*
     * 
     */
    
    public void test_throwables_runnable() {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            test_throwables_runnable(parallelizer);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_throwables_runnable(InterfaceParallelizerForTests parallelizer) {
        if (DEBUG) {
            Dbg.log("test_throwables_runnable(" + computeDescr(parallelizer) + ")");
        }
        
        final MyThrower thrower = new MyThrower();

        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final MyThrowingRunnable runnable = new MyThrowingRunnable(thrower);
            try {
                parallelizer.execute(runnable);
            } catch (Throwable e) {
                checkIsOrContainsMyThrowable(e);
            }
        }
    }
    
    public void test_throwables_splittable() {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            test_throwables_splittable(parallelizer);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_throwables_splittable(InterfaceParallelizerForTests parallelizer) {
        if (DEBUG) {
            Dbg.log("test_throwables_splittable(" + computeDescr(parallelizer) + ")");
        }
        
        final MyThrower thrower = new MyThrower();

        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final MyThrowingSp sp = new MyThrowingSp(
                    FIBO_MIN_SEQ_N,
                    FIBO_MAX_N,
                    thrower);
            try {
                parallelizer.execute(sp);
            } catch (Throwable e) {
                final Throwable cause;
                if (e.getClass() == RethrowException.class) {
                    cause = ((RethrowException) e).getCause();
                } else {
                    cause = e;
                }
                checkIsOrContainsMyThrowable(cause);
            }
        }
    }
    
    public void test_throwables_splitmergable() {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            test_throwables_splitmergable(parallelizer);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_throwables_splitmergable(InterfaceParallelizerForTests parallelizer) {
        if (DEBUG) {
            Dbg.log("test_throwables_splitmergable(" + computeDescr(parallelizer) + ")");
        }
        
        final MyThrower thrower = new MyThrower();

        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final MyThrowingSpm spm = new MyThrowingSpm(
                    FIBO_MIN_SEQ_N,
                    FIBO_MAX_N,
                    thrower);
            try {
                parallelizer.execute(spm);
            } catch (Throwable e) {
                final Throwable cause;
                if (e.getClass() == RethrowException.class) {
                    cause = ((RethrowException) e).getCause();
                } else {
                    cause = e;
                }
                checkIsOrContainsMyThrowable(cause);
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_interrupt() {
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList()) {
            test_interrupt(parallelizer);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_interrupt(InterfaceParallelizerForTests parallelizer) {
        if (DEBUG) {
            Dbg.log("test_interrupt(" + computeDescr(parallelizer) + ")");
        }

        final int n = FIBO_MAX_N;
        final int expected = fiboSeq(n);
        
        for (boolean mustInterruptInConstructorElseRun : new boolean[]{true,false}) {
            for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
                if (DEBUG) {
                    Dbg.log();
                    Dbg.log("k = " + k);
                }

                final MyInterruptingFiboSpm spm = new MyInterruptingFiboSpm(
                        FIBO_MIN_SEQ_N,
                        n,
                        mustInterruptInConstructorElseRun);

                parallelizer.execute(spm);
                
                if (mustInterruptInConstructorElseRun) {
                    // Current thread got interrupted before
                    // parallelization started.
                    // Checking that parallelization didn't clear
                    // interrupt status.
                    assertTrue(Thread.currentThread().isInterrupted());
                } else {
                    // {split/run/merge}, and therefore interrupts,
                    // might have been done only in worker threads.
                }

                // Clearing eventual interrupt status.
                Thread.interrupted();

                final int actual = spm.result;
                if (DEBUG) {
                    Dbg.log("not expected = " + expected + ", actual = " + actual);
                }
                if (actual == expected) {
                    throw new AssertionError("" + actual);
                }
            }
        }
    }

    /*
     * 
     */
    
    public void test_exceptionHandler_splittable() {
        final MyExceptionHandler exceptionHandler = new MyExceptionHandler();
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList(exceptionHandler)) {
            test_exceptionHandler_splittable(parallelizer, exceptionHandler);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_exceptionHandler_splittable(
            InterfaceParallelizerForTests parallelizer,
            MyExceptionHandler exceptionHandler) {
        if (DEBUG) {
            Dbg.log("test_exceptionHandler_splittable(" + computeDescr(parallelizer) + ")");
        }
        
        // Not throwing from worthToSplit(),
        // which is called early before eventual parallelization,
        // so that we can assume that any thrown exception get handled.
        final MyThrower thrower_worthToSplit = new MyThrower(0.0);
        final MyThrower thrower = new MyThrower();

        boolean didThrowAtLeastOnce = false;
        boolean didHandleAtLeastOnce = false;

        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final MyThrowingSp sp = new MyThrowingSp(
                    FIBO_MIN_SEQ_N,
                    FIBO_MAX_N,
                    thrower_worthToSplit,
                    thrower);
            
            exceptionHandler.firstHandledRef.set(null);
            
            boolean didThrow = false;
            try {
                parallelizer.execute(sp);
            } catch (Throwable e) {
                didThrow = true;
                didThrowAtLeastOnce = true;
                final Throwable cause;
                if (e.getClass() == RethrowException.class) {
                    cause = ((RethrowException) e).getCause();
                } else {
                    cause = e;
                }
                checkIsOrContainsMyThrowable(cause);
            }
            
            final Throwable handled = exceptionHandler.firstHandledRef.get();
            assertEquals(didThrow, (handled != null));
            if (handled != null) {
                didHandleAtLeastOnce = true;
                checkIsOrContainsMyThrowable(handled);
            }
        }
        
        assertTrue(didThrowAtLeastOnce);
        assertTrue(didHandleAtLeastOnce);
    }
    
    public void test_exceptionHandler_splitmergable() {
        final MyExceptionHandler exceptionHandler = new MyExceptionHandler();
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList(exceptionHandler)) {
            test_exceptionHandler_splitmergable(parallelizer, exceptionHandler);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_exceptionHandler_splitmergable(
            InterfaceParallelizerForTests parallelizer,
            MyExceptionHandler exceptionHandler) {
        if (DEBUG) {
            Dbg.log("test_exceptionHandler_splitmergable(" + computeDescr(parallelizer) + ")");
        }
        
        // Not throwing from worthToSplit(),
        // which is called early before eventual parallelization,
        // so that we can assume that any thrown exception get handled.
        final MyThrower thrower_worthToSplit = new MyThrower(0.0);
        final MyThrower thrower = new MyThrower();
        
        boolean didThrowAtLeastOnce = false;
        boolean didHandleAtLeastOnce = false;

        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            if (DEBUG) {
                Dbg.log();
                Dbg.log("k = " + k);
            }
            
            final MyThrowingSpm spm = new MyThrowingSpm(
                    FIBO_MIN_SEQ_N,
                    FIBO_MAX_N,
                    thrower_worthToSplit,
                    thrower);
            
            exceptionHandler.firstHandledRef.set(null);
            
            boolean didThrow = false;
            try {
                parallelizer.execute(spm);
            } catch (Throwable e) {
                didThrow = true;
                didThrowAtLeastOnce = true;
                final Throwable cause;
                if (e.getClass() == RethrowException.class) {
                    cause = ((RethrowException) e).getCause();
                } else {
                    cause = e;
                }
                checkIsOrContainsMyThrowable(cause);
            }
            
            final Throwable handled = exceptionHandler.firstHandledRef.get();
            assertEquals(didThrow, (handled != null));
            if (handled != null) {
                didHandleAtLeastOnce = true;
                checkIsOrContainsMyThrowable(handled);
            }
        }
        
        assertTrue(didThrowAtLeastOnce);
        assertTrue(didHandleAtLeastOnce);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
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
    
    /**
     * @return True if the specified throwable is or directly contains a Mythrowable.
     */
    private static boolean isOrContainsMyThrowable(Throwable t) {
        if (t instanceof MyThrowable) {
            return true;
        } else if ((t != null)
                && (t.getCause() instanceof MyThrowable)) {
            return true;
        }
        return false;
    }
    
    /**
     * Method useful because ThreadPoolExecutor can wrap an Error around
     * Throwables to rethrow them.
     * 
     * Checks that the specified throwable is or directly contains a MyThrowable.
     */
    private static void checkIsOrContainsMyThrowable(Throwable t) {
        if (isOrContainsMyThrowable(t)) {
            // Fine.
            return;
        }
        assertEquals(MyThrowable.class, t.getClass());
    }
}
