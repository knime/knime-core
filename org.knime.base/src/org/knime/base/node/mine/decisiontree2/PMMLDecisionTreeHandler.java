/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 18, 2008 (albrecht): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeLeaf;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitContinuous;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitNominal;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitNominalBinary;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * @author Christian Albrecht, University of Konstanz
 */
public class PMMLDecisionTreeHandler extends PMMLContentHandler {

    private DecisionTree m_tree = null;

    private DecisionTreeNode m_root = null;

    private StringBuffer m_buffer;

    private Stack<String> m_stack = new Stack<String>();

    private Stack<TempTreeNodeContainer> m_nodeStack =
            new Stack<TempTreeNodeContainer>();

    private Stack<DecisionTreeNode> m_childStack =
            new Stack<DecisionTreeNode>();

    private int m_level = 0;

    private String m_classColumn = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        // necessary for inside ArrayElement
        if (m_buffer == null) {
            m_buffer = new StringBuffer();
        }
        m_buffer.append(ch, start, length);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        m_tree = new DecisionTree(m_root, m_classColumn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {

        m_stack.pop();
        if (name.equals("Array") && m_stack.peek().equals("SimpleSetPredicate")) {
            String[] temp = m_buffer.toString().trim().split("\\s+");
            List<String> splitValues = new ArrayList<String>();
            for (String currentClass : temp) {
                splitValues.add(currentClass);
            }
            m_nodeStack.peek().addSplitValues(splitValues);
        }
        if (name.equals("Node")) {
            DecisionTreeNode newNode = null;

            // create new leaf node
            if (m_nodeStack.isEmpty()
                    || m_nodeStack.peek().getLevel() <= m_level) {
                int nodeId = m_nodeStack.peek().getOwnIndex();
                DataCell majorityClass = m_nodeStack.peek().getMajorityClass();
                HashMap<DataCell, Double> classCounts =
                        m_nodeStack.peek().getClassCounts();
                DecisionTreeNodeLeaf leafNode =
                        new DecisionTreeNodeLeaf(nodeId, majorityClass,
                                classCounts);
                newNode = leafNode;
            }

            //
            else {
                ArrayList<TempTreeNodeContainer> containerChildren =
                        new ArrayList<TempTreeNodeContainer>();
                while (m_nodeStack.peek().getLevel() > m_level) {
                    containerChildren.add(m_nodeStack.pop());
                }

                int nodeId = m_nodeStack.peek().getOwnIndex();
                DataCell majorityClass = m_nodeStack.peek().getMajorityClass();
                HashMap<DataCell, Double> classCounts =
                        m_nodeStack.peek().getClassCounts();

                String splitAttribute =
                        getSplitAttributeFromChildren(containerChildren);
                assert splitAttribute != null;

                DecisionTreeNode[] children =
                        new DecisionTreeNode[containerChildren.size()];
                for (int i = children.length - 1; i >= 0; i--) {
                    children[i] = m_childStack.pop();
                }

                ParentNodeType type =
                        findNodeTypeFromChildren(containerChildren);
                assert type != ParentNodeType.UNKNOWN;

                if (type == ParentNodeType.CONTINUOUS_SPLIT_NODE) {
                    assert containerChildren.size() == 2;

                    double threshold =
                            getThresholdFromChildren(containerChildren);
                    DecisionTreeNodeSplitContinuous splitNode =
                            new DecisionTreeNodeSplitContinuous(nodeId,
                                    majorityClass, classCounts, splitAttribute,
                                    children, threshold);
                    newNode = splitNode;
                } else if (type == ParentNodeType.NOMINAL_SPLIT_NODE_NORMAL) {
                    DataCell[] splitValues =
                            getSplitValuesFromChildren(containerChildren);
                    DecisionTreeNodeSplitNominal splitNode =
                            new DecisionTreeNodeSplitNominal(nodeId,
                                    majorityClass, classCounts, splitAttribute,
                                    splitValues, children);
                    newNode = splitNode;

                } else if (type == ParentNodeType.NOMINAL_SPLIT_NODE_BINARY) {
                    assert containerChildren.size() == 2;
                    DataCell[] splitValues =
                            getSplitValuesFromChildrenBinary(containerChildren);
                    int[][] splitMappings =
                            getSplitMappingsFromChildren(containerChildren);
                    int[] splitMappingsLeft = splitMappings[0];
                    int[] splitMappingsRight = splitMappings[1];
                    DecisionTreeNodeSplitNominalBinary splitNode =
                            new DecisionTreeNodeSplitNominalBinary(nodeId,
                                    majorityClass, classCounts, splitAttribute,
                                    splitValues, splitMappingsLeft,
                                    splitMappingsRight, children);
                    newNode = splitNode;
                }

            }

            m_level--;
            if (m_level == 0) {
                m_root = newNode;
            } else {
                m_childStack.push(newNode);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String name, final Attributes atts) throws SAXException {

        m_stack.push(name);
        if (name.equals("Node")) {
            int ownIndex = Integer.parseInt(atts.getValue("id"));
            String majorityClass = atts.getValue("score");
            double allClassFrequency =
                    Double.parseDouble(atts.getValue("recordCount"));
            m_nodeStack.push(new TempTreeNodeContainer(ownIndex, majorityClass,
                    allClassFrequency, ++m_level));
        } else if (name.equals("SimplePredicate")) {
            m_nodeStack.peek().addSplitAttribute(atts.getValue("field"));
            m_nodeStack.peek().addSplitValue(atts.getValue("value"));
        } else if (name.equals("SimpleSetPredicate")) {
            m_nodeStack.peek().addSplitAttribute(atts.getValue("field"));
        } else if (name.equals("ScoreDistribution")) {
            DataCell className = new StringCell(atts.getValue("value"));
            double value = Double.parseDouble(atts.getValue("recordCount"));
            m_nodeStack.peek().addClassCount(className, value);
        }
        else if (name.equals("MiningField")){
            String type = atts.getValue("usageType");
            if(type != null && type.equals("predicted")){
                m_classColumn = atts.getValue("name");
            }
        }

    }

    /**
     * @return decision tree object
     */
    public DecisionTree getDecisionTree() {
        return m_tree;
    }

    private ParentNodeType findNodeTypeFromChildren(
            final ArrayList<TempTreeNodeContainer> children) {
        ParentNodeType type = ParentNodeType.UNKNOWN;
        for (TempTreeNodeContainer c : children) {
            if (c.getParentNodeType() != ParentNodeType.UNKNOWN) {
                if (type != ParentNodeType.UNKNOWN
                        && type != c.getParentNodeType()) {
                    return ParentNodeType.UNKNOWN;
                } else {
                    type = c.getParentNodeType();
                }
            }
        }
        return type;

    }

    private String getSplitAttributeFromChildren(
            final ArrayList<TempTreeNodeContainer> children) {
        String result = null;
        for (TempTreeNodeContainer c : children) {
            if (result != null && !result.equals(c.getSplitAttribute())) {
                return null;
            } else {
                result = c.getSplitAttribute();
            }
        }
        return result;
    }

    private double getThresholdFromChildren(
            final ArrayList<TempTreeNodeContainer> children) {
        for (TempTreeNodeContainer c : children) {
            if (!c.getThreshold().isNaN()) {
                return c.getThreshold();
            }
        }
        return Double.NaN;
    }

    private DataCell[] getSplitValuesFromChildren(
            final ArrayList<TempTreeNodeContainer> children) {
        DataCell[] result = new DataCell[children.size()];
        for (int i = 0; i < result.length; i++) {
            TempTreeNodeContainer c = children.get(i);
            assert c.getSplitValue() != null;
            result[i] = new StringCell(c.getSplitValue());
        }
        return result;
    }

    private DataCell[] getSplitValuesFromChildrenBinary(
            final ArrayList<TempTreeNodeContainer> children) {
        List<String> leftSplitValues = children.get(0).getSplitValues();
        List<String> rightSplitValues = children.get(1).getSplitValues();
        DataCell[] result =
                new DataCell[leftSplitValues.size() + rightSplitValues.size()];
        for (int i = 0; i < leftSplitValues.size(); i++) {
            result[i] = new StringCell(leftSplitValues.get(i));
        }
        for (int i = 0; i < rightSplitValues.size(); i++) {
            result[i + leftSplitValues.size()] =
                    new StringCell(rightSplitValues.get(i));
        }
        return result;
    }

    private int[][] getSplitMappingsFromChildren(
            final ArrayList<TempTreeNodeContainer> children) {
        List<String> leftSplitValues = children.get(0).getSplitValues();
        List<String> rightSplitValues = children.get(1).getSplitValues();
        int[][] result = new int[2][];
        result[0] = new int[leftSplitValues.size()];
        result[1] = new int[rightSplitValues.size()];
        for (int i = 0; i < leftSplitValues.size(); i++) {
            result[0][i] = i;
        }
        for (int i = 0; i < rightSplitValues.size(); i++) {
            result[1][i] = i + leftSplitValues.size();
        }
        return result;
    }

}
