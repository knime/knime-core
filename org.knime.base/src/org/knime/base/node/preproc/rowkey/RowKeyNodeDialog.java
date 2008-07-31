/*
 * ------------------------------------------------------------------
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
 * History 03.11.2006 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.rowkey;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * The node dialog of the row key manipulation node. The node allows the user
 * to replace the row key with another column and/or to append a new column
 * with the values of the current row key.
 *
 * @author Tobias Koetter
 */
public class RowKeyNodeDialog extends DefaultNodeSettingsPane {

    /**The label of the replace row key group section.*/
    private static final String REPLACE_ROW_KEY_GROUP_LABEL =
        "Replace RowID:";

    /**The label of the replace row id select box which enables/disables
     * the replacing options.*/
    private static final String REPLACE_ROW_BOX_LABEL =
        "Replace RowID with selected column values";

    /**The label of the new row key column select box.*/
    private static final String NEW_ROW_KEY_COLUMN_LABEL =
        "New RowID column:";
    /**The label of the uniqueness check box.*/
    private  static final String REMOVE_ROWKEY_COL_LABEL =
        "Remove selected column";
    private  static final String REMOVE_ROWKEY_COL_TOOLTIP =
        "Removes the selected new RowID column";

    /**The label of the replace missing values check box.*/
    private static final String HANDLE_MISSING_VALUES_LABEL =
        "Handle missing values";
    /**The tool tip of the replace missing value check box.*/
    private static final String HANDLEMISSING_VALUES_TOOLTIP =
        "Replaces missing values with '"
        + RowKeyUtil.MISSING_VALUE_REPLACEMENT + "'.";

    /**The label of the uniqueness check box.*/
    protected static final String ENSURE_UNIQUENESS_LABEL =
        "Ensure uniqueness";
    /**The tool tip of the uniqueness check box.*/
    private static final String ENSURE_UNIQUENESS_TOOLTIP =
        "Appends (x) to none unique values.";


    /**The name of the append row key group section.*/
    private static final String APPEND_ROW_KEY_GROUP_LABEL =
        "Append RowID column:";

    /**The label of the append row key column check box which enables/disables
     * the append row key options.*/
    private static final String APPEND_ROW_KEY_COLUMN_LABEL =
        "Create new column with the RowID values";

    /**The label of the new column name input field.*/
    private static final String NEW_COLUMN_NAME_LABEL = "New column name:";

    private final SettingsModelBoolean m_replaceKey;

    private final SettingsModelString m_newRowKeyColumn;

    private final SettingsModelBoolean m_removeRowKeyCol;

    private final SettingsModelBoolean m_ensureUniqueness;

    private final SettingsModelBoolean m_handleMissingVals;

    private final SettingsModelBoolean m_appendRowKey;

    private final SettingsModelString m_newColumnName;

    private DataTableSpec m_tableSpec = null;

    /**
     * New dialog for configuring the the row key node.
     */
    @SuppressWarnings("unchecked")
    public RowKeyNodeDialog() {
        super();
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

        createNewGroup(REPLACE_ROW_KEY_GROUP_LABEL);
        final DialogComponent replaceRowKey = new DialogComponentBoolean(
                m_replaceKey, REPLACE_ROW_BOX_LABEL);
        addDialogComponent(replaceRowKey);

        final DialogComponent newRowKeyCol =
            new DialogComponentColumnNameSelection(m_newRowKeyColumn,
                    NEW_ROW_KEY_COLUMN_LABEL, RowKeyNodeModel.DATA_IN_PORT,
                    DataValue.class);
        addDialogComponent(newRowKeyCol);

        final DialogComponent removeRowKeyCol = new DialogComponentBoolean(
                m_removeRowKeyCol, REMOVE_ROWKEY_COL_LABEL);
        removeRowKeyCol.setToolTipText(REMOVE_ROWKEY_COL_TOOLTIP);
        addDialogComponent(removeRowKeyCol);

        final DialogComponent ensureUniqueness = new DialogComponentBoolean(
                m_ensureUniqueness, ENSURE_UNIQUENESS_LABEL);
        ensureUniqueness.setToolTipText(ENSURE_UNIQUENESS_TOOLTIP);
        addDialogComponent(ensureUniqueness);

        final DialogComponent replaceMissingvals = new DialogComponentBoolean(
                m_handleMissingVals, HANDLE_MISSING_VALUES_LABEL);
        replaceMissingvals.setToolTipText(HANDLEMISSING_VALUES_TOOLTIP);
        addDialogComponent(replaceMissingvals);

        createNewGroup(APPEND_ROW_KEY_GROUP_LABEL);
        final DialogComponent appendRowKeyCol = new DialogComponentBoolean(
                m_appendRowKey, APPEND_ROW_KEY_COLUMN_LABEL);
        addDialogComponent(appendRowKeyCol);

        final DialogComponent newColumnName = new DialogComponentString(
                m_newColumnName, NEW_COLUMN_NAME_LABEL);
        addDialogComponent(newColumnName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        if (specs.length > 0) {
            m_tableSpec = specs[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
        if (m_tableSpec != null) {
            RowKeyNodeModel.validateInput(m_tableSpec,
                    m_appendRowKey.getBooleanValue(),
                    m_newColumnName.getStringValue(),
                    m_replaceKey.getBooleanValue(),
                    m_newRowKeyColumn.getStringValue(),
                    m_removeRowKeyCol.getBooleanValue());
        }
    }
}
