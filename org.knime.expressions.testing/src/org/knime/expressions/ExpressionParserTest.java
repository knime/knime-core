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
package org.knime.expressions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.expressions.node.ExpressionCompletionProvider;

/**
 * UnitTest to test the parsing of an expression.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class ExpressionParserTest {

	/**
	 * Tests if simple parsing works.
	 */
	@Test
	public void simpleParserTest() {
		String exp = "5*7";
		ParsedScript parsedScript = null;

		try {
			parsedScript = ExpressionUtils.parseScript(exp, null, null, null);
		} catch (Exception e) {
			fail();
		}

		assertFalse(parsedScript.usesColumns());
		assertFalse(parsedScript.isUseRowCount());
		assertFalse(parsedScript.isUseRowId());
		assertFalse(parsedScript.isUseRowIndex());

		String script = parsedScript.getScript();

		assertTrue(script.contains(FunctionScript.METHOD_NAME));

		assertEquals(1, StringUtils.countMatches(script, "{"));

		assertEquals(1, StringUtils.countMatches(script, "}"));

		assertTrue(script.lastIndexOf("{") < script.indexOf("}"));
	}

	/**
	 * Tests that delimiters are not the same, so that we are able to distinguish
	 * the beginning and end of flow variables, columns, or special expressions,
	 * i.e. ROWID, ROWINDEX, ROWCOUNT.
	 */
	@Test
	public void delimiterTest() {
		String[] delimiters = new String[] { ExpressionCompletionProvider.getEscapeColumnEndSymbol(),
				ExpressionCompletionProvider.getEscapeColumnStartSymbol(),
				ExpressionCompletionProvider.getEscapeExpressionEndSymbol(),
				ExpressionCompletionProvider.getEscapeExpressionStartSymbol(),
				ExpressionCompletionProvider.getEscapeFlowVariableEndSymbol(),
				ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol() };

		for (int i = 0; i < delimiters.length - 1; i++) {
			for (int j = i + 1; j < delimiters.length; j++) {
				assertNotEquals(delimiters[i], delimiters[j]);
			}
		}
	}

	/**
	 * Tests the recognition of existing columns and if an exception is thrown if
	 * the column is not present.
	 */
	@Test
	public void parseColumnTest() {
		String columnStart = ExpressionCompletionProvider.getEscapeColumnStartSymbol();
		String columnEnd = ExpressionCompletionProvider.getEscapeColumnEndSymbol();
		String exp = "5*7 \n" + columnStart + "name" + "\n 3*2";

		try {
			ExpressionUtils.parseScript(exp, null, null, null);
			fail();
		} catch (ScriptParseException ex) {
			// expected
		} catch (Exception ex) {
			fail();
		}

		DataColumnSpecCreator specCreator = new DataColumnSpecCreator("name", DataType.getType(StringCell.class));
		DataColumnSpec column = specCreator.createSpec();

		try {
			ExpressionUtils.parseScript(exp, new DataColumnSpec[] { column }, null, null);
			fail();
		} catch (ScriptParseException ex) {
			// expected
		} catch (Exception ex) {
			fail();
		}

		exp = "5*7 \n" + columnStart + "name" + columnEnd + "\n 3*2";

		try {
			ExpressionUtils.parseScript(exp, new DataColumnSpec[] { column }, null, null);
		} catch (Exception ex) {
			fail();
		}
	}

	/**
	 * Tests the recognition of existing flow variables and if an exception is
	 * thrown if the column is not present.
	 */
	@Test
	public void parseFlowVariableTest() {
		String variableStart = ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol();
		String variableEnd = ExpressionCompletionProvider.getEscapeFlowVariableEndSymbol();
		String exp = "5*7 \n" + variableStart + "name" + "\n3*2";

		try {
			ExpressionUtils.parseScript(exp, null, null, null);
			fail();
		} catch (ScriptParseException ex) {
			// expected
		} catch (Exception ex) {
			fail();
		}

		FlowVariable variable = new FlowVariable("name", "test");

		try {
			ExpressionUtils.parseScript(exp, null, new FlowVariable[] { variable }, null);
			fail();
		} catch (ScriptParseException ex) {
			// expected
		} catch (Exception ex) {
			fail();
		}

		exp = "5*7 \n" + variableStart + "name" + variableEnd + "\n 3*2";

		try {
			ExpressionUtils.parseScript(exp, null, new FlowVariable[] { variable }, null);
		} catch (Exception ex) {
			fail();
		}
	}

	/**
	 * Tests the recognition of special expressions (ROWID, ROWINDEX, ROWCOUNT) and
	 * if an exception is thrown if no end delimiter is present.
	 */
	@Test
	public void parseSpecialExpressionTest() {
		String expressionStart = ExpressionCompletionProvider.getEscapeExpressionStartSymbol();
		String expressionEnd = ExpressionCompletionProvider.getEscapeExpressionEndSymbol();
		String exp = "5*7 \n" + expressionStart + org.knime.ext.sun.nodes.script.expression.Expression.ROWID + "\n3*2";

		try {
			ExpressionUtils.parseScript(exp, null, null, null);
			fail();
		} catch (ScriptParseException ex) {
			// expected
		} catch (Exception ex) {
			fail();
		}

		exp = "5*7 \n" + expressionStart + org.knime.ext.sun.nodes.script.expression.Expression.ROWID + expressionEnd
				+ "\n3*2";

		try {
			ExpressionUtils.parseScript(exp, null, null, null);
		} catch (Exception ex) {
			fail();
		}
	}

	/**
	 * Tests the replacement of flow variable, column and special expression if
	 * their name is the same.
	 */
	@Test
	public void parseSameNameTest() {
		String columnStart = ExpressionCompletionProvider.getEscapeColumnStartSymbol();
		String columnEnd = ExpressionCompletionProvider.getEscapeColumnEndSymbol();
		String variableStart = ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol();
		String variableEnd = ExpressionCompletionProvider.getEscapeFlowVariableEndSymbol();
		String expressionStart = ExpressionCompletionProvider.getEscapeExpressionStartSymbol();
		String expressionEnd = ExpressionCompletionProvider.getEscapeExpressionEndSymbol();

		String name = org.knime.ext.sun.nodes.script.expression.Expression.ROWID;

		// Simple String concatenation.
		String exp = columnStart + name + columnEnd + " + " + variableStart + name + variableEnd + " + "
				+ expressionStart + name + expressionEnd;

		FlowVariable variable = new FlowVariable(name, "test");
		DataColumnSpecCreator specCreator = new DataColumnSpecCreator(name, DataType.getType(StringCell.class));
		DataColumnSpec column = specCreator.createSpec();

		String script = "";

		try {
			script = ExpressionUtils
					.parseScript(exp, new DataColumnSpec[] { column }, new FlowVariable[] { variable }, null)
					.getScript();
		} catch (Exception ex) {
			fail();
		}

		/*
		 * Check if the names have been replaced with their prefix, depending on the
		 * type.
		 */
		assertTrue(script.contains("c_" + name + " + f_" + name + " + se_" + name));
	}

	/**
	 * Tests that replaced columns, flow variables, and special expressions will
	 * have a unique name if the parsed name, i.e. name with specified prefix ("c_",
	 * "f_", "se_"), already exists.
	 */
	@Test
	public void parsedNameExistsTest() {
		String columnStart = ExpressionCompletionProvider.getEscapeColumnStartSymbol();
		String columnEnd = ExpressionCompletionProvider.getEscapeColumnEndSymbol();
		String variableStart = ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol();
		String variableEnd = ExpressionCompletionProvider.getEscapeFlowVariableEndSymbol();
		String expressionStart = ExpressionCompletionProvider.getEscapeExpressionStartSymbol();
		String expressionEnd = ExpressionCompletionProvider.getEscapeExpressionEndSymbol();

		String name = org.knime.ext.sun.nodes.script.expression.Expression.ROWID;

		// Simple String concatenation.
		String exp = "c_" + name + " + f_" + name + " + se_" + name + " + " + columnStart + name + columnEnd + " + "
				+ variableStart + name + variableEnd + " + " + expressionStart + name + expressionEnd;

		FlowVariable variable = new FlowVariable(name, "test");
		DataColumnSpecCreator specCreator = new DataColumnSpecCreator(name, DataType.getType(StringCell.class));
		DataColumnSpec column = specCreator.createSpec();

		String script = "";

		try {
			script = ExpressionUtils
					.parseScript(exp, new DataColumnSpec[] { column }, new FlowVariable[] { variable }, null)
					.getScript();
		} catch (Exception ex) {
			fail();
		}

		/* Receive the line of the script describing the expression. */
		String[] split = script.split("\n");

		for (String line : split) {
			if (line.startsWith("c_" + name + " + ")) {
				split = line.split(" + ");
				break;
			}
		}

		/* Check that the replacements are unique. */
		for (int i = 0; i < split.length - 1; i++) {
			for (int j = i + 1; j < split.length; j++) {
				assertNotEquals(split[i], split[j]);
			}
		}
	}

	/**
	 * Tests if a method is not accidentally appended when its name is part of
	 * another name used here (e.g. or(...) and for(...)).
	 */
	@Test
	public void doNotAppendMethodTest() {
		String exp = "for(i = 0; i < 10; i++) {i*i}";
		ParsedScript parsedScript = null;

		try {
			parsedScript = ExpressionUtils.parseScript(exp, null, null, null);
		} catch (Exception e) {
			fail();
		}
		
		String script = parsedScript.getScript();

		assertFalse(script.contains("or(..." ));
	}
}
