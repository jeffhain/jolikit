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
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.algr5.AlgrBwdBinding;
import net.jolikit.bwd.impl.algr5.AlgrBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.test.cases.XxxBwdTestCaseHomeProvider;
import net.jolikit.bwd.test.mains.utils.BwdBindingLaunchUtils;
import net.jolikit.bwd.test.mains.utils.BwdMainLaunchUtils;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBindingMainLaunchInfo;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHome;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHomeProvider;
import net.jolikit.bwd.test.utils.InterfaceMainLaunchInfo;

/**
 * Main to launch BWD test cases with Allegro5.
 */
public class Algr5BoundBwdMain implements InterfaceBindingMainLaunchInfo {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    protected static final String BINDING_NAME = "Allegro5";

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public Algr5BoundBwdMain() {
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
        return JlkBinConfig.getSpecificClasspathListForAlgr5();
    }

    @Override
    public String getSpecificJvmArgs() {
        return JlkBinConfig.getSpecificJvmArgsForAlgr5();
    }

    public static void main(String[] args) {
        if (BwdMainLaunchUtils.mustRunInThisJvm(args)) {
            final InterfaceBwdTestCaseHomeProvider testCaseHomeProvider = new XxxBwdTestCaseHomeProvider();
            
            launchTheBindingWithTestCaseFromArgs(args, testCaseHomeProvider);
        } else {
            final InterfaceMainLaunchInfo mainLaunchInfo = new Algr5BoundBwdMain();

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

        final double[] pixelRatioOsOverDeviceXyArr = BwdTestUtils.getPixelRatioOsOverDeviceXyArr();

        final GRect screenBounds = BwdTestUtils.getScreenBoundsFromDefaultBinding(
                ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE);

        // Set later from test case.
        final List<String> bonusSystemFontFilePathList = null;
        final AlgrBwdBindingConfig bindingConfig = new AlgrBwdBindingConfig(
                bonusSystemFontFilePathList,
                pixelRatioOsOverDeviceXyArr[0],
                pixelRatioOsOverDeviceXyArr[1],
                screenBounds,
                BwdTestUtils.getDecorationInsetsAlgr5());
        
        BwdBindingLaunchUtils.setParallelizerParallelism(testCaseHome, bindingConfig);
        BwdBindingLaunchUtils.setInternalParallelism(testCaseHome, bindingConfig);

        final AlgrBwdBinding binding = new AlgrBwdBinding(bindingConfig);

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
