/*
 *
 * -------------------------------------------------------------------
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
 *   23.07.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree2.model;

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
import org.knime.core.node.config.Config;
import org.w3c.dom.Node;

/**
 * The Leaf of a decision tree. It stores class information and also some
 * information about the patterns this leaf "coveres".
 *
 * @author Michael Berthold, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class DecisionTreeNodeLeaf extends DecisionTreeNode {

    private static final String CONFIG_KEY_PATTERNS = "patterns";

    private static final String CONFIG_KEY_PATTERN = "pattern";

    private HashSet<RowKey> m_coveredPattern = new HashSet<RowKey>();

    private boolean m_pureEnough = false;

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
     * Constructor of base class. The necessary data is provided directly in the
     * constructor.
     *
     * @param nodeId the id of this node
     * @param majorityClass the majority class of the records in this node
     * @param classCounts the class distribution of the data in this node
     */
    public DecisionTreeNodeLeaf(final int nodeId, final DataCell majorityClass,
            final HashMap<DataCell, Double> classCounts) {

        // everything is done in the super constructor up to now
        super(nodeId, majorityClass, classCounts);
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
     * @param weight the weight of the row (between 0.0 and 1.0)
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    @Override
    public final void addCoveredPattern(final DataRow row,
            final DataTableSpec spec, final double weight) throws Exception {
        m_coveredPattern.add(row.getKey());
        addCoveredColor(row, spec, weight);
    }

    /**
     * Add colors for a pattern given as a row of values. This is a leaf so we
     * will simply add the color to our list.
     *
     * @param row input pattern
     * @param spec the corresponding table spec
     * @param weight the weight of the row (between 0.0 and 1.0)
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    @Override
    public final void addCoveredColor(final DataRow row,
            final DataTableSpec spec, final double weight) throws Exception {
        Color col = spec.getRowColor(row).getColor();
        addColorToMap(col, weight);
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
    public void saveNodeInternalsToPredParams(final ModelContentWO pConf,
            final boolean saveKeysAndPatterns) {

        // if the keys and colors are supposed to be stored
        if (saveKeysAndPatterns) {
            Config patternsConfig = pConf.addConfig(CONFIG_KEY_PATTERNS);
            int counter = 0;
            for (RowKey entry : m_coveredPattern) {
                patternsConfig.addString(CONFIG_KEY_PATTERN + "_" + counter,
                        entry.getString());
                counter++;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadNodeInternalsFromPredParams(final ModelContentRO pConf)
            throws InvalidSettingsException {

        // if the keys and colors are available for loadeding
        if (pConf.containsKey(CONFIG_KEY_PATTERNS)) {
            Config patternsConfig = pConf.getConfig(CONFIG_KEY_PATTERNS);
            m_coveredPattern.clear();
            for (String key : patternsConfig) {
                RowKey keyCell;
                try {
                    keyCell = new RowKey(
                            patternsConfig.getDataCell(key).toString());
                } catch (InvalidSettingsException ise) {
                    keyCell = new RowKey(patternsConfig.getString(key));
                }
                m_coveredPattern.add(keyCell);
            }
        }
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCountOfSubtree() {
        return 1;
    }

    /**
     * Get the marker field for the purity.
     *
     * @return whether this leaf is pure enough or not; must have been set
     * properly
     */
    public boolean isPureEnough() {
        return m_pureEnough;
    }

    /**
     * Set the marker field for the purity.
     * @param pureEnough true if this leaf should be marked as pure enough
     */
    public void setPureEnough(final boolean pureEnough) {
        this.m_pureEnough = pureEnough;
    }
}
