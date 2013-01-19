/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   04.06.2011 (hofer): created
 */
package org.knime.base.node.viz.crosstable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Gamma;
import org.knime.base.node.viz.crosstable.CrosstabNodeModel.CrosstabTotals;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

/**
 * Compute statistics for the crosstab node.
 *
 * @author Heiko Hofer
 */
class CrosstabStatisticsCalculator {
    private final BufferedDataTable m_freqTable;
    private final int m_freqIndex;
    private final int m_rowIndex;
    private final int m_colIndex;
    private final CrosstabTotals m_totals;
    private boolean m_isRun;
    private BufferedDataTable m_statTable;
    private CrosstabStatistics m_statistics;

    private final CrosstabProperties m_props;

    /**
     * @param freqTable
     *            table with at least three columns for the explanatory and
     *            response variable and for the frequency.
     * @param rowIndex
     *            the column index of the explanatory variable
     * @param colIndex
     *            the column index of the response variable
     * @param freqIndex
     *            the column index of the frequency
     * @param totals
     *            the wrapper for the totals (row count total, column count
     *            totals and total count)
     * @param props
     *            the crosstab properties which is a source for the column names
     *            of the table return by getTable()
     */
    public CrosstabStatisticsCalculator(final BufferedDataTable freqTable,
            final int rowIndex, final int colIndex, final int freqIndex,
            final CrosstabTotals totals, final CrosstabProperties props) {
        super();
        m_freqTable = freqTable;
        m_rowIndex = rowIndex;
        m_colIndex = colIndex;
        m_freqIndex = freqIndex;
        m_totals = totals;
        m_isRun = false;
        m_props = props;
    }

    /**
     * This method is not Thread-Safe. Call this method before calling any of
     * the getter methods.
     *
     * @param exec
     *            the execution context.
     */
    void run(final ExecutionContext exec) {
        if (m_isRun) {
            return;
        }
        m_isRun = true;
        double chiSquare = 0;
        DataTableSpec spec = createSpec();
        BufferedDataContainer cont = exec.createDataContainer(spec);
        for (DataRow row : m_freqTable) {
            DataCell rowVar = row.getCell(m_rowIndex);
            DataCell colVar = row.getCell(m_colIndex);
            DataCell freqCell = row.getCell(m_freqIndex);
            List<DataCell> cells = new ArrayList<DataCell>();
            DoubleCell cellChiSquare = cellChiSquare(rowVar, colVar, freqCell);
            cells.add(cellChiSquare);
            chiSquare += cellChiSquare.getDoubleValue();
            DataRow statRow = new DefaultRow(row.getKey(), cells);
            cont.addRowToTable(statRow);
        }
        cont.close();
        m_statTable = cont.getTable();

        // compute chi-square statistic
        int chiSquareDF = (m_totals.getRowTotal().size() - 1)
            * (m_totals.getColTotal().size() - 1);
        double chiSquarePValue = chiQuarePValue(chiSquare, chiSquareDF);
        // compute fisher's exact test
        double fisherPValue = fisherExactPValue(m_freqTable,
                m_rowIndex, m_colIndex,
                m_freqIndex,
                m_totals.getRowTotal().keySet(),
                m_totals.getColTotal().keySet());
        m_statistics = new CrosstabStatistics(chiSquare, chiSquareDF,
                chiSquarePValue, fisherPValue);
        m_statistics.run(exec);

    }

    /**
     * Compute p-value of the chi-squared statistics.
     * @param x the value
     * @param df the degrees of freedom
     * @return the p-value of the chi-squared statistics
     */
    private double chiQuarePValue(final double x, final int df) {
        try {
            return Gamma.regularizedGammaQ(df / 2.0, x / 2.0);
        } catch (MathException e) {
            throw new IllegalStateException("Chi-square statistics cannot"
                    + "be computed.");
        }
    }

    /**
     * Compute the cell chi-square, e.g. the contribution to the chi-square
     * statistic of this cell.
     */
    private DoubleCell cellChiSquare(final DataCell rowVar,
            final DataCell colVar, final DataCell freqCell) {
        double nij = freqCell.isMissing() ? 0.0 : ((DoubleValue) freqCell)
                .getDoubleValue();
        // the row total
        double nidot = m_totals.getRowTotal().get(rowVar);
        // the column total
        double ndotj = m_totals.getColTotal().get(colVar);
        // the overall total
        double n = m_totals.getTotal();
        double eij = nidot / n * ndotj;
        // the cell chi square
        double cellChiSquare = (nij / eij * nij) - (2 * nij) + eij;
        return new DoubleCell(cellChiSquare);
    }


