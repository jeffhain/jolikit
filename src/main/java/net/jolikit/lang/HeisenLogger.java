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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Logger designed to debug concurrent treatments, in particular to investigate
 * heisenbugs that direct calls to System.out or other logging facilities would
 * prevent from happening.
 * 
 * Can also be used as general debug logger since it only depends on core JDK,
 * and happens to also be suited for when things are or get concurrent
 * (low overhead, and thread-local id as prefix by default).
 * 
 * Log methods entail a very small overhead and minimal memory synchronization
 * (at most a volatile lazy set and possibly a volatile read), unless in case of
 * flush, which causes locking.
 * 
 * Configuration is non-final, for setting before (or while, if you know what
 * you're doing...) debugging, static, for simplicity, and non-volatile, for
 * performances, so its modifications are typically not immediately (if ever...)
 * visible across threads.
 * 
 * Logs are added to a (thread-)local logger instance, and flushed periodically
 * by a single daemon thread, or after a number of logs done by a same logger
 * instance, or on user command.
 * 
 * Flushing thread refers to whatever thread calls flushPendingLogs() method
 * (the daemon thread is by nature a flushing thread).
 * 
 * Instance log methods are not thread-safe, but allow for very efficient
 * logging. For a logger instance, the (current) thread that makes use of them
 * is referred to as the logging thread.
 * 
 * Static log method is thread-safe and uses a default thread-local logger
 * instance.
 * 
 * Object's toString is computed on flush into print stream, not at log time;
 * but it's of course possible to log toString's result as a regular log
 * message.
 * 
 * If the last object to log is a Throwable, not logging its toString(), but
 * the same content as obtained in System.err by Throwable.printStackTrace().
 * This makes log(...) method behave somewhat like usual log treatments
 * (log(Object,Throwable)), except that if only a Throwable is logged it still
 * prints its stack trace.
 * 
 * Logs are preceded by logger (instance) id, which is a unique long, but time
 * is logged before logger id, to allow for easy temporal sorting.
 */
public class HeisenLogger {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static PrintStream PRINT_STREAM = System.out;
    public static PrintStream getPrintStream() {
        return PRINT_STREAM;
    }
    /**
     * @param printStream Must not be null.
     */
    public static void setPrintStream(PrintStream printStream) {
        if (printStream == null) {
            throw new NullPointerException();
        }
        PRINT_STREAM = printStream;
    }

    /**
     * If false, loggers are never removed from shared set,
     * which can cause a memory leak and longer flushes.
     * If true, causes a volatile read (and add into set
     * if it is surely not in it) in logLocalLogs() method.
     */
    private static boolean MUST_REMOVE_FLUSHED_LOGGERS = true;
    public static boolean getMustRemoveFlushedLoggers() {
        return MUST_REMOVE_FLUSHED_LOGGERS;
    }
    public static void setMustRemoveFlushedLoggers(boolean mustRemoveFlushedLoggers) {
        MUST_REMOVE_FLUSHED_LOGGERS = mustRemoveFlushedLoggers;
    }

    /**
     * False by default because System.nanoTime() can be quite slow.
     */
    private static boolean MUST_LOG_TIME = false;
    public static boolean getMustLogTime() {
        return MUST_LOG_TIME;
    }
    public static void setMustLogTime(boolean mustLogTime) {
        MUST_LOG_TIME = mustLogTime;
    }

    /**
     * If true, printing an underscore just after seconds digit if any,
     * in time logs.
     */
    private static boolean MUST_USE_PRETTY_TIME_FORMAT = false;
    public static boolean getMustUsePrettyTimeFormat() {
        return MUST_USE_PRETTY_TIME_FORMAT;
    }
    public static void setMustUsePrettyTimeFormat(boolean mustUsePrettyTimeFormat) {
        MUST_USE_PRETTY_TIME_FORMAT = mustUsePrettyTimeFormat;
    }

    /**
     * False by default because threads toString() is relatively costly,
     * and logger id is logged anyway, which already allows for some kind
     * of log source identification.
     */
    private static boolean MUST_LOG_THREAD = false;
    public static boolean getMustLogThread() {
        return MUST_LOG_THREAD;
    }
    public static void setMustLogThread(boolean mustLogThread) {
        MUST_LOG_THREAD = mustLogThread;
    }

    /**
     * Logs are sorted by time only for a same flush to print stream.
     */
    private static boolean MUST_SORT_IF_TIMED = true;
    public static boolean getMustSortIfTimed() {
        return MUST_SORT_IF_TIMED;
    }
    public static void setMustSortIfTimed(boolean mustSortIfTimed) {
        MUST_SORT_IF_TIMED = mustSortIfTimed;
    }

    /**
     * For daemon thread.
     */
    private static long FLUSH_SLEEP_MS = 10L;
    public static long getFlushSleepMs() {
        return FLUSH_SLEEP_MS;
    }
    public static void setFlushSleepMs(long flushSleepMs) {
        FLUSH_SLEEP_MS = flushSleepMs;
    }

    /**
     * To avoid too many logs piling-up, especially if daemon thread
     * can't keep up.
     * This value is used per-logger, to avoid usage of a concurrent counter.
     * Max number of logs stored is approximately this value times the
     * number of loggers.
     */
    private static long MAX_NBR_OF_LOGS_WITHOUT_FLUSH = 1000L;
    public static long getMaxNbrOfLogsWithoutFlush() {
        return MAX_NBR_OF_LOGS_WITHOUT_FLUSH;
    }
    public static void setMaxNbrOfLogsWithoutFlush(long maxNbrOfLogsWithoutFlush) {
        MAX_NBR_OF_LOGS_WITHOUT_FLUSH = maxNbrOfLogsWithoutFlush;
    }

    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------

    /**
     * Holds a non-volatile reference to a logger instance.
     * 
     * Making sure this name doesn't start with "l",
     * else it can come up first in auto-completion
     * while looking for log method.
     */
    public static class RefLocal {
        private HeisenLogger logger;
    }

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyLogData {
        private final String loggerId;
        private final long timeNs;
        private final Thread thread;
        private final boolean mustSuffixWithNewLine;
        private final Object[] messages;
        /**
         * Next log data, if any, for same batch.
         * Written by logging thread, before lazy set
         * that makes it visible to flush treatments.
         */
        private MyLogData next;
        public MyLogData(
                String loggerId,
                long timeNs,
                Thread thread,
                boolean mustSuffixWithNewLine,
                Object[] messages) {
            this.loggerId = loggerId;
            this.timeNs = timeNs;
            this.thread = thread;
            this.mustSuffixWithNewLine = mustSuffixWithNewLine;
            this.messages = messages;
        }
        public void appendString(StringBuilder sb) {
            // Time logged first, to allow for easy sorting by time.
            if (MUST_LOG_TIME) {
                appendTimeNs(sb, this.timeNs);
                sb.append(" ");
            }
            sb.append(this.loggerId);
            if (this.thread != null) {
                sb.append(" ");
                /*
                 * We use LangUtils.toString(Thread) for thread id,
                 * and we also indicate hexadecimal identity hash code,
                 * to help make sure which object it is.
                 */
                sb.append(NbrsUtils.toString(System.identityHashCode(this.thread), 16));
                sb.append("@");
                sb.append(LangUtils.toString(this.thread));
            }
            final Object[] messages = this.messages;
            final int nMinusOne = messages.length-1;
            for (int i = 0; i < nMinusOne; i++) {
                sb.append(" ; ");
                sb.append(messages[i]);
            }
            if (nMinusOne >= 0) {
                sb.append(" ; ");
                final Object lastMessage = messages[nMinusOne];
                if (lastMessage instanceof Throwable) {
                    sb.append(LINE_SEPARATOR);
                    LangUtils.appendStackTrace(sb, (Throwable)lastMessage);
                } else {
                    sb.append(lastMessage);
                }
            }
            if (this.mustSuffixWithNewLine) {
                sb.append(LINE_SEPARATOR);
            }
        }
        private static void appendTimeNs(StringBuilder sb, long ns) {
            if (MUST_USE_PRETTY_TIME_FORMAT) {
                final long ONE_BILLION = 1000L * 1000L * 1000L;
                final long anteSepS = ns / ONE_BILLION;
                if (anteSepS == 0) {
                    sb.append(ns);
                } else {
                    // Separator just after seconds, much more readable.
                    final long postSepS = ns - anteSepS * ONE_BILLION;
                    sb.append(anteSepS);
                    final int sepIndex = sb.length();
                    /*
                     * Adding one billion, which will put its '1'
                     * at separator index, to avoid heading zeroes vanishing.
                     */
                    sb.append(ONE_BILLION + postSepS);
                    sb.setCharAt(sepIndex, '_');
                }
            } else {
                sb.append(ns);
            }
            sb.append(" ");
        }
    }
    
    /**
     * To sort logs by time.
     */
    private static class MyLogDataComparator implements Comparator<MyLogData> {
        @Override
        public int compare(MyLogData a, MyLogData b) {
            if (a.timeNs < b.timeNs) {
                return -1;
            } else if (a.timeNs > b.timeNs) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Extended AtomicReference holds reference to eventual next
     * batch of log data for same logger.
     */
    private static class MyLogBatch extends AtomicReference<MyLogBatch> {
        private static final long serialVersionUID = 1L;
        /**
         * Before first log data for this batch.
         * (Using such a header to avoid an if statement
         * in logLocal(Object...) method. Also implies
         * to always have a log batch instance in pending
         * batch reference.)
         */
        private final MyLogData beforeFirstLogData = new MyLogData(null, 0L, null, false, null);
        /**
         * Only used by logging thread.
         */
        private MyLogData lastLogData = this.beforeFirstLogData;
        /**
         * Only usable by logging thread.
         */
        public boolean isEmpty() {
            // Empty if only the header in the list.
            return this.lastLogData == this.beforeFirstLogData;
        }
        /**
         * Only usable by logging thread.
         */
        public void addLog(MyLogData logData) {
            this.lastLogData.next = logData;
            this.lastLogData = logData;
        }
    }
    
    private static class MyLazyDaemonStart {
        static {
            addShutdownHook();
            DAEMON.start();
        }
        static void loadClass() {
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /*
     * static members
     */

    private static final String LINE_SEPARATOR = LangUtils.LINE_SEPARATOR;
    
    private static final Lock MAIN_LOCK = new ReentrantLock();

    private static final MyLogDataComparator LOG_DATA_COMPARATOR = new MyLogDataComparator();

    /**
     * Guarded by main lock.
     */
    private static final StringBuilder FLUSH_BUFFER = new StringBuilder();

    private static final Thread DAEMON;
    static {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    flushPendingLogsAndStream();
                    try {
                        Thread.sleep(FLUSH_SLEEP_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace(PRINT_STREAM);
                        // ok stopping
                        break;
                    }
                }
            }
        };
        final Thread thread = new Thread(
                runnable,
                HeisenLogger.class.getSimpleName());
        thread.setDaemon(true);
        DAEMON = thread;
    }

    private static final ThreadLocal<HeisenLogger> TL_LOGGER = new ThreadLocal<HeisenLogger>() {
        @Override
        public HeisenLogger initialValue() {
            return newLogger();
        }
    };

    /**
     * Set of local loggers that might contain pending logs.
     */
    private static final ConcurrentHashMap<HeisenLogger, Boolean> PENDING_LOGGER_SET = new ConcurrentHashMap<HeisenLogger, Boolean>();

    private static final AtomicLong LOGGER_ID_PROVIDER = new AtomicLong();

    /*
     * instance fields
     */

    /**
     * For logger (and thread, if thread-specific) identification in logs.
     */
    private final String loggerId = Long.toString(LOGGER_ID_PROVIDER.incrementAndGet());

    /**
     * Used to remove this logger from the set once it has been flushed
     * (without using weak references).
     * Note that the default value (0) is the right one in case we don't
     * remove flushed loggers: in that case, we don't have to do a volatile
     * (non-lazy) set have this value properly initialized.
     */
    private final PostPaddedAtomicInteger inPendingSet = new PostPaddedAtomicInteger();

    /**
     * Can't use first pending batch instead, because it could be null,
     * and logging thread would need to initialize it, i.e. read it
     * and then eventually set it, which would require additional
     * memory synchronizations with flushing thread.
     * 
     * Only used in main lock (after construction).
     */
    private MyLogBatch beforeNextPendingBatch = new MyLogBatch();

    /**
     * Last batch from this logger to be pending
     * for being logged by flushing thread.
     * 
     * Only used by logging thread.
     */
    private MyLogBatch lastPendingBatch = this.beforeNextPendingBatch;

    /**
     * Batch of local logs, that only become pending, and visible
     * to flushing thread, after call to logLocalLogs().
     * 
     * Only used by logging thread.
     */
    private MyLogBatch localBatch = new MyLogBatch();

    /**
     * Only used by logging thread.
     */
    private long nbrOfLogsWithoutFlush = 0;

    /*
     * temps
     */

    /**
     * For sorting.
     * Guarded by main lock.
     */
    private static MyLogData[] TMP_LOG_DATA_ARRAY = new MyLogData[4];

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /*
     * static methods
     */

    /**
     * A possible use-case is to create a logger for a few local logs,
     * and actually make them visible to flush treatments (using logLocalLogs()
     * method) only if desired.
     * 
     * @return A new (and non thread-safe) logger.
     */
    public static HeisenLogger newLogger() {
        final HeisenLogger logger = new HeisenLogger();
        if (!MUST_REMOVE_FLUSHED_LOGGERS) {
            // Won't be removed from set after flush, in which case we allow
            // logLocalLogs() method not to bother checking if it must be added
            // or not in the set, so we must do it here.
            putInPendingLoggerSet(logger);
        }
        return logger;
    }

    /**
     * @return Default thread-local logger.
     */
    public static HeisenLogger getDefaultThreadLocalLogger() {
        return TL_LOGGER.get();
    }

    /**
     * This treatment makes it easy to quickly retrieve default thread-local
     * logger across treatments known to always be used by a same thread,
     * by reducing thread-local usage.
     * 
     * @param ref A RefLocal, updated with default thread-local
     *        logger if empty.
     * @return Logger, if any, referenced by the specified RefLocal,
     *         else default thread-local logger.
     */
    public static HeisenLogger getDefaultThreadLocalLogger(RefLocal ref) {
        HeisenLogger logger = ref.logger;
        if (logger == null) {
            logger = TL_LOGGER.get();
            ref.logger = logger;
        }
        return logger;
    }

    /**
     * Together "main" (handy) and "convenience" (just calls other methods) log method.
     * 
     * Equivalent to successive calls to getDefaultThreadLocalLogger(),
     * logLocal(Object...) and then logLocalLogs().
     * 
     * Does suffix with a new line.
     */
    public static void log(Object... messages) {
        final HeisenLogger logger = getDefaultThreadLocalLogger();
        logger.logLocal(messages);
        logger.logLocalLogs();
    }

    /**
     * Together "main" (handy) and "convenience" (just calls other methods) log method.
     * 
     * Equivalent to successive calls to getDefaultThreadLocalLogger(),
     * logLocal(Object...) and then logLocalLogs().
     * 
     * Does not suffix with a new line.
     */
    public static void logNoNl(Object... messages) {
        final HeisenLogger logger = getDefaultThreadLocalLogger();
        logger.logLocalNoNl(messages);
        logger.logLocalLogs();
    }
    
    /**
     * Flush pending logs into print stream.
     * 
     * Will miss local logs, and last pending logs done by other threads that
     * did not synchronize main memory with their memory yet.
     */
    public static void flushPendingLogs() {
        MAIN_LOCK.lock();
        try {
            final boolean mustSort = (MUST_LOG_TIME && MUST_SORT_IF_TIMED);
            MyLogData[] logDataToSortArr = (mustSort ? TMP_LOG_DATA_ARRAY : null);
            int logDataToSortCount = 0;

            FLUSH_BUFFER.setLength(0);

            final Iterator<Map.Entry<HeisenLogger, Boolean>> it = PENDING_LOGGER_SET.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<HeisenLogger, Boolean> entry;
                try {
                    entry = it.next();
                } catch (NoSuchElementException e) {
                    // quiet
                    break;
                }
                final HeisenLogger logger = entry.getKey();

                // Need to remove thread data before iterating on its logs,
                // else logs added after iteration and before removal might be lost.
                if (MUST_REMOVE_FLUSHED_LOGGERS) {
                    it.remove();
                    // Need to set it after remove, else logging thread might
                    // add it to the set between calls to setNotInPendingSet()
                    // and remove(), and the removal will go unnoticed.
                    logger.setNotInPendingSet();
                }

                MyLogBatch anteNextPending = logger.beforeNextPendingBatch;
                MyLogBatch logBatch;
                // Getting log data lazily set by user thread.
                while ((logBatch = anteNextPending.get()) != null) {
                    // Batch never empty here, for only added if non-empty.
                    MyLogData logData = logBatch.beforeFirstLogData.next;
                    while (true) {
                        if (mustSort) {
                            final boolean isFull = (logDataToSortCount == logDataToSortArr.length);
                            if (isFull) {
                                final int newCapacity =
                                        LangUtils.increasedArrayLength(
                                                logDataToSortArr.length,
                                                logDataToSortCount + 1);
                                final MyLogData[] newArr = new MyLogData[newCapacity];
                                System.arraycopy(logDataToSortArr, 0, newArr, 0, logDataToSortCount);
                                logDataToSortArr = newArr;
                                TMP_LOG_DATA_ARRAY = newArr;
                            }
                            logDataToSortArr[logDataToSortCount++] = logData;
                        } else {
                            logData.appendString(FLUSH_BUFFER);
                        }
                        final MyLogData nextLogData = logData.next;
                        if (nextLogData == null) {
                            break;
                        }
                        logData.next = null; // helping GC
                        logData = nextLogData;
                    }

                    // Lazy set OK, since done AFTER first lazy set's result
                    // became visible (if current thread is not log's user thread),
                    // and BEFORE another thread would eventually read this value,
                    // due to main lock which will synchronize main memory with
                    // current thread memory when we will release it.
                    anteNextPending.lazySet(null); // helping GC

                    anteNextPending = logBatch;
                }

                logger.beforeNextPendingBatch = anteNextPending;
            }

            /*
             * 
             */

            if (logDataToSortCount != 0) {
                Arrays.sort(
                        logDataToSortArr,
                        0,
                        logDataToSortCount,
                        LOG_DATA_COMPARATOR);

                for (int i = 0; i < logDataToSortCount; i++) {
                    final MyLogData logData = logDataToSortArr[i];
                    logData.appendString(FLUSH_BUFFER);
                }
            }

            /*
             * 
             */

            // Print in main lock,
            // to make sure logs are done in proper sequence.
            PRINT_STREAM.print(FLUSH_BUFFER.toString());
        } finally {
            MAIN_LOCK.unlock();
        }
    }

    /**
     * Flush pending logs into print stream,
     * and flushes print stream.
     * 
     * Will miss local logs, and last pending logs done by other threads that
     * did not flush their memory yet.
     */
    public static void flushPendingLogsAndStream() {
        flushPendingLogs();
        PRINT_STREAM.flush();
    }
    
    /**
     * Useful if wanting to create timestamps
     * homogeneous with this class ones.
     * 
     * @return Value subtracted from System.nanoTime()
     *         to obtain logs timestamps.
     */
    public static long getInitialTimeNs() {
        return LangUtils.getClassLoadNanoTimeNs();
    }

    /*
     * instance methods
     */

    @Override
    public String toString() {
        // Simple name should be enough, and not too annoying.
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(this.hashCode())
        + "[notInPendingSet=" + this.isNotInPendingSet()
        + "]";
    }
    
    /**
     * Not thread-safe.
     * 
     * Lowest-overhead log method.
     * Does not use any lock or volatile variable.
     * 
     * Adds the specified log to local logs of this logger
     * (does not make it visible to flush treatments).
     * 
     * For performances purpose, the specified array is not cloned
     * (which would be useless in the common case of multiple args),
     * so if you only specify a single array it should not be mutated.
     * 
     * Does suffix with a new line.
     */
    public void logLocal(Object... messages) {
        final boolean mustSuffixWithNewLine = true;
        logLocal_internal(mustSuffixWithNewLine, messages);
    }
    
    /**
     * Not thread-safe.
     * 
     * Lowest-overhead log method.
     * Does not use any lock or volatile variable.
     * 
     * Adds the specified log to local logs of this logger
     * (does not make it visible to flush treatments).
     * 
     * For performances purpose, the specified array is not cloned
     * (which would be useless in the common case of multiple args),
     * so if you only specify a single array it should not be mutated.
     * 
     * Does not suffix with a new line.
     */
    public void logLocalNoNl(Object... messages) {
        final boolean mustSuffixWithNewLine = false;
        logLocal_internal(mustSuffixWithNewLine, messages);
    }

    /**
     * Not thread-safe.
     * 
     * Makes local logs pending, i.e. makes them visible
     * to flush treatments, possibly after some time,
     * since it is done using a lazy set.
     * 
     * First ensures daemon thread start, which might cause some memory barriers
     * (just once, or a few times, depending on how it's implemented),
     * and some calls also entail both volatile write and volatile read
     * semantics, for bookkeeping of active loggers and eventual
     * periodic flushing.
     */
    public void logLocalLogs() {

        /*
         * Moving batch from being local batch
         * to being last pending batch.
         */

        final MyLogBatch logBatch = this.localBatch;
        if (logBatch.isEmpty()) {
            // No local log.
            return;
        }
        this.localBatch = new MyLogBatch();

        final MyLogBatch lastPendingBatch = this.lastPendingBatch;
        if (lastPendingBatch == null) {
            /*
             * TODO Had it null once I think.
             * Due to wrong concurrent usage of this non-thread-safe method?
             */
            System.out.println("this.inPendingSet = " + this.inPendingSet);
            System.out.println("this.beforeNextPendingBatch = " + this.beforeNextPendingBatch);
            System.out.println("this.lastPendingBatch = " + this.lastPendingBatch);
            System.out.println("this.localBatch = " + this.localBatch);
            System.out.println("this.nbrOfLogsWithoutFlush = " + this.nbrOfLogsWithoutFlush);
            throw new AssertionError();
        }
        lastPendingBatch.lazySet(logBatch);
        this.lastPendingBatch = logBatch;

        /*
         * 
         */

        MyLazyDaemonStart.loadClass();

        /*
         * 
         */

        // Non-volatile test first.
        if (MUST_REMOVE_FLUSHED_LOGGERS && this.isNotInPendingSet()) {
            // Not put just after construction, and is not
            // in pending set: adding this to pending set.
            putInPendingLoggerSet(this);
        }

        if (this.nbrOfLogsWithoutFlush >= MAX_NBR_OF_LOGS_WITHOUT_FLUSH) {
            // Flushing everyone's pending logs.
            flushPendingLogsAndStream();
            this.nbrOfLogsWithoutFlush = 0;
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /*
     * static methods
     */

    /**
     * Not to lose last logs on JVM normal or abnormal exit.
     */
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(HeisenLogger.class.getSimpleName() + "-flush") {
            @Override
            public void run() {
                HeisenLogger.flushPendingLogsAndStream();
            }
        });
    }

    private static void putInPendingLoggerSet(HeisenLogger logger) {
        // Need to set it before put, else flushing thread might
        // remove it from the set between calls to put(...)
        // and lazySetInPendingSet(), and the removal will go
        // unnoticed.
        // Lazy set OK because subsequent put ensures required
        // memory synchronization (before the actual put).
        logger.lazySetInPendingSet();
        PENDING_LOGGER_SET.put(logger, Boolean.TRUE);
    }

    /**
     * Using a common initial time for all loggers, to allow for easy time
     * comparisons for logs from different loggers.
     */
    private static long getTimeNs() {
        return LangUtils.getNanoTimeFromClassLoadNs();
    }

    /*
     * instance methods
     */

    private HeisenLogger() {
    }

    private void logLocal_internal(boolean mustSuffixWithNewLine, Object... messages) {
        final MyLogData logData = new MyLogData(
                this.loggerId,
                (MUST_LOG_TIME ? getTimeNs() : 0L),
                (MUST_LOG_THREAD ? Thread.currentThread() : null),
                mustSuffixWithNewLine,
                messages);
        this.localBatch.addLog(logData);
        ++this.nbrOfLogsWithoutFlush;
    }
    
    /**
     * @return True if is surely not in pending set,
     *         false if might or might not be in pending set.
     */
    private boolean isNotInPendingSet() {
        return this.inPendingSet.get() == 0;
    }

    private void setNotInPendingSet() {
        this.inPendingSet.set(0);
    }

    private void lazySetInPendingSet() {
        this.inPendingSet.lazySet(1);
    }
}
