/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   16.04.2010 (hofer): created
 */
package org.knime.base.node.preproc.joiner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.knime.base.node.preproc.joiner.Joiner2Settings.JoinMode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 *
 * @author Heiko Hofer
 */
public class JoinerTest {
    private ExecutionContext m_exec;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        NodeFactory<NodeModel> dummyFactory =
            (NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);
        m_exec =
            new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(dummyFactory),
                SingleNodeContainer.MemoryPolicy.CacheOnDisc, new HashMap<Integer, ContainerTable>());
    }

    /**
     * Checks whether an inner join works as expected when the number of partitions must be increased due to low memory.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public final void testIncreaseNumPartitionsInnerJoin() throws Exception {

        Joiner2Settings settingsRef = createReferenceSettings("Data");
        Joiner2Settings settingsTest = createReferenceSettings("Data");

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);

        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }

    /**
     * Checks whether a left outer join works as expected when the number of partitions must be increased due to low
     * memory.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public final void testIncreaseNumPartitionsLeftOuterJoin() throws Exception {

        Joiner2Settings settingsRef = createReferenceSettings("Data");
        settingsRef.setJoinMode(JoinMode.LeftOuterJoin);

        Joiner2Settings settingsTest = createReferenceSettings("Data");
        settingsTest.setJoinMode(JoinMode.LeftOuterJoin);

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);

        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }

    /**
     * Checks whether a right outer join works as expected when the number of partitions must be increased due to low
     * memory.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public final void testIncreaseNumPartitionsRightOuterJoin() throws Exception {

        Joiner2Settings settingsRef = createReferenceSettings("Data");
        settingsRef.setJoinMode(JoinMode.RightOuterJoin);

        Joiner2Settings settingsTest = createReferenceSettings("Data");
        settingsTest.setJoinMode(JoinMode.RightOuterJoin);

        // Create data with fields that consume a lot memory

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);
        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }

    /**
     * Checks whether a right outer join works as expected when the number of partitions must be increased due to low
     * memory.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public final void testIncreaseNumPartitionsFullOuterJoin() throws Exception {

        Joiner2Settings settingsRef = createReferenceSettings("Data");
        settingsRef.setJoinMode(JoinMode.FullOuterJoin);

        Joiner2Settings settingsTest = createReferenceSettings("Data");
        settingsTest.setJoinMode(JoinMode.FullOuterJoin);

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);
        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);
        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }

    @Test
    public final void testSkipPartitionsInnerJoin() throws Exception {

        Joiner2Settings settingsRef = createReferenceSettings("Data");
        Joiner2Settings settingsTest = createReferenceSettings("Data");

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);
        joinerTest.setNumBitsInitial(8);
        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }


    @Test
    public final void testSkipPartitionsLeftOuterJoin() throws Exception {
        Joiner2Settings settingsRef = createReferenceSettings("Data");
        settingsRef.setJoinMode(JoinMode.LeftOuterJoin);

        Joiner2Settings settingsTest = createReferenceSettings("Data");
        settingsTest.setJoinMode(JoinMode.LeftOuterJoin);

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);
        joinerTest.setNumBitsInitial(8);
        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }


    @Test
    public final void testSkipPartitionsRightOuterJoin() throws Exception {
        Joiner2Settings settingsRef = createReferenceSettings("Data");
        settingsRef.setJoinMode(JoinMode.RightOuterJoin);

        Joiner2Settings settingsTest = createReferenceSettings("Data");
        settingsTest.setJoinMode(JoinMode.RightOuterJoin);

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);
        joinerTest.setNumBitsInitial(8);
        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }


    @Test
    public final void testSkipPartitionsFullOuterJoin() throws Exception {
        Joiner2Settings settingsRef = createReferenceSettings("Data");
        settingsRef.setJoinMode(JoinMode.FullOuterJoin);

        Joiner2Settings settingsTest = createReferenceSettings("Data");
        settingsTest.setJoinMode(JoinMode.FullOuterJoin);

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);
        joinerTest.setNumBitsInitial(8);
        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }

    @Test
    public void testSortPartitionsInnerJoin() throws Exception {
        Joiner2Settings settingsRef = createReferenceSettings(Joiner2Settings.ROW_KEY_IDENTIFIER);
        Joiner2Settings settingsTest = createReferenceSettings(Joiner2Settings.ROW_KEY_IDENTIFIER);
        settingsTest.setMaxOpenFiles(3);

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);
        joinerTest.setNumBitsInitial(0);
        joinerTest.setNumBitsMaximal(6);
        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }


    @Test
    public void testSortPartitionsLeftOuterJoin() throws Exception {
        Joiner2Settings settingsRef = createReferenceSettings(Joiner2Settings.ROW_KEY_IDENTIFIER);
        settingsRef.setJoinMode(JoinMode.LeftOuterJoin);

        Joiner2Settings settingsTest = createReferenceSettings(Joiner2Settings.ROW_KEY_IDENTIFIER);
        settingsTest.setJoinMode(JoinMode.LeftOuterJoin);
        settingsTest.setMaxOpenFiles(3);

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);
        joinerTest.setNumBitsInitial(0);
        joinerTest.setNumBitsMaximal(6);
        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }


    @Test
    public void testSortPartitionsRightOuterJoin() throws Exception {
        Joiner2Settings settingsRef = createReferenceSettings(Joiner2Settings.ROW_KEY_IDENTIFIER);
        settingsRef.setJoinMode(JoinMode.RightOuterJoin);

        Joiner2Settings settingsTest = createReferenceSettings(Joiner2Settings.ROW_KEY_IDENTIFIER);
        settingsTest.setJoinMode(JoinMode.RightOuterJoin);
        settingsTest.setMaxOpenFiles(3);

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);
        joinerTest.setNumBitsInitial(0);
        joinerTest.setNumBitsMaximal(6);
        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }


    @Test
    public void testSortPartitionsFullOuterJoin() throws Exception {
        Joiner2Settings settingsRef = createReferenceSettings(Joiner2Settings.ROW_KEY_IDENTIFIER);
        settingsRef.setJoinMode(JoinMode.FullOuterJoin);

        Joiner2Settings settingsTest = createReferenceSettings(Joiner2Settings.ROW_KEY_IDENTIFIER);
        settingsTest.setJoinMode(JoinMode.FullOuterJoin);
        settingsTest.setMaxOpenFiles(3);

        BufferedDataTable leftTable = m_exec.createBufferedDataTable(new TestData(100, 1), m_exec);
        BufferedDataTable rightTable = m_exec.createBufferedDataTable(new TestData(200, 1), m_exec);

        // run joiner with reference settings
        Joiner joinerRef = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsRef);
        BufferedDataTable reference = joinerRef.computeJoinTable(leftTable, rightTable, m_exec);

        // run joiner with test settings
        Joiner joinerTest = new Joiner(leftTable.getDataTableSpec(), rightTable.getDataTableSpec(), settingsTest);
        joinerTest.setRowsAddedBeforeOOM(10);
        joinerTest.setNumBitsInitial(0);
        joinerTest.setNumBitsMaximal(6);
        BufferedDataTable test = joinerTest.computeJoinTable(leftTable, rightTable, m_exec);
        compareTables(reference, test);
    }


    private Joiner2Settings createReferenceSettings(final String col) {
        Joiner2Settings settingsRef = new Joiner2Settings();
        String[] joinColumns = new String[]{col};
        settingsRef.setLeftJoinColumns(joinColumns);
        settingsRef.setRightJoinColumns(joinColumns);
        return settingsRef;
    }

    private void compareTables(final BufferedDataTable reference, final BufferedDataTable test) {
        // Check if it has the same results as defaultResult
        assertThat("Unequal number of rows in result table", test.getRowCount(), is(reference.getRowCount()));
        RowIterator referenceIter = reference.iterator();
        RowIterator testIter = test.iterator();
        while (referenceIter.hasNext()) {
            DataRow refRow = referenceIter.next();
            DataRow testRow = testIter.next();
            assertThat("Unexpected row key", testRow.getKey(), is(refRow.getKey()));

            Iterator<DataCell> refCell = refRow.iterator();
            Iterator<DataCell> testCell = testRow.iterator();
            while (refCell.hasNext()) {
                assertThat("Unexpected cell in row " + refRow.getKey(), testCell.next(), is(refCell.next()));
            }
        }
    }

    private static class TestData implements DataTable {
        private final int m_size;

        private final int m_randSeed;

        public TestData(final int size, final int randSeed) {
            m_size = size;
            m_randSeed = randSeed;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataTableSpec getDataTableSpec() {
            return new DataTableSpec("TestDataSpec", new String[]{"Index", "Data"}, new DataType[]{IntCell.TYPE,
                StringCell.TYPE});
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RowIterator iterator() {
            return new TestDataIterator(m_size, m_randSeed);
        }

        private static class TestDataIterator extends RowIterator {
            private int m_count;

            private final int m_size;

            private final Random m_rand;

            public TestDataIterator(final int size, final int randSeed) {
                m_rand = new Random(randSeed);
                m_size = size;
                m_count = 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasNext() {
                return m_size > m_count;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataRow next() {
                m_count++;

                return new DefaultRow(Integer.toString(m_count), new IntCell(m_rand.nextInt()), new StringCell(
                    Integer.toString(m_count)));
            }
        }

    }

}
