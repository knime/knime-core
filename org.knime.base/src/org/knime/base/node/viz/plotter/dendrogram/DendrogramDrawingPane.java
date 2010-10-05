/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   10.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.dendrogram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.List;

import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.LabelPaintUtil;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ShapeFactory;

/**
 * Interprets the {@link org.knime.base.node.viz.plotter.dendrogram.BinaryTree}
 * of {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramPoint}s such
 * that the leaf nodes (the data points) are painted and the cluster nodes are
 * drawn with a horizontal line between the contained subnodes and vertical 
 * lines to that subnodes. The distance of the two subclsuters is displayed on
 * the y axis, the data points are displayed on the x axis. A tooltip to provide
 * information about the exct distance is provided for the nodes.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DendrogramDrawingPane extends AbstractDrawingPane {
    
    /** The tree containing the DendrogramPoints. */
    private BinaryTree<DendrogramPoint> m_rootNode;
    
    /** Flag to display or hide the points. */
    private boolean m_showDots = true;
    
    private int m_dotSize = 4;
    
    private int m_lineThickness = 1;
    
    /** Constant by which the thickness of hilited or selected lines is 
     * increased. */
    private static final float EMPH = 1.4f;
    
    /**
     * Inititalizes the tooltip.
     *
     */
    public DendrogramDrawingPane() {
        setToolTipText("");
    }
    
    /**
     * Sets the view model (a binary tree containing points).
     * @param root the view model.
     */
    public void setRootNode(final BinaryTree<DendrogramPoint> root) {
        m_rootNode = root;
    }
    
    /**
     * 
     * @param show true if dots should be displayed
     */
    public void setShowDots(final boolean show) {
        m_showDots = show;
    }
    
    /**
     * 
     * @return the size of the dots.
     */
    public int getDotSize() {
        return m_dotSize;
    }
    
    /**
     * 
     * @param dotSize sets the dot size
     */
    public void setDotSize(final int dotSize) {
        m_dotSize = dotSize;
    }
    
    /**
     * 
     * @param thickness sets the line thickness
     */
    public void setLineThickness(final int thickness) {
        m_lineThickness = thickness;
    }
    

    /**
     * Paints a dendrogram such that the leaf nodes are painted at the border
     * (with distance 0) and on the nominal x axis and the cluster nodes are 
     * drawn with increasing distance to the top (that is the distance is 
     * plotted on the y axis). Each cluster node has exactly two subnodes, 
     * a horizontal line on the height of the distance between these two 
     * subnodes and two vertical lines from the end points of the 
     * horizontal line to the subnodes. 
     * 
     * <p>
     * Hilited (sub)nodes are colored with the hilite color and hilited and 
     * selected clusteres are visualized through color and emphasized line 
     * thickness.
     * 
     * {@inheritDoc}
     */
    @Override
    public void paintContent(final Graphics g) {
        if (m_rootNode == null) {
            return;
        }
        Stroke backupStroke = ((Graphics2D)g).getStroke();
        Color backupColor = g.getColor();
        List<BinaryTreeNode<DendrogramPoint>>nodes = m_rootNode.getNodes(
                BinaryTree.Traversal.IN);
        for (BinaryTreeNode<DendrogramPoint> node : nodes) {
            DendrogramPoint dendroPoint = node.getContent();
            // set the correct stroke and color
            g.setColor(ColorAttr.DEFAULT.getColor(
                    node.getContent().isSelected(), 
                    node.getContent().isHilite()));
            if (node.getContent().isSelected() 
                    || node.getContent().isHilite()) {
                ((Graphics2D)g).setStroke(new BasicStroke(
                        (float)(m_lineThickness * EMPH)));
            } else {
                ((Graphics2D)g).setStroke(new BasicStroke(m_lineThickness));
            }
            
            if (node.getLeftChild() != null 
                    || node.getRightChild() != null) {
//                draw horizontal line
                Point leftPoint = node.getLeftChild().getContent().getPoint();
                Point rightPoint = node.getRightChild().getContent().getPoint();
                g.drawLine(leftPoint.x, node.getContent().getPoint().y, 
                        rightPoint.x, node.getContent().getPoint().y);
            }
            // draw vertical line
            if (node.getParent() != null) {
                g.setColor(ColorAttr.DEFAULT.getColor(
                        node.getParent().getContent().isSelected(), 
                        node.getParent().getContent().isHilite()));
                // check if parent is selected
                // if yes bold line, else normal line
                if (!node.getParent().getContent().isSelected() 
                        && !node.getParent().getContent().isHilite()) {
                    ((Graphics2D)g).setStroke(new BasicStroke(m_lineThickness));
                } else {
                    ((Graphics2D)g).setStroke(new BasicStroke(
                            (float)(m_lineThickness * EMPH)));
                }
                g.drawLine(node.getContent().getPoint().x, 
                        node.getContent().getPoint().y,
                        node.getContent().getPoint().x, 
                        node.getParent().getContent().getPoint().y);
            }
            if (m_showDots) {
                Point p = node.getContent().getPoint();
                ShapeFactory.Shape shape = node.getContent().getShape();
                int size = (int)(node.getContent().getRelativeSize() 
                        * m_dotSize);
                shape.paint(g, p.x, p.y, size, 
                        node.getContent().getColor().getColor(), 
                        dendroPoint.isHilite(), dendroPoint.isSelected(), 
                        false);
            }
            ((Graphics2D)g).setStroke(backupStroke);
            g.setColor(backupColor);
        }
    }
    
    
    /**
     * The original (not mapped) distance of the clustering between the two
     * subnodes. 
     * 
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent event) {
        if (m_rootNode == null) {
            return "";
        }
        Point p = event.getPoint();
        List<BinaryTreeNode<DendrogramPoint>> nodes = m_rootNode.getNodes(
                BinaryTree.Traversal.IN);
        for (BinaryTreeNode<DendrogramPoint> node : nodes) {
            Point nodePoint = node.getContent().getPoint();
            double dotSize = m_dotSize 
                    * node.getContent().getRelativeSize();
            if (p.x > nodePoint.x - (dotSize / 2) 
                    && p.x < nodePoint.x + (dotSize / 2)
                    && p.y > nodePoint.y - (dotSize / 2)
                    && p.y < nodePoint.y + (dotSize / 2)) {
                if (node.getContent().getRows().size() == 1) {
                    for (RowKey row : node.getContent().getRows()) {
                        return row.toString();
                    }
                } else {
                    return "dist: " + LabelPaintUtil.getDoubleAsString(
                            node.getContent().getDistance(), 1000);
                }
            }
                    
        }
        return "";
    }

}
