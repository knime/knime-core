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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.knime.base.algorithms.outlier.listeners.WarningsListenerPool;
import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.GlobalSettings.AggregationContext;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.base.data.aggregation.general.CountOperator;
import org.knime.base.data.aggregation.numerical.PSquarePercentileOperator;
import org.knime.base.data.aggregation.numerical.QuantileOperator;
import org.knime.base.node.preproc.groupby.BigGroupByTable;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.GroupByTable;
import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.base.node.preproc.groupby.MemoryGroupByTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * The algorithm to calculate the permitted intervals based on the interquartile range.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class OutlierIntervalsCalculator {

    /** The column name policy used by the {@link GroupByTable} */
    private static final ColumnNamePolicy COLUMN_NAME_POLICY = ColumnNamePolicy.AGGREGATION_METHOD_COLUMN_NAME;

    /** Interval calculation routine message. */
    private static final String INTERVAL_MSG = "Calculating intervals";

    /** Statistics calculation routine message. */
    private static final String STATISTICS_MSG = "Calculating statistics";

    /** The default groups name. */
    private static final String DEFAULT_GROUPS_NAME = "none";

    /** Exception message if the MemoryGroupByTable execution fails due to heap-space problems. */
    private static final String MEMORY_EXCEPTION =
        "More heap-space required. Please change to out of memory computation, or increase the provided heap-space";

    /** Treatment of missing cells. */
    private static final boolean INCL_MISSING_CELLS = false;

    /** The percentile values. */
    private static final double[] PERCENTILES = new double[]{0.25, 0.75};

    /** The outlier column names. */
    private final String[] m_outlierColNames;

    /** The group column names. */
    private final List<String> m_groupColNames;

    /** The estimation type. */
    private final EstimationType m_estimationType;

    /** The interquartile range scalar. */
    private final double m_iqrScalar;

    /** Listeners pool forwarding warning messages. */
    private final WarningsListenerPool m_listenerPool;

    /** The table storing the interval bounds for each group */
    private BufferedDataTable m_intervalTable;

    /** Tells whether the computation is done in or out of memory. */
    private final boolean m_inMemory;

    /**
     * Builder of the IntervalsCalculator.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public static class Builder {

        // Required parameters
        /** The outlier column names. */
        private final String[] m_outlierColNames;

        /** The group column names. */
        private List<String> m_groupColNames = Collections.emptyList();

        /** The estimation type. */
        private EstimationType m_estimationType = EstimationType.R_4;

        /** The interquartile range scalar. */
        private double m_iqrScalar = 1.5d;

        /** Listeners pool forwarding warning messages. */
        private WarningsListenerPool m_listenerPool;

        /** Tells whether the computation is done in or out of memory. */
        private boolean m_inMemory = false;

        /**
         * Sets the outlier column names.
         *
         * @param outlierColNames the outlier column names to be used
         */
        public Builder(final String[] outlierColNames) {
            m_outlierColNames = outlierColNames;
        }

        /**
         * Sets the estimation type, used for the in memory quartile computation. The estimation type is ignored when
         * the computation is not carried out in memory.
         *
         * @param estimationType the estimation type to be used
         * @return the builder itself
         */
        public Builder setEstimationType(final EstimationType estimationType) {
            m_estimationType = estimationType;
            return this;
        }

        /**
         * Sets the group column names.
         *
         * @param groupColNames the group column names
         * @return the builder itself
         */
        public Builder setGroupColumnNames(final List<String> groupColNames) {
            m_groupColNames = groupColNames;
            return this;
        }

        /**
         * Sets the in memory calculation flag. Note that the in memory calculation requires far more space, than the
         * out-of-memory computation.
         *
         * @param inMemory the in memory calculation flag
         * @return the builder itself
         */
        public Builder calcInMemory(final boolean inMemory) {
            m_inMemory = inMemory;
            return this;
        }

        /**
         * Sets the interquartile range scalar.
         *
         * @param iqrScalar the interquartile scalar
         * @return the builder itself
         */
        public Builder setIQRScalar(final double iqrScalar) {
            m_iqrScalar = iqrScalar;
            return this;
        }

        /**
         * Sets the given listener pool, which is triggered whenever the {@link OutlierIntervalsCalculator} creates a
         * warning.
         *
         * @param listenerPool the listener to be set
         * @return the builder itself
         */
        public Builder setWarningListenerPool(final WarningsListenerPool listenerPool) {
            m_listenerPool = listenerPool;
            return this;
        }

        /**
         * Constructs the outlier detector using the settings provided by the builder.
         *
         * @return the outlier detector using the settings provided by the builder
         */
        public OutlierIntervalsCalculator build() {
            return new OutlierIntervalsCalculator(this);
        }
    }

    /**
     * Cosntructor.
     *
     * @param b the builder providing all settings
     */
    private OutlierIntervalsCalculator(final Builder b) {
        m_outlierColNames = b.m_outlierColNames;
        m_groupColNames = b.m_groupColNames;
        m_estimationType = b.m_estimationType;
        m_iqrScalar = b.m_iqrScalar;
        m_inMemory = b.m_inMemory;
        m_listenerPool = b.m_listenerPool;

    }

    /**
     * Tells whether the computation is done in or out of memory.
     *
     * @return the memory policy
     */
    public boolean inMemory() {
        return m_inMemory;
    }

    /**
     * Returns the data table storing the permitted intervals.
     *
     * @return the data table storing the permitted intervals
     */
    public BufferedDataTable getIntervalsTable() {
        return m_intervalTable;
    }

    /**
     * Returns the spec of the table storing the permitted intervals.
     *
     * @param inSpec the spec of the input data table
     * @return the spec of the table storing the permitted intervals
     */
    public DataTableSpec getIntervalsTableSpec(final DataTableSpec inSpec) {
        return addOutlierCountsAndPermute(renameColumns(GroupByTable.createGroupByTableSpec(inSpec, m_groupColNames,
            getAggretators(inSpec, GlobalSettings.DEFAULT), COLUMN_NAME_POLICY))).createSpec();
    }

    /**
     * Calculates the permitted intervals.
     *
     * @param in the data table for which the outliers have to be detected
     * @param exec the execution context
     * @return returns the mapping between groups and the permitted intervals for each outlier column
     * @throws Exception if the execution failed, due to internal reasons or cancelation from the outside.
     */
    public Map<GroupKey, Map<String, double[]>> calculateIntervals(final BufferedDataTable in,
        final ExecutionContext exec) throws Exception {

        // calculate the first and third quartile of each outlier column wr.t. the groups this method might cause an
        // out of memory exception while cloning the aggregators. However, this is very unlikely
        final GroupByTable t;

        // the quartile calculation progress
        final double quartilesProgress = 0.8;
        // the interval calculation progress
        final double intervalsProgress = 1 - quartilesProgress;

        // start the computation of the first and third quartile (and some additional stuff)
        exec.setMessage(STATISTICS_MSG);
        try {
            t = getGroupByTable(in, exec.createSubExecutionContext(quartilesProgress));
        } catch (final OutOfMemoryError e) {
            throw new IllegalArgumentException(MEMORY_EXCEPTION, e);
        }
        // skipped groups implies in our case an out of memory error. This can only occur if the computation is
        // carried out inside the memory
        if (!t.getSkippedGroupsByColName().isEmpty()) {
            throw new IllegalArgumentException(MEMORY_EXCEPTION);
        }

        // start the permitted interval calculation
        exec.setMessage(INTERVAL_MSG);

        // interval subexecution context
        ExecutionContext intervalExec = exec.createSubExecutionContext(intervalsProgress);

        // calculate the intervals for each column w.r.t. to the groups
        m_intervalTable = calcIntervals(intervalExec, t.getBufferedTable());

        // calculate the map encoding the information stored in the interval table
        Map<GroupKey, Map<String, double[]>> intervalsGroupsMap = extractIntervals(intervalExec);

        exec.setProgress(1);
        return intervalsGroupsMap;
    }

    /**
     * Adds the outlier replacement count to the intervals table, and renames and permutes columns.
     *
     * @param exec the execution context
     * @param outlierRepCounts the outlier repetitions counts to be added to the table
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    public void updateIntervalsTable(final ExecutionContext exec, final Map<GroupKey, Integer>[] outlierRepCounts)
        throws CanceledExecutionException {
        // rename the interval table
        m_intervalTable = exec.createSpecReplacerTable(m_intervalTable, renameColumns(m_intervalTable.getSpec()));
        // permute the interval table
        m_intervalTable = exec.createColumnRearrangeTable(m_intervalTable,
            addOutlierCountsAndPermute(m_intervalTable.getSpec(), outlierRepCounts), exec);
    }

    /**
     * Creates an column rearranger that adds the replacement count for each outlier column and changes the ordering of
     * the columns.
     *
     * @param inSpec the table spec of the data table
     * @return the column rearranger
     */
    private ColumnRearranger addOutlierCountsAndPermute(final DataTableSpec inSpec) {
        return addOutlierCountsAndPermute(inSpec, null);
    }

    /**
     * Creates an column rearranger that adds the replacement count for each outlier column and changes the ordering of
     * the columns.
     *
     * @param inSpec the table spec of the data table
     * @param outlierRepCounts the array holding the outlier replacement counts
     * @return the column rearranger
     */
    private ColumnRearranger addOutlierCountsAndPermute(final DataTableSpec inSpec,
        final Map<GroupKey, Integer>[] outlierRepCounts) {

        // first index where outliers can be found
        final int outlierOffset = m_groupColNames.size();

        // the number of outliers
        final int noOutliers = m_outlierColNames.length;

        // append cells storing the number of outliers per column
        final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);

        // create the specs for the new columns
        final DataColumnSpec[] outlierCountSpecs = new DataColumnSpec[noOutliers];
        for (int i = 0; i < noOutliers; i++) {
            outlierCountSpecs[i] =
                new DataColumnSpecCreator(m_outlierColNames[i] + "(outlier count)", IntCell.TYPE).createSpec();
        }

        // array storing the outlier count values
        final DataCell[] outlierCountCells = new DataCell[noOutliers];
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
                // set the values for the current group
                for (int i = 0; i < noOutliers; i++) {
                    // if no key exists this column did not contain an outlier for the given group, otherwise
                    // set the proper value
                    if (outlierRepCounts[i].containsKey(key)) {
                        outlierCountCells[i] = IntCellFactory.create(outlierRepCounts[i].get(key));
                    } else {
                        outlierCountCells[i] = IntCellFactory.create(0);
                    }
                }
                // return the outlier counts
                return outlierCountCells;
            }
        };
        // append the newly created columns
        colRearranger.append(fac);

        // calculate the new layout of the columns
        final int[] permutation = calcPermutation(inSpec, outlierOffset, noOutliers);

        // reorder the columns
        colRearranger.permute(permutation);

        // return the rearranger
        return colRearranger;

    }

    /**
     * Permutes the columns of the interval table such that all columns belonging to the same outlier (column) are
     * blocked in the output table.
     *
     * @param inSpec the in spec
     * @param outlierOffset offset encoding the first position where the interval columns can be found
     * @param noOutliers the number of outlier columns
     * @return the permutation ensuring that all statistics belonging to the same outlier are blocked in the output
     *         table
     */
    private int[] calcPermutation(final DataTableSpec inSpec, final int outlierOffset, final int noOutliers) {
        // the permutation array
        final int[] permutation = new int[inSpec.getNumColumns() + noOutliers];

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
     * Renames the columns.
     *
     * @param inSpec the table spec
     * @return the renamed columns table spec
     */
    private DataTableSpec renameColumns(final DataTableSpec inSpec) {
        DataColumnSpec[] cols = new DataColumnSpec[inSpec.getNumColumns()];

        final int outlierOffset = m_groupColNames.size();
        final int noOutliers = m_outlierColNames.length;
        final int memberCountOffset = 2 * noOutliers + outlierOffset;

        for (int i = 0; i < outlierOffset; i++) {
            cols[i] = inSpec.getColumnSpec(i);
        }

        // rename the columns
        for (int i = 0; i < noOutliers; i++) {
            cols[i + memberCountOffset] =
                createColumnSpec(inSpec.getColumnSpec(i + memberCountOffset), m_outlierColNames[i] + " (member count)");
            int index = i * 2 + outlierOffset;
            cols[index] = createColumnSpec(inSpec.getColumnSpec(index), m_outlierColNames[i] + " (lower bound)");
            ++index;
            cols[index] = createColumnSpec(inSpec.getColumnSpec(index), m_outlierColNames[i] + " (upper bound)");

        }
        // renamed spec
        return new DataTableSpec(inSpec.getName(), cols);
    }

    /**
     * Creates a data column spec with the given name.
     *
     * @param inSpec the input spec
     * @param name the new name of that column
     * @return the column spec with the new name
     */
    private DataColumnSpec createColumnSpec(final DataColumnSpec inSpec, final String name) {
        final DataColumnSpecCreator specCreator = new DataColumnSpecCreator(inSpec);
        specCreator.setName(name);
        return specCreator.createSpec();
    }

    /**
     * Constructs the {@link GroupByTable} in accordance with the given settings.
     *
     * @param in the input data table
     * @param exec the execution context
     * @return the {@link GroupByTable} w.r.t. the selected settings
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private GroupByTable getGroupByTable(final BufferedDataTable in, final ExecutionContext exec)
        throws CanceledExecutionException {

        // get the global settings
        final GlobalSettings gSettings = getGlobalSettings(in);

        // create the column aggregators
        final ColumnAggregator[] agg = getAggretators(in.getDataTableSpec(), gSettings);

        // init and return the GroupByTable obeying the chosen memory settings
        final GroupByTable t;
        if (m_inMemory) {
            t = new MemoryGroupByTable(exec, in, m_groupColNames, agg, gSettings, false, COLUMN_NAME_POLICY, false);
        } else {
            t = new BigGroupByTable(exec, in, m_groupColNames, agg, gSettings, false, COLUMN_NAME_POLICY, false);
        }
        return t;
    }

    /**
     * Create the global settings used by the {@link GroupByTable}.
     *
     * @param in the input data table
     * @return the global settings
     */
    private GlobalSettings getGlobalSettings(final BufferedDataTable in) {
        // set the number of unique values to the number of table rows (might cause OutOfMemoryException
        // during execution
        return GlobalSettings.builder()//
            .setMaxUniqueValues(KnowsRowCountTable.checkRowCount(in.size())) //
            .setAggregationContext(AggregationContext.COLUMN_AGGREGATION) //
            .setDataTableSpec(in.getDataTableSpec()) //
            .setGroupColNames(m_groupColNames) //
            .setValueDelimiter(GlobalSettings.STANDARD_DELIMITER) //
            .build();
    }

    /**
     * Creates column aggregators for each of the outlier columns.
     *
     * @param inSpec the input data table spec
     * @param gSettings the global settings
     * @return an array of column aggregators
     */
    private ColumnAggregator[] getAggretators(final DataTableSpec inSpec, final GlobalSettings gSettings) {
        final ColumnAggregator[] aggregators = new ColumnAggregator[m_outlierColNames.length * 3];

        int pos = 0;
        // for each outlier column name create the aggregators
        for (final String outlierColName : m_outlierColNames) {
            // the operator column settings
            final OperatorColumnSettings cSettings =
                new OperatorColumnSettings(INCL_MISSING_CELLS, inSpec.getColumnSpec(outlierColName));

            // add an aggregator to count the number of non-missing elements for each individual group
            // to the end of the aggregators array (makes it easier to parse the quartile columns)
            aggregators[2 * m_outlierColNames.length + pos / 2] =
                new ColumnAggregator(cSettings.getOriginalColSpec(), new CountOperator(gSettings, cSettings));

            // add the aggregators for calculating the first and third quartile with respect to the selected
            // memory policy
            for (final double percentile : PERCENTILES) {
                final AggregationMethod method;
                if (m_inMemory) {
                    method = new QuantileOperator(
                        new OperatorData("Quantile", true, false, DoubleValue.class, INCL_MISSING_CELLS), gSettings,
                        cSettings, percentile, m_estimationType.name());
                } else {
                    method = new PSquarePercentileOperator(gSettings, cSettings, 100 * percentile);
                }
                aggregators[pos++] = new ColumnAggregator(cSettings.getOriginalColSpec(), method);
            }

        }

        // return the aggregators
        return aggregators;
    }

    /**
     * Replaces the first and third quartile values stored in the input table by the permitted interval boundaries.
     * columns and interval boundaries.
     *
     * @param exec the execution context
     * @param quartiles the data table holding the groups, and the first and third quartile for each of the outlier
     *            columns
     * @return the data table storing the permitted interval
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private BufferedDataTable calcIntervals(final ExecutionContext exec, final BufferedDataTable quartiles)
        throws CanceledExecutionException {
        final DataTableSpec intervalSpec = quartiles.getDataTableSpec();

        // create column re-arranger to overwrite cells corresponding to outliers
        final ColumnRearranger colRearranger = new ColumnRearranger(intervalSpec);

        // array storing the permitted intervals
        final DataCell[] intervals = new DataCell[2 * m_outlierColNames.length];

        // first position where outlier columns can be found
        final int outlierOffset = m_groupColNames.size();

        final int[] colsToRepInd = new int[intervals.length];
        final DataColumnSpec[] colsToRepSpecs = new DataColumnSpec[intervals.length];
        for (int i = 0; i < intervals.length; i++) {
            colsToRepInd[i] = i + outlierOffset;
            colsToRepSpecs[i] = intervalSpec.getColumnSpec(i + outlierOffset);
        }

        final OutlierDomainsUpdater domainsUpdater = new OutlierDomainsUpdater();

        final String[] tableColNames = intervalSpec.getColumnNames();

        // replace the intervals table content by the permitted interval boundaries
        final AbstractCellFactory fac = new AbstractCellFactory(colsToRepSpecs) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                for (int i = 0; i < intervals.length; i += 2) {
                    final int index = i + outlierOffset;
                    // the first quartile cell
                    DataCell fQuart = row.getCell(index);
                    // the third quartile cell
                    DataCell tQuart = row.getCell(index + 1);
                    if (!fQuart.isMissing() && !tQuart.isMissing()) {
                        // value of the first quartile
                        final double fQ = ((DoubleValue)fQuart).getDoubleValue();
                        // value of the third quartile
                        final double tQ = ((DoubleValue)tQuart).getDoubleValue();

                        // calculate the scaled IQR
                        final double iqr = m_iqrScalar * (tQ - fQ);

                        // store the permitted interval boundaries
                        final double lowerBound = fQ - iqr;
                        fQuart = DoubleCellFactory.create(lowerBound);
                        domainsUpdater.updateDomain(tableColNames[index], lowerBound);

                        final double upperBound = tQ + iqr;
                        tQuart = DoubleCellFactory.create(tQ + iqr);
                        domainsUpdater.updateDomain(tableColNames[index + 1], upperBound);

                    }
                    intervals[i] = fQuart;
                    intervals[i + 1] = tQuart;
                }
                return intervals;
            }
        };
        // replace the outlier columns by their updated versions
        colRearranger.replace(fac, colsToRepInd);

        // return the table storing the permitted intervals with updated domains
        return domainsUpdater.updateDomain(exec, exec.createColumnRearrangeTable(quartiles, colRearranger, exec));
    }

    /**
     * Stores the interval boundaries for each outlier w.r.t. the different groups.
     *
     * @param exec the execution context
     * @return a map from groups to pairs of columns and interval boundaries
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private Map<GroupKey, Map<String, double[]>> extractIntervals(final ExecutionContext exec)
        throws CanceledExecutionException {
        final Map<GroupKey, Map<String, double[]>> intervalsGroupsMap = new HashMap<GroupKey, Map<String, double[]>>();

        final long rowCount = m_intervalTable.size();
        long rowCounter = 1;
        final int outlierOffset = m_groupColNames.size();

        for (final DataRow r : m_intervalTable) {
            exec.checkCanceled();
            final long rowCounterLong = rowCounter++; // 'final' due to access in lambda expression
            exec.setProgress(rowCounterLong / (double)rowCount,
                () -> "Storing interval for row " + rowCounterLong + " of " + rowCount);

            // calculate the groups key
            final DataCell[] groupVals = new DataCell[m_groupColNames.size()];
            for (int i = 0; i < outlierOffset; i++) {
                groupVals[i] = r.getCell(i);
            }

            // calculate for the current key the IQR of all outliers
            final HashMap<String, double[]> colsIQR = new HashMap<String, double[]>();
            for (int i = 0; i < m_outlierColNames.length; i++) {
                final String outlierName = m_outlierColNames[i];
                final double[] interval;
                // the GroupByTable might return MissingValues, but only if
                // the entire group consists of Missing Values
                final int index = i * 2 + outlierOffset;
                final DataCell fQuart = r.getCell(index);
                final DataCell tQuart = r.getCell(index + 1);
                if (!fQuart.isMissing() && !tQuart.isMissing()) {
                    interval =
                        new double[]{((DoubleValue)fQuart).getDoubleValue(), ((DoubleValue)tQuart).getDoubleValue()};
                } else {
                    interval = null;
                    String groupNames = Arrays.stream(groupVals).map(groupCell -> groupCell.toString())
                        .collect(Collectors.joining(", "));
                    if (groupNames.isEmpty()) {
                        groupNames = DEFAULT_GROUPS_NAME;
                    }
                    m_listenerPool.warnListeners(
                        "Group <" + groupNames + "> contains only missing values in column " + outlierName);
                }
                // setting null here is no problem since during the update we will only access the interval
                // if the cell is not missing. However, this implies that the interval is not null
                colsIQR.put(outlierName, interval);
            }

            // associate the group's key with the outliers' interval
            intervalsGroupsMap.put(new GroupKey(groupVals), colsIQR);

        }

        // return the mapping
        return intervalsGroupsMap;
    }

}
