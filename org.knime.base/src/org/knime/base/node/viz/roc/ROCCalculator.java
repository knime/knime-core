/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   26.05.2015 (Alexander): created
 */
package org.knime.base.node.viz.roc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.base.data.sort.SortedTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 *
 * @author Alexander Fillbrunn
 * @since 2.12
 */
public class ROCCalculator {

    /**
     * The spec of the table created by this class.
     */
    public static final DataTableSpec OUT_SPEC;

    static {
        DataColumnSpec dcs =
                new DataColumnSpecCreator("Area Under Curve", DoubleCell.TYPE)
                        .createSpec();
        OUT_SPEC = new DataTableSpec(dcs);
    }

    private List<String> m_curves;

    private String m_classCol;

    private String m_posClass;

    private int m_maxPoints;

    private boolean m_ignoreMissingValues;

    private BufferedDataTable m_outTable;

    private List<ROCCurve> m_outCurves;

    private String m_warningMessage = null;

    /**
     * @return Warning messages that occurred during execution
     */
    public String getWarningMessage() {
        return m_warningMessage;
    }

    /**
     * Instantiates the ROCCalculator.
     * @param curves the score column of the curves
     * @param classCol the class column
     * @param maxPoints the maximum number of points to put into the calculated ROC curves
     * @param posClass the the positive class
     */
    public ROCCalculator(final List<String> curves, final String classCol,
                        final int maxPoints, final String posClass) {
        this(curves, classCol, maxPoints, posClass, false);
    }

    /**
     * Instantiates the ROCCalculator.
     * @param curves the score column of the curves
     * @param classCol the class column
     * @param maxPoints the maximum number of points to put into the calculated ROC curves
     * @param posClass the the positive class
     * @param ignoreMissingValues whether ignore missing values
     * @since 3.4
     */
    public ROCCalculator(final List<String> curves, final String classCol,
                        final int maxPoints, final String posClass, final boolean ignoreMissingValues) {
        m_curves = curves;
        m_classCol = classCol;
        m_posClass = posClass;
        m_maxPoints = maxPoints;
        m_ignoreMissingValues = ignoreMissingValues;
    }

    /**
     * Calculates the ROC curve.
     * @param table the table with the data
     * @param exec the execution context to use for reporting progress
     * @throws CanceledExecutionException when the user cancels the execution
     */
    public void calculateCurveData(final BufferedDataTable table, final ExecutionContext exec)
            throws CanceledExecutionException {
        m_warningMessage = null;
        List<ROCCurve> curves = new ArrayList<ROCCurve>();
        int classIndex = table.getDataTableSpec().findColumnIndex(m_classCol);
        int curvesSize = m_curves.size();
        int size = table.getRowCount();
        if (size == 0) {
            m_warningMessage = "Input table contains no rows";
        }
        BufferedDataContainer outCont = exec.createDataContainer(OUT_SPEC);
        for (int i = 0; i < curvesSize; i++) {
            exec.checkCanceled();
            String c = m_curves.get(i);

            ExecutionContext subExec = exec.createSubExecutionContext(
                    1.0 / curvesSize);
            SortedTable sortedTable =
                    new SortedTable(table, Collections.singletonList(c),
                            new boolean[]{false}, subExec);
            subExec.setProgress(1.0);

            int tp = 0, fp = 0;
            // these contain the coordinates for the plot
            double[] xValues = new double[size + 1];
            double[] yValues = new double[size + 1];
            int k = 0;
            final int scoreColIndex =
                    sortedTable.getDataTableSpec().findColumnIndex(c);
            DataCell lastScore = null;
            for (DataRow row : sortedTable) {
                exec.checkCanceled();
                DataCell realClass = row.getCell(classIndex);
                if (realClass.isMissing() || row.getCell(scoreColIndex).isMissing()) {
                    if (m_ignoreMissingValues) {
                        continue;
                    } else {
                        m_warningMessage = "Table contains missing values.";
                    }
                }
                if (realClass.toString().equals(m_posClass)) {
                    tp++;
                } else {
                    fp++;
                }

                // Only add a new line point if probability values differ. If they are equal we can't prefer one
                // value over the other as they are indifferent; for a sequence of equal probabilities, think of what
                // would happen if we first encounter all TP and then the FP and the other way
                // around ... the following lines circumvent this.
                if (!row.getCell(scoreColIndex).equals(lastScore)) {
                    k++;
                    lastScore = row.getCell(scoreColIndex);
                }
                xValues[k] = fp;
                yValues[k] = tp;
            }

            xValues = Arrays.copyOf(xValues, k + 1);
            yValues = Arrays.copyOf(yValues, k + 1);

            for (int j = 0; j <= k; j++) {
                xValues[j] /= fp;
                yValues[j] /= tp;
            }
            xValues[xValues.length - 1] = 1;
            yValues[yValues.length - 1] = 1;

            double area = 0;
            for (k = 1; k < xValues.length; k++) {
                if (xValues[k - 1] < xValues[k]) {
                    // magical math: the rectangle + the triangle under
                    // the segment xValues[k] to xValues[k - 1]
                    area += 0.5 * (xValues[k] - xValues[k - 1])
                        * (yValues[k] + yValues[k - 1]);
                }
            }

            curves.add(new ROCCurve(c, xValues, yValues, area, m_maxPoints));
            outCont.addRowToTable(new DefaultRow(new RowKey(c.toString()),
                    new DoubleCell(area)));
        }

        m_outCurves = curves;
        outCont.close();
        m_outTable = outCont.getTable();
    }

    /**
     * @return the table with areas under the curves
     */
    public BufferedDataTable getOutputTable() {
        return m_outTable;
    }

    /**
     * @return The output ROC curves for displaying
     */
    public List<ROCCurve> getOutputCurves() {
        return m_outCurves;
    }

}
