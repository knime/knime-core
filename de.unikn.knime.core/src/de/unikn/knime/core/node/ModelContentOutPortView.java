/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   26.10.2005 (gabriel): created
 *   15.05.1006 (sieb&ohl): reviewed
 */
package de.unikn.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 * A port view showing the port's ModelContent description.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ModelContentOutPortView extends NodeOutPortView {

    /** Shows the ModelContent as JTree. */
    private final JTree m_tree;

    /** If no ModelContent available. */
    private static final TreeNode NO_TEXT 
        = new DefaultMutableTreeNode("<No Model>", false);

    /**
     * A view showing the data model stored in the specified ModelContent
     * ouput port.
     * 
     * @param nodeName Name of the node the inspected port belongs to. Will
     *            be part of the frame's title.
     * @param portName Name of the port to view the ModelContent from. Will
     *            be part of the frame's title.
     * 
     */
    ModelContentOutPortView(final String nodeName, final String portName) {
        super(nodeName + ", " + portName);
        m_tree = new JTree();
        m_tree.setEditable(false);
        m_tree.setFont(new Font("Courier", Font.PLAIN, 12));
        Container cont = getContentPane();
        cont.setLayout(new BorderLayout());
        cont.setBackground(NodeView.COLOR_BACKGROUND);
        cont.add(new JScrollPane(m_tree), BorderLayout.CENTER);
    }

    /**
     * Updates the view's content with new ModelContent object.
     * 
     * @param predParams The new content can be null.
     */
    void updatePredictorParams(final ModelContent predParams) {
        m_tree.removeAll();
        if (predParams == null) {
            m_tree.setModel(new DefaultTreeModel(NO_TEXT));
        } else {
            //String text = predParams.toString();
            m_tree.setModel(new DefaultTreeModel(predParams));
        }
        super.updatePortView();
    }

}
