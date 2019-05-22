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

/**
 * Interface for clocks which modifications (*) can be listened to.
 * 
 * (*) IMPORTANT NOTE: Hard clocks time might jump without
 * listeners being notified, due to potential system
 * time jumps. If system time can be expected to have
 * important jumps, you might want to frequently check for
 * actual clock's time and not only rely on listeners
 * to stop a waiting for example.
 */
public interface InterfaceListenableClock extends InterfaceClock {
    
    /**
     * @param listener The listener to add. Must not be null.
     * @return True if listener was added, false otherwise, i.e. if it was already registered.
     */
    public boolean addListener(InterfaceClockModificationListener listener);
    
    /**
     * @param listener The listener to remove. Can be null (no effect).
     * @return True if listener was removed, false otherwise, i.e. if it was not registered.
     */
    public boolean removeListener(InterfaceClockModificationListener listener);
}
