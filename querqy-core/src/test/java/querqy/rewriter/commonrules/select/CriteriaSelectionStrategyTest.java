/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Querqy Contributors
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
package querqy.rewriter.commonrules.select;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import querqy.rewriter.commonrules.model.Limit;

import java.util.Collections;

public class CriteriaSelectionStrategyTest {

    @Test
    public void testThatFlatCollectorIsUsedIfThereIsNotMaxCount() {
        final CriteriaSelectionStrategy strategy = new CriteriaSelectionStrategy(new Criteria(new PropertySorting("f1",
                Sorting.SortOrder.ASC), new Limit(-1, true), Collections.emptyList()));
        final TopRewritingActionCollector topRewritingActionCollector = strategy.createTopRewritingActionCollector();
        assertTrue(topRewritingActionCollector instanceof FlatTopRewritingActionCollector);
    }

    @Test
    public void testThatFlatCollectorIsUsedIfCountIsZero() {
        final CriteriaSelectionStrategy strategy = new CriteriaSelectionStrategy(new Criteria(new PropertySorting("f1",
                Sorting.SortOrder.ASC), new Limit(0, true), Collections.emptyList()));
        final TopRewritingActionCollector topRewritingActionCollector = strategy.createTopRewritingActionCollector();
        assertTrue(topRewritingActionCollector instanceof FlatTopRewritingActionCollector);
    }

    @Test
    public void testThatFlatCollectorIsUsedIfLevelsNotSetInLimit() {
        final CriteriaSelectionStrategy strategy = new CriteriaSelectionStrategy(new Criteria(new PropertySorting("f1",
                Sorting.SortOrder.ASC), new Limit(1, false), Collections.emptyList()));
        final TopRewritingActionCollector topRewritingActionCollector = strategy.createTopRewritingActionCollector();
        assertTrue(topRewritingActionCollector instanceof FlatTopRewritingActionCollector);
    }

    @Test
    public void testThatLevelCollectorIsUsedIfLevelsSetInLimit() {
        final CriteriaSelectionStrategy strategy = new CriteriaSelectionStrategy(new Criteria(new PropertySorting("f1",
                Sorting.SortOrder.ASC), new Limit(1, true), Collections.emptyList()));
        final TopRewritingActionCollector topRewritingActionCollector = strategy.createTopRewritingActionCollector();
        assertTrue(topRewritingActionCollector instanceof TopLevelRewritingActionCollector);
    }

}
