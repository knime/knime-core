/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   25.06.2012 (hofer): created
 */
package org.knime.base.node.stats.testing.ttest;

import org.knime.base.node.stats.testing.ttest.Grouping.Group;
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
public class TwoSampleTTest {

    private String[] m_testColumns;
    private Grouping m_grouping;
    private double m_confidenceIntervalProb;

    /**
     * A two-sample t-Test.
     *
     * @param testColumns the test columns
     * @param grouping the grouping to use
     * @param confidenceIntervalProb the probability used to compute
     * confidence intervals (Typically 0.95)
     */
    public TwoSampleTTest(final String[] testColumns, final Grouping grouping,
            final double confidenceIntervalProb) {
        super();
        m_testColumns = testColumns;
        m_grouping = grouping;
        m_confidenceIntervalProb = confidenceIntervalProb;
    }



    public TwoSampleTTestStatistics[] execute(final BufferedDataTable table,
            final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {

        DataTableSpec spec = table.getDataTableSpec();
        int groupingIndex = spec.findColumnIndex(m_grouping.getColumn());
        if (groupingIndex == -1) {
            throw new InvalidSettingsException("Grouping column not found.");
        }
        int[] testColumnsIndex = new int[m_testColumns.length];
        for (int i = 0; i < testColumnsIndex.length; i++) {
            testColumnsIndex[i] = spec.findColumnIndex(m_testColumns[i]);
        }

        int testColumnCount = m_testColumns.length;
        TwoSampleTTestStatistics[] result =
            new TwoSampleTTestStatistics[testColumnCount];
        for (int i = 0; i < testColumnCount; i++) {
            result[i] = new TwoSampleTTestStatistics(m_testColumns[i],
                    m_grouping.getGroupLabels(), m_confidenceIntervalProb);
        }

        final int rowCount = table.getRowCount();
        int rowIndex = 0;
        for (DataRow row : table) {
            exec.checkCanceled();
            exec.setProgress(rowIndex++ / (double)rowCount,
                    rowIndex + "/" + rowCount + " (\"" + row.getKey() + "\")");
            DataCell groupCell = row.getCell(groupingIndex);
            Group group = m_grouping.getGroup(groupCell);
            for (int i = 0; i < testColumnCount; i++) {
                if (group == null) {
                    if (groupCell.isMissing()) {
                        result[i].addMissingGroup();
                    } else {
                        result[i].addIgnoredGroup();
                    }
                    continue;
                }
                DataCell cell = row.getCell(testColumnsIndex[i]);
                if (!cell.isMissing()) {
                    DoubleValue value = (DoubleValue)cell;
                    result[i].addValue(value.getDoubleValue(), group);
                } else {
                    result[i].addMissing(group);
                }
            }
        }
        return result;
    }

}
