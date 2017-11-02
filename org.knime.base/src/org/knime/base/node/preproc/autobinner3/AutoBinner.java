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
 *   12.07.2010 (hofer): created
 */
package org.knime.base.node.preproc.autobinner3;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.autobinner.apply.AutoBinnerApply;
import org.knime.base.node.preproc.autobinner.pmml.DisretizeConfiguration;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretize;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretizeBin;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretizePreprocPortObjectSpec;
import org.knime.base.node.preproc.autobinner.pmml.PMMLInterval;
import org.knime.base.node.preproc.autobinner.pmml.PMMLInterval.Closure;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretize;
import org.knime.base.node.preproc.autobinner3.AutoBinnerLearnSettings.BinNaming;
import org.knime.base.node.preproc.autobinner3.AutoBinnerLearnSettings.EqualityMethod;
import org.knime.base.node.preproc.autobinner3.AutoBinnerLearnSettings.Method;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocPortObjectSpec;

/**
 * Creates Bins. Use this class in other nodes.
 *
 * @author Heiko Hofer
 */
public class AutoBinner {

    private AutoBinnerLearnSettings m_settings;

    private PMMLPreprocPortObjectSpec m_pmmlOutSpec;

    private DataTableSpec m_tableOutSpec;

    private String[] m_included;

    /**
     * @param settings The settings object.
     * @param spec the data table spec
     * @throws InvalidSettingsException when settings are not consistent
     */
    public AutoBinner(final AutoBinnerLearnSettings settings, final DataTableSpec spec) throws InvalidSettingsException {
        m_settings = settings;
        m_included = m_settings.getFilterConfiguration().applyTo(spec).getIncludes();
    }

    /**
     * @return the settings
     */
    protected AutoBinnerLearnSettings getSettings() {
        return m_settings;
    }

    /**
     * Determine bins.
     *
     * @param data the input data
     * @param exec the execution context
     * @return the operation with the discretisation information
     * @throws Exception ...
     */
    public PMMLPreprocDiscretize execute(final BufferedDataTable data, final ExecutionContext exec) throws Exception {
        final DataTableSpec spec = data.getDataTableSpec();
        // determine intervals
        if (m_settings.getMethod().equals(Method.fixedNumber)) {
            if (m_settings.getEqualityMethod().equals(EqualityMethod.width)) {
                BufferedDataTable inData =
                    calcDomainBoundsIfNeccessary(data, exec.createSubExecutionContext(0.9),
                        Arrays.asList(m_included));
                init(inData.getDataTableSpec());
                Map<String, double[]> edgesMap = new HashMap<String, double[]>();
                for (String target : m_included) {
                    DataTableSpec inSpec = inData.getDataTableSpec();
                    DataColumnSpec targetCol = inSpec.getColumnSpec(target);

                    // bounds of the domain
                    double min = ((DoubleValue)targetCol.getDomain().getLowerBound()).getDoubleValue();
                    double max = ((DoubleValue)targetCol.getDomain().getUpperBound()).getDoubleValue();

                    // the edges of the bins
                    int binCount = m_settings.getBinCount();

                    double[] edges = calculateBounds(binCount, min, max);

                    if (m_settings.getIntegerBounds()) {
                        edges = toIntegerBoundaries(edges);
                    }

                    edgesMap.put(target, edges);
                }
                return createDisretizeOp(edgesMap);
            } else { // EqualityMethod.equalCount
                Map<String, double[]> edgesMap = new HashMap<String, double[]>();
                for (String target : m_included) {
                    int colIndex = data.getDataTableSpec().findColumnIndex(target);
                    List<Double> values = new ArrayList<Double>();
                    for (DataRow row : data) {
                        if (!row.getCell(colIndex).isMissing()) {
                            values.add(((DoubleValue)row.getCell(colIndex)).getDoubleValue());
                        }
                    }
                    edgesMap.put(target, findEdgesForEqualCount(values, m_settings.getBinCount()));
                }
                return createDisretizeOp(edgesMap);
            }
        } else if (m_settings.getMethod().equals(Method.sampleQuantiles)) {
            init(spec);
            Map<String, double[]> edgesMap = new LinkedHashMap<String, double[]>();
            final int colCount = m_included.length;
            // contains all numeric columns if include all is set!
            for (String target : m_included) {
                exec.setMessage("Calculating quantiles (column \"" + target + "\")");
                ExecutionContext colSortContext = exec.createSubExecutionContext(0.7 / colCount);
                ExecutionContext colCalcContext = exec.createSubExecutionContext(0.3 / colCount);
                ColumnRearranger singleRearranger = new ColumnRearranger(spec);
                singleRearranger.keepOnly(target);
                BufferedDataTable singleColSorted =
                    colSortContext.createColumnRearrangeTable(data, singleRearranger, colSortContext);
                SortedTable sorted =
                    new SortedTable(singleColSorted, Collections.singletonList(target), new boolean[]{true},
                        colSortContext);
                colSortContext.setProgress(1.0);
                double[] edges =
                    createEdgesFromQuantiles(sorted.getBufferedDataTable(), colCalcContext,
                        m_settings.getSampleQuantiles());
                colCalcContext.setProgress(1.0);
                exec.clearTable(singleColSorted);
                if (m_settings.getIntegerBounds()) {
                    edges = toIntegerBoundaries(edges);
                }
                edgesMap.put(target, edges);
            }
            return createDisretizeOp(edgesMap);
        } else {
            throw new IllegalStateException("Unknown binning method.");
        }
    }

