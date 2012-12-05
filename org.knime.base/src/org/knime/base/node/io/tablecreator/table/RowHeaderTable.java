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
 *   04.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator.table;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

/**
 * The table that is the row header of a spreadsheet.
 *
 * @author Heiko Hofer
 */
class RowHeaderTable extends JTable {
    private static final long serialVersionUID = 4830133852530795086L;
    private RowHeaderRenderer m_renderer;

    /**
     * Creates a new instance.
     * @param table the spreadsheet
     */
    public RowHeaderTable(final SpreadsheetTable table) {
        super();
        TableModel model = new RowModel(table.getModel());
        setModel(model);

        m_renderer = new RowHeaderRenderer(table);
        setDefaultRenderer(Object.class, m_renderer);

        setIntercellSpacing(new Dimension(0, 0));

        setPreferredWidth();
        setRowHeight(table.getRowHeight());

        setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        addMouseListener(
                new RowHeaderMouseAdapter(table, this));
        setSelectionModel(table.getSelectionModel());
        setFocusable(false);
        model.addTableModelListener(new TableModelListener() {
            public void tableChanged(final TableModelEvent e) {
                setPreferredWidth();
                JScrollPane sp = (JScrollPane)
                    SwingUtilities.getAncestorOfClass(JScrollPane.class,
                            RowHeaderTable.this);
                if (null != sp) {
                    // Causes adjustment of the size
                    sp.setRowHeaderView(RowHeaderTable.this);
                }
            }
        });
    }

    private void setPreferredWidth() {
        Component comp = m_renderer.getTableCellRendererComponent(this,
                getRowModel().getValueAt(getRowCount(), 0), false, false,
                getRowCount() - 1, 0);

        Dimension d = getPreferredScrollableViewportSize();
        d.width = comp.getPreferredSize().width + 5;
        setPreferredScrollableViewportSize(d);
    }

    /**
     * Returns the model of this table.
     * @return the model of this table.
     */
    RowModel getRowModel() {
        return (RowModel)getModel();
    }

    /**
     * Define whether the output table should be highlighted.
     *
     * @param show true when the output table should be highlighted.
     */
    void showOutputTable(final boolean show) {
        m_renderer.showOutputTable(show);
    }

    /**
     * The cell renderer of the RowHeaderTable
     *
     * @author Heiko Hofer
     */
    private static class RowHeaderRenderer extends HeaderRenderer {
        private static final long serialVersionUID = -3557222901902686106L;
        private SpreadsheetTable m_table;

