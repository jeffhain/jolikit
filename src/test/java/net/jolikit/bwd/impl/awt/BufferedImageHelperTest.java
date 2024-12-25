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
        final BufferedImage image = newImage();
        final BufferedImageHelper helper = new BufferedImageHelper(image);
        // allowColorModelAvoiding is true by default.
        assertTrue(helper.isColorModelAvoided());
    }
    
    public void test_BufferedImageHelper_BufferedImage_2boolean() {
        for (BufferedImage image : newImageList()) {
            final int imageType = image.getType();
            final BihPixelFormat pixelFormat =
                BufferedImageHelper.computePixelFormat(image);
            
            final boolean isImageTypeCmaCompatible =
                (pixelFormat != null)
                || (imageType == BufferedImage.TYPE_USHORT_555_RGB)
                || (imageType == BufferedImage.TYPE_USHORT_565_RGB)
                || (imageType == BufferedImage.TYPE_USHORT_GRAY)
                || (imageType == BufferedImage.TYPE_BYTE_GRAY);
            
            final boolean isImageTypeAduCompatible =
                isImageTypeCmaCompatible;
                
            for (boolean allowColorModelAvoiding : new boolean[] {false, true}) {
                for (boolean allowArrayDirectUse : new boolean[] {false, true}) {
                    final BufferedImageHelper helper = new BufferedImageHelper(
                        image,
                        allowColorModelAvoiding,
                        allowArrayDirectUse);
                    
                    // For all supported image types,
                    // we have scalar pixels,
                    // so color model is actually avoided.
                    final boolean expectedCma =
                        allowColorModelAvoiding
                        && isImageTypeCmaCompatible;
                    final boolean actualCma =
                        helper.isColorModelAvoided();
                    assertEquals(expectedCma, actualCma);
                    
                    final boolean expectedAdu =
                        allowArrayDirectUse
                        && isImageTypeAduCompatible
                        && expectedCma;
                    final boolean actualAdu =
                        helper.isArrayDirectlyUsed();
                    assertEquals(expectedAdu, actualAdu);
                }
            }
        }
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
    
    public void test_isColorModelAvoided() {
        // Already tested in constructor test.
    }
    
    public void test_isArrayDirectlyUsed() {
        // Already tested in constructor test.
    }
    
    /*
     * 
     */
    
    public void test_computePixelFormat() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        /*
         * Images corresponding to all (BihPixelFormat,premul) types.
         */
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            for (boolean premul : BihTestUtils.newPremulArr(pixelFormat)) {
                
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        width,
                        height,
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
                    width,
                    height,
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
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        
        // too small array
        try {
            BufferedImageHelper.newBufferedImageWithIntArray(
                new int[width * height - 1],
                width,
                height,
                BufferedImage.TYPE_INT_ARGB);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        
        // bad width
        for (int badWidth : new int[] {
            Integer.MIN_VALUE,
            -1,
            0,
        }) {
            try {
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    badWidth,
                    height,
                    BufferedImage.TYPE_INT_ARGB);
                fail();
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
            }
        }
        
        // bad height
        for (int badHeight : new int[] {
            Integer.MIN_VALUE,
            -1,
            0,
        }) {
            try {
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    width,
                    badHeight,
                    BufferedImage.TYPE_INT_ARGB);
                fail();
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
            }
        }
        
        // bad type
        for (int imageType : TYPE_XXX_LIST) {
            if (!TYPE_INT_XXX_LIST.contains(imageType)) {
                try {
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        width,
                        height,
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
            final BufferedImage image =
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    width,
                    height,
                    imageType);
            assertEquals(width, image.getWidth());
            assertEquals(height, image.getHeight());
            assertEquals(imageType, image.getType());
            final int[] pixelArr = BufferedImageHelper.getIntArray(image);
            assertNotNull(pixelArr);
            assertEquals(width * height, pixelArr.length);
        }
        
        /*
         * With array specified.
         */
        
        {
            final int[] pixelArr = new int[width * height];
            for (int imageType : TYPE_INT_XXX_LIST) {
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        pixelArr,
                        width,
                        height,
                        imageType);
                assertEquals(width, image.getWidth());
                assertEquals(height, image.getHeight());
                assertEquals(imageType, image.getType());
                assertSame(pixelArr, BufferedImageHelper.getIntArray(image));
            }
        }
    }
    
    public void test_newBufferedImageWithIntArray_BihPixelFormat() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        
        // too small array
        try {
            BufferedImageHelper.newBufferedImageWithIntArray(
                new int[width * height - 1],
                width,
                height,
                BihPixelFormat.ARGB32,
                false);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        
        // bad width
        for (int badWidth : new int[] {
            Integer.MIN_VALUE,
            -1,
            0,
        }) {
            try {
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    badWidth,
                    height,
                    BihPixelFormat.ARGB32,
                    false);
                fail();
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
            }
        }
        
        // bad height
        for (int badHeight : new int[] {
            Integer.MIN_VALUE,
            -1,
            0,
        }) {
            try {
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    width,
                    badHeight,
                    BihPixelFormat.ARGB32,
                    false);
                fail();
            } catch (IllegalArgumentException e) {
                assertNotNull(e);
            }
        }
        
        // null pixel format
        try {
            BufferedImageHelper.newBufferedImageWithIntArray(
                null,
                width,
                height,
                null,
                false);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
        
        // bad premul
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            if (!pixelFormat.hasAlpha()) {
                try {
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        width,
                        height,
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
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        null,
                        width,
                        height,
                        pixelFormat,
                        premul);
                assertEquals(width, image.getWidth());
                assertEquals(height, image.getHeight());
                final int expectedImageType =
                    pixelFormat.toImageType(premul);
                assertEquals(expectedImageType, image.getType());
                final int[] pixelArr = BufferedImageHelper.getIntArray(image);
                assertNotNull(pixelArr);
                assertEquals(width * height, pixelArr.length);
            }
        }
        
        /*
         * With array specified.
         */
        
        {
            final int[] pixelArr = new int[width * height];
            for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
                for (boolean premul : BihTestUtils.newPremulArr(pixelFormat)) {
                    final BufferedImage image =
                        BufferedImageHelper.newBufferedImageWithIntArray(
                            pixelArr,
                            width,
                            height,
                            pixelFormat,
                            premul);
                    assertEquals(width, image.getWidth());
                    assertEquals(height, image.getHeight());
                    final int expectedImageType =
                        pixelFormat.toImageType(premul);
                    assertEquals(expectedImageType, image.getType());
                    assertSame(pixelArr, BufferedImageHelper.getIntArray(image));
                }
            }
        }
    }
    
    public void test_newBufferedImageWithIntArray_cptIndexes() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        
        // too small array
        try {
            BufferedImageHelper.newBufferedImageWithIntArray(
                new int[width * height - 1],
                width,
                height,
                //
                false,
                -1,
                //
                1,
                2,
                3);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
        
        // bad width
        for (int badWidth : new int[] {
            Integer.MIN_VALUE,
            -1,
            0,
        }) {
            try {
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    badWidth,
                    height,
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
        }
        
        // bad height
        for (int badHeight : new int[] {
            Integer.MIN_VALUE,
            -1,
            0,
        }) {
            try {
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    width,
                    badHeight,
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
        }
        
        // bad premul
        try {
            BufferedImageHelper.newBufferedImageWithIntArray(
                null,
                width,
                height,
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
        
        // bad component indexes
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
                        width,
                        height,
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
                            width,
                            height,
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
            final BufferedImage image =
                BufferedImageHelper.newBufferedImageWithIntArray(
                    null,
                    width,
                    height,
                    //
                    premul,
                    0,
                    //
                    1,
                    2,
                    3);
            assertEquals(width, image.getWidth());
            assertEquals(height, image.getHeight());
            final int expectedImageType =
                (premul
                    ? BufferedImage.TYPE_INT_ARGB_PRE
                        : BufferedImage.TYPE_INT_ARGB);
            assertEquals(expectedImageType, image.getType());
            final int[] pixelArr = BufferedImageHelper.getIntArray(image);
            assertNotNull(pixelArr);
            assertEquals(width * height, pixelArr.length);
        }
        
        /*
         * With array specified.
         */
        
        {
            final int[] pixelArr = new int[width * height];
            for (boolean premul : new boolean[] {false, true}) {
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        pixelArr,
                        width,
                        height,
                        //
                        premul,
                        0,
                        //
                        1,
                        2,
                        3);
                assertEquals(width, image.getWidth());
                assertEquals(height, image.getHeight());
                final int expectedImageType =
                    (premul
                        ? BufferedImage.TYPE_INT_ARGB_PRE
                            : BufferedImage.TYPE_INT_ARGB);
                assertEquals(expectedImageType, image.getType());
                assertSame(pixelArr, BufferedImageHelper.getIntArray(image));
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * Tests all methods related to int array.
     */
    public void test_xxxIntArray() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        final int[] pixelArr = new int[width * height];
        
        /*
         * Bad array type.
         */
        
        {
            final BufferedImage image =
                new BufferedImage(
                    width,
                    height,
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
        
        for (BihPixelFormat pixelFormat : BihPixelFormat.values()) {
            for (boolean premul : BihTestUtils.newPremulArr(pixelFormat)) {
                
                final BufferedImage image =
                    BufferedImageHelper.newBufferedImageWithIntArray(
                        pixelArr,
                        width,
                        height,
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
    
    /*
     * 
     */
    
    public void test_getArgb32At_setArgb32At_exceptions() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean premul = false;
                
                for (int badX : new int[] {
                    Integer.MIN_VALUE,
                    -1,
                    width,
                    Integer.MAX_VALUE}) {
                    
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
                
                for (int badY : new int[] {
                    Integer.MIN_VALUE,
                    -1,
                    height,
                    Integer.MAX_VALUE}) {
                    
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
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList_allBihPixelFormat(width, height)) {
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final BihPixelFormat pixelFormat = helper.getPixelFormat();
                final int[] pixelArr = BufferedImageHelper.getIntArray(image);
                
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
                
                {
                    final int argb32 =
                        (imagePremul ? expectedPremulArgb32 : expectedNonPremulArgb32);
                    final int pixel = pixelFormat.toPixelFromArgb32(argb32);
                    pixelArr[x + width * y] = pixel;
                }
                
                final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                checkEqual(expectedNonPremulArgb32, actualNonPremulArgb32);
                
                final int actualPremulArgb32 = helper.getPremulArgb32At(x, y);
                checkEqual(expectedPremulArgb32, actualPremulArgb32);
            }
        }
    }
    
    public void test_setXxxArgb32At_allBihPixelFormat() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList_allBihPixelFormat(width, height)) {
            
            final boolean imagePremul = image.isAlphaPremultiplied();
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final BihPixelFormat pixelFormat = helper.getPixelFormat();
                final int[] pixelArr = BufferedImageHelper.getIntArray(image);
                
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
                
                {
                    helper.setNonPremulArgb32At(x, y, nonPremulArgb32);
                    final int actualPixel = pixelArr[x + width * y];
                    checkEqual(expectedPixel, actualPixel);
                }
                
                {
                    helper.setPremulArgb32At(x, y, premulArgb32);
                    final int actualPixel = pixelArr[x + width * y];
                    checkEqual(expectedPixel, actualPixel);
                }
            }
        }
    }
    
    public void test_getXxxArgb32At_allImageType() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            final BufferedImage image = new BufferedImage(
                width,
                height,
                imageType);
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean cma = helper.isColorModelAvoided();
                
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
                        System.out.println("isColorModelAvoided() = " + helper.isColorModelAvoided());
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
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            final BufferedImage image = new BufferedImage(
                width,
                height,
                imageType);
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean cma = helper.isColorModelAvoided();
                
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
                        System.out.println("isColorModelAvoided() = " + helper.isColorModelAvoided());
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
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (ImageTypeEnum imageTypeEnum : ImageTypeEnum.values()) {
            final int imageType = imageTypeEnum.imageType();
            final BufferedImage image = new BufferedImage(
                width,
                height,
                imageType);
            
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean cma = helper.isColorModelAvoided();
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
                        System.out.println("isColorModelAvoided() = " + helper.isColorModelAvoided());
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
    
    public void test_drawPointPremulAt_allBihPixelFormat() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList_allBihPixelFormat(width, height)) {
            
            final BufferedImageHelper helper = new BufferedImageHelper(image);
            assertTrue(helper.isColorModelAvoided());
            
            final BihPixelFormat pixelFormat = helper.getPixelFormat();
            
            final int x = 1;
            final int y = 2;
            // Components values high enough to preserve bijectivity
            // when converting between premul and non premul.
            int nonPremulArgb32 = 0xA76543B1;
            if (!pixelFormat.hasAlpha()) {
                nonPremulArgb32 = Argb32.toOpaque(nonPremulArgb32);
            }
            final int premulArgb32 = BindingColorUtils.toPremulAxyz32(nonPremulArgb32);
            
            // First draw: blending equivalent to set.
            {
                final int expectedNonPremulArgb32 = nonPremulArgb32;
                helper.drawPointPremulAt(x, y, premulArgb32);
                
                final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                checkEqual(expectedNonPremulArgb32, actualNonPremulArgb32);
            }
            
            // Second draw: blending into itself.
            {
                final int expectedNonPremulArgb32 =
                    BindingColorUtils.toNonPremulAxyz32(
                        BindingColorUtils.blendPremulAxyz32_srcOver(
                            premulArgb32,
                            premulArgb32));
                helper.drawPointPremulAt(x, y, premulArgb32);
                
                final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                checkEqual(expectedNonPremulArgb32, actualNonPremulArgb32);
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_clearRect_fillRectPremul_invertPixels_exceptions() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                for (int badX : new int[] {
                    Integer.MIN_VALUE,
                    -1,
                    width,
                    Integer.MAX_VALUE}) {
                    
                    try {
                        helper.clearRect(badX, 0, 1, 1, 0xFF000000, false);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        assertNotNull(e);
                    }
                    
                    try {
                        helper.fillRectPremul(badX, 0, 1, 1, 0xFF000000);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        assertNotNull(e);
                    }
                    
                    try {
                        helper.invertPixels(badX, 0, 1, 1);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        assertNotNull(e);
                    }
                }
                
                for (int badY : new int[] {
                    Integer.MIN_VALUE,
                    -1,
                    height,
                    Integer.MAX_VALUE}) {
                    
                    try {
                        helper.clearRect(0, badY, 1, 1, 0xFF000000, false);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        assertNotNull(e);
                    }
                    
                    try {
                        helper.fillRectPremul(0, badY, 1, 1, 0xFF000000);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        assertNotNull(e);
                    }
                    
                    try {
                        helper.invertPixels(0, badY, 1, 1);
                        fail();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        assertNotNull(e);
                    }
                }
            }
        }
    }
    
    public void test_clearRect_allBihPixelFormat() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList_allBihPixelFormat(width, height)) {
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
    
    public void test_fillRectPremul_allBihPixelFormat() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList_allBihPixelFormat(width, height)) {
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
            
            /*
             * Filling.
             */
            
            helper.fillRectPremul(1, 1, 3, 2, premulArgb32);
            
            // Rectangle filled.
            for (int x = 1; x <= 3; x++) {
                for (int y = 1; y <= 2; y++) {
                    final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                    checkEqual(nonPremulArgb32, actualNonPremulArgb32);
                }
            }
            
            // Surroundings not filled.
            for (int x : new int[] {0, 4}) {
                for (int y : new int[] {0, 3}) {
                    final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                    checkEqual(initialNonPremulArgb32, actualNonPremulArgb32);
                }
            }
            
            /*
             * Filling again.
             */
            
            helper.fillRectPremul(1, 1, 3, 2, premulArgb32);
            
            // Color blended into itself (no change if opaque).
            for (int x = 1; x <= 3; x++) {
                for (int y = 1; y <= 2; y++) {
                    final int expectedNonPremulArgb32 =
                        BindingColorUtils.toNonPremulAxyz32(
                            BindingColorUtils.blendPremulAxyz32_srcOver(
                                premulArgb32,
                                premulArgb32));
                    final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                    checkEqual(expectedNonPremulArgb32, actualNonPremulArgb32);
                }
            }
        }
    }
    
    public void test_invertPixels_allBihPixelFormat() {
        final int width = SMALL_WIDTH;
        final int height = SMALL_HEIGHT;
        for (BufferedImage image : BihTestUtils.newImageList_allBihPixelFormat(width, height)) {
            final BufferedImageHelper helper = new BufferedImageHelper(image);
            
            final BihPixelFormat pixelFormat = helper.getPixelFormat();
            
            int nonPremulArgb32 = 0xCC88C88C;
            if (!pixelFormat.hasAlpha()) {
                nonPremulArgb32 = Argb32.toOpaque(nonPremulArgb32);
            }
            final int premulArgb32 =
                BindingColorUtils.toPremulAxyz32(nonPremulArgb32);
            // Inverted must be bijective through premul.
            checkEqual(nonPremulArgb32,
                Argb32.inverted(
                    BindingColorUtils.toNonPremulAxyz32(
                        BindingColorUtils.toPremulAxyz32(
                            Argb32.inverted(
                                BindingColorUtils.toNonPremulAxyz32(
                                    premulArgb32))))));
            
            /*
             * Clearing all with the color (to invert it).
             */
            
            helper.clearRect(0, 0, width, height, nonPremulArgb32, false);
            
            /*
             * Inverting.
             */
            
            helper.invertPixels(1, 1, 3, 2);
            
            // Rectangle inverted.
            for (int x = 1; x <= 3; x++) {
                for (int y = 1; y <= 2; y++) {
                    final int expectedNonPremulArgb32 =
                        Argb32.inverted(nonPremulArgb32);
                    final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                    checkEqual(expectedNonPremulArgb32, actualNonPremulArgb32);
                }
            }
            
            // Surroundings not inverted.
            for (int x : new int[] {0, 4}) {
                for (int y : new int[] {0, 3}) {
                    final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                    checkEqual(nonPremulArgb32, actualNonPremulArgb32);
                }
            }
            
            /*
             * Inverting again.
             */
            
            helper.invertPixels(1, 1, 3, 2);
            
            // All become as after clearing.
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    final int actualNonPremulArgb32 = helper.getNonPremulArgb32At(x, y);
                    checkEqual(nonPremulArgb32, actualNonPremulArgb32);
                }
            }
        }
    }
    
    /*
     * 
     */
    
    public void test_getPixelsInto_exceptions_fullRect() {
        final boolean srcRectElseFullRect = false;
        this.test_getPixelsInto_exceptions_xxx(srcRectElseFullRect);
    }
    
    public void test_getPixelsInto_exceptions_srcRect() {
        final boolean srcRectElseFullRect = true;
        this.test_getPixelsInto_exceptions_xxx(srcRectElseFullRect);
    }
    
    public void test_getPixelsInto_exceptions_xxx(boolean srcRectElseFullRect) {
        // Large enough for some coordinates leeway.
        final int width = 30;
        final int height = 20;
        for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                /*
                 * Null array.
                 */
                try {
                    callGetPixelsInto_xxx(
                        srcRectElseFullRect,
                        helper,
                        //
                        0,
                        0,
                        width,
                        height,
                        //
                        null,
                        width,
                        //
                        BihPixelFormat.ARGB32,
                        false);
                    fail();
                } catch (NullPointerException e) {
                    assertNotNull(e);
                }
                /*
                 * Null pixel format.
                 */
                try {
                    callGetPixelsInto_xxx(
                        srcRectElseFullRect,
                        helper,
                        //
                        0,
                        0,
                        width,
                        height,
                        //
                        new int[width * height],
                        width,
                        //
                        null,
                        false);
                    fail();
                } catch (NullPointerException e) {
                    assertNotNull(e);
                }
                /*
                 * Bad premul.
                 */
                try {
                    callGetPixelsInto_xxx(
                        srcRectElseFullRect,
                        helper,
                        //
                        0,
                        0,
                        width,
                        height,
                        //
                        new int[width * height],
                        width,
                        //
                        BihPixelFormat.XRGB24,
                        true);
                    fail();
                } catch (IllegalArgumentException e) {
                    assertNotNull(e);
                }
                /*
                 * Bad x positions.
                 */
                if (srcRectElseFullRect) {
                    for (int[] badXPosSpan : new int[][] {
                        {Integer.MIN_VALUE, 0},
                        {-1, 0},
                        {width, 0},
                        {Integer.MAX_VALUE, 0},
                        //
                        {0, Integer.MIN_VALUE},
                        {0, -1},
                        {0, width + 1},
                        {0, Integer.MAX_VALUE},
                        //
                        {width-1, 2},
                        {width-1, Integer.MAX_VALUE},
                    }) {
                        final int srcX = badXPosSpan[0];
                        final int srcWidth = badXPosSpan[1];
                        try {
                            callGetPixelsInto_srcRect(
                                helper,
                                //
                                srcX,
                                0,
                                srcWidth,
                                height,
                                //
                                new int[width * height],
                                width,
                                //
                                BihPixelFormat.ARGB32,
                                false);
                            fail();
                        } catch (IllegalArgumentException e) {
                            assertNotNull(e);
                        }
                    }
                } else {
                    // N/A
                }
                /*
                 * Bad y positions.
                 */
                if (srcRectElseFullRect) {
                    for (int[] badYPosSpan : new int[][] {
                        {Integer.MIN_VALUE, 0},
                        {-1, 0},
                        {height, 0},
                        {Integer.MAX_VALUE, 0},
                        //
                        {0, Integer.MIN_VALUE},
                        {0, -1},
                        {0, height + 1},
                        {0, Integer.MAX_VALUE},
                        //
                        {height-1, 2},
                        {height-1, Integer.MAX_VALUE},
                    }) {
                        final int srcY = badYPosSpan[0];
                        final int srcHeight = badYPosSpan[1];
                        try {
                            callGetPixelsInto_srcRect(
                                helper,
                                //
                                0,
                                srcY,
                                width,
                                srcHeight,
                                //
                                new int[width * height],
                                width,
                                //
                                BihPixelFormat.ARGB32,
                                false);
                            fail();
                        } catch (IllegalArgumentException e) {
                            assertNotNull(e);
                        }
                    }
                } else {
                    // N/A
                }
                /*
                 * Bad scanline stride.
                 */
                for (int badScanlineStride : new int[] {
                    Integer.MIN_VALUE,
                    -1,
                    0,
                    width - 1,
                    // Not bad in itself, but too large for array,
                    // and to test overflow handling in checks.
                    Integer.MAX_VALUE,
                }) {
                    try {
                        callGetPixelsInto_xxx(
                            srcRectElseFullRect,
                            helper,
                            //
                            0,
                            0,
                            width,
                            height,
                            //
                            new int[width * height],
                            badScanlineStride,
                            //
                            BihPixelFormat.ARGB32,
                            false);
                        fail();
                    } catch (IllegalArgumentException e) {
                        assertNotNull(e);
                    }
                }
                /*
                 * Not bad scanline stride:
                 * zero valid if srcWidth is zero.
                 */
                if (srcRectElseFullRect) {
                    final int badScanlineStride = 0;
                    try {
                        callGetPixelsInto_srcRect(
                            helper,
                            //
                            0,
                            0,
                            0,
                            1,
                            //
                            new int[width * height],
                            badScanlineStride,
                            //
                            BihPixelFormat.ARGB32,
                            false);
                    } catch (IllegalArgumentException e) {
                        throw new AssertionError(e);
                    }
                }
                /*
                 * Bad array length.
                 */
                {
                    final int srcX = (srcRectElseFullRect ? 2 : 0);
                    final int srcY = (srcRectElseFullRect ? 1 : 0);
                    final int srcWidth = (srcRectElseFullRect ? width - 8 : width);
                    final int srcHeight = (srcRectElseFullRect ? height - 9 : height);
                    final int scanlineStride = srcWidth + 1;
                    for (int badArrayLength : new int[] {
                        0,
                        1,
                        ((srcHeight - 1) * scanlineStride + srcWidth) - 1,
                    }) {
                        try {
                            callGetPixelsInto_xxx(
                                srcRectElseFullRect,
                                helper,
                                //
                                srcX,
                                srcY,
                                srcWidth,
                                srcHeight,
                                //
                                new int[badArrayLength],
                                scanlineStride,
                                //
                                BihPixelFormat.ARGB32,
                                false);
                            fail();
                        } catch (IllegalArgumentException e) {
                            assertNotNull(e);
                        }
                    }
                }
            }
        }
    }
    
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
         * and various kinds of (alpha,color) pairs,
         * taking into account that when srcRectElseFullRect is true
         * we usually only cover a small part of the image
         * due to random position and spans.
         */
        final int width = 256 * (srcRectElseFullRect ? 4 : 1);
        final int height = 32 * (srcRectElseFullRect ? 4 : 1);
        
        final int color32ArrScanlineStride = width + 1;
        
        final int[] expectedColor32Arr =
            new int[color32ArrScanlineStride * height];
        final int[] actualColor32Arr =
            new int[color32ArrScanlineStride * height];
        
        final Random random = TestUtils.newRandom123456789L();
        
        for (BufferedImage image : BihTestUtils.newImageList(width, height)) {
            for (BufferedImageHelper helper : BihTestUtils.newHelperList(image)) {
                
                final boolean isColorModelAvoided = helper.isColorModelAvoided();
                
                // Can be null.
                final BihPixelFormat imagePixelFormat =
                    helper.getPixelFormat();
                final boolean imagePremul = image.isAlphaPremultiplied();
                // Can be null.
                final ImageTypeEnum imageTypeEnum =
                    ImageTypeEnum.enumByType().get(image.getType());
                
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        final int argb32 = random.nextInt();
                        helper.setNonPremulArgb32At(x, y, argb32);
                    }
                }
                
                for (BihPixelFormat pixelFormatTo : BihPixelFormat.values()) {
                    for (boolean premulTo : BihTestUtils.newPremulArr(pixelFormatTo)) {
                        if (DEBUG) {
                            System.out.println();
                            System.out.println("imagePixelFormat = " + imagePixelFormat);
                            System.out.println("imagePremul = " + imagePremul);
                            System.out.println("imageTypeEnum = " + imageTypeEnum);
                            System.out.println("isColorModelAvoided = " + isColorModelAvoided);
                            System.out.println("image pixelFormat = " + imagePixelFormat);
                            System.out.println("image premul = " + image.isAlphaPremultiplied());
                            System.out.println("array pixelFormat = " + pixelFormatTo);
                            System.out.println("array premul = " + premulTo);
                        }
                        
                        final int srcX;
                        final int srcY;
                        final int srcWidth;
                        final int srcHeight;
                        if (srcRectElseFullRect) {
                            srcX = random.nextInt(width);
                            srcY = random.nextInt(height);
                            // Zero width/height accepted.
                            srcWidth = random.nextInt(width - srcX + 1);
                            srcHeight = random.nextInt(height - srcY + 1);
                        } else {
                            srcX = 0;
                            srcY = 0;
                            srcWidth = width;
                            srcHeight = height;
                        }
                        
                        Arrays.fill(expectedColor32Arr, 0);
                        Arrays.fill(actualColor32Arr, 0);
                        
                        getPixelsInto_reference(
                            helper,
                            //
                            srcX,
                            srcY,
                            srcWidth,
                            srcHeight,
                            //
                            expectedColor32Arr,
                            color32ArrScanlineStride,
                            //
                            pixelFormatTo,
                            premulTo);
                        
                        callGetPixelsInto_xxx(
                            srcRectElseFullRect,
                            helper,
                            //
                            srcX,
                            srcY,
                            srcWidth,
                            srcHeight,
                            //
                            actualColor32Arr,
                            color32ArrScanlineStride,
                            //
                            pixelFormatTo,
                            premulTo);
                        
                        for (int y = srcY; y < srcY + srcHeight; y++) {
                            for (int x = srcX; x < srcX + srcWidth; x++) {
                                final int index =
                                    (y - srcY) * color32ArrScanlineStride
                                    + (x - srcX);
                                final int expectedColor32 = expectedColor32Arr[index];
                                final int actualColor32 = actualColor32Arr[index];
                                
                                if (isColorModelAvoided
                                    && image.isAlphaPremultiplied()
                                    && (!premulTo)) {
                                    /*
                                     * In this case, we use drawImage(),
                                     * but it can give a slightly different result.
                                     */
                                    final int imageNonPremulArgb32 =
                                        helper.getNonPremulArgb32At(x, y);
                                    final int imageAlpha8 =
                                        Argb32.getAlpha8(
                                            imageNonPremulArgb32);
                                    final int cptDeltaTol =
                                        ((imageAlpha8 <= 0xFE) ? 1 : 0);
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
        final int ve1 = Argb32.getAlpha8(expectedColor32);
        final int va1 = Argb32.getAlpha8(actualColor32);
        final int ve2 = Argb32.getRed8(expectedColor32);
        final int va2 = Argb32.getRed8(actualColor32);
        final int ve3 = Argb32.getGreen8(expectedColor32);
        final int va3 = Argb32.getGreen8(actualColor32);
        final int ve4 = Argb32.getBlue8(expectedColor32);
        final int va4 = Argb32.getBlue8(actualColor32);
        if (DEBUG
            && ((Math.abs(va1 - ve1) > cptDeltaTol)
                || (Math.abs(va2 - ve2) > cptDeltaTol)
                || (Math.abs(va3 - ve3) > cptDeltaTol)
                || (Math.abs(va4 - ve4) > cptDeltaTol))) {
            final String expected = Argb32.toString(expectedColor32);
            final String actual = Argb32.toString(actualColor32);
            System.out.println("cptDeltaTol = " + cptDeltaTol);
            System.out.println("expected = " + expected);
            System.out.println("actual =   " + actual);
        }
        TestCase.assertEquals(ve1, va1, cptDeltaTol);
        TestCase.assertEquals(ve2, va2, cptDeltaTol);
        TestCase.assertEquals(ve3, va3, cptDeltaTol);
        TestCase.assertEquals(ve4, va4, cptDeltaTol);
    }
    
    /*
     * 
     */
    
    private static void callGetPixelsInto_srcRect(
        BufferedImageHelper helper,
        //
        int srcX,
        int srcY,
        int srcWidth,
        int srcHeight,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        final boolean srcRectElseFullRect = true;
        callGetPixelsInto_xxx(
            srcRectElseFullRect,
            helper,
            //
            srcX,
            srcY,
            srcWidth,
            srcHeight,
            //
            color32Arr,
            color32ArrScanlineStride,
            pixelFormatTo,
            premulTo);
    }
    
    private static void callGetPixelsInto_fullRect(
        BufferedImageHelper helper,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        final boolean srcRectElseFullRect = false;
        callGetPixelsInto_xxx(
            srcRectElseFullRect,
            helper,
            //
            0,
            0,
            helper.getImage().getWidth(),
            helper.getImage().getHeight(),
            //
            color32Arr,
            color32ArrScanlineStride,
            pixelFormatTo,
            premulTo);
    }
    
    private static void callGetPixelsInto_xxx(
        boolean srcRectElseFullRect,
        BufferedImageHelper helper,
        //
        int srcX,
        int srcY,
        int srcWidth,
        int srcHeight,
        //
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        
        if (DEBUG) {
            System.out.println();
            if (srcRectElseFullRect) {
                System.out.println("calling getPixelsInto() (srcRect):");
                System.out.println("srcX = " + srcX);
                System.out.println("srcY = " + srcY);
                System.out.println("srcWidth = " + srcWidth);
                System.out.println("srcHeight = " + srcHeight);
            } else {
                System.out.println("calling getPixelsInto() (fullRect):");
            }
            System.out.println("color32Arr.length = "
                + ((color32Arr != null) ? color32Arr.length : 0));
            System.out.println("color32ArrScanlineStride = " + color32ArrScanlineStride);
            System.out.println("pixelFormatTo = " + pixelFormatTo);
            System.out.println("premulTo = " + premulTo);
        }
        
        if (srcRectElseFullRect) {
            helper.getPixelsInto(
                srcX,
                srcY,
                srcWidth,
                srcHeight,
                //
                color32Arr,
                color32ArrScanlineStride,
                //
                pixelFormatTo,
                premulTo);
        } else {
            helper.getPixelsInto(
                color32Arr,
                color32ArrScanlineStride,
                //
                pixelFormatTo,
                premulTo);
        }
    }
    
    /**
     * Trivial implementation, based on single-pixel methods,
     * to use as reference for getPixelsInto() correctness,
     * other than for exception checks.
     */
    private static void getPixelsInto_reference(
        BufferedImageHelper helper,
        //
        int srcX,
        int srcY,
        int srcWidth,
        int srcHeight,
        int[] color32Arr,
        int color32ArrScanlineStride,
        //
        BihPixelFormat pixelFormatTo,
        boolean premulTo) {
        
        for (int j = 0; j < srcHeight; j++) {
            final int lineOffset =
                j * color32ArrScanlineStride;
            for (int i = 0; i < srcWidth; i++) {
                final int index = lineOffset + i;
                final int argb32 = helper.getArgb32At(
                    srcX + i,
                    srcY + j,
                    premulTo);
                final int color32 =
                    pixelFormatTo.toPixelFromArgb32(
                        argb32);
                color32Arr[index] = color32;
            }
        }
    }
}
