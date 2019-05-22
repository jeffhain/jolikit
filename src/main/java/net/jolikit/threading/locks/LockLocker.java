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
import java.util.concurrent.locks.Lock;

/**
 * Locker based on a lock.
 */
public class LockLocker implements InterfaceLocker {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final Lock lock;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param lock Lock to use for locking.
     */
    public LockLocker(Lock lock) {
        this.lock = lock;
    }

    /**
     * @return Lock used for locking.
     */
    public Lock getLock() {
        return this.lock;
    }
    
    @Override
    public void runInLock(Runnable runnable) {
        final Lock lock = this.lock;
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public <V> V callInLock(Callable<V> callable) throws Exception {
        final Lock lock = this.lock;
        lock.lock();
        try {
            return callable.call();
        } finally {
            lock.unlock();
        }
    }
}
