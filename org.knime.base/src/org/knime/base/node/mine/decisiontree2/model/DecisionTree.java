/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   26.10.2005 M. Berthold: created
 */
package org.knime.base.node.mine.decisiontree2.model;

import java.io.Serializable;
import java.util.LinkedHashMap;

import org.knime.base.node.mine.decisiontree2.PMMLMissingValueStrategy;
import org.knime.base.node.mine.decisiontree2.PMMLNoTrueChildStrategy;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.util.Pair;


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

    private PMMLMissingValueStrategy m_missingValueStrategy;
    private PMMLNoTrueChildStrategy m_noTrueChildStrategy;

    private String m_colorColumn;

    /**
     * Create DecisionTree based on a root node to which the remainder of the
     * tree is already attached.
     *
     * @param rootNode the node to attach everything to
     * @param classifyColumn name of attribute to classify with this tree
     * @param mvStrategy the strategy to to apply in case of missing values
     * @param ntcStrategy the strategy to apply when no branch is found
     */
    public DecisionTree(final DecisionTreeNode rootNode,
            final String classifyColumn,
            final PMMLMissingValueStrategy mvStrategy,
            final PMMLNoTrueChildStrategy ntcStrategy) {
        this(rootNode, classifyColumn, null);
        m_missingValueStrategy = mvStrategy;
        m_noTrueChildStrategy = ntcStrategy;
        if (rootNode instanceof DecisionTreeNodeSplit) {
            DecisionTreeNodeSplit splitNode = (DecisionTreeNodeSplit)rootNode;
            splitNode.setMVStrategy(m_missingValueStrategy);
            splitNode.setNTCStrategy(m_noTrueChildStrategy);
        }
    }

    /**
     * Create DecisionTree based on a root node to which the remainder of the
     * tree is already attached.
     *
     * @param rootNode the node to attach everything to
     * @param classifyColumn name of attribute to classify with this tree
     * @param mvStrategy the strategy to to apply in case of missing values
     */
    public DecisionTree(final DecisionTreeNode rootNode,
            final String classifyColumn,
            final PMMLMissingValueStrategy mvStrategy) {
        this(rootNode, classifyColumn);
        m_missingValueStrategy = mvStrategy;
        if (rootNode instanceof DecisionTreeNodeSplit) {
            DecisionTreeNodeSplit splitNode = (DecisionTreeNodeSplit)rootNode;
            splitNode.setMVStrategy(m_missingValueStrategy);
        }
    }

    /**
     * Create DecisionTree based on a root node to which the remainder of the
     * tree is already attached.
     *
     * @param rootNode the node to attach everything to
     * @param classifyColumn name of attribute to classify with this tree
     */
    public DecisionTree(final DecisionTreeNode rootNode,
            final String classifyColumn) {
        m_rootNode = rootNode;
        m_classifyColumn = classifyColumn;
        m_missingValueStrategy = PMMLMissingValueStrategy.getDefault();
        m_noTrueChildStrategy = PMMLNoTrueChildStrategy.getDefault();
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
     * Delegates to
     * {@link DecisionTreeNode#getWinnerAndClasscounts(DataRow, DataTableSpec)}
     */
    public final Pair<DataCell, LinkedHashMap<DataCell, Double>>
            getWinnerAndClasscounts(final DataRow row, final DataTableSpec spec)
            throws Exception {
        return m_rootNode.getWinnerAndClasscounts(row, spec);
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
        pConf.addString("color_column", m_colorColumn);
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
        // added in v2.3
        m_colorColumn = pConf.getString("color_column", null);
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
            node = node.getChildAt(0);
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

    /**
     * @return the missing value strategy
     */
    public PMMLMissingValueStrategy getMVStrategy() {
        return m_missingValueStrategy;
    }

    /**
     * @return the noTrueChildStrategy
     */
    public PMMLNoTrueChildStrategy getNTCStrategy() {
        return m_noTrueChildStrategy;
    }

    /**
     * @return the colorColumn
     */
    public String getColorColumn() {
        return m_colorColumn;
    }

    /**
     * @param colorColumn the colorColumn to set
     */
    public void setColorColumn(final String colorColumn) {
        m_colorColumn = colorColumn;
    }


}
