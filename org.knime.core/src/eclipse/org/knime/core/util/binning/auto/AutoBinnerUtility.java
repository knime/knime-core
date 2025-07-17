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
 *   2018. jun. 6. (Mor Kalla): created
 */
package org.knime.core.util.binning.auto;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.dmg.pmml.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.util.Pair;
import org.knime.core.util.binning.auto.pmml.Closure;
import org.knime.core.util.binning.auto.pmml.DisretizeConfiguration;
import org.knime.core.util.binning.auto.pmml.PMMLDiscretize;
import org.knime.core.util.binning.auto.pmml.PMMLDiscretizeBin;
import org.knime.core.util.binning.auto.pmml.PMMLInterval;
import org.knime.core.util.binning.auto.pmml.PMMLPreprocDiscretize;
import org.knime.core.util.binning.numeric.Bin;
import org.knime.core.util.binning.numeric.NumericBin;
import org.knime.core.util.binning.numeric.PMMLBinningTranslator;

/**
 * Utility class for auto binner nodes.
 *
 * @author Mor Kalla
 * @since 3.6
 *
 * @deprecated Uses the outdated {@link PMMLPreprocDiscretize} class for binning, and the buggy {@link NumericBin}
 *             class. Instead look at {@link AutoBinningPMMLCreator}.
 */
@Deprecated
public final class AutoBinnerUtility {

    /**
     * This method translates a {@link PMMLPreprocDiscretize} object into {@link PMMLPortObject}.
     *
     * @param pmmlDiscretize {@link PMMLPreprocDiscretize} object
     * @param dataTableSpec {@link DataTableSpec} if incoming {@link BufferedDataTable}
     * @return a {@link PMMLPortObject} containing required parameters for binning operation
     */
    public static PMMLPortObject translate(final PMMLPreprocDiscretize pmmlDiscretize,
        final DataTableSpec dataTableSpec) {

        final Map<String, Bin[]> columnToBins = new HashMap<>();
        final Map<String, String> columnToAppend = new HashMap<>();

        final List<String> replacedColumnNames = pmmlDiscretize.getConfiguration().getNames();
        for (String replacedColumnName : replacedColumnNames) {
            final PMMLDiscretize discretize = pmmlDiscretize.getConfiguration().getDiscretize(replacedColumnName);
            final List<PMMLDiscretizeBin> bins = discretize.getBins();

            final String originalColumnName = discretize.getField();
            final boolean replaceColumnTheSame = replacedColumnName.equals(originalColumnName);
            columnToAppend.put(originalColumnName, replaceColumnTheSame ? null : replacedColumnName);

            final NumericBin[] numericBin =
                bins.stream().map(AutoBinnerUtility::getNumericBin).toArray(NumericBin[]::new);

            columnToBins.put(originalColumnName, numericBin);
        }

        final DataTableSpec newDataTableSpec = createNewDataTableSpec(dataTableSpec, columnToAppend);
        final PMMLPortObjectSpecCreator pmmlSpecCreator = new PMMLPortObjectSpecCreator(newDataTableSpec);
        final PMMLPortObject pmmlPortObject = new PMMLPortObject(pmmlSpecCreator.createSpec(), null, newDataTableSpec);
        final PMMLBinningTranslator trans =
            new PMMLBinningTranslator(columnToBins, columnToAppend, new DerivedFieldMapper(pmmlPortObject));
        final TransformationDictionary exportToTransDict = trans.exportToTransDict();
        pmmlPortObject.addGlobalTransformations(exportToTransDict);
        return pmmlPortObject;
    }

