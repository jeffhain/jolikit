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
 * Contains the API for drawing.
 * 
 * @see #InterfaceBwdGraphics
 * 
 * There are no treatments to deal with alpha pre-multiplication
 * in this package, to keep the API simple, and let this topic
 * be an under-the-hood binding implementation hack.
 * 
 * Alpha/opacity: Sticking to the "alpha" term, for simplicity/consistency,
 * unless appropriate, for example for an isOpaque(...) method.
 * 
 * -----------------------------------------------------------------------------
 * Note about integer <-> floating point conversions.
 * 
 * AWT and JavaFX use 255 as divisor and factor to convert between integer
 * values in [0,255] and floating point values in [0,1], with
 * "i = (int) (f * 255 + 0.5)" and "f = i / 255.0",
 * but it causes irregular conversions:
 * [0.0/255, 0.5/255[ (span = 0.5/255) -> 0 -> 0.0
 * [0.5/255, 1.5/255[ (span = 1.0/255) -> 1 -> 1/255.0
 * ...
 * [253.5/255, 254.5/255[ (span = 1.0/255) -> 254 -> 254/255.0
 * [254.5/255, 255.0/255] (span = 0.5/255) -> 255 -> 1.0
 * 
 * Instead, we could use
 * "i = (int) (f * nextDown(256.0))" and "f = i / 255.0",
 * and have about:
 * [0.0/256.0, 1.0/256.0[ (span ~= 1.0/256) -> 0 -> 0.0
 * [1.0/256.0, 2.0/256.0[ (span ~= 1.0/256) -> 1 -> 1/255.0
 * ...
 * [254.0/256.0, 255.0/256.0[ (span ~= 1.0/256) -> 254 -> 254/255.0
 * [255.0/256.0, 256.0/256.0] (span ~= 1.0/256) -> 255 -> 1.0
 * 
 * Though, we don't, and stick to the AWT way for the fp-to-int conversion,
 * because it's how libraries usually do it, and because it's more consistent
 * with the obvious int-to-fp conversion, by end up in the middle of the
 * corresponding float range.
 * -----------------------------------------------------------------------------
 * Note about 8 bits <-> 16 bits components conversions.
 * 
 * For 8 bits to 16 bits conversions, the 8 bits are copied into
 * both the 8 MSBits and the 8 LSBits of the resulting 16 bits value.
 * For example, 0xFF converts to 0xFFFF, not to 0xFF00.
 * 
 * For 16 bits to 8 bits conversions, only the 8 MSBits are used.
 * For example, 0x1289 converts to 0x12.
 * We don't round, such as converting 0x1289 to 0x13,
 * because it would make 8-to-16-to-8 bits conversions not stable
 * (ex.: 0x80 -> 0x8080 -> 0x81).
 * -----------------------------------------------------------------------------
 */
package net.jolikit.bwd.api.graphics;
