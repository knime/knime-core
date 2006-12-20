/*
 * ------------------------------------------------------------------- 
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.mine.cluster.hierarchical;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.knime.base.node.mine.cluster.hierarchical.distfunctions.DistanceFunction;
import org.knime.base.node.mine.cluster.hierarchical.distfunctions.EuclideanDist;
import org.knime.base.node.mine.cluster.hierarchical.distfunctions.ManhattanDist;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.dendrogram.ClusterNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
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

/**
 * Implements a Single Linkage Hirarchical Clustering.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class HierarchicalClusterNodeModel extends NodeModel implements
        DataProvider {

    /**
     * Different types of determination of the distance between two clusters.
     * 
     * @author Fabian Dill, University of Konstanz
     */
    public enum Linkage {
        /** Minimal distance between any two points of two clusters. */
        SINGLE,
        /** Average distance between all points of both clusters. */
        AVERAGE,
        /** Maximal distance between any two points of two clusters. */
        COMPLETE;
    }
    

    private static final String CFG_HCLUST = "hClust";

    private static final String CFG_H_CLUST_DATA = "hClustData";

    private static final String CFG_DIST_DATA = "distanceData";

    /**
     * Key to store the number of clusters for output in the settings.
     */
    public static final String NRCLUSTERS_KEY = "numberClusters";

    /**
     * Key to store the distance function in the settings.
     */
    public static final String DISTFUNCTION_KEY = "distFunction";

    /**
     * Key to store the linkage type in the settings.
     */
    public static final String LINKAGETYPE_KEY = "linkageType";

    /**
     * Specifies the mode the distance between two clusters is calculated.
     */
    private Linkage m_linkageType = Linkage.SINGLE;

    /**
     * Specifies the number clusters when the output table should be generated.
     */
    private int m_numClustersForOutput = 3;

    /**
     * The distance function to use.
     */
    private DistanceFunction.Names m_distFunction 
        = DistanceFunction.Names.Euclidean;

    private DataArray m_dataArray;

    private ClusterNode m_rootNode;

    private DataArray m_fusionTable;

    /**
     * Creates a new hierarchical clustering model.
     */
    public HierarchicalClusterNodeModel() {
        super(1, 1);

    }

    /**
     * @see org.knime.base.node.viz.plotter.DataProvider# getDataArray(int)
     */
    public DataArray getDataArray(final int index) {
        if (index == 0) {
            return m_fusionTable;
        }
        return m_dataArray;
    }

    /**
     * 
     * @return the root node of the cluster hierarchie.
     */
    public ClusterNode getRootNode() {
        return m_rootNode;
    }

    /**
     * Executes the algorithm. The output data table has then clusterd entries.
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws Exception {

        BufferedDataTable inputData = data[0];
        DataTable outputData = null;

        // generate initial clustering
        // which means that every data point is one cluster
        Vector<ClusterNode> clusters = initClusters(inputData, exec);
        List<ClusterNode> clusterStorage = new ArrayList<ClusterNode>();
        clusterStorage.addAll(clusters);
        // store the distance per each fusion step
        DataContainer fusionCont = new DataContainer(createFusionSpec());
        int iterationStep = 0;

        while (clusters.size() > 1) {
            exec.checkCanceled();
            // checks if number clusters to generate output table is reached
            if (m_numClustersForOutput == clusters.size()) {

                outputData = createResultTable(inputData, clusters, exec);
            }
            iterationStep++;

            // calculate distance between all clusters
            double currentSmallestDist = Double.MAX_VALUE;
            ClusterNode currentClosestCluster1 = null;
            ClusterNode currentClosestCluster2 = null;
            double dist = 0;
            for (int i = 0; i < clusters.size(); i++) {
                ClusterNode node1 = clusters.get(i);
                for (int j = i + 1; j < clusters.size(); j++) {
                    ClusterNode node2 = clusters.get(j);
                    // call the choosen function to calculate the distance
                    // between two clusters. At the moment is single linkage
                    // and average linkage supported.
                    if (m_linkageType.equals(Linkage.SINGLE)) {
                        dist = calculateSingleLinkageDist(node1, node2);
                    } else if (m_linkageType.equals(Linkage.AVERAGE)) {
                        dist = calculateAverageLinkageDist(node1, node2);
                    } else {
                        dist = calculateCompleteLinkageDist(node1, node2);
                    }
                    if (dist < currentSmallestDist) {
                        currentClosestCluster1 = node1;
                        currentClosestCluster2 = node2;
                        currentSmallestDist = dist;
                    }
                }
            }
            // make one cluster of the two closest
            ClusterNode newNode =
                    new ClusterNode(currentClosestCluster1,
                            currentClosestCluster2, currentSmallestDist);
            clusterStorage.add(newNode);
            clusters.remove(currentClosestCluster1);
            clusters.remove(currentClosestCluster2);

            clusters.add(newNode);

            // store the distance per each fusion step
            fusionCont.addRowToTable(new DefaultRow(
            // row key
                    new IntCell(clusters.size()),
                    // x-axis scatter plotter
                    new IntCell(clusters.size()),
                    // y-axis scatter plotter
                    new DoubleCell(newNode.getDist())));

            // // print number clusters and their data points
            // LOGGER.debug("Iteration " + iterationStep + ":");
            // LOGGER.debug(" Number Clusters: " + clusters.size());
            // printClustersDataRows(clusters);

        }
        m_rootNode = clusters.get(0);
        m_dataArray =
                new DefaultDataArray(outputData, 1, inputData.getRowCount());
        fusionCont.close();
        m_fusionTable =
                new DefaultDataArray(fusionCont.getTable(), 1, iterationStep);
        return new BufferedDataTable[]{exec.createBufferedDataTable(outputData,
                exec)};
    }

    private DataTableSpec createFusionSpec() {
        DataColumnSpecCreator creatorX =
                new DataColumnSpecCreator("Nr. of Clusters", IntCell.TYPE);
        DataColumnSpecCreator creatorY =
                new DataColumnSpecCreator("Distance", DoubleCell.TYPE);
        DataTableSpec spec =
                new DataTableSpec(creatorX.createSpec(), creatorY.createSpec());
        return spec;
    }

    /**
     * Resets all internal data.
     */
    @Override
    public final void reset() {
        m_dataArray = null;
        m_rootNode = null;
        m_fusionTable = null;
    }

    /*
     * Calculates the distance via the single linkage paradigm. That means two
     * clusters have the distance of its closest data rows
     * 
     */
    private double calculateSingleLinkageDist(final ClusterNode node1,
            final ClusterNode node2) {
        DataRow[] rowsOfCluster1 = node1.getAllDataRows();
        DataRow[] rowsOfCluster2 = node2.getAllDataRows();
        double smallestDist = Double.MAX_VALUE;
        DistanceFunction distFunction;  
        if (m_distFunction.equals(DistanceFunction.Names.Manhattan)) {
            distFunction = ManhattanDist.MANHATTEN_DISTANCE;
        } else {
            distFunction = EuclideanDist.EUCLIDEAN_DISTANCE;
        }
        for (int i = 0; i < rowsOfCluster1.length; i++) {
            for (int j = 0; j < rowsOfCluster2.length; j++) {
                double dist =
                        distFunction.calcDistance(rowsOfCluster1[i],
                                rowsOfCluster2[j]);
                if (dist < smallestDist) {
                    smallestDist = dist;
                }
            }
        }
        return smallestDist;
    }

    /*
     * Calculates the distance via the complete linkage paradigm. That means two
     * clusters have the distance of its farest data rows
     * 
     */
    private double calculateCompleteLinkageDist(final ClusterNode node1,
            final ClusterNode node2) {
        DataRow[] rowsOfCluster1 = node1.getAllDataRows();
        DataRow[] rowsOfCluster2 = node2.getAllDataRows();
        double maxDist = Double.MIN_VALUE;
        DistanceFunction distFunction;  
        if (m_distFunction.equals(DistanceFunction.Names.Manhattan)) {
            distFunction = ManhattanDist.MANHATTEN_DISTANCE;
        } else {
            distFunction = EuclideanDist.EUCLIDEAN_DISTANCE;
        }
        for (int i = 0; i < rowsOfCluster1.length; i++) {
            for (int j = 0; j < rowsOfCluster2.length; j++) {
                double dist =
                        distFunction.calcDistance(rowsOfCluster1[i],
                                rowsOfCluster2[j]);
                maxDist = Math.max(dist, maxDist);
            }
        }
        return maxDist;
    }

    /*
     * Calculates the distance via the average linkage paradigm. That means two
     * clusters have the distance of the average distance of all their member
     * data rows.
     */
    private double calculateAverageLinkageDist(final ClusterNode node1,
            final ClusterNode node2) {
        DataRow[] rowsOfCluster1 = node1.getAllDataRows();
        DataRow[] rowsOfCluster2 = node2.getAllDataRows();
        double sumDist = 0;
        DistanceFunction distFunction;  
        if (m_distFunction.equals(DistanceFunction.Names.Manhattan)) {
            distFunction = ManhattanDist.MANHATTEN_DISTANCE;
        } else {
            distFunction = EuclideanDist.EUCLIDEAN_DISTANCE;
        }
        for (int i = 0; i < rowsOfCluster1.length; i++) {
            for (int j = 0; j < rowsOfCluster2.length; j++) {
                sumDist +=
                        distFunction.calcDistance(rowsOfCluster1[i],
                                rowsOfCluster2[j]);
            }
        }
        // divide by the number pairewise distances
        return sumDist / (rowsOfCluster1.length * rowsOfCluster2.length);
    }

    /**
     * Creates number of data rows clusters as initial clustering.
     * 
     * @param inputData the input data rows
     * @param exec to check for user cancelations
     * 
     * @throws CanceledExecutionException if user canceled
     * 
     * @return the vector with all initial clusters.
     */
    private Vector<ClusterNode> initClusters(final DataTable inputData,
            final ExecutionContext exec) throws CanceledExecutionException {
        Vector<ClusterNode> rowVector = new Vector<ClusterNode>();
        int rowIdx = 0;
        for (DataRow row : inputData) {
            rowVector.add(new ClusterNode(row, rowIdx++));
            exec.checkCanceled();
        }
        return rowVector;
    }

    /**
     * Creates a standard table as the result table. The result table is
     * constructed for the desired number of clusters.
     * 
     * @param inputData the input data table which has the meta information like
     *            column names classes, and so on
     * 
     * @param clusters the vector with the clusters
     * @param exec to check for user cancelations
     * @return the result data table which contains the data rows with the class
     *         information
     * @throws CanceledExecutionException if user canceled
     */
    private DataTable createResultTable(final DataTable inputData,
            final Vector<ClusterNode> clusters, final ExecutionContext exec)
            throws CanceledExecutionException {
        DataTableSpec inputSpec = inputData.getDataTableSpec();
        DataTableSpec outputSpec = generateOutSpec(inputSpec);

        DataContainer resultTable = exec.createDataContainer(outputSpec);
        for (int i = 0; i < clusters.size(); i++) {
            DataRow[] memberRows = clusters.get(i).getAllDataRows();
            for (DataRow dataRow : memberRows) {
                DataCell[] cells = new DataCell[dataRow.getNumCells() + 1];
                for (int j = 0; j < dataRow.getNumCells(); j++) {
                    cells[j] = dataRow.getCell(j);
                }

                // append the cluster id the row belongs to
                cells[cells.length - 1] = new StringCell("cluster_" + i);
                resultTable.addRowToTable(
                        new DefaultRow(dataRow.getKey(), cells));
                
                exec.checkCanceled();
            }

        }

        resultTable.close();
        return resultTable.getTable();
    }

    /**
     * @see org.knime.core.node.NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check the range of the cluster number
        if (m_numClustersForOutput < 1) {
            throw new InvalidSettingsException(
                    "Number of output clusters must be greater than 0.");
        }
        if ((!m_linkageType.equals(Linkage.SINGLE))
                && (!m_linkageType.equals(Linkage.AVERAGE))
                && (!m_linkageType.equals(Linkage.COMPLETE))) {
            throw new InvalidSettingsException("Linkage Type must either be "
                    + Linkage.SINGLE + ", " + Linkage.AVERAGE + " or "
                    + Linkage.COMPLETE);
        }
        return new DataTableSpec[]{generateOutSpec(inSpecs[0])};
    }

    /**
     * Load validated settings in the model.
     * 
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_numClustersForOutput = settings.getInt(NRCLUSTERS_KEY);
        m_distFunction = DistanceFunction.Names.valueOf(
                settings.getString(DISTFUNCTION_KEY));
        m_linkageType = Linkage.valueOf(settings.getString(LINKAGETYPE_KEY));
    }

    /**
     * Saves the settings from the <code>HierarchicalClusterNodeModel</code>.
     * <ul>
     * <li> Number of clusters for output</li>
     * <li> The class of the distance function</li>
     * <li> Linkage Type</li>
     * </ul>
     * 
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(NRCLUSTERS_KEY, m_numClustersForOutput);
        settings.addString(DISTFUNCTION_KEY, m_distFunction.name());
        settings.addString(LINKAGETYPE_KEY, m_linkageType.name());
    }

    /**
     * Settings are validated.
     * <ul>
     * <li>Number of clusters for output must be greater than 0</li>
     * <li>A distance function object is instanced</li>
     * <li>The linkage type must either be <code>SINGLE_LINKAGE</code> or
     * <code>AVERAGE_LINKAGE</code></li>
     * </ul>
     * 
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        int numClustersForOutput = settings.getInt(NRCLUSTERS_KEY);
        // check the range of the cluster number
        if (numClustersForOutput <= 0) {
            throw new InvalidSettingsException(
                    "Number of output clusters must be greater than 0.");
        }

        // check distance function
        String dist = settings.getString(DISTFUNCTION_KEY);
        DistanceFunction.Names.valueOf(dist);


        // check linkage methode
        String linkageType = settings.getString(LINKAGETYPE_KEY);
        boolean valid = false;
        for (Linkage link : Linkage.values()) {
            if (link.name().equals(linkageType)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new InvalidSettingsException("Linkage Type must either be "
                    + Linkage.SINGLE + ", " + Linkage.AVERAGE + " or "
                    + Linkage.COMPLETE);
        }
    }

    /** Generate output spec based on input spec (appends column). */
    private static DataTableSpec generateOutSpec(final DataTableSpec inSpec) {
        int oldColCount = inSpec.getNumColumns();
        int colCount = oldColCount + 1;
        DataColumnSpec[] colSpecs = new DataColumnSpec[colCount];
        for (int i = 0; i < oldColCount; i++) {
            colSpecs[i] = inSpec.getColumnSpec(i);
        }
        // the additional column contains the cluster information
        DataColumnSpecCreator colspeccreator =
                new DataColumnSpecCreator("Cluster", StringCell.TYPE);
        colSpecs[oldColCount] = colspeccreator.createSpec();
        return new DataTableSpec(colSpecs);
    }

    /**
     * @see org.knime.core.node. NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // distances
        File distFile = new File(nodeInternDir, CFG_DIST_DATA);
        ContainerTable table1 = DataContainer.readFromZip(distFile);
        m_fusionTable = new DefaultDataArray(table1, 1, table1.getRowCount());
        // data rows
        File dataFile = new File(nodeInternDir, CFG_H_CLUST_DATA);
        ContainerTable table2 = DataContainer.readFromZip(dataFile);
        m_dataArray = new DefaultDataArray(table2, 1, table2.getRowCount());

        File f = new File(nodeInternDir, CFG_HCLUST);
        FileInputStream fis = new FileInputStream(f);
        NodeSettingsRO settings = NodeSettings.loadFromXML(fis);
        try {
            m_rootNode = ClusterNode.loadFromXML(settings, m_dataArray);
        } catch (InvalidSettingsException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * @see org.knime.core.node. NodeModel#saveInternals(File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File dataFile = new File(nodeInternDir, CFG_H_CLUST_DATA);
        DataContainer.writeToZip(m_dataArray, dataFile, exec);
        File distFile = new File(nodeInternDir, CFG_DIST_DATA);
        DataContainer.writeToZip(m_fusionTable, distFile, exec);
        NodeSettings settings = new NodeSettings(CFG_HCLUST);
        m_rootNode.saveToXML(settings);
        File f = new File(nodeInternDir, CFG_HCLUST);
        FileOutputStream fos = new FileOutputStream(f);
        settings.saveToXML(fos);
    }

}
