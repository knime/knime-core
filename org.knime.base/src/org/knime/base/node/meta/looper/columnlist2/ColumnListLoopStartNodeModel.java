/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   16.12.2009 (meinl): created
 */
package org.knime.base.node.meta.looper.columnlist2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * This is the model for the column list loop start node that does the real
 * work.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ColumnListLoopStartNodeModel extends NodeModel implements LoopStartNodeTerminator {

    /** Config identifier for columns for execution policy if no input
     * columns are selected. */
    static final String CFG_NO_COLUMNS_POLICY = "no_columns_policy";

    /** Boolean Settings to store the execution policy if no input columns are selected.
     * True means to run one iteration and False means the node should fail. */
    private final SettingsModelBoolean m_noColumnsSettings = createNoColumnsPolicySetings();

    private DataColumnSpecFilterConfiguration m_filterConfig;

    private int m_currentColIndex = 0;

    private boolean m_lastIteration;

    private int m_iteration;

    private String[] m_included;

    private String[] m_alwaysIncludedColumns;

    /**
     * Creates a new model with one data out- and inport, respectively.
     */
    public ColumnListLoopStartNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];
        if (m_filterConfig == null) {
            m_filterConfig = createDCSFilterConfiguration();
            m_filterConfig.loadDefaults(spec, true);
            throw new InvalidSettingsException("No settings available.");
        }

        FilterResult filter = m_filterConfig.applyTo(spec);

        assert m_iteration == 0;
        pushFlowVariableInt("currentIteration", m_iteration);

        m_included = filter.getIncludes();
        m_alwaysIncludedColumns = filter.getExcludes();

        boolean runOneIter = m_noColumnsSettings.getBooleanValue();
        if (m_included.length == 0 && !runOneIter) {
            throw new InvalidSettingsException("No columns selected.");
        }

        ColumnRearranger crea = createRearranger(inSpecs[0]);

        return new DataTableSpec[]{crea.createSpec()};
    }

    private ColumnRearranger createRearranger(final DataTableSpec inSpec) {

        boolean runOneIter = m_noColumnsSettings.getBooleanValue();
        if (m_included.length == 0 && runOneIter) {
            return new ColumnRearranger(inSpec);
        }

        int alwaysInclColLength = m_alwaysIncludedColumns.length + 1;
        String[] newColChunk = new String[alwaysInclColLength];

        newColChunk = Arrays.copyOf(m_alwaysIncludedColumns, alwaysInclColLength);
        newColChunk[alwaysInclColLength - 1] = m_included[m_currentColIndex];

        pushFlowVariableString("currentColumnName", m_included[m_currentColIndex]);

        ColumnRearranger crea = new ColumnRearranger(inSpec);

        crea.keepOnly(newColChunk);
        return crea;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger crea = createRearranger(inData[0].getDataTableSpec());

        m_currentColIndex++;

        m_lastIteration = m_currentColIndex >= m_included.length;

        pushFlowVariableInt("currentIteration", m_iteration);
        // increment counter for next iteration
        m_iteration++;

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(
                inData[0], crea, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_currentColIndex = 0;
        m_iteration = 0;
        m_lastIteration = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return m_lastIteration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration config = createDCSFilterConfiguration();
        config.loadConfigurationInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration conf = createDCSFilterConfiguration();
        conf.loadConfigurationInModel(settings);
        m_filterConfig = conf;
        // added in 3.2
        m_noColumnsSettings.setBooleanValue(settings.getBoolean(CFG_NO_COLUMNS_POLICY, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_filterConfig.saveConfiguration(settings);
        m_noColumnsSettings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no op
    }

    /**
     * A new configuration to store the settings. Only Columns of Type String are available.
     *
     * @return filter configuration
     */
    static final DataColumnSpecFilterConfiguration createDCSFilterConfiguration() {
        return new DataColumnSpecFilterConfiguration("column-filter");
    }

    /**
     * Create a new {@link SettingsModelBoolean} to store the execution policy
     * if no input columns are selected.
     *
     * @return settings model
     */
    static final SettingsModelBoolean createNoColumnsPolicySetings() {
        return new SettingsModelBoolean(CFG_NO_COLUMNS_POLICY, true);
    }
}
