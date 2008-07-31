/* 
 * 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   05.08.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree.predictor.decisiontree;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * Renderer for a DecisionTreeNode within a JTree.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class DecisionTreeNodeRenderer extends DefaultTreeCellRenderer {
    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTreeCellRendererComponent(final JTree tree,
            final Object value, final boolean sel, final boolean expanded,
            final boolean leaf, final int row, final boolean hasFoc) {
        DecisionTreeNodeView rend = new DecisionTreeNodeView(
                (DecisionTreeNode)value);
        if (!sel) {
            rend.setBackground(super.backgroundNonSelectionColor);
            rend.setBorder(BorderFactory.createLineBorder(
                    super.backgroundNonSelectionColor, /* width= */2));
        } else {
            rend.setBackground(super.backgroundNonSelectionColor);
            rend.setBorder(BorderFactory.createLineBorder(super
                    .getBorderSelectionColor(), /* width= */2));
        }
        return rend;
    }
}
