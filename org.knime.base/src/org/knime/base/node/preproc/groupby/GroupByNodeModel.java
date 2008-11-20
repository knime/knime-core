/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *    28.06.2007 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.groupby;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.knime.base.node.preproc.groupby.aggregation.AggregationMethod;
import org.knime.base.node.preproc.groupby.aggregation.ColumnAggregator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;


/**
 * The {@link NodeModel} implementation of the group by node which uses
 * the {@link GroupByTable} class to create the resulting table.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class GroupByNodeModel extends NodeModel {

    private static final String INTERNALS_FILE_NAME = "hilite_mapping.xml.gz";

    /**Configuration key of the selected group by columns.*/
    protected static final String CFG_GROUP_BY_COLUMNS = "grouByColumns";

    /**Old configuration key of the selected aggregation method for
     * numerical columns. This was used by the previous version.*/
    public static final String OLD_CFG_NUMERIC_COL_METHOD =
        "numericColumnMethod";

    /**Old configuration key for the move the group by columns to front
     * option. This was used by the previous version.*/
    protected static final String OLD_CFG_MOVE_GROUP_BY_COLS_2_FRONT =
        "moveGroupByCols2Front";

    /**Old configuration key of the selected aggregation method for
     * none numerical columns. This was used by the previous version.*/
    public static final String OLD_CFG_NOMINAL_COL_METHOD =
        "nominalColumnMethod";

    /**Configuration key for the maximum none numerical values.*/
    protected static final String CFG_MAX_UNIQUE_VALUES =
        "maxNoneNumericalVals";

    /**Configuration key for the enable hilite option.*/
    protected static final String CFG_ENABLE_HILITE = "enableHilite";

    /**Configuration key for the sort in memory option.*/
    protected static final String CFG_SORT_IN_MEMORY = "sortInMemory";

    /**Configuration key for the keep original column name option.*/
    protected static final String CFG_KEEP_COLUMN_NAME =
        "keepColumnName";



    private final SettingsModelFilterString m_groupByCols =
        new SettingsModelFilterString(CFG_GROUP_BY_COLUMNS);

//    private final SettingsModelString m_numericColMethod =
//        new SettingsModelString(CFG_NUMERIC_COL_METHOD,
//                AggregationMethod.getDefaultNumericMethod().getLabel());
//
//    private final SettingsModelString m_nominalColMethod =
//        new SettingsModelString(CFG_NOMINAL_COL_METHOD,
//                AggregationMethod.getDefaultNominalMethod().getLabel());

    private final SettingsModelIntegerBounded m_maxUniqueValues =
        new SettingsModelIntegerBounded(CFG_MAX_UNIQUE_VALUES, 10000, 1,
                Integer.MAX_VALUE);

    private final SettingsModelBoolean m_enableHilite =
        new SettingsModelBoolean(CFG_ENABLE_HILITE, false);

    private final SettingsModelBoolean m_sortInMemory =
        new SettingsModelBoolean(CFG_SORT_IN_MEMORY, false);

//    private final SettingsModelBoolean m_moveGroupCols2Front =
//        new SettingsModelBoolean(CFG_MOVE_GROUP_BY_COLS_2_FRONT, false);

    private final SettingsModelBoolean m_keepColumnName =
        new SettingsModelBoolean(CFG_KEEP_COLUMN_NAME, false);

    private final List<ColumnAggregator> m_columnAggregators =
        new LinkedList<ColumnAggregator>();

    /**This variable holds the label of the nominal method which was used
     * in the previous implementation.*/
    private String m_oldNominal = null;
    /**This variable holds the label of the numerical method which was used
     * in the previous implementation.*/
    private String m_oldNumerical = null;

    /**
     * Node returns a new hilite handler instance.
     */
    private final HiLiteTranslator m_hilite = new HiLiteTranslator();

    /**
     * Creates a new group by model with one in- and outport.
     */
    public GroupByNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
