/*
 *
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
 * -------------------------------------------------------------------
 *
 * History
 *   06.08.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree2.model;

import java.awt.Color;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * Is replaced by the more general DecisionTreeNodeSplitPMML node.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
@Deprecated
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
            final LinkedHashMap<DataCell, Double> classCounts,
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
    public LinkedHashMap<DataCell, Double> getClassCounts(final DataCell cell,
            final DataRow row, final DataTableSpec spec) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeChildren(final Set<Integer> indices) {
        super.removeChildren(indices);
        DataCell[] attributes
                = new DataCell[m_splitValues.length - indices.size()];
        int pos = 0;
        for (int i = 0; i < m_splitValues.length; i++) {
            if (!indices.contains(i)) {
                attributes[pos++] = m_splitValues[i];
            }
        }
        m_splitValues = attributes;
    }
}
