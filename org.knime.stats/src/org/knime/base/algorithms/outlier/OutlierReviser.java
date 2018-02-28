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
 *   Feb 21, 2018 (ortmann): created
 */
package org.knime.base.algorithms.outlier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.algorithms.outlier.listeners.Warning;
import org.knime.base.algorithms.outlier.listeners.WarningListener;
import org.knime.base.algorithms.outlier.options.OutlierReplacementStrategy;
import org.knime.base.algorithms.outlier.options.OutlierTreatmentOption;
import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * The algorithm to treat outliers based on the permitted intervals provided.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class OutlierReviser {

    /** Suffix of the outlier replacement count column. */
    private static final String REPLACEMENT_COUNT_SUFFIX = " (outlier count)";

    /** Suffix of the member count column. */
    private static final String MEMBER_COUNT_SUFFIX = " (member count)";

    /** Outlier treatment and output generation routine message. */
    private static final String TREATMENT_MSG = "Treating outliers and generating output";

    /** Empty table warning text. */
    private static final String EMPTY_TABLE_WARNING = "Node created an empty data table";

    /** The outlier treatment option. */
    private final OutlierTreatmentOption m_treatment;

    /** The outlier replacement strategy. */
    private final OutlierReplacementStrategy m_repStrategy;

    /** List of listeners receiving warning messages. */
    private final List<WarningListener> m_listeners;

    /** The outlier column names. */
    private String[] m_outlierColNames;

    /** The outlier free table. */
    private BufferedDataTable m_outTable;

    /** The table storing the summary. */
    private BufferedDataTable m_summaryTable;

    /** Tells whether the domains of the outlier columns have to be updated after the computation or not. */
    private final boolean m_updateDomain;

    /** Counter storing the number of outliers per column and group. */
    private GroupsMemberCounterPerColumn m_outlierRepCounter;

    /** Counter storing the number of members (non-missings) per column and group. */
    private GroupsMemberCounterPerColumn m_memberCounter;

    /** Counter storing for each missing group the number of members per column. */
    private GroupsMemberCounterPerColumn m_missingGroupsCounter;

    /**
     * Builder of the OutlierReviser.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public static final class Builder {

        // Optional parameters
        /** The outlier treatment option. */
        private OutlierTreatmentOption m_treatment = OutlierTreatmentOption.REPLACE;

        /** The outlier replacement strategy. */
        private OutlierReplacementStrategy m_repStrategy = OutlierReplacementStrategy.INTERVAL_BOUNDARY;

        /** Tells whether the domains of the outlier columns have to be update after the computation or not. */
        private boolean m_updateDomain = false;

        /**
         * Constructort.
         *
         */
        public Builder() {
        }

        /**
         * Defines how outlier have to be treated, see {@link OutlierTreatmentOption}.
         *
         * @param treatment the treatment option to be used
         * @return the builder itself
         */
        public Builder setTreatmentOption(final OutlierTreatmentOption treatment) {
            m_treatment = treatment;
            return this;
        }

        /**
         * Defines the outlier replacement strategy, see {@link OutlierReplacementStrategy}.
         *
         * @param repStrategy the replacement strategy
         * @return the builder itself
         */
        public Builder setReplacementStrategy(final OutlierReplacementStrategy repStrategy) {
            m_repStrategy = repStrategy;
            return this;
        }

        /**
         * Sets the domain policy flag.
         *
         * @param resetDomain the domain policy
         * @return the builder itself
         */
        public Builder updateDomain(final boolean resetDomain) {
            m_updateDomain = resetDomain;
            return this;
        }

        /**
         * Constructs the outlier reviser using the settings provided by the builder.
         *
         * @return the outlier reviser using the settings provided by the builder
         */
        public OutlierReviser build() {
            return new OutlierReviser(this);
        }
    }

    /**
     * Constructor.
     *
     * @param b the builder
     */
    private OutlierReviser(final Builder b) {
        m_treatment = b.m_treatment;
        m_repStrategy = b.m_repStrategy;
        m_updateDomain = b.m_updateDomain;
        m_listeners = new LinkedList<WarningListener>();
    }

    /**
     * Adds the given listener.
     *
     * @param listener the listener to add
     */
    public void addListener(final WarningListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    /**
     * Returns the outlier free data table.
     *
     * @return the outlier free data table
     */
    public BufferedDataTable getOutTable() {
        return m_outTable;
    }

    /**
     * Returns the spec of the outlier free data table.
     *
     * @param inSpec the spec of the input data table
     * @return the spec of the outlier free data table
     */
    public DataTableSpec getOutTableSpec(final DataTableSpec inSpec) {
        return inSpec;
    }

    /**
     * Returns the data table storing the permitted intervals and additional information about member counts.
     *
     * @return the data table storing the summary
     */
    public BufferedDataTable getSummaryTable() {
        return m_summaryTable;
    }

    /**
     * Returns the spec of the table storing the permitted intervals and additional information about member counts.
     *
     * @param inSpec the spec of the input data table
     * @param outlierOffset offset encoding the first position where the permitted interval columns can be found
     * @param outlierColNames the outlier column names
     * @return the spec of the data table storing the summary
     */
    public static DataTableSpec getSummaryTableSpec(final DataTableSpec inSpec, final int outlierOffset,
        final String[] outlierColNames) {
        return addCountsAndPermute(inSpec, outlierOffset, outlierColNames, null, null).createSpec();
    }

    /**
     * Returns the outlier treatment option.
     *
     * @return the outlier treatment option
     */
    OutlierTreatmentOption getTreatmentOption() {
        return m_treatment;
    }

    /**
     * Returns the outlier replacement strategy.
     *
     * @return the outlier replacement strategy
     */
    OutlierReplacementStrategy getReplacementStrategy() {
        return m_repStrategy;
    }

    /**
     * Returns the update domain flag.
     *
     * @return the update domain flag
     */
    boolean updateDomain() {
        return m_updateDomain;
    }

    /**
     * Clears the input data table of its outliers according to the defined outlier treatment settings.
     *
     * <p>
     * Given that outliers have to be replaced, each of the cells containing an outlier is either replaced by an missing
     * value or set to value of the closest value within the permitted interval. Otherwise all rows containing an
     * outlier are removed from the input data table.
     * </p>
     *
     * @param exec the execution context
     * @param in the data table whose outliers have to be treated
     * @param permIntervalsModel the model storing the permitted intervals
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    public void treatOutliers(final ExecutionContext exec, final BufferedDataTable in,
        final OutlierModel permIntervalsModel) throws CanceledExecutionException {
        // start the treatment step
        exec.setMessage(TREATMENT_MSG);

        // set the outlier column names
        m_outlierColNames = permIntervalsModel.getOutlierColNames();

        // total number of outlier columns
        final int noOutliers = m_outlierColNames.length;

        // counters for the number of non-missing values and outliers contained in each outlier column respective
        // the different groups
        m_outlierRepCounter = new GroupsMemberCounterPerColumn(noOutliers);
        m_memberCounter = new GroupsMemberCounterPerColumn(noOutliers);
        m_missingGroupsCounter = new GroupsMemberCounterPerColumn(noOutliers);

        // the domains updater
        final OutlierDomainsUpdater domainsUpdater = new OutlierDomainsUpdater();

        // treat the outliers with respect to the selected treatment option
        if (m_treatment == OutlierTreatmentOption.REPLACE) {
            // replaces outliers according to the set replacement strategy
            replaceOutliers(exec, in, permIntervalsModel, domainsUpdater);
        } else {
            // we remove all columns containing at least one outlier
            removeRows(exec, in, permIntervalsModel, domainsUpdater);
        }

        // set the summary table, i.e., append the member columns, permute it, and add missing group keys
        writeSummaryTable(exec, permIntervalsModel);

        // reset the domain
        if (m_updateDomain) {
            m_outTable = domainsUpdater.updateDomain(exec, m_outTable);
        }

        // reset the counters
        m_memberCounter = null;
        m_outlierRepCounter = null;
        m_missingGroupsCounter = null;
        m_outlierColNames = null;

        // update progress
        exec.setProgress(1);
    }

    /**
     * Replaces outliers found in the input table according to the selected replacement option. Additionally, the
     * outlier replacement counts and new domains are calculated.
     *
     * @param exec the execution context
     * @param in the input data table
     * @param permIntervalsModel the model storing the permitted intervals
     * @param domainsUpdater the domains updater
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private void replaceOutliers(final ExecutionContext exec, final BufferedDataTable in,
        final OutlierModel permIntervalsModel, final OutlierDomainsUpdater domainsUpdater)
        throws CanceledExecutionException {
        // total number of outlier columns
        final int noOutliers = m_outlierColNames.length;

        // the in table spec
        final DataTableSpec inSpec = in.getDataTableSpec();

        // create column re-arranger to overwrite cells corresponding to outliers
        final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);

        // store the positions where the outlier columns names can be found in the input table
        final int[] outlierIndices = calculateOutlierIndicies(inSpec);

        final DataColumnSpec[] outlierSpecs = new DataColumnSpec[noOutliers];
        for (int i = 0; i < noOutliers; i++) {
            outlierSpecs[i] = inSpec.getColumnSpec(outlierIndices[i]);
        }
        // values are copied anyways by the re-arranger so there is no need to
        // create new instances for each row
        final DataCell[] treatedVals = new DataCell[noOutliers];

        final AbstractCellFactory fac = new AbstractCellFactory(outlierSpecs) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                final GroupKey key = permIntervalsModel.getKey(row, inSpec);
                final Map<String, double[]> colsMap = permIntervalsModel.getGroupIntervals(key);
                for (int i = 0; i < noOutliers; i++) {
                    final DataCell curCell = row.getCell(outlierIndices[i]);
                    final DataCell treatedCell;
                    if (!curCell.isMissing()) {
                        // if the key exists treat the value otherwise we process an unkown group
                        if (colsMap != null) {
                            // increment the member counter
                            m_memberCounter.incrementMemberCount(i, key);
                            // treat the value of the cell if its a outlier
                            treatedCell = treatCellValue(colsMap.get(m_outlierColNames[i]), curCell);
                        } else {
                            m_missingGroupsCounter.incrementMemberCount(i, key);
                            treatedCell = curCell;
                        }
                    } else {
                        treatedCell = curCell;
                    }
                    // if we changed the value this is an outlier
                    if (!treatedCell.equals(curCell)) {
                        m_outlierRepCounter.incrementMemberCount(i, key);
                    }
                    // update the domain if necessary
                    if (m_updateDomain && !treatedCell.isMissing()) {
                        domainsUpdater.updateDomain(m_outlierColNames[i], ((DoubleValue)treatedCell).getDoubleValue());
                    }
                    treatedVals[i] = treatedCell;
                }
                return treatedVals;
            }

        };
        // replace the outlier columns by their updated versions
        colRearranger.replace(fac, outlierIndices);
        m_outTable = exec.createColumnRearrangeTable(in, colRearranger, exec);
        exec.setProgress(0.95);
    }

    /**
     * Calculates the positions where the outlier columns can be found in the input table.
     *
     * @param inSpec the input table spec
     * @return the positions of the outlier columns w.r.t. the input spec
     */
    private int[] calculateOutlierIndicies(final DataTableSpec inSpec) {
        final int[] outlierIndices = new int[m_outlierColNames.length];
        for (int i = 0; i < m_outlierColNames.length; i++) {
            outlierIndices[i] = inSpec.findColumnIndex(m_outlierColNames[i]);
        }
        return outlierIndices;
    }

    /**
     * If necessary the value/type of the data cell is modified in accordance with the selected outlier replacement
     * strategy.
     *
     * @param interval the permitted interval
     * @param cell the the current data cell
     * @return the new data cell after replacing its value if necessary
     */
    private DataCell treatCellValue(final double[] interval, final DataCell cell) {
        double val = ((DoubleValue)cell).getDoubleValue();
        // checks if the value is an outlier
        if (m_repStrategy == OutlierReplacementStrategy.MISSING && (val < interval[0] || val > interval[1])) {
            return DataType.getMissingCell();
        }
        if (cell.getType() == DoubleCell.TYPE) {
            // sets to the lower interval bound if necessary
            val = Math.max(val, interval[0]);
            // sets to the higher interval bound if necessary
            val = Math.min(val, interval[1]);
            return DoubleCellFactory.create(val);
        } else {
            // sets to the lower interval bound if necessary
            // to the smallest integer inside the permitted interval
            val = Math.max(val, Math.ceil(interval[0]));
            // sets to the higher interval bound if necessary
            // to the largest integer inside the permitted interval
            val = Math.min(val, Math.floor(interval[1]));
            // return the proper DataCell
            if (cell.getType() == LongCell.TYPE) {
                return LongCellFactory.create((long)val);
            }
            return IntCellFactory.create((int)val);
        }
    }

    /**
     * Removes rows from the input table that contain outliers. Additionally, the outlier and group related counts, and
     * the new domains are calculated.
     *
     * @param exec the execution context
     * @param in the input data table
     * @param permIntervalsModel the model storing the permitted intervals
     * @param domainsUpdater the domains updater
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private void removeRows(final ExecutionContext exec, final BufferedDataTable in,
        final OutlierModel permIntervalsModel, final OutlierDomainsUpdater domainsUpdater)
        throws CanceledExecutionException {
        // the in spec
        final DataTableSpec inSpec = in.getDataTableSpec();

        // store the positions where the outlier columns names can be found in the input table
        final int[] outlierIndices = calculateOutlierIndicies(inSpec);

        // total number of outlier columns
        final int noOutliers = m_outlierColNames.length;

        // the outlier free data table container
        BufferedDataContainer container = exec.createDataContainer(inSpec);

        // ensure that the max process equals 0.95 and not 1
        final long rowCount = in.size();
        final double divisor = rowCount + 0.05 * rowCount;
        long rowCounter = 1;

        // for each row test if it contains an outlier
        for (final DataRow row : in) {
            exec.checkCanceled();
            final long rowCounterLong = rowCounter++; // 'final' due to access in lambda expression
            exec.setProgress(rowCounterLong / divisor,
                () -> "Testing row " + rowCounterLong + " of " + rowCount + " for outliers");

            // get the group key of the currently processed row
            final GroupKey key = permIntervalsModel.getKey(row, inSpec);
            //get the map holding the permitted intervals for the given groups key
            Map<String, double[]> colsMap = permIntervalsModel.getGroupIntervals(key);
            boolean toInsert = true;
            for (int i = 0; i < noOutliers; i++) {
                final DataCell cell = row.getCell(outlierIndices[i]);
                // if the key is existent check the rows, otherwise increment the missing group counters
                if (colsMap != null) {
                    final double[] interval = colsMap.get(m_outlierColNames[i]);
                    if (!cell.isMissing()) {
                        // increment the member counter
                        m_memberCounter.incrementMemberCount(i, key);
                        final double val = ((DoubleValue)cell).getDoubleValue();
                        if (val < interval[0] || val > interval[1]) {
                            toInsert = false;
                            // increment the outlier counter
                            m_outlierRepCounter.incrementMemberCount(i, key);
                        }
                    }
                } else {
                    if (!cell.isMissing()) {
                        m_missingGroupsCounter.incrementMemberCount(i, key);
                    }
                }
            }
            if (toInsert) {
                container.addRowToTable(row);
                // update the domain if necessary
                if (m_updateDomain) {
                    DataCell cell;
                    for (int i = 0; i < noOutliers; i++) {
                        if (!(cell = row.getCell(outlierIndices[i])).isMissing()) {
                            domainsUpdater.updateDomain(m_outlierColNames[i], ((DoubleValue)cell).getDoubleValue());
                        }
                    }
                }
            }
        }
        container.close();
        m_outTable = container.getTable();
        if (m_outTable.size() == 0) {
            // NodeModel#executeModel only sets the empty table warning if no other warnings were set before
            warnListeners(EMPTY_TABLE_WARNING);
        }
    }

    /**
     * Adds the replacement and member count for each outlier column to the summary table, and renames and permutes
     * columns.
     *
     * @param exec the execution context
     * @param permIntervalsModel the model storing the permitted intervals
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private void writeSummaryTable(final ExecutionContext exec, final OutlierModel permIntervalsModel)
        throws CanceledExecutionException {
        m_summaryTable = exec.createBufferedDataTable(permIntervalsModel.getModel(), exec);

        // permute the interval table
        m_summaryTable = exec.createColumnRearrangeTable(m_summaryTable,
            addCountsAndPermute(m_summaryTable.getSpec(), permIntervalsModel.getGroupColNames().length,
                permIntervalsModel.getOutlierColNames(), m_memberCounter, m_outlierRepCounter),
            exec);

        addMissingGroups(exec, permIntervalsModel);
    }

    /**
     * Adds the missing groups and their respective counts to the the summary table.
     *
     * @param exec the execution context
     * @param permIntervalsModel the permitted intervals model
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private void addMissingGroups(final ExecutionContext exec, final OutlierModel permIntervalsModel)
        throws CanceledExecutionException {
        long size = m_summaryTable.size();

        // if there are missing groups add them to the summary table
        if (!m_missingGroupsCounter.isEmpty()) {
            // create a data container for the missing groups
            BufferedDataContainer dataContainer = exec.createDataContainer(m_summaryTable.getDataTableSpec());
            final DataCell[] rowCells = new DataCell[m_summaryTable.getDataTableSpec().getNumColumns()];
            // for each missing group create the new row
            for (final GroupKey key : m_missingGroupsCounter.getAllKeys()) {
                int k = 0;
                // store the group
                for (; k < key.size(); k++) {
                    rowCells[k] = key.getGroupVals()[k];
                }
                // for each outlier column create the entries for the current group
                for (int i = 0; i < permIntervalsModel.getOutlierColNames().length; i++) {
                    DataCell[] colEntries = createEntriesForMissingColumn(key, i);
                    for (int j = 0; j < colEntries.length; j++) {
                        rowCells[k++] = colEntries[j];
                    }
                }
                // add the newly created row
                dataContainer.addRowToTable(new DefaultRow("Row" + (size++), rowCells));
            }
            // add the new missing groups to the summary table
            dataContainer.close();
            m_summaryTable = exec.createConcatenateTable(exec, m_summaryTable,
                exec.createBufferedDataTable(dataContainer.getTable(), exec));
        }
    }

    /**
     * Creates the entries, i.e., member count, replacement count and permitted intervals for the given outlier column
     * and group.
     *
     * @param key the groups key
     * @param index the outlier column index
     * @return the entries for the given outlier column and group
     */
    private DataCell[] createEntriesForMissingColumn(final GroupKey key, final int index) {
        final DataCell[] cells = new DataCell[4];
        cells[0] = m_missingGroupsCounter.getCount(index, key);
        cells[1] = IntCellFactory.create(0);
        cells[2] = DataType.getMissingCell();
        cells[3] = DataType.getMissingCell();
        return cells;
    }

    /**
     * Creates an column rearranger that adds the replacement and member count for each outlier column and changes the
     * ordering of the columns.
     *
     * @param inSpec the table spec of the data table
     * @param outlierOffset offset encoding the first position where the permitted interval columns can be found
     * @param outlierColNames the outlier column names
     * @param memberCounter the member counter
     * @param outlierRepCounter the replacement counter
     * @return the column rearranger
     */
    private static ColumnRearranger addCountsAndPermute(final DataTableSpec inSpec, final int outlierOffset,
        final String[] outlierColNames, final GroupsMemberCounterPerColumn memberCounter,
        final GroupsMemberCounterPerColumn outlierRepCounter) {

        // the number of outliers
        final int noOutliers = outlierColNames.length;

        // append cells storing the number of outliers per column
        final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);

        // create the specs for the new columns
        final DataColumnSpec[] outlierCountSpecs = new DataColumnSpec[noOutliers * 2];
        for (int i = 0; i < noOutliers; i++) {
            outlierCountSpecs[i] =
                new DataColumnSpecCreator(outlierColNames[i] + MEMBER_COUNT_SUFFIX, IntCell.TYPE).createSpec();
            outlierCountSpecs[i + noOutliers] =
                new DataColumnSpecCreator(outlierColNames[i] + REPLACEMENT_COUNT_SUFFIX, IntCell.TYPE).createSpec();
        }

        // array storing the outlier count values
        final DataCell[] outlierCountCells = new DataCell[noOutliers * 2];
        // array storing the group values (key)
        final DataCell[] groupVals = new DataCell[outlierOffset];

        // factory
        final AbstractCellFactory fac = new AbstractCellFactory(outlierCountSpecs) {

            @Override
            public DataCell[] getCells(final DataRow row) {
                // calculate group key
                for (int i = 0; i < outlierOffset; i++) {
                    groupVals[i] = row.getCell(i);
                }
                final GroupKey key = new GroupKey(groupVals);
                // set the values for the current column and group
                for (int i = 0; i < noOutliers; i++) {
                    outlierCountCells[i] = memberCounter.getCount(i, key);
                    outlierCountCells[i + noOutliers] = outlierRepCounter.getCount(i, key);
                }
                // return the outlier counts
                return outlierCountCells;
            }
        };
        // append the newly created columns
        colRearranger.append(fac);

        // calculate the new layout of the columns
        final int[] permutation = calcPermutation(colRearranger.createSpec(), outlierOffset, noOutliers);

        // reorder the columns
        colRearranger.permute(permutation);

        // return the rearranger
        return colRearranger;
    }

    /**
     * Permutes the columns of the summary table such that all columns belonging to the same outlier (column) are
     * blocked in the output table.
     *
     * @param inSpec the in spec
     * @param outlierOffset offset encoding the first position where the permitted interval columns can be found
     * @param noOutliers the number of outlier columns
     * @return the permutation ensuring that all statistics belonging to the same outlier are blocked in the output
     *         table
     */
    private static int[] calcPermutation(final DataTableSpec inSpec, final int outlierOffset, final int noOutliers) {
        // the permutation array
        final int[] permutation = new int[inSpec.getNumColumns()];

        // offset encoding the first position where the member count columns can be found
        final int memberCountOffset = 2 * noOutliers + outlierOffset;

        // offset encoding the first position where the outlier count columns can be found
        final int outlierCountOffset = 3 * noOutliers + outlierOffset;

        int permIndex = 0;
        // group columns are not moved
        for (int i = 0; i < outlierOffset; i++) {
            permutation[permIndex++] = i;
        }
        // block columns belonging to the same outlier
        for (int i = 0; i < noOutliers; i++) {
            permutation[permIndex++] = i + memberCountOffset;
            permutation[permIndex++] = i + outlierCountOffset;
            int index = i * 2 + outlierOffset;
            permutation[permIndex++] = index;
            ++index;
            permutation[permIndex++] = index;

        }
        // return the permutation
        return permutation;
    }

    /**
     * Informs the listeners that a problem occured.
     *
     * @param msg the warning message
     */
    private void warnListeners(final String msg) {
        final Warning warning = new Warning(msg);
        // warn all listeners
        m_listeners.forEach(l -> l.warning(warning));
    }

    /**
     * Counts the number of members for each column respective the given groups.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private class GroupsMemberCounterPerColumn {

        /** Map storing the number of members for each column respective the different groups. */
        final Map<GroupKey, Integer>[] m_groupCounts;

        /**
         * Constructor.
         *
         * @param numOfColumns the number of columsn for which the members have to be counted
         */
        @SuppressWarnings("unchecked")
        GroupsMemberCounterPerColumn(final int numOfColumns) {
            m_groupCounts = new Map[numOfColumns];
            for (int i = 0; i < numOfColumns; i++) {
                m_groupCounts[i] = new HashMap<GroupKey, Integer>();
            }
        }

        /**
         * Tells whether or not the counter is empty.
         *
         * @return <code>True</code> if the counter is empty, <code>False</code> otherwise
         */
        boolean isEmpty() {
            return Arrays.stream(m_groupCounts).allMatch(data -> data.isEmpty());
        }

        /**
         * Returns the set of group keys.
         *
         * @return the set of group keys
         */
        Set<GroupKey> getAllKeys() {
            return Arrays.stream(m_groupCounts).map(data -> data.keySet()).flatMap(keys -> keys.stream())
                .collect(Collectors.toSet());
        }

        /**
         * Increment the member count for the given index key pair.
         *
         * @param index the position of the value to increment
         * @param key the key of the group whose count needs to be incremented
         */
        void incrementMemberCount(final int index, final GroupKey key) {
            final Map<GroupKey, Integer> map = m_groupCounts[index];
            // if key not contained initialize by 0
            if (!map.containsKey(key)) {
                map.put(key, 0);
            }
            // increment the value
            map.put(key, map.get(key) + 1);
        }

        /**
         * Returns the member count for the given index key pair.
         *
         * @param index the position of the value to return
         * @param key the key of the group whose count needs to be returned
         * @return the member count for the given index key pair
         */
        DataCell getCount(final int index, final GroupKey key) {
            final Map<GroupKey, Integer> map = m_groupCounts[index];
            int count = 0;
            if (map.containsKey(key)) {
                count = map.get(key);
            }
            return IntCellFactory.create(count);
        }

    }
}