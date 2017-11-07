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
 * -------------------------------------------------------------------
 *
 * History
 *   21.04.2005 (cebron): created
 */
package org.knime.base.data.normalize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.base.data.statistics.Statistics3Table;
import org.knime.base.node.util.DoubleFormat;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * A wrapper table to normalize all DataRows. Three methods of normalization are available:
 * <ul>
 * <li>Min-Max Normalization</li>
 * <li>Z-Score Normalization</li>
 * <li>Normalization by decimal scaling</li>
 * </ul>
 * <b>Important !</b> Be sure to pull a new {@link org.knime.core.data.DataTableSpec} with
 * {@link #generateNewSpec(DataTableSpec, String[])}, because {@link org.knime.core.data.def.IntCell} columns are
 * converted to {@link org.knime.core.data.def.DoubleCell} columns.
 *
 * @author Nicolas Cebron, University of Konstanz
 * @since 2.8
 */
public final class Normalizer2 {
    /**
     *
     */
    private static final int DECIMAL_BASE = 10;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Normalizer2.class);

    /**
     * Table to be wrapped.
     */
    private final BufferedDataTable m_table;

    /**
     * Column indices to work on.
     */
    private int[] m_colindices;

    private String m_errormessage;

    /**
     * Prepares a Normalizer to process the buffered data table <code>table</code>. Only columns as contained in the
     * array argument are considered.
     *
     * @param table table to be wrapped
     * @param columns to work on
     * @see DataTable#getDataTableSpec()
     */
    public Normalizer2(final BufferedDataTable table, final String[] columns) {
        m_table = table;
        DataTableSpec spec = table.getDataTableSpec();
        m_colindices = findNumericalColumns(spec, columns);
    }

    /**
     * Creates a new DataTableSpec. IntCell-columns are converted to DoubleCell-columns.
     *
     * @param inspec the DataTableSpec of the input table
     * @param columns the columns that are normalized
     * @return DataTableSpec for the output table
     */
    public static final DataTableSpec generateNewSpec(final DataTableSpec inspec, final String[] columns) {
        // filters out all non-numerical columns in argument
        int[] colindices = findNumericalColumns(inspec, columns);
        Arrays.sort(colindices); // will make a binary search on it.
        int nrCols = inspec.getNumColumns();
        DataColumnSpec[] colspecs = new DataColumnSpec[nrCols];
        for (int i = 0; i < nrCols; i++) {
            DataColumnSpec colspec = inspec.getColumnSpec(i);
            if (Arrays.binarySearch(colindices, i) >= 0) {
                DataType coltype = colspec.getType();
                // findNumericalColumns makes sure that only double compatible
                // types are included.
                assert coltype.isCompatible(DoubleValue.class);
                DataColumnSpecCreator c = new DataColumnSpecCreator(colspec);
                // must not set domain - that will change anyway.
                c.setDomain(null);
                // int type must be overwritten
                c.setType(DoubleCell.TYPE);
                colspecs[i] = c.createSpec();
            } else {
                colspecs[i] = colspec;
            }
        }
        return new DataTableSpec(colspecs);
    }

    /**
     * Method that looks into spec and filters all columns given by the second argument AND which are also double
     * compatible.
     *
     * @param spec the spec to look into
     * @param columns the columns to include
     * @return the valid indices
     */
    private static final int[] findNumericalColumns(final DataTableSpec spec, final String[] columns) {
        int[] colindices = new int[columns.length];
        int validCount = 0;
        for (int i = 0; i < columns.length; i++) {
            int index = spec.findColumnIndex(columns[i]);
            if (index < 0) {
                throw new IllegalArgumentException("Column \"" + columns[i] + "\" not contained in data.");
            }
            DataType type = spec.getColumnSpec(index).getType();
            if (!type.isCompatible(DoubleValue.class)) {
                LOGGER.debug("Non-numerical column: \"" + columns[i] + "\", skipping.");
            } else {
                colindices[validCount] = index;
                validCount++;
            }
        }
        int[] result = new int[validCount];
        System.arraycopy(colindices, 0, result, 0, validCount);
        return result;
    }

    /**
     * Does the Min-Max Normalization.
     *
     * @param newmax the new maximum
     * @param newmin the new minimum
     * @param exec an object to check for user cancelations. Can be <code>null</code>.
     * @throws CanceledExecutionException if user canceled
     * @return normalized DataTable
     */
    public AffineTransTable doMinMaxNorm(final double newmax, final double newmin, final ExecutionContext exec)
        throws CanceledExecutionException {
        ExecutionContext statisticsExec = exec.createSilentSubExecutionContext(.5);
        Statistics3Table st;
        st = new Statistics3Table(m_table, false, 0, Collections.<String> emptyList(), statisticsExec);
        checkForMissVals(st);

        DataTableSpec spec = m_table.getDataTableSpec();
        double[] max = st.getMax();
        double[] min = st.getMin();
        final double[] scales = new double[m_colindices.length];
        final double[] transforms = new double[m_colindices.length];
        final double[] mins = new double[m_colindices.length];
        final double[] maxs = new double[m_colindices.length];

        for (int i = 0; i < transforms.length; i++) {
            DataColumnSpec cSpec = spec.getColumnSpec(m_colindices[i]);
            boolean isDouble = cSpec.getType().isCompatible(DoubleValue.class);
            if (!isDouble) {
                assert (!isDouble);
                scales[i] = Double.NaN;
                transforms[i] = Double.NaN;
                mins[i] = Double.NaN;
                maxs[i] = Double.NaN;
            } else {
                // scales and translation to [0,1]
                double maxI = max[m_colindices[i]];
                double minI = min[m_colindices[i]];
                scales[i] = (maxI == minI ? 1 : 1.0 / (maxI - minI));
                transforms[i] = -minI * scales[i];
                // scale and translation to [newmin, newmax]
                scales[i] *= (newmax - newmin);
                transforms[i] *= (newmax - newmin);
                transforms[i] += newmin;
                mins[i] = newmin;
                maxs[i] = newmax;
            }
        }
        String[] includes = getNames();
        String minS = DoubleFormat.formatDouble(newmin);
        String maxS = DoubleFormat.formatDouble(newmax);
        String summary = "Min/Max (" + minS + ", " + maxS + ") normalization " + "on " + includes.length + " column(s)";
        AffineTransConfiguration configuration =
            new AffineTransConfiguration(includes, scales, transforms, mins, maxs, summary);
        return new AffineTransTable(m_table, configuration);
    }

    /**
     * Does the Z-Score Normalization.
     *
     * @param exec an object to check for user cancelations. Can be <code>null</code>.
     * @throws CanceledExecutionException if user canceled
     * @return the normalized DataTable
     */
    public AffineTransTable doZScoreNorm(final ExecutionContext exec) throws CanceledExecutionException {
        ExecutionContext statisticsExec = exec.createSubExecutionContext(.5);
        final Statistics3Table st =
            new Statistics3Table(m_table, false, 0, Collections.<String> emptyList(), statisticsExec);
        checkForMissVals(st);
        double[] mean = st.getMean();
        double[] stddev = st.getStandardDeviation();

        final double[] scales = new double[m_colindices.length];
        final double[] transforms = new double[m_colindices.length];
        final double[] mins = new double[m_colindices.length];
        final double[] maxs = new double[m_colindices.length];

        for (int i = 0; i < m_colindices.length; i++) {
            if (Double.isNaN(mean[m_colindices[i]])) {
                scales[i] = Double.NaN;
                transforms[i] = Double.NaN;
            } else {
                scales[i] = (stddev[m_colindices[i]] == 0.0 ? 1.0 : 1.0 / stddev[m_colindices[i]]);
                transforms[i] = -mean[m_colindices[i]] * scales[i];
            }
            mins[i] = Double.NaN;
            maxs[i] = Double.NaN;
        }

        String[] includes = getNames();
        String summary = "Z-Score (Gaussian) normalization on " + includes.length + " column(s)";
        AffineTransConfiguration configuration =
            new AffineTransConfiguration(includes, scales, transforms, mins, maxs, summary);
        return new AffineTransTable(m_table, configuration);
    }

    /**
     * Does the decimal scaling.
     *
     * @param exec an object to check for user cancellations. Can be <code>null</code>.
     * @throws CanceledExecutionException if user canceled
     * @return the normalized DataTable
     */
    public AffineTransTable doDecimalScaling(final ExecutionContext exec) throws CanceledExecutionException {
        Statistics3Table st = new Statistics3Table(m_table, false, 0, Collections.<String> emptyList(), exec);
        checkForMissVals(st);
        String[] includes = getNames();
        double[] max = st.getMax();
        double[] min = st.getMin();
        double[] scales = new double[m_colindices.length];
        double[] transforms = new double[m_colindices.length];
        double[] mins = new double[m_colindices.length];
        double[] maxs = new double[m_colindices.length];
        for (int i = 0; i < m_colindices.length; i++) {
            int trueIndex = m_colindices[i];
            double absMax = Math.abs(max[trueIndex]);
            double absMin = Math.abs(min[trueIndex]);
            double maxvalue = absMax > absMin ? absMax : absMin;
            int exp = 0;
            // Unreported bug fix: when there was an infinite value, it takes infinite time to reach 1 by / 10.
            if (Double.isInfinite(maxvalue)) {
                throw new IllegalStateException("Cannot handle infinite values: " + includes[i]);
            }
            while (Math.abs(maxvalue) > 1) {
                maxvalue = maxvalue / DECIMAL_BASE;
                exp++;
            }
            scales[i] = 1.0 / Math.pow(DECIMAL_BASE, exp);
            transforms[i] = 0.0;
            mins[i] = -1.0;
            maxs[i] = 1.0;
        }
        String summary = "Decimal Scaling normalization on " + includes.length + " column(s)";
        AffineTransConfiguration configuration =
            new AffineTransConfiguration(includes, scales, transforms, mins, maxs, summary);
        return new AffineTransTable(m_table, configuration);
    }

    /* Get the names for all included columns. */
    private String[] getNames() {
        int[] cols = m_colindices;
        String[] result = new String[cols.length];
        DataTableSpec spec = m_table.getDataTableSpec();
        for (int i = 0; i < cols.length; i++) {
            result[i] = spec.getColumnSpec(m_colindices[i]).getName();
        }
        return result;
    }

    private void checkForMissVals(final Statistics3Table table) {
        List<Integer> missValsColVec = new ArrayList<Integer>();
        int[] missingCount = table.getNumberMissingValues();
        for (int index = 0; index < missingCount.length; index++) {
            if (missingCount[index] == table.getRowCount()) {
                boolean isIncluded = false;
                for (int incl : m_colindices) {
                    if (incl == index) {
                        isIncluded = true;
                    }
                }
                if (isIncluded) {
                    missValsColVec.add(index);
                }

            }
        }
        if (missValsColVec.size() > 0) {
            StringBuffer missColsBuffer = new StringBuffer();
            for (Integer i : missValsColVec) {
                missColsBuffer.append(m_table.getDataTableSpec().getColumnSpec(i).getName()).append(" ");
            }
            String message = "Ignore column(s) " + missColsBuffer.toString() + "as it/they contain only missing values";
            setErrorMessage(message);
            List<Integer> newColIndices = new ArrayList<Integer>();
            for (int val : m_colindices) {
                newColIndices.add(val);
            }
            newColIndices.removeAll(missValsColVec);
            int[] colIndices = new int[newColIndices.size()];
            int counter = 0;
            for (Integer i : newColIndices) {
                colIndices[counter] = i;
                counter++;
            }
            m_colindices = colIndices;
        }
    }

    /**
     * Sets an error message, if something went wrong during initialization.
     *
     * @param message the message to set.
     */
    void setErrorMessage(final String message) {
        if (m_errormessage == null) {
            m_errormessage = message;
        }
    }

    /**
     * @return error message if something went wrong, <code>null</code> otherwise.
     */
    public String getErrorMessage() {
        return m_errormessage;
    }
}
