/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Mar 30, 2008 (wiswedel): created
 */
package org.knime.core.node.config;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.knime.core.node.config.ConfigEditTreeModel.ConfigEditTreeNode;
import org.knime.core.node.workflow.ScopeObjectStack;

/**
 * Renderer implementation of a {@link ConfigEditJTree}. It uses
 * a {@link ConfigEditTreeNodePanel} to display the individual entries.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ConfigEditTreeRenderer extends DefaultTreeCellRenderer {
    
    private final ConfigEditTreeNodePanel m_panelFull;
    private final ConfigEditTreeNodePanel m_panelPlain;
    private ConfigEditTreeNodePanel m_active;
    
    /** Only creates fields. */
    public ConfigEditTreeRenderer() {
        m_panelFull = new ConfigEditTreeNodePanel(true);
        m_panelPlain = new ConfigEditTreeNodePanel(false);
        m_active = m_panelPlain;
    }
    
    /** {@inheritDoc} */
    @Override
    public Component getTreeCellRendererComponent(
            final JTree tree, final Object value, final boolean isSelected, 
            final boolean expanded, final boolean leaf, final int row,
            final boolean isFocused) {
        setValue(tree, value);
        return super.getTreeCellRendererComponent(
                tree, value, isSelected, expanded, leaf, row, isFocused);
    }
    
    /** Called whenever a new value is to be renderer, updates underlying
     * component. 
     * @param tree The associated tree (get the scope object stack from.)
     * @param value to be renderer, typically a <code>ConfigEditTreeNode</code>
     */
    public void setValue(final JTree tree, final Object value) {
        ConfigEditTreeNode node;
        if (value instanceof ConfigEditTreeNode) {
            node = (ConfigEditTreeNode)value;
            m_active = node.isLeaf() ? m_panelFull : m_panelPlain;
        } else {
            node = null;
            m_active = m_panelPlain;
        }
        ScopeObjectStack stack = null;
        if (tree instanceof ConfigEditJTree) {
            stack = ((ConfigEditJTree)tree).getScopeStack();
        }
        m_active.setScopeStack(stack);
        m_active.setTreeNode(node);
        setLeafIcon(m_active.getIcon());
        setOpenIcon(m_active.getIcon());
        setClosedIcon(m_active.getIcon());
        setToolTipText(m_active.getToolTipText());
    }
    
    /** {@inheritDoc} */
    @Override
    public void setText(final String text) {
    }
    
    /** {@inheritDoc} */
    @Override
    public Dimension getPreferredSize() {
        Dimension r = super.getPreferredSize();
        Dimension panelSize = m_active.getPreferredSize();
        if (r == null) {
            return panelSize;
        }
        int width = Math.max(panelSize.width + getIconTextGap() + 16, r.width);
        int height = 4 + Math.max(r.height, panelSize.height);
        return new Dimension(width, height);
    }
    
    private final Rectangle m_rectangle = new Rectangle();
    
    /** {@inheritDoc} */
    @Override
    protected void paintComponent(final Graphics g) {
        Insets ins = getInsets();
        int iconWidth = getIcon() != null 
            ? getIcon().getIconWidth() + 2 * getIconTextGap() : 0;
        int x = ins.left + iconWidth;
        int y = ins.top;
        int width = getWidth() - ins.left - iconWidth - ins.right;
        int height = getHeight() - ins.top - ins.bottom;
        m_rectangle.setBounds(x, y, width, height);
        Dimension d = new Dimension(width, height);
        m_active.setSize(d);
        m_active.validate();
        m_active.setBackground(selected ? getBackgroundSelectionColor() 
                : getBackgroundNonSelectionColor());
        SwingUtilities.paintComponent(g, m_active, this, m_rectangle);
        super.paintComponent(g);
    }
    

}
