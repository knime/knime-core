/* -------------------------------------------------------------------
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
 * History 03.11.2006 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.rowkey;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * The node dialog of the row key manipulation node. The node allows the user
 * to replace the row key with another column and/or to append a new column 
 * with the values of the current row key.
 * 
 * @author Tobias Koetter
 */
public class RowKeyNodeDialog extends DefaultNodeSettingsPane {
    
    /**The label of the replace missing values checkbox.*/
    protected static final String HANDLE_MISSING_VALUES_LABEL = 
        "Handle missing values";

    /**The label of the uniqueness checkbox.*/
    protected static final String ENSURE_UNIQUENESS_LABEL = 
        "Ensure uniqueness";

    private final DialogComponentBoolean m_replaceRowKey;
    
    /**
     * The dialog component which holds the column the user has selected as 
     * new row key column.
     */
    private final DialogComponentColumnNameSelection m_newRowKeyCol;
    
    private final DialogComponentBoolean m_appendRowKeyCol;
    
    private final DialogComponentBoolean m_ensureUniqueness;
    
    private final DialogComponentBoolean m_replaceMissingvals;
    
    /**The dialog component which holds the name of the new column which
     * should contain the row key as value.*/
    private final DialogComponentString m_newColumnName;
    
    /**
     * New dialog for configuring the the row key node.
     */
    @SuppressWarnings("unchecked")
    public RowKeyNodeDialog() {
        super();
        createNewGroup("Replace row key:");
        final SettingsModelBoolean replaceKeyModel = 
            new SettingsModelBoolean(RowKeyNodeModel.REPLACE_ROWKEY, true);
        replaceKeyModel.addChangeListener(
                new ChangeListener() {
                    public void stateChanged(final ChangeEvent e) {
                        final boolean b = m_replaceRowKey.isSelected();
                        m_newRowKeyCol.setEnabled(b);
                        m_ensureUniqueness.setEnabled(b);
                        m_replaceMissingvals.setEnabled(b);
                    }
                });
        m_replaceRowKey = 
            new DialogComponentBoolean(replaceKeyModel,
                    "Replace row key with selected column values");
        addDialogComponent(m_replaceRowKey);
         m_newRowKeyCol = new DialogComponentColumnNameSelection(
                 new SettingsModelString(
                         RowKeyNodeModel.SELECTED_NEW_ROWKEY_COL, (String)null),
                 "New row key column:", RowKeyNodeModel.DATA_IN_PORT,
                DataValue.class);
         final boolean replaceKey = m_replaceRowKey.isSelected();
         m_newRowKeyCol.setEnabled(replaceKey);
         addDialogComponent(m_newRowKeyCol);
         m_ensureUniqueness = new DialogComponentBoolean(
                 new SettingsModelBoolean(RowKeyNodeModel.ENSURE_UNIQUNESS, 
                         false),
         ENSURE_UNIQUENESS_LABEL);
         m_ensureUniqueness.setToolTipText("Appends (x) to none unique values");
         addDialogComponent(m_ensureUniqueness);
         m_replaceMissingvals = new DialogComponentBoolean(
                 new SettingsModelBoolean(RowKeyNodeModel.REPLACE_MISSING_VALS, 
                         false), HANDLE_MISSING_VALUES_LABEL);
         m_replaceMissingvals.setToolTipText("Replaces missing values with " 
                 + RowKeyUtil.MISSING_VALUE_REPLACEMENT);
         addDialogComponent(m_replaceMissingvals);
         createNewGroup("Append row key column:");
         final SettingsModelBoolean appendRowModel = new SettingsModelBoolean(
                 RowKeyNodeModel.APPEND_ROWKEY_COLUMN, false);
         appendRowModel.addChangeListener(
                 new ChangeListener() {
                    public void stateChanged(final ChangeEvent e) {
                        m_newColumnName.setEnabled(
                                m_appendRowKeyCol.isSelected());
                    }
                 });
         m_appendRowKeyCol = 
             new DialogComponentBoolean(appendRowModel, 
                     "Create new column with row keys");
         addDialogComponent(m_appendRowKeyCol);
         m_newColumnName = new DialogComponentString(new SettingsModelString(
                 RowKeyNodeModel.NEW_COL_NAME_4_ROWKEY_VALS, (String)null),
                 "New column name:");
         m_newColumnName.setEnabled(m_appendRowKeyCol.isSelected());
         addDialogComponent(m_newColumnName);
    }
}
