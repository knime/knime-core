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
 *   22.07.2010 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.view.graph;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import org.knime.core.data.property.ColorAttr;

/**
 * A widget for a tree.
 *
 * @author Heiko Hofer
 * @param <K> The type of the nodes user objects
 */
//DefaultMutableTreeNode does not support generics.
@SuppressWarnings("unchecked")
public abstract class HierarchicalGraphView<K> {
    /** The alignment of the graph. */
    public enum Align { /** Root is in top left corner. */ Left,
        /** Root is centered above the graph. */ Center
        };

    private boolean m_leftAligned;
    private Map<K, NodeWidget<K>> m_widgets;
    private Map<K, Rectangle> m_visible;
    private Map<Rectangle, DefaultMutableTreeNode> m_collapseSign;
    private Map<Rectangle, DefaultMutableTreeNode> m_expandSign;
    private DefaultMutableTreeNode m_root;
    private Set<K> m_collapsed;
    private JComponent m_component;
    private LayoutSettings m_layoutSettings;
    private K m_selected;
    private Set<K> m_hilited;
    private List<GraphListener> m_graphListeners;
    private int m_nodeWidth;
    private Map<K, DefaultMutableTreeNode> m_treeMap;
    private Map<Rectangle, String> m_toolTips;

    /**
     * Create a new instance.
     * @param root the root of the graph
     */
    public HierarchicalGraphView(final K root) {
        this(root, Align.Center);
    }

    /**
     * Create a new instance.
     * @param root the root of the tree
     * @param alignment the alignment of the graph
     */
    public HierarchicalGraphView(final K root, final Align alignment) {
        m_leftAligned = alignment.equals(Align.Left);
        m_widgets = new HashMap<K, NodeWidget<K>>();
        m_visible = new HashMap<K, Rectangle>();
        m_collapsed = new HashSet<K>();
        m_collapseSign = new HashMap<Rectangle, DefaultMutableTreeNode>();
        m_expandSign = new HashMap<Rectangle, DefaultMutableTreeNode>();
        m_hilited = new HashSet<K>();
        m_treeMap = new HashMap<K, DefaultMutableTreeNode>();
        m_graphListeners = new ArrayList<GraphListener>();
        m_toolTips = new HashMap<Rectangle, String>();

        m_component = new HierarchicalGraphComponent<K>(this);
        m_nodeWidth = 100;
        init(root);
    }


