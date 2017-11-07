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
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * This is the model implementation of "One-Sample T-Test" Node.
 *
 * @author Heiko Hofer
 */
public class OneSampleTTestNodeModel extends NodeModel
        implements BufferedDataTableHolder {
	private final OneSampleTTestNodeSettings m_settings;
	private BufferedDataTable m_descStats;
	private BufferedDataTable m_stats;

    /**
     * Constructor for the node model.
     */
    protected OneSampleTTestNodeModel() {
        super(1, 2);
        m_settings = new OneSampleTTestNodeSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];
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
                OneSampleTTestStatistics.getTableSpec()
                , OneSampleTTestStatistics.getDescStatsSpec()};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec spec = inData[0].getSpec();
        FilterResult filter = m_settings.getTestColumns().applyTo(spec);
        OneSampleTTest test = new OneSampleTTest(filter.getIncludes(),
                m_settings.getTestValue(),
                m_settings.getConfidenceIntervalProb());
        OneSampleTTestStatistics[] result = test.execute(inData[0], exec);

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
            final OneSampleTTestStatistics[] result, final ExecutionContext exec) {
        BufferedDataContainer cont = exec.createDataContainer(
                OneSampleTTestStatistics.getDescStatsSpec());
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
            final OneSampleTTestStatistics[] result, final ExecutionContext exec) {
        BufferedDataContainer cont = exec.createDataContainer(
                OneSampleTTestStatistics.getTableSpec());
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
    OneSampleTTestNodeSettings getSettings() {
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
    	OneSampleTTestNodeSettings s = new OneSampleTTestNodeSettings();
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

