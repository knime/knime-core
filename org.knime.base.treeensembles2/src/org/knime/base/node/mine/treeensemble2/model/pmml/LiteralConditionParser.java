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
 *   06.09.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import org.dmg.pmml.ArrayType;
import org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate;
import org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate.BooleanOperator.Enum;
import org.dmg.pmml.FalseDocument.False;
import org.dmg.pmml.NodeDocument.Node;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicateDocument.SimpleSetPredicate;
import org.dmg.pmml.TrueDocument.True;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeNumericColumnMetaData;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNodeSurrogateCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeColumnCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNominalBinaryCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNominalCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNumericCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNumericCondition.NumericOperator;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSurrogateCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSurrogateOnlyDefDirCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeTrueCondition;
import org.knime.core.node.util.CheckUtils;

/**
 * Parses the exact condition from the provided {@link Node} object (including any tests for missing values).
 *
 * @author Adrian Nembach, KNIME.com
 */
final class LiteralConditionParser implements ConditionParser {
    private static final String BACKSLASH = "\\";

    private static final String DOUBLE_QUOT = "\"";

    private static final String SPACE = " ";

    private static final String TAB = "\t";

    private static final int OR_COMPOUND_SIMPLEPREDICATE_LIMIT = 2;

    private static final int OR_COMPOUND_SIMPLESETPREDICATE_LIMIT = 1;

    private final MetaDataMapper m_metaDataMapper;

