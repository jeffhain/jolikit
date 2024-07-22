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
package net.jolikit.internal.nodepto.commonslogging;

public class LogFactory {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean MUST_LOG_TRACE_AND_HIGHER = false;
    private static final boolean MUST_LOG_WARN_AND_HIGHER = MUST_LOG_TRACE_AND_HIGHER || true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyLogger implements Log {
        @Override
        public boolean isFatalEnabled() {
            return MUST_LOG_WARN_AND_HIGHER;
        }
        @Override
        public boolean isErrorEnabled() {
            return MUST_LOG_WARN_AND_HIGHER;
        }
        @Override
        public boolean isWarnEnabled() {
            return MUST_LOG_WARN_AND_HIGHER;
        }
        @Override
        public boolean isInfoEnabled() {
            return MUST_LOG_TRACE_AND_HIGHER;
        }
        @Override
        public boolean isDebugEnabled() {
            return MUST_LOG_TRACE_AND_HIGHER;
        }
        @Override
        public boolean isTraceEnabled() {
            return MUST_LOG_TRACE_AND_HIGHER;
        }
        /*
         * 
         */
        @Override
        public void fatal(Object message) {
            if (MUST_LOG_WARN_AND_HIGHER) {
                System.err.println(message);
            }
        }
        @Override
        public void error(Object message) {
            if (MUST_LOG_WARN_AND_HIGHER) {
                System.err.println(message);
            }
        }
        @Override
        public void warn(Object message) {
            if (MUST_LOG_WARN_AND_HIGHER) {
                System.out.println(message);
            }
        }
        @Override
        public void info(Object message) {
            if (MUST_LOG_TRACE_AND_HIGHER) {
                System.out.println(message);
            }
        }
        @Override
        public void debug(Object message) {
            if (MUST_LOG_TRACE_AND_HIGHER) {
                System.out.println(message);
            }
        }
        @Override
        public void trace(Object message) {
            if (MUST_LOG_TRACE_AND_HIGHER) {
                System.out.println(message);
            }
        }
        /*
         * 
         */
        @Override
        public void fatal(Object message, Throwable t) {
            if (MUST_LOG_WARN_AND_HIGHER) {
                synchronized (System.err) {
                    System.err.println(message);
                    t.printStackTrace(System.err);
                }
            }
        }
        @Override
        public void error(Object message, Throwable t) {
            if (MUST_LOG_WARN_AND_HIGHER) {
                synchronized (System.err) {
                    System.err.println(message);
                    t.printStackTrace(System.err);
                }
            }
        }
        @Override
        public void warn(Object message, Throwable t) {
            if (MUST_LOG_WARN_AND_HIGHER) {
                synchronized (System.out) {
                    System.out.println(message);
                    t.printStackTrace(System.out);
                }
            }
        }
        @Override
        public void info(Object message, Throwable t) {
            if (MUST_LOG_TRACE_AND_HIGHER) {
                synchronized (System.out) {
                    System.out.println(message);
                    t.printStackTrace(System.out);
                }
            }
        }
        @Override
        public void debug(Object message, Throwable t) {
            if (MUST_LOG_TRACE_AND_HIGHER) {
                synchronized (System.out) {
                    System.out.println(message);
                    t.printStackTrace(System.out);
                }
            }
        }
        @Override
        public void trace(Object message, Throwable t) {
            if (MUST_LOG_TRACE_AND_HIGHER) {
                synchronized (System.out) {
                    System.out.println(message);
                    t.printStackTrace(System.out);
                }
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final MyLogger LOGGER = new MyLogger();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    @SuppressWarnings("unused")
    public static Log getLog(String name) {
        return LOGGER;
    }
    
    @SuppressWarnings("unused")
    public static Log getLog(Class<?> clazz) {
        return LOGGER;
    }
}
