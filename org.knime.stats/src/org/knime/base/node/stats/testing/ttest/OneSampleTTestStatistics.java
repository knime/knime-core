/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   26.06.2012 (hofer): created
 */
package org.knime.base.node.stats.testing.ttest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.util.FastMath;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MutableInteger;
import org.knime.stats.StatsUtil;

/**
 * The independent samples tow tailed T-test. Test statistics are computed for
 * the assumption the variances are equal and for the case that this assumption
 * is not made.
 *
 * @author Heiko Hofer
 */
public class OneSampleTTestStatistics {

    public static final String T_VALUE = "t";
    public static final String DEGREES_OF_FREEDOM = "df";
    public static final String P_VALUE = "p-value (2-tailed)";

    public static final String COLUMN = "Column";
    public static final String N = "N";
    public static final String MISSING_COUNT = "Missing Count";
    public static final String LABEL = "Label";
    public static final String MEAN = "Mean";
    public static final String MEAN_DIFFERENCE = "Mean Difference";
    public static final String STANDARD_DEVIATION = "Standard Deviation";
    public static final String TEST_VALUE = "Test Value";

    public static final String STANDARD_ERROR_MEAN =
        "Standard Error Mean";
    public static final String CONFIDENCE_INTERVAL_PROBABILITY =
        "Confidence Interval Probability";
    public static final String CONFIDENCE_INTERVAL_LOWER_BOUND =
        "Confidence Interval of the Difference (Lower Bound)";
    public static final String CONFIDENCE_INTERVAL_UPPER_BOUND =
        "Confidence Interval of the Difference (Upper Bound)";

    /** the test column. */
    private String m_column;
    /** the test value. */
    private double m_testValue;

    /** The alpha used for the confidence interval. */
    private double m_confidenceIntervalProp;

    /** summary statistics for the test column */
    private SummaryStatistics m_stats;
    /** summary statistics for the (test column - m_testValue) */
    private SummaryStatistics m_statsDiff;

    /** the number of rows with missing values */
    private MutableInteger m_missing;

    /** Dummy object to access protected methods of {@link TTest} */
    private KnimeTTest m_tTest;

    /**
     * @param column the column
     * @param testValue the test value
     * @param confidenceIntervalProb the probability used to compute
     * confidence intervals (Typically 0.95)
     */
    public OneSampleTTestStatistics(final String column,
            final double testValue,
            final double confidenceIntervalProb) {
        super();
        m_column = column;
        m_testValue = testValue;
        m_confidenceIntervalProp = confidenceIntervalProb;

        m_tTest = new KnimeTTest();
        m_missing = new MutableInteger(0);

        m_stats = new SummaryStatistics();
        m_statsDiff = new SummaryStatistics();
    }

    /**
     * Add a value from the data.
     *
     * @param value the value of the test column
     */
    public void addValue(final double value) {
        m_stats.addValue(value);
        m_statsDiff.addValue(value - m_testValue);
    }

    /**
     * Increment missing value counter
     */
    public void addMissing() {
        m_missing.add(1);
    }

    /**
     * Get the spec of the group statistics table.
     * @return the spec of the group statistics table
     */
    public static DataTableSpec getDescStatsSpec() {
        return new DataTableSpec(new String[] {
                COLUMN
                , N
                , MISSING_COUNT
                , MEAN
                , STANDARD_DEVIATION
                , STANDARD_ERROR_MEAN},
                new DataType[] {
                StringCell.TYPE
                , IntCell.TYPE
                , IntCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
        });
    }

    /**
     * Get descriptive statistics
     * @param exec
     * @return the descriptive statistics for each column test column
     */
    public BufferedDataTable getDescStatsTable(final ExecutionContext exec) {
        DataTableSpec outSpec = getDescStatsSpec();
        BufferedDataContainer cont = exec.createDataContainer(outSpec);

        int r = 0;
        for (List<DataCell> cells : getDescStatsCells()) {
            cont.addRowToTable(new DefaultRow(RowKey.createRowKey(r), cells));
            r++;
        }

        cont.close();
        BufferedDataTable outTable = cont.getTable();
        return outTable;
    }

