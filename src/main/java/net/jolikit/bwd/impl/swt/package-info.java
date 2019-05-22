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
 * SWT binding.
 * 
 * Links:
 * - https://www.eclipse.org/swt
 * 
 * TODO swt Principal known issues or limitations:
 * - Unicode support:
 *   - in key event: limited to BMP (Event.character is a char).
 *   - in text rendering: limited to BMP (GC.getCharWidth(...) takes
 *     a char as argument).
 * - Dirty painting support:
 *   - On Mac, need to repaint everything at each painting.
 * - No way to tell if a font has a glyph for a code point,
 *   so we have to use FontBox for that.
 * - Font metrics are slow, especially on Mac.
 * - On Windows, undecorated windows actually have some insets,
 *   with a black border.
 * - On Mac, can't load user fonts.
 * - On Mac, maximizing a window can have unexpected side effects
 *   on other windows, so we don't always allow for it.
 */
package net.jolikit.bwd.impl.swt;
