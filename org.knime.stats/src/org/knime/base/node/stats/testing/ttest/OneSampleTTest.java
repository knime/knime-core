/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   25.06.2012 (hofer): created
 */
package org.knime.base.node.stats.testing.ttest;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Heiko Hofer
 */
public class OneSampleTTest {

    private String[] m_columns;
    private double m_testValue;
    private double m_confidenceIntervalProb;

    /**
     * A paired t-Test.
     *
     * @param leftColumns the left columns of the pairs
     * @param testValue the value to test for
     * @param confidenceIntervalProb the probability used to compute
     * confidence intervals (Typically 0.95)
     */
    public OneSampleTTest(final String[] leftColumns, final double testValue,
            final double confidenceIntervalProb) {
        super();
        m_columns = leftColumns;
        m_testValue = testValue;
        m_confidenceIntervalProb = confidenceIntervalProb;
    }



    /**
     * Performs one-sample t-tests.
     * @param table the input table
     * @param exec execution context
     * @return results for each performed t-test
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     */
    public OneSampleTTestStatistics[] execute(final BufferedDataTable table,
            final ExecutionContext exec) throws InvalidSettingsException,
            CanceledExecutionException {

        DataTableSpec spec = table.getDataTableSpec();

        int colCount = m_columns.length;
        int[] columnsIndex = new int[colCount];
        for (int i = 0; i < colCount; i++) {
            columnsIndex[i] = spec.findColumnIndex(m_columns[i]);
        }

        OneSampleTTestStatistics[] result = new OneSampleTTestStatistics[colCount];
        for (int i = 0; i < colCount; i++) {
            result[i] = new OneSampleTTestStatistics(m_columns[i],
                    m_testValue, m_confidenceIntervalProb);
        }

        final int rowCount = table.getRowCount();
        int rowIndex = 0;
        for (DataRow row : table) {
            exec.checkCanceled();
            exec.setProgress(rowIndex++ / (double)rowCount,
                    rowIndex + "/" + rowCount + " (\"" + row.getKey() + "\")");
            for (int i = 0; i < colCount; i++) {
                DataCell cell = row.getCell(columnsIndex[i]);

                if (!cell.isMissing()) {
                    DoubleValue left = (DoubleValue)cell;
                    result[i].addValue(left.getDoubleValue());
                } else {
                    result[i].addMissing();
                }
            }
        }

        return result;
    }

}
