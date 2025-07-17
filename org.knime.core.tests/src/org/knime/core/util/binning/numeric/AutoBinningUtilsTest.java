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
 *   Jun 20, 2025 (david): created
 */
package org.knime.core.util.binning.numeric;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.util.binning.numeric.AutoBinningSettings.BinBoundary;
import org.knime.core.util.binning.numeric.AutoBinningSettings.BinBoundaryExactMatchBehaviour;
import org.knime.core.util.binning.numeric.AutoBinningSettings.BinNaming;
import org.knime.core.util.binning.numeric.AutoBinningSettings.BinNamingSettings;
import org.knime.core.util.binning.numeric.AutoBinningSettings.BinningSettings;
import org.knime.core.util.binning.numeric.AutoBinningSettings.ColumnOutputNamingSettings;
import org.knime.core.util.binning.numeric.AutoBinningSettings.DataBoundsSettings;
import org.knime.core.util.binning.numeric.AutoBinningSettings.DataBoundsSettings.BoundSetting;
import org.knime.core.util.binning.numeric.AutoBinningSettings.NumberFormatSettingsGroup;
import org.knime.core.util.binning.numeric.AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat;
import org.knime.core.util.binning.numeric.AutoBinningSettings.NumberFormatSettingsGroup.PrecisionMode;
import org.knime.core.util.binning.numeric.AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection;
import org.knime.core.util.binning.numeric.AutoBinningSettings.NumberFormattingSettings.ColumnFormat;
import org.knime.core.util.binning.numeric.AutoBinningSettings.NumberFormattingSettings.CustomFormat;
import org.knime.core.util.binning.numeric.PMMLPreprocDiscretizeTranslatorConfiguration.ClosureStyle;
import org.knime.core.util.binning.numeric.PMMLPreprocDiscretizeTranslatorConfiguration.Interval;
import org.knime.testing.util.InputTableNode;
import org.knime.testing.util.TableTestUtil;

