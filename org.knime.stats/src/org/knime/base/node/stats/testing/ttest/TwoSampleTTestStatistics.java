/*
 * ------------------------------------------------------------------------
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   26.06.2012 (hofer): created
 */
package org.knime.base.node.stats.testing.ttest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.util.FastMath;
import org.knime.base.node.stats.testing.ttest.Grouping.Group;
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
public class TwoSampleTTestStatistics {
    public static final String VARIANCE_ASSUMPTION = "Variance Assumption";
    public static final StringCell EQUAL_VARIANCES_ASSUMED =
        new StringCell("Equal variances assumed");
    public static final StringCell EQUAL_VARIANCES_NOT_ASSUMED =
        new StringCell("Equal variances not assumed");
    public static final String T_VALUE = "t";
    public static final String DEGREES_OF_FREEDOM = "df";
    public static final String P_VALUE = "p-value (2-tailed)";

    public static final String TEST_COLUMN = "Test Column";
    public static final String GROUP = "Group";
    public static final String N = "N";
    public static final String MISSING_COUNT = "Missing Count";
    public static final String MISSING_COUNT_GROUP_COL =
        "Missing Count (Group Column)";
    public static final String IGNORED_COUNT_GROUP_COL =
        "Ignored Count (Group Column)";
    public static final String MEAN = "Mean";
    public static final String STANDARD_DEVIATION = "Standard Deviation";
    public static final String STANDARD_ERROR = "Standard Error Mean";

    public static final String MEAN_DIFFERENCE = "Mean Difference";
    public static final String STANDARD_ERROR_DIFFERENCE =
        "Standard Error Difference";
    public static final String CONFIDENCE_INTERVAL_PROBABILITY =
        "Confidence Interval Probability";
    public static final String CONFIDENCE_INTERVAL_LOWER_BOUND =
        "Confidence Interval of the Difference (Lower Bound)";
    public static final String CONFIDENCE_INTERVAL_UPPER_BOUND =
        "Confidence Interval of the Difference (Upper Bound)";

    /** the test column. */
    private String m_column;
    /** The alpha used for the confidence interval. */
    private double m_confidenceIntervalProp;
    /** the group identifiers. */
    private Map<Group, String> m_groups;
    /** summary statistics per group. */
    private Map<Group, SummaryStatistics> m_gstats;
    /** summary statistics across groups. */
    private SummaryStatistics m_stats;
    /** number of missing values per group. */
    private Map<Group, MutableInteger> m_missing;
    /** number of missing values in the grouping column. */
    private MutableInteger m_missingGroup;
    /** Dummy object to access protected methods of {@link TTest}. */
    private KnimeTTest m_tTest;
    /** Number of ignored cells in the group column. Ignored are all values
     * which are not in m_groups.
     */
    private MutableInteger m_ignoredGroup;

    /**
     * @param column the test column
     * @param groups the group identifiers
     * @param confindeceIntervalProb the probability used to compute
     * confidence intervals (Typically 0.95)
     */
    public TwoSampleTTestStatistics(final String column,
            final Map<Group, String> groups,
            final double confindeceIntervalProb) {
        super();
        m_column = column;
        m_confidenceIntervalProp = confindeceIntervalProb;
        m_groups = groups;
        m_tTest = new KnimeTTest();
        m_missingGroup = new MutableInteger(0);
        m_ignoredGroup = new MutableInteger(0);

        m_gstats = new LinkedHashMap<Group, SummaryStatistics>();
        m_gstats.put(Group.GroupX, new SummaryStatistics());
        m_gstats.put(Group.GroupY, new SummaryStatistics());
        m_stats = new SummaryStatistics();
        m_missing = new LinkedHashMap<Group, MutableInteger>();
        m_missing.put(Group.GroupX, new MutableInteger(0));
        m_missing.put(Group.GroupY, new MutableInteger(0));
    }

    /**
     * Add value to the test.
     * @param value the value
     * @param group the group of the value
     */
    public void addValue(final double value, final Group group) {
        m_gstats.get(group).addValue(value);
        m_stats.addValue(value);
    }

    /**
     * Notify about a missing value in the given group.
     * @param group the group
     */
    public void addMissing(final Group group) {
        m_missing.get(group).add(1);
    }

