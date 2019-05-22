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
package net.jolikit.bwd.impl.lwjgl3;

import java.util.concurrent.atomic.AtomicReference;

import net.jolikit.bwd.impl.utils.AbstractBwdBinding;
import net.jolikit.bwd.impl.utils.basics.PixelCoordsConverter;
import net.jolikit.bwd.impl.utils.events.CmnInputConvState;

/**
 * Class to avoid cyclic dependencies between binding and host.
 */
public abstract class AbstractLwjglBwdBinding extends AbstractBwdBinding {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final AtomicReference<AbstractBwdBinding> SINGLE_INSTANCE_REF =
            new AtomicReference<AbstractBwdBinding>();
    
    private final CmnInputConvState eventsConverterCommonState = new CmnInputConvState();
    
    private final PixelCoordsConverter pixelCoordsConverter;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public AbstractLwjglBwdBinding(LwjglBwdBindingConfig bindingConfig) {
        super(bindingConfig, SINGLE_INSTANCE_REF);
        
        this.pixelCoordsConverter = new PixelCoordsConverter(
                bindingConfig.getPixelRatioOsOverDeviceX(),
                bindingConfig.getPixelRatioOsOverDeviceY());
    }

    @Override
    public LwjglBwdBindingConfig getBindingConfig() {
        return (LwjglBwdBindingConfig) super.getBindingConfig();
    }

    public CmnInputConvState getEventsConverterCommonState() {
        return this.eventsConverterCommonState;
    }
    
    public PixelCoordsConverter getPixelCoordsConverter() {
        return this.pixelCoordsConverter;
    }
}
