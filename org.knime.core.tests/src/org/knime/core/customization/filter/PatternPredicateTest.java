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
 *   Jun 15, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.customization.filter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PatternPredicate}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class PatternPredicateTest {

    private PatternPredicate regexPredicate;
    private PatternPredicate nonRegexPredicate;

    @BeforeEach
    public void setUp() {
        regexPredicate = new PatternPredicate(List.of(".*test.*", ".*sample.*"), true);
        nonRegexPredicate = new PatternPredicate(List.of("test", "sample"), false);
    }

    @Test
    void testConstructorAndGetPatterns() {
        List<Pattern> regexPatterns = regexPredicate.getPatterns();
        List<Pattern> nonRegexPatterns = nonRegexPredicate.getPatterns();

        assertEquals(2, regexPatterns.size());
        assertEquals(2, nonRegexPatterns.size());

        assertTrue(regexPatterns.get(0).pattern().equals(".*test.*"));
        assertTrue(nonRegexPatterns.get(0).pattern().equals("\\Qtest\\E"));
    }

    @Test
    void testIsRegex() {
        assertTrue(regexPredicate.isRegex());
        assertFalse(nonRegexPredicate.isRegex());
    }

    @Test
    void testMatches() {
        assertTrue(regexPredicate.matches("this is a test"));
        assertTrue(regexPredicate.matches("sample text here"));
        assertFalse(regexPredicate.matches("no match"));

        assertTrue(nonRegexPredicate.matches("test"));
        assertTrue(nonRegexPredicate.matches("sample"));
        assertFalse(nonRegexPredicate.matches("this is a test"));
    }

    @Test
    void testToString() {
        String regexToString = regexPredicate.toString();
        String nonRegexToString = nonRegexPredicate.toString();

        assertEquals("PatternFilter{patterns=[.*test.*, .*sample.*]}", regexToString);
        assertEquals("PatternFilter{patterns=[\\Qtest\\E, \\Qsample\\E]}", nonRegexToString);
    }
}

