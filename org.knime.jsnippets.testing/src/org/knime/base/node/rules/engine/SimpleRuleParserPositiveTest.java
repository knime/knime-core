package org.knime.base.node.rules.engine;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.knime.base.node.rules.engine.SimpleRuleParser;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;

@RunWith(value=Parameterized.class)
public class SimpleRuleParserPositiveTest {
	private SimpleRuleParser simpleRuleParser;
	private String m_rule;
	private final DataType m_outcomeType;
	private final String m_ruleToString;

	public SimpleRuleParserPositiveTest(String rule, DataType outcomeType, String ruleToString) {
		this.m_rule = rule;
		this.m_outcomeType = outcomeType;
		this.m_ruleToString = ruleToString;
		DataColumnSpec dbl = new DataColumnSpecCreator("Dbl", DoubleCell.TYPE).createSpec();
		DataColumnSpec str = new DataColumnSpecCreator("Str", StringCell.TYPE).createSpec();
		DataColumnSpec integer = new DataColumnSpecCreator("Integer", IntCell.TYPE).createSpec();
		DataTableSpec spec = new DataTableSpec(dbl, str, integer);
		simpleRuleParser = new SimpleRuleParser(spec, Collections.<String, FlowVariable>emptyMap());
	}
	
	@Parameters
	public static Collection<Object[]> rules() {
		return Arrays.asList(a("4 > 3 => $$ROWINDEX$$ ", IntCell.TYPE, "4>3 => $$ROWINDEX$$"),
				a("   \"Hello world\" LIKE \"Hello*\"  => $$ROWCOUNT$$  ", IntCell.TYPE, "Hello world like Hello* => $$ROWCOUNT$$"),
				a("1 < \"Hello\" => 32", IntCell.TYPE, "1<Hello => 32"),
				a("$Dbl$ = 3 => 32", IntCell.TYPE, "$Dbl$=3 => 32"),
				a("$Str$ CONTAINS \"b[oa]ttle\" => \"bttle\"", StringCell.TYPE, "$Str$ contains b[oa]ttle => bttle"),
				a("1 > \"Hello\" => 32", IntCell.TYPE, "1>Hello => 32"),
				a("1<=\"Hello\" => 32", IntCell.TYPE, "1<=Hello => 32"),
				a(" NOT MISSING $Str$ OR 4>4 => \"Q\"", StringCell.TYPE, "not(or([missing($Str$), 4>4])) => Q"),
				a("(NOT MISSING $Str$) AND MISSING $Str$ => 32", IntCell.TYPE, "and([not(missing($Str$)), missing($Str$)]) => 32")
				);
	}

	private static Object[] a(String string, DataType t, String toString) {
		return new Object[] {string, t, toString};
	}

	@Test
	public void testParse() throws ParseException {
		final Rule rule = simpleRuleParser.parse(m_rule);
		assertNotNull(m_rule, rule);
		assertEquals(m_rule, m_outcomeType, rule.getOutcome().getType());
		assertEquals(m_rule, m_ruleToString, rule.toString().split("\n")[0]);
		
	}

}
