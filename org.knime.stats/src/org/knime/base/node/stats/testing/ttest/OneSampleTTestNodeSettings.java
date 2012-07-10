package org.knime.base.node.stats.testing.ttest;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;


/**
 * The settings object for the "One-Sample T-Test" Node.
 */
public class OneSampleTTestNodeSettings {
    private static final String TEST_COLUMNS = "testColumns";
    private static final String TEST_VALUE = "testValue";
    private static final String CONFIDENCE_INTERVAL_PROB =
        "confidenceIntervalProb";

    private DataColumnSpecFilterConfiguration m_testColumns;
    private double m_testValue;
    private double m_confidenceIntervalProb;

	/**
	 * Create a settings object with default values.
	 */
	public OneSampleTTestNodeSettings() {
	    m_testColumns = null;
	    m_testValue = 0;
	    m_confidenceIntervalProb = 0.95;
	    m_testColumns = new DataColumnSpecFilterConfiguration(TEST_COLUMNS,
                new DataTypeColumnFilter(DoubleValue.class));
	}

	/** Saves current parameters to settings object.
     * @param settings to save to
     */
    public void saveSettings(final NodeSettingsWO settings) {
        m_testColumns.saveConfiguration(settings);
        settings.addDouble(TEST_VALUE, m_testValue);
        settings.addDouble(CONFIDENCE_INTERVAL_PROB, m_confidenceIntervalProb);
    }

    /** Loads parameters in NodeModel.
     * @param settings to load from
     * @throws InvalidSettingsException if settings are inconsistent
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_testColumns.loadConfigurationInModel(settings);
        m_testValue = settings.getDouble(TEST_VALUE);
        m_confidenceIntervalProb = settings.getDouble(CONFIDENCE_INTERVAL_PROB);
    }


    /** Loads parameters in Dialog.
     * @param settings to load from
     * @param spec the spec of the input table
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings,
            final DataTableSpec spec) {
        m_testColumns.loadConfigurationInDialog(settings, spec);
        m_testValue = settings.getDouble(TEST_VALUE, 0);
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
     * @return the testValue
     */
    public double getTestValue() {
        return m_testValue;
    }

    /**
     * @param testValue the testValue to set
     */
    public void setTestValue(final double testValue) {
        m_testValue = testValue;
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
