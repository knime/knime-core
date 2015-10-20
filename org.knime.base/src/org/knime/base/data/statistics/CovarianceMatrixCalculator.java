/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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

import static org.knime.core.node.util.CheckUtils.checkArgument;
import static org.knime.core.node.util.CheckUtils.checkSetting;
import static org.knime.core.node.util.CheckUtils.checkSettingNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.StorelessCovariance;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;

/**
 * Utility class which computes the covariance matrix for a given BufferedDataTable considering missing values. The
 * algorithms uses the {@link StorelessCovariance} of Apache and therefore traverses the data once and does not require
 * the input data to be read completely in memory.
 *
 * @author Marcel Hanser
 * @since 2.11
 */
public final class CovarianceMatrixCalculator {

    private final DataTableSpec m_resultSpec;

    private final DataTableSpec m_targetSpec;

    private final int[] m_indexes;

    /**
     * @param targetSpec the target spec
     * @param columns to include
     * @throws InvalidSettingsException if the included columns do not exist or are not compatible with
     *             {@link DoubleValue}
     */
    public CovarianceMatrixCalculator(final DataTableSpec targetSpec, final String... columns)
        throws InvalidSettingsException {
        m_targetSpec = targetSpec;
        List<DataColumnSpec> list = new ArrayList<>(columns.length);
        m_indexes = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            DataColumnSpec columnSpec =
                checkSettingNotNull(targetSpec.getColumnSpec(column), "Column: '%s' does not exist in input.", column);
            checkSetting(columnSpec.getType().isCompatible(DoubleValue.class), "Column: '%s' is not a double value.",
                column);
            list.add(new DataColumnSpecCreator(column, DoubleCell.TYPE).createSpec());
            m_indexes[i] = targetSpec.findColumnIndex(column);
        }

