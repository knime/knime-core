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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.02.2013 (meinl): created
 */
package org.knime.base.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Testcases for {@link WildcardMatcher}.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class WildcardMatcherTest {
    /**
     * Test without escaping.
     */
    @Test
    public void testConversionWOEscaping() {
        Pattern p = Pattern.compile(WildcardMatcher.wildcardToRegex("All * kids"));
        assertTrue("* not converted correctly", p.matcher("All my kids").matches());
        assertTrue("* not converted correctly", p.matcher("All your kids").matches());
        assertFalse("* not converted correctly", p.matcher("Alllllll my kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("All ? kids"));
        assertTrue("? not converted correctly", p.matcher("All 2 kids").matches());
        assertTrue("? not converted correctly", p.matcher("All 5 kids").matches());
        assertFalse("? not converted correctly", p.matcher("All 12 kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("All * \\ kids"));
        assertTrue("\\ not escaped correctly", p.matcher("All my \\ kids").matches());
        assertFalse("\\ not escaped correctly", p.matcher("All my kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("All \\* kids"));
        assertTrue("\\* w/o escaping not handled correctly", p.matcher("All \\ my kids").matches());
        assertFalse("\\* w/o escaping not handled correctly", p.matcher("All * kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("All \\? kids"));
        assertTrue("\\? w/o escaping not handled correctly", p.matcher("All \\5 kids").matches());
        assertFalse("\\? w/o escaping not handled correctly", p.matcher("All \\ 5 kids").matches());
        assertFalse("\\? w/o escaping not handled correctly", p.matcher("All ? kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("\\* kids"));
        assertTrue("\\* at beginning w/o escaping not handled correctly", p.matcher("\\ all kids").matches());
        assertTrue("\\* at beginning  w/o escaping not handled correctly", p.matcher("\\* kids").matches());
        assertFalse("\\* at beginning  w/o escaping not handled correctly", p.matcher("* kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("\\? kids"));
        assertTrue("\\? at beginning w/o escaping not handled correctly", p.matcher("\\5 kids").matches());
        assertTrue("\\? at beginning w/o escaping not handled correctly", p.matcher("\\? kids").matches());
        assertFalse("\\? at beginning w/o escaping not handled correctly", p.matcher("? kids").matches());
    }

    /**
     * Test with escaping.
     */
    @Test
    public void testConversionWithEscaping() {
        Pattern p = Pattern.compile(WildcardMatcher.wildcardToRegex("All * kids", true));
        assertTrue("* not converted correctly", p.matcher("All my kids").matches());
        assertTrue("* not converted correctly", p.matcher("All your kids").matches());
        assertFalse("* not converted correctly", p.matcher("Alllllll my kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("All ? kids", true));
        assertTrue("? not converted correctly", p.matcher("All 2 kids").matches());
        assertTrue("? not converted correctly", p.matcher("All 5 kids").matches());
        assertFalse("? not converted correctly", p.matcher("All 12 kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("All * \\ kids", true));
        assertTrue("\\ not escaped correctly", p.matcher("All *  kids").matches());
        assertFalse("\\ not escaped correctly", p.matcher("All * \\ kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("All \\* kids", true));
        assertTrue("\\* w/ escaping not handled correctly", p.matcher("All * kids").matches());
        assertFalse("\\* w/ escaping not handled correctly", p.matcher("All \\* kids").matches());
        assertFalse("\\* w/ escaping not handled correctly", p.matcher("All \\my kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("All \\? kids", true));
        assertTrue("\\? w/ escaping not handled correctly", p.matcher("All ? kids").matches());
        assertFalse("\\? w/ escaping not handled correctly", p.matcher("All \\5 kids").matches());
        assertFalse("\\? w/ escaping not handled correctly", p.matcher("All 5 kids").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("\\* Bla", true));
        assertTrue("\\* w/ escaping not handled correctly", p.matcher("* Bla").matches());
        assertFalse("\\* w/ escaping not handled correctly", p.matcher("\\* Bla").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("\\\\ Bla", true));
        assertTrue("\\\\ w/ escaping not handled correctly", p.matcher("\\ Bla").matches());
        assertFalse("\\\\ w/ escaping not handled correctly", p.matcher("\\\\ Bla").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("*\\\\ Bla", true));
        assertTrue("\\\\ w/ escaping not handled correctly", p.matcher("Hallo\\ Bla").matches());

        p = Pattern.compile(WildcardMatcher.wildcardToRegex("?\\\\ Bla", true));
        assertTrue("\\\\ w/ escaping not handled correctly", p.matcher("5\\ Bla").matches());
    }
}