        /**
         * @param table the speadsheet
         */
        RowHeaderRenderer(final SpreadsheetTable table) {
            m_table = table;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean isInOutputTable(final int row, final int column) {
            SpreadsheetTableModel model =
                (SpreadsheetTableModel)m_table.getModel();
            int maxRow = model.getMaxRow();
            int maxColumn = model.getMaxColumn();
            return !(row >= maxRow || column >= maxColumn);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void applyDefaults() {
            super.applyDefaults();
            setHorizontalAlignment(LEFT);
        }

    }

    /**
     * Controls selection and displays the popup menu.
     *
     * @author Heiko Hofer
     */
    private static class RowHeaderMouseAdapter extends MouseAdapter {
        private RowHeaderTable m_rowHeaderTable;
        private SpreadsheetTable m_table;
        private JPopupMenu m_popup;

        /**
         * @param table the spreadsheet
         * @param rowHeaderTable the table which is the row header
         * of the spreadsheet.
         */
        RowHeaderMouseAdapter(final SpreadsheetTable table,
                final RowHeaderTable rowHeaderTable) {
            m_table = table;
            m_rowHeaderTable = rowHeaderTable;
            m_popup = new JPopupMenu();
            m_popup.add(new PropertyRowsAction(m_rowHeaderTable));
            m_popup.add(new JSeparator());
            m_popup.add(new InsertRowsAction(m_table));
            m_popup.add(new DeleteRowsAction(m_table));
            m_table.add(m_popup);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            // Commit edit, otherwise strange behavior can happen
            TableCellEditor editor = m_table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
            // Use e.isPopupTriggger for Linux und OSX.
            // One windows the popup is usually displayed on mouseReleased
            // event. To support this we would have to use platform specific
            // code.
            boolean isPopupTrigger = e.isPopupTrigger()
                || SwingUtilities.isRightMouseButton(e);

            if (!isPopupTrigger) {
                int row = m_rowHeaderTable.rowAtPoint(e.getPoint());

                if (m_table.getSelectionModel().isSelectionEmpty()) {
                    m_table.getSelectionModel().setSelectionInterval(row, row);
                }
                if (m_table.getSelectionModel().getMinSelectionIndex() == row
                        && m_table.getSelectionModel().getMaxSelectionIndex()
                            == row
                        && e.isControlDown()) {
                    m_table.getSelectionModel().clearSelection();
                    m_table.getColumnModel().getSelectionModel()
                        .clearSelection();
                    return;
                }
                m_table.getColumnModel().getSelectionModel()
                        .setSelectionInterval(m_table.getColumnCount() - 1, 0);
            } else {
                m_popup.show(m_rowHeaderTable, e.getX(), e.getY());
            }

        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseReleased(final MouseEvent e) {
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() == 2) {
                new PropertyRowsAction(m_rowHeaderTable).actionPerformed(null);
            }
        }
    }

    /**
     * The table model of the row header table.
     *
     * @author Heiko Hofer
     */
    static class RowModel extends AbstractTableModel {
        private static final long serialVersionUID = 7706913916654672652L;
        private TableModel m_model;
        private String m_rowIdPrefix = "Row";
        private String m_rowIdSuffix = "";
        private int m_rowIdStartValue = 1;

        /**
         * @param model the model of the spreadsheet
         */
        RowModel(final TableModel model) {
            this.m_model = model;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCellEditable(final int rowIndex,
                final int columnIndex) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return Object.class;
        }

        /**
         * {@inheritDoc}
         */
        public int getColumnCount() {
            return 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(final int columnIndex) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return m_model.getRowCount();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValueAt(final Object aValue, final int rowIndex,
                final int columnIndex) {
            // do nothing, table is not editable
        }

        /**
         * {@inheritDoc}
         */
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            StringBuilder rowId = new StringBuilder();
            rowId.append(m_rowIdPrefix);
            rowId.append(Integer.toString(rowIndex + m_rowIdStartValue));
            rowId.append(m_rowIdSuffix);
            return rowId.toString();
        }

        /**
         * @return the rowIdPrefix
         */
        public final String getRowIdPrefix() {
            return m_rowIdPrefix;
        }
        /**
         * @param rowIdPrefix the rowIdPrefix to set
         */
        public final void setRowIdPrefix(final String rowIdPrefix) {
            m_rowIdPrefix = rowIdPrefix;
            fireTableRowsUpdated(0, getRowCount() - 1);
        }
        /**
         * @return the rowIdSuffix
         */
        public final String getRowIdSuffix() {
            return m_rowIdSuffix;
        }
        /**
         * @param rowIdSuffix the rowIdSuffix to set
         */
        public final void setRowIdSuffix(final String rowIdSuffix) {
            m_rowIdSuffix = rowIdSuffix;
            fireTableRowsUpdated(0, getRowCount() - 1);
        }
        /**
         * @return the rowIdStartValue
         */
        public final int getRowIdStartValue() {
            return m_rowIdStartValue;
        }
        /**
         * @param rowIdStartValue the rowIdStartValue to set
         */
        public final void setRowIdStartValue(final int rowIdStartValue) {
            m_rowIdStartValue = rowIdStartValue;
            fireTableRowsUpdated(0, getRowCount() - 1);
        }
    }

}
