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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

public class DefaultThreadFactory implements ThreadFactory {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyRunnable implements Runnable {
        private final Runnable backingRunnable;
        private MyRunnable(Runnable backingRunnable) {
            this.backingRunnable = backingRunnable;
        }
        @Override
        public void run() {
            final Runnable r = this.backingRunnable;
            // Keeps calling run() until it completes normally
            // or handler throws.
            while (true) {
                try {
                    r.run();
                    // Completed normally: we are done.
                    break;
                } catch (Throwable t) {
                    exceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final boolean DEFAULT_MUST_SWALLOW_ELSE_RETHROW = true;
    
    private final ThreadFactory backingThreadFactory;

    private final UncaughtExceptionHandler exceptionHandler;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Uses Thread constructor, and logs and error and calls runnables again
     * if they throw.
     */
    public DefaultThreadFactory() {
        this(null, new DefaultExceptionHandler(DEFAULT_MUST_SWALLOW_ELSE_RETHROW));
    }
    
    /**
     * @param backingThreadFactory Thread factory to use. Can be null, in which
     *        case Thread constructor is used.
     * @param exceptionHandler Must not be null.
     */
    public DefaultThreadFactory(
            ThreadFactory backingThreadFactory,
            UncaughtExceptionHandler exceptionHandler) {
        this.backingThreadFactory = backingThreadFactory;
        this.exceptionHandler = LangUtils.requireNonNull(exceptionHandler);
    }
    
    @Override
    public Thread newThread(Runnable r) {
        final MyRunnable mr = new MyRunnable(r);
        final Thread thread;
        if (this.backingThreadFactory != null) {
            thread = this.backingThreadFactory.newThread(mr);
        } else {
            thread = new Thread(mr);
        }
        return thread;
    }
}
