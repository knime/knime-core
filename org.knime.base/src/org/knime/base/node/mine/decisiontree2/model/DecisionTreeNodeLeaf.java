/*
 *
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
 * -------------------------------------------------------------------
 *
 * History
 *   23.07.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree2.model;

import java.awt.Color;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

    private HashSet<RowKey> m_coveredPattern = 
        new HashSet<RowKey>();

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
            final LinkedHashMap<DataCell, Double> classCounts) {

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
    public LinkedHashMap<DataCell, Double> getClassCounts(final DataRow row,
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
