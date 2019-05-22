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

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *  \brief A user-defined event type (event.user.*)
 * typedef struct SDL_UserEvent
 * {
 *     Uint32 type;        // ::SDL_USEREVENT through ::SDL_LASTEVENT-1
 *     Uint32 timestamp;
 *     Uint32 windowID;    // The associated window if any
 *     Sint32 code;        // User defined event code
 *     void *data1;        // User defined data pointer
 *     void *data2;        // User defined data pointer
 * } SDL_UserEvent;
 */
public class SDL_UserEvent extends Structure {
    public static class ByReference extends SDL_UserEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_UserEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public int windowID;
    public int code;
    public Pointer data1;
    public Pointer data2;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("type", "timestamp", "windowID", "code", "data1", "data2");
    }
}