    /**
     * @param binCount number of bins
     * @param min minimum value
     * @param max maximum value
     * @return the boundaries
     */
    public static double[] calculateBounds(final int binCount, final double min, final double max) {
        double[] edges = new double[binCount + 1];
        edges[0] = min;
        edges[edges.length - 1] = max;
        for (int i = 1; i < edges.length - 1; i++) {
            edges[i] = min + i / (double)binCount * (max - min);
        }
        return edges;
    }

    /**
     * Converts double boundaries to integer boundaries. The resulting array may be shorter if some boundaries map to
     * the same integer value.
     *
     * @param boundaries a sorted array of boundaries
     * @return the new boundaries, all integer values
     * @since 3.1
     */
    public static double[] toIntegerBoundaries(final double[] boundaries) {
        Set<Double> intBoundaries = new TreeSet<Double>();
        intBoundaries.add(Math.floor(boundaries[0]));
        for (int i = 1; i < boundaries.length; i++) {
            intBoundaries.add(Math.ceil(boundaries[i]));
        }
        double[] newEdges = new double[intBoundaries.size()];
        int i = 0;
        for (Double edge : intBoundaries) {
            newEdges[i++] = edge;
        }
        return newEdges;
    }

    private double[] findEdgesForEqualCount(final List<Double> values, final int binCount) {
        Collections.sort(values);
        int countPerBin = (int)(Math.round(values.size() / (double)binCount));
        double[] edges = new double[binCount + 1];
        edges[0] = m_settings.getIntegerBounds() ? Math.floor(values.get(0)) : values.get(0);
        edges[edges.length - 1] = roundedValue(values.get(values.size() - 1));
        int startIndex = 0;
        int index = countPerBin - 1;
        for (int i = 1; i < edges.length - 1; i++) {
            if (index < values.size()) {
                double edge = roundedValue(values.get(index));
                // get lower index
                int lowerIndex = index;
                while (lowerIndex >= startIndex && !(edge > roundedValue(values.get(lowerIndex)))) {
                    lowerIndex--;
                }
                // get higher index
                int higherIndex = index;
                while (higherIndex < values.size() - 1 && !(roundedValue(values.get(higherIndex + 1)) > edge)) {
                    higherIndex++;
                }
                int lowerDiff = -1 * (lowerIndex - startIndex + 1 - countPerBin);
                int higherDiff = higherIndex - startIndex + 1 - countPerBin;
                if (!(lowerIndex < startIndex) && lowerDiff <= higherDiff) {
                    index = lowerIndex;
                } else {
                    index = higherIndex;
                }
                edges[i] = roundedValue(values.get(index));
                startIndex = index + 1;
                index += countPerBin;
            } else {
                edges[i] = edges[i - 1];
            }
        }
        return edges;
    }

