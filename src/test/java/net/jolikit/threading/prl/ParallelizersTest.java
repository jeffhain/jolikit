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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.RethrowException;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.ConcUnit;
import net.jolikit.test.utils.TestUtils;
import net.jolikit.threading.basics.CancellableUtils;
import net.jolikit.threading.execs.FixedThreadExecutor;

/**
 * Basic tests for parallelizers.
 */
public class ParallelizersTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_RUNNABLES = false;
    
    /**
     * Actual parallelism is this +1,
     * since user thread is used to run tasks.
     */
    private static final int DEFAULT_PARALLELISM = 3;
    private static final int DEFAULT_DISCREPANCY = 3;
    
    /*
     * 
     */
    
    private static final int FIBO_MIN_SEQ_N = 3;
    private static final int FIBO_MAX_N = 13;
    
    private static final int NBR_OF_RUNS_PER_CASE = 10 * 1000;
    /**
     * Must be large enough to cover all interesting cases.
     */
    private static final int NBR_OF_RUNS_PER_CASE_WHEN_SPLITS_PLUS_EXCEPTIONS = 50 * 1000;
    
    /*
     * 
     */
    
    /**
     * For exceptions tests.
     * Must not be too high else, if too many splits and not enough runs,
     * might not have runs with no or just a few exceptions.
     * Must not be too low else, if too few splits and not enough runs,
     * might not have runs with some cases of multiple exceptions.
     */
    private static final double DEFAULT_EXCEPTION_PROBABILITY = 0.05;
    
    /*
     * 
     */
    
    /**
     * @return A new list of parallelizers to test.
     */
    private static List<InterfaceParallelizerForTests> newParallelizerList() {
        return newParallelizerList(null);
    }
    
    /**
     * @return A new list of parallelizers to test.
     */
    private static List<InterfaceParallelizerForTests> newParallelizerList(
        final UncaughtExceptionHandler exceptionHandler) {
        final double rejectionProba = 0.0;
        return newParallelizerList(
            exceptionHandler,
            rejectionProba);
    }
    
    /**
     * @return A new list of parallelizers to test.
     */
    private static List<InterfaceParallelizerForTests> newParallelizerList(
        final UncaughtExceptionHandler exceptionHandler,
        final double rejectionProba) {
        
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
                        } catch (Throwable e) {
                            if (isOrContainsThrownByTests(e)) {
                                // We throw that: quiet.
                            } else {
                                Unchecked.throwIt(e);
                            }
                        }
                    }
                };
                return new Thread(wrapper);
            }
        };
        
        final Random random = TestUtils.newRandom123456789L();
        
        final List<InterfaceParallelizerForTests> ret =
            new ArrayList<>();
        if (true) {
            /*
             * Executor rejecting by throwing RejectedExecutionException.
             */
            final Executor executor =
                new ThreadPoolExecutor(
                    DEFAULT_PARALLELISM,
                    DEFAULT_PARALLELISM,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    threadFactory) {
                @Override
                public void execute(Runnable command) {
                    if ((rejectionProba != 0.0)
                        && (random.nextDouble() < rejectionProba)) {
                        throw new RejectedExecutionException();
                    } else {
                        super.execute(command);
                    }
                }
            };
            ret.add(new ExecutorParallelizerForTests(
                executor,
                DEFAULT_PARALLELISM,
                DEFAULT_DISCREPANCY,
                exceptionHandler));
        }
        if (true) {
            /*
             * Executor rejecting by calling onCancel() if cancellable,
             * else by throwing RejectedExecutionException.
             */
            final int maxWorkerCountForBasicQueue = 4;
            final Executor executor =
                new FixedThreadExecutor(
                    "HeEpTest",
                    true,
                    DEFAULT_PARALLELISM,
                    Integer.MAX_VALUE,
                    maxWorkerCountForBasicQueue,
                    threadFactory) {
                @Override
                public void execute(Runnable command) {
                    if ((rejectionProba != 0.0)
                        && (random.nextDouble() < rejectionProba)) {
                        CancellableUtils.call_onCancel_IfCancellableElseThrowREE(command);
                    } else {
                        super.execute(command);
                    }
                }
            };
            ret.add(new ExecutorParallelizerForTests(
                executor,
                DEFAULT_PARALLELISM,
                DEFAULT_DISCREPANCY,
                exceptionHandler));
        }
        return ret;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Non-blocking fixed-capacity list with thread-safe add(E),
     * for use as "instanceList".
     * Should cause much less contention than a Vector.
     */
    private static class MyConcurrentAddList<E> extends AbstractList<E> {
        private final Object[] arr;
        private final AtomicInteger size = new AtomicInteger();
        public MyConcurrentAddList(int capacity) {
            this.arr = new Object[capacity];
        }
        @Override
        public void clear() {
            this.size.set(0);
        }
        @Override
        public int size() {
            return this.size.get();
        }
        @Override
        public boolean add(E element) {
            final int index = this.size.getAndIncrement();
            this.arr[index] = element;
            return true;
        }
        @Override
        public E get(int index) {
            @SuppressWarnings("unchecked")
            final E e = (E) this.arr[index];
            return e;
        }
    }
    
    /*
     * Abstract SP/SPM for debug/logs.
     */
    
    private static abstract class MyAbstractSpx implements InterfaceSplittable {
        final int shotId;
        int depth;
        final StringBuilder leftRightSb = new StringBuilder();
        int splitOkCount = 0;
        int runCount = 0;
        private List<MyAbstractSpx> instanceList = null;
        public MyAbstractSpx(
            int shotId,
            int depth) {
            this.shotId = shotId;
            this.depth = depth;
        }
        /**
         * Optional list, where this instance and all split instances are added.
         * Useful to retrieve all used (created) instances and check their data.
         */
        public void setInstanceListAndAddThis(List<MyAbstractSpx> instanceList) {
            this.instanceList = instanceList;
            instanceList.add(this);
        }
        @Override
        public String toString() {
            return "[s=" + this.shotId
                + ",d=" + this.depth
                + "," + this.leftRightSb.toString()
                + "]";
        }
        @Override
        public final boolean worthToSplit() {
            if (DEBUG_RUNNABLES) {
                Dbg.log(this + " : worthToSplit()");
            }
            final boolean ret = this.worthToSplitImpl();
            if (DEBUG_RUNNABLES) {
                Dbg.log(this + " : worthToSplit() : ret = " + ret);
            }
            return ret;
        }
        @Override
        public InterfaceSplittable split() {
            if (DEBUG_RUNNABLES) {
                Dbg.log(this + " : split()");
            }
            final MyAbstractSpx ret =
                (MyAbstractSpx) this.splitImpl(this.depth + 1);
            // splitImpl() did not throw: split is OK.
            this.depth++;
            ret.leftRightSb.setLength(0);
            ret.leftRightSb.append(this.leftRightSb);
            ret.leftRightSb.append('R');
            this.leftRightSb.append('L');
            this.splitOkCount++;
            if (this.instanceList != null) {
                ret.setInstanceListAndAddThis(this.instanceList);
            }
            return ret;
        }
        @Override
        public final void run() {
            if (DEBUG_RUNNABLES) {
                Dbg.log(this + " : run()");
            }
            this.runCount++;
            this.runImpl();
        }
        protected abstract boolean worthToSplitImpl();
        protected abstract InterfaceSplittable splitImpl(int newDepth);
        protected abstract void runImpl();
    }
    
    private static abstract class MyAbstractSp extends MyAbstractSpx {
        public MyAbstractSp(
            int shotId,
            int depth) {
            super(shotId, depth);
        }
        @Override
        public final InterfaceSplittable split() {
            return super.split();
        }
    }

    private static abstract class MyAbstractSpm extends MyAbstractSpx implements InterfaceSplitmergable {
        int mergeCount = 0;
        public MyAbstractSpm(
            int shotId,
            int depth) {
            super(shotId, depth);
        }
        @Override
        public final InterfaceSplitmergable split() {
            return (InterfaceSplitmergable) super.split();
        }
        @Override
        public final void merge(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            if (DEBUG_RUNNABLES) {
                Dbg.log(this + " : merge(" + a + "," + b + ")");
            }
            // Merge counts even if it throws,
            // so updating fields before.
            this.depth--;
            this.leftRightSb.setLength(this.leftRightSb.length() - 1);
            this.mergeCount++;
            this.mergeImpl(a, b);
            if (DEBUG_RUNNABLES) {
                Dbg.log(this + " (after merge)");
            }
        }
        protected abstract void mergeImpl(InterfaceSplitmergable a, InterfaceSplitmergable b);
    }

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
    
    private static class MyFiboSp extends MyAbstractSp {
        final int minSeqN;
        int n;
        private final AtomicInteger result;
        /**
         * @param result If not null, result is added to it.
         */
        public MyFiboSp(
            int shotId,
            int depth,
            int minSeqN,
            int n,
            AtomicInteger result) {
            super(shotId, depth);
            this.minSeqN = minSeqN;
            this.n = n;
            this.depth = depth;
            this.result = result;
        }
        @Override
        public String toString() {
            return super.toString() + "[n=" + this.n + ",result=" + this.result + "]";
        }
        @Override
        protected boolean worthToSplitImpl() {
            return (this.n > this.minSeqN);
        }
        @Override
        protected InterfaceSplittable splitImpl(int newDepth) {
            final int oldN = this.n;
            this.n = oldN - 1;
            return new MyFiboSp(
                this.shotId,
                newDepth,
                this.minSeqN,
                oldN - 2,
                this.result);
        }
        @Override
        protected void runImpl() {
            if (this.result != null) {
                final int res = fiboSeq(this.n);
                this.result.addAndGet(res);
            }
        }
    }
    
    private static class MyFiboSpm extends MyAbstractSpm {
        final int minSeqN;
        int n;
        int result;
        public MyFiboSpm(
            int shotId,
            int depth,
            int minSeqN,
            int n) {
            super(shotId, depth);
            this.minSeqN = minSeqN;
            this.n = n;
            this.depth = depth;
        }
        @Override
        public String toString() {
            return super.toString() + "[n=" + this.n + ",result=" + this.result + "]";
        }
        @Override
        protected boolean worthToSplitImpl() {
            return (this.n > this.minSeqN);
        }
        @Override
        protected InterfaceSplitmergable splitImpl(int newDepth) {
            final int oldN = this.n;
            this.n = oldN - 1;
            return new MyFiboSpm(
                this.shotId,
                newDepth,
                this.minSeqN,
                oldN - 2);
        }
        @Override
        protected void runImpl() {
            this.result = fiboSeq(this.n);
        }
        @Override
        protected void mergeImpl(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            final MyFiboSpm aImpl = (MyFiboSpm) a;
            final MyFiboSpm bImpl = (MyFiboSpm) b;
            final int aRes = ((aImpl == null) ? 0 : aImpl.result);
            final int bRes = ((bImpl == null) ? 0 : bImpl.result);
            this.result = aRes + bRes;
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
        public String toString() {
            return "MyReentrantRunnable";
        }
        @Override
        public void run() {
            if (DEBUG_RUNNABLES) {
                Dbg.logPr(this + " : run() : nbrOfReenterings = " + this.nbrOfReenterings);
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
    
    private class MyReentrantSp extends MyAbstractSp {
        /**
         * For random split and reentrancy patterns.
         */
        final Random random;
        final InterfaceParallelizer parallelizer;
        int n;
        /**
         * Must be n.
         */
        private final AtomicInteger result;
        /**
         * Uses ConcUnit, so must call ConcUnit.assertNoError() at test end.
         * @param result Must not be null.
         */
        public MyReentrantSp(
            int shotId,
            int depth,
            Random random,
            InterfaceParallelizer parallelizer,
            int n,
            AtomicInteger result) {
            super(shotId, depth);
            this.random = random;
            this.parallelizer = parallelizer;
            this.n = n;
            this.result = result;
        }
        @Override
        protected boolean worthToSplitImpl() {
            return (this.n >= 2) && this.random.nextBoolean();
        }
        @Override
        protected InterfaceSplittable splitImpl(int newDepth) {
            final int halfish = this.n / 2;
            this.n -= halfish;
            return new MyReentrantSp(
                this.shotId,
                newDepth,
                this.random,
                this.parallelizer,
                halfish,
                this.result);
        }
        @Override
        protected void runImpl() {
            // Often going reentrant, to help testing against deadlocks,
            // but not always else can get call stack overflow.
            final boolean mustReenter = (this.random.nextDouble() < 0.9);
            if (DEBUG_RUNNABLES) {
                Dbg.log(this + " : run() : mustReenter = " + mustReenter);
            }
            final int res;
            if (mustReenter) {
                final AtomicInteger innerSum = new AtomicInteger();
                final MyReentrantSp innerSp = new MyReentrantSp(
                    this.shotId + 1,
                    0,
                    this.random,
                    this.parallelizer,
                    this.n,
                    innerSum);
                if (DEBUG_RUNNABLES) {
                    Dbg.log(this + " : run() : execute... innerSp.n = " + innerSp.n);
                }
                this.parallelizer.execute(innerSp);
                final int innerSumValue = innerSum.get();
                if (DEBUG_RUNNABLES) {
                    Dbg.log(this + " : run() : ...execute innerSumValue = " + innerSumValue);
                }
                // Tests that execute(...) blocks until parallelization
                // completion.
                cu.assertEquals(this.n, innerSumValue);
                res = innerSumValue;
            } else {
                res = this.n;
            }
            this.result.addAndGet(res);
        }
    }
    
    private class MyReentrantSpm extends MyAbstractSpm {
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
            int shotId,
            int depth,
            Random random,
            InterfaceParallelizer parallelizer,
            int n) {
            super(shotId, depth);
            this.random = random;
            this.parallelizer = parallelizer;
            this.n = n;
        }
        @Override
        protected boolean worthToSplitImpl() {
            return (this.n >= 2) && this.random.nextBoolean();
        }
        @Override
        protected InterfaceSplitmergable splitImpl(int newDepth) {
            final int halfish = this.n / 2;
            this.n -= halfish;
            return new MyReentrantSpm(
                this.shotId,
                newDepth,
                this.random,
                this.parallelizer,
                halfish);
        }
        @Override
        protected void runImpl() {
            // Often going reentrant, to help testing against deadlocks,
            // but not always else can get call stack overflow.
            final boolean mustReenter = (this.random.nextDouble() < 0.9);
            if (DEBUG_RUNNABLES) {
                Dbg.log(this + " : run() : mustReenter = " + mustReenter);
            }
            if (mustReenter) {
                final MyReentrantSpm innerSpm = new MyReentrantSpm(
                    this.shotId + 1,
                    0,
                    this.random,
                    this.parallelizer,
                    this.n);
                if (DEBUG_RUNNABLES) {
                    Dbg.log(this + " : run() : execute... innerSpm.n = " + innerSpm.n);
                }
                this.parallelizer.execute(innerSpm);
                if (DEBUG_RUNNABLES) {
                    Dbg.log(this + " : run() : ...execute innerSpm.result = " + innerSpm.result);
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
        protected void mergeImpl(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            final MyReentrantSpm aImpl = (MyReentrantSpm) a;
            final MyReentrantSpm bImpl = (MyReentrantSpm) b;
            final int aRes = ((aImpl == null) ? 0 : aImpl.result);
            final int bRes = ((bImpl == null) ? 0 : bImpl.result);
            this.result = aRes + bRes;
        }
    }
    
    /*
     * For exceptions test.
     */
    
    private static class MyThrowable extends Throwable {
        private static final long serialVersionUID = 1L;
        final Object thrower;
        public MyThrowable(
            Object thrower,
            String message) {
            super(message);
            this.thrower = thrower;
        }
    }
    
    private static class MyThrower {
        private final int shotId;
        private final double exceptionProbability;
        private final AtomicReference<Throwable> firstThrown = new AtomicReference<Throwable>();
        private final Random random;
        public MyThrower(int shotId) {
            this(shotId, DEFAULT_EXCEPTION_PROBABILITY);
        }
        public MyThrower(
            int shotId,
            double exceptionProbability) {
            this.shotId = shotId;
            this.exceptionProbability = exceptionProbability;
            this.random = new Random(shotId);
        }
        @Override
        public String toString() {
            return "[s=" + this.shotId + "]";
        }
        private void throwOrNot(String methodName) {
            if (random.nextDouble() < this.exceptionProbability) {
                final Object thrower = this;
                final MyThrowable e = new MyThrowable(
                    thrower,
                    this + " throwing from "
                        + methodName
                        + " in "
                        + Thread.currentThread());
                final boolean didSet =
                    this.firstThrown.compareAndSet(null, e);
                if (DEBUG_RUNNABLES) {
                    Dbg.log("(first=" + didSet + ") " + e.getMessage());
                }
                Unchecked.throwIt(e);
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
    
    private static class MyThrowingSp extends MyAbstractSp {
        final MyThrower thrower;
        public MyThrowingSp(
            int shotId,
            int depth,
            MyThrower thrower) {
            super(shotId, depth);
            this.thrower = thrower;
        }
        @Override
        protected boolean worthToSplitImpl() {
            thrower.throwOrNot("worthToSplit()");
            return true;
        }
        @Override
        protected InterfaceSplittable splitImpl(int newDepth) {
            thrower.throwOrNot("split()");
            return new MyThrowingSp(
                this.shotId,
                newDepth,
                this.thrower);
        }
        @Override
        protected void runImpl() {
            thrower.throwOrNot("run()");
        }
    }

    private static class MyThrowingSpm extends MyAbstractSpm {
        final MyThrower thrower;
        public MyThrowingSpm(
            int shotId,
            int depth,
            MyThrower thrower) {
            super(shotId, depth);
            this.thrower = thrower;
        }
        @Override
        protected boolean worthToSplitImpl() {
            thrower.throwOrNot("worthToSplit()");
            return true;
        }
        @Override
        protected InterfaceSplitmergable splitImpl(int newDepth) {
            thrower.throwOrNot("split()");
            return new MyThrowingSpm(
                this.shotId,
                newDepth,
                this.thrower);
        }
        @Override
        protected void runImpl() {
            thrower.throwOrNot("run()");
        }
        @Override
        protected void mergeImpl(InterfaceSplitmergable a, InterfaceSplitmergable b) {
            thrower.throwOrNot("merge(...)");
        }
    }
    
    /*
     * For interrupt test.
     */
    
    private class MyInterruptingFiboSpm extends MyFiboSpm {
        private final boolean mustInterruptInConstructorElseRun;
        public MyInterruptingFiboSpm(
            int shotId,
            int depth,
            int minSeqN,
            int n,
            boolean mustInterruptInConstructorElseRun) {
            super(shotId, depth, minSeqN, n);
            this.mustInterruptInConstructorElseRun = mustInterruptInConstructorElseRun;
            if (this.mustInterruptInConstructorElseRun) {
                Thread.currentThread().interrupt();
            }
        }
        @Override
        protected InterfaceSplitmergable splitImpl(int newDepth) {
            final int oldN = this.n;
            this.n = oldN - 1;
            return new MyInterruptingFiboSpm(
                this.shotId,
                newDepth,
                this.minSeqN,
                oldN - 2,
                this.mustInterruptInConstructorElseRun);
        }
        @Override
        protected void runImpl() {
            if (Thread.currentThread().isInterrupted()) {
                /*
                 * Not running.
                 */
            } else {
                super.runImpl();
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
            final boolean didSet = this.firstHandledRef.compareAndSet(null, e);
            if (didSet) {
                if (DEBUG) {
                    Dbg.log("firstHandled : " + e.getMessage());
                }
            }
            /*
             * Does rethrow: the parallelizer must be robust to that.
             * so that parallelization still terminate (doesn't block),
             * even if computations are aborted.
             */
            Unchecked.throwIt(e);
        }
    }
    
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
        @SuppressWarnings("unused")
        Object o;
        
        try {
            o = new ExecutorParallelizer(null, 1, 0);
            fail();
        } catch (@SuppressWarnings("unused") NullPointerException e) {
            // ok
        }
        
        final Executor executor = Executors.newSingleThreadExecutor();
        
        for (int badParallelism : new int[]{Integer.MIN_VALUE, 0}) {
            try {
                o = new ExecutorParallelizer(executor, badParallelism, 0);
                fail();
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                // ok
            }
        }
        
        for (int badMaxDepth : new int[]{Integer.MIN_VALUE, -1}) {
            try {
                o = new ExecutorParallelizer(executor, 1, badMaxDepth);
                fail();
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                // ok
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
        
        int shotId = 0;
        
        for (int n = FIBO_MIN_SEQ_N; n <= FIBO_MAX_N; n++) {
            final int expected = fiboSeq(n);
            
            for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
                shotId++;
                if (DEBUG) {
                    Dbg.log();
                    Dbg.log("n = " + n);
                    Dbg.log("shotId = " + shotId);
                }
                
                final AtomicInteger result = new AtomicInteger();
                final MyFiboSp sp = new MyFiboSp(
                    shotId,
                    0,
                    FIBO_MIN_SEQ_N,
                    n,
                    result);
                parallelizer.execute(sp);
                
                final int actual = result.get();
                if (DEBUG) {
                    Dbg.log("expected = " + expected + ", actual = " + actual);
                }
                assertEquals(expected, actual);
            }
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
        
        int shotId = 0;
        
        for (int n = FIBO_MIN_SEQ_N; n <= FIBO_MAX_N; n++) {
            final int expected = fiboSeq(n);
            
            for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
                shotId++;
                if (DEBUG) {
                    Dbg.log();
                    Dbg.log("n = " + n);
                    Dbg.log("shotId = " + shotId);
                }
                
                final MyFiboSpm spm = new MyFiboSpm(
                    shotId,
                    0,
                    FIBO_MIN_SEQ_N,
                    n);
                parallelizer.execute(spm);
                
                final int actual = spm.result;
                if (DEBUG) {
                    Dbg.log("expected = " + expected + ", actual = " + actual);
                }
                assertEquals(expected, actual);
            }
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
        
        final Random random = TestUtils.newRandom123456789L();
        
        // Rather large, to allow for many splits.
        final int n = 10 * parallelizer.getParallelism();
        
        int shotId = 0;
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            shotId++;
            if (DEBUG) {
                Dbg.log();
                Dbg.log("shotId = " + shotId);
            }
            
            final AtomicInteger result = new AtomicInteger();
            final MyReentrantSp sp = new MyReentrantSp(
                shotId,
                0,
                random,
                parallelizer,
                n,
                result);
            parallelizer.execute(sp);
            
            final int expected = n;
            final int actual = result.get();
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
        
        final Random random = TestUtils.newRandom123456789L();
        
        // Rather large, to allow for many splits.
        final int n = 10 * parallelizer.getParallelism();
        
        int shotId = 0;
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            shotId++;
            if (DEBUG) {
                Dbg.log();
                Dbg.log("shotId = " + shotId);
            }
            
            final MyReentrantSpm spm = new MyReentrantSpm(
                shotId,
                0,
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
        
        int shotId = 0;
        
        boolean didCompleteNormallyAtLeastOnce = false;
        boolean didCompleteExceptionallyAtLeastOnce = false;
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
            shotId++;
            if (DEBUG) {
                Dbg.log();
                Dbg.log("shotId = " + shotId);
            }
            
            final MyThrower thrower = new MyThrower(shotId);
            final MyThrowingRunnable runnable = new MyThrowingRunnable(thrower);
            try {
                parallelizer.execute(runnable);
                didCompleteNormallyAtLeastOnce = true;
            } catch (Throwable e) {
                didCompleteExceptionallyAtLeastOnce = true;
                checkIsOrContainsThrownByTests(e);
            }
        }
        
        assertTrue(didCompleteNormallyAtLeastOnce);
        assertTrue(didCompleteExceptionallyAtLeastOnce);
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
        
        boolean didCompleteNormallyAtLeastOnce = false;
        boolean didCompleteExceptionallyAtLeastOnce = false;
        
        int shotId = 0;
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE_WHEN_SPLITS_PLUS_EXCEPTIONS; k++) {
            shotId++;
            if (DEBUG) {
                Dbg.log();
                Dbg.log("shotId = " + shotId);
            }
            
            final MyThrower thrower = new MyThrower(shotId);
            
            final MyThrowingSp sp = new MyThrowingSp(
                shotId,
                0,
                thrower);
            try {
                parallelizer.execute(sp);
                didCompleteNormallyAtLeastOnce = true;
            } catch (Throwable e) {
                didCompleteExceptionallyAtLeastOnce = true;
                checkIsOrContainsThrownByTests(e);
            }
        }
        
        assertTrue(didCompleteNormallyAtLeastOnce);
        assertTrue(didCompleteExceptionallyAtLeastOnce);
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
        
        int shotId = 0;
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE_WHEN_SPLITS_PLUS_EXCEPTIONS; k++) {
            shotId++;
            if (DEBUG) {
                Dbg.log();
                Dbg.log("shotId= " + shotId);
            }
            
            final MyThrower thrower = new MyThrower(shotId);
            
            final MyThrowingSpm spm = new MyThrowingSpm(
                shotId,
                0,
                thrower);
            try {
                parallelizer.execute(spm);
            } catch (Throwable e) {
                checkIsOrContainsThrownByTests(e);
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
        
        int shotId = 0;
        
        for (boolean mustInterruptInConstructorElseRun : new boolean[]{true,false}) {
            for (int k = 0; k < NBR_OF_RUNS_PER_CASE; k++) {
                shotId++;
                if (DEBUG) {
                    Dbg.log();
                    Dbg.log("shotId = " + shotId);
                }
                
                final MyInterruptingFiboSpm spm = new MyInterruptingFiboSpm(
                    shotId,
                    0,
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
    
    public void test_exceptionHandling_splittable_noCheckForExhaustiveWork() {
        this.test_exceptionHandling_splittable_xxx(false);
    }
    
    public void test_exceptionHandling_splittable_withCheckForExhaustiveWork() {
        this.test_exceptionHandling_splittable_xxx(true);
    }
    
    /**
     * @param mustCheckForExhaustiveWork Want to also test without,
     *        because causes additional memory barriers.
     */
    public void test_exceptionHandling_splittable_xxx(boolean mustCheckForExhaustiveWork) {
        final double rejectionProba = DEFAULT_EXCEPTION_PROBABILITY;
        final MyExceptionHandler exceptionHandler = new MyExceptionHandler();
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList(
            exceptionHandler,
            rejectionProba)) {
            test_exceptionHandling_splittable(
                mustCheckForExhaustiveWork,
                parallelizer,
                exceptionHandler);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_exceptionHandling_splittable(
        boolean mustCheckForExhaustiveWork,
        InterfaceParallelizerForTests parallelizer,
        MyExceptionHandler exceptionHandler) {
        if (DEBUG) {
            Dbg.log("test_exceptionHandling_splittable("
                + mustCheckForExhaustiveWork
                + ","
                + computeDescr(parallelizer)
                + ")");
        }
        
        final boolean spmElseSp = false;
        this.test_exceptionHandling(
            spmElseSp,
            mustCheckForExhaustiveWork,
            parallelizer,
            exceptionHandler);
    }
    
    public void test_exceptionHandling_splitmergable_noCheckForExhaustiveWork() {
        this.test_exceptionHandling_splitmergable_xxx(false);
    }
    
    public void test_exceptionHandling_splitmergable_withCheckForExhaustiveWork() {
        this.test_exceptionHandling_splitmergable_xxx(true);
    }
    
    /**
     * @param mustCheckForExhaustiveWork Want to also test without,
     *        because causes additional memory barriers.
     */
    public void test_exceptionHandling_splitmergable_xxx(boolean mustCheckForExhaustiveWork) {
        final double rejectionProba = DEFAULT_EXCEPTION_PROBABILITY;
        final MyExceptionHandler exceptionHandler = new MyExceptionHandler();
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList(
            exceptionHandler,
            rejectionProba)) {
            test_exceptionHandling_splitmergable(
                mustCheckForExhaustiveWork,
                parallelizer,
                exceptionHandler);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_exceptionHandling_splitmergable(
        boolean mustCheckForExhaustiveWork,
        InterfaceParallelizerForTests parallelizer,
        MyExceptionHandler exceptionHandler) {
        if (DEBUG) {
            Dbg.log("test_exceptionHandling_splitmergable("
                + mustCheckForExhaustiveWork
                + ","
                + computeDescr(parallelizer)
                + ")");
        }
        
        final boolean spmElseSp = true;
        this.test_exceptionHandling(
            spmElseSp,
            mustCheckForExhaustiveWork,
            parallelizer,
            exceptionHandler);
    }

    public void test_exceptionHandling(
        boolean spmElseSp,
        boolean mustCheckForExhaustiveWork,
        InterfaceParallelizerForTests parallelizer,
        MyExceptionHandler exceptionHandler) {
        
        boolean didDetectAtLeastOnce = false;
        boolean didHandleAtLeastOnce = false;
        
        final List<MyAbstractSpx> instanceList;
        if (mustCheckForExhaustiveWork) {
            final int maxDepth = PrlUtils.computeMaxDepth(
                DEFAULT_PARALLELISM,
                DEFAULT_DISCREPANCY);
            final int spxCountBound = (int) Math.pow(2.0, maxDepth + 1);
            instanceList = new MyConcurrentAddList<>(spxCountBound);
        } else {
            instanceList = null;
        }
        
        int shotId = 0;
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE_WHEN_SPLITS_PLUS_EXCEPTIONS; k++) {
            shotId++;
            if (DEBUG) {
                Dbg.log();
                Dbg.log("shotId = " + shotId);
            }
            
            if (mustCheckForExhaustiveWork) {
                instanceList.clear();
            }
            
            final MyThrower thrower = new MyThrower(shotId);
            
            final MyAbstractSpx spx;
            if (spmElseSp) {
                spx = new MyThrowingSpm(
                    shotId,
                    0,
                    thrower);
            } else {
                spx = new MyThrowingSp(
                    shotId,
                    0,
                    thrower);
            }
            if (mustCheckForExhaustiveWork) {
                spx.setInstanceListAndAddThis(instanceList);
            }
            
            exceptionHandler.firstHandledRef.set(null);
            
            Throwable firstDetected = null;
            try {
                parallelizer.execute(spx);
            } catch (Throwable e) {
                firstDetected = e;
                didDetectAtLeastOnce = true;
            }
            
            if (firstDetected != null) {
                checkIsOrContainsThrownByTests(firstDetected);
                
                final MyThrowable myThrowable = getMyThrowable(firstDetected);
                if (myThrowable != null) {
                    assertSame(thrower, myThrowable.thrower);
                }
            }

            final Throwable firstThrown = thrower.firstThrown.get();
            
            final Throwable firstHandled = exceptionHandler.firstHandledRef.get();
            if (firstHandled != null) {
                didHandleAtLeastOnce = true;
                checkIsOrContainsThrownByTests(firstHandled);
                
                final MyThrowable myThrowable = getMyThrowable(firstHandled);
                if (myThrowable != null) {
                    // Because we don't test with concurrent or reentrant parallelizations.
                    assertSame(thrower, myThrowable.thrower);
                }
            }
            
            if (getRejectedExecutionException(firstDetected) != null) {
                /*
                 * firstThrown can be null or not
                 */
            } else {
                /*
                 * The first exception throw (by tests) might not be
                 * the first exception detected (and rethrown) by the parallelizer,
                 * so just comparing nullity.
                 */
                if ((firstThrown != null) != (firstDetected != null)) {
                    logThrowable("firstThrown", firstThrown);
                    logThrowable("firstDetected", firstDetected);
                    fail();
                }
            }
            
            if (getRejectedExecutionException(firstHandled) != null) {
                /*
                 * firstThrown can be null or not
                 */
            } else {
                /*
                 * If did not throw, nothing must have be fed to UEH.
                 */
                if ((firstThrown == null) && (firstHandled != null)) {
                    logThrowable("firstThrown", firstThrown);
                    logThrowable("firstHandled", firstHandled);
                    fail();
                }
            }
            
            /*
             * We don't feed UEH with first detected (and rethrown).
             */
            if ((firstDetected != null)
                && (firstDetected == firstHandled)) {
                logThrowable("firstDetected,firstHandled", firstDetected);
                logThrowable("firstThrown", firstThrown);
                fail();
            }
            
            /*
             * 
             */
            
            if (mustCheckForExhaustiveWork) {
                final int spxCount = instanceList.size();
                int totalSplitOkCount = 0;
                int totalRunCount = 0;
                int totalMergeCount = 0;
                for (int i = 0; i < spxCount; i++) {
                    final MyAbstractSpx spz = instanceList.get(i);
                    if (spz.runCount != 1) {
                        Dbg.log("spxCount = " + spxCount);
                        Dbg.log("spz[" + i + "] with runCount " + spz.runCount + " : " + spz);
                        fail();
                    }
                    totalSplitOkCount += spz.splitOkCount;
                    totalRunCount += spz.runCount;
                    if (spmElseSp) {
                        final MyThrowingSpm spzImpl = (MyThrowingSpm) spz;
                        totalMergeCount += spzImpl.mergeCount;
                    }
                }
                final int expectedTotalSplitOkCount = spxCount - 1;
                final int expectedTotalRunCount = spxCount;
                final int expectedMinTotalMergeCount = (spmElseSp ? totalSplitOkCount : 0);
                /*
                 * Allowing for one more merge,
                 * due to eventual final merge to copy the result
                 * into the input splitmergable.
                 */
                final int expectedMaxTotalMergeCount = (spmElseSp ? totalSplitOkCount + 1 : 0);
                if ((expectedTotalSplitOkCount != totalSplitOkCount)
                    || (expectedTotalRunCount != totalRunCount)
                    || (expectedMinTotalMergeCount > totalMergeCount)
                    || (expectedMaxTotalMergeCount < totalMergeCount)) {
                    Dbg.log("expectedTotalSplitOkCount = " + expectedTotalSplitOkCount);
                    Dbg.log("totalSplitOkCount =         " + totalSplitOkCount);
                    Dbg.log("expectedTotalRunCount = " + expectedTotalRunCount);
                    Dbg.log("totalRunCount =         " + totalRunCount);
                    Dbg.log("expectedMinTotalMergeCount = " + expectedMinTotalMergeCount);
                    Dbg.log("expectedMaxTotalMergeCount = " + expectedMaxTotalMergeCount);
                    Dbg.log("totalMergeCount =            " + totalMergeCount);
                    fail();
                }
            }
        }
        
        assertTrue(didDetectAtLeastOnce);
        assertTrue(didHandleAtLeastOnce);
    }
    
    /*
     * 
     */
    
    public void test_rejectedExecution_splittable() {
        final double rejectionProba = 0.5;
        final MyExceptionHandler exceptionHandler = new MyExceptionHandler();
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList(
            exceptionHandler,
            rejectionProba)) {
            test_rejectedExecution_splittable(parallelizer, exceptionHandler);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_rejectedExecution_splittable(
        InterfaceParallelizerForTests parallelizer,
        MyExceptionHandler exceptionHandler) {
        if (DEBUG) {
            Dbg.log("test_rejectedExecution_splittable(" + computeDescr(parallelizer) + ")");
        }
        
        final boolean spmElseSp = false;
        this.test_rejectedExecution_xxx(spmElseSp, parallelizer, exceptionHandler);
    }
    
    public void test_rejectedExecution_splitmergable() {
        final double rejectionProba = 0.5;
        final MyExceptionHandler exceptionHandler = new MyExceptionHandler();
        for (InterfaceParallelizerForTests parallelizer : newParallelizerList(
            exceptionHandler,
            rejectionProba)) {
            test_rejectedExecution_splitmergable(parallelizer, exceptionHandler);
            parallelizer.shutdownAndWait();
        }
    }
    
    public void test_rejectedExecution_splitmergable(
        InterfaceParallelizerForTests parallelizer,
        MyExceptionHandler exceptionHandler) {
        if (DEBUG) {
            Dbg.log("test_rejectedExecution_splitmergable(" + computeDescr(parallelizer) + ")");
        }
        
        final boolean spmElseSp = true;
        this.test_rejectedExecution_xxx(spmElseSp, parallelizer, exceptionHandler);
    }
    
    public void test_rejectedExecution_xxx(
        boolean spmElseSp,
        InterfaceParallelizerForTests parallelizer,
        MyExceptionHandler exceptionHandler) {
        
        final boolean expectedGracefulRejection =
            parallelizer.executorRejectsWithOnCancelIfCancellable();
        if (DEBUG) {
            Dbg.log();
            Dbg.log("expectedGracefulRejection = " + expectedGracefulRejection);
        }
        
        boolean didDetectAtLeastOnce = false;
        boolean didHandleAtLeastOnce = false;
        
        int shotId = 0;
        
        for (int k = 0; k < NBR_OF_RUNS_PER_CASE_WHEN_SPLITS_PLUS_EXCEPTIONS; k++) {
            shotId++;
            if (DEBUG) {
                Dbg.log();
                Dbg.log("shotId = " + shotId);
            }
            
            final int n = FIBO_MAX_N;
            
            final InterfaceSplittable spx;
            if (spmElseSp) {
                spx = new MyFiboSpm(
                    shotId,
                    0,
                    FIBO_MIN_SEQ_N,
                    n);
            } else {
                spx = new MyFiboSp(
                    shotId,
                    0,
                    FIBO_MIN_SEQ_N,
                    n,
                    new AtomicInteger());
            }
            
            exceptionHandler.firstHandledRef.set(null);
            
            Throwable firstDetected = null;
            try {
                parallelizer.execute(spx);
            } catch (Throwable e) {
                firstDetected = e;
                didDetectAtLeastOnce = true;
            }
            
            if (firstDetected != null) {
                if (expectedGracefulRejection) {
                    Dbg.log("expectedGracefulRejection = " + expectedGracefulRejection);
                    Dbg.log("firstDetected", firstDetected);
                }
                assertEquals(RethrowException.class, firstDetected.getClass());
                assertEquals(RejectedExecutionException.class, firstDetected.getCause().getClass());
            }
            
            final Throwable firstHandled = exceptionHandler.firstHandledRef.get();
            if (firstHandled != null) {
                if (expectedGracefulRejection) {
                    Dbg.log("expectedGracefulRejection = " + expectedGracefulRejection);
                    Dbg.log("firstHandled", firstHandled);
                }
                didHandleAtLeastOnce = true;
                assertEquals(RejectedExecutionException.class, firstHandled.getClass());
            }
            
            /*
             * Must compute correct result even in case of rejections,
             * thanks to best effort work policy.
             */
            {
                final int expected = fiboSeq(n);
                final int actual;
                if (spmElseSp) {
                    actual = ((MyFiboSpm) spx).result;
                } else {
                    actual = ((MyFiboSp) spx).result.get();
                }
                assertEquals(expected, actual);
            }
        }
        
        final boolean expectedRee = !expectedGracefulRejection;
        assertEquals(expectedRee, didDetectAtLeastOnce);
        assertEquals(expectedRee, didHandleAtLeastOnce);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void logThrowable(String name, Throwable throwable) {
        Dbg.log(name + ":");
        if (throwable != null) {
            Dbg.log(throwable);
        } else {
            Dbg.log("null");
        }
    }
    
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
     * Checks that the specified throwable is or contains
     * an exception throw by tests.
     */
    private static void checkIsOrContainsThrownByTests(Throwable e) {
        if (!isOrContainsThrownByTests(e)) {
            Dbg.log("not throw by test:");
            Dbg.log(e);
            fail();
        }
    }
    
    /**
     * @return True is the specified throwable is or contains
     *         an exception throw by tests.
     */
    private static boolean isOrContainsThrownByTests(Throwable e) {
        return (getRejectedExecutionException(e) != null)
            || (getMyThrowable(e) != null);
    }
    
    /**
     * @return The RejectedExecutionException, or null if none.
     */
    private static RejectedExecutionException getRejectedExecutionException(Throwable e) {
        RejectedExecutionException ret = null;
        while (e != null) {
            if (e instanceof RejectedExecutionException) {
                ret = (RejectedExecutionException) e;
                break;
            } else {
                e = e.getCause();
            }
        }
        return ret;
    }
    
    /**
     * @return The MyThrowable, or null if none.
     */
    private static MyThrowable getMyThrowable(Throwable e) {
        MyThrowable ret = null;
        while (e != null) {
            if (e instanceof MyThrowable) {
                ret = (MyThrowable) e;
                break;
            } else {
                e = e.getCause();
            }
        }
        return ret;
    }
}
