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
 * -------------------------------------------------------------------
 *
 */
package org.knime.expressions.node;

import java.util.ArrayList;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.knime.base.node.util.KnimeCompletionProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
public class ExpressionCompletionProvider extends KnimeCompletionProvider {

	private static final String ESCAPE_COLUMN_START_SYMBOL = "${";
	private static final String ESCAPE_COLUMN_END_SYMBOL = "}";
	private static final String ESCAPE_FLOW_VARIABLE_START_SYMBOL = "$[";
	private static final String ESCAPE_FLOW_VARIABLE_END_SYMBOL = "]";
	private static final String ESCAPE_EXPRESSION_START_SYMBOL = "$(";
	private static final String ESCAPE_EXPRESSION_END_SYMBOL = ")";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String escapeColumnName(String colName) {
		return ESCAPE_COLUMN_START_SYMBOL + colName + ESCAPE_COLUMN_END_SYMBOL;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String escapeFlowVariableName(String varName) {
		return ESCAPE_FLOW_VARIABLE_START_SYMBOL + varName + ESCAPE_FLOW_VARIABLE_END_SYMBOL;
	}

	/**
	 * 
	 * @return the escape start symbol used for column names.
	 */
	public static String getEscapeColumnStartSymbol() {
		return ESCAPE_COLUMN_START_SYMBOL;
	}

	/**
	 * 
	 * @return the escape start symbol used for flow variables.
	 */
	public static String getEscapeFlowVariableStartSymbol() {
		return ESCAPE_FLOW_VARIABLE_START_SYMBOL;
	}
	
	/**
	 * 
	 * @return the escape start symbol used for expressions (e.g. {@link Expression#ROWID})
	 */
	public static String getEscapeExpressionStartSymbol() {
		return ESCAPE_EXPRESSION_START_SYMBOL;
	}
	
	/**
	 * 
	 * @return the escape end symbol used for column names.
	 */
	public static String getEscapeColumnEndSymbol() {
		return ESCAPE_COLUMN_END_SYMBOL;
	}

	/**
	 * 
	 * @return the escape start symbol used for flow variables.
	 */
	public static String getEscapeFlowVariableEndSymbol() {
		return ESCAPE_FLOW_VARIABLE_END_SYMBOL;
	}
	
	/**
	 * 
	 * @return the escape start symbol used for expressions (e.g. {@link Expression#ROWID})
	 */
	public static String getEscapeExpressionEndSymbol() {
		return ESCAPE_EXPRESSION_END_SYMBOL;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFlowVariables(final Iterable<FlowVariable> variables) {
		ArrayList<Completion> completionList = new ArrayList<>();

		for (FlowVariable var : variables) {
			completionList.add(new BasicCompletion(this,
					ESCAPE_FLOW_VARIABLE_START_SYMBOL + var.getName() + ESCAPE_FLOW_VARIABLE_END_SYMBOL,
					var.getType().toString(), "The flow variable " + var.getName() + "."));
		}
		
		addCompletions(completionList);
	}

}
