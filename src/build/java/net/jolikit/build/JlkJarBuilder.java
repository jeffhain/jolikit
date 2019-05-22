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
package net.jolikit.build;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import net.jadecy.comp.JavacHelper;
import net.jadecy.comp.JdcFsUtils;
import net.jadecy.names.InterfaceNameFilter;

/**
 * Builds from sources to one jar.
 */
public class JlkJarBuilder {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final String MAIN_SRC_PATH = "src/main/java";

    private static final String COMP_DIR_PATH = "build_comp";
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void buildJar(
            List<String> classpathElementList,
            //
            InterfaceNameFilter classNameFilter,
            String sourceVersion,
            String targetVersion,
            //
            String implementationVersion,
            String jarFileName) {
        if (DEBUG) {
            System.out.println("building " + jarFileName);
        }
        
        final JavacHelper helper = new JavacHelper(
                sourceVersion,
                targetVersion,
                classpathElementList);
        
        final File compDir = new File(COMP_DIR_PATH);

        // To make sure it's empty,
        // for compilation only ensures its existence.
        JdcFsUtils.ensureEmptyDir(compDir);

        /*
         * Works with properly specified source directories.
         */
        final InterfaceNameFilter relativeNameFilter = classNameFilter;
        helper.compile(
                COMP_DIR_PATH,
                relativeNameFilter,
                Arrays.asList(MAIN_SRC_PATH));
        
        final String distDirPath = "dist";
        JdcFsUtils.ensureDir(new File(distDirPath));
        
        final String jarFilePath = distDirPath + "/" + jarFileName;
        JlkJarUtils.createJarFile(
                compDir,
                implementationVersion,
                jarFilePath);
        
        System.out.println("generated " + jarFilePath);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private JlkJarBuilder() {
    }
}
