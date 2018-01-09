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
 *   06.08.2014 (Marcel Hanser): created
 */
package org.knime.base.data.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Random;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 *
 * @author Marcel Hanser
 */
public class CovarianceMatrixCalculatorTest {
    private static final String DOUBLE_CELL_NAME = "Husten";

    private static final DataTableSpec SPEC_4 = new DataTableSpec(doubleSpec(DOUBLE_CELL_NAME),
        doubleSpec(DOUBLE_CELL_NAME + "1"), doubleSpec(DOUBLE_CELL_NAME + "2"), doubleSpec(DOUBLE_CELL_NAME + "3"));

    private static final DataTableSpec SPEC_2 = new DataTableSpec(doubleSpec(DOUBLE_CELL_NAME),
        doubleSpec(DOUBLE_CELL_NAME + "1"));

    private ExecutionContext m_exec;

    private static final int TEST_TABLE_SIZE = 500;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        @SuppressWarnings({"unchecked", "rawtypes"})
        NodeFactory<NodeModel> dummyFactory =
            (NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);
        m_exec =
            new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(dummyFactory),
                SingleNodeContainer.MemoryPolicy.CacheOnDisc, new HashMap<Integer, ContainerTable>());
    }

    /**
     * Tests the covariance computation on data with missing values
     *
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     */
    @Test
    public void computeCovarianceOfRandomDataWithMissingValues() throws InvalidSettingsException,
        CanceledExecutionException {
        long currentTimeMillis = System.currentTimeMillis();
        System.out.println("Mahalanobis test random seed: " + currentTimeMillis);
        final Random random = new Random(47);

        double[][] data = new double[10][];

        BufferedDataContainer inTableCont = generateData(random, data, SPEC_2);

        // add two rows with missing values, at the end both should be ignored
        DataCell[] row = new DataCell[2];
        row[0] = new DoubleCell(random.nextDouble());
        row[1] = DataType.getMissingCell();
        inTableCont.addRowToTable(new DefaultRow(new RowKey("Missing!1"), row));
        row[1] = new DoubleCell(random.nextDouble());
        row[0] = DataType.getMissingCell();
        inTableCont.addRowToTable(new DefaultRow(new RowKey("Missing!2"), row));

        inTableCont.close();
        BufferedDataTable inTable = inTableCont.getTable();

        //As the missing row should be ignored the test the covariance matrix computation should be the same
        CovarianceMatrixCalculator covMatrixCalculator =
            new CovarianceMatrixCalculator(SPEC_2, SPEC_2.getColumnNames());
        BufferedDataContainer covDataContainer = m_exec.createDataContainer(covMatrixCalculator.getResultSpec());

        RealMatrix covMatrixUnderTest = covMatrixCalculator.computeCovarianceMatrix(m_exec, inTable, covDataContainer);
        covDataContainer.close();

        Covariance covariance = new Covariance(data);
        RealMatrix referenceCovarianceMatrix = covariance.getCovarianceMatrix();

        BufferedDataTable covTableUnderTest = covDataContainer.getTable();

        // The diagonal is the variance which also changes considering missing values...
        // but we check only the part of the covariance matrix at the top right triangle.
        assertCovarianceMatrixEquality(covMatrixUnderTest, referenceCovarianceMatrix, covTableUnderTest, SPEC_2, false);
    }

    /**
     * Computes a set of random double
     *
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     */
    @Test
    public void computeCovarianceOfRandomData() throws InvalidSettingsException, CanceledExecutionException {
        long currentTimeMillis = System.currentTimeMillis();
        System.out.println("Mahalanobis test random seed: " + currentTimeMillis);
        final Random random = new Random(currentTimeMillis);

        double[][] data = new double[TEST_TABLE_SIZE][];

        BufferedDataContainer inTableCont = generateData(random, data, SPEC_4);
        inTableCont.close();
        BufferedDataTable inTable = inTableCont.getTable();

        //test the covariance matrix computation
        CovarianceMatrixCalculator covMatrixCalculator =
            new CovarianceMatrixCalculator(SPEC_4, SPEC_4.getColumnNames());
        BufferedDataContainer covDataContainer = m_exec.createDataContainer(covMatrixCalculator.getResultSpec());

        RealMatrix covMatrixUnderTest = covMatrixCalculator.computeCovarianceMatrix(m_exec, inTable, covDataContainer);
        covDataContainer.close();

        Covariance covariance = new Covariance(data);
        RealMatrix referenceCovarianceMatrix = covariance.getCovarianceMatrix();

        BufferedDataTable covTableUnderTest = covDataContainer.getTable();

        assertCovarianceMatrixEquality(covMatrixUnderTest, referenceCovarianceMatrix, covTableUnderTest, SPEC_4, true);
    }

    /**
     * @param covMatrixUnderTest
     * @param referenceCovarianceMatrix
     * @param covTableUnderTest
     */
    private static void assertCovarianceMatrixEquality(final RealMatrix covMatrixUnderTest,
        final RealMatrix referenceCovarianceMatrix, final BufferedDataTable covTableUnderTest,
        final DataTableSpec spec, final boolean considerVarianceBzwDiagonal) {
        // make sure, that we have data
        assertTrue(covMatrixUnderTest.getColumnDimension() == spec.getNumColumns()
            && covMatrixUnderTest.getRowDimension() == spec.getNumColumns()
            && covTableUnderTest.getRowCount() == spec.getNumColumns());
        assertEquals(referenceCovarianceMatrix.getColumnDimension(), covMatrixUnderTest.getColumnDimension());
        assertEquals(referenceCovarianceMatrix.getRowDimension(), covMatrixUnderTest.getRowDimension());

        int rowIndex = 0;
        // check in memory and data table cov variance matrix
        for (DataRow row : covTableUnderTest) {
            for (int col = 0; col < referenceCovarianceMatrix.getRowDimension(); col++) {
                // variance is on the diagonal
                if (!(col == rowIndex) || considerVarianceBzwDiagonal) {
                    assertEquals("Col: " + col + " Row: " + rowIndex,
                        referenceCovarianceMatrix.getEntry(rowIndex, col), covMatrixUnderTest.getEntry(rowIndex, col),
                        0.00001d);
                    assertEquals(referenceCovarianceMatrix.getEntry(rowIndex, col),
                        ((DoubleValue)row.getCell(col)).getDoubleValue(), 0.00001d);
                }
            }
            rowIndex++;
        }
    }

    private static DataColumnSpec doubleSpec(final String doubleCellName) {
        return new DataColumnSpecCreator(doubleCellName, DoubleCell.TYPE).createSpec();
    }

    /**
     * @param random
     * @param size
     * @param data
     */
    private BufferedDataContainer generateData(final Random random, final double[][] data, final DataTableSpec spec) {
        BufferedDataContainer createDataContainer = m_exec.createDataContainer(spec);
        for (int j = 0; j < data.length; j++) {

            double[] row = new double[spec.getNumColumns()];
            for (int i = 0; i < spec.getNumColumns(); i++) {
                {
                    row[i] = random.nextDouble();
                }
            }
            data[j] = row;
            createDataContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(j), row));
        }

        return createDataContainer;
    }
}
