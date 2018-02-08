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
package org.knime.expressions.core;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.expressions.core.exceptions.ScriptCompilationException;
import org.knime.expressions.core.exceptions.ScriptParseException;

/**
 * A class that prepares and compiles an expression, and provides a
 * {@link FunctionScript} which can be used to run the script on different
 * {@link ScriptRowInput}s.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class Expressions {

	/**
	 * Wraps the provided script into a general {@link FunctionScript} that takes a
	 * {@link ScriptRowInput} as input and returns a {@link DataCell} of the
	 * provided {@link DataType}.
	 * 
	 * @param expression
	 *            The expression that shall be parsed and compiled.
	 * @param columns
	 *            The {@link DataColumnSpec}s of the input table.
	 * @param variables
	 *            The available {@link FlowVariable}s.
	 * @param returnType
	 *            The {@link DataType} that shall be returned.
	 * @return {@link FunctionScript} encapsulating the script.
	 * @throws ScriptParseException
	 *             Exception when an error occurred during parsing.
	 * @throws ScriptCompilationException
	 *             Exception when an error occurred during compilation.
	 */
	public static FunctionScript<ScriptRowInput, DataCell> wrap(String expression, DataColumnSpec[] columns,
			FlowVariable[] variables, DataType returnType) throws ScriptParseException, ScriptCompilationException {

		ParsedScript parsedScript = ExpressionUtils.parseScript(expression, columns, variables,
				ExpressionConverterUtils.getDestinationType(returnType));

		Class<?> compiledClass = ExpressionUtils.compile(parsedScript);

		return new DefaultFunctionScript(compiledClass, parsedScript);
	}

	/**
	 * Wraps the provided script into a general {@link FunctionScript} that takes a
	 * {@link ScriptRowInput} as input and returns a {@link Boolean}.
	 * 
	 * @param expression
	 *            The expression that shall be parsed and compiled.
	 * @param columns
	 *            The {@link DataColumnSpec}s of the input table.
	 * @param variables
	 *            The available {@link FlowVariable}s.
	 * @return {@link PredicateScript} encapsulating the script.
	 * @throws ScriptParseException
	 *             Exception when an error occurred during parsing.
	 * @throws ScriptCompilationException
	 *             Exception when an error occurred during compilation.
	 */
	public static PredicateScript<ScriptRowInput> wrap(String expression, DataColumnSpec[] columns,
			FlowVariable[] variables) throws ScriptParseException, ScriptCompilationException {

		ParsedScript parsedScript = ExpressionUtils.parseScript(expression, columns, variables, Boolean.class);

		Class<?> compiledClass = ExpressionUtils.compile(parsedScript);

		return new DefaultPredicateScript(compiledClass, parsedScript);
	}
}
