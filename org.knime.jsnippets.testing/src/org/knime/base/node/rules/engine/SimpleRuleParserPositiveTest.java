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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.rules.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Tests where {@link SimpleRuleParser} should parse the input, it also checks
 * the generated tree.
 *
 * @author Gabor Bakos
 */
@RunWith(value = Parameterized.class)
public class SimpleRuleParserPositiveTest {
	private SimpleRuleParser simpleRuleParser, noColumnParser;
	private String m_rule;
	private final DataType m_outcomeType;
	private final String m_ruleToString;

	/**
	 * Construct with the parameter.
	 *
	 * @param rule
	 *            The test {@link String}.
	 * @param outcomeType
	 *            The expected outcome type.
	 * @param ruleToString
	 *            The parsed tree's {@link Rule#toString()}.
	 */
	public SimpleRuleParserPositiveTest(final String rule,
			final DataType outcomeType, final String ruleToString) {
		this.m_rule = rule;
		this.m_outcomeType = outcomeType;
		this.m_ruleToString = ruleToString;
		DataColumnSpec dbl = new DataColumnSpecCreator("Dbl", DoubleCell.TYPE)
				.createSpec();
		DataColumnSpec str = new DataColumnSpecCreator("Str", StringCell.TYPE)
				.createSpec();
		DataColumnSpec integer = new DataColumnSpecCreator("Integer",
				IntCell.TYPE).createSpec();
		DataColumnSpec bool = new DataColumnSpecCreator("Boolean",
				BooleanCell.TYPE).createSpec();
		DataTableSpec spec = new DataTableSpec(dbl, str, integer, bool);
		simpleRuleParser = new SimpleRuleParser(spec,
				Collections.<String, FlowVariable> emptyMap());
        noColumnParser = new SimpleRuleParser(null,
            Collections.<String, FlowVariable> emptyMap());
        noColumnParser.disableColumnCheck();
	}

	/**
	 * Generates parameters.
	 *
	 * @return The test parameters. (Input (String), expected outcome type
	 *         (DataType), rule's toString())
	 */
	@Parameters/*(name = "{index}: rule: {0}, type: {1},\nparsed: {2}")*/
	public static Collection<Object[]> rules() {
		return Arrays
				.asList(a("4 > 3 => $$ROWINDEX$$ ", LongCell.TYPE,
						"4>3 => $$ROWINDEX$$"),
						a("   \"Hello world\" LIKE \"Hello*\"  => $$ROWCOUNT$$  ",
								LongCell.TYPE,
								"Hello world like Hello* => $$ROWCOUNT$$"),
						a("1 < \"Hello\" => 32", IntCell.TYPE, "1<Hello => 32"),
						a("$Dbl$ = 3 => 32", IntCell.TYPE, "$Dbl$=3 => 32"),
						// a("$Str$ CONTAINS \"b[oa]ttle\" => \"bttle\"",
						// StringCell.TYPE,
						// "$Str$ contains b[oa]ttle => bttle"),
						a("1 > \"Hello\" => 32", IntCell.TYPE, "1>Hello => 32"),
						a("1<=\"Hello\" => 32", IntCell.TYPE, "1<=Hello => 32"),
						a(" NOT MISSING $Str$ OR 4>4 => \"Q\"",
								StringCell.TYPE,
								"or([not(missing($Str$)), 4>4]) => Q"),
						a(" MISSING $Str$ XOR NOT 4>4 => \"Q\"",
								StringCell.TYPE,
								"xor([missing($Str$), not(4>4)]) => Q"),
						a(" MISSING $Str$ XOR NOT 4>4 AND TRUE => \"Q\"",
								StringCell.TYPE,
								"xor([missing($Str$), and([not(4>4), true])]) => Q"),
						a(" MISSING $Str$ XOR NOT 4>4 OR FALSE => \"Q\"",
								StringCell.TYPE,
								"or([xor([missing($Str$), not(4>4)]), false]) => Q"),
						a(" TRUE => FALSE", BooleanCell.TYPE, "true => false"),
						a(" MISSING $Str$ OR 4>4 AND 3 > 2 => \"Q\"", StringCell.TYPE, "or([missing($Str$), and([4>4, 3>2])]) => Q"),
						a("MISSING $Str$ AND 2< 3 AND 4 > 3=> \"Q\"", StringCell.TYPE, "and([missing($Str$), 2<3, 4>3]) => Q"),
						a("$Boolean$ > TRUE => FALSE", BooleanCell.TYPE, "$Boolean$>true => false"),
						a("$Boolean$ XOR TRUE => FALSE", BooleanCell.TYPE, "xor([$Boolean$, true]) => false"),
						a("$Boolean$ XOR TRUE OR FALSE AND 1=1 => FALSE", BooleanCell.TYPE, "or([xor([$Boolean$, true]), and([false, 1=1])]) => false"),
						a("$Boolean$ XOR TRUE AND FALSE OR 1=1 => FALSE", BooleanCell.TYPE, "or([xor([$Boolean$, and([true, false])]), 1=1]) => false"),
						a("$Boolean$  => FALSE", BooleanCell.TYPE, "$Boolean$ => false"),
						a("NOT $Boolean$ XOR TRUE => FALSE", BooleanCell.TYPE, "xor([not($Boolean$), true]) => false"),
						a("(NOT $Boolean$) XOR $Boolean$ => FALSE", BooleanCell.TYPE, "xor([not($Boolean$), $Boolean$]) => false"),
						a("(TRUE < $$ROWINDEX$$ AND 4<4) => FALSE", BooleanCell.TYPE, "and([true<$$ROWINDEX$$, 4<4]) => false"),
						a("(NOT MISSING $Str$) AND MISSING $Str$ => 32",
								IntCell.TYPE,
								"and([not(missing($Str$)), missing($Str$)]) => 32"));
	}

	private static Object[] a(final String string, final DataType t,
			final String toString) {
		return new Object[] { string, t, toString };
	}

	/**
	 * Tests parsing.
	 *
	 * @throws ParseException
	 *             Should not happen.
	 * @see SimpleRuleParser#parse(String)
	 */
	@Test
	public void testParse() throws ParseException {
		final Rule rule = simpleRuleParser.parse(m_rule);
		assertNotNull(m_rule, rule);
		assertEquals(m_rule, m_outcomeType, rule.getOutcome().getType());
		assertEquals(m_rule, m_ruleToString, rule.toString().split("\n")[0]);
	}

	/**
	 * Tests, whether we could disable column name check, or not.
	 *
	 * @throws ParseException Should not happen.
	 */
	@Test
	public void testDummyParse() throws ParseException {
        final Rule dummyRule = noColumnParser.parse(m_rule);
        assertNotNull(m_rule, dummyRule);
	}
}
