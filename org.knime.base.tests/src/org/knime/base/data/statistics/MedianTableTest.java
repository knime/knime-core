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
 */
package org.knime.base.data.statistics;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Random;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.base.node.preproc.sorter.SorterNodeFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.SingleNodeContainer;

/**
 * @author Gabor
 *
 */
public class MedianTableTest {
    private static ExecutionContext EXEC_CONTEXT;
    private BufferedDataTable smallTable;
    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        EXEC_CONTEXT = new ExecutionContext(
           new DefaultNodeProgressMonitor(), new Node(new SorterNodeFactory()),
                    SingleNodeContainer.MemoryPolicy.CacheSmallInMemory, new HashMap<Integer, ContainerTable>());
    }


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        Random random = new Random(37);
        DataColumnSpec[] colSpecs = new DataColumnSpec[] {
            new DataColumnSpecCreator("AscendingDouble", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("DescendingDouble", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("ThreeValuesSingleMedian", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("ThreeValuesDifferentMedian", DoubleCell.TYPE).createSpec(),
//            new DataColumnSpecCreator("FourValuesSingleMedian", DoubleCell.TYPE).createSpec(),
//            new DataColumnSpecCreator("FourValuesDifferentMedian", DoubleCell.TYPE).createSpec()
        };
        DataTableSpec spec = new DataTableSpec(colSpecs);
        final BufferedDataContainer container = EXEC_CONTEXT.createDataContainer(spec);
        try {
            int count = 100;
            for (int i = 0; i < count; ++i) {
                int col = 0;
                DataCell[] rowVals = new DataCell[colSpecs.length];
                rowVals[col++] = new DoubleCell(i);
                rowVals[col++] = new DoubleCell(count - i-1);
                rowVals[col++] = i == 4 ? DataType.getMissingCell() : new DoubleCell(i < count / 2 ? 0 : i * 2 >= count + 2 ? 4 : 1);
                rowVals[col++] = new DoubleCell(i < count / 2 ? 0 : i * 2 >= count + 2 ? 4 : 1);
                container.addRowToTable(new DefaultRow(Integer.toString(i), rowVals));
            }
        } finally {
            container.close();
        }

        smallTable = container.getTable();
        NodeLogger.getLogger(getClass()).debug("Contents of test table:");
        for (DataRow row : smallTable) {

            StringBuilder buf = new StringBuilder();
            for (DataCell dataCell : row) {
                buf.append(dataCell + "\t");
            }
            NodeLogger.getLogger(getClass()).debug(buf.toString());
        }
    }

    /**
     * Test method for {@link org.knime.base.data.statistics.MedianTable#medianValues(org.knime.core.node.ExecutionContext)}.
     * @throws CanceledExecutionException
     */
    @Test
    public void testMedianValues() throws CanceledExecutionException {
        double[] medianValues = new MedianTable(smallTable, new int[] {0, 1, 2, 3}).medianValues(EXEC_CONTEXT);
        assertEquals(49.5, medianValues[0], 1E-20);
        assertEquals(49.5, medianValues[1], 1E-20);
        assertEquals(1, medianValues[2], 1E-20);
        assertEquals(.5, medianValues[3], 1E-20);
    }

}
