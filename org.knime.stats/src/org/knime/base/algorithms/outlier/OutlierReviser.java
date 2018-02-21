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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.algorithms.outlier.listeners.WarningsListenerPool;
import org.knime.base.algorithms.outlier.options.OutlierReplacementStrategy;
import org.knime.base.algorithms.outlier.options.OutlierTreatmentOption;
import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * The algorithm to treat outliers based on the permitted intervals.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class OutlierReviser {

    /** Outlier treatment and output generation routine message. */
    private static final String TREATMENT_MSG = "Treating outliers and generating output";

    /** Empty table warning text. */
    private static final String EMPTY_TABLE_WARNING = "Node created an empty data table";

    /** The outlier column names. */
    private final String[] m_outlierColNames;

    /** The group column names. */
    private final List<String> m_groupColNames;

    /** The outlier treatment option. */
    private final OutlierTreatmentOption m_treatment;

    /** The outlier replacement strategy. */
    private final OutlierReplacementStrategy m_repStrategy;

    /** Listeners pool forwarding warning messages. */
    private final WarningsListenerPool m_listenerPool;

    /** The from outlier claned table */
    private BufferedDataTable m_outTable;

    /** Tells whether the domains of the outlier columns have to be updated after the computation or not. */
    private final boolean m_updateDomain;

    /**
     * Builder of the OutlierReviser.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public static class Builder {

        // Required parameters
        /** The outlier column names. */
        private String[] m_outlierColNames;

        // Optional parameters
        /** The group column names. */
        private List<String> m_groupColNames = Collections.emptyList();

        /** The outlier treatment option. */
        private OutlierTreatmentOption m_treatment = OutlierTreatmentOption.REPLACE;

        /** The outlier replacement strategy. */
        private OutlierReplacementStrategy m_repStrategy = OutlierReplacementStrategy.INTERVAL_BOUNDARY;

        /** Listeners pool forwarding warning messages. */
        private WarningsListenerPool m_listenerPool = new WarningsListenerPool();

        /** Tells whether the domains of the outlier columns have to be update after the computation or not. */
        private boolean m_updateDomain = false;

        /**
         * Sets the outlier column names.
         *
         * @param outlierColNames the outlier column names to be used
         */
        public Builder(final String[] outlierColNames) {
            m_outlierColNames = outlierColNames;
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
         * Sets the given listener pool, which is triggered whenever the {@link OutlierIntervalsCalculator} creates a warning.
         *
         * @param listenerPool the listener to be set
         * @return the builder itself
         */
        public Builder setWarningListenerPool(final WarningsListenerPool listenerPool) {
            m_listenerPool = listenerPool;
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

    private OutlierReviser(final Builder b) {
        m_outlierColNames = b.m_outlierColNames;
        m_groupColNames = b.m_groupColNames;
        m_treatment = b.m_treatment;
        m_repStrategy = b.m_repStrategy;
        m_updateDomain = b.m_updateDomain;
        m_listenerPool = b.m_listenerPool;
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
     * @param intervalsGroupsMap mapping between groups and the permitted intervals for each outlier column
     * @param calcOutlierCounts tells whether outlier counts per column and group have to be calculated or not
     * @return the number of replaced outliers for each column and group
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    @SuppressWarnings("unchecked")
    public Map<GroupKey, Integer>[] treatOutliers(final ExecutionContext exec, final BufferedDataTable in,
        final Map<GroupKey, Map<String, double[]>> intervalsGroupsMap, final boolean calcOutlierCounts)
        throws CanceledExecutionException {
        // start the treatment step
        exec.setMessage(TREATMENT_MSG);

        // input data table spec
        DataTableSpec inSpec = in.getDataTableSpec();

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

        // the domains updater
        final OutlierDomainsUpdater domainsUpdater = new OutlierDomainsUpdater();

        if (m_treatment == OutlierTreatmentOption.REPLACE) {
            replaceOutliers(exec, in, groupIndices, outlierIndices, intervalsGroupsMap, outlierRepCounts,
                domainsUpdater);
        } else {
            // we remove all columns containing at least one outlier
            removeRows(exec, in, groupIndices, outlierIndices, intervalsGroupsMap, outlierRepCounts, domainsUpdater);
        }
        // reset the domain
        if (m_updateDomain) {
            m_outTable = domainsUpdater.updateDomain(exec, m_outTable);
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
     * @param intervalsGroupsMap mapping between groups and the permitted intervals for each outlier column
     * @param outlierRepCounts map to store the outlier counts per column and group
     * @param domainsUpdater the domains updater
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private void replaceOutliers(final ExecutionContext exec, final BufferedDataTable in,
        final List<Integer> groupIndices, final int[] outlierIndices,
        final Map<GroupKey, Map<String, double[]>> intervalsGroupsMap, final Map<GroupKey, Integer>[] outlierRepCounts,
        final OutlierDomainsUpdater domainsUpdater) throws CanceledExecutionException {
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
     * Modifies the the value/type of the data cell if necessary according the selected outlier replacement strategy.
     *
     * @param interval the permitted interval
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
     * Removes rows from the input table that contain outerlis, calculates the outlier replacement counts, and new
     * domains.
     *
     * @param exec the execution context
     * @param in the input data table
     * @param groupIndices the positions where the group column names can be found in the input table
     * @param outlierIndices the positions where the outlier columns names can be found in the input table
     * @param intervalsGroupsMap mapping between groups and the permitted intervals for each outlier column
     * @param outlierRepCounts map to store the outlier counts per column and group
     * @param domainsUpdater the domains updater
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private void removeRows(final ExecutionContext exec, final BufferedDataTable in, final List<Integer> groupIndices,
        final int[] outlierIndices, final Map<GroupKey, Map<String, double[]>> intervalsGroupsMap,
        final Map<GroupKey, Integer>[] outlierRepCounts, final OutlierDomainsUpdater domainsUpdater)
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
            m_listenerPool.warnListeners(EMPTY_TABLE_WARNING);
        }
    }

}
