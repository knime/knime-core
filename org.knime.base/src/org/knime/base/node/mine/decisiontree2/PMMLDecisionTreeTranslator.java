/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.decisiontree2;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.dmg.pmml.ArrayType;
import org.dmg.pmml.FIELDUSAGETYPE;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.MININGFUNCTION.Enum;
import org.dmg.pmml.MISSINGVALUESTRATEGY;
import org.dmg.pmml.MiningFieldDocument.MiningField;
import org.dmg.pmml.NOTRUECHILDSTRATEGY;
import org.dmg.pmml.NodeDocument;
import org.dmg.pmml.NodeDocument.Node;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.ScoreDistributionDocument.ScoreDistribution;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate.Operator;
import org.dmg.pmml.SimpleSetPredicateDocument.SimpleSetPredicate;
import org.dmg.pmml.TreeModelDocument;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.dmg.pmml.TreeModelDocument.TreeModel.SplitCharacteristic;
import org.knime.base.node.mine.decisiontree2.learner2.SplitNominalBinary;
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
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.node.util.CheckUtils;

/**
 * A DecisionTree translator class between KNIME and PMML.
 *
 * @author wenlin, Zementis Inc., Apr 2011
 *
 */
public class PMMLDecisionTreeTranslator extends PMMLConditionTranslator implements PMMLTranslator {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(PMMLDecisionTreeTranslator.class);

    private static final Map<PMMLMissingValueStrategy,
            MISSINGVALUESTRATEGY.Enum> MV_STRATEGY_TO_PMML_MAP;

    private static final Map<MISSINGVALUESTRATEGY.Enum,
    PMMLMissingValueStrategy> MV_STRATEGY_TO_KNIME_MAP;

    static {
        MV_STRATEGY_TO_PMML_MAP =
            new HashMap<PMMLMissingValueStrategy, MISSINGVALUESTRATEGY.Enum>();
        MV_STRATEGY_TO_PMML_MAP.put(
                PMMLMissingValueStrategy.AGGREGATE_NODES,
                MISSINGVALUESTRATEGY.AGGREGATE_NODES);
        MV_STRATEGY_TO_PMML_MAP.put(
                PMMLMissingValueStrategy.DEFAULT_CHILD,
                MISSINGVALUESTRATEGY.DEFAULT_CHILD);
        MV_STRATEGY_TO_PMML_MAP.put(
                PMMLMissingValueStrategy.LAST_PREDICTION,
                MISSINGVALUESTRATEGY.LAST_PREDICTION);
        MV_STRATEGY_TO_PMML_MAP.put(PMMLMissingValueStrategy.NONE,
                MISSINGVALUESTRATEGY.NONE);
        MV_STRATEGY_TO_PMML_MAP.put(
                PMMLMissingValueStrategy.NULL_PREDICTION,
                MISSINGVALUESTRATEGY.NULL_PREDICTION);
        MV_STRATEGY_TO_PMML_MAP.put(
                PMMLMissingValueStrategy.WEIGHTED_CONFIDENCE,
                MISSINGVALUESTRATEGY.WEIGHTED_CONFIDENCE);

        MV_STRATEGY_TO_KNIME_MAP =
            new HashMap<MISSINGVALUESTRATEGY.Enum, PMMLMissingValueStrategy>();
        MV_STRATEGY_TO_KNIME_MAP.put(
                MISSINGVALUESTRATEGY.AGGREGATE_NODES,
                PMMLMissingValueStrategy.AGGREGATE_NODES);
        MV_STRATEGY_TO_KNIME_MAP.put(MISSINGVALUESTRATEGY.DEFAULT_CHILD,
                PMMLMissingValueStrategy.DEFAULT_CHILD);
        MV_STRATEGY_TO_KNIME_MAP.put(
                MISSINGVALUESTRATEGY.LAST_PREDICTION,
                PMMLMissingValueStrategy.LAST_PREDICTION);
        MV_STRATEGY_TO_KNIME_MAP.put(MISSINGVALUESTRATEGY.NONE,
                PMMLMissingValueStrategy.NONE);
        MV_STRATEGY_TO_KNIME_MAP.put(
                MISSINGVALUESTRATEGY.NULL_PREDICTION,
                PMMLMissingValueStrategy.NULL_PREDICTION);
        MV_STRATEGY_TO_KNIME_MAP.put(
                MISSINGVALUESTRATEGY.WEIGHTED_CONFIDENCE,
                PMMLMissingValueStrategy.WEIGHTED_CONFIDENCE);
    }
    private DecisionTree m_tree;