    private double roundedValue(final double value) {
        return m_settings.getIntegerBounds() ? Math.ceil(value) : value;
    }

    @SuppressWarnings("null")
    private static double[] createEdgesFromQuantiles(final BufferedDataTable data, final ExecutionContext exec,
        final double[] sampleQuantiles) throws CanceledExecutionException {
        double[] edges = new double[sampleQuantiles.length];
        long n = data.size();
        long c = 0;
        int cc = 0;
        RowIterator iter = data.iterator();
        DataRow rowQ = null;
        DataRow rowQ1 = null;
        if (iter.hasNext()) {
            rowQ1 = iter.next();
            rowQ = rowQ1;
        }

        for (double p : sampleQuantiles) {
            double h = (n - 1) * p + 1;
            int q = (int)Math.floor(h);
            while ((1.0 == p || c < q) && iter.hasNext()) {
                rowQ = rowQ1;
                rowQ1 = iter.next();
                c++;
                exec.setProgress(c / (double)n);
                exec.checkCanceled();
            }
            rowQ = 1.0 != p ? rowQ : rowQ1;
            final DataCell xqCell = rowQ.getCell(0);
            final DataCell xq1Cell = rowQ1.getCell(0);
            // TODO should be able to handle missing values (need to filter
            // data first?)
            if (xqCell.isMissing() || xq1Cell.isMissing()) {
                throw new RuntimeException("Missing values not support for " + "quantile calculation (error in row \""
                    + rowQ1.getKey() + "\")");
            }
            // for quantile calculation see also
            // http://en.wikipedia.org/wiki/
            //                Quantile#Estimating_the_quantiles_of_a_population.
            // this implements R-7
            double xq = ((DoubleValue)xqCell).getDoubleValue();
            double xq1 = ((DoubleValue)xq1Cell).getDoubleValue();
            double quantile = xq + (h - q) * (xq1 - xq);
            edges[cc] = quantile;
            cc++;
        }
        return edges;
    }

    /**
     * @param edgesMap the boundary map
     * @return the {@link PMMLPreprocDiscretize} model
     */
    protected PMMLPreprocDiscretize createDisretizeOp(final Map<String, double[]> edgesMap) {
        Map<String, List<PMMLDiscretizeBin>> binMap = createBins(edgesMap);

        List<String> names = new ArrayList<String>();
        Map<String, PMMLDiscretize> discretize = new HashMap<String, PMMLDiscretize>();
        for (String target : m_included) {
            String binnedCol = m_settings.getReplaceColumn() ? target : target + " [Binned]";
            names.add(binnedCol);
            discretize.put(binnedCol, new PMMLDiscretize(target, binMap.get(target)));
        }

        DisretizeConfiguration config = new DisretizeConfiguration(names, discretize);

        PMMLPreprocDiscretize op = new PMMLPreprocDiscretize(config);
        return op;
    }

