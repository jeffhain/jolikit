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
package net.jolikit.bwd.impl.jogl;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.cursor.FallbackCursors;
import net.jolikit.lang.LangUtils;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.nativewindow.util.PixelRectangle;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;

public class JoglCursorRepository {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------

    private static class MyDimIm implements DimensionImmutable {
        final int width;
        final int height;
        public MyDimIm(int width, int height) {
            this.width = width;
            this.height = height;
        }
        @Override
        public Object cloneMutable() {
            return new Dimension(this.width, this.height);
        }
        @Override
        public int getWidth() {
            return this.width;
        }
        @Override
        public int getHeight() {
            return this.height;
        }
        @Override
        public int compareTo(DimensionImmutable other) {
            {
                final int cmp = this.getHeight() - other.getHeight();
                if (cmp != 0) {
                    return cmp;
                }
            }
            {
                final int cmp = this.getWidth() - other.getWidth();
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        }
    }
    
    private static class MyPixelRectangle implements PixelRectangle {
        final ByteBuffer pixels;
        final MyDimIm dim;
        public MyPixelRectangle(
                int width,
                int height,
                int[] argb32Arr) {
            
            final ByteBuffer pixels = ByteBuffer.allocate(
                    width * height * BYTES_PER_PIXEL);
            pixels.order(BindingBasicsUtils.NATIVE_ORDER);
            this.pixels = pixels;
            
            this.dim = new MyDimIm(width, height);

            putArgb32Arr(argb32Arr, pixels);
        }
        @Override
        public boolean isGLOriented() {
            return false;
        }
        @Override
        public int getStride() {
            return this.dim.width * 4;
        }
        @Override
        public DimensionImmutable getSize() {
            return this.dim;
        }
        @Override
        public ByteBuffer getPixels() {
            return this.pixels;
        }
        @Override
        public PixelFormat getPixelformat() {
            return PIXEL_FORMAT;
        }
    };

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int BYTES_PER_PIXEL = 4;
    
    /**
     * OpenGL expects native RGBA.
     */
    private static final PixelFormat PIXEL_FORMAT = PixelFormat.RGBA8888;

    private final Display display;
    
    private final SortedMap<Integer,PointerIcon> backingCursorByCursor =
            new TreeMap<Integer,PointerIcon>();
    
    private final IdentityHashMap<PointerIcon,Integer> cursorByBackingCursor =
            new IdentityHashMap<PointerIcon,Integer>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public JoglCursorRepository(Display display) {
        this.display = LangUtils.requireNonNull(display);
    }
    
    public void init() {
        // Cleanup in case of multiple init calls.
        this.close();
        
        for (int cursor : BwdCursors.cursorList()) {
            final PointerIcon backingCursor = newBackingCursor(cursor);
            if (backingCursor != null) {
                this.backingCursorByCursor.put(cursor, backingCursor);
                this.cursorByBackingCursor.put(backingCursor, cursor);
            }
        }
    }
    
    public void close() {
        for (PointerIcon backingCursor : this.cursorByBackingCursor.keySet()) {
            backingCursor.destroy();
        }
        this.backingCursorByCursor.clear();
        this.cursorByBackingCursor.clear();
    }
    
    /**
     * @param backingCursor A backing cursor returned by the convert(cursor)
     *        method of this instance, or null.
     * @return The corresponding cursor.
     * @throws IllegalArgumentException if the specified backing cursor
     *         is not null and does not come from this instance.
     */
    public int convertToCursor(PointerIcon backingCursor) {
        if (backingCursor == null) {
            return BwdCursors.INVISIBLE;
        }
        final Integer cursorRef = this.cursorByBackingCursor.get(backingCursor);
        if (cursorRef == null) {
            throw new IllegalArgumentException("backing cursor is not from this insance : " + backingCursor);
        }
        return cursorRef.intValue();
    }
    
    /**
     * @param cursor A cursor.
     * @return The corresponding backing cursor, possibly null.
     */
    public PointerIcon convertToBackingCursor(int cursor) {
        return this.backingCursorByCursor.get(cursor);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return The corresponding backing cursor, possibly null.
     */
    private PointerIcon newBackingCursor(int cursor) {
        if (cursor == BwdCursors.INVISIBLE) {
            return null;
        }
        
        if (!FallbackCursors.isAvailable(cursor)) {
            return null;
        }
        
        final int width = FallbackCursors.width(cursor);
        final int height = FallbackCursors.height(cursor);
        final int hotX = FallbackCursors.hotX(cursor);
        final int hotY = FallbackCursors.hotY(cursor);
        final int[] argb32Arr = FallbackCursors.argb32Arr(cursor);
        
        final MyPixelRectangle pixelRectangle = new MyPixelRectangle(
                width,
                height,
                argb32Arr);
        final PointerIcon backingCursor;
        try {
            backingCursor = this.display.createPointerIcon(
                    pixelRectangle,
                    hotX,
                    hotY);
        } catch (IllegalStateException e) {
            /*
             * TODO jogl If display is not valid yet, can have:
             * java.lang.IllegalStateException: Display.createPointerIcon: Display invalid
             *  NEWT-Display[.windows_nil-1, excl false, refCount 0, hasEDT true, edtRunning true, null]
             *     at jogamp.newt.DisplayImpl$3.run(DisplayImpl.java:207)
             *     at jogamp.newt.DisplayImpl.runOnEDTIfAvail(DisplayImpl.java:450)
             *     at jogamp.newt.DisplayImpl.createPointerIcon(DisplayImpl.java:203)
             */
            throw new BindingError("could not create backing cursor (exception) for " + cursor, e);
        }
        if (backingCursor == null) {
            throw new BindingError("could not create backing cursor (null) for " + cursor);
        }
        if (!backingCursor.validate()) {
            throw new BindingError("could not create valid backing cursor for " + cursor);
        }
        return backingCursor;
    }
    
    private static void putArgb32Arr(int[] argb32Arr, ByteBuffer pixels) {
        for (int i = 0; i < argb32Arr.length; i++) {
            putArgb32At(i, argb32Arr[i], pixels);
        }
    }
    
    private static void putArgb32At(int pixelIndex, int argb32, ByteBuffer pixels) {
        final int color32 = JoglPaintHelper.getArrayColor32FromArgb32(argb32);
        pixels.putInt(pixelIndex * BYTES_PER_PIXEL, color32);
    }
}
