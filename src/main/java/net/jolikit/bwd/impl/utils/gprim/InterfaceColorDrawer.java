/*
 * Copyright 2020 Jeff Hain
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
package net.jolikit.bwd.impl.utils.gprim;

public interface InterfaceColorDrawer {

    /**
     * To know whether drawing a same point multiple times
     * can be done without ending up with an overly bold color,
     * which can allow for simpler and faster drawing algorithms.
     * 
     * @return Whether the color used for drawing is opaque.
     */
    public boolean isColorOpaque();
}
