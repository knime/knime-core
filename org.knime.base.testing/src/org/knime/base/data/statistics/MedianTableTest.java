/**
 * 
 */
package org.knime.base.data.statistics;

import static org.junit.Assert.*;

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
                    SingleNodeContainer.MemoryPolicy.CacheSmallInMemory,
                    new HashMap<Integer, ContainerTable>());
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
        for (DataRow row : smallTable) {
            for (DataCell dataCell : row) {
                System.out.print(dataCell + "\t");
            }
            System.out.println();
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
