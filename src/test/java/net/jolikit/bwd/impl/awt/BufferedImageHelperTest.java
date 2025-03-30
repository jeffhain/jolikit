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
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.impl.awt.BufferedImageHelper.BihPixelFormat;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.NbrsUtils;

public class BufferedImageHelperTest extends TestCase {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    /*
     * Large enough to get all kinds of alpha and color components,
     * and various kinds of (alpha,color) pairs.
     * Not too large because we test a lot of helper pairs.
     */
    
    private static final int BULK_METHODS_IMAGE_WIDTH = 64;
    private static final int BULK_METHODS_IMAGE_HEIGHT = 32;
    
    private static final int SMALL_WIDTH = 7;
    private static final int SMALL_HEIGHT = 5;
    
    /**
     * For binary, we compute a cpt8 delta depending
     * on actual vs expected white pixels ratio.
     * Here we tolerate 2 percents of wrong pixels.
     */
    private static final int CPT_DELTA_TOL_BIN = (int) (0.02 * 0xFF + 0.5);
    
    /**
     * For non-binary cases, a tolerance of zero is fine
     * due to very restriced usages of drawImage()
     * (and no usage at all for int array images
     * for which array direct use is allowed).
     */
    private static final int CPT_DELTA_TOL_OTHER = 0;
    
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
    
    /**
     * We filling output outer area with this semi opaque color,
     * which opaque version should be exact whatever the image type,
     * to check for no damage outside destination area
     * (we also randomize destination area,
     * to check that it won't combine with src).
     */
    private static final int OUTER_NON_PREMUL_ARGB32 = 0x80FFFFFF;
    
    private static final List<Integer> TYPE_INT_XXX_LIST =
        Collections.unmodifiableList(
            Arrays.asList(
                BufferedImage.TYPE_INT_ARGB,
                BufferedImage.TYPE_INT_ARGB_PRE,
                BufferedImage.TYPE_INT_RGB,
                BufferedImage.TYPE_INT_BGR));
    
    //--------------------------------------------------------------------------
    // CONSTRUCTORS
    //--------------------------------------------------------------------------
    
