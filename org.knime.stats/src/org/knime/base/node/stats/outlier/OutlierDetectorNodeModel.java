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
 *   Jan 31, 2018 (ortmann): created
 */
package org.knime.base.node.stats.outlier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.GlobalSettings.AggregationContext;
import org.knime.base.data.aggregation.GlobalSettings.GlobalSettingsBuilder;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.base.data.aggregation.numerical.PSquarePercentileOperator;
import org.knime.base.data.aggregation.numerical.QuantileOperator;
import org.knime.base.node.preproc.groupby.BigGroupByTable;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.GroupByTable;
import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.base.node.preproc.groupby.MemoryGroupByTable;
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
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Model to identify outliers based on interquartile ranges.
 *
 * @author Mark Ortmann
 */
public final class OutlierDetectorNodeModel extends NodeModel {

    /** Treatment of missing cells. */
    private static final boolean INCL_MISSING_CELLS = false;

    /** The percentile values. */
    private static final double[] PERCENTILE_VALUES = new double[]{.25, .75};

    /** Default number of max unique values used by {@link BigGroupByTable} */
    private static final int DEFAULT_MAX_UNIQUE_VALS = (int)1e4;

    /** Number of data in-ports. */
    private static final int NBR_INPORTS = 1;

    /** Number of data out-ports. */
    private static final int NBR_OUTPORTS = 1;

    /** Config key for the apply to groups setting. */
    private static final String CFG_USE_GROUPS = "use-groups";

    /** Config key of the parameter. */
    private static final String CFG_TUKEY_PAR = "tukeys-parameter";

    /** Config key of the columns defining the groups. */
    private static final String CFG_GROUP_COLS = "groups-list";

    /** Config key of the columns need to be processed. */
    private static final String CFG_PROCESS_COLS = "columns-to-process-list";

    /** Config key of the included columns. */
    private static final String CFG_MEM_POLICY = "memory-policy";

    /** Config key of the estimation type used for in-memory computation. */
    private static final String CFG_ESTIMATION_TYPE = "estmation-type";

    /** Config key of the outlier treatment. */
    private static final String CFG_OUTLIER_TREATMENT = "outlier-treatment";

    /** Config key of the outlier replacement strategy. */
    private static final String CFG_OUTLIER_REPLACEMENT = "replacement-strategy";

    /** Default scalar to scale the IQR */
    private static final double DEFAULT_SCALAR = 1.5d;

    /** Default memory policy */
    private static final boolean DEFAULT_MEM_POLICY = true;

    /** Settings model of the selected groups. */
    private SettingsModelColumnFilter2 m_groupSettings;

    /** Settings model of the columns to check for outliers. */
    private SettingsModelColumnFilter2 m_outlierSettings;

    /** Settings model indicating whether the algorithm should be executed in or out of memory. */
    private SettingsModelBoolean m_memorySetting;

    /** Settings model holding information on how the quartiles are calculated if the algorithm is running in-memory. */
    private SettingsModelString m_estimationSettings;

    /** Settings model holding the information on the outlier treatment. */
    private SettingsModelString m_outlierTreatmentSettings;

    /** Settings model holding the information on the outlier replacement strategy. */
    private SettingsModelString m_outlierReplacementSettings;

    /** Settings model holding the factor to scale the interquartile range. */
    private SettingsModelDouble m_scalarModel;

    /** Settings model indicating whether the algorithm shall use the provided groups information. */
    private SettingsModelBoolean m_useGroupsSetting;

    /**
     * Enum encoding the outlier treatment.
     *
     * @author Mark Ortmann
     */
    enum TREATMENT_OPTIONS {
            REPLACE("Replace"), FILTER("Filter");

        private final String m_name;

        TREATMENT_OPTIONS(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }

