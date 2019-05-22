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
package net.jolikit.bwd.impl.lwjgl3;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jolikit.bwd.api.BwdCursors;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;
import net.jolikit.bwd.impl.utils.cursor.FallbackCursors;
import net.jolikit.bwd.impl.utils.graphics.DirectBuffers;
import net.jolikit.lang.LangUtils;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;

public class LwjglCursorRepository {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int BYTES_PER_PIXEL = 4;
    
    private final boolean mustUseSystemCursorsWhenAvailable;
    
    private final SortedMap<Integer,Long> backingCursorByCursor =
            new TreeMap<Integer,Long>();
    
    private final IdentityHashMap<Long,Integer> cursorByBackingCursor =
            new IdentityHashMap<Long,Integer>();
    
    /*
     * temps
     */
    
    /**
     * Grown if too small.
     * 
     * Needs to be direct, else we get that:
     * Exception in thread "main" java.lang.NullPointerException
     *     at org.lwjgl.system.Checks.check(Checks.java:96)
     *     at org.lwjgl.glfw.GLFWImage.validate(GLFWImage.java:274)
     *     at org.lwjgl.glfw.GLFW.nglfwCreateCursor(GLFW.java:2904)
     *     at org.lwjgl.glfw.GLFW.glfwCreateCursor(GLFW.java:2935)
     */
    private ByteBuffer tmpPixels =
            DirectBuffers.newDirectByteBuffer_nativeOrder(
                    32 * 32 * BYTES_PER_PIXEL);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public LwjglCursorRepository(boolean mustUseSystemCursorsWhenAvailable) {
        this.mustUseSystemCursorsWhenAvailable = mustUseSystemCursorsWhenAvailable;
    }
    
    public void init() {
        // Cleanup in case of multiple init calls.
        this.close();
        
        for (int cursor : BwdCursors.cursorList()) {
            final Long backingCursor = newBackingCursor(cursor);
            if (backingCursor != null) {
                this.backingCursorByCursor.put(cursor, backingCursor);
                this.cursorByBackingCursor.put(backingCursor, cursor);
            }
        }
    }
    
    public void close() {
        for (Long backingCursor : this.cursorByBackingCursor.keySet()) {
            GLFW.glfwDestroyCursor(backingCursor.longValue());
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
    public int convertToCursor(Long backingCursor) {
        if (backingCursor == null) {
            return BwdCursors.INVISIBLE;
        }
        final Integer cursorRef = this.cursorByBackingCursor.get(backingCursor);
        if (cursorRef == null) {
            throw new IllegalArgumentException(
                    "backing cursor is not from this insance : " + backingCursor);
        }
        return cursorRef.intValue();
    }
    
    /**
     * @param cursor A cursor.
     * @return The corresponding backing cursor, possibly null.
     */
    public Long convertToBackingCursor(int cursor) {
        return this.backingCursorByCursor.get(cursor);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return The corresponding backing cursor, possibly null.
     */
    private Long newBackingCursor(int cursor) {
        if (cursor == BwdCursors.INVISIBLE) {
            return null;
        }
        
        /*
         * System cursors.
         */
        
        if (this.mustUseSystemCursorsWhenAvailable) {
            switch (cursor) {
            case BwdCursors.ARROW: return GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
            case BwdCursors.CROSSHAIR: return GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR);
            case BwdCursors.IBEAM_TEXT: return GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
            case BwdCursors.WAIT: break;
            case BwdCursors.RESIZE_NESW: break;
            case BwdCursors.RESIZE_NWSE: break;
            case BwdCursors.RESIZE_NS: return GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
            case BwdCursors.RESIZE_WE: return GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
            case BwdCursors.HAND: return GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
            case BwdCursors.MOVE: break;
            default:
                throw new AssertionError("" + cursor);
            }
        }
        
        /*
         * Fallback cursors.
         */
        
        if (!FallbackCursors.isAvailable(cursor)) {
            return null;
        }
        
        final int width = FallbackCursors.width(cursor);
        final int height = FallbackCursors.height(cursor);
        final int hotX = FallbackCursors.hotX(cursor);
        final int hotY = FallbackCursors.hotY(cursor);
        final int[] argb32Arr = FallbackCursors.argb32Arr(cursor);
        
        final int usedByteSize = width * height * BYTES_PER_PIXEL;
        final ByteBuffer pixels = this.getClearedTmpByteBuffer(usedByteSize);
        /*
         * TODO lwjgl glfwCreateCursor(...) spec says:
         * "The pixels are 32-bit, little-endian, non-premultiplied RGBA".
         * The little-endian is maybe just accidental,
         * due to most hardware being little endian.
         * We use native order instead (i.e. little almost always),
         * because OpenGL expects RGBA in native order, and we think
         * the "little-endian" remark actually comes from that.
         */
        pixels.order(BindingBasicsUtils.NATIVE_ORDER);
        
        putArgb32Arr(argb32Arr, pixels);
        
        pixels.flip();
        
        final GLFWImage cursorImage= GLFWImage.create();
        cursorImage.width(width);
        cursorImage.height(height);
        cursorImage.pixels(pixels);
        
        final long backingCursor = GLFW.glfwCreateCursor(cursorImage, hotX , hotY);
        
        /*
         * glfwCreateCursor(...) spec says:
         * "The specified image data is copied before this function returns.",
         * so we can delete it here.
         */
        cursorImage.clear();
        
        return backingCursor;
    }
    
    private static void putArgb32Arr(int[] argb32Arr, ByteBuffer pixels) {
        for (int i = 0; i < argb32Arr.length; i++) {
            putArgb32At(i, argb32Arr[i], pixels);
        }
    }
    
    private static void putArgb32At(int pixelIndex, int argb32, ByteBuffer pixels) {
        /*
         * TODO lwjgl glfwCreateCursor(...) spec says:
         * "The pixels are 32-bit, little-endian, non-premultiplied RGBA".
         * Here the value is premultiplied, but it doesn't hurt since we only use
         * opaque colors for cursors (except 0x00000000 where nothing is to be drawn,
         * but it's the same value whether premultiplied or not).
         */
        final int color32 = LwjglPaintHelper.getArrayColor32FromArgb32(argb32);
        pixels.putInt(pixelIndex * BYTES_PER_PIXEL, color32);
    }
    
    private ByteBuffer getClearedTmpByteBuffer(int minCapacity) {
        if (this.tmpPixels.capacity() < minCapacity) {
            final int newCapacity = LangUtils.increasedArrayLength(
                    this.tmpPixels.capacity(), minCapacity);
            this.tmpPixels = DirectBuffers.newDirectByteBuffer_nativeOrder(newCapacity);
        }
        this.tmpPixels.clear();
        return this.tmpPixels;
    }
}
