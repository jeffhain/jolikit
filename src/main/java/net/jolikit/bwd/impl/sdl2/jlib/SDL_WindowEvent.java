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
 * \brief Window state change event data (event.window.*)
 *  
 * typedef struct SDL_WindowEvent
 * {
 *     Uint32 type;        // ::SDL_WINDOWEVENT
 *     Uint32 timestamp;
 *     Uint32 windowID;    // The associated window
 *     Uint8 event;        // ::SDL_WindowEventID
 *     Uint8 padding1;
 *     Uint8 padding2;
 *     Uint8 padding3;
 *     Sint32 data1;       // event dependent data
 *     Sint32 data2;       // event dependent data
 * } SDL_WindowEvent;
 */
public class SDL_WindowEvent extends Structure {
    public static class ByReference extends SDL_WindowEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_WindowEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public int windowID;
    public byte event;
    public byte padding1;
    public byte padding2;
    public byte padding3;
    public int data1;
    public int data2;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "type", "timestamp", "windowID",
                "event", "padding1", "padding2", "padding3",
                "data1", "data2");
    }
}
