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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   19 Apr 2022 (leon.wenzler): created
 */
package org.knime.core.data.transpose;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * Class to transpose a table.
 *
 * @author Leon Wenzler, KNIME AG, Zurich, Switzerland
 */
public abstract class AbstractTableTransposer {

    final BufferedDataTable m_inputTable;

    final DataTableSpec m_dataTableSpec;

    final int m_colsInOutTable;

    final int m_rowsInOutTable;

    final ExecutionContext m_exec;

    final BufferedDataContainer m_container;

    final Buffer m_buffer = new Buffer();

    /**
     * Holds data rows of the output table.
     *
     * Output rows are constructed by passing the i-th row of the input table to the buffer, which appends the data cell
     * in the column j to the j-th row in the output, where it becomes the data cell in column i.
     *
     * To support the case where a table doesn't fit into memory, the buffer supports constructing the output rows in
     * chunks. For example, with a chunk size of 2, only two columns at a time are converted to rows.
     *
     * For a table with N rows, a chunk size of C requires N/C passes over the input table, during which C*N data cells
     * must be held in memory. Between passes, rows can be flushed to disk and the buffer can be cleared.
     */
    class Buffer {
        /**
         * The key is the column name of the input table, which becomes a row id in the output table. The value is the
         * list of data cells that form the column in the input table, which becomes a row in the output table.
         */
        final Map<String, ArrayList<DataCell>> m_map = new LinkedHashMap<>();

        /**
         * Iterates through columns in input table to retrieve all DataCells of a row. Adds DataCells to corresponding
         * row in transposed output table.
         *
         * @param row current row
         * @param lowerBound chunk start
         * @param upperBound chunk end
         */
        void storeRowInColumns(final DataRow row, final int lowerBound, final int upperBound) {
            for (int c = lowerBound; c < upperBound; c++) {
                // get the corresponding column and fill the list of DataCells
                var newRowKey = m_dataTableSpec.getColumnSpec(c).getName();
                List<DataCell> cellList = m_map.computeIfAbsent(newRowKey, k -> new ArrayList<>());
                // no rowId has to be specified here, cells are appended in correct row order
                cellList.add(row.getCell(c));
            }
        }

        /**
         * Dynamically reduce chunk size by keeping only the first n of the rows that are currently being constructed.
         *
         * @param limit the number of rows to keep
         */
        void truncateRows(final int limit) {
            // list of keys specifying the first elements
            final var keyList = new ArrayList<>();
            var rowCount = 0;
            for (String key : m_map.keySet()) {
                keyList.add(key);
                rowCount++;
                if (rowCount >= limit) {
                    break;
                }
            }
            // removes all elements not present in the keyList
            m_map.keySet().retainAll(keyList);
        }

        /**
         * Get the constructed rows.
         */
        List<DataRow> getRows() {
            return m_map.entrySet().stream()//
                .map(e -> new DefaultRow(e.getKey(), e.getValue()))//
                .collect(Collectors.toList());
        }

        /**
         * Release all temporary data.
         */
        void clear() {
            m_map.clear();
        }
    }

    AbstractTableTransposer(final BufferedDataTable inputTable, final ExecutionContext exec)
        throws CanceledExecutionException {
        if (inputTable == null) {
            throw new IllegalArgumentException("Argument for input table must not be null.");
        }
        m_inputTable = inputTable;
        m_dataTableSpec = inputTable.getDataTableSpec();
        m_colsInOutTable = (int)inputTable.size();
        m_rowsInOutTable = m_dataTableSpec.getNumColumns();
        m_exec = exec;
        m_container = createOutputContainer(inputTable, exec);
    }

    /**
     * Creates the BufferedDataContainer based on the inputTable. Determines the new column types and names. Uses the
     * ExecutionContext to create the new container.
     *
     * @param inputTable BufferedDataTable
     * @param exec ExecutionContent
     * @return output BufferedDataContainer
     * @throws CanceledExecutionException
     */
    private static BufferedDataContainer createOutputContainer(final BufferedDataTable inputTable,
        final ExecutionContext exec) throws CanceledExecutionException {
        final int newNrCols = (int)inputTable.size();
        // new column names
        final ArrayList<String> colNames = new ArrayList<>();
        // new column types
        final ArrayList<DataType> colTypes = new ArrayList<>();

        // index for unique colNames if row id only contains whitespace
        var idx = 0;

        try (final CloseableRowIterator iterator = inputTable.iterator()) {
            while (iterator.hasNext()) {
                final DataRow row = iterator.next();
                exec.checkCanceled();
                exec.setMessage(() -> "Determine most-general column type for row: " + row.getKey().getString());
                DataType type = null;
                // and all cells
                for (var i = 0; i < row.getNumCells(); i++) {
                    DataType newType = row.getCell(i).getType();
                    type = type == null ? newType : DataType.getCommonSuperType(type, newType);
                }
                if (type == null || type.isMissingValueType()) {
                    type = DataType.getType(DataCell.class);
                }
                String colName = row.getKey().getString().trim();
                if (colName.isEmpty()) {
                    colName = "<empty_" + idx + ">";
                    idx++;
                }
                colNames.add(colName);
                colTypes.add(type);
            }
        }

        // create the specs
        final var colSpecs = new DataColumnSpec[newNrCols];
        for (var c = 0; c < newNrCols; c++) {
            colSpecs[c] = new DataColumnSpecCreator(colNames.get(c), colTypes.get(c)).createSpec();
            exec.checkCanceled();
        }
        return exec.createDataContainer(new DataTableSpec(colSpecs));
    }

    /**
     * Checks if the current execution has been canceled and if so, container is closed and exception thrown.
     *
     * @throws CanceledExecutionException
     */
    void handleIfCanceled() throws CanceledExecutionException {
        try {
            m_exec.checkCanceled();
        } catch (CanceledExecutionException cee) {
            m_container.close();
            m_buffer.clear();
            throw cee;
        }
    }

    /**
     * Compute the transposed table, i.e., columns of the input table become rows in the output table. Note that column
     * properties are lost during transposition!
     *
     * @throws CanceledExecutionException
     */
    public abstract void transpose() throws CanceledExecutionException;

    /**
     * @return transposed output table
     */
    public BufferedDataTable getTransposedTable() {
        if (m_container.isOpen()) {
            m_container.close();
        }
        m_buffer.clear();
        return m_container.getTable();
    }
}
