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
 */

package org.knime.base.node.mine.cluster.hierarchical;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.plotter.dendrogram.DendrogramNode;
import org.knime.core.data.DataRow;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Represents a Node in the hierarchy tree (Dendrogram) of a hierarchical
 * clustering.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class ClusterNode implements DendrogramNode {

    /**
     * The data row which represents the leaf of a hierarchy tree. Is null if
     * this node is not a leaf.
     */
    private DataRow m_leafDataPoint;

    private int m_rowIdx = -1;

    private double m_dist;

    /**
     * Holds the child nodes of a hierarchy sub tree. Is empty if this node is
     * a leaf node. In this implementation a parent has two child nodes. (Binary
     * tree)
     */
    private ClusterNode[] m_hierarchieNodes;

    /**
     * Indicates whether this is a leaf node or not.
     */
    private boolean m_isLeaf;

    /**
     * Constructs a new leaf node from a data row.
     *
     * @param row data row to create a node for
     * @param rowIdx the row index for later reconstruction in load/save
     *            internals.
     */
    public ClusterNode(final DataRow row, final int rowIdx) {
        m_rowIdx = rowIdx;
        m_leafDataPoint = row;
        m_isLeaf = true;
    }


    /**
     * Returns the index of the row stored inside this leaf node.
     *
     * @return the index of the row if the node is a leaf node -1 otherwise.
     */
    public int getRowIndex() {
        return m_rowIdx;
    }

    /**
     * Constructs a new parent node from two child nodes.
     *
     * @param node1 the first node to create a parent node for
     * @param node2 the second node to create a parent node for
     * @param dist the distance to the node.
     */
    public ClusterNode(final ClusterNode node1, final ClusterNode node2,
            final double dist) {
        m_hierarchieNodes = new ClusterNode[2];
        m_hierarchieNodes[0] = node1;
        m_hierarchieNodes[1] = node2;
        m_dist = dist;

    }

    /**
     * Returns all data row (leaf nodes) this sub tree.
     *
     * @return the array of data rows which are included in this sub tree.
     */
    public DataRow[] getAllDataRows() {

        // if already a leaf node
        if (m_isLeaf) {

            DataRow[] rows = {m_leafDataPoint};

            return rows;
        }

        // else recursively get the rows

        List<DataRow> rowVector = new ArrayList<DataRow>();
        getDataRows(this, rowVector);

        DataRow[] rows = new DataRow[rowVector.size()];
        rows = rowVector.toArray(rows);

        return rows;
    }

    /**
     * Puts all data rows of a node in a vector if the node is a leaf. Otherwise
     * the method is invoked with the nodes child nodes. This is a recursive
     * tree traversing method.
     *
     * @param clusterNode the node to get the data rows from.
     * @param rowVector the vector to store the found data rows in.
     */
    private void getDataRows(final DendrogramNode clusterNode,
            final List<DataRow> rowVector) {

        // rekursives auslesen aller data rows
        DendrogramNode subNode1 = clusterNode.getFirstSubnode();
        DendrogramNode subNode2 = clusterNode.getSecondSubnode();

        if (subNode1.isLeaf()) {

            rowVector.add(subNode1.getLeafDataPoint());
        } else {

            getDataRows(subNode1, rowVector);
        }

        if (subNode2.isLeaf()) {

            rowVector.add(subNode2.getLeafDataPoint());
        } else {

            getDataRows(subNode2, rowVector);
        }

    }

    /**
     * Returns an Iterable over all leaf nodes contained in this node.
     *
     * @return an iterable over leaf nodes
     */
    public Iterable<ClusterNode> leafs() {
        return new Iterable<ClusterNode>() {
            public Iterator<ClusterNode> iterator() {
                return new LeafIterator();
            }
        };
    }

    /**
     * Returns the number of leaf nodes contained in this node. Leaf nodes
     * return 1 by definition.
     *
     * @return the number of leaf nodes
     */
    public int getLeafCount() {
        if (m_isLeaf) {
            return 1;
        } else {
            return getFirstSubnode().getLeafCount()
                    + getSecondSubnode().getLeafCount();
        }
    }

    /**
     * {@inheritDoc}
     */
    public ClusterNode getFirstSubnode() {
        if (m_hierarchieNodes == null) {
            return null;
        }
        return m_hierarchieNodes[0];
    }

    /**
     * {@inheritDoc}
     */
    public double getDist() {
        if (isLeaf()) {
            return 0;
        }
        return m_dist;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLeaf() {
        return m_isLeaf;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaxDistance() {
        if ((getFirstSubnode() == null) && (getSecondSubnode() == null)) {
            return m_dist;
        }
        double dist1 = getFirstSubnode().getMaxDistance();
        double dist2 = getSecondSubnode().getMaxDistance();
        double dist3 = Math.max(dist1, dist2);
        return Math.max(dist3, getDist());
    }

    /**
     * {@inheritDoc}
     */
    public DataRow getLeafDataPoint() {
        return m_leafDataPoint;
    }

    /**
     * {@inheritDoc}
     */
    public ClusterNode getSecondSubnode() {
        if (m_hierarchieNodes == null) {
            return null;
        }
        return m_hierarchieNodes[1];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("dist: " + m_dist);
        if (m_isLeaf) {
            buffer.append("leaf: " + m_leafDataPoint.getKey().getString());
        } else {
            buffer.append("left: " + getFirstSubnode() + " right: "
                    + getSecondSubnode());
        }
        return buffer.toString();
    }

    private static final String CFG_DISTANCE = "distance";

    private static final String CFG_LEAF = "isLeaf";

    private static final String CFG_LEFTCHILD = "leftChild";

    private static final String CFG_RIGHTCHILD = "rightChild";

    private static final String CFG_ROW_IDX = "row";

    private ClusterNode() {

    }

    /**
     * Saves the tree structure into the config. Stores the distance, the rowy
     * key (if its a leaf) and the left and right child.
     *
     * @param settings the config to save to.
     */
    public void saveToXML(final NodeSettingsWO settings) {
        // each node stores its distance
        settings.addDouble(CFG_DISTANCE, m_dist);
        settings.addBoolean(CFG_LEAF, m_isLeaf);
        // if leaf node store the referring data point
        if (isLeaf()) {
            // TODO: store the whole data row
            settings.addInt(CFG_ROW_IDX, m_rowIdx);
        }
        // and the left and the right child
        NodeSettingsWO left = (NodeSettingsWO)settings.addConfig(CFG_LEFTCHILD);
        NodeSettingsWO right =
                (NodeSettingsWO)settings.addConfig(CFG_RIGHTCHILD);
        if (getFirstSubnode() != null && getSecondSubnode() != null) {
            getFirstSubnode().saveToXML(left);
            getSecondSubnode().saveToXML(right);
        }
    }

    /**
     * Loads a cluster node from the settings.
     *
     * @param settings the config to load from
     * @param orgTable the original table containing the rows in the same order!
     * @return a cluster node
     * @throws InvalidSettingsException if not stored properly.
     */
    public static ClusterNode loadFromXML(final NodeSettingsRO settings,
            final DataArray orgTable) throws InvalidSettingsException {
        ClusterNode node = new ClusterNode();
        double dist = settings.getDouble(CFG_DISTANCE);
        node.m_dist = dist;
        boolean isLeaf = settings.getBoolean(CFG_LEAF);
        node.m_isLeaf = isLeaf;
        node.m_hierarchieNodes = new ClusterNode[2];
        if (isLeaf) {
            node.m_isLeaf = true;
            int rowIdx = settings.getInt(CFG_ROW_IDX);
            node.m_leafDataPoint = orgTable.getRow(rowIdx);
            node.m_rowIdx = rowIdx;
        } else {
            NodeSettingsRO leftSettings =
                    settings.getNodeSettings(CFG_LEFTCHILD);
            NodeSettingsRO rightSettings =
                    settings.getNodeSettings(CFG_RIGHTCHILD);
            node.m_hierarchieNodes[0] = loadFromXML(leftSettings, orgTable);
            node.m_hierarchieNodes[1] = loadFromXML(rightSettings, orgTable);
        }
        return node;
    }

    private static final List<ClusterNode> EMPTY_STACK =
            new ArrayList<ClusterNode>();

    private class LeafIterator implements Iterator<ClusterNode> {
        private final List<ClusterNode> m_stack;

        private ClusterNode m_nextLeaf;

        LeafIterator() {
            if (ClusterNode.this.isLeaf()) {
                m_stack = EMPTY_STACK;
                m_nextLeaf = ClusterNode.this;
            } else {
                m_stack = new ArrayList<ClusterNode>();
                m_stack.add(ClusterNode.this);
                findNextLeaf();
            }
        }

        private void findNextLeaf() {
            while (!m_stack.isEmpty()) {
                ClusterNode node = m_stack.remove(m_stack.size() - 1);
                if (!node.isLeaf()) {
                    m_stack.add(node.getFirstSubnode());
                    m_stack.add(node.getSecondSubnode());
                } else {
                    m_nextLeaf = node;
                    return;
                }
            }
            m_nextLeaf = null;
        }

        public boolean hasNext() {
            return m_nextLeaf != null;
        }

        public ClusterNode next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements");
            }

            ClusterNode leaf = m_nextLeaf;
            findNextLeaf();
            assert leaf.isLeaf();
            return leaf;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
