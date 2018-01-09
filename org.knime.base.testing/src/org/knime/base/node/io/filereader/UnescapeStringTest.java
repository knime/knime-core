/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   27.04.2005 (ohl): created
 */
package org.knime.base.node.io.filereader;

import junit.framework.TestCase;

import org.knime.core.util.tokenizer.TokenizerSettings;

/**
 *
 * @author Peter Ohl, University of Konstanz
 */
public class UnescapeStringTest extends TestCase {

    /**
     * Tests the unescape method. It converts a string containing the character
     * sequence "\t" or "\n" into a string with the characters '\t' and '\n'.
     * Currently supported are only those two sequences \t and \n.
     */
    public void testIt() {
        String s;
        // pure and single occurances should be handled
        s = "\\t";
        assertEquals("\t", TokenizerSettings.unescapeString(s));
        s = "\\n";
        assertEquals("\n", TokenizerSettings.unescapeString(s));
        // escaped sequences at the end of the string.
        s = "foo\\t";
        assertEquals("foo\t", TokenizerSettings.unescapeString(s));
        s = "foo\\n";
        assertEquals("foo\n", TokenizerSettings.unescapeString(s));
        // escaped sequences at the beginning of the string
        s = "\\tfoo";
        assertEquals("\tfoo", TokenizerSettings.unescapeString(s));
        s = "\\nfoo";
        assertEquals("\nfoo", TokenizerSettings.unescapeString(s));
        // combinations of all of that
        s = "\\tfoo\\n\\t\\nfoo\\tfoo\\t";
        assertEquals("\tfoo\n\t\nfoo\tfoo\t",
                TokenizerSettings.unescapeString(s));
        s = "foo\\n\\t\\nfoo\\tfoo\\t";
        assertEquals("foo\n\t\nfoo\tfoo\t",
                TokenizerSettings.unescapeString(s));
        s = "\\tfoo\\n\\t\\nfoo\\tfoo";
        assertEquals("\tfoo\n\t\nfoo\tfoo",
                TokenizerSettings.unescapeString(s));
        // make sure backslashes are handled correctly
        s = "foo\\\\foo\\t\\\\\\nfoo";
        assertEquals("foo\\foo\t\\\nfoo", TokenizerSettings.unescapeString(s));
        // characters other than t and n shouldn't be escaped
        s = "\\gfoo\\h";
        assertEquals("gfooh", TokenizerSettings.unescapeString(s));
        // strings without backslash should be returned unchanged
        s = "blahblah";
        assertSame(s, TokenizerSettings.unescapeString(s));
        // should not choke on null
        s = null;
        assertNull(TokenizerSettings.unescapeString(s));
        // nor empty strings
        s = "";
        assertEquals("", TokenizerSettings.unescapeString(s));
        // nor backslashes at the end
        s = "\\";
        assertEquals("\\", TokenizerSettings.unescapeString(s));
        s = "foo\\";
        assertEquals("foo\\", TokenizerSettings.unescapeString(s));
        s = "foo\\n\\";
        assertEquals("foo\n\\", TokenizerSettings.unescapeString(s));
    }
}
