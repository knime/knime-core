/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
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

    private final Map<RowKey, Set<RowKey>> m_clusteringMap;

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
    public EntropyCalculator(final Map<RowKey, RowKey> referenceMap,
            final Map<RowKey, Set<RowKey>> clusteringMap) {
        m_clusteringMap = clusteringMap;
        m_entropy = getEntropy(referenceMap, m_clusteringMap);
        m_quality = getQuality(referenceMap, m_clusteringMap);
        // count objects as found in clusters
        int patInCluster = 0;
        for (Set<RowKey> s : m_clusteringMap.values()) {
            patInCluster += s.size();
        }
        m_patternsInClusters = patInCluster;
        m_nrClusters = m_clusteringMap.size();

        m_patternsInReference = referenceMap.size();
        HashSet<RowKey> referenceCluster = new HashSet<RowKey>(referenceMap
                .values());
        m_nrReference = referenceCluster.size();
        m_scoreTable = createScoreTable(referenceMap, m_clusteringMap);
    }

    /* Internal constructor used by the load method. */
    private EntropyCalculator(final double entropy, final double quality,
            final int patternsInClusters, final int nrClusters,
            final int patternsInReference, final int nrReference,
            final DataTable scoreTable,
            final Map<RowKey, Set<RowKey>> clusteringMap) {
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
    
    /** @return Table spec to {@link #getScoreTable()}. */
    public static DataTableSpec getScoreTableSpec() {
        return new DataTableSpec("Entropy Scores", NAMES, TYPES);
    }

    /**
     * Map of Cluster name -&gt; cluster members (in a set) as given in the
     * clustering to score.
     * 
     * @return the clusteringMap
     */
    public Map<RowKey, Set<RowKey>> getClusteringMap() {
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
        for (Map.Entry<RowKey, Set<RowKey>> entry : m_clusteringMap
                .entrySet()) {
            exec.checkCanceled();
            RowKey key = entry.getKey();
            Set<RowKey> values = entry.getValue();
            NodeSettingsWO keySettings = subConfig.addNodeSettings(key
                    .toString());
            keySettings.addRowKey(key.toString(), key);
            keySettings.addRowKeyArray(CFG_MAPPED_KEYS, values
                    .toArray(new RowKey[values.size()]));
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
        LinkedHashMap<RowKey, Set<RowKey>> map 
            = new LinkedHashMap<RowKey, Set<RowKey>>();
        for (String key : subConfig.keySet()) {
            exec.checkCanceled();
            NodeSettingsRO keySettings = subConfig.getNodeSettings(key);
            Set<RowKey> rowKeys;
            RowKey keyCell;
            try {
                keyCell = new RowKey(keySettings.getDataCell(key).toString());
                // load settings before 2.0
                DataCell[] mappedKeys = keySettings
                    .getDataCellArray(CFG_MAPPED_KEYS);
                rowKeys = new LinkedHashSet<RowKey>();
                for (DataCell dc : mappedKeys) {
                    rowKeys.add(new RowKey(dc.toString()));
                }
            } catch (InvalidSettingsException ise) {
                keyCell = keySettings.getRowKey(key);
                RowKey[] mappedKeys = keySettings
                    .getRowKeyArray(CFG_MAPPED_KEYS);
                rowKeys = new LinkedHashSet<RowKey>(Arrays.asList(mappedKeys));
            }
            map.put(keyCell, rowKeys);
        }
        return new EntropyCalculator(entropy, quality, patternsInCluster,
                nrClusters, patternsInReference, nrReferences, scorerTable, 
                map);
    }

    private static final String[] NAMES = new String[]{"Size", "Entropy",
            "Normalized Entropy", "Quality"};

    private static final DataType[] TYPES = new DataType[]{IntCell.TYPE,
            DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE};

    private static DataTable createScoreTable(
            final Map<RowKey, RowKey> referenceMap,
            final Map<RowKey, Set<RowKey>> clusteringMap) {
        ArrayList<DefaultRow> sortedRows = new ArrayList<DefaultRow>();
        // number of different clusters in reference clustering, used for
        // normalization
        int clusterCardinalityInReference = (new HashSet<RowKey>(referenceMap
                .values())).size();
        double normalization = Math.log(clusterCardinalityInReference)
                / Math.log(2.0);
        int totalSize = 0;
        for (Map.Entry<RowKey, Set<RowKey>> e : clusteringMap.entrySet()) {
            int size = e.getValue().size();
            DataCell sizeCell = new IntCell(size);
            totalSize += size;
            double entropy = entropy(referenceMap, e.getValue());
            DataCell entropyCell = new DoubleCell(entropy);
            DataCell normEntropy = new DoubleCell(entropy / normalization);
            DataCell quality = DataType.getMissingCell();
            RowKey clusterID = e.getKey();
            DefaultRow row = new DefaultRow(clusterID, sizeCell, entropyCell,
                    normEntropy, quality);
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
        DataTableSpec tableSpec = getScoreTableSpec();
        DataContainer container = new DataContainer(tableSpec);
        for (DataRow r : rows) {
            container.addRowToTable(r);
        }
        // last row contains overall quality values
        double entropy = getEntropy(referenceMap, clusteringMap);
        double quality = getQuality(referenceMap, clusteringMap);
        DataCell entropyCell = new DoubleCell(entropy);
        DataCell normEntropy = new DoubleCell(entropy / normalization);
        DataCell qualityCell = new DoubleCell(quality);
        DataCell size = new IntCell(totalSize);
        RowKey clusterID = new RowKey("Overall");
        int uniquifier = 1;
        while (clusteringMap.containsKey(clusterID)) {
            clusterID = new RowKey("Overall (#" + (uniquifier++) + ")");
        }
        
        DefaultRow row = new DefaultRow(clusterID, size, entropyCell,
                normEntropy, qualityCell);
        container.addRowToTable(row);
        container.close();
        return container.getTable();
    }

    private static HashMap<RowKey, Set<RowKey>> getClusterMap(
            final DataTable table, final int colIndex, 
            final ExecutionMonitor ex) throws CanceledExecutionException {
        HashMap<RowKey, Set<RowKey>> result 
            = new LinkedHashMap<RowKey, Set<RowKey>>();
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
            RowKey id = row.getKey();
            RowKey clusterMember = new RowKey(row.getCell(colIndex).toString());
            Set<RowKey> members = result.get(clusterMember);
            if (members == null) {
                members = new HashSet<RowKey>();
                result.put(clusterMember, members);
            }
            members.add(id);
        }
        return result;
    }
    
    private static HashMap<RowKey, RowKey> getMap(final DataTable table,
            final int colIndex, final ExecutionMonitor ex)
            throws CanceledExecutionException {
        HashMap<RowKey, RowKey> result 
            = new LinkedHashMap<RowKey, RowKey>();
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
            RowKey id = row.getKey();
            RowKey clusterMember = new RowKey(row.getCell(colIndex).toString());
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
    public static double getQuality(final Map<RowKey, RowKey> reference,
            final Map<RowKey, Set<RowKey>> clusterMap) {
        // optimistic guess (we don't have counterexamples!)
        if (clusterMap.isEmpty()) {
            return 1.0;
        }
        // get the number of different clusters in the reference set
        int refClusterCount = 
            new HashSet<RowKey>(reference.values()).size();
        // normalizing value (such that the maximum value for the entropy is 1
        double normalizer = Math.log(refClusterCount) / Math.log(2.0);
        double quality = 0.0;
        int patCount = 0;
        for (Set<RowKey> pats : clusterMap.values()) {
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
        return quality / patCount;
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
    public static double getEntropy(final Map<RowKey, RowKey> reference,
            final Map<RowKey, Set<RowKey>> clusterMap) {
        // optimistic guess (we don't have counterexamples!)
        if (clusterMap.isEmpty()) {
            return 0.0;
        }
        double entropy = 0.0;
        int patCount = 0;
        for (Set<RowKey> pats : clusterMap.values()) {
            int size = pats.size();
            patCount += size;
            double e = entropy(reference, pats);
            entropy += size * e;
        }
        // normalizing over the number of objects in the reference set
        return entropy / patCount;
    }

    /**
     * Get entropy for one single cluster.
     * 
     * @param ref the reference clustering
     * @param pats the single cluster to score
     * @return the (not-normalized) entropy of <code>pats</code> wrt.
     *         <code>ref</code>
     */
    public static double entropy(final Map<RowKey, RowKey> ref,
            final Set<RowKey> pats) {
        // that will map the "original" cluster ID to a counter.
        HashMap<RowKey, MutableInteger> refClusID2Count 
            = new HashMap<RowKey, MutableInteger>();
        for (RowKey pat : pats) {
            RowKey origCluster = ref.get(pat);
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
