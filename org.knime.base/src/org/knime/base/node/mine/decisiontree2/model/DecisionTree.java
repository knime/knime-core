/*
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
 *   26.10.2005 M. Berthold: created
 */
package org.knime.base.node.mine.decisiontree2.model;

import java.io.Serializable;
import java.util.LinkedHashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * A Wrapper for a decision tree, allowing for save/load to
 * {@link org.knime.core.node.ModelContent} objects.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class DecisionTree implements Serializable {

    private static final long serialVersionUID = 42L;

    private DecisionTreeNode m_rootNode;

    private String m_classifyColumn;

    /**
     * Create DecisionTree based on a root node to which the remainder of the
     * tree is already attached.
     *
     * @param rootNode the node to attach everything to
     * @param classifyColumn name of attribute to classify with this tree
     */
    public DecisionTree(final DecisionTreeNode rootNode, final String classifyColumn) {
        m_rootNode = rootNode;
        m_classifyColumn = classifyColumn;
    }

    /**
     * Create Decision Tree based on an ModelContent object.
     *
     * @param pConf configuration object to load decision tree from
     * @throws InvalidSettingsException if something goes wrong
     */
    public DecisionTree(final ModelContentRO pConf)
            throws InvalidSettingsException {
        this.loadFromPredictorParams(pConf);
    }

    /**
     * @return root node of tree
     */
    // TODO make protected... (or remove completely)
    public DecisionTreeNode getRootNode() {
        return m_rootNode;
    }

    /**
     * Classify a new pattern given as a row of values. Returns the class with
     * the maximum count.
     *
     * @param row input pattern
     * @param spec the corresponding table spec
     * @return class of pattern the decision tree predicts
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    public final DataCell classifyPattern(final DataRow row,
            final DataTableSpec spec) throws Exception {
        return m_rootNode.classifyPattern(row, spec);
    }

    /**
     * Add a new pattern to this tree for HiLiting purposes. Stores pattern ID
     * and color (if available) in the leaves.
     *
     * @param row input pattern
     * @param spec the corresponding table spec
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    public final void addCoveredPattern(final DataRow row,
            final DataTableSpec spec) throws Exception {
        m_rootNode.addCoveredPattern(row, spec, 1.0);
    }

    /**
     * Add color of a new pattern to this tree. Does NOT store pattern ID
     * but only it's color (if available) in the leaves.
     *
     * @param row input pattern
     * @param spec the corresponding table spec
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    public final void addCoveredColor(final DataRow row,
            final DataTableSpec spec) throws Exception {
        m_rootNode.addCoveredColor(row, spec, 1.0);
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
    public LinkedHashMap<DataCell, Double> getClassCounts(final DataRow row,
            final DataTableSpec spec) throws Exception {
        return m_rootNode.getClassCounts(row, spec);
    }
    
    /**
     * Clean all color information in the entire tree.
     */
    public void resetColorInformation() {
        m_rootNode.resetColorInformation();
    }

    // ///////////////////
    // Save & Load
    // ///////////////////

    /**
     * Save decision tree to a ModelContent object.
     *
     * @param pConf configuration object to attach decision tree to
     * @param saveKeysAndPatterns whether to save the keys and patterns
     */
    public void saveToPredictorParams(final ModelContentWO pConf,
            final boolean saveKeysAndPatterns) {
        pConf.addString("type", "DecisionTree");
        pConf.addString("version", "0.0");
        ModelContentWO newNodeConf = pConf.addModelContent("rootNode");
        m_rootNode.saveToPredictorParams(newNodeConf, saveKeysAndPatterns);
    }

    /**
     * Load Decision Tree from a ModelContent object.
     *
     * @param pConf configuration object to load decision tree from
     * @throws InvalidSettingsException if something goes wrong
     */
    public void loadFromPredictorParams(final ModelContentRO pConf)
            throws InvalidSettingsException {
        String type = pConf.getString("type");
        String version = pConf.getString("version");
        if (!type.equals("DecisionTree")) {
            throw new InvalidSettingsException("DecisionTree can not load"
                    + " information of type '" + type + "'!");
        }
        if (!version.equals("0.0")) {
            throw new InvalidSettingsException("DecisionTree v0.0 can not"
                    + " load information of version '" + type + "'!");
        }
        ModelContentRO newNodeConf = pConf.getModelContent("rootNode");
        m_rootNode = DecisionTreeNode.createNodeFromPredictorParams(
                newNodeConf, null);
    }

    /**
     * Returns the number decision tree nodes.
     *
     * @return the number decision tree nodes
     */
    public int getNumberNodes() {
        // traverse the tree and count the nodes
        return m_rootNode.getCountOfSubtree();

    }

    /**
     * Returns the first leaf according to a depth first traversal.
     *
     * @return the first leaf according to a depth first traversal
     */
    public DecisionTreeNodeLeaf getFirstLeafDFS() {

        DecisionTreeNode node = m_rootNode;
        while (!node.isLeaf()) {
            node = (DecisionTreeNode)node.getChildAt(0);
        }
        return (DecisionTreeNodeLeaf)node;
    }

    /**
     * Sets a new root node.
     *
     * @param root the new root to set
     */
    public void setRoot(final DecisionTreeNode root) {
        m_rootNode = root;
    }

    /**
     * @return name of attribute to classify with this tree
     */
    public String getClassifyColumn(){
        return m_classifyColumn;
    }
}
