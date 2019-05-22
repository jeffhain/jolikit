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

/**
 * Not defined in Allegro, but helps factor code.
 */
public enum AlgrEventKind {
    ALGR_JOYSTICK_EVENT_KIND(ALLEGRO_JOYSTICK_EVENT.class),
    ALGR_KEY_EVENT_KIND(ALLEGRO_KEYBOARD_EVENT.class),
    ALGR_MOUSE_EVENT_KIND(ALLEGRO_MOUSE_EVENT.class),
    ALGR_TIMER_EVENT_KIND(ALLEGRO_TIMER_EVENT.class),
    ALGR_WINDOW_EVENT_KIND(ALLEGRO_DISPLAY_EVENT.class),
    ALGR_TOUCH_EVENT_KIND(ALLEGRO_TOUCH_EVENT.class),
    /**
     * TODO algr Not sure what that is. Clarifications are on the roadmap.
     */
    ALGR_PHYSICAL_DISPLAY_EVENT_KIND(ALLEGRO_ANY_EVENT.class),
    //
    ALGR_USER_EVENT_KIND(ALLEGRO_USER_EVENT.class);
    
    private final Class<?> kindClass;
    
    private AlgrEventKind(Class<?> kindClass) {
        this.kindClass = kindClass;
    }
    
    public Class<?> kindClass() {
        return this.kindClass;
    }
}
