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
 * ------------------------------------------------------------------------
 *
 * History
 *   26.06.2012 (hofer): created
 */
package org.knime.base.node.stats.testing.anova;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.FastMath;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
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
 * The independent samples tow tailed T-test. Test statistics are computed for the assumption the variances are equal
 * and for the case that this assumption is not made.
 *
 * @author Heiko Hofer
 */
public class OneWayANOVAStatistics {

    public static final String F_VALUE = "F";
    public static final String DEGREES_OF_FREEDOM = "df";
    public static final String P_VALUE = "p-value";

    public static final String TEST_COLUMN = "Test Column";
    public static final String GROUP = "Group";
    public static final String N = "N";
    public static final String MISSING_COUNT = "Missing Count";

    public static final String MISSING_COUNT_GROUP_COL = "Missing Count (Group Column)";
    public static final String MEAN = "Mean";
    public static final String STANDARD_DEVIATION = "Standard Deviation";
    public static final String STANDARD_ERROR = "Standard Error Mean";

    public static final String CONFIDENCE_INTERVAL_PROBABILITY = "Confidence Interval Probability";

    public static final String CONFIDENCE_INTERVAL_LOWER_BOUND = "Confidence Interval of the Difference (Lower Bound)";

    public static final String CONFIDENCE_INTERVAL_UPPER_BOUND = "Confidence Interval of the Difference (Upper Bound)";
    public static final String MINIMUM = "Minimum";
    public static final String MAXIMUM = "Maximum";

    public static final String SUM_OF_SQUARES = "Sum of Squares";
    public static final String MEAN_SQUARE = "Mean Square";

    public static final String SOURCE = "Source";
    public static final String SOURCE_BETWEEN_GROUPS = "Between Groups";
    public static final String SOURCE_WITHIN_GROUPS = "Within Groups";

    public static final String SOURCE_TOTAL = "Total";


    /** the test column. */
    private final String m_column;
    /** The alpha used for the confidence interval. */
    private final double m_confidenceIntervalProp;
    /** the group identifiers. */
    private final List<String> m_groups;
    /** summary statistics across groups. */
    private final SummaryStatistics m_stats;
    /** summary statistics per group. */
    private final List<SummaryStatistics> m_gstats;
    /** number of missing values per group. */
    private final List<MutableInteger> m_missing;
    /** number of missing values in the grouping column */
    private final MutableInteger m_missingGroup;

    /**
     * @param column the test column
     * @param groups the group identifiers
     * @param confindeceIntervalProb the probability used to compute confidence intervals (Typically 0.95)
     */
    public OneWayANOVAStatistics(final String column, final List<String> groups, final double confindeceIntervalProb) {
        super();
        m_column = column;
        m_confidenceIntervalProp = confindeceIntervalProb;
        m_groups = groups;
        m_missingGroup = new MutableInteger(0);

        m_stats = new SummaryStatistics();
        final int numGroups = groups.size();
        m_gstats = new ArrayList<SummaryStatistics>(numGroups);
        for (int i = 0; i < numGroups; i++) {
            m_gstats.add(new SummaryStatistics());
        }

        m_missing = new ArrayList<MutableInteger>(numGroups);
        for (int i = 0; i < numGroups; i++) {
            m_missing.add(new MutableInteger(0));
        }
    }

    /**
     * Add value to the test.
     * @param value the value
     * @param gIndex the group of the value
     */
    public void addValue(final double value, final int gIndex) {
        m_gstats.get(gIndex).addValue(value);
        m_stats.addValue(value);
    }

    /**
     * Notify about a missing value in the given group.
     * @param gIndex the group
     */
    public void addMissing(final int gIndex) {
        m_missing.get(gIndex).add(1);
    }

    /**
     * Add one to the counter for missing cells in the grouping column.
     */
    public void addMissingGroup() {
        m_missingGroup.add(1);
    }

