/*
 * Copyright 2019-2024 Jeff Hain
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
package net.jolikit.bwd.test.utils;

/**
 * Paths of resources for BWD tests.
 */
public class BwdTestResources {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final String TEST_FONTS_DIR_PATH = "src/test/resources/fonts";
    private static final String TEST_IMAGES_DIR_PATH = "src/test/resources/images";
    
    /*
     * Fonts.
     */
    
    public static final String TEST_FONT_FREE_MONO_OTF = TEST_FONTS_DIR_PATH + "/FreeMono.otf";
    public static final String TEST_FONT_FREE_MONO_BOLD_OBLIQUE_OTF = TEST_FONTS_DIR_PATH + "/FreeMonoBoldOblique.otf";
    
    public static final String TEST_FONT_FREE_MONO_TTF = TEST_FONTS_DIR_PATH + "/freemono.ttf";
    public static final String TEST_FONT_FREE_MONO_BOLD_OBLIQUE_TTF = TEST_FONTS_DIR_PATH + "/freemonoboldoblique.ttf";
    
    public static final String TEST_FONT_LUCIDA_CONSOLE_TTF = TEST_FONTS_DIR_PATH + "/lucida_console/lucida_console.ttf";
    public static final String TEST_FONT_LUCIDA_SANS_UNICODE_TTF = TEST_FONTS_DIR_PATH + "/lucida_sans_unicode/lucida_sans_unicode.ttf";
    public static final String TEST_FONT_UNIFONT_8_0_01_TTF = TEST_FONTS_DIR_PATH + "/unifont/unifont-8.0.01.ttf";
    public static final String TEST_FONT_WQY_MICROHEI_TTF = TEST_FONTS_DIR_PATH + "/wqy-microhei.ttf";
    
    public static final String TEST_FONT_WQY_MICROHEI_TTC = TEST_FONTS_DIR_PATH + "/wqy-microhei.ttc";

    public static final String TEST_FONT_A010013L_AFM = TEST_FONTS_DIR_PATH + "/a010013l/a010013l.afm";

    /*
     * Images.
     * 
     * time_square image from imgscalr library,
     * less dark and with more features than cat_and_mice image.
     */
    
    public static final String TEST_IMG_FILE_PATH_CAT_AND_MICE_PNG = TEST_IMAGES_DIR_PATH + "/cat_and_mice.png";
    public static final String TEST_IMG_FILE_PATH_CAT_AND_MICE_ALPHA_PNG = TEST_IMAGES_DIR_PATH + "/cat_and_mice_alpha.png";
    
    public static final String TEST_IMG_FILE_PATH_TIME_SQUARE_PNG = TEST_IMAGES_DIR_PATH + "/time_square.png";
    public static final String TEST_IMG_FILE_PATH_TIME_SQUARE_ALPHA_PNG = TEST_IMAGES_DIR_PATH + "/time_square_alpha.png";
    public static final String TEST_IMG_FILE_PATH_TIME_SQUARE_ALPHA_COLUMNS_PNG = TEST_IMAGES_DIR_PATH + "/time_square_alpha_columns.png";
    
    public static final String TEST_IMG_FILE_PATH_MOUSE_HEAD_PNG = TEST_IMAGES_DIR_PATH + "/mouse_head.png";
    public static final String TEST_IMG_FILE_PATH_MOUSE_HEAD_ALPHA_PNG = TEST_IMAGES_DIR_PATH + "/mouse_head_alpha.png";

    public static final String TEST_IMG_FILE_PATH_FILLED_GREY_PNG = TEST_IMAGES_DIR_PATH + "/test_img_41_21_filled_grey.png";
    public static final String TEST_IMG_FILE_PATH_FILLED_GREY_ALPHA_PNG = TEST_IMAGES_DIR_PATH + "/test_img_41_21_filled_grey_alpha.png";
    
    public static final String TEST_IMG_FILE_PATH_LOREM_PNG = TEST_IMAGES_DIR_PATH + "/lorem_334_164.png";
    public static final String TEST_IMG_FILE_PATH_LOREM_X4_PNG = TEST_IMAGES_DIR_PATH + "/lorem_x4_1336_656.png";
    
    public static final String TEST_IMG_FILE_PATH_STRUCT_GREY_PNG = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_grey.png";
    
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_BMP_01_BIT = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color_01_bit.bmp";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_BMP_04_BIT = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color_04_bits.bmp";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_BMP_08_BIT = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color_08_bits.bmp";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_BMP = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color.bmp";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_GIF = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color.gif";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_JPG = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color.jpg";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_PGM = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color.pgm";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_PNG = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color.png";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_PPM = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color.ppm";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_TGA = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color.tga";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_TIF = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color.tif";
    public static final String TEST_IMG_FILE_PATH_STRUCT_COLOR_ALPHA_PNG = TEST_IMAGES_DIR_PATH + "/test_img_41_21_struct_color_alpha.png";
}
