/*
 * Copyright 2019-2024 Jeff Hain
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
package net.jolikit.threading.basics;

import java.util.concurrent.RejectedExecutionException;

public class CancellableUtils {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param runnable Can be null.
     * @return True if called onCancel(), false otherwise.
     */
    public static boolean call_onCancel_IfCancellable(Runnable runnable) {
        boolean ret = false;
        if (runnable instanceof InterfaceCancellable) {
            ((InterfaceCancellable) runnable).onCancel();
            ret = true;
        }
        return ret;
    }
    
    /**
     * @param runnable Can be null.
     * @throws RejectedExecutionException if the specified runnable
     *         is not null and is not a cancellable.
     */
    public static void call_onCancel_IfCancellableElseThrowREE(Runnable runnable) {
        if ((!call_onCancel_IfCancellable(runnable))
            && (runnable != null)) {
            throw new RejectedExecutionException();
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private CancellableUtils() {
    }
}
