/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 3, 2015 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.database.tablecreator.DBColumn;

/**
 * Panel used to define column settings for table creator
 *
 * @author Budi Yanto, KNIME.com
 */
public class ColumnsPanel extends AbstractTablePanel {

    private static final long serialVersionUID = 1L;

    /** Default title of this panel **/
    static final String DEFAULT_TITLE = "Columns";

    /** Width of the Not Null column in the table **/
    private static final int NOT_NULL_SIZE = 60;

    /** Cell editor used to edit SQL data type in the table **/
    private final SQLTypeCellEditor m_sqlTypeEditor;

    /**
     * Creates a new instance of ColumnsPanel
     *
     * @param key key used to store settings to NodeSettingsWO
     * @param config config to store all of the settings
     */
    public ColumnsPanel(final String key, final DBTableCreatorConfiguration config) {
        this(key, config, null, null, null);
    }

    /**
     * Creates a new instance of ColumnsPanel
     *
     * @param key key used to store settings to NodeSettingsWO
     * @param config config to store all of the settings
     * @param addButtonText text of the add button
     * @param removeButtonText text of the remove button
     * @param removeAllButtonText text of the removeAll button
     */
    public ColumnsPanel(final String key, final DBTableCreatorConfiguration config, final String addButtonText,
        final String removeButtonText, final String removeAllButtonText) {
        super(key, DEFAULT_TITLE, config, new TableCreatorTableModel(ColumnElement.COLUMN_NAMES,
            ColumnElement.COLUMN_CLASSES, config.getRowElements(key)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(final Object aValue, final int rowIndex, final int columnIndex) {
                if (aValue == null) {
                    return;
                }
                ColumnElement elem = (ColumnElement)getElement(rowIndex);
                switch (columnIndex) {
                    case ColumnElement.COLUMN_NAME_IDX:
                        elem.setName(aValue.toString());
                        break;
                    case ColumnElement.COLUMN_TYPE_IDX:
                        final String val = aValue.toString();
                        if(!StringUtils.isBlank(val)) {
                            elem.setType(val);
                        }
                        break;
                    case ColumnElement.NOT_NULL_IDX:
                        elem.setNotNull(((Boolean)aValue).booleanValue());
                        break;
                    default:
                        break;
                }
            }

            @Override
            protected boolean isEditable(final int rowIndex, final int columnIndex) {
                if(config.useDynamicSettings()) {
                    return false;
                }
                return true;
            }

            @Override
            protected Object getValueAtRow(final int rowIndex, final int columnIndex) {
                ColumnElement elem = (ColumnElement)getElement(rowIndex);
                switch (columnIndex) {
                    case ColumnElement.COLUMN_NAME_IDX:
                        return elem.getName();
                    case ColumnElement.COLUMN_TYPE_IDX:
                        return elem.getType();
                    case ColumnElement.NOT_NULL_IDX:
                        return elem.isNotNull();
                    default:
                        return null;
                }
            }

        }, addButtonText, removeButtonText, removeAllButtonText);

        m_sqlTypeEditor = getConfig().getSqlTypeCellEditor(key, SQLTypeCellEditor.NO_RELATED_COLUMN);

        // Adapts column "Column Type"
        TableColumnModel columnModel = getTable().getColumnModel();
        columnModel.getColumn(ColumnElement.COLUMN_TYPE_IDX).setCellEditor(m_sqlTypeEditor);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        // Adapts column "Column Name"
        columnModel.getColumn(ColumnElement.COLUMN_NAME_IDX).setCellEditor(new NameCellEditor());

        // Adapts column "Not Null"
        columnModel.getColumn(ColumnElement.NOT_NULL_IDX).setMinWidth(NOT_NULL_SIZE);
        columnModel.getColumn(ColumnElement.NOT_NULL_IDX).setMaxWidth(NOT_NULL_SIZE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRemoveAction() {

        int[] selectedRows = getTable().getSelectedRows();

        // Directly remove the selected columns if no key is associated with them.
        if (!columnsInKeys(selectedRows)) {
            super.onRemoveAction();
        } else { // Columns are used in key definition, warn user before removing the selected columns
            int answer = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(getTable()),
                "Do you really want to remove the selected columns? \n"
                    + "All keys that are associated with those columns will also be removed.",
                "Remove Columns Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                // Remove the corresponding keys if exist
                List<DBColumn> selectedRowsCollection = new ArrayList<>();
                for (int row : selectedRows) {
                    ColumnElement elem = (ColumnElement)getTableModel().getElement(row);
                    selectedRowsCollection.add(elem.getDBColumn());
                }
                removeKeysWithColumns(selectedRowsCollection);
                super.onRemoveAction(); // Remove the selected columns from the table
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRemoveAllAction() {
        List<RowElement> keysElement = getConfig().getRowElements(DBTableCreatorConfiguration.CFG_KEYS_SETTINGS);
        if (keysElement.isEmpty()) {
            super.onRemoveAllAction();
        } else {
            int answer = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(getTable()),
                "Do you really want to remove all columns? \n" + "All keys will also be removed.",
                "Remove Columns Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                keysElement.clear();
                super.onRemoveAllAction();
            }
        }
    }

    /**
     * Check whether the columns in the given rows are used in keys definition
     *
     * @param rows the selected rows in table
     * @return true if the columns in the selected rows are used in keys definition, otherwise false
     */
    private boolean columnsInKeys(final int[] rows) {
        for (int row : rows) {
            final ColumnElement elem = (ColumnElement)getTableModel().getElement(row);
            if (columnInKeys(elem.getDBColumn())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper method to check whether the given column is used as Key
     *
     * @param column the column to check
     * @return true if the column is used as key, otherwise false
     */
    private boolean columnInKeys(final DBColumn column) {
        final List<RowElement> keyElems = getConfig().getRowElements(DBTableCreatorConfiguration.CFG_KEYS_SETTINGS);
        for (RowElement elem : keyElems) {
            if (((KeyElement)elem).getColumns().contains(column)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes all keys that contain at least one column from the column collections
     *
     * @param columnIndices the column indices
     */
    void removeKeysWithColumns(final Collection<DBColumn> columns) {
        final List<RowElement> keysToRemove = new LinkedList<>();
        final List<RowElement> keyElems = getConfig().getRowElements(DBTableCreatorConfiguration.CFG_KEYS_SETTINGS);
        for (RowElement elem : keyElems) {
            for (DBColumn col : ((KeyElement)elem).getColumns()) {
                if (columns.contains(col)) {
                    keysToRemove.add(elem);
                }
            }
        }
        keyElems.removeAll(keysToRemove);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getDefaultValue(final int columnIndex) {
        if (columnIndex < 0 || columnIndex >= getTableModel().getColumnCount()) {
            throw new IllegalArgumentException("invalid column index");
        }

        switch (columnIndex) {
            case ColumnElement.COLUMN_NAME_IDX:
                return getNewIdentifierForColumn(ColumnElement.COLUMN_NAME_IDX);
            case ColumnElement.COLUMN_TYPE_IDX:
                return DBUtil.getDefaultSQLType(StringCell.TYPE);
            case ColumnElement.NOT_NULL_IDX:
                return ColumnElement.DEFAULT_NOT_NULL_VALUE;
            default:
                return null;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RowElement createRowElement() {
        return new ColumnElement((String)getDefaultValue(ColumnElement.COLUMN_NAME_IDX),
            (String)getDefaultValue(ColumnElement.COLUMN_TYPE_IDX),
            (Boolean)getDefaultValue(ColumnElement.NOT_NULL_IDX));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoad() {
        if(getConfig().useDynamicSettings()) {
            setEnabledAddButton(false);
            setEnabledRemoveButton(false);
        } else {
            super.onLoad();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClick(final MouseEvent ev) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createNoRowAvailableMenu(final JPopupMenu menu) {
        if(getConfig().useDynamicSettings()) {
            createNotEditableMenu(menu);
        } else {
            super.createNoRowAvailableMenu(menu);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createTablePopupMenu(final JPopupMenu menu) {
        if(getConfig().useDynamicSettings()) {
            createNotEditableMenu(menu);
        } else {
            createNoRowSelectedMenu(menu);
            menu.addSeparator();
            createToggleNotNullMenu(menu);
            menu.addSeparator();
            createSetColumnTypeMenu(menu);
            createSetToNewTypeMenu(menu);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createNoRowSelectedMenu(final JPopupMenu menu) {
        if(getConfig().useDynamicSettings()) {
            createNotEditableMenu(menu);
        } else {
            createSelectColumnsFromTypeMenu(menu);
        }
    }

    /**
     * Create pop up menu to select all items from the specified type.
     *
     * @param menu the parent pop up menu to attach the menu items
     */
    private void createSelectColumnsFromTypeMenu(final JPopupMenu menu) {
        final JMenu subMenu = new JMenu("Select columns from type");
        menu.add(subMenu);

        // Store all data types used in the table and their corresponding row indices
        Map<String, List<Integer>> typeRowIdxMapping = new TreeMap<>();
        for (int i = 0; i < getTableModel().getRowCount(); i++) {
            ColumnElement elem = (ColumnElement)getTableModel().getElement(i);
            String type = elem.getType();
            List<Integer> indices = typeRowIdxMapping.get(type);
            if (indices == null) {
                indices = new ArrayList<>();
                indices.add(i);
                typeRowIdxMapping.put(type, indices);
            } else {
                indices.add(i);
            }
        }

        // Create menu items for all used data types and add their ActionListener
        for (Entry<String, List<Integer>> entry : typeRowIdxMapping.entrySet()) {
            final JMenuItem item = new JMenuItem(entry.getKey());
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    // Select all columns that has the corresponding type
                    getTable().clearSelection();
                    for (int idx : entry.getValue()) {
                        getTable().addRowSelectionInterval(idx, idx);
                    }
                }
            });
            subMenu.add(item);
        }
    }

    /**
     * Creates pop up menu item to toggle not null options for all selected items.
     *
     * @param menu the parent pop up menu to attach the menu item
     */
    private void createToggleNotNullMenu(final JPopupMenu menu) {
        final JMenuItem item = new JMenuItem("Toggle not null options");
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                for (final int row : getTable().getSelectedRows()) {
                    ColumnElement elem = (ColumnElement)getTableModel().getElement(row);
                    getTableModel().setValueAt(!elem.isNotNull(), row, ColumnElement.NOT_NULL_IDX);
                }
            }
        });

        menu.add(item);
    }

    /**
     * Creates pop up menu to set all selected items to the specified type.
     *
     * @param menu the parent pop up menu to attach the menu items
     */
    private void createSetColumnTypeMenu(final JPopupMenu menu) {
        final JMenu subMenu = new JMenu("Set type to");
        menu.add(subMenu);
        for (int i = 0; i < m_sqlTypeEditor.getSize(); i++) {
            String type = m_sqlTypeEditor.getElementAt(i);
            final JMenuItem item = new JMenuItem(type);
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    for (final int row : getTable().getSelectedRows()) {
                        getTableModel().setValueAt(type, row, ColumnElement.COLUMN_TYPE_IDX);
                    }
                }
            });
            subMenu.add(item);
        }
    }

    /**
     * Creates pop up menu to set all selected items to a newly created type.
     *
     * @param menu the parent pop up menu to attach the menu item
     */
    private void createSetToNewTypeMenu(final JPopupMenu menu) {
        final JMenuItem item = new JMenuItem("Set to a new type");
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                // Shows an input dialog to allow user to input a new type
                String newType = JOptionPane.showInputDialog(menu, "Enter a new type: ", "Create New Type",
                    JOptionPane.PLAIN_MESSAGE);

                if (newType == null) {
                    // Do nothing, the user click cancel button
                } else if (newType.isEmpty()) {
                    // Type cannot be empty, show error dialog
                    JOptionPane.showMessageDialog(menu, "Type cannot be empty", "Error: Empty Type",
                        JOptionPane.ERROR_MESSAGE);
                } else { // Everything is okay, set to new type
                    newType = newType.trim();
                    m_sqlTypeEditor.addElement(newType);

                    for (final int row : getTable().getSelectedRows()) {
                        getTableModel().setValueAt(newType, row, ColumnElement.COLUMN_TYPE_IDX);
                    }
                }
            }
        });
        menu.add(item);
    }

}
