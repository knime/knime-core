/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Nov 10, 2015 (hornm): created
 */
package org.knime.base.node.meta.looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedRowsTable;
import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.data.container.ConcatenateTable;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.DuplicateKeyException;

/**
 * Helper class that collects the rows of different tables. From these collection of tables a {@link ConcatenateTable}
 * can finally be created.
 *
 * @author Martin Horn, University of Konstanz
 * @since 3.1
 */
class ConcatenateTableFactory {

    /** Maximum number of tables to be kept. If this threshold is exceeded the so far created
     * tables are copied into an entire new one.*/
    private static final int MAX_NUM_TABLES = 50;

    private ArrayList<BufferedDataContainer> m_tables;

    private BufferedDataContainer m_emptyTable;

    private boolean m_ignoreEmptyTables;

    private boolean m_tolerateColumnTypes;

    private boolean m_addIterationColumn;

    private boolean m_tolerateChangingSpecs;

    private Function<RowKey, RowKey> m_rowKeyCreator;

    private DuplicateChecker m_duplicateChecker;

    /** keeps track of the number of call of the addTable-function. m_iterationCount <= m_tables.size() */
    private int m_iterationCount = 0;


    /**
     * Creates a new factory that allows to create a {@link ConcatenateTable}.
     *
     * @param ignoreEmptyTables if empty tables should entirely be skipped
     * @param tolerateColumnTypes if the change of a column type should be tolerated (common supertype is determined)
     * @param addIterationColumn  if an iteration column should be appended
     * @param rowKeyCreator an optional row key creator. If not provided, the row keys remain unmodified.
     * @param exec execution context mainly in order to create the new data containers
     */
    ConcatenateTableFactory(final boolean ignoreEmptyTables, final boolean tolerateColumnTypes,
        final boolean addIterationColumn, final boolean tolerateChangingSpecs, final Optional<Function<RowKey, RowKey>> rowKeyCreator) {

        m_ignoreEmptyTables = ignoreEmptyTables;
        m_tolerateColumnTypes = tolerateColumnTypes;
        m_addIterationColumn = addIterationColumn;
        m_tolerateChangingSpecs = tolerateChangingSpecs;

        m_tables = new ArrayList<BufferedDataContainer>();
        if (rowKeyCreator.isPresent()) {
            m_rowKeyCreator = rowKeyCreator.get();
        }
        m_duplicateChecker = new DuplicateChecker();
        m_iterationCount = 0;
    }

    /**
     * Table is added and rows are copied to a new data container. Creates a new data container if this data table spec
     * differs from the previous table. This method call checks for row keys duplicates and throws a
     * {@link DuplicateKeyException}.
     *
     * @param table the table to be added
     * @param exec the execution context to possibly create a new data container
     * @throws InterruptedException
     * @throws IOException
     * @throws DuplicateKeyException
     * @throws CanceledExecutionException
     */
    void addTable(final BufferedDataTable table, final ExecutionContext exec)
        throws InterruptedException, DuplicateKeyException, IOException, CanceledExecutionException {
        DataTableRowInput rowInput = new DataTableRowInput(table);
        addTable(rowInput, exec);
        rowInput.close();
    }

