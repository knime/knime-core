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
 *
 * History
 *   Jan 14, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.model;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.MISSINGVALUESTRATEGY;
import org.dmg.pmml.NOTRUECHILDSTRATEGY;
import org.dmg.pmml.NodeDocument;
import org.dmg.pmml.NodeDocument.Node;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.ScoreDistributionDocument.ScoreDistribution;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate.Operator;
import org.dmg.pmml.TreeModelDocument;
import org.dmg.pmml.TreeModelDocument.TreeModel.SplitCharacteristic;
import org.knime.base.node.mine.treeensemble.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble.model.TreeNodeNumericCondition.NumericOperator;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;

/**
 * 
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeModelPMMLTranslator implements PMMLTranslator {

    private final TreeModelClassification m_treeModel;

    /**
     * enumeration of the tree nodes. KNIME does not support node ids of type string (if it did we were using the
     * TreeSignature instead of this int).
     */
    private int m_nodeIndex;

    /**
     *  */
    public TreeModelPMMLTranslator(final TreeModelClassification treeModel) {
        m_treeModel = treeModel;
    }

    /** {@inheritDoc} */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        throw new UnsupportedOperationException("Reading PMML not supported");
    }

    /** {@inheritDoc} */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc, final PMMLPortObjectSpec spec) {

        PMML pmml = pmmlDoc.getPMML();
        TreeModelDocument.TreeModel treeModel = pmml.addNewTreeModel();

        PMMLMiningSchemaTranslator.writeMiningSchema(spec, treeModel);
        treeModel.setModelName("DecisionTree");
        treeModel.setFunctionName(MININGFUNCTION.CLASSIFICATION);

        TreeNodeClassification rootNode = m_treeModel.getRootNode();
        // ----------------------------------------------
        // set up splitCharacteristic
        if (isMultiSplitRecursive(rootNode)) {
            treeModel.setSplitCharacteristic(SplitCharacteristic.MULTI_SPLIT);
        } else {
            treeModel.setSplitCharacteristic(SplitCharacteristic.BINARY_SPLIT);
        }

        // ----------------------------------------------
        // set up missing value strategy
        treeModel.setMissingValueStrategy(MISSINGVALUESTRATEGY.NONE);

        // -------------------------------------------------
        // set up no true child strategy
        treeModel.setNoTrueChildStrategy(NOTRUECHILDSTRATEGY.RETURN_LAST_PREDICTION);

        // --------------------------------------------------
        // set up tree node
        NodeDocument.Node rootPMMLNode = treeModel.addNewNode();
        addTreeNode(rootPMMLNode, rootNode);
        return TreeModelDocument.TreeModel.type;
    }

    /**
     * @param pmmlNode
     * @param node
     */
    private void addTreeNode(final Node pmmlNode, final TreeNodeClassification node) {
        int index = m_nodeIndex++;
        pmmlNode.setId(Integer.toString(index));
        pmmlNode.setScore(node.getMajorityClassName());
        double[] targetDistribution = node.getTargetDistribution();
        NominalValueRepresentation[] targetVals = node.getTargetMetaData().getValues();
        double sum = 0.0;
        for (Double v : targetDistribution) {
            sum += v;
        }
        pmmlNode.setRecordCount(sum);

        TreeNodeCondition condition = node.getCondition();
        if (condition instanceof TreeNodeTrueCondition) {
            pmmlNode.addNewTrue();
        } else if (condition instanceof TreeNodeColumnCondition) {
            final TreeNodeColumnCondition colCondition = (TreeNodeColumnCondition)condition;
            final String colName = colCondition.getColumnMetaData().getAttributeName();
            final Operator.Enum operator;
            final String value;
            if (condition instanceof TreeNodeNominalCondition) {
                final TreeNodeNominalCondition nominalCondition = (TreeNodeNominalCondition)condition;
                operator = Operator.EQUAL;
                value = nominalCondition.getValue();
            } else if (condition instanceof TreeNodeBitCondition) {
                final TreeNodeBitCondition bitCondition = (TreeNodeBitCondition)condition;
                operator = Operator.EQUAL;
                value = bitCondition.getValue() ? "1" : "0";
            } else if (condition instanceof TreeNodeNumericCondition) {
                final TreeNodeNumericCondition numCondition = (TreeNodeNumericCondition)condition;
                NumericOperator numOperator = numCondition.getNumericOperator();
                switch (numOperator) {
                    case LargerThan:
                        operator = Operator.GREATER_THAN;
                        break;
                    case LessThanOrEqual:
                        operator = Operator.LESS_OR_EQUAL;
                        break;
                    default:
                        throw new IllegalStateException("Unsupported operator (not " + "implemented): " + numOperator);
                }
                value = Double.toString(numCondition.getSplitValue());
            } else {
                throw new IllegalStateException("Unsupported condition (not " + "implemented): "
                    + condition.getClass().getSimpleName());
            }
            SimplePredicate pmmlSimplePredicate = pmmlNode.addNewSimplePredicate();
            pmmlSimplePredicate.setField(colName);
            pmmlSimplePredicate.setOperator(operator);
            pmmlSimplePredicate.setValue(value);
        } else {
            throw new IllegalStateException("Unsupported condition (not " + "implemented): "
                + condition.getClass().getSimpleName());
        }

        // adding score distribution (class counts)
        for (int i = 0; i < targetDistribution.length; i++) {
            String className = targetVals[i].getNominalValue();
            double freq = targetDistribution[i];
            ScoreDistribution pmmlScoreDist = pmmlNode.addNewScoreDistribution();
            pmmlScoreDist.setValue(className);
            pmmlScoreDist.setRecordCount(freq);
        }

        for (int i = 0; i < node.getNrChildren(); i++) {
            addTreeNode(pmmlNode.addNewNode(), node.getChild(i));
        }
    }

    private static boolean isMultiSplitRecursive(final TreeNodeClassification node) {
        final int nrChildren = node.getNrChildren();
        if (nrChildren > 2) {
            return true;
        }
        for (int i = 0; i < nrChildren; i++) {
            TreeNodeClassification child = node.getChild(i);
            if (isMultiSplitRecursive(child)) {
                return true;
            }
        }
        return false;
    }

}
