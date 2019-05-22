package net.jolikit.bwd.impl.utils.images;

import net.jolikit.bwd.api.graphics.InterfaceBwdImage;

/**
 * Designed to remove images, on disposal, from binding's image repository,
 * which must exist so that bindings can dispose images on shut down
 * (unless images dispose method is a no-op).
 */
public interface InterfaceBwdImageDisposalListener {

    public void onImageDisposed(InterfaceBwdImage image);
}
