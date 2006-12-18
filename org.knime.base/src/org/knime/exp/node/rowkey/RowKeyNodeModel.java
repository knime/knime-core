/*
 * ------------------------------------------------------------------- 
 * This source code, its documentation and all appendant files are protected by
 * copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006 University of Konstanz, Germany. Chair for
 * Bioinformatics and Information Mining Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce, create
 * derivative works from, distribute, perform, display, or in any way exploit
 * any of the content, in whole or in part, except as otherwise expressly
 * permitted in writing by the copyright owner or as specified in the license
 * file distributed with this product.
 * 
 * If you have any questions please contact the copyright holder: website:
 * www.knime.org email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History 05.11.2006 (Tobias Koetter): created
 */
package org.knime.exp.node.rowkey;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The node model of the row key manipulation node. The node allows the user
 * to replace the row key with another column and/or to append a new column 
 * with the values of the current row key. 
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class RowKeyNodeModel extends NodeModel {

    // our logger instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RowKeyNodeModel.class);

    /** The port were the model expects the in data. */
    public static final int DATA_IN_PORT = 0;

    /** The port which the model uses to return the data. */
    public static final int DATA_OUT_PORT = 0;

    /**
     * The name of the settings tag which holds the boolean if the user
     * wants to replace the row key with values of the selected column.*/
    public static final String REPLACE_ROWKEY = "replaceRowKey";

    /**
     * The name of the settings tag which holds the boolean if the user
     * wants to have the uniqueness ensured.*/
    public static final String ENSURE_UNIQUNESS = "ensureUniqueness";
    
    /**
     * The name of the settings tag which holds the boolean if the user
     * wants to replace missing values.*/
    public static final String REPLACE_MISSING_VALS = "replaceMissingValues";
    
    /**
     * The name of the settings tag which holds the name of the column which
     * values should be used as new row keys for the result table the user has
     * selected as <code>String</code>. Could be <code>empty</code> if the
     * user only wants create a new column with the values of the current row
     * key.
     */
    public static final String SELECTED_NEW_ROWKEY_COL = "newRowKeyColumnName";

    /**
     * The name of the settings tag which holds the boolean if the user wants
     * to have the row key values in a new column or not.*/
    public static final String APPEND_ROWKEY_COLUMN = "appendRowKeyCol";

    /**
     * The name of the settings tag which holds the name of new column which
     * contains the row keys as values the user has entered in the dialog as
     * <code>String</code>. Could be empty if the user only wants to replace
     * the existing rowkey column with another column.
     */
    public static final String NEW_COL_NAME_4_ROWKEY_VALS = 
        "newColumnName4RowKeyValues";

    /** Holds the <code>DataTableSpec</code> of the input data port. */
    private DataTableSpec m_tableSpec;

    /**If<code>true</code> the user wants to replace the existing row key
     * with the values of the selected column.*/
    private final SettingsModelBoolean m_replaceRowKey = 
        new SettingsModelBoolean(REPLACE_ROWKEY, true);

    /**If<code>true</code> the user wants to replace the existing row key
     * with the values of the selected column.*/
    private final SettingsModelBoolean m_ensureUniqueness = 
        new SettingsModelBoolean(ENSURE_UNIQUNESS, false);
    
    /**If<code>true</code> the user wants to replace the existing row key
     * with the values of the selected column.*/
    private final SettingsModelBoolean m_replaceMissingVals = 
        new SettingsModelBoolean(REPLACE_MISSING_VALS, false);
    
    /**The name of the column with the new row key values. Could be
     * <code>null</code>.*/
    private final SettingsModelString m_selectedNewRowKeyColName = 
        new SettingsModelString(SELECTED_NEW_ROWKEY_COL, (String)null);

    /**If <code>true</code> the user wants the values of the row key copied
     * to a new column with the given name.*/
    private final SettingsModelBoolean m_appendRowKeyCol = 
        new SettingsModelBoolean(APPEND_ROWKEY_COLUMN, false);

    /**The name of the new column which should contain the row key values. Could
     * be <code>null</code>.*/
    private final SettingsModelString m_newColumnName = 
        new SettingsModelString(NEW_COL_NAME_4_ROWKEY_VALS, (String)null);

    /**
     * The output HiLite handler.
     */
    private final DefaultHiLiteHandler m_hiLiteHandler;

    /**
     * Constructor for class RowKeyNodeModel.
     */
    protected RowKeyNodeModel() {
        // we have one data in and one data out port
        super(1, 1);
        m_hiLiteHandler = new DefaultHiLiteHandler();
    }

    /**
     * @see org.knime.core.node.NodeModel #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        LOGGER.debug("Entering execute(inData, exec) of class RowKeyNodeModel.");
        // check input data
        if (inData == null || inData.length != 1
                || inData[DATA_IN_PORT] == null) {
            throw new IllegalArgumentException("No input data available.");
        }
        final BufferedDataTable data = inData[DATA_IN_PORT];
        BufferedDataTable outData = null;
        if (m_replaceRowKey.getBooleanValue()) {
            LOGGER.debug("The user wants to replace the row ID with the"
                    + " column " + m_selectedNewRowKeyColName.getStringValue()
                    + " optional appended column name" + m_newColumnName);
            // the user wants a new column as rowkey column
            final int colIdx = data.getDataTableSpec().findColumnIndex(
                    m_selectedNewRowKeyColName.getStringValue());
            if (colIdx < 0) {
                throw new InvalidSettingsException("No column with name: "
                        + m_selectedNewRowKeyColName.getStringValue() 
                        + " exists. Please select a valid column name.");
            }
            
            DataColumnSpec newColSpec = null;
            if (m_appendRowKeyCol.getBooleanValue()) {
                final String newColName = m_newColumnName.getStringValue();
                final DataType newColType = getCommonSuperType4RowKey(data);
                if (newColName == null || newColName.length() < 1) {
                    throw new InvalidSettingsException("Please provide a valid"
                            + " name for the new column.");
                }
                if (newColType == null) {
                    throw new InvalidSettingsException(
                            "Internal exception: The DataType of the new "
                            + "column shouldn't be null.");
                }
                final DataColumnSpecCreator colSpecCreater = 
                        new DataColumnSpecCreator(newColName, newColType);
                newColSpec = colSpecCreater.createSpec();
            }
            final RowKeyUtil util = new RowKeyUtil();
            outData = util.changeRowKey(data, exec,
                    m_selectedNewRowKeyColName.getStringValue(),
                    m_appendRowKeyCol.getBooleanValue(), newColSpec,
                    m_ensureUniqueness.getBooleanValue(), 
                    m_replaceMissingVals.getBooleanValue());
            final int missingValueCounter = util.getMissingValueCounter();
            final int duplicatesCounter = util.getDuplicatesCounter();
            final StringBuilder warningMsg = new StringBuilder();
            if (duplicatesCounter > 0) {
                warningMsg.append(duplicatesCounter 
                        + " duplicate(s) replaced. ");
            }
            if (missingValueCounter > 0) {
                warningMsg.append(missingValueCounter 
                        + " missing value(s) replaced with " 
                        + RowKeyUtil.MISSING_VALUE_REPLACEMENT + ".");
            }
            if (warningMsg.length() > 0) {
                setWarningMessage(warningMsg.toString());
            }
            LOGGER.debug("Row key replaced successfully");

        } else if (m_appendRowKeyCol.getBooleanValue()) {
            LOGGER.debug("The user only wants to append a new column with "
                    + "name " + m_newColumnName);
            // the user wants only a column with the given name which 
            //contains the rowkey as value
            final RowKeyUtil cellAppender = new RowKeyUtil();
            final DataTableSpec tableSpec = data.getDataTableSpec();
            final DataType type = getCommonSuperType4RowKey(data);
            final DataColumnSpec[] colSpecs = RowKeyUtil.getResultColSpecs(
                    tableSpec, m_newColumnName.getStringValue(), type);
            final AppendedColumnTable appTable = new AppendedColumnTable(
                    data, cellAppender, colSpecs);
            outData = exec.createBufferedDataTable(appTable, exec);
            exec.setMessage("New column created");
            LOGGER.debug("Column appended successfully");
        } else {
            //the user doesn't want to do anything at all so we simply return
            //the given data
            outData = data;
            LOGGER.debug("The user hasn't selected a new row key column"
                    + " and hasn't entered a new column name.");
        }
        LOGGER.debug("Exiting execute(inData, exec) of class RowKeyNodeModel.");
        return new BufferedDataTable[]{outData};
    }

    /**
     * @param data the {@link BufferedDataTable} which row key type should be
     * determined.
     * @return the super type of the row key column
     */
    private static DataType getCommonSuperType4RowKey(
            final BufferedDataTable data) {
        if (data == null) {
            return DataType.getType(DataCell.class);
        }
        Set<DataType> types = new HashSet<DataType>();
        for (DataRow row : data) {
             types.add(row.getKey().getId().getType());
        }
        if (types.size() < 1) {
            return DataType.getType(DataCell.class);
        } else if (types.size() == 1) {
            return types.iterator().next();
        } else {
            final Iterator<DataType> dataTypes = types.iterator();
            DataType currentSuperType = dataTypes.next();
            while (dataTypes.hasNext()) {
                final DataType dataType = dataTypes.next();
                currentSuperType = 
                    DataType.getCommonSuperType(currentSuperType, dataType);
            }
            return currentSuperType;
        }
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_hiLiteHandler.fireClearHiLiteEvent();
    }

    /**
     * @throws InvalidSettingsException 
     * @see org.knime.core.node.NodeModel
     *      #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check the input data
        assert (inSpecs != null && inSpecs.length == 1 
                && inSpecs[DATA_IN_PORT] != null);
        m_tableSpec = inSpecs[DATA_IN_PORT];
        if (m_tableSpec != null
                && m_tableSpec.containsName(m_newColumnName.getStringValue())) {
            throw new InvalidSettingsException("Column with name: '"
                    + m_newColumnName.getStringValue() + "' already exists."
                    + " Please enter a new name for the column to append.");
        }
        if (m_replaceRowKey.getBooleanValue()) {
            final String selRowKey = 
                m_selectedNewRowKeyColName.getStringValue();
            if (selRowKey == null || selRowKey.trim().length() < 1) {
                throw new InvalidSettingsException(
                        "Please select the new row ID column");
            }
            if (!m_tableSpec.containsName(selRowKey)) {
                throw new InvalidSettingsException(
                "Selected column: '" + selRowKey 
                + "' not found in input table.");
            }
        }
        DataTableSpec spec = m_tableSpec;
        if (m_appendRowKeyCol.getBooleanValue()) {
            spec = null;
            //I can't set the right type of the row key since I don't know it
            //here so we return no table spec
//            final DataType type = DataType.getType(DataCell.class);
//            final DataColumnSpecCreator colSpecCreator = 
//                new DataColumnSpecCreator(m_newColumnName.getStringValue(), 
//                        type);
//            final DataColumnSpec colSpec = colSpecCreator.createSpec();
//            spec = AppendedColumnTable.getTableSpec(inSpecs[DATA_IN_PORT], 
//                    colSpec);   
        }
        return new DataTableSpec[]{spec};
    }

    /**
     * @see org.knime.core.node.NodeModel#getOutHiLiteHandler(int)
     */
    @Override
    public HiLiteHandler getOutHiLiteHandler(final int outPortID) {
        assert (outPortID == 0);
        return m_hiLiteHandler;
    }

    /**
     * @see org.knime.core.node.NodeModel #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);
        m_replaceRowKey.saveSettingsTo(settings);
        m_ensureUniqueness.saveSettingsTo(settings);
        m_replaceMissingVals.saveSettingsTo(settings);
        m_selectedNewRowKeyColName.saveSettingsTo(settings);
        m_appendRowKeyCol.saveSettingsTo(settings);
        m_newColumnName.saveSettingsTo(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        m_replaceRowKey.loadSettingsFrom(settings);
        if (m_replaceRowKey.getBooleanValue()) {
            m_ensureUniqueness.loadSettingsFrom(settings);
            m_replaceMissingVals.loadSettingsFrom(settings);
            m_selectedNewRowKeyColName.loadSettingsFrom(settings);
        }
        m_appendRowKeyCol.loadSettingsFrom(settings);
        if (m_appendRowKeyCol.getBooleanValue()) {
            //remove space at the beginning and end
            m_newColumnName.loadSettingsFrom(settings);
        }
    }

    /**
     * @see org.knime.core.node.NodeModel #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        final SettingsModelBoolean replaceRowKeyModel = 
            m_replaceRowKey.createCloneWithValidatedValue(settings);
        final boolean replaceRowKey = replaceRowKeyModel.getBooleanValue();
        final SettingsModelBoolean appendRowKeyModel = 
            m_appendRowKeyCol.createCloneWithValidatedValue(settings);
        final boolean appendRowKeyCol = appendRowKeyModel.getBooleanValue();
        if (!(replaceRowKey || appendRowKeyCol)) {
            //the user hasn't enabled an option
            throw new InvalidSettingsException(
                    "Please select at least on option.");
        }
        
        if (replaceRowKey) {
            final SettingsModelString newRowKeyModel = 
                m_selectedNewRowKeyColName.createCloneWithValidatedValue(
                        settings);
            final String newRowKeyCol = newRowKeyModel.getStringValue();
            if (newRowKeyCol == null || newRowKeyCol.trim().length() < 1) {
                throw new InvalidSettingsException("Please select a column"
                        + " which should be used as new row ID");
            }
            if (m_tableSpec != null 
                    && !m_tableSpec.containsName(newRowKeyCol)) {
                throw new InvalidSettingsException(
                "Selected column: '" + newRowKeyCol 
                + "' not found in table specification.");
            }
        }
        if (appendRowKeyCol) {
            final SettingsModelString newColNameModel = 
                m_newColumnName.createCloneWithValidatedValue(settings);
            final String newColName = newColNameModel.getStringValue();
            if (newColName == null || newColName.trim().length() < 1) {
                throw new InvalidSettingsException(
                        "Please provide a name for the column to append.");
            }
            if (m_tableSpec != null && m_tableSpec.containsName(newColName)) {
                throw new InvalidSettingsException("Column with name: '"
                        + newColName + "' already exists."
                        + " Please enter a new name for the column to append.");
            }
        }
    }

    /**
     * @see org.knime.core.node. NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        // nothing to do since the dialog settings are stored by the framework
    }

    /**
     * @see org.knime.core.node. NodeModel#saveInternals(File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        // nothing to do since the dialog settings are stored by the framework
    }

}
