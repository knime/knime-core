/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.testing.util.TableTestUtil.assertTableResults;
import static org.knime.testing.util.TableTestUtil.createTableFromColumns;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.Pair;
import org.knime.testing.util.TableTestUtil;

public class UnivariateStatisticsTest {

    private static final String[] stringValues = new String[]{"333", "55555", "4444", "22", "666666"};

    @Test
    void testGetStringLengthsTable() {
        final var table =
            createTableFromColumns(new TableTestUtil.ObjectColumn("string", StringCell.TYPE, stringValues));
        var exec = TableTestUtil.getExec();

        var outData = UnivariateStatistics.getStringLengths(table, exec);
        assertTableResults(outData, new String[]{"Integer"}, new Object[][]{{3, 5, 4, 2, 6}});
    }

    @Test
    void testGetFirstAndLastString() {
        final var table =
            createTableFromColumns(new TableTestUtil.ObjectColumn("string", StringCell.TYPE, stringValues));

        var outData = UnivariateStatistics.getFirstAndLastString(table);
        assertThat(outData.getFirst()).as("First string in array").isEqualTo(stringValues[0]);
        assertThat(outData.getSecond()).as("Last string in array").isEqualTo(stringValues[stringValues.length - 1]);
    }

    @Test
    void testFormatMostFrequentValuesString() {
        assertFormatMostFrequentValues(i -> new StringCell(Integer.toString(i)), StringCell.TYPE);
    }

    @Test
    void testFormatMostFrequentValuesDouble() {
        assertFormatMostFrequentValues(DoubleCell::new, DoubleCell.TYPE);
    }

    @Test
    void testFormatMostFrequentValuesCollection() {
        var type = ListCell
            .getCollectionType(CollectionCellFactory.getElementType(new DataType[]{StringCell.TYPE, IntCell.TYPE}));
        var valuesAndFreqs = List.of(new Pair<>(1, 3), new Pair<>(2, 2), new Pair<>(3, 1), new Pair<>(4, 1));
        var pairs = valuesAndFreqs.stream()
            .map(i -> new Pair<DataValue, Long>(
                CollectionCellFactory.createListCell(List.of(new StringCell(Integer.toString(i.getFirst())),
                    new IntCell((i.getFirst())), new IntCell(i.getFirst()))),
                (long)i.getSecond()))
            .collect(Collectors.toList());
        int totalObs = valuesAndFreqs.stream().mapToInt(Pair::getSecond).sum();
        var result = UnivariateStatistics.formatMostFrequentValues(pairs, type, totalObs);
        assertThat(result).isEqualTo(new String[]{"[1, 1, 1] (3; 42.86%)", "[2, 2, 2] (2; 28.57%)",
            "[3, 3, 3] (1; 14.29%)", "[4, 4, 4] (1; 14.29%)"});
    }

    private void assertFormatMostFrequentValues(final Function<Integer, DataValue> dataValueFactory,
        final DataType type) {
        var valuesAndFreqs = List.of(new Pair<>(1, 3), new Pair<>(2, 2), new Pair<>(3, 1), new Pair<>(4, 1));
        var pairs =
            valuesAndFreqs.stream().map(i -> new Pair<>(dataValueFactory.apply(i.getFirst()), (long)i.getSecond()))
                .collect(Collectors.toList());
        int totalObs = valuesAndFreqs.stream().mapToInt(Pair::getSecond).sum();
        var result = UnivariateStatistics.formatMostFrequentValues(pairs, type, totalObs);
        assertThat(result).isEqualTo(new String[]{"1 (3; 42.86%)", "2 (2; 28.57%)", "3 (1; 14.29%)", "4 (1; 14.29%)"});
    }

    @Test
    void testFormatMOstFrequentValuesWithMissing() {
        var pairs = List.of((DataValue)new StringCell("a"), new StringCell("b"), new StringCell("c")).stream()
            .map(v -> new Pair<>(v, (long)1)).collect(Collectors.toList());
        pairs.add(new Pair<>(null, (long)1));
        int totalObs = pairs.size();
        var result = UnivariateStatistics.formatMostFrequentValues(pairs, StringCell.TYPE, totalObs);
        assertThat(result).as("Most Common Strings")
            .isEqualTo(new String[]{"a (1; 25.0%)", "b (1; 25.0%)", "c (1; 25.0%)", "? (1; 25.0%)"});
    }
}
