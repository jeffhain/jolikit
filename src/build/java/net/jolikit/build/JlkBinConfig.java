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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jolikit.bwd.impl.utils.basics.OsUtils;
import net.jolikit.lang.PropertiesFileUtils;

/**
 * Installation related configuration for binaries.
 * 
 * Useful for build, and executions (tests and samples).
 */
public class JlkBinConfig {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final String CONFIG_FILE_PATH;
    static {
        if (OsUtils.isWindows()) {
            CONFIG_FILE_PATH = "src/build/resources/bin_config-win.properties";
        } else if (OsUtils.isMac()) {
            CONFIG_FILE_PATH = "src/build/resources/bin_config-mac.properties";
        } else {
            CONFIG_FILE_PATH = "src/build/resources/bin_config-fallback.properties";
        }
    }
    
    private static final PropertiesFileUtils PROPERTIES_FILE_UTILS =
            new PropertiesFileUtils(CONFIG_FILE_PATH);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @return Path of java[.exe] executable.
     */
    public static String getJavaPath() {
        return PROPERTIES_FILE_UTILS.getRequiredProperty("JAVA_PATH");
    }

    /**
     * @return Path of directory where auto-compiled classes or jars
     *         of Jolikit can be found.
     */
    public static String getAutoCompDirPath() {
        return PROPERTIES_FILE_UTILS.getRequiredProperty("AUTO_COMP_DIR_PATH");
    }

    /*
     * 
     */
    
    /**
     * @return Classpaths for all of src/build/samples/test code,
     *         to which binding-specific classpaths must be added
     *         to have complete classpath.
     */
    public static List<String> getCommonClasspathList() {
        return getPropValueSplitOnSemiColon("CLASSPATH_COMMON");
    }
    
    /*
     * 
     */
    
    public static List<String> getSpecificClasspathListForAll() {
        final Set<String> uniq = new HashSet<String>();
        final List<String> list = new ArrayList<String>();
        for (Object lizt_ : new Object[]{
                getSpecificClasspathListForJfx(),
                //
                getSpecificClasspathListForSwt(),
                getSpecificClasspathListForLwjgl3(),
                getSpecificClasspathListForJogl(),
                //
                getSpecificClasspathListForQtj4(),
                getSpecificClasspathListForAlgr5(),
                getSpecificClasspathListForSdl2(),
        }) {
            @SuppressWarnings("unchecked")
            final List<String> lizt = (List<String>) lizt_;
            for (String cpe : lizt) {
                if (uniq.add(cpe)) {
                    list.add(cpe);
                }
            }
        }
        return list;
    }

    public static List<String> getSpecificClasspathListForJfx() {
        return getPropValueSplitOnSemiColon("CLASSPATH_JFX");
    }
    
    public static List<String> getSpecificClasspathListForSwt() {
        return getPropValueSplitOnSemiColon("CLASSPATH_SWT");
    }
    
    public static List<String> getSpecificClasspathListForLwjgl3() {
        return getPropValueSplitOnSemiColon("CLASSPATH_LWJGL3");
    }
    
    public static List<String> getSpecificClasspathListForJogl() {
        return getPropValueSplitOnSemiColon("CLASSPATH_JOGL");
    }
    
    public static List<String> getSpecificClasspathListForQtj4() {
        return getPropValueSplitOnSemiColon("CLASSPATH_QTJ4");
    }
    
    public static List<String> getSpecificClasspathListForAlgr5() {
        return getPropValueSplitOnSemiColon("CLASSPATH_ALGR5");
    }
    
    public static List<String> getSpecificClasspathListForSdl2() {
        return getPropValueSplitOnSemiColon("CLASSPATH_SDL2");
    }
    
    /*
     * 
     */
    
    public static String getSpecificJvmArgsForJfx() {
        return getPropValue("JVM_ARGS_JFX");
    }
    
    public static String getSpecificJvmArgsForSwt() {
        return getPropValue("JVM_ARGS_SWT");
    }
    
    public static String getSpecificJvmArgsForLwjgl3() {
        return getPropValue("JVM_ARGS_LWJGL3");
    }
    
    public static String getSpecificJvmArgsForJogl() {
        return getPropValue("JVM_ARGS_JOGL");
    }
    
    public static String getSpecificJvmArgsForQtj4() {
        return getPropValue("JVM_ARGS_QTJ4");
    }
    
    public static String getSpecificJvmArgsForAlgr5() {
        return getPropValue("JVM_ARGS_ALGR5");
    }
    
    public static String getSpecificJvmArgsForSdl2() {
        return getPropValue("JVM_ARGS_SDL2");
    }
    
    /*
     * 
     */
    
    public static List<String> getToLoadListForSdl2() {
        return getPropValueSplitOnSemiColon("TO_LOAD_SDL2");
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private JlkBinConfig() {
    }

    private static List<String> getPropValueSplitOnSemiColon(String propKey) {
        final String concatStr = PROPERTIES_FILE_UTILS.getRequiredProperty(propKey);
        final String[] strArr = concatStr.split(";");
        final List<String> strList = new ArrayList<String>();
        for (String str : strArr) {
            if (str.isEmpty()) {
                // Can cause trouble.
            } else {
                strList.add(str);
            }
        }
        return strList;
    }

    private static String getPropValue(String propKey) {
        return PROPERTIES_FILE_UTILS.getRequiredProperty(propKey);
    }
}
