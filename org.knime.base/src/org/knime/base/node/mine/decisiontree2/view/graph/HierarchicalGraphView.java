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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
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
import javax.swing.tree.DefaultMutableTreeNode;

import org.knime.core.data.property.ColorAttr;

/**
 * A widget for a tree.
 *
 * @author Heiko Hofer
 * @param <K> The type of the nodes user objects
 */
public abstract class HierarchicalGraphView<K> {
    /** The alignment of the graph. */
    public enum Align { Left, Center };

    private boolean m_leftAligned = false;
    private Map<DefaultMutableTreeNode, NodeWidget<K>> m_widgets;
    private Map<DefaultMutableTreeNode, Rectangle> m_visible;
    private Map<Rectangle, DefaultMutableTreeNode> m_collapseSign;
    private Map<Rectangle, DefaultMutableTreeNode> m_expandSign;
    private DefaultMutableTreeNode m_root;
    private Set<DefaultMutableTreeNode> m_collapsed;
    private JComponent m_component;
    private LayoutInfo m_layoutInfo;
    private Set<DefaultMutableTreeNode> m_selected;
    private Set<K> m_hilited;
    private List<GraphListener> m_graphListeners;

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
        m_widgets = new HashMap<DefaultMutableTreeNode, NodeWidget<K>>();
        m_visible = new HashMap<DefaultMutableTreeNode, Rectangle>();
        m_collapsed = new HashSet<DefaultMutableTreeNode>();
        m_collapseSign = new HashMap<Rectangle, DefaultMutableTreeNode>();
        m_expandSign = new HashMap<Rectangle, DefaultMutableTreeNode>();
        m_selected = new HashSet<DefaultMutableTreeNode>();
        m_hilited = new HashSet<K>();
        m_graphListeners = new ArrayList<GraphListener>();

