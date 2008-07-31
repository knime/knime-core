/* 
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
 *   26.10.2005 M. Berthold: created
 */
package org.knime.base.node.mine.decisiontree.predictor.decisiontree;

import java.io.Serializable;
import java.util.HashMap;

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
 */
public class DecisionTree implements Serializable {

    private static final long serialVersionUID = 42L;

    private DecisionTreeNode m_rootNode;

    /**
     * Create DecisionTree based on a root node to which the remainder of the
     * tree is already attached.
     * 
     * @param rootNode the node to attach everything to
     */
    public DecisionTree(final DecisionTreeNode rootNode) {
        m_rootNode = rootNode;
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
        m_rootNode.addCoveredPattern(row, spec);
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
        m_rootNode.addCoveredColor(row, spec);
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
    public HashMap<DataCell, Double> getClassCounts(final DataRow row,
            final DataTableSpec spec) throws Exception {
        return m_rootNode.getClassCounts(row, spec);
    }

    // ///////////////////
    // Save & Load
    // ///////////////////

    /**
     * Save decision tree to a ModelContent object.
     * 
     * @param pConf configuration object to attach decision tree to
     */
    public void saveToPredictorParams(final ModelContentWO pConf) {
        pConf.addString("type", "DecisionTree");
        pConf.addString("version", "0.0");
        ModelContentWO newNodeConf = pConf.addModelContent("rootNode");
        m_rootNode.saveToPredictorParams(newNodeConf);
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
}
