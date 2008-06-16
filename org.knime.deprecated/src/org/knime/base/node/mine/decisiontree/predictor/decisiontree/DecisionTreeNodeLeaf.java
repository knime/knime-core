/* 
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   23.07.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree.predictor.decisiontree;

import java.awt.Color;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.TreeNode;

import org.knime.base.data.util.DataCellStringMapper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.w3c.dom.Node;

/**
 * The Leaf of a decision tree. It stores class information and also some
 * information about the patterns this leaf "coveres".
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class DecisionTreeNodeLeaf extends DecisionTreeNode {
    private HashSet<RowKey> m_coveredPattern = new HashSet<RowKey>();

    private HashMap<Color, Double> m_coveredColors 
       = new HashMap<Color, Double>();

    /**
     * Empty Constructor visible only within package.
     */
    DecisionTreeNodeLeaf() {
    }

    /**
     * Constructor of derived class. Read all type-specific information from XML
     * File.
     * 
     * @param xmlNode XML node containing info
     * @param mapper map translating column names to {@link DataCell}s and vice
     *            versa
     */
    public DecisionTreeNodeLeaf(final Node xmlNode,
            final DataCellStringMapper mapper) {
        super(xmlNode, mapper); // let super read all type-invariant info
        // no additional information read at this time
    }

    /**
     * Add a new node to the tree structure based on a depth-first indexing
     * strategy.
     * 
     * @param node node to be inserted
     * @param ix index of this node in depth first traversal order
     * @return false always since this node is a leaf!
     */
    @Override
    public boolean addNodeToTreeDepthFirst(final DecisionTreeNode node,
            final int ix) {
        return false;
    }

    /**
     * Determine class counts for a new pattern given as a row of values.
     * Returns a HashMap listing counts for all classes.
     * 
     * @param row input pattern
     * @param spec the corresponding table spec
     * @return HashMap class/count
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    @Override
    public HashMap<DataCell, Double> getClassCounts(final DataRow row,
            final DataTableSpec spec) throws Exception {
        return getClassCounts();
    }

    /**
     * Add patterns given as a row of values. This is a leaf so we will simply
     * add the RowKey to our list of hiliteable rows.
     * 
     * @param row input pattern
     * @param spec the corresponding table spec
     * @throws Exception if something went wrong (unknown attribute for example)
     */
    @Override
    public final void addCoveredPattern(final DataRow row,
            final DataTableSpec spec) throws Exception {
        m_coveredPattern.add(row.getKey());
        addCoveredColor(row, spec);
    }

    /**
     * Add colors for a pattern given as a row of values.
     * This is a leaf so we will simply add the color to our list.
     * 
     * @param row input pattern
     * @param spec the corresponding table spec
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    @Override
    public final void addCoveredColor(final DataRow row,
            final DataTableSpec spec) throws Exception {
        Color col = spec.getRowColor(row).getColor();
        if (m_coveredColors.containsKey(col)) {
            Double oldCount = m_coveredColors.get(col);
            m_coveredColors.remove(col);
            m_coveredColors.put(col, new Double(oldCount.doubleValue() + 1.0));
        } else {
            m_coveredColors.put(col, new Double(1.0));
        }
    }

    /**
     * @return set of data cells which are the row keys that are covered by this
     *         leaf node
     */
    @Override
    public Set<RowKey> coveredPattern() {
        return m_coveredPattern;
    }

    /**
     * @return list of colors and coverage counts covered by this leaf node
     */
    @Override
    public HashMap<Color, Double> coveredColors() {
        return m_coveredColors;
    }

    /**
     * @return string summary of node content (class of leaf)
     */
    @Override
    public String getStringSummary() {
        return "class " + super.getMajorityClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveNodeInternalsToPredParams(final ModelContentWO pConf) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadNodeInternalsFromPredParams(final ModelContentRO pConf)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getChildCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndex(final TreeNode node) {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeNode getChildAt(final int pos) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLeaf() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<DecisionTreeNode> children() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getAllowsChildren() {
        return false;
    }
}
