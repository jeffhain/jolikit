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

/**
 * Exception to rethrow an exception (typically a checked exception),
 * to make it go up the stack without having to add throw clause
 * on methods all the way up.
 * 
 * For used instead of a simple RuntimeException, to clearly indicate
 * that it has no value other than allowing not to have to add throw clauses,
 * and that the meaningful exception is its cause.
 */
public class RethrowException extends RuntimeException {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final long serialVersionUID = 1L;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param cause Must not be null.
     */
    public RethrowException(String message, Throwable cause) {
        super(message, LangUtils.requireNonNull(cause));
    }

    /**
     * @param cause Must not be null.
     */
    public RethrowException(Throwable cause) {
        super(LangUtils.requireNonNull(cause));
    }
}
