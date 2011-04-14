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
package org.knime.base.node.mine.decisiontree2;

import static org.knime.core.node.port.pmml.PMMLPortObject.CDATA;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.node.mine.decisiontree2.learner.SplitNominalBinary;
import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeLeaf;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitContinuous;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitNominal;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitNominalBinary;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplitPMML;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.util.tokenizer.Tokenizer;
import org.knime.core.util.tokenizer.TokenizerSettings;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class PMMLDecisionTreeHandler extends PMMLContentHandler {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(PMMLDecisionTreeHandler.class);

    private static final Set<String> UNSUPPORTED = new LinkedHashSet<String>();

    private static final Set<String> IGNORED = new LinkedHashSet<String>();

    private static final Set<String> KNOWN = new LinkedHashSet<String>();

    private static final Set<String> SUPPORTED_DATA_TYPES =
            new LinkedHashSet<String>();

    static {
        // TODO verify and extend list of unsupported and ignored elements
        // for PMML 4.0
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

    private final Stack<PMMLCompoundPredicate> m_predStack =
            new Stack<PMMLCompoundPredicate>();

    private final Stack<TempTreeNodeContainer> m_nodeStack =
            new Stack<TempTreeNodeContainer>();

    private final Stack<String> m_elementStack = new Stack<String>();

    private int m_level = 0;

    private StringBuffer m_buffer;

    private String m_classColumn;

    private PMMLMissingValueStrategy m_mvStrategy;

    private PMMLNoTrueChildStrategy m_ntcStrategy;

    private DecisionTree m_tree;

    private final Stack<DecisionTreeNode> m_childStack =
            new Stack<DecisionTreeNode>();



    /**
     * Creates a new empty decision tree handler. The initialization has to
     * be performed by registering the handler to a parser.
     */
    public PMMLDecisionTreeHandler() {
        super();
    }

    /**
     * Creates an initialized decision tree handler that can be used to
     * output the decision tree model by invoking
     * {@link #addPMMLModel(org.w3c.dom.DocumentFragment, PMMLPortObjectSpec)}.
     * @param tree the decision tree
     */
    public PMMLDecisionTreeHandler(final DecisionTree tree) {
        m_tree = tree;
    }




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
                        new PMMLSimplePredicate(atts.getValue("field"),
                                atts.getValue("operator"),
                                atts.getValue("value"));
            } else if (name.equals("True")) {
                pred = new PMMLTruePredicate();
            } else if (name.equals("False")) {
                pred = new PMMLFalsePredicate();
            } else if (name.equals("CompoundPredicate")) {
                pred =
                        new PMMLCompoundPredicate(
                                atts.getValue("booleanOperator"));
            } else if (name.equals("SimpleSetPredicate")) {
                pred =
                        new PMMLSimpleSetPredicate(atts.getValue("field"),
                                atts.getValue("booleanOperator"));
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
            m_mvStrategy =
                    PMMLMissingValueStrategy.get(atts
                            .getValue("missingValueStrategy"));
            m_ntcStrategy =
                    PMMLNoTrueChildStrategy.get(atts
                            .getValue("noTrueChildStrategy"));
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
            Tokenizer tokenizer =
                    new Tokenizer(new StringReader(m_buffer.toString().trim()));
            // create settings for the tokenizer
            TokenizerSettings settings = new TokenizerSettings();
            settings.addDelimiterPattern(" ",
            /* combine multiple= */true,
            /* return as token= */false,
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
                ArrayList<TempTreeNodeContainer> containerChildren =
                        new ArrayList<TempTreeNodeContainer>();
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
        m_tree =
                new DecisionTree(m_childStack.pop(), m_classColumn,
                        m_mvStrategy, m_ntcStrategy);
        LOGGER.info("Decision tree with missing value strategy: '"
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getSupportedVersions() {
        TreeSet<String> versions = new TreeSet<String>();
        versions.add(PMMLPortObject.PMML_V3_0);
        versions.add(PMMLPortObject.PMML_V3_1);
        versions.add(PMMLPortObject.PMML_V3_2);
        versions.add(PMMLPortObject.PMML_V4_0);
        return versions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addPMMLModelContent(final TransformerHandler handler,
            final PMMLPortObjectSpec spec)
            throws SAXException {
        if (m_tree == null) {
            String msg =
                    "DecisionTreeHandler has not been initialized "
                            + "correctly. No model content is added.";
            LOGGER.warn(msg);
            return;
        }
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "modelName", CDATA, "DecisionTree");
        atts.addAttribute(null, null, "functionName", CDATA, "classification");
        String splitCharacteristic;
        if (treeIsMultisplit(m_tree.getRootNode())) {
            splitCharacteristic = "multiSplit";
        } else {
            splitCharacteristic = "binarySplit";
        }
        atts.addAttribute(null, null, "splitCharacteristic", CDATA,
                splitCharacteristic);

        PMMLMissingValueStrategy mvStrategy = m_tree.getMVStrategy();
        if (mvStrategy != null
                && mvStrategy != PMMLMissingValueStrategy.getDefault()) {
            atts.addAttribute(null, null, "missingValueStrategy", CDATA,
                    mvStrategy.toString());
        }
        PMMLNoTrueChildStrategy ntcStrategy = m_tree.getNTCStrategy();
        if (ntcStrategy != null
                && ntcStrategy != PMMLNoTrueChildStrategy.getDefault()) {
            atts.addAttribute(null, null, "noTrueChildStrategy", CDATA,
                    ntcStrategy.toString());
        }

        handler.startElement(null, null, "TreeModel", atts);
        PMMLPortObjectSpec.writeMiningSchema(spec, handler);

        //adding empty local transformations that can be filled later
        handler.startElement(null, null, "LocalTransformations", null);
        handler.endElement(null, null, "LocalTransformations");

        addTreeNode(handler, m_tree.getRootNode());
        handler.endElement(null, null, "TreeModel");
    }

    /**
     * @return true if the tree contains at least one non binary split
     */
    private static boolean treeIsMultisplit(final DecisionTreeNode node) {
        if (node instanceof DecisionTreeNodeLeaf) {
            return false;
        }
        if ((node instanceof DecisionTreeNodeSplitContinuous)
                || (node instanceof DecisionTreeNodeSplitNominalBinary)) {
            boolean leftSide = treeIsMultisplit(node.getChildAt(0));
            boolean rightSide = treeIsMultisplit(node.getChildAt(1));
            return (leftSide || rightSide);
        }
        if (node instanceof DecisionTreeNodeSplitNominal) {
            return true;
        }
        if (node instanceof DecisionTreeNodeSplitPMML) {
            int childCount = node.getChildCount();
            if (childCount > 2) {
                return true;
            } else {
                boolean first = treeIsMultisplit(node.getChildAt(0));
                boolean second = treeIsMultisplit(node.getChildAt(1));
                return (first || second);
            }
        }
        // and we should never reach this point
        assert false;
        return false;
    }

    /**
     * @param handler
     * @param node
     * @throws SAXException
     */
    private static void addTreeNode(final TransformerHandler handler,
            final DecisionTreeNode node) throws SAXException {

        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "id", CDATA,
                ((Integer)node.getOwnIndex()).toString());
        atts.addAttribute(null, null, "score", CDATA, node.getMajorityClass()
                .toString());
        atts.addAttribute(null, null, "recordCount", CDATA,
                ((Double)node.getEntireClassCount()).toString());
        if (node instanceof DecisionTreeNodeSplitPMML) {
            int defaultChild =
                    ((DecisionTreeNodeSplitPMML)node).getDefaultChildIndex();
            if (defaultChild > -1) {
                atts.addAttribute(null, null, "defaultChild", CDATA,
                        String.valueOf(defaultChild));
            }
        }
        handler.startElement(null, null, "Node", atts);

        // adding score and stuff from parent
        DecisionTreeNode parent = node.getParent();
        if (parent == null) {
            handler.startElement(null, null, "True", null);
            handler.endElement(null, null, "True");
        } else if (parent instanceof DecisionTreeNodeSplitContinuous) {
            DecisionTreeNodeSplitContinuous splitNode =
                    (DecisionTreeNodeSplitContinuous)parent;

            if (splitNode.getIndex(node) == 0) {
                AttributesImpl predAtts = new AttributesImpl();
                predAtts.addAttribute(null, null, "field", CDATA,
                        splitNode.getSplitAttr());
                predAtts.addAttribute(null, null, "operator", CDATA,
                        "lessOrEqual");
                predAtts.addAttribute(null, null, "value", CDATA,
                        ((Double)splitNode.getThreshold()).toString());
                handler.startElement(null, null, "SimplePredicate", predAtts);
                handler.endElement(null, null, "SimplePredicate");
            } else if (splitNode.getIndex(node) == 1) {
                handler.startElement(null, null, "True", null);
                handler.endElement(null, null, "True");
            }

        } else if (parent instanceof DecisionTreeNodeSplitNominalBinary) {
            DecisionTreeNodeSplitNominalBinary splitNode =
                    (DecisionTreeNodeSplitNominalBinary)parent;
            AttributesImpl setPredAtts = new AttributesImpl();
            setPredAtts.addAttribute(null, null, "field", CDATA,
                    splitNode.getSplitAttr());
            setPredAtts.addAttribute(null, null, "booleanOperator", CDATA,
                    "isIn");
            handler.startElement(null, null, "SimpleSetPredicate", setPredAtts);
            AttributesImpl arrayAtts = new AttributesImpl();
            arrayAtts.addAttribute(null, null, "type", CDATA, "string");
            handler.startElement(null, null, "Array", arrayAtts);
            DataCell[] splitValues = splitNode.getSplitValues();
            List<Integer> indices = null;
            if (splitNode.getIndex(node) == SplitNominalBinary.LEFT_PARTITION) {
                indices = splitNode.getLeftChildIndices();
            } else if (splitNode.getIndex(node) == SplitNominalBinary.RIGHT_PARTITION) {
                indices = splitNode.getRightChildIndices();
            }
            StringBuilder classSet = new StringBuilder();
            for (Integer i : indices) {
                if (classSet.length() > 0) {
                    classSet.append(" ");
                }
                classSet.append(splitValues[i].toString());
            }
            handler.characters(classSet.toString().toCharArray(), 0,
                    classSet.length());
            handler.endElement(null, null, "Array");
            handler.endElement(null, null, "SimpleSetPredicate");

        } else if (parent instanceof DecisionTreeNodeSplitNominal) {
            DecisionTreeNodeSplitNominal splitNode =
                    (DecisionTreeNodeSplitNominal)parent;
            AttributesImpl predAtts = new AttributesImpl();
            predAtts.addAttribute(null, null, "field", CDATA,
                    splitNode.getSplitAttr());
            predAtts.addAttribute(null, null, "operator", CDATA, "equal");
            int nodeIndex = parent.getIndex(node);
            predAtts.addAttribute(null, null, "value", CDATA,
                    splitNode.getSplitValues()[nodeIndex].toString());
            handler.startElement(null, null, "SimplePredicate", predAtts);
            handler.endElement(null, null, "SimplePredicate");
        } else if (parent instanceof DecisionTreeNodeSplitPMML) {
            DecisionTreeNodeSplitPMML splitNode =
                    (DecisionTreeNodeSplitPMML)parent;
            int nodeIndex = parent.getIndex(node);
            // get the PMML predicate of the current node from its parent
            PMMLPredicate predicate = splitNode.getSplitPred()[nodeIndex];
            // delegate the writing to the predicate
            predicate.writePMML(handler);
        } else {
            LOGGER.error("Node Type " + parent.getClass()
                    + " is not supported!");
        }

        // adding score distribution (class counts)
        Set<Entry<DataCell, Double>> classCounts =
                node.getClassCounts().entrySet();
        Iterator<Entry<DataCell, Double>> iterator = classCounts.iterator();
        while (iterator.hasNext()) {
            Entry<DataCell, Double> entry = iterator.next();
            DataCell cell = entry.getKey();
            Double freq = entry.getValue();
            AttributesImpl distrAtts = new AttributesImpl();
            distrAtts.addAttribute(null, null, "value", CDATA, cell.toString());
            distrAtts.addAttribute(null, null, "recordCount", CDATA,
                    freq.toString());
            handler.startElement(null, null, "ScoreDistribution", distrAtts);
            handler.endElement(null, null, "ScoreDistribution");
        }

        // adding children
        if (!(node instanceof DecisionTreeNodeLeaf)) {
            for (int i = 0; i < node.getChildCount(); i++) {
                addTreeNode(handler, node.getChildAt(i));
            }
        }

        handler.endElement(null, null, "Node");
    }

}
