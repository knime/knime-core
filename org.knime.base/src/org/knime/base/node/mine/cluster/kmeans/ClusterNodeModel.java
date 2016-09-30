/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.cluster.kmeans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.knime.base.node.mine.cluster.PMMLClusterTranslator;
import org.knime.base.node.mine.cluster.PMMLClusterTranslator.ComparisonMeasure;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;

/**
 * Generate a clustering using a fixed number of cluster centers and the k-means
 * algorithm. Right now this works only on {@link DataTable}s holding
 * {@link org.knime.core.data.def.DoubleCell}s (or derivatives thereof).
 *
 * @author Michael Berthold, University of Konstanz
 */
public class ClusterNodeModel extends NodeModel {
    /** Constant for the RowKey generation and identification in the view. */
    public static final String CLUSTER = "cluster_";

    /** Constant for the initial number of clusters used in the dialog. */
    public static final int INITIAL_NR_CLUSTERS = 3;

    /** Constant for the initial number of iterations used in the dialog. */
    public static final int INITIAL_MAX_ITERATIONS = 99;

    /** Config key for the number of clusters. */
    public static final String CFG_NR_OF_CLUSTERS = "nrClusters";

    /** Config key for the maximal number of iterations. */
    public static final String CFG_MAX_ITERATIONS = "maxNrIterations";

    /** Config key for the enable hiliting setting.
     * @since 3.3 */
    public static final String CFG_ENABLE_HILITE = "enableHilite";

    /** Config key for the used columns. */
    public static final String CFG_COLUMNS = "cfgColmns";

    private static final String SETTINGS_FILE_NAME = "kMeansInternalSettings";

    private static final String CFG_COVERAGE = "clusterCoverage";

    private static final String CFG_DIMENSION = "dimensions";

    private static final String CFG_IGNORED_COLS = "ignoredColumns";

    private static final String CFG_CLUSTER = "kMeansCluster";

    private int m_dimension; // dimension of input space

    private int m_nrIgnoredColumns;

    private boolean[] m_ignoreColumn;

    // mapping from cluster to covering data point

    private final HiLiteTranslator m_translator = new HiLiteTranslator();

    private static final String CFG_FEATURE_NAMES = "FeatureNames";

    private static final String CFG_HILITEMAPPING = "HiLiteMapping";

    /*
     * Use settings models and dialog components.
     * Introduced a column filter.
     * 02.05.2007 Dill
     */
    private final SettingsModelIntegerBounded m_nrOfClusters
        = new SettingsModelIntegerBounded(CFG_NR_OF_CLUSTERS, INITIAL_NR_CLUSTERS, 1, Integer.MAX_VALUE);

    private final SettingsModelIntegerBounded m_nrMaxIterations
        = new SettingsModelIntegerBounded(CFG_MAX_ITERATIONS, INITIAL_MAX_ITERATIONS, 1, Integer.MAX_VALUE);

    private final SettingsModelFilterString m_usedColumns
        = new SettingsModelFilterString(CFG_COLUMNS);

    private final SettingsModelBoolean m_enableHilite = new SettingsModelBoolean(CFG_ENABLE_HILITE, false);

    private ClusterViewData m_viewData;

    private boolean m_pmmlInEnabled;
    private boolean m_outputCenters;

    /**
     * Constructor, remember parent and initialize status.
     */
    ClusterNodeModel() {
        this(true, false);
    }