        /**
         * Returns the enum for a given String
         *
         * @param name enum name
         * @return the enum
         */
        static TREATMENT_OPTIONS getEnum(final String name) {
            if (name == null) {
                throw new NullPointerException("Name is null");
            }
            for (final TREATMENT_OPTIONS op : values()) {
                if (op.m_name.equals(name)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("No TREATMENT_OPTIONS constant with name: " + name);
        }

    }

    /**
     * Enum encoding the replacement strategy.
     *
     * @author Mark Ortmann
     */
    enum REPLACEMENT_STRATEGY {
            MISSING("Missing values"), IQR("Closest interval boundary");

        private final String m_name;

        REPLACEMENT_STRATEGY(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }

        /**
         * Returns the enum for a given String
         *
         * @param name the enum name
         * @return the enum
         */
        static REPLACEMENT_STRATEGY getEnum(final String name) {
            if (name == null) {
                throw new NullPointerException("Name is null");
            }
            for (final REPLACEMENT_STRATEGY op : values()) {
                if (op.m_name.equals(name)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("No REPLACEMENT_OPTIONS constant with name: " + name);
        }

    }

    /** Init the outlier detector node model with one input and output. */
    OutlierDetectorNodeModel() {
        super(NBR_INPORTS, NBR_OUTPORTS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        // check if the input can be processed
        if (inData == null || inData[0] == null) {
            throw new Exception("No data table available!");
        }

        // access the table to process
        final BufferedDataTable in = inData[0];

        // get the group columns and outlier columns
        final List<String> groupColNames = getGroupColNames(in);
        final String[] outlierColNames = m_outlierSettings.applyTo(in.getSpec()).getIncludes();

        // calculate the first and third quartile of each outlier column wr.t. the groups
        final GroupByTable t = getGroupByTable(in, exec.createSubExecutionContext(0.7), groupColNames, outlierColNames);
        final BufferedDataTable result = t.getBufferedTable();

        // calculate the IQR for each column w.r.t. to the groups
        Map<GroupKey, Map<String, double[]>> map =
            calcIQR(exec.createSubExecutionContext(0.8), result, groupColNames, outlierColNames);

        // write the results
        return updateData(exec, in, map, groupColNames, outlierColNames);
    }

    /**
     * Convenience method returning the group column names stored in a list. If no group columns are selected an empty
     * list is returned.
     *
     * @param in the input data table
     * @return the list of group column names
     */
    private List<String> getGroupColNames(final BufferedDataTable in) {
        final List<String> groupColNames;
        if (m_useGroupsSetting.getBooleanValue()) {
            groupColNames = Arrays.asList(m_groupSettings.applyTo(in.getDataTableSpec()).getIncludes());
        } else {
            groupColNames = Collections.emptyList();
        }
        return groupColNames;
    }

    /**
     * Constructs the {@link GroupByTable} in accordance with the given settings.
     *
     * @param in the input data table
     * @param exec the execution context
     * @param groupColNames the group column names
     * @param outlierCols the outlier column names
     * @return the {@link GroupByTable} w.r.t. the selected settings
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private GroupByTable getGroupByTable(final BufferedDataTable in, final ExecutionContext exec,
        final List<String> groupColNames, final String[] outlierCols) throws CanceledExecutionException {

        // get the global settings
        final GlobalSettings gSettings = getGlobalSettings(in, groupColNames);

        // create the column aggregators
        final ColumnAggregator[] agg = getAggretators(in.getDataTableSpec(), gSettings, outlierCols);

        // init and return the GroupByTable obeying the chose memory settings
        final GroupByTable t;
        if (m_memorySetting.getBooleanValue()) {
            t = new MemoryGroupByTable(exec, in, groupColNames, agg, gSettings, false,
                ColumnNamePolicy.AGGREGATION_METHOD_COLUMN_NAME, false);
        } else {
            t = new BigGroupByTable(exec, in, groupColNames, agg, gSettings, false,
                ColumnNamePolicy.AGGREGATION_METHOD_COLUMN_NAME, false);
        }
        return t;
    }

    /**
     * Create the global settings used by the {@link GroupByTable}.
     *
     * @param in the input data table
     * @param groupColNames the group column names
     * @return the global settings
     */
    private GlobalSettings getGlobalSettings(final BufferedDataTable in, final List<String> groupColNames) {
        final GlobalSettingsBuilder builder = GlobalSettings.builder();

        builder.setAggregationContext(AggregationContext.COLUMN_AGGREGATION);
        builder.setDataTableSpec(in.getDataTableSpec());
        builder.setGroupColNames(groupColNames);
        builder.setMaxUniqueValues(DEFAULT_MAX_UNIQUE_VALS);
        builder.setValueDelimiter(GlobalSettings.STANDARD_DELIMITER);

        return builder.build();
    }

    /**
     * Creates column aggregators for each of the outlier columns.
     *
     * @param inSpec the input data table spec
     * @param gSettings the global settings
     * @param outlierCols the outlier column names
     * @return an array of column aggregators
     */
    private ColumnAggregator[] getAggretators(final DataTableSpec inSpec, final GlobalSettings gSettings,
        final String[] outlierCols) {
        final ColumnAggregator[] aggregators = new ColumnAggregator[outlierCols.length << 1];

        int pos = 0;
        // for each outlier column name create, depending on the selected memory policy,
        // two aggregators. The one calculates the first and the other the third quartile.
        for (final String outlierColName : outlierCols) {
            final OperatorColumnSettings cSettings =
                new OperatorColumnSettings(INCL_MISSING_CELLS, inSpec.getColumnSpec(outlierColName));
            for (final double percentile : PERCENTILE_VALUES) {
                AggregationMethod method = m_memorySetting.getBooleanValue()
                    ? new QuantileOperator(
                        new OperatorData("Quantile", true, false, DoubleValue.class, INCL_MISSING_CELLS), gSettings,
                        cSettings, percentile, m_estimationSettings.getStringValue())
                    : new PSquarePercentileOperator(gSettings, cSettings, percentile);
                aggregators[pos++] = new ColumnAggregator(cSettings.getOriginalColSpec(), method);
            }
        }
        return aggregators;
    }

    /**
     * Stores the results of the quartile calculation for each outlier w.r.t the different groups.
     *
     * @param subExec the execution context
     * @param data the data table holding the groups, and first and third quartile for each of the outlier columns
     * @param groupColNames the group column names
     * @param outlierColNames the outlier column names
     * @return a map from groups to pairs of columns and IQR
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private Map<GroupKey, Map<String, double[]>> calcIQR(final ExecutionContext subExec, final BufferedDataTable data,
        final List<String> groupColNames, final String[] outlierColNames) throws CanceledExecutionException {
        final Map<GroupKey, Map<String, double[]>> iqrGroupsMap = new HashMap<GroupKey, Map<String, double[]>>();

        final long rowCount = data.size();
        double rowCounter = 1;
        final int offset = groupColNames.size();
        final double scalar = m_scalarModel.getDoubleValue();

        for (final DataRow r : data) {
            subExec.checkCanceled();
            subExec.setProgress(rowCounter++ / rowCount, "Calculating IQR for row " + rowCounter + " of " + rowCount);

            // calculate the groups key
            final DataCell[] groupVals = new DataCell[groupColNames.size()];
            for (int i = 0; i < groupColNames.size(); i++) {
                groupVals[i] = r.getCell(i);
            }

            // calculate for the current key the IQR of all outliers
            final HashMap<String, double[]> colsIQR = new HashMap<String, double[]>();
            for (int i = 0; i < outlierColNames.length; i++) {
                final String outlierName = outlierColNames[i];
                final double[] interval = new double[2];
                int index = i * 2 + offset;
                // first quartile of the current column
                interval[0] = getDoubleValue(r.getCell(index));
                // third quartile of the current column
                interval[1] = getDoubleValue(r.getCell(++index));

                // calculate the scaled IQR
                final double iqr = scalar * (interval[1] - interval[0]);

                // update the interval
                interval[0] -= iqr;
                interval[1] += iqr;

                colsIQR.put(outlierName, interval);
            }

            // associate the groups key with the outliers' IQR
            iqrGroupsMap.put(new GroupKey(groupVals), colsIQR);

        }
        return iqrGroupsMap;
    }

    /**
     * Convenience method to access the value of a datacell.
     *
     * @param cell the cell holding the value
     * @return the double value of the cell
     */
    private double getDoubleValue(final DataCell cell) {
        return ((DoubleCell)cell).getDoubleValue();
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
     * @param map mapping between groups and the IQR for each outlier column
     * @param groupColNames the group column names
     * @param outlierColNames the outlier column names
     * @return a modified version of the input data table with proper outlier treatment
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private BufferedDataTable[] updateData(final ExecutionContext exec, final BufferedDataTable in,
        final Map<GroupKey, Map<String, double[]>> map, final List<String> groupColNames,
        final String[] outlierColNames) throws CanceledExecutionException {

        // table storing the result
        final BufferedDataTable out;

        // input data table spec
        DataTableSpec inSpec = in.getDataTableSpec();

        // store the positions where the group column names can be found in the input table
        final List<Integer> groupIndices = new ArrayList<Integer>(groupColNames.size());
        for (final String groupColName : groupColNames) {
            groupIndices.add(inSpec.findColumnIndex(groupColName));
        }

        if (TREATMENT_OPTIONS.getEnum(m_outlierTreatmentSettings.getStringValue()) == TREATMENT_OPTIONS.REPLACE) {

            // get the replacement strategy
            final REPLACEMENT_STRATEGY repOpt =
                REPLACEMENT_STRATEGY.getEnum(m_outlierReplacementSettings.getStringValue());

            // create column re-arranger to overwrite cells corresponding to outliers
            final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);

            final int noOutliers = outlierColNames.length;
            final int[] outlierIndices = new int[noOutliers];
            final DataColumnSpec[] outlierSpecs = new DataColumnSpec[noOutliers];
            for (int i = 0; i < noOutliers; i++) {
                outlierIndices[i] = inSpec.findColumnIndex(outlierColNames[i]);
                outlierSpecs[i] = inSpec.getColumnSpec(outlierIndices[i]);
            }
            // values are copied anyways by the re-arranger so there is no need to
            // create new instances for each row
            final DataCell[] treatedVals = new DataCell[noOutliers];

            final AbstractCellFactory fac = new AbstractCellFactory(outlierSpecs) {
                @Override
                public DataCell[] getCells(final DataRow row) {
                    final Map<String, double[]> colsMap = map.get(getGroupKey(groupIndices, row));
                    for (int i = 0; i < noOutliers; i++) {
                        // treat the value of the cell if its a outlier
                        treatedVals[i] = treatCellValue(repOpt, colsMap.get(outlierColNames[i]),
                            getDoubleValue(row.getCell(outlierIndices[i])));
                    }
                    return treatedVals;
                }
            };
            // replace the outlier columns by their updated versions
            colRearranger.replace(fac, outlierIndices);
            out = exec.createColumnRearrangeTable(in, colRearranger, exec);
        } else {
            // we remove all columns containing an outlier
            BufferedDataContainer container = exec.createDataContainer(in.getDataTableSpec());
            for (final DataRow row : in) {
                Map<String, double[]> colsMap = map.get(getGroupKey(groupIndices, row));
                boolean toInsert = true;
                for (final Entry<String, double[]> entry : colsMap.entrySet()) {
                    final int outlierInd = inSpec.findColumnIndex(entry.getKey());
                    final double val = getDoubleValue(row.getCell(outlierInd));
                    if (val < entry.getValue()[0] || val > entry.getValue()[1]) {
                        toInsert = false;
                        break;
                    }
                }
                if (toInsert) {
                    container.addRowToTable(row);
                }
            }
            container.close();
            out = container.getTable();
        }
        return new BufferedDataTable[]{out};
    }

    /**
     * Modifies the the value/type of the data cell if necessary according the selected outlier replacement strategy.
     *
     * @param repOpt the selected outlier replacement strategy
     * @param interval the IQR interval
     * @param val the value of the current data cell
     * @return the new data cell after replacing its value if necessary
     */
    private DataCell treatCellValue(final REPLACEMENT_STRATEGY repOpt, final double[] interval, double val) {
        // checks if the value is an outlier
        if (repOpt == REPLACEMENT_STRATEGY.MISSING && (val < interval[0] || val > interval[1])) {
            return DataType.getMissingCell();
        }
        // sets to the lower interval bound if necessary
        val = Math.max(val, interval[0]);
        // sets to the higher interval bound if necessary
        val = Math.min(val, interval[1]);
        return DoubleCellFactory.create(val);
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
        return new GroupKey(groupVals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // check if the table contains any row holding numerical values
        DataTableSpec in = inSpecs[0];
        if (!in.containsCompatibleType(DoubleValue.class)) {
            throw new InvalidSettingsException("No double compatible columns in input");
        }

        String[] includes;

        // check and initialize the groups settings model
        if (m_groupSettings == null) {
            m_groupSettings = createGroupFilterModel();
            //            m_groupSettings.loadDefaults(in);

            includes = m_groupSettings.applyTo(in).getIncludes();
            if (includes.length > 0) {
                setWarningMessage(
                    "Auto configuration: Groups use all suitable columns(in total " + includes.length + ")");
            }
        } else {
            includes = m_groupSettings.applyTo(in).getIncludes();
        }

        // check and initialize the outlier settings model
        if (m_outlierSettings == null) {
            m_outlierSettings = createOutlierFilterModel();
            m_outlierSettings.loadDefaults(in);
            includes = m_outlierSettings.applyTo(in).getIncludes();
            if (includes.length > 0) {
                setWarningMessage(
                    "Auto configuration: Outliers use all suitable columns (in total " + includes.length + ")");
            }
        } else {
            includes = m_outlierSettings.applyTo(in).getIncludes();
            if (includes.length == 0) {
                throw new InvalidSettingsException("Please include at leaste one numerical column!");
            }
        }

        // initialize the remaining settings models if necessary
        init();

        // we can only perform filtering on a single column. Only works since the initial treatment value is
        // an empty string
        final String treatment = m_outlierTreatmentSettings.getStringValue();
        if (includes.length > 1 && !treatment.isEmpty()
            && TREATMENT_OPTIONS.getEnum(treatment) == TREATMENT_OPTIONS.FILTER) {
            throw new InvalidSettingsException("Filtering outliers on multiple columns is currently disabled");
        }

        // return the output spec
        return new DataTableSpec[]{in};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_estimationSettings != null) {
            m_estimationSettings.saveSettingsTo(settings);
        }
        if (m_groupSettings != null) {
            m_groupSettings.saveSettingsTo(settings);
        }
        if (m_memorySetting != null) {
            m_memorySetting.saveSettingsTo(settings);
        }
        if (m_outlierSettings != null) {
            m_outlierSettings.saveSettingsTo(settings);
        }
        if (m_scalarModel != null) {
            m_scalarModel.saveSettingsTo(settings);
        }
        if (m_useGroupsSetting != null) {
            m_useGroupsSetting.saveSettingsTo(settings);
        }
        if (m_outlierReplacementSettings != null) {
            m_outlierReplacementSettings.saveSettingsTo(settings);
        }
        if (m_outlierTreatmentSettings != null) {
            m_outlierTreatmentSettings.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel model : getSettings()) {
            model.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel model : getSettings()) {
            model.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

    /**
     * Creates not yet initialized settings and returns an array storing all them.
     *
     * @return array holding all used settings models.
     */
    private SettingsModel[] getSettings() {
        init();
        return new SettingsModel[]{m_groupSettings, m_outlierSettings, m_estimationSettings, m_scalarModel,
            m_memorySetting, m_useGroupsSetting, m_outlierReplacementSettings, m_outlierTreatmentSettings};
    }

    /**
     * Creates all non-initialized settings.
     */
    private void init() {
        if (m_groupSettings == null) {
            m_groupSettings = createGroupFilterModel();
        }
        if (m_outlierSettings == null) {
            m_outlierSettings = createOutlierFilterModel();
        }
        if (m_estimationSettings == null) {
            m_estimationSettings = createEstimationModel();
        }
        if (m_scalarModel == null) {
            m_scalarModel = createScalarModel();
        }
        if (m_memorySetting == null) {
            m_memorySetting = createMemoryModel();
        }
        if (m_useGroupsSetting == null) {
            m_useGroupsSetting = createUseGroupsModel();
        }
        if (m_outlierReplacementSettings == null) {
            m_outlierReplacementSettings = createOutlierReplacementModel();
        }
        if (m_outlierTreatmentSettings == null) {
            m_outlierTreatmentSettings = createOutlierTreatmentModel();
        }
    }

    /**
     * Returns the settings model holding the factor to scale the IQR.
     *
     * @return the IQR scalar settings model
     */
    public static SettingsModelDouble createScalarModel() {
        return new SettingsModelDouble(CFG_TUKEY_PAR, DEFAULT_SCALAR);
    }

    /**
     * Returns the settings model of the columns to check for outliers.
     *
     * @return the outlier settings model
     */
    @SuppressWarnings("unchecked")
    public static SettingsModelColumnFilter2 createOutlierFilterModel() {
        return new SettingsModelColumnFilter2(CFG_PROCESS_COLS, DoubleValue.class);

    }

    /**
     * Returns the settings model holding the selected groups.
     *
     * @return the groups settings model
     */
    public static SettingsModelColumnFilter2 createGroupFilterModel() {
        return new SettingsModelColumnFilter2(CFG_GROUP_COLS);
    }

    /**
     * Returns the settings model indicating whether the algorithm should be executed in or out of memory.
     *
     * @return the memory settings model
     */
    public static SettingsModelBoolean createMemoryModel() {
        return new SettingsModelBoolean(CFG_MEM_POLICY, DEFAULT_MEM_POLICY);
    }

    /**
     * Returns the settings model holding information on how the quartiles are calculated if the algorithm is running
     * in-memory.
     *
     * @return the estimation type settings model
     */
    public static SettingsModelString createEstimationModel() {
        return new SettingsModelString(CFG_ESTIMATION_TYPE, EstimationType.values()[0].name());
    }

    /**
     * Returns the settings model telling whether to apply the algorithm to the selected groups or not.
     *
     * @return the use groups settings model
     */
    public static SettingsModelBoolean createUseGroupsModel() {
        return new SettingsModelBoolean(CFG_USE_GROUPS, false);
    }

    /**
     * Returns the settings model informing about the treatment of outliers (replace or filter).
     *
     * @return the outlier treatment settings model
     */
    public static SettingsModelString createOutlierTreatmentModel() {
        return new SettingsModelString(CFG_OUTLIER_TREATMENT, TREATMENT_OPTIONS.values()[0].toString());
    }

    /**
     * Returns the settings model informing about the selected replacement strategy (Missings or IQR).
     *
     * @return the outlier replacement settings model
     */
    public static SettingsModelString createOutlierReplacementModel() {
        return new SettingsModelString(CFG_OUTLIER_REPLACEMENT, REPLACEMENT_STRATEGY.values()[0].toString());
    }

}
