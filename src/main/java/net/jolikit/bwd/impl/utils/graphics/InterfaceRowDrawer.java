/*
 * Copyright 2021 Jeff Hain
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

/**
 * Interface to draw pixels row by row.
 */
public interface InterfaceRowDrawer {
    
    /**
     * @param rowArr (in) Array containing the row of pixels to draw.
     * @param rowOffset Offset of the row in the specified array.
     * @param dstX X where to start drawing the row.
     * @param dstY Y where to draw the row.
     * @param length Length of the row.
     */
    public void drawRow(
            int[] rowArr,
            int rowOffset,
            //
            int dstX,
            int dstY,
            //
            int length);
}
