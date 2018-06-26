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
 *   08.02.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.BitSet;

import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.data.TestDataGenerator;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.sample.row.DefaultRowSample;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;

/**
 * This class contains tests for the root-descendant approach taken for the optimization of the tree building
 *
 * @author Adrian Nembach, KNIME.com
 */
public class RootDescendantDataMembershipsTest {

    @Test
    public void testCreateChildDataMemberships() {
        TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(false);
        TestDataGenerator dataGen = new TestDataGenerator(config);
        TreeData data = dataGen.createTennisData();
        DefaultDataIndexManager indexManager = new DefaultDataIndexManager(data);
        int nrRows = data.getNrRows();
        RowSample rowSample = new DefaultRowSample(nrRows);
        RootDataMemberships rootMemberships = new RootDataMemberships(rowSample, data, indexManager);

        BitSet firstHalf = new BitSet(nrRows);
        firstHalf.set(0, nrRows / 2);
        DataMemberships firstHalfChildMemberships = rootMemberships.createChildMemberships(firstHalf);
        assertThat(firstHalfChildMemberships, instanceOf(BitSetDescendantDataMemberships.class));
        BitSetDescendantDataMemberships bitSetFirstHalfChildMemberships =
            (BitSetDescendantDataMemberships)firstHalfChildMemberships;
        assertEquals(firstHalf, bitSetFirstHalfChildMemberships.getBitSet());

        BitSet firstQuarter = new BitSet(nrRows);
        firstQuarter.set(0, nrRows / 4);
        DataMemberships firstQuarterGrandChild = firstHalfChildMemberships.createChildMemberships(firstQuarter);
        assertThat(firstQuarterGrandChild, instanceOf(BitSetDescendantDataMemberships.class));
        BitSetDescendantDataMemberships bitSetFirstQuarterGrandChild =
            (BitSetDescendantDataMemberships)firstQuarterGrandChild;
        assertEquals(firstQuarter, bitSetFirstQuarterGrandChild.getBitSet());
    }

    @Test
    public void testGetColumnMemberships() {
        TreeEnsembleLearnerConfiguration config = new TreeEnsembleLearnerConfiguration(false);
        TestDataGenerator dataGen = new TestDataGenerator(config);
        TreeData data = dataGen.createTennisData();
        DefaultDataIndexManager indexManager = new DefaultDataIndexManager(data);
        int nrRows = data.getNrRows();
        RowSample rowSample = new DefaultRowSample(nrRows);
        RootDataMemberships rootMemberships = new RootDataMemberships(rowSample, data, indexManager);

        ColumnMemberships rootColMem = rootMemberships.getColumnMemberships(0);
        assertThat(rootColMem, instanceOf(IntArrayColumnMemberships.class));
        assertEquals(nrRows, rootColMem.size());
        int[] expectedOriginalIndices = new int[]{0, 1, 7, 8, 10, 2, 6, 11, 12, 3, 4, 5, 9, 13};
        for (int i = 0; rootColMem.next(); i++) {
            // in this case originalIndex and indexInDataMemberships are the same
            assertEquals(expectedOriginalIndices[i], rootColMem.getIndexInDataMemberships());
            assertEquals(expectedOriginalIndices[i], rootColMem.getIndexInDataMemberships());
            assertEquals(i, rootColMem.getIndexInColumn());
        }

        BitSet lastHalf = new BitSet(nrRows);
        lastHalf.set(nrRows / 2, nrRows);
        DataMemberships lastHalfChild = rootMemberships.createChildMemberships(lastHalf);
        ColumnMemberships childColMem = lastHalfChild.getColumnMemberships(0);
        assertThat(childColMem, instanceOf(DescendantColumnMemberships.class));
        assertEquals(nrRows / 2, childColMem.size());
        expectedOriginalIndices = new int[]{7, 8, 10, 11, 12, 9, 13};
        int[] expectedIndexInColumn = new int[]{2, 3, 4, 7, 8, 12, 13};
        int[] expectedIndexInDataMemberships = new int[]{7, 8, 10, 11, 12, 9, 13};
        for (int i = 0; childColMem.next(); i++) {
            assertEquals(expectedOriginalIndices[i], childColMem.getOriginalIndex());
            assertEquals(expectedIndexInColumn[i], childColMem.getIndexInColumn());
            assertEquals(expectedIndexInDataMemberships[i], childColMem.getIndexInDataMemberships());
        }
    }
}
