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
 * History 03.11.2006 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.rowkey;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
        "Replace RowID with selected column values or create a new one";

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

    /**The label of the enable hilite values check box.*/
    private static final String ENABLE_HILITE_LABEL =
        "Enable hiliting";
    /**The tool tip of the enable hilite value check box.*/
    private static final String ENABLE_HILITE_TOOLTIP =
        "Enabling leads to more memory consumtion.";

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

    private final SettingsModelBoolean m_enableHilite;

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
        final boolean enableReplaceOptions = enableReplaceOptions();
        m_removeRowKeyCol = new SettingsModelBoolean(
                RowKeyNodeModel.REMOVE_ROW_KEY_COLUM, false);
        m_removeRowKeyCol.setEnabled(enableReplaceOptions);
        m_ensureUniqueness = new SettingsModelBoolean(
                RowKeyNodeModel.ENSURE_UNIQUNESS, false);
        m_ensureUniqueness.setEnabled(enableReplaceOptions);
        m_handleMissingVals = new SettingsModelBoolean(
                RowKeyNodeModel.HANDLE_MISSING_VALS, false);
        m_handleMissingVals.setEnabled(enableReplaceOptions);
        m_enableHilite = new SettingsModelBoolean(
                RowKeyNodeModel.CFG_ENABLE_HILITE, false);
        m_enableHilite.setEnabled(enableReplaceOptions);

        m_appendRowKey = new SettingsModelBoolean(
                RowKeyNodeModel.APPEND_ROWKEY_COLUMN, false);
        m_newColumnName = new SettingsModelString(
                RowKeyNodeModel.NEW_COL_NAME_4_ROWKEY_VALS, (String)null);
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
                rowKeyColChanged();
            }
        });

        m_appendRowKey.addChangeListener(new ChangeListener() {
            @Override
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
                    false, true, DataValue.class);
        newRowKeyCol.setToolTipText("Select <none> to generate a new row key");
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
        final DialogComponent enableHilite = new DialogComponentBoolean(
                m_enableHilite, ENABLE_HILITE_LABEL);
        enableHilite.setToolTipText(ENABLE_HILITE_TOOLTIP);
        addDialogComponent(enableHilite);


        createNewGroup(APPEND_ROW_KEY_GROUP_LABEL);
        final DialogComponent appendRowKeyCol = new DialogComponentBoolean(
                m_appendRowKey, APPEND_ROW_KEY_COLUMN_LABEL);
        addDialogComponent(appendRowKeyCol);

        final DialogComponent newColumnName = new DialogComponentString(
                m_newColumnName, NEW_COLUMN_NAME_LABEL);
        addDialogComponent(newColumnName);
    }

    /**
     * @return <code>true</code> if the replace options should be enabled
     */
    protected boolean enableReplaceOptions() {
        return m_replaceKey.getBooleanValue()
            && m_newRowKeyColumn.getStringValue() != null;
    }

    private void rowKeyColChanged() {
        final boolean b = enableReplaceOptions();
        m_removeRowKeyCol.setEnabled(b);
        m_ensureUniqueness.setEnabled(b);
        m_handleMissingVals.setEnabled(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        m_tableSpec = specs[0];
        rowKeyColChanged();
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
