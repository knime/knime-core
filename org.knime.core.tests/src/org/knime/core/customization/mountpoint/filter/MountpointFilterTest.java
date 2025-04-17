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
package org.knime.core.customization.mountpoint.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knime.core.customization.filter.PatternPredicate;
import org.knime.core.customization.filter.RuleEnum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Tests for {@link MountPointFilter}.
 */
public class MountpointFilterTest {

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void testDeserializationViaPattern() throws JsonProcessingException {
        String ymlInput = """
                rule: allow
                predicate:
                  type: pattern
                  patterns:
                    - .+\\.hub\\.knime\\.org
                    - .+\\.hub\\.knime\\.com
                  isRegex: true
                """;

        final MountPointFilter customization = mapper.readValue(ymlInput, MountPointFilter.class);

        assertEquals(RuleEnum.ALLOW, customization.getRule());
        assertTrue(customization.getPredicate() instanceof PatternPredicate);
        final PatternPredicate filter = (PatternPredicate)customization.getPredicate();
        assertNotNull(filter.getPatterns());
        assertEquals(2, filter.getPatterns().size());
        assertEquals(".+\\.hub\\.knime\\.org", filter.getPatterns().get(0).pattern());
        assertEquals(".+\\.hub\\.knime\\.com", filter.getPatterns().get(1).pattern());
        final String toString = customization.toString();
        assertThat("toString() contains pattern", toString, containsString("knime\\.com"));
        assertThat("toString() contains filter type", toString, containsString("ALLOW"));
    }

    @Test
    void testDeserializationViaPlainText() throws JsonProcessingException {
        String ymlInput = """
                rule: deny
                predicate:
                  type: pattern
                  patterns:
                    - "(this can be invalid"
                    - "api.hub.knime.com"
                  isRegex: false
                """;

        final MountPointFilter customization = mapper.readValue(ymlInput, MountPointFilter.class);

        assertEquals(RuleEnum.DENY, customization.getRule());
        assertTrue(customization.getPredicate() instanceof PatternPredicate);
        final PatternPredicate filter = (PatternPredicate)customization.getPredicate();
        assertNotNull(filter.getPatterns());
        assertEquals(2, filter.getPatterns().size());
        assertEquals("\\Q(this can be invalid\\E", filter.getPatterns().get(0).pattern());
        assertEquals("\\Qapi.hub.knime.com\\E", filter.getPatterns().get(1).pattern());
        final String toString = customization.toString();
        assertThat("toString() contains pattern", toString, containsString("knime.com"));
        assertThat("toString() contains filter type", toString, containsString("DENY"));
    }

    @Test
    void testDeserializationViaPattern_FailOnInvalidPattern() throws JsonProcessingException {
        String nonRegexPatternYML = """
                rule: allow
                predicate:
                  type: pattern
                  patterns:
                  - "org\\.knime(.+"
                  - ".+\\.hub\\.knime\\.com"
                  isRegex: true
                """;

        Assertions.assertThrows(JsonProcessingException.class,
            () -> mapper.readValue(nonRegexPatternYML, MountPointFilter.class));
    }

    @Test
    void testDeserializationViaPattern_FailOnInvalidRule() throws JsonProcessingException {
        // accept is invalid
        String nonRuleYML = """
                rule: accept
                predicate:
                  type: pattern
                  patterns:
                    - ".+\\.hub\\.knime\\.org"
                    - ".+\\.hub\\.knime\\.com"
                  isRegex: true
                  """;

        Assertions.assertThrows(JsonProcessingException.class,
            () -> mapper.readValue(nonRuleYML, MountPointFilter.class));
    }

}
