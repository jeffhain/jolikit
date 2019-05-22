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
 *  \brief Touch finger event structure (event.tfinger.*)
 * typedef struct SDL_TouchFingerEvent
 * {
 *     Uint32 type;        // ::SDL_FINGERMOTION or ::SDL_FINGERDOWN or ::SDL_FINGERUP
 *     Uint32 timestamp;
 *     SDL_TouchID touchId; // The touch device id
 *     SDL_FingerID fingerId;
 *     float x;            // Normalized in the range 0...1
 *     float y;            // Normalized in the range 0...1
 *     float dx;           // Normalized in the range -1...1
 *     float dy;           // Normalized in the range -1...1
 *     float pressure;     // Normalized in the range 0...1
 * } SDL_TouchFingerEvent;
 */
public class SDL_TouchFingerEvent extends Structure {
    public static class ByReference extends SDL_TouchFingerEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_TouchFingerEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public long touchId;
    public long fingerId;
    public float x;
    public float y;
    public float dx;
    public float dy;
    public float pressure;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "type", "timestamp", "touchId", "fingerId",
                "x", "y", "dx", "dy",
                "pressure");
    }
}
