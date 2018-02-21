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
 *   Feb 15, 2018 (ortmann): created
 */
package org.knime.base.algorithms.outlier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
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
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * The algorithm to identify and treat outliers based on the interquartile range.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class OutlierDetector {

    /**
     *
     */
    private static final ColumnNamePolicy COLUMN_NAME_POLICY = ColumnNamePolicy.AGGREGATION_METHOD_COLUMN_NAME;

    /** Outlier treatment and output generation routine message. */
    private static final String TREATMENT_MSG = "Treating outliers and generating output";

    /** Interval calculation routine message. */
    private static final String INTERVAL_MSG = "Calculating intervals";

    /** Statistics calculation routine message. */
    private static final String STATISTICS_MSG = "Calculating statistics";

    /** Empty table warning text. */
    private static final String EMPTY_TABLE_WARNING = "Node created an empty data table";

    /** The default groups name. */
    private static final String DEFAULT_GROUPS_NAME = "none";

    /** Exception message if the MemoryGroupByTable execution fails due to heap-space problems. */
    private static final String MEMORY_EXCEPTION =
        "Outlier detection requires more heap-space than provided. Please change to out of memory computation, or "
            + "increase the provided heap-space.";

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

    /** The outlier treatment option. */
    private final OutlierTreatmentOption m_treatment;

    /** The outlier replacement strategy. */
    private final OutlierReplacementStrategy m_repStrategy;

    /** The interquartile range scalar. */
    private final double m_iqrScalar;

    /** List of listeners recieving warning messages. */
    private final List<WarningListener> m_listeners;

    /** The table storing the interval bounds for each group */
    private BufferedDataTable m_intervalTable;

    /** The from outlier claned table */
    private BufferedDataTable m_outTable;

    /** Tells whether the computation is done in or out of memory. */
    private final boolean m_inMemory;

    /** Tells whether the domains of the outlier columns have to be reset after the computation. */
    private final boolean m_resetDomain;

    /**
     * Builder of the OutlierDetector.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public static class Builder {

        // Required parameters
        private final String[] m_outlierColNames;

        // Optional parameters
        private EstimationType m_estimationType = EstimationType.R_4;

        private OutlierTreatmentOption m_treatment = OutlierTreatmentOption.REPLACE;

        private OutlierReplacementStrategy m_repStrategy = OutlierReplacementStrategy.INTERVAL_BOUNDARY;

        private List<String> m_groupColNames = Collections.emptyList();

        private double m_iqrScalar = 1.5d;

        private final List<WarningListener> m_listeners;

        private boolean m_inMemory = false;

        private boolean m_resetDomain = false;

        /**
         * Sets the outlier column names.
         *
         * @param outlierColNames the outlier column names to be used
         */
        public Builder(final String[] outlierColNames) {
            m_outlierColNames = outlierColNames;
            m_listeners = new LinkedList<WarningListener>();
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
         * Adds a listener that gets triggered whenever the {@link OutlierDetector} creates a warning.
         *
         * @param l the listener to be added
         * @return the builder itself
         */
        public Builder addWarningListener(final WarningListener l) {
            m_listeners.add(l);
            return this;
        }

        /**
         * Sets the domain policy flag.
         *
         * @param resetDomain the domain policy
         * @return the builder itself
         */
        public Builder resetDomain(final boolean resetDomain) {
            m_resetDomain = resetDomain;
            return this;
        }

        /**
         * Constructs the outlier detector using the settigns provided by the builder.
         *
         * @return the outlier detector using the settigns provided by the builder
         */
        public OutlierDetector build() {
            return new OutlierDetector(this);
        }

    }

    /**
     * Cosntructor.
     *
     * @param b the builder providing all settings
     */
    private OutlierDetector(final Builder b) {
        m_outlierColNames = b.m_outlierColNames;
        m_groupColNames = b.m_groupColNames;
        m_estimationType = b.m_estimationType;
        m_treatment = b.m_treatment;
        m_repStrategy = b.m_repStrategy;
        m_iqrScalar = b.m_iqrScalar;
        m_inMemory = b.m_inMemory;
        m_resetDomain = b.m_resetDomain;
        m_listeners = new LinkedList<WarningListener>(b.m_listeners);

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
     * Returns the data table storing the allowed intervals.
     *
     * @return the data table storing the allowed intervals
     */
    public BufferedDataTable getIntervalTable() {
        return m_intervalTable;
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
     * Returns the spec of the table storing the allowed intervals.
     *
     * @param inSpec the spec of the input data table
     * @return the spec of the table storing the allowed intervals
     */
    public DataTableSpec getIntervalTableSpec(final DataTableSpec inSpec) {
        return addOutlierCountsAndPermute(renameColumns(GroupByTable.createGroupByTableSpec(inSpec, m_groupColNames,
            getAggretators(inSpec, GlobalSettings.DEFAULT), COLUMN_NAME_POLICY))).createSpec();
    }

    /**
     * Detects and treats the outliers.
     *
     * @param in the data table for which the outliers have to be detected
     * @param exec the execution context
     * @throws Exception if the execution failed, due to internal reasons or cancelation from the outside.
     */
    public void execute(final BufferedDataTable in, final ExecutionContext exec) throws Exception {

        // calculate the first and third quartile of each outlier column wr.t. the groups this method might cause an
        // out of memory exception while cloning the aggregators. However, this is very unlikely
        final GroupByTable t;

        // defines the end of the current progress
        final double extractionProgress = 0.1;
        final double renameProgress = 0.02;
        final double groupProgress;

        if (m_inMemory) {
            groupProgress = 0.4;
        } else {
            groupProgress = 0.8;
        }

        final double writeProgress = 1 - renameProgress - groupProgress - extractionProgress;

        // start the computation of the first and third quartile (and some additional stuff)
        exec.setMessage(STATISTICS_MSG);
        try {
            t = getGroupByTable(in, exec.createSubExecutionContext(groupProgress));
        } catch (final OutOfMemoryError e) {
            throw new IllegalArgumentException(MEMORY_EXCEPTION, e);
        }
        // skipped groups implies in our case an out of memory error. This can only occur if the computation is
        // carried out inside the memory
        if (!t.getSkippedGroupsByColName().isEmpty()) {
            throw new IllegalArgumentException(MEMORY_EXCEPTION);
        }

        // start the allowed interval calculation
        exec.setMessage(INTERVAL_MSG);

        // interval subexecution context
        ExecutionContext intervalExec = exec.createSubExecutionContext(extractionProgress);

        // calculate the intervals for each column w.r.t. to the groups
        m_intervalTable = calcIntervals(intervalExec, t.getBufferedTable());

        // calculate the map encoding the information stored in the interval table
        Map<GroupKey, Map<String, double[]>> intervalsGroupsMap = extractIntervals(intervalExec);

        // start the treatment step
        exec.setMessage(TREATMENT_MSG);

        // treat the outliers and store the results to m_out
        Map<GroupKey, Integer>[] outlierRepCounts =
            treatOutliers(exec.createSubExecutionContext(writeProgress), in, intervalsGroupsMap, true);

        // rename the interval table
        m_intervalTable = exec.createSpecReplacerTable(m_intervalTable, renameColumns(m_intervalTable.getSpec()));
        // permute the interval table
        m_intervalTable = exec.createColumnRearrangeTable(m_intervalTable,
            addOutlierCountsAndPermute(m_intervalTable.getSpec(), outlierRepCounts), exec);

        exec.setProgress(1);
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
     * Replaces the first and third quartile values stored in the input table by the allowed interval boundaries.
     * columns and interval boundaries.
     *
     * @param exec the execution context
     * @param quartiles the data table holding the groups, and the first and third quartile for each of the outlier
     *            columns
     * @return the data table storing the allowed interval
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private BufferedDataTable calcIntervals(final ExecutionContext exec, final BufferedDataTable quartiles)
        throws CanceledExecutionException {
        final DataTableSpec intervalSpec = quartiles.getDataTableSpec();

        // create column re-arranger to overwrite cells corresponding to outliers
        final ColumnRearranger colRearranger = new ColumnRearranger(intervalSpec);

        // array storing the allowed intervals
        final DataCell[] intervals = new DataCell[2 * m_outlierColNames.length];

        // first position where outlier columns can be found
        final int outlierOffset = m_groupColNames.size();

        final int[] colsToRepInd = new int[intervals.length];
        final DataColumnSpec[] colsToRepSpecs = new DataColumnSpec[intervals.length];
        for (int i = 0; i < intervals.length; i++) {
            colsToRepInd[i] = i + outlierOffset;
            colsToRepSpecs[i] = intervalSpec.getColumnSpec(i + outlierOffset);
        }

        final Map<String, double[]> domainMap = new HashMap<String, double[]>();

        final String[] tableColNames = intervalSpec.getColumnNames();

        // replace the intervals table content by the allowed interval boundaries
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

                        // store the allowed interval boundaries
                        final double lowerBound = fQ - iqr;
                        fQuart = DoubleCellFactory.create(lowerBound);
                        updateDomain(domainMap, tableColNames[index], lowerBound);

                        final double upperBound = tQ + iqr;
                        tQuart = DoubleCellFactory.create(tQ + iqr);
                        updateDomain(domainMap, tableColNames[index + 1], upperBound);

                    }
                    intervals[i] = fQuart;
                    intervals[i + 1] = tQuart;
                }
                return intervals;
            }
        };
        // replace the outlier columns by their updated versions
        colRearranger.replace(fac, colsToRepInd);

        // return the table storing the allowed intervals with updated domains
        return resetDomain(exec, exec.createColumnRearrangeTable(quartiles, colRearranger, exec), domainMap);
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
                    warnListeners("Group <" + groupNames + "> contains only missing values in column " + outlierName);
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
     * Clears the input data table of its outliers, depending on the chose outlier treatment.
     *
     * <p>
     * Given that outlier have to be replaced, each of the cells containing an outlier is either replaced by an missing
     * value or set to value of the closest (sclaed) IQR boundary. Otherwise all rows containing an outlier are removed
     * from the input data table
     * </p>
     *
     *
     * @param exec the execution context
     * @param in the inputer data table
     * @param intervalsGroupsMap mapping between groups and the allowed intervals for each outlier column
     * @param calcOutlierCounts tells whether outlier counts per column and group have to be calculated or not
     * @return the number of replaced outliers for each column and group
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    @SuppressWarnings("unchecked")
    private Map<GroupKey, Integer>[] treatOutliers(final ExecutionContext exec, final BufferedDataTable in,
        final Map<GroupKey, Map<String, double[]>> intervalsGroupsMap, final boolean calcOutlierCounts)
        throws CanceledExecutionException {

        // input data table spec
        DataTableSpec inSpec = in.getDataTableSpec();

        // store the min and max values for the domain reset
        final HashMap<String, double[]> domainMap = new HashMap<>();

        // store the positions where the group column names can be found in the input table
        final List<Integer> groupIndices = new ArrayList<Integer>(m_groupColNames.size());
        for (final String groupColName : m_groupColNames) {
            groupIndices.add(inSpec.findColumnIndex(groupColName));
        }

        // total number of outlier columns
        final int noOutliers = m_outlierColNames.length;

        // map storing the outlier counts per column and group
        final Map<GroupKey, Integer>[] outlierRepCounts;
        if (calcOutlierCounts) {
            outlierRepCounts = new HashMap[noOutliers];
            for (int i = 0; i < noOutliers; i++) {
                outlierRepCounts[i] = new HashMap<GroupKey, Integer>();
            }
        } else {
            outlierRepCounts = null;
        }
        // store the positions where the outlier columns names can be found in the input table
        final int[] outlierIndices = new int[noOutliers];
        for (int i = 0; i < noOutliers; i++) {
            outlierIndices[i] = inSpec.findColumnIndex(m_outlierColNames[i]);
        }

        if (m_treatment == OutlierTreatmentOption.REPLACE) {

            replaceOutliers(exec, in, groupIndices, outlierIndices, intervalsGroupsMap, outlierRepCounts, domainMap);
        } else {
            // we remove all columns containing at least one outlier
            removeRows(exec, in, groupIndices, outlierIndices, intervalsGroupsMap, outlierRepCounts, domainMap);
        }
        // reset the domain
        if (m_resetDomain) {
            m_outTable = resetDomain(exec, m_outTable, domainMap);
        }
        // update progress
        exec.setProgress(1);

        return outlierRepCounts;
    }

    /**
     * Replaces outliers found in the input table according to the selected replacement option. Additionally, the
     * outlier replacement counts and new domains are calculated if necessary.
     *
     * @param exec the execution context
     * @param in the input data table
     * @param groupIndices the positions where the group column names can be found in the input table
     * @param outlierIndices the positions where the outlier columns names can be found in the input table
     * @param intervalsGroupsMap mapping between groups and the allowed intervals for each outlier column
     * @param outlierRepCounts map to store the outlier counts per column and group
     * @param domainMap map to store the min and max values for each domain
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private void replaceOutliers(final ExecutionContext exec, final BufferedDataTable in,
        final List<Integer> groupIndices, final int[] outlierIndices,
        final Map<GroupKey, Map<String, double[]>> intervalsGroupsMap, final Map<GroupKey, Integer>[] outlierRepCounts,
        final Map<String, double[]> domainMap) throws CanceledExecutionException {
        // total number of outlier columns
        final int noOutliers = m_outlierColNames.length;

        // the in table spec
        final DataTableSpec inSpec = in.getDataTableSpec();

        // create column re-arranger to overwrite cells corresponding to outliers
        final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);

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
                final GroupKey key = getGroupKey(groupIndices, row);
                final Map<String, double[]> colsMap = intervalsGroupsMap.get(key);
                for (int i = 0; i < noOutliers; i++) {
                    // treat the value of the cell if its a outlier
                    final DataCell treatedCell =
                        treatCellValue(colsMap.get(m_outlierColNames[i]), row.getCell(outlierIndices[i]));
                    // if we changed the value this is an outlier
                    if (outlierRepCounts != null && !treatedCell.equals(row.getCell(outlierIndices[i]))) {
                        incrementOutlierCount(outlierRepCounts[i], key);
                    }
                    // update the domain if necessary
                    if (m_resetDomain && !treatedCell.isMissing()) {
                        updateDomain(domainMap, m_outlierColNames[i], ((DoubleValue)treatedCell).getDoubleValue());
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
     * Removes rows from the input table that contain outerlis, calculates the outlier replacement counts, and new
     * domains.
     *
     * @param exec the execution context
     * @param in the input data table
     * @param groupIndices the positions where the group column names can be found in the input table
     * @param outlierIndices the positions where the outlier columns names can be found in the input table
     * @param intervalsGroupsMap mapping between groups and the allowed intervals for each outlier column
     * @param outlierRepCounts map to store the outlier counts per column and group
     * @param domainMap map to store the min and max values for each domain
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private void removeRows(final ExecutionContext exec, final BufferedDataTable in, final List<Integer> groupIndices,
        final int[] outlierIndices, final Map<GroupKey, Map<String, double[]>> intervalsGroupsMap,
        final Map<GroupKey, Integer>[] outlierRepCounts, final Map<String, double[]> domainMap)
        throws CanceledExecutionException {
        // total number of outlier columns
        final int noOutliers = m_outlierColNames.length;

        BufferedDataContainer container = exec.createDataContainer(in.getDataTableSpec());

        // ensure that the max process equals 0.95 and not 1
        final long rowCount = in.size();
        final double divisor = rowCount + 0.05 * rowCount;
        long rowCounter = 1;

        for (final DataRow row : in) {
            exec.checkCanceled();
            final long rowCounterLong = rowCounter++; // 'final' due to access in lambda expression
            exec.setProgress(rowCounterLong / divisor,
                () -> "Testing row " + rowCounterLong + " of " + rowCount + " for outliers");

            final GroupKey key = getGroupKey(groupIndices, row);
            Map<String, double[]> colsMap = intervalsGroupsMap.get(key);
            boolean toInsert = true;
            for (int i = 0; i < noOutliers; i++) {
                final double[] interval = colsMap.get(m_outlierColNames[i]);
                final DataCell cell = row.getCell(outlierIndices[i]);
                if (!cell.isMissing()) {
                    final double val = ((DoubleValue)cell).getDoubleValue();
                    if (val < interval[0] || val > interval[1]) {
                        toInsert = false;
                        // increment the outlier count if necessary
                        if (outlierRepCounts != null) {
                            incrementOutlierCount(outlierRepCounts[i], key);
                        }
                    }
                }
            }
            if (toInsert) {
                container.addRowToTable(row);
                // update the domain if necessary
                if (m_resetDomain) {
                    DataCell cell;
                    for (int i = 0; i < noOutliers; i++) {
                        if (!(cell = row.getCell(outlierIndices[i])).isMissing()) {
                            updateDomain(domainMap, m_outlierColNames[i], ((DoubleValue)cell).getDoubleValue());
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
     * Modifies the the value/type of the data cell if necessary according the selected outlier replacement strategy.
     *
     * @param interval the allowed interval
     * @param cell the the current data cell
     * @return the new data cell after replacing its value if necessary
     */
    private DataCell treatCellValue(final double[] interval, final DataCell cell) {
        if (cell.isMissing()) {
            return cell;
        }
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
            // to the smallest integer inside the allowed interval
            val = Math.max(val, Math.ceil(interval[0]));
            // sets to the higher interval bound if necessary
            // to the largest integer inside the allowed interval
            val = Math.min(val, Math.floor(interval[1]));
            // return the proper DataCell
            if (cell.getType() == LongCell.TYPE) {
                return LongCellFactory.create((long)val);
            }
            return IntCellFactory.create((int)val);
        }
    }

    /**
     * Calculates the group key for a given data row.
     *
     * @param groupsIndices the row indices where the groups are located
     * @param row the row to holding the group key
     * @return the group key of the row
     */
    private GroupKey getGroupKey(final List<Integer> groupsIndices, final DataRow row) {
        final DataCell[] groupVals = new DataCell[groupsIndices.size()];
        for (int i = 0; i < groupsIndices.size(); i++) {
            groupVals[i] = row.getCell(groupsIndices.get(i));
        }
        // return the group key
        return new GroupKey(groupVals);
    }

    /**
     * Increment the outlier count for the given key.
     *
     * @param map the maping holding number of outliers to increment
     * @param key the key for the group which whose count needs to be incremented
     */
    private void incrementOutlierCount(final Map<GroupKey, Integer> map, final GroupKey key) {
        // if key not contained initialize by 0
        if (!map.containsKey(key)) {
            map.put(key, 0);
        }
        // increment the value
        map.put(key, map.get(key) + 1);
    }

    /**
     * Updates the domain map for the respective outlier column.
     *
     * @param domainMap the domain values to update
     * @param colName the outlier column name
     * @param val the value
     */
    private void updateDomain(final Map<String, double[]> domainMap, final String colName, final double val) {
        if (!domainMap.containsKey(colName)) {
            domainMap.put(colName, new double[]{val, val});
        }
        final double[] domainVals = domainMap.get(colName);
        domainVals[0] = Math.min(domainVals[0], val);
        domainVals[1] = Math.max(domainVals[1], val);
    }

    /**
     * Resets the domain for all outlier columns.
     *
     * @param exec the execution context
     * @param the data table whose domains have to be reseted
     * @param domainMap the map containing the min and max values for each outlier
     * @return the data table after reseting the domains
     */
    private BufferedDataTable resetDomain(final ExecutionContext exec, final BufferedDataTable data,
        final Map<String, double[]> domainMap) {
        DataTableSpec spec = data.getSpec();
        final DataColumnSpec[] domainSpecs = new DataColumnSpec[spec.getNumColumns()];
        for (int i = 0; i < spec.getNumColumns(); i++) {
            final DataColumnSpec columnSpec = spec.getColumnSpec(i);
            if (domainMap.containsKey(columnSpec.getName())) {
                domainSpecs[i] = updateDomainSpec(columnSpec, domainMap.get(columnSpec.getName()));
            } else {
                domainSpecs[i] = columnSpec;
            }
        }
        return exec.createSpecReplacerTable(data, new DataTableSpec(spec.getName(), domainSpecs));
    }

    /**
     * Updates the domain of the input spec.
     *
     * @param inputSpec the spec to be updated
     * @param domainVals the min and max value of the input spec column
     * @return the updated spec
     */
    private DataColumnSpec updateDomainSpec(final DataColumnSpec inputSpec, final double[] domainVals) {
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator(inputSpec);
        DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(inputSpec.getDomain());
        DataCell[] domainBounds = createBoundCells(inputSpec.getType(), domainVals[0], domainVals[1]);
        domainCreator.setLowerBound(domainBounds[0]);
        domainCreator.setUpperBound(domainBounds[1]);
        specCreator.setDomain(domainCreator.createDomain());
        return specCreator.createSpec();
    }

    /**
     * Creates two data cells of the proper type holding storing the given domain.
     *
     * @param type the type of the cell to create
     * @param lowerBound the lower bound of the domain
     * @param upperBound the upper bound of the domain
     * @return cells of the proper storing the given value
     */
    private DataCell[] createBoundCells(final DataType type, final double lowerBound, final double upperBound) {
        if (type == DoubleCell.TYPE) {
            return new DataCell[]{DoubleCellFactory.create(lowerBound), DoubleCellFactory.create(upperBound)};
        }
        // for int and long type use floor of the lower bound and ceil of the upper bound
        if (type == LongCell.TYPE) {
            return new DataCell[]{LongCellFactory.create((long)Math.floor(lowerBound)),
                LongCellFactory.create((long)Math.ceil(upperBound))};
        }
        // it must be a int cell
        return new DataCell[]{IntCellFactory.create((int)Math.floor(lowerBound)),
            IntCellFactory.create((int)Math.ceil(upperBound))};
    }
}
