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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Event structures
 *
 * All event types have the following fields in common.
 *
 *  type      -- the type of event this is
 *  timestamp -- when this event was generated
 *  source    -- which event source generated this event
 *
 * #define _AL_EVENT_HEADER(srctype)                    \
 *    ALLEGRO_EVENT_TYPE type;                          \
 *    srctype *source;                                  \
 *    double timestamp;
 */
public class _AL_EVENT_HEADER extends Structure {
    public static class ByReference extends _AL_EVENT_HEADER implements Structure.ByReference {
    }
    public static class ByValue extends _AL_EVENT_HEADER implements Structure.ByValue {
    }
    public int type;
    public Pointer source;
    public double timestamp;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("type", "source", "timestamp");
    }
    /**
     * For getFieldOrder() methods.
     */
    static List<String> concat(List<String> head, String... tail) {
        final ArrayList<String> list = new ArrayList<String>(head);
        for (String str : tail) {
            list.add(str);
        }
        return Collections.unmodifiableList(list);
    }
}
