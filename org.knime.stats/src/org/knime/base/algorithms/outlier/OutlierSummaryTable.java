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
 *   Mar 1, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.algorithms.outlier;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.algorithms.outlier.helpers.Helper4TypeExtraction;
import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * Class responsible for creating the outlier summary table.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class OutlierSummaryTable {

    /** The name of the outlier column. */
    private static final String OUTLIER_COL_NAME = "Column name";

    /** The name of the groups column. */
    private static final String GROUPS_COL_NAME = "Groups name";

    /** The name of the outlier replacement count column. */
    private static final String REPLACEMENT_COUNT = "outlier count";

    /** The name of the member count column. */
    private static final String MEMBER_COUNT = "member count";

    /** The name of the column storing the upper bound. */
    private static final String UPPER_BOUND = "upper bound";

    /** The name of the column storing the lower bound. */
    private static final String LOWER_BOUND = "lower bound";

    /** The number of columns in the table. */
    private static final int COLS_NUM = 6;

    /**
     * Returns the spec of the table storing the permitted intervals and additional information about member counts.
     *
     * @param inSpec the in table spec
     * @param groupColNames the group columns names
     * @return the spec of the table storing the permitted intervals and additional information about member counts
     */
    static DataTableSpec getExtendedModelSpec(final DataTableSpec inSpec, final String[] groupColNames) {
        return getExtendedModelSpec(Helper4TypeExtraction.extractTypes(inSpec, groupColNames), groupColNames);
    }

    /**
     * Returns the spec of the data table storing the permitted intervals and additional information about member
     * counts.
     *
     * @param types the data types of the group columns
     * @param groupColNames the group columns names
     * @return the spec of the table storing the permitted intervals and additional information about member counts
     */

    private static DataTableSpec getExtendedModelSpec(final DataType[] types, final String[] groupColNames) {
        final DataColumnSpec[] specs = new DataColumnSpec[COLS_NUM];
        specs[0] = new DataColumnSpecCreator(OUTLIER_COL_NAME, StringCell.TYPE).createSpec();
        specs[1] = createListCellSpec(types, groupColNames).createSpec();
        specs[2] = new DataColumnSpecCreator(MEMBER_COUNT, IntCell.TYPE).createSpec();
        specs[3] = new DataColumnSpecCreator(REPLACEMENT_COUNT, IntCell.TYPE).createSpec();
        specs[4] = new DataColumnSpecCreator(LOWER_BOUND.trim(), DoubleCell.TYPE).createSpec();
        specs[5] = new DataColumnSpecCreator(UPPER_BOUND.trim(), DoubleCell.TYPE).createSpec();
        return new DataTableSpec(specs);
    }

    /**
     * Creates the group column list cell spec
     *
     * @param types the data types of group name columns
     * @param groupColNames the group name column names
     * @return the group column list cell spec
     */
    private static DataColumnSpecCreator createListCellSpec(final DataType[] types, final String[] groupColNames) {
        final DataColumnSpecCreator listSpecCreator;
        if (groupColNames.length != 0) {
            final DataType type = CollectionCellFactory.getElementType(types);
            listSpecCreator = new DataColumnSpecCreator(GROUPS_COL_NAME, ListCell.getCollectionType(type));
            listSpecCreator.setElementNames(groupColNames);
        } else {
            listSpecCreator = new DataColumnSpecCreator(GROUPS_COL_NAME,
                ListCell.getCollectionType(DataType.getMissingCell().getType()));
        }
        return listSpecCreator;
    }

    /**
     * Returns of the data table storing the permitted intervals and additional information about member counts.
     *
     * @param exec the execution context
     * @param outlierModel the outlier model
     * @param memberCounter the member counter
     * @param outlierRepCounter the outlier replacement counter
     * @param missingGroups the missing groups counter
     *
     * @return the data table storing the permitted intervals and additional information about member counts.
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    static BufferedDataTable getExtendedModel(final ExecutionContext exec, final OutlierModel outlierModel,
        final GroupsMemberCounterPerColumn memberCounter, final GroupsMemberCounterPerColumn outlierRepCounter,
        final GroupsMemberCounterPerColumn missingGroups) throws CanceledExecutionException {
        // create the data container storing the table
        final DataContainer container = exec.createDataContainer(
            getExtendedModelSpec(outlierModel.getGroupColTypes(), outlierModel.getGroupColNames()));

        // create the array storing the rows
        final DataCell[] row = new DataCell[COLS_NUM];
        int rowCount = 0;

        // the missing group keys
        Set<GroupKey> missingGroupKeys = missingGroups.getGroupKeys();

        // numerics used for the progress update
        final long outlierCount = outlierModel.getOutlierColNames().length;
        final double divisor = outlierCount;
        int colCount = 0;

        // write the rows
        for (final String outlierColName : outlierModel.getOutlierColNames()) {
            exec.checkCanceled();
            row[0] = StringCellFactory.create(outlierColName);
            for (Entry<GroupKey, Map<String, double[]>> entry : outlierModel.getEntries()) {
                final GroupKey key = entry.getKey();
                final double[] permInterval = entry.getValue().get(outlierColName);
                addRow(container, memberCounter, outlierRepCounter, row, outlierColName, key, permInterval, rowCount++);
            }
            if (missingGroupKeys.size() != 0) {
                for (final GroupKey key : missingGroupKeys) {
                    addRow(container, missingGroups, outlierRepCounter, row, outlierColName, key, null, rowCount++);
                }
            }
            final int count = ++colCount;
            exec.setProgress(count / divisor, () -> "Writing summary for column " + count + " of " + outlierCount);
        }
        // close the container and return the data table
        container.close();
        return exec.createBufferedDataTable(container.getTable(), exec);
    }

    /**
     * Adds the row to the container
     *
     * @param container the data container
     * @param memberCounter the member counter
     * @param outlierRepCounter the outlier replacement counter
     * @param row the data cell row
     * @param outlierColName the outlier column name
     * @param key the groups key
     * @param permInterval the permitted interval
     * @param rowCount the row count
     */
    private static void addRow(final DataContainer container, final GroupsMemberCounterPerColumn memberCounter,
        final GroupsMemberCounterPerColumn outlierRepCounter, final DataCell[] row, final String outlierColName,
        final GroupKey key, final double[] permInterval, final int rowCount) {
        row[1] = CollectionCellFactory.createListCell(Arrays.stream(key.getGroupVals()).collect(Collectors.toList()));
        row[2] = memberCounter.getCount(outlierColName, key);
        row[3] = outlierRepCounter.getCount(outlierColName, key);
        if (permInterval != null) {
            row[4] = DoubleCellFactory.create(permInterval[0]);
            row[5] = DoubleCellFactory.create(permInterval[1]);
        } else {
            row[4] = DataType.getMissingCell();
            row[5] = DataType.getMissingCell();
        }
        container.addRowToTable(new DefaultRow("Row" + rowCount, row));
    }
}
