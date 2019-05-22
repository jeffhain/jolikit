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
package net.jolikit.time.clocks;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Does not accept null listener.
 * 
 * Listeners add/removal methods are thread-safe and blocking (synchronization).
 * 
 * Notification method must not be called concurrently, typically only
 * from clock's modification's lock.
 * 
 * If there is no listener, listener notification
 * methods return immediately without synchronization.
 */
public class ClockListeners {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final HashSet<InterfaceClockModificationListener> listeners = new HashSet<InterfaceClockModificationListener>();
    
    private final InterfaceClock clock;
    
    /**
     * To avoid synchronization if there is no listener
     * (might act wrongly, but only in case of add/removal
     * of listener concurrent with modification, so no biggie).
     */
    private volatile boolean someListeners;
    
    /*
     * temps
     */
    
    private final ArrayList<InterfaceClockModificationListener> tmpListeners = new ArrayList<InterfaceClockModificationListener>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param clock Clock which is listened to.
     */
    public ClockListeners(InterfaceClock clock) {
        this.clock = clock;
    }
    
    /**
     * Thread-safe and non-blocking.
     * @return True if there are (or were-soon or will-be-soon) no listener
     *         to be notified, false otherwise.
     */
    public boolean isEmpty() {
        return !this.someListeners;
    }

    public boolean addListener(InterfaceClockModificationListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        synchronized (this.listeners) {
            boolean didAdd = this.listeners.add(listener);
            if (didAdd) {
                this.someListeners = (this.listeners.size() != 0);
            }
            return didAdd;
        }
    }
    
    public boolean removeListener(InterfaceClockModificationListener listener) {
        synchronized (this.listeners) {
            boolean didRemove = this.listeners.remove(listener);
            if (didRemove) {
                this.someListeners = (this.listeners.size() != 0);
            }
            return didRemove;
        }
    }

    public void notifyListeners() {
        if (!this.someListeners) {
            return;
        }
        
        this.tmpListeners.clear();
        
        // Retrieving elements to iterate outside lock,
        // to avoid possible deadlocks.
        synchronized (this.listeners) {
            this.tmpListeners.addAll(this.listeners);
        }
        
        final int nbrOfListeners = this.tmpListeners.size();
        for (int i = 0; i < nbrOfListeners; i++) {
            InterfaceClockModificationListener tmpListener = this.tmpListeners.get(i);
            tmpListener.onClockModification(this.clock);
        }
    }
}
