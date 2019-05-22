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
package net.jolikit.bwd.impl.misc;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import net.jolikit.build.JlkBinConfig;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.jfx.JfxBwdBinding;
import net.jolikit.bwd.impl.jfx.JfxBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.OsUtils;
import net.jolikit.lang.RuntimeExecHelper;

/**
 * Uses SampleBwd with JavaFX.
 * 
 * On Mac, need to relaunch with fresh classpath
 * (maybe due to SWT being in IDE classpath)
 * for Application.main(...) to even be called,
 * so we extend Application from another class,
 * which we (re)launch from the main of this POJO.
 */
public class SampleBwdWithJavaFxMain {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * Where compiled classes of this library can be found.
     */
    private static final String CLASSES_DIR = JlkBinConfig.getAutoCompDirPath();
    
    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------
    
    public static class JavaFxMain extends Application {
        /**
         * Needs to be public, else getting a NoSuchMethodException.
         */
        public JavaFxMain() {
        }
        public static void main(String[] args) {
            System.out.println("launching");

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    /*
                     * No reliable way to get insets in JavaFX,
                     * depending on nodes states and/or positions and spans
                     * being updated asynchronously, so we specify them here.
                     */
                    final GRect borderRect;
                    if (OsUtils.isMac()) {
                        borderRect = GRect.valueOf(0, 4+19, 0, 0);
                    } else {
                        borderRect = GRect.valueOf(4, 4+19, 4, 4);
                    }
                    final JfxBwdBindingConfig bindingConfig = new JfxBwdBindingConfig(borderRect);

                    final JfxBwdBinding binding = new JfxBwdBinding(bindingConfig);

                    SampleBwd.playWithBinding(binding);
                }
            });

            launch(args);
        }
        @Override
        public void start(Stage primaryStage) throws Exception {
        }
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {

        final String JAVA_PATH = JlkBinConfig.getJavaPath();
        final String cmd =
                JAVA_PATH
                + " -cp " + CLASSES_DIR + " "
                + SampleBwdWithJavaFxMain.JavaFxMain.class.getName()
                + " antiRelaunchArg";

        System.out.println("relaunching");

        RuntimeExecHelper.execAsync(
                cmd,
                System.out,
                System.out);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private SampleBwdWithJavaFxMain() {
    }
}
