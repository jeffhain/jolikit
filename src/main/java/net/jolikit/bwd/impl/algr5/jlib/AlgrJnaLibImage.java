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
package net.jolikit.bwd.impl.algr5.jlib;

import com.sun.jna.Library;
import com.sun.jna.Native;

import net.jolikit.bwd.impl.utils.basics.OsUtils;

/**
 * Java binding for (a subset of) "allegro_image" part of Allegro5.
 */
public interface AlgrJnaLibImage extends Library {

    public static final AlgrJnaLibImage INSTANCE = (AlgrJnaLibImage) (
            (OsUtils.isWindows() ? Native.loadLibrary("allegro_image-5.2", AlgrJnaLibImage.class)
                    : (OsUtils.isMac() ? Native.loadLibrary("allegro_image.5.2.2", AlgrJnaLibImage.class)
                            // Unsupported.
                            : null)));

    //--------------------------------------------------------------------------
    // allegro_image.h
    //--------------------------------------------------------------------------
    
    /**
     * ALLEGRO_IIO_FUNC(bool, al_init_image_addon, (void));
     */
    public boolean al_init_image_addon();

    /**
     * ALLEGRO_IIO_FUNC(void, al_shutdown_image_addon, (void));
     */
    public boolean al_shutdown_image_addon();

    /**
     * ALLEGRO_IIO_FUNC(uint32_t, al_get_allegro_image_version, (void));
     */
    public int al_get_allegro_image_version();
}
