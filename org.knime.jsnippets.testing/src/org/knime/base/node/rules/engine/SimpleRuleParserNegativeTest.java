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
