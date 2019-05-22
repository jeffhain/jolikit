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

import java.util.Collection;

/**
 * Designed for developer-oriented debug logs (not occurring in production),
 * or for tests logs, for them to be consistent with eventual debug logs.
 * 
 * Useful as indirection layer, to change actual logging treatments
 * without having to change code everywhere.
 */
public class Dbg {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    static {
        HeisenLogger.setMustLogTime(true);
        HeisenLogger.setMustUsePrettyTimeFormat(true);
        // Shorter with just the logger (thread-local) id.
        HeisenLogger.setMustLogThread(true);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static void log(Object... messages) {
        HeisenLogger.log(messages);
    }
    
    /**
     * Logs with class name of the specified caller's class, minus package,
     * such as "A$B", as inserted first element.
     * "pr" for prefix.
     * 
     * Can help a lot to figure out where the log comes from.
     */
    public static void logPr(Object caller, Object... messages) {
        
        final String prefix;
        {
            String tmp = caller.getClass().getName();
            final int lastDotIndex = tmp.lastIndexOf('.');
            if (lastDotIndex >= 0) {
                tmp = tmp.substring(lastDotIndex + 1);
            }
            prefix = tmp;
        }
        
        final Object[] newMsgArr = new Object[messages.length + 1];
        newMsgArr[0] = prefix;
        System.arraycopy(messages, 0, newMsgArr, 1, messages.length);
        
        log(newMsgArr);
    }
    
    /**
     * Does one log(...) call per element,
     * not to log the whole collection in a single line.
     */
    public static <T> void logColl(Collection<T> coll) {
        int i = 0;
        for (T element : coll) {
            log("coll[" + i + "] = " + element);
            i++;
        }
    }
    
    public static void flush() {
        HeisenLogger.flushPendingLogsAndStream();
    }
    
    public static void logStrackTrace() {
        log(new RuntimeException("for stack"));
    }
    
    public static void flushAndExit() {
        /*
         * Always printing stack trace, to make it easy
         * to find where exit was done.
         */
        log("EXIT", new RuntimeException("for stack"));
        flush();
        System.exit(1);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private Dbg() {
    }
}
