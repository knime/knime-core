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
 *   Dec 3, 2015 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Budi Yanto, KNIME.com
 */
abstract class AbstractTablePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(AbstractTablePanel.class);

    /** Default text for the add button */
    private static final String ADD_BUTTON_TEXT = "Add";

    /** Default text for the remove button */
    private static final String REMOVE_BUTTON_TEXT = "Remove";

    /** Default text for the remove all button*/
    private static final String REMOVE_ALL_BUTTON_TEXT = "Remove All";

    /** the key of the table panel */
    private final String m_key;

    /** the title of the table penal */
    private final String m_title;

    /** the underlying jtable component */
    private final JTable m_table;

    /** the add button */
    private final JButton m_addButton;

    /** the remove button */
    private final JButton m_removeButton;

    /** the remove all button */
    private final JButton m_removeAllButton;

    /** the underlying table model for the jtable component */
    private final TableCreatorTableModel m_tableModel;

    /** the configuration */
    private final DBTableCreatorConfiguration m_config;

    /**
     * Creates a new instance of AbstractTablePanel
     * @param key
     * @param tableModel
     * @param addButtonText
     * @param removeButtonText
     */
    AbstractTablePanel(final String key, final String title, final DBTableCreatorConfiguration config,
        final TableCreatorTableModel tableModel, String addButtonText, String removeButtonText,
        String removeAllButtonText) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }

        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty");
        }

        if (addButtonText == null || addButtonText.isEmpty()) {
            addButtonText = ADD_BUTTON_TEXT;
        }

        if (removeButtonText == null || removeButtonText.isEmpty()) {
            removeButtonText = REMOVE_BUTTON_TEXT;
        }

        if (removeAllButtonText == null || removeAllButtonText.isEmpty()) {
            removeAllButtonText = REMOVE_ALL_BUTTON_TEXT;
        }

        setLayout(new BorderLayout());
        m_key = key;
        m_title = title;
        m_config = config;
        m_tableModel = tableModel;
        m_table = new JTable(m_tableModel);
        m_table.setFillsViewportHeight(true);
        m_table.getTableHeader().setReorderingAllowed(false);
        m_table.addMouseListener(new TableListener());
        final JScrollPane scrollPane = new JScrollPane(m_table);

        final JPanel buttonsPanel = new JPanel(new FlowLayout());
        m_addButton = new JButton(addButtonText);
        m_addButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                onAdd();
            }
        });

        m_removeButton = new JButton(removeButtonText);
        m_removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                onRemove();
            }
        });

        m_removeAllButton = new JButton(removeAllButtonText);
        m_removeAllButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                onRemoveAll();
            }
        });

        buttonsPanel.add(m_addButton);
        buttonsPanel.add(m_removeButton);
        buttonsPanel.add(m_removeAllButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        adaptTableColumnModel(getTable().getColumnModel());
    }

    /**
     * Revalidates table
     */
    public void revalidateTable() {
        m_table.revalidate();
    }

    /**
     * @return the key of this table panel
     */
    protected String getKey() {
        return m_key;
    }

    /**
     * @return the title of this panel
     */
    public String getTitle() {
        return m_title;
    }

    /**
     * @return the configuration
     */
    protected DBTableCreatorConfiguration getConfig() {
        return m_config;
    }

    /**
     * @return the underlying jtable component
     */
    protected JTable getTable() {
        return m_table;
    }

    /**
     * @return the table model
     */
    protected TableCreatorTableModel getTableModel() {
        return m_tableModel;
    }

    /**
     * Returns a new identifier of the given column
     *
     * @param columnIndex index of the column
     * @return a new identifier of the given column
     */
    protected Object getNewIdentifierForColumn(final int columnIndex) {

        Set<String> nameHash = new HashSet<>();
        for (int idx = 0; idx < getTableModel().getRowCount(); idx++) {
            final String name = getTableModel().getValueAt(idx, columnIndex).toString();
            nameHash.add(name);
        }

        int idx = 1;
        final String prefix = DBTableCreatorConfiguration.getPrefix(m_key);
        String newIdentifier = prefix + idx;
        while (!nameHash.add(newIdentifier)) {
            newIdentifier = prefix + (++idx);
        }

        return newIdentifier;
    }

    /**
     * Stop all cell editing of the table.
     *
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected void stopCellEditing() throws InvalidSettingsException {
        if (getTable().isEditing() && getTable().getCellEditor() != null) {
            boolean success = getTable().getCellEditor().stopCellEditing();
            if (!success) {
                throw new InvalidSettingsException("Some settings are invalid. Please check it again.");
            }
        }
    }

    /**
     * Adds a new row element to the table
     *
     * @param element the element to add
     */
    protected void addRowElement(final RowElement element) {
        if (element == null) {
            return;
        }

        getTableModel().add(element);

        highlightRow(getTableModel().getRowCount() - 1);

        if (!isEnabledRemoveButton()) {
            setEnabledRemoveButton(true);
        }

    }

    /**
     * Highlights the row on the given index
     *
     * @param index index of the row to highlight
     */
    private void highlightRow(final int index) {
        if (index > -1) {
            final ListSelectionModel selectionModel = getTable().getSelectionModel();
            if (selectionModel != null) {
                //select the fresh added rows
                selectionModel.setSelectionInterval(index, index);
                //scroll first selected row into view
                getTable().scrollRectToVisible(new Rectangle(getTable().getCellRect(index, 0, true)));
            }
        }
    }

    /**
     * Invokes actions if the add button is clicked.
     */
    private void onAdd() {
        try{
            stopCellEditing();
        } catch(InvalidSettingsException ex){
            LOGGER.debug(ex.getMessage());
        }

        onAddAction();
    }

    /**
     * Invokes actions if the add button is clicked.
     * Subclass should override this method if another action is needed.
     */
    protected void onAddAction(){
        RowElement elem = createRowElement();
        addRowElement(elem);
    }

    /**
     * Invokes actions if the remove button is clicked.
     */
    private void onRemove() {
        try{
            stopCellEditing();
        } catch(InvalidSettingsException ex){
            LOGGER.debug(ex.getMessage());
        }

        onRemoveAction();
    }

    /**
     * Invokes actions if the remove button is clicked.
     * Subclass should override this method if another action is needed.
     */
    protected void onRemoveAction(){
        int[] selectedRows = getTable().getSelectedRows();
        getTableModel().remove(selectedRows);
        if (getTableModel().isEmpty()) {
            setEnabledRemoveButton(false);
        } else if (selectedRows.length == 1) {
            final int idx = selectedRows[0];
            if (idx == 0) {
                highlightRow(idx);
            } else {
                highlightRow(idx - 1);
            }
        } else {
            highlightRow(getTableModel().getRowCount() - 1);
        }
    }

    /**
     * Invokes actions if the removeAll button is clicked.
     */
    private void onRemoveAll() {
        try{
            stopCellEditing();
        } catch(InvalidSettingsException ex){
            LOGGER.debug(ex.getMessage());
        }

        onRemoveAllAction();
    }

    /**
     * Invokes actions if the removeAll button is clicked.
     * Subclass should override this method if another action is needed.
     */
    protected void onRemoveAllAction(){
        getTableModel().removeAll();
        setEnabledRemoveButton(false);
    }

    /**
     * Enables or disables the add button
     * @param isEnabled <code>true</code> if the add button should be enabled, otherwise <code>false</code>
     */
    public void setEnabledAddButton(final boolean isEnabled) {
        m_addButton.setEnabled(isEnabled);
    }

    /**
     * Enables or disables the remove and removeAll button
     * @param isEnabled <code>true</code> if the remove and removeAll button should be enabled, otherwise <code>false</code>
     */
    public void setEnabledRemoveButton(final boolean isEnabled) {
        m_removeButton.setEnabled(isEnabled);
        m_removeAllButton.setEnabled(isEnabled);
    }

    /**
     * @return <code>true</code> if the add button is enabled, otherwise <code>false</code>
     */
    protected boolean isEnabledAddButton() {
        return m_addButton.isEnabled();
    }

    /**
     * @return <code>true</code> if the remove and removeAll button is enabled, otherwise <code>false</code>
     */
    protected boolean isEnabledRemoveButton() {
        return m_removeButton.isEnabled() && m_removeAllButton.isEnabled();
    }

    /**
     * Returns true it the table is empty
     *
     * @return true if the table is empty, otherwise false
     */
    protected boolean isEmpty() {
        return getTableModel().isEmpty();
    }

    /**
     * Returns the number of rows in the table
     *
     * @return the number of rows in the table
     */
    protected int getRowCount() {
        return getTableModel().getRowCount();
    }

    /**
     * Adapts the table column model
     *
     * @param columnModel the column model to adapt
     */
    protected abstract void adaptTableColumnModel(final TableColumnModel columnModel);

    /**
     * Returns the default value of the given column
     *
     * @param columnIndex the index of the column
     * @return the default value of the given column
     */
    protected abstract Object getDefaultValue(final int columnIndex);

    /**
     * Creates a new row element
     *
     * @return a new row element
     */
    protected abstract RowElement createRowElement();

    /**
     * Invokes actions if the table is clicked
     *
     * @param ev
     */
    protected abstract void onClick(final MouseEvent ev);

    /**
     * Creates menu items for the given pop up menu
     *
     * @param menu the pop up menu to be created
     */
    protected abstract void createTablePopupMenu(final JPopupMenu menu);

    /**
     * Creates menu items for the given pop up menu if no row is selected.
     *
     * @param menu the pop up menu to attach the menu items
     */
    protected abstract void createNoRowSelectedMenu(final JPopupMenu menu);

    /**
     * Actions invoked during load
     */
    public void onLoad() {
        if (getTableModel().isEmpty()) {
            setEnabledRemoveButton(false);
        } else {
            setEnabledRemoveButton(true);
        }
    }

    /**
     * Actions invoked during save
     *
     * @throws InvalidSettingsException
     */
    public void onSave() throws InvalidSettingsException {
        stopCellEditing();
    }

    /**
     * Creates pop up menu item to select all columns in the table.
     *
     * @param menu the pop up menu to attach the menu items
     */
    private JPopupMenu createSelectAllMenu() {
        JPopupMenu menu = new JPopupMenu();
        final JMenuItem item = new JMenuItem("Select all");
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                getTable().selectAll();
            }
        });

        menu.add(item);
        return menu;
    }

    /**
     * Creates pop up menu item if no row is available
     *
     * @param menu the pop up menu to attach the menu items
     */
    protected void createNoRowAvailableMenu(final JPopupMenu menu) {
        final JMenuItem item = new JMenuItem("No row is available");
        item.setEnabled(false);
        menu.add(item);
    }

    /**
     * Creates pop up menu item if not editable
     *
     * @param menu the pop up menu to attach the menu items
     */
    protected void createNotEditableMenu(final JPopupMenu menu) {
        menu.removeAll();
        final JMenuItem item = new JMenuItem("Not editable");
        item.setEnabled(false);
        menu.add(item);
    }

    /**
     * A listener class to handle user clicks.
     */
    protected class TableListener extends MouseAdapter {
        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseClicked(final MouseEvent ev) {
            onClick(ev);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent ev) {
            maybeShowContextMenu(ev);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseReleased(final MouseEvent ev) {
            maybeShowContextMenu(ev);
        }

        /**
         * Maybe shows context menu depending on the data in the table
         *
         * @param ev a MouseEvent that triggers the context menu
         */
        private void maybeShowContextMenu(final MouseEvent ev) {
            if (ev.isPopupTrigger()) {
                final JPopupMenu menu;
                //the table contains no rows
                if (getTableModel().isEmpty()) {
                    menu = new JPopupMenu();
                    createNoRowAvailableMenu(menu);
                } else if (getTable().getSelectedRowCount() == 0) { // No row is selected
                    menu = createSelectAllMenu();
                    createNoRowSelectedMenu(menu);
                } else {
                    menu = createSelectAllMenu();
                    createTablePopupMenu(menu);
                }
                if (menu != null) {
                    menu.show(ev.getComponent(), ev.getX(), ev.getY());
                }
            }
        }
    }
}