    /**
     * Get the spec of the group statistics table.
     * @return the spec of the group statistics table
     */
    public static DataTableSpec getGroupStatisticsSpec() {
        return new DataTableSpec(
            new String[]{TEST_COLUMN, GROUP, N, MISSING_COUNT, MISSING_COUNT_GROUP_COL, MEAN, STANDARD_DEVIATION,
                STANDARD_ERROR, CONFIDENCE_INTERVAL_PROBABILITY, CONFIDENCE_INTERVAL_LOWER_BOUND,
                CONFIDENCE_INTERVAL_UPPER_BOUND, MINIMUM, MAXIMUM},
            new DataType[]{StringCell.TYPE, StringCell.TYPE, IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, DoubleCell.TYPE,
                DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE,
                DoubleCell.TYPE});
    }

    /**
     * Get descriptive statistics
     * @param exec
     * @return the descriptive statistics for each group
     */
    public BufferedDataTable getGroupTable(final ExecutionContext exec) {
        final DataTableSpec outSpec = getGroupStatisticsSpec();
        final BufferedDataContainer cont = exec.createDataContainer(outSpec);

        int r = 0;
        for (final List<DataCell> cells : getGroupStatisticsCells()) {
            cont.addRowToTable(new DefaultRow(RowKey.createRowKey(r), cells));
            r++;
        }

        cont.close();
        final BufferedDataTable outTable = cont.getTable();
        return outTable;
    }

    /**
     * Get descriptive statistics. The cells of the table that is returned by getGroupTable(exec).
     * 
     * @return the descriptive statistics for each group
     */
    public List<List<DataCell>> getGroupStatisticsCells() {
        final List<List<DataCell>> cells = new ArrayList<List<DataCell>>();

        for (int i = 0; i < m_groups.size(); i++) {
            cells.add(getGroupStatistics(i));
        }
        cells.add(getGroupsTotalStatistics());
        return cells;
    }

    /**
     * Get descriptive statistics for the given Group.
     * @param groupIndex the index of the group
     * @return the descriptive statistics for this group.
     */
    public List<DataCell> getGroupStatistics(final int groupIndex) {
        final List<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell(m_column));
        cells.add(new StringCell(m_groups.get(groupIndex)));
        final SummaryStatistics stats = m_gstats.get(groupIndex);
        cells.add(new IntCell((int)stats.getN()));
        cells.add(new IntCell(m_missing.get(groupIndex).intValue()));
        cells.add(new IntCell(m_missingGroup.intValue()));
        cells.add(new DoubleCell(stats.getMean()));
        cells.add(new DoubleCell(stats.getStandardDeviation()));
        cells.add(new DoubleCell(StatsUtil.getStandardError(stats)));
        cells.add(new DoubleCell(m_confidenceIntervalProp));

        final long df = stats.getN() - 1;
        final TDistribution distribution = new TDistribution(df);
        final double tValue =
            FastMath.abs(distribution.inverseCumulativeProbability((1 - m_confidenceIntervalProp) / 2));
        final double confidenceDelta = tValue * StatsUtil.getStandardError(stats);
        final double confidenceLowerBound = stats.getMean() - confidenceDelta;
        final double confidenceUpperBound = stats.getMean() + confidenceDelta;


        cells.add(new DoubleCell(confidenceLowerBound));
        cells.add(new DoubleCell(confidenceUpperBound));

