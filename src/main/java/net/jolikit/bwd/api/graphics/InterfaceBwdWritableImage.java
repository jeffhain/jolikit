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
package net.jolikit.bwd.api.graphics;

/**
 * An image that can be drawn using a graphics.
 */
public interface InterfaceBwdWritableImage extends InterfaceBwdImage {
    
    /**
     * Must be already initialized (init() called on image creation).
     * 
     * User can call finish() on it to "freeze" image content
     * (accidental feature of having init()/finish() in our graphics API).
     * 
     * Image's dispose() method must call finish() on its graphics.
     * 
     * @return Image's graphics.
     */
    public InterfaceBwdGraphics getGraphics();
}