    /**
     * This method creates a new {@link DataTableSpec} holding columns which were appended.
     *
     * @param dataTableSpec Incoming {@link DataTableSpec}
     * @param columnToAppend Map of Strings containing name of target column as key and name of binned column which has
     *            to be appended as value. If target column is not be appended, value is null
     * @return {@link DataTableSpec} containing target and binned columns
     */
    public static DataTableSpec createNewDataTableSpec(final DataTableSpec dataTableSpec,
        final Map<String, String> columnToAppend) {
        final List<DataColumnSpec> origDataColumnSpecs = new LinkedList<>();
        for (int i = 0; i < dataTableSpec.getNumColumns(); i++) {
            origDataColumnSpecs.add(dataTableSpec.getColumnSpec(i));
        }
        final List<DataColumnSpec> addDataColumnSpecs = new LinkedList<>();
        for (Entry<String, String> entry : columnToAppend.entrySet()) {
            final String replacedColumnName = entry.getValue();
            if (replacedColumnName != null) {
                addDataColumnSpecs.add(new DataColumnSpecCreator(replacedColumnName, StringCell.TYPE).createSpec());
            }
        }
        origDataColumnSpecs.addAll(addDataColumnSpecs);
        final DataTableSpec newDataTableSpec =
            new DataTableSpec(origDataColumnSpecs.toArray(new DataColumnSpec[origDataColumnSpecs.size()]));
        return newDataTableSpec;
    }

    /**
     * This method creates a {@link PMMLPreprocDiscretize} object.
     *
     * @param settings {@link AutoBinnerLearnSettings} object from node model
     * @param exec the {@link ExecutionMonitor} object from the node model
     * @param maxAndMin the maximum and minimum values by the given column name
     * @param includeCols the include columns from the node model
     * @return a {@link PMMLPreprocDiscretize} object containing required parameters for binning operation
     * @throws SQLException if something happened during reading from the database
     * @throws IOException if something happened during reading from the database
     * @throws CanceledExecutionException if the user cancels the execution
     * @throws InvalidSettingsException if settings are invalid
     */
    public static PMMLPreprocDiscretize createPMMLPrepocDiscretize(final AutoBinnerLearnSettings settings,
        final ExecutionMonitor exec, final Map<String, Pair<Double, Double>> maxAndMin, final String[] includeCols)
        throws SQLException, CanceledExecutionException, IOException, InvalidSettingsException {

        if (includeCols.length == 0) {
            return createDisretizeOp(settings, new LinkedHashMap<>(), includeCols);
        }

        final Map<String, double[]> edgesMap = new LinkedHashMap<>();
        for (Entry<String, Pair<Double, Double>> entry : maxAndMin.entrySet()) {
            double[] edges =
                calculateBounds(settings.getBinCount(), entry.getValue().getFirst(), entry.getValue().getSecond());
            if (settings.getIntegerBounds()) {
                edges = toIntegerBoundaries(edges);
            }
            edgesMap.put(entry.getKey(), edges);
        }

        return createDisretizeOp(settings, edgesMap, includeCols);
    }

    private static PMMLPreprocDiscretize createDisretizeOp(final AutoBinnerLearnSettings settings,
        final Map<String, double[]> edgesMap, final String[] includeCols) {
        final Map<String, List<PMMLDiscretizeBin>> binMap = createBins(settings, edgesMap, includeCols);

        final List<String> names = new ArrayList<String>();
        final Map<String, PMMLDiscretize> discretize = new HashMap<String, PMMLDiscretize>();
        for (String target : includeCols) {
            final String binnedCol = settings.getReplaceColumn() ? target : target + " [Binned]";
            names.add(binnedCol);
            discretize.put(binnedCol, new PMMLDiscretize(target, binMap.get(target)));
        }

        final DisretizeConfiguration config = new DisretizeConfiguration(names, discretize);

        return new PMMLPreprocDiscretize(config);
    }

    private static Map<String, List<PMMLDiscretizeBin>> createBins(final AutoBinnerLearnSettings settings,
        final Map<String, double[]> edgesMap, final String[] includeCols) {
        final Map<String, List<PMMLDiscretizeBin>> binMap = new HashMap<String, List<PMMLDiscretizeBin>>();
        for (final String target : includeCols) {
            if (isTargetValid(edgesMap, target)) {
                final double[] edges = edgesMap.get(target);

                final String[] binNames;
                if (BinNaming.NUMBERED == settings.getBinNaming()) {
                    binNames = getNumberedBinNames(edges.length - 1);
                } else if (BinNaming.EDGES == settings.getBinNaming()) {
                    binNames = getEdgedBinNames(edges, settings);
                } else { // BinNaming.MIDPOINTS
                    binNames = getMidPointsBinNames(edges, settings);
                }
                final List<PMMLDiscretizeBin> bins = new ArrayList<PMMLDiscretizeBin>();
                bins.add(new PMMLDiscretizeBin(binNames[0],
                    Arrays.asList(new PMMLInterval(edges[0], edges[1], Closure.CLOSED_CLOSED))));
                for (int i = 1; i < binNames.length; i++) {
                    bins.add(new PMMLDiscretizeBin(binNames[i],
                        Arrays.asList(new PMMLInterval(edges[i], edges[i + 1], Closure.OPEN_CLOSED))));
                }
                binMap.put(target, bins);
            } else {
                binMap.put(target, new ArrayList<PMMLDiscretizeBin>());
            }
        }
        return binMap;
    }

