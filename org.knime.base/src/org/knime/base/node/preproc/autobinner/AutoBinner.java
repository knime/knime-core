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
 *   12.07.2010 (hofer): created
 */
package org.knime.base.node.preproc.autobinner;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.autobinner.AutoBinnerLearnSettings.BinNaming;
import org.knime.base.node.preproc.autobinner.AutoBinnerLearnSettings.Method;
import org.knime.base.node.preproc.autobinner.apply.AutoBinnerApply;
import org.knime.base.node.preproc.autobinner.pmml.DisretizeConfiguration;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretize;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretizeBin;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretizePreprocPortObjectSpec;
import org.knime.base.node.preproc.autobinner.pmml.PMMLInterval;
import org.knime.base.node.preproc.autobinner.pmml.PMMLInterval.Closure;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretize;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
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

    /**
     * @param settings The settings object.
     * @throws InvalidSettingsException when settings are not consistent
     */
    public AutoBinner(final AutoBinnerLearnSettings settings)
            throws InvalidSettingsException {
        m_settings = settings;
    }


    /**
     * Determine bins.
     *
     * @param data the input data
     * @param exec the execution context
     * @return the operation with the discretisation information
     * @throws Exception
     */
    public PMMLPreprocDiscretize execute(
            final BufferedDataTable data, final ExecutionContext exec)
    throws Exception {
        // Auto configuration when target is not set
        if (null == m_settings.getTargetColumn() ||
                m_settings.getIncludeAll()) {
            addAllNumericCols(data.getDataTableSpec());
        }
        // determine intervals
        if (m_settings.getMethod().equals(Method.fixedNumber)) {
            BufferedDataTable inData = calcDomainBoundsIfNeccessary(data,
                    exec.createSubExecutionContext(0.9),
                    Arrays.asList(m_settings.getTargetColumn()));
            init(inData.getDataTableSpec());
            Map<String, double[]> edgesMap = new HashMap<String, double[]>();
            for (String target : m_settings.getTargetColumn()) {
                DataTableSpec inSpec = inData.getDataTableSpec();
                DataColumnSpec targetCol = inSpec.getColumnSpec(target);

                // bounds of the domain
                double min = ((DoubleValue)targetCol.getDomain()
                        .getLowerBound()).getDoubleValue();
                double max = ((DoubleValue)targetCol.getDomain()
                        .getUpperBound()).getDoubleValue();
                // the edges of the bins
                double[] edges = new double[m_settings.getBinCount() + 1];
                edges[0] = min;
                edges[edges.length - 1] = max;
                for (int i = 1; i < edges.length - 1; i++) {
                    edges[i] = min + i / (double) m_settings.getBinCount()
                                * (max - min);
                }
                edgesMap.put(target, edges);
            }
            return createDisretizeOp(edgesMap);

        } else if(m_settings.getMethod().equals(Method.sampleQuantiles)) {
            init(data.getDataTableSpec());
            boolean[] sortAsc =
                new boolean[m_settings.getTargetColumn().length];
            Arrays.fill(sortAsc, true);
            SortedTable sorted = new SortedTable(data,
                    Arrays.asList(m_settings.getTargetColumn()),
                    sortAsc,
                    exec.createSubExecutionContext(0.5));
            Map<String, double[]> edgesMap = createEdgesFromQuantiles(sorted,
                    exec.createSubExecutionContext(0.5));
            return createDisretizeOp(edgesMap);
        }
        else {
            throw new IllegalStateException("Unknown binning method.");
        }
    }

    private Map<String, double[]> createEdgesFromQuantiles(
            final SortedTable data, final ExecutionContext exec)
            throws CanceledExecutionException {
        Map<String, double[]> edgesMap = new HashMap<String, double[]>();
        for (String target : m_settings.getTargetColumn()) {
            double[] edges =
                new double[m_settings.getSampleQuantiles().length];
            edgesMap.put(target, edges);
        }
        Map<String, Integer> idx = new HashMap<String, Integer>();
        for (String target : m_settings.getTargetColumn()) {
            idx.put(target, data.getDataTableSpec().findColumnIndex(target));
        }
        int n = data.getRowCount();
        int c = 0;
        int cc = 0;
        RowIterator iter = data.iterator();
        DataRow rowQ = null;
        DataRow rowQ1 = null;
        if (iter.hasNext()) {
            rowQ1 = iter.next();
            rowQ = rowQ1;
        }

        for (double p : m_settings.getSampleQuantiles()) {
            double h = (n - 1) * p + 1;
            int q = (int)Math.floor(h);
            while ((1.0 == p || c < q) && iter.hasNext()) {
                rowQ = rowQ1;
                rowQ1 = iter.next();
                c++;
                exec.setProgress(c / (double) n);
                exec.checkCanceled();
            }
            rowQ = 1.0 != p ? rowQ : rowQ1;
            for (String target : m_settings.getTargetColumn()) {
                double xq = ((DoubleValue)
                        rowQ.getCell(idx.get(target))).getDoubleValue();
                double xq1 = ((DoubleValue)
                        rowQ1.getCell(idx.get(target))).getDoubleValue();
                double quantile = xq + (h - q) * (xq1 - xq);
                double[] edges = edgesMap.get(target);
                edges[cc] = quantile;
            }
            cc++;
        }
        return edgesMap;
    }

    private void addAllNumericCols(final DataTableSpec inSpec)
        throws InvalidSettingsException {
        List<String> numericCols = new ArrayList<String>();
        for (DataColumnSpec colSpec : inSpec) {
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                numericCols.add(colSpec.getName());

           }
        }
        if (!numericCols.isEmpty()) {
            m_settings.setTargetColumn(numericCols.toArray(new String[0]));
        } else {
            throw new InvalidSettingsException("No column in "
                    + "spec compatible to \"DoubleValue\".");
        }
    }

    private PMMLPreprocDiscretize createDisretizeOp(
            final Map<String, double[]> edgesMap) {
        Map<String, List<PMMLDiscretizeBin>> binMap =
            new HashMap<String, List<PMMLDiscretizeBin>>();

        binMap = createBins(edgesMap);

        List<String> names = new ArrayList<String>();
        Map<String, PMMLDiscretize> discretize =
            new HashMap<String, PMMLDiscretize>();
        for (String target : m_settings.getTargetColumn()) {
            String binnedCol = m_settings.getReplaceColumn() ? target
                    : target + " [Binned]";
            names.add(binnedCol);
            discretize.put(binnedCol, new PMMLDiscretize(target,
                    binMap.get(target)));
        }

        DisretizeConfiguration config = new DisretizeConfiguration(names,
                discretize);

        PMMLPreprocDiscretize op = new PMMLPreprocDiscretize(config);
        return op;
    }

    private Map<String, List<PMMLDiscretizeBin>> createBins(
            final Map<String, double[]> edgesMap) {
        Map<String, List<PMMLDiscretizeBin>> binMap =
            new HashMap<String, List<PMMLDiscretizeBin>>();
        for (String target : m_settings.getTargetColumn()) {
            if (null != edgesMap && null != edgesMap.get(target)
                    && edgesMap.get(target).length > 1) {
                double[] edges = edgesMap.get(target);
                // Names of the bins
                String[] binNames = new String[edges.length - 1];
                if (m_settings.getBinNaming().equals(BinNaming.numbered)) {
                    for (int i = 0; i < binNames.length; i++) {
                        binNames[i] = "Bin " + (i+1);
                    }
                } else { // BinNaming.edges
                    binNames[0] = "[" + BinnerNumberFormat.format(edges[0])
                        + "," + BinnerNumberFormat.format(edges[1]) + "]";
                    for (int i = 1; i < binNames.length; i++) {
                        binNames[i] = "(" + BinnerNumberFormat.format(edges[i])
                            + "," + BinnerNumberFormat.format(edges[i + 1])
                            + "]";
                    }
                }
                List<PMMLDiscretizeBin> bins =
                    new ArrayList<PMMLDiscretizeBin>();
                bins.add(new PMMLDiscretizeBin(binNames[0],
                        Arrays.asList(new PMMLInterval(edges[0], edges[1],
                                Closure.closedClosed))));
                for (int i = 1; i < binNames.length; i++) {
                    bins.add(new PMMLDiscretizeBin(binNames[i],
                            Arrays.asList(new PMMLInterval(edges[i], edges[i+1],
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
     * Determines the per column min/max values of the given data if not already
     * present in the domain.
     * @param data the data
     * @param exec the execution context
     * @param recalcValuesFor The columns
     * @return The data with extended domain information
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     */
    public BufferedDataTable calcDomainBoundsIfNeccessary(
            final BufferedDataTable data, final ExecutionContext exec,
            final List<String> recalcValuesFor)
            throws InvalidSettingsException, CanceledExecutionException {

        if (null == recalcValuesFor || recalcValuesFor.isEmpty()) {
            return data;
        }
        List<Integer> valuesI = new ArrayList<Integer>();
        for (String colName : recalcValuesFor) {
            DataColumnSpec colSpec = data.getDataTableSpec()
                                        .getColumnSpec(colName);
            if (!colSpec.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Can only process numeric "
                        + "data. The column \"" + colSpec.getName()
                        + "\" is not numeric.");
            }
            if (recalcValuesFor.contains(colName)
                    && !colSpec.getDomain().hasBounds()) {
                valuesI.add(data.getDataTableSpec().findColumnIndex(colName));
            }
        }
        if (valuesI.isEmpty()) {
            return data;
        }
        Map<Integer, Double> min =
            new HashMap<Integer, Double>();
        Map<Integer, Double> max =
            new HashMap<Integer, Double>();
        for (int col : valuesI) {
            min.put(col, Double.MAX_VALUE);
            max.put(col, Double.MIN_VALUE);
        }
        int c = 0;
        for (DataRow row : data) {
            c++;
            exec.checkCanceled();
            exec.setProgress(c / (double) data.getRowCount());
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
                DataColumnSpecCreator specCreator =
                    new DataColumnSpecCreator(columnSpec);
                DataColumnDomainCreator domainCreator =
                    new DataColumnDomainCreator(new DoubleCell(min.get(cc)),
                            new DoubleCell(max.get(cc)));
                specCreator.setDomain(domainCreator.createDomain());
                DataColumnSpec newColSpec = specCreator.createSpec();
                newColSpecList.add(newColSpec);
            } else {
                newColSpecList.add(columnSpec);
            }
            cc++;
        }
        DataTableSpec spec =
            new DataTableSpec(newColSpecList.toArray(new DataColumnSpec[0]));
        BufferedDataTable newDataTable =
            exec.createSpecReplacerTable(data, spec);
        return newDataTable;
    }

    /**
     * @param spec The <code>DataTableSpec</code> of the input table.
     * @return  The spec of the output.
     * @throws InvalidSettingsException If settings and spec given in
     * the constructor are invalid.
     */
    public PortObjectSpec[] getOutputSpec(final DataTableSpec spec)
      throws InvalidSettingsException {
        // Auto configuration when target is not set
        if (null == m_settings.getTargetColumn() ||
                m_settings.getIncludeAll()) {
            addAllNumericCols(spec);
        }
        init(spec);
        return new PortObjectSpec[] {m_tableOutSpec, m_pmmlOutSpec};
    }

    /** Initialize instance and check if settings are consistent. */
    private void init(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        PMMLPreprocDiscretize op = createDisretizeOp(null);

        AutoBinnerApply applier = new AutoBinnerApply();
        m_tableOutSpec = applier.getOutputSpec(op, inSpec);
        m_pmmlOutSpec = new PMMLDiscretizePreprocPortObjectSpec(op);
    }

    /**
     * Validates the settings in the passed <code>AutoBinnerLearnSettings</code>
     * object. The specified settings is checked for completeness and
     * consistency.
     *
     * @param settings The settings to validate.
     * @throws InvalidSettingsException If the validation of the settings
     *             failed.
     */
    public static void validateSettings(final AutoBinnerLearnSettings settings)
            throws InvalidSettingsException {

        String[] target = settings.getTargetColumn();
        if (target == null || target.length == 0) {
            throw new InvalidSettingsException("No target set.");
        }
    }

    /**
     * This formatted should not be changed, since it may result in a different
     * output of the binning labels.
     */
    private final static class BinnerNumberFormat {
        private BinnerNumberFormat() {
            // no op
        }

        /** for numbers less than 0.0001. */
        private static final DecimalFormat SMALL_FORMAT = new DecimalFormat(
                "0.00E0");
        /** in all other cases, use the default Java formatter. */
        private static final NumberFormat DEFAULT_FORMAT =
            NumberFormat.getNumberInstance();

        /**
         * Formats the double to a string. It will use the following either
         * the format <code>0.00E0</code> for numbers less than 0.0001 or
         * the default NumberFormat.
         * @param d the double to format
         * @return the string representation of <code>d</code>
         */
        public static String format(final double d) {
            if (d == 0.0 || Double.isInfinite(d) || Double.isNaN(d)) {
                return Double.toString(d);
            }
            NumberFormat format;
            double abs = Math.abs(d);
            if (abs < 0.0001) {
                format = SMALL_FORMAT;
            } else {
                format = DEFAULT_FORMAT;
            }
            synchronized (format) {
                return format.format(d);
            }
        }
    }

}
