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
 * History 05.11.2006 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.rowkey;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
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

import org.knime.base.data.append.column.AppendedColumnTable;

import java.io.File;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
     * wants to have the selected new row id column removed.*/
    public static final String REMOVE_ROW_KEY_COLUM = "removeRowKeyCol";

    /**
     * The name of the settings tag which holds the boolean if the user
     * wants to have the uniqueness ensured.*/
    public static final String ENSURE_UNIQUNESS = "ensureUniqueness";

    /**
     * The name of the settings tag which holds the boolean if the user
     * wants to replace missing values.*/
    public static final String HANDLE_MISSING_VALS = "replaceMissingValues";

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

    /**If<code>true</code> the user wants to replace the existing row key
     * with the values of the selected column.*/
    private final SettingsModelBoolean m_replaceKey;
    /**The name of the column with the new row key values. Could be
     * <code>null</code>.*/
    private final SettingsModelString m_newRowKeyColumn;

    private final SettingsModelBoolean m_removeRowKeyCol;
    /**If<code>true</code> the user wants to replace the existing row key
     * with the values of the selected column.*/
    private final SettingsModelBoolean m_ensureUniqueness;
    /**If<code>true</code> the user wants to replace the existing row key
     * with the values of the selected column.*/
    private final SettingsModelBoolean m_handleMissingVals;
    /**If <code>true</code> the user wants the values of the row key copied
     * to a new column with the given name.*/
    private final SettingsModelBoolean m_appendRowKey;
    /**The name of the new column which should contain the row key values. Could
     * be <code>null</code>.*/
    private final SettingsModelString m_newColumnName;

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
        //initialise the settings models
        m_hiLiteHandler = new DefaultHiLiteHandler();
        m_replaceKey = new SettingsModelBoolean(
                RowKeyNodeModel.REPLACE_ROWKEY, true);
        m_newRowKeyColumn = new SettingsModelString(
                RowKeyNodeModel.SELECTED_NEW_ROWKEY_COL, (String)null);
        m_newRowKeyColumn.setEnabled(m_replaceKey.getBooleanValue());
        m_removeRowKeyCol = new SettingsModelBoolean(
                RowKeyNodeModel.REMOVE_ROW_KEY_COLUM, false);
        m_removeRowKeyCol.setEnabled(m_replaceKey.getBooleanValue());
        m_ensureUniqueness = new SettingsModelBoolean(
                RowKeyNodeModel.ENSURE_UNIQUNESS, false);
        m_ensureUniqueness.setEnabled(m_replaceKey.getBooleanValue());
        m_handleMissingVals = new SettingsModelBoolean(
                RowKeyNodeModel.HANDLE_MISSING_VALS, false);
        m_handleMissingVals.setEnabled(m_replaceKey.getBooleanValue());

        m_appendRowKey = new SettingsModelBoolean(
                RowKeyNodeModel.APPEND_ROWKEY_COLUMN, false);
        m_newColumnName = new SettingsModelString(
                RowKeyNodeModel.NEW_COL_NAME_4_ROWKEY_VALS, (String)null);
        m_newColumnName.setEnabled(m_appendRowKey.getBooleanValue());

        m_replaceKey.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final boolean b = m_replaceKey.getBooleanValue();
                m_newRowKeyColumn.setEnabled(b);
                m_removeRowKeyCol.setEnabled(b);
                m_ensureUniqueness.setEnabled(b);
                m_handleMissingVals.setEnabled(b);
            }
        });
        m_appendRowKey.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_newColumnName.setEnabled(m_appendRowKey.getBooleanValue());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        LOGGER.debug("Entering execute(inData, exec) of class RowKeyNodeModel");
        // check input data
        if (inData == null || inData.length != 1
                || inData[DATA_IN_PORT] == null) {
            throw new IllegalArgumentException("No input data available.");
        }
        final BufferedDataTable data = inData[DATA_IN_PORT];
        BufferedDataTable outData = null;
        if (m_replaceKey.getBooleanValue()) {
            LOGGER.debug("The user wants to replace the row ID with the"
                    + " column " + m_newColumnName.getStringValue()
                    + " optional appended column name" + m_newColumnName);
            // the user wants a new column as rowkey column
            final int colIdx = data.getDataTableSpec().findColumnIndex(
                    m_newRowKeyColumn.getStringValue());
            if (colIdx < 0) {
                throw new InvalidSettingsException("No column with name: "
                        + m_newColumnName.getStringValue()
                        + " exists. Please select a valid column name.");
            }

            DataColumnSpec newColSpec = null;
            if (m_appendRowKey.getBooleanValue()) {
                final String newColName = m_newColumnName.getStringValue();
                newColSpec = createAppendRowKeyColSpec(newColName);
            }
            final RowKeyUtil util = new RowKeyUtil();
            outData = util.changeRowKey(data, exec,
                    m_newRowKeyColumn.getStringValue(),
                    m_appendRowKey.getBooleanValue(), newColSpec,
                    m_ensureUniqueness.getBooleanValue(),
                    m_handleMissingVals.getBooleanValue(),
                    m_removeRowKeyCol.getBooleanValue());
            final int missingValueCounter = util.getMissingValueCounter();
            final int duplicatesCounter = util.getDuplicatesCounter();
            final StringBuilder warningMsg = new StringBuilder();
            if (duplicatesCounter > 0) {
                warningMsg.append(duplicatesCounter
                        + " duplicate(s) now unique. ");
            }
            if (missingValueCounter > 0) {
                warningMsg.append(missingValueCounter
                        + " missing value(s) replaced with "
                        + RowKeyUtil.MISSING_VALUE_REPLACEMENT + ".");
            }
            if (warningMsg.length() > 0) {
                setWarningMessage(warningMsg.toString());
            }
            LOGGER.debug("Row ID replaced successfully");

        } else if (m_appendRowKey.getBooleanValue()) {
            LOGGER.debug("The user only wants to append a new column with "
                    + "name " + m_newColumnName);
            // the user wants only a column with the given name which
            //contains the rowkey as value
            final DataTableSpec tableSpec = data.getDataTableSpec();
            final String newColumnName = m_newColumnName.getStringValue();
            final ColumnRearranger c = RowKeyUtil.createColumnRearranger(
                    tableSpec, newColumnName, StringCell.TYPE);
            outData =
                exec.createColumnRearrangeTable(data, c, exec);
            exec.setMessage("New column created");
            LOGGER.debug("Column appended successfully");
        } else {
            //the user doesn't want to do anything at all so we simply return
            //the given data
            outData = data;
            LOGGER.debug("The user hasn't selected a new row ID column"
                    + " and hasn't entered a new column name.");
        }
        LOGGER.debug("Exiting execute(inData, exec) of class RowKeyNodeModel.");
        return new BufferedDataTable[]{outData};
    }

    /**
     * @param origSpec the original table specification (could be
     * <code>null</code>)
     * @param appendRowKey <code>true</code> if a new column should be created
     * @param newColName the name of the new column to append
     * @param replaceKey <code>true</code> if the row key should be replaced
     * @param newRowKeyCol the name of the row key column
     * @param removeRowKeyCol removes the selected row key column if set
     * to <code>true</code>
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected static void validateInput(final DataTableSpec origSpec,
            final boolean appendRowKey, final String newColName,
            final boolean replaceKey, final String newRowKeyCol,
            final boolean removeRowKeyCol) throws InvalidSettingsException {
        if (!(replaceKey || appendRowKey)) {
            //the user hasn't enabled an option
            throw new InvalidSettingsException(
                    "Please select at least on option.");
        }
        if (replaceKey) {
            if (newRowKeyCol == null || newRowKeyCol.trim().length() < 1) {
                throw new InvalidSettingsException(
                        "Please select the new row ID column.");
            }
            if (origSpec != null && !origSpec.containsName(newRowKeyCol)) {
                throw new InvalidSettingsException(
                "Selected column: '" + newRowKeyCol
                + "' not found in input table.");
            }
        }
        if (appendRowKey) {
            if (newColName == null || newColName.trim().length() < 1) {
                throw new InvalidSettingsException("Please provide a valid"
                        + " name for the new column.");
            }
            if (origSpec != null && origSpec.containsName(newColName)) {
                if (!replaceKey || !removeRowKeyCol
                        || !newRowKeyCol.equals(newColName)) {
                    throw new InvalidSettingsException("Column with name: '"
                        + newColName + "' already exists.");
                }
            }
        }
    }

    /**
     * @param origSpec the original table specification to check for
     * duplicate name
     * @param newColName the name of the column to add
     * @param newColType the type of the column to add
     * @return the column specification
     * @throws InvalidSettingsException if the name is invalid or exists in the
     * original table specification
     */
    private DataColumnSpec createAppendRowKeyColSpec(final String newColName) {
        final DataColumnSpecCreator colSpecCreater =
                new DataColumnSpecCreator(newColName, StringCell.TYPE);
        return colSpecCreater.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_hiLiteHandler.fireClearHiLiteEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check the input data
        assert (inSpecs != null && inSpecs.length == 1
                && inSpecs[DATA_IN_PORT] != null);
        DataTableSpec spec = inSpecs[DATA_IN_PORT];
        validateInput(spec, m_appendRowKey.getBooleanValue(),
                m_newColumnName.getStringValue(),
                m_replaceKey.getBooleanValue(),
                m_newRowKeyColumn.getStringValue(),
                m_removeRowKeyCol.getBooleanValue());
        if (m_replaceKey.getBooleanValue()) {
            final String selRowKey =
                m_newRowKeyColumn.getStringValue();
            if (m_removeRowKeyCol.getBooleanValue()) {
                spec = RowKeyUtil.createTableSpec(spec, selRowKey);
            }
        }
        if (m_appendRowKey.getBooleanValue()) {
            final DataColumnSpec colSpec = createAppendRowKeyColSpec(
                    m_newColumnName.getStringValue());
            spec = AppendedColumnTable.getTableSpec(spec,
                    colSpec);
        }
        return new DataTableSpec[]{spec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HiLiteHandler getOutHiLiteHandler(final int outPortID) {
        assert (outPortID == 0);
        return m_hiLiteHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);
        m_replaceKey.saveSettingsTo(settings);
        m_newRowKeyColumn.saveSettingsTo(settings);
        m_removeRowKeyCol.saveSettingsTo(settings);
        m_ensureUniqueness.saveSettingsTo(settings);
        m_handleMissingVals.saveSettingsTo(settings);
        m_appendRowKey.saveSettingsTo(settings);
        m_newColumnName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        m_replaceKey.loadSettingsFrom(settings);
        m_newRowKeyColumn.loadSettingsFrom(settings);
        try {
            m_removeRowKeyCol.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            // this is an older workflow set it to the old behaviour
            m_removeRowKeyCol.setBooleanValue(false);
        }
        m_ensureUniqueness.loadSettingsFrom(settings);
        m_handleMissingVals.loadSettingsFrom(settings);
        m_appendRowKey.loadSettingsFrom(settings);
        m_newColumnName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        final SettingsModelBoolean replaceRowKeyModel =
            m_replaceKey.createCloneWithValidatedValue(settings);
        final SettingsModelBoolean appendRowKeyModel =
            m_appendRowKey.createCloneWithValidatedValue(settings);
        final SettingsModelString newRowKeyModel =
            m_newRowKeyColumn.createCloneWithValidatedValue(
                    settings);
        final SettingsModelString newColNameModel =
                m_newColumnName.createCloneWithValidatedValue(settings);
        validateInput(null, appendRowKeyModel.getBooleanValue(),
                newColNameModel.getStringValue(),
                replaceRowKeyModel.getBooleanValue(),
                newRowKeyModel.getStringValue(),
                appendRowKeyModel.getBooleanValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        // nothing to do since the dialog settings are stored by the framework
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        // nothing to do since the dialog settings are stored by the framework
    }

}
