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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   11.02.2011 (hofer): created
 */
package org.knime.core.node.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

/**
 * A visual component to define key - value pairs. Both key and value are
 * of type string.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("serial")
public class KeyValuePanel extends ConfigTablePanel {
    private KeyValueTableModel m_model;

    /**
     * Create a new instance.
     */
    public KeyValuePanel() {
        super(new KeyValueTableModel());
        JTable table = getTable();
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        m_model = (KeyValueTableModel)getModel();


        KeyStroke ctrlV = KeyStroke.getKeyStroke(KeyEvent.VK_V,
                KeyEvent.CTRL_MASK);
        table.getInputMap().put(ctrlV, "TablePaste");
        table.getActionMap().put("TablePaste",
                new PasteAction(this));

        KeyStroke ctrlC = KeyStroke.getKeyStroke(KeyEvent.VK_V,
                KeyEvent.CTRL_MASK);
        table.getInputMap().put(ctrlC, "TableCopy");
        table.getActionMap().put("TableCopy",
                new CopyAction(this));

        table.addMouseListener(new TableMouseAdapter(this));
    }

    /**
     * Update the data.
     *
     * @param keys the elements of the first column
     * @param values the elements of the second column
     */
    public void setTableData(final String[] keys,
            final String[] values) {
        m_model.setTableData(keys, values);
    }

    /**
     * Get the elements of the first column.
     *
     * @return the elements of the first column
     */
    public String[] getKeys() {
        commitOpenEditor();
        return m_model.getKeys();
    }

    /**
     * Get the elements of the second column.
     *
     * @return the elements of the second column
     */
    public String[] getValues() {
        commitOpenEditor();
        return m_model.getValues();
    }



    /**
     * Set the label of the first column.
     *
     * @param label the label to set
     */
    public void setKeyColumnLabel(final String label) {
        m_model.setKeyColumnLabel(label);
    }

    /**
     * Set the label of the second column.
     *
     * @param label the label to set
     */
    public void setValueColumnLabel(final String label) {
        m_model.setValueColumnLabel(label);
    }

    private static class KeyValueTableModel extends AbstractTableModel
            implements ConfigTableModel {
        private final List<String> m_keys;
        private final List<String> m_values;
        private String m_keyColumnLabel;
        private String m_valueColumnLabel;

        public KeyValueTableModel() {
            m_keys = new ArrayList<String>();
            m_values = new ArrayList<String>();
            m_keyColumnLabel = "key";
            m_valueColumnLabel = "value";
        }

        /**
         * Append a row to the table.
         */
        @Override
        public void addRow() {
            int row = m_values.size();
            m_keys.add("");
            m_values.add("");
            fireTableRowsInserted(row, row);
        }


        /**
         * Remove given rows.
         *
         * @param rows the rows to be removed
         */
        @Override
        public void removeRows(final int[] rows) {
            Arrays.sort(rows);
            for (int i = rows.length - 1; i >= 0; i--) {
                m_keys.remove(rows[i]);
                m_values.remove(rows[i]);
                fireTableRowsDeleted(rows[i], rows[i]);
            }
        }


        /**
         * Update the data.
         *
         * @param keys the elements of the first column
         * @param values the elements of the second column
         */
        public void setTableData(final String[] keys, final String[] values) {
            m_keys.clear();
            m_keys.addAll(Arrays.asList(keys));
            m_values.clear();
            m_values.addAll(Arrays.asList(values));
            fireTableDataChanged();
        }


        /**
         * Get the elements of the first column.
         * @return the elements of the first column
         */
        public String[] getKeys() {
            return m_keys.toArray(new String[m_values.size()]);
        }

        /**
         * Get the elements of the second column.
         *
         * @return the elements of the second column
         */
        public String[] getValues() {
            return m_values.toArray(new String[m_values.size()]);
        }

        /**
         * Set the label of the first column.
         *
         * @param label the label to set
         */
        void setKeyColumnLabel(final String label) {
            m_keyColumnLabel = label;
            fireTableStructureChanged();
        }

        /**
         * Set the label of the second column.
         *
         * @param label the label to set
         */
        void setValueColumnLabel(final String label) {
            m_valueColumnLabel = label;
            fireTableStructureChanged();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            return 2;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(final int column) {
            switch (column) {
            case 0:
                return m_keyColumnLabel;
            case 1:
                return m_valueColumnLabel;

            default:
                return "Unknown";
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return m_values.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(final int row, final int col) {
            switch (col) {
            case 0:
                return m_keys.get(row);
            case 1:
                return m_values.get(row);

            default:
                throw new IllegalStateException("This is a programming error.");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCellEditable(final int rowIndex,
                final int columnIndex) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValueAt(final Object aValue,
                final int row, final int col) {
            switch (col) {
            case 0:
                m_keys.set(row, aValue.toString());
                fireTableRowsUpdated(row, row);
                break;
            case 1:
                m_values.set(row, aValue.toString());
                fireTableRowsUpdated(row, row);
                break;
            default:
                throw new IllegalStateException("This is a programming error.");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ConfigTableModel cloneModel() {
            KeyValueTableModel tempModel = new KeyValueTableModel();
            tempModel.setTableData(getKeys(), getValues());
            return tempModel;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setTableData(final ConfigTableModel tempModel) {
            KeyValueTableModel model = (KeyValueTableModel)tempModel;
            setTableData(model.getKeys(), model.getValues());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear() {
            setTableData(new String[0], new String[0]);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setColumnName(final int index, final String value) {
            if (index == 0) {
                setKeyColumnLabel(value);
            } else {
                setValueColumnLabel(value);
            }

        }

    }


    /**
     * The swing action for copying the selected cells to the system clipboard.
     *
     * @author Heiko Hofer
     */
    static class CopyAction extends AbstractAction {
        private final ConfigTablePanel m_panel;

        /**
         * Creates a new instance.
         *
         * @param panel the 'model' for this action
         */
        CopyAction(final ConfigTablePanel panel) {
            super("Copy");
            m_panel = panel;
            final JTable table = m_panel.getTable();
            table.getSelectionModel().addListSelectionListener(
                    new ListSelectionListener() {
                @Override
                public void valueChanged(final ListSelectionEvent e) {
                    setEnabled(!table.getSelectionModel()
                            .isSelectionEmpty());
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            JTable table = m_panel.getTable();
            int[] rows = table.getSelectedRows();
            int numCols = table.getColumnCount();

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < rows.length; i++) {
                for (int k = 0; k < numCols; k++) {
                    Object value = table.getValueAt(rows[i], k);
                    builder.append(value.toString());

                    if (k < numCols - 1) {
                        builder.append("\t");
                    }
                }
                builder.append("\n");
            }
            StringSelection str  = new StringSelection(builder.toString());
            Clipboard clipboard = Toolkit.getDefaultToolkit().
                getSystemClipboard();
            clipboard.setContents(str, str);
        }

    }

    /**
     * The swing action to insert cells from the system clipboard.
     *
     * @author Heiko Hofer
     */
    static class PasteAction extends AbstractAction {
        private final ConfigTablePanel m_panel;
        private int m_endRow;
        private int m_endCol;
        private ConfigTableModel m_tempModel;

        /**
         * Creates a new instance.
         *
         * @param panel the 'model' for this action
         */
        PasteAction(final ConfigTablePanel panel) {
            super("Paste");
            m_panel = panel;
            final JTable table = m_panel.getTable();
            table.getSelectionModel().addListSelectionListener(
                    new ListSelectionListener() {
                @Override
                public void valueChanged(final ListSelectionEvent e) {
                    setEnabled(!table.getSelectionModel().
                            isSelectionEmpty());
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            JTable table = m_panel.getTable();
            Clipboard clipboard = Toolkit.getDefaultToolkit().
                getSystemClipboard();
            int startRow = table.getSelectionModel().getAnchorSelectionIndex();
            int startCol = 0;
            m_endRow = startRow;
            m_endCol = startCol;
            m_tempModel = m_panel.getModel().cloneModel();

            try {
                String trstring =
                        (String)(clipboard.getContents(this)
                                .getTransferData(DataFlavor.stringFlavor));
                StringTokenizer rows = new StringTokenizer(trstring, "\n",
                        true);
                for (int i = 0; rows.hasMoreTokens(); i++) {
                    String row = rows.nextToken();
                    if (!row.equals("\n")) {
                        StringTokenizer cells = new StringTokenizer(row, "\t",
                                true);
                        for (int j = 0; cells.hasMoreTokens(); j++) {
                            String value = cells.nextToken();
                            if (!value.equals("\t")) {
                                setValueAt(value, startRow + i, startCol + j);
                                if (cells.hasMoreTokens()) {
                                    cells.nextToken();
                                    // When row ends with a delimiter
                                    if (!cells.hasMoreTokens()) {
                                        setValueAt("", startRow + i,
                                                startCol + j + 1);
                                    }
                                }
                            } else { // an empty cell
                                setValueAt("", startRow + i, startCol + j);
                                // When row ends with a delimiter
                                if (!cells.hasMoreTokens()) {
                                    setValueAt("", startRow + i,
                                            startCol + j + 1);
                                }
                            }
                        }
                        if (rows.hasMoreTokens()) {
                            rows.nextToken();
                        }
                    } else { // an empty row
                        while (startRow + i >= m_tempModel.getRowCount()) {
                            m_tempModel.addRow();
                        }
                    }
                }

                m_panel.getModel().setTableData(m_tempModel);

                m_tempModel.clear();
                m_tempModel = null;
                table.getSelectionModel().setSelectionInterval(startRow,
                        m_endRow);
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex.getMessage());
            }
        }

        private void setValueAt(final Object value, final int row,
                final int col) {
            while (row >= m_tempModel.getRowCount()) {
                m_tempModel.addRow();
            }
            if (col < m_tempModel.getColumnCount()) {
                m_endRow = Math.max(m_endRow, row);
                m_endCol = Math.max(m_endCol, col);
                m_tempModel.setValueAt(value, row, col);
            }
        }
    }

    /**
     * Shows popup menu of the table.
     *
     * @author Heiko Hofer
     */
    private static class TableMouseAdapter extends MouseAdapter {
        private final JTable m_table;
        private final JPopupMenu m_popup;

        /**
         * @param panel
         */
        TableMouseAdapter(final ConfigTablePanel panel) {
            m_table = panel.getTable();
            m_popup = new JPopupMenu();
            m_popup.add(new CopyAction(panel));
            m_popup.add(new PasteAction(panel));

            m_table.add(m_popup);
        }

        private void showPopup(final MouseEvent e) {
            int row = m_table.rowAtPoint(e.getPoint());
            int col = m_table.columnAtPoint(e.getPoint());
            if (row == -1 || col == -1) {
                return;
            }
            // click in selection
            if (m_table.getSelectionModel().isSelectedIndex(row)
                  && m_table.getColumnModel().getSelectionModel()
                      .isSelectedIndex(col)) {
                m_popup.show(m_table, e.getX(), e.getY());
            } else {
                if (!(e.isControlDown() || e.isShiftDown())) {
                    m_table.getSelectionModel().setSelectionInterval(
                            row, row);
                    m_table.getColumnModel().getSelectionModel()
                        .setSelectionInterval(col, col);
                    m_popup.show(m_table, e.getX(), e.getY());
                }
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseReleased(final MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

    }
}
