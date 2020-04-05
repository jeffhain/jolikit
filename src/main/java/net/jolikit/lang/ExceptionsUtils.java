/*
 * Copyright 2020 Jeff Hain
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

/**
 * Utilities to deal with exceptions.
 * 
 * @see RethrowException, Unchecked.
 */
public class ExceptionsUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * If the try block completed exceptionally (threw), swallows the specified
     * exception throw from finally block, to avoid it suppressing the one
     * thrown from the try block, else lets it go up by re-raising it
     * (either directly or using a RethrowException, it's not specified).
     * 
     * Not meant to be used every time untrusted/user code is called
     * in a finally block, but should be at least for low level utilities,
     * to avoid having to go too deep while debugging.
     * 
     * The specified boolean can be computed as this: initialize it to false
     * before entering the try block, and set it to true at the end of the
     * try block (before catch or finally blocks) as well as before any
     * return done in it.
     * 
     * NB: From Java 7, instead, can use addSuppressed() along with
     * more catches etc., but it shows up the first exception
     * at the bottom of the log, and we are fine (as a general rule)
     * with only showing the first issue that occurs
     * (showing everything doesn't scale, in particular
     * when doing parallelization).
     * 
     * @param t A throwable throw in a finally block, i.e. that would,
     *        if not catched and ignored, suppress any throwable thrown
     *        by the try block (or by catch blocks).
     * @param tryCompletedNormally True if the try block corresponding
     *        to the finally block did complete normally, false otherwise.
     */
    public static void swallowIfTryThrew(Throwable t, boolean tryCompletedNormally) {
        if (tryCompletedNormally) {
            Unchecked.throwIt(t);
        } else {
            // Swallowing.
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private ExceptionsUtils() {
    }
}
