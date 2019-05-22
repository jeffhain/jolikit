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
package net.jolikit.bwd.impl.jogl;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;

import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

/**
 * TODO jogl On Mac, this listener can be called,
 * not in the usual UI thread, which looks like
 * "[main-Display-.macosx_nil-1-EDT-1,5,main]",
 * but in some "[AppKit Thread,5,system]" thread.
 * 
 * Ensures that the backing listener is called in UI thread.
 */
public class JoglWindowListenerUiThreader implements WindowListener {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final WindowListener backingListener;
    
    private final InterfaceWorkerAwareScheduler uiThreadScheduler;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JoglWindowListenerUiThreader(
            WindowListener backingListener,
            InterfaceWorkerAwareScheduler uiThreadScheduler) {
        this.backingListener = backingListener;
        this.uiThreadScheduler = uiThreadScheduler;
    }
    
    @Override
    public void windowRepaint(final WindowUpdateEvent backingEvent) {
        if (this.uiThreadScheduler.isWorkerThread()) {
            this.backingListener.windowRepaint(backingEvent);
        } else {
            this.uiThreadScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    backingListener.windowRepaint(backingEvent);
                }
            });
        }
    }
    
    @Override
    public void windowMoved(final WindowEvent backingEvent) {
        if (this.uiThreadScheduler.isWorkerThread()) {
            this.backingListener.windowMoved(backingEvent);
        } else {
            this.uiThreadScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    backingListener.windowMoved(backingEvent);
                }
            });
        }
    }
    
    @Override
    public void windowResized(final WindowEvent backingEvent) {
        if (this.uiThreadScheduler.isWorkerThread()) {
            this.backingListener.windowResized(backingEvent);
        } else {
            this.uiThreadScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    backingListener.windowResized(backingEvent);
                }
            });
        }
    }
    
    @Override
    public void windowGainedFocus(final WindowEvent backingEvent) {
        if (this.uiThreadScheduler.isWorkerThread()) {
            this.backingListener.windowGainedFocus(backingEvent);
        } else {
            this.uiThreadScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    backingListener.windowGainedFocus(backingEvent);
                }
            });
        }
    }
    
    @Override
    public void windowLostFocus(final WindowEvent backingEvent) {
        if (this.uiThreadScheduler.isWorkerThread()) {
            this.backingListener.windowLostFocus(backingEvent);
        } else {
            this.uiThreadScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    backingListener.windowLostFocus(backingEvent);
                }
            });
        }
    }
    
    @Override
    public void windowDestroyNotify(final WindowEvent backingEvent) {
        if (this.uiThreadScheduler.isWorkerThread()) {
            this.backingListener.windowDestroyNotify(backingEvent);
        } else {
            this.uiThreadScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    backingListener.windowDestroyNotify(backingEvent);
                }
            });
        }
    }
    
    @Override
    public void windowDestroyed(final WindowEvent backingEvent) {
        if (this.uiThreadScheduler.isWorkerThread()) {
            this.backingListener.windowDestroyed(backingEvent);
        } else {
            this.uiThreadScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    backingListener.windowDestroyed(backingEvent);
                }
            });
        }
    }
}
