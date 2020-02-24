/*
 * ------------------------------------------------------------------------
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Mar 30, 2008 (wiswedel): created
 */
package org.knime.core.node.config;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

import org.knime.core.node.config.ConfigEditTreeModel.ConfigEditTreeNode;
import org.knime.core.node.workflow.FlowObjectStack;

/**
 * Renderer implementation of a {@link ConfigEditJTree}. It uses a {@link ConfigEditTreeNodePanel} to display the
 * individual entries.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
public class ConfigEditTreeRenderer extends DefaultTreeCellRenderer {
    private final ConfigEditTreeNodePanel m_panelFull;
    private final ConfigEditTreeNodePanel m_panelPlain;
    private ConfigEditTreeNodePanel m_active;
    private int m_currentCellPathDepth;

    private final Rectangle m_paintBounds;

    private final ConfigEditJTree m_parentTree;

    /**
     * Only creates fields.
     *
     * @param owningTree the parent tree
     * @since 4.2
     */
    public ConfigEditTreeRenderer(final ConfigEditJTree owningTree) {
        m_panelPlain = new ConfigEditTreeNodePanel(false, this);
        m_panelFull = new ConfigEditTreeNodePanel(true, this);
        m_active = m_panelPlain;

        m_paintBounds = new Rectangle();

        m_parentTree = owningTree;
    }

    ConfigEditJTree getParentTree() {
        return m_parentTree;
    }

    /** {@inheritDoc} */
    @Override
    public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean isSelected,
                                                  final boolean expanded, final boolean leaf, final int row,
                                                  final boolean isFocused) {
        if (value instanceof TreeNode) {
            m_currentCellPathDepth = m_parentTree.getModel().getPathToRoot((TreeNode)value).length;
        } else {
            m_currentCellPathDepth = 0;
        }
        setValue(tree, value);

        return super.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, isFocused);
    }

    /**
     * Called whenever a new value is to be renderer, updates underlying component.
     *
     * @param tree The associated tree (get the flow object stack from.)
     * @param value to be renderer, typically a <code>ConfigEditTreeNode</code>
     */
    public void setValue(final JTree tree, final Object value) {
        final ConfigEditTreeNode node;
        if (value instanceof ConfigEditTreeNode) {
            node = (ConfigEditTreeNode)value;
            m_active = node.isLeaf() ? m_panelFull : m_panelPlain;
        } else {
            node = null;
            m_active = m_panelPlain;
        }
        FlowObjectStack stack = null;
        if (tree instanceof ConfigEditJTree) {
            stack = ((ConfigEditJTree)tree).getFlowObjectStack();
        }
        m_active.setFlowObjectStack(stack);
        m_active.setTreeNode(node);
        setLeafIcon(m_active.getIcon());
        setOpenIcon(m_active.getIcon());
        setClosedIcon(m_active.getIcon());
        setToolTipText(m_active.getToolTipText());
    }

    /** {@inheritDoc} */
    @Override
    public void setText(final String text) {
        // empty
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

    /**
     * This method is used by our node panels to calculate their preferred size.
     *
     * @param i a potential icon to be rendered
     * @return the total width insets, including accounting for the indent due to tree path depth
     */
    int getTotalWidthInsets(final Icon i) {
        final Insets insets = getInsets();
        final int iconWidth = (i != null) ? (i.getIconWidth() + 2 * getIconTextGap()) : 0;

        return insets.left + iconWidth + insets.right + (m_currentCellPathDepth * 20);
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(final Graphics g) {
        int maxLabelWidth = m_parentTree.labelWidthToEnforce();
        final JLabel keyLabel = m_active.getKeyLabel();
        final FontMetrics fm = g.getFontMetrics(keyLabel.getFont());
        final Dimension labelSize = keyLabel.getSize();
        final int textWidth = fm.stringWidth(keyLabel.getText());
        if (textWidth > maxLabelWidth) {
            final Insets insets = keyLabel.getInsets();
            maxLabelWidth = textWidth + insets.left + insets.right;
        }
        if (labelSize.width < maxLabelWidth) {
            keyLabel.setSize(maxLabelWidth, labelSize.height);
            keyLabel.setPreferredSize(new Dimension(maxLabelWidth, labelSize.height));
            keyLabel.invalidate();
        }

        final Insets insets = getInsets();
        final int iconWidth = (getIcon() != null) ? (getIcon().getIconWidth() + 2 * getIconTextGap()) : 0;
        final int x = insets.left + iconWidth;
        final int y = insets.top;
        final int width = m_parentTree.getWidth() - insets.left - iconWidth - insets.right;
        final int height = getHeight() - insets.top - insets.bottom;
        m_paintBounds.setBounds(x, y, width, height);

        m_active.setSize(m_active.getPreferredSize());
        m_active.validate();
        m_active.setBackground(selected ? getBackgroundSelectionColor()
                                        : getBackgroundNonSelectionColor());
        SwingUtilities.paintComponent(g, m_active, this, m_paintBounds);

        m_parentTree.renderedKeyLabelWithWidth(keyLabel.getSize().width);

        super.paintComponent(g);
    }
}
