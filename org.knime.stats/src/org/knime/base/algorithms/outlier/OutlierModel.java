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
 *   Feb 26, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.algorithms.outlier;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class OutlierModel {

    /** Unkown outlier column expection. */
    private static final String OUTLIER_COL_REMOVE_EXCEPTION =
        "At least one of the provided column names does not specify an outlier column";

    /** Missing group column exception prefix. */
    private static final String MISSING_GROUP_EXECPTION_PREFIX = "Table does not contain group column ";

    /** Uknown outlier name expection. */
    private static final String UNKNOWN_OUTLIER_EXCEPTION = "unknown outlier name";

    /** Config key of the model content holding the interval. */
    private static final String CFG_INTERVAL = "interval";

    /** Config key of the model content holding the interval columns. */
    private static final String CFG_INTERVAL_COLUMNS = "interval-columns";

    /** Config key of the model content holding the group key. */
    private static final String CFG_GROUP_KEY = "group-key";

    /** Config key of the model content holding the permitted intervals for each group, outlier pair. */
    private static final String CFG_DATA = "data";

    /** Config key of the model content holding th permitted intervals of each outlier for the current group. */
    private static final String CFG_ROW = "row";

    /** Suffix of the columns storing the groups. */
    private static final String GROUPS_SUFFIX = " (group)";

    /** Suffix of the columns storing the upper bound. */
    private static final String UPPER_BOUND_SUFFIX = " (upper bound)";

    /** Suffix of the columns storing the lower bound. */
    private static final String LOWER_BOUND_SUFFIX = " (lower bound)";

    /** Map storing the permitted intervals for each group and outlier column. */
    private final Map<GroupKey, Map<String, double[]>> m_permIntervals;

    /** The group column names, */
    private String[] m_groupColNames;

    /** The outlier model spec. */
    private DataTableSpec m_spec;

    /** The outlier column names. */
    private String[] m_outlierColNames;

    /**
     * Constructor.
     *
     * @param groupColNames the group column names
     * @param outlierColNames the outlier column names
     * @param inSpec the in spec of the table holding the permitted intervals
     */
    OutlierModel(final String[] groupColNames, final String[] outlierColNames, final DataTableSpec inSpec) {
        // store the group column names
        m_groupColNames = groupColNames;

        // store the outlier column names
        m_outlierColNames = outlierColNames;

        // translate the inspec to the model spec
        m_spec = getModelSpec(inSpec, m_groupColNames, m_outlierColNames);

        // initialize the map holding the permitted intervals for each group and outlier column
        m_permIntervals = new LinkedHashMap<GroupKey, Map<String, double[]>>();
    }

    /**
     * Creates an outlier model instance and loads the provided information.
     *
     * @param model the model content storing the model information
     * @param spec the spec of the outlier model
     * @return an outlier model instance holding the provided information
     * @throws InvalidSettingsException if the outliers defined by the model content differs from the outliers defining
     *             the intervals
     */
    static OutlierModel loadModel(final ModelContentRO model, final DataTableSpec spec)
        throws InvalidSettingsException {
        return new OutlierModel(model, spec);
    }

    /**
     * Constructor loading the model content into the outlier model.
     *
     * @param model the model content storing the model information
     * @param spec the spec of the outlier model
     * @throws InvalidSettingsException if the outliers defined by the model content differs from the outliers defining
     *             the intervals
     */
    private OutlierModel(final ModelContentRO model, final DataTableSpec spec) throws InvalidSettingsException {
        // store the spec
        m_spec = spec;

        // initialize the map holding the permitted intervals for each group and outlier column
        m_permIntervals = new LinkedHashMap<GroupKey, Map<String, double[]>>();

        // load the model
        loadModel(model);
    }

    /**
     * Stores the permitted interval for the given group-outlier tuple.
     *
     * @param key the group key
     * @param outlier the outlier column name
     * @param interval the permitted interval
     * @throws IllegalArgumentException if the provided outlier name was not provided when construction this instance of
     *             outlier model
     */
    void addEntry(final GroupKey key, final String outlier, final double[] interval) throws IllegalArgumentException {
        // check if the outlier column names contain the provided outlier string
        if (!Arrays.stream(m_outlierColNames).anyMatch(outlier::matches)) {
            throw new IllegalArgumentException(UNKNOWN_OUTLIER_EXCEPTION);
        }
        // add the key if necessary
        if (!m_permIntervals.containsKey(key)) {
            m_permIntervals.put(key, new HashMap<String, double[]>());
        }
        // store the permitted interval
        m_permIntervals.get(key).put(outlier, interval);
    }

    /**
     * Get the group column names.
     *
     * @return the group column names
     */
    String[] getGroupColNames() {
        return m_groupColNames;
    }

    /**
     * Parse the group column names that were used to create the provided outlier model spec.
     *
     * @param outlierModelSpec the outlier model spec to be parsed
     * @return the group column names used to create the provided outlier model spec
     */
    static String[] getGroups(final DataTableSpec outlierModelSpec) {
        return Arrays.stream(outlierModelSpec.getColumnNames())//
            .filter(s -> s.endsWith(GROUPS_SUFFIX))//
            .map(s -> s.substring(0, s.lastIndexOf(GROUPS_SUFFIX)))//
            .toArray(String[]::new);
    }

    /**
     * Get the outlier column names.
     *
     * @return the outlier column names
     */
    String[] getOutlierColNames() {
        return m_outlierColNames;
    }

    /**
     * Parse the outlier column names that were used to create the provided outlier model spec.
     *
     * @param outlierModelSpec the outlier model spec to be parsed
     * @return the outlier column names used to create the provided outlier model spec
     */
    static String[] getOutliers(final DataTableSpec outlierModelSpec) {
        return Arrays.stream(outlierModelSpec.getColumnNames())//
            .filter(s -> s.endsWith(LOWER_BOUND_SUFFIX))//
            .map(s -> s.substring(0, s.lastIndexOf(LOWER_BOUND_SUFFIX)))//
            .toArray(String[]::new);
    }

    /**
     * Returns the map storing the intervals of all outlier columns for the provided group key.
     *
     * @param key the group key
     * @return the map storing the intervals of all outlier columns for the provided group key
     */
    Map<String, double[]> getGroupIntervals(final GroupKey key) {
        return m_permIntervals.get(key);
    }

    /**
     * Returns the group key for the given row.
     *
     * @param row the row storing the group key
     * @param spec the table spec of the provided row
     * @return the group key for the given row.
     */
    GroupKey getKey(final DataRow row, final DataTableSpec spec) {
        // array storing the group key
        final DataCell[] cellVals = new DataCell[m_groupColNames.length];
        // create the group key
        for (int i = 0; i < m_groupColNames.length; i++) {
            // find the position where the group column is located in the spec if the group is not contained
            // throw an exception
            final int index = spec.findColumnIndex(m_groupColNames[i]);
            if (index >= 0) {
                cellVals[i] = row.getCell(index);
            } else {
                throw new IllegalArgumentException(MISSING_GROUP_EXECPTION_PREFIX + m_groupColNames[i]);
            }
        }
        // return the group key
        return new GroupKey(cellVals);
    }

    /**
     * Translates the provided spec into the model spec by changing the column names.
     *
     * @param inSpec the input spec
     * @param groupNames the group column names
     * @param outlierNames the outlier column names
     * @return the model spec
     */
    static DataTableSpec getModelSpec(final DataTableSpec inSpec, final String[] groupNames,
        final String[] outlierNames) {
        // the new column spec
        final DataColumnSpec[] colSpec = new DataColumnSpec[groupNames.length + 2 * outlierNames.length];

        // for each groups column add the GROUPS_SUFFIX
        final int numGroupNames = groupNames.length;
        for (int i = 0; i < numGroupNames; i++) {
            final DataColumnSpecCreator creator = new DataColumnSpecCreator(inSpec.getColumnSpec(groupNames[i]));
            creator.setName(groupNames[i] + GROUPS_SUFFIX);
            colSpec[i] = creator.createSpec();
        }

        // for each outlier column add the LOWER/UPPER_BOUND_SUFFIX
        for (int i = 0; i < outlierNames.length; i++) {
            final int index = 2 * i + numGroupNames;
            colSpec[index] =
                new DataColumnSpecCreator(outlierNames[i] + LOWER_BOUND_SUFFIX, DoubleCell.TYPE).createSpec();
            colSpec[index + 1] =
                new DataColumnSpecCreator(outlierNames[i] + UPPER_BOUND_SUFFIX, DoubleCell.TYPE).createSpec();
        }

        // return the model spec
        return new DataTableSpec(colSpec);
    }

    /**
     * Returns the model spec.
     *
     * @return the model spec
     */
    DataTableSpec getModelSpec() {
        return m_spec;
    }

    /**
     * Returns the data table holding the permitted intervals for each group and outlier column.
     *
     * @return the data table holding the permitted intervals
     */
    DataTable getModel() {
        // create the data container storing the table
        final DataContainer container = new DataContainer(m_spec);

        // create the array storing the rows
        final DataCell[] row = new DataCell[m_groupColNames.length + 2 * m_outlierColNames.length];

        int rowCount = 0;
        final int numGroupNames = m_groupColNames.length;

        // for each group create a single row
        for (Entry<GroupKey, Map<String, double[]>> entry : m_permIntervals.entrySet()) {
            // store the groups key
            final DataCell[] keyVals = entry.getKey().getGroupVals();
            for (int i = 0; i < numGroupNames; i++) {
                row[i] = keyVals[i];
            }
            // store the intervals
            final Map<String, double[]> groupInterval = entry.getValue();
            for (int i = 0; i < m_outlierColNames.length; i++) {
                final int index = i * 2 + numGroupNames;
                final double[] interval = groupInterval.get(m_outlierColNames[i]);
                // if the interval is null we set missing values
                if (interval != null) {
                    row[index] = DoubleCellFactory.create(interval[0]);
                    row[index + 1] = DoubleCellFactory.create(interval[1]);
                } else {
                    row[index] = DataType.getMissingCell();
                    row[index + 1] = DataType.getMissingCell();
                }
            }
            // add the row
            container.addRowToTable(new DefaultRow("Row" + rowCount++, row));
        }
        // close the container and return the data table
        container.close();
        return container.getTable();
    }

    /**
     * Remove the provided outlier columns from the model.
     *
     * @param colsToDrop the outlier columns to be removed
     */
    void dropOutliers(final List<String> colsToDrop) {

        // drop entries from outlier colnames
        m_outlierColNames =
            Arrays.stream(m_outlierColNames).filter(s -> !colsToDrop.contains(s)).toArray(String[]::new);

        // drop entries from the map
        for (Map<String, double[]> map : m_permIntervals.values()) {
            colsToDrop.forEach(s -> map.remove(s));
        }

        // drop entries from the spec
        m_spec = dropOutlierColsFromSpec(m_spec, colsToDrop);
    }

    /**
     * Drops the provided outlier columns from the outlier model spec.
     *
     * @param outlierModelSpec the outlier model spec
     * @param outlierColsToDrop the outlier columns to be removed
     * @return the filtered outlier model spec
     * @throws IllegalArgumentException if any of the outlier columns to be drop is not available in the provided spec
     */
    static DataTableSpec dropOutlierColsFromSpec(final DataTableSpec outlierModelSpec,
        final List<String> outlierColsToDrop) throws IllegalArgumentException {

        // array storing the new spec
        final DataColumnSpec[] specs =
            new DataColumnSpec[outlierModelSpec.getNumColumns() - 2 * outlierColsToDrop.size()];
        int pos = 0;
        // only columns with LOWER/UPPER_BOUND_SUFFIX have to be droped
        for (DataColumnSpec oldSpec : outlierModelSpec) {
            String name = oldSpec.getName();
            if (name.endsWith(LOWER_BOUND_SUFFIX)) {
                name = name.substring(0, name.lastIndexOf(LOWER_BOUND_SUFFIX));
            } else if (name.endsWith(UPPER_BOUND_SUFFIX)) {
                name = name.substring(0, name.lastIndexOf(UPPER_BOUND_SUFFIX));
            }
            if (!outlierColsToDrop.contains(name)) {
                if (pos == specs.length) {
                    throw new IllegalArgumentException(OUTLIER_COL_REMOVE_EXCEPTION);
                }
                specs[pos++] = oldSpec;
            }
        }

        // return the new table spec
        return new DataTableSpec(specs);
    }

    /**
     * Save the model to the provided model content.
     *
     * @param model the model to save to
     */
    void saveModel(final ModelContentWO model) {
        // store groups and outlier column names
        model.addStringArray("groups", m_groupColNames);
        model.addStringArray("outliers", m_outlierColNames);

        // store the permitted intervals for each group and outlier column
        final ModelContentWO data = model.addModelContent(CFG_DATA);

        int rowCount = 0;
        for (Entry<GroupKey, Map<String, double[]>> entry : m_permIntervals.entrySet()) {
            final ModelContentWO rowContent = data.addModelContent(CFG_ROW + rowCount++);

            // store the group key
            rowContent.addModelContent("key").addDataCellArray(CFG_GROUP_KEY, entry.getKey().getGroupVals());

            // store the permitted intervals for the given key
            ModelContentWO intervalCols = rowContent.addModelContent(CFG_INTERVAL_COLUMNS);
            int intervalCount = 0;
            for (Entry<String, double[]> interval : entry.getValue().entrySet()) {
                final ModelContentWO rowIntervalContent = intervalCols.addModelContent(CFG_INTERVAL + intervalCount++);
                rowIntervalContent.addString("outlier", interval.getKey());
                rowIntervalContent.addDoubleArray(CFG_INTERVAL, interval.getValue());
            }
        }
    }

    /**
     * Loads the information provided by the model content into the model.
     *
     * @param model the model content to be loaded
     * @throws InvalidSettingsException if the outliers defined by the model content differs from the outliers defining
     *             the intervals
     */
    @SuppressWarnings("unchecked")
    private void loadModel(final ModelContentRO model) throws InvalidSettingsException {
        // load groups and outlier column names
        m_groupColNames = model.getStringArray("groups");
        m_outlierColNames = model.getStringArray("outliers");

        // load the permitted intervals for each group and outlier column
        final ModelContentRO data = model.getModelContent(CFG_DATA);

        final Enumeration<ModelContentRO> rowContents = data.children();
        while (rowContents.hasMoreElements()) {
            // acess the current group
            final ModelContentRO rowContent = rowContents.nextElement();

            // load the group key
            final GroupKey key = new GroupKey(rowContent.getModelContent("key").getDataCellArray(CFG_GROUP_KEY));

            // load all intervals for the current group key
            ModelContentRO intervalCols = rowContent.getModelContent(CFG_INTERVAL_COLUMNS);
            final Enumeration<ModelContentRO> intervalContents = intervalCols.children();
            while (intervalContents.hasMoreElements()) {
                final ModelContentRO intervalContent = intervalContents.nextElement();
                addEntry(key, intervalContent.getString("outlier"), intervalContent.getDoubleArray(CFG_INTERVAL));
            }
        }
    }

}
