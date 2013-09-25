package org.knime.base.node.rules.engine;

import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Tests where {@link SimpleRuleParser} should not parse the input.
 * 
 * @author Gabor Bakos
 */
@RunWith(value = Parameterized.class)
public class SimpleRuleParserNegativeTest {
    private SimpleRuleParser simpleRuleParser;

    private String m_rule;

    /**
     * Constructor for the parameter.
     * 
     * @param rule The "rule" as text.
     */
    public SimpleRuleParserNegativeTest(final String rule) {
        this.m_rule = rule;
        final DataTableSpec spec = new DataTableSpec(new DataColumnSpecCreator("a", StringCell.TYPE).createSpec());
        simpleRuleParser =
            new SimpleRuleParser(spec, Collections.singletonMap("knime.workspace", new FlowVariable("Sknime.workspace",
                "/tmp/workspace")));
    }

    /**
     * Generates parameters.
     * 
     * @return The failing rules, first element is a rule as {@link String}.
     */
    @Parameters
    public static Collection<Object[]> rules() {
        return Arrays.asList(a("4 > 3 => $$ROW_INDEX$$ "), a("   \"Hello world\" LIKE \"Hello*  => $$ROWCOUNT$$  "),
            a("1 < \"Hello\""), a(" NOTMISSING $a$ OR 4>4 => \"Q\""),
            a("$${Snonexistent}$$ = \"Hello\" => $$ROWCOUNT$$  "),
            a("MISSING $${Sknime.workspace}$$ => $$ROWCOUNT$$  "), a("NOTTRUE => $$ROWCOUNT$$  "), a("NOT TRUE => "),
            a("NOT TRUE"), a(" $a$ IN $a$ => \"Q\""));
    }

    private static Object[] a(final String string) {
        return new String[]{string};
    }

    /**
     * Tests for an error.
     * 
     * @throws ParseException The expected error.
     */
    @Test(expected = ParseException.class)
    public void testParse() throws ParseException {
        assertNotNull(m_rule, simpleRuleParser.parse(m_rule));
    }

}
