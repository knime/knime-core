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
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;

@RunWith(value=Parameterized.class)
public class SimpleRuleParserNegativeTest {
	private SimpleRuleParser simpleRuleParser;
	private String m_rule;

	public SimpleRuleParserNegativeTest(String rule) {
		this.m_rule = rule;
		DataTableSpec spec = new DataTableSpec(new DataColumnSpecCreator("a", StringCell.TYPE).createSpec());
		simpleRuleParser = new SimpleRuleParser(spec, Collections.<String, FlowVariable>emptyMap());
	}
	
	@Parameters
	public static Collection<Object[]> rules() {
		return Arrays.asList(a("4 > 3 => $$ROW_INDEX$$ "),
				a("   \"Hello world\" LIKE \"Hello*  => $$ROWCOUNT$$  "),
				a("1 < \"Hello\""), a(" NOTMISSING $a$ OR 4>4 => \"Q\""),
				a(" MISSING $a$ OR 4>4 AND 3 > 2 => \"Q\""),
				a("MISSING $${Sknime.workspace}$$ => $$ROWCOUNT$$  ")
				);
	}

	private static Object[] a(String string) {
		return new String[] {string};
	}

	@Test(expected=ParseException.class)
	public void testParse() throws ParseException {
		assertNotNull(m_rule, simpleRuleParser.parse(m_rule));
	}

}
