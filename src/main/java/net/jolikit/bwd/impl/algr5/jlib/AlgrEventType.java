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
package net.jolikit.bwd.impl.algr5.jlib;

import net.jolikit.bwd.impl.utils.basics.IntValuedHelper;
import net.jolikit.bwd.impl.utils.basics.IntValuedHelper.InterfaceIntValued;

public enum AlgrEventType implements InterfaceIntValued {
     ALLEGRO_EVENT_JOYSTICK_AXIS(1, AlgrEventKind.ALGR_JOYSTICK_EVENT_KIND),
     ALLEGRO_EVENT_JOYSTICK_BUTTON_DOWN(2, AlgrEventKind.ALGR_JOYSTICK_EVENT_KIND),
     ALLEGRO_EVENT_JOYSTICK_BUTTON_UP(3, AlgrEventKind.ALGR_JOYSTICK_EVENT_KIND),
     ALLEGRO_EVENT_JOYSTICK_CONFIGURATION(4, AlgrEventKind.ALGR_JOYSTICK_EVENT_KIND),
     //
     ALLEGRO_EVENT_KEY_DOWN(10, AlgrEventKind.ALGR_KEY_EVENT_KIND),
     ALLEGRO_EVENT_KEY_CHAR(11, AlgrEventKind.ALGR_KEY_EVENT_KIND),
     ALLEGRO_EVENT_KEY_UP(12, AlgrEventKind.ALGR_KEY_EVENT_KIND),
     //
     ALLEGRO_EVENT_MOUSE_AXES(20, AlgrEventKind.ALGR_MOUSE_EVENT_KIND),
     ALLEGRO_EVENT_MOUSE_BUTTON_DOWN(21, AlgrEventKind.ALGR_MOUSE_EVENT_KIND),
     ALLEGRO_EVENT_MOUSE_BUTTON_UP(22, AlgrEventKind.ALGR_MOUSE_EVENT_KIND),
     ALLEGRO_EVENT_MOUSE_ENTER_DISPLAY(23, AlgrEventKind.ALGR_MOUSE_EVENT_KIND),
     ALLEGRO_EVENT_MOUSE_LEAVE_DISPLAY(24, AlgrEventKind.ALGR_MOUSE_EVENT_KIND),
     ALLEGRO_EVENT_MOUSE_WARPED(25, AlgrEventKind.ALGR_MOUSE_EVENT_KIND),
     //
     ALLEGRO_EVENT_TIMER(30, AlgrEventKind.ALGR_TIMER_EVENT_KIND),
     //
     ALLEGRO_EVENT_DISPLAY_EXPOSE(40, AlgrEventKind.ALGR_WINDOW_EVENT_KIND),
     ALLEGRO_EVENT_DISPLAY_RESIZE(41, AlgrEventKind.ALGR_WINDOW_EVENT_KIND),
     ALLEGRO_EVENT_DISPLAY_CLOSE(42, AlgrEventKind.ALGR_WINDOW_EVENT_KIND),
     ALLEGRO_EVENT_DISPLAY_LOST(43, AlgrEventKind.ALGR_WINDOW_EVENT_KIND),
     ALLEGRO_EVENT_DISPLAY_FOUND(44, AlgrEventKind.ALGR_WINDOW_EVENT_KIND),
     ALLEGRO_EVENT_DISPLAY_SWITCH_IN(45, AlgrEventKind.ALGR_WINDOW_EVENT_KIND),
     ALLEGRO_EVENT_DISPLAY_SWITCH_OUT(46, AlgrEventKind.ALGR_WINDOW_EVENT_KIND),
     ALLEGRO_EVENT_DISPLAY_ORIENTATION(47, AlgrEventKind.ALGR_WINDOW_EVENT_KIND),
     ALLEGRO_EVENT_DISPLAY_HALT_DRAWING(48, AlgrEventKind.ALGR_WINDOW_EVENT_KIND),
     ALLEGRO_EVENT_DISPLAY_RESUME_DRAWING(49, AlgrEventKind.ALGR_WINDOW_EVENT_KIND),
     //
     ALLEGRO_EVENT_TOUCH_BEGIN(50, AlgrEventKind.ALGR_TOUCH_EVENT_KIND),
     ALLEGRO_EVENT_TOUCH_END(51, AlgrEventKind.ALGR_TOUCH_EVENT_KIND),
     ALLEGRO_EVENT_TOUCH_MOVE(52, AlgrEventKind.ALGR_TOUCH_EVENT_KIND),
     ALLEGRO_EVENT_TOUCH_CANCEL(53, AlgrEventKind.ALGR_TOUCH_EVENT_KIND),
     //
     ALLEGRO_EVENT_DISPLAY_CONNECTED(60, AlgrEventKind.ALGR_PHYSICAL_DISPLAY_EVENT_KIND),
     ALLEGRO_EVENT_DISPLAY_DISCONNECTED(61, AlgrEventKind.ALGR_PHYSICAL_DISPLAY_EVENT_KIND);
     
     private static final IntValuedHelper<AlgrEventType> HELPER =
             new IntValuedHelper<AlgrEventType>(AlgrEventType.values());

     private final int intValue;
     
     private final AlgrEventKind kind;
     
     private AlgrEventType(int intValue, AlgrEventKind kind) {
         this.intValue = intValue;
         this.kind = kind;
     }
     
     @Override
     public int intValue() {
         return this.intValue;
     }
     
     public AlgrEventKind kind() {
         return this.kind;
     }
     
     /**
      * @param intValue An int value.
      * @return The corresponding instance, or null if none.
      */
     public static AlgrEventType valueOf(int intValue) {
         return HELPER.instanceOf(intValue);
     }
}
