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
package net.jolikit.test.utils;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;

import net.jolikit.lang.Dbg;

/**
 * To report assertions errors from any thread
 * (JUnit's assertXXX methods don't work well from alien threads).
 */
public class ConcUnit {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    private final AtomicReference<Throwable> firstError = new AtomicReference<Throwable>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ConcUnit() {
    }
    
    /**
     * Resets first error to null.
     */
    public void reset() {
        this.firstError.set(null);
    }
    
    /**
     * Logs the error if it's first one to be reported.
     * 
     * @throws An Error, even if it's not first error to be reported.
     */
    public void onError(String msg) {
        onError(new AssertionError(msg));
    }

    /**
     * Logs the error if it's first one to be reported.
     * 
     * @throws An Error, even if it's not first error to be reported.
     */
    public void onError(Throwable error) {
        if (this.firstError.compareAndSet(null, error)) {
            logAndFlush(error);
            this.onFirstErrorReport(error);
        }
        throwError(error);
    }

    /**
     * Logs the error if it's first one to be reported.
     * 
     * @throws An Error, even if it's not first error to be reported.
     */
    public void onError(String msg, Throwable error) {
        if (this.firstError.compareAndSet(null, error)) {
            logAndFlush(msg, error);
            this.onFirstErrorReport(error);
        }
        throwError(error);
    }
    
    /**
     * Call it at the end of your test,
     * typically in tearDown() if using JUnit.
     */
    public void assertNoError() {
        final Throwable error = this.firstError.get();
        if (error != null) {
            logAndFlush(error);
            this.onNoErrorAssertFail(error);
            throwError(error);
        }
    }

    /*
     * Assertion methods.
     */

    public void fail() {
        onError("fail");
    }
    
    public void assertTrue(boolean a) {
        if (!a) {
            onError("expected true, got false");
        }
    }

    public void assertTrue(String msg, boolean a) {
        if (!a) {
            onError(msg+": expected true, got false");
        }
    }

    public void assertFalse(boolean a) {
        if (a) {
            onError("expected false, got true");
        }
    }

    public void assertFalse(String msg, boolean a) {
        if (a) {
            onError(msg+": expected false, got true");
        }
    }

    /**
     * To avoid Integer/Long non-equal problem.
     */
    public void assertEquals(long a, long b) {
        assertEquals(new Long(a), new Long(b));
    }

    public void assertEquals(String msg, long a, long b) {
        assertEquals(msg, new Long(a), new Long(b));
    }

    public void assertEquals(Object a, Object b) {
        if (!equalOrBothNull(a, b)) {
            onError("expected " + a + ", got " + b);
        }
    }

    public void assertEquals(String msg, Object a, Object b) {
        if (!equalOrBothNull(a, b)) {
            onError(msg + ": expected " + a + ", got " + b);
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    /**
     * Called on first error reporting.
     * Default implementation does nothing.
     * Can be used to add logs or whatever.
     * 
     * An AssertionError is thrown just after this call,
     * unless it throws.
     */
    protected void onFirstErrorReport(Throwable error) {
    }

    /**
     * Called when checking that no error has been reported,
     * if one has been reported.
     * 
     * An AssertionError is thrown just after this call,
     * unless it throws.
     */
    protected void onNoErrorAssertFail(Throwable error) {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Copy-pasted from LangUtils, to avoid cyclic dependencies.
     * 
     * @return True if the specified objects are equal or both null.
     */
    private static boolean equalOrBothNull(Object a, Object b) {
        if (a == b) {
            return true;
        } else {
            return (a != null) ? a.equals(b) : false;
        }
    }

    private static void throwError(Throwable error) {
        if (error instanceof Error) {
            throw (Error)error;
        } else {
            throw new AssertionError(error);
        }
    }
    
    private static void logAndFlush(Throwable error) {
        logAndFlush(null, error);
    }
    
    /**
     * @param msg Can be null.
     */
    private static void logAndFlush(String msg, Throwable error) {
        final String feStr = "first error:";
        /*
         * 
         */
        for (PrintStream stream : new PrintStream[]{System.out, System.err}) {
            synchronized (stream) {
                if (msg != null) {
                    stream.println(msg);
                }
                stream.println(feStr);
                error.printStackTrace(stream);
            }
            stream.flush();
        }
        /*
         * 
         */
        if (msg != null) {
            Dbg.log(msg);
        }
        Dbg.log(feStr, error);
        Dbg.flush();
    }
}