    /**
     * All rows of the given row input are added to a new data container. Creates a new data container if this data
     * table spec differs from the previous table. This method call checks for row keys duplicates and throws a
     * {@link DuplicateKeyException}.
     *
     * @param table the table to be added
     * @param exec the execution context to possibly create a new data container
     * @throws InterruptedException
     * @throws IOException
     * @throws DuplicateKeyException
     * @throws CanceledExecutionException
     */
    void addTable(final RowInput table, final ExecutionContext exec) throws InterruptedException, DuplicateKeyException, IOException, CanceledExecutionException {

        //check if last container has been closed (i.e. createTable was called)
        if (m_tables.size() > 0) {
            if (m_tables.get(m_tables.size() - 1).isClosed()) {
                throw new IllegalStateException(
                    "No more tables can be added! ConcatenateTable has already been created.");
            }
        }

        //poll first row in order to check whether the incoming table is empty
        DataRow row = table.poll();
        if(row == null) {
            //table is empty
            if(m_ignoreEmptyTables && m_tables.size() > 0) {
                m_iterationCount++;
                return;
            } else if(m_tables.size() == 0){
                //if this is the first table we receive and its empty, create an empty one and keep it
                m_emptyTable = exec.createDataContainer(createSpec(table.getDataTableSpec(), m_addIterationColumn, false));
                m_iterationCount++;
                return;
            }
        }

        //compare spec of the current table with the spec of the first table if changing specs are not tolerated
        if (!m_tolerateChangingSpecs && (m_tables.size() > 0 || m_emptyTable != null)) {
            if (!(m_ignoreEmptyTables && (row == null || m_emptyTable !=null))) {//don't fail if table is empty and to be ignored
                //create spec for comparision -> set the most common column type for both table spec, if altered column types
                //are to be tolerated
                DataTableSpec tmpSpec1;
                if(m_tables.size() == 0 && m_emptyTable!=null) {
                    tmpSpec1 = createSpec(m_emptyTable.getTableSpec(), false, m_tolerateColumnTypes);
                } else {
                    tmpSpec1 = createSpec(m_tables.get(0).getTableSpec(), false, m_tolerateColumnTypes);
                }
                DataTableSpec tmpSpec2 =
                    createSpec(table.getDataTableSpec(), m_addIterationColumn, m_tolerateColumnTypes);
                //fail if specs has been changed
                compareSpecsAndFail(tmpSpec1, tmpSpec2);
            }
        }

        //if table is empty and they are not to be ignored, nothing else to do -> return now
        if(row == null) {
            m_iterationCount++;
            return;
        }

        //if there are too much tables -> create one new and copy the whole data
        if(m_tables.size() > MAX_NUM_TABLES) {
            copyTablesIntoOneTable(exec);
        }

        //create a new data container except the previously added has the same data table spec -> problem: if in each iteration a new row is added we
        //end up with quite many data containers
        BufferedDataContainer con;
        DataTableSpec newTableSpec = createSpec(table.getDataTableSpec(), m_addIterationColumn, false);
        if (m_tables.size() == 0) {
            con = exec.createDataContainer(newTableSpec);
            m_tables.add(con);
        } else if (m_tables.size() > 0 && !newTableSpec.equalStructure(m_tables.get(m_tables.size() - 1).getTableSpec())) {
            con = m_tables.get(m_tables.size() - 1);
            con.close();
            con = exec.createDataContainer(newTableSpec);
            m_tables.add(con);
        } else {
            con = m_tables.get(m_tables.size() - 1);
        }

        //add rows of the table to the newly created data container
        do {
            exec.checkCanceled();
            //change row key if desired
            if (m_rowKeyCreator != null) {
                //change row key
                row = new BlobSupportDataRow(m_rowKeyCreator.apply(row.getKey()),row);
            }
            m_duplicateChecker.addKey(row.getKey().toString());

            //add additional iteration column if desired
            if(m_addIterationColumn) {
                IntCell currIterCell = new IntCell(m_iterationCount);
                row = new org.knime.core.data.append.AppendedColumnRow(row, currIterCell);
            }
            con.addRowToTable(row);
        } while ((row = table.poll()) != null);

        m_iterationCount++;
    }

    /**
     * Finally creates the {@link ConcatenateTable}. All data containers will be closed and no more tables can be added
     * to the factory after this method call.
     *
     * @return creates and returns a table that wraps all the previously added tables.
     * @throws CanceledExecutionException
     * @throws IOException
     * @throws DuplicateKeyException
     */
    BufferedDataTable createTable(final ExecutionContext exec) throws CanceledExecutionException, DuplicateKeyException, IOException {

    	//return at least the empty table if thats the only one that is available
        if(m_tables.size() == 0 && m_emptyTable != null) {
            m_emptyTable.close();
            return m_emptyTable.getTable();
        }

        m_duplicateChecker.checkForDuplicates();

        //close last used table
        m_tables.get(m_tables.size() - 1).close();
        BufferedDataTable[] res = new BufferedDataTable[m_tables.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = m_tables.get(i).getTable();
        }
        //don't check for duplicates since this already has been done
        return exec.createConcatenateTable(exec, Optional.empty(), false, res);
    }

