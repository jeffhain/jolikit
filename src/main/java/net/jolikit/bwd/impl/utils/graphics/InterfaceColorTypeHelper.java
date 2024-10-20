/*
 * Copyright 2024 Jeff Hain
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
 * Interface to abstract away whether the color type
 * if alpha-premultiplied or not, and the order
 * and the semantics (alpha or not) of its components.
 * 
 * Conversion methods are named "asXxx" and not "toXxx"
 * when they might not cause any actual conversion.
 */
public interface InterfaceColorTypeHelper {
    
    /**
     * @return True if the color type is alpha-premultiplied,
     *         false otherwise.
     */
    public boolean isPremul();
    
    /**
     * @param color32 The color, corresponding to isPremul().
     * @return The non-premul variant of the specified color
     *         (i.e. the same value is isPremul() is false). 
     */
    public int asNonPremul32FromType(int color32);
    
    /**
     * @param color32 The color, corresponding to isPremul().
     * @return The premul variant of the specified color
     *         (i.e. the same value is isPremul() is true). 
     */
    public int asPremul32FromType(int color32);
    
    /**
     * @param nonPremulColor32 A non-premul variant of the color type.
     * @return The corresponding color in the color type
     *         (i.e. the same value is isPremul() is false). 
     */
    public int asTypeFromNonPremul32(int nonPremulColor32);
    
    /**
     * @param premulColor32 A premul variant of the color type.
     * @return The corresponding color in the color type
     *         (i.e. the same value is isPremul() is true). 
     */
    public int asTypeFromPremul32(int premulColor32);
    
    /**
     * Meant to use something like SAT macro in JDK's TransformHelper.c,
     * to take components back into proper ranges.
     * 
     * @param a8 First premul component, possibly outside [0,255] range.
     * @param b8 Second premul component, possibly outside [0,255] range.
     * @param c8 Third premul component, possibly outside [0,255] range.
     * @param d8 Fourth premul component, possibly outside [0,255] range.
     * @return Corresponding valid premul value, i.e. with non-alpha components
     *         within [0,alpha8] range.
     */
    public int toValidPremul32(int a8, int b8, int c8, int d8);
}
