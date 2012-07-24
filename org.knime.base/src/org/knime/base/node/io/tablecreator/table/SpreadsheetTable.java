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
 *   03.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator.table;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import org.knime.base.node.io.filereader.ColProperty;
import org.knime.base.node.io.tablecreator.prop.SmilesTypeHelper;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;


/**
 * The main table of the spreadsheet component.
 *
 * @author Heiko Hofer
 */
class SpreadsheetTable extends JTable {
    private static final long serialVersionUID = 4270519208349907535L;
    /** Property fired when the focused row has changed. */
    static final String PROP_FOCUSED_ROW = "spreadsheet_focused_row";
    /** Property fired when the focused column has changed. */
    static final String PROP_FOCUSED_COLUMN = "spreadsheet_focused_column";
    /** the border for the focues cell. */
    private static final Color FOCUS_BORDER_COLOR = Color.DARK_GRAY;

    private ColumnHeaderRenderer m_colHeaderRenderer;
    private int m_focusedRow;
    private int m_focusedColumn;

    private JTextField m_editorTextField;
    private MyCellEditor m_cellEditor;
    private CellRenderer m_cellRenderer;

    /**
     * Create a new instance.
     */
    public SpreadsheetTable() {
        m_focusedRow = -1;
        m_focusedColumn = -1;
        setModel(new SpreadsheetTableModel());

        // Increase row height
        setRowHeight(getRowHeight() + 3);

        getTableHeader().setPreferredSize(new Dimension(
                getTableHeader().getPreferredSize().width,
                getRowHeight()));

        Color gridColor = getGridColor();
        // brighten the grid color
        setGridColor(new Color((gridColor.getRed() + 255) / 2
            , (gridColor.getGreen() + 255) / 2
            , (gridColor.getBlue() + 255) / 2));

        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        getTableHeader().setReorderingAllowed(false);

        setBackground(ColorAttr.BACKGROUND);
        // set table editor and renderer to custom ones
        //m_table.setDefaultRenderer(String.class, new SpreadsheetCellRenderer());
        // see Bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4256006
        // Workaround by bridgehajen (Submitted On 26-SEP-2002)
        m_editorTextField = new JTextField() {
            @Override
            protected boolean processKeyBinding(final KeyStroke ks,
                    final java.awt.event.KeyEvent e, final int condition,
                    final boolean pressed) {
                if (hasFocus()) {
                    return super.processKeyBinding(ks, e, condition, pressed);
                } else { // you get in this state when key was typed in a cell
                         // without active editor
                    // clear cell in this case
                    this.setText("");
                    // different cell editor behavior in this case
                    m_cellEditor.setStopEditingWidthArrows(true);
                    // request focus
                    this.requestFocus();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            processKeyBinding(ks, e, condition, pressed);
                        }
                    });
                    return true;
                }
            }
        };

        m_editorTextField.setBorder(
                BorderFactory.createLineBorder(ColorAttr.BACKGROUND));

        m_cellEditor = new MyCellEditor(m_editorTextField);
        setDefaultEditor(Object.class, m_cellEditor);
        m_cellRenderer = new CellRenderer(getSpreadsheetModel());
        setDefaultRenderer(Object.class, m_cellRenderer);

        // set selection mode for contiguous  intervals
        setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setCellSelectionEnabled(true);
        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                int selected =  getSelectionModel().getLeadSelectionIndex();
                if (selected > -1 && m_focusedRow != selected) {
                    int editRow = m_focusedRow;
                    m_focusedRow = selected;
                    repaint();
                    repaint(getCellRect(editRow, m_focusedColumn, false));
                    firePropertyChange(PROP_FOCUSED_ROW, editRow, m_focusedRow);
                }
            }
        });
        getColumnModel().getSelectionModel().addListSelectionListener(
          new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                int selected = getColumnModel().getSelectionModel()
                .getLeadSelectionIndex();
                if (selected > -1 && m_focusedColumn != selected) {
                    int editCol = m_focusedColumn;
                    m_focusedColumn = selected;
                    repaint();
                    repaint(getCellRect(m_focusedRow, editCol, false));
                    firePropertyChange(PROP_FOCUSED_COLUMN, editCol,
                            m_focusedColumn);
                }
            }
        });

        getColumnModel().getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                getTableHeader().repaint();
            }
        });
        getTableHeader().addMouseListener(new ColHeaderMouseAdapter(this));
        getTableHeader().addMouseMotionListener(
                new ColHeaderMouseAdapter(this));
        addMouseListener(new TableMouseAdapter(this));

        // see bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4503845
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    /**
     * @return the table model
     */
    SpreadsheetTableModel getSpreadsheetModel() {
        return (SpreadsheetTableModel)getModel();
    }

    /**
     * Define whether the output table should be highlighted.
     *
     * @param show true when the output table should be highlighted.
     */
    void showOutputTable(final boolean show) {
        m_colHeaderRenderer.showOutputTable(show);
        m_cellRenderer.showOutputTable(show);
    }


    /**
     * @return the editors text field
     */
    JTextField getEditorTextField() {
        return m_editorTextField;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addColumn(final TableColumn aColumn) {
        int idx = aColumn.getModelIndex();
        // not that prop can be null
        ColProperty prop = getSpreadsheetModel().getColumnProperties().get(idx);
        aColumn.setHeaderValue(prop);
        aColumn.setPreferredWidth(aColumn.getPreferredWidth() + 5);
        super.addColumn(aColumn);
        if (null == m_colHeaderRenderer) {
            m_colHeaderRenderer = new ColumnHeaderRenderer(this);
        }
        aColumn.setHeaderRenderer(m_colHeaderRenderer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D)g;
        Stroke prevStroke = g2.getStroke();
        Color prevColor = g2.getColor();

        g2.setStroke(new BasicStroke(2f));
        g2.setColor(FOCUS_BORDER_COLOR);
        Rectangle cell = getCellRect(m_focusedRow, m_focusedColumn, false);

        g2.draw(cell);
        g2.setStroke(prevStroke);
        g2.setColor(prevColor);
    }

    /**
     * Shows popup menu of the table.
     *
     * @author Heiko Hofer
     */
    private static class TableMouseAdapter extends MouseAdapter {
        private JTable m_table;
        private JPopupMenu m_popup;

        /**
         * @param table
         */
        TableMouseAdapter(final SpreadsheetTable table) {
            m_table = table;
            m_popup = new JPopupMenu();
            m_popup.add(new DeleteAction(table));
            m_popup.add(new JSeparator());
            m_popup.add(new CutAction(table));
            m_popup.add(new CopyAction(table));
            m_popup.add(new PasteAction(table));

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

    /**
     * The cell editor.
     *
     * @author Heiko Hofer
     */
    private static class MyCellEditor extends DefaultCellEditor {
        private JTextField m_editorField;
        private KeyListener m_keyListener;
        private MouseListener m_mouseListener;
        private JTable m_table;
        private int m_row;
        private int m_col;

        MyCellEditor(final JTextField editorField) {
            super(editorField);
            m_editorField = editorField;
            m_keyListener = new KeyAdapter () {
                @Override
                public void keyPressed(final KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT
                            || e.getKeyCode() == KeyEvent.VK_KP_RIGHT) {
                        stopCellEditing();
                        int col = Math.min(
                                m_table.getModel().getColumnCount() - 1,
                                m_col + 1);
                        m_table.changeSelection(m_row, col, false, false);
                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT
                            || e.getKeyCode() == KeyEvent.VK_KP_LEFT) {
                        stopCellEditing();
                        int col = Math.max(0, m_col - 1);
                        m_table.changeSelection(m_row, col, false, false);
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        cancelCellEditing();
                    }
                }
            };
            m_mouseListener = new MouseAdapter() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void mousePressed(final MouseEvent e) {
                    setStopEditingWidthArrows(false);
                }
            };
            m_editorField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(final KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (m_row < m_table.getRowCount()) {
                            m_table.changeSelection(m_row + 1, m_col,
                                    false, false);
                        }
                    }
                }
            });
        }

        void setStopEditingWidthArrows(final boolean value) {
            if (value) {
                m_editorField.addKeyListener(m_keyListener);
                m_editorField.addMouseListener(m_mouseListener);
            } else {
                m_editorField.removeKeyListener(m_keyListener);
                m_editorField.removeMouseListener(m_mouseListener);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopCellEditing() {
            m_editorField.removeKeyListener(m_keyListener);
            m_editorField.removeMouseListener(m_mouseListener);
            return super.stopCellEditing();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellEditorComponent(final JTable table,
                final Object value, final boolean isSelected, final int row,
                final int column) {
            m_table = table;
            m_row = row;
            m_col = column;
            String stringVal = null;
            if (value instanceof Cell) {
                stringVal = ((Cell)value).getText();
            } else {
                stringVal = value.toString();
            }
            Component c =
                    super.getTableCellEditorComponent(table, stringVal,
                            isSelected, row, column);
            return c;
        }
    }

    /**
     * Does column selection when clicking on the tables header. Show the
     * popup menu of the tables header.
     *
     * @author Heiko Hofer
     */
    private static class ColHeaderMouseAdapter extends MouseAdapter {
        private SpreadsheetTable m_table;
        private JPopupMenu m_popup;

        /**
         * @param table
         */
        ColHeaderMouseAdapter(final SpreadsheetTable table) {
            m_table = table;


            JMenu changeType = new JMenu("Change Type");
            JMenuItem doubleType = new JMenuItem("Double");
            doubleType.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    changeSelectedColumnsType(DoubleCell.TYPE);
                }
            });
            changeType.add(doubleType);
            JMenuItem intType = new JMenuItem("Integer");
            intType.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    changeSelectedColumnsType(IntCell.TYPE);
                }
            });
            changeType.add(intType);
            JMenuItem stringType = new JMenuItem("String");
            stringType.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    changeSelectedColumnsType(StringCell.TYPE);
                }
            });
            changeType.add(stringType);
            if (SmilesTypeHelper.INSTANCE.isSmilesAvailable()) {
                JMenuItem smilesType = new JMenuItem("Smiles");
                smilesType.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        changeSelectedColumnsType(
                                SmilesTypeHelper.INSTANCE.getSmilesType());
                    }
                });
                changeType.add(smilesType);
            }

            m_popup = new JPopupMenu();
            m_popup.add(new PropertyColumnsAction(m_table));
            m_popup.add(changeType);
            m_popup.add(new JSeparator());
            m_popup.add(new InsertColumnsAction(m_table));
            m_popup.add(new DeleteColumnsAction(m_table));

            m_table.add(m_popup);
        }

        private void changeSelectedColumnsType(final DataType type) {
            SpreadsheetTableModel model = m_table.getSpreadsheetModel();
            Map<Integer, ColProperty> changedProps =
                new HashMap<Integer, ColProperty>();

            Map<Integer, ColProperty> props = model.getColumnProperties();
            for (int col :  m_table.getColumnModel().getSelectedColumns()) {
                if (props.containsKey(col)) {
                    DataType colType = props.get(col).getColumnSpec().getType();
                    if (!colType.equals(type)) {
                        ColProperty newProp =
                            (ColProperty)props.get(col).clone();
                        newProp.changeColumnType(type);
                        changedProps.put(col, newProp);
                    }
                } else {
                    SortedMap<Integer, ColProperty> allProps =
                        new TreeMap<Integer, ColProperty>();
                    allProps.putAll(props);
                    allProps.putAll(changedProps);
                    ColProperty newProp =
                        PropertyColumnsAction.createDefaultColumnProperty(
                                allProps);
                    newProp.changeColumnType(type);
                    changedProps.put(col, newProp);
                }

            }
            model.addColProperties(changedProps);
        }

        private void showPopup(final MouseEvent e) {
            int col = m_table.getTableHeader().columnAtPoint(e.getPoint());
            if (!m_table.getColumnModel().getSelectionModel()
                    .isSelectedIndex(col)) {
                m_table.getColumnModel().getSelectionModel()
                    .setSelectionInterval(col, col);
                m_table.getSelectionModel()
                    .setSelectionInterval(m_table.getRowCount() - 1, 0);
            }
            m_popup.show(m_table.getTableHeader(), e.getX(), e.getY());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            } else if (e.getClickCount() == 2) {
                new PropertyColumnsAction(m_table).actionPerformed(null);
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            int col = m_table.getTableHeader().columnAtPoint(e.getPoint());
            // The autoscroller can generate drag events outside the
            // table's range.
            if (col == -1) {
                return;
            }
            // Commit edit, which are otherwise lost
            TableCellEditor editor = m_table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
            if (e.isPopupTrigger()) {
                showPopup(e);
            } else {
                m_table.changeSelection(0, col, e.isControlDown(),
                        e.isShiftDown());
                if (!m_table.getColumnModel().getSelectionModel()
                        .isSelectionEmpty()) {
                    m_table.getSelectionModel()
                        .setSelectionInterval(m_table.getRowCount() - 1, 0);
                } else {
                    m_table.getSelectionModel().clearSelection();
                }
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

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseDragged(final MouseEvent e) {
            int col = m_table.getTableHeader().columnAtPoint(e.getPoint());
            // The autoscroller can generate drag events outside the
            // table's range.
            if (col == -1) {
                return;
            }
            m_table.changeSelection(0, col, e.isControlDown(), true);
        }


    }

    /**
     * The renderer of the tables header
     *
     * @author Heiko Hofer
     */
    private static class ColumnHeaderRenderer extends HeaderRenderer {
        private JTable m_table;
        private ListSelectionModel m_selectionModel;

        /**
         * @param table the spreadsheet
         */
        ColumnHeaderRenderer(final JTable table) {
            m_table = table;
            m_selectionModel = table.getColumnModel().getSelectionModel();
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
            boolean skipped = false;
            if (model.getColumnProperties().containsKey(column)) {
                skipped = model.getColumnProperties().get(column)
                                        .getSkipThisColumn();
            }
            return !(skipped || row >= maxRow || column >= maxColumn);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(final JTable table,
                final Object value, final boolean selected,
                final boolean focused, final int row, final int column) {
            ColProperty prop = (ColProperty) value;
            Object v;
            if (null != prop) {
                v = prop.getColumnSpec().getName();
            } else {
                v = "";
            }
            super.getTableCellRendererComponent(table,
                    v, m_selectionModel.isSelectedIndex(column), focused,
                    row, column);
            if (null != prop) {
                setIcon(prop.getColumnSpec().getType().getIcon());
            } else {
                setIcon(null);
            }
            return this;
        }
    }

    /**
     * Getter for the focused column
     * @return  the focused column
     */
    public int getFocusedColumn() {
        return m_focusedColumn;
    }

    /**
     * Getter for the focused row
     * @return  the focused row
     */
    public int getFocusedRow() {
        return m_focusedRow;
    }

    /**
     * Resets focused row and focused column
     */
    public void clearFocusedCell() {
        if (m_focusedRow != -1) {
            int val = m_focusedRow;
            m_focusedRow = -1;
            firePropertyChange(PROP_FOCUSED_ROW, val, m_focusedRow);
        }
        if (m_focusedColumn != -1) {
            int val = m_focusedColumn;
            m_focusedColumn = -1;
            firePropertyChange(PROP_FOCUSED_COLUMN, val, m_focusedColumn);
        }
    }

    /**
     * Use this to stop editing from outside of the table.
     */
    public void stopCellEditing() {
        m_cellEditor.stopCellEditing();
    }

    /**
     * Overriden to prevent the clearance of selection when columns are added
     * or removed. This is not perfect since the table column model recreates
     * the columns which resets the selection as well. However, with this code
     * the editing cell is displayed correctly.
     * {@inheritDoc}
     */
    @Override
    public void tableChanged(final TableModelEvent e) {
        if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
            if (getAutoCreateColumnsFromModel()) {
                // This will effect invalidation of the JTable and JTableHeader.
                createDefaultColumnsFromModel();
                return;
            }

            resizeAndRepaint();
            return;
        }
        super.tableChanged(e);

    }

}
