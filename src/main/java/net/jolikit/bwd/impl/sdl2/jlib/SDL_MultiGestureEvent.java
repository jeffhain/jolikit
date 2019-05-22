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
package net.jolikit.bwd.impl.sdl2.jlib;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

/**
 *  \brief Multiple Finger Gesture Event (event.mgesture.*)
 * typedef struct SDL_MultiGestureEvent
 * {
 *     Uint32 type;        // ::SDL_MULTIGESTURE
 *     Uint32 timestamp;
 *     SDL_TouchID touchId; // The touch device index
 *     float dTheta;
 *     float dDist;
 *     float x;
 *     float y;
 *     Uint16 numFingers;
 *     Uint16 padding;
 * } SDL_MultiGestureEvent;
 */
public class SDL_MultiGestureEvent extends Structure {
    public static class ByReference extends SDL_MultiGestureEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_MultiGestureEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public long touchId;
    public float dTheta;
    public float dDist;
    public float x;
    public float y;
    public short numFingers;
    public short padding;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "type", "timestamp", "touchId",
                "dTheta", "dDist", "x", "y",
                "numFingers", "padding");
    }
}
