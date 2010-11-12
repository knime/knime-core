/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.table.AbstractTableModel;

import org.knime.base.node.io.filereader.ColProperty;
import org.knime.core.data.DataType;

/**
 * The table model of a spreadsheet.
 *
 * @author Heiko Hofer
 */
class SpreadsheetTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 5162625446456392992L;
    private SortedMap<IntPair, Cell> m_values;
    private SortedMap<Integer, ColProperty> m_colProps;

    /**
     * Create new instance.
     */
    public SpreadsheetTableModel() {
        m_colProps = new TreeMap<Integer, ColProperty>();
        m_values = new TreeMap<IntPair, Cell>();
        initData(new int[0], new int[0], new String[0]);
    }

    private void initData (final int[] rowIndices, final int[] columnIndices,
            final String[] values) {
        assert rowIndices.length == columnIndices.length;
        assert rowIndices.length == values.length;
        for (int i = 0; i < rowIndices.length; i++) {
            putValueAt(values[i], rowIndices[i], columnIndices[i]);
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
    public void setData(final Map<Integer, ColProperty> columnProperties,
            final int[] rowIndices, final int[] columnIndices,
            final String[] values) {
        m_colProps.clear();
        m_values.clear();
        m_colProps.putAll(columnProperties);
        initData(rowIndices, columnIndices, values);
        fireTableStructureChanged();
    }


    /**
     * Returns a mapping of column index to the {ColProperty} of the column.
     * The mapping must not exist for every column.
     *
     * @return a mapping of column index to the {ColProperty} of the column
     */
    SortedMap<Integer,ColProperty> getColumnProperties() {
        return m_colProps;
    }

    /**
     * see getValues();
     * @return row indices populated with data
     */
    int[] getRowIndices() {
        final int[] indices = new int[m_values.size()];
        int i = 0;
        for (final IntPair p : m_values.keySet()) {
            indices[i] = p.getRow();
            i++;
        }
        return indices;
    }

    /**
     * see getValues();
     * @return column indices populated with data
     */
    int[] getColumnIndices() {
        int[] indices = new int[m_values.size()];
        int i = 0;
        for (IntPair p : m_values.keySet()) {
            indices[i] = p.getCol();
            i++;
        }
        return indices;
    }

    /**
     * Use in combination with getRowIndices and getColumnIndices. Together
     * with this method you will three arrays which are a sparse representation
     * of the tables data.
     *
     * @return the table data.
     */
    String[] getValues() {
        String[] values = new String[m_values.size()];
        int i = 0;
        for (Cell cell : m_values.values()) {
            values[i] = cell.getText();
            i++;
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return String.class;
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return 256;
    }

    /**
     * @param colIdx
     * @param colProperty
     */
    public void setColProperty(final int colIdx,
            final ColProperty colProperty) {
        putColProperty(colIdx, colProperty);
        fireTableStructureChanged();
    }

    private void putColProperty(final int colIdx,
            final ColProperty colProperty) {
        DataType oldType = m_colProps.containsKey(colIdx) ?
        m_colProps.get(colIdx).getColumnSpec().getType() : null;
        DataType type = colProperty.getColumnSpec().getType();
        String oldMissVal = m_colProps.containsKey(colIdx) ?
                m_colProps.get(colIdx).getMissingValuePattern() : null;
        String missVal = colProperty.getMissingValuePattern();
        // when type has changed
        if ((null == oldType && null != type)
              || (null != oldType && !oldType.equals(type))
              || (null == oldMissVal && null != missVal)
            || (null != oldMissVal && !oldMissVal.equals(missVal))) {
            for (IntPair key : m_values.keySet()) {
                if (key.getCol() == colIdx) {
                    m_values.get(key).setType(type);
                    m_values.get(key).setMissingValuePattern(missVal);
                }
            }
        }
        m_colProps.put(colIdx, colProperty);
    }


    /**
     * Adds {link {@link ColProperty} objects to this model.
     * @param map {link {@link ColProperty} objects
     */
    public void addColProperties(final Map<Integer, ColProperty> map) {
        for (Integer colIdx : map.keySet()) {
            putColProperty(colIdx, map.get(colIdx));
        }
        fireTableStructureChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int columnIndex) {
        ColProperty colName = m_colProps.get(columnIndex);
        if (null != colName) {
            return colName.getColumnSpec().getName();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return 64000;
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        Cell value = m_values.get(new IntPair(rowIndex, columnIndex));
        if (null != value) {
            return value;
        } else {
            return "";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object aValue, final int rowIndex,
            final int columnIndex) {
        Object value = m_values.get(new IntPair(rowIndex, columnIndex));
        if (value == null && aValue.toString().isEmpty()) {
            return;
        }
        if (!aValue.equals(value)) {
            if (aValue.toString().isEmpty()) {
                m_values.remove(new IntPair(rowIndex, columnIndex));
            } else {
                putValueAt(aValue.toString(), rowIndex, columnIndex);
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    private void putValueAt(final String aValue, final int rowIndex,
            final int columnIndex) {
        DataType type = m_colProps.containsKey(columnIndex) ?
                m_colProps.get(columnIndex).getColumnSpec().getType() :
                    null;
        String missValue = m_colProps.containsKey(columnIndex) ?
                        m_colProps.get(columnIndex).getMissingValuePattern() :
                            null;
        Cell cell = new Cell(aValue, type, missValue);
        m_values.put(new IntPair(rowIndex, columnIndex),
                cell);
    }

    /**
     * Add values to the table.
     * @param tableValues the values and their position
     */
    public void addValues(final Map<IntPair, Cell> tableValues) {
        if (null == tableValues || tableValues.isEmpty()) {
            return;
        }
        m_values.putAll(tableValues);
        fireTableDataChanged();
    }

    /**
     * Deletes the columns.
     * @param cols the indices of the columns to delete.
     */
    public void deleteColumns(final int[] cols) {
        if (cols.length <= 0) {
            return;
        }
        Arrays.sort(cols);
        SortedMap<IntPair, Cell> moved = new TreeMap<IntPair, Cell>();
        for (Iterator<Entry<IntPair, Cell>> iter = m_values.entrySet().iterator();
                    iter.hasNext(); ) {
            Entry<IntPair, Cell> curr = iter.next();
            IntPair pos = curr.getKey();
            if (pos.getCol() >= cols[0]) {

                boolean delete = false;
                int moveby = 0;
                for (int col : cols) {
                    if (col == pos.getCol()) {
                        delete = true;
                        break;
                    } else if (col > pos.getCol()) {
                        break;
                    } else { // col < pos.getCol()
                        moveby++;
                    }
                }
                if (!delete) {
                    moved.put(new IntPair(pos.getRow(), pos.getCol() - moveby),
                            curr.getValue());
                }
                iter.remove();
            }
        }
        m_values.putAll(moved);

        SortedMap<Integer, ColProperty> movedColNames = new TreeMap<Integer, ColProperty>();
        for (Iterator<Entry<Integer, ColProperty>> iter = m_colProps.entrySet().iterator();
                    iter.hasNext(); ) {
            Entry<Integer, ColProperty> curr = iter.next();
            Integer pos = curr.getKey();
            if (pos >= cols[0]) {

                boolean delete = false;
                int moveby = 0;
                for (int col : cols) {
                    if (col == pos) {
                        delete = true;
                        break;
                    } else if (col > pos) {
                        break;
                    } else { // col < pos
                        moveby++;
                    }
                }
                if (!delete) {
                    movedColNames.put(new Integer(pos - moveby),
                            curr.getValue());
                }
                iter.remove();
            }
        }
        m_colProps.putAll(movedColNames);

        fireTableStructureChanged();
    }

    /**
     * Add columns with default properties.
     * @param cols the indices to insert columns
     */
    public void insertColumns(final int[] cols) {
        if (cols.length <= 0) {
            return;
        }
        Arrays.sort(cols);
        SortedMap<IntPair, Cell> moved = new TreeMap<IntPair, Cell>();
        for (Iterator<Entry<IntPair, Cell>> iter =
            m_values.entrySet().iterator();
                    iter.hasNext(); ) {
            Entry<IntPair, Cell> curr = iter.next();
            IntPair pos = curr.getKey();
            if (pos.getCol() >= cols[0]) {
                int moveby = 0;
                for (int i = 0; i < cols.length; i++) {
                    if (cols[i] <= pos.getCol()
                            || (i > 0
                                    && cols[i - 1] + 1 == cols[i])) {
                        moveby++;
                    } else {
                        break;
                    }
                }
                if (pos.getCol() + moveby < getColumnCount()) {
                    moved.put(new IntPair(pos.getRow(), pos.getCol() + moveby),
                                curr.getValue());
                }
                iter.remove();
            }
        }
        m_values.putAll(moved);

        SortedMap<Integer, ColProperty> movedColNames =
            new TreeMap<Integer, ColProperty>();
        for (Iterator<Entry<Integer, ColProperty>> iter =
            m_colProps.entrySet().iterator(); iter.hasNext(); ) {
            Entry<Integer, ColProperty> curr = iter.next();
            Integer pos = curr.getKey();
            if (pos >= cols[0]) {
                int moveby = 0;
                for (int i = 0; i < cols.length; i++) {
                    if (cols[i] <= pos
                            || (i > 0
                                    && cols[i - 1] + 1 == cols[i])) {
                        moveby++;
                    } else {
                        break;
                    }
                }                if (pos + moveby < getColumnCount()) {
                    movedColNames.put(new Integer(pos + moveby),
                                curr.getValue());
                }
                iter.remove();
            }
        }
        m_colProps.putAll(movedColNames);

        fireTableStructureChanged();
    }


    /**
     * Delete rows.
     * @param rows the indices of the rows to delete.
     */
    public void deleteRows(final int[] rows) {
        if (rows.length <= 0) {
            return;
        }
        Arrays.sort(rows);
        SortedMap<IntPair, Cell> moved = new TreeMap<IntPair, Cell>();
        for (Iterator<Entry<IntPair, Cell>> iter =
                    m_values.entrySet().iterator();
                    iter.hasNext(); ) {
            Entry<IntPair, Cell> curr = iter.next();
            IntPair pos = curr.getKey();
            if (pos.getRow() >= rows[0]) {
                boolean delete = false;
                int moveby = 0;
                for (int row : rows) {
                    if (row == pos.getRow()) {
                        delete = true;
                        break;
                    } else if (row > pos.getRow()) {
                        break;
                    } else { // col < pos.getRow()
                        moveby++;
                    }
                }
                if (!delete) {
                    moved.put(new IntPair(pos.getRow() - moveby, pos.getCol()),
                            curr.getValue());
                }
                iter.remove();
            }
        }
        m_values.putAll(moved);

        // This event does not clear the selection
        fireTableRowsUpdated(0, getRowCount() - 1);
    }

    /**
     * Insert empty rows.
     * @param rows the indices to insert empty rows.
     */
    public void insertRows(final int[] rows) {
        if (rows.length <= 0) {
            return;
        }
        Arrays.sort(rows);
        SortedMap<IntPair, Cell> moved = new TreeMap<IntPair, Cell>();
        for (Iterator<Entry<IntPair,
                Cell>> iter = m_values.entrySet().iterator();
                iter.hasNext(); ) {
            Entry<IntPair, Cell> curr = iter.next();
            IntPair pos = curr.getKey();
            if (pos.getRow() >= rows[0]) {
                int moveby = 0;
                for (int i = 0; i < rows.length; i++) {
                    if (rows[i] <= pos.getRow()
                            || (i > 0 && rows[i - 1] + 1 == rows[i])) {
                        moveby++;
                    } else {
                        break;
                    }
                }
                if (pos.getRow() + moveby < getRowCount()) {
                    moved.put(new IntPair(pos.getRow() + moveby, pos.getCol()),
                                curr.getValue());
                }
                iter.remove();
            }
        }
        m_values.putAll(moved);

        // This event does not clear the selection
        fireTableRowsUpdated(0, getRowCount() - 1);
    }

    /**
     * Returns the maximal row index of the filled area
     * @return the maximal row index of the filled area
     */
    public int getMaxRow() {
        int numRows = maxRow(m_values) + 1;
        return numRows;
    }

    /**
     * Returns the maximal column index of the filled area
     * @return the maximal column index of the filled area
     */
    public int getMaxColumn() {
        int numColumns = maxCol(m_values) + 1;
        if (!m_colProps.isEmpty()) {
            numColumns = Math.max(numColumns, m_colProps.lastKey() + 1);
        }
        return numColumns;
    }

    private int maxRow(final SortedMap<IntPair, Cell> tableValues) {
        if (null != tableValues && !tableValues.isEmpty()) {
            return tableValues.lastKey().getRow();
        } else {
            return -1;
        }
    }

    private int maxCol(final SortedMap<IntPair, Cell> tableValues) {
        int maxCol = -1;
        if (null != tableValues && !tableValues.isEmpty()) {
            for (IntPair key : tableValues.keySet()) {
                maxCol = Math.max(maxCol, key.getCol());
            }
        }
        return maxCol;
    }


    /**
     * Class to encapsulate the row and column index of a cell in a table.
     * @author Heiko Hofer
     */
    static class IntPair implements Comparable<IntPair> {
        private int m_row;
        private int m_col;

        /**
         * Create a new instance.
         *
         * @param row the row index.
         * @param col the column index.
         */
        IntPair(final int row, final int col) {
            this.m_row = row;
            this.m_col = col;
        }


        /**
         * @return the row
         */
        final int getRow() {
            return m_row;
        }


        /**
         * @return the col
         */
        final int getCol() {
            return m_col;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + m_row;
            result = prime * result + m_col;
            return result;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            IntPair other = (IntPair)obj;
            if (m_row != other.m_row)
                return false;
            if (m_col != other.m_col)
                return false;
            return true;
        }


        /**
         * {@inheritDoc}
         */
        public int compareTo(final IntPair that) {
            if (this.m_row == that.m_row) {
                return this.m_col - that.m_col;
            } else {
                return this.m_row - that.m_row;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "(" + m_row + "," + m_col + ")";
        }
    }



    /**
     * Getter for the table values.
     * @return the table values
     */
    Map<IntPair, Cell> getTableValues() {
        return m_values;
    }
}
