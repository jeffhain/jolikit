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
package net.jolikit.bwd.impl.jfx;

import java.nio.IntBuffer;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;

public class JfxPaintUtils {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    public static final boolean MUST_PRESERVE_OB_CONTENT_ON_RESIZE = true;

    public static final boolean ALLOW_OB_SHRINKING = true;
    
    /**
     * Can reuse a same backing image internally for this instance,
     * because we only draw the "same" thing (the client) into it,
     * and updating it in UI thread should not cause concurrency
     * issues with rendering (that would be terrible, and specs
     * don't warn about it, so we assume it doesn't happen).
     */
    public static final boolean MUST_REUSE_IMG_FOR_HOST_UTILS = true;
    
    /**
     * Can't reuse a same backing image internally for this instance,
     * because it can be used to draw different things,
     * and JavaFX's drawImage() methods are asynchronous
     * in such a way that if the specified image is modified
     * after the call the new content might be rendered instead.
     */
    public static final boolean MUST_REUSE_IMG_FOR_GRAPHICS_UTILS = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * We use this one when possible.
     */
    public static final WritablePixelFormat<IntBuffer> FORMAT_INT_ARGB_PRE =
        PixelFormat.getIntArgbPreInstance();

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private JfxPaintUtils() {
    }
}
