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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.mine.cluster.PMMLClusterHandler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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

    private double[][] m_clusters; // clusters generated by the algorithm

    private String[] m_featureNames;  // names of the cluster features
            // e.g. the names of the actually used columns

    private int[] m_clusterCoverage; // #patterns covered by each cluster


    // mapping from cluster to covering data point

    private final HiLiteTranslator m_translator = new HiLiteTranslator();

    private DataTableSpec m_spec;

//    private static final String CFG_PROTOTYPE = "prototype";

    private static final String CFG_FEATURE_NAMES = "FeatureNames";

    private static final String CFG_HILITEMAPPING = "HiLiteMapping";

    /*
     * Use settings models and dialog components.
     * Introduced a column filter.
     * 02.05.2007 Dill
     */
    private SettingsModelIntegerBounded m_nrOfClusters
        = new SettingsModelIntegerBounded(CFG_NR_OF_CLUSTERS,
                INITIAL_NR_CLUSTERS, 1, Integer.MAX_VALUE);

    private SettingsModelIntegerBounded m_nrMaxIterations
        = new SettingsModelIntegerBounded(CFG_MAX_ITERATIONS,
                INITIAL_MAX_ITERATIONS, 1, Integer.MAX_VALUE);

    private SettingsModelFilterString m_usedColumns
        = new SettingsModelFilterString(CFG_COLUMNS);

    /**
     * Constructor, remember parent and initialize status.
     */
    ClusterNodeModel() {
        super(new PortType[] {BufferedDataTable.TYPE},
                new PortType[] {
                    BufferedDataTable.TYPE,
                    PMMLPortObject.TYPE});
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        m_nrOfClusters.validateSettings(settings);
        m_nrMaxIterations.validateSettings(settings);
        // if exception is thrown -> catch it, and remember it
        // in configure set all numeric columns into includeList
        try {
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
     * @throws InvalidSettingsException if a property is not available - which
     *             shouldn't happen...
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        m_nrOfClusters.loadSettingsFrom(settings);
        m_nrMaxIterations.loadSettingsFrom(settings);
        m_dimension = m_usedColumns.getIncludeList().size()
            + m_usedColumns.getExcludeList().size();
        try {
            m_usedColumns.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            // do nothing, probably an old workflow
        }
    }

    /**
     * Get number of clusters.
     *
     * @return number of clusters
     */
    int getNumClusters() {
        return m_nrOfClusters.getIntValue();
    }

    /**
     * Set number of clusters.
     *
     * @param n number of clusters
     */
    void setNumClusters(final int n) {
        m_nrOfClusters.setIntValue(n);
    }

    /**
     * Get maximum number of iterations for batch mode.
     *
     * @return maximum number of iterations currently chosen
     */
    int getMaxNumIterations() {
        return m_nrMaxIterations.getIntValue();
    }

    /**
     * Set maximum number of iterations for batch mode.
     *
     * @param i maximum number of iterations
     */
    void setMaxNumIterations(final int i) {
        m_nrMaxIterations.setIntValue(i);
    }

    /**
     * Return dimension of feature space (and hence also clusters).
     *
     * @return dimension of feature space
     */
    int getDimension() {
        return m_dimension;
    }

    /**
     * @return the number of used columns
     */
    int getNrUsedColumns() {
        return m_dimension - m_nrIgnoredColumns;
    }

    /**
     * @return true if the model is executed (and not reset) and cluster centers
     *         are available
     */
    boolean hasModel() {
        return m_clusters != null;
    }

    /**
     * Return prototype vector of cluster c. Do not call if model is not
     * executed or reset.
     *
     * @param c index of cluster
     * @return array of doubles holding prototype vector
     */
    double[] getClusterCenter(final int c) {
        return m_clusters[c];
    }

    /** Return name of column at i'th postion within cluster prototype.
     *
     * @param i index of (double compatible = not ignored) feature
     * @return name
     */
    String getFeatureName(final int i) {
        if (m_featureNames == null) {
            return "??";
        }
        return m_featureNames[i];
    }

    /**
     * Return coverage of a cluster.
     *
     * @param c index of cluster
     * @return number of patterns covered by a cluster
     */
    int getClusterCoverage(final int c) {
        return m_clusterCoverage[c];
    }

    /**
     * Generate new clustering based on InputDataTable and specified number of
     * clusters. Currently the objective function only looks for cluster centers
     * that are extremely similar to the first n patterns...
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] data,
            final ExecutionContext exec) throws Exception {
        // FIXME actually do something useful with missing values!
        assert (data.length == 1);
        BufferedDataTable inData = (BufferedDataTable)data[0];
        m_spec = inData.getDataTableSpec();
        // get dimension of feature space
        m_dimension = inData.getDataTableSpec().getNumColumns();
        HashMap<RowKey, Set<RowKey>> mapping
          = new HashMap<RowKey, Set<RowKey>>();
        addExcludeColumnsToIgnoreList();
        initialize(inData);
        // --------- create clusters --------------
        // reserve space for cluster center updates (do batch update!)
        double[][] delta = new double[m_nrOfClusters.getIntValue()][];
        for (int c = 0; c < m_nrOfClusters.getIntValue(); c++) {
            delta[c] = new double[m_dimension - m_nrIgnoredColumns];
        }
        // also keep counts of how many patterns fall in a specific cluster
        m_clusterCoverage = new int[m_nrOfClusters.getIntValue()];
        // main loop - until clusters stop changing or maxNrIterations reached
        int currentIteration = 0;
        boolean finished = false;
        while ((!finished)
                && (currentIteration < m_nrMaxIterations.getIntValue())) {
            if (exec != null) {
                exec.checkCanceled();
                exec.setProgress((double)currentIteration
                        / (double)m_nrMaxIterations.getIntValue(), "Iteration "
                        + currentIteration);
            }
            // initialize counts and cluster-deltas
            for (int c = 0; c < m_nrOfClusters.getIntValue(); c++) {
                m_clusterCoverage[c] = 0;
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
            int nrOverallPatterns = 0;
            while (rowIt.hasNext()) {
                DataRow currentRow = rowIt.next();
                int winner = findClosestPrototypeFor(currentRow);
                if (winner >= 0) {
                    // update winning cluster centers delta
                    int deltaPos = 0;
                    for (int i = 0; i < m_dimension; i++) {
                        DataCell currentCell = currentRow.getCell(i);
                        if (!m_ignoreColumn[i]) {
                            if (!currentCell.isMissing()) {
                                delta[winner][deltaPos]
                                              += ((DoubleValue)(currentCell))
                                        .getDoubleValue();
                            } else {
                                throw new Exception("Missing Values not"
                                    + " (yet) allowed in k-Means.");
                            }
                            deltaPos++;
                        }
                    }
                    m_clusterCoverage[winner]++;
                } else {
                    // we didn't find any winner - very odd
                    assert (winner >= 0); // let's report this during
                    // debugging!
                    // otherwise just don't reproduce result
                    throw new IllegalStateException("No winner found: "
                            + winner);
                }
                nrOverallPatterns++;
            }
            // update cluster centers
            for (int c = 0; c < m_nrOfClusters.getIntValue(); c++) {
                if (m_clusterCoverage[c] > 0) {
                    // only update clusters who do cover some pattern:
                    int pos = 0;
                    for (int i = 0; i < m_dimension; i++) {
                        if (m_ignoreColumn[i]) {
                            continue;
                        }
                        // normalize delta by nr of covered patterns
                        double newValue = delta[c][pos] / m_clusterCoverage[c];
                        // compare before assigning the value to make sure we
                        // don't stop if things have changed substantially
                        if (Math.abs(m_clusters[c][pos] - newValue) > 1e-10) {
                            finished = false;
                        }
                        m_clusters[c][pos] = newValue;
                        pos++;
                    }
                }
            }
            currentIteration++;
        } // while(!finished & nrIt<maxNrIt)
        // create list of feature names
        int k = 0;  // index of not-ignored columns
        int j = 0;  // index of column
        m_featureNames = new String[m_dimension];
        do {
            if (!m_ignoreColumn[j]) {
                m_featureNames[k] = m_spec.getColumnSpec(j).getName();
                k++;
            }
            j++;
        } while (j < m_dimension);
        // create output container and also mapping for HiLiteing
        DataContainer labeledInput = new DataContainer(createAppendedSpec());
        for (DataRow row : inData) {
            int winner = findClosestPrototypeFor(row);
            DataCell cell = new StringCell(CLUSTER + winner);
            labeledInput.addRowToTable(new AppendedColumnRow(row, cell));
            RowKey key = new RowKey(CLUSTER + winner);
            if (mapping.get(key) == null) {
                Set<RowKey> set = new HashSet<RowKey>();
                set.add(row.getKey());
                mapping.put(key, set);
            } else {
                mapping.get(key).add(row.getKey());
            }
        }
        labeledInput.close();
        m_translator.setMapper(new DefaultHiLiteMapper(mapping));
        BufferedDataTable outData = exec.createBufferedDataTable(
                labeledInput.getTable(), exec);
        return new PortObject[]{outData, getPMMLOutPortObject()};
    }

    private void initialize(final DataTable input) {
        m_dimension = input.getDataTableSpec().getNumColumns();
        // initialize matrix of double (nr clusters * input dimension)
        m_clusters = new double[m_nrOfClusters.getIntValue()][];
        for (int c = 0; c < m_nrOfClusters.getIntValue(); c++) {
            m_clusters[c] = new double[m_dimension - m_nrIgnoredColumns];
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
                        m_clusters[c][pos] = 0;
                        // missing value: replace with zero
                    } else {
                        assert currentRow.getCell(i).getType().isCompatible(
                                DoubleValue.class);
                        DoubleValue currentValue = (DoubleValue)currentRow
                                .getCell(i);
                        m_clusters[c][pos] = currentValue.getDoubleValue();
                    }
                    pos++;
                }
            }
            c++;
        }
    }

    private int findClosestPrototypeFor(final DataRow row) {
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
                        assert currentCell.getType().isCompatible(
                                DoubleValue.class);
                        double d = (m_clusters[c][pos]
                                - ((DoubleValue)(currentCell))
                                .getDoubleValue());
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
        m_clusters = null;
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
        // make sure we are a 1-input
        assert (inSpecs.length == 1);
        m_spec = (DataTableSpec)inSpecs[0];
        // input is output spec with all double compatible values set to
        // Double.
        m_dimension = m_spec.getNumColumns();
        // Find out which columns we can use (must be Double compatible)
        // Note that, for simplicity, we still use the entire dimensionality
        // for cluster prototypes below and simply ignore useless columns.
        m_ignoreColumn = new boolean[m_dimension];
        m_nrIgnoredColumns = 0;

        if (m_usedColumns.isKeepAllSelected()) {
            boolean hasNumericColumn = false;
            for (DataColumnSpec colSpec : m_spec) {
                if (colSpec.getType().isCompatible(DoubleValue.class)) {
                    hasNumericColumn = true;
                    break;
                }
            }
            if (!hasNumericColumn) {
                throw new InvalidSettingsException(
                        "No numeric columns in input");
            }
        } else {
            // if not configured yet fill include list with
            // double compatible columns
            if (m_usedColumns.getIncludeList().size() == 0
                    && m_usedColumns.getExcludeList().size() == 0) {
                List<String> includedColumns = new ArrayList<String>();
                List<String> excludedColumns = new ArrayList<String>();
                for (int i = 0; i < m_spec.getNumColumns(); i++) {
                    DataColumnSpec colSpec = m_spec.getColumnSpec(i);
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
                setWarningMessage("No column in include list! "
                        + "Produces one huge cluster");
            }
        }
        addExcludeColumnsToIgnoreList();
        DataTableSpec appendedSpec = createAppendedSpec();
        // return spec for data and model outport!
        return new PortObjectSpec[]{appendedSpec, createPMMLSpec(m_spec)};
    }

    private DataTableSpec createAppendedSpec() {
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
        while (m_spec.getColumnSpec(colNameGuess) != null) {
            uniqueNr++;
            colNameGuess = "Cluster_" + uniqueNr;
        }
        // 2) create spec
        DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(
                possibleValues);
        DataColumnSpecCreator creator = new DataColumnSpecCreator(colNameGuess,
                StringCell.TYPE);
        creator.setDomain(domainCreator.createDomain());
        // create the appended column spec
        DataColumnSpec labelColSpec = creator.createSpec();
        DataTableSpec appendedSpec = new DataTableSpec(labelColSpec);
        return new DataTableSpec(m_spec, appendedSpec);
    }

    private void addExcludeColumnsToIgnoreList() {
        // add all excluded columns to the ignore list
        m_ignoreColumn = new boolean[m_dimension];
        m_nrIgnoredColumns = 0;
        Collection<String> exclList = m_usedColumns.getExcludeList();
        for (int i = 0; i < m_dimension; i++) {
            DataColumnSpec col = m_spec.getColumnSpec(i);
            // ignore if not compatible with double
            boolean ignore = !col.getType().isCompatible(DoubleValue.class);
            if (!ignore) {
                if (m_usedColumns.isKeepAllSelected()) {
                    // leave "ignore" untouched
                } else {
                    //  or if it is in the exclude list:
                    ignore = exclList.contains(col.getName());
                }
            }
            m_ignoreColumn[i] = ignore;
            if (ignore) {
                m_nrIgnoredColumns++;
            }
        }
    }

    private PMMLPortObject getPMMLOutPortObject() throws Exception {

    	  PMMLPortObjectSpec pmmlOutSpec 
          			= createPMMLSpec(m_spec);
    	  Set<String> columns = new HashSet<String>();
    	  for(String s: pmmlOutSpec.getLearningFields()){
    		  columns.add(s);
    	  }
          PMMLPortObject pmmlPort = new PMMLPortObject(pmmlOutSpec,
                  new PMMLClusterHandler(
                  		org.knime.base.node.mine.cluster.
                  		PMMLClusterHandler.ComparisonMeasure.squaredEuclidean, 
                  		m_nrOfClusters.getIntValue(), 
                  		m_clusters, 
                  		m_clusterCoverage, 
                  		columns));
          return pmmlPort;
    }

    private static List<DataColumnSpec> getColumnSpecsFor(
    		final List<String> colNames,
            final DataTableSpec tableSpec) {
        List<DataColumnSpec> colSpecs = new LinkedList<DataColumnSpec>();
        for (String colName : colNames) {
            DataColumnSpec colSpec = tableSpec.getColumnSpec(colName);
            if (colName == null) {
                throw new IllegalArgumentException(
                        "Column " + colName + " not found in data table spec!");
            }
            colSpecs.add(colSpec);
        }
        return colSpecs;
    }
    private PMMLPortObjectSpec createPMMLSpec(final DataTableSpec tableSpec)
            throws InvalidSettingsException {
        List<String> includes;
        if (m_usedColumns.isKeepAllSelected()) {
            includes = new ArrayList<String>();
            for (DataColumnSpec col : tableSpec) {
                if (col.getType().isCompatible(DoubleValue.class)) {
                    includes.add(col.getName());
                }
            }
        } else {
            includes = m_usedColumns.getIncludeList();
        }
        HashSet<String> colNameHash = new HashSet<String>(includes);
        // the order in this list is important, need to use the order defined
        // by DTS, not m_usedColumns
        List<String> activeCols = new LinkedList<String>();
        for (DataColumnSpec colSpec : tableSpec) {
            String name = colSpec.getName();
            if (colNameHash.remove(name)) {
                activeCols.add(name);
            }
        }
        if (!colNameHash.isEmpty()) {
            throw new InvalidSettingsException("Input table does not match "
                    + "selected columns, unable to find column(s): "
                    + colNameHash);
        }

        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(
                FilterColumnTable.createFilterTableSpec(tableSpec,
                        activeCols.toArray(new String[activeCols.size()])));
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
            m_clusterCoverage = settings.getIntArray(CFG_COVERAGE);
            m_clusters = new double[m_nrOfClusters.getIntValue()][m_dimension];
            for (int i = 0; i < m_nrOfClusters.getIntValue(); i++) {
                m_clusters[i] = settings.getDoubleArray(CFG_CLUSTER + i);
            }
            // loading the HiLite Mapper is a new (v1.2) feature
            // ignore and set mapper to null if info is not available.
            // (fixes bug #1016)
            m_featureNames = settings.getStringArray(CFG_FEATURE_NAMES);
            NodeSettingsRO mapSet = settings.getNodeSettings(
                    CFG_HILITEMAPPING);
            m_translator.setMapper(DefaultHiLiteMapper.load(mapSet));
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
        internalSettings.addIntArray(CFG_COVERAGE, m_clusterCoverage);
        for (int i = 0; i < m_nrOfClusters.getIntValue(); i++) {
            internalSettings.addDoubleArray(CFG_CLUSTER + i, m_clusters[i]);
        }
        internalSettings.addStringArray(CFG_FEATURE_NAMES, m_featureNames);
        NodeSettingsWO mapSet =
            internalSettings.addNodeSettings(CFG_HILITEMAPPING);
        ((DefaultHiLiteMapper) m_translator.getMapper()).save(mapSet);
        File f = new File(internDir, SETTINGS_FILE_NAME);
        FileOutputStream out = new FileOutputStream(f);
        internalSettings.saveToXML(out);
    }
}
