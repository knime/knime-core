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
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
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

        ROCCalculator calc = new ROCCalculator(m_settings.getCurves(),
                                                m_settings.getClassColumn(),
                                                m_settings.getMaxPoints(),
                                                m_settings.getPositiveClass().toString());

        calc.calculateCurveData(inData[0], exec);

        if (calc.getWarningMessage() != null) {
            setWarningMessage(calc.getWarningMessage());
        }

        m_curves = calc.getOutputCurves();
        return new BufferedDataTable[]{calc.getOutputTable()};
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
        int curveCount;
        try {
            curveCount = Integer.parseInt(in.readLine());
        } catch (final NumberFormatException e) {
            throw new IOException("Can't parse as int: " + e.getMessage(), e);
        }
        List<ROCCurve> curves = new ArrayList<>();
        for (int i = 0; i < curveCount; i++) {
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

            curves.add(new ROCCurve(name, x, y, area, -1));
        }
        m_curves = curves;

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

        if ((s.getMaxPoints() == 0) || (s.getMaxPoints() == 1)) {
            throw new InvalidSettingsException("At least two data points are necessary for each curve");
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
