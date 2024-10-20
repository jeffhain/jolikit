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

import java.io.File;
import java.util.List;

import net.jolikit.build.JlkBinConfig;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.sdl2.SdlBwdBinding;
import net.jolikit.bwd.impl.sdl2.SdlBwdBindingConfig;
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
 * Main to launch test cases with SDL2 (2.0.5).
 */
public class Sdl2BoundBwdMain implements InterfaceBindingMainLaunchInfo {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    protected static final String BINDING_NAME = "SDL2";

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public Sdl2BoundBwdMain() {
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
        return JlkBinConfig.getSpecificClasspathListForSdl2();
    }

    @Override
    public String getSpecificJvmArgs() {
        return JlkBinConfig.getSpecificJvmArgsForSdl2();
    }

    public static void main(String[] args) {
        if (BwdMainLaunchUtils.mustRunInThisJvm(args)) {
            final InterfaceBwdTestCaseHomeProvider testCaseHomeProvider = new XxxBwdTestCaseHomeProvider();
            
            launchTheBindingWithTestCaseFromArgs(args, testCaseHomeProvider);
        } else {
            final InterfaceMainLaunchInfo mainLaunchInfo = new Sdl2BoundBwdMain();

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

        eventuallyLoadLib();

        final SdlBwdBindingConfig bindingConfig;
        {
            // Set later from test case.
            final List<String> bonusSystemFontFilePathList = null;
            bindingConfig = new SdlBwdBindingConfig(
                    bonusSystemFontFilePathList,
                    BwdTestUtils.getDecorationInsets());
            final GRect screenBounds = BwdTestUtils.getScreenBoundsFromDefaultBinding(
                    ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE);
            bindingConfig.setScreenBoundsInOs(screenBounds);
            bindingConfig.setScreenBoundsType(ScreenBoundsType.CONFIGURED);
            
            BwdBindingLaunchUtils.setParallelizerParallelism(testCaseHome, bindingConfig);
            BwdBindingLaunchUtils.setInternalParallelism(testCaseHome, bindingConfig);
        }

        final SdlBwdBinding binding = new SdlBwdBinding(bindingConfig);

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
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void eventuallyLoadLib() {
        if (!SdlBwdBinding.MUST_CALL_IMG_Init_ON_CREATION) {
            final File currentDir = new File(".");
            final String currentDirAbsPath = currentDir.getAbsolutePath();
            for (String dllPath : JlkBinConfig.getToLoadListForSdl2()) {
                // System.load(...) wants an absolute path.
                final String dllAbsolutePath = currentDirAbsPath + "/" + dllPath;
                System.load(dllAbsolutePath);
            }
        }
    }
}
