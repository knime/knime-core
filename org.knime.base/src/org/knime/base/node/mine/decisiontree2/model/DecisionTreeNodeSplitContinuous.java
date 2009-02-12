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
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.knime.base.data.util.DataCellStringMapper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Michael Berthold, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class DecisionTreeNodeSplitContinuous extends DecisionTreeNodeSplit {
    /** The node logger for this class. */
    // private static final NodeLogger LOGGER =
    // NodeLogger.getLogger(DecisionTreeNodeSplitContinuous.class);
    private double m_threshold = 0.0;

    /**
     * Empty Constructor visible only within package.
     */
    DecisionTreeNodeSplitContinuous() {
    }

    /**
     * Constructor of derived class. Read all type-specific information from XML
     * File.
     *
     * @param xmlNode XML node info
     * @param mapper map translating column names to DataCells and vice versa
     */
    public DecisionTreeNodeSplitContinuous(final Node xmlNode,
            final DataCellStringMapper mapper) {
        super(xmlNode, mapper); // let super read all type-invariant info
        super.makeRoomForKids(2);
        // now read information related to a split on a continuous attribute
        Node splitNode = xmlNode.getChildNodes().item(3);
        assert splitNode.getNodeName().equals("SPLIT");
        String nrBranches =
                splitNode.getAttributes().getNamedItem("branches")
                        .getNodeValue();
        assert (nrBranches.equals("2"));
        NodeList splitKids = splitNode.getChildNodes();
        for (int i = 0; i < splitKids.getLength(); i++) {
            if (splitKids.item(i).getNodeName().equals("BRANCH")) {
                Node branchNode = splitKids.item(i);
                String id =
                        branchNode.getAttributes().getNamedItem("id")
                                .getNodeValue();
                String nodeId =
                        branchNode.getAttributes().getNamedItem("nodeId")
                                .getNodeValue();
                String cond =
                        branchNode.getAttributes().getNamedItem("cond")
                                .getNodeValue();
                if (id.equals("1")) {
                    super.setChildNodeIndex(0, Integer.parseInt(nodeId));
                    // branch no. 0 (left) should always tbe the "<=" one
                    assert cond.equals("leq");
                }
                if (id.equals("2")) {
                    super.setChildNodeIndex(1, Integer.parseInt(nodeId));
                    // branch no. 1 (right) should always tbe the ">" one
                    assert cond.equals("gt");
                }
            }
            if (splitKids.item(i).getNodeName().equals("CONTINUOUS")) {
                Node contNode = splitKids.item(i);
                String cut =
                        contNode.getAttributes().getNamedItem("Cut")
                                .getNodeValue();
                String lower =
                        contNode.getAttributes().getNamedItem("Lower")
                                .getNodeValue();
                String upper =
                        contNode.getAttributes().getNamedItem("Upper")
                                .getNodeValue();
                assert cut.equals(lower);
                assert cut.equals(upper);
                m_threshold = Double.parseDouble(cut);
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
     * @param children the children of this decission tree node
     * @param splitThreshold the split point of the given split attribute that
     *            partitions the data of this node
     */
    public DecisionTreeNodeSplitContinuous(final int nodeId,
            final DataCell majorityClass,
            final HashMap<DataCell, Double> classCounts,
            final String splitAttribute, final DecisionTreeNode[] children,
            final double splitThreshold) {

        super(nodeId, majorityClass, classCounts, splitAttribute);
        super.makeRoomForKids(2);

        assert children.length == 2;
        // branch no. 0 (left) should always tbe the "<=" one
        super.setChildNodeIndex(0, children[0].getOwnIndex());
        addNode(children[0], 0);
        children[0].setParent(this);

        // branch no. 1 (right) should always tbe the ">" one
        super.setChildNodeIndex(1, children[1].getOwnIndex());
        addNode(children[1], 1);
        children[1].setParent(this);

        m_threshold = splitThreshold;
        NumberFormat nf = NumberFormat.getInstance();
        if (super.getChildNodeAt(0) != null) {
            super.getChildNodeAt(0).setPrefix(
                    getSplitAttr() + " <= " + nf.format(m_threshold));
        }
        if (super.getChildNodeAt(1) != null) {
            super.getChildNodeAt(1).setPrefix(
                    getSplitAttr() + " > " + nf.format(m_threshold));
        }
    }

    /**
     * Determine class counts for a new pattern given as a row of values.
     * Returns a HashMap listing counts for all classes. For the continuous
     * split we need to analyse the attribute for this split and then ask the
     * left resp. right subtree for it's prediction. Whoever calls us was nice
     * enough to already pick out the DataCell used for this split so we do not
     * need to find it. It is also guaranteed that it is not missing and of the
     * right type.
     *
     * @param cell the cell to be used for the split at this level
     * @param row input pattern
     * @param spec the corresponding table spec
     * @return HashMap class/count
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    @Override
    public HashMap<DataCell, Double> getClassCounts(final DataCell cell,
            final DataRow row, final DataTableSpec spec) throws Exception {
        assert cell.getType().isCompatible(DoubleValue.class);
        double value = ((DoubleValue)cell).getDoubleValue();
        if (value <= m_threshold) {
            return super.getChildNodeAt(0).getClassCounts(row, spec);
        }
        return super.getChildNodeAt(1).getClassCounts(row, spec);
    }

    /**
     * Add patterns given as a row of values if they fall within a specific
     * node. This node simply forwards this request to the appropriate child.
     *
     * @param cell the cell to be used for the split at this level
     * @param row input pattern
     * @param spec the corresponding table spec
     * @param weight the weight of the row (between 0.0 and 1.0)
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    @Override
    public void addCoveredPattern(final DataCell cell, final DataRow row,
            final DataTableSpec spec, final double weight) throws Exception {
        double value = ((DoubleValue)cell).getDoubleValue();
        if (value <= m_threshold) {
            super.getChildNodeAt(0).addCoveredPattern(row, spec, weight);
        } else {
            super.getChildNodeAt(1).addCoveredPattern(row, spec, weight);
        }
        Color col = spec.getRowColor(row).getColor();
        addColorToMap(col, weight);
    }

    /**
     * Add colors for a pattern given as a row of values. This is a leaf so we
     * will simply add the color to our list.
     *
     * @param cell the cell to be used for the split at this level
     * @param row input pattern
     * @param spec the corresponding table spec
     * @param weight the weight of the row  (between 0.0 and 1.0)
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    @Override
    public void addCoveredColor(final DataCell cell, final DataRow row,
            final DataTableSpec spec, final double weight) throws Exception {
        double value = ((DoubleValue)cell).getDoubleValue();
        if (value <= m_threshold) {
            super.getChildNodeAt(0).addCoveredColor(row, spec, weight);
        } else {
            super.getChildNodeAt(1).addCoveredColor(row, spec, weight);
        }
        Color col = spec.getRowColor(row).getColor();
        addColorToMap(col, weight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RowKey> coveredPattern() {
        Set<RowKey> resultL = null;
        Set<RowKey> resultR = null;
        if (super.getChildNodeAt(0) != null) {
            resultL = super.getChildNodeAt(0).coveredPattern();
        }
        if (super.getChildNodeAt(1) != null) {
            resultR = super.getChildNodeAt(1).coveredPattern();
        }
        if (resultR == null) {
            return resultL;
        }
        if (resultL == null) {
            return resultR;
        }
        HashSet<RowKey> result = new HashSet<RowKey>(resultL);
        result.addAll(resultR);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringSummary() {
        return "split attr. '" + getSplitAttr() + "' at " + m_threshold
                + " (<=,>)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addNodeToTreeDepthFirst(final DecisionTreeNode node,
            final int ix) {
        if (!super.addNodeToTreeDepthFirst(node, ix)) {
            return false;
        }
        NumberFormat nf = NumberFormat.getInstance();
        if (super.getChildNodeAt(0) != null) {
            super.getChildNodeAt(0).setPrefix(
                    getSplitAttr() + " <= " + nf.format(m_threshold));
        }
        if (super.getChildNodeAt(1) != null) {
            super.getChildNodeAt(1).setPrefix(
                    getSplitAttr() + " > " + nf.format(m_threshold));
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveNodeSplitInternalsToPredParams(final ModelContentWO pConf) {
        pConf.addDouble("threshold", m_threshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadNodeSplitInternalsFromPredParams(final ModelContentRO pConf)
            throws InvalidSettingsException {
        m_threshold = pConf.getDouble("threshold");
    }

    /**
     * Returns the split threashold of this continous split.
     *
     * @return the split threashold of this continous split
     */
    public double getThreshold() {
        return m_threshold;
    }

}
