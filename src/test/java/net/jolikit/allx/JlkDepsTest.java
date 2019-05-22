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
package net.jolikit.allx;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;
import net.jadecy.DepUnit;
import net.jadecy.ElemType;
import net.jadecy.Jadecy;
import net.jadecy.JadecyUtils;
import net.jadecy.cmd.JadecyMain;
import net.jadecy.code.PackageData;
import net.jadecy.names.InterfaceNameFilter;
import net.jadecy.names.NameFilters;
import net.jadecy.parsing.ParsingFilters;
import net.jolikit.build.JlkBinConfig;
import net.jolikit.test.JlkTestCompHelper;

public class JlkDepsTest extends TestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /**
     * To ignore dependencies data related to fontbox,
     * which contains cycles, but which we don't want to touch
     * since it's third party code.
     */
    private static final String FONTBOX_PACKAGE = "net.jolikit.bwd.impl.utils.fontbox";

    private static final InterfaceNameFilter CLASS_NAME_FILTER =
            NameFilters.not(
                    NameFilters.or(
                            NameFilters.startsWith(FONTBOX_PACKAGE)));

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * For debug.
     */
    public static void main(String[] args) {
        if (true) {
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println("classes cycles:");
            JadecyMain.main(new String[]{
                    JlkBinConfig.getAutoCompDirPath(),
                    "-scycles",
                    "-onlystats",
            });
        }
        if (true) {
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println("packages cycles:");
            JadecyMain.main(new String[]{
                    JlkBinConfig.getAutoCompDirPath(),
                    "-packages",
                    "-scycles",
                    "-onlystats",
            });
        }
        if (true) {
            System.out.println();
            System.out.println();
            System.out.println();
            /*
             * To see Jolikit classes that depend on Dbg.
             */
            JadecyMain.main(new String[]{
                    JlkBinConfig.getAutoCompDirPath(),
                    "-depsto",
                    "net\\.jolikit\\.lang\\.Dbg",
                    "-steps",
                    "-maxsteps", "1",
            });
        }
        if (true) {
            /*
             * To see Jolikit classes BWD doesn't depend on.
             */
            System.out.println();
            System.out.println();
            System.out.println();
            final String compDirPath;
            if (false) {
                // Main only.
                compDirPath = JlkTestCompHelper.ensureCompiledAndGetOutputDirPath(
                        Arrays.asList(
                                JlkTestCompHelper.MAIN_SRC_PATH));
            } else {
                // All (main, test, etc.).
                compDirPath = JlkBinConfig.getAutoCompDirPath();
            }
            
            final Jadecy jdc = new Jadecy(true, false);
            jdc.parser().accumulateDependencies(
                    new File(compDirPath),
                    ParsingFilters.defaultInstance());
            
            final InterfaceNameFilter jlkFilter = NameFilters.startsWithName("net.jolikit");
            final Set<String> jlkClassSet =
                    jdc.computeMatches(
                            ElemType.CLASS,
                            jlkFilter).keySet();
            
            final InterfaceNameFilter bwdFilter = NameFilters.startsWithName("net.jolikit.bwd");
            final Set<String> bwdDepClassSet =
                    JadecyUtils.computeDepsMergedFromDepsLm(
                            jdc.computeDeps(
                                    ElemType.CLASS,
                                    bwdFilter,
                                    true,
                                    false,
                                    -1)).keySet();
            
            final TreeSet<String> bwdNoDepClassSet = new TreeSet<String>(jlkClassSet);
            bwdNoDepClassSet.removeAll(bwdDepClassSet);
            System.out.println();
            System.out.println("classes not used by BWD:");
            for (String superfluousClass : bwdNoDepClassSet) {
                System.out.println(superfluousClass);
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * Tests classes dependencies in/of main sources.
     */
    public void test_internalDeps_CLASS_main() {
        final String compDirPath =
                JlkTestCompHelper.ensureCompiledAndGetOutputDirPath(
                        Arrays.asList(
                                JlkTestCompHelper.MAIN_SRC_PATH));
        final DepUnit depUnit = newDepUnit(compDirPath);
        
        /*
         * Negative checks.
         */
        
        // Checking that "main code" class files don't depend on Dbg,
        // which is only meant for debug.
        depUnit.addIllegalDirectDeps(
                ElemType.CLASS,
                NameFilters.startsWithName("net.jolikit"),
                new InterfaceNameFilter[]{
                    NameFilters.startsWithName("net.jolikit.lang.Dbg"),
                });

        /*
         * 
         */

        depUnit.checkDeps(ElemType.CLASS);
    }
    
    /**
     * Tests packages dependencies in/of main sources.
     */
    public void test_internalDeps_PACKAGE_main() {
        final String compDirPath =
                JlkTestCompHelper.ensureCompiledAndGetOutputDirPath(
                        Arrays.asList(
                                JlkTestCompHelper.MAIN_SRC_PATH));
        final DepUnit depUnit = newDepUnit(compDirPath);

        /*
         * Positive checks.
         */

        depUnit.addAllowedDirectDeps(
                ElemType.PACKAGE,
                NameFilters.equalsName("net.jolikit.bwd"),
                new InterfaceNameFilter[]{
                    NameFilters.startsWithName("java.lang"),
                    NameFilters.startsWithName("net.jolikit.lang"),
                    NameFilters.startsWithName("net.jolikit.threading"),
                    NameFilters.startsWithName("net.jolikit.time"),
                });

        depUnit.addAllowedDirectDeps(
                ElemType.PACKAGE,
                NameFilters.startsWithName("net.jolikit.io"),
                new InterfaceNameFilter[]{
                    NameFilters.startsWithName("java.lang"),
                    NameFilters.startsWithName("java.util"),
                    NameFilters.startsWithName("java.io"),
                    NameFilters.startsWithName("java.nio"),
                    NameFilters.startsWithName("net.jolikit.lang"),
                    NameFilters.startsWithName("net.jolikit.threading"),
                    NameFilters.startsWithName("net.jolikit.utils"),
                });

        depUnit.addAllowedDirectDeps(
                ElemType.PACKAGE,
                NameFilters.startsWithName("net.jolikit.lang"),
                new InterfaceNameFilter[]{
                    NameFilters.startsWithName("java.lang"),
                    NameFilters.startsWithName("java.util"),
                    NameFilters.startsWithName("java.io"),
                });

        depUnit.addAllowedDirectDeps(
                ElemType.PACKAGE,
                NameFilters.startsWithName("net.jolikit.threading"),
                new InterfaceNameFilter[]{
                    NameFilters.startsWithName("java.lang"),
                    NameFilters.startsWithName("java.util"),
                    NameFilters.startsWithName("net.jolikit.lang"),
                });

        depUnit.addAllowedDirectDeps(
                ElemType.PACKAGE,
                NameFilters.startsWithName("net.jolikit.utils"),
                new InterfaceNameFilter[]{
                    NameFilters.startsWithName("java.lang"),
                    NameFilters.startsWithName("java.util"),
                    NameFilters.startsWithName("java.io"),
                    NameFilters.startsWithName("net.jolikit.lang"),
                });

        /*
         * Negative checks.
         */

        depUnit.addIllegalDirectDeps(
                ElemType.PACKAGE,
                NameFilters.startsWithName("net.jolikit.bwd.api"),
                new InterfaceNameFilter[]{
                    NameFilters.startsWithName("net.jolikit.bwd.impl"),
                });

        depUnit.addIllegalDirectDeps(
                ElemType.PACKAGE,
                NameFilters.startsWithName("net.jolikit.bwd.api.graphics"),
                new InterfaceNameFilter[]{
                    NameFilters.or(
                            NameFilters.equalsName("net.jolikit.bwd.api"),
                            NameFilters.startsWithName("net.jolikit.bwd.api.events")),
                });

        depUnit.addIllegalDirectDeps(
                ElemType.PACKAGE,
                NameFilters.startsWithName("net.jolikit.bwd.api.events"),
                new InterfaceNameFilter[]{
                    NameFilters.or(
                            NameFilters.equalsName("net.jolikit.bwd.api"),
                            NameFilters.startsWithName("net.jolikit.bwd.api.graphics"),
                            NameFilters.startsWithName("net.jolikit.bwd.api.fonts")),
                });

        depUnit.addIllegalDirectDeps(
                ElemType.PACKAGE,
                NameFilters.startsWithName("net.jolikit.bwd.api.fonts"),
                new InterfaceNameFilter[]{
                    NameFilters.or(
                            NameFilters.equalsName("net.jolikit.bwd.api"),
                            NameFilters.startsWithName("net.jolikit.bwd.api.events"),
                            NameFilters.startsWithName("net.jolikit.bwd.api.graphics")),
                });

        depUnit.addIllegalDirectDeps(
                ElemType.PACKAGE,
                NameFilters.startsWithName("net.jolikit.bwd.api.utils"),
                new InterfaceNameFilter[]{
                    NameFilters.and(
                            NameFilters.not(NameFilters.equalsName("net.jolikit.bwd.api.utils")),
                            NameFilters.startsWithName("net.jolikit.bwd.api")),
                });

        depUnit.addIllegalDirectDeps(
                ElemType.PACKAGE,
                NameFilters.startsWithName("net.jolikit.bwd.impl.utils.basics"),
                new InterfaceNameFilter[]{
                    NameFilters.and(
                            NameFilters.not(NameFilters.startsWithName("net.jolikit.bwd.impl.utils.basics")),
                            NameFilters.startsWithName("net.jolikit.bwd.impl.utils")),
                });

        /*
         * 
         */

        depUnit.checkDeps(ElemType.PACKAGE);
    }
    
    /*
     * 
     */

    /**
     * Tests classes cycles in all sources.
     */
    public void test_cycles_CLASS_all() {
        final String compDirPath = JlkBinConfig.getAutoCompDirPath();
        final DepUnit depUnit = newDepUnit(compDirPath);
        
        clearFontboxDeps(depUnit);

        if (DEBUG) {
            System.out.println();
            System.out.println();
            System.out.println("computing classes cycles in all sources:");
            System.out.println("----------------------------------------");
        }

        addAllowedCycles_CLASS_all(depUnit);

        depUnit.checkShortestCycles(ElemType.CLASS);
    }

    /**
     * Tests packages cycles in main sources.
     */
    public void test_cycles_PACKAGE_main() {
        final String compDirPath =
                JlkTestCompHelper.ensureCompiledAndGetOutputDirPath(
                        Arrays.asList(
                                JlkTestCompHelper.MAIN_SRC_PATH));
        final DepUnit depUnit = newDepUnit(compDirPath);
        
        clearFontboxDeps(depUnit);

        if (DEBUG) {
            System.out.println();
            System.out.println();
            System.out.println("computing packages cycles in main sources:");
            System.out.println("------------------------------------------");
        }

        depUnit.checkShortestCycles(ElemType.PACKAGE);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @return Jadecy with parsing done.
     */
    private static Jadecy newJadecy(String compDirPath) {
        // We don't care about intra-class cycles.
        final boolean mustMergeNestedClasses = true;
        final boolean apiOnly = false;
        final Jadecy jadecy = new Jadecy(
                mustMergeNestedClasses,
                apiOnly);

        ParsingFilters filters = ParsingFilters.defaultInstance();
        filters = filters.withClassNameFilter(CLASS_NAME_FILTER);

        jadecy.parser().accumulateDependencies(
                new File(compDirPath),
                filters);

        return jadecy;
    }

    private static DepUnit newDepUnit(String compDirPath) {
        return new DepUnit(newJadecy(compDirPath));
    }
    
    /*
     * 
     */
    
    private static void clearFontboxDeps(DepUnit depUnit) {
        final PackageData fontboxPd =
                depUnit.jadecy().parser().getDefaultPackageData().getPackageData(FONTBOX_PACKAGE);
        
        fontboxPd.clear();
    }
    
    /*
     * 
     */
    
    /**
     * Adds cycles allowed for classes in all sources.
     */
    private static void addAllowedCycles_CLASS_all(DepUnit depUnit) {
        if (false) {
            // No class cycle yet.
            // Template:
            depUnit.addAllowedCycle(
                    ElemType.CLASS,
                    new String[]{
                            "net.jolikit.lang.Bar",
                            "net.jolikit.lang.Foo",
                    });
        }
    }
}
