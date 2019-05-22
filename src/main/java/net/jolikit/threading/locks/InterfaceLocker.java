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
package net.jolikit.threading.locks;

import java.util.concurrent.Callable;

/**
 * Interface to run treatments in a lock, whatever its type (intrinsic or not).
 */
public interface InterfaceLocker {

    /**
     * @param runnable Runnable to run in lock.
     */
    public void runInLock(Runnable runnable);

    /**
     * @param callable Callable to call in lock.
     */
    public <V> V callInLock(Callable<V> callable) throws Exception;
}
