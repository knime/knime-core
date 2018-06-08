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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * The model for the constant value column filter node. Contains the logic for filtering columns containing only
 * duplicates of the same value from the input data table.
 *
 * @author Marc Bux, KNIME AG, Zurich, Switzerland
 * @since 3.6
 */
final class ConstantValueColumnFilterNodeModel extends NodeModel {
    /*
     * The warning message that is shown when this node is applied to a one-row table.
     */
    private static final String WARNING_ONEROW =
        "Input table contains only one row. All of its columns are considered constant value columns.";

    /*
     * The warning message that is shown when this node is applied to an empty table.
     */
    private static final String WARNING_SMALL_TABLE =
        "Input table has fewer rows than the minimum specified in the filter settings. Constant value column filtering disabled.";

    /*
     * The warning message to be displayed (in the workflow explorer) when partial filtering is selected, yet no type of
     * column (numeric, String, missing) is specified.
     */
    private static final String WARNING_NO_FILTER_SELECTED =
        "Filter configured to only remove columns with specific constant values but no such values are specified. Constant value column filtering disabled.";

    /*
     * The settings model for the list of columns to include in / exclude from the filtering.
     */
    private final SettingsModelColumnFilter2 m_columnFilter = createColumnFilterModel();

    /*
     * The settings model for the option to filter columns with a specific constant numeric value.
     */
    private final SettingsModelBoolean m_filterNumeric = createFilterNumericModel();

    /*
     * The settings model for the specific numeric value that is to be looked for in filtering.
     */
    private final SettingsModelDouble m_filterNumericValue = createFilterNumericValueModel();

    /*
     * The settings model for the option to filter columns with a specific constant String value.
     */
    private final SettingsModelBoolean m_filterString = createFilterStringModel();

    /*
     * The settings model for the specific String value that is to be looked for in filtering.
     */
    private final SettingsModelString m_filterStringValue = createFilterStringValueModel();

    /*
     * The settings model for the option to filter columns containing only missing values.
     */
    private final SettingsModelBoolean m_filterMissing = createFilterMissingModel();

    /*
     * The settings model for specifying the minimum number of rows a table must have to be considered for filtering.
     */
    private final SettingsModelLong m_rowThreshold = createRowThresholdModel();

    /*
     * The settings model for the option to filter all constant value columns.
     */
    private final SettingsModelString m_filterAll = createFilterAllModel();

    /**
     * Creates a new constant value column filter model with one and input and one output.
     */
    public ConstantValueColumnFilterNodeModel() {
        super(1, 1);
    }

