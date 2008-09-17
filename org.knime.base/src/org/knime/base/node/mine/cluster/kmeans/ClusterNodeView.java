/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;


/**
 * @author Michael Berthold, University of Konstanz
 */
public class ClusterNodeView extends GenericNodeView<ClusterNodeModel>
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

    private class ClusterMutableTreeNode extends DefaultMutableTreeNode {

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
