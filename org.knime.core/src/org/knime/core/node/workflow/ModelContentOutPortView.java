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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   26.10.2005 (gabriel): created
 *   15.05.1006 (sieb&ohl): reviewed
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeView;

/**
 * A port view showing the port's <code>ModelContent</code> as
 * <code>JTree</code>.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ModelContentOutPortView extends JComponent {

    /** Shows the ModelContent as JTree. */
    private final JTree m_tree;

    /** If no ModelContent available. */
    private static final TreeNode NO_TEXT
        = new DefaultMutableTreeNode("<No Model>", false);

    private final JPopupMenu m_treePopup = new JPopupMenu();

    private final ModelContentRO m_portObject;

    /**
     * A view showing the data model stored in the specified ModelContent
     * output port.
     *
     * @param portObject the port object whose content should be displayed
     *
     */
    public ModelContentOutPortView(final ModelContentRO portObject) {
        m_portObject = portObject;
        setName(portObject.getKey());
        m_tree = new JTree();
        m_tree.setEditable(false);
        m_tree.setFont(new Font("Courier", Font.PLAIN, 12));
        m_tree.setLargeModel(true);
        m_tree.setRowHeight(20);
        m_treePopup.setLightWeightPopupEnabled(false);
        final JMenuItem expand = new JMenuItem("Expand");
        expand.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                TreePath[] tps = m_tree.getSelectionPaths();
                if (tps != null) {
                    for (TreePath tp : tps) {
                        expandAll(tp);
                    }
                }
            }
        });
        m_treePopup.add(expand);
        final JMenuItem collapse = new JMenuItem("Collapse");
        collapse.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                TreePath[] tps = m_tree.getSelectionPaths();
                if (tps != null) {
                    for (TreePath tp : tps) {
                        collapseAll(tp);
                    }
                }
            }
        });
        m_treePopup.add(collapse);
        m_tree.add(m_treePopup);
        m_tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (e.isPopupTrigger() && m_tree.getSelectionPaths() != null) {
                    m_treePopup.show(m_tree, e.getX(), e.getY());
                }
            }
           @Override
           public void mousePressed(final MouseEvent e) {
               if (e.isPopupTrigger() && m_tree.getSelectionPaths() != null) {
                   m_treePopup.show(m_tree, e.getX(), e.getY());
               }
           }
        });
        setLayout(new BorderLayout());
        setBackground(NodeView.COLOR_BACKGROUND);
        add(new JScrollPane(m_tree), BorderLayout.CENTER);
        update();
    }

    private void expandAll(final TreePath parent) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(path);
            }
        }
        m_tree.expandPath(parent);
    }

    private void collapseAll(final TreePath parent) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                collapseAll(path);
            }
        }
        m_tree.collapsePath(parent);
    }

    /**
     * Updates the view's content with new ModelContent object.
     *
     */
    private void update() {
        m_tree.removeAll();
        if (m_portObject == null) {
            m_tree.setModel(new DefaultTreeModel(NO_TEXT));
        } else {
            //String text = predParams.toString();
            m_tree.setModel(new DefaultTreeModel(m_portObject));
        }
    }

    /**
     * Removes all nodes from the tree and sets the model to <code>null</code>.
     */
    public void dispose() {
        m_tree.removeAll();
        m_tree.setModel(null);
    }
}