    /**
     * Only used when a regression tree model is read in a pmml model appender node.
     * This is because in pmml there is no real difference between regression and classification
     * trees other than the kind of function they perform (regression or classification).
     */
    private boolean m_isClassification;

    /**
     * Creates a new decision tree translator initialized with the decision
     * tree. For usage with the
     * {@link #exportTo(PMMLDocument, PMMLPortObjectSpec)} method.
     *
     * @param tree the KNIME decision tree
     */
    public PMMLDecisionTreeTranslator(final DecisionTree tree) {
        this();
        m_tree = tree;
        m_isClassification = true;
    }

    /**
     * For usage with the {@link #initializeFrom(PMMLDocument)} method.
     */
    public PMMLDecisionTreeTranslator() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc,
            final PMMLPortObjectSpec spec) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);
        PMML pmml = pmmlDoc.getPMML();
        TreeModelDocument.TreeModel treeModel = pmml.addNewTreeModel();

        PMMLMiningSchemaTranslator.writeMiningSchema(spec, treeModel);
        treeModel.setModelName("DecisionTree");
        if (m_isClassification) {
            treeModel.setFunctionName(MININGFUNCTION.CLASSIFICATION);
        } else {
            treeModel.setFunctionName(MININGFUNCTION.REGRESSION);
        }

        // ----------------------------------------------
        // set up splitCharacteristic
        if (treeIsMultisplit(m_tree.getRootNode())) {
            treeModel.setSplitCharacteristic(SplitCharacteristic.MULTI_SPLIT);
        } else {
            treeModel.setSplitCharacteristic(SplitCharacteristic.BINARY_SPLIT);
        }

        // ----------------------------------------------
        // set up missing value strategy
        PMMLMissingValueStrategy mvStrategy =
            m_tree.getMVStrategy() != null ? m_tree.getMVStrategy()
                : PMMLMissingValueStrategy.NONE;
        treeModel.setMissingValueStrategy(MV_STRATEGY_TO_PMML_MAP
                .get(mvStrategy));

        // -------------------------------------------------
        // set up no true child strategy
        PMMLNoTrueChildStrategy ntcStrategy = m_tree.getNTCStrategy();
        if (PMMLNoTrueChildStrategy.RETURN_LAST_PREDICTION.equals(
                ntcStrategy)) {
            treeModel.setNoTrueChildStrategy(
                    NOTRUECHILDSTRATEGY.RETURN_LAST_PREDICTION);
        } else if (PMMLNoTrueChildStrategy.RETURN_NULL_PREDICTION
                .equals(ntcStrategy)) {
            treeModel.setNoTrueChildStrategy(
                    NOTRUECHILDSTRATEGY.RETURN_NULL_PREDICTION);
        }

        // --------------------------------------------------
        // set up tree node
        NodeDocument.Node rootNode = treeModel.addNewNode();
        addTreeNode(rootNode, m_tree.getRootNode(),
                new DerivedFieldMapper(pmmlDoc));
        return TreeModel.type;
    }

    /**
     * A recursive function which converts each KNIME Tree node to a
     * corresponding PMML element.
     *
     * @param pmmlNode the desired PMML element
     * @param node A KNIME DecisionTree node
     */
    private static void addTreeNode(final NodeDocument.Node pmmlNode,
            final DecisionTreeNode node,
            final DerivedFieldMapper mapper) {
        pmmlNode.setId(String.valueOf(node.getOwnIndex()));
        pmmlNode.setScore(node.getMajorityClass().toString());
        // don't add recordCount if it is zero
        // this is e.g. the case if a simple regression tree is
        // read in and then exported again
        if (node.getEntireClassCount() > 0) {
            pmmlNode.setRecordCount(node.getEntireClassCount());
        }

        if (node instanceof DecisionTreeNodeSplitPMML) {
            int defaultChild =
                ((DecisionTreeNodeSplitPMML) node).getDefaultChildIndex();
            if (defaultChild > -1) {
                pmmlNode.setDefaultChild(String.valueOf(defaultChild));
            }
        }

        // adding score and stuff from parent
        DecisionTreeNode parent = node.getParent();
        if (parent == null) {
            // When the parent is null, it is the root Node.
            // For root node, the predicate is always True.
            pmmlNode.addNewTrue();
        } else if (parent instanceof DecisionTreeNodeSplitContinuous) {
            // SimplePredicate case
			DecisionTreeNodeSplitContinuous splitNode
			        = (DecisionTreeNodeSplitContinuous) parent;
            if (splitNode.getIndex(node) == 0) {
				SimplePredicate pmmlSimplePredicate = pmmlNode
						.addNewSimplePredicate();
				pmmlSimplePredicate.setField(mapper.getDerivedFieldName(
                        splitNode.getSplitAttr()));
                pmmlSimplePredicate.setOperator(Operator.LESS_OR_EQUAL);
                pmmlSimplePredicate.setValue(String.valueOf(splitNode
                        .getThreshold()));
            } else if (splitNode.getIndex(node) == 1) {
                pmmlNode.addNewTrue();
            }
        } else if (parent instanceof DecisionTreeNodeSplitNominalBinary) {
            // SimpleSetPredicate case
			DecisionTreeNodeSplitNominalBinary splitNode
			        = (DecisionTreeNodeSplitNominalBinary) parent;
			SimpleSetPredicate pmmlSimpleSetPredicate = pmmlNode
					.addNewSimpleSetPredicate();
            pmmlSimpleSetPredicate.setField(mapper
                    .getDerivedFieldName(splitNode.getSplitAttr()));
            pmmlSimpleSetPredicate.setBooleanOperator(
                    SimpleSetPredicate.BooleanOperator.IS_IN);
            ArrayType pmmlArray = pmmlSimpleSetPredicate.addNewArray();
            pmmlArray.setType(ArrayType.Type.STRING);
            DataCell[] splitValues = splitNode.getSplitValues();
            List<Integer> indices = null;
            if (splitNode.getIndex(node) == SplitNominalBinary.LEFT_PARTITION) {
                indices = splitNode.getLeftChildIndices();
            } else if (splitNode.getIndex(node)
                    == SplitNominalBinary.RIGHT_PARTITION) {
                indices = splitNode.getRightChildIndices();
            } else {
                throw new IllegalArgumentException("Split node is neither "
                        + "contained in the right nor in the left partition.");
            }
            StringBuilder classSet = new StringBuilder();
            for (Integer i : indices) {
                if (classSet.length() > 0) {
                    classSet.append(" ");
                }
                classSet.append(splitValues[i].toString());
            }
            pmmlArray.setN(BigInteger.valueOf(indices.size()));
            XmlCursor xmlCursor = pmmlArray.newCursor();
            xmlCursor.setTextValue(classSet.toString());
            xmlCursor.dispose();
        } else if (parent instanceof DecisionTreeNodeSplitNominal) {
            DecisionTreeNodeSplitNominal splitNode =
                    (DecisionTreeNodeSplitNominal)parent;
            SimplePredicate pmmlSimplePredicate =
                    pmmlNode.addNewSimplePredicate();
            pmmlSimplePredicate.setField(mapper.getDerivedFieldName(
                    splitNode.getSplitAttr()));
            pmmlSimplePredicate.setOperator(Operator.EQUAL);
            int nodeIndex = parent.getIndex(node);
            pmmlSimplePredicate.setValue(String.valueOf(splitNode
                    .getSplitValues()[nodeIndex].toString()));
		} else if (parent instanceof DecisionTreeNodeSplitPMML) {
			DecisionTreeNodeSplitPMML splitNode
			        = (DecisionTreeNodeSplitPMML) parent;
            int nodeIndex = parent.getIndex(node);
            // get the PMML predicate of the current node from its parent
            PMMLPredicate predicate = splitNode.getSplitPred()[nodeIndex];
            predicate.setSplitAttribute(mapper.getDerivedFieldName(
                    predicate.getSplitAttribute()));
            // delegate the writing to the predicate translator
            PMMLPredicateTranslator.exportTo(predicate, pmmlNode);
        } else {
            throw new IllegalArgumentException("Node Type " + parent.getClass() + " is not supported!");
        }

        // adding score distribution (class counts)
		Set<Entry<DataCell, Double>> classCounts = node.getClassCounts()
				.entrySet();
        Iterator<Entry<DataCell, Double>> iterator = classCounts.iterator();
        while (iterator.hasNext()) {
            Entry<DataCell, Double> entry = iterator.next();
            DataCell cell = entry.getKey();
            Double freq = entry.getValue();
			ScoreDistribution pmmlScoreDist = pmmlNode
					.addNewScoreDistribution();
            pmmlScoreDist.setValue(cell.toString());
            pmmlScoreDist.setRecordCount(freq);
        }

        // adding children
        if (!(node instanceof DecisionTreeNodeLeaf)) {
            for (int i = 0; i < node.getChildCount(); i++) {
                addTreeNode(pmmlNode.addNewNode(), node.getChildAt(i), mapper);
            }
        }
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
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);
        TreeModel[] models = pmmlDoc.getPMML().getTreeModelArray();
        if (models.length == 0) {
            throw new IllegalArgumentException("No treemodel provided.");
        }
        TreeModel treeModel = models[0];

        m_tree = parseDecTreeFromModel(treeModel);
    }

    /**
     * Builds a decision tree object out of the TreeModel.
     * @param treeModel treeModel parsed from the PMML.
     *
     * @return DecisionTreeModel for further processing.
     */
    public DecisionTree parseDecTreeFromModel(final TreeModel treeModel) {
        // --------------------------------------------
        // check the mining function, only classification is allowed

        final Enum functionName = CheckUtils.checkArgumentNotNull(treeModel.getFunctionName(), "Function name must not be null");
        if (MININGFUNCTION.CLASSIFICATION.equals(functionName)) {
            m_isClassification = true;
        } else if (MININGFUNCTION.REGRESSION.equals(functionName)) {
            m_isClassification = false;
        } else {
            throw new IllegalArgumentException("Unsupported function name \"" + functionName + "\"");
        }

        // --------------------------------------------
        // Find the predicted field from the mining schema
        MiningField[] miningFields =
            treeModel.getMiningSchema().getMiningFieldArray();
        String predictedField = "predictedField";
        for (MiningField mf : miningFields) {
            if (FIELDUSAGETYPE.PREDICTED == mf.getUsageType() || FIELDUSAGETYPE.TARGET == mf.getUsageType()) {
                predictedField = mf.getName();
                break;
            }
        }

        // ------------------------------------------------
        // Parse PMML nodes to KNIME nodes
        Node pmmlRoot = treeModel.getNode();
        DecisionTreeNode knimeRoot = addKnimeTreeNode(pmmlRoot);

        // ------------------------------------------------
        // parse no true child strategy
        PMMLNoTrueChildStrategy ntcStrategy =
            PMMLNoTrueChildStrategy.RETURN_NULL_PREDICTION;
        if (NOTRUECHILDSTRATEGY.RETURN_LAST_PREDICTION.equals(treeModel
                .getNoTrueChildStrategy())) {
            ntcStrategy = PMMLNoTrueChildStrategy.RETURN_LAST_PREDICTION;
        }

        // -------------------------------------------------
        // initialize a KNIME decision tree
        return new DecisionTree(knimeRoot, predictedField,
                    MV_STRATEGY_TO_KNIME_MAP.get(treeModel
                            .getMissingValueStrategy()), ntcStrategy);
    }

    /**
     * @return the decision tree stored by this translator
     */
    public DecisionTree getDecisionTree() {
        if (!m_isClassification) {
            throw new IllegalStateException("The provided PMML Tree Model is not a classification tree.");
        }
        return m_tree;
    }

    private DecisionTreeNode addKnimeTreeNode(final Node pmmlNode) {
        Node[] pmmlChildrenNode = pmmlNode.getNodeArray();

        // TODO Handle the case that the id from PMML might not be an integer.
        String nodeId = pmmlNode.getId();
        int id;
        try {
            id = Integer.parseInt(nodeId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Only numeric node ids are supported in KNIME. Found \""
                    + nodeId + "\".");
        }
        if (pmmlChildrenNode.length == 0) {
            DecisionTreeNodeLeaf knimeLeaf =
                    new DecisionTreeNodeLeaf(id, getMajorityClass(pmmlNode),
                            getClassCount(pmmlNode));
            return knimeLeaf;
        } else {
            PMMLPredicate[] pmmlPredicates =
                    new PMMLPredicate[pmmlChildrenNode.length];
            DecisionTreeNode[] children =
                    new DecisionTreeNode[pmmlChildrenNode.length];
            for (int i = 0; i < pmmlChildrenNode.length; i++) {
                children[i] = addKnimeTreeNode(pmmlChildrenNode[i]);
                pmmlPredicates[i] = getPredicate(pmmlChildrenNode[i]);
            }

            DecisionTreeNodeSplitPMML knimeNode;

            if (pmmlNode.isSetDefaultChild()) {
                String defaultChild = pmmlNode.getDefaultChild();
                Integer knimeDefaultChildIndex;
                try {
                    knimeDefaultChildIndex = Integer.parseInt(defaultChild);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Only numeric node ids are supported in KNIME. "
                            + "Found \"" + defaultChild
                            + "\" as defaultChild.");
                }
                knimeNode =
                        new DecisionTreeNodeSplitPMML(id,
                                getMajorityClass(pmmlNode),
                                getClassCount(pmmlNode),
                                getChildrenSplitAttribute(pmmlNode),
                                pmmlPredicates,
                                children, knimeDefaultChildIndex);
            } else {
                knimeNode =
                    new DecisionTreeNodeSplitPMML(id,
                            getMajorityClass(pmmlNode),
                            getClassCount(pmmlNode),
                            getChildrenSplitAttribute(pmmlNode), pmmlPredicates,
                            children);
            }
            return knimeNode;
        }

    }

    private PMMLPredicate getPredicate(final Node node) {
        PMMLPredicate predicate = null;
        if (node.getTrue() != null) {
            predicate = new PMMLTruePredicate();
        } else if (node.getFalse() != null) {
            predicate = new PMMLFalsePredicate();
        } else if (node.getSimplePredicate() != null) {
            predicate = parseSimplePredicate(node.getSimplePredicate());
        } else if (node.getCompoundPredicate() != null) {
            predicate = parseCompoundPredicate(node.getCompoundPredicate());
        } else if (!(null == node.getSimpleSetPredicate())) {
            predicate = parseSimpleSetPredicate(node.getSimpleSetPredicate());
        }
        return predicate;
    }

    /* Retrieve the common split attribute of all children. The tree
     * representation of KNIME is different than in PMML: KNIME stores the
     * split attribute in its parent while PMML stores it as predicate with the
     * node itself. Hence we have to look at the children to get the KNIME
     * representation. */
    private String getChildrenSplitAttribute(final Node node) {
        String splitAttribute = "";
        for (Node child : node.getNodeArray()) {
            String childSplit = null;
            if (child.getSimplePredicate() != null) {
                childSplit = child.getSimplePredicate().getField();
            } else if (child.getSimpleSetPredicate() != null) {
                childSplit = child.getSimpleSetPredicate().getField();
            }
            if (childSplit == null) {
                continue;
            }
            if (splitAttribute != null && !splitAttribute.equals(childSplit)) {
                // to stay compatible with previous implementation
                return "";
            } else {
                splitAttribute = childSplit;
            }
        }

        if (splitAttribute != null) {
            /* Get the mapped column name for the predicate if it is not null
             * which is the case for True or False predicates, for example.*/
            splitAttribute = m_nameMapper.getColumnName(splitAttribute);
        }
        return splitAttribute;
    }

    private DataCell getMajorityClass(final Node node) {
        String score = node.getScore();
        if (score != null) {
            return  new StringCell(score);
        }
		LinkedHashMap<DataCell, Double> knimeScoreDistribution
		        = getClassCount(node);
        double maxValue = 0;
        String category = null;
        for (Entry<DataCell, Double> entry
                : knimeScoreDistribution.entrySet()) {
            // first encountered value wins on ties
            if (category == null // set the first value as default
                    || entry.getValue() > maxValue) {
                category = entry.getKey().toString();
                maxValue = entry.getValue();
            }
        }
        return new StringCell(category);
    }

    private LinkedHashMap<DataCell, Double> getClassCount(final Node node) {
		LinkedHashMap<DataCell, Double> knimeScoreDistribution
		        = new LinkedHashMap<DataCell, Double>();
		ScoreDistribution[] pmmlScoreDistArray = node
				.getScoreDistributionArray();
        for (ScoreDistribution sd : pmmlScoreDistArray) {
            String category = sd.getValue();
            Double recordCount = sd.getRecordCount();
            knimeScoreDistribution.put(new StringCell(category), recordCount);
        }
        return knimeScoreDistribution;
    }

    /**
     * @param operator
     * @return
     */
    @Override
    protected PMMLCompoundPredicate newCompoundPredicate(final String operator) {
        return new PMMLCompoundPredicate(
        operator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PMMLSimplePredicate newSimplePredicate(final String field, final String operator, final String value) {
        return new PMMLSimplePredicate(field, operator, value);
    }
}
