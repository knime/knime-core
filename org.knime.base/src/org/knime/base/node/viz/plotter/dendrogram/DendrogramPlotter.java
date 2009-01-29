/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   10.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.dendrogram;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.plotter.scatter.ScatterPlotter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * Converts a {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramNode}
 * into a {@link org.knime.base.node.viz.plotter.dendrogram.BinaryTree} of 
 * {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramPoint}s, which
 * is the visual representation of a hierachical clustering result stored in the
 * {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramNode}.
 *  
 * @author Fabian Dill, University of Konstanz
 */
public class DendrogramPlotter extends ScatterPlotter {
    
    private static final int OFFSET = 6;

    /** The hierarchical clustering result. */
    private DendrogramNode m_rootNode;
    /** The visual model of the clustering result. */ 
    private BinaryTree<DendrogramPoint> m_tree;
    /** The set of selected dendrogram points. */
    private Set<DendrogramPoint> m_selected;
    /** The dot size of the leafs and cluster nodes. */
    private int m_dotSize;
    
    /**
     * Default constructor.
     *
     */
    public DendrogramPlotter() {
        this(new DendrogramDrawingPane(), new DendrogramPlotterProperties());
    }
    
    /**
     * Constructor for extending classes. Registers all necessary listeners to
     * the control elements of the 
     * {@link org.knime.base.node.viz.plotter.dendrogram
     * .DendrogramPlotterProperties}
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
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                ((DendrogramDrawingPane)getDrawingPane()).setShowDots(
                        props.getShowDotsBox().isSelected());
                getDrawingPane().repaint();
            }   
        });
        props.getThicknessSpinner().addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                ((DendrogramDrawingPane)getDrawingPane()).setLineThickness(
                        (Integer)props.getThicknessSpinner().getValue());
                getDrawingPane().repaint();
            }
        });
        props.getDotSizeSpinner().addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                ((DendrogramDrawingPane)getDrawingPane()).setDotSize(
                        (Integer)props.getDotSizeSpinner().getValue());
                getDrawingPane().repaint();
            }
        });
        
    }
    
    
    /**
     * Resets the visual model and repaints. 
     * 
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#reset()
     */
    @Override
    public void reset() {
        m_rootNode = null;
        ((DendrogramDrawingPane)getDrawingPane()).setRootNode(null);
        getDrawingPane().repaint();
    }
    