    /**
     * Compute the p-value of the Fisther's exact test.
     */
    private double fisherExactPValue(final BufferedDataTable freqTable,
            final int rowIndex, final int colIndex, final int freqIndex,
            final Collection<DataCell> rowVars,
            final Collection<DataCell> colVars) {
        if (rowVars.size() != 2 || colVars.size() != 2) {
            return Double.NaN;
        }
        List<DataCell> rowVarList = new ArrayList<DataCell>();
        rowVarList.addAll(rowVars);
        List<DataCell> colVarList = new ArrayList<DataCell>();
        colVarList.addAll(colVars);

        // The column of freqTable with index freqIndex holds the data of the
        // contigency table. Fill crosstab with this data.
        int[][] crosstab = new int[2][2];
        for (DataRow row : freqTable) {
            DataCell cell = row.getCell(freqIndex);
            int intFreq = 0;
            if (!cell.isMissing()) {
                Double v = ((DoubleValue)cell).getDoubleValue();
                double freq = v;
                intFreq = (int)freq;
                // test if freq is a nonnegative integer
                if (freq != intFreq || intFreq < 0) {
                    return Double.NaN;
                }
            } else {
                intFreq = 0;
            }
            int i = rowVarList.indexOf(row.getCell(rowIndex));
            int k = colVarList.indexOf(row.getCell(colIndex));
            crosstab[i][k] = intFreq;
        }

        int cutoffIndex = Math.min(crosstab[1][0], crosstab[0][1]);
        double cutoffPValue = fishExactCondMatProb(crosstab);
        // Test variations
        crosstab[0][0] = crosstab[0][0] + cutoffIndex;
        crosstab[0][1] = crosstab[0][1] - cutoffIndex;
        crosstab[1][0] = crosstab[1][0] - cutoffIndex;
        crosstab[1][1] = crosstab[1][1] + cutoffIndex;
        int numVariations = Math.min(crosstab[0][0], crosstab[1][1]) + 1;
        double pValue = 0;
        for (int i = 0; i < numVariations; i++) {
            if (i != cutoffIndex) {
                double varPValue = fishExactCondMatProb(crosstab);
                if (varPValue <= cutoffPValue) {
                    pValue += varPValue;
                }
            } else {
                // This case has been calculated before
                pValue += cutoffPValue;
            }
            nextNonnegativeVariation(crosstab);
        }
        return pValue;
    }

    /**
     * Computes P_fisher an internal function used by fisherExactPValue
     */
    private double fishExactCondMatProb(final int[][] crosstab) {
        final double lowerBound = 0.00001;
        final double upperBound = 1;

        int[] nom = new int[] {
                crosstab[0][0] + crosstab[0][1],
                crosstab[1][0] + crosstab[1][1],
                crosstab[0][0] + crosstab[1][0],
                crosstab[0][1] + crosstab[1][1],
        };
        int[] den = new int[] {
                nom[0] + nom[1],
                crosstab[0][0],
                crosstab[0][1],
                crosstab[1][0],
                crosstab[1][1]
        };

        checkZeros(nom);
        checkZeros(den);

        int i = 0;
        int k = 0;
        double pvalue = 1.0;
        while (i < nom.length || k < den.length) {
            // process some values in the denominator
            while (k < den.length && (pvalue > lowerBound || i >= nom.length)) {
                pvalue = pvalue / den[k];
                den[k] = den[k] - 1;
                if (den[k] <= 1) {
                    k++;
                }
            }
            // process some values in the nominator
            while (i < nom.length && (pvalue < upperBound || k >= den.length)) {
                pvalue = pvalue * nom[i];
                nom[i] = nom[i] - 1;
                if (nom[i] <= 1) {
                    i++;
                }
            }
        }
        return pvalue;
    }

    /**
     * Determines the next variation. Returns P_v.
     */
    private boolean nextNonnegativeVariation(final int[][] crosstab) {
        if (crosstab[0][0] > 0 && crosstab[1][1] > 0) {
            crosstab[0][0] = crosstab[0][0] - 1;
            crosstab[0][1] = crosstab[0][1] + 1;
            crosstab[1][0] = crosstab[1][0] + 1;
            crosstab[1][1] = crosstab[1][1] - 1;
            return true;
        }
        return false;
    }

    /**
     * Converts all zeros in vec to one which is done before caculating the
     * factorial. 0! = 1 by definition.
     */
    private void checkZeros(final int[] vec) {
        for (int i = 0; i < vec.length; i++) {
            if (vec[i] == 0) {
                vec[i] = 1;
            }
        }
    }

    /**
     * The table with statistics which is appended to the output of the crosstab
     * node (contains e.g. the cell chi-square). Make sure the run() is finished
     * before calling this method.
     *
     * @return row-wise statistics to be appended to the output of the crosstab
     *         node
     */
    BufferedDataTable getTable() {
        return m_statTable;
    }

    /**
     * Get the statistics for the node.
     * Make sure the run() is finished before calling this method.
     * @return the statistics
     */
    CrosstabStatistics getStatistics() {
        return m_statistics;
    }

    /**
     * Create the data column spec of the table return by getTable().
     */
    private DataTableSpec createSpec() {
        List<DataColumnSpec> cspecs = new ArrayList<DataColumnSpec>();
        String cellChiSquare = m_props.getCellChiSquareName();
        cspecs.add((new DataColumnSpecCreator(cellChiSquare, DoubleCell.TYPE))
                .createSpec());
        return new DataTableSpec(cspecs.toArray(new DataColumnSpec[cspecs
                .size()]));
    }

