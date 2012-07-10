package org.knime.base.node.stats.testing.ttest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.knime.base.node.stats.testing.ttest.Grouping.Group;
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
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * This is the model implementation of "Two-Sample T-Test" Node.
 *
 * @author Heiko Hofer
 */
public class TwoSampleTTestNodeModel extends NodeModel
        implements BufferedDataTableHolder {
	private final TwoSampleTTestNodeSettings m_settings;
	private BufferedDataTable m_descStats;
	private BufferedDataTable m_leveneStats;
	private BufferedDataTable m_stats;

    /**
     * Constructor for the node model.
     */
    protected TwoSampleTTestNodeModel() {
        super(1, 3);
        m_settings = new TwoSampleTTestNodeSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];

        if (m_settings.getGroupingColumn() == null
                || !spec.containsName(m_settings.getGroupingColumn())) {
            throw new InvalidSettingsException(
                    "Please define a grouping column.");
        }
        if (m_settings.getGroupOne() == null) {
            throw new InvalidSettingsException(
                    "Value of group one is not set.");
        }
        if (m_settings.getGroupTwo() == null) {
            throw new InvalidSettingsException(
                    "Value of group two is not set.");
        }

        FilterResult filterResult = m_settings.getTestColumns().applyTo(spec);
        if (filterResult.getIncludes().length == 0) {
            if (filterResult.getExcludes().length > 0) {
                throw new InvalidSettingsException("Please select at least "
                        + "one test column.");
            } else {
                throw new InvalidSettingsException(
                      "There are no numeric columns "
                    + "in the input table. At least one numeric column "
                    + "is needed to perform the test.");
            }
        }

        return new DataTableSpec[]{
                TwoSampleTTestStatistics.getTableSpec()
                , LeveneTestStatistics.getTableSpec()
                , TwoSampleTTestStatistics.getGroupStatisticsSpec()};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        Map<Group, String> groups = new LinkedHashMap<Group, String>();
        groups.put(Group.GroupX, m_settings.getGroupOne());
        groups.put(Group.GroupY, m_settings.getGroupTwo());
        Grouping grouping = new StringValueGrouping(
                m_settings.getGroupingColumn(), groups);

        DataTableSpec spec = inData[0].getSpec();
        FilterResult filter = m_settings.getTestColumns().applyTo(spec);
        TwoSampleTTest test = new TwoSampleTTest(filter.getIncludes(),
                grouping, m_settings.getConfidenceIntervalProb());
        TwoSampleTTestStatistics[] result = test.execute(inData[0], exec);
        LeveneTest leveneTest = new LeveneTest(filter.getIncludes(),
                grouping, getGroupSummaryStats(result));
        LeveneTestStatistics[] leveneResult =
            leveneTest.execute(inData[0], exec);
        leveneResult[0].getTTestCells();

        m_descStats = getDescriptiveStatisticsTable(result, exec);
        m_leveneStats = getLeveneStatistices(leveneResult, exec);
        m_stats = getTestStatisticsTable(result, exec);

        return new BufferedDataTable[]{m_stats, m_leveneStats, m_descStats};
    }


    private List<Map<Group, SummaryStatistics>> getGroupSummaryStats(
            final TwoSampleTTestStatistics[] result) {
        List<Map<Group, SummaryStatistics>> gstats =
            new ArrayList<Map<Group, SummaryStatistics>>();
        for (TwoSampleTTestStatistics r : result) {
            gstats.add(r.getGroupSummaryStatistics());
        }
        return gstats;
    }

    /**
     * Get table with descriptive statistics
     * @param result test statistic
     * @param exec the exection context
     * @return a combined table of the test statistic
     */
    private BufferedDataTable getDescriptiveStatisticsTable(
            final TwoSampleTTestStatistics[] result,
            final ExecutionContext exec) {
        BufferedDataContainer cont = exec.createDataContainer(
                TwoSampleTTestStatistics.getGroupStatisticsSpec());
        int r = 0;
        for (int i = 0; i < result.length; i++) {
            for (List<DataCell> cells : result[i].getGroupStatisticsCells()) {
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
     * @param exec the execution context
     * @return a combined table of the test statistic
     */
    private BufferedDataTable getTestStatisticsTable(
            final TwoSampleTTestStatistics[] result,
            final ExecutionContext exec) {
        BufferedDataContainer cont = exec.createDataContainer(
                TwoSampleTTestStatistics.getTableSpec());
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
     * Get the table with the Levene-Test statistics
     * @param leveneResult the Levene-Test results
     * @param exec the execution context
     * @return the table with the Levene-Test statistics
     */
    private BufferedDataTable getLeveneStatistices(
            final LeveneTestStatistics[] leveneResult,
            final ExecutionContext exec) {
        BufferedDataContainer cont = exec.createDataContainer(
                LeveneTestStatistics.getTableSpec());
        int r = 0;
        for (int i = 0; i < leveneResult.length; i++) {
            for (List<DataCell> cells : leveneResult[i].getTTestCells()) {
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
        return m_stats != null && m_leveneStats != null && m_descStats != null;
    }

    /**
     * Get the settings of the node.
     *
     * @return the settings object
     */
    TwoSampleTTestNodeSettings getSettings() {
        return m_settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{
                m_stats, m_leveneStats, m_descStats};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        if (tables.length > 1) {
            m_stats = tables[0];
            m_leveneStats = tables[1];
            m_descStats = tables[2];
        }
    }


	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_stats = null;
        m_leveneStats = null;
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
    	TwoSampleTTestNodeSettings s = new TwoSampleTTestNodeSettings();
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
     * Get the table with the Levene-Test statistics
     * @return the descriptive statistics
     */
    public BufferedDataTable getLeveneTestStatistics() {
        return m_leveneStats;
    }

    /**
     * Get the table with the test statistics
     * @return the test statistics
     */
    public BufferedDataTable getTestStatistics() {
        return m_stats;
    }

}

