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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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

import org.knime.base.node.mine.cluster.hierarchical.distfunctions.DistanceFunction;
import org.knime.base.node.mine.cluster.hierarchical.distfunctions.EuclideanDist;
import org.knime.base.node.mine.cluster.hierarchical.distfunctions.ManhattanDist;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.util.HalfFloatMatrix;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Implements a Hierarchical Clustering.
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
     * Key to store the selected columns in the settings.
     */
    public static final String SELECTED_COLUMNS_KEY = "selectedColumns";

    /**
     * Key to store the cache flag in the settings.
     */
    public static final String USE_CACHE_KEY = "cacheDistances";

    /**
     * Specifies the mode the distance between two clusters is calculated.
     */
    private final SettingsModelString m_linkageType =
        HierarchicalClusterNodeDialog.createSettingsLinkageType();

    /**
     * Specifies the number clusters when the output table should be generated.
     */
    private final SettingsModelIntegerBounded m_numClustersForOutput =
        HierarchicalClusterNodeDialog.createSettingsNumberOfClusters();

    private final SettingsModelBoolean m_cacheDistances =
        HierarchicalClusterNodeDialog.createSettingsCacheKeys();

    private final SettingsModelFilterString m_selectedColumns =
        HierarchicalClusterNodeDialog.createSettingsColumns();

    /**
     * The distance function to use.
     */
    private final SettingsModelString m_distFunctionName =
        HierarchicalClusterNodeDialog.createSettingsDistanceFunction();

    private DistanceFunction m_distFunction;

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
     * {@inheritDoc}
     */
    public DataArray getDataArray(final int index) {
        if (index == 0) {
            return m_fusionTable;
        }
        return m_dataArray;
    }

    /**
     *
     * @return the root node of the cluster hierarchy.
     */
    public ClusterNode getRootNode() {
        return m_rootNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws Exception {

        // determine the indices of the selected columns
        List<String> inlcludedCols = m_selectedColumns.getIncludeList();
        int[] selectedColIndices = new int[inlcludedCols.size()];
        for (int count = 0; count < selectedColIndices.length; count++) {
            selectedColIndices[count] =
                    data[0].getDataTableSpec().findColumnIndex(
                            inlcludedCols.get(count));
        }

        BufferedDataTable inputData = data[0];

        DataTable outputData = null;

        if (DistanceFunction.Names.Manhattan.toString().equals(
                m_distFunctionName.getStringValue())) {
            m_distFunction = ManhattanDist.MANHATTEN_DISTANCE;
        } else {
            m_distFunction = EuclideanDist.EUCLIDEAN_DISTANCE;
        }

        // generate initial clustering
        // which means that every data point is one cluster
        List<ClusterNode> clusters = initClusters(inputData, exec);
        // store the distance per each fusion step
        DataContainer fusionCont = new DataContainer(createFusionSpec());
        int iterationStep = 0;

        final HalfFloatMatrix cache;
        if (m_cacheDistances.getBooleanValue()) {
            cache = new HalfFloatMatrix(inputData.getRowCount(), false);
            cache.fill(Float.NaN);
        } else {
            cache = null;
        }

        double max = inputData.getRowCount();
        // the number of clusters at the beginning is equal to the number
        // of data rows (each row is a cluster)
        int numberDataRows = clusters.size();

        while (clusters.size() > 1) {
            // checks if number clusters to generate output table is reached
            if (m_numClustersForOutput.getIntValue() == clusters.size()) {
                outputData = createResultTable(inputData, clusters, exec);
            }
            exec.setProgress((numberDataRows - clusters.size())
                    / (double)numberDataRows, clusters.size()
                    + " clusters left to merge.");
            iterationStep++;
            exec.setProgress(iterationStep / max, "Iteration " + iterationStep
                    + ", " + clusters.size() + " clusters remaining");

            // calculate distance between all clusters
            float currentSmallestDist = Float.MAX_VALUE;
            ClusterNode currentClosestCluster1 = null;
            ClusterNode currentClosestCluster2 = null;

            // subprogress for loop
            double availableProgress = (1.0 / numberDataRows);
            ExecutionContext subexec =
                    exec.createSubExecutionContext(availableProgress);
            for (int i = 0; i < clusters.size(); i++) {
                exec.checkCanceled();
                ClusterNode node1 = clusters.get(i);
                for (int j = i + 1; j < clusters.size(); j++) {
                    final float dist;
                    ClusterNode node2 = clusters.get(j);

                    // call the chosen function to calculate the distance
                    // between two clusters. At the moment is single linkage
                    // and average linkage supported.
                    if (m_linkageType.getStringValue().equals(Linkage.SINGLE.name())) {
                        dist = calculateSingleLinkageDist(node1, node2, cache,
                                selectedColIndices);
                    } else if (m_linkageType.getStringValue().equals(Linkage.AVERAGE.name())) {
                        dist = calculateAverageLinkageDist(node1, node2, cache,
                                selectedColIndices);
                    } else {
                        dist = calculateCompleteLinkageDist(node1, node2, cache,
                                selectedColIndices);
                    }

                    if (dist < currentSmallestDist) {
                        currentClosestCluster1 = node1;
                        currentClosestCluster2 = node2;
                        currentSmallestDist = dist;
                    }
                }
            }
            subexec.setProgress(1.0);
            // make one cluster of the two closest
            ClusterNode newNode =
                    new ClusterNode(currentClosestCluster1,
                            currentClosestCluster2, currentSmallestDist);
            clusters.remove(currentClosestCluster1);
            clusters.remove(currentClosestCluster2);

            clusters.add(newNode);

            // store the distance per each fusion step
            fusionCont.addRowToTable(new DefaultRow(
            // row key
                    Integer.toString(clusters.size()),
                    // x-axis scatter plotter
                    new IntCell(clusters.size()),
                    // y-axis scatter plotter
                    new DoubleCell(newNode.getDist())));

            // // print number clusters and their data points
            // LOGGER.debug("Iteration " + iterationStep + ":");
            // LOGGER.debug(" Number Clusters: " + clusters.size());
            // printClustersDataRows(clusters);

        }
        if (clusters.size() > 0) {
            m_rootNode = clusters.get(0);

        }

        fusionCont.close();

        // if there was no input data create an empty output data
        if (outputData == null) {
            outputData = createResultTable(inputData, clusters, exec);
        }
        m_dataArray = new DefaultDataArray(
                inputData, 1, inputData.getRowCount());
        m_fusionTable = new DefaultDataArray(
                fusionCont.getTable(), 1, iterationStep);

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
    private float calculateSingleLinkageDist(final ClusterNode node1,
            final ClusterNode node2, final HalfFloatMatrix cache,
            final int[] selectedColIndices) {
        float minDist = Float.MAX_VALUE;

        for (ClusterNode node1Leaf : node1.leafs()) {
            final DataRow row1 = node1Leaf.getLeafDataPoint();
            final int row1Index = node1Leaf.getRowIndex();
            for (ClusterNode node2Leaf : node2.leafs()) {
                final DataRow row2 = node2Leaf.getLeafDataPoint();
                final int row2Index = node2Leaf.getRowIndex();

                float f = Float.NaN;
                if (cache != null) {
                    f = cache.get(row1Index, row2Index);
                    if (Float.isNaN(f)) {
                        f =
                                (float)m_distFunction.calcDistance(row1, row2,
                                        selectedColIndices);
                        cache.set(row1Index, row2Index, f);
                    }
                } else {
                    f =
                            (float)m_distFunction.calcDistance(row1, row2,
                                    selectedColIndices);
                }
                minDist = Math.min(minDist, f);
            }
        }

        return minDist;
    }

    /*
     * Calculates the distance via the complete linkage paradigm. That means two
     * clusters have the distance of its farest data rows
     *
     */
    private float calculateCompleteLinkageDist(final ClusterNode node1,
            final ClusterNode node2, final HalfFloatMatrix cache,
            final int[] selectedColIndices) {
        float maxDist = 0;

        for (ClusterNode node1Leaf : node1.leafs()) {
            final DataRow row1 = node1Leaf.getLeafDataPoint();
            final int row1Index = node1Leaf.getRowIndex();
            for (ClusterNode node2Leaf : node2.leafs()) {
                final DataRow row2 = node2Leaf.getLeafDataPoint();
                final int row2Index = node2Leaf.getRowIndex();

                float f = Float.NaN;
                if (cache != null) {
                    f = cache.get(row1Index, row2Index);
                    if (Float.isNaN(f)) {
                        f =
                                (float)m_distFunction.calcDistance(row1, row2,
                                        selectedColIndices);
                        cache.set(row1Index, row2Index, f);
                    }
                } else {
                    f =
                            (float)m_distFunction.calcDistance(row1, row2,
                                    selectedColIndices);
                }
                maxDist = Math.max(maxDist, f);
            }
        }

        return maxDist;
    }

    /*
     * Calculates the distance via the average linkage paradigm. That means two
     * clusters have the distance of the average distance of all their member
     * data rows.
     */
    private float calculateAverageLinkageDist(final ClusterNode node1,
            final ClusterNode node2, final HalfFloatMatrix cache,
            final int[] selectedColIndices) {
        float sumDist = 0;

        for (ClusterNode node1Leaf : node1.leafs()) {
            final DataRow row1 = node1Leaf.getLeafDataPoint();
            final int row1Index = node1Leaf.getRowIndex();
            for (ClusterNode node2Leaf : node2.leafs()) {
                final DataRow row2 = node2Leaf.getLeafDataPoint();
                final int row2Index = node2Leaf.getRowIndex();

                float f = Float.NaN;
                if (cache != null) {
                    f = cache.get(row1Index, row2Index);
                    if (Float.isNaN(f)) {
                        f = (float)m_distFunction.calcDistance(row1, row2,
                                        selectedColIndices);
                        cache.set(row1Index, row2Index, f);
                    }
                } else {
                    f = (float)m_distFunction.calcDistance(row1, row2,
                                    selectedColIndices);
                }
                sumDist += f;
            }
        }

        // divide by the number pairewise distances
        return sumDist / (node1.getLeafCount() * node2.getLeafCount());
    }

    /**
     * Creates number of data rows clusters as initial clustering.
     *
     * @param inputData
     *            the input data rows
     * @param exec
     *            to check for user cancelations
     *
     * @throws CanceledExecutionException
     *             if user canceled
     *
     * @return the vector with all initial clusters.
     */
    private List<ClusterNode> initClusters(final DataTable inputData,
            final ExecutionContext exec) throws CanceledExecutionException {
        List<ClusterNode> rowVector = new ArrayList<ClusterNode>();
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
     * @param inputData
     *            the input data table which has the meta information like
     *            column names classes, and so on
     *
     * @param clusters
     *            the vector with the clusters
     * @param exec
     *            to check for user cancelations
     * @return the result data table which contains the data rows with the class
     *         information
     * @throws CanceledExecutionException
     *             if user canceled
     */
    private DataTable createResultTable(final DataTable inputData,
            final List<ClusterNode> clusters, final ExecutionContext exec)
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
                resultTable.addRowToTable(new DefaultRow(dataRow.getKey(),
                        cells));

                exec.checkCanceled();
            }

        }

        resultTable.close();
        return resultTable.getTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check the range of the cluster number
        if (m_numClustersForOutput.getIntValue() < 1) {
            throw new InvalidSettingsException(
                    "Number of output clusters must be greater than 0.");
        }
        if ((!m_linkageType.getStringValue().equals(Linkage.SINGLE.name()))
                && (!m_linkageType.getStringValue().equals(Linkage.AVERAGE.name()))
                && (!m_linkageType.getStringValue().equals(Linkage.COMPLETE.name()))) {
            throw new InvalidSettingsException("Linkage Type must either be "
                    + Linkage.SINGLE + ", " + Linkage.AVERAGE + " or "
                    + Linkage.COMPLETE);
        }

        if ((m_selectedColumns.getExcludeList().size() == 0)
                && (m_selectedColumns.getIncludeList().size() == 0)) {
            // use all numeric columns by default
            m_selectedColumns.setExcludeList(new String[0]);
            ArrayList<String> al = new ArrayList<String>();
            for (DataColumnSpec cs : inSpecs[0]) {
                if (cs.getType().isCompatible(DoubleValue.class)) {
                    al.add(cs.getName());
                }
            }
            if (al.size() == 0) {
                throw new InvalidSettingsException("No numeric columns in input"
                        + " table");
            }
            m_selectedColumns.setIncludeList(al);
        } else if (m_selectedColumns.getIncludeList().size() <= 0) {
            throw new InvalidSettingsException(
                    "No column for clustering included");
        }

        for (String col : m_selectedColumns.getIncludeList()) {
            DataColumnSpec colSpec = inSpecs[0].getColumnSpec(col);
            if (colSpec == null) {
                throw new InvalidSettingsException("Column '" + col + "' does "
                        + "not exist in input table");
            }
            if (!colSpec.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Column '" + col
                        + "' is not a numeric column");
            }
        }

        return new DataTableSpec[]{generateOutSpec(inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_numClustersForOutput.loadSettingsFrom(settings);
        m_distFunctionName.loadSettingsFrom(settings);
        m_linkageType.loadSettingsFrom(settings);
        m_cacheDistances.loadSettingsFrom(settings);
        try {
            m_selectedColumns.loadSettingsFrom(settings);
            if (m_selectedColumns.getIncludeList().size() <= 0) {
                setWarningMessage("No column included!");
            }
        } catch (InvalidSettingsException ex) {
            // handle pre-1.3 settings
            m_selectedColumns.setExcludeList(new String[0]);
            m_selectedColumns.setIncludeList(new String[0]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_numClustersForOutput.saveSettingsTo(settings);
        m_distFunctionName.saveSettingsTo(settings);
        m_linkageType.saveSettingsTo(settings);
        m_cacheDistances.saveSettingsTo(settings);
        m_selectedColumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_numClustersForOutput.validateSettings(settings);
        m_distFunctionName.validateSettings(settings);
        m_selectedColumns.validateSettings(settings);
        m_cacheDistances.validateSettings(settings);
        SettingsModelString linkageType =
            m_linkageType.createCloneWithValidatedValue(settings);
        // check linkage method
        boolean valid = false;
        for (Linkage link : Linkage.values()) {
            if (link.name().equals(linkageType.getStringValue())) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new InvalidSettingsException("Linkage Type must either be "
                    + Linkage.SINGLE + ", " + Linkage.AVERAGE + " or "
                    + Linkage.COMPLETE);
        }

        settings.getBoolean(USE_CACHE_KEY, false);
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
     * {@inheritDoc}
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
        // if we had some data...
        if (m_dataArray.size() > 0) {
            // we also have some clustering nodes
            try {
                m_rootNode = ClusterNode.loadFromXML(settings, m_dataArray);
            } catch (InvalidSettingsException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
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
        // no data -> no clustering nodes
        if (m_rootNode != null) {
            m_rootNode.saveToXML(settings);
        }
        File f = new File(nodeInternDir, CFG_HCLUST);
        FileOutputStream fos = new FileOutputStream(f);
        settings.saveToXML(fos);
    }

}
