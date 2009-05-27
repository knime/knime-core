/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   11.02.2008 (thor): created
 */
package org.knime.base.node.viz.roc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This model prepares the data for the ROC curve view.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ROCNodeModel extends NodeModel {
    private List<ROCCurve> m_curves = new ArrayList<ROCCurve>();

    private static final DataTableSpec OUT_SPEC;

    static {
        DataColumnSpec dcs =
                new DataColumnSpecCreator("Area Under Curve", DoubleCell.TYPE)
                        .createSpec();
        OUT_SPEC = new DataTableSpec(dcs);
    }

    /**
     * Small container class for a single ROC curve.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    static class ROCCurve {
        private final String m_name;

        private final double m_area;

        private final double[] m_x, m_y;

        /**
         * Creates a new ROC curve container.
         *
         * @param name the curve's name
         * @param x the curve's x-values
         * @param y the curve's y-values
         * @param area the curve's area
         */
        ROCCurve(final String name, final double[] x, final double[] y,
                final double area) {
            m_name = name;
            m_x = x;
            m_y = y;
            m_area = area;
        }

        /**
         * Returns the curve's name.
         *
         * @return the curve's name.
         */
        public String getName() {
            return m_name;
        }

        /**
         * Returns the curve's area.
         *
         * @return the curve's area.
         */
        public final double getArea() {
            return m_area;
        }

        /**
         * Returns the curve's x-values.
         *
         * @return the curve's x-values.
         */
        public final double[] getX() {
            return m_x;
        }

        /**
         * Returns the curve's y-values.
         *
         * @return the curve's y-values.
         */
        public final double[] getY() {
            return m_y;
        }
    }

    private final ROCSettings m_settings = new ROCSettings();

    /**
     * Creates a new model for the enrichment plotter node.
     */
    public ROCNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_settings.getCurves().size() == 0) {
            throw new InvalidSettingsException("No curves defined");
        }

        if (!inSpecs[0].containsName(m_settings.getClassColumn())) {
            throw new InvalidSettingsException("Class column '"
                    + m_settings.getClassColumn() + " ' does not exist");
        }

        for (String c : m_settings.getCurves()) {
            if (!inSpecs[0].containsName(c)) {
                throw new InvalidSettingsException("Sort column '" + c
                        + " ' does not exist");
            }
        }

        if (m_settings.getPositiveClass() == null) {
            throw new InvalidSettingsException(
                    "No value for the positive class chosen");
        }

        return new DataTableSpec[]{OUT_SPEC};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final int curvesSize = m_settings.getCurves().size();
        BufferedDataContainer outCont = exec.createDataContainer(OUT_SPEC);

        int classIndex =
                inData[0].getDataTableSpec().findColumnIndex(
                        m_settings.getClassColumn());
        int size = inData[0].getRowCount();
        if (size == 0) {
            setWarningMessage("Input table contains no rows");
        }

        for (int i = 0; i < curvesSize; i++) {
            exec.checkCanceled();
            String c = m_settings.getCurves().get(i);

            ExecutionContext subExec = exec.createSubExecutionContext(
                    1.0 / curvesSize);
            SortedTable sortedTable =
                    new SortedTable(inData[0], Collections.singletonList(c),
                            new boolean[]{false}, subExec);
            subExec.setProgress(1.0);

            int tp = 0, fp = 0;
            // these contain the coordinates for the plot
            double[] xValues = new double[size + 1];
            double[] yValues = new double[size + 1];
            int k = 1;
            final int scoreColIndex =
                    sortedTable.getDataTableSpec().findColumnIndex(c);
            DataCell lastScore = null;
            for (DataRow row : sortedTable) {
                exec.checkCanceled();
                DataCell realClass = row.getCell(classIndex);
                if (realClass.equals(m_settings.getPositiveClass())) {
                    tp++;
                } else {
                    fp++;
                }
                // if values differ (if they are equal we can't prefer one
                // value over the other as they are indifferent; for a sequence
                // of equal probabilities, think of what would happen if we 
                // first encounter all TP and then the FP and the other way
                // around ... the following lines circumvent this)
                if (!row.getCell(scoreColIndex).equals(lastScore)) {
                    xValues[k] = fp;
                    yValues[k] = tp;
                    k++;
                    lastScore = row.getCell(scoreColIndex);
                }
            }

            xValues = Arrays.copyOf(xValues, k + 1);
            yValues = Arrays.copyOf(yValues, k + 1);

            while (--k >= 0) {
                xValues[k] /= fp;
                yValues[k] /= tp;
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

            m_curves.add(new ROCCurve(c, xValues, yValues, area));
            outCont.addRowToTable(new DefaultRow(new RowKey(c.toString()),
                    new DoubleCell(area)));
        }

        outCont.close();
        return new BufferedDataTable[]{outCont.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, "curves.txt.gz");
        if (!f.exists()) {
            throw new IOException("Could not find internal data file");
        }

        BufferedReader in =
                new BufferedReader(new InputStreamReader(new GZIPInputStream(
                        new FileInputStream(f))));
        int curves;
        try {
            curves = Integer.parseInt(in.readLine());
        } catch (final NumberFormatException e) {
            throw new IOException("Can't parse as int: " + e.getMessage(), e);
        }
        m_curves.clear();
        for (int i = 0; i < curves; i++) {
            String name = in.readLine();
            double area;
            try {
                area = Double.parseDouble(in.readLine());
            } catch (final NumberFormatException e) {
                throw new IOException("Can't parse double: " 
                        + e.getMessage(), e);
            }

            String line = in.readLine();
            line = line.substring(1, line.length() - 1);
            String[] parts = line.split(", ");
            double[] x = new double[parts.length];
            for (int k = 0; k < parts.length; k++) {
                try {
                    x[k] = Double.parseDouble(parts[k]);
                } catch (final NumberFormatException e) {
                    throw new IOException("Can't parse double: " 
                            + e.getMessage(), e);
                }
            }

            line = in.readLine();
            line = line.substring(1, line.length() - 1);
            parts = line.split(", ");
            double[] y = new double[parts.length];
            for (int k = 0; k < parts.length; k++) {
                try {
                    y[k] = Double.parseDouble(parts[k]);
                } catch (final NumberFormatException e) {
                    throw new IOException("Can't parse double: " 
                            + e.getMessage(), e);
                }
            }

            m_curves.add(new ROCCurve(name, x, y, area));
        }

        in.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_curves = new ArrayList<ROCCurve>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        PrintWriter out =
                new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(
                        new FileOutputStream(new File(nodeInternDir,
                                "curves.txt.gz")))));
        out.println(m_curves.size());

        for (ROCCurve c : m_curves) {
            out.println(c.getName());
            out.println(c.getArea());
            out.println(Arrays.toString(c.getX()));
            out.println(Arrays.toString(c.getY()));
        }
        out.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ROCSettings s = new ROCSettings();
        s.loadSettings(settings);

        if (s.getCurves().size() < 1) {
            throw new InvalidSettingsException(
                    "No class probability column(s) selected");
        }

        if (s.getPositiveClass() == null) {
            throw new InvalidSettingsException(
                    "No value for the positive class chosen");
        }

        if (s.getPositiveClass().isMissing()) {
            throw new InvalidSettingsException(
                    "Missing value for the positive class chosen");
        }
    }

    /**
     * Returns a list with all pre-calculated curves that should be shown in the
     * view.
     *
     * @return a list with ROC curves
     */
    List<ROCCurve> getCurves() {
        return m_curves;
    }
}
