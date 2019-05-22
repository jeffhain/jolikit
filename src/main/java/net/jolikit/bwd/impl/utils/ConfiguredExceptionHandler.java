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
package net.jolikit.bwd.impl.utils;

import java.lang.Thread.UncaughtExceptionHandler;

import net.jolikit.lang.LangUtils;

/**
 * For actually used exception handler to always be retrieved from configuration,
 * so that we can configure it in test cases,
 * and without direct dependency to binding config.
 */
public class ConfiguredExceptionHandler implements UncaughtExceptionHandler {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final BaseBwdBindingConfig bindingConfig;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ConfiguredExceptionHandler(BaseBwdBindingConfig bindingConfig) {
        this.bindingConfig = LangUtils.requireNonNull(bindingConfig);
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        this.bindingConfig.getExceptionHandler().uncaughtException(thread, throwable);
    }
}
