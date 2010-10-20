/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   28.07.2010 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.view.DecTreeNodeWidgetFactory;
import org.knime.base.node.mine.decisiontree2.view.graph.HierarchicalGraphView;
import org.knime.base.node.mine.decisiontree2.view.graph.NodeWidgetFactory;
import org.knime.core.data.RowKey;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 *
 * @author Heiko HOfer
 */
final class DecTreeLearnerGraphView extends
        NodeView<DecisionTreeLearnerNodeModel> {
    private DecTreeGraphView m_graph;

    private HiLiteHandler m_hiLiteHdl;

    private JMenu m_hiLiteMenu;

    /**
     * Default constructor, taking the model as argument.
     *
     * @param model the underlying NodeModel
     */
    public DecTreeLearnerGraphView(final DecisionTreeLearnerNodeModel model) {
        super(model);
        DecisionTreeNode root =
                null != model.getDecisionTree() ? model.getDecisionTree()
                        .getRootNode() : null;
        m_graph = new DecTreeGraphView(root);
        JScrollPane treeView = new JScrollPane(m_graph.getView());
        Dimension prefSize = treeView.getPreferredSize();
        treeView.setPreferredSize(new Dimension(Math.min(prefSize.width, 800),
                prefSize.height));

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(1.0);
        splitPane.setLeftComponent(treeView);
        splitPane.setRightComponent(m_graph.createOutlineView());

        setComponent(splitPane);

        // retrieve HiLiteHandler from Input port
        m_hiLiteHdl = (model).getInHiLiteHandler(
                DecisionTreeLearnerNodeModel.DATA_INPORT);
        // and add menu entries for HiLite-ing
        m_hiLiteMenu = this.createHiLiteMenu();
        this.getJMenuBar().add(m_hiLiteMenu);
        m_hiLiteMenu.setEnabled(m_hiLiteHdl != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        DecisionTreeLearnerNodeModel model = this.getNodeModel();
        if (model != null) {
            DecisionTree dt = model.getDecisionTree();
            if (dt != null) {
                m_graph.setRootNode(dt.getRootNode());

                // retrieve HiLiteHandler from Input port
                m_hiLiteHdl = model.getInHiLiteHandler(
                                DecisionTreeLearnerNodeModel.DATA_INPORT);
                // and adjust menu entries for HiLite-ing
                m_hiLiteMenu.setEnabled(m_hiLiteHdl != null);
            } else {
                m_graph.setRootNode(null);
            }
        }
    }

    private void updateHiLite(final boolean state) {
        List<DecisionTreeNode> selected = m_graph.getSelectedSubtree();
        Set<RowKey> covPat = new HashSet<RowKey>();
        for (DecisionTreeNode node : selected) {
            covPat.addAll(node.coveredPattern());
        }
        if (state) {
            m_hiLiteHdl.fireHiLiteEvent(covPat);
        } else {
            m_hiLiteHdl.fireUnHiLiteEvent(covPat);
        }
    }

    /*
     * Create menu to control hiliting.
     *
     * @return A new JMenu with hiliting buttons
     */
    private JMenu createHiLiteMenu() {
        final JMenu result = new JMenu(HiLiteHandler.HILITE);
        result.setMnemonic('H');
        JMenuItem item =
                new JMenuItem(HiLiteHandler.HILITE_SELECTED + " Branch");
        item.setMnemonic('S');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                assert (m_hiLiteHdl != null);
                m_graph.hiliteSelectedBranch(true);
                updateHiLite(true);
            }
        });
        result.add(item);
        item = new JMenuItem(HiLiteHandler.UNHILITE_SELECTED + " Branch");
        item.setMnemonic('U');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                assert (m_hiLiteHdl != null);
                m_graph.hiliteSelectedBranch(false);
                updateHiLite(false);
            }
        });
        result.add(item);
        item = new JMenuItem(HiLiteHandler.CLEAR_HILITE);
        item.setMnemonic('C');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                assert (m_hiLiteHdl != null);
                m_graph.clearHilite();
                m_hiLiteHdl.fireClearHiLiteEvent();
            }
        });
        result.add(item);

        return result;
    }

    /**
     *
     * @author Heiko Hofer
     */
    private static class DecTreeGraphView extends
            HierarchicalGraphView<DecisionTreeNode> {

        /**
         * @param root
         */
        public DecTreeGraphView(final DecisionTreeNode root) {
            super(root);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NodeWidgetFactory<DecisionTreeNode> getNodeWidgetFactory() {
            return new DecTreeNodeWidgetFactory(this);
        }
    }
}
