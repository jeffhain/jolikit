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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods not to bother with checked exceptions,
 * typically by throwing wrapping unchecked exceptions instead.
 */
public class Unchecked {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Calls Thread.sleep(long).
     */
    public static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Restoring interrupted status.
            Thread.currentThread().interrupt();
            throw new RethrowException(e);
        }
    }

    /**
     * Calls Object.wait().
     */
    public static void wait(Object object) {
        try {
            object.wait();
        } catch (InterruptedException e) {
            // Restoring interrupted status.
            Thread.currentThread().interrupt();
            throw new RethrowException(e);
        }
    }

    /**
     * Calls Object.wait(long).
     * Take care that if ms is 0, waits until notified.
     */
    public static void waitMs(Object object, long ms) {
        try {
            object.wait(ms);
        } catch (InterruptedException e) {
            // Restoring interrupted status.
            Thread.currentThread().interrupt();
            throw new RethrowException(e);
        }
    }

    /**
     * Calls ExecutorService.awaitTermination(Long.MAX_VALUE,TimeUnit.NANOSECONDS).
     */
    public static void awaitTermination(ExecutorService executor) {
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            // Restoring interrupted status.
            Thread.currentThread().interrupt();
            throw new RethrowException(e);
        }
    }

    /**
     * Calls ExecutorService.shutdown()
     * and then ExecutorService.awaitTermination(Long.MAX_VALUE,TimeUnit.NANOSECONDS).
     */
    public static void shutdownAndAwaitTermination(ExecutorService executor) {
        executor.shutdown();
        awaitTermination(executor);
    }

    /**
     * Throws the specified throwable.
     * You might want to use RethrowException instead (no hack, and no
     * surprising checked exception coming out of methods that don't allow
     * them), but this can be handy, and is simpler (no wrapping exception).
     * 
     * If the specified throwable is null, does nothing.
     * 
     * @param t A throwable.
     * @return null. Allows to write "throw throwIt(Throwable)" in a catch
     *         clause, and avoid a compilation error, letting the compiler
     *         believe the returned exception will be thrown, while actually the
     *         specified one shall be.
     * @throws The specified throwable, if not null.
     */
    public static RuntimeException throwIt(Throwable t) {
        if (t != null) {
            Unchecked.<RuntimeException>uncheckedThrow(t);
        }
        return null;
    }
    
    /**
     * @param t Must not be null.
     * @return The specified throwable if it's a RethrowException,
     *         else a RethrowException wrapping it.
     */
    public static RethrowException wrapIfNeeded(Throwable t) {
        if (t instanceof RethrowException) {
            return (RethrowException) t;
        } else {
            return new RethrowException(t);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Sneaky throw hack, relying on generics limitations to evade
     * compiler complaints about rethrowing unchecked exceptions.
     * 
     * @param t A throwable. Must not be null.
     * @throws The specified throwable.
     */
    private static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t;
    }
}
