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
 * \brief Information the version of SDL in use.
 *
 * Represents the library's version as three levels: major revision
 * (increments with massive changes, additions, and enhancements),
 * minor revision (increments with backwards-compatible changes to the
 * major revision), and patchlevel (increments with fixes to the minor
 * revision).
 *
 * typedef struct SDL_version
 * {
 *     Uint8 major;        / **< major version * /
 *     Uint8 minor;        / **< minor version * /
 *     Uint8 patch;        / **< update version * /
 * } SDL_version;
 */
public class SDL_version extends Structure {
    public static class ByReference extends SDL_version implements Structure.ByReference {
    }
    public static class ByValue extends SDL_version implements Structure.ByValue {
    }
    public byte major;
    public byte minor;
    public byte patch;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("major", "minor", "patch");
    }
}
