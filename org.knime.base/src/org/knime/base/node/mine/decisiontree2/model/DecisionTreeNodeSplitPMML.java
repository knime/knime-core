/*
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 3, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2.model;

import java.awt.Color;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.knime.base.node.mine.decisiontree2.PMMLMissingValueStrategy;
import org.knime.base.node.mine.decisiontree2.PMMLNoTrueChildStrategy;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;


/**
 * Decision tree split node that supports PMML predicates to partition the data.
 * An arbitrary number of childs (>2) is allowed.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
@SuppressWarnings("serial")
public class DecisionTreeNodeSplitPMML extends DecisionTreeNodeSplit {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DecisionTreeNodeSplitPMML.class);

    private PMMLPredicate[] m_splitPred;

    private int m_defaultChild = -1;

    /**
     */
    public DecisionTreeNodeSplitPMML() {
        //
    }

    /**
     * Constructor of derived class. Read all type-specific information from XML
     * File.
     *
     * @param xmlNode XML node info
     * @param mapper map translating column names to DataCells and vice versa
     */
    // public DecisionTreeNodeSplitPMML(final Node xmlNode,
    // final DataCellStringMapper mapper) {
    // super(xmlNode, mapper); // let super read all type-invariant info
    // // now read information related to a split on a continuous attribute
    // Node splitNode = xmlNode.getChildNodes().item(3);
    // assert splitNode.getNodeName().equals("SPLIT");
    // String nrBranches =
    // splitNode.getAttributes().getNamedItem("branches")
    // .getNodeValue();
    // // make room for branches and split predicates
    // int nrSplits = Integer.parseInt(nrBranches);
    // assert (nrSplits >= 1);
    // super.makeRoomForKids(nrSplits);
    // m_splitPred = new PMMLPredicate[nrSplits];
    // // TODO add predicate creation
    //
    // NodeList splitKids = splitNode.getChildNodes();
    // for (int i = 0; i < splitKids.getLength(); i++) {
    // if (splitKids.item(i).getNodeName().equals("BRANCH")) {
    // Node branchNode = splitKids.item(i);
    // String id =
    // branchNode.getAttributes().getNamedItem("id")
    // .getNodeValue();
    // String nodeId =
    // branchNode.getAttributes().getNamedItem("nodeId")
    // .getNodeValue();
    // int pos = Integer.parseInt(id) - 1;
    // assert (pos >= 0 && pos < nrSplits);
    // super.setChildNodeIndex(pos, Integer.parseInt(nodeId));
    // }
    // }
    // }

    /**
     * Constructor of base class. The necessary data is provided directly in the
     * constructor.
     *
     * @param nodeId the id of this node
     * @param majorityClass the majority class of the records in this node
     * @param classCounts the class distribution of the data in this node
     * @param splitAttribute the attribute name on which to split
     * @param splitPredicates the split predicates used to partition the data
     * @param children the children split according to the split values
     */
    public DecisionTreeNodeSplitPMML(final int nodeId,
            final DataCell majorityClass,
            final LinkedHashMap<DataCell, Double> classCounts,
            final String splitAttribute, final PMMLPredicate[] splitPredicates,
            final DecisionTreeNode[] children) {
        super(nodeId, majorityClass, classCounts, splitAttribute);

        // make room for branches and split values
        int nrSplits = children.length;
        assert (nrSplits >= 1);
        super.makeRoomForKids(nrSplits);
        m_splitPred = splitPredicates;

        // make sure that we have one split predicate per child
        assert splitPredicates.length == children.length;

        for (int i = 0; i < children.length; i++) {
            super.setChildNodeIndex(i, children[i].getOwnIndex());
            addNode(children[i], i);
            children[i].setParent(this);
        }

        for (int i = 0; i < m_splitPred.length; i++) {
            if (super.getChildNodeAt(i) != null) {
                super.getChildNodeAt(i).setPrefix(m_splitPred[i].toString());
            }
        }
    }

    /**
     * Constructor of base class. The necessary data is provided directly in the
     * constructor.
     *
     * @param nodeId the id of this node
     * @param majorityClass the majority class of the records in this node
     * @param classCounts the class distribution of the data in this node
     * @param splitAttribute the attribute name on which to split
     * @param splitPredicates the split predicates used to partition the data
     * @param children the children split according to the split values
     * @param defaultChild index of the default child (only evaluated with
     *            {@link PMMLMissingValueStrategy} DEFAULT_CHILD
     */
    public DecisionTreeNodeSplitPMML(final int nodeId,
            final DataCell majorityClass,
            final LinkedHashMap<DataCell, Double> classCounts,
            final String splitAttribute, final PMMLPredicate[] splitPredicates,
            final DecisionTreeNode[] children, final int defaultChild) {
        this(nodeId, majorityClass, classCounts, splitAttribute,
                splitPredicates, children);
        m_defaultChild = defaultChild;
    }

    /**
     * @return the defaultChild
     */
    public DecisionTreeNode getDefaultChild() {
        if (m_defaultChild == -1) {
            throw new IllegalStateException("DefaultChild strategy specified, "
                    + "but no default child set for node with id "
                    + this.getOwnIndex());
        }
        return super.getChildNodeAt(m_defaultChild);
    }

    /**
     * @return the defaultChild index
     */
    public int getDefaultChildIndex() {
        return m_defaultChild;
    }



    /**
     * @param defaultChild the defaultChild to set
     */
    public void setDefaultChild(final int defaultChild) {
        m_defaultChild = defaultChild;
    }

    /* This method had to be overridden to pass missing values down to the
     * predicates. Otherwise the PMML strategies for missing value handling
     * could not be implemented. */
    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedHashMap<DataCell, Double> getClassCounts(final DataRow row,
            final DataTableSpec spec) {
        assert (spec != null);
        DecisionTreeNode matchingChild = getMatchingChild(row, spec);
        if (matchingChild == this) {
            return getNodeClassWeights();
        } else if (matchingChild != null) {
            return matchingChild.getClassCounts(row, spec);
        } else { // return null prediction strategy
            LOGGER.warn("Decision Tree Prediction failed."
                    + " Could not find branch for row '" + row.toString());
            // return empty map
            return new LinkedHashMap<DataCell, Double>();
        }
    }

    /**
     * Returns the first child which has a matching predicate. If there exists
     * no matching child and the strategy is last prediction the current node
     * is returned, otherwise null.
     *
     * @param row input pattern
     * @param spec the corresponding table spec
     * @return the matching child, null or the current node
     */
    private DecisionTreeNode getMatchingChild(final DataRow row,
            final DataTableSpec spec) {
        assert (spec != null);
        for (int i = 0; i < m_splitPred.length; i++) {
            Boolean result = m_splitPred[i].evaluate(row, spec);
            if (result != null && result) {
                return getChildNodeAt(i);
            } else if (result == null) {
                // Apply the missing value strategy
                switch (getMVStrategy()) {
                    case NONE:
                        /* missing counts as false
                        -> continue with next predicate */
                        break;
                    case DEFAULT_CHILD:
                        return getDefaultChild();
                    case LAST_PREDICTION:
                        return this;
                    default:
                        throw new UnsupportedOperationException(
                                "Missing value strategy '" + getMVStrategy()
                                + "' is not supported yet.");
                }
            }
        }
        /*
         * If we arrive here, no predicate evaluated to true and hence we could
         * not find a branch to continue with. Therefore the no true child
         * strategy must be considered.
         */
        if (getNTCStrategy()
                == PMMLNoTrueChildStrategy.RETURN_LAST_PREDICTION) {
            return this;
        } else { // return null prediction strategy
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedHashMap<DataCell, Double> getClassCounts(final DataCell cell,
            final DataRow row, final DataTableSpec spec) {
        return getClassCounts(null, row, spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCoveredPattern(final DataCell cell, final DataRow row,
            final DataTableSpec spec, final double weight) throws Exception {
        // first add pattern to the branch that matches the cell's value
        DecisionTreeNode matchingChild = getMatchingChild(row, spec);
        if (matchingChild != null && matchingChild != this) {
            matchingChild.addCoveredPattern(row, spec, weight);
        } else {
            LOGGER.debug("Decision Tree HiLiteAdder failed."
                    + " Could not find branch for value '" + cell.toString()
                    + "' for attribute '" + getSplitAttr() + "'."
                    + "Ignoring pattern.");
        }
        Color col = spec.getRowColor(row).getColor();
        addColorToMap(col, weight);
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCoveredColor(final DataCell cell, final DataRow row,
            final DataTableSpec spec, final double weight) throws Exception {
        DecisionTreeNode matchingChild = getMatchingChild(row, spec);
        if (matchingChild != null && matchingChild != this) {
            matchingChild.addCoveredColor(row, spec, weight);
            Color col = spec.getRowColor(row).getColor();
            addColorToMap(col, weight);
        } else {
            LOGGER.debug("Decision Tree HiLiteAdder failed."
                    + " Could not find branch for value '" + cell.toString()
                    + "' for attribute '" + getSplitAttr().toString() + "'."
                    + "Ignoring pattern.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RowKey> coveredPattern() {
        if (m_splitPred == null) {
            return null;
        }
        HashSet<RowKey> result =
                new HashSet<RowKey>(super.getChildNodeAt(0).coveredPattern());
        for (int i = 1; i < m_splitPred.length; i++) {
            result.addAll(super.getChildNodeAt(i).coveredPattern());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadNodeSplitInternalsFromPredParams(final ModelContentRO pConf)
            throws InvalidSettingsException {
    	m_splitPred = new PMMLPredicate[pConf.getInt("nrChildren")];
        for (int i = 0; i < m_splitPred.length; i++) {
            Config predConfig = pConf.getConfig("pred" + i);
            m_splitPred[i] = PMMLPredicate.getPredicateForConfig(predConfig);
            m_splitPred[i].loadFromPredParams(predConfig);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveNodeSplitInternalsToPredParams(final ModelContentWO pConf) {
        int i = 0;
        for (PMMLPredicate pred : m_splitPred) {
            Config predConfig = pConf.addConfig("pred" + i++);
            pred.saveToPredParams(predConfig);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringSummary() {
        StringBuffer sb =
                new StringBuffer("split attr. '" + getSplitAttr()
                        + "' on predicates: ");
        for (PMMLPredicate pred : m_splitPred) {
            sb.append(pred + ", ");
        }
        return sb.toString();
    }

    /**
     * Returns the split predicate array of this node.
     *
     * @return the split predicate array of this node
     */
    public PMMLPredicate[] getSplitPred() {
        return m_splitPred;
    }
}
