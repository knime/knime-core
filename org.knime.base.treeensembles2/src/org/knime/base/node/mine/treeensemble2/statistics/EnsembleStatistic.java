/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   28.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.statistics;

import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;

/**
 *
 * @author Adrian Nembach
 */
public class EnsembleStatistic {

    private final int m_minLevel;
    private final int m_maxLevel;
    private final double m_avgLevel;
    private final int m_minNumNodes;
    private final int m_maxNumNodes;
    private final double m_avgNumNodes;
    private final double m_avgNumSurrogates;
    private final TreeStatistic[] m_treeStats;

    /**
     * Creates an ensemble statistic for the given Tree Ensemble model
     *
     * @param ensembleModel
     */
    public EnsembleStatistic(final TreeEnsembleModel ensembleModel) {
        final int numModels = ensembleModel.getNrModels();
        int minLevel = Integer.MAX_VALUE;
        int maxLevel = -1;
        double avgLevel = 0;
        int minNumNodes = Integer.MAX_VALUE;
        int maxNumNodes = -1;
        double avgNumNodes = 0;
        double avgNumSurrogates = 0;
        m_treeStats = new TreeStatistic[numModels];
        for (int i = 0; i < numModels; i++) {
            final TreeStatistic treeStatistic = new TreeStatistic(ensembleModel.getTreeModel(i));
            m_treeStats[i] = treeStatistic;
            int numLevels = treeStatistic.getNumLevels();
            int numNodes = treeStatistic.getNumNodes();
            avgLevel += numLevels;
            avgNumNodes += numNodes;
            avgNumSurrogates += treeStatistic.getMeanNumSurrogates();
            if (numLevels < minLevel) {
                minLevel = numLevels;
            }
            if (numNodes < minNumNodes) {
                minNumNodes = numNodes;
            }
            if (numLevels > maxLevel) {
                maxLevel = numLevels;
            }
            if (numNodes > maxNumNodes) {
                maxNumNodes = numNodes;
            }
        }
        m_minLevel = minLevel;
        m_maxLevel = maxLevel;
        m_minNumNodes = minNumNodes;
        m_maxNumNodes = maxNumNodes;
        m_avgLevel = avgLevel / numModels;
        m_avgNumNodes = avgNumNodes / numModels;
        m_avgNumSurrogates = avgNumSurrogates / numModels;
    }

    public TreeStatistic getTreeStatistic(final int treeIdx) {
        return m_treeStats[treeIdx];
    }

    public int getMinLevel() {
        return m_minLevel;
    }

    public int getMaxLevel() {
        return m_maxLevel;
    }

    public double getAvgLevel() {
        return m_avgLevel;
    }

    public int getMinNumNodes() {
        return m_minNumNodes;
    }

    public int getMaxNumNodes() {
        return m_maxNumNodes;
    }

    public double getAvgNumNodes() {
        return m_avgNumNodes;
    }

    public double getAvgNumSurrogates() {
        return m_avgNumSurrogates;
    }
}
