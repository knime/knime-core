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
 *   04.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.view.classification;

import org.knime.base.node.mine.decisiontree2.view.graph.NodeWidget;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeClassification;
import org.knime.base.node.mine.treeensemble2.node.view.RandomForestAbstractTreeNodeWidgetFactory;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class RandomForestClassificationTreeNodeWidgetFactory extends RandomForestAbstractTreeNodeWidgetFactory {

    private boolean m_displayTables;

    private boolean m_displayCharts;

    private boolean m_tablesCollapsed;

    private boolean m_chartsCollapsed;

    private final RandomForestClassificationTreeGraphView m_graph;

    public RandomForestClassificationTreeNodeWidgetFactory(final RandomForestClassificationTreeGraphView graph,
        final boolean displayTables, final boolean displayCharts, final boolean tablesCollapsed,
        final boolean chartsCollapsed) {
        m_graph = graph;
        m_displayTables = displayTables;
        m_displayCharts = displayCharts;
        m_tablesCollapsed = tablesCollapsed;
        m_chartsCollapsed = chartsCollapsed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeWidget<AbstractTreeNode> createGraphNode(final AbstractTreeNode object) {
        if (object instanceof TreeNodeClassification) {
            TreeNodeClassification node = (TreeNodeClassification)object;
            return new RandomForestClassificationTreeNodeWidget(m_graph, node, m_displayTables, m_displayCharts,
                m_tablesCollapsed, m_chartsCollapsed);
        }
        return null;
    }

}
