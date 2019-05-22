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
 * A treatment that can be split, typically for parallelization.
 * 
 * Extends Runnable so that can be run sequentially by any Executor.
 * 
 * For a same instance, if a method of this interface throws an exception,
 * treatments must be aborted (no other method called).
 * 
 * For a same parallelization session, methods of a same instance are to be used
 * by only one worker thread, except worthToSplit() method, which might be first
 * called by another thread, since depending on its result the splittable might
 * not be sent to a worker thread, and computed on place.
 */
public interface InterfaceSplittable extends Runnable {
    
    /**
     * @return True if this splittable is worth to be split (and forked),
     *         false otherwise, i.e. if its run() method should be called next.
     */
    public boolean worthToSplit();
    
    /**
     * Should try to split the amount of work into approximately equal parts.
     * 
     * Must only be called if last call to worthToSplit() returned true.
     * 
     * @return The new splittable resulting from the split of this splittable
     *         (into a modified self and the returned new splittable).
     */
    public InterfaceSplittable split();
}
