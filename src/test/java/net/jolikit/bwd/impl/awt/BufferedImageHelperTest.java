/*
 * Copyright 2024-2025 Jeff Hain
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

import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.test.utils.TestUtils;

public class BufferedImageHelperTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private enum MyNewImageMethodType {
        WITH_IMAGE_TYPE,
        WITH_PIXEL_FORMAT,
        WITH_CPT_INDEXES,
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final int SMALL_WIDTH = 7;
    private static final int SMALL_HEIGHT = 5;
    
    private static final List<Integer> TYPE_INT_XXX_LIST =
        Collections.unmodifiableList(
            Arrays.asList(
                BufferedImage.TYPE_INT_ARGB,
                BufferedImage.TYPE_INT_ARGB_PRE,
                BufferedImage.TYPE_INT_RGB,
                BufferedImage.TYPE_INT_BGR));
    
    /**
     * Includes TYPE_CUSTOM.
     */
    private static final List<Integer> TYPE_XXX_LIST;
    static {
        List<Integer> list = new ArrayList<>();
        list.add(BufferedImage.TYPE_CUSTOM);
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            list.add(imageTypeEnum.imageType());
        }
        TYPE_XXX_LIST = Collections.unmodifiableList(list);
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public BufferedImageHelperTest() {
    }
    
    /*
     * 
     */
    
    public void test_BufferedImageHelper_BufferedImage() {
        for (BufferedImage image : newImageList()) {
            final BufferedImageHelper helper = new BufferedImageHelper(image);
            // Both true by default.
            assertTrue(helper.isColorModelAvoidingAllowed());
            assertTrue(helper.isArrayDirectUseAllowed());
        }
    }
    
    public void test_BufferedImageHelper_BufferedImage_2boolean() {
        for (BufferedImage image : newImageList()) {
            final int imageType = image.getType();
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            
            final boolean isImageTypeSinglePixelCmaCompatible =
                (pixelFormat != null)
                || (imageType == BufferedImage.TYPE_USHORT_555_RGB)
                || (imageType == BufferedImage.TYPE_USHORT_565_RGB)
                || (imageType == BufferedImage.TYPE_USHORT_GRAY)
                || (imageType == BufferedImage.TYPE_BYTE_GRAY);
            
            final boolean isImageTypeSinglePixelAduCompatible =
                isImageTypeSinglePixelCmaCompatible;
            
            for (boolean allowColorModelAvoiding : new boolean[] {false, true}) {
                for (boolean allowArrayDirectUse : new boolean[] {false, true}) {
                    final BufferedImageHelper helper = new BufferedImageHelper(
                        image,
                        allowColorModelAvoiding,
                        allowArrayDirectUse);
                    
                    assertEquals(
                        allowColorModelAvoiding,
                        helper.isColorModelAvoidingAllowed());
                    assertEquals(
                        allowColorModelAvoiding
                        && allowArrayDirectUse,
                        helper.isArrayDirectUseAllowed());
                    
                    // For all supported image types,
                    // we have scalar pixels,
                    // so color model is actually avoided
                    // for single pixel methods.
                    final boolean expectedSinglePixelCma =
                        allowColorModelAvoiding
                        && isImageTypeSinglePixelCmaCompatible;
                    assertEquals(
                        expectedSinglePixelCma,
                        helper.isColorModelAvoidedForSinglePixelMethods());
                    
                    final boolean expectedSinglePixelAdu =
                        allowArrayDirectUse
                        && isImageTypeSinglePixelAduCompatible
                        && expectedSinglePixelCma;
                    assertEquals(
                        expectedSinglePixelAdu,
                        helper.isArrayDirectlyUsed());
                    
                    if (expectedSinglePixelAdu) {
                        final DataBuffer buffer = image.getRaster().getDataBuffer();
                        if (buffer instanceof DataBufferInt) {
                            assertSame(
                                ((DataBufferInt) buffer).getData(),
                                helper.getIntArrayDirectlyUsed());
                        } else {
                            assertNull(helper.getIntArrayDirectlyUsed());
                        }
                        if (buffer instanceof DataBufferUShort) {
                            assertSame(
                                ((DataBufferUShort) buffer).getData(),
                                helper.getShortArrayDirectlyUsed());
                        } else {
                            assertNull(helper.getShortArrayDirectlyUsed());
                        }
                        if (buffer instanceof DataBufferByte) {
                            assertSame(
                                ((DataBufferByte) buffer).getData(),
                                helper.getByteArrayDirectlyUsed());
                        } else {
                            assertNull(helper.getByteArrayDirectlyUsed());
                        }
                    } else {
                        assertNull(helper.getIntArrayDirectlyUsed());
                        assertNull(helper.getShortArrayDirectlyUsed());
                        assertNull(helper.getByteArrayDirectlyUsed());
                    }
                    
                    // Scanline stride always retrieved if possible.
                    assertEquals(
                        BufferedImageHelper.getScanlineStride(image),
                        helper.getScanlineStride());
                }
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_duplicate() {
        for (BufferedImage image : newImageList()) {
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                final BufferedImageHelper dup = helper.duplicate();
                test_duplicate_xxx(helper, dup);
            }
        }
    }
    
    public void test_duplicate_xxx(
        BufferedImageHelper helper,
        BufferedImageHelper dup) {
        
        assertSame(helper.getImage(), dup.getImage());
        assertSame(helper.getPixelFormat(), dup.getPixelFormat());
        //
        assertSame(helper.isColorModelAvoidingAllowed(), dup.isColorModelAvoidingAllowed());
        assertSame(helper.isColorModelAvoidedForSinglePixelMethods(), dup.isColorModelAvoidedForSinglePixelMethods());
        //
        assertSame(helper.isArrayDirectUseAllowed(), dup.isArrayDirectUseAllowed());
        assertSame(helper.isArrayDirectlyUsed(), dup.isArrayDirectlyUsed());
        //
        assertSame(helper.getIntArrayDirectlyUsed(), dup.getIntArrayDirectlyUsed());
        assertSame(helper.getShortArrayDirectlyUsed(), dup.getShortArrayDirectlyUsed());
        assertSame(helper.getByteArrayDirectlyUsed(), dup.getByteArrayDirectlyUsed());
        //
        assertSame(helper.getScanlineStride(), dup.getScanlineStride());
    }
    
    /*
     * 
     */
    
    public void test_getImage() {
        final BufferedImage image = newImage();
        final BufferedImageHelper helper = new BufferedImageHelper(image);
        assertSame(image, helper.getImage());
    }
    
    public void test_getPixelFormat() {
        for (BufferedImage image : newImageList()) {
            final BufferedImageHelper helper = new BufferedImageHelper(image);
            
            final BihPixelFormat expected =
                BufferedImageHelper.computePixelFormat(image);
            final BihPixelFormat actual = helper.getPixelFormat();
            assertEquals(expected, actual);
        }
    }
    
    public void test_isColorModelAvoidingAllowed() {
        // Already tested in constructor test.
    }
    
    public void test_isArrayDirectUsedAllowed() {
        // Already tested in constructor test.
    }
    
    /*
     * 
     */
    
    public void test_getIntArrayDirectlyUsed() {
        // Already tested in constructor test.
    }
    
    public void test_getShortArrayDirectlyUsed() {
        // Already tested in constructor test.
    }
    
    public void test_getByteArrayDirectlyUsed() {
        // Already tested in constructor test.
    }
    
    public void test_getScanlineStride() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        final int scanlineStride = imageWidth + 1;
        final BufferedImage image =
            BufferedImageHelper.newBufferedImageWithIntArray(
                null,
                scanlineStride,
                //
                imageWidth,
                imageHeight,
                //
                false,
                -1,
                //
                1,
                2,
                3);
        for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
            // NB: Here we only test int flavor.
            assertEquals(scanlineStride, helper.getScanlineStride());
        }
    }
    
    /*
     * 
     */
    
    public void test_computePixelFormat() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        /*
         * Images corresponding to all (BihPixelFormat,premul) types.
         */
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            for (boolean premul : BihTestUtils.newPremulArr(pixelFormat)) {
                
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        imageWidth,
                        imageWidth,
                        imageHeight,
                        pixelFormat,
                        premul);
                
                final BihPixelFormat actualPixelFormat =
                    BufferedImageHelper.computePixelFormat(image);
                assertEquals(pixelFormat, actualPixelFormat);
            }
        }
        /*
         * Images corresponding to all BufferedImage types.
         */
        // Excluding TYPE_CUSTOM.
        final Map<Integer,BihPixelFormat> pixelFormatByImageType = new TreeMap<>();
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            for (boolean premul : BihTestUtils.newPremulArr(pixelFormat)) {
                final int imageType = pixelFormat.toImageType(premul);
                if (imageType != BufferedImage.TYPE_CUSTOM) {
                    pixelFormatByImageType.put(imageType, pixelFormat);
                }
            }
        }
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            final BufferedImage image =
                new BufferedImage(
                    imageWidth,
                    imageHeight,
                    imageType);
            final BihPixelFormat actualPixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            
            // Possibly null.
            final BihPixelFormat expectedPixelFormat =
                pixelFormatByImageType.get(imageType);
            assertEquals(expectedPixelFormat, actualPixelFormat);
        }
    }
    
    /*
     * 
     */
    
    public void test_newBufferedImageWithIntArray_imageType() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        
        test_newBufferedImageWithIntArray_exceptions_lengthsSpans_xxx(
            MyNewImageMethodType.WITH_IMAGE_TYPE);
        
        /*
         * Bad type.
         */
        for (int imageType : TYPE_XXX_LIST) {
            if (!TYPE_INT_XXX_LIST.contains(imageType)) {
                try {
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        imageWidth,
                        imageWidth,
                        imageHeight,
                        imageType);
                    fail();
                } catch (IllegalArgumentException e) {
                    assertNotNull(e);
                }
            }
        }
        
        /*
         * With array created internally.
         */
        
        for (int imageType : TYPE_INT_XXX_LIST) {
            final int scanlineStride = imageWidth + 1;
            final BufferedImage image =
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    scanlineStride,
                    imageWidth,
                    imageHeight,
                    imageType);
            assertEquals(imageWidth, image.getWidth());
            assertEquals(imageHeight, image.getHeight());
            assertEquals(imageType, image.getType());
            final int[] pixelArr = BufferedImageHelper.getIntArray(image);
            assertNotNull(pixelArr);
            final int expectedLength =
                (imageHeight - 1) * scanlineStride + imageWidth;
            assertEquals(expectedLength, pixelArr.length);
        }
        
        /*
         * With array specified.
         */
        
        {
            final int scanlineStride = imageWidth + 1;
            final int length =
                (imageHeight - 1) * scanlineStride + imageWidth;
            final int[] pixelArr = new int[length];
            for (int imageType : TYPE_INT_XXX_LIST) {
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        pixelArr,
                        scanlineStride,
                        imageWidth,
                        imageHeight,
                        imageType);
                assertEquals(imageWidth, image.getWidth());
                assertEquals(imageHeight, image.getHeight());
                assertEquals(imageType, image.getType());
                assertSame(pixelArr, BufferedImageHelper.getIntArray(image));
                assertEquals(scanlineStride, BufferedImageHelper.getScanlineStride(image));
            }
        }
    }
    
    public void test_newBufferedImageWithIntArray_BihPixelFormat() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        
        test_newBufferedImageWithIntArray_exceptions_lengthsSpans_xxx(
            MyNewImageMethodType.WITH_PIXEL_FORMAT);
        
        /*
         * Null pixel format.
         */
        try {
            BufferedImageHelper.newBufferedImageWithIntArray(
                null,
                imageWidth,
                imageWidth,
                imageHeight,
                null,
                false);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
        /*
         * Bad premul.
         */
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            if (!pixelFormat.hasAlpha()) {
                try {
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        imageWidth,
                        imageWidth,
                        imageHeight,
                        pixelFormat,
                        true);
                    fail();
                } catch (IllegalArgumentException e) {
                    assertNotNull(e);
                }
            }
        }
        
        /*
         * With array created internally.
         */
        
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            for (boolean premul : BihTestUtils.newPremulArr(pixelFormat)) {
                final int scanlineStride = imageWidth + 1;
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        scanlineStride,
                        imageWidth,
                        imageHeight,
                        pixelFormat,
                        premul);
                assertEquals(imageWidth, image.getWidth());
                assertEquals(imageHeight, image.getHeight());
                final int expectedImageType =
                    pixelFormat.toImageType(premul);
                assertEquals(expectedImageType, image.getType());
                final int[] pixelArr = BufferedImageHelper.getIntArray(image);
                assertNotNull(pixelArr);
                final int expectedLength =
                    (imageHeight - 1) * scanlineStride + imageWidth;
                assertEquals(expectedLength, pixelArr.length);
            }
        }
        
        /*
         * With array specified.
         */
        
        {
            final int scanlineStride = imageWidth + 1;
            final int length =
                (imageHeight - 1) * scanlineStride + imageWidth;
            final int[] pixelArr = new int[length];
            for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
                for (boolean premul : BihTestUtils.newPremulArr(pixelFormat)) {
                    final BufferedImage image =
                        BufferedImageHelper.newBufferedImageWithIntArray(
                            pixelArr,
                            scanlineStride,
                            imageWidth,
                            imageHeight,
                            pixelFormat,
                            premul);
                    assertEquals(imageWidth, image.getWidth());
                    assertEquals(imageHeight, image.getHeight());
                    final int expectedImageType =
                        pixelFormat.toImageType(premul);
                    assertEquals(expectedImageType, image.getType());
                    assertSame(pixelArr, BufferedImageHelper.getIntArray(image));
                    assertEquals(scanlineStride, BufferedImageHelper.getScanlineStride(image));
                }
            }
        }
    }
    
    public void test_newBufferedImageWithIntArray_cptIndexes() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        
        test_newBufferedImageWithIntArray_exceptions_lengthsSpans_xxx(
            MyNewImageMethodType.WITH_CPT_INDEXES);
        
        /*
         * Bad premul.
         */
        try {
            BufferedImageHelper.newBufferedImageWithIntArray(
                null,
                imageWidth,
                //
                imageWidth,
                imageHeight,
                //
                true,
                -1,
                //
                1,
                2,
                3);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        /*
         * Bad component indexes.
         */
        {
            final Random random = TestUtils.newRandom123456789L();
            final double looseIndexRangeProba = 0.1;
            final int nbrOfCalls = 10 * 1000;
            for (int i = 0; i < nbrOfCalls; i++) {
                final int aIndex = randomAlphaCptIndex(
                    random,
                    looseIndexRangeProba);
                final boolean gotAlpha = (aIndex >= 0);
                final int rIndex = randomColorCptIndex(
                    random,
                    gotAlpha,
                    looseIndexRangeProba);
                final int gIndex = randomColorCptIndex(
                    random,
                    gotAlpha,
                    looseIndexRangeProba);
                final int bIndex = randomColorCptIndex(
                    random,
                    gotAlpha,
                    looseIndexRangeProba);
                
                final Set<Integer> indexSet = new TreeSet<>();
                indexSet.add(aIndex);
                indexSet.add(rIndex);
                indexSet.add(gIndex);
                indexSet.add(bIndex);
                final int expectMinColCptI = (gotAlpha ? 0 : 1);
                final boolean expectedOk =
                    (indexSet.size() == 4)
                    && NbrsUtils.isInRange(-1, 3, aIndex)
                    && NbrsUtils.isInRange(expectMinColCptI, 3, rIndex)
                    && NbrsUtils.isInRange(expectMinColCptI, 3, gIndex)
                    && NbrsUtils.isInRange(expectMinColCptI, 3, bIndex);
                
                if (expectedOk) {
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        imageWidth,
                        //
                        imageWidth,
                        imageHeight,
                        //
                        false,
                        aIndex,
                        //
                        rIndex,
                        gIndex,
                        bIndex);
                } else {
                    try {
                        BufferedImageHelper.newBufferedImageWithIntArray(
                            null,
                            imageWidth,
                            //
                            imageWidth,
                            imageHeight,
                            //
                            false,
                            aIndex,
                            //
                            rIndex,
                            gIndex,
                            bIndex);
                        fail();
                    } catch (IllegalArgumentException e) {
                        assertNotNull(e);
                    }
                }
            }
        }
        
        /*
         * With array created internally.
         */
        
        for (boolean premul : new boolean[] {false, true}) {
            final int scanlineStride = imageWidth + 1;
            final BufferedImage image =
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    scanlineStride,
                    //
                    imageWidth,
                    imageHeight,
                    //
                    premul,
                    0,
                    //
                    1,
                    2,
                    3);
            assertEquals(imageWidth, image.getWidth());
            assertEquals(imageHeight, image.getHeight());
            final int expectedImageType =
                (premul
                    ? BufferedImage.TYPE_INT_ARGB_PRE
                        : BufferedImage.TYPE_INT_ARGB);
            assertEquals(expectedImageType, image.getType());
            final int[] pixelArr = BufferedImageHelper.getIntArray(image);
            assertNotNull(pixelArr);
            assertEquals((imageHeight - 1) * scanlineStride + imageWidth, pixelArr.length);
        }
        
        /*
         * With array specified.
         */
        
        {
            for (boolean premul : new boolean[] {false, true}) {
                final int scanlineStride = imageWidth + 1;
                final int length =
                    (imageHeight - 1) * scanlineStride + imageWidth;
                final int[] pixelArr = new int[length];
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        pixelArr,
                        scanlineStride,
                        //
                        imageWidth,
                        imageHeight,
                        //
                        premul,
                        0,
                        //
                        1,
                        2,
                        3);
                assertEquals(imageWidth, image.getWidth());
                assertEquals(imageHeight, image.getHeight());
                final int expectedImageType =
                    (premul
                        ? BufferedImage.TYPE_INT_ARGB_PRE
                            : BufferedImage.TYPE_INT_ARGB);
                assertEquals(expectedImageType, image.getType());
                assertSame(pixelArr, BufferedImageHelper.getIntArray(image));
                assertEquals(scanlineStride, BufferedImageHelper.getScanlineStride(image));
            }
        }
    }
    
    public void test_newBufferedImageWithIntArray_exceptions_lengthsSpans_xxx(
        MyNewImageMethodType methodType) {
        
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        
        /*
         * Too small array.
         */
        try {
            callNewImageWithIntArray_lengthsSpans(
                methodType,
                //
                new int[imageWidth * imageHeight - 1],
                imageWidth,
                //
                imageWidth,
                imageHeight);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        /*
         * Bad scanline stride.
         */
        for (int badScanlineStride : newBadScanlineStrideArr(imageWidth)) {
            try {
                callNewImageWithIntArray_lengthsSpans(
                    methodType,
                    //
                    new int[imageWidth * imageHeight],
                    badScanlineStride,
                    //
                    imageWidth,
                    imageHeight);
                fail();
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
            }
        }
        /*
         * Bad array length.
         */
        {
            final int scanlineStride = imageWidth + 1;
            for (int badArrayLength : new int[] {
                0,
                1,
                ((imageHeight - 1) * scanlineStride + imageWidth) - 1,
            }) {
                try {
                    callNewImageWithIntArray_lengthsSpans(
                        methodType,
                        //
                        new int[badArrayLength],
                        scanlineStride,
                        //
                        imageWidth,
                        imageHeight);
                    fail();
                } catch (IllegalArgumentException e) {
                    assertNotNull(e);
                }
            }
        }
        /*
         * Bad width.
         */
        for (int badWidth : new int[] {
            Integer.MIN_VALUE,
            -1,
            0,
        }) {
            try {
                callNewImageWithIntArray_lengthsSpans(
                    methodType,
                    //
                    null,
                    imageWidth,
                    //
                    badWidth,
                    imageHeight);
                fail();
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
            }
        }
        /*
         * Bad height.
         */
        for (int badHeight : new int[] {
            Integer.MIN_VALUE,
            -1,
            0,
        }) {
            try {
                callNewImageWithIntArray_lengthsSpans(
                    methodType,
                    //
                    null,
                    imageWidth,
                    //
                    imageWidth,
                    badHeight);
                fail();
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * Tests all static methods related to int array.
     */
    public void test_xxxIntArray_static() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        final int scanlineStride = imageWidth + 1;
        // Fine if larger than needed.
        final int[] pixelArr = new int[scanlineStride * imageHeight];
        
        /*
         * Bad array type.
         */
        
        {
            final BufferedImage image =
                new BufferedImage(
                    imageWidth,
                    imageHeight,
                    BufferedImage.TYPE_3BYTE_BGR);
            
            assertFalse(BufferedImageHelper.hasSimpleIntArray(image));
            
            final boolean premul = false;
            
            assertFalse(BufferedImageHelper.hasCompatibleSimpleIntArray(
                image,
                BihPixelFormat.ARGB32,
                premul));
            
            try {
                BufferedImageHelper.requireCompatibleSimpleIntArray(
                    image,
                    BihPixelFormat.ARGB32,
                    premul);
                fail();
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
            }
            
            try {
                BufferedImageHelper.getIntArray(image);
                fail();
            } catch (ClassCastException e) {
                assertNotNull(e);
            }
        }
        
        /*
         * 
         */
        
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            for (boolean premul : BihTestUtils.newPremulArr(pixelFormat)) {
                
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        pixelArr,
                        scanlineStride,
                        imageWidth,
                        imageHeight,
                        pixelFormat,
                        premul);
                
                /*
                 * All good.
                 */
                
                assertTrue(BufferedImageHelper.hasSimpleIntArray(image));
                
                assertTrue(BufferedImageHelper.hasCompatibleSimpleIntArray(
                    image,
                    pixelFormat,
                    premul));
                
                BufferedImageHelper.requireCompatibleSimpleIntArray(
                    image,
                    pixelFormat,
                    premul);
                
                assertSame(pixelArr, BufferedImageHelper.getIntArray(image));
                
                assertEquals(scanlineStride, BufferedImageHelper.getScanlineStride(image));
                
                /*
                 * Bad pixel formats.
                 */
                
                for (BihPixelFormat badPixelFormat : BihPixelFormat.values()) {
                    if (badPixelFormat == pixelFormat) {
                        // good format
                        continue;
                    }
                    
                    assertFalse(BufferedImageHelper.hasCompatibleSimpleIntArray(
                        image,
                        badPixelFormat,
                        premul));
                    
                    try {
                        BufferedImageHelper.requireCompatibleSimpleIntArray(
                            image,
                            badPixelFormat,
                            premul);
                        fail();
                    } catch (IllegalArgumentException e) {
                        assertNotNull(e);
                    }
                }
                
                /*
                 * Bad premul.
                 */
                
                {
                    final boolean badPremul = !premul;
                    
                    assertFalse(BufferedImageHelper.hasCompatibleSimpleIntArray(
                        image,
                        pixelFormat,
                        badPremul));
                    
                    try {
                        BufferedImageHelper.requireCompatibleSimpleIntArray(
                            image,
                            pixelFormat,
                            badPremul);
                        fail();
                    } catch (IllegalArgumentException e) {
                        assertNotNull(e);
                    }
                }
            }
        }
    }
    
    public void test_getScanlineStride_static() {
        // Already covered by non-static getScanlineStride() tests.
    }
    
    /*
     * 
     */
    
    public void test_getArgb32At_setArgb32At_exceptions() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList(imageWidth, imageHeight)) {
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean premul = false;
                
                for (int badX : newBadPositionArr(imageWidth)) {
                    try {
                        helper.getArgb32At(badX, 0, premul);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        assertNotNull(e);
                    }
                    
                    try {
                        helper.setArgb32At(badX, 0, 0xFF000000, premul);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        assertNotNull(e);
                    }
                }
                
                for (int badY : newBadPositionArr(imageHeight)) {
                    try {
                        helper.getArgb32At(0, badY, premul);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        assertNotNull(e);
                    }
                    
                    try {
                        helper.setArgb32At(0, badY, 0xFF000000, premul);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        assertNotNull(e);
                    }
                }
            }
        }
    }
    
    public void test_getXxxArgb32At_allBihPixelFormat() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList_allPixelFormat(imageWidth, imageHeight)) {
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final BihPixelFormat pixelFormat = helper.getPixelFormat();
                final int[] pixelArr = BufferedImageHelper.getIntArray(image);
                final int scanlineStride = helper.getScanlineStride();
                
                final int x = 1;
                final int y = 2;
                // Components values high enough to preserve bijectivity
                // when converting between premul and non premul.
                int expectedNonPremulArgb32 = 0xA76543B1;
                if (!pixelFormat.hasAlpha()) {
                    expectedNonPremulArgb32 = Argb32.toOpaque(expectedNonPremulArgb32);
                }
                final int expectedPremulArgb32 =
                    BindingColorUtils.toPremulAxyz32(
                        expectedNonPremulArgb32);
                
                final int index = y * scanlineStride + x;
                {
                    final int argb32 =
                        (imagePremul ? expectedPremulArgb32 : expectedNonPremulArgb32);
                    final int pixel = pixelFormat.toPixelFromArgb32(argb32);
                    pixelArr[index] = pixel;
                }
                
                final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                checkEqual(expectedNonPremulArgb32, actualNonPremulArgb32);
                
                final int actualPremulArgb32 = helper.getPremulArgb32At(x, y);
                checkEqual(expectedPremulArgb32, actualPremulArgb32);
            }
        }
    }
    
    public void test_setXxxArgb32At_allBihPixelFormat() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList_allPixelFormat(imageWidth, imageHeight)) {
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final BihPixelFormat pixelFormat = helper.getPixelFormat();
                final int[] pixelArr = BufferedImageHelper.getIntArray(image);
                final int scanlineStride = helper.getScanlineStride();
                
                final int x = 1;
                final int y = 2;
                // Components values high enough to preserve bijectivity
                // when converting between premul and non premul.
                int nonPremulArgb32 = 0xA76543B1;
                if (!pixelFormat.hasAlpha()) {
                    nonPremulArgb32 = Argb32.toOpaque(nonPremulArgb32);
                }
                final int premulArgb32 = BindingColorUtils.toPremulAxyz32(nonPremulArgb32);
                
                final int expectedPixel =
                    pixelFormat.toPixelFromArgb32(
                        (imagePremul ? premulArgb32 : nonPremulArgb32));
                
                final int index = y * scanlineStride + x;
                {
                    helper.setNonPremulArgb32At(x, y, nonPremulArgb32);
                    final int actualPixel = pixelArr[index];
                    checkEqual(expectedPixel, actualPixel);
                }
                
                {
                    helper.setPremulArgb32At(x, y, premulArgb32);
                    final int actualPixel = pixelArr[index];
                    checkEqual(expectedPixel, actualPixel);
                }
            }
        }
    }
    
    public void test_getXxxArgb32At_allImageType() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            final BufferedImage image = new BufferedImage(
                imageWidth,
                imageHeight,
                imageType);
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean cma = helper.isColorModelAvoidedForSinglePixelMethods();
                
                final int x = 1;
                final int y = 2;
                
                final List<Integer> imageRgbSetList;
                final List<Integer> expectedHelperRgbGetList;
                if ((imageType == BufferedImage.TYPE_4BYTE_ABGR)
                    || (imageType == BufferedImage.TYPE_INT_ARGB)
                    || (imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE)
                    || (imageType == BufferedImage.TYPE_INT_ARGB_PRE)) {
                    imageRgbSetList = Arrays.asList(
                        0x00000000,
                        0x80804020,
                        0xFF818387,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = imageRgbSetList;
                } else if ((imageType == BufferedImage.TYPE_3BYTE_BGR)
                    || (imageType == BufferedImage.TYPE_INT_BGR)
                    || (imageType == BufferedImage.TYPE_INT_RGB)) {
                    imageRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF818387,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = imageRgbSetList;
                } else if (imageType == BufferedImage.TYPE_USHORT_555_RGB) {
                    imageRgbSetList = Arrays.asList(
                        0xFF000000,
                        // r,g,b:10001000
                        0xFF888888,
                        // r,g,b:11101000
                        0xFFE8E8E8,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (LSBits bits filled with MSBits hi)
                        0xFF8C8C8C,
                        // both: delta ko (should be 0xFFEFEFEF, but set pixel is messy for large RGBs)
                        (cma ? 0xFFE7E7E7 : 0xFFE6E6E6),
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_USHORT_565_RGB) {
                    imageRgbSetList = Arrays.asList(
                        0xFF000000,
                        // r,b:10001000, g:10000100
                        0xFF888488,
                        // r,b:11101000, g:11100100
                        0xFFE8E4E8,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (LSBits bits filled with MSBits hi)
                        0xFF8C868C,
                        // both: delta ko (should be 0xFFEFE7EF, but set pixel is messy for large RGBs)
                        (cma ? 0xFFE7E3E7 : 0xFFE6E3E6),
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_BYTE_INDEXED) {
                    imageRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF808080,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (only 256 colors)
                        0xFF7E7E7E,
                        0xFFFFFFFF);
                } else if ((imageType == BufferedImage.TYPE_USHORT_GRAY)
                    || (imageType == BufferedImage.TYPE_BYTE_GRAY)) {
                    imageRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF989898,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = Arrays.asList(
                        0xFF000000,
                        // (same, delta ko (color model heavy rework))
                        (cma ? 0xFF505050 : 0xFF989898),
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_BYTE_BINARY) {
                    imageRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF7F7F7F,
                        0xFF818181,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (only 2 colors)
                        0xFF000000,
                        // delta ok (only 2 colors)
                        0xFFFFFFFF,
                        0xFFFFFFFF);
                } else {
                    throw new AssertionError("" + imageTypeEnum);
                }
                
                for (int i = 0; i < imageRgbSetList.size(); i++) {
                    final int imageRgbSet = imageRgbSetList.get(i);
                    final int expectedHelperRgbGet = expectedHelperRgbGetList.get(i);
                    
                    image.setRGB(x, y, imageRgbSet);
                    
                    final int actualHelperRgbGet = helper.getNonPremulArgb32At(x, y);
                    
                    if (DEBUG) {
                        System.out.println("imageTypeEnum = " + imageTypeEnum);
                        System.out.println("colorModelAvoided = " + cma);
                        System.out.println("imageRgbSet =          " + Argb32.toString(imageRgbSet));
                        System.out.println("expectedHelperRgbGet = " + Argb32.toString(expectedHelperRgbGet));
                        System.out.println("actualHelperRgbGet =   " + Argb32.toString(actualHelperRgbGet));
                    }
                    checkEqual(expectedHelperRgbGet, actualHelperRgbGet);
                }
            }
        }
    }
    
    public void test_setXxxArgb32At_allImageType() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            final BufferedImage image = new BufferedImage(
                imageWidth,
                imageHeight,
                imageType);
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean cma = helper.isColorModelAvoidedForSinglePixelMethods();
                
                final int x = 1;
                final int y = 2;
                
                final List<Integer> helperRgbSetList;
                final List<Integer> expectedImageRgbGetList;
                if ((imageType == BufferedImage.TYPE_4BYTE_ABGR)
                    || (imageType == BufferedImage.TYPE_INT_ARGB)
                    || (imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE)
                    || (imageType == BufferedImage.TYPE_INT_ARGB_PRE)) {
                    helperRgbSetList = Arrays.asList(
                        0x00000000,
                        0x80804020,
                        0xFF818387,
                        0xFFFFFFFF);
                    expectedImageRgbGetList = helperRgbSetList;
                } else if ((imageType == BufferedImage.TYPE_3BYTE_BGR)
                    || (imageType == BufferedImage.TYPE_INT_BGR)
                    || (imageType == BufferedImage.TYPE_INT_RGB)) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF818387,
                        0xFFFFFFFF);
                    expectedImageRgbGetList = helperRgbSetList;
                } else if (imageType == BufferedImage.TYPE_USHORT_555_RGB) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        // r,g,b:10001000
                        0xFF888888,
                        // r,g,b:11101000
                        0xFFE8E8E8,
                        0xFFFFFFFF);
                    expectedImageRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (LSBits bits filled with MSBits hi)
                        0xFF8C8C8C,
                        // (delta ok (LSBits bits filled with MSBits hi),
                        // delta ko (set pixel is messy for large RGBs))
                        (cma ? 0xFFEFEFEF : 0xFFE6E6E6),
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_USHORT_565_RGB) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        // r,b:10001000, g:10000100
                        0xFF888488,
                        // r,b:11101000, g:11100100
                        0xFFE8E4E8,
                        0xFFFFFFFF);
                    expectedImageRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (LSBits bits filled with MSBits hi)
                        0xFF8C868C,
                        // (delta ok (LSBits bits filled with MSBits hi),
                        // delta ko (set pixel is messy for large RGBs))
                        (cma ? 0xFFEFE7EF : 0xFFE6E3E6),
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_BYTE_INDEXED) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF808080,
                        0xFFFFFFFF);
                    expectedImageRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (only 256 colors)
                        0xFF7E7E7E,
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_USHORT_GRAY) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF505050,
                        0xFFFFFFFF);
                    expectedImageRgbGetList = Arrays.asList(
                        0xFF000000,
                        // (delta ko (color model heavy rework), same)
                        (cma ? 0xFF989898 : 0xFF505050),
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_BYTE_GRAY) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF505050,
                        0xFFFFFFFF);
                    expectedImageRgbGetList = Arrays.asList(
                        0xFF000000,
                        // (delta ko (color model heavy rework),
                        // delta ko (color model not exactly bijective))
                        (cma ? 0xFF989898 : 0xFF4F4F4F),
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_BYTE_BINARY) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF7F7F7F,
                        0xFF818181,
                        0xFFFFFFFF);
                    expectedImageRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (only 2 color)
                        0xFF000000,
                        // delta ok (only 2 color)
                        0xFFFFFFFF,
                        0xFFFFFFFF);
                } else {
                    throw new AssertionError("" + imageTypeEnum);
                }
                
                for (int i = 0; i < helperRgbSetList.size(); i++) {
                    final int helperRgbSet = helperRgbSetList.get(i);
                    final int expectedImageRgbGet = expectedImageRgbGetList.get(i);
                    
                    helper.setNonPremulArgb32At(x, y, helperRgbSet);
                    
                    final int actualImageRgbGet = image.getRGB(x, y);
                    
                    if (DEBUG) {
                        System.out.println("imageTypeEnum = " + imageTypeEnum);
                        System.out.println("colorModelAvoided = " + cma);
                        System.out.println("helperRgbSet =        " + Argb32.toString(helperRgbSet));
                        System.out.println("expectedImageRgbGet = " + Argb32.toString(expectedImageRgbGet));
                        System.out.println("actualImageRgbGet =   " + Argb32.toString(actualImageRgbGet));
                    }
                    checkEqual(expectedImageRgbGet, actualImageRgbGet);
                }
            }
        }
    }
    
    public void test_setXxxArgb32At_getXxxArgb32At_deltasOkWithCma_allImageType() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            final BufferedImage image = new BufferedImage(
                imageWidth,
                imageHeight,
                imageType);
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean cma = helper.isColorModelAvoidedForSinglePixelMethods();
                if (!cma) {
                    continue;
                }
                
                final int x = 1;
                final int y = 2;
                
                final List<Integer> helperRgbSetList;
                final List<Integer> expectedHelperRgbGetList;
                if ((imageType == BufferedImage.TYPE_4BYTE_ABGR)
                    || (imageType == BufferedImage.TYPE_INT_ARGB)
                    || (imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE)
                    || (imageType == BufferedImage.TYPE_INT_ARGB_PRE)) {
                    helperRgbSetList = Arrays.asList(
                        0x00000000,
                        0x80804020,
                        0xFF818387,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = helperRgbSetList;
                } else if ((imageType == BufferedImage.TYPE_3BYTE_BGR)
                    || (imageType == BufferedImage.TYPE_INT_BGR)
                    || (imageType == BufferedImage.TYPE_INT_RGB)) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF818387,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = helperRgbSetList;
                } else if (imageType == BufferedImage.TYPE_USHORT_555_RGB) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        // r,g,b:10001000
                        0xFF888888,
                        // r,g,b:11101000
                        0xFFE8E8E8,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (LSBits bits filled with MSBits hi)
                        0xFF8C8C8C,
                        // delta ok (LSBits bits filled with MSBits hi)
                        0xFFEFEFEF,
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_USHORT_565_RGB) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        // r,b:10001000, g:10000100
                        0xFF888488,
                        // r,b:11101000, g:11100100
                        0xFFE8E4E8,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (LSBits bits filled with MSBits hi)
                        0xFF8C868C,
                        // delta ok (LSBits bits filled with MSBits hi)
                        0xFFEFE7EF,
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_BYTE_INDEXED) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF808080,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (only 256 colors)
                        0xFF7E7E7E,
                        0xFFFFFFFF);
                } else if (imageType == BufferedImage.TYPE_USHORT_GRAY) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF505050,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = helperRgbSetList;
                } else if (imageType == BufferedImage.TYPE_BYTE_GRAY) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF505050,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = helperRgbSetList;
                } else if (imageType == BufferedImage.TYPE_BYTE_BINARY) {
                    helperRgbSetList = Arrays.asList(
                        0xFF000000,
                        0xFF7F7F7F,
                        0xFF818181,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = Arrays.asList(
                        0xFF000000,
                        // delta ok (only 2 color)
                        0xFF000000,
                        // delta ok (only 2 color)
                        0xFFFFFFFF,
                        0xFFFFFFFF);
                } else {
                    throw new AssertionError("" + imageTypeEnum);
                }
                
                for (int i = 0; i < helperRgbSetList.size(); i++) {
                    final int helperRgbSet = helperRgbSetList.get(i);
                    final int expectedHelperRgbGet = expectedHelperRgbGetList.get(i);
                    
                    helper.setNonPremulArgb32At(x, y, helperRgbSet);
                    
                    final int actualHelperRgbGet = helper.getNonPremulArgb32At(x, y);
                    
                    if (DEBUG) {
                        System.out.println("imageTypeEnum = " + imageTypeEnum);
                        System.out.println("colorModelAvoided = " + cma);
                        System.out.println("helperRgbSet =         " + Argb32.toString(helperRgbSet));
                        System.out.println("expectedHelperRgbGet = " + Argb32.toString(expectedHelperRgbGet));
                        System.out.println("actualHelperRgbGet =   " + Argb32.toString(actualHelperRgbGet));
                    }
                    checkEqual(expectedHelperRgbGet, actualHelperRgbGet);
                }
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_clearRect_exceptions() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList(imageWidth, imageHeight)) {
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                for (int badX : newBadPositionArr(imageWidth)) {
                    try {
                        helper.clearRect(badX, 0, 1, 1, 0xFF000000, false);
                        fail();
                    } catch (IllegalArgumentException e) {
                        assertNotNull(e);
                    }
                }
                
                for (int badY : newBadPositionArr(imageHeight)) {
                    try {
                        helper.clearRect(0, badY, 1, 1, 0xFF000000, false);
                        fail();
                    } catch (IllegalArgumentException e) {
                        assertNotNull(e);
                    }
                }
            }
        }
    }
    
    public void test_clearRect_allBihPixelFormat() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList_allPixelFormat(imageWidth, imageHeight)) {
            final BufferedImageHelper helper = new BufferedImageHelper(image);
            
            final BihPixelFormat pixelFormat = helper.getPixelFormat();
            
            final int initialNonPremulArgb32 = helper.getNonPremulArgb32At(0, 0);
            
            int nonPremulArgb32 = 0xC0806040;
            if (!pixelFormat.hasAlpha()) {
                nonPremulArgb32 = Argb32.toOpaque(nonPremulArgb32);
            }
            final int premulArgb32 =
                BindingColorUtils.toPremulAxyz32(nonPremulArgb32);
            // Must be bijective.
            checkEqual(nonPremulArgb32, BindingColorUtils.toNonPremulAxyz32(premulArgb32));
            
            for (boolean premul : new boolean[] {false, true}) {
                final int argb32 =
                    (premul ? premulArgb32 : nonPremulArgb32);
                
                /*
                 * Clearing.
                 */
                
                helper.clearRect(1, 1, 3, 2, argb32, premul);
                
                // Rectangle cleared.
                for (int x = 1; x <= 3; x++) {
                    for (int y = 1; y <= 2; y++) {
                        final int actualArgb32 = helper.getNonPremulArgb32At(x, y);
                        checkEqual(nonPremulArgb32, actualArgb32);
                    }
                }
                
                // Surroundings not cleared.
                for (int x : new int[] {0, 4}) {
                    for (int y : new int[] {0, 3}) {
                        final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                        checkEqual(initialNonPremulArgb32, actualNonPremulArgb32);
                    }
                }
                
                /*
                 * Clearing again.
                 */
                
                helper.clearRect(1, 1, 3, 2, argb32, premul);
                
                // Still same color: equivalent to draw() not set().
                for (int x = 1; x <= 3; x++) {
                    for (int y = 1; y <= 2; y++) {
                        final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                        checkEqual(nonPremulArgb32, actualNonPremulArgb32);
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_getPixelsInto_exceptions() {
        final boolean isGetElseSet = true;
        this.test_getPixelsInto_setPixelsInto_exceptions_xxx(
            isGetElseSet);
    }
    
    public void test_setPixelsFrom_exceptions() {
        final boolean isGetElseSet = false;
        this.test_getPixelsInto_setPixelsInto_exceptions_xxx(
            isGetElseSet);
    }
    
    public void test_getPixelsInto_setPixelsInto_exceptions_xxx(
        boolean isGetElseSet) {
        // Large enough for some coordinates leeway.
        final int imageWidth = 30;
        final int imageHeight = 20;
        for (BufferedImage image : BihTestUtils.newImageList(imageWidth, imageHeight)) {
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                /*
                 * Null array.
                 */
                try {
                    callGetOrSetPixelsInto(
                        isGetElseSet,
                        helper,
                        //
                        null,
                        imageWidth,
                        BihPixelFormat.ARGB32,
                        false,
                        0,
                        0,
                        //
                        0,
                        0,
                        //
                        imageWidth,
                        imageHeight);
                    fail();
                } catch (NullPointerException e) {
                    assertNotNull(e);
                }
                /*
                 * Null pixel format.
                 */
                try {
                    callGetOrSetPixelsInto(
                        isGetElseSet,
                        helper,
                        //
                        new int[imageWidth * imageHeight],
                        imageWidth,
                        null,
                        false,
                        0,
                        0,
                        //
                        0,
                        0,
                        //
                        imageWidth,
                        imageHeight);
                    fail();
                } catch (NullPointerException e) {
                    assertNotNull(e);
                }
                /*
                 * Bad premul.
                 */
                try {
                    callGetOrSetPixelsInto(
                        isGetElseSet,
                        helper,
                        //
                        new int[imageWidth * imageHeight],
                        imageWidth,
                        BihPixelFormat.XRGB24,
                        true,
                        0,
                        0,
                        //
                        0,
                        0,
                        //
                        imageWidth,
                        imageHeight);
                    fail();
                } catch (IllegalArgumentException e) {
                    assertNotNull(e);
                }
                /*
                 * Bad X range.
                 * Using same width for array as for image.
                 */
                for (int[] arrImgSpan : newBadRangeArr(imageWidth, imageWidth)) {
                    final int arrX = arrImgSpan[0];
                    final int imgX = arrImgSpan[1];
                    final int width = arrImgSpan[2];
                    try {
                        callGetOrSetPixelsInto(
                            isGetElseSet,
                            helper,
                            //
                            new int[imageWidth * imageHeight],
                            imageWidth,
                            BihPixelFormat.ARGB32,
                            false,
                            arrX,
                            0,
                            //
                            imgX,
                            0,
                            //
                            width,
                            imageHeight);
                        fail();
                    } catch (IllegalArgumentException e) {
                        assertNotNull(e);
                    }
                }
                /*
                 * Bad Y range.
                 * Using same height for array as for image.
                 */
                for (int[] arrImgSpan : newBadRangeArr(imageHeight, imageHeight)) {
                    final int arrY = arrImgSpan[0];
                    final int imgY = arrImgSpan[1];
                    final int height = arrImgSpan[2];
                    try {
                        callGetOrSetPixelsInto(
                            isGetElseSet,
                            helper,
                            //
                            new int[imageWidth * imageHeight],
                            imageWidth,
                            BihPixelFormat.ARGB32,
                            false,
                            0,
                            arrY,
                            //
                            0,
                            imgY,
                            //
                            imageWidth,
                            height);
                        fail();
                    } catch (IllegalArgumentException e) {
                        assertNotNull(e);
                    }
                }
                /*
                 * Bad scanline stride.
                 * Zero allowed if width is zero,
                 * to be able to use width as scanline stride.
                 */
                for (int badScanlineStride : newBadScanlineStrideArr(imageWidth)) {
                    for (int width : new int[] {0, imageWidth}) {
                        try {
                            callGetOrSetPixelsInto(
                                isGetElseSet,
                                helper,
                                //
                                new int[imageWidth * imageHeight],
                                badScanlineStride,
                                BihPixelFormat.ARGB32,
                                false,
                                0,
                                0,
                                //
                                0,
                                0,
                                //
                                width,
                                imageHeight);
                            if (width != 0) {
                                fail();
                            }
                        } catch (IllegalArgumentException e) {
                            if (width == 0) {
                                assertNotNull(e);
                            }
                        }
                    }
                }
                /*
                 * Bad array length.
                 */
                {
                    final int srcX = 2;
                    final int srcY = 1;
                    final int width = imageWidth - 8;
                    final int height = imageHeight - 9;
                    final int scanlineStride = width + 1;
                    for (int badArrayLength : new int[] {
                        0,
                        1,
                        ((height - 1) * scanlineStride + width) - 1,
                    }) {
                        try {
                            callGetOrSetPixelsInto(
                                isGetElseSet,
                                helper,
                                //
                                new int[badArrayLength],
                                scanlineStride,
                                BihPixelFormat.ARGB32,
                                false,
                                0,
                                0,
                                //
                                srcX,
                                srcY,
                                //
                                width,
                                height);
                            fail();
                        } catch (IllegalArgumentException e) {
                            assertNotNull(e);
                        }
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_getPixelsInto_allImageKind_fullRect() {
        final boolean srcRectElseFullRect = false;
        this.test_getPixelsInto_allImageKind_xxx(srcRectElseFullRect);
    }
    
    public void test_getPixelsInto_allImageKind_srcRect() {
        final boolean srcRectElseFullRect = true;
        this.test_getPixelsInto_allImageKind_xxx(srcRectElseFullRect);
    }
    
    public void test_getPixelsInto_allImageKind_xxx(boolean srcRectElseFullRect) {
        /*
         * Large enough to get all kinds of alpha and color components,
         * and various kinds of (alpha,color) pairs
         * (at least when srcRectElseFullRect is false).
         */
        final int imageWidth = 256;
        final int imageHeight = 32;
        
        final Random random = TestUtils.newRandom123456789L();
        
        for (int strideBonus : new int[] {0, 1}) {
            
            final int color32ArrScanlineStride = imageWidth + strideBonus;
            
            final int[] expectedColor32Arr =
                new int[color32ArrScanlineStride * imageHeight];
            final int[] actualColor32Arr =
                new int[color32ArrScanlineStride * imageHeight];
            
            for (BufferedImage image : BihTestUtils.newImageList(imageWidth, imageHeight)) {
                for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                    
                    final boolean cmaAllowed =
                        helper.isColorModelAvoidingAllowed();
                    
                    // Can be null.
                    final BihPixelFormat imagePixelFormat =
                        helper.getPixelFormat();
                    final boolean imagePremul = image.isAlphaPremultiplied();
                    // Can be null.
                    final ImageTypeEnum imageTypeEnum =
                        ImageTypeEnum.enumByType().get(image.getType());
                    
                    // Randomizing input image.
                    for (int y = 0; y < imageHeight; y++) {
                        for (int x = 0; x < imageWidth; x++) {
                            final int argb32 = random.nextInt();
                            helper.setNonPremulArgb32At(x, y, argb32);
                        }
                    }
                    
                    for (BihPixelFormat pixelFormatTo : BihPixelFormat.values()) {
                        for (boolean premulTo : BihTestUtils.newPremulArr(pixelFormatTo)) {
                            if (DEBUG) {
                                System.out.println();
                                System.out.println("imageWidth = " + imageWidth);
                                System.out.println("imageHeight = " + imageHeight);
                                System.out.println("imagePixelFormat = " + imagePixelFormat);
                                System.out.println("imagePremul = " + imagePremul);
                                System.out.println("imageTypeEnum = " + imageTypeEnum);
                                System.out.println("colorModelAvoidingAllowed = " + cmaAllowed);
                                System.out.println("scanlineStrideTo = " + color32ArrScanlineStride);
                                System.out.println("pixelFormatTo = " + pixelFormatTo);
                                System.out.println("premulTo = " + premulTo);
                            }
                            
                            final int srcX;
                            final int srcY;
                            final int width;
                            final int height;
                            if (srcRectElseFullRect) {
                                srcX = random.nextInt(imageWidth);
                                srcY = random.nextInt(imageHeight);
                                // Zero width/imageHeight accepted.
                                width = random.nextInt(imageWidth - srcX + 1);
                                height = random.nextInt(imageHeight - srcY + 1);
                            } else {
                                srcX = 0;
                                srcY = 0;
                                width = imageWidth;
                                height = imageHeight;
                            }
                            
                            // Filling output with some semi opaque color,
                            // which opaque version should be exact whatever the image type,
                            // to check for no damage outside destination area.
                            final int initialPixel;
                            {
                                int initialArgb32 = 0x80FFFFFF;
                                if (premulTo) {
                                    initialArgb32 =
                                        BindingColorUtils.toPremulAxyz32(
                                            initialArgb32);
                                }
                                initialPixel =
                                    pixelFormatTo.toPixelFromArgb32(
                                        initialArgb32);
                                Arrays.fill(actualColor32Arr, initialPixel);
                            }
                            
                            BihTestUtils.getPixelsInto_reference(
                                helper,
                                //
                                srcX,
                                srcY,
                                //
                                expectedColor32Arr,
                                color32ArrScanlineStride,
                                pixelFormatTo,
                                premulTo,
                                0,
                                0,
                                //
                                width,
                                height);
                            
                            callGetPixelsInto(
                                helper,
                                //
                                srcX,
                                srcY,
                                //
                                actualColor32Arr,
                                color32ArrScanlineStride,
                                pixelFormatTo,
                                premulTo,
                                0,
                                0,
                                //
                                width,
                                height);
                            
                            // Checking no damage to pixels outside the area.
                            for (int i = 0; i < actualColor32Arr.length; i++) {
                                final int x = i % color32ArrScanlineStride;
                                final int y = i / color32ArrScanlineStride;
                                if ((x >= width)
                                    || (y >= height)) {
                                    final int actualPixel = actualColor32Arr[i];
                                    checkEqual(initialPixel, actualPixel);
                                }
                            }
                            
                            // Checking pixels within the area.
                            for (int y = srcY; y < srcY + height; y++) {
                                for (int x = srcX; x < srcX + width; x++) {
                                    final int index =
                                        (y - srcY) * color32ArrScanlineStride
                                        + (x - srcX);
                                    final int expectedColor32 = expectedColor32Arr[index];
                                    final int actualColor32 = actualColor32Arr[index];
                                    
                                    final BihPixelFormat drawPixelFormatTo =
                                        BufferedImageHelper.DRAW_IMAGE_FAST_DST_PIXEL_FORMAT;
                                    
                                    final int drawImageTol =
                                        BufferedImageHelper.getDrawImageMaxCptDelta(
                                            image.getType(),
                                            helper.getPixelFormat(),
                                            image.isAlphaPremultiplied(),
                                            //
                                            drawPixelFormatTo.toImageType(premulTo),
                                            drawPixelFormatTo,
                                            premulTo);
                                    
                                    if (cmaAllowed
                                        && (drawImageTol == 1)) {
                                        /*
                                         * In this case, we use drawImage(),
                                         * but it can give a slightly different result,
                                         * but only when color is not opaque.
                                         */
                                        final int refArgb32 =
                                            helper.getNonPremulArgb32At(x, y);
                                        final int refAlpha8 =
                                            Argb32.getAlpha8(
                                                refArgb32);
                                        final int cptDeltaTol =
                                            ((refAlpha8 <= 0xFE) ? 1 : 0);
                                        checkCloseColor32(
                                            expectedColor32,
                                            actualColor32,
                                            cptDeltaTol);
                                    } else {
                                        checkEqual(expectedColor32, actualColor32);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_setPixelsFrom_allImageKind_fullRect() {
        final boolean srcRectElseFullRect = false;
        this.test_setPixelsFrom_allImageKind_xxx(srcRectElseFullRect);
    }
    
    public void test_setPixelsFrom_allImageKind_srcRect() {
        final boolean srcRectElseFullRect = true;
        this.test_setPixelsFrom_allImageKind_xxx(srcRectElseFullRect);
    }
    
    public void test_setPixelsFrom_allImageKind_xxx(boolean srcRectElseFullRect) {
        /*
         * Large enough to get all kinds of alpha and color components,
         * and various kinds of (alpha,color) pairs
         * (at least when srcRectElseFullRect is false).
         */
        final int imageWidth = 256;
        final int imageHeight = 32;
        
        final Random random = TestUtils.newRandom123456789L();
        
        for (int strideBonus : new int[] {0, 1}) {
            
            final int color32ArrScanlineStride = imageWidth + strideBonus;
            final int[] color32Arr =
                new int[color32ArrScanlineStride * imageHeight];
            
            for (BufferedImage image : BihTestUtils.newImageList(imageWidth, imageHeight)) {
                for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                    
                    final boolean cmaAllowed =
                        helper.isColorModelAvoidingAllowed();
                    
                    // Can be null.
                    final BihPixelFormat imagePixelFormat =
                        helper.getPixelFormat();
                    final boolean imagePremul = image.isAlphaPremultiplied();
                    // Can be null.
                    final ImageTypeEnum imageTypeEnum =
                        ImageTypeEnum.enumByType().get(image.getType());
                    
                    final BufferedImageHelper expectedImageHelper =
                        BihTestUtils.newIdenticalImageAndHelper(helper);
                    
                    for (BihPixelFormat pixelFormatFrom : BihPixelFormat.values()) {
                        for (boolean premulFrom : BihTestUtils.newPremulArr(pixelFormatFrom)) {
                            if (DEBUG) {
                                System.out.println();
                                System.out.println("scanlineStrideFrom = " + color32ArrScanlineStride);
                                System.out.println("pixelFormatFrom = " + pixelFormatFrom);
                                System.out.println("premulFrom = " + premulFrom);
                                System.out.println("imageWidth = " + imageWidth);
                                System.out.println("imageHeight = " + imageHeight);
                                System.out.println("imagePixelFormat = " + imagePixelFormat);
                                System.out.println("imagePremul = " + imagePremul);
                                System.out.println("imageTypeEnum = " + imageTypeEnum);
                                System.out.println("colorModelAvoidingAllowed = " + cmaAllowed);
                            }
                            
                            final int dstX;
                            final int dstY;
                            final int dstWidth;
                            final int dstHeight;
                            if (srcRectElseFullRect) {
                                dstX = random.nextInt(imageWidth);
                                dstY = random.nextInt(imageHeight);
                                // Zero width/imageHeight accepted.
                                dstWidth = random.nextInt(imageWidth - dstX + 1);
                                dstHeight = random.nextInt(imageHeight - dstY + 1);
                            } else {
                                dstX = 0;
                                dstY = 0;
                                dstWidth = imageWidth;
                                dstHeight = imageHeight;
                            }
                            
                            // Randomizing input array.
                            for (int i = 0; i < color32Arr.length; i++) {
                                int argb32 = random.nextInt();
                                if (pixelFormatFrom.hasAlpha()) {
                                    if (premulFrom) {
                                        argb32 = BindingColorUtils.toPremulAxyz32(argb32);
                                    }
                                } else {
                                    argb32 = Argb32.toOpaque(argb32);
                                }
                                final int pixel = pixelFormatFrom.toPixelFromArgb32(argb32);
                                color32Arr[i] = pixel;
                            }
                            
                            // Filling output with some semi opaque color,
                            // which opaque version should be exact whatever the image type,
                            // to check for no damage outside destination area.
                            final int initialNonPremulArgb32;
                            {
                                int argb32 = 0x80FFFFFF;
                                if (image.getTransparency() == Transparency.OPAQUE) {
                                    argb32 = Argb32.toOpaque(argb32);
                                }
                                initialNonPremulArgb32 = argb32;
                                helper.clearRect(0, 0, imageWidth, imageHeight, initialNonPremulArgb32, false);
                            }
                            
                            BihTestUtils.setPixelsFrom_reference(
                                expectedImageHelper,
                                //
                                color32Arr,
                                color32ArrScanlineStride,
                                pixelFormatFrom,
                                premulFrom,
                                0,
                                0,
                                //
                                dstX,
                                dstY,
                                //
                                dstWidth,
                                dstHeight);
                            
                            callSetPixelsFrom(
                                helper,
                                //
                                color32Arr,
                                color32ArrScanlineStride,
                                pixelFormatFrom,
                                premulFrom,
                                0,
                                0,
                                //
                                dstX,
                                dstY,
                                //
                                dstWidth,
                                dstHeight);
                            
                            // Checking no damage to pixels outside the area.
                            for (int y = 0; y < imageHeight; y++) {
                                for (int x = 0; x < imageWidth; x++) {
                                    if ((x < dstX)
                                        || (y < dstY)
                                        || (x >= dstX + dstWidth)
                                        || (y >= dstY + dstHeight)) {
                                        final int actualNonPremulArgb32 =
                                            helper.getNonPremulArgb32At(x, y);
                                        checkEqual(
                                            initialNonPremulArgb32,
                                            actualNonPremulArgb32);
                                    }
                                }
                            }
                            
                            // Checking pixels within the area.
                            for (int y = dstY; y < dstY + dstHeight; y++) {
                                for (int x = dstX; x < dstX + dstWidth; x++) {
                                    final int expectedNonPremulArgb32 =
                                        expectedImageHelper.getNonPremulArgb32At(x, y);
                                    final int actualNonPremulArgb32 =
                                        helper.getNonPremulArgb32At(x, y);
                                    
                                    final int drawImageTol =
                                        BufferedImageHelper.getDrawImageMaxCptDelta(
                                            pixelFormatFrom.toImageType(premulFrom),
                                            pixelFormatFrom,
                                            premulFrom,
                                            //
                                            image.getType(),
                                            helper.getPixelFormat(),
                                            image.isAlphaPremultiplied());
                                    
                                    if (cmaAllowed
                                        && (drawImageTol == 1)) {
                                        /*
                                         * In this case, we use drawImage(),
                                         * but it can give a slightly different result,
                                         * but only when color is not opaque or
                                         * (destination) image is of gray type.
                                         */
                                        final int refAlpha8 =
                                            Argb32.getAlpha8(
                                                expectedNonPremulArgb32);
                                        final int cptDeltaTol =
                                            ((refAlpha8 <= 0xFE)
                                                || isGray(image.getType()) ? 1 : 0);
                                        checkCloseColor32(
                                            expectedNonPremulArgb32,
                                            actualNonPremulArgb32,
                                            cptDeltaTol);
                                    } else {
                                        checkEqual(expectedNonPremulArgb32, actualNonPremulArgb32);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_copyImage_exceptions() {
        // Large enough for some coordinates leeway.
        for (int srcWidth : new int[] {30,31}) {
            for (int srcHeight : new int[] {20,21}) {
                final BufferedImage imageFrom =
                    newImage(srcWidth, srcHeight);
                final BufferedImageHelper helperFrom =
                    new BufferedImageHelper(imageFrom);
                for (int dstWidth : new int[] {30,31}) {
                    for (int dstHeight : new int[] {20,21}) {
                        final BufferedImage imageTo =
                            newImage(dstWidth, dstHeight);
                        final BufferedImageHelper helperTo =
                            new BufferedImageHelper(imageTo);
                        test_copyImage_exceptions_xxx(
                            helperFrom,
                            helperTo);
                    }
                }
            }
        }
    }
    
    public void test_copyImage_exceptions_xxx(
        BufferedImageHelper helperFrom,
        BufferedImageHelper helperTo) {
        
        final BufferedImage imageFrom = helperFrom.getImage();
        final BufferedImage imageTo = helperTo.getImage();
        
        final int srcWidth = imageFrom.getWidth();
        final int srcHeight = imageFrom.getHeight();
        
        final int dstWidth = imageTo.getWidth();
        final int dstHeight = imageTo.getHeight();
        
        final int minWidth = Math.min(srcWidth, dstWidth);
        final int minHeight = Math.min(srcHeight, dstHeight);
        
        /*
         * Null helper.
         */
        try {
            callCopyImage(
                helperFrom,
                0,
                0,
                null,
                0,
                0,
                minWidth,
                minHeight);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
        try {
            callCopyImage(
                null,
                0,
                0,
                helperTo,
                0,
                0,
                minWidth,
                minHeight);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
        /*
         * Bad X range.
         */
        for (int[] srcDstSpan : newBadRangeArr(srcWidth, dstWidth)) {
            final int srcX = srcDstSpan[0];
            final int dstX = srcDstSpan[1];
            final int width = srcDstSpan[2];
            try {
                callCopyImage(
                    helperFrom,
                    srcX,
                    0,
                    helperTo,
                    dstX,
                    0,
                    width,
                    0);
                fail();
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
            }
        }
        /*
         * Bad Y range.
         */
        for (int[] srcDstSpan : newBadRangeArr(srcHeight, dstHeight)) {
            final int srcY = srcDstSpan[0];
            final int dstY = srcDstSpan[1];
            final int height = srcDstSpan[2];
            try {
                callCopyImage(
                    helperFrom,
                    0,
                    srcY,
                    helperTo,
                    0,
                    dstY,
                    0,
                    height);
                fail();
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
            }
        }
    }
    
    public void test_copyImage_fullRect() {
        final boolean srcDstRectElseFullRect = false;
        this.test_copyImage_xxx(srcDstRectElseFullRect);
    }
    
    public void test_copyImage_srcDstRect() {
        final boolean srcDstRectElseFullRect = true;
        this.test_copyImage_xxx(srcDstRectElseFullRect);
    }
    
    public void test_copyImage_xxx(boolean srcDstRectElseFullRect) {
        /*
         * Large enough to get all kinds of alpha and color components,
         * and various kinds of (alpha,color) pairs
         * (at least when srcDstRectElseFullRect is false).
         * Not too large because we test a lot of helper pairs.
         * Using a single Random for all the loops helps
         * encountering more cases.
         */
        final int imageWidth = 256/4;
        final int imageHeight = 32/2;
        
        final Random random = TestUtils.newRandom123456789L();
        
        for (BufferedImage imageFrom : BihTestUtils.newImageList(imageWidth, imageHeight)) {
            for (BufferedImageHelper helperFrom : BihTestUtils.newHelperList(imageFrom)) {
                
                // When not doing full copies,
                // using different image width and height.
                final int spanDelta = (srcDstRectElseFullRect ? 1 : 0);
                
                for (BufferedImage imageTo : BihTestUtils.newImageList(
                    imageWidth + spanDelta,
                    imageHeight - spanDelta)) {
                    for (BufferedImageHelper helperTo : BihTestUtils.newHelperList(imageTo)) {
                        
                        test_copyImage_xxx_withHelpers(
                            random,
                            srcDstRectElseFullRect,
                            helperFrom,
                            helperTo);
                    }
                }
            }
        }
    }
    
    public void test_copyImage_xxx_withHelpers(
        Random random,
        boolean srcDstRectElseFullRect,
        BufferedImageHelper helperFrom,
        BufferedImageHelper helperTo) {
        
        final BufferedImage imageFrom = helperFrom.getImage();
        final BufferedImage imageTo = helperTo.getImage();
        
        final int srcImageWidth = imageFrom.getWidth();
        final int srcImageHeight = imageFrom.getHeight();
        
        final int dstImageWidth = imageTo.getWidth();
        final int dstImageHeight = imageTo.getHeight();
        
        final int minImageWidth = Math.min(srcImageWidth, dstImageWidth);
        final int minImageHeight = Math.min(srcImageHeight, dstImageHeight);
        
        // Randomizing input image.
        for (int y = 0; y < srcImageHeight; y++) {
            for (int x = 0; x < srcImageWidth; x++) {
                final int argb32 = random.nextInt();
                helperFrom.setNonPremulArgb32At(x, y, argb32);
            }
        }
        
        // Can be null.
        final ImageTypeEnum imageTypeEnumFrom =
            ImageTypeEnum.enumByType().get(imageFrom.getType());
        // Can be null.
        final BihPixelFormat pixelFormatFrom =
            helperFrom.getPixelFormat();
        final boolean premulFrom = imageFrom.isAlphaPremultiplied();
        
        // Can be null.
        final ImageTypeEnum imageTypeEnumTo =
            ImageTypeEnum.enumByType().get(imageTo.getType());
        // Can be null.
        final BihPixelFormat pixelFormatTo =
            helperTo.getPixelFormat();
        final boolean premulTo = imageTo.isAlphaPremultiplied();
        
        final boolean cmaAllowedFrom =
            helperFrom.isColorModelAvoidingAllowed();
        final boolean aduAllowedFrom =
            helperFrom.isArrayDirectUseAllowed();
        
        final boolean cmaAllowedTo =
            helperTo.isColorModelAvoidingAllowed();
        final boolean aduAllowedTo =
            helperTo.isArrayDirectUseAllowed();
        
        if (DEBUG) {
            System.out.println();
            System.out.println("srcImageWidth = " + srcImageWidth);
            System.out.println("srcImageHeight = " + srcImageHeight);
            System.out.println("dstImageWidth = " + dstImageWidth);
            System.out.println("dstImageHeight = " + dstImageHeight);
            //
            System.out.println("imageTypeEnumFrom = " + imageTypeEnumFrom);
            System.out.println("pixelFormatFrom = " + pixelFormatFrom);
            System.out.println("premulFrom = " + premulFrom);
            System.out.println("imageTypeEnumTo = " + imageTypeEnumTo);
            System.out.println("pixelFormatTo = " + pixelFormatTo);
            System.out.println("premulTo = " + premulTo);
            //
            System.out.println("cmaAllowedFrom = " + cmaAllowedFrom);
            System.out.println("aduAllowedFrom = " + aduAllowedFrom);
            System.out.println("cmaAllowedTo = " + cmaAllowedTo);
            System.out.println("aduAllowedTo = " + aduAllowedTo);
        }
        
        final int srcX;
        final int srcY;
        final int dstX;
        final int dstY;
        final int width;
        final int height;
        if (srcDstRectElseFullRect) {
            srcX = random.nextInt(srcImageWidth);
            srcY = random.nextInt(srcImageHeight);
            dstX = random.nextInt(dstImageWidth);
            dstY = random.nextInt(dstImageHeight);
            // Zero width/height accepted.
            width = random.nextInt(
                Math.min(
                    srcImageWidth - srcX,
                    dstImageWidth - dstX) + 1);
            height = random.nextInt(
                Math.min(
                    srcImageHeight - srcY,
                    dstImageHeight - dstY) + 1);
        } else {
            srcX = 0;
            srcY = 0;
            dstX = 0;
            dstY = 0;
            // Min for safety, in practice all equal here.
            width = minImageWidth;
            height = minImageHeight;
        }
        
        // Filling output with some semi opaque color,
        // which opaque version should be exact whatever the image type,
        // to check for no damage outside destination area.
        final int initialNonPremulArgb32;
        {
            int argb32 = 0x80FFFFFF;
            if (imageTo.getTransparency() == Transparency.OPAQUE) {
                argb32 = Argb32.toOpaque(argb32);
            }
            initialNonPremulArgb32 = argb32;
            for (int y = 0; y < dstImageHeight; y++) {
                for (int x = 0; x < dstImageWidth; x++) {
                    helperTo.setNonPremulArgb32At(x, y, initialNonPremulArgb32);
                }
            }
        }
        
        final BufferedImageHelper expectedImageHelper =
            BihTestUtils.newIdenticalImageAndHelper(helperTo);
        
        BihTestUtils.copyImage_reference(
            helperFrom,
            srcX,
            srcY,
            expectedImageHelper,
            dstX,
            dstY,
            width,
            height);
        
        callCopyImage(
            helperFrom,
            srcX,
            srcY,
            helperTo,
            dstX,
            dstY,
            width,
            height);
        
        // Checking no damage to pixels outside the area.
        for (int y = 0; y < dstImageHeight; y++) {
            for (int x = 0; x < dstImageWidth; x++) {
                if ((x < dstX)
                    || (y < dstY)
                    || (x >= dstX + width)
                    || (y >= dstY + height)) {
                    final int actualArgb32 =
                        helperTo.getNonPremulArgb32At(x, y);
                    checkEqual(
                        initialNonPremulArgb32,
                        actualArgb32);
                }
            }
        }
        
        // Checking pixels within the area.
        for (int y = dstY; y < dstY + height; y++) {
            for (int x = dstX; x < dstX + width; x++) {
                final int expectedArgb32 =
                    expectedImageHelper.getNonPremulArgb32At(x, y);
                final int actualArgb32 =
                    helperTo.getNonPremulArgb32At(x, y);
                
                final int drawImageTolWithFormatTo =
                    BufferedImageHelper.getDrawImageMaxCptDelta(
                        imageFrom.getType(),
                        pixelFormatFrom,
                        premulFrom,
                        //
                        imageTo.getType(),
                        pixelFormatTo,
                        premulTo);
                final BihPixelFormat fastDstFormat =
                    BufferedImageHelper.DRAW_IMAGE_FAST_DST_PIXEL_FORMAT;
                final int drawImageTolWithFastDstFormat =
                    BufferedImageHelper.getDrawImageMaxCptDelta(
                        imageFrom.getType(),
                        pixelFormatFrom,
                        premulFrom,
                        //
                        fastDstFormat.toImageType(premulTo),
                        fastDstFormat,
                        premulTo);
                
                if (cmaAllowedFrom
                    && cmaAllowedTo
                    && ((drawImageTolWithFormatTo == 1)
                        || (drawImageTolWithFastDstFormat == 1))) {
                    /*
                     * In this case, we use drawImage(),
                     * but it can give a slightly different result,
                     * but only when color is not opaque or
                     * destination image is of gray type.
                     */
                    final int dx = x + (srcX - dstX);
                    final int dy = y + (srcY - dstY);
                    final int refArgb32 =
                        helperFrom.getNonPremulArgb32At(dx, dy);
                    final int refAlpha8 = Argb32.getAlpha8(refArgb32);
                    final int cptDeltaTol =
                        ((refAlpha8 <= 0xFE)
                            || isGray(imageTo.getType()) ? 1 : 0);
                    checkCloseColor32(
                        expectedArgb32,
                        actualArgb32,
                        cptDeltaTol);
                } else {
                    checkEqual(expectedArgb32, actualArgb32);
                }
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_getDrawImageMaxCptDelta() {
        
        // Large enough to encounter all possible issues.
        final int imageWidth = 256;
        final int imageHeight = 256;
        
        for (BufferedImage imageFrom : BihTestUtils.newImageList_allImageType(imageWidth, imageHeight)) {
            final BufferedImageHelper helperFrom = new BufferedImageHelper(imageFrom);
            
            // Randomizing input image (always the same).
            {
                final Random random = TestUtils.newRandom123456789L();
                for (int y = 0; y < imageHeight; y++) {
                    for (int x = 0; x < imageWidth; x++) {
                        final int argb32 = random.nextInt();
                        helperFrom.setNonPremulArgb32At(x, y, argb32);
                    }
                }
            }
            
            for (BufferedImage imageTo : BihTestUtils.newImageList_allImageType(imageWidth, imageHeight)) {
                final BufferedImageHelper helperTo = new BufferedImageHelper(imageTo);
                
                final BufferedImageHelper expectedHelperTo =
                    BihTestUtils.newIdenticalImageAndHelper(helperTo);
                
                BihTestUtils.copyImage_reference(
                    helperFrom,
                    0,
                    0,
                    expectedHelperTo,
                    0,
                    0,
                    imageWidth,
                    imageHeight);
                
                final Graphics g = imageTo.getGraphics();
                try {
                    g.drawImage(imageFrom, 0, 0, null);
                } finally {
                    g.dispose();
                }
                
                final int expectedMaxCptDelta_raw =
                    BihTestUtils.computeMaxCptDelta(
                        expectedHelperTo,
                        helperTo);
                // For whatever >= 2 we use 0xFF.
                final int expectedMaxCptDelta =
                    (expectedMaxCptDelta_raw >= 2
                    ? 0xFF : expectedMaxCptDelta_raw);
                
                final int actualMaxCptDelta =
                    BufferedImageHelper.getDrawImageMaxCptDelta(
                        imageFrom.getType(),
                        helperFrom.getPixelFormat(),
                        imageFrom.isAlphaPremultiplied(),
                        //
                        imageTo.getType(),
                        helperTo.getPixelFormat(),
                        imageTo.isAlphaPremultiplied());
                
                if (expectedMaxCptDelta != actualMaxCptDelta) {
                    final String srcStr = BihTestUtils.toStringImageKind(imageFrom);
                    final String dstStr = BihTestUtils.toStringImageKind(imageTo);
                    final String srcDstStr = srcStr + "->" + dstStr;
                    System.out.println(
                        srcDstStr
                        + " : expected max delta "
                        + expectedMaxCptDelta
                        + ", got "
                        + actualMaxCptDelta);
                }
                assertEquals(expectedMaxCptDelta, actualMaxCptDelta);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Uses small spans.
     * 
     * @return An image, for which color model can be avoided.
     */
    private static BufferedImage newImage() {
        return newImage(
            SMALL_WIDTH,
            SMALL_HEIGHT);
    }
    
    /**
     * @return An image, for which color model can be avoided.
     */
    private static BufferedImage newImage(int width, int height) {
        return new BufferedImage(
            width,
            height,
            BufferedImage.TYPE_INT_ARGB);
    }
    
    /**
     * Uses small spans.
     * 
     * @return Images of all BufferedImage types
     *         and all (BihPixelFormat,premul) types (which overlap a bit).
     */
    private static List<BufferedImage> newImageList() {
        return BihTestUtils.newImageList(SMALL_WIDTH, SMALL_HEIGHT);
    }
    
    private static int randomAlphaCptIndex(
        Random random,
        double looseIndexRangeProba) {
        int min = -1;
        int max = 3;
        if (random.nextDouble() < looseIndexRangeProba) {
            min--;
            max++;
        }
        return min + random.nextInt(max - min + 1);
    }
    
    private static int randomColorCptIndex(
        Random random,
        boolean gotAlpha,
        double looseIndexRangeProba) {
        int min = (gotAlpha ? 0 : 1);
        int max = 3;
        if (random.nextDouble() < looseIndexRangeProba) {
            min--;
            max++;
        }
        return min + random.nextInt(max - min + 1);
    }
    
    /*
     * 
     */

    private static int[] newBadPositionArr(int span) {
        return new int[] {
            Integer.MIN_VALUE,
            -1,
            span,
            Integer.MAX_VALUE,
        };
    }
    
    private static int[] newBadScanlineStrideArr(int width) {
        return new int[] {
            Integer.MIN_VALUE,
            -1,
            0,
            width - 1,
            // Not bad in itself, but too large for array,
            // and to test overflow handling in checks.
            Integer.MAX_VALUE,
        };
    }
    
    /**
     * @param span1 Span of image 1.
     * @param span2 Span of image 2.
     * @return Array of {pos1, pos2, span} triplets
     *         that don't fit into both images.
     */
    private static int[][] newBadRangeArr(
        int span1,
        int span2) {
        
        final int minSpan = Math.min(span1, span2);
        
        final List<int[]> retList = new ArrayList<>();
        
        final int[] crazyOrNotArr = new int[] {
            Integer.MIN_VALUE,
            -1,
            0, // not crazy
            minSpan + 1,
            Integer.MAX_VALUE};
        for (int pos1 : crazyOrNotArr) {
            for (int pos2 : crazyOrNotArr) {
                for (int span : crazyOrNotArr) {
                    if ((pos1|pos2|span) != 0) {
                        retList.add(new int[] {pos1, pos2, span});
                    }
                }
            }
        }
        retList.add(new int[] {1, 0, span1});
        retList.add(new int[] {0, 1, span2});
        retList.add(new int[] {span1, 0, 0});
        retList.add(new int[] {0, span2, 0});
        retList.add(new int[] {span1, 0, Integer.MAX_VALUE});
        retList.add(new int[] {0, span2, Integer.MAX_VALUE});
        retList.add(new int[] {span1 - 1, 0, 2});
        retList.add(new int[] {0, span2 - 1, 2});
        retList.add(new int[] {span1 - 1, 0, Integer.MAX_VALUE});
        retList.add(new int[] {0, span2 - 1, Integer.MAX_VALUE});
        return retList.toArray(new int[retList.size()][]);
    }
    
    /*
     * 
     */
    
    private static boolean isGray(int imageType) {
        return (imageType == BufferedImage.TYPE_USHORT_GRAY)
            || (imageType == BufferedImage.TYPE_BYTE_GRAY);
    }

    /*
     * 
     */
    
    private static void checkEqual(int expectedColor32, int actualColor32) {
        final String expected = Argb32.toString(expectedColor32);
        final String actual = Argb32.toString(actualColor32);
        if (DEBUG) {
            if (!expected.equals(actual)) {
                System.out.println("expected = " + expected);
                System.out.println("actual =   " + actual);
            }
        }
        assertEquals(expected, actual);
    }
    
    private static void checkCloseColor32(
        int expectedColor32,
        int actualColor32,
        int cptDeltaTol) {
        if (DEBUG
            && (BihTestUtils.getMaxCptDelta(
                expectedColor32,
                actualColor32) > cptDeltaTol)) {
            final String expected = Argb32.toString(expectedColor32);
            final String actual = Argb32.toString(actualColor32);
            System.out.println("cptDeltaTol = " + cptDeltaTol);
            System.out.println("expected = " + expected);
            System.out.println("actual =   " + actual);
        }
        final int ve1 = Argb32.getAlpha8(expectedColor32);
        final int va1 = Argb32.getAlpha8(actualColor32);
        final int ve2 = Argb32.getRed8(expectedColor32);
        final int va2 = Argb32.getRed8(actualColor32);
        final int ve3 = Argb32.getGreen8(expectedColor32);
        final int va3 = Argb32.getGreen8(actualColor32);
        final int ve4 = Argb32.getBlue8(expectedColor32);
        final int va4 = Argb32.getBlue8(actualColor32);
        TestCase.assertEquals(ve1, va1, cptDeltaTol);
        TestCase.assertEquals(ve2, va2, cptDeltaTol);
        TestCase.assertEquals(ve3, va3, cptDeltaTol);
        TestCase.assertEquals(ve4, va4, cptDeltaTol);
    }
    
    /*
     * 
     */
    
    /**
     * Useful for exceptions tests,
     * since the checks on lengths/spans are the same
     * for all image creation methods.
     */
    private static BufferedImage callNewImageWithIntArray_lengthsSpans(
        MyNewImageMethodType methodType,
        //
        int[] pixelArr,
        int scanlineStride,
        //
        int width,
        int height) {
        
        final BufferedImage ret;
        if (methodType == MyNewImageMethodType.WITH_IMAGE_TYPE) {
            ret = BufferedImageHelper.newBufferedImageWithIntArray(
                pixelArr,
                scanlineStride,
                //
                width,
                height,
                //
                BufferedImage.TYPE_INT_ARGB);
        } else if (methodType == MyNewImageMethodType.WITH_PIXEL_FORMAT) {
            ret = BufferedImageHelper.newBufferedImageWithIntArray(
                pixelArr,
                scanlineStride,
                //
                width,
                height,
                //
                BihPixelFormat.ARGB32,
                false);
        } else if (methodType == MyNewImageMethodType.WITH_CPT_INDEXES) {
            ret = BufferedImageHelper.newBufferedImageWithIntArray(
                pixelArr,
                scanlineStride,
                //
                width,
                height,
                //
                false,
                -1,
                //
                1,
                2,
                3);
        } else {
            throw new AssertionError();
        }
        return ret;
    }
    
    /*
     * 
     */
    
    /**
     * Useful for exceptions tests,
     * since the checks are the same for get and set.
     */
    private static void callGetOrSetPixelsInto(
        boolean isGetElseSet,
        BufferedImageHelper helper,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormat,
        boolean premul,
        int arrX,
        int arrY,
        //
        int imgX,
        int imgY,
        //
        int width,
        int height) {
        if (isGetElseSet) {
            callGetPixelsInto(
                helper,
                //
                imgX,
                imgY,
                //
                color32Arr,
                color32ArrScanlineStride,
                pixelFormat,
                premul,
                arrX,
                arrY,
                //
                width,
                height);
        } else {
            callSetPixelsFrom(
                helper,
                //
                color32Arr,
                color32ArrScanlineStride,
                pixelFormat,
                premul,
                arrX,
                arrY,
                //
                imgX,
                imgY,
                //
                width,
                height);
        }
    }
    
    /*
     * 
     */
    
    private static void callGetPixelsInto(
        BufferedImageHelper helper,
        //
        int srcX,
        int srcY,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatTo,
        boolean premulTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        if (DEBUG) {
            System.out.println("calling getPixelsInto():");
            System.out.println("srcX = " + srcX);
            System.out.println("srcY = " + srcY);
            System.out.println("color32Arr.length = "
                + ((color32Arr != null) ? color32Arr.length : 0));
            System.out.println("color32ArrScanlineStride = " + color32ArrScanlineStride);
            System.out.println("pixelFormatTo = " + pixelFormatTo);
            System.out.println("premulTo = " + premulTo);
            System.out.println("dstX = " + dstX);
            System.out.println("dstY = " + dstY);
            System.out.println("width = " + width);
            System.out.println("height = " + height);
        }
        
        helper.getPixelsInto(
            srcX,
            srcY,
            //
            color32Arr,
            color32ArrScanlineStride,
            pixelFormatTo,
            premulTo,
            dstX,
            dstY,
            //
            width,
            height);
    }
    
    private static void callSetPixelsFrom(
        BufferedImageHelper helper,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        BihPixelFormat pixelFormatFrom,
        boolean premulFrom,
        int srcX,
        int srcY,
        //
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        if (DEBUG) {
            System.out.println("calling setPixelsFrom():");
            System.out.println("color32Arr.length = "
                + ((color32Arr != null) ? color32Arr.length : 0));
            System.out.println("color32ArrScanlineStride = " + color32ArrScanlineStride);
            System.out.println("pixelFormatFrom = " + pixelFormatFrom);
            System.out.println("premulFrom = " + premulFrom);
            System.out.println("srcX = " + srcX);
            System.out.println("srcY = " + srcY);
            System.out.println("dstX = " + dstX);
            System.out.println("dstY = " + dstY);
            System.out.println("width = " + width);
            System.out.println("height = " + height);
        }
        
        helper.setPixelsFrom(
            color32Arr,
            color32ArrScanlineStride,
            pixelFormatFrom,
            premulFrom,
            srcX,
            srcY,
            //
            dstX,
            dstY,
            //
            width,
            height);
    }
    
    private static void callCopyImage(
        BufferedImageHelper helperFrom,
        int srcX,
        int srcY,
        //
        BufferedImageHelper helperTo,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        if (DEBUG) {
            System.out.println("calling copyImage():");
            System.out.println("helperFrom = " + helperFrom);
            System.out.println("srcX = " + srcX);
            System.out.println("srcY = " + srcY);
            System.out.println("helperTo = " + helperTo);
            System.out.println("dstX = " + dstX);
            System.out.println("dstY = " + dstY);
            System.out.println("width = " + width);
            System.out.println("height = " + height);
        }
        
        BufferedImageHelper.copyImage(
            helperFrom,
            srcX,
            srcY,
            //
            helperTo,
            dstX,
            dstY,
            //
            width,
            height);
    }
}
