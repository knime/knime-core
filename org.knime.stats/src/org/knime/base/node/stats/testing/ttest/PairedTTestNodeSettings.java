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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.stats.testing.ttest;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * The settings object for the "Paired T-Test" Node.
 */
public class PairedTTestNodeSettings {
    private static final String LEFT_COLUMNS = "leftColumns";
    private static final String RIGHT_COLUMNS = "rightColumns";
    private static final String CONFIDENCE_INTERVAL_PROB =
        "confidenceIntervalProb";

    private String[] m_leftColumns;
    private String[] m_rightColumns;

    private double m_confidenceIntervalProb;

	/**
	 * Create a settings object with default values.
	 */
	public PairedTTestNodeSettings() {
	    m_leftColumns = null;
	    m_rightColumns = null;
	    m_confidenceIntervalProb = 0.95;
	}

	/** Saves current parameters to settings object.
     * @param settings to save to
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addStringArray(LEFT_COLUMNS, m_leftColumns);
        settings.addStringArray(RIGHT_COLUMNS, m_rightColumns);
        settings.addDouble(CONFIDENCE_INTERVAL_PROB, m_confidenceIntervalProb);
    }

    /** Loads parameters in NodeModel.
     * @param settings to load from
     * @throws InvalidSettingsException if settings are inconsistent
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_leftColumns = settings.getStringArray(LEFT_COLUMNS);
        m_rightColumns = settings.getStringArray(RIGHT_COLUMNS);
        m_confidenceIntervalProb = settings.getDouble(CONFIDENCE_INTERVAL_PROB);
    }


    /** Loads parameters in Dialog.
     * @param settings to load from
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_leftColumns = settings.getStringArray(LEFT_COLUMNS, (String[])null);
        m_rightColumns = settings.getStringArray(RIGHT_COLUMNS, (String[])null);
        m_confidenceIntervalProb = settings.getDouble(CONFIDENCE_INTERVAL_PROB,
                0.95);
    }


    /**
     * @return the leftColumns
     */
    public String[] getLeftColumns() {
        return m_leftColumns;
    }

    /**
     * @param leftColumns the leftColumns to set
     */
    public void setLeftColumns(final String[] leftColumns) {
        m_leftColumns = leftColumns;
    }

    /**
     * @return the rightColumns
     */
    public String[] getRightColumns() {
        return m_rightColumns;
    }

    /**
     * @param rightColumns the rightColumns to set
     */
    public void setRightColumns(final String[] rightColumns) {
        m_rightColumns = rightColumns;
    }

    /**
     * @return the confidenceIntervalProb
     */
    public double getConfidenceIntervalProb() {
        return m_confidenceIntervalProb;
    }

    /**
     * @param confidenceIntervalProb the confidenceIntervalProb to set
     */
    public void setConfidenceIntervalProb(final double confidenceIntervalProb) {
        m_confidenceIntervalProb = confidenceIntervalProb;
    }

}
