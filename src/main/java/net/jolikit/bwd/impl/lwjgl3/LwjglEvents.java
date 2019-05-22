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
package net.jolikit.bwd.impl.lwjgl3;

/**
 * Contains classes to hold data of backing events,
 * for there is none in LWJGL.
 */
public class LwjglEvents {

    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------

    /*
     * Key events.
     */
    
    public static class LwjglKeyEvent {
        public final int key;
        public final int scancode;
        public final int action;
        public final int mods;
        public LwjglKeyEvent(
                int key,
                int scancode,
                int action,
                int mods) {
            this.key = key;
            this.scancode = scancode;
            this.action = action;
            this.mods = mods;
        }
        @Override
        public String toString() {
            return "[key = " + key + ", scancode = " + scancode + ", action = " + action + ", mods = " + mods + "]";
        }
    }

    public static class LwjglCharEvent {
        public final int codepoint;
        public LwjglCharEvent(int codepoint) {
            this.codepoint = codepoint;
        }
        @Override
        public String toString() {
            return "[codepoint = " + codepoint + "]";
        }
    }

    public static class LwjglCharModsEvent {
        public final int codepoint;
        public final int mods;
        public LwjglCharModsEvent(
                int codepoint,
                int mods) {
            this.codepoint = codepoint;
            this.mods = mods;
        }
        @Override
        public String toString() {
            return "[codepoint = " + codepoint + ", mods = " + mods + "]";
        }
    }
    
    /*
     * Mouse events.
     */

    public static class LwjglCursorPosEvent {
        public final double xpos;
        public final double ypos;
        public LwjglCursorPosEvent(
                double xpos,
                double ypos) {
            this.xpos = xpos;
            this.ypos = ypos;
        }
        @Override
        public String toString() {
            return "[xpos = " + xpos + ", ypos = " + ypos + "]";
        }
    }
    
    public static class LwjglCursorEnterEvent {
        public final boolean entered;
        public LwjglCursorEnterEvent(boolean entered) {
            this.entered = entered;
        }
        @Override
        public String toString() {
            return "[entered = " + entered + "]";
        }
    }

    public static class LwjglMouseButtonEvent {
        public final int button;
        public final int action;
        public final int mods;
        public LwjglMouseButtonEvent(
                int button,
                int action,
                int mods) {
            this.button = button;
            this.action = action;
            this.mods = mods;
        }
        @Override
        public String toString() {
            return "[button = " + button + ", action = " + action + ", mods = " + mods + "]";
        }
    }
    
    /*
     * Wheel events.
     */
    
    public static class LwjglScrollEvent {
        public final double xoffset;
        public final double yoffset;
        public LwjglScrollEvent(
                double xoffset,
                double yoffset) {
            this.xoffset = xoffset;
            this.yoffset = yoffset;
        }
        @Override
        public String toString() {
            return "[xoffset = " + xoffset + ", yoffset = " + yoffset + "]";
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private LwjglEvents() {
    }
}
