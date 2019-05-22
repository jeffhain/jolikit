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
 * Locker based on an object's monitor.
 */
public class MonitorLocker implements InterfaceLocker {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Object mutex;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param mutex Object which monitor is to be used for locking.
     */
    public MonitorLocker(Object mutex) {
        this.mutex = mutex;
    }
    
    /**
     * @return Object which monitor is used for locking.
     */
    public Object getMutex() {
        return this.mutex;
    }

    @Override
    public void runInLock(Runnable runnable) {
        synchronized (this.mutex) {
            runnable.run();
        }
    }
    
    @Override
    public <V> V callInLock(Callable<V> callable) throws Exception {
        synchronized (this.mutex) {
            return callable.call();
        }
    }
}
