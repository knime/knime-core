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
import java.util.Set;

import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * Class storing the permitted intervals for each group and outleir column combination.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class NumericOutliersModel {

    /** Missing group column exception prefix. */
    private static final String MISSING_GROUP_EXECPTION_PREFIX = "Table does not contain group column ";

    /** Uknown outlier name expection. */
    private static final String UNKNOWN_OUTLIER_EXCEPTION = "Unknown outlier name";

    /** Config key of outlier column names. */
    private static final String CFG_OUTLIER_COL_NAMES = "outliers";

    /** Config key of group column names. */
    private static final String CFG_GROUP_COL_NAMES = "groups";

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

    /** Map storing the permitted intervals for each group and outlier column. */
    private final Map<GroupKey, Map<String, double[]>> m_permIntervals;

    /** The group column names, */
    private String[] m_groupColNames;

    /** The outlier column names. */
    private String[] m_outlierColNames;

    /**
     * Constructor.
     *
     * @param inSpec the spec of the table storing the group and outlier column names
     * @param groupColNames the group column names
     * @param outlierColNames the outlier column names
     */
    NumericOutliersModel(final String[] groupColNames, final String[] outlierColNames) {
        // store the group column names
        m_groupColNames = groupColNames;

        // store the outlier column names
        m_outlierColNames = outlierColNames;

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
    static NumericOutliersModel loadInstance(final ModelContentRO model) throws InvalidSettingsException {
        final NumericOutliersModel outlierModel = new NumericOutliersModel(model.getStringArray(CFG_GROUP_COL_NAMES),
            model.getStringArray(CFG_OUTLIER_COL_NAMES));
        outlierModel.loadModel(model.getModelContent(CFG_DATA));
        return outlierModel;
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
        if (!Arrays.stream(m_outlierColNames).anyMatch(outlier::equals)) {
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
     * Get the outlier column names.
     *
     * @return the outlier column names
     */
    String[] getOutlierColNames() {
        return m_outlierColNames;
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
     * Remove the provided outlier columns from the model.
     *
     * @param colsToDrop the outlier columns to be removed
     */
    void dropOutliers(final List<String> colsToDrop) {
        // drop entries from outlier colnames
        m_outlierColNames = Arrays.stream(m_outlierColNames)//
            .filter(s -> !colsToDrop.contains(s))//
            .toArray(String[]::new);

        // drop entries from the map
        for (Map<String, double[]> map : m_permIntervals.values()) {
            colsToDrop.forEach(s -> map.remove(s));
        }
    }

    /**
     * Save the model to the provided model content.
     *
     * @param model the model to save to
     */
    void saveModel(final ModelContentWO model) {
        // store groups and outlier column names
        model.addStringArray(CFG_GROUP_COL_NAMES, m_groupColNames);
        model.addStringArray(CFG_OUTLIER_COL_NAMES, m_outlierColNames);

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
        final Enumeration<ModelContentRO> rowContents = model.children();
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

    /**
     * Returns a set view of the outlier model mappings.
     *
     * @return a set view of the outlier model mappings
     */
    public Set<Entry<GroupKey, Map<String, double[]>>> getEntries() {
        return m_permIntervals.entrySet();
    }

}
