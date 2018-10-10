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
package org.knime.core.data.sort;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
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
public class TableSorterTest {
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

    /**
     * @throws CanceledExecutionException
     */
    @Test
    public final void testLowMemoryRun() throws CanceledExecutionException {
        runMemoryTest(100, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Test if merge of more buffers than maxOpenBuffers works.
     * @throws CanceledExecutionException
     */
    @Test
    public final void testMultiStageMerge() throws CanceledExecutionException {
        runMemoryTest(100, 5, 8);
    }

    private void runMemoryTest(final int numRows,
            final int maxNumRowsPerContainer,
            final int maxOpenContainers) throws CanceledExecutionException {
        // Create data with fields that consume a lot memory
        DataTable inputTable = new TestData(numRows, 1);

        BufferedDataTable bdt =
            m_exec.createBufferedDataTable(inputTable, m_exec);
        BufferedDataTableSorter sorter =
            new BufferedDataTableSorter(bdt, Arrays.asList("Index"),
                    new boolean[] {true});
        sorter.setMaxOpenContainers(maxOpenContainers);
        BufferedDataTable defaultResult = sorter.sort(m_exec);


        sorter.setMaxRows(maxNumRowsPerContainer);
        // 10MB free memory
        long currentlyUsed = MemoryAlertSystem.getUsedMemory();
        double fraction = Math.min(1, (currentlyUsed + (10 << 20)) / (double)MemoryAlertSystem.getMaximumMemory());

        MemoryAlertSystem.getInstance().setFractionUsageThreshold(fraction);
        try {
            sorter.setMemService(MemoryAlertSystem.getInstance());

            // run again with change settings
            BufferedDataTable result = sorter.sort(m_exec);

            // Check if column is sorted in ascending order
            int prevValue = Integer.MIN_VALUE;
            for (DataRow row : result) {
                int thisValue = ((IntValue)row.getCell(0)).getIntValue();
                Assert.assertTrue(thisValue >= prevValue);
            }
            // Check if it has the same results as defaultResult
            Assert.assertTrue(defaultResult.getRowCount() == result.getRowCount());
            RowIterator defaultIter = defaultResult.iterator();
            RowIterator iter = result.iterator();
            while (defaultIter.hasNext()) {
                DataRow defaultRow = defaultIter.next();
                DataRow row = iter.next();
                Assert.assertTrue(defaultRow.getKey().getString().equals(
                        row.getKey().getString()));
                Iterator<DataCell> defaultCellIter = defaultRow.iterator();
                Iterator<DataCell> cellIter = row.iterator();
                while (defaultCellIter.hasNext()) {
                    Assert.assertTrue(
                            defaultCellIter.next().equals(cellIter.next()));
                }

            }
        } finally {
            MemoryAlertSystem.getInstance().setFractionUsageThreshold(MemoryAlertSystem.DEFAULT_USAGE_THRESHOLD);
        }
    }

    private static class TestData implements DataTable {
        private final int m_size;
        private final int m_randSeed;

        public TestData(final int size,  final int randSeed) {
             m_size = size;
             m_randSeed = randSeed;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataTableSpec getDataTableSpec() {
            return new DataTableSpec("TestDataSpec",
                    new String[]{"Index", "Data"},
                    new DataType[]{IntCell.TYPE, StringCell.TYPE});
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
            private final int m_strCellSize;

            public TestDataIterator(final int size, final int randSeed) {
                m_rand = new Random(randSeed);
                // every output row has size * 2 bytes
                m_strCellSize = 100000;
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
                m_count ++;

                char[] output = new char[m_strCellSize];
                for (int i = 0; i < m_strCellSize; i++) {
                  // number of visible characters in ascii
                  int visCount = 0x007E - 0x0020;
                  // random integer with next >= 0 and next <= visCount
                  int next = m_rand.nextInt(visCount);
                  // space (0x0020) and visible characters, only
                  next += 0x0020;
                  output[i] = (char) next;
                }

                return new DefaultRow(Integer.toString(m_count),
                        new IntCell(m_rand.nextInt()),
                        new StringCell(new String(output)));
            }
        }

    }

}
