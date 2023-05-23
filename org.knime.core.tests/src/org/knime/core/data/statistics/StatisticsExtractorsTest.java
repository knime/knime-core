/*
 * ------------------------------------------------------------------------
 *
r *  Copyright by KNIME AG, Zurich, Switzerland
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

import static org.knime.core.data.v2.TableExtractorUtil.extractData;

import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.statistics.StatisticsExtractors.DoubleSumExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.FirstQuartileExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.IntSumExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.KurtosisExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MaximumExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MeanAbsoluteDeviation;
import org.knime.core.data.statistics.StatisticsExtractors.MeanExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MedianExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MinimumExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.SkewnessExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.StandardDeviationExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.ThirdQuartileExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.ValidDoublesCounter;
import org.knime.core.data.statistics.StatisticsExtractors.VarianceExtractor;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.TableTestUtil.ObjectColumn;
import org.knime.testing.util.TableTestUtil.SpecBuilder;

/**
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class StatisticsExtractorsTest {
    @Test
    void testValidDoublesCounter() {

        final var table = TableTestUtil.createTableFromColumns(
            new ObjectColumn("double", DoubleCell.TYPE, new Object[]{Double.NaN, null, 1d, null, 2d, Double.NaN, 3d}));
        final var extractor = new ValidDoublesCounter(0);
        extractData(table, extractor);
        Assertions.assertThat(extractor.getOutput()).isEqualTo(3);
    }

    @Test
    void testIntSumExtractor() {
        Supplier<IntStream> intValues = () -> IntStream.of(1, 2, 5);
        var table =
            TableTestUtil.createTableFromColumns(new ObjectColumn("double", DoubleCell.TYPE, new Double[]{1d, 2d, 5d}),
                new ObjectColumn("int", IntCell.TYPE, intValues.get().boxed().toArray()));
        final var extractor = new IntSumExtractor(1);
        extractData(table, extractor);
        Assertions.assertThat(extractor.getOutput()).isEqualTo(intValues.get().sum());
    }

    @Test
    void testDoubleSumExtractor() {
        Supplier<DoubleStream> doubleValues = () -> DoubleStream.of(2d, 2d, 5d);
        var table = TableTestUtil.createTableFromColumns(
            new ObjectColumn("double", DoubleCell.TYPE, doubleValues.get().boxed().toArray()),
            new ObjectColumn("int", IntCell.TYPE, new Integer[]{1, 2, 5}));
        final var extractor = new DoubleSumExtractor(0);
        extractData(table, extractor);
        Assertions.assertThat(extractor.getOutput()).isEqualTo(doubleValues.get().sum());
    }

    @Test
    void testQuantileExtractors() {

        final var colIndex = 0;

        final var minExtractor = new MinimumExtractor(colIndex);
        final var q1Extractor = new FirstQuartileExtractor(colIndex);
        final var medianExtractor = new MedianExtractor(colIndex);
        final var q3Extractor = new ThirdQuartileExtractor(colIndex);
        final var maxExtractor = new MaximumExtractor(colIndex);

        final var spec = new SpecBuilder().addColumn("col", DoubleCell.TYPE).build();

        final var table1 = TableTestUtil.tableBuilder(spec) //
            .addRow(new DoubleCell(1d)) //
            .addRow(new DoubleCell(2d)) //
            .addRow(new DoubleCell(3d)) //
            .addRow(new DoubleCell(4d)) //
            .addRow(new DoubleCell(5d)) //
            .build().get();
        extractData(table1, minExtractor, q1Extractor, medianExtractor, q3Extractor, maxExtractor);
        Assertions.assertThat(minExtractor.getOutput()).isEqualTo(1d);
        Assertions.assertThat(q1Extractor.getOutput()).isEqualTo(1.5);
        Assertions.assertThat(medianExtractor.getOutput()).isEqualTo(3d);
        Assertions.assertThat(q3Extractor.getOutput()).isEqualTo(4.5);
        Assertions.assertThat(maxExtractor.getOutput()).isEqualTo(5d);

        final var table2 = TableTestUtil.tableBuilder(spec) //
            .addRow(new DoubleCell(1d)) //
            .addRow(new DoubleCell(2d)) //
            .addRow(new DoubleCell(3d)) //
            .addRow(new DoubleCell(4d)) //
            .addRow(new DoubleCell(5d)) //
            .addRow(new DoubleCell(6d)) //
            .build().get();
        extractData(table2, minExtractor, q1Extractor, medianExtractor, q3Extractor, maxExtractor);
        Assertions.assertThat(minExtractor.getOutput()).isEqualTo(1d);
        Assertions.assertThat(q1Extractor.getOutput()).isEqualTo(1.75);
        Assertions.assertThat(medianExtractor.getOutput()).isEqualTo(3.5);
        Assertions.assertThat(q3Extractor.getOutput()).isEqualTo(5.25);
        Assertions.assertThat(maxExtractor.getOutput()).isEqualTo(6d);

        final var table3 = TableTestUtil.tableBuilder(spec) //
            .build().get();
        extractData(table3, minExtractor, q1Extractor, medianExtractor, q3Extractor, maxExtractor);
        Assertions.assertThat(minExtractor.getOutput()).isNull();
        Assertions.assertThat(q1Extractor.getOutput()).isNull();
        Assertions.assertThat(medianExtractor.getOutput()).isNull();
        Assertions.assertThat(q3Extractor.getOutput()).isNull();
        Assertions.assertThat(maxExtractor.getOutput()).isNull();
    }

    @Test
    void testMeanExtractor() {
        Double[] doubleVals = IntStream.range(1, 10).asDoubleStream().boxed().toArray(Double[]::new);
        final var table = TableTestUtil.createTableFromColumns(new ObjectColumn("double", DoubleCell.TYPE, doubleVals));

        var meanExtractor = new MeanExtractor(0);
        extractData(table, meanExtractor);
        Assertions.assertThat(meanExtractor.getOutput()).isEqualTo(5.0);

        final var emptyTable =
            TableTestUtil.createTableFromColumns(new ObjectColumn("double", DoubleCell.TYPE, new Double[0]));
        extractData(emptyTable, meanExtractor);
        Assertions.assertThat(meanExtractor.getOutput()).isNaN();
    }

    @Test
    void testVarianceExtractor() {
        Double[] doubleVals = IntStream.range(1, 9).asDoubleStream().boxed().toArray(Double[]::new);
        final var table = TableTestUtil.createTableFromColumns(new ObjectColumn("double", DoubleCell.TYPE, doubleVals));

        var mean = 4.5;
        var variance = 6.0;
        var meanAbsoluteDeviationExt = new MeanAbsoluteDeviation(0, mean);
        var varianceExtractor = new VarianceExtractor(0, mean);
        var standardDeviationExtractor = new StandardDeviationExtractor(0, mean);
        extractData(table, meanAbsoluteDeviationExt, varianceExtractor, standardDeviationExtractor);
        Assertions.assertThat(meanAbsoluteDeviationExt.getOutput()).isEqualTo(2.0);
        Assertions.assertThat(varianceExtractor.getOutput()).isEqualTo(variance);
        Assertions.assertThat(standardDeviationExtractor.getOutput()).isEqualTo(Math.sqrt(variance));

        final var emptyTable =
            TableTestUtil.createTableFromColumns(new ObjectColumn("double", DoubleCell.TYPE, new Double[0]));
        extractData(emptyTable, meanAbsoluteDeviationExt, varianceExtractor, standardDeviationExtractor);
        Assertions.assertThat(meanAbsoluteDeviationExt.getOutput()).isNaN();
        Assertions.assertThat(varianceExtractor.getOutput()).isNaN();
        Assertions.assertThat(standardDeviationExtractor.getOutput()).isNaN();

        final var tableWithOneRow =
            TableTestUtil.createTableFromColumns(new ObjectColumn("double", DoubleCell.TYPE, new Double[]{mean}));
        extractData(tableWithOneRow, meanAbsoluteDeviationExt, varianceExtractor, standardDeviationExtractor);
        Assertions.assertThat(varianceExtractor.getOutput()).isNaN();
        Assertions.assertThat(standardDeviationExtractor.getOutput()).isNaN();

        var varianceExtractorTwoDDOF = new VarianceExtractor(0, mean, 2);
        var standardDeviationExtractorTwoDDOF = new StandardDeviationExtractor(0, mean, 2);

        final var tableWithTwoRows =
            TableTestUtil.createTableFromColumns(new ObjectColumn("double", DoubleCell.TYPE, new Double[]{mean, mean}));
        extractData(tableWithTwoRows, meanAbsoluteDeviationExt, varianceExtractor, varianceExtractorTwoDDOF,
            standardDeviationExtractor, standardDeviationExtractorTwoDDOF);
        Assertions.assertThat(varianceExtractor.getOutput()).isNotNaN();
        Assertions.assertThat(standardDeviationExtractor.getOutput()).isNotNaN();
        Assertions.assertThat(varianceExtractorTwoDDOF.getOutput()).isNaN();
        Assertions.assertThat(standardDeviationExtractorTwoDDOF.getOutput()).isNaN();

    }

    @Test
    void testSkewnessKurtosisExtractor() {
        Double[] doubleVals = IntStream.range(1, 8).asDoubleStream().boxed().toArray(Double[]::new);
        final var table = TableTestUtil.createTableFromColumns(new ObjectColumn("double", DoubleCell.TYPE, doubleVals));

        var mean = 4;
        var stdDeviation = 2;
        var biasedVariance = 4;
        var skewnessExtractor = new SkewnessExtractor(0, mean, stdDeviation);
        var kurtosisExtractor = new KurtosisExtractor(0, mean, biasedVariance);
        extractData(table, skewnessExtractor, kurtosisExtractor);
        Assertions.assertThat(skewnessExtractor.getOutput()).isZero();
        Assertions.assertThat(kurtosisExtractor.getOutput()).isEqualTo(-1.2);

        final var emptyTable =
            TableTestUtil.createTableFromColumns(new ObjectColumn("double", DoubleCell.TYPE, new Double[0]));
        extractData(emptyTable, skewnessExtractor, kurtosisExtractor);
        Assertions.assertThat(skewnessExtractor.getOutput()).isNaN();
        Assertions.assertThat(kurtosisExtractor.getOutput()).isNaN();
    }

    @Test
    void testCountUniqueExtractor() {
        var doubleVals = new Double[]{1.0, 1.0, 1.0, 4.11, 4.11, 29.3};
        final var table = TableTestUtil.createTableFromColumns(new ObjectColumn("double", DoubleCell.TYPE, doubleVals));
        var countUniqueExtractor = new StatisticsExtractors.CountUniqueExtractor();
        extractData(table, countUniqueExtractor);
        Assertions.assertThat(countUniqueExtractor.getNumberOfUniqueValues()).isEqualTo(3);
        var mostFrequentValues = countUniqueExtractor.getMostFrequentValues(2);
        Assertions.assertThat(mostFrequentValues.size()).isEqualTo(2);
        Assertions.assertThat(((DoubleValue)mostFrequentValues.get(0).getFirst()).getDoubleValue()).isEqualTo(1.0);
        Assertions.assertThat(mostFrequentValues.get(0).getSecond()).isEqualTo(3);
        Assertions.assertThat(((DoubleValue)mostFrequentValues.get(1).getFirst()).getDoubleValue()).isEqualTo(4.11);
        Assertions.assertThat(mostFrequentValues.get(1).getSecond()).isEqualTo(2);
    }

}
