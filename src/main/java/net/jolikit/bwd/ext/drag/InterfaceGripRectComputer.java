/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.ext.drag;

import net.jolikit.bwd.api.graphics.GRect;

/**
 * Interface for drag controllers that drag client bounds around.
 */
public interface InterfaceGripRectComputer {

    /**
     * Mainly useful for the position,
     * but for the sake of completeness and locality
     * we have it compute the whole bounds.
     */
    public GRect computeGripRectInClientBox(
            GRect clientBox,
            GripType gripType);
}
