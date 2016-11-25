/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 11, 2015 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableColumnModel;

import org.knime.base.data.aggregation.dialogutil.BooleanCellRenderer;

/**
 * Panel used to define key based on the name of the columns
 *
 * @author Budi Yanto, KNIME.com
 */
public class NameBasedKeysPanel extends AbstractTablePanel {

    private static final long serialVersionUID = 1L;

    /** Default title of this panel **/
    private static final String DEFAULT_TITLE = "Dynamic Keys Settings";

    /** Width of the Regex column in the table **/
    private static final int REGEX_SIZE = 45;

    /** Width of the Primary Key column in the table **/
    private static final int PRIMARY_KEY_SIZE = 80;

    /**
     * Creates a new instance of NameBasedKeysPanel
     *
     * @param key key used to store settings to NodeSettingsWO
     * @param config config to store all of the settings
     */
    public NameBasedKeysPanel(final String key, final DBTableCreatorConfiguration config) {
        this(key, config, null, null, null);
    }

    /**
     * Creates a new instance of NameBasedKeysPanel
     *
     * @param key key used to store settings to NodeSettingsWO
     * @param config config to store all of the settings
     * @param addButtonText text of the add button
     * @param removeButtonText text of the remove button
     * @param removeAllButtonText text of the removeAll button
     */
    public NameBasedKeysPanel(final String key, final DBTableCreatorConfiguration config, final String addButtonText,
        final String removeButtonText, final String removeAllButtonText) {
        super(key, DEFAULT_TITLE, config, new TableCreatorTableModel(NameBasedKeysElement.COLUMN_NAMES,
            NameBasedKeysElement.COLUMN_CLASSES, config.getRowElements(key)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(final Object aValue, final int rowIndex, final int columnIndex) {
                if (aValue == null) {
                    return;
                }
                NameBasedKeysElement elem = (NameBasedKeysElement)getElement(rowIndex);
                switch (columnIndex) {
                    case NameBasedKeysElement.NAME_PATTERN_IDX:
                        elem.setNamePattern(aValue.toString());
                        break;
                    case NameBasedKeysElement.REGEX_IDX:
                        elem.setRegex((Boolean)aValue);
                        break;
                    case NameBasedKeysElement.KEY_NAME_IDX:
                        elem.setKeyName(aValue.toString());
                        break;
                    case NameBasedKeysElement.PRIMARY_KEY_IDX:
                        elem.setPrimaryKey((Boolean)aValue);
                        break;
                    default:
                        break;
                }
            }

            @Override
            protected boolean isEditable(final int rowIndex, final int columnIndex) {
                return true;
            }

            @Override
            protected Object getValueAtRow(final int rowIndex, final int columnIndex) {
                NameBasedKeysElement elem = (NameBasedKeysElement)getElement(rowIndex);
                switch (columnIndex) {
                    case NameBasedKeysElement.NAME_PATTERN_IDX:
                        return elem.getNamePattern();
                    case NameBasedKeysElement.REGEX_IDX:
                        return elem.isRegex();
                    case NameBasedKeysElement.KEY_NAME_IDX:
                        return elem.getKeyName();
                    case NameBasedKeysElement.PRIMARY_KEY_IDX:
                        return elem.isPrimaryKey();
                    default:
                        return null;
                }
            }
        }, addButtonText, removeButtonText, removeAllButtonText);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        // Adapts column "Regex"
        int colIdx = NameBasedKeysElement.REGEX_IDX;
        columnModel.getColumn(colIdx)
            .setCellRenderer(new BooleanCellRenderer("Tick if the pattern is a regular expression"));
        columnModel.getColumn(colIdx).setMinWidth(REGEX_SIZE);
        columnModel.getColumn(colIdx).setMaxWidth(REGEX_SIZE);

        // Adapts column "Key Name"
        colIdx = NameBasedKeysElement.KEY_NAME_IDX;
        columnModel.getColumn(colIdx).setCellEditor(new NameCellEditor());

        // Adapts column "Primary Key"
        colIdx = NameBasedKeysElement.PRIMARY_KEY_IDX;
        columnModel.getColumn(colIdx).setCellEditor(new RadioButtonCellEditor());
        columnModel.getColumn(colIdx).setCellRenderer(new RadioButtonCellRenderer());
        columnModel.getColumn(colIdx).setMinWidth(PRIMARY_KEY_SIZE);
        columnModel.getColumn(colIdx).setMaxWidth(PRIMARY_KEY_SIZE);
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
            case NameBasedKeysElement.NAME_PATTERN_IDX:
                return NameBasedKeysElement.DEFAULT_NAME_PATTERN;
            case NameBasedKeysElement.REGEX_IDX:
                return NameBasedKeysElement.DEFAULT_IS_REGEX;
            case NameBasedKeysElement.KEY_NAME_IDX:
                return getNewIdentifierForColumn(NameBasedKeysElement.KEY_NAME_IDX);
            case NameBasedKeysElement.PRIMARY_KEY_IDX:
                return NameBasedKeysElement.DEFAULT_PRIMARY_KEY;
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RowElement createRowElement() {
        return new NameBasedKeysElement((String)getDefaultValue(NameBasedKeysElement.NAME_PATTERN_IDX),
            (Boolean)getDefaultValue(NameBasedKeysElement.REGEX_IDX),
            (String)getDefaultValue(NameBasedKeysElement.KEY_NAME_IDX),
            (Boolean)getDefaultValue(NameBasedKeysElement.PRIMARY_KEY_IDX));
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
    protected void createTablePopupMenu(final JPopupMenu menu) {
        menu.addSeparator();
        createToggleRegexMenu(menu);
        if (getTable().getSelectedRowCount() == 1) {
            createSetAsPrimaryKeyMenu(menu);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createNoRowSelectedMenu(final JPopupMenu menu) {
        // TODO Auto-generated method stub
    }

    /**
     * Creates pop up menu item to toggle regex options for all selected items.
     *
     * @param menu the parent pop up menu to attach the menu item
     */
    private void createToggleRegexMenu(final JPopupMenu menu) {
        final JMenuItem item = new JMenuItem("Toggle regex options");
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                for (final int row : getTable().getSelectedRows()) {
                    NameBasedKeysElement elem = (NameBasedKeysElement)getTableModel().getElement(row);
                    getTableModel().setValueAt(!elem.isRegex(), row, NameBasedKeysElement.REGEX_IDX);
                }
            }
        });

        menu.add(item);
    }

    /**
     * Creates table pop up menu item to toggle primary key options for all selected items
     *
     * @param menu the parent table pop up menu to attach the menu item
     */
    private void createSetAsPrimaryKeyMenu(final JPopupMenu menu) {
        final JMenuItem item = new JMenuItem("Toggle primary key option");
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                togglePrimaryKey(getTable().getSelectedRow());
            }
        });
        menu.add(item);
    }

    /**
     * A helper method to set a key as primary key and on the same time set other keys as unique. Only one primary key
     * is allowed.
     *
     * @param index the index of the key to set as primary key
     */
    private void togglePrimaryKey(final int index) {
        // Set it to false if it is already a primary key
        if ((Boolean)getTableModel().getValueAt(index, NameBasedKeysElement.PRIMARY_KEY_IDX)) {
            getTableModel().setValueAt(false, index, NameBasedKeysElement.PRIMARY_KEY_IDX);
        } else {
            // Set it to true to indicate that it is a primary key
            // and on the same time set all other keys to false to indicate
            // that they are unique keys
            for (int i = 0; i < getTableModel().getRowCount(); i++) {
                if (i == index) {
                    getTableModel().setValueAt(true, i, NameBasedKeysElement.PRIMARY_KEY_IDX);
                } else {
                    getTableModel().setValueAt(false, i, NameBasedKeysElement.PRIMARY_KEY_IDX);
                }
            }
        }
    }

}