    private static boolean isTargetValid(final Map<String, double[]> edgesMap, final String target) {
        return edgesMap != null && edgesMap.get(target) != null && edgesMap.get(target).length > 1;
    }

    private static String[] getNumberedBinNames(final int length) {
        final String[] binNames = new String[length];
        for (int i = 0; i < length; i++) {
            binNames[i] = "Bin " + (i + 1);
        }
        return binNames;
    }

    private static String[] getEdgedBinNames(final double[] edges, final AutoBinnerLearnSettings settings) {
        final String[] binNames = new String[edges.length - 1];
        binNames[0] = "[" + BinnerNumberFormatter.format(edges[0], settings) + ","
            + BinnerNumberFormatter.format(edges[1], settings) + "]";
        for (int i = 1; i < binNames.length; i++) {
            binNames[i] = "(" + BinnerNumberFormatter.format(edges[i], settings) + ","
                + BinnerNumberFormatter.format(edges[i + 1], settings) + "]";
        }
        return binNames;
    }

    private static String[] getMidPointsBinNames(final double[] edges, final AutoBinnerLearnSettings settings) {
        final String[] binNames = new String[edges.length - 1];
        binNames[0] = BinnerNumberFormatter.format((edges[1] - edges[0]) / 2 + edges[0], settings);
        for (int i = 1; i < binNames.length; i++) {
            binNames[i] = BinnerNumberFormatter.format((edges[i + 1] - edges[i]) / 2 + edges[i], settings);
        }
        return binNames;
    }

    private static NumericBin getNumericBin(final PMMLDiscretizeBin bin) {
        final String binName = bin.getBinValue();
        final List<PMMLInterval> intervals = bin.getIntervals();
        boolean leftOpen = false;
        boolean rightOpen = false;
        double leftMargin = 0;
        double rightMargin = 0;
        //always returns only one interval
        for (PMMLInterval interval : intervals) {
            final Closure closure = interval.getClosure();
            switch (closure) {
                case OPEN_CLOSED:
                    leftOpen = true;
                    rightOpen = false;
                    break;
                case OPEN_OPEN:
                    leftOpen = true;
                    rightOpen = true;
                    break;
                case CLOSED_OPEN:
                    leftOpen = false;
                    rightOpen = true;
                    break;
                case CLOSED_CLOSED:
                    leftOpen = false;
                    rightOpen = false;
                    break;
                default:
                    leftOpen = true;
                    rightOpen = false;
                    break;
            }
            leftMargin = interval.getLeftMargin();
            rightMargin = interval.getRightMargin();
        }

        return new NumericBin(binName, leftOpen, leftMargin, rightOpen, rightMargin);
    }

    private static double[] calculateBounds(final int binCount, final double min, final double max) {
        final double[] edges = new double[binCount + 1];
        edges[0] = min;
        edges[edges.length - 1] = max;
        for (int i = 1; i < edges.length - 1; i++) {
            edges[i] = min + i / (double)binCount * (max - min);
        }
        return edges;
    }

    private static double[] toIntegerBoundaries(final double[] boundaries) {
        final Set<Double> intBoundaries = new TreeSet<Double>();
        intBoundaries.add(Math.floor(boundaries[0]));
        for (int i = 1; i < boundaries.length; i++) {
            intBoundaries.add(Math.ceil(boundaries[i]));
        }
        final double[] newEdges = new double[intBoundaries.size()];
        int i = 0;
        for (Double edge : intBoundaries) {
            newEdges[i++] = edge;
        }
        return newEdges;
    }

    private AutoBinnerUtility() {
        throw new UnsupportedOperationException();
    }
}
