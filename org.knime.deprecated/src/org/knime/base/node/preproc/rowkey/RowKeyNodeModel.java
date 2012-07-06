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
 * History 05.11.2006 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.rowkey;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.node.preproc.rowkey2.RowKeyUtil2;

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
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The node model of the row key manipulation node. The node allows the user
 * to replace the row key with another column and/or to append a new column
 * with the values of the current row key.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.6
 */
@Deprecated
public class RowKeyNodeModel extends NodeModel {

    // our logger instance
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RowKeyNodeModel.class);

    /** The port were the model expects the in data. */
    public static final int DATA_IN_PORT = 0;

    /** The port which the model uses to return the data. */
    public static final int DATA_OUT_PORT = 0;

    private static final String INTERNALS_FILE_NAME = "hilite_mapping.xml.gz";

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

    /**Configuration key for the enable hilite option.*/
    protected static final String CFG_ENABLE_HILITE = "enableHilite";

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

    private final SettingsModelBoolean m_enableHilite;

    /**The name of the new column which should contain the row key values. Could
     * be <code>null</code>.*/
    private final SettingsModelString m_newColumnName;

    /**
     * Node returns a new hilite handler instance.
     */
    private final HiLiteTranslator m_hilite = new HiLiteTranslator();
    /**
     * Constructor for class RowKeyNodeModel.
     */
    protected RowKeyNodeModel() {
        // we have one data in and one data out port
        super(1, 1);
        //initialise the settings models
        m_replaceKey = new SettingsModelBoolean(REPLACE_ROWKEY, true);
        m_newRowKeyColumn = new SettingsModelString(SELECTED_NEW_ROWKEY_COL,
                (String)null);
        m_newRowKeyColumn.setEnabled(m_replaceKey.getBooleanValue());
        final boolean enableReplaceOptions = enableReplaceOptions();
        m_removeRowKeyCol = new SettingsModelBoolean(
                REMOVE_ROW_KEY_COLUM, false);
        m_removeRowKeyCol.setEnabled(enableReplaceOptions);
        m_ensureUniqueness = new SettingsModelBoolean(ENSURE_UNIQUNESS, false);
        m_ensureUniqueness.setEnabled(enableReplaceOptions);
        m_handleMissingVals = new SettingsModelBoolean(HANDLE_MISSING_VALS,
                false);
        m_handleMissingVals.setEnabled(enableReplaceOptions);
        m_enableHilite = new SettingsModelBoolean(CFG_ENABLE_HILITE, false);
        m_enableHilite.setEnabled(m_replaceKey.getBooleanValue());

        m_appendRowKey = new SettingsModelBoolean(APPEND_ROWKEY_COLUMN, false);
        m_newColumnName = new SettingsModelString(NEW_COL_NAME_4_ROWKEY_VALS,
                (String)null);
        m_newColumnName.setEnabled(m_appendRowKey.getBooleanValue());

        m_replaceKey.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                final boolean b = enableReplaceOptions();
                m_newRowKeyColumn.setEnabled(m_replaceKey.getBooleanValue());
                m_removeRowKeyCol.setEnabled(b);
                m_ensureUniqueness.setEnabled(b);
                m_handleMissingVals.setEnabled(b);
                m_enableHilite.setEnabled(m_replaceKey.getBooleanValue());
            }
        });

        m_newRowKeyColumn.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                final boolean b = enableReplaceOptions();
                m_removeRowKeyCol.setEnabled(b);
                m_ensureUniqueness.setEnabled(b);
                m_handleMissingVals.setEnabled(b);
            }
        });
        m_appendRowKey.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_newColumnName.setEnabled(m_appendRowKey.getBooleanValue());
            }
        });
    }

    /**
     * @return <code>true</code> if the replace options should be enabled
     */
    protected boolean enableReplaceOptions() {
        return m_replaceKey.getBooleanValue()
            && m_newRowKeyColumn.getStringValue() != null;
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
                    + " optional appended column name"
                    + m_appendRowKey.getBooleanValue());
            if (m_newRowKeyColumn.getStringValue() != null) {
                // the user wants a new column as rowkey column
                final int colIdx = data.getDataTableSpec().findColumnIndex(
                        m_newRowKeyColumn.getStringValue());
                if (colIdx < 0) {
                    throw new InvalidSettingsException("No column with name: "
                            + m_newColumnName.getStringValue()
                            + " exists. Please select a valid column name.");
                }
            }
            DataColumnSpec newColSpec = null;
            if (m_appendRowKey.getBooleanValue()) {
                final String newColName = m_newColumnName.getStringValue();
                newColSpec = createAppendRowKeyColSpec(newColName);
            }
            final RowKeyUtil2 util = new RowKeyUtil2();
            outData = util.changeRowKey(data, exec,
                    m_newRowKeyColumn.getStringValue(),
                    m_appendRowKey.getBooleanValue(), newColSpec,
                    m_ensureUniqueness.getBooleanValue(),
                    m_handleMissingVals.getBooleanValue(),
                    m_removeRowKeyCol.getBooleanValue(),
                    m_enableHilite.getBooleanValue());
            if (m_enableHilite.getBooleanValue()) {
                m_hilite.setMapper(new DefaultHiLiteMapper(
                        util.getHiliteMapping()));
            }
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
            final ColumnRearranger c = RowKeyUtil2.createColumnRearranger(
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
//            if (newRowKeyCol == null || newRowKeyCol.trim().length() < 1) {
//                throw new InvalidSettingsException(
//                        "Please select the new row ID column.");
//            }
            if (newRowKeyCol != null && origSpec != null
                    && !origSpec.containsName(newRowKeyCol)) {
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
     * @param newColName the name of the column to add
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
        if (m_appendRowKey.getBooleanValue()
                && !m_replaceKey.getBooleanValue()) {
            // use the original hilite handler if the row keys do not change
            return getInHiLiteHandler(outIndex);
        } else {
            return m_hilite.getFromHiLiteHandler();
        }
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
            if (selRowKey == null) {
                setWarningMessage(
                        "No row key column selected generate a new one");
            } else if (m_removeRowKeyCol.getBooleanValue()) {
                spec = RowKeyUtil2.createTableSpec(spec, selRowKey);
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);
        m_replaceKey.saveSettingsTo(settings);
        m_newRowKeyColumn.saveSettingsTo(settings);
        m_removeRowKeyCol.saveSettingsTo(settings);
        m_ensureUniqueness.saveSettingsTo(settings);
        m_handleMissingVals.saveSettingsTo(settings);
        m_appendRowKey.saveSettingsTo(settings);
        m_newColumnName.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
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
        try {
            m_enableHilite.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
         // this option was introduced in KNIME version 2.0.3
            m_enableHilite.setBooleanValue(false);
        }
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
            final ExecutionMonitor exec) throws IOException  {
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
            final ExecutionMonitor exec) throws IOException  {
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

}
