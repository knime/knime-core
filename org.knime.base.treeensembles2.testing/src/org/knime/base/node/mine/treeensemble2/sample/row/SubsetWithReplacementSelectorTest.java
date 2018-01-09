/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   03.08.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.sample.row;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

import org.apache.commons.math.random.RandomData;
import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.data.TestDataGenerator;

import com.google.common.collect.Lists;

/**
 * Tests the functionality of the {@link SubsetWithReplacementSelector} implementation of the {@link SubsetSelector}
 * interface.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class SubsetWithReplacementSelectorTest {
    @Test
    public void testSelect() throws Exception {
        final SubsetWithReplacementSelector selector = SubsetWithReplacementSelector.getInstance();
        final RandomData rd = TestDataGenerator.createRandomData();

        for (int i = 1; i < 20; i++) {
            SubsetWithReplacementRowSample sample = selector.select(rd, 20, i);
            int included = SamplerTestUtil.countRows(sample);
            assertThat("Unexpected number of included rows", included, is(i));
        }

        SubsetWithReplacementRowSample sample = selector.select(rd, 1000, 1000);
        int uniqueRows = SamplerTestUtil.countUniqueRows(sample);
        assertThat("A bootstrap sample will usually contain about 63.2% of the rows.", uniqueRows, is(lessThan(700)));
    }


    @Test
    public void testCombine() throws Exception {
        int[] rc1 = new int[]{0, 0, 3, 2, 0, 1, 2};
        int[] rc2 = new int[]{1, 2, 0, 0, 0, 3, 0, 1};
        SubsetWithReplacementRowSample sample1 = new SubsetWithReplacementRowSample(rc1);
        SubsetWithReplacementRowSample sample2 = new SubsetWithReplacementRowSample(rc2);
        int[] offsets = new int[]{0, 7};
        SubsetWithReplacementRowSample combined =
            SubsetWithReplacementSelector.getInstance().combine(Lists.newArrayList(sample1, sample2), offsets, 15);
        assertThat("Unexpected number of combined rows", SamplerTestUtil.countRows(combined),
            is(SamplerTestUtil.countRows(sample1) + SamplerTestUtil.countRows(sample2)));
        assertThat("Unexpected number of rows", combined.getNrRows(), is(15));
    }
}
