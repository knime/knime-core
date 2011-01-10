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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.mine.decisiontree2.learner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeRenderer;
import org.knime.core.data.RowKey;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * This node view is exactly the same as the one for the c4.5 encapsulating
 * view.
 *
 * @author mb, University of Konstanz
 */
public class DecTreeNodeView extends NodeView<DecisionTreeLearnerNodeModel> {

    private JTree m_jTree;

    private HiLiteHandler m_hiLiteHdl;

    private JMenu m_hiLiteMenu;

    /**
     * Default constructor, taking the model as argument.
     *
     * @param model the underlying NodeModel
     */
    public DecTreeNodeView(final DecisionTreeLearnerNodeModel model) {
        super(model);
//        assert (model instanceof DecisionTreeLearnerNodeModel);
        m_jTree = new JTree();
        m_jTree.putClientProperty("JTree.lineStyle", "Angled");
        m_jTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        m_jTree.setRootVisible(true);
        // try to viz it...
        JScrollPane treeView = new JScrollPane(m_jTree);
        setComponent(treeView);
        // retrieve HiLiteHandler from Input port
        m_hiLiteHdl = ((model)
                .getInHiLiteHandler(DecisionTreeLearnerNodeModel.DATA_INPORT));
        // and add menu entries for HiLite-ing
        m_hiLiteMenu = this.createHiLiteMenu();
        this.getJMenuBar().add(m_hiLiteMenu);
        m_hiLiteMenu.setEnabled(m_hiLiteHdl != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        NodeModel model = this.getNodeModel();
        DecisionTree dt = ((DecisionTreeLearnerNodeModel)model).
                                getDecisionTree();
        if (dt != null) {
            // set new model
            m_jTree.setModel(new DefaultTreeModel(dt.getRootNode()));
            // change default renderer
            m_jTree.setCellRenderer(new DecisionTreeNodeRenderer());
            // make sure no default height is assumed (the renderer's
            // preferred size should be used instead)
            m_jTree.setRowHeight(0);
            // retrieve HiLiteHandler from Input port
            m_hiLiteHdl = (((DecisionTreeLearnerNodeModel)model)
                    .getInHiLiteHandler(DecisionTreeLearnerNodeModel.
                            DATA_INPORT));
            // and adjust menu entries for HiLite-ing
            m_hiLiteMenu.setEnabled(m_hiLiteHdl != null);
        } else {
            m_jTree.setModel(null);
        }
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

    // /////////////////////////////
    // routines for HiLite Support
    // /////////////////////////////

    /*
     * hilite or unhilite all items that are covered by currently selected
     * branches in the tree
     *
     * @param state if true hilite, otherwise unhilite selection
     */
    private void changeSelectedHiLite(final boolean state) {
        TreePath[] selectedPaths = m_jTree.getSelectionPaths();
        if (selectedPaths == null) {
            return; // nothing selected
        }
        for (int i = 0; i < selectedPaths.length; i++) {
            assert (selectedPaths[i] != null);
            if (selectedPaths[i] == null) {
                return;
            }
            TreePath path = selectedPaths[i];
            Object lastNode = path.getLastPathComponent();
            assert (lastNode != null);
            assert (lastNode instanceof DecisionTreeNode);
            Set<RowKey> covPat = ((DecisionTreeNode)lastNode)
                    .coveredPattern();
            if (state) {
                m_hiLiteHdl.fireHiLiteEvent(covPat);
            } else {
                m_hiLiteHdl.fireUnHiLiteEvent(covPat);
            }
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
        JMenuItem item = new JMenuItem(HiLiteHandler.HILITE_SELECTED
                + " Branch");
        item.setMnemonic('S');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                assert (m_hiLiteHdl != null);
                changeSelectedHiLite(true);
            }
        });
        result.add(item);
        item = new JMenuItem(HiLiteHandler.UNHILITE_SELECTED + " Branch");
        item.setMnemonic('U');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                assert (m_hiLiteHdl != null);
                changeSelectedHiLite(false);
            }
        });
        result.add(item);
        item = new JMenuItem(HiLiteHandler.CLEAR_HILITE);
        item.setMnemonic('C');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                assert (m_hiLiteHdl != null);
                m_hiLiteHdl.fireClearHiLiteEvent();
            }
        });
        result.add(item);
        // listener when the hilite handler changes (disable/enable the menu)
        // TODO
        /*
         * PropertyChangeListener hiliterChangeListener = new
         * PropertyChangeListener() { public void propertyChange(final
         * PropertyChangeEvent evt) { result.setEnabled(tView.hasData() &&
         * tView.hasHiLiteHandler()); } };
         * tView.getContentModel().addPropertyChangeListener(
         * hiliterChangeListener); result.setEnabled(tView.hasData() &&
         * tView.hasHiLiteHandler());
         */
        return result;
    } // createHiLiteMenu()

}
