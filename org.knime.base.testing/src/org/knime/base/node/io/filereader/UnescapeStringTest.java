/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
