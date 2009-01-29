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
 *   06.08.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree2.model;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.knime.base.data.util.DataCellStringMapper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Michael Berthold, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class DecisionTreeNodeSplitNominal extends DecisionTreeNodeSplit {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DecisionTreeNodeSplitNominal.class);

    private DataCell[] m_splitValues = null;

    /**
     * Empty Constructor visible only within package.
     */
    DecisionTreeNodeSplitNominal() {
    }

    /**
     * Constructor of derived class. Read all type-specific information from XML
     * File.
     *
     * @param xmlNode XML node info
     * @param mapper map translating column names to DataCells and vice versa
     */
    public DecisionTreeNodeSplitNominal(final Node xmlNode,
            final DataCellStringMapper mapper) {
        super(xmlNode, mapper); // let super read all type-invariant info
        // now read information related to a split on a continuous attribute
        Node splitNode = xmlNode.getChildNodes().item(3);
        assert splitNode.getNodeName().equals("SPLIT");
        String nrBranches =
                splitNode.getAttributes().getNamedItem("branches")
                        .getNodeValue();

        // make room for branches and split values
        int nrSplits = Integer.parseInt(nrBranches);
        assert (nrSplits >= 1);
        super.makeRoomForKids(nrSplits);
        m_splitValues = new DataCell[nrSplits];

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
                String val =
                        branchNode.getAttributes().getNamedItem("val")
                                .getNodeValue();
                int pos = Integer.parseInt(id) - 1;
                assert (pos >= 0 && pos < nrSplits);
                super.setChildNodeIndex(pos, Integer.parseInt(nodeId));
                m_splitValues[pos] = mapper.stringToDataCell(val);
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
     * @param splitValues the split values used to partition the data
     * @param children the children split according to the split values
     */
    public DecisionTreeNodeSplitNominal(final int nodeId,
            final DataCell majorityClass,
            final HashMap<DataCell, Double> classCounts,
            final String splitAttribute, final DataCell[] splitValues,
            final DecisionTreeNode[] children) {
        super(nodeId, majorityClass, classCounts, splitAttribute);

        // make room for branches and split values
        int nrSplits = children.length;
        assert (nrSplits >= 1);
        super.makeRoomForKids(nrSplits);
        m_splitValues = splitValues;

        // if the number of split values is not equal to the number of
        // children, this is a non-standart-nominal split, (extended)
        // thus do not continue as the sub class performs this
        if (splitValues.length == children.length) {

            for (int i = 0; i < children.length; i++) {
                super.setChildNodeIndex(i, children[i].getOwnIndex());
                addNode(children[i], i);
                children[i].setParent(this);
            }

            for (int i = 0; i < m_splitValues.length; i++) {
                if (super.getChildNodeAt(i) != null) {
                    super.getChildNodeAt(i).setPrefix(
                            getSplitAttr() + " = " + m_splitValues[i]);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HashMap<DataCell, Double> getClassCounts(final DataCell cell,
            final DataRow row, final DataTableSpec spec) throws Exception {
        for (int i = 0; i < m_splitValues.length; i++) {
            if (m_splitValues[i].equals(cell)) {
                return super.getChildNodeAt(i).getClassCounts(row, spec);
            }
        }
        LOGGER.error("Decision Tree Prediction failed."
                + " Could not find branch for value '" + cell.toString()
                + "' for attribute '" + getSplitAttr().toString() + "'."
                + "Return Missing instead.");
        return this.getClassCounts();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCoveredPattern(final DataCell cell, final DataRow row,
            final DataTableSpec spec, final double weight) throws Exception {
        // first add pattern to the branch that matches the cell's value
        boolean notFound = true;
        for (int i = 0; i < m_splitValues.length; i++) {
            if (m_splitValues[i].equals(cell)) {
                super.getChildNodeAt(i).addCoveredPattern(row, spec, weight);
                notFound = false;
                break;
            }
        }
        if (notFound) {
            LOGGER.error("Decision Tree HiLiteAdder failed."
                    + " Could not find branch for value '" + cell.toString()
                    + "' for attribute '" + getSplitAttr().toString() + "'."
                    + "Ignoring pattern.");
        }
        Color col = spec.getRowColor(row).getColor();
        addColorToMap(col, weight);
        return;
    }

    /**
     * Add colors for a pattern given as a row of values. This is a leaf so we
     * will simply add the color to our list.
     *
     * @param cell the cell to be used for the split at this level
     * @param row input pattern
     * @param spec the corresponding table spec
     * @param weight the weight of the row (between 0.0 and 1.0)
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    @Override
    public void addCoveredColor(final DataCell cell, final DataRow row,
            final DataTableSpec spec, final double weight) throws Exception {
        for (int i = 0; i < m_splitValues.length; i++) {
            if (m_splitValues[i].equals(cell)) {
                super.getChildNodeAt(i).addCoveredColor(row, spec, weight);
                Color col = spec.getRowColor(row).getColor();
                addColorToMap(col, weight);
                return;
            }
        }
        LOGGER.error("Decision Tree HiLiteAdder failed."
                + " Could not find branch for value '" + cell.toString()
                + "' for attribute '" + getSplitAttr().toString() + "'."
                + "Ignoring pattern.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RowKey> coveredPattern() {
        if (m_splitValues == null) {
            return null;
        }
        HashSet<RowKey> result =
                new HashSet<RowKey>(super.getChildNodeAt(0).coveredPattern());
        for (int i = 1; i < m_splitValues.length; i++) {
            result.addAll(super.getChildNodeAt(i).coveredPattern());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringSummary() {
        return "split nominal attr. '" + getSplitAttr();
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
        for (int i = 0; i < m_splitValues.length; i++) {
            if (super.getChildNodeAt(i) != null) {
                super.getChildNodeAt(i).setPrefix(
                        getSplitAttr() + " = " + m_splitValues[i]);
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveNodeSplitInternalsToPredParams(final ModelContentWO pConf) {
        pConf.addDataCellArray("splitValues", m_splitValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadNodeSplitInternalsFromPredParams(final ModelContentRO pConf)
            throws InvalidSettingsException {
        m_splitValues = pConf.getDataCellArray("splitValues");
    }

    /**
     * Returns the values array of this nodes split attribute.
     *
     * @return the values array of this nodes split attribute
     */
    public DataCell[] getSplitValues() {
        return m_splitValues;
    }
}
