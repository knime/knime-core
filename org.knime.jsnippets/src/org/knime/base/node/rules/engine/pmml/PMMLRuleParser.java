/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * Created on 2013.08.09. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.pmml;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.rules.engine.BaseRuleParser;
import org.knime.base.node.rules.engine.Expression;
import org.knime.base.node.rules.engine.ExpressionFactory;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.RuleExpressionFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * PMML 4.1 rule parser.
 *
 * @author Gabor Bakos
 * @since 2.9
 */
public class PMMLRuleParser extends BaseRuleParser<PMMLPredicate> {

    /**
     * Constructs the {@link PMMLRuleParser} based on the specification and the flow variables.
     *
     * @param spec A {@link DataTableSpec}.
     * @param flowVariables The flow variables.
     */
    public PMMLRuleParser(final DataTableSpec spec, final Map<String, FlowVariable> flowVariables) {
        super(spec, flowVariables, new PMMLExpressionFactory(), ExpressionFactory.getInstance(), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PMMLPredicate handleRelationRightOperand(final ParseState state, final int beforeLeft,
        final Expression left, final int beforeOperator, final Operators op) throws ParseException {
        switch (op) {
            case LIKE:
            case MATCHES:
                throw new ParseException("PMML do not support this operator: " + op, beforeOperator);
            default:
                break;
        }
        try {
            return super.handleRelationRightOperand(state, beforeLeft, left, beforeOperator, op);
        } catch (RuntimeException e) {
            throw new ParseException(e.getMessage(), beforeLeft);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression parseOutcomeOperand(final ParseState state, final Boolean booleanOutcome)
        throws ParseException {
        int positionBeforeOutcome = state.getPosition();
        Expression outcome = super.parseOutcomeOperand(state, booleanOutcome);
        if (!outcome.isConstant()) {
            throw new ParseException("Cannot have a reference in the outcome", positionBeforeOutcome);
        }
        return outcome;
    }

    /**
     * @return the usedColumns.
     */
    public List<String> getUsedColumns() {
        RuleExpressionFactory<PMMLPredicate, Expression> factoryPred = getFactoryPred();
        if (factoryPred instanceof PMMLExpressionFactory) {
            PMMLExpressionFactory factory = (PMMLExpressionFactory)factoryPred;
            return new ArrayList<String>(factory.getUsedColumns());
        }
        assert false : "For some reason the factory is not of proper type: " + (factoryPred == null ? "null"
            : factoryPred.getClass().getName());
        return Collections.emptyList();
    }
}
