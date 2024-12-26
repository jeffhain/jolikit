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
package net.jolikit.bwd.impl.awt;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;

public class BihTestUtils {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return Images of all BufferedImage types
     *         and all (BihPixelFormat,premul) types (which overlap a bit).
     *         For all these images, array has been created by the buffer
     *         (no call to theTrackable.setUntrackable()).
     */
    public static List<BufferedImage> newImageList(int width, int height) {
        final List<BufferedImage> ret = new ArrayList<>();
        
        final Set<Integer> coveredImageTypeSet = new TreeSet<>();
        
        for (BufferedImage image : newImageList_allBihPixelFormat(width, height)) {
            ret.add(image);
            coveredImageTypeSet.add(image.getType());
        }
        
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            // Guards against image types already covered.
            if (coveredImageTypeSet.add(imageType)) {
                final BufferedImage image = new BufferedImage(
                    width,
                    height,
                    imageType);
                ret.add(image);
            }
        }
        
        return ret;
    }
    
    /**
     * @return List of images of all possible (BihPixelFormat,premul) types,
     *         with array created by the buffer
     *         (no call to theTrackable.setUntrackable()).
     */
    public static List<BufferedImage> newImageList_allBihPixelFormat(
        int width,
        int height) {
        final List<BufferedImage> ret = new ArrayList<>();
        
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            for (boolean premul : newPremulArr(pixelFormat)) {
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        width,
                        height,
                        pixelFormat,
                        premul);
                ret.add(image);
            }
        }
        
        return ret;
    }
    
    /**
     * @return Helpers with no redundant capabilities for single pixel methods.
     */
    public static List<BufferedImageHelper> newHelperListForSinglePixel(BufferedImage image) {
        final List<BufferedImageHelper> ret = new ArrayList<>();
        
        boolean didWithNoCmaForSinglePixel = false;
        boolean didWithCmaButNoAduForSinglePixel = false;
        
        for (BufferedImageHelper helper : newHelperList(image)) {
            if (!helper.isColorModelAvoidedForSinglePixelMethods()) {
                if (didWithNoCmaForSinglePixel) {
                    // Redundant.
                    continue;
                }
                didWithNoCmaForSinglePixel = true;
            }
            if (helper.isColorModelAvoidedForSinglePixelMethods()
                && !helper.isArrayDirectlyUsedForSinglePixelMethods()) {
                if (didWithCmaButNoAduForSinglePixel) {
                    // Redundant.
                    continue;
                }
                didWithCmaButNoAduForSinglePixel = true;
            }
            
            ret.add(helper);
        }
        
        return ret;
    }
    
    /**
     * @return A list with all kinds of helpers to test.
     */
    public static List<BufferedImageHelper> newHelperList(BufferedImage image) {
        final List<BufferedImageHelper> ret = new ArrayList<>();
        for (boolean allowColorModelAvoiding : new boolean[] {false, true}) {
            for (boolean allowArrayDirectUse : new boolean[] {false, true}) {
                if (allowArrayDirectUse
                    && (!allowColorModelAvoiding)) {
                    // Equivalent to (false,false).
                    continue;
                }
                
                final BufferedImageHelper helper =
                    new BufferedImageHelper(
                        image,
                        allowColorModelAvoiding,
                        allowArrayDirectUse);
                ret.add(helper);
            }
        }
        return ret;
    }
    
    /**
     * @param imageType Can be null (BihPixelFormat covers all alpha cases,
     *        so null pixel format meant premul not possible).
     */
    public static boolean[] newPremulArr(BihPixelFormat pixelFormat) {
        final boolean withTrue =
            (pixelFormat != null)
            && pixelFormat.hasAlpha();
        return newBooleanArr(withTrue);
    }
    
    public static boolean[] newBooleanArr(boolean withTrue) {
        if (withTrue) {
            return new boolean[] {false, true};
        } else {
            return new boolean[] {false};
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BihTestUtils() {
    }
}
