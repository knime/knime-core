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
 *   Jul 23, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.columntogrid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.data.sort.SortedTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * Model for Column-to-Grid node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnToGridNodeModel extends org.knime.core.node.NodeModel {

    private ColumnToGridConfiguration m_configuration;
    private final HiLiteHandler m_hiliteHandler;

    /**
     * Constructor, one in, one out.
     */
    protected ColumnToGridNodeModel() {
        super(1, 1);
        m_hiliteHandler = new HiLiteHandler();
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];
        return new DataTableSpec[] {createOutputSpec(spec)};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        String[] includes = m_configuration.getIncludes();
        String groupColumn = m_configuration.getGroupColumn();
        final ExecutionMonitor mainExec;
        final BufferedDataTable inputTable;
        if (groupColumn != null) {
            exec.setMessage("Sorting input table");
            BufferedDataTable in = inData[0];
            ExecutionContext sortExec = exec.createSubExecutionContext(0.5);
            ColumnRearranger sortFilterRearranger =
                new ColumnRearranger(in.getDataTableSpec());
            String[] relevantCols = new String[includes.length + 1];
            System.arraycopy(includes, 0, relevantCols, 0, includes.length);
            relevantCols[relevantCols.length - 1] = groupColumn;
            sortFilterRearranger.keepOnly(relevantCols);
            BufferedDataTable toBeSortedTable = exec.createColumnRearrangeTable(
                    in, sortFilterRearranger, exec.createSubProgress(0.0));
            SortedTable sorter = new SortedTable(toBeSortedTable,
                    Collections.singletonList(groupColumn),
                    new boolean[] {true}, sortExec);
            inputTable = sorter.getBufferedDataTable();
            mainExec = exec.createSubProgress(0.5);
        } else {
            inputTable = inData[0];
            mainExec = exec;
        }
        exec.setMessage("Assembling output");
        DataTableSpec spec = inputTable.getDataTableSpec();
        DataTableSpec outSpec = createOutputSpec(spec);
        BufferedDataContainer cont = exec.createDataContainer(outSpec);

        int[] includeIndices = new int[includes.length];
        for (int i = 0; i < includes.length; i++) {
            int index = spec.findColumnIndex(includes[i]);
            includeIndices[i] = index;
        }
        int gridCount = m_configuration.getColCount();

        final int cellCount;
        final int groupColIndex;
        if (groupColumn != null) {
            cellCount = includeIndices.length * gridCount + 1;
            groupColIndex = spec.findColumnIndex(groupColumn);
        } else {
            cellCount = includeIndices.length * gridCount;
            groupColIndex = -1;
        }
        final DataCell[] cells = new DataCell[cellCount];
        PushBackRowIterator it = new PushBackRowIterator(inputTable.iterator());
        int currentRow = 0;
        int totalRows = inputTable.getRowCount();
        int currentOutRow = 0;
        DataCell curGroupValue = null;
        while (it.hasNext()) {
            Arrays.fill(cells, DataType.getMissingCell());
            // assign group column (if enabled)
            if (groupColIndex >= 0) {
                DataRow row = it.next();
                curGroupValue = row.getCell(groupColIndex);
                cells[cells.length - 1] = curGroupValue;
                it.pushBack(row);
            }
            for (int grid = 0; grid < gridCount; grid++) {
                if (!it.hasNext()) {
                    break;
                }
                DataRow inRow = it.next();
                DataCell groupValue = groupColIndex < 0 ? null
                        : inRow.getCell(groupColIndex);
                if (ConvenienceMethods.areEqual(curGroupValue, groupValue)) {
                    mainExec.setProgress(currentRow / (double)totalRows,
                            "Processing row " + currentRow + "/" + totalRows
                            + ": " + inRow.getKey());
                    currentRow += 1;
                    mainExec.checkCanceled();
                    for (int i = 0; i < includeIndices.length; i++) {
                        cells[grid * includeIndices.length + i] =
                            inRow.getCell(includeIndices[i]);
                    }
                } else {
                    // start new group, i.e. new row
                    it.pushBack(inRow);
                    break;
                }
            }
            RowKey key = RowKey.createRowKey(currentOutRow++);
            cont.addRowToTable(new DefaultRow(key, cells));
        }
        cont.close();
        return new BufferedDataTable[] {cont.getTable()};
    }

    private DataTableSpec createOutputSpec(
            final DataTableSpec spec) throws InvalidSettingsException {
        if (m_configuration == null) {
            m_configuration = new ColumnToGridConfiguration(spec);
            setWarningMessage("Guessed \"" + Arrays.toString(
                    m_configuration.getIncludes()) + "\" as target column");
        }
        String[] includes = m_configuration.getIncludes();
        if (includes == null || includes.length == 0) {
            throw new InvalidSettingsException("No column(s) selected");
        }
        DataColumnSpec[] inColSpecs = new DataColumnSpec[includes.length];
        for (int i = 0; i < includes.length; i++) {
            String s = includes[i];
            DataColumnSpec c = spec.getColumnSpec(s);
            if (c == null) {
                throw new InvalidSettingsException("No such column: " + s);
            }
            inColSpecs[i] = c;
        }
        DataColumnSpec groupColumn = null;
        String groupColumnName = m_configuration.getGroupColumn();
        if (groupColumnName != null) {
            groupColumn = spec.getColumnSpec(groupColumnName);
            if (groupColumn == null) {
                throw new InvalidSettingsException("Group colunmn \""
                        + groupColumnName + "\" not present in input table");
            }
        }
        int gridCount = m_configuration.getColCount();
        Set<String> cols = new HashSet<String>();
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
        for (int grid = 0; grid < gridCount; grid++) {
            for (int i = 0; i < includes.length; i++) {
                DataColumnSpec in = inColSpecs[i];
                String name = in.getName() + " (" + grid + ")";
                String colName = getUniqueColumn(name, cols);
                DataColumnSpecCreator newSpecC = new DataColumnSpecCreator(in);
                newSpecC.setName(colName);
                newSpecC.removeAllHandlers();
                colSpecs.add(newSpecC.createSpec());
            }
        }
        if (groupColumn != null) {
            colSpecs.add(groupColumn);
        }
        DataColumnSpec[] columnArray =
            colSpecs.toArray(new DataColumnSpec[colSpecs.size()]);
        return new DataTableSpec(spec.getName(), columnArray);
    }

    private static String getUniqueColumn(final String baseName,
            final Set<String> nameHash) {
        String colName = baseName;
        int uniquifier = 1;
        while (!nameHash.add(colName)) {
            colName = baseName + " (" + (uniquifier++) + ")";
        }
        return colName;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.saveSettingsTo(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new ColumnToGridConfiguration().loadSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ColumnToGridConfiguration config = new ColumnToGridConfiguration();
        config.loadSettings(settings);
        m_configuration = config;
    }

    /** {@inheritDoc} */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hiliteHandler;
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** @return new settings model used in model and dialog. */
    static final SettingsModelFilterString createColFilterModel() {
        return new SettingsModelFilterString(
                "include_cols", new String[0], new String[0]);
    }

    /** @return new settings model used in model and dialog. */
    static final SettingsModelIntegerBounded createGridColCountModel() {
        return new SettingsModelIntegerBounded(
                "grid_col_count", 4, 1, Integer.MAX_VALUE);
    }

    private static final class PushBackRowIterator extends RowIterator {

        private final RowIterator m_delegate;
        private DataRow m_lastReturned;
        /**
         *
         */
        PushBackRowIterator(final RowIterator delegate) {
            m_delegate = delegate;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return m_lastReturned != null || m_delegate.hasNext();
        }

        /** {@inheritDoc} */
        @Override
        public DataRow next() {
            if (m_lastReturned != null) {
                DataRow lastReturned = m_lastReturned;
                m_lastReturned = null;
                return lastReturned;
            } else {
                return m_delegate.next();
            }
        }

        public void pushBack(final DataRow row) {
            m_lastReturned = row;
        }


    }

}
