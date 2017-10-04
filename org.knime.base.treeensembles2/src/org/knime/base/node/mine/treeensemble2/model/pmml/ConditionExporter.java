/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   13.09.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.dmg.pmml.ArrayType;
import org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate;
import org.dmg.pmml.NodeDocument.Node;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate.Operator;
import org.dmg.pmml.SimpleSetPredicateDocument.SimpleSetPredicate;
import org.dmg.pmml.SimpleSetPredicateDocument.SimpleSetPredicate.BooleanOperator.Enum;
import org.knime.base.node.mine.decisiontree2.PMMLArrayType;
import org.knime.base.node.mine.decisiontree2.PMMLBooleanOperator;
import org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLFalsePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLOperator;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSetOperator;
import org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimpleSetPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLTruePredicate;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNodeSurrogateCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeColumnCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeTrueCondition;

/**
 * Exports node conditions to PMML.
 *
 * @author Adrian Nembach, KNIME
 */
final class ConditionExporter {

    static void exportCondition(final TreeNodeCondition condition, final Node pmmlNode) {
        if (condition instanceof TreeNodeTrueCondition) {
            pmmlNode.addNewTrue();
        } else if (condition instanceof TreeNodeColumnCondition) {
            final TreeNodeColumnCondition colCondition = (TreeNodeColumnCondition)condition;
            exportColumnCondition(colCondition, pmmlNode);
        } else if (condition instanceof AbstractTreeNodeSurrogateCondition) {
            final AbstractTreeNodeSurrogateCondition surrogateCond = (AbstractTreeNodeSurrogateCondition)condition;
            setValuesFromPMMLCompoundPredicate(pmmlNode.addNewCompoundPredicate(), surrogateCond.toPMMLPredicate());
        } else {
            throw new IllegalStateException(
                "Unsupported condition (not implemented): " + condition.getClass().getSimpleName());
        }
    }

    private static void exportColumnCondition(final TreeNodeColumnCondition condition, final Node pmmlNode) {
        final PMMLPredicate knimePredicate = condition.toPMMLPredicate();
        if (knimePredicate instanceof PMMLCompoundPredicate) {
            setValuesFromPMMLCompoundPredicate(pmmlNode.addNewCompoundPredicate(),
                (PMMLCompoundPredicate)knimePredicate);
        } else if (knimePredicate instanceof PMMLSimplePredicate) {
            setValuesFromPMMLSimplePredicate(pmmlNode.addNewSimplePredicate(), (PMMLSimplePredicate)knimePredicate);
        } else if (knimePredicate instanceof PMMLSimpleSetPredicate) {
            setValuesFromPMMLSimpleSetPredicate(pmmlNode.addNewSimpleSetPredicate(),
                (PMMLSimpleSetPredicate)knimePredicate);
        } else {
            throw new IllegalArgumentException(
                "A column condition can only contain compound, simple or simple set predicates.");
        }
    }

    private static void setValuesFromPMMLSimplePredicate(final SimplePredicate to, final PMMLSimplePredicate from) {
        to.setField(from.getSplitAttribute());
        Operator.Enum operator;
        final PMMLOperator op = from.getOperator();
        switch (op) {
            case EQUAL:
                operator = Operator.EQUAL;
                to.setValue(from.getThreshold());
                break;
            case GREATER_OR_EQUAL:
                operator = Operator.GREATER_OR_EQUAL;
                to.setValue(from.getThreshold());
                break;
            case GREATER_THAN:
                operator = Operator.GREATER_THAN;
                to.setValue(from.getThreshold());
                break;
            case IS_MISSING:
                operator = Operator.IS_MISSING;
                break;
            case IS_NOT_MISSING:
                operator = Operator.IS_NOT_MISSING;
                break;
            case LESS_OR_EQUAL:
                operator = Operator.LESS_OR_EQUAL;
                to.setValue(from.getThreshold());
                break;
            case LESS_THAN:
                operator = Operator.LESS_THAN;
                to.setValue(from.getThreshold());
                break;
            case NOT_EQUAL:
                operator = Operator.NOT_EQUAL;
                to.setValue(from.getThreshold());
                break;
            default:
                throw new IllegalStateException("Unknown pmml operator \"" + op + "\".");
        }
        to.setOperator(operator);
    }

