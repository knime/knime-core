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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.knime.base.data.aggregation.dialogutil.BooleanCellRenderer;
import org.knime.core.data.DataType;

/**
 * Panel used to map columns to SQL data types based on the Knime data type of the columns
 *
 * @author Budi Yanto, KNIME.com
 */
public class KNIMEBasedMappingPanel extends AbstractTablePanel {

    private static final long serialVersionUID = 1L;

    /** Default title of this panel **/
    private static final String DEFAULT_TITLE = "KNIME-Based SQL Type Mapping";

    /** Width of the Not Null column in the table **/
    private static final int NOT_NULL_SIZE = 60;

    /** Cell editor used to edit SQL data type in the table **/
    private final SQLTypeCellEditor m_sqlTypeEditor;

    /** Cell editor used to edit Knime data type in the table **/
    private final KNIMETypeCellEditor m_knimeTypeEditor;

    /**
     * Creates a new instance of KnimeBasedMappingPanel
     *
     * @param key key used to store settings to NodeSettingsWO
     * @param config config to store all of the settings
     */
    public KNIMEBasedMappingPanel(final String key, final DBTableCreatorConfiguration config) {
        this(key, config, null, null, null);
    }

    /**
     * Creates a new instance of KnimeBasedMappingPanel
     *
     * @param key key used to store settings to NodeSettingsWO
     * @param config config to store all of the settings
     * @param addButtonText text of the add button
     * @param removeButtonText text of the remove button
     * @param removeAllButtonText text of the removeAll button
     */
    public KNIMEBasedMappingPanel(final String key, final DBTableCreatorConfiguration config,
        final String addButtonText, final String removeButtonText, final String removeAllButtonText) {
        super(key, DEFAULT_TITLE, config, new TableCreatorTableModel(KNIMEBasedMappingElement.COLUMN_NAMES,
            KNIMEBasedMappingElement.COLUMN_CLASSES, config.getRowElements(key)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(final Object aValue, final int rowIndex, final int columnIndex) {
                if (aValue == null) {
                    return;
                }
                KNIMEBasedMappingElement elem = (KNIMEBasedMappingElement)getElement(rowIndex);
                switch (columnIndex) {
                    case KNIMEBasedMappingElement.KNIME_TYPE_IDX:
                        DataType knimeType = (DataType)aValue;
                        elem.setKnimeType(knimeType);

                        // Change SQL-Type value to default value according to Knime-Type
                        setValueAt(DBUtil.getDefaultSQLType(knimeType), rowIndex,
                            KNIMEBasedMappingElement.SQL_TYPE_IDX);
                        break;
                    case KNIMEBasedMappingElement.SQL_TYPE_IDX:
                        elem.setSqlType(aValue.toString());
                        break;
                    case KNIMEBasedMappingElement.NOT_NULL_IDX:
                        elem.setNotNull((Boolean)aValue);
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
                KNIMEBasedMappingElement elem = (KNIMEBasedMappingElement)getElement(rowIndex);
                switch (columnIndex) {
                    case KNIMEBasedMappingElement.KNIME_TYPE_IDX:
                        return elem.getKnimeType();
                    case KNIMEBasedMappingElement.SQL_TYPE_IDX:
                        return elem.getSqlType();
                    case KNIMEBasedMappingElement.NOT_NULL_IDX:
                        return elem.isNotNull();
                    default:
                        return null;
                }
            }
        }, addButtonText, removeButtonText, removeAllButtonText);

        // Adapts column "SQL-Type"
        m_sqlTypeEditor = getConfig().getSqlTypeCellEditor(key, KNIMEBasedMappingElement.KNIME_TYPE_IDX);
        TableColumnModel columnModel = getTable().getColumnModel();
        columnModel.getColumn(KNIMEBasedMappingElement.SQL_TYPE_IDX).setCellEditor(m_sqlTypeEditor);

        // Adapts column "Knime-Type"
        m_knimeTypeEditor = new KNIMETypeCellEditor(DBUtil.getKNIMETypeList(null));
        columnModel.getColumn(KNIMEBasedMappingElement.KNIME_TYPE_IDX).setCellEditor(m_knimeTypeEditor);
        columnModel.getColumn(KNIMEBasedMappingElement.KNIME_TYPE_IDX).setCellRenderer(new DefaultTableCellRenderer() {

            private static final long serialVersionUID = 1L;

            /**
             * {@inheritDoc}
             */
            @Override
            protected void setValue(final Object value) {
                DataType dt = (DataType)value;
                if (dt == null) {
                    setText("");
                } else {
                    setText(dt.toString());
                    setIcon(dt.getIcon());
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {

        // Adapts column "Not Null"
        int colIdx = KNIMEBasedMappingElement.NOT_NULL_IDX;
        columnModel.getColumn(colIdx).setCellRenderer(
            new BooleanCellRenderer("Tick if the columns that have the selected knime type should be not null"));
        columnModel.getColumn(colIdx).setMinWidth(NOT_NULL_SIZE);
        columnModel.getColumn(colIdx).setMaxWidth(NOT_NULL_SIZE);
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
            case KNIMEBasedMappingElement.KNIME_TYPE_IDX:
                return KNIMEBasedMappingElement.DEFAULT_KNIME_TYPE;
            case KNIMEBasedMappingElement.SQL_TYPE_IDX:
                return KNIMEBasedMappingElement.DEFAULT_SQL_TYPE;
            case KNIMEBasedMappingElement.NOT_NULL_IDX:
                return KNIMEBasedMappingElement.DEFAULT_NOT_NULL;
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RowElement createRowElement() {
        DataType dt = m_knimeTypeEditor.getNextItem();
        if (dt != null) {
            if (m_knimeTypeEditor.isItemAvailable()) {
                setEnabledAddButton(false);
            }

            final String sqlType = DBUtil.getDefaultSQLType(dt);
            return new KNIMEBasedMappingElement(dt, sqlType,
                (Boolean)getDefaultValue(KNIMEBasedMappingElement.NOT_NULL_IDX));
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoad() {
        super.onLoad();
        m_knimeTypeEditor.setKNIMETypes(DBUtil.getKNIMETypeList(getConfig().getTableSpec()));
        m_knimeTypeEditor.removeAllFromHash();
        for (RowElement elem : getTableModel().getElements()) {
            final DataType dt = ((KNIMEBasedMappingElement)elem).getKnimeType();
            m_knimeTypeEditor.addItemToHash(dt);
        }

        if (m_knimeTypeEditor.isItemAvailable()) {
            setEnabledAddButton(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRemoveAction() {
        for (int idx : getTable().getSelectedRows()) {
            KNIMEBasedMappingElement elem = (KNIMEBasedMappingElement)getTableModel().getElement(idx);
            m_knimeTypeEditor.removeItemFromHash(elem.getKnimeType());
        }
        if (!isEnabledAddButton()) {
            setEnabledAddButton(true);
        }
        super.onRemoveAction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRemoveAllAction() {
        m_knimeTypeEditor.removeAllFromHash();
        if (!isEnabledAddButton()) {
            setEnabledAddButton(true);
        }
        super.onRemoveAllAction();
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
        createNoRowSelectedMenu(menu);
        menu.addSeparator();
        createToggleNotNullMenu(menu);
        menu.addSeparator();
        createSetToTypeMenu(menu);
        createSetToNewTypeMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createNoRowSelectedMenu(final JPopupMenu menu) {
        createSelectKnimeTypesMapTo(menu);
    }

    /**
     * Create pop up menu to select all Knime data types that map to a specified type
     *
     * @param menu the parent pop up menu to attach the menu items
     */
    private void createSelectKnimeTypesMapTo(final JPopupMenu menu) {
        final JMenu subMenu = new JMenu("Select knime types map to");
        menu.add(subMenu);

        // Store all data types used in the table and their corresponding row indices
        Map<String, List<Integer>> typeRowIdxMapping = new TreeMap<>();
        for (int i = 0; i < getTableModel().getRowCount(); i++) {
            KNIMEBasedMappingElement elem = (KNIMEBasedMappingElement)getTableModel().getElement(i);
            String type = elem.getSqlType();
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
                    // Select all knime types that map to the specified type
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
                    KNIMEBasedMappingElement elem = (KNIMEBasedMappingElement)getTableModel().getElement(row);
                    getTableModel().setValueAt(!elem.isNotNull(), row, KNIMEBasedMappingElement.NOT_NULL_IDX);
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
    private void createSetToTypeMenu(final JPopupMenu menu) {
        final JMenu subMenu = new JMenu("Set type to");
        menu.add(subMenu);
        Set<String> typeHash = new HashSet<>();
        for (final int row : getTable().getSelectedRows()) {
            final KNIMEBasedMappingElement elem = (KNIMEBasedMappingElement)getTableModel().getElement(row);
            final String modelKey = elem.getKnimeType().toString();
            for (int i = 0; i < m_sqlTypeEditor.getSize(modelKey); i++) {
                final String type = m_sqlTypeEditor.getElementAt(modelKey, i);
                if (typeHash.add(type)) {
                    final JMenuItem item = new JMenuItem(type);
                    item.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            for (final int idx : getTable().getSelectedRows()) {
                                final KNIMEBasedMappingElement el =
                                    (KNIMEBasedMappingElement)getTableModel().getElement(idx);
                                m_sqlTypeEditor.addElement(el.getKnimeType().toString(), type);
                                getTableModel().setValueAt(type, idx, KNIMEBasedMappingElement.SQL_TYPE_IDX);
                            }
                        }
                    });
                    subMenu.add(item);
                }
            }
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

                    for (final int row : getTable().getSelectedRows()) {
                        final KNIMEBasedMappingElement elem = (KNIMEBasedMappingElement)getTableModel().getElement(row);
                        m_sqlTypeEditor.addElement(elem.getKnimeType().toString(), newType);
                        getTableModel().setValueAt(newType, row, KNIMEBasedMappingElement.SQL_TYPE_IDX);
                    }
                }
            }
        });
        menu.add(item);
    }
}
