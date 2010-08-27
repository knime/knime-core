/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   Aug 18, 2008 (albrecht): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLModelType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Christian Albrecht, University of Konstanz
 */
public class PMMLDecisionTreePortObject extends PMMLPortObject implements
        PortObject {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(PMMLDecisionTreePortObject.class);

    /**
     *
     */
    public static final PortType TYPE =
            new PortType(PMMLDecisionTreePortObject.class);

    private DecisionTree m_tree;

    /**
     * @param tree underlying decision tree
     * @param spec pmml mining schema of the tree
     */
    public PMMLDecisionTreePortObject(final DecisionTree tree,
            final PMMLPortObjectSpec spec) {
        super(spec, PMMLModelType.TreeModel);
        m_tree = tree;
    }

    /**
     *
     */
    public PMMLDecisionTreePortObject() {
        // empty constructor for load from method
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writePMMLModel(final TransformerHandler handler)
            throws SAXException {

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

        PMMLPortObjectSpec.writeMiningSchema(getSpec(), handler,
                getWriteVersion());
        writeLocalTransformations(handler);

        addTreeNode(handler, m_tree.getRootNode());
        handler.endElement(null, null, "TreeModel");
    }

    /**
     * @return true if the tree contains at least one non binary split
     */
    private boolean treeIsMultisplit(final DecisionTreeNode node) {
        if (node instanceof DecisionTreeNodeLeaf) {
            return false;
        }
        if ((node instanceof DecisionTreeNodeSplitContinuous)
            || (node instanceof DecisionTreeNodeSplitNominalBinary)) {
            boolean leftSide =
                    treeIsMultisplit((DecisionTreeNode)node.getChildAt(0));
            boolean rightSide =
                    treeIsMultisplit((DecisionTreeNode)node.getChildAt(1));
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
                boolean first = treeIsMultisplit((DecisionTreeNode)
                        node.getChildAt(0));
                boolean second = treeIsMultisplit((DecisionTreeNode)
                        node.getChildAt(1));
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
    private void addTreeNode(final TransformerHandler handler,
            final DecisionTreeNode node) throws SAXException {

        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "id", CDATA,
                ((Integer)node.getOwnIndex()).toString());
        atts.addAttribute(null, null, "score", CDATA, node.getMajorityClass()
                .toString());
        atts.addAttribute(null, null, "recordCount", CDATA, ((Double)node
                .getEntireClassCount()).toString());
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
        DecisionTreeNode parent = (DecisionTreeNode)node.getParent();
        if (parent == null) {
            handler.startElement(null, null, "True", null);
            handler.endElement(null, null, "True");
        } else if (parent instanceof DecisionTreeNodeSplitContinuous) {
            DecisionTreeNodeSplitContinuous splitNode =
                    (DecisionTreeNodeSplitContinuous)parent;

            if (splitNode.getIndex(node) == 0) {
                AttributesImpl predAtts = new AttributesImpl();
                predAtts.addAttribute(null, null, "field", CDATA, splitNode
                        .getSplitAttr());
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
            setPredAtts.addAttribute(null, null, "field", CDATA, splitNode
                    .getSplitAttr());
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
            } else if (splitNode.getIndex(node)
                    == SplitNominalBinary.RIGHT_PARTITION) {
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
            predAtts.addAttribute(null, null, "field", CDATA, splitNode
                    .getSplitAttr());
            predAtts.addAttribute(null, null, "operator", CDATA, "equal");
            int nodeIndex = parent.getIndex(node);
            predAtts.addAttribute(null, null, "value", CDATA, splitNode
                    .getSplitValues()[nodeIndex].toString());
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
            distrAtts.addAttribute(null, null, "recordCount", CDATA, freq
                    .toString());
            handler.startElement(null, null, "ScoreDistribution", distrAtts);
            handler.endElement(null, null, "ScoreDistribution");
        }

        // adding children
        if (!(node instanceof DecisionTreeNodeLeaf)) {
            for (int i = 0; i < node.getChildCount(); i++) {
                addTreeNode(handler, (DecisionTreeNode)node.getChildAt(i));
            }
        }

        handler.endElement(null, null, "Node");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFrom(final PMMLPortObjectSpec spec,
            final InputStream stream, final String version)
            throws ParserConfigurationException, SAXException, IOException {
        /* TODO pass spec to PMMLDecisionTreeHandler (usage of parsed
         * Mining Schema information */
        PMMLDecisionTreeHandler hdl = new PMMLDecisionTreeHandler();
        super.addPMMLContentHandler("TreeModel", hdl);
        super.loadFrom(spec, stream, version);
        hdl = (PMMLDecisionTreeHandler)super.getPMMLContentHandler("TreeModel");
        m_tree = hdl.getDecisionTree();
        LOGGER.info("Loaded tree port object");

    }

    /**
     * @return loaded classification tree object
     */
    public DecisionTree getTree() {
        return m_tree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return "PMML Decision Tree Port with " + m_tree.getNumberNodes()
                + " nodes";
    }
}
