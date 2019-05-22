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
 *  \brief A video driver dependent system event (event.syswm.*)
 *         This event is disabled by default, you can enable it with SDL_EventState()
 *
 *  \note If you want to use this event, you should include SDL_syswm.h.
 * typedef struct SDL_SysWMEvent
 * {
 *     Uint32 type;        // ::SDL_SYSWMEVENT
 *     Uint32 timestamp;
 *     SDL_SysWMmsg *msg;  // driver dependent data, defined in SDL_syswm.h
 * } SDL_SysWMEvent;
 */
public class SDL_SysWMEvent extends Structure {
    public static class ByReference extends SDL_SysWMEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_SysWMEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public Pointer msg;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("type", "timestamp", "msg");
    }
}