    private Map<String, List<PMMLDiscretizeBin>> createBins(final Map<String, double[]> edgesMap) {
        BinnerNumberFormat formatter = new BinnerNumberFormat();
        Map<String, List<PMMLDiscretizeBin>> binMap = new HashMap<String, List<PMMLDiscretizeBin>>();
        for (String target : m_included) {
            if (null != edgesMap && null != edgesMap.get(target) && edgesMap.get(target).length > 1) {
                double[] edges = edgesMap.get(target);
                // Names of the bins
                String[] binNames = new String[edges.length - 1];
                if (m_settings.getBinNaming().equals(BinNaming.numbered)) {
                    for (int i = 0; i < binNames.length; i++) {
                        binNames[i] = "Bin " + (i + 1);
                    }
                } else if (m_settings.getBinNaming().equals(BinNaming.edges)) {
                    binNames[0] = "[" + formatter.format(edges[0]) + "," + formatter.format(edges[1]) + "]";
                    for (int i = 1; i < binNames.length; i++) {
                        binNames[i] = "(" + formatter.format(edges[i]) + "," + formatter.format(edges[i + 1]) + "]";
                    }
                } else { // BinNaming.midpoints
                    binNames[0] = formatter.format((edges[1] - edges[0]) / 2 + edges[0]);
                    for (int i = 1; i < binNames.length; i++) {
                        binNames[i] = formatter.format((edges[i + 1] - edges[i]) / 2 + edges[i]);
                    }
                }
                List<PMMLDiscretizeBin> bins = new ArrayList<PMMLDiscretizeBin>();
                bins.add(new PMMLDiscretizeBin(binNames[0], Arrays.asList(new PMMLInterval(edges[0], edges[1],
                    Closure.closedClosed))));
                for (int i = 1; i < binNames.length; i++) {
                    bins.add(new PMMLDiscretizeBin(binNames[i], Arrays.asList(new PMMLInterval(edges[i], edges[i + 1],
                        Closure.openClosed))));
                }
                binMap.put(target, bins);
            } else {
                binMap.put(target, new ArrayList<PMMLDiscretizeBin>());
            }
        }
        return binMap;
    }

    /**
     * Determines the per column min/max values of the given data if not already present in the domain.
     *
     * @param data the data
     * @param exec the execution context
     * @param recalcValuesFor The columns
     * @return The data with extended domain information
     * @throws InvalidSettingsException ...
     * @throws CanceledExecutionException ...
     */
    public BufferedDataTable calcDomainBoundsIfNeccessary(final BufferedDataTable data, final ExecutionContext exec,
        final List<String> recalcValuesFor) throws InvalidSettingsException, CanceledExecutionException {

        if (null == recalcValuesFor || recalcValuesFor.isEmpty()) {
            return data;
        }
        List<Integer> valuesI = new ArrayList<Integer>();
        for (String colName : recalcValuesFor) {
            DataColumnSpec colSpec = data.getDataTableSpec().getColumnSpec(colName);
            if (!colSpec.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Can only process numeric " + "data. The column \""
                    + colSpec.getName() + "\" is not numeric.");
            }
            if (recalcValuesFor.contains(colName) && !colSpec.getDomain().hasBounds()) {
                valuesI.add(data.getDataTableSpec().findColumnIndex(colName));
            }
        }
        if (valuesI.isEmpty()) {
            return data;
        }
        Map<Integer, Double> min = new HashMap<Integer, Double>();
        Map<Integer, Double> max = new HashMap<Integer, Double>();
        for (int col : valuesI) {
            min.put(col, Double.MAX_VALUE);
            max.put(col, Double.MIN_VALUE);
        }
        int c = 0;
        for (DataRow row : data) {
            c++;
            exec.checkCanceled();
            exec.setProgress(c / (double)data.size());
            for (int col : valuesI) {
                double val = ((DoubleValue)row.getCell(col)).getDoubleValue();
                if (min.get(col) > val) {
                    min.put(col, val);
                }
                if (max.get(col) < val) {
                    min.put(col, val);
                }
            }
        }

