/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.base.node.stats.testing.levene;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Heiko Hofer
 */
public class LeveneTest {

    private String[] m_testColumns;
    private List<String> m_groups;
    /** summary statistics per group. */
    private List<List<SummaryStatistics>> m_gstats;
    private String m_groupingColumn;

    /**
     * Levene-Test
     *
     * @param testColumns the test columns
     * @param groupingColumn the column of the input table holding the groups
     * @param groups the groups to use
     * @param gstats the summary statistics per group if already computed
     */
    public LeveneTest(final String[] testColumns, final String groupingColumn,
            final List<String> groups,
            final List<List<SummaryStatistics>> gstats) {
        super();
        m_testColumns = testColumns;
        m_groupingColumn = groupingColumn;
        m_groups = groups;
        m_gstats = gstats;
    }



    public LeveneTestStatistics[] execute(final BufferedDataTable table,
            final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException {

        DataTableSpec spec = table.getDataTableSpec();
        int groupingIndex = spec.findColumnIndex(m_groupingColumn);
        if (groupingIndex == -1) {
            throw new InvalidSettingsException("Grouping column not found.");
        }
        int[] testColumnsIndex = new int[m_testColumns.length];
        for (int i = 0; i < testColumnsIndex.length; i++) {
            testColumnsIndex[i] = spec.findColumnIndex(m_testColumns[i]);
        }

        int testColumnCount = m_testColumns.length;

        LeveneTestPreProcessing[] levenePre =
            new LeveneTestPreProcessing[testColumnCount];
        for (int i = 0; i < testColumnCount; i++) {
            levenePre[i] = new LeveneTestPreProcessing(m_gstats.get(i));
        }
        final int rowCount = table.getRowCount();
        ExecutionMonitor secondPassExec = exec;
        if (m_groups.size() > 2) {
            // we can skip the pre-processing for a groups size of two.
            ExecutionMonitor firstPassExec = exec.createSubProgress(0.5);
            secondPassExec = exec.createSubProgress(0.5);
            exec.setMessage("1st pass");
            int rowIndex = 0;
            for (DataRow row : table) {
                firstPassExec.checkCanceled();
                firstPassExec.setProgress(rowIndex++ / (double)rowCount,
                        rowIndex + "/" + rowCount + " (\"" + row.getKey() + "\")");
                final DataCell groupCell = row.getCell(groupingIndex);
                String group = groupCell.isMissing() ? null : groupCell.toString();
                for (int i = 0; i < testColumnCount; i++) {
                    if (group == null) {
                        continue;
                    }
                    int gIndex = m_groups.indexOf(group);
                    DataCell cell = row.getCell(testColumnsIndex[i]);
                    if (!cell.isMissing()) {
                        DoubleValue value = (DoubleValue)cell;
                        levenePre[i].addValue(value.getDoubleValue(), gIndex);
                    }
                }
            }
            firstPassExec.setProgress(1.0);
            exec.setMessage("2nd pass");
        }

        LeveneTestStatistics[] result =
            new LeveneTestStatistics[testColumnCount];
        for (int i = 0; i < testColumnCount; i++) {
            result[i] = new LeveneTestStatistics(m_testColumns[i],
                    m_groups, levenePre[i]);
        }
        int rowIndex = 0;
        // a second run over the data for a group size greater than two
        for (DataRow row : table) {
            secondPassExec.checkCanceled();
            secondPassExec.setProgress(rowIndex++ / (double)rowCount,
                    rowIndex + "/" + rowCount + " (\"" + row.getKey() + "\")");
            DataCell groupCell = row.getCell(groupingIndex);
            String group = groupCell.toString();
            for (int i = 0; i < testColumnCount; i++) {
                if (groupCell.isMissing()) {
                    result[i].addMissingGroup();
                    continue;
                }
                int gIndex = m_groups.indexOf(group);
                if (gIndex == -1) {
                    continue;
//                    throw new RuntimeException("Unexpected value \""
//                            + group + "\" in group column. Allowed values are "
//                            + m_groups);
                }
                DataCell cell = row.getCell(testColumnsIndex[i]);
                if (!cell.isMissing()) {
                    DoubleValue value = (DoubleValue)cell;
                    result[i].addValue(value.getDoubleValue(), gIndex);
                } else {
                    result[i].addMissing(gIndex);
                }
            }
        }
        return result;
    }

}
