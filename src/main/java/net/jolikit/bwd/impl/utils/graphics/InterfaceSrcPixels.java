/*
 * Copyright 2021-2024 Jeff Hain
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
package net.jolikit.bwd.impl.utils.graphics;

import net.jolikit.bwd.api.graphics.GRect;

/**
 * Interface to read pixels.
 */
public interface InterfaceSrcPixels {
    
    public GRect getRect();
    
    public int getWidth();
    
    public int getHeight();
    
    /**
     * Can be null, in which case getColor32At(...) can be used instead.
     */
    public int[] color32Arr();
    
    /**
     * Must use this, and not width, when computing indexes
     * in array returned by color32Arr().
     * If there is no array, must return 0
     * (throwing could be annoying).
     */
    public int getScanlineStride();
    
    /**
     * Useful if color32Arr() returns null.
     * 
     * We prefer specifying (x,y) than just index, because computing
     * index from (x,y) is fast, but not (x,y) from index, due to modulo.
     */
    public int getColor32At(int x, int y);
}
