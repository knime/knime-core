/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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

import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSurrogateCondition;

/**
 *
 * @author Adrian Nembach
 */
public class TreeStatistic {

    private final int m_numLevels;

    private final int m_numNodes;

    private final int m_sumSurrogates;

    public TreeStatistic(final AbstractTreeModel treeModel) {
        final TreeNodeStatistic rootNodeStatistic = new TreeNodeStatistic(treeModel.getRootNode());
        m_numLevels = rootNodeStatistic.m_numLevels;
        m_numNodes = rootNodeStatistic.m_numNodes;
        m_sumSurrogates = rootNodeStatistic.m_sumSurrogates;
    }

    public int getNumLevels() {
        return m_numLevels;
    }

    public int getNumNodes() {
        return m_numNodes;
    }

    public int getSumSurrogates() {
        return m_sumSurrogates;
    }

    public double getMeanNumSurrogates() {
        return ((double)m_sumSurrogates) / m_numNodes;
    }

    private static class TreeNodeStatistic {
        private int m_numLevels;

        private int m_numNodes;

        private int m_sumSurrogates;

        public TreeNodeStatistic(final AbstractTreeNode rootNode) {
            List<AbstractTreeNode> childNodes = rootNode.getChildren();
            TreeNodeCondition condition = rootNode.getCondition();
            if (condition instanceof TreeNodeSurrogateCondition) {
                m_sumSurrogates = ((TreeNodeSurrogateCondition)condition).getNumSurrogates();
            } else {
                m_sumSurrogates = 0;
            }
            if (childNodes.isEmpty()) {
                m_numLevels = 0;
                m_numNodes = 1;
            } else {
                List<TreeNodeStatistic> nodeStatistics = new ArrayList<TreeNodeStatistic>(childNodes.size());
                for (AbstractTreeNode childNode : childNodes) {
                    nodeStatistics.add(new TreeNodeStatistic(childNode));
                }
                m_numNodes = 1;
                int numLevels = -1;
                for (TreeNodeStatistic childNodeStatistic : nodeStatistics) {
                    m_numNodes += childNodeStatistic.m_numNodes;
                    if (childNodeStatistic.m_numLevels > numLevels) {
                        numLevels = childNodeStatistic.m_numLevels;
                    }
                    m_sumSurrogates += childNodeStatistic.m_sumSurrogates;
                }
                m_numLevels = numLevels + 1;
            }
        }
    }
}
