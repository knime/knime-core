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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.expressions.node.ExpressionCompletionProvider;

/**
 * TestCase to test execution of expressions.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class ExpressionExecuteTest {

	/**
	 * Tests that the last statement will be returned.
	 */
	@Test
	public void returnTest() {
		/* Check that last statement is returned. */
		String exp = "5*4 \n" + "5*6 \n" + "5*8";

		FunctionalScript<DataRow, DataCell> function = null;

		try {
			ParsedScript pScript = ExpressionUtils.parseScript(exp, null, null, DataType.getType(DoubleCell.class));
			FunctionScript fscript = ExpressionUtils.compile(pScript);
			function = ExpressionUtils.wrap(fscript);
		} catch (ScriptParseException | ScriptCompilationException e) {
			fail();
		}

		DoubleCell result = null;

		try {
			result = (DoubleCell) function.apply(null);
		} catch (ScriptExecutionException e) {
			fail();
		}

		assertEquals(40.0, result.getDoubleValue(), 0);

		/*
		 * Check that still last statement is returned, even though it's assigned to a
		 * variable.
		 */
		exp = "a1 = 5*4 \n" + "5*6 \n" + "a3 = 5*8";

		try {
			ParsedScript pScript = ExpressionUtils.parseScript(exp, null, null, DataType.getType(DoubleCell.class));
			FunctionScript fscript = ExpressionUtils.compile(pScript);
			function = ExpressionUtils.wrap(fscript);
		} catch (ScriptParseException | ScriptCompilationException e) {
			fail();
		}

		try {
			result = (DoubleCell) function.apply(null);
		} catch (ScriptExecutionException e) {
			fail();
		}

		assertEquals(40.0, result.getDoubleValue(), 0);

		/* Check that a1 will be returned, as 'return a1' is last statement. */
		exp = "a1 = 5*4 \n" + "5*6 \n" + "a3 = 5*8 \n return a1";

		try {
			ParsedScript pScript = ExpressionUtils.parseScript(exp, null, null, DataType.getType(DoubleCell.class));
			FunctionScript fscript = ExpressionUtils.compile(pScript);
			function = ExpressionUtils.wrap(fscript);
		} catch (ScriptParseException | ScriptCompilationException e) {
			fail();
		}

		try {
			result = (DoubleCell) function.apply(null);
		} catch (ScriptExecutionException e) {
			fail();
		}

		assertEquals(20.0, result.getDoubleValue(), 0);
	}

	/**
	 * Tests access of columns in the script.
	 */
	@Test
	public void columnAccessTest() {
		DataRow row = new DefaultRow("TestRow 1", IntCellFactory.create(3), IntCellFactory.create(2));
		DataRow row2 = new DefaultRow("TestRow 1", IntCellFactory.create(5), IntCellFactory.create(7));
		DataColumnSpecCreator specCreator = new DataColumnSpecCreator("col1", DataType.getType(IntCell.class));

		DataColumnSpec col1 = specCreator.createSpec();
		specCreator.setName("col2");
		DataColumnSpec col2 = specCreator.createSpec();

		String columnStart = ExpressionCompletionProvider.getEscapeColumnStartSymbol();
		String columnEnd = ExpressionCompletionProvider.getEscapeColumnEndSymbol();

		String expr = columnStart + "col1" + columnEnd + " + " + columnStart + "col2" + columnEnd;

		FunctionalScript<DataRow, DataCell> function = null;

		try {
			ParsedScript pScript = ExpressionUtils.parseScript(expr, new DataColumnSpec[] { col1, col2 }, null,
					DataType.getType(IntCell.class));
			FunctionScript fScript = ExpressionUtils.compile(pScript);
			function = ExpressionUtils.wrap(fScript);

			IntCell result = (IntCell) function.apply(row);

			assertEquals(5, result.getIntValue());

			result = (IntCell) function.apply(row2);

			assertEquals(12, result.getIntValue());
		} catch (Exception e) {
			fail();
		}
	}

	/**
	 * Tests access of flow variables in the script.
	 */
	@Test
	public void testFlowVariableAccess() {
		FlowVariable var1 = new FlowVariable("var1", 2);
		FlowVariable var2 = new FlowVariable("var2", 3);

		String varStart = ExpressionCompletionProvider.getEscapeFlowVariableStartSymbol();
		String varEnd = ExpressionCompletionProvider.getEscapeFlowVariableEndSymbol();

		String expr = varStart + "var1" + varEnd + " + " + varStart + "var2" + varEnd;

		FunctionalScript<DataRow, DataCell> function = null;

		try {
			ParsedScript pScript = ExpressionUtils.parseScript(expr, null, new FlowVariable[] { var1, var2 },
					DataType.getType(IntCell.class));
			FunctionScript fScript = ExpressionUtils.compile(pScript);
			function = ExpressionUtils.wrap(fScript);

			IntCell result = (IntCell) function.apply(null);

			assertEquals(5, result.getIntValue());
		} catch (Exception e) {
			fail();
		}
	}

	/**
	 * Tests method calls within the expression. This assumes the logical functions
	 * and(...) and or(...) are registered at {@link ExpressionSetRegistry}, e.g.
	 * {@link org.knime.expressions.sets.logical.And} and
	 * {@link org.knime.expressions.sets.logical.Or}.
	 */
	@Test
	public void functionCallTest() {
		DataRow row = new DefaultRow("TestRow 1", IntCellFactory.create(3), IntCellFactory.create(2));
		DataRow row2 = new DefaultRow("TestRow 1", IntCellFactory.create(5), IntCellFactory.create(7));
		DataRow row3 = new DefaultRow("TestRow 1", IntCellFactory.create(5), IntCellFactory.create(10));
		DataColumnSpecCreator specCreator = new DataColumnSpecCreator("col1", DataType.getType(IntCell.class));

		DataColumnSpec col1 = specCreator.createSpec();
		specCreator.setName("col2");
		DataColumnSpec col2 = specCreator.createSpec();

		String columnStart = ExpressionCompletionProvider.getEscapeColumnStartSymbol();
		String columnEnd = ExpressionCompletionProvider.getEscapeColumnEndSymbol();

		String expr = "and(" + columnStart + "col1" + columnEnd + " < " + columnStart + "col2" + columnEnd
				+ ", or(false, " + columnStart + "col2" + columnEnd + " < 10))";

		FunctionalScript<DataRow, DataCell> function = null;

		try {
			ParsedScript pScript = ExpressionUtils.parseScript(expr, new DataColumnSpec[] { col1, col2 }, null,
					DataType.getType(BooleanCell.class));
			FunctionScript fScript = ExpressionUtils.compile(pScript);
			function = ExpressionUtils.wrap(fScript);

			BooleanCell result = (BooleanCell) function.apply(row);

			assertFalse(result.getBooleanValue());

			result = (BooleanCell) function.apply(row2);

			assertTrue(result.getBooleanValue());

			result = (BooleanCell) function.apply(row3);

			assertFalse(result.getBooleanValue());
		} catch (Exception e) {
			fail();
		}
	}
}
