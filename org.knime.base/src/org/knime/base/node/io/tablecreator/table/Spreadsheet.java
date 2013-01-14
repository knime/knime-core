/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   02.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator.table;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.knime.base.node.io.filereader.ColProperty;
import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;

/**
 * The spreadsheet component with an input line in the north a spreadsheet
 * table in the center and further controls in the south.
 *
 * @author Heiko Hofer
 */
public class Spreadsheet extends JComponent {
    private final SpreadsheetTable m_table;
    private final RowHeaderTable m_rowHeaderTable;
    private final JTextField m_inputLine;
    private JCheckBox m_highlightOutputTable;
    private JLabel m_outputTableMessage;
    private final JScrollPane m_scrollPane;

    /**
     * Create a new instance.
     */
    public Spreadsheet() {
        m_inputLine = new JTextField();

        setLayout(new BorderLayout());

        m_table = new SpreadsheetTable();
        m_scrollPane = new JScrollPane(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(m_scrollPane, BorderLayout.CENTER);


        m_scrollPane.setViewportView(m_table);
        m_scrollPane.setColumnHeaderView(m_table.getTableHeader());
        m_rowHeaderTable = new RowHeaderTable(m_table);
        m_scrollPane.setRowHeaderView(m_rowHeaderTable);

        JPanel cornerPanel = new JPanel();
        cornerPanel.setFocusable(false);
        cornerPanel.addMouseListener(new MouseAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void mousePressed(final MouseEvent e) {
                m_table.getSelectionModel().setSelectionInterval(
                        m_table.getRowCount() - 1, 0);
                m_table.getColumnModel().getSelectionModel()
                    .setSelectionInterval(m_table.getColumnCount() - 1, 0);
            }
        });
        m_scrollPane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER,
                cornerPanel);

        add(createEditingPanel(), BorderLayout.NORTH);
        add(createOutputTableInfoPanel(), BorderLayout.SOUTH);

        OutputTableController outputTableContr = new OutputTableController();
        m_table.getModel().addTableModelListener(outputTableContr);
        AutoColumnHeaderController autoHeaderContr =
            new AutoColumnHeaderController();
        m_table.getModel().addTableModelListener(autoHeaderContr);

        InputLineController inputLineContr = new InputLineController();
        m_table.addPropertyChangeListener(inputLineContr);
        m_table.getModel().addTableModelListener(inputLineContr);
        m_table.getDefaultEditor(Object.class).addCellEditorListener(
                inputLineContr);
        m_inputLine.addKeyListener(inputLineContr);
        m_inputLine.addActionListener(inputLineContr);

        m_table.getEditorTextField().addKeyListener(inputLineContr);

        KeyStroke ctrlX = KeyStroke.getKeyStroke(KeyEvent.VK_X,
                KeyEvent.CTRL_MASK);
        m_table.getInputMap().put(ctrlX, "SpreadsheetCut");
        m_table.getActionMap().put("SpreadsheetCut",
                new CutAction(m_table));

