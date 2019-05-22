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
package net.jolikit.bwd.impl.utils.basics;

import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility to call a method if it exists in current JDK,
 * or best effort methods or No-Op otherwise.
 */
public class BindingCallIfExistsUtils {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /*
     * 
     */
    
    private static final ThreadLocal<Object[]> tlObjectArr1 = new ThreadLocal<Object[]>() {
        @Override
        public Object[] initialValue() {
            return new Object[1];
        }
    };
    
    /*
     * 
     */
    
    private static final Method Window_setOpacity_float = getMethodIfExists(
            Window.class,
            "setOpacity",
            float.class);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Calls instance.setOpacity(float) if it exists,
     * else does nothing.
     */
    public static void setOpacityElseNoOp(Window instance, float opacity) {
        if (Window_setOpacity_float != null) {
            try {
                final Object[] objectArr1 = tlObjectArr1.get();
                objectArr1[0] = opacity;
                Window_setOpacity_float.invoke(instance, objectArr1);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            /*
             * No-Op.
             */
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BindingCallIfExistsUtils() {
    }
    
    private static Method getMethodIfExists(
            Class<?> instanceClass,
            String name,
            Class<?>... parameterTypes) {
        Method method = null;
        try {
            method = instanceClass.getMethod(name, parameterTypes);
        } catch (SecurityException e) {
            // quiet
        } catch (NoSuchMethodException e) {
            // quiet
        }
        return method;
    }
}
