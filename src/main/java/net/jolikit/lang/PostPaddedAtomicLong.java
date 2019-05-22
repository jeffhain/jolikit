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

import java.util.concurrent.atomic.AtomicLong;

/**
 * AtomicLong cache-line-padded after the volatile value.
 */
public class PostPaddedAtomicLong extends AtomicLong {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Padding for 64 bytes cache lines.
     */
    volatile long p1,p2,p3,p4,p5,p6,p7;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Creates a new PostPaddedAtomicLong with the given initial value.
     *
     * @param initialValue the initial value
     */
    public PostPaddedAtomicLong(long initialValue) {
        super(initialValue);
        this.setPadding(-1L);
    }

    /**
     * Creates a new PostPaddedAtomicLong with initial value {@code 0}.
     */
    public PostPaddedAtomicLong() {
        this(0L);
    }
    
    /**
     * @param value Value to set padding with.
     */
    public void setPadding(long value) {
        p1 = p2 = p3 = p4 = p5 = p6 = p7 = value;
    }
}
