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
 *
 * History
 *    28.06.2007 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.groupby;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The {@link NodeModel} implementation of the group by node which uses the
 * {@link GroupByTable} class implementations to create the resulting table.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class GroupByNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(GroupByNodeModel.class);

    /**
     * Old configuration key of the selected aggregation method for numerical
     * columns. This key was used prior Knime 2.0.
     */
    private static final String OLD_CFG_NUMERIC_COL_METHOD =
        "numericColumnMethod";
    /**
     * Old configuration key of the selected aggregation method for none
     * numerical columns. This was used by the previous version prior 2.0.
     */
    private static final String OLD_CFG_NOMINAL_COL_METHOD =
        "nominalColumnMethod";
    /**
     * Old configuration key for the move the group by columns to front option.
     * This key was used prior Knime 2.0.
     */
    private static final String OLD_CFG_MOVE_GROUP_BY_COLS_2_FRONT =
        "moveGroupByCols2Front";
    /**
     * Configuration key for the keep original column name option. This key was
     * used prior Knime 2.0.
     */
    private static final String OLD_CFG_KEEP_COLUMN_NAME = "keepColumnName";
    /**
     * This variable holds the label of the nominal method which was used prior
     * Knime 2.0.
     */
    private String m_oldNominal = null;
    /**
     * This variable holds the label of the numerical method which was used
     * prior Knime 2.0.
     */
    private String m_oldNumerical = null;

    private static final String INTERNALS_FILE_NAME = "hilite_mapping.xml.gz";

    /** Configuration key of the selected group by columns. */
    protected static final String CFG_GROUP_BY_COLUMNS = "grouByColumns";

    /** Configuration key for the maximum none numerical values. */
    protected static final String CFG_MAX_UNIQUE_VALUES =
        "maxNoneNumericalVals";

    /** Configuration key for the enable hilite option. */
    protected static final String CFG_ENABLE_HILITE = "enableHilite";

    /** This setting was used prior KNIME 2.6. */
    @Deprecated
    protected static final String CFG_SORT_IN_MEMORY = "sortInMemory";

    /** Configuration key for the retain order option. */
    protected static final String CFG_RETAIN_ORDER = "retainOrder";

    /** Configuration key for the in memory option. */
    protected static final String CFG_IN_MEMORY = "inMemory";

    /** Configuration key for the aggregation column name policy. */
    protected static final String CFG_COLUMN_NAME_POLICY = "columnNamePolicy";

    /**Configuration key for the value delimiter option.*/
    protected static final String CFG_VALUE_DELIMITER = "valueDelimiter";

    private final SettingsModelFilterString m_groupByCols =
        new SettingsModelFilterString(CFG_GROUP_BY_COLUMNS);

    private final SettingsModelIntegerBounded m_maxUniqueValues =
        new SettingsModelIntegerBounded(CFG_MAX_UNIQUE_VALUES, 10000, 1,
                Integer.MAX_VALUE);

    private final SettingsModelBoolean m_enableHilite =
        new SettingsModelBoolean(CFG_ENABLE_HILITE, false);
    
    //This setting was used prior KNNIME 2.6
    private final SettingsModelBoolean m_sortInMemory =
        new SettingsModelBoolean(CFG_SORT_IN_MEMORY, false);

    private final SettingsModelBoolean m_retainOrder = new SettingsModelBoolean(
            CFG_RETAIN_ORDER, false);

    private final SettingsModelBoolean m_inMemory = new SettingsModelBoolean(
            CFG_IN_MEMORY, false);

    private final SettingsModelString m_columnNamePolicy =
        new SettingsModelString(GroupByNodeModel.CFG_COLUMN_NAME_POLICY,
                ColumnNamePolicy.getDefault().getLabel());

    private final SettingsModelString m_valueDelimiter =
        new SettingsModelString(GroupByNodeModel.CFG_VALUE_DELIMITER,
            GlobalSettings.STANDARD_DELIMITER);

    private final List<ColumnAggregator> m_columnAggregators =
        new LinkedList<ColumnAggregator>();

    private List<ColumnAggregator> m_columnAggregators2Use;

    /**
     * Node returns a new hilite handler instance.
     */
    private final HiLiteTranslator m_hilite = new HiLiteTranslator();

    /** Creates a new group by model with one in- and one out-port. */
    public GroupByNodeModel() {
        this(1, 1);
    }

    /**
     * Creates a new group by model.
     * @param ins number of data input ports
     * @param outs number of data output ports
     */
    public GroupByNodeModel(final int ins, final int outs) {
        super(ins, outs);
        m_retainOrder.setEnabled(!m_inMemory.getBooleanValue());
    }

    /**
     * Call this method if the process in memory flag has changed.
     * @deprecated obsolete to be notified when consistent settings are
     *          loaded into the model (since 2.6)
     */
    @Deprecated
    protected void inMemoryChanged() {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettingsRO config = NodeSettings
                    .loadFromXML(new FileInputStream(new File(nodeInternDir,
                            INTERNALS_FILE_NAME)));
            try {
                setHiliteMapping(DefaultHiLiteMapper.load(config));
                m_hilite.addToHiLiteHandler(getInHiLiteHandler(0));
            } catch (final InvalidSettingsException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettings config = new NodeSettings("hilite_mapping");
            final DefaultHiLiteMapper mapper = (DefaultHiLiteMapper) m_hilite
                    .getMapper();
            if (mapper != null) {
                mapper.save(config);
            }
            config.saveToXML(new FileOutputStream(new File(nodeInternDir,
                    INTERNALS_FILE_NAME)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_groupByCols.saveSettingsTo(settings);
        m_maxUniqueValues.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
        //this setting was used prior KNIME 2.6
        m_sortInMemory.saveSettingsTo(settings);
        if (m_columnAggregators.isEmpty() && m_oldNominal != null
                && m_oldNumerical != null) {
            // these settings were used prior Knime 2.0
            settings.addString(OLD_CFG_NUMERIC_COL_METHOD, m_oldNumerical);
            settings.addString(OLD_CFG_NOMINAL_COL_METHOD, m_oldNominal);
        } else {
            ColumnAggregator.saveColumnAggregators(settings,
                    m_columnAggregators);
        }
        m_columnNamePolicy.saveSettingsTo(settings);
        m_retainOrder.saveSettingsTo(settings);
        m_inMemory.saveSettingsTo(settings);
        m_valueDelimiter.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_groupByCols.validateSettings(settings);
        final List<String> groupByCols =
            ((SettingsModelFilterString) m_groupByCols
                .createCloneWithValidatedValue(settings)).getIncludeList();
        m_maxUniqueValues.validateSettings(settings);
        m_enableHilite.validateSettings(settings);

        // the option to use a column multiple times was introduced
        // with Knime 2.0 as well as the naming policy
        try {
            final List<ColumnAggregator> aggregators = ColumnAggregator
                    .loadColumnAggregators(settings);
            if (groupByCols.isEmpty() && aggregators.isEmpty()) {
                throw new IllegalArgumentException(
                    "Please select at least one group or aggregation column");
            }
            ColumnNamePolicy namePolicy;
            try {
                final String policyLabel =
                    ((SettingsModelString) m_columnNamePolicy
                        .createCloneWithValidatedValue(settings))
                        .getStringValue();
                namePolicy = ColumnNamePolicy.getPolicy4Label(policyLabel);
            } catch (final InvalidSettingsException e) {
                namePolicy = compGetColumnNamePolicy(settings);
            }
            if (ColumnNamePolicy.KEEP_ORIGINAL_NAME.equals(namePolicy)) {
                // avoid using the same column multiple times
                final Set<String> uniqueNames = new HashSet<String>(
                        aggregators.size());
                for (final ColumnAggregator aggregator : aggregators) {
                    if (!uniqueNames.add(aggregator.getOriginalColSpec()
                            .getName())) {
                        throw new IllegalArgumentException(
                                "Duplicate column names: "
                                        + aggregator.getOriginalColSpec()
                                                .getName()
                                        + ". Not possible with "
                                        + "'Keep original name(s)' option");
                    }
                }
            } else {
                // avoid using the same column with the same method
                // multiple times
                final Set<String> uniqueAggregators = new HashSet<String>(
                        aggregators.size());
                for (final ColumnAggregator aggregator : aggregators) {
                    final String uniqueName = aggregator.getOriginalColName()
                            + "@" + aggregator.getMethodTemplate().getId()
                            + "@" + aggregator.inclMissingCells();
                    if (!uniqueAggregators.add(uniqueName)) {
                        throw new IllegalArgumentException(
                                "Duplicate settings: Column "
                                        + aggregator.getOriginalColSpec()
                                                .getName()
                                        + " with aggregation method "
                                        + aggregator.getMethodTemplate()
                                                .getLabel());
                    }
                }
            }
        } catch (final InvalidSettingsException e) {
            // these settings are prior Knime 2.0 and can't contain
            // a column several times
        } catch (final IllegalArgumentException e) {
            throw new InvalidSettingsException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        final boolean move2Front = settings.getBoolean(
                OLD_CFG_MOVE_GROUP_BY_COLS_2_FRONT, true);
        if (!move2Front) {
            setWarningMessage(
                    "Setting ignored: Group columns will be moved to front");
        }
        m_groupByCols.loadSettingsFrom(settings);
        m_columnAggregators.clear();
        try {
            // this option was introduced in Knime 2.0
            m_columnAggregators.addAll(ColumnAggregator
                    .loadColumnAggregators(settings));
            m_oldNumerical = null;
            m_oldNominal = null;
        } catch (final InvalidSettingsException e) {
            m_oldNumerical = settings.getString(OLD_CFG_NUMERIC_COL_METHOD);
            m_oldNominal = settings.getString(OLD_CFG_NOMINAL_COL_METHOD);
        }
        try {
            // this option was introduced in Knime 2.0
            m_columnNamePolicy.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            final ColumnNamePolicy colNamePolicy = GroupByNodeModel
                    .compGetColumnNamePolicy(settings);
            m_columnNamePolicy.setStringValue(colNamePolicy.getLabel());
        }
        try {
            // this option was introduced in Knime 2.0.3+
            m_retainOrder.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_retainOrder.setBooleanValue(false);
        }
        try {
            // this option was introduced in Knime 2.1.2+
            m_inMemory.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_inMemory.setBooleanValue(false);
        }
        m_maxUniqueValues.loadSettingsFrom(settings);
        m_enableHilite.loadSettingsFrom(settings);
        try {
            // this option was introduce in KNIME 2.4.+
            m_valueDelimiter.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_valueDelimiter.setStringValue(GlobalSettings.STANDARD_DELIMITER);
        }
    }

    /**
     * Applies a new mapping to the hilite translator.
     * @param mapper new hilite mapping, or null
     */
    protected final void setHiliteMapping(final DefaultHiLiteMapper mapper) {
        m_hilite.setMapper(mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_hilite.setMapper(null);
        for (final ColumnAggregator colAggr : m_columnAggregators) {
            colAggr.reset();
        }
        m_columnAggregators2Use = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        m_hilite.removeAllToHiliteHandlers();
        m_hilite.addToHiLiteHandler(hiLiteHdl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hilite.getFromHiLiteHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        if (inSpecs == null || inSpecs[0] == null) {
            throw new InvalidSettingsException(
                    "No input specification available.");
        }
        final DataTableSpec origSpec = (DataTableSpec) inSpecs[0];
        if (origSpec.getNumColumns() < 1) {
            setWarningMessage("Input table should contain at least one column");
        }

        final List<String> groupByCols = m_groupByCols.getIncludeList();
        if (groupByCols.isEmpty()) {
            setWarningMessage(
                    "No grouping column included. Aggregate complete table.");
        }
        // be compatible to versions prior KNIME 2.0
        compCheckColumnAggregators(groupByCols, origSpec);

        // we have to explicitly set all not group columns in the
        // exclude list of the SettingsModelFilterString.
        // The DialogComponentColumnFilter component always uses the exclude
        // list to update the component if we don't set the exclude list
        // all columns are added as group by columns.
        final Collection<String> exclList = getExcludeList(origSpec,
                groupByCols);
        m_groupByCols.setExcludeList(exclList);

        // generate group-by spec given the original spec and selected columns
        final DataTableSpec groupBySpec =
            createGroupBySpec(origSpec, groupByCols);
        return new DataTableSpec[] {groupBySpec};
    }

    /**
     * Generate table spec based on the input spec and the selected columns
     * for grouping.
     * @param origSpec original input spec
     * @param groupByCols group-by columns
     * @return a new table spec containing the group-by and aggregation columns
     * @throws InvalidSettingsException if the group-by can't by generated due
     *         to invalid settings
     */
    protected final DataTableSpec createGroupBySpec(
            final DataTableSpec origSpec, final List<String> groupByCols)
            throws InvalidSettingsException {
        // remove all column aggregator
        m_columnAggregators2Use = new ArrayList<ColumnAggregator>(
                m_columnAggregators.size());
        final ArrayList<ColumnAggregator> invalidColAggrs =
            new ArrayList<ColumnAggregator>(1);
        for (final ColumnAggregator colAggr : m_columnAggregators) {
            final DataColumnSpec colSpec = origSpec.getColumnSpec(colAggr
                    .getOriginalColName());
            if (colSpec != null && colSpec.getType().equals(
                    colAggr.getOriginalDataType())) {
                m_columnAggregators2Use.add(colAggr);
            } else {
                invalidColAggrs.add(colAggr);
            }
        }
        if (!invalidColAggrs.isEmpty()) {
            setWarningMessage("Invalid aggregation columns removed.");
        }
        if (m_columnAggregators2Use.isEmpty()) {
            setWarningMessage("No aggregation column defined");
        }
        // check for invalid group columns
        try {
            GroupByTable.checkGroupCols(origSpec, groupByCols);
        } catch (final IllegalArgumentException e) {
            throw new InvalidSettingsException(e.getMessage());
        }
        if (origSpec.getNumColumns() > 1
                && groupByCols.size() == origSpec.getNumColumns()) {
            setWarningMessage("All columns selected as group by column");
        }
        final ColumnNamePolicy colNamePolicy = ColumnNamePolicy
                .getPolicy4Label(m_columnNamePolicy.getStringValue());

        // Check if no column at all has been selected as in the validate
        // settings method. This check has to be after the explicit setting
        // of the group columns above!!!
        if (groupByCols.isEmpty() && m_columnAggregators2Use.isEmpty()) {
            throw new InvalidSettingsException(
                    "Please select at least one group or aggregation column");
        }
        return GroupByTable.createGroupByTableSpec(
                origSpec, groupByCols,
                m_columnAggregators2Use.toArray(new ColumnAggregator[0]),
                colNamePolicy);
    }

    private static Collection<String> getExcludeList(
            final DataTableSpec origSpec, final List<String> groupByCols) {
        final Collection<String> exlList = new LinkedList<String>();
        for (final DataColumnSpec spec : origSpec) {
            if (groupByCols == null || groupByCols.isEmpty()
                    || !groupByCols.contains(spec.getName())) {
                exlList.add(spec.getName());
            }
        }
        return exlList;
    }

    /**
     * Returns list of columns selected for group-by operation.
     * @return group-by columns
     */
    protected final List<String> getGroupByColumns() {
        return m_groupByCols.getIncludeList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        if (inData == null || inData[0] == null) {
            throw new Exception("No data table available!");
        }
        // create the data object
        final BufferedDataTable table = (BufferedDataTable) inData[0];
        if (table == null) {
            throw new IllegalArgumentException("No input table found");
        }
        if (table.getRowCount() < 1) {
            setWarningMessage("Empty input table found");
        }

        final List<String> groupByCols = m_groupByCols.getIncludeList();

        // be compatible to versions prior KNIME 2.0
        compCheckColumnAggregators(groupByCols, table.getDataTableSpec());

        final GroupByTable resultTable = createGroupByTable(exec, table,
                groupByCols);
        return new BufferedDataTable[] {resultTable.getBufferedTable()};
    }

    /**
     * Create group-by table.
     * @param exec execution context
     * @param table input table to group
     * @param groupByCols column selected for group-by operation
     * @return table with group and aggregation columns
     * @throws CanceledExecutionException if the group-by table generation was
     *         canceled externally
     */
    protected final GroupByTable createGroupByTable(final ExecutionContext exec,
            final BufferedDataTable table, final List<String> groupByCols)
            throws CanceledExecutionException {
        final boolean inMemory = m_inMemory.getBooleanValue();
        final boolean retainOrder = m_retainOrder.getBooleanValue();
        return createGroupByTable(exec, table, groupByCols, inMemory,
                retainOrder, m_columnAggregators2Use);
    }

    /**
     * Create group-by table.
     * @param exec execution context
     * @param table input table to group
     * @param groupByCols column selected for group-by operation
     * @param inMemory keep data in memory
     * @param retainOrder reconstructs original data order
     * @param aggregators column aggregation to use
     * @return table with group and aggregation columns
     * @throws CanceledExecutionException if the group-by table generation was
     *         canceled externally
     * @since 2.6
     */
    protected final GroupByTable createGroupByTable(final ExecutionContext exec,
            final BufferedDataTable table, final List<String> groupByCols,
            final boolean inMemory, final boolean retainOrder,
            final List<ColumnAggregator> aggregators)
            throws CanceledExecutionException {
        return createGroupByTable(exec, table, groupByCols, inMemory, false,
                retainOrder, aggregators);
    }

    /**
     * Create group-by table.
     * @param exec execution context
     * @param table input table to group
     * @param groupByCols column selected for group-by operation
     * @param inMemory keep data in memory
     * @param sortInMemory does sorting in memory
     * @param retainOrder reconstructs original data order
     * @param aggregators column aggregation to use
     * @return table with group and aggregation columns
     * @throws CanceledExecutionException if the group-by table generation was
     *         canceled externally
     * @deprecated sortInMemory is no longer required
     * @see #createGroupByTable(ExecutionContext, BufferedDataTable, List,
     * boolean, boolean, List)
     */
    @Deprecated
    protected final GroupByTable createGroupByTable(final ExecutionContext exec,
            final BufferedDataTable table, final List<String> groupByCols,
            final boolean inMemory, final boolean sortInMemory,
            final boolean retainOrder, final List<ColumnAggregator> aggregators)
            throws CanceledExecutionException {
        final int maxUniqueVals = m_maxUniqueValues.getIntValue();
        final boolean enableHilite = m_enableHilite.getBooleanValue();
        final ColumnNamePolicy colNamePolicy = ColumnNamePolicy
        .getPolicy4Label(m_columnNamePolicy.getStringValue());
        final GlobalSettings globalSettings =
            createGlobalSettings(exec, table, groupByCols, maxUniqueVals);

        //reset all aggregators in order to use enforce operator creation
        for (final ColumnAggregator colAggr : aggregators) {
            colAggr.reset();
        }
        final GroupByTable resultTable;
        if (inMemory || groupByCols.isEmpty()) {
            resultTable = new MemoryGroupByTable(exec, table, groupByCols,
                    aggregators.toArray(new ColumnAggregator[0]),
                    globalSettings, sortInMemory, enableHilite, colNamePolicy,
                    retainOrder);
        } else {
            resultTable = new BigGroupByTable(exec, table, groupByCols,
                    aggregators.toArray(new ColumnAggregator[0]),
                    globalSettings, sortInMemory, enableHilite, colNamePolicy,
                    retainOrder);
        }
        if (m_enableHilite.getBooleanValue()) {
            setHiliteMapping(new DefaultHiLiteMapper(
                    resultTable.getHiliteMapping()));
        }
        // check for skipped columns
        final String warningMsg = resultTable.getSkippedGroupsMessage(3, 3);
        if (warningMsg != null) {
            setWarningMessage(warningMsg);
            LOGGER.info(resultTable.getSkippedGroupsMessage(
                        Integer.MAX_VALUE, Integer.MAX_VALUE));
        }
        return resultTable;
    }

    /**
     * Creates the {@link GlobalSettings} object that is passed to all
     * {@link AggregationMethod}s.
     * @param exec the {@link ExecutionContext}
     * @param table the {@link BufferedDataTable}
     * @param groupByCols the names of the columns to group by
     * @param maxUniqueVals the maximum number of unique values per group
     * @return the {@link GlobalSettings} object to use
     * @since 2.6
     */
    protected GlobalSettings createGlobalSettings(final ExecutionContext exec,
            final BufferedDataTable table, final List<String> groupByCols,
            final int maxUniqueVals) {
        return new GlobalSettings(groupByCols,
                maxUniqueVals, m_valueDelimiter.getStringValue(),
                table.getDataTableSpec(), table.getRowCount());
    }

    /**
     * @return <code>true</code> if the row order should be retained
     */
    protected boolean isRetainOrder() {
        return m_retainOrder.getBooleanValue();
    }

    /**
     * @return <code>true</code> if all operations should be processed in
     * memory
     */
    protected boolean isProcessInMemory() {
        return m_inMemory.getBooleanValue();
    }

    /**
     * @return <code>true</code> if any sorting should be performed in memory
     * @deprecated sort in memory is no longer required
     */
    @Deprecated
    protected boolean isSortInMemory() {
        return false;
    }

    /**
     * @return list of column aggregator methods
     */
    protected List<ColumnAggregator> getColumnAggregators() {
        return Collections.unmodifiableList(m_columnAggregators2Use);
    }

    /**
     * @return column name policy used to create resulting pivot columns
     */
    protected ColumnNamePolicy getColumnNamePolicy() {
        return ColumnNamePolicy.getPolicy4Label(
                m_columnNamePolicy.getStringValue());
    }

//**************************************************************************
// COMPATIBILITY METHODS
// **************************************************************************

    /**
     * Compatibility method used for compatibility to versions prior Knime 2.0.
     * Helper method to get the {@link ColumnNamePolicy} for the old node
     * settings.
     *
     * @param settings
     *            the settings to read the old column name policy from
     * @return the {@link ColumnNamePolicy} equivalent to the old setting
     */
    protected static ColumnNamePolicy compGetColumnNamePolicy(
            final NodeSettingsRO settings) {
        try {
            if (settings.getBoolean(OLD_CFG_KEEP_COLUMN_NAME)) {
                return ColumnNamePolicy.KEEP_ORIGINAL_NAME;
            }
            return ColumnNamePolicy.AGGREGATION_METHOD_COLUMN_NAME;
        } catch (final InvalidSettingsException ise) {
            // this is an even older version with no keep column name option
            return ColumnNamePolicy.AGGREGATION_METHOD_COLUMN_NAME;
        }
    }

    /**
     * Compatibility method used for compatibility to versions prior Knime 2.0.
     * Helper method to get the aggregation methods for the old node settings.
     *
     * @param spec
     *            the input {@link DataTableSpec}
     * @param excludeCols
     *            the columns that should be excluded from the aggregation
     *            columns
     * @param config
     *            the config object to read from
     * @return the {@link ColumnAggregator}s
     */
    protected static List<ColumnAggregator> compGetColumnMethods(
            final DataTableSpec spec, final List<String> excludeCols,
            final ConfigRO config) {
        String numeric = null;
        String nominal = null;
        try {
            numeric = config.getString(OLD_CFG_NUMERIC_COL_METHOD);
            nominal = config.getString(OLD_CFG_NOMINAL_COL_METHOD);
        } catch (final InvalidSettingsException e) {
            numeric = AggregationMethods.getDefaultNumericalMethod().getId();
            nominal = AggregationMethods.getDefaultNotNumericalMethod().getId();
        }
        return compCreateColumnAggregators(spec, excludeCols, numeric, nominal);
    }

    /**
     * Compatibility method used for compatibility to versions prior Knime 2.0.
     * In the old version the user had defined one method for all nominal and
     * one for all numerical columns.
     *
     * @param groupByCols
     *            the names of the group by columns
     * @param spec
     *            the input {@link DataTableSpec}
     */
    private void compCheckColumnAggregators(final List<String> groupByCols,
            final DataTableSpec spec) {
        if (m_columnAggregators.isEmpty() && m_oldNumerical != null
                && m_oldNominal != null) {
            m_columnAggregators.addAll(compCreateColumnAggregators(spec,
                    groupByCols, m_oldNumerical, m_oldNominal));
        }
    }

    /**
     * Compatibility method used for compatibility to versions prior Knime 2.0.
     * Method to get the aggregation methods for the versions with only one
     * method for numerical and one for nominal columns.
     *
     * @param spec
     *            the {@link DataTableSpec}
     * @param excludeCols
     *            the name of all columns to be excluded
     * @param numeric
     *            the name of the numerical aggregation method
     * @param nominal
     *            the name of the nominal aggregation method
     * @return {@link Collection} of the {@link ColumnAggregator}s
     */
    private static List<ColumnAggregator> compCreateColumnAggregators(
            final DataTableSpec spec, final List<String> excludeCols,
            final String numeric, final String nominal) {
        final AggregationMethod numericMethod = AggregationMethods
                .getMethod4Id(numeric);
        final AggregationMethod nominalMethod = AggregationMethods
                .getMethod4Id(nominal);
        final Set<String> groupCols = new HashSet<String>(excludeCols);
        final List<ColumnAggregator> colAg = new LinkedList<ColumnAggregator>();
        for (int colIdx = 0, length = spec.getNumColumns(); colIdx < length;
            colIdx++) {
            final DataColumnSpec colSpec = spec.getColumnSpec(colIdx);
            if (!groupCols.contains(colSpec.getName())) {
                final AggregationMethod method = AggregationMethods
                        .getAggregationMethod(colSpec, numericMethod,
                                nominalMethod);
                colAg.add(new ColumnAggregator(colSpec, method, method
                        .inclMissingCells()));
            }
        }
        return colAg;
    }
}
