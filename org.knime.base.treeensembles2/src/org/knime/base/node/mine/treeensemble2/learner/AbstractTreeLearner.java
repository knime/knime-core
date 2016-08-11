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
 *
 * History
 *   Oct 19, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.memberships.IDataIndexManager;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.sample.column.ColumnSampleStrategy;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractTreeLearner {

    private final TreeEnsembleLearnerConfiguration m_config;

    private final TreeData m_data;

    private final RowSample m_rowSampling;

    private final RandomData m_randomData;

    private final ColumnSampleStrategy m_colSamplingStrategy;

    private final IDataIndexManager m_indexManager;

    private final TreeNodeSignatureFactory m_signatureFactory;

    /**
     * @param config
     * @param data
     * @param randomData
     */
    public AbstractTreeLearner(final TreeEnsembleLearnerConfiguration config, final TreeData data,
        final IDataIndexManager indexManager, final TreeNodeSignatureFactory signatureFactory,
        final RandomData randomData, final RowSample rowSample) {
        m_config = config;
        m_data = data;
        m_randomData = randomData;
        m_rowSampling = rowSample;
        m_colSamplingStrategy = m_config.createColumnSampleStrategy(m_data, randomData);
        m_indexManager = indexManager;
        m_signatureFactory = signatureFactory;
    }

    final IDataIndexManager getIndexManager() {
        return m_indexManager;
    }

    /**
     * @return the rowSampling
     */
    public final RowSample getRowSampling() {
        return m_rowSampling;
    }

    /**
     * @return the colSamplingStrategy
     */
    public final ColumnSampleStrategy getColSamplingStrategy() {
        return m_colSamplingStrategy;
    }

    /**
     * @return the data
     */
    final TreeData getData() {
        return m_data;
    }

    final RandomData getRandomData() {
        return m_randomData;
    }

    /**
     * @return the config
     */
    final TreeEnsembleLearnerConfiguration getConfig() {
        return m_config;
    }

    /**
     * Signatures should be created through this factory to enable reuse of already existing TreeNodeSignatures.
     *
     * return the TreeNodeSignatureFactory
     */
    final TreeNodeSignatureFactory getSignatureFactory() {
        return m_signatureFactory;
    }

    /**
     * Learns a single decision tree model
     *
     * @param exec
     * @param rd
     * @return an extension of the AbstractTreeModel (either regression or classification)
     * @throws CanceledExecutionException
     */
    public abstract AbstractTreeModel learnSingleTree(final ExecutionMonitor exec, final RandomData rd)
        throws CanceledExecutionException;

}