    /**
     * Extends the given table spec by either appending another iteration column or setting the super columns types
     * (according to the flags given in the constructor).
     *
     * @param inSpec
     * @param addIterationCol adds the column spec of the appended iteration column
     * @param withMostCommonColTypes creates a spec where all columns have the most common data type
     * @return the data table spec possibly modified (e.g. an appended iteration column or more general column types)
     */
    static DataTableSpec createSpec(final DataTableSpec inSpec, final boolean addIterationCol, final boolean withMostCommonColTypes) {
        final DataTableSpec outSpec;
        if (withMostCommonColTypes) {
            DataColumnSpec[] commonSpecs = new DataColumnSpec[inSpec.getNumColumns()];
            for (int i = 0; i < commonSpecs.length; i++) {
                DataColumnSpecCreator cr = new DataColumnSpecCreator(inSpec.getColumnSpec(i));
                // init with most common types
                cr.setType(DataType.getType(DataCell.class));
                commonSpecs[i] = cr.createSpec();
            }
            outSpec = new DataTableSpec(commonSpecs);
        } else {
            outSpec = inSpec;
        }
        if (addIterationCol) {
            DataColumnSpecCreator crea =
                new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(outSpec, "Iteration"), IntCell.TYPE);
            if(withMostCommonColTypes) {
                crea.setType(DataType.getType(DataCell.class));
            }
            DataTableSpec newSpec = new DataTableSpec(crea.createSpec());
            return new DataTableSpec(outSpec, newSpec);
        } else {
            return outSpec;
        }
    }

    /** Compares the given specs and fails with an IllegalArgException if not equal*/
    private void compareSpecsAndFail(final DataTableSpec firstIterSpec, final DataTableSpec newTableSpec) {
        if (!newTableSpec.equalStructure(firstIterSpec)) {
            StringBuilder error =
                new StringBuilder("Input table's structure differs from reference (first iteration) table: ");
            if (newTableSpec.getNumColumns() != firstIterSpec.getNumColumns()) {
                error.append("different column counts ");
                error.append(newTableSpec.getNumColumns());
                error.append(" vs. ").append(firstIterSpec.getNumColumns());
            } else {
                for (int i = 0; i < newTableSpec.getNumColumns(); i++) {
                    DataColumnSpec inCol = newTableSpec.getColumnSpec(i);
                    DataColumnSpec predCol = firstIterSpec.getColumnSpec(i);
                    if (!inCol.equalStructure(predCol)) {
                        error.append("Column ").append(i).append(" [");
                        error.append(inCol).append("] vs. [");
                        error.append(predCol).append("]");
                    }
                }
            }
            throw new IllegalArgumentException(error.toString());
        }
    }

    /** Copies all tables, except the last still not-closed table, into an entire new table */
    private void copyTablesIntoOneTable(final ExecutionContext exec) throws CanceledExecutionException {
        BufferedDataTable[] tables = new BufferedDataTable[m_tables.size()-1];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = m_tables.get(i).getTable();
        }
        AppendedRowsTable wrapper = new AppendedRowsTable(org.knime.core.data.append.AppendedRowsTable.DuplicatePolicy.Fail, null, tables);
        BufferedDataContainer con = exec.createDataContainer(wrapper.getDataTableSpec());
        RowIterator rowIt = wrapper.iterator();
        exec.setProgress("Too many tables. Copy tables into one table.");
        while(rowIt.hasNext()) {
            exec.checkCanceled();
            con.addRowToTable(rowIt.next());
        }
        con.close();
        BufferedDataContainer last = m_tables.get(m_tables.size()-1);
        m_tables.clear();
        m_tables.add(con);
        m_tables.add(last);
        exec.setProgress("Tables copied into one.");
    }
}