/**
 * Tests for the {@link AutoBinningUtils} class.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class AutoBinningUtilsTest {

    ExecutionContext m_exec;

    @Test
    void testFindStreakEndpoints() {
        var endPoints =
            AutoBinningUtils.findStreakEndpoints(List.of(0.0, 2.05, 2.1, 2.1, 2.1, 2.2, 4.0, 5.0), 3, false);

        assertEquals(2, endPoints.first(), "First endpoint should be at correct index");
        assertEquals(4, endPoints.second(), "Second endpoint should be at correct index");

        var endPointsWithIntegerCoercion =
            AutoBinningUtils.findStreakEndpoints(List.of(0.0, 2.05, 2.1, 2.1, 2.1, 2.2, 4.0, 5.0), 3, true);

        assertEquals(1, endPointsWithIntegerCoercion.first(),
            "First endpoint with integer coercion should be at correct index");
        assertEquals(5, endPointsWithIntegerCoercion.second(),
            "Second endpoint with integer coercion should be at correct index");
    }

    @Nested
    class BinNameNumberFormatTests {

        record TestCase(NumberFormat fmt, int precision, RoundingDirection roundingMode, PrecisionMode precisionMode,
            double firstBinBoundary, String expectedBinName) {
        }

        static Stream<Arguments> provideTestParameters() {
            return Stream.of( //
                new TestCase(NumberFormat.ENGINEERING_STRING, 1, RoundingDirection.CEILING,
                    PrecisionMode.SIGNIFICANT_FIGURES, 12345, "20E+3"), //
                new TestCase(NumberFormat.ENGINEERING_STRING, 2, RoundingDirection.FLOOR,
                    PrecisionMode.SIGNIFICANT_FIGURES, 12345, "12E+3"), //
                new TestCase(NumberFormat.ENGINEERING_STRING, 2, RoundingDirection.DOWN, PrecisionMode.DECIMAL_PLACES,
                    12345.6789, "12345.67"), //
                new TestCase(NumberFormat.PLAIN_STRING, 2, RoundingDirection.UP, PrecisionMode.DECIMAL_PLACES,
                    12345.6789, "12345.68"), //
                new TestCase(NumberFormat.PLAIN_STRING, 2, RoundingDirection.CEILING, PrecisionMode.SIGNIFICANT_FIGURES,
                    12345.6789, "13000"), //
                new TestCase(NumberFormat.STANDARD_STRING, 2, RoundingDirection.FLOOR,
                    PrecisionMode.SIGNIFICANT_FIGURES, 12345.6789, "1.2E+4"), //
                new TestCase(NumberFormat.STANDARD_STRING, 3, RoundingDirection.DOWN, PrecisionMode.DECIMAL_PLACES,
                    12345.6789, "12345.678") //
            ).map(Arguments::of);
        }

        @ParameterizedTest
        @MethodSource("provideTestParameters")
        void testBinNameNumberFormatting(final TestCase tc) throws InvalidSettingsException {
            var numberFormatSettingsGroup =
                new NumberFormatSettingsGroup(tc.fmt, tc.precision, tc.precisionMode, tc.roundingMode);

            var settings = new BinNamingSettings(BinNaming.MIDPOINTS, new CustomFormat(numberFormatSettingsGroup));

            var data = createDataTable( //
                new Column("col1", new double[]{tc.firstBinBoundary, tc.firstBinBoundary}) //
            ).get();

            var edges = AutoBinningUtils.createEdgesForEqualWidth(data.getSpec().getColumnSpec(0),
                OptionalDouble.empty(), OptionalDouble.empty(), false, 1);
            var binName = AutoBinningUtils.createBins(Map.of("col1", edges), settings).get("col1").get(0).getBinName();

            assertEquals(tc.expectedBinName, binName, "Bin name should match expected name");
        }
    }

    @Nested
    class EdgesToBinsTests {

        private Map<String, List<BinBoundary>> m_data;

        @BeforeEach
        void setupData() {
            m_data = Map.of( //
                "col1", List.of( //
                    new BinBoundary(1.0, BinBoundaryExactMatchBehaviour.TO_UPPER_BIN), //
                    new BinBoundary(2.5, BinBoundaryExactMatchBehaviour.TO_UPPER_BIN), //
                    new BinBoundary(4.0, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN) //
                ), //
                "col2", List.of( //
                    new BinBoundary(10.0, BinBoundaryExactMatchBehaviour.TO_UPPER_BIN), //
                    new BinBoundary(25.0, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN), //
                    new BinBoundary(26.0, BinBoundaryExactMatchBehaviour.TO_UPPER_BIN), //
                    new BinBoundary(39.0, BinBoundaryExactMatchBehaviour.TO_UPPER_BIN) //
                ) //
            );
        }

        record TestCase(BinNamingSettings naming, Map<String, String[]> expectedBinNames) {
        }

        static Stream<Arguments> provideTestParameters() {
            return Stream.of( //
                Arguments.of("numbered", new TestCase( //
                    new BinNamingSettings(BinNaming.NUMBERED, new ColumnFormat()), //
                    Map.of( //
                        "col1", new String[]{"Bin 1", "Bin 2"}, //
                        "col2", new String[]{"Bin 1", "Bin 2", "Bin 3"} //
                    ) //
                )), //
                Arguments.of("midpoints", new TestCase( //
                    new BinNamingSettings(BinNaming.MIDPOINTS, new ColumnFormat()), //
                    Map.of( //
                        "col1", new String[]{"1.75", "3.25"}, //
                        "col2", new String[]{"17.5", "25.5", "32.5"} //
                    ) //
                )), //
                Arguments.of("borders", new TestCase( //
                    new BinNamingSettings(BinNaming.BORDERS, new ColumnFormat()), //
                    Map.of( //
                        "col1", new String[]{"[1, 2.5)", "[2.5, 4]"}, //
                        "col2", new String[]{"[10, 25]", "(25, 26)", "[26, 39)"} //
                    ) //
                )) //
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideTestParameters")
        void testCreateBins(@SuppressWarnings("unused") final String name, final TestCase tc)
            throws InvalidSettingsException {
            var bins = AutoBinningUtils.createBins(m_data, tc.naming);

            assertEquals(2, bins.size(), "Expected 2 outputs for 2 columns");
            assertTrue(bins.containsKey("col1"), "Bins should contain col1");
            assertTrue(bins.containsKey("col2"), "Bins should contain col2");

            var outputCol1 = List.of( //
                new Interval(1.0, 2.5, ClosureStyle.CLOSED_OPEN).toNumericBin(tc.expectedBinNames.get("col1")[0]), //
                new Interval(2.5, 4.0, ClosureStyle.CLOSED_CLOSED).toNumericBin(tc.expectedBinNames.get("col1")[1]) //
            );

            var outputCol2 = List.of( //
                new Interval(10.0, 25.0, ClosureStyle.CLOSED_CLOSED).toNumericBin(tc.expectedBinNames.get("col2")[0]), //
                new Interval(25.0, 26.0, ClosureStyle.OPEN_OPEN).toNumericBin(tc.expectedBinNames.get("col2")[1]), //
                new Interval(26.0, 39.0, ClosureStyle.CLOSED_OPEN).toNumericBin(tc.expectedBinNames.get("col2")[2]) //
            );

            assertEquals(outputCol1, bins.get("col1"), "Bins for col1 should match expected output");
            assertEquals(outputCol2, bins.get("col2"), "Bins for col2 should match expected output");
        }
    }

    @Nested
    class TestsForEdgeCreation {

        @Nested
        class TestsForEqualWidthEdgeCreation {

            @Test
            void testNoBoundsAndNoIntegerForcing() {
                var data = createDataTable( //
                    new Column("col1", new double[]{1, 2, 3, 4}) //
                );

                var edges = AutoBinningUtils.createEdgesForEqualWidth( //
                    data.get().getDataTableSpec().getColumnSpec(0), //
                    OptionalDouble.empty(), //
                    OptionalDouble.empty(), //
                    false, //
                    2 //
                );

                assertEquals(3, edges.size(), "Expected 3 edges");
                assertEquals(List.of(1.0, 2.5, 4.0),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(0).exactMatchBehaviour(),
                    "First boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(1).exactMatchBehaviour(),
                    "Second boundary should match to to lower bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(2).exactMatchBehaviour(),
                    "Last boundary should match to lower bin");
            }

            @Test
            void testBoundsAndNoIntegerForcing() {
                var data = createDataTable( //
                    new Column("col1", new double[]{0, 1, 2, 3, 4, 5}) //
                );

                var edges = AutoBinningUtils.createEdgesForEqualWidth( //
                    data.get().getDataTableSpec().getColumnSpec(0), //
                    OptionalDouble.of(1.0), // lower bound
                    OptionalDouble.of(4.0), // upper bound
                    false, // no integer forcing
                    2 // number of bins
                );

                assertEquals(3, edges.size(), "Expected 3 edges");
                assertEquals(List.of(1.0, 2.5, 4.0),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(0).exactMatchBehaviour(),
                    "First boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(1).exactMatchBehaviour(),
                    "Second boundary should match to lower bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(2).exactMatchBehaviour(),
                    "Last boundary should match to lower bin");
            }

            @Test
            void testNoBoundsAndIntegerForcing() {
                var data = createDataTable( //
                    new Column("col1", new double[]{1, 2, 3, 4}) //
                );

                var edges = AutoBinningUtils.createEdgesForEqualWidth( //
                    data.get().getDataTableSpec().getColumnSpec(0), //
                    OptionalDouble.empty(), // no lower bound
                    OptionalDouble.empty(), // no upper bound
                    true, // integer forcing
                    2 // number of bins
                );

                assertEquals(3, edges.size(), "Expected 3 edges");
                assertEquals(List.of(1.0, 3.0, 4.0),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(0).exactMatchBehaviour(),
                    "First boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(1).exactMatchBehaviour(),
                    "Second boundary should match to lower bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(2).exactMatchBehaviour(),
                    "Last boundary should match to lower bin");
            }
        }

        @Nested
        class TestsForEqualCountEdgeCreation {

            @Test
            void testAllIdenticalValues() throws CanceledExecutionException {
                var data = createDataTable( //
                    new Column("col1", new double[]{1, 1, 1, 1}) //
                );

                var edges = AutoBinningUtils.createEdgesForEqualCount( //
                    data.get(), //
                    createExecutionContext(data), //
                    2, // number of bins
                    OptionalDouble.empty(), // no lower bound
                    OptionalDouble.empty(), // no upper bound
                    false // no integer forcing
                );

                assertEquals(3, edges.size(), "Expected 3 edges");
                assertEquals(List.of(1.0, 1.0, 1.0),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(0).exactMatchBehaviour(),
                    "First boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(1).exactMatchBehaviour(),
                    "Second boundary should match to lower bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(2).exactMatchBehaviour(),
                    "Last boundary should match to lower bin");
            }

            @Test
            void testLongStreak() throws CanceledExecutionException {
                var data = createDataTable( //
                    new Column("col1", new double[]{1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 4}) //
                );

                var edges = AutoBinningUtils.createEdgesForEqualCount( //
                    data.get(), //
                    createExecutionContext(data), //
                    10, // number of bins
                    OptionalDouble.empty(), // no lower bound
                    OptionalDouble.empty(), // no upper bound
                    false // no integer forcing
                );

                assertEquals(11, edges.size(), "Expected 11 edges");
                assertEquals(List.of(1, 1, 2, 3, 4, 4, 4, 4, 4, 4, 4).stream().map(Double::valueOf).toList(),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertTrue(edges.get(0).exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_UPPER_BIN,
                    "First boundary should match to upper bin");

                for (int i = 1; i < edges.size(); i++) {
                    assertTrue(edges.get(i).exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_LOWER_BIN,
                        "All non-first boundaries should match to lower bin");
                }
            }

            @Test
            void testBoundariesFallingOnExactValues() throws CanceledExecutionException {
                var data = createDataTable( //
                    new Column("col1", new double[]{1, 2, 3, 4}) //
                );

                var edges = AutoBinningUtils.createEdgesForEqualCount( //
                    data.get(), //
                    createExecutionContext(data), //
                    2, // number of bins
                    OptionalDouble.empty(), // no lower bound
                    OptionalDouble.empty(), // no upper bound
                    false // no integer forcing
                );

                assertEquals(3, edges.size(), "Expected 3 edges");
                assertEquals(List.of(1.0, 2.0, 4.0),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(0).exactMatchBehaviour(),
                    "First boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(1).exactMatchBehaviour(),
                    "Second boundary should match to lower bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(2).exactMatchBehaviour(),
                    "Last boundary should match to lower bin");
            }

            @Test
            void testNoBoundsAndNoIntegerForcing() throws CanceledExecutionException {
                var data = createDataTable( //
                    new Column("col1", new double[]{1, 2, 3, 4}) //
                );

                var edges = AutoBinningUtils.createEdgesForEqualCount( //
                    data.get(), //
                    createExecutionContext(data), //
                    2, // number of bins
                    OptionalDouble.empty(), // no lower bound
                    OptionalDouble.empty(), // no upper bound
                    false // no integer forcing
                );

                assertEquals(3, edges.size(), "Expected 3 edges");

                assertEquals(List.of(1.0, 2.0, 4.0),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(0).exactMatchBehaviour(),
                    "First boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(1).exactMatchBehaviour(),
                    "Second boundary should match to lower bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(2).exactMatchBehaviour(),
                    "Last boundary should match to lower bin");
            }

            @Test
            void testWithBounds() throws CanceledExecutionException {
                var data = createDataTable( //
                    new Column("col1", new double[]{0, 1, 2, 3, 4, 5}) //
                );

                var edges = AutoBinningUtils.createEdgesForEqualCount( //
                    data.get(), //
                    createExecutionContext(data), //
                    2, // number of bins
                    OptionalDouble.of(1.0), // lower bound
                    OptionalDouble.of(4.0), // upper bound
                    false // no integer forcing
                );

                assertEquals(3, edges.size(), "Expected 3 edges");
                assertEquals(List.of(1.0, 2.0, 4.0),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(0).exactMatchBehaviour(),
                    "First boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(1).exactMatchBehaviour(),
                    "Second boundary should match to lower bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(2).exactMatchBehaviour(),
                    "Last boundary should match to lower bin");
            }
        }

        @Nested
        class TestsForQuantileEdgeCreation {

            @Test
            void testCreateEdgesForCustomQuantiles() throws CanceledExecutionException {
                var data = createDataTable( //
                    new Column("col1", new double[]{10, 20, 30, 40, 50}) //
                );

                var edges = AutoBinningUtils.createEdgesFromQuantiles( //
                    data.get(), //
                    createExecutionContext(data), //
                    List.of( //
                        new BinBoundary(0.0, BinBoundaryExactMatchBehaviour.TO_UPPER_BIN), // 0% quantile
                        new BinBoundary(0.25, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN), // 25% quantile
                        new BinBoundary(0.5, BinBoundaryExactMatchBehaviour.TO_UPPER_BIN), // 50% quantile
                        new BinBoundary(0.75, BinBoundaryExactMatchBehaviour.TO_UPPER_BIN), // 75% quantile
                        new BinBoundary(1.0, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN) // 100% quantile
                    ) //
                );

                assertEquals(5, edges.size(), "Expected 5 edges");
                assertEquals(List.of(10.0, 20.0, 30.0, 40.0, 50.0),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(0).exactMatchBehaviour(),
                    "First boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(1).exactMatchBehaviour(),
                    "Second boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(2).exactMatchBehaviour(),
                    "Third boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(3).exactMatchBehaviour(),
                    "Fourth boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(4).exactMatchBehaviour(),
                    "Last boundary should match to lower bin");
            }
        }
    }

    static boolean dataCellsEqual(final DataCell d1, final DataCell d2) {
        if (d1 instanceof DoubleValue dv1 && d2 instanceof DoubleValue dv2) {
            return dv1.getDoubleValue() == dv2.getDoubleValue(); // NOSONAR we want exact equality
        } else {
            throw new IllegalArgumentException("DataCells must be DoubleValue instances for this comparison");
        }
    }

    static void assertDomainsEqual(final DataColumnDomain d1, final DataColumnDomain d2, final String message) {
        if (!d1.hasLowerBound() && !d2.hasLowerBound() && !d1.hasUpperBound() && !d2.hasUpperBound()) {
            return; // domains are same cuz neither are bounded
        } else if (d1.hasLowerBound() != d2.hasLowerBound() || d1.hasUpperBound() != d2.hasUpperBound()) {
            fail("Domains do not have the same bounds present: " + d1 + " vs " + d2 + "(" + message + ")");
        }

        if (d1.hasUpperBound() && d2.hasUpperBound() && !dataCellsEqual(d1.getUpperBound(), d2.getUpperBound())) {
            fail("Upper bounds are different: " + d1.getUpperBound() + " vs " + d2.getUpperBound() + " (" + message
                + ")");
        } else if (d1.hasLowerBound() && d2.hasLowerBound()
            && !dataCellsEqual(d1.getLowerBound(), d2.getLowerBound())) {
            fail("Lower bounds are different: " + d1.getLowerBound() + " vs " + d2.getLowerBound() + " (" + message
                + ")");
        }
    }

    static void assertDomainsNotEqual(final DataColumnDomain d1, final DataColumnDomain d2, final String message) {

        try {
            assertDomainsEqual(d1, d2, message);
            fail("Expected domains to be different but they were equal: " + d1 + " vs " + d2 + " (" + message + ")");
        } catch (AssertionError e) { // NOSONAR
            // expected, domains are not equal
        }
    }

    @Test
    void testRecalculateDomain() throws InvalidSettingsException, CanceledExecutionException {
        var dataWithoutDomain = createDataTable( //
            new Column("col1", new double[]{1, 2, 3, 4}), //
            new Column("col2", new double[]{10, 20, 30, 40}), //
            new Column("colWithNoDomain", new double[]{5, 6, 7, 8}) //
        );

        var exec = createExecutionContext(dataWithoutDomain);

        var oldSpec = dataWithoutDomain.get().getDataTableSpec();
        var newSpecCreator = new DataTableSpecCreator();

        var firstColumnSpecCreator = new DataColumnSpecCreator(oldSpec.getColumnSpec(0));
        firstColumnSpecCreator.setDomain(new DataColumnDomainCreator( //
            DoubleCellFactory.create(-10), //
            DoubleCellFactory.create(10) //
        ).createDomain());

        var secondColumnSpecCreator = new DataColumnSpecCreator(oldSpec.getColumnSpec(1));
        secondColumnSpecCreator.setDomain(new DataColumnDomainCreator( //
            DoubleCellFactory.create(5), //
            DoubleCellFactory.create(50) //
        ).createDomain());

        var newSpec = newSpecCreator.addColumns( //
            firstColumnSpecCreator.createSpec(), //
            secondColumnSpecCreator.createSpec(), //
            oldSpec.getColumnSpec(2) // the column without a domain
        ).createSpec();

        var newTableWithDomain = exec.createSpecReplacerTable( //
            dataWithoutDomain.get(), //
            newSpec //
        );

        var recalculatedDomains = AutoBinningUtils.calcDomainBoundsIfNeccessary( //
            newTableWithDomain, //
            exec, //
            List.of("col1") //
        ).getDataTableSpec().stream().map(DataColumnSpec::getDomain).toList();

        assertDomainsEqual(newSpec.getColumnSpec(0).getDomain(), recalculatedDomains.get(0),
            "First column domain should not have been recalculated since it had a domain");
        assertDomainsEqual(newSpec.getColumnSpec(1).getDomain(), recalculatedDomains.get(1),
            "Second column domain should not have been recalculated since it had a domain and was not specified");
        assertDomainsEqual(newSpec.getColumnSpec(2).getDomain(), recalculatedDomains.get(2),
            "Third column domain should not have been recalculated since it was not specified");

        // now try again but let's specify the 2nd and 3rd cols
        recalculatedDomains = AutoBinningUtils.calcDomainBoundsIfNeccessary( //
            newTableWithDomain, //
            exec, //
            List.of("col2", "colWithNoDomain") //
        ).getDataTableSpec().stream().map(DataColumnSpec::getDomain).toList();

        assertDomainsEqual(newSpec.getColumnSpec(0).getDomain(), recalculatedDomains.get(0),
            "First column domain should not have been recalculated since it had a domain and was not specified");
        assertDomainsEqual(newSpec.getColumnSpec(1).getDomain(), recalculatedDomains.get(1),
            "Second column domain should not have been recalculated since it had a domain");
        assertDomainsNotEqual(newSpec.getColumnSpec(2).getDomain(), recalculatedDomains.get(2),
            "Third column domain should have been recalculated since it had no domain and was specified");

        // and with an empty list of columns
        recalculatedDomains = AutoBinningUtils.calcDomainBoundsIfNeccessary( //
            newTableWithDomain, //
            exec, //
            List.of() // no columns specified
        ).getDataTableSpec().stream().map(DataColumnSpec::getDomain).toList();

        assertDomainsEqual(newSpec.getColumnSpec(0).getDomain(), recalculatedDomains.get(0),
            "First column domain should not have been recalculated since it had a domain and was not specified");
        assertDomainsEqual(newSpec.getColumnSpec(1).getDomain(), recalculatedDomains.get(1),
            "Second column domain should not have been recalculated since it had a domain and was not specified");
        assertDomainsEqual(newSpec.getColumnSpec(2).getDomain(), recalculatedDomains.get(2),
            "Third column domain should not have been recalculated since it had no domain and was not specified");

        // and with missing data
        var missingDataTable = new TableTestUtil.TableBuilder(dataWithoutDomain.get().getDataTableSpec()) //
            .addRow(new Object[]{new MissingCell(""), new DoubleCell(10.0), new MissingCell("")}) //
            .addRow(new Object[]{new MissingCell(""), new DoubleCell(20.0), new MissingCell("")}) //
            .addRow(new Object[]{new DoubleCell(3.0), new MissingCell(""), new MissingCell("")}) //
            .addRow(new Object[]{new MissingCell(""), new DoubleCell(40.0), new MissingCell("")}) //
            .build().get();

        // should not throw as both have at least one non-missing row
        AutoBinningUtils.calcDomainBoundsIfNeccessary( //
            missingDataTable, //
            exec, //
            List.of("col1", "col2") //
        ).getDataTableSpec().stream().map(DataColumnSpec::getDomain).toList();

        assertThrows(IllegalArgumentException.class, () -> {
            AutoBinningUtils.calcDomainBoundsIfNeccessary( //
                missingDataTable, //
                exec, //
                List.of("colWithNoDomain") // this column has no data, so should throw
            );
        }, "Should throw exception when trying to calculate domain for column with no data");

        // zero length table should return original table
        var zeroLengthTable = new TableTestUtil.TableBuilder(dataWithoutDomain.get().getDataTableSpec()).build().get();
        var zeroLengthResult = AutoBinningUtils.calcDomainBoundsIfNeccessary( //
            zeroLengthTable, //
            exec, //
            List.of("col1", "col2") //
        );
        assertEquals(zeroLengthTable, zeroLengthResult, "Should return original table spec for zero length table");

        // test fails on non-double data
        var nonDoubleDataSpec = new DataTableSpecCreator() //
            .addColumns(new DataColumnSpecCreator("col1", DoubleCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("col2", StringCell.TYPE).createSpec()) //
            .createSpec();
        var nonDoubleDataTable = new TableTestUtil.TableBuilder(nonDoubleDataSpec) //
            .addRow(new Object[]{new DoubleCell(1.0), new StringCell("test")}) //
            .addRow(new Object[]{new DoubleCell(2.0), new StringCell("test")}) //
            .build().get();

        // should be fine if we don't specify the string column
        AutoBinningUtils.calcDomainBoundsIfNeccessary( //
            nonDoubleDataTable, //
            exec, //
            List.of("col1") //
        );

        // but should throw if we do
        assertThrows(InvalidSettingsException.class, () -> {
            AutoBinningUtils.calcDomainBoundsIfNeccessary( //
                nonDoubleDataTable, //
                exec, //
                List.of("col1", "col2") // this column is not double
            );
        }, "Should throw exception when trying to calculate domain for non-double column");
    }

    static Supplier<BufferedDataTable> createDataTable(final Column... columns) {
        // first make sure all columns have the same length
        if (1 != Arrays.stream(columns).mapToInt(a -> a.values.length).distinct().count()) {
            throw new IllegalArgumentException("All columns must have the same length");
        }

        var specBuilder = new TableTestUtil.SpecBuilder();
        for (Column column : columns) {
            specBuilder.addColumn(column.name, DoubleCellFactory.TYPE);
        }
        var tableBuilder = new TableTestUtil.TableBuilder(specBuilder.build());

        for (int i = 0; i < columns[0].values.length; i++) {
            var rowData = new Object[columns.length];

            for (int j = 0; j < columns.length; j++) {
                rowData[j] = new DoubleCell(columns[j].values[i]);
            }

            tableBuilder.addRow(rowData);
        }

        var table = tableBuilder.build().get();

        return () -> table;
    }

    record Column(String name, double[] values) {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static ExecutionContext createExecutionContext(final Supplier<BufferedDataTable> table) {
        var monitor = new DefaultNodeProgressMonitor();
        NodeFactory<NodeModel> factory = (NodeFactory)new InputTableNode.InputDataNodeFactory(table);
        var node = new Node(factory);
        var memoryPolicy = SingleNodeContainer.MemoryPolicy.CacheInMemory;
        var thing = NotInWorkflowDataRepository.newInstance();

        return new ExecutionContext(monitor, node, memoryPolicy, thing);
    }

    @Nested
    class AutoBinnerTests {

        AutoBinningSettings m_settings;

        BufferedDataTable m_data;

        @BeforeEach
        void setup() {
            m_settings = new AutoBinningSettings( //
                List.of("col1", "col2"), //
                new BinningSettings.EqualWidth(3), //
                false, //
                new BinNamingSettings(BinNaming.NUMBERED, new ColumnFormat()), //
                new DataBoundsSettings(new BoundSetting.NoBound(""), new BoundSetting.NoBound("")), //
                new ColumnOutputNamingSettings.ReplaceColumn() //
            );

            var spec = new TableTestUtil.SpecBuilder() //
                .addColumn("col1", DoubleCell.TYPE) //
                .addColumn("col2", DoubleCell.TYPE) //
                .addColumn("colWithoutDomain", DoubleCell.TYPE) //
                .addColumn("missingColumn", DoubleCell.TYPE) //
                .addColumn("notDouble", StringCell.TYPE) //
                .build();

            m_data = new TableTestUtil.TableBuilder(spec) //
                .addRow(new Object[]{new DoubleCell(1.0), new DoubleCell(10.0), new DoubleCell(5.0),
                    new MissingCell(""), new StringCell("test")}) //
                .addRow(new Object[]{new DoubleCell(2.0), new DoubleCell(20.0), new DoubleCell(6.0),
                    new MissingCell(""), new StringCell("test")}) //
                .addRow(new Object[]{new DoubleCell(3.0), new DoubleCell(30.0), new MissingCell(""),
                    new MissingCell(""), new StringCell("test")}) //
                .addRow(new Object[]{new DoubleCell(4.0), new DoubleCell(40.0), new DoubleCell(8.0),
                    new MissingCell(""), new StringCell("test")}) //
                .buildDataTable();
        }

        @Test
        void testAppendReplaceIsRespected() throws InvalidSettingsException {
            var binner = new AutoBinningUtils.AutoBinner(m_settings, m_data.getSpec());
            var outSpec = (DataTableSpec)binner.createOutputSpec(m_data.getDataTableSpec())[0];

            assertArrayEquals(outSpec.getColumnNames(), m_data.getDataTableSpec().getColumnNames(),
                "Output spec should have same column names as input spec when replacing columns");

            var settingsWithAppend = new AutoBinningSettings( //
                List.of("col1", "col2"), //
                new BinningSettings.EqualWidth(3), //
                false, //
                new BinNamingSettings(BinNaming.NUMBERED, new ColumnFormat()), //
                new DataBoundsSettings(new BoundSetting.NoBound(""), new BoundSetting.NoBound("")), //
                new ColumnOutputNamingSettings.AppendSuffix("_beans") //
            );

            binner = new AutoBinningUtils.AutoBinner(settingsWithAppend, m_data.getSpec());

            outSpec = (DataTableSpec)binner.createOutputSpec(m_data.getDataTableSpec())[0];

            // should have the same columns as input spec but with "_beans" suffix on col1 and col2
            var expectedOutputCols =
                Arrays.copyOf(m_data.getSpec().getColumnNames(), m_data.getSpec().getNumColumns() + 2);
            expectedOutputCols[expectedOutputCols.length - 2] = "col1_beans";
            expectedOutputCols[expectedOutputCols.length - 1] = "col2_beans";

            assertArrayEquals(expectedOutputCols, outSpec.getColumnNames(),
                "Output spec should have same column names as input spec + suffix when appending columns "
                    + expectedOutputCols + " \n\n " + outSpec.getColumnNames());
        }

        @Test
        void testExtractAndSortSingleColumn() throws CanceledExecutionException {
            var data = createDataTable( //
                new Column("col1", new double[]{3.0, 2.0, 1.0, 4.0, 5.0}), //
                new Column("col2", new double[]{10.0, 20.0, 30.0, 40.0, 50.0}) //
            );
            var exec = createExecutionContext(data);

            var sortedSingleColumn = AutoBinningUtils.AutoBinner.extractAndSortSingleColumn(data.get(), exec, "col2");

            var extractedData = StreamSupport.stream(sortedSingleColumn.spliterator(), false) //
                .map(row -> row.getCell(0)) //
                .map(DoubleValue.class::cast) //
                .mapToDouble(DoubleValue::getDoubleValue) //;
                .toArray();

            // assert that the data is sorted and contains same values as col2
            var expectedData = new double[]{10.0, 20.0, 30.0, 40.0, 50.0};

            assertEquals(expectedData.length, extractedData.length, "Data length mismatch");
            for (int i = 0; i < expectedData.length; i++) {
                assertEquals(expectedData[i], extractedData[i], "Data value mismatch at index " + i);
            }
        }
    }
}
