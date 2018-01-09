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
 *   29.05.2015 (Alexander): created
 */
package org.knime.base.node.viz.liftchart;

import java.util.LinkedList;
import java.util.List;

import org.knime.base.data.sort.SortedTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 *
 * @author Alexander Fillbrunn
 * @since 2.12
 */
public class LiftCalculator {

    private String m_responseColumn;
    private String m_probabilityColumn;
    private String m_responseLabel;
    private double m_intervalWidth;

    private boolean m_ignoreMissingValues;

    private BufferedDataTable m_lift;
    private BufferedDataTable m_response;
    private SortedTable m_sorted;

    /**
     * @return the lift
     */
    public BufferedDataTable getLiftTable() {
        return m_lift;
    }

    /**
     * @return the response
     */
    public BufferedDataTable getResponseTable() {
        return m_response;
    }

    /**
     * @return the sorted input table
     */
    public BufferedDataTable getSortedInput() {
        return m_sorted.getBufferedDataTable();
    }

    /**
     * Creates a new instance of LisftCalculator.
     * @param responseColumn the response column
     * @param probabilityColumn the probability column
     * @param responseLabel the response label
     * @param intervalWidth the interval width
     */
    public LiftCalculator(final String responseColumn, final String probabilityColumn,
                            final String responseLabel, final double intervalWidth) {
        this(responseColumn, probabilityColumn, responseLabel, intervalWidth, false);
    }

    /**
     * Creates a new instance of LisftCalculator.
     * @param responseColumn the response column
     * @param probabilityColumn the probability column
     * @param responseLabel the response label
     * @param intervalWidth the interval width
     * @param ignoreMissingValues whether ignore missing values
     * @since 3.4
     */
    public LiftCalculator(final String responseColumn, final String probabilityColumn,
                            final String responseLabel, final double intervalWidth, final boolean ignoreMissingValues) {
        m_responseColumn = responseColumn;
        m_probabilityColumn = probabilityColumn;
        m_responseLabel = responseLabel;
        m_intervalWidth = intervalWidth;
        m_ignoreMissingValues = ignoreMissingValues;
    }

    /**
     * Calculates the tables necessary for displaying a lift chart.
     * @param table the data table
     * @param exec the execution context to report progress to
     * @return warning messages or null
     * @throws CanceledExecutionException when the user cancels the execution
     */
    public String calculateLiftTables(final BufferedDataTable table, final ExecutionContext exec)
            throws CanceledExecutionException {
        int predColIndex = table.getDataTableSpec().findColumnIndex(m_responseColumn);
        String warning = null;
        List<String> inclList = new LinkedList<String>();

        inclList.add(m_probabilityColumn);
        int probColInd = table.getDataTableSpec().findColumnIndex(m_probabilityColumn);

        boolean[] order = new boolean[]{false};

        m_sorted = new SortedTable(table, inclList, order, exec);

        long totalResponses = 0;

        double partWidth = m_intervalWidth;

        int nrParts = (int)Math.ceil(100.0 / partWidth);

        List<Integer> positiveResponses = new LinkedList<Integer>();

        int rowIndex = 0;
        for (DataRow row : m_sorted) {
            if (row.getCell(predColIndex).isMissing() || row.getCell(probColInd).isMissing()) {
                if (row.getCell(predColIndex).isMissing()) {
                    // miss. values in class column we always ignore
                    continue;
                }
                if (m_ignoreMissingValues) {
                    continue;
                } else {
                    warning = "Table contains missing values.";
                }
            }

            String response =
                    ((StringValue)row.getCell(predColIndex)).getStringValue()
                            .trim();

            if (response.equalsIgnoreCase(m_responseLabel)) {
                totalResponses++;
                positiveResponses.add(rowIndex);
            }

            rowIndex++;
        }

        int[] counter = new int[nrParts];
        int partWidthAbsolute = (int)Math.ceil(rowIndex / (double)nrParts);

        double avgResponse = (double)positiveResponses.size() / rowIndex;

        for (int rIndex : positiveResponses) {
            int index = rIndex / partWidthAbsolute;
            counter[index]++;
        }

        DataColumnSpec[] colSpec = new DataColumnSpec[3];

        colSpec[0] =
                new DataColumnSpecCreator("Lift", DoubleCell.TYPE).createSpec();
        colSpec[1] =
                new DataColumnSpecCreator("Baseline", DoubleCell.TYPE)
                        .createSpec();
        colSpec[2] =
                new DataColumnSpecCreator("Cumulative Lift", DoubleCell.TYPE)
                        .createSpec();

        DataTableSpec tableSpec = new DataTableSpec(colSpec);

        DataContainer cont = exec.createDataContainer(tableSpec);//new DataContainer(tableSpec);

        colSpec = new DataColumnSpec[2];

        colSpec[0] =
                new DataColumnSpecCreator("Actual", DoubleCell.TYPE)
                        .createSpec();
        colSpec[1] =
                new DataColumnSpecCreator("Baseline", DoubleCell.TYPE)
                        .createSpec();

        tableSpec = new DataTableSpec(colSpec);

        DataContainer responseCont = exec.createDataContainer(tableSpec);//new DataContainer(tableSpec);

        long cumulativeCounter = 0;

        responseCont.addRowToTable(new DefaultRow(new RowKey("0"), 0.0, 0.0));

        for (int i = 0; i < counter.length; i++) {
            cumulativeCounter += counter[i];
            double responseRate = (double)counter[i] / partWidthAbsolute;
            double lift = responseRate / avgResponse;

            double cumResponseRate = (double)cumulativeCounter / totalResponses;

            long number = partWidthAbsolute * (i + 1);

            // well.. rounding problems
            if (number > rowIndex) {
                number = rowIndex;
            }

            double cumulativeLift =
            // (double)cumulativeCounter / (partWidthAbsolute * (i + 1));
                    (double)cumulativeCounter / number;
            cumulativeLift /= avgResponse;

            // cumulativeLift = lifts / (i+1);

            double rowKey = ((i + 1) * partWidth);
            if (rowKey > 100) {
                rowKey = 100;
            }
            cont.addRowToTable(new DefaultRow(new RowKey("" + rowKey), lift,
                    1.0, cumulativeLift));

            double cumBaseline = (i + 1) * partWidth;

            if (cumBaseline > 100) {
                cumBaseline = 100;
            }
            responseCont.addRowToTable(new DefaultRow(new RowKey("" + rowKey),
                    cumResponseRate * 100, cumBaseline));
        }

        cont.close();
        responseCont.close();

        m_lift = (BufferedDataTable)cont.getTable();
        m_response = (BufferedDataTable)responseCont.getTable();
        return warning;
    }
}
