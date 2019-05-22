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
 *  \brief The "quit requested" event
 * typedef struct SDL_QuitEvent
 * {
 *     Uint32 type;        // ::SDL_QUIT
 *     Uint32 timestamp;
 * } SDL_QuitEvent;
 * 
 * An ::SDL_QUIT event is generated when the user tries to close the application
 * window.  If it is ignored or filtered out, the window will remain open.
 * If it is not ignored or filtered, it is queued normally and the window
 * is allowed to close.  When the window is closed, screen updates will
 * complete, but have no effect.
 *
 * SDL_Init() installs signal handlers for SIGINT (keyboard interrupt)
 * and SIGTERM (system termination request), if handlers do not already
 * exist, that generate ::SDL_QUIT events as well.  There is no way
 * to determine the cause of an ::SDL_QUIT event, but setting a signal
 * handler in your application will override the default generation of
 * quit events for that signal.
 */
public class SDL_QuitEvent extends Structure {
    public static class ByReference extends SDL_QuitEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_QuitEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("type", "timestamp");
    }
}