        KeyStroke ctrlC = KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyEvent.CTRL_MASK);
        m_table.getInputMap().put(ctrlC, "SpreadsheetCopy");
        m_table.getActionMap().put("SpreadsheetCopy",
                new CopyAction(m_table));

        KeyStroke ctrlV = KeyStroke.getKeyStroke(KeyEvent.VK_V,
                KeyEvent.CTRL_MASK);
        m_table.getInputMap().put(ctrlV, "SpreadsheetPaste");
        m_table.getActionMap().put("SpreadsheetPaste",
                new PasteAction(m_table));

        KeyStroke ctrlA = KeyStroke.getKeyStroke(KeyEvent.VK_A,
                KeyEvent.CTRL_MASK);
        m_table.getInputMap().put(ctrlA, "SpreadsheetSelectAll");
        m_table.getActionMap().put("SpreadsheetSelectAll",
                new SelectAllAction(m_table));

        KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        m_table.getInputMap().put(delete, "SpreadsheetDelete");
        m_table.getActionMap().put("SpreadsheetDelete",
                new DeleteAction(m_table));
        m_table.getTableHeader().getInputMap().put(delete, "SpreadsheetDelete");
        m_table.getTableHeader().getActionMap().put("SpreadsheetDelete",
                new DeleteAction(m_table));
    }

    private JPanel createEditingPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(4, 2, 4, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Input line: "), c);
        c.gridx++;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.add(m_inputLine, c);
        return p;
    }

    private JPanel createOutputTableInfoPanel() {
        JPanel p = new JPanel(new BorderLayout());

        m_outputTableMessage = new JLabel();
        m_outputTableMessage.setText(getOutputTableMessage(0,0,0));
        p.add(m_outputTableMessage, BorderLayout.CENTER);
        m_highlightOutputTable = new JCheckBox("Highlight output table");
        m_highlightOutputTable.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(final ActionEvent e) {
                m_table.showOutputTable(m_highlightOutputTable.isSelected());
                m_rowHeaderTable.showOutputTable(m_highlightOutputTable.isSelected());
                // repaint table
                m_scrollPane.invalidate();
                m_scrollPane.repaint();
            }
        });
        p.add(m_highlightOutputTable, BorderLayout.LINE_END);
        return p;
    }

    private String getOutputTableMessage(final int numRows, final int numCols,
                final int skipped) {
        StringBuilder builder = new StringBuilder();
        builder.append("The output table has ");
        builder.append(numRows);
        builder.append(" rows and ");
        builder.append(numCols - skipped);
        builder.append(" columns");
        if (skipped > 0) {
            builder.append(" (");
            builder.append(skipped);
            builder.append(" skipped)");
        }
        builder.append(".");


        return builder.toString();
    }

    private class AutoColumnHeaderController implements TableModelListener {
        /**
         * {@inheritDoc}
         */
        @Override
		public void tableChanged(final TableModelEvent e) {
            int col =  m_table.getSpreadsheetModel().getMaxColumn() - 1;
            if (col == -1) {
                return;
            }

            Map<Integer, ColProperty> map =
                new HashMap<Integer, ColProperty>();
            SortedMap<Integer, ColProperty> props =
                new TreeMap<Integer, ColProperty>();
            props.putAll(
                    m_table.getSpreadsheetModel().getColumnProperties());
            for (int i = 0; i <= col; i++) {
                if (!props.containsKey(i)) {
                    ColProperty prop =
                        PropertyColumnsAction.createDefaultColumnProperty(
                                props);
                    props.put(i, prop);
                    map.put(i, prop);
                }
            }
            if (!map.isEmpty()) {
                int editingRow = m_table.getFocusedRow();
                int editingColumn = m_table.getFocusedColumn();
                m_table.getSpreadsheetModel().addColProperties(map);
                m_table.getSelectionModel().setSelectionInterval(
                        editingRow, editingRow);
                m_table.getColumnModel().getSelectionModel().
                        setSelectionInterval(
                        editingColumn, editingColumn);
            }
        }
    }

    private class OutputTableController implements TableModelListener {
        private int m_numRows = 0;
        private int m_numColumns = 0;
        private int m_skipped = 0;

        /**
         * {@inheritDoc}
         */
        @Override
		public void tableChanged(final TableModelEvent e) {
            SortedMap<Integer, ColProperty> props =
                m_table.getSpreadsheetModel().getColumnProperties();
            int numColumns = m_table.getSpreadsheetModel().getMaxColumn();
            int numRows = m_table.getSpreadsheetModel().getMaxRow();
            int skipped = 0;
            for (ColProperty prop : props.values()) {
                skipped = prop.getSkipThisColumn() ? skipped + 1 : skipped;
            }
            if (numRows != m_numRows || numColumns != m_numColumns
                    || skipped != m_skipped) {
                m_outputTableMessage.setText(getOutputTableMessage(numRows,
                        numColumns, skipped));
                if (m_highlightOutputTable.isSelected()) {
                    // repaint table
                    m_scrollPane.invalidate();
                    m_scrollPane.repaint();
                }
                m_numRows = numRows;
                m_numColumns = numColumns;
                m_skipped = skipped;
            }
        }
    }


    /**
     * Keeps the table and the input line in sync.
     */
    private class InputLineController implements TableModelListener,
            PropertyChangeListener,
            KeyListener, ActionListener,
            CellEditorListener {

        /**
         *
         */
        public InputLineController() {
            updateInputLine(m_table.getFocusedRow(),
                        m_table.getFocusedColumn());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(SpreadsheetTable.PROP_FOCUSED_ROW)
                    || evt.getPropertyName().equals(
                            SpreadsheetTable.PROP_FOCUSED_COLUMN)) {
                updateInputLine(m_table.getFocusedRow(),
                        m_table.getFocusedColumn());
            }
        }

        private void updateInputLine(final int row, final int col) {
            if (row > -1 && col > -1) {
                m_inputLine.setEnabled(true);
                Object value = m_table.getModel().getValueAt(row, col);
                if (value instanceof Cell) {
                    m_inputLine.setText(((Cell)value).getText());
                } else {
                    m_inputLine.setText(value.toString());
                }
                // set caret to 0 so that the beginning of long text
                // is displayed (and not the end).
                m_inputLine.setCaretPosition(0);
            } else {
                m_inputLine.setEnabled(false);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyPressed(final KeyEvent e) {
            // do nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyReleased(final KeyEvent e) {
            if (e.getSource() == m_inputLine) {
                int row = m_table.getFocusedRow();
                int col = m_table.getFocusedColumn();

                if (row > -1 && col > -1) {
                    String text = m_inputLine.getText();
                    // remove this form the table model listener,
                    // since we will trigger an event in the next line.
                    m_table.getModel().removeTableModelListener(this);
                    // this triggers a table model changed event
                    m_table.getModel().setValueAt(text, row, col);
                    m_table.getModel().addTableModelListener(this);
                }
            } else {
                m_inputLine.setText(m_table.getEditorTextField().getText());
                // set caret to 0 so that the beginning of long text
                // is displayed (and not the end).
                m_inputLine.setCaretPosition(0);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyTyped(final KeyEvent e) {
            // do nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void tableChanged(final TableModelEvent e) {
            if (e.getType() == TableModelEvent.UPDATE
                    && e.getColumn() == m_table.getFocusedColumn()
                    && e.getFirstRow() == m_table.getFocusedRow()
                    && e.getLastRow() == m_table.getFocusedRow()) {
                updateInputLine(m_table.getFocusedRow(),
                        m_table.getFocusedColumn());
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            int row = m_table.getFocusedRow();
            if (row < m_table.getRowCount()) {
                m_table.getSelectionModel().setSelectionInterval(row + 1,
                        row + 1);
                m_table.scrollRectToVisible(
                        m_table.getCellRect(row + 1,
                                m_table.getFocusedColumn(), false));
            } else {
                m_table.getSelectionModel().setSelectionInterval(row,
                        row);
            }
            m_table.requestFocusInWindow();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void editingStopped(final ChangeEvent e) {
            // empty
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void editingCanceled(final ChangeEvent e) {
            updateInputLine(m_table.getFocusedRow(),
                    m_table.getFocusedColumn());
        }
    }


    /**
     * Replace the data of the table model by the given settings.
     *
     * @param columnProperties the properties of column
     * @param rowIndices the row indices
     * @param columnIndices the column indices
     * @param values the values at the given row and column indices
     */
    public void setData(final SortedMap<Integer,ColProperty> columnProperties,
            final int[] rowIndices, final int[] columnIndices,
            final String[] values) {
        m_table.getSpreadsheetModel().setData(
                columnProperties, rowIndices,
                columnIndices, values);
    }

    /**
     * see getValues();
     * @return row indices populated with data
     */
    public int[] getRowIndices() {
        return m_table.getSpreadsheetModel().getRowIndices();
    }

    /**
     * see getValues();
     * @return column indices populated with data
     */
    public int[] getColumnIndices() {
        return m_table.getSpreadsheetModel().getColumnIndices();
    }

    /**
     * Use in combination with getRowIndices and getColumnIndices. Together
     * with this method you will three arrays which are a sparse representation
     * of the tables data.
     *
     * @return the table data.
     */
    public String[] getValues() {
        return m_table.getSpreadsheetModel().getValues();
    }

    /**
     * Returns a mapping of column index to the {ColProperty} of the column.
     * The mapping must not exist for every column.
     *
     * @return a mapping of column index to the {ColProperty} of the column
     */
    public SortedMap<Integer, ColProperty> getColumnProperties() {
        return m_table.getSpreadsheetModel().getColumnProperties();
    }

    /**
     * reset the focused cell.
     */
    public void clearFocusedCell() {
        m_table.clearFocusedCell();
    }

    /**
     * Use this to stop cell editing from outside of the spreadsheet. Typically
     * needed to commit changes when editor is active.
     */
    public void stopCellEditing() {
        m_table.stopCellEditing();
    }

    /**
     * @return the rowIdPrefix
     */
    public final String getRowIdPrefix() {
        return m_rowHeaderTable.getRowModel().getRowIdPrefix();
    }
    /**
     * @param rowIdPrefix the rowIdPrefix to set
     */
    public final void setRowIdPrefix(final String rowIdPrefix) {
        m_rowHeaderTable.getRowModel().setRowIdPrefix(rowIdPrefix);
    }
    /**
     * @return the rowIdSuffix
     */
    public final String getRowIdSuffix() {
        return m_rowHeaderTable.getRowModel().getRowIdSuffix();
    }
    /**
     * @param rowIdSuffix the rowIdSuffix to set
     */
    public final void setRowIdSuffix(final String rowIdSuffix) {
        m_rowHeaderTable.getRowModel().setRowIdSuffix(rowIdSuffix);
    }
    /**
     * @return the rowIdStartValue
     */
    public final int getRowIdStartValue() {
         return m_rowHeaderTable.getRowModel().getRowIdStartValue();
    }
    /**
     * @param rowIdStartValue the rowIdStartValue to set
     */
    public final void setRowIdStartValue(final int rowIdStartValue) {
        m_rowHeaderTable.getRowModel().setRowIdStartValue(rowIdStartValue);
    }

    /**
     * Defines if output table should be highlighted or not.
     * @param hightlightOutputTable
     */
    public void setHighlightOutputTable(final boolean hightlightOutputTable) {
        if (hightlightOutputTable != m_highlightOutputTable.isSelected()) {
            m_highlightOutputTable.doClick();
        }
    }

    /**
     * @return the highlight output table property
     */
    public boolean getHightLightOutputTable() {
        return m_highlightOutputTable.isSelected();
    }

    /**
     * @return true when not every cell can be parsed
     */
    public boolean hasParseErrors() {
        SpreadsheetTableModel model = m_table.getSpreadsheetModel();
        int colCount = model.getColumnCount();
        int rowCount = model.getRowCount();
        for (int k = 0; k < colCount; k++) {
            ColProperty colProperty =
                model.getColumnProperties().get(k);
            String missingValuePattern = colProperty != null ?
                    colProperty.getMissingValuePattern() : "";
            for (int i = 0; i < rowCount; i++) {
                Object value = model.getValueAt(i, k);
                if (value instanceof Cell) {
                    Cell cell = (Cell)value;
                    if (cell.getValue() == null) {
                        return true;
                    }
                } else { // value is an empty string
                    if (null != colProperty) {
                        DataCellFactory cellFactory = new DataCellFactory();
                        cellFactory.setMissingValuePattern(missingValuePattern);
                        DataCell dataCell = cellFactory.createDataCellOfType(
                            colProperty.getColumnSpec().getType(),
                            value.toString());
                        if (null == dataCell) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
