/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.impl.misc;

import net.jolikit.build.JlkBinConfig;
import net.jolikit.bwd.impl.awt.AwtBwdBinding;
import net.jolikit.bwd.impl.awt.AwtBwdBindingConfig;
import net.jolikit.lang.OsUtils;
import net.jolikit.lang.RuntimeExecHelper;

/**
 * Uses SampleBwd with AWT.
 */
public class SampleBwdWithAwtMain {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Where compiled classes of this library can be found.
     */
    private static final String CLASSES_DIR = JlkBinConfig.getAutoCompDirPath();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        
        if (OsUtils.isMac() && (args.length == 0)) {
            final String JAVA_PATH = JlkBinConfig.getJavaPath();
            final String cmd =
                    JAVA_PATH
                    + " -cp " + CLASSES_DIR + " "
                    + SampleBwdWithAwtMain.class.getName()
                    + " antiRelaunchArg";
            
            System.out.println("relaunching");
            
            RuntimeExecHelper.execAsync(
                    cmd,
                    System.out,
                    System.out);
            
            return;
        }

        System.out.println("launching");

        final AwtBwdBindingConfig bindingConfig = new AwtBwdBindingConfig();

        final AwtBwdBinding binding = new AwtBwdBinding(bindingConfig);

        SampleBwd.playWithBinding(binding);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private SampleBwdWithAwtMain() {
    }
}
