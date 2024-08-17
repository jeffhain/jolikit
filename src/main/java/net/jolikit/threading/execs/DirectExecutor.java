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
package net.jolikit.threading;

import java.util.concurrent.Executor;

/**
 * Executor that synchronously executes the specified Runnable.
 */
public class SynchronousExecutor implements Executor {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final SynchronousExecutor DEFAULT_INSTANCE = new SynchronousExecutor();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SynchronousExecutor() {
    }
    
    /**
     * @return A default instance.
     */
    public static SynchronousExecutor getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }
    
    /**
     * @param runnable Runnable to execute synchronously.
     * @throws NullPointerException if the specified Runnable is null.
     */
    @Override
    public void execute(Runnable runnable) {
        runnable.run();
    }
}
