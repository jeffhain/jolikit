/*
 * Copyright 2019-2020 Jeff Hain
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
/**
 * Binding based on SDL2 (tested with 2.0.5).
 * Uses JNA (works with 4.5.0).
 * 
 * Links:
 * - https://www.libsdl.org
 * - https://github.com/java-native-access
 * 
 * TODO sdl Principal known issues or limitations:
 * - Unicode support:
 *   - in key event: limited to [0,255] (SDL_Keysym.sym is a key code,
 *     which we only assume is a code point when in [0,255] and not a key).
 *   - in text rendering: limited to BMP (TTF_GlyphMetrics(...) takes a char).
 * - Memory leak with SDL fonts, as if TTF_CloseFont(...) had no effect.
 * - On Mac, can't get key events to occur.
 * - On Mac, messed-up native decoration top after programmatic resize.
 * - On Mac, if setting a window height larger than available screen height,
 *   window height gets shrunk to fit the screen, but it has no effect
 *   on programmatically retrieved heights, whether from SDL_GetWindowSize(...)
 *   or SDL_GetWindowSurface(...), causing flattening of displayed content.
 *   Resizing width span from decoration (and maybe also other kinds of spans
 *   modifications) fixes the inconsistency, with shrunk height becoming
 *   visible from code.
 * - On Mac, maximization disabled for undecorated windows,
 *   to avoid issues.
 */
package net.jolikit.bwd.impl.sdl2;
