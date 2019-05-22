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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.jolikit.lang.RethrowException;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Helpful URL:
 * https://jna.java.net/javadoc/overview-summary.html
 * 
 * NB:
 * - JNA has classes such as IntegerType, that can be extended to define types
 *   with custom byte size, signed or unsigned.
 *   But, for example, when in C there is an uint8, in Java, instead of some
 *   UInt8 class, one can just use byte, which is much easier to deal with,
 *   as long as we take care that 0xFF in C will correspond to -1 in Java, etc.
 *   It's especially useful and safe when values are small enough to always fit
 *   on a signed integer.
 * - JNA and strings:
 *   "String" corresponds to "char*", and "WString" ("Unicode") to "wchar_t*".
 *   For String, the default platform encoding is used, or the one specified
 *   with the jna.encoding system property if it's value ("UTF8" for example).
 *   For WString, values are "copied directly". Since wchar_t is only 16 bits,
 *   the "directly" seem to mean that it cannot work outside of the
 *   Basic Multilingual Plane, and even can only work on a smaller subset
 *   due to surrogates etc., except if the underlying C code uses UTF-16
 *   format as well.
 *   ===> It seems safer to use "char*" signature methods when possible,
 *        along with String, and hoping that the backing library uses
 *        default platform encoding, else specify the proper one with the
 *        jna.encoding system property.
 */
public class AlgrJnaUtils {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * To avoid the boilerplate of equivalent class-specific static utility methods.
     * 
     * @throws NullPointerException if the specified class or pointer is null.
     * @return A new instance of structure of the specified class,
     *         with read() method called.
     */
    public static <S extends Structure> S newAndRead(Class<S> structClass, Pointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        // Implicit null check for structClass.
        final S instance = newInstance(structClass, pointer);
        instance.read();
        return instance;
    }
    
    /*
     * 
     */

    public static AlgrEventType getEventType(ALLEGRO_EVENT event) {

        ALLEGRO_ANY_EVENT.ByValue eventAsAny =
                (ALLEGRO_ANY_EVENT.ByValue) event.getTypedValue(
                        ALLEGRO_ANY_EVENT.ByValue.class);
        
        final AlgrEventType eventType = AlgrEventType.valueOf(eventAsAny.type);
        return eventType;
    }
    
    /*
     * 
     */
    
    public static ALLEGRO_DISPLAY_EVENT.ByValue get_ALLEGRO_DISPLAY_EVENT(ALLEGRO_EVENT eventUnion) {
        return (ALLEGRO_DISPLAY_EVENT.ByValue) eventUnion.getTypedValue(ALLEGRO_DISPLAY_EVENT.ByValue.class);
    }

    public static ALLEGRO_KEYBOARD_EVENT.ByValue get_ALLEGRO_KEYBOARD_EVENT(ALLEGRO_EVENT eventUnion) {
        return (ALLEGRO_KEYBOARD_EVENT.ByValue) eventUnion.getTypedValue(ALLEGRO_KEYBOARD_EVENT.ByValue.class);
    }

    public static ALLEGRO_MOUSE_EVENT.ByValue get_ALLEGRO_MOUSE_EVENT(ALLEGRO_EVENT eventUnion) {
        return (ALLEGRO_MOUSE_EVENT.ByValue) eventUnion.getTypedValue(ALLEGRO_MOUSE_EVENT.ByValue.class);
    }

    public static ALLEGRO_TOUCH_EVENT.ByValue get_ALLEGRO_TOUCH_EVENT(ALLEGRO_EVENT eventUnion) {
        return (ALLEGRO_TOUCH_EVENT.ByValue) eventUnion.getTypedValue(ALLEGRO_TOUCH_EVENT.ByValue.class);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private AlgrJnaUtils() {
    }
    
    private static <T> T newInstance(Class<T> clazz, Object... initArgs) {
        Constructor<T> constructor;
        try {
            constructor = clazz.getConstructor(Pointer.class);
        } catch (NoSuchMethodException e) {
            throw new RethrowException(e);
        } catch (SecurityException e) {
            throw new RethrowException(e);
        }
        try {
            return constructor.newInstance(initArgs);
        } catch (InstantiationException e) {
            throw new RethrowException(e);
        } catch (IllegalAccessException e) {
            throw new RethrowException(e);
        } catch (IllegalArgumentException e) {
            throw new RethrowException(e);
        } catch (InvocationTargetException e) {
            throw new RethrowException(e);
        }
    }
}