    /**
     * Add one to the counter for missing cells in the grouping column.
     */
    public void addMissingGroup() {
        m_missingGroup.add(1);
    }

    /**
     * Add one to the counter for ignored cells in the grouping column.
     */
    public void addIgnoredGroup() {
        m_ignoredGroup.add(1);
    }

    /**
     * Get the spec of the group statistics table.
     * @return the spec of the group statistics table
     */
    public static DataTableSpec getGroupStatisticsSpec() {
        return new DataTableSpec(new String[] {
                TEST_COLUMN
                , GROUP
                , N
                , MISSING_COUNT
                , MISSING_COUNT_GROUP_COL
                , IGNORED_COUNT_GROUP_COL
                , MEAN
                , STANDARD_DEVIATION
                , STANDARD_ERROR},
                new DataType[] {
                StringCell.TYPE
                , StringCell.TYPE
                , IntCell.TYPE
                , IntCell.TYPE
                , IntCell.TYPE
                , IntCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
                , DoubleCell.TYPE
        });
    }

    /**
     * Get descriptive statistics.
     * @param exec the execution context
     * @return the descriptive statistics for each group
     */
    public BufferedDataTable getGroupTable(final ExecutionContext exec) {
        DataTableSpec outSpec = getGroupStatisticsSpec();
        BufferedDataContainer cont = exec.createDataContainer(outSpec);

        int r = 0;
        for (List<DataCell> cells : getGroupStatisticsCells()) {
            cont.addRowToTable(new DefaultRow(RowKey.createRowKey(r), cells));
            r++;
        }

        cont.close();
        BufferedDataTable outTable = cont.getTable();
        return outTable;
    }

    /**
     * Get descriptive statistics. The cells of the table that is returned by
     * getGroupTable(exec).
     * @return the descriptive statistics for each group
     */
    public List<List<DataCell>> getGroupStatisticsCells() {
        List<List<DataCell>> cells = new ArrayList<List<DataCell>>();

        for (Group group : m_groups.keySet()) {
            cells.add(getGroupStatistics(group));
        }

        return cells;
    }

