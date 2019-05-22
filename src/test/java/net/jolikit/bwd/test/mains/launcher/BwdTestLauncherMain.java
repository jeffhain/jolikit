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
package net.jolikit.bwd.test.mains.launcher;

import net.jolikit.build.JlkBinConfig;
import net.jolikit.bwd.test.cases.XxxBwdTestCaseHomeProvider;
import net.jolikit.bwd.test.mains.XxxBoundBwdMainLaunchInfo;
import net.jolikit.bwd.test.mains.launcher.gui.BwdTestLauncherGui;
import net.jolikit.bwd.test.mains.utils.BwdMainLaunchUtils;
import net.jolikit.bwd.test.utils.DefaultMainLaunchInfo;
import net.jolikit.bwd.test.utils.InterfaceMainLaunchInfo;

/**
 * Launches a little GUI to choose which BWD binding(s)
 * and BWD test case(s) to launch.
 * 
 * On Mac, can run KillBwdTestJvmsOnMacMain to cleanup
 * the eventual remaining mess after crashes or such.
 */
public class BwdTestLauncherMain extends DefaultMainLaunchInfo {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final String GUI_TITLE = "BWD test Launcher GUI";
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public BwdTestLauncherMain() {
    }
    
    public static void main(String[] args) {
        if (BwdMainLaunchUtils.mustRunInThisJvm(args)) {
            final XxxBwdTestCaseHomeProvider testCaseHomeProvider = new XxxBwdTestCaseHomeProvider();
            
            final BwdTestLauncherGui gui = new BwdTestLauncherGui(
                    GUI_TITLE,
                    XxxBoundBwdMainLaunchInfo.getAllMainLaunchInfoListList(),
                    testCaseHomeProvider.getHomeColumnList(),
                    //
                    JlkBinConfig.getJavaPath());
            
            gui.launchGui();
        } else {
            final InterfaceMainLaunchInfo mainLaunchInfo = new BwdTestLauncherMain();
            
            final BwdMainLaunchUtils mainLaunchUtils = new BwdMainLaunchUtils(
                    JlkBinConfig.getJavaPath());
            mainLaunchUtils.launchMainInANewJvmWithMritjAdded(mainLaunchInfo, args);
        }
    }
}