        m_resultSpec = new DataTableSpec(list.toArray(new DataColumnSpec[0]));
    }

    /**
     * Computes the covariance matrix and puts the result in the given (optional) data container and additionally
     * returns a in memory representation. The data container is expected to have the data table spec returned at
     * {@link #getResultSpec()}. The implementation traverses the data once.
     *
     * @param exec the execution container
     * @param inTable input data
     * @param resultDataContainer optional result data container
     * @return the covariance matrix
     * @throws CanceledExecutionException if the user canceled the execution
     */
    public RealMatrix computeCovarianceMatrix(final ExecutionMonitor exec, final BufferedDataTable inTable,
        final DataContainer resultDataContainer) throws CanceledExecutionException {
        return calculateCovarianceMatrix(exec, inTable, inTable.size(), resultDataContainer);
    }

    /**
     * Computes the covariance matrix and puts the result in the given (optional) data container and additionally
     * returns a in memory representation. The data container is expected to have the data table spec returned at
     * {@link #getResultSpec()}. The implementation traverses the data once.
     *
     * @param exec the execution container
     * @param inTable input data
     * @param tableSize the data table size
     * @param resultDataContainer optional result data container
     * @return the covariance matrix
     * @throws CanceledExecutionException if the user canceled the execution
     */
    public RealMatrix calculateCovarianceMatrix(final ExecutionMonitor exec, final DataTable inTable,
        final long tableSize, final DataContainer resultDataContainer) throws CanceledExecutionException {
        checkArgument(m_targetSpec.equalStructure(inTable.getDataTableSpec()),
            "Target tables spec is different from the one given in the constructor!");
        if (resultDataContainer != null) {
            checkArgument(m_resultSpec.equalStructure(resultDataContainer.getTableSpec()),
                "Result tables spec is invalid!");
        }

        final ExecutionMonitor computingProgress = exec.createSubProgress(resultDataContainer != null ? 0.8 : 1);

        List<StorelessCovariance> covariancesList = new ArrayList<>();
        // create covariance pairs
        for (int i = 0; i < m_indexes.length; i++) {
            for (int j = i; j < m_indexes.length; j++) {
                covariancesList.add(new StorelessCovariance(2));
            }
        }

        // compute rest of co-variance matrix
        int rowCount = 0;
        double[] buffer = new double[2];
        for (DataRow dataRow : inTable) {
            for (int i = 0; i < m_indexes.length; i++) {
                final int outerIndex = m_indexes[i];
                final DataCell outerCell = dataRow.getCell(outerIndex);
                if (outerCell.isMissing()) {
                    // skip missing values
                    continue;
                }
                final double outerDouble = ((DoubleValue)outerCell).getDoubleValue();

                for (int j = i; j < m_indexes.length; j++) {
                    final int innerIndex = m_indexes[j];
                    final DataCell innerCell = dataRow.getCell(innerIndex);
                    if (innerCell.isMissing()) {
                        // skip missing values
                        continue;
                    }
                    final double innerDouble = ((DoubleValue)innerCell).getDoubleValue();
                    buffer[0] = outerDouble;
                    buffer[1] = innerDouble;
                    int covListIndex = index(m_indexes.length, i, j);
                    covariancesList.get(covListIndex).increment(buffer);
                }
            }
            computingProgress.setProgress(rowCount++ / (double)tableSize,
                "Calculate covariance values, processing row: '" + dataRow.getKey() + "'");
            computingProgress.checkCanceled();
        }

        // Copy the storeless covariances to a real matrix
        RealMatrix covMatrix = new Array2DRowRealMatrix(m_indexes.length, m_indexes.length);
        for (int i = 0; i < m_indexes.length; i++) {
            for (int j = i; j < m_indexes.length; j++) {
                int covListIndex = index(m_indexes.length, i, j);
                double covValue;
                try {
                    covValue =
                        i == j ? covariancesList.get(covListIndex).getCovariance(1, 1) : covariancesList.get(
                            covListIndex).getCovariance(0, 1);
                } catch (NumberIsTooSmallException e) {
                    throw new IllegalArgumentException(String.format("There were not enough valid values to "
                        + "compute covariance between columns: '%s' and '%s'.", inTable.getDataTableSpec()
                        .getColumnSpec(m_indexes[i]).getName(), inTable.getDataTableSpec().getColumnSpec(m_indexes[j])
                        .getName()), e);
                }
                covMatrix.setEntry(i, j, covValue);
                covMatrix.setEntry(j, i, covValue);
            }
        }

        if (resultDataContainer != null) {
            exec.setProgress("Writing matrix to data table");
            final ExecutionMonitor writingProgress = exec.createSubProgress(0.2);
            for (int i = 0; i < covMatrix.getRowDimension(); i++) {
                resultDataContainer.addRowToTable(new DefaultRow(RowKey.toRowKeys(resultDataContainer.getTableSpec()
                    .getColumnSpec(i).getName())[0], covMatrix.getRow(i)));
                exec.checkCanceled();
                writingProgress.setProgress((double)i / covMatrix.getRowDimension(), "Writing row: "
                    + resultDataContainer.getTableSpec().getColumnSpec(i).getName());
            }
        }
        return covMatrix;
    }

    /**
     * @return the spec of the resulting data table
     */
    public DataTableSpec getResultSpec() {
        return m_resultSpec;
    }

    /** Assuming you have columns A, B, C, D the column pairs of co-variance are
     * AA, AB, AC, AD, BB, BC, BD, CC, CD, DD.
     * To get 0,3 you compute (0)+3,
     * To get 1,2 you compute (4)+2
     * To get 2,2 you compute (4+3)+2
     */
    private static int index(final int max, final int row, final int col) {
        return partialGaussSum(max, row) + col;
    }

    private static int partialGaussSum(final int starts, final int iterations) {
        int t = 0;
        for (int i = iterations; i > 0; i--) {
            t += starts - i;
        }
        return t;
    }
}
