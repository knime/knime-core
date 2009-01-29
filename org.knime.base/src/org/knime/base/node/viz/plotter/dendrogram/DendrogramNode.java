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
 * ---------------------------------------------------------------------
 * 
 * History
 *   16.05.2007 (thor): created
 */
package org.knime.base.node.viz.plotter.dendrogram;

import org.knime.core.data.DataRow;

/**
 * This interface describes a node depicted in a dendrogram plot.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public interface DendrogramNode {
    /**
     * Returns the first sub-node of this node. This method is implemented
     * because of the binary characteristic of this tree.
     * 
     * @return the first sub node
     */
    public DendrogramNode getFirstSubnode();

    /**
     * Returns the distance between the two children nodes on the next level.
     * Must return 0 for leaf nodes.
     * 
     * @return the distance to the next level.
     */
    public double getDist();

    /**
     * Returns if this node is a leaf.
     * 
     * @return <code>true</code> if the node is a leaf node,
     *         <code>false</code> otherwise
     */
    public boolean isLeaf();

    /**
     * Returns the maximum distance from this node to any of the leafs. Must be
     * 0 for leaf nodes.
     * 
     * @return the maximum distance to a leaf node
     */
    public double getMaxDistance();

    /**
     * Returns the DataRow associated with a leaf node.
     * 
     * @return the leaf data point or <code>null</code> if this node is not a
     *         leaf
     */
    public DataRow getLeafDataPoint();

    /**
     * Returns the second sub-node of this node. This method is implemented
     * because of the binary characteristic of this tree.
     * 
     * @return the second sub node
     */
    public DendrogramNode getSecondSubnode();
}
