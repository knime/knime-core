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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   27.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.Pair;

/**
 * This class contains the settings for the feature elimination filter node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimFilterSettings {
    private int m_nrOfFeatures;

    private boolean m_includeTargetColumn;

    private double m_errorThreshold;

    private boolean m_thresholdMode;

    /**
     * Sets the error threshold for automatic feature selection.
     *
     * @param d the threshold, usually between 0 and 1
     */
    public void errorThreshold(final double d) {
        m_errorThreshold = d;
    }

    /**
     * Returns the error threshold for automatic feature selection.
     *
     * @return the threshold, usually between 0 and 1
     */
    public double errorThreshold() {
        return m_errorThreshold;
    }

    /**
     * Sets if the features are selected manually or dynamically by an error
     * threshold.
     *
     * @param b <code>true</code> if an error threshold should be used,
     *            <code>false</code> if the features are selected manually by
     *            level
     */
    public void thresholdMode(final boolean b) {
        m_thresholdMode = b;
    }

    /**
     * Returns if the features are selected manually or dynamically by an error
     * threshold.
     *
     * @return <code>true</code> if an error threshold should be used,
     *            <code>false</code> if the features are selected manually by
     *            level
     */
    public boolean thresholdMode() {
        return m_thresholdMode;
    }

    /**
     * Returns if the target column should be included in the output table.
     *
     * @return <code>true</code> if it should be included, <code>false</code>
     *         otherwise
     */
    public boolean includeTargetColumn() {
        return m_includeTargetColumn;
    }

    /**
     * sets if the target column should be included in the output table.
     *
     * @param b <code>true</code> if it should be included, <code>false</code>
     *            otherwise
     */
    public void includeTargetColumn(final boolean b) {
        m_includeTargetColumn = b;
    }

    /**
     * Returns a list with the columns that should be included in the output
     * table. This list also includes the target column if it should be
     * included.
     *
     * @param model the feature elimination model used
     * @return a list with column names
     *
     * @see #includeTargetColumn()
     */
    public List<String> includedColumns(final BWElimModel model) {
        List<String> l = new ArrayList<String>();
        if (m_thresholdMode) {
            Pair<Double, Collection<String>> p =
                    findMinimalSet(model, m_errorThreshold);
            if (p != null) {
                l.addAll(p.getSecond());
            }
        } else {
            for (Pair<Double, Collection<String>> p : model.featureLevels()) {
                Collection<String> incFeatures = p.getSecond();
                if (incFeatures.size() == m_nrOfFeatures) {
                    l.addAll(incFeatures);
                    break;
                }
            }
        }

        if (m_includeTargetColumn) {
            l.add(model.targetColumn());
        }

        return l;
    }

    /**
     * Returns the number of included feature for the selected level. This is
     * not necessarily the same as the size of
     * {@link #includedColumns(BWElimModel)} as the latter only contains columns
     * that are present in the input table while the number of features is the
     * "level" that comes out from the elimination loop.
     *
     * @return the number of included features
     */
    public int nrOfFeatures() {
        return m_nrOfFeatures;
    }

    /**
     * Sets the number of included feature for the selected level. This is not
     * necessarily the same as the size of {@link #includedColumns(BWElimModel)}
     * as the latter only contains columns that are present in the input table
     * while the number of features is the "level" that comes out from the
     * elimination loop.
     *
     * @param number the number of included features
     */
    public void nrOfFeatures(final int number) {
        m_nrOfFeatures = number;
    }

    /**
     * Saves the settings from this object into the passed node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addInt("nrOfFeatures", m_nrOfFeatures);
        settings.addBoolean("includeTargetColumn", m_includeTargetColumn);
        settings.addBoolean("thresholdMode", m_thresholdMode);
        settings.addDouble("errorThreshold", m_errorThreshold);
    }

    /**
     * Loads the settings from passed node settings object into this object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if a settings is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_nrOfFeatures = settings.getInt("nrOfFeatures");
        m_includeTargetColumn = settings.getBoolean("includeTargetColumn");

        /** @since 2.4 */
        m_thresholdMode = settings.getBoolean("thresholdMode", false);
        m_errorThreshold = settings.getDouble("errorThreshold", 0.5);
    }

    /**
     * Loads the settings from passed node settings object into this object
     * using default values if a settings is missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_nrOfFeatures = settings.getInt("nrOfFeatures", -1);
        m_includeTargetColumn =
                settings.getBoolean("includeTargetColumn", false);
        m_thresholdMode = settings.getBoolean("thresholdMode", false);
        m_errorThreshold = settings.getDouble("errorThreshold", 0.5);
    }

    static Pair<Double, Collection<String>> findMinimalSet(
            final BWElimModel model, final double threshold) {
        List<Pair<Double, Collection<String>>> col =
                new ArrayList<Pair<Double, Collection<String>>>(
                        model.featureLevels());
        Collections.sort(col,
                new Comparator<Pair<Double, Collection<String>>>() {
                    @Override
                    public int compare(
                            final Pair<Double, Collection<String>> o1,
                            final Pair<Double, Collection<String>> o2) {
                        return o1.getFirst().compareTo(o2.getFirst());
                    }
                });

        Pair<Double, Collection<String>> selectedLevel = null;
        for (Pair<Double, Collection<String>> p : col) {
            if (p.getFirst() <= threshold) {
                if ((selectedLevel == null)
                        || (selectedLevel.getSecond().size() > p.getSecond()
                                .size())) {
                    selectedLevel = p;
                }
            }
        }
        return selectedLevel;
    }
}
