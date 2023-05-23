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
 */

package org.knime.core.data.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

/**
 * Given an input table, compute a statistics table in which each row contains statistics about a column in the input
 * table.
 *
 * @author Juan Diaz Baquero
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
final class StatisticsTableUtil {

    private StatisticsTableUtil() {
        // utilities
    }

    /**
     * Construct a row for the resulting statistics table containing statistics for a column in the input table.
     *
     * @param columnStatistics Computed statistics for a column of the input table
     * @param selectedStatistics The statistics to include
     * @return A row containing statistics values
     */
    @SuppressWarnings("java:S1541") // complexity
    static DataRow createTableRow(final UnivariateStatistics columnStatistics,
        final Collection<UnivariateStatistics.Statistic> selectedStatistics) {
        final List<DataCell> cells = new ArrayList<>();
        if (selectedStatistics.contains(UnivariateStatistics.Statistic.NAME)) {
            cells.add(new StringCell(columnStatistics.getName()));
        }
        if (selectedStatistics.contains(UnivariateStatistics.Statistic.TYPE)) {
            cells.add(new StringCell(columnStatistics.getType()));
        }
        if (selectedStatistics.contains(UnivariateStatistics.Statistic.NUMBER_UNIQUE_VALUES)) {
            cells.add(new LongCell(columnStatistics.getNumberUniqueValues()));
        }

        if (selectedStatistics.contains(UnivariateStatistics.Statistic.MINIMUM)) {
            cells.add(getStringOrMissingCell(columnStatistics.getFirstValue()));
        }
        if (selectedStatistics.contains(UnivariateStatistics.Statistic.MAXIMUM)) {
            cells.add(getStringOrMissingCell(columnStatistics.getLastValue()));
        }

        if (selectedStatistics.contains(UnivariateStatistics.Statistic.K_MOST_COMMON)) {
            cells.add(getStringOrMissingCell(columnStatistics.getCommonValues()));
        }

        var allQuantilesData = columnStatistics.getQuantiles();
        var allQuantiles = List.of(UnivariateStatistics.Statistic.QUANTILE_1, UnivariateStatistics.Statistic.QUANTILE_5,
            UnivariateStatistics.Statistic.QUANTILE_10, UnivariateStatistics.Statistic.QUANTILE_25,
            UnivariateStatistics.Statistic.QUANTILE_50, UnivariateStatistics.Statistic.QUANTILE_75,
            UnivariateStatistics.Statistic.QUANTILE_90, UnivariateStatistics.Statistic.QUANTILE_95,
            UnivariateStatistics.Statistic.QUANTILE_99);
        IntStream.range(0, allQuantiles.size()) //
            .filter(i -> selectedStatistics.contains(allQuantiles.get(i))) //
            .forEach(i -> cells.add(getDoubleOrMissingCell(allQuantilesData[i])));

        if (selectedStatistics.contains(UnivariateStatistics.Statistic.MEAN)) {
            cells.add(getDoubleOrMissingCell(columnStatistics.getMean().orElse(null)));
        }
        if (selectedStatistics.contains(UnivariateStatistics.Statistic.SUM)) {
            cells.add(getDoubleOrMissingCell(columnStatistics.getSum().orElse(null)));
        }
        if (selectedStatistics.contains(UnivariateStatistics.Statistic.MEAN_ABSOLUTE_DEVIATION)) {
            cells.add(getDoubleOrMissingCell(columnStatistics.getMeanAbsoluteDeviation().orElse(null)));
        }
        if (selectedStatistics.contains(UnivariateStatistics.Statistic.STD_DEVIATION)) {
            cells.add(getDoubleOrMissingCell(columnStatistics.getStandardDeviation().orElse(null)));
        }
        if (selectedStatistics.contains(UnivariateStatistics.Statistic.VARIANCE)) {
            cells.add(getDoubleOrMissingCell(columnStatistics.getVariance().orElse(null)));
        }
        if (selectedStatistics.contains(UnivariateStatistics.Statistic.SKEWNESS)) {
            cells.add(getDoubleOrMissingCell(columnStatistics.getSkewness().orElse(null)));
        }
        if (selectedStatistics.contains(UnivariateStatistics.Statistic.KURTOSIS)) {
            cells.add(getDoubleOrMissingCell(columnStatistics.getKurtosis().orElse(null)));
        }

        return new DefaultRow(columnStatistics.getName(), cells.toArray(new DataCell[0]));
    }

    /**
     * @param columnNames an array with a {@link String} for every new table on the array
     * @param dataTypes an array with a {@link DataType} for the column on every new table
     * @param exec ExecutionContext for the table creation
     * @return a Map with every given string as key and an empty DataContainer with a single column of the type given
     */
    static Map<String, BufferedDataContainer> createEmptyContainersPerDimension(final String[] columnNames,
        final DataType[] dataTypes, final ExecutionContext exec) {
        Map<String, BufferedDataContainer> map = new LinkedHashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            map.put(columnNames[i], exec
                .createDataContainer(new DataTableSpec(new String[]{columnNames[i]}, new DataType[]{dataTypes[i]})));
        }
        return map;
    }

    /**
     * @param table whose columns will be split
     * @param splitColNames the name of the columns to be splitted
     * @param ignoreMissingValues defines if the result table should contain any missing values
     * @param ignoreNaNValues defined if the result table should ignore any NaN (not a number) values, defined according
     *            to {@link Double#isNaN(double)}
     * @param exec Execution Context to create the new tables from
     * @return A mapping from column name to a {@link BufferedDataTable} containing the single column with that name.
     */
    static Map<String, BufferedDataTable> splitTableByColumnNames(final BufferedDataTable table,
        final String[] splitColNames, final boolean ignoreMissingValues, final boolean ignoreNaNValues,
        final ExecutionContext exec) {
        final var spec = table.getSpec();
        final var dataTypes =
            Stream.of(splitColNames).map(dimName -> spec.getColumnSpec(dimName).getType()).toArray(DataType[]::new);
        final var containers = createEmptyContainersPerDimension(splitColNames, dataTypes, exec);
        final var columnIndices = spec.columnsToIndices(splitColNames);

        for (var row : table) {
            for (int i = 0; i < splitColNames.length; i++) {
                final var cell = row.getCell(columnIndices[i]);
                if ((ignoreMissingValues && cell.isMissing()) || (ignoreNaNValues && isCellNaN(cell))) {
                    continue;
                }
                containers.get(splitColNames[i]).addRowToTable(new DefaultRow(row.getKey(), cell));
            }
        }
        containers.forEach((key, value) -> value.close());
        return containers.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, container -> container.getValue().getTable()));
    }

    private static boolean isCellNaN(final DataCell cell) {
        return !cell.isMissing() && cell.getType().isCompatible(DoubleValue.class)
            && Double.isNaN(((DoubleValue)cell).getDoubleValue());
    }

    private static DataCell getDoubleOrMissingCell(final Double value) {
        return value != null ? new DoubleCell(value) : DataType.getMissingCell();
    }

    private static DataCell getStringOrMissingCell(final String value) {
        return value != null ? new StringCell(value) : DataType.getMissingCell();
    }

}
