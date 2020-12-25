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
package net.jolikit.lang;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.jolikit.test.utils.ProcessorsUser;
import junit.framework.TestCase;

public class HeisenLoggerTest extends TestCase {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyNoOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
    }

    private static class MyPrintStream extends PrintStream {
        private final ConcurrentHashMap<LongWrapper, Boolean> linesCounters = new ConcurrentHashMap<LongWrapper, Boolean>();
        /**
         * One counter per flushing thread (daemon thread, or logging threads).
         */
        private final ThreadLocal<LongWrapper> tlLong = new ThreadLocal<LongWrapper>() {
            @Override
            public LongWrapper initialValue() {
                final LongWrapper counter = new LongWrapper();
                linesCounters.put(counter, Boolean.TRUE);
                return counter;
            }
        };
        public MyPrintStream() {
            super(new MyNoOutputStream());
        }
        @Override
        public void print(String s) {
            // One separator for each line.
            final int nbrOfLines = computeNbrOfLineSeparator(s);
            (this.tlLong.get().value) += nbrOfLines;
        }
        @Override
        public void flush() {
            // nothing to flush
        }
        public long computeNbrOfChars() {
            long sum = 0;
            for (Map.Entry<LongWrapper,Boolean> entry : this.linesCounters.entrySet()) {
                final LongWrapper counter = entry.getKey();
                sum += counter.value;
            }
            return sum;
        }
        private static int computeNbrOfLineSeparator(String s) {
            int result = 0;
            int fromIndex = 0;
            while ((fromIndex = 1 + s.indexOf(LINE_SEPARATOR, fromIndex)) != 0) {
                ++result;
            }
            return result;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    
    /*
     * Supposing noone did change and not restore defaults.
     */
    
    private static final PrintStream DEFAULT_PRINT_STREAM = HeisenLogger.getPrintStream();
    private static final boolean DEFAULT_REMOVE_FLUSHED_LOGGERS = HeisenLogger.getMustRemoveFlushedLoggers();
    private static final boolean DEFAULT_DO_LOG_TIME = HeisenLogger.getMustLogTime();
    private static final boolean DEFAULT_DO_LOG_THREAD = HeisenLogger.getMustLogThread();
    private static final boolean DEFAULT_DO_SORT_IF_TIMED = HeisenLogger.getMustSortIfTimed();
    private static final long DEFAULT_FLUSH_SLEEP_MS = HeisenLogger.getFlushSleepMs();
    private static final long DEFAULT_FLUSH_EVERY = HeisenLogger.getMaxNbrOfLogsWithoutFlush();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    @Override
    public void setUp() throws Exception {
        ProcessorsUser.start();
    }

    @Override
    public void tearDown() throws Exception {
        ProcessorsUser.stop();
        resetDefaults();
    }

    public void test_defaults() {
        assertEquals(System.out,HeisenLogger.getPrintStream());
        assertEquals(true,HeisenLogger.getMustRemoveFlushedLoggers());
        assertEquals(false,HeisenLogger.getMustLogTime());
        assertEquals(false,HeisenLogger.getMustLogThread());
        assertEquals(true,HeisenLogger.getMustSortIfTimed());
        assertEquals(10L,HeisenLogger.getFlushSleepMs());
        assertEquals(1000L,HeisenLogger.getMaxNbrOfLogsWithoutFlush());
    }

    public void test_logsAndFlush_logThread() {
        HeisenLogger.setMustLogThread(true);
        private_test_logsAndFlush_noConfig();
    }

    public void test_logsAndFlush_noRemoveAndNoTime() {
        HeisenLogger.setMustRemoveFlushedLoggers(false);
        HeisenLogger.setMustLogTime(false);
        HeisenLogger.setMustSortIfTimed(false);
        private_test_logsAndFlush_noConfig();
    }

    public void test_logsAndFlush_noRemoveAndTimeAndNoSort() {
        HeisenLogger.setMustRemoveFlushedLoggers(false);
        HeisenLogger.setMustLogTime(true);
        HeisenLogger.setMustSortIfTimed(false);
        private_test_logsAndFlush_noConfig();
    }

    public void test_logsAndFlush_noRemoveAndTimeAndSort() {
        HeisenLogger.setMustRemoveFlushedLoggers(false);
        HeisenLogger.setMustLogTime(true);
        HeisenLogger.setMustSortIfTimed(true);
        private_test_logsAndFlush_noConfig();
    }

    public void test_logsAndFlush_removeAndNoTime() {
        HeisenLogger.setMustRemoveFlushedLoggers(true);
        HeisenLogger.setMustLogTime(false);
        HeisenLogger.setMustSortIfTimed(false);
        private_test_logsAndFlush_noConfig();
    }

    public void test_logsAndFlush_removeAndTimeAndNoSort() {
        HeisenLogger.setMustRemoveFlushedLoggers(true);
        HeisenLogger.setMustLogTime(true);
        HeisenLogger.setMustSortIfTimed(false);
        private_test_logsAndFlush_noConfig();
    }

    public void test_logsAndFlush_removeAndTimeAndSort() {
        HeisenLogger.setMustRemoveFlushedLoggers(true);
        HeisenLogger.setMustLogTime(true);
        HeisenLogger.setMustSortIfTimed(true);
        private_test_logsAndFlush_noConfig();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private void private_test_logsAndFlush_noConfig() {
        final int nbrOfRuns = 100;
        final int nbrOfThreads = 4;
        final long nbrOfLogsPerThread = 10L * 1000L;

        // Else won't ever flush on log.
        assertTrue(nbrOfLogsPerThread > HeisenLogger.getMaxNbrOfLogsWithoutFlush());

        for (int k = 0; k < nbrOfRuns; k++) {
            final MyPrintStream printStream = new MyPrintStream();
            HeisenLogger.setPrintStream(printStream);

            final ExecutorService executor = Executors.newCachedThreadPool();

            for (int n=0;n<nbrOfThreads;n++) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (long i = 0; i < nbrOfLogsPerThread; i++) {
                            HeisenLogger.log(i);
                        }
                    }
                });
            }

            Unchecked.shutdownAndAwaitTermination(executor);

            // Should flush everything here, since publisher threads are now idle.
            HeisenLogger.flushPendingLogsAndStream();

            assertEquals(nbrOfThreads * nbrOfLogsPerThread, printStream.computeNbrOfChars());
        }
    }
    
    private static void resetDefaults() {
        HeisenLogger.setPrintStream(DEFAULT_PRINT_STREAM);
        HeisenLogger.setMustRemoveFlushedLoggers(DEFAULT_REMOVE_FLUSHED_LOGGERS);
        HeisenLogger.setMustLogTime(DEFAULT_DO_LOG_TIME);
        HeisenLogger.setMustLogThread(DEFAULT_DO_LOG_THREAD);
        HeisenLogger.setMustSortIfTimed(DEFAULT_DO_SORT_IF_TIMED);
        HeisenLogger.setFlushSleepMs(DEFAULT_FLUSH_SLEEP_MS);
        HeisenLogger.setMaxNbrOfLogsWithoutFlush(DEFAULT_FLUSH_EVERY);
    }
}
