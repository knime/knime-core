package org.knime.base.data.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.knime.base.data.statistics.calculation.DoubleMinMax;
import org.knime.base.data.statistics.calculation.Kurtosis;
import org.knime.base.data.statistics.calculation.Mean;
import org.knime.base.data.statistics.calculation.Median;
import org.knime.base.data.statistics.calculation.MinMax;
import org.knime.base.data.statistics.calculation.MissingValue;
import org.knime.base.data.statistics.calculation.Skewness;
import org.knime.base.data.statistics.calculation.SpecialDoubleCells;
import org.knime.base.data.statistics.calculation.StandardDeviation;
import org.knime.base.data.statistics.calculation.Variance;
import org.knime.base.node.preproc.sorter.SorterNodeFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.workflow.SingleNodeContainer;

/**
 * @author Marcel Hanser
 */
public class StatisticCalculatorTest {

    private static final String STRING_FEATURE = "StringFeature";

    private static final String FEATURE2 = "Feature2";

    private static final String FEATURE1 = "Feature1";

    private static ExecutionContext EXEC_CONTEXT;

    private BufferedDataTable testTable;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        EXEC_CONTEXT =
            new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(new SorterNodeFactory()),
                SingleNodeContainer.MemoryPolicy.CacheSmallInMemory, new HashMap<Integer, ContainerTable>());
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        DataColumnSpec[] colSpecs =
            new DataColumnSpec[]{new DataColumnSpecCreator(FEATURE1, DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator(FEATURE2, DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator(STRING_FEATURE, StringCell.TYPE).createSpec()};

        DataTableSpec spec = new DataTableSpec(colSpecs);
        final BufferedDataContainer container = EXEC_CONTEXT.createDataContainer(spec);

        int i = 0;
        //values second row : miss,1,1,1,3,4,5,8    mean: 3
        //values 3rd row : miss,miss,A,B,E,F,G,H,Z  mean: F
        container.addRowToTable(creatRow(i++, 1, 1d, "A"));
        container.addRowToTable(creatRow(i++, 1, 1d, "Z"));
        container.addRowToTable(creatRow(i++, 2, 3d, "B"));
        container.addRowToTable(creatRow(i++, 2, 5d, "G"));
        container.addRowToTable(creatRow(i++, 2, 1d, "E"));
        container.addRowToTable(creatRow(i++, 6, 4d, "F"));
        container.addRowToTable(creatRow(i++, 7, null, "H"));
        container.addRowToTable(creatRow(i++, 8, null, null));
        container.addRowToTable(creatRow(i++, 8, 8d, null));
        container.close();
        testTable = container.getTable();
    }

    private static DataRow creatRow(final int i, final double d, final Double e, final String string) {
        return new DefaultRow(RowKey.createRowKey(i), new DoubleCell(d), e == null ? DataType.getMissingCell()
            : new DoubleCell(e), string == null ? DataType.getMissingCell() : new StringCell(string));
    }

    /**
     * Tests common statistics.
     *
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     *
     */
    @Test
    public void testOrderingOfColumns() throws InvalidSettingsException, CanceledExecutionException {
        Mean mean = new Mean();
        StatisticCalculator statisticCalculator =
            new StatisticCalculator(testTable.getDataTableSpec(), new String[]{FEATURE2, FEATURE1}, mean);

        statisticCalculator.evaluate(testTable, EXEC_CONTEXT);

        assertEquals(4.1111, mean.getResult(FEATURE1), 0.0001);
        assertEquals(3.2857, mean.getResult(FEATURE2), 0.0001);

        Mean mean2 = new Mean();
        StatisticCalculator statisticCalculator2 =
            new StatisticCalculator(testTable.getDataTableSpec(), new String[]{FEATURE1, FEATURE2}, mean2);

        statisticCalculator2.evaluate(testTable, EXEC_CONTEXT);

        assertEquals(4.1111, mean2.getResult(FEATURE1), 0.0001);
        assertEquals(3.2857, mean2.getResult(FEATURE2), 0.0001);
    }

    /**
     * Tests common statistics.
     *
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     */
    @Test
    public void testGeneralStats() throws InvalidSettingsException, CanceledExecutionException {
        BufferedDataTable createRandomTable = createRandomTableWithMissingValues(20, 8);

        Skewness skewness = new Skewness();
        MinMax minMax = new MinMax();
        DoubleMinMax doubleMinMax = new DoubleMinMax(true);
        Kurtosis kurtosis = new Kurtosis();
        Mean mean = new Mean();
        Variance variance = new Variance();
        StandardDeviation sDev = new StandardDeviation();
        MissingValue missingValue = new MissingValue();
        SpecialDoubleCells sdc = new SpecialDoubleCells();

        StatisticCalculator statisticCalculator =
            new StatisticCalculator(createRandomTable.getDataTableSpec(), createRandomTable.getSpec().getColumnNames(),
                skewness, minMax, kurtosis, mean, variance, missingValue, sDev, doubleMinMax, sdc);

        statisticCalculator.evaluate(createRandomTable, EXEC_CONTEXT);

        Statistics3Table statistics3Table =
            new Statistics3Table(createRandomTable, false, 0, Collections.<String> emptyList(), EXEC_CONTEXT);

        for (int i = 0; i < createRandomTable.getDataTableSpec().getNumColumns(); i++) {
            String colName = "" + i;
            assertEquals(statistics3Table.getMean(i), mean.getResult(colName), 0.0001);
            assertEquals(statistics3Table.getKurtosis(i), kurtosis.getResult(colName), 0.0001);
            checkValueOrMissingIfNaN(statistics3Table.getMin()[i], minMax.getMin(colName));
            assertEquals(statistics3Table.getMinCells()[i], minMax.getMin(colName));
            checkValueOrMissingIfNaN(doubleMinMax.getMin(colName), statistics3Table.getNonInfMin(i) );
            checkValueOrMissingIfNaN(statistics3Table.getMax()[i], minMax.getMax(colName));
            assertEquals(statistics3Table.getMaxCells()[i], minMax.getMax(colName));
            checkValueOrMissingIfNaN(doubleMinMax.getMax(colName),statistics3Table.getNonInfMax(i) );
            assertEquals(statistics3Table.getVariance(i), variance.getResult(colName), 0.0001);
            assertEquals(statistics3Table.getStandardDeviation(i), sDev.getResult(colName), 0.0001);
            assertEquals(statistics3Table.getNumberMissingValues(i), missingValue.getNumberMissingValues(colName),
                0.0001);
            assertEquals(statistics3Table.getNumberNegativeInfiniteValues(i),
                sdc.getNumberNegativeInfiniteValues(colName), 0.0001);
            assertEquals(statistics3Table.getNumberPositiveInfiniteValues(i),
                sdc.getNumberPositiveInfiniteValues(colName), 0.0001);
            assertEquals(statistics3Table.getNumberNaNValues(i), sdc.getNumberNaNValues(colName), 0.0001);
        }
    }

    /**
     * Ensures that the given value is either identical or NaN and missing.
     * @param d the value to test against
     * @param dataCell the data cell to test
     */
    private void checkValueOrMissingIfNaN(final double d, final DataCell dataCell) {
        if (dataCell.isMissing()) {
            assertTrue(Double.isNaN(d));
        } else {
            assertEquals(d, ((DoubleValue)dataCell).getDoubleValue(), 0.0001);
        }
    }

    /**
     * Tests the median.
     *
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     */
    @Test
    public void testMedian() throws InvalidSettingsException, CanceledExecutionException {
        Median median = new Median(FEATURE1, FEATURE2, STRING_FEATURE);
        StatisticCalculator statisticCalculator = new StatisticCalculator(testTable.getDataTableSpec(), median);
        statisticCalculator.evaluate(testTable, EXEC_CONTEXT);
        System.out.println(median.getMedian(STRING_FEATURE));

        Statistics3Table stat3Table =
            new Statistics3Table(testTable, true, 0, Collections.<String> emptyList(), EXEC_CONTEXT,
                ascendingIntArray(testTable.getDataTableSpec().getNumColumns()));

        assertEquals(stat3Table.getMedian(0), Double.valueOf(median.getMedian(FEATURE1).toString()), 0.001);
        assertEquals(stat3Table.getMedian(1), Double.valueOf(median.getMedian(FEATURE2).toString()), 0.001);
        assertEquals("F", median.getMedian(STRING_FEATURE).toString());
    }

    /**
     * Tests the median with double cells.
     *
     * @throws Exception e
     */
    @Test
    public void doubleMedianTest() throws Exception {
        // create some random tables with random missing values
        for (int i = 0; i < 50; i++) {
            final BufferedDataTable table = createRandomTableWithMissingValues(4, 100);
            Statistics3Table statistics3Table =
                new Statistics3Table(table, true, 0, Collections.<String> emptyList(), EXEC_CONTEXT,
                    ascendingIntArray(4));

            Median median = new Median();
            StatisticCalculator statisticCalculator =
                new StatisticCalculator(table.getDataTableSpec(), table.getDataTableSpec().getColumnNames(), median);

            statisticCalculator.evaluate(table, EXEC_CONTEXT);

            for (int j = 0; j < 4; j++) {
                double oldMed = statistics3Table.getMedian(j);
                double newMed =
                    ((DoubleValue)median.getMedian(table.getDataTableSpec().getColumnSpec(j).getName()))
                        .getDoubleValue();

                assertEquals(oldMed, newMed, 0.00001);
            }
        }
    }

    /**
     * Test median performance.
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void medianPerformanceTest() throws Exception {
        int cols = 100;
        final BufferedDataTable table = createRandomTableWithMissingValues(cols, 500000);

        System.out.println("MEASSURING NOW: " + "Sorting");
        long t = System.currentTimeMillis();

        Median median = new Median();
        //                Mean mean = new Mean();
        StatisticCalculator statisticCalculator =
                new StatisticCalculator(table.getDataTableSpec(), table.getDataTableSpec().getColumnNames(), median);

        statisticCalculator.evaluate(table, EXEC_CONTEXT);
        System.out.println("Result: " + median.getMedian("0"));
        System.out.println("finished: " + (System.currentTimeMillis() - t) / 1000d + " sec");
    }

    private static BufferedDataTable createRandomTableWithMissingValues(final int cols, final int rows) {
        long currentTimeMillis = System.currentTimeMillis();
        System.out.println("Using seed: " + currentTimeMillis);
        Random random = new Random(currentTimeMillis);

        DataTableSpecCreator creator = new DataTableSpecCreator();

        for (int i = 0; i < cols; i++) {
            creator.addColumns(new DataColumnSpecCreator("" + i, DoubleCell.TYPE).createSpec());
        }

        final BufferedDataContainer container = EXEC_CONTEXT.createDataContainer(creator.createSpec());
        for (int i = 0; i < rows; i++) {
            DataCell[] rowVals = new DataCell[cols];
            for (int j = 0; j < cols; j++) {
                rowVals[j] =
                    random.nextDouble() > 0.66 ? new DoubleCell(random.nextDouble()) : DataType.getMissingCell();
            }
            container.addRowToTable(new DefaultRow(Integer.toString(i), rowVals));
            if (i % 1000 == 0) {
                System.out.println("Added row: " + i);
            }
        }
        container.close();
        return container.getTable();
    }

    /**
     * @param string
     * @param statisticCalculator
     * @throws Exception
     */
    private void doTimed(final String string, final Callable<?> call) throws Exception {
        System.out.println("MEASSURING NOW: " + string);
        long t = System.currentTimeMillis();

        call.call();

        System.out.println("finished: " + (System.currentTimeMillis() - t) / 1000d + " sec");
    }

    private static int[] ascendingIntArray(final int cols2) {
        int[] toReturn = new int[cols2];
        for (int i = 0; i < cols2; i++) {
            toReturn[i] = i;
        }
        return toReturn;
    }
}
