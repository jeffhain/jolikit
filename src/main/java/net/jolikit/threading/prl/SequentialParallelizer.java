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

/**
 * Does not parallelize, just executes the specified runnables synchronously.
 */
public class SequentialParallelizer implements InterfaceParallelizer {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final SequentialParallelizer DEFAULT_INSTANCE = new SequentialParallelizer();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public static SequentialParallelizer getDefault() {
        return DEFAULT_INSTANCE;
    }
    
    public SequentialParallelizer() {
    }
    
    /**
     * @return 1.
     */
    @Override
    public int getParallelism() {
        return 1;
    }

    /**
     * Executes the specified runnable synchronously.
     */
    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }
}
