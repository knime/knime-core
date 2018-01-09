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

import static org.junit.Assert.assertEquals;

import java.util.BitSet;

import org.apache.commons.math.random.RandomData;
import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.data.TestDataGenerator;

import com.google.common.collect.Lists;

/**
 * Contains unit tests for the {@link SubsetNoReplacementSelector} implementation of the {@link SubsetSelector}
 * interface.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class SubsetNoReplacementSelectorTest {

    @Test
    public void testSelectValidParameters() throws Exception {
        final RandomData rd = TestDataGenerator.createRandomData();
        final SubsetNoReplacementSelector selector = SubsetNoReplacementSelector.getInstance();
        for (int i = 1; i <= 20; i++) {
            SubsetNoReplacementRowSample sample = selector.select(rd, 20, i);
            int included = sample.getIncludedBitSet().cardinality();
            assertEquals("The sample was expected to contain " + i + "rows but contained " + included + "rows instead.",
                i, included);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testSelectRdNull() throws Exception {
        SubsetNoReplacementSelector.getInstance().select(null, 10, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectNrTotalSmallerZero() throws Exception {
        final RandomData rd = TestDataGenerator.createRandomData();
        SubsetNoReplacementSelector.getInstance().select(rd, -2, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectNrSelectSmallerZero() throws Exception {
        final RandomData rd = TestDataGenerator.createRandomData();
        SubsetNoReplacementSelector.getInstance().select(rd, 10, -5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectNrTotalSmallerNrSelect() throws Exception {
        final RandomData rd = TestDataGenerator.createRandomData();
        SubsetNoReplacementSelector.getInstance().select(rd, 10, 20);
    }

    @Test
    public void testCombine() throws Exception {
        SubsetNoReplacementSelector selector = SubsetNoReplacementSelector.getInstance();
        BitSet bs1 = new BitSet(5);
        bs1.set(0);
        bs1.set(2);
        bs1.set(4);
        BitSet bs2 = new BitSet(10);
        bs2.set(5);
        bs2.set(7);
        bs2.set(8);
        bs2.set(9);
        SubsetNoReplacementRowSample sample1 = new SubsetNoReplacementRowSample(bs1, 5, 0.6);
        SubsetNoReplacementRowSample sample2 = new SubsetNoReplacementRowSample(bs2, 10, 0.4);
        int[] offsets = new int[]{0, 5};
        SubsetNoReplacementRowSample combined = selector.combine(Lists.newArrayList(sample1, sample2), offsets, 15);
        assertEquals("The number of rows in the combined Sample is not correct.", 7,
            combined.getIncludedBitSet().cardinality());
        assertEquals(
            "The total number of rows (the size of the set we draw from) is not correct in the combined sample.", 15,
            combined.getNrRows());
    }

}
