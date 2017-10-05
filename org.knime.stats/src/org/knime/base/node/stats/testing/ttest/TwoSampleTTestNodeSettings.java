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
 */
package org.knime.base.node.stats.testing.ttest;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;


/**
 * The settings object for the "Two-Sample T-Test" Node.
 */
public class TwoSampleTTestNodeSettings {
    private static final String TEST_COLUMNS = "testColumns";
    private static final String GROUPING_COLUMN = "groupingColumn";
    private static final String GROUP_ONE = "groupOne";
    private static final String GROUP_TWO = "groupTwo";
    private static final String CONFIDENCE_INTERVAL_PROB =
        "confidenceIntervalProb";
    private static final String INCLUDE_ALL = "includeAll";

    private DataColumnSpecFilterConfiguration m_testColumns;
    private String m_groupingColumn;
    private String m_groupOne;
    private String m_groupTwo;
    private double m_confidenceIntervalProb;

	/**
	 * Create a settings object with default values.
	 */
	public TwoSampleTTestNodeSettings() {
        m_testColumns = new DataColumnSpecFilterConfiguration(TEST_COLUMNS,
                new DataTypeColumnFilter(DoubleValue.class));
	    m_groupingColumn = null;
	    m_groupOne = null;
	    m_groupTwo = null;
	    m_confidenceIntervalProb = 0.95;
	}

	/** Saves current parameters to settings object.
     * @param settings to save to
     */
    public void saveSettings(final NodeSettingsWO settings) {
        m_testColumns.saveConfiguration(settings);
        settings.addString(GROUPING_COLUMN, m_groupingColumn);
        settings.addString(GROUP_ONE, m_groupOne);
        settings.addString(GROUP_TWO, m_groupTwo);
        settings.addDouble(CONFIDENCE_INTERVAL_PROB, m_confidenceIntervalProb);
    }

    /** Loads parameters in NodeModel.
     * @param settings to load from
     * @throws InvalidSettingsException if settings are inconsistent
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_testColumns.loadConfigurationInModel(settings);
        m_groupingColumn = settings.getString(GROUPING_COLUMN);
        m_groupOne = settings.getString(GROUP_ONE);
        m_groupTwo = settings.getString(GROUP_TWO);
        m_confidenceIntervalProb = settings.getDouble(CONFIDENCE_INTERVAL_PROB);
    }


    /** Loads parameters in Dialog.
     * @param settings to load from
     * @param spec the spec of the input table
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings,
            final DataTableSpec spec) {
        m_testColumns.loadConfigurationInDialog(settings, spec);
        m_groupingColumn = settings.getString(GROUPING_COLUMN, null);
        m_groupOne = settings.getString(GROUP_ONE, null);
        m_groupTwo = settings.getString(GROUP_TWO, null);
        m_confidenceIntervalProb = settings.getDouble(CONFIDENCE_INTERVAL_PROB,
                0.95);
    }


    /**
     * @return the testColumns
     */
    public DataColumnSpecFilterConfiguration getTestColumns() {
        return m_testColumns;
    }

    /**
     * @param testColumns the testColumns to set
     */
    public void setTestColumns(final DataColumnSpecFilterConfiguration testColumns) {
        m_testColumns = testColumns;
    }

    /**
     * @return the groupingColumn
     */
    public String getGroupingColumn() {
        return m_groupingColumn;
    }

    /**
     * @param groupingColumn the groupingColumn to set
     */
    public void setGroupingColumn(final String groupingColumn) {
        m_groupingColumn = groupingColumn;
    }

    /**
     * @return the groupOne
     */
    public String getGroupOne() {
        return m_groupOne;
    }

    /**
     * @param groupOne the groupOne to set
     */
    public void setGroupOne(final String groupOne) {
        m_groupOne = groupOne;
    }

    /**
     * @return the groupTwo
     */
    public String getGroupTwo() {
        return m_groupTwo;
    }

    /**
     * @param groupTwo the groupTwo to set
     */
    public void setGroupTwo(final String groupTwo) {
        m_groupTwo = groupTwo;
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
