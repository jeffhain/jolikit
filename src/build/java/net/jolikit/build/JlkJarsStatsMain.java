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
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;

import net.jadecy.ElemType;
import net.jadecy.Jadecy;
import net.jadecy.names.InterfaceNameFilter;
import net.jadecy.names.NameFilters;
import net.jadecy.parsing.ParsingFilters;

/**
 * To see what's in dist jars, and how big it is.
 */
public class JlkJarsStatsMain {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final MyStatsLevel STATS_LEVEL = MyStatsLevel.JAR;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private enum MyStatsLevel {
        JAR,
        PACKAGE,
        CLASS
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        
        final boolean mustMergeNestedClasses = true;
        final boolean apiOnly = false;
        final Jadecy jdc = new Jadecy(
                mustMergeNestedClasses,
                apiOnly);
        
        final File distDir = new File(JlkBuildMain.DIST_PATH);
        
        final FilenameFilter fileNameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        };
        final String[] jarFileNameArr = distDir.list(fileNameFilter);
        
        if (jarFileNameArr != null) {
            Arrays.sort(jarFileNameArr);
            
            for (String jarFileName : jarFileNameArr) {
                final File jarFile = new File(distDir.getAbsolutePath() + "/" + jarFileName);
                
                final ParsingFilters parsingFilters = ParsingFilters.defaultInstance();

                printStats(jdc, jarFile, parsingFilters);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void printStats(
            Jadecy jdc,
            File jarFile,
            ParsingFilters parsingFilters) {
        
        jdc.parser().getDefaultPackageData().clear();
        jdc.parser().accumulateDependencies(
                jarFile,
                parsingFilters);
        
        /*
         * 
         */

        final SortedMap<String,Long> byteSizeByPackageName =
                jdc.computeMatches(ElemType.PACKAGE, NameFilters.any());
        
        if (STATS_LEVEL.ordinal() >= MyStatsLevel.PACKAGE.ordinal()) {
            System.out.println("");
            System.out.println("byte size by package/class name:");
        }

        long totalByteSize = 0;
        long totalClassCount = 0;
        
        for (Map.Entry<String,Long> packageEntry : byteSizeByPackageName.entrySet()) {
            final String packageName = packageEntry.getKey();
            final long packageByteSize = packageEntry.getValue();
            
            totalByteSize += packageByteSize;

            if (packageByteSize != 0) {
                final SortedMap<String,Long> byteSizeByPackageClassName =
                        jdc.computeMatches(
                                ElemType.CLASS,
                                new InterfaceNameFilter() {
                                    @Override
                                    public String getPrefix() {
                                        // Package name never empty here.
                                        return packageName + ".";
                                    }
                                    @Override
                                    public boolean accept(String name) {
                                        final int lastDotIndex = name.lastIndexOf('.');
                                        return (lastDotIndex == packageName.length())
                                                && name.startsWith(packageName);
                                    }
                                });

                final int classCount = byteSizeByPackageClassName.size();
                totalClassCount += classCount;

                if (STATS_LEVEL.ordinal() >= MyStatsLevel.PACKAGE.ordinal()) {
                    System.out.println(packageName + " : " + packageByteSize + " (" + classCount + " top level classes)");

                    if (STATS_LEVEL.ordinal() >= MyStatsLevel.CLASS.ordinal()) {
                        for (Map.Entry<String,Long> classEntry : byteSizeByPackageClassName.entrySet()) {
                            final String className = classEntry.getKey();
                            final long classByteSize = classEntry.getValue();
                            System.out.println("    " + className + " : " + classByteSize);
                        }
                    }
                }
            }
        }
        System.out.println(jarFile.getName() + ": " + totalByteSize + " bytes, " + totalClassCount + " top level classes");
    }
}