    /**
     * Sets the result of the hierachical clustering represented in a 
     * {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramNode}.
     * 
     * @param root the root node of the dendrogram.
     */
    public void setRootNode(final DendrogramNode  root) {
        m_rootNode = root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSelection() {
        m_selected.clear();
        updatePaintModel();

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLiteSelected() {
        for (DendrogramPoint p : m_selected) {
            delegateHiLite(p.getRows());
        }
        updatePaintModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectClickedElement(final Point clicked) {
        int sensivity = ((DendrogramDrawingPane)getDrawingPane()).getDotSize();
        selectElementsIn(new Rectangle(clicked.x - (sensivity / 2),
                clicked.y - (sensivity / 2), sensivity, sensivity));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
        // traverse through the tree from the drawing pane
        // if point is in rect/ get the keys
        for (BinaryTreeNode<DendrogramPoint> node
                    : m_tree.getNodes(BinaryTree.Traversal.IN)) {
            if (selectionRectangle.contains(node.getContent().getPoint())) {
                m_selected.add(node.getContent());
                selectElementsRecursively(node);
            }
        }
        updatePaintModel();
    }
    
    private void selectElementsRecursively(
            final BinaryTreeNode<DendrogramPoint> node) {
        if (node.isLeaf()) {
            return;
        }
        m_selected.add(node.getLeftChild().getContent());
        selectElementsRecursively(node.getLeftChild());
        m_selected.add(node.getRightChild().getContent());
        selectElementsRecursively(node.getRightChild());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteSelected() {
        for (DendrogramPoint p : m_selected) {
            delegateUnHiLite(p.getRows());
        }
        updatePaintModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePaintModel() {
        if (m_rootNode == null) {
            return;
        }
        if (getDataProvider() == null 
                || getDataProvider().getDataArray(1) == null
                || getDataProvider().getDataArray(1).size() == 0) {
            return;
        }
        double min = 0.0;
        double max = m_rootNode.getMaxDistance();
        setPreserve(false);
        createYCoordinate(min, max);
        m_dotSize = ((DendrogramDrawingPane)getDrawingPane()).getDotSize();
        getYAxis().setStartTickOffset(OFFSET  + m_dotSize);
        Set<RowKey> rowKeys = new LinkedHashSet<RowKey>();
        getRowKeys(m_rootNode, rowKeys);
        Set<DataCell> keys = new LinkedHashSet<DataCell>();
        for (RowKey rk : rowKeys) {
            keys.add(new StringCell(rk.getString()));
        }
        createNominalXCoordinate(keys);
        m_tree = null;
        createViewModel(m_rootNode);
        ((DendrogramDrawingPane)getDrawingPane()).setRootNode(m_tree);
        getDrawingPane().repaint();
        
    }
    
    private void getRowKeys(final DendrogramNode node, 
            final Set<RowKey> ids) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            ids.add(node.getLeafDataPoint().getKey());
            return;
        }
        getRowKeys(node.getFirstSubnode(), ids);
        getRowKeys(node.getSecondSubnode(), ids);
        
    }
    
    /**
     * Converts the cluster node into a view model,
     * where cluster nodes are points and leaf nodes are dots.
     * 
     * @param node the cluster node tree.
     */
    public void createViewModel(final DendrogramNode node) {
        if (node == null) {
            return;
        }
        BinaryTreeNode<DendrogramPoint> viewNode = createViewModelFor(node);
        m_tree = new BinaryTree<DendrogramPoint>(viewNode);
    }
    
    /**
     * Recursive method to convert the result of the hierachical clustering 
     * result represented by a 
     * {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramNode} into a 
     * {@link org.knime.base.node.viz.plotter.dendrogram.BinaryTree} of 
     * {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramPoint}s.
     * 
     * @param node the node to convert
     * @return the visual model of the passed 
     * {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramNode}
     */
    private BinaryTreeNode<DendrogramPoint> createViewModelFor(
            final DendrogramNode node) {
        if (getXAxis() == null || getXAxis().getCoordinate() == null
                || getYAxis() == null || getYAxis().getCoordinate() == null) {
            updatePaintModel();
        }
        BinaryTreeNode<DendrogramPoint> viewNode;
//        distinction between cluster node and leaf:
        int height = getDrawingPaneDimension().height - (2 * OFFSET) 
            - (m_dotSize / 2);
        int y = (int)getYAxis().getCoordinate().calculateMappedValue(
                new DoubleCell(node.getDist()), height);
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
                    new StringCell(row.getKey().getString()),
                    getDrawingPaneDimension().width);
            p = new DendrogramPoint(new Point(x, y), 
                    node.getDist());
            DataTableSpec spec = getDataProvider().getDataArray(1)
                .getDataTableSpec();
            p.setColor(spec.getRowColor(row));
            p.setShape(spec.getRowShape(row));
            p.setRelativeSize(spec.getRowSizeFactor(row));
            p.setHilite(delegateIsHiLit(row.getKey()));
        }
        viewNode = new BinaryTreeNode<DendrogramPoint>(p);
        Set<RowKey> keys = new LinkedHashSet<RowKey>();
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

    
    /**
     * The x position is the center of the distance between the two subnodes or
     * the position of the leaf node on the x axis.
     * 
     * @param node the node to determine the mapped x position for
     * @return the x position of the visual model for the passed node
     */
    private int getXPosition(final DendrogramNode node) {
        if (node.isLeaf()) {
            DataCell value = new StringCell(
                    node.getLeafDataPoint().getKey().getString());
            return (int)getXAxis().getCoordinate().calculateMappedValue(
                    value, getDrawingPaneDimension().width);
        }
        return (getXPosition(node.getFirstSubnode()) + getXPosition(
                node.getSecondSubnode())) / 2;
    }
    
    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteAll(final KeyEvent event) {
        updatePaintModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {
        updatePaintModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        updatePaintModel();
    }
}
