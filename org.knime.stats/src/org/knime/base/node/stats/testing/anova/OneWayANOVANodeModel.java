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
package org.knime.base.node.stats.testing.anova;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.knime.base.node.stats.testing.levene.LeveneTest;
import org.knime.base.node.stats.testing.levene.LeveneTestStatistics;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
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
 * This is the model implementation of "one-way ANOVA" Node.
 *
 * @author Heiko Hofer
 */
public class OneWayANOVANodeModel extends NodeModel
        implements BufferedDataTableHolder {
	private final OneWayANOVANodeSettings m_settings;
	private BufferedDataTable m_descStats;
	private BufferedDataTable m_leveneStats;
	private BufferedDataTable m_stats;

    /**
     * Constructor for the node model.
     */
    protected OneWayANOVANodeModel() {
        super(1, 3);
        m_settings = new OneWayANOVANodeSettings();
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
        if (m_settings.getConfidenceIntervalProb() > 0.99 ||
                m_settings.getConfidenceIntervalProb() < 0.01) {
            throw new InvalidSettingsException("The property "
                    + "\"Confidence Interval (in %)\" must be in the range "
                    + "[1, 99].");
        }
        return new DataTableSpec[]{
                OneWayANOVAStatistics.getTableSpec()
                , LeveneTestStatistics.getTableSpec()
                , OneWayANOVAStatistics.getGroupStatisticsSpec()};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        DataTableSpec spec = inData[0].getSpec();
        FilterResult filter = m_settings.getTestColumns().applyTo(spec);
        ExecutionMonitor groupExec = exec.createSubProgress(0.3);
        ExecutionMonitor oneWayAnovaExec = exec.createSubProgress(0.3);
        ExecutionMonitor leveneExec = exec.createSubProgress(0.4); // runs twice, maybe
        exec.setMessage("Determining groups");
        List<String> groups = getGroups(inData[0], groupExec);
        groupExec.setProgress(1.0);

        exec.setMessage("Computing ANOVA");
        OneWayANOVA test = new OneWayANOVA(filter.getIncludes(),
                m_settings.getGroupingColumn(),
                groups, m_settings.getConfidenceIntervalProb());
        OneWayANOVAStatistics[] result = test.execute(inData[0], oneWayAnovaExec);
        oneWayAnovaExec.setProgress(1.0);


        exec.setMessage("Computing Levene");
        LeveneTest leveneTest = new LeveneTest(filter.getIncludes(),
                m_settings.getGroupingColumn(),
                groups,
                getGroupSummaryStats(result));
        LeveneTestStatistics[] leveneResult = leveneTest.execute(
                inData[0], leveneExec);
        leveneResult[0].getTTestCells();
        leveneExec.setProgress(1.0);

        exec.setMessage("Assembling output");
        m_descStats = getDescriptiveStatisticsTable(result, exec);
        m_leveneStats = getLeveneStatistices(leveneResult, exec);
        m_stats = getTestStatisticsTable(result, exec);

        return new BufferedDataTable[]{m_stats, m_leveneStats, m_descStats};
    }

    private List<String> getGroups(final BufferedDataTable inData,
            final ExecutionMonitor exec) throws InvalidSettingsException, CanceledExecutionException {
        DataTableSpec spec = inData.getSpec();
        int gIndex = spec.findColumnIndex(m_settings.getGroupingColumn());
        LinkedHashSet<String> groups = new LinkedHashSet<String>();
        if (gIndex < 0) {
            throw new InvalidSettingsException("Grouping column not found.");
        }
        final int rowCount = inData.getRowCount();
        int rowIndex = 0;
        for (DataRow row : inData) {
            exec.checkCanceled();
            exec.setProgress(rowIndex++ / (double)rowCount,
                    rowIndex + "/" + rowCount + " (\"" + row.getKey() + "\")");
            DataCell group = row.getCell(gIndex);
            if (!group.isMissing()) {
                groups.add(group.toString());
            }
        }
        return Arrays.asList(groups.toArray(new String[groups.size()]));
    }

    private List<List<SummaryStatistics>> getGroupSummaryStats(
            final OneWayANOVAStatistics[] result) {
        List<List<SummaryStatistics>> gstats =
            new ArrayList<List<SummaryStatistics>>();
        for (OneWayANOVAStatistics r : result) {
            List<SummaryStatistics> stats = new ArrayList<SummaryStatistics>();
            stats.addAll(r.getGroupSummaryStatistics());
            gstats.add(stats);
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
            final OneWayANOVAStatistics[] result,
            final ExecutionContext exec) {
        BufferedDataContainer cont = exec.createDataContainer(
                OneWayANOVAStatistics.getGroupStatisticsSpec());
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
            final OneWayANOVAStatistics[] result,
            final ExecutionContext exec) {
        BufferedDataContainer cont = exec.createDataContainer(
                OneWayANOVAStatistics.getTableSpec());
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
    OneWayANOVANodeSettings getSettings() {
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
    	OneWayANOVANodeSettings s = new OneWayANOVANodeSettings();
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

