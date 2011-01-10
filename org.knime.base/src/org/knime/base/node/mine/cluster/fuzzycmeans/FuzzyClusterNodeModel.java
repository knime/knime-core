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
package org.knime.base.node.mine.cluster.fuzzycmeans;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.node.mine.cluster.PMMLClusterPortObject;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 * Generate a fuzzy c-means clustering using a fixed number of cluster centers.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */

public class FuzzyClusterNodeModel extends NodeModel {

    /**
     * Key for the Cluster Columns in the output DataTable.
     */
    public static final String CLUSTER_KEY = "cluster_";

    /**
     * Key for the Cluster Columns in the output DataTable.
     */
    public static final String NOISESPEC_KEY = "NoiseCluster";

    /**
     * Key to store the number of clusters in the settings.
     */
    public static final String NRCLUSTERS_KEY = "nrclusters";

    /**
     * Key to store the number of maximal iterations in the settings.
     */
    public static final String MAXITERATIONS_KEY = "maxiterations";

    /**
     * Key to store the fuzzifier in the settings.
     */
    public static final String FUZZIFIER_KEY = "fuzzifier";

    /**
     * Key to store the excluded column list in the settings.
     */
    public static final String INCLUDELIST_KEY = "exclude";

    /**
     * Key to store wheher a noise cluster is induced.
     */
    public static final String NOISE_KEY = "noise";

    /**
     * Key to store the delta value in the config.
     */
    public static final String DELTAVALUE_KEY = "delta";

    /**
     * Key to store the lambda value in the config.
     */
    public static final String LAMBDAVALUE_KEY = "lambda";

    /**
     * Key to store whether the clustering should be performed in memory
     * in the PredParams.
     */
    public static final String MEMORY_KEY = "memory";

    /**
     * Key to store whether cluster quality measures should be calculated.
     */
    public static final String MEASURES_KEY = "measures";

    /** Config key to keep all columns in include list. */
    public static final String CFGKEY_KEEPALL = "keep_all_columns";

    /** List contains the columns to include. */
    private ArrayList<String> m_list;

    private boolean m_keepAll = false;

    /**
     * The input port used here.
     */
    static final int INPORT = 0;

    /**
     * The output port used here. Contains the original rows with additional
     * cluster membership information.
     */
    static final int OUTPORT = 0;

    private static final int INITIAL_NR_CLUSTERS = 3;

    private static final int INITIAL_MAX_ITERATIONS = 99;

    private static final double INITIAL_FUZZIFIER = 2.0;

    private static final double MAXFUZZIFIER = 10;

    // information about the clusters
    private int m_nrClusters; // number of clusters to be used

    private double[][] m_clusters; // clusters generated by the algorithm

    /*
     * 'fuzzifier' controls how much the clusters can overlap
     */
    private double m_fuzzifier;

    /*
     * information controlling the algorithm's operation max number of
     * iterations
     */
    private int m_maxNrIterations;

     /*
     * Flag indicating whether a noise cluster is induced.
     */
    private boolean m_noise;

    /*
     * Flag indicating whether delta is given or should be updated
     * automatically.
     */
    private boolean m_calculateDelta;

    /*
     * The delta value of the noise cluster.
     */
    private double m_delta;

    /*
     * DataTableSpec of the input datatable.
     */
    private DataTableSpec m_spec;

    /*
     * Lambda value for automatic update process of noise delta.
     */
    private double m_lambda;

    /*
     * WithinClustusterVariations
     */
    private double[] m_withinClusterVariation = null;

    /*
     * Fuzzy Hypervolumnes
     */
    private double[] m_fuzzyHypervolumes = null;

    /*
     * Between Cluster Variation
     */
    private double m_betweenClusterVariation = Double.NaN;

    /*
     * Partition Coefficient
     */
    private double m_partitionCoefficient = Double.NaN;

    /*
     * Partition Entropy
     */
    private double m_partitionEntropy = Double.NaN;

