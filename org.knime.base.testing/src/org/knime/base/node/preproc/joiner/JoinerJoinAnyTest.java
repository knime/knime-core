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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.base.node.preproc.joiner.Joiner2Settings.CompositionMode;
import org.knime.base.node.preproc.joiner.Joiner2Settings.JoinMode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 *
 * @author Heiko Hofer
 */
public class JoinerJoinAnyTest {
    private ExecutionContext m_exec;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        NodeFactory<NodeModel> dummyFactory =
            (NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);
        m_exec = new ExecutionContext(
                new DefaultNodeProgressMonitor(),
                new Node(dummyFactory),
                    SingleNodeContainer.MemoryPolicy.CacheOnDisc, new HashMap<Integer, ContainerTable>());
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }


    private final void testRunner(final Joiner2Settings settings,
            final Integer[][] reference,
            final int numBitsInitial, final int numBitsMaximal)
            throws CanceledExecutionException, InvalidSettingsException {
        // Create data with fields that consume a lot memory
        DataTable leftInput = new IntegerTable(new String[]{"L1", "L2"},
            new Integer[][]{
                 new Integer[]{0,0}
                ,new Integer[]{0,1}
                ,new Integer[]{1,0}
                ,new Integer[]{1,1}
                ,new Integer[]{2,2}
                ,new Integer[]{3,3}
                ,new Integer[]{4,4}
                ,new Integer[]{5,5}
                ,new Integer[]{6,6}
            }
        );
        DataTable rightInput = new IntegerTable(new String[]{"R1", "R2"},
            new Integer[][]{
                 new Integer[]{0,1}
                ,new Integer[]{1,0}
                ,new Integer[]{1,1}
                ,new Integer[]{1,2}
                ,new Integer[]{2,2}
                ,new Integer[]{3,3}
                ,new Integer[]{10,10}
            }
        );

        BufferedDataTable bdtLeft =
            m_exec.createBufferedDataTable(leftInput, m_exec);
        BufferedDataTable bdtRight =
            m_exec.createBufferedDataTable(rightInput, m_exec);
        // run joiner
        Joiner joiner = new Joiner(leftInput.getDataTableSpec(),
                rightInput.getDataTableSpec(), settings);
        // force one bin only
        joiner.setNumBitsInitial(numBitsInitial);
        joiner.setNumBitsMaximal(numBitsMaximal);
        BufferedDataTable output = joiner.computeJoinTable(bdtLeft, bdtRight,
                m_exec);
        Integer[][] outputArray = toIntegerArray(output);

        // Test for equality of the arrays
        Assert.assertEquals(reference.length, outputArray.length);
        for (int i = 0; i < reference.length; i++) {
            Assert.assertArrayEquals(reference[i], outputArray[i]);
        }

    }

    private Joiner2Settings createBasicSettings() {
        Joiner2Settings settings = new Joiner2Settings();
        settings.setLeftJoinColumns(new String[]{"L1", "L2"});
        settings.setRightJoinColumns(new String[]{"R1", "R2"});
        settings.setCompositionMode(CompositionMode.MatchAny);
        settings.setJoinMode(JoinMode.InnerJoin);
        settings.setRemoveLeftJoinCols(false);
        settings.setRemoveRightJoinCols(false);
        return settings;
    }

    /** Create the reference output depending on join mode.
     */
    private Integer[][] getReference(final JoinMode mode) {
        List<Integer[]> rows = new ArrayList<Integer[]>();
        // the inner join results
        rows.add(new Integer[]{0,0,0,1});
        rows.add(new Integer[]{0,0,1,0});
        rows.add(new Integer[]{0,1,0,1});
        rows.add(new Integer[]{0,1,1,1});
        rows.add(new Integer[]{1,0,1,0});
        rows.add(new Integer[]{1,0,1,1});
        rows.add(new Integer[]{1,0,1,2});
        rows.add(new Integer[]{1,1,0,1});
        rows.add(new Integer[]{1,1,1,0});
        rows.add(new Integer[]{1,1,1,1});
        rows.add(new Integer[]{1,1,1,2});
        rows.add(new Integer[]{2,2,1,2});
        rows.add(new Integer[]{2,2,2,2});
        rows.add(new Integer[]{3,3,3,3});
        if (mode.equals(JoinMode.LeftOuterJoin)
                || mode.equals(JoinMode.FullOuterJoin)) {
            rows.add(new Integer[]{4,4,null,null});
            rows.add(new Integer[]{5,5,null,null});
            rows.add(new Integer[]{6,6,null,null});
        }
        if (mode.equals(JoinMode.RightOuterJoin)
                || mode.equals(JoinMode.FullOuterJoin)) {
            rows.add(new Integer[]{null,null,10,10});
        }
        return rows.toArray(new Integer[0][0]);
    }
    /**
     * Test inner join with one partition.
     * Testcase for Bug 3138 & 3139.
     *
     * @throws CanceledExecutionException when execution is canceled
     * @throws InvalidSettingsException when settings are invalid
     */
    @Test
    public final void testJoinAnyInnerOnePartitionRun()
           throws CanceledExecutionException, InvalidSettingsException {
        Joiner2Settings settings = createBasicSettings();
        settings.setJoinMode(JoinMode.InnerJoin);

        Integer[][] reference = getReference(settings.getJoinMode());

        testRunner(settings, reference, 0, 0);
    }

    /**
     * Test inner join with 64 partitions.
     * Testcase for Bug 3138 & 3139.
     *
     * @throws CanceledExecutionException when execution is canceled
     * @throws InvalidSettingsException when settings are invalid
     */
    @Test
    public final void testJoinAnyInner64PartitionsRun()
           throws CanceledExecutionException, InvalidSettingsException {
        Joiner2Settings settings = createBasicSettings();
        settings.setJoinMode(JoinMode.InnerJoin);

        Integer[][] reference = getReference(settings.getJoinMode());

        testRunner(settings, reference, 6, Integer.SIZE);
    }


    /**
     * Test left outer join with one partition.
     *
     * @throws CanceledExecutionException when execution is canceled
     * @throws InvalidSettingsException when settings are invalid
     */
    @Test
    public final void testJoinAnyLeftOuterOnePartitionRun()
           throws CanceledExecutionException, InvalidSettingsException {
        Joiner2Settings settings = createBasicSettings();
        settings.setJoinMode(JoinMode.LeftOuterJoin);

        Integer[][] reference = getReference(settings.getJoinMode());

        testRunner(settings, reference, 0, 0);
    }

    /**
     * Test left outer join with 64 partitions.
     *
     * @throws CanceledExecutionException when execution is canceled
     * @throws InvalidSettingsException when settings are invalid
     */
    @Test
    public final void testJoinAnyLeftOuter64PartitionsRun()
           throws CanceledExecutionException, InvalidSettingsException {
        Joiner2Settings settings = createBasicSettings();
        settings.setJoinMode(JoinMode.LeftOuterJoin);

        Integer[][] reference = getReference(settings.getJoinMode());

        testRunner(settings, reference, 6, Integer.SIZE);
    }

    /**
     * Test right outer join with one partition.
     * Testcase for Bug 3138.
     *
     * @throws CanceledExecutionException when execution is canceled
     * @throws InvalidSettingsException when settings are invalid
     */
    @Test
    public final void testJoinAnyRightOuterOnePartitionRun()
           throws CanceledExecutionException, InvalidSettingsException {
        Joiner2Settings settings = createBasicSettings();
        settings.setJoinMode(JoinMode.RightOuterJoin);

        Integer[][] reference = getReference(settings.getJoinMode());

        testRunner(settings, reference, 0, 0);
    }

    /**
     * Test right outer join with 64 partitions.
     * Testcase for Bug 3138.
     *
     * @throws CanceledExecutionException when execution is canceled
     * @throws InvalidSettingsException when settings are invalid
     */
    @Test
    public final void testJoinAnyRightOuter64PartitionsRun()
           throws CanceledExecutionException, InvalidSettingsException {
        Joiner2Settings settings = createBasicSettings();
        settings.setJoinMode(JoinMode.RightOuterJoin);

        Integer[][] reference = getReference(settings.getJoinMode());

        testRunner(settings, reference, 6, Integer.SIZE);
    }

    /**
     * Test full outer join with one partition.
     *
     * @throws CanceledExecutionException when execution is canceled
     * @throws InvalidSettingsException when settings are invalid
     */
    @Test
    public final void testJoinAnyFullOuterOnePartitionRun()
           throws CanceledExecutionException, InvalidSettingsException {
        Joiner2Settings settings = createBasicSettings();
        settings.setJoinMode(JoinMode.FullOuterJoin);

        Integer[][] reference = getReference(settings.getJoinMode());

        testRunner(settings, reference, 0, 0);
    }


    /**
     * Test full outer join with 64 partitions.
     *
     * @throws CanceledExecutionException when execution is canceled
     * @throws InvalidSettingsException when settings are invalid
     */
    @Test
    public final void testJoinAnyFullOuter64PartitionsRun()
            throws CanceledExecutionException, InvalidSettingsException {
        Joiner2Settings settings = createBasicSettings();
        settings.setJoinMode(JoinMode.FullOuterJoin);

        Integer[][] reference = getReference(settings.getJoinMode());

        testRunner(settings, reference, 6, Integer.SIZE);
    }

    /**
     * Get the data of an DataTable as an integer array.
     * @param dataTable the data table
     * @return the data as an integer array
     */
    private Integer[][] toIntegerArray(final BufferedDataTable dataTable) {
        int rowCount = ConvenienceMethods.checkTableSize(dataTable.size());
        int colCount = dataTable.getDataTableSpec().getNumColumns();
        Integer[][] data = new Integer[rowCount][colCount];
        RowIterator iter = dataTable.iterator();
        int r = 0;
        while (iter.hasNext()) {
            DataRow row = iter.next();
            Iterator<DataCell> cellIter = row.iterator();
            int c = 0;
            while (cellIter.hasNext()) {
                DataCell cell = cellIter.next();
                data[r][c] = !cell.isMissing()
                    ? ((IntCell)cell).getIntValue()
                    : null;
                c++;
            }
            r++;
        }
        return data;
    }

    /**
     * A DataTable with only integer entries.
     *
     * @author Heiko Hofer
     */
    private static class IntegerTable implements DataTable {
        private final String[] m_colums;
        private final Integer[][] m_data;


        /**
         * @param colums the colum names
         * @param data the data
         */
        public IntegerTable(final String[] colums, final Integer[][] data) {
            super();
            m_colums = colums;
            m_data = data;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataTableSpec getDataTableSpec() {
            DataType[] dataTypes = new DataType[m_colums.length];
            for (int i = 0; i < m_colums.length; i++) {
                dataTypes[i] = IntCell.TYPE;
            }
            return new DataTableSpec("IntegerTable",
                    m_colums, dataTypes);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RowIterator iterator() {
            return new TestDataIterator(m_colums.length, m_data);
        }

        private static class TestDataIterator extends RowIterator {
            private int m_count;
            private int m_numCols;
            private final Integer[][] m_data;

            public TestDataIterator(final int numCols, final Integer[][] data) {
                m_data = data;
                m_count = 0;
                m_numCols = numCols;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasNext() {
                return m_data.length > m_count;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataRow next() {
                DataCell[] cells = new DataCell[m_numCols];
                for (int i = 0; i < cells.length; i++) {
                    if (null == m_data[m_count][i]) {
                        cells[i] = DataType.getMissingCell();
                    } else {
                        cells[i] = new IntCell(m_data[m_count][i]);
                    }
                }
                RowKey rowID = RowKey.createRowKey(m_count);
                m_count++;
                return new DefaultRow(rowID, cells);
            }
        }

    }

}
