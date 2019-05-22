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
 *  \brief An event used to request a file open by the system (event.drop.*)
 *         This event is enabled by default, you can disable it with SDL_EventState().
 *  \note If this event is enabled, you must free the filename in the event.
 * typedef struct SDL_DropEvent
 * {
 *     Uint32 type;        // ::SDL_DROPFILE
 *     Uint32 timestamp;
 *     char *file;         // The file name, which should be freed with SDL_free()
 * } SDL_DropEvent;
 */
public class SDL_DropEvent extends Structure {
    public static class ByReference extends SDL_DropEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_DropEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public String file;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("type", "timestamp", "file");
    }
}