        List<DataColumnSpec> newColSpecList = new ArrayList<DataColumnSpec>();
        int cc = 0;
        for (DataColumnSpec columnSpec : data.getDataTableSpec()) {
            if (recalcValuesFor.contains(columnSpec.getName())) {
                DataColumnSpecCreator specCreator = new DataColumnSpecCreator(columnSpec);
                DataColumnDomainCreator domainCreator =
                    new DataColumnDomainCreator(new DoubleCell(min.get(cc)), new DoubleCell(max.get(cc)));
                specCreator.setDomain(domainCreator.createDomain());
                DataColumnSpec newColSpec = specCreator.createSpec();
                newColSpecList.add(newColSpec);
            } else {
                newColSpecList.add(columnSpec);
            }
            cc++;
        }
        DataTableSpec spec = new DataTableSpec(newColSpecList.toArray(new DataColumnSpec[0]));
        BufferedDataTable newDataTable = exec.createSpecReplacerTable(data, spec);
        return newDataTable;
    }

    /**
     * @param spec The <code>DataTableSpec</code> of the input table.
     * @return The spec of the output.
     * @throws InvalidSettingsException If settings and spec given in the constructor are invalid.
     */
    public PortObjectSpec[] getOutputSpec(final DataTableSpec spec) throws InvalidSettingsException {
        init(spec);
        return new PortObjectSpec[]{m_tableOutSpec, m_pmmlOutSpec};
    }

    /** Initialize instance and check if settings are consistent. */
    private void init(final DataTableSpec inSpec) throws InvalidSettingsException {
        PMMLPreprocDiscretize op = createDisretizeOp(null);

        AutoBinnerApply applier = new AutoBinnerApply();
        m_tableOutSpec = applier.getOutputSpec(op, inSpec);
        m_pmmlOutSpec = new PMMLDiscretizePreprocPortObjectSpec(op);
    }

    /**
     * This formatted should not be changed, since it may result in a different output of the binning labels.
     */
    protected class BinnerNumberFormat {
        /**Constructor.*/
        protected BinnerNumberFormat() {
            // no op
        }

        /** for numbers less than 0.0001. */
        private final DecimalFormat m_smallFormat = new DecimalFormat("0.00E0", new DecimalFormatSymbols(Locale.US));

        /** in all other cases, use the default Java formatter. */
        private final NumberFormat m_defaultFormat = NumberFormat.getNumberInstance(Locale.US);

        /**
         * Formats the double to a string. It will use the following either the format <code>0.00E0</code> for numbers
         * less than 0.0001 or the default NumberFormat.
         *
         * @param d the double to format
         * @return the string representation of <code>d</code>
         */
        public String format(final double d) {
            if (m_settings.getAdvancedFormatting()) {
                return advancedFormat(d);
            } else {
                if (d == 0.0) {
                    return "0";
                }
                if (Double.isInfinite(d) || Double.isNaN(d)) {
                    return Double.toString(d);
                }
                NumberFormat format;
                double abs = Math.abs(d);
                if (abs < 0.0001) {
                    format = m_smallFormat;
                } else {
                    format = m_defaultFormat;
                }
                synchronized (format) {
                    return format.format(d);
                }
            }
        }

        /**
         * @param d the double to format
         * @return the formated value
         */
        public String advancedFormat(final double d) {
            BigDecimal bd = new BigDecimal(d);
            switch (m_settings.getPrecisionMode()) {
                case Decimal:
                    bd = bd.setScale(m_settings.getPrecision(), m_settings.getRoundingMode());
                    break;
                case Significant:
                    bd = bd.round(new MathContext(m_settings.getPrecision(), m_settings.getRoundingMode()));
                    break;
            }
            switch (m_settings.getOutputFormat()) {
                case Standard:
                    return bd.toString();
                case Plain:
                    return bd.toPlainString();
                case Engineering:
                    return bd.toEngineeringString();
                default:
                    return Double.toString(bd.doubleValue());
            }
        }

        /**
         * @return the smallFormat
         */
        public DecimalFormat getSmallFormat() {
            return m_smallFormat;
        }

        /**
         * @return the default format
         */
        public NumberFormat getDefaultFormat() {
            return m_defaultFormat;
        }
    }

}
