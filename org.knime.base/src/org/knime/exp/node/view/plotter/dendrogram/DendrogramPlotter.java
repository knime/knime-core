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
 *   10.10.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.dendrogram;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.exp.node.cluster.hierarchical.ClusterNode;
import org.knime.exp.node.view.plotter.scatter.ScatterPlotter;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DendrogramPlotter extends ScatterPlotter {
    
    private static final int OFFSET = 6;

    private ClusterNode m_rootNode;
    
    private BinaryTree<DendrogramPoint> m_tree;
    
    private Set<DendrogramPoint> m_selected;
    
    private int m_dotSize;
    
    /**
     * Default constructor.
     *
     */
    public DendrogramPlotter() {
        this(new DendrogramDrawingPane(), new DendrogramPlotterProperties());
    }
    
    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#reset()
     */
    @Override
    public void reset() {
        m_rootNode = null;
        ((DendrogramDrawingPane)getDrawingPane()).setRootNode(null);
        getDrawingPane().repaint();
    }
    
    /**
     * 
     * @param panel drawing pane
     * @param props properties
     */
    public DendrogramPlotter(final DendrogramDrawingPane panel,
            final DendrogramPlotterProperties props) {
        super(panel, props);
        m_selected = new LinkedHashSet<DendrogramPoint>();
        props.getShowDotsBox().addChangeListener(new ChangeListener() {
            /**
             * @see javax.swing.event.ChangeListener#stateChanged(
             * javax.swing.event.ChangeEvent)
             */
            public void stateChanged(final ChangeEvent e) {
                ((DendrogramDrawingPane)getDrawingPane()).setShowDots(
                        props.getShowDotsBox().isSelected());
                getDrawingPane().repaint();
            }   
        });
        props.getThicknessSpinner().addChangeListener(new ChangeListener() {
            /**
             * @see javax.swing.event.ChangeListener#stateChanged(
             * javax.swing.event.ChangeEvent)
             */
            public void stateChanged(final ChangeEvent e) {
                ((DendrogramDrawingPane)getDrawingPane()).setLineThickness(
                        (Integer)props.getThicknessSpinner().getValue());
                getDrawingPane().repaint();
            }
        });
        props.getDotSizeSpinner().addChangeListener(new ChangeListener() {
            /**
             * @see javax.swing.event.ChangeListener#stateChanged(
             * javax.swing.event.ChangeEvent)
             */
            public void stateChanged(final ChangeEvent e) {
                ((DendrogramDrawingPane)getDrawingPane()).setDotSize(
                        (Integer)props.getDotSizeSpinner().getValue());
                getDrawingPane().repaint();
            }
        });
        
    }
    
    /**
     * 
     * @param root the root node of the dendrogram.
     */
    public void setRootNode(final ClusterNode  root) {
        m_rootNode = root;
    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#clearSelection()
     */
    @Override
    public void clearSelection() {
        m_selected.clear();
        updatePaintModel();

    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#hiLite(
     * org.knime.core.node.property.hilite.KeyEvent)
     */
    @Override
    public void hiLite(final KeyEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#hiLiteSelected()
     */
    @Override
    public void hiLiteSelected() {
        for (DendrogramPoint p : m_selected) {
            delegateHiLite(p.getRows());
        }
        updatePaintModel();
    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#
     * selectClickedElement(java.awt.Point)
     */
    @Override
    public void selectClickedElement(final Point clicked) {
        int sensivity = ((DendrogramDrawingPane)getDrawingPane()).getDotSize();
        selectElementsIn(new Rectangle(clicked.x - (sensivity / 2),
                clicked.y - (sensivity / 2), sensivity, sensivity));
    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#selectElementsIn(
     * java.awt.Rectangle)
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
        // traverse through the tree from the drawing pane
        // if point is in rect
        // get the keys
        List<BinaryTreeNode<DendrogramPoint>> nodes 
            = new ArrayList<BinaryTreeNode<DendrogramPoint>>();
        nodes = m_tree.getNodes(BinaryTree.Traversal.IN);
        for (BinaryTreeNode<DendrogramPoint> node : nodes) {
            if (selectionRectangle.contains(node.getContent().getPoint())) {
                m_selected.add(node.getContent());
            }
        }
        updatePaintModel();
    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#unHiLite(
     * org.knime.core.node.property.hilite.KeyEvent)
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#unHiLiteSelected()
     */
    @Override
    public void unHiLiteSelected() {
        for (DendrogramPoint p : m_selected) {
            delegateUnHiLite(p.getRows());
        }
        updatePaintModel();
    }

    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#updatePaintModel()
     */
    @Override
    public void updatePaintModel() {
        if (m_rootNode == null) {
            return;
        }
        if (getDataProvider() == null 
                || getDataProvider().getDataArray(1) == null) {
            return;
        }
        double min = 0.0;
        double max = m_rootNode.getMaxDistance(m_rootNode);
        setPreserve(false);
        createYCoordinate(min, max);
        m_dotSize = ((DendrogramDrawingPane)getDrawingPane()).getDotSize();
        getYAxis().setStartTickOffset(OFFSET  + m_dotSize);
        Set<DataCell> rowKeys = new LinkedHashSet<DataCell>();
        getRowKeys(m_rootNode, rowKeys);
        createNominalXCoordinate(rowKeys);
        m_tree = null;
        createViewModel(m_rootNode);
        ((DendrogramDrawingPane)getDrawingPane()).setRootNode(m_tree);
        getDrawingPane().repaint();
        
    }
    
    private void getRowKeys(final ClusterNode node, final Set<DataCell> ids) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            ids.add(node.getLeafDataPoint().getKey().getId());
            return;
        }
        getRowKeys(node.getFirstSubnode(), ids);
        getRowKeys(node.getSecondSubnode(), ids);
        
    }
    
    /**
     * Converts the cluster node into a view model,
     * where cluster nodes are points and leaf nodes are dots.
     * @param node the cluster node tree.
     */
    public void createViewModel(final ClusterNode node) {
        if (node == null) {
            return;
        }
        BinaryTreeNode<DendrogramPoint> viewNode = createViewModelFor(node);
        m_tree = new BinaryTree<DendrogramPoint>(viewNode);
    }
    
    private BinaryTreeNode<DendrogramPoint> createViewModelFor(
            final ClusterNode node) {
        if (getXAxis() == null || getXAxis().getCoordinate() == null
                || getYAxis() == null || getYAxis().getCoordinate() == null) {
            updatePaintModel();
        }
        BinaryTreeNode<DendrogramPoint> viewNode;
//        distinction between cluster node and leaf:
        int height = getDrawingPaneDimension().height - (2 * OFFSET) 
            - (m_dotSize / 2);
        int y = (int)getYAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(node.getDist()), height, true);
        y = getDrawingPaneDimension().height - y - OFFSET - m_dotSize;
        int x;
        DendrogramPoint p;
        if (!node.isLeaf()) {
            x = getXPosition(node);
            p = new DendrogramPoint(new Point(x, y), 
                    node.getDist());
        } else {
            DataRow row = node.getLeafDataPoint();
            x = (int)getXAxis().getCoordinate().calculateMappedValue(
                    row.getKey().getId(),
                    getDrawingPaneDimension().width, true);
            p = new DendrogramPoint(new Point(x, y), 
                    node.getDist());
            DataTableSpec spec = getDataProvider().getDataArray(1)
                .getDataTableSpec();
            p.setColor(spec.getRowColor(row));
            p.setShape(spec.getRowShape(row));
            p.setRelativeSize(spec.getRowSize(row));
        }
        viewNode = new BinaryTreeNode<DendrogramPoint>(p);
        Set<DataCell>keys = new LinkedHashSet<DataCell>();
        getRowKeys(node, keys);
        viewNode.getContent().addRows(keys);
        viewNode.getContent().setSelected(m_selected.contains(viewNode
                .getContent()));
        viewNode.getContent().setHilite(delegateIsHiLit(keys));
        if (node.getFirstSubnode() != null) {
            BinaryTreeNode<DendrogramPoint> leftNode = createViewModelFor(
                    node.getFirstSubnode());
            leftNode.setParent(viewNode);
            viewNode.setLeftChild(leftNode);
        }
        if (node.getSecondSubnode() != null) {
            BinaryTreeNode<DendrogramPoint> rightNode = createViewModelFor(
                    node.getSecondSubnode());
            rightNode.setParent(viewNode);
            viewNode.setRightChild(rightNode);
        }
        return viewNode;
    }

    
    private int getXPosition(final ClusterNode node) {
        if (node.isLeaf()) {
            DataCell value = node.getLeafDataPoint().getKey().getId();
            return (int)getXAxis().getCoordinate().calculateMappedValue(
                    value, getDrawingPaneDimension().width, true);
        }
        return (getXPosition(node.getFirstSubnode()) + getXPosition(
                node.getSecondSubnode())) / 2;
    }
    
    /**
     * @see org.knime.exp.node.view.plotter.AbstractPlotter#updateSize()
     */
    @Override
    public void updateSize() {
        if (m_rootNode == null) {
            return;
        }
        if (getDataProvider() == null 
                || getDataProvider().getDataArray(1) == null) {
            return;
        }
        if (getXAxis() == null || getYAxis() == null) {
            updatePaintModel();
        }
        m_tree = null;
        createViewModel(m_rootNode);
        ((DendrogramDrawingPane)getDrawingPane()).setRootNode(m_tree);
        getDrawingPane().repaint();
    }

    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener#unHiLiteAll()
     */
    @Override
    public void unHiLiteAll() {
        updatePaintModel();
    }

}
