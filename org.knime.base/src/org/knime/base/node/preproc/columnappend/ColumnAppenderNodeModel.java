/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */
package org.knime.base.node.preproc.columnappend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.util.UniqueNameGenerator;

/**
 * This is the model implementation of ColumnAppender. A fast way to reverse the operation of a splitter noded.
 *
 * @author Aaron Hart, Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Martin Horn, University of Konstanz
 */
final class ColumnAppenderNodeModel extends NodeModel {


    static SettingsModelBoolean createWrapTableModel() {
        return new SettingsModelBoolean("wrap_table", true);
    }

    static final String[] ROW_KEY_SELECT_OPTIONS = {"Use row keys from FIRST table", "Use row keys from SECOND table", "Generate new row keys"};

    static SettingsModelString createRowKeySelectModel() {
        return new SettingsModelString("row_key_select", ROW_KEY_SELECT_OPTIONS[0]);
    }


    /* settings whether the data table should either be wrapped or newly created */
    private final SettingsModelBoolean m_wrapTable;

    /* settings that determine from what table the row keys are to be used*/
    private final SettingsModelString m_rowKeySelect;

    /**
     * Constructor for the node model.
     */
    ColumnAppenderNodeModel() {
        super(2, 1);
        //initialize settings models
        m_wrapTable = createWrapTableModel();
        m_rowKeySelect = createRowKeySelectModel();
        m_rowKeySelect.setEnabled(!m_wrapTable.getBooleanValue());
        m_wrapTable.addChangeListener(l -> m_rowKeySelect.setEnabled(!m_wrapTable.getBooleanValue()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        DataTableSpec[] inSpecs = {inData[0].getDataTableSpec(), inData[1].getDataTableSpec()};
        DataTableSpec newSpec = createOutSpec(inSpecs);

        BufferedDataTable out;
        if (m_wrapTable.getBooleanValue()) {
            BufferedDataTable uniquifiedTable = exec.createSpecReplacerTable(inData[1], newSpec);
            out = exec.createJoinedTable(inData[0], uniquifiedTable, exec);
        } else {
            //create a new table and fill the rows accordingly

            BufferedDataContainer container =
                exec.createDataContainer(new DataTableSpec(inSpecs[0], createOutSpec(inSpecs)));

            CustomRowIterator tableIt1 = new CustomRowIteratorImpl1(inData[0].iterator());
            CustomRowIterator tableIt2 = new CustomRowIteratorImpl1(inData[1].iterator());


            //combine rows
            compute(tableIt1, tableIt2, inSpecs[0].getNumColumns() + inSpecs[1].getNumColumns(),
                row -> container.addRowToTable(row), exec, inData[0].size(), inData[1].size());

            container.close();
            out = container.getTable();
        }
        return new BufferedDataTable[]{out};
    }

    /* combines the rows in case a new table is created */
    private void compute(final CustomRowIterator rowIt1, final CustomRowIterator rowIt2, final int numColsTotal,
        final RowConsumer output, final ExecutionContext exec, final long numRowsTab1, final long numRowsTab2) throws InterruptedException, CanceledExecutionException {

        boolean useRowKeysFromFirstTable = m_rowKeySelect.getStringValue().equals(ROW_KEY_SELECT_OPTIONS[0]);
        boolean useRowKeysFromSecondTable = m_rowKeySelect.getStringValue().equals(ROW_KEY_SELECT_OPTIONS[1]);
        boolean generateRowKeys = m_rowKeySelect.getStringValue().equals(ROW_KEY_SELECT_OPTIONS[2]);

        long rowCount = 0;
        long numRows;
        if (numRowsTab1 != -1) {
            numRows = useRowKeysFromFirstTable ? numRowsTab1
                : (useRowKeysFromSecondTable ? numRowsTab2 : Math.max(numRowsTab1, numRowsTab2));
        } else {
            numRows = -1;
        }
        while (rowIt1.hasNext() && rowIt2.hasNext()) {
            if (numRows != -1) {
                exec.setProgress(rowCount / (double)numRows);
                final long rowCountFinal = rowCount;
                exec.setMessage(() -> "Appending columns (row " + rowCountFinal + "/" + numRows + ")");
            }
            exec.checkCanceled();

            DataRow row1 = rowIt1.next();
            DataRow row2 = rowIt2.next();
            if (m_wrapTable.getBooleanValue() && !row1.getKey().equals(row2.getKey())) {
                errorDifferingRowKeys(rowCount, row1.getKey(), row2.getKey());
            }
            ArrayList<DataCell> cells = new ArrayList<DataCell>(numColsTotal);
            for (DataCell cell : row1) {
                cells.add(cell);
            }
            for (DataCell cell : row2) {
                cells.add(cell);
            }
            DefaultRow res;
            if (useRowKeysFromFirstTable) {
                res = new DefaultRow(row1.getKey(), cells);
            } else if(useRowKeysFromSecondTable) {
                res = new DefaultRow(row2.getKey(), cells);
            } else {
                res = new DefaultRow("Row" + (rowCount), cells);
            }
            output.consume(res);
            rowCount++;

        }

        /* --add missing cells if row counts mismatch --*/
        long extraRowsTab1 = 0;
        while (((rowIt1.hasNext() && useRowKeysFromFirstTable) || (rowIt1.hasNext() && generateRowKeys)) && !rowIt2.hasNext()) {
            if (numRows != -1) {
                exec.setProgress((rowCount + extraRowsTab1) / (double)numRows);
                final long rowCountFinal = rowCount + extraRowsTab1;
                exec.setMessage(() -> "Appending columns (row " + rowCountFinal + "/" + numRows + ")");
            }
            exec.checkCanceled();

            DataRow row = rowIt1.next();
            ArrayList<DataCell> cells = new ArrayList<DataCell>(numColsTotal);
            for (DataCell cell : row) {
                cells.add(cell);
            }
            for (int i = 0; i < numColsTotal - row.getNumCells(); i++) {
                cells.add(DataType.getMissingCell());
            }

            DefaultRow res;
            if (generateRowKeys) {
                res = new DefaultRow("Row" + (rowCount + extraRowsTab1), cells);
            } else {
                res = new DefaultRow(row.getKey(), cells);
            }
            output.consume(res);
            extraRowsTab1++;
        }

        long extraRowsTab2 = 0;
        while (((rowIt2.hasNext() && useRowKeysFromSecondTable) || (rowIt2.hasNext() && generateRowKeys)) && !rowIt1.hasNext()) {
            if (numRows != -1) {
                exec.setProgress((rowCount + extraRowsTab2) / (double)numRows);
                final long rowCountFinal = rowCount + extraRowsTab2;
                exec.setMessage(() -> "Appending columns (row " + rowCountFinal + "/" + numRows + ")");
            }
            exec.checkCanceled();

            DataRow row = rowIt2.next();
            ArrayList<DataCell> cells = new ArrayList<DataCell>(numColsTotal);
            for (int i = 0; i < numColsTotal - row.getNumCells(); i++) {
                cells.add(DataType.getMissingCell());
            }
            for (DataCell cell : row) {
                cells.add(cell);
            }
            DefaultRow res;
            if (generateRowKeys) {
                res = new DefaultRow("Row" + (rowCount + extraRowsTab2), cells);
            } else {
                res = new DefaultRow(row.getKey(), cells);
            }
            output.consume(res);
            extraRowsTab2++;
        }

        //set warning messages if missing values have been inserted or one table was truncated
        if(useRowKeysFromFirstTable) {
            if (extraRowsTab1 == 0 && rowIt2.hasNext()) {
                setWarningMessage("First table is shorter than the second table! Second table has been truncated.");
            } else if (extraRowsTab1 > 0) {
                setWarningMessage(
                    "First table is longer than the second table! Missing values have been added to the second table.");
            }
        } else if(useRowKeysFromSecondTable) {
            if (extraRowsTab2 == 0 && rowIt1.hasNext()) {
                setWarningMessage("Second table is shorter than the first table! First table has been truncated.");
            } else if (extraRowsTab2 > 0) {
                setWarningMessage(
                    "Second table is longer than the first table! Missing values have been added to the first table.");
            }
        } else {
            if(extraRowsTab1 > 0 || extraRowsTab2 > 0) {
                setWarningMessage("Both tables differ in length! Missing values have been added accordingly.");
            }
        }

        //throw error messages if the "wrap"-option is set and tables vary in size
        if(m_wrapTable.getBooleanValue()) {
            if (extraRowsTab1 != extraRowsTab2) {
                errorDifferingTableSize(rowCount + extraRowsTab1, rowCount + extraRowsTab2);
            }
        }
    }

    private static void errorDifferingTableSize(final long rowCnt1, final long rowCnt2) {
        throw new IllegalArgumentException("Tables can't be joined, non "
                + "matching row counts: " + rowCnt1 + " vs. "
                + rowCnt2);
    }

    private static void errorDifferingRowKeys(final long rowIndex, final RowKey leftKey, final RowKey rightKey) {
        throw new IllegalArgumentException("Tables contain non-matching rows or are sorted "
            + "differently, keys in row " + rowIndex + " do not match: \"" + leftKey + "\" vs. \"" + rightKey + "\"");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec spec = createOutSpec(inSpecs);
        return new DataTableSpec[]{new DataTableSpec(inSpecs[0], spec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_wrapTable.saveSettingsTo(settings);
        m_rowKeySelect.saveSettingsTo(settings);;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            m_wrapTable.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            //use default settings for backwards-compatibility
        }
        m_rowKeySelect.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            m_wrapTable.validateSettings(settings);
        } catch (InvalidSettingsException e) {
            //use default settings for backwards-compatibility
        }
        m_rowKeySelect.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {

    }

    private static DataTableSpec createOutSpec(final DataTableSpec[] inSpecs) {

        DataColumnSpec[] cspecs = new DataColumnSpec[inSpecs[1].getNumColumns()];

        // Look in the bottom input table spec and uniquify column names
        UniqueNameGenerator nameGenerator = new UniqueNameGenerator(inSpecs[0]);
        for (int i = 0; i < inSpecs[1].getNumColumns(); i++) {
            DataColumnSpec oldSpec = inSpecs[1].getColumnSpec(i);
            cspecs[i] = nameGenerator.newCreator(oldSpec).createSpec();
        }

        DataTableSpec outSpec = new DataTableSpec(cspecs);

        return outSpec;
    }

    //////////////// STREAMING FUNCTIONS ////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {

                RowInput in1 = (RowInput)inputs[0];
                RowInput in2 = (RowInput)inputs[1];

                RowOutput out = (RowOutput)outputs[0];

                CustomRowIterator tableIt1 = new CustomRowIteratorImpl2(in1);
                CustomRowIterator tableIt2 = new CustomRowIteratorImpl2(in2);

                compute(tableIt1, tableIt2,
                    in1.getDataTableSpec().getNumColumns() + in2.getDataTableSpec().getNumColumns(), row -> {
                        out.push(row);
                    }, exec, -1, -1);

                //poll all the remaining rows if there are any but don't do anything with them
                while (tableIt1.hasNext()) {
                    tableIt1.next();
                }
                while (tableIt2.hasNext()) {
                    tableIt2.next();
                }

                in1.close();
                in2.close();
                out.close();
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
            //in-ports are non-distributed since it can't be guaranteed that the chunks at each port are of identical size
            return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_STREAMABLE, InputPortRole.NONDISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
            return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    //////////////// HELPER CLASSES for the compute-method /////////////////////

    static interface CustomRowIterator {

        boolean hasNext() throws InterruptedException;

        DataRow next();
    }

    private static final class CustomRowIteratorImpl1 implements CustomRowIterator {

        private RowIterator m_rowIt;

        CustomRowIteratorImpl1(final RowIterator rowIt) {
            m_rowIt = rowIt;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_rowIt.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            return m_rowIt.next();
        }

    }

    private static final class CustomRowIteratorImpl2 implements CustomRowIterator {

        private RowInput m_rowInput;

        private DataRow m_row = null;

        CustomRowIteratorImpl2(final RowInput rowInput) {
            m_rowInput = rowInput;
        }

        /**
         * {@inheritDoc}
         *
         */
        @Override
        public boolean hasNext() throws InterruptedException {
            //if hasNext() is called multiple times without calling next() in between,
            //this if-clause ensures that it still returns true
            if (m_row == null) {
                m_row = m_rowInput.poll();
            }
            return m_row != null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataRow next() {
            DataRow row = m_row;
            m_row = null;
            return row;
        }

    }

    private static interface RowConsumer {

        void consume(DataRow row) throws InterruptedException;
    }

}
