/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.test.cases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jolikit.bwd.test.cases.unitbenches.FontMethodsBenchBwdTestCase;
import net.jolikit.bwd.test.cases.unitbenches.UiSchedulerAsapBwdTestCase;
import net.jolikit.bwd.test.cases.unitbenches.UiSchedulerTimedBwdTestCase;
import net.jolikit.bwd.test.cases.unitsturds.NoLeakBwdTestCase;
import net.jolikit.bwd.test.cases.unittests.DrawingBenchBwdTestCase;
import net.jolikit.bwd.test.cases.unittests.DrawingCheckAndOverheadBwdTestCase;
import net.jolikit.bwd.test.cases.unittests.FontApiUnitTestBwdTestCase;
import net.jolikit.bwd.test.cases.unittests.FontHomeApiWchUnitTestBwdTestCase;
import net.jolikit.bwd.test.cases.unittests.FontHomeApiWsUnitTestBwdTestCase;
import net.jolikit.bwd.test.cases.unittests.FontMetricsApiUnitTestBwdTestCase;
import net.jolikit.bwd.test.cases.unittests.GraphicsApiUnitTestBwdTestCase;
import net.jolikit.bwd.test.cases.unittests.HostUnitTestBwdTestCase;
import net.jolikit.bwd.test.cases.unittests.UiSchedulerAndThrowUnitTestBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchDirtyFillRectBulkBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchDirtyFillRectBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchDrawImageBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchDrawTextBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchNewChildGBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchPacMiceOpaqBgOpaqFgBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchPacMiceOpaqBgTranspFgBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchPacMiceTranspBgOpaqFgBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchPacMiceTranspBgTranspFgBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchParallelFillBwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchRepaint_0s_BwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchRepaint_1Over30s_BwdTestCase;
import net.jolikit.bwd.test.cases.visualbenches.BenchRepaint_1Over60s_BwdTestCase;
import net.jolikit.bwd.test.cases.visualsturds.ConcurrentFontCreaDispBwdTestCase;
import net.jolikit.bwd.test.cases.visualsturds.ConcurrentImageCreaDispBwdTestCase;
import net.jolikit.bwd.test.cases.visualsturds.ParallelDrawingBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.AllEventsBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.Alpha1LayerWinOpaqueBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.Alpha1LayerWinTranspBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.AlphaNLayersWinOpaqueBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.AlphaNLayersWinTranspBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.ArcDrawFillBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.ClearBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.ClientDirtyPaintTestBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.ClientRepaintTestBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.ClippingBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.CursorManagerBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.DefaultFontBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.DrawOrderingBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.DrawingMethodsCliBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.DrawingMethodsWiBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.FontGlyphTableBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.HelloBindingBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.HostBoundsGripsBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.HostBoundsSetGetBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.HostBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.HostCoordsHugeBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.HostCoordsRegularBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.HostDefaultBoundsBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.ImageFormatsBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.KeyEventsBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.MandelbrotBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.PixelReadFromGraphicsBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.PixelReadFromImageBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.TextAlphaBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.TextCasesBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.TextClippingBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.TextWidthBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.TheoreticalOvalOnGridBwdTestCase;
import net.jolikit.bwd.test.cases.visualtests.UnicodeBwdTestCase;
import net.jolikit.bwd.test.utils.BwdTestCaseHomesColumn;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHome;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHomeProvider;

/**
 * Contains BWD test cases homes.
 */
public class XxxBwdTestCaseHomeProvider implements InterfaceBwdTestCaseHomeProvider {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final List<BwdTestCaseHomesColumn> homesColumnList;
    private final Map<String,InterfaceBwdTestCaseHome> homeByClassName;
    {
        final List<BwdTestCaseHomesColumn> columnList = new ArrayList<BwdTestCaseHomesColumn>();
        {
            final BwdTestCaseHomesColumn column = new BwdTestCaseHomesColumn("Visual Tests");
            columnList.add(column);

            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new HelloBindingBwdTestCase(),
            });

