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
package net.jolikit.bwd.impl.utils.drag;

/**
 * Interface for object drag controllers.
 * 
 * A lower-level API would be to provide initial and current mouse (x,y)
 * coordinates, but it would be less user-friendly than directly providing the
 * desired location for the dragged object.
 */
public interface InterfaceDragController {

    /**
     * @return X coordinate, in client, dragged object should be moved to if possible.
     *         Can be any value (not necessarily >= 0).
     */
    public int getDesiredX();
    
    /**
     * @return Y coordinate, in client, dragged object should be moved to if possible.
     *         Can be any value (not necessarily >= 0).
     */
    public int getDesiredY();
}
