/*
 * Copyright 2019-2024 Jeff Hain
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
package net.jolikit.threading.prl;

/**
 * Simple LIFO for parallelizers.
 */
class ExecPrlzrLifo<T> {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Initially small to ensure a few grows, to make sure that works.
     */
    private Object[] elements = new Object[2];
    
    private int size;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ExecPrlzrLifo() {
    }
    
    public int size() {
        return this.size;
    }
    
    public void addLast(T element) {
        if (this.size == this.elements.length) {
            this.grow();
        }
        this.elements[this.size++] = element;
    }
    
    public T removeLast() {
        final int index = --this.size;
        @SuppressWarnings("unchecked")
        final T res = (T) this.elements[index];
        this.elements[index] = null;
        return res;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void grow() {
        final int previousCapacity = this.elements.length;
        if (previousCapacity == Integer.MAX_VALUE) {
            throw new IllegalStateException("full");
        }
        // Never stagnates, as long as previous capacity is >= 2.
        final int newCapacity = (int) (previousCapacity * 1.5);
        final Object[] newElements = new Object[newCapacity];
        System.arraycopy(this.elements, 0, newElements, 0, this.size);
        this.elements = newElements;
    }
}