    /**
     * @return a new settings model for the list of columns to include in / exclude from the filtering
     */
    static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("filter-list");
    }

    /**
     * @return a new settings model for the option to filter columns with a specific constant numeric value
     */
    static SettingsModelBoolean createFilterNumericModel() {
        SettingsModelBoolean filterNumeric = new SettingsModelBoolean("filter-numeric", false);
        filterNumeric.setEnabled(false);
        return filterNumeric;
    }

    /**
     * @return a new settings model that holds the specific numeric value that is to be looked for in filtering
     */
    static SettingsModelDouble createFilterNumericValueModel() {
        SettingsModelDouble filterNumericValue = new SettingsModelDouble("filter-numeric-value", 0);
        filterNumericValue.setEnabled(false);
        return filterNumericValue;
    }

    /**
     * @return a new settings model for the option to filter columns with a specific constant String value
     */
    static SettingsModelBoolean createFilterStringModel() {
        SettingsModelBoolean filterString = new SettingsModelBoolean("filter-string", false);
        filterString.setEnabled(false);
        return filterString;
    }

    /**
     * @return a new settings model holds the specific String value that is to be looked for in filtering
     */
    static SettingsModelString createFilterStringValueModel() {
        SettingsModelString filterStringValue = new SettingsModelString("filter-string-value", "");
        filterStringValue.setEnabled(false);
        return filterStringValue;
    }

    /**
     * @return a new settings model for the option to filter columns containing only missing values
     */
    static SettingsModelBoolean createFilterMissingModel() {
        SettingsModelBoolean filterMissing = new SettingsModelBoolean("filter-missing", false);
        filterMissing.setEnabled(false);
        return filterMissing;
    }

    /**
     * @return a new settings model for the option to filter all constant value columns
     */
    static SettingsModelString createFilterAllModel() {
        return new SettingsModelString("filter-all", ConstantValueColumnFilterNodeDialogPane.FILTER_OPTIONS_ALL_LABEL);
    }

    /**
     * @return a new settings model for specifying the minimum number of rows a table must have to be considered for
     *         filtering
     */
    static SettingsModelLong createRowThresholdModel() {
        SettingsModelLong rowThreshold = new SettingsModelLong("row-threshold", 1);

        rowThreshold.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent arg0) {
                if (rowThreshold.getLongValue() < 1) {
                    rowThreshold.setLongValue(1l);
                }
            }
        });

        return rowThreshold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        /*
         * No internal state to load.
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        /*
         * No internal state to save.
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnFilter.saveSettingsTo(settings);
        m_filterAll.saveSettingsTo(settings);
        m_filterNumeric.saveSettingsTo(settings);
        m_filterNumericValue.saveSettingsTo(settings);
        m_filterString.saveSettingsTo(settings);
        m_filterStringValue.saveSettingsTo(settings);
        m_filterMissing.saveSettingsTo(settings);
        m_rowThreshold.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnFilter.validateSettings(settings);
        m_filterNumericValue.validateSettings(settings);
        m_filterStringValue.validateSettings(settings);
        m_filterAll.validateSettings(settings);
        m_filterNumeric.validateSettings(settings);
        m_filterString.validateSettings(settings);
        m_filterMissing.validateSettings(settings);
        m_rowThreshold.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnFilter.loadSettingsFrom(settings);
        m_filterAll.loadSettingsFrom(settings);
        m_filterNumeric.loadSettingsFrom(settings);
        m_filterNumericValue.loadSettingsFrom(settings);
        m_filterString.loadSettingsFrom(settings);
        m_filterStringValue.loadSettingsFrom(settings);
        m_filterMissing.loadSettingsFrom(settings);
        m_rowThreshold.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        /*
         * No internal state to reset.
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (!m_filterAll.getStringValue().equals(ConstantValueColumnFilterNodeDialogPane.FILTER_OPTIONS_ALL_LABEL)
            && !m_filterNumeric.getBooleanValue() && !m_filterString.getBooleanValue()
            && !m_filterMissing.getBooleanValue()) {
            setWarningMessage(WARNING_NO_FILTER_SELECTED);
        }

        /*
         * The columns containing only constant values cannot be determined without looking at the data contained within
         * the table. Hence, the DataTableSpec cannot be determined before execution onset.
         */
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
        FilterResult filterResult = m_columnFilter.applyTo(inputTableSpec);
        String[] toFilter = filterResult.getIncludes();

        if (inputTable.size() < m_rowThreshold.getLongValue()) {
            setWarningMessage(WARNING_SMALL_TABLE);
        } else if (inputTable.size() == 1) {
            setWarningMessage(WARNING_ONEROW);
        } else if (!m_filterAll.getStringValue()
            .equals(ConstantValueColumnFilterNodeDialogPane.FILTER_OPTIONS_ALL_LABEL)
            && !m_filterNumeric.getBooleanValue() && !m_filterString.getBooleanValue()
            && !m_filterMissing.getBooleanValue()) {
            setWarningMessage(WARNING_NO_FILTER_SELECTED);
        }

        ConstantValueColumnFilter filter = new ConstantValueColumnFilter.ConstantValueColumnFilterBuilder()
            .filterAll(
                m_filterAll.getStringValue().equals(ConstantValueColumnFilterNodeDialogPane.FILTER_OPTIONS_ALL_LABEL))
            .filterNumeric(m_filterNumeric.getBooleanValue()).filterNumericValue(m_filterNumericValue.getDoubleValue())
            .filterString(m_filterString.getBooleanValue()).filterStringValue(m_filterStringValue.getStringValue())
            .filterMissing(m_filterMissing.getBooleanValue()).rowThreshold(m_rowThreshold.getLongValue())
            .createConstantValueColumnFilter();

        String[] toRemove = filter.determineConstantValueColumns(inputTable, toFilter, exec);

        ColumnRearranger columnRearranger = new ColumnRearranger(inputTableSpec);
        columnRearranger.remove(toRemove);

        BufferedDataTable outputTable = exec.createColumnRearrangeTable(inputTable, columnRearranger, exec);
        return new BufferedDataTable[]{outputTable};
    }
}