//        m_nominalColMethod.addChangeListener(new ChangeListener() {
//            public void stateChanged(final ChangeEvent e) {
//                m_maxUniqueValues.setEnabled(
//                        GroupByNodeDialogPane.enableUniqueValuesModel(
//                                m_numericColMethod, m_nominalColMethod));
//            }
//        });
//        m_numericColMethod.addChangeListener(new ChangeListener() {
//            public void stateChanged(final ChangeEvent e) {
//                m_maxUniqueValues.setEnabled(
//                        GroupByNodeDialogPane.enableUniqueValuesModel(
//                                m_numericColMethod, m_nominalColMethod));
//            }
//        });
//        m_maxUniqueValues.setEnabled(
//                GroupByNodeDialogPane.enableUniqueValuesModel(
//                m_numericColMethod, m_nominalColMethod));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettingsRO config = NodeSettings.loadFromXML(
                    new FileInputStream(new File(nodeInternDir, 
                            INTERNALS_FILE_NAME)));
            try {
                m_hilite.setMapper(DefaultHiLiteMapper.load(config));
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
            final DefaultHiLiteMapper mapper =
                (DefaultHiLiteMapper) m_hilite.getMapper();
            if (mapper != null) {
                mapper.save(config);
            }
            config.saveToXML(
                    new FileOutputStream(new File(nodeInternDir,
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
        m_sortInMemory.saveSettingsTo(settings);
        m_keepColumnName.saveSettingsTo(settings);
        if (m_columnAggregators.isEmpty()
                && m_oldNominal != null && m_oldNumerical != null) {
            settings.addString(OLD_CFG_NUMERIC_COL_METHOD, m_oldNumerical);
            settings.addString(OLD_CFG_NOMINAL_COL_METHOD, m_oldNominal);
        } else {
            ColumnAggregator.saveColumnAggregators(settings,
                    m_columnAggregators);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_groupByCols.validateSettings(settings);
        final List<String> groupByCols =
            ((SettingsModelFilterString)m_groupByCols.
                    createCloneWithValidatedValue(settings)).getIncludeList();
        if (groupByCols == null || groupByCols.size() < 1) {
            throw new InvalidSettingsException("No grouping column included");
        }
//        m_numericColMethod.validateSettings(settings);
//        m_nominalColMethod.validateSettings(settings);
//        m_moveGroupCols2Front.validateSettings(settings);
        m_maxUniqueValues.validateSettings(settings);
        m_enableHilite.validateSettings(settings);
        m_sortInMemory.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       final boolean move2Front =
           settings.getBoolean(OLD_CFG_MOVE_GROUP_BY_COLS_2_FRONT, true);
       if (!move2Front) {
           setWarningMessage(
                   "Setting ignored: Group columns will be moved to front");
       }
       m_groupByCols.loadSettingsFrom(settings);
       m_columnAggregators.clear();
       try {
           m_columnAggregators.addAll(
               ColumnAggregator.loadColumnAggregators(settings));
           m_oldNumerical = null;
           m_oldNominal = null;
       } catch (final InvalidSettingsException e) {
            // be compatible to previous version
           m_oldNumerical = settings.getString(OLD_CFG_NUMERIC_COL_METHOD);
           m_oldNominal = settings.getString(OLD_CFG_NOMINAL_COL_METHOD);
        }
//       m_numericColMethod.loadSettingsFrom(settings);
//       m_nominalColMethod.loadSettingsFrom(settings);
//       m_moveGroupCols2Front.loadSettingsFrom(settings);
       m_maxUniqueValues.loadSettingsFrom(settings);
       m_enableHilite.loadSettingsFrom(settings);
       m_sortInMemory.loadSettingsFrom(settings);
       try {
           m_keepColumnName.loadSettingsFrom(settings);
       } catch (final InvalidSettingsException e) {
           //be compatible to previous versions
           m_keepColumnName.setBooleanValue(false);
       }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_hilite.setMapper(null);
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
        final DataTableSpec origSpec = (DataTableSpec)inSpecs[0];
        if (origSpec.getNumColumns() < 1) {
            setWarningMessage(
                    "Input table should contain at least one column");
        }

        final List<String> groupByCols = m_groupByCols.getIncludeList();
        //be compatible to the previous version
        checkColumnAggregators(groupByCols, origSpec);
        //remove all invalid column aggregators
        final List<ColumnAggregator> invalidColAggrs =
            new LinkedList<ColumnAggregator>();
        for (final ColumnAggregator colAggr : m_columnAggregators) {
            if (!origSpec.containsName(colAggr.getColName())) {
                invalidColAggrs.add(colAggr);
            }
        }
        if (!invalidColAggrs.isEmpty()) {
            m_columnAggregators.removeAll(invalidColAggrs);
        }

        if (m_columnAggregators.isEmpty()) {
            setWarningMessage("No aggregation column defined");
        }

        //we have to explicit set the all not group columns in the
        //exclude list of the SettingsModelFilterString
        final Collection<String> exclList =
            getExcludeList(origSpec, groupByCols);
        m_groupByCols.setExcludeList(exclList);
        //remove all invalid group columns
        try {
            GroupByTable.checkGroupCols(origSpec, groupByCols);
        } catch (final IllegalArgumentException e) {
            throw new InvalidSettingsException(
                    "Please define the group by column(s)");
        }
        if (origSpec.getNumColumns() > 1
                && groupByCols.size() == origSpec.getNumColumns()) {
            setWarningMessage("All columns selected as group by column");
        }
        final DataTableSpec spec = GroupByTable.createGroupByTableSpec(
                origSpec, groupByCols,
                m_columnAggregators.toArray(new ColumnAggregator[0]),
                m_keepColumnName.getBooleanValue());

        return new DataTableSpec[] {spec};
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
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
    final ExecutionContext exec) throws Exception {
        if (inData == null || inData[0] == null) {
            throw new Exception("No data table available!");
        }
        // create the data object
        final BufferedDataTable table = (BufferedDataTable)inData[0];
        if (table == null) {
            throw new IllegalArgumentException("NO input table found");
        }
        if (table.getRowCount() < 1) {
            setWarningMessage("Empty input table found");
        }

        final List<String> groupByCols = m_groupByCols.getIncludeList();
        final int maxUniqueVals = m_maxUniqueValues.getIntValue();
        final boolean sortInMemory = m_sortInMemory.getBooleanValue();
        final boolean enableHilite = m_enableHilite.getBooleanValue();
//        final boolean move2Front = m_moveGroupCols2Front.getBooleanValue();
        final boolean keepColName = m_keepColumnName.getBooleanValue();
        //be compatible to the previous version
        checkColumnAggregators(groupByCols, table.getDataTableSpec());

        final GroupByTable resultTable = new GroupByTable(exec, table,
                groupByCols,
                m_columnAggregators.toArray(new ColumnAggregator[0]),
                maxUniqueVals, sortInMemory, enableHilite, keepColName);
        if (m_enableHilite.getBooleanValue()) {
            m_hilite.setMapper(new DefaultHiLiteMapper(
                    resultTable.getHiliteMapping()));
        }
        //check for skipped columns
        final String warningMsg = resultTable.getSkippedGroupsMessage(3, 3);
        if (warningMsg != null) {
            setWarningMessage(warningMsg);
        }
        return new BufferedDataTable[]{resultTable.getBufferedTable()};
    }

    /**
     * This method is used for compatibility to old versions. In the old version
     * the user had defined one method for all nominal and one for all numerical
     * columns.
     *
     * @param groupByCols the names of the group by columns
     * @param spec the input {@link DataTableSpec}
     */
    private void checkColumnAggregators(final List<String> groupByCols,
            final DataTableSpec spec) {
        if (m_columnAggregators.isEmpty() && m_oldNumerical != null
                && m_oldNominal != null) {
            m_columnAggregators.addAll(createColumnAggregators(spec,
                    groupByCols, m_oldNumerical, m_oldNominal));
        }
    }

    /**
     * Helper method to get the aggregation methods for the previous version
     * with only one method for numerical and one for nominal columns.
     * @param spec the {@link DataTableSpec}
     * @param excludeCols the name of all columns to be excluded
     * @param numeric the name of the numerical aggregation method
     * @param nominal the name of the nominal aggregation method
     * @return {@link Collection} of the {@link ColumnAggregator}s
     */
    public static List<ColumnAggregator> createColumnAggregators(
            final DataTableSpec spec, final List<String> excludeCols,
            final String numeric, final String nominal) {
        final AggregationMethod numericMethod =
            AggregationMethod.getMethod4Label(numeric);
        final AggregationMethod nominalMethod =
            AggregationMethod.getMethod4Label(nominal);
        final Set<String> groupCols = new HashSet<String>(excludeCols);
        final List<ColumnAggregator> colAg =
            new LinkedList<ColumnAggregator>();
        for (int colIdx = 0, length = spec.getNumColumns();
            colIdx < length; colIdx++) {
            final DataColumnSpec colSpec = spec.getColumnSpec(colIdx);
            if (!groupCols.contains(colSpec.getName())) {
                final AggregationMethod method =
                    AggregationMethod.getAggregationMethod(colSpec,
                            numericMethod, nominalMethod);
                colAg.add(new ColumnAggregator(colSpec, method));
            }
        }
        return colAg;
    }
}
