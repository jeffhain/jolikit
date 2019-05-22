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
 * AWT binding.
 * 
 * TODO awt Principal known issues or limitations:
 * - Unicode support:
 *   - in key event: limited to BMP (KeyEvent.getKeyChar() returns a char).
 *   - in text rendering: limited to BMP.
 * - Cannot create a modal dialog, if one already exists for some window
 *   (looks like a AWT/Swing issue or feature).
 * - When pressing AltGraph key, events for Control and Alt keys
 *   are generated instead (as for other libraries), but we make up
 *   for it by taking key location into account.
 *   Also, when pressing/releasing Alt key and then pressing/releasing AltGraph,
 *   only released events are generated for Control and Alt(Graph) keys,
 *   which we don't try to make up for.
 * - When pressing some modifier keys (like Control, Alt, AltGraph, etc.),
 *   key pressed/released events might not be generated.
 * - In some cases showing doesn't cause focus (for example, on deiconification
 *   if was maximized), or seem to cause focus at random (for example, if two
 *   levels of dialogs, last one having focus, and hiding and showing root host,
 *   much focus events occur, and first level dialog ends up with focus).
 */
package net.jolikit.bwd.impl.awt;
