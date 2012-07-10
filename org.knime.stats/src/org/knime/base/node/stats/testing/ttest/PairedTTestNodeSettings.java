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