        m_component = new HierarchicalGraphComponent<K>(this);
        setRootNode(root);
    }


    /**
     * Replace the root which is the model for this view.
     *
     * @param root the root
     */
    public void setRootNode(final K root) {
        m_widgets.clear();
        m_visible.clear();
        m_collapsed.clear();
        m_collapseSign.clear();
        m_expandSign.clear();
        m_selected.clear();
        m_hilited.clear();
        if (null != root) {
            m_root = new DefaultMutableTreeNode(root);
            // recursive build tree
            buildTree(m_root);
            // display root an its children by default
            m_visible.put(m_root, new Rectangle());
            for (Object o : Collections.list(m_root.children())) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode) o;
                m_visible.put(n, new Rectangle());
            }
            layoutGraph();
            // With this call parent components get informed about the
            // change in the preferred size.
            m_component.revalidate();
            // make sure that the root node is in the visible area
            m_component.scrollRectToVisible(m_visible.get(m_root));
        }
    }


    /**
     * Get the root.
     * @return the root
     */
    @SuppressWarnings("unchecked") /* DefaultMutableTreeNode does not support
        generics. */
    public K getRootNode() {
        return (K)m_root.getUserObject();
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
        for (K child : children) {
            DefaultMutableTreeNode childNode
                    = new DefaultMutableTreeNode(child);
            node.add(childNode);
            nodeChildren.add(childNode);
            m_widgets.put(childNode,
                    getNodeWidgetFactory().createGraphNode(child));
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
    private void layoutGraph() {
        if (null == m_root) {
            return;
        }
        m_collapseSign.clear();
        m_expandSign.clear();
        // clear visible
        for (DefaultMutableTreeNode key : m_visible.keySet()) {
            m_visible.put(key, new Rectangle());
        }
        NodeWidget<K> rootWidget = m_widgets.get(m_root);
        if (null == rootWidget) {
            K rootObject = (K)m_root.getUserObject();
            rootWidget = getNodeWidgetFactory().createGraphNode(rootObject);
            m_widgets.put(m_root, rootWidget);
        }
        m_layoutInfo = new LayoutInfo(rootWidget.getPreferredSize());

        Stack<DefaultMutableTreeNode> stack =
            new Stack<DefaultMutableTreeNode>();
        // start at first leaf
        pushtoFirstLeaf(m_root, stack);
        // recursive layout
        DefaultMutableTreeNode nextToLayout = stack.peek();
        while (null != nextToLayout) {
            nextToLayout = iterativeLayout(stack, m_layoutInfo);
        }
        Rectangle graphBounds = computeGraphBounds();
        m_component.setPreferredSize(
                new Dimension(m_layoutInfo.getLeftGap()
                        + graphBounds.width + m_layoutInfo.getRightGap(),
                        m_layoutInfo.getTopGap() + graphBounds.height
                        + m_layoutInfo.m_levelGap / 2
                        + m_layoutInfo.getBottomGap()));
    }

    /**
     * A function to compute the layout of the graph in a loop. The layout
     * starts from the first leaf.
     * @param stack the parent nodes that must be layouted
     * @param info some constants for the layout
     */
    private DefaultMutableTreeNode iterativeLayout(
            final Stack<DefaultMutableTreeNode> stack,
            final LayoutInfo info) {
        DefaultMutableTreeNode node = stack.pop();
        int level = node.getLevel();
        Rectangle bounds = new Rectangle(info.getNodeSize());
        bounds.y = info.getTopGap()
                + level * ((int)info.getNodeSize().getHeight()
                + info.getLevelGap());
        boolean layoutParent;
        if (node.isLeaf()) {
            layoutParent = false;
        } else {
            DefaultMutableTreeNode child =
                (DefaultMutableTreeNode)node.getChildAt(0);
            layoutParent = m_visible.containsKey(child);
        }
        if (layoutParent) {
            // Layout a parent node
            DefaultMutableTreeNode firstChild =
                (DefaultMutableTreeNode)node.getChildAt(0);
            DefaultMutableTreeNode lastChild = m_leftAligned ? firstChild
                    : (DefaultMutableTreeNode)node.getChildAt(
                            node.getChildCount() - 1);
            Rectangle first = m_visible.get(firstChild);
            Rectangle last = m_visible.get(lastChild);
            bounds.x = (last.x + first.x) / 2;
            // This node can be collapsed
            Point p = new Point(info.getSignOffset());
            p.translate(bounds.x, bounds.y);
            Rectangle collapseSign = new Rectangle(p, info.getSignSize());
            m_collapseSign.put(collapseSign, node);
        } else {
            // Layout a leaf node, which might be the first leaf in the branch
            Rectangle graphBounds = computeGraphBounds();
            bounds.x = 0 != graphBounds.width
                ? graphBounds.x + graphBounds.width + info.getBranchGap()
                : info.getLeftGap();
            if (!node.isLeaf()) {
                // This node can be expanded
                Point p = new Point(info.getSignOffset());
                p.translate(bounds.x, bounds.y);
                Rectangle expandSign = new Rectangle(p, info.getSignSize());
                m_expandSign.put(expandSign, node);
            }
        }
        assignBounds(node, bounds);
        if (stack.isEmpty()) {
            return null;
        }
        // go ahead with first leaf of parents next child
        DefaultMutableTreeNode parent = stack.peek();
        assert parent == node.getParent();
        DefaultMutableTreeNode nextChild =
            (DefaultMutableTreeNode)parent.getChildAfter(node);
        if (null != nextChild) {
            // go ahead with first leaf of this child
            return pushtoFirstLeaf(nextChild, stack);
        } else {
            // go ahead with parent
            return parent;
        }
    }


    /**
     * Computes the rectangle the encloses the graph completely.
     * @return the bounds ot the graph
     */
    private Rectangle computeGraphBounds() {
        Rectangle rectangle = new Rectangle();
        for (Rectangle next : m_visible.values()) {
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
            final Stack<DefaultMutableTreeNode> stack) {
        DefaultMutableTreeNode n = node;
        boolean continueIteration = continueIteration(n);
        while (continueIteration) {
            stack.push(n);
            n = (DefaultMutableTreeNode)n.getChildAt(0);
            continueIteration = continueIteration(n);
        }
        stack.push(n);
        return n;
    }

    /** Returns true when n has visible children. */
    private boolean continueIteration(final DefaultMutableTreeNode n) {
        boolean continueIteration = !n.isLeaf();
        if (!n.isLeaf()) {
            DefaultMutableTreeNode child =
                (DefaultMutableTreeNode)n.getChildAt(0);
            continueIteration = m_visible.containsKey(child);
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
        m_visible.put(node, bounds);
        NodeWidget<K> widget = m_widgets.get(node);
        if (null == widget) {
            K object = (K)node.getUserObject();
            widget = getNodeWidgetFactory().createGraphNode(object);
            m_widgets.put(node, widget);
        }
        widget.setSize(new Dimension(bounds.width, bounds.height));
    }

    /**
     * Paints the nodes, the connectors and the labels on the connectors to
     * the given graphics object. Nodes are painted with different background
     * or border whether they are selected or hilited.
     *
     * @param g the graphics object
     */
    void paint(final Graphics2D g) {
        if (null == m_root) {
            return;
        }
        final Paint origPaint = g.getPaint();
        final Stroke origStroke = g.getStroke();
        g.setColor(ColorAttr.BORDER);
        Enumeration<DefaultMutableTreeNode> breadthFirst =
            m_root.breadthFirstEnumeration();
        while (breadthFirst.hasMoreElements()) {
            DefaultMutableTreeNode curr = breadthFirst.nextElement();
            if (!m_visible.containsKey(curr)) {
                continue;
            }
            paintNode(curr, g);
            Rectangle bounds = m_visible.get(curr);
            List<Rectangle> visibleChilds =
                new ArrayList<Rectangle>();
            if (!curr.isLeaf()) {
                DefaultMutableTreeNode firstChild =
                    (DefaultMutableTreeNode)curr.getChildAt(0);
                if (m_visible.containsKey(firstChild)) {
                    for (int i = 0; i < curr.getChildCount(); i++) {
                        DefaultMutableTreeNode child =
                            (DefaultMutableTreeNode)curr.getChildAt(i);
                        visibleChilds.add(m_visible.get(child));
                    }
                }
            }

            if (curr.isLeaf()) {
                // currently do nothing. It would be possible to draw a leaf
                // sign here.
            } else if (visibleChilds.isEmpty()) {
                // draw line to the plus sign
                int x = bounds.x + bounds.width / 2;
                int y = bounds.y + bounds.height
                            + m_layoutInfo.getLevelGap() / 2;
                g.drawLine(x, bounds.y + bounds.height, x, y);
            } else {
                // draw connections to the children
                int x = bounds.x + bounds.width / 2;
                int y = bounds.y + bounds.height
                            + m_layoutInfo.getLevelGap() / 2;
                g.drawLine(x, bounds.y + bounds.height, x, y);

                Rectangle firstChild = visibleChilds.get(0);
                if (firstChild.x != m_visible.get(curr).x) {
                    GeneralPath path = new GeneralPath();
                    path.moveTo(x, y);
                    path.lineTo(firstChild.x + firstChild.width / 2 + 3, y);
                    path.quadTo(firstChild.x + firstChild.width / 2, y,
                            firstChild.x + firstChild.width / 2, y + 3);
                    path.lineTo(firstChild.x + firstChild.width / 2,
                            firstChild.y);
                    g.draw(path);
                } else {
                    g.drawLine(x, y, x, firstChild.y);
                }
                if (visibleChilds.size() > 1) {
                    Rectangle lastChild =
                        visibleChilds.get(visibleChilds.size() - 1);
                    GeneralPath path = new GeneralPath();
                    path.moveTo(x, y);
                    path.lineTo(lastChild.x + lastChild.width / 2 - 3, y);
                    path.quadTo(lastChild.x + lastChild.width / 2, y,
                            lastChild.x + lastChild.width / 2, y + 3);
                    path.lineTo(lastChild.x + lastChild.width / 2,
                            lastChild.y);
                    g.draw(path);
                }

                if (visibleChilds.size() > 2) {
                    for (int i = 1; i < visibleChilds.size() - 1; i++) {
                        Rectangle child = visibleChilds.get(i);
                        int xx = child.x + child.width / 2;
                        g.drawLine(xx, y, xx, child.y);
                    }
                }
            }
            // draw text on the connectors
            NodeWidget<K> widget = m_widgets.get(curr);
            if (null != widget.getConnectorLabelAbove()) {
                String text = widget.getConnectorLabelAbove();
                Font prevFont = g.getFont();
                Font font = prevFont.deriveFont(Font.BOLD | Font.ITALIC);
                g.setFont(font);
                TextLayout textLayout = new TextLayout(text, font,
                        g.getFontRenderContext());
                Rectangle textBounds = textLayout.getBounds().getBounds();
                int width = textBounds.width;
                int height = textBounds.height;
                int x = bounds.x + (bounds.width - width) / 2;
                int y = bounds.y - height - 18;
                Color prevColor = g.getColor();
                g.setColor(ColorAttr.BACKGROUND);
                g.fillRect(x, y - 2, width, height + 4);
                g.setColor(Color.black.brighter());
                g.setColor(ColorAttr.BORDER);
                textLayout.draw(g, x - textBounds.x, y - textBounds.y);
                g.setColor(prevColor);
                g.setFont(prevFont);
            }
            if (null != widget.getConnectorLabelBelow()
                    && !visibleChilds.isEmpty()) {
                String text = widget.getConnectorLabelBelow();
                Font prevFont = g.getFont();
                Font font = prevFont.deriveFont(Font.BOLD | Font.ITALIC);
                g.setFont(font);
                TextLayout textLayout = new TextLayout(text, font,
                        g.getFontRenderContext());
                Rectangle textBounds = textLayout.getBounds().getBounds();
                int width = textBounds.width;
                int height = textBounds.height;
                int x = bounds.x + (bounds.width - width) / 2;
                int y = bounds.y + bounds.height + 10;
                Color prevColor = g.getColor();
                g.setColor(ColorAttr.BACKGROUND);
                g.fillRect(x, y - 2, width, height + 4);
                g.setColor(ColorAttr.BORDER);
                textLayout.draw(g, x - textBounds.x, y - textBounds.y);
                g.setColor(prevColor);
                g.setFont(prevFont);
            }
        }
        for (Rectangle bounds : m_collapseSign.keySet()) {
            int y = bounds.y + bounds.height / 2;
            int delta = 3;
            g.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
            g.drawLine(bounds.x + delta, y, bounds.x + bounds.width - delta, y);
        }
        for (Rectangle bounds : m_expandSign.keySet()) {
            int x = bounds.x + bounds.width / 2;
            int y = bounds.y + bounds.height / 2;
            int delta = 3;
            g.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
            g.drawLine(bounds.x + delta, y, bounds.x + bounds.width - delta, y);
            g.drawLine(x, bounds.y + delta, x,
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
    private void paintNode(final DefaultMutableTreeNode node,
            final Graphics2D g) {
        NodeWidget<K> widget = m_widgets.get(node);
        Rectangle bounds = m_visible.get(node);
        boolean selected = m_selected.contains(node);
        boolean hilited = m_hilited.contains((node.getUserObject()));
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

        AffineTransform previousTransform = g.getTransform();
        AffineTransform trans = g.getTransform();
        trans.concatenate(AffineTransform.getTranslateInstance(
                bounds.x, bounds.y));
        g.setTransform(trans);
        widget.paint(g);
        g.setTransform(previousTransform);
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

    /**
     * Handler for the mouse pressed event.
     * @param e the mouse event
     * */
    void mousePressed(final MouseEvent e) {
        for (Rectangle r : m_collapseSign.keySet()) {
            if (r.contains(e.getPoint())) {
                // collapse children
                DefaultMutableTreeNode node = m_collapseSign.get(r);
                Enumeration<DefaultMutableTreeNode> enumeration =
                        node.breadthFirstEnumeration();
                // skip starting node
                enumeration.nextElement();
                for (DefaultMutableTreeNode n : Collections.list(enumeration)) {
                    if (m_collapsed.contains(n)) {
                        m_collapsed.remove(n);
                    } else if (m_visible.containsKey(n)) {
                        m_visible.remove(n);
                        m_collapsed.add(n);
                    }
                }
                layoutGraph();
                // check if a node was selected in the collapsed subtree
                List<Object> collapsedSubtree =
                        Collections.list(node.breadthFirstEnumeration());
                for (Object o : collapsedSubtree) {
                    DefaultMutableTreeNode n = (DefaultMutableTreeNode)o;
                    if (m_selected.contains(n)) {
                        m_selected.remove(n);
                        m_selected.add(node);
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
                assert !node.isLeaf();
                for (Object o : Collections.list(node.children())) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode)o;
                    m_collapsed.add(child);
                }
                Enumeration<DefaultMutableTreeNode> enumeration =
                        node.breadthFirstEnumeration();
                // skip starting node
                enumeration.nextElement();
                for (DefaultMutableTreeNode n : Collections.list(enumeration)) {
                    if (m_collapsed.contains(n)) {
                        m_visible.put(n, new Rectangle());
                        m_collapsed.remove(n);
                    }
                }
                layoutGraph();
                // With this call parent components get informed about the
                // change in the preferred size.
                m_component.revalidate();
                // get surrounding rectangle of the opened subtree
                Rectangle p = m_visible.get(node);
                Rectangle vis = new Rectangle(p);
                List<Object> expandedSubtree =
                        Collections.list(node.breadthFirstEnumeration());
                for (Object o : expandedSubtree) {
                    DefaultMutableTreeNode n = (DefaultMutableTreeNode)o;
                    Rectangle rr = m_visible.get(n);
                    if (null != rr) {
                        vis = vis.union(rr);
                    }
                }
                // Show also the sign for expanding childs
                vis.height += m_layoutInfo.getLevelGap() / 2;
                // Display node and all visible children
                m_component.scrollRectToVisible(vis);
                // Make sure that node will be displayed
                m_component.scrollRectToVisible(p);
                m_component.repaint();
                return;
            }
        }
        for (Map.Entry<DefaultMutableTreeNode, Rectangle> entry
                : m_visible.entrySet()) {
            if (entry.getValue().contains(e.getPoint())) {
                if (m_selected.contains(entry.getKey()) && e.isControlDown()) {
                    m_selected.remove(entry.getKey());
                } else {
                    m_selected.clear();
                    m_selected.add(entry.getKey());
                }
                m_component.repaint();
                return;
            }
        }

    }

    /** Constants for the layout. */
    private static class LayoutInfo {
        private int m_levelGap = 100;
        private int m_branchGap = 20;
        private Dimension m_nodeSize;
        private Dimension m_signSize;
        private Point m_signOffset;

        public LayoutInfo(final Dimension nodeSize) {
            m_nodeSize = new Dimension(nodeSize.width + 20,
                    nodeSize.height + 5);
            m_signSize = new Dimension(12, 12);
            int delta = 4;
            m_signOffset = new Point(
                    m_nodeSize.width / 2 - m_signSize.width - delta,
                    m_nodeSize.height + m_levelGap / 2
                    - m_signSize.height - delta);
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
         * @return the nodeSize
         */
        final Dimension getNodeSize() {
            return m_nodeSize;
        }
        /**
         * @return the levelGap
         */
        final int getLevelGap() {
            return m_levelGap;
        }
      }

    /**
     * Get the selected nodes.
     * @return the selected nodes
     */
    public List<K> getSelected() {
        List<K> selected = new ArrayList<K>();
        for (DefaultMutableTreeNode node : m_selected) {
            selected.add((K)node.getUserObject());
        }
        return selected;
    }

    /**
     * HiLite the given nodes.
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
        for (Map.Entry<DefaultMutableTreeNode, Rectangle> entry
                : m_visible.entrySet()) {
            if (entry.getValue().contains(p)) {
                return (K)(entry.getKey().getUserObject());
            }
        }
        return null;
    }
}
