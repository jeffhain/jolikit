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
package net.jolikit.bwd.test.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BwdTestCaseHomesColumn {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final String title;
    
    private final List<List<InterfaceBwdTestCaseHome>> homeListList =
            new ArrayList<List<InterfaceBwdTestCaseHome>>();

    private final List<List<InterfaceBwdTestCaseHome>> homeListList_unmod =
            Collections.unmodifiableList(this.homeListList);

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public BwdTestCaseHomesColumn(String title) {
        this.title = title;
    }

    public void addHomeGroup(InterfaceBwdTestCaseHome... homeArr) {
        final List<InterfaceBwdTestCaseHome> homeList = new ArrayList<InterfaceBwdTestCaseHome>();
        for (InterfaceBwdTestCaseHome home : homeArr) {
            homeList.add(home);
        }
        this.homeListList.add(Collections.unmodifiableList(homeList));
    }
    
    public String getTitle() {
        return this.title;
    }

    public List<List<InterfaceBwdTestCaseHome>> getHomeListList() {
        return this.homeListList_unmod;
    }
}
