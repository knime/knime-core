/* Created on Oct 20, 2006 3:19:14 PM by thor
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 */
package org.knime.base.node.viz.enrich;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
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
import org.knime.base.node.viz.enrich.EnrichmentPlotterSettings.Curve;

/**
 * This class is the model for the enrichment plotter node. It does the
 * precalculation for the view, i.e. it sorts the the data according to the
 * selected columns and computes the y-values for the enrichment curves.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class EnrichmentPlotterModel extends NodeModel {
    private List<EnrichmentPlot> m_curves = new ArrayList<EnrichmentPlot>();

    private static final DataTableSpec OUT_SPEC;

    static {
        DataColumnSpec dcs =
                new DataColumnSpecCreator("Area", DoubleCell.TYPE).createSpec();
        OUT_SPEC = new DataTableSpec(dcs);
    }

    private static class Helper implements Comparable<Helper> {
        final double a, b;

        Helper(final double aa, final double bb) {
            this.a = aa;
            this.b = bb;
        }

        /**
         * {@inheritDoc}
         */
        public int compareTo(final Helper o) {
            return (int)Math.signum(this.a - o.a);
        }
    }

    /**
     * Small container class for a single enrichment curve.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    static class EnrichmentPlot implements Serializable {
        private final String m_name;

        private final double m_area;

        private final double[] m_x, m_y;

        /**
         * Creates a new enrich curve container.
         *
         * @param name the curve's name
         * @param x the curve's x-values
         * @param y the curve's y-values
         * @param area the curve's area
         */
        EnrichmentPlot(final String name, final double[] x, final double[] y,
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

    private final EnrichmentPlotterSettings m_settings =
            new EnrichmentPlotterSettings();

    /**
     * Creates a new model for the enrichment plotter node.
     */
    public EnrichmentPlotterModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_settings.getCurveCount() == 0) {
            throw new InvalidSettingsException("No curves defined in dialog.");
        }

        for (int i = 0; i < m_settings.getCurveCount(); i++) {
            Curve c = m_settings.getCurve(i);
            if (inSpecs[0].findColumnIndex(c.getSortColumn()) == -1) {
                throw new InvalidSettingsException("Sort column '"
                        + c.getSortColumn() + " ' does not exist");
            }
            if (inSpecs[0].findColumnIndex(c.getHitColumn()) == -1) {
                throw new InvalidSettingsException("Hit column '"
                        + c.getHitColumn() + " ' does not exist");
            }
        }

        return new DataTableSpec[]{OUT_SPEC};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final double maxProgress =
                m_settings.getCurveCount() * inData[0].getRowCount();
        BufferedDataContainer outCont = exec.createDataContainer(OUT_SPEC);

        for (int i = 0; i < m_settings.getCurveCount(); i++) {
            exec.checkCanceled();
            int localProgress = i * inData[0].getRowCount();
            exec.setProgress(localProgress / maxProgress);
            Curve c = m_settings.getCurve(i);
            final Helper[] curve = new Helper[inData[0].getRowCount()];

            int sortIndex =
                    inData[0].getDataTableSpec().findColumnIndex(
                            c.getSortColumn());
            int hitIndex =
                    inData[0].getDataTableSpec().findColumnIndex(
                            c.getHitColumn());

            int k = 0, maxK = 0;
            for (DataRow row : inData[0]) {
                DataCell c1 = row.getCell(sortIndex);
                DataCell c2 = row.getCell(hitIndex);

                if (k++ % 1000 == 0) {
                    exec.checkCanceled();
                    exec.setProgress((localProgress + k) / maxProgress);
                }

                if (c1.isMissing()) {
                    continue;
                } else if (c2.isMissing()) {
                    curve[maxK] =
                            new Helper(((DoubleValue)c1).getDoubleValue(), 0);
                } else {
                    curve[maxK] =
                            new Helper(((DoubleValue)c1).getDoubleValue(),
                                    ((DoubleValue)c2).getDoubleValue());
                }
                maxK++;
            }

            Arrays.sort(curve, 0, maxK);
            if (c.isSortDescending()) {
                for (int j = 0; j < maxK / 2; j++) {
                    Helper h = curve[j];
                    curve[j] = curve[maxK - j - 1];
                    curve[maxK - j - 1] = h;
                }
            }

            final int size = Math.min(4000, maxK);
            final double downSample = maxK / (double)size;
            double[] xValues = new double[size + 1];
            double[] yValues = new double[size + 1];
            int lastK = 0;
            double y = 0, area = 0;
            xValues[0] = 0;
            yValues[0] = 0;
            for (k = 1; k <= maxK; k++) {
                Helper h = curve[k - 1];
                if (m_settings.sumHitValues()) {
                    y += h.b;
                } else {
                    y += (h.b >= m_settings.hitThreshold()) ? 1 : 0;
                }
                area += y / maxK;

                if ((int)(k / downSample) >= lastK + 1) {
                    lastK++;
                    xValues[lastK] = k;
                    yValues[lastK] = y;
                }
            }
            xValues[xValues.length - 1] = maxK;
            yValues[yValues.length - 1] = y;
            area /= y;
            m_curves.add(new EnrichmentPlot(c.getSortColumn() + " vs "
                    + c.getHitColumn(), xValues, yValues, area));
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
        File f = new File(nodeInternDir, "curves.ser.gz");
        if (!f.exists()) {
            throw new IOException("Could not find internal data file");
        }

        ObjectInputStream in =
                new ObjectInputStream(new GZIPInputStream(
                        new FileInputStream(f)));
        try {
            m_curves = (List<EnrichmentPlot>)in.readObject();
        } catch (ClassNotFoundException ex) {
            // should not happen at all
            throw new IOException(ex.getLocalizedMessage());
        }
        in.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_curves = new ArrayList<EnrichmentPlot>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        ObjectOutputStream out =
                new ObjectOutputStream(new GZIPOutputStream(
                        new FileOutputStream(new File(nodeInternDir,
                                "curves.ser.gz"))));
        out.writeObject(m_curves);
        out.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        EnrichmentPlotterSettings s = new EnrichmentPlotterSettings();
        s.loadSettingsFrom(settings);
        for (int i = 0; i < s.getCurveCount(); i++) {
            if (s.getCurve(i).getHitColumn().equals(
                    s.getCurve(i).getSortColumn())) {
                throw new InvalidSettingsException("Hit and sort column are "
                        + "identical: " + s.getCurve(i).getHitColumn());
            }
        }
    }

    /**
     * Returns a list with all precalculated curves that should be shown in the
     * view.
     *
     * @return a list with enrichment curves
     */
    List<EnrichmentPlot> getCurves() {
        return m_curves;
    }
}
