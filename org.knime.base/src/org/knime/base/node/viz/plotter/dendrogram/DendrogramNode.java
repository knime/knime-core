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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
