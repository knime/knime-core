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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.enrichment;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.viz.enrichment.EnrichmentPlotterSettings.Curve;
import org.knime.base.node.viz.enrichment.EnrichmentPlotterSettings.PlotMode;
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
import org.knime.core.util.MutableInteger;

/**
 * This class is the model for the enrichment plotter node. It does the
 * pre-calculation for the view, i.e. it sorts the the data according to the
 * selected columns and computes the y-values for the enrichment curves.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class EnrichmentPlotterModel extends NodeModel {
    private List<EnrichmentPlot> m_curves = new ArrayList<EnrichmentPlot>();

    private static final DataTableSpec AREA_OUT_SPEC;

    private static final DataTableSpec DISCRATE_OUT_SPEC;

    private static final double[] DISCRATE_POINTS =
            {0.1, 0.5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 40, 50,
                    60, 80, 100};

    // maximum x-resolution for the graphs
    private static final int MAX_RESOLUTION;

    static {
        DataColumnSpec dcs =
                new DataColumnSpecCreator("Area", DoubleCell.TYPE).createSpec();
        AREA_OUT_SPEC = new DataTableSpec(dcs);

        DataColumnSpec[] cs = new DataColumnSpec[DISCRATE_POINTS.length];
        for (int i = 0; i < cs.length; i++) {
            cs[i] =
                    new DataColumnSpecCreator("Discovery rate at "
                            + DISCRATE_POINTS[i] + "%", DoubleCell.TYPE)
                            .createSpec();
        }

        DISCRATE_OUT_SPEC = new DataTableSpec(cs);

        int res;
        try {
            res =
                    (int)(Toolkit.getDefaultToolkit().getScreenSize()
                            .getWidth() * 3);
        } catch (HeadlessException ex) {
            res = 4000;
        }
        MAX_RESOLUTION = res;
    }

    private static class Helper implements Comparable<Helper> {
        final double a;

        final DataCell b;

        Helper(final double aa, final DataCell bb) {
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
        private static final long serialVersionUID = 3967048973324638794L;

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
        super(1, 2);
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

            if (inSpecs[0].findColumnIndex(c.getActivityColumn()) == -1) {
                throw new InvalidSettingsException("Activity/cluster column '"
                        + c.getActivityColumn() + " ' does not exist");
            }

            if ((m_settings.plotMode() != PlotMode.PlotClusters)
                    && !inSpecs[0].getColumnSpec(c.getActivityColumn())
                            .getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException(
                        "Activity column is not numeric");
            }
        }

        return new DataTableSpec[]{AREA_OUT_SPEC, DISCRATE_OUT_SPEC};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final double rowCount = inData[0].getRowCount();
        final BufferedDataContainer areaOutCont =
                exec.createDataContainer(AREA_OUT_SPEC);
        final BufferedDataContainer discrateOutCont =
                exec.createDataContainer(DISCRATE_OUT_SPEC);

        for (int i = 0; i < m_settings.getCurveCount(); i++) {
            final ExecutionMonitor sexec =
                    exec.createSubProgress(1.0 / m_settings.getCurveCount());
            exec.setMessage("Generating curve " + (i + 1));

            final Curve c = m_settings.getCurve(i);
            final Helper[] curve = new Helper[inData[0].getRowCount()];

            final int sortIndex =
                    inData[0].getDataTableSpec().findColumnIndex(
                            c.getSortColumn());
            final int actIndex =
                    inData[0].getDataTableSpec().findColumnIndex(
                            c.getActivityColumn());

            int k = 0, maxK = 0;
            for (DataRow row : inData[0]) {
                DataCell c1 = row.getCell(sortIndex);
                DataCell c2 = row.getCell(actIndex);

                if (k++ % 100 == 0) {
                    sexec.checkCanceled();
                    sexec.setProgress(k / rowCount);
                }

                if (c1.isMissing()) {
                    continue;
                } else {
                    curve[maxK] =
                            new Helper(((DoubleValue)c1).getDoubleValue(), c2);
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

            // this is for down-sampling so that the view is faster;
            // plotting >100,000 points takes quite a long time
            final int size = Math.min(MAX_RESOLUTION, maxK);
            final double downSampleRate = maxK / (double)size;

            final double[] xValues = new double[size + 1];
            final double[] yValues = new double[size + 1];
            xValues[0] = 0;
            yValues[0] = 0;

            int lastK = 0;
            double y = 0, area = 0;
            int nextHitRatePoint = 0;
            final double[] hitRateValues = new double[DISCRATE_POINTS.length];
            final HashMap<DataCell, MutableInteger> clusters =
                new HashMap<DataCell, MutableInteger>();

            for (k = 1; k <= maxK; k++) {
                final Helper h = curve[k - 1];
                if (m_settings.plotMode() == PlotMode.PlotSum) {
                    y += ((DoubleValue)h.b).getDoubleValue();
                } else if (m_settings.plotMode() == PlotMode.PlotHits) {
                    if (!h.b.isMissing()
                            && (((DoubleValue)h.b).getDoubleValue() >= m_settings
                                    .hitThreshold())) {
                        y++;
                    }
                } else if (!h.b.isMissing()) {
                    MutableInteger count = clusters.get(h.b);
                    if (count == null) {
                        count = new MutableInteger(0);
                        clusters.put(h.b, count);
                    }
                    if (count.inc() == m_settings.minClusterMembers()) {
                        y++;
                    }
                }
                area += y / maxK;

                if ((int)(k / downSampleRate) >= lastK + 1) {
                    lastK++;
                    xValues[lastK] = k;
                    yValues[lastK] = y;
                }

                if ((nextHitRatePoint < DISCRATE_POINTS.length)
                        && (k == (int)Math.floor(maxK
                                * DISCRATE_POINTS[nextHitRatePoint] / 100))) {
                    hitRateValues[nextHitRatePoint] = y;
                    nextHitRatePoint++;
                }
            }
            xValues[xValues.length - 1] = maxK;
            yValues[yValues.length - 1] = y;
            area /= y;

            m_curves.add(new EnrichmentPlot(c.getSortColumn() + " vs "
                    + c.getActivityColumn(), xValues, yValues, area));
            areaOutCont.addRowToTable(new DefaultRow(new RowKey(c.toString()),
                    new DoubleCell(area)));

            for (int j = 0; j < hitRateValues.length; j++) {
                hitRateValues[j] /= y;
            }

            discrateOutCont.addRowToTable(new DefaultRow(new RowKey(c
                    .toString()), hitRateValues));
        }

        areaOutCont.close();
        discrateOutCont.close();
        return new BufferedDataTable[]{areaOutCont.getTable(),
                discrateOutCont.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
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
        m_settings.loadSettings(settings);
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
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        EnrichmentPlotterSettings s = new EnrichmentPlotterSettings();
        s.loadSettings(settings);
        for (int i = 0; i < s.getCurveCount(); i++) {
            if (s.getCurve(i).getActivityColumn().equals(
                    s.getCurve(i).getSortColumn())) {
                throw new InvalidSettingsException("Activity and sort column "
                        + " are identical: "
                        + s.getCurve(i).getActivityColumn());
            }
        }
    }

    /**
     * Returns a list with all pre-calculated curves that should be shown in the
     * view.
     *
     * @return a list with enrichment curves
     */
    List<EnrichmentPlot> getCurves() {
        return m_curves;
    }
}
