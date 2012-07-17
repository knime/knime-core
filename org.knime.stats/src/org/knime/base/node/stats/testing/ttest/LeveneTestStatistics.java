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
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
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

/**
 * The levence test for two groups.
 *
 * @author Heiko Hofer
 */
public class LeveneTestStatistics {
    public static final String F_VALUE = "test statistic (Levene)";
    public static final String DF1_VALUE = "df 1";
    public static final String DF2_VALUE = "df 2";
    public static final String P_VALUE = "p-value (Levene)";

    /** the test column. */
    private String m_column;
    /** the group identifiers. */
    private List<String> m_groups;
    /** summary statistics per group (levence test).
     * Yji in http://de.wikipedia.org/wiki/Levene-Test*/
    private List<SummaryStatistics> m_lgstats;
    /** summary statistics per group (levence test).
     * Yji - Y_bar in http://de.wikipedia.org/wiki/Levene-Test */
    private List<SummaryStatistics> m_lgstats2;
    /** the mean over all groups */
    private double m_m;
    /** the total number of observations */
    private int m_N;
    /** summary statistics across groups (levence test). */
    private SummaryStatistics m_lstats;
    /** summary statistics per group. */
    private List<SummaryStatistics> m_gstats;
    /** number of missing values per group. */
    private List<MutableInteger> m_missing;
    /** number of missing values in the grouping column */
    private MutableInteger m_missingGroup;


    /**
     * A Levene-Test used in the context of a two sample t-test for comparing
     * the equality of mean.
     * @param column the test column
     * @param groups the group identifiers
     * @param gstats summary statistics per group
     */
    public LeveneTestStatistics(final String column,
            final List<String> groups,
            final List<SummaryStatistics> gstats) {
        super();
        m_column = column;
        m_groups = groups;
        m_gstats = gstats;
        m_missingGroup = new MutableInteger(0);

        int numGroups = groups.size();
        m_lgstats = new ArrayList<SummaryStatistics>(numGroups);
        for (int i = 0; i < numGroups; i++) {
            m_lgstats.add(new SummaryStatistics());
        }
        m_lgstats2 = new ArrayList<SummaryStatistics>(numGroups);
        for (int i = 0; i < numGroups; i++) {
            m_lgstats2.add(new SummaryStatistics());
        }
        m_lstats = new SummaryStatistics();
        m_missing = new ArrayList<MutableInteger>(numGroups);
        for (int i = 0; i < numGroups; i++) {
            m_missing.add(new MutableInteger(0));
        }
        m_N = 0;
        m_m = 0;
        for (int i = 0; i < numGroups; i++) {
            SummaryStatistics statsGi = gstats.get(i);
            double ni = statsGi.getN();
            double mi = statsGi.getMean();
            m_N += (int)ni;
            m_m += ni * mi;
        }
        m_m = m_m / m_N;
    }

    /**
     * Add value to the test.
     * @param value the value
     * @param gIndex the group of the value
     */
    public void addValue(final double value, final int gIndex) {
        // Yji - Y_bar in http://de.wikipedia.org/wiki/Levene-Test
        double Yji = FastMath.abs(value - m_gstats.get(gIndex).getMean());
        m_lgstats.get(gIndex).addValue(Yji);
        m_lgstats2.get(gIndex).addValue(Yji - m_m);
        m_lstats.addValue(
                FastMath.abs(value - m_gstats.get(gIndex).getMean()));
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
    public static DataTableSpec getTableSpec() {
        return new DataTableSpec(new String[] {
                TwoSampleTTestStatistics.TEST_COLUMN
                , TwoSampleTTestStatistics.VARIANCE_ASSUMPTION
                , F_VALUE
                , DF1_VALUE
                , DF2_VALUE
                , P_VALUE
                },
                new DataType[] {
                StringCell.TYPE
                , StringCell.TYPE
                , DoubleCell.TYPE
                , IntCell.TYPE
                , IntCell.TYPE
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
     * Get the test result of the Levene test.
     * @return the Levene test
     */
    public List<List<DataCell>> getTTestCells() {
        if (m_groups.size() == 2) {
            // optimized version for two groups
            return getLeveneTestTwoGroupsCells();
        }


        double num = 0;
        int k = m_groups.size();
        for (int i = 0; i < k; i++) {
            SummaryStatistics statsGi = m_lgstats.get(i);
            double ni = statsGi.getN();
            double mi = statsGi.getMean();
            num += ni * (mi - m_m) * (mi - m_m) / (k - 1);
        }
        double den = 0;
        for (int i = 0; i < k; i++) {
            SummaryStatistics stats2Gi = m_lgstats2.get(i);
            den += stats2Gi.getSumsq() / (m_N - k);
        }
        double L = num / den;

        long df1 = k - 1 ;
        long df2 = m_N - k;
        FDistribution distribution = new FDistribution(df1, df2);
        double pValue = 1 -
            distribution.cumulativeProbability(L);

        List<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell(m_column));
        cells.add(TwoSampleTTestStatistics.EQUAL_VARIANCES_ASSUMED);
        cells.add(new DoubleCell(L));
        cells.add(new IntCell((int)df1));
        cells.add(new IntCell((int)df2));
        cells.add(new DoubleCell(pValue));
        return Collections.singletonList(cells);

    }

    /**
     * Get the test result of the Levene test. This is an optimized version for
     * two groups.
     * @return the Levene test
     */
    public List<List<DataCell>> getLeveneTestTwoGroupsCells() {
        SummaryStatistics statsX = m_lgstats.get(0);
        SummaryStatistics statsY = m_lgstats.get(1);

        // overall sample mean
        double m = m_lstats.getMean();
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

        // Levene's test
        double num = n1 * (m1 - m) * (m1 - m)
            + n2 * (m2 - m) * (m2 - m);
        double den = (n1 - 1) * v1 + (n2 - 1) * v2;
        double L = (n1 + n2 - 2) / den * num;

        long df1 = 1;
        long df2 = (long)n1 + (long)n2 - 2;
        FDistribution distribution = new FDistribution(df1, df2);
        double pValue = 1 -
            distribution.cumulativeProbability(L);

        List<DataCell> cells = new ArrayList<DataCell>();
        cells.add(new StringCell(m_column));
        cells.add(TwoSampleTTestStatistics.EQUAL_VARIANCES_ASSUMED);
        cells.add(new DoubleCell(L));
        cells.add(new IntCell((int)df1));
        cells.add(new IntCell((int)df2));
        cells.add(new DoubleCell(pValue));
        return Collections.singletonList(cells);
    }
}
