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
package net.jolikit.bwd.test.mains;

import net.jolikit.build.JlkBinConfig;
import net.jolikit.bwd.impl.awt.AwtBwdBinding;
import net.jolikit.bwd.impl.awt.AwtBwdBindingConfig;
import net.jolikit.bwd.test.cases.XxxBwdTestCaseHomeProvider;
import net.jolikit.bwd.test.mains.utils.BwdBindingLaunchUtils;
import net.jolikit.bwd.test.mains.utils.BwdMainLaunchUtils;
import net.jolikit.bwd.test.utils.DefaultMainLaunchInfo;
import net.jolikit.bwd.test.utils.InterfaceBindingMainLaunchInfo;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHomeProvider;
import net.jolikit.bwd.test.utils.InterfaceMainLaunchInfo;

/**
 * Main to launch BWD test cases with AWT.
 */
public class AwtBoundBwdMain extends DefaultMainLaunchInfo implements InterfaceBindingMainLaunchInfo {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    protected static final String BINDING_NAME = "AWT";

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AwtBoundBwdMain() {
    }
    
    @Override
    public String getBindingName() {
        return BINDING_NAME;
    }

    public static void main(String[] args) {
        if (BwdMainLaunchUtils.mustRunInThisJvm(args)) {
            final InterfaceBwdTestCaseHomeProvider testCaseHomeProvider = new XxxBwdTestCaseHomeProvider();
            
            launchTheBindingWithTestCaseFromArgs(args, testCaseHomeProvider);
        } else {
            final InterfaceMainLaunchInfo mainLaunchInfo = new AwtBoundBwdMain();

            final BwdMainLaunchUtils mainLaunchUtils = new BwdMainLaunchUtils(
                    JlkBinConfig.getJavaPath());
            mainLaunchUtils.launchMainInANewJvmWithMritjAddedAndKillGui(mainLaunchInfo, args);
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected static void launchTheBindingWithTestCaseFromArgs(
            final String[] args,
            final InterfaceBwdTestCaseHomeProvider testCaseHomeProvider) {
        
        final AwtBwdBindingConfig bindingConfig = new AwtBwdBindingConfig();
        
        final AwtBwdBinding binding = new AwtBwdBinding(bindingConfig);
        
        /*
         * 
         */
        
        binding.getUiThreadScheduler().execute(new Runnable() {
            @Override
            public void run() {
                BwdBindingLaunchUtils.launchBindingWithTestCase(args, binding, BINDING_NAME, testCaseHomeProvider);
            }
        });
    }
}
