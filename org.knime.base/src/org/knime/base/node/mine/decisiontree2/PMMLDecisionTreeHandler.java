/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   Sep 3, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.knime.base.node.io.filetokenizer.FileTokenizer;
import org.knime.base.node.io.filetokenizer.FileTokenizerSettings;
import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeLeaf;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitPMML;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class PMMLDecisionTreeHandler extends PMMLContentHandler {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PMMLDecisionTreeHandler.class);

    private static final Set<String> UNSUPPORTED = new LinkedHashSet<String>();

    private static final Set<String> IGNORED = new LinkedHashSet<String>();

    private static final Set<String> KNOWN = new LinkedHashSet<String>();

    private static final Set<String> SUPPORTED_DATA_TYPES =
            new LinkedHashSet<String>();

    static {
        // TODO verify and extend list of unsupported and ignored elements
        // for PMML 4.0
        UNSUPPORTED.add("LocalTransformations");
        UNSUPPORTED.add("Partition");
        UNSUPPORTED.add("EmbeddedModel");

        IGNORED.add("Extension");
        IGNORED.add("Output");
        IGNORED.add("OutputField");
        IGNORED.add("ModelStats");
        IGNORED.add("ModelExplanation");
        IGNORED.add("Targets");
        IGNORED.add("ModelVerification");
        IGNORED.add("Interval");
        IGNORED.add("Value");

        KNOWN.add("PMML");
        KNOWN.add("Header");
        KNOWN.add("Application");
        KNOWN.add("DataDictionary");
        // KNOWN.add("DataField");
        KNOWN.add("MiningSchema");

        SUPPORTED_DATA_TYPES.add("string");
        SUPPORTED_DATA_TYPES.add("integer");
        SUPPORTED_DATA_TYPES.add("float");
        SUPPORTED_DATA_TYPES.add("double");
    }

    private Stack<PMMLCompoundPredicate> m_predStack =
            new Stack<PMMLCompoundPredicate>();
    private Stack<TempTreeNodeContainer> m_nodeStack
            = new Stack<TempTreeNodeContainer>();
    private Stack<String> m_elementStack = new Stack<String>();
    private int m_level = 0;
    private StringBuffer m_buffer;

    private String m_classColumn;
    private PMMLMissingValueStrategy m_mvStrategy;
    private PMMLNoTrueChildStrategy m_ntcStrategy;

    private DecisionTree m_tree;
    private Stack<DecisionTreeNode> m_childStack =
            new Stack<DecisionTreeNode>();

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
    public void startElement(final String uri, final String localName,
            final String name, final Attributes atts) throws SAXException {
        if (UNSUPPORTED.contains(name)) {
            throw new IllegalArgumentException("Element " + name
                    + " is not supported!");
        } else if (IGNORED.contains(name)) {
            LOGGER.info("Element " + name + " is ignored.");
        } else if (name.equals("Node")) {
            int ownIndex = Integer.parseInt(atts.getValue("id"));
            String majorityClass = atts.getValue("score");
            String defaultChild = atts.getValue("defaultChild");
            double allClassFrequency =
                    Double.parseDouble(atts.getValue("recordCount"));
            m_nodeStack.push(new TempTreeNodeContainer(ownIndex, majorityClass,
                    allClassFrequency, ++m_level, defaultChild));
        } else if (name.endsWith("Predicate") || name.equals("True")
                || name.equals("False")) {
            // predicate handling
            PMMLPredicate pred = null;
            if (name.equals("SimplePredicate")) {
                pred =
                        new PMMLSimplePredicate(atts.getValue("field"), atts
                                .getValue("operator"), atts.getValue("value"));
            } else if (name.equals("True")) {
                pred = new PMMLTruePredicate();
            } else if (name.equals("False")) {
                pred = new PMMLFalsePredicate();
            } else if (name.equals("CompoundPredicate")) {
                pred = new PMMLCompoundPredicate(atts
                                .getValue("booleanOperator"));
            } else if (name.equals("SimpleSetPredicate")) {
                pred =
                        new PMMLSimpleSetPredicate(atts.getValue("field"), atts
                                .getValue("booleanOperator"));
            }
            // determine if it is a sub predicate of a compound predicate
            if (!m_elementStack.peek().equals("CompoundPredicate")) {
                // add predicate to parent node
                m_nodeStack.peek().setPredicate(pred);
            } else {
                // add to compound predicate on top of stack
                m_predStack.peek().addPredicate(pred);
            }
            if (pred instanceof PMMLCompoundPredicate) {
                m_predStack.add((PMMLCompoundPredicate)pred);
            }

        } else if (name.equals("ScoreDistribution")) {
            DataCell className = new StringCell(atts.getValue("value"));
            double value = Double.parseDouble(atts.getValue("recordCount"));
            m_nodeStack.peek().addClassCount(className, value);
        } else if (name.equals("MiningField")) {
            String type = atts.getValue("usageType");
            if (type != null && type.equals("predicted")) {
                // the class column should only be set once
                if (m_classColumn != null) {
                    throw new IllegalArgumentException("Multiple predicted "
                            + "fields in mining schema found.");
                }
                m_classColumn = atts.getValue("name");
            }
        } else if (name.equals("DataField")) {
            String dataType = atts.getValue("dataType");
            if (!SUPPORTED_DATA_TYPES.contains(dataType)) {
                LOGGER.warn("Unsupported data type " + dataType);
            }
        } else if (name.equals("Array")
                && m_elementStack.peek().equals("SimpleSetPredicate")) {
            getPreviousSimpleSetPredicate().setArrayType(atts.getValue("type"));
        } else if (name.equals("TreeModel")) {
           m_mvStrategy = PMMLMissingValueStrategy.get(
                   atts.getValue("missingValueStrategy"));
           m_ntcStrategy = PMMLNoTrueChildStrategy.get(
                   atts.getValue("noTrueChildStrategy"));
        } else if (!KNOWN.contains(name)) {
            LOGGER.warn("Skipping unknown element " + name);
        }
        m_elementStack.push(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {
        m_elementStack.pop();
        if (name.equals("Array")
                && m_elementStack.peek().equals("SimpleSetPredicate")) {
            // remove optional quotes and split on whitespace
            FileTokenizer tokenizer = new FileTokenizer(new StringReader(
                    m_buffer.toString().trim()));
            //create settings for the tokenizer
            FileTokenizerSettings settings = new FileTokenizerSettings();
            settings.addDelimiterPattern(" ",
                    /* combine multiple= */true,
                    /* return as token= */ false,
                    /* include in token= */false);
            settings.addQuotePattern("\"", "\"", '\\');
            settings.addWhiteSpaceCharacter(' ');
            settings.addWhiteSpaceCharacter('\t');
            settings.addWhiteSpaceCharacter('\n');
            tokenizer.setSettings(settings);

            List<String> splitValues = new ArrayList<String>();
            String token = null;
            while ((token = tokenizer.nextToken()) != null) {
                splitValues.add(token);
            }
            m_buffer.setLength(0);
            getPreviousSimpleSetPredicate().setValues(splitValues);
        } else if (name.equals("CompoundPredicate")) {
            // remove from predicate stack
            m_predStack.pop();
        } else if (name.equals("Node")) {
            // when a node has completed the predicate stack should always be
            // empty
            assert m_predStack.isEmpty();

            DecisionTreeNode newNode = null;
            TempTreeNodeContainer topNode = m_nodeStack.peek();
            if (m_level >= topNode.getLevel()) { // leaf node
                int nodeId = topNode.getOwnIndex();
                DataCell majorityClass = topNode.getMajorityClass();
                LinkedHashMap<DataCell, Double> classCounts =
                        topNode.getClassCounts();
                newNode =
                        new DecisionTreeNodeLeaf(nodeId, majorityClass,
                                classCounts);
            } else { // split node
                // collect all children of the current node from the child stack
                ArrayList<TempTreeNodeContainer> containerChildren
                        = new ArrayList<TempTreeNodeContainer>();
                ArrayList<DecisionTreeNode> childrenList =
                        new ArrayList<DecisionTreeNode>();
                ArrayList<PMMLPredicate> predicateList =
                        new ArrayList<PMMLPredicate>();

                while (m_level < m_nodeStack.peek().getLevel()) {
                    TempTreeNodeContainer top = m_nodeStack.pop();
                    if (top.getLevel() > m_level + 1) {
                        assert false : "Level count inconsistent";
                    }
                    // add new elements to start of both lists (at index 0)!
                    containerChildren.add(0, top);
                    childrenList.add(0, m_childStack.pop());
                    predicateList.add(0, top.getPredicate());
                }

                DecisionTreeNode[] children =
                        childrenList.toArray(new DecisionTreeNode[childrenList
                                .size()]);
                PMMLPredicate[] splitPredicates =
                        predicateList.toArray(new PMMLPredicate[predicateList
                                .size()]);

                TempTreeNodeContainer currentParent = m_nodeStack.peek();
                // collect all necessary information for the current split node
                int nodeId = currentParent.getOwnIndex();
                DataCell majorityClass = currentParent.getMajorityClass();
                LinkedHashMap<DataCell, Double> classCounts =
                        currentParent.getClassCounts();
                int defaultChild = currentParent.getDefaultChild();
                String splitAttribute =
                        getChildSplitAttribute(containerChildren);

                newNode =
                        new DecisionTreeNodeSplitPMML(nodeId, majorityClass,
                                classCounts, splitAttribute, splitPredicates,
                                children, defaultChild);
            }
            m_level--;
            m_childStack.push(newNode);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        // we should have only one node left - the root node
        assert m_childStack.size() == 1;
        if (m_classColumn == null) {
            throw new IllegalArgumentException("No predicted "
                    + "field in mining schema found.");
        }
        m_tree = new DecisionTree(m_childStack.pop(), m_classColumn,
                m_mvStrategy, m_ntcStrategy);
        LOGGER.info("Decision tree with missing value strateg: '"
                + m_mvStrategy + "' and no true child strategy: '"
                + m_ntcStrategy + "' created.");
    }

    /**
     * @return decision tree object
     */
    public DecisionTree getDecisionTree() {
        return m_tree;
    }

    /**
     * Returns the name of the split attribute of the children, or an empty
     * String if the split works on multiple attributes.
     *
     * @param containerChildren the children
     *
     * @return the name of the split attribute or ""
     */
    public String getChildSplitAttribute(
            final ArrayList<TempTreeNodeContainer> containerChildren) {
        String splitAttribute =
                containerChildren.get(0).getPredicate().getSplitAttribute();
        for (TempTreeNodeContainer child : containerChildren) {
            String current = child.getPredicate().getSplitAttribute();
            if (current == null) {
                continue;
            } else if (!current.equals(splitAttribute)) {
                return "";
            }
        }
        return splitAttribute;
    }

    /**
     * Get the most recent SimpleSetPredicate. This can be found on the node
     * stack, or predicate stack if it is part of a compound predicate.
     *
     * @return the previous SimpleSetPredicate
     */
    private PMMLSimpleSetPredicate getPreviousSimpleSetPredicate() {
        if (m_predStack.empty()) {
            return (PMMLSimpleSetPredicate)m_nodeStack.peek().getPredicate();
        } else {
            return (PMMLSimpleSetPredicate)m_predStack.peek()
                    .getLastPredicate();
        }
    }

}
