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
package net.jolikit.bwd.impl.utils.graphics;

import junit.framework.TestCase;

public class IntArrayGraphicBufferTest extends TestCase {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_IntArrayGraphicBuffer_false() {
        final boolean mustCopyOnStorageResize = false;
        test_IntArrayGraphicBuffer_boolean(mustCopyOnStorageResize);
    }
    
    public void test_IntArrayGraphicBuffer_true() {
        final boolean mustCopyOnStorageResize = true;
        test_IntArrayGraphicBuffer_boolean(mustCopyOnStorageResize);
    }
    
    public void test_IntArrayGraphicBuffer_boolean(boolean mustCopyOnStorageResize) {
        final boolean allowShrinking = false;
        final IntArrayGraphicBuffer buffer = new IntArrayGraphicBuffer(
                mustCopyOnStorageResize,
                allowShrinking);
        
        final int n = 10;
        buffer.setSize(n, n);
        
        final int[] oldArr = buffer.getPixelArr();
        for (int j = 0; j < n; j++) {
            final int lineOffset = j * buffer.getScanlineStride();
            for (int i = 0; i < n; i++) {
                final int index = lineOffset + i;
                final int pos = j * n + i;
                oldArr[index] = pos;
            }
        }

        // Big growth, to cause array growth.
        final int m = n * 10;
        buffer.setSize(m, m);

        final int[] newArr = buffer.getPixelArr();
        if (newArr == oldArr) {
            throw new AssertionError();
        }
        for (int j = 0; j < n; j++) {
            final int lineOffset = j * buffer.getScanlineStride();
            for (int i = 0; i < n; i++) {
                final int index = lineOffset + i;
                final int pixel = newArr[index];
                if (mustCopyOnStorageResize) {
                    final int pos = j * n + i;
                    assertEquals(pos, pixel);
                } else {
                    assertEquals(0, pixel);
                }
            }
        }
    }
}
