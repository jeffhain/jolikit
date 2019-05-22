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
 * BWD binding to QtJambi4 library.
 * 
 * Links:
 * - https://sourceforge.net/projects/qtjambi
 * 
 * TODO qtj Principal known issues or limitations:
 * - Unicode support:
 *   - in key event: no limit.
 *   - in text rendering: seems limited to BMP.
 * - Font metrics are slow, especially on Mac.
 * - On Windows 7, after about 1050 font loads,
 *   Qt outputs the following error on each font load:
 *   "QFontEngine::loadEngine: CreateFontIndirect failed ()"
 *   and a default font is returned instead, with a default size.
 * - On Mac, when maximizing from native window decoration,
 *   it's not the window but the client area that gets screen bounds,
 *   and the window is not flagged as maximized.
 * - When pressing AltGraph key, events for Control and Alt keys
 *   are generated instead (as for other libraries).
 * - On Mac, when setting bounds with huge spans (larger than screen),
 *   height is reduced immediately to fit screen height,
 *   but y position is discarded and even grows a bit very soon
 *   (which are the issue), and x position is zeroed (but width
 *   remains huge as specified).
 */
package net.jolikit.bwd.impl.qtj4;
