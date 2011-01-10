/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   01.12.2010 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.view.graph;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * An action to expand the branch starting from the selected node in a
 * {@link HierarchicalGraphView}.
 *
 * @author Heiko Hofer
 * @param <K> The type of the nodes user objects in a
 * {@link HierarchicalGraphView}
 */
public class ExpandBranchAction<K> extends AbstractAction {
    private static final long serialVersionUID = 788511232602946113L;
    private HierarchicalGraphView<K> m_graph;

    /**
     * @param graph nodes of the graph are effected by this action
     */
    public ExpandBranchAction(final HierarchicalGraphView<K> graph) {
        super("Expand Branch");
        m_graph = graph;
    }

    /**
     * {@inheritDoc}
     */
    // DefaultMutableTreeNode does not support generics
    @SuppressWarnings("unchecked")
    @Override
    public void actionPerformed(final ActionEvent e) {
        K selected = m_graph.getSelected();
        if (null == selected) {
            return;
        }
        DefaultMutableTreeNode node =
            m_graph.getTreeMap().get(selected);
        K userObject = (K)node.getUserObject();
        List<Object> branch =
            Collections.list(node.breadthFirstEnumeration());
        for (Object o : branch) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode)o;
            K k = (K)n.getUserObject();
            m_graph.getVisible().put(k, new Rectangle());
            m_graph.getCollapsed().remove(k);
        }

        m_graph.layoutGraph();
        // With this call parent components get informed about the
        // change in the preferred size.
        m_graph.getView().revalidate();

        // get surrounding rectangle of the opened subtree
        Rectangle p = m_graph.getVisible().get(userObject);

        Rectangle vis = new Rectangle(p);
        List<Object> expandedSubtree =
                Collections.list(node.breadthFirstEnumeration());
        for (Object o : expandedSubtree) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode)o;
            K k = (K)n.getUserObject();
            Rectangle rr = m_graph.getVisible().get(k);
            if (null != rr) {
                vis = vis.union(rr);
            }
        }
        // Show also the sign for expanding childs
        vis.height += m_graph.getLayoutSettings().getLevelGap() / 2;
        // Display node and all visible children
        m_graph.getView().scrollRectToVisible(vis);
        // Make sure that at least the selected node will be displayed
        m_graph.getView().scrollRectToVisible(p);
        m_graph.getView().repaint();
        return;
    }
}
