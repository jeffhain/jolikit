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
package net.jolikit.time.sched;

public class SchedUtils {
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MySchedulableRunnableAdapter implements InterfaceSchedulable {
        private final Runnable runnable;
        public MySchedulableRunnableAdapter(Runnable runnable) {
            this.runnable = runnable;
        }
        @Override
        public void setScheduling(InterfaceScheduling scheduling) {
            // No use for it.
        }
        @Override
        public void run() {
            this.runnable.run();
        }
        @Override
        public void onCancel() {
            // Nothing to do.
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static InterfaceSchedulable asSchedulable(Runnable runnable) {
        if (runnable instanceof InterfaceSchedulable) {
            return (InterfaceSchedulable) runnable;
        } else {
            return new MySchedulableRunnableAdapter(runnable);
        }
    }

    public static void call_setScheduling_IfSchedulable(
            Runnable runnable,
            InterfaceScheduling scheduling) {
        if (runnable instanceof InterfaceSchedulable) {
            ((InterfaceSchedulable) runnable).setScheduling(scheduling);
        }
    }

    public static void call_onCancel_IfCancellable(Runnable runnable) {
        if (runnable instanceof InterfaceCancellable) {
            ((InterfaceCancellable) runnable).onCancel();
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private SchedUtils() {
    }
}
