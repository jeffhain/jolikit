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
package net.jolikit.threading.prl;

import net.jolikit.lang.LangUtils;

/**
 * Splittable which is not worth to split, backed by a Runnable.
 * Useful to use Runnable under the form of a splittable, to keep things simple.
 */
public class NonSplittable implements InterfaceSplittable {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final Runnable runnable;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @throws NullPointerException if the specified Runnable is null.
     */
    public NonSplittable(Runnable runnable) {
        this.runnable = LangUtils.requireNonNull(runnable);
    }

    /**
     * @return false.
     */
    @Override
    public boolean worthToSplit() {
        return false;
    }

    /**
     * @throws UnsupportedOperationException.
     */
    @Override
    public InterfaceSplittable split() {
        throw new UnsupportedOperationException();
    }

    /**
     * Runs the backing Runnable.
     */
    @Override
    public void run() {
        this.runnable.run();
    }
}
