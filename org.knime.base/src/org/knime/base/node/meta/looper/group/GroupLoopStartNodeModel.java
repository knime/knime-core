/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 *   10.05.2012 (kilian): created
 */
package org.knime.base.node.meta.looper.group;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.DuplicateKeyException;

/**
 * The node model of the group loop start node. Optionally sorting data and
 * looping over the groups. Groups are build based on specified columns.
 * Sorting can be switched. In this case an already properly sorted input table
 * is required. If sorting is switched off but input table is not properly
 * sorted an error will occur (Exception thrown).
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class GroupLoopStartNodeModel extends NodeModel implements
        LoopStartNodeTerminator, BufferedDataTableHolder {

    /**
     * The default "sorted input table" setting.
     */
    public static final boolean DEF_SORTED_INPUT_TABLE = false;

    /**
     * The separator to separate groups in group identifier.
     */
    public static final String GROUP_SEPARATOR = "%";

    private final SettingsModelColumnFilter2 m_filterGroupColModel =
        GroupLoopStartNodeDialog.getFilterDoubleColModel();

    private final SettingsModelBoolean m_sortedInputTableModel =
        GroupLoopStartNodeDialog.getSortedInputTableModel();

    // loop invariants
    private BufferedDataTable m_table;
    private BufferedDataTable m_sortedTable;
    private CloseableRowIterator m_iterator;
    private DataTableSpec m_spec;
    private int[] m_includedColIndices;
    private DuplicateChecker m_duplicateChecker;

    // loop variants
    private int m_iteration;

    private DataRow m_lastRow;
    private GroupingState m_currentGroupingState;
    private GroupingState m_lastGroupingState;

    private boolean m_isFinalRow = false;
    private boolean m_endLoop = false;

    /**
     * Creates a new model.
     */
    public GroupLoopStartNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        m_spec = inSpecs[0];

        // check if all included columns are available in the spec
        List<String> includedColNames =
                Arrays.asList(m_filterGroupColModel.applyTo(m_spec)
                        .getIncludes());

        // at least one column containing double values must be specified
        if (includedColNames.size() <= 0) {
            throw new InvalidSettingsException(
                    "Select at least one column containing group information!");
        }

        for (String colName : includedColNames) {
            if (!m_spec.containsName(colName)) {
                throw new InvalidSettingsException("Column \"" + colName
                        + "\" is not available!");
            }
        }

        assert m_iteration == 0;
        pushFlowVariableInt("currentIteration", m_iteration);
        initGroupColumnsAsFlowVariables();
        pushFlowVariableString("groupIdentifier", "");
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        ///////////////////////////
        //
        /// DATA TABLES (SORTING)
        //
        ///////////////////////////
        BufferedDataTable table = inData[0];
        DataTableSpec spec = table.getDataTableSpec();
        if (table.getRowCount() <= 0) {
            m_endLoop = true;
        }

        // parameters
        m_includedColIndices = getIncludedColIndices(table.getDataTableSpec());
        boolean checkDuplicates = m_sortedInputTableModel.getBooleanValue();

        // remember table and sort table if necessary
        if (m_iteration == 0) {
            assert getLoopEndNode() == null : "1st iteration but end node set";
            m_table = table;
            m_spec = m_table.getDataTableSpec();

            // sort if not already sorted
            if (!m_sortedInputTableModel.getBooleanValue()) {
                // asc
                final String[] includes = m_filterGroupColModel.applyTo(spec).getIncludes();
                boolean[] sortAsc = new boolean[includes.length];
                Arrays.fill(sortAsc, true);
                BufferedDataTableSorter tableSorter =
                    new BufferedDataTableSorter(table,
                            Arrays.asList(includes), sortAsc, false);
                m_sortedTable = tableSorter.sort(exec);
            } else {
                // no sort necessary
                m_sortedTable = table;
            }

            m_iterator = m_sortedTable.iterator();
        } else {
            assert getLoopEndNode() != null : "No end node set";
            assert table == m_table : "Input tables differ between iterations";
        }


        ///////////////////////////
        //
        /// INIT
        //
        ///////////////////////////
        BufferedDataContainer cont = exec.createDataContainer(table.getSpec());

        // create new duplicate checker if null
        if (m_duplicateChecker == null) {
            m_duplicateChecker = new DuplicateChecker();
        }
        // initialize grouping states if null
        if (m_currentGroupingState == null) {
            m_currentGroupingState = new GroupingState("", false, null);
        }
        m_lastGroupingState = m_currentGroupingState;


        ///////////////////////////
        //
        /// GROUPING / ROW COLLECTION
        //
        ///////////////////////////

        // if there is one last row left, which did not fit in the last group
        // add now to new group
        if (m_lastRow != null) {
            cont.addRowToTable(m_lastRow);
        }
        // if the final row has been reached and added set end loop flag
        if (m_isFinalRow) {
            m_endLoop = true;
        }

        // walk trough input table and group data
        // as long as new row fits into the current group or there are no more
        // rows left.
        boolean groupEnd = false;
        while (!groupEnd && m_iterator.hasNext()) {
            DataRow row = m_iterator.next();

            // get grouping state according to new row
            m_currentGroupingState = getGroupingState(row);
            groupEnd = m_currentGroupingState.isGroupEnd();

            // if first row in table remember grouping state and add identifier
            // to duplicate checker.
            if (m_lastRow == null) {
                m_lastGroupingState = m_currentGroupingState;
                if (checkDuplicates) {
                    m_duplicateChecker.addKey(
                            m_currentGroupingState.getGroupIdentifier());
                }
            }
            m_lastRow = row;

            // if group end has not been reached add row
            if (!groupEnd) {
                cont.addRowToTable(row);
                m_lastGroupingState = m_currentGroupingState;

            // if group end has been reached add identifier of new group to
            // duplicate checker
            } else {
                if (checkDuplicates) {
                    try {
                        m_duplicateChecker.addKey(
                            m_currentGroupingState.getGroupIdentifier());
                    } catch (DuplicateKeyException e) {
                        throw new DuplicateKeyException("Input table was "
                             + "not sorted, found duplicate (group identifier:"
                             + m_currentGroupingState.getGroupIdentifier()
                             + ")");
                    }
                }
            }

            // if current row was the final row of an additional group it has
            // not been added so far. A final iteration needs to be done in
            // which row will be added.
            if (!m_iterator.hasNext() && !m_isFinalRow) {
                m_isFinalRow = true;

                // if group end is not reached, row has been already added
                // thus end loop
                if (!groupEnd) {
                    m_endLoop = true;
                }
            }
        }
        cont.close();

        // check for duplicates and throw exception if duplicate exist
        try {
            m_duplicateChecker.checkForDuplicates();
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("Input table was "
                 + "not sorted, found duplicate (group identifier:"
                 + m_currentGroupingState.getGroupIdentifier()
                 + ")");
        }

        // push variables
        pushFlowVariableInt("currentIteration", m_iteration);
        pushGroupColumnValuesAsFlowVariables(m_lastGroupingState);
        pushFlowVariableString("groupIdentifier",
                m_lastGroupingState.getGroupIdentifier());
        m_iteration++;

        return new BufferedDataTable[] {cont.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        boolean continueLoop = m_iterator == null || !m_endLoop;
        return !continueLoop;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_iterator != null) {
            m_iterator.close();
            m_iterator = null;
        }
        if (m_duplicateChecker != null) {
            m_duplicateChecker.clear();
            m_duplicateChecker = null;
        }

        m_iteration = 0;
        m_table = null;
        m_sortedTable = null;
        m_lastRow = null;
        m_spec = null;
        m_includedColIndices = null;

        m_endLoop = false;
        m_isFinalRow = false;

        m_lastGroupingState = null;
        m_currentGroupingState = null;
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getInternalTables() {
        if (!m_endLoop) {
            return new BufferedDataTable[] {m_sortedTable};
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        // ignore, can't persist loop start anyway
    }

    /**
     * Pushed the values of the specified grouping state as flow variables.
     *
     * @param gs The grouping state holding the variables (cells) to push.
     */
    private void pushGroupColumnValuesAsFlowVariables(final GroupingState gs) {
        if (gs != null) {
            DataCell[] cells = gs.getGroupCells();
            if (cells != null) {
                for (int i = 0; i < cells.length; i++) {
                    DataCell c = cells[i];
                    int j = m_includedColIndices[i];
                    pushVariable(c.getType(), m_spec.getColumnSpec(j).getName(),
                            c);
                }
            }
        }
    }

    /**
     * Pushed initial values for each group column as flow variables.
     */
    private void initGroupColumnsAsFlowVariables() {
        if (m_spec != null && m_filterGroupColModel != null) {
            List<String> inclCols = Arrays.asList(m_filterGroupColModel.applyTo(
                    m_spec).getIncludes());
            for (String colName : inclCols) {
                DataType dt = m_spec.getColumnSpec(colName).getType();
                pushVariable(dt, m_spec.getColumnSpec(colName).getName(), null);
            }
        }
    }

    /**
     * Pushes a certain flow variable based on the specified data type.
     *
     * @param type The type of the variable to push.
     * @param name The name of the variable to push.
     * @param c The value of the variable to push.
     */
    private void pushVariable(final DataType type, final String name,
            final DataCell c) {
        DataType dt = type;
        if (c != null) {
            dt = c.getType();
        }

        if (dt.isCompatible(IntValue.class)) {
            int value = 0;
            if (c != null) {
                if (!c.isMissing()) {
                    value = ((IntValue)c).getIntValue();
                }
            }
            pushFlowVariableInt(name, value);
        } else if (dt.isCompatible(DoubleValue.class)) {
            double value = 0.0;
            if (c != null) {
                if (!c.isMissing()) {
                    value = ((DoubleValue)c).getDoubleValue();
                }
            }
            pushFlowVariableDouble(name, value);
        } else {
            String value = "";
            if (c != null) {
                if (!c.isMissing()) {
                    value = c.toString();
                }
            }
            pushFlowVariableString(name, value);
        }
    }

    private GroupingState getGroupingState(final DataRow row) {
        // sanity checks
        if (row == null) {
            throw new IllegalArgumentException(
                    "Row to check for group end may not be null!");
        }
        if (m_includedColIndices == null) {
            throw new IllegalStateException(
                    "Indices of included columns may not be null!");
        }
        if (m_spec == null) {
            throw new IllegalStateException("Data table spec may not be null!");
        }

        // check for end of group and create group identifier
        boolean isGroupEnd = false;
        String groupIdentifier = "";
        DataCell[] groupCells = new DataCell[m_includedColIndices.length];

        int currIncludedColIndex = 0;
        int nextIncludedColIndex = m_includedColIndices[currIncludedColIndex];

        // walk through all columns, compare values and update grouping
        // identifier
        for (int c = 0; c < row.getNumCells(); c++) {

            // if column is included for grouping
            if (c == nextIncludedColIndex) {
                // compare only if last row exists
                if (m_lastRow != null) {
                    DataCell lastCell = m_lastRow.getCell(c);
                    DataCell newCell = row.getCell(c);

                    // compare last and new values, if one value differs, group
                    // end is reached
                    if (m_spec.getColumnSpec(c).getType().getComparator()
                            .compare(lastCell, newCell) != 0) {
                        isGroupEnd = true;
                    }
                }

                groupIdentifier += GROUP_SEPARATOR + row.getCell(c).toString()
                                 + GROUP_SEPARATOR;

                // get current group cell
                groupCells[currIncludedColIndex] = row.getCell(c);

                // get index of next included column
                currIncludedColIndex++;
                if (currIncludedColIndex < m_includedColIndices.length) {
                    nextIncludedColIndex =
                        m_includedColIndices[currIncludedColIndex];
                }
            }
        }

        return new GroupingState(groupIdentifier, isGroupEnd, groupCells);
    }

    /**
     * Creates and returns an array containing the indices of the included
     * columns in the input data table spec.
     *
     * @param dataSpec The input data table spec.
     * @return An array containing the indices of the included columns.
     */
    private int[] getIncludedColIndices(final DataTableSpec dataSpec) {
        List<String> includedColNames = Arrays.asList(
                m_filterGroupColModel.applyTo(dataSpec).getIncludes());
        int[] includedColIndices = new int[includedColNames.size()];
        int noCols = dataSpec.getNumColumns();
        int j = 0;
        for (int i = 0; i < noCols; i++) {
            String currColName = dataSpec.getColumnSpec(i).getName();
            if (includedColNames.contains(currColName)) {
                includedColIndices[j] = i;
                j++;
            }
        }
        return includedColIndices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_filterGroupColModel.saveSettingsTo(settings);
        m_sortedInputTableModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_filterGroupColModel.validateSettings(settings);
        m_sortedInputTableModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_filterGroupColModel.loadSettingsFrom(settings);
        m_sortedInputTableModel.loadSettingsFrom(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }

    /**
     * Encapsulates the grouping state, consisting of the group identifier and
     * a flag specifying whether group has been reached or not.
     *
     * @author Kilian Thiel, KNIME.com, Berlin, Germany
     */
    private class GroupingState {
        private String m_groupIdentifier;
        private boolean m_groupEnd;
        private DataCell[] m_cells;

        public GroupingState(final String groupIdentifier,
                final boolean groupEnd, final DataCell[] cells) {
            m_groupIdentifier = groupIdentifier;
            m_groupEnd = groupEnd;
            m_cells = cells;
        }

        public boolean isGroupEnd() {
            return m_groupEnd;
        }

        public String getGroupIdentifier() {
            return m_groupIdentifier;
        }

        public DataCell[] getGroupCells() {
            return m_cells;
        }
    }
}
