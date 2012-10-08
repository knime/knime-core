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
 *   Apr 18, 2011 (morent): created
 */

package org.knime.base.node.mine.decisiontree2;

import java.math.BigInteger;

import org.apache.xmlbeans.XmlCursor;
import org.dmg.pmml.ArrayType;
import org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate;
import org.dmg.pmml.CompoundRuleDocument.CompoundRule;
import org.dmg.pmml.NodeDocument.Node;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate;
import org.dmg.pmml.SimpleRuleDocument.SimpleRule;
import org.dmg.pmml.SimpleSetPredicateDocument.SimpleSetPredicate;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public final class PMMLPredicateTranslator {

    private PMMLPredicateTranslator() {
        // hiding constructor of utility class
    }
    /**
     * @param predicate the predicate to export
     * @param node the Node element to add the predicate to
     */
    public static void exportTo(final PMMLPredicate predicate,
            final Node node) {
        /** Is basically a duplicate of the other export methods
         * but there is no common parent class and therefore the code is not
         * really reusable. */
        if (predicate instanceof PMMLFalsePredicate) {
            node.addNewFalse();
        } else if (predicate instanceof PMMLTruePredicate) {
            node.addNewTrue();
        } else if (predicate instanceof PMMLFalsePredicate) {
            node.addNewFalse();
        } else if (predicate instanceof PMMLSimplePredicate) {
            PMMLSimplePredicate sp = (PMMLSimplePredicate)predicate;
            SimplePredicate simplePred = node.addNewSimplePredicate();
            initSimplePredicate(sp, simplePred);
        } else if (predicate instanceof PMMLSimpleSetPredicate) {
            PMMLSimpleSetPredicate sp = (PMMLSimpleSetPredicate)predicate;
            SimpleSetPredicate setPred = node.addNewSimpleSetPredicate();
            initSimpleSetPred(sp, setPred);
        } else if (predicate instanceof PMMLCompoundPredicate) {
            PMMLCompoundPredicate compPred = (PMMLCompoundPredicate)predicate;
            CompoundPredicate cp = CompoundPredicate.Factory.newInstance();
            cp.setBooleanOperator(getOperator(compPred.getBooleanOperator()));
            for (PMMLPredicate pred : compPred.getPredicates()) {
                exportTo(pred, cp);
            }
        }
    }

    private static void initSimpleSetPred(final PMMLSimpleSetPredicate sp,
            final SimpleSetPredicate setPred) {
        setPred.setField(sp.getSplitAttribute());
        setPred.setBooleanOperator(getOperator(sp.getSetOperator()));
        ArrayType array = setPred.addNewArray();
        array.setN(BigInteger.valueOf(sp.getValues().size()));
        array.setType(getType(sp.getArrayType()));
        // how to set content?
        StringBuffer sb = new StringBuffer();
        if (sp.getArrayType() == PMMLArrayType.STRING) {
            for (String value : sp.getValues()) {
                sb.append('"');
                sb.append(value.replace("\"", "\\\""));
                sb.append('"');
                sb.append(' ');
            }
        } else {
            for (String value : sp.getValues()) {
                sb.append(value);
                sb.append(' ');
            }
        }
        XmlCursor xmlCursor = array.newCursor();
        xmlCursor.setTextValue(sb.toString());
        xmlCursor.dispose();
    }

    private static void initSimplePredicate(final PMMLSimplePredicate sp,
            final SimplePredicate simplePred) {
        simplePred.setField(sp.getSplitAttribute());
        simplePred.setOperator(getOperator(sp.getOperator()));
        simplePred.setValue(sp.getThreshold());
    }

    /**
     * @param predicate the predicate to export
     * @param compound the CompundPredicate element to add the predicate to
     */
    public static void exportTo(final PMMLPredicate predicate,
            final CompoundPredicate compound) {
        /** Is basically a duplicate of the other export methods
         * but there is no common parent class and therefore the code is not
         * really reusable. */
        if (predicate instanceof PMMLFalsePredicate) {
            compound.addNewFalse();
        } else if (predicate instanceof PMMLTruePredicate) {
            compound.addNewTrue();
        } else if (predicate instanceof PMMLFalsePredicate) {
            compound.addNewFalse();
        } else if (predicate instanceof PMMLSimplePredicate) {
            PMMLSimplePredicate sp = (PMMLSimplePredicate)predicate;
            SimplePredicate simplePred = compound.addNewSimplePredicate();
            initSimplePredicate(sp, simplePred);
        } else if (predicate instanceof PMMLSimpleSetPredicate) {
            PMMLSimpleSetPredicate sp = (PMMLSimpleSetPredicate)predicate;
            SimpleSetPredicate setPred = compound.addNewSimpleSetPredicate();
            initSimpleSetPred(sp, setPred);
        } else if (predicate instanceof PMMLCompoundPredicate) {
            PMMLCompoundPredicate compPred = (PMMLCompoundPredicate)predicate;
            CompoundPredicate cp = CompoundPredicate.Factory.newInstance();
            cp.setBooleanOperator(getOperator(compPred.getBooleanOperator()));
            for (PMMLPredicate pred : compPred.getPredicates()) {
                exportTo(pred, cp);
            }
        }
    }

    /**
     * @param predicate the predicate to export
     * @param simpleRule the SimpleRule element to add the predicate to
     */
    public static void exportTo(final PMMLPredicate predicate,
            final SimpleRule simpleRule) {
        throw new UnsupportedOperationException(
            "SimpleRule exporting is not supported.");
    }

    /**
     * @param predicate  the predicate to export
     * @param compoundRule the CompoundRule element to add the predicate to
     */
    public static void exportTo(final PMMLPredicate predicate,
            final CompoundRule compoundRule) {
        throw new UnsupportedOperationException(
                "CompoundRule exporting is not supported.");
    }

    private static SimplePredicate.Operator.Enum getOperator(
            final PMMLOperator op) {
        switch (op) {
            case EQUAL:
                return SimplePredicate.Operator.EQUAL;
            case GREATER_OR_EQUAL:
                return SimplePredicate.Operator.GREATER_OR_EQUAL;
            case GREATER_THAN:
                return SimplePredicate.Operator.GREATER_THAN;
            case IS_MISSING:
                return SimplePredicate.Operator.IS_MISSING;
            case IS_NOT_MISSING:
                return SimplePredicate.Operator.IS_NOT_MISSING;
            case LESS_OR_EQUAL:
                return SimplePredicate.Operator.LESS_OR_EQUAL;
            case LESS_THAN:
                return SimplePredicate.Operator.LESS_THAN;
            case NOT_EQUAL:
                return SimplePredicate.Operator.NOT_EQUAL;
        }
        return null;
    }

    private static SimpleSetPredicate.BooleanOperator.Enum getOperator(
            final PMMLSetOperator op) {
        switch (op) {
            case IS_IN:
                return SimpleSetPredicate.BooleanOperator.IS_IN;
            case IS_NOT_IN:
                return SimpleSetPredicate.BooleanOperator.IS_NOT_IN;
        }
        return null;
    }

    private static CompoundPredicate.BooleanOperator.Enum getOperator(
            final PMMLBooleanOperator op) {
        switch (op) {
            case AND:
                return CompoundPredicate.BooleanOperator.AND;
            case OR:
                return CompoundPredicate.BooleanOperator.OR;
            case SURROGATE:
                return CompoundPredicate.BooleanOperator.SURROGATE;
            case XOR:
                return CompoundPredicate.BooleanOperator.XOR;
        }
        return null;
    }

    private static ArrayType.Type.Enum getType(final PMMLArrayType type) {
        switch (type) {
        case INT:
            return ArrayType.Type.INT;
        case REAL:
            return ArrayType.Type.REAL;
        case STRING:
            return ArrayType.Type.STRING;
        }
        return null;
    }

}
