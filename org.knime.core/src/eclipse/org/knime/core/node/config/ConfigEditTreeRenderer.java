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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

import org.eclipse.core.runtime.Platform;
import org.knime.core.node.config.ConfigEditTreeModel.ConfigEditTreeNode;
import org.knime.core.node.workflow.FlowObjectStack;

/**
 * Renderer implementation of a {@link ConfigEditJTree}. It uses a {@link ConfigEditTreeNodePanel} to display the
 * individual entries.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
// TODO: consider making this class package-scope
public class ConfigEditTreeRenderer extends DefaultTreeCellRenderer {
    static final boolean PLATFORM_IS_MAC = Platform.OS_MACOSX.equals(Platform.getOS());
    private final int PIXEL_INDENT_PER_PATH_DEPTH = 22;


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
        m_panelPlain = new ConfigEditTreeNodePanel(false, this, false);
        m_panelFull = new ConfigEditTreeNodePanel(true, this, false);
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
        m_active.setTreePathDepth(m_currentCellPathDepth);
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
        final Dimension r = super.getPreferredSize();
        final Dimension panelSize = m_active.getPreferredSize();
        final int computedMinimumWidth = m_active.computeMinimumWidth();
        final int panelWidthToUse = Math.max(panelSize.width, computedMinimumWidth);
        final Dimension viewportSize = viewportSize();
        final Insets insets = getInsets();
        if (r == null) {
            if (viewportSize.width > 0) {
                return new Dimension(viewportSize.width, (panelSize.height + insets.top + insets.bottom));
            } else {
                return new Dimension(panelWidthToUse, (panelSize.height + insets.top + insets.bottom));
            }
        }
        final int panelInfluencedWidth = panelWidthToUse + getIconTextGap() + 16;
        final int otherWidth = (viewportSize.width > 0) ? (int)(viewportSize.width * 0.8) - PIXEL_INDENT_PER_PATH_DEPTH
                                                        : r.width;
        final int width = Math.max(panelInfluencedWidth, otherWidth) + (PLATFORM_IS_MAC ? 0 : 10)
                                + insets.left + insets.right;
        final int height = 4 + Math.max(r.height, panelSize.height);

        return new Dimension(width, height);
    }

    private Dimension viewportSize() {
        final Container panel = m_parentTree.getParent();
        if (panel != null) {
            final Container panelParent = panel.getParent();
            final JViewport jv;

            if (panelParent instanceof JViewport) {
                jv = (JViewport)panelParent;
            } else if (panelParent instanceof JScrollPane) {
                jv = ((JScrollPane)panelParent).getViewport();
            } else {
                jv = null;
            }

            if (jv != null) {
                return jv.getViewSize();
            }
        }

        return new Dimension(-1, -1);
    }

    /**
     * This method is used by our node panels to calculate their preferred size.
     *
     * @param i a potential icon to be rendered
     * @return the total width insets, including accounting for the indent due to tree path depth
     */
    int getTotalWidthInsets(final Icon i) {
        final Insets insets = getInsets();
        final int iconWidth = drawWidthForIcon(i);

        return insets.left + iconWidth + insets.right + (m_currentCellPathDepth * PIXEL_INDENT_PER_PATH_DEPTH);
    }

    private int drawWidthForIcon(final Icon i) {
        return (i != null) ? (i.getIconWidth() + 2 * getIconTextGap()) : 0;
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(final Graphics g) {
        /*
         * Arguably, a lot of this size calculation stuff should occur in an override of #validate() and not
         *  here.
         */
        final int newKeyWidth = m_active.updateKeyLabelSize(g);

        final Insets insets = getInsets();
        // the x, y and width nudges are due to the editor placing the node panel in a different location
        //      than where this component renders
        final int x = insets.left + drawWidthForIcon(getIcon()) - 4;
        final int y = insets.top + (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH ? 2 : -1);
        final int widthToUse;
        if (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
            widthToUse = m_parentTree.getWidth();
        } else {
            final int paneMinimumWidth = m_active.computeMinimumWidth();
            final int thisWidth = getWidth();
            widthToUse = (thisWidth > paneMinimumWidth) ? thisWidth : paneMinimumWidth;
        }
        final int width = widthToUse + (PLATFORM_IS_MAC ? 0 : 10) + 4;
        final int height = getHeight() - insets.top - insets.bottom + 2;
        m_paintBounds.setBounds(x, y, width, height);

        final Dimension paneSize;
        if (ConfigEditJTree.ROW_SHOULD_FILL_WIDTH) {
            paneSize = m_active.getPreferredSize();
        } else {
            paneSize = new Dimension(width, height);
        }
        m_active.setSize(paneSize);
        m_active.validate();
        m_active.setBackground(selected ? getBackgroundSelectionColor()
                                        : getBackgroundNonSelectionColor());
        SwingUtilities.paintComponent(g, m_active, this, m_paintBounds);

        m_parentTree.renderedKeyLabelAtDepthWithWidth(m_currentCellPathDepth, newKeyWidth);
        m_active.recordPostPaintPreferredSize(m_currentCellPathDepth, paneSize);

        super.paintComponent(g);
    }
}
