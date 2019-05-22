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
package net.jolikit.bwd.impl.algr5;

import net.jolikit.bwd.impl.algr5.jlib.ALLEGRO_EVENT;

/**
 * Typically useful to listen to system Allegro events,
 * and process them in the host or binding, without creating
 * a cyclic dependency between UI scheduler and host or binding.
 */
public interface InterfaceAlgrEventListener {

    public void onEvent(ALLEGRO_EVENT eventUnion);
}
