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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.cluster.kmeans;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;


/**
 * @author Michael Berthold, University of Konstanz
 */
public class ClusterNodeView extends NodeView<ClusterNodeModel>
    implements HiLiteListener {

    // private static final NodeLogger LOGGER = NodeLogger.getLogger(
    // PMMLClusterNodeView.class);

    // components holding information about ClusterModel
    private final JTree m_jtree; // contents of this tree will be updated

    private Set<RowKey> m_selected;

    private static final String HILITE = "Hilite selected";

    private static final String UNHILITE = "Unhilte selected";

    private static final String UNHILITE_ALL = "Clear hilite";

    private JMenu m_hiliteMenu;

    private static final int HILITE_POS = 0;

    private static final int UNHILITE_POS = 1;

    private static final int CLEAR_POS = 2;

    /**
     * Constructor - set name of view and
     * {@link org.knime.core.node.NodeModel}.
     *
     * @param nodeModel the underlying model
     */
    public ClusterNodeView(final ClusterNodeModel nodeModel) {
        super(nodeModel);
        JComponent myComp = new JPanel();
        myComp.setLayout(new BorderLayout());
        m_selected = new HashSet<RowKey>();
        m_jtree = new JTree();
        m_jtree.setCellRenderer(new ClusterTreeCellRenderer());
        m_jtree.addMouseListener(new MouseAdapter() {
            private boolean m_openPopup = false;

            @Override
            public void mouseReleased(final MouseEvent e) {
                if (!e.isControlDown()) {
                    m_selected.clear();
                }
                m_openPopup = false;
                if (m_jtree.getSelectionPaths() == null) {
                    return;
                }
                for (TreePath path : m_jtree.getSelectionPaths()) {
                    if (path.getLastPathComponent()
                            instanceof ClusterMutableTreeNode) {
                        RowKey rowKey = ((ClusterMutableTreeNode)path
                                .getLastPathComponent()).getRowId();
                        m_selected.add(rowKey);
                        m_openPopup = true;
                    }
                }
                if (e.isPopupTrigger() && m_openPopup) {
                    openPopupMenu(e);
                }
            }
        });
        myComp.add(new JScrollPane(m_jtree));
        myComp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(final MouseEvent e) {
                boolean selected = m_selected.size() > 0;
                m_hiliteMenu.getMenuComponent(HILITE_POS).setEnabled(selected);
                boolean hasHilite = getNodeModel()
                        .getHiLiteHandler().getHiLitKeys().size() > 0;
                m_hiliteMenu.getMenuComponent(UNHILITE_POS).setEnabled(
                        selected && hasHilite);
                m_hiliteMenu.getMenuComponent(CLEAR_POS).setEnabled(
                        selected && hasHilite);
            }
        });
        m_hiliteMenu = getHiLiteMenu();
        super.getJMenuBar().add(m_hiliteMenu);
        super.setComponent(myComp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ClusterNodeModel getNodeModel() {
        return super.getNodeModel();
    }

    private JMenu getHiLiteMenu() {
        JMenu menu = new JMenu("Hilite");
        JMenuItem item = new JMenuItem(HILITE);
        item.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent arg0) {
                getNodeModel().getHiLiteHandler().fireHiLiteEvent(m_selected);
            }
        });
        menu.add(item);
        item = new JMenuItem(UNHILITE);
        item.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent arg0) {
                getNodeModel().getHiLiteHandler().fireUnHiLiteEvent(m_selected);
            }
        });
        menu.add(item);
        item = new JMenuItem(UNHILITE_ALL);
        item.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent arg0) {
                getNodeModel().getHiLiteHandler().fireClearHiLiteEvent();
            }
        });
        menu.add(item);
        return menu;
    }

    private void openPopupMenu(final MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem(HILITE);
        item.addActionListener(new ActionListener() {

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent arg0) {
                getNodeModel().getHiLiteHandler().fireHiLiteEvent(m_selected);
            }

        });
        menu.add(item);
        item = new JMenuItem(UNHILITE);
        item.addActionListener(new ActionListener() {

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent arg0) {
                getNodeModel().getHiLiteHandler().fireUnHiLiteEvent(m_selected);
            }

        });
        menu.add(item);
        item = new JMenuItem(UNHILITE_ALL);
        item.addActionListener(new ActionListener() {

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent arg0) {
                getNodeModel().getHiLiteHandler().fireClearHiLiteEvent();
            }
        });
        menu.add(item);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * {@inheritDoc}
     */
    public void hiLite(final KeyEvent event) {
        getComponent().repaint();
        getNodeModel().getHiLiteHandler().fireHiLiteEvent(event.keys());
    }

    /**
     * {@inheritDoc}
     */
    public void unHiLite(final KeyEvent event) {
        getComponent().repaint();
        getNodeModel().getHiLiteHandler().fireUnHiLiteEvent(event.keys());
    }


    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll(final KeyEvent event) {
        getNodeModel().getHiLiteHandler().fireClearHiLiteEvent();
        getComponent().repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        getNodeModel().getHiLiteHandler().addHiLiteListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // nothing to do, listeners are unregistered elsewhere
        getNodeModel().getHiLiteHandler().removeHiLiteListener(this);
    }

    /**
     * Update content of view - in this case fill TreeModel of JTree with new
     * information or a message indicating nonexistend or erronous model.
     *
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public final void modelChanged() {
        DefaultMutableTreeNode root = null;
        if (getNodeModel() == null) { // do we even have a NodeModel?
            root = new DefaultMutableTreeNode("No Model");
        } else { // check for empty cluster model before adding content to
            // tree
            ClusterNodeModel myModel = getNodeModel();
            if (!myModel.hasModel()) {
                root = new DefaultMutableTreeNode("Empty Model");
            } else { // put cluster info into the tree
                root = new DefaultMutableTreeNode(myModel.getNumClusters()
                        + " Clusters");
                DefaultMutableTreeNode clusterParent; // TreeNode for cluster
                // c
                for (int c = 0; c < myModel.getNumClusters(); c++) {
                    // add information about coverage and center vector
                    clusterParent = new ClusterMutableTreeNode(
                            ClusterNodeModel.CLUSTER + c + " (coverage: "
                                    + myModel.getClusterCoverage(c) + ")",
                            new RowKey(ClusterNodeModel.CLUSTER + c));
                    for (int i = 0; i < myModel.getNrUsedColumns(); i++) {
                        clusterParent.add(new DefaultMutableTreeNode(
                                  myModel.getFeatureName(i) + " = "
                                + myModel.getClusterCenter(c)[i]));
                    }
                    root.add(clusterParent);
                }
            }
        }
        // set new JTreeModel
        m_jtree.setModel(new DefaultTreeModel(root));
    }

    private static class ClusterMutableTreeNode extends DefaultMutableTreeNode {

        private final RowKey m_rowId;

        /**
         * Constructor like super but stores also the row key for hiliting
         * purposes.
         *
         * @param o the object to be displayed
         * @param rowId the row key srtored internally
         */
        public ClusterMutableTreeNode(final Object o, final RowKey rowId) {
            super(o);
            m_rowId = rowId;
        }

        /**
         * @return the internally stored row key of the cluster
         */
        public RowKey getRowId() {
            return m_rowId;
        }

    }

    private class ClusterTreeCellRenderer extends DefaultTreeCellRenderer {

        private boolean m_isHilite = false;

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTreeCellRendererComponent(final JTree tree,
                final Object value, final boolean sel, final boolean expanded,
                final boolean leaf, final int row, final boolean hasFoc) {
            if (value instanceof ClusterMutableTreeNode) {
                // can cast and check whether it is hilited
                ClusterMutableTreeNode node = (ClusterMutableTreeNode)value;
                m_isHilite = getNodeModel()
                        .getHiLiteHandler().isHiLit(node.getRowId());
            } else {
                m_isHilite = false;
            }
            if (sel) {
                if (m_isHilite) {
                    setBackgroundSelectionColor(ColorAttr.SELECTED_HILITE);
                } else {
                    setBackgroundSelectionColor(ColorAttr.SELECTED);
                }
            } else {
                if (m_isHilite) {
                    setBackgroundNonSelectionColor(ColorAttr.HILITE);
                } else {
                    setBackgroundNonSelectionColor(ColorAttr.BACKGROUND);
                }
            }
            setTextSelectionColor(Color.BLACK);
            setTextNonSelectionColor(Color.BLACK);
            return super.getTreeCellRendererComponent(tree, value, sel,
                    expanded, leaf, row, hasFoc);
        }
    }
}