    /**
     * Container for the test statistics.
     *
     * @author Heiko Hofer
     */
    static class CrosstabStatistics {
        private static final String CHI_SQUARE = "Chi-Square";
        private static final String CHI_SQUARE_DF = "Chi-Square (DF)";
        private static final String CHI_SQUARE_PVALUE = "Chi-Square (Prop)";
        private static final String FISHER_EXACT_PVALUE =
            "Fisher's Excact Test (2-Tail) (Prop)";

        private final double m_chiSquare;
        private final int m_chiSquareDF;
        private final double m_chiSquarePValue;
        private final double m_fisherExactPValue;
        private BufferedDataTable m_table;

        /**
         * @param chiSquare chi-squared test statistics - value
         * @param chiSquareDF chi-squared test statistics - degrees of freedom
         * @param chiSquarePValue chi-squared test statistics - p-value
         * @param fisherExactPValue the p-value of Fisher's exact test
         */
        CrosstabStatistics(final double chiSquare,
                final int chiSquareDF,
                final double chiSquarePValue,
                final double fisherExactPValue) {
            super();
            m_chiSquare = chiSquare;
            m_chiSquareDF = chiSquareDF;
            m_chiSquarePValue = chiSquarePValue;
            m_fisherExactPValue = fisherExactPValue;
        }

        /**
         * Retrieve statistics from the given table.
         * @param table the data table width the statistics
         */
        CrosstabStatistics(final BufferedDataTable table) {
            super();
            m_table = table;
            DataTableSpec spec = table.getDataTableSpec();
            DataRow row = m_table.iterator().next();
            m_chiSquare = getValue(row, CHI_SQUARE, spec);
            m_chiSquareDF = (int)getValue(row, CHI_SQUARE_DF, spec);
            m_chiSquarePValue = getValue(row, CHI_SQUARE_PVALUE, spec);
            m_fisherExactPValue = getValue(row, FISHER_EXACT_PVALUE, spec);
        }

        private double getValue(final DataRow row, final String col,
                final DataTableSpec spec) {
            DataCell cell = row.getCell(spec.findColumnIndex(col));
            if (cell.isMissing()) {
                return Double.NaN;
            } else {
                return ((DoubleValue)cell).getDoubleValue();
            }
        }

        /**
         * Create the data table spec of the table return by getTable().
         * @return the data table spec
         */
        static DataTableSpec createSpec() {
            List<DataColumnSpec> cspecs = new ArrayList<DataColumnSpec>();
            cspecs.add((new DataColumnSpecCreator(CHI_SQUARE,
                    DoubleCell.TYPE)).createSpec());
            cspecs.add((new DataColumnSpecCreator(CHI_SQUARE_DF,
                    IntCell.TYPE)).createSpec());
            cspecs.add((new DataColumnSpecCreator(CHI_SQUARE_PVALUE,
                    DoubleCell.TYPE)).createSpec());
            cspecs.add((new DataColumnSpecCreator(FISHER_EXACT_PVALUE,
                    DoubleCell.TYPE)).createSpec());
            return new DataTableSpec(cspecs.toArray(new DataColumnSpec[cspecs
                    .size()]));
        }

        /**
         * Create the statistics data table.
         *
         * @param exec the execution context.
         */
        void run(final ExecutionContext exec) {
            DataTableSpec spec = createSpec();
            BufferedDataContainer cont = exec.createDataContainer(spec);
            List<DataCell> cells = new ArrayList<DataCell>();
            cells.add(new DoubleCell(m_chiSquare));
            cells.add(new IntCell(m_chiSquareDF));
            cells.add(new DoubleCell(m_chiSquarePValue));
            if (!Double.isNaN(m_fisherExactPValue)) {
                cells.add(new DoubleCell(m_fisherExactPValue));
            } else {
                cells.add(DataType.getMissingCell());
            }
            cont.addRowToTable(new DefaultRow(RowKey.createRowKey(0),
                    cells));
            cont.close();
            m_table = cont.getTable();
        }

        /**
         * Get the Pearson chi-squared statistic.
         *
         * @return the Pearson chi-squared statistic.
         */
        double getChiSquaredStatistic() {
            return m_chiSquare;
        }

        /**
         * Get the degrees of freedom for the Pearson chi-squared statistic.
         *
         * @return the degrees of freedom Pearson chi-squared statistic.
         */
        int getChiSquaredDegreesOfFreedom() {
            return m_chiSquareDF;
        }

        /**
         * Get the p-value for the Pearson chi-squared statistic.
         *
         * @return the p-value for the  Pearson chi-squared statistic.
         */
        double getChiSquaredPValue() {
            return m_chiSquarePValue;
        }

        /**
         * Get the p-value for the Fisher's exact test.
         *
         * @return the p-value for the Fisher's exact test
         */
        double getFisherExactPValue() {
            return m_fisherExactPValue;
        }

        /**
         * @return the table with the statistics
         */
        BufferedDataTable getTable() {
            return m_table;
        }

    }
}