    LiteralConditionParser(final MetaDataMapper metaDataMapper) {
        m_metaDataMapper = metaDataMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeNodeCondition parseCondition(final Node node) {
        CompoundPredicate compound = node.getCompoundPredicate();
        if (compound != null) {
            return handleCompoundPredicate(compound);
        }
        SimplePredicate simplePred = node.getSimplePredicate();
        if (simplePred != null) {
            return handleSimplePredicate(simplePred, false);
        }
        SimpleSetPredicate simpleSetPred = node.getSimpleSetPredicate();
        if (simpleSetPred != null) {
            return handleSimpleSetPredicate(simpleSetPred, false);
        }
        True truePred = node.getTrue();
        if (truePred != null) {
            return TreeNodeTrueCondition.INSTANCE;
        }
        False falsePred = node.getFalse();
        if (falsePred != null) {
            throw new IllegalArgumentException("There is no False condition in KNIME.");
        }
        throw new IllegalStateException("The pmmlNode contains no valid Predicate.");
    }

    private TreeNodeCondition handleCompoundPredicate(final CompoundPredicate compound) {
        Enum operator = compound.getBooleanOperator();
        if (operator.equals(CompoundPredicate.BooleanOperator.SURROGATE)) {
            return parseSurrogateCompound(compound);
        } else if (operator.equals(CompoundPredicate.BooleanOperator.OR)) {
            return parseOrCompound(compound);
        }
        throw new IllegalArgumentException("The operator \"" + operator + "\" is currently not supported.");
    }

    private AbstractTreeNodeSurrogateCondition parseSurrogateCompound(final CompoundPredicate compound) {
        CheckUtils.checkArgument(compound.getCompoundPredicateList().isEmpty(),
            "Compound predicates inside surrogate-compounds are currently not supported.");
        List<TreeNodeColumnCondition> conds = new ArrayList<>();
        for (SimplePredicate simplePred : compound.getSimplePredicateList()) {
            conds.add(handleSimplePredicate(simplePred, false));
        }
        for (SimpleSetPredicate simpleSetPred : compound.getSimpleSetPredicateList()) {
            conds.add(handleSimpleSetPredicate(simpleSetPred, false));
        }
        CheckUtils.checkArgument(!conds.isEmpty(),
            "The surrogate-compound \"%s\" contains no SimplePredicates or SimpleSetPredicates.", compound);
        boolean defaultResponse = !compound.getTrueList().isEmpty();

        if (conds.size() == 1) {
            return new TreeNodeSurrogateOnlyDefDirCondition(conds.get(0), defaultResponse);
        } else {
            return new TreeNodeSurrogateCondition(conds.toArray(new TreeNodeColumnCondition[conds.size()]),
                defaultResponse);
        }
    }

    private TreeNodeColumnCondition parseOrCompound(final CompoundPredicate compound) {
        CheckUtils.checkArgument(compound.getCompoundPredicateList().isEmpty(),
            "Currently only simple or-compounds are supported that consist of exactly one"
            + " SimpleSetPredicate or SimplePredicate followed by a SimplePredicate with operator isMissing.");
        List<SimplePredicate> simplePredicates = compound.getSimplePredicateList();
        CheckUtils.checkArgument(simplePredicates.size() <= OR_COMPOUND_SIMPLEPREDICATE_LIMIT,
                "Currently at most two SimplePredicates are supported in an or-compound.");
        List<SimpleSetPredicate> simpleSetPredicates = compound.getSimpleSetPredicateList();
        CheckUtils.checkArgument(simpleSetPredicates.size() <= OR_COMPOUND_SIMPLESETPREDICATE_LIMIT,
                "Currently at most one SimpleSetPredicate is supported in an or-compound.");
        boolean acceptsMissings = false;
        for (SimplePredicate simplePred : simplePredicates) {
            if (simplePred.getOperator() == SimplePredicate.Operator.IS_MISSING) {
                acceptsMissings = true;
                // we may not try to parse a simple predicate with operator is_missing
                simplePredicates.remove(simplePred);
                break;
            }
        }
        if (!simplePredicates.isEmpty()) {
            return handleSimplePredicate(simplePredicates.get(0), acceptsMissings);
        } else if (!simpleSetPredicates.isEmpty()) {
            return handleSimpleSetPredicate(simpleSetPredicates.get(0), acceptsMissings);
        }
        throw new IllegalArgumentException("There was only a is_missing predicate contained in the or-compound \""
            + compound + "\" (or at least no more SimplePredicates or SimpleSetPredicates).");
    }

    private TreeNodeColumnCondition handleSimplePredicate(final SimplePredicate simplePred,
        final boolean acceptsMissings) {
        String field = simplePred.getField();
        if (m_metaDataMapper.isNominal(field)) {
            TreeNominalColumnMetaData metaData = m_metaDataMapper.getNominalColumnMetaData(field);
            return new TreeNodeNominalCondition(metaData, getValueIndex(metaData, simplePred.getValue()),
                acceptsMissings);
        } else {
            TreeNumericColumnMetaData metaData = m_metaDataMapper.getNumericColumnMetaData(field);
            double value = Double.parseDouble(simplePred.getValue());
            return new TreeNodeNumericCondition(metaData, value, parseNumericOperator(simplePred.getOperator()),
                acceptsMissings);
        }
    }

    private static NumericOperator parseNumericOperator(final SimplePredicate.Operator.Enum operator) {
        if (operator.equals(SimplePredicate.Operator.LESS_OR_EQUAL)) {
            return NumericOperator.LessThanOrEqual;
        } else if (operator.equals(SimplePredicate.Operator.GREATER_THAN)) {
            return NumericOperator.LargerThan;
        }
        throw new IllegalArgumentException("The numeric operator \"" + operator + "\" is currently not supported.");
    }

    private static int getValueIndex(final TreeNominalColumnMetaData metaData, final String value) {
        // this implementation is slow as it has to scan the nominal value representation for all values
        NominalValueRepresentation[] values = metaData.getValues();
        Optional<NominalValueRepresentation> optional = Arrays.stream(values)
                .filter(v -> v.getNominalValue().equals(value)).findFirst();
        CheckUtils.checkArgument(optional.isPresent(),
            "The value \"%s\" is not a valid value for field \"%s\".", value, metaData.getAttributeName());
        return optional.get().getAssignedInteger();
    }

    private TreeNodeColumnCondition handleSimpleSetPredicate(final SimpleSetPredicate simpleSetPred,
        final boolean acceptsMissings) {
        String field = simpleSetPred.getField();
        CheckUtils.checkArgument(m_metaDataMapper.isNominal(field),
            "The field \"%s\" is not nominal but currently only nominal fields can be used for SimpleSetPredicates",
            field);
        TreeNominalColumnMetaData metaData = m_metaDataMapper.getNominalColumnMetaData(field);
        boolean isInSet = simpleSetPred.getBooleanOperator().equals(SimpleSetPredicate.BooleanOperator.IS_IN);

        return new TreeNodeNominalBinaryCondition(
            metaData, parseValuesMask(simpleSetPred, metaData), isInSet, acceptsMissings);
    }

    private static BigInteger parseValuesMask(final SimpleSetPredicate simpleSetPred,
        final TreeNominalColumnMetaData metaData) {
        String[] array = parseArrayType(simpleSetPred.getArray());
        BitSet bs = new BitSet();
        for (String val : array) {
            bs.set(getValueIndex(metaData, val));
        }
        return new BigInteger(bs.toByteArray());
    }

    private static String[] parseArrayType(final ArrayType array) {
        String content = array.newCursor().getTextValue();
        String[] stringValues;
        content = content.trim();

        if (content.contains(DOUBLE_QUOT)) {
            content = content.replace(BACKSLASH + DOUBLE_QUOT, TAB);
            // ==> <Array n="3" type="string">"Cheval  Blanc" "TABTAB"
            // "Latour"</Array>

            stringValues = content.split(DOUBLE_QUOT + SPACE);

            for (int i = 0; i < stringValues.length; i++) {
                stringValues[i] = stringValues[i].replace(DOUBLE_QUOT, "");
                stringValues[i] = stringValues[i].replace(TAB, DOUBLE_QUOT);
                if (ArrayType.Type.STRING != array.getType()) {
                    // do not trim string values
                    stringValues[i] = stringValues[i].trim();
                }
            }
        } else {
            stringValues = content.split("\\s+");
        }
        return stringValues;
    }

}
