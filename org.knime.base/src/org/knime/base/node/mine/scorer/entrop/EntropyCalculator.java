/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
package org.knime.base.node.mine.scorer.entrop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.FileUtil;
import org.knime.core.util.MutableInteger;


/**
 * Utility class that allows to calculate some entropy and quality values for
 * clustering results given a reference clustering.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class EntropyCalculator {
    private final double m_entropy;

    private final double m_quality;

    private final int m_patternsInClusters;

    private final int m_nrClusters;

    private final int m_patternsInReference;

    private final int m_nrReference;

    private final DataTable m_scoreTable;

    private final Map<DataCell, Set<DataCell>> m_clusteringMap;

    /**
     * Creates new instance.
     * 
     * @param reference the reference table, i.e. the clusters that should be
     *            found
     * @param clustering the table containing the clustering to judge
     * @param referenceCol the column index in <code>reference</code> that
     *            contains the cluster membership
     * @param clusteringCol the column index in <code>clustering</code> that
     *            contains the cluster membership
     * @param exec the execution monitor for canceling and progress
     * @throws CanceledExecutionException if canceled
     * 
     */
    public EntropyCalculator(final DataTable reference,
            final DataTable clustering, final int referenceCol,
            final int clusteringCol, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        this(getMap(reference, referenceCol, exec.createSubProgress(0.5)),
                getClusterMap(clustering, clusteringCol, exec
                        .createSubProgress(0.5)));
    }

    /**
     * Creates new instance given the maps of clustering and reference.
     * 
     * @param referenceMap the reference clustering, mapping ID -&gt; cluster
     *            name
     * @param clusteringMap the clustering to score, cluster name -&gt; cluster
     *            members in a set (may not necessarily be unique)
     */
    public EntropyCalculator(final Map<DataCell, DataCell> referenceMap,
            final Map<DataCell, Set<DataCell>> clusteringMap) {
        m_clusteringMap = clusteringMap;
        m_entropy = getEntropy(referenceMap, m_clusteringMap);
        m_quality = getQuality(referenceMap, m_clusteringMap);
        // count objects as found in clusters
        int patInCluster = 0;
        for (Set<DataCell> s : m_clusteringMap.values()) {
            patInCluster += s.size();
        }
        m_patternsInClusters = patInCluster;
        m_nrClusters = m_clusteringMap.size();

        m_patternsInReference = referenceMap.size();
        HashSet<DataCell> referenceCluster = new HashSet<DataCell>(referenceMap
                .values());
        m_nrReference = referenceCluster.size();
        m_scoreTable = createScoreTable(referenceMap, m_clusteringMap);
    }

    /* Internal constructor used by the load method. */
    private EntropyCalculator(final double entropy, final double quality,
            final int patternsInClusters, final int nrClusters,
            final int patternsInReference, final int nrReference,
            final DataTable scoreTable,
            final Map<DataCell, Set<DataCell>> clusteringMap) {
        m_entropy = entropy;
        m_quality = quality;
        m_patternsInClusters = patternsInClusters;
        m_nrClusters = nrClusters;
        m_patternsInReference = patternsInReference;
        m_nrReference = nrReference;
        m_scoreTable = scoreTable;
        m_clusteringMap = clusteringMap;
    }

    /**
     * @return the entropy
     */
    public double getEntropy() {
        return m_entropy;
    }

    /**
     * @return the quality
     */
    public double getQuality() {
        return m_quality;
    }

    /**
     * @return the nrClusters
     */
    public int getNrClusters() {
        return m_nrClusters;
    }

    /**
     * @return the nrReference
     */
    public int getNrReference() {
        return m_nrReference;
    }

    /**
     * @return the patternsInClusters
     */
    public int getPatternsInClusters() {
        return m_patternsInClusters;
    }

    /**
     * @return the patternsInReference
     */
    public int getPatternsInReference() {
        return m_patternsInReference;
    }

    /**
     * @return the scoreTable
     */
    public DataTable getScoreTable() {
        return m_scoreTable;
    }

    /**
     * Map of Cluster name -&gt; cluster members (in a set) as given in the
     * clustering to score.
     * 
     * @return the clusteringMap
     */
    public Map<DataCell, Set<DataCell>> getClusteringMap() {
        return m_clusteringMap;
    }

    private static final String FILE_SCORER_TABLE = "scorer_table.zip";

    private static final String FILE_SETTINGS = "entropy.xml.gz";

    private static final String CFG_SETTINGS = "entropy_settings";

    private static final String CFG_ENTROPY = "entropy";

    private static final String CFG_QUALITY = "quality";

    private static final String CFG_PAT_IN_CLUSTER = "patterns_in_cluster";

    private static final String CFG_PAT_IN_REFERENCE = "patterns_in_reference";

    private static final String CFG_NR_CLUSTER = "nr_cluster";

    private static final String CFG_NR_REFERENCES = "nr_references";

    private static final String CFG_CLUSTERING_MAP = "clustering_map";

    private static final String CFG_MAPPED_KEYS = "mapped_keys";

    /**
     * Saves the structure of this objec to the target directory.
     * 
     * @param dir to save to
     * @param exec for progress/cancel
     * @throws IOException if that fails
     * @throws CanceledExecutionException if canceled
     */
    public void save(final File dir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        File scorerFile = new File(dir, FILE_SCORER_TABLE);
        DataContainer.writeToZip(m_scoreTable, scorerFile, exec);
        File settingsFile = new File(dir, FILE_SETTINGS);
        NodeSettings config = new NodeSettings(CFG_SETTINGS);
        config.addDouble(CFG_ENTROPY, m_entropy);
        config.addDouble(CFG_QUALITY, m_quality);
        config.addInt(CFG_PAT_IN_CLUSTER, m_patternsInClusters);
        config.addInt(CFG_PAT_IN_REFERENCE, m_patternsInReference);
        config.addInt(CFG_NR_CLUSTER, m_nrClusters);
        config.addInt(CFG_NR_REFERENCES, m_nrReference);
        NodeSettingsWO subConfig = config.addNodeSettings(CFG_CLUSTERING_MAP);
        for (Map.Entry<DataCell, Set<DataCell>> entry : m_clusteringMap
                .entrySet()) {
            exec.checkCanceled();
            DataCell key = entry.getKey();
            Set<DataCell> values = entry.getValue();
            NodeSettingsWO keySettings = subConfig.addNodeSettings(key
                    .toString());
            keySettings.addDataCell(key.toString(), key);
            keySettings.addDataCellArray(CFG_MAPPED_KEYS, values
                    .toArray(new DataCell[values.size()]));
        }
        config.saveToXML(new BufferedOutputStream(new GZIPOutputStream(
                new FileOutputStream(settingsFile))));
    }

    /**
     * Factory method to restore this object given a directory in which the
     * content is saved.
     * 
     * @param dir the dir to read from
     * @param exec for cancellation.
     * @return a new object as read from dir
     * @throws IOException if that fails
     * @throws InvalidSettingsException if the internals don't match
     * @throws CanceledExecutionException if canceled
     */
    public static EntropyCalculator load(final File dir,
            final ExecutionMonitor exec) throws IOException,
            InvalidSettingsException, CanceledExecutionException {
        File scorerFile = new File(dir, FILE_SCORER_TABLE);
        File tempFile = DataContainer.createTempFile();
        FileUtil.copy(scorerFile, tempFile);
        DataTable scorerTable = DataContainer.readFromZip(tempFile);
        File settingsFile = new File(dir, FILE_SETTINGS);
        NodeSettingsRO config = NodeSettings
                .loadFromXML(new BufferedInputStream(new GZIPInputStream(
                        new FileInputStream(settingsFile))));
        double entropy = config.getDouble(CFG_ENTROPY);
        double quality = config.getDouble(CFG_QUALITY);
        int patternsInCluster = config.getInt(CFG_PAT_IN_CLUSTER);
        int patternsInReference = config.getInt(CFG_PAT_IN_REFERENCE);
        int nrClusters = config.getInt(CFG_NR_CLUSTER);
        int nrReferences = config.getInt(CFG_NR_REFERENCES);
        NodeSettingsRO subConfig = config.getNodeSettings(CFG_CLUSTERING_MAP);
        LinkedHashMap<DataCell, Set<DataCell>> map 
            = new LinkedHashMap<DataCell, Set<DataCell>>();
        for (String key : subConfig.keySet()) {
            exec.checkCanceled();
            NodeSettingsRO keySettings = subConfig.getNodeSettings(key);
            DataCell cellKey = keySettings.getDataCell(key);
            DataCell[] mappedKeys = keySettings
                    .getDataCellArray(CFG_MAPPED_KEYS);
            map.put(cellKey, new LinkedHashSet<DataCell>(Arrays
                    .asList(mappedKeys)));
        }
        return new EntropyCalculator(entropy, quality, patternsInCluster,
                nrClusters, patternsInReference, nrReferences, scorerTable, 
                map);
    }

    private static final String[] NAMES = new String[]{"Size", "Entropy",
            "Normalized Entropy"};

    private static final DataType[] TYPES = new DataType[]{IntCell.TYPE,
            DoubleCell.TYPE, DoubleCell.TYPE};

    private static DataTable createScoreTable(
            final Map<DataCell, DataCell> referenceMap,
            final Map<DataCell, Set<DataCell>> clusteringMap) {
        ArrayList<DefaultRow> sortedRows = new ArrayList<DefaultRow>();
        // number of different clusters in reference clustering, used for
        // normalization
        int clusterCardinalityInReference = (new HashSet<DataCell>(referenceMap
                .values())).size();
        double normalization = Math.log(clusterCardinalityInReference)
                / Math.log(2.0);
        for (Map.Entry<DataCell, Set<DataCell>> e : clusteringMap.entrySet()) {
            DataCell size = new IntCell(e.getValue().size());
            double entropy = entropy(referenceMap, e.getValue());
            DataCell entropyCell = new DoubleCell(entropy);
            DataCell normEntropy = new DoubleCell(entropy / normalization);
            DataCell clusterID = e.getKey();
            DefaultRow row = new DefaultRow(clusterID, size, entropyCell,
                    normEntropy);
            sortedRows.add(row);
        }
        Collections.sort(sortedRows, new Comparator<DefaultRow>() {
            public int compare(final DefaultRow o1, final DefaultRow o2) {
                double e1 = ((DoubleValue)o1.getCell(2)).getDoubleValue();
                double e2 = ((DoubleValue)o2.getCell(2)).getDoubleValue();
                return e1 < e2 ? -1 : e1 > e2 ? 1 : 0;
            }
        });
        DataRow[] rows = sortedRows.toArray(new DataRow[0]);
        return new DefaultTable(rows, NAMES, TYPES);
    }

    private static HashMap<DataCell, Set<DataCell>> getClusterMap(
            final DataTable table, final int colIndex, 
            final ExecutionMonitor ex) throws CanceledExecutionException {
        HashMap<DataCell, Set<DataCell>> result 
            = new LinkedHashMap<DataCell, Set<DataCell>>();
        int rowCount = -1;
        if (table instanceof BufferedDataTable) {
            rowCount = ((BufferedDataTable)table).getRowCount();
        }
        int i = 1; // row counter
        final String name = table.getDataTableSpec().getName();
        for (RowIterator it = table.iterator(); it.hasNext(); i++) {
            DataRow row = it.next();
            String m = "Scanning row " + i + " of table \"" + name + "\".";
            if (rowCount >= 0) {
                ex.setProgress(i / (double)rowCount, m);
            } else {
                ex.setMessage(m);
            }
            ex.checkCanceled();
            DataCell id = row.getKey().getId();
            DataCell clusterMember = row.getCell(colIndex);
            Set<DataCell> members = result.get(clusterMember);
            if (members == null) {
                members = new HashSet<DataCell>();
                result.put(clusterMember, members);
            }
            members.add(id);
        }
        return result;
    }

    private static HashMap<DataCell, DataCell> getMap(final DataTable table,
            final int colIndex, final ExecutionMonitor ex)
            throws CanceledExecutionException {
        HashMap<DataCell, DataCell> result 
            = new LinkedHashMap<DataCell, DataCell>();
        int rowCount = -1;
        if (table instanceof BufferedDataTable) {
            rowCount = ((BufferedDataTable)table).getRowCount();
        }
        int i = 1; // row counter
        final String name = table.getDataTableSpec().getName();
        for (RowIterator it = table.iterator(); it.hasNext(); i++) {
            DataRow row = it.next();
            String m = "Scanning row " + i + " of table \"" + name + "\".";
            if (rowCount >= 0) {
                ex.setProgress(i / (double)rowCount, m);
            } else {
                ex.setMessage(m);
            }
            ex.checkCanceled();
            DataCell id = row.getKey().getId();
            DataCell clusterMember = row.getCell(colIndex);
            result.put(id, clusterMember);
        }
        return result;
    }

    /**
     * Get quality measure of current cluster result (in 0-1). The quality value
     * is defined as
     * <p>
     * sum over all clusters (curren_cluster_size / patterns_count * (1 -
     * entropy (current_cluster wrt. reference).
     * <p>
     * For further details see Bernd Wiswedel, Michael R. Berthold, <b>Fuzzy
     * Clustering in Parallel Universes</b>, <i>International Journal of
     * Approximate Reasoning</i>, 2006.
     * 
     * @param reference the reference clustering, maps patterns to cluster ID.
     *            The reference map is supposed to contain all data (if there
     *            are noise objects, that should be contained and have an own).
     *            The quality value is normalized over the size of this set.
     * @param clusterMap the map containing the clusters that have been found,
     *            i.e. clusterID (as above) as key and the set of all contained
     *            patterns as value
     * @return quality value in [0,1]
     */
    public static double getQuality(final Map<DataCell, DataCell> reference,
            final Map<DataCell, Set<DataCell>> clusterMap) {
        // get the number of different clusters in the reference set
        HashSet<DataCell> differentClusters = new HashSet<DataCell>();
        for (DataCell c : reference.values()) {
            differentClusters.add(c);
        }
        int refClusterCount = differentClusters.size();
        differentClusters = null; // garbage
        // normalizing value (such that the maximum value for the entropy is 1
        double normalizer = Math.log(refClusterCount) / Math.log(2.0);
        double quality = 0.0;
        int patCount = 0;
        for (Set<DataCell> pats : clusterMap.values()) {
            int size = pats.size();
            patCount += size;
            double entropy = entropy(reference, pats);
            double normalizedEntropy;
            if (normalizer == 0.0) {
                assert entropy == 0.0;
                normalizedEntropy = 0.0;
            } else {
                normalizedEntropy = entropy / normalizer;
            }
            quality += size * (1.0 - normalizedEntropy);
        }
        // normalizing over the number of objects in the reference set
        return quality / reference.size();
    }

    /**
     * Get entropy according to reference clustering, the entropy value is not
     * normalized, i.e. the result is in the range of
     * <code>[0, log<sub>2</sub>(|cluster|)</code>.
     * 
     * @param reference the reference clustering to compare to
     * @param clusterMap the clustering to judge
     * @return entropy value
     */
    public static double getEntropy(final Map<DataCell, DataCell> reference,
            final Map<DataCell, Set<DataCell>> clusterMap) {
        double entropy = 0.0;
        for (Set<DataCell> pats : clusterMap.values()) {
            double e = entropy(reference, pats);
            entropy += e;
        }
        return entropy;
    }

    /**
     * Get entropy for one single cluster.
     * 
     * @param ref the reference clustering
     * @param pats the single cluster to score
     * @return the (not-normalized) entropy of <code>pats</code> wrt.
     *         <code>ref</code>
     */
    public static double entropy(final Map<DataCell, DataCell> ref,
            final Set<DataCell> pats) {
        // that will map the "original" cluster ID to a counter.
        HashMap<DataCell, MutableInteger> refClusID2Count 
            = new HashMap<DataCell, MutableInteger>();
        for (DataCell pat : pats) {
            DataCell origCluster = ref.get(pat);
            MutableInteger countForClus = refClusID2Count.get(origCluster);
            // if we haven't had cluster id before ...
            if (countForClus == null) {
                // init the counter with 1
                refClusID2Count.put(origCluster, new MutableInteger(1));
            } else {
                countForClus.inc();
            }
        }
        final int size = pats.size();
        double e = 0.0;
        for (MutableInteger clusterCount : refClusID2Count.values()) {
            int count = clusterCount.intValue();
            double quot = count / (double)size;
            e -= quot * Math.log(quot) / Math.log(2.0);
        }
        return e;
    }
}
