/*
 * Copyright 2019-2021 Jeff Hain
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import net.jolikit.build.JlkBinConfig;
import net.jolikit.bwd.impl.jfx.JfxBwdBinding;
import net.jolikit.bwd.impl.jfx.JfxBwdBindingConfig;
import net.jolikit.bwd.test.cases.XxxBwdTestCaseHomeProvider;
import net.jolikit.bwd.test.mains.utils.BwdBindingLaunchUtils;
import net.jolikit.bwd.test.mains.utils.BwdMainLaunchUtils;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBindingMainLaunchInfo;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHome;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHomeProvider;
import net.jolikit.bwd.test.utils.InterfaceMainLaunchInfo;
import net.jolikit.test.utils.TestUtils;

/**
 * Main to launch test cases with JavaFX.
 * 
 * On Mac, need to relaunch with fresh classpath
 * (maybe due to SWT being in IDE classpath)
 * for Application.main(...) to even be called,
 * so we extend Application from another class,
 * which we (re)launch from the main of this POJO.
 * 
 * TODO jfx If using
 * -Djavafx.pulseLogger=true
 * -Dprism.debug=true
 * -Dprism.verbose=true
 * -Dprism.trace=true
 * we get an exception and display doesn't update properly (???).
 * 
 * TODO jfx From Java 9, can also start JavaFX with Platform.startup(Runnable),
 * cf. https://bugs.openjdk.java.net/browse/JDK-8090585.
 */
public class JfxBoundBwdMain {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    protected static final String BINDING_NAME = "JavaFX";
    
    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------
    
    public static class JavaFxMain extends Application implements InterfaceBindingMainLaunchInfo {
        public JavaFxMain() {
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
            return JlkBinConfig.getSpecificClasspathListForJfx();
        }
        @Override
        public String getSpecificJvmArgs() {
            return JlkBinConfig.getSpecificJvmArgsForJfx();
        }
        public static void main(String[] args) {
            final JavaFxMain instance = new JavaFxMain();
            
            final InterfaceBwdTestCaseHomeProvider testCaseHomeProvider = new XxxBwdTestCaseHomeProvider();
            
            instance.launchTheBindingWithTestCaseFromArgs(args, testCaseHomeProvider);
        }
        @Override
        public void start(Stage primaryStage) throws Exception {
            // Not doing anything with primary stage,
            // we create our own stages on hosts creations.
        }
        protected void launchTheBindingWithTestCaseFromArgs(
                final String[] args,
                final InterfaceBwdTestCaseHomeProvider testCaseHomeProvider) {
            
            final InterfaceBwdTestCaseHome testCaseHome = BwdBindingLaunchUtils.getTestCaseHome(
                    args,
                    testCaseHomeProvider);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    final JfxBwdBindingConfig bindingConfig = new JfxBwdBindingConfig(
                            BwdTestUtils.getDecorationInsets());
                    
                    BwdBindingLaunchUtils.setParallelizerParallelism(testCaseHome, bindingConfig);

                    /*
                     * XXX jfx Can have an IllegalStateException (using JDK 8u25)
                     * if doing this out of UI thread, due to usage of
                     * Screen.getPrimary().getDpi() during JfxBwdFontHome construction.
                     */
                    final JfxBwdBinding binding = new JfxBwdBinding(bindingConfig);
                    
                    /*
                     * 
                     */
                    
                    BwdBindingLaunchUtils.launchBindingWithTestCase(args, binding, BINDING_NAME, testCaseHome);
                }
            });
        
            /*
             * 
             */
            
            /*
             * Launching JavaFX application.
             * Needs to be done if wanting to have JavaFX hosts around.
             * Completes when JavaFX application terminates, such as due to a call
             * to Platform.exit(), or closing all stages with implicit exit.
             */
            try {
                launch(args);
            } catch (IllegalStateException e) {
                /*
                 * TODO jfx
                 * Having that exception (JDK 1.8.0_40-ea-b22) after closing JavaFX windows,
                 * and then closing a DOCUMENT_MODAL JDialog
                 * (no exception if MODELESS or the DOCUMENT_MODAL window is a java.awt.Dialog).
    java.lang.IllegalStateException: Toolkit has exited
     at com.sun.javafx.application.PlatformImpl.runAndWait(PlatformImpl.java:333)
     at com.sun.javafx.application.PlatformImpl.runAndWait(PlatformImpl.java:307)
     at com.sun.javafx.application.LauncherImpl.launchApplication1(LauncherImpl.java:816)
     at com.sun.javafx.application.LauncherImpl.lambda$launchApplication$152(LauncherImpl.java:182)
     at com.sun.javafx.application.LauncherImpl$$Lambda$130/1327763628.run(Unknown Source)
     at java.lang.Thread.run(Thread.java:745)
                 */
                e.printStackTrace(System.out);
            } finally {
                System.out.println("done");
            }
            System.out.println(TestUtils.getJVMInfo());
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public JfxBoundBwdMain() {
    }
    
    public static void main(String[] args) {
        final InterfaceMainLaunchInfo mainLaunchInfo = new JavaFxMain();
        
        final BwdMainLaunchUtils mainLaunchUtils = new BwdMainLaunchUtils(
                JlkBinConfig.getJavaPath());
        mainLaunchUtils.launchMainInANewJvmWithMritjAddedAndKillGui(mainLaunchInfo, args);
    }
}
