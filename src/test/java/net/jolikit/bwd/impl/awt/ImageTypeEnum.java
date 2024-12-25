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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * BufferedImage image types as an enum
 * (TYPE_CUSTOM excluded - can't have premul flag for it).
 */
public enum ImageTypeEnum {
    TYPE_INT_ARGB(BufferedImage.TYPE_INT_ARGB, true, false),
    TYPE_INT_ARGB_PRE(BufferedImage.TYPE_INT_ARGB_PRE, true, true),
    TYPE_INT_RGB(BufferedImage.TYPE_INT_RGB, false, false),
    TYPE_INT_BGR(BufferedImage.TYPE_INT_BGR, false, false),
    //
    TYPE_4BYTE_ABGR(BufferedImage.TYPE_4BYTE_ABGR, true, false),
    TYPE_4BYTE_ABGR_PRE(BufferedImage.TYPE_4BYTE_ABGR_PRE, true, true),
    TYPE_3BYTE_BGR(BufferedImage.TYPE_3BYTE_BGR, false, false),
    //
    TYPE_USHORT_555_RGB(BufferedImage.TYPE_USHORT_555_RGB, false, false),
    TYPE_USHORT_565_RGB(BufferedImage.TYPE_USHORT_565_RGB, false, false),
    TYPE_USHORT_GRAY(BufferedImage.TYPE_USHORT_GRAY, false, false),
    //
    TYPE_BYTE_GRAY(BufferedImage.TYPE_BYTE_GRAY, false, false),
    TYPE_BYTE_BINARY(BufferedImage.TYPE_BYTE_BINARY, false, false),
    TYPE_BYTE_INDEXED(BufferedImage.TYPE_BYTE_INDEXED, false, false);
    /*
     * 
     */
    private final int imageType;
    private final boolean hasAlpha;
    private final boolean isPremul;
    ImageTypeEnum(
        int imageType,
        boolean hasAlpha,
        boolean isPremul) {
        this.imageType = imageType;
        this.hasAlpha = hasAlpha;
        this.isPremul = isPremul;
    }
    public int imageType() {
        return this.imageType;
    }
    public boolean hasAlpha() {
        return this.hasAlpha;
    }
    public boolean isPremul() {
        return this.isPremul;
    }
    /*
     * 
     */
    private static final List<ImageTypeEnum> LIST =
        Collections.unmodifiableList(
            Arrays.asList(
                ImageTypeEnum.values()));
    public static List<ImageTypeEnum> list() {
        return LIST;
    }
    /*
     * 
     */
    private static final SortedMap<Integer,ImageTypeEnum> ENUM_BY_TYPE;
    static {
        final SortedMap<Integer,ImageTypeEnum> map = new TreeMap<>();
        for (ImageTypeEnum ite : ImageTypeEnum.values()) {
            map.put(ite.imageType(), ite);
        }
        ENUM_BY_TYPE = Collections.unmodifiableSortedMap(map);
    }
    public static SortedMap<Integer,ImageTypeEnum> enumByType() {
        return ENUM_BY_TYPE;
    }
}
