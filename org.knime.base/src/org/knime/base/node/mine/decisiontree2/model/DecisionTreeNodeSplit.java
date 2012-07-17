/*
 *
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   28.09.2005 (User): created
 */
package org.knime.base.node.mine.decisiontree2.model;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.swing.tree.TreeNode;

import org.knime.base.data.util.DataCellStringMapper;
import org.knime.base.node.mine.decisiontree2.PMMLMissingValueStrategy;
import org.knime.base.node.mine.decisiontree2.PMMLNoTrueChildStrategy;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.Node;

/**
 * An abstract implementation of an inner node of a decision tree, i.e. one that
 * is not a leaf. It mostly holds information about children.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class DecisionTreeNodeSplit extends DecisionTreeNode {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DecisionTreeNodeSplit.class);

    /** Returns the name of the field the predicate operates on, an empty String
    * if it works on multiple attributes, or null if not applicable.
    * (Can only be empty for compound predicates operating on multiple
    * attributes. */
    private String m_splitAttr;

    private DecisionTreeNode[] m_child;

    private int[] m_childIndex;
    private PMMLMissingValueStrategy m_mvStrategy;
    private PMMLNoTrueChildStrategy m_ntcStrategy;

    /**
     * Empty Constructor visible only within package.
     */
    DecisionTreeNodeSplit() {
    }

    /**
     * Constructor of derived class. Read all type-specific information from XML
     * File.
     *
     * @param xmlNode XML node info
     * @param mapper map translating column names to {@link DataCell}s and vice
     *            versa
     */
    public DecisionTreeNodeSplit(final Node xmlNode,
            final DataCellStringMapper mapper) {
        super(xmlNode, mapper); // let super read all type-invariant info
        makeRoomForKids(2);
        // now read information related to a split on a continuous attribute
        Node splitNode = xmlNode.getChildNodes().item(3);
        assert splitNode.getNodeName().equals("SPLIT");
        String splitAttr =
                splitNode.getAttributes().getNamedItem("attribute")
                        .getNodeValue();
        m_splitAttr = mapper.stringToOrigString(splitAttr);
    }

    /**
     * Constructor of base class. The necessary data is provided directly in the
     * constructor.
     *
     * @param nodeId the id of this node
     * @param majorityClass the majority class of the records in this node
     * @param classCounts the class distribution of the data in this node
     * @param splitAttribute the attribute name on which to split
     */
    protected DecisionTreeNodeSplit(final int nodeId,
            final DataCell majorityClass,
            final LinkedHashMap<DataCell, Double> classCounts,
            final String splitAttribute) {
        super(nodeId, majorityClass, classCounts);
        makeRoomForKids(2);
        // now read information related to a split on an attribute
        m_splitAttr = splitAttribute;
    }

    /**
     * Return name of attribute this node splits on.
     *
     * @return string the name of the column used for the split
     */
    public String getSplitAttr() {
        return m_splitAttr;
    }

    /**
     * Reserve space for specific number of kids.
     *
     * @param nrKids number of children attached to this node
     */
    protected void makeRoomForKids(final int nrKids) {
        m_childIndex = new int[nrKids];
        m_child = new DecisionTreeNode[nrKids];
    }

    /**
     * Mark index of child node at a specific branch.
     *
     * @param pos position of branch at this node
     * @param index index of child node
     */
    protected void setChildNodeIndex(final int pos, final int index) {
        assert m_childIndex != null;
        assert pos >= 0 && pos < m_childIndex.length;
        m_childIndex[pos] = index;
    }

    /**
     * Return DecisionTreeNode at specific branch.
     *
     * @param pos position of branch
     * @return node attached to this branch
     */
    protected DecisionTreeNode getChildNodeAt(final int pos) {
        assert m_child != null;
        assert pos >= 0 && pos < m_child.length;
        return m_child[pos];
    }

    /**
     * Add the given node to this node at the given branch index.
     *
     * @param node node to be inserted
     * @param index of the child array where to insert the given node
     */
    public void addNode(final DecisionTreeNode node, final int index) {
        m_child[index] = node;
    }

    /**
     * Add a new node to the tree structure based on a depth-first indexing
     * strategy.
     *
     * @param node node to be inserted
     * @param ix index of this node in depth first traversal order
     * @return true only if the node was successfully inserted
     */
    @Override
    public boolean addNodeToTreeDepthFirst(final DecisionTreeNode node,
            final int ix) {
        assert m_childIndex != null;
        if (ix < m_childIndex[0]) {
            // can't be true, all lower indices should already have been used
            return false;
        }
        int kidpos = 0;
        while (kidpos < m_childIndex.length) {
            if (ix == m_childIndex[kidpos]) {
                // node is (hopefully first and only) child for this position
                if (m_child[kidpos] != null) {
                    return false;
                }
                m_child[kidpos] = node;
                m_childIndex[kidpos] = ix;
                m_child[kidpos].setParent(this);
                return true;
            }
            if ((kidpos < m_childIndex.length - 1)
                    && (ix < m_childIndex[kidpos + 1])) {
                // node's index is below next kid's, has to fit into this branch
                if (m_child[kidpos] == null) {
                    return false;
                }
                return m_child[kidpos].addNodeToTreeDepthFirst(node, ix);
            }
            kidpos++;
        }
        int rightMostKid = m_childIndex.length - 1;
        assert (ix > m_childIndex[rightMostKid]);
        // node must fit into the rightmost branch
        if (m_child[rightMostKid] == null) {
            return false;
        }
        return m_child[rightMostKid].addNodeToTreeDepthFirst(node, ix);
    }

    // remember prev table spec and index used for classification to save time
    private transient DataTableSpec m_previousSpec = null;

    private transient int m_previousIndex = -1;

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedHashMap<DataCell, Double> getClassCounts(
            final DataRow row,
            final DataTableSpec spec) {
        assert (spec != null);
        if (m_splitAttr != null && !m_splitAttr.isEmpty()) {
            if (spec != m_previousSpec) {
                m_previousIndex = spec.findColumnIndex(m_splitAttr);
                m_previousSpec = spec;
            }
            assert (m_previousIndex != -1);
            DataCell cell = row.getCell(m_previousIndex);
            if (cell.isMissing()) {
                // we can not determine the split at this node because value is missing
                return getNodeClassWeights();
            }
            return getClassCounts(cell, row, spec);
        }
        return getClassCounts(null, row, spec);
    }

    /**
     * Combines all class weights from _all_ branches of this node.
     * @return the combined class weights.
     */
    protected LinkedHashMap<DataCell, Double> getNodeClassWeights() {
        // initialize result HashMap
        LinkedHashMap<DataCell, Double> result =
                new LinkedHashMap<DataCell, Double>();
        // check each branch for it's counts and add them up
        for (DecisionTreeNode nodeIt : m_child) {
            LinkedHashMap<DataCell, Double> thisNodeCounts =
                    nodeIt.getClassCounts();
            for (DataCell cellIt : thisNodeCounts.keySet()) {
                // if entry for this class already exist, modify
                // value, otherwise insert new one
                if (result.containsKey(cellIt)) {
                    double newCount =
                            thisNodeCounts.get(cellIt)
                                    + result.get(cellIt);
                    result.remove(cellIt);
                    result.put(cellIt, newCount);
                } else {
                    result.put(cellIt, thisNodeCounts.get(cellIt));
                }
            }
        }
        // return result
        return result;
    }

    /**
     * Determine class counts for a new pattern given as a row of values.
     * Returns a HashMap listing counts for all classes.
     *
     * @param cell the call to be used for classification at this node
     * @param row input pattern
     * @param spec the corresponding table spec
     * @return HashMap class/count
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    public abstract LinkedHashMap<DataCell, Double> getClassCounts(
            final DataCell cell, final DataRow row, final DataTableSpec spec);

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addCoveredPattern(final DataRow row,
            final DataTableSpec spec, final double weight) throws Exception {
        assert (spec != null);
        if (m_splitAttr != null && !m_splitAttr.isEmpty()) {
            if (spec != m_previousSpec) {
                m_previousIndex = spec.findColumnIndex(m_splitAttr);
                if (m_previousIndex == -1) {
                    LOGGER.error(spec.toString());
                    throw new Exception("Decision Tree Prediction failed."
                            + " Could not find attribute '"
                            + m_splitAttr + "'");
                }
                m_previousSpec = spec;
            }
            assert (m_previousIndex != -1);
            DataCell cell = row.getCell(m_previousIndex);
            if (cell.isMissing()) {
                // if we can not determine the split at this node because
                // value is missing, we add the row to each child
                // with the weight proportional to the number of
                // records belonging to each node
                double allOverCount = getEntireClassCount();
                for (DecisionTreeNode child : m_child) {
                    double newWeight =
                            (child.getEntireClassCount() / allOverCount)
                                    * weight;
                    child.addCoveredPattern(row, spec, newWeight);
                }
                return;
            }
            addCoveredPattern(cell, row, spec, weight);
            return;
        }
        addCoveredPattern(null, row, spec, weight);
    }

    /**
     * Add patterns given as a row of values if they fall within a specific
     * node. Usually only Leafs will actually hold a list of RowKeys, all
     * intermediate nodes will collect "their" information recursively.
     *
     * @param cell the cell to be used for classification at this node
     * @param row input pattern
     * @param spec the corresponding table spec
     * @param weight the weight of this row (between 0.0 and 1.0)
     * @throws Exception if something went wrong (unknown attribute for example)
     */
    public abstract void addCoveredPattern(DataCell cell, DataRow row,
            DataTableSpec spec, double weight) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addCoveredColor(final DataRow row,
            final DataTableSpec spec, final double weight) throws Exception {
        assert (spec != null);
        if (m_splitAttr != null && !m_splitAttr.isEmpty()) {
            if (spec != m_previousSpec) {
                m_previousIndex = spec.findColumnIndex(m_splitAttr);
                if (m_previousIndex == -1) {
                    LOGGER.error(spec.toString());
                    throw new Exception("Decision Tree Prediction failed."
                            + " Could not find attribute '"
                            + m_splitAttr + "'");
                }
                m_previousSpec = spec;
            }
            assert (m_previousIndex != -1);
            DataCell cell = row.getCell(m_previousIndex);
            if (cell.isMissing()) {
                // of we can not determine the split at this node because
                // value is missing, we add the row to each child
                // with the weight proportional to the number of
                // records belonging to each node
                double allOverCount = getEntireClassCount();
                for (DecisionTreeNode child : m_child) {
                    double newWeight =
                            (child.getEntireClassCount() / allOverCount)
                                    * weight;
                    child.addCoveredColor(row, spec, newWeight);
                }
                return;
            }
            addCoveredColor(cell, row, spec, weight);
            return;
        }
        addCoveredColor(null, row, spec, weight);
    }

    /**
     * Add colors for patterns given as a row of values if they fall within a
     * specific node.
     *
     * @param cell the call to be used for classification at this node
     * @param row input pattern
     * @param spec the corresponding table spec
     * @param weight the weight of this row (between 0.0 and 1.0)
     * @throws Exception if something went wrong (unknown attribute for example)
     */
    public abstract void addCoveredColor(DataCell cell, DataRow row,
            DataTableSpec spec, double weight) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Set<RowKey> coveredPattern();

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void saveNodeInternalsToPredParams(final ModelContentWO pConf,
            final boolean saveKeysAndPatterns) {
        saveNodeSplitInternalsToPredParams(pConf);
        pConf.addString("splitAttribute", m_splitAttr);
        pConf.addInt("nrChildren", m_child.length);
        for (int i = 0; i < m_child.length; i++) {
            ModelContentWO newChildConf = pConf.addModelContent("child" + i);
            newChildConf.addInt("index", m_childIndex[i]);
            m_child[i].saveToPredictorParams(newChildConf, saveKeysAndPatterns);
        }
    }

    /**
     * save internal SplitNode settings to a ModelContent object.
     *
     * @param pConf configuration object to save decision tree to
     */
    public abstract void saveNodeSplitInternalsToPredParams(
            final ModelContentWO pConf);

    /**
     * {@inheritDoc}
     */
    @Override
    public final void loadNodeInternalsFromPredParams(
            final ModelContentRO pConf)
            throws InvalidSettingsException {
        loadNodeSplitInternalsFromPredParams(pConf);
        m_splitAttr = pConf.getString("splitAttribute");
        int nrKids = pConf.getInt("nrChildren");
        m_child = new DecisionTreeNode[nrKids];
        m_childIndex = new int[nrKids];
        for (int i = 0; i < nrKids; i++) {
            ModelContentRO newChildConf = pConf.getModelContent("child" + i);
            m_child[i] =
                    DecisionTreeNode.createNodeFromPredictorParams(
                            newChildConf, this);
           // int kidIndex = newChildConf.getInt("index");
//            if (!getPrefix().equals("root")
//                    && kidIndex != m_child[i].getOwnIndex()) {
//               throw new InvalidSettingsException("DecisionTreeNode: Expected"
//                        + " index does not match real index: " + kidIndex
//                        + " != " + m_child[i].getOwnIndex());
//            }
        }

        // no need to store or load these, since they are only used to
        // speed up subsequent access to the same attribute:
        m_previousSpec = null;
        m_previousIndex = -1;
    }

    /**
     * Load internal SplitNode settings from a ModelContent object.
     *
     * @param pConf configuration object to load decision tree from.
     * @throws InvalidSettingsException if something goes wrong
     */
    public abstract void loadNodeSplitInternalsFromPredParams(
            final ModelContentRO pConf) throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    public int getChildCount() {
        if (m_childIndex == null) {
            return 0;
        }
        return m_childIndex.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndex(final TreeNode node) {
        if (m_childIndex == null) {
            // no kids!
            return -1;
        }
        for (int i = 0; i < m_child.length; i++) {
            if (node == m_child[i]) {
                // return position as index if node is one of the kids
                return i;
            }
        }
        // node not found:
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DecisionTreeNode getChildAt(final int pos) {
        assert m_child != null;
        assert pos >= 0 && pos < m_child.length;
        return m_child[pos];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLeaf() {
        return m_child == null;
    }

    /**
     * @return enumeration of all children
     */
    @Override
    public Enumeration<DecisionTreeNode> children() {
        // TODO: fix. JTree doesn't seem to need it, luckily.
        /*
         * if (m_leftChild != null) { children.add(m_leftChild); } if
         * (m_rightChild != null) { children.add(m_rightChild); } return
         * children; Vector children = new Vector();
         */
        return null;
    }

    /**
     * Returns the children.
     *
     * @return the children
     */
    public DecisionTreeNode[] getChildren() {
        return m_child;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getAllowsChildren() {
        return m_child != null;
    }

    /**
     * Replace the given child by the new given one.
     *
     * @param oldNode the node to replace
     * @param newNode the new node
     */
    public void replaceChild(final DecisionTreeNode oldNode,
            final DecisionTreeNode newNode) {

        int count = 0;
        for (DecisionTreeNode child : m_child) {
            if (child == oldNode) {
                m_child[count] = newNode;
            }
            count++;
        }
    }

    /**
     * Removes the children at the given indices
     * .
     * @param indices the indices of the children to remove
     */
    public void removeChildren(final Set<Integer> indices) {
        DecisionTreeNode[] children
                = new DecisionTreeNode[m_child.length - indices.size()];
        int[] childIndex =
            new int[m_childIndex.length - indices.size()];

        int pos = 0;
        for (int i = 0; i < m_child.length; i++) {
            if (!indices.contains(i)) {
                children[pos] = m_child[i];
                childIndex[pos] = m_childIndex[i];
                pos++;
            }
        }
        m_child = children;
        m_childIndex = childIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCountOfSubtree() {
        int childCounts = 0;
        for (DecisionTreeNode node : m_child) {
            childCounts += node.getCountOfSubtree();
        }

        return childCounts + 1;
    }

    /**
     * @return the missing value strategy
     */
    public PMMLMissingValueStrategy getMVStrategy() {
        return m_mvStrategy;
    }

    /**
     * Set the missing value strategy and propagate it to all children that
     * are split nodes.
     * @param strategy the strategy to set
     */
    public void setMVStrategy(final PMMLMissingValueStrategy strategy) {
        m_mvStrategy = strategy;
        for (DecisionTreeNode child : m_child) {
            if (child instanceof DecisionTreeNodeSplit) {
                ((DecisionTreeNodeSplit)child).setMVStrategy(strategy);
            }
        }
    }

    /**
     * @return the the no true child strategy
     */
    public PMMLNoTrueChildStrategy getNTCStrategy() {
        return m_ntcStrategy;
    }

    /**
     * Set the no true child strategy and propagate it to all children that are
     * split nodes.
     *
     * @param ntcStrategy the strategy to set
     */
    public void setNTCStrategy(final PMMLNoTrueChildStrategy ntcStrategy) {
        m_ntcStrategy = ntcStrategy;
        for (DecisionTreeNode child : m_child) {
            if (child instanceof DecisionTreeNodeSplit) {
                ((DecisionTreeNodeSplit)child).setNTCStrategy(ntcStrategy);
            }
        }
    }
}
