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
 * Package for BWD binding with Allegro 5.
 * Uses JNA (works with 4.5.0).
 * 
 * Links:
 * - https://liballeg.org/a5docs/5.2.0/index.html
 * - https://github.com/liballeg/allegro5
 * - https://github.com/java-native-access
 * 
 * This binding uses the monolithic library ("allegro_monolith-5.2"),
 * but to optimize size it would be possible to split the JNA binding
 * across a few sub-libraries and then only depend on them.
 * 
 * TODO algr Principal known issues:
 * - Unicode support:
 *   - in key event: no limit (ALLEGRO_KEYBOARD_EVENT.unichar is an int).
 *   - in text rendering: seems limited to BMP.
 * - Dirty painting support:
 *   - On Mac, need to repaint everything at each painting.
 * - Client area content is erased if moving another window
 *   on top of our host (same for SDL).
 * - al_draw_text(...) can be very slow, so badly that it causes very visible
 *   slow down of test cases involving multiple lines of text.
 * - On Windows, JVM can crash or freeze if closing a window
 *   just after having created a dialog on it.
 * - On Mac, issues with text/fonts:
 *   - Can't get any text to be rendered,
 *     even though programmatically things look good
 *     (other than unmodified pixels).
 *   - Crashes sometimes when dealing with fonts, like involving:
 *     "com.sun.proxy.$Proxy2.al_destroy_font(Lnet/jolikit/bwd/impl/algr5/jlib/ALLEGRO_FONT;)V+16"
 * - On Mac, setting bounds on a window can have side-effect
 *   on bounds on its dialogs.
 * - On Mac, we don't allow maximization for undecorated windows or dialogs,
 *   for it can either not work well in itself or have bad side effects.
 * - On Mac, issues with undecorated hosts:
 *   - Sometimes not showing, even though programmatically
 *     things look good.
 *   - Can cause crashes, or the error log:
 *     "2019-03-28 21:32:54.150 java[710:21778] *** Assertion failure in -[ALWindow _changeJustMain],
 *     /Library/Caches/com.apple.xbs/Sources/AppKit/AppKit-1504.83.101/AppKit.subproj/NSWindow.m:14861"
 */
package net.jolikit.bwd.impl.algr5;