    /**
     * Get descriptive statistics for the given Group.
     * @param group the group
     * @return the
     */
    public List<DataCell> getGroupStatistics(final Group group) {
        List<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell(m_column));
        cells.add(new StringCell(m_groups.get(group)));
        SummaryStatistics stats = m_gstats.get(group);
        cells.add(new IntCell((int)stats.getN()));
        cells.add(new IntCell(m_missing.get(group).intValue()));
        cells.add(new IntCell(m_missingGroup.intValue()));
        cells.add(new IntCell(m_ignoredGroup.intValue()));
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
                TEST_COLUMN
                , VARIANCE_ASSUMPTION
                , T_VALUE
                , DEGREES_OF_FREEDOM
                , P_VALUE
                , MEAN_DIFFERENCE
                , STANDARD_ERROR_DIFFERENCE
                , CONFIDENCE_INTERVAL_PROBABILITY
                , CONFIDENCE_INTERVAL_LOWER_BOUND
                , CONFIDENCE_INTERVAL_UPPER_BOUND
                },
                new DataType[] {
                StringCell.TYPE
                , StringCell.TYPE
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
        cells.add(m_tTest.getEqualVariancesAssumedTTest());
        cells.add(m_tTest.getEqualVariancesNotAssumedTTest());

        return cells;
    }

    /** A wrapper to the TTest class to access protected methods. */
    private class KnimeTTest extends TTest {

        /**
         * Computes p-value for 2-sided, 2-sample t-test.
         * Does not assume that subpopulation variances are equal.
         *
         * @return the row with t-Test statistics
         */
        private List<DataCell> getEqualVariancesNotAssumedTTest() {

            SummaryStatistics statsX = m_gstats.get(Group.GroupX);
            SummaryStatistics statsY = m_gstats.get(Group.GroupY);

            // first sample mean
            double m1 = statsX.getMean();
            // second sample mean
            double m2 = statsY.getMean();
            // first sample variance
            double v1 = statsX.getVariance();
            // second sample variance
            double v2 = statsY.getVariance();
            // first sample count
            double n1 = statsX.getN();
            // second sample count
            double n2 = statsY.getN();
            // the t-test statistic =
            //            (difference in means) / standard error (difference):
            double t = t(m1, m2, v1, v2, n1, n2);
            // approximate degrees of freedom for 2-sample t-test
            double df = df(v1, v2, n1, n2);
            TDistribution distribution = new TDistribution(df);
            double pValue = 2.0
                * distribution.cumulativeProbability(-FastMath.abs(t));

            double meanDifference = m1 - m2;

            double standardErrorDiff = FastMath.sqrt((v1 / n1) + (v2 / n2));
            double tValue = FastMath.abs(
                    distribution.inverseCumulativeProbability(
                            (1 - m_confidenceIntervalProp) / 2));
            double confidenceDelta = tValue * standardErrorDiff;
            double confidenceLowerBound = meanDifference - confidenceDelta;
            double confidenceUpperBound = meanDifference + confidenceDelta;

            List<DataCell> cells = new ArrayList<DataCell>();
            cells.add(new StringCell(m_column));
            cells.add(EQUAL_VARIANCES_NOT_ASSUMED);
            cells.add(new DoubleCell(t));
            cells.add(new DoubleCell(df));
            cells.add(new DoubleCell(pValue));
            cells.add(new DoubleCell(meanDifference));
            cells.add(new DoubleCell(standardErrorDiff));
            cells.add(new DoubleCell(m_confidenceIntervalProp));
            cells.add(new DoubleCell(confidenceLowerBound));
            cells.add(new DoubleCell(confidenceUpperBound));
            return cells;
        }

        /**
         * Computes p-value for 2-sided, 2-sample t-test, under the assumption
         * of equal subpopulation variances.
         * The sum of the sample sizes minus 2 is used as degrees of freedom.
         *
         * @return the row with t-Test statistics
         */
        private List<DataCell> getEqualVariancesAssumedTTest() {

            SummaryStatistics statsX = m_gstats.get(Group.GroupX);
            SummaryStatistics statsY = m_gstats.get(Group.GroupY);

            // first sample mean
            double m1 = statsX.getMean();
            // second sample mean
            double m2 = statsY.getMean();
            // first sample variance
            double v1 = statsX.getVariance();
            // second sample variance
            double v2 = statsY.getVariance();
            // first sample count
            double n1 = statsX.getN();
            // second sample count
            double n2 = statsY.getN();
            // the t-test statistic =
            //            (difference in means) / standard error (difference):
            double t = homoscedasticT(m1, m2, v1, v2, n1, n2);
            double df = n1 + n2 - 2;
            TDistribution distribution = new TDistribution(df);
            double pValue = 2.0
                * distribution.cumulativeProbability(-FastMath.abs(t));

            double meanDifference = m1 - m2;

            double pooledVariance =
                ((n1  - 1) * v1 + (n2 - 1) * v2) / (n1 + n2 - 2);
            double standardErrorDiff =
                FastMath.sqrt(pooledVariance * (1d / n1 + 1d / n2));
            double tValue = FastMath.abs(
                    distribution.inverseCumulativeProbability(
                            (1 - m_confidenceIntervalProp) / 2));
            double confidenceDelta = tValue * standardErrorDiff;
            double confidenceLowerBound = meanDifference - confidenceDelta;
            double confidenceUpperBound = meanDifference + confidenceDelta;

            List<DataCell> cells = new ArrayList<DataCell>();
            cells.add(new StringCell(m_column));
            cells.add(EQUAL_VARIANCES_ASSUMED);
            cells.add(new DoubleCell(t));
            cells.add(new DoubleCell(df));
            cells.add(new DoubleCell(pValue));
            cells.add(new DoubleCell(meanDifference));
            cells.add(new DoubleCell(standardErrorDiff));
            cells.add(new DoubleCell(m_confidenceIntervalProp));
            cells.add(new DoubleCell(confidenceLowerBound));
            cells.add(new DoubleCell(confidenceUpperBound));
            return cells;
        }

    }

    /**
     * Get summary statistics per group.
     * @return the summary statistics
     */
    public Map<Group, SummaryStatistics> getGroupSummaryStatistics() {
        return m_gstats;
    }

    /**
     * Get summary statistics across groups.
     * @return the summary statistics
     */
    public SummaryStatistics getSummaryStatistics() {
        return m_stats;
    }
}
