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
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
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

    /**Configuration key of the selected group by columns.*/
    protected static final String CFG_GROUP_BY_COLUMNS = "grouByColumns";

    /**Configuration key of the selected aggregation method for
     * numerical columns.*/
    protected static final String CFG_NUMERIC_COL_METHOD =
        "numericColumnMethod";

    /**Configuration key of the selected aggregation method for
     * none numerical columns.*/
    protected static final String CFG_NOMINAL_COL_METHOD =
        "nominalColumnMethod";

    /**Configuration key for the maximum none numerical values.*/
    protected static final String CFG_MAX_UNIQUE_VALUES =
        "maxNoneNumericalVals";

    /**Configuration key for the enable hilite option.*/
    protected static final String CFG_ENABLE_HILITE = "enableHilite";

    /**Configuration key for the sort in memory option.*/
    protected static final String CFG_SORT_IN_MEMORY = "sortInMemory";

    /**Configuration key for the move the group by columns to front option.*/
    protected static final String CFG_MOVE_GROUP_BY_COLS_2_FRONT =
        "moveGroupByCols2Front";

    /**Configuration key for the keep original column name option.*/
    protected static final String CFG_KEEP_COLUMN_NAME =
        "keepColumnName";



    private final SettingsModelFilterString m_groupByCols =
        new SettingsModelFilterString(CFG_GROUP_BY_COLUMNS);

    private final SettingsModelString m_numericColMethod =
        new SettingsModelString(CFG_NUMERIC_COL_METHOD,
                AggregationMethod.getDefaultNumericMethod().getLabel());

    private final SettingsModelString m_nominalColMethod =
        new SettingsModelString(CFG_NOMINAL_COL_METHOD,
                AggregationMethod.getDefaultNominalMethod().getLabel());

    private final SettingsModelIntegerBounded m_maxUniqueValues =
        new SettingsModelIntegerBounded(CFG_MAX_UNIQUE_VALUES, 10000, 1,
                Integer.MAX_VALUE);

    private final SettingsModelBoolean m_enableHilite =
        new SettingsModelBoolean(CFG_ENABLE_HILITE, false);

    private final SettingsModelBoolean m_sortInMemory =
        new SettingsModelBoolean(CFG_SORT_IN_MEMORY, false);

    private final SettingsModelBoolean m_moveGroupCols2Front =
        new SettingsModelBoolean(CFG_MOVE_GROUP_BY_COLS_2_FRONT, false);

    private final SettingsModelBoolean m_keepColumnName =
        new SettingsModelBoolean(CFG_KEEP_COLUMN_NAME, false);

    /**
     * Node returns a new hilite handler instance.
     */

    /**
     * Node returns a new hilite handler instance.
     */
    private final HiLiteTranslator m_hilite = new HiLiteTranslator(
            new DefaultHiLiteHandler());

    /**
     * Creates a new group by model with one in- and outport.
     */
    public GroupByNodeModel() {
        super(1, 1);
        m_nominalColMethod.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_maxUniqueValues.setEnabled(
                        GroupByNodeDialogPane.enableUniqueValuesModel(
                                m_numericColMethod, m_nominalColMethod));
            }
        });
        m_numericColMethod.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_maxUniqueValues.setEnabled(
                        GroupByNodeDialogPane.enableUniqueValuesModel(
                                m_numericColMethod, m_nominalColMethod));
            }
        });
        m_maxUniqueValues.setEnabled(
                GroupByNodeDialogPane.enableUniqueValuesModel(
                m_numericColMethod, m_nominalColMethod));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        if (m_enableHilite.getBooleanValue()) {
            final NodeSettingsRO config = NodeSettings.loadFromXML(
                    new FileInputStream(new File("hilite_mapping.xml.gz")));
            try {
                m_hilite.setMapper(DefaultHiLiteMapper.load(config));
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
                    new FileOutputStream(new File("hilite_mapping.xml.gz")));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_groupByCols.saveSettingsTo(settings);
        m_numericColMethod.saveSettingsTo(settings);
        m_nominalColMethod.saveSettingsTo(settings);
        m_maxUniqueValues.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
        m_sortInMemory.saveSettingsTo(settings);
        m_moveGroupCols2Front.saveSettingsTo(settings);
        m_keepColumnName.saveSettingsTo(settings);
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
        m_numericColMethod.validateSettings(settings);
        m_nominalColMethod.validateSettings(settings);
        m_maxUniqueValues.validateSettings(settings);
        m_enableHilite.validateSettings(settings);
        m_sortInMemory.validateSettings(settings);
        m_moveGroupCols2Front.validateSettings(settings);
        try {
            m_keepColumnName.validateSettings(settings);
        } catch (final InvalidSettingsException e) {
            //be compatible to previous versions
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       m_groupByCols.loadSettingsFrom(settings);
       m_numericColMethod.loadSettingsFrom(settings);
       m_nominalColMethod.loadSettingsFrom(settings);
       m_maxUniqueValues.loadSettingsFrom(settings);
       m_enableHilite.loadSettingsFrom(settings);
       m_sortInMemory.loadSettingsFrom(settings);
       m_moveGroupCols2Front.loadSettingsFrom(settings);
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
        m_hilite.getFromHiLiteHandler().fireClearHiLiteEvent();
        m_hilite.setMapper(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        m_hilite.removeAllToHiliteHandlers();
        if (hiLiteHdl != null) {
            m_hilite.addToHiLiteHandler(hiLiteHdl);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        assert outIndex == 0;
        return m_hilite.getFromHiLiteHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        final List<String> groupByCols = m_groupByCols.getIncludeList();
        final DataTableSpec origSpec = inSpecs[0];
        try {
            GroupByTable.checkIncludeList(origSpec, groupByCols);
        } catch (final IllegalArgumentException e) {
            throw new InvalidSettingsException(
                    "Please define the group by column(s)");
        }
        if (origSpec.getNumColumns() > 1
                && groupByCols.size() == origSpec.getNumColumns()) {
            setWarningMessage("All columns selected as group by column");
        }
        if (origSpec.getNumColumns() < 1) {
            setWarningMessage(
                    "Input table should contain at least one column");
        }
        final AggregationMethod numericMethod =
            AggregationMethod.getMethod4SettingsModel(m_numericColMethod);
        final AggregationMethod nominalMethod =
            AggregationMethod.getMethod4SettingsModel(m_nominalColMethod);
        final DataTableSpec spec = GroupByTable.createGroupByTableSpec(
                origSpec, groupByCols, numericMethod, nominalMethod,
                m_moveGroupCols2Front.getBooleanValue(),
                m_keepColumnName.getBooleanValue());
        return new DataTableSpec[] {spec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable table = inData[0];
        if (table == null) {
            throw new IllegalArgumentException("NO input table found");
        }
        if (table.getRowCount() < 1) {
            setWarningMessage("Empty input table found");
        }
        final List<String> groupByCols = m_groupByCols.getIncludeList();
        final AggregationMethod numericMethod =
            AggregationMethod.getMethod4SettingsModel(m_numericColMethod);
        final AggregationMethod noneNumericMethod =
            AggregationMethod.getMethod4SettingsModel(m_nominalColMethod);
        final int maxUniqueVals = m_maxUniqueValues.getIntValue();
        final boolean sortInMemory = m_sortInMemory.getBooleanValue();
        final boolean enableHilite = m_enableHilite.getBooleanValue();
        final boolean move2Front = m_moveGroupCols2Front.getBooleanValue();
        final boolean keepColName = m_keepColumnName.getBooleanValue();
        final GroupByTable resultTable = new GroupByTable(table, groupByCols,
                numericMethod, noneNumericMethod, maxUniqueVals, sortInMemory,
                enableHilite, move2Front, keepColName, exec);
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
}
