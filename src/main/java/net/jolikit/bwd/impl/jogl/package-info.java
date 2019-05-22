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
 * BWD binding to JOGL library, using NEWT windowing
 * (since we already use AWT/Swing/SWT in specific bindings).
 * 
 * Links:
 * - https://jogamp.org
 * 
 * TODO jogl Using "-Djogl.1thread=true" doesn't force single threading,
 * but merely enables it if it's available. There is no way to force single
 * threading. For me it's always false (Threading.getMode() = Mode.MT).
 * Also, using "-Djogl.1thread=awt" causes the following exception
 * if GLProfile.isAWTAvailable() is false:
 * "java.lang.RuntimeException: Unsupported value for property jogl.1thread:
 * awt, should be [true/auto, worker, awt or false]"
 * 
 * TODO jogl Principal known issues or limitations:
 * - Unicode support:
 *   - in key event: limited to BMP (KeyEvent.getKeyChar() returns a char).
 *   - in text rendering: seems limited to BMP (since we use AWT).
 * - Key typed events: limited to characters in Unicode BMP.
 * - When pressing AltGraph key, events for Control and Alt keys
 *   are generated instead (as for other libraries), but then
 *   we replace Alt with AltGraph.
 * - On Mac, iconified windows are typically still visible,
 *   just weirdly moved and/or shrinked.
 * - For some reason, transparency doesn't work with NEWT:
 *   the background is always opaque (but it does work
 *   with all other jogl windowings).
 * - Sometimes, after growing a bit a large (1000*1000) window,
 *   everything ends up properly done and sized in logs,
 *   but the rendering into OpenGL only occupies the old spans
 *   (bottom-left cornered) of the client area (content appearing shrinked).
 *   Can easily happen with maximization/demaximization.
 * - Not supporting "GLAutoDrawable Reconfiguration", to keep things simple
 *   ("One use case is where a window is being dragged to another screen with
 *   a different pixel configuration, ie GLCapabilities. The implementation
 *   shall be able to detect such cases in conjunction with the associated
 *   NativeSurface.").
 */
package net.jolikit.bwd.impl.jogl;
