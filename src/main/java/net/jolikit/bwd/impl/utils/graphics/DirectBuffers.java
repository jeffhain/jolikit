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
package net.jolikit.bwd.impl.utils.graphics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Utilities to create direct buffers, which are often needed due to
 * backing libraries Java bindings not working with heap buffers.
 * 
 * Provides access to thread-local instances, for direct buffers are not
 * easily garbaged, which would be needed when used as instance-local buffers
 * (host-local or binding-local).
 */
public class DirectBuffers {
    
    /*
     * Using thread-local direct buffers,
     * for they're not easily (!) garbaged.
     */
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Contains lazily initialized thread-local buffers.
     * 
     * Using a single instance for eventually multiple buffers,
     * to keep thread-local instances low.
     */
    private static class MyTlb {
        private IntBuffer directIntBuffer_1024_sq_nativeOrder;
        public IntBuffer getDirectIntBuffer_1024_sq_nativeOrder() {
            IntBuffer buffer = this.directIntBuffer_1024_sq_nativeOrder;
            if (buffer == null) {
                buffer = newDirectIntBuffer_nativeOrder(1024 * 1024);
                this.directIntBuffer_1024_sq_nativeOrder = buffer;
            }
            return buffer;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * Not available in Java 6.
     */
    
    private static final int Float_BYTES = 4;
    private static final int Integer_BYTES = 4;
    
    /*
     * 
     */

    private static final ThreadLocal<MyTlb> TL_BUFFERS = new ThreadLocal<MyTlb>() {
        @Override
        protected MyTlb initialValue() {
            return new MyTlb();
        }
    };
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return A thread-local direct int buffer of 1024*1024 elements,
     *         with native endian order.
     */
    public static IntBuffer getThreadLocalDirectIntBuffer_1024_sq_nativeOrder() {
        return TL_BUFFERS.get().getDirectIntBuffer_1024_sq_nativeOrder();
    }
    
    /*
     * 
     */
    
    public static ByteBuffer newDirectByteBuffer_nativeOrder(int length) {
        final ByteBuffer bb = ByteBuffer.allocateDirect(length);
        bb.order(ByteOrder.nativeOrder());
        return bb;
    }

    public static FloatBuffer newDirectFloatBuffer_nativeOrder(int length) {
        final int byteLength = length * Float_BYTES;
        return newDirectByteBuffer_nativeOrder(byteLength).asFloatBuffer();
    }

    public static IntBuffer newDirectIntBuffer_nativeOrder(int length) {
        final int byteLength = length * Integer_BYTES;
        return newDirectByteBuffer_nativeOrder(byteLength).asIntBuffer();
    }

    /*
     * 
     */
    
    public static FloatBuffer newDirectFloatBuffer_nativeOrder(float[] elements) {
        final FloatBuffer buffer = newDirectFloatBuffer_nativeOrder(elements.length);
        buffer.put(elements);
        buffer.rewind();
        return buffer;
    }

    public static IntBuffer newDirectIntBuffer(int[] elements) {
        final IntBuffer buffer = newDirectIntBuffer_nativeOrder(elements.length);
        buffer.put(elements);
        buffer.rewind();
        return buffer;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private DirectBuffers() {
    }
}