            /*
             * Hosts and clients states and bounds.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new HostBwdTestCase(),
                    new HostDefaultBoundsBwdTestCase(),
                    new HostBoundsGripsBwdTestCase(),
                    new HostBoundsSetGetBwdTestCase(),
                    new HostCoordsRegularBwdTestCase(),
                    new HostCoordsHugeBwdTestCase(),
            });

            /*
             * Client repainting (while playing with the window),
             * and what repaint (dirty rects).
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new ClientRepaintTestBwdTestCase(),
                    new ClientDirtyPaintTestBwdTestCase(),
            });

            /*
             * Drawing (no alpha).
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new ClearBwdTestCase(),
                    new DrawingMethodsCliBwdTestCase(),
                    new DrawingMethodsWiBwdTestCase(),
                    new DrawOrderingBwdTestCase(),
                    new ArcDrawFillBwdTestCase(),
                    new ClippingBwdTestCase(),
                    new TheoreticalOvalOnGridBwdTestCase(),
                    new PixelReadFromGraphicsBwdTestCase(),
                    new PixelReadFromImageBwdTestCase(),
            });

            /*
             * Drawing (alpha).
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new Alpha1LayerWinOpaqueBwdTestCase(),
                    new Alpha1LayerWinTranspBwdTestCase(),
                    new AlphaNLayersWinOpaqueBwdTestCase(),
                    new AlphaNLayersWinTranspBwdTestCase(),
            });

            /*
             * Cursor.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new CursorManagerBwdTestCase(),
            });

            /*
             * Devices (Key/Mouse/Wheel) events.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new AllEventsBwdTestCase(),
                    new KeyEventsBwdTestCase(),
            });

            /*
             * Fonts.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new DefaultFontBwdTestCase(),
                    new FontGlyphTableBwdTestCase(),
                    new TextCasesBwdTestCase(),
                    new TextWidthBwdTestCase(),
                    new UnicodeBwdTestCase(),
                    new TextAlphaBwdTestCase(),
                    new TextClippingBwdTestCase(),
            });

            /*
             * Images from files formats.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new ImageFormatsBwdTestCase(),
            });
            
            /*
             * Writable images.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new MandelbrotBwdTestCase(),
            });
        }
        {
            final BwdTestCaseHomesColumn column = new BwdTestCaseHomesColumn("Visual Benches/Stability");
            columnList.add(column);

            /*
             * Paints Per Second and Frames Per Second,
             * depending on painting delay.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new BenchRepaint_0s_BwdTestCase(),
                    new BenchRepaint_1Over30s_BwdTestCase(),
                    new BenchRepaint_1Over60s_BwdTestCase(),
            });
            
            /*
             * Sub/Child graphics benches.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new BenchNewChildGBwdTestCase(),
            });

            /*
             * Parallel.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new ParallelDrawingBwdTestCase(),
                    new ConcurrentFontCreaDispBwdTestCase(),
                    new ConcurrentImageCreaDispBwdTestCase(),
            });
            
            /*
             * Drawing methods benches.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new BenchDrawTextBwdTestCase(),
                    new BenchDrawImageBwdTestCase(),
            });

            /*
             * Painting benches (with default painting delay).
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new BenchPacMiceOpaqBgOpaqFgBwdTestCase(),
                    new BenchPacMiceOpaqBgTranspFgBwdTestCase(),
                    new BenchPacMiceTranspBgOpaqFgBwdTestCase(),
                    new BenchPacMiceTranspBgTranspFgBwdTestCase(),
                    new BenchDirtyFillRectBwdTestCase(),
                    new BenchDirtyFillRectBulkBwdTestCase(),
                    new BenchParallelFillBwdTestCase(),
            });
        }
        {
            final BwdTestCaseHomesColumn column = new BwdTestCaseHomesColumn("Unit Tests");
            columnList.add(column);

            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new HostUnitTestBwdTestCase(),
            });
            
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new GraphicsApiUnitTestBwdTestCase(),
                    new DrawingCheckAndOverheadBwdTestCase(),
                    new DrawingBenchBwdTestCase(),
            });

            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new UiSchedulerAndThrowUnitTestBwdTestCase(),
            });

            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new FontHomeApiWsUnitTestBwdTestCase(),
                    new FontHomeApiWchUnitTestBwdTestCase(),
                    new FontApiUnitTestBwdTestCase(),
                    new FontMetricsApiUnitTestBwdTestCase(),
            });
        }
        {
            final BwdTestCaseHomesColumn column = new BwdTestCaseHomesColumn("Unit Benches/Stability");
            columnList.add(column);

            /*
             * Font methods benches.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new FontMethodsBenchBwdTestCase(),
            });

            /*
             * Scheduling benches.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new UiSchedulerAsapBwdTestCase(),
                    new UiSchedulerTimedBwdTestCase(),
            });

            /*
             * Memory.
             */
            column.addHomeGroup(new InterfaceBwdTestCaseHome[]{
                    new NoLeakBwdTestCase(),
            });
        }

        final SortedMap<String,InterfaceBwdTestCaseHome> map = new TreeMap<String,InterfaceBwdTestCaseHome>();
        for (BwdTestCaseHomesColumn column : columnList) {
            for (List<InterfaceBwdTestCaseHome> group : column.getHomeListList()) {
                for (InterfaceBwdTestCaseHome home : group) {
                    map.put(home.getClass().getName(), home);
                }
            }
        }
        this.homesColumnList = Collections.unmodifiableList(columnList);
        this.homeByClassName = Collections.unmodifiableSortedMap(map);
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Creates a new instance with its own mock instances.
     */
    public XxxBwdTestCaseHomeProvider() {
    }

    public List<BwdTestCaseHomesColumn> getHomeColumnList() {
        return this.homesColumnList;
    }

    @Override
    public InterfaceBwdTestCaseHome getDefaultHome() {
        final BwdTestCaseHomesColumn column = this.homesColumnList.get(0);
        final List<InterfaceBwdTestCaseHome> homeList = column.getHomeListList().get(0);
        return homeList.get(0);
    }

    @Override
    public InterfaceBwdTestCaseHome getHome(String homeClassName) {
        return this.homeByClassName.get(homeClassName);
    }
}
