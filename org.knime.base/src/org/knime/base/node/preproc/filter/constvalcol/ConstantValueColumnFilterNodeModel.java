/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   4 Apr 2018 (Marc): created
 */
package org.knime.base.node.preproc.filter.constvalcol;

import java.io.File;
import java.io.IOException;

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
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * The model for the constant value column filter node. Contains the logic for filtering columns containing only
 * duplicates of the same value from the input data table.
 *
 * @author Marc Bux, KNIME AG, Zurich, Switzerland
 * @since 3.6
 */
public class ConstantValueColumnFilterNodeModel extends NodeModel {
    /**
     * The name of the settings tag which holds the names of the columns the user has selected in the dialog as
     * to-be-filtered
     */
    public static final String SELECTED_COLS = "filter-list";

    /**
     * The name of the settings tag for the option to filter all constant value columns.
     */
    public static final String FILTER_ALL = "filter-all";

    /**
     * The name of the settings tag for the option to filter columns with a specific constant numeric value.
     */
    public static final String FILTER_NUMERIC = "filter-numeric";

    /**
     * The name of the settings tag that holds the specific numeric value that is to be looked for in filtering.
     */
    public static final String FILTER_NUMERIC_VALUE = "filter-numeric-value";

    /**
     * The name of the settings tag for the option to filter columns with a specific constant String value.
     */
    public static final String FILTER_STRING = "filter-string";

    /**
     * The name of the settings tag that holds the specific String value that is to be looked for in filtering.
     */
    public static final String FILTER_STRING_VALUE = "filter-string-value";

    /**
     * The name of the settings tag for the option to filter columns containing only missing values.
     */
    public static final String FILTER_MISSING = "filter-missing";

    /**
     * The warning message that is shown when this node is applied to a one-row table.
     */
    private static final String WARNING_ONEROW =
        "Input table contains only one row. All of its columns are constant value columns.";

    /**
     * The warning message that is shown when this node is applied to an empty table.
     */
    private static final String WARNING_EMPTY = "Input table is empty. None of its columns are value columns.";

    /**
     * The configuration of the list of columns to include in / exclude from the filtering.
     */
    private final DataColumnSpecFilterConfiguration m_conf = new DataColumnSpecFilterConfiguration(SELECTED_COLS);

    /**
     * The settings model for the option to filter all constant value columns.
     */
    private final SettingsModelBoolean m_filterAll = new SettingsModelBoolean(FILTER_ALL, false);

    /**
     * The settings model for the option to filter columns with a specific constant numeric value.
     */
    private final SettingsModelBoolean m_filterNumeric = new SettingsModelBoolean(FILTER_NUMERIC, false);

    /**
     * The settings model for the specific numeric value that is to be looked for in filtering.
     */
    private final SettingsModelDouble m_filterNumericValue = new SettingsModelDouble(FILTER_NUMERIC_VALUE, 0);

    /**
     * The settings model for the option to filter columns with a specific constant String value.
     */
    private final SettingsModelBoolean m_filterString = new SettingsModelBoolean(FILTER_STRING, false);

    /**
     * The settings model for the specific String value that is to be looked for in filtering.
     */
    private final SettingsModelString m_filterStringValue = new SettingsModelString(FILTER_STRING_VALUE, "");

    /**
     * The settings model for the option to filter columns containing only missing values.
     */
    private final SettingsModelBoolean m_filterMissing = new SettingsModelBoolean(FILTER_MISSING, false);

    /**
     * Creates a new constant value column filter model with one and input and one output.
     */
    public ConstantValueColumnFilterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No internal state to load.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No internal state to save.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_conf.saveConfiguration(settings);
        m_filterAll.saveSettingsTo(settings);
        m_filterNumeric.saveSettingsTo(settings);
        m_filterNumericValue.saveSettingsTo(settings);
        m_filterString.saveSettingsTo(settings);
        m_filterStringValue.saveSettingsTo(settings);
        m_filterMissing.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration conf = new DataColumnSpecFilterConfiguration(SELECTED_COLS);
        SettingsModelBoolean filterAll = new SettingsModelBoolean(FILTER_ALL, false);
        SettingsModelBoolean filterNumeric = new SettingsModelBoolean(FILTER_NUMERIC, false);
        SettingsModelDouble filterNumericValue = new SettingsModelDouble(FILTER_NUMERIC_VALUE, 0);
        SettingsModelBoolean filterString = new SettingsModelBoolean(FILTER_STRING, false);
        SettingsModelString filterStringValue = new SettingsModelString(FILTER_STRING_VALUE, "");
        SettingsModelBoolean filterMissing = new SettingsModelBoolean(FILTER_MISSING, false);

        conf.loadConfigurationInModel(settings);
        filterAll.loadSettingsFrom(settings);
        filterNumeric.loadSettingsFrom(settings);
        filterNumericValue.loadSettingsFrom(settings);
        filterString.loadSettingsFrom(settings);
        filterStringValue.loadSettingsFrom(settings);
        filterMissing.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_conf.loadConfigurationInModel(settings);
        m_filterAll.loadSettingsFrom(settings);
        m_filterNumeric.loadSettingsFrom(settings);
        m_filterNumericValue.loadSettingsFrom(settings);
        m_filterString.loadSettingsFrom(settings);
        m_filterStringValue.loadSettingsFrom(settings);
        m_filterMissing.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // No internal state to reset.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // The columns containing only constant values cannot be determined without looking at the data contained within
        // the table. Hence, the DataTableSpec cannot be determined before execution onset.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable inputTable = inData[0];
        DataTableSpec inputTableSpec = inputTable.getDataTableSpec();
        FilterResult filterResult = m_conf.applyTo(inputTableSpec);
        String[] toFilter = filterResult.getIncludes();

        if (inputTable.size() == 1) {
            setWarningMessage(WARNING_ONEROW);
        }
        if (inputTable.size() < 1) {
            setWarningMessage(WARNING_EMPTY);
        }

        ConstantValueColumnFilter filter = new ConstantValueColumnFilter(m_filterAll.getBooleanValue(),
            m_filterNumeric.getBooleanValue(), m_filterNumericValue.getDoubleValue(), m_filterString.getBooleanValue(),
            m_filterStringValue.getStringValue(), m_filterMissing.getBooleanValue());
        String[] toRemove = filter.determineConstantValueColumns(inputTable, toFilter);

        ColumnRearranger columnRearranger = new ColumnRearranger(inputTableSpec);
        columnRearranger.remove(toRemove);
        BufferedDataTable outputTable = exec.createColumnRearrangeTable(inputTable, columnRearranger, exec);
        return new BufferedDataTable[]{outputTable};
    }

}
