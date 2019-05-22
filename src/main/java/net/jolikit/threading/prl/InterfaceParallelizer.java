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

import java.util.concurrent.Executor;

/**
 * An Executor that might recognize implementations of InterfaceSplittable and
 * InterfaceSplitmergable, i.e. split them and execute them in parallel when
 * suitable, in addition to other Runnables, that must be executed as a
 * splittables or splitmergables that would not be worth to split.
 * 
 * Unless specified otherwise, implementation must be reentrant.
 * 
 * Unless specified otherwise, implementation is allowed to consider a
 * splitmergable resulting from the split of a splittable, as a simple
 * splittable (without merge being taken care of), or as an error case.
 */
public interface InterfaceParallelizer extends Executor {
    
    /**
     * Actual parallelism can be higher, for example if this parallelizer has no
     * own worker thread but can use multiple user threads as worker threads, or
     * if it uses user threads along with its own worker threads.
     * 
     * @return The parallelism level (>= 1) of this parallelizer, i.e. 1 if it
     *         has no own worker thread and only uses user thread(s) as worker
     *         thread(s), or its target parallelism otherwise.
     */
    public int getParallelism();

    /**
     * Must not return before work completion (normal or exceptional).
     * 
     * In case of exceptional completion, an exception must be (re)thrown to
     * indicate at least the first detected exception, and depending on
     * implementation, merges (in case of splitmergable) might either be done
     * with "null" where computation failed, or be aborted (fail-fast) in which
     * case there is no merged result to be expected.
     * 
     * @param runnable Runnable, splittable or splitmergable to run. If it is a
     *        splitmergable, this argument must hold the result if any.
     */
    @Override
    public void execute(Runnable runnable);
}