        cells.add(new DoubleCell(stats.getMin()));
        cells.add(new DoubleCell(stats.getMax()));
        return cells;
    }

    /**
     * Get descriptive statistics for all groups.
     * @return the descriptive statistics for all groups
     */
    public List<DataCell> getGroupsTotalStatistics() {
        final List<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell(m_column));
        cells.add(new StringCell("Total"));
        final SummaryStatistics stats = m_stats;
        cells.add(new IntCell((int)stats.getN()));
        int missingCount = 0;
        for (final MutableInteger m : m_missing) {
            missingCount += m.intValue();
        }
        cells.add(new IntCell(missingCount));
        cells.add(new IntCell(m_missingGroup.intValue()));
        cells.add(new DoubleCell(stats.getMean()));
        cells.add(new DoubleCell(stats.getStandardDeviation()));
        cells.add(new DoubleCell(StatsUtil.getStandardError(stats)));
        cells.add(new DoubleCell(m_confidenceIntervalProp));

        final long df = stats.getN() - 1;
        final TDistribution distribution = new TDistribution(df);
        final double tValue =
            FastMath.abs(distribution.inverseCumulativeProbability((1 - m_confidenceIntervalProp) / 2));
        final double confidenceDelta = tValue * StatsUtil.getStandardError(stats);
        final double confidenceLowerBound = stats.getMean() - confidenceDelta;
        final double confidenceUpperBound = stats.getMean() + confidenceDelta;


        cells.add(new DoubleCell(confidenceLowerBound));
        cells.add(new DoubleCell(confidenceUpperBound));

        cells.add(new DoubleCell(stats.getMin()));
        cells.add(new DoubleCell(stats.getMax()));
        return cells;
    }


    /**
     * Get the spec of the group statistics table.
     * @return the spec of the group statistics table
     */
    public static DataTableSpec getTableSpec() {
        final List<DataColumnSpec> allColSpecs = new ArrayList<>(7);
        allColSpecs.add(new DataColumnSpecCreator(TEST_COLUMN, StringCell.TYPE).createSpec());
        allColSpecs.add(new DataColumnSpecCreator(SOURCE, StringCell.TYPE).createSpec());
        allColSpecs.add(new DataColumnSpecCreator(SUM_OF_SQUARES, DoubleCell.TYPE).createSpec());
        allColSpecs.add(new DataColumnSpecCreator(DEGREES_OF_FREEDOM, IntCell.TYPE).createSpec());
        allColSpecs.add(new DataColumnSpecCreator(MEAN_SQUARE, DoubleCell.TYPE).createSpec());
        allColSpecs.add(new DataColumnSpecCreator(F_VALUE, DoubleCell.TYPE).createSpec());
        allColSpecs.add(StatsUtil.createDataColumnSpec(P_VALUE, StatsUtil.FULL_PRECISION_RENDERER, DoubleCell.TYPE));

        return new DataTableSpec(allColSpecs.toArray(new DataColumnSpec[0]));
    }

    /**
     * Get the test result of the t-test, for the assumption of equal variance and the assumption of unequal variances.
     * 
     * @param exec the execution context
     * @return the t-test results
     */
    public BufferedDataTable getTTestTable(final ExecutionContext exec) {
        final DataTableSpec outSpec = getTableSpec();
        final BufferedDataContainer cont = exec.createDataContainer(outSpec);

        int r = 0;
        for (final List<DataCell> cells : getTTestCells()) {
            cont.addRowToTable(new DefaultRow(RowKey.createRowKey(r), cells));
            r++;
        }

        cont.close();
        final BufferedDataTable outTable = cont.getTable();
        return outTable;
    }


    /**
     * Get the test result of the t-test.
     * @return the t-test results
     */
    public List<List<DataCell>> getTTestCells() {
        final List<List<DataCell>> cells = new ArrayList<List<DataCell>>();

        final ANOVA anova = new ANOVA();
        cells.add(getBetweenGroups(anova));
        cells.add(getWithinGroups(anova));
        cells.add(getTotal(anova));
        return cells;
    }

    /**
     * Get the row of the ANOVA table with the cells "Between Groups".
     */
    private List<DataCell> getBetweenGroups(final ANOVA anova) {
        final List<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell(m_column));
        cells.add(new StringCell(SOURCE_BETWEEN_GROUPS));
        cells.add(new DoubleCell(anova.getSqurb()));
        cells.add(new IntCell((int)anova.getDfb()));
        cells.add(new DoubleCell(anova.getMsqurb()));
        cells.add(new DoubleCell(anova.getF()));
        cells.add(new DoubleCell(anova.getpValue()));
        return cells;
    }

    /**
     * Get the row of the ANOVA table with the cells "Within Groups".
     */
    private List<DataCell> getWithinGroups(final ANOVA anova) {
        final List<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell(m_column));
        cells.add(new StringCell(SOURCE_WITHIN_GROUPS));
        cells.add(new DoubleCell(anova.getSquri()));
        cells.add(new IntCell((int)anova.getDfi()));
        cells.add(new DoubleCell(anova.getMsquri()));
        cells.add(DataType.getMissingCell());
        cells.add(DataType.getMissingCell());
        return cells;
    }

    /**
     * Get the row of the ANOVA table with the cells "Total".
     */
    private List<DataCell> getTotal(final ANOVA anova) {
        final List<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell(m_column));
        cells.add(new StringCell(SOURCE_TOTAL));
        cells.add(new DoubleCell(anova.getSqurb() + anova.getSquri()));
        cells.add(new IntCell((int)(anova.getDfb() + anova.getDfi())));
        cells.add(DataType.getMissingCell());
        cells.add(DataType.getMissingCell());
        cells.add(DataType.getMissingCell());
        return cells;
    }


    /**
     * Get summary statistics per group
     * @return the summary statistics
     */
    public List<SummaryStatistics> getGroupSummaryStatistics() {
        return m_gstats;
    }

    /**
     * Get summary statistics across groups
     * @return the summary statistics
     */
    public SummaryStatistics getSummaryStatistics() {
        return m_stats;
    }

    private class ANOVA {
        /** sum of squares between the groups */
        private double m_squrb;
        /** sum of squares within the groups */
        private double m_squri;
        /** degrees of freedom between the groups */
        private final long m_dfb;
        /** degrees of freedom within the groups */
        private final long m_dfi;
        /** mean square between the groups */
        private final double m_msqurb;
        /** mean square within the groups */
        private final double m_msquri;
        /** the test statistic F */
        private final double m_f;
        /** the p-value */
        private final double m_pValue;

        /**
         * Computes all values for the ANOVA table.
         */
        public ANOVA() {
            final int k = m_groups.size();
            // sum of squares between the groups:
            m_squrb = 0;
            for (final SummaryStatistics stat : m_gstats) {
                m_squrb += stat.getN() * (stat.getMean() - m_stats.getMean()) * (stat.getMean() - m_stats.getMean());
            }

            // sum of squares within the groups:
            m_squri = 0;
            for (final SummaryStatistics stat : m_gstats) {
                m_squri += (stat.getN() - 1) * stat.getStandardDeviation() * stat.getStandardDeviation();
            }

            m_dfb = k - 1;
            m_dfi = m_stats.getN() - k;
            m_msqurb = m_squrb / m_dfb;
            m_msquri = m_squri / m_dfi;
            m_f = m_msqurb / m_msquri;

            final FDistribution distribution = new FDistribution(m_dfb, m_dfi);
            m_pValue = 1 - distribution.cumulativeProbability(m_f);
        }

        /**
         * @return the squrb
         */
        public double getSqurb() {
            return m_squrb;
        }

        /**
         * @return the squri
         */
        public double getSquri() {
            return m_squri;
        }

        /**
         * @return the dfb
         */
        public long getDfb() {
            return m_dfb;
        }

        /**
         * @return the dfi
         */
        public long getDfi() {
            return m_dfi;
        }

        /**
         * @return the msqurb
         */
        public double getMsqurb() {
            return m_msqurb;
        }

        /**
         * @return the msquri
         */
        public double getMsquri() {
            return m_msquri;
        }

        /**
         * @return the f
         */
        public double getF() {
            return m_f;
        }

        /**
         * @return the pValue
         */
        public double getpValue() {
            return m_pValue;
        }


    }
}
