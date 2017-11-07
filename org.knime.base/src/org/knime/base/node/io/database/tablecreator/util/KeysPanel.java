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

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumnModel;

import org.knime.core.node.port.database.tablecreator.DBColumn;

/**
 * Panel used to define key settings for table creator
 *
 * @author Budi Yanto, KNIME.com
 */
public class KeysPanel extends AbstractTablePanel {

    private static final long serialVersionUID = 1L;

    /** Default title of this panel **/
    private static final String DEFAULT_TITLE = "Keys";

    /** Width of the Primary Key column in the table **/
    private static final int PRIMARY_KEY_SIZE = 80;

    /**
     * Creates a new instance of KeysPanel
     *
     * @param key key used to store settings to NodeSettingsWO
     * @param config config to store all of the settings
     */
    public KeysPanel(final String key, final DBTableCreatorConfiguration config) {
        this(key, config, null, null, null);
    }

    /**
     * Creates a new instance of KeysPanel
     * @param key key used to store settings to NodeSettingsWO
     * @param config config to store all of the settings
     * @param addButtonText text of the add button
     * @param removeButtonText text of the remove button
     * @param removeAllButtonText text of the removeAll button
     */
    public KeysPanel(final String key, final DBTableCreatorConfiguration config, final String addButtonText,
        final String removeButtonText, final String removeAllButtonText) {
        super(key, DEFAULT_TITLE, config,
            new TableCreatorTableModel(KeyElement.COLUMN_NAMES, KeyElement.COLUMN_CLASSES, config.getRowElements(key)) {

                private static final long serialVersionUID = 1L;

                @SuppressWarnings("unchecked")
                @Override
                protected void setValue(final Object aValue, final int rowIndex, final int columnIndex) {
                    if (aValue == null) {
                        return;
                    }

                    KeyElement elem = (KeyElement)getElement(rowIndex);
                    switch (columnIndex) {
                        case KeyElement.KEY_NAME_IDX:
                            elem.setName(aValue.toString());
                            break;
                        case KeyElement.KEY_COLUMNS_IDX:
                            elem.setColumnElements((Set<ColumnElement>) aValue);
                            break;
                        case KeyElement.PRIMARY_KEY_IDX:
                            elem.setPrimaryKey((Boolean)aValue);
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
                    switch (columnIndex) {
                        case KeyElement.KEY_NAME_IDX:
                            return true;
                        case KeyElement.KEY_COLUMNS_IDX:
                            return false;
                        case KeyElement.PRIMARY_KEY_IDX:
                            return true;
                        default:
                            return false;
                    }
                }

                @Override
                protected Object getValueAtRow(final int rowIndex, final int columnIndex) {
                    KeyElement elem = (KeyElement)getElement(rowIndex);
                    switch (columnIndex) {
                        case KeyElement.KEY_NAME_IDX:
                            return elem.getName();
                        case KeyElement.KEY_COLUMNS_IDX:
                            return elem.getColumnsString();
                        case KeyElement.PRIMARY_KEY_IDX:
                            return elem.isPrimaryKey();
                        default:
                            break;
                    }
                    return null;
                }
            }, addButtonText, removeButtonText, removeAllButtonText);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        // Adapts column "Key Name"
        columnModel.getColumn(KeyElement.KEY_NAME_IDX).setCellEditor(new NameCellEditor());

        // Adapts column "Primary Key"
        columnModel.getColumn(KeyElement.PRIMARY_KEY_IDX).setMinWidth(PRIMARY_KEY_SIZE);
        columnModel.getColumn(KeyElement.PRIMARY_KEY_IDX).setMaxWidth(PRIMARY_KEY_SIZE);
        columnModel.getColumn(KeyElement.PRIMARY_KEY_IDX).setCellEditor(new RadioButtonCellEditor());
        columnModel.getColumn(KeyElement.PRIMARY_KEY_IDX).setCellRenderer(new RadioButtonCellRenderer());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onAddAction() {
        openDialog(-1);
    }

    /**
     * Opens a dialog to choose columns for the new key
     * @param rowIdx
     */
    private void openDialog(final int rowIdx) {
        // Only shows the dialog if at least one column is defined.
        if (isColumnAvailable()) {
            ColumnsDialog dialog = new ColumnsDialog(SwingUtilities.getWindowAncestor(this), rowIdx);
            dialog.initialize();
        }
    }

    /**
     * Returns true if columns are defined in Columns Tab, otherwise false. Show a message dialog if no column is
     * defined.
     *
     * @param settings the column settings
     * @return true if columns are defined in Columns Tab, otherwise false
     */
    private boolean isColumnAvailable() {
        // No column is defined in 'Columns' tab
        List<RowElement> colElems = getConfig().getRowElements(DBTableCreatorConfiguration.CFG_COLUMNS_SETTINGS);
        if (colElems.isEmpty()) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(getTable()),
                String.format("No column is defined in '%s' tab.", ColumnsPanel.DEFAULT_TITLE), "Information",
                JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        // There are columns defined in 'Columns' tab.
        return true;
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
            case KeyElement.KEY_NAME_IDX:
                return getNewIdentifierForColumn(KeyElement.KEY_NAME_IDX);
            case KeyElement.KEY_COLUMNS_IDX:
                return ""; // No default value
            case KeyElement.PRIMARY_KEY_IDX:
                return KeyElement.DEFAULT_PRIMARY_KEY_VALUE;
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RowElement createRowElement() {
        return null;
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
        if(getConfig().useDynamicSettings()) {
            return;
        }

        /* Only open dialog if dynamic settings is not selected */
        int colIdx = getTable().columnAtPoint(ev.getPoint());
        int rowIdx = getTable().rowAtPoint(ev.getPoint());
        int rowCount = getTableModel().getRowCount();

        // Make sure that the dialog is only opened when the user double clicks on
        // the right column and within the table
        if (ev.getButton() == MouseEvent.BUTTON1 && ev.getClickCount() == 2 && colIdx == KeyElement.KEY_COLUMNS_IDX
            && rowIdx > -1 && rowIdx < rowCount) {
            openDialog(rowIdx);
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
            if (getTable().getSelectedRowCount() == 1) {
                menu.addSeparator();
                createSetAsPrimaryKeyMenu(menu);
                menu.addSeparator();
                createChangeKeyColumnsMenu(menu);
            }
        }
    }

    /**
     * Creates table popup menu item to set the selected key as a primary key
     *
     * @param menu the parent table popup menu to attach the menu item
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
     * Creates table popup menu item to change key columns of the selected key
     *
     * @param menu the parent table popup menu to attach the menu item
     */
    private void createChangeKeyColumnsMenu(final JPopupMenu menu) {
        final JMenuItem item = new JMenuItem("Change key columns");
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                openDialog(getTable().getSelectedRow());
            }
        });
        menu.add(item);
    }

