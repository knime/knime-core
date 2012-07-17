/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 */

package org.knime.base.node.preproc.columnaggregator;

import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.NamedAggregationOperator;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.File;
import java.io.IOException;


/**
 * {@link NodeModel} implementation of the column aggregator node.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class ColumnAggregatorNodeModel extends NodeModel {

    /**Configuration key for the aggregation method settings.*/
    protected static final String CFG_AGGREGATION_METHODS =
        "aggregationMethods";

    private final SettingsModelColumnFilter2 m_aggregationCols =
        createAggregationColsModel();

    private final SettingsModelBoolean m_removeRetainedCols =
        createRemoveRetainedColsModel();

    private final SettingsModelBoolean m_removeAggregationCols =
        createRemoveAggregationColsModel();

    private final SettingsModelIntegerBounded m_maxUniqueValues =
        createMaxUniqueValsModel();

    private final SettingsModelString m_valueDelimiter =
    createValueDelimiterModel();

    private final List<NamedAggregationOperator> m_methods =
        new ArrayList<NamedAggregationOperator>();

    /**
     * @return the maximum unique values model
     */
    static SettingsModelIntegerBounded createMaxUniqueValsModel() {
        return new SettingsModelIntegerBounded("maxNoneNumericalVals", 10000, 1,
                Integer.MAX_VALUE);
    }

    /**
     * @return the value delimiter model
     */
    static SettingsModelString createValueDelimiterModel() {
        return new SettingsModelString("valueDelimiter",
                GlobalSettings.STANDARD_DELIMITER);
    }

    /**
     * @return the remove aggregation column model
     */
    static SettingsModelBoolean createRemoveAggregationColsModel() {
        return new SettingsModelBoolean("removeAggregationColumns", false);
    }

    /**
     * @return the remove aggregation column model
     */
    static SettingsModelBoolean createRemoveRetainedColsModel() {
        return new SettingsModelBoolean("removeRetainedColumns", false);
    }

    /**
     * @return the aggregation column model
     */
    static SettingsModelColumnFilter2 createAggregationColsModel() {
        return new SettingsModelColumnFilter2("aggregationColumns");
    }

    /**Constructor for class ColumnAggregatorNodeModel.
     */
    protected ColumnAggregatorNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_methods.isEmpty()) {
            throw new InvalidSettingsException(
                    "Please select at least one aggregation method");
        }
        //check if at least one of the columns exists in the input table
        final DataTableSpec inSpec = inSpecs[0];
        final FilterResult filterResult = m_aggregationCols.applyTo(inSpec);
        final List<String> selectedCols =
            Arrays.asList(filterResult.getIncludes());
        if (selectedCols == null || selectedCols.isEmpty()) {
            throw new InvalidSettingsException(
                    "Please select at least one aggregation column");
        }
        int missing = 0;
        for (final String colName : selectedCols) {
            if (!inSpec.containsName(colName)) {
                missing++;
                setWarningMessage(colName + " not found in input table");
            }
        }
        if (missing > 0) {
            setWarningMessage(missing
                    + " of the selected columns not found in input table. "
                    + "See console for details.");
        }
        if (missing == selectedCols.size()) {
            throw new InvalidSettingsException(
                    "None of the selected columns found in input table.");
        }
        final AggregationCellFactory cellFactory = new AggregationCellFactory(
                inSpec, selectedCols, GlobalSettings.DEFAULT, m_methods);
        return new DataTableSpec[]{
                createRearranger(inSpec, cellFactory).createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable table = inData[0];
        final DataTableSpec origSpec = table.getSpec();
        final FilterResult filterResult = m_aggregationCols.applyTo(origSpec);
        final List<String> selectedCols =
            Arrays.asList(filterResult.getIncludes());
        final AggregationCellFactory cellFactory = new AggregationCellFactory(
                origSpec, selectedCols, new GlobalSettings(
                        m_maxUniqueValues.getIntValue(),
                        m_valueDelimiter.getStringValue(), origSpec,
                        table.getRowCount()), m_methods);
        final ColumnRearranger cr =
            createRearranger(origSpec, cellFactory);
        final BufferedDataTable out =
            exec.createColumnRearrangeTable(table, cr, exec);
        return new BufferedDataTable[]{out};
    }

    private ColumnRearranger createRearranger(final DataTableSpec oSpec,
            final CellFactory cellFactory) {
        final ColumnRearranger cr = new ColumnRearranger(oSpec);
        cr.append(cellFactory);
        final FilterResult filterResult = m_aggregationCols.applyTo(oSpec);
        if (m_removeAggregationCols.getBooleanValue()) {
            cr.remove(filterResult.getIncludes());
        }
        if (m_removeRetainedCols.getBooleanValue()) {
            cr.remove(filterResult.getExcludes());
        }
        return cr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_aggregationCols.saveSettingsTo(settings);
        m_removeRetainedCols.saveSettingsTo(settings);
        m_removeAggregationCols.saveSettingsTo(settings);
        m_valueDelimiter.saveSettingsTo(settings);
        m_maxUniqueValues.saveSettingsTo(settings);
        final Config cnfg = settings.addConfig(CFG_AGGREGATION_METHODS);
        NamedAggregationOperator.saveMethods(cnfg, m_methods);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
//        SettingsModelColumnFilter2 selectedColsModel =
//            (SettingsModelColumnFilter2) m_aggregationCols
//                .createCloneWithValidatedValue(settings);
//        if (selectedCols == null || selectedCols.isEmpty()) {
//            throw new InvalidSettingsException(
//                    "Please select at least one aggregation column");
//        }
        m_aggregationCols.validateSettings(settings);
        m_removeRetainedCols.validateSettings(settings);
        m_removeAggregationCols.validateSettings(settings);
        m_valueDelimiter.validateSettings(settings);
        m_maxUniqueValues.validateSettings(settings);
        if (!settings.containsKey(CFG_AGGREGATION_METHODS)) {
            throw new InvalidSettingsException(
                    "Methods configuration not found");
        }
        //check for duplicate column names
        final Config cnfg = settings.getConfig(
                ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS);
        final List<NamedAggregationOperator> methods =
            NamedAggregationOperator.loadMethods(cnfg);
        if (methods.isEmpty()) {
            throw new InvalidSettingsException(
                    "Please select at least one aggregation method");
        }
        final Map<String, Integer> colNames =
            new HashMap<String, Integer>(methods.size());
        int colIdx = 1;
        for (final NamedAggregationOperator method : methods) {
            final Integer oldIdx =
                colNames.put(method.getName(), Integer.valueOf(colIdx));
            if (oldIdx != null) {
                throw new InvalidSettingsException("Duplicate column name '"
                        + method.getName() + "' found in row " + oldIdx
                        + " and " + colIdx);
            }
            colIdx++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_aggregationCols.loadSettingsFrom(settings);
        m_removeRetainedCols.loadSettingsFrom(settings);
        m_removeAggregationCols.loadSettingsFrom(settings);
        m_valueDelimiter.loadSettingsFrom(settings);
        m_maxUniqueValues.loadSettingsFrom(settings);
        m_methods.clear();
        final Config cnfg = settings.getConfig(
                ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS);
        m_methods.addAll(NamedAggregationOperator.loadMethods(cnfg));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do

    }

}
