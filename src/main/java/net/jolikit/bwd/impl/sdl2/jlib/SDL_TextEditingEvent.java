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
 *  \brief Keyboard text editing event structure (event.edit.*)
 * typedef struct SDL_TextEditingEvent
 * {
 *     Uint32 type;                                // ::SDL_TEXTEDITING
 *     Uint32 timestamp;
 *     Uint32 windowID;                            // The window with keyboard focus, if any
 *     char text[SDL_TEXTEDITINGEVENT_TEXT_SIZE];  // The editing text
 *     Sint32 start;                               // The start cursor of selected editing text
 *     Sint32 length;                              // The length of selected editing text
 * } SDL_TextEditingEvent;
 */
public class SDL_TextEditingEvent extends Structure {
    public static class ByReference extends SDL_TextEditingEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_TextEditingEvent implements Structure.ByValue {
    }
    public static final int SDL_TEXTEDITINGEVENT_TEXT_SIZE = 32;
    public int type;
    public int timestamp;
    public int windowID;
    /**
     * byte, not char, to make sure we don't leak.
     */
    public byte[] text = new byte[SDL_TEXTEDITINGEVENT_TEXT_SIZE];
    public int start;
    public int length;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "type", "timestamp", "windowID",
                "text", "start", "length");
    }
}
