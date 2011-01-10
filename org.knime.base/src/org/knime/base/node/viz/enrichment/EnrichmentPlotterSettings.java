/* Created on Oct 23, 2006 9:24:02 AM by thor
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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

import java.util.ArrayList;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the enrichment plotter node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class EnrichmentPlotterSettings {
    /**
     * A simple container that holds the necessary information for specifying a
     * curve that should be plotted.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    public static class Curve {
        /** The name of the column by which the data should be sorted. */
        private final String m_sortColumn;

        /** The name of the column with the hit values. */
        private final String m_hitColumn;

        /**
         * <code>true</code> if the sort column should be sorted descendingly,
         * <code>false</code> if it should be sorted ascendingly.
         */
        private final boolean m_sortDescending;

        /**
         * Creates a new Curve object.
         *
         * @param sortColumn the column by which the table should be sorted
         * @param hitColumn the column with the hit values
         * @param sortDescending <code>true</code> if the
         *            <code>sortColumn</code> should be sorted descending,
         *            <code>false</code> if it should be sorted ascending
         */
        public Curve(final String sortColumn, final String hitColumn,
                final boolean sortDescending) {
            m_sortColumn = sortColumn;
            m_hitColumn = hitColumn;
            m_sortDescending = sortDescending;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Curve)) {
                return false;
            }

            Curve c = (Curve)o;
            if (!m_sortColumn.equals(c.m_sortColumn)) {
                return false;
            }
            if (!m_hitColumn.equals(c.m_hitColumn)) {
                return false;
            }

            return m_sortDescending == c.m_sortDescending;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return m_sortColumn.hashCode() ^ (m_hitColumn.hashCode() << 2);
        }

        /**
         * @return the sortColumn
         */
        public String getSortColumn() {
            return m_sortColumn;
        }

        /**
         * @return the column containing the activity or cluster values
         */
        public String getActivityColumn() {
            return m_hitColumn;
        }

        /**
         * @return the sortDescending
         */
        public boolean isSortDescending() {
            return m_sortDescending;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_sortColumn + (m_sortDescending ? " (DESC)" : "") + " vs. "
                    + m_hitColumn;
        }
    }

    /**
     * Enum for the three different plot modes.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    public static enum PlotMode {
        /** Plot the sum of values in the activity column. */
        PlotSum,
        /** Plot the sum of hits in the activity column. */
        PlotHits,
        /** Plot the number of discovered clusters in the cluster column. */
        PlotClusters
    }

    private final ArrayList<Curve> m_curves = new ArrayList<Curve>();

    private PlotMode m_plotMode = PlotMode.PlotHits;

    private double m_hitThreshold = 5.5;

    private int m_minClusterMembers = 1;

    /**
     * Adds a new curve to the settings.
     *
     * @param sortColumn the column by which the table should be sorted
     * @param activityColumn the column with the activity or cluster values
     * @param sortDescending <code>true</code> if the <code>sortColumn</code>
     *            should be sorted descending, <code>false</code> if it should
     *            be sorted ascending
     */
    public void addCurve(final String sortColumn, final String activityColumn,
            final boolean sortDescending) {
        Curve c = new Curve(sortColumn, activityColumn, sortDescending);
        if (!m_curves.contains(c)) {
            m_curves.add(c);
        }
    }

    /**
     * Returns the curve settings for the given index.
     *
     * @param index an index
     * @return the curve settings at the index
     */
    public Curve getCurve(final int index) {
        return m_curves.get(index);
    }

    /**
     * Returns the number of curves that should be plotted.
     *
     * @return the number of curves
     */
    public int getCurveCount() {
        return m_curves.size();
    }

    /**
     * Removes the given curve from the settings.
     *
     * @param curve a curve
     * @return <code>true</code> if the curve was removed, <code>false</code>
     *         otherwise (likely because the curve does not exist)
     */
    public boolean removeCurve(final Curve curve) {
        return m_curves.remove(curve);
    }

    /**
     * Saves this object's settings to the given node settings.
     *
     * @param settings the node settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addInt("curveCount", m_curves.size());

        int i = 1;
        for (Curve c : m_curves) {
            settings.addString("curve_" + i + "_sort", c.getSortColumn());
            settings.addString("curve_" + i + "_act", c.getActivityColumn());
            settings.addBoolean("curve_" + i + "_descending", c
                    .isSortDescending());
            i++;
        }

        settings.addString("plotMode", m_plotMode.name());
        settings.addDouble("hitThreshold", m_hitThreshold);
        settings.addInt("minClusterMembers", m_minClusterMembers);
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings the node settings
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_curves.clear();
        int curveCount = settings.getInt("curveCount");

        for (int i = 1; i <= curveCount; i++) {
            String sort = settings.getString("curve_" + i + "_sort");
            String hit;
            try {
                hit = settings.getString("curve_" + i + "_act");
            } catch (InvalidSettingsException ex) {
                // try old settings
                hit = settings.getString("curve_" + i + "_hit");
            }
            boolean sortDescending =
                    settings.getBoolean("curve_" + i + "_descending");
            m_curves.add(new Curve(sort, hit, sortDescending));
        }

        m_hitThreshold = settings.getDouble("hitThreshold");
        try {
            String plotMode = settings.getString("plotMode");
            m_plotMode = PlotMode.valueOf(plotMode);
        } catch (InvalidSettingsException ex) {
            if (settings.getBoolean("sumHitValues")) {
                m_plotMode = PlotMode.PlotSum;
            } else {
                m_plotMode = PlotMode.PlotHits;
            }
        }
        m_minClusterMembers = settings.getInt("minClusterMembers", 1);
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings the node settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_curves.clear();
        int curveCount = settings.getInt("curveCount", 0);

        for (int i = 1; i <= curveCount; i++) {
            try {
                String sort = settings.getString("curve_" + i + "_sort");
                String hit = settings.getString("curve_" + i + "_act");
                boolean sortDescending =
                        settings.getBoolean("curve_" + i + "_descending");
                m_curves.add(new Curve(sort, hit, sortDescending));
            } catch (InvalidSettingsException ex) {
                // ignore it
            }
        }

        m_hitThreshold = settings.getDouble("hitThreshold", 5.5);
        m_minClusterMembers = settings.getInt("minClusterMembers", 1);
        m_plotMode =
                PlotMode.valueOf(settings.getString("plotMode",
                        PlotMode.PlotHits.name()));
    }

    /**
     * Returns the desired plot mode.
     *
     * @return the plot mode
     */
    public PlotMode plotMode() {
        return m_plotMode;
    }

    /**
     * Sets the desired plot mode.
     *
     * @param mode the plot mode
     */
    public void plotMode(final PlotMode mode) {
        m_plotMode = mode;
    }

    /**
     * Returns the threshold above and including which a data point is
     * considered a hit. This settings is only relevant if {@link #plotMode()}
     * is {@link PlotMode#PlotHits}.
     *
     * @return the hit threshold
     */
    public double hitThreshold() {
        return m_hitThreshold;
    }

    /**
     * Sets the threshold above and including which a data point is considered a
     * hit. This settings is only relevant if {@link #plotMode()} is
     * {@link PlotMode#PlotHits}.
     *
     * @param thres the hit threshold
     */
    public void hitThreshold(final double thres) {
        m_hitThreshold = thres;
    }


    /**
     * Returns the minimum number of molecules from the same cluster that have
     * to be found so that a cluster is declared to be found.
     *
     * @return the minimum number of cluster members
     */
    public int minClusterMembers() {
        return m_minClusterMembers;
    }

    /**
     * Sets the minimum number of molecules from the same cluster that have to
     * be found so that a cluster is declared to be found.
     *
     * @param min the minimum number of cluster members
     */
    public void minClusterMembers(final int min) {
        m_minClusterMembers = min;
    }
}
