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

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Structure;

/**
 * typedef struct ALLEGRO_TOUCH_INPUT ALLEGRO_TOUCH_INPUT;
 */
public class ALLEGRO_TOUCH_INPUT extends Structure {
    public static class ByReference extends ALLEGRO_TOUCH_INPUT implements Structure.ByReference {
    }
    public static class ByValue extends ALLEGRO_TOUCH_INPUT implements Structure.ByValue {
    }
    @Override
    protected List<String> getFieldOrder() {
        return new ArrayList<String>();
    }
}
