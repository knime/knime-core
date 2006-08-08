/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * History
 *   17.07.2006 (cebron): created
 */
package org.knime.base.node.mine.cluster.fuzzycmeans;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;

/**
 * This {@link CellFactory} produces appended cells: for each {@link DataRow}
 * the memberships to the cluster prototypes and the winner cluster in the last
 * column.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class ClusterMembershipFactory implements CellFactory {
    private int m_nrClusters;

    private double[][] m_weights;

    private boolean m_noise;

    private int m_rowCounter;

    /**
     * Constructor.
     * 
     * @param algo the trained FCM-model
     */
    public ClusterMembershipFactory(final FCMAlgorithm algo) {
        m_nrClusters = algo.getClusterCentres().length;
        m_weights = algo.getweightMatrix();
        m_noise = algo.noiseClustering();
        m_rowCounter = 0;
    }

    /**
     * 
     * @see org.knime.core.data.container.CellFactory
     *      #getCells(org.knime.core.data.DataRow)
     */
    public DataCell[] getCells(final DataRow row) {
        DataCell[] memberships = new DataCell[m_nrClusters + 1];
        int winnercluster = -1;
        double maxmembership = Double.MIN_VALUE;
        for (int i = 0; i < m_nrClusters; i++) {
            double membership = m_weights[m_rowCounter][i];
            memberships[i] = new DoubleCell(membership);
            if (membership > maxmembership) {
                maxmembership = membership;
                winnercluster = i;
            }
        }
        if (m_noise && winnercluster == m_nrClusters - 1) {
            memberships[memberships.length - 1] = new StringCell(
                    FuzzyClusterNodeModel.NOISESPEC_KEY);
        } else {
            memberships[memberships.length - 1] = new StringCell(
                    FuzzyClusterNodeModel.CLUSTER_KEY + winnercluster);
        }
        m_rowCounter++;
        return memberships;
    }

    /**
     * @see org.knime.core.data.container.CellFactory#getColumnSpecs()
     */
    public DataColumnSpec[] getColumnSpecs() {
        int nrclusters = m_nrClusters;
        DataColumnSpec[] newSpec = new DataColumnSpec[nrclusters + 1];
        int cluster = 0;
        for (int j = 0; j < nrclusters; j++) {
            if (m_noise && j == (newSpec.length - 2)) {
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
        return newSpec;
    }

    /**
     * 
     * @see org.knime.core.data.container.CellFactory#setProgress(int,
     *      int, org.knime.core.data.RowKey,
     *      org.knime.core.node.ExecutionMonitor)
     */
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress((double)curRowNr / (double)rowCount,
                "Producing Cluster Memberships");
    }
}
