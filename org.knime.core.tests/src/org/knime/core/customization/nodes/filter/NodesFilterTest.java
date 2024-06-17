/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 24, 2024 (wiswedel): created
 */
package org.knime.core.customization.nodes.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knime.core.customization.filter.PatternPredicate;
import org.knime.core.customization.filter.RuleEnum;
import org.knime.core.customization.nodes.filter.NodesFilter.ScopeEnum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Tests for {@link NodesFilter}.
 */
public class NodesFilterTest {

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void testDeserializationViaPattern() throws JsonProcessingException {
        String ymlInput = """
                scope: view
                rule: allow
                predicate:
                  type: pattern
                  patterns:
                    - org\\.knime.+
                    - com\\.knime.+
                  isRegex: true
                """;

        final NodesFilter customization = mapper.readValue(ymlInput, NodesFilter.class);

        assertEquals(ScopeEnum.VIEW, customization.getScope());
        assertEquals(RuleEnum.ALLOW, customization.getRule());
        assertTrue(customization.getPredicate() instanceof PatternPredicate);
        final PatternPredicate filter = (PatternPredicate)customization.getPredicate();
        assertNotNull(filter.getPatterns());
        assertEquals(2, filter.getPatterns().size());
        assertEquals("org\\.knime.+", filter.getPatterns().get(0).pattern());
        assertEquals("com\\.knime.+", filter.getPatterns().get(1).pattern());
        final String toString = customization.toString();
        assertThat("toString() contains pattern", toString, containsString("com\\.knime"));
        assertThat("toString() contains filter type", toString, containsString("ALLOW"));
    }

    @Test
    void testDeserializationViaPlainText() throws JsonProcessingException {
        String ymlInput = """
                scope: use
                rule: deny
                predicate:
                  type: pattern
                  patterns:
                    - "(this can be invalid"
                    - "org.knime.foo.FileReaderNodeFactory"
                  isRegex: false
                """;

        final NodesFilter customization = mapper.readValue(ymlInput, NodesFilter.class);

        assertEquals(ScopeEnum.USE, customization.getScope());
        assertEquals(RuleEnum.DENY, customization.getRule());
        assertTrue(customization.getPredicate() instanceof PatternPredicate);
        final PatternPredicate filter = (PatternPredicate)customization.getPredicate();
        assertNotNull(filter.getPatterns());
        assertEquals(2, filter.getPatterns().size());
        assertEquals("\\Q(this can be invalid\\E", filter.getPatterns().get(0).pattern());
        assertEquals("\\Qorg.knime.foo.FileReaderNodeFactory\\E", filter.getPatterns().get(1).pattern());
        final String toString = customization.toString();
        assertThat("toString() contains pattern", toString, containsString("FileReaderNodeFactory"));
        assertThat("toString() contains filter type", toString, containsString("DENY"));
    }

    @Test
    void testDeserializationViaPattern_FailOnInvalidPattern() throws JsonProcessingException {
        String nonRegexPatternYML = """
                scope: repository-view
                rule: allow
                predicate:
                  type: pattern
                  patterns:
                  - "org\\.knime(.+"
                  - "com\\.knime.+"
                  isRegex: true
                """;

        Assertions.assertThrows(JsonProcessingException.class,
            () -> mapper.readValue(nonRegexPatternYML, NodesFilter.class));
    }

    @Test
    void testDeserializationViaPattern_FailOnInvalidRule() throws JsonProcessingException {
        // accept is invalid
        String nonRuleYML = """
                scope: repository-view
                rule: accept
                predicate:
                  type: pattern
                  patterns:
                    - "org\\.knime.+"
                    - "com\\.knime.+"
                  isRegex: true
                  """;

        Assertions.assertThrows(JsonProcessingException.class,
            () -> mapper.readValue(nonRuleYML, NodesFilter.class));
    }

}
