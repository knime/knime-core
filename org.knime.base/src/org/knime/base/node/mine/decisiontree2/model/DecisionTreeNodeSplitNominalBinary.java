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
 *   20.03.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.knime.base.data.util.DataCellStringMapper;
import org.knime.base.node.mine.decisiontree2.learner.SplitNominalBinary;
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
 *
 * Is replaced by the more general DecisionTreeNodeSplitPMML node.
 *
 * Represents a nominal split node that splits subsets of values in a binary
 * manner.
 *
 * @author Christoph Sieb, University of Konstanz
 */
@Deprecated
public class DecisionTreeNodeSplitNominalBinary extends
        DecisionTreeNodeSplitNominal {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DecisionTreeNodeSplitNominalBinary.class);

    private List<Integer> m_leftChildIndices = new ArrayList<Integer>();

    private List<Integer> m_rightChildIndices = new ArrayList<Integer>();

    private static int[] toIntArray(final List<Integer> intList) {
        int[] result = new int[intList.size()];
        int counter = 0;
        for (Integer integer : intList) {
            result[counter] = integer;
            counter++;
        }
        return result;
    }

    private static ArrayList<Integer> toArrayList(final int[] intList) {

        ArrayList<Integer> result = new ArrayList<Integer>();
        int counter = 0;
        for (int integer : intList) {
            result.add(integer);
            counter++;
        }
        return result;
    }

    /**
     * Empty Constructor visible only within package.
     */
    DecisionTreeNodeSplitNominalBinary() {
    }

    /**
     * Constructor of derived class. Read all type-specific information from XML
     * File.
     *
     * @param xmlNode XML node info
     * @param mapper map translating column names to DataCells and vice versa
     */
    public DecisionTreeNodeSplitNominalBinary(final Node xmlNode,
            final DataCellStringMapper mapper) {
        super(xmlNode, mapper); // let super read all type-invariant info

        // NOTE: this constructor is not needed yet, as the changed
        // c4.5 code with xml output does not have binary splits yet
    }

    /**
     * Constructor for a nominal split in binary format. The necessary data is
     * provided directly in the constructor.
     *
     * @param nodeId the id of this node
     * @param majorityClass the majority class of the records in this node
     * @param classCounts the class distribution of the data in this node
     * @param splitAttribute the attribute name on which to split
     * @param splitValues all nominal split values in the order of their integer
     *            mapping
     * @param splitMappingsLeft the integer mapping values for the nominal
     *            values that fall into the left partition
     * @param splitMappingsRight the integer mapping values for the nominal
     *            values that fall into the right partition
     * @param children the children split according to the split values
     */
    public DecisionTreeNodeSplitNominalBinary(final int nodeId,
            final DataCell majorityClass,
            final LinkedHashMap<DataCell, Double> classCounts,
            final String splitAttribute, final DataCell[] splitValues,
            final int[] splitMappingsLeft, final int[] splitMappingsRight,
            final DecisionTreeNode[] children) {
        super(nodeId, majorityClass, classCounts, splitAttribute, splitValues,
                children);

        assert 2 == children.length;
        // make room for branches and split values
        int nrSplits = children.length;
        assert (nrSplits >= 1);
        super.makeRoomForKids(nrSplits);

        // set the left child (at index 0 - no need to force "left" or "right"
        // index consistency here since the child knows which values to check
        // for. But the partitioning and mapping relies on proper indexing!
        DecisionTreeNode leftChild =
            children[SplitNominalBinary.LEFT_PARTITION];
        super.setChildNodeIndex(0, leftChild.getOwnIndex());
        addNode(leftChild, 0);
        leftChild.setParent(this);
        // and its indices
        m_leftChildIndices = new ArrayList<Integer>();
        for (int mapping : splitMappingsLeft) {
            m_leftChildIndices.add(mapping);
        }
        // and its prefix
        super.getChildNodeAt(0).setPrefix(
                      getSplitAttr()
                    + " = "
                    + getNominalValueString(splitValues, splitMappingsLeft));

        // set the right child - at index 1, see above.
        DecisionTreeNode rightChild =
            children[SplitNominalBinary.RIGHT_PARTITION];
        super.setChildNodeIndex(1, rightChild.getOwnIndex());
        addNode(rightChild, 1);
        rightChild.setParent(this);
        // and its indices
        m_rightChildIndices = new ArrayList<Integer>();
        for (int mapping : splitMappingsRight) {
            m_rightChildIndices.add(mapping);
        }
        // and its prefix
        super.getChildNodeAt(1).setPrefix(
                      getSplitAttr()
                    + " = "
                    + getNominalValueString(splitValues, splitMappingsRight));

    }

    /**
     * Creates a string of the nominal values for the prefix string. The input
     * are all possible split values and the integer mappings (the split values
     * must be in the correct order corresponding to the mappings)
     *
     * @param splitValues all nominal split values
     * @param splitMappings the integer mappings of the values that should be
     *            included in the string
     *
     * @return the string of the wished split values separated by comma
     */
    private String getNominalValueString(final DataCell[] splitValues,
            final int[] splitMappings) {
        StringBuilder sb = new StringBuilder();
        for (int mapping : splitMappings) {
            sb.append(splitValues[mapping]);
            sb.append(", ");
        }
        // remove the last comma
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedHashMap<DataCell, Double> getClassCounts(final DataCell cell,
            final DataRow row, final DataTableSpec spec) throws Exception {
        int childIndex = getIndexOfChild(cell);
        if (childIndex >= 0) {
            return super.getChildNodeAt(childIndex).getClassCounts(row, spec);
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
        // first add pattern to the branch that contains the cell's value
        boolean notFound = true;
        int childIndex = getIndexOfChild(cell);

        if (childIndex >= 0) {
            super.getChildNodeAt(childIndex).addCoveredPattern(row, spec,
                    weight);
            notFound = false;
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
     * Add colors for a pattern given as a row of values.
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

        int childIndex = getIndexOfChild(cell);
        if (childIndex < 0) {
            LOGGER.error("Decision Tree HiLiteAdder failed."
                    + " Could not find branch for value '" + cell.toString()
                    + "' for attribute '" + getSplitAttr().toString() + "'."
                    + "Ignoring pattern.");
            return;
        }
        super.getChildNodeAt(childIndex).addCoveredColor(row, spec, weight);
        Color col = spec.getRowColor(row).getColor();
        addColorToMap(col, weight);
        return;

    }

    private int getIndexOfChild(final DataCell value) {
        for (int i = 0; i < getSplitValues().length; i++) {
            if (getSplitValues()[i].equals(value)) {
                if (m_leftChildIndices.contains(i)) {
                    return 0;
                } else if (m_rightChildIndices.contains(i)) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RowKey> coveredPattern() {
        if (getSplitValues() == null) {
            return null;
        }
        HashSet<RowKey> result =
                new HashSet<RowKey>(super.getChildNodeAt(0).coveredPattern());
        result.addAll(super.getChildNodeAt(1).coveredPattern());

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

        if (super.getChildNodeAt(0) != null) {
            super.getChildNodeAt(0).setPrefix(
                    getSplitAttr() + " in "
                            + getConcatenatedValues(m_leftChildIndices));
        }

        if (super.getChildNodeAt(1) != null) {
            super.getChildNodeAt(1).setPrefix(
                    getSplitAttr() + " in "
                            + getConcatenatedValues(m_rightChildIndices));
        }

        return true;
    }

    private String getConcatenatedValues(final List<Integer> childIndices) {
        StringBuilder sb = new StringBuilder();
        for (int index : childIndices) {
            sb.append(getSplitValues()[index]).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveNodeSplitInternalsToPredParams(final ModelContentWO pConf) {
        super.saveNodeSplitInternalsToPredParams(pConf);
        pConf.addIntArray("childIndices0", toIntArray(m_leftChildIndices));
        pConf.addIntArray("childIndices1", toIntArray(m_rightChildIndices));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadNodeSplitInternalsFromPredParams(final ModelContentRO pConf)
            throws InvalidSettingsException {
        super.loadNodeSplitInternalsFromPredParams(pConf);
        m_leftChildIndices = toArrayList(pConf.getIntArray("childIndices0"));
        m_rightChildIndices = toArrayList(pConf.getIntArray("childIndices1"));
    }

    /**
     * @return indices of patterns that fall into child node 0 (left).
     */
    public List<Integer> getLeftChildIndices() {
        return m_leftChildIndices;
    }

    /**
     * @return indices of patterns that fall into child node 1 (right).
     */
    public List<Integer> getRightChildIndices() {
        return m_rightChildIndices;
    }
}
