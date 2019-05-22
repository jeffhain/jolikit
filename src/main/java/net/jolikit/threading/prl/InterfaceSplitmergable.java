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
 * A treatment that can be split, typically for parallelization, and then
 * requires merge, either for result computation or just for waiting for
 * completion.
 * 
 * For splitmergers that can merge forker into forked, merge(...) method can be
 * called by initial thread, at the end of parallelization session, for result
 * setting.
 */
public interface InterfaceSplitmergable extends InterfaceSplittable {
    
    /*
     * We want to allow splitmergers implementations to either merge forked
     * into forker, or forker into forked, which can make their implementation
     * easier.
     * Having a merge(InterfaceSplitmergable,InterfaceSplitmergable) method,
     * instead of just merge(InterfaceSplitmergable), allows to copy end
     * result into the splitmergable specified to a splitmerger, so that we
     * don't need to return the instance that would otherwise hold the result
     * (due to unspecified merge direction), and can use Executor interface
     * for splitmergers.
     */
    
    // Overriding for typing.
    @Override
    public InterfaceSplitmergable split();
    
    /**
     * Merges the specified splitmergables, and sets the result in this
     * splitmergable.
     * Any (but not both) of the specified splitmergables can be null, for
     * example if an instance threw an exception and can't be used.
     * Any (but not both) of the specified splitmergables can be this
     * splitmergable.
     * 
     * Merge must be commutative, since whether (a,b) is (forker,forked) or
     * (forked,forker) depends on parallelizer's implementation and runtime
     * behavior.
     * 
     * @param a Splitmergable to merge with "b", if non-null, into this one.
     *        Can be null.
     * @param b Splitmergable to merge with "a", if non-null, into this one.
     *        Can be null.
     */
    public void merge(InterfaceSplitmergable a, InterfaceSplitmergable b);
}
