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
package net.jolikit.test.utils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Random;

/**
 * Utility treatments for tests or benches.
 */
public class TestUtils {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    
    private static final String JVM_INFO = readJVMInfo();
    
    private static final String BLACK_HOLE_SIDE_EFFECT_MESSAGE =
            "black hole spit out something big: not supposed to happen";
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return JVM info to serve as header for tests logs.
     */
    public static String getJVMInfo() {
        return JVM_INFO;
    }
    
    /**
     * Handy not to have to type 123456789L (or whatever other constants)
     * every time we want a (single) deterministic Random instance.
     * 
     * @return A new Random instance seeded with 123456789L.
     */
    public static Random newRandom123456789L() {
        return new Random(123456789L);
    }
    
    /*
     * Black holes: Consume the specified value, without side effect
     * other than consuming a few CPU cycles, but in such a way that
     * the JVM should not optimize them away.
     * 
     * Small overhead, so that can be used in benches.
     * 
     * If compilers can figure out that these calls "could be removed",
     * we are doomed.
     */
    
    /**
     * @param antiOptim A value.
     */
    public static void blackHole(int antiOptim) {
        if (antiOptim + Math.PI == Math.E) {
            System.out.println(BLACK_HOLE_SIDE_EFFECT_MESSAGE);
        }
    }

    /**
     * @param antiOptim A value.
     */
    public static void blackHole(long antiOptim) {
        /*
         * Conversion/promotion from long to double
         * can be slow for huge values, so we just swallow
         * the 32 LSBits.
         */
        blackHole((int) antiOptim);
    }

    /**
     * @param antiOptim A value.
     */
    public static void blackHole(double antiOptim) {
        /*
         * Cast from double to int should always be fast.
         */
        blackHole((int) antiOptim);
    }

    /**
     * @param antiOptim An object. Must not be null.
     */
    public static void blackHole(Object antiOptim) {
        /*
         * identityHashCode() should be fast.
         */
        blackHole(System.identityHashCode(antiOptim));
    }
    
    /*
     * 
     */
    
    /**
     * @param s A duration in seconds.
     * @return The specified duration in seconds, rounded to 3 digits past comma.
     */
    public static double sRounded(double s) {
        return Math.round(s*1e3)/1e3;
    }
    
    /**
     * @param ns A duration in nanoseconds.
     * @return The specified duration in seconds, rounded to 3 digits past comma.
     */
    public static double nsToSRounded(long ns) {
        return Math.round(ns/1e6)/1e3;
    }
    
    /**
     * @param ns A duration in nanoseconds.
     * @return The specified duration in milliseconds, rounded to 3 digits past comma.
     */
    public static double nsToMsRounded(long ns) {
        return Math.round(ns/1e3)/1e3;
    }
    
    /**
     * @param ns A duration in nanoseconds.
     * @return The specified duration in microseconds.
     */
    public static double nsToUs(long ns) {
        return ns/1e3;
    }

    /**
     * Sleeps in chunks of 10ms, to prevent the risk of a GC
     * eating the whole sleeping duration, and not letting
     * program enough duration to make progress.
     * 
     * Useful to ensure GC-proof-ness of tests sleeping for
     * some time to let concurrent treatment make progress.
     * 
     * @param ms Duration to sleep for, in milliseconds.
     */
    public static void sleepMSInChunks(long ms) {
        final long chunkMs = 10;
        while (ms >= chunkMs) {
            try {
                Thread.sleep(chunkMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ms -= chunkMs;
        }
    }
    
    /**
     * Sleeps 100 ms, flushes System.out and System.err,
     * and triggers a GC.
     */
    public static void settle() {
        // Wait first, in case some short busy-waits or alike are still busy.
        // 100ms should be enough.
        sleepMSInChunks(100);
        // err flush last for it helps error appearing last.
        System.out.flush();
        System.err.flush();
        System.gc();
    }

    /**
     * Settles and prints a new line.
     */
    public static void settleAndNewLine() {
        settle();
        System.out.println();
        System.out.flush();
    }

    /**
     * @return A new temp file, configured to be deleted on exit
     *         (not deleted if not properly closed,
     *         or in case of JVM crash).
     */
    public static File newTempFile() {
        try {
            final File file = File.createTempFile("jolikit_test_", ".tmp");
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static String readJVMInfo() {
        StringBuilder sb = new StringBuilder();
        final String[] SYSTEM_PROPERTIES = new String[] {
                "java.vm.name",
                "java.runtime.version",
                "java.class.version",
                "os.name",
                "os.arch",
                "os.version",
                "sun.arch.data.model"
        };
        for (int i = 0; i < SYSTEM_PROPERTIES.length; i++) {
            sb.append(SYSTEM_PROPERTIES[i] + "=" + System.getProperty(SYSTEM_PROPERTIES[i]));
            sb.append(LINE_SEPARATOR);
        }
        sb.append("availableProcessors: " + Runtime.getRuntime().availableProcessors());
        sb.append(LINE_SEPARATOR);
        sb.append("JVM input arguments: " + ManagementFactory.getRuntimeMXBean().getInputArguments());
        sb.append(LINE_SEPARATOR);
        return sb.toString();
    }
}
