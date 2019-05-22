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
/**
 * BWD binding to LWJGL3 library, using AWT for fonts.
 * 
 * Links:
 * - https://github.com/LWJGL/lwjgl3
 * 
 * TODO lwjgl Principal known issues or limitations:
 * - Unicode support:
 *   - in key event: seems no limit (LwjglChar(Mods)Event.codepoint is an int).
 *   - in text rendering: seems limited to BMP (since we use AWT).
 * - 1-bit BMPs are not supported.
 * - On Mac, can't get key events to occur.
 * - On Mac, not allowing maximization for undecorated windows,
 *   to avoid issues.
 * - On Mac, can't have another cursor than default one.
 */
package net.jolikit.bwd.impl.lwjgl3;
