package org.knime.base.node.stats.testing.ttest;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of "Paired T-Test" Node.
 *
 * @author Heiko Hofer
 */
public class PairedTTestNodeModel extends NodeModel
        implements BufferedDataTableHolder {
	private final PairedTTestNodeSettings m_settings;
	private BufferedDataTable m_descStats;
	private BufferedDataTable m_stats;

    /**
     * Constructor for the node model.
     */
    protected PairedTTestNodeModel() {
        super(1, 2);
        m_settings = new PairedTTestNodeSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_settings.getLeftColumns() == null
                || m_settings.getLeftColumns().length == 0) {
            throw new InvalidSettingsException(
                    "Please select at least one pair.");
        }
        if (m_settings.getConfidenceIntervalProb() > 0.99 ||
                m_settings.getConfidenceIntervalProb() < 0.01) {
            throw new InvalidSettingsException("The property "
                    + "\"Confidence Interval (in %)\" must be in the range "
                    + "[1, 99].");
        }


        return new DataTableSpec[]{
                PairedTTestStatistics.getTableSpec()
                , PairedTTestStatistics.getDescStatsSpec()};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        PairedTTest test = new PairedTTest(m_settings.getLeftColumns(),
                m_settings.getRightColumns(),
                m_settings.getConfidenceIntervalProb());
        PairedTTestStatistics[] result = test.execute(inData[0], exec);
        m_descStats = getDescriptiveStatisticsTable(result, exec);
        m_stats = getTestStatisticsTable(result, exec);

        return new BufferedDataTable[]{m_stats, m_descStats};
    }

    /**
     * Get table with descriptive statistics
     * @param result test statistic
     * @param exec the exection context
     * @return a combined table of the test statistic
     */
    private BufferedDataTable getDescriptiveStatisticsTable(
            final PairedTTestStatistics[] result, final ExecutionContext exec) {
        BufferedDataContainer cont = exec.createDataContainer(
                PairedTTestStatistics.getDescStatsSpec());
        int r = 0;
        for (int i = 0; i < result.length; i++) {
            for (List<DataCell> cells : result[i].getDescStatsCells()) {
                cont.addRowToTable(new DefaultRow(RowKey.createRowKey(r),
                        cells));
                r++;
            }
        }
        cont.close();
        return cont.getTable();
    }

    /**
     * Get table with test statistics
     * @param result test statistic
     * @param exec the exection context
     * @return a combined table of the test statistic
     */
    private BufferedDataTable getTestStatisticsTable(
            final PairedTTestStatistics[] result, final ExecutionContext exec) {
        BufferedDataContainer cont = exec.createDataContainer(
                PairedTTestStatistics.getTableSpec());
        int r = 0;
        for (int i = 0; i < result.length; i++) {
            for (List<DataCell> cells : result[i].getTTestCells()) {
                cont.addRowToTable(new DefaultRow(RowKey.createRowKey(r),
                        cells));
                r++;
            }
        }
        cont.close();
        return cont.getTable();
    }

    /**
     * Returns <code>true</code> if model is available, i.e. node has been
     * executed.
     *
     * @return if model has been executed
     */
    boolean isDataAvailable() {
        return m_stats != null && m_descStats != null;
    }

    /**
     * Get the settings of the node.
     *
     * @return the settings object
     */
    PairedTTestNodeSettings getSettings() {
        return m_settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{
                m_stats, m_descStats};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        if (tables.length > 1) {
            m_stats = tables[0];
            m_descStats = tables[1];
        }
    }


	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_stats = null;
        m_descStats = null;
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	PairedTTestNodeSettings s = new PairedTTestNodeSettings();
        s.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	// no internals, nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	// no internals, nothing to save
    }

    /**
     * Get the table with the descriptive statistics
     * @return the descriptive statistics
     */
    public BufferedDataTable getDescritiveStatistics() {
        return m_descStats;
    }

    /**
     * Get the table with the test statistics
     * @return the test statistics
     */
    public BufferedDataTable getTestStatistics() {
        return m_stats;
    }

}