    public BufferedImageHelperTest() {
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public void test_BufferedImageHelper_BufferedImage() {
        for (BufferedImage image : newImageListWithStrides()) {
            final BufferedImageHelper helper = new BufferedImageHelper(image);
            // Both true by default.
            assertTrue(helper.isColorModelAvoidingAllowed());
            assertTrue(helper.isArrayDirectUseAllowed());
        }
    }
    
    public void test_BufferedImageHelper_BufferedImage_2boolean() {
        for (BufferedImage image : newImageListWithStrides()) {
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
    
    public void test_BufferedImageHelper_BihPixelFormat() {
        final boolean mustTestContructor = true;
        this.test_newBufferedImageWithIntArrayOrConstructor_BihPixelFormat_xxx(
            mustTestContructor);
    }
    
    /*
     * 
     */
    
    public void test_duplicate() {
        for (BufferedImage image : newImageListWithStrides()) {
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
    
    public void test_imageDataGetters() {
        for (BufferedImage image : newImageListWithStrides()) {
            final BufferedImageHelper helper = new BufferedImageHelper(image);
            
            assertSame(image, helper.getImage());
            assertEquals(image.getType(), helper.getImageType());
            assertEquals(image.getWidth(), helper.getWidth());
            assertEquals(image.getHeight(), helper.getHeight());
            assertEquals(image.getTransparency() == Transparency.OPAQUE, helper.isOpaque());
            assertEquals(image.getColorModel().hasAlpha(), helper.hasAlpha());
            assertEquals(image.isAlphaPremultiplied(), helper.isAlphaPremultiplied());
        }
    }
    
    public void test_getPixelFormat() {
        for (BufferedImage image : newImageListWithStrides()) {
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
        
        for (TestImageTypeEnum imageTypeEnum : TestImageTypeEnum.values()) {
            final BihPixelFormat pixelFormat = imageTypeEnum.pixelFormat();
            final BufferedImage image = BihTestUtils.newImage(
                imageWidth,
                imageHeight,
                imageTypeEnum);
            
            final BihPixelFormat actualPixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            
            // Possibly null.
            assertEquals(pixelFormat, actualPixelFormat);
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
        for (TestImageTypeEnum imageTypeEnum : TestImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
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
        final boolean mustTestContructor = false;
        this.test_newBufferedImageWithIntArrayOrConstructor_BihPixelFormat_xxx(mustTestContructor);
    }
    
    public void test_newBufferedImageWithIntArrayOrConstructor_BihPixelFormat_xxx(
        boolean mustTestContructor) {
        
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        
        test_newBufferedImageWithIntArray_exceptions_lengthsSpans_xxx(
            MyNewImageMethodType.WITH_PIXEL_FORMAT);
        
        /*
         * Null pixel format.
         */
        try {
            if (mustTestContructor) {
                new BufferedImageHelper(
                    null,
                    imageWidth,
                    imageWidth,
                    imageHeight,
                    null,
                    false).hashCode(); // hashCode() anti-warning
            } else {
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    imageWidth,
                    imageWidth,
                    imageHeight,
                    null,
                    false);
            }
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
                    if (mustTestContructor) {
                        new BufferedImageHelper(
                            null,
                            imageWidth,
                            imageWidth,
                            imageHeight,
                            pixelFormat,
                            true).hashCode(); // hashCode() anti-warning
                    } else {
                        BufferedImageHelper.newBufferedImageWithIntArray(
                            null,
                            imageWidth,
                            imageWidth,
                            imageHeight,
                            pixelFormat,
                            true);
                    }
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
                final BufferedImage image;
                if (mustTestContructor) {
                    image = new BufferedImageHelper(
                        null,
                        scanlineStride,
                        imageWidth,
                        imageHeight,
                        pixelFormat,
                        premul).getImage();
                } else {
                    image = BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        scanlineStride,
                        imageWidth,
                        imageHeight,
                        pixelFormat,
                        premul);
                }
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
                    final BufferedImage image;
                    if (mustTestContructor) {
                        image = new BufferedImageHelper(
                            pixelArr,
                            scanlineStride,
                            imageWidth,
                            imageHeight,
                            pixelFormat,
                            premul).getImage();
                    } else {
                        image = BufferedImageHelper.newBufferedImageWithIntArray(
                            pixelArr,
                            scanlineStride,
                            imageWidth,
                            imageHeight,
                            pixelFormat,
                            premul);
                    }
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
            final Random random = BihTestUtils.newRandom();
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
        for (BufferedImage image : BihTestUtils.newImageListOfDimWithStrides(imageWidth, imageHeight)) {
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
    
    public void test_getXxxArgb32At_normal() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageListOfDimWithStrides(imageWidth, imageHeight)) {
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final BihPixelFormat pixelFormat = helper.getPixelFormat();
                final int scanlineStride = helper.getScanlineStride();
                
                final int x = 1;
                final int y = 2;
                int expectedNonPremulArgb32;
                if (pixelFormat != null) {
                    // Components values high enough to preserve bijectivity
                    // when converting between premul and non premul.
                    expectedNonPremulArgb32 = 0xA76543B1;
                    if (!pixelFormat.hasAlpha()) {
                        expectedNonPremulArgb32 = Argb32.toOpaque(expectedNonPremulArgb32);
                    }
                } else {
                    // Works for all image types.
                    expectedNonPremulArgb32 = 0xFFFFFFFF;
                }
                final int expectedPremulArgb32 =
                    BindingColorUtils.toPremulAxyz32(
                        expectedNonPremulArgb32);
                
                if (pixelFormat != null) {
                    final int argb32 =
                        (imagePremul ? expectedPremulArgb32 : expectedNonPremulArgb32);
                    final int pixel = pixelFormat.toPixelFromArgb32(argb32);
                    final int index = y * scanlineStride + x;
                    final int[] pixelArr = BufferedImageHelper.getIntArray(image);
                    pixelArr[index] = pixel;
                } else {
                    if (helper.isColorModelAvoidedForSinglePixelMethods()) {
                        // Assuming setter works.
                        helper.setNonPremulArgb32At(x, y, expectedNonPremulArgb32);
                    } else {
                        image.setRGB(x, y, expectedNonPremulArgb32);
                    }
                }
                
                final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                BihTestUtils.checkColorEquals(expectedNonPremulArgb32, actualNonPremulArgb32);
                
                final int actualPremulArgb32 = helper.getPremulArgb32At(x, y);
                BihTestUtils.checkColorEquals(expectedPremulArgb32, actualPremulArgb32);
            }
        }
    }
    
    public void test_setXxxArgb32At_normal() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageListOfDimWithStrides(imageWidth, imageHeight)) {
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final BihPixelFormat pixelFormat = helper.getPixelFormat();
                final int scanlineStride = helper.getScanlineStride();
                
                final int x = 1;
                final int y = 2;
                int nonPremulArgb32;
                if (pixelFormat != null) {
                    // Components values high enough to preserve bijectivity
                    // when converting between premul and non premul.
                    nonPremulArgb32 = 0xA76543B1;
                    if (!pixelFormat.hasAlpha()) {
                        nonPremulArgb32 = Argb32.toOpaque(nonPremulArgb32);
                    }
                } else {
                    // Works for all image types.
                    nonPremulArgb32 = 0xFFFFFFFF;
                }
                final int premulArgb32 = BindingColorUtils.toPremulAxyz32(nonPremulArgb32);
                
                final int index = y * scanlineStride + x;
                for (boolean testPremulSetter : new boolean[] {false, true}) {
                    if (testPremulSetter) {
                        helper.setPremulArgb32At(x, y, premulArgb32);
                    } else {
                        helper.setNonPremulArgb32At(x, y, nonPremulArgb32);
                    }
                    if (pixelFormat != null) {
                        final int[] pixelArr = BufferedImageHelper.getIntArray(image);
                        final int actualPixel = pixelArr[index];
                        final int expectedPixel =
                            pixelFormat.toPixelFromArgb32(
                                (imagePremul ? premulArgb32 : nonPremulArgb32));
                        BihTestUtils.checkColorEquals(expectedPixel, actualPixel);
                    } else {
                        final int actualNonPremulArgb32;
                        if (helper.isColorModelAvoidedForSinglePixelMethods()) {
                            // Assuming getter works.
                            actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                        } else {
                            actualNonPremulArgb32 = image.getRGB(x, y);
                        }
                        BihTestUtils.checkColorEquals(nonPremulArgb32, actualNonPremulArgb32);
                    }
                }
            }
        }
    }
    
    public void test_getXxxArgb32At_allImageType() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (TestImageTypeEnum imageTypeEnum : TestImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            if (imageType == BufferedImage.TYPE_CUSTOM) {
                if ((imageTypeEnum == TestImageTypeEnum.TYPE_CUSTOM_INT_ABGR)
                    || (imageTypeEnum == TestImageTypeEnum.TYPE_CUSTOM_INT_ABGR_PRE)) {
                    // ok
                } else {
                    // Not bothering with these.
                    continue;
                }
            }
            
            final BufferedImage image =
                BihTestUtils.newImage(
                    imageWidth,
                    imageHeight,
                    imageTypeEnum);
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean cma = helper.isColorModelAvoidedForSinglePixelMethods();
                
                final int x = 1;
                final int y = 2;
                
                final List<Integer> imageRgbSetList;
                final List<Integer> expectedHelperRgbGetList;
                if ((imageType == BufferedImage.TYPE_4BYTE_ABGR)
                    || (imageType == BufferedImage.TYPE_INT_ARGB)
                    || (imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE)
                    || (imageType == BufferedImage.TYPE_INT_ARGB_PRE)
                    || (imageTypeEnum == TestImageTypeEnum.TYPE_CUSTOM_INT_ABGR)
                    || (imageTypeEnum == TestImageTypeEnum.TYPE_CUSTOM_INT_ABGR_PRE)) {
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
                    BihTestUtils.checkColorEquals(expectedHelperRgbGet, actualHelperRgbGet);
                }
            }
        }
    }
    
    public void test_setXxxArgb32At_allImageType() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (TestImageTypeEnum imageTypeEnum : TestImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            final BufferedImage image = BihTestUtils.newImage(
                imageWidth,
                imageHeight,
                imageTypeEnum);
            final boolean hasAlpha = image.getColorModel().hasAlpha();
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean cma = helper.isColorModelAvoidedForSinglePixelMethods();
                
                final int x = 1;
                final int y = 2;
                
                final List<Integer> helperRgbSetList;
                final List<Integer> expectedImageRgbGetList;
                if ((imageType == BufferedImage.TYPE_4BYTE_ABGR)
                    || (imageType == BufferedImage.TYPE_INT_ARGB)
                    || (imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE)
                    || (imageType == BufferedImage.TYPE_INT_ARGB_PRE)
                    || ((imageType == BufferedImage.TYPE_CUSTOM) && hasAlpha)) {
                    helperRgbSetList = Arrays.asList(
                        0x00000000,
                        0x80804020,
                        0xFF818387,
                        0xFFFFFFFF);
                    expectedImageRgbGetList = helperRgbSetList;
                } else if ((imageType == BufferedImage.TYPE_3BYTE_BGR)
                    || (imageType == BufferedImage.TYPE_INT_BGR)
                    || (imageType == BufferedImage.TYPE_INT_RGB)
                    || ((imageType == BufferedImage.TYPE_CUSTOM) && (!hasAlpha))) {
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
                    BihTestUtils.checkColorEquals(expectedImageRgbGet, actualImageRgbGet);
                }
            }
        }
    }
    
    public void test_setXxxArgb32At_getXxxArgb32At_deltasOkWithCma_allImageType() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (TestImageTypeEnum imageTypeEnum : TestImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            final BufferedImage image = BihTestUtils.newImage(
                imageWidth,
                imageHeight,
                imageTypeEnum);
            final boolean hasAlpha = image.getColorModel().hasAlpha();
            
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
                    || (imageType == BufferedImage.TYPE_INT_ARGB_PRE)
                    || ((imageType == BufferedImage.TYPE_CUSTOM) && hasAlpha)) {
                    helperRgbSetList = Arrays.asList(
                        0x00000000,
                        0x80804020,
                        0xFF818387,
                        0xFFFFFFFF);
                    expectedHelperRgbGetList = helperRgbSetList;
                } else if ((imageType == BufferedImage.TYPE_3BYTE_BGR)
                    || (imageType == BufferedImage.TYPE_INT_BGR)
                    || (imageType == BufferedImage.TYPE_INT_RGB)
                    || ((imageType == BufferedImage.TYPE_CUSTOM) && (!hasAlpha))) {
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
                    BihTestUtils.checkColorEquals(expectedHelperRgbGet, actualHelperRgbGet);
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
        for (BufferedImage image : BihTestUtils.newImageListOfDimWithStrides(imageWidth, imageHeight)) {
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
    
    public void test_clearRect_normal() {
        final int imageWidth = SMALL_WIDTH;
        final int imageHeight = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageListOfDimWithStrides(imageWidth, imageHeight)) {
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                for (int k = 0; k <= 0xF; k++) {
                    final boolean leftHit = ((k & 1) != 0);
                    final boolean rightHit = ((k & 2) != 0);
                    final boolean topHit = ((k & 4) != 0);
                    final boolean bottomHit = ((k & 8) != 0);
                    test_clearRect_xxx(
                        helper,
                        leftHit,
                        rightHit,
                        topHit,
                        bottomHit);
                }
            }
        }
    }
    
    public void test_clearRect_xxx(
        BufferedImageHelper helper,
        boolean leftHit,
        boolean rightHit,
        boolean topHit,
        boolean bottomHit) {
        
        // Always the same random initial state.
        final Random random = BihTestUtils.newRandom();
        BihTestUtils.randomizeHelper(
            random,
            helper,
            false);
        
        final int iw = helper.getWidth();
        final int ih = helper.getHeight();
        
        final BufferedImageHelper copyHelper =
            BihTestUtils.newSameTypeImageAndHelper(helper);
        BufferedImageHelper.copyImage(helper, 0, 0, copyHelper, 0, 0, iw, ih);
        
        final int imageType = helper.getImageType();
        final BihPixelFormat pixelFormat = helper.getPixelFormat();
        final boolean hasBihPixelFormat = (pixelFormat != null);
        
        final int rx = (leftHit ? 0 : 1);
        final int ry = (topHit ? 0 : 1);
        final int rw = (iw - rx) - (rightHit ? 0 : 1);
        final int rh = (ih - ry) - (bottomHit ? 0 : 1);
        if (DEBUG) {
            System.out.println();
            System.out.println("imageType = " + imageType);
            System.out.println("pixelFormat = " + pixelFormat);
            System.out.println("rx = " + rx);
            System.out.println("ry = " + ry);
            System.out.println("rw = " + rw);
            System.out.println("rh = " + rh);
        }

        for (boolean flipFlop : new boolean[] {false, true}) {
            int nonPremulArgb32;
            int argb32;
            boolean premul;
            if (hasBihPixelFormat
                || (imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE)) {
                /*
                 * flipFlop: alternating between non-premul and premul.
                 */
                nonPremulArgb32 = 0xC0806040;
                if (!helper.hasAlpha()) {
                    nonPremulArgb32 = Argb32.toOpaque(nonPremulArgb32);
                }
                final int premulArgb32 =
                    BindingColorUtils.toPremulAxyz32(nonPremulArgb32);
                // Must be bijective.
                BihTestUtils.checkColorEquals(
                    nonPremulArgb32,
                    BindingColorUtils.toNonPremulAxyz32(premulArgb32));
                
                premul = flipFlop;
                argb32 = (premul ? premulArgb32 : nonPremulArgb32);
            } else {
                /*
                 * flipFlop: alternating between two colors.
                 */
                if (imageType == BufferedImage.TYPE_BYTE_BINARY) {
                    nonPremulArgb32 = (flipFlop ? 0xFFFFFFFF : 0xFF000000);
                } else if ((imageType == BufferedImage.TYPE_BYTE_GRAY)
                    || (imageType == BufferedImage.TYPE_USHORT_GRAY)) {
                    nonPremulArgb32 = (flipFlop ? 0xFF888888 : 0xFF666666);
                } else if (imageType == BufferedImage.TYPE_BYTE_INDEXED) {
                    nonPremulArgb32 = (flipFlop ? 0xFF00FF00 : 0xFFFF00FF);
                } else {
                    /*
                     * Works for all remaining types.
                     */
                    nonPremulArgb32 = (flipFlop ? 0xFF42FF21 : 0xFFFF10FF);
                }
                premul = false;
                argb32 = nonPremulArgb32;
            }
            
            /*
             * Clearing.
             */
            
            helper.clearRect(rx, ry, rw, rh, argb32, premul);
            
            // Rectangle cleared.
            for (int py = ry; py < ry + rh; py++) {
                for (int px = rx; px < rx + rw; px++) {
                    final int actualArgb32 = helper.getNonPremulArgb32At(px, py);
                    BihTestUtils.checkColorEquals(nonPremulArgb32, actualArgb32);
                }
            }
            
            // Surroundings not cleared.
            for (int py : new int[] {ry - 1, ry + rh}) {
                for (int px : new int[] {rx - 1, rx + rw}) {
                    if ((px >= 0)
                        && (py >= 0)
                        && (px < iw)
                        && (py < ih)) {
                        final int expectedNonPremulArgb32 = copyHelper.getNonPremulArgb32At(px, py);
                        final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(px, py);
                        BihTestUtils.checkColorEquals(
                            expectedNonPremulArgb32,
                            actualNonPremulArgb32);
                    }
                }
            }
            
            /*
             * Clearing again.
             */
            
            helper.clearRect(rx, ry, rw, rh, argb32, premul);
            
            // Still same color: equivalent to set() not draw().
            for (int py = ry; py < ry + rh; py++) {
                for (int px = rx; px < rx + rw; px++) {
                    final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(px, py);
                    BihTestUtils.checkColorEquals(nonPremulArgb32, actualNonPremulArgb32);
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
        for (BufferedImage image : BihTestUtils.newImageListOfDimWithStrides(imageWidth, imageHeight)) {
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
    
    public void test_getPixelsInto_fullRect() {
        final boolean srcRectElseFullRect = false;
        this.test_getPixelsInto_xxx(srcRectElseFullRect);
    }
    
    public void test_getPixelsInto_srcRect() {
        final boolean srcRectElseFullRect = true;
        this.test_getPixelsInto_xxx(srcRectElseFullRect);
    }
    
    public void test_getPixelsInto_xxx(boolean srcRectElseFullRect) {
        
        final int srcImageWidth = BULK_METHODS_IMAGE_WIDTH;
        final int srcImageHeight = BULK_METHODS_IMAGE_HEIGHT;
        final int dstImageWidth = srcImageWidth + (srcRectElseFullRect ? 3*0+1 : 0);
        final int dstImageHeight = srcImageHeight + (srcRectElseFullRect ? 5*0+1 : 0);
        
        final Random random = BihTestUtils.newRandom();
        
        for (int strideBonus : new int[] {0, 1}) {
            
            final int dstScanlineStride = dstImageWidth + strideBonus;
            
            final int[] expectedDstArr =
                new int[dstScanlineStride * dstImageHeight];
            final int[] actualDstArr =
                new int[dstScanlineStride * dstImageHeight];
            
            for (BufferedImage srcImage : BihTestUtils.newImageListOfDimWithStrides(srcImageWidth, srcImageHeight)) {
                for (BufferedImageHelper srcHelper : BihTestUtils.newHelperList(srcImage)) {
                    
                    final boolean srcCmaAllowed =
                        srcHelper.isColorModelAvoidingAllowed();
                    
                    // Can be null.
                    final BihPixelFormat srcPixelFormat =
                        srcHelper.getPixelFormat();
                    final boolean srcPremul = srcImage.isAlphaPremultiplied();
                    // Can be null.
                    final TestImageTypeEnum srcImageTypeEnum =
                        TestImageTypeEnum.enumByType().get(srcImage.getType());
                    
                    BihTestUtils.randomizeHelper(
                        random,
                        srcHelper,
                        false);
                    
                    for (BihPixelFormat dstPixelFormat : BihPixelFormat.values()) {
                        for (boolean dstPremul : BihTestUtils.newPremulArr(dstPixelFormat)) {
                            if (DEBUG) {
                                System.out.println();
                                System.out.println("srcImageWidth = " + srcImageWidth);
                                System.out.println("srcImageHeight = " + srcImageHeight);
                                System.out.println("srcPixelFormat = " + srcPixelFormat);
                                System.out.println("srcPremul = " + srcPremul);
                                System.out.println("srcImageTypeEnum = " + srcImageTypeEnum);
                                System.out.println("srcCmaAllowed = " + srcCmaAllowed);
                                System.out.println("dstScanlineStride = " + dstScanlineStride);
                                System.out.println("dstImageWidth = " + dstImageWidth);
                                System.out.println("dstImageHeight = " + dstImageHeight);
                                System.out.println("dstPixelFormat = " + dstPixelFormat);
                                System.out.println("dstPremul = " + dstPremul);
                            }
                            
                            final int srcX;
                            final int srcY;
                            final int dstX;
                            final int dstY;
                            final int width;
                            final int height;
                            if (srcRectElseFullRect) {
                                srcX = BihTestUtils.randomPosOrZero(random, srcImageWidth);
                                srcY = BihTestUtils.randomPosOrZero(random, srcImageHeight);
                                dstX = BihTestUtils.randomPosOrZero(random, dstImageWidth);
                                dstY = BihTestUtils.randomPosOrZero(random, dstImageHeight);
                                // Zero width/imageHeight accepted.
                                width = random.nextInt(
                                    Math.min(srcImageWidth - srcX, dstImageWidth - dstX) + 1);
                                height = random.nextInt(
                                    Math.min(srcImageHeight - srcY, dstImageHeight - dstY) + 1);
                            } else {
                                srcX = 0;
                                srcY = 0;
                                dstX = 0;
                                dstY = 0;
                                width = srcImageWidth;
                                height = srcImageHeight;
                            }
                            
                            final int outerPixel;
                            {
                                int initialArgb32 = OUTER_NON_PREMUL_ARGB32;
                                if (dstPremul) {
                                    initialArgb32 =
                                        BindingColorUtils.toPremulAxyz32(
                                            initialArgb32);
                                }
                                outerPixel =
                                    dstPixelFormat.toPixelFromArgb32(
                                        initialArgb32);
                                // Fill for surrounding.
                                Arrays.fill(actualDstArr, outerPixel);
                                // Randomizing for destination area.
                                BihTestUtils.randomizeArray(
                                    random,
                                    //
                                    actualDstArr,
                                    dstScanlineStride,
                                    dstPixelFormat,
                                    dstPremul,
                                    //
                                    dstX,
                                    dstY,
                                    width,
                                    height,
                                    //
                                    false);
                                // Same for expected.
                                BihTestUtils.copyArray(
                                    actualDstArr,
                                    dstScanlineStride,
                                    expectedDstArr,
                                    dstScanlineStride,
                                    dstImageWidth,
                                    dstImageHeight);
                            }
                            
                            BihTestUtils.getPixelsInto_reference(
                                srcHelper,
                                //
                                srcX,
                                srcY,
                                //
                                expectedDstArr,
                                dstScanlineStride,
                                dstPixelFormat,
                                dstPremul,
                                dstX,
                                dstY,
                                //
                                width,
                                height);
                            
                            callGetPixelsInto(
                                srcHelper,
                                //
                                srcX,
                                srcY,
                                //
                                actualDstArr,
                                dstScanlineStride,
                                dstPixelFormat,
                                dstPremul,
                                dstX,
                                dstY,
                                //
                                width,
                                height);
                            
                            // Checking no damage to pixels outside the area.
                            for (int i = 0; i < actualDstArr.length; i++) {
                                final int x = i % dstScanlineStride;
                                final int y = i / dstScanlineStride;
                                if ((x < dstX)
                                    || (y < dstY)
                                    || (x >= dstX + width)
                                    || (y >= dstY + height)) {
                                    final int actualPixel = actualDstArr[i];
                                    BihTestUtils.checkColorEquals(outerPixel, actualPixel);
                                }
                            }
                            
                            // Checking pixels within the area.
                            if ((width != 0) && (height != 0)) {
                                final BufferedImage expectedDstImage =
                                    BufferedImageHelper.newBufferedImageWithIntArray(
                                        expectedDstArr,
                                        dstScanlineStride,
                                        //
                                        dstImageWidth,
                                        dstImageHeight,
                                        //
                                        dstPixelFormat,
                                        dstPremul);
                                final BufferedImage actualDstImage =
                                    BufferedImageHelper.newBufferedImageWithIntArray(
                                        actualDstArr,
                                        dstScanlineStride,
                                        //
                                        dstImageWidth,
                                        dstImageHeight,
                                        //
                                        dstPixelFormat,
                                        dstPremul);
                                final BufferedImageHelper expectedDstHelper =
                                    new BufferedImageHelper(expectedDstImage);
                                final BufferedImageHelper actualDstHelper =
                                    new BufferedImageHelper(actualDstImage);
                                BihTestUtils.checkImageResult(
                                    srcHelper,
                                    srcX,
                                    srcY,
                                    //
                                    expectedDstHelper,
                                    //
                                    actualDstHelper,
                                    //
                                    dstX,
                                    dstY,
                                    width,
                                    height,
                                    //
                                    CPT_DELTA_TOL_BIN,
                                    CPT_DELTA_TOL_OTHER);
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
    
    public void test_setPixelsFrom_fullRect() {
        final boolean srcRectElseFullRect = false;
        this.test_setPixelsFrom_xxx(srcRectElseFullRect);
    }
    
    public void test_setPixelsFrom_srcRect() {
        final boolean srcRectElseFullRect = true;
        this.test_setPixelsFrom_xxx(srcRectElseFullRect);
    }
    
    public void test_setPixelsFrom_xxx(boolean srcRectElseFullRect) {
        
        final int srcImageWidth = BULK_METHODS_IMAGE_WIDTH;
        final int srcImageHeight = BULK_METHODS_IMAGE_HEIGHT;
        final int dstImageWidth = srcImageWidth + (srcRectElseFullRect ? 3 : 0);
        final int dstImageHeight = srcImageHeight + (srcRectElseFullRect ? 5 : 0);
        
        final Random random = BihTestUtils.newRandom();
        
        for (int strideBonus : new int[] {0, 1}) {
            
            final int srcScanlineStride = srcImageWidth + strideBonus;
            final int[] srcArr =
                new int[srcScanlineStride * srcImageHeight];
            
            for (BufferedImage dstImage : BihTestUtils.newImageListOfDimWithStrides(dstImageWidth, dstImageHeight)) {
                for (BufferedImageHelper dstHelper : BihTestUtils.newHelperList(dstImage)) {
                    
                    final boolean dstCmaAllowed =
                        dstHelper.isColorModelAvoidingAllowed();
                    
                    // Can be null.
                    final BihPixelFormat dstPixelFormat =
                        dstHelper.getPixelFormat();
                    final boolean dstPremul = dstImage.isAlphaPremultiplied();
                    // Can be null.
                    final TestImageTypeEnum dstImageTypeEnum =
                        TestImageTypeEnum.enumByType().get(dstImage.getType());
                    
                    final BufferedImageHelper expectedDstHelper =
                        BihTestUtils.newSameTypeImageAndHelper(dstHelper);
                    
                    for (BihPixelFormat srcPixelFormat : BihPixelFormat.values()) {
                        for (boolean srcPremul : BihTestUtils.newPremulArr(srcPixelFormat)) {
                            if (DEBUG) {
                                System.out.println();
                                System.out.println("srcScanlineStride = " + srcScanlineStride);
                                System.out.println("srcImageWidth = " + srcImageWidth);
                                System.out.println("srcImageHeight = " + srcImageHeight);
                                System.out.println("srcPixelFormat = " + srcPixelFormat);
                                System.out.println("srcPremul = " + srcPremul);
                                System.out.println("dstImageWidth = " + dstImageWidth);
                                System.out.println("dstImageHeight = " + dstImageHeight);
                                System.out.println("dstPixelFormat = " + dstPixelFormat);
                                System.out.println("dstPremul = " + dstPremul);
                                System.out.println("dstImageTypeEnum = " + dstImageTypeEnum);
                                System.out.println("dstCmaAllowed = " + dstCmaAllowed);
                            }
                            
                            final int srcX;
                            final int srcY;
                            final int dstX;
                            final int dstY;
                            final int width;
                            final int height;
                            if (srcRectElseFullRect) {
                                srcX = BihTestUtils.randomPosOrZero(random, srcImageWidth);
                                srcY = BihTestUtils.randomPosOrZero(random, srcImageHeight);
                                dstX = BihTestUtils.randomPosOrZero(random, dstImageWidth);
                                dstY = BihTestUtils.randomPosOrZero(random, dstImageHeight);
                                // Zero width/imageHeight accepted.
                                width = random.nextInt(
                                    Math.min(srcImageWidth - srcX, dstImageWidth - dstX) + 1);
                                height = random.nextInt(
                                    Math.min(srcImageHeight - srcY, dstImageHeight - dstY) + 1);
                            } else {
                                srcX = 0;
                                srcY = 0;
                                dstX = 0;
                                dstY = 0;
                                width = srcImageWidth;
                                height = srcImageHeight;
                            }
                            
                            // Randomizing input array.
                            final boolean opaque = !srcPixelFormat.hasAlpha();
                            for (int i = 0; i < srcArr.length; i++) {
                                int argb32 = BihTestUtils.randomArgb32(random, opaque);
                                if (srcPremul) {
                                    argb32 = BindingColorUtils.toPremulAxyz32(argb32);
                                }
                                final int pixel = srcPixelFormat.toPixelFromArgb32(argb32);
                                srcArr[i] = pixel;
                            }
                            
                            final int outerNonPremulArgb32;
                            {
                                int argb32 = OUTER_NON_PREMUL_ARGB32;
                                if (dstHelper.isOpaque()) {
                                    argb32 = Argb32.toOpaque(argb32);
                                }
                                outerNonPremulArgb32 = argb32;
                                // Fill for surrounding.
                                BihTestUtils.fillHelperWithNonPremulArgb32(
                                    dstHelper,
                                    outerNonPremulArgb32);
                                // Randomizing for destination area.
                                BihTestUtils.randomizeHelper(
                                    random,
                                    dstHelper,
                                    dstX,
                                    dstY,
                                    width,
                                    height,
                                    false);
                                // Same for expected.
                                BihTestUtils.copyHelper(
                                    dstHelper,
                                    expectedDstHelper);
                            }
                            
                            BihTestUtils.setPixelsFrom_reference(
                                expectedDstHelper,
                                //
                                srcArr,
                                srcScanlineStride,
                                srcPixelFormat,
                                srcPremul,
                                srcX,
                                srcY,
                                //
                                dstX,
                                dstY,
                                //
                                width,
                                height);
                            
                            callSetPixelsFrom(
                                dstHelper,
                                //
                                srcArr,
                                srcScanlineStride,
                                srcPixelFormat,
                                srcPremul,
                                srcX,
                                srcY,
                                //
                                dstX,
                                dstY,
                                //
                                width,
                                height);
                            
                            // Checking no damage to pixels outside the area.
                            for (int y = 0; y < dstImageHeight; y++) {
                                for (int x = 0; x < dstImageWidth; x++) {
                                    if ((x < dstX)
                                        || (y < dstY)
                                        || (x >= dstX + width)
                                        || (y >= dstY + height)) {
                                        final int actualNonPremulArgb32 =
                                            dstHelper.getNonPremulArgb32At(x, y);
                                        BihTestUtils.checkColorEquals(
                                            outerNonPremulArgb32,
                                            actualNonPremulArgb32);
                                    }
                                }
                            }
                            
                            // Checking pixels within the area.
                            if ((width != 0) && (height != 0)) {
                                final BufferedImage srcImage =
                                    BufferedImageHelper.newBufferedImageWithIntArray(
                                        srcArr,
                                        srcScanlineStride,
                                        //
                                        srcImageWidth,
                                        srcImageHeight,
                                        //
                                        srcPixelFormat,
                                        srcPremul);
                                final BufferedImageHelper srcHelper =
                                    new BufferedImageHelper(srcImage);
                                BihTestUtils.checkImageResult(
                                    srcHelper,
                                    srcX,
                                    srcY,
                                    //
                                    expectedDstHelper,
                                    //
                                    dstHelper,
                                    //
                                    dstX,
                                    dstY,
                                    width,
                                    height,
                                    //
                                    CPT_DELTA_TOL_BIN,
                                    CPT_DELTA_TOL_OTHER);
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
                final BufferedImage srcImage =
                    BihTestUtils.newImageArgb(srcWidth, srcHeight);
                final BufferedImageHelper srcHelper =
                    new BufferedImageHelper(srcImage);
                for (int dstWidth : new int[] {30,31}) {
                    for (int dstHeight : new int[] {20,21}) {
                        final BufferedImage dstImage =
                            BihTestUtils.newImageArgb(dstWidth, dstHeight);
                        final BufferedImageHelper dstHelper =
                            new BufferedImageHelper(dstImage);
                        test_copyImage_exceptions_xxx(
                            srcHelper,
                            dstHelper);
                    }
                }
            }
        }
    }
    
    public void test_copyImage_exceptions_xxx(
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        final BufferedImage dstImage = dstHelper.getImage();
        
        final int srcWidth = srcImage.getWidth();
        final int srcHeight = srcImage.getHeight();
        
        final int dstWidth = dstImage.getWidth();
        final int dstHeight = dstImage.getHeight();
        
        final int minWidth = Math.min(srcWidth, dstWidth);
        final int minHeight = Math.min(srcHeight, dstHeight);
        
        /*
         * Null helper.
         */
        try {
            callCopyImage(
                srcHelper,
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
                dstHelper,
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
                    srcHelper,
                    srcX,
                    0,
                    dstHelper,
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
                    srcHelper,
                    0,
                    srcY,
                    dstHelper,
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
        
        final int imageWidth = BULK_METHODS_IMAGE_WIDTH;
        final int imageHeight = BULK_METHODS_IMAGE_HEIGHT;
        
        final Random random = BihTestUtils.newRandom();
        
        for (BufferedImage srcImage : BihTestUtils.newImageListOfDimWithStrides(imageWidth, imageHeight)) {
            for (BufferedImageHelper srcHelper : BihTestUtils.newHelperList(srcImage)) {
                
                BihTestUtils.randomizeHelper(
                    random,
                    srcHelper,
                    false);
                
                // When not doing full copies,
                // using different image width and height.
                final int spanDelta = (srcDstRectElseFullRect ? 1 : 0);
                
                final List<BufferedImage> dstImageList = BihTestUtils.newImageListOfDimWithStrides(
                    imageWidth + spanDelta,
                    imageHeight - spanDelta);
                for (BufferedImage dstImage : dstImageList) {
                    for (BufferedImageHelper dstHelper : BihTestUtils.newHelperList(dstImage)) {
                        test_copyImage_xxx_withHelpers(
                            random,
                            srcDstRectElseFullRect,
                            srcHelper,
                            dstHelper);
                    }
                }
            }
        }
    }
    
    public void test_copyImage_xxx_withHelpers(
        Random random,
        boolean srcDstRectElseFullRect,
        BufferedImageHelper srcHelper,
        BufferedImageHelper dstHelper) {
        
        final BufferedImage srcImage = srcHelper.getImage();
        final BufferedImage dstImage = dstHelper.getImage();
        
        final int srcImageWidth = srcImage.getWidth();
        final int srcImageHeight = srcImage.getHeight();
        
        final int dstImageWidth = dstImage.getWidth();
        final int dstImageHeight = dstImage.getHeight();
        
        final int minImageWidth = Math.min(srcImageWidth, dstImageWidth);
        final int minImageHeight = Math.min(srcImageHeight, dstImageHeight);
        
        // Can be null.
        final TestImageTypeEnum srcImageTypeEnum =
            TestImageTypeEnum.enumByType().get(srcImage.getType());
        // Can be null.
        final BihPixelFormat srcPixelFormat =
            srcHelper.getPixelFormat();
        final boolean srcPremul = srcImage.isAlphaPremultiplied();
        
        // Can be null.
        final TestImageTypeEnum dstImageTypeEnum =
            TestImageTypeEnum.enumByType().get(dstImage.getType());
        // Can be null.
        final BihPixelFormat dstPixelFormat =
            dstHelper.getPixelFormat();
        final boolean dstPremul = dstImage.isAlphaPremultiplied();
        
        final boolean srcCmaAllowed =
            srcHelper.isColorModelAvoidingAllowed();
        final boolean srcAduAllowed =
            srcHelper.isArrayDirectUseAllowed();
        
        final boolean dstCmaAllowed =
            dstHelper.isColorModelAvoidingAllowed();
        final boolean dstAduAllowed =
            dstHelper.isArrayDirectUseAllowed();
        
        if (DEBUG) {
            System.out.println();
            System.out.println("srcImageWidth = " + srcImageWidth);
            System.out.println("srcImageHeight = " + srcImageHeight);
            System.out.println("dstImageWidth = " + dstImageWidth);
            System.out.println("dstImageHeight = " + dstImageHeight);
            //
            System.out.println("srcImageTypeEnum = " + srcImageTypeEnum);
            System.out.println("srcPixelFormat = " + srcPixelFormat);
            System.out.println("srcPremul = " + srcPremul);
            System.out.println("dstImageTypeEnum = " + dstImageTypeEnum);
            System.out.println("dstPixelFormat = " + dstPixelFormat);
            System.out.println("dstPremul = " + dstPremul);
            //
            System.out.println("srcCmaAllowed = " + srcCmaAllowed);
            System.out.println("srcAduAllowed = " + srcAduAllowed);
            System.out.println("dstCmaAllowed = " + dstCmaAllowed);
            System.out.println("dstAduAllowed = " + dstAduAllowed);
        }
        
        final int srcX;
        final int srcY;
        final int dstX;
        final int dstY;
        final int width;
        final int height;
        if (srcDstRectElseFullRect) {
            srcX = BihTestUtils.randomPosOrZero(random, srcImageWidth);
            srcY = BihTestUtils.randomPosOrZero(random, srcImageHeight);
            dstX = BihTestUtils.randomPosOrZero(random, dstImageWidth);
            dstY = BihTestUtils.randomPosOrZero(random, dstImageHeight);
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
        
        final BufferedImageHelper expectedDstHelper =
            BihTestUtils.newSameTypeImageAndHelper(dstHelper);
        
        final int outerNonPremulArgb32;
        {
            int argb32 = OUTER_NON_PREMUL_ARGB32;
            if (dstHelper.isOpaque()) {
                argb32 = Argb32.toOpaque(argb32);
            }
            outerNonPremulArgb32 = argb32;
            // Fill for surrounding.
            BihTestUtils.fillHelperWithNonPremulArgb32(
                dstHelper,
                outerNonPremulArgb32);
            // Randomizing for destination area.
            BihTestUtils.randomizeHelper(
                random,
                dstHelper,
                dstX,
                dstY,
                width,
                height,
                false);
            // Same for expected.
            BihTestUtils.copyHelper(
                dstHelper,
                expectedDstHelper);
        }
        
        BihTestUtils.copyImage_reference(
            srcHelper,
            srcX,
            srcY,
            expectedDstHelper,
            dstX,
            dstY,
            width,
            height);
        
        callCopyImage(
            srcHelper,
            srcX,
            srcY,
            dstHelper,
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
                        dstHelper.getNonPremulArgb32At(x, y);
                    BihTestUtils.checkColorEquals(
                        outerNonPremulArgb32,
                        actualArgb32);
                }
            }
        }
        
        // Checking pixels within the area.
        BihTestUtils.checkImageResult(
            srcHelper,
            srcX,
            srcY,
            //
            expectedDstHelper,
            //
            dstHelper,
            //
            dstX,
            dstY,
            width,
            height,
            //
            CPT_DELTA_TOL_BIN,
            CPT_DELTA_TOL_OTHER);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Uses small spans.
     * 
     * @return Images of all BufferedImage types
     *         and all (BihPixelFormat,premul) types (which overlap a bit).
     */
    private static List<BufferedImage> newImageListWithStrides() {
        return BihTestUtils.newImageListOfDimWithStrides(SMALL_WIDTH, SMALL_HEIGHT);
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
        BihPixelFormat dstPixelFormat,
        boolean dstPremul,
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
            System.out.println("dstPixelFormat = " + dstPixelFormat);
            System.out.println("dstPremul = " + dstPremul);
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
            dstPixelFormat,
            dstPremul,
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
        BihPixelFormat srcPixelFormat,
        boolean srcPremul,
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
            System.out.println("srcPixelFormat = " + srcPixelFormat);
            System.out.println("srcPremul = " + srcPremul);
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
            srcPixelFormat,
            srcPremul,
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
        BufferedImageHelper srcHelper,
        int srcX,
        int srcY,
        //
        BufferedImageHelper dstHelper,
        int dstX,
        int dstY,
        //
        int width,
        int height) {
        
        if (DEBUG) {
            System.out.println("calling copyImage():");
            System.out.println("srcHelper = " + srcHelper);
            System.out.println("srcX = " + srcX);
            System.out.println("srcY = " + srcY);
            System.out.println("dstHelper = " + dstHelper);
            System.out.println("dstX = " + dstX);
            System.out.println("dstY = " + dstY);
            System.out.println("width = " + width);
            System.out.println("height = " + height);
        }
        
        BufferedImageHelper.copyImage(
            srcHelper,
            srcX,
            srcY,
            //
            dstHelper,
            dstX,
            dstY,
            //
            width,
            height);
    }
}