    private static void setValuesFromPMMLSimpleSetPredicate(final SimpleSetPredicate to,
        final PMMLSimpleSetPredicate from) {
        to.setField(from.getSplitAttribute());
        final Enum operator;
        final PMMLSetOperator setOp = from.getSetOperator();
        switch (setOp) {
            case IS_IN:
                operator = SimpleSetPredicate.BooleanOperator.IS_IN;
                break;
            case IS_NOT_IN:
                operator = SimpleSetPredicate.BooleanOperator.IS_NOT_IN;
                break;
            default:
                throw new IllegalStateException("Unknown set operator \"" + setOp + "\".");
        }
        to.setBooleanOperator(operator);
        final Set<String> values = from.getValues();
        ArrayType array = to.addNewArray();
        array.setN(BigInteger.valueOf(values.size()));
        org.w3c.dom.Node arrayNode = array.getDomNode();
        arrayNode.appendChild(arrayNode.getOwnerDocument().createTextNode(setToWhitspaceSeparatedString(values)));
        final org.dmg.pmml.ArrayType.Type.Enum type;
        final PMMLArrayType arrayType = from.getArrayType();
        switch (arrayType) {
            case INT:
                type = ArrayType.Type.INT;
                break;
            case REAL:
                type = ArrayType.Type.REAL;
                break;
            case STRING:
                type = ArrayType.Type.STRING;
                break;
            default:
                throw new IllegalStateException("Unknown array type \"" + arrayType + "\".");
        }
        array.setType(type);
    }

    private static void setValuesFromPMMLCompoundPredicate(final CompoundPredicate to,
        final PMMLCompoundPredicate from) {
        final PMMLBooleanOperator boolOp = from.getBooleanOperator();
        switch (boolOp) {
            case AND:
                to.setBooleanOperator(CompoundPredicate.BooleanOperator.AND);
                break;
            case OR:
                to.setBooleanOperator(CompoundPredicate.BooleanOperator.OR);
                break;
            case SURROGATE:
                to.setBooleanOperator(CompoundPredicate.BooleanOperator.SURROGATE);
                break;
            case XOR:
                to.setBooleanOperator(CompoundPredicate.BooleanOperator.XOR);
                break;
            default:
                throw new IllegalStateException("Unknown boolean predicate \"" + boolOp + "\".");
        }
        final List<PMMLPredicate> predicates = from.getPredicates();
        for (final PMMLPredicate predicate : predicates) {
            if (predicate instanceof PMMLSimplePredicate) {
                setValuesFromPMMLSimplePredicate(to.addNewSimplePredicate(), (PMMLSimplePredicate)predicate);
            } else if (predicate instanceof PMMLSimpleSetPredicate) {
                setValuesFromPMMLSimpleSetPredicate(to.addNewSimpleSetPredicate(), (PMMLSimpleSetPredicate)predicate);
            } else if (predicate instanceof PMMLTruePredicate) {
                to.addNewTrue();
            } else if (predicate instanceof PMMLFalsePredicate) {
                to.addNewFalse();
            } else if (predicate instanceof PMMLCompoundPredicate) {
                final CompoundPredicate compound = to.addNewCompoundPredicate();
                final PMMLCompoundPredicate knimeCompound = (PMMLCompoundPredicate)predicate;
                setValuesFromPMMLCompoundPredicate(compound, knimeCompound);
            } else {
                throw new IllegalStateException("Unknown predicate type \"" + predicate + "\".");
            }
        }
    }

    private static String setToWhitspaceSeparatedString(final Set<String> set) {
        final StringBuilder sb = new StringBuilder();
        for (final String string : set.stream().sorted().collect(Collectors.toList())) {
            sb.append("\"");
            sb.append(string);
            sb.append("\"");
            sb.append(" ");
        }
        return sb.toString();
    }

}