    /**
     * Constructor, remember parent and initialize status.
     * @param pmmlInEnabled if true, the node has an input PMML port
     * @param outputCenters if true, the node has another output port for the cluster centers
     */
    ClusterNodeModel(final boolean pmmlInEnabled, final boolean outputCenters) {
        super(pmmlInEnabled ? new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE_OPTIONAL}
                                : new PortType[]{BufferedDataTable.TYPE},
            outputCenters ? new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE, PMMLPortObject.TYPE}
        : new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE});
        m_pmmlInEnabled = pmmlInEnabled;
        m_outputCenters = outputCenters;
    }


    /**
     * @return cluster centers' hilite handler
     */
    final HiLiteHandler getHiLiteHandler() {
        return m_translator.getFromHiLiteHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (outIndex == 1) {
            return m_translator.getFromHiLiteHandler();
        } else {
            return super.getOutHiLiteHandler(outIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        m_translator.removeAllToHiliteHandlers();
        m_translator.addToHiLiteHandler(hiLiteHdl);
    }


    /**
     * Appends to the given node settings the model specific configuration, that
     * are, the current settings (e.g. from the
     * {@link org.knime.core.node.NodeDialogPane}), as wells, the
     * {@link NodeModel} itself if applicable.
     * <p>
     * Method is called by the {@link org.knime.core.node.Node} if the
     * current configuration needs to be saved.
     *
     * @param settings to write into
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);
        m_nrOfClusters.saveSettingsTo(settings);
        m_nrMaxIterations.saveSettingsTo(settings);
        m_usedColumns.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
    }

    /**
     * Method is called when before the model has to change it's configuration
     * (@see loadsettings) using the given one. This method is also called
     * by the {@link org.knime.core.node.Node}.
     *
     * @param settings to validate
     * @throws InvalidSettingsException if a property is not available or
     *             doesn't fit
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        assert (settings != null);
        m_nrOfClusters.validateSettings(settings);
        m_nrMaxIterations.validateSettings(settings);
        // if exception is thrown -> catch it, and remember it
        // in configure set all numeric columns into includeList
        try {
            m_enableHilite.validateSettings(settings);
            m_usedColumns.validateSettings(settings);
        } catch (InvalidSettingsException ise) {
            // do nothing: problably an old workflow
        }
    }

    /**
     * Method is called when the {@link NodeModel} has to set its
     * configuration using the given one. This method is also called by the
     * {@link org.knime.core.node.Node}. Note that the settings should
     * have been validated before this method is called.
     *
     * @param settings to read from
     * @throws InvalidSettingsException if a property is not available - which shouldn't happen...
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        assert (settings != null);
        m_nrOfClusters.loadSettingsFrom(settings);
        m_nrMaxIterations.loadSettingsFrom(settings);
        m_dimension = m_usedColumns.getIncludeList().size() + m_usedColumns.getExcludeList().size();
        // added in 3.3
        if (settings.containsKey(CFG_ENABLE_HILITE)) {
            m_enableHilite.loadSettingsFrom(settings);
        } else {
            m_enableHilite.setBooleanValue(false);
        }
        try {
            m_usedColumns.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            // do nothing, probably an old workflow
        }
    }

    /**
     * Generate new clustering based on InputDataTable and specified number of
     * clusters. Currently the objective function only looks for cluster centers
     * that are extremely similar to the first n patterns...
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] data, final ExecutionContext exec) throws Exception {
        // FIXME actually do something useful with missing values!
        BufferedDataTable inData = (BufferedDataTable)data[0];
        DataTableSpec spec = inData.getDataTableSpec();
        // get dimension of feature space
        m_dimension = inData.getDataTableSpec().getNumColumns();
        HashMap<RowKey, Set<RowKey>> mapping = new HashMap<RowKey, Set<RowKey>>();
        addExcludeColumnsToIgnoreList(spec);
        double[][] clusters = initializeClusters(inData);

        // also keep counts of how many patterns fall in a specific cluster
        int[] clusterCoverage = new int[m_nrOfClusters.getIntValue()];

        // --------- create clusters --------------
        // reserve space for cluster center updates (do batch update!)
        double[][] delta = new double[m_nrOfClusters.getIntValue()][];
        for (int c = 0; c < m_nrOfClusters.getIntValue(); c++) {
            delta[c] = new double[m_dimension - m_nrIgnoredColumns];
        }

        // main loop - until clusters stop changing or maxNrIterations reached
        int currentIteration = 0;
        boolean finished = false;
        while ((!finished) && (currentIteration < m_nrMaxIterations.getIntValue())) {
            exec.checkCanceled();
            exec.setProgress((double)currentIteration / (double)m_nrMaxIterations.getIntValue(),
                                 "Iteration " + currentIteration);
            // initialize counts and cluster-deltas
            for (int c = 0; c < m_nrOfClusters.getIntValue(); c++) {
                clusterCoverage[c] = 0;
                delta[c] = new double[m_dimension - m_nrIgnoredColumns];
                int deltaPos = 0;
                for (int i = 0; i < m_dimension; i++) {
                    if (!m_ignoreColumn[i]) {
                        delta[c][deltaPos++] = 0.0;
                    }
                }
            }
            // assume that we are done (i.e. clusters have stopped changing)
            finished = true;
            RowIterator rowIt = inData.iterator(); // first training example
            while (rowIt.hasNext()) {
                DataRow currentRow = rowIt.next();
                int winner = findClosestPrototypeFor(currentRow, clusters);
                if (winner >= 0) {
                    // update winning cluster centers delta
                    int deltaPos = 0;
                    for (int i = 0; i < m_dimension; i++) {
                        DataCell currentCell = currentRow.getCell(i);
                        if (!m_ignoreColumn[i]) {
                            if (!currentCell.isMissing()) {
                                delta[winner][deltaPos] += ((DoubleValue)(currentCell)).getDoubleValue();
                            } else {
                                throw new Exception("Missing Values not (yet) allowed in k-Means.");
                            }
                            deltaPos++;
                        }
                    }
                    clusterCoverage[winner]++;
                } else {
                    // we didn't find any winner - very odd
                    assert (winner >= 0); // let's report this during
                    // debugging!
                    // otherwise just don't reproduce result
                    throw new IllegalStateException("No winner found: " + winner);
                }
            }
            // update cluster centers
            finished = updateClusterCenters(clusterCoverage, clusters, delta);
            currentIteration++;
        } // while(!finished & nrIt<maxNrIt)
        // create list of feature names
        int k = 0;  // index of not-ignored columns
        int j = 0;  // index of column
        String[] featureNames = new String[m_dimension];
        do {
            if (!m_ignoreColumn[j]) {
                featureNames[k] = spec.getColumnSpec(j).getName();
                k++;
            }
            j++;
        } while (j < m_dimension);
        // create output container and also mapping for HiLiteing
        BufferedDataContainer labeledInput = exec.createDataContainer(createAppendedSpec(spec));
        for (DataRow row : inData) {
            int winner = findClosestPrototypeFor(row, clusters);
            DataCell cell = new StringCell(CLUSTER + winner);
            labeledInput.addRowToTable(new AppendedColumnRow(row, cell));
            if (m_enableHilite.getBooleanValue()) {
                RowKey key = new RowKey(CLUSTER + winner);
                if (mapping.get(key) == null) {
                    Set<RowKey> set = new HashSet<RowKey>();
                    set.add(row.getKey());
                    mapping.put(key, set);
                } else {
                    mapping.get(key).add(row.getKey());
                }
            }
        }
        labeledInput.close();
        if (m_enableHilite.getBooleanValue()) {
            m_translator.setMapper(new DefaultHiLiteMapper(mapping));
        }
        BufferedDataTable outData = labeledInput.getTable();

        // handle the optional PMML input
        PMMLPortObject inPMMLPort = m_pmmlInEnabled ? (PMMLPortObject)data[1] : null;
        PMMLPortObjectSpec inPMMLSpec = null;
        if (inPMMLPort != null) {
            inPMMLSpec = inPMMLPort.getSpec();
        }
        PMMLPortObjectSpec pmmlOutSpec = createPMMLSpec(inPMMLSpec, spec);
        PMMLPortObject outPMMLPort = new PMMLPortObject(pmmlOutSpec, inPMMLPort, spec);
        Set<String> columns = new LinkedHashSet<String>();
        for (String s : pmmlOutSpec.getLearningFields()) {
            columns.add(s);
        }
        outPMMLPort.addModelTranslater(new PMMLClusterTranslator(ComparisonMeasure.squaredEuclidean,
                m_nrOfClusters.getIntValue(), clusters, clusterCoverage, columns));
        m_viewData = new ClusterViewData(clusters, clusterCoverage, m_dimension - m_nrIgnoredColumns, featureNames);

        if (m_outputCenters) {
            DataContainer clusterCenterContainer = exec.createDataContainer(createClusterCentersSpec(spec));
            int i = 0;
            for (double[] cluster : clusters) {
                List<DataCell> cells = new ArrayList<>();
                for (double d : cluster) {
                    cells.add(new DoubleCell(d));
                }
                clusterCenterContainer.addRowToTable(new DefaultRow(new RowKey(
                    PMMLClusterTranslator.CLUSTER_NAME_PREFIX + i++), cells));
            }
            clusterCenterContainer.close();
            return new PortObject[]{outData, (BufferedDataTable)clusterCenterContainer.getTable(), outPMMLPort};
        } else {
            return new PortObject[]{outData, outPMMLPort};
        }
     }

    private boolean updateClusterCenters(final int[] clusterCoverage,
                                        final double[][] clusters,
                                        final double[][] delta) {
        boolean finished = true;
        for (int c = 0; c < m_nrOfClusters.getIntValue(); c++) {
            if (clusterCoverage[c] > 0) {
                // only update clusters who do cover some pattern:
                int pos = 0;
                for (int i = 0; i < m_dimension; i++) {
                    if (m_ignoreColumn[i]) {
                        continue;
                    }
                    // normalize delta by nr of covered patterns
                    double newValue = delta[c][pos] / clusterCoverage[c];
                    // compare before assigning the value to make sure we
                    // don't stop if things have changed substantially
                    if (Math.abs(clusters[c][pos] - newValue) > 1e-10) {
                        finished = false;
                    }
                    clusters[c][pos] = newValue;
                    pos++;
                }
            }
        }
        return finished;
    }

    private double[][] initializeClusters(final DataTable input) {
        // initialize matrix of double (nr clusters * input dimension)
        double[][] clusters = new double[m_nrOfClusters.getIntValue()][];
        for (int c = 0; c < m_nrOfClusters.getIntValue(); c++) {
            clusters[c] = new double[m_dimension - m_nrIgnoredColumns];
        }
        // initialize cluster centers with values of first rows in table
        RowIterator rowIt = input.iterator();
        int c = 0;
        while (rowIt.hasNext() && c < m_nrOfClusters.getIntValue()) {
            DataRow currentRow = rowIt.next();
            int pos = 0;
            for (int i = 0; i < currentRow.getNumCells(); i++) {
                if (!m_ignoreColumn[i]) {
                    if (currentRow.getCell(i).isMissing()) {
                        clusters[c][pos] = 0;
                        // missing value: replace with zero
                    } else {
                        assert currentRow.getCell(i).getType().isCompatible(DoubleValue.class);
                        DoubleValue currentValue = (DoubleValue)currentRow.getCell(i);
                        clusters[c][pos] = currentValue.getDoubleValue();
                    }
                    pos++;
                }
            }
            c++;
        }
        return clusters;
    }

    private int findClosestPrototypeFor(final DataRow row, final double[][] clusters) {
        // find closest cluster center
        int winner = -1; // closest cluster so far
        double winnerDistance = Double.MAX_VALUE; // best distance
        for (int c = 0; c < m_nrOfClusters.getIntValue(); c++) {
            double distance = 0.0;
            int pos = 0;
            for (int i = 0; i < m_dimension; i++) {
                DataCell currentCell = row.getCell(i);
                if (!m_ignoreColumn[i]) {
                    if (!currentCell.isMissing()) {
                        assert currentCell.getType().isCompatible(DoubleValue.class);
                        double d = (clusters[c][pos] - ((DoubleValue)(currentCell)).getDoubleValue());
                        if (!Double.isNaN(d)) {
                            distance += d * d;
                        }
                    } else {
                        distance += 0.0; // missing
                    }
                    pos++;
                }
            }
            if (distance < winnerDistance) { // found closer cluster
                winner = c; // make it new winner
                winnerDistance = distance;
            }
        } // for all clusters (find closest one)
        return winner;
    }

    /**
     * Clears the model.
     *
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
        // remove the clusters
        m_viewData = null;
        m_translator.setMapper(null);
    }

    /**
     * Returns <code>true</code> always and passes the current input spec to
     * the output spec which is identical to the input specification - after
     * all, we are building cluster centers in the original feature space.
     *
     * @param inSpecs the specifications of the input port(s) - should be one
     * @return the copied input spec
     * @throws InvalidSettingsException if PMML incompatible type was found
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        DataTableSpec spec = (DataTableSpec)inSpecs[0];
        // input is output spec with all double compatible values set to
        // Double.
        m_dimension = spec.getNumColumns();
        // Find out which columns we can use (must be Double compatible)
        // Note that, for simplicity, we still use the entire dimensionality
        // for cluster prototypes below and simply ignore useless columns.
        m_ignoreColumn = new boolean[m_dimension];
        m_nrIgnoredColumns = 0;

        LinkedList<String> includes = new LinkedList<String>();
        includes.addAll(m_usedColumns.getIncludeList());

        LinkedList<String> excludes = new LinkedList<String>();
        excludes.addAll(m_usedColumns.getExcludeList());

        LinkedList<String> includes2 = new LinkedList<String>();
        includes2.addAll(m_usedColumns.getIncludeList());

        LinkedList<String> excludes2 = new LinkedList<String>();
        excludes2.addAll(m_usedColumns.getExcludeList());

        // We have to check if we have to update the included and excluded columns
        // First check if all incoming columns are either excluded or included
        for (String col : spec.getColumnNames()) {
            if (m_usedColumns.getIncludeList().contains(col)) {
                includes2.remove(col);
            } else if (m_usedColumns.getExcludeList().contains(col)) {
                excludes2.remove(col);
            } else {
                includes.add(col);
            }
        }

        //Leftover included columns that do not exist in the incoming table
        for (String col : includes2) {
            includes.remove(col);
        }
        // Same for excluded columns
        for (String col : excludes2) {
            excludes.remove(col);
        }
        m_usedColumns.setExcludeList(excludes);
        m_usedColumns.setIncludeList(includes);

        if (m_usedColumns.isKeepAllSelected()) {
            boolean hasNumericColumn = false;
            for (DataColumnSpec colSpec : spec) {
                if (colSpec.getType().isCompatible(DoubleValue.class)) {
                    hasNumericColumn = true;
                    break;
                }
            }
            if (!hasNumericColumn) {
                throw new InvalidSettingsException("No numeric columns in input");
            }
        } else {
            // if not configured yet fill include list with
            // double compatible columns
            if (m_usedColumns.getIncludeList().size() == 0 && m_usedColumns.getExcludeList().size() == 0) {
                List<String> includedColumns = new ArrayList<String>();
                List<String> excludedColumns = new ArrayList<String>();
                for (int i = 0; i < spec.getNumColumns(); i++) {
                    DataColumnSpec colSpec = spec.getColumnSpec(i);
                    if (colSpec.getType().isCompatible(DoubleValue.class)) {
                        includedColumns.add(colSpec.getName());
                    } else {
                        excludedColumns.add(colSpec.getName());
                    }
                }
                // set all double compatible columns as include list
                m_usedColumns.setIncludeList(includedColumns);
                m_usedColumns.setExcludeList(excludedColumns);
            }
            // check if some columns are included
            if (m_usedColumns.getIncludeList().size() <= 0) {
                setWarningMessage("No column in include list! Produces one huge cluster");
            }
        }

        addExcludeColumnsToIgnoreList(spec);
        DataTableSpec appendedSpec = createAppendedSpec(spec);
        // return spec for data and model outport!
        PMMLPortObjectSpec pmmlSpec;
        if (m_pmmlInEnabled) {
            pmmlSpec = (PMMLPortObjectSpec)inSpecs[1];
        } else {
            pmmlSpec = new PMMLPortObjectSpecCreator(spec).createSpec();
        }
        if (m_outputCenters) {
            return new PortObjectSpec[]{appendedSpec, createClusterCentersSpec(spec), createPMMLSpec(pmmlSpec, spec)};
        } else {
            return new PortObjectSpec[]{appendedSpec, createPMMLSpec(pmmlSpec, spec)};
        }
    }

    private DataTableSpec createClusterCentersSpec(final DataTableSpec spec) {
     // Create spec for cluster center table
        DataTableSpecCreator clusterCenterSpecCreator = new DataTableSpecCreator();
        for (int i = 0; i < m_dimension; i++) {
            if (!m_ignoreColumn[i]) {
                clusterCenterSpecCreator.addColumns(
                    new DataColumnSpecCreator(spec.getColumnSpec(i).getName(), DoubleCell.TYPE).createSpec());
            }
        }
        clusterCenterSpecCreator.dropAllDomains();
        return clusterCenterSpecCreator.createSpec();
    }

    private DataTableSpec createAppendedSpec(final DataTableSpec originalSpec) {
        // determine the possible values of the appended column
        DataCell[] possibleValues = new DataCell[m_nrOfClusters.getIntValue()];
        for (int i = 0; i < m_nrOfClusters.getIntValue(); i++) {
            DataCell key = new StringCell(CLUSTER + i);
            possibleValues[i] = key;
        }
        // create the domain
        // 1) guess an unused name for the new column (fixes bug #1022)
        String colNameGuess = "Cluster";
        int uniqueNr = 0;
        while (originalSpec.getColumnSpec(colNameGuess) != null) {
            uniqueNr++;
            colNameGuess = "Cluster_" + uniqueNr;
        }
        // 2) create spec
        DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(possibleValues);
        DataColumnSpecCreator creator = new DataColumnSpecCreator(colNameGuess, StringCell.TYPE);
        creator.setDomain(domainCreator.createDomain());
        // create the appended column spec
        DataColumnSpec labelColSpec = creator.createSpec();
        return new DataTableSpec(originalSpec, new DataTableSpec(labelColSpec));
    }

    private void addExcludeColumnsToIgnoreList(final DataTableSpec originalSpec) {
        // add all excluded columns to the ignore list
        m_ignoreColumn = new boolean[m_dimension];
        m_nrIgnoredColumns = 0;
        Collection<String> exclList = m_usedColumns.getExcludeList();
        for (int i = 0; i < m_dimension; i++) {
            DataColumnSpec col = originalSpec.getColumnSpec(i);
            // ignore if not compatible with double
            boolean ignore = !col.getType().isCompatible(DoubleValue.class);
            if (!ignore && !m_usedColumns.isKeepAllSelected()) {
                //  or if it is in the exclude list:
                ignore = exclList.contains(col.getName());
            }
            m_ignoreColumn[i] = ignore;
            if (ignore) {
                m_nrIgnoredColumns++;
            }
        }
    }

    private PMMLPortObjectSpec createPMMLSpec(final PMMLPortObjectSpec pmmlSpec, final DataTableSpec originalSpec)
            throws InvalidSettingsException {
        List<String> includes;
        if (m_usedColumns.isKeepAllSelected()) {
            includes = new ArrayList<String>();
            for (DataColumnSpec col : originalSpec) {
                if (col.getType().isCompatible(DoubleValue.class)) {
                    includes.add(col.getName());
                }
            }
        } else {
            includes = new ArrayList<String>();
            for (String s : m_usedColumns.getIncludeList()) {
                if (originalSpec.getColumnSpec(s).getType().isCompatible(DoubleValue.class)) {
                    includes.add(s);
                }
            }
        }
        HashSet<String> colNameHash = new HashSet<String>(includes);
        // the order in this list is important, need to use the order defined
        // by DTS, not m_usedColumns
        List<String> activeCols = new LinkedList<String>();
        for (DataColumnSpec colSpec : originalSpec) {
            String name = colSpec.getName();
            if (colNameHash.remove(name)) {
                activeCols.add(name);
            }
        }
        if (!colNameHash.isEmpty()) {
            throw new InvalidSettingsException("Input table does not match "
                    + "selected columns, unable to find column(s): " + colNameHash);
        }

        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(pmmlSpec, originalSpec);
        creator.setLearningColsNames(activeCols);
        return creator.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File settingsFile = new File(internDir, SETTINGS_FILE_NAME);
        FileInputStream in = new FileInputStream(settingsFile);
        NodeSettingsRO settings = NodeSettings.loadFromXML(in);
        try {
            m_dimension = settings.getInt(CFG_DIMENSION);
            m_nrIgnoredColumns = settings.getInt(CFG_IGNORED_COLS);
            int[] clusterCoverage = settings.getIntArray(CFG_COVERAGE);
            double[][] clusters = new double[m_nrOfClusters.getIntValue()][m_dimension];
            for (int i = 0; i < m_nrOfClusters.getIntValue(); i++) {
                clusters[i] = settings.getDoubleArray(CFG_CLUSTER + i);
            }
            // loading the HiLite Mapper is a new (v1.2) feature
            // ignore and set mapper to null if info is not available.
            // (fixes bug #1016)
            String[] featureNames = settings.getStringArray(CFG_FEATURE_NAMES);
            m_viewData = new ClusterViewData(clusters, clusterCoverage, m_dimension - m_nrIgnoredColumns, featureNames);
            if (m_enableHilite.getBooleanValue()) {
                NodeSettingsRO mapSet = settings.getNodeSettings(CFG_HILITEMAPPING);
                m_translator.setMapper(DefaultHiLiteMapper.load(mapSet));
            }
        } catch (InvalidSettingsException e) {
            throw new IOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        NodeSettings internalSettings = new NodeSettings("kMeans");
        internalSettings.addInt(CFG_DIMENSION, m_dimension);
        internalSettings.addInt(CFG_IGNORED_COLS, m_nrIgnoredColumns);
        internalSettings.addIntArray(CFG_COVERAGE, m_viewData.clusterCoverage());
        for (int i = 0; i < m_nrOfClusters.getIntValue(); i++) {
            internalSettings.addDoubleArray(CFG_CLUSTER + i, m_viewData.clusters()[i]);
        }
        internalSettings.addStringArray(CFG_FEATURE_NAMES, m_viewData.featureNames());
        if (m_enableHilite.getBooleanValue()) {
            NodeSettingsWO mapSet = internalSettings.addNodeSettings(CFG_HILITEMAPPING);
            ((DefaultHiLiteMapper) m_translator.getMapper()).save(mapSet);
        }
        File f = new File(internDir, SETTINGS_FILE_NAME);
        FileOutputStream out = new FileOutputStream(f);
        internalSettings.saveToXML(out);
    }

    ClusterViewData getViewData() {
        return m_viewData;
    }
}
