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
                int dotSize = (Integer)props.getDotSizeSpinner().getValue();
                setDotSize(dotSize);
                updateSize();
                getDrawingPane().repaint();
            }
        });
        
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void setDotSize(final int dotSize) {
        ((DendrogramDrawingPane)getDrawingPane()).setDotSize(dotSize);
        super.setDotSize(dotSize);
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
        int y = getMappedYValue(new DoubleCell(node.getDist())); 
        int x;
        DendrogramPoint p;
        if (!node.isLeaf()) {
            x = getXPosition(node);
            p = new DendrogramPoint(new Point(x, y), 
                    node.getDist());
        } else {
            DataRow row = node.getLeafDataPoint();
            x = getMappedXValue(new StringCell(row.getKey().getString()));
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
            return getMappedXValue(value);
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