    private void init(final K root) {
        getWidgets().clear();
        getVisible().clear();
        getCollapsed().clear();
        m_collapseSign.clear();
        m_expandSign.clear();
        m_selected = null;
        m_hilited.clear();
        if (null != root) {
            m_root = new DefaultMutableTreeNode(root);
            m_treeMap.put(root, m_root);
            // recursive build tree
            buildTree(m_root);
            // display root an its children by default
            getVisible().put(root, new Rectangle());
            for (Object o : Collections.list(m_root.children())) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode) o;
                K k = (K)n.getUserObject();
                getVisible().put(k, new Rectangle());
            }
            layoutGraph();
            // With this call parent components get informed about the
            // change in the preferred size.
            m_component.revalidate();
            // make sure that the root node is in the visible area
            m_component.scrollRectToVisible(getVisible().get(root));
        }
    }


    /**
     * Replace the root which is the model for this view.
     *
     * @param root the root
     */
    public void setRootNode(final K root) {
        init(root);
    }


    /**
     * Get the root.
     * @return the root
     */
    public K getRootNode() {
        return null != m_root ? (K)m_root.getUserObject() : null;
    }

    /** Recursively add the children to the given node. The children are
     * obtained from the graphs {@link NodeWidgetFactory}.
     * @param node the node
     */
    private void buildTree(final DefaultMutableTreeNode node) {
        List<K> children =
            getNodeWidgetFactory().getChildren((K)node.getUserObject());
        if (null == children || children.isEmpty()) {
            return;
        }
        List<DefaultMutableTreeNode> nodeChildren =
            new ArrayList<DefaultMutableTreeNode>();
        for (K childK : children) {
            DefaultMutableTreeNode childNode
                    = new DefaultMutableTreeNode(childK);
            node.add(childNode);
            m_treeMap.put(childK, childNode);
            nodeChildren.add(childNode);
        }
        for (DefaultMutableTreeNode childNode : nodeChildren) {
            buildTree(childNode);
        }
    }

    /**
     * Get the {@link NodeWidgetFactory} for this Graph.
     * @return the {@link NodeWidgetFactory}
     */
    public abstract NodeWidgetFactory<K> getNodeWidgetFactory();

    /**
     * Registers a graph listener.
     * Currently only used by the {@link OutlineView}
     * @param listener the graph listener
     */
    final void addGraphListener(final GraphListener listener) {
        m_graphListeners.add(listener);
    }

    /**
     * Unregisters a graph listener.
     * Currently only used by the {@link OutlineView}
     * @param listener the graph listener
     */
    final void removeGraphListener(final GraphListener listener) {
        m_graphListeners.remove(listener);
    }

    /**
     * Returns the {@link JComponent} for the HierarchicalGraphView. This method
     * returns always the same instance.
     *
     * @return the created view
     */
    public final JComponent getView() {
        return m_component;
    }

    /**
     * Creates an outline view, which is a birds eye view on the graph.
     * @return the outline view
     */
    public final JComponent createOutlineView() {
        JComponent outlineView = new OutlineView<K>(this);
        return outlineView;
    }

    /** Re-layout the graph. */
    protected void layoutGraph() {
        if (null == m_root) {
            return;
        }
        m_collapseSign.clear();
        m_expandSign.clear();
        // clear visible
        for (K key : getVisible().keySet()) {
            getVisible().put(key, new Rectangle());
        }
        K rootK = (K)m_root.getUserObject();
        NodeWidget<K> rootWidget = getWidgets().get(rootK);
        if (null == rootWidget) {
            rootWidget = getNodeWidgetFactory().createGraphNode(rootK);
            getWidgets().put(rootK, rootWidget);
        }
        m_layoutSettings = new LayoutSettings(getNodeWidth());

        Stack<DefaultMutableTreeNode> stack =
            new Stack<DefaultMutableTreeNode>();
        LayoutInfo layoutInfo = new LayoutInfo();
        layoutInfo.setYOffset(m_layoutSettings.getTopGap());
        // start at first leaf
        pushtoFirstLeaf(m_root, stack, layoutInfo);
        // recursive layout
        while (!stack.isEmpty()) {
            iterativeLayout(stack, layoutInfo);
        }
        Rectangle graphBounds = computeGraphBounds();
        m_component.setPreferredSize(
                new Dimension(m_layoutSettings.getLeftGap()
                        + graphBounds.width + m_layoutSettings.getRightGap(),
                        m_layoutSettings.getTopGap() + graphBounds.height
                        + m_layoutSettings.getLevelGap() / 2
                        + m_layoutSettings.getBottomGap()));
    }

    /**
     * A function to compute the layout of the graph in a loop. The layout
     * starts from the first leaf.
     * @param stack the parent nodes that must be layouted
     * @param info some constants for the layout
     */
    private void iterativeLayout(
            final Stack<DefaultMutableTreeNode> stack,
            final LayoutInfo layoutInfo) {
        DefaultMutableTreeNode node = stack.pop();
        K k = (K)node.getUserObject();

        NodeWidget<K> widget = getWidgets().get(k);
        Dimension preferredSize = widget.getPreferredSize();
        layoutInfo.setYOffset(layoutInfo.getYOffset() - preferredSize.height
                - m_layoutSettings.getLevelGap());
        Rectangle bounds = new Rectangle(m_layoutSettings.getNodeWidth(),
                preferredSize.height);
        boolean layoutParent;
        if (node.isLeaf()) {
            layoutParent = false;
        } else {
            DefaultMutableTreeNode child =
                (DefaultMutableTreeNode)node.getChildAt(0);
            K childK = (K)child.getUserObject();
            layoutParent = getVisible().containsKey(childK);
        }
        if (layoutParent) {
            // Layout a parent node
            DefaultMutableTreeNode firstChild =
                (DefaultMutableTreeNode)node.getChildAt(0);
            K firstChildK = (K)firstChild.getUserObject();
            DefaultMutableTreeNode lastChild = m_leftAligned ? firstChild
                    : (DefaultMutableTreeNode)node.getChildAt(
                            node.getChildCount() - 1);
            K lastChildK = (K)lastChild.getUserObject();
            Rectangle first = getVisible().get(firstChildK);
            Rectangle last = getVisible().get(lastChildK);
            bounds.x = (last.x + first.x) / 2;
            bounds.y = layoutInfo.getYOffset();
            // This node can be collapsed
            Rectangle collapseSign = m_layoutSettings.getSignBounds(bounds);
            m_collapseSign.put(collapseSign, node);
        } else {
            // Layout a leaf node, which might be the first leaf in the branch
            Rectangle graphBounds = computeGraphBounds();
            bounds.x = 0 != graphBounds.width
                ? graphBounds.x + graphBounds.width
                        + m_layoutSettings.getBranchGap()
                : m_layoutSettings.getLeftGap();
            bounds.y = layoutInfo.m_yOffset;
            if (!node.isLeaf()) {
                // This node can be expanded
                Rectangle expandSign = m_layoutSettings.getSignBounds(bounds);
                m_expandSign.put(expandSign, node);
            }
        }
        assignBounds(node, bounds);
        if (stack.isEmpty()) {
            // layout finished
            return;
        }
        // go ahead with first leaf of parents next child
        DefaultMutableTreeNode parent = stack.peek();
        assert parent == node.getParent();
        DefaultMutableTreeNode nextChild =
            (DefaultMutableTreeNode)parent.getChildAfter(node);
        if (null != nextChild) {
            // go ahead with first leaf of this child
            pushtoFirstLeaf(nextChild, stack, layoutInfo);
        } else {
            // go ahead with parent
            return;
        }
    }


    /**
     * Computes the rectangle the encloses the graph completely.
     * @return the bounds ot the graph
     */
    private Rectangle computeGraphBounds() {
        Rectangle rectangle = new Rectangle();
        for (Rectangle next : getVisible().values()) {
            rectangle = rectangle.union(next);
        }
        for (Rectangle next : m_expandSign.keySet()) {
            rectangle = rectangle.union(next);
        }

        return rectangle;
    }

    /**
     * Push the nodes on the path to the first leaf on the stack. Only visible
     * nodes are regarded.
     * @param node the starting node
     * @param stack the stack
     */
    private DefaultMutableTreeNode pushtoFirstLeaf(
            final DefaultMutableTreeNode node,
            final Stack<DefaultMutableTreeNode> stack,
            final LayoutInfo layoutInfo) {
        DefaultMutableTreeNode n = node;
        boolean continueIteration = continueIteration(n);
        while (continueIteration) {
            pushOnStack(n, stack, layoutInfo);
            n = (DefaultMutableTreeNode)n.getChildAt(0);
            continueIteration = continueIteration(n);
        }
        pushOnStack(n, stack, layoutInfo);
        return n;
    }

    private void pushOnStack(final DefaultMutableTreeNode node,
            final Stack<DefaultMutableTreeNode> stack,
            final LayoutInfo layoutInfo) {
        K k = (K)node.getUserObject();
        stack.push(node);
        NodeWidget<K> widget = getWidgets().get(k);
        if (null == widget) {
            widget = getNodeWidgetFactory().createGraphNode(k);
            getWidgets().put(k, widget);
        }
        Dimension preferredSize = widget.getPreferredSize();
        layoutInfo.setYOffset(layoutInfo.getYOffset() + preferredSize.height
                + m_layoutSettings.getLevelGap());
    }

    /** Returns true when n has visible children. */
    private boolean continueIteration(final DefaultMutableTreeNode n) {
        boolean continueIteration = !n.isLeaf();
        if (!n.isLeaf()) {
            DefaultMutableTreeNode child =
                (DefaultMutableTreeNode)n.getChildAt(0);
            K childK = (K)child.getUserObject();
            continueIteration = getVisible().containsKey(childK);
        }
        return continueIteration;
    }

    /**
     * Assign the given bounds to the node and its {@link NodeWidget}.
     * @param node the node
     * @param bounds the bounds
     */
    private void assignBounds(final DefaultMutableTreeNode node,
            final Rectangle bounds) {
        K k = (K)node.getUserObject();
        getVisible().put((K)node.getUserObject(), bounds);
        NodeWidget<K> widget = getWidgets().get(k);
        if (null == widget) {
            widget = getNodeWidgetFactory().createGraphNode(k);
            getWidgets().put(k, widget);
        }
        widget.setSize(new Dimension(bounds.width, bounds.height));
    }

    /**
     * Paints the nodes, the connectors and the labels on the connectors to
     * the given graphics object. Nodes are painted with different background
     * or border whether they are selected or hilited.
     *
     * @param c the component to paint on
     * @param g the graphics object
     * @param x the x value of the top left corner
     * @param y the y value of the top left corner
     * @param width The width to be paint
     * @param height The height to be paint
     */
    void paint(final Component c, final Graphics2D g, final int x,
            final int y, final int width, final int height) {
        if (null == m_root) {
            return;
        }
        // shared instance used to draw labels
        JLabel label = new JLabel();
        m_toolTips.clear();
        final Paint origPaint = g.getPaint();
        final Stroke origStroke = g.getStroke();
        g.setColor(ColorAttr.BORDER);
        Enumeration<DefaultMutableTreeNode> breadthFirst =
            m_root.breadthFirstEnumeration();
        while (breadthFirst.hasMoreElements()) {
            DefaultMutableTreeNode curr = breadthFirst.nextElement();
            K currK = (K)curr.getUserObject();
            if (!getVisible().containsKey(currK)) {
                continue;
            }
            paintNode(c, curr, g);
            Rectangle bounds = getVisible().get(currK);
            List<Rectangle> visibleChilds =
                new ArrayList<Rectangle>();
            if (!curr.isLeaf()) {
                DefaultMutableTreeNode firstChild =
                    (DefaultMutableTreeNode)curr.getChildAt(0);
                K firstChildK = (K)firstChild.getUserObject();
                if (getVisible().containsKey(firstChildK)) {
                    for (int i = 0; i < curr.getChildCount(); i++) {
                        DefaultMutableTreeNode child =
                            (DefaultMutableTreeNode)curr.getChildAt(i);
                        K childK = (K)child.getUserObject();
                        visibleChilds.add(getVisible().get(childK));
                    }
                }
            }

            if (curr.isLeaf()) {
                // currently do nothing. It would be possible to draw a leaf
                // sign here.
            } else if (visibleChilds.isEmpty()) {
                // draw line to the plus sign
                int xx = bounds.x + bounds.width / 2;
                int yy = bounds.y + bounds.height
                            + m_layoutSettings.getLevelGap() / 2;
                g.drawLine(xx, bounds.y + bounds.height, xx, yy);
            } else {
                // draw connections to the children
                int xx = bounds.x + bounds.width / 2;
                int yy = bounds.y + bounds.height
                            + m_layoutSettings.getLevelGap() / 2;
                g.drawLine(xx, bounds.y + bounds.height, xx, yy);

                Rectangle firstChild = visibleChilds.get(0);
                if (firstChild.x != getVisible().get(currK).x) {
                    GeneralPath path = new GeneralPath();
                    path.moveTo(xx, yy);
                    path.lineTo(firstChild.x + firstChild.width / 2 + 3, yy);
                    path.quadTo(firstChild.x + firstChild.width / 2, yy,
                            firstChild.x + firstChild.width / 2, yy + 3);
                    path.lineTo(firstChild.x + firstChild.width / 2,
                            firstChild.y);
                    g.draw(path);
                } else {
                    g.drawLine(xx, yy, xx, firstChild.y);
                }
                if (visibleChilds.size() > 1) {
                    Rectangle lastChild =
                        visibleChilds.get(visibleChilds.size() - 1);
                    GeneralPath path = new GeneralPath();
                    path.moveTo(xx, yy);
                    path.lineTo(lastChild.x + lastChild.width / 2 - 3, yy);
                    path.quadTo(lastChild.x + lastChild.width / 2, yy,
                            lastChild.x + lastChild.width / 2, yy + 3);
                    path.lineTo(lastChild.x + lastChild.width / 2,
                            lastChild.y);
                    g.draw(path);
                }

                if (visibleChilds.size() > 2) {
                    for (int i = 1; i < visibleChilds.size() - 1; i++) {
                        Rectangle child = visibleChilds.get(i);
                        int xxx = child.x + child.width / 2;
                        g.drawLine(xxx, yy, xxx, child.y);
                    }
                }
            }
            // draw text on the connectors
            NodeWidget<K> widget = getWidgets().get(currK);
            String labelAbove = widget.getConnectorLabelAbove();
            if (null != labelAbove && !labelAbove.isEmpty()) {
                label.setText(labelAbove);
                label.setOpaque(true);
                label.setBackground(ColorAttr.BACKGROUND);
                label.setFont(g.getFont().deriveFont(Font.BOLD | Font.ITALIC));
                int w = Math.min(m_layoutSettings.getNodeWidth() - 10,
                        label.getPreferredSize().width);
                int h = label.getPreferredSize().height;
                int xx = bounds.x + (bounds.width - w) / 2;
                int yy = bounds.y - h - 18;
                Rectangle labelBounds = new Rectangle(xx, yy, w, h);
                m_toolTips.put(labelBounds, labelAbove);
                SwingUtilities.paintComponent(g, label, (Container)c,
                        labelBounds);
            }
            String labelBelow = widget.getConnectorLabelBelow();
            if (null != labelBelow && !labelBelow.isEmpty()
                    && !visibleChilds.isEmpty()) {
                label.setText(labelBelow);
                label.setOpaque(true);
                label.setBackground(ColorAttr.BACKGROUND);
                label.setFont(g.getFont().deriveFont(Font.BOLD | Font.ITALIC));
                int w = Math.min(m_layoutSettings.getNodeWidth() - 10,
                        label.getPreferredSize().width);
                int h = label.getPreferredSize().height;
                int xx = bounds.x + (bounds.width - w) / 2;
                int yy = bounds.y + bounds.height + 10;
                Rectangle labelBounds = new Rectangle(xx, yy, w, h);
                m_toolTips.put(labelBounds, labelBelow);
                SwingUtilities.paintComponent(g, label, (Container)c,
                        labelBounds);
            }
        }
        for (Rectangle bounds : m_collapseSign.keySet()) {
            int yy = bounds.y + bounds.height / 2;
            int delta = 3;
            g.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
            g.drawLine(bounds.x + delta, yy,
                    bounds.x + bounds.width - delta, yy);
        }
        for (Rectangle bounds : m_expandSign.keySet()) {
            int xx = bounds.x + bounds.width / 2;
            int yy = bounds.y + bounds.height / 2;
            int delta = 3;
            g.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
            g.drawLine(bounds.x + delta, yy,
                    bounds.x + bounds.width - delta, yy);
            g.drawLine(xx, bounds.y + delta, xx,
                    bounds.y  + bounds.height - delta);
        }
        g.setPaint(origPaint);
        g.setStroke(origStroke);
        // notify listeners about the repaint
        for (GraphListener listener : m_graphListeners) {
            listener.graphRepaint();
        }
    }

    /**
     * Draw the node, its background and border to the graphics object.
     */
    private void paintNode(final Component c,
            final DefaultMutableTreeNode node,
            final Graphics2D g) {
        K k = (K)node.getUserObject();
        NodeWidget<K> widget = getWidgets().get(k);
        Rectangle bounds = getVisible().get(k);
        boolean selected = m_selected == k;
        boolean hilited = m_hilited.contains(k);
        if (selected && hilited) {
            g.setPaint(ColorAttr.SELECTED_HILITE);
            g.fillRoundRect(bounds.x, bounds.y,
                bounds.width, bounds.height,
                5, 5);
        } else if (selected) {
            g.setPaint(ColorAttr.SELECTED);
        } else if (hilited) {
            g.setPaint(ColorAttr.HILITE);
        } else {
            g.setPaint(ColorAttr.BACKGROUND);
        }
        g.fillRoundRect(bounds.x, bounds.y,
                bounds.width, bounds.height,
                8, 8);

        widget.paint(c, g, bounds);

        if (selected) {
            g.setStroke(new BasicStroke(2f));
        } else {
            g.setStroke(new BasicStroke(1f));
        }
        g.setPaint(ColorAttr.BORDER);
        g.drawRoundRect(bounds.x, bounds.y,
                bounds.width - 1, bounds.height - 1,
                8, 8);
        g.setStroke(new BasicStroke(1f));
    }

    /** Returns the string to be used as the tooltip for event.
     * @param event the mouse event
     * @return the tooltip
     */
    public String getToolTipText(final MouseEvent event) {
        String tip = null;
        Point p = event.getPoint();

        for (Rectangle r : m_toolTips.keySet()) {
            if (r.contains(p)) {
                tip = m_toolTips.get(r);
            }
        }
        return tip;
    }

    /**
     * Handler for the mouse clicked event.
     * @param e the mouse event
     */
    public void mouseClicked(final MouseEvent e) {
        // do nothing
    }

    /**
     * Handler for the mouse released event.
     * @param e the mouse event
     */
    public void mouseReleased(final MouseEvent e) {
        // do nothing
    }

    /**
     * Handler for the mouse pressed event.
     * @param e the mouse event
     */
    public void mousePressed(final MouseEvent e) {
        for (Rectangle r : m_collapseSign.keySet()) {
            if (r.contains(e.getPoint())) {
                // collapse children
                DefaultMutableTreeNode node = m_collapseSign.get(r);
                K userObject = (K)node.getUserObject();
                Enumeration<DefaultMutableTreeNode> enumeration =
                        node.breadthFirstEnumeration();
                // skip starting node
                enumeration.nextElement();
                for (DefaultMutableTreeNode n : Collections.list(enumeration)) {
                    K k = (K)n.getUserObject();
                    if (getCollapsed().contains(k)) {
                        getCollapsed().remove(k);
                        getWidgets().remove(k);
                    } else if (getVisible().containsKey(k)) {
                        getVisible().remove(k);
                        getCollapsed().add(k);
                    }
                }
                layoutGraph();
                // check if a node was selected in the collapsed subtree
                List<Object> collapsedSubtree =
                        Collections.list(node.breadthFirstEnumeration());
                for (Object o : collapsedSubtree) {
                    DefaultMutableTreeNode n = (DefaultMutableTreeNode)o;
                    K k = (K)n.getUserObject();
                    if (m_selected == k) {
                        m_selected = userObject;
                    }
                }
                m_component.revalidate();
                m_component.repaint();
                return;
            }
        }
        for (Rectangle r : m_expandSign.keySet()) {
            if (r.contains(e.getPoint())) {
                // expand children
                DefaultMutableTreeNode node = m_expandSign.get(r);
                K userObject = (K)node.getUserObject();
                assert !node.isLeaf();
                for (Object o : Collections.list(node.children())) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode)o;
                    K k = (K)child.getUserObject();
                    getCollapsed().add(k);
                }
                Enumeration<DefaultMutableTreeNode> enumeration =
                        node.breadthFirstEnumeration();
                // skip starting node
                enumeration.nextElement();
                for (DefaultMutableTreeNode n : Collections.list(enumeration)) {
                    K k = (K)n.getUserObject();
                    if (getCollapsed().contains(k)) {
                        getVisible().put(k, new Rectangle());
                        getCollapsed().remove(k);
                    }
                }
                layoutGraph();
                // With this call parent components get informed about the
                // change in the preferred size.
                m_component.revalidate();
                // get surrounding rectangle of the opened subtree
                Rectangle p = getVisible().get(userObject);
                Rectangle vis = new Rectangle(p);
                List<Object> expandedSubtree =
                        Collections.list(node.breadthFirstEnumeration());
                for (Object o : expandedSubtree) {
                    DefaultMutableTreeNode n = (DefaultMutableTreeNode)o;
                    K k = (K)n.getUserObject();
                    Rectangle rr = getVisible().get(k);
                    if (null != rr) {
                        vis = vis.union(rr);
                    }
                }
                // Show also the sign for expanding childs
                vis.height += m_layoutSettings.getLevelGap() / 2;
                // Display node and all visible children
                m_component.scrollRectToVisible(vis);
                // Make sure that node will be displayed
                m_component.scrollRectToVisible(p);
                m_component.repaint();
                return;
            }
        }
        for (Map.Entry<K, Rectangle> entry : getVisible().entrySet()) {
            if (entry.getValue().contains(e.getPoint())) {
                K k = entry.getKey();
                if (m_selected == k && e.isControlDown()) {
                    m_selected = null;
                } else {
                    m_selected = k;
                }
                // translate event
                MouseEvent ee = new MouseEvent(
                        e.getComponent(),
                        e.getID(),
                        e.getWhen(),
                        e.getModifiers(),
                        e.getX() - entry.getValue().x,
                        e.getY() - entry.getValue().y,
                        e.getXOnScreen(),
                        e.getYOnScreen(),
                        e.getClickCount(),
                        e.isPopupTrigger(),
                        e.getButton());
                getWidgets().get(k).mousePressed(ee);
                m_component.repaint();
                return;
            }
        }
    }


    /**
     * Get the selected node.
     * @return the selected node
     */
    public K getSelected() {
        return m_selected;
    }

    /**
     * HiLite the given nodes.
     * @param toHiLite the nodes to HiLite
     */
    public void hiLite(final Set<K> toHiLite) {
        m_hilited.clear();
        m_hilited.addAll(toHiLite);
        m_component.repaint();
    }

    /**
     * Unhilite all nodes.
     */
    public void clearHilite() {
        m_hilited.clear();
        m_component.repaint();
    }

    /**
     * Returns the node at a certain point.
     *
     * @param p the point on screen
     * @return the node at the given point
     */
    public K nodeAtPoint(final Point p) {
        for (Map.Entry<K, Rectangle> entry
                : getVisible().entrySet()) {
            if (entry.getValue().contains(p)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns the widgets for the nodes returned by getVisible() and
     * getCollapsed().
     * @return the widgets for visible and collapsed nodes
     */
    public Map<K, NodeWidget<K>> getWidgets() {
        return m_widgets;
    }

    /**
     * Returns the visible nodes and their bounds.
     * @return the visible the visible nodes and their bounds.
     */
    protected Map<K, Rectangle> getVisible() {
        return m_visible;
    }

    /**
     * Returns the collapsed nodes. When a branch is collapsed, all visible
     * nodes of that branch are moved to this set. When a branch is expanded,
     * all collapsed nodes of that branch will be made visible. If there are no
     * collapsed nodes the first level of the expanded branch will be made
     * visible. Visible nodes can be retrieved by getVisible().
     * @return the collapsed
     */
    protected Set<K> getCollapsed() {
        return m_collapsed;
    }

    /**
     * Changes the common width of the visible nodes. This method does not
     * trigger repaint.
     * Call following method for a relayout and a repaint:
     *      layoutGraph();
     *      getView().revalidate();
     *      getView().repaint();
     *
     * @param nodeWidth the nodeWidth to set
     */
    protected void setNodeWidth(final int nodeWidth) {
        m_nodeWidth = nodeWidth;
    }

    /**
     * The common widht of the visible nodes.
     * @return the nodeWidth
     */
    protected int getNodeWidth() {
        return m_nodeWidth;
    }

    /**
     * The root node. getRoot().getUserObject() returns the node given in
     * the constructor and the node provided by set RootNode, respectivly.
     * @return the root
     */
    protected DefaultMutableTreeNode getRoot() {
        return m_root;
    }

    /**
     * The keys are retrieved from getNodeWidgetFactory().getChildren(...).
     * Calling getTreeMap(key).getUserObject() returns key.
     * @return a dictionary of all nodes in the decision tree
     */
    Map<K, DefaultMutableTreeNode> getTreeMap() {
        return m_treeMap;
    }

    /**
     * Return an object width constants used for layout the graph.
     * @return the layoutSettings
     */
    LayoutSettings getLayoutSettings() {
        return m_layoutSettings;
    }

    /** Mutable class used during layout. */
    private class LayoutInfo {
        private int m_yOffset;

        /**
         * @return the yOffset
         */
        final int getYOffset() {
            return m_yOffset;
        }

        /**
         * @param yOffset the yOffset to set
         */
        final void setYOffset(final int yOffset) {
            m_yOffset = yOffset;
        }
    }

    /** Constants for the layout. */
    static class LayoutSettings {
        private int m_levelGap = 100;
        private int m_branchGap = 20;
        private int m_nodeWidth;
        private Dimension m_signSize;
        private Point m_signOffset;

        /**
         * @param nodeWidth The width of the nodes.
         */
        public LayoutSettings(final int nodeWidth) {
            m_nodeWidth = nodeWidth;
            m_signSize = new Dimension(12, 12);
            int delta = 4;
            m_signOffset = new Point(-m_signSize.width - delta,
                    m_levelGap / 2 - m_signSize.height - delta);
        }

        /**
         * Compute bounds of the collapse/expand sign.
         * @param parentNode The bounds of the node above the sign
         * @return the bounds of the sign
         */
        public Rectangle getSignBounds(final Rectangle parentNode) {
            Point p = new Point(getSignOffset());
            p.translate(parentNode.x + parentNode.width / 2,
                    parentNode.y + parentNode.height);
            Rectangle collapseSign = new Rectangle(p,
                    getSignSize());
            return collapseSign;
        }

        /**
         * @return the gap at the bottom
         */
        public int getBottomGap() {
            return 5;
        }

        /**
         * @return the gap on the right side
         */
        public int getRightGap() {
            return 5;
        }

        /**
         * @return the branchGap
         */
        final int getBranchGap() {
            return m_branchGap;
        }
        /**
         * @return the plus sign size
         */
        public Dimension getSignSize() {
            return m_signSize;
        }
        /**
         * @return the plus sign offset
         */
        public Point getSignOffset() {
            return m_signOffset;
        }
        /**
         * @return the gap on the left side
         */
        public int getLeftGap() {
            return 5;
        }
        /**
         * @return the gap on top
         */
        public int getTopGap() {
            return 5;
        }
        /**
         * @return the common width fo nodes
         */
        final int getNodeWidth() {
            return m_nodeWidth;
        }
        /**
         * @return the levelGap
         */
        final int getLevelGap() {
            return m_levelGap;
        }
      }

}
