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
