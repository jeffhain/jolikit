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
package net.jolikit.bwd.impl.sdl2.jlib;

import net.jolikit.bwd.impl.utils.basics.IntValuedHelper;
import net.jolikit.bwd.impl.utils.basics.IntValuedHelper.InterfaceIntValued;

/**
 * Not to be confused with SDL_PixelFormat structure.
 */
public enum SdlPixelFormat implements InterfaceIntValued {
    SDL_PIXELFORMAT_UNKNOWN(0),

    SDL_PIXELFORMAT_INDEX1LSB(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_INDEX1.intValue(), SdlBitmapPixelOrder.SDL_BITMAPORDER_4321.intValue(), 0, 1, 0)),
    SDL_PIXELFORMAT_INDEX1MSB(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_INDEX1.intValue(), SdlBitmapPixelOrder.SDL_BITMAPORDER_1234.intValue(), 0, 1, 0)),
    SDL_PIXELFORMAT_INDEX4LSB(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_INDEX4.intValue(), SdlBitmapPixelOrder.SDL_BITMAPORDER_4321.intValue(), 0, 4, 0)),
    SDL_PIXELFORMAT_INDEX4MSB(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_INDEX4.intValue(), SdlBitmapPixelOrder.SDL_BITMAPORDER_1234.intValue(), 0, 4, 0)),
    SDL_PIXELFORMAT_INDEX8(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_INDEX8.intValue(), 0, 0, 8, 1)),

    SDL_PIXELFORMAT_RGB332(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED8.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_XRGB.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_332.intValue(), 8, 1)),
    SDL_PIXELFORMAT_RGB444(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_XRGB.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_4444.intValue(), 12, 2)),
    SDL_PIXELFORMAT_RGB555(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_XRGB.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_1555.intValue(), 15, 2)),
    SDL_PIXELFORMAT_BGR555(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_XBGR.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_1555.intValue(), 15, 2)),
    
    SDL_PIXELFORMAT_ARGB4444(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_ARGB.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_4444.intValue(), 16, 2)),
    SDL_PIXELFORMAT_RGBA4444(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_RGBA.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_4444.intValue(), 16, 2)),
    SDL_PIXELFORMAT_ABGR4444(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_ABGR.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_4444.intValue(), 16, 2)),
    SDL_PIXELFORMAT_BGRA4444(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_BGRA.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_4444.intValue(), 16, 2)),
    
    SDL_PIXELFORMAT_ARGB1555(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_ARGB.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_1555.intValue(), 16, 2)),
    SDL_PIXELFORMAT_RGBA5551(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_RGBA.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_5551.intValue(), 16, 2)),
    SDL_PIXELFORMAT_ABGR1555(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_ABGR.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_1555.intValue(), 16, 2)),
    SDL_PIXELFORMAT_BGRA5551(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_BGRA.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_5551.intValue(), 16, 2)),
    
    SDL_PIXELFORMAT_RGB565(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_XRGB.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_565.intValue(), 16, 2)),
    SDL_PIXELFORMAT_BGR565(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED16.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_XBGR.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_565.intValue(), 16, 2)),
    
    SDL_PIXELFORMAT_RGB24(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_ARRAYU8.intValue(), SdlArrayComponentOrder.SDL_ARRAYORDER_RGB.intValue(), 0, 24, 3)),
    SDL_PIXELFORMAT_BGR24(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_ARRAYU8.intValue(), SdlArrayComponentOrder.SDL_ARRAYORDER_BGR.intValue(), 0, 24, 3)),
    
    SDL_PIXELFORMAT_RGB888(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED32.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_XRGB.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_8888.intValue(), 24, 4)),
    SDL_PIXELFORMAT_RGBX8888(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED32.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_RGBX.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_8888.intValue(), 24, 4)),
    SDL_PIXELFORMAT_BGR888(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED32.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_XBGR.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_8888.intValue(), 24, 4)),
    SDL_PIXELFORMAT_BGRX8888(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED32.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_BGRX.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_8888.intValue(), 24, 4)),
    
    SDL_PIXELFORMAT_ARGB8888(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED32.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_ARGB.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_8888.intValue(), 32, 4)),
    SDL_PIXELFORMAT_RGBA8888(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED32.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_RGBA.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_8888.intValue(), 32, 4)),
    SDL_PIXELFORMAT_ABGR8888(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED32.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_ABGR.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_8888.intValue(), 32, 4)),
    SDL_PIXELFORMAT_BGRA8888(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED32.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_BGRA.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_8888.intValue(), 32, 4)),
    
    SDL_PIXELFORMAT_ARGB2101010(SdlStatics.SDL_DEFINE_PIXELFORMAT(SdlPixelType.SDL_PIXELTYPE_PACKED32.intValue(), SdlPackedComponentOrder.SDL_PACKEDORDER_ARGB.intValue(), SdlPackedComponentLayout.SDL_PACKEDLAYOUT_2101010.intValue(), 32, 4)),
    
    // Planar mode: Y + V + U  (3 planes)
    SDL_PIXELFORMAT_YV12(SdlStatics.SDL_DEFINE_PIXELFOURCC('Y', 'V', '1', '2')),
    // Planar mode: Y + U + V  (3 planes)
    SDL_PIXELFORMAT_IYUV(SdlStatics.SDL_DEFINE_PIXELFOURCC('I', 'Y', 'U', 'V')),
    // Packed mode: Y0+U0+Y1+V0 (1 plane)
    SDL_PIXELFORMAT_YUY2(SdlStatics.SDL_DEFINE_PIXELFOURCC('Y', 'U', 'Y', '2')),
    // Packed mode: U0+Y0+V0+Y1 (1 plane)
    SDL_PIXELFORMAT_UYVY(SdlStatics.SDL_DEFINE_PIXELFOURCC('U', 'Y', 'V', 'Y')),
    // Packed mode: Y0+V0+Y1+U0 (1 plane)
    SDL_PIXELFORMAT_YVYU(SdlStatics.SDL_DEFINE_PIXELFOURCC('Y', 'V', 'Y', 'U')),
    // Planar mode: Y + U/V interleaved  (2 planes)
    SDL_PIXELFORMAT_NV12(SdlStatics.SDL_DEFINE_PIXELFOURCC('N', 'V', '1', '2')),
    // Planar mode: Y + V/U interleaved  (2 planes)
    SDL_PIXELFORMAT_NV21(SdlStatics.SDL_DEFINE_PIXELFOURCC('N', 'V', '2', '1'));
    
    private static final IntValuedHelper<SdlPixelFormat> HELPER =
            new IntValuedHelper<SdlPixelFormat>(SdlPixelFormat.values());
            
    private final int intValue;
    
    private SdlPixelFormat(int intValue) {
        this.intValue = intValue;
    }
    
    @Override
    public int intValue() {
        return this.intValue;
    }
    
    /**
     * @param intValue An int value.
     * @return The corresponding instance, or null if none.
     */
    public static SdlPixelFormat valueOf(int intValue) {
        return HELPER.instanceOf(intValue);
    }
}
