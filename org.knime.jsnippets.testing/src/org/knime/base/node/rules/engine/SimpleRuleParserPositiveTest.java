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
	private SimpleRuleParser simpleRuleParser;
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
	}

	/**
	 * Generates parameters.
	 * 
	 * @return The test parameters. (Input (String), expected outcome type
	 *         (DataType), rule's toString())
	 */
	@Parameters
	public static Collection<Object[]> rules() {
		return Arrays
				.asList(a("4 > 3 => $$ROWINDEX$$ ", IntCell.TYPE,
						"4>3 => $$ROWINDEX$$"),
						a("   \"Hello world\" LIKE \"Hello*\"  => $$ROWCOUNT$$  ",
								IntCell.TYPE,
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

}