    /**
     * Get descriptive statistics. The cells of the table that is returned by
     * getDescStatsTable(exec).
     * @return the descriptive statistics for each column test column
     */
    public List<List<DataCell>> getDescStatsCells() {
        List<List<DataCell>> cells = new ArrayList<List<DataCell>>();
        cells.add(getDescStats(m_column, m_stats, m_missing));

        return cells;
    }

    /**
     * Get descriptive statistics for the given column
     * @param rowID the row key
     * @param column the name of the column
     * @param stats the statistics of the column
     * @param missing the missing values in this column
     * @return a DataRow with descriptive statistics
     */
    private List<DataCell> getDescStats(final String column,
            final SummaryStatistics stats, final MutableInteger missing) {
        List<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell(column));

        cells.add(new IntCell((int)stats.getN()));
        cells.add(new IntCell(missing.intValue()));
        cells.add(new DoubleCell(stats.getMean()));
        cells.add(new DoubleCell(stats.getStandardDeviation()));
        cells.add(new DoubleCell(StatsUtil.getStandardError(stats)));
        return cells;
    }

    /**
     * Get the spec of the group statistics table.
     * @return the spec of the group statistics table
     */
    public static DataTableSpec getTableSpec() {
        return new DataTableSpec(new String[] {
                LABEL
                , TEST_VALUE
                , T_VALUE
                , DEGREES_OF_FREEDOM
                , P_VALUE
                , MEAN_DIFFERENCE
                , CONFIDENCE_INTERVAL_PROBABILITY
                , CONFIDENCE_INTERVAL_LOWER_BOUND
                , CONFIDENCE_INTERVAL_UPPER_BOUND
                },
                new DataType[] {
                StringCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
        });
    }

    /**
     * Get the test result of the t-test, for the assumption of equal
     * variance and the assumption of unequal variances.
     * @param exec the execution context
     * @return the t-test results
     */
    public BufferedDataTable getTTestTable(final ExecutionContext exec) {
        DataTableSpec outSpec = getTableSpec();
        BufferedDataContainer cont = exec.createDataContainer(outSpec);

        int r = 0;
        for (List<DataCell> cells : getTTestCells()) {
            cont.addRowToTable(new DefaultRow(RowKey.createRowKey(r), cells));
            r++;
        }

        cont.close();
        BufferedDataTable outTable = cont.getTable();
        return outTable;
    }

    /**
     * Get the test result of the t-test.
     * @return the t-test results
     */
    public List<List<DataCell>> getTTestCells() {
        List<List<DataCell>> cells = new ArrayList<List<DataCell>>();
        cells.add(m_tTest.getOneSampleTTest());

        return cells;
    }


    /** A wrapper to the TTest class to access protected methods. */
    private class KnimeTTest extends TTest {

        /**
         * Returns the  one-sample, two-tailed t-test
         * @return the test results
         */
        public List<DataCell> getOneSampleTTest() {
            double meanDiff = m_statsDiff.getMean();
            double stdDeviation = m_statsDiff.getStandardDeviation();
            double stdErrorMean =
                stdDeviation / FastMath.sqrt(m_statsDiff.getN());
            double df = m_statsDiff.getN() - 1;
            TDistribution distribution = new TDistribution(df);
            double tValue = FastMath.abs(
                    distribution.inverseCumulativeProbability(
                            (1 - m_confidenceIntervalProp) / 2));
            double confidenceDelta = tValue * stdErrorMean;
            double confidenceLowerBound = meanDiff - confidenceDelta;
            double confidenceUpperBound = meanDiff + confidenceDelta;

            double t = meanDiff / stdErrorMean;
            double pValue = 2.0 * distribution.cumulativeProbability(
                    -FastMath.abs(t));

            List<DataCell> cells = new ArrayList<DataCell>();
            cells.add(new StringCell(m_column));
            cells.add(new DoubleCell(m_testValue));
            cells.add(new DoubleCell(t));
            cells.add(new DoubleCell(df));
            cells.add(new DoubleCell(pValue));
            cells.add(new DoubleCell(meanDiff));
            cells.add(new DoubleCell(m_confidenceIntervalProp));
            cells.add(new DoubleCell(confidenceLowerBound));
            cells.add(new DoubleCell(confidenceUpperBound));
            return cells;
        }
    }
}