    /**
     * A helper method to set a key as primary key and on the same time set other keys as unique.
     * Only one primary key is allowed.
     *
     * @param index the index of the key to set as primary key
     */
    private void togglePrimaryKey(final int index) {
        // Set it to false if it is already a primary key
        if ((Boolean)getTableModel().getValueAt(index, KeyElement.PRIMARY_KEY_IDX)) {
            getTableModel().setValueAt(false, index, KeyElement.PRIMARY_KEY_IDX);
        } else {
            // Set it to true to indicate that it is a primary key
            // and on the same time set all other keys to false to indicate
            // that they are unique keys
            for (int i = 0; i < getTableModel().getRowCount(); i++) {
                if (i == index) {
                    getTableModel().setValueAt(true, i, KeyElement.PRIMARY_KEY_IDX);
                } else {
                    getTableModel().setValueAt(false, i, KeyElement.PRIMARY_KEY_IDX);
                }
            }
        }
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
    protected void createNoRowSelectedMenu(final JPopupMenu menu) {
        if(getConfig().useDynamicSettings()) {
            createNotEditableMenu(menu);
        } else {
            createSelectKeysContain(menu);
        }
    }

    /**
     * Creates table pop up menu item to select keys that contain the specified column
     *
     * @param menu the parent pop up menu to attach the menu items
     */
    private void createSelectKeysContain(final JPopupMenu menu) {
        final JMenu subMenu = new JMenu("Select keys contain column");
        menu.add(subMenu);

        Map<String, List<Integer>> columnKeyIdxMapping = new TreeMap<>();
        for (int i = 0; i < getTableModel().getRowCount(); i++) {
            KeyElement key = (KeyElement)getTableModel().getElement(i);
            for (DBColumn col : key.getColumns()) {
                final String colName = col.getName();
                List<Integer> indices = columnKeyIdxMapping.get(colName);
                if (indices == null) {
                    indices = new ArrayList<>();
                    indices.add(i);
                    columnKeyIdxMapping.put(colName, indices);
                } else {
                    indices.add(i);
                }
            }
        }

        for (Entry<String, List<Integer>> entry : columnKeyIdxMapping.entrySet()) {
            final JMenuItem item = new JMenuItem(entry.getKey());
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
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
     * Dialog to show all available columns to be associated with a key
     *
     * @author Budi Yanto, KNIME.com
     */
    private class ColumnsDialog extends JDialog {
        private static final long serialVersionUID = 1L;

        private static final String BORDER_TITLE = "Available Columns";

        private static final int DIALOG_DEFAULT_WIDTH = 300;

        private final JList<ColumnElement> m_list;

        private final DefaultListModel<ColumnElement> m_listModel;

        private final int m_rowIdx;

        /**
         * Constructor to create the dialog.
         *
         * @param parent the parent component
         * @param rowIdx the row index of the selected key in the table
         */
        private ColumnsDialog(final Window parent, final int rowIdx) {
            super(parent, "Select columns as Key", Dialog.ModalityType.APPLICATION_MODAL);
            final JPanel contentPanel = new JPanel(new BorderLayout());
            m_rowIdx = rowIdx;
            m_listModel = new DefaultListModel<>();
            List<RowElement> colElems = new ArrayList<>(
                    getConfig().getRowElements(DBTableCreatorConfiguration.CFG_COLUMNS_SETTINGS));
            Collections.sort(colElems, new Comparator<RowElement>() {
                @Override
                public int compare(final RowElement o1, final RowElement o2) {
                    final ColumnElement col1 = (ColumnElement) o1;
                    final ColumnElement col2 = (ColumnElement) o2;
                    return col1.getName().compareTo(col2.getName());
                }
            });
            for (RowElement elem : colElems) {
                m_listModel.addElement(((ColumnElement)elem));
            }

            m_list = new JList<>(m_listModel);
            m_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            final int visibleRowCount = Math.max(1, Math.min(m_list.getVisibleRowCount(), m_listModel.size()));
            m_list.setVisibleRowCount(visibleRowCount);
            m_list.setPreferredSize(new Dimension(DIALOG_DEFAULT_WIDTH, m_list.getPreferredSize().height));

            int[] selectedIndices = null;

            // rowIdx is bigger than -1 which means that the dialog opens because
            // an existing key is selected, so select all columns that are
            // associated with that key
            if (rowIdx > -1) {
                KeyElement selKey = (KeyElement)getTableModel().getElement(rowIdx);
                selectedIndices = new int[selKey.getColumns().size()];
                int idx = 0;
                for (DBColumn col : selKey.getColumns()) {
                    selectedIndices[idx++] = m_listModel.indexOf(ColumnElement.createColumnElement(col));
                }
            } else {
                // rowIdx is smaller than 0 which means that the dialog opens
                // because a new key is added, so only select the first column
                selectedIndices = new int[]{0};
            }

            m_list.setSelectedIndices(selectedIndices);

            final JScrollPane scrollPane = new JScrollPane(m_list);
            scrollPane.setBorder(BorderFactory.createTitledBorder(null, BORDER_TITLE, TitledBorder.CENTER,
                TitledBorder.DEFAULT_POSITION));
            contentPanel.add(scrollPane, BorderLayout.CENTER);

            final JPanel buttonsPanel = new JPanel(new FlowLayout());
            final JButton okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    onOK();
                }
            });
            final JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    setVisible(false);
                    dispose();
                }
            });

            buttonsPanel.add(okButton);
            buttonsPanel.add(cancelButton);

            contentPanel.add(buttonsPanel, BorderLayout.SOUTH);
            getContentPane().add(contentPanel);

        }

        /**
         * Initializes the dialog.
         */
        private void initialize() {
            pack();
            setLocationRelativeTo(getParent());
            setVisible(true);
        }

        private void onOK() {
            Set<ColumnElement> selectedColumns = new HashSet<>(m_list.getSelectedValuesList());

            // No column is selected, show error popup
            if (selectedColumns.isEmpty()) {
                JOptionPane.showMessageDialog(ColumnsDialog.this, "At least one column must be selected as key.",
                    "No Column Selected Error", JOptionPane.ERROR_MESSAGE);
            } else { // At least one column is selected
                KeyElement elem = null;
                if (m_rowIdx < 0) { // New key
                    elem = new KeyElement((String)getDefaultValue(KeyElement.KEY_NAME_IDX), selectedColumns,
                        (Boolean)getDefaultValue(KeyElement.PRIMARY_KEY_IDX));

                    addRowElement(elem);
                } else { // Existing key, update the columns associated with that key
                    elem = (KeyElement)getTableModel().getElement(m_rowIdx);
                    getTableModel().setValueAt(selectedColumns, m_rowIdx, KeyElement.KEY_COLUMNS_IDX);
                }

                setVisible(false);
                dispose();
            }
        }
    }
}