    /*
     * Xie Beni Index
     */
    private double m_xieBeniIndex = Double.NaN;
    /*
     * The underlying fuzzy c-means algorithm.
     */
    private FCMAlgorithm m_fcmAlgo;

    /*
     * Object to calculate cluster quality measures
     */
    private FCMQualityMeasures m_fcmmeasures;

    /*
     * Flag indicating whether the clustering should be performed in memory.
     */
    private boolean m_memory;

    /*
     * Flag indicating whether cluster quality measures should be calculated.
     */
    private boolean m_measures;

    /**
     * Constructor, remember parent and initialize status.
     */
    public FuzzyClusterNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{
                BufferedDataTable.TYPE, PMMLClusterPortObject.TYPE});

        m_nrClusters = INITIAL_NR_CLUSTERS;
        m_maxNrIterations = INITIAL_MAX_ITERATIONS;
        m_fuzzifier = INITIAL_FUZZIFIER;
        m_noise = false;
        m_calculateDelta = false;
        m_delta = .2;
        m_memory = true;
        m_measures = true;
    }

    /**
     * Generate new clustering based on InputDataTable and specified number of
     * clusters. In the output table, you will find the datarow with
     * supplementary information about the membership to each cluster center.
     * OUTPORT = original datarows with cluster membership information
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable indata = (BufferedDataTable) inData[0];
        m_clusters = null;
        m_betweenClusterVariation = Double.NaN;
        m_withinClusterVariation = null;
        if (m_noise) {
            if (m_calculateDelta) {
                if (m_memory) {
                    m_fcmAlgo = new FCMAlgorithmMemory(m_nrClusters,
                            m_fuzzifier, m_calculateDelta, m_lambda);
                } else {
                    m_fcmAlgo = new FCMAlgorithm(m_nrClusters, m_fuzzifier,
                            m_calculateDelta, m_lambda);
                }
            } else {
                if (m_memory) {
                    m_fcmAlgo = new FCMAlgorithmMemory(m_nrClusters,
                            m_fuzzifier, m_calculateDelta, m_delta);
                } else {
                    m_fcmAlgo = new FCMAlgorithm(m_nrClusters, m_fuzzifier,
                            m_calculateDelta, m_delta);
                }
            }
        } else {
            if (m_memory) {
                m_fcmAlgo = new FCMAlgorithmMemory(m_nrClusters, m_fuzzifier);
            } else {
                m_fcmAlgo = new FCMAlgorithm(m_nrClusters, m_fuzzifier);
            }
        }

        int nrRows = indata.getRowCount();
        m_spec = indata.getDataTableSpec();
        int nrCols = m_spec.getNumColumns();

        List<String> learningCols = new LinkedList<String>();
        List<String> ignoreCols = new LinkedList<String>();
        // counter for included columns
        int z = 0;
        final int[] columns = new int[m_list.size()];
        for (int i = 0; i < nrCols; i++) {
            // if include does contain current column name
            String colname = m_spec.getColumnSpec(i).getName();
            if (m_list.contains(colname)) {
                columns[z] = i;
                z++;
                learningCols.add(colname);
            } else {
                ignoreCols.add(colname);
            }
        }
        PMMLPortObjectSpec pmmlspec =
                createPMMLPortObjectSpec(m_spec, learningCols);

        ColumnRearranger colre = new ColumnRearranger(m_spec);
        colre.keepOnly(columns);

        BufferedDataTable filteredtable =
                exec.createColumnRearrangeTable(indata, colre, exec);


        // get dimension of feature space
        int dimension = filteredtable.getDataTableSpec().getNumColumns();
        m_fcmAlgo.init(nrRows, dimension, filteredtable);

        // main loop - until clusters stop changing or maxNrIterations reached
        int currentIteration = 0;
        double totalchange = Double.MAX_VALUE;
        while ((totalchange > 1e-7)
                && (currentIteration < m_maxNrIterations)) {
            if (exec != null) {
                exec.checkCanceled();
                exec.setProgress((double)currentIteration
                        / (double)m_maxNrIterations, "Iteration "
                        + currentIteration
                        + " Total change of prototypes: " + totalchange);
            }
            totalchange = m_fcmAlgo.doOneIteration(exec);
            currentIteration++;
        } // while(!finished & nrIt<maxNrIt)

        if (m_measures) {
            double[][] data = null;
            if (m_fcmAlgo instanceof FCMAlgorithmMemory) {
                data = ((FCMAlgorithmMemory)m_fcmAlgo).getConvertedData();
            } else {
                data =
                        new double[nrRows][m_fcmAlgo.getDimension()];
                int curRow = 0;
                for (DataRow dRow : filteredtable) {
                    for (int j = 0; j < dRow.getNumCells(); j++) {
                        if (!(dRow.getCell(j).isMissing())) {
                            DoubleValue dv = (DoubleValue)dRow.getCell(j);
                            data[curRow][j] = dv.getDoubleValue();

                        } else {
                            data[curRow][j] = 0;
                        }
                    }
                    curRow++;
                }
            }
            m_fcmmeasures =
                    new FCMQualityMeasures(m_fcmAlgo.getClusterCentres(),
                            m_fcmAlgo.getweightMatrix(), data, m_fuzzifier);
        }

        ColumnRearranger colRearranger = new ColumnRearranger(m_spec);
        CellFactory membershipFac = new ClusterMembershipFactory(m_fcmAlgo);
        colRearranger.append(membershipFac);
        BufferedDataTable result =
                exec.createColumnRearrangeTable(indata, colRearranger, exec);

        // don't write out the noise cluster!
        double[][] clustercentres = m_fcmAlgo.getClusterCentres();
        if (m_noise) {
            double[][] cleaned = new double[clustercentres.length - 1][];
            for (int i = 0; i < cleaned.length; i++) {
                cleaned[i] = new double[clustercentres[i].length];
                System.arraycopy(clustercentres[i], 0, cleaned[i], 0,
                        clustercentres[i].length);
            }
            clustercentres = cleaned;
        }
        PMMLClusterPortObject pmmlcluster =
                new PMMLClusterPortObject(clustercentres, m_nrClusters,
                        pmmlspec);
        return new PortObject[]{result, pmmlcluster};
    } // end execute()

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        m_betweenClusterVariation = Double.NaN;
        m_withinClusterVariation = null;
        m_partitionCoefficient = Double.NaN;
        m_partitionEntropy = Double.NaN;
        m_xieBeniIndex = Double.NaN;
        m_fuzzyHypervolumes = null;
        m_fcmAlgo = null;
        m_clusters = null;
        m_fcmmeasures = null;
    }

    /**
     * Saves the number of Clusters and the maximum number of iterations in the
     * settings.
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(CFGKEY_KEEPALL, m_keepAll);
        settings.addInt(NRCLUSTERS_KEY, m_nrClusters);
        settings.addInt(MAXITERATIONS_KEY, m_maxNrIterations);
        settings.addDouble(FUZZIFIER_KEY, m_fuzzifier);
        if (m_list != null) {
            String[] templist = new String[m_list.size()];
            templist = m_list.toArray(templist);
            settings.addStringArray(INCLUDELIST_KEY, templist);
        }
        settings.addBoolean(NOISE_KEY, m_noise);
        if (!m_calculateDelta) {
            settings.addDouble(DELTAVALUE_KEY, m_delta);
            settings.addDouble(LAMBDAVALUE_KEY, -1);
        } else {
            settings.addDouble(LAMBDAVALUE_KEY, m_lambda);
            settings.addDouble(DELTAVALUE_KEY, -1);
        }
        settings.addBoolean(MEMORY_KEY, m_memory);
        settings.addBoolean(MEASURES_KEY, m_measures);
    }

    /**
     * Validates the number of Clusters and the maximum number of iterations in
     * the settings.
     *
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        int tempnrclusters = settings.getInt(NRCLUSTERS_KEY);
        if ((1 > tempnrclusters) || (tempnrclusters > 9999)) {
            throw new InvalidSettingsException(
                    "Value out of range for number of"
                            + " clusters, must be in [1,9999]");
        }

        int tempmaxiter = settings.getInt(MAXITERATIONS_KEY);
        if ((1 > tempmaxiter) || (tempmaxiter > 9999)) {
            throw new InvalidSettingsException("Value out of range "
                    + "for maximum number of iterations, must be in "
                    + "[1,9999]");
        }
        double tempfuzzifier = settings.getDouble(FUZZIFIER_KEY);
        if ((1 <= tempfuzzifier) && (tempfuzzifier > MAXFUZZIFIER)) {
            throw new InvalidSettingsException("Value out of range "
                    + "for fuzzifier, must be in " + "[>1,10]");
        }
        if (settings.containsKey(INCLUDELIST_KEY)) {
            // get list of included columns
            String[] columns = settings.getStringArray(INCLUDELIST_KEY);
            if (columns.length < 1) {
                throw new InvalidSettingsException("No attributes set to work"
                      + " on. Please check the second tab "
                          + "\'Used Attributes\' in the dialog");
            }
        }
    }

    /**
     * Loads the number of clusters and the maximum number of iterations from
     * the settings.
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_nrClusters = settings.getInt(NRCLUSTERS_KEY);
        m_maxNrIterations = settings.getInt(MAXITERATIONS_KEY);
        m_fuzzifier = settings.getDouble(FUZZIFIER_KEY);
        if (settings.containsKey(NOISE_KEY)) {
            boolean noise = settings.getBoolean(NOISE_KEY);
            m_noise = noise;
            if (settings.containsKey(DELTAVALUE_KEY)) {
                double delta = settings.getDouble(DELTAVALUE_KEY);
                if (delta > 0) {
                    m_delta = delta;
                } else {
                    m_calculateDelta = true;
                }
            }
            if (settings.containsKey(LAMBDAVALUE_KEY)) {
                double lambda = settings.getDouble(LAMBDAVALUE_KEY);
                if (lambda > 0) {
                    m_lambda = lambda;
                } else {
                    m_calculateDelta = false;
                }
            }
        }
        if (settings.containsKey(INCLUDELIST_KEY)) {
            // clear include column list
            m_list = new ArrayList<String>();
            // get list of included columns
            String[] columns = settings.getStringArray(INCLUDELIST_KEY, m_list
                    .toArray(new String[0]));
            for (int i = 0; i < columns.length; i++) {
                m_list.add(columns[i]);
            }
        }
        if (settings.containsKey(MEMORY_KEY)) {
            m_memory = settings.getBoolean(MEMORY_KEY);
        }
        if (settings.containsKey(MEASURES_KEY)) {
            m_measures = settings.getBoolean(MEASURES_KEY);
        }
        m_keepAll = settings.getBoolean(CFGKEY_KEEPALL, false);
    }

    /**
     * Number of columns in the output table is not deterministic.
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inspec = (DataTableSpec) inSpecs[0];
        if (m_keepAll || m_list == null) {
            m_list = new ArrayList<String>();
            for (DataColumnSpec colspec : inspec) {
                if (colspec.getType().isCompatible(DoubleValue.class)) {
                    m_list.add(colspec.getName());
                }
            }
            if (!m_keepAll) {
                setWarningMessage("List of columns to use has been set"
                    + " automatically, please check it in the dialog.");
            }
        }
        HashSet<String> listAsHash = new HashSet<String>(m_list);
        List<String> learningCols = new LinkedList<String>();
        for (int i = 0; i < inspec.getNumColumns(); i++) {
            String colname = inspec.getColumnSpec(i).getName();
            // if column is selected attribute
            if (listAsHash.remove(colname)) {
                learningCols.add(colname);
            }
        }
        if (!listAsHash.isEmpty()) {
            throw new InvalidSettingsException("Input table does not match "
                    + "selected columns, unable to find column(s): "
                    + listAsHash);
        }

        int nrCols = m_nrClusters + 1; // number of clusters + winner cluster
        if (m_noise) {
            nrCols++; // one column for the noise cluster
        }
        DataColumnSpec[] newSpec = new DataColumnSpec[nrCols];
        int cluster = 0;
        for (int j = 0; j < nrCols - 1; j++) {
            if (m_noise && j == (nrCols - 2)) {
                newSpec[j] = new DataColumnSpecCreator(
                        FuzzyClusterNodeModel.NOISESPEC_KEY, DoubleCell.TYPE)
                        .createSpec();
                break;
            }
            newSpec[j] = new DataColumnSpecCreator(
                    FuzzyClusterNodeModel.CLUSTER_KEY + cluster,
                    DoubleCell.TYPE).createSpec();
            cluster++;
        }
        newSpec[newSpec.length - 1] = new DataColumnSpecCreator(
                "Winner Cluster", StringCell.TYPE).createSpec();
        DataTableSpec newspec = new DataTableSpec(newSpec);
        DataTableSpec returnspec = new DataTableSpec(inspec, newspec);

        return new PortObjectSpec[]{returnspec,
                createPMMLPortObjectSpec(inspec, learningCols)};
    }

    private PMMLPortObjectSpec createPMMLPortObjectSpec(
            final DataTableSpec inspec, final List<String> learningCols)
        throws InvalidSettingsException {
        PMMLPortObjectSpecCreator pmmlSpecCreator =
            new PMMLPortObjectSpecCreator(
                    FilterColumnTable.createFilterTableSpec(inspec,
                            learningCols.toArray(
                                    new String[learningCols.size()])));
        pmmlSpecCreator.setLearningColsNames(learningCols);
        return pmmlSpecCreator.createSpec();
    }

    /**
     * @return the cluster centers as 2-dimensional double matrix
     */
    public double[][] getClusterCentres() {
        if (m_fcmAlgo != null) {
            return m_fcmAlgo.getClusterCentres();
        }
        if (m_clusters != null) {
            return m_clusters;
        }
        return null;
    }

    /**
     * @return the 2-dimensional weight matrix
     */
    public double[][] getweightMatrix() {
        if (m_fcmAlgo != null) {
            return m_fcmAlgo.getweightMatrix();
        }
        return null;
    }

    /**
     * Calculates the Between-Cluster Variation.
     *
     * @return the between cluster variation
     */
    public double getBetweenClusterVariation() {
        if (!Double.isNaN(m_betweenClusterVariation)) {
            return m_betweenClusterVariation;
        }
        if (m_fcmmeasures != null) {
            return m_fcmmeasures.getBetweenClusterVariation();
        }
        return Double.NaN;
    }

    /**
     * Calculates the partition coefficient.
     *
     * @return the partition coefficient
     */
    public double getPartitionCoefficient() {
        if (!Double.isNaN(m_partitionCoefficient)) {
            return m_partitionCoefficient;
        }
        if (m_fcmmeasures != null) {
            return m_fcmmeasures.getPartitionCoefficient();
        }
        return Double.NaN;
    }

    /**
     * Calculates the partition entropy.
     *
     * @return the partition entropy
     */
    public double getPartitionEntropy() {
        if (!Double.isNaN(m_partitionEntropy)) {
            return m_partitionEntropy;
        }
        if (m_fcmmeasures != null) {
            return m_fcmmeasures.getPartitionEntropy();
        }
        return Double.NaN;
    }

    /**
     * Calculates the Xie Beni Index.
     *
     * @return the Xie Beni Index
     */
    public double getXieBeniIndex() {
        if (!Double.isNaN(m_xieBeniIndex)) {
            return m_xieBeniIndex;
        }
        if (m_fcmmeasures != null) {
            return m_fcmmeasures.getXieBeniIndex();
        }
        return Double.NaN;
    }


    /**
     * Calculates the Within-Cluster Variation for each cluster. We take 'crisp'
     * cluster centers to determine the membership from a datarow to a cluster
     * center.
     *
     * @return withinClusterVariations
     */
    public double[] getWithinClusterVariations() {
        if (m_withinClusterVariation != null) {
            return m_withinClusterVariation;
        }
        if (m_fcmmeasures != null) {
            return m_fcmmeasures.getWithinClusterVariations();
        }
        return null;
    }

    /**
     * Calculates the fuzzy hypervolumnes for each cluster.
     *
     * @return fuzzy hypervolumnes of all clusters
     */
    public double[] getFuzzyHyperVolumes() {
        if (m_fuzzyHypervolumes != null) {
            return m_fuzzyHypervolumes;
        }
        if (m_fcmmeasures != null) {
            double[] fhypervolumnes = new double[m_nrClusters];
            for (int c = 0; c < m_nrClusters; c++) {
                fhypervolumnes[c] = m_fcmmeasures.getFuzzyHyperVolume(c);
            }
            return fhypervolumnes;
        }
        return null;
    }

    /**
     * @return flag indicating whether a noise clustering was performed
     */
    public boolean noiseClustering() {
        return m_noise;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, "FuzzyCMeans");
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
        int nrClusters = 0;
        int nrDimensions = 0;
        try {
            nrClusters = in.readInt();
            nrDimensions = in.readInt();
            m_clusters = new double[nrClusters][nrDimensions];
            for (int c = 0; c < nrClusters; c++) {
                for (int d = 0; d < nrDimensions; d++) {
                    m_clusters[c][d] = in.readDouble();
                }
            }
            double[] withinClusterVariation = new double[nrClusters];
            for (int c = 0; c < nrClusters; c++) {
                withinClusterVariation[c] = in.readDouble();
            }
            m_withinClusterVariation = withinClusterVariation;
            m_betweenClusterVariation = in.readDouble();
        } catch (EOFException eof) {
            // In the new version, these measures are only stored if
            // the dialog option 'compute quality measures' is checked.
        }
        try {
            m_partitionCoefficient = in.readDouble();
            m_partitionEntropy = in.readDouble();
            m_xieBeniIndex = in.readDouble();
            double[] fHyperVolumes = new double[nrClusters];
            for (int c = 0; c < nrClusters; c++) {
                fHyperVolumes[c] = in.readDouble();
            }
            m_fuzzyHypervolumes = fHyperVolumes;
        } catch (EOFException eof) {
            // older versions don't have the measures. In this case, the
            // measurements will be null and not shown in the view.
        }
        in.close();
        exec.setProgress(1.0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        double[][] clusters = null;
        if (m_fcmAlgo != null) {
            clusters = m_fcmAlgo.getClusterCentres();
        } else {
            clusters = m_clusters;
        }
        if (clusters == null) {
            assert (clusters != null);
            return;
        }
        File f = new File(internDir, "FuzzyCMeans");
        ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(f));

        int nrClusters = clusters.length;
        int nrDimensions = clusters[0].length;
        out.writeInt(nrClusters);
        out.writeInt(nrDimensions);
        for (int c = 0; c < nrClusters; c++) {
            for (int d = 0; d < nrDimensions; d++) {
                out.writeDouble(clusters[c][d]);
            }
        }
        if (getWithinClusterVariations() != null) {
            double[] withinClusterVar = getWithinClusterVariations();
            for (int c = 0; c < withinClusterVar.length; c++) {
                out.writeDouble(withinClusterVar[c]);
            }
        }
        if (!Double.isNaN(getBetweenClusterVariation())) {
            out.writeDouble(getBetweenClusterVariation());
        }
        if (!Double.isNaN(getPartitionCoefficient())) {
            out.writeDouble(getPartitionCoefficient());
        }
        if (!Double.isNaN(getPartitionEntropy())) {
            out.writeDouble(getPartitionEntropy());
        }
        if (!Double.isNaN(getXieBeniIndex())) {
            out.writeDouble(getXieBeniIndex());
        }
        if (getFuzzyHyperVolumes() != null) {
            double[] fuzzyHyperVol = getFuzzyHyperVolumes();
            for (int c = 0; c < fuzzyHyperVol.length; c++) {
                out.writeDouble(fuzzyHyperVol[c]);
            }
        }
        out.close();
        exec.setProgress(1.0);

    }
}
