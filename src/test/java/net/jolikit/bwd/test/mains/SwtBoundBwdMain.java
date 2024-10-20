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
package net.jolikit.bwd.test.mains;

import java.util.List;

import net.jolikit.build.JlkBinConfig;
import net.jolikit.bwd.impl.swt.SwtBwdBinding;
import net.jolikit.bwd.impl.swt.SwtBwdBindingConfig;
import net.jolikit.bwd.test.cases.XxxBwdTestCaseHomeProvider;
import net.jolikit.bwd.test.mains.utils.BwdBindingLaunchUtils;
import net.jolikit.bwd.test.mains.utils.BwdMainLaunchUtils;
import net.jolikit.bwd.test.utils.InterfaceBindingMainLaunchInfo;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHome;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHomeProvider;
import net.jolikit.bwd.test.utils.InterfaceMainLaunchInfo;

/**
 * Main to launch test cases with SWT (4.7).
 */
public class SwtBoundBwdMain implements InterfaceBindingMainLaunchInfo {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    protected static final String BINDING_NAME = "SWT";
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public SwtBoundBwdMain() {
    }

    @Override
    public String getBindingName() {
        return BINDING_NAME;
    }

    @Override
    public String getMainClassName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getSpecificClasspathList() {
        return JlkBinConfig.getSpecificClasspathListForSwt();
    }

    @Override
    public String getSpecificJvmArgs() {
        return JlkBinConfig.getSpecificJvmArgsForSwt();
    }

    public static void main(String[] args) {
        if (BwdMainLaunchUtils.mustRunInThisJvm(args)) {
            final InterfaceBwdTestCaseHomeProvider testCaseHomeProvider = new XxxBwdTestCaseHomeProvider();
            
            /*
             * TODO swt Two binding instances:
             * works on Windows, but not on Mac,
             * where we get that:
             * "Exception in thread "TWIN" org.eclipse.swt.SWTError: Not implemented [multiple displays]
             *     at org.eclipse.swt.SWT.error(Unknown Source)
             *     at org.eclipse.swt.widgets.Display.checkDisplay(Unknown Source)
             *     at org.eclipse.swt.widgets.Display.create(Unknown Source)
             *     at org.eclipse.swt.graphics.Device.<init>(Unknown Source)
             *     at org.eclipse.swt.widgets.Display.<init>(Unknown Source)
             *     at org.eclipse.swt.widgets.Display.<init>(Unknown Source)"
             */
            if (false) {
                final String[] args_final = args;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        launchTheBindingWithTestCaseFromArgs(
                                args_final,
                                testCaseHomeProvider);
                    }
                },"TWIN").start();
            }
            launchTheBindingWithTestCaseFromArgs(args, testCaseHomeProvider);
        } else {
            final InterfaceMainLaunchInfo mainLaunchInfo = new SwtBoundBwdMain();

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

        final InterfaceBwdTestCaseHome testCaseHome = BwdBindingLaunchUtils.getTestCaseHome(
                args,
                testCaseHomeProvider);

        final SwtBwdBindingConfig bindingConfig = new SwtBwdBindingConfig();
        
        BwdBindingLaunchUtils.setParallelizerParallelism(testCaseHome, bindingConfig);
        BwdBindingLaunchUtils.setInternalParallelism(testCaseHome, bindingConfig);

        final SwtBwdBinding binding = new SwtBwdBinding(bindingConfig);

        /*
         * 
         */

        binding.getUiThreadScheduler().execute(new Runnable() {
            @Override
            public void run() {
                BwdBindingLaunchUtils.launchBindingWithTestCase(args, binding, BINDING_NAME, testCaseHome);
            }
        });

        binding.processUntilShutdownUninterruptibly();
    }
}
