package net.jolikit.bwd.impl.utils.gprim;

/**
 * Pixel status relative to figure to draw or fill,
 * but regardless of clip.
 */
enum PixelFigStatus {
    /**
     * Means that pixel must be drawn if in clip.
     */
    PIXEL_REQUIRED,
    /**
     * Means that pixel drawing is allowed but not required.
     */
    PIXEL_ALLOWED,
    /**
     * Means that pixel must not be drawn.
     */
    PIXEL_NOT_ALLOWED;
}
