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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jadecy.names.InterfaceNameFilter;
import net.jadecy.names.NameFilters;

/**
 * Compiles sources and creates the jar.
 */
public class JlkBuildMain {
    
    /*
     * Building one jar per binding, both for modularity,
     * and for best effort build, without having everything fail
     * if some required library jar is missing.
     * 
     * TODO If want to make a jolikit-all.jar:
     * JEP 238: Multi-Release JAR Files extends the JAR file format to allow
     * multiple, Java-release-specific versions of class/resource files
     * to coexist in the same archive.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    public static final String DIST_PATH = "dist";
    
    /*
     * In compilation order.
     */
    
    private static final List<MyJarBuildInfo> JBI_LIST = new ArrayList<MyJarBuildInfo>();
    static {
        final MyJarBuildInfo jolikitJbi = addedInList(new MyJarBuildInfo(
                "1.6", "1.6", "0.1", "jolikit.jar",
                //
                NameFilters.and(
                        NameFilters.startsWithName("net.jolikit"),
                        NameFilters.not(
                                NameFilters.startsWithName("net.jolikit.bwd.impl"))),
                                //
                                null,
                                null));
        
        final MyJarBuildInfo jolikitBwdImplUtilsJbi = addedInList(new MyJarBuildInfo(
                "1.6", "1.6", "0.1", "jolikit-bwd-impl-utils.jar",
                //
                NameFilters.startsWithName("net.jolikit.bwd.impl.utils"),
                //
                null,
                Arrays.asList(jolikitJbi)));
        
        /*
         * 
         */

        final MyJarBuildInfo jolikitBwdImplAwtJbi = addedInList(new MyJarBuildInfo(
                "1.6", "1.6", "0.1", "jolikit-bwd-impl-awt.jar",
                //
                NameFilters.startsWithName("net.jolikit.bwd.impl.awt"),
                //
                null,
                Arrays.asList(
                        jolikitJbi,
                        jolikitBwdImplUtilsJbi)));
        
        addedInList(new MyJarBuildInfo(
                "1.6", "1.6", "0.1", "jolikit-bwd-impl-swing.jar",
                //
                NameFilters.startsWithName("net.jolikit.bwd.impl.swing"),
                //
                null,
                Arrays.asList(
                        jolikitJbi,
                        jolikitBwdImplUtilsJbi,
                        jolikitBwdImplAwtJbi)));
        
        /*
         * 1.6 works, even though we depend on JavaFX API.
         */
        addedInList(new MyJarBuildInfo(
                "1.6", "1.6", "0.1", "jolikit-bwd-impl-jfx.jar",
                //
                NameFilters.startsWithName("net.jolikit.bwd.impl.jfx"),
                //
                JlkBinConfig.getSpecificClasspathListForJfx(),
                Arrays.asList(
                        jolikitJbi,
                        jolikitBwdImplUtilsJbi)));
        
        /*
         * 
         */
        
        addedInList(new MyJarBuildInfo(
                "1.6", "1.6", "0.1", "jolikit-bwd-impl-swt.jar",
                //
                NameFilters.startsWithName("net.jolikit.bwd.impl.swt"),
                //
                JlkBinConfig.getSpecificClasspathListForSwt(),
                Arrays.asList(
                        jolikitJbi,
                        jolikitBwdImplUtilsJbi)));
        
        // TODO lwjgl Need 1.8 source version, for a lambda.
        addedInList(new MyJarBuildInfo(
                "1.8", "1.8", "0.1", "jolikit-bwd-impl-lwjgl3.jar",
                //
                NameFilters.startsWithName("net.jolikit.bwd.impl.lwjgl3"),
                //
                JlkBinConfig.getSpecificClasspathListForLwjgl3(),
                Arrays.asList(
                        jolikitJbi,
                        jolikitBwdImplUtilsJbi,
                        // AWT needed for fonts.
                        jolikitBwdImplAwtJbi)));
        
        addedInList(new MyJarBuildInfo(
                "1.6", "1.6", "0.1", "jolikit-bwd-impl-jogl.jar",
                //
                NameFilters.startsWithName("net.jolikit.bwd.impl.jogl"),
                //
                JlkBinConfig.getSpecificClasspathListForJogl(),
                Arrays.asList(
                        jolikitJbi,
                        jolikitBwdImplUtilsJbi,
                        // AWT needed for fonts and images.
                        jolikitBwdImplAwtJbi)));
        
        /*
         * 
         */

        addedInList(new MyJarBuildInfo(
                "1.6", "1.6", "0.1", "jolikit-bwd-impl-qtj4.jar",
                //
                NameFilters.startsWithName("net.jolikit.bwd.impl.qtj4"),
                //
                JlkBinConfig.getSpecificClasspathListForQtj4(),
                Arrays.asList(
                        jolikitJbi,
                        jolikitBwdImplUtilsJbi)));

        addedInList(new MyJarBuildInfo(
                "1.6", "1.6", "0.1", "jolikit-bwd-impl-algr5.jar",
                //
                NameFilters.startsWithName("net.jolikit.bwd.impl.algr5"),
                //
                JlkBinConfig.getSpecificClasspathListForAlgr5(),
                Arrays.asList(
                        jolikitJbi,
                        jolikitBwdImplUtilsJbi)));

        addedInList(new MyJarBuildInfo(
                "1.6", "1.6", "0.1", "jolikit-bwd-impl-sdl2.jar",
                //
                NameFilters.startsWithName("net.jolikit.bwd.impl.sdl2"),
                //
                JlkBinConfig.getSpecificClasspathListForSdl2(),
                Arrays.asList(
                        jolikitJbi,
                        jolikitBwdImplUtilsJbi)));
    }

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyJarBuildInfo {
        final String sourceVersion;
        final String targetVersion;
        final String implementationVersion;
        final String jarFileName;
        final InterfaceNameFilter classNameFilter;
        /**
         * Can be null.
         */
        final List<String> specificClasspathList;
        /**
         * Can be null.
         */
        final List<MyJarBuildInfo> depJarBuildInfoList;
        public MyJarBuildInfo(
                String sourceVersion,
                String targetVersion,
                String implementationVersion,
                String jarFileName,
                //
                InterfaceNameFilter classNameFilter,
                List<String> specificClasspathList,
                List<MyJarBuildInfo> depJarBuildInfoList) {
            this.sourceVersion = sourceVersion;
            this.targetVersion = targetVersion;
            this.implementationVersion = implementationVersion;
            this.jarFileName = jarFileName;
            this.classNameFilter = classNameFilter;
            this.specificClasspathList = specificClasspathList;
            this.depJarBuildInfoList = depJarBuildInfoList;
        }
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        for (MyJarBuildInfo jbi : JBI_LIST) {
            try {
                buildJar(jbi);
            } catch (Exception e) {
                // Just printing issues,
                // and keeping up with remaining jars.
                e.printStackTrace();
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private JlkBuildMain() {
    }
    
    private static MyJarBuildInfo addedInList(MyJarBuildInfo jbi) {
        JBI_LIST.add(jbi);
        return jbi;
    }

    private static void buildJar(MyJarBuildInfo jbi) {
        
        final List<String> classpathList = new ArrayList<String>();

        final Set<String> alreadyAddedSet = new HashSet<String>();

        addUniq(classpathList, jbi.specificClasspathList, alreadyAddedSet);
        
        if (jbi.depJarBuildInfoList != null) {
            for (MyJarBuildInfo depJbi : jbi.depJarBuildInfoList) {
                addUniq(classpathList, Arrays.asList(DIST_PATH + "/" + depJbi.jarFileName), alreadyAddedSet);
                addUniq(classpathList, depJbi.specificClasspathList, alreadyAddedSet);
            }
        }

        JlkJarBuilder.buildJar(
                classpathList,
                jbi.classNameFilter,
                jbi.sourceVersion,
                jbi.targetVersion,
                jbi.implementationVersion,
                jbi.jarFileName);
    }
    
    /**
     * @param intoList (in,out)
     * @param fromList (in) Can be null.
     * @param alreadyAddedSet (in,out)
     */
    private static void addUniq(
            List<String> intoList,
            List<String> fromList,
            Set<String> alreadyAddedSet) {
        if (fromList == null) {
            return;
        }
        
        for (String elem : fromList) {
            if (alreadyAddedSet.add(elem)) {
                intoList.add(elem);
            }
        }
    }
}
