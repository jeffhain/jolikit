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
package net.jolikit.lang;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.jolikit.lang.HeisenLogger;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.lang.Unchecked;
import net.jolikit.test.utils.TestUtils;

public class HeisenLoggerPerf {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int NBR_OF_PROC = Runtime.getRuntime().availableProcessors();
    private static final int CEILED_NBR_OF_PROC = NumbersUtils.ceilingPowerOfTwo(NBR_OF_PROC);
    
    private static final int MIN_PARALLELISM = 1;
    private static final int MAX_PARALLELISM = 2 * CEILED_NBR_OF_PROC;
    
    private static final boolean USE_SYSTEM_OUT = false;

    private static final long NBR_OF_LOGS = USE_SYSTEM_OUT ? 10L * 1000L : 1000L * 1000L;
    
    private static final int MAX_NBR_OF_LOGS_PER_BATCH = 10;
    private static final int NBR_OF_RUNS = 4;

    private static final boolean DO_LOG_TIME = false;
    private static final boolean DO_LOG_THREAD = false;

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MySynchronizedPrintRunnable implements Runnable {
        private static final long INITIAL_TIME_NS = System.nanoTime();
        private final PrintStream printStream;
        private final long nbrOfLogs;
        private static final StringBuilder tmpBuff = new StringBuilder();
        public MySynchronizedPrintRunnable(
                final PrintStream printStream,
                long nbrOfLogs) {
            this.printStream = printStream;
            this.nbrOfLogs = nbrOfLogs;
        }
        @Override
        public String toString() {
            return this.getClass().getSimpleName();
        }
        @Override
        public void run() {
            final long loggerId = this.hashCode();
            final Thread currentThread = Thread.currentThread();
            final PrintStream printStream = this.printStream;
            final StringBuilder sb = tmpBuff;
            for (int i = 0; i < nbrOfLogs; i++) {
                synchronized (sb) {
                    sb.setLength(0);
                    sb.append(loggerId);
                    if (DO_LOG_TIME) {
                        sb.append(" ");
                        sb.append(System.nanoTime() - INITIAL_TIME_NS);
                    }
                    if (DO_LOG_THREAD) {
                        sb.append(" ");
                        sb.append(currentThread);
                    }
                    sb.append(" - ");
                    sb.append(BENCH_LOG);
                    printStream.println(sb.toString());
                }
            }
        }
    }

    private static class MyHeisenLoggerRunnable implements Runnable {
        private final long nbrOfLogs;
        public MyHeisenLoggerRunnable(long nbrOfLogs) {
            this.nbrOfLogs = nbrOfLogs;
        }
        @Override
        public String toString() {
            return this.getClass().getSimpleName();
        }
        @Override
        public void run() {
            for (int i = 0; i < this.nbrOfLogs; i++) {
                HeisenLogger.log(BENCH_LOG);
            }
            HeisenLogger.flushPendingLogsAndStream();
        }
    }

    private static class MyHeisenLoggerBatchRunnable implements Runnable {
        private final long nbrOfLogs;
        private final int maxNbrOfLogsPerBatch;
        public MyHeisenLoggerBatchRunnable(
                long nbrOfLogs,
                int maxNbrOfLogsPerBatch) {
            this.nbrOfLogs = nbrOfLogs;
            this.maxNbrOfLogsPerBatch = maxNbrOfLogsPerBatch;
        }
        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "(" + this.maxNbrOfLogsPerBatch + ")";
        }
        @Override
        public void run() {
            final HeisenLogger logger = HeisenLogger.newLogger();
            long nbrOfLogsRemaining = this.nbrOfLogs;
            while (nbrOfLogsRemaining > 0) {
                final long nbrOfLogsForBatch = Math.min(nbrOfLogsRemaining, this.maxNbrOfLogsPerBatch);
                nbrOfLogsRemaining -= nbrOfLogsForBatch;
                for (long i = 0; i < nbrOfLogsForBatch; i++) {
                    logger.logLocal(BENCH_LOG);
                }
                logger.logLocalLogs();
            }
            HeisenLogger.flushPendingLogsAndStream();
        }
    }

    private static class MyNoOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
    }

    private static class MyNoPrintStream extends PrintStream {
        public MyNoPrintStream() {
            super(new MyNoOutputStream());
        }
        @Override
        public void print(String s) {
        }
        @Override
        public void flush() {
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final String BENCH_LOG = "dummy log";

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        System.out.println(TestUtils.getJVMInfo());
        newRun(args);
    }

    public static void newRun(String[] args) {
        new HeisenLoggerPerf().run(args);
    }
    
    public HeisenLoggerPerf() {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void run(String[] args) {
        // XXX
        System.out.println("--- " + HeisenLoggerPerf.class.getSimpleName() + "... ---");
        System.out.println("number of logs = " + NBR_OF_LOGS);

        HeisenLogger.setMustLogTime(DO_LOG_TIME);
        HeisenLogger.setMustLogThread(DO_LOG_THREAD);

        // If using System.out, printing results last,
        // else they are lost in spam.
        final boolean resultsLast = USE_SYSTEM_OUT;

        final PrintStream printStream = USE_SYSTEM_OUT ? System.out : new MyNoPrintStream();
        HeisenLogger.setPrintStream(printStream);

        final long nbrOfLogsTotal = NBR_OF_LOGS;
        final int maxNbrOfLogsPerBatch = MAX_NBR_OF_LOGS_PER_BATCH;
        final int nbrOfRuns = NBR_OF_RUNS;

        long a;
        long b;

        final ArrayList<String> results = new ArrayList<String>();

        for (int nbrOfThreads=MIN_PARALLELISM;nbrOfThreads<=MAX_PARALLELISM;nbrOfThreads*=2) {
            final long nbrOfLogsPerThread = nbrOfLogsTotal/nbrOfThreads;

            final String header = nbrOfThreads + " thread(s) : ";
            
            if (resultsLast) {
                results.add("");
            } else {
                System.out.println();
                System.out.flush();
            }

            for (int i = 0; i < 3; i++) {
                for (int k = 0; k < nbrOfRuns; k++) {
                    final ExecutorService executor = Executors.newCachedThreadPool();

                    a = System.nanoTime();
                    String runnableName = null;
                    for (int n=0;n<nbrOfThreads;n++) {
                        final Runnable runnable;
                        if (i == 0) {
                            runnable = new MySynchronizedPrintRunnable(
                                    printStream,
                                    nbrOfLogsPerThread);
                        } else if (i == 1) {
                            runnable = new MyHeisenLoggerRunnable(nbrOfLogsPerThread);
                        } else {
                            runnable = new MyHeisenLoggerBatchRunnable(
                                    nbrOfLogsPerThread,
                                    maxNbrOfLogsPerBatch);
                        }
                        runnableName = runnable.toString();
                        executor.execute(runnable);
                    }

                    Unchecked.shutdownAndAwaitTermination(executor);

                    printStream.flush();

                    b = System.nanoTime();
                    final String result = header + runnableName + " took " + TestUtils.nsToSRounded(b-a) + " s";
                    if (resultsLast) {
                        results.add(result);
                    } else {
                        System.out.println(result);
                        System.out.flush();
                    }
                }
            }
        }
        
        if (resultsLast) {
            for (String result : results) {
                System.out.println(result);
            }
        }

        System.out.println("--- ..." + HeisenLoggerPerf.class.getSimpleName() + " ---");
    }
}
