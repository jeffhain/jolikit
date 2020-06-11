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
package net.jolikit.bwd.api.graphics;

public interface InterfaceBwdImage {
    
    /*
     * Spans.
     */
    
    /**
     * If this image has been disposed, the behavior is undefined.
     * 
     * @return Image width in pixels (>= 0).
     */
    public int getWidth();
    
    /**
     * If this image has been disposed, the behavior is undefined.
     * 
     * @return Image height in pixels (>= 0).
     */
    public int getHeight();
    
    /**
     * If this image has been disposed, the behavior is undefined.
     * 
     * @return A rectangle equal to (0,0,width,height).
     */
    public GRect getRect();
    
    /*
     * Colors.
     */
    
    /**
     * If this image has been disposed, the behavior is undefined.
     * 
     * Note that you might want to use getArgb32At(...) instead
     * if your binding only supports 32 bits ARGB internally,
     * for that would be as accurate and faster.
     * 
     * @param x X in image coordinates.
     * @param y Y in image coordinates.
     * @return The 64 bits ARGB at the specified location.
     * @throws IllegalArgumentException if the specified coordinates
     *         are out of the image.
     */
    public long getArgb64At(int x, int y);
    
    /**
     * If this image has been disposed, the behavior is undefined.
     * 
     * @param x X in image coordinates.
     * @param y Y in image coordinates.
     * @return The 32 bits ARGB at the specified location.
     * @throws IllegalArgumentException if the specified coordinates
     *         are out of the image.
     */
    public int getArgb32At(int x, int y);
    
    /*
     * Disposal.
     */
    
    /**
     * @return Whether this image is disposed, which can be due to a call
     *         to an effective dispose() method on this instance,
     *         or to another reason, such as binding shutdown.
     */
    public boolean isDisposed();

    /**
     * Disposes this image.
     * 
     * Must be idempotent.
     */
    public void dispose();
}
