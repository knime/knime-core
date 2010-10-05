/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * History
 *   17.07.2006 (cebron): created
 */
package org.knime.base.node.mine.cluster.fuzzycmeans;

import java.util.Collections;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DoubleBarRenderer;
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        int nrclusters = m_nrClusters;
        DataColumnSpec[] newSpec = new DataColumnSpec[nrclusters + 1];
        int cluster = 0;
        DataColumnSpecCreator colspecCreator = null;
        for (int j = 0; j < nrclusters; j++) {
            if (m_noise && j == (newSpec.length - 2)) {
                colspecCreator =
                        new DataColumnSpecCreator(
                                FuzzyClusterNodeModel.NOISESPEC_KEY,
                                DoubleCell.TYPE);
                colspecCreator.setProperties(new DataColumnProperties(
                        Collections.singletonMap(
                                DataValueRenderer.PROPERTY_PREFERRED_RENDERER,
                                DoubleBarRenderer.DESCRIPTION)));
                colspecCreator.setDomain(new DataColumnDomainCreator(
                        new DoubleCell(0.0), new DoubleCell(1.0))
                        .createDomain());
                newSpec[j] = colspecCreator.createSpec();
                break;
            }
            colspecCreator =
                    new DataColumnSpecCreator(FuzzyClusterNodeModel.CLUSTER_KEY
                            + cluster, DoubleCell.TYPE);
            colspecCreator.setProperties(new DataColumnProperties(Collections
                    .singletonMap(
                            DataValueRenderer.PROPERTY_PREFERRED_RENDERER,
                            DoubleBarRenderer.DESCRIPTION)));
            colspecCreator.setDomain(new DataColumnDomainCreator(
                    new DoubleCell(0.0), new DoubleCell(1.0)).createDomain());
            newSpec[j] = colspecCreator.createSpec();
            cluster++;
        }
        newSpec[newSpec.length - 1] = new DataColumnSpecCreator(
                "Winner Cluster", StringCell.TYPE).createSpec();
        return newSpec;
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress((double)curRowNr / (double)rowCount,
                "Producing Cluster Memberships");
    }
}
